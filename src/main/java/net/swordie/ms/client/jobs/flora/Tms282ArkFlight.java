package net.swordie.ms.client.jobs.flora;

import net.swordie.ms.client.Client;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.skills.Option;
import net.swordie.ms.client.character.skills.SkillStat;
import net.swordie.ms.client.character.skills.info.SkillUseInfo;
import net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat;
import net.swordie.ms.connection.protocol.Protocol;
import net.swordie.ms.connection.protocol.ProtocolVersion;
import net.swordie.ms.connection.packet.Tms282SkillProcess;
import net.swordie.ms.connection.packet.Tms282SkillUseRequest;
import net.swordie.ms.constants.JobConstants;
import net.swordie.ms.loaders.SkillData;
import net.swordie.ms.handlers.login.tms282.Tms282LoginState;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/** Native 10999770/10FDA74 derives this flight from Mystical Leap; 57B1C08 requires SpecterMode. */
public final class Tms282ArkFlight {
    public static final int SKILL = 155001205, PARENT = 155001104;

    private Tms282ArkFlight() { }

    public static int learnedLevel(Char chr) {
        return chr != null && Protocol.active().version() == ProtocolVersion.TMS_282_3
                && JobConstants.isArk(chr.getJob()) && chr.getJob() >= 15500
                ? Math.min(5, Math.max(0, chr.getSkillLevel(PARENT))) : 0;
    }

    /** Before any generic cast cost or cooldown; the client has already started its local flight. */
    public static String refusal(Char chr) {
        if (learnedLevel(chr) <= 0) return "Ark flight requires learned Mystical Leap";
        if (!(chr.getJobHandler() instanceof Ark) || chr.getId() <= 0 || chr.getHP() <= 0 || chr.getField() == null
                || chr.getClient() == null || chr.getClient().getChr() != chr || !chr.getClient().getCh().isActive()
                || chr.getClient().getCurrentState() != Client.IN_FIELD || chr.getClient().getNextState() != Client.NONE
                || chr.getClient().getUser() == null
                || chr.getClient().getUser().getId() <= 0 || chr.getClient().getUser() != chr.getUser()
                || !Tms282LoginState.of(chr.getClient()).isChannel()
                || Tms282LoginState.of(chr.getClient()).fieldCharacter() != chr
                || chr.getField().getCharByID(chr.getId()) != chr)
            return "Ark flight requires its living field owner";
        Option mode = chr.getTemporaryStatManager().getOption(CharacterTemporaryStat.SpecterMode);
        return mode.nOption != 0 && mode.rOption == Ark.SPECTER_STATE ? null : "Ark flight requires SpecterMode";
    }

    /** Native 59E53A0/53AB (also 5A7A266/274): common option0, then a constant zero byte. */
    public static void validateUse(Tms282SkillUseRequest request) {
        ByteBuffer in = ByteBuffer.wrap(request.rest()).order(ByteOrder.LITTLE_ENDIAN);
        if (in.remaining() < 4) throw new IllegalArgumentException("Missing flight transport trailer");
        in.limit(in.remaining()-4);
        int option = Tms282SkillProcess.decodeUsePrefix(in, new SkillUseInfo(request.skillId(), request.slv()));
        if (option != 0 || in.remaining() != 1 || in.get() != 0)
            throw new IllegalArgumentException("Invalid native flight suffix");
    }

    /** The type10 WZ time is already milliseconds, as for the other NewFlying skills. */
    public static int durationMillis(int slv) {
        var info = SkillData.getSkillInfoById(SKILL);
        if (info == null || slv < 1 || slv > info.getMaxLevel())
            throw new IllegalArgumentException("Invalid Ark flight level");
        int millis = info.getValue(SkillStat.time, slv);
        if (millis <= 0) throw new IllegalArgumentException("Missing Ark flight duration");
        return millis;
    }

    /** Native 5443330 lands this flight when SpecterMode ends; do not cancel another source's NewFlying. */
    public static void onSpecterExit(Char chr) {
        if (chr == null || Protocol.active().version() != ProtocolVersion.TMS_282_3) return;
        // Existing TSM removal revokes this Char's bound book, including its original/copy identity check.
        chr.getTemporaryStatManager().removeStatsBySkill(SKILL);
    }
}
