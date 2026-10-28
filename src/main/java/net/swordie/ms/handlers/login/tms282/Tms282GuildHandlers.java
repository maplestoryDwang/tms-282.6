package net.swordie.ms.handlers.login.tms282;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.packet.Tms282GuildRequest;
import net.swordie.ms.connection.packet.Tms282GuildPackets;
import net.swordie.ms.connection.packet.Tms282StatPackets;
import net.swordie.ms.handlers.header.InHeader;
import java.nio.charset.Charset;
import java.sql.SQLException;
import net.swordie.ms.client.social.Guild.Tms282GuildRuntime;
import net.swordie.ms.client.social.Guild.Tms282GuildApplications;

/**
 * Native guild core: name/confirm, invitation/info/join, leave, online kick/rank, notice and invitation decline.
 * Unsupported selectors remain rejected/recorded; 282 selectors never pass to the incompatible 265 handler.
 * Core mutations require the guild runtime flag, durable journal migration and shared save/recovery hooks.
 */
final class Tms282GuildHandlers {
    private final Charset charset;
    Tms282GuildHandlers() { this(Tms282LoginSettings.defaults().nameCharset()); }
    Tms282GuildHandlers(Tms282LoginContext context) { this(context.settings().nameCharset()); }
    private Tms282GuildHandlers(Charset charset) { this.charset = charset; }
    void register(Tms282Routes routes) {
        routes.channel(Tms282GuildRequest.GUILD_REQUEST, InHeader.GUILD_REQUEST).when(Tms282Guards.IN_FIELD)
                .handle("native guild core and durable membership/notice persistence", this::record);
        routes.channel(Tms282GuildRequest.GUILD_RESULT, InHeader.GUILD_RESULT).when(Tms282Guards.IN_FIELD)
                .handle("native guild invitation decline; other prompt answers recorded", this::record);
    }

    private void record(Tms282LoginState session, int op, byte[] packet) throws SQLException {
        Char chr = session.fieldCharacter();
        try {
            Tms282GuildRequest request = Tms282GuildRequest.parse(packet, op);
            if(op==Tms282GuildRequest.GUILD_RESULT && Tms282GuildRuntime.enabled() && request.type()>=48 && request.type()<=50) {
                String[] names=request.decline(charset);Tms282GuildRuntime.decline(chr,names[0],names[1]);return;
            }
            if(op==Tms282GuildRequest.GUILD_REQUEST && Tms282GuildRuntime.enabled() && request.type()!=19) {
                if(request.type()==47) {
                    chr.write(net.swordie.ms.client.social.Guild.Tms282GuildSearch.search(chr,
                            net.swordie.ms.connection.packet.Tms282GuildSearchPackets.parse(request,charset),charset));return;
                }
                if(Tms282GuildApplications.enabled() && (request.type()==6||request.type()==7)) {
                    Tms282GuildApplications.review(chr,request.applicationTargets(),request.type()==6,charset);return;
                }
                var action=request.action(charset);
                switch(action.type()) {
                    case 1 -> Tms282GuildRuntime.prepareCreate(chr,action.text(),charset);
                    case 2 -> Tms282GuildRuntime.create(chr,charset);
                    case 3 -> { if(Tms282GuildApplications.enabled())Tms282GuildApplications.apply(chr,action.targetId(),action.text(),charset);
                                else Tms282GuildRuntime.joinInvited(chr,action.targetId(),charset); }
                    case 4 -> Tms282GuildApplications.withdraw(chr,action.targetId(),charset);
                    case 34 -> { if(!Tms282GuildApplications.enabled())throw new IllegalArgumentException("Guild application list not enabled");Tms282GuildApplications.restore(chr,charset); }
                    case 8 -> Tms282GuildRuntime.leave(chr,action.targetId(),action.text(),charset);
                    case 9 -> Tms282GuildRuntime.manageMember(chr,action.targetId(),0,true,charset);
                    case 12 -> Tms282GuildRuntime.manageMember(chr,action.targetId(),Integer.parseInt(action.text()),false,charset);
                    case 32 -> Tms282GuildRuntime.transferLeadership(chr,action.targetId(),charset);
                    case 33 -> Tms282GuildRuntime.information(chr,action.targetId(),charset);
                    case 35 -> Tms282GuildRuntime.invite(chr,action.text(),charset);
                    case 40 -> net.swordie.ms.client.social.Guild.Tms282GuildSkills.learn(chr,action.targetId(),Integer.parseInt(action.text()),charset);
                    case 42 -> Tms282GuildRuntime.cancelCreate(chr);
                    default -> throw new IllegalArgumentException("公會操作尚未移植。");
                }
                return;
            }
            if (op == Tms282GuildRequest.GUILD_REQUEST && request.type() == 19) {
                String notice = request.notice(charset);
                var guild = chr.getGuild();
                if (guild == null) throw new IllegalArgumentException("Character is not in a guild");
                synchronized (guild) {
                    if (!guild.hasPermission(chr.getId(), 2)) throw new IllegalArgumentException("Guild notice permission required");
                    try {
                        guild.saveNotice(notice);
                    } catch (SQLException failure) {
                        System.err.printf("[TMS282] Guild notice persistence failed guild=%d character=%d%n", guild.getId(), chr.getId());
                        return;
                    }
                    guild.broadcast(Tms282GuildPackets.notice(guild.getId(), chr.getId(), notice, charset));
                }
                return;
            }
            System.out.printf("[TMS282] REPORT %s character=%d (282 guild not ported)%n", request, chr.getId());
        } catch (SQLException e) {
            chr.chatMessage("公會資料尚未完成保存，請重新登入以復原。");
            System.err.printf("[TMS282] Guild operation failed character=%d state=%s%n",chr.getId(),e.getSQLState());
            throw e;
        } catch (IllegalArgumentException e) {
            chr.chatMessage(e.getMessage());
            System.out.printf("[TMS282] REJECTED CP%04X len=%d: %s%n", op, packet.length, e.getMessage());
        } finally {
            session.send(Tms282StatPackets.unlock());
        }
    }
}
