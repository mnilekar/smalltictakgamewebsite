// Game UI wiring for game-service
const GAME_API = localStorage.getItem('gameApiBase') || 'http://localhost:8091/api/game';
console.log('GAME_API =', GAME_API);

let current = null;   // { id, mode, youAre, board, turn, status, deadlineAt }
let timerId = null;

function authFetch(url, opts = {}) {
  const token = sessionStorage.getItem('token');
  if (!token) { location.href = '/login.html'; return Promise.reject('no token'); }
  const headers = Object.assign({}, opts.headers || {}, { 'Authorization': `Bearer ${token}` });
  return fetch(url, Object.assign({}, opts, { headers }));
}

function secsLeft(deadlineAt) {
  if (!deadlineAt) return 0;
  const ms = new Date(deadlineAt).getTime() - Date.now();
  return Math.max(0, Math.floor(ms / 1000));
}

function renderBoard(board) {
  const cells = document.querySelectorAll('.cell');
  for (let i=0;i<9;i++) cells[i].textContent = board.charAt(i) === '.' ? '' : board.charAt(i);
}

function setStatus(text, good=false) {
  const el = document.getElementById('g-status');
  el.textContent = text || '';
  el.className = good ? 'value status-ok' : 'value status-bad';
}

function updateTop() {
  document.getElementById('g-id').textContent   = current ? current.id : '—';
  document.getElementById('g-mode').textContent = current ? current.mode : '—';
  document.getElementById('g-you').textContent  = current ? current.youAre : '—';
  document.getElementById('g-turn').textContent = current ? (`Turn: ${current.turn}`) : '—';
}

function disableBoard(disabled) {
  document.querySelectorAll('.cell').forEach(c=>{
    c.classList.toggle('disabled', !!disabled);
  });
}

function startTimer() {
  stopTimer();
  tick(); // immediate paint
  timerId = setInterval(tick, 250);
  function tick(){
    if (!current) return;
    const s = secsLeft(current.deadlineAt);
    const mm = String(Math.floor(s/60)).padStart(2,'0');
    const ss = String(s%60).padStart(2,'0');
    document.getElementById('g-timer').textContent = `${mm}:${ss}`;

    // disable clicks if not your turn (vs-system) or if finished
    const finished = current.status !== 'IN_PROGRESS';
    let canClick = !finished;
    if (current.mode === 'VS_SYSTEM') {
      // Only allow on your turn
      const you = current.youAre; // 'X' or 'O'
      canClick = canClick && (current.turn === you);
    }
    disableBoard(!canClick);

    if (finished || s <= 0) {
      if (finished) {
        let msg = '';
        if (current.status === 'X_WON' || current.status === 'O_WON') {
          msg = `${current.status === 'X_WON' ? 'X' : 'O'} won!`;
        } else if (current.status === 'TIE') {
          msg = 'It’s a tie.';
        } else if (current.status === 'FORFEIT') {
          msg = 'Forfeit due to timeout.';
        }
        setStatus(msg || 'Finished', true);
        stopTimer();
      }
    }
  }
}
function stopTimer(){ if (timerId) { clearInterval(timerId); timerId = null; } }

async function startSelf(opts = {}) {
  clearMsg();
  const first = (opts.first || document.getElementById('self-first').value || 'X').toUpperCase();

  try {
    const resp = await authFetch(`${GAME_API}/self/start`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ firstPlayer: first })
    });
    const j = await resp.json();
    if (!resp.ok) throw new Error(j.message || resp.status);

    current = {
      id: j.gameId, mode: j.mode, youAre: j.youAre,
      board: j.board, turn: j.turn, status: j.status, deadlineAt: j.deadlineAt
    };
    renderBoard(current.board);
    updateTop();
    setStatus('Game started.', true);
    startTimer();
  } catch (e) { showMsg(e.message || 'Start failed'); }
}

