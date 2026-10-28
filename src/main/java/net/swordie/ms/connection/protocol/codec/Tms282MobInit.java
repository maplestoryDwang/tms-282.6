package net.swordie.ms.connection.protocol.codec;

import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.life.mob.Mob;
import net.swordie.ms.life.mob.skill.ShootingMoveStat;
import net.swordie.ms.util.Position;
import net.swordie.ms.util.container.Tuple;
import net.swordie.ms.world.field.Foothold;

import java.util.ArrayList;
import java.util.List;

/**
 * TMS 282.3 CMob::Init body (base CMob 0x3E65880, mob-enter-field-282.md section 2, 44 fields). Values come from
 * the 265 {@link Mob}; fields 282 added or whose meaning is unknown are sent as 0 / empty, as section 2.1 advises.
 * Differences from 265 {@code Mob.encodeInit}: appearType is a short (#8); the int group before refImgMobID has
 * three ints, not four (#15..#17); #28 is a count of 5-int entries (265 wrote two ints); #33..#44 follow 282.
 * <p>
 * #45 (zakum-0930, research/tms2823/zakum-crash-0930/README.md): #44 is read through CMob vtbl+0x148. Mobs whose
 * CMob subclass overrides it with 437FEE0 (the hit-part / skeleton bosses: Zakum body and arms of every difficulty,
 * 8881000, 8240130/47/49/50, 9410418..20, and every skeleton+boss template with a HitParts node) read one more
 * byte, the hit-part count, and for each entry {@code Str name} (+ {@code B stage, I value} when the name is one of
 * the template's parts). Without it the client throws at 43805E0 (0 bytes remain), reports CP00AE and closes the
 * connection: the Zakum altar (280030000) crash of 2026-09-30. We send count 0 (the client keeps the template's
 * parts).
 */
public final class Tms282MobInit {
    private Tms282MobInit() {
    }

    /** banban boss check 0xEAF850 (same ids as 265 is_banban_boss). */
    static boolean isBanban(int templateId) {
        return templateId == 8910000 || templateId == 8910100 || templateId == 9990033;
    }

    /**
     * Templates whose CMob reads the #45 hit-part count (vtbl+0x148 -> 437FEE0). CMob factory 3FB5010: 8881000
     * (20D8320), 8623E0 = 8800002/8800022/8800102/9834726 (43856F0), 8622F0 = 862210 (8800003..10, 8800023..30,
     * 8800103..110) + 862170 (9834727..34) + 8800141..48 (4387DD0), 8240130/8240149/8240150 (4384B40), 8240147
     * (20D7CA0), template id 9410418..20 (6159510); otherwise template skeleton (+0xA6) and boss (+0xA1) and a
     * non-empty HitParts list (ED8BC0) -> 437FA00. The last group is every mob with a HitParts node in the 265 +
     * 282 Mob.wz (all of them skeleton + boss); 9300565..73 link to 8800002..10 (assumed: the linked img's parts).
     */
    static final java.util.Set<Integer> HIT_PART_MOBS = hitPartMobs();

    private static java.util.Set<Integer> hitPartMobs() {
        java.util.Set<Integer> ids = new java.util.HashSet<>();
        int[][] ranges = {
                {8240130, 8240130}, {8240147, 8240147}, {8240149, 8240150},
                {8800002, 8800010}, {8800022, 8800030}, {8800102, 8800110}, {8800141, 8800148},
                {8881000, 8881000},
                {9300565, 9300573},
                {9400075, 9400076}, {9400080, 9400080}, {9400086, 9400086},
                {9402197, 9402197}, {9402204, 9402204}, {9402270, 9402281}, {9402370, 9402380},
                {9410418, 9410420}, {9410866, 9410868},
                {9602123, 9602131}, {9602142, 9602150}, {9602162, 9602170}, {9602201, 9602208},
                {9834726, 9834734},
        };
        for (int[] r : ranges) {
            for (int id = r[0]; id <= r[1]; id++) {
                ids.add(id);
            }
        }
        return java.util.Collections.unmodifiableSet(ids);
    }

    /** #45 is sent for {@link #HIT_PART_MOBS}; {@code -Dtms282.mob.hitParts.enabled=false} restores the old body. */
    static boolean readsHitParts(int templateId) {
        return HIT_PART_MOBS.contains(templateId)
                && !"false".equalsIgnoreCase(System.getProperty("tms282.mob.hitParts.enabled", "true"));
    }

    /** Template check 0xED6380 for the targetFromSvr int (#32); 265 used these two ids. */
    static boolean hasTargetFromSvr(int templateId) {
        return templateId == 8880102 || templateId == 8880605;
    }

