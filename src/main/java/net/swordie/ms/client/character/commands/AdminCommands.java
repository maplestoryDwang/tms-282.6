package net.swordie.ms.client.character.commands;

import net.swordie.ms.DiscordAPI;
import net.swordie.ms.Server;
import net.swordie.ms.ServerConfig;
import net.swordie.ms.client.Account;
import net.swordie.ms.client.Client;
import net.swordie.ms.client.User;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.PortableChair;
import net.swordie.ms.client.character.items.Equip;
import net.swordie.ms.client.character.items.Item;
import net.swordie.ms.client.character.items.ItemOption;
import net.swordie.ms.client.character.quest.Quest;
import net.swordie.ms.client.character.runestones.RuneStone;
import net.swordie.ms.client.character.skills.ForceAtom;
import net.swordie.ms.client.character.skills.Option;
import net.swordie.ms.client.character.skills.Skill;
import net.swordie.ms.client.character.skills.StolenSkill;
import net.swordie.ms.client.character.skills.info.ForceAtomInfo;
import net.swordie.ms.client.character.skills.info.SkillInfo;
import net.swordie.ms.client.character.skills.matrix.MatrixCore;
import net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat;
import net.swordie.ms.client.character.skills.temp.TemporaryStatBase;
import net.swordie.ms.client.character.skills.temp.TemporaryStatManager;
import net.swordie.ms.client.jobs.nova.Kaiser;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.packet.*;
import net.swordie.ms.constants.EventConstants;
import net.swordie.ms.constants.GameConstants;
import net.swordie.ms.constants.ItemConstants;
import net.swordie.ms.constants.JobConstants.JobEnum;
import net.swordie.ms.enums.*;
import net.swordie.ms.handlers.header.OutHeader;
import net.swordie.ms.life.mob.Mob;
import net.swordie.ms.life.mob.MobStat;
import net.swordie.ms.life.mob.MobTemporaryStat;
import net.swordie.ms.life.mob.skill.SpiderWeb;
import net.swordie.ms.life.npc.Npc;
import net.swordie.ms.loaders.*;
import net.swordie.ms.loaders.containerclasses.SkillStringInfo;
import net.swordie.ms.scripts.ScriptType;
import net.swordie.ms.util.FileTime;
import net.swordie.ms.util.Position;
import net.swordie.ms.util.Rect;
import net.swordie.ms.util.Util;
import net.swordie.ms.world.Channel;
import net.swordie.ms.world.event.InGameEventManager;
import net.swordie.ms.world.field.ClockPacket;
import net.swordie.ms.world.field.Field;
import net.swordie.ms.world.field.Portal;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static net.swordie.ms.ServerConstants.*;
import static net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat.*;
import static net.swordie.ms.enums.AccountType.*;
import static net.swordie.ms.enums.ChatType.*;
import static net.swordie.ms.enums.InventoryOperation.Add;
import static net.swordie.ms.enums.MessageType.QUEST_RECORD_MESSAGE;

public class AdminCommands {

    @Command(names = {"info"}, requiredType = Admin)
    public static class Dispose extends AdminCommand {
        public static void execute(Char chr, String[] args) {
            Map<BaseStat, Integer> basicStats = chr.getTotalBasicStats();
            StringBuilder sb = new StringBuilder();
            List<BaseStat> sortedList = Arrays.stream(BaseStat.values()).sorted(Comparator.comparing(Enum::toString)).toList();
            for (BaseStat bs : sortedList) {
                sb.append(String.format("%s = %d, ", bs, basicStats.getOrDefault(bs, 0)));
            }
            chr.chatMessage(Mob, String.format("地圖ID=%d, X=%d, Y=%d, 狀態: %s", chr.getField().getId(), chr.getPosition().getX(), chr.getPosition().getY(), sb));
        }
    }

    @Command(names = {"notice"}, requiredType = GameMaster)
    public static class Notice extends AdminCommand {

        public static void execute(Char chr, String[] args) {
            String text = "";
            for (String i : args) {
                text += i + " ";
            }
            text = text.substring(8);
            for (Char x : Server.get().getWorld().getChars()) {
                x.chatMessage(Notice2, "[公告]: " + text);
            }
            DiscordAPI.send("general-chat", text, DiscordAPI.mainGuildServer);
        }
    }

    @Command(names = {"die"}, requiredType = Admin)
    public static class Die extends AdminCommand {

        public static void execute(Char chr, String[] args) {
            String name = args[1];
            Char x = chr.getField().getCharByName(name);
            x.heal(-chr.getMaxHP());
            x.die();
        }
    }

    @Command(names = {"rune"}, requiredType = Admin)
    public static class Rune extends AdminCommand {

        public static void execute(Char chr, String[] args) {
            RuneStone runeStone = new RuneStone().getRuneStoneByType(Integer.parseInt(args[1]), chr.getField());
            runeStone.activateRuneStoneEffect(chr);
        }
    }

    @Command(names = {"test"}, requiredType = Admin)
    public static class Test extends AdminCommand {

        public static void execute(Char chr, String[] args) {
            OutPacket outPacket = new OutPacket(1568);
            outPacket.encodeByte(false);
            outPacket.encodeInt(1);
            outPacket.encodeString("concac");
            outPacket.encodeInt(0);
            chr.write(outPacket);
        }
    }

    @Command(names = {"mo"}, requiredType = Admin)
    public static class Mo extends AdminCommand {

        public static void execute(Char chr, String[] args) {
            if (args.length < 2) {
                chr.chatMessage("語法: !mo <類型> <層數> <時間> <層數2>");
                return;
            }
            OutPacket outPacket = new OutPacket(OutHeader.EXTRA_SYSTEM_RESULT);
            outPacket.encodeInt(-1289454273);
            outPacket.encodeShort(34);
            outPacket.encodeByte(Integer.parseInt(args[1])); // type
            outPacket.encodeInt(Integer.parseInt(args[2]));
            outPacket.encodeInt(3000);
            outPacket.encodeInt(1);
            outPacket.encodeByte(true);
            outPacket.encodeShort(1866);
            outPacket.encodeByte(Integer.parseInt(args[3]));
            outPacket.encodeByte(0);
            outPacket.encodeByte(0);
            outPacket.encodeByte(Util.getRandom(-100, 100));
            chr.write(outPacket);
        }
    }

    @Command(names = {"bot"}, requiredType = Admin)
    public static class Bot extends AdminCommand {

        public static void execute(Char chr, String[] args) {
            int charID = Integer.parseInt(args[1]);
            int itemID = Integer.parseInt(args[2]);
            Char bot = Char.getCharDataByID(charID);
            bot.loadCharacterData();
            bot.loadCharacterGuildData();
            bot.setAccount(Account.getAccountFromSQLByAccountID(chr.getAccId()));
            bot.setUser(User.getUserFromSQLByID(1));
            bot.setMoveAction(chr.getMoveAction());
            bot.setFoothold(chr.getFoothold());
            bot.setPosition(chr.getPosition());
            if (itemID != 0) {
                bot.setChair(new PortableChair(bot, itemID, ChairType.NormalChair));
            } else if (itemID == 0 && chr.getChair().getItemID() != 0) {
                bot.setChair(chr.getChair());
            }
            bot.setBot(true);
            bot.setField(chr.getField());
            chr.getField().getChars().add(bot);
            chr.getField().broadcast(UserPool.userEnterField(bot));
        }
    }

    @Command(names = {"hilla"}, requiredType = Admin)
    public static class hilla extends AdminCommand {

        public static void execute(Char chr, String[] args) {
            chr.write(FieldPacket.clock(ClockPacket.secondsClock(1800)));
            chr.setDeathCount(5);
            chr.setVHDeathCount(new boolean[]{true, true, true, true, true});
            chr.showDeathCount(5);
            for (int i = 8880405; i <= 8880407; i++) {
                chr.getScriptManager().spawnMob(i, 0, 266, false);
            }
        }
    }

    @Command(names = {"will"}, requiredType = Admin)
    public static class summonWill extends AdminCommand {

        public static void execute(Char chr, String[] args) {
            chr.write(FieldPacket.clock(ClockPacket.secondsClock(1800)));
            chr.setDeathCount(10);
            chr.showDeathCount(chr.getDeathCount());
            chr.write(WillPacket.setMoonGauge(100, 45));
            chr.getTimer().addFixedRateEvent(() -> chr.write(WillPacket.addMoonGauge(10)), 0, 1000, false);
            SpiderWeb.load();
            int stage = chr.getFieldID() == 450008150 ? 1 : chr.getFieldID() == 450008250 ? 3 : 5;
            if (stage == 1) {
                chr.getScriptManager().spawnMob(8880303, 352, 0, false);
                chr.getScriptManager().spawnMob(8880304, 352, -2020, false);
                chr.getScriptManager().spawnMob(8880300, 352, -2020, false);
                chr.getScriptManager().spawnMob(8880321, 352, 0, false);
                chr.getScriptManager().spawnMob(8880322, 352, -2020, false);
                chr.getScriptManager().spawnMob(8880325, 252, 0, false);
                chr.getScriptManager().spawnMob(8880326, 252, -2020, false);
            } else if (stage == 3) {
                chr.getScriptManager().spawnMob(8880301, 0, 215, false);
                chr.getScriptManager().spawnMob(8880323, 352, 215, false);
                chr.getScriptManager().spawnMob(8880327, 252, 215, false);
            } else if (stage == 5) {
                for (int i = 0; i < 35; i++) {
                    chr.getField().spawnLife(new SpiderWeb(i), null);
                }
                chr.getScriptManager().spawnMob(8880302, -4, 25, false);
                chr.getScriptManager().spawnMob(8880324, 352, 281, false);
                chr.getScriptManager().spawnMob(8880328, 252, 281, false);
            }
        }
    }

    @Command(names = {"resetData"}, requiredType = Admin)
    public static class resetData extends AdminCommand {

        public static void execute(Char chr, String[] args) {
            for (Client c : Server.get().getClients()) {
                c.close();
            }
            for (Channel ch : Server.get().getWorld().getChannels()) {
                ch.getFields().clear();
            }
            FieldData.load();
        }
    }

    @Command(names = {"testbuff"}, requiredType = Admin)
    public static class TestBuff extends AdminCommand {

        public static void execute(Char chr, String[] args) {
            TemporaryStatManager tsm = chr.getTemporaryStatManager();
            Option o1 = new Option();
            o1.nOption = 1;
            o1.rOption = 41141004;
            tsm.sendStat(Unk841, o1);
        }
    }

    @Command(names = {"shutdown"}, requiredType = Admin)
    public static class ShutDownServer extends AdminCommand {

        public static void execute(Char chr, String[] args) {
            if (args.length < 1) {
                chr.chatMessage("語法: !shutdown [<時間> / NOW]");
                return;
            }
            ServerConfig.ADMIN_LOGIN = !ServerConfig.ADMIN_LOGIN;
            String strTime = "";
            int time = 60000; // default == 1 minute
            if (args[1].equalsIgnoreCase("now")) {
                time = 1;
            } else {
                time *= Integer.parseInt(args[1]);
            }

            if (time > 1) {
                int seconds = (time / 1000) % 60;
                int minutes = (time / (1000 * 60)) % 60;
                int hours = (time / (1000 * 60 * 60)) % 24;
                int days = (time / (1000 * 60 * 60 * 24));
                if (days > 0) {
                    strTime += days + " 天 ";
                }
                if (hours > 0) {
                    strTime += hours + " 小時 ";
                }
                if (seconds > 0) {
                    if (minutes > 0) {
                        strTime += minutes + " 分 ";
                    }
                    strTime += seconds + " 秒";
                } else {
                    strTime += minutes + " 分鐘";
                }
            }
            String msg = SERVER_NAME + " 即將進行維護，將於 " + strTime + " 後關閉伺服器，請盡快安全登出遊戲。";
            chr.sendPopupSay(msg);
            for (Client c : Server.get().getClients()) {
                Char player = c.getChr();
                if (player != null) {
                    if (player.getField() != null) {
                        player.chatMessage(ChatType.AdminChat, msg);
                        player.sendPopupSay(msg);
                        player.getTimer().addEvent(c::close, time, TimeUnit.MILLISECONDS);
                    } else {
                        c.close();
                    }
                } else {
                    c.close();
                }
            }
            chr.getTimer().addEvent(() -> {
                System.exit(0);
                DiscordAPI.send("lệnh", "Thành công tắt máy chủ.", DiscordAPI.staffGuildServer);
            }, time + 5000L, TimeUnit.MILLISECONDS);
        }
    }

