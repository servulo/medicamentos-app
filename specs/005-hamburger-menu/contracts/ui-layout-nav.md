# UI Contract: Layout autenticado — navegação hambúrguer / barra

**Scope**: Cabeçalho de `LayoutComponent` (`frontend/src/app/shared/layout.component.ts`),
usado em todas as rotas filhas autenticadas.  
**HTTP**: Sem alteração. Sem novos endpoints.

## Breakpoint

| Viewport | Modo | Navegação |
|----------|------|-----------|
| largura ≤ 767px | telefone | Controle hambúrguer + painel; **sem** barra permanente |
| largura ≥ 768px | tablet / computador | Barra permanente de links; **sem** controle hambúrguer |

O mesmo `<nav>` serve os dois modos (CSS). Destinos não podem divergir.

## Cabeçalho (ambos os modos)

| Elemento | Comportamento |
|----------|----------------|
| Marca “💊 Medicamentos” | Sempre visível; navega para `/medicamentos` |
| Botão Sair | Sempre visível no cabeçalho; chama logout existente; **não** fica só no painel |
| Destino ativo | Classe visual `active` via `routerLinkActive` (painel ou barra) |

## Telefone (≤ 767px)

| Elemento | Comportamento |
|----------|----------------|
| Botão hambúrguer | Visível; ícone de três linhas; acessível como “Menu”; `aria-expanded` reflete aberto/fechado |
| Painel | Fechado no carregamento. Abre/fecha pelo botão. Lista vertical dos destinos visíveis |
| Overlay | Visível com o painel aberto; acionar fecha o painel sem mudar a rota |
| Escolher destino | Navega e **fecha** o painel |
| Voltar do navegador | Painel fechado após a navegação |
| Resize para ≥ 768px | Painel fecha; hambúrguer some; barra aparece; rota inalterada |

## Tablet e computador (≥ 768px)

| Elemento | Comportamento |
|----------|----------------|
| Barra de links | Sempre visível no cabeçalho |
| Hambúrguer / overlay / painel | Não visíveis |
| Escolher destino | Navega; sem estado de painel |
| Resize para ≤ 767px | Barra some; hambúrguer aparece; painel **fechado**; rota inalterada |

## Destinos

| Rótulo | Rota | Condição |
|--------|------|----------|
| Medicamentos | `/medicamentos` | Autenticado |
| Agendas | `/agendas` | Autenticado |
| Doses | `/doses` | Autenticado |
| Compras | `/alertas` | Autenticado |
| Histórico | `/historico` | Autenticado |
| Admin | `/admin/whitelist` | Somente se perfil admin |

Restrição de rota admin permanece `adminGuard` (fora deste contrato visual).

## Fora de escopo deste contrato

- Telas `/login` e `/bloqueado` (não usam `LayoutComponent`)
- Novos destinos, novos rótulos ou mudança de rotas
- Persistência de “menu aberto”
- Biblioteca de overlay / CDK
- OpenAPI / backend
