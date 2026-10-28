package net.swordie.ms.handlers.login.tms282;

import net.swordie.ms.connection.protocol.ProtocolConfig;
import net.swordie.ms.connection.protocol.codec.Tms282AttackLayout;
import net.swordie.ms.connection.protocol.codec.Tms282MobLocator;
import net.swordie.ms.constants.GameConstants;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Locale;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.Properties;

/**
 * Settings of the TMS 282.3 combat overlay ({@link Tms282CombatHandlers}), from {@code config/server.properties};
 * a JVM {@code -D} property with the same key wins.
 * <ul>
 *     <li>{@code tms282.attackMobLocator}: {@code anchored} (default: a mob block is found by its field object id +
 *     template id when the static layout misses it) or {@code sequential} (trust the static layout only);</li>
 *     <li>{@code tms282.combat.antiCheat}: {@code log} (default: findings are logged, the attack is applied) or
 *     {@code drop} (an attack with a blocking finding -- 265 exploit skill, damage out of range -- is not applied).
 *     Never disconnects and never bans: the 265 checks use 265 data and would misjudge 282 characters;</li>
 *     <li>{@code tms282.combat.maxHitDamage}: per-hit damage limit of the check, default 265 {@code DAMAGE_CAP};</li>
 *     <li>{@code tms282.combat.unknownSkill}: {@code apply} (default: a skill id without 265 data is logged and
 *     its damage applied, as for skill 0) or {@code drop};</li>
 *     <li>{@code tms282.dropPickUpOp}: the DROP_PICK_UP_REQUEST opcode (hex {@code 0x054A} or decimal). Unset by
 *     default: the opcode is inside the client VM and has not been captured, so no handler is registered.</li>
 * </ul>
 */
public record Tms282CombatSettings(Tms282MobLocator attackMobLocator, AntiCheatMode antiCheat, long maxHitDamage,
                                   boolean applyUnknownSkill, OptionalInt dropPickUpOp) {
    public static final String KEY_ATTACK_MOB_LOCATOR = "tms282.attackMobLocator";
    public static final String KEY_ANTI_CHEAT = "tms282.combat.antiCheat";
    public static final String KEY_MAX_HIT_DAMAGE = "tms282.combat.maxHitDamage";
    public static final String KEY_UNKNOWN_SKILL = "tms282.combat.unknownSkill";
    public static final String KEY_DROP_PICK_UP_OP = "tms282.dropPickUpOp";

    /** What the combat check does with a blocking finding. */
    public enum AntiCheatMode {
        /** Log only; the attack is applied. */
        LOG,
        /** Log and do not apply the attack (the connection stays open). */
        DROP
    }

    public Tms282CombatSettings {
        Objects.requireNonNull(attackMobLocator, "attackMobLocator");
        Objects.requireNonNull(antiCheat, "antiCheat");
        Objects.requireNonNull(dropPickUpOp, "dropPickUpOp");
        if (maxHitDamage <= 0) {
            throw new IllegalArgumentException("maxHitDamage must be positive");
        }
        dropPickUpOp.ifPresent(op -> {
            if (op <= 0 || op > 0xFFFF) {
                throw new IllegalArgumentException("dropPickUpOp out of range: " + op);
            }
        });
    }

    public static Tms282CombatSettings load() {
        Properties p = new Properties();
        if (Files.isRegularFile(ProtocolConfig.CONFIG_FILE)) {
            try (InputStream in = Files.newInputStream(ProtocolConfig.CONFIG_FILE)) {
                p.load(in);
            } catch (IOException e) {
                throw new IllegalStateException("Cannot read " + ProtocolConfig.CONFIG_FILE, e);
            }
        }
        return fromProperties(p);
    }

    public static Tms282CombatSettings fromProperties(Properties file) {
        String op = value(file, KEY_DROP_PICK_UP_OP, null);
        String unknown = value(file, KEY_UNKNOWN_SKILL, "apply").toLowerCase(Locale.ROOT);
        if (!unknown.equals("apply") && !unknown.equals("drop")) {
            throw new IllegalArgumentException(KEY_UNKNOWN_SKILL + " must be apply or drop, got " + unknown);
        }
        return new Tms282CombatSettings(
                Tms282MobLocator.parse(value(file, KEY_ATTACK_MOB_LOCATOR, null)),
                AntiCheatMode.valueOf(value(file, KEY_ANTI_CHEAT, "log").toUpperCase(Locale.ROOT)),
                Long.parseLong(value(file, KEY_MAX_HIT_DAMAGE, String.valueOf(GameConstants.DAMAGE_CAP))),
                unknown.equals("apply"),
                op == null ? OptionalInt.empty() : OptionalInt.of(parseOpcode(op)));
    }

    /** Defaults only (no config file); checks use it. */
    public static Tms282CombatSettings defaults() {
        return fromProperties(new Properties());
    }

    public Tms282CombatSettings withDropPickUpOp(int op) {
        return new Tms282CombatSettings(attackMobLocator, antiCheat, maxHitDamage, applyUnknownSkill, OptionalInt.of(op));
    }

    public Tms282CombatSettings withAntiCheat(AntiCheatMode mode) {
        return new Tms282CombatSettings(attackMobLocator, mode, maxHitDamage, applyUnknownSkill, dropPickUpOp);
    }

    public Tms282CombatSettings withMaxHitDamage(long max) {
        return new Tms282CombatSettings(attackMobLocator, antiCheat, max, applyUnknownSkill, dropPickUpOp);
    }

    public Tms282CombatSettings withAttackMobLocator(Tms282MobLocator locator) {
        return new Tms282CombatSettings(locator, antiCheat, maxHitDamage, applyUnknownSkill, dropPickUpOp);
    }

    /** The standard attack layout with this configuration's mob locator. */
    public Tms282AttackLayout attackLayout() {
        return Tms282AttackLayout.STANDARD.withMobLocator(attackMobLocator);
    }

    static int parseOpcode(String s) {
        String t = s.trim().toLowerCase(Locale.ROOT);
        return t.startsWith("0x") ? Integer.parseInt(t.substring(2), 16) : Integer.parseInt(t);
    }

    private static String value(Properties file, String key, String fallback) {
        String v = System.getProperty(key);
        if (v == null) {
            v = file.getProperty(key);
        }
        return v == null || v.isBlank() ? fallback : v.trim();
    }
}
