package net.swordie.ms.connection.api.launcher.daily;

import com.fasterxml.jackson.databind.JsonNode;
import net.swordie.ms.connection.api.launcher.LauncherCodes;
import net.swordie.ms.connection.api.launcher.LauncherContext;
import net.swordie.ms.connection.api.launcher.LauncherResult;
import net.swordie.ms.connection.api.launcher.SessionReadAction;
import net.swordie.ms.connection.api.launcher.account.WorldDirectory;

import java.sql.Connection;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.regex.Pattern;

/**
 * {@code dailyStatus} (session, {@code world}): the caller's daily gift state in one advertised world
 * ({@link DailyStatusView}) - claims this month, claimed today, the next reward, this month's dated claims. Read-only:
 * the day / month rollover is applied to a copy only. A world the login server does not list answers
 * {@code invalidInput}; the user is the session's (a userId in the request is ignored).
 * <p>
 * fixes10 (/api/ping feature {@code dailyStatus.month}): optional {@code "month":"YYYY-MM"} = whose dated claims
 * {@code claimDates} lists (and {@code month} echoes); the current month or up to {@link DailyStatusView#PAST_MONTHS}
 * before it. Absent or null = the current month (as before). Malformed -> {@code invalidInput} {@code reason:"month"};
 * a future month or one older than the range -> {@code invalidInput} {@code reason:"monthRange"} with
 * {@code minMonth} / {@code maxMonth}.
 */
public final class DailyStatusAction extends SessionReadAction {
    private static final Pattern DIGITS = Pattern.compile("[0-9]{1,4}");
    private static final Pattern MONTH = Pattern.compile("[0-9]{4}-(0[1-9]|1[0-2])");
    private final DailyStatusReader reader;

    public DailyStatusAction() {
        this(new DailyStatusReader());
    }

    /** With another reader (checks: a fixed clock). */
    public DailyStatusAction(DailyStatusReader reader) {
        this.reader = Objects.requireNonNull(reader, "reader");
    }

    @Override
    public String name() {
        return "dailyStatus";
    }

    @Override
    protected LauncherResult read(LauncherContext ctx, JsonNode request, Connection con, int userId) throws Exception {
        OptionalInt world = world(request);
        if (world.isEmpty() || !WorldDirectory.current().advertised(world.getAsInt())) {
            return LauncherResult.fail(LauncherCodes.INVALID_INPUT);
        }
        YearMonth month = null;
        JsonNode m = request == null ? null : request.get("month");
        if (m != null && !m.isNull()) {
            month = month(m);
            if (month == null) {
                return LauncherResult.fail(LauncherCodes.INVALID_INPUT).with("reason", "month");
            }
            YearMonth current = reader.currentMonth();
            if (!DailyStatusView.inRange(month, current)) {
                return LauncherResult.fail(LauncherCodes.INVALID_INPUT).with("reason", "monthRange")
                        .with("minMonth", current.minusMonths(DailyStatusView.PAST_MONTHS).toString()).with("maxMonth", current.toString());
            }
        }
        DailyStatusView view = reader.read(con, userId, world.getAsInt(), ctx.services(), month);
        LauncherResult result = LauncherResult.ok(LauncherCodes.OK);
        view.toMap().forEach(result::with);
        return result;
    }

    /** {@code "YYYY-MM"} (four-digit year, month 01-12); null when malformed. */
    public static YearMonth month(JsonNode m) {
        if (m == null || !m.isTextual() || !MONTH.matcher(m.textValue()).matches()) {
            return null;
        }
        try {
            return YearMonth.parse(m.textValue());
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /** {@code world} as a JSON integer (or a string of digits); empty when absent or malformed. */
    public static OptionalInt world(JsonNode request) {
        JsonNode w = request == null ? null : request.get("world");
        if (w == null) {
            return OptionalInt.empty();
        }
        if (w.isInt() && w.intValue() >= 0) {
            return OptionalInt.of(w.intValue());
        }
        if (w.isTextual() && DIGITS.matcher(w.textValue()).matches()) {
            return OptionalInt.of(Integer.parseInt(w.textValue()));
        }
        return OptionalInt.empty();
    }
}
