package net.swordie.ms.connection.packet;

import net.swordie.ms.client.character.tms282.Tms282BuffDirectory;
import net.swordie.ms.connection.protocol.ProfileCodecs;
import net.swordie.ms.connection.protocol.ProtocolVersion;
import net.swordie.ms.connection.protocol.codec.NpcPacketEncoder;
import net.swordie.ms.connection.protocol.codec.ProfileEncoders;
import net.swordie.ms.connection.protocol.codec.StagePacketEncoder;
import net.swordie.ms.connection.protocol.codec.UserPoolPacketEncoder;

import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Objects;

/**
 * The TMS 282.3 field codec families that the 265 builders defer to (users in the field, NPCs, field-change
 * SetField): {@link Tms282UserPoolPackets}, {@link Tms282NpcPackets}, {@link Tms282StageEncoder}.
 * <p>
 * {@link #register} adds them to a profile's {@link ProfileCodecs} (for the profile factory);
 * {@link #installFallback} makes them the 282 fallback of {@link ProfileEncoders} until the profile registers them.
 */
public final class Tms282FieldEncoders {
    /**
     * LP03D0 #24: the buffs each character's socket published ({@link Tms282BuffDirectory}), remote CTS table from
     * data/protocol/tms282.3 (loaded on the first character with a buff).
     */
    static final Tms282RemoteStatView REMOTE_STATS = Tms282RemoteStatView.of(Tms282RemoteBuffs::activeStats,
            Path.of("data", "protocol", "tms282.3")); // multiplayer-0930: + the TSM's buffs (was the 282 book only)

    private Tms282FieldEncoders() {
    }

