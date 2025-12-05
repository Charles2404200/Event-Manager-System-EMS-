CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ===========================================================
-- PERSON (base)
-- ===========================================================
CREATE TABLE IF NOT EXISTS persons (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    full_name TEXT NOT NULL,
    dob DATE,
    email TEXT,
    phone TEXT,
    username TEXT UNIQUE NOT NULL,
    password_hash TEXT NOT NULL,
    role TEXT NOT NULL,                 -- ATTENDEE, PRESENTER, EVENT_ADMIN, SYSTEM_ADMIN
    bio TEXT,
    created_at TIMESTAMP DEFAULT NOW()
);

-- ===========================================================
-- ATTENDEE (subclass of Person)
-- ===========================================================
CREATE TABLE IF NOT EXISTS attendees (
    id UUID PRIMARY KEY REFERENCES persons(id) ON DELETE CASCADE,
    activity_history TEXT DEFAULT '[]'  -- JSON array of activity strings
);

-- ===========================================================
-- PRESENTER (subclass of Person)
-- ===========================================================
CREATE TABLE IF NOT EXISTS presenters (
    id UUID PRIMARY KEY REFERENCES persons(id) ON DELETE CASCADE,
    presenter_type TEXT,                -- KEYNOTE_SPEAKER, PANELIST, MODERATOR, GUEST
    material_paths TEXT DEFAULT '[]'    -- JSON array of material paths
);

-- ===========================================================
-- EVENT
-- ===========================================================
CREATE TABLE IF NOT EXISTS events (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name TEXT NOT NULL,
    type TEXT NOT NULL,                 -- CONFERENCE, WORKSHOP,...
    location TEXT,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    status TEXT DEFAULT 'SCHEDULED',
    image_path TEXT,
    created_at TIMESTAMP DEFAULT NOW()
);

-- ===========================================================
-- SESSION
-- ===========================================================
CREATE TABLE IF NOT EXISTS sessions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    event_id UUID REFERENCES events(id) ON DELETE CASCADE,
    title TEXT NOT NULL,
    description TEXT,
    venue TEXT,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    capacity INT DEFAULT 100,
    material_path TEXT,
    created_at TIMESTAMP DEFAULT NOW()
);

-- Presenter <-> Session (many-to-many)
CREATE TABLE IF NOT EXISTS presenter_session (
    presenter_id UUID REFERENCES presenters(id) ON DELETE CASCADE,
    session_id UUID REFERENCES sessions(id) ON DELETE CASCADE,
    PRIMARY KEY (presenter_id, session_id)
);

-- ===========================================================
-- ATTENDEE REGISTRATION (many-to-many)
-- ===========================================================
CREATE TABLE IF NOT EXISTS attendee_session (
    attendee_id UUID REFERENCES attendees(id) ON DELETE CASCADE,
    session_id UUID REFERENCES sessions(id) ON DELETE CASCADE,
    PRIMARY KEY (attendee_id, session_id)
);

-- Auto-register event when attendee joins session
CREATE TABLE IF NOT EXISTS attendee_event (
    attendee_id UUID REFERENCES attendees(id) ON DELETE CASCADE,
    event_id UUID REFERENCES events(id) ON DELETE CASCADE,
    PRIMARY KEY (attendee_id, event_id)
);

-- ===========================================================
-- TICKETS
-- ===========================================================
CREATE TABLE IF NOT EXISTS tickets (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    attendee_id UUID REFERENCES attendees(id) ON DELETE SET NULL,
    event_id UUID REFERENCES events(id) ON DELETE SET NULL,
    session_id UUID REFERENCES sessions(id) ON DELETE SET NULL,
    type TEXT NOT NULL,                 -- GENERAL, VIP, EARLY_BIRD
    status TEXT DEFAULT 'ACTIVE',       -- ACTIVE, USED, CANCELLED
    payment_status TEXT DEFAULT 'PAID', -- PAID, UNPAID, REFUNDED
    qr_code_data TEXT,
    price NUMERIC(10, 2) NOT NULL,
    created_at TIMESTAMP DEFAULT NOW()
);

-- ===========================================================
-- INDEXING FOR PERFORMANCE
-- ===========================================================

