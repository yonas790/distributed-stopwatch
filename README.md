# Distributed Stopwatch — Clock Synchronization
### Group 5 | Concepts: Clock Sync · Coordination · Socket Communication

---

## What This Project Does

A distributed stopwatch where **multiple client windows all show the same time**,
coordinated by a central server over TCP sockets.

- **Any client** can press Start / Stop / Reset
- The server broadcasts the new state to **all** connected clients instantly
- New clients that join mid-session immediately receive the current time (no manual sync needed)
- The GUI shows connection status and which sync algorithm is in use

---

## Concepts Demonstrated

| Concept | Where |
|---|---|
| **Clock Synchronization** | `ClockClient.handleServerUpdate()` — Cristian's Algorithm: adjusts for network latency using `offset = serverTime + RTT/2 - localTime` |
| **Coordinator Pattern** | `ClockServer` is the single source of truth; clients never talk to each other |
| **Socket Communication** | `ServerSyncProtocol` (server) & `ClockClient` (client) use Java `ObjectOutputStream` / `ObjectInputStream` over TCP |
| **Broadcast / Multicast** | `ClockServer.broadcast()` sends to all connected clients on every state change |
| **Thread Safety** | `Clock` methods are `synchronized`; client list is a `Collections.synchronizedList` |

---

## Project Structure

```
distributed-stopwatch/
├── shared/
│   ├── Clock.java              # Stopwatch logic + Cristian's offset
│   └── ClockSyncProtocol.java  # Serializable message (command + time + timestamp)
├── server/
│   ├── ClockServer.java        # Coordinator — accepts clients, holds authoritative clock
│   └── ServerSyncProtocol.java # Per-client TCP handler thread
└── client/
    ├── ClockClient.java        # Networking + sync logic
    └── ClockClientGUI.java     # Swing GUI (no JavaFX, no external libs)
```

---

## How to Run (plain Java — no Maven, no IntelliJ needed)

### Step 1 — Compile (once)

```bash
javac shared/*.java server/*.java client/*.java
```

### Step 2 — Start the Server

```bash
java server.ClockServer
```

You will see:
```
INFO: === Distributed Stopwatch Server starting on port 12345 ===
```

### Step 3 — Start one or more Clients

Open a **new terminal** for each client:

```bash
java client.ClockClientGUI
```

- Open 2–3 clients to see synchronization in action
- Press **Start** on one → all windows begin counting simultaneously
- Press **Stop** on another → all windows freeze at the same time
- Press **Reset** on any → all windows go back to 00:00:00

---

## Synchronization Protocol Messages

| Command | Direction | Meaning |
|---|---|---|
| `START` | Client → Server → All Clients | Begin the stopwatch |
| `STOP` | Client → Server → All Clients | Pause the stopwatch |
| `RESET` | Client → Server → All Clients | Reset to zero |
| `SYNC` | Client → Server → Client | Request time correction (Cristian's) |
| `STATUS_UPDATE` | Server → new Client | Initial state sent on connect |

---

## Requirements

- Java JDK 11 or higher (JRE alone is NOT enough — you need `javac`)
- No other dependencies