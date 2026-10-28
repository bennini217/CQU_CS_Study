package com.example.chat.ui;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.GridLayout;

/**
 * 登录对话框，输入服务器地址、端口、用户名。
 */
public class LoginDialog extends JDialog {
    private final JTextField hostField = new JTextField("127.0.0.1");
    private final JTextField portField = new JTextField("5000");
    private final JTextField nameField = new JTextField("user" + (int) (Math.random() * 1000));
    private boolean confirmed = false;

    public LoginDialog(JFrame owner) {
        super(owner, "登录聊天", true);
        buildUI();
        pack();
        setLocationRelativeTo(owner);
    }

    private void buildUI() {
        JPanel form = new JPanel(new GridLayout(3, 2, 8, 8));
        form.add(new JLabel("服务器IP:"));
        form.add(hostField);
        form.add(new JLabel("端口:"));
        form.add(portField);
        form.add(new JLabel("用户名:"));
        form.add(nameField);

        JButton ok = new JButton("连接");
        JButton cancel = new JButton("取消");
        ok.addActionListener(e -> {
            confirmed = true;
            setVisible(false);
        });
        cancel.addActionListener(e -> setVisible(false));
        JPanel buttons = new JPanel();
        buttons.add(ok);
        buttons.add(cancel);

        getContentPane().add(form, BorderLayout.CENTER);
        getContentPane().add(buttons, BorderLayout.SOUTH);
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public String getHost() {
        return hostField.getText().trim();
    }

    public int getPort() {
        return Integer.parseInt(portField.getText().trim());
    }

    public String getUserName() {
        return nameField.getText().trim();
    }
}
