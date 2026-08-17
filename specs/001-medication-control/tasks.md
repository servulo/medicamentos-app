# Tasks: Controle de Medicamentos

**Input**: Design documents from `/specs/001-medication-control/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/, quickstart.md

**Tests**: Incluídos testes de API Quarkus (REST Assured) — obrigatórios pela
constitution (Princípio IV). Sem E2E de UI como gate.

**Organization**: Fases por user story (US5 → US1 → US2 → US3 → US4) após Setup
e Foundational.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: US1…US5 conforme spec.md
- Include exact file paths in descriptions

## Path Conventions

- Backend: `backend/src/main/java/app/medicamentos/`, testes em `backend/src/test/java/`
- Frontend: `frontend/src/app/`
- Deploy: `deploy/`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Inicializar monorepo Angular + Quarkus + Docker

- [x] T001 Create directories `backend/`, `frontend/`, `deploy/`, `deploy/nginx/` per plan.md
- [x] T002 Initialize Quarkus Java 21 Maven project in `backend/pom.xml` with REST, Hibernate ORM Panache, Flyway, OIDC, Scheduler, JDBC PostgreSQL
- [x] T003 [P] Initialize Angular standalone app in `frontend/` with routing, HttpClient, and PWA/service-worker support
- [x] T004 [P] Create `deploy/docker-compose.yml` with services `db` (PostgreSQL 16), `backend`, `frontend`
- [x] T005 [P] Create `deploy/.env.example` with `ADMIN_EMAIL`, `APP_TIMEZONE`, Google OIDC, DB, and VAPID placeholders
- [x] T006 [P] Add `backend/Dockerfile` and `frontend/Dockerfile` multi-stage builds
- [x] T007 [P] Add root `.gitignore` for Java/Node/env secrets

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Infra compartilhada — bloqueia todas as user stories

**⚠️ CRITICAL**: Nenhuma user story começa antes desta fase

- [x] T008 Configure `backend/src/main/resources/application.properties` for DB, OIDC Google, `admin.email`, `app.timezone`, CORS
- [x] T009 Create Flyway baseline `backend/src/main/resources/db/migration/V1__users_and_whitelist.sql` for `users` and `whitelist_entries`
- [x] T010 [P] Implement `User` Panache entity in `backend/src/main/java/app/medicamentos/auth/UserEntity.java`
- [x] T011 [P] Implement `WhitelistEntry` Panache entity in `backend/src/main/java/app/medicamentos/auth/WhitelistEntryEntity.java`
- [x] T012 Implement whitelist check + admin resolution filter/guard in `backend/src/main/java/app/medicamentos/auth/AccessControlFilter.java`
- [x] T013 Implement `GET /api/v1/me` in `backend/src/main/java/app/medicamentos/auth/MeResource.java` returning email, admin flag, timezone
- [x] T014 [P] Create Angular auth core (OIDC login/logout, interceptor, auth guard) in `frontend/src/app/core/auth/`
- [x] T015 [P] Create API base URL and `MeService` in `frontend/src/app/core/api/`
- [x] T016 Add global error/unauthorized handling shell in `frontend/src/app/core/errors/`
- [x] T017 Seed migration or startup hook ensuring `ADMIN_EMAIL` exists in whitelist in `backend/src/main/resources/db/migration/V2__seed_admin_whitelist.sql`
- [x] T018 Add API test support (`@QuarkusTest`, Testcontainers or Quarkus Dev Services) in `backend/src/test/java/app/medicamentos/support/ApiTestBase.java`

**Checkpoint**: Login Google + bloqueio fora da whitelist + `/me` funcionando

---

## Phase 3: User Story 5 - Acesso autorizado (Priority: P1)

**Goal**: Whitelist administrável só pelo e-mail `ADMIN_EMAIL`; não-admin sem acesso à gestão

**Independent Test**: Admin CRUD whitelist; usuário comum 403 em admin; fora da lista bloqueado

### Tests for User Story 5

- [x] T019 [P] [US5] API tests for whitelist list/add/remove and non-admin 403 in `backend/src/test/java/app/medicamentos/admin/WhitelistResourceTest.java`
- [x] T020 [P] [US5] API tests for access denied when email not whitelisted in `backend/src/test/java/app/medicamentos/auth/AccessControlTest.java`
- [x] T067 [P] [US5] API tests rejecting DELETE of `ADMIN_EMAIL` whitelist entry and allowing admin login even if missing from list in `backend/src/test/java/app/medicamentos/admin/AdminWhitelistProtectionTest.java`

### Implementation for User Story 5

- [x] T021 [US5] Implement whitelist service in `backend/src/main/java/app/medicamentos/admin/WhitelistService.java`
- [x] T022 [US5] Implement `GET/POST /api/v1/admin/whitelist` and `DELETE /api/v1/admin/whitelist/{entryId}` in `backend/src/main/java/app/medicamentos/admin/WhitelistResource.java` (block delete of `ADMIN_EMAIL`)
- [x] T023 [US5] Create admin whitelist feature UI in `frontend/src/app/features/admin/whitelist/`
- [x] T024 [US5] Add admin-only route guard in `frontend/src/app/core/auth/admin.guard.ts` and wire nav link only for admin
- [x] T025 [US5] Add blocked/unauthorized page for non-whitelisted users in `frontend/src/app/features/auth/blocked/`
- [x] T068 [US5] Ensure `AccessControlFilter` authorizes `ADMIN_EMAIL` even when not present in whitelist in `backend/src/main/java/app/medicamentos/auth/AccessControlFilter.java`

**Checkpoint**: US5 validável isoladamente (auth + whitelist admin)

---

## Phase 4: User Story 1 - Cadastrar medicamento e agenda (Priority: P1) — parte do MVP (com US5)

**Goal**: Catálogo reutilizável + agendas (dias/horários/duração indefinida ou N tomadas) com pausa/reativação (`PAUSED`/`COMPLETED`/`CANCELLED` → `ACTIVE`)

**Independent Test**: Criar medicamento, agenda todos os dias, agenda N doses → `COMPLETED`, nova agenda reutilizando catálogo, pausar/cancelar/reativar

### Tests for User Story 1

- [x] T026 [P] [US1] API tests for medications CRUD in `backend/src/test/java/app/medicamentos/medication/MedicationResourceTest.java`
- [x] T027 [P] [US1] API tests for schedules create/list/update/reactivate and FIXED_TAKEN_DOSES rules in `backend/src/test/java/app/medicamentos/schedule/ScheduleResourceTest.java`
- [x] T069 [P] [US1] API tests that user B cannot read/update user A medication or schedule (404/403) in `backend/src/test/java/app/medicamentos/auth/ResourceIsolationTest.java`

### Implementation for User Story 1

- [x] T028 [US1] Add Flyway `backend/src/main/resources/db/migration/V3__medications_and_schedules.sql`
- [x] T029 [P] [US1] Create `MedicationEntity` in `backend/src/main/java/app/medicamentos/medication/MedicationEntity.java`
- [x] T030 [P] [US1] Create `TreatmentScheduleEntity` in `backend/src/main/java/app/medicamentos/schedule/TreatmentScheduleEntity.java`
- [x] T031 [US1] Implement `MedicationService` in `backend/src/main/java/app/medicamentos/medication/MedicationService.java`
- [x] T032 [US1] Implement medications API `GET/POST /api/v1/medications` and `GET/PATCH /api/v1/medications/{id}` in `backend/src/main/java/app/medicamentos/medication/MedicationResource.java`
- [x] T033 [US1] Implement `ScheduleService` (ACTIVE/PAUSED/COMPLETED/CANCELLED, days, times, duration) in `backend/src/main/java/app/medicamentos/schedule/ScheduleService.java`
- [x] T034 [US1] Implement schedules API per OpenAPI in `backend/src/main/java/app/medicamentos/schedule/ScheduleResource.java`
- [x] T035 [P] [US1] Build medications UI (list/create/edit) in `frontend/src/app/features/medications/`
- [x] T036 [US1] Build schedules UI (create from catalog, days/times/duration, pause/reactivate) in `frontend/src/app/features/schedules/`
- [x] T037 [US1] Display `APP_TIMEZONE` from `/me` on schedule forms in `frontend/src/app/features/schedules/`

**Checkpoint**: US1 MVP de domínio testável via API + UI (com auth da US5)

---

## Phase 5: User Story 2 - Lembretes, tomada e adiamento (Priority: P1)

**Goal**: Materializar doses, push só em celulares registrados (todos), take/skip/snooze 10/30/60 ilimitado, auto-skip 2h

**Independent Test**: Registrar device mobile; no horário recebe push; take/skip/snooze; multi-device resolve uma vez

### Tests for User Story 2

- [x] T038 [P] [US2] API tests for dose take/skip/snooze and auto-skip window in `backend/src/test/java/app/medicamentos/dose/DoseResourceTest.java`
- [x] T039 [P] [US2] API tests for device register/list/delete and mobile-only push targeting in `backend/src/test/java/app/medicamentos/device/DeviceResourceTest.java`
- [x] T040 [US2] API/integration test that taken doses increment schedule `takenCount` and complete FIXED schedules in `backend/src/test/java/app/medicamentos/dose/DoseLimitTest.java`

### Implementation for User Story 2

- [x] T041 [US2] Add Flyway `backend/src/main/resources/db/migration/V4__doses_and_devices.sql`
- [x] T042 [P] [US2] Create `DoseOccurrenceEntity` in `backend/src/main/java/app/medicamentos/dose/DoseOccurrenceEntity.java`
- [x] T043 [P] [US2] Create `PushDeviceEntity` in `backend/src/main/java/app/medicamentos/device/PushDeviceEntity.java`
- [x] T044 [US2] Implement dose materialization + auto-skip scheduler in `backend/src/main/java/app/medicamentos/notify/DoseScheduler.java`
- [x] T045 [US2] Implement Web Push sender (VAPID) in `backend/src/main/java/app/medicamentos/notify/WebPushService.java`
- [x] T046 [US2] Implement `DoseService` (take/skip/snooze, stock side-effect hook point, idempotency) in `backend/src/main/java/app/medicamentos/dose/DoseService.java`
- [x] T047 [US2] Implement doses API `GET /api/v1/doses` and take/skip/snooze actions in `backend/src/main/java/app/medicamentos/dose/DoseResource.java`
- [x] T048 [US2] Implement devices API in `backend/src/main/java/app/medicamentos/device/DeviceResource.java`
- [x] T049 [P] [US2] Implement PWA push subscription sending `isMobile=true` on mobile and `false` on desktop in `frontend/src/app/features/doses/push-registration.service.ts` and `frontend/ngsw-config.json`
- [x] T050 [US2] Build pending dose actions UI (take/skip/snooze 10/30/60) in `frontend/src/app/features/doses/`
- [x] T051 [US2] Persist client `isMobile` as source of truth (no UA override) in `backend/src/main/java/app/medicamentos/device/DeviceResource.java`
- [x] T070 [US2] Persist `NotificationLog` rows on each push attempt (include `scheduled_at`, `sent_at`, `success`) in `backend/src/main/java/app/medicamentos/notify/WebPushService.java` and Flyway if needed in `backend/src/main/resources/db/migration/V5__notification_log.sql`

**Checkpoint**: US2 entregável com lembretes acionáveis

---

## Phase 6: User Story 3 - Estoque e aviso de compra (Priority: P2)

**Goal**: Estoque editável, decremento na tomada, alerta in-app (sem push)

**Independent Test**: Ajustar estoque abaixo do limiar → aparece em alertas Mobile/Web; tomada reduz saldo; sem push de compra

### Tests for User Story 3

- [x] T052 [P] [US3] API tests for purchase alerts and stock decrement on take in `backend/src/test/java/app/medicamentos/medication/StockAndAlertsTest.java`

### Implementation for User Story 3

- [x] T053 [US3] Enforce stock decrement on take and `purchaseNeeded` derivation in `backend/src/main/java/app/medicamentos/medication/MedicationService.java`
- [x] T054 [US3] Implement `GET /api/v1/alerts/purchase` in `backend/src/main/java/app/medicamentos/medication/PurchaseAlertResource.java`
- [x] T055 [P] [US3] Build in-app purchase alerts UI in `frontend/src/app/features/alerts/`
- [x] T056 [US3] Add stock/threshold editors on medication form in `frontend/src/app/features/medications/`

**Checkpoint**: US3 validável sem depender de push

---

## Phase 7: User Story 4 - Acompanhar doses tomadas e puladas (Priority: P2)

**Goal**: Histórico/resumo de TAKEN e SKIPPED com filtro por medicamento

**Independent Test**: Com doses existentes, abrir acompanhamento e filtrar por medicamento

### Tests for User Story 4

- [x] T057 [P] [US4] API tests for dose history filters (status, medication, date range) in `backend/src/test/java/app/medicamentos/dose/DoseHistoryTest.java`

### Implementation for User Story 4

- [x] T058 [US4] Verify `GET /api/v1/doses` query params (`medicationId`, `status`, `from`, `to`) match `specs/001-medication-control/contracts/openapi.yaml` in `backend/src/main/java/app/medicamentos/dose/DoseResource.java`
- [x] T059 [US4] Build adherence/history feature UI in `frontend/src/app/features/adherence/`
- [x] T060 [US4] Wire navigation entry to adherence view in `frontend/src/app/app.routes.ts`

**Checkpoint**: US4 independente com dados de doses já existentes

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: Deploy, hardening e validação quickstart

- [x] T061 [P] Configure nginx TLS reverse proxy sample in `deploy/nginx/default.conf`
- [x] T062 [P] Document runbook notes (iOS PWA push caveat, env vars) in `deploy/README.md`
- [x] T063 Align OpenAPI-implemented paths with `specs/001-medication-control/contracts/openapi.yaml` and fix drifts
- [x] T064 Run full backend API test suite via `backend` Maven test and fix failures
- [x] T065 Execute manual validation checklist from `specs/001-medication-control/quickstart.md` against Compose stack
- [x] T071 Validate SC-002: after ≥10 successful controlled dose pushes, confirm ≥95% of `NotificationLog` success rows have `sent_at - scheduled_at` ≤ 60s (script or documented SQL in `specs/001-medication-control/quickstart.md`)
- [x] T066 [P] Ensure secrets are not committed; verify `.gitignore` and `.env.example` only

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 Setup**: imediato
- **Phase 2 Foundational**: após Setup — **bloqueia** todas as stories
- **Phase 3 US5**: após Foundational (auth completa)
- **Phase 4 US1**: após US5 (precisa usuário autenticado/autorizado)
- **Phase 5 US2**: após US1 (precisa agendas)
- **Phase 6 US3**: após US2 take (decremento) — pode começar UI de estoque em paralelo após US1, mas alerta completo após take
- **Phase 7 US4**: após US2 (precisa doses TAKEN/SKIPPED)
- **Phase 8 Polish**: após stories desejadas

### User Story Dependencies

- **US5 (P1)**: Foundational only
- **US1 (P1)**: Depends on US5 for real users; MVP de domínio
- **US2 (P1)**: Depends on US1 schedules
- **US3 (P2)**: Depends on US1 medications; take integration from US2
- **US4 (P2)**: Depends on US2 dose history

### Within Each User Story

- API tests before or alongside implementation (fail first when practical)
- Migrations → entities → services → resources → frontend
- Story checkpoint before next priority when sequential

### Parallel Opportunities

- T003–T007 parallel in Setup
- T010–T011, T014–T015 parallel in Foundational
- T019–T020 parallel US5 tests
- T026–T027, T029–T030, T035 parallel within US1
- T038–T039, T042–T043, T049 parallel within US2
- T055 parallel with T053–T054 in US3
- After Foundational, frontend shell work can proceed while backend story APIs land

---

## Parallel Example: User Story 1

```bash
# Tests in parallel:
Task: "API tests medications in backend/.../MedicationResourceTest.java"
Task: "API tests schedules in backend/.../ScheduleResourceTest.java"

