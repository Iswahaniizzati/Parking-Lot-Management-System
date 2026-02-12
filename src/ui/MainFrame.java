package ui;

import javax.swing.*;
import java.awt.*;
import data.DataStore;
import service.ExitService;
import service.PaymentProcessor;

public class MainFrame extends JFrame {
    private DataStore store;
    private ExitService exitService;
    private PaymentProcessor paymentProcessor;

    public MainFrame(DataStore store, ExitService exitService, PaymentProcessor paymentProcessor) {
        this.store = store;
        this.exitService = exitService;
        this.paymentProcessor = paymentProcessor;

        // Basic Window Setup
        setTitle("University Parking Management System");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Layout: Tabbed Pane to separate Entry/Exit and Admin
        JTabbedPane tabbedPane = new JTabbedPane();

        // 1. Entry/Exit Tab (Where the daily operations happen)
        // You will need to create this panel or use a placeholder for now
        tabbedPane.addTab("Entry/Exit Operations", new EntryExitPanel(store, exitService, paymentProcessor));

        // 2. Admin Tab (Where schemes are changed)
        tabbedPane.addTab("Admin Dashboard", new AdminPanel(exitService));

        add(tabbedPane);
    }
}