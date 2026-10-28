package net.swordie.ms.handlers.login.tms282;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.tms282.Tms282BuffBook;
import net.swordie.ms.client.character.tms282.Tms282BuffDirectory;
import net.swordie.ms.client.character.tms282.Tms282BuffExpiry;
import net.swordie.ms.client.character.tms282.Tms282SkillService;

import java.util.Objects;
import java.util.function.LongSupplier;

/**
 * One channel socket's CP04B4 buffs and cooldowns ({@link Tms282BuffBook}) and the {@link Tms282BuffExpiry} timer
 * that sends LP009F when they end (the research probe's {@code buffs} / {@code buffExpiry} / armBuffExpiry). The
 * timer is created on the first accepted skill use and cancelled when the socket closes
 * ({@link Tms282LoginState} stops it with the other session timers); after that it is never armed again.
 * <p>
 * LP009F goes to the owner; the expiry's LP0490 / LP048F go to the other users of the owner's current field
 * ({@link Tms282RemoteAudience}). The book is published in {@link Tms282BuffDirectory} while the socket lives, so
 * LP03D0 for this character shows its buffs to users who enter the field later.
 */
final class Tms282SessionBuffs {
    private final Tms282BuffBook book = new Tms282BuffBook();
    private final Tms282BuffDirectory directory;
    private Tms282BuffExpiry expiry;
    private boolean closed;
    private int publishedAs;

    Tms282SessionBuffs() {
        this(Tms282BuffDirectory.INSTANCE);
    }

    Tms282SessionBuffs(Tms282BuffDirectory directory) {
        this.directory = java.util.Objects.requireNonNull(directory, "directory");
    }

    Tms282BuffBook book() {
        return book;
    }

    /** After an accepted use: (re)arms the expiry for the book's earliest end. */
    synchronized void arm(Tms282LoginState session, int characterId, Tms282SkillService service, LongSupplier clock) {
        if (closed) {
            return;
        }
        if (publishedAs != characterId) {
            if (publishedAs != 0) {
                directory.withdraw(publishedAs, book);
            }
            directory.publish(characterId, book);
            publishedAs = characterId;
        }
        if (expiry == null) {
            Objects.requireNonNull(session, "session");
            expiry = new Tms282BuffExpiry(service, book, new Tms282BuffExpiry.Sink() {
                @Override
                public void send(net.swordie.ms.connection.OutPacket packet) {
                    session.send(packet);
                }

                @Override
                public void broadcast(net.swordie.ms.connection.OutPacket packet) {
                    Char chr = session.fieldCharacter();
                    if (chr != null && chr.getId() == characterId) {
                        Tms282RemoteAudience.FIELD_OTHERS.broadcast(chr, packet);
                    }
                }

                @Override
                public void log(String line) {
                    System.out.println("[TMS282] " + line + " character=" + characterId);
                }
            }, clock, characterId);
        }
        expiry.arm();
    }

    /** Re-arms an existing timer (checks, after moving the clock); nothing when none exists or closed. */
    synchronized void rearm() {
        if (!closed && expiry != null) {
            expiry.arm();
        }
    }

    /** Socket closed: the expiry timer stops for good. */
    synchronized void cancel() {
        closed = true;
        if (expiry != null) {
            expiry.cancel();
        }
        if (publishedAs != 0) {
            directory.withdraw(publishedAs, book);
            publishedAs = 0;
        }
    }

    synchronized boolean cancelled() {
        return closed;
    }
}
