# Feature Specification: Menu de navegação em formato hambúrguer

**Feature Branch**: `005-hamburger-menu`

**Created**: 2026-08-18

**Status**: Draft

**Input**: User description: "Alterar o layout para que o menu seja exibido no formato de hamburguer menu."

## Clarifications

### Session 2026-08-18

- Q: Em quais larguras de tela o menu deve ser hambúrguer versus barra de links? → A: Barra de links no desktop e hambúrguer só no celular.
- Q: Em um tablet, a navegação deve aparecer como menu hambúrguer ou como barra de links permanente? → A: Hambúrguer só em telefone; tablet e computador usam barra de links.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Abrir e navegar pelo menu hambúrguer no telefone (Priority: P1)

Como usuário autorizado em um telefone (celular), quero abrir o menu de
navegação a partir de um controle hambúrguer no cabeçalho e escolher o destino
desejado, para acessar as telas do aplicativo sem ocupar a área principal com
uma barra de links sempre visível.

**Why this priority**: No telefone, sem o menu hambúrguer funcional o usuário
não consegue trocar de tela após a mudança de layout. É o núcleo da feature
nesse dispositivo.

**Independent Test**: Em um telefone, autenticar, acionar o controle
hambúrguer, escolher um destino (por exemplo Agendas) e confirmar que a tela
correspondente é exibida e o menu se fecha.

**Acceptance Scenarios**:

1. **Given** o usuário autenticado está em qualquer tela com o layout principal
   em um telefone, **When** a tela é apresentada, **Then** o cabeçalho exibe o
   controle de menu hambúrguer (reconhecível como três linhas horizontais) e os
   destinos de navegação **não** aparecem como uma fileira permanente de links.
2. **Given** o menu está fechado no telefone, **When** o usuário aciona o
   controle hambúrguer, **Then** o painel de navegação abre e lista os destinos
   disponíveis.
3. **Given** o menu está aberto no telefone, **When** o usuário escolhe um
   destino, **Then** é levado à tela correspondente e o menu fecha.
4. **Given** o menu está aberto no telefone, **When** o usuário aciona novamente
   o controle hambúrguer, **Then** o menu fecha e o conteúdo da tela permanece
   visível.
5. **Given** o usuário está em uma tela acessível pelo menu no telefone,
   **When** abre o menu, **Then** o destino da tela atual aparece destacado em
   relação aos demais.

---

### User Story 2 - Navegar pela barra de links no tablet e no computador (Priority: P1)

Como usuário autorizado em um tablet ou computador (desktop), quero ver os
destinos de navegação como uma barra de links sempre visível no cabeçalho,
para trocar de tela com um clique, sem abrir um menu hambúrguer.

**Why this priority**: No tablet e no computador há espaço para os atalhos; o
hambúrguer nesse contexto seria um passo extra desnecessário. Sem esta
história o layout ficaria incorreto fora do telefone.

**Independent Test**: Em um tablet e em um computador, autenticar e clicar em
um destino da barra (por exemplo Agendas); confirmar que a tela correspondente
é exibida e que não há controle hambúrguer.

**Acceptance Scenarios**:

1. **Given** o usuário autenticado está em qualquer tela com o layout principal
   em um tablet ou computador, **When** a tela é apresentada, **Then** o
   cabeçalho exibe a barra permanente de links com os destinos disponíveis e
   **não** exibe o controle hambúrguer.
2. **Given** a barra de links está visível no tablet ou no computador,
   **When** o usuário escolhe um destino, **Then** é levado à tela
   correspondente.
3. **Given** o usuário está em uma tela acessível pela barra no tablet ou no
   computador, **When** olha a barra, **Then** o destino da tela atual aparece
   destacado em relação aos demais.

---

### User Story 3 - Fechar o menu hambúrguer sem navegar (Priority: P2)

Como usuário autorizado no telefone, quero fechar o menu sem escolher um
destino, para voltar ao conteúdo da tela atual quando abri o menu por engano
ou apenas para consultar as opções.

**Why this priority**: Evita que o usuário fique “preso” no painel aberto no
telefone; é o complemento de usabilidade do fluxo P1. Não se aplica a tablet
nem a computador, onde não há painel a fechar.

**Independent Test**: No telefone, abrir o menu e fechá-lo por cada forma
prevista (controle hambúrguer e área fora do painel) sem mudar de tela.

**Acceptance Scenarios**:

1. **Given** o menu está aberto no telefone, **When** o usuário toca ou clica
   fora do painel de navegação (área de conteúdo ou fundo), **Then** o menu
   fecha e a tela atual permanece a mesma.
2. **Given** o menu está aberto no telefone, **When** o usuário aciona o
   controle hambúrguer, **Then** o menu fecha sem alterar a tela atual.
