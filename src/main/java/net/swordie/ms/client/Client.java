package net.swordie.ms.client;

import net.swordie.ms.Server;
import net.swordie.ms.ServerConfig;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.TradeRoom;
import net.swordie.ms.client.social.Alliance.Alliance;
import net.swordie.ms.client.social.Alliance.AllianceResult;
import net.swordie.ms.client.social.Friend.Friend;
import net.swordie.ms.client.social.Friend.FriendResult;
import net.swordie.ms.client.social.Guild.Guild;
import net.swordie.ms.client.social.Guild.GuildMember;
import net.swordie.ms.client.social.Guild.GuildResult;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.crypto.TripleDESCipher;
import net.swordie.ms.connection.netty.NettyClient;
import net.swordie.ms.connection.packet.Login;
import net.swordie.ms.connection.packet.MiniroomPacket;
import net.swordie.ms.connection.packet.WvsContext;
import net.swordie.ms.constants.FieldConstants;
import net.swordie.ms.enums.social.Friend.FriendFlag;
import net.swordie.ms.handlers.ClientSocket;
import net.swordie.ms.handlers.header.InHeader;
import net.swordie.ms.handlers.header.OutHeader;
import net.swordie.ms.util.DataPrinter;
import net.swordie.ms.util.FileTime;
import net.swordie.ms.util.Util;
import net.swordie.ms.world.Channel;
import net.swordie.ms.world.World;
import net.swordie.ms.world.field.Field;
import org.python.jline.internal.Log;

import java.time.LocalDateTime;
import java.util.*;

public class Client extends NettyClient {

    public static final byte LOGOUT = 0,
            LOGIN_TRANSITION = 1,
            IN_FIELD = 2,
            IN_CASH_SHOP = 3,
            IN_AUCTION_HOUSE = 4,
            CHANGING_CHANNEL = 5,
            NONE = 6;
    private byte currentState = NONE;
    private byte nextState = NONE;
    private Char chr;
    private Account account;
    private User user;
    private byte channel;
    private byte worldId;
    private boolean authorized;
    private Channel channelInstance;
    private byte oldChannel;
    private final Map<Short, Short> encryptedHeaderToNormalHeaders = new HashMap<>();
    private long ping;
    private long lastPingTime;
    private boolean waitingForAliveAck;
    private long lastAPIAttempt = 0;
    private volatile boolean exiting;

    public Client(io.netty.channel.Channel channel) {
        super(channel);
    }

    public void dispose() {
        write(WvsContext.exclRequest());
    }

    public boolean isExiting() {
        return exiting;
    }

    public void setExiting(boolean exiting) {
        this.exiting = exiting;
    }

    public void sendPing() {
        this.lastPingTime = System.currentTimeMillis();
        this.waitingForAliveAck = true;
        write(Login.sendAliveReq());
    }

    public void setPing(long ping) {
        this.ping = ping;
    }

    public long getPing() {
        return ping;
    }

    public long getLastPingTime() {
        return lastPingTime;
    }

    public void setLastPingTime(long lastPingTime) {
        this.lastPingTime = lastPingTime;
    }

    public boolean isWaitingForAliveAck() {
        return waitingForAliveAck;
    }

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    public byte getChannel() {
        return channel;
    }

    public void setChannel(byte channel) {
        this.channel = channel;
    }

    public byte getWorldId() {
        return worldId;
    }

    public void setWorldId(byte worldId) {
        this.worldId = worldId;
    }

    public Char getChr() {
        return chr;
    }

    public void setChr(Char chr) {
        if (chr != null) {
            Server.get().addChar(chr);
        }
        this.chr = chr;
    }

    public boolean isAuthorized() {
        return authorized;
    }

    public void setAuthorized(boolean authorized) {
        this.authorized = authorized;
    }

    public Channel getChannelInstance() {
        return channelInstance;
    }

    public void setChannelInstance(Channel channelInstance) {
        this.channelInstance = channelInstance;
    }

    public World getWorld() {
        return Server.get().getWorld();
    }

    public byte getOldChannel() {
        return oldChannel;
    }

