package net.nitrado.hytale.plugins.performance_saver.viewradius;

import com.sun.management.GarbageCollectionNotificationInfo;
import javax.management.*;
import javax.management.NotificationListener;
import javax.management.openmbean.CompositeData;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryType;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class GcObserver {
    private final int maxRuns;
    private final Deque<GcRun> recentRuns;
    private final List<ListenerRegistration> registeredListeners = new ArrayList<>();

    private record ListenerRegistration(ObjectName name, NotificationListener listener) {}

    public GcObserver() {
        this(5);
    }

    public GcObserver(int maxRuns) {
        this.maxRuns = maxRuns;
        this.recentRuns = new ArrayDeque<>(maxRuns);
    }

    public record GcRun(long timeMs, long durationMs, long bytesBefore, long bytesAfter) {}

    public void start() throws Exception {
        for (GarbageCollectorMXBean gcBean : ManagementFactory.getGarbageCollectorMXBeans()) {
            ObjectName name = new ObjectName("java.lang:type=GarbageCollector,name=" + gcBean.getName());
            NotificationListener listener = (notification, handback) -> {
                if (!notification.getType().equals(GarbageCollectionNotificationInfo.GARBAGE_COLLECTION_NOTIFICATION)) return;
                GarbageCollectionNotificationInfo info =
                        GarbageCollectionNotificationInfo.from((CompositeData) notification.getUserData());

                String cause = info.getGcCause(); // e.g., "Allocation Failure"
                String action = info.getGcAction(); // "end of minor GC" or "end of major GC"

                // Track major/full GCs
                if (!action.toLowerCase().contains("major") && !cause.toLowerCase().contains("tenured")) {
                    return;
                }

                Set<String> heapPoolNames = ManagementFactory.getMemoryPoolMXBeans().stream()
                        .filter(p -> p.getType() == MemoryType.HEAP)
                        .map(MemoryPoolMXBean::getName)
                        .collect(Collectors.toSet());

                long before = info.getGcInfo().getMemoryUsageBeforeGc().entrySet().stream()
                        .filter(e -> heapPoolNames.contains(e.getKey()))
                        .mapToLong(e -> e.getValue().getUsed()).sum();
                long after = info.getGcInfo().getMemoryUsageAfterGc().entrySet().stream()
                        .filter(e -> heapPoolNames.contains(e.getKey()))
                        .mapToLong(e -> e.getValue().getUsed()).sum();

                var run = new GcRun(info.getGcInfo().getEndTime(), info.getGcInfo().getDuration(), before, after);
                synchronized (recentRuns) {
                    if (recentRuns.size() >= maxRuns) {
                        recentRuns.removeFirst();
                    }
                    recentRuns.addLast(run);
                }
            };
            ManagementFactory.getPlatformMBeanServer().addNotificationListener(name, listener, null, null);
            registeredListeners.add(new ListenerRegistration(name, listener));
        }
    }

    public void stop() throws Exception {
        for (ListenerRegistration reg : registeredListeners) {
            ManagementFactory.getPlatformMBeanServer().removeNotificationListener(reg.name(), reg.listener());
        }
        registeredListeners.clear();
    }

    public List<GcRun> getRecentRuns() {
        synchronized (recentRuns) {
            return new ArrayList<>(recentRuns);
        }
    }
}