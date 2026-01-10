package net.nitrado.hytale.plugins.performance_saver.chunks;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.universe.Universe;
import net.nitrado.hytale.plugins.performance_saver.config.ChunkGarbageCollectorConfig;

public class ChunkGarbageCollector {

    private HytaleLogger logger;
    private ChunkGarbageCollectorConfig config;

    private int maxObservedChunks = 0;
    private long belowThresholdSince = 0;

    public ChunkGarbageCollector(HytaleLogger logger, ChunkGarbageCollectorConfig config) {
        this.logger = logger;
        this.config = config;
    }

    public boolean execute() {
        var now = System.nanoTime();
        var activeChunks = this.getTotalActiveChunks();

        if (activeChunks > this.maxObservedChunks) {
            this.maxObservedChunks = activeChunks;
            return false;
        }

        if (this.maxObservedChunks < this.config.getMinChunkCount()) {
            return false;
        }

        var chunkDropThreshold = this.maxObservedChunks * (1 - this.config.getChunkDropRatioThreshold());
        if (activeChunks >= chunkDropThreshold) {
            this.belowThresholdSince = 0;
            return false;
        }

        if (this.belowThresholdSince == 0) {
            this.belowThresholdSince = now;
            return false;
        }

        if (now - this.belowThresholdSince < this.config.getGarbageCollectionDelay().toNanos()) {
            return false;
        }

        this.maxObservedChunks = activeChunks;
        this.belowThresholdSince = 0;
        this.logger.atInfo().log("Triggering garbage collection");
        System.gc();

        return true;
    }

    private int getTotalActiveChunks() {
        var activeChunks = 0;
        for (var entry : Universe.get().getWorlds().entrySet()) {
            activeChunks += entry.getValue().getChunkStore().getLoadedChunksCount();
        };

        return activeChunks;
    }
}
