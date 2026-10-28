package net.swordie.ms.handlers.login.tms282;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.HyperTPRock;
import net.swordie.ms.connection.packet.Tms282FieldPackets;
import net.swordie.ms.connection.packet.Tms282MapTransferRequest;
import net.swordie.ms.connection.packet.Tms282PortalScriptRequest;
import net.swordie.ms.connection.packet.Tms282StatPackets;
import net.swordie.ms.connection.packet.Tms282TransferFieldRequest;
import net.swordie.ms.enums.FieldOption;
import net.swordie.ms.enums.MapTransferType;
import net.swordie.ms.handlers.header.InHeader;
import net.swordie.ms.world.field.Field;
import net.swordie.ms.world.field.Portal;

import java.nio.charset.Charset;
import java.util.Objects;

/**
 * TMS 282.3 field-transfer requests (phase 2), channel socket after SetField ({@link Tms282Guards#IN_FIELD}):
 * <ul>
 *     <li>CP019E / CP019F / CP01A0 USER_PORTAL_SCRIPT_REQUEST [verified] ({@link Tms282PortalScriptRequest}): the
 *     portal's script (265 MigrationHandler.handleUserPortalScriptRequest), then LP009D releases the request lock;</li>
 *     <li>CP01A3 USER_MAP_TRANSFER_REQUEST [verified] ({@link Tms282MapTransferRequest}): teleport-rock list edit on
 *     the 265 HyperTPRock list, answered with LP00B0 ({@link Tms282FieldPackets#mapTransferResult});</li>
 *     <li>USER_TRANSFER_FIELD_REQUEST (walking through a portal) on the opcode configured as
 *     {@code tms282.transferFieldOp} ({@link Tms282TransferSettings}); no route without it. The body is parsed with
 *     the 265 layout ({@link Tms282TransferFieldRequest}) and logged in full, so the first capture shows whether the
 *     layout holds; a portal name moves the user through {@link Tms282FieldTransfer}, anything else (revive, a
 *     field-change request the server did not ask for) follows 265 and is refused with LP02F4 when nothing moves.</li>
 * </ul>
 * Requests that do not parse are logged, answered with LP02F4 (which releases the lock) and never fatal.
 */
final class Tms282TransferHandlers {
    private final Charset charset;
    private final Tms282TransferSettings settings;
    private final Tms282FieldTransfer transfer;
    /** Takes USER_TRANSFER_FIELD_REQUEST first while its state matches (cash shop phase: the shop's exit). */
    private Interception interception;

    /** A command that handles the configured transfer-field opcode instead, while {@code when} holds. */
    record Interception(java.util.function.Predicate<Tms282LoginState> when, Tms282Command command) {
        Interception {
            Objects.requireNonNull(when, "when");
            Objects.requireNonNull(command, "command");
        }
    }

    Tms282TransferHandlers(Charset charset, Tms282TransferSettings settings) {
        this(charset, settings, new Tms282FieldTransfer());
    }

    Tms282TransferHandlers(Charset charset, Tms282TransferSettings settings, Tms282FieldTransfer transfer) {
        this.charset = Objects.requireNonNull(charset, "charset");
        this.settings = Objects.requireNonNull(settings, "settings");
        this.transfer = Objects.requireNonNull(transfer, "transfer");
    }

    /** Installs {@code interception} (one at most); returns this. */
    Tms282TransferHandlers intercept(Interception interception) {
        this.interception = Objects.requireNonNull(interception, "interception");
        return this;
    }

