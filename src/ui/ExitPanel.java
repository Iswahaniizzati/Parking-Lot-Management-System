package ui;

import javax.swing.*;
import java.awt.*;
import data.DataStore;
import service.ExitService;
import service.PaymentProcessor;

public class ExitPanel extends JPanel {
    private DataStore store;
    private JTextField plateField;
    private JTextArea receiptArea;
    private JList<String> vehiclesInsideList;
    private DefaultListModel<String> listModel;
    private JButton processBtn;

    public ExitPanel(DataStore store, ExitService exitService, PaymentProcessor paymentProcessor) {
        this.store = store;
        setLayout(new BorderLayout(15, 15));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // --- TOP SECTION: Search Bar ---
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        searchPanel.add(new JLabel("Enter License Plate:"));
        
        plateField = new JTextField(15);
        searchPanel.add(plateField);
        
        JButton searchBtn = new JButton("Search Vehicle");
        searchPanel.add(searchBtn);

        // Manual Exit Time Checkbox
        JCheckBox manualTimeCheck = new JCheckBox("Manual Exit Time");
        searchPanel.add(manualTimeCheck);
        
        JTextField timeDisplay = new JTextField(java.time.LocalDateTime.now().toString().substring(0, 19));
        timeDisplay.setEditable(false);
        timeDisplay.setEnabled(false);
        searchPanel.add(timeDisplay);

        // --- CENTER SECTION: Two-Column Display ---
        JPanel mainDisplayPanel = new JPanel(new BorderLayout(15, 0));

        // Left Column: Vehicles Inside
        listModel = new DefaultListModel<>();
        vehiclesInsideList = new JList<>(listModel);
        JScrollPane listScroll = new JScrollPane(vehiclesInsideList);
        listScroll.setBorder(BorderFactory.createTitledBorder("Vehicles Inside"));
        listScroll.setPreferredSize(new Dimension(200, 0));

        // Right Column: Exit Receipt
        receiptArea = new JTextArea();
        receiptArea.setEditable(false);
        JScrollPane receiptScroll = new JScrollPane(receiptArea);
        receiptScroll.setBorder(BorderFactory.createTitledBorder("Exit Receipt / Invoice"));

        mainDisplayPanel.add(listScroll, BorderLayout.WEST);
        mainDisplayPanel.add(receiptScroll, BorderLayout.CENTER);

        // --- BOTTOM SECTION: Action Button ---
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        processBtn = new JButton("Process Payment & Exit");
        processBtn.setBackground(new Color(52, 152, 219)); // Light blue color
        processBtn.setForeground(Color.WHITE);
        processBtn.setFont(new Font("SansSerif", Font.BOLD, 12));
        processBtn.setPreferredSize(new Dimension(200, 40));
        bottomPanel.add(processBtn);

        // Add panels to main container
        add(searchPanel, BorderLayout.NORTH);
        add(mainDisplayPanel, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        // Initial Load
        refreshVehiclesInside();
    }

    private void refreshVehiclesInside() {
        listModel.clear();
        // This will link to your database parking_session table
        store.getAllActiveSessions().forEach(session -> {
            listModel.addElement(session.getPlate() + " (" + session.getSpotId() + ")");
        });
    }
}