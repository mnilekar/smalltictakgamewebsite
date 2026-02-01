// web-ui/src/main/resources/static/app.js

let AUTH_BASE;
let GAME_BASE;
let WS_BASE;
let AUTH_API;
let configPromise;

async function loadConfig() {
  if (configPromise) return configPromise;
  configPromise = (async () => {
    try {
      const res = await fetch('/config.json', { cache: 'no-store' });
      const cfg = await res.json();
      AUTH_BASE = cfg.authBase || 'http://localhost:8081';
      GAME_BASE = cfg.gameBase || 'http://localhost:8091';
      WS_BASE = cfg.wsBase || 'ws://localhost:8091/ws';
    } catch (e) {
      AUTH_BASE = 'http://localhost:8081';
      GAME_BASE = 'http://localhost:8091';
      WS_BASE = 'ws://localhost:8091/ws';
    }
    AUTH_API = `${AUTH_BASE}/api/auth`;
    window.AUTH_BASE = AUTH_BASE;
    window.GAME_BASE = GAME_BASE;
    window.WS_BASE = WS_BASE;
    window.AUTH_API = AUTH_API;
    console.log('AUTH_API =', AUTH_API);
  })();
  return configPromise;
}

window.loadConfig = loadConfig;

if (typeof window.authFetch !== 'function') {
  window.authFetch = async (url, opts = {}) => {
    const token = sessionStorage.getItem('token');
    if (!token) { location.href = '/login.html'; return Promise.reject('no token'); }
    const headers = Object.assign({}, opts.headers || {}, { 'Authorization': `Bearer ${token}` });
    return fetch(url, Object.assign({}, opts, { headers }));
  };
}

function $(id){ return document.getElementById(id); }
function setMsg(id, text, ok=false){
  const el = $(id);
  if (!el) return;
  el.textContent = text || '';
  el.className = ok ? 'success' : 'error';
}
function validPassword(p){
  return /[A-Z]/.test(p) && /[a-z]/.test(p) && /[^A-Za-z0-9]/.test(p) && p.length >= 8;
}

// ------- Register -------
async function setupRegisterPage(){
  await loadConfig();
  const first = $('firstName'), last = $('lastName'), uname = $('username');
  const hints = $('unameHints');

  async function updateSuggestions(){
    const f = (first.value || '').trim();
    const l = (last.value || '').trim();
    if (!f || !l) { hints.textContent = ''; return; }
    try {
      const resp = await fetch(`${AUTH_API.replace('/api/auth','')}/api/auth/suggest?first=${encodeURIComponent(f)}&last=${encodeURIComponent(l)}`);
      if (!resp.ok) return;
      const data = await resp.json();
      hints.textContent = (data.suggestions || []).slice(0,3).join(' • ');
    } catch { hints.textContent = ''; }
  }

  first.addEventListener('input', updateSuggestions);
  last.addEventListener('input', updateSuggestions);

  $('regForm').addEventListener('submit', async (e)=>{
    e.preventDefault();
    setMsg('reg-msg',''); setMsg('reg-ok','');

    const payload = {
      firstName: (first.value || '').trim(),
      lastName: (last.value || '').trim(),
      birthDate: $('birthDate').value,
      nationality: ($('nationality').value || '').trim(),
      email: ($('email').value || '').trim(),
      mobile: ($('mobile').value || '').trim(),
      username: (uname.value || '').trim(),
      password: $('password').value
    };

    if (!validPassword(payload.password)) {
      setMsg('reg-msg','Password policy not met'); return;
    }

    try {
      const resp = await fetch(`${AUTH_API}/register`, {
        method:'POST',
        headers:{'Content-Type':'application/json'},
        body: JSON.stringify(payload)
      });

      const text = await resp.text(); // read once for diagnostics
      if (resp.status === 201 || resp.status === 200) {
        setMsg('reg-ok','Registration successful. Redirecting to login...', true);
        setTimeout(()=>location.href='/login.html', 800);
      } else if (resp.status === 409) {
        setMsg('reg-msg', text || 'Username or email already in use');
      } else {
        setMsg('reg-msg', text || `Error: ${resp.status}`);
      }
    } catch (err) {
      setMsg('reg-msg','Network error');
    }
  });
}

// ------- Login -------
async function setupLoginPage(){
  await loadConfig();
  $('loginForm').addEventListener('submit', async (e)=>{
    e.preventDefault();
    setMsg('login-msg','');

    const payload = {
      username: ($('login-username').value || '').trim(),
      password: $('login-password').value
    };

    try {
      const resp = await fetch(`${AUTH_API}/login`, {
        method:'POST',
        headers:{'Content-Type':'application/json'},
        body: JSON.stringify(payload)
      });

      if (!resp.ok) {
        const text = await resp.text();
        setMsg('login-msg', text || `Login failed: ${resp.status}`);
        return;
      }

      const j = await resp.json();
      sessionStorage.setItem('token', j.token);
      sessionStorage.setItem('username', j.username || payload.username);
      location.href = '/dashboard.html';
    } catch {
      setMsg('login-msg','Network error');
    }
  });
}

// ------- Dashboard -------
async function setupDashboard(){
  await loadConfig();
  const token = sessionStorage.getItem('token');
  if (!token) { location.href='/login.html'; return; }

  fetch(`${AUTH_API}/me`, { headers: { 'Authorization': `Bearer ${token}` }})
    .then(r => r.ok ? r.json() : Promise.reject(r))
    .then(j => { $('who').textContent = j.username || sessionStorage.getItem('username') || 'player'; })
    .catch(() => { logout(); });
}

function goStats(){ location.href = '/stats.html'; }
function goProfile(){ location.href = '/profile.html'; }
function logout(){ sessionStorage.clear(); location.href='/login.html'; }


function isLoggedIn() { return !!sessionStorage.getItem('token'); }

function applyNavAuthState() {
  const logged = isLoggedIn();
  const $ = (id) => document.getElementById(id);

  if ($('nav-home')) {
    $('nav-home').href = logged ? '/dashboard.html' : '/index.html';
    $('nav-home').style.display = logged ? 'none' : '';
  }
  if ($('nav-register')) $('nav-register').style.display = logged ? 'none' : '';
  if ($('nav-login')) $('nav-login').style.display = logged ? 'none' : '';
  if ($('nav-dashboard')) $('nav-dashboard').style.display = logged ? '' : 'none';
  if ($('nav-stats')) $('nav-stats').style.display = logged ? '' : 'none';
  if ($('nav-profile')) $('nav-profile').style.display = logged ? '' : 'none';
  if ($('nav-logout')) {
    $('nav-logout').style.display = logged ? '' : 'none';
    $('nav-logout').onclick = (e) => {
      e.preventDefault();
      sessionStorage.removeItem('token');
      window.location.href = '/index.html';
    };
  }
}

function setActiveNav() {
  const path = window.location.pathname;
  const map = [
    { match: '/index.html', id: 'nav-home' },
    { match: '/login.html', id: 'nav-login' },
    { match: '/register.html', id: 'nav-register' },
    { match: '/dashboard.html', id: 'nav-dashboard' },
    { match: '/stats.html', id: 'nav-stats' },
    { match: '/profile.html', id: 'nav-profile' }
  ];
  map.forEach(({ match, id }) => {
    const el = document.getElementById(id);
    if (!el) return;
    el.classList.toggle('active', path.endsWith(match));
  });
}

document.addEventListener('DOMContentLoaded', () => {
  applyNavAuthState();
  setActiveNav();
});
