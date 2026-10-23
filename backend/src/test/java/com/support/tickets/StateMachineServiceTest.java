package com.support.tickets;

import com.support.tickets.domain.TicketStatus;
import com.support.tickets.exception.InvalidStateTransitionException;
import com.support.tickets.service.StateMachineService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.support.tickets.domain.TicketStatus.*;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("StateMachineService — Transition Validation")
class StateMachineServiceTest {

    private StateMachineService stateMachineService;

    @BeforeEach
    void setUp() {
        stateMachineService = new StateMachineService();
    }

    // =========================================================
    // VALID TRANSITIONS
    // =========================================================

    @Test
    @DisplayName("OPEN → IN_PROGRESS is valid")
    void openToInProgress_valid() {
        assertDoesNotThrow(() -> stateMachineService.validateTransition(OPEN, IN_PROGRESS));
    }

    @Test
    @DisplayName("OPEN → CANCELLED is valid")
    void openToCancelled_valid() {
        assertDoesNotThrow(() -> stateMachineService.validateTransition(OPEN, CANCELLED));
    }

    @Test
    @DisplayName("IN_PROGRESS → RESOLVED is valid")
    void inProgressToResolved_valid() {
        assertDoesNotThrow(() -> stateMachineService.validateTransition(IN_PROGRESS, RESOLVED));
    }

    @Test
    @DisplayName("IN_PROGRESS → CANCELLED is valid")
    void inProgressToCancelled_valid() {
        assertDoesNotThrow(() -> stateMachineService.validateTransition(IN_PROGRESS, CANCELLED));
    }

    @Test
    @DisplayName("RESOLVED → CLOSED is valid")
    void resolvedToClosed_valid() {
        assertDoesNotThrow(() -> stateMachineService.validateTransition(RESOLVED, CLOSED));
    }

    // =========================================================
    // INVALID TRANSITIONS FROM OPEN
    // =========================================================

    @Test
    @DisplayName("OPEN → RESOLVED is invalid")
    void openToResolved_throwsException() {
        assertThrows(InvalidStateTransitionException.class,
                () -> stateMachineService.validateTransition(OPEN, RESOLVED));
    }

    @Test
    @DisplayName("OPEN → CLOSED is invalid")
    void openToClosed_throwsException() {
        assertThrows(InvalidStateTransitionException.class,
                () -> stateMachineService.validateTransition(OPEN, CLOSED));
    }

    // =========================================================
    // INVALID TRANSITIONS FROM IN_PROGRESS
    // =========================================================

    @Test
    @DisplayName("IN_PROGRESS → OPEN is invalid")
    void inProgressToOpen_throwsException() {
        assertThrows(InvalidStateTransitionException.class,
                () -> stateMachineService.validateTransition(IN_PROGRESS, OPEN));
    }

    @Test
    @DisplayName("IN_PROGRESS → CLOSED is invalid")
    void inProgressToClosed_throwsException() {
        assertThrows(InvalidStateTransitionException.class,
                () -> stateMachineService.validateTransition(IN_PROGRESS, CLOSED));
    }

    // =========================================================
    // INVALID TRANSITIONS FROM RESOLVED
    // =========================================================

    @Test
    @DisplayName("RESOLVED → OPEN is invalid")
    void resolvedToOpen_throwsException() {
        assertThrows(InvalidStateTransitionException.class,
                () -> stateMachineService.validateTransition(RESOLVED, OPEN));
    }

    @Test
    @DisplayName("RESOLVED → IN_PROGRESS is invalid")
    void resolvedToInProgress_throwsException() {
        assertThrows(InvalidStateTransitionException.class,
                () -> stateMachineService.validateTransition(RESOLVED, IN_PROGRESS));
    }

    @Test
    @DisplayName("RESOLVED → CANCELLED is invalid")
    void resolvedToCancelled_throwsException() {
        assertThrows(InvalidStateTransitionException.class,
                () -> stateMachineService.validateTransition(RESOLVED, CANCELLED));
    }

    // =========================================================
    // INVALID TRANSITIONS FROM CLOSED (terminal state)
    // =========================================================

    @Test
    @DisplayName("CLOSED → OPEN is invalid (terminal state)")
    void closedToOpen_throwsException() {
        assertThrows(InvalidStateTransitionException.class,
                () -> stateMachineService.validateTransition(CLOSED, OPEN));
    }

    @Test
    @DisplayName("CLOSED → IN_PROGRESS is invalid (terminal state)")
    void closedToInProgress_throwsException() {
        assertThrows(InvalidStateTransitionException.class,
                () -> stateMachineService.validateTransition(CLOSED, IN_PROGRESS));
    }

    @Test
    @DisplayName("CLOSED → RESOLVED is invalid (terminal state)")
    void closedToResolved_throwsException() {
        assertThrows(InvalidStateTransitionException.class,
                () -> stateMachineService.validateTransition(CLOSED, RESOLVED));
    }

    @Test
    @DisplayName("CLOSED → CANCELLED is invalid (terminal state)")
    void closedToCancelled_throwsException() {
        assertThrows(InvalidStateTransitionException.class,
                () -> stateMachineService.validateTransition(CLOSED, CANCELLED));
    }

    // =========================================================
    // INVALID TRANSITIONS FROM CANCELLED (terminal state)
    // =========================================================

    @Test
    @DisplayName("CANCELLED → OPEN is invalid (terminal state)")
    void cancelledToOpen_throwsException() {
        assertThrows(InvalidStateTransitionException.class,
                () -> stateMachineService.validateTransition(CANCELLED, OPEN));
    }

    @Test
    @DisplayName("CANCELLED → IN_PROGRESS is invalid (terminal state)")
    void cancelledToInProgress_throwsException() {
        assertThrows(InvalidStateTransitionException.class,
                () -> stateMachineService.validateTransition(CANCELLED, IN_PROGRESS));
    }

    @Test
    @DisplayName("CANCELLED → RESOLVED is invalid (terminal state)")
    void cancelledToResolved_throwsException() {
        assertThrows(InvalidStateTransitionException.class,
                () -> stateMachineService.validateTransition(CANCELLED, RESOLVED));
    }

    @Test
    @DisplayName("CANCELLED → CLOSED is invalid (terminal state)")
    void cancelledToClosed_throwsException() {
        assertThrows(InvalidStateTransitionException.class,
                () -> stateMachineService.validateTransition(CANCELLED, CLOSED));
    }

    // =========================================================
    // EXCEPTION MESSAGE VERIFICATION
    // =========================================================

    @Test
    @DisplayName("Exception message includes both states")
    void exceptionMessage_containsBothStates() {
        InvalidStateTransitionException ex = assertThrows(
                InvalidStateTransitionException.class,
                () -> stateMachineService.validateTransition(CLOSED, OPEN)
        );
        assertTrue(ex.getMessage().contains("CLOSED"), "Message should contain FROM state");
        assertTrue(ex.getMessage().contains("OPEN"), "Message should contain TO state");
    }
}
