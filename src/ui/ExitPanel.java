package ui;

import data.DataStore;
import enums.PaymentMethod;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
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

    private JCheckBox manualTimeCheck;

    private static final DateTimeFormatter TIME_FORMAT = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

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

        manualTimeCheck = new JCheckBox("Manual Exit Time");
        searchPanel.add(manualTimeCheck);

        timeDisplay = new JTextField(LocalDateTime.now().format(TIME_FORMAT), 19);
        timeDisplay.setEditable(false);
        timeDisplay.setEnabled(false);
        searchPanel.add(timeDisplay);

        methodCombo = new JComboBox<>(PaymentMethod.values());
        searchPanel.add(new JLabel("Payment Method:"));
        searchPanel.add(methodCombo);

        cardField = new JTextField(16);
        cardField.setEnabled(false);
        searchPanel.add(new JLabel("Card:"));
        searchPanel.add(cardField);

        // --- CENTER SECTION ---
        JPanel mainDisplayPanel = new JPanel(new BorderLayout(15, 0));

        listModel = new DefaultListModel<>();
        vehiclesInsideList = new JList<>(listModel);
        JScrollPane listScroll = new JScrollPane(vehiclesInsideList);
        listScroll.setBorder(BorderFactory.createTitledBorder("Vehicles Inside"));
        listScroll.setPreferredSize(new Dimension(220, 0));
        mainDisplayPanel.add(listScroll, BorderLayout.WEST);

        receiptArea = new JTextArea();
        receiptArea.setEditable(false);
        receiptArea.setFont(new Font("Monospaced", Font.PLAIN, 13));
        JScrollPane receiptScroll = new JScrollPane(receiptArea);
        receiptScroll.setBorder(BorderFactory.createTitledBorder("Exit Preview / Receipt"));
        mainDisplayPanel.add(receiptScroll, BorderLayout.CENTER);

        // --- BOTTOM SECTION ---
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        amountPaidField = new JTextField(8);
        bottomPanel.add(new JLabel("Amount Paid (RM):"));
        bottomPanel.add(amountPaidField);

        changeField = new JTextField(8);
        changeField.setEditable(false);
        bottomPanel.add(new JLabel("Change (RM):"));
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

        // ─── Listeners ──────────────────────────────────────────────────────

        // Manual time checkbox control
        manualTimeCheck.addActionListener(e -> {
            boolean manual = manualTimeCheck.isSelected();
            timeDisplay.setEditable(manual);
            timeDisplay.setEnabled(manual);
            timeDisplay.setBackground(manual ? new Color(255, 255, 210) : Color.WHITE);

            if (!manual) {
                timeDisplay.setText(LocalDateTime.now().format(TIME_FORMAT));
            }
            
            refreshPreview();  // refresh after toggle
        });

        // Auto-refresh preview when manual time is edited
        timeDisplay.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e)  { refreshPreview(); }
            @Override public void removeUpdate(DocumentEvent e)   { refreshPreview(); }
            @Override public void changedUpdate(DocumentEvent e)  { refreshPreview(); }
        });

        // Select vehicle from list → fill plate + show preview
        vehiclesInsideList.addListSelectionListener((ListSelectionEvent e) -> {
            if (!e.getValueIsAdjusting() && vehiclesInsideList.getSelectedValue() != null) {
                String selected = vehiclesInsideList.getSelectedValue();
                int endIndex = selected.indexOf(" (");
                if (endIndex > 0) {
                    String plate = selected.substring(0, endIndex).trim();
                    plateField.setText(plate);
                    refreshPreview();
                }
            }
        });

        // Payment method → enable/disable card field
        methodCombo.addActionListener(e -> {
            boolean isCard = methodCombo.getSelectedItem() == PaymentMethod.CARD;
            cardField.setEnabled(isCard);
            cardField.setBackground(isCard ? new Color(240, 248, 255) : Color.WHITE);
        });

        searchBtn.addActionListener(e -> refreshPreview());  // button now uses same logic
        processBtn.addActionListener(e -> processPaymentExit());

        refreshVehiclesInside();
    }

    private void refreshVehiclesInside() {
        listModel.clear();
        store.getAllActiveSessions().forEach(session ->
            listModel.addElement(session.getPlate() + " (" + session.getSpotId() + ")")
        );
    }

    private void refreshPreview() {
        String plate = plateField.getText().trim().toUpperCase();
        if (plate.isEmpty()) {
            receiptArea.setText("");
            return;
        }

        ParkingSession session = store.getOpenSessionByPlate(plate);
        if (session == null) {
            receiptArea.setText("Vehicle not found or already exited.");
            return;
        }

        String exitTimeStr = timeDisplay.getText().trim();

        if (!exitTimeStr.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}")) {
            receiptArea.setText("Invalid time format.\nUse: yyyy-MM-dd HH:mm:ss");
            return;
        }

        long durationHours;
        try {
            durationHours = exitService.calculateHours(session.getEntryTime(), exitTimeStr);
        } catch (Exception ex) {
            receiptArea.setText("Error parsing times:\n" + ex.getMessage());
            return;
        }

        PaymentRecord preview = exitService.calculateExitPreview(plate, exitTimeStr);
        if (preview == null) {
            receiptArea.setText("Error calculating fees.");
            return;
        }

        receiptArea.setText(
            "Vehicle Found:\n" +
            "Ticket No:      " + preview.getTicketNo() + "\n" +
            "Plate:          " + preview.getPlate() + "\n" +
            "Spot:           " + session.getSpotId() + "\n" +
            "Entry Time:     " + session.getEntryTime() + "\n" +
            "Exit Time:      " + exitTimeStr + "\n" +
            "Duration:       ≈ " + durationHours + " hours\n" +
            "----------------------------------------\n" +
            "Parking Fee:    RM " + String.format("%.2f", preview.getParkingFee()) + "\n" +
            "Fines Due:      RM " + String.format("%.2f", preview.getFinePaid()) + "\n" +
            "TOTAL DUE:      RM " + String.format("%.2f", preview.getTotalDue()) + "\n" +
            "----------------------------------------\n" +
            "Ready to pay → select method and amount."
        );

        vehiclesInsideList.setSelectedValue(session.getPlate() + " (" + session.getSpotId() + ")", true);
    }

    private void processPaymentExit() {
        String plate = plateField.getText().trim().toUpperCase();
        if (plate.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Enter plate number first.");
            return;
        }

        ParkingSession session = store.getOpenSessionByPlate(plate);
        if (session == null) {
            JOptionPane.showMessageDialog(this, "Vehicle not found.");
            return;
        }

        String exitTime = timeDisplay.getText().trim();

        // Validate time format
        if (!exitTime.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}")) {
            JOptionPane.showMessageDialog(this, "Invalid exit time format.");
            return;
        }

        PaymentMethod method = (PaymentMethod) methodCombo.getSelectedItem();

        if (method == PaymentMethod.CARD) {
            String card = cardField.getText().trim();
            if (!card.matches("\\d{16}")) {
                JOptionPane.showMessageDialog(this, "Invalid card number (16 digits required).");
                return;
            }
        }

        double amountPaid;
        try {
            amountPaid = Double.parseDouble(amountPaidField.getText().trim());
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Invalid amount entered.");
            return;
        }

        PaymentRecord preview = exitService.calculateExitPreview(plate, exitTime);
        if (preview == null) {
            JOptionPane.showMessageDialog(this, "Cannot calculate amount due.");
            return;
        }

        double totalDue = preview.getTotalDue();

        if (amountPaid < totalDue - 0.10) {
            JOptionPane.showMessageDialog(this,
                "Insufficient payment.\nTotal due: RM " + String.format("%.2f", totalDue),
                "Payment Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        PaymentRecord finalRecord = exitService.processExit(plate, exitTime);
        if (finalRecord == null) {
            JOptionPane.showMessageDialog(this, "Failed to process exit.");
            return;
        }

        try {
            paymentProcessor.processPayment(
                    finalRecord.getTicketNo(),
                    plate,
                    finalRecord.getParkingFee(),
                    finalRecord.getFinePaid(),
                    method,
                    amountPaid
            );
        } catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(this, e.getMessage());
            return;
        }

        double change = amountPaid - finalRecord.getTotalDue();
        changeField.setText(String.format("RM %.2f", Math.max(0, change)));

        receiptArea.setText(generateReceipt(finalRecord, amountPaid));

        JOptionPane.showMessageDialog(this, "Payment processed successfully.\nVehicle may now exit.");

        refreshVehiclesInside();
        plateField.setText("");
        amountPaidField.setText("");
        changeField.setText("");
        cardField.setText("");
        receiptArea.setText("");
    }

    private String generateReceipt(PaymentRecord p, double amountPaid) {
        double balance = amountPaid - p.getTotalDue();
        return "========= PARKING RECEIPT =========\n" +
               "Ticket No: " + p.getTicketNo() + "\n" +
               "Plate: " + p.getPlate() + "\n" +
               "Exit Time: " + p.getPaidTime() + "\n" +
               "-----------------------------------\n" +
               "Parking Fee:    RM " + String.format("%.2f", p.getParkingFee()) + "\n" +
               "Fines Due:      RM " + String.format("%.2f", p.getFinePaid()) + "\n" +
               "TOTAL DUE:      RM " + String.format("%.2f", p.getTotalDue()) + "\n" +
               "-----------------------------------\n" +
               "Payment Method: " + p.getMethod() + "\n" +
               "Amount Paid:    RM " + String.format("%.2f", amountPaid) + "\n" +
               "Change:         RM " + String.format("%.2f", Math.max(balance, 0)) + "\n" +
               "===================================\n";
    }
}