package net.swordie.ms.connection.packet;

import net.swordie.ms.client.User;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.console.config.ServerRuntimeConfig;
import net.swordie.ms.constants.JobConstants;

import java.nio.charset.Charset;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

import static net.swordie.ms.connection.packet.Tms282LoginPacketSupport.DEFAULT_CHARSET;
import static net.swordie.ms.connection.packet.Tms282LoginPacketSupport.checkByte;
import static net.swordie.ms.connection.packet.Tms282LoginPacketSupport.str;

/**
 * TMS 282.3 login authentication and account-security replies: LP0000 password result, LP0015
 * account info (CP0073 re-authentication), LP0016 account creation, LP002B web token, secondary password /
 * PIN (LP0030-0032, LP0B1D, LP0B1E), OTP (LP002C, LP002E, LP002F) and account setup (LP003F, LP0040).
 * Every secondary-password builder is named {@code secondaryPassword*}.
 * <p>
 * The LP0000/LP0015 job table is filled from a {@code Predicate<LoginJob>}; the overloads without one use
 * {@link ServerRuntimeConfig#isJobCreationEnabled}, so offline checks can pass fixed flags instead of the
 * current {@code config/server.properties}.
 * See {@link Tms282LoginPacketSupport} for the opcodes deliberately left out.
 */
public final class Tms282AuthPackets {
    private Tms282AuthPackets() {
    }

    /** CLogin::OnCheckPasswordResult, RVA 0x3CA2550. Exactly 36 job entries, flags from the runtime configuration. */
    public static OutPacket passwordSuccess(User user) {
        return passwordSuccess(user, ServerRuntimeConfig::isJobCreationEnabled);
    }

    /** As {@link #passwordSuccess(User)}, with each job's creation flag supplied by {@code jobEnabled}. */
    public static OutPacket passwordSuccess(User user, Predicate<JobConstants.LoginJob> jobEnabled) {
        Objects.requireNonNull(user, "user");
        Objects.requireNonNull(jobEnabled, "jobEnabled");
        OutPacket out = Tms282LoginPacketSupport.packet(0x0000);
        out.encodeByte(0);
        str(out, "", DEFAULT_CHARSET);
        str(out, user.getName(), DEFAULT_CHARSET);
        out.encodeLong(user.getId());
        out.encodeInt(user.getId());
        out.encodeByte(user.getGender());
        out.encodeInt(user.getAccountType().getVal());
        out.encodeInt(user.getAge());
        out.encodeInt(0);
        out.encodeInt(0);
        out.encodeInt(user.getVipGrade());
        out.encodeByte(user.getPurchaseExp());
        out.encodeByte(user.getnBlockReason());
        out.encodeLong(user.getChatUnblockDate());
        out.encodeInt(0);
        out.encodeByte(1); // No additional censored account-name string.
        str(out, user.getName(), DEFAULT_CHARSET);
        encodeJobTable(out, jobEnabled);
        out.encodeByte(0);
        out.encodeByte(0);
        return out;
    }

    /**
     * LP0015 (3CA3C80) ACCOUNT_INFO_RESULT, the native reply to CP0073 (token re-authentication; static
     * pairing, not live-tested). The account fields go through the same setter 5DF1600 as LP0000, but
     * LP0015 has no leading str, no ignored i32 and no u8 + conditional str, carries two account strings
     * after the FILETIME and does not switch the login stage. The final u8 0 clears the CLogin+0x220 lock.
     */
    public static OutPacket accountInfoSuccess(User user, Charset charset) {
        return accountInfoSuccess(user, charset, ServerRuntimeConfig::isJobCreationEnabled);
    }

