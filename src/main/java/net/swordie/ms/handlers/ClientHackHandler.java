package net.swordie.ms.handlers;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.skills.Skill;
import net.swordie.ms.client.character.skills.SkillStat;
import net.swordie.ms.client.character.skills.info.AttackInfo;
import net.swordie.ms.client.character.skills.info.MobAttackInfo;
import net.swordie.ms.client.character.skills.info.SkillInfo;
import net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat;
import net.swordie.ms.client.character.skills.temp.TemporaryStatManager;
import net.swordie.ms.client.jobs.cygnus.DawnWarrior;
import net.swordie.ms.client.social.Party.PartyMember;
import net.swordie.ms.connection.packet.Effect;
import net.swordie.ms.connection.packet.UserPacket;
import net.swordie.ms.connection.packet.UserRemote;
import net.swordie.ms.constants.JobConstants;
import net.swordie.ms.enums.ChatType;
import net.swordie.ms.life.Life;
import net.swordie.ms.life.mob.Mob;
import net.swordie.ms.life.mob.MobStat;
import net.swordie.ms.loaders.MobData;
import net.swordie.ms.loaders.SkillData;
import net.swordie.ms.loaders.StringData;
import net.swordie.ms.util.Position;
import net.swordie.ms.util.Rect;
import net.swordie.ms.util.container.Tuple;

import java.text.DecimalFormat;

import static net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat.Enrage;

public class ClientHackHandler {
    private static boolean isExploitSkill(Char chr, AttackInfo attackInfo) {
        switch (attackInfo.skillId) {
            case 80001332:
            case 80001431:
            case 80001593:
            case 80001706:
                System.out.printf("[Warning] Type: Skill Exploit | Char: %s | Skill ID: %d | Skill Name: %s%n", chr.getName(), attackInfo.skillId, StringData.getSkillStringById(attackInfo.skillId));
                return false;
        }
        return true;
    }

    private static boolean isCorrectAttackCount(Char chr, AttackInfo attackInfo) {
        SkillInfo skillInfo = SkillData.getSkillInfoById(attackInfo.skillId);
        TemporaryStatManager tsm = chr.getTemporaryStatManager();

        if (skillInfo != null) {
            short jobID = chr.getJob();
            int skillID = skillInfo.getSkillId();
            byte hits = (byte) skillInfo.getValue(SkillStat.attackCount, attackInfo.slv);
            if (JobConstants.isHero(jobID)) {
                //Final Attack
                if (skillID == 1100002) {
                    hits++;
                }
                //Raging Blow Hyper
                else if ((skillID == 1120017 || skillID == 1121008) && chr.hasSkill(1120051)) {
                    hits++;
                }
            } else if (JobConstants.isDawnWarrior(jobID)) {
                if ((skillInfo.getSkillId() == 11121203 || skillInfo.getSkillId() == 11121103) && chr.hasSkill(11120048)) { //Hyper Skill.
                    hits++;
                }
                if (tsm.hasStatBySkillId(DawnWarrior.FALLING_MOON)) {
                    hits = (byte) (hits * 2);
                }
                if (hits > 15) {
                    hits = 15;
                }
            }
            if (hits != attackInfo.hits) {
                chr.chatMessage(ChatType.Notice2, String.format("[警告] 類型：攻擊次數 | 角色：%s | 技能ID：%d | 攻擊次數：%d | 資料攻擊次數 %d", chr.getName(), attackInfo.skillId, attackInfo.hits, hits));
                return false;
            }
        }
        return true;
    }

