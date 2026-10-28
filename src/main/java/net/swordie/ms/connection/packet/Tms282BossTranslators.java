package net.swordie.ms.connection.packet;

import java.util.List;

/**
 * Boss-mechanics 265 -> 282 packet translators, registered once in {@link Tms282FieldEncoders} (bossmech1-0930,
 * research/tms2823/lucid-re-0930/APPLIED.md).
 * <p>
 * Extension point: every boss patch (research/tms2823/boss-mech-0930 and later) adds ONE line to {@link #PARTS} (a
 * new {@link Tms282LegacyPackets.Contributor}, one class per boss or per shared packet family) instead of editing
 * Tms282FieldEncoders again. {@link Tms282LegacyPackets.Builder#on} throws when two parts claim the same 265
 * header, so an overlap is found at profile build time (every check that builds the 282 profile fails).
 * Each part keeps its own kill switch ({@code -Dtms282.bossShared.enabled=false}, {@code -Dtms282.lucid.enabled=false}, {@code -Dtms282.bossMech.enabled=false}, {@code -Dtms282.seren.enabled=false}, {@code -Dtms282.dynamicFoothold.enabled=false}).
 */
public final class Tms282BossTranslators implements Tms282LegacyPackets.Contributor {
    public static final Tms282BossTranslators INSTANCE = new Tms282BossTranslators();

    /** In registration order. Next boss patch: append here. */
    static final List<Tms282LegacyPackets.Contributor> PARTS = List.of(
            Tms282BossSharedTranslators.INSTANCE, // LP070B next target, LP050E/050F death count, LP0332/0334 obstacle, LP06BD skill delay, LP010C weather notice, LP06BB/06BC mob speaking, LP06E1 laser, LP06CE forced action
            Tms282LucidTranslators.INSTANCE,      // Lucid LP081C..0826 (butterfly, dragon, skills, glass, statue, flying, barrage, fury)
            Tms282BossFieldTranslators.INSTANCE,  // bossmech2-0930: Will LP086C..0881, Demian LP080E..0818 + LP06EF, Verus Hilla LP0372/06FC, Pierre LP0340, MOB_FORCED_SKILL_ACTION LP06CA (research/tms2823/boss-mech-0930)
            Tms282DynamicFootholdTranslators.INSTANCE, // zakum-0930: FIELD_SYNC_DYNAMIC_FOOTHOLD -> LP0321 (Zakum altar platforms; research/tms2823/zakum-crash-0930); before Seren (Tms282SerenCheck wants Seren last)
            Tms282SerenTranslators.INSTANCE       // bossmech3-0930: Seren LP08A9 time of day, LP08AA sun gauge, LP08AB clock (research/tms2823/newboss-0930)
            // , Tms282<Boss>Translators.INSTANCE  // next boss patch
    );

    private Tms282BossTranslators() {
    }

    /** The registered parts (read-only), for checks. */
    public static List<Tms282LegacyPackets.Contributor> parts() {
        return PARTS;
    }

    @Override
    public void contribute(Tms282LegacyPackets.Builder builder) {
        for (Tms282LegacyPackets.Contributor part : PARTS) {
            builder.with(part);
        }
    }
}
