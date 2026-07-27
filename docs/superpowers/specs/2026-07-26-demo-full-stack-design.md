# Gallae Mallae Demo Full-Stack Design

## Goal

Make the backend support and verify the real demo journey used by the frontend:
OAuth login, onboarding, group invitation, multi-user menu voting, final-menu
resolution, restaurant search and selection, and previous-history retrieval.

## Source of truth and scope

Existing backend domain rules remain authoritative. Changes are limited to
failures and missing capabilities that block the demo.

Included:

- restore a compiling and passing test baseline
- complete authorization checks on demo-reachable session actions
- provide browser-safe CORS and credential configuration for the configured
  frontend origin
- complete REST contracts needed by frontend state recovery and final voting
- expose persisted restaurant results and an owner-only final selection action
- transition a selected restaurant session to `COMPLETED` atomically
- publish or expose sufficient state for WebSocket reconnect recovery
- retain and verify previous-history list/detail behavior

Excluded unless a failing demo dependency proves otherwise:

- notification delivery
- broad MQ/DLQ redesign
- deployment automation
- unrelated schema or architectural refactoring

## Backend state machine and APIs

The existing menu state machine is retained:

- recommendation moves the session into menu voting
- first-round close applies current candidate-count policy
- one candidate supports owner confirm or re-recommendation
- two candidates support member final voting and owner tie resolution
- three or more allowed candidates support owner selection
- final menu selection moves to `RESTAURANT_SEARCHING`

The restaurant slice is completed:

1. owner requests nearby search in `RESTAURANT_SEARCHING`
2. asynchronous processing persists ordered results and moves to
   `RESTAURANT_SELECTION`
3. authorized group members can retrieve persisted results
4. the owner selects one result
5. store selection and session transition to `COMPLETED` occur in one
   transaction
6. completed data is returned by previous-history endpoints

Exact endpoint shapes will follow existing controller conventions and use the
standard response envelope. Group ID and vote-session ID must both be validated
to prevent cross-group session access.

## Components and boundaries

- API controllers authenticate, validate DTOs, and map responses.
- Core services enforce group membership, owner permissions, state transitions,
  and transaction boundaries.
- Repository ports expose only the persisted-result listing and final-selection
  operations required by core services.
- DB adapters implement ordering and atomic selection without leaking JPA
  entities.
- WebSocket events are hints; `/vote-state` and restaurant result retrieval are
  the recovery sources of truth.
- CORS accepts only configured frontend origins and supports credentials and
  authorization headers.

## Error handling

- unauthenticated requests return the current `401` envelope
- non-members and non-owners return existing domain authorization errors
- path group/session mismatches are rejected without exposing another group
- invalid state transitions return the existing conflict-style error
- empty restaurant searches preserve `RESTAURANT_SEARCHING` so the owner can
  retry
- missing or foreign restaurant IDs do not modify session state
- repeated final-selection requests are either idempotent for the same result
  or rejected consistently by the state policy

## Testing

Behavior changes follow red-green-refactor:

- first fix the stale test import so the existing suite can compile
- controller and core tests for membership, ownership, path mismatch, state,
  empty results, listing, final selection, rollback, and completion
- repository tests for result ordering and one selected restaurant
- CORS preflight integration test for the configured frontend origin
- all module tests and Gradle build
- local runtime verification with DB and Redis
- frontend/backend browser verification using two independent authenticated
  sessions

Real Naver, OpenAI, and Kakao checks are performed when credentials are
available; otherwise the remaining steps and required values are handed off
precisely.

