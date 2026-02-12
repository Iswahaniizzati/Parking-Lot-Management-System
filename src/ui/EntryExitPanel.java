package ui;

import javax.swing.*;
import java.awt.*;
import service.ExitService;
import service.PaymentProcessor;
import enums.PaymentMethod;
import data.DataStore;
import model.PaymentRecord;

public class EntryExitPanel extends JPanel {
    private JTextField plateField;
    private JTextArea displayArea;
    private JButton btnCheckOut, btnPay;
    
    private ExitService exitService;
    private PaymentProcessor paymentProcessor;
    
    // Store the record between calculation and payment
    private PaymentRecord currentRecord;

    public EntryExitPanel(DataStore db, ExitService exitService, PaymentProcessor paymentProcessor) {
        this.exitService = exitService;
        this.paymentProcessor = paymentProcessor;
        
        setLayout(new BorderLayout());
        
        // --- Input Section ---
        JPanel inputPanel = new JPanel();
        inputPanel.add(new JLabel("Vehicle Plate:"));
        plateField = new JTextField(10);
        btnCheckOut = new JButton("Calculate Fee");
        inputPanel.add(plateField);
        inputPanel.add(btnCheckOut);
        
        // --- Display Section ---
        displayArea = new JTextArea(15, 35);
        displayArea.setEditable(false);
        displayArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        
        // --- Action Section ---
        JPanel actionPanel = new JPanel();
        btnPay = new JButton("Process Payment");
        btnPay.setEnabled(false);
        actionPanel.add(btnPay);
        
        add(inputPanel, BorderLayout.NORTH);
        add(new JScrollPane(displayArea), BorderLayout.CENTER);
        add(actionPanel, BorderLayout.SOUTH);

        setupListeners();
    }

    private void setupListeners() {
        btnCheckOut.addActionListener(e -> {
            String plate = plateField.getText().trim();
            if (plate.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please enter a plate number.");
                return;
            }

            // 1. Fetch the breakdown from ExitService
            // Note: Ensure ExitService.processExit returns the PaymentRecord object
            currentRecord = exitService.processExit(plate, java.time.ZonedDateTime.now().toString());
            
            if (currentRecord != null) {
                updateDisplayWithSummary();
                btnPay.setEnabled(true);
            } else {
                displayArea.setText("No active session found for: " + plate);
                btnPay.setEnabled(false);
            }
        });

        btnPay.addActionListener(e -> {
            PaymentMethod[] methods = PaymentMethod.values();
            PaymentMethod selected = (PaymentMethod) JOptionPane.showInputDialog(
                    this, "Select Payment Method", "Payment", 
                    JOptionPane.QUESTION_MESSAGE, null, methods, methods[0]);

            if (selected != null) {
                double amountPaid = currentRecord.getTotalDue();
                
                // Requirement 5: Cash Balance Calculation
                if (selected == PaymentMethod.CASH) {
                    String input = JOptionPane.showInputDialog(this, 
                        "Total Due: RM " + String.format("%.2f", currentRecord.getTotalDue()) + 
                        "\nEnter Cash Amount Received:");
                    try {
                        amountPaid = Double.parseDouble(input);
                        if (amountPaid < currentRecord.getTotalDue()) {
                            JOptionPane.showMessageDialog(this, "Error: Insufficient cash provided.");
                            return;
                        }
                    } catch (NumberFormatException ex) {
                        return;
                    }
                }

                // 2. Execute Payment (Saves to DB, clears fines, frees spot)
                paymentProcessor.processPayment(
                    currentRecord.getTicketNo(),
                    currentRecord.getPlate(),
                    currentRecord.getParkingFee(),
                    currentRecord.getFinePaid(),
                    selected,
                    amountPaid
                );

                // 3. Update UI with the final formal receipt
                updateDisplayWithFinalReceipt(selected, amountPaid);
                
                JOptionPane.showMessageDialog(this, "Payment Successful! Spot is now available.");
                btnPay.setEnabled(false);
            }
        });
    }

    private void updateDisplayWithSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("--- PRE-PAYMENT SUMMARY ---\n");
        sb.append(String.format("Plate:        %s\n", currentRecord.getPlate()));
        sb.append(String.format("Parking Fee:  RM %.2f\n", currentRecord.getParkingFee()));
        sb.append(String.format("Fines Due:    RM %.2f\n", currentRecord.getFinePaid()));
        sb.append("---------------------------\n");
        sb.append(String.format("TOTAL DUE:    RM %.2f\n", currentRecord.getTotalDue()));
        sb.append("---------------------------\n");
        sb.append("Select 'Process Payment' to complete.");
        displayArea.setText(sb.toString());
    }

    private void updateDisplayWithFinalReceipt(PaymentMethod method, double amountPaid) {
        double balance = amountPaid - currentRecord.getTotalDue();
        
        StringBuilder sb = new StringBuilder();
        sb.append("===================================\n");
        sb.append("       OFFICIAL RECEIPT            \n");
        sb.append("===================================\n");
        sb.append(String.format("Plate:           %s\n", currentRecord.getPlate()));
        sb.append(String.format("Ticket No:      %s\n", currentRecord.getTicketNo()));
        sb.append(String.format("Payment Method:  %s\n", method));
        sb.append("-----------------------------------\n");
        sb.append(String.format("Parking Fee:     RM %.2f\n", currentRecord.getParkingFee()));
        sb.append(String.format("Fines Paid:      RM %.2f\n", currentRecord.getFinePaid()));
        sb.append(String.format("Total Due:       RM %.2f\n", currentRecord.getTotalDue()));
        sb.append("-----------------------------------\n");
        sb.append(String.format("Amount Paid:     RM %.2f\n", amountPaid));
        sb.append(String.format("Balance/Change:  RM %.2f\n", balance));
        sb.append("===================================\n");
        sb.append("    Thank you for your payment!    \n");
        displayArea.setText(sb.toString());
    }

    private void resetUI() {
        plateField.setText("");
        displayArea.setText("");
        btnPay.setEnabled(false);
        currentRecord = null;
    }
}