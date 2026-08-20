# Feature Specification: Edição e Exclusão de Agendas

**Feature Branch**: `006-agenda-edit-delete`

**Created**: 2026-08-19

**Status**: Ready for planning

**Input**: User description: "permitir que as agendas sejam editadas. Para isso, incluir um botão de editar na visão das agendas e abrir a tela de edição, similar ou igual à tela de cadastramento. A alteração das unidades por dose não poderá ser feito sem clicar no botão de editar. Permitir que agendas sejam excluídas e, com isso, seja excluído o seu histórico."

## Clarifications

### Session 2026-08-19

- Q: Ao editar uma agenda e mudar o tipo de duração, o que deve acontecer com takenCount e status? → A: Manter takenCount; recalcular status automaticamente (ex.: COMPLETED → ACTIVE se novo limite > takenCount; ACTIVE → COMPLETED se takenCount já atingiu o novo limite).
- Q: Onde o botão "Excluir" deve ficar disponível? → A: Apenas na tela de edição da agenda.
- Q: Ao editar dias/horários, o que acontece com doses pendentes adiadas (snooze)? → A: Remover todas as PENDING (incluindo adiadas) e regenerar conforme o novo padrão.
- Q: Ao salvar edição de agenda pausada, deve permanecer pausada ou reativar? → A: Passa automaticamente para ACTIVE ao salvar qualquer edição.
- Q: Ao salvar edição de agenda cancelada, deve reativar automaticamente? → A: Passa para ACTIVE ao salvar (salvo recálculo para COMPLETED por FR-008a).

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Editar agenda pela tela dedicada (Priority: P1)

Como usuário autorizado, quero abrir uma agenda existente em uma tela de
edição semelhante à de cadastro, para ajustar dias, horários, duração e
unidades por dose sem recriar a agenda do zero.

**Why this priority**: É o fluxo central solicitado; sem edição estruturada o
usuário precisa cancelar e recadastrar, perdendo histórico ou cometendo erros.

**Independent Test**: Na lista de agendas, acionar "Editar", alterar ao menos
dois campos (ex.: horários e unidades por dose), salvar e confirmar que a
agenda reflete as mudanças na listagem.

**Acceptance Scenarios**:

1. **Given** o usuário está na lista de agendas com ao menos uma agenda,
   **When** aciona o botão "Editar" dessa agenda, **Then** abre a tela de
   edição com os dados atuais preenchidos (medicamento, unidades por dose,
   dias da semana, horários, tipo de duração e limite de doses, quando
   aplicável).
2. **Given** o usuário está na tela de edição, **When** altera dias, horários,
   duração ou unidades por dose e confirma, **Then** a agenda é atualizada e
   o usuário retorna à lista (ou vê confirmação equivalente) com os novos
   valores visíveis.
3. **Given** o usuário está na tela de edição, **When** tenta salvar sem ao
   menos um dia ou sem ao menos um horário, **Then** o sistema impede o
   salvamento e exibe mensagem clara, com as mesmas regras da tela Nova
   Agenda.
4. **Given** o usuário está na tela de edição, **When** cancela ou volta sem
   salvar, **Then** nenhuma alteração é persistida.
5. **Given** uma agenda existente, **When** o usuário está apenas na lista de
   agendas (fora da tela de edição), **Then** não há campo editável de
   unidades por dose na listagem — o valor é exibido somente leitura.

---

### User Story 2 - Excluir agenda e seu histórico (Priority: P1)

Como usuário autorizado, quero excluir uma agenda que não uso mais e ter
certeza de que todo o histórico de doses daquela agenda também desaparece,
para manter o acompanhamento limpo sem apagar o medicamento do catálogo.

**Why this priority**: Complementa a edição com remoção definitiva de planos
de tratamento obsoletos; hoje não há exclusão isolada de agenda.

**Independent Test**: Criar agenda com doses tomadas, puladas e pendentes;
excluir a agenda após confirmação; verificar que ela some da lista e que
nenhuma dose daquela agenda permanece no acompanhamento.

**Acceptance Scenarios**:

1. **Given** uma agenda com histórico de doses (tomadas, puladas e/ou
   pendentes), **When** o usuário confirma a exclusão, **Then** a agenda
   deixa de existir e todas as doses vinculadas a ela deixam de aparecer no
   acompanhamento.
2. **Given** um medicamento com duas ou mais agendas, **When** o usuário
   exclui apenas uma delas, **Then** as demais agendas e seus históricos
   permanecem intactos e o medicamento continua no catálogo.