    /** As {@link #accountInfoSuccess(User, Charset)}, with each job's creation flag supplied by {@code jobEnabled}. */
    public static OutPacket accountInfoSuccess(User user, Charset charset, Predicate<JobConstants.LoginJob> jobEnabled) {
        Objects.requireNonNull(user, "user");
        Objects.requireNonNull(jobEnabled, "jobEnabled");
        OutPacket out = Tms282LoginPacketSupport.packet(0x0015);
        out.encodeByte(0);                              // result 0 (0x0C also continues)
        str(out, "", charset);                          // message, shown only on failure
        out.encodeLong(user.getId());                   // app+0x2448
        out.encodeInt(user.getId());                    // app+0x23AC
        out.encodeByte(user.getGender());               // app+0x30
        out.encodeInt(user.getAccountType().getVal());  // app+0x28
        out.encodeInt(user.getAge());                   // buf4 -> app+0x34
        out.encodeInt(0);                               // buf4, zeroed by the client
        out.encodeInt(user.getVipGrade());              // app+0x23E8 (the LP0000 slot passwordSuccess fills with the VIP grade)
        out.encodeByte(user.getPurchaseExp());          // app+0x23EC
        out.encodeByte(user.getnBlockReason());         // app+0x241C
        out.encodeLong(user.getChatUnblockDate());      // FILETIME -> app+0x241E
        str(out, user.getName(), charset);              // app+0x23C0, the login account string
        str(out, "", charset);                          // app+0x23D8, empty clears it
        out.encodeInt(0);                               // != 1 -> app+0x3B01 = 1, as in LP0000
        str(out, user.getName(), charset);              // CLogin+0x398
        encodeJobTable(out, jobEnabled);
        out.encodeByte(0);                              // CLogin+0x4B4
        out.encodeByte(0);                              // keepBusy 0 clears CLogin+0x220
        return out;
    }

    /**
     * LP0015 result 0x84 (3CB1F70): while retries remain (config 0x2BE, default 10, or 0x2AC, default 5)
     * the handler returns right after the message and timer 3C92AF0 re-sends CP0073 after
     * max(config 0x2A8, 500) ms. Once retries run out the client clears the CLogin+0x220 lock.
     */
    public static OutPacket accountInfoRetry(Charset charset) {
        OutPacket out = Tms282LoginPacketSupport.packet(0x0015);
        out.encodeByte(0x84);
        str(out, "", charset);
        return out;
    }

    /** u8 hasJobs = 1, u8 jobOrder, then exactly 36 (u8, u16) creation flags (LP0000, LP0015). */
    private static void encodeJobTable(OutPacket out, Predicate<JobConstants.LoginJob> jobEnabled) {
        out.encodeByte(1);
        out.encodeByte(JobConstants.jobOrder);
        for (int i = 0; i < 36; i++) {
            // Native 3D4BEB0/3D4BEE0: regional race 1000 maps to slot32, not31.
            JobConstants.LoginJob job=JobConstants.getTms282LoginJob(i);
            int enabled = jobEnabled.test(job) ? 1 : 0;
            out.encodeByte(enabled);
            out.encodeShort(enabled);
        }
    }

    public static OutPacket passwordFailure(int code, String message) {
        if (code < 1 || code > 255) {
            throw new IllegalArgumentException("Password failure code must be between 1 and 255");
        }
        OutPacket out = Tms282LoginPacketSupport.packet(0x0000);
        out.encodeByte(code);
        str(out, Objects.requireNonNull(message, "message"), DEFAULT_CHARSET);
        return out;
    }

    /** CLogin handler3CAB880: state synchronization, not successful PIN authentication. */
    public static OutPacket secondaryPasswordStatus(int state, int context) {
        checkByte(state, "Secondary-password state");
        checkByte(context, "Secondary-password context");
        OutPacket out=Tms282LoginPacketSupport.packet(0x0031);
        out.encodeByte(state);out.encodeByte(context);
        return out;
    }

    /**
     * LP0B1D (3CABCF0). result 0 continues login with no job table (u8 hasJobs = 0); 0x14 shows
     * notice 0x1E. Used as the CP00C3 (creation SPW check) reply, which is itself a static guess.
     */
    public static OutPacket secondaryPasswordCheckResult(boolean ok) {
        OutPacket out = Tms282LoginPacketSupport.packet(0x0B1D);
        out.encodeByte(ok ? 0 : 0x14);
        if (ok) {
            out.encodeByte(0);
        }
        return out;
    }

    /** LP0B1E (3CAC2D0), reply to CP00C4: 0 = registered (client sets CLogin+230 = 1), 6 = notice 0x25. */
    public static OutPacket secondaryPasswordRegisterResult(boolean ok) {
        OutPacket out = Tms282LoginPacketSupport.packet(0x0B1E);
        out.encodeByte(ok ? 0 : 6);
        return out;
    }

    /** LP0016 (3CA44F0): u8 result; 0 = notice 0x2B, 0x2A..0x36 mapped notices, others notice 0x0C. */
    public static OutPacket createMapleAccountResult(int result) {
        checkByte(result, "Create-account result");
        OutPacket out = Tms282LoginPacketSupport.packet(0x0016);
        out.encodeByte(result);
        return out;
    }

