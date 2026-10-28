package net.swordie.ms.client.character;

import net.swordie.ms.connection.packet.DreamBreakerPacket;
import net.swordie.ms.connection.packet.UserLocal;
import net.swordie.ms.constants.GameConstants;
import net.swordie.ms.enums.DeathType;
import net.swordie.ms.enums.InvType;
import net.swordie.ms.handlers.GlobalTimerManager;
import net.swordie.ms.life.mob.Mob;
import net.swordie.ms.scripts.ScriptManagerImpl;
import net.swordie.ms.util.Util;
import net.swordie.ms.world.field.Instance;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ScheduledFuture;

public class DreamBreaker {

    private final ScriptManagerImpl sm;
    private final Char chr;
    private final int entryQuest = 15901;
    private final int dreamCoin = 4036068;
    public static final int gameMap = 921171000;
    private final int exitMap = 921171100;
    private final int time = 180 * 1000;
    private final int[][] positions = {{792, -457}, {813, -1963}, {3107, -237}, {838, 1485}, {-1450, -237}};
    private final int[][] musicBox = {{9833080, 9833070}, {9833081, 9833071}, {9833082, 9833072}, {9833083, 9833073}, {9833084, 9833074}};
    private final int[][] monsters = {{9833090, 9833091}, {9833092, 9833093}, {9833094, 9833095}, {9833096, 9833097}, {9833098, 9833099}};
    private ScheduledFuture<?> updateGaugeTimer;
    private ScheduledFuture<?> spawnOriginTimer;

    public DreamBreaker(Char chr) {
        this.chr = chr;
        this.sm = chr.getScriptManager();
    }

