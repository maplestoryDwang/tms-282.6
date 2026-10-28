package net.swordie.ms.enums;

public enum AvatarModifiedMask {
    AvatarLook(0x1),
    Speed(0x2),
    CarryItemEffect(0x4),
    SubAvatarLook(0x8),
    NotifyAvatarModified(0x9);

    private final byte val;

    AvatarModifiedMask(int val) {
        this.val = (byte) val;
    }

    public byte getVal() {
        return val;
    }
}
