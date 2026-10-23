# State Machine Specification

## States

| State | Description |
|-------|-------------|
| OPEN | Ticket created, not yet picked up |
| IN_PROGRESS | Actively being worked on |
| RESOLVED | Work complete, pending confirmation |
| CLOSED | Confirmed resolved, archived |
| CANCELLED | Ticket withdrawn, will not be worked |

## Valid Transitions

| From \ To | OPEN | IN_PROGRESS | RESOLVED | CLOSED | CANCELLED |
|-----------|------|-------------|----------|--------|-----------|
| OPEN | — | ✓ | ✗ | ✗ | ✓ |
| IN_PROGRESS | ✗ | — | ✓ | ✗ | ✓ |
| RESOLVED | ✗ | ✗ | — | ✓ | ✗ |
| CLOSED | ✗ | ✗ | ✗ | — | ✗ |
| CANCELLED | ✗ | ✗ | ✗ | ✗ | — |

✓ = valid, ✗ = invalid (throws InvalidStateTransitionException → HTTP 409)

## Transition Diagram

```
                ┌──────────────────┐
                │                  │
          ┌─────▼─────┐     ┌──────▼──────┐
  CREATE  │           │     │             │
 ────────►│   OPEN    ├────►│ IN_PROGRESS │
          │           │     │             │
          └─────┬─────┘     └──────┬──────┘
                │                  │
                │  CANCELLED       │ RESOLVED
                ▼                  ▼
          ┌───────────┐     ┌─────────────┐     ┌────────┐
          │           │     │             │     │        │
          │ CANCELLED │     │  RESOLVED   ├────►│ CLOSED │
          │           │     │             │     │        │
          └───────────┘     └─────────────┘     └────────┘
                                   │
                             (also CANCELLED
                              from IN_PROGRESS)
```

## Business Rules

1. CLOSED is a terminal state — no further transitions allowed.
2. CANCELLED is a terminal state — no further transitions allowed.
3. Re-opening cancelled/closed tickets is not supported in v1.
4. RESOLVED tickets can only move to CLOSED (i.e. cannot be cancelled at this stage).
