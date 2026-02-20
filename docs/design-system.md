# Design System - AI$HA

## Goal

Document the design system currently in use and define a new semantic color palette based on:

- `#07004d`
- `#19647e` (secondary)
- `#42e2b8`
- `#f0ec57` (primary)
- `#eb8a90`

## Current State (Already Implemented)

### UI technologies and patterns

- Server-side rendering with Thymeleaf.
- HTMX-driven interactivity for list/form screens.
- Centralized CSS in `src/main/resources/static/css/app.css`.
- Primary font: `"Source Sans 3", "Segoe UI", sans-serif`.
- UI language: pt-BR.

### Current visual structure

- Global light gradient background (`body`) and white cards.
- Main layout with centered `wrap` and max width.
- Reusable components:
  - Topbar + navigation + period filter (`fragments/header.html`)
  - Cards (`.card`)
  - Buttons (`.btn-primary`, `.btn-secondary`, `.btn-danger`)
  - Responsive tables with mobile card fallback (`.responsive-list`)
  - Loading/skeleton states for dashboard.

### Current color tokens (in use)

Currently defined in `:root` in `app.css`:

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

### Current chart colors (hardcoded in dashboard)

- Balance line: blue tones (`#0b7fab`, `rgba(11,127,171,0.14)`).
- Revenue vs expenses: green (`#1b7e4b`) and red (`#9e2f2b`).
- Donut/stacked by category: mixed palette (`#0b7fab`, `#1f7346`, `#a75f00`, `#7a4c8e`, `#ba3f32`, `#64748b`, ...).

Note: there is currently a mix of CSS tokens and literal colors in CSS/JS.

## New Semantic Palette (Defined Now)

## 1) Brand base colors

- `brand-primary`: `#f0ec57` (main yellow)
- `brand-secondary`: `#19647e` (secondary blue-teal)
- `brand-deep`: `#07004d` (deep blue for contrast/identity)
- `brand-mint`: `#42e2b8` (positive highlight)
- `brand-coral`: `#eb8a90` (attention highlight)

## 2) Support scale (same color family)

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

## 3) Semantic color roles

### 3.1 Surfaces and borders

- `color-bg-app`: `#f7f9fc` (global background)
- `color-bg-subtle`: `#eef3f7` (secondary areas)
- `color-bg-card`: `#ffffff` (cards)
- `color-bg-elevated`: `#ffffff` (modals/dropdowns)
- `color-border-default`: `#d7e0e8`
- `color-border-strong`: `#b8c7d4`
- `color-overlay`: `rgba(7, 0, 77, 0.45)`

### 3.2 Typography

- `color-text-primary`: `#101f33`
- `color-text-secondary`: `#35506a`
- `color-text-muted`: `#5e7388`
- `color-text-on-primary`: `#07004d` (on `#f0ec57`)
- `color-text-on-secondary`: `#ffffff` (on `#19647e`)
- `color-text-on-dark`: `#f7f9fc`
- `color-link`: `#145267`
- `color-link-hover`: `#07004d`

### 3.3 Actions and interactive components

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

### 3.4 Feedback and domain states

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

## 4) Mapping by interface element

- Topbar: translucent light background (`color-bg-card` + blur), links in `color-link`.
- Active navigation: text in `color-text-on-secondary` with `color-action-secondary-bg` background.
- Primary button: `color-action-primary-bg` background and `color-action-primary-text` text.
- Secondary button: `color-action-secondary-bg` background and white text.
- Neutral/support button: `color-action-tertiary-*`.
- Destructive actions: `color-danger` and variants.
- Inputs: white background, `color-border-default` border, focus with `color-focus-ring`.
- Summary cards:
  - Balance: `color-info-bg` base + `color-info` accent
  - Revenue: `color-success-bg` base + `color-success` accent
  - Expenses: `color-danger-bg` base + `color-danger` accent
- Tables: header with `color-text-muted`, rows with `color-border-default`.
- Skeleton/loading: `color-bg-subtle` tones.

## 5) Chart palette

### 5.1 Main series (suggested order)

1. `#19647e` (primary comparison)
2. `#42e2b8` (positive)
3. `#eb8a90` (warning/negative)
4. `#07004d` (contrast support)
5. `#f0ec57` (highlight)
6. `#8cb7c7`
7. `#22bf96`
8. `#cf6770`

### 5.2 Conventions by chart type

- Cumulative balance (line/area): line `#19647e`, area `rgba(25,100,126,0.16)`.
- Revenue vs expenses:
  - Revenue: `#22bf96`
  - Expenses: `#cf6770`
- Donut by category: use the sequence from section 5.1.
- No data: `#d7e0e8`.
- Grid/axis labels: `#5e7388`.

## 6) Proposed CSS tokens (reference)

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

## 7) Adoption guidelines

- Centralize all colors in CSS tokens (remove hardcoded hex values in templates/JS).
- Sync charts with shared palette constants.
- Ensure minimum WCAG AA contrast for text and buttons.
- Preserve current responsive patterns (table-to-card behavior on mobile).
- Future evolution: move tokens to a single theme and prepare seasonal variants without changing semantics.

## 8) Recorded decisions

- Official primary color: `#f0ec57`.
- Official secondary color: `#19647e`.
- `#07004d` is the strong contrast base for typography on light surfaces and identity elements.
- `#42e2b8` and `#eb8a90` are functional accents (positive/attention) and chart colors.
