package net.swordie.ms.handlers.login.tms282;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.InPacket;
import net.swordie.ms.connection.packet.Tms282CollectionExploreRequest;
import net.swordie.ms.connection.packet.Tms282StatPackets;
import net.swordie.ms.handlers.header.InHeader;
import net.swordie.ms.handlers.user.UserHandler;
import net.swordie.ms.util.DataPrinter;

import java.util.Arrays;

/**
 * Monster Collection requests of the TMS 282.3 overlay:
 * <ul>
 *     <li>CP0245 exploration [opcode 推定] ({@link Tms282CollectionExploreRequest}) -> 265
 *     {@link UserHandler#handleMonsterCollectionExploreReq}: collection record (MESSAGE 39) + LP01D7 result; the 265
 *     end-of-request EXCL_REQUEST becomes the scoped LP009D unlock.</li>
 * </ul>
 * The reward claim (265 MONSTER_COLLECTION_COMPLETE_REWARD_REQ, 5 x i32) has no 282 sender found yet, so it has no
 * route. Collection state at login: CharacterData [47] (Tms282CollectionRecordBlock); saved by MonsterCollectionStore.
 */
final class Tms282MonsterCollectionHandlers {

    void register(Tms282Routes routes) {
        routes.channel(Tms282CollectionExploreRequest.OPCODE, InHeader.MONSTER_COLLECTION_EXPLORE_REQ)
                .when(Tms282Guards.IN_FIELD)
                .handle("monster collection exploration -> 265 (MESSAGE 39 + LP01D7) + LP009D unlock", this::explore);
    }

    private void explore(Tms282LoginState session, int op, byte[] packet) {
        Tms282CollectionExploreRequest request;
        try {
            request = Tms282CollectionExploreRequest.parse(packet);
        } catch (IllegalArgumentException e) {
            System.out.printf("[TMS282] REJECTED CP%04X len=%d: %s (%s)%n", op, packet.length, e.getMessage(),
                    packet.length <= 48 ? Arrays.toString(packet) : "");
            session.send(Tms282StatPackets.unlock());
            return;
        }
        Char chr = session.fieldCharacter();
        System.out.println("[TMS282] " + request + " character=" + chr.getId() + " -> 265");
        try {
            UserHandler.handleMonsterCollectionExploreReq(chr, new InPacket(request.toGms265Body()));
        } catch (RuntimeException e) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, e);
            System.out.printf("[TMS282] FAILED CP%04X character=%d: %s%n", op, chr.getId(), e);
            session.send(Tms282StatPackets.unlock());
        }
    }
}
