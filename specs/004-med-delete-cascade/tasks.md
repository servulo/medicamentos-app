# Tasks: Exclusão Completa de Medicamento, Agenda e Histórico

**Input**: Design documents from `/specs/004-med-delete-cascade/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/, quickstart.md

**Tests**: Incluídos testes de API Quarkus (REST Assured) — obrigatórios pela
constitution (Princípio IV) e pelo plan.md. Sem E2E de UI como gate.
Confirmação da US2 é validação manual (diálogo nativo).

**Organization**: Setup → Foundational (migração V7 + remover `deleted_at`) →
US1 → US2 → US3 → Polish

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies on incomplete work)
- **[Story]**: US1 / US2 / US3 conforme spec.md
- Include exact file paths in descriptions

## Path Conventions

- Backend: `backend/src/main/java/app/medicamentos/`, testes em `backend/src/test/java/`
- Frontend: `frontend/src/app/`
- Specs: `specs/004-med-delete-cascade/`
- OpenAPI canônico: `specs/001-medication-control/contracts/openapi.yaml`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Alinhar contrato desta feature no OpenAPI canônico e confirmar o
próximo Flyway

- [x] T001 Merge DELETE hard-delete semantics from `specs/004-med-delete-cascade/contracts/openapi.yaml` into `specs/001-medication-control/contracts/openapi.yaml` (001 remains canonical; 004 delta stays as change log). Replace **all** 003 wording on `deleteMedication` (soft-delete, `CANCELLED`, `SKIPPED`) with: cascata permanente (logs → doses → agendas → medicamento); 204; segundo DELETE → 404; GET med/agenda → 404; doses e alertas daquele id ausentes. Do this in T001 completely — do not leave a follow-up OpenAPI pass
- [x] T002 [P] Confirm next Flyway version is `V7` (existing `V6__med_delete_dose_units.sql`) and list touchpoints: `backend/src/main/java/app/medicamentos/medication/MedicationService.java`, `MedicationEntity.java`, `notify/NotificationLogEntity.java`, `frontend/src/app/features/medications/medications.page.ts`, `backend/src/test/java/app/medicamentos/medication/MedicationDeleteTest.java`, `MedicationDeleteIsolationTest.java`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Schema sem `deleted_at` e list/get sem soft-delete — bloqueia US1/US2/US3

**⚠️ CRITICAL**: Nenhuma user story começa antes desta fase

- [x] T003 Create Flyway migration `backend/src/main/resources/db/migration/V7__med_hard_delete_cascade.sql`: purge rows with `medications.deleted_at IS NOT NULL` in FK order (`notification_log` of those doses → `dose_occurrences` → `treatment_schedules` → `medications`); then `ALTER TABLE medications DROP COLUMN deleted_at`
- [x] T004 Remove `deletedAt` field and `isDeleted()` from `backend/src/main/java/app/medicamentos/medication/MedicationEntity.java`
- [x] T005 Update `backend/src/main/java/app/medicamentos/medication/MedicationService.java`: drop `deletedAt is null` filters on list/get; remove `getIncludingDeleted`; in `delete()` keep only the ownership `get(id)` (404 if missing/other user) and **do not** set `deletedAt`, cancel schedules, or skip doses (no 003 residual). Full cascade is T008. Project must compile. Do not demo or ship DELETE until T008

**Checkpoint**: App compila; `deleted_at` inexistente; list/get só linhas existentes; `delete()` ainda não remove o medicamento nem filhos — **não demo/entregar exclusão até T008**

---

## Phase 3: User Story 1 - Excluir medicamento remove agenda e histórico (Priority: P1) 🎯 MVP

**Goal**: Hard-delete transacional: medicamento + todas as agendas + todas as
doses (PENDING/TAKEN/SKIPPED) + `notification_log` dessas doses; 404 no
segundo DELETE e em GET de agenda/medicamento apagados; sem restore

**Independent Test**: (1) med + agenda + doses → DELETE 204; GET med/agenda 404; `GET /doses` e `GET /doses?status=PENDING` sem aquele `medicationId`; segundo DELETE 404; (2) várias agendas → todas 404; (3) med só no catálogo → 204 depois GET 404

### Tests for User Story 1

> Escrever primeiro; devem **falhar** até T008

- [x] T006 [P] [US1] Rewrite API tests in `backend/src/test/java/app/medicamentos/medication/MedicationDeleteTest.java`: (1) med + one ACTIVE schedule + PENDING/TAKEN/SKIPPED → DELETE 204; GET med 404; GET schedule 404 (not `CANCELLED`); `GET /doses` and `GET /doses?status=PENDING` contain none of that `medicationId` (not `SKIPPED` with `medicationName`); TAKEN/SKIPPED history gone; second DELETE 404; (2) med with two+ schedules (`ACTIVE` and `PAUSED` or `COMPLETED`/`CANCELLED`) → all those schedule ids 404; (3) catalog-only med (no schedules/doses) → DELETE 204 then GET 404; no stock assertions on a missing row
- [x] T007 [P] [US1] Update API tests in `backend/src/test/java/app/medicamentos/medication/MedicationDeleteIsolationTest.java`: other user DELETE still 404; owner DELETE then `POST /schedules` with that id → 404; owner’s data unchanged when other user attempts delete

### Implementation for User Story 1

- [x] T008 [US1] Implement transactional hard-delete in `backend/src/main/java/app/medicamentos/medication/MedicationService.java` `delete(UUID)`: resolve own medication (404 otherwise); delete `notification_log` rows for those doses (via `backend/src/main/java/app/medicamentos/notify/NotificationLogEntity.java`); delete `dose_occurrences` for `medicationId`+`userId`; delete `treatment_schedules` for `medicationId`+`userId`; delete the medication row. Single `@Transactional`. Do not cancel/skip. `MedicationResource.delete` already returns 204 — keep that

**Checkpoint**: US1 validável só via API (`MedicationDelete*`); MVP de exclusão completa pronto. Texto do `confirm()` ainda desatualizado até US2

---

## Phase 4: User Story 2 - Confirmar exclusão irreversível (Priority: P1)

**Goal**: Diálogo de confirmação avisa perda permanente de medicamento, agendas
e histórico; cancelar não chama a API

**Independent Test**: Clicar Excluir → ler aviso de perda permanente; cancelar → dados intactos; confirmar → mesmo resultado da US1 (manual; sem E2E)

### Implementation for User Story 2

- [x] T009 [US2] Update the `confirm()` copy in `frontend/src/app/features/medications/medications.page.ts` `remove()` so it states that the medication, all schedules, and all dose history will be permanently deleted and cannot be restored; keep `return` on cancel so `deleteMedication` is not called

**Checkpoint**: US2 validável na UI Mobile e Web (mesmo `confirm`); cancelar não dispara DELETE

---

## Phase 5: User Story 3 - Demais tratamentos permanecem intactos (Priority: P2)

**Goal**: Excluir um medicamento não altera catálogo, agendas, histórico nem
avisos de compra dos outros itens do mesmo usuário

**Independent Test**: Dois medicamentos com agenda e histórico; DELETE só do primeiro → segundo intacto em listagens de med/agenda/doses; alerta de compra do primeiro some; do segundo permanece se `stock <= threshold`

### Tests for User Story 3

> Escrever primeiro; devem **falhar** se T008 não filtrar por `medicationId`

- [x] T010 [P] [US3] Add API tests in `backend/src/test/java/app/medicamentos/medication/MedicationDeleteSiblingTest.java` (do not edit `MedicationDeleteTest.java`): two medications for the same user; DELETE first; second still GET 200 with schedules and doses; `GET /alerts/purchase` excludes the deleted id and still includes the sibling when it meets the unit threshold

### Implementation for User Story 3

- [x] T011 [US3] Confirm cascade deletes in `backend/src/main/java/app/medicamentos/medication/MedicationService.java` are scoped only to the target `medicationId` and `userId` (no unscoped `deleteAll`); fix if sibling tests fail

**Checkpoint**: US3 validável via API — isolamento entre medicamentos do mesmo usuário

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Sem referências a soft-delete; validar quickstart

- [x] T012 [P] Remove leftover `deletedAt` / `deleted_at` / `getIncludingDeleted` / soft-delete copy from `backend/src/main/java/app/medicamentos/`, `backend/src/test/java/app/medicamentos/`, and `frontend/src/app/`
- [x] T013 Run `cd backend && ./mvnw test -Dtest=MedicationDelete*` (covers `MedicationDeleteTest`, `MedicationDeleteIsolationTest`, `MedicationDeleteSiblingTest`) and walk US1–US3 checks in `specs/004-med-delete-cascade/quickstart.md`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: Sem dependências
- **Foundational (Phase 2)**: Depende do Setup — **bloqueia** todas as user stories
- **US1 / US2 / US3 (Phases 3–5)**: Dependem do Foundational
  - US1 (T008) desbloqueia a semântica real do DELETE
  - US2 pode ir em paralelo com US1 (arquivo só de UI)
  - US3 testes (T010, arquivo próprio) podem ser escritos em paralelo com T006/T007; T011 só após T008
- **Polish (Phase 6)**: Após as stories desejadas

### User Story Dependencies

- **US1 (P1)**: Após Foundational — hard-delete em cascata; MVP
- **US2 (P1)**: Após Foundational — só texto do `confirm()`; independente do backend, mas o aviso só é verdadeiro depois de T008
- **US3 (P2)**: Após T008 — prova que a cascata é restrita ao `medicationId`

### Within Each User Story

- Testes de API primeiro (falhando) → `MedicationService.delete` → UI de confirmação
- US1: T006/T007 → T008
- US2: T009 (manual)
- US3: T010 → T011 se sibling vazar

### Parallel Opportunities

- T001 ∥ T002 (Setup)
- T006 ∥ T007 ∥ T010 (testes em arquivos distintos)
- T009 (US2 UI) ∥ T006–T008 (US1 backend) — arquivos diferentes
- T012 (Polish grep; T013 é sequencial — roda os testes)

---

## Parallel Example: User Story 1

```bash
# Tests in parallel:
Task: "Rewrite MedicationDeleteTest.java for hard-delete cascade"
Task: "Update MedicationDeleteIsolationTest.java for 404/ownership"
Task: "Add MedicationDeleteSiblingTest.java for sibling + purchase alerts"

