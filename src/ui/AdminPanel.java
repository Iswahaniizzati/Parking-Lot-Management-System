package ui;

import javax.swing.*;
import java.awt.*;
import service.ExitService;
import data.DataStore;
import model.ParkingSpot;
import java.util.List;

public class AdminPanel extends JPanel {
    private ExitService exitService;
    private DataStore store;
    private JLabel lblOccupancy, lblRevenue, lblUnpaidFines;

    public AdminPanel(ExitService exitService, DataStore store) {
        this.exitService = exitService;
        this.store = store;
        setLayout(new BorderLayout(15, 15));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // --- TOP SECTION: 3 Statistics Cards ---
        JPanel statsPanel = new JPanel(new GridLayout(1, 3, 20, 0));
        
        // Occupancy Card (Blue)
        lblOccupancy = createStatCard(statsPanel, "Occupancy Rate", "0", new Color(52, 152, 219));
        
        // Revenue Card (Green)
        lblRevenue = createStatCard(statsPanel, "Total Revenue", "RM 0.00", new Color(46, 204, 113));
        
        // Unpaid Fines Card (Red)
        lblUnpaidFines = createStatCard(statsPanel, "Unpaid Fines", "RM 0.00", new Color(231, 76, 60));
        
        add(statsPanel, BorderLayout.NORTH);

        // --- CENTER SECTION: Fine Management ---
        JPanel configPanel = new JPanel(new GridBagLayout());
        configPanel.setBorder(BorderFactory.createTitledBorder("Fine Scheme Configuration"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);

        String[] options = {"Fixed Fine (RM 50)", "Progressive (Tiered)", "Hourly (RM 20/hr)"};
        JComboBox<String> schemeDropdown = new JComboBox<>(options);
        JButton btnApply = new JButton("Update Fine Policy");
        
        gbc.gridx = 0; gbc.gridy = 0; configPanel.add(new JLabel("Active Scheme:"), gbc);
        gbc.gridx = 1; configPanel.add(schemeDropdown, gbc);
        gbc.gridx = 2; configPanel.add(btnApply, gbc);
        
        add(configPanel, BorderLayout.CENTER);

        btnApply.addActionListener(e -> {
            // Update logic here
            JOptionPane.showMessageDialog(this, "Policy Updated for future entries.");
            refreshStats();
        });

        // --- BOTTOM SECTION: Refresh Button ---
        JButton refreshBtn = new JButton("Refresh Dashboard & Spot Data");
        refreshBtn.setPreferredSize(new Dimension(0, 40));
        refreshBtn.addActionListener(e -> refreshStats());
        add(refreshBtn, BorderLayout.SOUTH);

        refreshStats();
    }

    private JLabel createStatCard(JPanel parent, String title, String value, Color bgColor) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
        
        // Header with color
        JLabel titleLabel = new JLabel(title, SwingConstants.CENTER);
        titleLabel.setOpaque(true);
        titleLabel.setBackground(bgColor);
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        titleLabel.setPreferredSize(new Dimension(0, 30));
        
        // Value section
        JLabel valLabel = new JLabel(value, SwingConstants.CENTER);
        valLabel.setFont(new Font("SansSerif", Font.BOLD, 28));
        valLabel.setPreferredSize(new Dimension(0, 80));
        
        card.add(titleLabel, BorderLayout.NORTH);
        card.add(valLabel, BorderLayout.CENTER);
        parent.add(card);
        return valLabel;
    }

    public void refreshStats() {
        // Fetch values from DB
        double revenue = store.getTotalRevenue(); 
        int occupied = store.getOccupiedSpotCount(); 
        double unpaidFines = store.getTotalUnpaidFines(); 

        // Update UI
        lblRevenue.setText(String.format("RM %.2f", revenue));
        lblOccupancy.setText(String.valueOf(occupied));
        lblUnpaidFines.setText(String.format("RM %.2f", unpaidFines));
    }
}