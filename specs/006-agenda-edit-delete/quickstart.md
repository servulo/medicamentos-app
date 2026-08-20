# Quickstart: Edição e Exclusão de Agendas

**Feature**: `006-agenda-edit-delete` | **Date**: 2026-08-19

Validação manual + API após implementar plan/tasks. Contratos:
[contracts/openapi.yaml](./contracts/openapi.yaml),
[contracts/ui-editar-agenda.md](./contracts/ui-editar-agenda.md). Modelo:
[data-model.md](./data-model.md).

## Prerequisites

- Stack no ar (`docker compose` em `deploy/` ou Quarkus + `ng serve` + Postgres)
- Usuário na whitelist com login Google
- Medicamento no catálogo para vincular agendas

## Setup

```bash
# a partir da raiz do repo — conforme quickstart 001
docker compose -f deploy/docker-compose.yml up -d --build
# ou backend Quarkus + frontend ng serve
```

## US1 — Editar agenda pela tela dedicada

1. Criar medicamento e agenda (ex.: Seg/Qua/Sex 08:00, 2 unidades/dose).
2. Na lista `/agendas`, confirmar que unidades por dose aparecem **sem**
   campo editável inline.
3. Clicar **Editar** → tela pré-preenchida; medicamento somente leitura.
4. Alterar horário para 09:00 e unidades para 3 → Salvar.
5. **Esperado**: lista mostra novos valores; PATCH retorna 200; doses
   pendentes antigas removidas se recorrência mudou; TAKEN/SKIPPED
   preservados.

### Regressão validação

- Salvar sem dias ou sem horários → bloqueado com mensagem.
- Cancelar/voltar → nenhuma alteração persistida.

## US1b — Reativar ao editar pausada/cancelada

1. Pausar agenda na listagem (PATCH status-only).
2. Editar e salvar qualquer campo estrutural.
3. **Esperado**: status `ACTIVE` (salvo se `takenCount >= maxTakenDoses` →
   `COMPLETED`).

## US2 — Excluir agenda e histórico

1. Agenda com doses TAKEN, SKIPPED e PENDING.
2. Abrir edição → **Excluir** → confirmar aviso irreversível.
3. **Esperado**: agenda some da lista; `GET /schedules/{id}` → 404;
   acompanhamento sem doses daquela agenda; medicamento permanece no catálogo.
4. API: `DELETE /api/v1/schedules/{id}` → 204; segundo DELETE → 404.

## US3 — Confirmação irreversível

1. Iniciar exclusão na tela de edição.
2. **Esperado**: diálogo menciona perda permanente de agenda e histórico.
3. Cancelar → dados intactos.
4. Confirmar → mesmo resultado da US2.

## US4 — Isolamento entre agendas

1. Mesmo medicamento com duas agendas distintas, cada uma com histórico.
2. Excluir apenas a primeira.
3. **Esperado**: segunda agenda e seu histórico intactos; medicamento no
   catálogo.

## Regressão rápida

| Check | Esperado |
|-------|----------|
| Pausar na listagem | PATCH só `status`; agenda permanece pausada até editar ou reativar |
| maxTakenDoses &lt; takenCount | PATCH edição → 400 |
| Outro usuário | PATCH/DELETE → 404 |
| Estoque | Exclusão de agenda não repõe unidades já consumidas |
| Delete medicamento (004) | Continua apagando todas agendas do item |

## API tests (dev)

```bash
cd backend
./mvnw test -Dtest=ScheduleEdit*,ScheduleDelete*
```

Cenários mínimos nos testes:

- Edição completa altera campos e recalcula status
- PAUSED → save edit → ACTIVE
- Mudança de horários purga PENDING (incl. snoozed)
- DELETE remove agenda + doses; sibling schedule intacto
- Isolamento entre usuários
