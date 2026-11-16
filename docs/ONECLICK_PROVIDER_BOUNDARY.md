# OneClick Provider Boundary — KIOQ.network

> Status: integration contract / implementation guardrails. This document records how KIOQ must consume OneClick without importing OneClick's product, account, or financial model into KIOQ. It does not claim that a live integration exists.

## 1. Ownership boundary

- **OneClick is an external provider**. It supplies provider-side plans/products, wholesale cost information, submission endpoints, provider transaction IDs/references, provider statuses, and provider account balance.
- **KIOQ.network owns the platform**: owner and trader accounts, authentication/roles, customer records, KIOQ balances, pricing/markup, debts, collections, profit, cashbox, notifications, and the KIOQ transaction ledger.
- A OneClick account balance is **not** a KIOQ trader balance, customer balance, wallet, debt, collection, or profit.
- A OneClick plan code/name/operator/status is provider catalog data, not a KIOQ user, service permission, product identity, or retail price.
- Never copy OneClick dashboard sections (Navio, POS, gift cards, bills, eSIM, etc.) into KIOQ merely because they exist in OneClick. KIOQ exposes only features already approved in its own scope.

## 2. Current KIOQ service scope

The current KIOQ Android prototype lists:
- Djezzy
- Ooredoo
- Mobilis
- IDoom Fibre
- IDoom 4G

This is KIOQ's product vocabulary. Map it to provider operations through an explicit server-side adapter; do not rename KIOQ screens to match provider labels.

The OneClick v3 introduction currently documents:
- Mobile top-ups for Mobilis, Djezzy, and Ooredoo (and some references/examples mention Pixx).
- Internet top-ups for ADSL and 4G LTE.
- Other OneClick products such as gift cards, Navio, and bill payment.

**Important unresolved mapping:** OneClick documents internet types as `ADSL` and `4G`, with ADSL number validation and 4G LTE validation. The docs do not establish that its `ADSL` product is identical to KIOQ's named `IDoom Fibre`. Do not map Fibre to ADSL or enable it until the exact product/number format and account entitlement are confirmed with OneClick. Map KIOQ's `IDoom 4G` to provider `4G` only after confirming the provider's returned products and account access.

Do not expose Pixx or any other provider-only operator in KIOQ unless the project owner explicitly approves it.

## 3. Provider API contract (server only)

- API base: `https://api.oneclickdz.com/v3`
- Authentication header: `X-Access-Token`
- Use separate sandbox and production credentials.
- Store credentials only in server-side secret storage. Never place them in Android code, APK resources, client preferences, logs, or Git.
- The Android app calls KIOQ's own API only. It must never call OneClick directly.

### Mobile

- Catalog: `GET /mobile/plans`
- Submit: `POST /mobile/send`
- Lookup by KIOQ-supplied provider reference: `GET /mobile/check-ref/{ref}`
- The provider request uses `plan_code`, `MSSIDN`, `amount` (required for dynamic plans; ignored for fixed plans), and `ref`.
- `MSSIDN` must be a string in the provider's documented local format with leading zero; do not parse it as a number or silently convert it to international format.
- Plan catalog distinguishes dynamic plans (min/max amount) from fixed plans (predefined amount), and includes `isEnabled`. KIOQ must use the current enabled state and validate dynamic amount bounds server-side.
- `GETMENU_*` plans retrieve offers for a specific subscriber number; they are not ordinary fixed-value top-ups. Handle them as a distinct provider operation and only show offers actually returned for that number.

### Internet

- Products: `GET /internet/products`
- Number validation: `GET /internet/check-number?type=ADSL|4G&number=...`
- Submit: `POST /internet/send`
- Lookup by reference: `GET /internet/check-ref/{ref}`
- Submit fields documented: `type`, `number`, `value`, `ref`.
- Validate the subscriber number before submitting. Preserve the provider's exact required number format.
- Provider docs describe ADSL and 4G LTE; KIOQ's Fibre/4G naming must be resolved by an explicit mapping table, not guessed.

## 4. Catalog and pricing separation

- Sync provider catalog on the server and retain the provider plan code, provider operator, plan kind, bounds/fixed amount, enabled flag, cost multiplier (when returned), and sync timestamp.
- The provider's `cost` multiplier is wholesale-cost information. For example, `0.96` means 96% of face value. It is **not** KIOQ's retail price or trader discount.
- Calculate KIOQ cost and retail selling price using KIOQ's own approved pricing rules and ledger. Never return provider `cost`, raw provider margin, API credentials, or provider account balance to traders.
- Provider catalog data may be cached, but do not offer disabled plans. Revalidate critical conditions at order time.
- Do not assume every plan response contains `cost`; treat absent optional provider fields as absent, not as zero.
- Keep KIOQ-facing service names and prices independent from provider display names and prices.

## 5. Transaction lifecycle and idempotency

