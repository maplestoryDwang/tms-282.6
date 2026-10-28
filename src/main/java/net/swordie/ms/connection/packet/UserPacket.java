package net.swordie.ms.connection.packet;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.achievement.AchievementData;
import net.swordie.ms.client.character.items.Item;
import net.swordie.ms.client.character.skills.jupiterthunder.JupiterThunder;
import net.swordie.ms.client.character.skills.jupiterthunder.JupiterThunderUpdateInfo;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.enums.ChatUserType;
import net.swordie.ms.handlers.header.OutHeader;
import net.swordie.ms.life.Summon;
import net.swordie.ms.life.mob.Mob;
import net.swordie.ms.loaders.StringData;
import net.swordie.ms.loaders.containerclasses.MobSkillInfo;
import net.swordie.ms.util.Position;

import java.util.Collections;
import java.util.List;

/**
 * Created on 2/3/2018.
 */
public class UserPacket {

    /**
     * Creates a chat packet.
     *
     * @param chr         the Char
     * @param type        the type of the chat
     * @param msg         the message that the Char said
     * @param onlyBalloon what to show (0 = nothing, 1 = just chat, 2 = just balloon, 3 = both)
     * @param idk         idk
     * @param worldID     world id of the Char
     * @return the created outpacket
     */
    public static OutPacket chat(Char chr, ChatUserType type, String msg, int onlyBalloon, int idk, int worldID) {
        OutPacket outPacket = new OutPacket(OutHeader.CHAT);

        outPacket.encodeInt(chr.getId());

        outPacket.encodeByte(type.getVal());
        outPacket.encodeString(msg);
        chr.encodeChatInfo(outPacket, msg);
        outPacket.encodeByte(onlyBalloon); // 3
        outPacket.encodeByte(idk); // 0
        outPacket.encodeByte(worldID); // 19
        outPacket.encodeByte(0); // 0

        return outPacket;
    }

    public static OutPacket setADBoard(Char chr, boolean open) {
        OutPacket outPacket = new OutPacket(OutHeader.AD_BOARD);

        outPacket.encodeInt(chr.getId());

        outPacket.encodeByte(open);
        if (open) {
            outPacket.encodeString(chr.getADBoardRemoteMsg());
        }

        return outPacket;
    }

    public static OutPacket setConsumeItemEffect(int charID, int itemID) {
        OutPacket outPacket = new OutPacket(OutHeader.SET_CONSUME_ITEM_EFFECT);

        outPacket.encodeInt(charID);
        outPacket.encodeInt(itemID);

        return outPacket;
    }

    public static OutPacket showItemUpgradeEffect(int charID, boolean success, boolean enchantDlg, int uItemID, int eItemID, boolean boom) {
        OutPacket outPacket = new OutPacket(OutHeader.SHOW_ITEM_UPGRADE_EFFECT);

        outPacket.encodeInt(charID);

        outPacket.encodeByte(boom ? 2 : success ? 1 : 0);
        outPacket.encodeByte(enchantDlg);
        outPacket.encodeInt(uItemID);
        outPacket.encodeInt(eItemID);
        outPacket.encodeInt(0);
        outPacket.encodeByte(0);
        outPacket.encodeByte(0);

        return outPacket;
    }


    public static OutPacket showItemSkillSocketUpgradeEffect(int charID, boolean success) {
        OutPacket outPacket = new OutPacket(OutHeader.SHOW_ITEM_SKILL_SOCKET_UPGRADE_EFFECT);

        outPacket.encodeInt(charID);

        outPacket.encodeByte(success);

        return outPacket;
    }

    public static OutPacket showItemSkillOptionUpgradeEffect(int charID, boolean success, boolean boom, int itemID, int soulID) {
        OutPacket outPacket = new OutPacket(OutHeader.SHOW_ITEM_SKILL_OPTION_UPGRADE_EFFECT);

        outPacket.encodeInt(charID);

        outPacket.encodeByte(success);
        outPacket.encodeByte(boom);
        outPacket.encodeInt(itemID);
        outPacket.encodeInt(soulID); // The effect of the Soul has been applied.

        return outPacket;
    }

