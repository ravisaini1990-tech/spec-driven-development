import React, { useState } from 'react';
import TicketList from './components/TicketList.jsx';
import TicketDetail from './components/TicketDetail.jsx';
import CreateTicketForm from './components/CreateTicketForm.jsx';
import AiAssistant from './components/AiAssistant.jsx';

const VIEWS = {
  LIST: 'list',
  DETAIL: 'detail',
  CREATE: 'create',
};

export default function App() {
  const [view, setView] = useState(VIEWS.LIST);
  const [selectedTicketId, setSelectedTicketId] = useState(null);

  function navigateToTicket(id) {
    setSelectedTicketId(id);
    setView(VIEWS.DETAIL);
  }

  function navigateToList() {
    setSelectedTicketId(null);
    setView(VIEWS.LIST);
  }

  function navigateToCreate() {
    setView(VIEWS.CREATE);
  }

  function handleTicketCreated(ticket) {
    navigateToTicket(ticket.id);
  }

  return (
    <div style={{ minHeight: '100vh', background: '#f9fafb', fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif' }}>
      {/* Top navigation bar */}
      <header style={{
        background: '#1e40af',
        padding: '0 24px',
        height: '56px',
        display: 'flex',
        alignItems: 'center',
        boxShadow: '0 1px 3px rgba(0,0,0,0.2)',
      }}>
        <button
          onClick={navigateToList}
          style={{ background: 'none', border: 'none', cursor: 'pointer', color: '#fff', fontSize: '18px', fontWeight: '700', padding: 0, display: 'flex', alignItems: 'center', gap: '8px' }}
        >
          <span style={{ fontSize: '20px' }}>🎫</span>
          SupportDesk
        </button>
        <div style={{ flex: 1 }} />
        {view !== VIEWS.CREATE && (
          <button
            onClick={navigateToCreate}
            style={{ padding: '7px 16px', background: 'rgba(255,255,255,0.15)', border: '1px solid rgba(255,255,255,0.3)', borderRadius: '6px', color: '#fff', cursor: 'pointer', fontSize: '14px', fontWeight: '500' }}
          >
            + New Ticket
          </button>
        )}
      </header>

      {/* Main content */}
      <main>
        {view === VIEWS.LIST && (
          <TicketList
            onSelectTicket={navigateToTicket}
            onCreateNew={navigateToCreate}
          />
        )}
        {view === VIEWS.CREATE && (
          <div style={{ padding: '32px 24px' }}>
            <CreateTicketForm
              onCreated={handleTicketCreated}
              onCancel={navigateToList}
            />
          </div>
        )}
        {view === VIEWS.DETAIL && selectedTicketId && (
          <TicketDetail
            ticketId={selectedTicketId}
            onBack={navigateToList}
            onNavigateToTicket={navigateToTicket}
          />
        )}
      </main>

      {/* AI Assistant floating panel — always visible */}
      <AiAssistant onNavigateToTicket={navigateToTicket} />
    </div>
  );
}
