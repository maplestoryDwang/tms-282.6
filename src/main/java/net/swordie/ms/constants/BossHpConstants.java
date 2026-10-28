package net.swordie.ms.constants;

import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.console.config.BossRuntimeConfig;
import net.swordie.ms.enums.BossPartyType;

import java.util.EnumMap;
import java.util.Map;

/**
 * 首領標準血量（80 難度平滑曲線）。
 * 生成工具：scripts/tools/generate_boss_hp_table.py
 */
public final class BossHpConstants {

    private BossHpConstants() {
    }

    private static final Int2LongOpenHashMap MOB_HP = new Int2LongOpenHashMap();
    private static final Map<BossPartyType, Int2LongOpenHashMap> BOSS_CONTEXT_HP = new EnumMap<>(BossPartyType.class);

    static {
        MOB_HP.put(6160003, 4212491667L);
        MOB_HP.put(8500002, 160565365L);
        MOB_HP.put(8500012, 356811923L);
        MOB_HP.put(8500022, 1141798154L);
        MOB_HP.put(8644650, 957809713L);
        MOB_HP.put(8644655, 3064991082L);
        MOB_HP.put(8645009, 1225996433L);
        MOB_HP.put(8645066, 2451992866L);
        MOB_HP.put(8800000, 2000000L);
        MOB_HP.put(8800003, 640000L);
        MOB_HP.put(8800004, 640000L);
        MOB_HP.put(8800005, 640000L);
        MOB_HP.put(8800006, 640000L);
        MOB_HP.put(8800007, 640000L);
        MOB_HP.put(8800008, 640000L);
        MOB_HP.put(8800009, 640000L);
        MOB_HP.put(8800010, 640000L);
        MOB_HP.put(8800020, 900000L);
        MOB_HP.put(8800023, 640000L);
        MOB_HP.put(8800024, 640000L);
        MOB_HP.put(8800025, 640000L);
        MOB_HP.put(8800026, 640000L);
        MOB_HP.put(8800027, 640000L);
        MOB_HP.put(8800028, 640000L);
        MOB_HP.put(8800029, 640000L);
        MOB_HP.put(8800030, 640000L);
        MOB_HP.put(8800100, 6400000L);
        MOB_HP.put(8800103, 640000L);
        MOB_HP.put(8800104, 640000L);
        MOB_HP.put(8800105, 640000L);
        MOB_HP.put(8800106, 640000L);
        MOB_HP.put(8800107, 640000L);
        MOB_HP.put(8800108, 640000L);
        MOB_HP.put(8800109, 640000L);
        MOB_HP.put(8800110, 640000L);
        MOB_HP.put(8810018, 2560000L);
        MOB_HP.put(8810122, 8192000L);
        MOB_HP.put(8810214, 1152000L);
        MOB_HP.put(8820014, 23611832L);
        MOB_HP.put(8820101, 75557862L);
        MOB_HP.put(8830000, 1L);
        MOB_HP.put(8830001, 1L);
        MOB_HP.put(8830002, 1L);
        MOB_HP.put(8830003, 1000000L);
        MOB_HP.put(8830007, 1L);
        MOB_HP.put(8830008, 1L);
        MOB_HP.put(8830009, 1L);
        MOB_HP.put(8830010, 250000L);
        MOB_HP.put(8840000, 5066550L);
        MOB_HP.put(8840007, 11258999L);
        MOB_HP.put(8840014, 22517998L);
        MOB_HP.put(8850011, 30223145L);
        MOB_HP.put(8850111, 13600415L);
        MOB_HP.put(8860000, 14411519L);
        MOB_HP.put(8870000, 3276800L);
        MOB_HP.put(8870100, 6553600L);
        MOB_HP.put(8880000, 36893488L);
        MOB_HP.put(8880002, 18446744L);
        MOB_HP.put(8880010, 8301035L);
        MOB_HP.put(8880100, 126765060L);
        MOB_HP.put(8880110, 63382530L);
        MOB_HP.put(8880151, 170141183L);
        MOB_HP.put(8880153, 340282366L);
        MOB_HP.put(8880155, 76563532L);
        MOB_HP.put(8880200, 278759315L);
        MOB_HP.put(8880302, 913438524L);
        MOB_HP.put(8880342, 456719262L);
        MOB_HP.put(8880410, 1169201310L);
        MOB_HP.put(8880500, 617338291L);
        MOB_HP.put(8880501, 617338291L);
        MOB_HP.put(8880502, 1028897152L);
        MOB_HP.put(8880503, 1028897152L);
        MOB_HP.put(8880504, 823117722L);
        MOB_HP.put(8880549, 4115588609L);
        MOB_HP.put(8880602, 3138550868L);
        MOB_HP.put(8880632, 1569275434L);
        MOB_HP.put(8880652, 8631014887L);
        MOB_HP.put(8880700, 4017345110L);
        MOB_HP.put(8880711, 2008672555L);
        MOB_HP.put(8880802, 2571100871L);
        MOB_HP.put(8880842, 3291009115L);
        MOB_HP.put(8880912, 1480954102L);
        MOB_HP.put(8880942, 6582018230L);
        MOB_HP.put(8880972, 18100550132L);
        MOB_HP.put(8881000, 49517602L);
        MOB_HP.put(8881012, 1156995392L);
        MOB_HP.put(8881032, 5142201742L);
        MOB_HP.put(8881052, 14141054790L);
        MOB_HP.put(8881202, 212770943L);
        MOB_HP.put(8881332, 5391989333L);
        MOB_HP.put(8881382, 10783978666L);
        MOB_HP.put(8881601, 13803492694L);
        MOB_HP.put(8881710, 48588294282L);
        MOB_HP.put(8900000, 13421773L);
        MOB_HP.put(8900100, 4194304L);
        MOB_HP.put(8910000, 17179869L);
        MOB_HP.put(8910001, 5000000L);
        MOB_HP.put(8910100, 5368709L);
        MOB_HP.put(8920000, 21990234L);
        MOB_HP.put(8920100, 6871948L);
        MOB_HP.put(8930000, 28147498L);
        MOB_HP.put(8930100, 8796093L);
        MOB_HP.put(8950002, 77371252L);
        MOB_HP.put(8950102, 38685626L);
        MOB_HP.put(9390602, 81129638L);
        MOB_HP.put(9421581, 103845937L);
        MOB_HP.put(9421583, 207691874L);
        MOB_HP.put(9450022, 132922800L);
        MOB_HP.put(9601623, 120000000L);

        {
            Int2LongOpenHashMap map = new Int2LongOpenHashMap();
            map.put(9601623, 120000000L);
            BOSS_CONTEXT_HP.put(BossPartyType.AKECHI_MITSUHIDE, map);
        }
        {
            Int2LongOpenHashMap map = new Int2LongOpenHashMap();
            map.put(8860000, 6485184L);
            BOSS_CONTEXT_HP.put(BossPartyType.ARKARIUM_EASY, map);
        }
        {
            Int2LongOpenHashMap map = new Int2LongOpenHashMap();
            map.put(8860000, 14411519L);
            BOSS_CONTEXT_HP.put(BossPartyType.ARKARIUM_NORMAL, map);
        }
        {
            Int2LongOpenHashMap map = new Int2LongOpenHashMap();
            map.put(8881601, 13803492694L);
            BOSS_CONTEXT_HP.put(BossPartyType.BALDRIX_HARD, map);
        }
        {
            Int2LongOpenHashMap map = new Int2LongOpenHashMap();
            map.put(8881601, 6901746347L);
            BOSS_CONTEXT_HP.put(BossPartyType.BALDRIX_NORMAL, map);
        }
        {
            Int2LongOpenHashMap map = new Int2LongOpenHashMap();
            map.put(8830010, 250000L);
            BOSS_CONTEXT_HP.put(BossPartyType.BALROG, map);
        }
        {
            Int2LongOpenHashMap map = new Int2LongOpenHashMap();
            map.put(8880500, 617338291L);
            map.put(8880501, 617338291L);
            map.put(8880502, 1028897152L);
            map.put(8880503, 1028897152L);
            map.put(8880504, 823117722L);
            map.put(8880549, 4115588609L);
            BOSS_CONTEXT_HP.put(BossPartyType.BLACK_MAGE_EXTREME, map);
        }
        {
            Int2LongOpenHashMap map = new Int2LongOpenHashMap();
            map.put(8880500, 224486651L);
            map.put(8880501, 224486651L);
            map.put(8880502, 374144419L);
            map.put(8880503, 374144419L);
            map.put(8880504, 299315535L);
            map.put(8880549, 1795893211L);
            BOSS_CONTEXT_HP.put(BossPartyType.BLACK_MAGE_HARD, map);
        }
        {
            Int2LongOpenHashMap map = new Int2LongOpenHashMap();
            map.put(8880652, 8631014887L);
            BOSS_CONTEXT_HP.put(BossPartyType.CHOSEN_SEREN_EXTREME, map);
        }
        {
            Int2LongOpenHashMap map = new Int2LongOpenHashMap();
            map.put(8880602, 3138550868L);
            BOSS_CONTEXT_HP.put(BossPartyType.CHOSEN_SEREN_HARD, map);
        }
        {
            Int2LongOpenHashMap map = new Int2LongOpenHashMap();
            map.put(8880632, 1569275434L);
            BOSS_CONTEXT_HP.put(BossPartyType.CHOSEN_SEREN_NORMAL, map);
        }
        {
            Int2LongOpenHashMap map = new Int2LongOpenHashMap();
            map.put(8850111, 13600415L);
            BOSS_CONTEXT_HP.put(BossPartyType.CYGNUS_EASY, map);
        }
        {
            Int2LongOpenHashMap map = new Int2LongOpenHashMap();
            map.put(8850011, 30223145L);
            BOSS_CONTEXT_HP.put(BossPartyType.CYGNUS_NORMAL, map);
        }
        {
            Int2LongOpenHashMap map = new Int2LongOpenHashMap();
            map.put(8880100, 126765060L);
            BOSS_CONTEXT_HP.put(BossPartyType.DAMIEN_HARD, map);
        }
        {
            Int2LongOpenHashMap map = new Int2LongOpenHashMap();
            map.put(8880110, 63382530L);
            BOSS_CONTEXT_HP.put(BossPartyType.DAMIEN_NORMAL, map);
        }
        {
            Int2LongOpenHashMap map = new Int2LongOpenHashMap();
            map.put(8645066, 2451992866L);
            BOSS_CONTEXT_HP.put(BossPartyType.DARKNELL_HARD, map);
        }
        {
            Int2LongOpenHashMap map = new Int2LongOpenHashMap();
            map.put(8645009, 1225996433L);
            BOSS_CONTEXT_HP.put(BossPartyType.DARKNELL_NORMAL, map);
        }
        {
            Int2LongOpenHashMap map = new Int2LongOpenHashMap();
            map.put(8881710, 3975405896L);
            BOSS_CONTEXT_HP.put(BossPartyType.FIRST_ADVERSARY_EASY, map);
        }
        {
            Int2LongOpenHashMap map = new Int2LongOpenHashMap();
            map.put(8881710, 48588294282L);
            BOSS_CONTEXT_HP.put(BossPartyType.FIRST_ADVERSARY_EXTREME, map);
        }
        {
            Int2LongOpenHashMap map = new Int2LongOpenHashMap();
            map.put(8881710, 17668470648L);
            BOSS_CONTEXT_HP.put(BossPartyType.FIRST_ADVERSARY_HARD, map);
        }
        {
            Int2LongOpenHashMap map = new Int2LongOpenHashMap();
            map.put(8881710, 8834235324L);
            BOSS_CONTEXT_HP.put(BossPartyType.FIRST_ADVERSARY_NORMAL, map);
        }
        {
            Int2LongOpenHashMap map = new Int2LongOpenHashMap();
            map.put(8644655, 3064991082L);
            BOSS_CONTEXT_HP.put(BossPartyType.GLOOM_CHAOS, map);
        }
        {
            Int2LongOpenHashMap map = new Int2LongOpenHashMap();
            map.put(8644650, 957809713L);
            BOSS_CONTEXT_HP.put(BossPartyType.GLOOM_NORMAL, map);
        }
        {
            Int2LongOpenHashMap map = new Int2LongOpenHashMap();
            map.put(9390602, 81129638L);
            BOSS_CONTEXT_HP.put(BossPartyType.GOLLUX, map);
        }
        {
            Int2LongOpenHashMap map = new Int2LongOpenHashMap();
            map.put(8870100, 6553600L);
            BOSS_CONTEXT_HP.put(BossPartyType.HILLA_HARD, map);
        }
        {
            Int2LongOpenHashMap map = new Int2LongOpenHashMap();
            map.put(8870000, 3276800L);
            BOSS_CONTEXT_HP.put(BossPartyType.HILLA_NORMAL, map);
        }
        {
            Int2LongOpenHashMap map = new Int2LongOpenHashMap();
            map.put(8810122, 8192000L);
            BOSS_CONTEXT_HP.put(BossPartyType.HORNTAIL_CHAOS, map);
        }
        {
            Int2LongOpenHashMap map = new Int2LongOpenHashMap();
            map.put(8810214, 1152000L);
            BOSS_CONTEXT_HP.put(BossPartyType.HORNTAIL_EASY, map);
        }
        {
            Int2LongOpenHashMap map = new Int2LongOpenHashMap();
            map.put(8810018, 2560000L);
            BOSS_CONTEXT_HP.put(BossPartyType.HORNTAIL_NORMAL, map);
        }
        {
            Int2LongOpenHashMap map = new Int2LongOpenHashMap();
            map.put(8880912, 1480954102L);
            BOSS_CONTEXT_HP.put(BossPartyType.KALING_EASY, map);
        }
        {
            Int2LongOpenHashMap map = new Int2LongOpenHashMap();
            map.put(8880972, 18100550132L);
            BOSS_CONTEXT_HP.put(BossPartyType.KALING_EXTREME, map);
        }
        {
            Int2LongOpenHashMap map = new Int2LongOpenHashMap();
            map.put(8880942, 6582018230L);
            BOSS_CONTEXT_HP.put(BossPartyType.KALING_HARD, map);
        }
        {
            Int2LongOpenHashMap map = new Int2LongOpenHashMap();
            map.put(8880842, 3291009115L);
            BOSS_CONTEXT_HP.put(BossPartyType.KALING_NORMAL, map);
        }
        {
            Int2LongOpenHashMap map = new Int2LongOpenHashMap();
            map.put(8881012, 1156995392L);
            BOSS_CONTEXT_HP.put(BossPartyType.KALOS_EASY, map);
        }
        {
            Int2LongOpenHashMap map = new Int2LongOpenHashMap();
            map.put(8881052, 14141054790L);
            BOSS_CONTEXT_HP.put(BossPartyType.KALOS_EXTREME, map);
        }
        {
            Int2LongOpenHashMap map = new Int2LongOpenHashMap();
            map.put(8881032, 5142201742L);
            BOSS_CONTEXT_HP.put(BossPartyType.KALOS_HARD, map);
        }
        {
            Int2LongOpenHashMap map = new Int2LongOpenHashMap();
            map.put(8880802, 2571100871L);
            BOSS_CONTEXT_HP.put(BossPartyType.KALOS_NORMAL, map);
        }
        {
            Int2LongOpenHashMap map = new Int2LongOpenHashMap();
            map.put(8881382, 10783978666L);
            BOSS_CONTEXT_HP.put(BossPartyType.LIMBO_HARD, map);
        }
        {
            Int2LongOpenHashMap map = new Int2LongOpenHashMap();
            map.put(8881332, 5391989333L);
            BOSS_CONTEXT_HP.put(BossPartyType.LIMBO_NORMAL, map);
        }
        {
            Int2LongOpenHashMap map = new Int2LongOpenHashMap();
            map.put(8881202, 212770943L);
            BOSS_CONTEXT_HP.put(BossPartyType.LOTUS_EXTREME, map);
        }
        {
            Int2LongOpenHashMap map = new Int2LongOpenHashMap();
            map.put(8950002, 77371252L);
            BOSS_CONTEXT_HP.put(BossPartyType.LOTUS_HARD, map);
        }
        {
            Int2LongOpenHashMap map = new Int2LongOpenHashMap();
            map.put(8950102, 38685626L);
            BOSS_CONTEXT_HP.put(BossPartyType.LOTUS_NORMAL, map);
        }
        {
            Int2LongOpenHashMap map = new Int2LongOpenHashMap();
            map.put(8880155, 76563532L);
            BOSS_CONTEXT_HP.put(BossPartyType.LUCID_EASY, map);
        }
        {
            Int2LongOpenHashMap map = new Int2LongOpenHashMap();
            map.put(8880153, 340282366L);
            BOSS_CONTEXT_HP.put(BossPartyType.LUCID_HARD, map);
        }
        {
            Int2LongOpenHashMap map = new Int2LongOpenHashMap();
            map.put(8880151, 170141183L);
            BOSS_CONTEXT_HP.put(BossPartyType.LUCID_NORMAL, map);
        }
        {
            Int2LongOpenHashMap map = new Int2LongOpenHashMap();
            map.put(8880010, 8301035L);
            BOSS_CONTEXT_HP.put(BossPartyType.MAGNUS_EASY, map);
        }
        {
            Int2LongOpenHashMap map = new Int2LongOpenHashMap();
            map.put(8880000, 36893488L);
            BOSS_CONTEXT_HP.put(BossPartyType.MAGNUS_HARD, map);
        }
        {
            Int2LongOpenHashMap map = new Int2LongOpenHashMap();
            map.put(8880002, 18446744L);
            BOSS_CONTEXT_HP.put(BossPartyType.MAGNUS_NORMAL, map);
        }
        {
            Int2LongOpenHashMap map = new Int2LongOpenHashMap();
            map.put(6160003, 4212491667L);
            BOSS_CONTEXT_HP.put(BossPartyType.MONSTER_PARK_EXTREME, map);
        }
        {
            Int2LongOpenHashMap map = new Int2LongOpenHashMap();
            map.put(8880200, 278759315L);
            BOSS_CONTEXT_HP.put(BossPartyType.OMNI_CLN_NORMAL, map);
        }
        {
            Int2LongOpenHashMap map = new Int2LongOpenHashMap();
            map.put(8500022, 1141798154L);
            BOSS_CONTEXT_HP.put(BossPartyType.PAPULATUS_CHAOS, map);
        }
        {
            Int2LongOpenHashMap map = new Int2LongOpenHashMap();
            map.put(8500002, 160565365L);
            BOSS_CONTEXT_HP.put(BossPartyType.PAPULATUS_EASY, map);
        }
        {
            Int2LongOpenHashMap map = new Int2LongOpenHashMap();
            map.put(8500012, 356811923L);
            BOSS_CONTEXT_HP.put(BossPartyType.PAPULATUS_NORMAL, map);
        }
        {
            Int2LongOpenHashMap map = new Int2LongOpenHashMap();
            map.put(8900000, 13421773L);
            BOSS_CONTEXT_HP.put(BossPartyType.PIERRE_CHAOS, map);
        }
        {
            Int2LongOpenHashMap map = new Int2LongOpenHashMap();
            map.put(8900100, 4194304L);
            BOSS_CONTEXT_HP.put(BossPartyType.PIERRE_NORMAL, map);
        }
        {
            Int2LongOpenHashMap map = new Int2LongOpenHashMap();
            map.put(8820101, 75557862L);
            BOSS_CONTEXT_HP.put(BossPartyType.PINK_BEAN_CHAOS, map);
        }
        {
            Int2LongOpenHashMap map = new Int2LongOpenHashMap();
            map.put(8820014, 23611832L);
            BOSS_CONTEXT_HP.put(BossPartyType.PINK_BEAN_NORMAL, map);
        }
        {
            Int2LongOpenHashMap map = new Int2LongOpenHashMap();
            map.put(9450022, 132922800L);
            BOSS_CONTEXT_HP.put(BossPartyType.PRINCESS_NO, map);
        }
        {
            Int2LongOpenHashMap map = new Int2LongOpenHashMap();
            map.put(8920000, 21990234L);
            BOSS_CONTEXT_HP.put(BossPartyType.QUEEN_CHAOS, map);
        }
        {
            Int2LongOpenHashMap map = new Int2LongOpenHashMap();
            map.put(8920100, 6871948L);
            BOSS_CONTEXT_HP.put(BossPartyType.QUEEN_NORMAL, map);
        }
        {
            Int2LongOpenHashMap map = new Int2LongOpenHashMap();
            map.put(9421583, 207691874L);
            BOSS_CONTEXT_HP.put(BossPartyType.RANMARU_HARD, map);
        }
        {
            Int2LongOpenHashMap map = new Int2LongOpenHashMap();
            map.put(9421581, 103845937L);
            BOSS_CONTEXT_HP.put(BossPartyType.RANMARU_NORMAL, map);
        }
        {
            Int2LongOpenHashMap map = new Int2LongOpenHashMap();
            map.put(8880700, 4017345110L);
            BOSS_CONTEXT_HP.put(BossPartyType.SLIME_HARD, map);
        }
        {
            Int2LongOpenHashMap map = new Int2LongOpenHashMap();
            map.put(8880711, 2008672555L);
            BOSS_CONTEXT_HP.put(BossPartyType.SLIME_NORMAL, map);
        }
        {
            Int2LongOpenHashMap map = new Int2LongOpenHashMap();
            map.put(8881000, 49517602L);
            BOSS_CONTEXT_HP.put(BossPartyType.URSUS, map);
        }
        {
            Int2LongOpenHashMap map = new Int2LongOpenHashMap();
            map.put(8930000, 28147498L);
            BOSS_CONTEXT_HP.put(BossPartyType.VELLUM_CHAOS, map);
        }
        {
            Int2LongOpenHashMap map = new Int2LongOpenHashMap();
            map.put(8930100, 8796093L);
            BOSS_CONTEXT_HP.put(BossPartyType.VELLUM_NORMAL, map);
        }
        {
            Int2LongOpenHashMap map = new Int2LongOpenHashMap();
            map.put(8880410, 1169201310L);
            BOSS_CONTEXT_HP.put(BossPartyType.VERUS_HILLA_HARD, map);
        }
        {
            Int2LongOpenHashMap map = new Int2LongOpenHashMap();
            map.put(8880410, 584600655L);
            BOSS_CONTEXT_HP.put(BossPartyType.VERUS_HILLA_NORMAL, map);
        }
        {
            Int2LongOpenHashMap map = new Int2LongOpenHashMap();
            map.put(8910000, 17179869L);
            BOSS_CONTEXT_HP.put(BossPartyType.VON_BON_CHAOS, map);
        }
        {
            Int2LongOpenHashMap map = new Int2LongOpenHashMap();
            map.put(8910100, 5368709L);
            BOSS_CONTEXT_HP.put(BossPartyType.VON_BON_NORMAL, map);
        }
        {
            Int2LongOpenHashMap map = new Int2LongOpenHashMap();
            map.put(8840000, 5066550L);
            BOSS_CONTEXT_HP.put(BossPartyType.VON_LEON_EASY, map);
        }
        {
            Int2LongOpenHashMap map = new Int2LongOpenHashMap();
            map.put(8840014, 22517998L);
            BOSS_CONTEXT_HP.put(BossPartyType.VON_LEON_HARD, map);
        }
        {
            Int2LongOpenHashMap map = new Int2LongOpenHashMap();
            map.put(8840007, 11258999L);
            BOSS_CONTEXT_HP.put(BossPartyType.VON_LEON_NORMAL, map);
        }
        {
            Int2LongOpenHashMap map = new Int2LongOpenHashMap();
            map.put(8880342, 205523668L);
            BOSS_CONTEXT_HP.put(BossPartyType.WILL_EASY, map);
        }
        {
            Int2LongOpenHashMap map = new Int2LongOpenHashMap();
            map.put(8880302, 913438524L);
            BOSS_CONTEXT_HP.put(BossPartyType.WILL_HARD, map);
        }
        {
            Int2LongOpenHashMap map = new Int2LongOpenHashMap();
            map.put(8880342, 456719262L);
            BOSS_CONTEXT_HP.put(BossPartyType.WILL_NORMAL, map);
        }
        {
            Int2LongOpenHashMap map = new Int2LongOpenHashMap();
            map.put(8800100, 6400000L);
            BOSS_CONTEXT_HP.put(BossPartyType.ZAKUM_CHAOS, map);
        }
        {
            Int2LongOpenHashMap map = new Int2LongOpenHashMap();
            map.put(8800020, 900000L);
            BOSS_CONTEXT_HP.put(BossPartyType.ZAKUM_EASY, map);
        }
        {
            Int2LongOpenHashMap map = new Int2LongOpenHashMap();
            map.put(8800000, 2000000L);
            BOSS_CONTEXT_HP.put(BossPartyType.ZAKUM_NORMAL, map);
        }
    }