    @Command(names = {"packet"}, requiredType = Admin)
    public static class TestPacket extends AdminCommand {

        public static void execute(Char chr, String[] args) {
            if (args.length < 3) {
                chr.chatMessage("用法: !packet <op> <資料>");
                return;
            }
            OutPacket outPacket = new OutPacket(Short.parseShort(args[1]));
            StringBuilder data = new StringBuilder();
            for (int i = 2; i < args.length; i++) {
                data.append(" ").append(args[i]);
            }
            outPacket.encodeArr(data.toString());
            chr.write(outPacket);

        }
    }

    @Command(names = {"testmobstat"}, requiredType = Admin)
    public static class TestMobStat extends AdminCommand {

        public static void execute(Char chr, String[] args) {
            for (Mob mob : chr.getField().getMobs()) {
                Option o = new Option();
                o.nOption = 1;
                o.rOption = 3221014;
                o.tOption = 1000;
                mob.getTemporaryStat().addStatOptions(mob, MobStat.Stun, o);
            }
        }
    }

    @Command(names = {"warpto"}, requiredType = Admin)
    public static class WarpTo extends AdminCommand {

        public static void execute(Char chr, String[] args) {
            if (args.length > 1 && Util.isValidString(args[1])) {
                Char player = Server.get().getWorld().getCharByName(args[1]);
                if (player != null) {
                    if (player.getClient().getChannel() == chr.getClient().getChannel()) {
                        chr.warp(player.getField());
                    } else {
                        chr.changeChannelAndWarp(player.getClient().getChannel(), player.getFieldID());
                    }
                } else {
                    chr.chatMessage(Notice2, "找不到地圖 ID " + args[1]);
                }
            } else {
                chr.chatMessage("請輸入正確的角色名稱。");
            }
        }
    }

    @Command(names = {"warpHere"}, requiredType = Admin)
    public static class WarpHere extends AdminCommand {

        public static void execute(Char chr, String[] args) {
            if (args.length > 1 && Util.isValidString(args[1])) {
                Char player = Server.get().getWorld().getCharByName(args[1]);
                if (player != null) {
                    if (player.getClient().getChannel() == chr.getClient().getChannel()) {
                        player.warp(chr.getField());
                    } else {
                        player.changeChannelAndWarp(chr.getClient().getChannel(), chr.getFieldID());
                    }
                } else {
                    chr.chatMessage(Notice2, "找不到地圖 ID " + args[1]);
                }
            } else {
                chr.chatMessage("請輸入正確的角色名稱。");
            }
        }
    }

    @Command(names = {"atom"}, requiredType = Admin)
    public static class Atom extends AdminCommand {

        public static void execute(Char chr, String[] args) {
            int charID = chr.getId();
            ForceAtomInfo forceAtomInfo = new ForceAtomInfo(142110011, ForceAtomEnum.KINESIS_ORB_REAL.getInc(), 3, 3, 0, 0, (int) System.currentTimeMillis(), 1,
                    142110011, new Position());

            Mob mob = (Mob) chr.getField().getLifes().get(chr.getField().getLifes().size() - 1);
            List<Integer> mobs = new ArrayList<>();
            int mobID = mob.getObjectId();
            mobs.add(mobID);

            ForceAtomEnum fae = ForceAtomEnum.GUIDED_ARROW;
            ForceAtom fa = new ForceAtom(false, 0, chr.getId(), fae,
                    true, 0, 142110011, forceAtomInfo, null, 0, 300,
                    new Position(), 142110011, new Position(), 0);
            chr.getField().broadcast(FieldPacket.createForceAtom(fa));

        }
    }

    @Command(names = {"sendQRvalue", "qr"}, requiredType = Admin)
    public static class SendQRValue extends AdminCommand {

        public static void execute(Char chr, String[] args) {
            if (args.length < 3 || !Util.isNumber(args[1])) {
                chr.chatMessage("用法: !qr <任務ID> <QR值>");
                return;
            }
            int questId = Integer.parseInt(args[1]);
            chr.createQuestWithQRValue(questId, args[2]);
        }
    }

    @Command(names = {"reloadcs"}, requiredType = Admin)
    public static class ReloadCS extends AdminCommand {

        public static void execute(Char chr, String[] args) {
            //Server.getInstance().initCashShop();
        }
    }

    @Command(names = {"roll"}, requiredType = Admin)
    public static class OneArmedBandit extends AdminCommand {

        public static void execute(Char chr, String[] args) {

            String[] str = new String[]{
                    "Map/Effect.img/miro/frame",
                    "Map/Effect.img/miro/RR1/" + Util.getRandom(4),
                    "Map/Effect.img/miro/RR2/" + Util.getRandom(4),
                    "Map/Effect.img/miro/RR3/" + Util.getRandom(4)
            };

            for (String s : str) {
                chr.write(UserPacket.effect(Effect.effectFromWZ(s)));
            }
        }
    }

    @Command(names = {"openUI"}, requiredType = Admin)
    public static class openUI extends AdminCommand {
        public static void execute(Char chr, String[] args) {
            int id = Integer.valueOf(args[1]);
            chr.write(FieldPacket.openUI(id));
        }
    }

    @Command(names = {"closeUI"}, requiredType = Admin)
    public static class closeUI extends AdminCommand {
        public static void execute(Char chr, String[] args) {
            int id = Integer.valueOf(args[1]);
            chr.write(FieldPacket.closeUI(id));
        }
    }

    @Command(names = {"jobV"}, requiredType = Admin)
    public static class jobV extends AdminCommand {
        public static void execute(Char chr, String[] args) {
            chr.completeQuest(1465);
        }
    }

    @Command(names = {"jobVI"}, requiredType = Admin)
    public static class jobVI extends AdminCommand {
        public static void execute(Char chr, String[] args) {
            chr.completeQuest(1488);
            OutPacket outPacket = new OutPacket(OutHeader.MESSAGE);
            outPacket.encodeByte(QUEST_RECORD_MESSAGE.getVal());
            outPacket.encodeInt(1488);
            outPacket.encodeByte(2);
            outPacket.encodeFT(FileTime.currentTime());
            chr.write(outPacket);
        }
    }

    @Command(names = {"openCustomCore"}, requiredType = Admin)
    public static class openCustomCore extends AdminCommand {
        public static void execute(Char chr, String[] args) {
            if (args.length < 5) {
                chr.chatMessage(Notice2, "參數不足！<核心ID> <技能ID1> <技能ID2> <技能ID3> <crc>");
                return;
            }
            int coreID = Integer.valueOf(args[1]);
            int skillID1 = Integer.valueOf(args[2]);
            int skillID2 = Integer.valueOf(args[3]);
            int skillID3 = Integer.valueOf(args[4]);
            MatrixCore core = new MatrixCore(chr.getId(), coreID, skillID1, skillID2, skillID3);
            chr.addMatrixCore(core);
            chr.write(WvsContext.nodeStoneResult(core));
            chr.write(WvsContext.updateVMatrix(chr, false, 0, 0));
        }
    }

    @Command(names = {"dc"}, requiredType = Admin)
    public static class disconnectPlayer extends AdminCommand {

        public static void execute(Char chr, String[] args) {
            Char player = Server.get().getWorld().getCharByName(args[1]);
            if (player != null) {
                player.getClient().close();
                player.getClient().write(SecurityPacket.sendGameKick("由於偵測到可疑的遊戲或用戶端行為，你已被遊戲安全系統踢出。"));
            }
        }
    }

    @Command(names = {"clearcd"}, requiredType = Admin)
    public static class clearCDSkills extends AdminCommand {
        public static void execute(Char chr, String[] args) {
            for (Integer skillID : chr.getSkillCoolTimes().keySet()) {
                chr.addSkillCoolTime(skillID, 0);
                chr.write(UserLocal.skillCooltimeSetM(skillID, 0));
            }
        }
    }

    @Command(names = {"debug"}, requiredType = Admin)
    public static class Debug extends AdminCommand {

        public static void execute(Char chr, String[] args) {
            if (ServerConfig.DEBUG_MODE) {
                ServerConfig.DEBUG_MODE = false;
                chr.chatMessage("伺服器除錯模式：關閉");
            } else {
                ServerConfig.DEBUG_MODE = true;
                chr.chatMessage("伺服器除錯模式：開啟");
            }
        }
    }

    @Command(names = {"forceevent"}, requiredType = GameMaster)
    public static class ForceEvent extends AdminCommand {

        public static void execute(Char chr, String[] args) {
            int id = Integer.parseInt(args[1]);
            InGameEventManager.getInstance().forceGMvent(id);
        }
    }

    @Command(names = {"spawn"}, requiredType = GameMaster)
    public static class Spawn extends AdminCommand {

        public static void execute(Char chr, String[] args) {
            int id = Integer.parseInt(args[1]);
            int count = 1;
            if (args.length > 2) {
                count = Integer.parseInt(args[2]);
            }
            int hp = 0;
            if (args.length > 3) {
                hp = Integer.parseInt(args[3]);
            }
            for (int i = 0; i < count; i++) {
                Mob mob = MobData.getMobDeepCopyById(id);
                if (mob == null) {
                    chr.chatMessage("找不到該 ID 的怪物。");
                    return;
                }
                Field field = chr.getField();
                Position pos = chr.getPosition();
                mob.setPosition(pos.deepCopy());
                mob.setPrevPos(pos.deepCopy());
                mob.setPosition(pos.deepCopy());
                mob.getForcedMobStat().setMaxMP(Integer.MAX_VALUE);
                if (hp > 0) {
                    mob.setMaxHp(hp);
                    mob.setHp(hp);
                }
                mob.setNotRespawnable(true);
                if (mob.getField() == null) {
                    mob.setField(field);
                }
                field.spawnLife(mob, null);
                //mob.spawnEliteBoss();
            }
        }
    }

    @Command(names = {"npc", "spawnnpc"}, requiredType = GameMaster)
    public static class NPC extends AdminCommand {

        public static void execute(Char chr, String[] args) {
            int id = Integer.parseInt(args[1]);
            Npc npc = NpcData.getNpcDeepCopyById(id);
            if (npc == null) {
                chr.chatMessage("找不到該 ID 的 NPC。");
                return;
            }
            Field field = chr.getField();
            Position pos = chr.getPosition();
            npc.setPosition(pos.deepCopy());
            npc.setCy(chr.getPosition().getY());
            npc.setRx0(chr.getPosition().getX() + 50);
            npc.setRx1(chr.getPosition().getX() - 50);
            npc.setFh(chr.getFoothold());
            npc.setNotRespawnable(true);
            if (npc.getField() == null) {
                npc.setField(field);
            }
            field.spawnLife(npc, null);
            System.out.println("npc has id " + npc.getObjectId());
        }
    }

