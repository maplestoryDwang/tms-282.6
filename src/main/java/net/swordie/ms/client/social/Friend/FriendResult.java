package net.swordie.ms.client.social.Friend;

import net.swordie.ms.enums.social.Friend.FriendType;
import net.swordie.ms.connection.OutPacket;

import java.util.Set;

public class FriendResult {

    private Set<Friend> friends;
    private Friend friend;
    private FriendType friendType;
    private String name;
    private boolean isAccountFriend;
    private int level;
    private int job;
    private int subJob;
    private boolean isOnline;
    private int channel;
    private byte maxFriendSlot;
    private byte showBox;
    private int mode;

    private boolean isDenied;
    private String message;

    private FriendResult(FriendType friendType) {
        this.friendType = friendType;
    }

    // ---- read access for profile codecs (TMS 282.3 Tms282FriendPackets) ----
    public FriendType getFriendType() { return friendType; }
    public Set<Friend> getFriends() { return friends; }
    public Friend getFriend() { return friend; }
    public String getName() { return name; }
    public boolean isAccountFriend() { return isAccountFriend; }
    public int getLevel() { return level; }
    public int getJob() { return job; }
    public int getSubJob() { return subJob; }
    public boolean isOnline() { return isOnline; }
    public int getChannel() { return channel; }

    public static FriendResult response_Set(String name) {
        FriendResult friendResult = new FriendResult(FriendType.Response_Set_Success);
        friendResult.name = name;
        return friendResult;
    }

    public static FriendResult response_Invite(Friend friend, boolean isAccountFriend, int level, int job, int subJob) {
        FriendResult friendResult = new FriendResult(FriendType.Response_Invite);
        friendResult.friend = friend;
        friendResult.isAccountFriend = isAccountFriend;
        friendResult.level = level;
        friendResult.job = job;
        friendResult.subJob = subJob;
        return friendResult;
    }

    public static FriendResult response_Load_Success(Set<Friend> friends) {
        FriendResult friendResult = new FriendResult(FriendType.Response_Load_Success);
        friendResult.friends = friends;
        return friendResult;
    }

    public static FriendResult response_Set_Messenger(boolean isDenied, String message) {
        FriendResult friendResult = new FriendResult(FriendType.Response_Set_Messenger);
        friendResult.isDenied = isDenied;
        friendResult.message = message;
        return friendResult;
    }

    public static FriendResult response_Set_MessengerMode(int mode) {
        FriendResult friendResult = new FriendResult(FriendType.Response_Set_MessengerMode);
        friendResult.mode = mode;
        return friendResult;
    }

    public static FriendResult response_Accept_Messenger(boolean isDenied, String message) {
        FriendResult friendResult = new FriendResult(FriendType.Response_Accept_Messenger);
        friendResult.isDenied = isDenied;
        friendResult.message = message;
        return friendResult;
    }

    public static FriendResult response_Delete_Success(Friend friend) {
        FriendResult friendResult = new FriendResult(FriendType.Response_Delete_Success);
        friendResult.friend = friend;
        return friendResult;
    }

    public static FriendResult response_Delete_Messenger(boolean isDenied, String message) {
        FriendResult friendResult = new FriendResult(FriendType.Response_Delete_Messenger);
        friendResult.isDenied = isDenied;
        friendResult.message = message;
        return friendResult;
    }

    public static FriendResult response_Notify(Friend friend, int channel, boolean isOnline) {
        FriendResult friendResult = new FriendResult(FriendType.Response_Notify);
        friendResult.friend = friend;
        friendResult.channel = channel;
        friendResult.isOnline = isOnline;
        return friendResult;
    }

    public static FriendResult response_NotifyChange_FriendInfo(Friend friend) {
        FriendResult friendResult = new FriendResult(FriendType.Response_NotifyChange_FriendInfo);
        friendResult.friend = friend;
        return friendResult;
    }

    public static FriendResult response_IncreaseMaxSlot_Success(byte maxFriendSlot) {
        FriendResult friendResult = new FriendResult(FriendType.Response_IncreaseMaxSlot_Success);
        friendResult.maxFriendSlot = maxFriendSlot;
        return friendResult;
    }

    public static FriendResult response_IncreaseMaxSlot_Messenger(boolean isDenied, String message) {
        FriendResult friendResult = new FriendResult(FriendType.Response_IncreaseMaxSlot_Messenger);
        friendResult.isDenied = isDenied;
        friendResult.message = message;
        return friendResult;
    }

