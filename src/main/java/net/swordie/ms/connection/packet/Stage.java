package net.swordie.ms.connection.packet;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.damage.DamageCalc;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.protocol.codec.ProfileEncoders;
import net.swordie.ms.connection.protocol.codec.StagePacketEncoder;
import net.swordie.ms.constants.FieldConstants;
import net.swordie.ms.constants.JobConstants;
import net.swordie.ms.enums.DBChar;
import net.swordie.ms.handlers.header.OutHeader;
import net.swordie.ms.util.FileTime;
import net.swordie.ms.util.Position;
import net.swordie.ms.world.event.SunnySunday;
import net.swordie.ms.world.field.Field;
import net.swordie.ms.world.field.FieldCustom;

import java.security.SecureRandom;
import java.util.Random;

public class Stage {

    public static OutPacket setField(Char chr, Field field, int channelId, boolean dev, int oldDriverID,
                                     boolean characterData, boolean usingBuffProtector, byte portal, Position position,
                                     int mobStatAdjustRate, FieldCustom fieldCustom, int stackEventGauge) {
        if (!characterData) {
            // A profile with its own SetField (TMS 282.3 LP02EF) builds the field change; its first entry is sent by
            // its FieldEntryPolicy, so characterData = true keeps the 265 body (dropped by that profile's guard).
            var profile = ProfileEncoders.find(StagePacketEncoder.class);
            if (profile.isPresent()) {
                return profile.get().changeField(chr, field, channelId, portal & 0xFF, position, usingBuffProtector);
            }
        }
        OutPacket outPacket = new OutPacket(OutHeader.SET_FIELD);

        boolean unk = false;
        outPacket.encodeFT(FileTime.currentTime());
        outPacket.encodeByte(unk);
        outPacket.encodeInt(channelId - 1);
        outPacket.encodeByte(dev);
        outPacket.encodeInt(oldDriverID);

        outPacket.encodeByte(characterData ? 1 : 2); // 4 ? bossing
        outPacket.encodeInt(characterData ? 0 : field.getFieldType().getVal()); // unused
        outPacket.encodeInt(field.getWidth());
        outPacket.encodeInt(field.getHeight());
        outPacket.encodeByte(characterData);
        short notifierCheck = 0;
        outPacket.encodeShort(notifierCheck);
        if (notifierCheck > 0) {
            outPacket.encodeString(""); // pBlockReasonIter
            for (int i = 0; i < notifierCheck; i++) {
                outPacket.encodeString(""); // sMsg2
            }
        }

        if (characterData) {
            Random random = new SecureRandom();
            int s1 = random.nextInt();
            int s2 = random.nextInt();
            int s3 = random.nextInt();
            chr.setDamageCalc(new DamageCalc(chr, s1, s2, s3));
            outPacket.encodeInt(s1);
            outPacket.encodeInt(s2);
            outPacket.encodeInt(s3);
            chr.encode(outPacket, DBChar.All); // <<<<------------------------------------
            boolean bool1 = true;
            outPacket.encodeByte(bool1); // something with logout event
            if (bool1) {
                boolean bool2 = false;
                outPacket.encodeByte(bool2);
                if (bool2) {
                    outPacket.encodeInt(0);
                }
                outPacket.encodeFT(FileTime.MIN_TIME());
                outPacket.encodeByte(0);
                outPacket.encodeFT(FileTime.MIN_TIME());
                outPacket.encodeFT(FileTime.MIN_TIME());
                outPacket.encodeInt(-1);
            }
        } else {
            outPacket.encodeByte(usingBuffProtector);
            outPacket.encodeInt(field.getId());
            outPacket.encodeByte(portal);
            outPacket.encodeInt(chr.getAvatarData().getCharacterStat().getHp());
            outPacket.encodeInt(0);
            outPacket.encodeByte(position != null);
            if (position != null) {
                outPacket.encodeInt(position.getX());
                outPacket.encodeInt(position.getY());
            }
        }

        boolean bool = false;
        outPacket.encodeByte(bool);
        if (bool) {
            outPacket.encodeInt(0);
            outPacket.encodeInt(0);
        }
        outPacket.encodeByte(2);
        outPacket.encodeByte(0); // 1
        outPacket.encodeInt(mobStatAdjustRate); // 100

        boolean hasFieldCustom = fieldCustom != null;
        outPacket.encodeByte(hasFieldCustom);
        if (hasFieldCustom) {
            fieldCustom.encode(outPacket);
        }
        outPacket.encodeByte(false); // CWvsContext::OnInitPvPStat
        outPacket.encodeInt(0);
        outPacket.encodeByte(false);

        chr.extraTMSSystem.encodeBuffer(outPacket);

        outPacket.encodeByte(JobConstants.isExtendSpJob(chr.getJob()));
        // sub_14572D720
        outPacket.encodeLong(0);
        outPacket.encodeInt(-1);

        outPacket.encodeByte(stackEventGauge > 0);
        if (stackEventGauge > 0) {
            outPacket.encodeInt(stackEventGauge);
        }
        // start new 199
        if (FieldConstants.isVonbonField(chr.getFieldID())) {
            int size = 0;
            outPacket.encodeByte(size);
            for (int i = 0; i < size; i++) {
                outPacket.encodeString("");
            }
        }

        outPacket.encodeInt(0); // CUser::DecodeTextEquipInfo
        chr.getFreezeHotEventInfo().encode(outPacket); // CUser::DecodeFreezeHotEventInfo
        outPacket.encodeInt(chr.getEventBestFriendAID()); // CUser::DecodeEventBestFriendInfo

        bool = true;
        outPacket.encodeByte(bool);
        if (bool) {
            outPacket.encodeInt(-1);
            outPacket.encodeInt(0);
            outPacket.encodeInt(0);
            outPacket.encodeInt(999999999);
            outPacket.encodeInt(999999999);
            outPacket.encodeString("");
        }
        // sub_144B271F0
        outPacket.encodeInt(chr.getActiveDamageSkin().getDamageSkinID());
        outPacket.encodeString("");
        outPacket.encodeString("");

        // Sunny Sunday Maple!
        SunnySunday.encode(outPacket);

        outPacket.encodeInt(0); // UI_OPEN(%d)
        outPacket.encodeInt(0);
        outPacket.encodeByte(true);
        outPacket.encodeInt(0);
        // sub_140A55710
        outPacket.encodeInt(0);
        if (unk) {
            outPacket.encodeByte(0);
        }
        // nullsub_179 (Opcode: 1056)
        int size = 0;
        outPacket.encodeInt(size);
        for (int i = 0; i < size; i++) {
            outPacket.encodeInt(0);
        }
        chr.encodeCustomNickName(outPacket);

        return outPacket;
    }

