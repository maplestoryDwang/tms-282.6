package net.swordie.ms.connection.packet;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Objects;

/**
 * CP01A6 USER_QUEST_REQUEST (265 InHeader 0x19E). Senders 4220120 (six sites, 422090A in the auto-start loop),
 * 422BAD0, 578E790 (inside the LP04D0 QUEST_RESULT handler) and 5F2F7C0. Evidence:
 * packet-audit/infield-capture-1790158495223.md 2.1 and appendix A.
 * <pre>
 * act  name (265 QuestType)     after the u8 act                                            evidence
 * 1    AcceptQuest              u32 quest, u32 npc, u32 0, i16 x, i16 y, u32 0 (23 bytes)    [verified: capture, sites
 *                                                                                           422090A/4220CC9 "1 4 4 4 2 2 4"]
 * 4    OpeningScript            u32 quest, u32 npc, i16 x, i16 y (15 bytes)                  [verified: capture]
 * 7    (282, no 265 request)    u32 quest (7 bytes)                                         [verified: static sender
 *                                                                                           578FB23..578FB4D, sent after
 *                                                                                           LP04D0 act 12 success]
 * 2    CompleteQuest            u32 quest, u32 npc, i16 x, i16 y, i32 selection (-1 = none) [verified: capture
 *                                                                                           formal-channel-1790176449461-3
 *                                                                                           recv-122..126, the autoComplete
 *                                                                                           quests 504755..504759]
 * 3    ResignQuest              u32 quest (5 bytes)                                         [presumed: 265 layout]
 * 5    CompleteScript           u32 quest, u32 npc, i16 x, i16 y (as act 4)                 [presumed: 265 layout]
 * 6    LaterStep                u32 quest, u32 npc, i16 x, i16 y (as act 4)                 [verified: capture
 *                                                                                           formal-channel-1791262905973-1319
 *                                                                                           recv-5061, quest 102430 after the
 *                                                                                           LP MAKE_DESC_ON_UI "sp" step];
 *                                                                                           the 265 shape (u32 quest) is kept
 * </pre>
 * Acts 3, 5 and 6 are parsed only in exactly their known lengths; any other shape (and every other act) is kept
 * as {@link Other} with its raw body so a caller can log it without guessing a layout. Acts 1, 2, 4 and 7 are parsed
 * strictly (exact length).
 */
public record Tms282QuestRequest(int act, Payload payload) {
    public static final int OPCODE = 0x01A6;
    public static final int ACT_ACCEPT = 1;
    public static final int ACT_COMPLETE = 2;
    public static final int ACT_RESIGN = 3;
    public static final int ACT_OPENING_SCRIPT = 4;
    public static final int ACT_COMPLETE_SCRIPT = 5;
    public static final int ACT_LATER_STEP = 6;
    public static final int ACT_RESULT_ACK = 7;

    public Tms282QuestRequest {
        Objects.requireNonNull(payload, "payload");
        boolean matches = switch (act) {
            case ACT_ACCEPT -> payload instanceof Accept;
            case ACT_COMPLETE -> payload instanceof Complete;
            case ACT_OPENING_SCRIPT -> payload instanceof OpeningScript;
            case ACT_RESULT_ACK -> payload instanceof ResultAck;
            case ACT_RESIGN -> payload instanceof Resign || payload instanceof Other;
            case ACT_COMPLETE_SCRIPT -> payload instanceof CompleteScript || payload instanceof Other;
            case ACT_LATER_STEP -> payload instanceof LaterStep || payload instanceof Other;
            default -> payload instanceof Other;
        };
        if (!matches || act < 0 || act > 255) {
            throw new IllegalArgumentException("CP01A6 act " + act + " does not carry " + payload.getClass().getSimpleName());
        }
    }

    public sealed interface Payload permits QuestPayload, Other {
    }

    /** Payloads that name one quest. */
    public sealed interface QuestPayload extends Payload permits Accept, Complete, Resign, OpeningScript, CompleteScript,
            LaterStep, ResultAck {
        int questId();
    }

    /** Act 2 (also sent by the client itself for autoComplete quests). {@code selection} -1: no reward choice. */
    public record Complete(int questId, int npcId, short x, short y, int selection) implements QuestPayload {
    }

    /** Act 3 [presumed 265 layout]. */
    public record Resign(int questId) implements QuestPayload {
    }

