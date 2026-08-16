# Quickstart: Validar seletores da Nova Agenda

Validação manual da UI (sem suite E2E obrigatória). API e Docker como em
`specs/001-medication-control/quickstart.md`.

## Pré-requisitos

- App no ar (Compose ou dev local) com usuário whitelist autenticado
- Pelo menos um medicamento no catálogo

## Cenário A — Todos os dias + um horário da lista

1. Abrir **Nova agenda** (`/agendas/nova`).
2. Confirmar: **não** há campo de texto livre “horários separados por vírgula”.
3. Confirmar: lista removível de horários está **vazia**.
4. Acionar **Selecionar todos os dias** → Seg–Dom marcados.
5. No seletor, escolher `08:00` → aparece na lista removível; seletor reseta.
6. Abrir o seletor de novo → `08:00` visível porém **desabilitado**.
7. Escolher `20:00` → lista removível mostra `08:00`, `20:00` (ordem cronológica).
8. Selecionar medicamento e duração contínua; salvar.
9. **Esperado**: agenda listada com todos os dias e horários `08:00`, `20:00`.

## Cenário B — Ordem cronológica independente da adição

1. Nova agenda; adicionar `20:00`, depois `08:00`.
2. **Esperado**: lista removível exibe `08:00` depois `20:00`.

## Cenário C — Remoção e bloqueio de save

1. Adicionar `08:00`; remover da lista removível.
2. Tentar salvar sem horários (com medicamento e ≥1 dia).
3. **Esperado**: salvamento bloqueado **e** mensagem visível pedindo horário.
4. Desmarcar todos os dias (mesmo com horário); tentar salvar.
5. **Esperado**: salvamento bloqueado **e** mensagem visível pedindo dia.

## Cenário D — Selecionar todos idempotente

1. Marcar todos os dias manualmente ou pelo botão; acionar o botão de novo.
2. **Esperado**: os sete dias permanecem selecionados.

## Cenário E — Reset do seletor após adicionar

1. Adicionar `08:00` pelo seletor.
2. **Esperado**: seletor volta a vazio/placeholder (não fica preso em `08:00`).
3. Adicionar `20:00` em seguida sem limpar o controle manualmente.
4. **Esperado**: ambos na lista removível.

## Referências

- Contrato UI: [contracts/ui-nova-agenda.md](./contracts/ui-nova-agenda.md)
- Modelo de draft: [data-model.md](./data-model.md)
- API inalterada: `ScheduleCreate` em `../001-medication-control/contracts/openapi.yaml`
