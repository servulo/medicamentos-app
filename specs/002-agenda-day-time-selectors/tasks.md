# Tasks: Seletores de dias e horários na Nova Agenda

**Input**: Design documents from `/specs/002-agenda-day-time-selectors/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/, quickstart.md

**Tests**: Não solicitados na spec — sem tarefas de teste automatizado (constitution: sem E2E gate; API inalterada).

**Organization**: Por user story. Alterações concentram-se em `frontend/src/app/features/schedules/schedules.page.ts` (`ScheduleFormPage`).

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Pode rodar em paralelo (arquivos diferentes, sem dependência incompleta)
- **[Story]**: US1 / US2
- Incluir caminhos de arquivo nas descrições

## Path Conventions

- Web app: `frontend/src/`, `backend/src/` (backend fora de escopo desta feature)

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Confirmar ponto único de mudança no monorepo existente

- [x] T001 Confirm `ScheduleFormPage` in `frontend/src/app/features/schedules/schedules.page.ts` matches contracts in `specs/002-agenda-day-time-selectors/contracts/ui-nova-agenda.md` (no new packages)

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Estado e grade horária compartilhados pelas stories

**⚠️ CRITICAL**: Completar antes de US1/US2

- [x] T002 Add `HOURLY_SLOTS` (`00:00`…`23:00`) and `selectedTimes: string[] = []` (starts empty) on `ScheduleFormPage` in `frontend/src/app/features/schedules/schedules.page.ts`

**Checkpoint**: Draft state ready — stories can proceed

---

## Phase 3: User Story 1 - Escolher horários em uma lista (Priority: P1) 🎯 MVP

**Goal**: Substituir input de texto por seletor horário + lista removível (vazia, cronológica, já adicionados desabilitados)

**Independent Test**: Em `/agendas/nova`, adicionar/remover horários só pela lista; salvar exige ≥1 horário; payload `timesOfDay` com valores `HH:00`

### Implementation for User Story 1

- [x] T003 [US1] Remove free-text `times` form control and “Horários (separe por vírgula)” input from `ScheduleFormPage` in `frontend/src/app/features/schedules/schedules.page.ts`
- [x] T004 [US1] Add hourly add `<select>` bound to `HOURLY_SLOTS` with already-selected options `disabled` in `frontend/src/app/features/schedules/schedules.page.ts`; after a successful add, reset the select to an empty/placeholder value so another hour can be chosen immediately
- [x] T005 [US1] Add removable times list UI (each item removable) showing `selectedTimes` in chronological order in `frontend/src/app/features/schedules/schedules.page.ts`
- [x] T006 [US1] Implement `addTime` / `removeTime` (no duplicates; re-sort chronological; reset add-select after add) and update `save()` to send `timesOfDay: this.selectedTimes` via `ApiService.createSchedule` in `frontend/src/app/features/schedules/schedules.page.ts`
- [x] T007 [US1] When `selectedTimes.length === 0`, block submit and show a visible validation message that at least one time is required (do not rely on a disabled button alone) in `frontend/src/app/features/schedules/schedules.page.ts`

**Checkpoint**: US1 testável sem o botão “selecionar todos” (dias ainda só por checkbox)

---

## Phase 4: User Story 2 - Selecionar todos os dias da semana (Priority: P1)

**Goal**: Botão que marca Seg–Dom de uma vez (idempotente)

**Independent Test**: Em `/agendas/nova`, acionar o botão → sete dias marcados; desmarcar um individualmente; salvar com todos os dias

### Implementation for User Story 2

- [x] T008 [US2] Add “Selecionar todos os dias” control and `selectAllDays()` setting `selectedDays` to `[1,2,3,4,5,6,7]` (idempotent) beside the existing day checkboxes; preserve individual `toggleDay` checkboxes in `frontend/src/app/features/schedules/schedules.page.ts`

**Checkpoint**: US1 + US2 funcionando na mesma tela

---

## Phase 5: Polish & Cross-Cutting Concerns

**Purpose**: Validação manual e consistência do formulário

- [x] T009 Ensure create button requires `selectedDays.length > 0` and `selectedTimes.length > 0`, and show a visible message when days are missing (and reuse/keep the times message from T007) in `frontend/src/app/features/schedules/schedules.page.ts`
- [x] T010 Run manual scenarios A–D in `specs/002-agenda-day-time-selectors/quickstart.md` against `/agendas/nova`; confirm medication and duration fields still work unchanged (FR-008)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: Imediato
- **Foundational (Phase 2)**: Após Setup — bloqueia stories
- **US1 (Phase 3)**: Após Foundational — MVP
- **US2 (Phase 4)**: Após Foundational; preferir após US1 porque o mesmo arquivo (`schedules.page.ts`) evita conflito de merge
- **Polish (Phase 5)**: Após US1 + US2

### User Story Dependencies

- **User Story 1 (P1)**: Independente funcionalmente após T002
- **User Story 2 (P1)**: Independente funcionalmente (só dias); mesmo arquivo → executar sequencialmente após US1 na prática

### Parallel Opportunities

- Poucas: feature monofile. Sem `[P]` nas tasks de implementação para evitar edição concorrente do mesmo arquivo.
- US1 e US2 são testáveis de forma independente após implementadas.

---

## Parallel Example

Não aplicável em paralelo de código (único arquivo). Sequência recomendada:

```text
T001 → T002 → T003 → T004 → T005 → T006 → T007 → T008 → T009 → T010
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. T001–T002
2. T003–T007 (lista de horários)
3. Validar quickstart cenários B/C (parte horários)
4. Depois T008 (selecionar todos) + T009–T010

### Incremental Delivery

1. Setup + Foundational
2. US1 → demo MVP (sem digitar horário)
3. US2 → demo agenda diária em um toque
4. Polish / quickstart completo

---

## Notes

- Sem mudanças em `backend/` ou OpenAPI
- Sem default `08:00` — lista removível inicia vazia (FR-009)
- Formato checklist validado: checkbox + ID + [Story] onde cabe + caminho de arquivo
