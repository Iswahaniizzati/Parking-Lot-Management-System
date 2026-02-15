package ui;

import data.DataStore;
import java.awt.*;
import javax.swing.*;
import service.ExitService;

public class AdminPanel extends JPanel {

    private final ExitService exitService;
    private final DataStore store;

    // UI Components that need updating
    private JLabel lblOccupancy, lblRevenue, lblUnpaidFines;
    private DefaultListModel<String> vehiclesListModel;
    private JComboBox<String> schemeDropdown;

    public AdminPanel(ExitService exitService, DataStore store) {
        this.exitService = exitService;
        this.store = store;

        // Modern layout styling
        setLayout(new BorderLayout(15, 15));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        initComponents();
        refreshStats(); // Initial data load
    }

    private void initComponents() {
        initTopStats();
        initCenterPanel();
        initBottomPanel();
    }

    // ---------------- TOP STATS CARDS ----------------
    private void initTopStats() {
        JPanel statsPanel = new JPanel(new GridLayout(1, 3, 20, 0));

        // Create cards with specific colors for visual distinction
        lblOccupancy = createStatCard(statsPanel, "Occupancy Rate", "0/0 (0%)", new Color(52, 152, 219));
        lblRevenue = createStatCard(statsPanel, "Total Revenue", "RM 0.00", new Color(46, 204, 113));
        lblUnpaidFines = createStatCard(statsPanel, "Unpaid Fines", "RM 0.00", new Color(231, 76, 60));

        add(statsPanel, BorderLayout.NORTH);
    }

    private JLabel createStatCard(JPanel parent, String title, String value, Color bgColor) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
        card.setBackground(Color.WHITE);

        JLabel titleLabel = new JLabel(title, SwingConstants.CENTER);
        titleLabel.setOpaque(true);
        titleLabel.setBackground(bgColor);
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        titleLabel.setPreferredSize(new Dimension(0, 35));

        JLabel valLabel = new JLabel(value, SwingConstants.CENTER);
        valLabel.setFont(new Font("SansSerif", Font.BOLD, 22));
        valLabel.setPreferredSize(new Dimension(0, 80));

        card.add(titleLabel, BorderLayout.NORTH);
        card.add(valLabel, BorderLayout.CENTER);
        parent.add(card);

        return valLabel;
    }

    // ---------------- CENTER DASHBOARD ----------------
    private void initCenterPanel() {
        JPanel centerPanel = new JPanel(new GridLayout(1, 2, 20, 0));

        // --- Vehicle List Side ---
        vehiclesListModel = new DefaultListModel<>();
        JList<String> vehiclesList = new JList<>(vehiclesListModel);
        vehiclesList.setFont(new Font("Monospaced", Font.PLAIN, 12));
        
        JScrollPane vehicleScroll = new JScrollPane(vehiclesList);
        vehicleScroll.setBorder(BorderFactory.createTitledBorder("Vehicles Currently Parked"));
        centerPanel.add(vehicleScroll);

        // --- Fine Configuration Side ---
        JPanel finePanel = new JPanel(new GridBagLayout());
        finePanel.setBorder(BorderFactory.createTitledBorder("System Configuration"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        finePanel.add(new JLabel("Active Fine Policy:"), gbc);

        String[] options = {"Fixed Fine (RM 50)", "Progressive (Tiered)", "Hourly (RM 20/hr)"};
        schemeDropdown = new JComboBox<>(options);
        gbc.gridx = 1;
        finePanel.add(schemeDropdown, gbc);

        JButton btnApply = new JButton("Update Policy");
        btnApply.setBackground(new Color(44, 62, 80));
        btnApply.setForeground(Color.WHITE);
        gbc.gridy = 1; gbc.gridx = 0; gbc.gridwidth = 2;
        finePanel.add(btnApply, gbc);

        btnApply.addActionListener(e -> {
            String scheme = (String) schemeDropdown.getSelectedItem();
            store.setActiveFineScheme(scheme);
            JOptionPane.showMessageDialog(this, "Fine policy updated for future records.");
            refreshStats();
        });

        centerPanel.add(finePanel);
        add(centerPanel, BorderLayout.CENTER);
    }

    // ---------------- BOTTOM ACTIONS ----------------
    private void initBottomPanel() {
        JButton refreshBtn = new JButton("Refresh Dashboard");
        refreshBtn.setPreferredSize(new Dimension(0, 45));
        refreshBtn.setFont(new Font("SansSerif", Font.BOLD, 12));
        refreshBtn.addActionListener(e -> refreshStats());
        add(refreshBtn, BorderLayout.SOUTH);
    }

    // ---------------- REFRESH LOGIC ----------------
    public void refreshStats() {
        // Fetch latest data from DataStore (SQLite)
        double revenue = store.getTotalRevenue();
        int occupied = store.getOccupiedSpotCount();
        int totalSpots = store.getTotalSpotCount();
        double unpaidFines = store.getTotalUnpaidFines();

        // Safe percentage calculation
        double percent = (totalSpots > 0) ? (occupied * 100.0 / totalSpots) : 0;
        String occupancyStr = String.format("%d/%d (%.1f%%)", occupied, totalSpots, percent);

        // Update Labels
        lblRevenue.setText(String.format("RM %.2f", revenue));
        lblOccupancy.setText(occupancyStr);
        lblUnpaidFines.setText(String.format("RM %.2f", unpaidFines));

        // Update Parked Vehicles List
        vehiclesListModel.clear();
        store.getAllActiveSessions().forEach(session ->
                vehiclesListModel.addElement(String.format("%-10s | Spot: %s", 
                    session.getPlate(), session.getSpotId()))
        );
        
        // Ensure the panel redraws
        revalidate();
        repaint();
    }
}