# Tasks: Edição e Exclusão de Agendas

**Input**: Design documents from `/specs/006-agenda-edit-delete/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/, quickstart.md

**Tests**: Incluídos testes de API Quarkus (REST Assured) — obrigatórios pela
constitution (Princípio IV) e pelo plan.md. Sem E2E de UI como gate.
Confirmação da US3 é validação manual (diálogo nativo).

**Organization**: Setup → Foundational (PATCH estrutural vs status-only) → US1
→ US2 → US3 → Polish

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies on incomplete work)
- **[Story]**: US1 / US2 / US3 conforme spec.md
- Include exact file paths in descriptions

## Path Conventions

- Backend: `backend/src/main/java/app/medicamentos/`, testes em `backend/src/test/java/`
- Frontend: `frontend/src/app/`
- Specs: `specs/006-agenda-edit-delete/`
- OpenAPI canônico: `specs/001-medication-control/contracts/openapi.yaml`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Alinhar contrato desta feature no OpenAPI canônico e confirmar
ausência de migração Flyway

- [x] T001 Merge PATCH estrutural + `DELETE /schedules/{scheduleId}` semantics from `specs/006-agenda-edit-delete/contracts/openapi.yaml` into `specs/001-medication-control/contracts/openapi.yaml` (001 remains canonical; 006 delta stays as change log). Document status-only vs edição completa on `updateSchedule`; add `deleteSchedule` 204/404; keep 004 medication delete unchanged
- [x] T002 [P] Confirm **no** new Flyway migration (schema unchanged; `V7` is latest) and list touchpoints: `backend/src/main/java/app/medicamentos/schedule/ScheduleService.java`, `ScheduleResource.java`, `frontend/src/app/features/schedules/schedules.page.ts`, `frontend/src/app/app.routes.ts`, `frontend/src/app/core/api/api.ts`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Separar PATCH status-only (listagem) de edição completa (formulário)
— bloqueia US1 e US2

**⚠️ CRITICAL**: Nenhuma user story começa antes desta fase

- [x] T003 Refactor `backend/src/main/java/app/medicamentos/schedule/ScheduleService.java` `update()`: detect structural fields (`daysOfWeek`, `timesOfDay`, `durationType`, `maxTakenDoses`, `quantityPerDose`) per research.md; if none → existing status-only path (pause/reactivate, `resetTakenCount`); if any → delegate to new `updateFull()` stub that throws 501 or no-op until T007 — project must compile
- [x] T004 Add private helpers in `backend/src/main/java/app/medicamentos/schedule/ScheduleService.java`: `recurrenceChanged(before, after)`, `recalcStatusAfterFullEdit(entity)`, `purgePendingDoses(UUID scheduleId)` (delete `notification_log` then `dose_occurrences` where `status=PENDING` for schedule+user). Helpers may be no-op/skeleton until T008 but signatures must exist

**Checkpoint**: App compila; PATCH status-only na listagem inalterado; edição
completa ainda não funcional até US1

---

## Phase 3: User Story 1 - Editar agenda pela tela dedicada (Priority: P1) 🎯 MVP

**Goal**: Edição completa via PATCH estrutural (FR-006–008, FR-008a/b, FR-007);
tela `/agendas/:id/editar` reutilizando formulário Nova Agenda; listagem sem
edição inline de unidades por dose; botão Editar

**Independent Test**: Lista → Editar → alterar horário e unidades/dose → Salvar
→ lista reflete mudanças; PATCH 200; PAUSED/CANCELLED → ACTIVE ao salvar;
mudança de horários purga PENDING (incl. snoozed); TAKEN/SKIPPED preservados

### Tests for User Story 1

> Escrever primeiro; devem **falhar** até T008

- [x] T005 [P] [US1] Add API tests in `backend/src/test/java/app/medicamentos/schedule/ScheduleEditTest.java`: (1) PATCH structural changes days/times/qty/duration → 200 with updated fields; (2) PAUSED + structural PATCH → ACTIVE; (3) CANCELLED + structural PATCH → ACTIVE; (4) FIXED `maxTakenDoses < takenCount` → 400; (5) change `timesOfDay` removes all PENDING (incl. snoozed) but keeps TAKEN/SKIPPED; (5b) change only `daysOfWeek` also purges PENDING; (6) duration-only change without days/times change does not purge PENDING; (7) COMPLETED + increased max → ACTIVE; (7b) ACTIVE FIXED with `takenCount >=` new `maxTakenDoses` → COMPLETED; (8) ignore/reject `medicationId` change if sent; (9) other user structural PATCH → 404 and owner schedule unchanged; (10) take with quantityPerDose=1 (stock decreases by 1), then PATCH quantityPerDose=2 → previous stock unchanged; next take decreases by 2
- [x] T006 [P] [US1] Extend `backend/src/test/java/app/medicamentos/schedule/ScheduleResourceTest.java`: PATCH **only** `{status: PAUSED}` leaves schedule PAUSED (no FR-008b); reactivate with `{status: ACTIVE}` still works with `resetTakenCount`

### Implementation for User Story 1

- [x] T007 [US1] Implement `updateFull()` in `backend/src/main/java/app/medicamentos/schedule/ScheduleService.java`: apply structural fields; validate `maxTakenDoses >= takenCount`; FR-008b (PAUSED/CANCELLED→ACTIVE); FR-008a recalc; FR-007 purge when recurrence changed; reject/ignore `medicationId`
- [x] T008 [US1] Keep `backend/src/main/java/app/medicamentos/schedule/ScheduleResource.java` `update()` as a thin delegate to `ScheduleService.update()` (no structural vs status-only branching in the resource). Confirm PATCH with only `{status}` still uses the status-only path after T007

- [x] T009 [P] [US1] Add `schedule(id)` GET and ensure `updateSchedule` sends full structural body in `frontend/src/app/core/api/api.ts`
- [x] T010 [US1] Add route `{ path: 'agendas/:id/editar', component: ScheduleFormPage }` in `frontend/src/app/app.routes.ts`
- [x] T011 [US1] Refactor `ScheduleFormPage` in `frontend/src/app/features/schedules/schedules.page.ts` for create vs edit: edit loads `GET /schedules/{id}`; medication read-only; title "Editar agenda"; submit PATCH structural (no `status`); navigate to `/agendas` on success; same day/time validations as Nova Agenda per `specs/006-agenda-edit-delete/contracts/ui-editar-agenda.md`; "Voltar"/cancel navigates to `/agendas` without calling PATCH
- [x] T012 [US1] Update `SchedulesPage` in `frontend/src/app/features/schedules/schedules.page.ts`: add Editar link per agenda; show `quantityPerDose` as read-only text; remove inline `<input>` and `saveQuantity()`; keep pause/reactivate buttons (status-only PATCH)

**Checkpoint**: US1 validável via API (`ScheduleEditTest`) e manualmente na UI;
exclusão ainda indisponível até US2/US3

---

## Phase 4: User Story 2 - Excluir agenda e seu histórico (Priority: P1)

**Goal**: `DELETE /schedules/{id}` remove agenda + todas as doses (PENDING/TAKEN/
SKIPPED) + logs; medicamento e outras agendas intactos; 404 segundo DELETE e
para outro usuário

**Independent Test**: Agenda com histórico → DELETE 204 → GET schedule 404;
`GET /doses` sem doses daquela agenda; medicamento permanece; segunda agenda
do mesmo item intacta

### Tests for User Story 2

> Escrever primeiro; devem **falhar** até T015

- [x] T013 [P] [US2] Add API tests in `backend/src/test/java/app/medicamentos/schedule/ScheduleDeleteTest.java`: schedule with PENDING/TAKEN/SKIPPED → DELETE 204; GET schedule 404; doses of that schedule absent from `GET /doses`; medication still GET 200; second DELETE 404; no stock change on medication
- [x] T014 [P] [US2] Add API tests in `backend/src/test/java/app/medicamentos/schedule/ScheduleDeleteIsolationTest.java`: other user DELETE → 404; same medication with two schedules → DELETE one leaves sibling schedule + its doses; owner data unchanged on foreign delete attempt

### Implementation for User Story 2

- [x] T015 [US2] Implement transactional `delete(UUID id)` in `backend/src/main/java/app/medicamentos/schedule/ScheduleService.java`: resolve own schedule (404); delete `notification_log` for those dose ids → all `dose_occurrences` for schedule+user → schedule row; do **not** delete medication or sibling schedules
- [x] T016 [US2] Add `@DELETE @Path("/{id}")` returning 204 in `backend/src/main/java/app/medicamentos/schedule/ScheduleResource.java`

**Checkpoint**: US2 validável via API; UI delete button ainda pendente (US3)

---

## Phase 5: User Story 3 - Confirmar exclusão irreversível (Priority: P2)

**Goal**: Botão Excluir **somente** na tela de edição; `confirm()` avisa perda
permanente de agenda e histórico; cancelar não chama API

**Independent Test**: Editar agenda → Excluir → ler aviso irreversível;
cancelar → dados intactos; confirmar → mesmo resultado da US2

### Implementation for User Story 3

- [x] T017 [US3] Add `deleteSchedule(id)` in `frontend/src/app/core/api/api.ts` and `remove()` on edit mode in `frontend/src/app/features/schedules/schedules.page.ts`: destructive button only on edit form; `confirm()` states agenda and all dose history will be permanently deleted and cannot be restored; `return` on cancel; on confirm call DELETE then navigate to `/agendas`. List page MUST NOT expose delete (FR-009)

**Checkpoint**: US3 validável na UI Mobile e Web; fluxo completo edit + delete

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Regressão e validação quickstart

- [x] T018 [P] Verify no inline quantity edit or list-level delete remains in `frontend/src/app/features/schedules/schedules.page.ts`; grep `saveQuantity` / delete on list removed
- [x] T019 Run `cd backend && ./mvnw test -Dtest=ScheduleEdit*,ScheduleDelete*,ScheduleResourceTest` and walk US1–US3 checks in `specs/006-agenda-edit-delete/quickstart.md`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: Sem dependências
- **Foundational (Phase 2)**: Depende do Setup — **bloqueia** US1 e US2
- **US1 (Phase 3)**: Depende do Foundational — MVP edição
- **US2 (Phase 4)**: Depende do Foundational — delete backend independente da UI
- **US3 (Phase 5)**: Depende de T016 (DELETE endpoint) + T011 (edit form exists)
- **Polish (Phase 6)**: Após stories desejadas

### User Story Dependencies

- **US1 (P1)**: Após Foundational — edição completa + UI; MVP
- **US2 (P1)**: Após Foundational — delete backend; testável sem UI
- **US3 (P2)**: Após US2 backend + US1 edit screen — só confirmação UI

### Within Each User Story

- Testes de API primeiro (falhando) → serviço → resource → frontend
- US1: T005/T006 → T007/T008 → T009–T012 (T009 ∥ backend se API pronta)
- US2: T013/T014 → T015/T016
- US3: T017 após T016 e T011

### Parallel Opportunities

- T001 ∥ T002 (Setup)
- T005 ∥ T006 (testes US1 em arquivos distintos)
- T013 ∥ T014 (testes US2 em arquivos distintos)
- T009 (api.ts) ∥ T007/T008 após Foundational — arquivos diferentes
- T010 ∥ T009 (routes vs api)
- US2 backend (T013–T016) ∥ US1 frontend (T010–T012) após T008 desbloqueia API
- T018 (Polish grep; T019 sequencial)

---

## Parallel Example: User Story 1

```bash
# Tests in parallel:
Task: "Add ScheduleEditTest.java for full edit, status recalc, pending purge"
Task: "Extend ScheduleResourceTest.java for status-only regression"

