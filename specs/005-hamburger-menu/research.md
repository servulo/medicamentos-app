# Research: Menu de navegação em formato hambúrguer

## 1. Ponto de mudança no código

**Decision**: Implementar só em `frontend/src/app/shared/layout.component.ts`
(template, estilos e estado `menuOpen`). Não criar componente de nav separado
nem alterar `app.routes.ts`.

**Rationale**: O layout autenticado já concentra marca, links, Admin
condicional e Sair. Login e bloqueado já ficam fora desse componente (FR-013).
Extrair um módulo extra aumentaria arquivos sem ganho nesta entrega.

**Alternatives considered**:
- Novo `NavMenuComponent` + layout fino → rejeitado (complexidade sem requisito).
- Duplicar listas de links (uma para barra, uma para painel) → rejeitado;
  um único `<nav>` com CSS por breakpoint evita destinos divergentes.

## 2. Corte telefone vs tablet/computador

**Decision**: Viewport `max-width: 767px` = telefone (hambúrguer). Viewport
`min-width: 768px` = tablet e computador (barra de links), inclusive tablet
em retrato (iPad típico ≥ 768px). O critério é **largura da janela**, não
detecção de aparelho.

**Rationale**: A spec deixa o corte exato para a implementação e exige barra
no tablet retrato. 768px é o limiar usual de “tablet portrait”. Telefone em
paisagem largo (≥ 768px) pode mostrar a barra — aceitável porque o critério
é espaço horizontal, não o rótulo do hardware.

**Alternatives considered**:
- Manter o `max-width: 700px` já existente no header → rejeitado; 700px
  deixaria tablets estreitos no modo hambúrguer, contra a clarificação A.
- `max-width: 599px` (Material “phone”) → rejeitado; tablets pequenos em
  retrato (~600–767px) ficariam na barra, mas o limiar 767px alinha melhor
  “só telefone” vs “iPad retrato = barra”.
- User-agent / `pointer: coarse` → rejeitado (frágil; viola o modelo por
  largura da spec).

## 3. Painel no telefone (abertura / fechamento)

**Decision**: Controle hambúrguer (botão com ícone de três linhas e rótulo
acessível “Menu”). Painel em lista vertical sobre um fundo escurecido
(overlay). Fechar: (1) botão de novo, (2) toque no overlay, (3) escolha de
destino, (4) `NavigationEnd` (voltar do navegador), (5) cruzar para ≥ 768px
via `matchMedia` (força `menuOpen = false`). Estado inicial `menuOpen = false`.
Não persistir em `localStorage`.

**Rationale**: Cobre FR-004, FR-005, FR-008, FR-009, FR-014, FR-015 e o edge
case do botão voltar. Overlay torna o “fora do painel” óbvio. Sem CDK: o
projeto não declara `@angular/cdk`.

**Alternatives considered**:
- `@angular/cdk` Overlay/BreakpointObserver → rejeitado (nova dependência).
- Só CSS (`:target` / checkbox hack) → rejeitado; não fecha de forma
  confiável no resize nem no `NavigationEnd`.
- Drawer lateral com biblioteca → desnecessário para o MVP.

## 4. Destinos, Admin e Sair

**Decision**: Manter os mesmos `routerLink` e rótulos. Admin continua
`@if (me.profile()?.admin)`. `RouterLinkActive` marca o destino atual na
barra e no painel. Sair permanece botão no cabeçalho, visível nos dois
modos, fora do painel.

**Rationale**: FR-006, FR-007, FR-010, FR-011, FR-016. `adminGuard` já
protege a rota; a UI só esconde o atalho.

**Alternatives considered**:
- Mover Sair para dentro do painel → rejeitado pela spec.
- Esconder Admin só no CSS → rejeitado; a condição atual por perfil é a
  fonte da verdade.

## 5. Testes

**Decision**: Nenhum teste de API novo. Sem E2E como gate. Validação manual
pelo [quickstart.md](./quickstart.md) (DevTools device mode basta para o
corte 767/768). Unit test Angular opcional para o toggle/`matchMedia`.

**Rationale**: Constitution IV. A feature não toca contratos HTTP.

**Alternatives considered**:
- Playwright/Cypress como gate → proibido pela constitution como gate.
