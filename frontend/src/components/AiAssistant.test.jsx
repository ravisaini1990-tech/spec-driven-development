import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import AiAssistant from './AiAssistant.jsx';
import { askAi } from '../api/ticketApi.js';

vi.mock('../api/ticketApi.js', () => ({
  askAi: vi.fn(),
}));

function openPanelAndAsk(question) {
  fireEvent.click(screen.getByTitle('Open AI Assistant'));
  fireEvent.change(screen.getByPlaceholderText(/authentication tickets/i), {
    target: { value: question },
  });
  fireEvent.click(screen.getByRole('button', { name: /ask/i }));
}

describe('AiAssistant', () => {
  beforeEach(() => {
    askAi.mockReset();
  });

  it('renders a grounded answer with clickable ticket citations', async () => {
    askAi.mockResolvedValue({
      answer: 'Payment failures were caused by declined cards (Ticket #101).',
      ticket_ids: ['101'],
      no_relevant_tickets: false,
    });

    render(<AiAssistant onNavigateToTicket={() => {}} />);
    openPanelAndAsk('What caused previous payment failures?');

    await waitFor(() => {
      expect(screen.getByText(/Payment failures were caused by declined cards/)).toBeInTheDocument();
    });
    expect(screen.getByText('#101')).toBeInTheDocument();
  });

  it('shows an honest no-match message instead of a fabricated answer when no tickets are relevant', async () => {
    askAi.mockResolvedValue({
      answer: 'No relevant tickets found for your query.',
      ticket_ids: [],
      no_relevant_tickets: true,
    });

    render(<AiAssistant onNavigateToTicket={() => {}} />);
    openPanelAndAsk('Do we support quantum teleportation refunds?');

    await waitFor(() => {
      expect(screen.getByText(/No relevant tickets found for your query\. Try a different question/)).toBeInTheDocument();
    });
  });

  it('calls onNavigateToTicket with the ticket id when a citation chip is clicked', async () => {
    const onNavigateToTicket = vi.fn();
    askAi.mockResolvedValue({
      answer: 'See Ticket #42 for details.',
      ticket_ids: ['42'],
      no_relevant_tickets: false,
    });

    render(<AiAssistant onNavigateToTicket={onNavigateToTicket} />);
    openPanelAndAsk('Any shipment tracking issues?');

    const chip = await screen.findByText('#42');
    fireEvent.click(chip);

    expect(onNavigateToTicket).toHaveBeenCalledWith(42);
  });

  it('displays an error message when the AI request fails', async () => {
    askAi.mockRejectedValue({ data: { error: 'AI service unavailable' } });

    render(<AiAssistant onNavigateToTicket={() => {}} />);
    openPanelAndAsk('What caused previous payment failures?');

    await waitFor(() => {
      expect(screen.getByText('AI service unavailable')).toBeInTheDocument();
    });
  });

  it('disables the Ask button while the question is blank', () => {
    render(<AiAssistant onNavigateToTicket={() => {}} />);
    fireEvent.click(screen.getByTitle('Open AI Assistant'));

    expect(screen.getByRole('button', { name: /ask/i })).toBeDisabled();
  });
});
