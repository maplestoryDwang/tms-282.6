package net.swordie.ms.handlers.login.tms282;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.packet.Tms282FieldPackets;
import net.swordie.ms.connection.packet.Tms282StatPackets;
import net.swordie.ms.scripts.ScriptType;
import net.swordie.ms.world.field.Field;
import net.swordie.ms.world.field.Portal;

import java.util.Objects;

/**
 * TMS 282.3 field change (strategy used by {@link Tms282TransferHandlers}): resolves a portal of the 265 field data
 * (target field and target portal, as 265 MigrationHandler does) and moves the character with the 265 server-side
 * warp {@link Char#warp(Field, Portal)}.
 * <p>
 * The warp does the whole 265 field change in the 265 order: LP03D1 to the users of the old field, LP03D0 to the
 * users of the new field, then the field-change SetField LP02EF ({@code Stage.setField} defers to
 * {@code Tms282StageEncoder}: {@code Tms282StagePackets.setFieldChangeField} with the target field and portal index),
 * then the new field's users (LP03D0), NPCs (LP072B / LP072D) and mobs. 265 packets without a 282 codec are dropped
 * by the fail-closed OutboundGuard, as on field entry.
 * <p>
 * A refused request answers LP02F4 TRANSFER_FIELD_REQ_IGNORED, which also releases the client's request lock.
 */
final class Tms282FieldTransfer {
    /** What a request led to. */
    enum Outcome {
        /** The character was warped (LP02EF sent). */
        MOVED,
        /** A portal script was started (it may warp by itself). */
        SCRIPT,
        /** Refused with LP02F4. */
        REFUSED
    }

    /**
     * Walks {@code chr} through {@code portalName} of its field: a scripted portal runs its script (then the request
     * lock is released like 265 {@code chr.dispose()}), a plain portal warps to its target field and portal ("sp"
     * when the target portal is missing, 265).
     */
    Outcome throughPortal(Char chr, String portalName) {
        Objects.requireNonNull(portalName, "portalName");
        Field field = chr.getField();
        if (field == null) {
            return refuse(chr, Tms282FieldPackets.Reason.CANNOT_MOVE, "no field");
        }
        Portal portal = field.getPortalByName(portalName);
        if (portal == null) {
            return refuse(chr, Tms282FieldPackets.Reason.CANNOT_MOVE, "portal \"" + portalName + "\" not in field " + field.getId());
        }
        String script = portal.getScript();
        if (script != null && !script.isEmpty()) {
            return runPortalScript(chr, portal.getId(), script);
        }
        return toTarget(chr, portal);
    }

    /** Warps to {@code portal}'s target (265 USER_TRANSFER_FIELD_REQUEST, plain portal). */
    Outcome toTarget(Char chr, Portal portal) {
        Field toField = chr.getOrCreateFieldByCurrentInstanceType(portal.getTargetMapId());
        if (toField == null) {
            return refuse(chr, Tms282FieldPackets.Reason.CANNOT_MOVE, "target field " + portal.getTargetMapId()
                    + " of portal \"" + portal.getName() + "\" missing from server data");
        }
        Portal toPortal = toField.getPortalByName(portal.getTargetPortalName());
        if (toPortal == null) {
            toPortal = toField.getPortalByName("sp");
        }
        if (toPortal == null) {
            toPortal = toField.getDefaultPortal();
        }
        return warp(chr, toField, toPortal);
    }

    /** The 265 server-side warp; its SetField is the 282 field change (LP02EF). */
    Outcome warp(Char chr, Field toField, Portal toPortal) {
        int from = chr.getField() == null ? -1 : chr.getField().getId();
        chr.warp(toField, toPortal);
        System.out.printf("[TMS282] CHANNEL field change character=%d %d -> %d portal=%s%n", chr.getId(), from,
                toField.getId(), toPortal == null ? "-" : toPortal.getId() + " (" + toPortal.getName() + ")");
        return Outcome.MOVED;
    }

    /**
     * Portal script (265 USER_PORTAL_SCRIPT_REQUEST): starts {@code script}, then releases the request lock with
     * LP009D (265 {@code chr.dispose()}).
     */
    Outcome runPortalScript(Char chr, int portalId, String script) {
        chr.getScriptManager().startScript(chr, portalId, script, ScriptType.Portal);
        chr.write(Tms282StatPackets.unlock());
        return Outcome.SCRIPT;
    }

    /** LP02F4 with {@code reason}; logged. */
    Outcome refuse(Char chr, Tms282FieldPackets.Reason reason, String why) {
        System.out.printf("[TMS282] CHANNEL field change of character %d refused (%s): %s%n", chr.getId(), reason, why);
        chr.write(Tms282FieldPackets.transferFieldReqIgnored(reason));
        return Outcome.REFUSED;
    }
}
