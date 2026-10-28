package net.swordie.ms.world.event;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.packet.MobPool;
import net.swordie.ms.connection.packet.UserLocal;
import net.swordie.ms.enums.WeatherEffNoticeType;
import net.swordie.ms.handlers.Timer;
import net.swordie.ms.life.mob.Mob;
import net.swordie.ms.scripts.ScriptManagerImpl;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class FrittoEagle {

    private int score;
    private int bullet;

    public FrittoEagle(int score, int bullet) {
        this.score = score;
        this.bullet = bullet;
    }

    public void createGun(Char chr) {
        chr.write(UserLocal.createGun());
        chr.write(UserLocal.setGun());
        chr.write(UserLocal.setAmmo(getBullet()));
    }

    public void checkFinish(Char chr) {
        ScriptManagerImpl sm = chr.getScriptManager();
        if (chr.getStartEventTimer() != null) {
            chr.getStartEventTimer().cancel(false);
        }
        chr.setStartEventTimer(chr.getTimer().addFixedRateEvent(() -> {
            if (chr.getField() != null) {
                if (getScore() >= 1000 || chr.getField().getMobs().isEmpty() || getBullet() == 0) {
                    sm.showEffectToField("Map/Effect.img/killing/clear");
                    chr.getTimer().addEvent(() -> {
                        if (chr.getStartEventTimer() != null) {
                            chr.getStartEventTimer().cancel(false);
                        }
                        if (chr.getEndEventTimer() != null) {
                            chr.getEndEventTimer().cancel(false);
                        }
                        if (chr.getField().getId() == 993000200) {
                            sm.unlockUI();
                            sm.hideUser(false);
                            sm.warpNoReturn(993000601, 0);
                        }
                    }, 1000);
                } else if (!chr.getField().getMobs().isEmpty() && getBullet() == 0) {
                    sm.showEffectToField("Map/Effect.img/killing/failed");
                    chr.getTimer().addEvent(() -> {
                        if (chr.getStartEventTimer() != null) {
                            chr.getStartEventTimer().cancel(false);
                        }
                        if (chr.getEndEventTimer() != null) {
                            chr.getEndEventTimer().cancel(false);
                        }
                        if (chr.getField().getId() == 993000200) {
                            sm.unlockUI();
                            sm.hideUser(false);
                            sm.warpNoReturn(993000601, 0);
                        }
                    }, 1000);
                }
            }
        }, 1, 5, TimeUnit.SECONDS, false));
    }

    public void addScore(Mob monster, Char chr) {
        int be = getScore();
        switch (monster.getTemplateId()) {
            case 9833000 -> this.score += 50;
            case 9833001 -> this.score += 100;
            case 9833002 -> this.score += 200;
            case 9833003 -> this.score -= 50;
        }
        if (chr.hasQuest(15141)) {
            chr.setQRValueByKey(15141, "point", "" + getScore());
        } else {
            chr.createQuestWithQRValue(15141, "point=" + getScore());
        }
        chr.write(MobPool.deadFPSMode(monster.getObjectId(), getScore() - be));
    }

    public void updateNewWave(Char chr) {
        ScriptManagerImpl sm = chr.getScriptManager();
        sm.showEffectToField("Map/Effect.img/killing/first/start");
        for (int i = 0; i < 6; i++) {
            sm.spawnMob(9833000, 0, 0, false);
        }
        for (int i = 0; i < 3; i++) {
            sm.spawnMob(9833001, 0, 0, false);
        }
        for (int i = 0; i < 2; i++) {
            sm.spawnMob(9833002, 0, 0, false);
        }
        for (int i = 0; i < 4; i++) {
            sm.spawnMob(9833003, 0, 0, false);
        }
        chr.getTimer().addEvent(() -> checkFinish(chr), 2000);
    }

    public void shootResult(Char chr) {
        if (getBullet() > 1) {
            chr.write(UserLocal.shootFPS());
        } else {
            ScriptManagerImpl sm = chr.getScriptManager();
            if (chr.getField().getId() == 993000200) {
                sm.unlockUI();
                sm.hideUser(false);
                sm.warpNoReturn(993000601, 0);
            }
        }
    }

    public void start(Char chr) {
        ScriptManagerImpl sm = chr.getScriptManager();
        createGun(chr);
        if (chr.hasQuest(15141)) {
            chr.setQRValueByKey(15141, "point", "0");
        } else {
            chr.createQuestWithQRValue(15141, "point=0");
        }
        sm.lockUI();
        sm.OnOffLayer_On(500, "0", 0, 380, 0, "Map/Effect.img/PoloFritto/msg1", 4, 1, -1, 0);
        sm.showWeatherNotice("冷靜地一隻一隻抓老鷹吧！禿鷹沒有用，不要抓牠們！", WeatherEffNoticeType.Fritto);
        sm.createStopWatch(30);
        updateNewWave(chr);
        chr.setEndEventTimer(chr.getTimer().addEvent(() -> {
            if (chr.getField().getId() == 993000200) {
                sm.unlockUI();
                sm.hideUser(false);
                sm.warpNoReturn(993000601, 0);
            }
        }, 30000));
    }

    public int getScore() {
        return score;
    }

    public int getBullet() {
        return bullet;
    }
}
