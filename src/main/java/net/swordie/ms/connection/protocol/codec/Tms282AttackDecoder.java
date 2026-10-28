package net.swordie.ms.connection.protocol.codec;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.skills.info.AttackInfo;
import net.swordie.ms.client.character.skills.info.MobAttackInfo;
import net.swordie.ms.connection.InPacket;
import net.swordie.ms.enums.BaseStat;
import net.swordie.ms.handlers.header.InHeader;
import net.swordie.ms.life.Life;
import net.swordie.ms.life.mob.Mob;
import net.swordie.ms.loaders.MobData;
import net.swordie.ms.loaders.SkillData;
import net.swordie.ms.util.Position;
import net.swordie.ms.world.field.Field;

import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * TMS 282.3 attack requests CP010D..CP0111 into the 265 {@link AttackInfo} (layout: {@link Tms282AttackLayout}).
 * Differences from 265 [verified]: one more u32 before skillId, one more u32 before each mob's damages. The rest is
 * read by replaceable strategies; the part the client builds in its VM is kept as {@link AttackRequest#unparsedTail}.
 * <p>
 * Safety: nothing is thrown to the caller. A request that cannot be read (too short, a list count out of range, a
 * mob block that cannot be found) is reported through the {@code reporter} and dropped ({@link Optional#empty()});
 * the connection stays open. The unknown-skill and damage checks are not the decoder's job
 * ({@code Tms282CombatGuard}): a skill id without 265 data decodes with {@code skillInfo == null}.
 */
public final class Tms282AttackDecoder implements AttackRequestDecoder {
    /** Bytes of the unparsed tail echoed in a log line. */
    static final int LOG_TAIL_BYTES = 48;

    private final Tms282AttackLayout layout;
    private final Consumer<String> reporter;

    public Tms282AttackDecoder(Tms282AttackLayout layout, Consumer<String> reporter) {
        this.layout = Objects.requireNonNull(layout, "layout");
        this.reporter = Objects.requireNonNull(reporter, "reporter");
    }

    public Tms282AttackDecoder(Tms282AttackLayout layout) {
        this(layout, System.out::println);
    }

    public Tms282AttackLayout layout() {
        return layout;
    }

    @Override
    public Optional<AttackRequest> decode(Char chr, InPacket in, InHeader header) {
        byte[] body = in.getUnreadAmount() > 0 ? in.decodeArr(in.getUnreadAmount()) : new byte[0];
        Optional<Tms282AttackKind> kind = Tms282AttackKind.ofHeader(header);
        if (kind.isEmpty()) {
            reporter.accept("[TMS282] REJECTED attack: " + header + " is not a 282 attack request");
            return Optional.empty();
        }
        try {
            AttackRequest request = parse(kind.get(), body, mobsOf(chr), chr.getPosition(), totalCriticalRate(chr));
            return Optional.of(request);
        } catch (RuntimeException e) {
            reporter.accept(String.format("[TMS282] REJECTED CP%04X len=%d character=%d: %s; attack dropped (%s)",
                    kind.get().op(), body.length, chr.getId(), e, preview(body, 0)));
            return Optional.empty();
        }
    }

    /**
     * Reads one request body (no opcode).
     *
     * @param mobs     the attacker's field mobs (anchors of the mob blocks)
     * @param position the attacker's position ({@code AttackInfo.charPosition})
     * @param totalCr  critical rate for the 265 critical estimate of each hit
     * @throws RuntimeException when the body cannot be read with this layout
     */
    public AttackRequest parse(Tms282AttackKind kind, byte[] body, Tms282MobLocator.MobDirectory mobs, Position position,
                               int totalCr) {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(body, "body");
        Objects.requireNonNull(mobs, "mobs");
        List<String> notes = new ArrayList<>();
        InPacket in = new InPacket(body);
        AttackInfo ai = new AttackInfo();
        ai.inHeader = kind.request();
        ai.attackHeader = kind.remote();
        if (position != null) {
            ai.charPosition = position;
        }
        in.decodeInt();
        in.decodeInt();
        if (kind.boxAttackByte()) {
            ai.boxAttack = in.decodeByte() != 0;
        }
        ai.fieldKey = in.decodeByte();
        byte mask = in.decodeByte();
        ai.hits = (byte) (mask & 0xF);
        ai.mobCount = (mask >>> 4) & 0xF;
        int extra = in.decodeInt();
        int requestedSkillId = in.decodeInt();
        ai.skillId = requestedSkillId;
        ai.slv = in.decodeInt();
        if (kind.addAttackProc()) {
            ai.addAttackProc = in.decodeByte();
        }
        if (ai.skillId == 12121055 || ai.skillId == 12141011)
            throw new IllegalArgumentException("retired Phoenix charge attack is absent from 282");
        ai.skillInfo = SkillData.getSkillInfoById(requestedSkillId);
        int headerEnd = (int) in.getPosition();
        notes.add("u32 before skillId=" + extra);

        int expected = -1;
        boolean reliable;
        try {
            for (Tms282AttackSection section : layout.beforeMobs()) {
                section.read(in, kind, ai);
            }
            expected = (int) in.getPosition();
            reliable = true;
        } catch (RuntimeException e) {
            reliable = false;
            // Anchor recovery can preserve damage blocks, but an incomplete prefix cannot authorize a process ID.
            if (ai.processType != null) ai.processType.nativeProcessId = 0;
            notes.add("static prefix stopped (" + e + ")");
        }

        if (ai.processType == null) {
            // the prefix sections did not get to the ProcessType: 265 UserRemote.attack reads processType.skillUseInfo
            // when the attack is broadcast (a null there aborts the whole 265 attack task, mob damage included)
            ai.processType = new net.swordie.ms.client.character.skills.ProcessType(
                    new net.swordie.ms.client.character.skills.info.SkillUseInfo(ai.skillInfo, ai.slv));
        }
        int cursor = reliable ? expected : headerEnd;
        // An anchor scan never trusts the static position: mob #0 is searched from the end of the verified header,
        // every later mob from the end of the previous mob's verified block.
        int searchFrom = headerEnd;
        long totalDamage = 0;
        for (int i = 0; i < ai.mobCount; i++) {
            Tms282MobLocator.Located at = layout.mobLocator().locate(body, reliable ? cursor : -1, reliable, searchFrom, mobs);
            if (i == 0 && ai.processType != null && ai.processType.nativeProcessId > 0 && at.offset() != cursor)
                ai.processType.nativeProcessId = 0; // A recovered first mob cannot prove the native process boundary.
            MobAttackInfo mai;
            try {
                if (!at.found()) {
                    throw new IllegalArgumentException("block not found after offset " + cursor + " with locator "
                            + layout.mobLocator());
                }
                in.seek(at.offset());
                mai = readMobBlock(in, kind, ai.hits);
            } catch (RuntimeException e) {
                if (i == 0) {
                    throw new IllegalArgumentException("mob #0: " + e.getMessage(), e);
                }
                // A later block the layout cannot place: keep the mobs read so far, leave the rest as tail.
                notes.add("mob#" + i + " of " + ai.mobCount + " not read (" + e.getMessage() + "); kept " + i);
                reliable = false;
                break;
            }
            mai.crits = Gms265AttackDecoder.calculateCriticalsByAverage(mai.damages, totalCr);
            for (long d : mai.damages) {
                totalDamage += d;
            }
            notes.add("mob#" + i + " id=" + mai.mobId + " at " + at.offset() + " (" + at.how() + ")");
            int afterBlock = (int) in.getPosition();
            searchFrom = afterBlock;
            try {
                layout.betweenMobs().read(in, kind, ai, mai);
                cursor = (int) in.getPosition();
                if (cursor < afterBlock || cursor > body.length)
                    throw new IllegalArgumentException("mob auxiliary cursor outside packet: " + cursor);
                reliable = true;
            } catch (RuntimeException e) {
                in.seek(afterBlock);
                cursor = afterBlock;
                reliable = false;
                notes.add("mob#" + i + " info stopped (" + e + ")");
            }
            ai.mobAttackInfo.add(mai);
        }
        ai.totalDamageDealt = totalDamage;

        // Both captured Khali process families end a zero-target attack with only the four-byte transport field.
        if ((ai.skillId == 154141008 || ai.skillId == 154141500) && ai.mobCount == 0 && ai.processType != null
                && (!reliable || body.length - cursor != Integer.BYTES)) ai.processType.nativeProcessId = 0;

        in.seek(cursor);
        byte[] tail;
        if (reliable) {
            tail = layout.tail().read(in, kind, ai);
        } else {
            tail = Tms282AttackTail.RAW.read(in, kind, ai);
        }
        if (tail.length > 0) {
            notes.add("tail " + tail.length + "B at " + cursor + ": " + preview(tail, 0));
        }
        return new AttackRequest(ai, requestedSkillId, tail, notes);
    }

    /** The verified per-mob block up to and including the two crc ints. */
    static MobAttackInfo readMobBlock(InPacket in, Tms282AttackKind kind, int hits) {
        int minimumBytes = 49 + (kind.magicMobExtra() ? 1 : 0) + hits * Long.BYTES;
        if (hits < 0 || hits > 15 || in.getUnreadAmount() < minimumBytes)
            throw new IllegalArgumentException("truncated mob damage block");
        MobAttackInfo mai = new MobAttackInfo();
        mai.mobId = in.decodeInt();
        mai.hitAction = in.decodeByte();
        mai.left = in.decodeByte();
        mai.idk3 = in.decodeByte();
        mai.forceActionAndLeft = in.decodeByte();
        mai.frameIdx = in.decodeByte();
        mai.templateID = in.decodeInt();
        mai.calcDamageStatIndexAndDoomed = in.decodeByte();
        mai.hitX = in.decodeShort();
        mai.hitY = in.decodeShort();
        mai.oldPosX = in.decodeShort();
        mai.oldPosY = in.decodeShort();
        if (kind.magicMobExtra()) {
            mai.hpPerc = in.decodeByte();
            mai.magicInfo = in.decodeShort();
        } else {
            mai.idk6 = in.decodeShort();
        }
        mai.idk7 = in.decodeInt();
        mai.idk8 = in.decodeInt();
        mai.idk9 = in.decodeByte();
        mai.idkInt = in.decodeInt(); // 282 new [DE4]
        mai.damages = new long[hits];
        for (int j = 0; j < hits; j++) {
            mai.damages[j] = in.decodeLong();
        }
        mai.mobUpDownYRange = in.decodeInt();
        mai.crc1 = in.decodeInt();
        mai.crc2 = in.decodeInt();
        return mai;
    }

    /** The mobs of {@code chr}'s field (none when it has no field). */
    public static Tms282MobLocator.MobDirectory mobsOf(Char chr) {
        Field field = chr == null ? null : chr.getField();
        if (field == null) {
            return Tms282MobLocator.MobDirectory.NONE;
        }
        return new Tms282MobLocator.MobDirectory() {
            @Override
            public int templateOf(int objectId) {
                Life life = field.getLifeByObjectID(objectId);
                return life instanceof Mob mob ? mob.getTemplateId() : -1;
            }

            @Override
            public boolean isKnownTemplate(int templateId) {
                return templateId > 0 && MobData.getMobById(templateId) != null;
            }
        };
    }

    private static int totalCriticalRate(Char chr) {
        try {
            return chr.getTotalStat(BaseStat.cr);
        } catch (RuntimeException e) {
            return 0;
        }
    }

    static String preview(byte[] data, int from) {
        int end = Math.min(data.length, from + LOG_TAIL_BYTES);
        return HexFormat.of().formatHex(data, from, end) + (end < data.length ? "..." : "");
    }
}
