package net.swordie.ms.connection.api.launcher;

import net.swordie.ms.Server;
import net.swordie.ms.client.Account;
import net.swordie.ms.client.User;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.daily.DailyGift;
import net.swordie.ms.world.Channel;
import net.swordie.ms.world.World;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * What the read-only launcher views take from the running game server's memory: online counts, channel loads,
 * uptime, and the live values of an online character / user / daily gift record (the database lags behind them until
 * the next save).
 * <p>
 * {@link #live()} is the only launcher code that reads {@link Server}; it only calls methods the formal jar already
 * has. The CLI and tests use {@link #none()} (or a fake): {@code Server}'s static initialiser builds the whole server,
 * so a CLI JVM must never load it.
 */
public interface ServerMemory {

    /** One channel of the running world. */
    record ChannelLoad(int id, String name, int online) {
    }

    /** An online character as the server holds it. */
    record CharacterLive(int level, int job, int subJob) {
    }

    /** An online user (account) as the server holds it. */
    record UserLive(int vipGrade, int characterSlots) {
    }

    /** A world account's daily gift record as the game holds it (dailygift: id, date, dateComplete, isClaim). */
    record DailyGiftLive(long id, long date, int dateComplete, boolean claimed) {
    }

    /** True in the game server process: the values below are live. False: no server memory (CLI). */
    boolean available();

    /** Users held by some client (login server, channels, cash shop). */
    int onlineUsers();

    /** Channels of the running world in order; empty while the world is not up. */
    List<ChannelLoad> channels();

    /** Seconds since the server started; 0 while unknown. */
    long uptimeSec();

    Optional<CharacterLive> onlineCharacter(int characterId);

    Optional<UserLive> onlineUser(int userId);

    /**
     * The daily gift record of {@code userId}'s world-{@code worldId} account while the server holds that user - the
     * copy the game claims on, read under the same lock as {@code DailyGift.claim}; empty when the user is not held or
     * the account has no record yet.
     */
    Optional<DailyGiftLive> dailyGift(int userId, int worldId);

    /** HttpAuthApi: the running server. */
    static ServerMemory live() {
        return Live.INSTANCE;
    }

    /** CLI: nothing is online as far as this process can tell. */
    static ServerMemory none() {
        return None.INSTANCE;
    }

    enum Live implements ServerMemory {
        INSTANCE;

        @Override
        public boolean available() {
            return true;
        }

        @Override
        public int onlineUsers() {
            return Server.get().getUsers().size();
        }

        @Override
        public List<ChannelLoad> channels() {
            World world = Server.get().getWorld();
            List<ChannelLoad> out = new ArrayList<>();
            if (world == null || world.getChannels() == null) {
                return out;
            }
            for (Channel channel : new ArrayList<>(world.getChannels())) {
                if (channel != null) {
                    var chars = channel.getChars();
                    out.add(new ChannelLoad(channel.getChannelId(), channel.getName(), chars == null ? 0 : chars.size()));
                }
            }
            return out;
        }

        @Override
        public long uptimeSec() {
            LocalDateTime start = Server.startTime;
            if (start == null) {
                return 0;
            }
            return Math.max(0, Duration.between(start, LocalDateTime.now()).toSeconds());
        }

        @Override
        public Optional<CharacterLive> onlineCharacter(int characterId) {
            Char chr = Server.get().getChars().get(characterId);
            if (chr == null) {
                return Optional.empty();
            }
            return Optional.of(new CharacterLive(chr.getLevel(), chr.getJob(), chr.getSubJob()));
        }

        @Override
        public Optional<UserLive> onlineUser(int userId) {
            User user = Server.get().getUserById(userId);
            return user == null ? Optional.empty() : Optional.of(new UserLive(user.getVipGrade(), user.getCharacterSlots()));
        }

        @Override
        public Optional<DailyGiftLive> dailyGift(int userId, int worldId) {
            User user = Server.get().getUserById(userId);
            if (user == null || user.getAccounts() == null) {
                return Optional.empty();
            }
            for (Account account : new ArrayList<>(user.getAccounts())) {
                if (account == null || account.getWorldId() != worldId) {
                    continue;
                }
                synchronized (account) {
                    // DailyGift.claim holds this lock from the rollover to the save: never a half-applied claim
                    DailyGift g = account.getDailyGift();
                    return g == null ? Optional.empty()
                            : Optional.of(new DailyGiftLive(g.getId(), g.getDate(), g.getDateComplete(), g.isClaim()));
                }
            }
            return Optional.empty();
        }
    }

    enum None implements ServerMemory {
        INSTANCE;

        @Override
        public boolean available() {
            return false;
        }

        @Override
        public int onlineUsers() {
            return 0;
        }

        @Override
        public List<ChannelLoad> channels() {
            return List.of();
        }

        @Override
        public long uptimeSec() {
            return 0;
        }

        @Override
        public Optional<CharacterLive> onlineCharacter(int characterId) {
            return Optional.empty();
        }

        @Override
        public Optional<UserLive> onlineUser(int userId) {
            return Optional.empty();
        }

        @Override
        public Optional<DailyGiftLive> dailyGift(int userId, int worldId) {
            return Optional.empty();
        }
    }
}
