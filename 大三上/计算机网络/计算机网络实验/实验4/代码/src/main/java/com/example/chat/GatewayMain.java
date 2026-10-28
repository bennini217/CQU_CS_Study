package com.example.chat;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 同时启动 TCP 聊天服务器 + WebSocket 网关 + 静态页面。
 * 默认端口：TCP 5000，HTTP/WebSocket 8080。
 */
public class GatewayMain {
    public static void main(String[] args) throws Exception {
        int tcpPort = 5000;
        int webPort = 8080;
        if (args.length >= 1) tcpPort = Integer.parseInt(args[0]);
        if (args.length >= 2) webPort = Integer.parseInt(args[1]);

        ChatServer chatServer = new ChatServer(tcpPort);
        new Thread(chatServer::start, "tcp-chat").start();

        String indexHtml = loadFile("web/index.html");
        String appJs = loadFile("web/app.js");
        WebSocketGatewayServer gateway = new WebSocketGatewayServer(chatServer, webPort, indexHtml, appJs);
        gateway.start();

        System.out.println("启动完成：TCP聊天端口 " + tcpPort + "，Web端口 " + webPort);
    }

    private static String loadFile(String path) throws Exception {
        return Files.readString(Path.of(path));
    }
}
