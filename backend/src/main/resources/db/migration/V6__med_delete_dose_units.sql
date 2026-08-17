-- Move quantity_per_dose to schedules; soft-delete meds; stock/threshold as unit integers
-- Written to work on PostgreSQL (prod) and H2 (Quarkus tests)

ALTER TABLE treatment_schedules ADD COLUMN quantity_per_dose INTEGER;

UPDATE treatment_schedules
SET quantity_per_dose = COALESCE((
    SELECT CAST(CASE WHEN m.quantity_per_dose < 1 THEN 1 ELSE ROUND(m.quantity_per_dose) END AS INT)
    FROM medications m
    WHERE m.id = treatment_schedules.medication_id
), 1);

UPDATE treatment_schedules SET quantity_per_dose = 1 WHERE quantity_per_dose IS NULL;

ALTER TABLE treatment_schedules ALTER COLUMN quantity_per_dose SET DEFAULT 1;
ALTER TABLE treatment_schedules ALTER COLUMN quantity_per_dose SET NOT NULL;

ALTER TABLE treatment_schedules
    ADD CONSTRAINT treatment_schedules_quantity_per_dose_check CHECK (quantity_per_dose >= 1);

ALTER TABLE medications ADD COLUMN deleted_at TIMESTAMP WITH TIME ZONE NULL;

ALTER TABLE medications ADD COLUMN stock_quantity_int INTEGER;

UPDATE medications
SET stock_quantity_int = CAST(CASE WHEN stock_quantity < 0 THEN 0 ELSE ROUND(stock_quantity) END AS INT);

ALTER TABLE medications DROP COLUMN stock_quantity;

ALTER TABLE medications RENAME COLUMN stock_quantity_int TO stock_quantity;

ALTER TABLE medications ALTER COLUMN stock_quantity SET DEFAULT 0;
ALTER TABLE medications ALTER COLUMN stock_quantity SET NOT NULL;

ALTER TABLE medications
    ADD CONSTRAINT medications_stock_quantity_check CHECK (stock_quantity >= 0);

ALTER TABLE medications ADD COLUMN purchase_threshold_units INTEGER DEFAULT 10;

UPDATE medications SET purchase_threshold_units = 10;

ALTER TABLE medications ALTER COLUMN purchase_threshold_units SET NOT NULL;

ALTER TABLE medications DROP COLUMN purchase_threshold_doses;

ALTER TABLE medications
    ADD CONSTRAINT medications_purchase_threshold_units_check CHECK (purchase_threshold_units >= 0);

ALTER TABLE medications DROP COLUMN quantity_per_dose;
