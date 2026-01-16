package net.nitrado.hytale.plugins.performance_saver.config;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.server.core.HytaleServer;

import java.time.Duration;

public class RestoreConfig {
    public static final BuilderCodec<RestoreConfig> CODEC = BuilderCodec.builder(RestoreConfig.class, RestoreConfig::new)
            .append(
                    new KeyedCodec<>("InitialViewRadius", Codec.INTEGER),
                    (config, value) -> config.initialViewRadius = value,
                    config -> config.initialViewRadius
            ).add()
            .build();

    private int initialViewRadius = HytaleServer.get().getConfig().getMaxViewRadius();

    public int getInitialViewRadius() {
        return initialViewRadius;
    }
}
