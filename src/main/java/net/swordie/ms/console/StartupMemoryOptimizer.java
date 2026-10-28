package net.swordie.ms.console;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;

/** GUI 顯示用：堆已用 / 已提交（比 used/max 更能反映實際佔用）。 */
public final class StartupMemoryOptimizer {
    private StartupMemoryOptimizer() {
    }

    public static HeapSnapshot snapshot() {
        MemoryUsage heap = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
        return new HeapSnapshot(heap.getUsed(), heap.getCommitted(), heap.getMax());
    }

    public static final class HeapSnapshot {
        public final long usedBytes;
        public final long committedBytes;
        public final long maxBytes;

        public HeapSnapshot(long usedBytes, long committedBytes, long maxBytes) {
            this.usedBytes = usedBytes;
            this.committedBytes = committedBytes;
            this.maxBytes = maxBytes;
        }

        public double usedGb() {
            return usedBytes / 1024.0 / 1024.0 / 1024.0;
        }

        public double committedGb() {
            return committedBytes / 1024.0 / 1024.0 / 1024.0;
        }

        public double maxGb() {
            return maxBytes > 0 ? maxBytes / 1024.0 / 1024.0 / 1024.0 : 0;
        }

        /** 進度條百分比：已用 / 已提交（無提交時回退到 max）。 */
        public int usagePercentOfCommitted() {
            long denom = committedBytes > 0 ? committedBytes : maxBytes;
            if (denom <= 0) {
                return 0;
            }
            return (int) Math.min(100L, usedBytes * 100L / denom);
        }
    }
}
