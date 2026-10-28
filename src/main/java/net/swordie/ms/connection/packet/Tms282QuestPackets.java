package net.swordie.ms.connection.packet;

import net.swordie.ms.connection.OutPacket;

import java.nio.charset.Charset;
import java.util.Objects;

import static net.swordie.ms.connection.packet.Tms282LoginPacketSupport.checkByte;
import static net.swordie.ms.connection.packet.Tms282LoginPacketSupport.str;

/**
 * TMS 282.3 quest replies to CP01A6 ({@link Tms282QuestRequest}). Without one, the client re-evaluates its auto-start
 * quests every 10 s and sends the same 24 CP01A6 again (packet-audit/infield-capture-1790158495223.md 2.1).
 * <p>
 * <b>LP00AE MESSAGE, type 1 QuestRecord</b> (CWvsContext handler 5ED27F0: u8 type at 5ED281A, type 1 -> 5EEF5D0)
 * [verified: setfield_trace.py --entry 5ED27F0 --this-is-ctx, expected/infield-quest-record-*.tsv]:
 * <pre>
 * u8 1, i32 questId (5EEF627), u8 state (5EEF632), then by state:
 *   0 NotStarted  u8 0      (5EEF768)
 *   1 Started     Str value (5EEF6D6)
 *   2 Completed   Buf8 FILETIME (5EEF75A)
 * </pre>
 * Identical to 265 WvsContext.questRecordMessage.
 * <p>
 * <b>LP04D0 QUEST_RESULT</b> (CUserLocal handler 578E790: u8 act at 578E824, jump table op - 8 at 5790BF4)
 * [verified: setfield_trace.py --entry 578E790, expected/infield-quest-result.tsv]: act <b>12</b> is the success
 * case (578E84D): {@code i32 questId (578E85F), i32 npcId (578E86D), i32 nextQuestId (578FBFC), u8 navigate (578FC1C)}.
 * 282 numbers the results one higher than 265 (265 QuestRes_Act_Success = 11; 282 act 11 is a u16-count timer list
 * and misreads the 265 packet). The success handler also sends CP01A6 act 7 with the quest ID (578FB23..578FB4D).
 */
public final class Tms282QuestPackets {
    public static final int MESSAGE = 0x00AE;
    public static final int QUEST_RESULT = 0x04D0;
    public static final int MESSAGE_QUEST_RECORD = 1;
    public static final int STATE_NOT_STARTED = 0;
    public static final int STATE_STARTED = 1;
    public static final int STATE_COMPLETED = 2;
    /** LP04D0 success act in 282 (265 QuestType.QuestRes_Act_Success is 11). */
    public static final int RESULT_ACT_SUCCESS = 12;
    /** Windows FILETIME of the Unix epoch (100 ns ticks since 1601-01-01). */
    public static final long FILETIME_UNIX_EPOCH = 116444736000000000L;

    private Tms282QuestPackets() {
    }

    /** LP00AE type 1, state 1: the quest is started, with its QR value (empty for a plain quest). */
    public static OutPacket questRecordStarted(int questId, String value, Charset charset) {
        Objects.requireNonNull(value, "value");
        Objects.requireNonNull(charset, "charset");
        OutPacket out = questRecordHeader(questId, STATE_STARTED);
        str(out, value, charset);
        return out;
    }

    /** LP00AE type 1, state 0: the quest record is removed (forfeit). */
    public static OutPacket questRecordNotStarted(int questId) {
        OutPacket out = questRecordHeader(questId, STATE_NOT_STARTED);
        out.encodeByte(0);
        return out;
    }

    /** LP00AE type 1, state 2: completed at {@code completedFileTime} (Windows FILETIME, see {@link #fileTimeOf}). */
    public static OutPacket questRecordCompleted(int questId, long completedFileTime) {
        OutPacket out = questRecordHeader(questId, STATE_COMPLETED);
        out.encodeLong(completedFileTime);
        return out;
    }

    /** LP04D0 act 12 success. {@code nextQuestId} 0 = none; {@code navigate} starts the quest navigation. */
    public static OutPacket questResultSuccess(int questId, int npcId, int nextQuestId, boolean navigate) {
        OutPacket out = Tms282LoginPacketSupport.packet(QUEST_RESULT);
        out.encodeByte(RESULT_ACT_SUCCESS);
        out.encodeInt((long) questId);
        out.encodeInt((long) npcId);
        out.encodeInt((long) nextQuestId);
        out.encodeByte(navigate ? 1 : 0);
        return out;
    }

    /** Epoch milliseconds to the Windows FILETIME the client reads. */
    public static long fileTimeOf(long epochMillis) {
        return Math.addExact(Math.multiplyExact(epochMillis, 10_000L), FILETIME_UNIX_EPOCH);
    }

    private static OutPacket questRecordHeader(int questId, int state) {
        checkByte(state, "Quest state");
        OutPacket out = Tms282LoginPacketSupport.packet(MESSAGE);
        out.encodeByte(MESSAGE_QUEST_RECORD);
        // encodeInt(long): OutPacket.encodeInt(int) silently drops the value -88888.
        out.encodeInt((long) questId);
        out.encodeByte(state);
        return out;
    }
}
