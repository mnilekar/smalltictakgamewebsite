// web-ui/src/main/resources/static/app.js

// Configure API base once. You can override in the browser console via:
// localStorage.setItem('apiBase', 'http://localhost:8081/api/auth'); location.reload();
const API_BASE = localStorage.getItem('apiBase') || 'http://localhost:8081/api/auth';
console.log('API_BASE =', API_BASE);

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
function setupRegisterPage(){
  const first = $('firstName'), last = $('lastName'), uname = $('username');
  const hints = $('unameHints');

  async function updateSuggestions(){
    const f = (first.value || '').trim();
    const l = (last.value || '').trim();
    if (!f || !l) { hints.textContent = ''; return; }
    try {
      const resp = await fetch(`${API_BASE.replace('/api/auth','')}/api/auth/suggest?first=${encodeURIComponent(f)}&last=${encodeURIComponent(l)}`);
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
      const resp = await fetch(`${API_BASE}/register`, {
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
function setupLoginPage(){
  $('loginForm').addEventListener('submit', async (e)=>{
    e.preventDefault();
    setMsg('login-msg','');

    const payload = {
      username: ($('login-username').value || '').trim(),
      password: $('login-password').value
    };

    try {
      const resp = await fetch(`${API_BASE}/login`, {
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
function setupDashboard(){
  const token = sessionStorage.getItem('token');
  if (!token) { location.href='/login.html'; return; }

  fetch(`${API_BASE}/me`, { headers: { 'Authorization': `Bearer ${token}` }})
    .then(r => r.ok ? r.json() : Promise.reject(r))
    .then(j => { $('who').textContent = j.username || sessionStorage.getItem('username') || 'player'; })
    .catch(() => { logout(); });
}

function goStats(){ alert('Stats page — coming next'); }
function goProfile(){ alert('Profile page — coming next'); }
function logout(){ sessionStorage.clear(); location.href='/login.html'; }
