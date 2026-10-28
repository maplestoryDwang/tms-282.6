package net.swordie.ms.world.field;

import net.swordie.ms.Server;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.social.Party.Party;
import net.swordie.ms.constants.FieldConstants;

import java.util.ArrayList;
import java.util.List;

/**
 * Per-member membership of an {@link Instance} (multiplayer-0930).
 * <p>
 * A party instance is shared by every member that was online when it was set up ({@link Instance#getChars()}).
 * Before this class, a death with no death count left, a relogin, or the boss lobby exit ran
 * {@code warpInstanceOut}, which cleared the whole instance and warped every online party member (also those in
 * town) to the forced-return map. Official behaviour: only that character leaves; the instance stays for the
 * members still inside and closes when the last one is gone (or on its timeout, {@link Instance#clear()}).
 */
public final class InstanceMembership {
    private InstanceMembership() {
    }

    /** chr stands on one of {@code instance}'s field copies (the very object, not a channel copy with the same id). */
    public static boolean isInside(Char chr, Instance instance) {
        if (chr == null || instance == null) {
            return false;
        }
        Field f = chr.getField();
        return f != null && instance.getFields().get(f.getId()) == f;
    }

    /** Online eligible characters of {@code instance} that stand inside it. */
    public static List<Char> presentMembers(Instance instance) {
        List<Char> present = new ArrayList<>();
        if (instance == null) {
            return present;
        }
        for (Char c : new ArrayList<>(instance.getChars().values())) {
            if (c != null && c.getClient() != null && isInside(c, instance)) {
                present.add(c);
            }
        }
        return present;
    }

    /**
     * Only {@code chr} leaves its instance and is warped to {@code fieldId} ({@code < 0}: the instance's forced
     * return map and portal). The other members stay; the instance is closed when nobody is left inside.
     */
    public static void leave(Char chr, int fieldId, int portalId) {
        if (chr == null) {
            return;
        }
        Instance instance = chr.getInstance();
        if (instance == null) {
            if (fieldId >= 0) {
                chr.warp(fieldId, Math.max(portalId, 0), false);
            }
            return;
        }
        int target;
        int portal;
        if (fieldId >= 0) {
            target = fieldId;
            portal = Math.max(portalId, 0);
        } else {
            target = instance.getForcedReturn();
            portal = Math.max(instance.getForcedReturnPortalId(), 0);
        }
        if (target <= 0 || target == FieldConstants.NO_MAP_ID) {
            Field f = chr.getField();
            target = f != null && f.getForcedReturn() != FieldConstants.NO_MAP_ID ? f.getForcedReturn() : FieldConstants.HENESYS_ID;
            portal = 0;
        }
        chr.setInstance(null); // death count, debuffs, QR 102401, script events; removes chr from getChars()
        instance.removeChar(chr);
        try {
            chr.warp(target, portal, false);
        } finally {
            closeIfAbandoned(instance);
        }
    }

    /**
     * Disconnect while in an instance: chr leaves the instance (the others keep it) and, when its map has no forced
     * return of its own, the saved map becomes the instance's forced return, so the relogin does not land in a
     * channel copy of the boss map. Call before {@code field.removeChar}.
     */
    public static void onDisconnect(Char chr) {
        if (chr == null) {
            return;
        }
        Instance instance = chr.getInstance();
        if (instance == null) {
            return;
        }
        Field f = chr.getField();
        if (isInside(chr, instance) && (f.getForcedReturn() <= 0 || f.getForcedReturn() == FieldConstants.NO_MAP_ID)) {
            int ret = instance.getForcedReturn();
            if (ret > 0 && ret != FieldConstants.NO_MAP_ID) {
                chr.setFieldID(ret);
            }
        }
        chr.setInstance(null);
        instance.removeChar(chr);
        closeIfAbandoned(instance);
    }

    /**
     * Field entry (login / channel change): the character is placed in a channel field, never inside an instance.
     * A stale eligibility left from the previous session (same character id) is dropped; the instance of the other
     * members is not touched. When the saved map is one of the instance's maps, the character is sent to the
     * instance's forced return map (the channel copy of a boss map is not a place to stand in).
     */
    public static void onFieldEntry(Char chr) {
        if (chr == null) {
            return;
        }
        Instance instance = chr.getInstance();
        if (instance == null || isInside(chr, instance)) {
            return;
        }
        boolean onInstanceMap = chr.getField() != null && instance.getFields().containsKey(chr.getField().getId());
        int ret = instance.getForcedReturn();
        instance.getChars().remove(chr.getId());
        closeIfAbandoned(instance);
        if (onInstanceMap && ret > 0 && ret != FieldConstants.NO_MAP_ID) {
            chr.warp(ret, Math.max(instance.getForcedReturnPortalId(), 0), false);
        }
    }

    /** Closes the instance (timer, fields, party link) when no online eligible member stands inside it. */
    public static boolean closeIfAbandoned(Instance instance) {
        if (instance == null || !presentMembers(instance).isEmpty()) {
            return false;
        }
        instance.stopEvents();
        Party party = instance.getParty();
        if (party != null && party.getInstance() == instance) {
            party.setInstance(null);
        }
        instance.getFields().clear();
        instance.getChars().clear();
        instance.getProperties().clear();
        Server.get().getInstances().remove(instance);
        return true;
    }
}
