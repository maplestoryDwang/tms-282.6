package net.swordie.ms.client.character.commands;

import net.swordie.ms.Server;
import net.swordie.ms.ServerConstants;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.social.Alliance.AllianceResult;
import net.swordie.ms.client.social.Guild.GuildResult;
import net.swordie.ms.client.social.Party.PartyBoss;
import net.swordie.ms.client.social.Party.PartyResult;
import net.swordie.ms.connection.packet.*;
import net.swordie.ms.constants.FieldConstants;
import net.swordie.ms.constants.ItemConstants;
import net.swordie.ms.enums.*;
import net.swordie.ms.life.drop.DropInfo;
import net.swordie.ms.life.mob.Mob;
import net.swordie.ms.loaders.StringData;
import net.swordie.ms.scripts.ScriptType;
import net.swordie.ms.util.Rect;
import net.swordie.ms.util.Util;
import net.swordie.ms.util.container.Tuple;
import net.swordie.ms.world.event.InGameEventManager;
import net.swordie.ms.world.field.Field;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static net.swordie.ms.enums.AccountType.Player;
import static net.swordie.ms.enums.ChatType.SpeakerChannel;

public class PlayerCommands {

    @Command(names = {"help"}, requiredType = Player)
    public static class Help extends PlayerCommand {

        public static void execute(Char chr, String[] args) {
            if (args.length > 1) {
                return;
            }
            chr.chatMessage("___________________________________");
            chr.chatMessage("伺服器 " + ServerConstants.SERVER_NAME + " 的可用命令：");
            chr.chatMessage("@dispose: 修復卡住/異常狀態。");
            chr.chatMessage("@event: 參加正在進行的活動。");
            chr.chatMessage("@outfit（或 @搭配）: 時裝搭配共享：分享、預覽、缺件、一鍵購買與穿上。");
            chr.chatMessage("@check: 查看你或其他玩家的資訊，格式 @check <名稱>。");
            chr.chatMessage("@sell: 出售你的物品。");
            chr.chatMessage("@checkboss: 查看BOSS剩餘冷卻時間。");
            chr.chatMessage("@checkmob: 查看附近怪物或BOSS資訊。");
            chr.chatMessage("@home: 如果你有快速移動按鈕，將你傳送到弓箭手村。");
            chr.chatMessage("@ssb: 查看驚喜造型箱中的道具。");
            chr.chatMessage("@end: 儲存並返回登入畫面。");
            chr.chatMessage("___________________________________");
            chr.getScriptManager().dispose();
            if (chr.getHP() <= 0) {
                chr.openUIOnDead();
            }
            chr.write(WvsContext.updateVMatrix(chr, false, 0, 0));
        }
    }

    /** 時裝搭配共享：分享外觀、預覽、缺件、一鍵購買與穿上（net.swordie.ms.world.outfit）。 */
    @Command(names = {"outfit", "搭配"}, requiredType = Player)
    public static class OutfitShareCommand extends PlayerCommand {
        public static void execute(Char chr, String[] args) {
            chr.getScriptManager().startScript(chr, 0, "outfit_share", ScriptType.Content);
        }
    }

    @Command(names = {"checkmob", "checkmonster"}, requiredType = Player)
    public static class MobInfo extends PlayerCommand {

        public static void execute(Char chr, String[] args) {
            Rect rect = new Rect(
                    chr.getPosition().deepCopy().getX() - 500,
                    chr.getPosition().deepCopy().getY() - 500,
                    chr.getPosition().deepCopy().getX() + 500,
                    chr.getPosition().deepCopy().getY() + 500
            );
            Set<Mob> mobs = chr.getField().getMobs().stream().filter(m -> rect.hasPositionInside(m.getPosition())).collect(Collectors.toSet());
            if (mobs.isEmpty()) {
                chr.chatMessage(SpeakerChannel, "附近找不到任何怪物。");
            } else {
                int i = 1;
                for (Mob mob : mobs) {
                    if (mob != null && i < 5) {
                        String msg = String.format(i + ". ID: %d | 怪物名稱: %s | HP: %s/%s | 經驗值: %s。",
                                mob.getTemplateId(),
                                StringData.getMobStringById(mob.getTemplateId()),
                                Util.getNumberFormat(mob.getHp()),
                                Util.getNumberFormat(mob.getMaxHp()),
                                Util.getNumberFormat(mob.getExp())
                        );
                        chr.chatMessage(SpeakerChannel, msg);
                        String msg2 = "可掉落道具：";
                        for (DropInfo d : mob.getDrops()) {
                            msg2 += StringData.getItemStringById(d.getItemID()) + ", ";
                        }
                        chr.chatMessage(SpeakerChannel, msg2);
                        i++;
                    }
                }
                chr.chatMessage("總數量：" + chr.getField().getMobs().size());
            }
        }
    }

