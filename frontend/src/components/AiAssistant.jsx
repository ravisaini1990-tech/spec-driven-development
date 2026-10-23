import React, { useState, useRef, useEffect } from 'react';
import { askAi } from '../api/ticketApi.js';

export default function AiAssistant({ onNavigateToTicket }) {
  const [open, setOpen] = useState(false);
  const [question, setQuestion] = useState('');
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState(null);
  const [error, setError] = useState('');
  const inputRef = useRef(null);

  useEffect(() => {
    if (open && inputRef.current) {
      inputRef.current.focus();
    }
  }, [open]);

  async function handleAsk() {
    if (!question.trim() || loading) return;
    setError('');
    setResult(null);
    setLoading(true);

    try {
      const data = await askAi(question.trim());
      setResult(data);
    } catch (err) {
      setError(err.data?.error || 'Failed to get answer from AI assistant.');
    } finally {
      setLoading(false);
    }
  }

  function handleKeyDown(e) {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleAsk();
    }
  }

  function handleReset() {
    setQuestion('');
    setResult(null);
    setError('');
  }

  return (
    <div style={{ position: 'fixed', bottom: '24px', right: '24px', zIndex: 1000 }}>
      {/* Panel */}
      {open && (
        <div style={{
          position: 'absolute',
          bottom: '60px',
          right: 0,
          width: '380px',
          background: '#fff',
          borderRadius: '12px',
          boxShadow: '0 20px 60px rgba(0,0,0,0.15)',
          border: '1px solid #e5e7eb',
          overflow: 'hidden',
        }}>
          {/* Header */}
          <div style={{ background: '#1e40af', padding: '14px 18px', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <div>
              <div style={{ color: '#fff', fontWeight: '700', fontSize: '15px' }}>AI Assistant</div>
              <div style={{ color: '#bfdbfe', fontSize: '12px' }}>Ask questions about your tickets</div>
            </div>
            <button
              onClick={handleReset}
              style={{ background: 'rgba(255,255,255,0.15)', border: 'none', color: '#fff', cursor: 'pointer', borderRadius: '4px', padding: '4px 8px', fontSize: '12px' }}
            >
              Clear
            </button>
          </div>

          {/* Body */}
          <div style={{ padding: '16px' }}>
            <div style={{ marginBottom: '12px' }}>
              <textarea
                ref={inputRef}
                value={question}
                onChange={e => setQuestion(e.target.value)}
                onKeyDown={handleKeyDown}
                placeholder="e.g. Which authentication tickets are open?"
                style={{
                  width: '100%',
                  padding: '10px 12px',
                  border: '1px solid #d1d5db',
                  borderRadius: '8px',
                  fontSize: '14px',
                  resize: 'none',
                  minHeight: '72px',
                  boxSizing: 'border-box',
                  fontFamily: 'inherit',
                  lineHeight: '1.5',
                }}
              />
            </div>

            <button
              onClick={handleAsk}
              disabled={loading || !question.trim()}
              style={{
                width: '100%',
                padding: '10px',
                background: '#2563eb',
                color: '#fff',
                border: 'none',
                borderRadius: '8px',
                cursor: (loading || !question.trim()) ? 'not-allowed' : 'pointer',
                fontSize: '14px',
                fontWeight: '600',
                opacity: (loading || !question.trim()) ? 0.6 : 1,
                transition: 'opacity 0.15s',
              }}
            >
              {loading ? 'Searching tickets...' : 'Ask'}
            </button>

            {loading && (
              <div style={{ marginTop: '12px', textAlign: 'center', color: '#6b7280', fontSize: '13px' }}>
                Searching ticket knowledge base...
              </div>
            )}

            {error && !loading && (
              <div style={{ marginTop: '12px', background: '#fee2e2', color: '#991b1b', padding: '10px', borderRadius: '8px', fontSize: '13px' }}>
                {error}
              </div>
            )}

            {result && !loading && (
              <div style={{ marginTop: '14px' }}>
                {result.no_relevant_tickets ? (
                  <div style={{ background: '#fefce8', border: '1px solid #fde68a', borderRadius: '8px', padding: '12px', fontSize: '13px', color: '#92400e' }}>
                    No relevant tickets found for your query. Try a different question or check if tickets have been created.
                  </div>
                ) : (
                  <div>
                    <div style={{ background: '#f0fdf4', border: '1px solid #bbf7d0', borderRadius: '8px', padding: '12px', fontSize: '14px', color: '#166534', lineHeight: '1.6', marginBottom: '10px', whiteSpace: 'pre-wrap' }}>
                      {result.answer}
                    </div>
                    {result.ticket_ids && result.ticket_ids.length > 0 && (
                      <div>
                        <div style={{ fontSize: '12px', fontWeight: '600', color: '#6b7280', marginBottom: '6px' }}>Referenced tickets:</div>
                        <div style={{ display: 'flex', gap: '6px', flexWrap: 'wrap' }}>
                          {result.ticket_ids.map(id => (
                            <button
                              key={id}
                              onClick={() => onNavigateToTicket && onNavigateToTicket(Number(id))}
                              style={{
                                padding: '3px 10px',
                                background: '#dbeafe',
                                color: '#1e40af',
                                border: '1px solid #93c5fd',
                                borderRadius: '12px',
                                cursor: 'pointer',
                                fontSize: '12px',
                                fontWeight: '600',
                              }}
                            >
                              #{id}
                            </button>
                          ))}
                        </div>
                      </div>
                    )}
                  </div>
                )}
              </div>
            )}
          </div>
        </div>
      )}

      {/* Toggle button */}
      <button
        onClick={() => setOpen(prev => !prev)}
        style={{
          width: '52px',
          height: '52px',
          borderRadius: '50%',
          background: open ? '#1e40af' : '#2563eb',
          color: '#fff',
          border: 'none',
          cursor: 'pointer',
          fontSize: '22px',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          boxShadow: '0 4px 14px rgba(37,99,235,0.4)',
          transition: 'background 0.2s, transform 0.2s',
          transform: open ? 'rotate(45deg)' : 'none',
        }}
        title={open ? 'Close AI Assistant' : 'Open AI Assistant'}
      >
        {open ? '✕' : '✦'}
      </button>
    </div>
  );
}
