package org.ems.infrastructure.db;

import org.ems.config.DatabaseConfig;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
            }

        } catch (Exception e) {
            System.err.println(" Error seeding admin user: " + e.getMessage());
        }
    }

    public static void seedSampleData() {
        try (Connection conn = DatabaseConfig.getConnection()) {
            System.out.println("\n=== Starting Data Seeding ===\n");

            // Seed Attendees
            seedAttendees(conn);

            // Seed Presenters
            seedPresenters(conn);

            // Seed Events
            seedEvents(conn);

            // Seed Sessions
            seedSessions(conn);

            // Seed Tickets
            seedTickets(conn);

            System.out.println("\n=== Data Seeding Complete ===\n");

        } catch (Exception e) {
            System.err.println(" Error seeding sample data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void seedAttendees(Connection conn) {
        try {
            String[] attendees = {
                "john_doe,John Doe,john@example.com,+84901234567",
                "jane_smith,Jane Smith,jane@example.com,+84901234568",
                "bob_wilson,Bob Wilson,bob@example.com,+84901234569",
                "alice_johnson,Alice Johnson,alice@example.com,+84901234570",
                "charlie_brown,Charlie Brown,charlie@example.com,+84901234571",
                "david_lee,David Lee,david@example.com,+84901234572",
                "emma_watson,Emma Watson,emma@example.com,+84901234573",
                "frank_miller,Frank Miller,frank@example.com,+84901234574",
            };

            String personQuery = """
                    INSERT INTO persons (id, full_name, dob, email, phone, username, password_hash, role, created_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW())
                    ON CONFLICT DO NOTHING
                    """;

            String attendeeQuery = """
                    INSERT INTO attendees (id, activity_history)
                    VALUES (?, '[]')
                    ON CONFLICT DO NOTHING
                    """;

            int addedCount = 0;
            for (String data : attendees) {
                String[] parts = data.split(",");
                UUID id = UUID.randomUUID();

                try (PreparedStatement personStmt = conn.prepareStatement(personQuery)) {
                    personStmt.setObject(1, id);
                    personStmt.setString(2, parts[1]);
                    personStmt.setObject(3, LocalDate.of(1995, 1, 1));
                    personStmt.setString(4, parts[2]);
                    personStmt.setString(5, parts[3]);
                    personStmt.setString(6, parts[0]);
                    personStmt.setString(7, "password123");
                    personStmt.setString(8, "ATTENDEE");
                    int personRows = personStmt.executeUpdate();

                    if (personRows > 0) {
                        try (PreparedStatement attendeeStmt = conn.prepareStatement(attendeeQuery)) {
                            attendeeStmt.setObject(1, id);
                            attendeeStmt.executeUpdate();
                            addedCount++;
                        }
                    }
                }
            }

            System.out.println(" ✓ Added " + addedCount + " new attendees (total: " + attendees.length + ")");

        } catch (Exception e) {
            System.err.println(" Error seeding attendees: " + e.getMessage());
        }
    }

    private static void seedPresenters(Connection conn) {
        try {
            String[] presenters = {
                "dr_smith,Dr. Smith,Senior Developer,smith@example.com,+84902234567",
                "prof_jones,Prof. Jones,Tech Lead,jones@example.com,+84902234568",
                "engineer_lee,Lee Chen,Software Engineer,lee@example.com,+84902234569",
                "architect_kim,Kim Park,Solution Architect,kim@example.com,+84902234570",
                "consultant_nguyen,Nguyen Minh,Tech Consultant,nguyen@example.com,+84902234571",
                "master_tran,Tran Anh,Full Stack Master,tran@example.com,+84902234572",
            };

            String personQuery = """
                    INSERT INTO persons (id, full_name, dob, email, phone, username, password_hash, role, created_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW())
                    ON CONFLICT DO NOTHING
                    """;

            String presenterQuery = """
                    INSERT INTO presenters (id, role)
                    VALUES (?, ?)
                    ON CONFLICT DO NOTHING
                    """;

            int addedCount = 0;
            for (String data : presenters) {
                String[] parts = data.split(",");
                UUID id = UUID.randomUUID();

                try (PreparedStatement personStmt = conn.prepareStatement(personQuery)) {
                    personStmt.setObject(1, id);
                    personStmt.setString(2, parts[1]);
                    personStmt.setObject(3, LocalDate.of(1980, 1, 1));
                    personStmt.setString(4, parts[3]);
                    personStmt.setString(5, parts[4]);
                    personStmt.setString(6, parts[0]);
                    personStmt.setString(7, "password123");
                    personStmt.setString(8, "PRESENTER");
                    int personRows = personStmt.executeUpdate();

                    if (personRows > 0) {
                        try (PreparedStatement presenterStmt = conn.prepareStatement(presenterQuery)) {
                            presenterStmt.setObject(1, id);
                            presenterStmt.setString(2, parts[2]);
                            presenterStmt.executeUpdate();
                            addedCount++;
                        }
                    }
                }
            }

            System.out.println(" ✓ Added " + addedCount + " new presenters (total: " + presenters.length + ")");

        } catch (Exception e) {
            System.err.println(" Error seeding presenters: " + e.getMessage());
        }
    }

    private static void seedEvents(Connection conn) {
        try {
            String eventQuery = """
                    INSERT INTO events (id, name, type, location, start_date, end_date, status, created_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, NOW())
                    """;

            Object[][] events = {
                {"Tech Conference 2025", "CONFERENCE", "Hanoi Convention Center", LocalDate.of(2025, 12, 10), LocalDate.of(2025, 12, 12), "SCHEDULED"},
                {"Web Development Workshop", "WORKSHOP", "Da Nang Tech Hub", LocalDate.of(2025, 12, 15), LocalDate.of(2025, 12, 16), "SCHEDULED"},
                {"AI & Machine Learning Summit", "CONFERENCE", "Ho Chi Minh City Arena", LocalDate.of(2025, 12, 20), LocalDate.of(2025, 12, 22), "SCHEDULED"},
                {"Cloud Computing Seminar", "SEMINAR", "Hanoi Business Center", LocalDate.of(2025, 12, 25), LocalDate.of(2025, 12, 25), "SCHEDULED"},
                {"Java Enterprise Development", "WORKSHOP", "Da Nang Tech Hub", LocalDate.of(2026, 1, 5), LocalDate.of(2026, 1, 7), "SCHEDULED"},
                {"DevOps Best Practices", "WORKSHOP", "Ho Chi Minh City Hub", LocalDate.of(2026, 1, 10), LocalDate.of(2026, 1, 12), "SCHEDULED"},
                {"React Advanced Patterns", "SEMINAR", "Hanoi Tech Center", LocalDate.of(2026, 1, 15), LocalDate.of(2026, 1, 15), "SCHEDULED"},
                {"Mobile Development Summit", "CONFERENCE", "Da Nang Arena", LocalDate.of(2026, 1, 20), LocalDate.of(2026, 1, 22), "SCHEDULED"},
            };

            int addedCount = 0;
            for (Object[] event : events) {
                try (PreparedStatement stmt = conn.prepareStatement(eventQuery)) {
                    stmt.setObject(1, UUID.randomUUID());
                    stmt.setString(2, (String) event[0]);
                    stmt.setString(3, (String) event[1]);
                    stmt.setString(4, (String) event[2]);
                    stmt.setObject(5, event[3]);
                    stmt.setObject(6, event[4]);
                    stmt.setString(7, (String) event[5]);
                    int rows = stmt.executeUpdate();
                    if (rows > 0) addedCount++;
                }
            }

            System.out.println(" ✓ Added " + addedCount + " new events (total: " + events.length + ")");

        } catch (Exception e) {
            System.err.println(" Error seeding events: " + e.getMessage());
        }
    }

    private static void seedSessions(Connection conn) {
        try {
            // Get event IDs
            String getEventsQuery = "SELECT id, name FROM events ORDER BY created_at DESC LIMIT 8";
            java.util.List<Object[]> eventsList = new java.util.ArrayList<>();
            try (PreparedStatement stmt = conn.prepareStatement(getEventsQuery)) {
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    eventsList.add(new Object[]{rs.getObject("id"), rs.getString("name")});
                }
            }

            if (eventsList.isEmpty()) {
                System.out.println(" No events found for seeding sessions");
                return;
            }

            String sessionQuery = """
                    INSERT INTO sessions (id, event_id, title, description, start, end, venue, capacity, created_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW())
                    """;

            Object[][] sessionData = {
                {"Opening Keynote", "Industry trends and future of technology", "Main Hall", 500},
                {"Web Development Best Practices", "Modern web development techniques", "Room 101", 100},
                {"Cloud Infrastructure Deep Dive", "AWS, Azure, and GCP comparison", "Room 202", 80},
                {"AI Workshop", "Hands-on machine learning", "Lab Room", 50},
                {"Networking Session", "Meet fellow developers and professionals", "Main Hall", 200},
                {"Advanced Java Patterns", "Design patterns in enterprise apps", "Room 301", 60},
                {"Database Optimization", "Performance tuning strategies", "Room 302", 70},
                {"DevOps & CI/CD Pipeline", "Automation and deployment strategies", "Room 303", 65},
                {"Kubernetes Mastery", "Container orchestration deep dive", "Room 304", 55},
                {"Microservices Architecture", "Building scalable systems", "Room 305", 65},
                {"Security Best Practices", "Web application security", "Room 306", 75},
                {"Performance Tuning", "Optimizing application performance", "Room 307", 60},
            };

            int sessionCount = 0;
            for (int i = 0; i < sessionData.length; i++) {
                Object[] session = sessionData[i];
                Object eventId = eventsList.get(i % eventsList.size())[0];

                LocalDateTime startTime = LocalDateTime.of(2025, 12, 10 + (i / 3), 9 + (i % 3) * 3, 0);
                LocalDateTime endTime = startTime.plusHours(1).plusMinutes(30);

                try (PreparedStatement stmt = conn.prepareStatement(sessionQuery)) {
                    stmt.setObject(1, UUID.randomUUID());
                    stmt.setObject(2, eventId);
                    stmt.setString(3, (String) session[0]);
                    stmt.setString(4, (String) session[1]);
                    stmt.setObject(5, startTime);
                    stmt.setObject(6, endTime);
                    stmt.setString(7, (String) session[2]);
                    stmt.setInt(8, (Integer) session[3]);
                    int rows = stmt.executeUpdate();
                    if (rows > 0) sessionCount++;
                }
            }

            System.out.println(" ✓ Added " + sessionCount + " new sessions (total: " + sessionData.length + ")");

        } catch (Exception e) {
            System.err.println(" Error seeding sessions: " + e.getMessage());
        }
    }

    private static void seedTickets(Connection conn) {
        try {
            // Get attendee and session IDs
            String attendeesQuery = "SELECT id FROM attendees LIMIT 10";
            java.util.List<UUID> attendeeIds = new java.util.ArrayList<>();
            try (PreparedStatement stmt = conn.prepareStatement(attendeesQuery)) {
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    attendeeIds.add((UUID) rs.getObject("id"));
                }
            }

            String sessionsQuery = "SELECT id, event_id FROM sessions ORDER BY created_at DESC LIMIT 12";
            java.util.List<Object[]> sessionIds = new java.util.ArrayList<>();
            try (PreparedStatement stmt = conn.prepareStatement(sessionsQuery)) {
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    sessionIds.add(new Object[]{rs.getObject("id"), rs.getObject("event_id")});
                }
            }

            if (attendeeIds.isEmpty() || sessionIds.isEmpty()) {
                System.out.println(" No attendees or sessions found for seeding tickets");
                return;
            }

            String ticketQuery = """
                    INSERT INTO tickets (id, attendee_id, event_id, session_id, type, price, status, payment_status, qr_code_data, created_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())
                    """;

            String[] types = {"GENERAL", "VIP", "EARLY_BIRD", "STUDENT"};
            double[] prices = {50.0, 100.0, 30.0, 25.0};

            int ticketCount = 0;
            for (int i = 0; i < attendeeIds.size() * 4; i++) {
                UUID attendeeId = attendeeIds.get(i % attendeeIds.size());
                Object[] sessionData = sessionIds.get(i % sessionIds.size());
                UUID sessionId = (UUID) sessionData[0];
                UUID eventId = (UUID) sessionData[1];

                String type = types[i % types.length];
                double price = prices[i % prices.length];
                String qrCode = "QR-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();

                try (PreparedStatement stmt = conn.prepareStatement(ticketQuery)) {
                    stmt.setObject(1, UUID.randomUUID());
                    stmt.setObject(2, attendeeId);
                    stmt.setObject(3, eventId);
                    stmt.setObject(4, sessionId);
                    stmt.setString(5, type);
                    stmt.setBigDecimal(6, new java.math.BigDecimal(price));
                    stmt.setString(7, "ACTIVE");
                    stmt.setString(8, "PAID");
                    stmt.setString(9, qrCode);
                    int rows = stmt.executeUpdate();
                    if (rows > 0) ticketCount++;
                }
            }

            System.out.println(" ✓ Added " + ticketCount + " new tickets");

        } catch (Exception e) {
            System.err.println(" Error seeding tickets: " + e.getMessage());
        }
    }
}

