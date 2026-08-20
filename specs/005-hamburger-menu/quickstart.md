# Quickstart: Validar navegação hambúrguer / barra

Validação manual da UI (sem suite E2E obrigatória). API e Docker como em
`specs/001-medication-control/quickstart.md`.

## Pré-requisitos

- App no ar (Compose ou `ng serve`) com usuário whitelist autenticado
- DevTools com simulação de largura (ou janela redimensionável)
- Opcional: segundo usuário administrador para o cenário Admin

## Cenário A — Telefone: abrir, navegar e fechar

1. Autenticar. Ajustar viewport para **375px** de largura (telefone).
2. Confirmar: ícone hambúrguer visível; **não** há fileira permanente de links;
   Sair e a marca estão no cabeçalho; conteúdo da tela visível (menu fechado).
3. Acionar o hambúrguer → painel lista Medicamentos, Agendas, Doses, Compras,
   Histórico (e Admin só se for admin). Destino da tela atual destacado.
4. Escolher **Agendas** → URL `/agendas`; painel **fechado**.
5. Abrir de novo; acionar o hambúrguer → painel fecha; permanece em Agendas.
6. Abrir de novo; tocar o fundo escurecido (fora da lista) → fecha; permanece
   em Agendas.

**Esperado**: SC-001, SC-005, SC-006, SC-007; FR-001, FR-003–FR-005, FR-008–FR-009.

## Cenário B — Tablet e computador: barra sem hambúrguer

1. Viewport **768px** (tablet retrato) e depois **1280px** (computador).
2. Confirmar: barra permanente com os destinos; **sem** ícone hambúrguer.
3. Clicar **Doses** → `/doses`; Doses destacado na barra.
4. Confirmar Sair e marca visíveis.

**Esperado**: SC-002; FR-002, FR-010, FR-011.

## Cenário C — Resize com menu aberto

1. Viewport 375px; abrir o menu.
2. Alargar para **768px** (ou mais).
3. **Esperado**: painel some, barra aparece, hambúrguer some, rota **não** muda.
4. Estreitar de volta para **375px**.
5. **Esperado**: barra some, hambúrguer aparece, menu **fechado**.

**Esperado**: SC-008; FR-014, FR-015.

## Cenário D — Voltar do navegador

1. Telefone; em Medicamentos, abrir o menu, ir para Histórico.
2. Usar voltar do navegador.
3. **Esperado**: volta a Medicamentos; painel não permanece aberto.

## Cenário E — Perfil Admin vs comum

1. Usuário comum no telefone e em 768px: **não** vê Admin.
2. Administrador nos dois modos: vê **Admin** e chega em `/admin/whitelist`.

**Esperado**: FR-006, FR-007, SC-004.

## Cenário F — Fora do layout autenticado

1. Abrir `/login` e `/bloqueado`.
2. **Esperado**: sem hambúrguer e sem barra de destinos autenticados.

## Referências

- Contrato UI: [contracts/ui-layout-nav.md](./contracts/ui-layout-nav.md)
- Estado do menu: [data-model.md](./data-model.md)
- Rotas inalteradas: `frontend/src/app/app.routes.ts`
