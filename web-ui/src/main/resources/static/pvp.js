// Reuse GAME_API, authFetch, renderBoard, startTimer, stopTimer, updateTop, disableBoard, etc. from game.js

let WS_URL;
let AUTH_API_BASE;

async function ensurePvpConfig() {
  if (typeof window.ensureGameConfig === 'function') {
    await window.ensureGameConfig();
  } else if (typeof window.loadConfig === 'function') {
    await window.loadConfig();
  }
  const authBase = window.AUTH_BASE || 'http://localhost:8081';
  const wsBase = window.WS_BASE || 'ws://localhost:8091/ws';
  AUTH_API_BASE = `${authBase}/api/auth`;
  WS_URL = wsBase;
}
let stomp = null;
let autoRefreshTimer = null;
let startCountdownTimer = null;
let countdownRemaining = 0;
let countdownActive = false;
let startAnnouncementDone = false;
const playerNames = { X: null, O: null };
const playerIds = { X: null, O: null };
window.pvpNameForMark = (mark) => nameForMark(mark);

function showMsg(t){ const el = document.getElementById('msg'); if (el) el.textContent = t || ''; }
function clearMsg(){ showMsg(''); }

function displayName(profile) {
  return profile?.firstName || profile?.username || 'Player';
}

async function fetchProfile(userId) {
  if (!userId) return null;
  try {
    const resp = await fetch(`${AUTH_API_BASE}/profile/${userId}`);
    if (!resp.ok) return null;
    return await resp.json();
  } catch {
    return null;
  }
}

async function syncPlayerNames() {
  if (!current) return;
  if (current.playerXId && current.playerXId !== playerIds.X) {
    playerIds.X = current.playerXId;
    const profile = await fetchProfile(current.playerXId);
    playerNames.X = displayName(profile);
  }
  if (current.playerOId && current.playerOId !== playerIds.O) {
    playerIds.O = current.playerOId;
    const profile = await fetchProfile(current.playerOId);
    playerNames.O = displayName(profile);
  }
}

function nameForMark(mark) {
  return playerNames[mark] || mark;
}

function setCountdownModalVisible(visible) {
  const modal = document.getElementById('countdown-modal');
  if (!modal) return;
  modal.classList.toggle('show', visible);
  modal.setAttribute('aria-hidden', visible ? 'false' : 'true');
}

function updateCountdownModal() {
  const title = document.getElementById('countdown-title');
  const player = document.getElementById('countdown-player');
  const number = document.getElementById('countdown-number');
  if (title) title.textContent = 'Game starts in';
  if (player) player.textContent = `Starting player: ${nameForMark(current.turn)}`;
  if (number) number.textContent = String(countdownRemaining);
}

function updateStatusFromState() {
  if (!current) return;
  if (current.status === 'X_WON' || current.status === 'O_WON') {
    const winnerMark = current.status === 'X_WON' ? 'X' : 'O';
    setStatus(`${nameForMark(winnerMark)} Won !`, true);
    return;
  }
  if (current.status === 'TIE') {
    setStatus('Its a tie , Well played !', true);
    return;
  }
  setStatus('-', true);
}

function resetStartCountdown() {
  if (startCountdownTimer) clearInterval(startCountdownTimer);
  startCountdownTimer = null;
  countdownRemaining = 0;
  countdownActive = false;
  window.pvpCountdownActive = false;
  startAnnouncementDone = false;
  setCountdownModalVisible(false);
}

function startCountdown() {
  if (countdownActive) return;
  countdownRemaining = 3;
  countdownActive = true;
  window.pvpCountdownActive = true;
  disableBoard(true);
  updateCountdownModal();
  setCountdownModalVisible(true);
  updateStatusFromState();
  startCountdownTimer = setInterval(() => {
    countdownRemaining -= 1;
    if (countdownRemaining > 0) {
      updateCountdownModal();
      updateStatusFromState();
      return;
    }
    clearInterval(startCountdownTimer);
    startCountdownTimer = null;
    countdownActive = false;
    window.pvpCountdownActive = false;
    startAnnouncementDone = true;
    setCountdownModalVisible(false);
    updateStatusFromState();
  }, 1000);
}

function maybeStartCountdown() {
  if (!current) return;
  if (startAnnouncementDone || countdownActive) return;
  if (current.status === 'IN_PROGRESS' && current.playerXId && current.playerOId) {
    startCountdown();
  }
}

function connectWs(gameId){
  const sock = new SockJS(WS_URL);
  stomp = Stomp.over(sock);
  stomp.debug = null; // quiet
  stomp.connect({}, () => {
    stomp.subscribe(`/topic/game.${gameId}`, async (frame) => {
      const evt = JSON.parse(frame.body);
      if (!current || current.id !== evt.gameId) return;
      // Update state from event
      current.board = evt.board;
      current.turn = evt.turn;
      current.status = evt.status;
      current.deadlineAt = evt.deadlineAt;
      renderBoard(current.board);
      updateTop();
      startTimer();
      await syncPlayerNames();
      maybeStartCountdown();
      updateStatusFromState();
    });
  }, (err) => {
    console.error('WS error', err);
  });
}

