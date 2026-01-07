package net.nitrado.hytale.plugins.performance_saver.config;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

public class PerformanceSaverPluginConfig {
    public static final BuilderCodec<PerformanceSaverPluginConfig> CODEC = BuilderCodec.builder(PerformanceSaverPluginConfig.class, PerformanceSaverPluginConfig::new)
            .append(
                    new KeyedCodec<>("ViewRadius", ViewRadiusConfig.CODEC),
                    (config, value) -> config.viewRadiusConfig = value,
                    config -> config.viewRadiusConfig
            ).add()
            .append(
                    new KeyedCodec<>("ChunkGarbageCollection", ChunkGarbageCollectorConfig.CODEC),
                    (config, value) -> config.chunkGarbageCollectorConfig = value,
                    config -> config.chunkGarbageCollectorConfig
            ).add()
            .append(
                    new KeyedCodec<>("Tps", TpsAdjusterConfig.CODEC),
                    (config, value) -> config.tpsAdjusterConfig = value,
                    config -> config.tpsAdjusterConfig
            ).add()
            .build();

    private ViewRadiusConfig viewRadiusConfig = new ViewRadiusConfig();
    private ChunkGarbageCollectorConfig chunkGarbageCollectorConfig = new ChunkGarbageCollectorConfig();
    private TpsAdjusterConfig tpsAdjusterConfig = new  TpsAdjusterConfig();

    public ViewRadiusConfig getViewRadiusConfig() {
        return viewRadiusConfig;
    }

    public ChunkGarbageCollectorConfig getChunkGarbageCollectorConfig() {
        return chunkGarbageCollectorConfig;
    }

    public TpsAdjusterConfig getTpsAdjusterConfig() {
        return tpsAdjusterConfig;
    }
}