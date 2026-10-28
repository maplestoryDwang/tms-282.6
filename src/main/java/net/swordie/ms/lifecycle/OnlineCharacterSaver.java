package net.swordie.ms.lifecycle;

import net.swordie.ms.Server;
import net.swordie.ms.client.Account;
import net.swordie.ms.client.Client;
import net.swordie.ms.client.User;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.tms282.EconomyLocks;
import net.swordie.ms.client.character.tms282.Tms282CashExpansionService;

/**
 * Saves one character that is still online, without logging it out (the periodic save). Takes the same locks as
 * every other character save started outside the global monitor - {@code save gate -> chr} ({@link EconomyLocks}
 * step 6; {@code ECONOMY_MONITOR -> chr} with tms282.save.outsideEconomy=false), then the user - and checks again
 * under them that the character is still the online one of its client: a character that logged out in the meantime is left to the logout save (finalizeSession),
 * so an old object is never written over a newer login.
 * <p>
 * What is written is what a cash-shop entry writes ({@code User.saveToSQL(false)}: character, world account, user
 * row); when the user's cash balance is uncertain (an unfinished 282 cash purchase) the user row is skipped as
 * User.saveToSQL does, but character and account are still saved.
 */
public final class OnlineCharacterSaver {
    public enum Result {SAVED, OFFLINE, NOT_IN_FIELD, FAILED}

    /** Test seam: replaces the actual SQL writes (null = the real saves). */
    interface Writer {
        void write(Char chr, Client client, User user);
    }

    static volatile Writer writer;

    private OnlineCharacterSaver() {
    }

    public static Result save(Char chr) {
        if (chr == null || chr.isBot()) {
            return Result.OFFLINE;
        }
        Client client = chr.getClient();
        if (!online(chr, client)) {
            return Result.OFFLINE;
        }
        if (client.getCurrentState() != Client.IN_FIELD || client.getNextState() != Client.NONE) {
            return Result.NOT_IN_FIELD;
        }
        try {
            Result[] out = {Result.FAILED};
            // step 6: save gate -> chr -> user, without ECONOMY_MONITOR (EconomyLocks.saveWithoutEconomy); the old
            // ECONOMY_MONITOR -> chr -> user when tms282.save.outsideEconomy=false
            if (EconomyLocks.saveWithoutEconomy(chr, () -> {
                out[0] = locked(chr, client);
                return null;
            })) {
                return out[0];
            }
            return EconomyLocks.call(chr, () -> locked(chr, client));
        } catch (Throwable t) {
            System.out.printf("[PeriodicSave] character %d (%s) save failed: %s%n", chr.getId(), chr.getName(), t);
            return Result.FAILED;
        }
    }

    /** Under the character's monitor (and its gate, or ECONOMY_MONITOR): checks again, then writes. */
    private static Result locked(Char chr, Client client) {
        if (!online(chr, client)) {
            return Result.OFFLINE;
        }
        if (client.getCurrentState() != Client.IN_FIELD || client.getNextState() != Client.NONE) {
            return Result.NOT_IN_FIELD;
        }
        User user = client.getUser();
        if (user == null) {
            write(chr, client, null);
            return Result.SAVED;
        }
        synchronized (user) {
            write(chr, client, user);
        }
        return Result.SAVED;
    }

    private static boolean online(Char chr, Client client) {
        return client != null && client.getChr() == chr && Server.get().getChars().get(chr.getId()) == chr
                && client.getCh() != null && client.getCh().isActive();
    }

    private static void write(Char chr, Client client, User user) {
        Writer w = writer;
        if (w != null) {
            w.write(chr, client, user);
            return;
        }
        if (user != null && user.getCurrentChr() == chr && !Tms282CashExpansionService.isBalanceUncertain(user)) {
            user.saveToSQL(false); // character + current world account + user row
            return;
        }
        chr.saveToSQL();
        Account account = client.getAccount() != null ? client.getAccount() : chr.getAccount();
        if (account != null) {
            account.saveToSQL();
        }
    }
}
