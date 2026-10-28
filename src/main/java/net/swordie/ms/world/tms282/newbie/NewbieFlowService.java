package net.swordie.ms.world.tms282.newbie;

import net.swordie.ms.client.Account;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.CharacterStat;
import net.swordie.ms.connection.protocol.Protocol;
import net.swordie.ms.connection.protocol.ProtocolVersion;
import net.swordie.ms.constants.JobConstants;
import net.swordie.ms.scripts.ScriptManagerImpl;
import net.swordie.ms.scripts.ScriptType;
import net.swordie.ms.world.tms282.newbie.steps.SpawnStep;

import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * The newbie flow (新手流程) of the 282 server: new characters are created in the quiet lobby instead of their job's
 * intro map, and an NPC dialog opens by itself - story pages, first job, starter gift, warp menu, as configured in
 * {@code data/newbie/newbie-flow.json} ({@link NewbieFlowSource}, hot reloaded).
 * <ul>
 *     <li>{@link #beforeCreateSave} / {@link #afterCreate}: LoginHandler.createNewCharacter - the Spawn step's map,
 *     then the character's {@code newbie_flow} row (only for characters that spawn in the lobby; a race set back to
 *     {@code "original"} keeps the old behaviour entirely).</li>
 *     <li>{@link #onLoginEntry} (MailLoginHook, every migrate-in) and {@link #onFieldEnter} (Char.warp): after
 *     {@code popupDelayMs}, when the character is still online and in the lobby (or anywhere at login with
 *     {@code resumeOnLogin}), has an unfinished row and no script running, the dialog starts at the stored step.</li>
 * </ul>
 * Only SCRIPT_MESSAGE dialogs (say / yes-no / menu) are used; no in-game direction, cut scene or UI lock. Never throws
 * into its callers.
 */
public final class NewbieFlowService {
    public static final String SCRIPT_NAME = "newbie_flow";

    /** Starts the dialog that runs {@code body} (the real one: ScriptManagerImpl.startJavaDialog). */
    @FunctionalInterface
    public interface DialogStarter {
        boolean start(Char chr, int speakerNpc, Consumer<NewbieDialog> body);
    }

    /** Runs {@code task} after {@code delayMs} off the caller's thread. */
    @FunctionalInterface
    public interface Scheduler {
        void later(long delayMs, Runnable task);
    }

    public enum Trigger {LOGIN, LOBBY}

    private static volatile NewbieFlowService instance;

    private final NewbieFlowSource source;
    private final NewbieStore store;
    private final NewbieGame game;
    private final DialogStarter starter;
    private final Scheduler scheduler;
    private final boolean only282;
    private final ConcurrentHashMap<Integer, Object> running = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, String> activeCharacters = new ConcurrentHashMap<>();

    public NewbieFlowService(NewbieFlowSource source, NewbieStore store, NewbieGame game, DialogStarter starter,
                             Scheduler scheduler, boolean only282) {
        this.source = Objects.requireNonNull(source, "source");
        this.store = Objects.requireNonNull(store, "store");
        this.game = Objects.requireNonNull(game, "game");
        this.starter = Objects.requireNonNull(starter, "starter");
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
        this.only282 = only282;
    }

    public static NewbieFlowService get() {
        NewbieFlowService s = instance;
        if (s == null) {
            synchronized (NewbieFlowService.class) {
                if (instance == null) {
                    instance = new NewbieFlowService(
                            new NewbieFlowSource(NewbieFlowSource.DEFAULT_FILE, NewbieFlowData::problems, System::currentTimeMillis),
                            new JdbcNewbieStore(), new WorldNewbieGame(), NewbieFlowService::scriptDialog,
                            NewbieFlowService::virtualLater, true);
                }
                s = instance;
            }
        }
        return s;
    }

    /** Checks replace the service (fake store / game / dialog); null restores the server's on the next get(). */
    public static void install(NewbieFlowService service) {
        instance = service;
    }

    static boolean scriptDialog(Char chr, int npc, Consumer<NewbieDialog> body) {
        ScriptManagerImpl sm = chr.getScriptManager();
        return sm.startJavaDialog(chr, npc, SCRIPT_NAME, m -> body.accept(new ScriptDialog(m)));
    }

    static void virtualLater(long delayMs, Runnable task) {
        Thread.ofVirtual().name("newbie-popup").start(() -> {
            try {
                if (delayMs > 0) Thread.sleep(delayMs);
                task.run();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }

    public NewbieFlowSource source() {
        return source;
    }

    public NewbieStore store() {
        return store;
    }

    private boolean active() {
        if (!net.swordie.ms.scripts.ScriptManagerImpl.GAME_SCRIPTS_ENABLED) return false;
        return !only282 || Protocol.active().version() == ProtocolVersion.TMS_282_3
                && !"false".equalsIgnoreCase(System.getProperty("tms282.newbie.enabled", "true"));
    }

    // ------------------------------------------------------------------ creation

    /** Before the first save of a new character: the Spawn step's map (lobby by default). */
    public void beforeCreateSave(Char chr, JobConstants.LoginJob loginJob) {
        try {
            if (!active() || chr == null || loginJob == null) return;
            NewbieFlowConfig c = source.current();
            if (!c.enabled()) return;
            Optional<SpawnStep> spawn = c.spawn();
            if (spawn.isEmpty()) return;
            CharacterStat stat = chr.getAvatarData().getCharacterStat();
            int original = (int) stat.getPosMap();
            int map = spawn.get().mapFor(loginJob.name(), original, c.lobbyMap());
            if (map != original) {
                stat.setPosMap(map);
                stat.setPortal(map == c.lobbyMap() ? c.lobbyPortal() : 0);
                System.out.println("[Newbie] new character " + chr.getName() + " (" + loginJob + ") spawns in " + map
                        + " instead of " + original);
            }
        } catch (RuntimeException e) {
            System.out.println("[Newbie] spawn rule failed for " + (chr == null ? "?" : chr.getName()) + ": " + e);
        }
    }

    /** After the new character was saved: its progress row (only when it spawns in the lobby). */
    public void afterCreate(Char chr, Account account, JobConstants.LoginJob loginJob) {
        try {
            if (!active() || chr == null || chr.getId() <= 0 || loginJob == null) return;
            NewbieFlowConfig c = source.current();
            if (!c.enabled()) return;
            int accountId = account == null ? chr.getAccId() : account.getId();
            int userId = account == null ? 0 : account.getOwnerUserId();
            activeCharacters.put(chr.getId(), loginJob.name());
            if (store.create(chr.getId(), accountId, userId, loginJob.name(), c.firstStepId())) {
                System.out.println("[Newbie] flow row created for character " + chr.getId() + " (" + loginJob + ")");
            }
        } catch (SQLException | RuntimeException e) {
            System.out.println("[Newbie] flow row for character " + (chr == null ? "?" : chr.getId()) + " failed: " + e);
        }
    }

    // ------------------------------------------------------------------ popup

    /** Before SetField and entry scripts: recover only the configured old intro maps; never edit offline SQL. */
    public void beforeLoginField(Char chr, Account account) throws SQLException {
        if (!active() || chr == null) return;
        NewbieFlowConfig c = source.current();
        if (!c.enabled()) return;
        CharacterStat stat = chr.getAvatarData().getCharacterStat();
        int saved = (int) stat.getPosMap();
        boolean zeroPrologue = chr.getJob() == 10000 && chr.getLevel() >= 100;
        boolean recover = c.recoveryMaps().contains(saved) && (chr.getLevel() <= 30
                || JobConstants.isZero(chr.getJob()) && chr.getLevel() <= 100);
        Optional<NewbieProgress> existing = store.load(chr.getId());
        if (!recover && !zeroPrologue && saved != c.lobbyMap() && existing.isEmpty() && !activeCharacters.containsKey(chr.getId())) return;
        if (store.load(chr.getId()).isEmpty()) {
            String job = activeCharacters.getOrDefault(chr.getId(), java.util.Arrays.stream(JobConstants.LoginJob.values())
                    .filter(j -> j.getBeginJob().getJobId() == chr.getJob()).map(Enum::name).findFirst().orElse("RECOVERED"));
            job = job.endsWith("RECOVERED") && chr.getSubJob() == 1 ? "DUAL_BLADE" : job;
            store.create(chr.getId(), account == null ? chr.getAccId() : account.getId(),
                    account == null ? 0 : account.getOwnerUserId(), job, c.firstStepId());
        }
        NewbieProgress progress = store.load(chr.getId()).orElseThrow(() -> new SQLException("newbie progress not created"));
        if (zeroPrologue && progress.completed()) {
            String firstJob = c.steps().stream().filter(s -> s instanceof net.swordie.ms.world.tms282.newbie.steps.FirstJobStep)
                    .map(NewbieStep::id).findFirst().orElse(c.firstStepId());
            store.reopenZeroPrologue(chr.getId(), firstJob);
            progress = store.load(chr.getId()).orElseThrow();
        }
        if (!progress.completed()) activeCharacters.put(chr.getId(), progress.loginJob());
        else activeCharacters.remove(chr.getId());
        if (recover && c.spawn().map(sp -> sp.defaultTarget() == SpawnStep.LOBBY).orElse(false)) {
            stat.setPosMap(c.lobbyMap());
            stat.setPortal(c.lobbyPortal());
            System.out.println("[Newbie] recovered character=" + chr.getId() + " intro=" + saved + " -> lobby=" + c.lobbyMap());
        }
    }

    /** New characters use the configured NPC dialogue on their own map while the native cutscenes are repaired. */
    public boolean replacesEntryScript(Char chr) {
        return active() && chr != null && source.current().enabled() && activeCharacters.containsKey(chr.getId());
    }

    public void onLoginEntry(Char chr) {
        schedule(chr, Trigger.LOGIN);
    }

    public void onFieldEnter(Char chr, int fieldId) {
        try {
            if (!active() || chr == null || !activeCharacters.containsKey(chr.getId())) return;
            NewbieFlowConfig c = source.current();
            if (!c.enabled()) return;
            NewbieStep last = c.steps().getLast();
            if (chr.getJob() != 10000 && last instanceof net.swordie.ms.world.tms282.newbie.steps.WarpMenuStep warp
                    && warp.destinations().stream().anyMatch(d -> d.map() == fieldId)) {
                if (store.complete(chr.getId(), last.id())) {
                    activeCharacters.remove(chr.getId());
                    System.out.println("[Newbie] completed on arrival: character=" + chr.getId() + " map=" + fieldId);
                }
            }
            if (fieldId == c.lobbyMap()) schedule(chr, Trigger.LOBBY);
        } catch (SQLException | RuntimeException e) {
            System.out.println("[Newbie] field entry hook failed: " + e);
        }
    }

    private void schedule(Char chr, Trigger trigger) {
        try {
            if (!active() || chr == null || chr.getId() <= 0) return;
            NewbieFlowConfig c = source.current();
            if (!c.enabled()) return;
            scheduler.later(c.popupDelayMs(), () -> tryStart(chr, trigger, 0));
        } catch (RuntimeException e) {
            System.out.println("[Newbie] popup scheduling failed: " + e);
        }
    }

    /** One attempt to open the dialog; public for the checks. */
    public void tryStart(Char chr, Trigger trigger, int attempt) {
        try {
            if (!active() || chr == null) return;
            if (!game.online(chr)) return;
            NewbieFlowConfig c = source.current();
            if (!c.enabled()) return;
            boolean inLobby = chr.getFieldID() == c.lobbyMap();
            if (trigger == Trigger.LOBBY && !inLobby) return;
            if (trigger == Trigger.LOGIN && !inLobby && !c.resumeOnLogin()) return;
            Optional<NewbieProgress> p = store.load(chr.getId());
            if (p.isEmpty() || p.get().completed()) return;
            ScriptManagerImpl sm = chr.getScriptManager();
            if (sm == null) return;
            if (sm.getLastActiveScriptType() != ScriptType.None) {
                // another script / dialog is open (field script, NPC): try again a little later
                if (attempt < 5) scheduler.later(3_000, () -> tryStart(chr, trigger, attempt + 1));
                return;
            }
            Object token = sm;
            Object prev = running.putIfAbsent(chr.getId(), token);
            if (prev != null) {
                if (prev == token) return; // this login's dialog is already open
                running.put(chr.getId(), token); // a dialog of an earlier login that has not noticed its logout yet
            }
            NewbieProgress progress = p.get();
            boolean started;
            try {
                started = starter.start(chr, c.speakerNpc(), dialog -> {
                try {
                    NewbieFlowRunner.End end = NewbieFlowRunner.run(new NewbieRun(chr, dialog, c, store, game, progress));
                    System.out.println("[Newbie] dialog of character " + chr.getId() + " ended: " + end);
                } catch (SQLException e) {
                    System.out.println("[Newbie] dialog of character " + chr.getId() + " stopped on a database error: " + e);
                } finally {
                    running.remove(chr.getId(), token);
                    reopenForGift(chr, c);
                }
                });
            } catch (RuntimeException e) {
                running.remove(chr.getId(), token);
                throw e;
            }
            if (!started) {
                running.remove(chr.getId(), token);
                if (attempt < 5) scheduler.later(3_000, () -> tryStart(chr, trigger, attempt + 1));
            } else {
                System.out.println("[Newbie] dialog opened for character " + chr.getId() + " at step "
                        + (progress.stepId().isEmpty() ? c.firstStepId() : progress.stepId()) + " (" + trigger + ")");
            }
        } catch (SQLException | RuntimeException e) {
            System.out.println("[Newbie] popup for character " + (chr == null ? "?" : chr.getId()) + " failed: " + e);
        }
    }

    /** The starter gift is not optional: a declined / closed dialog comes back while the character is in the lobby. */
    private void reopenForGift(Char chr, NewbieFlowConfig c) {
        try {
            if (!game.online(chr) || chr.getFieldID() != c.lobbyMap()) return;
            Optional<NewbieProgress> p = store.load(chr.getId());
            if (p.isEmpty() || p.get().completed() || p.get().giftClaimed()) return;
            int at = c.indexOf(p.get().stepId());
            if (at < 0 || !(c.steps().get(at) instanceof net.swordie.ms.world.tms282.newbie.steps.StarterGiftStep)) return;
            scheduler.later(GIFT_REOPEN_MS, () -> tryStart(chr, Trigger.LOBBY, 0));
        } catch (SQLException | RuntimeException e) {
            System.out.println("[Newbie] gift reopen for character " + chr.getId() + " failed: " + e);
        }
    }

    private static final long GIFT_REOPEN_MS = 2_000;

    /** True while a flow dialog of this character is open (checks). */
    public boolean isRunning(int characterId) {
        return running.containsKey(characterId);
    }

    /** The original Zero entry script shares the same save/receipt path when its intro map is retained. */
    public boolean completeZeroPrologue(Char chr) {
        if (Protocol.active().version() != ProtocolVersion.TMS_282_3 || chr == null) return false;
        try {
            // An already advanced character without our pre-advancement row may have received the old script's
            // rewards. Only retry 10112 when its ZERO row proves this flow owned the unfinished prologue.
            Optional<NewbieProgress> prior = store.load(chr.getId());
            if (chr.getJob() == 10112 && (prior.isEmpty() || !"ZERO".equals(prior.get().loginJob()))) return true;
            store.create(chr.getId(), chr.getAccId(), chr.getUser() == null ? 0 : chr.getUser().getId(),
                    "ZERO", source.current().firstStepId());
            NewbieGame.Job result = game.completeZeroPrologue(chr, () -> store.markZeroJobDone(chr.getId(), chr.getName()));
            return (result == NewbieGame.Job.ADVANCED || result == NewbieGame.Job.ALREADY) && chr.getJob() == 10112;
        } catch (SQLException | RuntimeException e) {
            System.out.println("[Newbie] Zero entry retry required: character=" + chr.getId() + " " + e);
            return false;
        }
    }

    // ------------------------------------------------------------------ admin

    public NewbieFlowSource.Status status() {
        return source.status();
    }

    public NewbieFlowSource.Status reload() {
        return source.reload();
    }

    public List<NewbieStore.Row> progress(String query, int limit) throws SQLException {
        return store.list(query, limit);
    }
}