async function createLobby(){
  clearMsg();
  try {
    const resp = await authFetch(`${GAME_API}/pvp/start`, { method: 'POST' });
    const j = await resp.json();
    if (!resp.ok) throw new Error(j.message || resp.status);
    current = {
      id: j.gameId, mode: j.mode, youAre: j.youAre,
      board: j.board, turn: j.turn, status: j.status, deadlineAt: j.deadlineAt,
      playerXId: j.playerXId, playerOId: j.playerOId
    };
    renderBoard(current.board);
    updateTop();
    startTimer();
    resetStartCountdown();
    await syncPlayerNames();
    maybeStartCountdown();
    updateStatusFromState();

    document.getElementById('g-id').textContent = current.id;
    document.getElementById('g-mode').textContent = current.mode;
    document.getElementById('g-you').textContent = current.youAre;
    document.getElementById('g-turn').textContent = `Turn: ${current.turn}`;

    // share link
    const shareUrl = `${location.origin}/pvp.html?gameId=${current.id}`;
    document.getElementById('share').innerHTML = `Share this link: <strong>${shareUrl}</strong>`;
    connectWs(current.id);
  } catch (e) { showMsg(e.message || 'Failed to create lobby'); }
}

async function joinLobby(gameId){
  clearMsg();
  try {
    const resp = await authFetch(`${GAME_API}/pvp/${gameId}/join`, { method: 'POST' });
    const j = await resp.json();
    if (!resp.ok) throw new Error(j.message || resp.status);
    current = {
      id: j.gameId, mode: j.mode, youAre: j.youAre,
      board: j.board, turn: j.turn, status: j.status, deadlineAt: j.deadlineAt,
      playerXId: j.playerXId, playerOId: j.playerOId
    };
    renderBoard(current.board);
    updateTop();
    startTimer();
    resetStartCountdown();
    await syncPlayerNames();
    maybeStartCountdown();
    updateStatusFromState();
    connectWs(current.id);
  } catch (e) { showMsg(e.message || 'Failed to join lobby'); }
}

async function playCellPvp(ev){
  if (!current || current.status !== 'IN_PROGRESS') return;
  if (window.pvpCountdownActive) return;
  const idx = Number(ev.currentTarget.getAttribute('data-i'));
  const row = Math.floor(idx/3), col = idx%3;
  try {
    const resp = await authFetch(`${GAME_API}/pvp/${current.id}/move`, {
      method: 'POST',
      headers: {'Content-Type':'application/json'},
      body: JSON.stringify({row, col})
    });
    const j = await resp.json().catch(()=> ({}));
    if (!resp.ok) throw new Error(j.message || resp.status);
    // After our move, the server will broadcast. We also apply the response immediately:
    current.board = j.board; current.turn = j.turn; current.status = j.status; current.deadlineAt = j.deadlineAt;
    renderBoard(current.board); updateTop(); startTimer();
    updateStatusFromState();
  } catch (e) { showMsg(e.message || 'Move failed'); }
}

async function refreshState(){
  // Optional: if you exposed GET /api/game/{id} you can reuse it here
  if (!current) return;
  try {
    const resp = await authFetch(`${GAME_API}/${current.id}`);
    const j = await resp.json();
    if (resp.ok) {
      current.board = j.board;
      current.turn = j.turn;
      current.status = j.status;
      current.deadlineAt = j.deadlineAt;
      current.playerXId = j.playerXId;
      current.playerOId = j.playerOId;
      renderBoard(current.board);
      updateTop();
      startTimer();
      await syncPlayerNames();
      maybeStartCountdown();
      updateStatusFromState();
    }
  } catch {}
}

async function setupPvpPage(){
  if (!sessionStorage.getItem('token')) { location.href = '/login.html'; return; }
  await ensurePvpConfig();
  applyNavAuthState?.();

  document.getElementById('btn-create').addEventListener('click', createLobby);
  document.getElementById('btn-refresh').addEventListener('click', refreshState);
  document.querySelectorAll('.cell').forEach(c => c.addEventListener('click', playCellPvp));

  // Auto-join flow
  const p = new URLSearchParams(location.search);
  const gid = p.get('gameId');
  if (gid) {
    joinLobby(Number(gid));
  }

  if (!autoRefreshTimer) {
    autoRefreshTimer = setInterval(refreshState, 2000);
  }
  window.addEventListener('beforeunload', () => {
    if (autoRefreshTimer) clearInterval(autoRefreshTimer);
    if (startCountdownTimer) clearInterval(startCountdownTimer);
    if (stomp) stomp.disconnect();
  });
}