    @Command(names = {"pnpc"}, requiredType = GameMaster)
    public static class PNPC extends AdminCommand {

        public static void execute(Char chr, String[] args) {
//            int id = Integer.parseInt(args[1]);
//            Npc npc = NpcData.getNpcDeepCopyById(id);
//            if (npc == null) {
//                chr.chatMessage("Could not find an npc with that ID.");
//                return;
//            }
//            Field field = chr.getField();
//            Position pos = chr.getPosition();
//            npc.setPosition(pos.deepCopy());
//            npc.setCy(chr.getPosition().getY());
//            npc.setRx0(chr.getPosition().getX() + 50);
//            npc.setRx1(chr.getPosition().getX() - 50);
//            npc.setFh(chr.getFoothold());
//            npc.setNotRespawnable(true);
//            if (npc.getField() == null) {
//                npc.setField(field);
//            }
//            field.spawnLife(npc, null);
//            System.out.println("npc has id " + npc.getObjectId());
//
//            Session session = DatabaseManager.getSession();
//            Transaction transaction = session.beginTransaction();
//
//            Query npcQuery = session.createNativeQuery("INSERT INTO npc (npcid,mapid,x,y,cy,rx0,rx1,fh) VALUES (:npcid,:mapid,:x,:y,:cy,:rx0,:rx1,:fh)");
//            npcQuery.setParameter("npcid", id);
//            npcQuery.setParameter("mapid", field.getId());
//            npcQuery.setParameter("x", pos.getX());
//            npcQuery.setParameter("y", pos.getY());
//            npcQuery.setParameter("cy", npc.getCy());
//            npcQuery.setParameter("rx0", npc.getRx0());
//            npcQuery.setParameter("rx1", npc.getRx1());
//            npcQuery.setParameter("fh", npc.getFh());
//
//            npcQuery.executeUpdate();
//
//            transaction.commit();
//            session.close();

        }
    }

    @Command(names = {"forcechase"}, requiredType = GameMaster)
    public static class ForceChase extends AdminCommand {

        public static void execute(Char chr, String[] args) {

            for (Mob m : chr.getField().getMobs()) {
                System.out.println("" + m.isUserControll());
            }

            for (Mob m : chr.getField().getMobs()) {
                //c.write(MobPool.damaged(mob.getObjectId(), dmg, mob.getTemplateId(), (byte) 1, (int) mob.getHp(), (int) mob.getMaxHp()));
                //chr.getField().broadcastPacket(MobPool.damaged(m.getObjectId(),(long)2000,m.getTemplateId(),(byte)1,(int)m.getHp(),(int)m.getMaxHp()));
                chr.getField().broadcast(MobPool.forceChase(m.getObjectId(), false));
            }

        }
    }

    @Command(names = {"setcontroller"}, requiredType = GameMaster)
    public static class SetController extends AdminCommand {

        public static void execute(Char chr, String[] args) {

            String chrName = args[1];

            Char newController = chr.getField().getCharByName(chrName);
            if (newController == null) {
                chr.chatMessage("找不到角色");
                return;
            }

            for (Mob m : chr.getField().getMobs()) {
                m.setControllerID(chr.getId());
                m.notifyControllerChange();
            }

        }
    }

    @Command(names = {"mobcontroller"}, requiredType = GameMaster)
    public static class MobController extends AdminCommand {

        public static void execute(Char chr, String[] args) {

            String chrName = args[1];

            for (Mob m : chr.getField().getMobs()) {
                Char controller = m.getField().getCharByID(m.getControllerID());
                if (controller != null) {
                    chr.chatMessage(m.getObjectId() + " : " + controller.getName());
                } else {
                    chr.chatMessage(m.getObjectId() + " : null");
                }
            }

        }
    }

    @Command(names = {"proitem"}, requiredType = GameMaster)
    public static class ProItem extends AdminCommand {

        public static void execute(Char chr, String[] args) {
            if (args.length < 5) {
                chr.chatMessage(Notice2, "參數不足！<ID> <屬性> <攻擊> <附加能力>");
                return;
            }
            int id = Integer.parseInt(args[1]);
            int stat = Integer.parseInt(args[2]);
            int atk = Integer.parseInt(args[3]);
            int flames = Integer.parseInt(args[4]);
            Equip equip = ItemData.getEquipDeepCopyFromID(id, false);
            equip.setBaseStat(EquipBaseStat.iStr, stat);
            equip.setBaseStat(EquipBaseStat.iDex, stat);
            equip.setBaseStat(EquipBaseStat.iInt, stat);
            equip.setBaseStat(EquipBaseStat.iLuk, stat);
            equip.setBaseStat(EquipBaseStat.iPAD, atk);
            equip.setBaseStat(EquipBaseStat.iMAD, atk);
            equip.setBaseStat(EquipBaseStat.bdr, flames);
            equip.setBaseStat(EquipBaseStat.imdr, flames);
            equip.setBaseStat(EquipBaseStat.damR, flames);
            equip.setBaseStat(EquipBaseStat.statR, flames);

            chr.addItemToInventory(InvType.EQUIP, equip, false, false);
            chr.getClient().write(WvsContext.inventoryOperation(true, false,
                    Add, (short) equip.getBagIndex(), (byte) 1,
                    0, equip));

        }
    }

    @Command(names = {"item"}, requiredType = GameMaster)
    public static class GetItem extends AdminCommand {

        public static void execute(Char chr, String[] args) {
            if (Util.isNumber(args[1])) {
                int id = Integer.parseInt(args[1]);
                short attr = Short.parseShort(args[2]);
                Equip equip = ItemData.getEquipDeepCopyFromID(id, true);
                if (equip == null) {
                    Item item = ItemData.getItemDeepCopy(id, true);
                    if (item == null) {
                        chr.chatMessage(Mob, String.format("找不到道具 ID %d", id));
                        return;
                    }
                    short quant = 1;
                    if (args.length > 2) {
                        quant = Short.parseShort(args[2]);
                    }
                    if (GameConstants.isIntensePowerCrystal(item.getItemId())) {
                        int partySize = 1;
                        long price = GameConstants.getBossRewardPrice(9210074);
                        long sellingPrice = price / partySize;
                        item.setBossRewardID(9210074);
                        item.setPartySize(partySize);
                        item.setPrice(sellingPrice);
                        item.setDateExpire(FileTime.fromDate(LocalDateTime.now().plusDays(7)));
                        chr.write(WvsContext.setBossReward(chr));
                    }
                    item.setQuantity(quant);
                    chr.addItemToInventory(item);
                } else {
                    short quant = 1;
                    if (args.length > 2) {
                        quant = Short.parseShort(args[2]);
                    }
                    if (id == 1012632) {
                        equip.addExceptionalSlot((byte) 1);
                        equip.getExceptionalStat().setSTR(15);
                        equip.getExceptionalStat().setDEX(15);
                        equip.getExceptionalStat().setINT(15);
                        equip.getExceptionalStat().setLUK(15);
                        equip.getExceptionalStat().setPAD(10);
                        equip.getExceptionalStat().setMAD(10);
                        equip.getExceptionalStat().setHP(750);
                        equip.getExceptionalStat().setMP(750);
                    }
                    if (ItemConstants.isSymbol(id)) {
                        chr.getScriptManager().giveSymbol(id, quant);
                    } else {
                        chr.addItemToInventory(InvType.EQUIP, equip, false, false);
                    }

                }
            } else {
                StringBuilder query = new StringBuilder();
                int size = args.length;
                short quant = 1;
                if (Util.isNumber(args[size - 1])) {
                    size--;
                    quant = Short.parseShort(args[size]);
                }
                for (int i = 1; i < size; i++) {
                    query.append(args[i].toLowerCase()).append(" ");
                }
                query = new StringBuilder(query.substring(0, query.length() - 1));
                Map<Integer, String> map = StringData.getItemStringByName(query.toString());
                if (map.size() == 0) {
                    chr.chatMessage(Mob, "查無符合的道具：" + query);
                }
                for (Map.Entry<Integer, String> entry : map.entrySet()) {
                    int id = entry.getKey();
                    Item item = ItemData.getEquipDeepCopyFromID(id, true);
                    if (item != null) {
                        Equip equip = (Equip) item;
                        if (equip.getItemId() < 1000000) {
                            continue;
                        }
                        chr.addItemToInventory(equip);
                        chr.getClient().write(WvsContext.inventoryOperation(true, false,
                                Add, (short) equip.getBagIndex(), (byte) -1, 0, equip));
                        return;
                    }
                    item = ItemData.getItemDeepCopy(id);
                    if (item == null) {
                        continue;
                    }
                    item.setQuantity(quant);
                    chr.addItemToInventory(item);
                    chr.getClient().write(WvsContext.inventoryOperation(true, false,
                            Add, (short) item.getBagIndex(), (byte) -1, 0, item));
                    return;
                }
            }
        }
    }

    @Command(names = {"exitem"}, requiredType = GameMaster)
    public static class GetExItem extends AdminCommand {

        public static void execute(Char chr, String[] args) {
            if (Util.isNumber(args[1])) {
                int id = Integer.parseInt(args[1]);
                Equip equip = ItemData.getEquipDeepCopyFromID(id, true);
                if (equip != null) {
                    chr.addItemToInventory(id, 1, "day", Integer.parseInt(args[2]));
                }
            }
        }
    }

    @Command(names = {"mesos", "money"}, requiredType = GameMaster)
    public static class Mesos extends AdminCommand {

        public static void execute(Char chr, String[] args) {
            long mesos = Long.parseLong(args[1]);
            chr.addMoney(mesos);
        }
    }

    @Command(names = {"ld", "liedetector"}, requiredType = GameMaster)
    public static class LD extends AdminCommand {

        public static void execute(Char chr, String[] args) {
            if (args.length < 1) {
                chr.chatMessage(SpeakerChannel, "參數不足！請用 !ld <名稱> 或 !ld @me 測試。");
                return;
            }

            String name = args[1];
            Char chrToLD = chr;

            if (!name.equals("@me")) {
                chrToLD = Server.get().getWorld().getCharByName(name);

                if (chrToLD == null) {
                    chr.chatMessage(SpeakerChannel, String.format("角色「%s」不在線上。", name));
                    return;
                }
            }

            if (chrToLD.sendLieDetector()) {
                net.swordie.ms.world.tms282.audit.AuditLog.antiMacro(chrToLD, chr.getId(), 0, chrToLD.getLieDetectorAnswer(), null, null, "REQUEST"); // audit
                chr.chatMessage(SpeakerChannel, String.format("已對「%s」發送測謊機。", chrToLD.getName()));
            } else {
                chr.chatMessage(SpeakerChannel, "測謊機發送失敗。");
            }
        }
    }

    @Command(names = {"ban"}, requiredType = GameMaster)
    public static class Ban extends AdminCommand {

