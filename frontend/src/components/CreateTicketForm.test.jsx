import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import CreateTicketForm from './CreateTicketForm.jsx';
import { createTicket } from '../api/ticketApi.js';

vi.mock('../api/ticketApi.js', () => ({
  createTicket: vi.fn(),
}));

describe('CreateTicketForm', () => {
  beforeEach(() => {
    createTicket.mockReset();
  });

  it('submits title/description and calls onCreated with the created ticket', async () => {
    const onCreated = vi.fn();
    createTicket.mockResolvedValue({ id: 1, title: 'Login broken', status: 'OPEN' });

    render(<CreateTicketForm onCreated={onCreated} onCancel={() => {}} />);

    fireEvent.change(screen.getByPlaceholderText('Brief summary of the issue'), {
      target: { value: 'Login broken' },
    });
    fireEvent.change(screen.getByPlaceholderText('Detailed description of the problem...'), {
      target: { value: 'Users cannot log in on Safari.' },
    });
    fireEvent.click(screen.getByRole('button', { name: /create ticket/i }));

    await waitFor(() => expect(onCreated).toHaveBeenCalledWith({ id: 1, title: 'Login broken', status: 'OPEN' }));
    expect(createTicket).toHaveBeenCalledWith(expect.objectContaining({
      title: 'Login broken',
      description: 'Users cannot log in on Safari.',
    }));
  });

  it('shows backend field validation errors returned as 400 without calling onCreated', async () => {
    const onCreated = vi.fn();
    createTicket.mockRejectedValue({
      status: 400,
      data: { errors: { title: 'Title must not be blank' } },
    });

    render(<CreateTicketForm onCreated={onCreated} onCancel={() => {}} />);
    fireEvent.click(screen.getByRole('button', { name: /create ticket/i }));

    await waitFor(() => {
      expect(screen.getByText('Title must not be blank')).toBeInTheDocument();
    });
    expect(onCreated).not.toHaveBeenCalled();
  });

  it('shows a generic server error message for non-validation failures', async () => {
    createTicket.mockRejectedValue({ status: 500, data: { error: 'Internal server error' } });

    render(<CreateTicketForm onCreated={() => {}} onCancel={() => {}} />);
    fireEvent.click(screen.getByRole('button', { name: /create ticket/i }));

    await waitFor(() => {
      expect(screen.getByText('Internal server error')).toBeInTheDocument();
    });
  });
});
