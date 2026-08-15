# Feature Specification: Controle de Medicamentos

**Feature Branch**: `001-medication-control`

**Created**: 2026-08-15

**Status**: Ready for implementation

**Input**: User description: "Controle de medicamentos. Quero cadastrar os medicamentos que preciso tomar diariamente e receber lembretes. Cadastro um medicamento, escolho as horas do dia e os dias da semana. Posso tomar o medicamento todos os dias da semana ou escolher os dias que vou tomar. Posso tomar indefinidamente ou apenas X doses. Exemplo: tomar o medicamento X todo dia. Tomar o medicamento Y 10 vezes. O medicamento uma vez cadastrado pode ser reutilizado. Quero cadastrar também a quantidade de medicamentos que tenho para ser avisado quando será necessário comprar. Quero visualizar um acompanhamento das doses tomadas e puladas. Quando receber um aviso quero poder adiar a tomada daquele medicamento. Quero notificações apenas no celular. Qualquer dúvida perguntar-me antes de tomar uma decisão."

## Clarifications

### Session 2026-08-15

- Q: Quando a agenda tem limite de N doses, o que conta para chegar nesse limite? → A: Só doses tomadas contam para N (puladas não consomem o limite).
- Q: Como o sistema identifica o administrador que pode gerenciar a whitelist? → A: E-mail do admin fixado na configuração do ambiente.
- Q: Em qual fuso horário o sistema interpreta os horários das doses e dos adiamentos? → A: Fuso fixo único do servidor/ambiente (ex.: America/Sao_Paulo) para todos.
- Q: Se o usuário tiver mais de um celular com o app, para quais dispositivos o lembrete de dose deve ser enviado? → A: Todos os celulares registrados do usuário.
- Q: Como o usuário deve receber o aviso de que é necessário comprar o medicamento? → A: Somente no app (Mobile e Web), sem push de compra.
- Q (analyze/remediação): O que significa agenda “encerrada”? → A: `COMPLETED` (N tomadas atingidas) ou `CANCELLED` (encerrada manualmente); ambas podem ser reativadas para `ACTIVE`.
- Q (analyze/remediação): Como marcar dispositivo mobile para push? → A: Cliente envia `isMobile` no registro; servidor persiste o valor; push só para `isMobile=true` (desktop deve enviar `false`).
- Q (analyze/remediação): Admin pode sair da whitelist? → A: Não — API/UI proíbem remover o e-mail `ADMIN_EMAIL`; login do admin permanece autorizado se `email == ADMIN_EMAIL`.

## Glossary

- **Agenda** (`TreatmentSchedule` / API `Schedule`): plano de tomadas de um medicamento do catálogo.
- **Encerrada**: status `COMPLETED` ou `CANCELLED` (não `PAUSED`).
- **Dose** (`DoseOccurrence`): ocorrência agendada de tomada.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Cadastrar medicamento e agenda de tomadas (Priority: P1)

Como usuário autorizado, quero cadastrar um medicamento com horários e dias da
semana (todos os dias ou dias selecionados) e definir se o tratamento é
indefinido ou limitado a um número de doses, para que o sistema saiba quando
devo tomar.

**Why this priority**: Sem cadastro e agenda não há lembretes nem acompanhamento.

**Independent Test**: Cadastrar um medicamento com horários/dias e duração
(indefinida ou N doses) e confirmar que a agenda fica disponível para o usuário.

**Acceptance Scenarios**:

1. **Given** um usuário autenticado e autorizado, **When** cadastra um
   medicamento com nome, um ou mais horários e todos os dias da semana com
   duração indefinida, **Then** o medicamento fica no catálogo e a agenda fica
   ativa refletindo esses horários.
2. **Given** um usuário autenticado e autorizado, **When** cadastra um
   medicamento com dias específicos da semana e limite de N doses, **Then** o
   sistema agenda apenas nesses dias e encerra o tratamento após N doses
   tomadas (puladas não reduzem o saldo de N).
3. **Given** um medicamento já no catálogo, **When** o usuário cria uma nova
   agenda a partir dele, **Then** a nova agenda usa o mesmo medicamento sem
   exigir recadastro do item.
4. **Given** uma agenda `PAUSED`, `COMPLETED` ou `CANCELLED`, **When** o
   usuário a reativa, **Then** o status volta a `ACTIVE` e os lembretes voltam
   a ser gerados (em `FIXED_TAKEN_DOSES`, default = novo ciclo com contador de
   tomadas zerado).
5. **Given** uma agenda com limite N doses tomadas, **When** a N-ésima dose é
   marcada como tomada, **Then** a agenda passa a `COMPLETED` (encerrada por
   limite).

---

### User Story 2 - Receber lembrete no celular e registrar tomada/adiamento (Priority: P1)

