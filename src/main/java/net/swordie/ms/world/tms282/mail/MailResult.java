package net.swordie.ms.world.tms282.mail;

/** Outcome of a memo / parcel request; {@code message} is the player text (Traditional Chinese). */
public record MailResult(boolean ok, String message) {
    public static MailResult ok(String message) {
        return new MailResult(true, message);
    }

    public static MailResult fail(String message) {
        return new MailResult(false, message);
    }
}
