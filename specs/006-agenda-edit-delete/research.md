# Research: Edição e Exclusão de Agendas

**Feature**: `006-agenda-edit-delete` | **Date**: 2026-08-19

## 1. Edição completa vs PATCH de status rápido

**Decision**: Um único `PATCH /schedules/{id}`. Se o corpo inclui qualquer
campo estrutural (`daysOfWeek`, `timesOfDay`, `durationType`, `maxTakenDoses`,
`quantityPerDose`), trata como **edição completa** (formulário de edição) e
aplica FR-007, FR-008a e FR-008b. Se o corpo contém **apenas** `status` (e
opcionalmente `resetTakenCount`), mantém o fluxo rápido da listagem
(pausar/reativar) sem reativar automaticamente agendas pausadas/canceladas.

**Rationale**: Evita novo endpoint ou flag artificial; a UI de edição sempre
envia o conjunto completo de campos estruturais; a listagem envia só status.

**Alternatives considered**: `PUT` separado para replace completo (duplica
validação); campo `mode: "fullEdit"` (ruído no contrato); tela de edição que
sempre força status ACTIVE no body (quebraria FR-008a quando resultado é
COMPLETED).

## 2. Purga de doses pendentes na mudança de recorrência

**Decision**: Na edição completa, se `daysOfWeek` ou `timesOfDay` mudarem
(comparação normalizada), apagar **todas** as `dose_occurrences` com
`status = PENDING` daquela agenda (incluindo snooze), após apagar
`notification_log` dessas doses. Doses `TAKEN`/`SKIPPED` permanecem.
Novas `PENDING` serão materializadas pelo `DoseScheduler` no próximo slot
compatível (comportamento existente por minuto).

**Rationale**: Alinha FR-007 e clarificação de snooze; scheduler já evita
duplicata por `(scheduleId, originalScheduledAt)`.

**Alternatives considered**: Recalcular `scheduledAt` de pendentes (complexo
e inconsistente com snooze); purgar também TAKEN/SKIPPED (viola spec).

## 3. Mudança só de duração/unidades sem alterar dias/horários

**Decision**: Alteração exclusiva de `durationType`, `maxTakenDoses` ou
`quantityPerDose` **não** dispara purga de pendentes; aplica FR-008a (recalc
status) e FR-008b (PAUSED/CANCELLED → ACTIVE na edição completa). Unidades
por dose afetam apenas tomadas futuras (já lido da agenda no `take`).

**Rationale**: FR-007 limita purga a dias/horários/padrão de recorrência;
horários pendentes válidos no padrão atual devem permanecer.

## 4. Recálculo de status (FR-008a/b)

**Decision**: Ordem na edição completa:

1. Validar e aplicar campos estruturais (`apply()` existente + checagem
   `maxTakenDoses >= takenCount` para FIXED).
2. Se status atual é `PAUSED` ou `CANCELLED` → `ACTIVE` (FR-008b).
3. Recalcular: se `FIXED_TAKEN_DOSES` e `takenCount >= maxTakenDoses` →
   `COMPLETED`; se `INDEFINITE` → nunca `COMPLETED` por limite; se estava
   `COMPLETED` e `takenCount < maxTakenDoses` → `ACTIVE`.
4. Purga pendentes se recorrência mudou (item 2 acima).

**Rationale**: Espelha clarificações da sessão 2026-08-19; reutiliza enums
existentes.

**Alternatives considered**: Zerar `takenCount` ao mudar duração (rejeitado na
clarificação); manter PAUSED ao salvar edição (rejeitado — usuário escolheu
reativação automática).

## 5. Exclusão de agenda (hard-delete escopado)

**Decision**: `DELETE /schedules/{id}` → 204. Transação:

```text
1. DELETE notification_log WHERE dose_id IN (doses da agenda)
2. DELETE dose_occurrences WHERE schedule_id AND user_id
3. DELETE treatment_schedules WHERE id AND user_id
```

Medicamento e outras agendas intactos. Sem alteração de estoque. Segundo
DELETE → 404.

**Rationale**: Mesmo padrão transacional de `MedicationService.delete` (004),
escopo reduzido a um `scheduleId`; atende FR-010/FR-011.

**Alternatives considered**: Soft-delete/CANCELLED (spec exige remoção do
histórico); marcar doses SKIPPED (004 abandonou isso para medicamento).

## 6. UI — reutilização do formulário Nova Agenda

**Decision**: Mesmo componente `ScheduleFormPage` em dois modos:

- **create**: rota `/agendas/nova` — medicamento selecionável; POST.
- **edit**: rota `/agendas/:id/editar` — GET pré-preenche; medicamento
  read-only; PATCH estrutural; botão Excluir com `confirm()` irreversível.

Listagem: botão Editar por agenda; unidades por dose somente texto; pausar/
reativar permanecem; remover input inline e `saveQuantity`.

**Rationale**: Spec FR-002/FR-004/FR-009; espelha `MedicationFormPage`
(`medicamentos/:id/editar`).

**Alternatives considered**: Componente separado só para edição (duplicação);
excluir na listagem (rejeitado na clarificação).

## 7. Migração de banco

**Decision**: Nenhuma migração Flyway nesta feature.

**Rationale**: Entidades e FKs já suportam cascata lógica no serviço; nenhum
campo novo.

**Alternatives considered**: `ON DELETE CASCADE` em FK (fora do padrão 004;
exige migração desnecessária).
