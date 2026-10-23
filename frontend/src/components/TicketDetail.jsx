import React, { useState, useEffect } from 'react';
import { getTicket, updateTicket, transitionStatus } from '../api/ticketApi.js';
import { nextValidStatuses } from '../constants/ticketStateMachine.js';
import StatusBadge from './StatusBadge.jsx';
import CommentSection from './CommentSection.jsx';

const PRIORITY_OPTIONS = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'];

function formatDate(dt) {
  if (!dt) return '';
  return new Date(dt).toLocaleString();
}

const inputStyle = {
  width: '100%',
  padding: '7px 11px',
  border: '1px solid #d1d5db',
  borderRadius: '6px',
  fontSize: '14px',
  boxSizing: 'border-box',
};

export default function TicketDetail({ ticketId, onBack, onNavigateToTicket }) {
  const [ticket, setTicket] = useState(null);
  const [loading, setLoading] = useState(true);
  const [editMode, setEditMode] = useState(false);
  const [editForm, setEditForm] = useState({});
  const [saving, setSaving] = useState(false);
  const [saveError, setSaveError] = useState('');
  const [statusError, setStatusError] = useState('');
  const [selectedStatus, setSelectedStatus] = useState('');
  const [transitioningStatus, setTransitioningStatus] = useState(false);

  useEffect(() => {
    loadTicket();
  }, [ticketId]);

  async function loadTicket() {
    setLoading(true);
    try {
      const data = await getTicket(ticketId);
      setTicket(data);
      setEditForm({
        title: data.title,
        description: data.description,
        priority: data.priority,
        assignee: data.assignee || '',
        category: data.category || '',
      });
      setSelectedStatus('');
    } catch (err) {
      console.error('Failed to load ticket', err);
    } finally {
      setLoading(false);
    }
  }

  function handleEditChange(e) {
    setEditForm({ ...editForm, [e.target.name]: e.target.value });
  }

  async function handleSave() {
    setSaveError('');
    setSaving(true);
    try {
      const updated = await updateTicket(ticketId, editForm);
      setTicket(updated);
      setEditMode(false);
    } catch (err) {
      setSaveError(err.data?.error || 'Failed to save changes.');
    } finally {
      setSaving(false);
    }
  }

  async function handleStatusTransition() {
    if (!selectedStatus) return;
    setStatusError('');
    setTransitioningStatus(true);
    try {
      const updated = await transitionStatus(ticketId, selectedStatus);
      setTicket(updated);
      setSelectedStatus('');
    } catch (err) {
      setStatusError(err.data?.error || 'Failed to transition status.');
    } finally {
      setTransitioningStatus(false);
    }
  }

  if (loading) {
    return <div style={{ padding: '40px', textAlign: 'center', color: '#6b7280' }}>Loading ticket...</div>;
  }

  if (!ticket) {
    return <div style={{ padding: '40px', textAlign: 'center', color: '#ef4444' }}>Ticket not found.</div>;
  }

  const nextStatuses = nextValidStatuses(ticket.status);

  return (
    <div style={{ padding: '24px', maxWidth: '800px', margin: '0 auto' }}>
      <button
        onClick={onBack}
        style={{ background: 'none', border: 'none', cursor: 'pointer', color: '#2563eb', fontSize: '14px', padding: 0, marginBottom: '20px', display: 'flex', alignItems: 'center', gap: '4px' }}
      >
        ← Back to tickets
      </button>

      <div style={{ background: '#fff', borderRadius: '8px', border: '1px solid #e5e7eb', padding: '24px' }}>
        {/* Header */}
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '20px' }}>
          <div style={{ flex: 1 }}>
            {editMode ? (
              <input
                style={{ ...inputStyle, fontSize: '18px', fontWeight: '600' }}
                name="title"
                value={editForm.title}
                onChange={handleEditChange}
              />
            ) : (
              <h2 style={{ margin: 0, color: '#111827', fontSize: '20px' }}>#{ticket.id} — {ticket.title}</h2>
            )}
          </div>
          <div style={{ display: 'flex', gap: '8px', marginLeft: '16px' }}>
            {!editMode ? (
              <button
                onClick={() => setEditMode(true)}
                style={{ padding: '6px 14px', border: '1px solid #d1d5db', borderRadius: '6px', background: '#fff', cursor: 'pointer', fontSize: '13px' }}
              >
                Edit
              </button>
            ) : (
              <>
                <button
                  onClick={() => { setEditMode(false); setSaveError(''); }}
                  style={{ padding: '6px 14px', border: '1px solid #d1d5db', borderRadius: '6px', background: '#fff', cursor: 'pointer', fontSize: '13px' }}
                >
                  Cancel
                </button>
                <button
                  onClick={handleSave}
                  disabled={saving}
                  style={{ padding: '6px 14px', background: '#2563eb', color: '#fff', border: 'none', borderRadius: '6px', cursor: saving ? 'not-allowed' : 'pointer', fontSize: '13px', opacity: saving ? 0.7 : 1 }}
                >
                  {saving ? 'Saving...' : 'Save'}
                </button>
              </>
            )}
          </div>
        </div>

        {saveError && (
          <div style={{ background: '#fee2e2', color: '#991b1b', padding: '10px', borderRadius: '6px', marginBottom: '16px', fontSize: '13px' }}>{saveError}</div>
        )}

        {/* Meta row */}
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: '16px', marginBottom: '20px' }}>
          <div>
            <div style={{ fontSize: '11px', fontWeight: '600', color: '#9ca3af', textTransform: 'uppercase', marginBottom: '4px' }}>Status</div>
            <StatusBadge status={ticket.status} />
          </div>
          <div>
            <div style={{ fontSize: '11px', fontWeight: '600', color: '#9ca3af', textTransform: 'uppercase', marginBottom: '4px' }}>Priority</div>
            {editMode ? (
              <select style={inputStyle} name="priority" value={editForm.priority} onChange={handleEditChange}>
                {PRIORITY_OPTIONS.map(p => <option key={p} value={p}>{p}</option>)}
              </select>
            ) : (
              <div style={{ fontSize: '14px', color: '#374151', fontWeight: '500' }}>{ticket.priority}</div>
            )}
          </div>
          <div>
            <div style={{ fontSize: '11px', fontWeight: '600', color: '#9ca3af', textTransform: 'uppercase', marginBottom: '4px' }}>Assignee</div>
            {editMode ? (
              <input style={inputStyle} name="assignee" value={editForm.assignee} onChange={handleEditChange} placeholder="Unassigned" />
            ) : (
              <div style={{ fontSize: '14px', color: '#374151' }}>{ticket.assignee || '—'}</div>
            )}
          </div>
          <div>
            <div style={{ fontSize: '11px', fontWeight: '600', color: '#9ca3af', textTransform: 'uppercase', marginBottom: '4px' }}>Category</div>
            {editMode ? (
              <input style={inputStyle} name="category" value={editForm.category} onChange={handleEditChange} placeholder="General" />
            ) : (
              <div style={{ fontSize: '14px', color: '#374151' }}>{ticket.category || '—'}</div>
            )}
          </div>
        </div>

        {/* Description */}
        <div style={{ marginBottom: '20px' }}>
          <div style={{ fontSize: '11px', fontWeight: '600', color: '#9ca3af', textTransform: 'uppercase', marginBottom: '8px' }}>Description</div>
          {editMode ? (
            <textarea
              style={{ ...inputStyle, minHeight: '120px', resize: 'vertical' }}
              name="description"
              value={editForm.description}
              onChange={handleEditChange}
            />
          ) : (
            <p style={{ margin: 0, fontSize: '14px', color: '#374151', lineHeight: '1.6', whiteSpace: 'pre-wrap' }}>{ticket.description}</p>
          )}
        </div>

        {/* Timestamps */}
        <div style={{ display: 'flex', gap: '24px', fontSize: '12px', color: '#9ca3af', marginBottom: '20px' }}>
          <span>Created: {formatDate(ticket.created_at)}</span>
          <span>Updated: {formatDate(ticket.updated_at)}</span>
        </div>

        {/* Status Transition */}
        {nextStatuses.length > 0 && (
          <div style={{ borderTop: '1px solid #f3f4f6', paddingTop: '20px', marginBottom: '4px' }}>
            <div style={{ fontSize: '13px', fontWeight: '600', color: '#374151', marginBottom: '10px' }}>Transition Status</div>
            {statusError && (
              <div style={{ background: '#fee2e2', color: '#991b1b', padding: '8px 12px', borderRadius: '6px', marginBottom: '10px', fontSize: '13px' }}>{statusError}</div>
            )}
            <div style={{ display: 'flex', gap: '10px', alignItems: 'center' }}>
              <select
                value={selectedStatus}
                onChange={e => { setSelectedStatus(e.target.value); setStatusError(''); }}
                style={{ padding: '7px 12px', border: '1px solid #d1d5db', borderRadius: '6px', fontSize: '14px', minWidth: '180px' }}
              >
                <option value="">Select new status...</option>
                {nextStatuses.map(s => (
                  <option key={s} value={s}>{s.replace('_', ' ')}</option>
                ))}
              </select>
              <button
                onClick={handleStatusTransition}
                disabled={!selectedStatus || transitioningStatus}
                style={{ padding: '7px 16px', background: '#059669', color: '#fff', border: 'none', borderRadius: '6px', cursor: (!selectedStatus || transitioningStatus) ? 'not-allowed' : 'pointer', fontSize: '14px', fontWeight: '600', opacity: (!selectedStatus || transitioningStatus) ? 0.5 : 1 }}
              >
                {transitioningStatus ? 'Updating...' : 'Update Status'}
              </button>
            </div>
          </div>
        )}
      </div>

      <CommentSection ticketId={ticketId} />
    </div>
  );
}
