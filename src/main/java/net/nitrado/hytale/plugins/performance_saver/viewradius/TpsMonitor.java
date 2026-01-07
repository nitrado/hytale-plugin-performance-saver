package net.nitrado.hytale.plugins.performance_saver.viewradius;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.metrics.metric.HistoricMetric;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import net.nitrado.hytale.plugins.performance_saver.config.TpsMonitorConfig;

import java.time.Duration;

public class TpsMonitor implements Monitor {
    private HytaleLogger logger;
    private TpsMonitorConfig config;

    public TpsMonitor(HytaleLogger logger, TpsMonitorConfig config) {
        this.logger = logger;
        this.config = config;
    }

    @Override
    public ViewRadiusResult getViewRadiusChange(long lastAdjustment) {
        long now = System.nanoTime();

        if (now - lastAdjustment < this.config.getAdjustmentDelay().toNanos()) {
            return ViewRadiusResult.KEEP;
        }

        var worlds = Universe.get().getWorlds();
        var onlyWorlds = this.config.getOnlyWorlds();
        if (onlyWorlds.contains(TpsMonitorConfig.DEFAULT_WORLD)) {
            onlyWorlds.add(Universe.get().getDefaultWorld().getName());
        }

        var allAboveHighWaterMark = true;
        var oneBelowLowWaterMark = false;
        for (var entry : worlds.entrySet()) {
            if (!onlyWorlds.isEmpty() && !onlyWorlds.contains(entry.getKey())) {
                continue;
            }

            var tps = this.getTPS(entry.getValue(), this.config.getWindow());
            var tpsRatio = tps / entry.getValue().getTps();
            if (tpsRatio < this.config.getTpsWaterMarkLow()) {
                oneBelowLowWaterMark = true;
                allAboveHighWaterMark = false;
                break;
            }

            allAboveHighWaterMark = allAboveHighWaterMark && tpsRatio > this.config.getTpsWaterMarkHigh();
        }

        if (oneBelowLowWaterMark) {
            return ViewRadiusResult.DECREASE;
        }

        if (allAboveHighWaterMark) {
            return ViewRadiusResult.INCREASE;
        }

        return ViewRadiusResult.KEEP;
    }

    @Override
    public void start() throws Exception {

    }

    @Override
    public void stop() throws Exception {

    }

    protected double getTPS(World world, Duration since) {
        long now = System.nanoTime();
        HistoricMetric metrics = world.getBufferedTickLengthMetricSet();

        long[] timestamps = metrics.getAllTimestamps();
        long[] values = metrics.getAllValues();

        int sampleLength = Math.min(timestamps.length, values.length);
        long ticksNewerThan = now - since.getNano();

        if (sampleLength == 0 || timestamps[0] > ticksNewerThan) {
            // We don't have enough measurements yet
            return world.getTps();
        }

        int ticksProcessed = 0;
        for (int i = sampleLength - 1; i >= 0; i--) {
            long ts = timestamps[i];
            if (ts <= ticksNewerThan) {
                break;
            }

            long delta = values[i];
            if (delta <= 0 || delta == Long.MAX_VALUE) continue;

            ticksProcessed++;
        }

        return ticksProcessed / (double) since.getSeconds();
    }
}
