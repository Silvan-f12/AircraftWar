package edu.hitsz.net;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SocketGameClient {
    // 网络层通过 Listener 把服务器事件回调给界面或游戏逻辑。
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
    // 读线程负责连接和持续接收服务器消息，避免阻塞 Android 主线程。
    private final ExecutorService readExecutor = Executors.newSingleThreadExecutor();
    // 写线程负责发送消息，保证发送顺序稳定。
    private final ExecutorService writeExecutor = Executors.newSingleThreadExecutor();

    private Socket socket;
    private PrintWriter writer;
    // running 用于控制读循环是否继续执行。
    private volatile boolean running;

    public SocketGameClient(Listener listener) {
        this.listener = listener;
    }

    public void connect(String host, int port, String playerName) {
        // Socket 连接不能放在主线程执行，否则 Android 会抛网络主线程异常。
        readExecutor.execute(() -> doConnect(host, port, playerName));
    }

    public void sendScore(int score) {
        // 向服务器上报当前分数，协议格式：SCORE|分数。
        sendLine("SCORE|" + score);
    }

    public void sendDead(int score) {
        // 向服务器上报死亡和最终分数，协议格式：DEAD|最终分数。
        sendLine("DEAD|" + score);
    }

    public void close() {
        // 主动退出时先发 QUIT，再关闭本地 Socket。
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
            // 建立 TCP Socket 长连接。
            socket = new Socket(host, port);
            // autoFlush=true，println 后会立即发送到服务器。
            writer = new PrintWriter(socket.getOutputStream(), true);
            running = true;
            listener.onConnected();
            // 握手消息：告诉服务器当前玩家准备进入匹配队列。
            writer.println("HELLO|" + playerName);

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                String line;
                // 持续读取服务器按行发送的消息，直到断开连接。
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
                    // 服务端使用 readLine() 接收，所以这里必须用 println() 发送换行。
                    writer.println(line);
                }
            }
        });
    }

    private void handleServerMessage(String message) {
        if (message == null || message.isBlank()) {
            return;
        }
        // 服务端消息采用 “类型|参数1|参数2...” 的文本协议。
        String[] parts = message.split("\\|");
        String type = parts[0];
        switch (type) {
            case "WAITING":
                // 已连接，但还没有匹配到第二名玩家。
                listener.onWaiting();
                break;
            case "MATCHED":
                // 匹配成功，服务器会返回当前玩家编号 1 或 2。
                if (parts.length >= 2) {
                    listener.onMatched(parseInt(parts[1]));
                }
                break;
            case "STATE":
                // 房间实时状态：双方分数和死亡状态。
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
                // 双方都死亡后，服务器广播最终结算分数。
                if (parts.length >= 3) {
                    listener.onSettle(parseInt(parts[1]), parseInt(parts[2]));
                }
                break;
            case "OPPONENT_LEFT":
                // 对手断开连接或主动退出。
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