1. KIOQ validates the authenticated trader, service, subscriber number, plan, amount, price, and available KIOQ trader balance.
2. KIOQ creates its own immutable transaction/order ID and ledger entries under a database transaction. The KIOQ ID is the stable business reference.
3. KIOQ sends the same unique reference as provider `ref` (or a deterministic provider-safe derivative stored alongside the KIOQ ID).
4. Persist provider `topupId` / returned provider reference separately from the KIOQ order ID.
5. A successful HTTP/API submission response means the request was accepted/created; it does **not** mean the recharge is fulfilled.
6. Query provider status by reference and update KIOQ's provider-status field. Translate provider states into KIOQ's own internal state model; preserve the raw provider state for audit.
7. Never create a second provider submission for the same KIOQ order just because the client retries, the network times out, or the result is unknown. Query by the original reference first.

Provider mobile states documented include `PENDING`, `HANDLING`, `FULFILLED`, `REFUNDED`, and `UNKNOWN_ERROR`. `UNKNOWN_ERROR` is not a confirmed failure; do not automatically refund or resubmit while the outcome is unresolved. The docs describe it as resolving within 24 hours.

Internet top-ups have their own product/status lifecycle and may include queued processing; do not reuse mobile-only status assumptions for internet orders. Store raw provider states and map only after validating the complete endpoint documentation for the relevant product.

## 6. Financial and permission rules

- KIOQ's ledger is the source of truth for trader debit, owner wholesale cost, markup/profit, debt, collections, and cashbox.
- Provider deductions/refunds are reconciled against KIOQ's recorded provider cost; they do not directly rewrite KIOQ trader balances.
- A provider refund is a provider-side event. KIOQ must post its own compensating ledger entries exactly once, linked to the original order and provider evidence.
- Traders see only KIOQ retail amount, their own balance effect, customer/number, KIOQ status, and allowed receipt details.
- Owner-only data includes provider cost, OneClick identifiers/diagnostics, provider account balance, margin/profit, reconciliation details, and credentials.
- Enforce every permission on the server; hiding fields in Android is not sufficient.

## 7. Error handling

- Parse the provider envelope (`success`, `data`, `meta`, `requestId`, and `error`) without assuming HTTP 200 means business success.
- A provider validation response can also mean a plan/operator is disabled or not enabled for this account. Do not classify all validation errors as malformed customer input.
- Insufficient provider balance is an owner/provider-funding issue, not a trader's KIOQ balance error.
- Duplicate provider reference: query the existing provider order; never generate a new reference for the same KIOQ order.
- Timeout, connection reset, 5xx, or unknown result: mark KIOQ order as awaiting reconciliation and query provider status. Never blindly retry or refund.
- Keep provider `requestId`, KIOQ order ID, provider reference, and provider transaction ID in protected operational logs. Redact tokens and personal data.

## 8. What is deliberately out of scope

Unless separately approved as KIOQ features, do not implement or expose OneClick's:
- Navio payment gateway
- OneClick wallet / provider balance as a customer wallet
- OneClick POS
- Gift cards and vouchers
- eSIM
- Bill payment
- OneClick reports/dashboard and its account/profile controls

These are OneClick platform capabilities, not automatic KIOQ requirements.

## 9. Current implementation status and next steps

The current repository README describes an Android Kotlin/Jetpack Compose prototype. It explicitly says there is no central backend, authentication, real OneClick connection, or live recharge yet; displayed recharge history is demo data and contacts are local SQLite.

Therefore this contract is a boundary/specification only. Do not add fake live data or place provider calls in the Android app. The correct implementation sequence is:
1. Build the KIOQ server and its own authentication/roles/ledger.
2. Implement a server-side OneClick adapter for catalog, validation, submission, status lookup, and reconciliation.
3. Verify provider product entitlement and the IDoom Fibre/ADSL mapping with OneClick.
4. Test in sandbox with duplicate, timeout, insufficient-balance, disabled-plan, pending, refund, and unknown-outcome cases.
5. Enable production only after owner approval and end-to-end reconciliation tests.

## 10. Official source pages

- Introduction: https://docs.oneclickdz.com/en/introduction
- Mobile plans: https://docs.oneclickdz.com/en/api-reference/mobile/list-plans
- Mobile submit: https://docs.oneclickdz.com/en/api-reference/mobile/send-topup
- Mobile status by reference: https://docs.oneclickdz.com/en/api-reference/mobile/check-by-ref
- Internet products: https://docs.oneclickdz.com/en/api-reference/internet/list-products
- Internet number validation: https://docs.oneclickdz.com/en/api-reference/internet/validate-number
- Internet submit: https://docs.oneclickdz.com/en/api-reference/internet/send-topup
- Internet status by reference: https://docs.oneclickdz.com/en/api-reference/internet/check-by-ref
