package net.swordie.ms.client.character;

import java.util.ArrayList;
import java.util.List;

public class IdleHunting {

    private int id;
    private int charId;
    private int mapId;
    private int mesoDropRate;
    private int itemDropRate; // Đổi tên dropRate thành itemDropRate cho rõ ràng
    private int expRate;
    private long mobKillRatePerSecond; // Tốc độ giết quái mỗi giây, lưu dưới dạng long
    private long expRatePerSecond; // Tốc độ nhận exp mỗi giây
    private long mesosRatePerSecond; // Tốc độ nhận mesos mỗi giây
    private long currentMobKilled = 0;
    private long currentEXPGained = 0;
    private long currentMesosLooted = 0;
    private long totalMobKilled;
    private long totalExpGained;
    private long totalMesosLooted;
    private List<Integer> itemIdsLooted = new ArrayList<>();
    private long startTimeMillis;
    private long endTimeMillis; // Thời gian kết thúc săn quái (người chơi đăng nhập lại)

    public IdleHunting(int charId, int mapId, long startTimeMillis, int mesoDropRate, int itemDropRate, int expRate) {
        this.charId = charId;
        this.mapId = mapId;
        this.startTimeMillis = startTimeMillis;
        this.mesoDropRate = mesoDropRate;
        this.itemDropRate = itemDropRate;
        this.expRate = expRate;
        this.totalExpGained = 0;
        this.totalMesosLooted = 0;
        this.totalMobKilled = 0;
        this.itemIdsLooted = new ArrayList<>();
    }

    // Constructor khi lấy dữ liệu từ database
    public IdleHunting(int id, int charId, int mapId, int mesoDropRate, int itemDropRate, int expRate, long mobKillRatePerSecond, long expRatePerSecond, long mesosRatePerSecond, long totalMobKilled, long totalExpGained, long totalMesosLooted, List<Integer> itemIdsLooted, long startTimeMillis, long endTimeMillis) {
        this.id = id;
        this.charId = charId;
        this.mapId = mapId;
        this.mesoDropRate = mesoDropRate;
        this.itemDropRate = itemDropRate;
        this.expRate = expRate;
        this.mobKillRatePerSecond = mobKillRatePerSecond;
        this.expRatePerSecond = expRatePerSecond;
        this.mesosRatePerSecond = mesosRatePerSecond;
        this.totalMobKilled = totalMobKilled;
        this.totalExpGained = totalExpGained;
        this.totalMesosLooted = totalMesosLooted;
        this.itemIdsLooted = itemIdsLooted;
        this.startTimeMillis = startTimeMillis;
        this.endTimeMillis = endTimeMillis;
    }

    // Getters and Setters
    public void setEndTimeMillis(long endTimeMillis) {
        this.endTimeMillis = endTimeMillis;
    }

    public long getOfflineDurationInSeconds() {
        if (endTimeMillis == 0) {
            return (System.currentTimeMillis() - startTimeMillis) / 1000;
        }
        return (endTimeMillis - startTimeMillis) / 1000;
    }

    public void setRates(long mobKillRate, long expRate, long mesoRate) {
        this.mobKillRatePerSecond = mobKillRate;
        this.expRatePerSecond = expRate;
        this.mesosRatePerSecond = mesoRate;
    }

    public long getMobKillRatePerSecond() {
        return mobKillRatePerSecond;
    }

    public void setMobKillRatePerSecond(long mobKillRatePerSecond) {
        this.mobKillRatePerSecond = mobKillRatePerSecond;
    }

    public long getExpRatePerSecond() {
        return expRatePerSecond;
    }

    public void setExpRatePerSecond(long expRatePerSecond) {
        this.expRatePerSecond = expRatePerSecond;
    }

    public long getMesosRatePerSecond() {
        return mesosRatePerSecond;
    }

    public void setMesosRatePerSecond(long mesosRatePerSecond) {
        this.mesosRatePerSecond = mesosRatePerSecond;
    }

    public long getCurrentMobKilled() {
        return currentMobKilled;
    }

    public void setCurrentMobKilled(long currentMobKilled) {
        this.currentMobKilled = currentMobKilled;
    }

    public void incMob(long inc) {
        this.currentMobKilled += inc;
    }

    public long getCurrentEXPGained() {
        return currentEXPGained;
    }

    public void incEXP(long inc) {
        this.currentEXPGained += inc;
    }

    public void setCurrentEXPGained(long currentEXPGained) {
        this.currentEXPGained = currentEXPGained;
    }

    public long getCurrentMesosLooted() {
        return currentMesosLooted;
    }

    public void incMeso(long inc) {
        this.currentMesosLooted += inc;
    }

    public void setCurrentMesosLooted(long currentMesosLooted) {
        this.currentMesosLooted = currentMesosLooted;
    }
}