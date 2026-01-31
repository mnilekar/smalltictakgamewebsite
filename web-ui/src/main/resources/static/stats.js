const GAME_API = localStorage.getItem('gameApiBase') || 'http://localhost:8091/api/game';

const historyState = { limit: 20, offset: 0, lastCount: 0 };
const globalState = { limit: 50, offset: 0, lastCount: 0 };

function fmtDuration(seconds) {
  if (seconds == null) return '—';
  const s = Math.max(0, Number(seconds));
  const hh = String(Math.floor(s / 3600)).padStart(2, '0');
  const mm = String(Math.floor((s % 3600) / 60)).padStart(2, '0');
  const ss = String(Math.floor(s % 60)).padStart(2, '0');
  return `${hh}:${mm}:${ss}`;
}

function fmtDate(value) {
  if (!value) return '—';
  const d = new Date(value);
  return Number.isNaN(d.getTime()) ? '—' : d.toLocaleString();
}

function renderCards(stats) {
  const container = document.getElementById('personal-cards');
  container.innerHTML = '';
  const items = [
    { label: 'Played', value: stats.played },
    { label: 'Wins', value: stats.wins },
    { label: 'Losses', value: stats.losses },
    { label: 'Ties', value: stats.ties },
    { label: 'Forfeits', value: stats.forfeits },
    { label: 'Win Rate %', value: (stats.avgWinRate * 100).toFixed(1) },
    { label: 'Total Time', value: fmtDuration(stats.totalTimeSeconds) }
  ];
  items.forEach(item => {
    const card = document.createElement('div');
    card.className = 'stat-card';
    card.innerHTML = `<div class="stat-label">${item.label}</div><div class="stat-value">${item.value}</div>`;
    container.appendChild(card);
  });
}

async function loadMyStats() {
  const resp = await authFetch(`${GAME_API}/stats/me`);
  if (!resp.ok) throw new Error('Failed to load stats');
  const data = await resp.json();
  renderCards(data);
}

async function loadHistory() {
  const params = new URLSearchParams();
  params.set('limit', historyState.limit);
  params.set('offset', historyState.offset);
  const from = document.getElementById('filter-from').value;
  const to = document.getElementById('filter-to').value;
  const mode = document.getElementById('filter-mode').value;
  if (from) params.set('from', from);
  if (to) params.set('to', to);
  if (mode) params.set('mode', mode);

  const resp = await authFetch(`${GAME_API}/history/me?${params.toString()}`);
  if (!resp.ok) throw new Error('Failed to load history');
  const rows = await resp.json();
  historyState.lastCount = rows.length;

  const body = document.getElementById('history-body');
  body.innerHTML = '';
  rows.forEach(row => {
    const tr = document.createElement('tr');
    tr.innerHTML = `
      <td>${row.gameId}</td>
      <td>${row.mode}</td>
      <td>${row.playedAgainst}</td>
      <td>${row.status}</td>
      <td>${row.winner || '—'}</td>
      <td>${fmtDuration(row.durationSeconds)}</td>
      <td>${fmtDate(row.createdAt)}</td>
      <td>${fmtDate(row.endedAt)}</td>
    `;
    body.appendChild(tr);
  });

  document.getElementById('history-page').textContent = `Page ${historyState.offset / historyState.limit + 1}`;
  document.getElementById('history-prev').disabled = historyState.offset === 0;
  document.getElementById('history-next').disabled = rows.length < historyState.limit;
}

async function loadGlobal() {
  const params = new URLSearchParams();
  params.set('limit', globalState.limit);
  params.set('offset', globalState.offset);
  const minMatches = document.getElementById('global-min').value;
  if (minMatches) params.set('minMatches', minMatches);

  const resp = await authFetch(`${GAME_API}/stats/global?${params.toString()}`);
  if (!resp.ok) throw new Error('Failed to load global stats');
  const rows = await resp.json();
  globalState.lastCount = rows.length;

  const body = document.getElementById('global-body');
  body.innerHTML = '';
  rows.forEach(row => {
    const tr = document.createElement('tr');
    tr.innerHTML = `
      <td>${row.rank}</td>
      <td>${row.username}</td>
      <td>${row.played}</td>
      <td>${row.wins}</td>
      <td>${row.losses}</td>
      <td>${row.ties}</td>
      <td>${row.forfeits}</td>
      <td>${(row.winRate * 100).toFixed(1)}</td>
    `;
    body.appendChild(tr);
  });

  document.getElementById('global-page').textContent = `Page ${globalState.offset / globalState.limit + 1}`;
  document.getElementById('global-prev').disabled = globalState.offset === 0;
  document.getElementById('global-next').disabled = rows.length < globalState.limit;
}

function setupTabs() {
  const personalBtn = document.getElementById('tab-personal');
  const globalBtn = document.getElementById('tab-global');
  const personalSection = document.getElementById('personal-section');
  const globalSection = document.getElementById('global-section');

  personalBtn.addEventListener('click', () => {
    personalBtn.classList.add('active');
    globalBtn.classList.remove('active');
    personalSection.style.display = '';
    globalSection.style.display = 'none';
  });

  globalBtn.addEventListener('click', () => {
    globalBtn.classList.add('active');
    personalBtn.classList.remove('active');
    globalSection.style.display = '';
    personalSection.style.display = 'none';
  });
}

function setupPaging() {
  document.getElementById('filter-apply').addEventListener('click', () => {
    historyState.offset = 0;
    loadHistory().catch(console.error);
  });
  document.getElementById('history-prev').addEventListener('click', () => {
    historyState.offset = Math.max(0, historyState.offset - historyState.limit);
    loadHistory().catch(console.error);
  });
  document.getElementById('history-next').addEventListener('click', () => {
    if (historyState.lastCount === historyState.limit) {
      historyState.offset += historyState.limit;
      loadHistory().catch(console.error);
    }
  });

  document.getElementById('global-apply').addEventListener('click', () => {
    globalState.offset = 0;
    loadGlobal().catch(console.error);
  });
  document.getElementById('global-prev').addEventListener('click', () => {
    globalState.offset = Math.max(0, globalState.offset - globalState.limit);
    loadGlobal().catch(console.error);
  });
  document.getElementById('global-next').addEventListener('click', () => {
    if (globalState.lastCount === globalState.limit) {
      globalState.offset += globalState.limit;
      loadGlobal().catch(console.error);
    }
  });
}

function initStats() {
  if (!sessionStorage.getItem('token')) {
    location.href = '/login.html';
    return;
  }
  setupTabs();
  setupPaging();
  loadMyStats().catch(console.error);
  loadHistory().catch(console.error);
  loadGlobal().catch(console.error);
}

document.addEventListener('DOMContentLoaded', initStats);