        public static void execute(Char chr, String[] args) {
            if (args.length < 5) {
                chr.chatMessage(SpeakerChannel, "參數不足！請用 !ban <名稱> <數量> <min/hour/day/year> <原因>");
                return;
            }
            String name = args[1];
            int amount = Integer.parseInt(args[2]);
            String amountType = args[3].toLowerCase();
            StringBuilder builder = new StringBuilder();
            for (int i = 4; i < args.length; i++) {
                builder.append(args[i] + " ");
            }
            String reason = builder.toString();
            reason = reason.substring(0, reason.length() - 1); // gets rid of the last space
            if (reason.length() > 255) {
                chr.chatMessage(SpeakerChannel, "封鎖原因太長。");
                return;
            }
            Char banChr = Server.get().getWorld().getCharByName(name);
            boolean online = true;
            if (banChr == null) {
                online = false;
                //banChr = Char.getFromDBByName(name);
                if (banChr == null) {
                    chr.chatMessage(SpeakerChannel, "找不到該角色。");
                    return;
                }
            }
            User banUser = banChr.getUser();
            LocalDateTime banDate = LocalDateTime.now();
            switch (amountType) {
                case "m":
                case "min":
                case "mins":
                    banDate = banDate.plusMinutes(amount);
                    break;
                case "h":
                case "hour":
                case "hours":
                    banDate = banDate.plusHours(amount);
                    break;
                case "d":
                case "day":
                case "days":
                    banDate = banDate.plusDays(amount);
                    break;
                case "y":
                case "year":
                case "years":
                    banDate = banDate.plusYears(amount);
                    break;
                default:
                    chr.chatMessage(SpeakerChannel, String.format("未知的時間單位 %s", amountType));
                    break;
            }
            banUser.setBanExpireDate(FileTime.fromDate(banDate));
            banUser.setBanReason(reason);
            chr.chatMessage(SpeakerChannel, String.format("角色 %s 已被封鎖。到期日：%s", name, banDate));
            if (online) {
                banChr.getClient().close();
            }
        }
    }

    @Command(names = {"killmobs"}, requiredType = GameMaster)
    public static class KillMobs extends AdminCommand {

        public static void execute(Char chr, String[] args) {
            List<Mob> mobs = new ArrayList<>(chr.getField().getMobs());
            for (Mob mob : mobs) {
                if (mob.getHp() > 0) {
                    mob.damage(chr, Long.MAX_VALUE, 0);
                }
            }
        }
    }

    @Command(names = {"showinvinfo", "invinfo"}, requiredType = GameMaster)
    public static class ShowInvInfo extends AdminCommand {

        public static void execute(Char chr, String[] args) {

            chr.chatMessage(Mob, "------------------------------------------------------------");
            for (InvType invType : InvType.values()) {
                chr.chatMessage(Mob, invType.toString());
                for (Item item : chr.getInventoryByType(invType).getItems()) {
                    item.setInvType(invType);
                    String name = StringData.getItemStringById(item.getItemId());
                    chr.chatMessage(Mob, String.format("%s, %d, %d, %d, %s", name, item.getItemId(), item.getId(),
                            item.getBagIndex(), item.getInvType().toString()));
                }
            }
        }
    }

    @Command(names = {"checkid", "getid", "charid"}, requiredType = GameMaster)
    public static class CheckID extends AdminCommand {

        public static void execute(Char chr, String[] args) {
            chr.chatMessage(SpeakerChannel, "你的角色ID = " + chr.getId() + " \r\n你的帳號ID = " + chr.getAccId());
        }
    }

    @Command(names = {"getphantomstolenskills"}, requiredType = GameMaster)
    public static class GetPhantomStolenSkills extends AdminCommand {

        public static void execute(Char chr, String[] args) {
            chr.getStolenSkills().stream().sorted(Comparator.comparing(StolenSkill::getPosition))
                    .forEach(ss
                            -> chr.chatMessage(GroupFriend, "[竊取技能]  技能ID: " + ss.getSkillid() + " 位置: " + ss.getPosition() + " 目前等級: " + ss.getCurrentlv()));
        }
    }

    @Command(names = {"stealskilllist"}, requiredType = GameMaster)
    public static class StealSkillList extends AdminCommand {

        public static void execute(Char chr, String[] args) {
            Set<Skill> skillSet = new HashSet<>();

            //Warriors
            int[] skillIds = new int[]{
                    //Hero
                    1101006, //Rage
                    1101011, //Brandish
                    1101012, //Combo Fury
                    1101013, //Combo Attack

                    1111008, //Shout
                    1111012, //Rush
                    1111010, //Intrepid Slash
                    1111008, //Shout

                    1121008, //Raging Blow
                    1121016, //Magic Crash(Hero)

                    1121054, //Cry Valhalla

                    //Paladin
                    1201011, //Flame Charge
                    1201012, //Blizzard Charge
                    1201013, //Close Combat

                    1211013, //Threaten
                    1211014, //Parashock Guard
                    1211012, //Rush
                    1211011, //Combat order
                    1211010, //HP Recovery
                    1211008, //Lightning Charge

                    1221016, //Guardian
                    1221014, //Magic Crash(Paladin)
                    1221011, //Heaven's Hammer
                    1221009, //Blast

                    1221054, //Sacrosanctity

                    //Dark Knight
                    1301007, //Hyper body
                    1301006, //Iron will
                    1301012, //Spear Sweep
                    1301013, //Evil Eye

                    1311015, //Cross Surge
                    1311011, //La Mancha Spear,
                    1311012, //Rush

                    1321012, //Dark Impale
                    1321013, //Gungnir's Descent
                    1321014, //Magic Crash(Dark Knight)

                    1321054, //Dark Thirst

                    2001002, //Magic Guard
                    //Mage FP
                    2101010, //Ignite
                    2101005, //Poison breath
                    2101004, //Flame Orb
                    2101001, //Meditation(FP)

                    2111002, //Explosion
                    2111003, //Poison mist

                    2121011, //Flame Haze
                    2121007, //Meteor Shower
                    2121006, //Paralyze

                    2121054, //Inferno Aura

                    //MageIL
                    2201008, //Cold Beam
                    2201005, //Thunder Bolt
                    2201001, //Meditation(IL)

                    2211010, //Glacier Chain

                    2221007, //Blizzard
                    2221012, //Frozen Orb
                    2221006, //Chain Lightning

                    2221054, //Absolute Zero Aura

                    //Bishop
                    2301004, //Bless
                    2301005, //Holy Arrow
                    2301002, //Heal

                    2311001, //Dispel
                    2311003, //Holy Symbol
                    2311004, //Shining Ray
                    2311011, //Holy Fountain
                    2311009, //Holy Magic Shell

                    2321008, //Genesis
                    2321007, //Angel Ray
                    2321006, //Resurrection
                    2321005, //Adv Blessing

                    2321054, //Righteously Indignant

                    //Bowmaster
                    3101008, //Covering Fire
                    3101005, //Arrowbomb

                    3111011, //Reckless Hunt: Bow
                    3111010, //Hookshot
                    3111003, //Flame Surge
                    3111013, //Arrow Blaster

                    3121004, //Hurricane
                    3121015, //Arrow Stream
                    3121002, //Sharp Eyes
                    3121014, //Blinding Shot

                    3121054, //Concentration

                    //Marksman
                    3201008, //Net Toss

                    3211008, //Dragon Breath
                    3211009, //Explosive Bolt
                    3211010, //Hookshot
                    3211011, //Pain Killer
                    3211012, //Reckless Hunt: XBow

                    3221007, //Snipe
                    3221006, //Illusion Step
                    3221002, //Sharp Eyes
                    3221001, //Piercing Arrow

                    3221054, //BullsEye Shot

                    4001003, //Dark Sight
                    4001005, //Haste
                    //Night Lord
                    4101011, //Sin Mark
                    4101010, //Gust Charm
                    4101008, //Shuriken Burst

                    4111013, //Shade Splitter
                    4111015, //Shade Splitter
                    4111010, //Triple Throw
                    4111003, //Shadow Web

                    4121017, //Showdown
                    4121016, //Sudden Raid (NL)
                    4121015, //Frailty Curse
                    4121013, //Quad Star

                    4121054, //Bleed Dart

                    //Shadower
                    4201012, //Svg Blow
                    4201011, //Meso Guard
                    4201004, //Steal

                    4211011, //Midnight Carnival
                    4211006, //Meso Explosion
                    4211002, //Phase Dash

                    4221014, //Assassinate
                    4221010, //Sudden Raid(Shad)
                    4221007, //Bstep
                    4221006, //Smoke screen

                    4221054, //Flip of the Coin

                    //Dual Blade
                    4301003, //Self Haste

                    4311003, //Slash Storm
                    4311002, //Fatal Blow

                    4321006, //Flying Assaulter
                    4321004, //Upper Stab
                    4321002, //FlashBang

                    4331011, //Blade Ascension
                    4331006, //Chains of Hell

                    4341011, //Sudden Raid (DB)
                    4341009, //Phantom Blow
                    4341004, //Blade Fury
                    4341002, //Final Cut

                    4341054, //Blade Clone

                    5001005, //Dash
                    //Bucc
                    5101004, //Corkscrew Blow

                    5111007, //Roll of the Dice
                    5111006, //Shock wave
                    5111009, //Spiral Assault
                    5111015, //Static Thumper
                    5111012, //Static Thumper

                    5121013, //Nautilus Strike
                    5121010, //Time Leap
                    5121009, //Speed Infusion
                    5121020, //octopunch
                    5121015, //Crossbones

                    5121054, //Stimulating Conversation

                    //Corsair
                    5201012, //Scurvy Summons
                    5201011, //Wings
                    5201006, //Recoil Shot
                    5201001, //Rapid blast

                    5211007, //Roll of the Dice
                    5211011, //All Aboard
                    5211009, //Cross cut Blast
                    5211010, //Blackboot bill
                    5211014, //Octo Cannon

                    5221018, //Jolly Roger
                    5221015, //Parrotargetting
                    5221016, //Brain scrambler
                    5221013, //Nautilus Strike
                    5221017, //Eigh-legs Easton
                    5221022, //Broadside

                    5221054, //Whaler's potion

                    //Cannon Master
                    5011001, //Cannon Strike

                    5301003, //Monkey Magic
                    5301001, //Barrel Bomb
                    5301000, //Scatter Shot

                    5311004, //Barrel Roulette
                    5311003, //Cannon Jump
                    5311005, //Luck of the Die
                    5311010, //Monkey Fury
                    5311002, //Monkey Wave
                    5311000, //Cannon Spike

                    5321012, //Cannon Barrage
                    5321010, //Pirate Spirit
                    5321004, //Monkey Militia
                    5321003, //Anchor Aweigh
                    5321001, //Nautilus Strike
                    5321000, //Cannon Bazooka

                    5321054, //BuckShot
            };

            for (int skillId : skillIds) {
                Skill skill = SkillData.getSkillDeepCopyById(skillId);
                if (skill == null) {
                    continue;
                }
                skillSet.add(skill);
            }

            chr.write(UserLocal.resultStealSkillList(skillSet, 4, 1, 2412));
        }
    }

    @Command(names = {"np", "nearestportal"}, requiredType = GameMaster)
    public static class NP extends AdminCommand {

        public static void execute(Char chr, String[] args) {
            Rect rect = new Rect(
                    new Position(
                            chr.getPosition().deepCopy().getX() - 30,
                            chr.getPosition().deepCopy().getY() - 30),
                    new Position(
                            chr.getPosition().deepCopy().getX() + 30,
                            chr.getPosition().deepCopy().getY() + 30)
            );
            chr.chatMessage(Normal, "~~~~~~~~~~");
            chr.chatMessage(SpeakerChannel, "目前地圖: " + Util.getNumberFormat(chr.getFieldID()));
            chr.chatMessage(SpeakerChannel, "目前回城地圖: " + Util.getNumberFormat(chr.getField().getReturnMap()));
            chr.chatMessage(SpeakerChannel, "");
            for (Portal portal : chr.getField().getClosestPortal(rect)) {
                chr.chatMessage(SpeakerChannel, "傳送點名稱: " + portal.getName());
                chr.chatMessage(SpeakerChannel, "傳送點ID: " + Util.getNumberFormat(portal.getId()));
                chr.chatMessage(SpeakerChannel, "傳送點目標地圖: " + Util.getNumberFormat(portal.getTargetMapId()));
                chr.chatMessage(SpeakerChannel, "傳送點腳本: " + portal.getScript());
                chr.chatMessage(SpeakerChannel, ".");
            }
            chr.chatMessage(Normal, "~~~~~~~~~~");
        }
    }

