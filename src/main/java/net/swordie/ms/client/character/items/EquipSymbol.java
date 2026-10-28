package net.swordie.ms.client.character.items;

import net.swordie.ms.connection.OutPacket;

public class EquipSymbol {
    private boolean isSacred;
    private short inc;
    private int arcExp;
    private int arcLevel;

    public EquipSymbol() {
    }

    public EquipSymbol(short inc, int arcExp, int arcLevel, boolean isSacred) {
        this.inc = inc;
        this.arcExp = arcExp;
        this.arcLevel = arcLevel;
        this.isSacred = isSacred;
    }

    public EquipSymbol deepCopy() {
        EquipSymbol ret = new EquipSymbol();
        ret.inc = getInc();
        ret.arcExp = getExp();
        ret.arcLevel = getLevel();
        ret.isSacred = isSacred();
        return ret;
    }

    public void reset() {
        this.inc = 0;
        this.arcExp = 0;
        this.arcLevel = 0;
    }

    public int getGrowthCost() {
        int level = getLevel();
        return isSacred() ? 9 * level * level + 20 * level : level * level + 11;
    }

    public long getMesoCost() {
        return isSacred()
                ? 100_000L * (long) Math.floor(getGrowthCost() * (13.2 - 0.6 * getLevel()))
                : 10_000L * (long) Math.floor(getGrowthCost() * (8 + 0.1 * getLevel()));
    }

    /**
     * The meso cost of the next level. On 282 the client's own table (static init 46940, cost AA07A0; research
     * union-282/REPORT.md): growth x (base + level x perLevel), floored to 10k (arcane) / 100k (authentic, grand);
     * the 265 formula matches only 1712001 and 1713000. Other versions and unknown symbols: {@link #getMesoCost()}.
     */
    public static long mesoCost(int itemId, EquipSymbol symbol) {
        if (net.swordie.ms.connection.protocol.Protocol.active().version()
                == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3) {
            long cost = tms282MesoCost(itemId, symbol.getLevel());
            if (cost > 0) {
                return cost;
            }
        }
        return symbol.getMesoCost();
    }

    /** The 282 table cost, or 0 for a symbol the table does not know (1712000 cannot be upgraded). */
    public static long tms282MesoCost(int itemId, int level) {
        long base;
        long perLevel;
        if (itemId >= 1712001 && itemId <= 1712006) {
            base = 80_000L + (itemId - 1712001) * 20_000L;
            perLevel = 1_000L;
        } else if (itemId >= 1713000 && itemId <= 1713005) {
            base = 1_320_000L + (itemId - 1713000) * 180_000L;
            perLevel = -60_000L;
        } else if (itemId == 1714000 || itemId == 1714001) {
            base = itemId == 1714000 ? 3_980_000L : 4_880_000L;
            perLevel = -60_000L;
        } else {
            return 0;
        }
        boolean arcane = itemId / 1000 == 1712;
        long growth = arcane ? (long) level * level + 11 : 9L * level * level + 20L * level;
        long unit = arcane ? 10_000L : 100_000L;
        long cost = growth * (base + level * perLevel);
        return Math.max(0, cost / unit * unit);
    }

    /** TMS plain merge materials include the normal giver's level-1 / exp-1 symbols. */
    public boolean isPlainMergeMaterial() {
        return getLevel() == 1 && (getExp() == 0 || getExp() == 1
                && net.swordie.ms.connection.protocol.Protocol.active().version()
                == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3);
    }

    public boolean canLevelUp() {
        return getLevel() >= 1 && getLevel() < (isSacred() ? 11 : 20)
                && getExp() >= getGrowthCost();
    }

    public void encode(OutPacket outPacket) {
        int curLevel = getLevel();
        int costGrowth;
        long mesoCost;

        outPacket.encodeInt(0);
        outPacket.encodeInt(curLevel); // level
        costGrowth = getGrowthCost();
        mesoCost = getMesoCost();
        outPacket.encodeInt(costGrowth);                   // Cost Growth
        outPacket.encodeLong(mesoCost);                    // Cost Required
        outPacket.encodeLong(0);
        outPacket.encodeShort(0);
        outPacket.encodeShort(0);
        outPacket.encodeShort(0);
        outPacket.encodeShort(0);
        outPacket.encodeShort(0);
        outPacket.encodeShort(0);
        outPacket.encodeShort(0);
        outPacket.encodeShort(0);
        outPacket.encodeShort(0);
        outPacket.encodeShort(0);
        outPacket.encodeShort(0);
        outPacket.encodeShort(0);
    }

    public short getInc() {
        return inc;
    }

    public void setInc(short inc) {
        this.inc = inc;
    }

    public int getExp() {
        return arcExp;
    }

    public void setExp(int arcExp) {
        this.arcExp = arcExp;
    }

    public int getLevel() {
        return arcLevel;
    }

    public void setLevel(int arcLevel) {
        this.arcLevel = arcLevel;
    }

    public boolean isSacred() {
        return isSacred;
    }

    public void setSacred(boolean isSacred) {
        this.isSacred = isSacred;
    }
}
