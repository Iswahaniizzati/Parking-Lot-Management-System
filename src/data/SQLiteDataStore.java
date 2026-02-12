package data;

import java.sql.*;

//handles all database operation
//implements DataStore interface

public class SQLiteDataStore implements DataStore {

    private Connection conn; //main DB connection


    @Override
    public void connect() {
        try {
            conn = DriverManager.getConnection("jdbc:sqlite:parking.db");
            System.out.println("Connected to SQLite database.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
     //create required table if they don't exist
    @Override
    public void initSchema() {
        String spotsql = """
        CREATE TABLE IF NOT EXISTS parking_spot (
            spot_id TEXT PRIMARY KEY,
            spot_type TEXT NOT NULL,
            status TEXT NOT NULL,
            hourly_rate REAL NOT NULL,
            current_plate TEXT
        );
    """;

        String sessionSql = """
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
    """;

        String fineSql = """
        CREATE TABLE IF NOT EXISTS fine (
         fine_id INTEGER PRIMARY KEY AUTOINCREMENT,
         plate TEXT NOT NULL,
         reason TEXT NOT NULL,
         amount REAL NOT NULL,
         issued_at TEXT NOT NULL,
         paid INTEGER NOT NULL DEFAULT 0,
         paid_at TEXT
     );
    """;

        String paymentSql = """
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
         REAL NOT NULL
     );
    """;

    try (Statement stmt = conn.createStatement()) {
        stmt.execute(spotsql);        // parking_spot
        stmt.execute(sessionSql); // parking_session
        stmt.execute(fineSql);
        stmt.execute(paymentSql);
        System.out.println("parking_spot table ready.");
        System.out.println("parking_session table ready.");
        System.out.println("fine table ready.");
        System.out.println("payment table ready.");
    } catch (SQLException e) {
        e.printStackTrace();
    }
}

    //close database connection safely
    @Override
    public void close() {
        try {
            if (conn != null) {
                conn.close();
                System.out.println("Database connection closed.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override //insert or update parking spot into DB
    public void upsertSpot(model.ParkingSpot spot) {
        String sql = """
            INSERT OR REPLACE INTO parking_spot
            (spot_id, spot_type, status, hourly_rate, current_plate)
            VALUES (?, ?, ?, ?, ?);
        """;

        try (java.sql.PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, spot.getSpotId());
            stmt.setString(2, spot.getType().toString());
            stmt.setString(3, spot.getStatus().toString());
            stmt.setDouble(4, spot.getHourlyRate());
            stmt.setString(5, spot.getCurrentVehiclePlate());

            stmt.executeUpdate();
            System.out.println("Spot inserted/updated: " + spot.getSpotId());

    } catch (java.sql.SQLException e) {
        e.printStackTrace();
    }
}


@Override //find all spots where spot_type matches and status AVAILABLE
public java.util.List<model.ParkingSpot> findAvailableSpots(String spotType) {

    java.util.List<model.ParkingSpot> result = new java.util.ArrayList<>();

    String sql = """
        SELECT spot_id, spot_type
        FROM parking_spot
        WHERE spot_type = ? AND status = 'AVAILABLE';
    """;

    try (java.sql.PreparedStatement stmt = conn.prepareStatement(sql)) {
        stmt.setString(1, spotType);

        try (java.sql.ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                String spotId = rs.getString("spot_id");
                String typeStr = rs.getString("spot_type");

                //ParkingSpot
                enums.SpotType type = enums.SpotType.valueOf(typeStr);

                model.ParkingSpot spot = new model.ParkingSpot(spotId, type);
                result.add(spot);
            }
        }

    } catch (java.sql.SQLException e) {
        e.printStackTrace();
    }

    return result;
}


@Override //update spot to OCCUPIED and store current plate
public void setSpotOccupied(String spotId, String plate) {

    String sql = """
        UPDATE parking_spot
        SET status = 'OCCUPIED',
            current_plate = ?
        WHERE spot_id = ?;
    """;

    try (java.sql.PreparedStatement stmt = conn.prepareStatement(sql)) {

        stmt.setString(1, plate);
        stmt.setString(2, spotId);

        int rows = stmt.executeUpdate();
        System.out.println("setSpotOccupied updated rows: " + rows);

    } catch (java.sql.SQLException e) {
        e.printStackTrace();
    }
}

@Override //update SPOT to AVAILABLE and clear plate
public void setSpotAvailable(String spotId) {

    String sql = """
        UPDATE parking_spot
        SET status = 'AVAILABLE',
            current_plate = NULL
        WHERE spot_id = ?;
    """;

    try (java.sql.PreparedStatement stmt = conn.prepareStatement(sql)) {

        stmt.setString(1, spotId);

        int rows = stmt.executeUpdate();
        System.out.println("setSpotAvailable updated rows: " + rows);

    } catch (java.sql.SQLException e) {
        e.printStackTrace();
    }
}

@Override //insert a new parking session record
public void createSession(model.ParkingSession session) {

    String sql = """
        INSERT INTO parking_session (ticket_no, plate, spot_id, entry_time)
        VALUES (?, ?, ?, ?);
    """;

    try (PreparedStatement stmt = conn.prepareStatement(sql)) {

        stmt.setString(1, session.getTicketNo());
        stmt.setString(2, session.getPlate());
        stmt.setString(3, session.getSpotId());
        stmt.setString(4, session.getEntryTime()); // ISO string

        stmt.executeUpdate();
        System.out.println("Session created: " + session.getTicketNo());

    } catch (SQLException e) {
        e.printStackTrace();
    }
}

@Override //retrive latest open session for a plate
public model.ParkingSession getOpenSessionByPlate(String plate) {

    String sql = """
        SELECT ticket_no, plate, spot_id, entry_time
        FROM parking_session
        WHERE plate = ? AND exit_time IS NULL
        ORDER BY session_id DESC
        LIMIT 1;
    """;

    try (PreparedStatement stmt = conn.prepareStatement(sql)) {
        stmt.setString(1, plate);

        try (ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return new model.ParkingSession(
                        rs.getString("ticket_no"),
                        rs.getString("plate"),
                        rs.getString("spot_id"),
                        rs.getString("entry_time")
                );
            }
        }

    } catch (SQLException e) {
        e.printStackTrace();
    }

    return null; // no open session found
}

@Override //update session with exit details
public void closeSession(String ticketNo, String exitTimeISO, int durationHours, double parkingFee) {

    String sql = """
        UPDATE parking_session
        SET exit_time = ?,
            duration_hours = ?,
            parking_fee = ?
        WHERE ticket_no = ?;
    """;

    try (PreparedStatement stmt = conn.prepareStatement(sql)) {

        stmt.setString(1, exitTimeISO);
        stmt.setInt(2, durationHours);
        stmt.setDouble(3, parkingFee);
        stmt.setString(4, ticketNo);

        int rows = stmt.executeUpdate();
        System.out.println("closeSession updated rows: " + rows);

    } catch (SQLException e) {
        e.printStackTrace();
    }
}

@Override //insert a fine record
public void addFine(model.FineRecord fine) {

    String sql = """
        INSERT INTO fine (plate, reason, amount, issued_at, paid, paid_at)
        VALUES (?, ?, ?, ?, ?, NULL);
    """;

    try (PreparedStatement stmt = conn.prepareStatement(sql)) {

        stmt.setString(1, fine.getPlate());
        stmt.setString(2, fine.getReason());
        stmt.setDouble(3, fine.getAmount());
        stmt.setString(4, fine.getIssuedAt());
        stmt.setInt(5, fine.isPaid() ? 1 : 0);

        stmt.executeUpdate();
        System.out.println("Fine added for plate: " + fine.getPlate());

    } catch (SQLException e) {
        e.printStackTrace();
    }
}

@Override //retrieve unpaid fines for a plate 
public java.util.List<model.FineRecord> getUnpaidFines(String plate) {

    java.util.List<model.FineRecord> fines = new java.util.ArrayList<>();

    String sql = """
        SELECT plate, reason, amount, issued_at, paid
        FROM fine
        WHERE plate = ? AND paid = 0
        ORDER BY fine_id DESC;
    """;

    try (PreparedStatement stmt = conn.prepareStatement(sql)) {
        stmt.setString(1, plate);

        try (ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                model.FineRecord f = new model.FineRecord(
                        rs.getString("plate"),
                        rs.getString("reason"),
                        rs.getDouble("amount"),
                        rs.getString("issued_at"),
                        rs.getInt("paid") == 1
                );
                fines.add(f);
            }
        }

    } catch (SQLException e) {
        e.printStackTrace();
    }

    return fines;
}

@Override //mark all fines as paid for a plates (dah bayar)
public void markAllFinesPaid(String plate, String paidTimeISO) {

    String sql = """
        UPDATE fine
        SET paid = 1,
            paid_at = ?
        WHERE plate = ? AND paid = 0;
    """;

    try (PreparedStatement stmt = conn.prepareStatement(sql)) {

        stmt.setString(1, paidTimeISO);
        stmt.setString(2, plate);

        int rows = stmt.executeUpdate();
        System.out.println("markAllFinesPaid updated rows: " + rows);

    } catch (SQLException e) {
        e.printStackTrace();
    }
}

@Override //insert a payment record
public void createPayment(model.PaymentRecord p) {

    String sql = """
        INSERT INTO payment
        (ticket_no, plate, method, paid_time, parking_fee, fine_paid, total_due, amount_paid, balance)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?);
    """;

    try (PreparedStatement stmt = conn.prepareStatement(sql)) {

        stmt.setString(1, p.getTicketNo());
        stmt.setString(2, p.getPlate());
        stmt.setString(3, p.getMethod());
        stmt.setString(4, p.getPaidTime());

        stmt.setDouble(5, p.getParkingFee());
        stmt.setDouble(6, p.getFinePaid());
        stmt.setDouble(7, p.getTotalDue());
        stmt.setDouble(8, p.getAmountPaid());
        stmt.setDouble(9, p.getBalance());

        stmt.executeUpdate();
        System.out.println("Payment saved for ticket: " + p.getTicketNo());

    } catch (SQLException e) {
        e.printStackTrace();
    }
}


}
