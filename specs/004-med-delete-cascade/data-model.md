# Data Model: Exclusão Completa de Medicamento, Agenda e Histórico

**Feature**: `004-med-delete-cascade` | **Date**: 2026-08-16  
**Base**: estende `specs/003-med-delete-dose-units/data-model.md` (e 001)

## Entity changes

### Medication (catálogo) — atualizado

| Field | Type | Rules |
|-------|------|-------|
| id | UUID | PK |
| user_id | UUID | FK User |
| name | string | 1–120 chars |
| unit | string | default `"unidade"` |
| stock_quantity | int | ≥ 0, default 0 |
| purchase_threshold_units | int | ≥ 0, default 10 |
| created_at / updated_at | timestamptz | |

**Removed**: `deleted_at` (soft-delete da feature 003).

**Rules**:
- Escopo por `user_id`.
- Listagens: todos os medicamentos do usuário (não há estado “excluído”).
- Update/estoque só se a linha existir e for do dono (404 caso contrário).
- **Purchase alert (derivado, inalterado)**:
  `stock_quantity <= purchase_threshold_units` sobre linhas existentes.
- Sem restore.

### TreatmentSchedule (agenda) — sem mudança de schema

Continua com `quantity_per_dose` e status `ACTIVE`/`PAUSED`/`COMPLETED`/`CANCELLED`.

**Rules novas na exclusão do medicamento**:
- Todas as agendas daquele `medication_id` são **apagadas**, qualquer status.
- `GET /schedules/{id}` após exclusão do medicamento → 404.
- Create de agenda continua exigindo medicamento existente do dono (404 se
  o medicamento já foi apagado).

### DoseOccurrence — sem mudança de schema

**Rules novas na exclusão do medicamento**:
- Todas as doses daquele `medication_id` são **apagadas** (`PENDING`,
  `TAKEN`, `SKIPPED`).
- Listagens de acompanhamento não retornam ocorrências do item apagado.

### NotificationLog — sem mudança de schema

Registros cujo `dose_id` pertence a doses do medicamento excluído são
apagados **antes** das doses (FK `notification_log.dose_id`).

### PurchaseAlert (visão/DTO)

Inalterado. Deixa de incluir o medicamento porque a linha do catálogo
deixou de existir.

## Delete cascade (transacional)

Ordem obrigatória (FKs RESTRICT atuais):

```text
1. DELETE notification_log WHERE dose_id IN (doses do medication_id do user)
2. DELETE dose_occurrences WHERE medication_id AND user_id
3. DELETE treatment_schedules WHERE medication_id AND user_id
4. DELETE medications WHERE id AND user_id
```

- Sem alteração de estoque (o registro some).
- Sem marcar doses como `SKIPPED` e sem cancelar agendas.
- Outros `medication_id` do mesmo usuário: intocados.
- Outro usuário: nenhuma linha apagada (404 antes da cascata).

## Migration (Flyway)

`V7__med_hard_delete_cascade.sql`:

1. Purge de medicamentos já soft-deleted (`deleted_at IS NOT NULL`) na mesma
   ordem da cascata acima (logs → doses → agendas → medicamentos).
2. `ALTER TABLE medications DROP COLUMN deleted_at`.

Não alterar FKs para `ON DELETE CASCADE` nesta feature.

## State / lifecycle notes

```text
Medication: EXISTS → (DELETE confirmado) → GONE   [terminal; sem restore]
Schedule:   qualquer status → linha removida com o medicamento
Dose:       PENDING|TAKEN|SKIPPED → linha removida com o medicamento
Purchase alert: some porque o medicamento some
```

Substitui o ciclo 003:

```text
(removido) Medication ACTIVE → DELETED (deleted_at)
(removido) Schedule ACTIVE|PAUSED → CANCELLED
(removido) Dose PENDING → SKIPPED
```

## Validation summary

- DELETE só do próprio medicamento (404 se outro usuário ou id inexistente)
- Segundo DELETE do mesmo id → 404
- POST agenda com `medicationId` apagado → 404
- Cancelar confirmação na UI: nenhum DELETE enviado
- Isolamento: dois medicamentos → apagar um não altera o outro
