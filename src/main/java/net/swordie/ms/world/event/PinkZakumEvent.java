package net.swordie.ms.world.event;

import net.swordie.ms.Server;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.packet.FieldPacket;
import net.swordie.ms.connection.packet.WvsContext;
import net.swordie.ms.enums.WeatherEffNoticeType;
import net.swordie.ms.enums.social.Event.InGameEventType;
import net.swordie.ms.life.drop.Drop;
import net.swordie.ms.world.Channel;
import net.swordie.ms.world.field.ClockPacket;
import net.swordie.ms.world.field.Field;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class PinkZakumEvent implements InGameEvent {

    public static final int BATTLE_MAP = 689013000; // pink zakum raid
    private final int TIME_LIMIT_SECONDS = 600; // 10 minutes
    private final int LOBBY_MAP = 689010000;
    private final int WARPOUT_MAP = 689012000; // pink zakum exit
    private final int REENTRY_MAP = 689011000; // pink zakum revive room
    private final int eventNpc = 9000155; // dizzy the roulette event administrator
    private int remindersSent = 0;
    private boolean started = false;
    private boolean active = false;
    private long startTimeMillis;
    private ScheduledFuture<?> startTimer;
    private ScheduledFuture<?> endTimer;
    private Channel channelInstance;
    private Map<Integer, Boolean> winners = new HashMap<>();

    public PinkZakumEvent() {
        channelInstance = Server.get().getWorld().getChannels().get(0);
    }

    @Override
    public String getEventName() {
        return "Pink Zakum";
    }

    @Override
    public void doEvent() {
        active = true;
        winners.clear();
        channelInstance = Server.get().getWorld().getChannels().get(0);
        startTimer = Server.get().getEventTimer().addEvent(this::start, InGameEventManager.REGISTRATION_DURATION_MINS, TimeUnit.MINUTES);
        startTimeMillis = System.currentTimeMillis() + InGameEventManager.REGISTRATION_DURATION_MINS * 60 * 1000;
        channelInstance.getField(LOBBY_MAP).setDropsDisabled(true); // to reduce lag
        sendNotice(LOBBY_MAP, "準備迎接粉紅殘暴炎魔的史詩對決吧！", InGameEventManager.REGISTRATION_DURATION_MINS * 60);
    }

    private void start() {
        if (startTimer != null && !startTimer.isDone()) {
            startTimer.cancel(false);
        }

        started = true;

        if (channelInstance.getField(LOBBY_MAP).getChars().size() > 0) {
            startTimeMillis = System.currentTimeMillis() + TIME_LIMIT_SECONDS * 1000;
            warpMap(LOBBY_MAP, BATTLE_MAP);
            broadcastClock(BATTLE_MAP, TIME_LIMIT_SECONDS);
            sendNotice(BATTLE_MAP, "殘暴炎魔將在 5 秒後出現！", 5);
            startTimer = Server.get().getEventTimer().addEvent(this::spawnZakum, 5, TimeUnit.SECONDS);
            endTimer = Server.get().getEventTimer().addEvent(this::endEvent, TIME_LIMIT_SECONDS, TimeUnit.SECONDS);
            //Server.getInstance().getWorld().initSlideNotice(String.format("Event registration has ended! The %s Event has been started!", getEventName()), 60);
        } else {
            endEvent();
            //Server.getInstance().getWorld().initSlideNotice(String.format("Since no one was interested, the %s Event was cancelled!", getEventName()), 60);
        }
    }

    private void spawnZakum() {
        if (startTimer != null && !startTimer.isDone()) {
            startTimer.cancel(false);
            startTimer = null;
        }

        Field field = channelInstance.getField(689013000);
        if (field.getChars().size() <= 0) {
            endEvent();
            return;
        }
        sendNotice(BATTLE_MAP, "打倒殘暴炎魔！", TIME_LIMIT_SECONDS);

        int pX = -5, pY = 329, pZakBody = 9400900, pZakArm = 9400903;

        field.spawnMob(pZakBody, pX, pY, false, 0);
        for (int i = 0; i < 8; i++) {
            field.spawnMob(pZakArm + i, pX, pY, false, 0);
        }
    }

    public void win() {
        startTimer = Server.get().getEventTimer().addEvent(this::endEvent, 5, TimeUnit.SECONDS);

        for (Char c : channelInstance.getField(BATTLE_MAP).getChars()) {
            winners.put(c.getId(), false);
        }
    }

    @Override
    public void endEvent() {
        active = false;
        started = false;
        startTimer = null;
        endTimer = null;
        warpMap(BATTLE_MAP, WARPOUT_MAP);

        if (getTimeLeft() <= 0) {
            sendNotice(BATTLE_MAP, "時間到了，下次再加油！", 10);
        } else {
            distributeRewards();
        }

        channelInstance.getField(WARPOUT_MAP).broadcast(FieldPacket.destroy());
    }

    @Override
    public void clear() {
        Field field = channelInstance.getFieldIfExists(BATTLE_MAP);

        if (field == null) {
            return; // nothing to clear
        }

        warpMap(BATTLE_MAP, WARPOUT_MAP);

        for (Drop d : field.getDrops()) {
            field.removeLife(d);
        }

        channelInstance.clearCache();
    }

    private void distributeRewards() {
        // todo
    }

    @Override
    public void joinEvent(Char c) {
        long timeLeftToStart = (startTimeMillis - System.currentTimeMillis()) / 1000;
        c.chatMessage("剩餘時間：" + timeLeftToStart);
        c.changeChannelAndWarp((byte) channelInstance.getChannelId(), LOBBY_MAP);
    }

    private void sendNotice(int fieldId, String msg, int seconds) {
        channelInstance.getField(fieldId).broadcast(WvsContext
                .weatherEffectNotice(WeatherEffNoticeType.PinkZakum, msg, seconds * 1000));
    }

    private void broadcastClock(int fieldId, int seconds) {
        channelInstance.getField(fieldId).broadcast(FieldPacket.clock(ClockPacket.secondsClock(seconds)));
    }

    private void warpMap(int fromField, int toFieldId) {
        for (Char c : channelInstance.getField(fromField).getChars()) {
            warpChar(c, toFieldId);
        }
    }

    private void warpChar(Char c, int toFieldId) {
        c.warp(toFieldId, 0, false);
    }

    @Override
    public void sendLobbyClock(Char c) {
        if (getTimeLeft() >= 2) {
            String message = started
                    ? "打倒殘暴炎魔！"
                    : "準備迎接粉紅殘暴炎魔的史詩對決吧！";

            int map = started
                    ? BATTLE_MAP
                    : LOBBY_MAP;

            c.write(FieldPacket.clock(ClockPacket.secondsClock(getTimeLeft())));
            sendNotice(map, message, getTimeLeft());
        }
    }

    private int getTimeLeft() {
        return (int) (startTimeMillis - System.currentTimeMillis()) / 1000;
    }

    @Override
    public boolean isActive() {
        return active;
    }

    @Override
    public boolean isOpen() {
        return !started;
    }

    @Override
    public InGameEventType getEventType() {
        return InGameEventType.PinkZakumBattle;
    }

    @Override
    public int getEventEntryMap() {
        return LOBBY_MAP;
    }

    @Override
    public boolean charInEvent(int charId) {
        if (!active) {
            return false;
        }

        int map = started ? BATTLE_MAP : LOBBY_MAP;
        for (Char c : channelInstance.getField(map).getChars()) {
            if (c.getId() == charId) {
                return true;
            }
        }

        return false;
    }

    @Override
    public void onMigrateDeath(Char c) {
        if (isActive()) {
            c.warp(REENTRY_MAP, 1, false);
        } else {
            c.warp(WARPOUT_MAP, 0, false);
        }
    }

    public boolean isWinner(Char c) {
        if (winners == null) {
            winners = new HashMap<>();
        }
        return winners.containsKey(c.getId());
    }

    public void setWinnerRewarded(Char c) {
        if (isWinner(c)) {
            winners.replace(c.getId(), true);
        }
    }

    public boolean getWinnerRewarded(Char c) {
        return winners.containsKey(c.getId()) && winners.get(c.getId());
    }
}
