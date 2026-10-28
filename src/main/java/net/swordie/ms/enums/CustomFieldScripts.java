package net.swordie.ms.enums;

import net.swordie.ms.constants.BossConstants;
import net.swordie.ms.world.event.RussianRouletteEvent;

import java.util.Arrays;

/**
 * Created on 1-12-2018.
 *
 * @author Asura
 */
public enum CustomFieldScripts { // Custom Field Scripts
    russianRoulette_enter(RussianRouletteEvent.EVENT_MAP, "russianRoulette_enter"),
    vonbonInsideMob(105200520, "vonbonInsideMob"),
    balog_summon(BossConstants.BALROG_HARD_BATTLE_MAP, "balog_summon"),
    easy_balog_summon(BossConstants.BALROG_EASY_BATTLE_MAP, "easy_balog_summon"),
    firstenter_280030000(BossConstants.ZAKUM_CHAOS_ALTAR, "firstenter_280030000"),
    firstenter_280030100(BossConstants.ZAKUM_NORMAL_ALTAR, "firstenter_280030100"),
    firstenter_280030200(BossConstants.ZAKUM_EASY_ALTAR, "firstenter_280030200"),
    firstenter_401060300(401060300, "firstenter_401060300"),
    firstenter_401060100(401060100, "firstenter_401060100"),
    firstenter_401060200(401060200, "firstenter_401060200"),
    firstenter_105200510(105200510, "firstenter_105200510"),
    firstenter_105200110(105200110, "firstenter_105200110"),
    firstenter_105200210(105200210, "firstenter_105200210"),
    firstenter_105200610(105200610, "firstenter_105200610"),
    firstenter_105200310(105200310, "firstenter_105200310"),
    firstenter_105200710(105200710, "firstenter_105200710"),
    firstenter_105200410(105200410, "firstenter_105200410"),
    firstenter_105200810(105200810, "firstenter_105200810"),
    firstenter_262030300(262030300, "firstenter_262030300"),
    firstenter_262031300(262031300, "firstenter_262031300"),
    firstenter_220080100(220080100, "firstenter_220080100"),
    firstenter_220080200(220080200, "firstenter_220080200"),
    firstenter_220080300(220080300, "firstenter_220080300"),
    hontale_boss1_easy(240060000, "hontale_boss1"),
    hontale_boss1_chaos(240060001, "hontale_boss1"),
    hontale_boss1_normal(240060002, "hontale_boss1"),
    firstenter_211070100(211070100, "firstenter_211070100"),
    firstenter_272020200(272020200, "firstenter_272020200"),
    firstenter_272020210(272020210, "firstenter_272020210"),
    firstenter_270050100(270050100, "firstenter_270050100"),
    firstenter_270051100(270051100, "firstenter_270051100"),
    firstenter_271040100(271040100, "firstenter_271040100"),
    firstenter_271041100(271041100, "firstenter_271041100"),
    firstenter_350060700(350060700, "firstenter_350060700"),
    firstenter_350060400(350060400, "firstenter_350060400"),
    firstenter_350160200(350160200, "firstenter_350160200"),
    firstenter_350160100(350160100, "firstenter_350160100"),
    firstenter_211041700(211041700, "firstenter_211041700"),
    firstenter_211042000(211042000, "firstenter_211042000"),
    firstenter_450004150(450004150, "firstenter_450004150"),
    firstenter_450004200(450004200, "firstenter_450004200"),
    firstenter_450004250(450004250, "firstenter_450004250"),
    firstenter_450008750(450008750, "firstenter_450008750"),
    firstenter_450008150(450008150, "firstenter_450008150"),
    firstenter_450008250(450008250, "firstenter_450008250"),
    firstenter_450008350(450008350, "firstenter_450008350"),
    firstenter_450008850(450008850, "firstenter_450008850"),
    firstenter_450008950(450008950, "firstenter_450008950"),
    firstenter_bossBlackMage(BossConstants.BLACK_MAGE_FIGHT_MAP, "firstenter_bossBlackMage"),
    firstenter_450013300(450013300, "firstenter_450013300"),
    firstenter_450013500(450013500, "firstenter_450013500"),
    firstenter_450013700(450013700, "firstenter_450013700"),
    firstenter_811000033(BossConstants.PRINCESS_NO_FIGHT_MAP, "firstenter_811000033"),
    firstenter_941000200(BossConstants.OMNI_CLN_FIGHT_MAP, "firstenter_941000200"),
    firstenter_410002160(BossConstants.SEREN_NORMAL_FIGHT_MAP, "firstenter_410002160"),
    firstenter_410002060(BossConstants.SEREN_HARD_FIGHT_MAP, "firstenter_410002060"),
    firstenter_410002080(BossConstants.SEREN_EXTREME_FIGHT_MAP, "firstenter_410002080"),
    firstenter_410006260(BossConstants.KALOS_EASY_FIGHT_MAP, "firstenter_410006260"),
    firstenter_410006060(BossConstants.KALOS_NORMAL_FIGHT_MAP, "firstenter_410006060"),
    firstenter_410006360(BossConstants.KALOS_HARD_FIGHT_MAP, "firstenter_410006360"),
    firstenter_410006460(BossConstants.KALOS_EXTREME_FIGHT_MAP, "firstenter_410006460"),
    firstenter_410007160(BossConstants.KALING_FIGHT_MAP, "firstenter_410007160"),
    firstenter_410007601(BossConstants.LIMBO_FIGHT_MAP, "firstenter_410007601"),
    firstenter_875001000(BossConstants.BALDRIX_FIGHT_MAP, "firstenter_875001000"),
    firstenter_954109500(BossConstants.FIRST_ADVERSARY_FIGHT_MAP, "firstenter_954109500"),
    firstenter_920012300(BossConstants.MONSTER_PARK_EXTREME_FIGHT_MAP, "firstenter_920012300"),
    firstenter_874004002(BossConstants.AKECHI_FIGHT_MAP, "firstenter_874004002"),
    firstenter_970072000(BossConstants.URSUS_FIGHT_MAP, "firstenter_970072000"),
    firstenter_450010500(BossConstants.VERUS_HILLA_FIGHT_MAP, "firstenter_450010500"),
    firstenter_450009400(450009400, "firstenter_450009400"),
    firstenter_450009450(450009450, "firstenter_450009450"),
    firstenter_450012210(450012210, "firstenter_450012210"),
    firstenter_450012600(450012600, "firstenter_450012600"),
    firstenter_160040000(160040000, "firstenter_160040000"),
    firstenter_160020000(160020000, "firstenter_160020000"),
    ;
    private final int id;
    private final String scriptName;

