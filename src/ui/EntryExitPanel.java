package ui;

import javax.swing.*;
import java.awt.*;
import service.ExitService;
import service.PaymentProcessor;
import enums.PaymentMethod;
import data.DataStore;

public class EntryExitPanel extends JPanel {
    private JTextField plateField;
    private JTextArea displayArea;
    private JButton btnCheckOut, btnPay;
    
    // Logic References
    private ExitService exitService;
    private PaymentProcessor paymentProcessor;
    
    // Temporary state to hold current transaction info
    private String currentPlate;
    private double currentTotal;

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
        displayArea = new JTextArea(15, 30);
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
            if (plate.isEmpty()) return;

            // 1. Fetch data from ExitService
            // (In a real app, you'd return a DTO here. For now, let's assume success)
            displayArea.setText("Calculating for " + plate + "...\n");
            
            // This triggers the internal logic we wrote earlier
            // In a full implementation, ExitService should return the calculation results
            exitService.processExit(plate, java.time.ZonedDateTime.now().toString());
            
            displayArea.append("\nSummary loaded above in console.\nReady for payment.");
            btnPay.setEnabled(true);
            currentPlate = plate;
        });

        btnPay.addActionListener(e -> {
            // 2. Select Payment Method via Popup
            PaymentMethod[] methods = PaymentMethod.values();
            PaymentMethod selected = (PaymentMethod) JOptionPane.showInputDialog(
                    this, "Select Method", "Payment", 
                    JOptionPane.QUESTION_MESSAGE, null, methods, methods[0]);

            if (selected != null) {
                // 3. Complete Transaction
                // Note: You'll need to pass the actual fee/fine variables here
                // paymentProcessor.processPayment(...);
                
                JOptionPane.showMessageDialog(this, "Payment Successful! Receipt Printed.");
                resetUI();
            }
        });
    }

    private void resetUI() {
        plateField.setText("");
        displayArea.setText("");
        btnPay.setEnabled(false);
    }
}