    private static boolean isCorrectMobCount(Char chr, AttackInfo attackInfo) {
        SkillInfo skillInfo = SkillData.getSkillInfoById(attackInfo.skillId);
        TemporaryStatManager tsm = chr.getTemporaryStatManager();

        if (skillInfo != null && attackInfo.mobAttackInfo.size() != 0) {
            short jobID = chr.getJob();
            int skillID = skillInfo.getSkillId();
            byte mobs = (byte) skillInfo.getValue(SkillStat.mobCount, attackInfo.slv);
            if (JobConstants.isHero(jobID)) {
                //Final Attack
                if (skillID == 1120013) {
                    mobs++;
                }
                //Raging Blow Hyper
                else if ((skillID == 1120017 || skillID == 1121008) && chr.hasSkill(1120050)) {
                    mobs = (byte) (mobs + 2 + 2);
                }
                //Enrage
                if (tsm.hasStatBySkillId(1121010)) {
                    mobs = 1;
                }

            } else if (JobConstants.isDawnWarrior(jobID)) {
                if ((skillID == 11121203 || skillID == 11121103) && chr.hasSkill(11120047)) {
                    mobs = (byte) (mobs + 2 + 2);
                }
            }
            if (attackInfo.mobCount > mobs) {
                chr.chatMessage(ChatType.Notice2, String.format("[警告] 類型：怪物數 | 角色：%s | 技能ID：%d | 怪物數：%d | 資料怪物數：%d", chr.getName(), attackInfo.skillId, attackInfo.mobCount, mobs));
                return false;
            }
        }
        return true;
    }

    private static boolean isCorrectMobLevel(Char chr, MobAttackInfo mobAttackInfo, Mob mob) {
        if (mob.getLevel() - chr.getLevel() > 30 && mobAttackInfo.damages[0] != 1) {
            System.out.printf("[Warning] Type: Edit | Char: %s | Char Level: %d | Mob ID: %d | Mob Level: %d | Mob Name: %s%n", chr.getName(), chr.getLevel(), mobAttackInfo.mobId, mob.getLevel(), StringData.getMobStringById(mobAttackInfo.mobId));
            return false;
        }
        return true;
    }

