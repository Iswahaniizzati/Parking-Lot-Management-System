package ui;

import data.DataStore;
import enums.FineReason;
import enums.PaymentMethod;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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
    private final ReportingPanel reportingPanel;

    private JTextField plateField;
    private JTextField exitTimeField;
    private JTextArea receiptArea;
    private DefaultListModel<String> listModel;
    private JButton processBtn;
    private JCheckBox hcCheckBox;
    private JLabel revenueLabel;

    private ParkingSession currentSession;
    private PaymentRecord previewRecord;

    public ExitPanel(DataStore store, ExitService exitService,
                     PaymentProcessor paymentProcessor,
                     AdminPanel adminPanel, ReportingPanel reportingPanel) {
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

        JButton searchBtn = new JButton("Preview Exit");
        topPanel.add(searchBtn);
        add(topPanel, BorderLayout.NORTH);

        searchBtn.addActionListener(e -> previewVehicleExit());
    }

    private void previewVehicleExit() {
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

            previewRecord = exitService.previewExit(currentSession, exitTime);
            if (previewRecord != null) {
                displayReceipt(previewRecord);
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

        processBtn = new JButton("Confirm Payment & Exit");
        processBtn.setEnabled(false);
        processBtn.setBackground(new Color(52, 152, 219));
        processBtn.setForeground(Color.WHITE);
        processBtn.setPreferredSize(new Dimension(220, 40));
        bottomPanel.add(processBtn, BorderLayout.EAST);
        add(bottomPanel, BorderLayout.SOUTH);

        processBtn.addActionListener(e -> openPaymentDialog());
    }

    private void openPaymentDialog() {
        if (currentSession == null || previewRecord == null) return;

        LocalDateTime exitTime = previewRecord.getPaidTime();
        double parkingFee = previewRecord.getParkingFee();

        List<FineRecord> pastFines = store.getUnpaidFinesByPlate(currentSession.getVehicle().getPlate());

        long hours = previewRecord.getDurationHours();
        List<FineRecord> newFines = new ArrayList<>();
        if (hours > 24) {
            newFines.add(new FineRecord(currentSession.getVehicle().getPlate(),
                    FineReason.OVERSTAY_24H,
                    exitService.getActiveFineScheme().calculateFine(hours - 24),
                    exitTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                    false));
        }
        if (currentSession.getSpotId().contains("RES") && !currentSession.getVehicle().isVIP()) {
            newFines.add(new FineRecord(currentSession.getVehicle().getPlate(),
                    FineReason.RESERVED_VIOLATION,
                    100.0,
                    exitTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                    false));
        }

        List<FineRecord> allFines = new ArrayList<>();
        allFines.addAll(pastFines);
        allFines.addAll(newFines);

        double totalFines = allFines.stream().mapToDouble(FineRecord::getAmount).sum();

        // --- Payment Dialog ---
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Payment", true);
        dialog.setLayout(new GridLayout(5, 2, 10, 10));
        dialog.setSize(400, 230);
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
                double typedAmount = Double.parseDouble(paidField.getText().trim());
                PaymentMethod method = (PaymentMethod) methodBox.getSelectedItem();

                if (typedAmount < parkingFee) {
                    JOptionPane.showMessageDialog(dialog,
                            "You must pay at least the full parking fee to exit!");
                    return;
                }

                double finePaid = 0.0;
                double remainingAmount = typedAmount - parkingFee;

                List<FineRecord> unpaidFines = store.getUnpaidFinesByPlate(currentSession.getVehicle().getPlate());
                for (FineRecord f : unpaidFines) {
                    if (remainingAmount <= 0) break;
                    double toPay = Math.min(f.getAmount(), remainingAmount);
                    store.reduceFineAmount(f, toPay);
                    finePaid += toPay;
                    remainingAmount -= toPay;
                }

                double totalPaid = parkingFee + finePaid;

                PaymentRecord payment = new PaymentRecord(
                        previewRecord.getTicketNo(),
                        previewRecord.getPlate(),
                        method,
                        exitTime,
                        previewRecord.getDurationHours(),
                        parkingFee,
                        finePaid,
                        totalPaid
                );

                exitService.confirmExit(currentSession, exitTime, payment, false);

                JOptionPane.showMessageDialog(dialog, "Payment & Exit Successful!");
                dialog.dispose();

                refreshVehiclesInside();
                if (adminPanel != null) adminPanel.refreshStats();
                if (reportingPanel != null) reportingPanel.refreshStats();
                resetPanel();

            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog, "Invalid amount entered!");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, "Error processing payment: " + ex.getMessage());
            }
        });

        dialog.setVisible(true);
    }

    private void displayReceipt(PaymentRecord record) {
        String plate = record.getPlate();
        ParkingSession session = store.getOpenSessionByPlate(plate);

        List<FineRecord> unpaidFines = store.getUnpaidFinesByPlate(plate);
        List<FineRecord> sessionFines = new ArrayList<>();
        if (session != null) {
            long hours = record.getDurationHours();
            sessionFines = exitService.getActiveFineScheme() != null ?
                    exitService.generateFinalFines(session, hours,
                            record.getPaidTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                            exitService.getActiveFineScheme()) :
                    new ArrayList<>();
        }

        List<FineRecord> allFines = new ArrayList<>();
        allFines.addAll(unpaidFines);
        allFines.addAll(sessionFines);

        double totalFines = allFines.stream().mapToDouble(FineRecord::getAmount).sum();
        double remainingFines = Math.max(0, totalFines - record.getFinePaid());

        StringBuilder sb = new StringBuilder();
        sb.append("========= PARKING RECEIPT =========\n");
        sb.append("Ticket No:      ").append(record.getTicketNo()).append("\n");
        sb.append("Plate:          ").append(record.getPlate()).append("\n");
        sb.append("Exit Time:      ").append(record.getPaidTime()).append("\n");
        sb.append("-----------------------------------\n");
        sb.append("Parking Fee:    RM ").append(String.format("%.2f", record.getParkingFee())).append("\n");
        sb.append("Fines Paid:     RM ").append(String.format("%.2f", record.getFinePaid())).append("\n");
        sb.append("Remaining Fines: RM ").append(String.format("%.2f", remainingFines)).append("\n");
        sb.append("TOTAL DUE:      RM ").append(String.format("%.2f", record.getAmountPaid())).append("\n");
        sb.append("-----------------------------------\n");
        sb.append("Payment Method: ").append(record.getMethod()).append("\n");
        sb.append("Amount Paid:    RM ").append(String.format("%.2f", record.getAmountPaid())).append("\n");
        sb.append("Balance/Change: RM ").append(String.format("%.2f", record.getBalance())).append("\n");
        sb.append("===================================\n");

        receiptArea.setText(sb.toString());
    }

    public void refreshVehiclesInside() {
        listModel.clear();
        store.getAllActiveSessions().forEach(session ->
                listModel.addElement(session.getPlate() + " (" + session.getSpotId() + ")")
        );
        double totalRev = store.getTotalRevenue();
        revenueLabel.setText(String.format("Total Revenue: RM %.2f", totalRev));
    }

    private void resetPanel() {
        receiptArea.setText("");
        plateField.setText("");
        hcCheckBox.setSelected(false);
        exitTimeField.setText(LocalDateTime.now().format(DISPLAY_FORMAT));
        processBtn.setEnabled(false);
        currentSession = null;
        previewRecord = null;
    }
}
