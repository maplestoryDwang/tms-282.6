package net.swordie.ms.connection.packet;

import net.swordie.ms.client.character.skills.info.AttackInfo;
import net.swordie.ms.client.character.skills.info.MobAttackInfo;
import net.swordie.ms.connection.protocol.codec.Tms282MobLocator;
import net.swordie.ms.handlers.header.OutHeader;
import net.swordie.ms.util.Position;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * CP02E8 SUMMONED_ATTACK (265 0x2DA), static layout of the sender 44DF2FC (constructor site; the mob loop ends in
 * the attack-info writer 5956260, which jumps into the Themida VM):
 * <pre>
 * i32 summonObjectId, i32 extra (282 new, id or -1), i32 updateTime, i32 skillId, i32 specialSkillId
 * bonus 136B950: u8 u8 u8, i32 n (&lt;= 10), n x (i32, i32), str, i32, u8, i32 m, m x i32
 *                (265 summonAttackBonusReader reads one more i32 before the string and no trailing list)
 * u8 left&lt;&lt;7|action, u8 mobCount&lt;&lt;4|hits, u8 0, i16 x, i16 y, i16 x2, i16 y2, u8 0, i32 -1, i16, i32, i32
 * per mob: i32 objectId, i32 templateId, u8 hitAction, u8 left, u8, u8 forceActionAndLeft, u8 frameIdx,
 *          i32 templateId, u8 calcDamageStatIndex, i16 hitX, i16 hitY, i16 oldX, i16 oldY, i32, i16, i32, i32, u8,
 *          i32 (282 new), hits x i64 damage, i32 mobUpDownYRange, attack info (VM part, variable)
 * </pre>
 * The skill-specific fields 265 reads between the header and the mobs (Mighty Mascot, Broadside, grenade position)
 * are not in the static part of the sender, so every mob block is found by its anchor (object id, template id at
 * +4 and again at +13; {@link Tms282MobLocator.MobDirectory}) from the end of the fixed header, and the block of a
 * mob that is no longer in the field is recognised by the two equal templates of a known mob.
 *
 * @param mobs the mob blocks read (a block that could not be found ends the list)
 */
