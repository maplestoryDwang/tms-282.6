package net.swordie.ms.handlers.life;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.skills.ShootObject;
import net.swordie.ms.client.character.skills.ShootObjectSkillInfo;
import net.swordie.ms.client.jobs.Job;
import net.swordie.ms.client.jobs.flora.Illium;
import net.swordie.ms.client.jobs.nova.Kaiser;
import net.swordie.ms.client.jobs.resistance.BattleMage;
import net.swordie.ms.connection.InPacket;
import net.swordie.ms.connection.packet.Summoned;
import net.swordie.ms.connection.packet.UserLocal;
import net.swordie.ms.enums.AssistType;
import net.swordie.ms.handlers.Handler;
import net.swordie.ms.handlers.header.InHeader;
import net.swordie.ms.life.Life;
import net.swordie.ms.life.Summon;
import net.swordie.ms.life.movement.MovementInfo;
import net.swordie.ms.world.field.Field;

public class SummonedHandler {

    @Handler(op = InHeader.SUMMONED_MOVE)
    public static void handleSummonedMove(Char chr, InPacket inPacket) {
        // CVecCtrlSummoned::EndUpdateActive
        int summonID = inPacket.decodeInt();
        if (chr != null) {
            Life life = chr.getField().getLifeByObjectID(summonID);
            if (life instanceof Summon summon) {
                if (summon.getChr() == null) {
                    summon.handleRemove();
                    return;
                }
                MovementInfo movementInfo = new MovementInfo(inPacket);
                movementInfo.applyTo(summon);
                chr.getField().broadcast(Summoned.move(chr.getId(), summonID, movementInfo), chr);
            }
        }
    }


    @Handler(op = InHeader.SUMMONED_REMOVE)
    public static void handleSummonedRemove(Char chr, InPacket inPacket) {
        int id = inPacket.decodeInt();
        inPacket.decodeInt(); // skillId

        Life life = chr.getField().getLifeByObjectID(id);
        if (life instanceof Summon summon) {
            if (summon.getChr() == null) {
                summon.handleRemove();
                return;
            }
            if (summon.getOwnerId() != chr.getId()) {
                return;
            }
            int skillId = summon.getSkillID();
            if (skillId == BattleMage.CONDEMNATION
                    || skillId == BattleMage.CONDEMNATION_I
                    || skillId == BattleMage.CONDEMNATION_II
                    || skillId == BattleMage.CONDEMNATION_III
                    || skillId == BattleMage.CONDEMNATION_VI) {
                if (chr.getJobHandler() instanceof BattleMage battleMage) {
                    battleMage.removeCondemnationBuff(summon);
                }
            } else if (summon.getAssistType() == AssistType.CreateShootObj) {
                return;
            }
            chr.getField().removeLife(id, false);
        }
    }

    @Handler(op = InHeader.SUMMONED_HIT)
    public static void handleSummonedHit(Char chr, InPacket inPacket) {
        Field field = chr.getField();

        int summonObjId = inPacket.decodeInt();
        byte attackId = inPacket.decodeByte();
        int damage = inPacket.decodeInt();
        int mobTemplateId = inPacket.decodeInt();
        boolean isLeft = inPacket.decodeByte() != 0;

        Life life = field.getLifeByObjectID(summonObjId);
        if (life instanceof Summon summon) {
            if (summon.getChr() == null) {
                summon.handleRemove();
            } else if (summon.getOwnerId() == chr.getId()) {
                summon.onHit(damage, mobTemplateId);
            }
        }
    }

    @Handler(op = InHeader.SUMMONED_SKILL)
    public static void handleSummonedSkill(Char chr, InPacket inPacket) {
        Field field = chr.getField();
        int objectID = inPacket.decodeInt();
        int skillId = inPacket.decodeInt();
        inPacket.decodeByte();
        inPacket.decodeByte(); // moveAction
        inPacket.decodeInt(); // tAttackAfter
        if (field.getLifeByObjectID(objectID) != null && field.getLifeByObjectID(objectID) instanceof Summon summon) {
            summon.onSkillUse(chr, skillId, inPacket);
        }
    }

    @Handler(op = InHeader.SUMMONED_ACTION)
    public static void handleSummonedAction(Char chr, InPacket inPacket) {
        Field field = chr.getField();

        int summonID = inPacket.decodeInt();
        byte action = inPacket.decodeByte();

        if (field.getLifeByObjectID(summonID) != null && field.getLifeByObjectID(summonID) instanceof Summon summon) {
            chr.getField().broadcast(Summoned.actionChange(summon, action));
        }
    }