    public static OutPacket setAuctionField(Char chr) {
        var profile = net.swordie.ms.connection.protocol.codec.AuctionPacketEncoder.active().setAuctionField(chr);
        if (profile.isPresent()) {
            return profile.get();
        }
        OutPacket outPacket = new OutPacket(OutHeader.SET_AUCTION_FIELD);

        outPacket.encodeFT(FileTime.currentTime());
        chr.encode(outPacket, DBChar.All);

        return outPacket;
    }

    public static OutPacket setCashShop(Char chr) {
        var profile = net.swordie.ms.connection.protocol.codec.CashShopPacketEncoder.active();
        if (profile.isPresent()) {
            return profile.get().setCashShop(chr);
        }
        OutPacket outPacket = new OutPacket(OutHeader.SET_CASH_SHOP);

        outPacket.encodeFT(FileTime.currentTime());
        chr.encode(outPacket, DBChar.All);
        outPacket.encodeByte(false);
        outPacket.encodeByte(true);
        outPacket.encodeByte(false);
        outPacket.encodeByte(false);
        //new CashShop().encodeNew(outPacket);
        outPacket.encodeArr("01 00 00 00 00 00 00 00 00 21 00 00 00 02 EE 87 23 0A 02 F4 87 23 0A 02 6F 85 23 0A 00 89 77 8E 06 00 C8 89 23 0A 00 1A 68 89 09 06 DE 9F 97 06 00 13 69 89 09 00 33 68 89 09 02 14 69 89 09 02 15 69 89 09 02 16 69 89 09 00 06 FC 8D 09 00 07 FC 8D 09 00 08 FC 8D 09 02 81 77 8E 06 02 80 D1 F0 08 02 20 58 F2 08 02 8F A4 BF 07 04 25 0E 27 07 04 27 0E 27 07 04 29 0E 27 07 04 D5 A4 BF 07 04 B9 A4 BF 07 04 BA A4 BF 07 04 E5 A4 BF 07 04 ED A4 BF 07 04 DE A4 BF 07 04 DF A4 BF 07 04 E6 A4 BF 07 04 EE A4 BF 07 04 87 D1 F0 08 04 27 58 F2 08 11 00 00 00 8F A4 BF 07 03 00 00 00 00 00 00 00 00 50 2D ED FB 06 DC 01 80 A9 A9 35 E5 0B DC 01 08 00 56 65 63 74 6F 72 52 30 00 00 00 00 00 50 BE 90 FC 1C DC 01 80 A9 3A D9 E5 21 DC 01 08 00 56 65 63 74 6F 72 52 30 00 00 00 00 00 50 4F 34 FD 32 DC 01 80 A9 CB 7C E6 37 DC 01 08 00 56 65 63 74 6F 72 52 30 80 77 8E 06 04 00 00 00 00 00 00 00 00 C0 B8 C9 A7 FB DB 01 80 29 E1 E3 E4 00 DC 01 08 00 56 65 63 74 6F 72 52 30 00 00 00 00 00 C0 49 6D A8 11 DC 01 80 29 72 87 E5 16 DC 01 08 00 56 65 63 74 6F 72 52 30 00 00 00 00 00 C0 DA 10 A9 27 DC 01 80 29 03 2B E6 2C DC 01 08 00 56 65 63 74 6F 72 52 30 00 00 00 00 00 C0 6B B4 A9 3D DC 01 80 29 94 CE E6 42 DC 01 08 00 56 65 63 74 6F 72 52 30 C0 77 8E 06 01 00 00 00 00 00 00 00 00 40 E0 FD 3B 37 4F 01 80 A9 E7 4F 24 B2 DA 01 08 00 56 65 63 74 6F 72 52 30 46 58 F2 08 01 00 00 00 00 00 00 00 00 40 E0 FD 3B 37 4F 01 80 A9 E7 4F 24 B2 DA 01 08 00 56 65 63 74 6F 72 52 30 81 77 8E 06 04 00 00 00 00 00 00 00 00 C0 B8 C9 A7 FB DB 01 80 29 E1 E3 E4 00 DC 01 08 00 56 65 63 74 6F 72 52 30 00 00 00 00 00 C0 49 6D A8 11 DC 01 80 29 72 87 E5 16 DC 01 08 00 56 65 63 74 6F 72 52 30 00 00 00 00 00 C0 DA 10 A9 27 DC 01 80 29 03 2B E6 2C DC 01 08 00 56 65 63 74 6F 72 52 30 00 00 00 00 00 C0 6B B4 A9 3D DC 01 80 29 94 CE E6 42 DC 01 08 00 56 65 63 74 6F 72 52 30 DE D1 F0 08 01 00 00 00 00 00 00 00 00 40 E0 FD 3B 37 4F 01 80 A9 E7 4F 24 B2 DA 01 08 00 56 65 63 74 6F 72 52 30 BC 77 8E 06 01 00 00 00 00 00 00 00 00 40 E0 FD 3B 37 4F 01 80 A9 E7 4F 24 B2 DA 01 08 00 56 65 63 74 6F 72 52 30 BD 77 8E 06 01 00 00 00 00 00 00 00 00 40 E0 FD 3B 37 4F 01 80 A9 E7 4F 24 B2 DA 01 08 00 56 65 63 74 6F 72 52 30 80 D1 F0 08 05 00 00 00 00 00 00 00 00 40 81 1B A8 06 DC 01 80 A9 A9 35 E5 0B DC 01 08 00 56 65 63 74 6F 72 52 30 00 00 00 00 00 70 C8 E0 5A 17 DC 01 80 69 56 B0 65 1C DC 01 08 00 56 65 63 74 6F 72 52 30 00 00 00 00 00 C0 DA 10 A9 27 DC 01 80 29 03 2B E6 2C DC 01 08 00 56 65 63 74 6F 72 52 30 00 00 00 00 00 80 87 8B 29 38 DC 01 80 E9 AF A5 66 3D DC 01 08 00 56 65 63 74 6F 72 52 30 00 00 00 00 00 40 34 06 AA 48 DC 01 80 A9 5C 20 E7 4D DC 01 08 00 56 65 63 74 6F 72 52 30 20 58 F2 08 05 00 00 00 00 00 00 00 00 40 81 1B A8 06 DC 01 80 A9 A9 35 E5 0B DC 01 08 00 56 65 63 74 6F 72 52 30 00 00 00 00 00 70 C8 E0 5A 17 DC 01 80 69 56 B0 65 1C DC 01 08 00 56 65 63 74 6F 72 52 30 00 00 00 00 00 C0 DA 10 A9 27 DC 01 80 29 03 2B E6 2C DC 01 08 00 56 65 63 74 6F 72 52 30 00 00 00 00 00 80 87 8B 29 38 DC 01 80 E9 AF A5 66 3D DC 01 08 00 56 65 63 74 6F 72 52 30 00 00 00 00 00 40 34 06 AA 48 DC 01 80 A9 5C 20 E7 4D DC 01 08 00 56 65 63 74 6F 72 52 30 87 D1 F0 08 03 00 00 00 00 00 00 00 00 40 1B 46 A4 82 DB 01 80 A9 43 60 E1 87 DB 01 08 00 56 65 63 74 6F 72 52 30 00 00 00 00 00 70 36 FF 0E B9 DB 01 80 29 2E F9 E2 BE DB 01 08 00 56 65 63 74 6F 72 52 30 00 00 00 00 00 70 C8 E0 5A 17 DC 01 80 69 56 B0 65 1C DC 01 08 00 56 65 63 74 6F 72 52 30 27 58 F2 08 03 00 00 00 00 00 00 00 00 40 1B 46 A4 82 DB 01 80 A9 43 60 E1 87 DB 01 08 00 56 65 63 74 6F 72 52 30 00 00 00 00 00 70 36 FF 0E B9 DB 01 80 29 2E F9 E2 BE DB 01 08 00 56 65 63 74 6F 72 52 30 00 00 00 00 00 70 C8 E0 5A 17 DC 01 80 69 56 B0 65 1C DC 01 08 00 56 65 63 74 6F 72 52 30 90 A4 BF 07 03 00 00 00 00 00 00 00 00 50 2D ED FB 06 DC 01 80 A9 A9 35 E5 0B DC 01 08 00 56 65 63 74 6F 72 52 30 00 00 00 00 00 50 BE 90 FC 1C DC 01 80 A9 3A D9 E5 21 DC 01 08 00 56 65 63 74 6F 72 52 30 00 00 00 00 00 50 4F 34 FD 32 DC 01 80 A9 CB 7C E6 37 DC 01 08 00 56 65 63 74 6F 72 52 30 C5 A4 BF 07 01 00 00 00 00 00 00 00 00 00 59 64 25 A9 DB 01 80 69 81 7E 62 AE DB 01 08 00 56 65 63 74 6F 72 52 30 C6 A4 BF 07 01 00 00 00 00 00 00 00 00 00 59 64 25 A9 DB 01 80 69 81 7E 62 AE DB 01 08 00 56 65 63 74 6F 72 52 30 E1 A4 BF 07 01 00 00 00 00 00 00 00 00 00 59 64 25 A9 DB 01 80 69 81 7E 62 AE DB 01 08 00 56 65 63 74 6F 72 52 30 E2 A4 BF 07 01 00 00 00 00 00 00 00 00 00 59 64 25 A9 DB 01 80 69 81 7E 62 AE DB 01 08 00 56 65 63 74 6F 72 52 30 03 00 00 00 05 00 52 6F 79 61 6C 43 00 44 6F 20 79 6F 75 20 77 61 6E 74 20 61 20 62 65 74 74 65 72 2C 20 70 72 65 74 74 69 65 72 2C 20 63 6F 6F 6C 65 72 20 63 68 61 72 61 63 74 65 72 3F 20 49 66 20 79 6F 75 20 64 6F 2C 20 43 4C 49 43 4B 21 0D 00 49 6E 66 69 6E 69 74 79 20 42 6F 6F 6B 2E 00 55 6E 6C 65 61 73 68 65 64 21 20 54 69 6D 65 20 74 6F 20 66 69 6E 64 20 55 6E 6C 65 61 73 68 65 64 20 54 72 65 61 73 75 72 65 73 21 21 21 06 00 53 63 72 6F 6C 6C 46 00 43 68 65 63 6B 20 6F 75 74 20 74 68 65 20 6E 65 77 20 73 63 72 6F 6C 6C 73 2E 20 54 68 65 79 27 72 65 20 6D 6F 72 65 20 70 6F 77 65 72 66 75 6C 20 61 6E 64 20 76 61 72 69 65 64 20 74 68 61 6E 20 65 76 65 72 21 00 00 1C 00 1C 00 00 00 00 AF 4F 00 01 00 00 00 56 9F 97 06 01 00 00 00 2C 01 00 00 00 00 00 00 00 07 00 00 00 E5 DA 34 01 E5 DA 34 01 FF FF FF FF FF FF FF FF 01 00 00 00 01 00 00 00 01 00 00 00 01 00 00 00 01 00 00 00 01 00 00 00 01 00 00 00 00 00 00 3C 00 00 00 02 AF 4F 00 01 00 00 00 57 9F 97 06 01 00 00 00 2C 01 00 00 00 00 00 00 00 07 00 00 00 E6 DA 34 01 E6 DA 34 01 FF FF FF FF FF FF FF FF 01 00 00 00 01 00 00 00 01 00 00 00 01 00 00 00 01 00 00 00 01 00 00 00 01 00 00 00 00 00 00 3C 00 00 00 03 AF 4F 00 01 00 00 00 58 9F 97 06 01 00 00 00 2C 01 00 00 00 00 00 00 00 07 00 00 00 E7 DA 34 01 E7 DA 34 01 FF FF FF FF FF FF FF FF 01 00 00 00 01 00 00 00 01 00 00 00 01 00 00 00 01 00 00 00 01 00 00 00 01 00 00 00 00 00 00 3C 00 00 00 04 AF 4F 00 01 00 00 00 59 9F 97 06 01 00 00 00 2C 01 00 00 00 00 00 00 00 07 00 00 00 E8 DA 34 01 E8 DA 34 01 FF FF FF FF FF FF FF FF 01 00 00 00 01 00 00 00 01 00 00 00 01 00 00 00 01 00 00 00 01 00 00 00 01 00 00 00 00 00 00 3C 00 00 00 05 AF 4F 00 01 00 00 00 5A 9F 97 06 01 00 00 00 2C 01 00 00 00 00 00 00 00 07 00 00 00 E9 DA 34 01 E9 DA 34 01 FF FF FF FF FF FF FF FF 01 00 00 00 01 00 00 00 01 00 00 00 01 00 00 00 01 00 00 00 01 00 00 00 01 00 00 00 00 00 00 3C 00 00 00 06 AF 4F 00 01 00 00 00 5B 9F 97 06 01 00 00 00 2C 01 00 00 00 00 00 00 00 07 00 00 00 EA DA 34 01 EA DA 34 01 FF FF FF FF FF FF FF FF 01 00 00 00 01 00 00 00 01 00 00 00 01 00 00 00 01 00 00 00 01 00 00 00 01 00 00 00 00 00 00 3C 00 00 00 07 AF 4F 00 01 00 00 00 5C 9F 97 06 01 00 00 00 2C 01 00 00 00 00 00 00 00 07 00 00 00 31 DB 34 01 31 DB 34 01 FF FF FF FF FF FF FF FF 01 00 00 00 01 00 00 00 01 00 00 00 01 00 00 00 01 00 00 00 01 00 00 00 01 00 00 00 00 00 00 3C 00 00 00 1B AF 4F 00 01 00 00 00 5D 9F 97 06 01 00 00 00 E8 03 00 00 00 00 00 00 00 07 00 00 00 E5 DA 34 01 E5 DA 34 01 FF FF FF FF FF FF FF FF 01 00 00 00 01 00 00 00 01 00 00 00 01 00 00 00 01 00 00 00 01 00 00 00 01 00 00 00 00 00 00 3C 00 00 00 1C AF 4F 00 01 00 00 00 5E 9F 97 06 01 00 00 00 E8 03 00 00 00 00 00 00 00 07 00 00 00 E6 DA 34 01 E6 DA 34 01 FF FF FF FF FF FF FF FF 01 00 00 00 01 00 00 00 01 00 00 00 01 00 00 00 01 00 00 00 01 00 00 00 01 00 00 00 00 00 00 3C 00 00 00 1D AF 4F 00 01 00 00 00 5F 9F 97 06 01 00 00 00 E8 03 00 00 00 00 00 00 00 07 00 00 00 E7 DA 34 01 E7 DA 34 01 FF FF FF FF FF FF FF FF 01 00 00 00 01 00 00 00 01 00 00 00 01 00 00 00 01 00 00 00 01 00 00 00 01 00 00 00 00 00 00 3C 00 00 00 1E AF 4F 00 01 00 00 00 60 9F 97 06 01 00 00 00 E8 03 00 00 00 00 00 00 00 07 00 00 00 E8 DA 34 01 E8 DA 34 01 FF FF FF FF FF FF FF FF 01 00 00 00 01 00 00 00 01 00 00 00 01 00 00 00 01 00 00 00 01 00 00 00 01 00 00 00 00 00 00 3C 00 00 00 1F AF 4F 00 01 00 00 00 61 9F 97 06 01 00 00 00 E8 03 00 00 00 00 00 00 00 07 00 00 00 E9 DA 34 01 E9 DA 34 01 FF FF FF FF FF FF FF FF 01 00 00 00 01 00 00 00 01 00 00 00 01 00 00 00 01 00 00 00 01 00 00 00 01 00 00 00 00 00 00 3C 00 00 00 20 AF 4F 00 01 00 00 00 62 9F 97 06 01 00 00 00 E8 03 00 00 00 00 00 00 00 07 00 00 00 EA DA 34 01 EA DA 34 01 FF FF FF FF FF FF FF FF 01 00 00 00 01 00 00 00 01 00 00 00 01 00 00 00 01 00 00 00 01 00 00 00 01 00 00 00 00 00 00 3C 00 00 00 21 AF 4F 00 01 00 00 00 63 9F 97 06 01 00 00 00 E8 03 00 00 00 00 00 00 00 07 00 00 00 31 DB 34 01 31 DB 34 01 FF FF FF FF FF FF FF FF 01 00 00 00 01 00 00 00 01 00 00 00 01 00 00 00 01 00 00 00 01 00 00 00 01 00 00 00 00 00 00 3C 00 00 00 00 AF 4F 00 01 00 00 00 FF 9F 97 06 01 00 00 00 2C 01 00 00 00 00 00 00 00 07 00 00 00 E5 DA 34 01 E6 DA 34 01 FF FF FF FF FF FF FF FF 01 00 00 00 01 00 00 00 01 00 00 00 01 00 00 00 01 00 00 00 01 00 00 00 01 00 00 00 00 00 00 3C 00 00 00 02 AF 4F 00 01 00 00 00 00 A0 97 06 01 00 00 00 2C 01 00 00 00 00 00 00 00 07 00 00 00 E6 DA 34 01 E7 DA 34 01 FF FF FF FF FF FF FF FF 01 00 00 00 01 00 00 00 01 00 00 00 01 00 00 00 01 00 00 00 01 00 00 00 01 00 00 00 00 00 00 3C 00 00 00 03 AF 4F 00 01 00 00 00 01 A0 97 06 01 00 00 00 2C 01 00 00 00 00 00 00 00 07 00 00 00 E7 DA 34 01 E8 DA 34 01 FF FF FF FF FF FF FF FF 01 00 00 00 01 00 00 00 01 00 00 00 01 00 00 00 01 00 00 00 01 00 00 00 01 00 00 00 00 00 00 3C 00 00 00 04 AF 4F 00 01 00 00 00 02 A0 97 06 01 00 00 00 2C 01 00 00 00 00 00 00 00 07 00 00 00 E8 DA 34 01 E9 DA 34 01 FF FF FF FF FF FF FF FF 01 00 00 00 01 00 00 00 01 00 00 00 01 00 00 00 01 00 00 00 01 00 00 00 01 00 00 00 00 00 00 3C 00 00 00 05 AF 4F 00 01 00 00 00 03 A0 97 06 01 00 00 00 2C 01 00 00 00 00 00 00 00 07 00 00 00 E9 DA 34 01 EA DA 34 01 FF FF FF FF FF FF FF FF 01 00 00 00 01 00 00 00 01 00 00 00 01 00 00 00 01 00 00 00 01 00 00 00 01 00 00 00 00 00 00 3C 00 00 00 06 AF 4F 00 01 00 00 00 04 A0 97 06 01 00 00 00 2C 01 00 00 00 00 00 00 00 07 00 00 00 EA DA 34 01 31 DB 34 01 FF FF FF FF FF FF FF FF 01 00 00 00 01 00 00 00 01 00 00 00 01 00 00 00 01 00 00 00 01 00 00 00 01 00 00 00 00 00 00 3C 00 00 00 07 AF 4F 00 01 00 00 00 05 A0 97 06 01 00 00 00 2C 01 00 00 00 00 00 00 00 07 00 00 00 31 DB 34 01 32 DB 34 01 FF FF FF FF FF FF FF FF 01 00 00 00 01 00 00 00 01 00 00 00 01 00 00 00 01 00 00 00 01 00 00 00 01 00 00 00 00 00 00 3C 00 00 00 1B AF 4F 00 01 00 00 00 06 A0 97 06 01 00 00 00 20 03 00 00 00 00 00 00 00 07 00 00 00 E5 DA 34 01 E6 DA 34 01 FF FF FF FF FF FF FF FF 01 00 00 00 01 00 00 00 01 00 00 00 01 00 00 00 01 00 00 00 01 00 00 00 01 00 00 00 00 00 00 3C 00 00 00 1C AF 4F 00 01 00 00 00 07 A0 97 06 01 00 00 00 20 03 00 00 00 00 00 00 00 07 00 00 00 E6 DA 34 01 E7 DA 34 01 FF FF FF FF FF FF FF FF 01 00 00 00 01 00 00 00 01 00 00 00 01 00 00 00 01 00 00 00 01 00 00 00 01 00 00 00 00 00 00 3C 00 00 00 1D AF 4F 00 01 00 00 00 08 A0 97 06 01 00 00 00 20 03 00 00 00 00 00 00 00 07 00 00 00 E7 DA 34 01 E8 DA 34 01 FF FF FF FF FF FF FF FF 01 00 00 00 01 00 00 00 01 00 00 00 01 00 00 00 01 00 00 00 01 00 00 00 01 00 00 00 00 00 00 3C 00 00 00 1E AF 4F 00 01 00 00 00 09 A0 97 06 01 00 00 00 20 03 00 00 00 00 00 00 00 07 00 00 00 E8 DA 34 01 E9 DA 34 01 FF FF FF FF FF FF FF FF 01 00 00 00 01 00 00 00 01 00 00 00 01 00 00 00 01 00 00 00 01 00 00 00 01 00 00 00 00 00 00 3C 00 00 00 1F AF 4F 00 01 00 00 00 0A A0 97 06 01 00 00 00 20 03 00 00 00 00 00 00 00 07 00 00 00 E9 DA 34 01 EA DA 34 01 FF FF FF FF FF FF FF FF 01 00 00 00 01 00 00 00 01 00 00 00 01 00 00 00 01 00 00 00 01 00 00 00 01 00 00 00 00 00 00 3C 00 00 00 20 AF 4F 00 01 00 00 00 0B A0 97 06 01 00 00 00 20 03 00 00 00 00 00 00 00 07 00 00 00 EA DA 34 01 31 DB 34 01 FF FF FF FF FF FF FF FF 01 00 00 00 01 00 00 00 01 00 00 00 01 00 00 00 01 00 00 00 01 00 00 00 01 00 00 00 00 00 00 3C 00 00 00 21 AF 4F 00 01 00 00 00 0C A0 97 06 01 00 00 00 20 03 00 00 00 00 00 00 00 07 00 00 00 31 DB 34 01 32 DB 34 01 FF FF FF FF FF FF FF FF 01 00 00 00 01 00 00 00 01 00 00 00 01 00 00 00 01 00 00 00 01 00 00 00 01 00 00 00 00 00 00 3C 00 00 00");

        return outPacket;
    }
}
