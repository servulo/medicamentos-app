# Implementation Plan: Seletores de dias e horários na Nova Agenda

**Branch**: `002-agenda-day-time-selectors` | **Date**: 2026-08-15 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/002-agenda-day-time-selectors/spec.md`

## Summary

Melhoria de usabilidade na tela **Nova Agenda**: botão para selecionar todos os
dias da semana e escolha de horários por lista horária (intervalos de 60 min),
com adição a uma lista removível (vazia no início, ordem cronológica, horários
já adicionados desabilitados no seletor). Sem mudança de API, persistência ou
regras de negócio de agenda — apenas o formulário Angular em
`frontend/src/app/features/schedules/`.

## Technical Context

**Language/Version**: TypeScript (Angular já usado no monorepo); Java/Quarkus
inalterados nesta feature

**Primary Dependencies**: Angular standalone components, ReactiveFormsModule,
Router; `ApiService.createSchedule` existente

**Storage**: N/A (sem alteração de schema); payload `timesOfDay` / `daysOfWeek`
permanece o contrato `ScheduleCreate` de `001-medication-control`

**Testing**: Sem mudança de endpoints → testes de API novos **não** são
obrigatórios (constitution IV). Validação via [quickstart.md](./quickstart.md)
manual. Testes unitários Angular opcionais.

**Target Platform**: Web e Mobile (UI responsiva existente); mesma stack Docker

**Project Type**: Web application (frontend change only)

**Performance Goals**: Escolha de dias + um horário no caminho feliz em &lt;30s
(SC-003); lista de 24 opções leve no cliente

**Constraints**: Sem campo de texto livre para horário; lista 00:00–23:00 de
hora em hora; escopo só criação (Nova Agenda); auth/whitelist/Docker
inalterados

**Scale/Scope**: Uma tela (`ScheduleFormPage`); 7 dias; 24 slots horários

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Princípio | Status | Evidência no plano |
|-----------|--------|-------------------|
| I. Propósito medicamentos/lembretes | PASS | Facilita cadastro correto de agendas de tomada |
| II. Camadas Angular ↔ API ↔ Quarkus ↔ PG | PASS | Só UI; continua `POST /schedules` existente |
| III. Google + whitelist + admin | PASS | Sem alteração de auth |
| IV. Testes de API, sem E2E gate | PASS | Sem mudança de API → sem novos testes de API obrigatórios; sem E2E gate |
| V. Mobile+Web, Docker, Ubuntu | PASS | Controles usáveis em mobile/web; deploy inalterado |

**Pós-Phase 1**: PASS mantido — design não altera backend, contratos HTTP nem
introduz complexidade (broker/cache). Contrato desta feature é de UI
([contracts/ui-nova-agenda.md](./contracts/ui-nova-agenda.md)).

## Project Structure

### Documentation (this feature)

```text
specs/002-agenda-day-time-selectors/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── ui-nova-agenda.md
└── tasks.md             # (/speckit-tasks — não criado aqui)
```

### Source Code (repository root)

```text
frontend/
└── src/app/features/schedules/
    └── schedules.page.ts    # ScheduleFormPage: dias + horários (único ponto de mudança)

# Inalterado nesta feature:
backend/                     # ScheduleCreate / ScheduleService
frontend/src/app/core/api/   # createSchedule
deploy/
```

**Structure Decision**: Alterar apenas `ScheduleFormPage` no feature module de
agendas já existente. Sem novos pacotes, sem migração DB, sem OpenAPI delta.

## Complexity Tracking

> Sem violações constitutivas — tabela não aplicável.
