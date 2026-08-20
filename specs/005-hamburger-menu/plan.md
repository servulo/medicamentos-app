# Implementation Plan: Menu de navegação em formato hambúrguer

**Branch**: `005-hamburger-menu` | **Date**: 2026-08-18 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/005-hamburger-menu/spec.md`

## Summary

Alterar o cabeçalho autenticado para navegação **responsiva por largura**: menu
hambúrguer só em viewport de telefone (`max-width: 767px`); barra permanente de
links em tablet e computador. Destinos, destaque da rota ativa, Sair e marca
permanecem os atuais. Implementação só em `LayoutComponent` (CSS + estado
aberto/fechado); sem mudança de API, rotas, auth ou persistência.

## Technical Context

**Language/Version**: TypeScript / Angular 19 (já no monorepo); Java/Quarkus
inalterados

**Primary Dependencies**: Angular standalone (`LayoutComponent`), `RouterLink`,
`RouterLinkActive`; media query CSS; `window.matchMedia` para fechar o painel
ao cruzar o corte telefone ↔ tablet. Sem `@angular/cdk` e sem biblioteca de UI.

**Storage**: N/A — estado do menu é efêmero no cliente; não persiste

**Testing**: Sem mudança de endpoints → testes de API novos **não** são
obrigatórios (constitution IV). Validação via [quickstart.md](./quickstart.md)
manual. Testes unitários Angular opcionais.

**Target Platform**: Web e Mobile (UI responsiva); mesma stack Docker

**Project Type**: Web application (frontend change only)

**Performance Goals**: Abrir o menu e ir a outra tela no telefone em &lt;5s;
trocar de tela pela barra no tablet/computador em &lt;3s (SC-003)

**Constraints**: Hambúrguer só em telefone; barra em tablet e computador
(inclusive tablet retrato); Sair e marca sempre no cabeçalho; login/bloqueado
fora do layout; auth/whitelist/Docker inalterados

**Scale/Scope**: Um componente (`LayoutComponent`); 5 destinos comuns + Admin
condicional; um breakpoint (`767px`)

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Princípio | Status | Evidência no plano |
|-----------|--------|-------------------|
| I. Propósito medicamentos/lembretes | PASS | Melhora o acesso às telas de tratamento (catálogo, agendas, doses) em telefone sem mudar o domínio |
| II. Camadas Angular ↔ API ↔ Quarkus ↔ PG | PASS | Só UI; nenhuma chamada HTTP nova ou alterada |
| III. Google + whitelist + admin | PASS | Auth inalterada; atalho Admin continua só se `me.profile()?.admin`; `adminGuard` permanece |
| IV. Testes de API, sem E2E gate | PASS | Sem mudança de API → sem novos testes de API; sem E2E como gate |
| V. Mobile+Web, Docker, Ubuntu | PASS | Estratégia responsiva aprovada na spec (hambúrguer telefone / barra tablet+PC); deploy inalterado |

**Pós-Phase 1**: PASS mantido — design não altera backend, contratos HTTP nem
introduz complexidade (broker/cache/CDK). Contrato desta feature é de UI
([contracts/ui-layout-nav.md](./contracts/ui-layout-nav.md)).

## Project Structure

### Documentation (this feature)

```text
specs/005-hamburger-menu/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── ui-layout-nav.md
└── tasks.md             # (/speckit-tasks — não criado aqui)
```

### Source Code (repository root)

```text
frontend/
└── src/app/shared/
    └── layout.component.ts    # único ponto de mudança (template, estilos, estado do menu)

# Inalterado nesta feature:
frontend/src/app/app.routes.ts           # login/bloqueado já fora do layout
frontend/src/app/core/auth/              # guards, logout
frontend/src/app/features/               # telas de destino
backend/
deploy/
```

**Structure Decision**: Alterar apenas `LayoutComponent` já usado pelo layout
autenticado. Sem novo componente, sem CDK, sem migração DB, sem OpenAPI delta.
Rotas existentes já isolam login e bloqueado.

## Complexity Tracking

> Sem violações constitutivas — tabela não aplicável.
