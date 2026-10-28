package net.swordie.ms.world.field;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.packet.FieldPacket;
import net.swordie.ms.enums.ClockType;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author Asura
 * Created on 14/09/2018.
 */
public class Clock {
    private ClockType clockType;
    private Field field;
    private int seconds;
    private long timeInMillis; // Time (in millis) when the Clock will be removed
    private ScheduledFuture<?> clockRemovalTimer;

    public Clock(ClockType clockType, Field field, int seconds) {
        this.clockType = clockType;
        this.field = field;
        this.seconds = seconds;
        this.timeInMillis = (seconds * 1000L) + System.currentTimeMillis();

        createClock();
    }

    public void createClock() {
        if (field.getClock() != null) {
            field.getClock().removeClock();
        }
        switch (getClockType()) {
            case SecondsClock:
                field.broadcast(FieldPacket.clock(ClockPacket.secondsClock(seconds)));
                break;
            case StopWatch:
                field.broadcast(FieldPacket.clock(ClockPacket.stopWatch(seconds)));
                break;
            case TimerGauge:
                field.broadcast(FieldPacket.clock(ClockPacket.timerGauge(seconds * 1000, seconds * 1000)));
                break;
            case TimerInfoEx:
                field.broadcast(FieldPacket.clock(ClockPacket.timerInfoEx(seconds * 1000, seconds * 1000)));
                break;
            case Unk1:
                field.broadcast(FieldPacket.clock(ClockPacket.Unk1(seconds * 1000, seconds * 1000)));
                break;
        }
        field.setClock(this);
        clockRemovalTimer = field.getTimer().addEvent(this::removeClock, seconds, TimeUnit.SECONDS);
    }

    public void showClock(Char chr) {
        if (field.getClock() != null) {
            switch (field.getClock().getClockType()) {
                case SecondsClock:
                    chr.write(FieldPacket.clock(ClockPacket.secondsClock(getRemainingTime())));
                    break;
                case StopWatch:
                    chr.write(FieldPacket.clock(ClockPacket.stopWatch(getRemainingTime())));
                    break;
                case TimerGauge:
                    chr.write(FieldPacket.clock(ClockPacket.timerGauge(seconds * 1000, seconds * 1000)));
                    break;
            }
        }
    }

    public int getRemainingTime() {
        return (int) ((timeInMillis - System.currentTimeMillis()) / 1000);
    }

    public void removeClock() {
        if (field.getClock() != null) {
            field.broadcast(FieldPacket.destroy());
            if (clockRemovalTimer != null) {
                clockRemovalTimer.cancel(false);
                clockRemovalTimer = null;
            }
            field.setClock(null);
        }
    }


    public ClockType getClockType() {
        return clockType;
    }

    public void setClockType(ClockType clockType) {
        this.clockType = clockType;
    }

    public Field getField() {
        return field;
    }

    public void setField(Field field) {
        this.field = field;
    }

    public int getSeconds() {
        return seconds;
    }

    public void setSeconds(int seconds) {
        this.seconds = seconds;
    }

    public long getTimeInMillis() {
        return timeInMillis;
    }

    public void setTimeInMillis(long timeInMillis) {
        this.timeInMillis = timeInMillis;
    }
}
