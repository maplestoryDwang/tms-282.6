package net.swordie.ms.world.event;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.enums.WeatherEffNoticeType;
import net.swordie.ms.scripts.ScriptManagerImpl;

import java.util.concurrent.ScheduledFuture;

public class FrittoEgg {

    private int stage;
    private ScheduledFuture<?> startTimer;

    public FrittoEgg(int stage) {
        this.stage = stage;
    }

    public void end(Char chr, int stage) {
        this.stage = stage;
        startTimer.cancel(false);
        ScriptManagerImpl sm = chr.getScriptManager();
        sm.showEffectToField("Map/Effect.img/killing/fail");
        if (chr.hasQuest(15042)) {
            chr.setQRValueByKey(15042, "stage", "" + stage);
        } else {
            chr.createQuestWithQRValue(15042, "stage=" + stage);
        }
        sm.stopEvents();
        sm.warpNoReturn(993000601, 0);
    }

    public void update(Char chr, int stage) {
        this.stage = stage;
        ScriptManagerImpl sm = chr.getScriptManager();
        if (chr.hasQuest(15042)) {
            chr.setQRValueByKey(15042, "stage", "" + stage);
        } else {
            chr.createQuestWithQRValue(15042, "stage=" + stage);
        }
        sm.showEffectToField("Map/Effect.img/killing/clear");
        if (stage == 5) {
            sm.spawnNpc(9001061, chr.getPosition().getX(), chr.getPosition().getY());
        }
    }

    public void start(Char chr) {
        ScriptManagerImpl sm = chr.getScriptManager();
        if (chr.hasQuest(15042)) {
            chr.setQRValueByKey(15042, "stage", "0");
        } else {
            chr.createQuestWithQRValue(15042, "stage=0");
        }
        sm.OnOffLayer_On(500, "0", 0, 380, 0, "Map/Effect.img/PoloFritto/msg2", 4, 1, -1, 0);
        sm.showWeatherNotice("噓！這個巢的最上面藏著一顆龍蛋，想辦法爬到最上面吧！", WeatherEffNoticeType.Fritto);
        sm.createStopWatch(60);
        sm.removeNpc(9001061);
        startTimer = chr.getTimer().addEvent(() -> {
            if (chr.getField().getId() == 993000300) {
                sm.warpNoReturn(993000601, 0);
            }
        }, 60000);
    }

    public int getStage() {
        return stage;
    }

    public void setStage(int stage) {
        this.stage = stage;
    }
}