3. **Given** uma agenda com doses pendentes e lembretes futuros, **When** a
   exclusão é confirmada, **Then** nenhum lembrete futuro é enviado para
   doses daquela agenda.
4. **Given** uma agenda que não pertence ao usuário autenticado, **When**
   tenta excluí-la, **Then** a operação é negada.

---

### User Story 3 - Confirmar exclusão irreversível (Priority: P2)

Como usuário, quero ser avisado de que excluir uma agenda apaga
permanentemente ela e todo o seu histórico, e poder cancelar antes de
confirmar, para evitar perda acidental de dados.

**Why this priority**: A exclusão é destrutiva; a confirmação explícita é a
principal salvaguarda contra erro.

**Independent Test**: Iniciar exclusão, cancelar e verificar que nada mudou;
repetir confirmando e verificar remoção completa.

**Acceptance Scenarios**:

1. **Given** o usuário inicia a exclusão de uma agenda, **When** o pedido de
   confirmação é exibido, **Then** o texto informa que a agenda e todo o seu
   histórico serão apagados de forma permanente e não poderão ser recuperados.
   *(A exclusão só pode ser iniciada a partir da tela de edição.)*
2. **Given** o pedido de confirmação visível, **When** o usuário cancela,
   **Then** a agenda e o histórico permanecem inalterados.
3. **Given** o pedido de confirmação visível, **When** o usuário confirma,
   **Then** a exclusão ocorre conforme a User Story 2.

---

### Edge Cases

- Edição de agenda `COMPLETED`: permitida; alterações de configuração são
  salvas; status recalculado conforme FR-008a.
- Edição de agenda `CANCELLED`: ao salvar qualquer alteração, o status passa
  automaticamente para `ACTIVE` (salvo se FR-008a recalcular para
  `COMPLETED`).
- Edição de agenda `PAUSED`: ao salvar qualquer alteração, o status passa
  automaticamente para `ACTIVE` e lembretes voltam a ser gerados (salvo se
  FR-008a recalcular para `COMPLETED`).
- Edição que reduz `maxTakenDoses` abaixo de `takenCount` em agenda com limite
  fixo: o sistema impede o salvamento e informa o conflito de forma clara.
- Mudança de tipo de duração na edição: `takenCount` é preservado; o status é
  recalculado automaticamente — ex.: agenda `COMPLETED` cujo novo
  `maxTakenDoses` fica maior que `takenCount` passa a `ACTIVE`; agenda
  `ACTIVE` cujo `takenCount` já atinge o novo `maxTakenDoses` passa a
  `COMPLETED`; mudança para uso contínuo remove o limite sem zerar
  `takenCount`.
- Exclusão de agenda sem nenhuma dose registrada: remove a agenda
  imediatamente após confirmação, sem efeitos colaterais em outras agendas.
- Exclusão de agenda com doses já tomadas: o estoque do medicamento não é
  revertido (as unidades já consumidas permanecem debitadas).
- Tentativa de editar ou excluir agenda inexistente ou de outro usuário:
  operação negada com resposta equivalente a "não encontrado".
- Medicamento da agenda permanece somente leitura na edição (não é possível
  transferir a agenda para outro medicamento por esta tela).
- Edição de dias/horários com doses pendentes adiadas: todas as doses
  `PENDING` (incluindo snooze) são removidas e substituídas por novas
  ocorrências conforme o padrão atualizado; nenhum lembrete permanece no
  horário adiado anterior.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: O sistema MUST exibir, na visão de listagem de agendas, um
  botão ou ação "Editar" por agenda que navegue para a tela de edição
  daquela agenda.
- **FR-002**: A tela de edição MUST reutilizar a mesma estrutura e validações
  da tela Nova Agenda (seleção de dias com "selecionar todos", lista de
  horários por seleção, duração contínua ou limitada a N doses tomadas,
  unidades por dose inteiras ≥ 1).
- **FR-003**: Ao abrir a edição, o formulário MUST vir pré-preenchido com os
  valores atuais da agenda selecionada.
- **FR-004**: O medicamento associado à agenda MUST ser exibido na edição em
  modo somente leitura (sem permitir troca de medicamento neste fluxo).
- **FR-005**: Na listagem de agendas, unidades por dose MUST ser exibidas
  apenas para leitura; MUST NOT existir campo inline editável fora da tela
  de edição.
- **FR-006**: O sistema MUST persistir alterações de dias, horários, tipo de
  duração, limite de doses tomadas (quando aplicável) e unidades por dose
  ao salvar a edição.
