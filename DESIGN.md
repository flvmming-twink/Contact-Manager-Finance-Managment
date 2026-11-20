# Finance Manager Design System

## Direction

The interface translates a household monthly ledger into Material 3. It feels measured and calm rather than bank-like or speculative: tabular numbers carry the hierarchy, the current month acts as the page divider, and charts use ruled measurement only where data needs a scale.

The world is code-led from the approved portrait reference in `.impeccable/mocks/decision/finance-ledger-dashboard.png`. Native Android behavior is authoritative when the reference and platform conventions differ.

## Direction Contract

### World

Warm paper-toned surfaces, deep ink text, emerald primary actions, coral expenses, and amber risk. The visual metaphor is an actively used monthly ledger, not a decorative notebook.

### First Viewport

The overview opens on the selected month, current balance, income and expense totals, the most urgent budget warning, category spending distribution, balance trend, and recent operations. The add action remains reachable with one thumb.

### Visitor Path

Overview explains the month. Operations records and filters entries. Budgets turns category limits into progress. Categories maintains the vocabulary used by both operations and analytics.

### Signature Interaction

Changing the month updates every metric and chart together with a short Material fade-through. Adding an operation returns to the relevant list, updates the overview, and surfaces a snackbar when a budget crosses a threshold.

### Cross Surface Reach

The same tabular figures, semantic colors, month selector, progress vocabulary, and 12 to 16 dp corner language apply to every screen and dialog.

### Honest Risk

Ledger references can become visually dense. The implementation protects scanability with Material spacing, compact but explicit labels, one primary action per screen, and no decorative grid outside chart regions.

## Raised Discipline

- Airport wayfinding: each screen exposes the next decision clearly and keeps navigation labels literal.
- Lexicon measure: explanatory copy stays short and columns keep a stable readable width.
- Star atlas: charts rank data through magnitude and position before color.
- Emission rail: budget states use text and progress shape in addition to hue.
- Catalog grid: dense lists align to whole rows and never become nested cards.

## Color Roles

- Primary: `#0B7A4B`
- On primary: `#FFFFFF`
- Primary container: `#D7F3E3`
- On primary container: `#092D20`
- Secondary ink: `#34445F`
- Background: `#FFF9F3`
- Surface: `#FFFCF8`
- Surface variant: `#F2ECE4`
- Outline: `#7B817E`
- Expense: `#B53A35`
- Expense container: `#FFE1DE`
- Warning: `#8A5A00`
- Warning container: `#FFE8B2`

Dark theme keeps the same roles with charcoal-brown surfaces, mint primary, soft coral expense, and amber warning. Dynamic Color may replace the static scheme on Android 12 and newer while semantic income, expense, and warning roles remain distinguishable.

## Typography

Use the Material type scale with Roboto. Monetary values use tabular numerals. Screen title is `titleLarge`, section labels are `titleMedium`, primary money is `headlineLarge`, list values are `titleMedium`, and supporting text is `bodyMedium` or `labelLarge`. No display font is introduced.

## Shape and Elevation

Large summary surfaces use 16 dp corners, ordinary containers 12 dp, fields 12 dp, and small chips 8 dp. Tonal elevation separates layers; shadows are reserved for the floating action button and temporary overlays.

## Components and States

- Bottom navigation has four destinations: Overview, Operations, Budgets, Categories.
- A single FAB creates an operation from Overview and Operations.
- Month navigation is a previous button, centered month label, and next button; future months remain available for budgets but transaction dates cannot be future dates.
- Empty states name the missing record and the action that resolves it.
- Budget progress shows amount, percentage, and a textual state: normal, close to limit, or exceeded.
- Destructive actions require a Material confirmation dialog.
- Errors explain the invalid field and preserve entered values.

## Motion and Accessibility

Transitions stay between 150 and 250 ms and communicate state. Touch targets are at least 48 dp. Color never carries meaning alone. Text uses sp, supports font scaling, and maintains readable contrast in light and dark themes. System Back closes dialogs or returns through navigation without interception.
