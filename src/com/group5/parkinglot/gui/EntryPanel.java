package com.group5.parkinglot.gui;

import com.group5.parkinglot.model.*;
import com.group5.parkinglot.service.ParkingService;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class EntryPanel extends JPanel {

    private final ParkingService parkingService;

    // Input fields
    private JTextField licenseField;
    private JComboBox<VehicleType> typeCombo;
    private JCheckBox handicapCheck;

    // Table
    private JTable spotTable;
    private DefaultTableModel tableModel;

    // Buttons
    private JButton searchButton;
    private JButton confirmButton;

    // Result
    private JTextArea resultArea;

    // Temporary vehicle (after search)
    private Vehicle pendingVehicle;

    // Callback after parking
    private Runnable afterParkingCallback;

    public EntryPanel(ParkingService parkingService) {
        this.parkingService = parkingService;

        setLayout(new BorderLayout(15, 15));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        add(createInputPanel(), BorderLayout.NORTH);
        add(createTablePanel(), BorderLayout.CENTER);
        add(createBottomPanel(), BorderLayout.SOUTH);
    }

    // Callback setter
    public void setAfterParkingCallback(Runnable callback) {
        this.afterParkingCallback = callback;
    }

    // ===============================
    // 1️⃣ Input Panel
    // ===============================
    private JPanel createInputPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Vehicle Entry"));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.anchor = GridBagConstraints.WEST;

        // License
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("License Plate:"), gbc);
        gbc.gridx = 1;
        licenseField = new JTextField(15);
        panel.add(licenseField, gbc);

        // Vehicle type
        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("Vehicle Type:"), gbc);
        gbc.gridx = 1;
        typeCombo = new JComboBox<>(VehicleType.values());
        panel.add(typeCombo, gbc);

        // Handicap
        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(new JLabel("Handicap Card:"), gbc);
        gbc.gridx = 1;
        handicapCheck = new JCheckBox();
        panel.add(handicapCheck, gbc);

        // Search Button
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        searchButton = new JButton("Search Available Spots");
        searchButton.addActionListener(this::handleSearch);
        panel.add(searchButton, gbc);

        return panel;
    }

    // ===============================
    // 2️⃣ Table Panel
    // ===============================
    private JScrollPane createTablePanel() {
        String[] columns = {"Spot ID", "Floor", "Row", "Type", "Rate (RM/hr)"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };

        spotTable = new JTable(tableModel);
        spotTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        return new JScrollPane(spotTable);
    }

    // ===============================
    // 3️⃣ Bottom Panel
    // ===============================
    private JPanel createBottomPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));

        confirmButton = new JButton("Confirm Parking");
        confirmButton.setEnabled(false);
        confirmButton.addActionListener(this::handleConfirm);

        resultArea = new JTextArea(6, 40);
        resultArea.setEditable(false);
        resultArea.setFont(new Font("Monospaced", Font.PLAIN, 13));

        panel.add(confirmButton, BorderLayout.NORTH);
        panel.add(new JScrollPane(resultArea), BorderLayout.CENTER);

        return panel;
    }

    // ===============================
    // Search Available Spots
    // ===============================
    private void handleSearch(ActionEvent e) {
        String license = licenseField.getText().trim();
        if (license.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Enter license plate!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        VehicleType type = (VehicleType) typeCombo.getSelectedItem();
        boolean hasCard = handicapCheck.isSelected();
        pendingVehicle = createVehicle(type, license, hasCard);

        if (pendingVehicle == null) return;

        tableModel.setRowCount(0);
        List<ParkingSpot> spots = parkingService.getFloors().stream()
                .flatMap(floor -> floor.getAvailableSpotsForVehicle(pendingVehicle).stream())
                .toList();

        if (spots.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No suitable spots available!", "No Spot", JOptionPane.WARNING_MESSAGE);
            confirmButton.setEnabled(false);
            return;
        }

        for (ParkingSpot spot : spots) {
            tableModel.addRow(new Object[]{
                    spot.getSpotId(),
                    spot.getFloorNumber(),
                    spot.getRowNumber(),
                    spot.getType(),
                    spot.getCurrentRate()
            });
        }

        confirmButton.setEnabled(true);
    }

    // ===============================
    // Confirm Parking
    // ===============================
    private void handleConfirm(ActionEvent e) {
        int selectedRow = spotTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Select a parking spot!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String spotId = tableModel.getValueAt(selectedRow, 0).toString();
        ParkingSpot selectedSpot = parkingService.getFloors().stream()
                .flatMap(floor -> floor.getSpots().stream())
                .filter(s -> s.getSpotId().equals(spotId))
                .findFirst().orElse(null);

        if (selectedSpot == null) {
            JOptionPane.showMessageDialog(this, "Invalid spot selection!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            ActiveTicket ticket = parkingService.parkVehicle(pendingVehicle, selectedSpot);

            resultArea.setText(
                    "=== PARKING TICKET ===\n" +
                    "Ticket ID : " + ticket.getTicketId() + "\n" +
                    "Vehicle   : " + ticket.getLicensePlate() + "\n" +
                    "Spot      : " + ticket.getSpotId() + "\n" +
                    "Type      : " + ticket.getVehicleType() + "\n" +
                    "Entry     : " + ticket.getEntryTime() + "\n"
            );

            tableModel.setRowCount(0);
            confirmButton.setEnabled(false);

            // Trigger callback for MainFrame
            if (afterParkingCallback != null) afterParkingCallback.run();

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Parking Failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ===============================
    // Vehicle Factory
    // ===============================
    private Vehicle createVehicle(VehicleType type, String license, boolean hasCard) {
        return switch (type) {
            case MOTORCYCLE -> new Motorcycle(license);
            case CAR -> new Car(license);
            case SUV_TRUCK -> new SUVTruck(license);
            case HANDICAPPED_VEHICLE -> new HandicappedVehicle(license, hasCard);
        };
    }
}
