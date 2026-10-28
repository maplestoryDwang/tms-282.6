package net.swordie.ms.handlers.login.tms282;

import net.swordie.ms.GameSettings;
import net.swordie.ms.ServerConstants;
import net.swordie.ms.client.User;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.packet.Tms282AuthPackets;
import net.swordie.ms.connection.packet.Tms282CharacterPackets;
import net.swordie.ms.connection.packet.Tms282FileReport;
import net.swordie.ms.connection.packet.Tms282LoginSystemPackets;
import net.swordie.ms.connection.packet.Tms282LoginUiPackets;
import net.swordie.ms.connection.packet.Tms282PasswordLoginRequest;
import net.swordie.ms.connection.protocol.ProtocolVersion;
import net.swordie.ms.handlers.header.InHeader;
import net.swordie.ms.util.HotProperties;
import org.mindrot.jbcrypt.BCrypt;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Connection, authentication and secondary-password packets of the TMS 282.3 login socket (probe
 * onClientHello, onPong, onFileReport, authenticate, onServerTimeRequest, onGameLogRequest,
 * onSecondaryPasswordStatus, onCreationSecondaryPasswordCheck, registerSecondaryPassword, CP0081, CP00DA and the
 * login-dialog declines).
 */
final class Tms282AuthHandlers {
    /**
     * EXPERIMENTAL SPW-dialog cancel fallback: seconds after LP0031 before LP0043 is sent when no login action followed
     * ({@code -Dtms282.spwCancelUnlockSec} or config/server.properties, hot reloaded). Default 0 = off, never LP0043:
     * hotfix 2026-10-05, LP0043 arriving while a slow player is still typing in the SPW dialog crashes the client (the
     * old fixed 20 s timer disconnected a remote friend). Any value above 0 is raised to at least
     * {@value #SPW_CANCEL_UNLOCK_MIN_SECONDS} s and capped at {@value #SPW_CANCEL_UNLOCK_MAX_SECONDS} s.
     */
    static final String SPW_CANCEL_UNLOCK_KEY = "tms282.spwCancelUnlockSec";
    static final int SPW_CANCEL_UNLOCK_MIN_SECONDS = 60;
    static final int SPW_CANCEL_UNLOCK_MAX_SECONDS = 3600;
    /** CP0071 region/version check: locale 6, 0x1A, 1, then the build at offset 7 (28203 / 28204 / 28205, tms282.clientBuild). */
    static final int CLIENT_BUILD = net.swordie.ms.connection.packet.Tms282ClientBuild.build();

    private final Tms282LoginContext context;
    private final Tms282WorldHandlers worlds;

    Tms282AuthHandlers(Tms282LoginContext context, Tms282WorldHandlers worlds) {
        this.context = Objects.requireNonNull(context, "context");
        this.worlds = Objects.requireNonNull(worlds, "worlds");
    }

