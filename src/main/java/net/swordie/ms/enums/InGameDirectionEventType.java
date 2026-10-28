package net.swordie.ms.enums;

import net.swordie.ms.util.Util;

public enum InGameDirectionEventType {
    ForcedAction(0),
    Unk1(1),
    Delay(2),
    EffectPlay(3),
    ForcedInput(4),
    PatternInputRequest(5),
    Unk6(6),
    CameraMove(7),// automated send delay
    CameraOnCharacter(8),
    CameraZoom(9),// automated send delay
    Unk10(10),
    CameraReleaseFromUserPoint(11),
    Unk12(12),
    VansheeMode(13),
    FaceOff(14),
    Monologue(15),
    MonologueScroll(16),
    AvatarLookSet(17),
    Unk18(18),
    Unk19(19),
    RemoveAdditionalEffect(20),
    RemoveAdditionalEffect_2(21),
    ForcedMove(22),
    ForcedFlip(23),
    Unk24(24),
    InputUI(25),
    CloseUI(26),
    Unk27(27),
    Unk28(28),
    Unk29(29),
    Unk30(30),
    Unk31(31),
    Unk32(32),
    Unk33(33),
    Unk34(34),
    Unk35(35),
    Unk36(36),
    Unk37(37),
    Unk38(38),
    Monologue_2(39),
    Unk40(40),
    ;

    private final int val;

    InGameDirectionEventType(int val) {
        this.val = val;
    }

    public static InGameDirectionEventType getByVal(byte val) {
        return Util.findWithPred(values(), v -> v.getVal() == val);
    }

    public int getVal() {
        return val;
    }
}
