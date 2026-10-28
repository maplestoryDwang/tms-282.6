package net.swordie.ms.connection.api.launcher.account;

import net.swordie.ms.connection.api.launcher.LauncherCodes;
import net.swordie.ms.connection.api.launcher.LauncherResult;
import net.swordie.ms.connection.api.launcher.daily.DailyStatusAction;
import net.swordie.ms.connection.api.launcher.daily.DailyStatusReader;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;

/**
 * The standard {@link DashboardSection}s: me and characters (required), worlds, then (P6) dailyStatus and
 * newsVersion.
 */
public final class DashboardSections {

    private DashboardSections() {
    }

    /** me, characters + lastPlayed, worlds, dailyStatus, newsVersion - with the connected database's character schema. */
    public static List<DashboardSection> standard() {
        return standard(new CharacterReader());
    }

    public static List<DashboardSection> standard(CharacterReader characters) {
        return standard(characters, new DailyStatusReader());
    }

    public static List<DashboardSection> standard(CharacterReader characters, DailyStatusReader daily) {
        return List.of(new Me(new AccountReader()), new Characters(characters), new Worlds(), new DailyStatus(daily),
                new NewsVersion(), new OnlineReward());
    }

    /** {@code me}: the {@link AccountView} of the session's user; the row being gone answers sessionExpired. */
    public static final class Me implements DashboardSection {
        private final AccountReader reader;

        public Me(AccountReader reader) {
            this.reader = reader;
        }

        @Override
        public String name() {
            return "me";
        }

        @Override
        public boolean required() {
            return true;
        }

        @Override
        public List<String> fields() {
            return List.of("me");
        }

        @Override
        public LauncherResult contribute(DashboardContext dc, LauncherResult out) throws Exception {
            Optional<AccountView> me = reader.read(dc.connection(), dc.userId(), dc.launcher().services());
            if (me.isEmpty()) {
                return LauncherResult.fail(LauncherCodes.SESSION_EXPIRED);
            }
            out.with("me", me.get().toMap());
            return null;
        }
    }

    /** {@code characters} and {@code lastPlayed} ({@link CharacterList}); later sections read the list. */
    public static final class Characters implements DashboardSection {
        private final CharacterReader reader;

        public Characters(CharacterReader reader) {
            this.reader = reader;
        }

        @Override
        public String name() {
            return "characters";
        }

        @Override
        public boolean required() {
            return true;
        }

        @Override
        public List<String> fields() {
            return List.of("characters", "lastPlayed");
        }

        @Override
        public LauncherResult contribute(DashboardContext dc, LauncherResult out) throws Exception {
            CharacterList list = reader.read(dc.connection(), dc.userId(), dc.launcher().services().memory());
            dc.characters(list);
            out.with("characters", list.characterMaps()).with("lastPlayed", list.lastPlayedMap());
            return null;
        }
    }

    /** {@code worlds}: {@link WorldDirectory#describe}, plus the worlds of the caller's characters. */
    public static final class Worlds implements DashboardSection {

        @Override
        public String name() {
            return "worlds";
        }

        @Override
        public boolean required() {
            return false;
        }

        @Override
        public List<String> fields() {
            return List.of("worlds");
        }

        @Override
        public LauncherResult contribute(DashboardContext dc, LauncherResult out) {
            CharacterList characters = dc.characters();
            out.with("worlds", WorldDirectory.current().describe(characters == null ? Set.of() : characters.worlds()));
            return null;
        }
    }

    /**
     * {@code dailyStatus} (P6): {@link DailyStatusReader} for the request's {@code world} when the login server lists
     * it, otherwise the world of {@code lastPlayed}, otherwise of the first character in a listed world; null when the
     * caller has no character in a listed world (the launcher shows "create a character to sign in").
     */
    public static final class DailyStatus implements DashboardSection {
        private final DailyStatusReader reader;

        public DailyStatus(DailyStatusReader reader) {
            this.reader = reader;
        }

        @Override
        public String name() {
            return "dailyStatus";
        }

        @Override
        public boolean required() {
            return false;
        }

        @Override
        public List<String> fields() {
            return List.of("dailyStatus");
        }

        @Override
        public LauncherResult contribute(DashboardContext dc, LauncherResult out) throws Exception {
            OptionalInt world = world(dc, WorldDirectory.current());
            out.with("dailyStatus", world.isEmpty() ? null
                    : reader.read(dc.connection(), dc.userId(), world.getAsInt(), dc.launcher().services()).toMap());
            return null;
        }

        static OptionalInt world(DashboardContext dc, WorldDirectory worlds) {
            OptionalInt asked = DailyStatusAction.world(dc.request());
            if (asked.isPresent() && worlds.advertised(asked.getAsInt())) {
                return asked;
            }
            CharacterList list = dc.characters();
            if (list == null) {
                return OptionalInt.empty();
            }
            if (list.lastPlayed() != null && worlds.advertised(list.lastPlayed().world())) {
                return OptionalInt.of(list.lastPlayed().world());
            }
            for (CharacterView c : list.characters()) {
                if (worlds.advertised(c.world())) {
                    return OptionalInt.of(c.world());
                }
            }
            return OptionalInt.empty();
        }
    }

    /** {@code newsVersion} (P6): the version of what {@code news} answers now (the launcher reloads its tab on change). */
    public static final class NewsVersion implements DashboardSection {

        @Override
        public String name() {
            return "newsVersion";
        }

        @Override
        public boolean required() {
            return false;
        }

        @Override
        public List<String> fields() {
            return List.of("newsVersion");
        }

        @Override
        public LauncherResult contribute(DashboardContext dc, LauncherResult out) {
            out.with("newsVersion", dc.launcher().services().news().snapshot().version());
            return null;
        }
    }

    /**
     * {@code onlineReward} (leftovers-1005): today's accumulated online minutes and the 樂豆點 granted for them
     * ({@code online_reward_day}, read-only), with the rules from {@code data/tms282-online-reward/online-reward.json}.
     */
    public static final class OnlineReward implements DashboardSection {

        @Override
        public String name() {
            return "onlineReward";
        }

        @Override
        public boolean required() {
            return false;
        }

        @Override
        public List<String> fields() {
            return List.of("onlineReward");
        }

        @Override
        public LauncherResult contribute(DashboardContext dc, LauncherResult out) throws Exception {
            var config = net.swordie.ms.world.online.OnlineRewardSettings.standard().get();
            var day = java.time.LocalDate.now(net.swordie.ms.ServerConfig.ZONE);
            var p = net.swordie.ms.world.online.JdbcOnlineRewardLedger.read(dc.connection(), dc.userId(), day);
            java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
            m.put("enabled", config.enabled());
            m.put("day", day.toString());
            m.put("minutes", p.minutes());
            m.put("points", p.points());
            m.put("dailyCapPoints", config.dailyCapPoints());
            m.put("intervalMinutes", config.intervalMinutes());
            m.put("pointsPerInterval", config.pointsPerInterval());
            m.put("afkMinutes", config.afkMinutes());
            out.with("onlineReward", m);
            return null;
        }
    }
}
