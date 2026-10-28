package net.swordie.ms.client.character.tms282;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.packet.Tms282FamiliarPackets;
import net.swordie.ms.constants.GameConstants;
import net.swordie.ms.enums.BaseStat;
import net.swordie.ms.life.mob.Mob;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * The summoned 萌獸 in the field (research/tms2823/familiar-20260927/PAWN-RPC-RESEARCH.md):
 * <ul>
 *     <li>the owner's client runs the familiar's AI and sends its RPCs on the pawn actor (0x60000000 + id): 84
 *     FlushMovePath (move path bytes) is relayed to the other players as 85; 88 CreateMagicAtom as 90; 86 AttackMob
 *     (i32 attack index, i32 vector of mob object ids, u8, i32 extra) names targets only: the server computes the
 *     damage, applies it through the 265 Mob.damage path and sends 87 OnAttackMob to everyone in the field;</li>
 *     <li>other players see the familiar through the same AFamiliar create / destroy with the owner's character id as
 *     LP0425 prefix (5AA7B10 resolves it to that remote user's UserCommon world);</li>
 *     <li>maps with info/vanishFamiliar forbid familiars: controller leaf60 0 there, summon refused (reason 12), a
 *     summoned familiar stays saved but is not created.</li>
 * </ul>
 * Damage (policy, 282 data has no familiar damage): the owner's stat attack (4 x main stat + main / 4) x attack / 100
 * x FamiliarTable stat[grade][level] pad, +-10%, one line per mob, at most 8 mobs, one attack per 300 ms, targets
 * within 1000 px of the owner.
 */
public final class Tms282FamiliarCombat {
    public static final Path ATTACK = Path.of("data", "protocol", "tms282.3", "familiar-attack.tsv");
    /** Maps with info/vanishFamiliar = 1 (265 Map.wz; none in the 282 client). */
    public static final Set<Integer> NO_FAMILIAR_MAPS = Set.of(309000000, 410009400, 410011400, 693005815, 811000099);
    static final int MAX_MOBS = 8;
    static final long MIN_INTERVAL_MS = 300;
    static final int MAX_RANGE = 1000;

    private static volatile Map<String, Double> ratios;
    private static final Map<Integer, Long> LAST_ATTACK = new ConcurrentHashMap<>();
    private static final ScheduledExecutorService LATER = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "tms282-familiar-observers");
        t.setDaemon(true);
        return t;
    });

    private Tms282FamiliarCombat() {
    }

    public static boolean allowedIn(int fieldId) {
        return !NO_FAMILIAR_MAPS.contains(fieldId);
    }

    static double ratio(int grade, int level) {
        Map<String, Double> map = ratios;
        if (map == null) {
            map = new HashMap<>();
            try {
                for (String line : Files.readAllLines(ATTACK)) {
                    if (line.isBlank() || line.startsWith("#")) {
                        continue;
                    }
                    String[] f = line.split("\t");
                    map.put(f[0].strip() + "/" + f[1].strip(), Double.parseDouble(f[2].strip()));
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            ratios = map;
        }
        int key = Math.max(0, Math.min(4, level - 1));
        return map.getOrDefault(grade + "/" + key, 0.2);
    }

    /** One familiar damage line against a mob (policy above). */
    public static long damage(Char chr, Tms282FamiliarState.RegisteredInfo familiar) {
        int main = Math.max(Math.max(chr.getTotalStat(BaseStat.str), chr.getTotalStat(BaseStat.dex)),
                Math.max(chr.getTotalStat(BaseStat.inte), chr.getTotalStat(BaseStat.luk)));
        int attack = Math.max(chr.getTotalStat(BaseStat.pad), chr.getTotalStat(BaseStat.mad));
        double base = (4.0 * main + main / 4.0) * Math.max(1, attack) / 100.0;
        double line = base * ratio(familiar.grade(), familiar.level()) * ThreadLocalRandom.current().nextDouble(0.9, 1.1);
        return Math.max(1, Math.min(GameConstants.DAMAGE_CAP, (long) line));
    }

    /** Frames the owner's client sent on its pawn actor. Returns a log line. */
    public static String onPawnFrame(Char chr, int wire, byte[] args) {
        long pawn = Tms282FamiliarPawn.pawnActorId(chr);
        Tms282FamiliarState state = Tms282FamiliarRuntime.cached(chr);
        if (state == null || state.summonedSerial() == 0 || chr.getField() == null) {
            return "pawn frame " + wire + " without a summoned familiar: ignored";
        }
        switch (wire) {
            case 84 -> {
                broadcast(chr, Tms282FamiliarPackets.pawnCallback(chr.getId(), pawn, 85, args), false);
                return "move path " + args.length + " B relayed";
            }
            case 88 -> {
                ByteBuffer in = ByteBuffer.wrap(args).order(ByteOrder.LITTLE_ENDIAN);
                if (args.length < 12) {
                    return "magic atom short: " + HexFormat.of().formatHex(args);
                }
                byte[] out = new byte[12];
                System.arraycopy(args, 0, out, 0, 12);
                broadcast(chr, Tms282FamiliarPackets.pawnCallback(chr.getId(), pawn, 90, out), false);
                return "magic atom " + in.getInt(0) + " at " + in.getInt(4) + "," + in.getInt(8) + " relayed";
            }
            case 86 -> {
                return attack(chr, state, args);
            }
            default -> {
                return "pawn wire " + wire + " unhandled: " + HexFormat.of().formatHex(args);
            }
        }
    }

    static String attack(Char chr, Tms282FamiliarState state, byte[] args) {
        ByteBuffer in = ByteBuffer.wrap(args).order(ByteOrder.LITTLE_ENDIAN);
        int attackIndex;
        int count;
        int[] mobs;
        int flag;
        int extra;
        try {
            attackIndex = in.getInt();
            count = varint(in);
            if (count < 0 || count > 64 || count * 4 > in.remaining()) {
                return "attack: bad mob count " + count;
            }
            mobs = new int[count];
            for (int i = 0; i < count; i++) {
                mobs[i] = in.getInt();
            }
            flag = Byte.toUnsignedInt(in.get());
            extra = in.getInt();
        } catch (RuntimeException e) {
            return "attack: unreadable " + HexFormat.of().formatHex(args);
        }
        long now = System.currentTimeMillis();
        Long last = LAST_ATTACK.put(chr.getId(), now);
        if (last != null && now - last < MIN_INTERVAL_MS) {
            return "attack: too fast (" + (now - last) + " ms)";
        }
        var familiar = state.registered().get(state.summonedSerial());
        Map<Integer, long[]> hits = new LinkedHashMap<>();
        java.util.List<Map.Entry<Mob, Long>> targets = new java.util.ArrayList<>();
        for (int i = 0; i < mobs.length && hits.size() < MAX_MOBS; i++) {
            if (!(chr.getField().getLifeByObjectID(mobs[i]) instanceof Mob mob) || mob.getHp() <= 0 || hits.containsKey(mobs[i])) {
                continue;
            }
            if (chr.getPosition() != null && mob.getPosition() != null
                    && (Math.abs(mob.getPosition().getX() - chr.getPosition().getX()) > MAX_RANGE
                    || Math.abs(mob.getPosition().getY() - chr.getPosition().getY()) > MAX_RANGE)) {
                continue;
            }
            long dmg = damage(chr, familiar);
            hits.put(mobs[i], new long[]{dmg});
            targets.add(Map.entry(mob, dmg));
        }
        // 87 OnAttackMob first (641F6A0 looks every mob up with CMobPool::GetMob 3FA4500 and skips a missing one), then
        // the damage: a killing hit's MOB_LEAVE_FIELD sent before 87 removed the mob, so the familiar's hit and damage
        // numbers were never shown (research/tms2823/familiar-mobhp-0930).
        broadcast(chr, Tms282FamiliarPackets.pawnAttack(chr.getId(), Tms282FamiliarPawn.pawnActorId(chr), attackIndex, hits,
                extra), true);
        for (Map.Entry<Mob, Long> target : targets) {
            target.getKey().damage(chr, target.getValue(), 0);
        }
        return "attack index " + attackIndex + " flag " + flag + " extra " + extra + " mobs " + mobs.length + " hit "
                + hits.size() + " " + hits.values().stream().map(v -> Long.toString(v[0])).toList();
    }

    static int varint(ByteBuffer in) {
        int value = 0;
        for (int i = 0; i < 3; i++) {
            int b = Byte.toUnsignedInt(in.get());
            value |= (b & 127) << (7 * i);
            if ((b & 128) == 0) {
                return value >>> 1;
            }
        }
        throw new IllegalArgumentException("varint");
    }

    /** To the other players in the owner's field (and to the owner too when {@code withOwner}). */
    static void broadcast(Char owner, OutPacket packet, boolean withOwner) {
        if (owner.getField() == null) {
            return;
        }
        for (Char other : owner.getField().getChars()) {
            if (other != owner || withOwner) {
                other.write(packet);
            }
        }
    }

    /** The owner's familiar for the other players in the field (after a summon / field entry). */
    public static void showToOthers(Char owner, Tms282FamiliarState state) {
        for (OutPacket p : Tms282FamiliarPawn.observerCreate(owner, state)) {
            broadcast(owner, p, false);
        }
    }

    public static void hideFromOthers(Char owner) {
        broadcast(owner, Tms282FamiliarPackets.pawnDestroy(owner.getId(), Tms282FamiliarPawn.pawnActorId(owner)), false);
    }

    /** A player entered a field: after the field's users are shown, their summoned familiars (1.5 s later). */
    public static void showOthersTo(Char observer) {
        var field = observer.getField();
        if (field == null) {
            return;
        }
        LATER.schedule(() -> {
            try {
                if (observer.getField() != field) {
                    return;
                }
                for (Char owner : field.getChars()) {
                    Tms282FamiliarState state = owner == observer ? null : Tms282FamiliarRuntime.cached(owner);
                    if (state != null && state.summonedSerial() != 0 && allowedIn(field.getId())) {
                        for (OutPacket p : Tms282FamiliarPawn.observerCreate(owner, state)) {
                            observer.write(p);
                        }
                    }
                }
            } catch (RuntimeException e) {
                System.out.println("[TMS282] familiar observer create failed char=" + observer.getId() + ": " + e);
            }
        }, 1500, TimeUnit.MILLISECONDS);
    }
}
