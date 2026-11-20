# Product

<!-- impeccable:product-schema 1 -->

## Platform

android

## Users

Primary users are individuals who want to record personal income and expenses on an Android phone without sending financial data to a third-party service. This is inferred from the supplied course project and the request to turn the existing local Contact Manager app into a financial manager.

## Product Purpose

Finance Manager helps a user record transactions, understand monthly cash flow, set category budgets, notice overspending, and export their data. Success means the user can add the first operation in under a minute and can understand the current month from the opening screen.

## Positioning

The product keeps the complete personal-finance dataset locally on the device while still providing budgets and visual monthly analytics. It is an educational, extensible implementation rather than a payment system or bank connector.

## Operating Context

The app is used on a personal Android device, often one-handed and in short sessions immediately after a purchase or income event. The default currency is Russian rubles. Dates and interface copy are Russian. Data remains available offline.

## Capabilities and Constraints

- Record, edit, delete, and filter income and expense transactions.
- Maintain separate income and expense categories with stable colors.
- Set one monthly spending limit per expense category.
- Show monthly income, expenses, balance, category distribution, and daily balance trend.
- Warn when a category budget is close to or above its limit.
- Export transactions as a UTF-8 CSV file.
- Store monetary values as integer kopecks and dates in ISO format in SQLite.
- Do not add bank synchronization, payments, cloud accounts, or fabricated financial advice in version 1.01.
- Preserve the existing Android application ID so version 1.01 can install as an update over the copied project.

## Brand Commitments

The application name is Finance Manager. Version 1.01 starts a new financial direction while retaining the repository history of Contact Manager. The interface is Russian-first and uses familiar Material 3 Android behavior.

## Evidence on Hand

- Course project source: `D:\Загрузки\АЮГрам\Косенок .docx`.
- Existing Android project and its local-first implementation patterns in this repository.
- The course project specifies users, categories, transactions, budgets, dashboard analytics, filters, budget warnings, and CSV export.
- No bank API, commercial claims, or production security audit is supplied; future work must not imply those exist.

## Product Principles

1. Recording an operation is the fastest path in the app.
2. Monthly status is understandable before the user opens a report.
3. Money calculations are exact and reproducible.
4. The interface explains empty and exceeded states instead of hiding them.
5. Personal financial data stays on the device unless the user explicitly exports it.

## Accessibility & Inclusion

Use Material touch targets of at least 48 dp, semantic labels, scalable text in sp, light and dark themes, non-color-only budget warnings, and number formatting that remains legible at increased font scale.
