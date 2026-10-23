const BASE_URL = '/api';

async function handleResponse(res) {
  if (!res.ok) {
    const data = await res.json().catch(() => ({}));
    throw { status: res.status, data };
  }
  return res.json();
}

export async function createTicket(payload) {
  const res = await fetch(`${BASE_URL}/tickets`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  });
  return handleResponse(res);
}

export async function listTickets(search, status) {
  const params = new URLSearchParams();
  if (search) params.set('search', search);
  if (status) params.set('status', status);
  const query = params.toString() ? `?${params.toString()}` : '';
  const res = await fetch(`${BASE_URL}/tickets${query}`);
  return handleResponse(res);
}

export async function getTicket(id) {
  const res = await fetch(`${BASE_URL}/tickets/${id}`);
  return handleResponse(res);
}

export async function updateTicket(id, payload) {
  const res = await fetch(`${BASE_URL}/tickets/${id}`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  });
  return handleResponse(res);
}

export async function transitionStatus(id, status) {
  const res = await fetch(`${BASE_URL}/tickets/${id}/status`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ status }),
  });
  return handleResponse(res);
}

export async function getComments(ticketId) {
  const res = await fetch(`${BASE_URL}/tickets/${ticketId}/comments`);
  return handleResponse(res);
}

export async function addComment(ticketId, payload) {
  const res = await fetch(`${BASE_URL}/tickets/${ticketId}/comments`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  });
  return handleResponse(res);
}

export async function askAi(question) {
  const res = await fetch(`${BASE_URL}/ai/ask`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ question }),
  });
  return handleResponse(res);
}