    public static OutPacket showItemReleaseEffect(int charID, short pos, boolean bonus) {
        OutPacket outPacket = new OutPacket(OutHeader.SHOW_ITEM_RELEASE_EFFECT);

        outPacket.encodeInt(charID);

        outPacket.encodeShort(pos);
        outPacket.encodeByte(bonus);

        return outPacket;
    }

    public static OutPacket showItemUnReleaseEffect(int charID, boolean success, int cItemID, int mgItemID, int eItemID) {
        OutPacket outPacket = new OutPacket(OutHeader.SHOW_ITEM_UNRELEASE_EFFECT);

        outPacket.encodeInt(charID);

        outPacket.encodeByte(success);
        outPacket.encodeInt(cItemID);
        outPacket.encodeInt(mgItemID);
        outPacket.encodeInt(eItemID);

        return outPacket;
    }

    public static OutPacket showItemLuckyItemEffect(int charID, boolean success, int itemID) {
        OutPacket outPacket = new OutPacket(OutHeader.SHOW_ITEM_LUCKY_ITEM_EFFECT);

        outPacket.encodeInt(charID);

        outPacket.encodeByte(success ? 1 : 0);
        outPacket.encodeInt(itemID);

        return outPacket;
    }

    public static OutPacket showItemMemorialEffect(int charID, boolean success, int itemID, int ePos, int uPos) {
        OutPacket outPacket = new OutPacket(OutHeader.SHOW_ITEM_MEMORIAL_EFFECT);

        outPacket.encodeInt(charID);

        outPacket.encodeByte(success);
        outPacket.encodeInt(itemID);
        outPacket.encodeInt(ePos);
        outPacket.encodeInt(uPos);

        return outPacket;
    }

    public static OutPacket showItemAdditionalUnReleaseEffect(int charID, boolean success, int itemID, int ePos, int uPos) {
        OutPacket outPacket = new OutPacket(OutHeader.SHOW_ITEM_ADDITIONAL_UN_RELEASE_EFFECT);

        outPacket.encodeInt(charID);

        outPacket.encodeByte(success);
        outPacket.encodeInt(itemID);
        outPacket.encodeInt(ePos);
        outPacket.encodeInt(uPos);

        return outPacket;
    }

    public static OutPacket showItemAdditionalSlotExtendEffect(int charID, boolean success, int itemID) {
        OutPacket outPacket = new OutPacket(OutHeader.SHOW_ITEM_ADDITIONAL_SLOT_EXTEND_EFFECT);

        outPacket.encodeInt(charID);

        outPacket.encodeByte(success ? 1 : 0);
        outPacket.encodeInt(itemID);

        return outPacket;
    }

    public static OutPacket setActiveDamageSkin(Char chr) {
        OutPacket outPacket = new OutPacket(OutHeader.SET_DAMAGE_SKIN);

        outPacket.encodeInt(chr.getId());

        outPacket.encodeInt(chr.getActiveDamageSkin().getDamageSkinID());
        outPacket.encodeString("");
        outPacket.encodeString("");

        return outPacket;
    }

    public static OutPacket SetSoulEffect(int charID, boolean set) {
        OutPacket outPacket = new OutPacket(OutHeader.SET_SOUL_EFFECT);

        outPacket.encodeInt(charID);

        outPacket.encodeByte(set);
        outPacket.encodeByte(1); // SetExclRequestSent

        return outPacket;
    }

    public static OutPacket createSpiritFlow(Char chr, int skillID, int duration) {
        OutPacket outPacket = new OutPacket(OutHeader.CREATE_SPIRIT_FLOW);

        outPacket.encodeInt(chr.getId());

        outPacket.encodeInt(skillID);
        outPacket.encodeInt(duration);

        return outPacket;
    }

    public static OutPacket nebuliteFuseResult(Char chr, boolean isSuccess, int itemID) {
        OutPacket outPacket = new OutPacket(OutHeader.NEBULITE_FUSE_RESULT);

        outPacket.encodeInt(chr.getId());

        outPacket.encodeByte(isSuccess);
        outPacket.encodeInt(itemID);

        return outPacket;
    }

