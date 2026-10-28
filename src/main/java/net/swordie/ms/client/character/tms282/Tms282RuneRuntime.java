package net.swordie.ms.client.character.tms282;

import net.swordie.ms.GameSettings;
import net.swordie.ms.client.Client;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.achievement.AchievementHandler;
import net.swordie.ms.client.character.runestones.RuneStone;
import net.swordie.ms.client.character.runestones.RuneStone.RuneStoneAction;
import net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat;
import net.swordie.ms.connection.protocol.Protocol;
import net.swordie.ms.connection.protocol.ProtocolVersion;
import net.swordie.ms.connection.packet.Tms282RunePackets;
import net.swordie.ms.connection.packet.Tms282RuneRequests;
import net.swordie.ms.constants.FieldConstants;
import net.swordie.ms.enums.Stat;
import net.swordie.ms.world.field.Field;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

/** One field/session-bound, single-use native rune challenge per character. No database access. */
public final class Tms282RuneRuntime {
    static final long CHALLENGE_MILLIS = 60_000;
    private static final long COOLDOWN_MILLIS = FieldConstants.RUNE_COOLDOWN_TIME * 60_000L;
    private Tms282RuneRuntime() { }

    public static final class State {
        Pending pending;
        RuneStoneAction previous;
    }
    private static final class Pending {
        final WeakReference<Field> field;
        final WeakReference<RuneStone> rune;
        final WeakReference<Client> client;
        final WeakReference<Char> owner;
        final RuneStoneAction action;
        final long expires;
        int nextDirection;
        Pending(Char chr, RuneStoneAction action, long now) {
            field = new WeakReference<>(chr.getField());
            rune = new WeakReference<>(chr.getField().getRuneStone());
            client = new WeakReference<>(chr.getClient());
            owner = new WeakReference<>(chr);
            this.action = action; expires = now + CHALLENGE_MILLIS;
        }
        boolean current(Char chr, long now) {
            return now < expires && owner.get() == chr && field.get() != null && chr.getField() == field.get()
                    && chr.getField().getRuneStone() == rune.get() && rune.get() != null
                    && chr.getClient() == client.get() && client.get() != null && !client.get().isExiting();
        }
    }
    private static State state(Char chr) {
        State state = chr.getRuneChallengeState();
        if (state == null) { state = new State(); chr.setRuneChallengeState(state); }
        return state;
    }
    /** Called at the shared setField boundary, including a trip away and back to the same Field object. */
    public static void clear(Char chr) {
        if (chr == null) return;
        synchronized (chr) {
            State s = chr.getRuneChallengeState();
            if (s != null) s.pending = null;
        }
    }
    /** The native rune special powers end on field change; their shared EXP reward is independent. */
    public static void onFieldChange(Char chr) {
        clear(chr);
        if (chr == null || chr.getField() == null || Protocol.active().version() != ProtocolVersion.TMS_282_3) return;
        synchronized (chr) {
            var tsm = chr.getTemporaryStatManager();
            for (int skillId : new int[]{80002888,80002889,80002890}) tsm.removeStatsBySkill(skillId);
            chr.getField().removeSummon(80002888, chr.getId());
        }
    }
    /** CP04B4 temporary authority, never a saved skill row; also rejects an expired CTS before timer cleanup. */
    public static int temporarySkillLevel(Char chr, int skillId, long now) {
        if (chr == null || chr.getField() == null || !GameSettings.runeEnabled()
                || Protocol.active().version() != ProtocolVersion.TMS_282_3) return 0;
        CharacterTemporaryStat stat = switch (skillId) {
            case 80002889 -> CharacterTemporaryStat.ShineRuneAttack;
            case 80002890 -> CharacterTemporaryStat.RuneContagion;
            default -> null;
        };
        if (stat == null) return 0;
        var option = chr.getTemporaryStatManager().getOption(stat);
        long duration = option.isInMillis() ? option.tOption : option.tOption * 1000L;
        return option.nOption != 0 && option.rOption == skillId && duration > 0
                && now >= option.startTime && now - option.startTime < duration ? 1 : 0;
    }
    public static void cancel(Char chr) {
        clear(chr);
        if (chr != null) chr.write(Tms282RunePackets.close());
    }
    public static boolean use(Char chr, Tms282RuneRequests.Use request) {
        RuneStoneAction[] actions = RuneStoneAction.values();
        return begin(chr, request, actions[ThreadLocalRandom.current().nextInt(actions.length)], System.currentTimeMillis());
    }
    static boolean begin(Char chr, Tms282RuneRequests.Use request, RuneStoneAction action, long now) {
        synchronized (chr) {
            State s = state(chr);
            if (s.pending != null && s.pending.current(chr, now)) return false;
            s.pending = null;
            Field field = chr.getField();
            RuneStone rune = field == null ? null : field.getRuneStone();
            if (!GameSettings.runeEnabled() || chr.getHP() <= 0 || chr.getClient() == null
                    || chr.getClient().isExiting() || rune == null || rune.getRuneType() == null
                    || request.index() != 0 || request.runeType() != rune.getRuneType().getVal()) {
                System.out.println("[TMS282] rune refused character=" + chr.getId() + " field=" + (field == null ? 0 : field.getId())
                        + ": enabled=" + GameSettings.runeEnabled() + " hp=" + chr.getHP() + " rune=" + (rune == null ? "none"
                        : String.valueOf(rune.getRuneType())) + " request index=" + request.index() + " type=" + request.runeType());
                return false;
            }
            long remaining = chr.getRuneCooldown() + COOLDOWN_MILLIS - now;
            if (remaining > 0) {
                chr.write(Tms282RunePackets.cooldown((int) Math.min(Integer.MAX_VALUE, remaining))); return false;
            }
            int minimumLevel = field.getMobs().stream().mapToInt(m -> m.getForcedMobStat().getLevel()).min().orElse(0);
            if (minimumLevel > chr.getStat(Stat.level)) {
                // Was a silent refusal: the space key then looks dead. Say why.
                chr.chatMessage("等級低於這張地圖的怪物（Lv." + minimumLevel + "），無法解開符文。");
                System.out.println("[TMS282] rune refused character=" + chr.getId() + " field=" + field.getId()
                        + ": level " + chr.getStat(Stat.level) + " < lowest mob level " + minimumLevel);
                return false;
            }
            // Preserve the native, verified cipher design; never immediately reuse the previous challenge.
            if (action == s.previous) action = RuneStoneAction.values()[(action.ordinal() + 1) % RuneStoneAction.values().length];
            s.previous = action;
            s.pending = new Pending(chr, action, now);
            chr.write(Tms282RunePackets.challenge(action.getPacket()));
            return true;
        }
    }
    public static boolean direction(Char chr, Tms282RuneRequests.Direction request) {
        return direction(chr, request, System.currentTimeMillis());
    }
    static boolean direction(Char chr, Tms282RuneRequests.Direction request, long now) {
        synchronized (chr) {
            State s = state(chr); Pending p = s.pending;
            if (!GameSettings.runeEnabled() || p == null || !p.current(chr, now)
                    || request.index() != p.nextDirection || p.nextDirection >= 4
                    || request.direction() != p.action.getAction()[p.nextDirection]) {
                cancel(chr); return false;
            }
            p.nextDirection++; return true;
        }
    }
    public static boolean finish(Char chr, Tms282RuneRequests.Finish request) {
        return finish(chr, request, System.currentTimeMillis());
    }
    static boolean finish(Char chr, Tms282RuneRequests.Finish request, long now) {
        synchronized (chr) {
            State s = state(chr); Pending p = s.pending;
            s.pending = null; // Any completion/cancellation consumes this attempt before awarding anything.
            if (!GameSettings.runeEnabled() || p == null || !p.current(chr, now) || chr.getHP() <= 0
                    || !request.completed() || p.nextDirection != 4
                    || request.tokenA() != 0x06425350 || request.tokenB() != tokenB(p.action)
                    || (!request.compact() && !Arrays.equals(request.directions(), p.action.getAction()))) {
                chr.write(Tms282RunePackets.close()); return false;
            }
            Field field = p.field.get(); RuneStone rune = p.rune.get();
            synchronized (field) {
                // Two characters may solve a field's challenge concurrently; only one can consume the stone.
                if (field.getRuneStone() != rune || chr.getRuneCooldown() + COOLDOWN_MILLIS > now) {
                    chr.write(Tms282RunePackets.close()); return false;
                }
                chr.setRuneCooldown(now);
                // Field.useRuneStone owns removal, the single skill acknowledgement and the respawn timer.
                field.useRuneStone(chr.getClient(), rune);
                rune.activateRuneStoneEffect(chr);
            }
            if (Tms282VMatrixRuntime.enabled())
                Tms282VMatrixRuntime.event(chr, Tms282VMatrixSpecialEngine.Type.RUNE, 1, 0);
            else if (chr.getSpecialNodeSkill() != null) {
                chr.getSpecialNodeSkill().setRune(true); chr.getSpecialNodeSkill().activate();
            }
            if (chr.getAccount() != null) AchievementHandler.handleUseRuneStone(chr, rune.getRuneType().getVal());
            return true;
        }
    }
    // Captured from the 282.4 native UI setter for all twenty existing ciphertexts (20/20 native checks).
    static int tokenB(RuneStoneAction action) {
        return switch (action) {
            case LEFT_RIGHT_DOWN_DOWN -> 0x34; case LEFT_RIGHT_UP_UP -> 0x35;
            case LEFT_RIGHT_LEFT_RIGHT -> 0x3B; case LEFT_DOWN_DOWN_LEFT -> 0x3C;
            case LEFT_DOWN_RIGHT_RIGHT -> 0x45; case UP_LEFT_RIGHT_RIGHT -> 0x37;
            case UP_DOWN_RIGHT_DOWN -> 0x3A; case DOWN_UP_DOWN_UP -> 0x36;
            case DOWN_LEFT_RIGHT_RIGHT -> 0x3E; case DOWN_DOWN_LEFT_UP -> 0x3F;
            case DOWN_DOWN_RIGHT_RIGHT -> 0x40; case DOWN_RIGHT_DOWN_UP -> 0x44;
            case DOWN_UP_RIGHT_LEFT -> 0x46; case DOWN_DOWN_LEFT_LEFT -> 0x47;
            case RIGHT_LEFT_LEFT_RIGHT -> 0x38; case RIGHT_UP_UP_LEFT -> 0x39;
            case RIGHT_UP_DOWN_DOWN -> 0x3D; case RIGHT_UP_RIGHT_DOWN -> 0x41;
            case RIGHT_DOWN_DOWN_LEFT -> 0x42; case RIGHT_UP_RIGHT_RIGHT -> 0x43;
        };
    }
}
