package net.swordie.ms.client.character.tms282;

import net.swordie.ms.client.Client;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.skills.Option;
import net.swordie.ms.client.character.skills.SkillStat;
import net.swordie.ms.client.character.skills.info.SkillUseInfo;
import net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat;
import net.swordie.ms.connection.packet.Tms282SkillUseRequest;
import net.swordie.ms.connection.packet.Tms282SkillProcess;
import net.swordie.ms.connection.protocol.Protocol;
import net.swordie.ms.connection.protocol.ProtocolVersion;
import net.swordie.ms.enums.BaseStat;
import net.swordie.ms.loaders.SkillData;
import net.swordie.ms.world.field.Field;

import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.BufferUnderflowException;
import java.util.Map;
import java.util.WeakHashMap;

/** Captured Ark/Xenon Loaded Dice: an accepted use opens one native CP03D5 selection. */
public final class Tms282LoadedDice {
    public static final int LOADED = 400051000, ROLL = 400051001;
    private record Prompt(WeakReference<Client> client, WeakReference<Field> field, int level) {}
    private static final Map<Char, Prompt> PROMPTS = new WeakHashMap<>();

    private Tms282LoadedDice() {}

    public static boolean handles(Char chr, int skill) {
        return Protocol.active().version() == ProtocolVersion.TMS_282_3 && skill == LOADED && chr != null
                && (chr.getJob() == 15512 || chr.getJob() == 3612);
    }

    /** Before skill-use cost; captured option0 and three zero suffix bytes are the only understood form. */
    public static void validate(Char chr, Tms282SkillUseRequest request) {
        if (handles(chr, request.skillId())) {
            int level = ownedLevel(chr);
            byte[] rest = request.rest();
            if (rest.length < 4) throw new IllegalArgumentException("Missing Loaded Dice transport trailer");
            ByteBuffer in = ByteBuffer.wrap(rest).order(ByteOrder.LITTLE_ENDIAN); in.limit(rest.length - 4);
            try {
                int option = Tms282SkillProcess.decodeUsePrefix(in, new SkillUseInfo(SkillData.getSkillInfoById(LOADED), level));
                if (option != 0 || in.remaining() != 3 || in.get() != 0 || in.get() != 0 || in.get() != 0)
                    throw new IllegalArgumentException("Unknown Loaded Dice use body");
            } catch (BufferUnderflowException truncated) {
                throw new IllegalArgumentException("Truncated Loaded Dice use body", truncated);
            }
        }
    }

    /** After an accepted use has paid HP and started its cooldown; skip the legacy choice-byte handler here. */
    public static boolean afterAccepted(Char chr, Tms282SkillUseRequest request) {
        if (!handles(chr, request.skillId())) return false;
        validate(chr, request);
        synchronized (PROMPTS) {
            PROMPTS.put(chr, new Prompt(new WeakReference<>(chr.getClient()), new WeakReference<>(chr.getField()), ownedLevel(chr)));
        }
        return true;
    }

    /** Field transfer closes the client prompt, including a later return to the same cached Field object. */
    public static void clear(Char chr) {
        if (Protocol.active().version() != ProtocolVersion.TMS_282_3) return;
        synchronized (PROMPTS) { PROMPTS.remove(chr); }
    }

    public static void select(Char chr, int choice) {
        if (choice < 1 || choice > 6) throw new IllegalArgumentException("Unsupported Loaded Dice face");
        int level = ownedLevel(chr);
        synchronized (PROMPTS) {
            Prompt prompt = PROMPTS.get(chr);
            if (prompt == null || prompt.client().get() != chr.getClient() || prompt.field().get() != chr.getField()
                    || prompt.level() != level) {
                PROMPTS.remove(chr);
                throw new IllegalArgumentException("No owned Loaded Dice selection");
            }
            PROMPTS.remove(chr); // One selection per paid use; malformed packets never reach this method.
        }
        var tsm = chr.getTemporaryStatManager();
        tsm.removeStatsBySkill(ROLL);
        chr.removeBaseStatByOption();
        Option loaded = new Option(); loaded.nOption = choice; loaded.rOption = LOADED; loaded.slv = level;
        tsm.sendStat(CharacterTemporaryStat.LoadedDice, loaded);
        tsm.throwDice(choice);
        if (choice == 1) return;
        var data = SkillData.getSkillInfoById(ROLL);
        // WZ damR is damage rate; the old 265 helper's face5 incorrectly uses attack rate.
        if (choice == 5) chr.addDiceBaseStat(BaseStat.damR, data.getValue(SkillStat.damR, level));
        else chr.addBaseStatByDiceNumber(choice, data, level);
        Option dice = new Option(); dice.nOption = choice; dice.rOption = ROLL; dice.slv = level;
        dice.tOption = data.getValue(SkillStat.time, level);
        tsm.sendStat(CharacterTemporaryStat.Dice, dice); // Existing Job.handleRemoveCTS removes the dice base stats on expiry.
    }

    private static int ownedLevel(Char chr) {
        var data = SkillData.getSkillInfoById(LOADED);
        int level = chr == null ? 0 : Tms282HiddenSkillParents.useLevel(chr, LOADED);
        if (!handles(chr, LOADED) || chr.getClient() == null || chr.getClient().getChr() != chr || chr.getField() == null
                || chr.getHP() <= 0 || chr.getLevel() < 200 || data == null || level <= 0 || level > data.getMaxLevel()
                || SkillData.getSkillInfoById(ROLL) == null)
            throw new IllegalArgumentException("Unowned Loaded Dice skill");
        return level;
    }
}
