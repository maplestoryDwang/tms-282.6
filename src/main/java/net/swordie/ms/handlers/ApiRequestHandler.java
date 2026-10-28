package net.swordie.ms.handlers;

import net.swordie.ms.Server;
import net.swordie.ms.ServerConstants;
import net.swordie.ms.client.Client;
import net.swordie.ms.client.User;
import net.swordie.ms.connection.InPacket;
import net.swordie.ms.connection.packet.ApiResponse;
import net.swordie.ms.constants.DevAccounts;
import net.swordie.ms.enums.AccountCreateResult;
import net.swordie.ms.enums.ApiTokenResultType;
import net.swordie.ms.util.DataPrinter;
import net.swordie.ms.util.Util;
import org.mindrot.jbcrypt.BCrypt;

import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ApiRequestHandler {

    public static final int RND_BITS = 11;
    private static final int USER_BITS = 19;
    private static final int PREFIX_BITS = 1;

    private static final int RND_MASK  = (1 << RND_BITS) - 1;
    public static final int USER_MASK = (1 << USER_BITS) - 1;

    private static final int PREFIX_SHIFT = USER_BITS + RND_BITS;
    private static final int USER_SHIFT   = RND_BITS;
    private static final int PREFIX_VALUE = 1; // luôn 1 để token lớn

    public static void handleTokenX64Request(Client c, InPacket inPacket) {
        String name = inPacket.decodeString();
        String password = inPacket.decodeString();
        boolean wrongWZFiles = false;
        // health audit step 2: no DEBUG_MODE exemption (DEBUG_MODE is true on the formal server)
        if (c.getLastAPIAttempt() + 5000 < System.currentTimeMillis()) {
            c.setLastAPIAttempt(System.currentTimeMillis());
        } else {
            c.write(ApiResponse.tokenRequestResult(ApiTokenResultType.TooManyRequest, "tooManyRequest"));
            return;
        }
        User user = User.getUserFromSQLForAPI(name);
        boolean success = false;
        if (user == null) {
            c.write(ApiResponse.tokenRequestResult(ApiTokenResultType.InvalidUserPassCombination, "wrongPassword"));
        } else {
            String dbPassword = user.getPassword();
            boolean hashed = Util.isStringBCrypt(dbPassword);
            if (hashed) {
                try {
                    success = BCrypt.checkpw(password, dbPassword);
                } catch (IllegalArgumentException e) { // if password hashing went wrong
                    System.out.printf("bcrypt check in login has failed! dbPassword: %s; stack trace: %s%n", dbPassword, e.getStackTrace().toString());
                    success = false;
                }
            } else {
                success = password.equals(dbPassword);
            }
        }
        if (success) {
            if (wrongWZFiles) {
                c.write(ApiResponse.tokenRequestResult(ApiTokenResultType.WrongWZFiles, "wrongWZFiles"));
            } else if (user.getBanExpireDate() != null && !user.getBanExpireDate().isExpired()) {
                c.write(ApiResponse.tokenRequestResult(ApiTokenResultType.Banned, "banned"));
            } else if (Server.get().getUserById(user.getId()) != null) {
                c.write(ApiResponse.tokenRequestResult(ApiTokenResultType.AlreadyLogin, "alreadyLogin"));
            } else {
                // Generate token
                final int token = getTokenPacked(user.getId());
                Server.get().addAuthToken(token, user.getId());
                c.write(ApiResponse.tokenRequestResult(ApiTokenResultType.Success, token));
                DataPrinter.send(DataPrinter.LOGIN, String.format("[IP: %s] 成功登錄賬號 %s，並生成 token: %d。", c.getIP(), name, token));
            }
        } else {
            c.write(ApiResponse.tokenRequestResult(ApiTokenResultType.InvalidUserPassCombination, "wrongPassword"));
        }
    }

    private static int getTokenPacked(int userId) {
        if ((userId & ~USER_MASK) != 0) {
            throw new IllegalArgumentException("userId too large for packed token");
        }
        for (int i = 0; i < 64; i++) {
            int rnd = ThreadLocalRandom.current().nextInt(1 << RND_BITS);
            int token = (PREFIX_VALUE << PREFIX_SHIFT) | (userId << USER_SHIFT) | rnd; // luôn dương
            if (!Server.get().isTokenExist(token)) return token;
        }
        // fallback
        return ThreadLocalRandom.current().nextInt(1_000_000_000, Integer.MAX_VALUE);
    }

    public static void handleCreateAccountRequest(Client c, InPacket inPacket) {
        String name = inPacket.decodeString();
        String pwd = inPacket.decodeString();
        String email = inPacket.decodeString();
        if (c.getLastAPIAttempt() + 5000 < System.currentTimeMillis()) {
            c.setLastAPIAttempt(System.currentTimeMillis());
        } else {
            c.write(ApiResponse.createAccountResult(AccountCreateResult.Unknown));
            return;
        }
        AccountCreateResult acr = AccountCreateResult.Success;
        User user = User.getUserFromSQLForAPI(name);
        Pattern p = Pattern.compile("[^a-z0-9 ]", Pattern.CASE_INSENSITIVE);
        if (user != null) {
            acr = AccountCreateResult.NameInUse;
        }
        if (DevAccounts.isForbiddenID(name)) {
            acr = AccountCreateResult.Unknown;
        }
        if (acr == AccountCreateResult.Success) {
            user = new User(name, pwd);
            boolean hashed = Util.isStringBCrypt(pwd);
            if (!hashed) {
                user.setPassword(BCrypt.hashpw(user.getPassword(), BCrypt.gensalt(ServerConstants.BCRYPT_ITERATIONS)));
                if (user.getPic() != null && user.getPic().length() >= 6 && !Util.isStringBCrypt(user.getPic())) {
                    user.setPic(BCrypt.hashpw(user.getPic(), BCrypt.gensalt(ServerConstants.BCRYPT_ITERATIONS)));
                }
            }
            user.setEmail(email);
            user.insertUserToSQL();
            DataPrinter.send(DataPrinter.REGISTER, String.format("[IP: %s] 成功註冊賬號 %s，Email %s。", c.getIP(), name, email));
        }
        c.write(ApiResponse.createAccountResult(acr));
    }
}
