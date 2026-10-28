package com.example.chat.ui;

import com.example.chat.ChatClient;
import com.example.chat.ChatMessage;
import com.example.chat.MessageType;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 主聊天窗口。
 */
public class ChatWindow extends JFrame implements ChatClient.Listener {
    private final ChatClient client;
    private final JTextArea chatArea = new JTextArea();
    private final JTextField inputField = new JTextField();
    private final DefaultListModel<String> userModel = new DefaultListModel<>();
    private final JList<String> userList = new JList<>(userModel);

    public ChatWindow(ChatClient client) {
        super("多人聊天 - " + client.toString());
        this.client = client;
        this.client.addListener(this);
        buildUI();
        setSize(900, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                client.close();
            }
        });
        setVisible(true);
    }

    private void buildUI() {
        chatArea.setEditable(false);
        JScrollPane chatScroll = new JScrollPane(chatArea);

        userList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        userList.setPreferredSize(new Dimension(180, 400));
        userModel.addElement("ALL");
        JScrollPane listScroll = new JScrollPane(userList);
        listScroll.setBorder(BorderFactory.createTitledBorder("在线用户（多选=组聊，ALL=群发）"));

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, listScroll, chatScroll);
        split.setDividerLocation(200);

        JPanel inputPanel = new JPanel(new BorderLayout(8, 8));
        inputPanel.add(inputField, BorderLayout.CENTER);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton sendBtn = new JButton("发送文字");
        JButton fileBtn = new JButton("发送文件");
        JButton imgBtn = new JButton("发送图片");
        buttons.add(sendBtn);
        buttons.add(fileBtn);
        buttons.add(imgBtn);
        inputPanel.add(buttons, BorderLayout.EAST);

        sendBtn.addActionListener(e -> sendText());
        fileBtn.addActionListener(e -> sendFile(false));
        imgBtn.addActionListener(e -> sendFile(true));
        inputField.addActionListener(e -> sendText());

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.add(inputPanel, BorderLayout.CENTER);
        bottom.add(new JLabel("请选择收件人，未选默认群发"), BorderLayout.SOUTH);

        getContentPane().add(split, BorderLayout.CENTER);
        getContentPane().add(bottom, BorderLayout.SOUTH);
    }

    private List<String> selectedTargets() {
        List<String> list = userList.getSelectedValuesList();
        List<String> targets = new ArrayList<>();
        for (String v : list) {
            if (!"ALL".equals(v)) {
                targets.add(v);
            }
        }
        return targets;
    }

    private void sendText() {
        String text = inputField.getText().trim();
        if (text.isEmpty()) {
            return;
        }
        inputField.setText("");
        try {
            client.sendText(selectedTargets(), text);
            appendLine("我: " + text);
        } catch (IOException ex) {
            showError("发送失败: " + ex.getMessage());
        }
    }

    private void sendFile(boolean isImage) {
        JFileChooser chooser = new JFileChooser();
        int res = chooser.showOpenDialog(this);
        if (res != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File file = chooser.getSelectedFile();
        try {
            client.sendFile(selectedTargets(), file, isImage);
            appendLine("我 发送了" + (isImage ? "图片" : "文件") + ": " + file.getName());
        } catch (IOException e) {
            showError("发送失败: " + e.getMessage());
        }
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "错误", JOptionPane.ERROR_MESSAGE);
    }

    private void appendLine(String line) {
        SwingUtilities.invokeLater(() -> {
            chatArea.append(line + "\n");
            chatArea.setCaretPosition(chatArea.getDocument().getLength());
        });
    }

    @Override
    public void onMessage(ChatMessage msg) {
        switch (msg.getType()) {
            case CHAT -> appendLine(msg.getFrom() + ": " + msg.getText());
            case SYSTEM -> appendLine("[系统] " + msg.getText());
            case USER_LIST, LOGIN_OK -> updateUsers(msg.getOnlineUsers());
            case FILE -> {
                appendLine(msg.getFrom() + " 发送文件: " + msg.getFileName());
                if (msg.getNote() != null) {
                    appendLine(" -> " + msg.getNote());
                }
            }
            case IMAGE -> {
                appendLine(msg.getFrom() + " 发送图片: " + msg.getFileName());
                if (msg.getNote() != null) {
                    appendLine(" -> " + msg.getNote());
                }
            }
            default -> {
            }
        }
    }

    private void updateUsers(List<String> users) {
        SwingUtilities.invokeLater(() -> {
            userModel.clear();
            userModel.addElement("ALL");
            if (users != null) {
                for (String u : users) {
                    userModel.addElement(u);
                }
            }
        });
    }

    @Override
    public void onDisconnected(String reason) {
        appendLine("[系统] 断开连接: " + reason);
        JOptionPane.showMessageDialog(this, reason, "断开", JOptionPane.WARNING_MESSAGE);
    }
}
