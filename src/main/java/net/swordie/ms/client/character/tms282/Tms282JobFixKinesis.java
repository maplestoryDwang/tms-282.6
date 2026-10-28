package net.swordie.ms.client.character.tms282;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.skills.info.SkillInfo;
import net.swordie.ms.connection.protocol.Protocol;
import net.swordie.ms.connection.protocol.ProtocolVersion;

/**
 * TMS 282.3 Kinesis skills that are new or changed on 282 (job-fixes-0929, 282 Skill.wz 14211 / 14212).
 * Called from {@code Kinesis.handleSkill} after {@code Job.handleSkill} ran (which already applied
 * {@code applyNdTime}: {@code ndTime > 0} -> IndieNotDamaged for ndTime + 1000 ms, a supported 282 INDIE stat).
 * <ul>
 * <li>142121004 異界祝禱: {@code psd = 1}, {@code basicStatUp = u(x/2)}, no {@code time}. All 52 楓葉祝福-family
 * skills in the 282 data have exactly this shape (psd, basicStatUp, no time): on 282 it is a passive the client
 * applies from the skill record. A timed/permanent BasicStatUp CTS on use would add the same percentage a second
 * time, so none is sent; the 265 破魂 attack case (IndiePMdR stacks) is skipped in handleAttack.</li>
 * <li>142111005 心靈遊動: {@code mpCon, subTime 200+200x, u 2000, v 1800, w 30, cooltime 2, excl 300} and no stat
 * key; the 282-only CTS the client would need (KinesisTeleport 918) has no 282 encoder (supported=0), so the use
 * is accepted (cost / cooldown by the service) and nothing else is sent.</li>
 * <li>142121041 永恆壞滅: {@code ndTime 10000} is handled by {@code Job.applyNdTime} (IndieNotDamaged 11 s);
 * {@code x = 180}, {@code dummyStr = 10}, {@code processtype 6} have no server-side meaning known without the
 * official packets.</li>
 * </ul>
 */
public final class Tms282JobFixKinesis {
    public static final int OTHERWORLD_BLESSING = 142121004;
    public static final int PSYCHIC_GLIDE = 142111005;
    public static final int ETERNAL_DESTRUCTION = 142121041;

    private Tms282JobFixKinesis() {
    }

    /** True when the 282 rule for this skill is complete (the 265 switch must not run). False outside 282.3. */
    public static boolean handled282(Char chr, int skillId, SkillInfo si, int slv) {
        if (Protocol.active().version() != ProtocolVersion.TMS_282_3) {
            return false;
        }
        return switch (skillId) {
            case OTHERWORLD_BLESSING, PSYCHIC_GLIDE, ETERNAL_DESTRUCTION -> true;
            default -> false;
        };
    }
}
