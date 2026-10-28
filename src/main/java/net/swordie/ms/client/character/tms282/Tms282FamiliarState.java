package net.swordie.ms.client.character.tms282;

import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.util.Map;
import java.util.Objects;

/** Immutable loaded282 authority. Does not invent first-registration levels, state, options or names. */
public record Tms282FamiliarState(int characterId, long revision, Map<Long, RegisteredInfo> registered,
                                  long summonedSerial, int essence) {
    private static final Charset CP950 = Charset.forName("MS950");

    /** Exact fourteen-field registered record. Serial is raw unsigned64 bits; familiarId is unsigned32.
     * exp and lookId are signed32. Other numeric fields are limited only by the verified wire widths.
     */
    public record RegisteredInfo(long serial, int state, long familiarId, String name, int level, int skillId,
                                 int exp, int optionLevel, int option1, int option2, int option3,
                                 int attributes, int grade, int lookId) {
        public RegisteredInfo {
            if (serial == 0 || familiarId < 0 || familiarId > 0xFFFFFFFFL)
                throw new IllegalArgumentException("Invalid registered familiar identity width");
            unsigned(state, 255); unsigned(attributes, 255); unsigned(grade, 255);
            for (int value : new int[]{level, skillId, optionLevel, option1, option2, option3}) unsigned(value, 65535);
            encodeName(name);
        }
        public byte[] nameBytes() { return encodeName(name); }
        public boolean favorite() { return (attributes & 0x04) != 0; }
        public boolean locked() { return (attributes & 0x10) != 0; }
        RegisteredInfo withAttributes(int next) {
            return new RegisteredInfo(serial,state,familiarId,name,level,skillId,exp,optionLevel,option1,option2,option3,next,grade,lookId);
        }
        RegisteredInfo withName(String next) {
            return new RegisteredInfo(serial,state,familiarId,next,level,skillId,exp,optionLevel,option1,option2,option3,attributes,grade,lookId);
        }
    }

    public Tms282FamiliarState {
        if (characterId <= 0 || revision < 0) throw new IllegalArgumentException("Invalid familiar authority owner/revision");
        registered = Map.copyOf(registered);
        for (var entry : registered.entrySet()) if (entry.getKey().longValue() != entry.getValue().serial())
            throw new IllegalArgumentException("Familiar serial map key mismatch");
        if (summonedSerial != 0 && !registered.containsKey(summonedSerial))
            throw new IllegalArgumentException("Summoned serial is not owned");
    }

    /** CP950 was verified in native string conversion; the inline wire name permits at most14 bytes. */
    public static byte[] encodeName(String name) {
        Objects.requireNonNull(name, "name");
        if (name.codePoints().anyMatch(Character::isISOControl)) throw new IllegalArgumentException("Control character in familiar name");
        try {
            var encoded = CP950.newEncoder().onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT).encode(CharBuffer.wrap(name));
            if (encoded.remaining() > 14) throw new IllegalArgumentException("Familiar name exceeds14 CP950 bytes");
            byte[] bytes = new byte[encoded.remaining()]; encoded.get(bytes); return bytes;
        } catch (CharacterCodingException failure) {
            throw new IllegalArgumentException("Familiar name is not representable in CP950", failure);
        }
    }
    private static void unsigned(int value, int max) {
        if (value < 0 || value > max) throw new IllegalArgumentException("Familiar field exceeds native unsigned width");
    }
}