    /** Adds the three families to {@code builder}; strings in {@code charset} (the character-name charset). */
    public static ProfileCodecs.Builder register(ProfileCodecs.Builder builder, Charset charset) {
        Objects.requireNonNull(charset, "charset");
        return builder.add(UserPoolPacketEncoder.class, new Tms282UserPoolPackets(charset, REMOTE_STATS))
                .add(TemporaryStatEncoder.class, temporaryStats())
                .add(net.swordie.ms.connection.protocol.codec.FieldAvailability.class,
                        Tms282FieldAvailability.load()) // maps the 282 client no longer has: warps refused
                .add(net.swordie.ms.connection.protocol.codec.ItemAvailability.class,
                        Tms282ItemAvailability.load()) // items the 282 client no longer has: left out of shops
                .add(NpcPacketEncoder.class, new Tms282NpcPackets(charset))
                .add(StagePacketEncoder.class, Tms282StageEncoder.INSTANCE)
                // items phase: LP048D REMOTE_AVATAR_MODIFIED for 265 Char.equip/unequip and look changes
                .add(net.swordie.ms.connection.protocol.codec.AvatarModifiedEncoder.class,
                        new Tms282AvatarModifiedPackets(charset))
                // NPC / script phase: LP0960 SCRIPT_MESSAGE for every 265 ScriptManagerImpl dialog
                .add(net.swordie.ms.connection.protocol.codec.ScriptMessageEncoder.class,
                        new Tms282ScriptPackets(charset))
                // skills phase: LP0664 SUMMONED_CREATED for every 265 summon spawn
                .add(net.swordie.ms.connection.protocol.codec.SummonedPacketEncoder.class,
                        new Tms282SummonedPackets(charset))
                // cash shop phase: LP02F1 SET_CASH_SHOP, LP0A75 query cash, LP0A76 cash item results
                .add(net.swordie.ms.connection.protocol.codec.CashShopPacketEncoder.class,
                        new Tms282CashShopPackets(charset))
                // skills phase 2: REMOTE_*_ATTACK hits as one i64 with the critical flag in bit 63
                .add(net.swordie.ms.connection.protocol.codec.RemoteAttackHitCodec.class,
                        net.swordie.ms.connection.protocol.codec.RemoteAttackHitCodec.TMS_282)
                // daily-play phase: LP0962 / LP0963 NPC shop, LP097B storage
                .add(net.swordie.ms.connection.protocol.codec.ShopPacketEncoder.class, new Tms282ShopPackets(charset))
                .add(net.swordie.ms.connection.protocol.codec.TrunkPacketEncoder.class, new Tms282TrunkPackets(charset))
                // control panel: 265 INVENTORY_OPERATION of scripts/timers (outside a route) as LP008F
                .add(net.swordie.ms.connection.protocol.codec.InventoryOperationEncoder.class,
                        new Tms282InventoryOperationEncoder(charset))
                .add(net.swordie.ms.connection.protocol.codec.MiniRoomPacketEncoder.class, new Tms282MiniRoomPackets(charset)) // mini-room: LP097F trade
                .add(net.swordie.ms.connection.protocol.codec.FriendPacketEncoder.class, new Tms282FriendPackets(charset)) // social-3: LP00DB
                // auction phase: LP02F0 SET_AUCTION_FIELD, LP0A95..0A98 auction house (282 item slots)
                .add(net.swordie.ms.connection.protocol.codec.AuctionPacketEncoder.class, new Tms282AuctionPackets(charset))
                // damage phase: 265 attack gates (mpCon/cooltime/GM one-hit) logged instead of dropping 282 damage
                .add(net.swordie.ms.handlers.user.AttackGatePolicy.class,
                        net.swordie.ms.handlers.login.tms282.Tms282AttackGates.load())
                // dropped-265 phase: CHAT_MSG, EXCL_REQUEST, SET_IN_GAME_DIRECTION_MODE, MESSAGE records
                .add(net.swordie.ms.connection.protocol.codec.LegacyPacketTranslator.class,
                        Tms282LegacyPackets.builder(charset)
                                .with(Tms282SystemMessageTranslators.INSTANCE)
                                .with(Tms282UnlockScope.INSTANCE) // EXCL_REQUEST -> LP009D unlock, request-scoped
                                .with(Tms282MountTranslators.INSTANCE) // social 3: LP00BD taming mob info
                                .with(Tms282InventoryArrangeTranslators.INSTANCE) // LP00C5/LP00C6 arrange results, LP048B tomb
                                .with(Tms282EquipmentEnchantTranslators.INSTANCE) // equip phase: LP01A1 star force window
                                .with(Tms282CashShopTranslators.INSTANCE) // cash shop phase: LP001F migrate command
                                .with(Tms282SkillLegacyTranslators.INSTANCE) // skills phase: LP009D, LP00A2, LP066F
                                .with(Tms282DirectionTranslators.INSTANCE) // boss/cutscene phase: LP0502, LP0308, LP030F, LP0317, LP00D7, LP04CA, NPC LP0735/0736/073D/0745
                                .with(Tms282PartyTranslators.INSTANCE) // social phase: LP00CF PARTY_RESULT
                                .with(Tms282ChatGroupTranslators.INSTANCE) // social phase: LP0305 WHISPER, LP0302 GROUP_MESSAGE
                                .with(Tms282PetPackets.INSTANCE) // social 2: LP0441..0446 pets
                                .with(Tms282HexaMatrixTranslators.INSTANCE) // 6th job: LP0271..LP0275, LP0277 HEXA matrix
                                .with(Tms282DailyPlayTranslators.INSTANCE) // daily-play phase: LP051E death window
                                .with(Tms282CombatTranslators.INSTANCE) // skills phase 2: LP0478..047B remote attacks, LP047C/047E, LP0671, LP0752..0754
                                .with(Tms282InteractionTranslators.INSTANCE) // interaction phase: LP04BE emotion, LP0401 sit, LP048C chair (stand-up)
                                .with(Tms282LoginInitTranslators.INSTANCE) // progression phase: LP0331 quick move
                                .with(Tms282AndroidTranslators.INSTANCE) // android / monster card: LP044F..0453, LP0481, LP00F9, LP01D7
                                .with(Tms282InventoryPurchasePackets.INSTANCE) // maple points: LP014A SET_MAPLE_POINT
                                .with(Tms282MobSkillTranslators.INSTANCE) // mob skills: LP069A after-attack, LP06D3 attack block
                                .with(Tms282DailyGiftTranslators.INSTANCE) // 每日禮物 LP0AA6
                                .with(Tms282DirectionVideoTranslators.INSTANCE) // VIDEO_BY_SCRIPT -> LP0506 (stuck-scene audit)
                                .with(Tms282SecondAtomTranslators.INSTANCE) // second atoms LP0463..0467 (Adele swords / shards)
                                .with(Tms282CompanionTranslators.INSTANCE) // dragon LP044B/044D/044E, Haku LP0454..0459, skill pet LP045C/045D/0460
                                .with(Tms282UnionPackets.INSTANCE) // 戰地聯盟: LP0AAC result, LP021A assign, LP0ABB coin
                                .with(Tms282SymbolTranslators.INSTANCE) // 符文: LP0AAF/0AB0/0AB1 enhance results
                                .with(Tms282InnerAbilityPackets.INSTANCE) // 內在潛能: LP0137/0138/0139/013D (SET, circulator result: 282 builders)
                                .with(Tms282PopularityTranslators.INSTANCE) // social records: LP00AC fame result
                                .with(Tms282NickItemTranslators.INSTANCE) // titles: LP0485 remote nick item (traced CLEAN)
                                .with(Tms282ForceAtomTranslators.INSTANCE) // force atoms: LP032B (research/tms2823/force-atom)
                                .with(Tms282SkillStackTranslators.INSTANCE) // stack skills: LP0266/0267 (research/tms2823/force-atom §6)
                                .with(Tms282SkillEventTranslators.INSTANCE) // skill events: LP0662 from ms cooldowns, LP0524 final attack, LP06D0 mob bind, LP00DF town portal
                                .with(Tms282TeleportTranslators.INSTANCE) // 265 TELEPORT -> LP04CC (lucid-audit-0930: every teleportToPortal / Char.teleport caller)
                                .with(Tms282BossTranslators.INSTANCE) // boss mechanics (bossmech1-0930): shared boss packets + Lucid LP081C..0826; next boss patches add a part inside Tms282BossTranslators.PARTS
                                .with(Tms282DropFixTranslators.INSTANCE) // dropfix-0930: guard-dropped 265 packets the 282.4 client reads (research/tms2823/dropfix-0930; -Dtms282.dropfix.enabled=false)
                                .with(Tms282ZeroPackets.INSTANCE) // zero-followup-1005: proven resource/WP/subHP and remote assist-end readers
                                .with(Tms282RunePackets.INSTANCE) // rune use/ack/disappear: LP04FE/0547/07F8
                                // further Tms282LegacyPackets.Contributor lines go here (one per feature)
                                .build());
    }

    public static void installFallback(Charset charset) {
        ProfileEncoders.installFallback(ProtocolVersion.TMS_282_3, register(ProfileCodecs.builder(), charset).build());
    }

    private static TemporaryStatEncoder temporaryStats() {
        try {
            return new Tms282TemporaryStatEncoder(Tms282CtsTable.load(Path.of("data", "protocol", "tms282.3")));
        } catch (java.io.IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
    }
}
