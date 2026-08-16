-- Purge previously soft-deleted medications and drop deleted_at

DELETE FROM notification_log
WHERE dose_id IN (
    SELECT d.id FROM dose_occurrences d
    INNER JOIN medications m ON m.id = d.medication_id
    WHERE m.deleted_at IS NOT NULL
);

DELETE FROM dose_occurrences
WHERE medication_id IN (SELECT id FROM medications WHERE deleted_at IS NOT NULL);

DELETE FROM treatment_schedules
WHERE medication_id IN (SELECT id FROM medications WHERE deleted_at IS NOT NULL);

DELETE FROM medications WHERE deleted_at IS NOT NULL;

ALTER TABLE medications DROP COLUMN deleted_at;