    @Command(names = {"afk"}, requiredType = Player)
    public static class AFK extends PlayerCommand {

        public static void execute(Char chr, String[] args) {
            chr.getScriptManager().startScript(chr, 0, "idle_hunting", ScriptType.Content);
        }
    }

    @Command(names = {"home"}, requiredType = Player)
    public static class Home extends PlayerCommand {

        public static void execute(Char chr, String[] args) {
            if (chr.getInstance() == null) {
                chr.warp(FieldConstants.HENESYS_ID, 0);
            }
        }
    }

    @Command(names = {"ssb"}, requiredType = Player)
    public static class SSB extends PlayerCommand {

        public static void execute(Char chr, String[] args) {
            List<Integer> ssb = new ArrayList<>();
            ssb.addAll(Arrays.asList(ItemConstants.SSB_Common));
            ssb.addAll(Arrays.asList(ItemConstants.SSB_Rare));
            String s = "";
            for (int i : ssb) {
                s += "#i" + i + "#,";
            }
            chr.getScriptManager().sendSayOkay("#r#e高級驚喜造型箱#n#k：\r\n" + s);
        }
    }

    @Command(names = {"check"}, requiredType = Player)
    public static class Check extends PlayerCommand {

        public static void execute(Char chr, String[] args) {
            if (args.length > 1) {
                String target = args[1];
                boolean bool = false;
                for (Char targetChar : chr.getField().getChars()) {
                    if (target.equalsIgnoreCase(targetChar.getName())) {
                        chr.write(FieldPacket.characterInfo(targetChar));
                        bool = true;
                        break;
                    }
                }
                if (!bool) {
                    chr.chatMessage("找不到名稱為 " + target + " 的角色。");
                }
                return;
            }
            chr.getScriptManager().startScript(chr, 0, "check_stats", ScriptType.Content);
        }
    }

    @Command(names = {"dispose"}, requiredType = Player)
    public static class Dispose extends PlayerCommand {

        public static void execute(Char chr, String[] args) {
            if (chr.getHP() <= 0) {
                chr.openUIOnDead();
            }
            chr.write(WvsContext.partyResult(PartyResult.load(chr.getParty())));
            if (chr.getGuild() != null) {
                chr.write(WvsContext.guildResult(GuildResult.response_GuildLoad_Success(chr.getGuild())));
                if (chr.getGuild().getAlliance() != null) {
                    chr.write(WvsContext.allianceResult(AllianceResult.loadDone(chr.getGuild().getAlliance())));
                    chr.write(WvsContext.allianceResult(AllianceResult.loadGuildDone(chr.getGuild().getAlliance())));
                }
            }
            chr.getScriptManager().dispose();
            chr.getScriptManager().getScripts().clear();
            chr.getScriptManager().lockInGameUI(false); // so players don't get stuck if a script fails

            final Field toField = chr.getField();
            //chr.warp(toField, toField.getPortalByName("sp"), false, false);
            chr.dispose();
            chr.chatMessage("[通知] 你的角色已處理成功！");
        }
    }

    @Command(names = {"sell", "quicksell"}, requiredType = Player)
    public static class SellItem extends PlayerCommand {

        public static void execute(Char chr, String[] args) {
            if (args.length > 1) {
                return;
            }
            chr.getScriptManager().startScript(chr, 0, "inv-seller", ScriptType.Content);
        }
    }

    @Command(names = {"logout", "end"}, requiredType = Player)
    public static class Logout extends PlayerCommand {

        public static void execute(Char chr, String[] args) {
            Server.get().getSwitchChars().put(chr.getUser().getId(), new Tuple<>(0, chr.getClient().getChannel()));
            chr.write(WvsContext.returnToTitle());
        }
    }

