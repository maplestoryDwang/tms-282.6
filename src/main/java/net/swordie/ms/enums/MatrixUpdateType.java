package net.swordie.ms.enums;

import java.util.Arrays;

/**
 * Created by MechAviv on 2/16/2019.
 */
public enum MatrixUpdateType {
    Activate(0),
    Deactivate(1),
    Swap(2),

    Enhance(4),
    EnhanceAll(5),
    Disassemble(6),
    DisassembleGroup(7),
    CraftNode(8),
    CraftCustomBoostNode(9),
    CraftNodestone(10),
    EnhanceSlot(11),
    ExpandSlot(12),
    EnhanceReset(13),
    Update(14),
    Lock(15),
    Unlock(16),
    ;

    private final int val;

    MatrixUpdateType(int val) {
        this.val = val;
    }

    public int getVal() {
        return val;
    }

    public static MatrixUpdateType getUpdateTypeByVal(int val) {
        return Arrays.stream(values()).filter(vut -> vut.getVal() == val).findAny().orElse(null);
    }
}
