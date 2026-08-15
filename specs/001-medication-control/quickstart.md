# Quickstart: Controle de Medicamentos

**Feature**: `001-medication-control` | **Date**: 2026-08-15

Guia de validação manual/automatizada após implementação. Detalhes de modelo:
[data-model.md](./data-model.md). Contrato HTTP: [contracts/openapi.yaml](./contracts/openapi.yaml).

## Prerequisites

- Docker + Docker Compose
- Conta Google OAuth (Client ID/Secret) com redirect URIs do ambiente
- Par de chaves VAPID para Web Push
- Celular com navegador capaz de PWA/push (Android Chrome; iOS Safari com app
  na tela inicial, ≥ 16.4)
- Variáveis (ver `deploy/.env.example` quando existir):
  - `ADMIN_EMAIL`
  - `APP_TIMEZONE` (ex.: `America/Sao_Paulo`)
  - Google OIDC + DB + VAPID

## Setup

```bash
# Na raiz do repositório (após código gerado)
cp deploy/.env.example deploy/.env
# Editar secrets e ADMIN_EMAIL

docker compose -f deploy/docker-compose.yml up --build -d
```

Esperado: serviços `db`, `backend`, `frontend` healthy; UI em HTTPS/HTTP
conforme proxy.

## Validação — Auth e whitelist (P1)

1. Abrir app na web → Login Google com e-mail **fora** da whitelist → acesso
   negado (403 / tela de bloqueio).
2. Como admin (`ADMIN_EMAIL`, já seedado na whitelist), abrir **Admin >
   Whitelist** → adicionar e-mail de teste.
3. Login com e-mail de teste → acesso OK.
4. Usuário não-admin tenta `/admin/whitelist` → 403.
5. Rodar testes de API do backend cobrindo os casos acima.

```bash
docker compose -f deploy/docker-compose.yml exec backend ./mvnw test
# ou comando de teste equivalente do módulo Quarkus
```

## Validação — Catálogo e agenda (P1)

1. Criar medicamento “Dipirona” com estoque e limiar.
2. Criar agenda: todos os dias, horário daqui a ~2 min, duração indefinida.
3. Criar segunda agenda reutilizando o mesmo medicamento (sem recadastrar).
4. Pausar e reativar agenda → status coerente na UI e na API.

## Validação — Push e ações de dose (P1)

1. No **celular**, instalar/abrir PWA, permitir notificações → dispositivo
   aparece em `GET /api/v1/devices` com `isMobile: true` (cliente enviou a flag).
2. Na web desktop, abrir o app → se registrar device, `isMobile: false` (sem
   push de dose).
3. No horário agendado (±1 min), push chega em **todos** os celulares
   (`isMobile: true`).
4. Em um celular: **Tomar** → dose `TAKEN`; estoque reduz; outros devices não
   exigem ação.
5. Nova dose: **Adiar** 10 → depois 60 → `scheduledAt` atualiza; push no novo
   horário; sem limite de adiamentos.
6. Nova dose: ignorar >2h → status `SKIPPED` automaticamente.
7. Agenda `FIXED_TAKEN_DOSES` com N=2: duas tomadas → agenda `COMPLETED`;
   puladas não contam para N.
8. **SC-002**: gerar ≥10 envios bem-sucedidos em teste controlado; consultar
   `notification_log` e confirmar ≥95% com `sent_at - scheduled_at ≤ 60 seconds`.

```sql
-- Exemplo de checagem SC-002 (ajustar nomes de coluna se necessário)
SELECT
  COUNT(*) FILTER (WHERE success AND sent_at <= scheduled_at + interval '1 minute') * 100.0
  / NULLIF(COUNT(*) FILTER (WHERE success), 0) AS pct_within_1min
FROM notification_log
WHERE sent_at > now() - interval '1 day';
```

## Validação — Estoque e acompanhamento (P2)

1. Ajustar estoque para abaixo do limiar → `GET /api/v1/alerts/purchase`
   retorna o item; UI Mobile **e** Web mostram aviso; **sem** push de compra.
2. Tela de acompanhamento lista `TAKEN` e `SKIPPED`; filtro por medicamento OK.

## Expected outcomes (definição de pronto da validação)

| Critério | Evidência |
|----------|-----------|
| SC-004 whitelist | Login fora da lista bloqueado |
| SC-002 push | ≥95% dos `NotificationLog` success ≤1 min (SQL acima) |
| SC-003 ações | Take/skip/snooze < 30s |
| SC-006 compra | Só in-app |
| SC-008 reuso | Nova agenda + reativação |
| Constitution IV | `./mvnw test` (API) verde |

## Notes

- Fuso exibido na UI deve refletir `APP_TIMEZONE` (`GET /me`).
- Não usar suíte E2E de UI como gate de entrega.
