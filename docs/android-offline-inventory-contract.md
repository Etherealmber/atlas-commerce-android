# Android offline inventory write contract (proposed Worker changes)

Android needs a location-specific, replay-safe mutation API before it can safely drain a durable offline queue. The current Worker has location listing/placements plus cycle-count and bill-receive endpoints, but no shared idempotency contract across offline writes. Android's scanner now exposes location-specific receive, issue, and cycle-count actions through a durable queue and targets the contract below.

## Reads

- `GET /api/inventory/locations` — active locations, including `id`, `code`, `name`, `is_default`.
- `GET /api/inventory/products/{product_id}/placements` — per-location quantities. Add a monotonically increasing `revision` per product/location placement (or an equivalent opaque version) so a count taken offline can detect intervening changes.

## Write

`POST /api/inventory/offline-operations`

Headers:

```http
Authorization: Bearer …
Idempotency-Key: <operation_id UUID>
Content-Type: application/json
```

Receive request:

```json
{
  "operation_id": "<same UUID as header>",
  "kind": "receive",
  "product_id": 42,
  "location_id": 3,
  "quantity_delta": 5,
  "note": "Supplier delivery",
  "client_occurred_at": "2026-10-21T09:30:00Z"
}
```

Issue uses the same shape with `kind: "issue"` and a positive `quantity_delta`; the Worker subtracts it and rejects a result below zero unless server policy explicitly allows negative stock.

Count request:

```json
{
  "operation_id": "<same UUID as header>",
  "kind": "count",
  "product_id": 42,
  "location_id": 3,
  "counted_quantity": 17,
  "observed_quantity": 15,
  "observed_revision": 8,
  "note": "Cycle count",
  "client_occurred_at": "2026-10-21T09:30:00Z"
}
```

Successful response envelope:

```json
{
  "success": true,
  "data": {
    "operation_id": "<UUID>",
    "kind": "receive",
    "product_id": 42,
    "location_id": 3,
    "previous_quantity": 12,
    "new_quantity": 17,
    "product_total": 31,
    "movement_id": 987,
    "applied_at": "2026-10-21T09:31:00Z",
    "replayed": false
  }
}
```

## Required semantics

1. Scope idempotency to the authenticated account/tenant. Persist the key, canonical payload hash, and original response atomically with the mutation. Repeating the same key and payload returns the original response without applying a second movement; using the same key with a different payload returns `409 IDEMPOTENCY_KEY_REUSED`.
2. Authenticate/authorize on every request, validate that the location is active and belongs to the account, and apply existing stock-role policy. Never trust client user/account identifiers.
3. Apply each operation and its movement/audit record atomically. The queue sends one operation per request and preserves insertion order.
4. For count, compare `observed_revision` (and optionally `observed_quantity`) to the current location placement. If stale, return `409 COUNT_CONFLICT` with the current quantity/revision; do not apply or silently overwrite. Android keeps that item visible for explicit user resolution.
5. For receive/issue, `quantity_delta` is a positive relative movement. For count, `counted_quantity` is an absolute on-hand quantity. A retry after timeout must never apply a second delta.
6. Return a stable response for both first application and replay, including the canonical resulting quantity and movement ID. Use `replayed: true` only as informational metadata; Android must treat both responses as success.
7. Return 4xx validation/permission/conflict responses as terminal queue outcomes (visible to the user); 5xx/network failures are safe to retry with the same idempotency key.

The queue stores operations against a specific server/account and `location_id`; it must not switch locations or accounts during replay. Android must not claim an operation is complete until it receives the idempotent success response.
