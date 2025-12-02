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
CREATE INDEX IF NOT EXISTS idx_sessions_event ON sessions(event_id);
CREATE INDEX IF NOT EXISTS idx_presenter_session_presenter ON presenter_session(presenter_id);
CREATE INDEX IF NOT EXISTS idx_attendee_session_attendee ON attendee_session(attendee_id);
CREATE INDEX IF NOT EXISTS idx_ticket_event ON tickets(event_id);
CREATE INDEX IF NOT EXISTS idx_ticket_session ON tickets(session_id);
CREATE INDEX IF NOT EXISTS idx_ticket_attendee ON tickets(attendee_id);
