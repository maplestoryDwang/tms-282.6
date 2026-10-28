package net.swordie.ms.handlers.login.tms282;

import net.swordie.ms.connection.netty.PacketHandler;
import net.swordie.ms.connection.protocol.InboundOpcode;
import net.swordie.ms.connection.protocol.OpcodeEvidence;
import net.swordie.ms.connection.protocol.OpcodeTable;
import net.swordie.ms.connection.protocol.SessionKind;
import net.swordie.ms.connection.protocol.WireFormat;
import net.swordie.ms.connection.protocol.codec.MovePathCodec;
import net.swordie.ms.handlers.dispatch.HandlerRegistry;
import net.swordie.ms.handlers.header.InHeader;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * The TMS 282.3 handler overlay for login and channel entry: the research probe's clientHandlers() and
 * channelHandlers() as {@link Tms282Route}s, grouped by function ({@link Tms282AuthHandlers},
 * {@link Tms282WorldHandlers}, {@link Tms282CharacterHandlers}, {@link Tms282ReportHandlers},
 * {@link Tms282ChannelHandlers}, {@link Tms282FieldHandlers}, {@link Tms282SkillHandlers}, {@link Tms282MobHandlers}).
 * <p>
 * {@link #registry} turns the routes into a {@link HandlerRegistry} overlay for one opcode table:
 * <ul>
 *     <li>one handler per opcode that routes by socket kind (the probe kept separate login and channel maps); a
 *     packet on the socket kind without a route is consumed and logged (the probe: "unhandled");</li>
 *     <li>every other PROFILE_CODEC opcode of the table with a header gets a record-only handler, so no 282 body
 *     ever reaches a 265 base handler and {@link HandlerRegistry#validate} passes.</li>
 * </ul>
 */
public final class Tms282LoginOverlay {
    public static final String NAME = "tms282.3-overlay";

    private final List<Tms282Route> routes;

    public Tms282LoginOverlay(Tms282LoginContext context) {
        Objects.requireNonNull(context, "context");
        Tms282ReportHandlers reports = Tms282ReportHandlers.standard(context.settings().nameCharset());
        Tms282Routes collector = new Tms282Routes(reports);
        Tms282WorldHandlers worlds = new Tms282WorldHandlers(context);
        new Tms282AuthHandlers(context, worlds).register(collector);
        worlds.register(collector);
        new Tms282CharacterHandlers(context).register(collector);
        reports.register(collector);
        new Tms282ChannelHandlers(context, reports).register(collector);
        new Tms282FieldHandlers(context).register(collector);
        new Tms282SkillHandlers(context).register(collector);
        new Tms282MobHandlers(Tms282Profiles.codecs().require(MovePathCodec.class)).register(collector);
        // Phase 2: users / NPCs / field change. The 265 builders defer to these encoders (ProfileEncoders fallback).
        net.swordie.ms.connection.packet.Tms282FieldEncoders.installFallback(context.settings().nameCharset());
        new Tms282MovementHandlers().register(collector);
        Tms282TransferSettings transferSettings = Tms282TransferSettings.load();
        // Cash shop: entered in place; its exit comes as the transfer-field opcode 0x00FB (intercepted when configured).
        Tms282CashShopHandlers cashShop = new Tms282CashShopHandlers(context, transferSettings);
        new Tms282TransferHandlers(context.settings().nameCharset(), transferSettings)
                .intercept(cashShop.exitInterception()).register(collector);
        cashShop.register(collector);
        new Tms282CombatHandlers(context).register(collector);
        new Tms282SummonHandlers(context).register(collector); // skills phase 2: CP02E7..02EC summons
        new Tms282SkillStateHandlers(context).register(collector); // skills phase 2: CP04B7 cancel, CP04B8/04B9 key-down
        new Tms282ItemHandlers(context).register(collector);
        new Tms282ConsumeItemHandlers(context).register(collector); // A1: CP014B/014F/0154/0155/0173/0174 (untested)
        new Tms282EquipHandlers(context).register(collector); // equipment enhancement (scrolls, star force, cubes...)
        new Tms282SkillRingHandlers(context.settings().nameCharset()).register(collector);
        new Tms282CubeHandlers(context).register(collector);
        new Tms282ScriptHandlers(context.settings().nameCharset()).register(collector);
        new Tms282DailyPlayHandlers(context).register(collector); // daily play: shop, storage, channel change, return scrolls
        new Tms282AuctionHandlers(context).register(collector); // auction house: CP0100 enter, CP013B requests, CP013C leave
        new Tms282BossHandlers().register(collector); // boss entry: CP02CB boss UI, CP0351 scripted party check
        new Tms282BossMechHandlers().register(collector); // boss mechanics (bossmech1-0930): Lucid CP0616/0617/0618; next bosses register inside it
        new Tms282FieldReportHandlers(context.settings().nameCharset()).register(collector);
        new Tms282PeriodicReportHandlers(reports, context.settings().nameCharset()).register(collector);
        new Tms282HexaMatrixHandlers().register(collector); // 6th job: CP0490 HEXA operation, CP0491 erda conversion
        new Tms282VMatrixHandlers().register(collector);
        new Tms282FamiliarHandlers().register(collector);
        new Tms282InventoryPurchaseHandlers().register(collector); // inventory "+": CP00AF balance, CP00B0 buy
        new Tms282InnerAbilityHandlers(context).register(collector); // 內在潛能: CP01CC honor reset, CP01CD/0185 circulator, CP01CE preset
        new Tms282UnionHandlers().register(collector); // 戰地聯盟: CP03E0 board / preset
        new Tms282HyperStatHandlers().register(collector); // 極限屬性: CP0239 hyper skill up, CP023A..023D reset / stat up / preset
        new Tms282LockReleaseHandlers().register(collector); // unknown locking requests -> LP009D unlock (none routed now)
        Tms282ActorRpcRouter.register(collector);
        new Tms282SocialHandlers(context.settings().nameCharset()).register(collector); // social: party, chat, friends, pets
        new Tms282PetHandlers(context.settings().nameCharset()).register(collector); // social 2: pet requests
        new Tms282MiniRoomHandlers(context.settings().nameCharset()).register(collector); // mini room: CP01F0 trade
        new Tms282FameHandlers().register(collector); // social records: CP0198 fame -> FameService (LP00AC)
        new Tms282InteractionHandlers().register(collector); // interaction: CP011A emotion, CP0108 portable chair
        new Tms282CharacterInfoBackgroundHandlers().register(collector); // CP0124: owned character info background
        new Tms282InventoryArrangeHandlers(context).register(collector); // gather/sort/lock, slot item, tomb, CP02A7 revive
        new Tms282GuildHandlers(context).register(collector); // native guild core and notice, feature-gated persistence
        new Tms282AndroidHandlers().register(collector); // android: CP02FE move, CP02FF action, CP011B emotion, CP025E shop
        new Tms282MonsterCollectionHandlers().register(collector); // monster collection: CP0245 exploration (opcode presumed)
        new Tms282TamingMobHandlers(context.settings().nameCharset()).register(collector); // social 3: CP014D mount food
        new Tms282CharacterSwitchHandlers(context).register(collector); // CP04A7 change character -> LP028D, CP077C
        new Tms282CharacterOrderHandlers().register(collector);
        new Tms282DreamEquipmentHandlers().register(collector); // 夢幻的裝備: CP02D3 -> LP02C8 (static only, off by default)
        new Tms282MushroomPassHandlers().register(collector); // 菇菇通行證 window: CP04FC claim (static only)
        new Tms282PunchKingHandlers().register(collector); // punch-king field (ft236): CP0685 ready (static only)
        new Tms282FrierenHandlers().register(collector); // Frieren collab: CP03BF skill right-click ON/OFF (static only)
        new Tms282ZeroHandlers().register(collector); // CP01E1 assist end; CP01DF awaits native counter/state proof
        // event-fixes-1005: in-game event list CP026B -> LP0175, 參加 CP04AF -> EventListActions (static RE, not live tested)
        new Tms282EventListHandlers(context.settings().nameCharset()).register(collector);
        new Tms282TitleHandlers().register(collector); // CP0120 title on/off (5EE28D0, Unicorn-traced) -> 265 nick item
        new Tms282RuneHandlers().register(collector); // CP0580/0581/0582: pending challenge and native rune replies
        new Tms282LinkSkillHandlers().register(collector); // CP0211 link skill window -> 265 handler, LP0251 replies
        new Tms282SummonShootHandlers().register(collector); // CP02EB summon shoot objects (Altar of Annihilation) -> 265 handler
        new Tms282SpecialExitHandlers().register(collector); // CP035D event map 離開 button -> 265 USER_SPECIAL_EXIT
        new Tms282SkillAuxHandlers().register(collector); // captured multi-job shoot/summon/reload requests
        new Tms282VisualSkillHandlers().register(collector); // movement effects, area expiry, normal moving pose
        new Tms282AtomStateHandlers().register(collector); // verified Illium projectile state -> LP032E
        new Tms282BlasterHandlers().register(collector); // native grab release and charge-list cancellation
        new Tms282SummonLifecycleHandlers().register(collector, InHeader.CP03CF); // projectile motion and summon phases
        new Tms282MarkCleanupHandlers().register(collector); // stale Wild Hunter target book entries
        new Tms282MultiSkillModeHandlers(context).register(collector); // Xenon multi-mode skill selection
        new Tms282ExSummonHandlers().register(collector); // EX attack start/end authority
        new Tms282ArkGaugeHandlers(context).register(collector); // server-clock Ark gauge pulses
        new Tms282SkillProcessLifecycleHandlers().register(collector,InHeader.CP03E8); // finish accepted native attack processes
        new Tms282OverdriveEndHandlers().register(collector); // expired owned negative Overdrive stat
        new Tms282LoadedDiceHandlers().register(collector); // paid Ark/Xenon dice selection
        new Tms282AdeleTimerHandlers(context).register(collector); // owned Aether Weaving passive timer
        new Tms282IlliumTimerHandlers(context).register(collector); // owned passive crystal timer
        this.routes = collector.build();
    }

    /** Every route, login socket first (registration order). */
    public List<Tms282Route> routes() {
        return routes;
    }

    /** Opcodes with a route on {@code socket}. */
    public Set<Integer> opcodes(SessionKind socket) {
        Set<Integer> out = new TreeSet<>();
        for (Tms282Route r : routes) {
            if (r.socket() == socket) {
                out.add(r.op());
            }
        }
        return out;
    }

    /** The route's own header for {@code op} (used when the opcode table has none), or null. */
    public InHeader declaredHeader(int op) {
        for (Tms282Route r : routes) {
            if (r.op() == op) {
                return r.header();
            }
        }
        return null;
    }

    /**
     * Adds an {@link OpcodeEvidence#UNVERIFIED} PROFILE_CODEC entry for every routed opcode the table lacks (the
     * probe answers or records opcodes whose status is still UNVERIFIED).
     */
    public void declareOpcodes(OpcodeTable.Builder table) {
        Set<Integer> done = new TreeSet<>();
        for (Tms282Route r : routes) {
            if (done.add(r.op()) && table.inboundAt(r.op()).isEmpty()) {
                table.inbound(new InboundOpcode(r.op(), r.header().name(), r.header(), WireFormat.PROFILE_CODEC,
                        OpcodeEvidence.UNVERIFIED));
            }
        }
    }

    /**
     * The overlay registry for {@code table}.
     *
     * @throws IllegalStateException when a routed opcode has no header in the table (build the table with
     *                               {@link #declareOpcodes} and {@link #declaredHeader} as fallback)
     */
    public HandlerRegistry registry(OpcodeTable table) {
        Map<Integer, PacketHandler[]> bySocket = new LinkedHashMap<>();
        for (Tms282Route r : routes) {
            bySocket.computeIfAbsent(r.op(), op -> new PacketHandler[SessionKind.values().length])[r.socket().ordinal()] = r.handler();
        }
        HandlerRegistry.Builder builder = HandlerRegistry.overlayBuilder(NAME);
        Set<InHeader> registered = EnumSet.noneOf(InHeader.class);
        for (Map.Entry<Integer, PacketHandler[]> e : bySocket.entrySet()) {
            int op = e.getKey();
            InHeader header = table.inHeader(op);
            if (header == null) {
                throw new IllegalStateException(String.format("CP%04X has a 282 handler but no header in %s", op, table));
            }
            PacketHandler[] handlers = e.getValue();
            builder.register(header, bySocket(op, handlers[SessionKind.LOGIN.ordinal()], handlers[SessionKind.CHANNEL.ordinal()]));
            registered.add(header);
        }
        for (InboundOpcode entry : table.inboundOpcodes()) {
            InHeader header = entry.header();
            if (entry.format() == WireFormat.PROFILE_CODEC && header != null && !bySocket.containsKey(entry.op())
                    && registered.add(header)) {
                builder.register(header, recordOnly(entry.op()));
            }
        }
        return builder.build();
    }

    /** Opcodes the registry answers with {@link #recordOnly} for {@code table}. */
    public List<Integer> recordOnlyOpcodes(OpcodeTable table) {
        List<Integer> out = new ArrayList<>();
        for (InboundOpcode entry : table.inboundOpcodes()) {
            if (entry.format() == WireFormat.PROFILE_CODEC && entry.header() != null && declaredHeader(entry.op()) == null) {
                out.add(entry.op());
            }
        }
        return out;
    }

    private static PacketHandler bySocket(int op, PacketHandler login, PacketHandler channel) {
        return (c, chr, in) -> {
            boolean onChannel = Tms282Guards.isChannel(c);
            PacketHandler h = onChannel ? channel : login;
            if (h != null) {
                h.handle(c, chr, in);
                return;
            }
            int length = in.getLength();
            Tms282Routes.consume(in);
            System.out.printf("[TMS282] %s %04X len=%d unhandled on this socket (recorded only)%n",
                    onChannel ? "CHANNEL" : "LOGIN", op, length);
        };
    }

    /** A 282 opcode with a known header but no ported handler yet: consumed and logged, never parsed. */
    static PacketHandler recordOnly(int op) {
        return (c, chr, in) -> {
            int length = in.getLength();
            Tms282Routes.consume(in);
            System.out.printf("[TMS282] %s %04X len=%d has no 282 handler yet (recorded only)%n",
                    Tms282Guards.isChannel(c) ? "CHANNEL" : "LOGIN", op, length);
        };
    }
}
