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
        refreshVehiclesInside();
    }

    private void initTopPanel() {
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        topPanel.add(new JLabel("License Plate:"));
        plateField = new JTextField(12);
        topPanel.add(plateField);

        hcCheckBox = new JCheckBox("Handicapped Card Holder?");
        topPanel.add(hcCheckBox);

        topPanel.add(new JLabel("Exit Time (yyyy-MM-ddTHH:mm):"));
        exitTimeField = new JTextField(16);
        exitTimeField.setText(LocalDateTime.now().format(DISPLAY_FORMAT));
        topPanel.add(exitTimeField);

        JButton searchBtn = new JButton("Calculate Parking Fees");
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
                long hours = previewRecord.getDurationHours();
                double unpaidFinesInDB = store.getUnpaidFinesByPlate(plate).stream().mapToDouble(FineRecord::getAmount).sum();
                double currentRate = hcCheckBox.isSelected() ? 2.0 : (currentSession.getSpotId().contains("RES") ? 10.0 : 5.0);
                double calculatedParkingFee = hours * currentRate;
                double totalDue = calculatedParkingFee + unpaidFinesInDB;
                String notificationMsg = String.format (
                    "Vehicle: %s\n" +
                    "Hours Parked: %d Hour(s)\n" +
                    "Parking Fee: RM %.2f\n" +
                    "Unpaid Fines: RM %.2f\n" +
                    "---------------------------\n" +
                    "TOTAL AMOUNT: RM %.2f\n\n" +
                    "Proceed to payment?",
                    plate, previewRecord.getDurationHours(), calculatedParkingFee, unpaidFinesInDB, totalDue
                );
                int response = JOptionPane.showConfirmDialog(this, notificationMsg, "Exit Preview - Pending Payment", JOptionPane.OK_CANCEL_OPTION, JOptionPane.INFORMATION_MESSAGE);

                if (response == JOptionPane.OK_OPTION) {
                    openPaymentDialog(); // If user clicks OK, open payment immediately
                }
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

        processBtn = new JButton("Confirm Payment & Exit");
        processBtn.setEnabled(false); // Only enabled via popup flow
        processBtn.setBackground(new Color(52, 152, 219));
        processBtn.setForeground(Color.WHITE);
        processBtn.setPreferredSize(new Dimension(220, 40));
        bottomPanel.add(processBtn, BorderLayout.EAST);
        add(bottomPanel, BorderLayout.SOUTH);

        //processBtn.addActionListener(e -> openPaymentDialog());
    }

    private void openPaymentDialog() {
        if (currentSession == null || previewRecord == null) return;
        LocalDateTime exitTime = previewRecord.getPaidTime();
        long hours = previewRecord.getDurationHours();

        double rate;

        if (hcCheckBox.isSelected()) {
            rate = 2.0;
        } else {
            String spotId = currentSession.getSpotId().toUpperCase();
            if (spotId.contains("COM")) rate = 2.0;         //Compact
            else if (spotId.contains("RES")) rate = 10.0;   //VIP
            else rate = 5.0;                                   //Regular
        }
        double parkingFee = hours * rate;

       List<FineRecord> pastFines = store.getUnpaidFinesByPlate(currentSession.getVehicle().getPlate());
       double totalPastFines = pastFines.stream().mapToDouble(FineRecord::getAmount).sum();

       List<FineRecord> newFines = new ArrayList<>();
       double currentFine = 0.0;

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
        double totalDueNow = parkingFee + totalFines;

        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Secure Payment", true);
        dialog.setLayout(new GridLayout(6, 2, 10, 10));
        dialog.setSize(450, 300);
        dialog.setLocationRelativeTo(this);
        dialog.add(new JLabel("  Payment Method:"));
        JComboBox<PaymentMethod> methodBox = new JComboBox<>(PaymentMethod.values());
        dialog.add(methodBox);
        dialog.add(new JLabel("  Parking Fee (RM):"));
        dialog.add(new JTextField(String.format("%.2f", parkingFee)) {{ setEditable(false); }});
        dialog.add(new JLabel("  Total Fines (RM):"));
        dialog.add(new JTextField(String.format("%.2f", totalPastFines + currentFine)) {{ setEditable(false); }});
        dialog.add(new JLabel("  TOTAL DUE (RM):"));
        dialog.add(new JTextField(String.format("%.2f", totalDueNow)) {{ setEditable(false); }});
        dialog.add(new JLabel("  Amount Paid (RM):"));
        JTextField paidField = new JTextField(String.format("%.2f", totalDueNow));
        dialog.add(paidField);

        JButton confirmBtn = new JButton("Complete Transaction");
        dialog.add(new JLabel());
        dialog.add(confirmBtn);

        confirmBtn.addActionListener(ev -> {
            try {
                double typedAmount = Double.parseDouble(paidField.getText().trim());
                PaymentMethod method = (PaymentMethod) methodBox.getSelectedItem();

                if (typedAmount < totalDueNow) {
                    JOptionPane.showMessageDialog(dialog, "Insufficient payment! Total due is RM " + totalDueNow);
                    return;
                }
                double finePaid = 0.0;
                double remainingAmount = typedAmount - parkingFee;

                for (FineRecord f : allFines) {
                    if (remainingAmount <= 0) break;
                    double toPay = Math.min(f.getAmount(), remainingAmount);
                    store.reduceFineAmount(f, toPay);
                    finePaid += toPay;
                    remainingAmount -= toPay;
                }
                PaymentRecord payment = new PaymentRecord( 
                    previewRecord.getTicketNo(),
                    previewRecord.getPlate(),
                    method,
                    exitTime,
                    (int)hours,
                    parkingFee,
                    finePaid,
                    parkingFee + finePaid
                );

                exitService.confirmExit(currentSession, exitTime, payment, false);

                // the PaymentMethod and Balance after clicking confirm.
                displayReceipt(payment);
                JOptionPane.showMessageDialog(dialog, "Payment Successful! Vehicle Can Now Exit.");
                dialog.dispose();

                refreshVehiclesInside();
                if (adminPanel != null) adminPanel.refreshStats();
                if (reportingPanel != null) reportingPanel.refreshStats();
                processBtn.setEnabled(false);
            }
            catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog, "Invalid numeric input.");
            }
        });
        dialog.setVisible(true);
    }

    private void displayReceipt(PaymentRecord record) {
        String plate = record.getPlate();
        double unpaidFinesInDB = store.getUnpaidFinesByPlate(plate).stream().mapToDouble(FineRecord::getAmount).sum();

        StringBuilder sb = new StringBuilder();
        sb.append("========= PARKING RECEIPT =========\n");
        sb.append("Ticket No:      ").append(record.getTicketNo()).append("\n");
        sb.append("Plate:          ").append(record.getPlate()).append("\n");
        sb.append("Exit Time:      ").append(record.getPaidTime()).append("\n");
        sb.append("-----------------------------------\n");
        sb.append("Hours Parked:   ").append(record.getDurationHours()).append(" Hour(s)\n");
        sb.append("Parking Fee:    RM ").append(String.format("%.2f", record.getParkingFee())).append("\n");
        sb.append("Unpaid Fines:   RM ").append(String.format("%.2f", unpaidFinesInDB)).append("\n");
        double totalDue = record.getParkingFee() + unpaidFinesInDB;
        sb.append("TOTAL AMOUNT:      RM ").append(String.format("%.2f", totalDue)).append("\n");
        sb.append("-----------------------------------\n");

        if (record.getMethod() != null) {
            sb.append("Payment Method: ").append(record.getMethod()).append("\n");
            sb.append("Amount Paid:    RM ").append(String.format("%.2f", record.getAmountPaid())).append("\n");
            sb.append("Balance/Change: RM ").append(String.format("%.2f", record.getBalance())).append("\n");
            sb.append("===================================\n");
            sb.append("Thank you for parking with us!     \n");
        }
        receiptArea.setText(sb.toString());
    }
    public void refreshVehiclesInside() {
        listModel.clear();
        store.getAllActiveSessions().forEach(session ->
                listModel.addElement(session.getPlate() + " (" + session.getSpotId() + ")")
        );
    }
    private void resetPanel() {
        plateField.setText("");
        hcCheckBox.setSelected(false);
        exitTimeField.setText(LocalDateTime.now().format(DISPLAY_FORMAT));
        processBtn.setEnabled(false);
        currentSession = null;
        previewRecord = null;
    }
}
