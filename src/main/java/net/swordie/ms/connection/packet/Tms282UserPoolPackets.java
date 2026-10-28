package net.swordie.ms.connection.packet;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.CharacterStat;
import net.swordie.ms.client.character.avatar.AvatarLook;
import net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat;
import net.swordie.ms.client.character.skills.temp.TemporaryStatManager;
import net.swordie.ms.client.character.social.tms282.RingRecordService;
import net.swordie.ms.client.character.info.ZeroInfo;
import net.swordie.ms.client.social.Guild.Guild;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.protocol.codec.UserPoolPacketEncoder;
import net.swordie.ms.constants.JobConstants;
import net.swordie.ms.life.pet.Pet;
import net.swordie.ms.util.Position;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static net.swordie.ms.connection.packet.Tms282LoginPacketSupport.str;

/**
 * TMS 282.3 CUserPool packets (dispatcher CUserPool::OnPacket 5AA5720): LP03D0 USER_ENTER_FIELD and LP03D1
 * USER_LEAVE_FIELD. Layout: research/tms2823/packet-audit/ingame-layouts-282.md section 1 (items #1..#96);
 * every packet here is read back by the emulated client (research/tms2823/field-trace, Tms282FieldMovementCheck).
 * <p>
 * LP03D0 (frame 5AA84D0, then CUserRemote::Init 5AC2D10), differences from 265 {@code UserPool.userEnterField}:
 * <ul>
 * <li>new #17 (i32 i32 i32 u8), #18 u8, #30 (i32, FF-list, i32, FF-list), #36 2 x i32, #39 i32 map (always read),
 * #45 u8, #47 2 x i32, #57 u8 n;</li>
 * <li>#24 the remote CTS block (14FCC50): 152-byte mask plus the unmasked fields a..h; the character's active 282
 * buffs through {@link Tms282RemoteStatView} ({@link Tms282RemoteStatEncoder}), the empty block (184 zero bytes)
 * when none is sendable;</li>
 * <li>#28/#29 the 282 AvatarLook ({@link Tms282AvatarEncoder#encodeLook});</li>
 * <li>#58 pets carry one more i32 after the template id (the 265 giantRate int is read as u16 u8 u8: same bytes);</li>
 * <li>#85 larkness is an i32 (265: u8); #91..#94 are {@code i32 1, i32 0, i32 n = 0, u8 0, u8 0} (265 wrote a
 * str there, which would shift every later byte);</li>
 * <li>#96: CUserRemote::Init ends with the reflection read 6FDDF00(72A3D50() = WZ2UserCommonData, archive) at
 * 5AC540C, the same class as SetField's 44BA3AB: one uint8 "Flags" read with DecodeBuffer(1) at 6FDDFB4. It is
 * written as 0 (the SetField value); without it the client's DecodeBuffer throws at the end of the packet.</li>
 * </ul>
 * Rings #65..#67 come from {@link RingRecordService#remoteRings} (ring-records trace; u8 0 each when none).
 * Not ported yet (written as "absent", which every 282 reader accepts): portable chair (#49 id 0, #56 flag 0),
 * mini room (#63), customize effect (#74 = 0: the 282 id > 0 tail
 * is read through a virtual call not yet traced), the Lynn extra system stream (#88 = 0, its end marker).
 */
public final class Tms282UserPoolPackets implements UserPoolPacketEncoder {
    public static final int USER_ENTER_FIELD = 0x03D0;
    public static final int USER_LEAVE_FIELD = 0x03D1;
    /** Remote CTS block with an empty 152-byte mask: mask + u8 u8 u8 i32 + (i32 i32 i32 i32 n=0) + i32 0 + i32 0 + u8. */
    public static final int EMPTY_REMOTE_CTS_BYTES = 152 + 3 + 4 + 16 + 4 + 4 + 1;
    /** 265 AngelicBuster default dress-up clothe (UserPool.userEnterField). */
    public static final int DEFAULT_DRESS_UP_CLOTHE = 1051291;

