package net.swordie.ms.scripts;

public enum ScriptType {
    None(""),
    Npc("npc"),
    Field("field"),
    FirstEnterField("field"),
    Portal("portal"),
    Reactor("reactor"),
    Item("item"),
    Quest("quest"),
    Boss("boss"),
    Content("content");

    private final String dir;

    ScriptType(String dir) {
        this.dir = dir;
    }

    public String getDir() {
        return dir;
    }
}