# Then sequential implementation:
Task: "Transactional hard-delete in MedicationService.java (logs → doses → schedules → med)"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Phase 1 Setup
2. Phase 2 Foundational (V7 + entity + list/get)
3. Phase 3 US1 (testes + hard-delete)
4. **STOP**: validar exclusão via `MedicationDeleteTest` e quickstart US1
5. Demo: medicamento some com agendas e histórico

### Incremental Delivery

1. Setup + Foundational → schema sem `deleted_at`
2. US1 → cascata → demo MVP
3. US2 → aviso de perda permanente → demo
4. US3 → isolamento entre medicamentos → demo
5. Polish → grep + quickstart

### Parallel Team Strategy

1. Equipe fecha Foundational junto
2. Depois: A = US1 (serviço + testes), B = US2 (confirm UI), C = US3 testes sibling (após T008)
3. Integrar e rodar T013

---

## Notes

- Migração é **V7** (`V6__med_delete_dose_units.sql` já existe)
- Hard-delete sem restore; FKs permanecem RESTRICT — ordem da cascata é obrigatória
- OpenAPI canônico = `specs/001-medication-control/contracts/openapi.yaml` após T001
- Constitution IV: testes de API obrigatórios; sem E2E
- Regras 003 (quantidade por dose, alerta por unidade) não entram nesta lista
