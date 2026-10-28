package net.swordie.ms.connection.api.admin;

import com.fasterxml.jackson.databind.JsonNode;
import net.swordie.ms.connection.api.launcher.LauncherResult;
import net.swordie.ms.connection.api.launcher.gift.GiftCodeKey;
import net.swordie.ms.connection.api.launcher.gift.GiftCodeService;
import net.swordie.ms.connection.api.launcher.gift.GiftSchema;
import net.swordie.ms.connection.hikariCP.DatabaseManager;
import net.swordie.ms.console.datamanage.ConsoleCharacterService;
import net.swordie.ms.console.datamanage.GiveItemDialog;
import net.swordie.ms.constants.ItemConstants;
import net.swordie.ms.enums.InvType;
import net.swordie.ms.world.tms282.items.Tms282ItemAvailability;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 禮包序號 (P7) admin actions: {@code gift.create}, {@code gift.list}, {@code gift.disable}, {@code gift.redemptions},
 * {@code gift.queue}, {@code gift.itemSearch}. The codes of gift.create are in its answer only (shown once by the
 * console); the database keeps their HMAC, the audit line keeps the parameters, never a code.
 */
final class GiftAdmin {
    private GiftAdmin() {
    }

    static Connection connection() throws Exception {
        GiftSchema.ensure();
        return DatabaseManager.getConnection();
    }

    static String type(int itemId) {
        InvType t = ItemConstants.getInvTypeByItemID(itemId);
        if (t == null) return "?";
        return switch (t) {
            case EQUIP, EQUIPPED -> "裝備";
            case CONSUME -> "消耗";
            case INSTALL -> "裝飾";
            case ETC -> "其他";
            case CASH -> "現金";
            case DECORATION -> "時裝";
            default -> t.name();
        };
    }

    static LauncherResult problem(String p) {
        return LauncherResult.fail(AdminCodes.REJECTED).with("problem", "gift." + p);
    }

    /** One line of free text for the audit log. */
    static String oneLine(String s) {
        return s.replace('\r', ' ').replace('\n', ' ');
    }

    // ------------------------------------------------------------------ gift.create

    static final class Create implements AdminAction {
        @Override
        public String name() {
            return "gift.create";
        }

        @Override
        public boolean mutating() {
            return true;
        }

        @Override
        public String auditSummary(JsonNode r) {
            StringBuilder items = new StringBuilder();
            for (JsonNode i : r.path("items")) {
                if (items.length() > 0) items.append(',');
                items.append(i.path("itemId").asLong()).append('x').append(i.path("quantity").asLong());
            }
            return "title=" + oneLine(AdminParams.text(r, "title")) + " items=" + items + " count="
                    + AdminParams.number(r, "count", 1, 200) + " maxUses=" + AdminParams.number(r, "maxUses", 1, 100_000)
                    + " perUser=" + AdminParams.number(r, "perUserLimit", 1, 100) + " world=" + AdminParams.number(r, "worldId", -1, 255)
                    + " minLevel=" + AdminParams.number(r, "minLevel", 0, 300);
        }

        @Override
        public LauncherResult execute(AdminContext ctx, JsonNode r) throws Exception {
            List<GiftCodeService.Item> items = new ArrayList<>();
            JsonNode list = r.path("items");
            if (!list.isArray()) return problem("items");
            for (JsonNode i : list) {
                Long id = AdminParams.number(i, "itemId", 1, Integer.MAX_VALUE), q = AdminParams.number(i, "quantity", 1, 1000);
                if (id == null) return problem("itemId");
                if (q == null) return problem("quantity");
                items.add(new GiftCodeService.Item(id.intValue(), q.intValue()));
            }
            Long maxUses = AdminParams.number(r, "maxUses", 1, 100_000), perUser = AdminParams.number(r, "perUserLimit", 1, 100),
                    world = AdminParams.has(r, "worldId") ? AdminParams.number(r, "worldId", -1, 255) : Long.valueOf(-1),
                    minLevel = AdminParams.has(r, "minLevel") ? AdminParams.number(r, "minLevel", 0, 300) : Long.valueOf(0),
                    count = AdminParams.has(r, "count") ? AdminParams.number(r, "count", 1, 200) : Long.valueOf(1);
            Long starts = AdminParams.has(r, "startsAt") ? AdminParams.number(r, "startsAt", 1, 253_402_300_799_000L) : null,
                    expires = AdminParams.has(r, "expiresAt") ? AdminParams.number(r, "expiresAt", 1, 253_402_300_799_000L) : null;
            if (maxUses == null) return problem("maxUses");
            if (perUser == null) return problem("perUserLimit");
            if (world == null) return problem("worldId");
            if (minLevel == null) return problem("minLevel");
            if (count == null) return problem("count");
            if (AdminParams.has(r, "startsAt") && starts == null) return problem("startsAt");
            if (AdminParams.has(r, "expiresAt") && expires == null) return problem("expiresAt");
            GiftCodeService.Spec spec = new GiftCodeService.Spec(AdminParams.text(r, "title"), AdminParams.text(r, "note"), items,
                    maxUses.intValue(), perUser.intValue(), world.intValue(), minLevel.intValue(), starts, expires, count.intValue());
            String p = GiftCodeService.validate(spec, System.currentTimeMillis());
            if (p != null) return problem(p);
            GiftCodeService.Created created = GiftCodeService.standard().create(spec, GiftCodeKey.standard());
            return LauncherResult.ok(AdminCodes.OK).with("ids", created.ids()).with("codes", created.codes()).with("batchId", created.batchId());
        }
    }

