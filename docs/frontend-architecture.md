# Front-End Architecture

This document describes the current front-end architecture used by AI$HA.

## Objectives

- Keep the UI server-rendered, predictable, and easy to audit.
- Deliver interactive behavior with minimal custom JavaScript.
- Preserve accessibility, responsiveness, and pt-BR UX conventions.

## Architectural Style

AI$HA uses server-side rendering with Spring MVC + Thymeleaf as the primary front-end architecture.

- Page rendering: full HTML views returned by MVC controllers.
- Partial updates: HTML fragments returned and swapped with HTMX.
- Browser-facing communication: HTML-first approach; JSON is mainly used by non-UI or dashboard API use cases.

## Main Building Blocks

- Templates: `src/main/resources/templates`
- Shared layout fragment: `src/main/resources/templates/fragments/header.html`
- Global styles: `src/main/resources/static/css/app.css`
- Shared icon bootstrapping: `src/main/resources/static/js/lucide-init.js`
- UI i18n bundles:
  - `src/main/resources/messages.properties`
  - `src/main/resources/messages_pt_BR.properties`

## Request and Rendering Flow

1. The browser requests a page route (for example, `/entries`).
2. The MVC controller prepares view-model data.
3. Thymeleaf renders a full HTML page.
4. For interactive actions, HTMX submits requests and replaces specific page fragments (typically `#table-container` or result sections).
5. Lucide icons are re-rendered after HTMX swaps through the `htmx:afterSwap` hook.

## Layout and Composition

The shared top area (brand, navigation, user actions, global date filter) is centralized in `fragments/header.html` and included with `th:replace` in pages.

This keeps cross-cutting UI concerns in one place:

- Main navigation
- Theme toggle
- Logout action
- Global date filter controls

## Interactivity Model (HTMX-First)

HTMX is the default mechanism for in-page interactions, especially in list and import screens.

Typical patterns currently used:

- `hx-post` for delete and bulk actions.
- `hx-get` for filter-driven partial refreshes.
- `hx-target` + `hx-swap="outerHTML"` to replace card/table fragments.
- Polling for long-running import status updates (`hx-trigger="every 1s"`).

Design guideline: when a screen can be implemented as fragment updates, prefer HTMX over custom JavaScript.

## JavaScript Responsibilities

Custom JavaScript is intentionally limited to cases where HTMX alone is not enough.

Current responsibilities include:

- Re-rendering Lucide icons after DOM updates.
- Theme selection and persistence (`localStorage` + `data-theme`).
- Dashboard charts and dynamic metric loading (Chart.js + API endpoints).

## Styling and Design System

Styling is centralized in `app.css` using shared tokens and reusable component classes.

Key patterns:

- Card-driven page composition (`.card`, chart cards, summary cards).
- Consistent button/input/table styles.
- Table-to-card responsive behavior via `.responsive-list` on narrower viewports.

For color tokens and visual language details, see [docs/design-system.md](design-system.md).

## Responsiveness Strategy

Mobile behavior prioritizes readability instead of horizontal scrolling.

In list views, tables switch to stacked cards under narrow breakpoints:

- table headers are visually hidden for accessibility-preserving structure,
- each cell uses `data-label` as an inline field label,
- actions and amount cells are adjusted for touch-friendly flow.

## Localization and Formatting

- UI text is served in pt-BR.
- Locale resolution defaults to `pt-BR` (`WebLocaleConfig`).
- Templates use message keys (`#{...}`) for labels, buttons, and validation texts.
- Monetary and date formatting on interactive dashboard scripts uses `pt-BR` formatters.

## Security in the UI Layer

Security is enforced in rendered forms and HTMX flows:

- CSRF token is included in all state-changing forms.
- Session-based authentication gates protected pages.
- Logout is a POST action with CSRF protection.

## Front-End Conventions for New Screens

- Use Thymeleaf templates and fragments as the default composition model.
- Prefer HTMX fragment interactions for list/form workflows.
- Keep text and labels in pt-BR message bundles.
- Include Lucide icons in navigation, table headers, and text buttons.
- Keep layouts responsive with table-to-card fallback for dense lists.
- Avoid introducing new JavaScript unless interaction complexity requires it.
