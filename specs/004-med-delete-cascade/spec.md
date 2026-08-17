# Feature Specification: Exclusão Completa de Medicamento, Agenda e Histórico

**Feature Branch**: `004-med-delete-cascade`

**Created**: 2026-08-16

**Status**: Implemented

**Input**: User description: "quando excluir um medicamento todo o histórico e agenda deve ser excluído."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Excluir medicamento remove agenda e histórico (Priority: P1)

Como usuário autorizado, quero que, ao excluir um medicamento do meu catálogo,
todas as agendas daquele item e todo o histórico de doses (tomadas, puladas e
pendentes) também desapareçam, para que o catálogo e o acompanhamento fiquem
sem rastros de um tratamento que eu não quero mais manter.

**Why this priority**: É o comportamento solicitado e substitui a regra atual
de manter histórico e agendas após a exclusão. Sem isso, o usuário continua
vendo um tratamento que decidiu apagar.

**Independent Test**: Cadastrar um medicamento com ao menos uma agenda e
doses no histórico; confirmar a exclusão; verificar que o item some do
catálogo, que nenhuma agenda daquele medicamento permanece e que o
acompanhamento deixa de listar qualquer dose daquele item.

**Acceptance Scenarios**:

1. **Given** um medicamento no catálogo do usuário autenticado, com uma ou
   mais agendas e histórico de doses, **When** o usuário confirma a exclusão,
   **Then** o medicamento deixa de existir no catálogo, todas as agendas
   daquele medicamento deixam de existir e nenhuma dose daquele medicamento
   permanece no acompanhamento.
2. **Given** um medicamento apenas com agendas (sem doses já tomadas ou
   puladas), **When** o usuário confirma a exclusão, **Then** o medicamento e
   todas as suas agendas deixam de existir e nenhum lembrete daquele item
   continua a ser enviado.
3. **Given** um medicamento apenas no catálogo (sem agenda e sem histórico),
   **When** o usuário confirma a exclusão, **Then** o medicamento deixa de
   existir no catálogo.
4. **Given** um medicamento que não pertence ao usuário autenticado,
   **When** tenta excluí-lo, **Then** a operação é negada e os dados do dono
   permanecem inalterados.

---

### User Story 2 - Confirmar exclusão irreversível (Priority: P1)

Como usuário, quero ser avisado de que a exclusão apaga de forma permanente o
medicamento, as agendas e o histórico, e poder cancelar antes de confirmar,
para não perder dados por engano.

**Why this priority**: A exclusão passa a ser irreversível e destrutiva; a
confirmação explícita é o único ponto de proteção contra perda acidental.

**Independent Test**: Iniciar a exclusão, ler o aviso de perda permanente,
cancelar e confirmar que nada foi apagado; em seguida confirmar e verificar
que tudo do item foi removido.

**Acceptance Scenarios**:

1. **Given** um medicamento com agenda e histórico, **When** o usuário inicia
   a exclusão, **Then** o sistema pede confirmação explícita informando que o
   medicamento, as agendas e o histórico serão apagados de forma permanente
   e que não será possível recuperá-los.
2. **Given** o pedido de confirmação visível, **When** o usuário cancela,
   **Then** o medicamento, as agendas e o histórico permanecem exatamente
   como estavam.
3. **Given** o pedido de confirmação visível, **When** o usuário confirma,
   **Then** a exclusão completa ocorre conforme a User Story 1.

---

### User Story 3 - Demais tratamentos permanecem intactos (Priority: P2)

Como usuário, quero que excluir um medicamento não altere os demais itens do
meu catálogo nem o histórico de outros tratamentos, para poder limpar só o
que realmente desejo apagar.

**Why this priority**: Garante que a exclusão em cascata é restrita ao item
escolhido e não corrompe o restante do acompanhamento.

**Independent Test**: Ter dois medicamentos com agendas e histórico;
excluir apenas um; verificar que o outro permanece completo no catálogo,
nas agendas e no acompanhamento.

**Acceptance Scenarios**:

1. **Given** dois medicamentos do mesmo usuário, cada um com agenda e
   histórico, **When** o usuário exclui somente o primeiro, **Then** o
   segundo permanece no catálogo com suas agendas e seu histórico
   inalterados.
2. **Given** um aviso de compra associado ao medicamento excluído,
   **When** a exclusão é confirmada, **Then** esse aviso deixa de aparecer;
   avisos de outros medicamentos permanecem.

---

### Edge Cases

- Exclusão com várias agendas do mesmo medicamento (ativas, pausadas ou
  encerradas): todas são removidas junto com o medicamento.
- Exclusão com doses pendentes (ainda não tomadas nem puladas): as doses
  pendentes são apagadas; nenhum lembrete adicional daquele medicamento é
  enviado.
- Exclusão com doses já tomadas ou puladas: o histórico dessas doses é
  apagado; o acompanhamento não as exibe mais.
- Exclusão enquanto o usuário está vendo o acompanhamento daquele
  medicamento: após a exclusão, a visão de histórico daquele item fica
  vazia ou o item deixa de estar disponível para consulta.
