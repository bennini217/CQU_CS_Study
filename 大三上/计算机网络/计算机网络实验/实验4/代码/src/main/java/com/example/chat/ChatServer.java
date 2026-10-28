package com.example.chat;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 聊天服务器入口。
 */
public class ChatServer {
    private final int port;
    private final Map<String, ChatSession> sessions = new ConcurrentHashMap<>();
    private volatile boolean running = true;

    public ChatServer(int port) {
        this.port = port;
    }

    public static void main(String[] args) {
        int port = 5000;
        if (args.length > 0) {
            port = Integer.parseInt(args[0]);
        }
        ChatServer server = new ChatServer(port);
        server.start();
    }

    public void start() {
        System.out.println("服务器启动，监听端口: " + port);
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (running) {
                Socket socket = serverSocket.accept();
                ClientHandler handler = new ClientHandler(socket, this);
                new Thread(handler, "client-" + socket.getPort()).start();
            }
        } catch (IOException e) {
            System.err.println("服务器异常: " + e.getMessage());
        }
    }

    public synchronized boolean register(String userName, ChatSession session) {
        if (sessions.containsKey(userName)) {
            return false;
        }
        sessions.put(userName, session);
        broadcastUserList();
        broadcastSystem("用户 " + userName + " 上线");
        return true;
    }

    public void unregister(String userName) {
        if (userName != null) {
            sessions.remove(userName);
            broadcastUserList();
            broadcastSystem("用户 " + userName + " 离线");
        }
    }

    public List<String> getUserList() {
        return Collections.unmodifiableList(new ArrayList<>(sessions.keySet()));
    }

    public void broadcastUserList() {
        List<String> users = getUserList();
        ChatMessage listMsg = ChatMessage.userList(users);
        sendToAll(listMsg);
    }

    public void broadcastSystem(String text) {
        sendToAll(ChatMessage.system(text));
    }

    public void sendToAll(ChatMessage message) {
        for (ChatSession s : sessions.values()) {
            s.send(message);
        }
    }

    public void forwardMessage(ChatMessage msg) {
        if (msg.getTargets() == null || msg.getTargets().isEmpty()) {
            sendToAll(msg);
            return;
        }
        for (String target : msg.getTargets()) {
            ChatSession session = sessions.get(target);
            if (session != null) {
                session.send(msg);
            }
        }
    }
}