    public void start() {
        sm.setSpeakerID(9010100);
        if (chr.getFieldID() == 921171100) {
            sm.warpInstanceOut(chr, 450004000);
            return;
        }
        if (chr.getParty() != null) {
            sm.sendSayOkay("請先退出隊伍再進入。");
            //sm.dispose();
            return;
        }
        if (chr.getLevel() >= 220 && chr.hasQuestCompleted(34330)) {
            final LocalDateTime now = LocalDateTime.now();
            if (!chr.hasQuest(entryQuest)) {
                chr.createQuestWithQRValue(entryQuest, "selectedStage=0;stage=0;best=0;besttime=0;best_b=0;besttime_b=0;dream=0;clearTime=0;rank_b=0;count=0;date=" + DateTimeFormatter.ofPattern("yy/MM/dd").format(now));
            }
            int selection = sm.sendNext("#e#b毀夢者#k#n\r\n(打呵欠) 我到底什麼時候才能好好睡一覺？\r\n\r\n#b" +
                    "#L0#挑戰<毀夢者>。#l\r\n" +
                    "#L1#兌換夢幻硬幣。#l\r\n" +
                    "#L2#查看我的紀錄。#l\r\n" +
                    "#L3#查看每週排名。#l\r\n" +
                    "#L4#領取毀夢者勳章。#l\r\n" +
                    "#L5#聽<毀夢者>的說明。#l#k\r\n\r\n" +
                    "#e通關 1 次後，就可以選擇立即完成。#n");
            switch (selection) {
                case 0:
                    int lastStage = Integer.parseInt(chr.getQRValueByKey(entryQuest, "best"));
                    if (lastStage == 0) {
                        selection = sm.sendNext("你可以挑戰以下任一關卡：\r\n#L0#第 1 關#l");
                    } else {
                        selection = sm.sendNext("你的最佳紀錄是第 " + lastStage + " 關。\r\n你可以挑戰以下任一關卡：\r\n" + getSelection(lastStage));
                    }
                    if (chr.getParty() != null) {
                        sm.sendSayOkay("你目前在隊伍中，請先退出隊伍才能進入！");
                    } else {
                        if (sm.checkAttempt(entryQuest, 3)) {
                            if (sm.getEmptyInventorySlots(InvType.ETC) >= 1) {
                                sm.warpInstanceIn(chr, gameMap, false);
                                chr.setQRValueByKey(entryQuest, "selectedStage", getSelectedStageBySelection(selection, lastStage) + "");
                                chr.setQRValueByKey(entryQuest, "stage", getSelectedStageBySelection(selection, lastStage) + "");
                                sm.addAttempt(entryQuest, 3);
                                return;
                            } else {
                                sm.sendSayOkay("請先在其他欄空出至少一格。");
                            }
                        } else {
                            sm.sendSayOkay("有隊員已達今日挑戰次數上限。");
                        }
                    }
                    break;
                case 1:
                    int max = sm.getQuantityOfItem(dreamCoin) / 30;
                    if (max > 0) {
                        int lacheleinSymbol = 1712003;
                        int count = sm.sendAskNumber("要把 #i" + dreamCoin + "# #b#t" + dreamCoin + "##k兌換成 #i" + lacheleinSymbol + "# #r#t" + lacheleinSymbol + "##k嗎？\r\n(#b#t" + dreamCoin + "# x30#k = #r#t" + lacheleinSymbol + "# x1#k)\r\n最多可以兌換 #e#r" + max + "#k#n 個。", 1, 1, max);
                        if (count > 0 && count <= max) {
                            if (sm.hasItem(dreamCoin, count * 30)) {
                                if (sm.canHold(lacheleinSymbol, count)) {
                                    sm.consumeItem(dreamCoin, count * 30);
                                    sm.giveSymbol(lacheleinSymbol, count);
                                } else {
                                    sm.sendSayOkay("請在裝備欄空出更多空間。");
                                }
                            } else {
                                sm.sendSayOkay("夢幻硬幣不足！別浪費我的時間。");
                            }
                        } else {
                            sm.sendSayOkay("輸入的數值有誤，請再試一次！"); // won't happen
                        }
                    } else {
                        sm.sendSayOkay("至少要有 30 個夢幻硬幣才能兌換。"); // won't happen
                    }
                    break;
                case 2:
                    int bestStage = Integer.parseInt(chr.getQRValueByKey(entryQuest, "best"));
                    int bestTime = Integer.parseInt(chr.getQRValueByKey(entryQuest, "besttime")) / 1000;
                    int clearTime = Integer.parseInt(chr.getQRValueByKey(entryQuest, "clearTime")) / 1000;
                    int dreamPoint = Integer.parseInt(chr.getQRValueByKey(entryQuest, "dream"));
                    sm.sendSayOkay("你的毀夢者紀錄：\r\n" +
                            "1. 最佳關卡：#r" + bestStage + "#k.\r\n" +
                            "2. 最佳時間：#r" + bestTime + " 秒#k\r\n" +
                            "3. 通關時間：#r" + clearTime + " 秒#k\r\n" +
                            "4. 夢幻點數：#r" + dreamPoint + "#k.\r\n");
                    break;
                case 4:
                    lastStage = Integer.parseInt(chr.getQRValueByKey(entryQuest, "best"));
                    String stage30 = chr.hasQuestCompleted(15922) ? "#r(已領取)#k" : "";
                    String stage40 = chr.hasQuestCompleted(15923) ? "#r(已領取)#k" : "";
                    String stage50 = chr.hasQuestCompleted(15924) ? "#r(已領取)#k" : "";
                    String stage60 = chr.hasQuestCompleted(15925) ? "#r(已領取)#k" : "";
                    String stage70 = chr.hasQuestCompleted(15926) ? "#r(已領取)#k" : "";
                    String stage80 = chr.hasQuestCompleted(15927) ? "#r(已領取)#k" : "";
                    String stage90 = chr.hasQuestCompleted(15928) ? "#r(已領取)#k" : "";
                    String stage100Plus = chr.hasQuestCompleted(15929) ? "#r(已領取)#k" : "";
                    selection = sm.sendNext("你需要哪一個？\r\n" +
                            "#L0##i1143013# #b#t1143013##k " + stage30 + "#l\r\n" +
                            "#L1##i1143014# #b#t1143014##k " + stage40 + "#l\r\n" +
                            "#L2##i1143015# #b#t1143015##k " + stage50 + "#l\r\n" +
                            "#L3##i1143016# #b#t1143016##k " + stage60 + "#l\r\n" +
                            "#L4##i1143017# #b#t1143017##k " + stage70 + "#l\r\n" +
                            "#L5##i1143018# #b#t1143018##k " + stage80 + "#l\r\n" +
                            "#L6##i1143019# #b#t1143019##k " + stage90 + "#l\r\n" +
                            "#L7##i1143020# #b#t1143020##k " + stage100Plus + "#l\r\n");
                    if (selection >= 0 && selection <= 7) {
                        if (!chr.hasQuestCompleted(selection + 15922) && lastStage >= (30 + 10 * selection)) {
                            if (sm.canHold(1143013 + selection)) {
                                sm.giveItem(1143013 + selection);
                            } else {
                                sm.sendNext("請在裝備欄空出更多空間。");
                            }
                        } else {
                            sm.sendSayOkay("必須在毀夢者中通過至少 " + (30 + 10 * selection) + " 關才能領取這個勳章。");
                        }
                    }
                    break;
                case 3:
                    sm.sendSayOkay("準備中！");
                    break;
                case 5:
                    chr.write(UserLocal.openUrl("https://forums.maplestory.nexon.net/discussion/16304/dream-defender-guide"));
                    break;
            }
        } else {
            sm.sendSayOkay("請先完成任務#e#r[拉契爾恩] 惡夢時計塔 4樓#k#n，並達到 220 等以上才能挑戰毀夢者！");
        }
        chr.dispose();
    }

