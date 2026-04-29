package client;

import shared.Clock;
import shared.ClockSyncProtocol;

import java.io.*;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Client-side networking logic for the Distributed Stopwatch.
 *
 * Concept Demonstrated: Cristian's Algorithm (simplified)
 *   When a STATUS_UPDATE or SYNC arrives the client reconciles its local
 *   clock with the server's authoritative time.  For full Cristian's:
 *       offset = serverTimestamp + (RTT / 2) - localTime
 *   Here RTT is approximated as the round-trip measured at connect time,
 *   keeping the demo simple while showing the principle.
 *
 * The GUI calls connect() then sendCommand() to interact.
 * Server updates arrive asynchronously via listenToServer().
 */
public class ClockClient {
    private static final Logger LOGGER = Logger.getLogger(ClockClient.class.getName());

    private final String host;
    private final int port;
    private final Clock clock;
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private volatile boolean connected = false;

    /** Callback so the GUI can react to server updates without polling. */
    private Runnable onUpdate;

    public ClockClient(String host, int port) {
        this.host = host;
        this.port = port;
        this.clock = new Clock();
    }

    public void setOnUpdate(Runnable onUpdate) {
        this.onUpdate = onUpdate;
    }

    public void connect() {
        try {
            LOGGER.info("Connecting to " + host + ":" + port + " ...");
            socket = new Socket(host, port);

            // ObjectOutputStream first to avoid stream header deadlock
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());
            connected = true;

            Thread listener = new Thread(this::listenToServer);
            listener.setDaemon(true);
            listener.start();

            LOGGER.info("Connected to server.");
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Could not connect to server at " + host + ":" + port, e);
        }
    }

    private void listenToServer() {
        try {
            while (connected) {
                ClockSyncProtocol update = (ClockSyncProtocol) in.readObject();
                handleServerUpdate(update);
            }
        } catch (EOFException | java.net.SocketException e) {
            LOGGER.info("Server connection closed.");
        } catch (IOException | ClassNotFoundException e) {
            LOGGER.log(Level.WARNING, "Error reading from server", e);
        } finally {
            connected = false;
            if (onUpdate != null) onUpdate.run();
        }
    }

    private void handleServerUpdate(ClockSyncProtocol update) {
        LOGGER.info("Received: " + update);

        switch (update.getCommand()) {
            case START:
                clock.setElapsedTime(update.getStopwatchTime());
                clock.setRunning(true);
                break;

            case STOP:
                clock.setRunning(false);
                clock.setElapsedTime(update.getStopwatchTime());
                break;

            case RESET:
                clock.setRunning(false);
                clock.reset();
                break;

            case STATUS_UPDATE:
            case SYNC:
                /*
                 * Cristian's Algorithm (simplified):
                 *   The server embeds its wall-clock timestamp in the message.
                 *   We estimate one-way latency ≈ (receiveTime - serverTimestamp) / 2
                 *   and correct the stopwatch time accordingly.
                 *
                 *   For a broadcast STATUS_UPDATE the latency adjustment is small,
                 *   but the principle is demonstrated here.
                 */
                long receiveTime = System.currentTimeMillis();
                long estimatedLatency = Math.max(0, (receiveTime - update.getTimestamp()) / 2);
                long correctedTime = update.getStopwatchTime() + (update.isRunning() ? estimatedLatency : 0);

                clock.setElapsedTime(correctedTime);
                clock.setRunning(update.isRunning());

                LOGGER.info(String.format(
                        "Clock sync applied — server=%dms, latency~=%dms, corrected=%dms",
                        update.getStopwatchTime(), estimatedLatency, correctedTime));
                break;
        }

        if (onUpdate != null) onUpdate.run();
    }

    public void sendCommand(ClockSyncProtocol.Command command) {
        if (!connected) {
            LOGGER.warning("Not connected — cannot send " + command);
            return;
        }
        try {
            ClockSyncProtocol msg = new ClockSyncProtocol(
                    command,
                    clock.getDisplayTime(),
                    System.currentTimeMillis(),
                    clock.isRunning()
            );
            synchronized (this) {
                out.writeObject(msg);
                out.flush();
                out.reset();
            }
            LOGGER.info("Sent command: " + command);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to send command " + command, e);
        }
    }

    public Clock getClock() { return clock; }
    public boolean isConnected() { return connected; }
}
