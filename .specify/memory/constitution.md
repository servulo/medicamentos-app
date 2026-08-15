<!--
Sync Impact Report
- Version change: (template placeholders) → 1.0.0
- Modified principles: N/A (primeira adoção a partir do scaffold)
- Added sections:
  - Core Principles (I–V)
  - Stack e Implantação
  - Fluxo de Desenvolvimento e Qualidade
  - Governance
- Removed sections: N/A
- Follow-up TODOs: nenhum placeholder adiado
-->

# Medicamentos App Constitution

## Core Principles

### I. Propósito: Controle e Lembrete de Medicamentos
O sistema existe para permitir que usuários autenticados registrem medicamentos
e recebam lembretes diários de tomada. Toda feature MUST alinhar-se a esse
propósito; funcionalidades fora desse escopo MUST ser justificadas e aprovadas
via emenda à constitution ou explicitamente aceitas na especificação da feature.
Racional: evita expansão descontrolada e mantém o produto focado na adesão ao
tratamento.

### II. Arquitetura em Camadas e Contratos Claros
O frontend (Angular) e o backend (Quarkus/Java) MUST permanecer desacoplados,
comunicando-se apenas via API HTTP bem definida. O backend MUST ser a única
camada autorizada a acessar o PostgreSQL. Contratos de API MUST ser versionados
ou documentados de forma que mudanças incompatíveis sejam explícitas.
Racional: permite evolução independente de UI e domínio e protege a
integridade dos dados.

### III. Autenticação Google e Whitelist (NÃO NEGOCIÁVEL)
Acesso ao sistema MUST exigir autenticação via login com Google. Somente
identidades presentes na whitelist MUST obter acesso. Identidades fora da
whitelist MUST ser rejeitadas mesmo após autenticação Google bem-sucedida.
A gestão da whitelist MUST ocorrer em tela administrativa do próprio sistema,
disponível exclusivamente ao proprietário/administrador designado. Não é
permitido bypass de whitelist por configuração ad hoc em produção sem registro
na própria whitelist.
Racional: o acesso externo é restrito a pessoas autorizadas; a whitelist é o
controle de autorização do produto.

### IV. Testes de API Obrigatórios (sem E2E)
Toda mudança em endpoints, regras de autenticação/autorização, persistência ou
contratos da API MUST incluir ou atualizar testes de API automatizados no
backend (Quarkus). Testes end-to-end (E2E) de UI NÃO são exigidos por esta
constitution e NÃO MUST bloquear entrega. Cobertura de UI pode existir como
prática opcional, nunca como gate obrigatório.
Racional: valida o comportamento crítico do domínio e da segurança na camada
mais estável, sem o custo e a fragilidade de suítes E2E.

### V. Disponibilidade Multiplataforma e Operação em Containers
A aplicação MUST ser utilizável em Mobile e Web (interface responsiva ou
estratégia equivalente aprovada na spec). Frontend, backend e PostgreSQL MUST
executar em containers Docker. O ambiente de hospedagem alvo é Ubuntu Server
na rede local, com acesso externo permitido sob as regras de autenticação e
whitelist. Mudanças de stack de runtime ou de modelo de deploy MUST ser
tratadas como emenda constitucional.
Racional: garante portabilidade, reprodutibilidade e acesso consistente entre
dispositivos.

## Stack e Implantação

- Frontend: Angular
- Backend: Quarkus / Java
- Persistência: PostgreSQL
- Empacotamento: Docker para frontend, backend e banco
- Hospedagem: Ubuntu Server (mesma rede do ambiente de desenvolvimento Windows),
  com exposição externa sujeita a login Google + whitelist
- Lembretes de medicamentos (canais, horários, falhas de entrega): definidos na
  especificação da feature, não nesta constitution

Segredos, credenciais OAuth e strings de conexão MUST NÃO ser commitados no
repositório. Configuração sensível MUST vir de variáveis de ambiente ou
mecanismo equivalente do ambiente de containers.

## Fluxo de Desenvolvimento e Qualidade

- Specs, planos e tarefas MUST respeitar esta constitution; conflitos MUST ser
  resolvidos em favor da constitution ou via emenda formal.
- Antes de considerar uma mudança de API pronta, os testes de API relevantes
  MUST passar.
- Complexidade adicional (novos serviços, brokers, caches) MUST ser justificada
  por necessidade concreta na spec; preferir a solução mais simples que atenda
  aos princípios.
- Revisões de código/PR MUST verificar: aderência à stack, ausência de bypass
  de whitelist, e presença de testes de API quando a superfície de API muda.

## Governance

Esta constitution prevalece sobre convenções locais, preferências ad hoc e
documentação informal. Emendas MUST:

1. Documentar a motivação e o impacto (princípios afetados, migração se houver).
2. Atualizar a versão segundo SemVer de governança:
   - MAJOR: remoção ou redefinição incompatível de princípios.
   - MINOR: novo princípio/seção ou expansão material de orientação.
   - PATCH: esclarecimentos, redação, correções sem mudança semântica.
3. Atualizar `Last Amended` para a data da alteração (ISO YYYY-MM-DD).
4. Registrar o Sync Impact Report no topo do arquivo.

Compliance: qualquer plano de implementação ou PR que viole um princípio
NÃO NEGOCIÁVEL MUST ser rejeitado até alinhamento ou emenda aprovada.
Dúvidas de interpretação MUST ser esclarecidas com o proprietário do projeto
antes de implementar comportamento ambíguo de autenticação, whitelist ou
acesso externo.

**Version**: 1.0.0 | **Ratified**: 2026-08-15 | **Last Amended**: 2026-08-15
