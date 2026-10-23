package com.support.tickets.service;

import com.support.tickets.domain.TicketStatus;
import com.support.tickets.exception.InvalidStateTransitionException;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;

/**
 * Validates ticket state transitions according to the defined state machine:
 *
 * OPEN        → IN_PROGRESS, CANCELLED
 * IN_PROGRESS → RESOLVED, CANCELLED
 * RESOLVED    → CLOSED
 * CLOSED      → (terminal, no transitions)
 * CANCELLED   → (terminal, no transitions)
 */
@Service
public class StateMachineService {

    private static final Map<TicketStatus, Set<TicketStatus>> VALID_TRANSITIONS = Map.of(
            TicketStatus.OPEN,        Set.of(TicketStatus.IN_PROGRESS, TicketStatus.CANCELLED),
            TicketStatus.IN_PROGRESS, Set.of(TicketStatus.RESOLVED, TicketStatus.CANCELLED),
            TicketStatus.RESOLVED,    Set.of(TicketStatus.CLOSED),
            TicketStatus.CLOSED,      Set.of(),
            TicketStatus.CANCELLED,   Set.of()
    );

    /**
     * Validates whether a transition from {@code from} to {@code to} is allowed.
     *
     * @param from current status
     * @param to   requested new status
     * @throws InvalidStateTransitionException if the transition is not permitted
     */
    public void validateTransition(TicketStatus from, TicketStatus to) {
        Set<TicketStatus> allowed = VALID_TRANSITIONS.getOrDefault(from, Set.of());
        if (!allowed.contains(to)) {
            throw new InvalidStateTransitionException(from, to);
        }
    }
}
