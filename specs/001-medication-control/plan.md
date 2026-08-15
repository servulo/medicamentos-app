# Implementation Plan: Controle de Medicamentos

**Branch**: `001-medication-control` | **Date**: 2026-08-15 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/001-medication-control/spec.md`

## Summary

Aplicação multiplataforma (Angular responsivo PWA + Quarkus/Java + PostgreSQL
em Docker) para catálogo de medicamentos, agendas de tomada, lembretes push
apenas em celulares registrados, registro de tomada/pulada/adiamento (10/30/60
min, ilimitado), estoque com aviso in-app de compra, acompanhamento e acesso
restrito via Google OAuth + whitelist (admin por e-mail de ambiente). Fuso
horário único configurável no ambiente.

## Technical Context

**Language/Version**: Java 21 (Quarkus), TypeScript (Angular 19.x LTS ou estável
atual do projeto)

**Primary Dependencies**: Quarkus (REST, Hibernate ORM with Panache, OIDC
Google, Scheduler, SmallRye Health); Angular (standalone, router, HttpClient);
Angular Service Worker / Web Push (VAPID) para notificações mobile; PostgreSQL
driver; Docker Compose

**Storage**: PostgreSQL 16

**Testing**: Quarkus `@QuarkusTest` + REST Assured para testes de API
(obrigatórios). Sem E2E de UI como gate. Testes unitários Angular opcionais.

**Target Platform**: Ubuntu Server (containers); clientes Web desktop e Mobile
(navegador/PWA). Push de dose apenas em dispositivos mobile registrados.

**Project Type**: Web application (frontend + backend + database)

**Performance Goals**: Lembrete de dose entregue em ≤1 min do horário agendado
(SC-002); ações de tomada/pulada/adiamento responsivas (<30s percepção usuário)

**Constraints**: Segredos só via env; backend único acesso ao DB; API HTTP
documentada; HTTPS na exposição externa; fuso fixo `APP_TIMEZONE`; admin
`ADMIN_EMAIL`; push só mobile; aviso de compra só in-app

**Scale/Scope**: Poucos usuários (whitelist); dezenas de medicamentos/agendas
por usuário; volume baixo de doses/dia — arquitetura simples sem message broker

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Princípio | Status | Evidência no plano |
|-----------|--------|-------------------|
| I. Propósito medicamentos/lembretes | PASS | Escopo = controle + lembretes + adesão |
| II. Camadas Angular ↔ API ↔ Quarkus ↔ PG | PASS | Estrutura `frontend/` + `backend/`; contratos OpenAPI |
| III. Google + whitelist + admin | PASS | OIDC Google, tabela whitelist, `ADMIN_EMAIL` |
| IV. Testes de API, sem E2E gate | PASS | REST Assured nos endpoints; sem Cypress/Playwright obrigatório |
| V. Mobile+Web, Docker, Ubuntu | PASS | PWA responsivo + Compose; deploy containers |

**Pós-Phase 1**: PASS mantido — design não introduz broker/cache nem bypass de
whitelist; Web Push justificado em Complexity Tracking.

## Project Structure

### Documentation (this feature)

```text
specs/001-medication-control/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── openapi.yaml
└── tasks.md             # (/speckit-tasks — não criado aqui)
```

### Source Code (repository root)

```text
backend/
├── src/main/java/app/medicamentos/
│   ├── auth/            # OIDC, whitelist, admin guard
│   ├── medication/      # catálogo, estoque
│   ├── schedule/        # agendas, horários, dias
│   ├── dose/            # ocorrências, tomada/pulada/adiamento
│   ├── device/          # registro push mobile
│   ├── notify/          # scheduler + Web Push
│   └── admin/           # CRUD whitelist
├── src/main/resources/
│   ├── application.properties
│   └── db/migration/    # Flyway
└── src/test/java/       # testes de API

frontend/
├── src/app/
│   ├── core/            # auth interceptors, guards
│   ├── features/
│   │   ├── auth/
│   │   ├── medications/
│   │   ├── schedules/
│   │   ├── doses/
│   │   ├── adherence/
│   │   ├── alerts/      # aviso compra in-app
│   │   └── admin/       # whitelist (só admin)
│   └── shared/
├── ngsw-config.json     # service worker / push
└── Dockerfile

deploy/
├── docker-compose.yml
├── .env.example
└── nginx/               # TLS reverse proxy (produção)

specs/001-medication-control/   # artefatos Spec Kit
```

**Structure Decision**: Monorepo com `frontend/` (Angular PWA), `backend/`
(Quarkus) e `deploy/` (Compose + proxy). Alinha à constitution (camadas
separadas, tudo em containers).

## Complexity Tracking

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| Web Push (VAPID) + service worker além da API REST | Spec exige notificação só no celular, multi-dispositivo | Polling no app não atende SC-002 com app fechado; app nativo (Capacitor/FCM) adiciona duas plataformas fora da stack Angular-only |
