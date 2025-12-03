-- H2 Compatible Schema for Event Manager System
-- H2 has built-in UUID support, no extension needed

-- ===========================================================
-- PERSON (base)
-- ===========================================================
CREATE TABLE IF NOT EXISTS persons (
    id UUID DEFAULT RANDOM_UUID() PRIMARY KEY,
    full_name VARCHAR(255) NOT NULL,
    dob DATE,
    email VARCHAR(255),
    phone VARCHAR(50),
    username VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    bio CLOB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ===========================================================
-- ATTENDEE (subclass of Person)
-- ===========================================================
CREATE TABLE IF NOT EXISTS attendees (
    id UUID PRIMARY KEY,
    activity_history CLOB DEFAULT '[]',
    FOREIGN KEY (id) REFERENCES persons(id) ON DELETE CASCADE
);

-- ===========================================================
-- PRESENTER (subclass of Person)
-- ===========================================================
CREATE TABLE IF NOT EXISTS presenters (
    id UUID PRIMARY KEY,
    presenter_type VARCHAR(50),
    material_paths CLOB DEFAULT '[]',
    FOREIGN KEY (id) REFERENCES persons(id) ON DELETE CASCADE
);

-- ===========================================================
-- EVENT
-- ===========================================================
CREATE TABLE IF NOT EXISTS events (
    id UUID DEFAULT RANDOM_UUID() PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(100) NOT NULL,
    location VARCHAR(255),
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    status VARCHAR(50) DEFAULT 'SCHEDULED',
    image_path VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ===========================================================
-- SESSION
-- ===========================================================
CREATE TABLE IF NOT EXISTS sessions (
    id UUID DEFAULT RANDOM_UUID() PRIMARY KEY,
    event_id UUID,
    title VARCHAR(255) NOT NULL,
    description CLOB,
    venue VARCHAR(255),
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    capacity INT DEFAULT 100,
    material_path VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE
);

-- Presenter <-> Session (many-to-many)
CREATE TABLE IF NOT EXISTS presenter_session (
    presenter_id UUID NOT NULL,
    session_id UUID NOT NULL,
    PRIMARY KEY (presenter_id, session_id),
    FOREIGN KEY (presenter_id) REFERENCES presenters(id) ON DELETE CASCADE,
    FOREIGN KEY (session_id) REFERENCES sessions(id) ON DELETE CASCADE
);

-- ===========================================================
-- ATTENDEE REGISTRATION (many-to-many)
-- ===========================================================
CREATE TABLE IF NOT EXISTS attendee_session (
    attendee_id UUID NOT NULL,
    session_id UUID NOT NULL,
    PRIMARY KEY (attendee_id, session_id),
    FOREIGN KEY (attendee_id) REFERENCES attendees(id) ON DELETE CASCADE,
    FOREIGN KEY (session_id) REFERENCES sessions(id) ON DELETE CASCADE
);

-- Auto-register event when attendee joins session
CREATE TABLE IF NOT EXISTS attendee_event (
    attendee_id UUID NOT NULL,
    event_id UUID NOT NULL,
    PRIMARY KEY (attendee_id, event_id),
    FOREIGN KEY (attendee_id) REFERENCES attendees(id) ON DELETE CASCADE,
    FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE
);

-- ===========================================================
-- TICKETS
-- ===========================================================
CREATE TABLE IF NOT EXISTS tickets (
    id UUID DEFAULT RANDOM_UUID() PRIMARY KEY,
    attendee_id UUID,
    event_id UUID,
    session_id UUID,
    type VARCHAR(50) NOT NULL,
    status VARCHAR(50) DEFAULT 'ACTIVE',
    payment_status VARCHAR(50) DEFAULT 'PAID',
    qr_code_data CLOB,
    price NUMERIC(10, 2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (attendee_id) REFERENCES attendees(id) ON DELETE SET NULL,
    FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE SET NULL,
    FOREIGN KEY (session_id) REFERENCES sessions(id) ON DELETE SET NULL
);

-- ===========================================================
-- INDEXING FOR PERFORMANCE
-- ===========================================================
CREATE INDEX IF NOT EXISTS idx_sessions_event ON sessions(event_id);
CREATE INDEX IF NOT EXISTS idx_presenter_session_presenter ON presenter_session(presenter_id);
CREATE INDEX IF NOT EXISTS idx_attendee_session_attendee ON attendee_session(attendee_id);
CREATE INDEX IF NOT EXISTS idx_ticket_event ON tickets(event_id);
CREATE INDEX IF NOT EXISTS idx_ticket_session ON tickets(session_id);
CREATE INDEX IF NOT EXISTS idx_ticket_attendee ON tickets(attendee_id);

