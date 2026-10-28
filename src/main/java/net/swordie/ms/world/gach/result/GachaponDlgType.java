package net.swordie.ms.world.gach.result;

public enum GachaponDlgType {// or TicketEffect but nvm
    TOWN(0),
    SPECIAL(4),
    REMOTE(5);

    private final int type;

    GachaponDlgType(final int type) {
        this.type = type;
    }

    public int getType() {
        return type;
    }
}
