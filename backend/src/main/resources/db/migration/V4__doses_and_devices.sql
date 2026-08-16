CREATE TABLE dose_occurrences (
    id UUID PRIMARY KEY,
    schedule_id UUID NOT NULL REFERENCES treatment_schedules(id),
    user_id UUID NOT NULL REFERENCES users(id),
    medication_id UUID NOT NULL REFERENCES medications(id),
    scheduled_at TIMESTAMP WITH TIME ZONE NOT NULL,
    original_scheduled_at TIMESTAMP WITH TIME ZONE NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('PENDING','TAKEN','SKIPPED')),
    snooze_count INTEGER NOT NULL DEFAULT 0,
    resolved_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    UNIQUE(schedule_id, original_scheduled_at)
);
CREATE INDEX idx_doses_user_time ON dose_occurrences(user_id, scheduled_at);
CREATE INDEX idx_doses_pending ON dose_occurrences(status, scheduled_at);

CREATE TABLE push_devices (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id),
    endpoint VARCHAR(2048) NOT NULL UNIQUE,
    p256dh VARCHAR(512) NOT NULL,
    auth VARCHAR(512) NOT NULL,
    user_agent VARCHAR(512),
    is_mobile BOOLEAN NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    last_seen_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX idx_devices_user ON push_devices(user_id);
