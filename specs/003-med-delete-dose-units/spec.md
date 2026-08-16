# Feature Specification: Exclusão de Medicamento, Dose na Agenda e Alerta por Unidade

**Feature Branch**: `003-med-delete-dose-units`

**Created**: 2026-08-15

**Status**: Implemented

**Input**: User description: "Permitir a exclusão de medicamentos. A quantidade por dose de um medicamento é definido na Agenda e não no cadastro do medicamento. O alerta de compra é por unidade do medicamento e não por dose."

## Clarifications

### Session 2026-08-15

- Q: When a medication is deleted and its active or paused schedules become cancelled, what should happen to dose occurrences that are still pending (not yet taken or skipped)? → A: Mark all pending doses as skipped right away; no more reminders for them
- Q: After a medication is soft-deleted, can the user restore it to the active catalog later? → A: No — once deleted, it stays out of the active catalog (history only)
- Q: For medications that already have a purchase threshold stored under the old “doses remaining” meaning, how should that number be treated after switching the alert to units? → A: Reset every medication to the new default of 10 units
- Q: Can the quantity per dose on an Agenda be a fraction (for example 0.5), or must it be a whole number of units only? → A: Whole numbers only (1, 2, 3…)
- Q: Must stock quantity and the purchase-alert threshold also be whole numbers only, matching quantity per dose? → A: Whole numbers only for stock and threshold (≥ 0)
- Q (analyze/remediação): Ao excluir, quais doses PENDING são puladas? → A: Todas as PENDING do medicamento (qualquer agenda), não só as das agendas ACTIVE/PAUSED canceladas.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Excluir medicamento do catálogo (Priority: P1)

Como usuário autorizado, quero excluir um medicamento do meu catálogo quando
não o uso mais, para manter a lista limpa sem perder o histórico de doses já
registradas.

**Why this priority**: Hoje o cadastro não permite remoção; é o gap mais
visível de gestão do catálogo e bloqueia higiene dos dados do usuário.

**Independent Test**: Com um medicamento existente, executar exclusão e
confirmar que ele some da lista ativa do catálogo, sem apagar o histórico de
doses já registradas.

**Acceptance Scenarios**:

1. **Given** um medicamento no catálogo do usuário autenticado, **When** o
   usuário confirma a exclusão, **Then** o medicamento deixa de aparecer na
   lista ativa do catálogo e não pode ser usado em novas agendas.
2. **Given** um medicamento com histórico de doses tomadas ou puladas,
   **When** o usuário o exclui, **Then** o acompanhamento continua exibindo
   essas doses do passado (o vínculo histórico permanece consultável).
3. **Given** um medicamento com uma ou mais agendas `ACTIVE` ou `PAUSED`,
   **When** o usuário confirma a exclusão, **Then** essas agendas passam a
   `CANCELLED`, **todas** as doses ainda `PENDING` desse medicamento (de
   qualquer agenda) passam a puladas, novos lembretes deixam de ser gerados
   e o medicamento some do catálogo ativo.
4. **Given** um medicamento que não pertence ao usuário autenticado,
   **When** tenta excluí-lo, **Then** a operação é negada.

---

### User Story 2 - Quantidade por dose definida na Agenda (Priority: P1)

Como usuário, quero informar quantas unidades do medicamento consumo em cada
tomada ao criar ou editar a Agenda (e não no cadastro do item), para que a
mesma embalagem possa ser usada em agendas com doses diferentes.

**Why this priority**: Corrige o modelo de domínio: a dose é propriedade do
plano de tratamento, não do item de catálogo.

**Independent Test**: Cadastrar um medicamento sem quantidade por dose; criar
duas agendas do mesmo item com quantidades por dose distintas; registrar
tomadas e verificar o consumo de estoque de cada uma.

**Acceptance Scenarios**:

1. **Given** o fluxo de cadastro de medicamento, **When** o usuário cria ou
   edita o item do catálogo, **Then** não há campo de quantidade por dose no
   cadastro do medicamento.
2. **Given** um medicamento no catálogo, **When** o usuário cria uma agenda,
   **Then** deve informar a quantidade por dose (em unidades do medicamento)
   como parte da agenda.
3. **Given** o mesmo medicamento em duas agendas com quantidades por dose
   diferentes (ex.: 1 e 2 unidades), **When** registra uma tomada em cada
   agenda, **Then** o estoque diminui pela quantidade definida na agenda
   correspondente.
4. **Given** uma agenda existente, **When** o usuário edita a quantidade por
   dose, **Then** tomadas futuras usam o novo valor; doses já registradas
   não são recalculadas no estoque.

---

### User Story 3 - Alerta de compra por unidade de estoque (Priority: P1)

