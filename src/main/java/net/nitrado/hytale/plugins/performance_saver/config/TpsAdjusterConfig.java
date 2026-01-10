package net.nitrado.hytale.plugins.performance_saver.config;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.server.core.util.thread.TickingThread;

import java.time.Duration;
import java.util.Set;

public class TpsAdjusterConfig {
    public static final BuilderCodec<TpsAdjusterConfig> CODEC = BuilderCodec.builder(TpsAdjusterConfig.class, TpsAdjusterConfig::new)
            .append(
                    new KeyedCodec<>("Enabled", Codec.BOOLEAN),
                    (config, value) -> config.enabled = value,
                    config -> config.enabled
            ).add()
            .append(
                    new KeyedCodec<>("TpsLimit", Codec.INTEGER),
                    (config, value) -> config.tpsLimit = value,
                    config -> config.tpsLimit
            ).add()
            .append(
                    new KeyedCodec<>("TpsLimitEmpty", Codec.INTEGER),
                    (config, value) -> config.tpsLimitEmpty = value,
                    config -> config.tpsLimitEmpty
            ).add()
            .append(
                    new KeyedCodec<>("OnlyWorlds", Codec.STRING_ARRAY),
                    (config, value) -> config.onlyWorlds = value,
                    config -> config.onlyWorlds
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
                    new KeyedCodec<>("EmptyLimitDelaySeconds", Codec.DURATION_SECONDS),
                    (config, value) -> config.emptyLimitDelay = value,
                    config -> config.emptyLimitDelay
            ).add()
            .build();

    public static final String DEFAULT_WORLD = "__DEFAULT";

    private static final int MIN_TPS = 1;
    private static final int MAX_TPS = TickingThread.TPS;
    private static final Duration MIN_DURATION = Duration.ofSeconds(1);

    private boolean enabled = true;
    private int tpsLimit = 20;
    private int tpsLimitEmpty = 5;
    private String[] onlyWorlds = new String[0];
    private Duration initialDelay = Duration.ofSeconds(30);
    private Duration checkInterval = Duration.ofSeconds(5);
    private Duration emptyLimitDelay = Duration.ofMinutes(5);

    public boolean isEnabled() {
        return enabled;
    }

    public int getTpsLimit() {
        return Math.clamp(tpsLimit, MIN_TPS, MAX_TPS);
    }

    public int getTpsLimitEmpty() {
        return Math.clamp(Math.min(tpsLimit, tpsLimitEmpty), MIN_TPS, MAX_TPS);
    }

    public Set<String> getOnlyWorlds() {
        return Set.of(onlyWorlds);
    }

    public Duration getInitialDelay() {
        return initialDelay.compareTo(Duration.ZERO) <= 0 ? MIN_DURATION : initialDelay;
    }

    public Duration getCheckInterval() {
        return checkInterval.compareTo(MIN_DURATION) <= 0 ? MIN_DURATION : checkInterval;
    }

    public Duration getEmptyLimitDelay() {
        return emptyLimitDelay.compareTo(Duration.ZERO) <= 0 ? MIN_DURATION : emptyLimitDelay;
    }
}