    public static long getMaxHp(int mobTemplateId) {
        return MOB_HP.getOrDefault(mobTemplateId, 0L);
    }

    public static Int2LongOpenHashMap getDefaultContextHpMap(BossPartyType partyType) {
        if (partyType == null) {
            return null;
        }
        return BOSS_CONTEXT_HP.get(partyType);
    }

    public static long getDefaultContextHp(BossPartyType partyType, int mobTemplateId) {
        Int2LongOpenHashMap ctx = getDefaultContextHpMap(partyType);
        if (ctx != null && ctx.containsKey(mobTemplateId)) {
            return ctx.get(mobTemplateId);
        }
        return getMaxHp(mobTemplateId);
    }

    public static BossPartyType getCurrentBossParty(Char chr) {
        if (chr == null) {
            return null;
        }
        String order = chr.getQRValueByKey(102401, "order");
        String diff = chr.getQRValueByKey(102401, "diff");
        if (order == null || diff == null) {
            return null;
        }
        try {
            return BossPartyType.getByOrderIdAndDifficulty(
                    Integer.parseInt(order), Integer.parseInt(diff));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static long resolveMaxHp(int mobTemplateId, Char chr) {
        BossPartyType partyType = getCurrentBossParty(chr);
        if (partyType != null) {
            long runtimeHp = BossRuntimeConfig.getContextHp(partyType, mobTemplateId);
            if (runtimeHp > 0) {
                return runtimeHp;
            }
            Int2LongOpenHashMap ctx = BOSS_CONTEXT_HP.get(partyType);
            if (ctx != null && ctx.containsKey(mobTemplateId)) {
                return ctx.get(mobTemplateId);
            }
        }
        return getMaxHp(mobTemplateId);
    }

    public static boolean isBalrogPart(int mobTemplateId) {
        return switch (mobTemplateId) {
            case BossConstants.BALROG_EASY_BODY, BossConstants.BALROG_EASY_LARM, BossConstants.BALROG_EASY_RARM,
                    BossConstants.BALROG_HARD_BODY, BossConstants.BALROG_HARD_LARM, BossConstants.BALROG_HARD_RARM ->
                    true;
            default -> false;
        };
    }

    public static int getBalrogDamageSinkId(int mobTemplateId) {
        return switch (mobTemplateId) {
            case BossConstants.BALROG_EASY_BODY, BossConstants.BALROG_EASY_LARM, BossConstants.BALROG_EASY_RARM ->
                    BossConstants.BALROG_EASY_DMGSINK;
            case BossConstants.BALROG_HARD_BODY, BossConstants.BALROG_HARD_LARM, BossConstants.BALROG_HARD_RARM ->
                    BossConstants.BALROG_HARD_DMGSINK;
            // bossmech3-0930 (newboss-0930): Seren phase-2 time-of-day bodies (Mob.wz HpLinkMob) -> the HP core
            // (hard 8880602 / normal 8880632 / extreme 8880652). Only forwarded while the core is in the same field.
            case 8880603, 8880607, 8880609, 8880612 -> 8880602;
            case 8880633, 8880637, 8880639, 8880642 -> 8880632;
            case 8880653, 8880657, 8880659, 8880662 -> 8880652;
            // bossflow-0930: Kalos phase-2 / Black Mage Extreme / Limbo bodies (Mob.wz HpLinkMob) -> their HP holder.
            default -> net.swordie.ms.world.boss.Tms282LinkedBodies.sinkFor(mobTemplateId);
        };
    }
}
