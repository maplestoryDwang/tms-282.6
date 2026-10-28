package net.swordie.ms.connection.packet;

import net.swordie.ms.client.character.skills.Option;
import net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat;

import java.util.List;
import java.util.Map;

/**
 * Where USER_ENTER_FIELD (LP03D0 #24) finds the temporary stats of the character it shows: the 282 skill service
 * book mirrors its options into the TemporaryStatManager. The field view combines book-owned and additional
 * legacy sources, once per source. Options follow {@link TemporaryStatEncoder}'s conventions.
 */
@FunctionalInterface
public interface Tms282RemoteStatSource {
    /** The character's active stats (empty when none are known). */
    Map<CharacterTemporaryStat, List<Option>> activeStats(int characterId);

    /** Nothing is known: LP03D0 carries the empty remote block. */
    Tms282RemoteStatSource NONE = characterId -> Map.of();
}
