package net.swordie.ms.world.tms282.mail;

/**
 * Packet status of memo / Duey on TMS 282.3 (no packet is sent or parsed yet; the feature runs through
 * {@link MailNotifier#CHAT}, {@link MailChatCommands} and the 9010009 篤伊 NPC script).
 * <ul>
 *   <li>LP MEMO_RESULT: 265 OutHeader 176 -> 282 0x00AF, UNVERIFIED (位移推定, CWvsContext handler 0x5ED35D0).
 *       Body layout not traced. When traced: load list (u8 type, u8 count, per memo i32 id, str sender, str
 *       content, FileTime sent, u8 flag in GMS lineage) -> implement a packet {@link MailNotifier}.</li>
 *   <li>CP MEMO_REQUEST / MEMO_FLAG_REQUEST: 265 InHeader UnkValue, 282 unknown.</li>
 *   <li>LP PARCEL_DLG (Duey dialog open / list / results): 265 OutHeader UnkValue, 282 unknown.</li>
 *   <li>CP PARCEL_REQUEST (Duey operations: send / receive / remove): not present in 265 InHeader; 282 unknown.</li>
 * </ul>
 * Do not add opcode values here; classify them through the tms-opcode process first.
 */
final class MailPacketsTodo {
    private MailPacketsTodo() { }
}
