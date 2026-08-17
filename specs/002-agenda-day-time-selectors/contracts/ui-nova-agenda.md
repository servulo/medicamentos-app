# UI Contract: Nova Agenda — dias e horários

**Scope**: Tela de criação `/agendas/nova` (`ScheduleFormPage`).  
**HTTP**: Sem alteração — continua `POST /api/v1/schedules` com
`ScheduleCreate` de `specs/001-medication-control/contracts/openapi.yaml`.

## Controles obrigatórios

### Dias da semana

| Elemento | Comportamento |
|----------|----------------|
| Checkboxes Seg–Dom | Toggle individual; valores 1–7 |
| Ação “Selecionar todos os dias” | Define todos os sete dias selecionados; idempotente se já completos |
| Submit sem dias | Bloqueado; mensagem visível pedindo ao menos um dia |

### Horários

| Elemento | Comportamento |
|----------|----------------|
| Seletor de adição | Lista `00:00`…`23:00` (passo 60 min); **não** é input de texto livre |
| Opção já na lista removível | Visível e **desabilitada** até remoção |
| Escolher opção habilitada | Adiciona à lista removível; seletor reseta em seguida |
| Lista removível | Mostra horários escolhidos; cada item tem ação de remover |
| Ordem da lista removível | Sempre cronológica (cedo → tarde) |
| Estado inicial | Lista removível **vazia** |
| Submit sem horários | Bloqueado; mensagem visível pedindo ao menos um horário |

## Payload ao salvar (inalterado semanticamente)

```json
{
  "medicationId": "<uuid>",
  "daysOfWeek": [1, 2, 3, 4, 5, 6, 7],
  "timesOfDay": ["08:00", "20:00"],
  "durationType": "INDEFINITE"
}
```

- `timesOfDay`: apenas strings da grade horária desta tela
- `daysOfWeek`: subconjunto ou os sete dias após “selecionar todos”

## Fora de escopo deste contrato

- Tela de edição de agenda
- Mudança de OpenAPI / validação server-side de “hora cheia”
- Botão “limpar todos os dias”
