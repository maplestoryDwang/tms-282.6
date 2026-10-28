package net.swordie.ms.connection.protocol.codec;

import net.swordie.ms.client.character.skills.info.AttackInfo;
import net.swordie.ms.connection.InPacket;

/**
 * The part of a TMS 282.3 attack request after the last mob block (strategy). The client builds it inside its
 * Themida VM (every send site jumps there after the mob loop, e.g. 57CDBA3 -> B80B378), so no field is known
 * statically. {@link #RAW} keeps the bytes unparsed; the 265 fields that live here (positions of some skills,
 * dragon, shoot object...) stay at their defaults. When live samples show the layout, add a parser here and select
 * it in {@link Tms282AttackLayout}.
 */
@FunctionalInterface
public interface Tms282AttackTail {

    /**
     * Reads what it understands from {@code in} (positioned after the last mob block) into {@code ai}.
     *
     * @return the bytes it left unparsed
     */
    byte[] read(InPacket in, Tms282AttackKind kind, AttackInfo ai);

    /** Keeps every remaining byte. */
    Tms282AttackTail RAW = (in, kind, ai) -> in.getUnreadAmount() > 0 ? in.decodeArr(in.getUnreadAmount()) : new byte[0];
}