    // ------------------------------------------------------------------ gift.list / disable / redemptions / queue

    static final class ListCodes implements AdminAction {
        @Override
        public String name() {
            return "gift.list";
        }

        @Override
        public boolean mutating() {
            return false;
        }

        @Override
        public LauncherResult execute(AdminContext ctx, JsonNode r) throws Exception {
            boolean all = r == null || r.path("includeDisabled").asBoolean(true);
            GiftCodeKey key = GiftCodeKey.standard();
            try (Connection c = connection()) {
                return LauncherResult.ok(AdminCodes.OK).with("keyId", key.keyId()).with("codes", GiftCodeService.standard().list(c, key, 300, all));
            }
        }
    }

    static final class Disable implements AdminAction {
        @Override
        public String name() {
            return "gift.disable";
        }

        @Override
        public boolean mutating() {
            return true;
        }

        @Override
        public String auditSummary(JsonNode r) {
            return "id=" + AdminParams.number(r, "id", 1, Integer.MAX_VALUE);
        }

        @Override
        public LauncherResult execute(AdminContext ctx, JsonNode r) throws Exception {
            Long id = AdminParams.number(r, "id", 1, Integer.MAX_VALUE);
            if (id == null) return LauncherResult.fail(AdminCodes.INVALID_INPUT).with("problem", "id");
            try (Connection c = connection()) {
                if (GiftCodeService.codeExists(c, id.intValue()).isEmpty()) return LauncherResult.fail(AdminCodes.NOT_FOUND);
                boolean changed = GiftCodeService.standard().disable(c, id.intValue());
                return LauncherResult.ok(AdminCodes.OK).with("id", id).with("changed", changed);
            }
        }
    }

    static final class Redemptions implements AdminAction {
        @Override
        public String name() {
            return "gift.redemptions";
        }

        @Override
        public boolean mutating() {
            return false;
        }

        @Override
        public LauncherResult execute(AdminContext ctx, JsonNode r) throws Exception {
            Long id = AdminParams.has(r, "codeId") ? AdminParams.number(r, "codeId", 1, Integer.MAX_VALUE) : null;
            if (AdminParams.has(r, "codeId") && id == null) return LauncherResult.fail(AdminCodes.INVALID_INPUT).with("problem", "codeId");
            try (Connection c = connection()) {
                return LauncherResult.ok(AdminCodes.OK).with("redemptions",
                        GiftCodeService.standard().redemptions(c, id == null ? null : id.intValue(), 300));
            }
        }
    }

    static final class Queue implements AdminAction {
        @Override
        public String name() {
            return "gift.queue";
        }

        @Override
        public boolean mutating() {
            return false;
        }

        @Override
        public LauncherResult execute(AdminContext ctx, JsonNode r) throws Exception {
            boolean delivered = r != null && r.path("includeDelivered").asBoolean(false);
            try (Connection c = connection()) {
                return LauncherResult.ok(AdminCodes.OK).with("queue", GiftCodeService.standard().queue(c, delivered, 300));
            }
        }
    }

    // ------------------------------------------------------------------ gift.itemSearch

    static final class ItemSearch implements AdminAction {
        @Override
        public String name() {
            return "gift.itemSearch";
        }

        @Override
        public boolean mutating() {
            return false;
        }

        @Override
        public LauncherResult execute(AdminContext ctx, JsonNode r) throws Exception {
            String q = AdminParams.text(r, "query").strip();
            if (q.isEmpty() || q.length() > 40) return LauncherResult.fail(AdminCodes.INVALID_INPUT).with("problem", "query");
            List<Map<String, Object>> out = new ArrayList<>();
            for (GiveItemDialog.ItemRow row : ConsoleCharacterService.searchItems(q, 60)) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("itemId", row.itemId);
                m.put("name", row.name);
                m.put("type", type(row.itemId));
                m.put("grantable", Tms282ItemAvailability.isGrantable(row.itemId));
                m.put("unique", ItemConstants.isEquip(row.itemId) || ItemConstants.isPet(row.itemId));
                out.add(m);
            }
            return LauncherResult.ok(AdminCodes.OK).with("items", out);
        }
    }
}
