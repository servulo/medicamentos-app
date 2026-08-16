# Data Model: Exclusão de Medicamento, Dose na Agenda e Alerta por Unidade

**Feature**: `003-med-delete-dose-units` | **Date**: 2026-08-15  
**Base**: estende `specs/001-medication-control/data-model.md`

## Entity changes

### Medication (catálogo) — atualizado

| Field | Type | Rules |
|-------|------|-------|
| id | UUID | PK |
| user_id | UUID | FK User |
| name | string | 1–120 chars |
| unit | string | default `"unidade"` |
| stock_quantity | int | ≥ 0, default 0 |
| purchase_threshold_units | int | ≥ 0, default **10** |
| deleted_at | timestamptz | null = ativo; non-null = soft-deleted |
| created_at / updated_at | timestamptz | |

**Removed**: `quantity_per_dose`, `purchase_threshold_doses`.

**Rules**:
- Escopo por `user_id`.
- Listagens ativas: `deleted_at IS NULL`.
- Soft-delete: set `deleted_at = now()`; **sem restore**.
- Update/estoque só se ativo (`deleted_at IS NULL`).
- **Purchase alert (derivado)**:
  `stock_quantity <= purchase_threshold_units`.

**Delete cascade (transacional)**:
1. `deleted_at = now()`
2. Agendas `ACTIVE` \| `PAUSED` → `CANCELLED`
3. **Todas** as doses `PENDING` do medicamento (qualquer `schedule_id`) →
   `SKIPPED` (`resolved_at = now`)
4. Sem alteração de `stock_quantity` no auto-skip

### TreatmentSchedule (agenda) — atualizado

| Field | Type | Rules |
|-------|------|-------|
| … | … | (campos 001 inalterados) |
| quantity_per_dose | int | ≥ 1, default 1, NOT NULL |

**Rules**:
- Create só se medicamento do usuário com `deleted_at IS NULL`.
- `quantity_per_dose` obrigatório na criação (default 1 se omitido na API).
- Editável via update; não recalcula estoque de doses já `TAKEN`.

### DoseOccurrence — inalterado em schema

Comportamento: `take` decrementa `Medication.stock_quantity` em
`TreatmentSchedule.quantity_per_dose` da agenda da dose (mínimo estoque 0).

### PurchaseAlert (visão/DTO)

| Field | Type | Notes |
|-------|------|-------|
| medicationId | UUID | |
| name | string | |
| stockQuantity | int | unidades |
| purchaseThresholdUnits | int | |
| purchaseNeeded | boolean | sempre true neste endpoint filtrado |
| unit | string | opcional UX |

**Removed from alert DTO**: `quantityPerDose`, `purchaseThresholdDoses`,
`remainingDoses`.

## Migration (Flyway)

Ordem sugerida em `V6__med_delete_dose_units.sql`:

1. `ALTER TABLE treatment_schedules ADD quantity_per_dose INT`
2. `UPDATE treatment_schedules s SET quantity_per_dose = GREATEST(1, ROUND(m.quantity_per_dose)) FROM medications m WHERE s.medication_id = m.id`
3. `UPDATE treatment_schedules SET quantity_per_dose = 1 WHERE quantity_per_dose IS NULL`
4. `ALTER … quantity_per_dose SET NOT NULL DEFAULT 1`
5. `ALTER medications ADD deleted_at TIMESTAMPTZ NULL`
6. Round stock: `UPDATE medications SET stock_quantity = GREATEST(0, ROUND(stock_quantity))` then cast column to INT
7. Add `purchase_threshold_units INT NOT NULL DEFAULT 10`; set **all rows to 10**; drop `purchase_threshold_doses`
8. Drop `medications.quantity_per_dose`

## State / lifecycle notes

```text
Medication: ACTIVE (deleted_at null) → DELETED (deleted_at set)  [terminal for UX]
Schedule on med delete: ACTIVE|PAUSED → CANCELLED
Dose on med delete: PENDING → SKIPPED (no stock change)
```

## Validation summary

- Inteiros apenas: stock ≥ 0, threshold ≥ 0, quantity_per_dose ≥ 1
- Reject fractional/zero/negative as applicable (400)
- Cannot create schedule for deleted medication (404/400)
- Cannot update deleted medication (404)
- Delete only own medication (404/403)