    private final Charset charset;
    private final Tms282RemoteStatView remoteStats;

    public Tms282UserPoolPackets(Charset charset) {
        this(charset, Tms282RemoteStatView.EMPTY);
    }

    /** {@code remoteStats}: the buffs LP03D0 #24 shows for each character. */
    public Tms282UserPoolPackets(Charset charset, Tms282RemoteStatView remoteStats) {
        this.charset = Objects.requireNonNull(charset, "charset");
        this.remoteStats = Objects.requireNonNull(remoteStats, "remoteStats");
    }

    @Override
    public OutPacket userEnterField(Char chr) {
        return userEnterField(RemoteUser.of(chr), charset, remoteStats, RingRecordService.remoteRings(chr));
    }

    @Override
    public OutPacket userLeaveField(Char chr) {
        return userLeaveField(chr.getId());
    }

    /** LP03D0 for a user that is not a {@link Char} (the outfit-share model), in this profile's name charset. */
    public OutPacket userEnterField(RemoteUser u) {
        return userEnterField(u, charset);
    }

    /** LP03D1 (5AA8BF0): {@code i32 characterId}. */
    public static OutPacket userLeaveField(int characterId) {
        OutPacket out = Tms282LoginPacketSupport.packet(USER_LEAVE_FIELD);
        out.encodeInt((long) characterId);
        return out;
    }

    /** LP03D0 with the empty remote CTS block; strings in {@code charset} (the character-name charset). */
    public static OutPacket userEnterField(RemoteUser u, Charset charset) {
        return userEnterField(u, charset, Tms282RemoteStatView.EMPTY);
    }

    /** LP03D0; #24 from {@code remoteStats}; strings in {@code charset} (the character-name charset). */
    public static OutPacket userEnterField(RemoteUser u, Charset charset, Tms282RemoteStatView remoteStats) {
        return userEnterField(u, charset, remoteStats, RingRecordService.RemoteRings.NONE);
    }

