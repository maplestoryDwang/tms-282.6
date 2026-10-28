package net.swordie.ms.client.jobs.resistance;

import net.swordie.ms.ServerConfig;
import net.swordie.ms.client.Client;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.CharacterStat;
import net.swordie.ms.client.character.info.HitInfo;
import net.swordie.ms.client.character.skills.Option;
import net.swordie.ms.client.character.skills.Skill;
import net.swordie.ms.client.character.skills.info.AttackInfo;
import net.swordie.ms.client.character.skills.info.SkillInfo;
import net.swordie.ms.client.character.skills.info.SkillUseInfo;
import net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat;
import net.swordie.ms.client.character.skills.temp.TemporaryStatManager;
import net.swordie.ms.client.jobs.Job;
import net.swordie.ms.connection.InPacket;
import net.swordie.ms.constants.FieldConstants;
import net.swordie.ms.constants.JobConstants;
import net.swordie.ms.life.mob.Mob;
import net.swordie.ms.loaders.SkillData;

import java.util.EnumMap;

import static net.swordie.ms.client.character.skills.SkillStat.speed;
import static net.swordie.ms.client.character.skills.SkillStat.time;
import static net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat.DarkSight;
import static net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat.Speed;

/**
 * Created on 12/14/2017.
 */
public class Citizen extends Job {

    public static final int CRYSTAL_THROW = 30001000;
    public static final int INFLITRATE = 30001001;
    public static final int POTION_MASTERY = 30000002;

    private final int[] addedSkills = new int[]{
            CRYSTAL_THROW,
            INFLITRATE,
            POTION_MASTERY
    };

    public Citizen(Char chr) {
        super(chr);

        if (chr.getId() != 0 && isHandlerOfJob(chr.getJob())) {
            for (int id : addedSkills) {
                if (!chr.hasSkill(id)) {
                    Skill skill = SkillData.getSkillDeepCopyById(id);
                    if (skill != null) {
                        skill.setRootId(3000);
                        skill.setMasterLevel(3);
                        skill.setMaxLevel(3);
                        chr.addSkill(skill);
                    }
                }
            }
        }
    }

    @Override
    public void handleDebuffOnMob(Client c, Mob mob, SkillInfo si, int skillID, int slv, long damage) {
        super.handleDebuffOnMob(c, mob, si, skillID, slv, damage);
    }

    @Override
    public void handleAttack(Client c, AttackInfo attackInfo, SkillInfo si, long now) {
        super.handleAttack(c, attackInfo, si, now);
    }

    @Override
    public void handleSkill(Client c, InPacket inPacket, SkillUseInfo skillUseInfo) {
        super.handleSkill(c, inPacket, skillUseInfo);
        SkillInfo si = skillUseInfo.skillInfo;
        int slv = skillUseInfo.slv;
        int skillID = skillUseInfo.skillID;
        if (skillID == INFLITRATE) {
            EnumMap<CharacterTemporaryStat, Option> newStats = new EnumMap<>(CharacterTemporaryStat.class);
            TemporaryStatManager tsm = chr.getTemporaryStatManager();
            Option o1 = new Option();
            Option o2 = new Option();
            o1.nOption = 1;
            o1.rOption = skillID;
            o1.tOption = si.getValue(time, slv);
            newStats.put(DarkSight, o1);
            o2.nOption = si.getValue(speed, slv);
            o2.rOption = skillID;
            o2.tOption = si.getValue(time, slv);
            newStats.put(Speed, o2);
            tsm.sendStat(newStats);
        }
    }

    @Override
    public void handleHit(Client c, InPacket inPacket, HitInfo hitInfo) {
        super.handleHit(c, inPacket, hitInfo);
    }

    @Override
    public void setCharCreationStats(Char chr) {
        super.setCharCreationStats(chr);
        CharacterStat cs = chr.getAvatarData().getCharacterStat();
        cs.setPosMap(FieldConstants.START_MAP);
    }

    @Override
    public boolean isHandlerOfJob(short id) {
        return id == JobConstants.JobEnum.CITIZEN.getJobId();
    }
}
