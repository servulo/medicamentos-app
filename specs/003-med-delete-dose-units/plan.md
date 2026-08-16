# Implementation Plan: Exclusão de Medicamento, Dose na Agenda e Alerta por Unidade

**Branch**: `003-med-delete-dose-units` | **Date**: 2026-08-15 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/003-med-delete-dose-units/spec.md`

**Note**: Argumento do comando (`pe`) interpretado como sem escopo extra — plano cobre a spec completa.

## Summary

Evoluir o domínio 001: (1) soft-delete de medicamento com cancelamento de
agendas ativas/pausadas e auto-skip de doses `PENDING`; (2) mover
`quantityPerDose` do catálogo para a Agenda; (3) alerta de compra por
comparação direta estoque ≤ limiar em **unidades inteiras** (default/migração
**10**). Backend Quarkus + Flyway + testes de API; frontend Angular (formulários
de medicamento/agenda, confirmação de exclusão, alertas).

## Technical Context

**Language/Version**: Java 21 (Quarkus), TypeScript (Angular já no monorepo)

**Primary Dependencies**: Quarkus REST + Hibernate Panache + Flyway; Angular
standalone + ReactiveForms; REST Assured (`@QuarkusTest`)

**Storage**: PostgreSQL 16 — migração Flyway nova (`V6__med_delete_dose_units.sql`; `V5` já usada)

**Testing**: Testes de API obrigatórios para DELETE medicamento, schemas
Medication/Schedule alterados, decremento de estoque via agenda, alerta por
unidade. Validação manual via [quickstart.md](./quickstart.md). Sem E2E gate.

**Target Platform**: Web + Mobile (PWA); Docker Compose inalterado

**Project Type**: Web application (frontend + backend + database)

**Performance Goals**: Exclusão com cascata (cancel + skip) concluída em
tempo interativo (&lt;2s tipicamente para dezenas de doses); SC-001/SC-005 da
spec

**Constraints**: Soft-delete sem restore; inteiros ≥0 (estoque/limiar) e ≥1
(quantidade por dose); limiar migrado sempre para 10; auth/whitelist
inalterados; aviso de compra continua só in-app

**Scale/Scope**: Whitelist pequena; exclusão afeta N agendas/doses do mesmo
medicamento — operação transacional única no backend

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Princípio | Status | Evidência no plano |
|-----------|--------|-------------------|
| I. Propósito medicamentos/lembretes | PASS | Exclusão e modelo de dose/estoque melhoram adesão e catálogo |
| II. Camadas Angular ↔ API ↔ Quarkus ↔ PG | PASS | Mudanças em `backend/` + `frontend/` via API HTTP; Flyway no backend |
| III. Google + whitelist + admin | PASS | Sem alteração de auth/whitelist |
| IV. Testes de API, sem E2E gate | PASS | Novos/atualizados testes REST Assured; sem E2E obrigatório |
| V. Mobile+Web, Docker, Ubuntu | PASS | UI Mobile+Web; containers inalterados |

**Pós-Phase 1**: PASS mantido — design não introduz broker/cache; contrato
OpenAPI versionado em [contracts/openapi.yaml](./contracts/openapi.yaml);
complexidade limitada a migração de schema + cascata de exclusão.

## Project Structure

### Documentation (this feature)

```text
specs/003-med-delete-dose-units/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── openapi.yaml     # delta / change log (canônico após merge: 001 openapi)
└── tasks.md
```

### Source Code (repository root)

```text
backend/
├── src/main/java/app/medicamentos/
│   ├── medication/       # soft-delete, ints, purchaseNeeded por unidades
│   ├── schedule/         # quantityPerDose na agenda; bloqueio se med deleted
│   └── dose/             # take usa quantity da agenda; skip em massa no delete
├── src/main/resources/db/migration/
│   └── V6__med_delete_dose_units.sql
└── src/test/java/app/medicamentos/
    ├── medication/       # delete + alert tests
    ├── schedule/         # quantityPerDose validation
    └── dose/             # stock decrement from schedule

frontend/
└── src/app/
    ├── core/api/api.ts                    # tipos + deleteMedication
    └── features/
        ├── medications/medications.page.ts  # sem quantityPerDose; delete + confirm
        ├── schedules/schedules.page.ts      # quantityPerDose create + edit
        └── alerts/alerts.page.ts            # stockQuantity + purchaseThresholdUnits
```

**Structure Decision**: Estender módulos `medication`, `schedule` e `dose` já
existentes (feature 001). Uma migração Flyway; contrato OpenAPI delta desta
feature; UI nos forms/listas atuais sem novos pacotes.

## Complexity Tracking

> Sem violações constitutivas — tabela não aplicável.