    @Command(names = {"boss", "checkboss"}, requiredType = Player)
    public static class CheckCDBoss extends PlayerCommand {
        public static void execute(Char chr, String[] args) {
            if (args.length > 1) {
                return;
            }
            final LocalDateTime now = LocalDateTime.now();
            String s = "#fs12#";
            VIPGrade vipGrade = VIPGrade.getValByNum(chr.getUser().getVipGrade());
            int count;
            for (PartyBoss partyBoss : chr.getPartyboss()) {
                if (partyBoss != null) {
                    BossPartyType bossPartyType = BossPartyType.getByOrderIdAndDifficulty(partyBoss.getOrderId(), partyBoss.getDifficulty());
                    if (bossPartyType != null) {
                        switch (bossPartyType.getEnterCount()) {
                            case OnceADay: {
                                // MVP Specials:
                                count = 0;
                                if (vipGrade != null) {
                                    if (vipGrade.getVal() == VIPGrade.Gold.getVal()) {
                                        count += 1;
                                    } else if (vipGrade.getVal() == VIPGrade.Diamond.getVal()) {
                                        count += 2;
                                    }
                                }
                                if (partyBoss.getAttempt() <= count) {
                                    s += String.format("- #e%s | %s:#n #gAvailable#k\r\n",
                                            partyBoss.getBossName(), bossPartyType.getDifficulty().getName());
                                } else {
                                    LocalDateTime start = partyBoss.getLastAttemptTime().toLocalDateTime();
                                    LocalDateTime end = start.plusDays(1);
                                    end = end.toLocalDate().atTime(0, 0, 0);
                                    final Duration diff = Duration.between(now, end);
                                    if (!end.isBefore(now) && diff.toSecondsPart() > 0) {
                                        s += getTimeLeft(diff, partyBoss, bossPartyType);
                                    } else {
                                        s += String.format("- #e%s | %s:#n #gAvailable#k\r\n",
                                                partyBoss.getBossName(), bossPartyType.getDifficulty().getName());
                                    }
                                }
                                break;
                            }
                            case TwiceAday: {
                                // MVP Specials:
                                count = 1;
                                if (vipGrade != null) {
                                    if (vipGrade.getVal() == VIPGrade.Gold.getVal()) {
                                        count += 1;
                                    } else if (vipGrade.getVal() == VIPGrade.Diamond.getVal()) {
                                        count += 2;
                                    }
                                }
                                if (partyBoss.getAttempt() <= count) {
                                    s += String.format("- #e%s | %s:#n #gAvailable#k\r\n",
                                            partyBoss.getBossName(), bossPartyType.getDifficulty().getName());
                                } else {
                                    LocalDateTime start = partyBoss.getLastAttemptTime().toLocalDateTime();
                                    LocalDateTime end = start.plusDays(1);
                                    end = end.toLocalDate().atTime(0, 0, 0);
                                    final Duration diff = Duration.between(now, end);
                                    if (!end.isBefore(now) && diff.toSecondsPart() > 0) {
                                        s += getTimeLeft(diff, partyBoss, bossPartyType);
                                    } else {
                                        s += String.format("- #e%s | %s:#n #gAvailable#k\r\n",
                                                partyBoss.getBossName(), bossPartyType.getDifficulty().getName());
                                    }
                                }
                                break;
                            }
                            case ThreeTimesAday: {
                                // MVP Specials:
                                count = 2;
                                if (vipGrade != null) {
                                    if (vipGrade.getVal() == VIPGrade.Gold.getVal()) {
                                        count += 1;
                                    } else if (vipGrade.getVal() == VIPGrade.Diamond.getVal()) {
                                        count += 2;
                                    }
                                }
                                if (partyBoss.getAttempt() <= count) {
                                    s += String.format("- #e%s | %s:#n #gAvailable#k\r\n",
                                            partyBoss.getBossName(), bossPartyType.getDifficulty().getName());
                                } else {
                                    LocalDateTime start = partyBoss.getLastAttemptTime().toLocalDateTime();
                                    LocalDateTime end = start.plusDays(1);
                                    end = end.toLocalDate().atTime(0, 0, 0);
                                    final Duration diff = Duration.between(now, end);
                                    if (!end.isBefore(now) && diff.toSecondsPart() > 0) {
                                        s += getTimeLeft(diff, partyBoss, bossPartyType);
                                    } else {
                                        s += String.format("- #e%s | %s:#n #gAvailable#k\r\n",
                                                partyBoss.getBossName(), bossPartyType.getDifficulty().getName());
                                    }
                                }
                                break;
                            }
                            case SevenTimesADay: {
                                // MVP Specials:
                                count = 6;
                                if (vipGrade != null) {
                                    if (vipGrade.getVal() == VIPGrade.Gold.getVal()) {
                                        count += 1;
                                    } else if (vipGrade.getVal() == VIPGrade.Diamond.getVal()) {
                                        count += 2;
                                    }
                                }
                                if (partyBoss.getAttempt() <= count) {
                                    s += String.format("- #e%s | %s:#n #gAvailable#k\r\n",
                                            partyBoss.getBossName(), bossPartyType.getDifficulty().getName());
                                } else {
                                    LocalDateTime start = partyBoss.getLastAttemptTime().toLocalDateTime();
                                    LocalDateTime end = start.plusDays(1);
                                    end = end.toLocalDate().atTime(0, 0, 0);
                                    final Duration diff = Duration.between(now, end);
                                    if (!end.isBefore(now) && diff.toSecondsPart() > 0) {
                                        s += getTimeLeft(diff, partyBoss, bossPartyType);
                                    } else {
                                        s += String.format("- #e%s | %s:#n #gAvailable#k\r\n",
                                                partyBoss.getBossName(), bossPartyType.getDifficulty().getName());
                                    }
                                }
                                break;
                            }
                            case TenTimesADay: {
                                // MVP Specials:
                                count = 9;
                                if (vipGrade != null) {
                                    if (vipGrade.getVal() == VIPGrade.Gold.getVal()) {
                                        count += 1;
                                    } else if (vipGrade.getVal() == VIPGrade.Diamond.getVal()) {
                                        count += 2;
                                    }
                                }
                                if (partyBoss.getAttempt() <= count) {
                                    s += String.format("- #e%s | %s:#n #gAvailable#k\r\n",
                                            partyBoss.getBossName(), bossPartyType.getDifficulty().getName());
                                } else {
                                    LocalDateTime start = partyBoss.getLastAttemptTime().toLocalDateTime();
                                    LocalDateTime end = start.plusDays(1);
                                    end = end.toLocalDate().atTime(0, 0, 0);
                                    final Duration diff = Duration.between(now, end);
                                    if (!end.isBefore(now) && diff.toSecondsPart() > 0) {
                                        s += getTimeLeft(diff, partyBoss, bossPartyType);
                                    } else {
                                        s += String.format("- #e%s | %s:#n #gAvailable#k\r\n",
                                                partyBoss.getBossName(), bossPartyType.getDifficulty().getName());
                                    }
                                }
                                break;
                            }
                            case OnceInSevendays: {
                                // MVP Specials:
                                count = 0;
                                if (vipGrade != null) {
                                    if (vipGrade.getVal() == VIPGrade.Gold.getVal()) {
                                        count += 1;
                                    } else if (vipGrade.getVal() == VIPGrade.Diamond.getVal()) {
                                        count += 2;
                                    }
                                }
                                if (partyBoss.getAttempt() <= count) {
                                    s += String.format("- #e%s | %s:#n #gAvailable#k\r\n",
                                            partyBoss.getBossName(), bossPartyType.getDifficulty().getName());
                                } else {
                                    LocalDateTime start = partyBoss.getLastAttemptTime().toLocalDateTime();
                                    LocalDateTime end = start.plusDays(7);
                                    end = end.toLocalDate().atTime(0, 0, 0);
                                    final Duration diff = Duration.between(now, end);
                                    if (!end.isBefore(now) && diff.toSecondsPart() > 0) {
                                        s += getTimeLeft(diff, partyBoss, bossPartyType);
                                    } else {
                                        s += String.format("- #e%s | %s:#n #gAvailable#k\r\n",
                                                partyBoss.getBossName(), bossPartyType.getDifficulty().getName());
                                    }
                                }
                                break;
                            }
                            case TwiceInSevendays: {
                                // MVP Specials:
                                count = 1;
                                if (vipGrade != null) {
                                    if (vipGrade.getVal() == VIPGrade.Gold.getVal()) {
                                        count += 1;
                                    } else if (vipGrade.getVal() == VIPGrade.Diamond.getVal()) {
                                        count += 2;
                                    }
                                }
                                if (partyBoss.getAttempt() <= count) {
                                    s += String.format("- #e%s | %s:#n #gAvailable#k\r\n",
                                            partyBoss.getBossName(), bossPartyType.getDifficulty().getName());
                                } else {
                                    LocalDateTime start = partyBoss.getLastAttemptTime().toLocalDateTime();
                                    LocalDateTime end = start.plusDays(7);
                                    end = end.toLocalDate().atTime(0, 0, 0);
                                    final Duration diff = Duration.between(now, end);
                                    if (!end.isBefore(now) && diff.toSecondsPart() > 0) {
                                        s += getTimeLeft(diff, partyBoss, bossPartyType);
                                    } else {
                                        s += String.format("- #e%s | %s:#n #gAvailable#k\r\n",
                                                partyBoss.getBossName(), bossPartyType.getDifficulty().getName());
                                    }
                                }
                                break;
                            }
                            case OnceInTwodays: {
                                // MVP Specials:
                                count = 0;
                                if (vipGrade != null) {
                                    if (vipGrade.getVal() == VIPGrade.Gold.getVal()) {
                                        count += 1;
                                    } else if (vipGrade.getVal() == VIPGrade.Diamond.getVal()) {
                                        count += 2;
                                    }
                                }
                                if (partyBoss.getAttempt() <= count) {
                                    s += String.format("- #e%s | %s:#n #gAvailable#k\r\n",
                                            partyBoss.getBossName(), bossPartyType.getDifficulty().getName());
                                } else {
                                    LocalDateTime start = partyBoss.getLastAttemptTime().toLocalDateTime();
                                    LocalDateTime end = start.plusDays(2);
                                    end = end.toLocalDate().atTime(0, 0, 0);
                                    final Duration diff = Duration.between(now, end);
                                    if (!end.isBefore(now) && diff.toSecondsPart() > 0) {
                                        s += getTimeLeft(diff, partyBoss, bossPartyType);
                                    } else {
                                        s += String.format("- #e%s | %s:#n #gAvailable#k\r\n",
                                                partyBoss.getBossName(), bossPartyType.getDifficulty().getName());
                                    }
                                }
                                break;
                            }
                            case OnceInThreedays: {
                                // MVP Specials:
                                count = 0;
                                if (vipGrade != null) {
                                    if (vipGrade.getVal() == VIPGrade.Gold.getVal()) {
                                        count += 1;
                                    } else if (vipGrade.getVal() == VIPGrade.Diamond.getVal()) {
                                        count += 2;
                                    }
                                }
                                if (partyBoss.getAttempt() <= count) {
                                    s += String.format("- #e%s | %s:#n #gAvailable#k\r\n",
                                            partyBoss.getBossName(), bossPartyType.getDifficulty().getName());
                                } else {
                                    LocalDateTime start = partyBoss.getLastAttemptTime().toLocalDateTime();
                                    LocalDateTime end = start.plusDays(3);
                                    end = end.toLocalDate().atTime(0, 0, 0);
                                    final Duration diff = Duration.between(now, end);
                                    if (!end.isBefore(now) && diff.toSecondsPart() > 0) {
                                        s += getTimeLeft(diff, partyBoss, bossPartyType);
                                    } else {
                                        s += String.format("- #e%s | %s:#n #gAvailable#k\r\n",
                                                partyBoss.getBossName(), bossPartyType.getDifficulty().getName());
                                    }
                                }
                                break;
                            }
                        }
                    }
                }
            }
            chr.getScriptManager().sendOK(s, 9010000);
        }
    }