    void register(Tms282Routes routes) {
        routes.login(0x71, InHeader.PERMISSION_REQUEST).handle("client hello -> official pre-login packets", this::onClientHello);
        routes.login(0xab, InHeader.PONG).handle("pong", (s, op, p) -> s.onPong());
        routes.login(0x78, InHeader.CP0078).handle("client file report -> LP005A", this::onFileReport);
        routes.login(0x74, InHeader.CHECK_LOGIN_AUTH_INFO).handle("authenticate -> LP0000 + worlds", this::authenticate);
        routes.login(0x73, InHeader.CP0073).handle("single-use relogin -> LP0015 + worlds", this::authenticateRelogin);
        routes.login(0xeb, InHeader.CP00EB).handle("server time -> LP0064", (s, op, p) -> s.send(Tms282LoginSystemPackets.serverTime()));
        routes.login(0xbc, InHeader.CP00BC).handle("pre-login CP00BC -> LP0045 [00]", (s, op, p) -> s.send(Tms282LoginSystemPackets.preLoginAck0045()));
        routes.login(0xe7, InHeader.CP00E7).handle("game log init -> LP0B8D -1", this::onGameLogRequest);
        routes.login(0x7c, InHeader.CHECK_SPW_EXIST_REQUEST).endsSpwCancelCheck()
                .require(Tms282Guards.LIST_SENT, "Secondary-password status requested before character selection")
                .handle("SPW status -> LP0031", this::onSecondaryPasswordStatus);
        routes.login(0xc3, InHeader.CHECK_SPW_FOR_CREATE).endsSpwCancelCheck().when(Tms282Guards.AUTHENTICATED)
                .handle("creation SPW check -> LP0B1D", this::onCreationSecondaryPasswordCheck);
        routes.login(0xc4, InHeader.REGISTER_SPW).endsSpwCancelCheck().when(Tms282Guards.AUTHENTICATED)
                .handle("register SPW -> LP0B1E", this::registerSecondaryPassword);
        // CP00DA: NexonOtpPopUp answer; the 3CAD2A0/3CADF70 paths stay busy, so unlock with LP0043.
        routes.login(0xda, InHeader.CP00DA).reportFirst().handle("OTP popup answer -> LP0043", (s, op, p) -> {
            s.send(Tms282LoginUiPackets.unlockRequest());
            System.out.println("[TMS282] CP00DA recorded; SENT LP0043 unlock");
        });
        // CP0081: logout button; sets app+3B00, which LP000E clears while returning to the title.
        routes.login(0x81, InHeader.CP0081).reportFirst().handle("logout -> LP000E", (s, op, p) -> {
            s.resetLogin();
            s.send(Tms282LoginUiPackets.returnToTitle());
            System.out.println("[TMS282] CP0081 logout recorded: session cleared, SENT LP000E return to title");
        });
        // CP00B4 (str account, str new SPW, u8) answers the LP003F setup dialog, which this server never opens.
        // Carries a plaintext SPW, so it is never a report. LP0040 0 shows the failure notice.
        routes.login(0xb4, InHeader.CP00B4).when(Tms282Guards.AUTHENTICATED)
                .handle("SPW setup dialog -> LP0040 0", decline(() -> Tms282AuthPackets.accountSetupResult(0), "SPW setup dialog; LP0040 0 = failure"));
        // Not guarded: both dialogs can be open before CP0074 succeeds; dropping them would leave CLogin+220 busy.
        routes.login(0xd4, InHeader.LP004C_CONFIRM_ANSWER).handle("LP004C answer -> LP004D 0",
                decline(() -> Tms282LoginUiPackets.loginQuestionResult(0), "LP004C answer; LP004D 0 only unlocks"));
        routes.login(0xd7, InHeader.CP00D7).handle("wait dialog button -> LP0B4D 2",
                decline(() -> Tms282LoginUiPackets.waitDialogClose(2), "wait-dialog button; LP0B4D type 2 closes it"));
    }

    /**
     * Busy CPs whose native reply is known statically but whose feature this server does not provide: answer with
     * the native decline so the client unlocks through its own handler (probe declineWith).
     */
    static Tms282Command decline(Supplier<OutPacket> reply, String what) {
        return (s, op, p) -> {
            OutPacket out = reply.get();
            s.send(out);
            System.out.printf("[TMS282] DECLINED %04X (%s): SENT native LP%04X%n", op, what, out.getHeader());
        };
    }

    /** CP0071: region/version check, then the verified official pre-login packets (send-*.bin, in order). */
    void onClientHello(Tms282LoginState s, int op, byte[] plain) throws IOException {
        if (plain.length < 11 || plain[2] != 6 || plain[3] != 0x1a || plain[4] != 1
                || ByteBuffer.wrap(plain, 7, 4).order(ByteOrder.LITTLE_ENDIAN).getInt() != CLIENT_BUILD) {
            throw new IOException("Unexpected client region/version");
        }
        List<OutPacket> packets = preauthPackets(context.settings().preauthDir());
        for (OutPacket packet : packets) {
            s.send(packet);
        }
        System.out.println("[TMS282] SENT verified 282.3 pre-login initialization (" + packets.size() + " packets) from "
                + context.settings().preauthDir().getFileName());
    }

