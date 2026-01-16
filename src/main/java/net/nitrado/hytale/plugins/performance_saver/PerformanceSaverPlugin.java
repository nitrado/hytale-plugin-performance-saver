package net.nitrado.hytale.plugins.performance_saver;

import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.permissions.PermissionsModule;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.util.Config;
import net.nitrado.hytale.plugins.performance_saver.chunks.ChunkGarbageCollector;
import net.nitrado.hytale.plugins.performance_saver.config.PerformanceSaverPluginConfig;
import net.nitrado.hytale.plugins.performance_saver.tps.TpsAdjuster;
import net.nitrado.hytale.plugins.performance_saver.viewradius.GcMonitor;
import net.nitrado.hytale.plugins.performance_saver.viewradius.Monitor;
import net.nitrado.hytale.plugins.performance_saver.viewradius.TpsMonitor;
import net.nitrado.hytale.plugins.performance_saver.viewradius.ViewRadiusResult;

import javax.annotation.Nonnull;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class PerformanceSaverPlugin extends JavaPlugin {

    private final Config<PerformanceSaverPluginConfig> _config = withConfig(PerformanceSaverPluginConfig.CODEC);
    private PerformanceSaverPluginConfig config;

    private ScheduledFuture<?> viewRadiusTask;
    private ScheduledFuture<?> chunkTask;
    private ScheduledFuture<?> tpsTask;
    private int initialViewRadius;

    private long lastAdjustmentNanos = 0;

    private Monitor gcMonitor;
    private Monitor tpsMonitor;

    private ChunkGarbageCollector chunkGarbageCollector;

    private TpsAdjuster tpsAdjuster;

    public PerformanceSaverPlugin(@Nonnull JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        this.config = this._config.get();

        this.gcMonitor = new GcMonitor(getLogger().getSubLogger("GcMonitor"), this.config.getViewRadiusConfig().getGcMonitorConfig());
        this.tpsMonitor = new TpsMonitor(getLogger().getSubLogger("TpsMonitor"), this.config.getViewRadiusConfig().getTpsMonitorConfig());
        this.chunkGarbageCollector = new ChunkGarbageCollector(getLogger().getSubLogger("ChunkGarbageCollector"), this.config.getChunkGarbageCollectorConfig());
        this.tpsAdjuster = new TpsAdjuster(getLogger().getSubLogger("TpsAdjuster"), this.config.getTpsAdjusterConfig());
        this._config.save();
    }

    @Override
    protected void start() {
        this.initialViewRadius = HytaleServer.get().getConfig().getMaxViewRadius();

        getLogger().atInfo().log("Initial view radius is %d", this.initialViewRadius);
        try {

            if (this.config.getViewRadiusConfig().isEnabled()) {
                this.tpsMonitor.start();
                this.gcMonitor.start();
                this.viewRadiusTask = HytaleServer.SCHEDULED_EXECUTOR.scheduleAtFixedRate(this::adjustViewRadius,
                        this.config.getViewRadiusConfig().getInitialDelay().getSeconds(), // Initial delay
                        this.config.getViewRadiusConfig().getCheckInterval().getSeconds(), // Interval
                        TimeUnit.SECONDS
                );
            }

            if (this.config.getChunkGarbageCollectorConfig().isEnabled()) {
                this.chunkTask = HytaleServer.SCHEDULED_EXECUTOR.scheduleAtFixedRate(this::checkChunks,
                        this.config.getChunkGarbageCollectorConfig().getInitialDelay().getSeconds(), // Initial delay
                        this.config.getChunkGarbageCollectorConfig().getCheckInterval().getSeconds(), // Interval
                        TimeUnit.SECONDS
                );
            }

            if (this.config.getTpsAdjusterConfig().isEnabled()) {
                this.tpsTask = HytaleServer.SCHEDULED_EXECUTOR.scheduleAtFixedRate(this::checkTps,
                        this.config.getTpsAdjusterConfig().getInitialDelay().getSeconds(),
                        this.config.getTpsAdjusterConfig().getCheckInterval().getSeconds(),
                        TimeUnit.SECONDS
                );
            }
        } catch (Exception e) {
            getLogger().atSevere().log("failed starting plugin: %s", e.getMessage());
        }
    }

    protected void adjustViewRadius() {
        var currentViewRadius = HytaleServer.get().getConfig().getMaxViewRadius();

        var gcViewRadiusResult = ViewRadiusResult.INCREASE;
        var tpsViewRadiusResult = ViewRadiusResult.INCREASE;

        long now = System.nanoTime();

        var lastAdjustmentDeltaNanos = now - lastAdjustmentNanos;

        if (this.config.getViewRadiusConfig().getGcMonitorConfig().isEnabled()) {
            gcViewRadiusResult = this.gcMonitor.getViewRadiusChange(lastAdjustmentDeltaNanos);
        }

        if (this.config.getViewRadiusConfig().getTpsMonitorConfig().isEnabled()) {
            tpsViewRadiusResult = this.tpsMonitor.getViewRadiusChange(lastAdjustmentDeltaNanos);
        }

        if (gcViewRadiusResult == ViewRadiusResult.DECREASE) {
            var newViewRadius = this.reduceViewRadius(currentViewRadius);

            if (newViewRadius != currentViewRadius) {
                if (this.config.getViewRadiusConfig().isRequireNotifyPermission()) {
                    Universe.get().getPlayers().stream()
                            .filter(playerRef -> PermissionsModule.get().hasPermission(playerRef.getUuid(), Permissions.NOTIFY_DECREASE))
                            .forEach(playerRef -> playerRef.sendMessage(Message.raw("Memory critical. Reducing view radius to " + newViewRadius + " chunks.")));
                } else {
                    Universe.get().getPlayers().forEach(playerRef -> playerRef.sendMessage(Message.raw("Memory critical. Reducing view radius to " + newViewRadius + " chunks.")));
                }
                if (currentViewRadius == this.initialViewRadius) {
                    if (this.config.getViewRadiusConfig().isRequireNotifyPermission()) {
                        Universe.get().getPlayers().stream()
                                .filter(playerRef -> PermissionsModule.get().hasPermission(playerRef.getUuid(), Permissions.NOTIFY_DECREASE))
                                .forEach(playerRef -> playerRef.sendMessage(Message.raw("Memory pressure can be caused by fast exploration and similar activities. View radius will recover over time if memory usage allows.")));
                    } else {
                        Universe.get().getPlayers().forEach(playerRef -> playerRef.sendMessage(Message.raw("Memory pressure can be caused by fast exploration and similar activities. View radius will recover over time if memory usage allows.")));
                    }
                }
                getLogger().atWarning().log("Memory critical. Reducing view radius to " + newViewRadius + " chunks.");
            }
        }

        if (tpsViewRadiusResult == ViewRadiusResult.DECREASE) {
            var newViewRadius = this.reduceViewRadius(currentViewRadius);

            if (newViewRadius != currentViewRadius) {
                if (this.config.getViewRadiusConfig().isRequireNotifyPermission()) {
                    Universe.get().getPlayers().stream()
                            .filter(playerRef -> PermissionsModule.get().hasPermission(playerRef.getUuid(), Permissions.NOTIFY_DECREASE))
                            .forEach(playerRef -> playerRef.sendMessage(Message.raw("TPS low. Reducing view radius to " + newViewRadius + " chunks.")));
                } else {
                    Universe.get().getPlayers().forEach(playerRef -> playerRef.sendMessage(Message.raw("TPS low. Reducing view radius to " + newViewRadius + " chunks.")));
                }
                if (currentViewRadius == this.initialViewRadius) {
                    if (this.config.getViewRadiusConfig().isRequireNotifyPermission()) {
                        Universe.get().getPlayers().stream()
                                .filter(playerRef -> PermissionsModule.get().hasPermission(playerRef.getUuid(), Permissions.NOTIFY_DECREASE))
                                .forEach(playerRef -> playerRef.sendMessage(Message.raw("Low TPS can be caused by chunk generation and large amounts of active NPCs. View radius will recover when load decreases.")));
                    } else {
                        Universe.get().getPlayers().forEach(playerRef -> playerRef.sendMessage(Message.raw("Low TPS can be caused by chunk generation and large amounts of active NPCs. View radius will recover when load decreases.")));
                    }
                }
                getLogger().atWarning().log("TPS low. Reducing view radius to " + newViewRadius + " chunks.");
            }
        }

        if (lastAdjustmentDeltaNanos > this.config.getViewRadiusConfig().getRecoveryWaitTime().toNanos()
                && gcViewRadiusResult == ViewRadiusResult.INCREASE
                && tpsViewRadiusResult == ViewRadiusResult.INCREASE) {
            this.increaseViewRadius(currentViewRadius);
        }
    }

    protected void checkChunks() {

        Universe.get().getDefaultWorld().getWorldConfig().setSaveNewChunks(true);

        var hasRun = this.chunkGarbageCollector.execute();

        if (hasRun) {
            this.lastAdjustmentNanos = System.nanoTime();
        }
    }

    protected void checkTps() {
        var changed = this.tpsAdjuster.execute();
        if (changed) {
            this.lastAdjustmentNanos = System.nanoTime();
        }
    }

    protected int reduceViewRadius(int currentViewRadius) {
        var minViewRadius = this.config.getViewRadiusConfig().getMinViewRadius();
        var factor = this.config.getViewRadiusConfig().getDecreaseFactor();


        var newViewRadius = (int) Math.max(minViewRadius, Math.floor(factor * currentViewRadius));

        if (newViewRadius >= currentViewRadius) {
            return currentViewRadius;
        }

        this.lastAdjustmentNanos = System.nanoTime();
        HytaleServer.get().getConfig().setMaxViewRadius(newViewRadius);
        return newViewRadius;
    }

    protected int increaseViewRadius(int currentViewRadius) {
        var newViewRadius = Math.min(currentViewRadius + this.config.getViewRadiusConfig().getIncreaseValue(), this.initialViewRadius);

        if (newViewRadius > currentViewRadius) {
            if (this.config.getViewRadiusConfig().isRequireNotifyPermission()) {
                Universe.get().getPlayers().stream()
                        .filter(playerRef -> PermissionsModule.get().hasPermission(playerRef.getUuid(), Permissions.NOTIFY_INCREASE))
                        .forEach(playerRef -> playerRef.sendMessage(Message.raw("Increasing view radius back to " + newViewRadius + " chunks.")));
            } else {
                Universe.get().getPlayers().forEach(playerRef -> playerRef.sendMessage(Message.raw("Increasing view radius back to " + newViewRadius + " chunks.")));
            }
            getLogger().atInfo().log("Increasing view radius back to " + newViewRadius + " chunks.");
            this.lastAdjustmentNanos = System.nanoTime();
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
            this.gcMonitor.stop();
            this.tpsMonitor.stop();

        } catch (Exception e) {
            getLogger().atSevere().log("failed stopping monitors: %s", e.getMessage());
        }

        if (viewRadiusTask != null) {
            this.viewRadiusTask.cancel(false);
        }

        if (chunkTask != null) {
            this.chunkTask.cancel(false);
        }

        if (tpsTask != null) {
            this.tpsTask.cancel(false);
        }

        this.tpsAdjuster.restore();
    }
}
