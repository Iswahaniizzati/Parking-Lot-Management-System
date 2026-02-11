package com.group5.parkinglot.gui;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.*;

public class LoginFrame extends JFrame {

    private JTextField usernameField;
    private JPasswordField passwordField;

    public LoginFrame() {
        setTitle("Login - Parking Lot System");
        setSize(500, 350);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Set background panel
        setContentPane(new BackgroundPanel());

        // Center layout
        setLayout(new GridBagLayout());

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(new Color(0, 0, 0, 150)); // semi-transparent black
        formPanel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.anchor = GridBagConstraints.WEST;

        // Username
        gbc.gridx = 0;
        gbc.gridy = 0;
        JLabel userLabel = new JLabel("Username:");
        userLabel.setForeground(Color.WHITE);
        formPanel.add(userLabel, gbc);

        gbc.gridx = 1;
        usernameField = new JTextField(15);
        formPanel.add(usernameField, gbc);

        // Password
        gbc.gridx = 0;
        gbc.gridy = 1;
        JLabel passLabel = new JLabel("Password:");
        passLabel.setForeground(Color.WHITE);
        formPanel.add(passLabel, gbc);

        gbc.gridx = 1;
        passwordField = new JPasswordField(15);
        formPanel.add(passwordField, gbc);

        // Buttons
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;

        JPanel buttonPanel = new JPanel();
        buttonPanel.setOpaque(false);

        JButton loginButton = new JButton("Login");
        JButton cancelButton = new JButton("Exit");

        loginButton.addActionListener(this::handleLogin);
        cancelButton.addActionListener(e -> System.exit(0));

        buttonPanel.add(loginButton);
        buttonPanel.add(cancelButton);

        formPanel.add(buttonPanel, gbc);

        add(formPanel);

        getRootPane().setDefaultButton(loginButton);
    }

    private void handleLogin(ActionEvent e) {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        if ("admin".equals(username) && "admin123".equals(password)) {
            openMainFrame("Admin");
        } 
        else if ("staff".equals(username) && "staff123".equals(password)) {
            openMainFrame("Attendant");
        } 
        else {
            JOptionPane.showMessageDialog(this,
                    "Invalid username or password!",
                    "Login Failed",
                    JOptionPane.ERROR_MESSAGE);
            passwordField.setText("");
        }
    }

    private void openMainFrame(String role) {
        dispose(); // close login window

        MainFrame frame = new MainFrame();

        if (!"Admin".equals(role)) {
            JTabbedPane tabbedPane =
                (JTabbedPane) frame.getContentPane().getComponent(0);
            tabbedPane.remove(2); // remove Admin tab
        }

        frame.setVisible(true);
    }


    // Background panel class
    class BackgroundPanel extends JPanel {
        private Image backgroundImage;

        public BackgroundPanel() {
            try {
                backgroundImage = ImageIO.read(
                        getClass().getResource("/resources/images/bg.jpg"));
            } catch (IOException | IllegalArgumentException e) {
                System.out.println("Background image not found.");
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (backgroundImage != null) {
                g.drawImage(backgroundImage, 0, 0,
                        getWidth(), getHeight(), this);
            }
        }
    }
}
