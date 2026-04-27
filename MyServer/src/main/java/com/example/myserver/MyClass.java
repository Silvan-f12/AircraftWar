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
		private final ExecutorService ioPool = Executors.newCachedThreadPool();
		private final AtomicInteger playerIdGenerator = new AtomicInteger(1);
		private volatile ClientSession waitingPlayer;

		MatchServer(int port) {
			this.port = port;
		}

		void start() {
			System.out.println("Socket match server started at port " + port);
			try (ServerSocket serverSocket = new ServerSocket(port)) {
				while (true) {
					Socket socket = serverSocket.accept();
					ClientSession session = new ClientSession(playerIdGenerator.getAndIncrement(), socket, this);
					ioPool.execute(session::listen);
				}
			} catch (IOException e) {
				System.out.println("Server stopped: " + e.getMessage());
			}
		}

		synchronized void onReady(ClientSession session) {
			if (waitingPlayer == null || waitingPlayer == session || !waitingPlayer.isOpen()) {
				waitingPlayer = session;
				session.send("WAITING");
				return;
			}
			ClientSession first = waitingPlayer;
			waitingPlayer = null;

			MatchRoom room = new MatchRoom(first, session);
			first.bindRoom(room, 1);
			session.bindRoom(room, 2);
			first.send("MATCHED|1");
			session.send("MATCHED|2");
			room.broadcastState();
		}

		synchronized void onClosed(ClientSession session) {
			if (waitingPlayer == session) {
				waitingPlayer = null;
			}
		}
	}

	private static final class MatchRoom {
		private final ClientSession player1;
		private final ClientSession player2;
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
				settled = true;
				broadcast("SETTLE|" + score1 + "|" + score2);
			}
		}

		synchronized void handleDisconnect(int playerIndex) {
			if (settled) {
				return;
			}
			settled = true;
			int quitter = playerIndex;
			broadcast("OPPONENT_LEFT|" + quitter);
		}

		synchronized void broadcastState() {
			broadcast("STATE|" + score1 + "|" + score2 + "|" + dead1 + "|" + dead2);
		}

		private void broadcast(String msg) {
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
		private PrintWriter output;

		ClientSession(int id, Socket socket, MatchServer server) {
			this.id = id;
			this.socket = socket;
			this.server = server;
		}

		void bindRoom(MatchRoom room, int playerIndex) {
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
			String[] parts = message.split("\\|");
			String type = parts[0];
			switch (type) {
				case "HELLO":
					server.onReady(this);
					break;
				case "SCORE":
					if (room != null && parts.length >= 2) {
						room.updateScore(playerIndex, parseInt(parts[1], 0));
					}
					break;
				case "DEAD":
					if (room != null && parts.length >= 2) {
						room.markDead(playerIndex, parseInt(parts[1], 0));
					}
					break;
					case "QUIT":
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
				out.println(message);
			}
		}

		private void cleanup() {
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