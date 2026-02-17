# Design System - AI$HA

## Objetivo
Documentar o design system atualmente em uso e definir uma nova paleta de cores semântica baseada nas cores:
- `#07004d`
- `#19647e` (secundária)
- `#42e2b8`
- `#f0ec57` (principal)
- `#eb8a90`

## Estado atual (já implementado)

### Tecnologias e padrões de UI
- Renderização server-side com Thymeleaf.
- Interatividade orientada a HTMX para telas de listagem/formulário.
- CSS centralizado em `src/main/resources/static/css/app.css`.
- Fonte principal: `"Source Sans 3", "Segoe UI", sans-serif`.
- Idioma da interface: pt-BR.

### Estrutura visual atual
- Fundo geral com gradiente claro (`body`) e cartões brancos.
- Layout principal com `wrap` centralizado e largura máxima.
- Componentes reutilizáveis:
  - Topbar + navegação + filtro de período (`fragments/header.html`)
  - Cartões (`.card`)
  - Botões (`.btn-primary`, `.btn-secondary`, `.btn-danger`)
  - Tabelas responsivas com fallback em cards no mobile (`.responsive-list`)
  - Estados de loading/skeleton para dashboard.

### Tokens de cor atuais (em uso)
Definidos hoje no `:root` de `app.css`:
- `--bg: #f4f7f9`
- `--card: #ffffff`
- `--text: #12212f`
- `--muted: #5a6b7b`
- `--line: #d6dde5`
- `--primary: #066e8a`
- `--primary-hover: #05566c`
- `--secondary: #e8eef2`
- `--secondary-hover: #dae4eb`
- `--danger: #b22e39`
- `--danger-hover: #8f1f29`

### Cores de gráficos atuais (hardcoded no dashboard)
- Linha de saldo: tons de azul (`#0b7fab`, `rgba(11,127,171,0.14)`).
- Receitas vs despesas: verde (`#1b7e4b`) e vermelho (`#9e2f2b`).
- Donut/stacked por categoria: paleta mista (`#0b7fab`, `#1f7346`, `#a75f00`, `#7a4c8e`, `#ba3f32`, `#64748b`, ...).

Observação: atualmente há mistura entre tokens CSS e cores literais em CSS/JS.

## Nova paleta semântica (definida agora)

## 1) Cores-base da marca
- `brand-primary`: `#f0ec57` (amarelo principal)
- `brand-secondary`: `#19647e` (azul petróleo secundário)
- `brand-deep`: `#07004d` (azul profundo para contraste/identidade)
- `brand-mint`: `#42e2b8` (realce positivo)
- `brand-coral`: `#eb8a90` (realce de atenção)

## 2) Escala de suporte (mesma linha cromática)
- `brand-primary-100`: `#fffde1`
- `brand-primary-200`: `#fbf89d`
- `brand-primary-300`: `#f0ec57`
- `brand-primary-400`: `#d8d33b`
- `brand-primary-500`: `#b5b126`

- `brand-secondary-100`: `#e8f3f7`
- `brand-secondary-200`: `#8cb7c7`
- `brand-secondary-300`: `#19647e`
- `brand-secondary-400`: `#145267`
- `brand-secondary-500`: `#103f50`

- `brand-deep-100`: `#d6d4e8`
- `brand-deep-200`: `#8f89b8`
- `brand-deep-300`: `#07004d`
- `brand-deep-400`: `#05003d`

- `brand-mint-100`: `#e6fcf6`
- `brand-mint-200`: `#9af0da`
- `brand-mint-300`: `#42e2b8`
- `brand-mint-400`: `#22bf96`

- `brand-coral-100`: `#fdecef`
- `brand-coral-200`: `#f5b5bb`
- `brand-coral-300`: `#eb8a90`
- `brand-coral-400`: `#cf6770`

## 3) Papéis semânticos de cor

### 3.1 Superfícies e bordas
- `color-bg-app`: `#f7f9fc` (fundo global)
- `color-bg-subtle`: `#eef3f7` (áreas secundárias)
- `color-bg-card`: `#ffffff` (cartões)
- `color-bg-elevated`: `#ffffff` (modais/dropdowns)
- `color-border-default`: `#d7e0e8`
- `color-border-strong`: `#b8c7d4`
- `color-overlay`: `rgba(7, 0, 77, 0.45)`

### 3.2 Tipografia
- `color-text-primary`: `#101f33`
- `color-text-secondary`: `#35506a`
- `color-text-muted`: `#5e7388`
- `color-text-on-primary`: `#07004d` (sobre `#f0ec57`)
- `color-text-on-secondary`: `#ffffff` (sobre `#19647e`)
- `color-text-on-dark`: `#f7f9fc`
- `color-link`: `#145267`
- `color-link-hover`: `#07004d`

