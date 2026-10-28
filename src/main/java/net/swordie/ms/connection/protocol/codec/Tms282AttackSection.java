package net.swordie.ms.connection.protocol.codec;

import net.swordie.ms.client.character.skills.info.AttackInfo;
import net.swordie.ms.client.character.skills.info.MobAttackInfo;
import net.swordie.ms.connection.InPacket;

/**
 * One stretch of a TMS 282.3 attack request (strategy). {@link Tms282AttackLayout} chains them; a stretch whose
 * layout live samples prove wrong is fixed by replacing its strategy only. Implementations read from {@code in}
 * and fill {@code ai}; running out of bytes throws (the decoder turns that into a logged, dropped attack).
 */
@FunctionalInterface
public interface Tms282AttackSection {

    void read(InPacket in, Tms282AttackKind kind, AttackInfo ai);

    /** The stretch after one mob's two crc ints and before the next mob (282 sub_5956260 and skill extras). */
    @FunctionalInterface
    interface PerMob {
        void read(InPacket in, Tms282AttackKind kind, AttackInfo ai, MobAttackInfo mai);
    }
}
