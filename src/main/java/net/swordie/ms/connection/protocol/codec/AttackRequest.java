package net.swordie.ms.connection.protocol.codec;

import net.swordie.ms.client.character.skills.info.AttackInfo;

import java.util.List;
import java.util.Objects;

/**
 * A decoded attack request.
 *
 * @param info             the 265 attack info the damage logic consumes
 * @param requestedSkillId the skill id as the client sent it (265 remaps some key-down ids in {@code info.skillId})
 * @param unparsedTail     bytes after the last understood field (282: the part the client builds inside its
 *                         VM, kept for the logs and for fixing the layout from live samples); empty when none
 * @param notes            how the decoder found each part (e.g. "mob#0 at 57 by sequential layout"), for logs
 */
public record AttackRequest(AttackInfo info, int requestedSkillId, byte[] unparsedTail, List<String> notes) {
    public AttackRequest {
        Objects.requireNonNull(info, "info");
        unparsedTail = unparsedTail == null ? new byte[0] : unparsedTail.clone();
        notes = notes == null ? List.of() : List.copyOf(notes);
    }

    /** A 265 request: nothing left over, no notes. */
    public static AttackRequest of(AttackInfo info, int requestedSkillId) {
        return new AttackRequest(info, requestedSkillId, new byte[0], List.of());
    }

    @Override
    public byte[] unparsedTail() {
        return unparsedTail.clone();
    }
}