    void register(Tms282Routes routes) {
        routes.channel(Tms282PortalScriptRequest.OPCODE, InHeader.USER_PORTAL_SCRIPT_REQUEST).when(Tms282Guards.IN_FIELD)
                .handle("portal script", this::portalScript);
        routes.channel(Tms282PortalScriptRequest.OPCODE_2, InHeader.USER_PORTAL_SCRIPT_REQUEST_2).when(Tms282Guards.IN_FIELD)
                .handle("portal script", this::portalScript);
        routes.channel(Tms282PortalScriptRequest.OPCODE_3, InHeader.USER_PORTAL_SCRIPT_REQUEST_3).when(Tms282Guards.IN_FIELD)
                .handle("portal script", this::portalScript);
        routes.channel(Tms282MapTransferRequest.OPCODE, InHeader.USER_MAP_TRANSFER_REQUEST).when(Tms282Guards.IN_FIELD)
                .handle("teleport rock list -> LP00B0", this::mapTransfer);
        settings.transferFieldOp().ifPresent(op -> routes.channel(op, InHeader.USER_TRANSFER_FIELD_REQUEST)
                .when(Tms282Guards.IN_FIELD).handle("transfer field (configured opcode) -> LP02EF or LP02F4",
                        this::transferField));
    }

    // ---- CP019E / 019F / 01A0 ------------------------------------------------------------------------------------

    private void portalScript(Tms282LoginState session, int op, byte[] packet) {
        Char chr = session.fieldCharacter();
        Tms282PortalScriptRequest request;
        try {
            request = Tms282PortalScriptRequest.parse(packet, charset);
        } catch (IllegalArgumentException e) {
            dropped(chr, op, packet, e);
            return;
        }
        System.out.printf("[TMS282] CHANNEL %04X portal script \"%s\" at (%d,%d)%n", op, request.portalName(),
                request.x(), request.y());
        guarded(chr, op, () -> portalScript(chr, request.portalName()));
    }

    /** 265 handleUserPortalScriptRequest: the portal's script, else a script named like the portal. */
    Tms282FieldTransfer.Outcome portalScript(Char chr, String portalName) {
        if (chr.getHP() <= 0 || chr.getField() == null) {
            chr.write(Tms282StatPackets.unlock());
            return Tms282FieldTransfer.Outcome.REFUSED;
        }
        Portal portal = chr.getField().getPortalByName(portalName);
        if (portal == null) {
            return transfer.refuse(chr, Tms282FieldPackets.Reason.SILENT, "unknown portal \"" + portalName + "\"");
        }
        String script = portal.getScript() == null || portal.getScript().isEmpty() ? portalName : portal.getScript();
        if (script.equals("pt_mutoHotPot")) {
            chr.checkIngredients();
            chr.write(Tms282StatPackets.unlock());
            return Tms282FieldTransfer.Outcome.SCRIPT;
        }
        return transfer.runPortalScript(chr, portal.getId(), script);
    }

    // ---- CP01A3 --------------------------------------------------------------------------------------------------

    private void mapTransfer(Tms282LoginState session, int op, byte[] packet) {
        Char chr = session.fieldCharacter();
        Tms282MapTransferRequest request;
        try {
            request = Tms282MapTransferRequest.parse(packet);
        } catch (IllegalArgumentException e) {
            dropped(chr, op, packet, e);
            return;
        }
        guarded(chr, op, () -> mapTransfer(chr, request));
    }

    /** 265 handleUserMapTransferRequest (delete / register) with the 282 LP00B0 reply. */
    void mapTransfer(Char chr, Tms282MapTransferRequest request) {
        chr.punishLieDetectorEvasion();
        if (chr.getHP() <= 0) {
            chr.write(Tms282StatPackets.unlock());
            return;
        }
        MapTransferType type = MapTransferType.getByVal((byte) request.type());
        if (type == MapTransferType.DeleteListRecv && request.fieldId().isPresent()) {
            HyperTPRock.removeFieldId(chr, request.fieldId().getAsInt());
            chr.write(Tms282FieldPackets.mapTransferResult(MapTransferType.DeleteListSend.getVal(), request.itemType(),
                    chr.getHyperRockFields()));
        } else if (type == MapTransferType.RegisterListRecv) {
            Field field = chr.getField();
            if (field == null || (field.getFieldLimit() & FieldOption.TeleportItemLimit.getVal()) > 0
                    || chr.getLevel() < field.getLvLimit()) {
                chr.write(Tms282FieldPackets.mapTransferResult(MapTransferType.RegisterFail.getVal(), request.itemType(), null));
                return;
            }
            HyperTPRock.addFieldId(chr, chr.getFieldID());
            chr.write(Tms282FieldPackets.mapTransferResult(MapTransferType.RegisterListSend.getVal(), request.itemType(),
                    chr.getHyperRockFields()));
        } else {
            chr.write(Tms282StatPackets.unlock());
        }
    }

