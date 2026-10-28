package net.swordie.ms.constants;

import net.swordie.ms.enums.WzHashType;

import java.util.List;

/**
 * Created on 14-7-2018.
 */
public class WzConstants {

    // Quest Icons
    public static final String ICON_QUEST_AVAILABLE = "#fUI/UIWindow2.img/QuestIcon/0/0#";
    public static final String ICON_QUEST_IN_PROGRESS = "#fUI/UIWindow2.img/QuestIcon/1/0#";
    public static final String ICON_QUEST_FINISHED = "#fUI/UIWindow2.img/QuestIcon/2/0#";
    public static final String ICON_SELECT_YOUR_REWARD = "#fUI/UIWindow2.img/QuestIcon/3/0#";
    public static final String ICON_OBTAINED = "#fUI/UIWindow2.img/QuestIcon/4/0#";
    public static final String ICON_MYSTERY_ITEM = "#fUI/UIWindow2.img/QuestIcon/5/0#";
    public static final String ICON_FAME = "#fUI/UIWindow2.img/QuestIcon/6/0#";
    public static final String ICON_MESOS = "#fUI/UIWindow2.img/QuestIcon/7/0#";
    public static final String ICON_EXP = "#fUI/UIWindow2.img/QuestIcon/8/0#";
    public static final String ICON_PET_CLOSENESS = "#fUI/UIWindow2.img/QuestIcon/9/0#";
    public static final String ICON_DRAGON_SP = "#fUI/UIWindow2.img/QuestIcon/10/0#";
    public static final String ICON_TRAITS = "#fUI/UIWindow2.img/QuestIcon/11/0#";

    // Effects
    public static final String EFFECT_MONSTER_PARK_CLEAR = "Map/Effect.img/monsterPark/clear";
    public static final String EFFECT_MONSTER_PARK_FINAL_STAGE = "Map/Effect.img/monsterPark/stageEff/final";
    public static final String EFFECT_MONSTER_PARK_STAGE_NUMBER = "Map/Effect.img/monsterPark/stageEff/number/"; // Requires a number (between 1~5) to be added afterwards
    public static final String EFFECT_MONSTER_PARK_STAGE = "Map/Effect.img/monsterPark/stageEff/stage";
    public static final String EFFECT_DOJO_CLEAR = "Map/Effect.img/dojang/end/clear";
    public static final String EFFECT_DOJO_STAGE_NUMBER = "Map/Effect.img/dojang/start/number/"; // Requires a number (between 1~41) to be added afterwards
    public static final String EFFECT_DOJO_STAGE = "Map/Effect.img/dojang/start/stage";

    public static final String EFFECT_FAIL = "Map/Effect.img/hillah/fail";
    public static final String EFFECT_FINISH = "Map/Effect.img/killing/first/finish";
    public static final String EFFECT_CLEAR = "Map/EffectTW.img/arisan/clear";
    public static final String EFECT_START = "Map/EffectTW.img/arisan/start";

    public static final String EFFECT_PQ_CLEAR = "Map/Effect.img/quest/party/clear";
    public static final String EFFECT_PQ_SOUND_CLEAR = "Sound/Field.img/Party1/Clear";
    public static final String EFECT_PQ_WRONG = "Map/Effect.img/quest/party/wrong_kor";
    public static final String EFECT_PQ_SOUND_WRONG = "Sound/Field.img/Party1/Failed";

    /**
     * Event Fever Scroll: Image Size (206, 25)
     **/
    public static final String EFFECT_FEVER_TIME_START = "Effect/BasicEff.img/FeverTime/start";
    public static final String EFFECT_FEVER_TIME_ING = "Effect/BasicEff.img/FeverTime/ing";
    public static final String EFFECT_FEVER_TIME_END = "Effect/BasicEff.img/FeverTime/end";

    /**
     * Event Miracle Cubing: Image Size (206, 25)
     **/
    public static final String EFFECT_MIRACLE_TIME_START = "Effect/BasicEff.img/MiracleTime/start";
    public static final String EFFECT_MIRACLE_TIME_ING = "Effect/BasicEff.img/MiracleTime/ing";
    public static final String EFFECT_MIRACLE_TIME_END = "Effect/BasicEff.img/MiracleTime/end";

    /**
     * Event Exp Rate: Image Size (238, 25)
     **/
    public static final String EFFECT_EXP_RATE_EVENT_START = "Effect/BasicEff.img/ExpRateEvent/start";
    public static final String EFFECT_EXP_RATE_EVENT_ING = "Effect/BasicEff.img/ExpRateEvent/ing";
    public static final String EFFECT_EXP_RATE_EVENT_END = "Effect/BasicEff.img/ExpRateEvent/end";

    /**
     * Event Drop Rate: Image Size (262, 25)
     **/
    public static final String EFFECT_DROP_RATE_EVENT_START = "Effect/BasicEff.img/DropRateEvent/start";
    public static final String EFFECT_DROP_RATE_EVENT_ING = "Effect/BasicEff.img/DropRateEvent/ing";
    public static final String EFFECT_DROP_RATE_EVENT_END = "Effect/BasicEff.img/DropRateEvent/end";

    public static final List<String> WZ_HASH = List.of(
            "fc5c0d430b936fd7dd2f04d8dedde65f", //MapleCoin.dll
            "d36615b9a7e25e29e9db257373d604cc", //MapleCoin.exe
            "09d9ede0592f8baa4d5a54dadd908245", //Character.wz
            "d7e04bb0eee95a2e1207750c6156b5df", //Etc.wz
            "514b0c917e4a3491238f2b611b2c3721", //Item.wz
            "b040fe777c8797b5324a05e42b66a138", //Map.wz
            "2eed0e91bb44beb97f0afdd4f0cf2739", //Map2.wz
            "c35ba28fd3b70c55069012e1fd8c4356", //Mob.wz
            "e8069de67e9ab509b0505a17db1e4f1c", //Mob2.wz
            "f16e5bba06fb344eedaebe640ec4f062"  //Skill.wz
    );

    public static boolean isCorrectWzHashByType(WzHashType type, String hash) {
        if (type == null) {
            return false;
        }
        return hash.contains(WZ_HASH.get(type.getVal()));
    }

    public static final List<Double> WM_DATA = List.of(
            1.2,
            1.2,
            1.7,
            1.5,
            1.5,
            1.5,
            1.34,
            1.3,
            1.43,
            1.49,
            1.35,
            1.75,
            1.75,
            1.25,
            1.25
    );
}