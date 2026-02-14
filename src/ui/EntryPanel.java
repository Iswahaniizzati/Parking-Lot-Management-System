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
        confirmBtn.setBackground(new Color(46, 204, 113)); // Green
        confirmBtn.setForeground(Color.WHITE);
        confirmBtn.setFont(new Font("SansSerif", Font.BOLD, 14));
        gbc.gridy = 5;
        gbc.weighty = 1.0; 
        gbc.anchor = GridBagConstraints.NORTH;
        formPanel.add(confirmBtn, gbc);

        // --- Confirm Action Listener ---
        confirmBtn.addActionListener(e -> {
            String plate = plateField.getText().trim().toUpperCase();
            if (plate.isEmpty()) {
                JOptionPane.showMessageDialog(this, 
                    "Please enter the license plate.", 
                    "Input Required", JOptionPane.WARNING_MESSAGE);
                return;
            }

            if (selectedSpotId == null) {
                JOptionPane.showMessageDialog(this, 
                    "Please select an available parking spot.", 
                    "Selection Required", JOptionPane.WARNING_MESSAGE);
                return;
            }

            // Optional: Check HC spot requirement
            ParkingSpot selectedSpot = store.getSpotById(selectedSpotId);
            if (selectedSpot.getType() == SpotType.HC && !hcCheckBox.isSelected()) {
                JOptionPane.showMessageDialog(this, 
                    "This is an HC (Handicap) spot.\nPlease select if the vehicle has HC privileges.", 
                    "Spot Restriction", JOptionPane.WARNING_MESSAGE);
                return;
            }

            // Create session (time is now auto-set in ParkingSession constructor)
            String ticketNo = "TKT-" + (System.currentTimeMillis() % 1000000); // simple but better uniqueness
            ParkingSession session = new ParkingSession(ticketNo, plate, selectedSpotId);

            // Save session & update spot status
            store.createSession(session);
            store.setSpotOccupied(selectedSpotId, plate);

            // Success feedback
            JOptionPane.showMessageDialog(this, 
                "Vehicle entry recorded!\n" +
                "Ticket No: " + ticketNo + "\n" +
                "Plate: " + plate + "\n" +
                "Spot: " + selectedSpotId + "\n" +
                "Entry Time: " + session.getEntryTime(),
                "Entry Successful", JOptionPane.INFORMATION_MESSAGE);

            // Reset form
            plateField.setText("");
            typeCombo.setSelectedIndex(0);
            hcCheckBox.setSelected(false);
            selectedSpotId = null;
            refreshSpotGrid(); // Refresh grid colors & labels
        });

        // --- RIGHT: Visual Spot Grid ---
        gridPanel = new JPanel(new GridLayout(0, 5, 10, 10));
        gridPanel.setBorder(BorderFactory.createTitledBorder("Available Parking Spots"));
        
        refreshSpotGrid();

        add(formPanel, BorderLayout.WEST);
        add(new JScrollPane(gridPanel), BorderLayout.CENTER);
    }

    private void refreshSpotGrid() {
        gridPanel.removeAll();

        for (ParkingSpot spot : store.getAllSpots()) {
            JButton spotBtn = new JButton();
            spotBtn.setPreferredSize(new Dimension(90, 60));
            spotBtn.setFont(new Font("SansSerif", Font.BOLD, 12));

            if (!spot.isAvailable()) {
                spotBtn.setBackground(new Color(220, 53, 69)); // Red
                spotBtn.setForeground(Color.WHITE);
                spotBtn.setText(spot.getSpotId() + "\nOCCUPIED");
                spotBtn.setEnabled(false);
            } else {
                spotBtn.setBackground(new Color(40, 167, 69)); // Green
                spotBtn.setForeground(Color.WHITE);
                spotBtn.setText(spot.getSpotId() + "\nAVAILABLE\n" + spot.getType());

                // Selection listener
                spotBtn.addActionListener(ev -> {
                    selectedSpotId = spot.getSpotId();
                    // Optional: visual highlight
                    for (Component c : gridPanel.getComponents()) {
                        if (c instanceof JButton) {
                            c.setBorder(BorderFactory.createEmptyBorder());
                        }
                    }
                    spotBtn.setBorder(BorderFactory.createLineBorder(new Color(0, 123, 255), 4));
                    JOptionPane.showMessageDialog(this, 
                        "Spot selected: " + selectedSpotId + 
                        "\nType: " + spot.getType(), 
                        "Spot Selected", JOptionPane.INFORMATION_MESSAGE);
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