Como usuário, quero ser avisado de comprar quando o estoque em unidades do
medicamento atingir o limiar que configurei em unidades (não em “doses”), para
planejar a reposição com base no que tenho na embalagem.

**Why this priority**: Alinha o alerta ao que o usuário realmente conta em
casa (unidades/comprimidos/ml), independentemente do tamanho da dose na
agenda.

**Independent Test**: Configurar estoque e limiar em unidades; reduzir o
estoque até o limiar e verificar o aviso in-app; confirmar que o cálculo não
converte estoque em número de doses.

**Acceptance Scenarios**:

1. **Given** um medicamento com estoque e limiar de compra informados em
   unidades, **When** o estoque fica igual ou abaixo do limiar, **Then** o
   usuário vê o aviso de compra no app (Mobile ou Web), sem push de compra.
2. **Given** estoque de 15 unidades e limiar de 10 unidades, **When** o
   estoque ainda está acima de 10, **Then** o aviso de compra não aparece.
3. **Given** uma agenda com quantidade por dose maior que 1, **When** o
   sistema avalia o alerta, **Then** compara estoque em unidades com o limiar
   em unidades (não divide o estoque pela quantidade por dose).
4. **Given** o usuário não informou limiar, **When** o sistema aplica o
   padrão, **Then** usa o limiar padrão de 10 unidades.

---

### Edge Cases

- Exclusão de medicamento sem agendas: remove do catálogo ativo imediatamente
  após confirmação.
- Exclusão com agendas `ACTIVE`/`PAUSED` que tenham doses `PENDING`: essas
  doses (e quaisquer outras `PENDING` do mesmo medicamento) são marcadas
  como puladas imediatamente; nenhum lembrete adicional é enviado para elas
  (estoque não é reduzido por esse auto-pulo).
- Exclusão com agendas `COMPLETED` ou `CANCELLED`: o medicamento some do
  catálogo ativo; agendas encerradas permanecem no histórico com status
  atual; doses `PENDING` remanescentes (se houver) também passam a puladas.
- Tentativa de criar nova agenda a partir de medicamento excluído: bloqueada.
- Quantidade por dose inválida (zero, negativa, vazia ou fracionária):
  rejeitada na agenda.
- Estoque ou limiar fracionário, negativo ou ausente (null/omitido quando
  enviado de forma inválida): rejeitado na edição do medicamento; apenas
  inteiros ≥ 0. O valor `0` é válido (estoque zerado ou limiar zero).
- Estoque zero com limiar zero: aviso de compra permanece visível enquanto o
  critério de limiar for atendido.
- Medicamento excluído ainda listado no acompanhamento histórico: permitido
  como leitura; edição de estoque/cadastro do item excluído não é permitida;
  restauração ao catálogo ativo não está disponível.
- Várias agendas ativas do mesmo medicamento com doses diferentes: cada
  tomada consome a quantidade da sua agenda; o alerta de compra continua
  baseado apenas no estoque total em unidades vs limiar em unidades.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: O sistema MUST permitir que o dono do medicamento no catálogo
  exclua o item, após confirmação explícita do usuário.
- **FR-002**: A exclusão MUST remover o medicamento da lista ativa do catálogo
  (soft-delete): o item NÃO MUST aparecer para novas agendas nem para edição
  de cadastro/estoque. Nesta feature, o sistema NÃO MUST oferecer restauração
  do medicamento excluído ao catálogo ativo.
- **FR-003**: Ao excluir um medicamento, o sistema MUST cancelar
  (`CANCELLED`) todas as agendas `ACTIVE` ou `PAUSED` vinculadas, MUST
  marcar como puladas **todas** as doses ainda `PENDING` vinculadas a
  **qualquer** agenda desse medicamento (incluindo agendas já
  `COMPLETED`/`CANCELLED` que ainda tenham ocorrência pendente), e MUST
  interromper novos lembretes dessas doses/agendas. O auto-pulo de doses
  pendentes NÃO MUST reduzir o estoque.
- **FR-004**: A exclusão NÃO MUST apagar o histórico de doses já registradas
  (tomadas e puladas); o acompanhamento MUST permanecer consultável.
- **FR-005**: O cadastro do medicamento (catálogo) MUST NÃO incluir quantidade
  por dose.
- **FR-006**: Cada Agenda MUST incluir quantidade por dose como inteiro
  positivo (≥ 1) expressa na unidade do medicamento do catálogo; a
  quantidade é obrigatória na criação e editável enquanto a agenda existir.
  Valores fracionários NÃO MUST ser aceitos.
- **FR-007**: Ao registrar uma dose como tomada, o sistema MUST reduzir o
  estoque do medicamento pela quantidade por dose da Agenda daquela dose.
