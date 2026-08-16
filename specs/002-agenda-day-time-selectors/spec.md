# Feature Specification: Seletores de dias e horários na Nova Agenda

**Feature Branch**: `002-agenda-day-time-selectors`

**Created**: 2026-08-15

**Status**: Ready for implementation

**Input**: User description: "Na tela Nova Agenda, deve haver um botão para selecionar todos os dias da semana. Em horários, não quero escrever um horário, quero escolher de uma lista."

## Clarifications

### Session 2026-08-15

- Q: When choosing times on Nova Agenda, how should the user pick one or more times from the hourly list? → A: Pick a time from the list to add it; selected times appear as a removable list
- Q: When the user opens Nova Agenda, should any time already appear in the removable list? → A: Start empty — user must add at least one time from the list
- Q: After a time is already in the removable list, how should that same hour appear in the add-from-list control? → A: Show them but not selectable (disabled/grayed)
- Q: In what order should times appear in the removable list after the user adds them? → A: Always chronological (earliest → latest), regardless of add order

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Escolher horários em uma lista (Priority: P1)

Como usuário autorizado na tela Nova Agenda, quero escolher um ou mais horários
de tomada a partir de uma lista pronta, sem digitar o horário manualmente, para
evitar erros de digitação e agilizar o cadastro.

**Why this priority**: Sem horários válidos a agenda não pode ser criada; a
entrada por texto livre é a principal fricção relatada nesta tela.

**Independent Test**: Abrir Nova Agenda, selecionar um ou mais horários apenas
pela lista (sem teclado de texto livre para o horário) e concluir a criação da
agenda com sucesso.

**Acceptance Scenarios**:

1. **Given** o usuário está na tela Nova Agenda, **When** ele usa o seletor de
   horário, **Then** vê uma lista de horários para adicionar (não um campo de
   texto livre para digitar o horário) e a lista removível de horários inicia
   vazia.
2. **Given** a lista de horários está disponível, **When** o usuário escolhe um
   horário, **Then** esse horário é adicionado à lista removível de horários da
   agenda em criação.
3. **Given** já há pelo menos um horário na lista removível, **When** o usuário
   escolhe outro horário distinto na lista, **Then** ambos aparecem na lista
   removível em ordem cronológica (do mais cedo ao mais tarde), independentemente
   da ordem de adição.
4. **Given** um horário consta na lista removível, **When** o usuário o remove
   dessa lista, **Then** esse horário deixa de fazer parte da agenda em
   criação.
5. **Given** nenhum horário está na lista removível, **When** o usuário tenta
   salvar a agenda, **Then** o sistema impede o salvamento e exibe mensagem
   clara de que é necessário escolher ao menos um horário (não basta apenas
   desabilitar o botão sem indicação).
6. **Given** um horário já está na lista removível, **When** o usuário vê a
   lista de adição, **Then** esse mesmo horário aparece desabilitado (visível,
   mas não selecionável).
7. **Given** o usuário acabou de adicionar um horário pela lista, **When**
   volta ao seletor de adição, **Then** o seletor está pronto para uma nova
   escolha (valor de adição resetado; não permanece “preso” no horário
   recém-adicionado).

---

### User Story 2 - Selecionar todos os dias da semana com um toque (Priority: P1)

Como usuário autorizado na tela Nova Agenda, quero um botão que marque todos os
dias da semana de uma vez, para agendas diárias sem marcar dia a dia.

**Why this priority**: O fluxo “tomar todos os dias” é um caso de uso explícito
do produto; o botão reduz esforço e erro de omissão de um dia.

**Independent Test**: Na Nova Agenda, com nenhum ou poucos dias marcados, acionar
o botão de selecionar todos e confirmar que os sete dias ficam selecionados;
salvar a agenda e verificar que ela vale para todos os dias da semana.

**Acceptance Scenarios**:

1. **Given** o usuário está na tela Nova Agenda com nenhum ou alguns dias
   marcados, **When** ele aciona o controle “selecionar todos os dias”, **Then**
   todos os sete dias da semana ficam selecionados.
2. **Given** todos os dias já estão selecionados, **When** o usuário desmarca
   um dia individualmente, **Then** apenas aquele dia deixa de estar
   selecionado e os demais permanecem.
3. **Given** nenhum dia está selecionado, **When** o usuário tenta salvar a
   agenda, **Then** o sistema impede o salvamento e exibe mensagem clara de
   que é necessário escolher ao menos um dia (não basta apenas desabilitar o
   botão sem indicação).
4. **Given** todos os dias foram selecionados pelo botão, **When** o usuário
   salva a agenda com os demais dados válidos, **Then** a agenda criada aplica-se
   a todos os dias da semana.

---

### Edge Cases

- Usuário aciona “selecionar todos” quando todos os dias já estão marcados: o
  estado permanece com todos os dias selecionados (ação idempotente).
