package net.swordie.ms.world.partyquest;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.social.Party.Party;
import net.swordie.ms.client.social.Party.PartyMember;
import net.swordie.ms.constants.GameConstants;
import net.swordie.ms.enums.WeatherEffNoticeType;
import net.swordie.ms.handlers.Timer;
import net.swordie.ms.life.mob.Mob;
import net.swordie.ms.scripts.ScriptManagerImpl;
import net.swordie.ms.world.field.Field;
import net.swordie.ms.world.field.MobGen;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

public class MoonBunny implements PartyQuest {

    private final ScriptManagerImpl sm;
    private final Char chr;
    private final Party party;
    private final int entryQuest = 1323;
    List<MobGen> mobGens = new ArrayList<>();
    List<Integer> stage1Mobs = List.of(9300058, 9300059);
    List<Integer> stage2Mobs = List.of(9300062, 9300063, 9300064);
    private ScheduledFuture<?> startEvent;
    private byte stage = 0;
    private boolean isAdded = false;

    public MoonBunny(Char chr) {
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
        if (chr.getField().getId() == GameConstants.MOON_BUNNY_STAGE) {
        } else {
            int selection = sm.sendSayOkay("<#e組隊任務：月妙的年糕#n>\r\n神秘的月妙只會在滿月時出現在#b迎月花山丘#k。#b弓箭手村公園#k的#b達爾利#k正在尋找願意為#b興兒#k找#r月妙年糕#k的冒險家。想見到月妙的話，就在指定的位置種下迎月花種子，召喚出滿月。在做出#r80 個年糕#k之前，保護月妙不被野獸攻擊。\r\n- #e等級#n：70 以上 #r(建議等級：70 - 255)#k\r\n- #e時間限制#n：10 分鐘\r\n- #e參加人數#n：3 ~ 6 人\r\n- #e獎勵#n：#v1002798# #t1002798#、性向經驗值與組隊點數#k\r\n#L0##b我要進行組隊任務。#k#l\r\n#L1##b查看今天剩餘的挑戰次數。#k#l\r\n#L2##b用年糕交換帽子。#k#l");
            switch (selection) {
                case 0:
                    if (sm.getFieldID() != GameConstants.MOON_BUNNY_ENTRANCE_MAP) {
                        sm.warp(GameConstants.MOON_BUNNY_ENTRANCE_MAP);
                    } else {
                        if (party == null) {
                            sm.sendSayOkay("必須加入#b隊伍#k才能進入月妙組隊任務。快去找朋友吧！");
                        } else if (!party.isLeader(chr)) {
                            sm.sendSayOkay("想挑戰任務的話，請讓#b隊長#k來跟我說話。");
                        } else if (sm.checkPartyForPQ()) {
                            if (isPartyEligible((short) 70, (short) 275, party)) {
                                if (sm.checkAttempt(entryQuest, party)) {
                                    chr.getParty().setPartyQuest(this);
                                    sm.setAchieveRatio(0);
                                    sm.warpInstanceIn(chr, GameConstants.MOON_BUNNY_STAGE, true);
                                    sm.setInstanceTime(10 * 60);
                                    sm.addAttempt(entryQuest, party);
                                    party.getInstance().addProperty("Moon Bunny", true);
                                    sm.showWeatherNoticeToField("必須找回被豬偷走的 6 顆迎月花種子。", WeatherEffNoticeType.RiceCakePQ);
                                    return;
                                } else {
                                    sm.sendSayOkay("有隊員已達今日挑戰次數上限。");
                                }
                            } else {
                                sm.sendSayOkay("有隊員的等級低於 70 或高於 255。");
                            }
                        }
                    }
                    break;
                case 1:
                    int count = 5;
                    if (chr.hasQuest(entryQuest)) {
                        count = 5 - Integer.parseInt(chr.getQRValueByKey(entryQuest, "count"));
                    }
                    sm.sendSayOkay("你還可以挑戰 " + count + " 次。");
                    break;
                case 2:
                    if (sm.hasItem(1003266)) {
                        if (sm.canHold(1003266)) {
                            if (sm.hasItem(GameConstants.RICE_CAKE, 20)) {
                                sm.consumeItem(GameConstants.RICE_CAKE, 20);
                                sm.giveItem(1003266);
                            } else {
                                sm.sendSayOkay("等你有 20 個年糕再來吧。");
                            }
                        } else {
                            sm.sendSayOkay("請確認裝備欄有足夠的空間。");
                        }
                    } else if (sm.hasItem(GameConstants.RICE_CAKE, 10)) {
                        if (sm.canHold(1002798)) {
                            sm.consumeItem(GameConstants.RICE_CAKE, 10);
                            sm.giveItem(1002798);
                        } else {
                            sm.sendSayOkay("請確認裝備欄有足夠的空間。");
                        }
                    } else {
                        sm.sendSayOkay("等你有 10 個年糕再來吧。");
                    }
                    break;
            }
        }
    }

