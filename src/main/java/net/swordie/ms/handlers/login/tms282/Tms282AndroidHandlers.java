package net.swordie.ms.handlers.login.tms282;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.InPacket;
import net.swordie.ms.connection.packet.Tms282AndroidPackets;
import net.swordie.ms.connection.packet.Tms282AndroidRequests;
import net.swordie.ms.connection.protocol.codec.MovePathCodec;
import net.swordie.ms.handlers.header.InHeader;
import net.swordie.ms.handlers.life.AndroidHandler;
import net.swordie.ms.life.Android;
import net.swordie.ms.util.DataPrinter;

import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * TMS 282.3 android requests on the channel socket after LP02EF. The android itself lives in 265 code (Char.initAndroid,
 * Field.spawnLife); its packets reach the client through {@code Tms282AndroidTranslators}.
 * <ul>
 *     <li>CP02FE move ({@link Tms282AndroidRequests.Move}): the 282 path is applied to the android and sent to the
 *     other users of the field as LP0450 (265 would re-encode it in the 265 path form);</li>
 *     <li>CP02FF action ({@link Tms282AndroidRequests.Action}) -> 265 {@code AndroidHandler.handleAndroidActionSet}
 *     (LP0451 to the field);</li>
 *     <li>CP011B emotion ({@link Tms282AndroidRequests.Emotion}) -> 265 {@code handleAndroidEmotion} (LP0481 to the
 *     others);</li>
 *     <li>CP025E android shop ({@link Tms282AndroidRequests.Shop}) -> 265 {@code handleAndroidShop}.</li>
 * </ul>
 * None of these senders sets the request lock. A request that does not parse is logged and dropped; a 265 failure is
 * logged; neither ends the session.
 */
final class Tms282AndroidHandlers {
    private final Supplier<MovePathCodec> paths;

    Tms282AndroidHandlers() {
        this(Tms282MovementHandlers::profilePaths);
    }

    Tms282AndroidHandlers(Supplier<MovePathCodec> paths) {
        this.paths = paths;
    }

    void register(Tms282Routes routes) {
        routes.channel(Tms282AndroidRequests.Move.OPCODE, InHeader.ANDROID_MOVE).when(Tms282Guards.IN_FIELD)
                .handle("android move -> LP0450 to the field", this::move);
        routes.channel(Tms282AndroidRequests.Action.OPCODE, InHeader.ANDROID_ACTION_SET).when(Tms282Guards.IN_FIELD)
                .handle("android action -> 265 AndroidHandler, LP0451", (s, op, p) -> delegate(s, op, p,
                        Tms282AndroidRequests.Action::parse, (chr, r) -> AndroidHandler.handleAndroidActionSet(chr,
                                new InPacket(r.toGms265Body()))));
        routes.channel(Tms282AndroidRequests.Emotion.OPCODE, InHeader.ANDROID_EMOTION).when(Tms282Guards.IN_FIELD)
                .handle("android emotion -> 265 AndroidHandler, LP0481", (s, op, p) -> delegate(s, op, p,
                        Tms282AndroidRequests.Emotion::parse, (chr, r) -> AndroidHandler.handleAndroidEmotion(chr,
                                new InPacket(r.toGms265Body()))));
        routes.channel(Tms282AndroidRequests.Shop.OPCODE, InHeader.USER_ANDROID_SHOP).when(Tms282Guards.IN_FIELD)
                .handle("android shop -> 265 AndroidHandler", (s, op, p) -> delegate(s, op, p,
                        Tms282AndroidRequests.Shop::parse, (chr, r) -> AndroidHandler.handleAndroidShop(chr,
                                new InPacket(r.toGms265Body()))));
    }

    private void move(Tms282LoginState session, int op, byte[] packet) {
        Char chr = session.fieldCharacter();
        Android android = chr.getAndroid();
        if (android == null || chr.getField() == null) {
            return;
        }
        MovePathCodec codec = paths.get();
        Tms282AndroidRequests.Move request;
        try {
            request = Tms282AndroidRequests.Move.parse(packet, codec);
        } catch (IllegalArgumentException e) {
            System.out.printf("[TMS282] CHANNEL %04X len=%d dropped: %s%n", op, packet.length, e.getMessage());
            return;
        }
        try {
            request.path().applyTo(android);
            if (net.swordie.ms.connection.packet.Tms282AndroidTranslators.ANDROID_VISIBLE) // no field androids: EQUIP-CRASH.md
                chr.getField().broadcast(Tms282AndroidPackets.move(chr.getId(), request.path(), codec), chr);
        } catch (RuntimeException e) {
            failed(chr, op, e);
        }
    }

    /** A parsed request handed to a 265 handler; a parse failure is logged, a 265 failure too. */
    private static <R> void delegate(Tms282LoginState session, int op, byte[] packet,
                                     Function<byte[], R> parser, BiConsumer<Char, R> handler) {
        R request;
        try {
            request = parser.apply(packet);
        } catch (IllegalArgumentException e) {
            System.out.printf("[TMS282] CHANNEL %04X len=%d dropped: %s%n", op, packet.length, e.getMessage());
            return;
        }
        Char chr = session.fieldCharacter();
        try {
            handler.accept(chr, request);
        } catch (RuntimeException e) {
            failed(chr, op, e);
        }
    }

    private static void failed(Char chr, int op, RuntimeException e) {
        DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, e);
        System.out.printf("[TMS282] FAILED CP%04X character=%d: %s%n", op, chr.getId(), e);
    }
}
