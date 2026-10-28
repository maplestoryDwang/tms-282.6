package net.swordie.ms.client.character.tms282;

import net.swordie.ms.client.character.skills.Option;
import net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat;
import net.swordie.ms.connection.packet.Tms282RemoteStatSource;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry of the {@link Tms282BuffBook} each online character's socket holds, so that USER_ENTER_FIELD for that
 * character (built for another user, on another thread) can carry its buffs ({@link Tms282RemoteStatSource}). A book
 * is published by its session when the first buff is set and withdrawn when the socket closes; withdrawing only
 * removes the entry when it is still that book (a newer socket of the same character may have replaced it).
 */
public final class Tms282BuffDirectory implements Tms282RemoteStatSource {
    /** The server's directory. */
    public static final Tms282BuffDirectory INSTANCE = new Tms282BuffDirectory();

    private final Map<Integer, Tms282BuffBook> books = new ConcurrentHashMap<>();

    public void publish(int characterId, Tms282BuffBook book) {
        books.put(characterId, Objects.requireNonNull(book, "book"));
    }

    public void withdraw(int characterId, Tms282BuffBook book) {
        if (books.remove(characterId, book)) book.release();
    }

    /** The book the character's socket published, or null (lucidfly-0930: Lucid flight landing). */
    public Tms282BuffBook book(int characterId) {
        return books.get(characterId);
    }

    public boolean tryStartCooldown(int characterId, int skillId, long nowMillis, long durationMillis) {
        Tms282BuffBook book = books.get(characterId);
        return book != null && book.tryStartCooldown(skillId, nowMillis, durationMillis);
    }

    public boolean consumeTanjiroDashAttack(int characterId, long nowMillis) {
        Tms282BuffBook book = books.get(characterId);
        return book != null && book.consumeTanjiroDashAttack(nowMillis);
    }

    @Override
    public Map<CharacterTemporaryStat, List<Option>> activeStats(int characterId) {
        Tms282BuffBook book = books.get(characterId);
        return book == null ? Map.of() : book.snapshot();
    }
}
