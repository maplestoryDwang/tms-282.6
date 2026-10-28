package net.swordie.ms.client.social.Guild;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.tms282.Tms282VMatrixRuntime;
import net.swordie.ms.connection.packet.Tms282GuildApplicationPackets;
import net.swordie.ms.connection.packet.Tms282GuildApplicationPackets.Summary;
import net.swordie.ms.connection.packet.Tms282GuildPackets;
import net.swordie.ms.console.config.ServerRuntimeConfig;
import java.nio.charset.Charset;
import java.sql.SQLException;
import java.util.*;

/** Native application lifecycle, separately gated until its CREATE-only table is deployed. */
public final class Tms282GuildApplications {
    private static final boolean ENABLED=Boolean.parseBoolean(System.getProperty("tms282.guild.applications.enabled",
            ServerRuntimeConfig.stringValue("tms282.guild.applications.enabled")));
    private static final Tms282GuildJournal JOURNAL=new Tms282GuildJournal();
    private Tms282GuildApplications() { }
    public static boolean enabled() { return ENABLED&&Tms282GuildRuntime.enabled(); }
    private static void access(Char chr) {
        if(!enabled())throw new IllegalArgumentException("公會申請功能尚未啟用。");
        Tms282VMatrixRuntime.requireEconomicAccess(chr);
        if(chr.getTradeRoom()!=null||chr.getMiniRoom()!=null)throw new IllegalArgumentException("請先結束目前交易。");
    }
    public static List<Tms282GuildJournal.Application> active(int characterId) throws SQLException {
        return !enabled()?List.of():JOURNAL.applications(characterId,false).stream().filter(Tms282GuildJournal.Application::active).toList();
    }
    public static Summary summary(Guild guild,boolean applied) {
        int average=(int)guild.getMembers().stream().mapToInt(GuildMember::getLevel).average().orElse(0);
        // Recruitment filter masks (+58/+5C/+60) are unset until supported tag persistence exists; native defaults are zero.
        return new Summary(guild.getId(),guild.getLevel(),guild.getName(),guild.getGuildLeader().getName(),
                guild.getMembers().size(),average,applied,0,true,guild.getNotice()==null?"":guild.getNotice(),0,0,0,false);
    }
    public static List<Tms282GuildPackets.Applicant> applicants(Guild guild,net.swordie.ms.world.World world) {
        if(!enabled())return guild.getRequestors().stream().map(a->new Tms282GuildPackets.Applicant(a.getCharID(),a.getName(),a.getJob(),a.getLevel(),a.isOnline(),"")).toList();
        try { return JOURNAL.applications(guild.getId(),true).stream().filter(Tms282GuildJournal.Application::active)
                .map(a->new Tms282GuildPackets.Applicant(a.characterId(),a.name(),a.job(),a.level(),world!=null&&world.getCharById(a.characterId())!=null,a.message())).toList(); }
        catch(SQLException e) { throw new IllegalStateException("Could not read authoritative guild applications",e); }
    }
    public static void restore(Char chr,Charset charset) throws SQLException {
        if(!enabled())return;
        synchronized(Tms282VMatrixRuntime.ECONOMY_MONITOR) {
            var summaries=new ArrayList<Summary>();
            for(var a:active(chr.getId())) {
                Guild guild=JOURNAL.findApplicationGuild(chr.getWorld().getWorldId(),a.guildId());
                if(guild==null) { // A removed guild must not trap its applicants at login; no fee or cooldown was incurred.
                    JOURNAL.application(chr,chr.getId(),a.guildId(),"prune",null,System.currentTimeMillis());continue;
                }
                summaries.add(summary(guild,true));
            }
            chr.write(Tms282GuildApplicationPackets.applications(summaries,charset));
        }
    }
    public static void apply(Char chr,int guildId,String message,Charset charset) throws SQLException {
        synchronized(Tms282VMatrixRuntime.ECONOMY_MONITOR) {
            access(chr);Guild guild=chr.getWorld().getGuilds().get(guildId);
            if(guild!=null && guild.hasInvitation(chr.getId(),System.currentTimeMillis())) {
                if(guild.hasPermission(guild.inviterId(chr.getId()),1)) {
                    Tms282GuildRuntime.joinInvited(chr,guildId,charset);return;
                }
                guild.revokeInvitation(chr.getId()); // former inviter lost authority; a normal application is still possible
            }
            guild=JOURNAL.findApplicationGuild(chr.getWorld().getWorldId(),guildId);
            if(guild==null)throw new IllegalArgumentException("找不到這個公會。");
            if(chr.getGuildID()!=0||chr.getGuild()!=null)throw new IllegalArgumentException("已加入公會。");
            if(!guild.isAppliable())throw new IllegalArgumentException("這個公會不接受加入申請。");
            if(chr.getLevel()<Math.max(101,guild.getReqLevel()))throw new IllegalArgumentException("等級未達公會申請條件。");
            var current=JOURNAL.applications(chr.getId(),false);
            if(current.stream().anyMatch(a->a.active()&&a.guildId()==guildId))throw new IllegalArgumentException("已申請加入這個公會。");
            if(current.stream().filter(Tms282GuildJournal.Application::active).count()>=5)throw new IllegalArgumentException("最多可申請加入5個公會。");
            long now=System.currentTimeMillis();
            if(current.stream().anyMatch(a->a.cancelUntil()>now))throw new IllegalArgumentException("取消申請後1分鐘內無法再次申請。");
            if(message==null||message.getBytes(charset).length>1000||!charset.newEncoder().canEncode(message))throw new IllegalArgumentException("申請內容過長或無法顯示。");
            JOURNAL.application(chr,chr.getId(),guildId,"apply",message,System.currentTimeMillis());
            chr.write(Tms282GuildApplicationPackets.submitted(summary(guild,true),charset));
            restore(chr,charset);
            Guild onlineGuild=chr.getWorld().getGuilds().get(guildId);
            if(onlineGuild!=null)broadcast(chr,onlineGuild,Tms282GuildApplicationPackets.applicant(guildId,
                    new Tms282GuildPackets.Applicant(chr.getId(),chr.getName(),chr.getJob(),chr.getLevel(),true,message),charset));
        }
    }
    public static void withdraw(Char chr,int guildId,Charset charset) throws SQLException {
        synchronized(Tms282VMatrixRuntime.ECONOMY_MONITOR) {
            access(chr);
            if(active(chr.getId()).stream().noneMatch(a->a.guildId()==guildId))throw new IllegalArgumentException("這份申請已不存在。");
            boolean unavailable=JOURNAL.findApplicationGuild(chr.getWorld().getWorldId(),guildId)==null;
            JOURNAL.application(chr,chr.getId(),guildId,unavailable?"prune":"withdraw",null,System.currentTimeMillis());
            chr.write(Tms282GuildApplicationPackets.removed(unavailable?25:32,chr.getId(),guildId));
            notifyRemoval(chr,guildId,chr.getId());restore(chr,charset);
        }
    }
    public static void review(Char actor,List<Integer> targets,boolean approve,Charset charset) throws SQLException {
        synchronized(Tms282VMatrixRuntime.ECONOMY_MONITOR) {
            access(actor);Guild guild=actor.getGuild();
            if(guild==null||!guild.hasPermission(actor.getId(),64)||targets.isEmpty()||targets.size()>(approve?255:100)
                    || targets.stream().anyMatch(id->id<=0)||new HashSet<>(targets).size()!=targets.size())
                throw new IllegalArgumentException("無公會申請審核權限或名單無效。");
            // Each accepted target commits independently. Earlier accepted targets remain accepted if a later row conflicts.
            for(int targetId:targets) {
                Char target=actor.getWorld().getCharById(targetId);
                if(target!=null)Tms282VMatrixRuntime.requireEconomicAccess(target);
                var previous=active(targetId);
                if(previous.stream().noneMatch(a->a.guildId()==guild.getId()))throw new IllegalArgumentException("這份申請已不存在。");
                if(approve && (guild.getMembers().size()>=guild.getMaxMembers()||target!=null&&(target.getGuildID()!=0||target.getGuild()!=null)))
                    throw new IllegalArgumentException("公會已滿或該角色已加入公會。");
                JOURNAL.application(actor,targetId,guild.getId(),approve?"approve":"reject",null,System.currentTimeMillis());
                target=actor.getWorld().getCharById(targetId);
                if(approve) {
                    if(target!=null) { JOURNAL.refreshOfflineMembership(target);Tms282GuildRuntime.sendMembership(target,13,charset); }
                    for(var app:previous)notifyRemoval(actor,app.guildId(),targetId);
                    for(var member:List.copyOf(guild.getMembers())) {
                        Char online=actor.getWorld().getCharById(member.getCharID());
                        if(online!=null&&online!=target)Tms282GuildRuntime.sendMembership(online,1,charset);
                    }
                } else {
                    notifyRemoval(actor,guild.getId(),targetId);
                    if(target!=null)target.write(Tms282GuildApplicationPackets.removed(25,targetId,guild.getId()));
                }
                if(target!=null)restore(target,charset);
            }
        }
    }
    private static void notifyRemoval(Char context,int guildId,int targetId) {
        Guild guild=context.getWorld().getGuilds().get(guildId);
        if(guild!=null)broadcast(context,guild,Tms282GuildApplicationPackets.removed(25,targetId,guildId));
    }
    private static void broadcast(Char context,Guild guild,net.swordie.ms.connection.OutPacket packet) {
        for(var member:List.copyOf(guild.getMembers())) {
            Char online=context.getWorld().getCharById(member.getCharID());
            if(online!=null && online.getGuildID()==guild.getId())online.write(packet);
        }
    }
    /** Existing create/invited-join paths clear applications in their same durable intent. */
    public static void joined(Char chr,List<Tms282GuildJournal.Application> previous,Charset charset) throws SQLException {
        if(!enabled())return;
        for(var app:previous)notifyRemoval(chr,app.guildId(),chr.getId());restore(chr,charset);
    }
}
