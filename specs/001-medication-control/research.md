# Research: Controle de Medicamentos

**Feature**: `001-medication-control` | **Date**: 2026-08-15

## 1. Autenticação Google + whitelist

**Decision**: Quarkus OIDC com provedor Google; após token válido, o backend
consulta a tabela `whitelist_entries` pelo e-mail; se ausente, retorna 403.
Admin = e-mail igual a `ADMIN_EMAIL` (env). Frontend usa fluxo OIDC (code +
PKCE) via Quarkus ou biblioteca Angular alinhada ao endpoint de autenticação.

**Rationale**: Constitution exige Google + whitelist; OIDC é o caminho nativo
Quarkus; e-mail admin em env evita elevação via UI.

**Alternatives considered**:
- Auth só no frontend (inseguro — rejeitado)
- Lista admin na whitelist com flag `is_admin` sem env (rejeitado pela
  clarificação: admin fixo em config)

## 2. Notificações mobile (push)

**Decision**: Progressive Web App (Angular service worker) + Web Push (VAPID).
Backend armazena `push_subscriptions` por usuário; job Quarkus Scheduler
dispara payloads no horário da dose (fuso `APP_TIMEZONE`). O cliente envia
`isMobile` no registro (desktop=`false`, mobile=`true`); o servidor não usa UA
como fonte de verdade. Cada tentativa de envio grava `NotificationLog` para
auditoria e SC-002.

**Rationale**: Stack Angular única; atende “só celular” e “todos os celulares
registrados”; sem apps nativos.

**Alternatives considered**:
- Capacitor + FCM (mais complexo, fora do mínimo)
- E-mail/SMS (fora do escopo da spec)
- Polling (não atende app em background / SC-002)

**Nota operacional**: Em iOS, Web Push requer PWA adicionada à tela inicial
(Safari ≥ 16.4). Documentar no quickstart.

## 3. Geração e ciclo de vida das doses

**Decision**: Scheduler periódico (ex.: a cada minuto) materializa ocorrências
`PENDING` para agendas `ACTIVE` no horário atual ± janela; envia push; após 2h
sem ação e sem `scheduled_at` futuro de adiamento, marca `SKIPPED`. Tomada
incrementa contador de doses tomadas da agenda; se `max_taken_doses` atingido,
agenda → `COMPLETED`. Adiar atualiza `scheduled_at` (+10/30/60) e mantém
`PENDING`.

**Rationale**: Modelo explícito de ocorrências simplifica histórico,
idempotência de push e ação multi-dispositivo (uma dose, muitos devices).

**Alternatives considered**:
- Calcular doses só on-the-fly sem persistir (histórico/adiamento mais frágeis)
- Message broker (overkill para escala whitelist)

## 4. Fuso horário

**Decision**: `APP_TIMEZONE` (ex.: `America/Sao_Paulo`) em env; todos os
horários de agenda armazenados como `LocalTime` + dias da semana interpretados
nesse fuso; instantes de dose em `timestamptz` derivados no servidor.

**Rationale**: Clarificação C da sessão; previsível e simples.

**Alternatives considered**: Fuso por usuário / por dispositivo (rejeitados).

## 5. Persistência e migrações

**Decision**: PostgreSQL 16 + Flyway no Quarkus; Panache entities.

**Rationale**: Constitution; migrações versionadas evitam drift.

**Alternatives considered**: Hibernate `update` em prod (rejeitado).

## 6. API e contratos

**Decision**: REST JSON sob `/api/v1/...`; OpenAPI 3 em `contracts/openapi.yaml`
como fonte de verdade da fase de design; testes de API cobrem authz, whitelist,
CRUD medicamentos/agendas, ações de dose, dispositivos e admin.

**Rationale**: Constitution II + IV.

**Alternatives considered**: GraphQL (desnecessário).

## 7. Deploy

**Decision**: `docker-compose` com serviços `db`, `backend`, `frontend` (+
opcional `proxy` TLS). Variáveis: Google OIDC, `ADMIN_EMAIL`, `APP_TIMEZONE`,
VAPID keys, DB URL.

**Rationale**: Constitution V; Ubuntu Server na rede local com acesso externo.

**Alternatives considered**: Deploy bare-metal JVM (rejeitado pela constitution).

## 8. Aviso de compra

**Decision**: Endpoint/consulta `GET /api/v1/alerts/purchase` (ou embutido no
listagem de medicamentos com flag `purchaseNeeded`); UI exibe banner/lista;
nenhum push.

**Rationale**: Clarificação B; limiar padrão 7 doses restantes.

**Alternatives considered**: Push de compra (rejeitado).