public record Tms282SummonAttackRequest(int summonObjectId, int extra, int updateTime, int skillId,
                                        int specialSkillId, boolean left, int action, int mobCount, int hits,
                                        Position position, List<MobAttackInfo> mobs, List<String> notes) {
    public static final int OPCODE = 0x02E8;
    /** Mob block up to the damages: id, template, 5 bytes, template, u8, 4 x i16, i32, i16, i32, i32, u8, i32. */
    static final int BLOCK_HEAD = 4 + 4 + 5 + 4 + 1 + 8 + 4 + 2 + 4 + 4 + 1 + 4;
    static final int MAX_BONUS = 10;

    public Tms282SummonAttackRequest {
        mobs = List.copyOf(mobs);
        notes = List.copyOf(notes);
    }

    public static Tms282SummonAttackRequest parse(byte[] packet, Charset charset, Tms282MobLocator.MobDirectory field) {
        Objects.requireNonNull(field, "field");
        Tms282FieldRequestReader r = Tms282FieldRequestReader.open(packet, OPCODE, "CP02E8");
        int summon = r.i32();
        int extra = r.i32();
        int updateTime = r.i32();
        int skillId = r.i32();
        int special = r.i32();
        r.u8();
        r.u8();
        r.u8();
        int n = r.i32();
        if (n < 0 || n > MAX_BONUS) {
            throw new IllegalArgumentException("CP02E8 bonus count " + n);
        }
        for (int i = 0; i < n; i++) {
            r.i32();
            r.i32();
        }
        r.str(charset);
        r.i32();
        r.u8();
        int m = r.i32();
        if (m < 0 || m * 4L > r.remaining()) {
            throw new IllegalArgumentException("CP02E8 trailing bonus list " + m);
        }
        for (int i = 0; i < m; i++) {
            r.i32();
        }
        int leftAndAction = r.u8();
        int mask = r.u8();
        r.u8();
        Position pos = new Position(r.i16(), r.i16());
        r.i16();
        r.i16();
        r.u8();
        r.i32();
        r.i16();
        r.i32();
        r.i32();
        int hits = mask & 0xF;
        int mobCount = (mask >>> 4) & 0xF;
        List<MobAttackInfo> mobs = new ArrayList<>();
        List<String> notes = new ArrayList<>();
        int from = r.position();
        byte[] data = r.packet();
        for (int i = 0; i < mobCount; i++) {
            int at = locate(data, from, field, hits);
            if (at < 0) {
                notes.add("mob#" + i + " of " + mobCount + " not found after " + from);
                break;
            }
            r.seek(at);
            MobAttackInfo mai = readBlock(r, hits);
            mobs.add(mai);
            notes.add("mob#" + i + " id=" + mai.mobId + " at " + at);
            from = r.position();
        }
        return new Tms282SummonAttackRequest(summon, extra, updateTime, skillId, special, (leftAndAction & 0x80) != 0,
                leftAndAction & 0x7F, mobCount, hits, pos, mobs, notes);
    }

    static MobAttackInfo readBlock(Tms282FieldRequestReader r, int hits) {
        MobAttackInfo mai = new MobAttackInfo();
        mai.mobId = r.i32();
        mai.templateID = r.i32();
        mai.hitAction = (byte) r.u8();
        mai.left = (byte) r.u8();
        mai.idk3 = (byte) r.u8();
        mai.forceActionAndLeft = (byte) r.u8();
        mai.frameIdx = (byte) r.u8();
        mai.templateID = r.i32();
        mai.calcDamageStatIndexAndDoomed = (byte) r.u8();
        mai.hitX = r.i16();
        mai.hitY = r.i16();
        mai.oldPosX = r.i16();
        mai.oldPosY = r.i16();
        r.i32();
        mai.idk6 = r.i16();
        mai.idk7 = r.i32();
        mai.idk8 = r.i32();
        mai.idk9 = (byte) r.u8();
        mai.idkInt = r.i32();
        mai.damages = new long[hits];
        for (int j = 0; j < hits; j++) {
            mai.damages[j] = r.i64();
        }
        mai.mobUpDownYRange = r.i32();
        return mai;
    }

    /** First offset &gt;= {@code from} holding a mob block of the field (or of a known template that left it). */
    static int locate(byte[] data, int from, Tms282MobLocator.MobDirectory field, int hits) {
        int need = BLOCK_HEAD + hits * 8 + 4;
        for (int at = from; at + need <= data.length; at++) {
            int id = intAt(data, at);
            int template = intAt(data, at + 4);
            if (template <= 0 || template != intAt(data, at + 13)) {
                continue;
            }
            int live = field.templateOf(id);
            if (live == template || (live < 0 && at == from && field.isKnownTemplate(template))) {
                return at;
            }
        }
        return -1;
    }

    static int intAt(byte[] b, int at) {
        return (b[at] & 0xFF) | (b[at + 1] & 0xFF) << 8 | (b[at + 2] & 0xFF) << 16 | (b[at + 3] & 0xFF) << 24;
    }

    /** The 265 {@link AttackInfo} of 265 AttackHandler.handleSummonedAttack (summon and skill info set by the caller). */
    public AttackInfo toAttackInfo() {
        AttackInfo ai = new AttackInfo();
        ai.attackHeader = OutHeader.SUMMONED_ATTACK;
        ai.updateTime = updateTime;
        ai.skillId = skillId;
        ai.summonSpecialSkillId = specialSkillId;
        ai.attackActionType = (byte) action;
        ai.left = left;
        ai.hits = (byte) hits;
        ai.mobCount = mobs.size();
        ai.pos = position;
        long total = 0;
        for (MobAttackInfo mai : mobs) {
            for (long d : mai.damages) {
                total += d;
            }
            ai.mobAttackInfo.add(mai);
        }
        ai.totalDamageDealt = total;
        return ai;
    }

    @Override
    public String toString() {
        return "CP02E8 summon " + summonObjectId + " skill " + skillId + "/" + specialSkillId + " extra " + extra
                + " hits " + hits + " mobs " + mobs.size() + "/" + mobCount;
    }
}
