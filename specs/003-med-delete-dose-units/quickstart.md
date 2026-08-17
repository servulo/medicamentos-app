# Quickstart: Exclusão, dose na Agenda, alerta por unidade

**Feature**: `003-med-delete-dose-units` | **Date**: 2026-08-15

Validação manual + API após implementar plan/tasks. Contratos:
[contracts/openapi.yaml](./contracts/openapi.yaml). Modelo:
[data-model.md](./data-model.md).

## Prerequisites

- Stack 001 no ar (`docker compose` ou dev local frontend + backend + Postgres)
- Usuário na whitelist com login Google
- Flyway `V6__med_delete_dose_units.sql` aplicada

## Setup

```bash
# a partir da raiz do repo — conforme quickstart 001
docker compose up -d --build
# ou backend Quarkus + frontend ng serve
```

Confirmar migração: medicamentos sem `quantity_per_dose`; agendas com
`quantity_per_dose`; limiares = 10; estoque inteiro.

## US1 — Excluir medicamento

1. Criar medicamento "Teste Delete" com estoque 20, limiar 10.
2. Criar agenda ACTIVE com dose PENDING (ou aguardar materialização).
3. Na UI: excluir com confirmação.
4. **Esperado**: some da lista; agendas ACTIVE/PAUSED → `CANCELLED`; doses
   `PENDING` → `SKIPPED`; histórico TAKEN/SKIPPED anterior ainda listável;
   sem novo push para essas doses.
5. API: `DELETE /api/v1/medications/{id}` → 204; segundo DELETE → 404;
   `POST /schedules` com esse `medicationId` → 404/400.
6. Confirmar que doses `PENDING` de **qualquer** agenda do med viraram
   `SKIPPED` (não só das agendas que estavam ACTIVE/PAUSED).

## US2 — Quantidade por dose na Agenda

1. Cadastro de medicamento: **sem** campo quantidade por dose.
2. Nova Agenda A: `quantityPerDose = 1`; Agenda B mesmo med: `quantityPerDose = 2`.
3. `take` em uma dose de A e uma de B (estoque inicial 20).
4. **Esperado**: estoque 20 → 19 → 17.
5. Editar quantidade por dose de uma agenda existente na lista de agendas;
   próxima take usa o novo valor; estoque passado não recalcula.
6. API rejeita `quantityPerDose: 0`, `1.5`, `-1` → 400.

## US3 — Alerta por unidade

1. Medicamento estoque 15, limiar 10 → **sem** alerta.
2. Ajustar estoque para 10 (ou take até ≤10) → alerta in-app Mobile e Web;
   **sem** push de compra.
3. Confirmar payload `GET /alerts/purchase`: `stockQuantity`,
   `purchaseThresholdUnits`; **sem** `remainingDoses` / `quantityPerDose`.
4. Novo medicamento sem limiar → default **10**.
5. Após migração de dados antigos: limiar de todos = 10.

## Regressão rápida

| Check | Esperado |
|-------|----------|
| Soft-delete | Sem restore na UI/API |
| Inteiros | Stock/limiar/quantidade rejeitam fração |
| Auth | DELETE de med de outro usuário → 404/403 |
| SC-003/SC-004 | Take por agenda; alerta só por unidades |

## API tests (dev)

```bash
cd backend
./mvnw test -Dtest=MedicationDelete*,ScheduleQuantity*,PurchaseAlert*
# nomes exatos definidos em tasks.md na implementação
```
