package com.group5.parkinglot.gui;

import com.group5.parkinglot.service.ParkingService;
import java.awt.*;
import java.awt.event.ActionEvent;
import javax.swing.*;

public class ExitPanel extends JPanel {

    private final ParkingService parkingService;
    private JTextField licenseField;
    private JButton exitButton;
    private JTextArea resultArea;

    // Callback after exit
    private Runnable afterExitCallback;

    public ExitPanel(ParkingService parkingService) {
        this.parkingService = parkingService;
        setLayout(new BorderLayout(15, 15));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        add(createInputPanel(), BorderLayout.NORTH);
        add(createResultPanel(), BorderLayout.CENTER);
    }

    // Callback setter
    public void setAfterExitCallback(Runnable callback) {
        this.afterExitCallback = callback;
    }

    private JPanel createInputPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.setBorder(BorderFactory.createTitledBorder("Vehicle Exit"));

        panel.add(new JLabel("License Plate:"));
        licenseField = new JTextField(15);
        panel.add(licenseField);

        exitButton = new JButton("Process Exit");
        exitButton.addActionListener(this::handleExit);
        panel.add(exitButton);

        return panel;
    }

    private JScrollPane createResultPanel() {
        resultArea = new JTextArea(6, 40);
        resultArea.setEditable(false);
        resultArea.setFont(new Font("Monospaced", Font.PLAIN, 13));
        return new JScrollPane(resultArea);
    }

    private void handleExit(ActionEvent e) {
        String license = licenseField.getText().trim();
        if (license.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Enter license plate!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        double total = parkingService.exitVehicle(license, "Cash");

        if (total > 0) {
            resultArea.setText(
                    "Vehicle " + license + " exited.\n" +
                    String.format("Total payment: RM %.2f\n", total)
            );

            // Trigger callback for MainFrame
            if (afterExitCallback != null) afterExitCallback.run();
        } else {
            resultArea.setText("No active ticket found for " + license);
        }

        licenseField.setText("");
    }
}
