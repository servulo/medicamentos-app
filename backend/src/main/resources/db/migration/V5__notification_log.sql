CREATE TABLE notification_log (
    id UUID PRIMARY KEY,
    dose_id UUID NOT NULL REFERENCES dose_occurrences(id),
    device_id UUID NOT NULL REFERENCES push_devices(id),
    scheduled_at TIMESTAMP WITH TIME ZONE NOT NULL,
    sent_at TIMESTAMP WITH TIME ZONE NOT NULL,
    success BOOLEAN NOT NULL,
    error_detail VARCHAR(2000)
);
CREATE INDEX idx_notification_log_dose ON notification_log(dose_id);
