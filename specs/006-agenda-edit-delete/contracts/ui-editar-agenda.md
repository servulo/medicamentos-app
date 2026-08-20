# UI Contract: Editar Agenda

**Scope**: Tela `/agendas/:id/editar` (`ScheduleFormPage` modo edição).  
**HTTP**: `GET /api/v1/schedules/{id}` (pré-preencher); `PATCH` estrutural;
`DELETE` (exclusão).

## Navegação

| Origem | Ação | Destino |
|--------|------|---------|
| Lista `/agendas` | Botão "Editar" por agenda | `/agendas/{id}/editar` |
| Edição | "Voltar" / cancelar | `/agendas` (sem persistir) |
| Edição | Salvar com sucesso | `/agendas` |
| Edição | Excluir confirmado | `/agendas` |

## Lista de agendas (`SchedulesPage`)

| Elemento | Comportamento |
|----------|----------------|
| Unidades por dose | **Somente leitura** (texto); sem `<input>` inline |
| Botão "Editar" | Visível por agenda; navega para edição |
| Pausar / Reativar | Permanecem (PATCH status-only) |
| Excluir | **Ausente** na listagem (FR-009) |

## Formulário de edição

Reutiliza controles de [ui-nova-agenda.md](../../002-agenda-day-time-selectors/contracts/ui-nova-agenda.md):

| Campo | Modo edição |
|-------|-------------|
| Medicamento | **Somente leitura** (nome exibido; sem `<select>`) |
| Unidades por dose | Editável (inteiro ≥ 1) |
| Dias da semana | Checkboxes + "Selecionar todos" |
| Horários | Seletor + lista removível (mesmas regras 002) |
| Duração | `INDEFINITE` \| `FIXED_TAKEN_DOSES` + max doses |
| Submit | "Salvar alterações" (desabilitado se inválido) |
| Excluir | Botão destrutivo; **somente nesta tela** |

### Pré-preenchimento

- Ao abrir, `GET /schedules/{id}` popula todos os campos.
- Horários na lista removível em ordem cronológica.
- Dias marcados conforme `daysOfWeek`.

### Validações (iguais à Nova Agenda)

- Ao menos um dia; ao menos um horário; mensagens visíveis (não só botão desabilitado).
- `quantityPerDose` inteiro ≥ 1.
- Se FIXED: `maxTakenDoses` ≥ 1.

### Exclusão (confirmação)

Texto do `confirm()` MUST informar que a agenda e **todo o histórico de
doses** serão apagados permanentemente e não poderão ser recuperados.

- Cancelar → nenhuma chamada `DELETE`.
- Confirmar → `DELETE /schedules/{id}` → navegar para `/agendas`.

## Payload PATCH (edição completa)

```json
{
  "daysOfWeek": [1, 3, 5],
  "timesOfDay": ["08:00", "20:00"],
  "durationType": "FIXED_TAKEN_DOSES",
  "maxTakenDoses": 30,
  "quantityPerDose": 2
}
```

- Não enviar `medicationId`.
- Não enviar `status` na edição estrutural (servidor recalcula FR-008a/b).

## Fora de escopo deste contrato

- Edição inline na listagem
- Troca de medicamento da agenda
- Restore de agenda excluída
