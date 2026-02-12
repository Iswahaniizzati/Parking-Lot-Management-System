package ui;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import model.FineRecord;
import data.DataStore;
import model.ParkingSpot;
import model.ParkingSession;

public class ReportingPanel extends JPanel {
    private DefaultTableModel tableModel;
    private DataStore store;
    private JLabel totalRevenueLabel;

    public ReportingPanel(DataStore store) {
        this.store = store;
        setLayout(new BorderLayout(10, 10));
        
        totalRevenueLabel = new JLabel("Revenue: RM 0.00");
        add(totalRevenueLabel, BorderLayout.NORTH);

        tableModel = new DefaultTableModel(new String[]{"License Plate", "Reason", "Amount"}, 0);
        add(new JScrollPane(new JTable(tableModel)), BorderLayout.CENTER);
        
        refreshStats(); // Initial load
    }

    public void refreshStats() {
        tableModel.setRowCount(0); // Clear current rows
        
        // Fetch list of active sessions from DataStore
        List<ParkingSession> activeSessions = store.getAllActiveSessions(); 
        
        for (ParkingSession s : activeSessions) {
            tableModel.addRow(new Object[]{
                s.getTicketNo(),
                s.getPlate(),
                s.getSpotId(),
                //s.getVehicleType(),
                s.getEntryTime()
            });
        }
    }

    private void loadVehicleReport() {
    // Check if tableModel is initialized before using it
    if (tableModel != null) {
        tableModel.setRowCount(0); // Clear old data
        // For testing, add dummy rows. Later, replace with store.getOccupiedSpots()
        tableModel.addRow(new Object[]{"XYZ 5678", "CAR", "Parked (F2-R3)"});
        tableModel.addRow(new Object[]{"DEF 9999", "SUV", "Parked (F3-R2)"});
    }
}
}