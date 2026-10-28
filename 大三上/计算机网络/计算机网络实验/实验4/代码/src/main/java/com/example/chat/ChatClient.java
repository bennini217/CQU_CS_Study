package com.example.chat;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 客户端网络层，负责与服务器通信。
 */
public class ChatClient implements Closeable {
    private final String host;
    private final int port;
    private final String userName;
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private Thread readerThread;
    private final List<Listener> listeners = new CopyOnWriteArrayList<>();
    private volatile boolean running = false;

    public interface Listener {
        void onMessage(ChatMessage msg);

        void onDisconnected(String reason);
    }

    public ChatClient(String host, int port, String userName) {
        this.host = host;
        this.port = port;
        this.userName = userName;
    }

    public String getUserName() {
        return userName;
    }

    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    public void connect() throws IOException, ClassNotFoundException {
        socket = new Socket(host, port);
        out = new ObjectOutputStream(socket.getOutputStream());
        out.flush();
        in = new ObjectInputStream(socket.getInputStream());
        running = true;

        // 登录
        send(ChatMessage.login(userName));
        ChatMessage resp = (ChatMessage) in.readObject();
        if (resp.getType() == MessageType.LOGIN_FAILED) {
            throw new IOException("登录失败: " + resp.getNote());
        }
        handleIncoming(resp);

        readerThread = new Thread(this::readLoop, "reader-" + userName);
        readerThread.start();
    }

    private void readLoop() {
        try {
            while (running) {
                ChatMessage msg = (ChatMessage) in.readObject();
                if (msg == null) {
                    break;
                }
                handleIncoming(msg);
            }
        } catch (Exception e) {
            if (running) {
                notifyDisconnect("连接中断: " + e.getMessage());
            }
        } finally {
            running = false;
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }
    }

    private void handleIncoming(ChatMessage msg) {
        if (msg.getType() == MessageType.FILE || msg.getType() == MessageType.IMAGE) {
            saveIncomingFile(msg);
        }
        for (Listener l : listeners) {
            l.onMessage(msg);
        }
    }

    private void saveIncomingFile(ChatMessage msg) {
        if (msg.getFileName() == null || msg.getData() == null) {
            return;
        }
        File dir = new File("downloads");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File outFile = new File(dir, msg.getFileName());
        try (FileOutputStream fos = new FileOutputStream(outFile)) {
            fos.write(msg.getData());
            msg.setNote("文件已保存至: " + outFile.getAbsolutePath());
        } catch (IOException e) {
            msg.setNote("文件保存失败: " + e.getMessage());
        }
    }

    public void sendText(List<String> targets, String text) throws IOException {
        send(ChatMessage.chat(userName, new ArrayList<>(targets), text));
    }

    public void sendFile(List<String> targets, File file, boolean isImage) throws IOException {
        byte[] bytes = Files.readAllBytes(file.toPath());
        send(ChatMessage.file(userName, new ArrayList<>(targets), file.getName(), bytes, isImage));
    }

    public synchronized void send(ChatMessage msg) throws IOException {
        if (!running) {
            throw new IOException("未连接服务器");
        }
        out.writeObject(msg);
        out.flush();
    }

    @Override
    public void close() {
        running = false;
        try {
            if (out != null) {
                ChatMessage logout = new ChatMessage();
                logout.setType(com.example.chat.MessageType.LOGOUT);
                logout.setFrom(userName);
                out.writeObject(logout);
                out.flush();
            }
        } catch (IOException ignored) {
        }
        try {
            socket.close();
        } catch (IOException ignored) {
        }
    }

    private void notifyDisconnect(String reason) {
        for (Listener l : listeners) {
            l.onDisconnected(reason);
        }
    }

    @Override
    public String toString() {
        return userName + "@" + host + ":" + port;
    }
}