    @Command(names = {"stats"}, requiredType = GameMaster)
    public static class Stats extends AdminCommand {

        public static void execute(Char chr, String[] args) {
            int strength = chr.getStat(Stat.str);
            int dexterity = chr.getStat(Stat.dex);
            int intellect = chr.getStat(Stat.inte);
            int luck = chr.getStat(Stat.luk);
            int hp = chr.getStat(Stat.hp);
            int mhp = chr.getStat(Stat.mhp);
            int mp = chr.getStat(Stat.mp);
            int mmp = chr.getStat(Stat.mmp);
            double hpratio = (((double) hp) / mhp) * 100;
            double mpratio = (((double) mp) / mmp) * 100;
            DecimalFormat formatNumbers = new DecimalFormat("##.00");
            NumberFormat addDeci = NumberFormat.getNumberInstance(Locale.US);
            chr.chatMessage(Notice2, "STR: " + addDeci.format(strength) + "  DEX: " + addDeci.format(dexterity) + "  INT: " + addDeci.format(intellect) + "  LUK: " + addDeci.format(luck));
            chr.chatMessage(Notice2, "HP: " + addDeci.format(hp) + " / " + addDeci.format(mhp) + " (" + formatNumbers.format(hpratio) + "%)   MP: " + addDeci.format(mp) + " / " + addDeci.format(mmp) + " (" + formatNumbers.format(mpratio) + "%)");
        }
    }

    @Command(names = {"testdrop"}, requiredType = GameMaster)
    public static class TestDrop extends AdminCommand {

        public static void execute(Char chr, String[] args) {
            int id = Integer.parseInt(args[1]);
            int count = 1;
            if (args.length > 2) {
                count = Integer.parseInt(args[2]);
            }
            for (int i = 0; i < count; i++) {
                Mob mob = MobData.getMobDeepCopyById(id);
                if (mob == null) {
                    chr.chatMessage("找不到該 ID 的怪物。");
                    return;
                }
                Field field = chr.getField();
                Position pos = chr.getPosition();
                mob.setPosition(pos.deepCopy());
                mob.setPrevPos(pos.deepCopy());
                mob.setPosition(pos.deepCopy());
                mob.getForcedMobStat().setMaxMP(3);
                mob.setMaxHp(3);
                mob.setHp(3);
                mob.setNotRespawnable(true);
                if (mob.getField() == null) {
                    mob.setField(field);
                }
                field.spawnLife(mob, null);

                System.out.println("Mob has id " + mob.getObjectId());
            }
        }
    }

    @Command(names = {"done"}, requiredType = GameMaster)
    public static class Done extends AdminCommand {

        public static void execute(Char chr, String[] args) {
            int num = 1000;
            int hp = 250000;
            int lv = 235;
            chr.setStatAndSendPacket(Stat.hp, hp);
            chr.setStatAndSendPacket(Stat.mhp, hp);
            chr.setStatAndSendPacket(Stat.mp, hp);
            chr.setStatAndSendPacket(Stat.mmp, hp);
            chr.setStatAndSendPacket(Stat.str, (short) num);
            chr.setStatAndSendPacket(Stat.dex, (short) num);
            chr.setStatAndSendPacket(Stat.inte, (short) num);
            chr.setStatAndSendPacket(Stat.luk, (short) num);
            chr.setStatAndSendPacket(Stat.level, (short) lv);
        }
    }

    @Command(names = {"hypertp"}, requiredType = GameMaster)
    public static class HyperTP extends AdminCommand {

        public static void execute(Char chr, String[] args) {
            int hyperTP = 5040004;
            Item hyperTP2 = ItemData.getItemDeepCopy(hyperTP);
            chr.addItemToInventory(hyperTP2.getInvType(), hyperTP2, false, false);
            chr.getClient().write(WvsContext.inventoryOperation(true, false,
                    Add, (short) hyperTP2.getBagIndex(), (byte) -1, 0, hyperTP2));
        }
    }

    @Command(names = {"job", "setjob"}, requiredType = GameMaster)
    public static class Job extends AdminCommand {

        public static void execute(Char chr, String[] args) {
            short id = Short.parseShort(args[1]);
            JobEnum job = JobEnum.getJobById(id);
            if (job != null) {
                chr.setJob(id);
                Map<Stat, Object> stats = new HashMap<>();
                stats.put(Stat.job, id);
                chr.sendStatsPacket(stats);
            }
        }
    }

    @Command(names = {"sp", "setsp"}, requiredType = GameMaster)
    public static class Sp extends AdminCommand {

        public static void execute(Char chr, String[] args) {
            int num = Integer.parseInt(args[1]);
            if (num >= 0) {
                chr.setSpToCurrentJob(num);
                Map<Stat, Object> stats = new HashMap<>();
                stats.put(Stat.sp, chr.getAvatarData().getCharacterStat().getExtendSP());
                chr.sendStatsPacket(stats);
            }
        }
    }

    @Command(names = {"ap", "setap"}, requiredType = GameMaster)
    public static class Ap extends AdminCommand {

        public static void execute(Char chr, String[] args) {
            int num = Integer.parseInt(args[1]);
            if (num >= 0) {
                chr.setStatAndSendPacket(Stat.ap, (short) num);
            }
        }
    }

    @Command(names = {"hp", "sethp"}, requiredType = GameMaster)
    public static class Hp extends AdminCommand {

        public static void execute(Char chr, String[] args) {
            int num = Integer.parseInt(args[1]);
            if (num >= 0) {
                chr.setStatAndSendPacket(Stat.hp, num);
                chr.setStatAndSendPacket(Stat.mhp, num);
            }
        }
    }

    @Command(names = {"mp", "setmp"}, requiredType = GameMaster)
    public static class Mp extends AdminCommand {

        public static void execute(Char chr, String[] args) {
            int num = Integer.parseInt(args[1]);
            if (num >= 0) {
                chr.setStatAndSendPacket(Stat.mp, num);
                chr.setStatAndSendPacket(Stat.mmp, num);
            }
        }
    }

    @Command(names = {"str", "setstr"}, requiredType = GameMaster)
    public static class Str extends AdminCommand {

        public static void execute(Char chr, String[] args) {
            int num = Integer.parseInt(args[1]);
            if (num >= 0) {
                chr.setStatAndSendPacket(Stat.str, (short) num);
            }
        }
    }

    @Command(names = {"dex", "setdex"}, requiredType = GameMaster)
    public static class Dex extends AdminCommand {

        public static void execute(Char chr, String[] args) {
            int num = Integer.parseInt(args[1]);
            if (num >= 0) {
                chr.setStatAndSendPacket(Stat.dex, (short) num);
            }
        }
    }

    @Command(names = {"int", "setint"}, requiredType = GameMaster)
    public static class SetInt extends AdminCommand {

        public static void execute(Char chr, String[] args) {
            int num = Integer.parseInt(args[1]);
            if (num >= 0) {
                chr.setStatAndSendPacket(Stat.inte, (short) num);
            }
        }
    }

    @Command(names = {"luk", "setluk"}, requiredType = GameMaster)
    public static class Luk extends AdminCommand {

        public static void execute(Char chr, String[] args) {
            int num = Integer.parseInt(args[1]);
            if (num >= 0) {
                chr.setStatAndSendPacket(Stat.luk, (short) num);
            }
        }
    }

    @Command(names = {"level", "setlevel", "lvl", "lv"}, requiredType = GameMaster)
    public static class Level extends AdminCommand {

        public static void execute(Char chr, String[] args) {
            int num = Integer.parseInt(args[1]);
            if (num >= 0) {
                chr.setStatAndSendPacket(Stat.level, (short) num);
                chr.setStatAndSendPacket(Stat.exp, 0);
                chr.getJobHandler().handleLevelUp((short) num);
                chr.getField().broadcast(UserRemote.effect(chr.getId(), Effect.levelUpEffect()));
            }
        }
    }

    @Command(names = {"leveluntil", "levelupuntil"}, requiredType = GameMaster)
    public static class LevelUntil extends AdminCommand {

        public static void execute(Char chr, String[] args) {
            int num = Integer.parseInt(args[1]);
            chr.getScriptManager().levelUntil(num);
        }
    }

    @Command(names = {"heal"}, requiredType = GameMaster)
    public static class Heal extends AdminCommand {

        public static void execute(Char chr, String[] args) {
            chr.heal(Integer.parseInt(args[1]), true);
        }
    }

    @Command(names = {"curhp"}, requiredType = GameMaster)
    public static class CurHp extends AdminCommand {

        public static void execute(Char chr, String[] args) {
            int num = Integer.parseInt(args[1]);
            if (num >= 0) {
                chr.setStatAndSendPacket(Stat.hp, num);
            }
        }
    }

    @Command(names = {"curmp"}, requiredType = GameMaster)
    public static class CurMp extends AdminCommand {

        public static void execute(Char chr, String[] args) {
            int num = Integer.parseInt(args[1]);
            if (num >= 0) {
                chr.setStatAndSendPacket(Stat.mp, num);
            }
        }
    }

    @Command(names = {"invincible"}, requiredType = GameMaster)
    public static class Invincible extends AdminCommand {

        public static void execute(Char chr, String[] args) {
            chr.setInvincible(!chr.isInvincible());
            chr.chatMessage("無敵: " + chr.isInvincible());
        }
    }

    @Command(names = {"morph"}, requiredType = GameMaster)
    public static class Morph extends AdminCommand {

        public static void execute(Char chr, String[] args) {
            int morphID = Integer.parseInt(args[1]);
            if (args.length < 2) {
                chr.chatMessage(Notice2, "參數不足！<ID>");
            }
            TemporaryStatManager tsm = chr.getTemporaryStatManager();
            Option o1 = new Option();
            o1.nOption = morphID;
            o1.rOption = Kaiser.FINAL_TRANCE;
            tsm.sendStat(Morph, o1);
        }
    }

    @Command(names = {"mount"}, requiredType = GameMaster)
    public static class Mount extends AdminCommand {

        public static void execute(Char chr, String[] args) {
            int mountID = Integer.parseInt(args[1]);
            if (args.length < 2) {
                chr.chatMessage(Notice2, "參數不足！<ID>");
            }
            TemporaryStatManager tsm = chr.getTemporaryStatManager();
            TemporaryStatBase tsb = tsm.getTSBByTSIndex(TSIndex.RideVehicle);
            tsb.setNOption(mountID);
            tsb.setROption(Kaiser.FINAL_TRANCE);
            tsm.sendStat(CharacterTemporaryStat.getByBitPos(Integer.parseInt(args[2])), tsb.getOption());
        }
    }

    @Command(names = {"setmap"}, requiredType = GameMaster)
    public static class SetMap extends AdminCommand {

        public static void execute(Char chr, String[] args) {
            if (args.length > 1 && Util.isNumber(args[1])) {
                Field toField = chr.getOrCreateFieldByCurrentInstanceType(Integer.parseInt(args[1]));
                if (toField != null) {
                    Portal portal = toField.getPortalByID(Integer.parseInt(args[2]));
                    chr.warp(toField, portal);
                } else {
                    chr.chatMessage(Notice2, "找不到地圖 ID " + args[1]);
                }
            } else {
                chr.chatMessage("第一個參數請輸入數字。");
            }
        }
    }

    @Command(names = {"setportal"}, requiredType = GameMaster)
    public static class SetPortal extends AdminCommand {