    CustomFieldScripts(int val, String scriptName) {
        this.id = val;
        this.scriptName = scriptName;
    }

    CustomFieldScripts(int val) {
        this(val, null);
    }

    public static CustomFieldScripts getByVal(int id) {
        return Arrays.stream(values()).filter(cfs -> cfs.getVal() == id).findAny().orElse(null);
    }

    /** Keep the 265 overrides intact; repaired 282 arenas use their actual map IDs. */
    public static String getScriptForField(int id) {
        if (net.swordie.ms.connection.protocol.Protocol.active().version()
                == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3) {
            switch (id) {
                case 875001000, 954109500, 211041700, 211042000, 941000200, 811000033: return null;
                case 350068100, 350068300, 350068500: return "first_Suu";
                case 410009800, 410011800: return "limbo_reward";
                case 410014500, 410015600: return "baldrix_combat";
                case 410020500, 410021500, 410022500, 410023500: return "enter_3phase_firstAdversary";
                case 807300110: return "firstenter_807300110";
                case 807300210: return "firstenter_807300210";
                case 221030910: return "firstenter_221030910";
                case 811000100: return "enter_811000100";
                case 811000200: return "enter_811000200";
                case 811000300: return "enter_811000300";
                case 811000400: return "enter_811000400";
                case 811000500: return "enter_811000500";
                default: break;
            }
            if (net.swordie.ms.world.boss.Malitia.isField(id)) return "malitia_enter_field";
            if (net.swordie.ms.world.boss.Kaling.isField(id)) return "kaling_enter_field";
        }
        CustomFieldScripts script = getByVal(id);
        return script == null ? null : script.getScriptName();
    }

    public int getVal() {
        return id;
    }

    public String getScriptName() {
        return scriptName != null ? scriptName : name();
    }
}
