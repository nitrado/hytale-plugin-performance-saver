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

    private static final int MIN_CHUNK_COUNT = 1;
    private static final double MIN_DROP_RATIO = 0.01;
    private static final double MAX_DROP_RATIO = 1.00;
    private static final Duration MIN_DURATION = Duration.ofSeconds(1);

    private boolean enabled = true;
    private int minChunkCount = 128;
    private double chunkDropRatioThreshold = 0.8;
    private Duration garbageCollectionDelay = Duration.ofMinutes(5);
    private Duration checkInterval = Duration.ofSeconds(5);
    private Duration initialDelay = Duration.ofSeconds(5);

    public boolean isEnabled() {
        return enabled;
    }

    public int getMinChunkCount() {
        return Math.max(minChunkCount, MIN_CHUNK_COUNT);
    }

    public double getChunkDropRatioThreshold() {
        return Math.clamp(chunkDropRatioThreshold, MIN_DROP_RATIO, MAX_DROP_RATIO);
    }

    public Duration getGarbageCollectionDelay() {
        return garbageCollectionDelay.compareTo(Duration.ZERO) <= 0 ? MIN_DURATION : garbageCollectionDelay;
    }

    public Duration getCheckInterval() {
        return checkInterval.compareTo(MIN_DURATION) < 0 ? MIN_DURATION : checkInterval;
    }

    public Duration getInitialDelay() {
        return initialDelay.compareTo(Duration.ZERO) <= 0 ? MIN_DURATION : initialDelay;
    }
}
