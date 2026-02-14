package ui;

import data.DataStore;
import fine.FineScheme;
import fine.FixedFineScheme;
import fine.HourlyFineScheme;
import fine.ProgressiveFineScheme;
import java.awt.*;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import model.*;
import service.ExitService;

public class AdminPanel extends JPanel {
    private ExitService exitService;
    private DataStore store;
    private JLabel lblOccupancy, lblRevenue, lblUnpaidFines;
    private JTable finesTable;
    private DefaultTableModel finesTableModel;

    public AdminPanel(ExitService exitService, DataStore store) {
        this.exitService = exitService;
        this.store = store;
        setLayout(new BorderLayout(15, 15));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // --- TOP SECTION: 3 Statistics Cards ---
        JPanel statsPanel = new JPanel(new GridLayout(1, 3, 20, 0));

        lblOccupancy = createStatCard(statsPanel, "Occupancy Rate", "0", new Color(52, 152, 219));
        lblRevenue = createStatCard(statsPanel, "Total Revenue", "RM 0.00", new Color(46, 204, 113));
        lblUnpaidFines = createStatCard(statsPanel, "Unpaid Fines", "RM 0.00", new Color(231, 76, 60));

        add(statsPanel, BorderLayout.NORTH);

        // --- CENTER SECTION: Fine Management ---
        JPanel centerPanel = new JPanel(new BorderLayout(10, 10));

        // 1️⃣ Fine Scheme Configuration
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

        centerPanel.add(configPanel, BorderLayout.NORTH);

        // 2️⃣ Table of Unpaid Fines
        String[] columns = {"Plate", "Reason/Type", "Amount (RM)", "Issued At"};
        finesTableModel = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        finesTable = new JTable(finesTableModel);
        JScrollPane tableScroll = new JScrollPane(finesTable);
        tableScroll.setBorder(BorderFactory.createTitledBorder("Unpaid Fines"));
        centerPanel.add(tableScroll, BorderLayout.CENTER);

        add(centerPanel, BorderLayout.CENTER);

        // --- BOTTOM SECTION: Refresh Button ---
        JButton refreshBtn = new JButton("Refresh Dashboard & Fine Data");
        refreshBtn.setPreferredSize(new Dimension(0, 40));
        refreshBtn.addActionListener(e -> refreshStats());
        add(refreshBtn, BorderLayout.SOUTH);

        // --- Action Listeners ---
        btnApply.addActionListener(e -> {
            String selected = (String) schemeDropdown.getSelectedItem();
            FineScheme scheme = switch (selected) {
                case "Fixed Fine (RM 50)" -> new FixedFineScheme();
                case "Progressive (Tiered)" -> new ProgressiveFineScheme();
                case "Hourly (RM 20/hr)" -> new HourlyFineScheme();
                default -> exitService.getActiveFineScheme();
            };
            exitService.setActiveFineScheme(scheme);
            JOptionPane.showMessageDialog(this, "Fine Policy Updated for future entries.");
            refreshStats();
        });

        refreshStats();
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

    public void refreshStats() {
        double revenue = store.getTotalRevenue();
        int occupied = store.getOccupiedSpotCount();
        double unpaidFines = store.getTotalUnpaidFines();

        lblRevenue.setText(String.format("RM %.2f", revenue));
        lblOccupancy.setText(String.valueOf(occupied));
        lblUnpaidFines.setText(String.format("RM %.2f", unpaidFines));

        // Refresh table
        finesTableModel.setRowCount(0); // Clear existing rows
        List<FineRecord> fines = store.getAllUnpaidFines();
        for (FineRecord fine : fines) {
            finesTableModel.addRow(new Object[]{
                    fine.getPlate(),
                    fine.getType(),
                    fine.getAmount(),
                    fine.getIssuedTime()
            });
        }
    }
}
