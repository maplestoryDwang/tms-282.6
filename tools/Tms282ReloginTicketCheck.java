import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import net.swordie.ms.handlers.login.tms282.Tms282SwitchTickets;

/** Isolated native authentication-cookie and selection-token lifecycle. Injected clock; no DB/network/sleeps. */
class Tms282ReloginTicketCheck {
    private static int checks;
    private static final String PEER = "127.0.0.1";

    public static void main(String[] args) throws Exception {
        AtomicLong now = new AtomicLong(1_000);
        var tickets = new Tms282SwitchTickets(now::get);
        check(tickets.authCookieFor(9).isEmpty(), "missing cookie is empty");
        check(!tickets.hasAuthenticatedRelogin(9), "missing ticket is not authenticated");
        String selection = tickets.issue(9, 13, 36, 55, 1, true, PEER);
        String cookie = tickets.authCookieFor(9);
        check(selection.matches("[A-Za-z0-9_-]{16}"), "selection token remains sixteen base64url characters");
        check(cookie.matches("[A-Za-z0-9_-]{32}"), "independent auth credential is thirty-two base64url characters");
        check(!selection.equals(cookie), "two credentials remain distinct");
        check(cookie.equals(tickets.authCookieFor(9)), "cookie read does not regenerate it");
        var target = tickets.pendingTarget(9).orElseThrow();
        check(target.accountId() == 13 && target.characterId() == 36 && target.worldId() == 55
                && target.channelId() == 1 && target.fileReportVerified(), "existing routing and source proof retained");
        check(!tickets.hasAuthenticatedRelogin(9), "issuing is not authentication");
        check(tickets.consumeAuthentication(null, PEER).isEmpty(), "null cookie refused");
        check(tickets.consumeAuthentication("", PEER).isEmpty(), "empty cookie refused");
        check(tickets.consumeAuthentication(selection, PEER).isEmpty(), "selection credential cannot authenticate");
        String wrong = (cookie.charAt(0) == 'A' ? "B" : "A") + cookie.substring(1);
        check(tickets.consumeAuthentication(wrong, PEER).isEmpty(), "same-length wrong cookie refused");
        for (String peer : new String[]{null, "", " ", "127.0.0.2", PEER + " "})
            check(tickets.consumeAuthentication(cookie, peer).isEmpty(), "missing or non-exact peer refused");
        check(cookie.equals(tickets.authCookieFor(9)), "failed authentication does not consume credential");
        check(tickets.consumeAuthentication(cookie, PEER).equals(Optional.of(9)), "correct bound peer authenticates owner");
        check(tickets.hasAuthenticatedRelogin(9), "authentication state set");
        check(tickets.authCookieFor(9).isEmpty(), "used auth credential no longer exposed");
        check(tickets.consumeAuthentication(cookie, PEER).isEmpty(), "authentication cookie is one-shot");
        check(tickets.pendingTarget(9).orElseThrow().equals(target), "authentication retains selection target");
        check(!tickets.consume(9, 14, 36, selection), "foreign account cannot select");
        check(!tickets.consume(9, 13, 37, selection), "foreign character cannot select");
        check(!tickets.consume(9, 13, 36, cookie), "auth cookie cannot serve as selection credential");
        check(tickets.hasAuthenticatedRelogin(9), "failed selection preserves authenticated handoff");
        check(tickets.consume(9, 13, 36, selection), "selection consumes its independent credential");
        check(!tickets.hasAuthenticatedRelogin(9) && tickets.pendingTarget(9).isEmpty(), "successful selection removes complete ticket");
        check(!tickets.consume(9, 13, 36, selection), "selection replay refused");

        String oldSelection = tickets.issue(9, 13, 36, 55, 1, true, PEER);
        String oldCookie = tickets.authCookieFor(9);
        check(tickets.consumeAuthentication(oldCookie, PEER).isPresent(), "first issue authenticated");
        String freshSelection = tickets.issue(9, 13, 35, 55, 2, true, PEER);
        String freshCookie = tickets.authCookieFor(9);
        check(!oldCookie.equals(freshCookie) && !oldSelection.equals(freshSelection), "new issue replaces both random credentials");
        check(!tickets.hasAuthenticatedRelogin(9), "new issue resets prior authentication state");
        check(tickets.consumeAuthentication(oldCookie, PEER).isEmpty(), "old cookie cannot authenticate replacement ticket");
        check(!tickets.consume(9, 13, 36, oldSelection), "old selection cannot consume replacement ticket");
        check(tickets.consumeAuthentication(freshCookie, PEER).isPresent(), "replacement cookie remains usable");
        check(tickets.pendingTarget(9).orElseThrow().characterId() == 35, "replacement target retained");
        tickets.cancel(9);
        check(!tickets.hasAuthenticatedRelogin(9) && tickets.authCookieFor(9).isEmpty(), "cancel removes both stages");

        String unbound = tickets.issue(9, 13, 36);
        check(tickets.authCookieFor(9).length() == 32, "legacy issue also generates independent auth credential");
        check(tickets.consumeAuthentication(tickets.authCookieFor(9), PEER).isEmpty(), "unbound ticket cannot authenticate from any peer");
        check(!tickets.pendingTarget(9).orElseThrow().fileReportVerified(), "unbound ticket never carries trusted file proof");
        check(tickets.consume(9, 13, 36, unbound), "existing selection-only caller remains compatible");
        tickets.issue(9, 13, 36, 55, 0, true, " ");
        check(tickets.consumeAuthentication(tickets.authCookieFor(9), " ").isEmpty(), "blank bound peer is not accepted");
        tickets.cancel(9);

        now.set(10_000);
        String expiresSelection = tickets.issue(9, 13, 36, 55, 0, true, PEER);
        String expiresCookie = tickets.authCookieFor(9);
        now.set(129_999);
        check(tickets.authCookieFor(9).equals(expiresCookie), "cookie is available just before deadline");
        check(tickets.consumeAuthentication("x".repeat(32), PEER).isEmpty(), "failed attempt near expiry does not consume ticket");
        now.set(130_000);
        check(tickets.consumeAuthentication(expiresCookie, PEER).isEmpty(), "failed attempt and getter do not renew deadline");
        check(tickets.authCookieFor(9).isEmpty() && !tickets.consume(9, 13, 36, expiresSelection), "expiry removes both credentials");
        now.set(200_000);
        String authenticatedSelection = tickets.issue(9, 13, 36, 55, 0, true, PEER);
        String authenticatedCookie = tickets.authCookieFor(9);
        now.set(319_999);
        check(tickets.consumeAuthentication(authenticatedCookie, PEER).isPresent(), "valid cookie at final millisecond accepted");
        now.set(320_000);
        check(!tickets.hasAuthenticatedRelogin(9), "authentication does not renew overall handoff expiry");
        check(!tickets.consume(9, 13, 36, authenticatedSelection), "selection also expires after authentication");
        now.set(400_000);
        tickets.issue(9, 13, 36, 55, 0, true, PEER);
        String futureCookie = tickets.authCookieFor(9);
        now.set(399_999);
        check(tickets.consumeAuthentication(futureCookie, PEER).isEmpty(), "backwards clock refuses ticket");

        now.set(500_000);
        String secretSelection = tickets.issue(9, 13, 36, 55, 0, true, PEER);
        String secretCookie = tickets.authCookieFor(9);
        tickets.issue(10, 14, 40, 47, 0, false, "127.0.0.2");
        check(tickets.consumeAuthentication(secretCookie, "127.0.0.2").isEmpty(), "another ticket's peer cannot authenticate this owner");
        var pending = Tms282SwitchTickets.class.getDeclaredField("pending"); pending.setAccessible(true);
        String diagnostic = pending.get(tickets).toString();
        check(!diagnostic.contains(secretCookie) && !diagnostic.contains(secretSelection), "ticket diagnostics contain no credentials");
        check(tickets.consumeAuthentication(secretCookie, PEER).equals(Optional.of(9)), "multiple users resolve the cookie to its actual owner");
        check(!tickets.hasAuthenticatedRelogin(10), "authentication state does not bleed into another user");
        System.out.println("PASS " + checks + " native relogin ticket checks; independent credentials, bound peer, replay/expiry; no DB/network");
    }
    private static void check(boolean ok, String description) {
        if (!ok) throw new AssertionError(description);
        checks++;
    }
}
