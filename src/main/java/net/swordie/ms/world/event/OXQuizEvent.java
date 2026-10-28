package net.swordie.ms.world.event;

import net.swordie.ms.Server;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.packet.FieldPacket;
import net.swordie.ms.connection.packet.HundredOXQuizPacket;
import net.swordie.ms.enums.social.Event.InGameEventType;
import net.swordie.ms.loaders.Etc.EtcData;
import net.swordie.ms.util.Rect;
import net.swordie.ms.util.Util;
import net.swordie.ms.util.container.Triple;
import net.swordie.ms.world.Channel;
import net.swordie.ms.world.field.ClockPacket;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class OXQuizEvent implements InGameEvent {

    public static final int LOBBY_MAP = 910048000;
    public static final int EVENT_MAP = 910048100;
    public static final int EXIT_MAP = 910048200;
    private final int TOTAL_ROUNDS = 20;
    private final int ROUND_LENGTH_SECONDS = 14; // 10s count effect + 2s for player to read
    private final int TIME_LIMIT_SECONDS = 310;
    private boolean started = false;
    private boolean active = false;
    private long startTimeMillis;
    private ScheduledFuture<?> startTimer;
    private ScheduledFuture<?> endTimer;
    private ScheduledFuture<?> nextRoundTimer;
    private int currentRound = 1;
    private boolean currentAnswer = false;
    private Channel channelInstance;
    private final Rect O_Rect = new Rect(-1386, 214, -1040, 454);
    private final Rect X_Rect = new Rect(-398, 214, 38, 454);
    private List<Triple<String, String, Boolean>> questions;

    public OXQuizEvent() {
        channelInstance = Server.get().getWorld().getChannels().get(0);
    }

    @Override
    public String getEventName() {
        return "OX Quiz";
    }

    @Override
    public void doEvent() {
        active = true;
        channelInstance = Server.get().getWorld().getChannels().get(0);
        startTimer = Server.get().getEventTimer().addEvent(this::start, InGameEventManager.REGISTRATION_DURATION_MINS, TimeUnit.MINUTES);
        startTimeMillis = System.currentTimeMillis() + InGameEventManager.REGISTRATION_DURATION_MINS * 60 * 1000;
        channelInstance.getField(EVENT_MAP).setDropsDisabled(true); // lag reduction, look at old extalia events and youll see trolls dropping items to cause major lag
    }

    @Override
    public void endEvent() {
        this.active = false;
        this.started = false;
        this.startTimer = null;
        this.endTimer = null;

        warpMap(EVENT_MAP, EXIT_MAP);

        // Players that afk in Waiting Room
        channelInstance.getField(LOBBY_MAP).broadcast(FieldPacket.destroy());
        warpMap(LOBBY_MAP, EXIT_MAP);
    }

    private void initQuestions() {
        questions = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            Triple<String, String, Boolean> q = Util.getRandomFromCollection(EtcData.getOXQuizs());
            if (!questions.contains(q)) {
                questions.add(q);
            }
        }
    }

    private void start() {
        if (startTimer != null && !startTimer.isDone()) {
            startTimer.cancel(false);
        }

        started = true;

        if (channelInstance.getField(LOBBY_MAP).getChars().size() > 0) {
            startTimeMillis = 0;
            warpMap(LOBBY_MAP, EVENT_MAP);
            initQuestions();
            startTimer = Server.get().getEventTimer().addEvent(this::startQuestion, 10, TimeUnit.SECONDS);
            endTimer = Server.get().getEventTimer().addEvent(this::endEvent, TIME_LIMIT_SECONDS, TimeUnit.SECONDS);
            //Server.getInstance().getWorld().initSlideNotice(String.format("Event registration has ended! The %s Event has been started!", getEventName()), 60);
        } else {
            endEvent();
            //Server.getInstance().getWorld().initSlideNotice(String.format("Since no one was interested, the %s Event was cancelled!", getEventName()), 60);
        }
    }

    private void startQuestion() {
        nextRoundTimer = Server.get().getEventTimer().addEvent(this::executeRound, ROUND_LENGTH_SECONDS, TimeUnit.SECONDS);
        Server.get().getEventTimer().addEvent(this::countEffect, 2, TimeUnit.SECONDS);

        sendQuestion(currentRound - 1, questions.get(currentRound - 1).getLeft());
        currentAnswer = questions.get(currentRound - 1).getRight();
        Server.get().getEventTimer().addEvent(() -> sendExplain(currentRound, questions.get(currentRound - 1).getMiddle()), 4, TimeUnit.SECONDS);
    }

    private void executeRound() {
        getAnswerResult();

        if (currentRound >= TOTAL_ROUNDS) {
            endEvent();
        } else {
            startTimer = Server.get().getEventTimer().addEvent(this::startQuestion, 3, TimeUnit.SECONDS);
        }
        currentRound += 1;
    }

    private void getAnswerResult() {
        if (channelInstance.getField(EVENT_MAP).getChars().size() <= 0) {
            endEvent();
        } else {
            for (Char c : channelInstance.getField(EVENT_MAP).getChars()) {
                if (currentAnswer) {
                    if (O_Rect.hasPositionInside(c.getPosition())) {
                        c.incOxQuizCount();
                        showResultEffect(c, true);
                    } else {
                        failedTeleport(c);
                        showResultEffect(c, false);
                    }
                } else {
                    if (X_Rect.hasPositionInside(c.getPosition())) {
                        c.incOxQuizCount();
                        showResultEffect(c, true);
                    } else {
                        failedTeleport(c);
                        showResultEffect(c, false);
                    }
                }
            }
        }
    }

    @Override
    public void clear() {
        // unused rn
    }

    public void joinEvent(Char c) {
        c.changeChannelAndWarp((byte) channelInstance.getChannelId(), LOBBY_MAP);
    }

    private void sendQuestion(int key, String msg) {
        channelInstance.getField(EVENT_MAP).broadcast(HundredOXQuizPacket.questions(key, msg));
    }

    private void sendExplain(int key, String msg) {
        channelInstance.getField(EVENT_MAP).broadcast(HundredOXQuizPacket.explan(key, msg));
    }

    private void countEffect() {
        channelInstance.getField(EVENT_MAP).broadcast(HundredOXQuizPacket.countEffect());
    }

    private void showResultEffect(Char c, boolean isSucess) {
        c.write(HundredOXQuizPacket.answerResult(isSucess));
    }

    private void failedTeleport(Char c) {
        c.write(HundredOXQuizPacket.moveToPortal(Util.getRandom(11, 19)));
    }

    private void warpMap(int fromField, int toFieldId) {
        for (Char c : channelInstance.getField(fromField).getChars()) {
            warpChar(c, toFieldId);
        }
    }

    private void warpChar(Char c, int toFieldId) {
        c.warp(toFieldId, 0, false);
    }

    private int getTimeLeft() {
        return (int) (startTimeMillis - System.currentTimeMillis()) / 1000;
    }

    @Override
    public void sendLobbyClock(Char c) {
        if (getTimeLeft() >= 2 && c.getFieldID() == LOBBY_MAP) {
            c.write(FieldPacket.clock(ClockPacket.secondsClock(getTimeLeft())));
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
        return InGameEventType.OXQuiz;
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

        for (Char c : channelInstance.getField(LOBBY_MAP).getChars()) {
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
