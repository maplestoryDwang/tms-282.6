package net.swordie.ms.client.character.social.tms282;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.packet.Effect;
import net.swordie.ms.connection.packet.WvsContext;
import net.swordie.ms.constants.GameConstants;
import net.swordie.ms.enums.AccountType;
import net.swordie.ms.enums.PopularityResultType;
import net.swordie.ms.enums.Stat;
import net.swordie.ms.util.DataPrinter;
import net.swordie.ms.util.FileTime;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Fame (人氣) with persisted history ({@link SocialTables#POP}): every fame / defame is one row
 * (character_id, target_id, created_at, increase), so both limits survive a restart:
 * <ul>
 *     <li>daily limit: at most {@link #DAILY_LIMIT} per {@link GameConstants#FAME_COOLDOWN} hours (rolling);</li>
 *     <li>one fame per target per {@link #TARGET_COOLDOWN_DAYS} days.</li>
 * </ul>
 * GM accounts (account type above Player) skip both limits, as in 265. The replies are the unchanged 265
 * {@link WvsContext#givePopularityResult} packets (282: LP00AC through {@code Tms282PopularityTranslators}).
 * A database failure refuses the fame (fail closed) instead of allowing unlimited fame.
 */
public final class FameService {
    public static final int DAILY_LIMIT = 1;
    public static final int TARGET_COOLDOWN_DAYS = 30;
    private static final FameService INSTANCE = new FameService();
    private static final Object LOCK = new Object();

    public enum Verdict { OK, NO_TARGET, SELF, LEVEL_LOW, DONE_TODAY, DONE_TARGET, FAILED }

    private FameService() {
    }

    public static FameService get() {
        return INSTANCE;
    }

    /**
     * USER_GIVE_POPULARITY_REQUEST body (i32 target character id, u8 1 = fame / 0 = defame), 265 and 282 alike
     * (282 CP0198, sender 5EE13A0: seq 4 1). Sends the replies and always ends with {@code chr.dispose()}.
     */
    public Verdict give(Char chr, int targetChrId, boolean increase) {
        Char target = chr.getField() == null ? null : chr.getField().getCharByID(targetChrId);
        Verdict v;
        synchronized (LOCK) {
            v = check(chr, target);
            if (v == Verdict.OK && !record(chr.getId(), target.getId(), increase)) {
                v = Verdict.FAILED;
            }
        }
        switch (v) {
            case OK -> {
                target.addStatAndSendPacket(Stat.pop, increase ? 1 : -1);
                int pop = target.getAvatarData().getCharacterStat().getPop();
                chr.write(WvsContext.givePopularityResult(PopularityResultType.Success, target, pop, increase));
                target.write(WvsContext.givePopularityResult(PopularityResultType.Notify, chr, pop, increase));
                chr.getAvatarData().getCharacterStat().setNextAvailableFameTime(
                        FileTime.fromDate(LocalDateTime.now().plusHours(GameConstants.FAME_COOLDOWN)));
                if (increase) {
                    Effect.showFameGradeUp(target);
                }
            }
            case NO_TARGET -> chr.write(WvsContext.givePopularityResult(PopularityResultType.InvalidCharacterId, target, 0, increase));
            case LEVEL_LOW -> chr.write(WvsContext.givePopularityResult(PopularityResultType.LevelLow, target, 0, increase));
            case DONE_TODAY -> chr.write(WvsContext.givePopularityResult(PopularityResultType.AlreadyDoneToday, target, 0, increase));
            case DONE_TARGET -> chr.write(WvsContext.givePopularityResult(PopularityResultType.AlreadyDoneTarget, target, 0, increase));
            case FAILED -> chr.chatMessage("目前無法提升或降低人氣，請稍後再試。");
            case SELF -> DataPrinter.send(DataPrinter.AUTOBAN_WARNING,
                    String.format("Character %d tried to fame themselves", chr.getId()));
        }
        chr.dispose();
        return v;
    }

    /** The limits, without recording anything. */
    public Verdict check(Char chr, Char target) {
        if (target == null) {
            return Verdict.NO_TARGET;
        }
        if (target.getId() == chr.getId()) {
            return Verdict.SELF;
        }
        if (chr.getLevel() < GameConstants.MIN_LEVEL_TO_FAME || target.getLevel() < GameConstants.MIN_LEVEL_TO_FAME) {
            return Verdict.LEVEL_LOW;
        }
        if (chr.getUser() != null && chr.getUser().getAccountType() != AccountType.Player) {
            return Verdict.OK;
        }
        try {
            LocalDateTime now = LocalDateTime.now();
            if (countSince(chr.getId(), 0, now.minusHours(GameConstants.FAME_COOLDOWN)) >= DAILY_LIMIT) {
                return Verdict.DONE_TODAY;
            }
            if (countSince(chr.getId(), target.getId(), now.minusDays(TARGET_COOLDOWN_DAYS)) > 0) {
                return Verdict.DONE_TARGET;
            }
            return Verdict.OK;
        } catch (SQLException e) {
            SocialTables.log("fame history of " + chr.getId(), e);
            return Verdict.FAILED;
        }
    }

    /** Time left until {@code chr} may fame again (zero when allowed now); for messages / commands. */
    public Duration nextFameIn(Char chr) {
        try (Connection c = SocialTables.connection(); PreparedStatement p = c.prepareStatement(
                "SELECT created_at FROM " + SocialTables.POP + " WHERE character_id=? ORDER BY created_at DESC LIMIT 1 OFFSET ?")) {
            p.setInt(1, chr.getId());
            p.setInt(2, DAILY_LIMIT - 1);
            try (ResultSet r = p.executeQuery()) {
                if (!r.next()) {
                    return Duration.ZERO;
                }
                LocalDateTime next = r.getTimestamp(1).toLocalDateTime().plusHours(GameConstants.FAME_COOLDOWN);
                Duration left = Duration.between(LocalDateTime.now(), next);
                return left.isNegative() ? Duration.ZERO : left;
            }
        } catch (SQLException e) {
            SocialTables.log("fame history of " + chr.getId(), e);
            return Duration.ZERO;
        }
    }

    /** Rows of {@code characterId} since {@code since}; {@code targetId} 0 = any target. */
    static int countSince(int characterId, int targetId, LocalDateTime since) throws SQLException {
        String sql = "SELECT COUNT(*) FROM " + SocialTables.POP + " WHERE character_id=? AND created_at>=?"
                + (targetId != 0 ? " AND target_id=?" : "");
        try (Connection c = SocialTables.connection(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setInt(1, characterId);
            p.setTimestamp(2, Timestamp.valueOf(since));
            if (targetId != 0) {
                p.setInt(3, targetId);
            }
            try (ResultSet r = p.executeQuery()) {
                return r.next() ? r.getInt(1) : 0;
            }
        }
    }

    static boolean record(int characterId, int targetId, boolean increase) {
        try (Connection c = SocialTables.connection(); PreparedStatement p = c.prepareStatement(
                "INSERT INTO " + SocialTables.POP + " (created_at, character_id, target_id, increase) VALUES (?,?,?,?)")) {
            p.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            p.setInt(2, characterId);
            p.setInt(3, targetId);
            p.setInt(4, increase ? 1 : 0);
            return p.executeUpdate() == 1;
        } catch (SQLException e) {
            SocialTables.log("record fame " + characterId + " -> " + targetId, e);
            return false;
        }
    }
}