    // ---- USER_TRANSFER_FIELD_REQUEST (configured opcode) ---------------------------------------------------------

    private void transferField(Tms282LoginState session, int op, byte[] packet) throws Exception {
        if (interception != null && interception.when().test(session)) {
            interception.command().execute(session, op, packet);
            return;
        }
        Char chr = session.fieldCharacter();
        Tms282TransferFieldRequest request;
        try {
            request = Tms282TransferFieldRequest.parse(packet, op, charset);
        } catch (IllegalArgumentException e) {
            dropped(chr, op, packet, e);
            return;
        }
        System.out.println("[TMS282] CHANNEL " + request.describe());
        guarded(chr, op, () -> transferField(chr, request));
    }

    /** 265 handleUserTransferFieldRequest without the cash-shop and event branches. */
    Tms282FieldTransfer.Outcome transferField(Char chr, Tms282TransferFieldRequest request) {
        if (request.hasPortal()) {
            return transfer.throughPortal(chr, request.portalName());
        }
        if (chr.getHP() <= 0) {
            // Revive (death window "OK"): 265 Char.revive warps to the field's returnMap (the 282 Map.wz info when
            // the TMS 282 data overlay is installed; 999999999 -> the previous field) and heals HP/MP to full. Its
            // 265 STAT_CHANGED is dropped by the guard, so the 282 HP/MP/EXP changes go out as LP009D (daily-play phase).
            java.util.List<net.swordie.ms.connection.OutPacket> inPlace = new Tms282ReviveService(charset)
                    .reviveInPlace(chr, net.swordie.ms.connection.packet.Tms282ReviveRequest.fromTransfer(request));
            if (!inPlace.isEmpty()) {
                // premium option + revive item (guild/bags phase): stay on the field, no returnMap
                inPlace.forEach(chr::write);
                return Tms282FieldTransfer.Outcome.MOVED;
            }
            net.swordie.ms.client.character.tms282.Tms282StatDiff stats = net.swordie.ms.client.character.tms282.Tms282StatDiff.of(chr);
            chr.revive(request.targetField());
            chr.write(stats.packet(true));
            return Tms282FieldTransfer.Outcome.MOVED;
        }
        Field current = chr.getField();
        if (current != null && chr.getTransferField() == request.targetField() && chr.getTransferFieldReq() == current.getId()) {
            Field toField = chr.getOrCreateFieldByCurrentInstanceType(chr.getTransferField());
            chr.setTransferField(0);
            if (toField != null && request.targetField() > 0) {
                return transfer.warp(chr, toField, toField.getPortalByName("sp") != null
                        ? toField.getPortalByName("sp") : toField.getDefaultPortal());
            }
        }
        return transfer.refuse(chr, Tms282FieldPackets.Reason.CANNOT_MOVE, "no portal and no pending transfer to "
                + request.targetField());
    }

    // ---- shared ----------------------------------------------------------------------------------------------------

    private void dropped(Char chr, int op, byte[] packet, IllegalArgumentException e) {
        System.out.printf("[TMS282] CHANNEL %04X len=%d does not parse (%s); refused%n", op, packet.length, e.getMessage());
        chr.write(Tms282FieldPackets.transferFieldReqIgnored(Tms282FieldPackets.Reason.SILENT));
    }

    /** Runs the 265 server-side part; a failure is logged and the request refused (LP02F4 releases the lock). */
    private void guarded(Char chr, int op, Runnable action) {
        try {
            action.run();
        } catch (RuntimeException e) {
            System.out.printf("[TMS282] CHANNEL %04X of character %d: server-side 265 logic failed: %s%n", op, chr.getId(), e);
            chr.write(Tms282FieldPackets.transferFieldReqIgnored(Tms282FieldPackets.Reason.SILENT));
        }
    }
}
