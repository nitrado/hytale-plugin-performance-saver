package net.nitrado.hytale.plugins.performance_saver.viewradius;

import com.hypixel.hytale.logger.HytaleLogger;
import net.nitrado.hytale.plugins.performance_saver.config.GcMonitorConfig;

import java.lang.management.ManagementFactory;
import java.time.temporal.ChronoUnit;

public class GcMonitor implements Monitor {

    private final GcObserver observer = new GcObserver(30);
    private final HytaleLogger logger;
    private final GcMonitorConfig config;

    private final long timeDiff = System.nanoTime() - ManagementFactory.getRuntimeMXBean().getUptime() * 1_000_000;

    public GcMonitor(HytaleLogger logger, GcMonitorConfig config) {
        this.logger = logger;
        this.config = config;
    }

    @Override
    public ViewRadiusResult getViewRadiusChange(long lastAdjustmentNanos) {
        var now = System.nanoTime();
        var gcRuns = this.observer.getRecentRuns();
        var totalHeap = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getMax();

        if (totalHeap < 0) {
            return ViewRadiusResult.INCREASE;
        }

        var matches = 0;
        for (var run : gcRuns.reversed()) {
            var runAsSystemNano = run.timeMs() * 1_000_000 + this.timeDiff;

            if (runAsSystemNano < lastAdjustmentNanos) {
                this.logger.atFiner().log("breaking because not enough measurements since last adjustment");
                break;
            }

            if (runAsSystemNano < now - this.config.getWindow().getNano()) {
                this.logger.atFiner().log("breaking because GC too long ago");
                break;
            }

            var relativeUsage = ((double) run.bytesAfter() / totalHeap);

            if (relativeUsage <  this.config.getHeapThresholdRatio()) {
                this.logger.atFiner().log("breaking because still %.2f%% left", 100 * relativeUsage);
                break;
            }

            matches++;
        }

        this.logger.atFiner().log("GC run matches: %d",  matches);

        if (matches >= this.config.getTriggerSequenceLength()) {
            return ViewRadiusResult.DECREASE;
        }

        if (matches == 0 && now - lastAdjustmentNanos > this.config.getRecoveryWaitTime().getNano()) {
            return ViewRadiusResult.INCREASE;
        }

        return ViewRadiusResult.KEEP;
    }

    @Override
    public void start() throws Exception {
        this.observer.start();
    }

    @Override
    public void stop() throws Exception {
        this.observer.stop();
    }
}
