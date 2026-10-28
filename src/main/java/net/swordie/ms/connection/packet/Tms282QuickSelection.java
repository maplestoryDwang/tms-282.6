package net.swordie.ms.connection.packet;

/** Native CWvsContext+E0/E4/E8/EC tuple, submitted as CP0079 tuple A. */
public record Tms282QuickSelection(int worldId, int channelId, int characterId) {
    public Tms282QuickSelection {
        if (worldId < 0 || worldId > 255 || channelId < 0 || channelId > 255 || characterId <= 0)
            throw new IllegalArgumentException("Invalid quick selection");
    }
}
