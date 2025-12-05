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

    public static void main(String[] args) {
        System.out.println("\n╔════════════════════════════════════════╗");
        System.out.println("║   Event Manager System - Data Seeder   ║");
        System.out.println("╚════════════════════════════════════════╝\n");

        seedAdminUser();
        seedSampleData();

        System.out.println("\n✅ Data seeding completed successfully!\n");
    }

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

            String checkQuery = "SELECT id FROM persons WHERE username = ?";
            String personQuery = """
                    INSERT INTO persons (id, full_name, dob, email, phone, username, password_hash, role, created_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW())
                    """;

            String attendeeQuery = """
                    INSERT INTO attendees (id, activity_history)
                    VALUES (?, '[]')
                    """;

            int addedCount = 0;
            for (String data : attendees) {
                String[] parts = data.split(",");
                String username = parts[0];

                // Check if already exists
                boolean exists = false;
                try (PreparedStatement checkStmt = conn.prepareStatement(checkQuery)) {
                    checkStmt.setString(1, username);
                    ResultSet rs = checkStmt.executeQuery();
                    exists = rs.next();
                }

                if (exists) continue;

                UUID id = UUID.randomUUID();

                try (PreparedStatement personStmt = conn.prepareStatement(personQuery)) {
                    personStmt.setObject(1, id);
                    personStmt.setString(2, parts[1]);
                    personStmt.setObject(3, LocalDate.of(1995, 1, 1));
                    personStmt.setString(4, parts[2]);
                    personStmt.setString(5, parts[3]);
                    personStmt.setString(6, username);
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
                "dr_smith,Dr. Smith,KEYNOTE_SPEAKER,smith@example.com,+84902234567",
                "prof_jones,Prof. Jones,PANELIST,jones@example.com,+84902234568",
                "engineer_lee,Lee Chen,MODERATOR,lee@example.com,+84902234569",
                "architect_kim,Kim Park,GUEST,kim@example.com,+84902234570",
                "consultant_nguyen,Nguyen Minh,KEYNOTE_SPEAKER,nguyen@example.com,+84902234571",
                "master_tran,Tran Anh,PANELIST,tran@example.com,+84902234572",
            };

            String checkQuery = "SELECT id FROM persons WHERE username = ?";
            String personQuery = """
                    INSERT INTO persons (id, full_name, dob, email, phone, username, password_hash, role, created_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW())
                    """;

            String presenterQuery = """
                    INSERT INTO presenters (id, presenter_type)
                    VALUES (?, ?)
                    """;

            int addedCount = 0;
            for (String data : presenters) {
                String[] parts = data.split(",");
                String username = parts[0];

                // Check if already exists
                boolean exists = false;
                try (PreparedStatement checkStmt = conn.prepareStatement(checkQuery)) {
                    checkStmt.setString(1, username);
                    ResultSet rs = checkStmt.executeQuery();
                    exists = rs.next();
                }

                if (exists) continue;

                UUID id = UUID.randomUUID();

                try (PreparedStatement personStmt = conn.prepareStatement(personQuery)) {
                    personStmt.setObject(1, id);
                    personStmt.setString(2, parts[1]);
                    personStmt.setObject(3, LocalDate.of(1980, 1, 1));
                    personStmt.setString(4, parts[3]);
                    personStmt.setString(5, parts[4]);
                    personStmt.setString(6, username);
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

            // Event templates
            String[] eventNames = {
                "Tech Conference", "Web Development Workshop", "AI & Machine Learning Summit",
                "Cloud Computing Seminar", "Java Enterprise Development", "DevOps Best Practices",
                "React Advanced Patterns", "Mobile Development Summit", "Python Mastery",
                "Microservices Architecture", "Kubernetes Deep Dive", "Docker Essentials",
                "GraphQL Advanced Techniques", "Vue.js Expert Training", "Angular Professional",
                "Node.js Performance Tuning", "TypeScript Masterclass", "Front-End Excellence",
                "Backend Development Bootcamp", "Full Stack Engineering", "Cybersecurity Summit",
                "Data Science Conference", "Big Data Analytics", "Machine Learning Ops",
                "Blockchain Technology", "Cryptocurrency Fundamentals", "Smart Contracts",
                "Web3 Development", "NFT & DeFi Summit", "Cloud Architecture",
                "AWS Solutions", "Azure Masterclass", "GCP Advanced Features",
                "Database Design", "SQL Performance", "NoSQL Databases",
                "PostgreSQL Advanced", "MongoDB Mastery", "Redis Optimization",
                "ElasticSearch Advanced", "API Design Best Practices", "REST API Mastery",
                "Testing Strategies", "Quality Assurance Summit", "Agile Methodology",
                "Scrum Master Workshop", "DevOps Pipeline", "CI/CD Excellence",
                "Git Workflow Advanced", "Linux Administration", "System Design",
                "Software Architecture", "Design Patterns", "Clean Code Principles",
                "Refactoring Techniques", "Performance Optimization", "Memory Management",
                "Concurrency Programming", "Parallel Processing", "Distributed Systems",
                "Load Balancing Strategies", "Database Scaling", "Caching Techniques",
                "Message Queues", "Event Streaming", "Real-time Applications",
                "WebSocket Programming", "Mobile App Development", "iOS Development",
                "Android Development", "React Native Mastery", "Flutter Advanced",
                "Cross-platform Development", "Progressive Web Apps", "Responsive Design",
                "UX/UI Principles", "User Experience Design", "Accessibility Standards",
                "Performance Metrics", "Monitoring & Logging", "Observability",
                "Open Source Contribution", "Community Building", "Tech Leadership",
                "Career Development", "Interview Preparation", "Salary Negotiation",
                "Freelancing Guide", "Remote Work Best Practices", "Team Collaboration",
                "Communication Skills", "Presentation Mastery", "Technical Writing"
            };

            String[] types = {"CONFERENCE", "WORKSHOP", "SEMINAR", "EXHIBITION", "CONCERT"};
            String[] locations = {
                "Hanoi Convention Center", "Da Nang Tech Hub", "Ho Chi Minh City Arena",
                "Hanoi Business Center", "Da Nang Arena", "Ho Chi Minh City Hub",
                "Hanoi Tech Center", "Da Nang Conference Hall", "HCMC Tech Park",
                "Hanoi International Center", "Da Nang Summit Hall", "HCMC Convention"
            };

            int addedCount = 0;
            LocalDate startDate = LocalDate.of(2025, 12, 10);

            // Generate 100 events
            for (int i = 0; i < 100; i++) {
                String eventName = eventNames[i % eventNames.length] + " #" + (i + 1);
                String type = types[i % types.length];
                String location = locations[i % locations.length];
                LocalDate eventStart = startDate.plusDays(i / 2);
                LocalDate eventEnd = eventStart.plusDays((i % 3) + 1);
                String status = (i % 5 == 0) ? "COMPLETED" : (i % 3 == 0) ? "ONGOING" : "SCHEDULED";

                try (PreparedStatement stmt = conn.prepareStatement(eventQuery)) {
                    stmt.setObject(1, UUID.randomUUID());
                    stmt.setString(2, eventName);
                    stmt.setString(3, type);
                    stmt.setString(4, location);
                    stmt.setObject(5, eventStart);
                    stmt.setObject(6, eventEnd);
                    stmt.setString(7, status);
                    int rows = stmt.executeUpdate();
                    if (rows > 0) addedCount++;
                }
            }

            System.out.println(" ✓ Added " + addedCount + " new events (total: 100)");

        } catch (Exception e) {
            System.err.println(" Error seeding events: " + e.getMessage());
        }
    }

    private static void seedSessions(Connection conn) {
        try {
            // Get all event IDs
            String getEventsQuery = "SELECT id FROM events ORDER BY created_at DESC LIMIT 100";
            java.util.List<UUID> eventIds = new java.util.ArrayList<>();
            try (PreparedStatement stmt = conn.prepareStatement(getEventsQuery)) {
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    eventIds.add((UUID) rs.getObject("id"));
                }
            }

            if (eventIds.isEmpty()) {
                System.out.println(" No events found for seeding sessions");
                return;
            }

            String sessionQuery = """
                    INSERT INTO sessions (id, event_id, title, description, start_time, end_time, venue, capacity, created_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW())
                    """;

            // Session templates (200+)
            String[] sessionTitles = {
                "Opening Keynote", "Web Development Best Practices", "Cloud Infrastructure Deep Dive",
                "AI Workshop", "Networking Session", "Advanced Java Patterns", "Database Optimization",
                "DevOps & CI/CD Pipeline", "Kubernetes Mastery", "Microservices Architecture",
                "Security Best Practices", "Performance Tuning", "Code Review Session", "Testing Strategies",
                "Agile Retrospective", "Scrum Planning", "Git Advanced Workflows", "Linux Essentials",
                "System Design Principles", "Software Architecture Patterns", "Design Patterns Workshop",
                "Clean Code Principles", "Refactoring Practice Session", "Memory Management",
                "Concurrency Programming", "Parallel Processing Basics", "Distributed Systems",
                "Load Balancing Techniques", "Database Scaling Strategies", "Caching Best Practices",
                "Message Queue Systems", "Event Streaming Basics", "Real-time Applications",
                "WebSocket Implementation", "Mobile Development Basics", "iOS Workshop",
                "Android Development", "React Native Bootcamp", "Flutter Introduction",
                "Cross-platform Solutions", "Progressive Web Apps", "Responsive Design Patterns",
                "UX/UI Fundamentals", "User Experience Optimization", "Accessibility Compliance",
                "Performance Metrics", "Monitoring & Logging", "Observability Practices",
                "Open Source Contribution", "Community Engagement", "Tech Leadership",
                "Career Planning", "Interview Preparation", "Salary Negotiation",
                "Freelancing Workshop", "Remote Work Best Practices", "Team Collaboration",
                "Communication Skills", "Presentation Training", "Technical Writing",
                "API Design Masterclass", "REST API Best Practices", "GraphQL Advanced",
                "Frontend Optimization", "Backend Performance", "Full Stack Development",
                "Deployment Strategies", "Infrastructure as Code", "Container Orchestration",
                "Serverless Architecture", "Microservices Patterns", "Event-Driven Design",
                "Database Modeling", "SQL Advanced Queries", "NoSQL Patterns",
                "Search Engine Optimization", "Analytics Implementation", "Data Visualization",
                "Machine Learning Basics", "Deep Learning Introduction", "NLP Workshop",
                "Computer Vision", "Reinforcement Learning", "TensorFlow Masterclass",
                "PyTorch Advanced", "Data Pipeline Design", "ETL Processes",
                "Data Quality Assurance", "Compliance & Regulations", "Security Hardening",
                "Encryption & Hashing", "Authentication & Authorization", "OWASP Top 10",
                "Penetration Testing", "Vulnerability Assessment", "Security Auditing",
                "Incident Response", "Disaster Recovery", "Business Continuity",
                "Cloud Migration", "Hybrid Cloud Strategy", "Multi-cloud Architecture",
                "Cost Optimization", "Resource Management", "Performance Benchmarking",
                "Load Testing", "Stress Testing", "Chaos Engineering",
                "Monitoring & Alerting", "Log Management", "Distributed Tracing",
                "APM Tools", "Metrics Collection", "Time Series Databases",
                "Version Control Strategies", "Branching Models", "Merge Strategies",
                "Code Quality", "Static Analysis", "Dynamic Analysis",
                "Unit Testing", "Integration Testing", "End-to-End Testing",
                "Test Automation", "BDD & TDD", "Continuous Testing",
                "Agile Transformation", "Kanban Practices", "Lean Methodology",
                "SAFe Framework", "DevOps Culture", "Cross-functional Teams",
                "Product Management", "Requirements Gathering", "User Stories",
                "Feature Planning", "Release Management", "Sprint Planning",
                "Daily Standup", "Sprint Review", "Sprint Retrospective",
                "Technical Debt", "Architecture Review", "Design Review",
                "Code Review Best Practices", "Peer Programming", "Mob Programming",
                "Continuous Improvement", "Metrics & KPIs", "Business Alignment",
                "Stakeholder Management", "Risk Management", "Quality Metrics",
                "Velocity Tracking", "Burndown Charts", "Release Planning",
                "Roadmap Planning", "Strategic Alignment", "OKR Framework",
                "Leadership Skills", "Decision Making", "Problem Solving",
                "Conflict Resolution", "Change Management", "Innovation",
                "Mentoring & Coaching", "Delegation", "Performance Management",
                "Feedback Loops", "Continuous Learning", "Knowledge Sharing",
                "Documentation", "Knowledge Management", "Best Practices",
                "Lessons Learned", "Post-mortems", "Root Cause Analysis",
                "Retrospectives", "Forward Planning", "Goal Setting"
            };

            String[] venues = {
                "Main Hall", "Room 101", "Room 102", "Room 201", "Room 202", "Room 203",
                "Lab Room", "Conference Room A", "Conference Room B", "Auditorium",
                "Breakout Room 1", "Breakout Room 2", "Breakout Room 3", "Networking Area",
                "Workshop Space", "Studio A", "Studio B", "Demo Area", "Theater Room",
                "Board Room", "Executive Suite", "Innovation Lab"
            };

            String[] descriptions = {
                "Hands-on practical training", "Expert insights and strategies", "Advanced techniques workshop",
                "Beginner to intermediate level", "Best practices and case studies", "Deep dive into technology",
                "Live coding session", "Interactive Q&A", "Networking opportunity", "Group discussion",
                "Problem-solving session", "Case study analysis", "Demonstration and tutorial",
                "Mentoring session", "Breakout group discussion", "Working session", "Practical exercise"
            };

            int sessionCount = 0;
            int sessionsPerEvent = 2; // 2 sessions per event = ~200 sessions for 100 events

            for (int eventIdx = 0; eventIdx < eventIds.size(); eventIdx++) {
                UUID eventId = eventIds.get(eventIdx);

                for (int sessionIdx = 0; sessionIdx < sessionsPerEvent; sessionIdx++) {
                    int titleIdx = (eventIdx * sessionsPerEvent + sessionIdx) % sessionTitles.length;
                    String title = sessionTitles[titleIdx];
                    String description = descriptions[(eventIdx + sessionIdx) % descriptions.length];
                    String venue = venues[(eventIdx + sessionIdx) % venues.length];
                    int capacity = 50 + (eventIdx % 4) * 25; // 50, 75, 100, 125

                    // Fix: Calculate day properly - use modulo to stay within month bounds
                    int dayOffset = (eventIdx / 4) % 20; // 0-19 days (safe for any month)
                    int hour = 9 + (sessionIdx * 2);
                    LocalDateTime startTime = LocalDateTime.of(2025, 12, 10 + dayOffset, hour, 0);
                    LocalDateTime endTime = startTime.plusHours(1).plusMinutes(30);

                    try (PreparedStatement stmt = conn.prepareStatement(sessionQuery)) {
                        stmt.setObject(1, UUID.randomUUID());
                        stmt.setObject(2, eventId);
                        stmt.setString(3, title + " - " + (sessionIdx + 1));
                        stmt.setString(4, description);
                        stmt.setObject(5, startTime);
                        stmt.setObject(6, endTime);
                        stmt.setString(7, venue);
                        stmt.setInt(8, capacity);
                        int rows = stmt.executeUpdate();
                        if (rows > 0) sessionCount++;
                    }
                }
            }

            System.out.println(" ✓ Added " + sessionCount + " new sessions");

        } catch (Exception e) {
            System.err.println(" Error seeding sessions: " + e.getMessage());
        }
    }

    private static void seedTickets(Connection conn) {
        try {
            // Get attendee and session IDs
            String attendeesQuery = "SELECT id FROM attendees";
            java.util.List<UUID> attendeeIds = new java.util.ArrayList<>();
            try (PreparedStatement stmt = conn.prepareStatement(attendeesQuery)) {
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    attendeeIds.add((UUID) rs.getObject("id"));
                }
            }

            String sessionsQuery = "SELECT id, event_id FROM sessions";
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
            // Create multiple tickets per attendee-session pair for realistic data
            for (int i = 0; i < attendeeIds.size() * 30; i++) {
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

