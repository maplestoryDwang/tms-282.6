package net.swordie.ms.connection.protocol.codec;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.InPacket;
import net.swordie.ms.handlers.header.InHeader;

import java.util.Optional;

/**
 * Decodes a user attack request (USER_MELEE / SHOOT / MAGIC / BODY / AREA_DOT_ATTACK) into the 265
 * {@link net.swordie.ms.client.character.skills.info.AttackInfo}, so the 265 damage, death, drop and EXP logic
 * ({@code AttackHandler.handleAttack(Char, AttackInfo)}) runs unchanged for every profile.
 * <p>
 * Codec family of {@link net.swordie.ms.connection.protocol.ProfileCodecs}: the GMS 265 profile registers none and
 * its handler keeps using {@link Gms265AttackDecoder} directly; TMS 282.3 registers {@link Tms282AttackDecoder}.
 */
public interface AttackRequestDecoder {

    /**
     * Reads the request body (opcode already consumed).
     *
     * @param chr    the attacking character (field, position, skills)
     * @param in     the request body
     * @param header the 265 attack header the request stands for
     * @return the decoded attack, or empty when the attack is dropped; the decoder has then already reported the
     * reason and answered the client the way its profile does (never by throwing)
     */
    Optional<AttackRequest> decode(Char chr, InPacket in, InHeader header);
}