    /** Act 5 [presumed: as act 4]. */
    public record CompleteScript(int questId, int npcId, short x, short y) implements QuestPayload {
    }

    /** Act 6: the client finished a step the quest was waiting for. The 265 shape carries the quest only. */
    public record LaterStep(int questId, int npcId, short x, short y) implements QuestPayload {
        public LaterStep(int questId) {
            this(questId, 0, (short) 0, (short) 0);
        }
    }

    /** Act 1. {@code unknown1}/{@code unknown2} were 0 in every captured sample. */
    public record Accept(int questId, int npcId, int unknown1, short x, short y, int unknown2) implements QuestPayload {
    }

    /** Act 4. npcId is 0 when the user clicks a quest-alarm entry (quest 504788 in the capture). */
    public record OpeningScript(int questId, int npcId, short x, short y) implements QuestPayload {
    }

    /** Act 7: sent by the LP04D0 handler after an act 12 success (578FB29 Encode1(7), 578FB41 Encode4(quest)). */
    public record ResultAck(int questId) implements QuestPayload {
    }

    /** An act whose layout has not been captured: the raw bytes after the act byte. */
    public record Other(byte[] body) implements Payload {
        public Other {
            body = Objects.requireNonNull(body, "body").clone();
        }

        @Override
        public byte[] body() {
            return body.clone();
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Other other && Arrays.equals(body, other.body);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(body);
        }

        @Override
        public String toString() {
            return "Other[" + body.length + " bytes]";
        }
    }

    private static Complete completeWithPosition(ByteBuffer in) {
        int questId = in.getInt();
        int npcId = in.getInt();
        in.getInt(); // 0, as in act 1
        return new Complete(questId, npcId, in.getShort(), in.getShort(), in.getInt());
    }

    private static Other other(ByteBuffer in) {
        byte[] body = new byte[in.remaining()];
        in.get(body);
        return new Other(body);
    }

    /** The quest ID of every parsed act; -1 for {@link Other}. */
    public int questId() {
        return payload instanceof QuestPayload q ? q.questId() : -1;
    }

    /** Accepts the full plaintext packet, including the opcode. */
    public static Tms282QuestRequest parse(byte[] packet) {
        Objects.requireNonNull(packet, "packet");
        ByteBuffer in = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN);
        try {
            if (Short.toUnsignedInt(in.getShort()) != OPCODE) {
                throw new IllegalArgumentException("Wrong CP01A6 opcode");
            }
            int act = Byte.toUnsignedInt(in.get());
            int rest = in.remaining();
            Payload payload = switch (act) {
                case ACT_ACCEPT -> new Accept(in.getInt(), in.getInt(), in.getInt(), in.getShort(), in.getShort(), in.getInt());
                // Two live shapes: 16 bytes (auto-complete, recv-122..126 of formal-channel-1790176449461-3: quest, npc,
                // u32 0, i32 -1) and 20 bytes (NPC completion, formal-channel-1790193171279-3: quest, npc, u32 0, i16 x,
                // i16 y, i32 -1 - the act 1 shape of 282, an extra u32 before the position).
                case ACT_COMPLETE -> rest == 20
                        ? completeWithPosition(in)
                        : new Complete(in.getInt(), in.getInt(), in.getShort(), in.getShort(), in.getInt());
                case ACT_OPENING_SCRIPT -> new OpeningScript(in.getInt(), in.getInt(), in.getShort(), in.getShort());
                case ACT_RESULT_ACK -> new ResultAck(in.getInt());
                case ACT_RESIGN -> rest == 4 ? new Resign(in.getInt()) : other(in);
                case ACT_LATER_STEP -> rest == 12 ? new LaterStep(in.getInt(), in.getInt(), in.getShort(), in.getShort())
                        : rest == 4 ? new LaterStep(in.getInt()) : other(in);
                case ACT_COMPLETE_SCRIPT -> rest == 12
                        ? new CompleteScript(in.getInt(), in.getInt(), in.getShort(), in.getShort()) : other(in);
                default -> {
                    byte[] body = new byte[in.remaining()];
                    in.get(body);
                    yield new Other(body);
                }
            };
            if (in.hasRemaining()) {
                throw new IllegalArgumentException("CP01A6 act " + act + " has " + in.remaining() + " trailing bytes");
            }
            return new Tms282QuestRequest(act, payload);
        } catch (BufferUnderflowException e) {
            throw new IllegalArgumentException("Truncated CP01A6", e);
        }
    }
}
