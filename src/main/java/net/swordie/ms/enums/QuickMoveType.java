package net.swordie.ms.enums;

public enum QuickMoveType {
    MonsterPark(100),
    Ardentmill(101),
    LegionCoinShop(102),
    CrystalShop(103),
    AuctionHouse(104),
    MesoMarket(105),
    DimensionalPortal(106),
    SymbolExpressPass(107),
    MapleAdministrator(109),
    StorageRoom(110),
    CubeShop(304),
    ;
    private final int val;

    QuickMoveType(int val) {
        this.val = val;
    }

    public int getVal() {
        return val;
    }

    public static QuickMoveType getValByNum(int num) {
        for (QuickMoveType at : QuickMoveType.values()) {
            if (at.getVal() == num) {
                return at;
            }
        }
        return null;
    }
}
