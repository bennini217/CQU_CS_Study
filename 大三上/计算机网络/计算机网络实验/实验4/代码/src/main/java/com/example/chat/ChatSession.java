package com.example.chat;

/**
 * 抽象的会话连接（TCP 客户端 / WebSocket 客户端）。
 */
public interface ChatSession {
    void send(ChatMessage msg);
}
