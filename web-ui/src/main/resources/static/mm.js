const MM_API = 'http://localhost:8091/api/game/pvp/mm';
let ticketId = null, countdown = 30, timer = null;

if (typeof window.authFetch !== 'function') {
  window.authFetch = async (url, init = {}) => {
    const token = sessionStorage.getItem('token');
    const headers = init.headers ? {...init.headers} : {};
    if (token) headers['Authorization'] = 'Bearer ' + token;
    if (!headers['Content-Type'] && init.body && typeof init.body === 'string') {
      headers['Content-Type'] = 'application/json';
    }
    return fetch(url, {...init, headers});
  };
}

function setMsg(t, ok=false){
  const el = document.getElementById('mm-msg');
  if (!el) return;
  el.textContent = t || '';
  el.classList.toggle('status-ok', ok);
  el.classList.toggle('status-bad', !ok);
}
function showActions(show){
  document.getElementById('mm-actions').style.display = show ? '' : 'none';
}
async function joinOnce(){
  setMsg('');
  const resp = await authFetch(`${MM_API}/join`, { method:'POST' });
  const j = await resp.json();
  if (!resp.ok) {
    const text = await resp.text().catch(()=> '');
    console.error('MM join failed', resp.status, text);
    throw new Error(text || resp.statusText);
  }
  if (!resp.ok) throw new Error(j.message || resp.status);
  if (j.matched) {
    // We are paired — jump straight into PvP
    const url = `/pvp.html?gameId=${j.gameId}`;
    window.location.href = url;
    return true;
  }
  ticketId = j.ticketId;
  countdown = j.timeoutSeconds || 30;
  document.getElementById('mm-secs').textContent = countdown;
  return false;
}
async function pollStatus(){
  if (!ticketId) return;
  const resp = await authFetch(`${MM_API}/status/${ticketId}`);
  const j = await resp.json();
  if (j.matched) {
    window.location.href = `/pvp.html?gameId=${j.gameId}`;
    return true;
  }
  return false;
}
function tick(){
  countdown -= 1;
  if (countdown >= 0) document.getElementById('mm-secs').textContent = countdown;
  if (countdown < 0) {
    timeoutUI();
    return;
  }
  pollStatus().catch(()=>{}); // best-effort
}
function timeoutUI(){
  clearInterval(timer); timer = null;
  setMsg('No global player available right now.', false);
  showActions(true);
}
async function tryAgain(){
  setMsg(''); showActions(false);
  ticketId = null; countdown = 30;
  const ok = await joinOnce();
  if (!ok) {
    timer = setInterval(tick, 1000);
  }
}
function backToDashboard(){
  // Cancel if a ticket exists
  if (ticketId) { authFetch(`${MM_API}/cancel/${ticketId}`, { method:'POST' }).catch(()=>{}); }
  window.location.href = '/dashboard.html';
}

async function setupMatchmakingPage(){
  if (!sessionStorage.getItem('token')) { window.location.href = '/login.html'; return; }
  applyNavAuthState?.();

  document.getElementById('mm-try').addEventListener('click', tryAgain);
  document.getElementById('mm-back').addEventListener('click', backToDashboard);

  try {
    const ok = await joinOnce();
    if (!ok) timer = setInterval(tick, 1000);
  } catch (e) {
    setMsg('Unable to start matchmaking. Please try again.', false);
    showActions(true);
  }
}