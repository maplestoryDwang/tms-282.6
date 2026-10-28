package net.swordie.ms.handlers.login.tms282;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.InPacket;
import net.swordie.ms.connection.packet.Tms282BossUiRequest;
import net.swordie.ms.connection.packet.Tms282CheckBossPartyRequest;
import net.swordie.ms.connection.packet.Tms282StatPackets;
import net.swordie.ms.handlers.header.InHeader;
import net.swordie.ms.handlers.script.ScriptHandler;
import net.swordie.ms.util.DataPrinter;

import java.util.Arrays;
import java.util.function.Consumer;

/**
 * Boss entry of TMS 282.3 on the channel socket, after LP02EF ({@code when(IN_FIELD)}): the boss window (LP00D7,
 * sent by 265 {@code sm.startBossUI} through {@link net.swordie.ms.connection.packet.Tms282DirectionTranslators}) and
 * scripted party checks are parsed with their 282 layout and handed to the unchanged 265 boss flow
 * (BossPartyType / BossConstants / BossRuntimeConfig: level, party, difficulty, waiting and fighting fields,
 * instance, timers). The 265 flow warps with {@code Char.warp}, whose SetField is the 282 LP02EF.
 * <ul>
 *     <li>CP02CB BOSS_UI_REQUEST ({@link Tms282BossUiRequest}) -> 265 {@link ScriptHandler#handleBossUIRequest};</li>
 *     <li>CP0351 CHECK_BOSS_PARTY_BY_SCRIPT ({@link Tms282CheckBossPartyRequest}) -> 265
 *     {@link ScriptHandler#handleCheckBossPartyByScript}.</li>
 * </ul>
 * When the 265 flow refuses (the character stays in its field), LP009D releases the client's request lock; the
 * 265 refusal text goes out as a chat line through the legacy translator.
 */
final class Tms282BossHandlers {

    void register(Tms282Routes routes) {
        routes.channel(Tms282BossUiRequest.OPCODE, InHeader.BOSS_UI_REQUEST).when(Tms282Guards.IN_FIELD)
                .handle("boss UI enter -> 265 boss waiting/fighting field", this::bossUi);
        routes.channel(Tms282CheckBossPartyRequest.OPCODE, InHeader.CHECK_BOSS_PARTY_BY_SCRIPT).when(Tms282Guards.IN_FIELD)
                .handle("scripted boss party check -> 265 boss waiting/fighting field", this::checkBossParty);
    }

    private void bossUi(Tms282LoginState session, int op, byte[] packet) {
        Tms282BossUiRequest request;
        try {
            request = Tms282BossUiRequest.parse(packet);
        } catch (IllegalArgumentException e) {
            rejected(op, packet, e);
            return;
        }
        Char chr = session.fieldCharacter();
        System.out.printf("[TMS282] CP02CB boss UI character=%d order=%d difficulty=%d practice=%d%n", chr.getId(),
                request.orderId(), request.difficulty(), request.practice());
        enter(chr, op, c -> ScriptHandler.handleBossUIRequest(c, new InPacket(request.toGms265Body())));
    }

    private void checkBossParty(Tms282LoginState session, int op, byte[] packet) {
        Tms282CheckBossPartyRequest request;
        try {
            request = Tms282CheckBossPartyRequest.parse(packet);
        } catch (IllegalArgumentException e) {
            rejected(op, packet, e);
            return;
        }
        Char chr = session.fieldCharacter();
        System.out.printf("[TMS282] CP0351 boss party check character=%d order=%d difficulty=%d%n", chr.getId(),
                request.orderId(), request.difficulty());
        enter(chr, op, c -> ScriptHandler.handleCheckBossPartyByScript(c, new InPacket(request.toGms265Body())));
    }

    /** Runs the 265 flow; unlocks the client when the character did not change field. */
    static void enter(Char chr, int op, Consumer<Char> flow) {
        Object before = chr.getField();
        // dropfix-0930 (research/tms2823/dropfix-0930/cts-inv): the entry scripts (zakum_accept...) add / take items,
        // whose 265 INVENTORY_OPERATION the 282 encoder declines inside a route -> send the bag diff as LP008F.
        net.swordie.ms.client.character.tms282.Tms282ChangeWatch watch = routeInventoryDiffEnabled()
                ? net.swordie.ms.client.character.tms282.Tms282ChangeWatch.of(chr) : null;
        try {
            flow.accept(chr);
        } catch (RuntimeException e) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, e);
            System.out.printf("[TMS282] FAILED CP%04X character=%d: %s%n", op, chr.getId(), e);
        }
        var ops = watch == null ? java.util.List.<net.swordie.ms.connection.packet.Tms282InventoryPackets.Operation>of()
                : watch.operations();
        boolean sameField = chr.getField() == before;
        if (!ops.isEmpty()) {
            chr.write(net.swordie.ms.connection.packet.Tms282InventoryPackets.inventoryOperation(sameField, ops,
                    Tms282LoginSettings.load().nameCharset()));
        } else if (sameField) {
            chr.write(Tms282StatPackets.unlock());
        }
    }

    /** {@code -Dtms282.routeInventoryDiff.enabled=false} (or tms282.dropfix.enabled=false): unlock only, as before. */
    static boolean routeInventoryDiffEnabled() {
        return !"false".equalsIgnoreCase(System.getProperty("tms282.routeInventoryDiff.enabled", "true"))
                && !"false".equalsIgnoreCase(System.getProperty("tms282.dropfix.enabled", "true"));
    }

    private static void rejected(int op, byte[] packet, IllegalArgumentException e) {
        System.out.printf("[TMS282] REJECTED CP%04X len=%d: %s (%s)%n", op, packet.length, e.getMessage(),
                packet.length <= 48 ? Arrays.toString(packet) : "");
    }
}