    @Override
    public void exit() {
        sm.setAchieveRatio(0, false);
        sm.warpInstanceOut(chr, GameConstants.MOON_BUNNY_ENTRANCE_MAP);
    }

    @Override
    public void event() {
        if (chr.getField().getOnFirstUserEnter().equals("moonrabbit_mapEnter")) {
            startEvent = chr.getTimer().addEvent(this::initMoonBunny, 2000);
        }
    }

    public void event(int parentID) {
        if (party.isLeader(chr)) {
            if (sm.getReactorState(parentID) == 0) {
                sm.consumeItem(GameConstants.PRIMROSE_SEED);
                sm.showWeatherNoticeToField("已經種下一顆種子。", WeatherEffNoticeType.RiceCakePQ, 3000);
                sm.increaseReactorState(parentID, 0);
                sm.increaseReactorState(GameConstants.MOON_REACTOR, 0);
                if (sm.getReactorState(GameConstants.MOON_REACTOR) == 6) {
                    sm.showWeatherNoticeToField("月妙只有在沒被攻擊的時候才能搗年糕！收集 80 個月妙年糕吧！", WeatherEffNoticeType.RiceCakePQ, 7000);
                    sm.setAchieveRatio(25);
                    sm.spawnMob(GameConstants.MOON_BUNNY, -190, -187, false);

                    sm.invokeAtFixedRate(8000, 6000, 16, "dropItem", GameConstants.RICE_CAKE, -190, -187, -284, -184);
                    sm.invokeAtFixedRate(8500, 6000, 16, "dropItem", GameConstants.RICE_CAKE, -190, -187, -232, -190);
                    sm.invokeAtFixedRate(9000, 6000, 16, "dropItem", GameConstants.RICE_CAKE, -190, -187, -190, -187);
                    sm.invokeAtFixedRate(9500, 6000, 16, "dropItem", GameConstants.RICE_CAKE, -190, -187, -150, -180);
                    sm.invokeAtFixedRate(10000, 6000, 16, "dropItem", GameConstants.RICE_CAKE, -190, -187, -89, -180);
                    stage = 1;
                    isAdded = false;
                }
            }
        } else {
            sm.chatScript("只有隊長可以執行此操作。");
        }
    }

    @Override
    public void end(Char chr) {
        if (startEvent != null) {
            startEvent.cancel(false);
        }
        ScriptManagerImpl sm = chr.getScriptManager();
        sm.setAchieveRatio(0);
        sm.warpInstanceOut(chr, GameConstants.MOON_BUNNY_ENTRANCE_MAP);
    }

    @Override
    public void clear(Char chr) {
    }

    @Override
    public void leftOrDisband(Char chr) {
        end(chr);
    }

    public void initMoonBunny() {
        Party party = chr.getParty();
        if (party.getInstance() == null || !party.getInstance().hasProperty("Moon Bunny")) {
            startEvent.cancel(false);
            return;
        }
        Field field = chr.getField();
        if (!isAdded) {
            switch (stage) {
                case 0:
                    for (MobGen mobGen : mobGens) {
                        if (stage1Mobs.contains(mobGen.getMob().getTemplateId())) {
                            field.addLife(mobGen);
                        }
                    }
                    isAdded = true;
                    break;
                case 1:
                    for (Mob mob : field.getMobs()) {
                        if (stage1Mobs.contains(mob.getTemplateId())) {
                            mob.remove();
                        }
                    }
                    for (MobGen mobGen : mobGens) {
                        if (stage1Mobs.contains(mobGen.getMob().getTemplateId())) {
                            field.removeLife(mobGen);
                        } else if (stage2Mobs.contains(mobGen.getMob().getTemplateId())) {
                            field.addLife(mobGen);
                        }
                    }
                    isAdded = true;
                    break;
            }
        } else if (isAdded && stage == 1) {
            if (!sm.hasMobById(GameConstants.MOON_BUNNY)) {
                end(chr);
            }
        }
    }
}