Como usuário, quero receber notificação no celular na hora da dose e poder
confirmar a tomada, marcar como pulada ou adiar, para manter o tratamento em
dia sem depender de lembrete manual.

**Why this priority**: O valor central do produto é o lembrete acionável.

**Independent Test**: Com uma dose agendada no horário atual (ou simulado),
receber aviso no celular e concluir as ações de tomar, pular ou adiar.

**Acceptance Scenarios**:

1. **Given** uma dose agendada para o horário atual, **When** chega o horário,
   **Then** o usuário recebe notificação em todos os celulares registrados
   (não em desktop/web).
2. **Given** uma notificação de dose pendente, **When** o usuário confirma a
   tomada, **Then** a dose é registrada como tomada no acompanhamento.
3. **Given** uma notificação de dose pendente, **When** o usuário marca como
   pulada, **Then** a dose é registrada como pulada no acompanhamento.
4. **Given** uma notificação de dose pendente, **When** o usuário escolhe adiar
   por 10, 30 ou 60 minutos, **Then** a dose é reagendada para o novo horário e
   um novo lembrete será enviado nesse momento.
5. **Given** uma dose já adiada, **When** o usuário adianta novamente (incluindo
   novamente por 60 minutos), **Then** o adiamento é aceito sem limite de
   quantidade de adiamentos.

---

### User Story 3 - Estoque e aviso de compra (Priority: P2)

Como usuário, quero informar a quantidade que tenho em casa e ser avisado
quando for necessário comprar, para não ficar sem medicamento.

**Why this priority**: Complementa a adesão, mas o lembrete de tomada é o núcleo.

**Independent Test**: Informar estoque, registrar tomadas que consumam estoque
(ou ajuste manual) e verificar aviso de compra ao atingir o limiar.

**Acceptance Scenarios**:

1. **Given** um medicamento cadastrado, **When** o usuário informa a quantidade
   em estoque, **Then** o sistema passa a considerar esse saldo.
2. **Given** estoque abaixo do limiar de aviso configurado (ou padrão),
   **When** o usuário abre o app (Mobile ou Web) ou consulta a área de
   alertas, **Then** vê o aviso de que é necessário comprar, sem receber
   push de compra no celular.
3. **Given** uma dose marcada como tomada, **When** o estoque é atualizado,
   **Then** o saldo diminui de forma coerente com a dose (unidade definida pelo
   usuário no cadastro).

---

### User Story 4 - Acompanhar doses tomadas e puladas (Priority: P2)

Como usuário, quero ver o histórico/acompanhamento de doses tomadas e puladas
para entender minha adesão ao tratamento.

**Why this priority**: Dá visibilidade após os fluxos de cadastro e registro.

**Independent Test**: Com doses tomadas e puladas existentes, abrir o
acompanhamento e ver ambas as categorias corretamente.

**Acceptance Scenarios**:

1. **Given** doses tomadas e puladas no período, **When** o usuário abre o
   acompanhamento, **Then** visualiza a lista/resumo de tomadas e puladas.
2. **Given** um medicamento com agenda ativa, **When** o usuário filtra ou
   seleciona esse medicamento, **Then** vê apenas o histórico correspondente.

---

### User Story 5 - Acesso autorizado (login e whitelist) (Priority: P1)

Como administrador, quero que só pessoas na whitelist acessem o sistema via
login com Google, e gerenciar essa lista em tela só para mim, para proteger
dados de saúde.

**Why this priority**: Exigido pela constitution do projeto; sem isso não há
acesso seguro. Incluído no escopo desta feature.

**Independent Test**: Usuário na whitelist entra; usuário autenticado no Google
mas fora da whitelist é bloqueado; admin gerencia a lista.

**Acceptance Scenarios**:

1. **Given** identidade Google na whitelist, **When** faz login, **Then**
   acessa o sistema.
2. **Given** identidade Google fora da whitelist e diferente de `ADMIN_EMAIL`,
   **When** tenta login, **Then** o acesso é negado.
3. **Given** o administrador designado (e-mail configurado no ambiente),
   **When** abre a gestão de whitelist, **Then** pode adicionar e remover
   identidades autorizadas.
4. **Given** um usuário autenticado cuja identidade não é o e-mail de
   administrador configurado, **When** tenta acessar a gestão de whitelist,
   **Then** o acesso é negado.
5. **Given** o administrador, **When** tenta remover da whitelist o e-mail
   igual a `ADMIN_EMAIL`, **Then** a operação é rejeitada.

---

### Edge Cases

- Quando N doses tomadas são atingidas, a agenda fica `COMPLETED` e não há
  novos lembretes; doses puladas não contam para N; o medicamento permanece no
  catálogo para reuso.
