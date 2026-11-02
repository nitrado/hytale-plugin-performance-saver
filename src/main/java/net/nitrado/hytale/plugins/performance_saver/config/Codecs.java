package net.nitrado.hytale.plugins.performance_saver.config;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.function.FunctionCodec;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Additional codecs not provided by {@link Codec} anymore.
 */
public final class Codecs {

    /**
     * Encodes/decodes a {@link Duration} as a plain number of seconds (e.g. {@code 5.0}),
     * matching the config file format previously produced by the upstream
     * {@code Codec.DURATION_SECONDS} constant, which has since been removed.
     */
    public static final FunctionCodec<Double, Duration> DURATION_SECONDS = new FunctionCodec<>(
            Codec.DOUBLE,
            v -> Duration.ofNanos((long) (v * (double) TimeUnit.SECONDS.toNanos(1))),
            v -> v == null ? null : Double.valueOf(v.toNanos() / (double) TimeUnit.SECONDS.toNanos(1))
    );

    private Codecs() {
    }
}
