# Research: Exclusão de Medicamento, Dose na Agenda e Alerta por Unidade

**Feature**: `003-med-delete-dose-units` | **Date**: 2026-08-15

## 1. Soft-delete vs hard-delete

**Decision**: Soft-delete com coluna `deleted_at` (timestamptz nullable) em
`medications`. Listagens e criação de agenda filtram `deleted_at IS NULL`.
Sem endpoint de restore.

**Rationale**: Spec exige histórico de doses consultável e vínculo ao
medicamento; hard-delete quebraria FKs ou forçaria NULL em histórico.

**Alternatives considered**: Hard-delete com `ON DELETE SET NULL` (perde nome
no histórico); hard-delete com cópia do nome na dose (denormalização extra
desnecessária agora).

## 2. Cascata na exclusão

**Decision**: Em uma única transação: set `deleted_at`; para agendas
`ACTIVE`/`PAUSED` → `CANCELLED`; para **todas** as doses `PENDING` do
medicamento (qualquer agenda, inclusive já `COMPLETED`/`CANCELLED`) →
`SKIPPED` com `resolved_at = now`; sem decremento de estoque; scheduler deixa
de materializar/enviar push (agenda cancelada + doses não PENDING).

**Rationale**: Remediação analyze + clarificação — evita lembretes órfãos em
qualquer agenda do item excluído.

**Alternatives considered**: Deixar PENDING sem push (histórico sujo);
apagar PENDING (perde rastro).

## 3. Quantidade por dose na Agenda

**Decision**: Coluna `quantity_per_dose INTEGER NOT NULL DEFAULT 1` em
`treatment_schedules`. Remover coluna de `medications` após copiar valor
existente para todas as agendas do medicamento (`GREATEST(1, ROUND(...))`).
API: `quantityPerDose` em Schedule create/update/response; removido de
Medication create/update/response.

**Rationale**: Spec FR-005/FR-006; permite doses distintas por agenda do mesmo
item.

**Alternatives considered**: Manter no medicamento como default overrideável
(ambiguidade); snapshot na dose occurrence (over-engineering para v1).

## 4. Decremento de estoque no take

**Decision**: `DoseService.take` lê `quantityPerDose` da **agenda** da dose e
chama `decrementStock(medicationId, amount)` com esse inteiro.

**Rationale**: FR-007; alinhado ao novo modelo.

**Alternatives considered**: Copiar quantity para DoseOccurrence no
materialize (útil se agenda mudar mid-flight — spec diz tomadas futuras usam
novo valor, então ler da agenda no take é correto).

## 5. Alerta de compra por unidades

**Decision**: `purchaseNeeded = stockQuantity <= purchaseThresholdUnits`.
Renomear campo API `purchaseThresholdDoses` → `purchaseThresholdUnits`
(breaking change aceitável: app ainda em evolução whitelist). Remover
`remainingDoses` / `quantityPerDose` do payload de alerta; expor
`remainingUnits` (= `stockQuantity`) ou omitir e usar só stock + threshold.
Default e migração: **10**.

**Rationale**: Clarificações — limiar em unidades, reset para 10, inteiros.

**Alternatives considered**: Manter nome `purchaseThresholdDoses` com
semântica nova (confuso); converter limiar antigo × quantity (rejeitado pelo
usuário).

## 6. Tipos inteiros

**Decision**: `stock_quantity` e `purchase_threshold_units` como INTEGER;
estoque fracionário existente → `ROUND` para inteiro ≥ 0 na migração.

**Rationale**: Clarificação — só inteiros para stock, threshold e quantity
per dose.

**Alternatives considered**: NUMERIC com validação na API (mais complexo, fora
da decisão).

## 7. Edição de quantidade na agenda

**Decision**: Incluir `quantityPerDose` em `ScheduleUpdate` / UI de edição de
agenda (se hoje só PATCH de status, expandir form de edição mínima ou modal).
Tomadas já registradas não recalculam estoque.

**Rationale**: FR-006 + US2 cenário 4.

**Alternatives considered**: Só na criação (insuficiente vs spec).

## 8. Contrato e testes

**Decision**: OpenAPI delta nesta feature; atualizar clientes Angular em
`api.ts`; testes API para delete cascata, rejeição de med deleted em nova
agenda, take com doses distintas, alerta `stock <= threshold`, validação de
não-inteiros/fracionários.

**Rationale**: Constitution IV — mudança de API exige testes.