    @Handler(op = InHeader.SUMMONED_SKILL_ATTACK)
    public static void handleSummonedSkillAttack(Char chr, InPacket inPacket) {
        int summonObjId = inPacket.decodeInt();
        inPacket.decodeInt(); // update_time
        int summonSkillId = inPacket.decodeInt(); // Skill ID the Summon originates from
        int summonAttackId = inPacket.decodeInt(); // Attack Skill Id
        if (summonSkillId == Illium.CRYSTALLINE_SPIRIT) {
            if (summonAttackId == Illium.REACTION_DESTRUCTION_II) {
                if (chr.getJobHandler() instanceof Illium illium) {
                    illium.doCrystallineDestruction(summonObjId, summonAttackId);
                }
            }
        }
        if (summonSkillId == Illium.DEPLOY_CRYSTAL) {
            if (summonAttackId == Illium.REACTION_DESTRUCTION_II) {
                return;
            }
        }
        Summon summon = (Summon) chr.getField().getLifeByObjectID(summonObjId);
        if (summon != null) {
            if (summon.getChr() == null) {
                summon.handleRemove();
            } else if (summon.getOwnerId() == chr.getId() && summon.getSkillID() == summonSkillId) {
                summon.onAttack(chr, summonAttackId);
            }
        }
    }

    @Handler(op = InHeader.SUMMONED_SEQUENCE_ATTACK)
    public static void handleSquenceAttack(Char chr, InPacket inPacket) {
        Field field = chr.getField();

        int summonID = inPacket.decodeInt();
        inPacket.decodeInt(); // 11?
        int summonSkillID = inPacket.decodeInt();
        int summonLinkingSkillID = inPacket.decodeInt();
        int unk = inPacket.decodeInt();
        int x = inPacket.decodeInt();
        int y = inPacket.decodeInt();

        if (field.getLifeByObjectID(summonID) != null
                && field.getLifeByObjectID(summonID) instanceof Summon summon) {
            if (summon.getSkillID() == summonSkillID && chr.hasSkill(summonLinkingSkillID)) {
                if (!chr.hasSkillOnCooldown(summonLinkingSkillID)) {
                    chr.setSkillCooldown(summonLinkingSkillID, chr.getSkillLevel(summonLinkingSkillID));
                }
            }
        }
    }

    @Handler(op = InHeader.RECALL_SUMMON)
    public static void handleRecallSummon(Char chr, InPacket inPacket) {
        int skillId = inPacket.decodeInt();
        int slv = inPacket.decodeInt();
        byte isLeft = inPacket.decodeByte();
        short unk = inPacket.decodeShort();

        if (!chr.hasSkill(skillId)) {
            return;
        }

        switch (skillId) {
            case Kaiser.NOVA_GUARDIANS:
                if (chr.getJobHandler() instanceof Kaiser kaiser) {
                    kaiser.recallNovaGuardians();
                }
                break;
        }
    }

    @Handler(op = InHeader.SUMMONED_CREATE_SHOOT_OBJ)
    public static void handleSummonedCreateShootOBj(Char chr, InPacket inPacket) {
        Field field = chr.getField();
        ShootObjectSkillInfo shootObjectSkillInfo = new ShootObjectSkillInfo(chr.getId());

        int summonObjId = inPacket.decodeInt();
        Life life = field.getLifeByObjectID(summonObjId);
        if (!(life instanceof Summon) || ((Summon) life).getChr() != chr) {
            return;
        }
        int action = inPacket.decodeByte();
        shootObjectSkillInfo.setSummonOwner((Summon) life);

        shootObjectSkillInfo.setSkillId(inPacket.decodeInt());
        shootObjectSkillInfo.setSlv(inPacket.decodeInt());
        shootObjectSkillInfo.setAction(inPacket.decodeInt());
        shootObjectSkillInfo.setActionSpeed(inPacket.decodeInt());
        shootObjectSkillInfo.setProjectileItemId(inPacket.decodeInt()); // unsure
        shootObjectSkillInfo.setPosition(inPacket.decodePosition());

        shootObjectSkillInfo.extraEncodeInt1 = inPacket.decodeInt();
        shootObjectSkillInfo.extraEncodeInt2 = inPacket.decodeInt();
        shootObjectSkillInfo.extraEncodeInt3 = inPacket.decodeInt(); // xPos int
        shootObjectSkillInfo.extraEncodeInt4 = inPacket.decodeInt(); // yPos int
        shootObjectSkillInfo.extraEncodeInt5 = inPacket.decodeInt();
        shootObjectSkillInfo.extraEncodeInt6 = inPacket.decodeInt();
        shootObjectSkillInfo.extraEncodeByte1 = inPacket.decodeByte();
        shootObjectSkillInfo.extraEncodeInt7 = inPacket.decodeInt();
        shootObjectSkillInfo.extraEncodeByte2 = inPacket.decodeByte();
        shootObjectSkillInfo.extraEncodeLong1 = inPacket.decodeLong();
        shootObjectSkillInfo.extraEncodeInt8 = inPacket.decodeInt();
        shootObjectSkillInfo.extraEncodeInt9 = inPacket.decodeInt();

        int loopSize = inPacket.decodeInt();
        for (int i = 0; i < loopSize; i++) {
            ShootObject shootObject = new ShootObject(chr, inPacket);
            shootObjectSkillInfo.getShootObjects().add(shootObject);
        }

        Job jobHandler = chr.getJobHandler();
        jobHandler.handleShootObject(chr, shootObjectSkillInfo);

        chr.write(UserLocal.shootObjectCreated(shootObjectSkillInfo));
        field.broadcast(Summoned.doSkill((Summon) life, (byte) action, shootObjectSkillInfo.getSkillId(), shootObjectSkillInfo), chr);
    }
}
