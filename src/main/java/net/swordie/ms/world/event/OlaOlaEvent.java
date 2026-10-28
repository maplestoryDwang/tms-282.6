package net.swordie.ms.world.event;

import net.swordie.ms.Server;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.packet.FieldPacket;
import net.swordie.ms.connection.packet.UserLocal;
import net.swordie.ms.connection.packet.WvsContext;
import net.swordie.ms.enums.ChatType;
import net.swordie.ms.enums.WeatherEffNoticeType;
import net.swordie.ms.enums.social.Event.InGameEventType;
import net.swordie.ms.life.drop.Drop;
import net.swordie.ms.world.Channel;
import net.swordie.ms.world.field.ClockPacket;
import net.swordie.ms.world.field.Field;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class OlaOlaEvent implements InGameEvent {

    public static int LOBBY_MAP = 109030001;
    public static int FINAL_MAP = 109030003;
    public static final int REWARD_MAP = 109050000;
    public static final int EXIT_MAP = 109050001;
    public static final String JOIN_PORTAL = "join00";
    private static final int TIME_LIMIT_SECONDS = 360; // 6 minutes
    private boolean started = false;
    private boolean active = false;
    private long startTimeMillis;
    private Channel channelInstance;
    private ScheduledFuture<?> startTimer;
    private ScheduledFuture<?> endTimer;

    public OlaOlaEvent() {
        channelInstance = Server.get().getWorld().getChannels().get(0);
    }

    @Override
    public String getEventName() {
        return "Ola Ola Event";
    }

    @Override
    public void doEvent() {
        this.active = true;
        startTimer = Server.get().getEventTimer().addEvent(this::start, InGameEventManager.REGISTRATION_DURATION_MINS, TimeUnit.MINUTES);
        startTimeMillis = System.currentTimeMillis() + (long) InGameEventManager.REGISTRATION_DURATION_MINS * 60 * 1000;
        channelInstance = Server.get().getWorld().getChannels().get(0);
        channelInstance.getField(LOBBY_MAP).setDropsDisabled(true); // to reduce lag
    }

    @Override
    public void endEvent() {
        this.active = false;
        this.started = false;
        this.startTimer = null;
        this.endTimer = null;

        for (int map = LOBBY_MAP; map <= FINAL_MAP; map++) {
            for (Char chr : channelInstance.getField(map).getChars()) {
                chr.warp(chr.getPreviousFieldID());
            }
            channelInstance.getField(map).broadcast(FieldPacket.destroy());
        }
    }

    @Override
    public void clear() {
        for (int map = LOBBY_MAP; map <= FINAL_MAP; map++) {
            warpMap(map, EXIT_MAP);
            Field field = channelInstance.getFieldIfExists(map);

            if (field == null) {
                return; // nothing to clear
            }

            for (Drop d : field.getDrops()) {
                field.removeLife(d);
            }
        }
        if (startTimer != null) {
            startTimer.cancel(false);
        }
        startTimer = null;
        if (endTimer != null) {
            endTimer.cancel(false);
        }
        endTimer = null;
        channelInstance.clearCache();
    }

    @Override
    public void joinEvent(Char c) {
        if (c.getClient().getChannelInstance().getChannelId() != channelInstance.getChannelId()) {
            c.chatMessage("請前往頻道 " + channelInstance.getChannelId() + " 參加" + getEventName() + "！");
        } else {
            warpChar(c, LOBBY_MAP);
        }
    }

    @Override
    public void sendLobbyClock(Char c) {
        if (getTimeLeft() >= 2) {
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
        return InGameEventType.OlaOlaJump;
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

        for (int map = LOBBY_MAP; map <= FINAL_MAP; map++) {
            for (Char c : channelInstance.getField(map).getChars()) {
                if (c.getId() == charId) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public void onMigrateDeath(Char c) {
        if (isActive()) {
            c.warp(LOBBY_MAP, 1, false);
        } else {
            c.warp(EXIT_MAP, 0, false);
        }
    }

    public void sendEventInfo(int fromField) {
        for (Char chr : channelInstance.getField(fromField).getChars()) {
            String sb = "#e[向上爬]#n" +
                    "\r\n\r\n" +
                    "你好，感謝你參加這次的活動。以下是遊戲的簡單介紹。" +
                    "\r\n\r\n" +
                    "[向上爬] 是參加者沿著梯子爬到頂端的遊戲。往上爬，並從眾多傳送點中選出正確的傳送點前往下一關。" +
                    "\r\n" +
                    "遊戲共有三關，時間限制為 #b6 分鐘#k。[向上爬] 進行中 #b無法跳躍、瞬間移動、使用加速技能，也不能用藥水或道具提升速度。#k" +
                    "\r\n" +
                    "另外還有會把你帶到奇怪地方的陷阱傳送點，請多加小心。" +
                    "\r\n\r\n" +
                    "如果對活動有任何疑問，歡迎在遊戲中提出。";
            chr.getScriptManager().sendOK(sb, 9010000);
        }
    }

    private void start() {
        started = true;

        if (!channelInstance.getChars().isEmpty()) {
            startTimeMillis = System.currentTimeMillis() + TIME_LIMIT_SECONDS * 1000;
            broadcastClock(LOBBY_MAP, TIME_LIMIT_SECONDS);
            endTimer = Server.get().getEventTimer().addEvent(this::endEvent, TIME_LIMIT_SECONDS, TimeUnit.SECONDS);
            //Server.getInstance().getWorld().initSlideNotice(String.format("Event registration has ended! The %s Event has been started!", getEventName()), 60);
            sendEventInfo(LOBBY_MAP);
            this.active = true;
            this.started = false;
            this.startTimer = null;
            Server.get().broadcastForWorld(UserLocal.chatMsg(ChatType.Notice2, getEventName() + " 活動已開始！"));
        } else {
            endEvent();
            Server.get().broadcastForWorld(UserLocal.chatMsg(ChatType.Notice2, String.format("由於無人蔘加，%s 活動已取消！", getEventName())));
        }
    }

    public void win() {
        startTimer = Server.get().getEventTimer().addEvent(this::endEvent, 5, TimeUnit.SECONDS);

        for (int map = LOBBY_MAP; map <= FINAL_MAP; map++) {
            for (Char chr : channelInstance.getField(map).getChars()) {
                chr.warp(chr.getPreviousFieldID());
            }
        }
    }

    private void sendNotice(int fieldId, String msg, int seconds) {
        channelInstance.getField(fieldId).broadcast(WvsContext
                .weatherEffectNotice(WeatherEffNoticeType.WizetAdminThumbsUp, msg, seconds * 1000));
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

    private int getTimeLeft() {
        return (int) (startTimeMillis - System.currentTimeMillis()) / 1000;
    }
}
