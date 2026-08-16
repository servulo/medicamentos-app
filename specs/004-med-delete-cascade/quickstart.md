# Quickstart: Exclusão completa de medicamento, agenda e histórico

**Feature**: `004-med-delete-cascade` | **Date**: 2026-08-16

Validação manual + API após implementar plan/tasks. Contratos:
[contracts/openapi.yaml](./contracts/openapi.yaml). Modelo:
[data-model.md](./data-model.md).

## Prerequisites

- Stack no ar (`docker compose` em `deploy/` ou Quarkus + `ng serve` + Postgres)
- Usuário na whitelist com login Google
- Flyway `V7__med_hard_delete_cascade.sql` aplicada (`deleted_at` removida)

## Setup

```bash
# a partir da raiz do repo — conforme quickstart 001
docker compose -f deploy/docker-compose.yml up -d --build
# ou backend Quarkus + frontend ng serve
```

Confirmar migração: coluna `medications.deleted_at` inexistente; medicamentos
que estavam soft-deleted (e suas agendas/doses) já não existem.

## US1 — Excluir remove catálogo, agendas e histórico

1. Criar medicamento "Teste Delete" com estoque 20, limiar 10.
2. Criar ao menos uma agenda; garantir doses (PENDING e, se possível, TAKEN
   ou SKIPPED).
3. Na UI: Excluir → ler o aviso de perda permanente → confirmar.
4. **Esperado**: some da lista do catálogo; `GET` do medicamento → 404;
   agendas daquele item → 404 / ausentes na lista; acompanhamento sem
   nenhuma dose daquele medicamento; sem novos lembretes daquele item.
5. API: `DELETE /api/v1/medications/{id}` → 204; segundo DELETE → 404;
   `POST /api/v1/schedules` com esse `medicationId` → 404.

## US2 — Confirmação irreversível

1. Iniciar exclusão de um medicamento com agenda e histórico.
2. **Esperado**: o diálogo informa que medicamento, agendas e histórico
   serão apagados de forma permanente.
3. Cancelar o diálogo → dados intactos (catálogo, agendas, doses).
4. Confirmar → mesmo resultado da US1.

## US3 — Demais tratamentos intactos

1. Dois medicamentos, cada um com agenda e histórico.
2. Excluir somente o primeiro.
3. **Esperado**: o segundo permanece no catálogo, com agendas e doses;
   aviso de compra do primeiro some; avisos do segundo (se houver)
   permanecem.

## Regressão rápida

| Check | Esperado |
|-------|----------|
| Isolamento | DELETE de med de outro usuário → 404; dados do dono intactos |
| Sem restore | Não há ação/tela para recuperar o item |
| Quantidade por dose / alerta | Regras da 003 inalteradas para itens não excluídos |
| Auth | Login Google + whitelist inalterados |

## API tests (dev)

```bash
cd backend
./mvnw test -Dtest=MedicationDelete*,MedicationDeleteIsolation*
```
