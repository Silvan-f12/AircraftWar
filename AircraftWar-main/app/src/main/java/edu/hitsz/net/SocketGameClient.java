package edu.hitsz.net;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SocketGameClient {
    public interface Listener {
        void onConnected();

        void onWaiting();

        void onMatched(int playerIndex);

        void onState(int score1, int score2, boolean dead1, boolean dead2);

        void onSettle(int score1, int score2);

        void onOpponentLeft(int playerIndex);

        void onError(String message);

        void onDisconnected();
    }

    private final Listener listener;
    private final ExecutorService readExecutor = Executors.newSingleThreadExecutor();
    private final ExecutorService writeExecutor = Executors.newSingleThreadExecutor();

    private Socket socket;
    private PrintWriter writer;
    private volatile boolean running;

    public SocketGameClient(Listener listener) {
        this.listener = listener;
    }

    public void connect(String host, int port, String playerName) {
        readExecutor.execute(() -> doConnect(host, port, playerName));
    }

    public void sendScore(int score) {
        sendLine("SCORE|" + score);
    }

    public void sendDead(int score) {
        sendLine("DEAD|" + score);
    }

    public void close() {
        running = false;
        sendLine("QUIT");
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException ignore) {
        }
    }

    private void doConnect(String host, int port, String playerName) {
        close();
        try {
            socket = new Socket(host, port);
            writer = new PrintWriter(socket.getOutputStream(), true);
            running = true;
            listener.onConnected();
            writer.println("HELLO|" + playerName);

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                String line;
                while (running && (line = reader.readLine()) != null) {
                    handleServerMessage(line);
                }
            }
        } catch (IOException e) {
            listener.onError("连接失败: " + e.getMessage());
        } finally {
            running = false;
            synchronized (this) {
                writer = null;
                socket = null;
            }
            listener.onDisconnected();
        }
    }

    private void sendLine(String line) {
        writeExecutor.execute(() -> {
            synchronized (this) {
                if (writer != null) {
                    writer.println(line);
                }
            }
        });
    }

    private void handleServerMessage(String message) {
        if (message == null || message.isBlank()) {
            return;
        }
        String[] parts = message.split("\\|");
        String type = parts[0];
        switch (type) {
            case "WAITING":
                listener.onWaiting();
                break;
            case "MATCHED":
                if (parts.length >= 2) {
                    listener.onMatched(parseInt(parts[1]));
                }
                break;
            case "STATE":
                if (parts.length >= 5) {
                    listener.onState(
                            parseInt(parts[1]),
                            parseInt(parts[2]),
                            parseBoolean(parts[3]),
                            parseBoolean(parts[4])
                    );
                }
                break;
            case "SETTLE":
                if (parts.length >= 3) {
                    listener.onSettle(parseInt(parts[1]), parseInt(parts[2]));
                }
                break;
            case "OPPONENT_LEFT":
                if (parts.length >= 2) {
                    listener.onOpponentLeft(parseInt(parts[1]));
                }
                break;
            case "ERROR":
                listener.onError(message);
                break;
            default:
                break;
        }
    }

    private int parseInt(String raw) {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private boolean parseBoolean(String raw) {
        return "true".equalsIgnoreCase(raw);
    }
}

