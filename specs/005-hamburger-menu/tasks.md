# Tasks: Menu de navegação em formato hambúrguer

**Input**: Design documents from `/specs/005-hamburger-menu/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/, quickstart.md

**Tests**: Não solicitados na spec — sem tarefas de teste automatizado (constitution: sem E2E gate; API inalterada).

**Organization**: Por user story. Alterações concentram-se em `frontend/src/app/shared/layout.component.ts`.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Pode rodar em paralelo (arquivos diferentes, sem dependência incompleta)
- **[Story]**: US1 / US2 / US3 / US4
- Incluir caminhos de arquivo nas descrições

## Path Conventions

- Web app: `frontend/src/` (backend fora de escopo desta feature)

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Confirmar ponto único de mudança no monorepo existente

- [x] T001 Confirm `LayoutComponent` in `frontend/src/app/shared/layout.component.ts` is the authenticated shell (brand, nav, Admin `@if`, Sair) and that `frontend/src/app/app.routes.ts` already keeps `/login` and `/bloqueado` outside that layout; no new npm packages per `specs/005-hamburger-menu/plan.md`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Estado do menu e corte 767/768 compartilhados por todas as stories

**⚠️ CRITICAL**: Completar antes de US1–US4

- [x] T002 Add `menuOpen = false`, `toggleMenu()`, and `closeMenu()` on `LayoutComponent` in `frontend/src/app/shared/layout.component.ts`; listen to `window.matchMedia('(max-width: 767px)')` and call `closeMenu()` when the viewport becomes wide (≥768px)
- [x] T003 Add a hamburger control (three-line icon, accessible name “Menu”, `aria-expanded` bound to `menuOpen`) in the header of `frontend/src/app/shared/layout.component.ts`, wired to `toggleMenu()`, without hiding the existing `<nav>` yet so tablet/desktop keep the current link bar

**Checkpoint**: Estado e botão prontos — stories podem começar (barra atual ainda visível em todas as larguras)

---

## Phase 3: User Story 1 - Abrir e navegar pelo menu hambúrguer no telefone (Priority: P1) 🎯 MVP

**Goal**: No telefone (≤767px), destinos só no painel hambúrguer; escolher um destino navega e fecha o menu

**Independent Test**: Viewport 375px, autenticar, abrir o hambúrguer, ir para Agendas; painel fecha e `/agendas` aparece. Sem fileira permanente de links.

### Implementation for User Story 1

- [x] T004 [US1] In `frontend/src/app/shared/layout.component.ts` styles, at `max-width: 767px` show the hamburger and hide the permanent horizontal nav when `menuOpen` is false (replace the existing `max-width: 700px` wrap rule)
- [x] T005 [US1] When `menuOpen` is true on phone, display the existing `<nav>` as a vertical panel listing Medicamentos, Agendas, Doses, Compras, Histórico (Admin still `@if`); keep `RouterLinkActive` highlight of the current destination in `frontend/src/app/shared/layout.component.ts`
- [x] T006 [US1] On destination click inside the panel, call `closeMenu()` in `frontend/src/app/shared/layout.component.ts` so navigation leaves the menu closed
- [x] T007 [US1] Keep brand (`routerLink="/medicamentos"`) and Sair visible in the phone header regardless of `menuOpen` in `frontend/src/app/shared/layout.component.ts`

**Checkpoint**: US1 testável em 375px; tablet/desktop ainda podem mostrar a barra antiga até US2

---

## Phase 4: User Story 2 - Navegar pela barra de links no tablet e no computador (Priority: P1)

**Goal**: Em ≥768px, barra permanente de links e nenhum hambúrguer/painel

**Independent Test**: Viewport 768px e 1280px: barra visível, sem ícone hambúrguer; clicar Doses vai a `/doses` com destaque ativo.

### Implementation for User Story 2

- [x] T008 [US2] In `frontend/src/app/shared/layout.component.ts` styles, at `min-width: 768px` show the horizontal link bar and hide the hamburger and stacked panel regardless of `menuOpen` (do not add an overlay here; overlay is T010)
- [x] T009 [US2] Confirm `RouterLinkActive` still highlights the current destination on the wide bar in `frontend/src/app/shared/layout.component.ts`

**Checkpoint**: Telefone = hambúrguer; tablet/computador = barra

---

## Phase 5: User Story 3 - Fechar o menu hambúrguer sem navegar (Priority: P2)

**Goal**: Fechar o painel no telefone pelo botão, pelo overlay ou pelo voltar do navegador, sem mudar a tela

**Independent Test**: Em 375px, abrir o menu e fechar pelo hambúrguer e pelo fundo; rota inalterada. Abrir, navegar, voltar no browser → painel fechado.

### Implementation for User Story 3

- [x] T010 [US3] Add an overlay/backdrop in `frontend/src/app/shared/layout.component.ts` visible only when `menuOpen` is true and viewport is ≤767px; clicking it calls `closeMenu()` without changing the route; at `min-width: 768px` the overlay MUST stay hidden even if `menuOpen` is still true
- [x] T011 [US3] Subscribe to Angular `Router` `NavigationEnd` in `frontend/src/app/shared/layout.component.ts` and call `closeMenu()` so the panel does not stay open after browser Back

**Checkpoint**: US3 cobre FR-009 e o edge case do voltar

---

## Phase 6: User Story 4 - Destinos corretos conforme o perfil (Priority: P2)

**Goal**: Mesmos destinos nos dois modos; Admin só para administrador

**Independent Test**: Usuário comum não vê Admin no painel (375px) nem na barra (768px). Admin vê e chega em `/admin/whitelist`.

### Implementation for User Story 4

- [x] T012 [US4] Keep a single shared `<nav>` in `frontend/src/app/shared/layout.component.ts` so phone panel and wide bar cannot diverge; Admin remains `@if (me.profile()?.admin)` with `routerLink="/admin/whitelist"` (do not duplicate destination lists)

**Checkpoint**: FR-006 / FR-007 nos dois modos

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Resize, empilhamento visual e validação manual

- [x] T013 Ensure crossing 767↔768px in `frontend/src/app/shared/layout.component.ts` closes an open panel, swaps hamburger ↔ bar, and does not change the current route (FR-014, FR-015); overlay z-index above `main` content
- [ ] T014 Run manual scenarios A–F in `specs/005-hamburger-menu/quickstart.md` against the authenticated layout (375px, 768px, 1280px, resize, Back, admin vs comum, `/login` and `/bloqueado`)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: Imediato
- **Foundational (Phase 2)**: Após Setup — bloqueia stories
- **US1 (Phase 3)**: Após Foundational — MVP telefone
- **US2 (Phase 4)**: Após Foundational; na prática após US1 (mesmo arquivo)
- **US3 (Phase 5)**: Após US1 (precisa do painel aberto)
- **US4 (Phase 6)**: Após Foundational; na prática após US1+US2 (mesmo `<nav>`)
- **Polish (Phase 7)**: Após US1–US4

### User Story Dependencies

- **User Story 1 (P1)**: Independente após T002–T003 — MVP
- **User Story 2 (P1)**: Independente funcionalmente (modo wide); mesmo arquivo → sequencial após US1
- **User Story 3 (P2)**: Depende do painel da US1
- **User Story 4 (P2)**: Independente funcionalmente (já há `@if` admin); confirmar após US1+US2

### Parallel Opportunities

- Poucas: feature monofile. Sem `[P]` nas tasks de implementação para evitar edição concorrente de `layout.component.ts`.
- Stories são testáveis de forma independente após implementadas (larguras diferentes / perfil).

---

## Parallel Example

Não aplicável em paralelo de código (único arquivo). Sequência recomendada:

```text
T001 → T002 → T003 → T004 → T005 → T006 → T007 → T008 → T009 → T010 → T011 → T012 → T013 → T014
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. T001–T003
2. T004–T007 (telefone hambúrguer)
3. **STOP and VALIDATE**: quickstart cenário A em 375px
4. Demo em telefone; tablet/desktop ainda com barra até US2

### Incremental Delivery

1. Setup + Foundational
2. US1 → validar telefone (MVP)
3. US2 → validar tablet/computador (entrega mínima completa para os dois modos)
4. US3 → fechar sem navegar
5. US4 → conferir Admin
6. Polish → quickstart A–F

### Parallel Team Strategy

Um desenvolvedor no mesmo arquivo. Não paralelizar US1–US4 em branches que toquem `layout.component.ts`.

---

## Notes

- Sem `[P]`: todas as tasks de código editam `frontend/src/app/shared/layout.component.ts`
- Sem testes automatizados (spec/constitution)
- Contrato: `specs/005-hamburger-menu/contracts/ui-layout-nav.md`
- Commit após cada task ou grupo lógico
- Parar em qualquer checkpoint para validar a story