-- PERSONS Table Indexes
CREATE INDEX IF NOT EXISTS idx_persons_username ON persons(username);
CREATE INDEX IF NOT EXISTS idx_persons_email ON persons(email);
CREATE INDEX IF NOT EXISTS idx_persons_role ON persons(role);
CREATE INDEX IF NOT EXISTS idx_persons_created_at ON persons(created_at);

-- EVENTS Table Indexes
CREATE INDEX IF NOT EXISTS idx_events_type ON events(type);
CREATE INDEX IF NOT EXISTS idx_events_status ON events(status);
CREATE INDEX IF NOT EXISTS idx_events_start_date ON events(start_date);
CREATE INDEX IF NOT EXISTS idx_events_end_date ON events(end_date);
CREATE INDEX IF NOT EXISTS idx_events_created_at ON events(created_at);
-- Composite index for date range queries
CREATE INDEX IF NOT EXISTS idx_events_date_range ON events(start_date, end_date);

-- SESSIONS Table Indexes
CREATE INDEX IF NOT EXISTS idx_sessions_event ON sessions(event_id);
CREATE INDEX IF NOT EXISTS idx_sessions_start_time ON sessions(start_time);
CREATE INDEX IF NOT EXISTS idx_sessions_end_time ON sessions(end_time);
CREATE INDEX IF NOT EXISTS idx_sessions_created_at ON sessions(created_at);
-- Composite index for event + time queries
CREATE INDEX IF NOT EXISTS idx_sessions_event_time ON sessions(event_id, start_time, end_time);

-- ATTENDEES Table Indexes (for queries on persons joined with attendees)
CREATE INDEX IF NOT EXISTS idx_attendees_id ON attendees(id);

-- PRESENTERS Table Indexes
CREATE INDEX IF NOT EXISTS idx_presenters_id ON presenters(id);
CREATE INDEX IF NOT EXISTS idx_presenters_type ON presenters(presenter_type);

-- PRESENTER_SESSION (Many-to-Many) Indexes
CREATE INDEX IF NOT EXISTS idx_presenter_session_presenter ON presenter_session(presenter_id);
CREATE INDEX IF NOT EXISTS idx_presenter_session_session ON presenter_session(session_id);
-- Covering index for both directions
CREATE INDEX IF NOT EXISTS idx_presenter_session_both ON presenter_session(presenter_id, session_id);

-- ATTENDEE_SESSION (Many-to-Many) Indexes
CREATE INDEX IF NOT EXISTS idx_attendee_session_attendee ON attendee_session(attendee_id);
CREATE INDEX IF NOT EXISTS idx_attendee_session_session ON attendee_session(session_id);
-- Covering index for both directions
CREATE INDEX IF NOT EXISTS idx_attendee_session_both ON attendee_session(attendee_id, session_id);

-- ATTENDEE_EVENT (Many-to-Many) Indexes
CREATE INDEX IF NOT EXISTS idx_attendee_event_attendee ON attendee_event(attendee_id);
CREATE INDEX IF NOT EXISTS idx_attendee_event_event ON attendee_event(event_id);
-- Covering index for both directions
CREATE INDEX IF NOT EXISTS idx_attendee_event_both ON attendee_event(attendee_id, event_id);

-- TICKETS Table Indexes
CREATE INDEX IF NOT EXISTS idx_ticket_event ON tickets(event_id);
CREATE INDEX IF NOT EXISTS idx_ticket_session ON tickets(session_id);
CREATE INDEX IF NOT EXISTS idx_ticket_attendee ON tickets(attendee_id);
CREATE INDEX IF NOT EXISTS idx_ticket_type ON tickets(type);
CREATE INDEX IF NOT EXISTS idx_ticket_status ON tickets(status);
CREATE INDEX IF NOT EXISTS idx_ticket_payment_status ON tickets(payment_status);
CREATE INDEX IF NOT EXISTS idx_ticket_created_at ON tickets(created_at);
-- Composite indexes for common query patterns
CREATE INDEX IF NOT EXISTS idx_ticket_attendee_status ON tickets(attendee_id, status);
CREATE INDEX IF NOT EXISTS idx_ticket_event_status ON tickets(event_id, status);
CREATE INDEX IF NOT EXISTS idx_ticket_session_attendee ON tickets(session_id, attendee_id);
