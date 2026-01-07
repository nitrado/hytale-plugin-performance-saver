package net.nitrado.hytale.plugins.performance_saver.config;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

import java.time.Duration;

public class GcMonitorConfig {
    public static final BuilderCodec<GcMonitorConfig> CODEC = BuilderCodec.builder(GcMonitorConfig.class, GcMonitorConfig::new)
            .append(
                    new KeyedCodec<>("Enabled", Codec.BOOLEAN),
                    (config, value) -> config.enabled = value,
                    config -> config.enabled
            ).add()
            .append(
                    new KeyedCodec<>("HeapThresholdRatio", Codec.DOUBLE),
                    (config, value) -> config.heapThresholdRatio = value,
                    config -> config.heapThresholdRatio
            ).add()
            .append(
                    new KeyedCodec<>("TriggerSequenceLength", Codec.INTEGER),
                    (config, value) -> config.triggerSequenceLength = value,
                    config -> config.triggerSequenceLength
            ).add()
            .append(
                    new KeyedCodec<>("WindowSeconds", Codec.DURATION_SECONDS),
                    (config, value) -> config.window = value,
                    config -> config.window
            ).add()
            .append(
                    new KeyedCodec<>("RecoveryWaitTimeSeconds", Codec.DURATION_SECONDS),
                    (config, value) -> config.recoveryWaitTime = value,
                    config -> config.recoveryWaitTime
            ).add()
            .build();

    private boolean enabled = true;
    private double heapThresholdRatio = 0.85;
    private int triggerSequenceLength = 3;
    private Duration window = Duration.ofSeconds(60);
    private Duration recoveryWaitTime = Duration.ofSeconds(60);

    public boolean isEnabled() {
        return enabled;
    }

    public double getHeapThresholdRatio() {
        return heapThresholdRatio;
    }

    public int getTriggerSequenceLength() {
        return triggerSequenceLength;
    }

    public Duration getWindow() {
        return window;
    }

    public Duration getRecoveryWaitTime() {
        return recoveryWaitTime;
    }
}