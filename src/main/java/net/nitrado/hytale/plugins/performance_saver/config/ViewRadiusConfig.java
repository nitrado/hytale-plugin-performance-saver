package net.nitrado.hytale.plugins.performance_saver.config;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

import java.time.Duration;

public class ViewRadiusConfig {
    public static final BuilderCodec<ViewRadiusConfig> CODEC = BuilderCodec.builder(ViewRadiusConfig.class, ViewRadiusConfig::new)
            .append(
                    new KeyedCodec<>("Enabled", Codec.BOOLEAN),
                    (config, value) -> config.enabled = value,
                    config -> config.enabled
            ).add()
            .append(
                    new KeyedCodec<>("MinViewRadius", Codec.INTEGER),
                    (config, value) -> config.minViewRadius = value,
                    config -> config.minViewRadius
            ).add()
            .append(
                    new KeyedCodec<>("DecreaseFactor", Codec.DOUBLE),
                    (config, value) -> config.decreaseFactor = value,
                    config -> config.decreaseFactor
            ).add()
            .append(
                    new KeyedCodec<>("IncreaseValue", Codec.INTEGER),
                    (config, value) -> config.increaseValue = value,
                    config -> config.increaseValue
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
            .append(
                    new KeyedCodec<>("RecoveryWaitTimeSeconds", Codec.DURATION_SECONDS),
                    (config, value) -> config.recoveryWaitTime = value,
                    config -> config.recoveryWaitTime
            ).add()
            .append(
                    new KeyedCodec<>("GcMonitor", GcMonitorConfig.CODEC),
                    (config, value) -> config.gcMonitorConfig = value,
                    config -> config.gcMonitorConfig
            ).add()
            .append(
                    new KeyedCodec<>("TpsMonitor", TpsMonitorConfig.CODEC),
                    (config, value) -> config.tpsMonitorConfig = value,
                    config -> config.tpsMonitorConfig
            ).add()
            .build();

    private static final int MIN_VIEW_RADIUS = 1;
    private static final int MAX_VIEW_RADIUS = 64;
    private static final double MIN_DECREASE_FACTOR = 0.1;
    private static final double MAX_DECREASE_FACTOR = 0.99;
    private static final int MIN_INCREASE_VALUE = 1;
    private static final Duration MIN_DURATION = Duration.ofSeconds(1);

    private boolean enabled = true;
    private int minViewRadius = 2;
    private double decreaseFactor = 0.75;
    private int increaseValue = 1;
    private Duration initialDelay = Duration.ofSeconds(30);
    private Duration checkInterval = Duration.ofSeconds(5);
    private Duration recoveryWaitTime = Duration.ofSeconds(60);
    private GcMonitorConfig gcMonitorConfig = new GcMonitorConfig();
    private TpsMonitorConfig tpsMonitorConfig = new TpsMonitorConfig();

    public boolean isEnabled() {
        return enabled;
    }

    public int getMinViewRadius() {
        return Math.clamp(minViewRadius, MIN_VIEW_RADIUS, MAX_VIEW_RADIUS);
    }

    public double getDecreaseFactor() {
        return Math.clamp(decreaseFactor, MIN_DECREASE_FACTOR, MAX_DECREASE_FACTOR);
    }

    public int getIncreaseValue() {
        return Math.max(increaseValue, MIN_INCREASE_VALUE);
    }

    public Duration getInitialDelay() {
        return initialDelay.compareTo(Duration.ZERO) <= 0 ? MIN_DURATION : initialDelay;
    }

    public Duration getCheckInterval() {
        return checkInterval.compareTo(MIN_DURATION) < 0 ? MIN_DURATION : checkInterval;
    }

    public Duration getRecoveryWaitTime() {
        return recoveryWaitTime.compareTo(Duration.ZERO) <= 0 ? MIN_DURATION : recoveryWaitTime;
    }

    public GcMonitorConfig getGcMonitorConfig() {
        return gcMonitorConfig;
    }

    public TpsMonitorConfig getTpsMonitorConfig() {
        return tpsMonitorConfig;
    }
}