- Encerramento manual da agenda resulta em `CANCELLED`; pausa resulta em
  `PAUSED`. `COMPLETED` e `CANCELLED` são reativáveis.
- Se o usuário não responder ao lembrete, após uma janela de 2 horas a dose
  pendente é marcada automaticamente como pulada (ver Assumptions).
- Se o estoque chegar a zero, o aviso de compra permanece; lembretes de tomada
  continuam até o usuário pausar ou encerrar a agenda.
- Se um adiamento cair no mesmo horário (ou após) de outra dose do mesmo
  medicamento, ambas permanecem como ocorrências distintas; o usuário trata
  cada uma (tomar, pular ou adiar de novo).
- Usuário só no computador: gerencia cadastros e histórico na web/desktop, sem
  push de dose nesse dispositivo.
- Com vários celulares registrados, o mesmo lembrete de dose é enviado a todos;
  registrar tomada/pulada/adiamento em um dispositivo resolve a dose para os
  demais (não exige ação duplicada).
- Horários cadastrados e adiamentos usam o fuso fixo do ambiente; se o
  dispositivo estiver em outro fuso, o relógio local pode diferir do horário
  exibido/agendado pelo sistema.
- O mesmo medicamento do catálogo pode ter mais de uma agenda ativa; cada
  agenda gera suas próprias doses e lembretes.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: O sistema MUST permitir que o usuário autenticado e autorizado
  cadastre medicamentos com identificação legível (nome) e dados necessários
  à agenda.
- **FR-002**: O sistema MUST permitir definir um ou mais horários do dia para
  cada agenda de medicamento.
- **FR-003**: O sistema MUST permitir escolher todos os dias da semana ou um
  subconjunto de dias da semana para a agenda.
- **FR-004**: O sistema MUST permitir duração indefinida ou limitada a um
  número inteiro positivo de doses tomadas (N).
- **FR-005**: Ao atingir N doses tomadas na agenda, o sistema MUST definir a
  agenda como `COMPLETED` e encerrar novos lembretes dessa agenda. Doses
  puladas, pendentes ou adiadas NÃO MUST contar para o limite N.
- **FR-006**: O sistema MUST manter medicamentos em catálogo reutilizável: o
  usuário MUST poder criar novas agendas a partir de um medicamento já
  cadastrado e MUST poder reativar agendas em `PAUSED`, `COMPLETED` ou
  `CANCELLED` para `ACTIVE`.
- **FR-007**: O sistema MUST enviar lembretes de dose apenas a dispositivos
  registrados com `isMobile=true`; web/desktop MUST registrar com
  `isMobile=false` e NÃO MUST receber push de dose. Se houver vários celulares
  (`isMobile=true`), o lembrete MUST ser enviado a todos eles.
- **FR-008**: A partir de um lembrete, o usuário MUST poder registrar a dose
  como tomada, pulada ou adiada.
- **FR-009**: O adiamento MUST oferecer as opções fixas de 10, 30 e 60 minutos,
  SEM limite de quantidade de adiamentos por dose; o usuário MUST poder adiar
  novamente (incluindo por 60 minutos) quantas vezes desejar enquanto a dose
  permanecer pendente.
- **FR-010**: O sistema MUST manter acompanhamento consultável de doses
  tomadas e puladas por medicamento e no conjunto.
- **FR-011**: O usuário MUST poder informar e atualizar a quantidade em estoque
  de cada medicamento.
- **FR-012**: O sistema MUST exibir aviso de compra no app (Mobile e Web)
  quando o estoque atingir ou ficar abaixo do limiar definido pelo usuário
  (padrão se não informado — ver Assumptions). Aviso de compra NÃO MUST usar
  push no celular.
- **FR-013**: Ao registrar uma dose como tomada, o sistema MUST reduzir o
  estoque de forma coerente com a unidade/quantidade por dose informada.
- **FR-014**: Cada usuário MUST ver e gerenciar apenas os próprios medicamentos,
  agendas, estoque e histórico. Tentativas de acessar recurso de outro usuário
  MUST retornar 404 ou 403.
- **FR-015**: Esta feature MUST incluir login com Google, verificação de
  whitelist e tela administrativa de whitelist. O administrador MUST ser a
  identidade cujo e-mail Google coincide com `ADMIN_EMAIL`. Somente esse
  administrador MUST acessar a gestão da whitelist. Identidades fora da
  whitelist MUST ser bloqueadas, exceto `ADMIN_EMAIL` (sempre autorizado a
  entrar). Remover `ADMIN_EMAIL` da whitelist MUST ser rejeitado.
- **FR-016**: Cadastro, edição, pausa/encerramento/reativação de agenda e
  consulta de histórico MUST estar disponíveis em Mobile e Web; apenas o canal
  de notificação push de dose é exclusivo do celular.
