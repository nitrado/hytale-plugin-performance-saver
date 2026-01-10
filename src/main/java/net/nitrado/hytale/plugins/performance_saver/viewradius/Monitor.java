package net.nitrado.hytale.plugins.performance_saver.viewradius;

public interface Monitor {
    ViewRadiusResult getViewRadiusChange(long lastAdjustmentDeltaNanos);
    void start() throws Exception;
    void stop() throws Exception;
}
