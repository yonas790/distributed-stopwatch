package shared;

import java.io.Serializable;

/**
 * Message types and structure for TCP communication between ClockClient and ClockServer.
 *
 * Commands:
 *   START        - Client requests server to start the stopwatch
 *   STOP         - Client requests server to stop the stopwatch
 *   RESET        - Client requests server to reset the stopwatch
 *   SYNC         - Client requests a clock sync response (Cristian's Algorithm)
 *   STATUS_UPDATE - Server broadcasts the current elapsed time and running state
 */
public class ClockSyncProtocol implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Command {
        START, STOP, RESET, SYNC, STATUS_UPDATE
    }

    private final Command command;
    private final long timestamp;     // Wall-clock time of the sender (for offset calculation)
    private final long stopwatchTime; // Current elapsed stopwatch time in millis
    private final boolean running;    // Whether the stopwatch is currently running

    public ClockSyncProtocol(Command command, long stopwatchTime, long timestamp, boolean running) {
        this.command = command;
        this.stopwatchTime = stopwatchTime;
        this.timestamp = timestamp;
        this.running = running;
    }

    // Convenience constructor for commands that don't carry running state
    public ClockSyncProtocol(Command command, long stopwatchTime, long timestamp) {
        this(command, stopwatchTime, timestamp, false);
    }

    public Command getCommand() { return command; }
    public long getStopwatchTime() { return stopwatchTime; }
    public long getTimestamp() { return timestamp; }
    public boolean isRunning() { return running; }

    @Override
    public String toString() {
        return "ClockSyncProtocol{cmd=" + command
                + ", time=" + stopwatchTime
                + ", ts=" + timestamp
                + ", running=" + running + "}";
    }
}
