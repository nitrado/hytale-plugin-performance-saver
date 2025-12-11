package net.nitrado.hytale.plugins.performance_saver;

import com.hypixel.hytale.metrics.metric.HistoricMetric;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import net.nitrado.hytale.plugins.performance_saver.gc.GcObserver;

import javax.annotation.Nonnull;
import java.lang.management.ManagementFactory;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class PerformanceSaver extends JavaPlugin {

    private GcObserver observer;
    private ScheduledFuture<?> gcTask;
    private ScheduledFuture<?> chunkTask;
    private ScheduledFuture<?> saveModeTask;
    private int initialViewRadius;

    private int initialTargetTPS;
    private int currentTargetTPS;

    private long lastAdjustment = 0;
    private long hasZeroChunksSince = 0;
    private boolean hasGcRun = true;
    private long playerLastSeenAt = 0;
    private int tpsAboveHighWatermark = 0;
    private int tpsBelowLowWatermark = 0;

    private final Map<String, TickSampleState> tickSamples = new ConcurrentHashMap<>();

    enum ViewRadiusResult {
        DECREASE,
        INCREASE,
        KEEP
    }

    public PerformanceSaver(@Nonnull JavaPluginInit init) {
        super(init);

        this.observer = new GcObserver();
    }

    @Override
    protected void setup() {
    }

    @Override
    protected void start() {
        this.initialViewRadius = HytaleServer.get().getConfig().getMaxViewRadius();
        this.initialTargetTPS = Math.min(20, Universe.get().getDefaultWorld().getTps());
        this.currentTargetTPS = this.initialTargetTPS;
        this.playerLastSeenAt = ManagementFactory.getRuntimeMXBean().getUptime();

        getLogger().atInfo().log("Initial view radius is %d", this.initialViewRadius);
        try {
            this.observer.start();

            this.gcTask = HytaleServer.SCHEDULED_EXECUTOR.scheduleAtFixedRate(this::adjustViewRadius,
                5, // Initial delay
                1, // Interval
                TimeUnit.SECONDS
            );

            this.chunkTask = HytaleServer.SCHEDULED_EXECUTOR.scheduleWithFixedDelay(this::checkChunks,
                    5, // Initial delay
                    5, // Interval
                    TimeUnit.SECONDS
            );

            this.saveModeTask = HytaleServer.SCHEDULED_EXECUTOR.scheduleAtFixedRate(this::checkPerformanceSaveMode,
                    60, 5, TimeUnit.SECONDS);
        } catch (Exception e) {
            getLogger().atSevere().log("failed starting observer: %s", e.getMessage());
        }
    }

    protected void adjustViewRadius() {
        var currentViewRadius = HytaleServer.get().getConfig().getMaxViewRadius();

        var gcViewRadiusResult = this.viewRadiusBasedOnGcRuns();
        var tpsViewRadiusResult = this.viewRadiusBasedOnTps();

        if  (gcViewRadiusResult == ViewRadiusResult.DECREASE) {
            var newViewRadius = this.reduceViewRadius(currentViewRadius);

            if (newViewRadius != currentViewRadius) {
                Universe.get().sendMessage(Message.raw("Memory critical. Reducing view radius to " + newViewRadius + " chunks."));
                if (currentViewRadius == this.initialViewRadius) {
                    Universe.get().sendMessage(Message.raw("Memory pressure can be caused by fast exploration and similar activities. View radius will recover over time if memory usage allows."));
                }
                getLogger().atSevere().log("Memory critical. Reducing view radius to " + newViewRadius + " chunks.");
            }
        }

        if  (tpsViewRadiusResult == ViewRadiusResult.DECREASE) {
            var newViewRadius = this.reduceViewRadius(currentViewRadius);

            if (newViewRadius != currentViewRadius) {
                Universe.get().sendMessage(Message.raw("TPS low. Reducing view radius to " + newViewRadius + " chunks."));
                if (currentViewRadius == this.initialViewRadius) {
                    Universe.get().sendMessage(Message.raw("Low TPS can be caused by chunk generation and large amounts of active NPCs. View radius will recover when load decreases."));
                }
                getLogger().atSevere().log("TPS low. Reducing view radius to " + newViewRadius  + " chunks.");
            }
        }

        if (gcViewRadiusResult  == ViewRadiusResult.INCREASE && tpsViewRadiusResult  == ViewRadiusResult.INCREASE) {
            this.increaseViewRadius(currentViewRadius);
        }
    }

    protected void checkChunks() {
        var thresholdMs = 5 * 60 * 1000;
        var loadedChunks = Universe.get().getDefaultWorld().getChunkStore().getLoadedChunksCount();

        if (loadedChunks > 0) {
            this.hasZeroChunksSince = 0;
            this.hasGcRun = false;

            return;
        }

        if (this.hasGcRun) {
            return;
        }

        var now = ManagementFactory.getRuntimeMXBean().getUptime();
        if (now - this.hasZeroChunksSince >= thresholdMs) {
            this.flushMemory();
            this.hasGcRun = true;
        }
    }

    protected void flushMemory() {
        getLogger().atInfo().log("Flushing memory");
        System.gc();
    }

    protected ViewRadiusResult viewRadiusBasedOnTps() {
        var subsequentMeasurementsThreshold = 3;

        var lowWaterMark = 0.6 * this.currentTargetTPS;
        var highWaterMark = 0.75 * this.currentTargetTPS;

        var tps = this.getTPS(Universe.get().getDefaultWorld());

        var now = ManagementFactory.getRuntimeMXBean().getUptime();

        if (this.lastAdjustment + 60 * 1000 > now) {
            return ViewRadiusResult.KEEP;
        }

        if (tps < lowWaterMark) {
            this.tpsBelowLowWatermark += 1;
            this.tpsAboveHighWatermark = 0;

            if (this.tpsBelowLowWatermark >= subsequentMeasurementsThreshold) {
                return ViewRadiusResult.DECREASE;
            }
        } else {
            this.tpsBelowLowWatermark = 0;
        }

        if (tps > highWaterMark) {
            this.tpsAboveHighWatermark += 1;
            this.tpsBelowLowWatermark = 0;

            if (this.tpsAboveHighWatermark >= subsequentMeasurementsThreshold) {
                return ViewRadiusResult.INCREASE;
            }
        } else {
            this.tpsAboveHighWatermark = 0;
        }

        return ViewRadiusResult.KEEP;
    }

    protected double getTPS(World world) {
        HistoricMetric metrics = world.getBufferedTickLengthMetricSet();
        long[] periods = metrics.getPeriodsNanos();
        if (periods.length == 0) {
            return -1;
        }

        int windowIndex = periods.length - 1;
        long[] timestamps = metrics.getTimestamps(windowIndex);
        long[] values = metrics.getValues(windowIndex);
        int sampleLength = Math.min(timestamps.length, values.length);
        if (sampleLength == 0) return -1;
        long tickStepNanos = world.getTickStepNanos();
        TickSampleState state = tickSamples.computeIfAbsent(world.getName(), k -> new TickSampleState());

        long now = System.nanoTime();
        long elapsedNanos = now - state.lastPollNano;
        if (elapsedNanos <= 0) elapsedNanos = tickStepNanos;
        state.lastPollNano = now;

        long newestTimestamp = state.lastProcessedTimestamp;
        int ticksProcessed = 0;
        for (int i = sampleLength - 1; i >= 0; i--) {
            long ts = timestamps[i];
            if (ts <= state.lastProcessedTimestamp) break;

            long delta = values[i];
            if (delta <= 0 || delta == Long.MAX_VALUE) continue;

            ticksProcessed++;
            if (ts > newestTimestamp) newestTimestamp = ts;
        }

        double tps;
        if (ticksProcessed > 0) {
            state.lastProcessedTimestamp = newestTimestamp;
            double elapsedSeconds = elapsedNanos / 1_000_000_000d;
            tps = ticksProcessed / Math.max(elapsedSeconds, tickStepNanos / 1_000_000_000d);
        } else if (state.lastProcessedTimestamp == Long.MIN_VALUE) {
            tps = world.getTps();
        } else {
            double elapsedSeconds = elapsedNanos / 1_000_000_000d;
            if (elapsedSeconds <= 0) elapsedSeconds = tickStepNanos / 1_000_000_000d;
            tps = 0.0d;
        }

        state.lastReportedTps = tps;

        return tps;
    }

    protected ViewRadiusResult viewRadiusBasedOnGcRuns() {
        var now = ManagementFactory.getRuntimeMXBean().getUptime();
        var gcRuns = this.observer.getRecentRuns();
        var totalHeap = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getMax();
        var threshold = 0.90;

        if (totalHeap < 0) {
            return ViewRadiusResult.KEEP;
        }

        var matches = 0;
        for (var run : gcRuns.reversed()) {
            if (run.timeMs() < this.lastAdjustment) {
                getLogger().atFiner().log("breaking because not enough measurements since last adjustment");
                break;
            }

            if (run.timeMs() < now - 60 * 1000) {
                getLogger().atFiner().log("breaking because GC too long ago");
                break;
            }

            var relativeUsage = ((double) run.bytesAfter() / totalHeap);

            if (relativeUsage <  threshold) {
                getLogger().atFiner().log("breaking because still %.2f%% left", 100 * relativeUsage);
                break;
            }

            matches++;
        }

        getLogger().atFiner().log("GC run matches: %d",  matches);

        if (matches >= 3) {
            return ViewRadiusResult.DECREASE;
        }

        if (matches == 0 && now - this.lastAdjustment > 30 * 1000) {
            return ViewRadiusResult.INCREASE;
        }

        return ViewRadiusResult.KEEP;
    }

    protected void checkPerformanceSaveMode() {
        var throttledTPS = 5;

        var now = ManagementFactory.getRuntimeMXBean().getUptime();

        if (Universe.get().getPlayerCount() > 0) {
            this.playerLastSeenAt = ManagementFactory.getRuntimeMXBean().getUptime();
        }

        this.currentTargetTPS = now - this.playerLastSeenAt > 5 * 60 * 1000 ? throttledTPS : this.initialTargetTPS;

        var defaultWorld = Universe.get().getDefaultWorld();
        if (defaultWorld.getTps() != this.currentTargetTPS) {
            getLogger().atInfo().log("Setting TPS to %d", this.currentTargetTPS);
            CompletableFuture.runAsync(() -> {
                defaultWorld.setTps(this.currentTargetTPS);
            }, defaultWorld);
        }
    }

    protected int reduceViewRadius(int currentViewRadius) {
        var minViewRadius = 2;
        var factor = 0.75;


        var newViewRadius = (int) Math.max(minViewRadius, Math.floor(factor * currentViewRadius));

        if (newViewRadius >= currentViewRadius) {
            return currentViewRadius;
        }

        this.lastAdjustment = ManagementFactory.getRuntimeMXBean().getUptime();
        HytaleServer.get().getConfig().setMaxViewRadius(newViewRadius);
        return newViewRadius;
    }

    protected int increaseViewRadius(int currentViewRadius) {
        var newViewRadius = Math.min(currentViewRadius + 1, this.initialViewRadius);

        if (newViewRadius > currentViewRadius) {
            Universe.get().sendMessage(Message.raw("Increasing view radius back to " + newViewRadius + " chunks."));
            getLogger().atInfo().log("Increasing view radius back to " + newViewRadius + " chunks.");
            this.lastAdjustment = ManagementFactory.getRuntimeMXBean().getUptime();
            HytaleServer.get().getConfig().setMaxViewRadius(newViewRadius);
            return newViewRadius;
        }

        return currentViewRadius;
    }

    @Override
    protected void shutdown() {
        getLogger().atInfo().log("Restoring view radius to %d", this.initialViewRadius);
        HytaleServer.get().getConfig().setMaxViewRadius(this.initialViewRadius);

        try {
            this.observer.stop();

        } catch (Exception e) {
            getLogger().atSevere().log("failed stopping observer: %s", e.getMessage());
        }

        this.gcTask.cancel(false);
        this.chunkTask.cancel(false);
        this.saveModeTask.cancel(false);
    }

    private static final class TickSampleState {
        long lastProcessedTimestamp = Long.MIN_VALUE;
        long lastPollNano = System.nanoTime();
        double lastReportedTps;
    }
}