    /**
     * LP002B (3CAC7B0), LOW confidence: i32 -> app+0x23AC, str -> app+0x23E0 (read by LP0000, LP000B
     * and the cash shop; looks like a web/launcher token). Semantics unverified.
     */
    public static OutPacket webToken(int value, String token) {
        OutPacket out = Tms282LoginPacketSupport.packet(0x002B);
        out.encodeInt(value);
        str(out, Objects.requireNonNull(token, "token"), DEFAULT_CHARSET);
        return out;
    }

    /** LP002C (3CAC8F0): u8 a -> app+0x2460, u8 b -> app+0x2464, u8 refresh (1 = refresh CLogin+0x308 UI). */
    public static OutPacket secondaryPasswordFlags(boolean a, boolean b, boolean refreshUi) {
        OutPacket out = Tms282LoginPacketSupport.packet(0x002C);
        out.encodeByte(a);
        out.encodeByte(b);
        out.encodeByte(refreshUi);
        return out;
    }

    /**
     * LP002E (3CACA20): u8 result; 3 = update success followed by u8 setting (-> app+0x436C),
     * 4 = unknown error, 5 = OTP failed. setting must be 0 for 4/5 because it is not sent.
     * <p>
     * The setting byte is a conditional read: after result 3 the client decodes it only while 15D7140()
     * returns true (login-lp-layouts-2823.md). That condition is not modelled here: the byte is always sent
     * with result 3 and is simply left unread when the condition is false.
     */
    public static OutPacket otpSimplifyUpdateResult(int result, int setting) {
        if (result < 3 || result > 5) {
            throw new IllegalArgumentException("OTP update result must be 3, 4 or 5");
        }
        checkByte(setting, "OTP setting");
        if (result != 3 && setting != 0) {
            throw new IllegalArgumentException("OTP setting is only sent with result 3");
        }
        OutPacket out = Tms282LoginPacketSupport.packet(0x002E);
        out.encodeByte(result);
        if (result == 3) {
            out.encodeByte(setting);
        }
        return out;
    }

    /** LP002F (3CACE40): u8 result; 4 unknown error, 5 OTP failed, 8/0x10/0x18 already-registered notices. */
    public static OutPacket otpSimplifyRegisterResult(int result) {
        if (result != 4 && result != 5 && result != 8 && result != 0x10 && result != 0x18) {
            throw new IllegalArgumentException("Unknown OTP register result");
        }
        OutPacket out = Tms282LoginPacketSupport.packet(0x002F);
        out.encodeByte(result);
        return out;
    }

    /**
     * LP0030 (3CAB440): u8 result; 0 -> notice 0x43, 0x14/0x16/0x33/0x39/0x3B/0x3C/6/9 -> error notices,
     * 0x3A -> notice 0x0B and return to title. Always clears CLogin+0x220.
     */
    public static OutPacket secondaryPasswordResult(int result) {
        if (!Set.of(0, 6, 9, 0x14, 0x16, 0x33, 0x39, 0x3A, 0x3B, 0x3C).contains(result)) {
            throw new IllegalArgumentException("Unknown secondary-password result");
        }
        OutPacket out = Tms282LoginPacketSupport.packet(0x0030);
        out.encodeByte(result);
        return out;
    }

    /**
     * LP0032 (3CABB90): str url, read only after the player confirms notice (2,6); the client opens it
     * and, after another confirmation, re-sends CP007C.
     */
    public static OutPacket secondaryPasswordWebSetup(String url, Charset charset) {
        OutPacket out = Tms282LoginPacketSupport.packet(0x0032);
        str(out, url, charset);
        return out;
    }

    /** LP003F (inline), low-medium: u8 ok; ok opens the gender/second-password setup dialog, 0 = "try again". */
    public static OutPacket accountSetupDialog(boolean ok) {
        OutPacket out = Tms282LoginPacketSupport.packet(0x003F);
        out.encodeByte(ok);
        return out;
    }

    /** LP0040 (inline), low-medium: u8; 0 = failure notice, non-zero re-logs in with CLogin+0x700/+0x6F8. */
    public static OutPacket accountSetupResult(int value) {
        checkByte(value, "Account-setup result");
        OutPacket out = Tms282LoginPacketSupport.packet(0x0040);
        out.encodeByte(value);
        return out;
    }
}
