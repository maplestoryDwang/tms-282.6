package net.swordie.ms.connection.protocol.codec;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.info.ExpIncreaseInfo;

/**
 * How a character gains EXP (and levels) when the profile's packets differ from 265: the 265
 * {@code Char.addExp(long, ExpIncreaseInfo, boolean)} asks the active profile for this family first and runs its
 * own body when there is none, so every 265 EXP source (mob kill and party share via {@code Mob.distributeExp},
 * multi-kill bonus, quests, EXP orbs, commands) takes the profile's path without changing its call site.
 * <p>
 * Codec family of {@link net.swordie.ms.connection.protocol.ProfileCodecs}; the GMS 265 profile registers none.
 */
public interface ExpGainStrategy {

    /**
     * @param amount EXP to add (265 skips {@code amount <= 0} and dead characters)
     * @param info   the EXP message to show, or null for none (265 {@code addExpNoMsg})
     */
    void addExp(Char chr, long amount, ExpIncreaseInfo info);
}