    public static OutPacket setStigmaEffect(Char chr) {
        OutPacket outPacket = new OutPacket(OutHeader.STIGMA_EFFECT);

        outPacket.encodeInt(chr.getId());

        outPacket.encodeByte(true);

        return outPacket;
    }

    public static OutPacket setGachaponEffect(Char chr) {
        OutPacket outPacket = new OutPacket(OutHeader.GACHAPON_EFFECT);

        outPacket.encodeInt(chr.getId());

        return outPacket;
    }

    public static OutPacket effect(Effect effect) {
        OutPacket outPacket = new OutPacket(OutHeader.EFFECT);

        effect.encode(outPacket);
        outPacket.encodeInt(-1); // v265.1

        return outPacket;
    }

    public static OutPacket followCharacter(int charID, int drivercharID, boolean transferField, Position position) {
        OutPacket outPacket = new OutPacket(OutHeader.FOLLOW_CHARACTER);

        outPacket.encodeInt(charID);

        outPacket.encodeInt(drivercharID);
        if (drivercharID < 0) {
            outPacket.encodeByte(transferField);
            if (transferField) {
                outPacket.encodePositionInt(position);
            }
        }

        return outPacket;
    }

    public static OutPacket userHitByCounter(int charID, int damage) {
        OutPacket outPacket = new OutPacket(OutHeader.USER_HIT_BY_COUNTER);

        outPacket.encodeInt(charID);
        outPacket.encodeInt(damage);

        return outPacket;
    }

    public static OutPacket tossedByMobSkill(int charID, Mob mob, MobSkillInfo msi, int impact) {
        OutPacket outPacket = new OutPacket(OutHeader.TOSSED_BY_MOB_SKILL);

        outPacket.encodeInt(charID);

        outPacket.encodeInt(mob.getObjectId());
        outPacket.encodeInt(msi.getId());
        outPacket.encodeInt(msi.getLevel());
        outPacket.encodeInt(impact);
        outPacket.encodeInt(0); // v212+

        return outPacket;
    }

    public static OutPacket itemLinkedChat(Char chr, ChatUserType type, String msg, int onlyBalloon, int idk,
                                           int worldID, Item item) {
        OutPacket outPacket = new OutPacket(OutHeader.ITEM_LINKED_CHAT);

        outPacket.encodeInt(chr.getId());
        outPacket.encodeByte(type.getVal());
        outPacket.encodeString(msg);
        chr.encodeChatInfo(outPacket, msg);
        outPacket.encodeByte(onlyBalloon);
        outPacket.encodeByte(idk);
        outPacket.encodeByte(worldID);
        outPacket.encodeInt(item != null ? 1 : 0);
        if (item != null) {
            outPacket.encodeByte(true);
            item.encode(outPacket);
            outPacket.encodeString(""); // other name
            outPacket.encodeString("");
            outPacket.encodeByte(0);
        }

        return outPacket;
    }

    public static OutPacket RWZeroBunkerMobBind(int charID, Mob mob, boolean isSuccess) {
        OutPacket outPacket = new OutPacket(OutHeader.RW_ZERO_BUNKER_MOB_BIND);

        outPacket.encodeInt(charID);

        outPacket.encodeByte(isSuccess);
        outPacket.encodeByte(mob.isAlive());
        outPacket.encodeInt(mob.getObjectId());
        outPacket.encodeInt(mob.getTemplateId());
        outPacket.encodePositionInt(mob.getPosition());

        return outPacket;
    }

    public static OutPacket teslaTriangle(List<Summon> rockNshockLifes, int charID) {
        OutPacket outPacket = new OutPacket(OutHeader.TESLA_TRIANGLE);

        outPacket.encodeInt(charID);

        for (int i = 0; i < 3; i++) {
            outPacket.encodeInt(rockNshockLifes.get(i).getObjectId());
        }
        return outPacket;
    }

    public static OutPacket gatherResult(int charID, boolean success) {
        OutPacket outPacket = new OutPacket(OutHeader.GATHER_RESULT);

        outPacket.encodeInt(charID);

        outPacket.encodeByte(success);

        return outPacket;
    }

