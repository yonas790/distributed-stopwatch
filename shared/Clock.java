package shared;

/**
 * Utility class for managing stopwatch time and synchronization offsets.
 * Demonstrates clock state management used in distributed systems.
 */
public class Clock {
    private long startTime;
    private long elapsedTime;
    private boolean running;
    private long serverOffset; // T_server - T_local (Cristian's Algorithm offset)

    public Clock() {
        this.startTime = 0;
        this.elapsedTime = 0;
        this.running = false;
        this.serverOffset = 0;
    }

    public synchronized void start() {
        if (!running) {
            startTime = System.currentTimeMillis();
            running = true;
        }
    }

    public synchronized void stop() {
        if (running) {
            elapsedTime += System.currentTimeMillis() - startTime;
            running = false;
        }
    }

    public synchronized void reset() {
        elapsedTime = 0;
        running = false;
        startTime = 0;
    }

    public synchronized long getDisplayTime() {
        if (running) {
            return elapsedTime + (System.currentTimeMillis() - startTime);
        }
        return elapsedTime;
    }

    /**
     * Forcefully set elapsed time (used when server broadcasts a sync update).
     * If currently running, resets the local startTime so the count continues correctly.
     */
    public synchronized void setElapsedTime(long time) {
        this.elapsedTime = time;
        if (running) {
            this.startTime = System.currentTimeMillis();
        }
    }

    public synchronized void setRunning(boolean shouldRun) {
        if (shouldRun && !running) {
            start();
        } else if (!shouldRun && running) {
            stop();
        }
    }

    public void setServerOffset(long offset) {
        this.serverOffset = offset;
    }

    public long getServerOffset() {
        return serverOffset;
    }

    public boolean isRunning() {
        return running;
    }

    public static String formatTime(long millis) {
        long minutes = (millis / 1000) / 60;
        long seconds = (millis / 1000) % 60;
        long hundredths = (millis % 1000) / 10;
        return String.format("%02d:%02d:%02d", minutes, seconds, hundredths);
    }
}
