package net.swordie.ms.world.partyquest;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.social.Party.Party;
import net.swordie.ms.client.social.Party.PartyMember;
import net.swordie.ms.connection.packet.MultiStagePacket;
import net.swordie.ms.constants.GameConstants;
import net.swordie.ms.handlers.GlobalTimerManager;
import net.swordie.ms.scripts.ScriptManagerImpl;
import net.swordie.ms.util.Position;
import net.swordie.ms.util.Util;
import net.swordie.ms.world.field.Field;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

public class DimensionInvasion implements PartyQuest {

    private final ScriptManagerImpl sm;
    private final Char chr;
    private final Party party;
    private final int entryQuest = 1322;

    private byte wave = 0;
    private byte stage = 0;
    private byte currentDIStage = 0;

    public DimensionInvasion(Char chr) {
        this.sm = chr.getScriptManager();
        this.chr = chr;
        this.party = chr.getParty();
    }

    private boolean isPartyEligible(short lowLevel, short highLevel, Party party) {
        for (PartyMember member : party.getMembers()) {
            if (member.getLevel() < lowLevel || member.getLevel() > highLevel) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void start() {
        int selection = sm.sendNext("有一群來自楓之谷世界的傢伙入侵了格蘭蒂斯。我們得查出是誰幹的，然後把他們收拾掉。\r\n\r\n#b#L0#進入次元入侵#l\r\n#L1#領取道具#l\r\n#L2#次元入侵是什麼？#l\r\n#L3#我參加過幾次次元入侵？#l");
        switch (selection) {
            case 0:
                if (party == null) {
                    sm.sendSayOkay("那裡可是戰場，不是遊樂場。我需要一支 4 人以下的小隊潛入那個區域，從後方瓦解他們的攻勢。組好隊伍、準備好了再來找我。");
                } else if (!party.isLeader(chr)) {
                    sm.sendSayOkay("想挑戰任務的話，請讓#b隊長#k來跟我說話。");
                } else if (sm.checkPartyForPQ()) {
                    if (isPartyEligible((short) 140, (short) 275, party)) {
                        if (sm.checkAttempt(entryQuest, party)) {
                            sm.warpInstanceIn(chr, GameConstants.DIMENSIONAL_INVASION_MAIN_STAGE, true);
                            sm.setInstanceTime(60 * 60);
                            chr.getParty().setPartyQuest(this);
                            sm.addAttempt(entryQuest, party);
                            return;
                        } else {
                            sm.sendSayOkay("有隊員已達今日挑戰次數上限。");
                        }
                    } else {
                        sm.sendSayOkay("有隊員的等級低於 140 或高於 275。");
                    }
                }
                break;
            case 1:
                sm.sendNext("你沒有可以領取的道具。你確定參加過次元入侵了嗎？");
                break;
            case 2:
                sm.sendNext("這是一場跨越次元的戰鬥。有人在楓之谷世界和格蘭蒂斯之間開了一個洞，現在宇宙各個角落的混蛋都想來佔領這裡。光是梅格耐斯就已經夠糟了……");
                sm.sendSay("進去之後要擋下五波敵人。要是讓太多敵人突破，我們就全輸了。");
                sm.sendSay("啊，差點忘了。通過次元入侵後就能進入隱藏關卡，在那裡會經歷新的故事、遇見新的敵人。至於是什麼樣的敵人？沒有人知道。");
                sm.sendSay("每個階段的獎勵都不一樣，隱藏關卡更是特別。你拯救我們的次元還能拿到戰利品，我卻只能面對一群只想拿好處的傻瓜。真羨慕你……");
                break;
            case 3:
                int count = 5;
                if (chr.hasQuest(entryQuest)) {
                    count = 5 - Integer.parseInt(chr.getQRValueByKey(entryQuest, "count"));
                }
                sm.sendSayOkay("你還可以挑戰 " + count + " 次。");
                break;
        }
    }

    @Override
    public void exit() {
        sm.setSpeakerID(9020009);
        if (sm.sendAskYesNo("已經要放棄了嗎？")) {
            sm.warpInstanceOut(chr, GameConstants.DIMENSIONAL_INVASION_ENTRANCE_MAP);
        }
    }

    @Override
    public void event() {
        if (chr == null || chr.getParty() == null || !chr.getParty().isLeader(chr)) {
            return;
        }
        chr.getField().setDropsDisabled(true);
        sm.showEffectToField("Map/Effect.img/BestClass/start");

        ScheduledFuture<?> sf = chr.getTimer().addFixedRateEvent(this::initDIStage, 5000, getDIDelayWaveByStage(stage), false);
        chr.setStartEventTimer(sf);
        GlobalTimerManager.addCharTimer(chr.getId(), sf);

        ScheduledFuture<?> sf2 = chr.getTimer().addFixedRateEvent(this::completeDIStage, 5000, 1000, false);
        chr.setEndEventTimer(sf2);
        GlobalTimerManager.addCharTimer(chr.getId(), sf2);
    }

    @Override
    public void end(Char chr) {
        if (chr.getStartEventTimer() != null) {
            chr.getStartEventTimer().cancel(false);
        }
        if (chr.getEndEventTimer() != null) {
            chr.getEndEventTimer().cancel(false);
        }
        ScriptManagerImpl sm = chr.getScriptManager();
        sm.warpInstanceOut(chr, GameConstants.DIMENSIONAL_INVASION_ENTRANCE_MAP);
    }

    @Override
    public void clear(Char chr) {
        this.stage = 0;
        this.currentDIStage = 0;
        for (Char player : party.getOnlineChars()) {
            player.addItemToInventory(2431127, 1);
        }
        end(chr);
    }

    @Override
    public void leftOrDisband(Char chr) {
        end(chr);
    }

    public boolean incMonsterGauge(int amount) {
        Field field = chr.getField();
        int inc = field.getMonsterGauge() + amount;
        if (inc > 100) {
            end(chr);
            return false;
        } else {
            field.incMonsterGauge(amount);
            field.broadcast(MultiStagePacket.setMonsterGauge(100, field.getMonsterGauge()));
        }
        return true;
    }

    public void setDIStage(Field field, byte stage) {
        currentDIStage = stage;
        field.broadcast(MultiStagePacket.setStage(field.getId(), stage));
    }

    public void initDIStage() {
        if (chr.getField().getId() != GameConstants.DIMENSIONAL_INVASION_MAIN_STAGE) {
            if (chr.getStartEventTimer() != null) {
                chr.getStartEventTimer().cancel(false);
            }
            if (chr.getEndEventTimer() != null) {
                chr.getEndEventTimer().cancel(false);
            }
            return;
        }
        if (currentDIStage != stage) {
            setDIStage(chr.getField(), stage);
        }
        if (wave >= getDIWaveByStage(stage)) {
            return;
        }
        int avgLevel = sm.getParty().getAvgPartyLevel();
        switch (stage) {
            case 0:
                if (incMonsterGauge(70)) {
                    int SpearmanID;
                    if (avgLevel >= 140 && avgLevel < 150) {
                        SpearmanID = 2500130;
                    } else if (avgLevel >= 150 && avgLevel < 160) {
                        SpearmanID = 2500131;
                    } else if (avgLevel >= 160 && avgLevel < 170) {
                        SpearmanID = 2500132;
                    } else {
                        SpearmanID = 2500133;
                    }
                    for (int i = 0; i < 8; i++) {
                        sm.spawnMob(SpearmanID, 2700, 29, false);
                    }
                    int BloodFangID;
                    if (avgLevel >= 140 && avgLevel < 150) {
                        BloodFangID = 2500800;
                    } else if (avgLevel >= 150 && avgLevel < 160) {
                        BloodFangID = 2500801;
                    } else if (avgLevel >= 160 && avgLevel < 170) {
                        BloodFangID = 2500802;
                    } else {
                        BloodFangID = 2500803;
                    }
                    for (int i = 0; i < 2; i++) {
                        sm.spawnMob(BloodFangID, 2600, 29, false);
                    }
                    wave++;
                }
                break;
            case 1:
                if (incMonsterGauge(70)) {
                    int LargeWoodenHorseID = 9300622;
                    long HP = 400L * avgLevel;
                    long EXP = (long) ((GameConstants.charExp[avgLevel] * 0.16D) / 100.0D);
                    chr.getField().spawnMob(LargeWoodenHorseID, 2700, 29, false, HP, 0, 0, 100, 100, EXP);
                    wave++;
                }
                break;
            case 2:
                if (incMonsterGauge(25)) {
                    int SkeletrooperID;
                    if (avgLevel >= 140 && avgLevel < 150) {
                        SkeletrooperID = 2500030;
                    } else if (avgLevel >= 150 && avgLevel < 160) {
                        SkeletrooperID = 2500031;
                    } else if (avgLevel >= 160 && avgLevel < 170) {
                        SkeletrooperID = 2500032;
                    } else {
                        SkeletrooperID = 2500033;
                    }
                    List<Position> randPos = new ArrayList<>() {
                        {
                            add(new Position(2700, 29));
                            add(new Position(3330, 29));
                            add(new Position(1980, -451));
                        }
                    };
                    Position pos = Util.getRandomFromCollection(randPos);
                    for (int i = 0; i < 25; i++) {
                        sm.spawnMob(SkeletrooperID, pos.getX(), pos.getY(), false);
                    }
                    wave++;
                }
                break;
            case 3:
                if (incMonsterGauge(60)) {
                    int GargoyleKnightID = 9300621;
                    long HP = 100L * avgLevel;
                    int PADMAD = avgLevel * 150;
                    long EXP = (long) ((GameConstants.charExp[avgLevel] * 0.035D) / 100.0D);
                    for (int i = 0; i < 20; i++) {
                        chr.getField().spawnMob(GargoyleKnightID, 2700, 29, false, HP, PADMAD, PADMAD, 0, 0, EXP);
                    }
                    for (int i = 0; i < 20; i++) {
                        chr.getField().spawnMob(GargoyleKnightID, 2070, -451, false, HP, PADMAD, PADMAD, 0, 0, EXP);
                    }
                    for (int i = 0; i < 20; i++) {
                        chr.getField().spawnMob(GargoyleKnightID, 3330, -451, false, HP, PADMAD, PADMAD, 0, 0, EXP);
                    }
                    wave++;
                }
                break;
            case 4:
                if (incMonsterGauge(70)) {
                    int ObeliskShieldID = 9300634;
                    long HP = 200L * avgLevel;
                    long EXP = (long) ((GameConstants.charExp[avgLevel] * 0.59D) / 100.0D);
                    chr.getField().spawnMob(ObeliskShieldID, 2700, 29, false, HP, 0, 0, 100, 100, EXP);
                    wave++;
                }
                break;
        }
    }

    public void completeDIStage() {
        if (wave == getDIWaveByStage(stage)) {
            if (chr.getField().getMobs().size() == 0) {
                if (chr.getStartEventTimer() != null) {
                    chr.getStartEventTimer().cancel(false);
                }
                ScheduledFuture<?> sf = chr.getTimer().addFixedRateEvent(this::initDIStage, 2000, getDIDelayWaveByStage(stage), false);
                chr.setStartEventTimer(sf);
                GlobalTimerManager.addCharTimer(chr.getId(), sf);

                sm.showClearStageExpWindowToParty(getDIExpByStage(stage));
                stage++;
                wave = 0;

                if (stage > 4) {
                    clear(chr);
                }
            }
        }
    }

    public byte getDIWaveByStage(byte stage) {
        switch (stage) {
            case 0:
                return 7;
            case 1:
            case 4:
                return 1;
            case 2:
                return 3;
            case 3:
                return 2;
            default:
                return 0;
        }
    }

    public long getDIDelayWaveByStage(byte stage) {
        switch (stage) {
            case 0:
            case 2:
                return 7000;
            case 1:
            case 4:
                return 5000;
            case 3:
                return 20000;
            default:
                return 0;
        }
    }

    public long getDIExpByStage(byte stage) {
        switch (stage) {
            case 0:
                return 284874;
            case 1:
                return 439941;
            case 2:
                return 583992;
            case 3:
                return 728044;
            case 4:
                return 875989;
            default:
                return 0;
        }
    }
}