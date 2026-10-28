package net.swordie.ms.world.tms282.collab.frieren;

import java.util.List;
import java.util.Set;

/**
 * 楓之谷 × 葬送的芙莉蓮 (TMS V282, 2026-09-09 .. 2026-10-06) ids, all read from the 282 client data
 * (research/tms2823/collab-frieren/README.md has the dump commands):
 * <ul>
 *     <li>Etc/SetItemInfo.img/1127 迷你葬送的芙莉蓮套裝: pets 5002828..5002831, parts=1 (pet set), completeCount=3,
 *     Effect/1..3/activeSkill = 80000922 / 80000923 / 80000924 (Skill 800009.img: psd, petPassive,
 *     padX = madX = 8 / 10 / 18; Lv.3 string adds the item pick-up range, a client-side effect);</li>
 *     <li>Item/Install/0370.img/03700821 title 葬送的芙莉蓮: info/nickSkill = 80010372 (Skill 800103.img: psd,
 *     padX 30, madX 30, bdR 30, ignoreMobpdpR 30, collabo; the String has no desc);</li>
 *     <li>Quest 68304 [葬送的芙莉蓮] 與芙莉蓮夥伴們的冒險 (autoStart, lvmin 101, after 68299, startscript q68304s)
 *     starts the partner event; its skills are 80012691 一起冒險 and 80012711 芙莉蓮夥伴們 (both timeLimited,
 *     toggled by right-click in the skill window: client CP03BF i32 skillId);</li>
 *     <li>Mob 9602566 複製體怪物: avatarLook/localUserCopyLook, onlyViewMob, individualReward, maxHP 2e9,
 *     fixedDamage 10,000,000, exp 0; quest 600141 caps its kills at 20 a day, quest 505524 counts 10 of them;</li>
 *     <li>80012712 一般攻擊魔法 (芙莉蓮, 1800% x10, 15 mobs, cooltime 120) and 80012713 除魔魔法 (費倫, 820% x12,
 *     15 mobs), area lt(-1200,-800) rb(1200,800);</li>
 *     <li>80012714 / 80012715: EquipmentSkill of the cash weapons 1703722 芙莉蓮的法杖 / 1703723 費倫的法杖
 *     (decorative, type 10, cooltime 360, "此為裝飾技能"); 80004107 / 80004108 are the same decorative casts with
 *     no granting item in the 282 client (older collab ids);</li>
 *     <li>80012692 / 80012693 / 80012694: unnamed summon skills (level/1 time 10,000,000, summon/height 70) next
 *     to 80012691, presumed the 芙莉蓮 / 費倫 / 修塔爾克 companions of 一起冒險 (not confirmed).</li>
 * </ul>
 */
public final class FrierenSkills {
    public static final int MINI_FRIEREN_LV1 = 80000922;
    public static final int MINI_FRIEREN_LV2 = 80000923;
    public static final int MINI_FRIEREN_LV3 = 80000924;
    public static final List<Integer> MINI_FRIEREN = List.of(MINI_FRIEREN_LV1, MINI_FRIEREN_LV2, MINI_FRIEREN_LV3);
    /** padX = madX per level, from Skill 800009.img (fallback when the server skill data lacks them). */
    public static final int[] MINI_FRIEREN_ATTACK = {8, 10, 18};

    public static final int TITLE_SKILL = 80010372;
    public static final int TITLE_ITEM = 3700821;

    public static final int ADVENTURE_TOGETHER = 80012691;
    public static final int PARTNERS = 80012711;
    public static final Set<Integer> TOGGLE_SKILLS = Set.of(ADVENTURE_TOGETHER, PARTNERS);

    public static final int FRIEREN_ATTACK_MAGIC = 80012712;
    public static final int FERN_EXORCISM_MAGIC = 80012713;
    public static final int FRIEREN_STAFF_DECOR = 80012714;
    public static final int FERN_STAFF_DECOR = 80012715;
    public static final int FRIEREN_DECOR_OLD = 80004107;
    public static final int FERN_ZOLTRAAK_DECOR_OLD = 80004108;

    /** Presumed companion summons of 一起冒險 (unnamed in String/Skill.img). */
    public static final List<Integer> COMPANION_SUMMONS = List.of(80012692, 80012693, 80012694);

    public static final int PET_SET_ID = 1127;
    public static final List<Integer> SET_PETS = List.of(5002828, 5002829, 5002830, 5002831);

    public static final int CLONE_MOB = 9602566;
    /** 靈魂艾爾達碎片 (untradeable variant, as the other 282 scripts give it). */
    public static final int SOL_ERDA_FRAGMENT = 4009548;

    public static final int QUEST_START = 68299;
    public static final int QUEST_PARTNERS = 68304;
    public static final int QUEST_PARTNERS_INFO = 68305;
    public static final int QUEST_CLONE_DAILY = 600141;

    /** Companion attack data (Skill 800127.img common), used when the server skill data lacks them. */
    public record CompanionAttack(int skillId, String speaker, int damagePercent, int attackCount, int mobCount,
                                  int left, int top, int right, int bottom) {
    }

    public static final CompanionAttack FRIEREN_WAVE =
            new CompanionAttack(FRIEREN_ATTACK_MAGIC, "芙莉蓮", 1800, 10, 15, -1200, -800, 1200, 800);
    public static final CompanionAttack FERN_WAVE =
            new CompanionAttack(FERN_EXORCISM_MAGIC, "費倫", 820, 12, 15, -1200, -800, 1200, 800);

    private FrierenSkills() {
    }

    public static boolean isToggleSkill(int skillId) {
        return TOGGLE_SKILLS.contains(skillId);
    }

    public static boolean isMiniFrieren(int skillId) {
        return MINI_FRIEREN.contains(skillId);
    }

    public static String skillName(int skillId) {
        return switch (skillId) {
            case ADVENTURE_TOGETHER -> "一起冒險";
            case PARTNERS -> "芙莉蓮夥伴們";
            case FRIEREN_ATTACK_MAGIC -> "一般攻擊魔法(除魔魔法)";
            case FERN_EXORCISM_MAGIC -> "除魔魔法(除魔魔法)";
            default -> Integer.toString(skillId);
        };
    }
}
