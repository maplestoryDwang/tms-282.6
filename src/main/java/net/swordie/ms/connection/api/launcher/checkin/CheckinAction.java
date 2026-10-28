package net.swordie.ms.connection.api.launcher.checkin;

import com.fasterxml.jackson.databind.JsonNode;
import net.swordie.ms.client.daily.DailyGiftLauncherClaim;
import net.swordie.ms.client.daily.DailyStreakBonus;
import net.swordie.ms.connection.api.launcher.LauncherAction;
import net.swordie.ms.connection.api.launcher.LauncherCodes;
import net.swordie.ms.connection.api.launcher.LauncherContext;
import net.swordie.ms.connection.api.launcher.LauncherRateLimits;
import net.swordie.ms.connection.api.launcher.LauncherResult;
import net.swordie.ms.connection.api.launcher.LauncherSession;
import net.swordie.ms.connection.api.launcher.LauncherTransport;
import net.swordie.ms.connection.api.launcher.LocalDatabaseGuard;
import net.swordie.ms.connection.api.launcher.account.CharacterList;
import net.swordie.ms.connection.api.launcher.account.CharacterReader;
import net.swordie.ms.connection.api.launcher.account.CharacterView;
import net.swordie.ms.connection.api.launcher.daily.DailyRewardTable;
import net.swordie.ms.connection.hikariCP.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * {@code checkin} (session) {characterId}: today's daily gift (the game's 28-day sign-in) claimed from the launcher for
 * the account of that character's world, sent to that character ({@link DailyGiftLauncherClaim}); the streak bonus
 * reached by this claim comes with it ({@link DailyStreakBonus}). Items arrive at the character's next field entry.
 * Answers {@code signed} (day, items, bonuses, character, delivery "nextLogin"), {@code alreadySigned} (claimed today,
 * in the game or here), {@code monthComplete}, {@code accountOnline} (claim in the game while online: one record),
 * {@code noReceiver}, {@code unavailable} or {@code invalidInput}. SERVER_WHEN_RUNNING; bucket GIFT.
 */
public final class CheckinAction implements LauncherAction {
    public static final String SIGNED = "signed";
    public static final String ALREADY_SIGNED = "alreadySigned";
    public static final String MONTH_COMPLETE = "monthComplete";
    public static final String ACCOUNT_ONLINE = "accountOnline";

    @Override
    public String name() {
        return "checkin";
    }

    @Override
    public boolean requiresSession() {
        return true;
    }

    @Override
    public boolean mutating() {
        return true;
    }

    @Override
    public String rateBucket() {
        return LauncherRateLimits.GIFT;
    }

    @Override
    public boolean remoteAllowed() {
        return true;
    }

    @Override
    public LauncherTransport transport() {
        return LauncherTransport.SERVER_WHEN_RUNNING;
    }

    /** The user's account in {@code world}, or null. */
    static Integer accountId(Connection con, int userId, int world) throws java.sql.SQLException {
        try (PreparedStatement ps = con.prepareStatement("SELECT id FROM accounts WHERE userid = ? AND worldid = ?")) {
            ps.setInt(1, userId);
            ps.setInt(2, world);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : null;
            }
        }
    }

    @Override
    public LauncherResult execute(LauncherContext ctx, JsonNode request) throws Exception {
        LauncherSession session = ctx.session();
        if (session == null) {
            return LauncherResult.fail(LauncherCodes.NEED_SESSION);
        }
        JsonNode chr = request == null ? null : request.get("characterId");
        if (chr == null || !chr.isIntegralNumber() || !chr.canConvertToInt() || chr.intValue() <= 0) {
            return LauncherResult.fail(LauncherCodes.INVALID_INPUT).with("problem", "characterId");
        }
        if (!LocalDatabaseGuard.accepted()) {
            return LauncherResult.fail(LauncherCodes.LOCAL_ONLY);
        }
        int userId = session.userId();
        if (ctx.services().onlineAccounts().online(userId)) {
            return LauncherResult.fail(ACCOUNT_ONLINE);
        }
        CharacterView receiver;
        Integer accountId;
        try (Connection c = DatabaseManager.getConnection()) {
            CharacterList list = new CharacterReader().read(c, userId, ctx.services().memory());
            receiver = list.characters().stream().filter(v -> v.id() == chr.intValue() && !v.deleting()).findFirst().orElse(null);
            accountId = receiver == null ? null : accountId(c, userId, receiver.world());
        }
        if (receiver == null || accountId == null) {
            return LauncherResult.fail(LauncherCodes.NO_RECEIVER);
        }
        DailyGiftLauncherClaim.Result r = DailyGiftLauncherClaim.claim(userId, accountId, receiver.id());
        return switch (r.status()) {
            case CLAIMED -> {
                List<Map<String, Object>> items = new ArrayList<>();
                items.add(item(r.itemId(), r.quantity(), rewardName(r.dayIndex(), r.itemId()), r.periodDays()));
                List<Integer> bonusDays = new ArrayList<>();
                for (DailyStreakBonus.Granted g : r.bonuses()) {
                    bonusDays.add(g.days());
                    for (DailyStreakBonus.Item i : g.items()) {
                        items.add(item(i.itemId(), i.quantity(), i.name(), 0));
                    }
                }
                yield LauncherResult.ok(SIGNED).with("day", r.dayIndex()).with("items", items).with("bonuses", bonusDays)
                        .with("character", Map.of("id", receiver.id(), "name", receiver.name(), "world", receiver.world()))
                        .with("delivery", "nextLogin");
            }
            case ALREADY_CLAIMED -> LauncherResult.fail(ALREADY_SIGNED);
            case MONTH_COMPLETE -> LauncherResult.fail(MONTH_COMPLETE);
            case UNAVAILABLE -> LauncherResult.fail(LauncherCodes.UNAVAILABLE);
        };
    }

    private static Map<String, Object> item(int itemId, int quantity, String name, int periodDays) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("itemId", itemId);
        m.put("quantity", quantity);
        m.put("name", name == null ? "" : name);
        m.put("periodDays", periodDays);
        return m;
    }

    private static String rewardName(int day, int itemId) {
        try {
            var rows = DailyRewardTable.standard().snapshot().rewards();
            if (day >= 1 && day <= rows.size() && rows.get(day - 1).itemId() == itemId) {
                return rows.get(day - 1).name();
            }
        } catch (RuntimeException ignored) {
            // the name is only for the message
        }
        return "";
    }
}
