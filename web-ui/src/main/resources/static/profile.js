const PROFILE_API = window.AUTH_API || localStorage.getItem('apiBase') || 'http://localhost:8081/api/auth';

let originalProfile = null;
let editMode = false;

function setMsg(id, text, ok=false){
  const el = document.getElementById(id);
  if (!el) return;
  el.textContent = text || '';
  el.className = ok ? 'success' : 'error';
}

function validPassword(p){
  return /[A-Z]/.test(p) && /[a-z]/.test(p) && /[^A-Za-z0-9]/.test(p) && p.length >= 8;
}

function setEditable(enabled) {
  ['firstName','lastName','birthDate','nationality','email','mobile','username','password']
    .forEach(id => document.getElementById(id).disabled = !enabled);
  editMode = enabled;
}

function updateButtons(dirty) {
  document.getElementById('save-btn').disabled = !dirty;
  document.getElementById('cancel-btn').disabled = !dirty;
}

function currentPayload() {
  return {
    firstName: document.getElementById('firstName').value.trim(),
    lastName: document.getElementById('lastName').value.trim(),
    birthDate: document.getElementById('birthDate').value,
    nationality: document.getElementById('nationality').value.trim(),
    email: document.getElementById('email').value.trim(),
    mobile: document.getElementById('mobile').value.trim(),
    username: document.getElementById('username').value.trim(),
    password: document.getElementById('password').value
  };
}

function isDirty(payload) {
  if (!originalProfile) return false;
  if (payload.password) return true;
  return (
    payload.firstName !== (originalProfile.firstName || '') ||
    payload.lastName !== (originalProfile.lastName || '') ||
    payload.birthDate !== (originalProfile.birthDate || '') ||
    payload.nationality !== (originalProfile.nationality || '') ||
    payload.email !== (originalProfile.email || '') ||
    payload.mobile !== (originalProfile.mobile || '') ||
    payload.username !== (originalProfile.username || '')
  );
}

function fillProfile(data) {
  originalProfile = data;
  document.getElementById('firstName').value = data.firstName || '';
  document.getElementById('lastName').value = data.lastName || '';
  document.getElementById('birthDate').value = data.birthDate || '';
  document.getElementById('nationality').value = data.nationality || '';
  document.getElementById('email').value = data.email || '';
  document.getElementById('mobile').value = data.mobile || '';
  document.getElementById('username').value = data.username || '';
  document.getElementById('createdAt').value = data.createdAt ? new Date(data.createdAt).toLocaleString() : '';
  document.getElementById('updatedAt').value = data.updatedAt ? new Date(data.updatedAt).toLocaleString() : '';
  document.getElementById('password').value = '';
  setEditable(false);
  updateButtons(false);
}

async function loadProfile() {
  const resp = await authFetch(`${PROFILE_API}/me`);
  if (!resp.ok) throw new Error('Failed to load profile');
  const data = await resp.json();
  fillProfile(data);
}

function buildChanges(payload) {
  const changes = {};
  if (!originalProfile) return changes;
  if (payload.firstName !== (originalProfile.firstName || '')) changes.firstName = payload.firstName;
  if (payload.lastName !== (originalProfile.lastName || '')) changes.lastName = payload.lastName;
  if (payload.birthDate !== (originalProfile.birthDate || '')) changes.birthDate = payload.birthDate;
  if (payload.nationality !== (originalProfile.nationality || '')) changes.nationality = payload.nationality;
  if (payload.email !== (originalProfile.email || '')) changes.email = payload.email;
  if (payload.mobile !== (originalProfile.mobile || '')) changes.mobile = payload.mobile;
  if (payload.username !== (originalProfile.username || '')) changes.username = payload.username;
  if (payload.password) changes.password = payload.password;
  return changes;
}

function validatePayload(payload) {
  const requiredFields = ['firstName','lastName','birthDate','nationality','email','mobile','username'];
  for (const key of requiredFields) {
    if (!payload[key]) {
      setMsg('profile-msg', 'Please fill all required fields.');
      return false;
    }
  }
  if (payload.password && !validPassword(payload.password)) {
    setMsg('profile-msg', 'Password policy not met.');
    return false;
  }
  return true;
}

function setupProfileForm() {
  document.getElementById('edit-btn').addEventListener('click', () => {
    setMsg('profile-msg', '');
    setMsg('profile-ok', '');
    setEditable(true);
    updateButtons(false);
  });

  document.getElementById('cancel-btn').addEventListener('click', () => {
    if (originalProfile) {
      fillProfile(originalProfile);
    }
    setMsg('profile-msg', '');
    setMsg('profile-ok', '');
  });

  document.getElementById('profileForm').addEventListener('input', () => {
    if (!editMode) return;
    const payload = currentPayload();
    updateButtons(isDirty(payload));
  });

  document.getElementById('profileForm').addEventListener('submit', async (e) => {
    e.preventDefault();
    if (!editMode) return;
    setMsg('profile-msg', '');
    setMsg('profile-ok', '');

    const payload = currentPayload();
    if (!validatePayload(payload)) return;

    const changes = buildChanges(payload);
    if (Object.keys(changes).length === 0) {
      setMsg('profile-msg', 'No changes to save.');
      return;
    }

    try {
      const resp = await authFetch(`${PROFILE_API}/me`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(changes)
      });
      const data = await resp.json();
      if (!resp.ok) {
        setMsg('profile-msg', data.message || 'Failed to update profile.');
        return;
      }
      fillProfile(data);
      setMsg('profile-ok', 'Profile updated', true);
    } catch (err) {
      setMsg('profile-msg', 'Network error.');
    }
  });
}

function initProfile() {
  if (!sessionStorage.getItem('token')) {
    location.href = '/login.html';
    return;
  }
  setupProfileForm();
  loadProfile().catch(() => setMsg('profile-msg', 'Unable to load profile.'));
}

document.addEventListener('DOMContentLoaded', initProfile);