- Usuário tenta adicionar o mesmo horário mais de uma vez pela lista: o horário
  já presente aparece na lista de adição como não selecionável (desabilitado) e
  não é duplicado na lista removível.
- Usuário remove todos os horários após ter selecionado alguns: o salvamento
  continua bloqueado até haver ao menos um horário.
- Lista de horários em dispositivos móveis e web: a escolha deve ser utilizável
  em ambos, sem depender de digitação do horário.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Na tela Nova Agenda, o sistema MUST oferecer um controle explícito
  (ex.: botão) que, ao ser acionado, selecione todos os sete dias da semana.
- **FR-002**: O usuário MUST continuar podendo marcar ou desmarcar dias
  individualmente após usar o controle de selecionar todos.
- **FR-003**: Na tela Nova Agenda, a definição de horários MUST ser feita
  escolhendo um horário em uma lista pré-definida para **adicionar** à agenda,
  sem campo de texto livre para digitar o horário.
- **FR-004**: O usuário MUST poder adicionar um ou mais horários distintos a
  partir da lista; os horários adicionados MUST aparecer em uma lista removível
  na própria tela.
- **FR-005**: O usuário MUST poder remover um horário já adicionado da lista
  removível antes de salvar.
- **FR-006**: O sistema MUST exigir ao menos um dia da semana e ao menos um
  horário selecionados para permitir a criação da agenda.
- **FR-007**: A lista de horários MUST cobrir o dia completo em intervalos de
  60 minutos (00:00, 01:00, …, 23:00), no fuso horário já usado pelas agendas
  do aplicativo.
- **FR-008**: O comportamento de medicamento, duração (indefinida ou N doses) e
  demais regras de criação de agenda MUST permanecer conforme a feature de
  controle de medicamentos já especificada; esta feature altera apenas a forma
  de escolher dias e horários na Nova Agenda.
- **FR-009**: Ao abrir a tela Nova Agenda, a lista removível de horários MUST
  iniciar vazia; o usuário MUST adicionar explicitamente ao menos um horário
  antes de salvar.
- **FR-010**: Horários já presentes na lista removível MUST continuar visíveis
  na lista de adição, porém MUST NÃO ser selecionáveis (estado desabilitado)
  até serem removidos da lista removível.
- **FR-011**: A lista removível de horários MUST exibir os horários em ordem
  cronológica (do mais cedo ao mais tarde), independentemente da ordem em que
  foram adicionados.

### Key Entities

- **Agenda em criação**: rascunho na tela Nova Agenda com medicamento, dias da
  semana selecionados, horários selecionados e duração; ainda não persistida
  até o salvamento bem-sucedido.
- **Dia da semana**: um dos sete dias; pode ser selecionado individualmente ou
  em bloco via “selecionar todos”.
- **Horário da lista**: opção da lista horária (intervalos de 60 minutos) que o
  usuário adiciona à agenda; horários já adicionados ficam em lista removível
  ordenada cronologicamente.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Em testes com usuários, 100% conseguem criar uma agenda diária
  (todos os dias) usando o controle de selecionar todos, sem marcar os sete
  dias um a um.
- **SC-002**: Em testes com usuários, 100% conseguem definir os horários da
  nova agenda apenas pela lista, sem digitar o horário.
- **SC-003**: Usuários familiarizados com o fluxo concluem a escolha de dias e
  horários na Nova Agenda em menos de 30 segundos no caminho feliz (todos os
  dias + um horário da lista).
- **SC-004**: Tentativas de salvar sem dia ou sem horário são bloqueadas em
  100% dos casos, com mensagem visível ao usuário explicando o que falta
  (dia e/ou horário).
- **SC-005**: Nenhuma agenda criada por este fluxo contém horário digitado
  fora da lista (somente valores escolhidos na lista).

## Assumptions

- O escopo desta feature é a tela **Nova Agenda** (criação). Se existir tela de
  edição de agenda com os mesmos campos de dias/horários, o mesmo padrão de
  interação SHOULD ser aplicado por consistência, mas não é requisito desta
  entrega.
- Intervalos de 60 minutos na lista são suficientes para o domínio de
  lembretes de medicamentos; horários fora desses slots não são necessários
  nesta versão.
- A lista removível de horários inicia vazia (sem horário pré-selecionado como
  `08:00`); o usuário deve adicionar ao menos um horário pela lista.
- O botão “selecionar todos os dias” apenas seleciona; não há obrigação de um
  botão “limpar todos” nesta entrega (o usuário pode desmarcar dias um a um).
- Regras de negócio de agenda (fuso do ambiente, múltiplos horários, duração,
  catálogo de medicamentos) permanecem as da especificação
  `001-medication-control`.
- A feature é de melhoria de usabilidade na interface; não altera o significado
  dos dados de dias e horários já persistidos.