    public void setOldChannel(byte oldChannel) {
        this.oldChannel = oldChannel;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public void handleChannelInactive() {
        System.out.printf("[ChannelInactive] Client %s: Current state: %s, Next state: %s.%n", getIP(), getState(currentState), getState(nextState));
        if (nextState == NONE) {
            // step 2: not a migration hand-off, so a ticket this socket asked for can no longer be meant for it
            int withdrawn = Server.get().getMigrations().discardIssuedBy(this);
            if (withdrawn > 0) {
                System.out.printf("[Migration] %s closed outside a migration: %d ticket(s) withdrawn%n", getIP(), withdrawn);
            }
        }
        if (nextState != NONE) {
            System.out.println("[ChannelInactive] 客戶端 " + getIP() + " 在狀態切換過程中斷開連接。");
        } else {
            switch (currentState) {
                case IN_FIELD:
                case IN_CASH_SHOP:
                case IN_AUCTION_HOUSE:
                    System.out.println("[ChannelInactive] 客戶端 " + getIP() + " 處於穩定登錄狀態，正在執行完整登出。");
                    finalizeSession();
                    break;
                case LOGOUT:
                case NONE:
                    System.out.println("[ChannelInactive] 客戶端 " + getIP() + " 尚未登錄，無需處理。");
                    break;
                case LOGIN_TRANSITION:
                    System.out.println("[ChannelInactive] 客戶端 " + getIP() + " 在選擇角色/頻道/世界時斷開連接。");
                    finalizeSession();
                    break;
                default:
                    System.out.printf("[ChannelInactive] 客戶端 " + getIP() + " 處於未處理的當前狀態 %d，正在執行安全登出。", getState(currentState));
                    finalizeSession();
                    break;
            }
        }
    }

    private String getState(int state) {
        switch (state) {
            case 0:
                return "LOGOUT";
            case 1:
                return "LOGIN_TRANSITION";
            case 2:
                return "IN_FIELD";
            case 3:
                return "IN_CASH_SHOP";
            case 4:
                return "IN_AUCTION_HOUSE";
            case 5:
                return "CHANGING_CHANNEL";
            default:
                return "NONE";
        }
    }

    public void migrateIn(boolean isAuctionHouse) {
        this.chr.punishLieDetectorEvasion();
        stopHeldSkills();
        this.chr.getField().removeChar(this.chr.getId());
        this.chr.initParty(true);
        byte state = isAuctionHouse ? IN_AUCTION_HOUSE : IN_CASH_SHOP;
        this.user.setClientState(state);
        setCurrentState(state);
        this.nextState = NONE;
        this.user.saveToSQL(false);
        if (isAuctionHouse) {
            write(Login.pingCheckResult());
            write(Login.authMessage());
            write(WvsContext.setMapleCoin(LocalDateTime.now()));
        }
    }

    public void migrateOut(Char chr) {
        user.setClientState(Client.CHANGING_CHANNEL);
        setCurrentState(Client.CHANGING_CHANNEL);
        setNextState(Client.IN_FIELD);
        user.saveToSQL(false);
        Channel channel = getChannelInstance();
        channel.addClientInTransfer(getChannel(), chr.getId(), this);
        chr.getTimer().addEvent(() -> write(ClientSocket.migrateCommand(this, true, (short) channel.getPort())), 1000);
        DataPrinter.send(DataPrinter.LOGIN, String.format("[IP: %s] [賬號: %s] [角色: %s] 正在離開現金商城 %d。", getIP(), user.getName(), chr.getName(), getChannel()));
    }

    public void auctionHouseOut(Char chr) {
        user.setClientState(Client.CHANGING_CHANNEL);
        setCurrentState(Client.CHANGING_CHANNEL);
        setNextState(Client.IN_FIELD);
        user.saveToSQL(false);
        Channel channel = getChannelInstance();
        channel.addClientInTransfer(getChannel(), chr.getId(), this);
        chr.getTimer().addEvent(() -> write(ClientSocket.auctionHouseOut(this, (short) channel.getPort())), 1000);
        DataPrinter.send(DataPrinter.LOGIN, String.format("[IP: %s] [賬號: %s] [角色: %s] 正在離開拍賣行 %d。", getIP(), user.getName(), chr.getName(), getChannel()));
    }

    public void finalizeSession() {
        finalizeSession(true);
    }

    public void finalizeSession(boolean notifyDisconnect) {
        if (chr == null) {
            setCurrentState(LOGOUT);
            setNextState(NONE);
            if (user != null) {
                user.setClientState(LOGOUT);
                user.saveToSQL(true);
                user.setCurrentChr(null);
            }
            if (ServerConfig.IP_LOG) {
                DataPrinter.send(DataPrinter.LOGIN, String.format("[IP: %s] 已斷開與服務器的連接。", getIP()));
            }
            return;
        }
        int charID = chr.getId();
        stopHeldSkills();
        chr.updateBuffDataList();
        chr.punishLieDetectorEvasion();
        chr.setOnline(false);
        // Event callbacks may warp/dispose scripts: run before taking the script-manager monitor.
        try {
            net.swordie.ms.scripts.event.EventManager.get().onDisconnect(chr);
            net.swordie.ms.world.tms282.revival.hub.RevivalHubs.onDisconnect(chr); // revival-1005
        } catch (RuntimeException | LinkageError eventFailure) {
            System.err.printf("[Event] disconnect callback for character %d failed: %s%n", charID, eventFailure);
        }
        if (!chr.getFriends().isEmpty()) {
            for (Friend friend : chr.getAllFriends()) {
                Char yourFriend = getWorld().getCharById(friend.getOwnerID());
                if (yourFriend != null) {
                    Friend thisCharFriend = yourFriend.getFriendByCharID(chr.getId());
                    if (thisCharFriend != null) {
                        byte flag = thisCharFriend.getFlag();
                        if (flag == FriendFlag.AccountFriendOnline.getVal()) {
                            thisCharFriend.setFlag(FriendFlag.AccountFriendOffline);
                        } else if (flag == FriendFlag.FriendOnline.getVal()) {
                            thisCharFriend.setFlag(FriendFlag.FriendOffline);
                        }
                        yourFriend.write(WvsContext.friendResult(FriendResult.response_Load_Success(yourFriend.getAllFriends())));
                    }
                }
            }
        }
        Guild guild = chr.getGuild();
        if (net.swordie.ms.client.social.Guild.Tms282GuildRuntime.enabled()) {
            try {
                net.swordie.ms.client.social.Guild.Tms282GuildRuntime.onPresence(chr, false);
            } catch (RuntimeException failure) {
                // Finish disconnect cleanup; an unresolved journal continues to block stale character saves.
                DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, failure);
            }
        } else if (guild != null) {
            GuildMember gm = guild.getMemberByCharID(chr.getId());
            if (gm != null) {
                gm.updateInfoFromChar(chr);
                guild.broadcast(WvsContext.guildResult(GuildResult.response_GuildNotify_LoginOrLogout(guild, gm, false, true)), chr);
                Alliance ally = guild.getAlliance();
                if (ally != null) {
                    ally.broadcast(WvsContext.allianceResult(AllianceResult.notifyLoginOrLogout(ally, guild, gm, true)), chr);
                }
            }
        }
        if (currentState == IN_FIELD || currentState == IN_CASH_SHOP || currentState == IN_AUCTION_HOUSE) {
            Field field = chr.getField();
            try {
                // multiplayer-0930: only this character leaves its (party) instance; the others keep fighting
                net.swordie.ms.world.field.InstanceMembership.onDisconnect(chr);
            } catch (RuntimeException e) {
                DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, e);
            }
            if (field != null) {
                if (field.getForcedReturn() != FieldConstants.NO_MAP_ID) {
                    chr.setFieldID(chr.getField().getForcedReturn());
                }
                if (field.getId() == FieldConstants.SUB_ZERO_HUNT) {
                    field.removeMobsByOnlyChar(chr);
                }
                field.removeChar(chr.getId());
            }
            TradeRoom tradeRoom = chr.getTradeRoom();
            if (tradeRoom != null) {
                tradeRoom.cancelTrade();
                Char other = tradeRoom.getOtherChar(chr.getId());
                if (other != null) { // không thể xảy ra nhưng cho chắc
                    other.write(MiniroomPacket.cancelTrade());
                    other.chatMessage("你的交易對象已斷開連接。");
                }
            }
            if (chr.getShop() != null) {
                chr.getShop().setChar(null);
                chr.setShop(null);
            }
            if (chr.getMobZoneDebuff() != null) {
                chr.setMobZoneDebuff(null);
            }
            if (chr.getScriptManager() != null) {
                chr.getScriptManager().dispose();
            }
            chr.cancelTimers();
            if (chr.getScriptManager() != null) {
                chr.getScriptManager().stopEvents();
            }
            chr.getAvatarData().getCharacterStat().setLastLogout(FileTime.currentTime());
            if (notifyDisconnect) {
                chr.chatPopup("你已與服務器斷開連接。");
            }
        }
        if (getNextState() == NONE) {
            this.currentState = LOGOUT;
            this.nextState = NONE;
        }

