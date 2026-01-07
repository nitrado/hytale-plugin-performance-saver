package net.nitrado.hytale.plugins.performance_saver.config;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

import java.time.Duration;

public class ChunkGarbageCollectorConfig {
    public static final BuilderCodec<ChunkGarbageCollectorConfig> CODEC = BuilderCodec.builder(ChunkGarbageCollectorConfig.class, ChunkGarbageCollectorConfig::new)
            .append(
                    new KeyedCodec<>("Enabled", Codec.BOOLEAN),
                    (config, value) -> config.enabled = value,
                    config -> config.enabled
            ).add()
            .append(
                    new KeyedCodec<>("ChunkDropRatioThreshold", Codec.DOUBLE),
                    (config, value) -> config.chunkDropRatioThreshold = value,
                    config -> config.chunkDropRatioThreshold
            ).add()
            .append(
                    new KeyedCodec<>("MinChunkCount", Codec.INTEGER),
                    (config, value) -> config.minChunkCount = value,
                    config -> config.minChunkCount
            ).add()
            .append(
                    new KeyedCodec<>("GarbageCollectionDelaySeconds", Codec.DURATION_SECONDS),
                    (config, value) -> config.garbageCollectionDelay = value,
                    config -> config.garbageCollectionDelay
            ).add()
            .append(
                    new KeyedCodec<>("InitialDelaySeconds", Codec.DURATION_SECONDS),
                    (config, value) -> config.initialDelay = value,
                    config -> config.initialDelay
            ).add()
            .append(
                    new KeyedCodec<>("CheckIntervalSeconds", Codec.DURATION_SECONDS),
                    (config, value) -> config.checkInterval = value,
                    config -> config.checkInterval
            ).add()
            .build();

    private boolean enabled = true;
    private int minChunkCount = 128;
    private double chunkDropRatioThreshold = 0.8;
    private Duration garbageCollectionDelay = Duration.ofMinutes(5);
    private Duration checkInterval = Duration.ofSeconds(5);
    private Duration initialDelay = Duration.ofSeconds(5);

    public boolean isEnabled() {
        return enabled;
    }

    public int  getMinChunkCount() {
        return minChunkCount;
    }

    public double getChunkDropRatioThreshold() {
        return chunkDropRatioThreshold;
    }

    public Duration getGarbageCollectionDelay() {
        return garbageCollectionDelay;
    }

    public Duration getCheckInterval() {
        return checkInterval;
    }

    public Duration getInitialDelay() {
        return initialDelay;
    }
}
