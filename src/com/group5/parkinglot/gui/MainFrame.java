package com.group5.parkinglot.gui;

import com.group5.parkinglot.service.ParkingService;
import java.awt.*;
import javax.swing.*;

public class MainFrame extends JFrame {

    private final ParkingService parkingService;
    private final AdminPanel adminPanel;
    private final JLabel footerLabel;

    public MainFrame() {
        setTitle("University Parking Lot Management System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 600);
        setLocationRelativeTo(null); // center on screen

        // Initialize service
        this.parkingService = new ParkingService();

        // Tabbed Pane
        JTabbedPane tabbedPane = new JTabbedPane();

        // Vehicle Entry Tab
        EntryPanel entryPanel = new EntryPanel(parkingService);
        tabbedPane.addTab("Vehicle Entry", entryPanel);

        // Vehicle Exit Tab
        ExitPanel exitPanel = new ExitPanel(parkingService);
        tabbedPane.addTab("Vehicle Exit", exitPanel);

        // Admin & Reports Tab
        adminPanel = new AdminPanel(parkingService);
        tabbedPane.addTab("Admin & Reports", adminPanel);

        add(tabbedPane, BorderLayout.CENTER);

        // Footer: total revenue
        footerLabel = new JLabel("", SwingConstants.CENTER);
        updateFooter();
        add(footerLabel, BorderLayout.SOUTH);

        // Listen for tab changes and refresh admin panel when selected
        tabbedPane.addChangeListener(e -> {
            if (tabbedPane.getSelectedComponent() == adminPanel) {
                adminPanel.refreshData();
                updateFooter();
            }
        });

        // Also refresh after entry or exit via EntryPanel/ExitPanel callbacks
        entryPanel.setAfterParkingCallback(this::refreshAdminPanel);
        exitPanel.setAfterExitCallback(this::refreshAdminPanel);
    }

    private void refreshAdminPanel() {
        adminPanel.refreshData();
        updateFooter();
    }

    private void updateFooter() {
        footerLabel.setText(String.format("Total Revenue: RM %.2f", parkingService.getTotalRevenue()));
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
        });
    }
}