- **FR-017**: Horários de agenda, lembretes e adiamentos MUST ser interpretados
  no fuso horário fixo definido na configuração do ambiente (o mesmo para
  todos os usuários); a interface MUST deixar claro qual fuso está em uso.

### Key Entities

- **Usuário**: pessoa autenticada via Google; é administrador se o e-mail
  Google coincidir com o e-mail de admin configurado no ambiente; caso
  contrário, se estiver na whitelist, é usuário comum autorizado.
- **Entrada da whitelist**: identidade Google (e-mail) autorizada a acessar o
  sistema; gerida apenas pelo administrador.
- **Medicamento (catálogo)**: item cadastrado pelo usuário (nome, unidade,
  quantidade por dose, estoque, limiar de compra); pode ter zero ou mais agendas.
- **Agenda de tratamento**: combinação de medicamento do catálogo, horários,
  dias da semana e duração (indefinida ou N doses tomadas); estados `ACTIVE`,
  `PAUSED`, `COMPLETED`, `CANCELLED`; reativável a partir de pausada/encerrada.
- **Dose (ocorrência)**: instante previsto de tomada; status pendente, tomada,
  pulada ou adiada (com novo horário); adiamentos ilimitados com opções de
  10/30/60 minutos.
- **Registro de acompanhamento**: histórico das doses tomadas e puladas.
- **Dispositivo registrado**: subscription Web Push com flag `isMobile`; só
  `isMobile=true` recebe lembretes de dose; um usuário pode ter vários.
- **Aviso de compra**: alerta in-app (Mobile e Web) quando o estoque está no
  limiar ou abaixo; sem notificação push.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Um usuário autorizado consegue cadastrar um medicamento com
  horários e dias e concluir o fluxo em menos de 3 minutos na primeira vez.
- **SC-002**: Em validação controlada (Compose + ≥10 ocorrências de dose com
  devices mobile registrados e permissão concedida), ≥95% dos envios de push
  registrados com sucesso no log de notificação ocorrem com `sent_at` ≤ 1
  minuto após `scheduled_at` (fuso do ambiente). Falhas de rede do aparelho
  fora do ambiente de teste não contam contra o critério.
- **SC-003**: A partir do lembrete, o usuário consegue registrar tomada, pulada
  ou adiamento em menos de 30 segundos.
- **SC-004**: 100% das tentativas de acesso de identidades fora da whitelist
  são bloqueadas.
- **SC-005**: O acompanhamento exibe corretamente todas as doses tomadas e
  puladas registradas no período consultado (sem omissões no conjunto de teste).
- **SC-006**: Quando o estoque atinge o limiar, o usuário vê o aviso de compra
  no app (Mobile ou Web) na próxima abertura ou na área de alertas, sem push
  de compra.
- **SC-007**: Usuários conseguem gerenciar medicamentos tanto no celular quanto
  na web; notificações de dose aparecem apenas no celular.
- **SC-008**: Um medicamento do catálogo pode ser usado em uma nova agenda e
  uma agenda encerrada/pausada pode ser reativada sem recadastrar o item.

## Assumptions

- Em agendas com limite N, somente doses registradas como tomadas consomem o
  contador; puladas não encerram o tratamento por contagem.
- Cada dose tomada consome a quantidade por dose configurada no medicamento;
  se não informada, assume-se 1 unidade por dose.
- Limiar padrão de aviso de compra: 7 doses restantes (o usuário pode alterar).
- Se o usuário não responder ao lembrete, após uma janela de 2 horas a dose
  pendente é marcada automaticamente como pulada, salvo se ainda estiver em
  ciclo de adiamento com novo horário futuro.
- Web e desktop permitem gestão completa; apenas push de lembrete de dose é
  exclusivo do celular.
- Dados de medicamentos e histórico são privados por usuário.
- Idioma da interface: português (Brasil).
- Login Google, whitelist e tela admin fazem parte desta feature (não são
  pré-requisito externo).
- O e-mail do administrador é definido na configuração do ambiente e não é
  removível da whitelist pela interface/API.
- O fuso horário da aplicação é único e definido na configuração do ambiente
  (ex.: America/Sao_Paulo), aplicável a todos os usuários.
- Registro de push: o cliente informa `isMobile`; desktop usa `false`; mobile
  com permissão concedida usa `true`.
- Aviso de necessidade de compra aparece apenas no app (Mobile e Web); push
  no celular fica restrito a lembretes de dose.
- Fora do escopo desta feature: prescrição médica, integração com farmácias,
  lembretes por e-mail/SMS, compartilhamento familiar de agendas, e canais de
  notificação além do celular.
