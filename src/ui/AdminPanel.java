package ui;

import javax.swing.*;
import java.awt.*;
import service.ExitService;
import fine.*;

public class AdminPanel extends JPanel {
    private ExitService exitService;
    private JComboBox<String> schemeDropdown;
    private JLabel statusLabel;

    public AdminPanel(ExitService exitService) {
        this.exitService = exitService;
        setLayout(new java.awt.FlowLayout());
        setBorder(BorderFactory.createTitledBorder("Fine Management (Admin)"));

        add(new JLabel("Select Active Fine Scheme:"));

        // Options matching your requirements
        String[] options = {"Fixed Fine (RM 50)", "Progressive (Tiered)", "Hourly (RM 20/hr)"};
        schemeDropdown = new JComboBox<>(options);
        
        JButton btnApply = new JButton("Apply Scheme");
        statusLabel = new JLabel(" Current: " + exitService.getActiveFineScheme().getSchemeName());

        btnApply.addActionListener(e -> updateScheme());

        add(schemeDropdown);
        add(btnApply);
        add(statusLabel);
    }

    private void updateScheme() {
        int index = schemeDropdown.getSelectedIndex();
        FineScheme newScheme;

        switch (index) {
            case 0 -> newScheme = new FixedFineScheme();
            case 1 -> newScheme = new ProgressiveFineScheme();
            case 2 -> newScheme = new HourlyFineScheme();
            default -> newScheme = new FixedFineScheme();
        }

        exitService.setActiveFineScheme(newScheme);
        statusLabel.setText(" Current: " + newScheme.getSchemeName());
        
        JOptionPane.showMessageDialog(this, "Fine scheme updated successfully!");
    }
}