3. **Given** o menu está fechado no telefone, **When** o usuário usa a tela
   normalmente (rolagem, preenchimento de formulário), **Then** o menu não
   abre sozinho.

---

### User Story 4 - Destinos corretos conforme o perfil (Priority: P2)

Como usuário autorizado, quero ver na navegação apenas os destinos que me
cabem, incluindo a área administrativa somente se eu for administrador, para
não encontrar atalhos indevidos e para que o administrador continue acessando
a whitelist — tanto no menu hambúrguer (telefone) quanto na barra de links
(tablet e computador).

**Why this priority**: Preserva a regra já estabelecida de visibilidade da
área admin; não é o gatilho da feature, mas é obrigatório para não regressar
autorização na interface.

**Independent Test**: Abrir a navegação (hambúrguer no telefone ou barra no
tablet/computador) como usuário comum (sem Admin) e como administrador (com
Admin); confirmar os destinos e que cada um leva à tela correta.

**Acceptance Scenarios**:

1. **Given** um usuário autenticado que não é administrador, **When** vê a
   navegação (painel no telefone ou barra no tablet/computador), **Then** vê os
   destinos Medicamentos, Agendas, Doses, Compras e Histórico, e **não** vê o
   destino Admin.
2. **Given** um usuário autenticado administrador, **When** vê a navegação,
   **Then** vê os mesmos destinos do usuário comum **e** o destino Admin.
3. **Given** a navegação visível, **When** o usuário escolhe qualquer destino
   visível, **Then** chega à tela já existente correspondente (catálogo,
   agendas, doses, alertas de compra, histórico ou whitelist administrativa).

---

### Edge Cases

- Usuário redimensiona a janela ou gira o dispositivo de telefone para tablet
  ou computador com o menu hambúrguer aberto: o painel fecha, o controle
  hambúrguer some e a barra de links passa a ser exibida; a tela atual não
  muda.
- Usuário redimensiona de tablet ou computador para telefone: a barra de
  links some, o controle hambúrguer aparece e o menu inicia fechado.
- Usuário abre o menu no telefone e usa o botão voltar do navegador: a tela
  anterior é restaurada e o menu não permanece aberto indevidamente sobre o
  conteúdo.
- Telas de autenticação (login, bloqueado) **não** exibem o menu hambúrguer
  nem a barra de links; o layout autenticado é o único que os inclui.
- Usuário com sessão encerrada (Sair): a navegação deixa de estar disponível,
  como já ocorre ao sair do layout autenticado.
- Destino Admin nunca aparece para quem não é administrador (a restrição de
  acesso existente permanece; esta feature só altera a forma de apresentar os
  atalhos).

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: No layout autenticado em **telefone**, o sistema MUST apresentar
  a navegação principal no formato de menu hambúrguer, sem barra permanente de
  links.
- **FR-002**: No layout autenticado em **tablet** ou **computador**, o sistema
  MUST apresentar a navegação principal como barra permanente de links no
  cabeçalho e MUST NÃO exibir o controle hambúrguer.
- **FR-003**: No telefone, o cabeçalho MUST exibir um controle explícito de
  menu hambúrguer (ícone de três linhas horizontais) enquanto o usuário está no
  layout autenticado.
- **FR-004**: No telefone, o menu MUST iniciar fechado ao carregar qualquer
  tela do layout autenticado, de modo que o conteúdo da tela seja a área
  principal visível.
- **FR-005**: No telefone, ao acionar o controle hambúrguer, o sistema MUST
  abrir um painel de navegação listando os destinos disponíveis; ao acioná-lo
  novamente, MUST fechar o painel.
- **FR-006**: A navegação (painel no telefone ou barra no tablet/computador)
  MUST incluir os destinos Medicamentos, Agendas, Doses, Compras e Histórico
  para todo usuário autenticado, com o mesmo significado e destinos já
  existentes no aplicativo.
- **FR-007**: O destino Admin MUST aparecer na navegação somente para o
  administrador; usuários comuns MUST NÃO vê-lo.
- **FR-008**: Ao escolher um destino no painel (telefone), o sistema MUST
  navegar para a tela correspondente e MUST fechar o menu. Ao escolher um
  destino na barra (tablet ou computador), o sistema MUST navegar para a tela
  correspondente.
- **FR-009**: No telefone, o sistema MUST permitir fechar o menu sem navegar,
  ao acionar o controle hambúrguer ou ao interagir fora do painel.
- **FR-010**: O destino correspondente à tela atual MUST estar visualmente
  distinguido dos demais, tanto no painel aberto (telefone) quanto na barra de
  links (tablet e computador).
