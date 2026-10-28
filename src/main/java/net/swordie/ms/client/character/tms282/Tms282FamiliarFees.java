package net.swordie.ms.client.character.tms282;

/**
 * 萌獸 fees sent to the client (LP0425 controller leaves 42..58) and charged by Tms282FamiliarGrowth. Values: the
 * Beanfun 282 announcement / 279 familiar.yml (familiar-20260927/fee_initialization_check.py, emulator-verified
 * layout); -1 = not available for that grade. Link fees are charged in mesos (currency not proven).
 */
public final class Tms282FamiliarFees {
    public static final int APPEARANCE_CHANGE = 2500;
    public static final int APPEARANCE_RESTORE = 1200;
    public static final int LINK_ACTIVATE_MAX_HOURS = 720;
    public static final int[] UPGRADE_NORMAL = {120, 300, 1100, 3200};
    public static final int[] UPGRADE_SPECIAL = {-1, -1, 1100, 3200};
    public static final int[] DISASSEMBLE_NORMAL = {3, 10, 30, 100, 400};
    public static final int[] DISASSEMBLE_SPECIAL = {-1, -1, 1000, 3000, 5000};
    public static final int[] LINK_UNLOCK = {3000, 5000};
    public static final int[] LINK_APPLY = {1000, 1000, 1000};
    public static final int[] LINK_ACTIVATE = {2000, 12000, 48000};

    private Tms282FamiliarFees() {
    }

    /** Essence to fill the upgrade gauge of {@code grade}; 0 when it cannot be upgraded. */
    public static int upgradeEssence(int grade) {
        return at(UPGRADE_NORMAL, grade);
    }

    public static int disassembleEssence(int grade) {
        return at(DISASSEMBLE_NORMAL, grade);
    }

    public static int linkUnlock(int index) {
        return at(LINK_UNLOCK, index);
    }

    public static int linkApply(int slot) {
        return at(LINK_APPLY, slot);
    }

    public static int linkActivate(int slot) {
        return at(LINK_ACTIVATE, slot);
    }

    static int at(int[] values, int i) {
        return i < 0 || i >= values.length ? 0 : Math.max(0, values[i]);
    }
}
