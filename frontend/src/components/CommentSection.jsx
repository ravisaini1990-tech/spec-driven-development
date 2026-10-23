import React, { useState, useEffect } from 'react';
import { getComments, addComment } from '../api/ticketApi.js';

function formatDate(dt) {
  if (!dt) return '';
  return new Date(dt).toLocaleString();
}

export default function CommentSection({ ticketId }) {
  const [comments, setComments] = useState([]);
  const [loading, setLoading] = useState(false);
  const [form, setForm] = useState({ author: '', body: '' });
  const [errors, setErrors] = useState({});
  const [submitting, setSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState('');

  useEffect(() => {
    loadComments();
  }, [ticketId]);

  async function loadComments() {
    setLoading(true);
    try {
      const data = await getComments(ticketId);
      setComments(data);
    } catch (err) {
      console.error('Failed to load comments', err);
    } finally {
      setLoading(false);
    }
  }

  function handleChange(e) {
    setForm({ ...form, [e.target.name]: e.target.value });
    setErrors({ ...errors, [e.target.name]: '' });
  }

  async function handleSubmit(e) {
    e.preventDefault();
    setSubmitError('');

    const newErrors = {};
    if (!form.author.trim()) newErrors.author = 'Author is required';
    if (!form.body.trim()) newErrors.body = 'Comment body is required';
    if (Object.keys(newErrors).length > 0) {
      setErrors(newErrors);
      return;
    }

    setSubmitting(true);
    try {
      const comment = await addComment(ticketId, { author: form.author, body: form.body });
      setComments(prev => [...prev, comment]);
      setForm({ author: '', body: '' });
    } catch (err) {
      setSubmitError(err.data?.error || 'Failed to add comment.');
    } finally {
      setSubmitting(false);
    }
  }

  const inputStyle = {
    width: '100%',
    padding: '8px 12px',
    border: '1px solid #d1d5db',
    borderRadius: '6px',
    fontSize: '14px',
    boxSizing: 'border-box',
    marginTop: '4px',
  };

  return (
    <div style={{ marginTop: '32px' }}>
      <h3 style={{ color: '#111827', marginBottom: '16px', fontSize: '16px' }}>
        Comments ({comments.length})
      </h3>

      {loading ? (
        <div style={{ color: '#6b7280', fontSize: '14px' }}>Loading comments...</div>
      ) : comments.length === 0 ? (
        <div style={{ color: '#6b7280', fontSize: '14px', marginBottom: '16px' }}>No comments yet.</div>
      ) : (
        <div style={{ marginBottom: '20px' }}>
          {comments.map(comment => (
            <div key={comment.id} style={{
              background: '#f9fafb',
              border: '1px solid #e5e7eb',
              borderRadius: '8px',
              padding: '14px 16px',
              marginBottom: '10px',
            }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '8px' }}>
                <span style={{ fontWeight: '600', fontSize: '14px', color: '#374151' }}>{comment.author}</span>
                <span style={{ fontSize: '12px', color: '#9ca3af' }}>{formatDate(comment.created_at)}</span>
              </div>
              <p style={{ margin: 0, fontSize: '14px', color: '#4b5563', lineHeight: '1.6', whiteSpace: 'pre-wrap' }}>{comment.body}</p>
            </div>
          ))}
        </div>
      )}

      <div style={{ background: '#fff', border: '1px solid #e5e7eb', borderRadius: '8px', padding: '16px' }}>
        <h4 style={{ margin: '0 0 12px 0', fontSize: '14px', color: '#374151' }}>Add Comment</h4>
        {submitError && (
          <div style={{ background: '#fee2e2', color: '#991b1b', padding: '8px 12px', borderRadius: '6px', marginBottom: '12px', fontSize: '13px' }}>
            {submitError}
          </div>
        )}
        <form onSubmit={handleSubmit}>
          <div style={{ marginBottom: '12px' }}>
            <input
              style={{ ...inputStyle, borderColor: errors.author ? '#ef4444' : '#d1d5db' }}
              name="author"
              value={form.author}
              onChange={handleChange}
              placeholder="Your name"
            />
            {errors.author && <div style={{ color: '#ef4444', fontSize: '12px', marginTop: '4px' }}>{errors.author}</div>}
          </div>
          <div style={{ marginBottom: '12px' }}>
            <textarea
              style={{ ...inputStyle, minHeight: '80px', resize: 'vertical', borderColor: errors.body ? '#ef4444' : '#d1d5db' }}
              name="body"
              value={form.body}
              onChange={handleChange}
              placeholder="Write your comment..."
            />
            {errors.body && <div style={{ color: '#ef4444', fontSize: '12px', marginTop: '4px' }}>{errors.body}</div>}
          </div>
          <button
            type="submit"
            disabled={submitting}
            style={{ padding: '8px 18px', background: '#2563eb', color: '#fff', border: 'none', borderRadius: '6px', cursor: submitting ? 'not-allowed' : 'pointer', fontSize: '14px', fontWeight: '600', opacity: submitting ? 0.7 : 1 }}
          >
            {submitting ? 'Adding...' : 'Add Comment'}
          </button>
        </form>
      </div>
    </div>
  );
}
