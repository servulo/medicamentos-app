# Data Model: Controle de Medicamentos

**Feature**: `001-medication-control` | **Date**: 2026-08-15

## Entities

### User

Representação local pós-login Google (opcional cache de perfil).

| Field | Type | Rules |
|-------|------|-------|
| id | UUID | PK |
| google_sub | string | único, obrigatório |
| email | string | único, obrigatório, lower-case |
| display_name | string | opcional |
| created_at | timestamptz | obrigatório |
| last_login_at | timestamptz | opcional |

** derivada**: `isAdmin` = `email == ADMIN_EMAIL` (não persistir flag admin).

### WhitelistEntry

| Field | Type | Rules |
|-------|------|-------|
| id | UUID | PK |
| email | string | único, obrigatório, lower-case |
| created_at | timestamptz | obrigatório |
| created_by_email | string | auditoria |

**Rules**: Só admin CRUD. Login autorizado se e-mail ∈ whitelist **ou**
`email == ADMIN_EMAIL`. Bootstrap via seed com `ADMIN_EMAIL` na whitelist.
**MUST NOT** permitir DELETE do registro cujo e-mail é `ADMIN_EMAIL`.

### Medication (catálogo)

| Field | Type | Rules |
|-------|------|-------|
| id | UUID | PK |
| user_id | UUID | FK User, obrigatório |
| name | string | obrigatório, 1–120 chars |
| unit | string | default `"unidade"` |
| quantity_per_dose | decimal | > 0, default 1 |
| stock_quantity | decimal | ≥ 0, default 0 |
| purchase_threshold_doses | int | ≥ 0, default 7 |
| created_at / updated_at | timestamptz | |

**Rules**: Escopo por `user_id`. Soft-delete opcional; preferir manter para
histórico.

**Purchase alert (derivado)**:
`stock_quantity / quantity_per_dose <= purchase_threshold_doses`.

### TreatmentSchedule (agenda)

| Field | Type | Rules |
|-------|------|-------|
| id | UUID | PK |
| user_id | UUID | FK User |
| medication_id | UUID | FK Medication |
| status | enum | `ACTIVE`, `PAUSED`, `COMPLETED`, `CANCELLED` |
| days_of_week | set/array int | 1–7 (ISO), não vazio; todos os dias = {1..7} |
| times_of_day | array time | ≥1, fuso APP_TIMEZONE |
| duration_type | enum | `INDEFINITE`, `FIXED_TAKEN_DOSES` |
| max_taken_doses | int nullable | obrigatório se FIXED; > 0 |
| taken_count | int | ≥ 0, só incrementa em TAKEN |
| created_at / updated_at | timestamptz | |

**State transitions**:
- `ACTIVE` → `PAUSED` (usuário)
- `PAUSED` → `ACTIVE` (reativar)
- `ACTIVE` → `COMPLETED` (taken_count >= max_taken_doses)
- `ACTIVE`/`PAUSED` → `CANCELLED` (encerrar manual)
- `COMPLETED`/`CANCELLED` → `ACTIVE` (reativar; reset ou continuar contagem —
  **regra**: ao reativar FIXED, usuário pode informar novo N ou continuar;
  default = novo ciclo com `taken_count = 0` e mesmo `max_taken_doses`)

### DoseOccurrence

| Field | Type | Rules |
|-------|------|-------|
| id | UUID | PK |
| schedule_id | UUID | FK TreatmentSchedule |
| user_id | UUID | FK User (denormalizado para queries) |
| medication_id | UUID | FK Medication |
| scheduled_at | timestamptz | horário efetivo (após adiamentos) |
| original_scheduled_at | timestamptz | primeira previsão |
| status | enum | `PENDING`, `TAKEN`, `SKIPPED` |
| snooze_count | int | ≥ 0 |
| resolved_at | timestamptz nullable | |
| created_at | timestamptz | |

**Transitions**:
- `PENDING` → `TAKEN` | `SKIPPED` | permanece `PENDING` com novo `scheduled_at`
  (adiar +10/30/60)
- Auto: `PENDING` → `SKIPPED` se agora > scheduled_at + 2h e sem ação

**Rules**: Ação em qualquer dispositivo atualiza a mesma ocorrência (idempotente
se já resolvida → 409 ou no-op com estado atual).

### PushDevice

| Field | Type | Rules |
|-------|------|-------|
| id | UUID | PK |
| user_id | UUID | FK User |
| endpoint | string | único global (Web Push endpoint) |
| p256dh | string | |
| auth | string | |
| user_agent | string | opcional |
| is_mobile | boolean | true para receber push de dose |
| created_at / last_seen_at | timestamptz | |

**Rules**: Envio de dose apenas se `is_mobile = true`. O cliente **MUST**
enviar `isMobile` no registro; o servidor persiste o valor sem inferir por UA
como fonte de verdade (UA fica só para auditoria). Desktop/web MUST enviar
`isMobile=false`.

### NotificationLog (auditoria de push — usado em SC-002)

| Field | Type | Rules |
|-------|------|-------|
| id | UUID | PK |
| dose_id | UUID | FK |
| device_id | UUID | FK |
| scheduled_at | timestamptz | cópia do horário alvo |
| sent_at | timestamptz | |
| success | boolean | |
| error_detail | string nullable | |

**Rules**: Cada tentativa de envio Web Push para device `is_mobile=true` MUST
gerar uma linha. SC-002 mede `sent_at - scheduled_at` nas linhas `success=true`
em amostra controlada.

## Relationships

```text
User 1──* Medication
User 1──* TreatmentSchedule
User 1──* PushDevice
User 1──* WhitelistEntry (admin gerencia; não é “dono” por user_id)
Medication 1──* TreatmentSchedule
TreatmentSchedule 1──* DoseOccurrence
```

## Validation summary

- Dias da semana e horários obrigatórios na agenda ativa
- Adiar só `PENDING` e só deltas 10|30|60
- Estoque não negativo; tomada reduz `stock_quantity` em `quantity_per_dose`
- Whitelist e-mail único case-insensitive
