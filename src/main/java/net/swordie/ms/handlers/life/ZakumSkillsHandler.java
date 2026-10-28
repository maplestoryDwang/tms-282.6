package net.swordie.ms.handlers.life;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.packet.MobPool;
import net.swordie.ms.constants.BossConstants;
import net.swordie.ms.life.mob.Mob;
import net.swordie.ms.life.mob.skill.MobSkill;
import net.swordie.ms.life.mob.skill.MobSkillID;
import net.swordie.ms.world.field.Field;

public class ZakumSkillsHandler {

    /**
     * Sử dụng để hiện thị remote cho các character còn lại trong map
     */
    public static void onSkillActionDisplay(Char chr, Mob mob, MobSkill mobSkill) {
        if (BossConstants.isZakumArm(mob.getTemplateId())) {
            Field field = chr.getField();
            //Normal Action
            field.broadcast(MobPool.forcedSkillAction(mob.getObjectId(), mobSkill.getAction() - 1), chr);

            //Fuse Arm Action slv 27 | Left Arm will hide and Right Arm will cast
            //[DEBUG] Testing sẽ để vào function lấy mobID fuse sau
            if (mobSkill.getSkillID() == MobSkillID.Damage.getVal() && mobSkill.getLevel() == 27) {
                Mob fusionMob = null;
                //Easy Zakum
                if (mob.getTemplateId() == 8800027) {
                    fusionMob = (Mob) field.getLifeByTemplateId(8800023);
                } else if (mob.getTemplateId() == 8800023) {
                    fusionMob = (Mob) field.getLifeByTemplateId(8800027);
                } else if (mob.getTemplateId() == 8800028) {
                    fusionMob = (Mob) field.getLifeByTemplateId(8800024);
                } else if (mob.getTemplateId() == 8800024) {
                    fusionMob = (Mob) field.getLifeByTemplateId(8800028);
                } else if (mob.getTemplateId() == 8800029) {
                    fusionMob = (Mob) field.getLifeByTemplateId(8800025);
                } else if (mob.getTemplateId() == 8800025) {
                    fusionMob = (Mob) field.getLifeByTemplateId(8800029);
                }
                //Normal Zakum
                else if (mob.getTemplateId() == 8800007) {
                    fusionMob = (Mob) field.getLifeByTemplateId(8800003);
                } else if (mob.getTemplateId() == 8800003) {
                    fusionMob = (Mob) field.getLifeByTemplateId(8800007);
                } else if (mob.getTemplateId() == 8800008) {
                    fusionMob = (Mob) field.getLifeByTemplateId(8800004);
                } else if (mob.getTemplateId() == 8800004) {
                    fusionMob = (Mob) field.getLifeByTemplateId(8800008);
                } else if (mob.getTemplateId() == 8800009) {
                    fusionMob = (Mob) field.getLifeByTemplateId(8800005);
                } else if (mob.getTemplateId() == 8800005) {
                    fusionMob = (Mob) field.getLifeByTemplateId(8800009);
                }
                //Chaos Zakum
                else if (mob.getTemplateId() == 8800107) {
                    fusionMob = (Mob) field.getLifeByTemplateId(8800103);
                } else if (mob.getTemplateId() == 8800103) {
                    fusionMob = (Mob) field.getLifeByTemplateId(8800107);
                } else if (mob.getTemplateId() == 8800108) {
                    fusionMob = (Mob) field.getLifeByTemplateId(8800104);
                } else if (mob.getTemplateId() == 8800104) {
                    fusionMob = (Mob) field.getLifeByTemplateId(8800108);
                } else if (mob.getTemplateId() == 8800109) {
                    fusionMob = (Mob) field.getLifeByTemplateId(8800105);
                } else if (mob.getTemplateId() == 8800105) {
                    fusionMob = (Mob) field.getLifeByTemplateId(8800109);
                }
                if (fusionMob != null) {
                    field.broadcast(MobPool.forcedSkillAction(fusionMob.getObjectId(), mobSkill.getAction() - 1));
                }
            }
        }
    }

    public static void onFuseArmSkillEffect(Char chr, Mob mob, MobSkill mobSkill) {
        Field field = chr.getField();
        if (BossConstants.isZakumArm(mob.getTemplateId())) {
            if (mobSkill.getSkillID() == MobSkillID.Damage.getVal()) {
                Mob fusionMob = null;
                //Easy Zakum
                if (mob.getTemplateId() == 8800027) {
                    fusionMob = (Mob) field.getLifeByTemplateId(8800023);
                } else if (mob.getTemplateId() == 8800023) {
                    fusionMob = (Mob) field.getLifeByTemplateId(8800027);
                } else if (mob.getTemplateId() == 8800028) {
                    fusionMob = (Mob) field.getLifeByTemplateId(8800024);
                } else if (mob.getTemplateId() == 8800024) {
                    fusionMob = (Mob) field.getLifeByTemplateId(8800028);
                } else if (mob.getTemplateId() == 8800029) {
                    fusionMob = (Mob) field.getLifeByTemplateId(8800025);
                } else if (mob.getTemplateId() == 8800025) {
                    fusionMob = (Mob) field.getLifeByTemplateId(8800029);
                }
                //Normal Zakum
                else if (mob.getTemplateId() == 8800007) {
                    fusionMob = (Mob) field.getLifeByTemplateId(8800003);
                } else if (mob.getTemplateId() == 8800003) {
                    fusionMob = (Mob) field.getLifeByTemplateId(8800007);
                } else if (mob.getTemplateId() == 8800008) {
                    fusionMob = (Mob) field.getLifeByTemplateId(8800004);
                } else if (mob.getTemplateId() == 8800004) {
                    fusionMob = (Mob) field.getLifeByTemplateId(8800008);
                } else if (mob.getTemplateId() == 8800009) {
                    fusionMob = (Mob) field.getLifeByTemplateId(8800005);
                } else if (mob.getTemplateId() == 8800005) {
                    fusionMob = (Mob) field.getLifeByTemplateId(8800009);
                }
                //Chaos Zakum
                else if (mob.getTemplateId() == 8800107) {
                    fusionMob = (Mob) field.getLifeByTemplateId(8800103);
                } else if (mob.getTemplateId() == 8800103) {
                    fusionMob = (Mob) field.getLifeByTemplateId(8800107);
                } else if (mob.getTemplateId() == 8800108) {
                    fusionMob = (Mob) field.getLifeByTemplateId(8800104);
                } else if (mob.getTemplateId() == 8800104) {
                    fusionMob = (Mob) field.getLifeByTemplateId(8800108);
                } else if (mob.getTemplateId() == 8800109) {
                    fusionMob = (Mob) field.getLifeByTemplateId(8800105);
                } else if (mob.getTemplateId() == 8800105) {
                    fusionMob = (Mob) field.getLifeByTemplateId(8800109);
                }
                if (fusionMob != null) {
                    mobSkill.applyEffect(fusionMob);
                }
            }
        }
    }
}
