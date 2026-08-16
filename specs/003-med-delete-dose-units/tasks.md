# Tasks: Exclusão de Medicamento, Dose na Agenda e Alerta por Unidade

**Input**: Design documents from `/specs/003-med-delete-dose-units/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/, quickstart.md

**Tests**: Incluídos testes de API Quarkus (REST Assured) — obrigatórios pela
constitution (Princípio IV) e pelo plan.md. Sem E2E de UI como gate.

**Organization**: Setup → Foundational (migração V6 compartilhada) → US1 → US2 → US3 → Polish

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies on incomplete work)
- **[Story]**: US1 / US2 / US3 conforme spec.md
- Include exact file paths in descriptions

## Path Conventions

- Backend: `backend/src/main/java/app/medicamentos/`, testes em `backend/src/test/java/`
- Frontend: `frontend/src/app/`
- Specs: `specs/003-med-delete-dose-units/`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Alinhar contrato desta feature e ponto de mudança no monorepo existente

- [x] T001 Merge Medication/Schedule/PurchaseAlert schema changes from `specs/003-med-delete-dose-units/contracts/openapi.yaml` into `specs/001-medication-control/contracts/openapi.yaml` (001 remains the canonical OpenAPI; 003 delta stays as change log only)
- [x] T002 [P] Confirm next Flyway version is `V6` (existing `V5__notification_log.sql`) and list touchpoints in `backend/src/main/java/app/medicamentos/medication/`, `schedule/`, `dose/` plus `frontend/src/app/core/api/api.ts`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Schema e entidades compartilhadas — bloqueia US1/US2/US3

**⚠️ CRITICAL**: Nenhuma user story começa antes desta fase

- [x] T003 Create Flyway migration `backend/src/main/resources/db/migration/V6__med_delete_dose_units.sql`: add `treatment_schedules.quantity_per_dose`; copy/round from `medications.quantity_per_dose`; add `medications.deleted_at`; cast/round `stock_quantity` to INT; add `purchase_threshold_units` default/reset all to 10; drop `purchase_threshold_doses` and `medications.quantity_per_dose`
- [x] T004 Update `MedicationEntity` in `backend/src/main/java/app/medicamentos/medication/MedicationEntity.java`: remove `quantityPerDose`; `stockQuantity` as int; rename to `purchaseThresholdUnits` (default 10); add `deletedAt`; change `purchaseNeeded()` to `stockQuantity <= purchaseThresholdUnits`
- [x] T005 Update `TreatmentScheduleEntity` in `backend/src/main/java/app/medicamentos/schedule/TreatmentScheduleEntity.java` to add `quantityPerDose` (int ≥ 1, default 1)
- [x] T006 Update Medication DTOs/views in `backend/src/main/java/app/medicamentos/medication/MedicationResource.java` and create/update logic in `MedicationService.java` for integer stock/threshold, no `quantityPerDose`, default threshold 10, filter list/get/update to `deletedAt == null`
- [x] T007 Update Schedule create/update DTOs and mapping in `backend/src/main/java/app/medicamentos/schedule/ScheduleResource.java` and `ScheduleService.java` to require/persist `quantityPerDose` (default 1) and reject create when medication is soft-deleted
- [x] T008 Update Angular API types and client methods in `frontend/src/app/core/api/api.ts` for Medication/Schedule/PurchaseAlert field renames and prepare `deleteMedication(id)` stub (implement body in US1)

**Checkpoint**: App compila com schema novo; listagens de medicamento ativo; agendas com `quantityPerDose`; create de med deleted bloqueado — exclusão/alerta/take ainda incompletos até stories

---

## Phase 3: User Story 1 - Excluir medicamento do catálogo (Priority: P1) 🎯 MVP

**Goal**: Soft-delete com confirmação; cancelar agendas ACTIVE/PAUSED; skip **todas** PENDING do medicamento; histórico preservado; sem restore

**Independent Test**: DELETE medicamento → some da lista; agendas canceladas; todas PENDING→SKIPPED sem baixar estoque; histórico TAKEN/SKIPPED consultável; segundo DELETE → 404

### Tests for User Story 1

- [x] T009 [P] [US1] API tests for soft-delete cascade (cancel ACTIVE/PAUSED; skip all PENDING for that medication across any schedule; no stock change; list excludes deleted; history readable) in `backend/src/test/java/app/medicamentos/medication/MedicationDeleteTest.java`
- [x] T010 [P] [US1] API tests that delete of another user’s medication returns 404/403 and deleted med cannot create schedule in `backend/src/test/java/app/medicamentos/medication/MedicationDeleteIsolationTest.java`

### Implementation for User Story 1

- [x] T011 [US1] Implement transactional soft-delete in `backend/src/main/java/app/medicamentos/medication/MedicationService.java` (set `deletedAt`; ACTIVE/PAUSED→CANCELLED; **all** PENDING doses for that `medicationId`→SKIPPED with `resolvedAt`; no stock decrement)
- [x] T012 [US1] Add `DELETE /api/v1/medications/{medicationId}` returning 204 in `backend/src/main/java/app/medicamentos/medication/MedicationResource.java`
- [x] T013 [US1] Wire `deleteMedication` in `frontend/src/app/core/api/api.ts` and add confirm + delete action on medication list/detail in `frontend/src/app/features/medications/medications.page.ts`
- [x] T014 [US1] Keep adherence history readable for deleted meds in `frontend/src/app/features/adherence/adherence.page.ts`: doses of soft-deleted medications must still list with a display name (e.g. include deleted meds in name lookup, or expose `medicationName` on dose API) — `doses.page.ts` has no catalog filter, no change required there unless name resolution breaks

**Checkpoint**: US1 validável via API + UI confirmação; MVP de exclusão pronto

---

## Phase 4: User Story 2 - Quantidade por dose na Agenda (Priority: P1)

**Goal**: Quantidade por dose só na agenda (criar **e** editar); take consome estoque pela quantidade da agenda; edição afeta só tomadas futuras

**Independent Test**: Duas agendas qty 1 e 2; takes reduzem 1 depois 2; editar qty de agenda existente altera próxima take; formulário de med sem quantity; rejeita 0/fração

### Tests for User Story 2

- [x] T015 [P] [US2] API tests for schedule `quantityPerDose` create/update validation (≥1 integer; reject 0/fraction) in `backend/src/test/java/app/medicamentos/schedule/ScheduleQuantityPerDoseTest.java`
- [x] T016 [P] [US2] API tests that take decrements stock by schedule `quantityPerDose` (two schedules, amounts 1 and 2) and that PATCH updating `quantityPerDose` applies only to subsequent takes in `backend/src/test/java/app/medicamentos/dose/DoseStockFromScheduleTest.java`

### Implementation for User Story 2

- [x] T017 [US2] Change `decrementStock` / take path so `DoseService.take` uses schedule `quantityPerDose` in `backend/src/main/java/app/medicamentos/dose/DoseService.java` and `MedicationService.java` (amount parameter, integer)
- [x] T018 [US2] Remove quantity-per-dose controls from medication create/edit UI in `frontend/src/app/features/medications/medications.page.ts`
- [x] T019 [US2] Add `quantityPerDose` (integer ≥1, default 1) to Nova Agenda create form in `frontend/src/app/features/schedules/schedules.page.ts` (`ScheduleFormPage`) and send via `ApiService.createSchedule`
- [x] T020 [US2] Allow editing `quantityPerDose` on an existing schedule from `SchedulesPage` in `frontend/src/app/features/schedules/schedules.page.ts` via `ApiService.updateSchedule` (inline control or edit affordance; validate ≥1 integer; show current value including after create)

**Checkpoint**: US2 testável — create + edit de quantidade; consumo de estoque por agenda

---

## Phase 5: User Story 3 - Alerta de compra por unidade (Priority: P1)

**Goal**: Alerta quando `stockQuantity <= purchaseThresholdUnits`; default/migração 10; sem conversão por dose; só in-app

**Independent Test**: Estoque 15 limiar 10 → sem alerta; estoque 10 → alerta in-app; payload sem `remainingDoses`/`quantityPerDose`

### Tests for User Story 3

- [x] T021 [P] [US3] Update/extend API tests for unit-based purchase alerts and integer stock/threshold validation in `backend/src/test/java/app/medicamentos/medication/StockAndAlertsTest.java`
- [x] T022 [P] [US3] API tests rejecting fractional/null-invalid stock/threshold on create/update while allowing `0` in `backend/src/test/java/app/medicamentos/medication/MedicationIntegerFieldsTest.java`

### Implementation for User Story 3

- [x] T023 [US3] Update `PurchaseAlertResource` DTO (`stockQuantity`, `purchaseThresholdUnits`, `unit`; drop `quantityPerDose`/`remainingDoses`/`purchaseThresholdDoses`) in `backend/src/main/java/app/medicamentos/medication/PurchaseAlertResource.java`
- [x] T024 [US3] Update alerts UI copy/bindings to `stockQuantity` and `purchaseThresholdUnits` (no `remainingUnits`) in `frontend/src/app/features/alerts/alerts.page.ts`
- [x] T025 [US3] Update medication form stock/threshold labels and integer inputs (default threshold 10; allow 0) in `frontend/src/app/features/medications/medications.page.ts`

**Checkpoint**: US3 validável — alerta só por unidades

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Consistência cross-story e validação do quickstart

- [x] T026 [P] Fix any remaining references to `quantityPerDose` on Medication or `purchaseThresholdDoses` in backend tests under `backend/src/test/java/app/medicamentos/`
- [x] T027 [P] Fix remaining frontend type/usages of old field names outside medications/schedules/alerts if any under `frontend/src/app/`
- [x] T028 Run scenarios US1–US3 and migration checks in `specs/003-med-delete-dose-units/quickstart.md`; confirm no purchase push and SC-003/SC-004 behavior

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: Sem dependências
- **Foundational (Phase 2)**: Depende do Setup — **bloqueia** todas as user stories
- **US1 / US2 / US3 (Phases 3–5)**: Dependem do Foundational; após T008 podem avançar em paralelo se houver capacidade (cuidado com conflitos em `MedicationService` / `medications.page.ts` / `schedules.page.ts`)
- **Polish (Phase 6)**: Após as stories desejadas

### User Story Dependencies

- **US1 (P1)**: Após Foundational — exclusão/cascata; não exige UI de quantity na agenda
- **US2 (P1)**: Após Foundational — quantity na agenda (create T019 + edit T020) + take; independente de DELETE
- **US3 (P1)**: Após Foundational — alerta por unidades; DTO/UI em paralelo com US2

### Within Each User Story

- Testes de API primeiro (falhando) → serviço/endpoint → frontend
- US1: service delete → resource → UI → adherence names
- US2: take/stock → remove med field → create qty → edit qty
- US3: alert resource → alerts UI → med form labels

### Parallel Opportunities

- T001 ∥ T002 (Setup)
- T009 ∥ T010 (US1 tests)
- T015 ∥ T016 (US2 tests)
- T021 ∥ T022 (US3 tests)
- T026 ∥ T027 (Polish)
- Após Foundational: Dev A = US1, Dev B = US2, Dev C = US3 (coordenar `api.ts` / `MedicationService`)

---

## Parallel Example: User Story 1

```bash
# Tests in parallel:
Task: "API tests soft-delete cascade in backend/.../MedicationDeleteTest.java"
Task: "API tests isolation in backend/.../MedicationDeleteIsolationTest.java"

