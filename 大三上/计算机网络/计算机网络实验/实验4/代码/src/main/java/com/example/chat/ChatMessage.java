package com.example.chat;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 通过 Socket 传递的统一消息模型。
 */
public class ChatMessage implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private MessageType type;
    private String from;
    private List<String> targets;
    private String text;
    private String fileName;
    private byte[] data;
    private long timestamp;
    private List<String> onlineUsers;
    private String note;

    public ChatMessage() {
        this.timestamp = System.currentTimeMillis();
    }

    public static ChatMessage login(String userName) {
        ChatMessage msg = new ChatMessage();
        msg.type = MessageType.LOGIN;
        msg.from = userName;
        return msg;
    }

    public static ChatMessage loginOk(List<String> users) {
        ChatMessage msg = new ChatMessage();
        msg.type = MessageType.LOGIN_OK;
        msg.onlineUsers = new ArrayList<>(users);
        return msg;
    }

    public static ChatMessage loginFailed(String reason) {
        ChatMessage msg = new ChatMessage();
        msg.type = MessageType.LOGIN_FAILED;
        msg.note = reason;
        return msg;
    }

    public static ChatMessage userList(List<String> users) {
        ChatMessage msg = new ChatMessage();
        msg.type = MessageType.USER_LIST;
        msg.onlineUsers = new ArrayList<>(users);
        return msg;
    }

    public static ChatMessage system(String text) {
        ChatMessage msg = new ChatMessage();
        msg.type = MessageType.SYSTEM;
        msg.text = text;
        return msg;
    }

    public static ChatMessage chat(String from, List<String> targets, String text) {
        ChatMessage msg = new ChatMessage();
        msg.type = MessageType.CHAT;
        msg.from = from;
        msg.targets = targets;
        msg.text = text;
        return msg;
    }

    public static ChatMessage file(String from, List<String> targets, String fileName, byte[] data, boolean isImage) {
        ChatMessage msg = new ChatMessage();
        msg.type = isImage ? MessageType.IMAGE : MessageType.FILE;
        msg.from = from;
        msg.targets = targets;
        msg.fileName = fileName;
        msg.data = data;
        return msg;
    }

    public MessageType getType() {
        return type;
    }

    public void setType(MessageType type) {
        this.type = type;
    }

    public String getFrom() {
        return from;
    }

    public List<String> getTargets() {
        return targets;
    }

    public String getText() {
        return text;
    }

    public String getFileName() {
        return fileName;
    }

    public byte[] getData() {
        return data;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public List<String> getOnlineUsers() {
        return onlineUsers;
    }

    public String getNote() {
        return note;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public void setTargets(List<String> targets) {
        this.targets = targets;
    }

    public void setText(String text) {
        this.text = text;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public void setOnlineUsers(List<String> onlineUsers) {
        this.onlineUsers = onlineUsers;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
