package net.swordie.ms.handlers;

import net.swordie.ms.Server;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledFuture;

public class GlobalTimerManager {
    public static final ConcurrentHashMap<Integer, CopyOnWriteArrayList<ScheduledFuture<?>>> managedCharacterTimers = new ConcurrentHashMap<>();
    public static final ConcurrentHashMap<Integer, CopyOnWriteArrayList<ScheduledFuture<?>>> managedFieldTimers = new ConcurrentHashMap<>();

    public static void addCharTimer(int charId, ScheduledFuture<?> timer) {
        managedCharacterTimers.computeIfAbsent(charId, k -> new CopyOnWriteArrayList<>()).add(timer);
    }

    public static void addFieldTimer(int fieldSerialId, ScheduledFuture<?> timer) {
        managedFieldTimers.computeIfAbsent(fieldSerialId, k -> new CopyOnWriteArrayList<>()).add(timer);
    }

    public static void removeAllCharTimers(int charID) {
        if (Server.get().getWorld().getCharById(charID) == null) {
            for (ScheduledFuture<?> timer : managedCharacterTimers.get(charID)) {
                if (timer != null) {
                    timer.cancel(true);
                }
            }
            GlobalTimerManager.managedCharacterTimers.remove(charID);
            System.out.println("[Cleaner] 已清理不存在角色的定時器: " + charID);
        }
    }
}
