package ui;

import java.awt.*;
import javax.swing.*;

public class MainFrame extends JFrame {
    private String role; 

    public MainFrame(data.DataStore store, service.ExitService exitService, service.EntryService entryService,
                     service.PaymentProcessor paymentProcessor, String role) {
        this.role = role;
        
        setTitle("University Parking Management System");
        setSize(1100, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // --- Initialize Panels ---
        ReportingPanel reportingPanel = new ReportingPanel(store);
        EntryPanel entryPanel = new EntryPanel(store, entryService);
        ExitPanel exitPanel = new ExitPanel(store, exitService, paymentProcessor, reportingPanel);
        AdminPanel adminPanel = new AdminPanel(exitService, store);

        // --- Header Setup ---
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(41, 128, 185));
        headerPanel.setPreferredSize(new Dimension(0, 60));
        
        // Add Title
        JLabel titleLabel = new JLabel("  PARKING CONTROL CENTER");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        titleLabel.setForeground(Color.WHITE);
        headerPanel.add(titleLabel, BorderLayout.WEST); 

        // Right side panel for User Info and Logout
        JPanel rightHeaderPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 15));
        rightHeaderPanel.setOpaque(false); 

        JLabel userLabel = new JLabel("Logged in as: " + role);
        userLabel.setForeground(Color.WHITE);
        
        // DECLARE ONCE: Setup Logout Button
        JButton logoutBtn = new JButton("Logout");
        logoutBtn.addActionListener(e -> {
            this.dispose();
            app.main.main(null); 
        });

        rightHeaderPanel.add(userLabel);
        rightHeaderPanel.add(logoutBtn); 
        
        headerPanel.add(rightHeaderPanel, BorderLayout.EAST);
        add(headerPanel, BorderLayout.NORTH);

        // --- Tabs ---
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Vehicle Entry", entryPanel);
        tabbedPane.addTab("Vehicle Exit", exitPanel);

        if (role.equalsIgnoreCase("Admin")) {
            tabbedPane.addTab("Admin Dashboard", adminPanel);
            tabbedPane.addTab("Live Reports", reportingPanel);
        }
        add(tabbedPane, BorderLayout.CENTER);

        // --- Tab Change Listener ---
        tabbedPane.addChangeListener(e -> {
            int index = tabbedPane.getSelectedIndex();
            if (index != -1) {
                String title = tabbedPane.getTitleAt(index);
                if (title.equals("Live Reports")) {
                    reportingPanel.refreshStats(); 
                }
            }
        });
    }
}