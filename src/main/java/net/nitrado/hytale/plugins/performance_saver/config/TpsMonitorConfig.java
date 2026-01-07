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
                    new KeyedCodec<>("WindowSeconds", Codec.DURATION_SECONDS),
                    (config, value) -> config.window = value,
                    config -> config.window
            ).add()
            .append(
                    new KeyedCodec<>("AdjustmentDelaySeconds", Codec.DURATION_SECONDS),
                    (config, value) -> config.adjustmentDelay = value,
                    config -> config.adjustmentDelay
            ).add()
            .append(
                    new KeyedCodec<>("RecoveryWaitTimeSeconds", Codec.DURATION_SECONDS),
                    (config, value) -> config.recoveryWaitTime = value,
                    config -> config.recoveryWaitTime
            ).add()
            .build();

    public static final String DEFAULT_WORLD = "__DEFAULT";

    private boolean enabled = true;
    private double tpsWaterMarkHigh = 0.75;
    private double tpsWaterMarkLow = 0.6;
    private String[] onlyWorlds = new String[0];
    private Duration window = Duration.ofSeconds(15);
    private Duration adjustmentDelay = Duration.ofSeconds(30);
    private Duration recoveryWaitTime = Duration.ofSeconds(60);

    public boolean isEnabled() {
        return enabled;
    }

    public double getTpsWaterMarkHigh() {
        return tpsWaterMarkHigh;
    }

    public double getTpsWaterMarkLow() {
        return tpsWaterMarkLow;
    }

    public Duration getWindow() {
        return window;
    }

    public Duration getAdjustmentDelay() {
        return adjustmentDelay;
    }

    public Duration getRecoveryWaitTime() {
        return recoveryWaitTime;
    }

    public Set<String> getOnlyWorlds() {
        return Set.of(onlyWorlds);
    }
}