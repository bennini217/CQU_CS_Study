package com.example.chat;

import com.example.chat.ui.ChatWindow;
import com.example.chat.ui.LoginDialog;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import java.io.IOException;

/**
 * 客户端入口。
 */
public class ClientMain {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame();
            LoginDialog dialog = new LoginDialog(frame);
            dialog.setVisible(true);
            if (!dialog.isConfirmed()) {
                System.exit(0);
                return;
            }
            String host = dialog.getHost();
            int port = dialog.getPort();
            String user = dialog.getUserName();
            ChatClient client = new ChatClient(host, port, user);
            try {
                client.connect();
                new ChatWindow(client);
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
                javax.swing.JOptionPane.showMessageDialog(frame, "连接失败: " + e.getMessage());
                System.exit(1);
            }
        });
    }
}
