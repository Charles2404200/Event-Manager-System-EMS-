package org.ems.infrastructure.db;

import org.ems.config.DatabaseConfig;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.UUID;

/**
 * @author <your group number>
 */
public class DataSeeder {

    public static void seedAdminUser() {
        try (Connection conn = DatabaseConfig.getConnection()) {
            // Check if admin already exists
            String checkQuery = "SELECT id FROM persons WHERE username = 'admin' LIMIT 1";
            try (PreparedStatement checkStmt = conn.prepareStatement(checkQuery)) {
                ResultSet rs = checkStmt.executeQuery();
                if (rs.next()) {
                    System.out.println(" Admin user already exists, skipping seeding.");
                    return;
                }
            }

            String insertQuery = """
                    INSERT INTO persons (id, full_name, dob, email, phone, username, password_hash, role, created_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW())
                    """;

            try (PreparedStatement stmt = conn.prepareStatement(insertQuery)) {
                stmt.setObject(1, UUID.randomUUID());
                stmt.setString(2, "System Administrator");
                stmt.setObject(3, LocalDate.of(1990, 1, 1));
                stmt.setString(4, "admin@eventsystem.com");
                stmt.setString(5, "+1234567890");
                stmt.setString(6, "admin");
                stmt.setString(7, "admin123");
                stmt.setString(8, "SYSTEM_ADMIN");

                stmt.executeUpdate();
                System.out.println(" Admin user seeded successfully!");
                System.out.println("   Username: admin");
                System.out.println("   Password: admin123");
            }

        } catch (Exception e) {
            System.err.println(" Error seeding admin user: " + e.getMessage());
        }
    }

    public static void seedSampleData() {
        try (Connection conn = DatabaseConfig.getConnection()) {
            UUID attendeeId = UUID.randomUUID();
            String attendeePersonQuery = """
                    INSERT INTO persons (id, full_name, dob, email, phone, username, password_hash, role, created_at)
                    SELECT ?, ?, ?, ?, ?, ?, ?, ?, NOW()
                    WHERE NOT EXISTS (SELECT 1 FROM persons WHERE username = 'testattendee')
                    """;

            try (PreparedStatement stmt = conn.prepareStatement(attendeePersonQuery)) {
                stmt.setObject(1, attendeeId);
                stmt.setString(2, "John Attendee");
                stmt.setObject(3, LocalDate.of(1995, 5, 15));
                stmt.setString(4, "john.attendee@example.com");
                stmt.setString(5, "+1111111111");
                stmt.setString(6, "testattendee");
                stmt.setString(7, "password123");
                stmt.setString(8, "ATTENDEE");

                stmt.executeUpdate();
            }

            String attendeeSubQuery = """
                    INSERT INTO attendees (id, activity_history)
                    SELECT ?, ?
                    WHERE NOT EXISTS (SELECT 1 FROM attendees WHERE id = ?)
                    """;

            try (PreparedStatement stmt = conn.prepareStatement(attendeeSubQuery)) {
                stmt.setObject(1, attendeeId);
                stmt.setString(2, "[]");
                stmt.setObject(3, attendeeId);
                stmt.executeUpdate();
                System.out.println(" Sample attendee user seeded!");
            }

            // Seed sample presenter
            UUID presenterId = UUID.randomUUID();

            // Insert into persons table
            String presenterPersonQuery = """
                    INSERT INTO persons (id, full_name, dob, email, phone, username, password_hash, role, created_at)
                    SELECT ?, ?, ?, ?, ?, ?, ?, ?, NOW()
                    WHERE NOT EXISTS (SELECT 1 FROM persons WHERE username = 'testpresenter')
                    """;

            try (PreparedStatement stmt = conn.prepareStatement(presenterPersonQuery)) {
                stmt.setObject(1, presenterId);
                stmt.setString(2, "Jane Presenter");
                stmt.setObject(3, LocalDate.of(1990, 3, 20));
                stmt.setString(4, "jane.presenter@example.com");
                stmt.setString(5, "+2222222222");
                stmt.setString(6, "testpresenter");
                stmt.setString(7, "password123");
                stmt.setString(8, "PRESENTER");

                stmt.executeUpdate();
            }

            // Insert into presenters table
            String presenterSubQuery = """
                    INSERT INTO presenters (id, presenter_type, material_paths)
                    SELECT ?, ?, ?
                    WHERE NOT EXISTS (SELECT 1 FROM presenters WHERE id = ?)
                    """;

            try (PreparedStatement stmt = conn.prepareStatement(presenterSubQuery)) {
                stmt.setObject(1, presenterId);
                stmt.setString(2, "KEYNOTE_SPEAKER");
                stmt.setString(3, "[]");
                stmt.setObject(4, presenterId);
                stmt.executeUpdate();
                System.out.println(" Sample presenter user seeded!");
            }

        } catch (Exception e) {
            System.err.println(" Error seeding sample data: " + e.getMessage());
        }
    }
}

