package net.swordie.ms.connection.api.admin;

import com.fasterxml.jackson.databind.JsonNode;
import net.swordie.ms.connection.api.launcher.LauncherResult;
import net.swordie.ms.connection.api.launcher.LauncherServices;
import net.swordie.ms.connection.api.launcher.LoginBackoff;
import net.swordie.ms.connection.api.launcher.account.CharacterList;
import net.swordie.ms.connection.api.launcher.account.CharacterReader;
import net.swordie.ms.connection.hikariCP.DatabaseManager;
import net.swordie.ms.handlers.login.tms282.Tms282LoginThrottle;
import net.swordie.ms.world.tms282.audit.AccountBlock;
import net.swordie.ms.world.tms282.audit.BanService;
import net.swordie.ms.world.tms282.audit.BanType;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * {@code account.find} {query} (帳號): one account by id, name or character name - its characters (with the live level /
 * job of online ones), online state, last played character, launcher sessions, the failed-password locks of the
 * launcher (8600) and of the game login (CP0074) per source IP, and its bans (account_block through BanService's
 * cache, plus the legacy users.banExpireDate). SELECT only.
 */
final class AccountFindAction implements AdminAction {
    private final CharacterReader characters = new CharacterReader();

    @Override
    public String name() {
        return "account.find";
    }

    @Override
    public boolean mutating() {
        return false;
    }

    @Override
    public LauncherResult execute(AdminContext ctx, JsonNode request) throws Exception {
        String query = AccountLookup.clean(AdminParams.text(request, "query"));
        if (query == null) {
            return LauncherResult.fail(AdminCodes.INVALID_INPUT).with("problem", "query");
        }
        LauncherServices s = ctx.env().services();
        AccountLookup.UserRow user;
        CharacterList list;
        try (Connection con = DatabaseManager.getConnection()) {
            Optional<AccountLookup.UserRow> found = AccountLookup.resolve(con, query);
            if (found.isEmpty()) {
                return LauncherResult.fail(AdminCodes.NOT_FOUND);
            }
            user = found.get();
            list = characters.read(con, user.id(), s.memory());
        }
        boolean online = s.memory().onlineUser(user.id()).isPresent() || s.onlineAccounts().online(user.id());
        long now = ctx.env().now();
        List<Map<String, Object>> locks = new ArrayList<>();
        addLocks(locks, "launcher", s.loginBackoff().pairsOf(user.name()));
        addLocks(locks, "game", Tms282LoginThrottle.standard().pairsOf(user.name()));
        return LauncherResult.ok(AdminCodes.OK)
                .with("account", user.toMap())
                .with("online", online)
                .with("sessions", s.sessions().countFor(user.id()))
                .with("characters", list.characterMaps())
                .with("lastPlayed", list.lastPlayedMap())
                .with("locks", locks)
                .with("bans", bans(user.id(), now))
                .with("legacyBanActive", user.banExpireAt() != null && user.banExpireAt() > now)
                .with("banTypes", banTypes());
    }

    private static void addLocks(List<Map<String, Object>> out, String kind, List<LoginBackoff.Pair> pairs) {
        for (LoginBackoff.Pair p : pairs) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("kind", kind);
            m.put("ip", p.sourceIp());
            m.put("failures", p.failures());
            m.put("lockedForSec", (p.lockedForMs() + 999) / 1000);
            m.put("lastFailure", p.lastFailure());
            out.add(m);
        }
    }

    static List<Map<String, Object>> bans(int userId, long now) {
        BanService.activeBan(userId); // loads the cache on the first lookup
        List<Map<String, Object>> out = new ArrayList<>();
        for (AccountBlock b : BanService.blocksOf(userId)) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", b.id());
            m.put("type", b.type());
            m.put("typeName", b.banType().name());
            m.put("typeLabel", b.banType().getLabel());
            m.put("blocksLogin", b.blocksLogin());
            m.put("createdAt", b.createdAt());
            m.put("endTime", b.isPermanent() ? null : b.endTime());
            m.put("permanent", b.isPermanent());
            m.put("active", b.isActive(now));
            m.put("reason", b.reason());
            out.add(m);
        }
        return out;
    }

    static List<Map<String, Object>> banTypes() {
        List<Map<String, Object>> out = new ArrayList<>();
        for (BanType t : BanType.values()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("val", t.getVal());
            m.put("name", t.name());
            m.put("label", t.getLabel());
            m.put("blocksLogin", t.blocksLogin());
            out.add(m);
        }
        return out;
    }
}
