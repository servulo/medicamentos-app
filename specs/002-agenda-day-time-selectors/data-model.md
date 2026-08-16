# Data Model: Seletores de dias e horários na Nova Agenda

Sem alteração de entidades persistidas. O modelo abaixo é o **estado de
rascunho na UI** antes de `POST /schedules`. Persistência continua conforme
[data-model.md de 001](../001-medication-control/data-model.md) (`schedules`,
`times_of_day`, `days_of_week`).

## Client draft: Nova Agenda

| Campo | Tipo (conceitual) | Regras |
|-------|-------------------|--------|
| medicationId | UUID string | Obrigatório; do catálogo |
| daysOfWeek | inteiros 1–7 | ≥1 para salvar; botão “selecionar todos” → `{1,2,3,4,5,6,7}` |
| timesOfDay | strings `HH:00` | ≥1 para salvar; só valores da grade 00:00–23:00; únicos; ordem cronológica na UI |
| durationType | `INDEFINITE` \| `FIXED_TAKEN_DOSES` | Inalterado (001) |
| maxTakenDoses | inteiro &gt; 0 | Só se FIXED; inalterado |

### Grade horária (catálogo de opções na UI)

- 24 valores: `00:00`, `01:00`, …, `23:00`
- Não persistidos como entidade; gerados no cliente

### Relacionamentos

- Draft → ao salvar, mapeia 1:1 para `ScheduleCreate` existente
- Sem novos estados de ciclo de vida de agenda

### Validação (UI)

| Regra | Origem |
|-------|--------|
| Sem texto livre de horário | FR-003 |
| Lista removível inicia vazia | FR-009 |
| Horário já adicionado: visível e não selecionável no seletor | FR-010 |
| Exibição cronológica na lista removível | FR-011 |
| ≥1 dia e ≥1 horário para criar | FR-006 |
| Sem duplicata de horário | Edge case / FR-004 |

### Transições

Nenhuma transição de status nova. Fluxo: draft vazio → usuário edita dias/horários
→ submit → agenda `ACTIVE` (comportamento 001).