### 3.3 Ações e componentes interativos
- `color-action-primary-bg`: `#f0ec57`
- `color-action-primary-hover`: `#d8d33b`
- `color-action-primary-active`: `#b5b126`
- `color-action-primary-text`: `#07004d`

- `color-action-secondary-bg`: `#19647e`
- `color-action-secondary-hover`: `#145267`
- `color-action-secondary-active`: `#103f50`
- `color-action-secondary-text`: `#ffffff`

- `color-action-tertiary-bg`: `#e8f3f7`
- `color-action-tertiary-hover`: `#d4e8f0`
- `color-action-tertiary-text`: `#145267`

- `color-focus-ring`: `#42e2b8`
- `color-selection`: `#fbf89d`
- `color-disabled-bg`: `#e6ebf0`
- `color-disabled-text`: `#8b99a6`

### 3.4 Feedback e estados do domínio
- `color-success`: `#22bf96`
- `color-success-bg`: `#e6fcf6`
- `color-success-text`: `#0d6b54`

- `color-warning`: `#d8a600`
- `color-warning-bg`: `#fff8d6`
- `color-warning-text`: `#6a5300`

- `color-danger`: `#cf6770`
- `color-danger-bg`: `#fdecef`
- `color-danger-text`: `#7e2f3a`

- `color-info`: `#19647e`
- `color-info-bg`: `#e8f3f7`
- `color-info-text`: `#103f50`

## 4) Mapeamento por elemento da interface
- Topbar: fundo translúcido claro (`color-bg-card` + blur), links em `color-link`.
- Navegação ativa: texto em `color-text-on-secondary` com fundo `color-action-secondary-bg`.
- Botão primário: fundo `color-action-primary-bg`, texto `color-action-primary-text`.
- Botão secundário: fundo `color-action-secondary-bg`, texto branco.
- Botão neutro/apoio: `color-action-tertiary-*`.
- Ações destrutivas: `color-danger` e variações.
- Inputs: fundo branco, borda `color-border-default`, foco com `color-focus-ring`.
- Cards de resumo:
  - Saldo: base `color-info-bg` + acento `color-info`
  - Receitas: base `color-success-bg` + acento `color-success`
  - Despesas: base `color-danger-bg` + acento `color-danger`
- Tabelas: cabeçalho com texto `color-text-muted`, linhas com `color-border-default`.
- Skeleton/loading: tons de `color-bg-subtle`.

## 5) Paleta para gráficos

### 5.1 Séries principais (ordem sugerida)
1. `#19647e` (principal comparativo)
2. `#42e2b8` (positivo)
3. `#eb8a90` (alerta/negativo)
4. `#07004d` (apoio de contraste)
5. `#f0ec57` (destaque)
6. `#8cb7c7`
7. `#22bf96`
8. `#cf6770`

### 5.2 Convenções por tipo
- Saldo acumulado (linha/área): linha `#19647e`, área `rgba(25,100,126,0.16)`.
- Receitas vs despesas:
  - Receitas: `#22bf96`
  - Despesas: `#cf6770`
- Donut por categoria: usar a sequência da seção 5.1.
- Sem dados: `#d7e0e8`.
- Grade/axis labels: `#5e7388`.

## 6) Tokens CSS propostos (referência)
```css
:root {
  --color-bg-app: #f7f9fc;
  --color-bg-card: #ffffff;
  --color-text-primary: #101f33;
  --color-text-secondary: #35506a;
  --color-border-default: #d7e0e8;

  --color-action-primary-bg: #f0ec57;
  --color-action-primary-hover: #d8d33b;
  --color-action-primary-text: #07004d;

  --color-action-secondary-bg: #19647e;
  --color-action-secondary-hover: #145267;
  --color-action-secondary-text: #ffffff;

  --color-success: #22bf96;
  --color-warning: #d8a600;
  --color-danger: #cf6770;
  --color-focus-ring: #42e2b8;
}
```

## 7) Diretrizes de adoção
- Centralizar todas as cores em tokens CSS (eliminar hex hardcoded em templates/JS).
- Sincronizar gráficos com constantes de paleta compartilhadas.
- Garantir contraste mínimo WCAG AA para textos e botões.
- Preservar os padrões responsivos já existentes (tabela para cards no mobile).
- Em evolução futura: mover tokens para tema único e preparar variantes sazonais sem alterar semântica.

## 8) Decisões registradas
- Cor principal oficial: `#f0ec57`.
- Cor secundária oficial: `#19647e`.
- `#07004d` será a base de contraste forte para tipografia sobre superfícies claras e elementos de identidade.
- `#42e2b8` e `#eb8a90` serão usados como acentos funcionais (positivo/atenção) e também em gráficos.
