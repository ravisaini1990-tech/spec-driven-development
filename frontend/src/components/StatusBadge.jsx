import React from 'react';

const STATUS_STYLES = {
  OPEN:        { background: '#dbeafe', color: '#1e40af', border: '1px solid #93c5fd' },
  IN_PROGRESS: { background: '#ffedd5', color: '#c2410c', border: '1px solid #fdba74' },
  RESOLVED:    { background: '#dcfce7', color: '#166534', border: '1px solid #86efac' },
  CLOSED:      { background: '#f3f4f6', color: '#374151', border: '1px solid #d1d5db' },
  CANCELLED:   { background: '#fee2e2', color: '#991b1b', border: '1px solid #fca5a5' },
};

const STATUS_LABELS = {
  OPEN:        'Open',
  IN_PROGRESS: 'In Progress',
  RESOLVED:    'Resolved',
  CLOSED:      'Closed',
  CANCELLED:   'Cancelled',
};

export default function StatusBadge({ status }) {
  const style = STATUS_STYLES[status] || STATUS_STYLES.OPEN;
  return (
    <span style={{
      ...style,
      padding: '2px 10px',
      borderRadius: '12px',
      fontSize: '12px',
      fontWeight: '600',
      display: 'inline-block',
      whiteSpace: 'nowrap',
    }}>
      {STATUS_LABELS[status] || status}
    </span>
  );
}
