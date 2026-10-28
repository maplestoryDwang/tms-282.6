package net.swordie.ms.world.tms282.revival.hub;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.packet.WvsContext;
import net.swordie.ms.enums.MessageType;
import net.swordie.ms.life.mob.Mob;
import net.swordie.ms.loaders.StringData;
import net.swordie.ms.world.tms282.activity.ActivityRewards;
import net.swordie.ms.world.tms282.revival.RevivalEvents;

/** Live wiring of the hubs: the shared {@link MissionService}, the kill hook and the script entry points. */
public final class RevivalHubs {
    private static volatile MissionService missions = new MissionService(new SqlProgressStore(), RevivalEvents::today,
            HubCatalog::open, HubCatalog::openAll);

    private RevivalHubs() {
    }

    public static MissionService missions() {
        return missions;
    }

    /** Checks replace the service (fake store / clock); {@code null} restores the SQL one. */
    public static void install(MissionService service) {
        missions = service != null ? service : new MissionService(new SqlProgressStore(), RevivalEvents::today,
                HubCatalog::open, HubCatalog::openAll);
    }

    public static MissionService.Actor actor(Char chr) {
        return new MissionService.Actor() {
            @Override
            public int charId() {
                return chr.getId();
            }

            @Override
            public int level() {
                return chr.getLevel();
            }

            @Override
            public void notice(String text) {
                chr.write(WvsContext.message(MessageType.SYSTEM_MESSAGE, 0, text, (byte) 0));
            }

            @Override
            public boolean inParty() {
                return chr.getParty() != null;
            }

            @Override
            public ActivityRewards.Result grant(String receiptKey, int[] itemIds, int[] quantities) {
                return ActivityRewards.grantOnce(chr, receiptKey, itemIds, quantities);
            }
        };
    }

    private static final java.util.concurrent.ExecutorService LOADER = java.util.concurrent.Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "revival-progress-loader");
        t.setDaemon(true);
        return t;
    });

    /** Char.warp after the field entry: one background load per character and session (no-op when loaded). */
    public static void onFieldEnter(Char chr) {
        // Runs on every field entry; the package checks forbid real DB access (-Dtms282.tests.rejectRealDatabase),
        // the same rule as TimedSkills.removeExpired (Tms282MyHomeLifecycleCheck counts every JDBC attempt of a warp).
        if (Boolean.getBoolean("tms282.tests.rejectRealDatabase")) return;
        try {
            if (chr == null || chr.getId() <= 0 || HubCatalog.openAll().isEmpty()) return;
            int id = chr.getId();
            boolean all = true;
            for (HubDefinition h : HubCatalog.openAll()) all &= missions.isLoaded(id, h.eventKey());
            if (!all) LOADER.execute(() -> missions.preload(id));
        } catch (RuntimeException e) {
            System.out.println("[Revival] preload " + e.getClass().getSimpleName());
        }
    }

    /** Client disconnect: drop the cached rows. */
    public static void onDisconnect(Char chr) {
        if (chr != null) missions.forget(chr.getId());
    }

    /** Char.handleMobKillForQuest: never throws into the kill path. */
    public static void onMobKilled(Char chr, Mob mob) {
        try {
            if (chr == null || mob == null || chr.getId() <= 0 || HubCatalog.openAll().isEmpty()) return;
            missions.onKill(actor(chr), mob.getTemplateId(), mob.getLevel());
        } catch (RuntimeException e) {
            System.out.println("[Revival] kill hook " + e.getClass().getSimpleName());
        }
    }

    public static HubDialog dialog(Char chr, HubDefinition hub) {
        MissionService.Actor a = actor(chr);
        LiveWallet wallet = new LiveWallet(chr);
        String key = hub.eventKey();
        return new HubDialog(hub, new HubDialog.Ops() {
            public MissionService.Status status(String id) { return missions.status(a, key, id); }
            public MissionService.AcceptResult accept(String id) { return missions.accept(a, key, id); }
            public MissionService.ClaimResult claim(String id) { return missions.claim(a, key, id); }
            public HubExchange.Result buy(HubDefinition.Offer o, int times) {
                return HubCatalog.open(key).isPresent() ? HubExchange.buy(wallet, o, times) : HubExchange.Result.CLOSED;
            }
            public int count(int itemId) { return wallet.count(itemId); }
        }, StringData::getItemStringById);
    }

    public static String page(Char chr, String eventKey) {
        HubDefinition hub = HubCatalog.open(eventKey).orElse(null);
        return hub == null ? "SAY:這個活動目前沒有開放。" : dialog(chr, hub).home();
    }

    public static String select(Char chr, String eventKey, int selection) {
        HubDefinition hub = HubCatalog.open(eventKey).orElse(null);
        return hub == null ? "SAY:這個活動目前沒有開放。" : dialog(chr, hub).select(selection);
    }
}
