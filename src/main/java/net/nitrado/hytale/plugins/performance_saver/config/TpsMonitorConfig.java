package net.nitrado.hytale.plugins.performance_saver.config;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

import java.time.Duration;
import java.util.Set;

public class TpsMonitorConfig {
    public static final BuilderCodec<TpsMonitorConfig> CODEC = BuilderCodec.builder(TpsMonitorConfig.class, TpsMonitorConfig::new)
            .append(
                    new KeyedCodec<>("Enabled", Codec.BOOLEAN),
                    (config, value) -> config.enabled = value,
                    config -> config.enabled
            ).add()
            .append(
                    new KeyedCodec<>("TpsWaterMarkHigh", Codec.DOUBLE),
                    (config, value) -> config.tpsWaterMarkHigh = value,
                    config -> config.tpsWaterMarkHigh
            ).add()
            .append(
                    new KeyedCodec<>("TpsWaterMarkLow", Codec.DOUBLE),
                    (config, value) -> config.tpsWaterMarkLow = value,
                    config -> config.tpsWaterMarkLow
            ).add()
            .append(
                    new KeyedCodec<>("OnlyWorlds", Codec.STRING_ARRAY),
                    (config, value) -> config.onlyWorlds = value,
                    config -> config.onlyWorlds
            ).add()
            .append(
                    new KeyedCodec<>("AdjustmentDelaySeconds", Codec.DURATION_SECONDS),
                    (config, value) -> config.adjustmentDelay = value,
                    config -> config.adjustmentDelay
            ).add()
            .build();

    public static final String DEFAULT_WORLD = "__DEFAULT";

    private static final double MIN_WATER_MARK = 0.01;
    private static final double MAX_WATER_MARK = 1.0;
    private static final Duration MIN_DURATION = Duration.ofSeconds(1);

    private boolean enabled = true;
    private double tpsWaterMarkHigh = 0.75;
    private double tpsWaterMarkLow = 0.6;
    private String[] onlyWorlds = new String[0];
    private Duration adjustmentDelay = Duration.ofSeconds(20);

    public boolean isEnabled() {
        return enabled;
    }

    public double getTpsWaterMarkHigh() {
        var clampedHigh = Math.clamp(tpsWaterMarkHigh, MIN_WATER_MARK, MAX_WATER_MARK);
        var clampedLow = Math.clamp(tpsWaterMarkLow, MIN_WATER_MARK, MAX_WATER_MARK);
        // Ensure high >= low
        return Math.max(clampedHigh, clampedLow);
    }

    public double getTpsWaterMarkLow() {
        var clampedHigh = Math.clamp(tpsWaterMarkHigh, MIN_WATER_MARK, MAX_WATER_MARK);
        var clampedLow = Math.clamp(tpsWaterMarkLow, MIN_WATER_MARK, MAX_WATER_MARK);
        // Ensure low <= high
        return Math.min(clampedLow, clampedHigh);
    }

    public Duration getAdjustmentDelay() {
        return adjustmentDelay.compareTo(MIN_DURATION) < 0 ? MIN_DURATION : adjustmentDelay;
    }

    public Set<String> getOnlyWorlds() {
        return Set.of(onlyWorlds);
    }
}