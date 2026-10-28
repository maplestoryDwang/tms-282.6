package net.swordie.ms.connection.packet;

import net.swordie.ms.client.Account;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.MonsterCollection;
import net.swordie.ms.connection.OutPacket;

import java.nio.charset.Charset;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * CharacterData block [47] of the TMS 282.3 SetField first entry: the Monster Collection records, read by 92AD20 at
 * 930861..9308B6 when flag byte [47] is set (mask test 9307E2):
 * <pre>
 * u16 n, n x (i32 recordKey (F8D280), str value) -> 8C0870(cd, key, value) = the record map cd+0xD8B
 * </pre>
 * 8C0870 is the same store as MESSAGE type 39 (COLLECTION_RECORD, 5ED27F0 -> 5ED32D0), so the block and the
 * per-record messages fill one map; the client reads a record's properties with 8BCEF0 (key=value pairs, 448160),
 * which matches the 265 value "session=hex". Keys and values come from {@link MonsterCollection#records()}:
 * sessions under their mobCollection.img recordID, explorations under their position [value format 推定, key 證實].
 * Without this block the 282 collection window starts empty after every login.
 */
public record Tms282CollectionRecordBlock(Map<Integer, String> records) {
    public static final int FLAG = 47;
    public static final Tms282CollectionRecordBlock EMPTY = new Tms282CollectionRecordBlock(Map.of());

    public Tms282CollectionRecordBlock {
        records = Map.copyOf(new TreeMap<>(records));
    }

    public boolean isEmpty() {
        return records.isEmpty();
    }

    /** The records of {@code chr}'s (world) account; empty without an account or a collection. */
    public static Tms282CollectionRecordBlock of(Char chr) {
        Objects.requireNonNull(chr, "chr");
        Account account = chr.getAccount();
        return account == null ? EMPTY : new Tms282CollectionRecordBlock(account.getMonsterCollection().records());
    }

    public void encode(OutPacket out, Charset charset) {
        if (records.size() > 0xFFFF) {
            throw new IllegalStateException("Too many collection records: " + records.size());
        }
        out.encodeShort(records.size());                        // 930864
        for (Map.Entry<Integer, String> record : new TreeMap<>(records).entrySet()) {
            out.encodeInt(record.getKey());                     // 930873 (F8D280 -> Decode4)
            Tms282LoginPacketSupport.str(out, record.getValue(), charset); // 930884
        }
    }
}
