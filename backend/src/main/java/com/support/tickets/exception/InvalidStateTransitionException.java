package com.support.tickets.exception;

import com.support.tickets.domain.TicketStatus;

public class InvalidStateTransitionException extends RuntimeException {

    public InvalidStateTransitionException(TicketStatus from, TicketStatus to) {
        super("Invalid state transition from " + from + " to " + to);
    }
}
