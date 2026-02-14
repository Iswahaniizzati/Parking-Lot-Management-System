package ui;

import data.DataStore;
import java.awt.*;
import javax.swing.*;
import service.ExitService;

public class AdminPanel extends JPanel {

    private ExitService exitService;
    private DataStore store;

    private JLabel lblOccupancy, lblRevenue, lblUnpaidFines;
    private DefaultListModel<String> vehiclesListModel;
    private JComboBox<String> schemeDropdown;

    public AdminPanel(ExitService exitService, DataStore store) {
        this.exitService = exitService;
        this.store = store;

        setLayout(new BorderLayout(15, 15));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        initTopStats();
        initCenterPanel();
        initBottomPanel();

        refreshStats();
    }

    // ---------------- TOP STATS ----------------
    private void initTopStats() {
        JPanel statsPanel = new JPanel(new GridLayout(1, 3, 20, 0));

        lblOccupancy = createStatCard(statsPanel, "Occupancy Rate", "0%", new Color(52, 152, 219));
        lblRevenue = createStatCard(statsPanel, "Total Revenue", "RM 0.00", new Color(46, 204, 113));
        lblUnpaidFines = createStatCard(statsPanel, "Unpaid Fines", "RM 0.00", new Color(231, 76, 60));

        add(statsPanel, BorderLayout.NORTH);
    }

    private JLabel createStatCard(JPanel parent, String title, String value, Color bgColor) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));

        JLabel titleLabel = new JLabel(title, SwingConstants.CENTER);
        titleLabel.setOpaque(true);
        titleLabel.setBackground(bgColor);
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        titleLabel.setPreferredSize(new Dimension(0, 30));

        JLabel valLabel = new JLabel(value, SwingConstants.CENTER);
        valLabel.setFont(new Font("SansSerif", Font.BOLD, 28));
        valLabel.setPreferredSize(new Dimension(0, 80));

        card.add(titleLabel, BorderLayout.NORTH);
        card.add(valLabel, BorderLayout.CENTER);
        parent.add(card);

        return valLabel;
    }

    // ---------------- CENTER PANEL ----------------
    private void initCenterPanel() {
        JPanel centerPanel = new JPanel(new GridLayout(1, 2, 20, 0));

        // --- Vehicle List Panel ---
        vehiclesListModel = new DefaultListModel<>();
        JList<String> vehiclesList = new JList<>(vehiclesListModel);
        JScrollPane vehicleScroll = new JScrollPane(vehiclesList);
        vehicleScroll.setBorder(BorderFactory.createTitledBorder("Vehicles Currently Parked"));
        centerPanel.add(vehicleScroll);

        // --- Fine Scheme Panel ---
        JPanel finePanel = new JPanel(new GridBagLayout());
        finePanel.setBorder(BorderFactory.createTitledBorder("Fine Scheme Configuration"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.gridx = 0; gbc.gridy = 0;

        finePanel.add(new JLabel("Active Scheme:"), gbc);

        String[] options = {"Fixed Fine (RM 50)", "Progressive (Tiered)", "Hourly (RM 20/hr)"};
        schemeDropdown = new JComboBox<>(options);
        gbc.gridx = 1;
        finePanel.add(schemeDropdown, gbc);

        JButton btnApply = new JButton("Update Fine Policy");
        gbc.gridx = 2;
        finePanel.add(btnApply, gbc);

        btnApply.addActionListener(e -> {
            String scheme = (String) schemeDropdown.getSelectedItem();
            store.setActiveFineScheme(scheme); // save for future entries
            JOptionPane.showMessageDialog(this, "Policy updated for future entries.");
            refreshStats();
        });

        centerPanel.add(finePanel);

        add(centerPanel, BorderLayout.CENTER);
    }

    // ---------------- BOTTOM PANEL ----------------
    private void initBottomPanel() {
        JButton refreshBtn = new JButton("Refresh Dashboard & Spot Data");
        refreshBtn.setPreferredSize(new Dimension(0, 40));
        refreshBtn.addActionListener(e -> refreshStats());
        add(refreshBtn, BorderLayout.SOUTH);
    }

    // ---------------- REFRESH ----------------
    public void refreshStats() {
        // Top stats
        double revenue = store.getTotalRevenue();
        int occupied = store.getOccupiedSpotCount();
        int totalSpots = store.getTotalSpotCount();
        double unpaidFines = store.getTotalUnpaidFines();

        String occupancyStr = String.format("%d/%d (%.1f%%)", occupied, totalSpots, occupied * 100.0 / totalSpots);

        lblRevenue.setText(String.format("RM %.2f", revenue));
        lblOccupancy.setText(occupancyStr);
        lblUnpaidFines.setText(String.format("RM %.2f", unpaidFines));

        // Vehicle list
        vehiclesListModel.clear();
        store.getAllActiveSessions().forEach(session ->
                vehiclesListModel.addElement(session.getPlate() + " (" + session.getSpotId() + ")")
        );
    }
}
