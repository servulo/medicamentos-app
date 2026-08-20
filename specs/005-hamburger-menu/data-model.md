# Data Model: Menu de navegação em formato hambúrguer

Sem alteração de entidades persistidas. O modelo abaixo é o **estado de UI no
layout autenticado**. Auth, whitelist e rotas continuam conforme
[data-model.md de 001](../001-medication-control/data-model.md).

## Client state: layout autenticado

| Campo | Tipo (conceitual) | Regras |
|-------|-------------------|--------|
| presentationMode | `phone` \| `wide` | Derivado da largura da janela: `phone` se ≤ 767px; `wide` se ≥ 768px. Não gravado. |
| menuOpen | boolean | Só relevante em `phone`. Inicia `false` em todo carregamento. `false` ao cruzar para `wide`, ao navegar, ao fechar pelo botão/overlay. |
| adminVisible | boolean | `true` só se o perfil autenticado for administrador (já carregado por `MeService`). |

### Destinos de navegação (catálogo de UI)

Não persistidos; constantes no layout.

| Rótulo | Rota | Visibilidade |
|--------|------|----------------|
| Medicamentos | `/medicamentos` | Todo autenticado |
| Agendas | `/agendas` | Todo autenticado |
| Doses | `/doses` | Todo autenticado |
| Compras | `/alertas` | Todo autenticado |
| Histórico | `/historico` | Todo autenticado |
| Admin | `/admin/whitelist` | Somente administrador |

Marca “💊 Medicamentos” → `/medicamentos` (sempre visível no cabeçalho).
Sair → encerra sessão (já existente); não é destino de navegação.

### Relacionamentos

- `presentationMode` determina se o usuário vê hambúrguer+painel ou barra.
- `menuOpen` só altera a visibilidade do painel quando `presentationMode = phone`.
- Destino ativo = rota atual (já via `RouterLinkActive`); um destaque por vez.

### Validação (UI)

| Regra | Origem |
|-------|--------|
| Telefone: sem barra permanente; hambúrguer visível | FR-001, FR-003 |
| Tablet/computador: barra permanente; sem hambúrguer | FR-002 |
| Menu inicia fechado no telefone | FR-004 |
| Admin só para administrador | FR-007 |
| Sair e marca sempre no cabeçalho | FR-011, FR-012 |
| Login/bloqueado sem este estado | FR-013 |
| Cruzar para wide fecha o painel | FR-014 |
| Cruzar para phone com menu fechado | FR-015 |

### Transições

```text
[carregamento phone] → menuOpen=false
menuOpen=false --toggle--> menuOpen=true
menuOpen=true  --toggle | overlay | destino | NavigationEnd | wide--> menuOpen=false
presentationMode phone --largura ≥768--> wide (menuOpen forçado false)
presentationMode wide  --largura ≤767--> phone (menuOpen=false)
```

Nenhuma transição de domínio (medicamento, agenda, dose).