# Entities in parallel:
Task: "MedicationEntity in backend/.../MedicationEntity.java"
Task: "TreatmentScheduleEntity in backend/.../TreatmentScheduleEntity.java"

# After services/APIs:
Task: "Medications UI in frontend/src/app/features/medications/"
Task: "Schedules UI in frontend/src/app/features/schedules/"
```

---

## Parallel Example: User Story 2

```bash
Task: "DoseOccurrenceEntity in backend/.../DoseOccurrenceEntity.java"
Task: "PushDeviceEntity in backend/.../PushDeviceEntity.java"
Task: "Push registration service in frontend/.../push-registration.service.ts"
```

---

## Implementation Strategy

### MVP First

1. Phase 1 Setup
2. Phase 2 Foundational
3. Phase 3 US5 (acesso seguro)
4. Phase 4 US1 (catálogo + agendas)
5. **STOP and VALIDATE** — MVP = **US5 + US1** via API tests + quickstart Auth + Catálogo/Agenda
6. Demo MVP

### Incremental Delivery

1. US5 → login seguro
2. US1 → valor de cadastro
3. US2 → lembretes (produto completo P1)
4. US3 → estoque
5. US4 → adesão
6. Polish → produção Ubuntu/Docker

### Parallel Team Strategy

1. Juntos: Setup + Foundational
2. Dev A: US5 → US1
3. Dev B (após US1 API): US2 devices/push
4. Dev C (após US2 doses): US3 + US4 UI

---

## Notes

- [P] = arquivos diferentes, sem dependência de tarefa incompleta
- Labels [US1]…[US5] mapeiam às stories do spec.md
- Contagem N = apenas doses TAKEN (FR-005) → status `COMPLETED`
- Push só `isMobile=true` (flag do cliente); aviso compra só in-app
- Remediações analyze: T067–T071 (admin whitelist, isolamento FR-014, NotificationLog/SC-002)
- Sem E2E UI obrigatório; API tests são gate
- Commit após cada tarefa ou grupo lógico
- Total atual: **71** tarefas (T001–T071)
