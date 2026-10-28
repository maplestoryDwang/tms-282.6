package net.swordie.ms.client.social.Party;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Pending party invitations (multiplayer-0930). The 282 answer CP01F3 type 47 only carries the inviter id; before
 * this, any client could send "accept" for any party and join it without an invitation. An invitation is valid for
 * {@link #TTL_MS}; the last invitation to a character replaces an earlier one.
 */
public final class PartyInvites {
    public static final long TTL_MS = 60_000L;

    private record Pending(int partyId, int inviterId, long expiresAt) {
    }

    private static final Map<Integer, Pending> BY_INVITED = new ConcurrentHashMap<>();

    private PartyInvites() {
    }

    public static void offer(int invitedId, int partyId, int inviterId) {
        offer(invitedId, partyId, inviterId, System.currentTimeMillis());
    }

    static void offer(int invitedId, int partyId, int inviterId, long now) {
        BY_INVITED.put(invitedId, new Pending(partyId, inviterId, now + TTL_MS));
    }

    /** True while an unexpired invitation of {@code partyId} to {@code invitedId} is pending. */
    public static boolean isPending(int invitedId, int partyId) {
        Pending p = BY_INVITED.get(invitedId);
        return p != null && p.partyId() == partyId && p.expiresAt() >= System.currentTimeMillis();
    }

    /** Removes the invitation; true when an unexpired invitation of {@code partyId} was pending (accept is valid). */
    public static boolean consume(int invitedId, int partyId) {
        Pending p = BY_INVITED.get(invitedId);
        if (p == null || p.partyId() != partyId) {
            return false;
        }
        BY_INVITED.remove(invitedId, p);
        return p.expiresAt() >= System.currentTimeMillis();
    }

    /** Forgets any invitation to {@code invitedId}. */
    public static void clear(int invitedId) {
        BY_INVITED.remove(invitedId);
    }
}
