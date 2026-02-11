package com.group5.parkinglot.gui;

import com.group5.parkinglot.model.*;
import com.group5.parkinglot.model.ParkingLot;
import com.group5.parkinglot.model.ParkingSpot;
import com.group5.parkinglot.model.Vehicle;
import com.group5.parkinglot.model.ParkedVehicle;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.List;

public class EntryPanel extends JPanel {

    private final ParkingLot parkingLot;

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

    public EntryPanel(ParkingLot parkingLot) {
        this.parkingLot = parkingLot;

        setLayout(new BorderLayout(15, 15));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        add(createInputPanel(), BorderLayout.NORTH);
        add(createTablePanel(), BorderLayout.CENTER);
        add(createBottomPanel(), BorderLayout.SOUTH);
    }

    // ===============================
    // 1️⃣ INPUT PANEL
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
    // 2️⃣ TABLE PANEL
    // ===============================
    private JScrollPane createTablePanel() {

        String[] columns = {"Spot ID", "Floor", "Row", "Type", "Rate (RM/hr)"};

        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        spotTable = new JTable(tableModel);
        spotTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        return new JScrollPane(spotTable);
    }

    // ===============================
    // 3️⃣ BOTTOM PANEL
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
    // SEARCH AVAILABLE SPOTS
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

        // Clear old rows
        tableModel.setRowCount(0);

        List<ParkingSpot> spots = parkingLot.getAvailableSpots(pendingVehicle);

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
    // CONFIRM PARKING
    // ===============================
    private void handleConfirm(ActionEvent e) {

        int selectedRow = spotTable.getSelectedRow();

        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Select a parking spot!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String spotId = tableModel.getValueAt(selectedRow, 0).toString();

        ParkingSpot selectedSpot = parkingLot.findSpotById(spotId);

        if (selectedSpot == null) {
            JOptionPane.showMessageDialog(this, "Invalid spot selection!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            Ticket ticket = parkingLot.parkVehicle(pendingVehicle, selectedSpot);

            resultArea.setText(
                    "=== PARKING TICKET ===\n" +
                    "Ticket ID : " + ticket.getTicketId() + "\n" +
                    "Vehicle   : " + pendingVehicle.getLicensePlate() + "\n" +
                    "Spot      : " + selectedSpot.getSpotId() + "\n" +
                    "Type      : " + selectedSpot.getType() + "\n" +
                    "Entry     : " + ticket.getEntryTime() + "\n" +
                    "Rate      : RM " + selectedSpot.getCurrentRate() + "/hour\n"
            );

            // Reset
            tableModel.setRowCount(0);
            confirmButton.setEnabled(false);

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Parking Failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ===============================
    // VEHICLE FACTORY
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