        public static void execute(Char chr, String[] args) {
            int portalID = Integer.parseInt(args[1]);
            Portal portal = chr.getField().getPortalByID(portalID);
            if (portal == null) {
                chr.chatMessage(Notice2, "傳送點不存在。");
                return;
            }
            Position position = new Position(portal.getX(), portal.getY());
            chr.write(FieldPacket.teleport(position, chr));
        }
    }

    @Command(names = {"getskill"}, requiredType = GameMaster)
    public static class GetSkill extends AdminCommand {

        public static void execute(Char chr, String[] args) {
            if (args.length < 4) {
                chr.chatMessage(Notice2, "參數不足！<ID> <目前> <最大>");
                return;
            }
            int id = Integer.parseInt(args[1]);
            int cur = Integer.parseInt(args[2]);
            int max = Integer.parseInt(args[3]);
            chr.addSkill(id, cur, max);
        }
    }

    @Command(names = {"maxskills"}, requiredType = GameMaster)
    public static class MaxSkills extends AdminCommand {

        public static void execute(Char chr, String[] args) {
            chr.maxSkills();
        }
    }

    @Command(names = {"lookup", "find"}, requiredType = GameMaster)
    public static class Lookup extends AdminCommand {

        public static void execute(Char chr, String[] args) {
            if (args.length < 3) {
                chr.chatMessage(Notice2, "參數不足！<查詢類型> <ID/(部分)名稱>");
                chr.chatMessage(Notice2, "可查詢類型：item, skill, mob, npc, map");
                return;
            }
            StringBuilder query = new StringBuilder();
            for (int i = 2; i < args.length; i++) {
                query.append(args[i].toLowerCase()).append(" ");
            }
            query = new StringBuilder(query.substring(0, query.length() - 1));
            chr.chatMessage("查詢: " + query);
            boolean isNumber = Util.isNumber(query.toString());
            if ("skill".equalsIgnoreCase(args[1])) {
                SkillStringInfo ssi;
                int id;
                if (isNumber) {
                    id = Integer.parseInt(query.toString());
                    ssi = StringData.getSkillStringById(id);
                    if (ssi == null) {
                        chr.chatMessage(Mob, "找不到技能 " + id);
                        return;
                    }
                    SkillInfo skillInfo = SkillData.getSkillInfoById(id);
                    chr.chatMessage(Mob, "名稱: " + ssi.getName());
                    chr.chatMessage(Mob, "說明: " + ssi.getDesc());
                    chr.chatMessage(Mob, "h: " + ssi.getH());
                    chr.chatMessage(Mob, "類型: " + skillInfo.getType());
                } else {
                    Map<Integer, SkillStringInfo> map = StringData.getSkillStringByName(query.toString());
                    if (map.size() == 0) {
                        chr.chatMessage(Mob, "查無符合的技能：" + query);
                    }
                    for (Map.Entry<Integer, SkillStringInfo> entry : map.entrySet()) {
                        id = entry.getKey();
                        ssi = entry.getValue();
                        SkillInfo si = SkillData.getSkillInfoById(id);
                        if (si != null) {
                            chr.chatMessage(Mob, "Id: " + id);
                            chr.chatMessage(Mob, "名稱: " + ssi.getName());
                            chr.chatMessage(Mob, "說明: " + ssi.getDesc());
                            chr.chatMessage(Mob, "h: " + ssi.getH());
                            chr.chatMessage(Mob, "類型: " + si.getType());
                        }
                    }
                }
            } else {
                String queryType = args[1].toLowerCase();
                int id;
                String name;
                if (isNumber) {
                    id = Integer.parseInt(query.toString());
                    switch (queryType) {
                        case "item":
                            name = StringData.getItemStringById(id);
                            break;
                        case "mob":
                            name = StringData.getMobStringById(id);
                            break;
                        case "npc":
                            name = StringData.getNpcStringById(id);
                            break;
                        case "map":
                            name = StringData.getMapStringById(id);
                            break;
                        default:
                            chr.chatMessage("未知的查詢類型 " + queryType);
                            return;
                    }
                    if (name == null) {
                        chr.chatMessage(Mob, "找不到 " + queryType + " " + id);
                        return;
                    }
                    chr.chatMessage(Mob, "名稱: " + name);
                } else {
                    Map<Integer, String> map;
                    switch (queryType) {
                        case "equip":
                            map = StringData.getItemStringByName(query.toString());
                            Set<Integer> nonEquips = new HashSet<>();
                            for (int itemId : map.keySet()) {
                                if (!ItemConstants.isEquip(itemId)) {
                                    nonEquips.add(itemId);
                                }
                            }
                            for (int itemId : nonEquips) {
                                map.remove(itemId);
                            }
                            break;
                        case "item":
                            map = StringData.getItemStringByName(query.toString());
                            break;
                        case "mob":
                            map = StringData.getMobStringByName(query.toString());
                            break;
                        case "npc":
                            map = StringData.getNpcStringByName(query.toString());
                            break;
                        case "map":
                            map = StringData.getMapStringByName(query.toString());
                            break;
                        default:
                            chr.chatMessage("未知的查詢類型 " + queryType);
                            return;
                    }
                    if (map.size() == 0) {
                        chr.chatMessage(Mob, "查無 " + queryType + "，查詢：" + query);
                        return;
                    }
                    TreeMap<Integer, String> sortedMap = new TreeMap<>(map);
                    for (Map.Entry<Integer, String> entry : sortedMap.entrySet()) {
                        id = entry.getKey();
                        name = entry.getValue();
                        if (queryType.equalsIgnoreCase("item")) {
                            Item item = ItemData.getEquipDeepCopyFromID(id, false);
                            if (item == null) {
                                item = ItemData.getItemDeepCopy(id);
                            }
                            if (item == null) {
                                continue;
                            }
                        }
                        chr.chatMessage(Mob, "Id: " + id);
                        chr.chatMessage(Mob, "名稱: " + name);
                    }
                }
            }
        }
    }

    /** !rate — show; !rate exp|exp1|exp2|exp3|meso|drop|quest &lt;1-10000&gt; — set now and save to server.properties. */
    @Command(names = {"rate", "rates"}, requiredType = Admin)
    public static class RateCommand extends AdminCommand {

        public static void execute(Char chr, String[] args) {
            if (args.length >= 2 && args[1].equalsIgnoreCase("reload")) {
                net.swordie.ms.RateSettings.reloadAll();
                for (String line : net.swordie.ms.RateSettings.summary().split("\n")) {
                    chr.chatMessage(line);
                }
                chr.chatMessage(net.swordie.ms.GameSettings.summary());
                chr.chatMessage("已重新讀取 config/server.properties。");
                return;
            }
            if (args.length < 3) {
                for (String line : net.swordie.ms.RateSettings.summary().split("\n")) {
                    chr.chatMessage(line);
                }
                chr.chatMessage(net.swordie.ms.GameSettings.summary());
                chr.chatMessage("用法: !rate <exp|exp1|exp2|exp3|meso|drop|quest> <1-10000>  (exp 同時設定三個等級段)；!rate reload 重新讀取設定");
                return;
            }
            java.util.List<net.swordie.ms.RateSettings.Rate> targets = switch (args[1].toLowerCase()) {
                case "exp" -> java.util.List.of(net.swordie.ms.RateSettings.Rate.EXP_1_100,
                        net.swordie.ms.RateSettings.Rate.EXP_101_210, net.swordie.ms.RateSettings.Rate.EXP_211);
                case "exp1" -> java.util.List.of(net.swordie.ms.RateSettings.Rate.EXP_1_100);
                case "exp2" -> java.util.List.of(net.swordie.ms.RateSettings.Rate.EXP_101_210);
                case "exp3" -> java.util.List.of(net.swordie.ms.RateSettings.Rate.EXP_211);
                case "meso" -> java.util.List.of(net.swordie.ms.RateSettings.Rate.MESO);
                case "drop" -> java.util.List.of(net.swordie.ms.RateSettings.Rate.DROP);
                case "quest" -> java.util.List.of(net.swordie.ms.RateSettings.Rate.QUEST_EXP);
                default -> java.util.List.of();
            };
            int value;
            try {
                value = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                value = -1;
            }
            if (targets.isEmpty() || !net.swordie.ms.RateSettings.valid(value)) {
                chr.chatMessage("用法: !rate <exp|exp1|exp2|exp3|meso|drop|quest> <1-10000>");
                return;
            }
            String failure = null;
            for (net.swordie.ms.RateSettings.Rate rate : targets) {
                try {
                    net.swordie.ms.RateSettings.set(rate, value, true);
                } catch (java.io.IOException e) {
                    failure = e.getMessage();
                }
                chr.chatMessage(rate.label() + " → x" + value);
            }
            chr.chatMessage(failure == null ? "已即時生效並寫入 config/server.properties。"
                    : "已即時生效，但寫入設定檔失敗（重啟後會恢復舊值）: " + failure);
        }
    }

    @Command(names = {"nx", "setnx"}, requiredType = GameMaster)
    public static class NxCommand extends AdminCommand {

        public static void execute(Char chr, String[] args) {
            int nx = Integer.parseInt(args[1]);
            chr.addNx(nx);
        }
    }

    @Command(names = {"dp", "setdp"}, requiredType = GameMaster)
    public static class DpCommand extends AdminCommand {

        public static void execute(Char chr, String[] args) {
            int dp = Integer.parseInt(args[1]);
            User user = chr.getUser();
            user.setDonationPoints(dp);
        }
    }

    @Command(names = {"vp", "setvp"}, requiredType = GameMaster)
    public static class VpCommand extends AdminCommand {

        public static void execute(Char chr, String[] args) {
            int vp = Integer.parseInt(args[1]);
            User user = chr.getUser();
            user.setVotePoints(vp);
        }
    }

    @Command(names = {"goto"}, requiredType = GameMaster)
    public static class GoTo extends AdminCommand {

