package net.nitrado.hytale.plugins.performance_saver.viewradius;

public interface Monitor {
    ViewRadiusResult getViewRadiusChange(long lastAdjustment);
    void start() throws Exception;
    void stop() throws Exception;
}