    /**
     * LP03D0 with the worn couple / friendship / marriage rings (#65..#67, {@link RingRecordService.RemoteRings},
     * traced CLEAN: research/tms2823/ring-records); {@code RemoteRings.NONE} gives the ring-less bytes.
     */
    public static OutPacket userEnterField(RemoteUser u, Charset charset, Tms282RemoteStatView remoteStats,
                                           RingRecordService.RemoteRings rings) {
        Objects.requireNonNull(u, "user");
        Objects.requireNonNull(rings, "rings");
        Objects.requireNonNull(charset, "charset");
        Objects.requireNonNull(remoteStats, "remoteStats");
        OutPacket out = Tms282LoginPacketSupport.packet(USER_ENTER_FIELD);
        // 1.1 frame (5AA84D0)
        out.encodeInt((long) u.accountId());              // #1 5AA84FB
        out.encodeInt(0L);                                // #2 5AA852C: 0 -> #3 follows
        out.encodeInt((long) u.characterId());            // #3 5AA853E
        out.encodeByte(0);                                // #4 5AA8591
        out.encodeInt((long) u.guildId());                // #5 5AA859D
        // 1.2 CUserRemote::Init (5AC2D10)
        out.encodeInt((long) u.level());                  // #6
        str(out, u.name(), charset);                      // #7
        str(out, "", charset);                            // #8 parent name
        RemoteGuild g = u.guild() == null ? RemoteGuild.NONE : u.guild();
        out.encodeInt((long) g.id());                     // #9
        str(out, g.name(), charset);                      // #10
        out.encodeShort(g.markBg());                      // #11
        out.encodeByte(g.markBgColor());                  // #12
        out.encodeShort(g.mark());                        // #13
        out.encodeByte(g.markColor());                    // #14
        out.encodeInt((long) g.customEmblemId());         // #15
        out.encodeInt((long) g.customEmblemFlag());       // #16
        out.encodeInt(0L);                                // #17 9B2310: 3 x i32 + u8 (282 new)
        out.encodeInt(0L);
        out.encodeInt(0L);
        out.encodeByte(0);
        out.encodeByte(0);                                // #18 282 new
        out.encodeByte(u.gender());                       // #19
        out.encodeInt((long) u.pop());                    // #20
        out.encodeInt(0L);                                // #21 name tag mark
        out.encodeByte(0);                                // #22
        out.encodeInt(0L);                                // #23
        remoteStats.encode(out, u.characterId());         // #24 14FCC50 remote CTS block
        out.encodeShort(u.job());                         // #25
        out.encodeShort(u.subJob());                      // #26
        out.encodeInt((long) u.chuc());                   // #27
        out.encodeInt((long) u.arc());
        out.encodeInt((long) u.aut());
        Tms282AvatarEncoder.encodeLook(out, u.look(), u.name(), charset);           // #28 901AF0
        if (isZero(u.job())) {
            Tms282AvatarEncoder.encodeLook(out, Objects.requireNonNull(u.zeroLook(), "Zero second look"),
                    u.name(), charset);                   // #29
        }
        out.encodeInt(0L);                                // #30 8D75E0: i32, FF-terminated list, i32, list
        out.encodeByte(0xFF);
        out.encodeInt(0L);
        out.encodeByte(0xFF);
        out.encodeInt((long) u.driverId());               // #31
        out.encodeInt((long) u.passengerId());            // #32
        out.encodeByte(1);                                // #33
        out.encodeInt(0L);                                // #34 610F960: i32, i32, n = 0
        out.encodeInt(0L);
        out.encodeInt(0L);
        out.encodeInt((long) u.chocoCount());             // #35 six ints
        out.encodeInt((long) u.activeEffectItemId());
        out.encodeInt((long) u.monkeyEffectItemId());
        out.encodeInt((long) u.activeNickItemId());
        out.encodeInt(0L);
        out.encodeInt(0L);
        out.encodeInt(0L);                                // #36 282 new
        out.encodeInt(0L);
        out.encodeByte(0);                                // #37
        out.encodeByte(0);                                // #38
        out.encodeInt(0L);                                // #39 68AF70 custom nick map, n = 0 (always read)
        out.encodeInt((long) u.damageSkinId());           // #40
        str(out, "", charset);                            // #41
        str(out, "", charset);
        out.encodeInt((long) u.demonWingId());            // #42
        out.encodeInt((long) u.kaiserWingId());
        out.encodeInt((long) u.kaiserTailId());
        out.encodeByte(u.hoYoung());                      // #43
        out.encodeByte(0);                                // #44
        out.encodeByte(0);                                // #45 282 new
        out.encodeInt((long) u.completedSetItemId());     // #46
        out.encodeInt(0L);                                // #47 282 new
        out.encodeInt(0L);
        out.encodeShort(u.fieldSeatId());                 // #48
        out.encodeInt(u.chair() == null ? 0L : (long) u.chair().itemId()); // #49 chair item id (Tms282ChairView)
        out.encodeInt(0L);                                // #50
        out.encodePosition(u.position());                 // #51
        out.encodeByte(u.moveAction());                   // #52
        out.encodeShort(u.foothold());                    // #53
        out.encodeByte(0);                                // #54
        out.encodeByte(0);                                // #55
        out.encodeByte(u.chair() != null);                // #56 chair flag, then the chair class's body (5AC3852)
        if (u.chair() != null) {
            u.chair().encodeBody(out, charset);
        }
        out.encodeByte(0);                                // #57 5694DD0 n = 0 (282 new)
        for (RemotePet pet : u.pets()) {                  // #58 419D7E0 per pet
            out.encodeByte(1);
            out.encodeInt((long) pet.idx());
            encodePet(out, pet, charset);
        }
        out.encodeByte(0);
        out.encodeByte(0);                                // #59
        out.encodeByte(0);                                // #60 familiar loop end
        out.encodeInt((long) u.tamingMobLevel());         // #61
        out.encodeInt((long) u.tamingMobExp());
        out.encodeInt((long) u.tamingMobFatigue());
        out.encodeByte(0);                                // #62
        out.encodeByte(u.miniRoom() == null ? 0 : u.miniRoom().type()); // #63 mini room (5AC3CA3, = 265 layout)
        if (u.miniRoom() != null) {
            u.miniRoom().encode(out, charset);
        }
        out.encodeByte(u.adBoardMessage() != null);       // #64
        if (u.adBoardMessage() != null) {
            str(out, u.adBoardMessage(), charset);
        }
        rings.encode282(out, charset);                    // #65 couple, #66 friendship, #67 marriage (5AC4271..5AC458D)
        out.encodeByte(1);                                // #68 CSecondAtom: u8 1, i32 n = 0
        out.encodeInt(0L);
        out.encodeByte(0);                                // #69 mask
        out.encodeInt((long) u.evanDragonGlide());        // #70
        if (JobConstants.isKaiser((short) u.job())) {     // #71 (5AC4740)
            out.encodeInt((long) u.kaiserHueExtern());
            out.encodeInt((long) u.kaiserPremiumBlack());
            out.encodeByte(u.kaiserHueInner());
        }
        out.encodeInt((long) u.makingMeisterSkillEff());  // #72
        for (int i = 0; i < 5; i++) {                     // #73
            out.encodeByte(u.eventNameTags()[i]);
        }
        out.encodeInt(0L);                                // #74 customize effect (id > 0 tail not traced)
        out.encodeByte(u.soulEffect());                   // #75
        // #76 (i32 n, n x i32) is read only when the client's remote CTS holds RideVehicle with a mix vehicle
        // (1157730); the CTS block above never carries RideVehicle (two-state), so it is never read and never written.
        out.encodeInt(-1L);                               // #77
        out.encodeByte(0);                                // #78 flash fire
        out.encodeByte(1);                                // #79 outer 1, inner 0 (265 1, 0)
        out.encodeByte(0);
        out.encodeInt(0L);                                // #80 TextEquipInfo n = 0
        out.encodeByte(u.freezeHotAccountType());         // #81
        out.encodeInt((long) u.freezeHotAccountId());
        out.encodeInt((long) u.eventBestFriendAid());     // #82
        out.encodeByte(u.kinesisShield());                // #83
        out.encodeByte(u.beastFormWing());                // #84
        out.encodeInt(u.larknessDark() ? 1L : 0L);        // #85 i32 in 282
        out.encodeInt((long) u.dressUpClothe());          // #86
        out.encodeByte(0);                                // #87
        out.encodeInt(0L);                                // #88 extra system stream: end marker (Lynn not ported)
        out.encodeInt(0L);                                // #89 ImmovableObj n = 0
        out.encodeInt(0L);                                // #90 map n = 0
        out.encodeInt(1L);                                // #91
        out.encodeInt(0L);
        out.encodeInt(0L);                                // #92 6C9C3F0 n = 0
        out.encodeByte(0);                                // #93
        out.encodeByte(0);                                // #94
        out.encodeLong(0);                                // #95 63CC510
        out.encodeInt(-1L);
        out.encodeByte(0);                                // #96 WZ2UserCommonData.Flags (6FDDFB4)
        return out;
    }