- **FR-008**: O usuário MUST informar estoque e limiar de aviso de compra como
  inteiros ≥ 0 na unidade do medicamento (mesma unidade do catálogo). Valores
  fracionários NÃO MUST ser aceitos.
- **FR-009**: O sistema MUST exibir aviso de compra in-app (Mobile e Web)
  quando `estoque_em_unidades <= limiar_em_unidades`. O cálculo MUST NÃO
  converter estoque em “número de doses”. Aviso de compra NÃO MUST usar push.
- **FR-010**: Se o limiar não for informado, o sistema MUST aplicar o limiar
  padrão de **10 unidades**. Na transição do modelo antigo (limiar em doses),
  limiares já persistidos MUST ser redefinidos para 10 unidades.
- **FR-011**: Exclusão, definição de quantidade por dose na agenda e alerta por
  unidade MUST respeitar o escopo por usuário (apenas o dono gerencia seus
  itens).
- **FR-012**: A feature MUST estar disponível em Mobile e Web, alinhada à
  gestão de catálogo e agenda já existente.

### Key Entities

- **Medicamento (catálogo)**: item do usuário com nome, unidade, estoque em
  unidades (inteiro ≥ 0) e limiar de compra em unidades (inteiro ≥ 0); pode
  estar ativo ou excluído (soft-delete); não possui quantidade por dose.
- **Agenda de tratamento**: plano vinculado a um medicamento ativo, com
  horários, dias, duração e **quantidade por dose** (inteiro ≥ 1 de unidades
  consumidas por tomada).
- **Dose (ocorrência)**: tomada agendada; ao ser marcada como tomada, consome
  do estoque a quantidade por dose da sua agenda.
- **Aviso de compra**: alerta in-app quando o estoque em unidades está no
  limiar ou abaixo; independente do tamanho da dose nas agendas.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Em teste guiado, 100% dos usuários conseguem excluir um
  medicamento do catálogo ativo em menos de 1 minuto (incluindo confirmação).
- **SC-002**: Após exclusão, o medicamento não aparece na lista ativa em 100%
  das consultas de catálogo do conjunto de teste; o histórico de doses
  anteriores permanece 100% consultável.
- **SC-003**: Em 100% dos casos de teste com duas agendas do mesmo medicamento
  e quantidades por dose distintas, cada tomada reduz o estoque exatamente
  pela quantidade da agenda correspondente.
- **SC-004**: Em 100% dos cenários de teste, o aviso de compra aparece
  somente quando o estoque em unidades é ≤ limiar em unidades, sem depender
  da quantidade por dose da agenda.
- **SC-005**: Usuários concluem criar/editar agenda informando quantidade por
  dose sem precisar voltar ao cadastro do medicamento, em menos de 2 minutos
  no fluxo guiado.

## Assumptions

- Soft-delete: exclusão oculta o medicamento do catálogo ativo e impede novo
  uso; não remove fisicamente o registro necessário ao histórico. Restauração
  ao catálogo ativo está fora do escopo desta feature.
- Ao excluir, doses `PENDING` de **qualquer** agenda do medicamento são
  marcadas como puladas de imediato (sem consumir estoque); lembretes dessas
  doses cessam. Agendas `ACTIVE`/`PAUSED` são canceladas automaticamente.
- Confirmação explícita é obrigatória antes de excluir (evita exclusão
  acidental).
- Agendas já `COMPLETED`/`CANCELLED` permanecem com esse status (não são
  reabertas); apenas `ACTIVE`/`PAUSED` passam a `CANCELLED`.
- Medicamentos já cadastrados com quantidade por dose no catálogo (modelo
  anterior) devem migrar esse valor para cada agenda existente desse
  medicamento; o campo some do catálogo.
- Limiar padrão de aviso de compra: **10 unidades** (substitui o conceito
  anterior de “7 doses restantes”). Na migração para alerta por unidade,
  limiares existentes MUST ser redefinidos para esse padrão (10 unidades);
  o usuário pode alterar depois.
- Se a quantidade por dose não for informada na criação da agenda, o padrão
  é 1 unidade por dose (inteiro). Fracionários não são permitidos para
  quantidade por dose, estoque nem limiar.
- Na migração, estoques fracionários existentes MUST ser arredondados para o
  inteiro mais próximo (≥ 0); limiares existentes MUST ser redefinidos para 10
  (não apenas arredondados).
- Aviso de compra continua apenas in-app (Mobile e Web), sem push — herdado
  da feature 001.
- Fora do escopo: exclusão em massa, exclusão permanente/purge, restauração
  de medicamento excluído, compartilhamento de catálogo entre usuários, e
  alteração do canal de notificação de compra.