- **FR-007**: Após salvar uma edição que altere dias, horários ou padrão de
  recorrência, o sistema MUST remover **todas** as doses futuras pendentes
  (incluindo as adiadas por snooze) que não se aplicam mais ao novo padrão;
  doses já tomadas ou puladas MUST permanecer no histórico. Novas ocorrências
  futuras MUST ser geradas pelo mecanismo de lembretes já existente no próximo
  horário compatível com a agenda atualizada (não no instante do salvamento).
- **FR-008**: Alteração de unidades por dose MUST aplicar-se apenas a tomadas
  futuras; doses já registradas como tomadas MUST NOT ter consumo de estoque
  recalculado retroativamente.
- **FR-008a**: Ao alterar o tipo de duração na edição, o sistema MUST
  preservar `takenCount` e MUST recalcular o status da agenda: se
  `FIXED_TAKEN_DOSES` e `takenCount >= maxTakenDoses`, status MUST ser
  `COMPLETED`; se `takenCount < maxTakenDoses` e a agenda estava `COMPLETED`,
  status MUST voltar a `ACTIVE`; mudança para `INDEFINITE` MUST remover o
  limite sem zerar `takenCount`.
- **FR-008b**: Ao salvar edição de agenda com status `PAUSED` ou `CANCELLED`,
  o sistema MUST definir status como `ACTIVE` antes de aplicar demais
  recálculos (FR-008a), salvo se o recálculo resultar em `COMPLETED`.
- **FR-009**: O sistema MUST oferecer ação "Excluir" exclusivamente na tela de
  edição da agenda, exigindo confirmação explícita antes de executar; a
  listagem de agendas MUST NOT expor exclusão direta.
- **FR-010**: Ao confirmar exclusão, o sistema MUST remover permanentemente a
  agenda e todas as doses (pendentes, tomadas e puladas) vinculadas a ela.
- **FR-011**: A exclusão de agenda MUST NOT remover o medicamento do catálogo
  nem afetar outras agendas do mesmo medicamento.
- **FR-012**: Apenas o proprietário autenticado da agenda MUST poder editá-la
  ou excluí-la.
- **FR-013**: Ações rápidas já existentes na listagem (ex.: pausar/reativar)
  MAY permanecer disponíveis fora da tela de edição, desde que unidades por
  dose não sejam editáveis inline.

### Key Entities

- **Agenda** (`TreatmentSchedule`): plano de tomadas de um medicamento;
  atributos editáveis nesta feature: dias da semana, horários, tipo de
  duração, limite de doses tomadas, unidades por dose; medicamento fixo após
  criação.
- **Dose** (`DoseOccurrence`): ocorrência agendada vinculada a uma agenda;
  excluída em cascata quando a agenda é excluída; regenerada parcialmente
  quando a recorrência da agenda é alterada na edição.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Usuários conseguem localizar e abrir a edição de uma agenda a
  partir da listagem em até 2 interações (localizar agenda + acionar Editar).
- **SC-002**: 100% das validações da Nova Agenda (dias, horários, duração,
  unidades por dose) se aplicam também na edição, com mensagens de erro
  equivalentes.
- **SC-003**: Após exclusão confirmada, nenhuma dose da agenda excluída
  permanece visível no acompanhamento (0 ocorrências residuais).
- **SC-004**: Exclusão de uma agenda entre várias do mesmo medicamento não
  altera a contagem nem o histórico das agendas restantes.
- **SC-005**: Usuários completam uma edição típica (alterar horário e
  unidades por dose) em menos de 2 minutos, sem recadastrar a agenda.

## Assumptions

- A tela de edição reutiliza o mesmo layout e controles da Nova Agenda
  (feature `002-agenda-day-time-selectors`), adaptados para modo edição.
- O medicamento da agenda não pode ser alterado na edição, para evitar
  inconsistência com histórico de doses já registradas sob outro item.
- Pausar permanece como ação rápida na listagem; ao salvar edição de agenda
  pausada ou cancelada, o status passa automaticamente para `ACTIVE` (salvo
  recálculo para `COMPLETED`).
- Exclusão de agenda segue o mesmo princípio de irreversibilidade já adotado
  na exclusão de medicamento (`004-med-delete-cascade`), porém limitada ao
  escopo de uma única agenda.
- Consumo de estoque já registrado por doses tomadas não é estornado na
  exclusão da agenda.
- Autenticação e whitelist existentes (`001-medication-control`) aplicam-se
  a todas as novas operações de edição e exclusão.
