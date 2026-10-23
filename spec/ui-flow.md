# UI Flow

## Screen States

The app manages view state with a single `useState` in `App.jsx`:

```
view: 'list' | 'detail' | 'create'
selectedTicketId: number | null
```

## Flow 1: Browse Tickets

1. App loads → `TicketList` renders, fetches `GET /api/tickets`.
2. User sees table of tickets: title, status badge, priority, assignee.
3. User types in search box → list filters live (re-fetch with `?search=`).
4. User selects status from dropdown → list filters (re-fetch with `?status=`).
5. User clicks a ticket row → `view='detail'`, `selectedTicketId=id`.

## Flow 2: Create Ticket

1. User clicks "New Ticket" button → `view='create'`.
2. `CreateTicketForm` renders with fields: title (required), description (required), priority select, assignee, category.
3. User submits → `POST /api/tickets`.
4. On 201 success → `view='list'`, list re-fetches.
5. On 400 → inline field errors displayed next to each invalid field.

## Flow 3: View Ticket Detail

1. `TicketDetail` loads ticket via `GET /api/tickets/{id}`.
2. Displays: title, description, status badge, priority, assignee, category, timestamps.
3. Status transition area: dropdown showing only valid next states for current status.
4. User selects new status + clicks "Update Status" → `PATCH /api/tickets/{id}/status`.
5. On 409 → error message shown.

## Flow 4: Edit Ticket

1. User clicks "Edit" toggle in TicketDetail.
2. Fields become editable inputs.
3. User edits title, description, priority, assignee, category.
4. User clicks "Save" → `PATCH /api/tickets/{id}`.
5. On success → edit mode exits, fields update.

## Flow 5: Add Comment

1. Below ticket detail, `CommentSection` lists existing comments.
2. User fills in "Author" and "Comment" fields.
3. User clicks "Add Comment" → `POST /api/tickets/{id}/comments`.
4. On success → comment appears in list immediately.

## Flow 6: AI Assistant

1. Floating button (bottom-right corner) — always visible.
2. Click → panel slides up, showing input + ask button.
3. User types question, clicks "Ask".
4. Loading indicator shown.
5. On response:
   - If `noRelevantTickets=true` → show: "No relevant tickets found for your query."
   - Else → show answer text + "Referenced tickets: #1, #3" links.
6. Clicking referenced ticket ID navigates to that ticket's detail view.
7. Click toggle button again → panel collapses.
