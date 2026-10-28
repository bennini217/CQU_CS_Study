package com.example.chat;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * WebSocket 文本帧的简易协议：使用 URL 查询串格式 key=value&key=value。
 * 数据字段 data 采用 Base64，targets 用逗号分隔。
 */
public class GatewayProtocol {
    public static String encode(ChatMessage msg) {
        StringBuilder sb = new StringBuilder();
        put(sb, "type", msg.getType().name());
        if (msg.getFrom() != null) put(sb, "from", msg.getFrom());
        if (msg.getText() != null) put(sb, "text", msg.getText());
        if (msg.getFileName() != null) put(sb, "fileName", msg.getFileName());
        if (msg.getTargets() != null && !msg.getTargets().isEmpty()) {
            put(sb, "targets", String.join(",", msg.getTargets()));
        }
        if (msg.getData() != null) {
            put(sb, "data", Base64.getEncoder().encodeToString(msg.getData()));
        }
        if (msg.getOnlineUsers() != null && !msg.getOnlineUsers().isEmpty()) {
            put(sb, "onlineUsers", String.join(",", msg.getOnlineUsers()));
        }
        if (msg.getNote() != null) put(sb, "note", msg.getNote());
        put(sb, "timestamp", String.valueOf(msg.getTimestamp()));
        // 去掉尾部 &
        if (sb.length() > 0 && sb.charAt(sb.length() - 1) == '&') {
            sb.setLength(sb.length() - 1);
        }
        return sb.toString();
    }

    private static void put(StringBuilder sb, String key, String value) {
        sb.append(key).append('=')
                .append(URLEncoder.encode(value, StandardCharsets.UTF_8))
                .append('&');
    }

    public static ChatMessage decode(String raw) {
        Map<String, String> map = parse(raw);
        MessageType type = MessageType.valueOf(map.getOrDefault("type", "CHAT"));
        ChatMessage msg = new ChatMessage();
        msg.setType(type);
        msg.setFrom(map.get("from"));
        msg.setText(map.get("text"));
        msg.setFileName(map.get("fileName"));
        msg.setNote(map.get("note"));
        msg.setTimestamp(parseLong(map.get("timestamp"), System.currentTimeMillis()));
        if (map.containsKey("targets")) {
            msg.setTargets(splitList(map.get("targets")));
        }
        if (map.containsKey("onlineUsers")) {
            msg.setOnlineUsers(splitList(map.get("onlineUsers")));
        }
        if (map.containsKey("data")) {
            msg.setData(Base64.getDecoder().decode(map.get("data")));
        }
        return msg;
    }

    private static Map<String, String> parse(String raw) {
        Map<String, String> map = new HashMap<>();
        if (raw == null || raw.isEmpty()) return map;
        String[] pairs = raw.split("&");
        for (String p : pairs) {
            int idx = p.indexOf('=');
            if (idx <= 0) continue;
            String k = p.substring(0, idx);
            String v = p.substring(idx + 1);
            map.put(k, URLDecoder.decode(v, StandardCharsets.UTF_8));
        }
        return map;
    }

    private static List<String> splitList(String s) {
        List<String> list = new ArrayList<>();
        for (String t : s.split(",")) {
            if (!t.isBlank()) list.add(t);
        }
        return list;
    }

    private static long parseLong(String v, long def) {
        try {
            return Long.parseLong(v);
        } catch (Exception e) {
            return def;
        }
    }
}
