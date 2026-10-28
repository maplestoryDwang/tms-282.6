package net.swordie.ms.enums.social.Friend;

import java.util.Arrays;

public enum FriendType {
    Request_Load(0),
    Request_Set(1),
    Request_Accept(2),
    Request_AccountAccept(3),
    Request_Delete(4),
    Request_AccountDelete(5),
    Request_Rejected(6),
    Request_AccountRejected(7),
    Request_NotifyLogin(8),
    Request_NotifyLogout(9),
    Request_IncreaseMaxSlot(10),
    Request_AccountConvert(11),
    Request_Modify(12),
    Request_GroupModify(13),
    Request_AccountGroupModify(14),
    Request_LogoutSet(15),
    Request_LoginSet(16),
    Request_BlackListSet(17),
    Request_BlackListDelete(18),
    Request_PointInfoLoad(19),
    Request_EventBestFriendInvite(20), //IDK
    Request_EventBestFriendAccept(21),
    Request_EventBestFriendReject(22),

    Response_Load_Success(21), // v263

    Response_NotifyChange_FriendInfo(24), // v263

    Response_Invite(26), // v263

    Response_Set_Success( 27), // v263

    //Return: BroadcastMsg: "Your buddy list is full."
    Response_Set_CurrentSlotFull(28), // v263

    //Return: BroadcastMsg: "The user's buddy list is full."
    Response_Set_TargetSlotFull(29), // v263

    //Return: BroadcastMsg: "The character is already registered as your buddy."
    Response_Set_AlreadySet(30), // v263

    //Return: BroadcastMsg: "Account buddy request already sent."
    Response_Set_AlreadyRequested(31), // v263

    //Return: BroadcastMsg: "That player is waiting to be added as a buddy."
    Response_Set_Ready(32), // v263

    //Return: BroadcastMsg: "You can't enter yourself as your buddy."
    Response_Set_CantAddYourSelf(33), // v263

    //Return: BroadcastMsg: "Gamemaster is not avaiable as a buddy."
    Response_Set_isGameMaster(34), // v263

    //Return: BroadcastMsg: "That character is not registered."
    Response_Set_UnknownUser(35), // v263

    //Encode: Byte (isDenied)
    //Encode: String (message)
    //Return: BroadcastMsg: "The request was denied due to an unknown error." (isDenied)
    //Return: BroadcastMsg: message (!isDenied)
    Response_Set_Messenger(36), // v263

    //Return: BroadcastMsg: "You are still waiting to be added as a buddy."
    Response_Set_RemainCharacterFriend(37), // v263

    //Encode: String (message)
    //Encode: Byte (???)
    //Encode: Int (isAccount ? getFriendAccountID : getFriendID)
    Response_Set_Unk219(38),

    //Encode: Int (mode)
    //Return: ???
    Response_Set_MessengerMode(39),

    //Encode: CInPacket::DecodeBuffer(a2, this, 0x13Du);
    Response_Set_SingleFriendInfo(40),

    //Encode: Byte (isDenied)
    //Encode: String (message)
    //Return: BroadcastMsg: "The request was denied due to an unknown error." (isDenied)
    //Return: BroadcastMsg: message (!isDenied)
    Response_Accept_Messenger(41),

    //Encode: Byte (isAccount)
    //Encode: Int (isAccount ? getFriendAccountID : getFriendID)
    //Action: Delete Friend Character in Friend List
    Response_Delete_Success(43), // v263

    //Encode: Byte (isDenied)
    //Encode: String (message)
    //Return: BroadcastMsg: "The request was denied due to an unknown error." (isDenied)
    //Return: BroadcastMsg: message (!isDenied)
    Response_Delete_Messenger(44), // v263

    Response_Notify(45), // v263

    Response_NotifyNewFriend(46), // v263

    //Encode: Byte (max Friend Slot)
    //Return: BroadcastMsg: "Your friends list has increased by 5 slots! Your wallet is 50000 Mesos lighter, but now you can make more friends."
    Response_IncreaseMaxSlot_Success(47), // v263

    //Encode: Byte (isDenied)
    //Encode: String (message)
    //Return: BroadcastMsg: "The request was denied due to an unknown error." (isDenied)
    //Return: BroadcastMsg: message (!isDenied)
    Response_IncreaseMaxSlot_Messenger(48), // v263

    //Return: BroadcastMsg: "You've already made the Friend Request. Please try again later."
    Response_AlreadyMadeFriendRequest(49), // v263

    //Encode: String (name)
    //Return: BroadcastMsg: "The request was denied due to an unknown error."
    Response_Set_BlockedBehavior(50), // v263

    //Encode: String (name)
    //Return: SystemNotice: "%s has declined the friend request."
    Response_Notice_Deleted(51),

    //Encode: Int (friendID) not sure
    //Encode: String (name)
    //Return: FadeYesNo: "from '%s'\r\nA request to become besties."
    Response_InviteEventBestFriend(52),

    //Encode: String (name)
    //Return: SystemNotice: "%s has declined the bestie request."
    Response_RejectEventBestFriend(53),

    //Return: BroadcastMsg: "Your buddy list is full."
    Response_SetStarPlanetFriend_FullMe(60),

    Response_Unk61(61),

    //Return: BroadcastMsg: "The character is already registered as your buddy."
    Response_SetStarPlanetFriend_AlreadySet(63),

    //Return: BroadcastMsg: "Account buddy request already sent."
    Response_SetStarPlanetFriend_AlreadyRequested(64),

    Response_Unk65(65),

    Response_Unk66(66), //Same 35

    //Return: BroadcastMsg: "Gamemaster is not available as a buddy."
    Response_SetStarPlanetFriend_UnknownUser(67),

    //Encode: String (name).
    //Return: BroadcastMsg: "You cannot add %s as a friend now, because Static is currently disconnected from Star Planet."
    Response_SetStarPlanetFriend_DisconnectUser(68),

    //Nothing happened
    Response_Unk77(77),

    //System Notice: "Your friend rejected the invite."
    Repsonse_Unk78(78),

    //System Notice: "Your friend could not be found."
    Response_Unk79(79),

    Response_Unk80(80),
    ;

    private final int val;

    FriendType(int val) {
        this.val = val;
    }

    public static FriendType getTypeByVal(byte val) {
        return Arrays.stream(values()).filter(grt -> grt.getVal() == val).findAny().orElse(null);
    }

    public int getVal() {
        return val;
    }
}
