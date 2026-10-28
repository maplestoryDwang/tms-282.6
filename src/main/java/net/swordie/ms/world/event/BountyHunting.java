package net.swordie.ms.world.event;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.packet.PoloFritoPacket;
import net.swordie.ms.enums.WeatherEffNoticeType;
import net.swordie.ms.handlers.Timer;
import net.swordie.ms.scripts.ScriptManagerImpl;
import net.swordie.ms.util.Randomizer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

public class BountyHunting {

    private int stage;
    private ScheduledFuture<?> startTimer;
    private List<List<Integer>> waveData = new ArrayList<>();

    public BountyHunting(int stage) {
        this.stage = stage;
    }

    public int getStage() {
        return this.stage;
    }

    public List<List<Integer>> getWaveData() {
        return this.waveData;
    }

    public void updateDefenseWave(Char chr) {
        chr.write(PoloFritoPacket.setBountyHuntingStage(getStage()));
    }

    public void updateNewWave(Char chr) {
        ScriptManagerImpl sm = chr.getScriptManager();
        sm.showEffectToField("Map/Effect.img/defense/count");
        if (chr.getField().getId() == 993000000) {
            sm.showEffectToField("Map/Effect.img/defense/wave/" + getStage());
            sm.showEffectToField("Map/Effect.img/killing/first/start");
            for (Integer wave : getWaveData().get(getStage() - 1)) {
                sm.spawnMob(wave, Randomizer.rand(500, 700) * -1, 126, false);
                sm.spawnMob(wave, Randomizer.rand(500, 700), 126, false);
            }
        }
    }

    public void checkFinish(Char chr) {
        ScriptManagerImpl sm = chr.getScriptManager();
        if (chr.getField() != null && chr.getField().getMobs().size() == 0) {
            if (getStage() < 5 && chr.getField().getId() == 993000000) {
                this.stage++;
                updateDefenseWave(chr);
                updateNewWave(chr);
            } else {
                startTimer.cancel(false);
                sm.showEffectToField("Map/Effect.img/killing/clear");
                chr.getTimer().addEvent(() -> {
                    if (chr.getField().getId() == 993000000) {
                        sm.warpNoReturn(993000600, 0);
                    }
                }, 2000);
            }
        }
    }

    public void insertWaveData() {
        List<Integer> waves = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            waves.add(9830000);
        }
        for (int i = 0; i < 2; i++) {
            waves.add(9830001);
        }
        getWaveData().add(waves);
        List<Integer> waves2 = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            waves2.add(9830002);
        }
        for (int i = 0; i < 2; i++) {
            waves2.add(9830004);
        }
        for (int i = 0; i < 5; i++) {
            waves2.add(9830003);
        }
        getWaveData().add(waves2);
        List<Integer> waves3 = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            waves3.add(9830005);
        }
        for (int i = 0; i < 2; i++) {
            waves3.add(9830006);
        }
        for (int i = 0; i < 5; i++) {
            waves3.add(9830007);
        }
        waves3.add(9830008);
        getWaveData().add(waves3);
        List<Integer> waves4 = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            waves4.add(9830009);
        }
        for (int i = 0; i < 5; i++) {
            waves4.add(9830010);
        }
        for (int i = 0; i < 5; i++) {
            waves4.add(9830011);
        }
        for (int i = 0; i < 5; i++) {
            waves4.add(9830012);
        }
        waves4.add(9830013);
        getWaveData().add(waves4);
        List<Integer> waves5 = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            waves5.add(9830014);
        }
        for (int i = 0; i < 5; i++) {
            waves5.add(9830015);
        }
        for (int i = 0; i < 5; i++) {
            waves5.add(9830016);
        }
        for (int i = 0; i < 5; i++) {
            waves5.add(9830017);
        }
        waves5.add(9830018);
        getWaveData().add(waves5);
    }

    public void start(Char chr) {
        ScriptManagerImpl sm = chr.getScriptManager();
        updateDefenseWave(chr);
        insertWaveData();
        sm.showWeatherNotice("牠們從四面八方湧過來了！打倒牠們就能獲得大量經驗值！", WeatherEffNoticeType.Pollo);
        sm.createClock(180);
        updateNewWave(chr);
        startTimer = chr.getTimer().addEvent(() -> {
            if (chr.getField().getId() == 993000000) {
                sm.warpNoReturn(993000600, 0);
            }
        }, 180000L);
    }
}
