package server;

import shared.ClockSyncProtocol;

import java.io.*;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handles the TCP connection and synchronization protocol for a single connected client.
 * Each instance runs in its own thread.
 *
 * Concept: In a distributed system each node communicates independently with the
 * coordinator (server). This class IS that per-node handler on the server side.
 */
public class ServerSyncProtocol implements Runnable {
    private static final Logger LOGGER = Logger.getLogger(ServerSyncProtocol.class.getName());

    private final Socket clientSocket;
    private final ClockServer server;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private final String clientId;

    public ServerSyncProtocol(Socket socket, ClockServer server) {
        this.clientSocket = socket;
        this.server = server;
        this.clientId = socket.getInetAddress() + ":" + socket.getPort();
    }

    @Override
    public void run() {
        try {
            // ObjectOutputStream MUST be created before ObjectInputStream to avoid deadlock
            out = new ObjectOutputStream(clientSocket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(clientSocket.getInputStream());

            LOGGER.info("Handler ready for client " + clientId);

            // Send the current server state immediately so the new client syncs up
            sendUpdate(new ClockSyncProtocol(
                    ClockSyncProtocol.Command.STATUS_UPDATE,
                    server.getStopwatchTime(),
                    System.currentTimeMillis(),
                    server.isRunning()
            ));

            // Main read loop: process commands from this client
            while (!clientSocket.isClosed()) {
                ClockSyncProtocol request = (ClockSyncProtocol) in.readObject();
                LOGGER.info("From " + clientId + ": " + request.getCommand());
                handleRequest(request);
            }
        } catch (EOFException | java.net.SocketException e) {
            LOGGER.info("Client " + clientId + " disconnected.");
        } catch (IOException | ClassNotFoundException e) {
            LOGGER.log(Level.WARNING, "Error with client " + clientId, e);
        } finally {
            cleanup();
        }
    }

    private void handleRequest(ClockSyncProtocol request) {
        switch (request.getCommand()) {
            case START:
                server.startStopwatch();
                break;
            case STOP:
                server.stopStopwatch();
                break;
            case RESET:
                server.resetStopwatch();
                break;
            case SYNC:
                // Cristian's Algorithm: reply with current server time so the client
                // can compute:  offset = (serverTime + RTT/2) - localTime
                sendUpdate(new ClockSyncProtocol(
                        ClockSyncProtocol.Command.SYNC,
                        server.getStopwatchTime(),
                        System.currentTimeMillis(),
                        server.isRunning()
                ));
                break;
            default:
                break;
        }
    }

    public synchronized void sendUpdate(ClockSyncProtocol msg) {
        try {
            if (out != null) {
                out.writeObject(msg);
                out.flush();
                out.reset(); // prevent object caching issues with repeated sends
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to send to " + clientId + ": " + e.getMessage());
        }
    }

    private void cleanup() {
        server.removeClient(this);
        try {
            if (clientSocket != null && !clientSocket.isClosed()) {
                clientSocket.close();
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error closing socket for " + clientId);
        }
    }
}
