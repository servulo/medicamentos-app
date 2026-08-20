# Deploy no Ubuntu Server com Cloudflare Tunnel

Runbook para baixar a aplicação do GitHub, configurar, subir os containers e
liberar o acesso externo via **Cloudflare Tunnel**. O Tunnel publica HTTPS na
internet **sem** abrir porta no roteador e **sem** IP público.

Repositório: [https://github.com/servulo/medicamentos-app](https://github.com/servulo/medicamentos-app)

A aplicação publicada está na branch **`001-medication-control`**. A branch
`main` ainda não contém o código da app.

---

## Arquitetura deste deploy

```
Internet (HTTPS)
    → Cloudflare (TLS + DNS)
        → Tunnel (cloudflared no Ubuntu)
            → nginx proxy em 127.0.0.1:8088
                → frontend (Angular) em /
                → backend (Quarkus) em /api/
                    → PostgreSQL (rede interna do Compose)
```

Serviços do Compose (`deploy/docker-compose.yml`):

| Serviço    | Função                         | Porta no host (Compose atual) |
|------------|--------------------------------|-------------------------------|
| `db`       | PostgreSQL 16                  | `5433` → 5432 (**não expor**) |
| `backend`  | API Quarkus                    | `8080`                        |
| `frontend` | UI Angular                     | `4200`                        |
| `proxy`    | nginx (`/` + `/api/`)          | `8088` (HTTP) e `8443` (TLS)  |

O serviço `proxy` só sobe com o perfil `tls`. Com Cloudflare, o TLS fica na
Cloudflare; o nginx local continua em **HTTP** na porta **8088**. Não use
`https://127.0.0.1:8443` — o nginx do repositório ainda não tem certificado local.

---

## 0. Pré-requisitos

- Ubuntu Server com acesso SSH
- Conta GitHub e permissão de leitura no repositório
- Conta Cloudflare com um **domínio** cujo DNS é gerenciado pela Cloudflare
- Hostname escolhido, por exemplo `app.seudominio.com`

O `cloudflared` só precisa **sair** na internet (HTTPS 443). Não é necessário
port forwarding, IP público nem abrir 80/443 no `ufw`.

---

## 1. Preparar o Ubuntu (Docker + Git)

```bash
sudo apt update
sudo apt install -y ca-certificates curl git
sudo install -m 0755 -d /etc/apt/keyrings
sudo curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/ubuntu $(. /etc/os-release && echo "$VERSION_CODENAME") stable" | sudo tee /etc/apt/sources.list.d/docker.list
sudo apt update
sudo apt install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin
sudo usermod -aG docker "$USER"
```

Saia e entre de novo no SSH para o grupo `docker` valer. Confira:

```bash
docker version
docker compose version
```

Firewall: deixe só SSH. **Não** abra 80, 443, 4200, 8080, 8088 ou 5433 — o
Tunnel não precisa disso.

```bash
sudo ufw allow OpenSSH
sudo ufw enable
sudo ufw status
```

---

## 2. Baixar a aplicação do GitHub

### Repositório público

```bash
cd ~
git clone -b 001-medication-control https://github.com/servulo/medicamentos-app.git
cd medicamentos-app
```

### Repositório privado

No GitHub, crie um Fine-grained PAT com permissão `Contents: Read` e clone:

```bash
git clone -b 001-medication-control https://<SEU_TOKEN>@github.com/servulo/medicamentos-app.git
cd medicamentos-app
```

Alternativa: chave SSH (`git@github.com:servulo/medicamentos-app.git`) com
deploy key ou chave do usuário.

Confira se existem as pastas `backend/`, `frontend/` e `deploy/`.

---

## 3. Ajuste obrigatório da URL da API

O frontend em `frontend/src/environments/environment.ts` chama, por padrão:

```ts
apiBaseUrl: 'http://localhost:8080/api/v1'
```

No celular ou PC remoto, `localhost` é o **aparelho do usuário**, não o
servidor. Com Tunnel + nginx, a API deve usar o **mesmo hostname público**.

Edite `frontend/src/environments/environment.ts` **antes** do `docker compose
up --build`:

```ts
export const environment = {
  production: true,
  apiBaseUrl: '/api/v1',
  useTestAuth: true
};
```

`/api/v1` é relativo: o browser pede `https://app.seudominio.com/api/v1/...`,
o Tunnel entrega isso ao nginx, e o nginx encaminha `/api/` para o backend.

Esse valor entra na **imagem Docker**. Qualquer mudança posterior exige rebuild
do serviço `frontend`.

---

## 4. Configurar o ambiente

```bash
cp deploy/.env.example deploy/.env
nano deploy/.env
```

Use o **mesmo hostname** que será criado no Cloudflare:

```env
ADMIN_EMAIL=seu-email@gmail.com
APP_TIMEZONE=America/Sao_Paulo
APP_AUTH_TEST_MODE=true
OIDC_ENABLED=false
VAPID_PUBLIC_KEY=not-configured
VAPID_PRIVATE_KEY=not-configured
VAPID_SUBJECT=mailto:seu-email@gmail.com
CORS_ORIGINS=https://app.seudominio.com
```

| Variável | Função |
|----------|--------|
| `ADMIN_EMAIL` | Administrador; já entra na whitelist no seed |
| `APP_TIMEZONE` | Fuso único da aplicação |
| `APP_AUTH_TEST_MODE` | `true` = UI envia `X-Test-User-Email` (modo atual) |
| `OIDC_ENABLED` | Login Google no backend; deixe `false` até a UI OIDC estar pronta |
| `CORS_ORIGINS` | Origem HTTPS pública (`https://app.seudominio.com`) |
| `VAPID_*` | Web Push; opcional neste primeiro deploy |

Para o primeiro acesso externo, o modo teste é o que a UI atual realmente usa.
Login Google de produção ainda não está completo no frontend.

Não commite `deploy/.env` (contém e-mail e, depois, segredos).

---

## 5. Subir os containers (com o proxy)

Na raiz do repositório:

```bash
cd ~/medicamentos-app
docker compose --profile tls -f deploy/docker-compose.yml --env-file deploy/.env up --build -d
docker compose --profile tls -f deploy/docker-compose.yml ps
```

O `--profile tls` é obrigatório para criar o container `proxy`. Sem ele, o
Tunnel não tem um único ponto de entrada (`/` + `/api/`).

Teste **somente no próprio servidor**:

```bash
curl -sI http://127.0.0.1:8088
curl -sI http://127.0.0.1:8088/api/v1/me
```

- Frontend: resposta HTTP `200`
- API: pode responder `401` (sem e-mail de teste) — isso indica que o proxy
  chegou no backend

### Endurecer portas (recomendado)

No `deploy/docker-compose.yml`, publique o proxy só em localhost para a LAN e
a internet não baterem direto nas portas:

```yaml
proxy:
  ports:
    - "127.0.0.1:8088:80"
```

Depois:

```bash
docker compose --profile tls -f deploy/docker-compose.yml --env-file deploy/.env up -d
```

Não exponha `5433` (Postgres) para fora. Frontend `4200` e backend `8080` não
são necessários para o Tunnel; podem ficar só para debug local no servidor.

---

## 6. Cloudflare Tunnel

Duas formas: **Zero Trust (token)** — mais simples em servidor sem navegador —
ou **CLI** com arquivo de configuração.

### 6.1 Instalar o `cloudflared`

```bash
sudo mkdir -p --mode=0755 /usr/share/keyrings
curl -fsSL https://pkg.cloudflare.com/cloudflare-main.gpg | sudo tee /usr/share/keyrings/cloudflare-main.gpg >/dev/null
echo 'deb [signed-by=/usr/share/keyrings/cloudflare-main.gpg] https://pkg.cloudflare.com/cloudflared any main' | sudo tee /etc/apt/sources.list.d/cloudflared.list
sudo apt-get update
sudo apt-get install -y cloudflared
cloudflared --version
```

### 6.2 Criar o túnel no painel (recomendado)

1. Abra [Cloudflare Zero Trust](https://one.dash.cloudflare.com/)
2. **Networks → Tunnels → Create a tunnel**
3. Tipo: **Cloudflared**
4. Nome, por exemplo `medicamentos`
5. Copie o comando de instalação com token (formato
   `sudo cloudflared service install eyJ...`) e execute **no Ubuntu**

Isso instala o serviço systemd. Confira:

```bash
sudo systemctl status cloudflared
sudo systemctl enable cloudflared
```

No painel o túnel deve ficar **Healthy**.

### 6.3 Hostname público → nginx local

No túnel criado: **Public Hostname → Add**:

| Campo     | Valor                         |
|-----------|-------------------------------|
| Subdomain | `app` (ou o que quiser)       |
| Domain    | `seudominio.com`              |
| Type      | `HTTP`                        |
| URL       | `http://127.0.0.1:8088`       |

Salve. A Cloudflare cria o DNS (CNAME para o túnel) e termina HTTPS em
`https://app.seudominio.com`.

Não aponte o hostname para `https://127.0.0.1:8443`.

### 6.4 Alternativa: Tunnel só por CLI

```bash
cloudflared tunnel login
cloudflared tunnel create medicamentos
cloudflared tunnel route dns medicamentos app.seudominio.com
```

O login abre o browser; em servidor headless, autorize a partir de outro
computador com o URL exibido no terminal, ou use o método do token (6.2).

Crie `/etc/cloudflared/config.yml` (troque `TUNNEL_ID` e o usuário):

```yaml
tunnel: TUNNEL_ID
credentials-file: /home/SEU_USUARIO/.cloudflared/TUNNEL_ID.json
ingress:
  - hostname: app.seudominio.com
    service: http://127.0.0.1:8088
  - service: http_status:404
```

```bash
sudo cloudflared service install
sudo systemctl enable --now cloudflared
sudo systemctl status cloudflared
```

---

## 7. Validar o acesso externo

No celular (4G, fora do Wi‑Fi de casa) abra `https://app.seudominio.com`:

1. A UI carrega (não fica em branco nem em “API indisponível”).
2. Login de teste com o e-mail definido em `ADMIN_EMAIL`.
3. Em **Admin → Whitelist**, cadastre os outros e-mails autorizados.
4. No DevTools do browser (aba Network), as chamadas devem ir para
   `https://app.seudominio.com/api/v1/...`, **não** para `localhost:8080`.

Se a UI abre mas a API falha, quase sempre a imagem do frontend ainda tem
`apiBaseUrl` antigo. Corrija `environment.ts` e reconstrua:

```bash
docker compose --profile tls -f deploy/docker-compose.yml --env-file deploy/.env up --build -d frontend
```

---

## 8. Atualizar depois de um push no GitHub

```bash
cd ~/medicamentos-app
git pull origin 001-medication-control
docker compose --profile tls -f deploy/docker-compose.yml --env-file deploy/.env up --build -d
```

O Tunnel continua no ar; não é preciso recriá-lo.

Comandos úteis:

```bash
# Logs
docker compose --profile tls -f deploy/docker-compose.yml logs -f --tail=100
docker compose --profile tls -f deploy/docker-compose.yml logs -f backend

# Parar
docker compose --profile tls -f deploy/docker-compose.yml down

# Parar e apagar o volume do Postgres (apaga dados)
docker compose --profile tls -f deploy/docker-compose.yml down -v
```

---

## 9. Depois que o Tunnel estiver estável

- **Web Push:** gere chaves VAPID, coloque em `deploy/.env` e recrie o backend.
  No iOS, a PWA precisa estar na tela inicial (Safari ≥ 16.4).
- **Produção Google:** só quando o login Google na UI estiver pronto. Aí
  `APP_AUTH_TEST_MODE=false`, `OIDC_ENABLED=true`, e no Google Cloud as
  Authorized JavaScript origins / redirect URIs devem ser
  `https://app.seudominio.com`. O `docker-compose.yml` atual ainda não passa
  `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET` ao backend; isso precisa ser
  adicionado na hora de ligar o OIDC.
- **Modo teste na internet:** `APP_AUTH_TEST_MODE=true` não substitui o Google.
  Qualquer pessoa que conheça um e-mail da whitelist consegue o cabeçalho de
  teste. Use só para validar o Tunnel; não trate como segurança definitiva.

---

## 10. Problemas comuns

| Sintoma | Causa provável | O que fazer |
|---------|----------------|-------------|
| Túnel **Inactive** / **Down** | `cloudflared` parado | `sudo systemctl status cloudflared` e `journalctl -u cloudflared -n 50` |
| `502` no hostname | nginx/Compose fora do ar, ou URL do Tunnel errada | `docker compose ... ps`; Public Hostname deve ser `http://127.0.0.1:8088` |
| UI abre, “API indisponível” | `apiBaseUrl` ainda é `localhost:8080` | Passo 3 + rebuild do `frontend` |
| CORS no browser | `CORS_ORIGINS` sem o HTTPS público | Ajuste `deploy/.env` e recrie o `backend` |
| Clone vazio / sem `backend/` | Branch `main` em vez de `001-medication-control` | `git clone -b 001-medication-control ...` |
| `permission denied` no Docker | usuário fora do grupo `docker` | `sudo usermod -aG docker $USER` e novo login SSH |
| `401` em `/api/v1/me` no `curl` local | esperado sem e-mail de teste | Confirma que o proxy chegou no backend |

---

## Resumo

1. Instale Docker e Git no Ubuntu; `ufw` só com SSH.
2. Clone a branch `001-medication-control`.
3. Altere `apiBaseUrl` para `/api/v1`.
4. Configure `deploy/.env` com `CORS_ORIGINS=https://app.seudominio.com`.
5. Suba com `docker compose --profile tls ... up --build -d`.
6. Instale o `cloudflared` e aponte o hostname para `http://127.0.0.1:8088`.

Não é necessário abrir porta no roteador.