- Tentativa de criar nova agenda a partir de medicamento já excluído:
  impossível, porque o item não existe mais no catálogo.
- Dois usuários com medicamentos de mesmo nome: a exclusão de um não afeta
  o catálogo, as agendas nem o histórico do outro.
- Confirmação recusada ou fechada sem confirmar: nenhum dado é alterado.
- Medicamento sem estoque, sem aviso de compra ou sem lembretes pendentes:
  a exclusão completa ainda ocorre normalmente.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: O sistema MUST permitir que o dono do medicamento no catálogo
  exclua o item, após confirmação explícita.
- **FR-002**: Ao confirmar a exclusão, o sistema MUST remover de forma
  permanente o medicamento do catálogo do usuário. O item NÃO MUST
  permanecer visível em nenhuma lista de catálogo (ativa ou “excluídos”) e
  NÃO MUST poder ser restaurado.
- **FR-003**: Ao confirmar a exclusão, o sistema MUST remover de forma
  permanente **todas** as agendas vinculadas àquele medicamento, qualquer
  que seja o status (ativa, pausada ou encerrada).
- **FR-004**: Ao confirmar a exclusão, o sistema MUST remover de forma
  permanente **todo** o histórico de doses daquele medicamento, incluindo
  doses tomadas, puladas e pendentes. O acompanhamento MUST NÃO exibir
  nenhuma ocorrência daquele item após a exclusão.
- **FR-005**: Após a exclusão, o sistema MUST interromper lembretes e avisos
  de tomada daquele medicamento. Avisos de compra daquele item MUST deixar
  de aparecer.
- **FR-006**: A confirmação MUST informar, em linguagem clara, que a ação
  apaga o medicamento, as agendas e o histórico de forma permanente e
  irreversível. Cancelar a confirmação MUST deixar todos os dados
  inalterados.
- **FR-007**: A exclusão MUST afetar somente o medicamento escolhido e os
  dados vinculados a ele. Demais medicamentos, agendas e históricos do
  mesmo usuário MUST permanecer intactos.
- **FR-008**: Exclusão MUST respeitar o escopo por usuário: somente o dono
  pode excluir o próprio medicamento; tentativa sobre item de outro usuário
  MUST ser negada sem alterar dados de ninguém.
- **FR-009**: Esta regra de exclusão completa MUST substituir a regra
  anterior de manter o histórico de doses e as agendas (apenas canceladas)
  após excluir o medicamento.
- **FR-010**: A feature MUST estar disponível em Mobile e Web, no mesmo
  fluxo de exclusão de medicamento já existente no catálogo.

### Key Entities

- **Medicamento (catálogo)**: item do usuário (nome, unidade, estoque e
  limiar de compra). Ao ser excluído nesta feature, deixa de existir para o
  usuário, sem recuperação.
- **Agenda de tratamento**: plano de tomadas vinculado a um medicamento.
  Todas as agendas daquele medicamento são removidas junto com ele.
- **Dose (histórico)**: ocorrência de tomada (tomada, pulada ou pendente)
  vinculada a uma agenda/medicamento. Todas as doses daquele medicamento
  são removidas junto com ele.
- **Aviso de compra**: alerta in-app baseado no estoque do medicamento;
  desaparece quando o medicamento é excluído.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Em 100% dos casos de teste com medicamento, agenda e
  histórico, após a confirmação da exclusão o item não aparece no catálogo,
  nenhuma agenda daquele medicamento permanece e o acompanhamento não lista
  nenhuma dose daquele item.
- **SC-002**: Em 100% dos casos de teste com dois ou mais medicamentos, a
  exclusão de um deixa os demais com catálogo, agendas e histórico 100%
  inalterados.
- **SC-003**: Em teste guiado, 100% dos usuários concluem a exclusão
  (incluindo leitura do aviso e confirmação) em menos de 1 minuto.
- **SC-004**: Em 100% das tentativas em que o usuário cancela a confirmação,
  medicamento, agendas e histórico permanecem 100% iguais ao estado anterior.
- **SC-005**: Após a exclusão, 100% dos cenários de teste deixam de gerar
  lembretes de tomada e avisos de compra daquele medicamento.

## Assumptions

- Esta feature altera o comportamento da exclusão já existente no catálogo:
  deixa de preservar histórico e agendas e passa a apagar tudo o que
  pertence ao medicamento excluído.
- A exclusão é permanente e irreversível; não há lixeira nem restauração.
- Confirmação explícita continua obrigatória, agora com aviso de perda
  total (medicamento + agendas + histórico).
- Lembretes já entregues no dispositivo não podem ser “desenviados”; o
  sistema apenas deixa de enviar novos lembretes daquele item.
- Fora do escopo: exclusão em massa de vários medicamentos de uma vez,
  restauração de item excluído, exclusão de dados de outro usuário, e
  alteração das regras de quantidade por dose ou de aviso de compra (essas
  regras da feature 003 permanecem para itens que não foram excluídos).
- Autenticação Google, whitelist e isolamento por usuário permanecem
  inalterados (constitution e feature 001).
