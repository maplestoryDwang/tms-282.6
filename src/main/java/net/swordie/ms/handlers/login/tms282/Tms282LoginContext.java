package net.swordie.ms.handlers.login.tms282;

import net.swordie.ms.Server;
import net.swordie.ms.ServerConstants;
import net.swordie.ms.connection.packet.Tms282FileReport;
import net.swordie.ms.loaders.Tms282MakeCharInfo;
import net.swordie.ms.world.Channel;
import net.swordie.ms.world.World;
import net.swordie.ms.world.migration.MigrationRegistry;

import java.nio.file.Files;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Services shared by every TMS 282.3 socket (dependency holder for the handler groups): settings, the world
 * catalog, the lazily loaded client file reference and MakeCharInfo, the last world per user (LP000C), the
 * migration registry and the in-field collaborators ({@link Tms282FieldServices}). Lazy loading keeps a missing research file from breaking startup; the first packet that
 * needs it ends its session instead, as the probe's startup would have failed.
 */
public final class Tms282LoginContext {
    private final Tms282LoginSettings settings;
    private final Tms282WorldCatalog worlds;
    private final Supplier<MigrationRegistry> migrations;
    private final Tms282FieldServices fieldServices;
    /** userId -> last world picked since this server started (LP000C preselection; the probe's LAST_WORLD). */
    private final Map<Integer, Integer> lastWorld = new ConcurrentHashMap<>();
    private final Tms282SwitchTickets switchTickets = new Tms282SwitchTickets();
    private final Tms282CharacterPreferences characterPreferences;
    private volatile Tms282FileReport fileReference;
    private volatile Tms282MakeCharInfo makeCharInfo;

    public Tms282LoginContext(Tms282LoginSettings settings, Tms282WorldCatalog worlds, Supplier<MigrationRegistry> migrations) {
        this(settings, worlds, migrations, Tms282FieldServices.standard());
    }

    public Tms282LoginContext(Tms282LoginSettings settings, Tms282WorldCatalog worlds, Supplier<MigrationRegistry> migrations,
                              Tms282FieldServices fieldServices) {
        this(settings, worlds, migrations, fieldServices, new Tms282CharacterPreferences(
                java.nio.file.Path.of("data", "tms282-character-preferences")));
    }

    public Tms282LoginContext(Tms282LoginSettings settings, Tms282WorldCatalog worlds, Supplier<MigrationRegistry> migrations,
                              Tms282FieldServices fieldServices, Tms282CharacterPreferences characterPreferences) {
        this.settings = Objects.requireNonNull(settings, "settings");
        this.worlds = Objects.requireNonNull(worlds, "worlds");
        this.migrations = Objects.requireNonNull(migrations, "migrations");
        this.fieldServices = Objects.requireNonNull(fieldServices, "fieldServices");
        this.characterPreferences = Objects.requireNonNull(characterPreferences, "characterPreferences");
    }

    /**
     * Settings from config, the probe's worlds, the server's migration registry and the standard in-field services
     * (265 database, field chat, admin chat commands @lv/@sp for accounttype >= 3, skill services).
     */
    public static Tms282LoginContext standard(Tms282LoginSettings settings) {
        return new Tms282LoginContext(settings, Tms282WorldCatalog.standard(settings.world0Type()),
                () -> Server.get().getMigrations(), Tms282FieldServices.standard());
    }

    public Tms282LoginSettings settings() {
        return settings;
    }

    public Tms282WorldCatalog worlds() {
        return worlds;
    }

    public MigrationRegistry migrations() {
        return migrations.get();
    }

    public Tms282FieldServices fieldServices() {
        return fieldServices;
    }

    Map<Integer, Integer> lastWorld() {
        return lastWorld;
    }

    Tms282SwitchTickets switchTickets() { return switchTickets; }
    Tms282CharacterPreferences characterPreferences() { return characterPreferences; }

    net.swordie.ms.client.character.Char quickCharacter(net.swordie.ms.client.User user,
                                                       net.swordie.ms.connection.packet.Tms282QuickSelection choice) {
        if (choice == null || !worlds.contains(choice.worldId()) || choice.channelId() >= worlds.channelCount()) return null;
        if (choice.channelId() >= Tms282WorldCatalog.NORMAL_CHANNELS
                && user.getVipGrade() < worlds.minimumVipGrade(choice.channelId())) return null;
        var account = user.getAccountByWorldId(choice.worldId());
        if (account == null || account.getCharacters() == null) return null;
        var chr = account.getCharById(choice.characterId());
        return chr != null && chr.getAccId() == account.getId() && chr.getDeletionStartTime() == 0 ? chr : null;
    }

    net.swordie.ms.connection.packet.Tms282QuickSelection favorite(net.swordie.ms.client.User user) {
        if (user.getId() <= 0) return null;
        try {
            var saved = characterPreferences.read(user.getId()).orElse(null);
            if (saved != null && user.getAccountByWorldId(saved.worldId()) == null) user.loadAccountsData();
            return quickCharacter(user, saved) == null ? null : saved;
        } catch (java.io.IOException e) {
            System.out.println("[TMS282] Cannot read character preference for user " + user.getId() + ": " + e.getMessage());
            return null;
        }
    }

    /** The verified CP0078 report (checked against all 396 client files). */
    Tms282FileReport fileReference() throws Exception {
        Tms282FileReport ref = fileReference;
        if (ref == null) {
            synchronized (this) {
                if (fileReference == null) {
                    fileReference = Tms282FileReport.parse(Files.readAllBytes(settings.fileReference()));
                }
                ref = fileReference;
            }
        }
        return ref;
    }

    Tms282MakeCharInfo makeCharInfo() throws Exception {
        Tms282MakeCharInfo info = makeCharInfo;
        if (info == null) {
            synchronized (this) {
                if (makeCharInfo == null) {
                    makeCharInfo = Tms282MakeCharInfo.load(Tms282MakeCharInfo.DEFAULT_FILE);
                }
                info = makeCharInfo;
            }
        }
        return info;
    }

    /**
     * Server channel for a zero-based 282 wire channel: channel {@code wire + 1} of the game world, else channel 1.
     */
    static byte serverChannel(int wireChannel) {
        byte wanted = (byte) (wireChannel + 1);
        World world = Server.get().getWorld();
        if (world == null || world.getChannelById(wanted) != null) {
            return wanted;
        }
        return 1;
    }

    /** Listening port of a server channel (as ChannelAcceptor binds it). */
    static int channelPort(byte serverChannel) {
        World world = Server.get().getWorld();
        Channel channel = world == null ? null : world.getChannelById(serverChannel);
        return channel != null ? channel.getPort() : ServerConstants.LOGIN_PORT + 100 + serverChannel;
    }
}
