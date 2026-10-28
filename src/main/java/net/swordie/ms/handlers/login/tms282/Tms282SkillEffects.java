package net.swordie.ms.handlers.login.tms282;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.CharacterStat;
import net.swordie.ms.client.character.skills.info.SkillInfo;
import net.swordie.ms.client.character.skills.info.SkillUseInfo;
import net.swordie.ms.client.jobs.Job;
import net.swordie.ms.client.jobs.flora.Adele;
import net.swordie.ms.connection.InPacket;
import net.swordie.ms.connection.packet.Tms282SkillUseRequest;
import net.swordie.ms.connection.packet.Tms282SkillProcess;
import net.swordie.ms.client.character.skills.SkillStat;
import net.swordie.ms.loaders.SkillData;

import java.util.Objects;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * The server-side effects of a skill use that the overlay's buff service ({@code Tms282SkillService}: cost,
 * cooldown, buff CTS) does not model: after an accepted CP04B4 the 265 job handler's
 * {@link Job#handleSkill(net.swordie.ms.client.Client, InPacket, SkillUseInfo)} runs, as 265
 * {@code SkillHandler.handleUserSkillUseRequest} would after its cost / cooldown checks. That is where every job
 * spawns its summons ({@code Field.spawnSummon} -> LP0664 through {@code Tms282SummonedPackets}), affected areas,
 * heals ({@code Char.heal} -> STAT_CHANGED, re-stamped as LP009D), mob debuffs (mob codecs), job resources and
 * skill-specific state. Packets 265 writes that 282 has no codec for (its TEMPORARY_STAT_SET, most effects) are
 * dropped by the fail-closed guard as before; the buff the client sees stays the service's LP009E.
 * <p>
 * The282 ProcessType and special-move fields are decoded before handing the remaining native skill body to
 * the legacy job handler. The transport trailer never reaches that handler. Padding is retained only after a
 * successfully bounded parse for legacy handlers' optional fields; malformed common fields never run a job.
 * <p>
 * Cost: the service already paid {@code mpCon} and {@code hpCon}; {@code Job.handleSkill} pays {@code hpCon}
 * again ({@code applyHpCost}), so that HP is given back without a packet first. ({@code hpRCon}, the percentage
 * cost, is paid only by the job handler.) Failures are logged; the use itself stays accepted.
 */
final class Tms282SkillEffects {
    /** Zero bytes after the 282 body: per-skill fields a job handler may read. */
    static final int PADDING = 256;

    private final Tms282SkillSettings settings;

    Tms282SkillEffects(Tms282SkillSettings settings) {
        this.settings = Objects.requireNonNull(settings, "settings");
    }

    /** What happened, for the log line. */
    record Result(boolean ran, String log) {
    }

    Result afterAccepted(Char chr, Tms282SkillUseRequest request) {
        Objects.requireNonNull(chr, "chr");
        Objects.requireNonNull(request, "request");
        if (!settings.skillEffects265()) {
            return new Result(false, "265 job effects off (" + Tms282SkillSettings.KEY_SKILL_EFFECTS_265 + ")");
        }
        Job job = chr.getJobHandler();
        if (job == null || chr.getField() == null || chr.getClient() == null) {
            return new Result(false, "no job handler / field / client");
        }
        int skillId = request.skillId();
        if(net.swordie.ms.client.jobs.resistance.Tms282OmegaBlaster.handles(skillId))
            return new Result(true,"282 Omega Blaster owned by its native lifecycle");
        SkillInfo si = SkillData.getSkillInfoById(skillId);
        if (si == null) {
            return new Result(false, "no skill data");
        }
        int slv = net.swordie.ms.client.character.tms282.Tms282HiddenSkillParents.useLevel(chr, skillId);
        SkillUseInfo sui = new SkillUseInfo(si, slv);
        sui.skillID = skillId;
        Parsed parsed = parse(request.rest(), sui);
        if (!parsed.fits()) return new Result(false, "skill body rejected: " + parsed.why());
        if (net.swordie.ms.client.character.tms282.Tms282LoadedDice.afterAccepted(chr, request))
            return new Result(true, "owned Loaded Dice selection opened");
        int hpCon = si.getValue(SkillStat.hpRCon, slv) > 0 ? 0 : Math.max(0, si.getValue(SkillStat.hpCon, slv));
        CharacterStat stat = chr.getAvatarData().getCharacterStat();
        if (hpCon > 0) {
            stat.setHp(Math.min(stat.getHp() + hpCon, chr.getMaxHP()));
        }
        try {
            job.handleSkill(chr.getClient(), new InPacket(parsed.handlerBody()), sui);
            net.swordie.ms.client.character.skills.PartyBooster.applyToParty(chr, si, slv);
        } catch (RuntimeException | LinkageError e) {
            return new Result(true, "265 " + job.getClass().getSimpleName() + ".handleSkill FAILED: " + e);
        }
        return new Result(true, "265 " + job.getClass().getSimpleName() + ".handleSkill ran");
    }

    /** The job handler's body and whether the 265 parse of the request rest fitted. */
    record Parsed(byte[] handlerBody, boolean fits, String why) {
    }

    /** Reject malformed native common fields before the service spends HP/MP or starts a cooldown. */
    static void validate(Tms282SkillUseRequest request) {
        validate(null,request);
    }

    static SkillUseInfo validate(Char chr,Tms282SkillUseRequest request) {
        SkillInfo info=SkillData.getSkillInfoById(request.skillId());
        if(info==null)return null; // The authority service reports missing skill data.
        SkillUseInfo sui=new SkillUseInfo(info,request.slv());sui.skillID=request.skillId();
        Parsed parsed=parse(request.rest(),sui);
        if(!parsed.fits())throw new IllegalArgumentException(parsed.why());
        net.swordie.ms.client.jobs.flora.Tms282ReturnSkillGuard.validate(chr,request);
        net.swordie.ms.client.character.tms282.Tms282LoadedDice.validate(chr,request);
        net.swordie.ms.client.jobs.flora.Tms282AdeleShardbreaker.validate(chr,sui);
        if (!net.swordie.ms.client.jobs.sengoku.Tms282KannaOniController.validate(chr,sui))
            throw new IllegalArgumentException("Unowned or unavailable Kanna Oni");
        if (!net.swordie.ms.client.jobs.resistance.Tms282PredatorsLeap.validate(chr,sui))
            throw new IllegalArgumentException("Unowned or unavailable Predator target");
        return sui;
    }

    /** Native1207F20 +118F980 +one option i32; preserve the skill-specific suffix and exclude346A070's trailer. */
    static Parsed parse(byte[] rest, SkillUseInfo sui) {
        try {
            if (rest.length < 4) throw new IllegalArgumentException("missing transport trailer");
            ByteBuffer in = ByteBuffer.wrap(rest).order(ByteOrder.LITTLE_ENDIAN);
            in.limit(rest.length - 4); // Opaque transport field; never an option or job-handler body.
            int option=Tms282SkillProcess.decodeUsePrefix(in,sui);
            if(sui.skillID==Adele.AETHER_CRYSTAL && net.swordie.ms.connection.packet.Tms282AdeleShardPackets.handles(sui.skillId)){
                // 5A7878A..78814: option, duplicate short XY, direction/local-state/zero bytes.
                sui.adeleCrystalOption=option;
                if(option!=32||sui.adeleCrystalOrigin==null||sui.adeleCrystalPosition==null||in.remaining()!=11)
                    throw new IllegalArgumentException("Invalid owned crystal continuation");
                ByteBuffer suffix=in.duplicate().order(in.order()); // Validate while retaining the existing handler-body contract.
                for(int i=0;i<2;i++)if(suffix.getShort()!=sui.adeleCrystalPosition.getX()||suffix.getShort()!=sui.adeleCrystalPosition.getY())
                    throw new IllegalArgumentException("Inconsistent crystal position suffix");
                if(Byte.toUnsignedInt(suffix.get())>1)throw new IllegalArgumentException("Invalid crystal direction");
                suffix.get();
                if(suffix.get()!=0)throw new IllegalArgumentException("Unknown crystal suffix");
            }
            // 4469B3F: this coordinate suffix is specific to1241010, not every option with bit4 set.
            if(sui.skillID==1241010 && (option&0x10)!=0){in.getShort();in.getShort();}
            if(sui.skillID==400031051)sui.isLeft=in.get()!=0;
            if(sui.skillID==Adele.RESONANCE_RUSH)sui.objectId=in.getInt();
            if(sui.skillID==121001008 && in.hasRemaining())throw new IllegalArgumentException("unexpected dash body suffix");
            byte[] body=new byte[in.remaining()+PADDING];in.get(body,0,in.remaining());
            return new Parsed(body,true,"");
        } catch (RuntimeException e) {
            return new Parsed(new byte[PADDING], false, e.toString());
        }
    }
}
