package net.nitrado.hytale.plugins.performance_saver.tps;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import net.nitrado.hytale.plugins.performance_saver.config.TpsAdjusterConfig;

import java.util.concurrent.CompletableFuture;

public class TpsAdjuster {

    private HytaleLogger logger;
    private TpsAdjusterConfig config;

    private long lastPlayerSeenAt = 0;

    public TpsAdjuster(HytaleLogger logger, TpsAdjusterConfig config) {
        this.logger = logger;
        this.config = config;
    }

    public boolean execute() {
        var now = System.nanoTime();

        var playerCount = Universe.get().getPlayerCount();
        if (playerCount > 0) {
            this.lastPlayerSeenAt = now;
        }

        var targetTps = this.config.getTpsLimit();
        if (now - lastPlayerSeenAt > this.config.getEmptyLimitDelay().toNanos()) {
            targetTps = this.config.getTpsLimitEmpty();
        }

        return this.setTps(targetTps);
    }

    private boolean setTps(int tps) {
        var onlyWorlds = this.config.getOnlyWorlds();

        if (onlyWorlds.contains(TpsAdjusterConfig.DEFAULT_WORLD)) {
            onlyWorlds.add(Universe.get().getDefaultWorld().getName());
        }

        var change = false;
        for (var entry : Universe.get().getWorlds().entrySet()) {
            if (!onlyWorlds.isEmpty() && !onlyWorlds.contains(entry.getKey())) {
                continue;
            }

            var world = entry.getValue();
            if (world.getTps() != tps) {
                change = true;

                this.logger.atInfo().log("Setting TPS of world %s to %d", world.getName(), tps);
                CompletableFuture.runAsync(() -> {
                    world.setTps(tps);
                }, world);
            }
        }

        return change;
    }
}
