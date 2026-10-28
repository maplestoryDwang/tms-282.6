package net.swordie.ms.connection.packet;

import java.nio.charset.Charset;
import java.util.Objects;

/**
 * CP012C USER_SCRIPT_MESSAGE_ANSWER [verified opcode; 265 0x129] (75 senders, one per dialog kind, each right after
 * its dialog closes: 42B54E6 Say, 42B63F0 yes/no, 42B66D1 accept, 42B69D9 text, 42B8EC6 number, 42B91F3 menu,
 * 42B5D64 image, 2BEA948 avatar; answer-only: 3B349F0 in-game direction 20, 58A3D80 video 21, 4921E00 monologue 42):
 * <pre>
 * u32 (0; Say sends a dialog value the server ignores, as 265 does), u8 wireType, then the tail of
 * {@link Tms282ScriptDialog#readAnswer} for wireType
 * </pre>
 * The same byte layout as 265 for these kinds; the parser is strict (whole packet, no trailing bytes) and keyed on
 * the wire type the client names. A wire type without a ported dialog is an {@link IllegalArgumentException}.
 */
public record Tms282ScriptAnswerRequest(int lead, int wireType, Tms282ScriptDialog dialog, Tms282ScriptDialog.Answer answer) {
    public static final int OPCODE = 0x012C;
    /**
     * Answer type of the in-game direction ask ({@link Tms282ScriptDialog#IN_GAME_DIRECTION}): sm.sendDelay /
     * moveCamera / zoomCamera. Live 09-29 {@code 14 01 00 01} (enter_450003000 sendDelay(5000), not the box chat:
     * box-chat Say answers with type 0, 42B5686).
     */
    public static final int IN_GAME_DIRECTION_WIRE_TYPE = 20;

    public Tms282ScriptAnswerRequest {
        Objects.requireNonNull(dialog, "dialog");
        Objects.requireNonNull(answer, "answer");
    }

    public static Tms282ScriptAnswerRequest parse(byte[] packet, Charset charset) {
        Tms282FieldRequestReader in = Tms282FieldRequestReader.open(packet, OPCODE, "script message answer");
        int lead = in.i32();
        int wireType = in.u8();
        Tms282ScriptDialog dialog = Tms282ScriptDialog.byWireType(wireType).orElseThrow(() ->
                new IllegalArgumentException("CP012C answers dialog type " + wireType + " which has no 282 port"));
        Tms282ScriptDialog.Answer answer = dialog.readAnswer(in, Objects.requireNonNull(charset, "charset"));
        in.end();
        return new Tms282ScriptAnswerRequest(lead, wireType, dialog, answer);
    }
}