    /** One remote pet (419D7E0), the 265 Pet.encode fields plus the 282 i32 after the template id. */
    static void encodePet(OutPacket out, RemotePet p, Charset charset) {
        out.encodeInt((long) p.templateId());
        out.encodeInt(0L);                                // 419D875 -> pet+0x21C (282 new)
        str(out, p.name(), charset);
        out.encodeLong(p.serialNumber());
        out.encodePosition(p.position());
        out.encodeByte(p.moveAction());
        out.encodeShort(p.foothold());
        out.encodeInt((long) p.hue());
        out.encodeInt((long) p.templateId());
        out.encodeShort(p.wonderGrade());
        out.encodeInt((long) p.giantRate());              // read as u16 + u8 + u8 (419DEAA/419DEC1/419DED8)
    }

    static boolean isZero(int job) {
        return job == 10000 || job / 100 == 101;
    }

    /** Guild fields of LP03D0 #9..#16 (265 Guild.encodeForRemote). */
    public record RemoteGuild(int id, String name, int markBg, int markBgColor, int mark, int markColor,
                              int customEmblemId, int customEmblemFlag) {
        public static final RemoteGuild NONE = new RemoteGuild(0, "", 0, 0, 0, 0, 0, 0);

        public RemoteGuild {
            Objects.requireNonNull(name, "name");
        }

        static RemoteGuild of(Guild g) {
            if (g == null) {
                return NONE;
            }
            boolean emblem = g.getCustomEmblem() != null;
            return new RemoteGuild(g.getId(), g.getName() == null ? "" : g.getName(), g.getMarkBg(), g.getMarkBgColor(),
                    g.getMark(), g.getMarkColor(), emblem ? g.getId() : 0, emblem ? 1 : 0);
        }
    }

