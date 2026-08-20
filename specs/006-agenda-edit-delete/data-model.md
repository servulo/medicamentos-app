# Data Model: Edição e Exclusão de Agendas

**Feature**: `006-agenda-edit-delete` | **Date**: 2026-08-19  
**Base**: estende `specs/003-med-delete-dose-units/data-model.md` e
`specs/004-med-delete-cascade/data-model.md`

## Schema changes

Nenhuma alteração de tabela ou coluna. Última migração aplicável: `V7`.

## Entity rules (delta)

### TreatmentSchedule (agenda)

Campos editáveis via **edição completa** (formulário):

| Field | Editável | Rules |
|-------|----------|-------|
| medication_id | Não | Fixo após criação (FR-004) |
| days_of_week | Sim | ISO 1–7, não vazio |
| times_of_day | Sim | ≥1, HH:mm |
| duration_type | Sim | `INDEFINITE` \| `FIXED_TAKEN_DOSES` |
| max_taken_doses | Sim | Obrigatório e &gt;0 se FIXED; null se INDEFINITE |
| quantity_per_dose | Sim | Inteiro ≥ 1 |
| taken_count | Não (direto) | Preservado na mudança de duração |
| status | Via regras | Ver transições abaixo |

**Edição completa** (PATCH com campos estruturais):

1. Validar `maxTakenDoses >= takenCount` quando FIXED; senão 400.
2. Se status ∈ {`PAUSED`, `CANCELLED`} → `ACTIVE` (FR-008b).
3. Recalcular status (FR-008a):
   - FIXED e `takenCount >= maxTakenDoses` → `COMPLETED`
   - FIXED e `takenCount < maxTakenDoses` → `ACTIVE` (inclui ex-`COMPLETED`)
   - INDEFINITE → nunca `COMPLETED` por limite; `maxTakenDoses` = null
4. Se `daysOfWeek` ou `timesOfDay` mudaram → purgar doses `PENDING` da agenda.

**PATCH status-only** (listagem — pausar/reativar):

- Comportamento atual preservado; **não** aplica FR-008b automaticamente.
- Reativar FIXED com `resetTakenCount: true` (default) zera contador.

### DoseOccurrence

**Na edição com mudança de recorrência**:

- Apagar linhas `PENDING` da agenda (+ logs de notificação).
- Manter `TAKEN` e `SKIPPED`.

**Na exclusão da agenda**:

- Apagar **todas** as linhas da agenda (`PENDING`, `TAKEN`, `SKIPPED`).
- Medicamento permanece; estoque não é ajustado.

### NotificationLog

Apagado antes das doses, tanto na purga parcial (só PENDING) quanto na
exclusão total da agenda (mesma ordem que 004).

### Medication

Inalterado por exclusão de agenda. Inalterado por edição (salvo consumo
futuro via `quantityPerDose` no `take`).

## Delete cascade (agenda — transacional)

```text
1. DELETE notification_log WHERE dose_id IN (SELECT id FROM dose_occurrences WHERE schedule_id = ? AND user_id = ?)
2. DELETE dose_occurrences WHERE schedule_id = ? AND user_id = ?
3. DELETE treatment_schedules WHERE id = ? AND user_id = ?
```

- Outras agendas do mesmo `medication_id`: intocadas.
- Outro usuário: 404 antes de qualquer delete.

## Purga parcial (recorrência alterada)

```text
1. SELECT dose ids WHERE schedule_id = ? AND user_id = ? AND status = 'PENDING'
2. DELETE notification_log WHERE dose_id IN (...)
3. DELETE dose_occurrences WHERE id IN (...) AND status = 'PENDING'
```

## State transitions (edição completa)

```text
PAUSED   + save edit → ACTIVE → (recalc) → ACTIVE | COMPLETED
CANCELLED+ save edit → ACTIVE → (recalc) → ACTIVE | COMPLETED
COMPLETED+ save edit (max increased) → ACTIVE
ACTIVE   + save edit (max reduced valid) → ACTIVE | COMPLETED
```

## Validation summary

| Rule | HTTP |
|------|------|
| Agenda de outro usuário | 404 |
| `maxTakenDoses < takenCount` | 400 |
| Dias/horários vazios | 400 |
| `quantityPerDose < 1` | 400 |
| DELETE agenda inexistente | 404 |
| Segundo DELETE | 404 |
| Troca de medicamento no PATCH | Ignorar/rejeitar `medicationId` se enviado |

## API surface (novo/alterado)

| Method | Path | Semântica |
|--------|------|-----------|
| PATCH | `/schedules/{id}` | Status-only OU edição completa (ver research §1) |
| DELETE | `/schedules/{id}` | Hard-delete agenda + histórico (204) |

Ver [contracts/openapi.yaml](./contracts/openapi.yaml).
