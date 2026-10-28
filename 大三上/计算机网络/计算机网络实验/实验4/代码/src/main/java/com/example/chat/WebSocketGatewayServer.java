package com.example.chat;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;

/**
 * 嵌入式 HTTP + WebSocket 网关。
 * - GET / 返回内置 index.html
 * - GET /app.js 返回前端脚本
 * - GET /ws 升级 WebSocket，文本帧使用 GatewayProtocol 协议
 */
public class WebSocketGatewayServer {
    private final ChatServer chatServer;
    private final int port;
    private volatile boolean running = true;
    private final String indexHtml;
    private final String appJs;

    public WebSocketGatewayServer(ChatServer chatServer, int port, String indexHtml, String appJs) {
        this.chatServer = chatServer;
        this.port = port;
        this.indexHtml = indexHtml;
        this.appJs = appJs;
    }

    public void start() {
        Thread t = new Thread(this::runLoop, "ws-gateway-" + port);
        t.start();
        System.out.println("Web 网关启动，HTTP/WebSocket 端口: " + port);
    }

    private void runLoop() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (running) {
                Socket socket = serverSocket.accept();
                new Thread(() -> handleConnection(socket), "http-" + socket.getPort()).start();
            }
        } catch (IOException e) {
            System.err.println("Web 网关异常: " + e.getMessage());
        }
    }

    private void handleConnection(Socket socket) {
        try (InputStream in = socket.getInputStream(); OutputStream out = socket.getOutputStream()) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            String requestLine = reader.readLine();
            if (requestLine == null || requestLine.isBlank()) {
                socket.close();
                return;
            }
            String[] parts = requestLine.split(" ");
            if (parts.length < 2) {
                socket.close();
                return;
            }
            String method = parts[0];
            String path = parts[1];
            List<String> headers = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null && !line.isEmpty()) {
                headers.add(line);
            }

            boolean isWebSocket = headers.stream().anyMatch(h -> h.toLowerCase(Locale.ROOT).contains("upgrade: websocket"));
            if (isWebSocket && "/ws".equals(path)) {
                handleWebSocket(socket, reader, out, headers);
                return;
            }

            if (!"GET".equalsIgnoreCase(method)) {
                respondText(out, "405 Method Not Allowed", "Method Not Allowed");
                return;
            }
            if ("/app.js".equals(path)) {
                respond(out, "200 OK", "application/javascript", appJs);
            } else { // default index
                respond(out, "200 OK", "text/html; charset=utf-8", indexHtml);
            }
        } catch (IOException e) {
            System.err.println("HTTP 处理异常: " + e.getMessage());
        }
    }

    private void respond(OutputStream out, String status, String contentType, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8));
        writer.write("HTTP/1.1 " + status + "\r\n");
        writer.write("Content-Type: " + contentType + "\r\n");
        writer.write("Content-Length: " + bytes.length + "\r\n");
        writer.write("Connection: close\r\n\r\n");
        writer.flush();
        out.write(bytes);
        out.flush();
    }

    private void respondText(OutputStream out, String status, String body) throws IOException {
        respond(out, status, "text/plain; charset=utf-8", body);
    }

    private void handleWebSocket(Socket socket, BufferedReader reader, OutputStream rawOut, List<String> headers) throws IOException {
        String key = null;
        for (String h : headers) {
            String lower = h.toLowerCase(Locale.ROOT);
            if (lower.startsWith("sec-websocket-key:")) {
                key = h.substring(h.indexOf(":") + 1).trim();
                break;
            }
        }
        if (key == null) {
            socket.close();
            return;
        }
        String accept = buildWebSocketAccept(key);
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(rawOut, StandardCharsets.UTF_8));
        writer.write("HTTP/1.1 101 Switching Protocols\r\n");
        writer.write("Upgrade: websocket\r\n");
        writer.write("Connection: Upgrade\r\n");
        writer.write("Sec-WebSocket-Accept: " + accept + "\r\n\r\n");
        writer.flush();

        WebSocketSession session = new WebSocketSession(socket, chatServer);
        session.loop();
    }

    private String buildWebSocketAccept(String key) {
        try {
            String concat = key + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
            MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
            byte[] hash = sha1.digest(concat.getBytes(StandardCharsets.US_ASCII));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 单个 WebSocket 会话。
     */
    static class WebSocketSession implements ChatSession, Runnable {
        private final Socket socket;
        private final ChatServer chatServer;
        private final InputStream in;
        private final OutputStream out;
        private String userName;
        private volatile boolean running = true;

        WebSocketSession(Socket socket, ChatServer chatServer) throws IOException {
            this.socket = socket;
            this.chatServer = chatServer;
            this.in = socket.getInputStream();
            this.out = socket.getOutputStream();
        }

        @Override
        public void run() {
            loop();
        }

        public void loop() {
            try {
                while (running) {
                    String text = readTextFrame();
                    if (text == null) break;
                    ChatMessage msg = GatewayProtocol.decode(text);
                    if (msg.getType() == MessageType.LOGIN) {
                        userName = msg.getFrom();
                        if (!chatServer.register(userName, this)) {
                            send(ChatMessage.loginFailed("用户名已存在"));
                            close();
                            break;
                        } else {
                            send(ChatMessage.loginOk(chatServer.getUserList()));
                        }
                        continue;
                    }
                    if (msg.getType() == MessageType.LOGOUT) {
                        running = false;
                        break;
                    }
                    // 确保来源标记为当前用户
                    msg.setFrom(userName);
                    chatServer.forwardMessage(msg);
                }
            } catch (IOException e) {
                System.err.println("WebSocket 会话异常: " + e.getMessage());
            } finally {
                close();
                chatServer.unregister(userName);
            }
        }

        @Override
        public void send(ChatMessage msg) {
            try {
                writeTextFrame(GatewayProtocol.encode(msg));
            } catch (IOException e) {
                running = false;
                close();
                chatServer.unregister(userName);
            }
        }

        private void close() {
            running = false;
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }

        private String readTextFrame() throws IOException {
            // 支持分片帧与大负载，组装完整文本消息
            java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
            boolean isFirst = true;
            while (true) {
                int b1 = in.read();
                if (b1 == -1) return null;
                int b2 = in.read();
                if (b2 == -1) return null;
                boolean fin = (b1 & 0x80) != 0;
                int opcode = b1 & 0x0F;
                boolean masked = (b2 & 0x80) != 0;
                long len = b2 & 0x7F;
                if (len == 126) {
                    len = readExactly(2);
                } else if (len == 127) {
                    len = readExactly(8);
                }
                byte[] mask = null;
                if (masked) {
                    mask = readExactlyBytes(4);
                }
                byte[] payload = readExactlyBytes((int) len);
                if (masked && mask != null) {
                    for (int i = 0; i < payload.length; i++) {
                        payload[i] = (byte) (payload[i] ^ mask[i % 4]);
                    }
                }

                if (opcode == 8) { // close
                    running = false;
                    return null;
                }
                if (opcode == 0) {
                    // continuation frame
                    buffer.write(payload);
                } else if (opcode == 1) {
                    if (!isFirst) {
                        throw new IOException("收到意外的文本起始帧");
                    }
                    buffer.write(payload);
                } else {
                    // 忽略非文本
                    return null;
                }

                if (fin) {
                    return buffer.toString(StandardCharsets.UTF_8);
                }
                isFirst = false;
            }
        }

        private long readExactly(int bytes) throws IOException {
            long val = 0;
            for (int i = 0; i < bytes; i++) {
                int b = in.read();
                if (b == -1) throw new IOException("连接已关闭");
                val = (val << 8) | (b & 0xFF);
            }
            return val;
        }

        private byte[] readExactlyBytes(int len) throws IOException {
            byte[] data = in.readNBytes(len);
            if (data.length != len) {
                throw new IOException("数据长度不足");
            }
            return data;
        }

        private void writeTextFrame(String text) throws IOException {
            byte[] data = text.getBytes(StandardCharsets.UTF_8);
            int len = data.length;
            List<byte[]> chunks = new ArrayList<>();
            if (len <= 125) {
                chunks.add(new byte[]{(byte) 0x81, (byte) len});
            } else if (len <= 65535) {
                chunks.add(new byte[]{(byte) 0x81, 126, (byte) (len >> 8), (byte) len});
            } else {
                byte[] header = new byte[10];
                header[0] = (byte) 0x81;
                header[1] = 127;
                long l = len;
                for (int i = 9; i >= 2; i--) {
                    header[i] = (byte) (l & 0xFF);
                    l >>= 8;
                }
                chunks.add(header);
            }
            for (byte[] h : chunks) {
                out.write(h);
            }
            out.write(data);
            out.flush();
        }
    }
}
