package net.swordie.ms.handlers.login.tms282;

import net.swordie.ms.connection.packet.Tms282EventListPackets;
import net.swordie.ms.handlers.header.InHeader;
import net.swordie.ms.world.event.EventListData;
import net.swordie.ms.world.event.EventListRepository;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * The in-game event list (UIEventList) on the channel socket, in field (event-fixes-1005,
 * research/tms2823/eventlist-1005; static RE + traced fixtures, not live tested):
 * <ul>
 *     <li>CP {@value #REQUEST_EVENT_LIST} (empty body; sender 50CECD0, sent when the UI opens while the client's list
 *     singleton is not loaded) -> LP 0x0175 {@link Tms282EventListPackets#eventList} of {@link EventListRepository}
 *     (re-read when events.tw.json changes).</li>
 *     <li>CP {@value #EVENT_LIST_OPERATION} (12 bytes: sender 5FEC480 writes i32 edx, i32 r9d, i32 r8d; its caller is
 *     in the virtualised region, so which entry field goes into which int is unknown). The three ints are
 *     recorded only; no entry resolution or gameplay action is authorized until native semantics are verified.</li>
 * </ul>
 */
final class Tms282EventListHandlers {
    static final int REQUEST_EVENT_LIST = 0x026B;
    static final int EVENT_LIST_OPERATION = 0x04AF;

    /** CP 0x04AF body in wire order. */
    record Operation(int first, int second, int third) {
        static Operation parse(byte[] packet) {
            if (packet.length != 2 + 12) {
                throw new IllegalArgumentException("CP04AF body is " + (packet.length - 2) + " bytes, expected 12");
            }
            ByteBuffer b = ByteBuffer.wrap(packet, 2, 12).order(ByteOrder.LITTLE_ENDIAN);
            return new Operation(b.getInt(), b.getInt(), b.getInt());
        }
    }

    private final Charset charset;
    private final Supplier<EventListRepository> events;

    Tms282EventListHandlers(Charset charset) {
        this(charset, EventListRepository::standard);
    }

    Tms282EventListHandlers(Charset charset, Supplier<EventListRepository> events) {
        this.charset = Objects.requireNonNull(charset, "charset");
        this.events = Objects.requireNonNull(events, "events");
    }

    void register(Tms282Routes routes) {
        routes.channel(REQUEST_EVENT_LIST, InHeader.REQUEST_EVENT_LIST).when(Tms282Guards.IN_FIELD)
                .handle("event list request -> LP0175", this::request);
        routes.channel(EVENT_LIST_OPERATION, InHeader.USER_EVENT_LIST_OPERATION).when(Tms282Guards.IN_FIELD)
                .handle("event list operation recorded only; three-i32 semantics unverified", this::operation);
    }

    private void request(Tms282LoginState session, int op, byte[] packet) {
        List<EventListData> list = events.get().current();
        session.send(Tms282EventListPackets.eventList(list, charset));
        System.out.printf("[TMS282] CP026B event list -> SENT LP0175 %d event(s)%s%n", list.size(),
                packet.length > 2 ? " (unexpected body " + (packet.length - 2) + " bytes ignored)" : "");
    }

    private void operation(Tms282LoginState session, int op, byte[] packet) {
        Operation o;
        try {
            o = Operation.parse(packet);
        } catch (IllegalArgumentException e) {
            Tms282SocialHandlers.rejected(op, packet, e);
            return;
        }
        System.out.printf("[TMS282] CP04AF event list operation %d %d %d recorded only: native field semantics unverified%n",
                o.first(), o.second(), o.third());
    }
}