    public void exit() {
        Instance instance = chr.getInstance();
        final int stage = Integer.parseInt(chr.getQRValueByKey(entryQuest, "stage"));
        chr.getField().removeMobs();
        if (updateGaugeTimer != null) updateGaugeTimer.cancel(true);
        if (spawnOriginTimer != null) spawnOriginTimer.cancel(true);
        chr.write(DreamBreakerPacket.disableTimer(true, instance.getRemainingTime() * 1000));
        int selectedStage = Integer.parseInt(chr.getQRValueByKey(entryQuest, "selectedStage"));
        if (stage == selectedStage) {
            sm.showEffect("Map/Effect2.img/event/gameover");
        } else if (stage > selectedStage) {
            sm.showEffect("Map/Effect3.img/hungryMuto/Clear");
            sm.chatScript("[毀夢者] 第 " + (stage - 1) + " 關通關！");
        }
        sm.warpInstanceOut(chr, exitMap);
    }

    public void event() {
        final int stage = Integer.parseInt(chr.getQRValueByKey(entryQuest, "selectedStage"));
        Instance instance = chr.getInstance();
        if (instance == null || chr.getFieldID() != gameMap) {
            end();
            return;
        }
        instance.addProperty("Gauge", 500);
        instance.addProperty("gaugeHold", "false");
        instance.addProperty("stopSpawn", "false");
        chr.write(DreamBreakerPacket.setStage(stage));
        chr.write(DreamBreakerPacket.disableTimer(true, time));
        chr.write(DreamBreakerPacket.setCooldown(stage));
        sm.addEvent(chr.getTimer().addEvent(() -> init(stage), 3000L));
    }

    public void end() {
        if (updateGaugeTimer != null) updateGaugeTimer.cancel(true);
        if (spawnOriginTimer != null) spawnOriginTimer.cancel(true);
        sm.showEffect("Map/Effect2.img/event/gameover");
        sm.warpInstanceOut(chr, exitMap);
        //sm.dispose();
    }

    public void cancelTimer() {
        if (updateGaugeTimer != null) updateGaugeTimer.cancel(true);
        if (spawnOriginTimer != null) spawnOriginTimer.cancel(true);
    }

    private List<Integer> getStages(int lastStage) {
        List<Integer> stages = new ArrayList<>();
        stages.add(1);
        for (int i = 10; i < 1000; i = i + 10) {
            if (lastStage >= i) {
                stages.add(i);
            }
        }
        stages.sort(Collections.reverseOrder());
        return stages;
    }

    private String getSelection(int lastStage) {
        String sb = "";
        int i = 0;
        for (Integer j : getStages(lastStage)) {
            sb += "#L" + i + "# 第 #b" + j + "#k 關#l\r\n";
            i++;
        }
        return sb;
    }

    private Integer getSelectedStageBySelection(int selection, int lastStage) {
        Map<Integer, Integer> stages = new HashMap<>();
        int x = 0;
        for (int i = getStages(lastStage).get(0); i >= 10; i = i - 10) {
            stages.put(x, i);
            x++;
        }
        for (Map.Entry<Integer, Integer> stage : stages.entrySet()) {
            if (selection == stage.getKey()) {
                return stage.getValue();
            }
        }
        return 1;
    }

