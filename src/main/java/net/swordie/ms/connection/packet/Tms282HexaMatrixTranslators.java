package net.swordie.ms.connection.packet;

import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.handlers.header.OutHeader;

import java.util.Optional;

/**
 * {@link Tms282LegacyPackets.Contributor} for the 265 HEXA matrix (6th job) packets that the unchanged 265 code builds
 * (WvsContext.hexaMessage / hexaSkillsUpdate / hexaStatsUpdate / hexaMatrixInformation / HexaMatrixActivateResult /
 * hexaSkillErdaConversion; login {@code Char.initialize}, UserHandler HEXA_MATRIX_OPREATION and HEXA_ERDA_CONVERSION).
 * Research: research/tms2823/packet-audit/hexa-vmatrix-2823.md, tracer research/tms2823/matrix-trace.
 * <p>
 * The 282 opcodes come from CWvsContext::OnPacket (context-dispatch.tsv) and the decoders they share with
 * CharacterData, not from the +12 guesses of ingame-lp-map (0x251..0x253 there are wrong):
 * <ul>
 *     <li>LP0271 HEXA_MESSAGE [證實 static]: case 5DFA189 reads Buf4 type (switch 0..8), Buf4 msgType, then
 *     2EA7DB0 reads i32 for types 0 and 2, i32 i32 for type 1 and nothing for 3..8. 265 type 3 (stat EnforceCore)
 *     adds coreId and level, which 282 does not read, so they are left out.</li>
 *     <li>LP0272 HEXA_SKILL_UPDATE [證實]: 5E6BCB0 decodes CharacterData+0x10F8 with 8F4460 - the decoder of the
 *     CharacterData HEXA skill block (92AD20 -> 931028) - then reads u8. The 265 body is read CLEAN
 *     (matrix-trace expected/hexa-skill-update.tsv).</li>
 *     <li>LP0273 HEXA_STAT_UPDATE [證實]: case 5DFA20E decodes CharacterData+0x1100 with 8F4CC0 (CharacterData
 *     HEXA stat block, 9311F8) and nothing else; the 265 body is read CLEAN (expected/hexa-stat-update.tsv).</li>
 *     <li>LP0274 HEXA_MATRIX_INFORMATION [證實 layout / 推定 new tables]: static 2B2ED50(packet). The skill cost
 *     tables are the 265 ones; the 265 stat-core list moved behind two new 282 tables ({@code i32 n + n*(i32 x3)}
 *     and {@code i32 n + n*(i32 key, i32 m, m*(i32 x3))}), whose meaning is unknown - they are sent empty. Each 265
 *     stat core is then read as {@code i32 key, 6F1230(...)}, which is byte for byte the 265 core record (id,
 *     unlock costs, upgrade/weight/cost lists). Traced CLEAN (expected/hexa-info.tsv).</li>
 *     <li>LP0275 HEXA_MATRIX_ACTIVATE_RESULT [推定]: case 5DF6168 -> 5F28B20 reads one u8 (= 265).</li>
 *     <li>LP0276 (282 only, not a 265 packet): 2B30A90 reads Buf4 result, then i32 on success.</li>
 *     <li>LP0277 HEXA_SKILL_ERDA_CONVERSION [證實 init / 推定 result]: 284F710 reads Buf4 type, Buf4, then the
 *     init list through 102D550 (CLEAN, expected/hexa-erda-init.tsv); the result branch 284FB66 reads i16, i32,
 *     i32 (= 265). Its request is CP0491 (senders 284F56D / 284F670 next to this handler).</li>
 * </ul>
 * The V matrix (5th job) packets are NOT translated: 282 replaced the 265 node-stone model (CharacterData block
 * 8F3E40: {@code map<i32, {i32, i32, u8, FILETIME}>, u8, set<i32>}; LP020D VMATRIX update), and the meaning of its
 * fields needs a live sample (hexa-vmatrix-2823.md section 3).
 */
public final class Tms282HexaMatrixTranslators implements Tms282LegacyPackets.Contributor {
    public static final int HEXA_MESSAGE = 0x0271;
    public static final int HEXA_SKILL_UPDATE = 0x0272;
    public static final int HEXA_STAT_UPDATE = 0x0273;
    public static final int HEXA_MATRIX_INFORMATION = 0x0274;
    public static final int HEXA_MATRIX_ACTIVATE_RESULT = 0x0275;
    public static final int HEXA_SKILL_ERDA_CONVERSION = 0x0277;
    /** HEXA_MESSAGE type whose 265 fields (coreId, level) the 282 client does not read. */
    static final int MESSAGE_STAT_ENFORCE_CORE = 3;

    public static final Tms282HexaMatrixTranslators INSTANCE = new Tms282HexaMatrixTranslators();

    private Tms282HexaMatrixTranslators() {
    }

