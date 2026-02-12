package data;

import java.sql.*;
import java.util.List;
import java.util.ArrayList;
import model.FineRecord;
import model.ParkingSpot;
import model.ParkingSession;
import model.PaymentRecord;
import enums.SpotType;
import enums.SpotStatus;

public class SQLiteDataStore implements DataStore {

    // 1. Database Configuration
    private static final String DB_URL = "jdbc:sqlite:parking.db";
    private Connection conn;

    @Override
    public void connect() {
        try {
            this.conn = DriverManager.getConnection(DB_URL);
            if (this.conn != null) {
                System.out.println("Connected to parking.db");
            }
        } catch (SQLException e) {
            System.err.println("Connection failed: " + e.getMessage());
        }
    }

    @Override
    public void initSchema() {
        String[] tables = {
            """
            CREATE TABLE IF NOT EXISTS parking_spot (
                spot_id TEXT PRIMARY KEY,
                spot_type TEXT NOT NULL,
                status TEXT NOT NULL,
                hourly_rate REAL NOT NULL,
                current_plate TEXT
            );
            """,
            """
            CREATE TABLE IF NOT EXISTS parking_session (
                session_id INTEGER PRIMARY KEY AUTOINCREMENT,
                ticket_no TEXT UNIQUE NOT NULL,
                plate TEXT NOT NULL,
                spot_id TEXT NOT NULL,
                entry_time TEXT NOT NULL,
                exit_time TEXT,
                duration_hours INTEGER,
                parking_fee REAL,
                FOREIGN KEY (spot_id) REFERENCES parking_spot(spot_id)
            );
            """,
            """
            CREATE TABLE IF NOT EXISTS fine (
                fine_id INTEGER PRIMARY KEY AUTOINCREMENT,
                plate TEXT NOT NULL,
                reason TEXT NOT NULL,
                amount REAL NOT NULL,
                issued_at TEXT NOT NULL,
                paid INTEGER NOT NULL DEFAULT 0,
                paid_at TEXT
            );
            """,
            """
            CREATE TABLE IF NOT EXISTS payment (
                payment_id INTEGER PRIMARY KEY AUTOINCREMENT,
                ticket_no TEXT NOT NULL,
                plate TEXT NOT NULL,
                method TEXT NOT NULL,
                paid_time TEXT NOT NULL,
                parking_fee REAL NOT NULL,
                fine_paid REAL NOT NULL,
                total_due REAL NOT NULL,
                amount_paid REAL NOT NULL,
                balance REAL NOT NULL
            );
            """,
            """
            CREATE TABLE IF NOT EXISTS users (
                username TEXT PRIMARY KEY,
                password TEXT NOT NULL,
                role TEXT NOT NULL
            );
            """
        };

        try (Statement stmt = conn.createStatement()) {
            for (String sql : tables) {
                stmt.execute(sql);
            }
            System.out.println("Database tables ready.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // --- Spot Management ---

    @Override
    public void upsertSpot(ParkingSpot spot) {
        String sql = "INSERT OR REPLACE INTO parking_spot (spot_id, spot_type, status, hourly_rate, current_plate) VALUES (?, ?, ?, ?, ?);";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, spot.getSpotId());
            stmt.setString(2, spot.getType().name());
            stmt.setString(3, spot.getStatus().name());
            stmt.setDouble(4, spot.getHourlyRate());
            stmt.setString(5, spot.getCurrentVehiclePlate());
            stmt.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    @Override
    public List<ParkingSpot> getAllSpots() {
        List<ParkingSpot> spots = new ArrayList<>();
        String sql = "SELECT * FROM parking_spot";
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                SpotType type = SpotType.valueOf(rs.getString("spot_type").toUpperCase());
                ParkingSpot spot = new ParkingSpot(rs.getString("spot_id"), type);
                spot.setStatus(SpotStatus.valueOf(rs.getString("status").toUpperCase()));
                spot.setCurrentVehiclePlate(rs.getString("current_plate"));
                spots.add(spot);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return spots;
    }

    @Override
    public List<ParkingSpot> findAvailableSpots(String type) {
        List<ParkingSpot> result = new ArrayList<>();
        String sql = "SELECT * FROM parking_spot WHERE spot_type = ? AND status = 'AVAILABLE'";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, type);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    SpotType spotType = SpotType.valueOf(rs.getString("spot_type").toUpperCase());
                    result.add(new ParkingSpot(rs.getString("spot_id"), spotType));
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return result;
    }

    @Override
    public void setSpotOccupied(String spotId, String plate) {
        String sql = "UPDATE parking_spot SET status = 'OCCUPIED', current_plate = ? WHERE spot_id = ?;";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, plate);
            stmt.setString(2, spotId);
            stmt.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    @Override
    public void setSpotAvailable(String spotId) {
        String sql = "UPDATE parking_spot SET status = 'AVAILABLE', current_plate = NULL WHERE spot_id = ?;";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, spotId);
            stmt.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    // --- Session & Payment ---

    @Override
    public void createSession(ParkingSession session) {
        String sql = "INSERT INTO parking_session (ticket_no, plate, spot_id, entry_time) VALUES (?, ?, ?, ?);";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, session.getTicketNo());
            stmt.setString(2, session.getPlate());
            stmt.setString(3, session.getSpotId());
            stmt.setString(4, session.getEntryTime());
            stmt.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    @Override
    public List<ParkingSession> getAllActiveSessions() {
        List<ParkingSession> sessions = new ArrayList<>();
        String sql = "SELECT * FROM parking_session WHERE exit_time IS NULL";
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                sessions.add(new ParkingSession(
                    rs.getString("ticket_no"),
                    rs.getString("plate"),
                    rs.getString("spot_id"),
                    rs.getString("entry_time")
                ));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return sessions;
    }

    @Override
    public void closeSession(String ticketNo, String exitTime, int hours, double fee) {
        String sql = "UPDATE parking_session SET exit_time = ?, duration_hours = ?, parking_fee = ? WHERE ticket_no = ?;";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, exitTime);
            stmt.setInt(2, hours);
            stmt.setDouble(3, fee);
            stmt.setString(4, ticketNo);
            stmt.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    // --- Admin Stats ---

    @Override
    public double getTotalRevenue() {
        String sql = "SELECT SUM(amount_paid) FROM payment";
        return executeScalarDouble(sql);
    }

    @Override
    public double getTotalUnpaidFines() {
        String sql = "SELECT SUM(amount) FROM fine WHERE paid = 0";
        return executeScalarDouble(sql);
    }

    @Override
    public int getOccupiedSpotCount() {
        String sql = "SELECT COUNT(*) FROM parking_spot WHERE status = 'OCCUPIED'";
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }

    @Override
    public int getTotalSpotCount() {
        String sql = "SELECT COUNT(*) FROM parking_spot";
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }

    private double executeScalarDouble(String sql) {
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getDouble(1);
        } catch (SQLException e) { e.printStackTrace(); }
        return 0.0;
    }

    // --- Auth & Generic ---

    @Override
    public String authenticate(String username, String password) {
        String sql = "SELECT role FROM users WHERE username = ? AND password = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return rs.getString("role");
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    @Override
    public void close() {
        try { if (conn != null) conn.close(); } catch (SQLException e) { e.printStackTrace(); }
    }

    // Boilerplate for interface compatibility
    @Override public List<model.ParkingSpot> getAvailableSpots(String type) { return findAvailableSpots(type); }
    @Override public model.ParkingSession getOpenSessionByPlate(String p) { return null; /* Implement logic similar to closeSession */ }
    @Override public void addFine(FineRecord f) {}
    @Override public List<FineRecord> getUnpaidFinesByPlate(String p) { return new ArrayList<>(); }
    @Override public List<FineRecord> getAllUnpaidFines() { return new ArrayList<>(); }
    @Override public void markAllFinesPaid(String p, String t) {}
    @Override public void createPayment(PaymentRecord p) {}
}