package net.swordie.ms.world.event;

import net.swordie.ms.Server;
import net.swordie.ms.ServerConfig;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.packet.UserLocal;
import net.swordie.ms.enums.ChatType;
import net.swordie.ms.enums.social.Event.InGameEventType;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class InGameEventManager {

    public static final int REGISTRATION_DURATION_MINS = 5; // devs want fast
    private static InGameEventManager instance = new InGameEventManager();
    private HashMap<InGameEventType, InGameEvent> events = new HashMap<>();
    private ScheduledFuture<?> eventSchedule;
    private InGameEventType previousEvent;
    private String previousEventName;
    private int remindersSent = 0;
    private long nextEventTime = 0;

    public InGameEventManager() {
        events.put(InGameEventType.RussianRoulette, new RussianRouletteEvent());
        //events.put(InGameEventType.PinkZakumBattle, new PinkZakumEvent());
        events.put(InGameEventType.OlaOlaJump, new OlaOlaEvent());
        events.put(InGameEventType.PhysicalFitnessJump, new PhysicalFitnessEvent());
        events.put(InGameEventType.OXQuiz, new OXQuizEvent());
        events.put(InGameEventType.GMJump, new GMJumpEvent());

        if (ServerConfig.AUTO_EVENT) {
            this.eventSchedule = Server.get().getEventTimer().addFixedRateEvent(this::doEvent, 1, 1, TimeUnit.HOURS, true);
        }
    }

    public static InGameEventManager getInstance() {
        return instance;
    }

    public void forceNextEvent() {
        InGameEvent curEvent = getActiveEvent();
        if (curEvent != null) {
            curEvent.endEvent();
        }
        doEvent();
    }

    public void forceGMvent(int LOBBY_MAP) {
        InGameEvent curEvent = getActiveEvent();
        if (curEvent != null) {
            curEvent.endEvent();
        }
        doGMEvent(LOBBY_MAP);
    }

    private void doEvent() {
        nextEventTime = System.currentTimeMillis();
        for (Map.Entry<InGameEventType, InGameEvent> entry : events.entrySet()) {
            if (entry.getValue().getEventName().equals("GM Jump Event")) {
                InGameEvent event = entry.getValue();
                previousEvent = event.getEventType();
                previousEventName = event.getEventName();
                event.clear(); // reset map info for next run
                String msg = event.getEventName() + " 開始報名！報名將在 " + REGISTRATION_DURATION_MINS + " 分鐘後截止，請輸入 @event 報名，別錯過了！";
                Server.get().broadcastForWorld(UserLocal.chatMsg(ChatType.Notice2, msg));
                event.doEvent();
                break;
            }
        }
    }

    private void doGMEvent(int LOBBY_MAP) {
        nextEventTime = System.currentTimeMillis();
        for (Map.Entry<InGameEventType, InGameEvent> entry : events.entrySet()) {
            if (entry.getValue().getEventName().equals("GM Jump Event")) {
                InGameEvent event = entry.getValue();
                previousEvent = event.getEventType();
                previousEventName = event.getEventName();
                event.clear(); // reset map info for next run
                String msg = event.getEventName() + " 開始報名！報名將在 " + REGISTRATION_DURATION_MINS + " 分鐘後截止，請輸入 @event 報名，別錯過了！";
                Server.get().broadcastForWorld(UserLocal.chatMsg(ChatType.Notice2, msg));
                ((GMJumpEvent) event).setGameMap(LOBBY_MAP);
                event.doEvent();
                break;
            }
        }
    }

    public InGameEvent getOpenEvent() {
        InGameEvent e = null;
        for (InGameEvent ige : events.values())
            if (ige.isOpen()) {
                e = ige;
            }
        return e;
    }

    public InGameEvent getActiveEvent() {
        for (InGameEvent ige : events.values())
            if (ige.isActive()) {
                return ige;
            }
        return null;
    }

    public void joinPublicEvent(Char c) {
        InGameEvent e = getActiveEvent();

        if (e == null) {
            c.chatMessage(ChatType.SystemNotice, "目前沒有進行中的活動，請稍後再來看看！");
        } else if (!e.isOpen()) {
            c.chatMessage(ChatType.SystemNotice, "活動已停止報名。");
        } else {
            e.joinEvent(c);
        }
    }

    public InGameEvent getEvent(InGameEventType type) {
        for (InGameEvent ige : events.values()) {
            if (ige.getEventType() == type) {
                return ige;
            }
        }
        return null; // shouldnt reach this point
    }

    public boolean charInEventMap(int charId) {
        InGameEvent e = getActiveEvent();

        if (e == null) {
            return false;
        }

        return e.charInEvent(charId);
    }

    public long getNextEventTime() {
        return nextEventTime;
    }

    public String getPreviousEventName() {
        return previousEventName;
    }
}