        public static void execute(Char chr, String[] args) {

            HashMap<String, Integer> gotomaps = new HashMap<>();
            gotomaps.put("ardent", 910001000);
            gotomaps.put("ariant", 260000100);
            gotomaps.put("amherst", 1010000);
            gotomaps.put("amoria", 680000000);
            gotomaps.put("aqua", 860000000);
            gotomaps.put("aquaroad", 230000000);
            gotomaps.put("boatquay", 541000000);
            gotomaps.put("cwk", 610030000);
            gotomaps.put("edelstein", 310000000);
            gotomaps.put("ellin", 300000000);
            gotomaps.put("ellinia", 101000000);
            gotomaps.put("ellinel", 101071300);
            gotomaps.put("elluel", 101050000);
            gotomaps.put("elnath", 211000000);
            gotomaps.put("ereve", 130000000);
            gotomaps.put("florina", 120000300);
            gotomaps.put("fm", 910000000);
            gotomaps.put("future", 271000000);
            gotomaps.put("gmmap", 180000000);
            gotomaps.put("happy", 209000000);
            gotomaps.put("harbor", 104000000);
            gotomaps.put("henesys", 100000000);
            gotomaps.put("herbtown", 251000000);
            gotomaps.put("kampung", 551000000);
            gotomaps.put("kerning", 103000000);
            gotomaps.put("korean", 222000000);
            gotomaps.put("leafre", 240000000);
            gotomaps.put("ludi", 220000000);
            gotomaps.put("malaysia", 550000000);
            gotomaps.put("mulung", 250000000);
            gotomaps.put("nautilus", 120000000);
            gotomaps.put("nlc", 600000000);
            gotomaps.put("omega", 221000000);
            gotomaps.put("orbis", 200000000);
            gotomaps.put("pantheon", 400000000);
            gotomaps.put("pinkbean", 270050100);
            gotomaps.put("phantom", 610010000);
            gotomaps.put("perion", 102000000);
            gotomaps.put("rien", 140000000);
            gotomaps.put("showatown", 801000000);
            gotomaps.put("singapore", 540000000);
            gotomaps.put("sixpath", 104020000);
            gotomaps.put("sleepywood", 105000000);
            gotomaps.put("southperry", 2000000);
            gotomaps.put("tot", 270000000);
            gotomaps.put("twilight", 273000000);
            gotomaps.put("tynerum", 301000000);
            gotomaps.put("zipangu", 800000000);
            gotomaps.put("pianus", 230040420);
            gotomaps.put("horntail", 240060200);
            gotomaps.put("chorntail", 240060201);
            gotomaps.put("griffey", 240020101);
            gotomaps.put("manon", 240020401);
            gotomaps.put("zakum", 280030000);
            gotomaps.put("czakum", 280030001);
            gotomaps.put("pap", 220080001);
            gotomaps.put("oxquiz", 109020001);
            gotomaps.put("ola", 109030101);
            gotomaps.put("fitness", 109040000);
            gotomaps.put("snowball", 109060000);
            gotomaps.put("boss", 682020000);
            gotomaps.put("dojo", 925020001);
            gotomaps.put("pq", 910002000);
            gotomaps.put("h", 100000000);
            gotomaps.put("gollux", 863010000);
            gotomaps.put("lotus", 350060300);
            gotomaps.put("damien", 105300303);
            gotomaps.put("ursus", 970072200);
            gotomaps.put("pno", 811000008);
            gotomaps.put("cygnus", 271040000);
            gotomaps.put("ra", 105200000);
            gotomaps.put("goldenbeach", 914200000);
            gotomaps.put("ardentmill", 910001000);
            gotomaps.put("oz", 992000000);
            gotomaps.put("runner", 993001000);

            if (args.length == 1) {
                chr.chatMessage(Notice2, "地點清單: " + gotomaps.keySet());
            } else if (gotomaps.containsKey(args[1])) {
                Field toField = chr.getClient().getChannelInstance().getField(gotomaps.get(args[1]));
                Portal portal = chr.getField().getDefaultPortal();
                chr.warp(toField, portal);
            } else if (args[1].equals("locations")) {
                chr.chatMessage(Notice2, "請用 !goto <地點>");
                StringBuilder sb = new StringBuilder();
                for (String s : gotomaps.keySet()) {
                    sb.append(s).append(",  ");
                }
                chr.chatMessage(Notice2, sb.substring(0, sb.length()));
            } else {
                chr.chatMessage(Notice2, "地圖不存在。");
            }
        }
    }

    @Command(names = {"savemap"}, requiredType = GameMaster)
    public static class SaveMap extends AdminCommand {

        private static HashMap<String, Integer> quickmaps = new HashMap<>();

        public static void execute(Char chr, String[] args) {
            int mapid = chr.getFieldID();
            if (args.length < 1 && !args[1].equalsIgnoreCase("list")) {
                chr.chatMessage(AdminChat, "語法錯誤: !SaveMap <save/go> <名稱>");
                chr.chatMessage(AdminChat, "查看已儲存的地圖清單請用: !SaveMap list");
            }
            if (args[1].equalsIgnoreCase("save")) {
                String key = args[2];
                quickmaps.put(key, mapid);
                chr.chatMessage(AdminChat, "[SaveMap] 地圖: " + mapid + " 已儲存為「" + key + "」。");
            } else if (args[1].equalsIgnoreCase("go")) {
                String key = args[2];
                if (quickmaps.get(key) == null) {
                    chr.chatMessage(AdminChat, "[SaveMap] 沒有儲存為「" + args[2] + "」的地圖。");
                    return;
                }
                Field toField = chr.getOrCreateFieldByCurrentInstanceType((quickmaps.get(key)));
                Portal portal = chr.getField().getDefaultPortal();
                chr.warp(toField, portal);
            } else if (args[1].equalsIgnoreCase("list")) {
                Set keys = quickmaps.keySet();
                chr.chatMessage(AdminChat, "[SaveMap] " + quickmaps.size() + " 個已儲存的地圖。");
                for (Object maps : keys) {
                    chr.chatMessage(AdminChat, "[SaveMap] 已儲存地圖: " + quickmaps.get(maps) + " 名稱「" + maps + "」。");
                }
            } else {
                chr.chatMessage(AdminChat, "語法錯誤: !SaveMap <save/go> <名稱>");
                chr.chatMessage(AdminChat, "查看已儲存的地圖清單請用: !SaveMap list");
            }
        }
    }

    @Command(names = {"warriorequips"}, requiredType = GameMaster)
    public static class WarriorEquips extends AdminCommand {

        public static void execute(Char chr, String[] args) {
            int[] warEquips = new int[]{
                    1302000,
                    1312000,
                    1322000,
                    1402000,
                    1412000,
                    1422000,
                    1432000,
                    1442000,
                    1542000,
                    1232000,
                    1582000,
                    1353400,
                    1352500,};
            for (int warEquip : warEquips) {
                Item item = ItemData.getItemDeepCopy(warEquip);
                chr.addItemToInventory(item);
            }
        }
    }

    @Command(names = {"mageequips"}, requiredType = GameMaster)
    public static class MageEquips extends AdminCommand {

        public static void execute(Char chr, String[] args) {
            int[] mageEquips = new int[]{
                    1382000,
                    1372000,
                    1552000,
                    1252000,
                    1262000,
                    1353200,};
            for (int mageEquip : mageEquips) {
                Item item = ItemData.getItemDeepCopy(mageEquip);
                chr.addItemToInventory(item);
            }
        }
    }

    @Command(names = {"archerequips"}, requiredType = GameMaster)
    public static class ArcherEquips extends AdminCommand {

        public static void execute(Char chr, String[] args) {
            int[] archerEquips = new int[]{
                    1452000,
                    1462000,
                    1522000,
                    1352004,};
            for (int archerEquip : archerEquips) {
                Item item = ItemData.getItemDeepCopy(archerEquip);
                chr.addItemToInventory(item);
            }
        }
    }

    @Command(names = {"thiefequips"}, requiredType = GameMaster)
    public static class ThiefEquips extends AdminCommand {

        public static void execute(Char chr, String[] args) {
            int[] thiefEquips = new int[]{
                    1472000,
                    1332000,
                    1342000,
                    1242000,
                    1362000,
                    1352100
            };
            for (int thiefEquip : thiefEquips) {
                Item item = ItemData.getItemDeepCopy(thiefEquip);
                chr.addItemToInventory(item);
            }
        }
    }

    @Command(names = {"pirateequips"}, requiredType = GameMaster)
    public static class PirateEquips extends AdminCommand {

        public static void execute(Char chr, String[] args) {
            int[] pirateEquips = new int[]{
                    1482000,
                    1353100,
                    1492000,
                    1222000,
                    1352600,
                    1532000,
                    1242000,};
            for (int pirateEquip : pirateEquips) {
                Item item = ItemData.getItemDeepCopy(pirateEquip);
                chr.addItemToInventory(item);
            }
        }
    }

    @Command(names = {"clearinv"}, requiredType = GameMaster)
    public static class ClearInv extends AdminCommand {

        public static void execute(Char chr, String[] args) {
            if (args.length < 2) {
                chr.chatMessage(Notice2, "語法錯誤: !ClearInv <欄位類型> <起始格> <結束格>");
                return;
            }
            InvType invType = InvType.getInvTypeByString(args[1]);
            if (invType == null) {
                chr.chatMessage("請輸入正確的欄位類型：equip / use / etc / setup / cash");
                return;
            }
            short startIndex = Short.parseShort(args[2]);
            short endIndex = Short.parseShort(args[3]);
            for (int i = startIndex; i < endIndex; i++) {
                Item removeItem = chr.getInventoryByType(invType).getItemBySlot((short) i);
                chr.consumeItem(removeItem);
            }
            chr.dispose();
        }
    }

    @Command(names = {"mobinfo"}, requiredType = GameMaster)
    public static class MobInfo extends AdminCommand {

        public static void execute(Char chr, String[] args) {
            Rect rect = new Rect(
                    chr.getPosition().deepCopy().getX() - 100,
                    chr.getPosition().deepCopy().getY() - 100,
                    chr.getPosition().deepCopy().getX() + 100,
                    chr.getPosition().deepCopy().getY() + 100
            );
            Mob mob = chr.getField().getMobs().stream().filter(m -> rect.hasPositionInside(m.getPosition())).findFirst().orElse(null);
            if (mob != null) {
                Char controller = chr.getField().getCharByID(mob.getControllerID());
                chr.chatMessage(SpeakerChannel, String.format("怪物ID: %s | 模板ID: %s | HP: %s/%s | MP: %s/%s | 朝左: %s | 控制者: %s",
                                Util.getNumberFormat(mob.getObjectId()),
                                Util.getNumberFormat(mob.getTemplateId()),
                                Util.getNumberFormat(mob.getHp()),
                                Util.getNumberFormat(mob.getMaxHp()),
                                Util.getNumberFormat(mob.getMp()),
                                Util.getNumberFormat(mob.getMaxMp()),
                                mob.isLeft(),
                                controller == null ? "null" : chr.getName()
                        )
                );
            } else {
                chr.chatMessage(SpeakerChannel, "找不到怪物。");
            }
        }
    }

    @Command(names = {"completequest"}, requiredType = GameMaster)
    public static class CompleteQuest extends AdminCommand {

        public static void execute(Char chr, String[] args) {
            chr.completeQuest(Integer.parseInt(args[1]));
        }
    }

    @Command(names = {"removequest"}, requiredType = GameMaster)
    public static class RemoveQuest extends AdminCommand {

        public static void execute(Char chr, String[] args) {
            chr.removeQuest(Integer.parseInt(args[1]));
        }
    }

    @Command(names = {"sethonor, honor"}, requiredType = Admin)
    public static class SetHonor extends AdminCommand {

        public static void execute(Char chr, String[] args) {
            if (args.length < 2) {
                chr.chatMessage(SpeakerChannel, "格式: !sethonor <名聲值>");
                return;
            }
            int honor = Integer.parseInt(args[1]);
            chr.setHonorExp(honor);
            chr.write(WvsContext.characterHonorExp(honor));
        }
    }

    @Command(names = {"startquest"}, requiredType = GameMaster)
    public static class StartQuest extends AdminCommand {

        public static void execute(Char chr, String[] args) {
            if (args.length < 2) {
                chr.chatMessage(SpeakerChannel, "格式: !startquest <任務ID>");
                return;
            }
            chr.getScriptManager().startQuest(Integer.parseInt(args[1]));
        }
    }

    @Command(names = {"deleteQuest"}, requiredType = GameMaster)
    public static class DeleteQuest extends AdminCommand {

        public static void execute(Char chr, String[] args) {
            if (args.length < 2) {
                chr.chatMessage(SpeakerChannel, "格式: !startquest <任務ID>");
                return;
            }
            chr.removeQuest(Integer.parseInt(args[1]));
        }
    }

    @Command(names = {"shop"}, requiredType = GameMaster)
    public static class Shop extends AdminCommand {

        public static void execute(Char chr, String[] args) {
            chr.getScriptManager().openShop(1011100);
        }
    }

    @Command(names = {"mobstat"}, requiredType = GameMaster)
    public static class MobStatTest extends AdminCommand {

