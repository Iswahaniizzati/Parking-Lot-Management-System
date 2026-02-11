package com.group5.parkinglot.gui;

import com.group5.parkinglot.model.ActiveTicket;
import com.group5.parkinglot.model.Floor;
import com.group5.parkinglot.service.ParkingService;
import java.awt.*;
import java.util.List;
import java.util.Map;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

/**
 * Admin & Report Panel
 * Shows:
 * - Occupancy per floor
 * - Active tickets (parked vehicles)
 * - Total revenue
 */
public class AdminPanel extends JPanel {

    private final ParkingService parkingService;

    private JTable floorTable;
    private DefaultTableModel floorTableModel;

    private JTable activeTable;
    private DefaultTableModel activeTableModel;

    private JLabel revenueLabel;

    public AdminPanel(ParkingService parkingService) {
        this.parkingService = parkingService;
        setLayout(new BorderLayout(15, 15));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        add(createFloorPanel(), BorderLayout.NORTH);
        add(createActiveTicketsPanel(), BorderLayout.CENTER);
        add(createRevenuePanel(), BorderLayout.SOUTH);

        refreshData();
    }

    // ===============================
    // Floor Occupancy Table
    // ===============================
    private JPanel createFloorPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Floor Occupancy"));

        String[] columns = {"Floor", "Total Spots", "Occupied Spots", "Occupancy (%)"};
        floorTableModel = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        floorTable = new JTable(floorTableModel);
        panel.add(new JScrollPane(floorTable), BorderLayout.CENTER);

        return panel;
    }

    // ===============================
    // Active Tickets Table
    // ===============================
    private JPanel createActiveTicketsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Active Tickets (Parked Vehicles)"));

        String[] columns = {"Ticket ID", "License Plate", "Vehicle Type", "Spot ID", "Entry Time"};
        activeTableModel = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        activeTable = new JTable(activeTableModel);
        panel.add(new JScrollPane(activeTable), BorderLayout.CENTER);

        return panel;
    }

    // ===============================
    // Revenue Label
    // ===============================
    private JPanel createRevenuePanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        revenueLabel = new JLabel("Total Revenue: RM 0.00");
        revenueLabel.setFont(new Font("Arial", Font.BOLD, 16));
        panel.add(revenueLabel);
        return panel;
    }

    // ===============================
    // Refresh All Data
    // ===============================
    public void refreshData() {
        refreshFloorData();
        refreshActiveTickets();
        refreshRevenue();
    }

    private void refreshFloorData() {
        floorTableModel.setRowCount(0);
        List<Floor> floors = parkingService.getFloors();
        for (Floor f : floors) {
            int total = f.getTotalSpots();
            int occupied = f.getOccupiedSpots();
            double percent = total == 0 ? 0 : (occupied * 100.0) / total;
            floorTableModel.addRow(new Object[]{f.getFloorNumber(), total, occupied, String.format("%.2f", percent)});
        }
    }

    private void refreshActiveTickets() {
        activeTableModel.setRowCount(0);
        Map<String, ActiveTicket> tickets = parkingService.getActiveTickets();
        for (ActiveTicket t : tickets.values()) {
            activeTableModel.addRow(new Object[]{
                    t.getTicketId(),
                    t.getLicensePlate(),
                    t.getVehicleType(),
                    t.getSpotId(),
                    t.getEntryTime()
            });
        }
    }

    private void refreshRevenue() {
        revenueLabel.setText(String.format("Total Revenue: RM %.2f", parkingService.getTotalRevenue()));
    }
}