        final Char savingChr = chr;
        final int savingCharId = charID;
        final Account savingAccount = account != null ? account
                : chr.getAccount() != null ? chr.getAccount()
                : user != null && user.getCurrentAcc() != null ? user.getCurrentAcc() : null;
        final User savingUser = user;

        if (savingUser != null) {
            savingUser.setClientState(LOGOUT);
            savingUser.setCurrentChr(null);
            if (savingUser.getCurrentAcc() != null) {
                savingUser.getCurrentAcc().setCurrentChr(null);
            }
        }

        savingChr.setClient(null);
        Server.get().removeChar(savingCharId);
        net.swordie.ms.client.character.social.tms282.RingRecordService.get().onLogout(savingChr); // social: forget cached ring records
        net.swordie.ms.client.character.emoticon.EmoticonService.get().onLogout(savingChr); // emoticon cache
        setChr(null);
        setExiting(false);

        // step 2: counted by PendingSaves until it ran, so a graceful shutdown waits for it
        Runnable logoutSave = net.swordie.ms.lifecycle.PendingSaves.track(() -> {
            try {
                if (savingAccount != null) {
                    savingAccount.saveToSQL();
                }
                if (savingUser != null) {
                    savingUser.saveToSQL(true);
                }
                savingChr.saveToSQL();
            } catch (Exception e) {
                DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, e);
            }
        });
        try {
            Server.get().getServerTimer().execute(logoutSave);
        } catch (RuntimeException rejected) {
            logoutSave.run(); // the timer is shutting down: save on this thread instead of losing it
        }

        if (ServerConfig.IP_LOG) {
            DataPrinter.send(DataPrinter.LOGIN, String.format("[IP: %s] 已斷開與服務器的連接。", getIP()));
        }
    }

    public Map<Short, Short> getEncryptedHeaderToNormalHeaders() {
        return encryptedHeaderToNormalHeaders;
    }

    public void sendOpcodeEncryption() {
        byte[] key = new byte[24];
        System.arraycopy("N3x@nGLEUH@ckEr!".getBytes(), 0, key, 0, 16);
        System.arraycopy(key, 0, key, 16, 8);
        TripleDESCipher cipher = new TripleDESCipher(key);
        StringBuilder content = new StringBuilder();
        List<Integer> possibleNums = new ArrayList<>();
        for (int i = InHeader.B_E_G_I_N__U_S_E_R.getValue(); i < 9999; i++) {
            possibleNums.add(i);
        }
        for (short header = InHeader.B_E_G_I_N__U_S_E_R.getValue(); header < InHeader.NO.getValue(); header++) {
            int randNum = Util.getRandomFromCollection(possibleNums);
            possibleNums.remove((Integer) randNum);
            String num = String.format("%04d", randNum);
            encryptedHeaderToNormalHeaders.put((short) randNum, header);
            content.append(num);
        }
        content.append(" ");
        byte[] buf = new byte[Short.MAX_VALUE + 1];
        byte[] encryptedBuf = cipher.encrypt(content.toString().getBytes());
        System.arraycopy(encryptedBuf, 0, buf, 0, encryptedBuf.length);
        Random random = new Random();
        for (int i = encryptedBuf.length; i < buf.length; i++) {
            buf[i] = (byte) random.nextInt();
        }
        write(ClientSocket.opcodeEncryption(buf));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NettyClient c = (NettyClient) o;
        return getIP().equals(c.getIP()) && getPort().equals(c.getPort()) && getCh().equals(c.getCh());
    }

    @Override
    public int hashCode() {
        return Objects.hash(ch);
    }

    public long getLastAPIAttempt() {
        return lastAPIAttempt;
    }

    public void setLastAPIAttempt(long lastAPIAttempt) {
        this.lastAPIAttempt = lastAPIAttempt;
    }

    /**
     * While the client sits in the cash shop / auction house, any field-only packet
     * (effects, inventory ops, remote user updates...) crashes it. Filter at the single
     * send exit: only whitelisted headers pass; everything else is dropped and logged,
     * so future offenders identify themselves in the console.
     */
    @Override
    public void write(OutPacket outPacket) {
        if (currentState == IN_CASH_SHOP || currentState == IN_AUCTION_HOUSE) {
            // A packet built for a protocol profile carries that profile's opcode: name it from the profile's table
            // (TMS 282.3: data/protocol/tms282.3/outbound.tsv), not from the 265 OutHeader numbers.
            String name = outPacket.getBuiltFor() != null ? profileOutName(outPacket.getHeader())
                    : nameOf(OutHeader.getOutHeaderByOp(outPacket.getHeader()));
            if (!isSafeOutOfFieldName(name) && !isCashCapacitySync(outPacket)) {
                System.out.printf("[OutOfFieldFilter] dropped %s(%d) while state=%d%n",
                        name, outPacket.getHeader(), currentState);
                return;
            }
        }
        super.write(outPacket);
    }

    /** Only the proven native capacity update may accompany a failed optional cash purchase. */
    private boolean isCashCapacitySync(OutPacket packet) {
        if(currentState!=IN_CASH_SHOP || packet.getOrigin()!=null || packet.getHeader()!=0x009A
                || packet.getBuiltFor()!=net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3
                || net.swordie.ms.connection.protocol.Protocol.active().version()!=net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3)return false;
        byte[] bytes=packet.getData();
        return bytes.length==4 && Byte.toUnsignedInt(bytes[2])>=1 && Byte.toUnsignedInt(bytes[2])<=6
                && Byte.toUnsignedInt(bytes[3])>0;
    }

    private static String nameOf(OutHeader h) {
        return h == null ? null : h.name();
    }

    private static String profileOutName(int op) {
        return net.swordie.ms.connection.protocol.Protocol.active().opcodes().outbound(op & 0xFFFF)
                .map(net.swordie.ms.connection.protocol.OutboundOpcode::name).orElse(null);
    }

    /** PING is the TMS 282.3 name of the keep-alive (265 ALIVE_REQ); no 265 header has that name. */
    private static boolean isSafeOutOfFieldName(String n) {
        if (n == null) {
            return false;
        }
        return n.equals("PING") || n.startsWith("CASH_SHOP") || n.startsWith("SET_CASH_SHOP") || n.contains("AUCTION")
                || n.contains("MIGRATE") || n.equals("ALIVE_REQ") || n.equals("CLIENT_ALIVE")
                || n.equals("PING_CHECK_RESULT") || n.equals("IN_GAME_PING_REQ")
                || n.equals("BROADCAST_MSG") || n.equals("AUTH_MESSAGE") || n.contains("MAPLE_COIN")
                || n.equals("EXCL_REQUEST") || n.equals("SECURITY_PACKET_CODE")
                || n.equals("SET_CLIENT_KEY") || n.contains("MEMBER_SHOP");
    }

    public byte getCurrentState() {
        return currentState;
    }

    public void setCurrentState(byte currentState) {
        if (currentState != IN_FIELD) stopHeldSkills();
        this.currentState = currentState;
    }

    private void stopHeldSkills() {
        net.swordie.ms.client.jobs.flora.Tms282AdeleShardbreaker.clear(chr);
        net.swordie.ms.client.jobs.flora.Tms282AetherGuard.reset(chr, System.currentTimeMillis());
        net.swordie.ms.client.jobs.adventurer.thief.Tms282BladeTempest.reset(chr);
        net.swordie.ms.client.jobs.resistance.Tms282OmegaBlaster.reset(chr);
        net.swordie.ms.client.jobs.Jianghu.Tms282HowlingStorm.reset(chr);
        net.swordie.ms.client.jobs.cygnus.BlazeWizard.stopInferno(chr);
    }

    public byte getNextState() {
        return nextState;
    }

    public void setNextState(byte nextState) {
        this.nextState = nextState;
    }

    public boolean isMigrating() {
        return currentState == CHANGING_CHANNEL;
    }
}
