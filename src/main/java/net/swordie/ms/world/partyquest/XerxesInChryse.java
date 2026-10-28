package net.swordie.ms.world.partyquest;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.social.Party.Party;
import net.swordie.ms.client.social.Party.PartyMember;
import net.swordie.ms.connection.packet.TemporarySkillMan;
import net.swordie.ms.constants.GameConstants;
import net.swordie.ms.life.mob.Mob;
import net.swordie.ms.scripts.ScriptManagerImpl;
import net.swordie.ms.world.field.Field;
import net.swordie.ms.world.field.MobGen;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class XerxesInChryse implements PartyQuest {

    private final ScriptManagerImpl sm;
    private final Char chr;
    private final Party party;
    private final int entryQuest = 1314;
    private List<Mob> mobs = new ArrayList<>();
    private List<ScheduledFuture> mobTimers = new ArrayList<>();

    public XerxesInChryse(Char chr) {
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
        sm.setSpeakerID(2170016);
        int selection = sm.sendNext("#e<組隊任務：克里塞的薛西斯>#n\r\n有人看到我的寵物嗎？牠又可愛又毛茸茸的，還有一支聽命於牠的可怕軍隊。\r\n\r\n" +
                "#b" +
                "#L0#進入克里塞組隊任務。#l\r\n" +
                "#L1#聽米加爾拉的故事。#l\r\n" +
                "#L2#領取獎勵。#l\r\n" +
                "#L3#查看今天剩餘的挑戰次數。#l#k");
        switch (selection) {
            case 0:
                if (party == null) {
                    sm.sendSayOkay("沒有加入隊伍就無法進入，請加入隊伍後再試一次。");
                } else if (!party.isLeader(chr)) {
                    sm.sendSayOkay("想挑戰任務的話，請讓#b隊長#k來跟我說話。");
                } else if (sm.checkPartyForPQ()) {
                    if (isPartyEligible((short) 110, (short) 275, party)) {
                        if (sm.checkAttempt(entryQuest, party)) {
                            sm.warpInstanceIn(chr, GameConstants.XERXES_CHRYSE_FRONT_FIELD, true);
                            sm.setInstanceTime(GameConstants.XERXES_CHRYSE_TIME, 910002000);
                            party.setPartyQuest(this);
                            chr.getField().broadcast(TemporarySkillMan.setTemporarySkillSet(80001705, 6));
                            sm.addAttempt(entryQuest, party);
                            return;
                        } else {
                            sm.sendSayOkay("有隊員已達今日挑戰次數上限。");
                        }
                    } else {
                        sm.sendSayOkay("有隊員的等級低於 110。");
                    }
                }
                break;
            case 1:
                sm.sendNext("我的寵物叫薛西斯，是全世界最柔軟、最可愛的小山羊。嗯……至少在牠開始招募大軍之前是這樣。");
                sm.sendSay("我想牠在競技場塔那裡又是蓋東西又是散播恐懼，一定沒在做什麼好事。請去看看我的寶貝寵物在那裡做什麼。\r\n\r\n" +
                        "- #e等級：#n 110 以上 #r(建議等級：110 - 129)#k\r\n" +
                        "- #e時間限制：#n 20 分鐘\r\n" +
                        "- #e人數：#n 1 - 4 人\r\n" +
                        "- #e獎勵：#n\r\n\r\n#i1022245# #z1022245#\r\n#i1022246# #z1022246#");
                break;
            case 2:
                if (sm.hasItem(4001844, 80)) {
                    int sel2 = sm.sendNext("你想要哪一個？\r\n\r\n#b#L0##i1022245# #z1022245##l\r\n#L1##i1022246# #z1022246##l#k");
                    if (sm.canHold(1022245) && sel2 == 0) {
                        sm.consumeItem(4001844, 80);
                        sm.giveItem(1022245);
                    } else if (sm.canHold(1022246) && sel2 == 1) {
                        sm.consumeItem(4001844, 80);
                        sm.giveItem(1022246);
                    }
                } else {
                    sm.sendNext("嗯……#h #，你有 #r#c4001844##k 個#t4001844#。至少要帶回 #r80#k 個才能領取獎勵。");
                }
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
        if (sm.getFieldID() == GameConstants.XERXES_CHRYSE_ENTRANCE_MAP) {
            if (sm.sendAskYesNo("要放棄並跟我一起回去嗎？")) {
                if (sm.sendAskYesNo("真的嗎？這樣不太像英雄喔……\r\n#r(按「是」將放棄進度並離開。)#k")) {
                    sm.warpInstanceOut(chr, 920012600);
                }
            }
        } else if (sm.getFieldID() == GameConstants.XERXES_CHRYSE_EXIT) {
            sm.sendNext("#h #！我那麼相信你，你卻這麼輕易就放棄了。不過我相信這只是一時的，希望你改天再回來幫忙。");
            int sel = sm.sendNext("我可以帶你回到下面其中一個地方。\r\n\r\n#b#L0#挑戰<克里塞的薛西斯>#l\r\n#L1#返回#e#m910002000##n#l#k");
            if (sel == 0) {
                start();
            } else if (sel == 1) {
                sm.sendNext("我馬上送你到#m910002000#。");
                sm.warpInstanceOut(chr, 910002000);
            }
        }
    }

    @Override
    public void event() {
        Field field = chr.getField();
        this.mobs = field.getMobs().stream().toList();
        this.mobTimers.clear();
        for (Mob mob : field.getMobs()) {
            mob.removeWithAnimation();
        }
        for (MobGen mobGen : field.getMobGens()) {
            field.removeLife(mobGen);
        }
        initStage();
    }

    private void initStage() {
        // Stage 1:
        ScheduledFuture<?> s1 = chr.getTimer().addEvent(() -> {
            sm.showEffectToField("Map/Effect.img/kreasePQ/stage1");
            for (int i = 0; i < 45; i++) {
                if (mobs.size() > i) {
                    Mob m = mobs.get(i);
                    sm.spawnMob(m.getTemplateId(), m.getX(), m.getY(), false);
                }
            }
        }, 1, TimeUnit.SECONDS);
        this.mobTimers.add(s1);
        // Stage 2:
        ScheduledFuture<?> s2 = chr.getTimer().addEvent(() -> {
            if (sm.getFieldID() != GameConstants.XERXES_CHRYSE_FRONT_FIELD) {
                this.mobTimers.clear();
                return;
            }
            sm.showEffectToField("Map/Effect.img/kreasePQ/stage2");
            for (int i = 0; i < 45; i++) {
                if (mobs.size() > i) {
                    Mob m = mobs.get(i);
                    sm.spawnMob(m.getTemplateId(), m.getX(), m.getY(), false);
                }
            }
        }, 5, TimeUnit.SECONDS);
        this.mobTimers.add(s2);
        // Stage 3:
        ScheduledFuture<?> s3 = chr.getTimer().addEvent(() -> {
            if (sm.getFieldID() != GameConstants.XERXES_CHRYSE_FRONT_FIELD) {
                this.mobTimers.clear();
                return;
            }
            sm.showEffectToField("Map/Effect.img/kreasePQ/stage3");
            for (int i = 0; i < 45; i++) {
                if (mobs.size() > i) {
                    Mob m = mobs.get(i);
                    sm.spawnMob(m.getTemplateId(), m.getX(), m.getY(), false);
                }
            }
        }, 15, TimeUnit.SECONDS);
        this.mobTimers.add(s3);
        // Stage 4:
        ScheduledFuture<?> s4 = chr.getTimer().addEvent(() -> {
            if (sm.getFieldID() != GameConstants.XERXES_CHRYSE_FRONT_FIELD) {
                this.mobTimers.clear();
                return;
            }
            sm.showEffectToField("Map/Effect.img/kreasePQ/stage4");
            for (int i = 0; i < 45; i++) {
                if (mobs.size() > i) {
                    Mob m = mobs.get(i);
                    sm.spawnMob(m.getTemplateId(), m.getX(), m.getY(), false);
                }
            }
        }, 25, TimeUnit.SECONDS);
        this.mobTimers.add(s4);
        // Stage 5:
        ScheduledFuture<?> s5 = chr.getTimer().addEvent(() -> {
            if (sm.getFieldID() != GameConstants.XERXES_CHRYSE_FRONT_FIELD) {
                this.mobTimers.clear();
                return;
            }
            sm.showEffectToField("Map/Effect.img/kreasePQ/stage5");
            for (int i = 0; i < 45; i++) {
                if (mobs.size() > i) {
                    Mob m = mobs.get(i);
                    sm.spawnMob(m.getTemplateId(), m.getX(), m.getY(), false);
                }
            }
        }, 35, TimeUnit.SECONDS);
        this.mobTimers.add(s5);
        // Stage 6:
        ScheduledFuture<?> s6 = chr.getTimer().addEvent(() -> {
            if (sm.getFieldID() != GameConstants.XERXES_CHRYSE_FRONT_FIELD) {
                this.mobTimers.clear();
                return;
            }
            sm.showEffectToField("Map/Effect.img/kreasePQ/stage6");
            for (int i = 0; i < 70; i++) {
                if (mobs.size() > i) {
                    Mob m = mobs.get(i);
                    sm.spawnMob(m.getTemplateId(), m.getX(), m.getY(), false);
                }
            }
        }, 50, TimeUnit.SECONDS);
        this.mobTimers.add(s6);
        // Stage 7:
        ScheduledFuture<?> s7 = chr.getTimer().addEvent(() -> {
            if (sm.getFieldID() != GameConstants.XERXES_CHRYSE_FRONT_FIELD) {
                this.mobTimers.clear();
                return;
            }
            sm.showEffectToField("Map/Effect.img/kreasePQ/stage7");
            for (int i = 0; i < 70; i++) {
                if (mobs.size() > i) {
                    Mob m = mobs.get(i);
                    sm.spawnMob(m.getTemplateId(), m.getX(), m.getY(), false);
                }
            }
        }, 65, TimeUnit.SECONDS);
        this.mobTimers.add(s7);
        // Stage 8:
        ScheduledFuture<?> s8 = chr.getTimer().addEvent(() -> {
            if (sm.getFieldID() != GameConstants.XERXES_CHRYSE_FRONT_FIELD) {
                this.mobTimers.clear();
                return;
            }
            sm.showEffectToField("Map/Effect.img/kreasePQ/stage8");
            for (int i = 0; i < 70; i++) {
                if (mobs.size() > i) {
                    Mob m = mobs.get(i);
                    sm.spawnMob(m.getTemplateId(), m.getX(), m.getY(), false);
                }
            }
        }, 80, TimeUnit.SECONDS);
        this.mobTimers.add(s8);
        // Stage 9:
        ScheduledFuture<?> s9 = chr.getTimer().addEvent(() -> {
            if (sm.getFieldID() != GameConstants.XERXES_CHRYSE_FRONT_FIELD) {
                this.mobTimers.clear();
                return;
            }
            sm.showEffectToField("Map/Effect.img/kreasePQ/stage9");
            for (int i = 0; i < 70; i++) {
                if (mobs.size() > i) {
                    Mob m = mobs.get(i);
                    sm.spawnMob(m.getTemplateId(), m.getX(), m.getY(), false);
                }
            }
        }, 105, TimeUnit.SECONDS);
        this.mobTimers.add(s9);
        // Stage 10:
        ScheduledFuture<?> s10 = chr.getTimer().addEvent(() -> {
            if (sm.getFieldID() != GameConstants.XERXES_CHRYSE_FRONT_FIELD) {
                this.mobTimers.clear();
                return;
            }
            sm.showEffectToField("Map/Effect.img/kreasePQ/stage10");
            for (int i = 0; i < 70; i++) {
                if (mobs.size() > i) {
                    Mob m = mobs.get(i);
                    sm.spawnMob(m.getTemplateId(), m.getX(), m.getY(), false);
                }
            }
        }, 130, TimeUnit.SECONDS);
        this.mobTimers.add(s10);
        // Final Stage:
        ScheduledFuture<?> sFinal = chr.getTimer().addEvent(() -> {
            if (sm.getFieldID() != GameConstants.XERXES_CHRYSE_FRONT_FIELD) {
                this.mobTimers.clear();
                return;
            }
            sm.showEffectToField("Map/Effect.img/kreasePQ/stageFinal");
            for (int i = 0; i < 70; i++) {
                if (mobs.size() > i) {
                    Mob m = mobs.get(i);
                    sm.spawnMob(m.getTemplateId(), m.getX(), m.getY(), false);
                }
            }
            for (int i = 0; i < 70; i++) {
                if (mobs.size() > i) {
                    Mob m = mobs.get(i);
                    sm.spawnMob(m.getTemplateId(), m.getX(), m.getY(), false);
                }
            }
        }, 150, TimeUnit.SECONDS);
        this.mobTimers.add(sFinal);
    }

    @Override
    public void end(Char chr) {
        for (ScheduledFuture<?> sf : mobTimers) {
            if (sf != null) {
                sf.cancel(false);
            }
        }
        ScriptManagerImpl sm = chr.getScriptManager();
        sm.warpInstanceOut(chr, GameConstants.XERXES_CHRYSE_EXIT);
        if (chr.getParty() != null) {
            chr.getParty().broadcast(TemporarySkillMan.setTemporarySkillSet(0, 0));
        } else {
            chr.write(TemporarySkillMan.setTemporarySkillSet(0, 0));
        }
    }

    @Override
    public void clear(Char chr) {
    }

    @Override
    public void leftOrDisband(Char chr) {
        end(chr);
    }
}