    public static OutPacket categoryEventNameTag(int charID, int nCategory, String sMsg, int nIdx) {
        OutPacket outPacket = new OutPacket(OutHeader.CATEGORY_EVENT_NAME_TAG);

        outPacket.encodeInt(charID);
        outPacket.encodeByte(nIdx);         //nIdx = -1 is disable
        outPacket.encodeString(sMsg);
        outPacket.encodeByte(nCategory);

        return outPacket;
    }

    public static OutPacket skillOnOffEffect(int charID, int skillID) {
        OutPacket outPacket = new OutPacket(OutHeader.SKILL_ON_OFF_EFFECT);

        outPacket.encodeInt(charID);
        outPacket.encodeByte(0);
        outPacket.encodeInt(skillID);

        return outPacket;
    }

    public static OutPacket setMesoChairCount(int charID, int meso) {
        OutPacket outPacket = new OutPacket(OutHeader.ADD_MESO_CHAIR_COUNT);

        outPacket.encodeInt(charID);
        outPacket.encodeInt(meso);
        outPacket.encodeLong(0);
        outPacket.encodeLong(0);

        return outPacket;
    }

    public static OutPacket shootObjectExplodeResult(int chrId, List<Integer> shootObjectIdList) {
        OutPacket outPacket = new OutPacket(OutHeader.SHOOT_OBJECT_EXPLODE_RESULT);

        outPacket.encodeInt(chrId);
        outPacket.encodeInt(shootObjectIdList.size());
        for (int shootObjId : shootObjectIdList) {
            outPacket.encodeInt(shootObjId);
        }

        return outPacket;
    }

    public static OutPacket shapeShiftResult(int charId, boolean bEnable) {
        OutPacket outPacket = new OutPacket(OutHeader.SHAPESHIFT_RESULT);

        outPacket.encodeInt(charId);
        outPacket.encodeByte(bEnable);

        return outPacket;
    }

    public static OutPacket jupiterThunderCreated(Char chr, JupiterThunder jupiterThunder) {
        return jupiterThunderCreated(chr, Collections.singletonList(jupiterThunder));
    }

    public static OutPacket jupiterThunderCreated(Char chr, List<JupiterThunder> jupiterThunderList) {
        OutPacket outPacket = new OutPacket(OutHeader.JUPITER_THUNDER_CREATED);

        outPacket.encodeInt(chr.getId());
        outPacket.encodeInt(jupiterThunderList.size()); // loop size
        for (var jt : jupiterThunderList) {
            outPacket.encode(jt);
        }

        return outPacket;
    }

    public static OutPacket jupiterThunderRemoved(JupiterThunder jupiterThunder) {
        OutPacket outPacket = new OutPacket(OutHeader.JUPITER_THUNDER_REMOVED);

        outPacket.encodeInt(jupiterThunder.getOwnerId());
        var size = 1;
        outPacket.encodeInt(size); // size
        for (var i = 0; i < size; i++) {
            outPacket.encodeInt(jupiterThunder.getObjectId());
        }

        return outPacket;
    }

    public static OutPacket jupiterThunderUpdateResult(JupiterThunder jupiterThunder, JupiterThunderUpdateInfo jtui) {
        OutPacket outPacket = new OutPacket(OutHeader.JUPITER_THUNDER_UPDATE_RESULT);

        outPacket.encodeInt(jupiterThunder.getOwnerId());
        var size = 1;
        outPacket.encodeInt(size); // size
        for (var i = 0; i < size; i++) {
            outPacket.encode(jtui);
        }

        return outPacket;
    }

    public static OutPacket inHumanSpeedResult(Char chr, int nextTime) {
        OutPacket outPacket = new OutPacket(OutHeader.INHUMAN_SPEED_RESULT);

        outPacket.encodeInt(chr.getId());
        outPacket.encodeByte(true);
        outPacket.encodeInt(nextTime); // updateTime + nextTime

        return outPacket;
    }

    public static OutPacket characterSelfInfo(int unk, boolean bool) {
        OutPacket outPacket = new OutPacket(OutHeader.CHARACTER_INFO_LOCAL);

        outPacket.encodeByte(0);
        outPacket.encodeInt(0);
        outPacket.encodeInt(unk);
        outPacket.encodeByte(bool);

        return outPacket;
    }
}
