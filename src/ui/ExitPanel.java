package ui;

import data.DataStore;
import enums.PaymentMethod;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import javax.swing.*;
import model.ParkingSession;
import model.PaymentRecord;
import service.ExitService;
import service.PaymentProcessor;

public class ExitPanel extends JPanel {
    private DataStore store;
    private JTextField plateField;
    private JTextField timeDisplay;
    private JTextField cardField;
    private JTextField amountPaidField;
    private JTextField changeField;
    private JTextArea receiptArea;
    private JList<String> vehiclesInsideList;
    private DefaultListModel<String> listModel;
    private JButton processBtn;
    private JComboBox<PaymentMethod> methodCombo;

    private ExitService exitService;
    private PaymentProcessor paymentProcessor;

    public ExitPanel(DataStore store, ExitService exitService, PaymentProcessor paymentProcessor) {
        this.store = store;
        this.exitService = exitService;
        this.paymentProcessor = paymentProcessor;

        setLayout(new BorderLayout(15, 15));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // --- TOP SECTION: Search Bar ---
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        searchPanel.add(new JLabel("Enter License Plate:"));
        
        plateField = new JTextField(10);
        searchPanel.add(plateField);
        
        JButton searchBtn = new JButton("Search Vehicle");
        searchPanel.add(searchBtn);

        // Manual Exit Time Checkbox
        JCheckBox manualTimeCheck = new JCheckBox("Manual Exit Time");
        searchPanel.add(manualTimeCheck);

        timeDisplay = new JTextField(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), 19);
        timeDisplay.setEditable(false);
        timeDisplay.setEnabled(false);
        searchPanel.add(timeDisplay);

        // Payment Method Dropdown
        methodCombo = new JComboBox<>(PaymentMethod.values());
        searchPanel.add(new JLabel("Payment Method:"));
        searchPanel.add(methodCombo);


        methodCombo.addActionListener(e -> {
            cardField.setEnabled(methodCombo.getSelectedItem() == PaymentMethod.CARD);
        });

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

        // --- BOTTOM SECTION: Action Buttons ---
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        amountPaidField = new JTextField(8);
        bottomPanel.add(new JLabel("Amount Paid:"));
        bottomPanel.add(amountPaidField);

        changeField = new JTextField(8);
        changeField.setEditable(false);
        bottomPanel.add(new JLabel("Change:"));
        bottomPanel.add(changeField);

        processBtn = new JButton("Process Payment & Exit");
        processBtn.setBackground(new Color(52, 152, 219));
        processBtn.setForeground(Color.WHITE);
        processBtn.setFont(new Font("SansSerif", Font.BOLD, 12));
        processBtn.setPreferredSize(new Dimension(200, 40));
        bottomPanel.add(processBtn);

        add(searchPanel, BorderLayout.NORTH);
        add(mainDisplayPanel, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        // --- Button Actions ---
        searchBtn.addActionListener(e -> searchVehicle());
        processBtn.addActionListener(e -> processPaymentExit());

        refreshVehiclesInside();
    }

    private void refreshVehiclesInside() {
        listModel.clear();
        store.getAllActiveSessions().forEach(session ->
                listModel.addElement(session.getPlate() + " (" + session.getSpotId() + ")")
        );
    }

    private void searchVehicle() {
        String plate = plateField.getText().trim();
        if (plate.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a plate number.");
            return;
        }

        ParkingSession session = store.getOpenSessionByPlate(plate);
        if (session == null) {
            JOptionPane.showMessageDialog(this, "Vehicle not found.");
            return;
        }

        receiptArea.setText("Vehicle Found:\n"
                + "Ticket No: " + session.getTicketNo() + "\n"
                + "Plate: " + session.getPlate() + "\n"
                + "Spot: " + session.getSpotId() + "\n"
                + "Entry Time: " + session.getEntryTime() + "\n"
        );

        // Select vehicle in the list
        vehiclesInsideList.setSelectedValue(session.getPlate() + " (" + session.getSpotId() + ")", true);
    }

    private void processPaymentExit() {
        String plate = plateField.getText().trim();
        if (plate.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Enter plate number first.");
            return;
        }

        ParkingSession session = store.getOpenSessionByPlate(plate);
        if (session == null) {
            JOptionPane.showMessageDialog(this, "Vehicle not found.");
            return;
        }

        // Get exit time
        String exitTime = timeDisplay.getText();

        // Calculate fees & fines
        PaymentRecord record = exitService.processExit(plate, exitTime);

        // Payment Method
        PaymentMethod method = (PaymentMethod) methodCombo.getSelectedItem();

        // Card validation if CARD selected
        if (method == PaymentMethod.CARD) {
            String card = cardField.getText().trim();
            if (!card.matches("\\d{16}")) {
                JOptionPane.showMessageDialog(this, "Invalid card number (16 digits required).");
                return;
            }
        }

        // Amount paid
        double amountPaid;
        try {
            amountPaid = Double.parseDouble(amountPaidField.getText().trim());
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Invalid amount entered.");
            return;
        }

        double balance = amountPaid - record.getTotalDue();
        changeField.setText(String.format("RM %.2f", Math.max(balance, 0)));

        // Update PaymentProcessor (partial payment allowed)
        paymentProcessor.processPayment(
                session.getTicketNo(),
                plate,
                record.getParkingFee(),
                record.getFinePaid(),
                method,
                amountPaid
        );

        JOptionPane.showMessageDialog(this, "Payment processed successfully.");

        refreshVehiclesInside();
        receiptArea.setText(generateReceipt(record, amountPaid));
    }

    private String generateReceipt(PaymentRecord p, double amountPaid) {
        double balance = amountPaid - p.getTotalDue();
        return  "========= PARKING RECEIPT =========\n" +
                "Ticket No:      " + p.getTicketNo() + "\n" +
                "Plate:          " + p.getPlate() + "\n" +
                "Exit Time:      " + p.getPaidTime() + "\n" +
                "-----------------------------------\n" +
                "Parking Fee:    RM " + String.format("%.2f", p.getParkingFee()) + "\n" +
                "Fines Due:      RM " + String.format("%.2f", p.getFinePaid()) + "\n" +
                "TOTAL DUE:      RM " + String.format("%.2f", p.getTotalDue()) + "\n" +
                "-----------------------------------\n" +
                "Payment Method: " + p.getMethod() + "\n" +
                "Amount Paid:    RM " + String.format("%.2f", amountPaid) + "\n" +
                "Balance/Change: RM " + String.format("%.2f", Math.max(balance, 0)) + "\n" +
                "===================================\n";
    }
}