# Backend then frontend:
Task: "Implement updateFull() in ScheduleService.java"
Task: "Wire PATCH in ScheduleResource.java"
Task: "Edit mode in schedules.page.ts + route + api.ts"
```

---

## Parallel Example: User Story 2

```bash
# Tests in parallel:
Task: "Add ScheduleDeleteTest.java"
Task: "Add ScheduleDeleteIsolationTest.java"

# Then:
Task: "delete() cascade in ScheduleService.java"
Task: "DELETE endpoint in ScheduleResource.java"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Phase 1 Setup
2. Phase 2 Foundational (PATCH branching)
3. Phase 3 US1 (testes + backend + UI edição)
4. **STOP**: validar via `ScheduleEditTest` e quickstart US1
5. Demo: editar agenda pela tela dedicada; listagem read-only para unidades/dose

### Incremental Delivery

1. Setup + Foundational → PATCH dual-mode pronto
2. US1 → edição completa → demo MVP
3. US2 → delete backend → demo via API
4. US3 → botão Excluir + confirm → demo fluxo completo
5. Polish → grep + quickstart

### Parallel Team Strategy

1. Equipe fecha Foundational junto
2. Depois: A = US1 backend (T005–T008), B = US1 frontend (T010–T012), C = US2 (T013–T016)
3. Integrar US3 (T017) e T019

---

## Notes

- **Sem migração Flyway** nesta feature
- Cascata de delete espelha `MedicationService.delete` (004) escopada a
  `scheduleId`
- OpenAPI canônico = `specs/001-medication-control/contracts/openapi.yaml` após T001
- Constitution IV: testes de API obrigatórios; sem E2E
- `DoseScheduler` existente materializa novas PENDING após purga — não duplicar
  lógica de geração na edição
