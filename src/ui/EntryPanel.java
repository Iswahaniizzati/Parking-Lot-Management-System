package ui;

import javax.swing.*;
import java.awt.*;
import data.DataStore;
import model.ParkingSpot;
import model.ParkingSession;
import enums.SpotType;
import java.util.List;

public class EntryPanel extends JPanel {
    private DataStore store;
    private JTextField plateField;
    private JComboBox<SpotType> typeCombo;
    private JCheckBox hcCheckBox;
    private JPanel gridPanel;
    private String selectedSpotId = null; // Track the chosen spot

    public EntryPanel(DataStore store) {
        this.store = store;
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
        typeCombo = new JComboBox<>(SpotType.values());
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
            if (plate.isEmpty() || selectedSpotId == null) {
                JOptionPane.showMessageDialog(this, "Please enter a plate and select a green spot!");
                return;
            }

            // 1. Create the session object
            String ticketNo = "TKT-" + (System.currentTimeMillis() % 10000);
            ParkingSession session = new ParkingSession(
                ticketNo, plate, selectedSpotId, java.time.LocalDateTime.now().toString()
            );

            // 2. Save to Database
            store.createSession(session);
            store.setSpotOccupied(selectedSpotId, plate);

            // 3. UI Feedback & Reset
            JOptionPane.showMessageDialog(this, "Ticket Printed: " + ticketNo);
            plateField.setText("");
            selectedSpotId = null;
            refreshSpotGrid(); // Turn the green spot RED
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
        // Fetch current status from Database
        for (ParkingSpot spot : store.getAllSpots()) {
            JButton spotBtn = new JButton(spot.getSpotId());
            spotBtn.setPreferredSize(new Dimension(80, 50));
            
            // Visual state based on DB status
            if (!spot.isAvailable()) {
                spotBtn.setBackground(Color.RED);
                spotBtn.setEnabled(false); 
            } else {
                spotBtn.setBackground(Color.GREEN);
                // Action Listener for selection
                spotBtn.addActionListener(e -> {
                    selectedSpotId = spot.getSpotId();
                    JOptionPane.showMessageDialog(this, "Selected Spot: " + selectedSpotId);
                });
            }
            
            spotBtn.setOpaque(true);
            spotBtn.setBorderPainted(true);
            gridPanel.add(spotBtn);
        }
        gridPanel.revalidate();
        gridPanel.repaint();
    }
}