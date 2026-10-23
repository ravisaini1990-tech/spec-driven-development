// Mirrors backend/src/main/java/com/support/tickets/service/StateMachineService.java
// and spec/state-machine.md. The backend is the source of truth and rejects invalid
// transitions with 409 regardless of what this map says — this only drives which
// options the UI offers, so users aren't shown a transition the API will reject.
export const VALID_TRANSITIONS = {
  OPEN: ['IN_PROGRESS', 'CANCELLED'],
  IN_PROGRESS: ['RESOLVED', 'CANCELLED'],
  RESOLVED: ['CLOSED'],
  CLOSED: [],
  CANCELLED: [],
};

export function nextValidStatuses(currentStatus) {
  return VALID_TRANSITIONS[currentStatus] || [];
}
