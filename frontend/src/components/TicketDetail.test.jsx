import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import TicketDetail from './TicketDetail.jsx';
import { getTicket, transitionStatus } from '../api/ticketApi.js';

vi.mock('../api/ticketApi.js', () => ({
  getTicket: vi.fn(),
  updateTicket: vi.fn(),
  transitionStatus: vi.fn(),
  getComments: vi.fn(),
  addComment: vi.fn(),
}));

const { getComments } = await import('../api/ticketApi.js');

function baseTicket(overrides = {}) {
  return {
    id: 1,
    title: 'Login broken',
    description: 'Users cannot log in.',
    status: 'OPEN',
    priority: 'HIGH',
    assignee: 'alice',
    category: 'authentication',
    created_at: '2026-01-01T10:00:00',
    updated_at: '2026-01-01T10:00:00',
    ...overrides,
  };
}

describe('TicketDetail — status transitions', () => {
  beforeEach(() => {
    getTicket.mockReset();
    transitionStatus.mockReset();
    getComments.mockReset();
    getComments.mockResolvedValue([]);
  });

  it('only offers OPEN -> IN_PROGRESS and OPEN -> CANCELLED for an OPEN ticket', async () => {
    getTicket.mockResolvedValue(baseTicket({ status: 'OPEN' }));

    render(<TicketDetail ticketId={1} onBack={() => {}} onNavigateToTicket={() => {}} />);

    await screen.findByText('#1 — Login broken');

    const select = screen.getByDisplayValue('Select new status...').closest('select');
    const options = Array.from(select.querySelectorAll('option')).map(o => o.value);

    expect(options).toEqual(['', 'IN_PROGRESS', 'CANCELLED']);
  });

  it('offers no transitions for a CLOSED (terminal) ticket', async () => {
    getTicket.mockResolvedValue(baseTicket({ status: 'CLOSED' }));

    render(<TicketDetail ticketId={1} onBack={() => {}} onNavigateToTicket={() => {}} />);

    await screen.findByText('#1 — Login broken');

    expect(screen.queryByText('Transition Status')).not.toBeInTheDocument();
  });

  it('offers no transitions for a CANCELLED (terminal) ticket', async () => {
    getTicket.mockResolvedValue(baseTicket({ status: 'CANCELLED' }));

    render(<TicketDetail ticketId={1} onBack={() => {}} onNavigateToTicket={() => {}} />);

    await screen.findByText('#1 — Login broken');

    expect(screen.queryByText('Transition Status')).not.toBeInTheDocument();
  });

  it('surfaces the backend 409 error message when an invalid transition is somehow submitted', async () => {
    getTicket.mockResolvedValue(baseTicket({ status: 'OPEN' }));
    transitionStatus.mockRejectedValue({ data: { error: 'Invalid state transition from OPEN to CLOSED' } });

    render(<TicketDetail ticketId={1} onBack={() => {}} onNavigateToTicket={() => {}} />);
    await screen.findByText('#1 — Login broken');

    const select = screen.getByDisplayValue('Select new status...').closest('select');
    fireEvent.change(select, { target: { value: 'IN_PROGRESS' } });
    fireEvent.click(screen.getByRole('button', { name: /update status/i }));

    await waitFor(() => {
      expect(screen.getByText('Invalid state transition from OPEN to CLOSED')).toBeInTheDocument();
    });
  });
});
