package net.swordie.ms.world.event;

import net.swordie.ms.Server;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.packet.Effect;
import net.swordie.ms.connection.packet.FieldPacket;
import net.swordie.ms.connection.packet.UserPacket;
import net.swordie.ms.connection.packet.WvsContext;
import net.swordie.ms.enums.ChatType;
import net.swordie.ms.enums.WeatherEffNoticeType;
import net.swordie.ms.enums.social.Event.InGameEventType;
import net.swordie.ms.util.Util;
import net.swordie.ms.world.Channel;
import net.swordie.ms.world.field.ClockPacket;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class RussianRouletteEvent implements InGameEvent {

    public static final int EVENT_MAP = 910030000; // same map extalia used to use
    public static final int REWARD_MAP = 109050000;
    public static final int EXIT_MAP = 109050001;
    private final int TOTAL_ROUNDS = 10; // edit this later, make it scalable by # of players registered for event
    private final int ROUND_LENGTH_SECONDS = 10;

    private final int[][] SECTIONS = { // these X-values are specific to fieldID 910030000
            {-800, -254}, // left
            {-327, 248},  // middle
            {171, 800}    // right
    };
    private final int[] npcPos = {208, -206}; // spawn points for above npc
    private boolean started = false;
    private boolean active = false;
    private long startTimeMillis;
    private ScheduledFuture<?> startTimer;
    private ScheduledFuture<?> killTimer;
    private int currentRound = 1;
    private Channel channelInstance;

    public RussianRouletteEvent() {
        channelInstance = Server.get().getWorld().getChannels().get(0);
    }

    @Override
    public String getEventName() {
        return "Russian Roulette";
    }

    @Override
    public void doEvent() {
        active = true;
        channelInstance = Server.get().getWorld().getChannels().get(0);
        startTimer = Server.get().getEventTimer().addEvent(this::start, InGameEventManager.REGISTRATION_DURATION_MINS, TimeUnit.MINUTES);
        startTimeMillis = System.currentTimeMillis() + InGameEventManager.REGISTRATION_DURATION_MINS * 60 * 1000;
        channelInstance.getField(EVENT_MAP).setDropsDisabled(true); // lag reduction, look at old extalia events and youll see trolls dropping items to cause major lag
        channelInstance.getField(EVENT_MAP).getNpcs().clear(); // remove the standard npcs
    }

    private void start() {
        if (startTimer != null && !startTimer.isDone()) {
            startTimer.cancel(false);
        }

        started = true;

        if (channelInstance.getField(EVENT_MAP).getChars().size() > 0) {
            sendNotice("請站好位置！", ROUND_LENGTH_SECONDS);
            broadcastClock(ROUND_LENGTH_SECONDS);
            killTimer = Server.get().getEventTimer().addEvent(this::broadcastCountdownEffect, ROUND_LENGTH_SECONDS - 3, TimeUnit.SECONDS);
            //Server.getInstance().getWorld().initSlideNotice(String.format("Event registration has ended! The %s Event has been started!", getEventName()), 60);
        } else {
            endEvent();
            //Server.getInstance().getWorld().initSlideNotice(String.format("Since no one was interested, the %s Event was cancelled!", getEventName()), 60);
        }
    }

    private void broadcastCountdownEffect() {
        channelInstance.getField(EVENT_MAP).broadcast(UserPacket.effect(Effect.effectFromWZ("Map/Effect.img/defense/count")));
        killTimer = Server.get().getEventTimer().addEvent(this::executeRound, 3, TimeUnit.SECONDS);
    }

    private void executeRound() {
        killCharsInRect();

        if (currentRound >= TOTAL_ROUNDS) {
            endEvent();
        } else {
            channelInstance.getField(EVENT_MAP).broadcast(FieldPacket.destroy());
            startTimer = Server.get().getEventTimer().addEvent(this::resetRound, 3, TimeUnit.SECONDS);
        }
        currentRound += 1;
    }

    private void resetRound() {
        broadcastClock(ROUND_LENGTH_SECONDS);
        killTimer = Server.get().getEventTimer().addEvent(this::executeRound, ROUND_LENGTH_SECONDS, TimeUnit.SECONDS);
    }

    private void killCharsInRect() {
        if (channelInstance.getField(EVENT_MAP).getChars().size() <= 0) {
            endEvent();
        } else {
            int[] randDomain = SECTIONS[Util.getRandom(2)]; // holds two X values

            // y-axis is inverted for some reason
            final int UPPER_BOUNDARY = -77;
            final int LOWER_BOUNDARY = 100;

            for (Char c : channelInstance.getField(EVENT_MAP).getChars()) {
                c.chatMessage(ChatType.Mob, "X: " + c.getPosition().getX() + " Y: " + c.getPosition().getY());
                // range
                if (c.getPosition().getY() <= UPPER_BOUNDARY || c.getPosition().getY() >= LOWER_BOUNDARY) {
                    c.damage(c.getMaxHP());
                }
                // domain
                if (c.getPosition().getX() >= randDomain[0] && c.getPosition().getX() <= randDomain[1]) {
                    c.damage(c.getMaxHP());
                }
            }

            List<Char> deadChars = new ArrayList<>();
            for (Char c : channelInstance.getField(EVENT_MAP).getChars()) {
                if (c.getHP() <= 0) {
                    deadChars.add(c);
                    deathEffect(c);
                }
            }
            // killTimer = EventManager.addEvent(() -> warpOut(deadChars), 5, TimeUnit.SECONDS); // what do we want to do with the dead?
            updatePlayerCount(deadChars.size());
        }
    }

    @Override
    public void endEvent() {
        active = false;
        started = false;
        startTimer = null;
        killTimer = null;
        for (Char c : channelInstance.getField(EVENT_MAP).getChars()) {
            warpOut(c);
        }
    }

    @Override
    public void clear() {
        // unused rn
    }

    private void warpOut(List<Char> charList) {
        for (Char c : charList) {
            warpOut(c);
        }
    }

    private void warpOut(Char c) {
        try {
            c.warp(EXIT_MAP, 0, false);
        } catch (Exception ex) {
            // if the player has disconnected before they get warped
        }
    }

    public void joinEvent(Char c) {
        c.changeChannelAndWarp((byte) channelInstance.getChannelId(), EVENT_MAP);
    }

    private void updatePlayerCount(int dead) {
        sendNotice("剩餘玩家：" + (channelInstance.getField(EVENT_MAP).getChars().size() - dead), ROUND_LENGTH_SECONDS);
    }

    private void deathEffect(Char c) {
        c.write(UserPacket.effect(Effect.effectFromWZ("Map/Effect.img/Yut/Lose")));
    }

    private void sendNotice(String msg, int seconds) {
        channelInstance.getField(EVENT_MAP)
                .broadcast(WvsContext.weatherEffectNotice(WeatherEffNoticeType.Fireworks, msg,
                        seconds * 1000));
    }

    private void broadcastClock(int seconds) {
        channelInstance.getField(EVENT_MAP).broadcast(FieldPacket.clock(ClockPacket.secondsClock(seconds)));
    }

    @Override
    public void sendLobbyClock(Char c) {
        long timeLeft = (startTimeMillis - System.currentTimeMillis()) / 1000;
        if (timeLeft >= 2) {
            c.write(FieldPacket.clock(ClockPacket.secondsClock((int) timeLeft)));
            sendNotice("準備好來一場楓之谷風格的俄羅斯輪盤吧！", (int) timeLeft);
        }
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
        return InGameEventType.RussianRoulette;
    }

    @Override
    public int getEventEntryMap() {
        return EVENT_MAP;
    }

    @Override
    public boolean charInEvent(int charId) {
        if (!active) {
            return false;
        }

        for (Char c : channelInstance.getField(EVENT_MAP).getChars()) {
            if (c.getId() == charId) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onMigrateDeath(Char c) {
        c.warp(EXIT_MAP, 0, false);
    }
}
