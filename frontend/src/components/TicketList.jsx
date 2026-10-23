import React, { useState, useEffect, useCallback } from 'react';
import { listTickets } from '../api/ticketApi.js';
import StatusBadge from './StatusBadge.jsx';

const STATUSES = ['', 'OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED', 'CANCELLED'];
const PRIORITY_COLORS = {
  LOW: '#6b7280',
  MEDIUM: '#2563eb',
  HIGH: '#d97706',
  CRITICAL: '#dc2626',
};

export default function TicketList({ onSelectTicket, onCreateNew }) {
  const [tickets, setTickets] = useState([]);
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const fetchTickets = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const data = await listTickets(search || undefined, statusFilter || undefined);
      setTickets(data);
    } catch (err) {
      setError('Failed to load tickets.');
    } finally {
      setLoading(false);
    }
  }, [search, statusFilter]);

  useEffect(() => {
    const timer = setTimeout(fetchTickets, 300);
    return () => clearTimeout(timer);
  }, [fetchTickets]);

  return (
    <div style={{ padding: '24px', maxWidth: '1100px', margin: '0 auto' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px' }}>
        <h1 style={{ margin: 0, color: '#111827', fontSize: '24px' }}>Support Tickets</h1>
        <button
          onClick={onCreateNew}
          style={{ padding: '8px 20px', background: '#2563eb', color: '#fff', border: 'none', borderRadius: '6px', cursor: 'pointer', fontSize: '14px', fontWeight: '600' }}
        >
          + New Ticket
        </button>
      </div>

      <div style={{ display: 'flex', gap: '12px', marginBottom: '20px' }}>
        <input
          type="text"
          placeholder="Search tickets..."
          value={search}
          onChange={e => setSearch(e.target.value)}
          style={{ flex: 1, padding: '8px 12px', border: '1px solid #d1d5db', borderRadius: '6px', fontSize: '14px' }}
        />
        <select
          value={statusFilter}
          onChange={e => setStatusFilter(e.target.value)}
          style={{ padding: '8px 12px', border: '1px solid #d1d5db', borderRadius: '6px', fontSize: '14px', minWidth: '150px' }}
        >
          {STATUSES.map(s => (
            <option key={s} value={s}>{s === '' ? 'All Statuses' : s.replace('_', ' ')}</option>
          ))}
        </select>
      </div>

      {error && (
        <div style={{ background: '#fee2e2', color: '#991b1b', padding: '10px', borderRadius: '6px', marginBottom: '16px' }}>{error}</div>
      )}

      {loading ? (
        <div style={{ textAlign: 'center', padding: '40px', color: '#6b7280' }}>Loading tickets...</div>
      ) : tickets.length === 0 ? (
        <div style={{ textAlign: 'center', padding: '40px', color: '#6b7280' }}>
          No tickets found. {!search && !statusFilter && 'Create one to get started!'}
        </div>
      ) : (
        <div style={{ background: '#fff', borderRadius: '8px', border: '1px solid #e5e7eb', overflow: 'hidden' }}>
          <table style={{ width: '100%', borderCollapse: 'collapse' }}>
            <thead>
              <tr style={{ background: '#f9fafb', borderBottom: '1px solid #e5e7eb' }}>
                <th style={{ padding: '12px 16px', textAlign: 'left', fontSize: '12px', fontWeight: '600', color: '#6b7280', textTransform: 'uppercase' }}>ID</th>
                <th style={{ padding: '12px 16px', textAlign: 'left', fontSize: '12px', fontWeight: '600', color: '#6b7280', textTransform: 'uppercase' }}>Title</th>
                <th style={{ padding: '12px 16px', textAlign: 'left', fontSize: '12px', fontWeight: '600', color: '#6b7280', textTransform: 'uppercase' }}>Status</th>
                <th style={{ padding: '12px 16px', textAlign: 'left', fontSize: '12px', fontWeight: '600', color: '#6b7280', textTransform: 'uppercase' }}>Priority</th>
                <th style={{ padding: '12px 16px', textAlign: 'left', fontSize: '12px', fontWeight: '600', color: '#6b7280', textTransform: 'uppercase' }}>Assignee</th>
                <th style={{ padding: '12px 16px', textAlign: 'left', fontSize: '12px', fontWeight: '600', color: '#6b7280', textTransform: 'uppercase' }}>Category</th>
              </tr>
            </thead>
            <tbody>
              {tickets.map((ticket, idx) => (
                <tr
                  key={ticket.id}
                  onClick={() => onSelectTicket(ticket.id)}
                  style={{
                    borderBottom: idx < tickets.length - 1 ? '1px solid #f3f4f6' : 'none',
                    cursor: 'pointer',
                    transition: 'background 0.1s',
                  }}
                  onMouseEnter={e => e.currentTarget.style.background = '#f9fafb'}
                  onMouseLeave={e => e.currentTarget.style.background = '#fff'}
                >
                  <td style={{ padding: '12px 16px', fontSize: '14px', color: '#6b7280' }}>#{ticket.id}</td>
                  <td style={{ padding: '12px 16px', fontSize: '14px', fontWeight: '500', color: '#111827', maxWidth: '350px' }}>
                    <div style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{ticket.title}</div>
                  </td>
                  <td style={{ padding: '12px 16px' }}><StatusBadge status={ticket.status} /></td>
                  <td style={{ padding: '12px 16px', fontSize: '14px', color: PRIORITY_COLORS[ticket.priority] || '#6b7280', fontWeight: '500' }}>
                    {ticket.priority}
                  </td>
                  <td style={{ padding: '12px 16px', fontSize: '14px', color: '#374151' }}>{ticket.assignee || '—'}</td>
                  <td style={{ padding: '12px 16px', fontSize: '14px', color: '#374151' }}>{ticket.category || '—'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
