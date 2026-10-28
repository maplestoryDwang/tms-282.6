package net.swordie.ms.client.character.tms282;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.life.mob.Mob;
import net.swordie.ms.util.Position;
import net.swordie.ms.util.Rect;

/**
 * TMS 282.3 Adele rules that differ from 265 (job-fixes-0929). Pure data / selection helpers; the job handler
 * ({@code Adele}) calls them only under the 282.3 protocol.
 * <ul>
 * <li>奇蹟 151100013 / 奇蹟VI 151140003 are passives on 282 (type 51, psd) with no CTS: an attack with a
 * {@code skillList} skill triggers 魔法派遣 once every {@code x} seconds (282 Skill.wz 15110 {@code x = 8},
 * 15114 {@code x = 8-d(x/15)}; the 265 active 151101013 with LWWonder is gone).</li>
 * <li>追蹤 151111003 / 追蹤VI 151141004: the 282 CP04B4 tail is not the 265 target list + position, so the server
 * picks the target (nearest live mob in the skill rect) and spawns at the character.</li>
 * </ul>
 */
public final class Tms282JobFixAdele {
    public static final int WONDER = 151100013;
    public static final int HEXA_WONDER = 151140003;

    /** 282 Skill.wz 15110/151100013 and 15114/151140003 {@code skillList} (identical). */
    private static final int[] WONDER_TRIGGERS = {
            151101000, 151111000, 151121000, 151121002, 151141000, 151141001, 151141005
    };

    private Tms282JobFixAdele() {
    }

    public static boolean wonderTrigger(int skillId) {
        for (int id : WONDER_TRIGGERS) {
            if (id == skillId) {
                return true;
            }
        }
        return false;
    }

    /** The Wonder skill the character has on 282 (HEXA first), or 0. */
    public static int wonderSkill(Char chr) {
        if (chr.getSkillLevel(HEXA_WONDER) > 0) {
            return HEXA_WONDER;
        }
        return chr.getSkillLevel(WONDER) > 0 ? WONDER : 0;
    }

    /** {@code x} seconds of the 282 data as milliseconds: 8 s; HEXA {@code 8-d(x/15)} (level 15: 7 s, 30: 6 s). */
    public static long wonderCooldownMillis(int wonderSkillId, int level) {
        int x = wonderSkillId == HEXA_WONDER ? 8 - Math.max(0, level) / 15 : 8;
        return Math.max(1, x) * 1000L;
    }

    /** Nearest live mob to {@code from} in {@code rect}, or null. */
    public static Mob nearestMob(Iterable<Mob> mobs, Position from) {
        Mob best = null;
        long bestD = Long.MAX_VALUE;
        for (Mob m : mobs) {
            if (m == null || m.getHp() <= 0 || m.getPosition() == null) {
                continue;
            }
            long dx = m.getPosition().getX() - from.getX();
            long dy = m.getPosition().getY() - from.getY();
            long d = dx * dx + dy * dy;
            if (d < bestD) {
                bestD = d;
                best = m;
            }
        }
        return best;
    }

    /** The character-relative search rect of 追蹤 (282 lt -500,-250 rb 500,170), facing-aware. */
    public static Rect decreeRect(Char chr, Rect skillRect) {
        Rect base = skillRect != null && skillRect.getWidth() > 0 ? skillRect : new Rect(-500, -250, 500, 170);
        Rect r = chr.getRectAround(base);
        if (!chr.isLeft()) {
            r = r.horizontalFlipAround(chr.getPosition().getX());
        }
        return r;
    }

    /** Spawn point: the character's position plus the atom's first extraPos offset (0,0 when absent). */
    public static Position spawnPosition(Position chrPos, Position extraPos0) {
        int x = chrPos == null ? 0 : chrPos.getX();
        int y = chrPos == null ? 0 : chrPos.getY();
        if (extraPos0 != null) {
            x += extraPos0.getX();
            y += extraPos0.getY();
        }
        return new Position(x, y);
    }
}
