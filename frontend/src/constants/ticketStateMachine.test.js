import { describe, it, expect } from 'vitest';
import { VALID_TRANSITIONS, nextValidStatuses } from './ticketStateMachine.js';

describe('ticketStateMachine', () => {
  it('matches the backend state machine transition table (spec/state-machine.md)', () => {
    expect(VALID_TRANSITIONS).toEqual({
      OPEN: ['IN_PROGRESS', 'CANCELLED'],
      IN_PROGRESS: ['RESOLVED', 'CANCELLED'],
      RESOLVED: ['CLOSED'],
      CLOSED: [],
      CANCELLED: [],
    });
  });

  it('returns valid next statuses for a non-terminal status', () => {
    expect(nextValidStatuses('OPEN')).toEqual(['IN_PROGRESS', 'CANCELLED']);
  });

  it('returns an empty array for terminal statuses', () => {
    expect(nextValidStatuses('CLOSED')).toEqual([]);
    expect(nextValidStatuses('CANCELLED')).toEqual([]);
  });

  it('returns an empty array for an unknown status rather than throwing', () => {
    expect(nextValidStatuses('NOT_A_REAL_STATUS')).toEqual([]);
  });
});
