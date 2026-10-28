package net.swordie.ms.connection.packet;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.CharacterStat;
import net.swordie.ms.client.character.PortableChair;
import net.swordie.ms.client.character.avatar.AvatarLook;
import net.swordie.ms.client.character.info.ZeroInfo;
import net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat;
import net.swordie.ms.client.character.skills.temp.TemporaryStatManager;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.protocol.codec.ProfileEncoders;
import net.swordie.ms.connection.protocol.codec.UserPoolPacketEncoder;
import net.swordie.ms.constants.JobConstants;
import net.swordie.ms.enums.ChairType;
import net.swordie.ms.enums.TSIndex;
import net.swordie.ms.handlers.header.OutHeader;
import net.swordie.ms.life.Familiar;
import net.swordie.ms.life.pet.Pet;

public class UserPool {
    public static OutPacket userEnterField(Char chr) {
        var profile = ProfileEncoders.find(UserPoolPacketEncoder.class);
        if (profile.isPresent()) {
            return profile.get().userEnterField(chr);
        }
        OutPacket outPacket = new OutPacket(OutHeader.USER_ENTER_FIELD);

        CharacterStat cs = chr.getAvatarData().getCharacterStat();
        AvatarLook al = chr.getAvatarData().getAvatarLook();
        TemporaryStatManager tsm = chr.getTemporaryStatManager();

        outPacket.encodeInt(chr.getUser().getId());
        outPacket.encodeInt(0);
        outPacket.encodeInt(chr.getId());
        outPacket.encodeByte(0);
        outPacket.encodeInt(chr.getGuildID());
        // CUserPool::OnUserInit
        outPacket.encodeInt(chr.getLevel());
        outPacket.encodeString(chr.getName());
        outPacket.encodeString(""); // parent name, deprecated
        if (chr.getGuild() != null) {
            chr.getGuild().encodeForRemote(outPacket);
        } else {
            outPacket.encodeInt(0);
            outPacket.encodeString("");
            outPacket.encodeShort(0);
            outPacket.encodeByte(0);
            outPacket.encodeShort(0);
            outPacket.encodeByte(0);
            outPacket.encodeInt(0);
            outPacket.encodeInt(0);
        }
        outPacket.encodeByte(cs.getGender());
        outPacket.encodeInt(cs.getPop());
        outPacket.encodeInt(0); // nNameTagMark
        outPacket.encodeByte(0); // v214
        outPacket.encodeInt(0); // v214
        tsm.encodeForRemote(outPacket, tsm.getRemoteStats());
        outPacket.encodeShort(chr.getJob());
        outPacket.encodeShort(cs.getSubJob());
        outPacket.encodeInt(chr.getTotalChuc(false));
        outPacket.encodeInt(chr.getTotalArc());
        outPacket.encodeInt(chr.getTotalAut());
        if (chr.getAvatarData().getZeroAvatarLook() != null) {
            //Change Current Zero Character Weapon Encode.
            ZeroInfo zeroInfo = chr.getZeroInfo();
            if (zeroInfo != null && zeroInfo.isZeroBetaState()) {
                al = chr.getAvatarData().getZeroAvatarLook();
            }
        }
        al.encode(outPacket);
        if (JobConstants.isZero(chr.getJob())) {
            chr.getAvatarData().getZeroAvatarLook().encode(outPacket);
        }
        outPacket.encodeInt(chr.getDriverID()); // 0
        outPacket.encodeInt(chr.getPassengerID()); // dwPassenserID
        outPacket.encodeByte(1); // ?

        // sub_141297170
        outPacket.encodeInt(0);
        outPacket.encodeInt(0);
        int size = 0;
        outPacket.encodeInt(size);
        for (int i = 0; i < size; i++) {
            outPacket.encodeInt(0);
            outPacket.encodeInt(0);
        }

        outPacket.encodeInt(chr.getChocoCount());
        outPacket.encodeInt(chr.getActiveEffectItemID());
        outPacket.encodeInt(chr.getMonkeyEffectItemID());
        outPacket.encodeInt(chr.getActiveNickItemID()); // custom Title (3700623)
        outPacket.encodeInt(0);
        outPacket.encodeInt(0);
        boolean bool = false;
        outPacket.encodeByte(bool);
        if (bool) {
            outPacket.encodeString("");
        }
        outPacket.encodeByte(bool);
        if (bool) {
            outPacket.encodeString("");
        }
        if (chr.getActiveNickItemID() == 3700623) {
            chr.encodeCustomNickName(outPacket);
        }
        outPacket.encodeInt(chr.getActiveDamageSkin() == null ? 0 : chr.getActiveDamageSkin().getDamageSkinID());
        outPacket.encodeString(""); // damage skin?
        outPacket.encodeString(""); // premium damage skin?
        outPacket.encodeInt(al.getDemonWingID());
        outPacket.encodeInt(al.getKaiserWingID());
        outPacket.encodeInt(al.getKaiserTailID());
        outPacket.encodeByte(JobConstants.isHoYoung(chr.getJob()));
        outPacket.encodeByte(0);
        outPacket.encodeInt(chr.getCompletedSetItemID());
        outPacket.encodeShort(chr.getFieldSeatID());
        PortableChair chair = chr.getChair() != null ? chr.getChair() : new PortableChair(chr, 0, ChairType.None);
        outPacket.encodeInt(chair.getItemID());
        outPacket.encodeInt(0);
        outPacket.encodePosition(chr.getPosition());
        outPacket.encodeByte(chr.getMoveAction());
        outPacket.encodeShort(chr.getFoothold());

        outPacket.encodeByte(0); // sub_143293E00
        outPacket.encodeByte(0); // sub_143293E10
        outPacket.encodeByte(0); // sub_14497CE70

        outPacket.encodeByte(chair.getType() != ChairType.None);
        if (chair.getType() != ChairType.None) {
            chair.encode(outPacket);
        }
        for (Pet pet : chr.getPets()) {
            outPacket.encodeByte(true);
            outPacket.encodeInt(pet.getIdx());
            pet.encode(outPacket);
        }
        outPacket.encodeByte(false); // indicating that pets are no longer being encoded

        bool = false;
        outPacket.encodeByte(bool);
        if (bool) {
            // virtual function :(
        }

        // TODO
        //List<Familiar> famList = chr.getFamiliarCodexManager().getActiveFamiliarsToSummon();
        size = 0;//famList.size();
        //if (!chr.getFamiliarCodexManager().isFamiliarsSummoned()) {
        //    size = 0;
        //}
        outPacket.encodeByte(size);
        for (int i = 0; i < size; i++) {
            Familiar f = null;//famList.get(i);
            outPacket.encodeByte(true); // show
            f.encodeForRemote(outPacket);
        }

        outPacket.encodeInt(chr.getTamingMobLevel());
        outPacket.encodeInt(chr.getTamingMobExp());
        outPacket.encodeInt(chr.getTamingMobFatigue());

        bool = false;
        outPacket.encodeByte(bool);
        if (bool) {
            // virtual function :(
        }

        byte miniRoomType = chr.getMiniRoom() != null ? chr.getMiniRoom().getType() : 0;
        outPacket.encodeByte(miniRoomType);
        if (miniRoomType > 0) {
            chr.getMiniRoom().encode(outPacket);
            chr.encodeChatInfo(outPacket, chr.getMiniRoom().getMsg());
        }
        outPacket.encodeByte(chr.getADBoardRemoteMsg() != null);
        if (chr.getADBoardRemoteMsg() != null) {
            outPacket.encodeString(chr.getADBoardRemoteMsg());
        }
        outPacket.encodeByte(chr.isInCouple());
        if (chr.isInCouple()) {
            chr.getCouple().encodeForRemote(outPacket);
        }
        outPacket.encodeByte(chr.hasFriendshipItem());
        if (chr.hasFriendshipItem()) {
            chr.getFriendshipRingRecord().encode(outPacket);
        }
        outPacket.encodeByte(chr.isMarried());
        if (chr.isMarried()) {
            chr.getMarriageRecord().encodeForRemote(outPacket);
        }
        bool = true;
        outPacket.encodeByte(bool); // v212+
        if (bool) {
            outPacket.encodeInt(0); // size
            // CSecondAtom::OnEnterField
        }
        byte mask = 0;
        outPacket.encodeByte(mask);
        if ((mask & 0x8) != 0) {
            outPacket.encodeInt(0);
        } else if ((mask & 0x10) != 0) {
            outPacket.encodeInt(0);
        } else if ((mask & 0x20) != 0) {
            outPacket.encodeInt(0);
        }
        outPacket.encodeInt(chr.getEvanDragonGlide());
        if (JobConstants.isKaiser(chr.getJob())) {
            outPacket.encodeInt(chr.getKaiserMorphRotateHueExtern());
            outPacket.encodeInt(chr.getKaiserMorphPrimiumBlack());
            outPacket.encodeByte(chr.getKaiserMorphRotateHueInnner());
        }
        outPacket.encodeInt(chr.getMakingMeisterSkillEff());
        chr.initEventNameTag();
        for (int i = 0; i < 5; i++) {
            if (chr.getEventNameTag() != null && chr.getEventNameTag().getActiveNameTags().length > 0) {
                outPacket.encodeByte(chr.getEventNameTag().getActiveNameTags()[i]); // activeEventNameTag
            } else {
                outPacket.encodeByte(-1); // activeEventNameTag
            }
        }
        outPacket.encodeInt(chr.getCustomizeEffect());
        if (chr.getCustomizeEffect() > 0) {
            outPacket.encodeString(chr.getCustomizeEffectMsg());
        }
        outPacket.encodeByte(chr.getSoulEffect());
        if (tsm.hasStat(CharacterTemporaryStat.RideVehicle)) {
            int vehicleID = tsm.getTSBByTSIndex(TSIndex.RideVehicle).getNOption();
            if (vehicleID == 1932249) { // is_mix_vehicle
                size = 0;
                outPacket.encodeInt(size); // ???
                for (int i = 0; i < size; i++) {
                    outPacket.encodeInt(0);
                }
            }
        }
        outPacket.encodeInt(-1); // FF FF FF FF
        outPacket.encodeByte(false); // flashFire
        outPacket.encodeByte(true); // 1
        outPacket.encodeByte(false);

        outPacket.encodeInt(0); // CUser::DecodeTextEquipInfo
        chr.getFreezeHotEventInfo().encode(outPacket); // CUser::DecodeFreezeHotEventInfo
        outPacket.encodeInt(chr.getEventBestFriendAID()); // CUser::DecodeEventBestFriendInfo

        outPacket.encodeByte(tsm.hasStat(CharacterTemporaryStat.KinesisPsychicEnergeShield));
        outPacket.encodeByte(chr.isBeastFormWingOn());
        outPacket.encodeByte(tsm.getLarknessManager() != null && tsm.getLarknessManager().isDark());
        outPacket.encodeInt(JobConstants.isAngelicBuster(chr.getJob()) && chr.getDressUpInfo() != null ? chr.getDressUpInfo().getClothe() : 1051291);
        bool = false;
        outPacket.encodeByte(bool);
        if (bool) {
            // OnDrawEventNameTag
            outPacket.encodeInt(0);
            outPacket.encodeInt(0);
        }
        // sub_145D48A10
        chr.extraTMSSystem.encodeEnterField(outPacket);
        int someID = 0;
        outPacket.encodeInt(someID);
        if (someID > 0) {
            // ImmovableObj::Decode
            outPacket.encodeInt(0);
            outPacket.encodeInt(0);
            outPacket.encodeInt(0);
            outPacket.encodeInt(0);
            outPacket.encodeInt(0);
            outPacket.encodeInt(0);
            outPacket.encodeInt(0);
            outPacket.encodeInt(0);
            outPacket.encodeInt(0);
        }
        size = 0;
        outPacket.encodeInt(size);
        for (int i = 0; i < size; i++) {
            outPacket.encodeInt(0); // 31121054 (Blue Blood) / 400021000 (Mana Overload) / 4121054 (Bleed Dart)
            outPacket.encodeByte(0); // 1 - 0
        }
        outPacket.encodeInt(1);
        outPacket.encodeInt(0);
        outPacket.encodeString("");
        outPacket.encodeInt(0);
        someID = 0;
        outPacket.encodeInt(someID);
        if (someID > 0) {
            outPacket.encodeInt(0);
            outPacket.encodeInt(0);
            outPacket.encodeInt(0);
            outPacket.encodeShort(0);
            outPacket.encodeShort(0);
        }
        outPacket.encodeInt(0);
        // sub_14572D720
        outPacket.encodeLong(0);
        outPacket.encodeInt(-1);

        return outPacket;
    }

    public static OutPacket userLeaveField(Char chr) {
        var profile = ProfileEncoders.find(UserPoolPacketEncoder.class);
        if (profile.isPresent()) {
            return profile.get().userLeaveField(chr);
        }
        OutPacket outPacket = new OutPacket(OutHeader.USER_LEAVE_FIELD);

        outPacket.encodeInt(chr.getId());

        return outPacket;
    }
}
