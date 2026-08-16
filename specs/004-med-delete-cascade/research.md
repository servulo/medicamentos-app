# Research: Exclusão Completa de Medicamento, Agenda e Histórico

**Feature**: `004-med-delete-cascade` | **Date**: 2026-08-16

## 1. Soft-delete vs hard-delete

**Decision**: Hard-delete permanente da linha em `medications`. O item deixa
de existir; não há lista de “excluídos”, lixeira nem restore.

**Rationale**: A spec (FR-002, FR-009) substitui a regra da feature 003 de
preservar o registro para histórico. Com histórico e agendas também apagados,
manter `deleted_at` não entrega valor ao usuário e só complica consultas.

**Alternatives considered**: Manter soft-delete e apagar só doses/agendas
(órfão no catálogo); soft-delete + flag “purged” (complexidade sem benefício).

## 2. Ordem da cascata e `notification_log`

**Decision**: Uma transação no serviço, na ordem imposta pelas FKs atuais
(RESTRICT, sem alterar para `ON DELETE CASCADE`):

1. Apagar `notification_log` das doses daquele `medication_id`
2. Apagar `dose_occurrences` daquele `medication_id`
3. Apagar `treatment_schedules` daquele `medication_id`
4. Apagar a linha em `medications`

Escopo sempre `user_id` do dono autenticado. Isolamento: 404 se o item não
é do usuário (igual ao DELETE atual).

**Rationale**: `notification_log.dose_id` referencia `dose_occurrences`;
doses e agendas referenciam o medicamento. Cascata explícita no serviço é o
padrão já usado (cancel/skip) e permanece testável sem mudar o schema de FKs.

**Alternatives considered**: `ON DELETE CASCADE` no Postgres (menos código,
mais risco de delete acidental via ferramenta); Panache `@OneToMany(cascade)`
(as entidades hoje usam UUIDs soltos, não associações JPA).

## 3. Dados já soft-deleted (feature 003)

**Decision**: Migração Flyway `V7__med_hard_delete_cascade.sql` faz o mesmo
purge para linhas com `deleted_at IS NOT NULL` e em seguida remove a coluna
`deleted_at`.

**Rationale**: FR-002 exige que o item não permaneça em nenhuma lista; deixar
fantasmas 003 quebraria o acompanhamento (histórico ainda visível). Uma
migração única alinha o banco ao novo contrato.

**Alternatives considered**: Preservar soft-deleted antigos (inconsistente com
a spec); job manual (esquecível).

## 4. Coluna `deleted_at` e `getIncludingDeleted`

**Decision**: Dropar `deleted_at`. Remover campo/método na entidade, filtros
`deletedAt is null` e o método `getIncludingDeleted` (hoje não é chamado).
`GET/PATCH/list` passam a tratar “não existe” de forma uniforme (404).

**Rationale**: Sem soft-delete, o filtro e o getter são código morto. Dose
history resolve o nome via `MedicationEntity.findById`; após hard-delete as
doses já não existem.

**Alternatives considered**: Manter coluna sempre null (dívida).

## 5. Contrato HTTP

**Decision**: Mesmo `DELETE /api/v1/medications/{medicationId}`: **204** se
o dono confirma via cliente; **404** se inexistente, de outro usuário ou já
apagado (segundo DELETE). Sem corpo. Sem endpoint de restore. Listagens de
agendas/doses/alertas simplesmente deixam de conter o item (não há 410).

**Rationale**: A superfície da API não muda; só a semântica. Constitution II
pede contrato explícito da breaking change de comportamento.

**Alternatives considered**: `DELETE` com query `?purge=true` (dois modos —
fora da spec); 200 com payload do que foi apagado (desnecessário).

## 6. Lembretes e aviso de compra

**Decision**: Sem mudança no scheduler nem no endpoint de alertas. Sem doses
`PENDING` daquele medicamento, o job não envia push; sem linha no catálogo,
`GET /alerts/purchase` não lista o item. Pushes já entregues no aparelho não
são revogados (assumption da spec).

**Rationale**: FR-005 é consequência da remoção dos dados, não de um canal
novo.

**Alternatives considered**: Flag “suppressed” no log de notificação (overkill).

## 7. Confirmação na UI

**Decision**: Atualizar o `confirm()` já existente na lista de medicamentos
para avisar perda permanente de medicamento, agendas e histórico. Cancelar
não chama a API. Mesmo fluxo em Mobile e Web (página atual).

**Rationale**: FR-006; o ponto de confirmação já existe — só o texto está
desatualizado (menciona cancelar agendas e pular doses).

**Alternatives considered**: Modal Angular dedicado (sem ganho nesta fase);
checkbox “entendo que é irreversível” (atrito extra não pedido).

## 8. Testes de API

**Decision**: Atualizar `MedicationDeleteTest` (após DELETE: medicamento 404,
agenda 404, doses daquele item ausentes — não mais `CANCELLED`/`SKIPPED`).
Estender isolamento e adicionar caso de dois medicamentos (SC-002). Sem E2E.

**Rationale**: Constitution IV — mudança de comportamento de endpoint exige
testes de API.

**Alternatives considered**: Só testes de persistência unitários (não cobrem
o contrato HTTP).
