# Implementation Plan: Edição e Exclusão de Agendas

**Branch**: `006-agenda-edit-delete` | **Date**: 2026-08-19 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/006-agenda-edit-delete/spec.md`

## Summary

Entregar edição estruturada de agendas via tela dedicada (reutilizando o
formulário da Nova Agenda), remover edição inline de unidades por dose na
listagem, e exclusão permanente de uma agenda com cascata do histórico de
doses daquela agenda — sem apagar o medicamento do catálogo.

Backend: estender `ScheduleService`/`ScheduleResource` com regras de edição
completa (FR-007, FR-008a/b), novo `DELETE /schedules/{id}` com cascata
espelhando o padrão de `MedicationService.delete` (004) limitado a um
`scheduleId`. Frontend: rota `/agendas/:id/editar`, botão Editar na lista,
botão Excluir só na edição, `PATCH` estrutural vs status-only distinguidos
pelo corpo da requisição.

Sem migração Flyway — schema inalterado.

## Technical Context

**Language/Version**: Java 21 (Quarkus), TypeScript (Angular standalone)

**Primary Dependencies**: Quarkus REST + Hibernate Panache; Angular
ReactiveForms; REST Assured (`@QuarkusTest`)

**Storage**: PostgreSQL 16 — sem alteração de schema (V7 é a última migração)

**Testing**: Testes de API obrigatórios para edição completa, exclusão em
cascata, isolamento entre agendas do mesmo medicamento e entre usuários.
Validação manual via [quickstart.md](./quickstart.md). Sem E2E gate.

**Target Platform**: Web + Mobile (PWA responsivo); Docker Compose inalterado

**Project Type**: Web application (frontend + backend + database)

**Performance Goals**: PATCH/DELETE de agenda em tempo interativo (&lt;2s);
regeneração de doses futuras delegada ao `DoseScheduler` existente

**Constraints**: Auth/whitelist inalterados; medicamento fixo na edição; estoque
não revertido na exclusão; FKs RESTRICT (cascata no serviço)

**Scale/Scope**: Whitelist pequena; uma agenda por operação; dezenas de doses
por agenda no máximo

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Princípio | Status | Evidência no plano |
|-----------|--------|-------------------|
| I. Propósito medicamentos/lembretes | PASS | Edição/exclusão de planos de tratamento alinhada ao controle de adesão |
| II. Camadas Angular ↔ API ↔ Quarkus ↔ PG | PASS | Lógica em `ScheduleService`; UI consome REST; sem acesso direto ao PG |
| III. Google + whitelist + admin | PASS | Sem alteração de auth/whitelist |
| IV. Testes de API, sem E2E gate | PASS | Novos testes REST Assured; quickstart manual; sem E2E obrigatório |
| V. Mobile+Web, Docker, Ubuntu | PASS | Mesmas telas responsivas; containers inalterados |

**Pós-Phase 1**: PASS mantido — sem broker/cache/serviço novo; contratos em
[contracts/](./contracts/); complexidade limitada a extensão de serviço
existente + rota UI + delete transacional.

## Project Structure

### Documentation (this feature)

```text
specs/006-agenda-edit-delete/
├── plan.md              # This file
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   ├── openapi.yaml     # delta PATCH + DELETE schedules
│   └── ui-editar-agenda.md
└── tasks.md             # /speckit-tasks
```

### Source Code (repository root)

```text
backend/
├── src/main/java/app/medicamentos/
│   ├── schedule/
│   │   ├── ScheduleResource.java    # DELETE; PATCH semântica ampliada
│   │   └── ScheduleService.java     # editFull, delete, purgePending, recalcStatus
│   ├── dose/                        # doses apagadas na cascata de agenda
│   └── notify/                      # notification_log apagado antes das doses
└── src/test/java/app/medicamentos/schedule/
    ├── ScheduleEditTest.java
    ├── ScheduleDeleteTest.java
    └── ScheduleDeleteIsolationTest.java

frontend/
├── src/app/
│   ├── app.routes.ts                # agendas/:id/editar
│   ├── core/api/api.ts              # deleteSchedule; PATCH estrutural
│   └── features/schedules/
│       └── schedules.page.ts        # lista read-only qty; Editar; form create/edit
```

**Structure Decision**: Estender módulos `schedule`, `dose` e `notify` já
existentes; UI no mesmo arquivo `schedules.page.ts` (padrão de
`medications.page.ts` create/edit). Sem novos pacotes nem migração.

## Complexity Tracking

> Sem violações constitutivas — tabela não aplicável.
