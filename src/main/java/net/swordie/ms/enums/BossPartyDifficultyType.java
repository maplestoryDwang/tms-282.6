package net.swordie.ms.enums;

public enum BossPartyDifficultyType {

    Easy(0, "Easy"),
    Normal(1, "Normal"),
    Hard(2, "Hard"),
    Chaos(3, "Chaos"),
    Extreme(4, "Extreme"),
    Story(5, "Story");

    private final int val;
    private final String name;

    BossPartyDifficultyType(int val, String name) {
        this.val = val;
        this.name = name;
    }

    public int getVal() {
        return val;
    }

    public String getName() {
        return name;
    }
}