    public static void encode(OutPacket out, Mob mob) {
        int templateId = mob.getTemplateId();
        Position pos = mob.getPosition() != null ? mob.getPosition() : new Position(0, 0);
        out.encodePosition(pos);                                   // 1, 2 x y
        out.encodeByte(mob.getMoveAction());                       // 3
        if (isBanban(templateId)) {
            out.encodeByte(0);                                     // 4
        }
        out.encodeShort(footholdId(mob.getCurFoodhold()));         // 5
        out.encodeShort(footholdId(mob.getHomeFoothold()));        // 6
        out.encodeByte(0);                                         // 7 (-> +0x135C)
        short appearType = mob.getAppearType();
        out.encodeShort(appearType);                               // 8 appearType is S in 282
        if (appearType == -3 || appearType == -6 || appearType >= 0) {
            out.encodeInt(mob.getOption());                        // 9
        }
        out.encodeByte(mob.getTeamForMCarnival());                 // 10
        out.encodeInt(mob.getScale());                             // 11 (-> +0xAA8)
        out.encodeLong(mob.getHp());                               // 12
        out.encodeInt(mob.getEffectItemID());                      // 13
        if (mob.isPatrolMob()) {                                   // 14 template[+0x12E]
            out.encodeInt(pos.getX() - mob.getRange());
            out.encodeInt(pos.getX() + mob.getRange());
            out.encodeInt(mob.getDetectX());
            out.encodeInt(mob.getSenseX());
        }
        out.encodeInt(0);                                          // 15 (-> +0xC90)
        out.encodeInt(mob.getPhase());                             // 16 (-> +0xC94)
        out.encodeInt(mob.getRefImgMobID());                       // 17 (265 curZoneDataType before it is gone)
        out.encodeInt(mob.getAfterAttack());                       // 18
        out.encodeInt(mob.getCurrentAction());                     // 19
        out.encodeInt(0);                                          // 20
        out.encodeByte(0);                                         // 21 (265: random byte)
        out.encodeByte(0);                                         // 22 flag (!= 0 -> I)
        out.encodeByte(0);                                         // 23 (-> +0x1370)
        out.encodeInt(0);                                          // 24 n x (I, I)
        out.encodeInt(mob.getScale());                             // 25 (-> +0xF20)
        out.encodeInt(mob.getEliteGrade());                        // 26
        if (mob.getEliteGrade() >= 0) {
            List<Tuple<Integer, Integer>> skills = mob.getEliteSkills() != null ? mob.getEliteSkills() : List.of();
            out.encodeInt(skills.size());
            for (Tuple<Integer, Integer> s : skills) {
                out.encodeInt(s.getLeft());
                out.encodeInt(s.getRight());
            }
            out.encodeInt(mob.getEliteType());
        }
        ShootingMoveStat sms = mob.getShootingMoveStat();
        out.encodeByte(sms != null);                               // 27
        if (sms != null) {
            sms.encode(out);
        }
        out.encodeInt(0);                                          // 28 Buf4 count of 5-int entries
        List<Integer> linked = linkedMobs(mob);                    // 29 n x (I type, I objectId)
        out.encodeInt(linked.size());
        for (int objectId : linked) {
            out.encodeInt(5);
            out.encodeInt(objectId);
        }
        out.encodeByte(0);                                         // 30 flag (!= 0 -> Buf480)
        out.encodeString("");                                      // 31
        if (hasTargetFromSvr(templateId)) {
            out.encodeInt(mob.getTargetFromSvr());                 // 32
        }
        out.encodeInt(0);                                          // 33 OnAttackBlock count
        out.encodeInt(0);                                          // 34
        out.encodeByte(0);                                         // 35 flag (!= 0 -> 8 I + L)
        out.encodeByte(0);                                         // 36 (-> +0x11E4)
        out.encodeInt(0);                                          // 37 (-> +0x11E8)
        out.encodeInt(0);                                          // 38 (!= 0 -> I)
        out.encodeInt(0);                                          // 39 (!= 0 -> I I)
        out.encodeInt(0);                                          // 40 a
        out.encodeInt(0);                                          //    b
        out.encodeInt(0);                                          //    c
        out.encodeInt(0);                                          // 41 n x I
        out.encodeInt(0);                                          // 42 (controller related, meaning unknown)
        out.encodeInt(0);                                          // 43 (-> +0x1544)
        out.encodeString("");                                      // 44 (vtbl+0x148, read and dropped)
        if (readsHitParts(templateId)) {
            out.encodeByte(0);                                     // 45 hit-part count (437FEE0 at 43805E0)
        }
    }

    private static int footholdId(Foothold fh) {
        return fh != null ? fh.getId() : 0;
    }

    /** 265 links Lucid's butterflies (8880102) to 8880101/8880111 as type 5. */
    private static List<Integer> linkedMobs(Mob mob) {
        List<Integer> list = new ArrayList<>();
        int templateId = mob.getTemplateId();
        if ((templateId == 8880101 || templateId == 8880111) && mob.getField() != null) {
            for (Mob m : mob.getField().getMobs()) {
                if (m.getTemplateId() == 8880102) {
                    list.add(m.getObjectId());
                }
            }
        }
        return list;
    }
}
