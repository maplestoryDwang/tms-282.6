package net.swordie.ms.client.social.Guild;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.tms282.Tms282VMatrixRuntime;
import net.swordie.ms.connection.packet.Tms282GuildPackets;
import net.swordie.ms.connection.packet.Tms282StatPackets;
import net.swordie.ms.connection.packet.Tms282GuildPackets.*;
import net.swordie.ms.connection.protocol.*;
import net.swordie.ms.console.config.ServerRuntimeConfig;
import java.nio.charset.Charset;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** Native guild core. Enable after CREATE-only journal migration and shared save/recovery hooks. */
public final class Tms282GuildRuntime {
    private static final boolean ENABLED=Boolean.parseBoolean(System.getProperty("tms282.guild.enabled",ServerRuntimeConfig.stringValue("tms282.guild.enabled")));
    private static final Tms282GuildJournal JOURNAL=new Tms282GuildJournal();
    private static final Map<Integer,Draft> DRAFTS=new ConcurrentHashMap<>();
    private record Draft(String name,long expires) { }
    private Tms282GuildRuntime() { }
    public static boolean enabled() { return ENABLED && Protocol.active().version()==ProtocolVersion.TMS_282_3; }
    /** Legacy mutators must stop before changing cached authority; native membership uses the redo journal. */
    public static void requireLegacyMembershipMutation() {
        if(enabled())throw new IllegalStateException("Use the native guild membership journal");
    }
    /** Keep the same economy monitor held across this check and irreversible character deletion. */
    public static boolean canDeleteCharacter(int characterId) throws SQLException {
        if(!enabled())return true;
        synchronized(Tms282VMatrixRuntime.ECONOMY_MONITOR) { return JOURNAL.canDeleteCharacter(characterId); }
    }
    public static long creationCost() {
        long value=Long.parseLong(System.getProperty("tms282.guild.createCost","100000000"));
        if(value<0)throw new IllegalStateException("Invalid guild creation price");return value;
    }
    public static void beforeLoad(Char chr) throws SQLException {
        if(enabled())synchronized(Tms282VMatrixRuntime.ECONOMY_MONITOR) { JOURNAL.recoverForLoad(chr); }
    }
    public static void onFieldEntry(Char chr,Charset charset) {
        if(enabled())synchronized(Tms282VMatrixRuntime.ECONOMY_MONITOR) {
            refreshOfflineMembership(chr);
            Guild guild=chr.getGuild();if(guild!=null&&guild.getMemberByCharID(chr.getId())!=null)guild.getMemberByCharID(chr.getId()).setOnline(true);
            sendMembership(chr,1,charset);
            try { Tms282GuildApplications.restore(chr,charset); }
            catch(SQLException e) { throw new IllegalStateException("Could not restore guild applications",e); }
        }
    }
    public static void onPresence(Char chr,boolean online) {
        if(!enabled())return;
        synchronized(Tms282VMatrixRuntime.ECONOMY_MONITOR) {
            // Also refresh on disconnect: never broadcast a preloaded instance's removed membership.
            refreshOfflineMembership(chr);
            String configured=System.getProperty("tms282.nameCharset",ServerRuntimeConfig.stringValue("tms282.nameCharset"));
            Charset charset=Charset.forName(configured==null||configured.isBlank()?"UTF-8":configured);
            Guild guild=chr.getGuild();if(guild==null)return;
            GuildMember member=guild.getMemberByCharID(chr.getId());if(member==null)return;
            member.updateInfoFromChar(chr);member.setOnline(online);
            broadcastMembership(chr.getWorld(),guild,charset,0,1);
        }
    }
    private static void refreshOfflineMembership(Char chr) {
        try { JOURNAL.refreshOfflineMembership(chr); }
        catch(SQLException e) { throw new IllegalStateException("Guild membership requires recovery before presence publication",e); }
    }
    public static void openCreate(Char chr) {
        requireEnabled();
        if(chr.getGuild()!=null || chr.getGuildID()!=0 || chr.getLevel()<101)throw new IllegalArgumentException("創立公會需要等級101且尚未加入公會。");
        chr.chatMessage("此伺服器創立公會費用："+creationCost()+"楓幣，確認名稱後才會扣款。");
        chr.write(Tms282GuildPackets.createDialog());
    }
    public static void prepareCreate(Char chr,String name,Charset charset) {
        requireEnabled();
        if(chr.getGuild()!=null || chr.getGuildID()!=0 || chr.getLevel()<101 || chr.getMoney()<creationCost())throw new IllegalArgumentException("不符合創立公會條件或楓幣不足。");
        if(name.isBlank() || name.length()>13 || name.getBytes(charset).length>24 || name.codePoints().anyMatch(c->Character.isWhitespace(c)||Character.isISOControl(c))
                || !charset.newEncoder().canEncode(name))throw new IllegalArgumentException("公會名稱格式不符。");
        if(chr.getWorld().getGuilds().values().stream().anyMatch(g->g.getName().equalsIgnoreCase(name)))throw new IllegalArgumentException("公會名稱已被使用。");
        long now=System.currentTimeMillis();DRAFTS.entrySet().removeIf(e->e.getValue().expires()<=now);
        if(DRAFTS.size()>=1000&&!DRAFTS.containsKey(chr.getId()))throw new IllegalArgumentException("請稍後再試。");
        DRAFTS.put(chr.getId(),new Draft(name,now+120000));
        chr.chatMessage("此伺服器創立公會費用："+creationCost()+"楓幣。");
        chr.write(Tms282GuildPackets.confirmCreate(name,charset));
    }
    public static void cancelCreate(Char chr) { DRAFTS.remove(chr.getId()); }
    public static void create(Char chr,Charset charset) throws SQLException {
        requireEnabled();
        synchronized(Tms282VMatrixRuntime.ECONOMY_MONITOR) { synchronized(chr) {
            access(chr);Draft draft=DRAFTS.remove(chr.getId());
            if(draft==null || draft.expires()<=System.currentTimeMillis() || chr.getLevel()<101)throw new IllegalArgumentException("創立確認已過期。");
            var applications=Tms282GuildApplications.active(chr.getId());
            chr.saveToSQL();JOURNAL.create(chr,draft.name(),creationCost());
            chr.write(Tms282StatPackets.statChanged(false,Map.of(Tms282StatPackets.Stat.MONEY,chr.getMoney())));
            sendMembership(chr,7,charset);Tms282GuildApplications.joined(chr,applications,charset);
        } }
    }
    public static void invite(Char chr,String name,Charset charset) {
        requireEnabled();
        synchronized(Tms282VMatrixRuntime.ECONOMY_MONITOR) {
            access(chr);Guild guild=requireGuild(chr);Char target=chr.getWorld().getCharByName(name);
            if(target==null || target.getId()==chr.getId() || target.getGuild()!=null || target.getGuildID()!=0 || target.getLevel()<101
                    || !guild.invite(chr.getId(),target.getId(),System.currentTimeMillis()))throw new IllegalArgumentException("目前無法邀請該角色。");
            target.write(Tms282GuildPackets.invitation(guild.getId(),guild.getName(),chr.getId(),chr.getName(),chr.getJob(),chr.getLevel(),guild.getLevel(),chr.getAccount().getId(),charset));
        }
    }
    public static void information(Char chr,int guildId,Charset charset) {
        requireEnabled();Guild guild=chr.getWorld().getGuilds().get(guildId);
        if(guild==null)throw new IllegalArgumentException("找不到公會。");
        chr.write(Tms282GuildPackets.information(snapshot(guild,chr.getWorld()),charset));
    }
    public static void decline(Char chr,String inviterName,String targetName) {
        if(!chr.getName().equals(targetName))throw new IllegalArgumentException("Invalid invitation target");
        for(Guild guild:chr.getWorld().getGuilds().values()) {
            int inviter=guild.inviterId(chr.getId());GuildMember member=guild.getMemberByCharID(inviter);
            if(member!=null && member.getName().equals(inviterName))guild.revokeInvitation(chr.getId());
        }
    }
    /** Invitation popup opens info first; type3 is the subsequent Apply button, never trust its client flags. */
    public static void joinInvited(Char chr,int guildId,Charset charset) throws SQLException {
        requireEnabled();
        synchronized(Tms282VMatrixRuntime.ECONOMY_MONITOR) { synchronized(chr) {
            access(chr);Guild guild=chr.getWorld().getGuilds().get(guildId);
            if(guild==null || chr.getLevel()<101 || !guild.hasInvitation(chr.getId(),System.currentTimeMillis())
                    || !guild.hasPermission(guild.inviterId(chr.getId()),1))throw new IllegalArgumentException("需要有效的公會邀請才能加入。");
            var applications=Tms282GuildApplications.active(chr.getId());
            chr.saveToSQL();JOURNAL.join(chr,guild);guild.revokeInvitation(chr.getId());
            broadcastMembership(chr.getWorld(),guild,charset,chr.getId(),13);Tms282GuildApplications.joined(chr,applications,charset);
        } }
    }
    public static void leave(Char chr,int characterId,String name,Charset charset) throws SQLException {
        requireEnabled();
        synchronized(Tms282VMatrixRuntime.ECONOMY_MONITOR) { synchronized(chr) {
            access(chr);Guild guild=requireGuild(chr);
            if(characterId!=chr.getId() || !name.equals(chr.getName()) || guild.getLeaderID()==chr.getId())throw new IllegalArgumentException("會長需要先移交會長職位。");
            chr.saveToSQL();JOURNAL.leave(chr,guild);
            var packet=Tms282GuildPackets.departure(false,guild.getId(),chr.getId(),chr.getName(),charset);
            chr.write(packet);guild.broadcast(packet);chr.write(Tms282GuildPackets.noGuild(chr.getId()));Tms282GuildSkills.sendPoints(chr);
            broadcastMembership(chr.getWorld(),guild,charset,0,1);
        } }
    }
    public static void manageMember(Char actor,int targetId,int rank,boolean kick,Charset charset) throws SQLException {
        requireEnabled();
        synchronized(Tms282VMatrixRuntime.ECONOMY_MONITOR) {
            access(actor);Guild guild=requireGuild(actor);GuildMember manager=guild.getMemberByCharID(actor.getId());
            GuildMember member=guild.getMemberByCharID(targetId);
            if(!guild.hasPermission(actor.getId(),kick?16:4) || member==null || member.getCharID()==actor.getId()
                    || member.getCharID()==guild.getLeaderID() || member.getRank()<=manager.getRank()
                    || !kick&&(rank<=manager.getRank()||rank>10))throw new IllegalArgumentException("沒有管理該成員的權限。");
            Char target=actor.getWorld().getCharById(targetId);
            if(target==null) {
                String targetName=member.getName();
                guild=JOURNAL.manageOffline(actor,targetId,guild.getId(),rank,kick);
                if(kick)guild.broadcast(Tms282GuildPackets.departure(true,guild.getId(),targetId,targetName,charset));
                broadcastMembership(actor.getWorld(),guild,charset,0,1);return;
            }
            synchronized(target) {
                access(target);target.saveToSQL();
                if(kick) {
                    JOURNAL.leave(target,guild);
                    var packet=Tms282GuildPackets.departure(true,guild.getId(),target.getId(),target.getName(),charset);
                    target.write(packet);guild.broadcast(packet);target.write(Tms282GuildPackets.noGuild(target.getId()));Tms282GuildSkills.sendPoints(target);
                } else JOURNAL.rank(target,guild,rank);
                broadcastMembership(actor.getWorld(),guild,charset,0,1);
            }
        }
    }
    /** Native333B2C3/333B30E: level10 and explicit temporary-job exclusions. */
    public static boolean eligibleLeader(int job,int level) {
        return level>=10&&!Set.of(13000,13100,13001,13500,12005,12006,12100,12200).contains(job);
    }
    public static void transferLeadership(Char actor,int targetId,Charset charset) throws SQLException {
        requireEnabled();
        synchronized(Tms282VMatrixRuntime.ECONOMY_MONITOR) {
            access(actor);Guild guild=requireGuild(actor);GuildMember target=guild.getMemberByCharID(targetId);
            if(guild.getLeaderID()!=actor.getId()||target==null||targetId==actor.getId()||target.getRank()==1)
                throw new IllegalArgumentException("Only the current guild master may transfer leadership to another member");
            if(guild.getAllianceID()!=0)throw new IllegalArgumentException("Alliance leadership transfer is not implemented; guild roles were not changed");
            Char online=actor.getWorld().getCharById(targetId);
            if(online!=null)access(online);
            if(!eligibleLeader(online==null?target.getJob():online.getJob(),online==null?target.getLevel():online.getLevel()))
                throw new IllegalArgumentException("This member cannot become guild master");
            int former=actor.getId();guild=JOURNAL.transferLeadership(actor,guild,targetId);
            for(GuildMember member:List.copyOf(guild.getMembers())) {
                Char chr=actor.getWorld().getCharById(member.getCharID());if(chr==null)continue;
                chr.write(Tms282GuildPackets.leadership(guild.getId(),former,targetId,chr.getId()==targetId));
                sendMembership(chr,1,charset);
            }
        }
    }
    public static void sendMembership(Char chr,int result,Charset charset) {
        Guild guild=chr.getGuild();
        if(guild==null)chr.write(Tms282GuildPackets.noGuild(chr.getId()));
        else chr.write(Tms282GuildPackets.membership(result,0,chr.getId(),snapshot(guild,chr.getWorld()),charset));
        Tms282GuildSkills.sendPoints(chr);
    }
    private static void broadcastMembership(net.swordie.ms.world.World world,Guild guild,Charset charset,int newcomer,int result) {
        for(GuildMember member:List.copyOf(guild.getMembers())) {
            Char online=world.getCharById(member.getCharID());
            if(online!=null && member.isOnline())sendMembership(online,online.getId()==newcomer?result:1,charset);
        }
    }
    private static void requireEnabled() { if(!enabled())throw new IllegalArgumentException("公會核心尚未啟用。"); }
    private static Guild requireGuild(Char chr) {
        Guild guild=chr.getGuild();if(guild==null || guild.getMemberByCharID(chr.getId())==null)throw new IllegalArgumentException("尚未加入公會。");return guild;
    }
    private static void access(Char chr) {
        Tms282VMatrixRuntime.requireEconomicAccess(chr);
        if(Tms282GuildJournal.isBlocked(chr.getId()) || chr.getTradeRoom()!=null || chr.getMiniRoom()!=null)throw new IllegalArgumentException("目前無法進行公會操作。");
    }
    public static Data snapshot(Guild guild) { return snapshot(guild,null); }
    private static Data snapshot(Guild guild,net.swordie.ms.world.World world) {
        var grades=new ArrayList<Grade>();for(int rank=1;rank<=10;rank++)grades.add(new Grade(text(guild.gradeName(rank)),guild.gradePermission(rank)));
        var applicants=Tms282GuildApplications.applicants(guild,world);
        var members=guild.getMembers().stream().map(m->new Member(m.getCharID(),text(m.getName()),m.getJob(),m.getLevel(),m.getRank(),
                m.getAllianceRank(),Tms282GuildSkills.joinedAt(guild,m.getCharID())==0?0:net.swordie.ms.util.FileTime.fromLong(Tms282GuildSkills.joinedAt(guild,m.getCharID())).toLong(),m.isOnline(),0,m.getContribution(),m.getDayContribution(),
                m.getContributionIncTime()==null?0:m.getContributionIncTime().toLong(),m.getIGP(),0)).toList();
        var skills=Tms282GuildSkills.presentation(guild);
        // Missing optional history/season/crest metadata has a native empty representation; no castle is advertised.
        return new Data(guild.getId(),guild.getName(),grades,applicants,members,guild.getMaxMembers(),guild.getMarkBg(),guild.getMarkBgColor(),
                guild.getMark(),guild.getMarkColor(),text(guild.getNotice()),guild.getHonorEXP(),guild.getAllianceID(),guild.getLevel(),
                guild.getRank(),guild.getGgp(),guild.getBattleSp(),guild.isAppliable(),0,0,guild.getJoinSetting(),guild.getReqLevel(),0,
                skills,0,new byte[0],0,0,0,0,false);
    }
    private static String text(String value) { return value==null?"":value; }
}
