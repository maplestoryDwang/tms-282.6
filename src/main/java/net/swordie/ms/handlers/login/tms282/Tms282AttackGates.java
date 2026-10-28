package net.swordie.ms.handlers.login.tms282;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.protocol.ProtocolConfig;
import net.swordie.ms.handlers.user.AttackGatePolicy;
import net.swordie.ms.life.mob.Mob;
import net.swordie.ms.life.mob.TrainingDummies;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TMS 282.3 {@link AttackGatePolicy}. The 282 client computes damage with its own (282) skill data; the server's 265
 * SkillData differs for 2193 skills (packet-audit/data282-diff-skills.tsv: 423 in cooltime/time/mpCon), so a 265 gate
 * would drop damage the client already showed and the mob would not lose HP. Hence, following the
 * {@code tms282.combat.antiCheat} mode of {@link Tms282CombatGuard}:
 * <ul>
 *     <li>{@code log} (default): a failed 265 gate is logged (at most once per character/skill/gate every 30 s) and
 *     the damage is applied;</li>
 *     <li>{@code drop}: the gate is enforced like 265.</li>
 * </ul>
 * {@code tms282.combat.gmOneHitKill} (default {@code false}): 265 raises every GM hit to the mob's whole HP, which hides
 * the real damage while testing; on 282 it is opt-in, and training dummies ({@link TrainingDummies}) are never
 * one-hit killed.
 */
public final class Tms282AttackGates implements AttackGatePolicy {
    public static final String KEY_GM_ONE_HIT_KILL = "tms282.combat.gmOneHitKill";
    private static final long LOG_EVERY_MS = 30_000;

    private final boolean enforce;
    private final boolean gmOneHitKill;
    private final Map<String, Long> lastLog = new ConcurrentHashMap<>();

    public Tms282AttackGates(boolean enforce, boolean gmOneHitKill) {
        this.enforce = enforce;
        this.gmOneHitKill = gmOneHitKill;
    }

    /** From config/server.properties (-D wins): the combat anti-cheat mode and the GM one-hit switch. */
    public static Tms282AttackGates load() {
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

    public static Tms282AttackGates fromProperties(Properties file) {
        Tms282CombatSettings combat = Tms282CombatSettings.fromProperties(file);
        String gm = System.getProperty(KEY_GM_ONE_HIT_KILL, file.getProperty(KEY_GM_ONE_HIT_KILL, "false"));
        return new Tms282AttackGates(combat.antiCheat() == Tms282CombatSettings.AntiCheatMode.DROP,
                Boolean.parseBoolean(gm.trim().toLowerCase(Locale.ROOT)));
    }

    public boolean enforcing() {
        return enforce;
    }

    public boolean gmOneHitKillEnabled() {
        return gmOneHitKill;
    }

    @Override
    public boolean enforce(Char chr, int skillId, Gate gate) {
        if (enforce) {
            return true;
        }
        String key = (chr == null ? 0 : chr.getId()) + "/" + skillId + "/" + gate;
        long now = System.currentTimeMillis();
        Long last = lastLog.get(key);
        if (last == null || now - last >= LOG_EVERY_MS) {
            lastLog.put(key, now);
            System.out.printf("[TMS282] COMBAT GATE %s failed for skill %d character %d (265 SkillData); damage applied"
                    + " (tms282.combat.antiCheat=log)%n", gate, skillId, chr == null ? 0 : chr.getId());
        }
        return false;
    }

    @Override
    public boolean gmOneHitKill(Char chr, Mob mob) {
        return (gmOneHitKill || net.swordie.ms.client.character.tms282.Tms282GmToggles.oneHitKill(chr))
                && (mob == null || !TrainingDummies.isDummy(mob.getTemplateId()));
    }
}
