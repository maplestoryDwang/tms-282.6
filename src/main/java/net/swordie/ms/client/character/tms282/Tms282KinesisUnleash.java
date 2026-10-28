package net.swordie.ms.client.character.tms282;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.packet.Tms282SkillPackets;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 282 凱內西斯 釋放 (142121003): while it lasts the 終極技 use {@code unleashCooltime} instead of {@code cooltime}
 * (282 Skill.wz, not loaded into SkillStat). The client shortens its own cooldowns; the server did not, so it
 * refused the attacks (Char.checkAndSetSkillCooltime) and sent the full cooldown back (Kinesis audit 2026-09-29).
 * The values below are every {@code unleashCooltime} of the 282 data (seconds).
 */
public final class Tms282KinesisUnleash {
    public static final int UNLEASH = 142121003;
    public static final Map<Integer, Integer> UNLEASH_COOLTIME = Map.of(
            142100001, 2, 142140001, 2,
            142111001, 10, 142121001, 10, 142121002, 10,
            142141002, 10, 142141003, 10, 142141007, 10,
            400021161, 5);

    private static final Map<Integer, Long> ACTIVE_UNTIL = new ConcurrentHashMap<>();

    private Tms282KinesisUnleash() {
    }

    public static void started(Char chr, int durationMillis, long now) {
        if (durationMillis > 0) {
            ACTIVE_UNTIL.put(chr.getId(), now + durationMillis);
        }
    }

    public static boolean active(Char chr, long now) {
        Long until = ACTIVE_UNTIL.get(chr.getId());
        return until != null && until > now;
    }

    /** The cooldown to use for {@code skillId} (seconds). */
    public static int cooldownSeconds(Char chr, int skillId, int normalSeconds, long now) {
        Integer unleash = UNLEASH_COOLTIME.get(skillId);
        return unleash != null && normalSeconds > unleash && active(chr, now) ? unleash : normalSeconds;
    }

    /** After an attack: replace the cooldown the 265 attack path just stored by the unleash one. */
    public static void afterAttack(Char chr, int skillId, long now) {
        Integer unleash = UNLEASH_COOLTIME.get(skillId);
        if (unleash == null || !active(chr, now)) {
            return;
        }
        chr.addSkillCoolTime(skillId, now + unleash * 1000L);
        chr.write(Tms282SkillPackets.skillCooltimeSet(skillId, unleash));
    }
}
