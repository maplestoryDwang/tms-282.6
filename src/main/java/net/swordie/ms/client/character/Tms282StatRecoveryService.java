package net.swordie.ms.client.character;

import net.swordie.ms.connection.hikariCP.DatabaseManager;
import net.swordie.ms.connection.packet.Tms282StatRecoveryRequest;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Objects;

/**
 * TMS 282.3 natural HP/MP recovery (CP0193, {@link Tms282StatRecoveryRequest}) on top of the 265 model. Wire formats
 * live in the request record and Tms282StatPackets; this class decides the new values and writes them back.
 * <p>
 * Cap: the 265 <b>total</b> max, {@link Char#getMaxHP()} / {@link Char#getMaxMP()} (equips, set effects, passive
 * skills, character potential, temporary stats), not the base {@link CharacterStat#getMaxHp()}. The client sends CP0193
 * whenever HP/MP is below its own total max: live session channel-1790159554177 (Tanjiro, level 10, base 1000/500
 * sent in LP02EF) kept requesting HP/MP from full base, and 265 computes 2500/1250 for that character (passive
 * 121000004). The base maximum stays what the client gets: LP02EF and the LP009D mhp/mmp fields are base values that
 * the client multiplies itself (265 Char.getClientMaxStatPacketValue sends the base-before-bonus value; sending the
 * total there would count the bonuses twice), so nothing else has to be sent for the bar to reach 100%.
 * <p>
 * If the 265 computation fails (a character without the data it needs), the base maximum is used and
 * {@link MaxHpMp#fromTotal()} is false, so the caller can log it.
 */
public final class Tms282StatRecoveryService {
    private Tms282StatRecoveryService() {
    }

    /** The cap used for one recovery. */
    public record MaxHpMp(int maxHp, int maxMp, boolean fromTotal, int baseMaxHp, int baseMaxMp) {
    }

    /** One applied recovery: values before and after, and the cap. */
    public record Applied(int oldHp, int newHp, int oldMp, int newMp, MaxHpMp cap) {
        public boolean changed() {
            return oldHp != newHp || oldMp != newMp;
        }
    }

    /** 265 total max HP/MP, or the base max when the 265 computation throws. */
    public static MaxHpMp maxHpMp(Char chr) {
        Objects.requireNonNull(chr, "chr");
        CharacterStat stat = stat(chr);
        try {
            return new MaxHpMp(chr.getMaxHP(), chr.getMaxMP(), true, stat.getMaxHp(), stat.getMaxMp());
        } catch (RuntimeException e) {
            return new MaxHpMp(stat.getMaxHp(), stat.getMaxMp(), false, stat.getMaxHp(), stat.getMaxMp());
        }
    }

    /** In memory: HP/MP = current + requested amount, capped at {@link #maxHpMp}; written to the CharacterStat. */
    public static Applied apply(Char chr, Tms282StatRecoveryRequest request) {
        Objects.requireNonNull(request, "request");
        MaxHpMp cap = maxHpMp(chr);
        CharacterStat stat = stat(chr);
        int oldHp = stat.getHp();
        int oldMp = stat.getMp();
        Tms282StatRecoveryRequest.Recovered r = request.applyTo(oldHp, cap.maxHp(), oldMp, cap.maxMp());
        stat.setHp(r.hp());
        stat.setMp(r.mp());
        return new Applied(oldHp, r.hp(), oldMp, r.mp(), cap);
    }

    /**
     * Writes only hp and mp of the character's characterstats row ({@link CharacterStat#getId()}). Throws
     * IllegalStateException when the row is missing or the update fails.
     */
    public static void saveHpMp(Char chr) {
        CharacterStat stat = stat(chr);
        if (stat.getId() <= 0) {
            throw new IllegalStateException("Character " + chr.getId() + " has no characterstats row");
        }
        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement("UPDATE characterstats SET hp = ?, mp = ? WHERE id = ?")) {
            ps.setInt(1, stat.getHp());
            ps.setInt(2, stat.getMp());
            ps.setInt(3, stat.getId());
            if (ps.executeUpdate() != 1) {
                throw new IllegalStateException("characterstats row " + stat.getId() + " not updated");
            }
        } catch (SQLException e) {
            throw new IllegalStateException("HP/MP save failed for character " + chr.getId(), e);
        }
    }

    private static CharacterStat stat(Char chr) {
        Objects.requireNonNull(chr, "chr");
        if (chr.getAvatarData() == null || chr.getAvatarData().getCharacterStat() == null) {
            throw new IllegalArgumentException("Character " + chr.getId() + " has no CharacterStat");
        }
        return chr.getAvatarData().getCharacterStat();
    }
}