async function startVs(opts = {}) {
  clearMsg();
  const you = (opts.you || document.getElementById('vs-you').value || 'X').toUpperCase();
  const difficulty = (opts.difficulty || document.getElementById('vs-diff').value || 'EASY').toUpperCase();

  try {
    const resp = await authFetch(`${GAME_API}/vs-system/start`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ youPlayAs: you, difficulty })
    });
    const j = await resp.json();
    if (!resp.ok) throw new Error(j.message || resp.status);

    current = {
      id: j.gameId, mode: j.mode, youAre: j.youAre,
      board: j.board, turn: j.turn, status: j.status, deadlineAt: j.deadlineAt
    };
    renderBoard(current.board);
    updateTop();
    setStatus('Game started.', true);
    startTimer();
  } catch (e) { showMsg(e.message || 'Start failed'); }
}

async function playCell(ev) {
  if (!current || current.status !== 'IN_PROGRESS') return;
  const idx = Number(ev.currentTarget.getAttribute('data-i'));
  const row = Math.floor(idx/3), col = idx%3;

  // Basic client-side guard: only allow vs-system when it's your turn
  if (current.mode === 'VS_SYSTEM' && current.turn !== current.youAre) return;

  try {
    const resp = await authFetch(`${GAME_API}/${current.id}/move`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ row, col })
    });
    const text = await resp.text(); // read once
    let j = {};
    try { j = text ? JSON.parse(text) : {}; } catch { /* keep empty */ }

    if (!resp.ok) {
      showMsg(j.message || text || `Error ${resp.status}`);
      if (resp.status === 409) { // e.g., deadline exceeded
        // Refresh state
        await refreshState();
      }
      return;
    }
    // Successful move; server may also include systemMove if VS_SYSTEM
    current.board = j.board;
    current.turn = j.turn;
    current.status = j.status;
    current.deadlineAt = j.deadlineAt;
    renderBoard(current.board);
    updateTop();
    setStatus(current.status === 'IN_PROGRESS' ? 'Your move (if it’s your turn).' : 'Finished.', current.status !== 'IN_PROGRESS');
    startTimer();
  } catch (e) {
    showMsg(e.message || 'Network error');
  }
}

async function refreshState() {
  if (!current) return;
  try {
    const resp = await authFetch(`${GAME_API}/${current.id}`);
    const j = await resp.json();
    if (!resp.ok) throw new Error(j.message || resp.status);
    current.board = j.board;
    current.turn = j.turn;
    current.status = j.status;
    current.deadlineAt = j.deadlineAt;
    renderBoard(current.board);
    updateTop();
    startTimer();
  } catch (e) {
    showMsg('Unable to refresh game.');
  }
}

function clearBoardOnly() {
  document.querySelectorAll('.cell').forEach(c=> c.textContent = '');
  document.getElementById('g-id').textContent = '—';
  document.getElementById('g-mode').textContent = '—';
  document.getElementById('g-you').textContent = '—';
  document.getElementById('g-turn').textContent = '—';
  document.getElementById('g-timer').textContent = '00:00';
  setStatus('No game yet.');
}

function quitGame() {
  stopTimer();
  current = null;
  clearBoardOnly();
  clearMsg();
}

function showMsg(t){ const el = document.getElementById('msg'); el.textContent = t || ''; }
function clearMsg(){ showMsg(''); }

function setupGamePage(){
  // require login
  if (!sessionStorage.getItem('token')) { location.href = '/login.html'; return; }

  document.getElementById('btn-self').addEventListener('click', startSelf);
  document.getElementById('btn-vs').addEventListener('click', startVs);
  document.getElementById('btn-reset').addEventListener('click', refreshState);
  document.getElementById('btn-quit').addEventListener('click', quitGame);
  document.querySelectorAll('.cell').forEach(c => c.addEventListener('click', playCell));

  clearBoardOnly();
    const q = new URLSearchParams(location.search);
    const mode = q.get('mode'); // self | vs-system | pvp
    if (mode) {
      const panel = document.getElementById('start-panel');
      if (panel) panel.style.display = 'none'; // hide chooser

      if (mode === 'self') {
        const first = q.get('first') || 'X';
        startSelf(first);
      } else if (mode === 'vs-system') {
        const you = q.get('you') || 'X';
        const diff = q.get('difficulty') || 'EASY';
        startVs(you, diff);
      } else if (mode === 'pvp') {
        showMsg('Global PvP coming in Task 9 🚀');
      }
    }
}