# Then sequential implementation:
Task: "Transactional soft-delete in MedicationService.java"
Task: "DELETE endpoint in MedicationResource.java"
Task: "UI confirm + delete in medications.page.ts + api.ts"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Phase 1 Setup
2. Phase 2 Foundational (V6 + entities + DTOs)
3. Phase 3 US1 (delete cascade)
4. **STOP**: validar exclusão via quickstart US1
5. Demo MVP de catálogo limpável

### Incremental Delivery

1. Setup + Foundational → schema pronto
2. US1 → exclusão → demo
3. US2 → dose na agenda (create+edit) + take correto → demo
4. US3 → alerta por unidade → demo
5. Polish → quickstart completo

### Parallel Team Strategy

1. Equipe fecha Foundational junto
2. Depois: A=US1, B=US2, C=US3
3. Integrar e rodar T028

---

## Notes

- Migração é **V6** (`V5__notification_log.sql` já existe)
- Soft-delete sem restore (clarificação)
- PENDING skip = **todas** do medicamento (analyze remediação I1)
- Schedule `quantityPerDose` editável na lista (T020) — FR-006 / remediação U1
- OpenAPI canônico = `specs/001-medication-control/contracts/openapi.yaml` após T001
- Limiar migrado sempre para **10** unidades
- Inteiros apenas: stock, threshold, quantityPerDose (`0` válido para stock/limiar)
- Constitution IV: testes de API obrigatórios nesta feature
