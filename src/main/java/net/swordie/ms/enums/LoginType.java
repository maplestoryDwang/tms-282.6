package net.swordie.ms.enums;

/**
 * Created on 12/12/2017.
 *
 * @author Sjonnie
 */
public enum LoginType {
    Success(0),
    TempBlocked(1), //Nothing.

    //Return: "This ID has been deleted or blocked. Please try a different ID."
    Blocked(2),

    //Return: "This ID has been deleted or blocked. Please try a different ID."
    Abandoned(3),

    IncorrectPassword(4),
    NotRegistered(5),

    //Return: "Having trouble logging in? Try logging at maplestory.nexon.net or visit the nexon homepage to view support options."
    DBFail(6),
    AlreadyConnected(7),
    NotConnectableWorld(8),

    //Return: "Having trouble logging in? Try logging at maplestory.nexon.net or visit the nexon homepage to view support options."
    Unknown(9),

    //Return: "The server is to busy to fullfill your request right now. Please try again later."
    Timeout(10),


    NotAdult(11),
    AuthFail(12), //-38
    ImpossibleIP(13),

    //Return: "You cannot login from your region. Check your personal information on our website or visit the Nexon homepage to see if your country is supported."
    NotAuthorizedNexonID(14),

    NoNexonID(15),

    //Return: "Your account needs to be verified before you can login. Please check your email and try again."
    IncorrectSSN2(16),
    WebAuthNeeded(17), //C++
    DeleteCharacterFailedOnGuildMaster(18),
    TempBlockedIP(19),
    IncorrectSPW(20),

    //Return: "Your account needs to be verified before you can login. Please check your email and try again."
    DeleteCharacterFailedEngaged(21),
    SamePasswordAndSPW(22), //Nothing
    WaitOTP(23), //Nothing.
    WrongOTP(24),
    OverCountErrOTP(25), //Nothing.
    SystemErr(26),
    CancelInputDeleteCharacterOTP(27), //C++
    PaymentWarning(28),
    DeleteCharacterFailedOnFamily(29),
    InvalidCharacterName(30),
    IncorrectSSN(31),
    SSNConfirmFailed(32),
    SSNNotConfirmed(33), //Nothing.
    WorldTooBusy(34), //Nothing.
    OTPReissuing(35), //Nothing.
    OTPInfoNotExist(36), //Nothing.
    Shutdowned(37), //Nothing.
    DeleteCharacterFailedHasEntrustedShop(38), //Nothing.
    AlbaPerform(39),
    TransferredToNxEmailID(40),

    //Return: "Please login using Maple ID or Nexon e-mail ID."
    UntransferredToNxEmailID(41),
    RequestedMapleIDAlreadyInUse(42), //Nothing.
    WaitSelectAccount(43), //Nothing.
    DeleteCharacterFailedProtectedItem(44),

    //Return: "You can't delete a character on an account that has Maple Together co-op permission."
    UnauthorizedUser(45),

    CannotCreateMoreMapleAccount(46), // C++
    CreateBanned(47),
    CreateTemporarilyBanned(48), //Nothing
    EventNewCharacterExpireFail(49),

    //Return: "Under youth protection legislation, all online games are time restricted for those under age of 18."
    SelectiveShutdowned(50),

    NonownerRequest(51), //Nothing.

    //Return: "Your PIC must be different from your password."
    OTPRequired(52),

    //Return: "You must create your Personal Identification Code (PIC) before proceeding. Please create one now."
    GuestServiceClosed(53),
    BlockedNexonID(54),
    DupMachineID(55), //Nothing.
    NotActiveAccount(56), //Nothing.
    IncorrectSPW4th(57),
    IncorrectSPW5th(58),
    InsufficientSPW(59), //Nothing.
    SameCharSPW(60),

    WebLaunchingOTPRequired(61), //Nothing.
    MergeWorld_CreateCharacterBanned(62),
    ChangeNewOTP(63),

    //Return: "Overseas login block activated. Please log in from Nexon.net."
    BlockedByServiceArea(64),

    ExceedReservedDeleteCharacter(65),
    UnionFieldChannelClosed(67), //Nothing.
    ProtectAccount(68),
    AntiMacroReq(69), //Nothing.
    AntiMacroCreateFailed(70),
    AntiMacroIncorrect(71),
    LimitCreateCharacter(72),

    //Return: "You've entered the wrong password to many times. You will blocked from logging in for a short time. Use the Find Password option below to confirm your identity and then try logging in again."
    ProtectSSOLogin(73),

    InvalidMapleIDThroughMobile(74), //Nothing.
    InvalidPasswordThroughMobile(75),
    HashedPasswordIsEmpty(76), //C++
    NGS_For_Ass(77), //Nothing.
    AlreadyConnectedThroughMobile(78),
    //Nothing
    Protected_For_Ass(79),
    Blocked_For_Ass(80),
    WrongVer(81),
    EMailVerify(82),

    //Return: "This character is only available until 10/6/2015."
    DenyJob(83),

    //Nothing
    InvalidObject(84),
    IncorrectLoginType_OtherToMapleID(85),
    FailedUserCreate(86),
    MobileTokenInvalid(87),
    MobileTokenDeviceIDInvalid(88),
    MobileTokenExpired(89),
    NotHaveNaverID(90),
    UserTossAIPlayer(91),

    //Korean
    InactivateMember(92),

    ProcFail(-1),

    ;
    private final byte value;

    LoginType(int value) {
        this.value = (byte) value;
    }

    public byte getValue() {
        return value;
    }
}
