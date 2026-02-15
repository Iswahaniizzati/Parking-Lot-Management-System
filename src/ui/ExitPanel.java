package ui;

import data.DataStore;
import enums.PaymentMethod;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javax.swing.*;
import model.FineRecord;
import model.ParkingSession;
import model.PaymentRecord;
import service.ExitService;
import service.PaymentProcessor;

public class ExitPanel extends JPanel {

    private static final DateTimeFormatter DISPLAY_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    private final DataStore store;
    private final ExitService exitService;
    private final PaymentProcessor paymentProcessor;
    private final AdminPanel adminPanel;
    private final ReportingPanel reportingPanel; // CHANGED from ReportingPanel

    private JTextField plateField;
    private JTextField exitTimeField;
    private JTextArea receiptArea;
    private DefaultListModel<String> listModel;
    private JButton processBtn;
    private JCheckBox hcCheckBox;
    private JLabel revenueLabel; 

    private ParkingSession currentSession;
    private PaymentRecord currentRecord;

    // CONSTRUCTOR CHANGED to accept AdminPanel
    public ExitPanel(DataStore store, ExitService exitService, PaymentProcessor paymentProcessor, AdminPanel adminPanel, ReportingPanel reportingPanel) {
        this.store = store;
        this.exitService = exitService;
        this.paymentProcessor = paymentProcessor;
        this.adminPanel = adminPanel;
        this.reportingPanel = reportingPanel;

        setLayout(new BorderLayout(15, 15));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        initTopPanel();
        initCenterPanel();
        initBottomPanel();
        refreshVehiclesInside(); 
    }

    private void initTopPanel() {
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        topPanel.add(new JLabel("License Plate:"));
        plateField = new JTextField(12);
        topPanel.add(plateField);

        hcCheckBox = new JCheckBox("HC Card Holder?");
        topPanel.add(hcCheckBox);

        topPanel.add(new JLabel("Exit Time (yyyy-MM-ddTHH:mm):"));
        exitTimeField = new JTextField(16);
        exitTimeField.setText(LocalDateTime.now().format(DISPLAY_FORMAT));
        topPanel.add(exitTimeField);

        JButton searchBtn = new JButton("Search Vehicle");
        topPanel.add(searchBtn);
        add(topPanel, BorderLayout.NORTH);

        searchBtn.addActionListener(e -> searchVehicle());
    }

