package server;

import shared.Clock;
import shared.ClockSyncProtocol;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

/**
 * Central coordination server for the Distributed Stopwatch.
 *
 * Concept Demonstrated: Clock Synchronization via a coordinator/server model.
 *   - The server holds the authoritative stopwatch state.
 *   - Any client can send START/STOP/RESET commands.
 *   - The server broadcasts the updated state to ALL connected clients,
 *     so every node sees the same time (simulating synchronized clocks).
 *   - New clients immediately receive current state upon connection (STATUS_UPDATE).
 *
 * Run this first, then launch one or more ClockClientGUI instances.
 */
public class ClockServer {
    private static final Logger LOGGER = Logger.getLogger(ClockServer.class.getName());
    public static final int PORT = 12345;

    private final Clock internalClock;
    private final List<ServerSyncProtocol> clients;

    public ClockServer() {
        this.internalClock = new Clock();
        this.clients = Collections.synchronizedList(new ArrayList<>());
    }

    public void start() {
        LOGGER.info("=== Distributed Stopwatch Server starting on port " + PORT + " ===");
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                LOGGER.info("New client connected: " + clientSocket.getInetAddress() + ":" + clientSocket.getPort());
                ServerSyncProtocol handler = new ServerSyncProtocol(clientSocket, this);
                clients.add(handler);
                Thread t = new Thread(handler);
                t.setDaemon(true);
                t.start();
            }
        } catch (IOException e) {
            LOGGER.severe("Server error: " + e.getMessage());
        }
    }

    // ---- Synchronized state-change methods ----

    public synchronized void startStopwatch() {
        if (!internalClock.isRunning()) {
            internalClock.start();
            broadcast(ClockSyncProtocol.Command.START);
            LOGGER.info("Stopwatch STARTED. Broadcasting to " + clients.size() + " clients.");
        }
    }

    public synchronized void stopStopwatch() {
        if (internalClock.isRunning()) {
            internalClock.stop();
            broadcast(ClockSyncProtocol.Command.STOP);
            LOGGER.info("Stopwatch STOPPED at " + Clock.formatTime(internalClock.getDisplayTime()));
        }
    }

    public synchronized void resetStopwatch() {
        internalClock.reset();
        broadcast(ClockSyncProtocol.Command.RESET);
        LOGGER.info("Stopwatch RESET. Broadcasting to " + clients.size() + " clients.");
    }

    public synchronized long getStopwatchTime() {
        return internalClock.getDisplayTime();
    }

    public synchronized boolean isRunning() {
        return internalClock.isRunning();
    }

    /**
     * Broadcast a command + current state to every connected client.
     * This is the key coordination step: one event → all nodes updated.
     */
    private void broadcast(ClockSyncProtocol.Command command) {
        ClockSyncProtocol msg = new ClockSyncProtocol(
                command,
                internalClock.getDisplayTime(),
                System.currentTimeMillis(),
                internalClock.isRunning()
        );
        synchronized (clients) {
            for (ServerSyncProtocol client : clients) {
                client.sendUpdate(msg);
            }
        }
    }

    public void removeClient(ServerSyncProtocol client) {
        clients.remove(client);
        LOGGER.info("Client removed. Active clients: " + clients.size());
    }

    public static void main(String[] args) {
        new ClockServer().start();
    }
}
