CREATE TABLE medications (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id),
    name VARCHAR(120) NOT NULL,
    unit VARCHAR(60) NOT NULL DEFAULT 'unidade',
    quantity_per_dose DECIMAL(14,3) NOT NULL DEFAULT 1 CHECK (quantity_per_dose > 0),
    stock_quantity DECIMAL(14,3) NOT NULL DEFAULT 0 CHECK (stock_quantity >= 0),
    purchase_threshold_doses INTEGER NOT NULL DEFAULT 7 CHECK (purchase_threshold_doses >= 0),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX idx_medications_user ON medications(user_id);

CREATE TABLE treatment_schedules (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id),
    medication_id UUID NOT NULL REFERENCES medications(id),
    status VARCHAR(20) NOT NULL CHECK (status IN ('ACTIVE','PAUSED','COMPLETED','CANCELLED')),
    days_of_week VARCHAR(20) NOT NULL,
    times_of_day VARCHAR(100) NOT NULL,
    duration_type VARCHAR(30) NOT NULL CHECK (duration_type IN ('INDEFINITE','FIXED_TAKEN_DOSES')),
    max_taken_doses INTEGER,
    taken_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CHECK ((duration_type = 'INDEFINITE' AND max_taken_doses IS NULL)
        OR (duration_type = 'FIXED_TAKEN_DOSES' AND max_taken_doses > 0))
);
CREATE INDEX idx_schedules_user ON treatment_schedules(user_id);
CREATE INDEX idx_schedules_active ON treatment_schedules(status);
