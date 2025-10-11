// Reuse GAME_API, authFetch, renderBoard, startTimer, stopTimer, updateTop, disableBoard, etc. from game.js

const WS_URL = 'http://localhost:8091/ws';
let stomp = null;

function showMsg(t){ const el = document.getElementById('msg'); if (el) el.textContent = t || ''; }
function clearMsg(){ showMsg(''); }

function connectWs(gameId){
  const sock = new SockJS(WS_URL);
  stomp = Stomp.over(sock);
  stomp.debug = null; // quiet
  stomp.connect({}, () => {
    stomp.subscribe(`/topic/game.${gameId}`, (frame) => {
      const evt = JSON.parse(frame.body);
      if (!window.current || window.current.id !== evt.gameId) return;
      // Update state from event
      window.current.board = evt.board;
      window.current.turn = evt.turn;
      window.current.status = evt.status;
      window.current.deadlineAt = evt.deadlineAt;
      renderBoard(window.current.board);
      updateTop();
      startTimer();
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
    window.current = {
      id: j.gameId, mode: j.mode, youAre: j.youAre,
      board: j.board, turn: j.turn, status: j.status, deadlineAt: j.deadlineAt
    };
    renderBoard(current.board); updateTop(); startTimer();

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
    window.current = {
      id: j.gameId, mode: j.mode, youAre: j.youAre,
      board: j.board, turn: j.turn, status: j.status, deadlineAt: j.deadlineAt
    };
    renderBoard(current.board); updateTop(); startTimer();
    connectWs(current.id);
  } catch (e) { showMsg(e.message || 'Failed to join lobby'); }
}

async function playCellPvp(ev){
  if (!window.current || current.status !== 'IN_PROGRESS') return;
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
  } catch (e) { showMsg(e.message || 'Move failed'); }
}

async function refreshState(){
  // Optional: if you exposed GET /api/game/{id} you can reuse it here
  if (!current) return;
  try {
    const resp = await authFetch(`${GAME_API}/${current.id}`);
    const j = await resp.json();
    if (resp.ok) {
      current.board = j.board; current.turn = j.turn; current.status = j.status; current.deadlineAt = j.deadlineAt;
      renderBoard(current.board); updateTop(); startTimer();
    }
  } catch {}
}

function setupPvpPage(){
  if (!sessionStorage.getItem('token')) { location.href = '/login.html'; return; }
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
}
