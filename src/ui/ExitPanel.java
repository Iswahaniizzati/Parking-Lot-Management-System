package ui;

import data.DataStore;
import java.awt.*;
import java.awt.event.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import javax.swing.*;
import model.ParkingSession;
import service.ExitService;
import service.PaymentProcessor;

public class ExitPanel extends JPanel {
    private DataStore store;
    private ExitService exitService;
    private PaymentProcessor paymentProcessor;

    private JTextField plateField;
    private JTextArea receiptArea;
    private JList<String> vehiclesInsideList;
    private DefaultListModel<String> listModel;
    private JButton processBtn;

    private JTextField timeDisplay;
    private JCheckBox manualTimeCheck;

    private static final DateTimeFormatter formatter =
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

        plateField = new JTextField(15);
        searchPanel.add(plateField);

        JButton searchBtn = new JButton("Search Vehicle");
        searchPanel.add(searchBtn);

        manualTimeCheck = new JCheckBox("Manual Exit Time");
        searchPanel.add(manualTimeCheck);

        timeDisplay = new JTextField(LocalDateTime.now().format(formatter));
        timeDisplay.setEditable(false);
        timeDisplay.setEnabled(false);
        searchPanel.add(timeDisplay);

        // --- CENTER SECTION ---
        JPanel mainDisplayPanel = new JPanel(new BorderLayout(15, 0));

        listModel = new DefaultListModel<>();
        vehiclesInsideList = new JList<>(listModel);
        JScrollPane listScroll = new JScrollPane(vehiclesInsideList);
        listScroll.setBorder(BorderFactory.createTitledBorder("Vehicles Inside"));
        listScroll.setPreferredSize(new Dimension(200, 0));

        receiptArea = new JTextArea();
        receiptArea.setEditable(false);
        JScrollPane receiptScroll = new JScrollPane(receiptArea);
        receiptScroll.setBorder(BorderFactory.createTitledBorder("Exit Receipt / Invoice"));

        mainDisplayPanel.add(listScroll, BorderLayout.WEST);
        mainDisplayPanel.add(receiptScroll, BorderLayout.CENTER);

        // --- BOTTOM SECTION ---
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        processBtn = new JButton("Process Payment & Exit");
        processBtn.setBackground(new Color(52, 152, 219));
        processBtn.setForeground(Color.WHITE);
        processBtn.setFont(new Font("SansSerif", Font.BOLD, 12));
        processBtn.setPreferredSize(new Dimension(200, 40));
        bottomPanel.add(processBtn);

        add(searchPanel, BorderLayout.NORTH);
        add(mainDisplayPanel, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        // =========================
        // 🔹 EVENT LISTENERS
        // =========================

        // Enable manual time editing
        manualTimeCheck.addActionListener(e -> {
            boolean enabled = manualTimeCheck.isSelected();
            timeDisplay.setEnabled(enabled);
            timeDisplay.setEditable(enabled);

            if (!enabled) {
                timeDisplay.setText(LocalDateTime.now().format(formatter));
            }
        });

        // Search button
        searchBtn.addActionListener(e -> searchVehicle());

        // Click vehicle in list auto-fills plate
        vehiclesInsideList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                String selected = vehiclesInsideList.getSelectedValue();
                if (selected != null) {
                    String plate = selected.split(" ")[0];
                    plateField.setText(plate);
                }
            }
        });

        // Process exit
        processBtn.addActionListener(e -> processExit());

        // Initial Load
        refreshVehiclesInside();
    }

    // =========================
    // 🔹 SEARCH VEHICLE
    // =========================
    private void searchVehicle() {

        String plate = plateField.getText().trim();

        if (plate.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a plate number.");
            return;
        }

        ParkingSession session = store.getOpenSessionByPlate(plate);

        if (session == null) {
            JOptionPane.showMessageDialog(this, "No active parking session found.");
            return;
        }

        receiptArea.setText(
                "========== VEHICLE FOUND ==========\n" +
                "Ticket No:  " + session.getTicketNo() + "\n" +
                "Plate:      " + session.getPlate() + "\n" +
                "Spot:       " + session.getSpotId() + "\n" +
                "Entry Time: " + session.getEntryTime() + "\n" +
                "Status:     ACTIVE\n"
        );
    }


    // =========================
    // 🔹 PROCESS EXIT
    // =========================
    private void processExit() {

        String plate = plateField.getText().trim();

        if (plate.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Enter plate number first.");
            return;
        }

        ParkingSession session = store.getOpenSessionByPlate(plate);

        if (session == null) {
            JOptionPane.showMessageDialog(this, "No active session found.");
            return;
        }

        try {
            // Convert entry time
            LocalDateTime entryTime = LocalDateTime.parse(session.getEntryTime());
            LocalDateTime exitTime = LocalDateTime.now();

            long hours = java.time.Duration.between(entryTime, exitTime).toHours();
            if (hours <= 0) hours = 1;

            double ratePerHour = 5.0;
            double parkingFee = hours * ratePerHour;

            double fineAmount = store.getUnpaidFinesByPlate(plate)
                                    .stream()
                                    .mapToDouble(f -> f.getAmount())
                                    .sum();

            double totalDue = parkingFee + fineAmount;

            // For now assume full payment and CASH
            double amountPaid = totalDue;

            paymentProcessor.processPayment(
                    session.getTicketNo(),
                    plate,
                    parkingFee,
                    fineAmount,
                    enums.PaymentMethod.CASH,
                    amountPaid
            );

            receiptArea.setText(
                    "========== RECEIPT ==========\n" +
                    "Ticket: " + session.getTicketNo() + "\n" +
                    "Plate: " + plate + "\n" +
                    "Spot: " + session.getSpotId() + "\n" +
                    "Duration: " + hours + " hours\n" +
                    "Parking Fee: RM " + parkingFee + "\n" +
                    "Fines: RM " + fineAmount + "\n" +
                    "TOTAL PAID: RM " + totalDue + "\n" +
                    "Status: PAID\n"
            );

            refreshVehiclesInside();
            plateField.setText("");

            JOptionPane.showMessageDialog(this, "Exit completed successfully!");

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error processing exit.");
        }
    }




    // =========================
    // 🔹 REFRESH LIST
    // =========================
    private void refreshVehiclesInside() {
        listModel.clear();
        store.getAllActiveSessions().forEach(session -> {
            listModel.addElement(session.getPlate() + " (" + session.getSpotId() + ")");
        });
    }
}
