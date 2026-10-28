package net.swordie.ms.world.event;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.packet.PoloFritoPacket;
import net.swordie.ms.enums.WeatherEffNoticeType;
import net.swordie.ms.handlers.Timer;
import net.swordie.ms.scripts.ScriptManagerImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

public class DefenseTowerWave {

    private int wave;
    private int life;
    private ScheduledFuture<?> startTimer;
    private List<List<Integer>> waveData = new ArrayList<>();

    public DefenseTowerWave(int wave, int life) {
        this.wave = wave;
        this.life = life;
    }

    public int getWave() {
        return wave;
    }

    public int getLife() {
        return life;
    }

    public List<List<Integer>> getWaveData() {
        return waveData;
    }

    public void updateDefenseWave(Char chr) {
        chr.write(PoloFritoPacket.setTowerDefenseWave(getWave()));
    }

    public void updateDefenseLife(Char chr) {
        chr.write(PoloFritoPacket.setTowerDefenseLife(getLife()));
    }

    public void updateNewWave(Char chr) {
        ScriptManagerImpl sm = chr.getScriptManager();
        sm.showEffectToField("Map/Effect.img/defense/count");
        sm.showEffectToField("Map/Effect.img/defense/wave/" + getWave());
        sm.showEffectToField("Map/Effect.img/killing/first/start");
        int time = 0;
        if (chr.getField().getId() == 993000100) {
            for (Integer wave : getWaveData().get(getWave() - 1)) {
                time++;
                chr.getTimer().addEvent(() -> {
                    if (chr.getField().getId() == 993000100) {
                        sm.spawnMob(wave, 363, 165, false);
                        sm.spawnMob(wave, 332, -195, false);
                    }
                }, (1000L * time));
            }
        }
    }

    public void checkFinish(Char chr) {
        ScriptManagerImpl sm = chr.getScriptManager();
        if (chr.getField() != null && chr.getField().getMobs().size() == 0) {
            if (getWave() < 3 && chr.getField().getId() == 993000100) {
                this.wave++;
                updateDefenseWave(chr);
                updateNewWave(chr);
            } else {
                startTimer.cancel(false);
                sm.showEffectToField("Map/Effect.img/killing/clear");
                chr.getTimer().addEvent(() -> {
                    if (chr.getField().getId() == 993000100) {
                        sm.warpNoReturn(993000600, 0);
                    }
                }, 2000);
            }
        }
    }

    public void insertWaveData() {
        List<Integer> waves = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            waves.add(9831000);
        }
        for (int i = 0; i < 2; i++) {
            waves.add(9831002);
        }
        for (int i = 0; i < 5; i++) {
            waves.add(9831001);
        }
        getWaveData().add(waves);
        List<Integer> waves2 = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            waves2.add(9831006);
        }
        for (int i = 0; i < 2; i++) {
            waves2.add(9831008);
        }
        for (int i = 0; i < 5; i++) {
            waves2.add(9831007);
        }
        getWaveData().add(waves2);
        List<Integer> waves3 = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            waves3.add(9831012);
        }
        waves3.add(9831014);
        getWaveData().add(waves3);
    }

    public void attacked(Char chr) {
        this.life--;
        updateDefenseLife(chr);
        if (getLife() <= 0) {
            ScriptManagerImpl sm = chr.getScriptManager();
            startTimer.cancel(false);
            sm.showEffectToField("Map/Effect.img/killing/fail");
            if (chr.getField().getId() == 993000100) {
                sm.warpNoReturn(993000600, 0);
            }
        }
    }

    public void start(Char chr) {
        ScriptManagerImpl sm = chr.getScriptManager();
        updateDefenseWave(chr);
        updateDefenseLife(chr);
        insertWaveData();
        sm.showWeatherNotice("牠們毫無畏懼地攻擊村莊！把牠們全部消滅！", WeatherEffNoticeType.Pollo);
        sm.createClock(300);
        updateNewWave(chr);
        startTimer = chr.getTimer().addEvent(() -> {
            if (chr.getField().getId() == 993000100) {
                sm.warpNoReturn(993000600, 0);
            }
        }, 300000);
    }
}