    private void init(int stage) {
        Instance instance = chr.getInstance();
        instance.setTimeout(time);
        instance.setProperty("gaugeHold", "false");
        instance.setProperty("stopSpawn", "false");
        chr.write(DreamBreakerPacket.disableTimer(false, time));
        for (Mob m : chr.getField().getMobs()) {
            chr.getField().removeMob(m.getObjectId(), DeathType.NO_ANIMATION_DEATH);
        }
        if (updateGaugeTimer != null) updateGaugeTimer.cancel(true);
        if (spawnOriginTimer != null) spawnOriginTimer.cancel(true);

        List<Integer> numbers = new ArrayList<>();
        for (int i = 0; i <= 4; i++) {
            numbers.add(i);
        }
        List<Integer> purpleMusicBox = new ArrayList<>(3);
        for (int i = 0; i < 3; i++) {
            Integer rand = Util.getRandomFromCollection(numbers);
            purpleMusicBox.add(rand);
            numbers.removeIf(x -> x.equals(rand));
        }
        for (Integer box : numbers) {
            sm.spawnMob(musicBox[box][1], positions[box][0], positions[box][1], false, GameConstants.getDreamBreakerHP(stage));
        }
        for (Integer box : purpleMusicBox) {
            sm.spawnMob(musicBox[box][0], positions[box][0], positions[box][1], false, GameConstants.getDreamBreakerHP(stage));
        }
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 2; j++) {
                for (int h = 0; h < 3; h++) {
                    sm.spawnMob(monsters[i][j], positions[i][0], positions[i][1], false, GameConstants.getDreamBreakerHP(stage));
                }
            }
        }
        ScheduledFuture<?> sf = chr.getTimer().addFixedRateEvent(this::updateGauge, 1000L, 1000L, false);
        this.updateGaugeTimer = sf;
        GlobalTimerManager.addCharTimer(chr.getId(), sf);

        ScheduledFuture<?> sf2 = chr.getTimer().addFixedRateEvent(this::spawnOrigin, 1000L, 10000L, false);
        this.spawnOriginTimer = sf2;
        GlobalTimerManager.addCharTimer(chr.getId(), sf2);
    }

    private void updateGauge() {
        if (chr.getField().getId() != gameMap) {
            end();
            return;
        }
        Instance instance = chr.getInstance();
        if (String.valueOf(instance.getProperty("gaugeHold")).equals("false")) {
            int purpleMusicBox = 0, yellowMusicBox = 0;
            for (Mob m : chr.getOrCreateFieldByCurrentInstanceType(gameMap).getMobs()) {
                int mobTemplateID = m.getTemplateId();
                if (mobTemplateID >= 9833080 && mobTemplateID <= 9833084) {
                    purpleMusicBox++;
                } else if (mobTemplateID >= 9833070 && mobTemplateID <= 9833074) {
                    yellowMusicBox++;
                }
            }

            int stage = Integer.parseInt(chr.getQRValueByKey(entryQuest, "stage"));
            final int best = Integer.parseInt(chr.getQRValueByKey(entryQuest, "best"));
            int currentDreamPoint = Integer.parseInt(chr.getQRValueByKey(entryQuest, "dream"));
            int nextStage = stage + 1;
            int time = 180 * 1000 - instance.getRemainingTime();

            int speed = (yellowMusicBox - purpleMusicBox) * -Math.max(10, stage / 15);
            int currentGauge = (int) instance.getProperty("Gauge");
            int newGauge = currentGauge + speed;
            instance.setProperty("Gauge", newGauge);
            chr.write(DreamBreakerPacket.setGauge(newGauge));

            if (newGauge <= 0) {
                chr.write(DreamBreakerPacket.disableTimer(true, instance.getRemainingTime() * 1000));
                if (stage == best) {
                    int bestTime = Integer.parseInt(chr.getQRValueByKey(entryQuest, "besttime"));
                    if (bestTime > time) {
                        chr.setQRValueByKey(entryQuest, "besttime_b", "" + bestTime);
                        chr.setQRValueByKey(entryQuest, "best_b", "" + stage);
                        chr.setQRValueByKey(entryQuest, "besttime", "" + time);
                    }
                } else if (stage > best) {
                    chr.setQRValueByKey(entryQuest, "besttime_b", chr.getQRValueByKey(entryQuest, "besttime"));
                    chr.setQRValueByKey(entryQuest, "best_b", chr.getQRValueByKey(entryQuest, "best"));
                    chr.setQRValueByKey(entryQuest, "best", "" + stage);
                    chr.setQRValueByKey(entryQuest, "besttime", "" + time);
                }

                sm.chatScript("獲得了 " + getPlusPoint(stage) + " 夢幻點數和 1 個夢幻硬幣！");
                chr.setQRValueByKey(entryQuest, "dream", currentDreamPoint + getPlusPoint(stage) + "");
                chr.setQRValueByKey(entryQuest, "clearTime", time + "");
                chr.setQRValueByKey(entryQuest, "stage", (stage + 1) + "");
                chr.write(DreamBreakerPacket.getResult(time));
                sm.giveItem(dreamCoin, 1);

                // Moving to next stage:
                sm.teleportInField(797, -457);
                instance.setProperty("Gauge", 500);
                chr.write(DreamBreakerPacket.setGauge(500));
                instance.setProperty("gaugeHold", "true");
                instance.setProperty("stopSpawn", "true");
                chr.write(DreamBreakerPacket.setStage(nextStage));
                chr.write(DreamBreakerPacket.disableTimer(true, time));
                chr.write(DreamBreakerPacket.setCooldown(nextStage));
                sm.addEvent(chr.getTimer().addEvent(() -> init(nextStage), 3000L));
            } else if (newGauge >= 1000) {
                exit();
            }
        }
    }

    private void spawnOrigin() {
        if (chr.getField().getId() != gameMap) {
            end();
            return;
        }
        Instance instance = chr.getInstance();
        if (String.valueOf(instance.getProperty("stopSpawn")).equals("false")) {
            int stage = Integer.parseInt(chr.getQRValueByKey(entryQuest, "stage"));
            Collection<Mob> mobs = chr.getField().getMobs();
            int count = 3 - (int) mobs.stream().filter(m -> m.getTemplateId() == 9833090).count();
            for (int i = 0; i < count; i++) {
                sm.spawnMob(9833090, 792, -457, false, GameConstants.getDreamBreakerHP(stage));
            }
            count = 3 - (int) mobs.stream().filter(m -> m.getTemplateId() == 9833091).count();
            for (int i = 0; i < count; i++) {
                sm.spawnMob(9833091, 792, -457, false, GameConstants.getDreamBreakerHP(stage));
            }
            count = 3 - (int) mobs.stream().filter(m -> m.getTemplateId() == 9833092).count();
            for (int i = 0; i < count; i++) {
                sm.spawnMob(9833092, 813, -1963, false, GameConstants.getDreamBreakerHP(stage));
            }
            count = 3 - (int) mobs.stream().filter(m -> m.getTemplateId() == 9833093).count();
            for (int i = 0; i < count; i++) {
                sm.spawnMob(9833093, 813, -1963, false, GameConstants.getDreamBreakerHP(stage));
            }
            count = 3 - (int) mobs.stream().filter(m -> m.getTemplateId() == 9833094).count();
            for (int i = 0; i < count; i++) {
                sm.spawnMob(9833094, 3107, -237, false, GameConstants.getDreamBreakerHP(stage));
            }
            count = 3 - (int) mobs.stream().filter(m -> m.getTemplateId() == 9833095).count();
            for (int i = 0; i < count; i++) {
                sm.spawnMob(9833095, 3107, -237, false, GameConstants.getDreamBreakerHP(stage));
            }
            count = 3 - (int) mobs.stream().filter(m -> m.getTemplateId() == 9833096).count();
            for (int i = 0; i < count; i++) {
                sm.spawnMob(9833096, 838, 1485, false, GameConstants.getDreamBreakerHP(stage));
            }
            count = 3 - (int) mobs.stream().filter(m -> m.getTemplateId() == 9833097).count();
            for (int i = 0; i < count; i++) {
                sm.spawnMob(9833097, 838, 1485, false, GameConstants.getDreamBreakerHP(stage));
            }
            count = 3 - (int) mobs.stream().filter(m -> m.getTemplateId() == 9833098).count();
            for (int i = 0; i < count; i++) {
                sm.spawnMob(9833098, -1450, -237, false, GameConstants.getDreamBreakerHP(stage));
            }
            count = 3 - (int) mobs.stream().filter(m -> m.getTemplateId() == 9833099).count();
            for (int i = 0; i < count; i++) {
                sm.spawnMob(9833099, -1450, -237, false, GameConstants.getDreamBreakerHP(stage));
            }
        }
    }

    private int getPlusPoint(int stage) {
        int plusPoint;
        if (stage < 10) {
            plusPoint = 10;
        } else if (stage >= 100) {
            plusPoint = 100;
        } else {
            plusPoint = stage - (stage % 10);
        }
        return plusPoint;
    }
}
