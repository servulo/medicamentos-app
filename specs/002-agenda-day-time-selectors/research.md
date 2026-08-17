# Research: Seletores de dias e horários na Nova Agenda

## 1. Escopo frontend-only vs. restrição no backend

**Decision**: Implementar somente na UI (`ScheduleFormPage`). Não alterar
validação de `timesOfDay` no Quarkus nem o OpenAPI de `001`.

**Rationale**: A spec é de usabilidade na Nova Agenda; agendas já persistidas
podem ter horários fora da grade horária. Restringir a API quebraria
compatibilidade e sairia do escopo (FR-008). SC-005 aplica-se a este fluxo de
criação na UI.

**Alternatives considered**:
- Validar no backend apenas horas cheias → rejeitado (compatibilidade + fora do
  escopo desta entrega).
- Endpoint novo de “slots” → desnecessário; grade é estática no cliente.

## 2. Modelo de interação dos horários

**Decision**: `<select>` (ou controle equivalente) com opções `00:00`…`23:00`;
ao escolher, adicionar à lista removível; opções já adicionadas permanecem
visíveis com `disabled`; lista removível ordenada cronologicamente; estado
inicial vazio.

**Rationale**: Alinha às clarificações (add → removable list; empty start;
disabled already-added; chronological). Select nativo funciona bem em mobile e
web sem dependência extra.

**Alternatives considered**:
- Checklist das 24 horas sempre visível → rejeitado na clarificação.
- Time picker livre / input texto → rejeitado pela spec (FR-003).
- Biblioteca de UI de chips → possível, mas desnecessária para o MVP.

## 3. Geração da lista horária

**Decision**: Constante/helper no frontend gerando 24 strings `HH:00` (00–23),
fuso apenas contextual (mensagem já existente do app); valores enviados como
hoje em `timesOfDay`.

**Rationale**: Intervalo de 60 min é regra de produto fixa; não há API de
configuração de slots.

**Alternatives considered**:
- Intervalos de 30 min → rejeitado pelo stakeholder.
- Slots configuráveis por admin → fora de escopo.

## 4. Botão “selecionar todos os dias”

**Decision**: Botão/ação que define `selectedDays = [1,2,3,4,5,6,7]` (mesma
numeração Seg=1…Dom=7 já usada). Ação idempotente se já estiverem todos
marcados. Sem botão “limpar todos”.

**Rationale**: Clarificação/assumptions; checkboxes individuais permanecem
(FR-002).

**Alternatives considered**:
- Toggle selecionar/desmarcar todos → não pedido; aumenta complexidade.
- “Todos os dias” como opção radio em vez de botão → mudaria o modelo atual de
  checkboxes sem ganho.

## 5. Validação do formulário

**Decision**: Desabilitar (ou bloquear) submit se `selectedDays.length === 0`
ou `selectedTimes.length === 0`; remover o default textual `'08:00'` do form
control de string; enviar `timesOfDay: selectedTimes` (já ordenados).

**Rationale**: FR-006, FR-009; elimina parsing por vírgula.

**Alternatives considered**:
- Manter input texto + lista → viola FR-003.
- Pré-preencher 08:00 → rejeitado na clarificação (opção A).

## 6. Testes

**Decision**: Nenhum teste de API novo obrigatório. Validação manual via
quickstart. Unit test Angular opcional para helper de sort/disable.

**Rationale**: Constitution IV exige testes de API só quando a superfície de
API muda.

**Alternatives considered**:
- E2E Playwright como gate → proibido pela constitution como gate.