        public static void execute(Char chr, String[] args) {
            List<Mob> mobs = new ArrayList<>(chr.getField().getMobs());
            if (mobs.size() > 0) {
                Mob mob = mobs.get(0);
                MobTemporaryStat mts = mob.getTemporaryStat();
                Option o = new Option();
                o.nOption = 1000;
                o.rOption = 145;
                o.slv = 1;
                o.tOption = 1000;

                o.wOption = 1000;

                o.mOption = 1000;
                o.bOption = 1000;
                o.nReason = 1000;
                mts.addStatOptions(mob, MobStat.PCounter, o);
            } else {
                chr.chatMessage("找不到怪物。");
            }
        }
    }

    @Command(names = {"fp", "findportal"}, requiredType = GameMaster)
    public static class FP extends AdminCommand { // FindPortal

        public static void execute(Char chr, String[] args) {
            if (args.length < 1) {
                chr.chatMessage(SpeakerChannel, "參數錯誤。請用 !findportal <ID/名稱>");
                return;
            }
            Field field = chr.getField();
            Portal portal;
            String query = args[1];
            if (Util.isNumber(query)) {
                portal = field.getPortalByID(Integer.parseInt(query));
            } else {
                portal = field.getPortalByName(query);
            }
            if (portal == null) {
                chr.chatMessage(SpeakerChannel, "找不到傳送點 " + query);
                return;
            }
            chr.chatMessage(SpeakerChannel, "傳送點名稱: " + portal.getName());
            chr.chatMessage(SpeakerChannel, "傳送點ID: " + Util.getNumberFormat(portal.getId()));
            chr.chatMessage(SpeakerChannel, "傳送點目標地圖: " + Util.getNumberFormat(portal.getTargetMapId()));
            chr.chatMessage(SpeakerChannel, "傳送點位置: " + portal.getX() + ", " + portal.getY());
            chr.chatMessage(SpeakerChannel, "傳送點腳本: " + portal.getScript());
            chr.chatMessage(SpeakerChannel, ".");
            System.out.println(portal.getScript());
        }
    }

    @Command(names = {"showbuffs"}, requiredType = GameMaster)
    public static class showBuffs extends AdminCommand {

        public static void execute(Char chr, String[] args) {
            TemporaryStatManager tsm = chr.getTemporaryStatManager();
            Set<Integer> buffs = new HashSet<>();
            for (List<Option> options : tsm.getCurrentStats().values()) {
                for (Option o : options) {
                    if (o.rOption != 0) {
                        buffs.add(o.rOption);
                    } else {
                        buffs.add(o.nReason);
                    }
                }
            }
            StringBuilder sb = new StringBuilder("目前的增益效果：");
            for (int id : buffs) {
                String skillName = StringData.getSkillStringById(id) != null ? StringData.getSkillStringById(id).getName() : "未知的技能 ID";
                sb.append(skillName).append(" (").append(id).append("), ");
            }
            chr.chatMessage(sb.substring(0, sb.toString().length() - 2));
        }
    }

    @Command(names = {"tohex"}, requiredType = GameMaster)
    public static class toHex extends AdminCommand {

        public static void execute(Char chr, String[] args) {
            int arg = Integer.parseInt(args[1]);
            byte[] arr = new byte[4];
            arr[0] = (byte) ((arg >> 24) & 0xFF);
            arr[1] = (byte) ((arg >> 16) & 0xFF);
            arr[2] = (byte) ((arg >> 8) & 0xFF);
            arr[3] = (byte) (arg & 0xFF);
            chr.chatMessage(Util.readableByteArray(arr));
        }
    }

    @Command(names = {"fromhex"}, requiredType = GameMaster)
    public static class fromHex extends AdminCommand {

        public static void execute(Char chr, String[] args) {
            if (args.length == 1) {
                return;
            }
            StringBuilder sb = new StringBuilder();
            for (int i = 1; i < args.length; i++) {
                sb.append(args[i].trim());
            }
            String s = sb.toString();
            s = s.replace("|", " ");
            s = s.replace(" ", "");
            int len = s.length();
            int[] arr = new int[len / 2];
            for (int i = 0; i < len; i += 2) {
                arr[i / 2] = ((Character.digit(s.charAt(i), 16) << 4)
                        + Character.digit(s.charAt(i + 1), 16));
            }
            int num = 0;
            for (int i = 0; i < arr.length; i++) {
                num += arr[i] << (i * 8);
            }
            chr.chatMessage("" + num);
        }
    }

    @Command(names = {"lookupreactor", "reactors"}, requiredType = GameMaster)
    public static class lookupreactor extends AdminCommand {

        public static void execute(Char chr, String[] args) {
            chr.getField().getReactors().forEach(reactor -> chr.chatMessage(reactor.toString()));
        }
    }

    @Command(names = {"script"}, requiredType = GameMaster)
    public static class StartScriptTest extends AdminCommand {

        public static void execute(Char chr, String[] args) {
            if (args.length < 3) {
                chr.chatMessage("!script <類型> <名稱>");
                return;
            }
            ScriptType st = null;
            for (ScriptType type : ScriptType.values()) {
                if (type.toString().equalsIgnoreCase(args[1])) {
                    st = type;
                    break;
                }
            }
            if (st == null) {
                StringBuilder str = new StringBuilder();
                for (ScriptType t : ScriptType.values()) {
                    str.append(t.toString()).append(", ");
                }
                String res = str.substring(0, str.length() - 2);
                chr.chatMessage(String.format("未知的腳本類型 %s，可用類型：%s", args[1], res));
                return;
            }
            chr.getScriptManager().startScript(chr, Integer.parseInt(args[3]), args[2], st);
        }
    }

    @Command(names = {"givenx"}, requiredType = GameMaster)
    public static class giveNx extends AdminCommand {

        public static void execute(Char chr, String[] args) {
            String name = args[1];
            int amount = Integer.valueOf(args[2]);
            Char other = chr.getWorld().getCharByName(name);
            other.addNx(amount);
        }
    }

    @Command(names = {"testCommand"}, requiredType = GameMaster)
    public static class testCommand extends AdminCommand {

        public static void execute(Char chr, String[] args) {
            ItemOption io = ItemData.getItemOptionById(Integer.parseInt(args[1]));
            for (var a : io.getStatValuesPerLevel().values()) {
                for (var b : a.entrySet()) {
                    System.out.println(b.getKey() + " " + b.getValue());
                }
            }

        }
    }

    @Command(names = {"testInstance"}, requiredType = GameMaster)
    public static class testInstance extends AdminCommand {

        public static void execute(Char chr, String[] args) {
            if (chr.getInstance() != null) {
                chr.chatMessage("副本：是");
            } else {
                chr.chatMessage("副本：否");
            }
        }
    }

    @Command(names = {"testAction"}, requiredType = GameMaster)
    public static class testAction extends AdminCommand {

        public static void execute(Char chr, String[] args) {
            int action = Integer.parseInt(args[1]);
            chr.chatMessage("動作: " + action);
            for (Mob mob : chr.getField().getMobs()) {
                System.out.println(mob.getTemplateId());
                chr.getField().broadcast(MobPool.forcedSkillAction(mob.getObjectId(), action));
            }
        }
    }

    @Command(names = {"serverInfo", "online"}, requiredType = GameMaster)
    public static class ServerInformation extends AdminCommand {

        public static void execute(Char chr, String[] args) {
            chr.getScriptManager().startScript(chr, 0, "Server_Information", ScriptType.Npc);
        }
    }

    @Command(names = {"toggleLoginMode"}, requiredType = GameMaster)
    public static class toggleLogin extends AdminCommand {

        public static void execute(Char chr, String[] args) {
            ServerConfig.ADMIN_LOGIN = !ServerConfig.ADMIN_LOGIN;
            chr.chatMessage("管理員登入: " + ServerConfig.ADMIN_LOGIN);
        }
    }

    @Command(names = {"toggleEvent"}, requiredType = Admin)
    public static class toggleEvent extends AdminCommand {

        public static void execute(Char chr, String[] args) {
            if (args.length < 2) {
                chr.chatMessage(SpeakerChannel, "參數錯誤。請用 !toggleEvent <FeverTime/MiracleTime/ExpRate/DropRate> <Enable/Disable>");
                return;
            }
            boolean isEnable = args[2].equals("Enable");
            switch (args[1]) {
                case "FeverTime":
                    if (isEnable) {
                        if (EventConstants.STAR_FORCE_FEVER_TIME_EVENT) {
                            chr.chatMessage(SpeakerChannel, "狂熱時間活動已經開啟！");
                            return;
                        } else if (EventConstants.MIRACLE_TIME_EVENT || EventConstants.EXP_RATE_EVENT || EventConstants.DROP_RATE_EVENT) {
                            chr.chatMessage(SpeakerChannel, "已有其他特殊活動開啟中！");
                            return;
                        }
                    } else if (!EventConstants.STAR_FORCE_FEVER_TIME_EVENT) {
                        chr.chatMessage(SpeakerChannel, "狂熱時間活動已經關閉！");
                        return;
                    }
                    chr.initFeverTimeEvent(isEnable);
                    break;
                case "MiracleTime":
                    if (isEnable) {
                        if (EventConstants.MIRACLE_TIME_EVENT) {
                            chr.chatMessage(SpeakerChannel, "奇蹟時間活動已經開啟！");
                            return;
                        } else if (EventConstants.STAR_FORCE_FEVER_TIME_EVENT || EventConstants.EXP_RATE_EVENT || EventConstants.DROP_RATE_EVENT) {
                            chr.chatMessage(SpeakerChannel, "已有其他特殊活動開啟中！");
                            return;
                        }
                    } else if (!EventConstants.MIRACLE_TIME_EVENT) {
                        chr.chatMessage(SpeakerChannel, "奇蹟時間活動已經關閉！");
                        return;
                    }
                    chr.initMiracleTimeEvent(isEnable);
                    break;
                case "ExpRate":
                    if (isEnable) {
                        if (EventConstants.EXP_RATE_EVENT) {
                            chr.chatMessage(SpeakerChannel, "經驗倍率活動已經開啟！");
                            return;
                        } else if (EventConstants.STAR_FORCE_FEVER_TIME_EVENT || EventConstants.MIRACLE_TIME_EVENT || EventConstants.DROP_RATE_EVENT) {
                            chr.chatMessage(SpeakerChannel, "已有其他特殊活動開啟中！");
                            return;
                        }
                    } else if (!EventConstants.EXP_RATE_EVENT) {
                        chr.chatMessage(SpeakerChannel, "經驗倍率活動已經關閉！");
                        return;
                    }
                    chr.initExpRateEvent(isEnable);
                    break;
                case "DropRate":
                    if (isEnable) {
                        if (EventConstants.DROP_RATE_EVENT) {
                            chr.chatMessage(SpeakerChannel, "掉寶倍率活動已經開啟！");
                            return;
                        } else if (EventConstants.STAR_FORCE_FEVER_TIME_EVENT || EventConstants.MIRACLE_TIME_EVENT || EventConstants.EXP_RATE_EVENT) {
                            chr.chatMessage(SpeakerChannel, "已有其他特殊活動開啟中！");
                            return;
                        }
                    } else if (!EventConstants.DROP_RATE_EVENT) {
                        chr.chatMessage(SpeakerChannel, "掉寶倍率活動已經關閉！");
                        return;
                    }
                    chr.initDropRateEvent(isEnable);
                    break;
            }
        }
    }

    @Command(names = {"checkProcess"}, requiredType = GameMaster)
    public static class checkProcess extends AdminCommand {

        public static void execute(Char chr, String[] args) {
            String target = args[1];
            for (Char targetChar : chr.getField().getChars()) {
                if (target.equals(targetChar.getName())) {
                    chr.chatMessage("名稱: " + target);
                    targetChar.write(WvsContext.checkProcessResult(false));
                    break;
                }
            }
        }
    }
}
