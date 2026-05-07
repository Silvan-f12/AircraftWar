package com.example.myserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class MyClass {
	private static final int DEFAULT_PORT = 7777;

	public static void main(String[] args) {
		// 支持从命令行传入端口号，不传时使用默认端口 7777。
		int port = DEFAULT_PORT;
		if (args.length > 0) {
			try {
				port = Integer.parseInt(args[0]);
			} catch (NumberFormatException ignore) {
				System.out.println("Invalid port, fallback to 7777");
			}
		}
		new MatchServer(port).start();
	}

	private static final class MatchServer {
		private final int port;
		// 每个客户端连接都交给线程池处理，避免一个客户端阻塞整个服务器。
		private final ExecutorService ioPool = Executors.newCachedThreadPool();
		// 给每个连接生成一个简单的自增编号，便于区分客户端。
		private final AtomicInteger playerIdGenerator = new AtomicInteger(1);
		// 当前正在等待匹配的玩家；volatile 保证其他线程能看到最新引用。
		private volatile ClientSession waitingPlayer;

		MatchServer(int port) {
			this.port = port;
		}

		void start() {
			System.out.println("Socket match server started at port " + port);
			try (ServerSocket serverSocket = new ServerSocket(port)) {
				while (true) {
					// accept() 会阻塞，直到有新的客户端 Socket 连进来。
					Socket socket = serverSocket.accept();
					ClientSession session = new ClientSession(playerIdGenerator.getAndIncrement(), socket, this);
					// 客户端的读消息循环放到线程池中异步执行。
					ioPool.execute(session::listen);
				}
			} catch (IOException e) {
				System.out.println("Server stopped: " + e.getMessage());
			}
		}

		synchronized void onReady(ClientSession session) {
			// 第一个玩家进入等待队列，第二个玩家到来时才创建房间。
			if (waitingPlayer == null || waitingPlayer == session || !waitingPlayer.isOpen()) {
				waitingPlayer = session;
				session.send("WAITING");
				return;
			}
			ClientSession first = waitingPlayer;
			waitingPlayer = null;

			// 两名玩家绑定到同一个房间，并分别分配为 P1 和 P2。
			MatchRoom room = new MatchRoom(first, session);
			first.bindRoom(room, 1);
			session.bindRoom(room, 2);
			first.send("MATCHED|1");
			session.send("MATCHED|2");
			room.broadcastState();
		}

		synchronized void onClosed(ClientSession session) {
			// 如果等待中的玩家断开连接，需要清空等待位。
			if (waitingPlayer == session) {
				waitingPlayer = null;
			}
		}
	}

	private static final class MatchRoom {
		private final ClientSession player1;
		private final ClientSession player2;
		// 房间只保存双方分数、死亡状态和是否已结算。
		private int score1;
		private int score2;
		private boolean dead1;
		private boolean dead2;
		private boolean settled;

		MatchRoom(ClientSession player1, ClientSession player2) {
			this.player1 = player1;
			this.player2 = player2;
		}

		synchronized void updateScore(int playerIndex, int score) {
			// 已结算的房间不再接收分数更新。
			if (settled) {
				return;
			}
			if (playerIndex == 1) {
				score1 = score;
			} else {
				score2 = score;
			}
			broadcastState();
		}

		synchronized void markDead(int playerIndex, int finalScore) {
			// 玩家死亡时，先记录最终分数，再记录死亡状态。
			if (settled) {
				return;
			}
			updateScore(playerIndex, finalScore);
			if (playerIndex == 1) {
				dead1 = true;
			} else {
				dead2 = true;
			}
			broadcastState();
			if (dead1 && dead2) {
				// 双方都死亡后，广播最终结算消息。
				settled = true;
				broadcast("SETTLE|" + score1 + "|" + score2);
			}
		}

		synchronized void handleDisconnect(int playerIndex) {
			// 任一玩家异常退出时，通知房间内另一名玩家。
			if (settled) {
				return;
			}
			settled = true;
			int quitter = playerIndex;
			broadcast("OPPONENT_LEFT|" + quitter);
		}

		synchronized void broadcastState() {
			// 文本协议格式：STATE|玩家1分数|玩家2分数|玩家1是否死亡|玩家2是否死亡。
			broadcast("STATE|" + score1 + "|" + score2 + "|" + dead1 + "|" + dead2);
		}

		private void broadcast(String msg) {
			// 房间广播就是给两个客户端各发送同一条消息。
			player1.send(msg);
			player2.send(msg);
		}
	}

	private static final class ClientSession {
		private final int id;
		private final Socket socket;
		private final MatchServer server;
		private MatchRoom room;
		private int playerIndex;
		private BufferedReader input;
		// PrintWriter 使用 println 发送按行分隔的文本消息。
		private PrintWriter output;

		ClientSession(int id, Socket socket, MatchServer server) {
			this.id = id;
			this.socket = socket;
			this.server = server;
		}

		void bindRoom(MatchRoom room, int playerIndex) {
			// 记录当前连接属于哪个房间、是房间中的几号玩家。
			this.room = room;
			this.playerIndex = playerIndex;
		}

		boolean isOpen() {
			return !socket.isClosed();
		}

		void listen() {
			try (Socket s = socket;
				 BufferedReader reader = new BufferedReader(new InputStreamReader(s.getInputStream()));
				 PrintWriter writer = new PrintWriter(s.getOutputStream(), true)) {
				input = reader;
				output = writer;
				send("CONNECTED|" + id);

				String line;
				// readLine() 按行读取客户端消息，连接断开时返回 null。
				while ((line = input.readLine()) != null) {
					handleMessage(line);
				}
			} catch (IOException ignore) {
			} finally {
				cleanup();
			}
		}

		private void handleMessage(String message) {
			if (message == null || message.isBlank()) {
				return;
			}
			// 客户端和服务端约定使用 “类型|参数” 的文本协议。
			String[] parts = message.split("\\|");
			String type = parts[0];
			switch (type) {
				case "HELLO":
					// 客户端握手，表示自己准备进入匹配。
					server.onReady(this);
					break;
				case "SCORE":
					// 客户端上报当前分数，服务端更新房间状态并广播。
					if (room != null && parts.length >= 2) {
						room.updateScore(playerIndex, parseInt(parts[1], 0));
					}
					break;
				case "DEAD":
					// 客户端上报死亡和最终分数，双方都死亡后触发结算。
					if (room != null && parts.length >= 2) {
						room.markDead(playerIndex, parseInt(parts[1], 0));
					}
					break;
				case "QUIT":
					// 主动退出时关闭 Socket，随后 finally 中会做清理。
					try {
						socket.close();
					} catch (IOException ignore) {
					}
					break;
				default:
					send("ERROR|Unknown message type: " + type);
					break;
			}
		}

		void send(String message) {
			PrintWriter out = output;
			if (out != null) {
				// println 会补换行，客户端才能用 readLine() 正确读到一整条消息。
				out.println(message);
			}
		}

		private void cleanup() {
			// 连接结束后，既要从等待队列清理，也要通知房间对手。
			server.onClosed(this);
			if (room != null && playerIndex > 0) {
				room.handleDisconnect(playerIndex);
			}
		}

		private int parseInt(String raw, int fallback) {
			try {
				return Integer.parseInt(raw);
			} catch (NumberFormatException e) {
				return fallback;
			}
		}
	}
}
