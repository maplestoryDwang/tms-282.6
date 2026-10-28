package net.swordie.ms.handlers.login.tms282;

import net.swordie.ms.connection.protocol.ProtocolConfig;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Properties;

/**
 * Settings of the TMS 282.3 skill system, from {@code config/server.properties}; a JVM {@code -D} property with the
 * same key wins.
 * <ul>
 *     <li>{@code tms282.skillEffects265} (default {@code true}): after an accepted CP04B4 the 265 job handler's
 *     {@code handleSkill} runs for the skill's server-side effects (summons, affected areas, heals, mob
 *     debuffs, job resources ...; {@link Tms282SkillEffects}). {@code false} keeps the overlay's buff-only
 *     service.</li>
 * </ul>
 *
 * @param skillEffects265 run the 265 job handler after an accepted skill use
 */
public record Tms282SkillSettings(boolean skillEffects265) {
    public static final String KEY_SKILL_EFFECTS_265 = "tms282.skillEffects265";
    public static final Tms282SkillSettings DEFAULTS = new Tms282SkillSettings(true);

    /** Reads {@link ProtocolConfig#CONFIG_FILE} (when present) plus system properties. */
    public static Tms282SkillSettings load() {
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

    public static Tms282SkillSettings fromProperties(Properties file) {
        String v = System.getProperty(KEY_SKILL_EFFECTS_265);
        if (v == null) {
            v = file.getProperty(KEY_SKILL_EFFECTS_265);
        }
        return new Tms282SkillSettings(v == null || v.isBlank() || Boolean.parseBoolean(v.trim()));
    }
}
