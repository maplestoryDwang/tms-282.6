package net.swordie.ms.loaders;

import net.swordie.ms.loaders.Etc.EtcData;
import net.swordie.ms.loaders.Etc.SetItemInfo.SetItemInfoData;
import net.swordie.ms.loaders.Etc.VCore.VCore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created on 11/17/2017.
 */
public class DataClasses {
    public static List<Class> dataClasses = new ArrayList<>();
    public static List<Class> datCreators = new ArrayList<>();

    static {
        dataClasses.addAll(Arrays.asList(
                        ItemData.class,
                        SkillData.class,
                        FieldData.class,
                        MobData.class,
                        NpcData.class,
                        ReactorData.class,
                        QuestData.class,
                        StringData.class,
                        EtcData.class,
                        VCore.class,
                        SetItemInfoData.class
                )
        );
        datCreators.addAll(Arrays.asList(
                        FieldData.class,
                        ItemData.class,
                        MobData.class,
                        StringData.class,
                        NpcData.class,
                        QuestData.class,
                        ReactorData.class,
                        SkillData.class,
                        EtcData.class,
                        SetItemInfoData.class
                )
        );
    }
}