    /** The send-*.bin files of {@code dir} in name order, as 282 codec packets (opcode + body, unchanged). */
    static List<OutPacket> preauthPackets(Path dir) throws IOException {
        List<OutPacket> out = new ArrayList<>();
        try (var files = Files.list(dir)) {
            for (Path file : files.filter(p -> p.getFileName().toString().startsWith("send-")).sorted().toList()) {
                byte[] data = Files.readAllBytes(file);
                if (data.length < 2) {
                    throw new IOException("Pre-login packet without opcode: " + file);
                }
                OutPacket packet = OutPacket.forProfile(ProtocolVersion.TMS_282_3, (data[0] & 0xFF) | (data[1] & 0xFF) << 8);
                packet.encodeArr(java.util.Arrays.copyOfRange(data, 2, data.length));
                out.add(packet);
            }
        }
        if (out.isEmpty()) {
            throw new IOException("No pre-login packets in " + dir);
        }
        return out;
    }

    /** CP0078: a malformed, incomplete or modified manifest gets no success reply (the parser or check throws). */
    void onFileReport(Tms282LoginState s, int op, byte[] plain) throws Exception {
        Tms282FileReport report = Tms282FileReport.parse(plain);
        report.validateAgainst(context.fileReference());
        s.markFileReportVerified();
        s.send(Tms282LoginSystemPackets.fileCheckComplete());
        System.out.println("[TMS282] VERIFIED client file report entries=" + report.fileCrcs().size() + "; SENT official LP005A completion");
    }

    /**
     * CP0074 (Tms282PasswordLoginRequest): 16-byte machine block, flags, then account and password at offset 25;
     * BCrypt; LP0000 then the world list. Offset 2 is the client's MAC first octet, not a layout tag.
     */
    void authenticate(Tms282LoginState s, int op, byte[] plain) throws Exception {
        Tms282PasswordLoginRequest request;
        try {
            request = Tms282PasswordLoginRequest.parse(plain);
        } catch (IllegalArgumentException e) {
            System.out.println("[TMS282] " + Tms282PasswordLoginRequest.rejectionSummary(plain) + ": " + e.getMessage());
            throw new IOException("Invalid authentication fields: " + e.getMessage());
        }
        String name = request.name();
        String password = request.password();
        if (net.swordie.ms.world.tms282.audit.BanService.isIpBanned(s.client().getIP())) { // audit: block_ip
            s.send(Tms282AuthPackets.passwordFailure(4, ""));
            return;
        }
        // step 2: LoginBackoff per (account, IP) + a per-socket limit; a locked pair is refused before any password check
        String ip = s.client().getIP();
        Tms282LoginThrottle throttle = Tms282LoginThrottle.standard();
        long lockedMs = throttle.lockedForMs(name, ip);
        if (lockedMs > 0) {
            // review 2026-10-02: say it is a temporary lock (the bare code 4 reads as "wrong password", even for the
            // correct one, and a friend kept guessing, which only doubles the lock)
            s.send(Tms282AuthPackets.passwordFailure(4, Tms282LoginThrottle.lockoutMessage(lockedMs)));
            s.terminate("CP0074 for " + name + " refused: locked for " + (lockedMs + 999) / 1000 + " s after repeated failures");
            return;
        }
        User candidate = User.getUserFromSQLForAPI(name);
        boolean valid = candidate != null && BCrypt.checkpw(password, candidate.getPassword());
        if (!valid) {
            Tms282LoginThrottle.Outcome next = throttle.failure(s.client().getCh(), name, ip);
            long nowLocked = throttle.lockedForMs(name, ip);
            s.send(Tms282AuthPackets.passwordFailure(4, nowLocked > 0 ? Tms282LoginThrottle.lockoutMessage(nowLocked) : ""));
            if (next == Tms282LoginThrottle.Outcome.DISCONNECT) {
                s.terminate("CP0074 for " + name + ": too many failed passwords (" + Tms282LoginThrottle.failuresOn(s.client().getCh())
                        + " on this socket, locked " + (throttle.lockedForMs(name, ip) + 999) / 1000 + " s)");
            }
            return;
        }
        throttle.success(name, ip);
        if ((candidate.getBanExpireDate() != null && !candidate.getBanExpireDate().isExpired())
                || net.swordie.ms.world.tms282.audit.BanService.activeBan(candidate.getId()).isPresent()) { // audit: account_block
            s.send(Tms282AuthPackets.passwordFailure(4, ""));
            return;
        }
        User user = User.getUserFromSQLByID(candidate.getId());
        if (user == null) {
            throw new IOException("User " + candidate.getId() + " could not be loaded");
        }
        if (GameSettings.refusedNewLogin(user)) {
            // login.maintenance or a maintenance countdown: only accounts at login.maintenance.minAccountType (GM) or above may log in.
            s.send(Tms282AuthPackets.passwordFailure(GameSettings.maintenanceCode(), GameSettings.maintenanceMessage()));
            System.out.println("[TMS282] REFUSED userId=" + user.getId() + ": login.maintenance / maintenance countdown (GM-only login)");
            return;
        }
        s.authenticate(user);
        OutPacket response = Tms282AuthPackets.passwordSuccess(user);
        s.send(response);
        System.out.println("[TMS282] AUTHENTICATED userId=" + user.getId() + " responseBytes=" + response.getLength());
        sendLoginBackground(s);
        worlds.sendWorlds(s);
    }