- **FR-011**: O controle para encerrar a sessão (Sair) MUST permanecer
  acessível no cabeçalho sem depender de o menu estar aberto (no telefone) e
  sem substituir a barra de links (no tablet e no computador).
- **FR-012**: A marca/identidade do aplicativo no cabeçalho MUST permanecer
  visível em telefone, tablet e computador e continuar levando à tela de
  Medicamentos ao ser acionada.
- **FR-013**: Telas fora do layout autenticado (login e acesso bloqueado)
  MUST NÃO exibir o menu hambúrguer nem a barra de links.
- **FR-014**: Ao passar de telefone para tablet ou computador (redimensionar
  ou girar), o sistema MUST fechar o painel se estiver aberto, ocultar o
  controle hambúrguer e exibir a barra de links, sem mudar a tela atual.
- **FR-015**: Ao passar de tablet ou computador para telefone, o sistema MUST
  ocultar a barra de links, exibir o controle hambúrguer e deixar o menu
  fechado, sem mudar a tela atual.
- **FR-016**: Destinos, regras de autenticação, whitelist e conteúdo das
  telas MUST permanecer os já especificados nas features anteriores; esta
  feature altera apenas a forma de apresentar e acionar a navegação conforme
  a largura da janela (telefone: ≤767px; tablet e computador: ≥768px).

### Key Entities

- **Modo de apresentação**: telefone (menu hambúrguer, janela ≤767px) ou
  tablet/computador (barra de links, janela ≥768px), determinado pela
  largura da janela; não é uma preferência gravada pelo usuário.
- **Menu hambúrguer**: controle no cabeçalho do layout autenticado **no
  telefone** que abre e fecha o painel de navegação; estado aberto ou fechado.
- **Painel de navegação**: lista de destinos visível ao abrir o hambúrguer no
  telefone, conforme o perfil (comum ou administrador).
- **Barra de links**: lista permanente de destinos no cabeçalho **no tablet e
  no computador**, conforme o perfil (comum ou administrador).
- **Destino de navegação**: atalho nomeado para uma tela já existente
  (Medicamentos, Agendas, Doses, Compras, Histórico, Admin).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Em 100% das telas do layout autenticado **no telefone**, os
  destinos de navegação não ocupam uma fileira permanente no cabeçalho; o
  acesso ocorre pelo menu hambúrguer.
- **SC-002**: Em 100% das telas do layout autenticado **no tablet e no
  computador**, os destinos aparecem como barra permanente de links e o
  controle hambúrguer está ausente.
- **SC-003**: Usuários familiarizados no telefone concluem “abrir o menu e ir
  a outra tela” em menos de 5 segundos no caminho feliz; no tablet e no
  computador, concluem a troca de tela pela barra em menos de 3 segundos.
- **SC-004**: 100% dos destinos que o perfil do usuário já podia acessar
  permanecem acessíveis no hambúrguer (telefone) e na barra (tablet e
  computador), incluindo Admin somente para administrador.
- **SC-005**: Em 100% das aberturas de tela autenticada no telefone, o menu
  inicia fechado e o conteúdo da tela é visível sem ação extra.
- **SC-006**: Em testes em telefone, 100% dos participantes conseguem abrir,
  escolher um destino e fechar o menu sem instrução além do ícone hambúrguer.
- **SC-007**: Fechar o menu no telefone sem escolher destino (controle
  hambúrguer ou área fora do painel) funciona em 100% das tentativas e não
  altera a tela atual.
- **SC-008**: Ao redimensionar de telefone (menu aberto) para tablet ou
  computador, em 100% dos casos o painel some, a barra de links aparece e a
  tela atual se mantém.

## Assumptions

- O critério é a **largura da janela**, não o modelo do aparelho: viewport
  ≤767px = telefone (hambúrguer); ≥768px = tablet e computador (barra),
  inclusive tablet em retrato. Um telefone em paisagem com largura ≥768px
  MUST usar a barra de links.
- Os destinos e rótulos permanecem os atuais (Medicamentos, Agendas, Doses,
  Compras, Histórico, Admin). Não há novos destinos nesta entrega.
- O botão Sair permanece no cabeçalho (sempre visível em telefone, tablet e
  computador), não exclusivo do painel, para encerrar a sessão sem abrir o
  menu no telefone.
- A marca no cabeçalho continua apontando para Medicamentos, como hoje.
- Não há persistência da preferência “menu aberto”: cada carregamento de
  tela no telefone começa com o menu fechado.
- Esta feature é somente de apresentação da navegação; não altera API,
  persistência, autenticação Google nem whitelist.
- Animações e detalhes visuais do painel no telefone (deslizar da lateral,
  sobreposição) podem ser definidos na implementação, desde que o painel seja
  óbvio, acionável e fechável conforme os requisitos.