    /** One pet of LP03D0 #58. */
    public record RemotePet(int idx, int templateId, String name, long serialNumber, Position position, int moveAction,
                            int foothold, int hue, int wonderGrade, int giantRate) {
        public RemotePet {
            Objects.requireNonNull(name, "name");
        }

        static RemotePet of(Pet p) {
            return new RemotePet(p.getIdx(), p.getTemplateId(), p.getName() == null ? "" : p.getName(),
                    p.getItem() == null ? 0 : p.getItem().getId(), p.getPosition(), p.getMoveAction(), p.getFh(),
                    p.getHue(), p.getWonderGrade(), p.getGiantRate());
        }
    }

    /**
     * What another user sees of a character (the values 265 UserPool.userEnterField reads from Char), so the packet
     * can be built and traced without a database.
     */
    /**
     * A remote user's open mini room (LP03D0 #63, packet-audit/chair-miniroom-282.md): u8 type; non-zero: i32 id,
     * str title, u8 private, u8 game kind, u8 users, u8 max users, u8 game on, then chat info (= 265 MiniRoom.encode +
     * Char.encodeChatInfo).
     */
    /** Off until the 282 mini room body is proven (packet-audit/chair-miniroom-282.md; emulation not CLEAN). */
    static final boolean MINI_ROOM_ENTER = Boolean.getBoolean("tms282.userEnter.miniRoom");

    public record MiniRoomView(int type, int id, String title, boolean isPrivate, int gameKind, int curUsers, int maxUsers,
                               boolean gameOn, String message, Tms282ChairView.ChatInfo chat) {
        void encode(OutPacket out, Charset charset) {
            out.encodeInt(id);
            str(out, title == null ? "" : title, charset);
            out.encodeByte(isPrivate);
            out.encodeByte(gameKind);
            out.encodeByte(curUsers);
            out.encodeByte(maxUsers);
            out.encodeByte(gameOn);
            chat.encode(out, message, charset);
        }

        static MiniRoomView of(Char chr) {
            if (!MINI_ROOM_ENTER) {
                return null; // layout not proven: the field-trace emulation of the 265 layout read past it (09-29)
            }
            var room = chr.getMiniRoom();
            if (room == null || room.getType() == 0) {
                return null;
            }
            return new MiniRoomView(room.getType(), room.getID(), room.getMiniRoomTitle(), room.isPrivate(),
                    room.getGameKind(), room.getCurUsers(), room.getMaxUsers(), room.isGameOn(), room.getMsg(),
                    Tms282ChairView.ChatInfo.of(chr));
        }
    }

