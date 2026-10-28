package net.swordie.ms.world.tms282.revival;

/**
 * Built-in revival-events.json, used when data/tms282-revival/revival-events.json is absent. The shipped sample
 * deploy/revival/revival-events.json must be identical (Tms282RevivalCheck data).
 */
public final class RevivalDefaults {
    public static final String JSON = """
            {
              "version": "2026-10-05.1",
              "events": {
                "sunday": {"enabled": true, "note": "星期天楓之谷：日期另見 data/resources/tms282/events/sunnysunday.tw.json"},
                "punch": {"enabled": true, "note": "世界最強拳擊王：大廳 993222000，NPC 9062764 入場、9062765 排行"},
                "mushroom": {"enabled": true, "note": "究極菇菇通行證：NPC 9401434 茱妮（弓箭手村、神木村）"},
                "lacheln": {"enabled": true, "note": "夢中的拉契爾恩：993236200，NPC 9063135 任務、9063164/9063141/9063162 碎片商店"},
                "mapletree": {"enabled": true, "note": "楓之樹：993200000，NPC 9062645 任務、9062653/9062657 商店"},
                "circus": {"enabled": true, "note": "星夜馬戲團（武陵篇）：867168000，NPC 9401996 任務、9402010 商店"},
                "restaurant": {"enabled": true, "note": "溫餐廳：993223000，NPC 9062797 任務、9062813/9062815 商店"},
                "edentisk": {"enabled": true, "note": "伊甸提斯克：993228000，NPC 9062962 任務、9062968/9062972 商店"}
              }
            }
            """;

    private RevivalDefaults() {
    }
}
