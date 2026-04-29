package client;

import shared.Clock;
import shared.ClockSyncProtocol;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class ClockClientGUI extends JFrame {

    // UI colours
    private static final Color BG           = new Color(30,  30,  40);
    private static final Color PANEL_BG     = new Color(40,  40,  55);
    private static final Color TIME_COLOR   = new Color(0,   230, 180);
    private static final Color BTN_START    = new Color(46,  204, 113);
    private static final Color BTN_STOP     = new Color(231, 76,  60);
    private static final Color BTN_RESET    = new Color(52,  152, 219);
    private static final Color BTN_TXT      = Color.WHITE;
    private static final Color STATUS_OK    = new Color(0,   200, 100);
    private static final Color STATUS_ERR   = new Color(231, 76,  60);
    private static final Color STATUS_WAIT  = new Color(200, 200, 200);

    // UI components
    private final JLabel timeLabel;
    private final JLabel statusLabel;
    private final JLabel syncLabel;
    private final JButton startBtn;
    private final JButton stopBtn;
    private final JButton resetBtn;

    // Networking
    private final ClockClient client;

    // Display refresh timer
    private final Timer displayTimer;

    public ClockClientGUI() {
        super("Distributed Stopwatch — Client");

        client = new ClockClient("localhost", ClockServer.PORT);

        // Time display
        timeLabel = new JLabel("00:00:00", SwingConstants.CENTER);
        timeLabel.setFont(new Font(Font.MONOSPACED, Font.BOLD, 60));
        timeLabel.setForeground(TIME_COLOR);
        timeLabel.setOpaque(false);

        // Status bar
        statusLabel = new JLabel("Connecting to server...", SwingConstants.CENTER);
        statusLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        statusLabel.setForeground(STATUS_WAIT);

        syncLabel = new JLabel(" ", SwingConstants.CENTER);
        syncLabel.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        syncLabel.setForeground(new Color(140, 140, 170));

        // Buttons
        startBtn = makeButton("▶  Start", BTN_START);
        stopBtn  = makeButton("⏹  Stop",  BTN_STOP);
        resetBtn = makeButton("↺  Reset", BTN_RESET);

        startBtn.addActionListener(e -> client.sendCommand(ClockSyncProtocol.Command.START));
        stopBtn .addActionListener(e -> client.sendCommand(ClockSyncProtocol.Command.STOP));
        resetBtn.addActionListener(e -> client.sendCommand(ClockSyncProtocol.Command.RESET));

        // Layout
        JPanel timePan = new JPanel(new BorderLayout());
        timePan.setBackground(PANEL_BG);
        timePan.setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));
        timePan.add(timeLabel, BorderLayout.CENTER);

        JPanel btnPan = new JPanel(new GridLayout(1, 3, 10, 0));
        btnPan.setBackground(BG);
        btnPan.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 20));
        btnPan.add(startBtn);
        btnPan.add(stopBtn);
        btnPan.add(resetBtn);

        JPanel statusPan = new JPanel(new GridLayout(2, 1, 0, 2));
        statusPan.setBackground(BG);
        statusPan.setBorder(BorderFactory.createEmptyBorder(8, 20, 10, 20));
        statusPan.add(statusLabel);
        statusPan.add(syncLabel);

        JPanel root = new JPanel(new BorderLayout(0, 12));
        root.setBackground(BG);
        root.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));
        root.add(timePan,   BorderLayout.CENTER);
        root.add(btnPan,    BorderLayout.SOUTH);

        getContentPane().setBackground(BG);
        getContentPane().add(root, BorderLayout.CENTER);
        getContentPane().add(statusPan, BorderLayout.SOUTH);

        // Window settings
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) {
                displayTimer.stop();
                dispose();
            }
        });
        setSize(480, 320);
        setMinimumSize(new Dimension(380, 260));
        setLocationRelativeTo(null);
        setVisible(true);

        // Connect & start refresh
        // Register callback so GUI refreshes instantly on server push
        client.setOnUpdate(() -> SwingUtilities.invokeLater(this::refresh));

        // Connect on a background thread so the GUI isn't blocked
        new Thread(() -> {
            client.connect();
            SwingUtilities.invokeLater(this::refresh);
        }).start();

        // Also poll every 50 ms to keep the running counter smooth
        displayTimer = new Timer(50, e -> refresh());
        displayTimer.start();
    }

    /** Refresh all labels to reflect current client state. */
    private void refresh() {
        timeLabel.setText(Clock.formatTime(client.getClock().getDisplayTime()));

        if (client.isConnected()) {
            statusLabel.setText("● Connected — clock synchronized with server");
            statusLabel.setForeground(STATUS_OK);
            syncLabel.setText("Synchronization: Cristian's Algorithm  |  Transport: TCP/ObjectStream");
        } else {
            statusLabel.setText("✖ Disconnected from server");
            statusLabel.setForeground(STATUS_ERR);
            syncLabel.setText("Retrying is not automatic — restart the client.");
        }
    }

    // Helper: create a styled button
    private JButton makeButton(String text, Color bg) {
        JButton b = new JButton(text);
        b.setBackground(bg);
        b.setForeground(BTN_TXT);
        b.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setOpaque(true);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setPreferredSize(new Dimension(120, 42));
        // Subtle hover effect
        b.addMouseListener(new java.awt.event.MouseAdapter() {
            final Color normal = bg;
            final Color hover  = bg.brighter();
            public void mouseEntered(java.awt.event.MouseEvent e) { b.setBackground(hover); }
            public void mouseExited (java.awt.event.MouseEvent e) { b.setBackground(normal); }
        });
        return b;
    }

    // Entry point
    public static void main(String[] args) {
        // Use the system look-and-feel for native window decorations
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
        catch (Exception ignored) {}

        SwingUtilities.invokeLater(ClockClientGUI::new);
    }

    // Inner reference to PORT constant — avoids a circular import
    private static final int ClockServer_PORT = 12345;
    // Alias for the inner usage above
    private static class ClockServer { static final int PORT = 12345; }
}