    private static boolean isCorrectFixedDamage(Char chr, AttackInfo attackInfo, MobAttackInfo mobAttackInfo, Mob mob) {
        DecimalFormat formatter = new DecimalFormat("###,###,###,###");
        short totalLineDuplicate = 0;
        SkillInfo skillInfo = SkillData.getSkillInfoById(attackInfo.skillId);
        if (skillInfo != null) {
            switch (skillInfo.getSkillId()) {
                case 80001770: //Level Up Skill
                    return true;
            }
            if (mobAttackInfo.damages.length > 1) {
                for (int i = 1; i < mobAttackInfo.damages.length; i++) {
                    if (mobAttackInfo.damages[0] == mobAttackInfo.damages[i]) {
                        totalLineDuplicate++;
                    }
                }
                if (totalLineDuplicate == mobAttackInfo.damages.length - 1) {
                    //TODO: Handle ngoại lệ với những skill Dot Damage, player Max dmg
                    if (!skillInfo.isFixDamageSkill() && mob.getFixedDamage() == 0 && mobAttackInfo.damages[0] != 1 && mobAttackInfo.damages[0] != 0) {
                        chr.chatMessage(ChatType.Notice2, String.format("[警告] 類型：傷害 | 角色：%s | 怪物ID：%d | 技能ID：%d | 傷害：%s", chr.getName(), mobAttackInfo.mobId, attackInfo.skillId, formatter.format(mobAttackInfo.damages[0])));
                        //chr.chatMessage(ChatType.Notice2, String.format("[Warning] Type: Damage | Char: %s | Mob ID: %d | Skill ID: %d | Damage: %s", chr.getName(), mobAttackInfo.mobId, attackInfo.skillId, formatter.format(mobAttackInfo.damages[0])));
                        //System.out.printf("[Warning] Type: Damage | Char: %s | Mob ID: %d | Skill ID: %d | Damage: %s", chr.getName(), mobAttackInfo.mobId, attackInfo.skillId, formatter.format(mobAttackInfo.damages[0])));
                        return false;
                    }
                }
            } else if (mobAttackInfo.damages.length == 1 && mobAttackInfo.damages[0] != 1 && mobAttackInfo.damages[0] != 0) {
                if (chr.getLastNormalAttack() == null) {
                    chr.setLastNormalAttack(new Tuple<>(mobAttackInfo.damages[0], (byte) 1));
                } else if (chr.getLastNormalAttack() != null) {
                    if (chr.getLastNormalAttack().getLeft() == mobAttackInfo.damages[0]) {
                        if (chr.getLastNormalAttack().getRight() < 2) {
                            chr.setLastNormalAttack(new Tuple<>(chr.getLastNormalAttack().getLeft(), (byte) (chr.getLastNormalAttack().getRight() + 1)));
                        } else if (chr.getLastNormalAttack().getRight() >= 2) {
                            chr.chatMessage(ChatType.Notice2, String.format("[警告] 類型：竄改 | 角色：%s | 怪物ID：%d | 技能ID：%d | 傷害：%s", chr.getName(), mobAttackInfo.mobId, attackInfo.skillId, formatter.format(mobAttackInfo.damages[0])));
                            //chr.chatMessage(ChatType.Notice2, String.format("[Warning] Type: Damage | Char: %s | Mob ID: %d | Skill ID: %d | Damage: %s", chr.getName(), mobAttackInfo.mobId, attackInfo.skillId, formatter.format(mobAttackInfo.damages[0])));
                            //System.out.printf("[Warning] Type: Damage | Char: %s | Mob ID: %d | Skill ID: %d | Damage: %s", chr.getName(), mobAttackInfo.mobId, attackInfo.skillId, formatter.format(mobAttackInfo.damages[0])));
                            chr.setLastNormalAttack(null);
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }

    private static boolean isCorrectLtRb(Char chr, AttackInfo attackInfo, MobAttackInfo mobAttackInfo) {
        SkillInfo skillInfo = SkillData.getSkillInfoById(attackInfo.skillId);
        Position charPos = attackInfo.charPosition;
        boolean isLeft = chr.isLeft();
        if (skillInfo != null) {
            Rect skillRect = skillInfo.getFirstRect();
            Rect leftRect = null;
            Rect rightRect = null;
            if (skillRect != null) {
                leftRect = chr.getRectAround(skillRect);
                leftRect = new Rect(leftRect.getLeft() - 250, leftRect.getTop() - 250, leftRect.getRight() + 250, leftRect.getBottom() + 250);
                rightRect = leftRect.horizontalFlipAround(charPos.getX());
                if (isLeft) {
                    leftRect = new Rect(leftRect.getLeft() - 50, leftRect.getTop(), leftRect.getRight() - 50, leftRect.getBottom());
                } else {
                    rightRect = new Rect(rightRect.getLeft() + 100, rightRect.getTop(), rightRect.getRight() + 100, rightRect.getBottom());
                }
                Position mobHitPos = new Position(mobAttackInfo.hitX, mobAttackInfo.hitY);
                if (!leftRect.hasPositionInside(mobHitPos) && !rightRect.hasPositionInside(mobHitPos)) {
                    chr.chatMessage(ChatType.Notice2, String.format("[警告] 類型：攻擊範圍 | 角色：%s | 角色朝左：%s | 怪物ID：%d | 技能ID：%d", chr.getName(), isLeft, mobAttackInfo.mobId, attackInfo.skillId));
                }
            }
        }
        return true;
    }

    public static boolean handleHackDetect(Char chr, AttackInfo attackInfo) {
//        isCorrectAttackCount(chr, attackInfo);
//        isCorrectMobCount(chr, attackInfo);
//        isExploitSkill(chr, attackInfo);
//        for(MobAttackInfo mobAttackInfo : attackInfo.mobAttackInfo){
//            Mob mob = (Mob) chr.getField().getLifeByObjectID(mobAttackInfo.mobId);
//            if(mob != null){
//                isCorrectLtRb(chr, attackInfo, mobAttackInfo);
//                isCorrectFixedDamage(chr, attackInfo, mobAttackInfo, mob);
//                isCorrectMobLevel(chr, mobAttackInfo, mob);
//            }
//        }
        isExploitSkill(chr, attackInfo);
        for (MobAttackInfo mobAttackInfo : attackInfo.mobAttackInfo) {
            Life life = chr.getField().getLifeByObjectID(mobAttackInfo.mobId);
            if (life instanceof Mob) {
                Mob mob = (Mob) chr.getField().getLifeByObjectID(mobAttackInfo.mobId);
                if (mob != null) {
                    //isCorrectLtRb(chr, attackInfo, mobAttackInfo);
                    //isCorrectFixedDamage(chr, attackInfo, mobAttackInfo, mob);
                    isCorrectMobLevel(chr, mobAttackInfo, mob);
                }
            }
        }
        return true;
    }
}