import React, { useState } from 'react';
import { createTicket } from '../api/ticketApi.js';

const inputStyle = {
  width: '100%',
  padding: '8px 12px',
  border: '1px solid #d1d5db',
  borderRadius: '6px',
  fontSize: '14px',
  boxSizing: 'border-box',
  marginTop: '4px',
};

const labelStyle = {
  display: 'block',
  fontSize: '13px',
  fontWeight: '600',
  color: '#374151',
  marginBottom: '2px',
};

export default function CreateTicketForm({ onCreated, onCancel }) {
  const [form, setForm] = useState({
    title: '',
    description: '',
    priority: 'MEDIUM',
    assignee: '',
    category: '',
  });
  const [errors, setErrors] = useState({});
  const [submitting, setSubmitting] = useState(false);
  const [serverError, setServerError] = useState('');

  function handleChange(e) {
    setForm({ ...form, [e.target.name]: e.target.value });
    setErrors({ ...errors, [e.target.name]: '' });
  }

  async function handleSubmit(e) {
    e.preventDefault();
    setServerError('');
    setSubmitting(true);

    try {
      const ticket = await createTicket({
        title: form.title,
        description: form.description,
        priority: form.priority || undefined,
        assignee: form.assignee || undefined,
        category: form.category || undefined,
      });
      onCreated(ticket);
    } catch (err) {
      if (err.status === 400 && err.data?.errors) {
        setErrors(err.data.errors);
      } else {
        setServerError(err.data?.error || 'Failed to create ticket.');
      }
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div style={{ background: '#fff', padding: '24px', borderRadius: '8px', maxWidth: '600px', margin: '0 auto' }}>
      <h2 style={{ marginTop: 0, color: '#111827' }}>New Support Ticket</h2>
      {serverError && (
        <div style={{ background: '#fee2e2', color: '#991b1b', padding: '10px', borderRadius: '6px', marginBottom: '16px' }}>
          {serverError}
        </div>
      )}
      <form onSubmit={handleSubmit}>
        <div style={{ marginBottom: '16px' }}>
          <label style={labelStyle}>Title *</label>
          <input
            style={{ ...inputStyle, borderColor: errors.title ? '#ef4444' : '#d1d5db' }}
            name="title"
            value={form.title}
            onChange={handleChange}
            placeholder="Brief summary of the issue"
          />
          {errors.title && <div style={{ color: '#ef4444', fontSize: '12px', marginTop: '4px' }}>{errors.title}</div>}
        </div>

        <div style={{ marginBottom: '16px' }}>
          <label style={labelStyle}>Description *</label>
          <textarea
            style={{ ...inputStyle, minHeight: '100px', resize: 'vertical', borderColor: errors.description ? '#ef4444' : '#d1d5db' }}
            name="description"
            value={form.description}
            onChange={handleChange}
            placeholder="Detailed description of the problem..."
          />
          {errors.description && <div style={{ color: '#ef4444', fontSize: '12px', marginTop: '4px' }}>{errors.description}</div>}
        </div>

        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: '16px', marginBottom: '16px' }}>
          <div>
            <label style={labelStyle}>Priority</label>
            <select style={inputStyle} name="priority" value={form.priority} onChange={handleChange}>
              <option value="LOW">Low</option>
              <option value="MEDIUM">Medium</option>
              <option value="HIGH">High</option>
              <option value="CRITICAL">Critical</option>
            </select>
          </div>
          <div>
            <label style={labelStyle}>Assignee</label>
            <input style={inputStyle} name="assignee" value={form.assignee} onChange={handleChange} placeholder="e.g. alice" />
          </div>
          <div>
            <label style={labelStyle}>Category</label>
            <input style={inputStyle} name="category" value={form.category} onChange={handleChange} placeholder="e.g. billing" />
          </div>
        </div>

        <div style={{ display: 'flex', gap: '12px', justifyContent: 'flex-end' }}>
          <button
            type="button"
            onClick={onCancel}
            style={{ padding: '8px 20px', border: '1px solid #d1d5db', borderRadius: '6px', background: '#fff', cursor: 'pointer', fontSize: '14px' }}
          >
            Cancel
          </button>
          <button
            type="submit"
            disabled={submitting}
            style={{ padding: '8px 20px', background: '#2563eb', color: '#fff', border: 'none', borderRadius: '6px', cursor: submitting ? 'not-allowed' : 'pointer', fontSize: '14px', fontWeight: '600', opacity: submitting ? 0.7 : 1 }}
          >
            {submitting ? 'Creating...' : 'Create Ticket'}
          </button>
        </div>
      </form>
    </div>
  );
}