    private void searchVehicle() {
        String plate = plateField.getText().trim().toUpperCase().replace("O", "0");
        String exitText = exitTimeField.getText().trim();

        if (plate.isEmpty() || exitText.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter plate and exit time.");
            return;
        }

        try {
            LocalDateTime exitTime = LocalDateTime.parse(exitText);
            currentSession = store.getOpenSessionByPlate(plate);

            if (currentSession == null) {
                JOptionPane.showMessageDialog(this, "Vehicle not found or already exited!");
                receiptArea.setText("");
                processBtn.setEnabled(false);
                return;
            }

            currentRecord = exitService.processExit(currentSession, exitTime);
            if (currentRecord != null) {
                displayReceipt(currentRecord);
                processBtn.setEnabled(true);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Invalid time format! Use yyyy-MM-ddTHH:mm");
        }
    }

    private void initCenterPanel() {
        JPanel centerPanel = new JPanel(new BorderLayout(15, 0));
        listModel = new DefaultListModel<>();
        JList<String> vehiclesList = new JList<>(listModel);
        
        vehiclesList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && vehiclesList.getSelectedValue() != null) {
                String selected = vehiclesList.getSelectedValue();
                plateField.setText(selected.split(" ")[0]);
            }
        });

        JScrollPane listScroll = new JScrollPane(vehiclesList);
        listScroll.setBorder(BorderFactory.createTitledBorder("Vehicles Inside"));
        listScroll.setPreferredSize(new Dimension(200, 0));
        centerPanel.add(listScroll, BorderLayout.WEST);

        receiptArea = new JTextArea();
        receiptArea.setFont(new Font("Monospaced", Font.PLAIN, 13));
        receiptArea.setEditable(false);
        JScrollPane receiptScroll = new JScrollPane(receiptArea);
        receiptScroll.setBorder(BorderFactory.createTitledBorder("Exit Receipt"));
        centerPanel.add(receiptScroll, BorderLayout.CENTER);

        add(centerPanel, BorderLayout.CENTER);
    }

    private void initBottomPanel() {
        JPanel bottomPanel = new JPanel(new BorderLayout());
        revenueLabel = new JLabel("Total Revenue: RM 0.00");
        revenueLabel.setFont(new Font("Arial", Font.BOLD, 14));
        bottomPanel.add(revenueLabel, BorderLayout.WEST);

        processBtn = new JButton("Process Payment & Exit");
        processBtn.setEnabled(false);
        processBtn.setBackground(new Color(52, 152, 219));
        processBtn.setForeground(Color.WHITE);
        processBtn.setPreferredSize(new Dimension(200, 40));

        bottomPanel.add(processBtn, BorderLayout.EAST);
        add(bottomPanel, BorderLayout.SOUTH);

        processBtn.addActionListener(e -> openPaymentDialog());
    }

    private void openPaymentDialog() {
        if (currentSession == null || currentRecord == null) return;

        LocalDateTime exitTime = LocalDateTime.parse(exitTimeField.getText().trim());
        double parkingFee = currentRecord.getParkingFee();
        double totalFines = store.getUnpaidFinesByPlate(currentSession.getPlate())
                .stream().mapToDouble(FineRecord::getAmount).sum();

        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Payment", true);
        dialog.setLayout(new GridLayout(5, 2, 10, 10));
        dialog.setSize(350, 220);
        dialog.setLocationRelativeTo(this);

        dialog.add(new JLabel("Payment Method:"));
        JComboBox<PaymentMethod> methodBox = new JComboBox<>(PaymentMethod.values());
        dialog.add(methodBox);

        dialog.add(new JLabel("Parking Fee (RM):"));
        dialog.add(new JTextField(String.format("%.2f", parkingFee)) {{ setEditable(false); }});

        dialog.add(new JLabel("Total Fines Due (RM):"));
        dialog.add(new JTextField(String.format("%.2f", totalFines)) {{ setEditable(false); }});

        dialog.add(new JLabel("Amount to Pay (RM):"));
        JTextField paidField = new JTextField(String.format("%.2f", parkingFee + totalFines));
        dialog.add(paidField);

        JButton confirmBtn = new JButton("Confirm Payment");
        dialog.add(new JLabel());
        dialog.add(confirmBtn);

        confirmBtn.addActionListener(ev -> {
            try {
                double amountPaid = Double.parseDouble(paidField.getText().trim());
                PaymentMethod method = (PaymentMethod) methodBox.getSelectedItem();

                boolean success = paymentProcessor.processPartialPayment(currentSession, method, amountPaid, exitTime);

                if (success) {
                    JOptionPane.showMessageDialog(dialog, "Payment Successful!");
                    dialog.dispose();
                    
                    refreshVehiclesInside(); // Update local list
                    if (adminPanel != null) adminPanel.refreshStats();
                    if (reportingPanel != null) reportingPanel.refreshStats(); // Update Admin Dashboard
                    resetPanel();
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, "Error processing payment.");
            }
        });
        dialog.setVisible(true);
    }

    private void displayReceipt(PaymentRecord record) {
        List<FineRecord> unpaidFines = store.getUnpaidFinesByPlate(record.getPlate());
        paymentProcessor.printReceipt(record, unpaidFines, receiptArea);
    }

    public void refreshVehiclesInside() {
        listModel.clear();
        store.getAllActiveSessions().forEach(session ->
                listModel.addElement(session.getPlate() + " (" + session.getSpotId() + ")")
        );
        double totalRev = store.getTotalRevenue();
        if (revenueLabel != null) revenueLabel.setText(String.format("Total Revenue: RM %.2f", totalRev));
    }

    private void resetPanel() {
        receiptArea.setText("");
        plateField.setText("");
        hcCheckBox.setSelected(false);
        exitTimeField.setText(LocalDateTime.now().format(DISPLAY_FORMAT));
        processBtn.setEnabled(false);
        currentSession = null;
        currentRecord = null;
    }
}