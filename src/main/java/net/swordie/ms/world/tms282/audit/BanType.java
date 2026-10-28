package net.swordie.ms.world.tms282.audit;

/** {@code account_block.type}. Types with {@code blocksLogin=false} are restrictions checked by feature code. */
public enum BanType {
    GENERAL(0, true, "一般封鎖"),
    HACK(1, true, "外掛/修改"),
    MACRO(2, true, "自動程式/測謊未過"),
    RMT(3, true, "現金交易/詐騙"),
    CHAT(4, false, "禁言"),
    TRADE(5, false, "禁止交易");

    private final int val;
    private final boolean blocksLogin;
    private final String label;

    BanType(int val, boolean blocksLogin, String label) {
        this.val = val;
        this.blocksLogin = blocksLogin;
        this.label = label;
    }

    public int getVal() {
        return val;
    }

    public boolean blocksLogin() {
        return blocksLogin;
    }

    public String getLabel() {
        return label;
    }

    public static BanType getByVal(int val) {
        for (BanType t : values()) {
            if (t.val == val) {
                return t;
            }
        }
        return null;
    }

    /** Accepts a number ("2") or a name ("macro"), case-insensitive. */
    public static BanType parse(String s) {
        if (s == null) {
            return null;
        }
        try {
            return getByVal(Integer.parseInt(s.trim()));
        } catch (NumberFormatException ignored) {
        }
        for (BanType t : values()) {
            if (t.name().equalsIgnoreCase(s.trim())) {
                return t;
            }
        }
        return null;
    }
}