    private static String getTimeLeft(Duration diff, PartyBoss partyBoss, BossPartyType bossPartyType) {
        String s = "";
        if (diff.toDays() > 0) {
            s += String.format("- #e%s | %s:#n #r%d days %02d hrs %02d mins %02d sec#k\r\n",
                    partyBoss.getBossName(), bossPartyType.getDifficulty().getName(), diff.toDays(), diff.toHoursPart(), diff.toMinutesPart(), diff.toSecondsPart());
        } else if (diff.toHoursPart() > 0) {
            s += String.format("- #e%s | %s:#n #r%02d hrs %02d mins %2d sec#k\r\n",
                    partyBoss.getBossName(), bossPartyType.getDifficulty().getName(), diff.toHoursPart(), diff.toMinutesPart(), diff.toSecondsPart());
        } else if (diff.toMinutesPart() > 0) {
            s += String.format("- #e%s | %s:#n #r%02d mins %2d sec#k\r\n",
                    partyBoss.getBossName(), bossPartyType.getDifficulty().getName(), diff.toMinutesPart(), diff.toSecondsPart());
        } else if (diff.toSecondsPart() > 0) {
            s += String.format("- #e%s | %s:#n #r%02d sec#k\r\n",
                    partyBoss.getBossName(), bossPartyType.getDifficulty().getName(), diff.toSecondsPart());
        }
        return s;
    }

    @Command(names = {"joinevent", "event"}, requiredType = Player)
    public static class JoinEvent extends PlayerCommand {

        public static void execute(Char chr, String[] args) {
            if (args.length > 1) {
                return;
            }
            InGameEventManager inGameEventManager = InGameEventManager.getInstance();
            if (inGameEventManager.getOpenEvent() != null && !inGameEventManager.getOpenEvent().isActive()) {
                inGameEventManager.joinPublicEvent(chr);
            } else {
                chr.chatMessage("無法參加活動。");
            }
        }
    }
}
