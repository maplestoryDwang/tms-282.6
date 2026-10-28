package net.swordie.ms.connection.packet;

import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.protocol.codec.ScriptMessageEncoder;
import net.swordie.ms.life.npc.NpcMessageType;
import net.swordie.ms.life.npc.NpcScriptInfo;

import java.nio.charset.Charset;
import java.util.Objects;

/**
 * LP0960 SCRIPT_MESSAGE of TMS 282.3 [verified: CField Script/Shop sub-dispatcher 42B49C0, handler 42B49F0], the
 * {@link ScriptMessageEncoder} of the 282 profile. Frame:
 * <pre>
 * MAIN: i32 objectId, u8 speakerType, i32 template, u8 hasOverride, [i32 override], u8 msgType, i16 param, u8 color,
 *       then the {@link Tms282ScriptDialog} body of msgType
 * AUX:  u8 (read at 42B4B20 through the AUX stream 0939890 / 093C000 and discarded; always 0)
 * </pre>
 * The MAIN header is the 265 one (ScriptMan). The AUX byte is new: the client reads it right after {@code color},
 * from the separate AUX cursor (avatar-wire-layout-2823.md "MAIN and AUX are separate wire regions"), so every
 * LP0960 is an extended frame {@code MAIN || AUX || u32 1} ({@link OutPacket#setAuxiliaryData}). A 265 frame has no
 * AUX region: the stream read throws in the client. Strings are written in the configured charset
 * (tms282.nameCharset).
 */
public final class Tms282ScriptPackets implements ScriptMessageEncoder {
    public static final int OPCODE = 0x0960;
    /** The AUX byte: the handler reads and ignores it (42B4B0F..42B4B29). */
    public static final byte AUX_BYTE = 0;

    private final Charset charset;

    public Tms282ScriptPackets(Charset charset) {
        this.charset = Objects.requireNonNull(charset, "charset");
    }

    @Override
    public OutPacket scriptMessage(NpcScriptInfo nsi, NpcMessageType type) {
        return message(nsi, type, charset);
    }

    /**
     * @throws UnsupportedOperationException for a 265 dialog type without a traced 282 layout (quizzes, android,
     *                                       slide / select menus, story UI, mix-hair...): the script ends there
     */
    public static OutPacket message(NpcScriptInfo nsi, NpcMessageType type, Charset charset) {
        Objects.requireNonNull(nsi, "nsi");
        Objects.requireNonNull(type, "type");
        Tms282ScriptDialog dialog = Tms282ScriptDialog.of(type).orElseThrow(() -> {
            System.out.println("[TMS282] LP0960 dialog " + type + " has no 282 layout yet; the script stops here");
            return new UnsupportedOperationException("TMS 282.3 has no ported layout for NPC dialog " + type);
        });
        return message(nsi, dialog, type, charset);
    }

    /** {@code dialog}'s LP0960 with {@code type}'s prev/next/delay (Say family). */
    public static OutPacket message(NpcScriptInfo nsi, Tms282ScriptDialog dialog, NpcMessageType type, Charset charset) {
        OutPacket out = Tms282LoginPacketSupport.packet(OPCODE);
        out.encodeInt(nsi.getObjectID());
        int override = nsi.getOverrideSpeakerTemplateID();
        out.encodeByte(wireSpeakerType(nsi, override));
        out.encodeInt(override != 0 ? override : nsi.getTemplateID());
        out.encodeByte(override > 0);
        if (override > 0) {
            out.encodeInt(override);
        }
        out.encodeByte(dialog.wireType());
        out.encodeShort(nsi.getParam());
        out.encodeByte(nsi.getColor());
        dialog.encodeBody(out, nsi, type, Objects.requireNonNull(charset, "charset"));
        out.setAuxiliaryData(new byte[]{AUX_BYTE});
        return out;
    }

    /**
     * Crash guard (hotfix 2026-10-05): a dialog without a field NPC (objectId 0) and without an override speaker must
     * go out with speakerType 4. speakerType 0 there crashed the 282 client at once (newbie popup after an earlier
     * script had stopped: formal-channel-1791182392941-476 send-188 / -1791181462161-415 send-263 vs the working
     * send-144 of -1791168552687-3133, the only difference is this byte). speakerType 0 with a real objectId (normal
     * NPC talk) or with an override speaker is what the working captures send, so it is kept as is.
     */
    static byte wireSpeakerType(NpcScriptInfo nsi, int override) {
        byte speakerType = nsi.getSpeakerType();
        if (speakerType == 0 && nsi.getObjectID() == 0 && override == 0) {
            System.out.println("[TMS282] LP0960 speakerType 0 without NPC object / override (template "
                    + nsi.getTemplateID() + ") sent as 4");
            return 4;
        }
        return speakerType;
    }
}