    void authenticateRelogin(Tms282LoginState s, int op, byte[] plain) throws Exception {
        if (s.authenticated()) throw new IOException("Repeated relogin authentication");
        var request = net.swordie.ms.connection.packet.Tms282ReloginAuthRequest.parse(plain);
        int userId = context.switchTickets().consumeAuthentication(request.cookie(), s.client().getIP())
                .orElseThrow(() -> new IOException("Invalid or expired relogin credential"));
        User user = User.getUserFromSQLByID(userId);
        if (user == null || (user.getBanExpireDate() != null && !user.getBanExpireDate().isExpired())
                || net.swordie.ms.world.tms282.audit.BanService.isLoginBlocked(user.getId(), s.client().getIP())) // audit: block_ip / account_block
            throw new IOException("Relogin account unavailable");
        if (GameSettings.refusedByMaintenance(user))
            throw new IOException("Relogin refused: login.maintenance (GM-only login), userId=" + userId);
        s.authenticate(user);
        s.markNativeRelogin();
        s.send(Tms282AuthPackets.accountInfoSuccess(user, context.settings().nameCharset()));
        System.out.println("[TMS282] AUTHENTICATED native single-use relogin userId=" + userId);
        sendLoginBackground(s);
        worlds.sendWorlds(s);
    }

    /** login.background: LP007D before the world list; nothing is sent while the key is empty (the old behaviour). */
    static void sendLoginBackground(Tms282LoginState s) {
        String background = GameSettings.loginBackground();
        if (background.isEmpty()) {
            return;
        }
        s.send(Tms282LoginUiPackets.loginBackgroundMarker(background));
        System.out.println("[TMS282] SENT LP007D login background \"" + background + "\" (login.background, unverified)");
    }

    static String readString(ByteBuffer input, int max) throws IOException {
        if (input.remaining() < 2) {
            throw new IOException("Missing string length");
        }
        int length = Short.toUnsignedInt(input.getShort());
        if (length < 1 || length > max || length > input.remaining()) {
            throw new IOException("Invalid string length");
        }
        byte[] data = new byte[length];
        input.get(data);
        return new String(data, StandardCharsets.UTF_8);
    }

    /**
     * CP00E7: the value feeds GC:InitGameLogManager, not a proven security nonce. With no game-log backend, the
     * reader's explicit no-init sentinel -1 (RVA 0xf3c57f compares against -1).
     */
    void onGameLogRequest(Tms282LoginState s, int op, byte[] plain) {
        s.send(Tms282LoginSystemPackets.gameLogUnavailable());
        System.out.println("[TMS282] SENT game-log initialization value=-1 (no backend)");
    }

