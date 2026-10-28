package net.swordie.ms.client;

import java.time.LocalDateTime;

/** Existing monthly MVP rules, including the five calendar days after expiry. */
public final class MvpMembership {
    private MvpMembership() { }
    public static boolean expired(LocalDateTime expiry, LocalDateTime now) {
        return expiry == null || !now.toLocalDate().isBefore(expiry.toLocalDate().plusDays(6));
    }
    public static boolean mayRenew(int grade, LocalDateTime expiry, LocalDateTime now) {
        return grade == 0 || (grade >= 1 && grade <= 4 && expiry != null
                && !now.isBefore(expiry) && !expired(expiry, now));
    }
    public static boolean dailyAvailable(LocalDateTime last, LocalDateTime now) {
        return last == null || last.toLocalDate().isBefore(now.toLocalDate());
    }
    public static int nextCost(int grade) {
        return switch (grade) { case 0 -> 249000; case 1 -> 350000; case 2 -> 600000;
            case 3 -> 1000000; case 4 -> 750000; default -> -1; };
    }
}
