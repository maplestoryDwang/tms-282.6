package net.swordie.ms.world.event;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.packet.PoloFritoPacket;
import net.swordie.ms.enums.WeatherEffNoticeType;
import net.swordie.ms.handlers.Timer;
import net.swordie.ms.life.Summon;
import net.swordie.ms.scripts.ScriptManagerImpl;
import net.swordie.ms.util.Randomizer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

public class FrittoDancing {

    private int state;
    private ScheduledFuture<?> sc;
    private ScheduledFuture<?> startTimer;
    private List<List<Integer>> waveData = new ArrayList<>();

    public FrittoDancing(int state) {
        this.state = state;
    }

    public void updateDefenseWave(Char chr) {
        chr.write(PoloFritoPacket.courtShipDanceState(getState()));
    }

    public void updateNewWave(Char chr) {
        ScriptManagerImpl sm = chr.getScriptManager();
        sm.showEffectToField("Map/Effect.img/defense/count");
        chr.getTimer().addEvent(() -> {
            if (chr.getField().getId() == 993000400) {
                sm.showEffectToField("Map/Effect.img/first/start");
            }
        }, 3000);
        chr.getTimer().addEvent(() -> {
            if (chr.getField().getId() == 993000400) {
                chr.write(PoloFritoPacket.courtShipDanceCommand(getWaveData()));
            }
        }, 4000);
    }

    public void finish(Char chr) {
        if (sc != null) {
            sc.cancel(false);
        }
        ScriptManagerImpl sm = chr.getScriptManager();
        sm.showEffectToField("Map/Effect.img/killing/clear");
        sm.createStopWatch(3);
        chr.getTimer().addEvent(() -> {
            startTimer.cancel(false);
            sm.lockInGameUI(false, false);
            sm.hideUser(false);
            sm.warpNoReturn(993000601, 0);
        }, 3000);
    }

    public void insertWaveData() {
        List<Integer> waves = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            waves.add(Randomizer.nextInt(4));
        }
        getWaveData().add(waves);
        List<Integer> waves2 = new ArrayList<>();
        for (int j = 0; j < 6; j++) {
            waves2.add(Randomizer.nextInt(4));
        }
        getWaveData().add(waves2);
        for (int i = 0; i < 3; i++) {
            List<Integer> waves3 = new ArrayList<>();
            for (int j = 0; j < 7; j++) {
                waves3.add(Randomizer.nextInt(4));
            }
            getWaveData().add(waves3);
        }
        for (int i = 0; i < 3; i++) {
            List<Integer> waves4 = new ArrayList<>();
            for (int j = 0; j < 8; j++) {
                waves4.add(Randomizer.nextInt(4));
            }
            getWaveData().add(waves4);
        }
        for (int i = 0; i < 2; i++) {
            List<Integer> waves5 = new ArrayList<>();
            for (int j = 0; j < 10; j++) {
                waves5.add(Randomizer.nextInt(4));
            }
            getWaveData().add(waves5);
        }
    }

    public void start(Char chr) {
        ScriptManagerImpl sm = chr.getScriptManager();
        for (Summon su : chr.getField().getSummonsByChar(chr)) {
            chr.getField().removeSummon(su.getSkillID(), chr.getId());
            chr.getTemporaryStatManager().removeStatsBySkill(su.getSkillID());
        }
        updateDefenseWave(chr);
        sm.lockInGameUI(true, false);
        sm.hideUser(true);
        sm.OnOffLayer_On(500, "0", 0, 380, 0, "Map/Effect.img/PoloFritto/msg3", 4, 1, -1, 0);
        sm.showWeatherNotice("要騙過雞才能偷到蛋！來吧，跟著我跳求偶舞！", WeatherEffNoticeType.Fritto);
        insertWaveData();
        updateNewWave(chr);
        startTimer = chr.getTimer().addEvent(() -> {
            if (chr.hasQuest(15143)) {
                chr.setQRValueByKey(15143, "score", "0");
            } else {
                chr.createQuestWithQRValue(15143, "score=0");
            }
            sm.createStopWatch(60);
            sc = chr.getTimer().addEvent(() -> {
                startTimer.cancel(false);
                if (chr.getField().getId() == 993000400) {
                    sm.lockInGameUI(false, false);
                    sm.hideUser(false);
                    sm.warpNoReturn(993000601, 0);
                }
            }, 60000);
        }, 4000);
    }

    public List<List<Integer>> getWaveData() {
        return this.waveData;
    }

    public void setWaveData(List<List<Integer>> waveData) {
        this.waveData = waveData;
    }

    public Integer getState() {
        return state;
    }
}