    @Override
    public void contribute(Tms282LegacyPackets.Builder builder) {
        builder.on(OutHeader.HEXA_MESSAGE, Tms282HexaMatrixTranslators::message)
                .on(OutHeader.HEXA_SKILL_UPDATE, legacy -> sameBody(legacy, HEXA_SKILL_UPDATE))
                .on(OutHeader.HEXA_STAT_UPDATE, legacy -> sameBody(legacy, HEXA_STAT_UPDATE))
                .on(OutHeader.HEXA_MATRIX_INFORMATION, Tms282HexaMatrixTranslators::information)
                .on(OutHeader.HEXA_MATRIX_ACTIVATE_RESULT, legacy -> sameBody(legacy, HEXA_MATRIX_ACTIVATE_RESULT))
                .on(OutHeader.HEXA_SKILL_ERDA_CONVERSION, legacy -> sameBody(legacy, HEXA_SKILL_ERDA_CONVERSION));
    }

    /** The 265 body unchanged under the 282 opcode. */
    static Optional<OutPacket> sameBody(OutPacket legacy, int opcode) {
        OutPacket out = Tms282LoginPacketSupport.packet(opcode);
        out.encodeArr(Tms282LegacyPackets.Body.of(legacy).rest());
        return Optional.of(out);
    }

    static Optional<OutPacket> message(OutPacket legacy) {
        Tms282LegacyPackets.Body body = Tms282LegacyPackets.Body.of(legacy);
        int type = body.i32();
        int msgType = body.i32();
        OutPacket out = Tms282LoginPacketSupport.packet(HEXA_MESSAGE);
        out.encodeInt(type);                               // 5DFA1A6 Buf4 -> switch 5DFBAC0 (0..8)
        out.encodeInt(msgType);                            // 5DFA1E2 Buf4 -> 2EA7DB0 r9d
        switch (type) {
            case 0, 2 -> out.encodeInt(body.i32());        // 2EA7E2C / 2EA816B coreId
            case 1 -> {
                out.encodeInt(body.i32());                 // 2EA7FCD coreId, level
                out.encodeInt(body.i32());
            }
            case MESSAGE_STAT_ENFORCE_CORE -> {
                body.i32();                                // 265 coreId, level: 2EA8330 reads nothing
                body.i32();
            }
            case 4, 5, 6, 7, 8 -> {
            }
            default -> {
                return Optional.empty();
            }
        }
        body.end();
        return Optional.of(out);
    }

    /** 265 HEXA_MATRIX_INFORMATION -> LP0274 (layout in the class comment). */
    static Optional<OutPacket> information(OutPacket legacy) {
        Tms282LegacyPackets.Body body = Tms282LegacyPackets.Body.of(legacy);
        OutPacket out = Tms282LoginPacketSupport.packet(HEXA_MATRIX_INFORMATION);
        int activate = count(body);                        // 2B2ED94: skill-core activation costs
        out.encodeInt(activate);
        for (int i = 0; i < activate; i++) {
            ints(body, out, 3);                            // 2B2EDBE type, 2B2EDC6 erda, 2B2EDD2 fragments
        }
        int upgrade = count(body);                         // 2B2EEF4: skill-core upgrade tables
        out.encodeInt(upgrade);
        for (int i = 0; i < upgrade; i++) {
            out.encodeInt(body.i32());                     // 2B2EF5C type
            int levels = count(body);                      // 2B2EF64
            out.encodeInt(levels);
            for (int l = 0; l < levels; l++) {
                ints(body, out, 3);                        // 2B2EF83 level, 2B2EF8D erda, 2B2EF98 fragments
            }
        }
        out.encodeInt(0);                                  // 2B2F1BC new table: n * (i32, i32, i32), meaning unknown
        out.encodeInt(0);                                  // 2B2F30B new table: n * (i32, i32 m, m * (i32 x3))
        int statCores = count(body);                       // 2B2F660: stat cores
        out.encodeInt(statCores);
        for (int i = 0; i < statCores; i++) {
            out.encodeInt(body.i32());                     // 2B2F683 key = 265 core id
            ints(body, out, 3);                            // 6F1262 unlock erda, 6F126F 0, 6F127C unlock fragments
            int costs = count(body);                       // 6F12DB
            out.encodeInt(costs);
            for (int c = 0; c < costs; c++) {
                ints(body, out, 4);                        // 6F12FB..6F131B level, erda, 0, fragments
            }
            int weights = count(body);                     // 6F144B
            out.encodeInt(weights);
            for (int w = 0; w < weights; w++) {
                out.encodeInt(body.i32());                 // 6F1463 level
                out.encodeLong(body.i64());                // 6F146D double
            }
            for (int list = 0; list < 2; list++) {
                int n = count(body);                       // 6F154A / 6F164E
                out.encodeInt(n);
                for (int k = 0; k < n; k++) {
                    out.encodeInt(body.i32());             // 6F1563 / 6F1664 level
                    out.encodeLong(body.i64());            // 6F156D / 6F166E cost
                }
            }
        }
        int fragments = count(body);                       // 2B2F892
        out.encodeInt(fragments);
        for (int i = 0; i < fragments; i++) {
            out.encodeInt(body.i32());                     // 2B2F8A3
        }
        body.end();
        return Optional.of(out);
    }

    private static int count(Tms282LegacyPackets.Body body) {
        int n = body.i32();
        if (n < 0 || n > 0x10000) {
            throw new IllegalArgumentException("bad 265 count " + n);
        }
        return n;
    }

    private static void ints(Tms282LegacyPackets.Body body, OutPacket out, int n) {
        for (int i = 0; i < n; i++) {
            out.encodeInt(body.i32());
        }
    }
}
