package net.swordie.ms.handlers.login.tms282;

/**
 * Boss-mechanics client requests of TMS 282 (bossmech1-0930, research/tms2823/lucid-re-0930/APPLIED.md), registered
 * once in {@link Tms282LoginOverlay}.
 * <p>
 * Extension point: every boss patch (research/tms2823/boss-mech-0930 and later) adds ONE line to {@link #register}
 * (a package-private {@code Tms282<Boss>Handlers} with {@code void register(Tms282Routes)}) instead of editing
 * Tms282LoginOverlay again. A second route for the same opcode is rejected by {@link Tms282Routes} /
 * HandlerRegistryCheck.
 */
final class Tms282BossMechHandlers {

    void register(Tms282Routes routes) {
        new Tms282LucidHandlers().register(routes); // Lucid: CP0616 fairy dust, CP0617 statue, CP0618 barrage end
        new Tms282BossFieldHandlers().register(routes); // bossmech2-0930: Will CP0648 moon gauge, CP0649 eye hit, CP064A web hit; Demian CP0608 sword node end
        // new Tms282<Boss>Handlers().register(routes); // next boss patch
    }
}
