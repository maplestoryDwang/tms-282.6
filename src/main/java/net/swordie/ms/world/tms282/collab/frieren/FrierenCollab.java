package net.swordie.ms.world.tms282.collab.frieren;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.protocol.Protocol;
import net.swordie.ms.connection.protocol.ProtocolVersion;
import net.swordie.ms.enums.DeathType;
import net.swordie.ms.life.Life;
import net.swordie.ms.life.Summon;
import net.swordie.ms.life.mob.Mob;
import net.swordie.ms.util.Position;
import net.swordie.ms.world.field.Field;
import net.swordie.ms.world.field.Foothold;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Server side of 葬送的芙莉蓮 一起冒險 (80012691) / 芙莉蓮夥伴們 (80012711), 282 profile only. Entry points (hooks
 * owned by other files, snippets in research/tms2823/collab-frieren/README.md):
 * <ul>
 *     <li>{@link #onMobKilled} from {@code Job.handleMobKilled}: level-range kills fill the 500 gauge; a full
 *     gauge summons the owner-only clone monster 9602566 on a random foothold; the clone's defeat gives the
 *     companion casts (server damage, {@link FrierenCompanionAttack}) and every 2nd defeat a 靈魂艾爾達碎片;</li>
 *     <li>{@link #onToggleRequest} from the CP03BF route: right-click ON/OFF of both skills;</li>
 *     <li>{@link #onFieldEnter} / {@link #onFieldLeave}: companion summons (flag, default OFF) and clone cleanup
 *     (a watchdog also removes a clone whose owner left, since an owner-only mob becomes visible to everyone
 *     once its owner is not in the field).</li>
 * </ul>
 * Player text is Traditional Chinese through the existing chat-message path.
 */
public final class FrierenCollab {
    private static volatile FrierenCollab instance;

    private final FrierenSettings settings;
    private final FrierenStateStore store;
    private final Supplier<LocalDateTime> clock;
    private final Map<Integer, ActiveClone> clones = new ConcurrentHashMap<>();
    private final Map<Integer, java.time.LocalDate> limitNotified = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "tms282-frieren");
        t.setDaemon(true);
        return t;
    });

    /** A summoned clone of one owner. */
    record ActiveClone(int ownerId, Field field, int objectId, long expiresAt) {
    }

    public FrierenCollab(FrierenSettings settings, FrierenStateStore store, Supplier<LocalDateTime> clock) {
        this.settings = settings;
        this.store = store;
        this.clock = clock;
    }

    public static FrierenCollab get() {
        FrierenCollab i = instance;
        if (i == null) {
            synchronized (FrierenCollab.class) {
                i = instance;
                if (i == null) {
                    FrierenSettings s = FrierenSettings.load();
                    i = new FrierenCollab(s, new FrierenStateStore(s.stateDir()), () -> LocalDateTime.now(FrierenSettings.ZONE));
                    instance = i;
                }
            }
        }
        return i;
    }

    public FrierenSettings settings() {
        return settings;
    }

    public FrierenStateStore store() {
        return store;
    }

    static boolean is282() {
        try {
            return Protocol.active().version() == ProtocolVersion.TMS_282_3;
        } catch (RuntimeException | LinkageError e) {
            return false;
        }
    }

    /** |mob level - character level| within the configured range. */
    public static boolean isLevelRange(int characterLevel, int mobLevel, int range) {
        return mobLevel > 0 && Math.abs(mobLevel - characterLevel) <= range;
    }

    private boolean running() {
        return is282() && settings.active(clock.get());
    }

    // ------------------------------------------------------------------ kills

    /** Hook: {@code Job.handleMobKilled(mob, skillID)} (the killer is the job's character). */
    public void onMobKilled(Char chr, Mob mob) {
        if (chr == null || mob == null || !running()) {
            return;
        }
        try {
            if (mob.getTemplateId() == FrierenSkills.CLONE_MOB) {
                ActiveClone clone = clones.get(chr.getId());
                if (clone != null && clone.objectId() == mob.getObjectId()) {
                    clones.remove(chr.getId());
                    onCloneDefeated(chr, mob.getObjectId());
                }
                return;
            }
            if (!chr.hasSkill(FrierenSkills.PARTNERS) || mob.isBoss()
                    || !isLevelRange(chr.getLevel(), mob.getLevel(), settings.levelRange())) {
                return;
            }
            FrierenState state = store.get(chr.getId());
            var today = clock.get().toLocalDate();
            FrierenState.KillResult result;
            synchronized (state) {
                result = state.onLevelRangeKill(settings, today);
            }
            switch (result) {
                case IGNORED -> {
                    if (state.partnersOn() && state.dailyLimitReached(settings, today)
                            && !today.equals(limitNotified.put(chr.getId(), today))) {
                        chr.chatMessage("[芙莉蓮夥伴們] 今天的複製體怪物召喚次數已達上限（" + settings.dailyCloneLimit() + "次）。");
                    }
                }
                case COUNTED -> {
                    if (state.gauge() % 100 == 0) {
                        chr.chatMessage("[芙莉蓮夥伴們] 量表 " + state.gauge() + "／" + settings.gaugeMax());
                    }
                    if (state.gauge() % 25 == 0) {
                        store.save(chr.getId());
                    }
                }
                case SUMMON_CLONE -> {
                    if (!spawnClone(chr)) {
                        synchronized (state) {
                            state.cloneNotPlaced(settings);
                        }
                    }
                    store.save(chr.getId());
                }
            }
        } catch (RuntimeException e) {
            System.err.println("[Frieren] kill hook for " + chr.getId() + ": " + e);
        }
    }

    // ------------------------------------------------------------------ clone

    /** Summons the owner-only clone on a random foothold of the owner's field. */
    boolean spawnClone(Char chr) {
        Field field = chr.getField();
        if (field == null) {
            return false;
        }
        ActiveClone old = clones.remove(chr.getId());
        if (old != null) {
            removeClone(old, old.field() == field ? chr : null);
        }
        List<Foothold> floors = new ArrayList<>();
        for (Foothold fh : field.getFootholds()) {
            if (!fh.isWall()) {
                floors.add(fh);
            }
        }
        if (floors.isEmpty()) {
            return false;
        }
        Position pos = floors.get(ThreadLocalRandom.current().nextInt(floors.size())).getRandomPosition();
        Mob mob = field.spawnMobForOnlyChar(chr, FrierenSkills.CLONE_MOB, pos.getX(), pos.getY(), false);
        if (mob == null) {
            return false;
        }
        long expires = settings.cloneLifetimeSec() == 0 ? Long.MAX_VALUE
                : System.currentTimeMillis() + settings.cloneLifetimeSec() * 1000L;
        ActiveClone clone = new ActiveClone(chr.getId(), field, mob.getObjectId(), expires);
        clones.put(chr.getId(), clone);
        chr.chatMessage("[芙莉蓮夥伴們] 量表已滿！地圖某處出現了外形與你相似的複製體怪物。");
        if (settings.gaugePacket()) {
            chr.write(FrierenPackets.skillEventEffect(FrierenSkills.PARTNERS, 0, mob.getObjectId(), 0, pos.getX(), pos.getY()));
        }
        scheduleWatch(chr, clone);
        return true;
    }

    private void scheduleWatch(Char owner, ActiveClone clone) {
        scheduler.schedule(() -> {
            try {
                if (clones.get(clone.ownerId()) != clone) {
                    return; // defeated, replaced or cleaned up by onFieldLeave
                }
                boolean ownerHere = owner.getField() == clone.field() && clone.field().getCharByID(clone.ownerId()) != null;
                if (!ownerHere || System.currentTimeMillis() >= clone.expiresAt()) {
                    clones.remove(clone.ownerId(), clone);
                    removeClone(clone, ownerHere ? owner : null);
                    if (ownerHere) {
                        owner.chatMessage("[芙莉蓮夥伴們] 複製體怪物消失了。");
                    }
                    return;
                }
                scheduleWatch(owner, clone);
            } catch (RuntimeException e) {
                System.err.println("[Frieren] clone watch: " + e);
            }
        }, 2, TimeUnit.SECONDS);
    }

    /**
     * Removes the clone mob without drops. With the owner present only the owner is told (only the owner saw it);
     * without the owner the leave goes to the whole field, since players who entered after the owner left may have
     * been sent it (an owner-only mob is broadcast once its owner is elsewhere).
     */
    private static void removeClone(ActiveClone clone, Char ownerPresent) {
        Field field = clone.field();
        Life life = field.getLifeByObjectID(clone.objectId());
        if (!(life instanceof Mob mob) || mob.getTemplateId() != FrierenSkills.CLONE_MOB) {
            return;
        }
        if (ownerPresent != null) {
            field.removeMob(ownerPresent, clone.objectId(), DeathType.INSTA_DEATH);
        } else {
            field.removeMob(clone.objectId(), DeathType.INSTA_DEATH);
        }
    }

    /** Hook for field changes (before the character is taken out of {@code oldField}): clone and companions. */
    public void onFieldLeave(Char chr, Field oldField) {
        if (chr == null) {
            return;
        }
        ActiveClone clone = clones.remove(chr.getId());
        if (clone != null) {
            removeClone(clone, oldField == clone.field() ? chr : null);
        }
        if (oldField != null && settings.companionSummons()) {
            for (int id : FrierenSkills.COMPANION_SUMMONS) {
                oldField.removeSummon(id, chr.getId());
            }
        }
    }

    void onCloneDefeated(Char chr, int objectId) {
        FrierenState state = store.get(chr.getId());
        FrierenState.CloneReward reward;
        synchronized (state) {
            reward = state.onCloneDefeated(settings);
        }
        store.save(chr.getId());
        if (reward.fragments() > 0) {
            if (chr.canHold(FrierenSkills.SOL_ERDA_FRAGMENT, reward.fragments())) {
                chr.addItemToInventory(FrierenSkills.SOL_ERDA_FRAGMENT, reward.fragments());
                chr.chatMessage("[芙莉蓮夥伴們] 獲得了靈魂艾爾達碎片 " + reward.fragments() + " 個。");
            } else {
                chr.chatMessage("[芙莉蓮夥伴們] 其他欄位已滿，無法獲得靈魂艾爾達碎片。");
            }
        }
        for (int i = 0; i < reward.casts(); i++) {
            FrierenSkills.CompanionAttack wave = i % 2 == 0 ? FrierenSkills.FRIEREN_WAVE : FrierenSkills.FERN_WAVE;
            scheduler.schedule(() -> companionWave(chr, wave, objectId), 1500L + i * 3000L, TimeUnit.MILLISECONDS);
        }
    }

    private void companionWave(Char chr, FrierenSkills.CompanionAttack wave, int excluded) {
        try {
            if (chr.getField() == null) {
                return;
            }
            chr.chatMessage(wave.skillId() == FrierenSkills.FRIEREN_ATTACK_MAGIC
                    ? "[芙莉蓮] 芙莉蓮登場，施放一般攻擊魔法(除魔魔法)！"
                    : "[費倫] 費倫登場，施展除魔魔法(除魔魔法)！");
            String log = FrierenCompanionAttack.execute(chr, wave, excluded);
            System.out.println("[Frieren] " + chr.getId() + " " + log);
        } catch (RuntimeException e) {
            System.err.println("[Frieren] companion wave: " + e);
        }
    }

    // ------------------------------------------------------------------ toggles

    /** CP03BF: right-click ON/OFF. Returns false when the request is not for a Frieren skill the character has. */
    public boolean onToggleRequest(Char chr, int skillId) {
        if (chr == null || !FrierenSkills.isToggleSkill(skillId) || !chr.hasSkill(skillId)) {
            return false;
        }
        FrierenState state = store.get(chr.getId());
        boolean on;
        synchronized (state) {
            on = state.toggle(skillId);
        }
        store.save(chr.getId());
        chr.chatMessage("[" + FrierenSkills.skillName(skillId) + "] " + (on ? "已開啟。" : "已關閉。"));
        if (skillId == FrierenSkills.ADVENTURE_TOGETHER && settings.companionSummons() && chr.getField() != null) {
            if (on) {
                onFieldEnter(chr);
            } else {
                for (int id : FrierenSkills.COMPANION_SUMMONS) {
                    chr.getField().removeSummon(id, chr.getId());
                }
            }
        }
        return true;
    }

    // ------------------------------------------------------------------ companions (flag)

    /** Hook after field entry: 一起冒險 companions in a map with level-range mobs (flag, default OFF). */
    public void onFieldEnter(Char chr) {
        if (chr == null || !settings.companionSummons() || !running() || !chr.hasSkill(FrierenSkills.ADVENTURE_TOGETHER)) {
            return;
        }
        FrierenState state = store.get(chr.getId());
        Field field = chr.getField();
        if (!state.adventureOn() || field == null || chr.getPosition() == null) {
            return;
        }
        boolean levelRangeMap = field.getMobs().stream()
                .anyMatch(m -> !m.isBoss() && isLevelRange(chr.getLevel(), m.getLevel(), settings.levelRange()));
        if (!levelRangeMap) {
            return;
        }
        for (int id : FrierenSkills.COMPANION_SUMMONS) {
            try {
                Summon summon = Summon.getSummonBy(chr, id, 1);
                summon.setAssistType(net.swordie.ms.enums.AssistType.None);
                summon.setAttackActive(false);
                field.spawnSummon(summon);
            } catch (RuntimeException e) {
                System.err.println("[Frieren] companion summon " + id + ": " + e);
            }
        }
    }

    /** For checks and GM tools: the owner's active clone object id, or 0. */
    public int activeCloneObjectId(int characterId) {
        ActiveClone c = clones.get(characterId);
        return c == null ? 0 : c.objectId();
    }
}
