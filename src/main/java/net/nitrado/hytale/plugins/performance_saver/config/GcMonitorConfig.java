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
            .build();

    private static final double MIN_HEAP_RATIO = 0.01;
    private static final double MAX_HEAP_RATIO = 0.99;
    private static final int MIN_TRIGGER_SEQUENCE = 1;
    private static final Duration MIN_WINDOW = Duration.ofSeconds(1);

    private boolean enabled = true;
    private double heapThresholdRatio = 0.85;
    private int triggerSequenceLength = 3;
    private Duration window = Duration.ofSeconds(60);

    public boolean isEnabled() {
        return enabled;
    }

    public double getHeapThresholdRatio() {
        return Math.clamp(heapThresholdRatio, MIN_HEAP_RATIO, MAX_HEAP_RATIO);
    }

    public int getTriggerSequenceLength() {
        return Math.max(triggerSequenceLength, MIN_TRIGGER_SEQUENCE);
    }

    public Duration getWindow() {
        return window.compareTo(MIN_WINDOW) < 0 ? MIN_WINDOW : window;
    }
}