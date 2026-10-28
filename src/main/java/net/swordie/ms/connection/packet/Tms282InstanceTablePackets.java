package net.swordie.ms.connection.packet;

import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.constants.SkillConstants;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.OptionalInt;

import static net.swordie.ms.connection.packet.Tms282LoginPacketSupport.str;

/**
 * TMS 282.3 LP0121 RESULT_INSTANCE_TABLE (CWvsContext handler 5FC9E00), the answer to CP01C0
 * ({@link Tms282InstanceTableRequest}) [verified: setfield_trace.py --entry 5FC9E00, expected/infield-instance-table.tsv]:
 * <pre>
 * Str  table     5FC9E29
 * i32  type      5FC9E32
 * i32  subType   5FC9E3C
 * u8   ok        5FC9E46
 * i32  value     5FC9E52
 * </pre>
 * Same as 265 WvsContext.resultInstanceTable. The handler formats a key from (table, type, subType) and stores the
 * value in the map at ctx+3980, which the requesting lookup (5FCA4DA/5FCAB22) reads.
 * <p>
 * Values ({@link #valueOf}) follow 265 UserStatHandler.handleUserRequestInstanceTable / SkillConstants
 * [presumed: 265 rules; the 282 client has no such table in Etc (InstanceTable.img holds only
 * dropWeaponOptionValue, dropArmorOptionValue and ADIndexVar), so the values are server data]:
 * <ul>
 * <li>{@code "hyper"}: level = type x 5; subType 1 (265 HyperActiveSkill) = active SP at 150/170/200, other
 * subTypes (0 = 265 HyperPassiveSkill) = passive SP every 10 levels from 140 to 220. Note: infield-capture 2.2
 * reads sub 0 as active; 265's enum and handler say sub 1 is active, and this follows 265.</li>
 * <li>{@code "incHyperStat"}: type = level, 3 + (level - 140) / 10.</li>
 * <li>{@code "needHyperStatLv"}: type = hyper stat level, the SP it costs.</li>
 * </ul>
 * Anything else (profession tables need the character's making-skill level) has no value here; the caller then
 * answers with ok = 0 or not at all.
 */
public final class Tms282InstanceTablePackets {
    public static final int RESULT_INSTANCE_TABLE = 0x0121;
    public static final String TABLE_HYPER = "hyper";
    public static final String TABLE_INC_HYPER_STAT = "incHyperStat";
    public static final String TABLE_NEED_HYPER_STAT_LV = "needHyperStatLv";
    /** 265 InstanceTableType.HyperActiveSkill subType. */
    public static final int HYPER_SUB_ACTIVE = 1;
    /** 265 InstanceTableType.HyperPassiveSkill subType. */
    public static final int HYPER_SUB_PASSIVE = 0;

    private Tms282InstanceTablePackets() {
    }

    /** LP0121. Table names are ASCII; they are written as US-ASCII. */
    public static OutPacket result(String table, int type, int subType, boolean ok, int value) {
        Objects.requireNonNull(table, "table");
        if (!StandardCharsets.US_ASCII.newEncoder().canEncode(table)) {
            throw new IllegalArgumentException("Instance table name must be ASCII");
        }
        OutPacket out = Tms282LoginPacketSupport.packet(RESULT_INSTANCE_TABLE);
        str(out, table, StandardCharsets.US_ASCII);
        // encodeInt(long): OutPacket.encodeInt(int) silently drops the value -88888.
        out.encodeInt((long) type);
        out.encodeInt((long) subType);
        out.encodeByte(ok ? 1 : 0);
        out.encodeInt((long) value);
        return out;
    }

    /** LP0121 answering {@code request}: ok = 1 with the {@link #valueOf} value, or ok = 0 and 0 when there is none. */
    public static OutPacket answer(Tms282InstanceTableRequest request) {
        Objects.requireNonNull(request, "request");
        OptionalInt value = valueOf(request);
        return result(request.table(), request.type(), request.subType(), value.isPresent(), value.orElse(0));
    }

    /** The table value by the 265 rules above; empty for tables this server has no data for. */
    public static OptionalInt valueOf(Tms282InstanceTableRequest request) {
        Objects.requireNonNull(request, "request");
        return switch (request.table()) {
            case TABLE_HYPER -> {
                long level = (long) request.type() * 5;
                if (level < 0 || level > Integer.MAX_VALUE) yield OptionalInt.empty();
                yield OptionalInt.of(request.subType() == HYPER_SUB_ACTIVE
                        ? SkillConstants.getHyperActiveSkillSpByLv((int) level)
                        : SkillConstants.getHyperPassiveSkillSpByLv((int) level));
            }
            case TABLE_INC_HYPER_STAT -> request.type() < 0 || request.type() > Short.MAX_VALUE ? OptionalInt.empty()
                    : OptionalInt.of(SkillConstants.getHyperStatSpByLv((short) request.type()));
            case TABLE_NEED_HYPER_STAT_LV -> OptionalInt.of(SkillConstants.getNeededSpForHyperStatSkill(request.type()));
            default -> OptionalInt.empty();
        };
    }
}
