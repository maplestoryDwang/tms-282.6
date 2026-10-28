package net.swordie.ms.scripts;

/** Presentation only for the user-owned panel. IDs, actions and other NPC dialogs stay intact. */
public final class CatPanelText {
    private CatPanelText() { }

    public static String format(String script, String text, boolean tms282) {
        if (!tms282 || script == null || text == null || text.isBlank()
                || !(script.equals("expand/menu") || script.startsWith("expand/menu/"))) return text;
        return text.replace("萬能面板", "貓貓谷").replace("返回主菜單", "返回貓貓谷")
                .replace("返回主選單", "返回貓貓谷");
    }
}
