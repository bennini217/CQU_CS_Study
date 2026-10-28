package com.example.chat;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * 单个客户端的处理线程。
 */
public class ClientHandler implements Runnable, ChatSession {
    private final Socket socket;
    private final ChatServer server;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private String userName;
    private volatile boolean running = true;

    public ClientHandler(Socket socket, ChatServer server) {
        this.socket = socket;
        this.server = server;
    }

    @Override
    public void run() {
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());

            // 首包必须是 LOGIN
            ChatMessage first = (ChatMessage) in.readObject();
            if (first.getType() != MessageType.LOGIN || first.getFrom() == null || first.getFrom().isBlank()) {
                send(ChatMessage.loginFailed("请先发送登录消息"));
                close();
                return;
            }
            userName = first.getFrom().trim();
            if (!server.register(userName, this)) {
                send(ChatMessage.loginFailed("用户名已存在"));
                close();
                return;
            }
            send(ChatMessage.loginOk(server.getUserList()));

            while (running) {
                ChatMessage msg = (ChatMessage) in.readObject();
                if (msg == null) {
                    break;
                }
                switch (msg.getType()) {
                    case CHAT, FILE, IMAGE -> server.forwardMessage(msg);
                    case LOGOUT -> running = false;
                    case HEARTBEAT -> {
                        // 心跳无需处理
                    }
                    default -> {
                        // 其他类型忽略
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("客户端连接异常: " + e.getMessage());
        } catch (ClassNotFoundException e) {
            System.err.println("反序列化失败: " + e.getMessage());
        } finally {
            close();
            server.unregister(userName);
        }
    }

    @Override
    public void send(ChatMessage msg) {
        try {
            out.writeObject(msg);
            out.flush();
        } catch (IOException e) {
            running = false;
            close();
            server.unregister(userName);
        }
    }

    private void close() {
        running = false;
        try {
            if (in != null) {
                in.close();
            }
        } catch (IOException ignored) {
        }
        try {
            if (out != null) {
                out.close();
            }
        } catch (IOException ignored) {
        }
        try {
            socket.close();
        } catch (IOException ignored) {
        }
    }
}
