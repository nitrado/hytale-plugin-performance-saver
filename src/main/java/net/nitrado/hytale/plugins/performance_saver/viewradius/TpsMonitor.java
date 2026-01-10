package net.nitrado.hytale.plugins.performance_saver.viewradius;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.metrics.metric.HistoricMetric;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import net.nitrado.hytale.plugins.performance_saver.config.TpsMonitorConfig;

public class TpsMonitor implements Monitor {
    private HytaleLogger logger;
    private TpsMonitorConfig config;

    public TpsMonitor(HytaleLogger logger, TpsMonitorConfig config) {
        this.logger = logger;
        this.config = config;
    }

    @Override
    public ViewRadiusResult getViewRadiusChange(long lastAdjustmentDeltaNanos) {
        if (lastAdjustmentDeltaNanos < this.config.getAdjustmentDelay().toNanos()) {
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

            var tps = this.getTPS(entry.getValue());
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

    protected double getTPS(World world) {
        HistoricMetric metrics = world.getBufferedTickLengthMetricSet();
        return metrics.getAverage(0);
    }
}