    public static FriendResult response_Set_BlockedBehavior(String name) {
        FriendResult friendResult = new FriendResult(FriendType.Response_IncreaseMaxSlot_Messenger);
        friendResult.name = name;
        return friendResult;
    }

    public static FriendResult response_Notice_Deleted(String name) {
        FriendResult friendResult = new FriendResult(FriendType.Response_Notice_Deleted);
        friendResult.name = name;
        return friendResult;
    }

    public static FriendResult response_RejectEventBestFriend(String name) {
        FriendResult friendResult = new FriendResult(FriendType.Response_RejectEventBestFriend);
        friendResult.name = name;
        return friendResult;
    }

    public static FriendResult request_SetStarPlanetFriend_DisconnectUser(String name) {
        FriendResult friendResult = new FriendResult(FriendType.Response_Set_BlockedBehavior);
        friendResult.name = name;
        return friendResult;
    }

    public static FriendResult response_InviteEventBestFriend(Friend friend, String name){
        FriendResult friendResult = new FriendResult(FriendType.Response_InviteEventBestFriend);
        friendResult.friend = friend;
        friendResult.name = name;
        return friendResult;
    }

    public static FriendResult msg(FriendType type) {
        return new FriendResult(type);
    }

    public void encode(OutPacket outPacket) {
        outPacket.encodeByte(friendType.getVal());
        switch (friendType) {
            case Response_Load_Success:
                outPacket.encodeInt(friends.size());
                for (Friend friend : friends) {
                    friend.encode(outPacket);
                }
                break;
            case Response_NotifyChange_FriendInfo:
                outPacket.encodeInt(friend.getFriendID());
                outPacket.encodeInt(friend.getFriendAccountID());
                outPacket.encodeByte(0);
                friend.encode(outPacket);
                break;
            case Response_Invite:
                outPacket.encodeByte(isAccountFriend);
                outPacket.encodeInt(friend.getFriendID());
                outPacket.encodeInt(friend.getFriendAccountID());
                outPacket.encodeString(friend.getName());
                outPacket.encodeInt(level);
                outPacket.encodeInt(job);
                outPacket.encodeInt(subJob);
                friend.encode(outPacket);
                break;
            case Response_Set_SingleFriendInfo:
                friend.encode(outPacket);
                break;
            case Response_Delete_Success:
                outPacket.encodeByte(friend.isAccount());
                if (friend.isAccount()) {
                    outPacket.encodeInt(friend.getFriendAccountID());
                } else {
                    outPacket.encodeInt(friend.getFriendID());
                }
                break;
            case Response_Set_Unk219:
                outPacket.encodeString(friend.getName());
                outPacket.encodeByte(friend.isAccount());
                if (friend.isAccount()) {
                    outPacket.encodeInt(friend.getFriendAccountID());
                } else {
                    outPacket.encodeInt(friend.getFriendID());
                }
                break;
            case Response_Set_Success:
            case Response_Set_BlockedBehavior:
            case Response_Notice_Deleted:
            case Response_RejectEventBestFriend:
            case Response_SetStarPlanetFriend_DisconnectUser:
                outPacket.encodeString(name);
                break;
            case Response_Notify:
                outPacket.encodeInt(friend.getFriendID());
                outPacket.encodeInt(friend.getFriendAccountID());
                // if non exist - 38:
                outPacket.encodeByte(6); //sFriendName._m_pStr
                outPacket.encodeInt(channel);
                outPacket.encodeByte(friend.getFlag());
                outPacket.encodeByte(isOnline);
                if (friend.getFlag() != 0) {
                    outPacket.encodeString(friend.getName()); //Friend Name.
                }
                break;
            case Response_Set_Messenger:
            case Response_Accept_Messenger:
            case Response_Delete_Messenger:
            case Response_IncreaseMaxSlot_Messenger:
                outPacket.encodeByte(isDenied);
                if (isDenied) {
                    outPacket.encodeString(message);
                }
                break;
            case Response_Set_MessengerMode:
                outPacket.encodeInt(mode);
                break;
            case Response_IncreaseMaxSlot_Success:
                outPacket.encodeByte(maxFriendSlot);
                break;
            case Response_InviteEventBestFriend:
                outPacket.encodeInt(friend.getFriendID());
                outPacket.encodeString(friend.getName());
                break;
        }
    }
}
