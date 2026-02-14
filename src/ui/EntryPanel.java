package ui;

import javax.swing.*;
import java.awt.*;
import data.DataStore;
import model.ParkingSpot;
import service.EntryService;
import model.ParkingSession;
import enums.SpotType;
import java.util.List;

public class EntryPanel extends JPanel {
    private DataStore store;
    private EntryService entryService;
    private JTextField plateField;
    private JComboBox<String> typeCombo;
    private JCheckBox hcCheckBox;
    private JPanel gridPanel;
    private String selectedSpotId = null; // Track the chosen spot

    public EntryPanel(DataStore store, EntryService entryService) {
        this.store = store;
        this.entryService = entryService;
        setLayout(new BorderLayout(20, 20));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // --- LEFT: Registration Form ---
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createTitledBorder("Vehicle Registration"));
        formPanel.setPreferredSize(new Dimension(350, 0));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;

        // License Plate Label & Field
        gbc.gridy = 0;
        formPanel.add(new JLabel("License Plate:"), gbc);
        plateField = new JTextField(15);
        gbc.gridy = 1;
        formPanel.add(plateField, gbc);

        // Vehicle Type Label & Combo
        gbc.gridy = 2;
        formPanel.add(new JLabel("Vehicle Type:"), gbc);
        String[] vehicleTypes = {"Motorcycle", "Car", "SUV/Truck", "Handicapped"};
        typeCombo = new JComboBox<>(vehicleTypes);
        typeCombo.addActionListener(e -> refreshSpotGrid());
        gbc.gridy = 3;
        formPanel.add(typeCombo, gbc);

        // HC Card Checkbox
        hcCheckBox = new JCheckBox("Has HC Card Holder?");
        gbc.gridy = 4;
        formPanel.add(hcCheckBox, gbc);

        // Confirm Button
        JButton confirmBtn = new JButton("Confirm & Print Ticket");
        confirmBtn.setBackground(new Color(46, 204, 113)); // Green color
        confirmBtn.setForeground(Color.WHITE);
        confirmBtn.setFont(new Font("SansSerif", Font.BOLD, 14));
        gbc.gridy = 5;
        gbc.weighty = 1.0; 
        gbc.anchor = GridBagConstraints.NORTH;
        formPanel.add(confirmBtn, gbc);

        // --- ADD ACTION LISTENER TO CONFIRM BUTTON ---
        confirmBtn.addActionListener(e -> {
            String plate = plateField.getText().trim();
            String type = typeCombo.getSelectedItem().toString();

            if (plate.isEmpty() || selectedSpotId == null) {
                JOptionPane.showMessageDialog(this, "Please enter a plate and select a green spot!");
                return;
            }

            // 1. Create the session object
            String ticketNo = entryService.registerVehicleEntry(plate, type, selectedSpotId);

            if (ticketNo != null) {
                JOptionPane.showMessageDialog(this, "Entry Successful!\nTicket Printed: " + ticketNo);

                plateField.setText("");
                selectedSpotId = null;
                refreshSpotGrid();
            } else {
                JOptionPane.showMessageDialog(this, "Error: This spot is not suitable for a " + type + "!");
            }
        });

        // --- RIGHT: Visual Spot Grid ---
        gridPanel = new JPanel(new GridLayout(0, 5, 10, 10));
        gridPanel.setBorder(BorderFactory.createTitledBorder("Select an Available Spot"));
        
        refreshSpotGrid();

        add(formPanel, BorderLayout.WEST);
        add(new JScrollPane(gridPanel), BorderLayout.CENTER);
    }

    private void refreshSpotGrid() {
        gridPanel.removeAll();
        String selectedType = typeCombo.getSelectedItem().toString().toUpperCase();
        // Fetch current status from Database
        for (ParkingSpot spot : store.getAllSpots()) {
            JButton spotBtn = new JButton(spot.getSpotId() + " (" + spot.getType() + ")");
            spotBtn.setPreferredSize(new Dimension(80, 50));
            
            // Visual state based on DB status
            if (!spot.isAvailable()) {
                spotBtn.setBackground(Color.RED);
                spotBtn.setEnabled(false); 
            } else {
                boolean isSuitable = checkSuitability(selectedType, spot.getType().toString());
                if (isSuitable) {
                    spotBtn.setBackground(Color.GREEN);
                    spotBtn.addActionListener(e -> {
                        selectedSpotId = spot.getSpotId();
                        JOptionPane.showMessageDialog(this, "Selected Spot: " + selectedSpotId);
                    });
                } else {
                    spotBtn.setBackground(Color.GRAY);
                    spotBtn.setEnabled(false); // Disable unsuitable spots
                }
            }
            gridPanel.add(spotBtn);
        }
        gridPanel.revalidate();
        gridPanel.repaint();
    }
    private boolean checkSuitability(String vType, String sType) {
        if (vType.equals("MOTORCYCLE")) return sType.equals("COMPACT");
        if (vType.equals("CAR")) return sType.equals("COMPACT") || sType.equals("REGULAR");
        if (vType.equals("SUV") || vType.equals("TRUCK")) return sType.equals("REGULAR");
        if (vType.equals("HANDICAPPED")) return true; // Can park anywhere
        return false;
    }
    
}