# Implementation Plan: Exclusão Completa de Medicamento, Agenda e Histórico

**Branch**: `004-med-delete-cascade` | **Date**: 2026-08-16 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/004-med-delete-cascade/spec.md`

## Summary

Substituir o soft-delete da feature 003 por **hard-delete em cascata**: ao
confirmar `DELETE /api/v1/medications/{id}`, o backend apaga em uma
transação os logs de notificação das doses, todas as doses, todas as agendas
e o próprio medicamento. Migração Flyway `V7` purga itens já `deleted_at` e
remove a coluna. UI atualiza o texto de confirmação. Testes de API passam a
exigir 404 de agendas e ausência de histórico — não mais `CANCELLED`/`SKIPPED`.

## Technical Context

**Language/Version**: Java 21 (Quarkus), TypeScript (Angular já no monorepo)

**Primary Dependencies**: Quarkus REST + Hibernate Panache + Flyway; Angular
standalone; REST Assured (`@QuarkusTest`)

**Storage**: PostgreSQL 16 — migração Flyway nova
(`V7__med_hard_delete_cascade.sql`; `V6` já usada)

**Testing**: Testes de API obrigatórios para a nova semântica do DELETE
(cascata, isolamento, segundo DELETE 404, dois medicamentos). Validação
manual via [quickstart.md](./quickstart.md). Sem E2E gate.

**Target Platform**: Web + Mobile (PWA); Docker Compose inalterado

**Project Type**: Web application (frontend + backend + database)

**Performance Goals**: Exclusão em cascata em tempo interativo (&lt;2s para
dezenas de doses/agendas do mesmo item); SC-001/SC-003 da spec

**Constraints**: Sem restore; auth/whitelist inalterados; FKs permanecem
RESTRICT (cascata só no serviço); quantidade por dose e alerta por unidade
(003) inalterados para itens restantes

**Scale/Scope**: Whitelist pequena; exclusão afeta N agendas/doses/logs do
mesmo medicamento — uma transação no backend

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Princípio | Status | Evidência no plano |
|-----------|--------|-------------------|
| I. Propósito medicamentos/lembretes | PASS | Catálogo e acompanhamento sem rastros de tratamento encerrado |
| II. Camadas Angular ↔ API ↔ Quarkus ↔ PG | PASS | Mudança em `MedicationService` + Flyway; UI só confirmação; contrato HTTP |
| III. Google + whitelist + admin | PASS | Sem alteração de auth/whitelist |
| IV. Testes de API, sem E2E gate | PASS | Atualizar/estender REST Assured de delete; sem E2E obrigatório |
| V. Mobile+Web, Docker, Ubuntu | PASS | Mesmo fluxo de exclusão na página atual; containers inalterados |

**Pós-Phase 1**: PASS mantido — design não introduz broker/cache/serviço novo;
contrato em [contracts/openapi.yaml](./contracts/openapi.yaml); complexidade
limitada a transação de delete + migração de purge + texto de confirmação.

## Project Structure

### Documentation (this feature)

```text
specs/004-med-delete-cascade/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── openapi.yaml     # delta DELETE (canônico após merge: 001 openapi)
└── tasks.md             # /speckit-tasks
```

### Source Code (repository root)

```text
backend/
├── src/main/java/app/medicamentos/
│   ├── medication/       # hard-delete transacional; remover deletedAt
│   ├── schedule/         # 404 natural se med/agenda apagados (sem mudança de regra 003)
│   ├── dose/             # histórico some com as linhas
│   └── notify/           # apagar notification_log antes das doses
├── src/main/resources/db/migration/
│   └── V7__med_hard_delete_cascade.sql
└── src/test/java/app/medicamentos/medication/
    ├── MedicationDeleteTest.java
    └── MedicationDeleteIsolationTest.java

frontend/
└── src/app/features/medications/medications.page.ts  # texto do confirm()
```

**Structure Decision**: Estender o fluxo de exclusão já existente nos módulos
`medication` / `schedule` / `dose` / `notify`. Uma migração Flyway; contrato
OpenAPI delta desta feature; UI só no confirm da lista atual, sem novos
pacotes.

## Complexity Tracking

> Sem violações constitutivas — tabela não aplicável.