    /** CP007C (guarded by LIST_SENT): LP0031 [state, 1]; then the experimental cancel fallback, off by default. */
    void onSecondaryPasswordStatus(Tms282LoginState s, int op, byte[] plain) throws IOException {
        if (plain.length != 2) {
            throw new IOException("Unexpected CP007C size");
        }
        System.out.println("[TMS282] CP007C fileReportVerified=" + s.fileReportVerified());
        User user = s.user();
        if (user.getPic() == null || user.getPic().isBlank()) {
            throw new IOException("Registration of a new secondary password is not implemented");
        }
        // spw-state-2823.md: byte0 -> CLogin+230, 1 = SPW registered, so enter-game shows the SPW dialog and
        // CP0079 carries it. byte1 -> ctx+90; 1 hides the unsupported BtResetSPW button.
        s.send(Tms282AuthPackets.secondaryPasswordStatus(Tms282CharacterPackets.secondaryPasswordState(user), 1));
        System.out.println("[TMS282] SENT secondary-password status [01 01]: SPW registered, dialog expected before CP0079");
        long delay = spwCancelUnlockSeconds(HotProperties.server());
        if (delay <= 0) {
            s.cancelSpwCancelCheck();
            return;
        }
        s.scheduleSpwCancelCheck(delay, () -> {
            s.send(Tms282LoginUiPackets.unlockRequest());
            System.out.println("[TMS282] SPW dialog likely cancelled after " + delay + " s: SENT LP0043 unlock (experimental)");
        });
    }

    /** {@value #SPW_CANCEL_UNLOCK_KEY}: 0 (default, absent, not a number or not above 0) = off; else 60..3600 s. */
    static long spwCancelUnlockSeconds(HotProperties settings) {
        int configured = settings.integer(SPW_CANCEL_UNLOCK_KEY, 0, Integer.MIN_VALUE, Integer.MAX_VALUE);
        if (configured <= 0) {
            return 0;
        }
        return Math.min(SPW_CANCEL_UNLOCK_MAX_SECONDS, Math.max(SPW_CANCEL_UNLOCK_MIN_SECONDS, configured));
    }

    /** CP00C3 str spw -> LP0B1D (reply code is a static guess). */
    void onCreationSecondaryPasswordCheck(Tms282LoginState s, int op, byte[] plain) throws IOException {
        String spw = packetString(plain, 64, context);
        boolean ok = s.secondaryPasswordMatches(spw);
        s.send(Tms282AuthPackets.secondaryPasswordCheckResult(ok));
        System.out.println("[TMS282] SENT creation SPW check (LP0B1D) ok=" + ok);
    }

    /** CP00C4 str newSpw -> LP0B1E. Only registers when the account has no PIC yet. */
    void registerSecondaryPassword(Tms282LoginState s, int op, byte[] plain) throws IOException {
        String spw = packetString(plain, 64, context);
        User user = s.user();
        boolean hasPic = user.getPic() != null && !user.getPic().isBlank();
        boolean valid = spw.length() >= 4 && spw.length() <= 16 && spw.chars().allMatch(Character::isLetterOrDigit);
        if (hasPic || !valid) {
            s.send(Tms282AuthPackets.secondaryPasswordRegisterResult(false));
            System.out.println("[TMS282] REJECTED SPW registration hasPic=" + hasPic + " valid=" + valid);
            return;
        }
        user.setPic(BCrypt.hashpw(spw, BCrypt.gensalt(ServerConstants.BCRYPT_ITERATIONS)));
        user.updateUserPICToSQL();
        s.send(Tms282AuthPackets.secondaryPasswordRegisterResult(true));
        System.out.println("[TMS282] REGISTERED secondary password for user " + user.getId());
    }

    /** Exactly one u16-length string after the opcode; empty is allowed. */
    static String packetString(byte[] plain, int max, Tms282LoginContext context) throws IOException {
        if (plain.length < 4) {
            throw new IOException("Truncated string packet");
        }
        int length = Short.toUnsignedInt(ByteBuffer.wrap(plain, 2, 2).order(ByteOrder.LITTLE_ENDIAN).getShort());
        if (length > max || plain.length != 4 + length) {
            throw new IOException("Unexpected string packet size");
        }
        return new String(plain, 4, length, context.settings().nameCharset());
    }
}
