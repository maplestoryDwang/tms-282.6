package net.swordie.ms.client.jobs.flora;

import net.swordie.ms.client.Client;
import net.swordie.ms.client.jobs.sengoku.Kanna;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.skills.SkillStat;
import net.swordie.ms.connection.packet.Tms282SkillUseRequest;
import net.swordie.ms.connection.protocol.Protocol;
import net.swordie.ms.connection.protocol.ProtocolVersion;
import net.swordie.ms.connection.protocol.codec.FieldAvailability;
import net.swordie.ms.constants.JobConstants;
import net.swordie.ms.handlers.login.tms282.Tms282LoginState;
import net.swordie.ms.loaders.SkillData;

/** The two captured282 home skills: guard their existing B4/service/Job warp before cost or cooldown. */
public final class Tms282ReturnSkillGuard {
    private Tms282ReturnSkillGuard() { }

    public static void validate(Char chr, Tms282SkillUseRequest request) {
        int skill = request.skillId();
        if (Protocol.active().version() != ProtocolVersion.TMS_282_3 || (skill != Illium.SHELTER_RETURN && skill != 40021227)) return;
        boolean illium = skill == Illium.SHELTER_RETURN;
        Client client = chr == null ? null : chr.getClient();
        if (chr == null || (illium ? !(chr.getJobHandler() instanceof Illium) || !JobConstants.isIllium(chr.getJob())
                : !(chr.getJobHandler() instanceof Kanna) || !JobConstants.isKanna(chr.getJob()))
                || chr.getSkillLevel(skill) != 1 || chr.getHP() <= 0 || chr.getId() <= 0
                || chr.getField() == null || chr.getField().getCharByID(chr.getId()) != chr
                || client == null || client.getChr() != chr || client.getCurrentState() != Client.IN_FIELD
                || client.getNextState() != Client.NONE || Tms282LoginState.of(client).fieldCharacter() != chr
                || client.getCh() == null || !client.getCh().isActive() || client.getUser() == null
                || client.getUser().getId() <= 0 || chr.getUser() != client.getUser()
                || !Tms282LoginState.of(client).isChannel())
            throw new IllegalArgumentException("Unowned or inactive captured home skill");
        var info = SkillData.getSkillInfoById(skill);
        if (info == null || info.getValue(SkillStat.x, 1) != (illium ? 940202000 : 807000000)
                || illium && info.getValue(SkillStat.y, 1) != 34900
                || info.getValue(SkillStat.mpCon, 1) != (illium ? 100 : 0) || info.getValue(SkillStat.cooltime, 1) != 600)
            throw new IllegalArgumentException("Unverified captured home skill data");
        if (illium && !chr.hasQuestCompleted(info.getValue(SkillStat.y, 1)))
            throw new IllegalArgumentException("Shelter Return requires completed quest34900");
        int destination = info.getValue(SkillStat.x, 1);
        if (!FieldAvailability.active().isAvailable(destination))
            throw new IllegalArgumentException("Home skill destination is unavailable to this client");
        var field = chr.getOrCreateFieldByCurrentInstanceType(destination);
        if (field == null || field.getId() != destination)
            throw new IllegalArgumentException("Home skill destination is missing");
    }
}