    public record RemoteUser(int accountId, int characterId, int guildId, int level, String name, RemoteGuild guild,
                             int gender, int pop, int job, int subJob, int chuc, int arc, int aut,
                             AvatarLook look, AvatarLook zeroLook,
                             int driverId, int passengerId, int chocoCount, int activeEffectItemId,
                             int monkeyEffectItemId, int activeNickItemId, int damageSkinId,
                             int demonWingId, int kaiserWingId, int kaiserTailId, boolean hoYoung,
                             int completedSetItemId, int fieldSeatId, Position position, int moveAction, int foothold,
                             List<RemotePet> pets, int tamingMobLevel, int tamingMobExp, int tamingMobFatigue,
                             String adBoardMessage, int evanDragonGlide, int kaiserHueExtern, int kaiserPremiumBlack,
                             int kaiserHueInner, int makingMeisterSkillEff, byte[] eventNameTags, int soulEffect,
                             int freezeHotAccountType, int freezeHotAccountId,
                             int eventBestFriendAid, boolean kinesisShield, boolean beastFormWing,
                             boolean larknessDark, int dressUpClothe, Tms282ChairView chair, MiniRoomView miniRoom) {
        /** Without a chair / mini room (the earlier components). */
        public RemoteUser(int accountId, int characterId, int guildId, int level, String name, RemoteGuild guild,
                          int gender, int pop, int job, int subJob, int chuc, int arc, int aut,
                          AvatarLook look, AvatarLook zeroLook,
                          int driverId, int passengerId, int chocoCount, int activeEffectItemId,
                          int monkeyEffectItemId, int activeNickItemId, int damageSkinId,
                          int demonWingId, int kaiserWingId, int kaiserTailId, boolean hoYoung,
                          int completedSetItemId, int fieldSeatId, Position position, int moveAction, int foothold,
                          List<RemotePet> pets, int tamingMobLevel, int tamingMobExp, int tamingMobFatigue,
                          String adBoardMessage, int evanDragonGlide, int kaiserHueExtern, int kaiserPremiumBlack,
                          int kaiserHueInner, int makingMeisterSkillEff, byte[] eventNameTags, int soulEffect,
                          int freezeHotAccountType, int freezeHotAccountId,
                          int eventBestFriendAid, boolean kinesisShield, boolean beastFormWing,
                          boolean larknessDark, int dressUpClothe) {
            this(accountId, characterId, guildId, level, name, guild, gender, pop, job, subJob, chuc, arc, aut, look,
                    zeroLook, driverId, passengerId, chocoCount, activeEffectItemId, monkeyEffectItemId,
                    activeNickItemId, damageSkinId, demonWingId, kaiserWingId, kaiserTailId, hoYoung,
                    completedSetItemId, fieldSeatId, position, moveAction, foothold, pets, tamingMobLevel,
                    tamingMobExp, tamingMobFatigue, adBoardMessage, evanDragonGlide, kaiserHueExtern,
                    kaiserPremiumBlack, kaiserHueInner, makingMeisterSkillEff, eventNameTags, soulEffect,
                    freezeHotAccountType, freezeHotAccountId, eventBestFriendAid, kinesisShield, beastFormWing,
                    larknessDark, dressUpClothe, null, null);
        }

        public RemoteUser {
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(look, "look");
            pets = List.copyOf(pets);
            Objects.requireNonNull(eventNameTags, "eventNameTags");
            if (eventNameTags.length != 5) {
                throw new IllegalArgumentException("Five event name tag bytes expected, got " + eventNameTags.length);
            }
            eventNameTags = eventNameTags.clone();
        }

        @Override
        public byte[] eventNameTags() {
            return eventNameTags.clone();
        }

        /** A plain character: no guild, pets, mount, rings or effects (checks and the tracer). */
        public static RemoteUser basic(int accountId, int characterId, int level, String name, int gender, int job,
                                       AvatarLook look, AvatarLook zeroLook, Position position, int moveAction,
                                       int foothold) {
            return new RemoteUser(accountId, characterId, 0, level, name, null, gender, 0, job, 0, 0, 0, 0, look,
                    zeroLook, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, JobConstants.isHoYoung((short) job), 0, 0, position,
                    moveAction, foothold, List.of(), 0, 0, 0, null, 0, 0, 0, 0, 0,
                    new byte[]{-1, -1, -1, -1, -1}, 0, 0, 0, 0, false, false, false, DEFAULT_DRESS_UP_CLOTHE);
        }

        /** The same values 265 UserPool.userEnterField writes. */
        public static RemoteUser of(Char chr) {
            CharacterStat cs = chr.getAvatarData().getCharacterStat();
            TemporaryStatManager tsm = chr.getTemporaryStatManager();
            AvatarLook look = chr.getAvatarData().getAvatarLook();
            AvatarLook zero = chr.getAvatarData().getZeroAvatarLook();
            if (zero != null) {
                ZeroInfo zeroInfo = chr.getZeroInfo();
                if (zeroInfo != null && zeroInfo.isZeroBetaState()) {
                    look = zero;
                }
            }
            List<RemotePet> pets = new ArrayList<>();
            for (Pet p : chr.getPets()) {
                pets.add(RemotePet.of(p));
            }
            chr.initEventNameTag();
            byte[] tags = new byte[5];
            for (int i = 0; i < 5; i++) {
                boolean active = chr.getEventNameTag() != null && chr.getEventNameTag().getActiveNameTags().length > i;
                tags[i] = active ? (byte) chr.getEventNameTag().getActiveNameTags()[i] : -1;
            }
            var freezeHot = chr.getFreezeHotEventInfo();
            int job = chr.getJob();
            return new RemoteUser(chr.getUser().getId(), chr.getId(), chr.getGuildID(), chr.getLevel(), chr.getName(),
                    RemoteGuild.of(chr.getGuild()), cs.getGender(), cs.getPop(), job, cs.getSubJob(),
                    chr.getTotalChuc(false), chr.getTotalArc(), chr.getTotalAut(), look,
                    isZero(job) ? chr.getAvatarData().getZeroAvatarLook() : null,
                    chr.getDriverID(), chr.getPassengerID(), chr.getChocoCount(), chr.getActiveEffectItemID(),
                    chr.getMonkeyEffectItemID(), chr.getActiveNickItemID(),
                    chr.getActiveDamageSkin() == null ? 0 : chr.getActiveDamageSkin().getDamageSkinID(),
                    look.getDemonWingID(), look.getKaiserWingID(), look.getKaiserTailID(), JobConstants.isHoYoung((short) job),
                    chr.getCompletedSetItemID(), chr.getFieldSeatID(), chr.getPosition(), chr.getMoveAction(),
                    chr.getFoothold(), pets, chr.getTamingMobLevel(), chr.getTamingMobExp(), chr.getTamingMobFatigue(),
                    chr.getADBoardRemoteMsg(), chr.getEvanDragonGlide(), chr.getKaiserMorphRotateHueExtern(),
                    chr.getKaiserMorphPrimiumBlack(), chr.getKaiserMorphRotateHueInnner(), chr.getMakingMeisterSkillEff(),
                    tags, chr.getSoulEffect(),
                    freezeHot == null ? 0 : freezeHot.getAccountType(), freezeHot == null ? 0 : freezeHot.getAccountID(),
                    chr.getEventBestFriendAID(), tsm.hasStat(CharacterTemporaryStat.KinesisPsychicEnergeShield),
                    chr.isBeastFormWingOn(), tsm.getLarknessManager() != null && tsm.getLarknessManager().isDark(),
                    JobConstants.isAngelicBuster(job) && chr.getDressUpInfo() != null
                            ? chr.getDressUpInfo().getClothe() : DEFAULT_DRESS_UP_CLOTHE,
                    Tms282ChairView.of(chr), MiniRoomView.of(chr));
        }
    }
}
