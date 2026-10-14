const tg = (window.Telegram && window.Telegram.WebApp) ? window.Telegram.WebApp : {};
const isTma = Boolean(tg && tg.initData && tg.initData.length > 0);
try {
  if (tg.ready) tg.ready();
  if (tg.expand) tg.expand();
} catch (e) {}

// Theme initialization (Telegram theme or saved preference)
(function initTheme() {
  let savedTheme = null;
  try { savedTheme = localStorage.getItem('tc_theme'); } catch (e) {}
  if (savedTheme === 'dark' || (!savedTheme && tg.colorScheme === 'dark')) {
    document.documentElement.setAttribute('data-theme', 'dark');
  }
})();

window.toggleTheme = function () {
  const isDark = document.documentElement.getAttribute('data-theme') === 'dark';
  const next = isDark ? 'light' : 'dark';
  if (next === 'dark') document.documentElement.setAttribute('data-theme', 'dark');
  else document.documentElement.removeAttribute('data-theme');
  try { localStorage.setItem('tc_theme', next); } catch (e) {}
  haptic('impact', 'light');
};

const API = location.origin;

function haptic(type, style) {
  try {
    if (!tg.HapticFeedback) return;
    if (type === 'impact') tg.HapticFeedback.impactOccurred(style);
    if (type === 'notification') tg.HapticFeedback.notificationOccurred(style);
  } catch (e) {}
}

function tgAlert(msg) {
  try {
    if (isTma && tg.showAlert) {
      tg.showAlert(msg);
      return;
    }
  } catch (e) {}
  showErrorNotification(msg);
}

function tgConfirm(msg, callback) {
  try {
    if (isTma && tg.showConfirm) {
      tg.showConfirm(msg, callback);
      return;
    }
  } catch (e) {}
  const ok = window.confirm(msg);
  callback(ok);
}

function storageGet(key, callback) {
  try {
    if (isTma && tg.CloudStorage && tg.CloudStorage.getItem) {
      tg.CloudStorage.getItem(key, (err, val) => {
        if (!err && val) return callback(null, val);
        let localVal = null;
        try { localVal = localStorage.getItem('tc_' + key); } catch (e) {}
        callback(null, localVal);
      });
      return;
    }
  } catch (e) {}
  let localVal = null;
  try { localVal = localStorage.getItem('tc_' + key); } catch (e) {}
  callback(null, localVal);
}

function storageSet(key, val) {
  try { localStorage.setItem('tc_' + key, val); } catch (e) {}
  try {
    if (isTma && tg.CloudStorage && tg.CloudStorage.setItem) {
      tg.CloudStorage.setItem(key, val);
    }
  } catch (e) {}
}

// ===== Global State =====
let token = null;
let refreshToken = null;
try {
  token = localStorage.getItem('tc_token') || null;
  refreshToken = localStorage.getItem('tc_refresh_token') || null;
} catch (e) {}

let authMode = 'login'; // 'login' | 'register'
let currentUser = null;
let currentUserId = null;
let currentWsId = null;
let allWorkspaces = [];
let colsData = [];
let tasksData = [];
let workspaceMembers = [];
let workspaceLabels = [];
let workspaceDirections = [];
let workspaceSprints = [];
let currentTaskId = null;
let currentFilter = 'all';
let searchQuery = '';
let selectedSprintFilter = '';
let selectedDirectionFilter = '';
let selectedLabelFilter = '';
let currentTab = 'board';
let sortPrefs = {};
let botUsername = '';

// Chat state
let chatChannel = 'public'; // 'public' | 'direct'
let activeChatPartner = null; // { id, name, fullName }
let chatReplyToId = null;
let chatUsers = [];

const urlParams = new URLSearchParams(window.location.search);
let initialTaskId = urlParams.get('task');
if (!initialTaskId && tg.initDataUnsafe?.start_param?.startsWith('task_')) {
  initialTaskId = tg.initDataUnsafe.start_param.substring(5);
}

function showToast(msg) {
  let t = document.getElementById('toast');
  if (!t) {
    t = document.createElement('div');
    t.id = 'toast';
    t.style = 'position:fixed;bottom:24px;left:50%;transform:translateX(-50%);background:var(--text-color);color:var(--bg-color);padding:10px 18px;border-radius:999px;font-size:13.5px;font-weight:600;box-shadow:0 8px 24px rgba(0,0,0,0.22);z-index:99999;transition:opacity 0.25s;opacity:0;pointer-events:none;';
    document.body.appendChild(t);
  }
  t.textContent = msg;
  t.style.opacity = '1';
  setTimeout(() => { t.style.opacity = '0'; }, 2400);
}

function showErrorNotification(msg) {
  haptic('notification', 'error');
  let t = document.getElementById('error-toast');
  if (!t) {
    t = document.createElement('div');
    t.id = 'error-toast';
    t.style = 'position:fixed;top:12px;left:50%;transform:translateX(-50%);background:#ef4444;color:#fff;padding:11px 18px;border-radius:12px;font-size:13.5px;box-shadow:0 6px 20px rgba(239,68,68,0.35);z-index:99999;transition:opacity 0.3s,top 0.3s;opacity:0;pointer-events:none;max-width:90%;text-align:center;font-weight:600;line-height:1.4;white-space:pre-wrap;';
    document.body.appendChild(t);
  }
  t.textContent = msg;
  t.style.top = '18px';
  t.style.opacity = '1';
  setTimeout(() => {
    t.style.opacity = '0';
    t.style.top = '-50px';
  }, 4000);
}

const colors = ['#1B4B66', '#D4AF37', '#C1440E', '#15803D', '#2563EB', '#9333EA', '#0D9488'];
function getColColor(str) {
  let hash = 0;
  const s = String(str || '');
  for (let i = 0; i < s.length; i++) hash = s.charCodeAt(i) + ((hash << 5) - hash);
  return colors[Math.abs(hash) % colors.length];
}

function getInitials(name) {
  if (!name) return '?';
  const parts = String(name).trim().split(/\s+/).filter(Boolean);
  if (!parts.length) return '?';
  return (parts[0][0] + (parts[1] ? parts[1][0] : '')).toUpperCase();
}

// ===== Token Refresh & API Client =====
let refreshPromise = null;

async function refreshAuthToken() {
  if (refreshPromise) return refreshPromise;
  refreshPromise = (async () => {
    if (refreshToken) {
      try {
        const res = await fetch(API + '/api/v1/auth/refresh', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ refreshToken })
        });
        if (res.ok) {
          const data = await res.json();
          const newAccess = data?.data?.accessToken || data?.accessToken;
          const newRefresh = data?.data?.refreshToken || data?.refreshToken;
          if (newAccess) {
            token = newAccess;
            if (newRefresh) refreshToken = newRefresh;
            try {
              localStorage.setItem('tc_token', token);
              if (newRefresh) localStorage.setItem('tc_refresh_token', refreshToken);
            } catch (e) {}
            return token;
          }
        }
      } catch (err) {
        console.warn('Refresh token error:', err);
      }
    }

    if (isTma) {
      try {
        const authRes = await fetch(API + '/api/v1/auth/telegram', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ initData: tg.initData })
        });
        if (authRes.ok) {
          const data = await authRes.json();
          token = data?.data?.token || data?.token;
          refreshToken = data?.data?.refreshToken || data?.refreshToken || refreshToken;
          try {
            if (token) localStorage.setItem('tc_token', token);
            if (refreshToken) localStorage.setItem('tc_refresh_token', refreshToken);
          } catch (e) {}
          return token;
        }
      } catch (e) {}
    }
    throw new Error('AUTH_FATAL');
  })().finally(() => {
    refreshPromise = null;
  });
  return refreshPromise;
}

async function api(path, opts = {}, retry = true) {
  const controller = new AbortController();
  const id = setTimeout(() => controller.abort(), 20000);
  try {
    const isFormData = opts.body instanceof FormData;
    const headers = {
      ...(isFormData ? {} : { 'Content-Type': 'application/json' }),
      ...(token && { Authorization: 'Bearer ' + token }),
      ...(opts.headers || {})
    };
    const res = await fetch(API + path, {
      ...opts,
      signal: controller.signal,
      headers
    });
    clearTimeout(id);

    if (res.status === 401) {
      if (retry && !path.includes('/auth/telegram') && !path.includes('/auth/refresh') && !path.includes('/auth/login')) {
        try {
          await refreshAuthToken();
          return api(path, opts, false);
        } catch (e) {
          throw new Error('AUTH');
        }
      }
      throw new Error('AUTH');
    }

    if (!res.ok) {
      const txt = await res.text();
      let errMsg = 'Xatolik (' + res.status + ')';
      try {
        const parsed = JSON.parse(txt);
        errMsg = parsed.message || parsed.error || txt;
        if (parsed.data && typeof parsed.data === 'object') {
          const details = Object.entries(parsed.data).map(([k, v]) => `${k}: ${v}`).join('\n');
          if (details) errMsg += '\n' + details;
        }
      } catch (e) {
        if (txt) errMsg = txt;
      }
      showErrorNotification(errMsg);
      const err = new Error(errMsg);
      err.isHandled = true;
      throw err;
    }
    if (res.status === 204) return null;
    const contentType = res.headers.get('content-type') || '';
    if (contentType.includes('application/json')) {
      return res.json();
    }
    return res.text();
  } catch (err) {
    clearTimeout(id);
    if (err.name === 'AbortError') {
      showErrorNotification('So\'rov vaqti tugadi (Timeout)');
      throw new Error('Timeout');
    }
    if (!err.isHandled && err.message !== 'AUTH' && err.message !== 'AUTH_FATAL') {
      showErrorNotification(err.message);
      err.isHandled = true;
    }
    throw err;
  }
}

const endpoints = {
  workspaces: '/api/workspaces',
  board: (ws, sprintId) => `/api/workspaces/${ws}/board${sprintId ? `?sprintId=${encodeURIComponent(sprintId)}` : ''}`,
  columns: (ws) => `/api/workspaces/${ws}/columns`,
  tasks: (ws) => `/api/workspaces/${ws}/tasks`,
  taskMove: (ws, id) => `/api/workspaces/${ws}/tasks/${id}/reorder`,
  taskDetails: (ws, id) => `/api/workspaces/${ws}/tasks/${id}`,
  comments: (id) => `/api/tasks/${id}/comments`,
  members: (ws) => `/api/workspaces/${ws}/members`,
  labels: (ws) => `/api/workspaces/${ws}/labels`,
  directions: (ws) => `/api/workspaces/${ws}/directions`,
  sprints: (ws) => `/api/workspaces/${ws}/sprints`,
  backlog: (ws) => `/api/workspaces/${ws}/backlog?size=100`,
  assign: (ws, id, userId) => `/api/workspaces/${ws}/tasks/${id}/assign?userId=${userId}`
};

// ===== Startup & Authentication (Works on Desktop + Mobile + Telegram Mini App) =====
async function initCore() {
  // 1. Try Telegram WebApp auth if inside Telegram Mini App and no valid session yet
  if (isTma && !token) {
    try {
      const r = await api('/api/v1/auth/telegram', {
        method: 'POST',
        body: JSON.stringify({ initData: tg.initData })
      }, false);
      token = r?.data?.token || r?.token;
      refreshToken = r?.data?.refreshToken || r?.refreshToken || refreshToken;
      if (token) localStorage.setItem('tc_token', token);
      if (refreshToken) localStorage.setItem('tc_refresh_token', refreshToken);
    } catch (e) {
      throw new Error('AUTH');
    }
  }

  if (!token) {
    throw new Error('AUTH');
  }

  // 2. Verify session via /api/me
  const meRes = await api('/api/me');
  currentUser = meRes?.data || meRes;
  currentUserId = currentUser?.id;
  updateSidebarUser();

  initWebSocket();
  refreshNotificationsBadge();

  try {
    const configRes = await api('/api/v1/config/public', {}, false);
    botUsername = configRes?.data?.botUsername || configRes?.botUsername || botUsername;
  } catch (e) {}

  await loadWorkspacesList();
}

function updateSidebarUser() {
  if (!currentUser) return;
  const av = document.getElementById('sidebar-avatar');
  const nm = document.getElementById('sidebar-username');
  const rl = document.getElementById('sidebar-userrole');
  const displayName = currentUser.fullName || currentUser.name || 'Foydalanuvchi';
  if (av) av.textContent = getInitials(displayName);
  if (nm) nm.textContent = displayName;
  if (rl) rl.textContent = (currentUser.statusNickname ? ('🏆 ' + currentUser.statusNickname) : ('@' + (currentUser.name || 'user')));

  const greetEl = document.getElementById('hero-greeting-badge');
  if (greetEl) greetEl.textContent = `Assalomu alaykum, ${displayName}!`;
  const rankEl = document.getElementById('hero-rank-pill');
  if (rankEl) rankEl.textContent = '🏆 ' + (currentUser.statusNickname || "O'zimizdan");
}

async function loadWorkspacesList(preferredWsId = null) {
  const wssRes = await api(endpoints.workspaces + '?size=100');
  allWorkspaces = wssRes?.data?.content || wssRes?.data || [];

  const selMobile = document.getElementById('ws');
  const selDesktop = document.getElementById('ws-desktop');
  const statsSel = document.getElementById('stats-ws');

  if (!allWorkspaces.length) {
    // Automatically create a starter workspace if user has none!
    try {
      const created = await api(endpoints.workspaces, {
        method: 'POST',
        body: JSON.stringify({
          title: 'Mening Loyiham',
          description: 'Asosiy ishchi maydon',
          bgColor: '#4f46e5',
          initDefaultColumns: true
        })
      });
      if (created?.data) allWorkspaces = [created.data];
    } catch (e) {}
  }

  [selMobile, selDesktop, statsSel].forEach(sel => {
    if (!sel) return;
    sel.innerHTML = '';
    allWorkspaces.forEach(w => {
      const opt = document.createElement('option');
      opt.value = w.id;
      opt.textContent = w.title || w.name;
      sel.appendChild(opt);
    });
  });

  if (selMobile) selMobile.onchange = (e) => selectWorkspace(e.target.value);
  if (selDesktop) selDesktop.onchange = (e) => selectWorkspace(e.target.value);

  storageGet('last_ws_id', async (err, val) => {
    let targetWsId = preferredWsId || (allWorkspaces[0] && allWorkspaces[0].id);
    if (initialTaskId) {
      try {
        const tRes = await api(`/api/tasks/${initialTaskId}`);
        if (tRes && tRes.data) targetWsId = tRes.data.workspaceId;
      } catch (e) {
        tgAlert("Vazifa topilmadi yoki ko'rish uchun ruxsat yo'q.");
        initialTaskId = null;
      }
    } else if (val && allWorkspaces.some(w => w.id === val)) {
      targetWsId = val;
    }

    if (targetWsId) {
      if (selMobile) selMobile.value = targetWsId;
      if (selDesktop) selDesktop.value = targetWsId;
      if (statsSel) statsSel.value = targetWsId;
      storageGet('sort_prefs', (errS, valS) => {
        if (valS) {
          try { sortPrefs = JSON.parse(valS); } catch (e) {}
        }
        loadBoard(targetWsId).then(() => {
          if (initialTaskId) {
            openSheet('view', initialTaskId);
            initialTaskId = null;
          }
        });
      });
    } else {
      hideLoader();
    }
  });
}

function selectWorkspace(wsId) {
  if (!wsId) return;
  const selMobile = document.getElementById('ws');
  const selDesktop = document.getElementById('ws-desktop');
  const statsSel = document.getElementById('stats-ws');
  if (selMobile) selMobile.value = wsId;
  if (selDesktop) selDesktop.value = wsId;
  if (statsSel) statsSel.value = wsId;
  selectedSprintFilter = '';
  loadBoard(wsId);
  if (currentTab === 'sprints') loadSprintsTab();
  if (currentTab === 'stats') loadStats();
  if (currentTab === 'workspace') loadWorkspaceSettingsTab();
}

function hideLoader() {
  const loader = document.getElementById('loader');
  if (loader) {
    loader.style.opacity = '0';
    setTimeout(() => { loader.style.display = 'none'; }, 250);
  }
}

async function init() {
  try {
    await initCore();
    document.getElementById('login-view').style.display = 'none';
    hideLoader();
  } catch (e) {
    hideLoader();
    if (e.message === 'AUTH' || e.message === 'AUTH_FATAL') {
      showAuthScreen();
    } else {
      showAuthScreen();
    }
  }
}

function showAuthScreen() {
  document.getElementById('loader').style.display = 'none';
  document.getElementById('login-view').style.display = 'flex';
  if (isTma && tg.MainButton) {
    tg.MainButton.text = authMode === 'register' ? "RO'YXATDAN O'TISH" : "KIRISH VA ULASH";
    tg.MainButton.show();
    tg.MainButton.offClick(handleLogin);
    tg.MainButton.onClick(handleLogin);
  }
}

window.switchAuthMode = function (mode) {
  authMode = mode === 'register' ? 'register' : 'login';
  document.querySelectorAll('.auth-tab').forEach(btn => {
    btn.classList.toggle('active', btn.dataset.mode === authMode);
  });
  const inviteGroup = document.getElementById('invite-token-group');
  if (inviteGroup) inviteGroup.style.display = authMode === 'register' ? 'block' : 'none';
  const submitBtn = document.getElementById('btn-auth-submit');
  if (submitBtn) submitBtn.textContent = authMode === 'register' ? "Ro'yxatdan o'tish" : "Tizimga kirish";
  if (isTma && tg.MainButton) {
    tg.MainButton.text = authMode === 'register' ? "RO'YXATDAN O'TISH" : "KIRISH VA ULASH";
  }
};

window.handleLogin = async function () {
  const username = document.getElementById('login-username').value.trim();
  const password = document.getElementById('login-password').value.trim();
  const inviteToken = document.getElementById('register-invite-token')?.value.trim() || null;
  const errDiv = document.getElementById('login-error');
  errDiv.textContent = '';

  if (!username || !password) {
    errDiv.textContent = 'Iltimos, login va parolni kiriting';
    return;
  }

  const submitBtn = document.getElementById('btn-auth-submit');
  if (submitBtn) submitBtn.disabled = true;
  if (isTma && tg.MainButton) tg.MainButton.showProgress();

  try {
    const endpoint = authMode === 'register' ? '/api/v1/auth/register' : '/api/v1/auth/login';
    const payload = authMode === 'register'
      ? { name: username, password, ...(inviteToken ? { inviteToken } : {}) }
      : { name: username, password };

    const loginRes = await fetch(API + endpoint, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    });

    if (!loginRes.ok) {
      let msg = authMode === 'register' ? "Ro'yxatdan o'tishda xatolik" : "Login yoki parol xato";
      try {
        const errJson = await loginRes.json();
        msg = errJson.message || msg;
      } catch (e) {}
      throw new Error(msg);
    }

    const loginData = await loginRes.json();
    token = loginData.data?.token || loginData.token;
    refreshToken = loginData.data?.refreshToken || loginData.refreshToken || null;
    try {
      if (token) localStorage.setItem('tc_token', token);
      if (refreshToken) localStorage.setItem('tc_refresh_token', refreshToken);
    } catch (e) {}

    // If inside Telegram Mini App, automatically link Telegram account
    if (isTma && tg.initData) {
      await fetch(API + '/api/users/me/telegram', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', Authorization: 'Bearer ' + token },
        body: JSON.stringify({ initData: tg.initData })
      }).catch(() => {});
    }

    document.getElementById('login-password').value = '';
    if (isTma && tg.MainButton) {
      tg.MainButton.hideProgress();
      tg.MainButton.offClick(handleLogin);
      tg.MainButton.hide();
    }
    document.getElementById('login-view').style.display = 'none';
    document.getElementById('loader').style.display = 'flex';
    document.getElementById('loader').style.opacity = '1';
    await initCore();
    hideLoader();
  } catch (e) {
    if (isTma && tg.MainButton) tg.MainButton.hideProgress();
    errDiv.textContent = e.message;
    showErrorNotification(e.message);
  } finally {
    if (submitBtn) submitBtn.disabled = false;
  }
};

// ===== Board & Workspace Data Loading =====
async function loadBoard(wsId) {
  if (!wsId) return;
  currentWsId = wsId;
  storageSet('last_ws_id', wsId);
  subscribeWsWorkspace(wsId);

  const container = document.getElementById('board-container');
  container.innerHTML = Array(3).fill(0).map(() => `
    <div class="col">
      <div class="col-header"><div class="skeleton" style="width:120px;height:20px;"></div></div>
      <div class="list" style="padding:12px;">
        <div class="skeleton card" style="height:85px;margin-bottom:10px;"></div>
        <div class="skeleton card" style="height:65px;"></div>
      </div>
    </div>
  `).join('');

  try {
    const [colsRes, membersRes, labelsRes, dirsRes, sprintsRes] = await Promise.all([
      api(endpoints.columns(wsId)),
      api(endpoints.members(wsId)).catch(() => ({ data: [] })),
      api(endpoints.labels(wsId)).catch(() => ({ data: [] })),
      api(endpoints.directions(wsId)).catch(() => ({ data: [] })),
      api(endpoints.sprints(wsId)).catch(() => ({ data: [] }))
    ]);

    colsData = colsRes?.data || [];
    workspaceMembers = (membersRes?.data || []).filter(m => m.name !== 'Taklif etilgan');
    workspaceLabels = labelsRes?.data || [];
    workspaceDirections = dirsRes?.data || [];
    workspaceSprints = sprintsRes?.data || [];

    tasksData = [];
    colsData.forEach(c => {
      if (c.tasks) tasksData.push(...c.tasks);
    });

    populateBoardToolbarSelects();
    renderFormPickers();
    renderBoard();
    hideLoader();
  } catch (e) {
    container.innerHTML = `<div class="empty-placeholder">Doskani yuklashda xatolik: ${e.message}</div>`;
    hideLoader();
  }
}

function populateBoardToolbarSelects() {
  // Sprint filter select
  const sprintSel = document.getElementById('board-sprint-select');
  if (sprintSel) {
    sprintSel.innerHTML = `<option value="">Barcha vazifalar (Doska)</option>`;
    workspaceSprints.forEach(s => {
      const opt = document.createElement('option');
      opt.value = s.id;
      opt.textContent = `${s.name} (${s.status === 'ACTIVE' ? 'Faol' : s.status})`;
      sprintSel.appendChild(opt);
    });
    sprintSel.value = selectedSprintFilter;
  }

  // Direction filter select
  const dirSel = document.getElementById('filter-direction');
  if (dirSel) {
    dirSel.innerHTML = `<option value="">Barcha yo'nalishlar</option>`;
    workspaceDirections.forEach(d => {
      const opt = document.createElement('option');
      opt.value = d.id;
      opt.textContent = d.name;
      dirSel.appendChild(opt);
    });
    dirSel.value = selectedDirectionFilter;
  }

  // Label filter select
  const lblSel = document.getElementById('filter-label');
  if (lblSel) {
    lblSel.innerHTML = `<option value="">Barcha teglar</option>`;
    workspaceLabels.forEach(l => {
      const opt = document.createElement('option');
      opt.value = l.id;
      opt.textContent = l.name;
      lblSel.appendChild(opt);
    });
    lblSel.value = selectedLabelFilter;
  }

  // Form selects (Column & Sprint)
  const fCol = document.getElementById('f-col');
  if (fCol) {
    fCol.innerHTML = '';
    colsData.forEach(c => {
      const opt = document.createElement('option');
      opt.value = c.id;
      opt.textContent = c.title;
      fCol.appendChild(opt);
    });
  }

  const fSprint = document.getElementById('f-sprint');
  if (fSprint) {
    fSprint.innerHTML = `<option value="">Backlog (Sprintsiz)</option>`;
    workspaceSprints.filter(s => s.status !== 'COMPLETED').forEach(s => {
      const opt = document.createElement('option');
      opt.value = s.id;
      opt.textContent = `${s.name} (${s.status})`;
      fSprint.appendChild(opt);
    });
  }

  const fParent = document.getElementById('f-parent-task');
  if (fParent) {
    fParent.innerHTML = `<option value="">Mustaqil vazifa (Ota vazifasiz)</option>`;
    tasksData.filter(t => !t.isArchived).forEach(t => {
      const opt = document.createElement('option');
      opt.value = t.id;
      opt.textContent = `[${t.publicId || 'TASK'}] ${t.title}`;
      fParent.appendChild(opt);
    });
  }
}

function renderFormPickers() {
  // Multi-select Assignees
  const picker = document.getElementById('f-assignee-picker');
  if (picker) {
    picker.innerHTML = '';
    workspaceMembers.forEach(m => {
      const d = document.createElement('div');
      d.className = 'member-avatar';
      d.dataset.id = m.id;
      d.title = m.fullName || m.name;
      d.textContent = getInitials(m.fullName || m.name);
      d.onclick = () => d.classList.toggle('active');
      picker.appendChild(d);
    });
    if (!workspaceMembers.length) {
      picker.innerHTML = `<span class="hint">A'zolar yo'q</span>`;
    }
  }

  // Multi-select Directions
  const dirPicker = document.getElementById('f-direction-picker');
  if (dirPicker) {
    dirPicker.innerHTML = '';
    workspaceDirections.forEach(dir => {
      const pill = document.createElement('span');
      pill.className = 'tag-choice';
      pill.dataset.id = dir.id;
      pill.textContent = dir.name;
      pill.onclick = () => pill.classList.toggle('active');
      dirPicker.appendChild(pill);
    });
    if (!workspaceDirections.length) {
      dirPicker.innerHTML = `<span class="hint">Yo'nalishlar yo'q ("Jamoa/Profil" bo'limidan qo'shing)</span>`;
    }
  }

  // Multi-select Labels
  const lblPicker = document.getElementById('f-label-picker');
  if (lblPicker) {
    lblPicker.innerHTML = '';
    workspaceLabels.forEach(lbl => {
      const pill = document.createElement('span');
      pill.className = 'tag-choice';
      pill.dataset.id = lbl.id;
      pill.textContent = lbl.name;
      pill.onclick = () => pill.classList.toggle('active');
      lblPicker.appendChild(pill);
    });
    if (!workspaceLabels.length) {
      lblPicker.innerHTML = `<span class="hint">Teglar yo'q ("Jamoa/Profil" bo'limidan qo'shing)</span>`;
    }
  }
}

function updateHeroLiveStats() {
  const ws = allWorkspaces.find(w => w.id === currentWsId);
  const wsTitleEl = document.getElementById('hero-ws-title');
  const wsDescEl = document.getElementById('hero-ws-desc');
  if (wsTitleEl && ws) {
    wsTitleEl.innerHTML = `${escapeHtmlText(ws.title || 'Ish maydoni')} — <span class="gold-accent">tartibli rejalar</span>`;
  }
  if (wsDescEl) {
    wsDescEl.textContent = (ws && ws.description)
      ? ws.description
      : "Vazifalarni ustuvorlik bo‘yicha boshqaring, sprint tezligini kuzating va jamoa bilan real vaqtda hamkorlik qiling.";
  }

  const activeTasks = tasksData.filter(t => !t.isArchived);
  const total = activeTasks.length;
  const doneColIds = new Set(colsData.filter(c => c.isDone || /bajarildi|tugadi|done|ko'z tegmasin/i.test(c.title || '')).map(c => c.id));
  const doneCount = activeTasks.filter(t => doneColIds.has(t.columnId)).length;
  const inProgCount = Math.max(0, total - doneCount);
  const overdueCount = activeTasks.filter(t => t.isOverdue && !doneColIds.has(t.columnId)).length;
  const pct = total > 0 ? Math.round((doneCount / total) * 100) : 0;

  const elTot = document.getElementById('hkpi-total');
  const elDone = document.getElementById('hkpi-done');
  const elAct = document.getElementById('hkpi-active');
  const elOvr = document.getElementById('hkpi-overdue');
  if (elTot) elTot.textContent = total;
  if (elDone) elDone.textContent = `${doneCount} (${pct}%)`;
  if (elAct) elAct.textContent = inProgCount;
  if (elOvr) elOvr.textContent = overdueCount;

  const floatDone = document.getElementById('float-chip-done-text');
  if (floatDone) floatDone.textContent = `Samaradorlik: ${pct}% (${doneCount}/${total})`;
}

function escapeHtmlText(str) {
  return String(str || '').replace(/[&<>"']/g, ch => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[ch]));
}

window.toggleHeroShowcase = function () {
  const hero = document.getElementById('hero-showcase');
  if (!hero) return;
  const collapsed = hero.classList.toggle('collapsed');
  try { localStorage.setItem('tc_hero_collapsed', collapsed ? '1' : '0'); } catch (e) {}
  haptic('impact', 'light');
};

function renderBoard() {
  updateHeroLiveStats();
  const container = document.getElementById('board-container');
  if (!container) return;
  container.innerHTML = '';

  colsData.forEach(c => {
    let colTasks = tasksData.filter(t => t.columnId === c.id);

    const sort = sortPrefs[c.id] || 'manual';
    if (sort === 'deadline') {
      colTasks.sort((a, b) => (a.dueDate || '9999-99-99').localeCompare(b.dueDate || '9999-99-99'));
    } else if (sort === 'priority') {
      const pMap = { URGENT: 4, HIGH: 3, MEDIUM: 2, LOW: 1 };
      colTasks.sort((a, b) => (pMap[b.priority] || 0) - (pMap[a.priority] || 0));
    } else if (sort === 'points') {
      colTasks.sort((a, b) => (b.storyPoints || 0) - (a.storyPoints || 0));
    } else {
      colTasks.sort((a, b) => (a.lexoRank || '').localeCompare(b.lexoRank || ''));
    }

    const colColor = c.isDone ? '#10b981' : getColColor(c.title || c.id);
    const colEl = document.createElement('div');
    colEl.className = 'col';
    colEl.style.setProperty('--col-color', colColor);

    colEl.innerHTML = `
      <div class="col-header">
        <div class="col-header-left">
          <span class="col-title-text"></span>
          ${c.isDone ? '<span class="col-done-badge">✓ Done</span>' : ''}
          <span class="col-count">0</span>
        </div>
        <div style="display:flex;align-items:center;gap:4px;">
          <button type="button" class="sort-btn rebal-btn" title="LexoRank tartibini muvozanatlash">⚖️</button>
          <button type="button" class="sort-btn" title="Saralash">⇅</button>
          <button type="button" class="sort-btn del-btn" title="Ustunni o‘chirish">×</button>
        </div>
      </div>
      <div class="list"></div>
      <div class="col-footer">
        <div class="quick-add">
          <input type="text" placeholder="+ Tez vazifa qo'shish (Enter)...">
        </div>
        <button type="button" class="btn-add-task">+ Batafsil vazifa yaratish</button>
      </div>
    `;

    colEl.querySelector('.col-header-left').onclick = () => openColumnEditModal(c);
    colEl.querySelector('.col-title-text').textContent = c.title;
    const countEl = colEl.querySelector('.col-count');
    if (c.wipLimit && c.wipLimit > 0) {
      countEl.textContent = `${colTasks.length}/${c.wipLimit}`;
      if (colTasks.length > c.wipLimit) {
        countEl.style.background = '#ef4444';
        countEl.style.color = '#fff';
        countEl.title = 'WIP limitdan oshib ketdi!';
      }
    } else {
      countEl.textContent = colTasks.length;
    }
    colEl.querySelector('.rebal-btn').onclick = async (e) => {
      e.stopPropagation();
      try {
        await api(`/api/workspaces/${currentWsId}/tasks/columns/${c.id}/rebalance`, { method: 'POST' });
        showToast("Ustun tartibi muvozanatlandi ⚖️");
        loadBoard(currentWsId);
      } catch (err) {}
    };
    colEl.querySelectorAll('.sort-btn')[1].onclick = (e) => { e.stopPropagation(); cycleSortColumn(c.id); };
    colEl.querySelector('.del-btn').onclick = (e) => { e.stopPropagation(); promptDeleteColumn(c.id); };

    const listEl = colEl.querySelector('.list');
    listEl.dataset.col = c.id;

    if (colTasks.length === 0) {
      listEl.innerHTML = `<div class="empty-placeholder"><div style="font-size:26px;margin-bottom:6px;">📋</div>Ushbu ustunda vazifalar yo'q</div>`;
    } else {
      colTasks.forEach(t => listEl.appendChild(createCard(t)));
    }

    const qInp = colEl.querySelector('.quick-add input');
    qInp.onkeydown = (e) => { if (e.key === 'Enter') quickAddTask(qInp, c.id); };
    colEl.querySelector('.btn-add-task').onclick = () => openSheet('create', null, c.id);

    container.appendChild(colEl);

    if (window.Sortable) {
      Sortable.create(listEl, {
        group: 'board',
        animation: 180,
        delay: 140,
        delayOnTouchOnly: true,
        ghostClass: 'sortable-ghost',
        dragClass: 'sortable-drag',
        onStart: () => haptic('impact', 'light'),
        onEnd: (ev) => {
          const taskId = ev.item.dataset.id;
          const newColId = ev.to.dataset.col;
          if (taskId && newColId) {
            const cards = Array.from(ev.to.querySelectorAll('.card'));
            const idx = cards.indexOf(ev.item);
            const prevTask = idx > 0 ? tasksData.find(t => t.id === cards[idx - 1].dataset.id) : null;
            const nextTask = idx < cards.length - 1 ? tasksData.find(t => t.id === cards[idx + 1].dataset.id) : null;
            haptic('impact', 'medium');
            moveTask(taskId, newColId, prevTask?.lexoRank || null, nextTask?.lexoRank || null);
          }
        }
      });
    }
  });

  const addColCard = document.createElement('div');
  addColCard.className = 'col';
  addColCard.style = 'justify-content:center;align-items:center;cursor:pointer;opacity:0.75;background:transparent;border:2px dashed var(--line);min-height:180px;';
  addColCard.innerHTML = `<div style="font-weight:700;color:var(--btn-color);">+ Yangi ustun qo'shish</div>`;
  addColCard.onclick = () => promptAddColumn();
  container.appendChild(addColCard);

  applyFilters();
}

function createCard(t) {
  const card = document.createElement('div');
  card.className = 'card';
  card.dataset.id = t.id;
  card.dataset.prio = t.priority || 'MEDIUM';

  let pressTimer = null;
  card.ontouchstart = () => {
    pressTimer = setTimeout(() => {
      pressTimer = null;
      haptic('impact', 'heavy');
      openSheet('quick', t.id);
    }, 600);
  };
  card.ontouchend = card.ontouchcancel = () => {
    if (pressTimer) clearTimeout(pressTimer);
  };
  card.oncontextmenu = (e) => {
    e.preventDefault();
    openSheet('quick', t.id);
  };
  card.onclick = () => openSheet('view', t.id);

  const issueIcons = { TASK: '📋', BUG: '🐞', STORY: '📘', EPIC: '🚀' };
  const issueIcon = issueIcons[t.issueType] || '📋';
  const prioMap = { URGENT: 'Shoshilinch', HIGH: 'Yuqori', MEDIUM: "O'rta", LOW: 'Past' };
  const prioLabel = prioMap[t.priority] || "O'rta";
  const prioClass = (t.priority || 'medium').toLowerCase();

  card.innerHTML = `
    <div class="card-top-row">
      <span class="task-key-badge"></span>
      <span class="points-slot"></span>
    </div>
    <div class="card-title"></div>
    <div class="card-meta">
      <div class="badges"></div>
      <div class="avatar-stack"></div>
    </div>
  `;

  card.querySelector('.task-key-badge').textContent = `${issueIcon} ${t.publicId || 'TASK'}`;
  if (t.storyPoints != null && t.storyPoints > 0) {
    const sp = document.createElement('span');
    sp.className = 'points-badge';
    sp.textContent = `${t.storyPoints} SP`;
    card.querySelector('.points-slot').appendChild(sp);
  }

  card.querySelector('.card-title').textContent = t.title;
  const badgesEl = card.querySelector('.badges');

  const bP = document.createElement('span');
  bP.className = `badge priority ${prioClass}`;
  bP.textContent = prioLabel;
  badgesEl.appendChild(bP);

  if (t.dueDate) {
    const bD = document.createElement('span');
    bD.className = `badge date ${t.isOverdue ? 'overdue' : ''}`;
    bD.textContent = '📅 ' + t.dueDate;
    badgesEl.appendChild(bD);
  }

  if (t.totalChecklistItems && t.totalChecklistItems > 0) {
    const bChk = document.createElement('span');
    bChk.className = 'badge date';
    if (t.completedChecklistItems === t.totalChecklistItems) {
      bChk.style.background = 'var(--ok-soft)';
      bChk.style.color = 'var(--ok)';
    }
    bChk.textContent = `✓ ${t.completedChecklistItems || 0}/${t.totalChecklistItems}`;
    badgesEl.appendChild(bChk);
  }

  if (t.loggedHours > 0 || t.estimatedHours > 0) {
    const bTime = document.createElement('span');
    bTime.className = 'badge date';
    bTime.textContent = `⏱ ${t.loggedHours || 0}${t.estimatedHours ? '/' + t.estimatedHours : ''}s`;
    badgesEl.appendChild(bTime);
  }

  if (t.parentId) {
    const parentTask = tasksData.find(pt => pt.id === t.parentId);
    const bPar = document.createElement('span');
    bPar.className = 'badge direction';
    bPar.textContent = `🔗 ${parentTask ? (parentTask.publicId || 'Sub-task') : 'Sub-task'}`;
    badgesEl.appendChild(bPar);
  }

  if (t.directions && t.directions.length) {
    t.directions.forEach(d => {
      const bDir = document.createElement('span');
      bDir.className = 'badge direction';
      bDir.textContent = d.name;
      badgesEl.appendChild(bDir);
    });
  }

  if (t.labels && t.labels.length) {
    t.labels.forEach(l => {
      const bLbl = document.createElement('span');
      bLbl.className = 'badge';
      bLbl.style.background = 'var(--sec-bg-color)';
      bLbl.style.border = '1px solid var(--line)';
      bLbl.textContent = '🏷 ' + l.name;
      badgesEl.appendChild(bLbl);
    });
  }

  const stack = card.querySelector('.avatar-stack');
  if (t.assignees && t.assignees.length > 0) {
    t.assignees.slice(0, 3).forEach(a => {
      const av = document.createElement('div');
      av.className = 'assignee-avatar';
      av.title = a.fullName || a.name;
      av.textContent = getInitials(a.fullName || a.name);
      stack.appendChild(av);
    });
  }

  return card;
}

window.setFilter = function (el) {
  document.querySelectorAll('#main-filters .chip').forEach(c => c.classList.remove('active'));
  el.classList.add('active');
  currentFilter = el.dataset.filter;
  haptic('impact', 'light');
  applyFilters();
};

window.applyFilters = function () {
  searchQuery = (document.getElementById('search-input')?.value || '').toLowerCase().trim();
  selectedSprintFilter = document.getElementById('board-sprint-select')?.value || '';
  selectedDirectionFilter = document.getElementById('filter-direction')?.value || '';
  selectedLabelFilter = document.getElementById('filter-label')?.value || '';

  document.querySelectorAll('#board-container .card').forEach(card => {
    const taskId = card.dataset.id;
    const task = tasksData.find(t => t.id === taskId);
    if (!task) return;

    let match = true;

    // Archive filter
    if (currentFilter === 'archived') {
      if (!task.isArchived) match = false;
    } else {
      if (task.isArchived) match = false;
    }

    if (match && searchQuery) {
      const hay = `${task.title || ''} ${task.publicId || ''} ${task.description || ''}`.toLowerCase();
      if (!hay.includes(searchQuery)) match = false;
    }

    if (match && selectedSprintFilter) {
      if (task.sprintId !== selectedSprintFilter) match = false;
    }

    if (match && selectedDirectionFilter) {
      if (!task.directions || !task.directions.some(d => d.id === selectedDirectionFilter)) match = false;
    }

    if (match && selectedLabelFilter) {
      if (!task.labels || !task.labels.some(l => l.id === selectedLabelFilter)) match = false;
    }

    if (match) {
      if (currentFilter === 'mine') {
        if (!currentUserId || !task.assignees || !task.assignees.some(a => a.id === currentUserId)) match = false;
      } else if (currentFilter === 'overdue') {
        if (!task.isOverdue) match = false;
      } else if (currentFilter === 'high') {
        if (task.priority !== 'HIGH' && task.priority !== 'URGENT') match = false;
      }
    }

    card.style.display = match ? 'block' : 'none';
  });

  document.querySelectorAll('#board-container .col').forEach(col => {
    const list = col.querySelector('.list');
    if (list) {
      const count = Array.from(list.children).filter(c => c.style.display !== 'none' && c.classList.contains('card')).length;
      const countBadge = col.querySelector('.col-count');
      if (countBadge) countBadge.textContent = count;
    }
  });
};

function cycleSortColumn(colId) {
  const order = ['manual', 'priority', 'deadline', 'points'];
  const labels = {
    manual: "Qo'lda (LexoRank)",
    priority: "Muhimlik bo'yicha",
    deadline: "Muddat bo'yicha",
    points: "Story Points bo'yicha"
  };
  const current = sortPrefs[colId] || 'manual';
  const next = order[(order.indexOf(current) + 1) % order.length];
  sortPrefs[colId] = next;
  storageSet('sort_prefs', JSON.stringify(sortPrefs));
  renderBoard();
  showToast('Saralash: ' + labels[next]);
}

// ===== Column Management Modals =====
window.promptAddColumn = function () {
  openCustomModal("Yangi ustun qo'shish", (body) => {
    body.innerHTML = `
      <div class="form-group">
        <label>Ustun nomi *</label>
        <input type="text" id="modal-col-title" placeholder="Masalan: Code Review yoki Testing">
      </div>
      <div class="form-group">
        <label>WIP Limit (maksimal vazifalar soni, ixtiyoriy)</label>
        <input type="number" id="modal-col-wip" min="0" placeholder="Masalan: 5">
      </div>
      <div class="form-group" style="display:flex;align-items:center;gap:8px;">
        <input type="checkbox" id="modal-col-done" style="width:18px;height:18px;">
        <label for="modal-col-done" style="margin:0;cursor:pointer;">Bu ustun yakuniy ("Bajarildi / Done") ustunmi?</label>
      </div>
      <div class="detail-actions">
        <button type="button" class="btn-action btn-soft" data-action="close-sheet">Bekor qilish</button>
        <button type="button" class="btn-action btn-primary" id="modal-col-save">Qo'shish</button>
      </div>
    `;
    body.querySelector('#modal-col-save').onclick = async () => {
      const title = body.querySelector('#modal-col-title').value.trim();
      const isDone = body.querySelector('#modal-col-done').checked;
      const wipVal = parseInt(body.querySelector('#modal-col-wip').value, 10);
      const wipLimit = (!isNaN(wipVal) && wipVal > 0) ? wipVal : null;
      if (!title) return tgAlert("Ustun nomini kiriting");
      try {
        const res = await api(endpoints.columns(currentWsId), {
          method: 'POST',
          body: JSON.stringify({ title, isDone, wipLimit })
        });
        colsData.push(res.data);
        closeSheet();
        renderBoard();
        populateBoardToolbarSelects();
        showToast("Ustun qo'shildi");
      } catch (e) {}
    };
  });
};

function openColumnEditModal(col) {
  openCustomModal("Ustunni sozlash", (body) => {
    body.innerHTML = `
      <div class="form-group">
        <label>Ustun nomi</label>
        <input type="text" id="modal-col-edit-title" ${col.isDefault ? 'disabled' : ''}>
        ${col.isDefault ? '<small class="hint">Standart ustun nomini o\'zgartirib bo\'lmaydi</small>' : ''}
      </div>
      <div class="form-group">
        <label>WIP Limit (0 = cheklovsiz)</label>
        <input type="number" id="modal-col-edit-wip" min="0" placeholder="Masalan: 5">
      </div>
      <div class="form-group" style="display:flex;align-items:center;gap:8px;">
        <input type="checkbox" id="modal-col-edit-done" style="width:18px;height:18px;">
        <label for="modal-col-edit-done" style="margin:0;cursor:pointer;">Yakuniy ("Done") ustun sifatida belgilash</label>
      </div>
      <div class="detail-actions">
        <button type="button" class="btn-action btn-soft" data-action="close-sheet">Yopish</button>
        <button type="button" class="btn-action btn-primary" id="modal-col-edit-save">Saqlash</button>
      </div>
    `;
    body.querySelector('#modal-col-edit-title').value = col.title || '';
    body.querySelector('#modal-col-edit-wip').value = col.wipLimit || '';
    body.querySelector('#modal-col-edit-done').checked = Boolean(col.isDone);
    body.querySelector('#modal-col-edit-save').onclick = async () => {
      const title = body.querySelector('#modal-col-edit-title').value.trim();
      const isDone = body.querySelector('#modal-col-edit-done').checked;
      const wipRaw = body.querySelector('#modal-col-edit-wip').value.trim();
      const wipLimit = wipRaw === '' ? 0 : (parseInt(wipRaw, 10) || 0);
      try {
        const payload = col.isDefault ? { isDone, wipLimit } : { title, isDone, wipLimit };
        const res = await api(`/api/workspaces/${currentWsId}/columns/${col.id}`, {
          method: 'PATCH',
          body: JSON.stringify(payload)
        });
        Object.assign(col, res.data);
        closeSheet();
        renderBoard();
        showToast("Ustun yangilandi");
      } catch (e) {}
    };
  });
}

function promptDeleteColumn(colId) {
  const colTasks = tasksData.filter(t => t.columnId === colId);
  if (colTasks.length > 0) {
    return tgAlert("Avval ushbu ustundagi vazifalarni boshqa ustunga ko'chiring yoki o'chiring.");
  }
  tgConfirm("Ushbu ustunni o'chirishni tasdiqlaysizmi?", (ok) => {
    if (!ok) return;
    api(`/api/workspaces/${currentWsId}/columns/${colId}`, { method: 'DELETE' })
      .then(() => {
        colsData = colsData.filter(c => c.id !== colId);
        renderBoard();
        populateBoardToolbarSelects();
        showToast("Ustun o'chirildi");
      })
      .catch(() => {});
  });
}

async function moveTask(taskId, targetColId, prevRank = null, nextRank = null) {
  const task = tasksData.find(t => t.id === taskId);
  if (!task) return;
  const oldColId = task.columnId;
  task.columnId = targetColId;
  renderBoard();

  try {
    const res = await api(endpoints.taskMove(currentWsId, taskId), {
      method: 'PATCH',
      body: JSON.stringify({ columnId: targetColId, prevRank, nextRank })
    });
    if (res?.data) Object.assign(task, res.data);
    const col = colsData.find(c => c.id === targetColId);
    if (col && (col.isDone || /bajarildi|tugadi|done|ko'z tegmasin/i.test(col.title))) {
      celebrate();
    } else {
      showToast("Holat yangilandi");
    }
  } catch (e) {
    task.columnId = oldColId;
    renderBoard();
  }
}

function celebrate() {
  haptic('notification', 'success');
  showToast("🎉 Barakalla! Vazifa bajarildi!");
}

async function quickAddTask(inputEl, colId) {
  const title = inputEl.value.trim();
  if (!title) return;
  inputEl.disabled = true;
  try {
    const res = await api(endpoints.tasks(currentWsId), {
      method: 'POST',
      body: JSON.stringify({
        title,
        columnId: colId,
        issueType: 'TASK',
        priority: 'MEDIUM',
        sprintId: selectedSprintFilter || null
      })
    });
    if (res?.data) tasksData.push(res.data);
    inputEl.value = '';
    renderBoard();
    haptic('notification', 'success');
    showToast("Vazifa yaratildi");
  } catch (e) {}
  inputEl.disabled = false;
  inputEl.focus();
}

// ===== Sheets & Modals (Create / Edit / View / Custom) =====
function hideAllSheetViews() {
  ['task-form-view', 'task-detail-view', 'quick-action-view', 'custom-modal-view'].forEach(id => {
    const el = document.getElementById(id);
    if (el) el.style.display = 'none';
  });
}

function openCustomModal(titleText, renderFn) {
  haptic('impact', 'light');
  hideAllSheetViews();
  document.getElementById('sheet-title').textContent = titleText;
  const customView = document.getElementById('custom-modal-view');
  customView.innerHTML = '';
  customView.style.display = 'block';
  renderFn(customView);
  document.getElementById('sheet-overlay').classList.add('active');
  document.getElementById('bottom-sheet').classList.add('active');
  if (isTma && tg.BackButton) {
    tg.BackButton.show();
    tg.BackButton.onClick(closeSheet);
  }
}

window.openSheet = function (mode, taskId = null, defaultColId = null) {
  haptic('impact', 'light');
  hideAllSheetViews();
  document.getElementById('sheet-overlay').classList.add('active');
  document.getElementById('bottom-sheet').classList.add('active');

  const formView = document.getElementById('task-form-view');
  const detailView = document.getElementById('task-detail-view');
  const quickView = document.getElementById('quick-action-view');

  currentTaskId = taskId;
  if (isTma && tg.BackButton) {
    tg.BackButton.show();
    tg.BackButton.onClick(closeSheet);
  }

  if (mode === 'create') {
    formView.style.display = 'block';
    document.getElementById('sheet-title').textContent = "Yangi vazifa yaratish";
    document.getElementById('f-id').value = '';
    document.getElementById('f-title').value = '';
    document.getElementById('f-desc').value = '';
    document.getElementById('f-priority').value = 'MEDIUM';
    document.getElementById('f-issue-type').value = 'TASK';
    document.getElementById('f-date').value = '';
    document.getElementById('f-story-points').value = '';
    if (document.getElementById('f-est-hours')) document.getElementById('f-est-hours').value = '';
    if (document.getElementById('f-parent-task')) document.getElementById('f-parent-task').value = '';
    document.getElementById('f-sprint').value = selectedSprintFilter || '';
    if (defaultColId) document.getElementById('f-col').value = defaultColId;

    document.querySelectorAll('#f-assignee-picker .member-avatar').forEach(a => a.classList.remove('active'));
    document.querySelectorAll('#f-direction-picker .tag-choice').forEach(a => a.classList.remove('active'));
    document.querySelectorAll('#f-label-picker .tag-choice').forEach(a => a.classList.remove('active'));

    if (isTma && tg.MainButton) {
      tg.MainButton.text = "SAQLASH";
      tg.MainButton.show();
      tg.MainButton.offClick(saveTask);
      tg.MainButton.onClick(saveTask);
    }
  } else if (mode === 'edit') {
    const task = tasksData.find(t => t.id === taskId);
    if (!task) return closeSheet();
    formView.style.display = 'block';
    document.getElementById('sheet-title').textContent = "Vazifani tahrirlash";
    document.getElementById('f-id').value = task.id;
    document.getElementById('f-title').value = task.title || '';
    document.getElementById('f-desc').value = task.description || '';
    document.getElementById('f-priority').value = task.priority || 'MEDIUM';
    document.getElementById('f-issue-type').value = task.issueType || 'TASK';
    document.getElementById('f-date').value = task.dueDate || '';
    document.getElementById('f-story-points').value = task.storyPoints != null ? task.storyPoints : '';
    if (document.getElementById('f-est-hours')) document.getElementById('f-est-hours').value = task.estimatedHours != null ? task.estimatedHours : '';
    if (document.getElementById('f-parent-task')) document.getElementById('f-parent-task').value = task.parentId || '';
    document.getElementById('f-sprint').value = task.sprintId || '';
    document.getElementById('f-col').value = task.columnId;

    const assignedIds = new Set((task.assignees || []).map(a => a.id));
    document.querySelectorAll('#f-assignee-picker .member-avatar').forEach(a => {
      a.classList.toggle('active', assignedIds.has(a.dataset.id));
    });

    const dirIds = new Set((task.directions || []).map(d => d.id));
    document.querySelectorAll('#f-direction-picker .tag-choice').forEach(el => {
      el.classList.toggle('active', dirIds.has(el.dataset.id));
    });

    const lblIds = new Set((task.labels || []).map(l => l.id));
    document.querySelectorAll('#f-label-picker .tag-choice').forEach(el => {
      el.classList.toggle('active', lblIds.has(el.dataset.id));
    });

    if (isTma && tg.MainButton) {
      tg.MainButton.text = "O'ZGARISHLARNI SAQLASH";
      tg.MainButton.show();
      tg.MainButton.offClick(saveTask);
      tg.MainButton.onClick(saveTask);
    }
  } else if (mode === 'view') {
    const task = tasksData.find(t => t.id === taskId);
    if (!task) return closeSheet();
    detailView.style.display = 'block';
    document.getElementById('sheet-title').textContent = "Vazifa tafsilotlari";
    document.getElementById('d-public-id').textContent = `${task.publicId || 'TASK'} • ${task.issueType || 'TASK'}`;
    document.getElementById('d-title').textContent = task.title;
    document.getElementById('d-desc').textContent = task.description || "Tavsif kiritilmagan";

    const archiveBtn = document.getElementById('btn-archive-task');
    if (archiveBtn) {
      archiveBtn.textContent = task.isArchived ? '📤 Arxivdan chiqarish' : '📦 Arxivlash';
    }

    const dCol = document.getElementById('d-col-select');
    dCol.innerHTML = '';
    colsData.forEach(c => {
      const opt = document.createElement('option');
      opt.value = c.id;
      opt.textContent = c.title;
      dCol.appendChild(opt);
    });
    dCol.value = task.columnId;

    // Assignees list in detail view
    const assigneesBox = document.getElementById('d-assignees-list');
    assigneesBox.innerHTML = '';
    if (task.assignees && task.assignees.length) {
      task.assignees.forEach(a => {
        const chip = document.createElement('span');
        chip.className = 'badge';
        chip.style.background = 'var(--sec-bg-color)';
        chip.textContent = '👤 ' + (a.fullName || a.name);
        assigneesBox.appendChild(chip);
      });
    } else {
      assigneesBox.innerHTML = '<span class="hint">Biriktirilmagan</span>';
    }

    const dBadges = document.getElementById('d-badges');
    dBadges.innerHTML = '';

    const prioMap = { URGENT: 'Shoshilinch', HIGH: 'Yuqori', MEDIUM: "O'rta", LOW: 'Past' };
    const pb = document.createElement('span');
    pb.className = `badge priority ${(task.priority || 'medium').toLowerCase()}`;
    pb.textContent = prioMap[task.priority] || "O'rta";
    dBadges.appendChild(pb);

    if (task.storyPoints != null) {
      const sp = document.createElement('span');
      sp.className = 'points-badge';
      sp.textContent = `${task.storyPoints} Story Points`;
      dBadges.appendChild(sp);
    }

    if (task.dueDate) {
      const db = document.createElement('span');
      db.className = `badge date ${task.isOverdue ? 'overdue' : ''}`;
      db.textContent = '📅 ' + task.dueDate;
      dBadges.appendChild(db);
    }

    (task.directions || []).forEach(d => {
      const b = document.createElement('span');
      b.className = 'badge direction';
      b.textContent = d.name;
      dBadges.appendChild(b);
    });

    (task.labels || []).forEach(l => {
      const ls = document.createElement('span');
      ls.className = 'badge';
      ls.style.background = 'var(--sec-bg-color)';
      ls.textContent = '🏷 ' + l.name;
      dBadges.appendChild(ls);
    });

    if (isTma && tg.MainButton) tg.MainButton.hide();
    switchDetailSubtab('checklists');
    loadChecklists(task.id);
    loadSubtasks(task.id);
    loadWorklogs(task.id);
    loadComments(task.id);
    loadAttachments(task.id);
    loadActivities(task.id);
  } else if (mode === 'quick') {
    const task = tasksData.find(t => t.id === taskId);
    if (!task) return closeSheet();
    quickView.style.display = 'block';
    document.getElementById('sheet-title').textContent = "Tezkor amallar";
    document.getElementById('qa-title').textContent = task.title;

    const sel = document.getElementById('qa-col-select');
    sel.innerHTML = `<option value="" disabled selected>Ustunga ko'chirish...</option>`;
    colsData.forEach(c => {
      const opt = document.createElement('option');
      opt.value = c.id;
      opt.textContent = c.title;
      if (c.id === task.columnId) opt.disabled = true;
      sel.appendChild(opt);
    });
  }
};

window.switchDetailSubtab = function (subtab) {
  document.querySelectorAll('.sub-tab').forEach(btn => {
    btn.classList.toggle('active', btn.dataset.subtab === subtab);
  });
  ['checklists', 'subtasks', 'worklogs', 'comments', 'attachments', 'activities'].forEach(name => {
    const panel = document.getElementById('subtab-' + name);
    if (panel) panel.style.display = name === subtab ? 'block' : 'none';
  });
};

window.closeSheet = function () {
  document.getElementById('sheet-overlay').classList.remove('active');
  document.getElementById('bottom-sheet').classList.remove('active');
  if (isTma && tg.BackButton) {
    tg.BackButton.hide();
    tg.BackButton.offClick(closeSheet);
  }
  if (isTma && tg.MainButton) {
    tg.MainButton.hide();
    tg.MainButton.offClick(saveTask);
  }
};

window.saveTask = async function () {
  const id = document.getElementById('f-id').value;
  const colId = document.getElementById('f-col').value;
  const title = document.getElementById('f-title').value.trim();
  const desc = document.getElementById('f-desc').value.trim();
  const priority = document.getElementById('f-priority').value;
  const issueType = document.getElementById('f-issue-type').value;
  const dueDate = document.getElementById('f-date').value || null;
  const spVal = document.getElementById('f-story-points').value;
  const storyPoints = spVal !== '' ? parseInt(spVal, 10) : null;
  const sprintId = document.getElementById('f-sprint').value || null;

  const assigneeIds = Array.from(document.querySelectorAll('#f-assignee-picker .member-avatar.active')).map(el => el.dataset.id);
  const directionIds = Array.from(document.querySelectorAll('#f-direction-picker .tag-choice.active')).map(el => el.dataset.id);
  const labelIds = Array.from(document.querySelectorAll('#f-label-picker .tag-choice.active')).map(el => el.dataset.id);

  if (!title) {
    return tgAlert("Vazifa sarlavhasini kiriting!");
  }

  if (isTma && tg.MainButton) tg.MainButton.showProgress();
  try {
    const estRaw = document.getElementById('f-est-hours')?.value || '';
    const estimatedHours = estRaw !== '' ? parseFloat(estRaw) : null;
    const parentId = document.getElementById('f-parent-task')?.value || null;
    const payload = {
      title,
      description: desc,
      priority,
      issueType,
      dueDate,
      storyPoints,
      estimatedHours,
      parentId,
      sprintId: sprintId || '',
      columnId: colId,
      assigneeIds,
      directionIds,
      labelIds
    };

    if (id) {
      const res = await api(endpoints.taskDetails(currentWsId, id), {
        method: 'PUT',
        body: JSON.stringify(payload)
      });
      const updated = res.data;
      tasksData = tasksData.map(t => t.id === id ? updated : t);
    } else {
      if (!payload.sprintId) delete payload.sprintId;
      const res = await api(endpoints.tasks(currentWsId), {
        method: 'POST',
        body: JSON.stringify(payload)
      });
      tasksData.push(res.data);
    }
    haptic('notification', 'success');
    showToast("Muvaffaqiyatli saqlandi");
    closeSheet();
    renderBoard();
    if (currentTab === 'sprints') loadSprintsTab();
    if (currentTab === 'mytasks') loadMyTasks();
  } catch (e) {
  } finally {
    if (isTma && tg.MainButton) tg.MainButton.hideProgress();
  }
};

window.deleteTask = async function () {
  if (!currentTaskId) return;
  tgConfirm("Ushbu vazifani o'chirishni tasdiqlaysizmi?", async (ok) => {
    if (!ok) return;
    try {
      await api(endpoints.taskDetails(currentWsId, currentTaskId), { method: 'DELETE' });
      tasksData = tasksData.filter(t => t.id !== currentTaskId);
      haptic('notification', 'success');
      showToast("Vazifa o'chirildi");
      closeSheet();
      renderBoard();
    } catch (e) {}
  });
};

window.toggleWatchTask = async function () {
  if (!currentTaskId || !currentWsId) return;
  try {
    await api(`/api/workspaces/${currentWsId}/tasks/${currentTaskId}/watch`, { method: 'POST' });
    showToast("Kuzatuv holati yangilandi");
  } catch (e) {}
};

window.toggleArchiveTask = async function () {
  if (!currentTaskId || !currentWsId) return;
  const task = tasksData.find(t => t.id === currentTaskId);
  if (!task) return;
  const endpoint = task.isArchived ? 'unarchive' : 'archive';
  try {
    const res = await api(`/api/workspaces/${currentWsId}/tasks/${currentTaskId}/${endpoint}`, { method: 'POST' });
    if (res?.data) {
      tasksData = tasksData.map(t => t.id === currentTaskId ? res.data : t);
    }
    showToast(task.isArchived ? "Arxivdan chiqarildi" : "Arxivlandi");
    closeSheet();
    renderBoard();
  } catch (e) {}
};

window.moveTaskFromSelect = async function (newColId) {
  if (!currentTaskId) return;
  const task = tasksData.find(t => t.id === currentTaskId);
  if (!task || task.columnId === newColId) return;
  await moveTask(currentTaskId, newColId);
};

// ===== Task Sub-Panels: Checklists, Comments, Attachments, Activities =====
async function loadChecklists(taskId) {
  const list = document.getElementById('checklists-list');
  const fill = document.getElementById('checklist-progress-fill');
  const txt = document.getElementById('checklist-progress-text');
  list.innerHTML = '<div class="skeleton" style="height:32px;margin-bottom:8px;"></div>';
  try {
    const res = await api(`/api/workspaces/${currentWsId}/tasks/${taskId}/checklists`);
    const items = res?.data || [];
    list.innerHTML = '';

    const completedCount = items.filter(i => i.isCompleted).length;
    const pct = items.length ? Math.round((completedCount / items.length) * 100) : 0;
    if (fill) fill.style.width = pct + '%';
    if (txt) txt.textContent = `${completedCount}/${items.length} (${pct}%)`;

    if (!items.length) {
      list.innerHTML = `<div class="hint" style="margin-bottom:10px;">Quyi bandlar hali qo'shilmagan</div>`;
    } else {
      items.forEach(c => {
        const d = document.createElement('div');
        d.className = 'checklist-item' + (c.isCompleted ? ' completed' : '');
        d.innerHTML = `
          <input type="checkbox">
          <span class="checklist-title" style="flex:1;"></span>
          <button type="button" class="icon-btn sm" style="color:var(--danger);border:none;">×</button>
        `;
        d.querySelector('input').checked = Boolean(c.isCompleted);
        d.querySelector('input').onchange = (e) => toggleChecklist(c.id, e.target.checked);
        d.querySelector('.checklist-title').textContent = c.title;
        d.querySelector('button').onclick = () => deleteChecklist(c.id);
        list.appendChild(d);
      });
    }
  } catch (e) {
    list.innerHTML = `<div class="hint">Yuklashda xatolik</div>`;
  }
}

window.addChecklist = async function () {
  const input = document.getElementById('checklist-input');
  const title = input.value.trim();
  if (!title || !currentTaskId) return;
  input.disabled = true;
  try {
    await api(`/api/workspaces/${currentWsId}/tasks/${currentTaskId}/checklists`, {
      method: 'POST',
      body: JSON.stringify({ title })
    });
    input.value = '';
    loadChecklists(currentTaskId);
  } catch (e) {}
  input.disabled = false;
};

async function toggleChecklist(itemId, isCompleted) {
  try {
    await api(`/api/workspaces/${currentWsId}/tasks/${currentTaskId}/checklists/${itemId}`, {
      method: 'PUT',
      body: JSON.stringify({ isCompleted })
    });
    loadChecklists(currentTaskId);
  } catch (e) {
    loadChecklists(currentTaskId);
  }
}

async function deleteChecklist(itemId) {
  try {
    await api(`/api/workspaces/${currentWsId}/tasks/${currentTaskId}/checklists/${itemId}`, { method: 'DELETE' });
    loadChecklists(currentTaskId);
  } catch (e) {}
}

async function loadSubtasks(taskId) {
  const list = document.getElementById('subtasks-list');
  const countEl = document.getElementById('subtasks-count');
  if (!list) return;
  list.innerHTML = '<div class="skeleton" style="height:32px;margin-bottom:8px;"></div>';
  try {
    const res = await api(`/api/workspaces/${currentWsId}/tasks/${taskId}/subtasks`);
    const subtasks = res?.data || [];
    if (countEl) countEl.textContent = subtasks.length;
    list.innerHTML = '';
    if (!subtasks.length) {
      list.innerHTML = '<div class="hint" style="margin-bottom:8px;">Hozircha ostki vazifalar (sub-tasks) yo‘q</div>';
      return;
    }
    subtasks.forEach(st => {
      const row = document.createElement('div');
      row.className = 'checklist-item';
      row.style.cursor = 'pointer';
      const col = colsData.find(c => c.id === st.columnId);
      row.innerHTML = `
        <span class="task-key-badge">${escapeHtmlText(st.publicId || 'SUB')}</span>
        <span style="flex:1;font-weight:700;">${escapeHtmlText(st.title)}</span>
        <span class="badge date">${escapeHtmlText(col ? col.title : 'Ustun')}</span>
      `;
      row.onclick = () => openSheet('view', st.id);
      list.appendChild(row);
    });
  } catch (e) {
    list.innerHTML = '<div class="hint">Ostki vazifalarni yuklashda xatolik</div>';
  }
}

window.addSubtask = async function () {
  const inp = document.getElementById('subtask-input');
  const title = inp?.value.trim();
  if (!title || !currentTaskId || !currentWsId) return;
  const parentTask = tasksData.find(t => t.id === currentTaskId);
  const colId = parentTask ? parentTask.columnId : (colsData[0] && colsData[0].id);
  if (!colId) return;
  inp.disabled = true;
  try {
    const res = await api(endpoints.tasks(currentWsId), {
      method: 'POST',
      body: JSON.stringify({
        title,
        columnId: colId,
        parentId: currentTaskId,
        priority: 'MEDIUM',
        issueType: 'TASK'
      })
    });
    if (res?.data) tasksData.push(res.data);
    inp.value = '';
    showToast("Ostki vazifa yaratildi");
    loadSubtasks(currentTaskId);
    renderBoard();
  } catch (e) {}
  inp.disabled = false;
};

async function loadWorklogs(taskId) {
  const list = document.getElementById('worklogs-list');
  const totalEl = document.getElementById('worklogs-total');
  if (!list) return;
  list.innerHTML = '<div class="skeleton" style="height:32px;margin-bottom:8px;"></div>';
  try {
    const res = await api(`/api/workspaces/${currentWsId}/tasks/${taskId}/worklogs`);
    const logs = res?.data || [];
    const totalHours = logs.reduce((acc, w) => acc + (w.hours || 0), 0);
    if (totalEl) totalEl.textContent = `${Math.round(totalHours * 100) / 100}s`;
    list.innerHTML = '';
    if (!logs.length) {
      list.innerHTML = '<div class="hint" style="margin-bottom:8px;">Hali ish vaqti (worklog) qayd etilmagan</div>';
      return;
    }
    logs.forEach(w => {
      const row = document.createElement('div');
      row.className = 'checklist-item';
      row.innerHTML = `
        <span class="points-badge">⏱ ${w.hours} soat</span>
        <div style="flex:1;min-width:0;">
          <div style="font-weight:700;font-size:13px;">${escapeHtmlText(w.description || 'Ish bajarildi')}</div>
          <div class="hint" style="font-size:11px;">👤 ${escapeHtmlText(w.userName || 'A’zo')} • 📅 ${escapeHtmlText(w.workDate || '')}</div>
        </div>
      `;
      list.appendChild(row);
    });
  } catch (e) {
    list.innerHTML = '<div class="hint">Vaqt tarixini yuklashda xatolik</div>';
  }
}

window.addWorklog = async function () {
  const hrsInp = document.getElementById('worklog-hours-input');
  const descInp = document.getElementById('worklog-desc-input');
  const hours = parseFloat(hrsInp?.value || '');
  const description = descInp?.value.trim() || '';
  if (!hours || hours <= 0 || !currentTaskId || !currentWsId) {
    return tgAlert("Sarflangan soatni (0 dan katta) kiriting");
  }
  hrsInp.disabled = true;
  try {
    await api(`/api/workspaces/${currentWsId}/tasks/${currentTaskId}/worklogs`, {
      method: 'POST',
      body: JSON.stringify({ hours, description })
    });
    hrsInp.value = '';
    if (descInp) descInp.value = '';
    const task = tasksData.find(t => t.id === currentTaskId);
    if (task) task.loggedHours = Math.round(((task.loggedHours || 0) + hours) * 100) / 100;
    showToast("Ish vaqti saqlandi ⏱");
    loadWorklogs(currentTaskId);
    renderBoard();
  } catch (e) {}
  hrsInp.disabled = false;
};

async function loadComments(taskId) {
  const list = document.getElementById('comments-list');
  const count = document.getElementById('comments-count');
  list.innerHTML = '<div class="skeleton" style="height:40px;margin-bottom:8px;"></div>';
  try {
    const res = await api(endpoints.comments(taskId) + '?size=50');
    const comments = res?.data?.content || res?.data || [];
    count.textContent = comments.length;
    list.innerHTML = '';
    if (!comments.length) {
      list.innerHTML = `<div class="hint" style="text-align:center;padding:12px;">Hali izohlar yo'q</div>`;
    } else {
      comments.forEach(c => {
        const div = document.createElement('div');
        div.className = 'comment';
        div.innerHTML = `
          <div class="comment-avatar"></div>
          <div class="comment-content">
            <div class="comment-author"><span class="c-name"></span><span class="c-del" style="cursor:pointer;color:var(--danger);"></span></div>
            <div class="comment-text"></div>
          </div>
        `;
        div.querySelector('.comment-avatar').textContent = getInitials(c.authorFullName || c.authorName);
        div.querySelector('.c-name').textContent = c.authorFullName || c.authorName || 'Foydalanuvchi';
        div.querySelector('.comment-text').textContent = c.content;
        if (c.authorId === currentUserId) {
          const delEl = div.querySelector('.c-del');
          delEl.textContent = "O'chirish";
          delEl.onclick = async () => {
            await api(`/api/tasks/${taskId}/comments/${c.id}`, { method: 'DELETE' });
            loadComments(taskId);
          };
        }
        list.appendChild(div);
      });
    }
    list.scrollTop = list.scrollHeight;
  } catch (e) {
    list.innerHTML = `<div class="hint">Izohlarni yuklashda xatolik</div>`;
  }
}

window.addComment = async function () {
  const input = document.getElementById('comment-input');
  const text = input.value.trim();
  if (!text || !currentTaskId) return;
  input.disabled = true;
  try {
    await api(endpoints.comments(currentTaskId), {
      method: 'POST',
      body: JSON.stringify({ content: text })
    });
    input.value = '';
    loadComments(currentTaskId);
  } catch (e) {}
  input.disabled = false;
};

async function loadAttachments(taskId) {
  const list = document.getElementById('attachments-list');
  if (!list) return;
  list.innerHTML = '<div class="hint">Yuklanmoqda...</div>';
  try {
    const res = await api(`/api/tasks/${taskId}/attachments`);
    const files = res?.data || [];
    list.innerHTML = '';
    if (!files.length) {
      list.innerHTML = '<div class="hint">Biriktirilgan fayllar yo\'q</div>';
      return;
    }
    files.forEach(f => {
      const row = document.createElement('div');
      row.className = 'checklist-item';
      row.innerHTML = `
        <span style="flex:1;font-weight:600;overflow:hidden;text-overflow:ellipsis;">📎 <span class="f-name"></span></span>
        <button type="button" class="btn-chip-action dl-btn">Yuklab olish</button>
        <button type="button" class="icon-btn sm del-btn" style="color:var(--danger);border:none;">×</button>
      `;
      row.querySelector('.f-name').textContent = f.fileName || 'fayl';
      row.querySelector('.dl-btn').onclick = () => window.open(`${API}/api/attachments/${f.id}/download`, '_blank');
      row.querySelector('.del-btn').onclick = async () => {
        await api(`/api/attachments/${f.id}`, { method: 'DELETE' });
        loadAttachments(taskId);
      };
      list.appendChild(row);
    });
  } catch (e) {
    list.innerHTML = '<div class="hint">Fayllarni yuklashda xatolik</div>';
  }
}

window.uploadTaskAttachment = async function () {
  const fileInp = document.getElementById('attachment-file-input');
  if (!fileInp || !fileInp.files.length || !currentTaskId) {
    return tgAlert("Avval faylni tanlang");
  }
  const fd = new FormData();
  fd.append('file', fileInp.files[0]);
  try {
    await api(`/api/tasks/${currentTaskId}/attachments`, { method: 'POST', body: fd });
    fileInp.value = '';
    showToast("Fayl yuklandi");
    loadAttachments(currentTaskId);
  } catch (e) {}
};

async function loadActivities(taskId) {
  const list = document.getElementById('activities-list');
  if (!list) return;
  list.innerHTML = '<div class="hint">Yuklanmoqda...</div>';
  try {
    const res = await api(`/api/tasks/${taskId}/activities?size=30`);
    const acts = res?.data?.content || res?.data || [];
    list.innerHTML = '';
    if (!acts.length) {
      list.innerHTML = '<div class="hint">Tarix mavjud emas</div>';
      return;
    }
    acts.forEach(a => {
      const row = document.createElement('div');
      row.style = 'padding:8px 0;border-bottom:1px solid var(--line);font-size:12.5px;';
      const b = document.createElement('strong');
      b.textContent = (a.userFullName || a.userName || 'Tizim') + ': ';
      const sp = document.createElement('span');
      sp.textContent = `${a.actionType} (${a.oldValue || '—'} → ${a.newValue || '—'})`;
      row.appendChild(b);
      row.appendChild(sp);
      list.appendChild(row);
    });
  } catch (e) {
    list.innerHTML = '<div class="hint">Tarixni yuklashda xatolik</div>';
  }
}

// ===== Tab Navigation =====
window.switchTab = function (tabId) {
  document.querySelectorAll('.tab-content').forEach(c => { c.style.display = 'none'; });
  document.querySelectorAll('.tab-btn, .nav-item').forEach(b => {
    b.classList.toggle('active', b.dataset.tab === tabId);
  });
  const target = document.getElementById('tab-' + tabId);
  if (target) target.style.display = 'flex';
  currentTab = tabId;
  haptic('impact', 'light');

  if (tabId === 'sprints') loadSprintsTab();
  if (tabId === 'mytasks') loadMyTasks();
  if (tabId === 'chat') loadChatTab();
  if (tabId === 'stats') loadStats();
  if (tabId === 'workspace') loadWorkspaceSettingsTab();
};

// ===== TAB 2: SPRINTS & BACKLOG =====
window.loadSprintsTab = async function () {
  if (!currentWsId) return;
  const container = document.getElementById('sprints-container');
  container.innerHTML = '<div class="skeleton card" style="height:100px;margin-bottom:12px;"></div>';

  try {
    const [sprintsRes, backlogRes] = await Promise.all([
      api(endpoints.sprints(currentWsId)),
      api(endpoints.backlog(currentWsId))
    ]);
    workspaceSprints = sprintsRes?.data || [];
    const backlogData = backlogRes?.data || {};
    const backlogTasks = backlogData.tasks?.content || backlogData.tasks || [];

    container.innerHTML = '';

    // Render Sprints
    if (!workspaceSprints.length) {
      const emptyCard = document.createElement('div');
      emptyCard.className = 'section-card';
      emptyCard.innerHTML = `<div class="hint">Hali sprintlar yaratilmagan. "+ Yangi Sprint" tugmasi orqali birinchi sprintni boshlang.</div>`;
      container.appendChild(emptyCard);
    } else {
      workspaceSprints.forEach(s => {
        const sc = document.createElement('div');
        sc.className = 'section-card';
        sc.innerHTML = `
          <div class="section-card-header">
            <div class="section-card-title">
              <span>🏃‍♂️</span>
              <span class="s-name"></span>
              <span class="status-pill ${s.status}">${s.status}</span>
            </div>
            <div style="display:flex;gap:6px;flex-wrap:wrap;" class="s-actions"></div>
          </div>
          <p class="hint s-goal" style="margin-bottom:10px;"></p>
          <div style="display:flex;gap:14px;font-size:12.5px;color:var(--hint-color);">
            <span>📋 Vazifalar: <strong>${s.taskCount || 0}</strong></span>
            <span>⚡ Story Points: <strong>${s.totalStoryPoints || 0} SP</strong></span>
            <span>📅 ${s.startDate || '—'} → ${s.endDate || '—'}</span>
          </div>
        `;
        sc.querySelector('.s-name').textContent = s.name;
        sc.querySelector('.s-goal').textContent = s.goal || 'Maqsad kiritilmagan';

        const acts = sc.querySelector('.s-actions');
        if (s.status === 'PLANNED') {
          const btnStart = document.createElement('button');
          btnStart.className = 'btn-chip-action primary';
          btnStart.textContent = '▶ Sprintni boshlash';
          btnStart.onclick = async () => {
            await api(`/api/workspaces/${currentWsId}/sprints/${s.id}/start`, { method: 'POST' });
            showToast('Sprint boshlandi!');
            loadSprintsTab();
            loadBoard(currentWsId);
          };
          acts.appendChild(btnStart);
        }
        if (s.status === 'ACTIVE') {
          const btnComp = document.createElement('button');
          btnComp.className = 'btn-chip-action primary';
          btnComp.textContent = '✓ Sprintni yakunlash';
          btnComp.onclick = async () => {
            await api(`/api/workspaces/${currentWsId}/sprints/${s.id}/complete`, {
              method: 'POST',
              body: JSON.stringify({})
            });
            showToast('Sprint yakunlandi!');
            loadSprintsTab();
            loadBoard(currentWsId);
          };
          acts.appendChild(btnComp);
        }

        const btnViewBoard = document.createElement('button');
        btnViewBoard.className = 'btn-chip-action';
        btnViewBoard.textContent = "Doskada ko'rish";
        btnViewBoard.onclick = () => {
          selectedSprintFilter = s.id;
          const sprintSel = document.getElementById('board-sprint-select');
          if (sprintSel) sprintSel.value = s.id;
          switchTab('board');
          applyFilters();
        };
        acts.appendChild(btnViewBoard);

        const btnDel = document.createElement('button');
        btnDel.className = 'btn-chip-action';
        btnDel.style.color = 'var(--danger)';
        btnDel.textContent = "O'chirish";
        btnDel.onclick = () => {
          tgConfirm("Sprintni o'chirasizmi? (Vazifalar backlogga qaytadi)", async (ok) => {
            if (!ok) return;
            await api(`/api/workspaces/${currentWsId}/sprints/${s.id}`, { method: 'DELETE' });
            loadSprintsTab();
            loadBoard(currentWsId);
          });
        };
        acts.appendChild(btnDel);

        container.appendChild(sc);
      });
    }

    // Render Backlog Section
    const backlogCard = document.createElement('div');
    backlogCard.className = 'section-card';
    backlogCard.innerHTML = `
      <div class="section-card-header">
        <div class="section-card-title">📦 Backlog (${backlogTasks.length} ta vazifa)</div>
        <button type="button" class="btn-chip-action primary" id="btn-backlog-new">+ Backlogga vazifa</button>
      </div>
      <div class="backlog-list"></div>
    `;
    backlogCard.querySelector('#btn-backlog-new').onclick = () => openSheet('create');
    const bList = backlogCard.querySelector('.backlog-list');

    const openSprints = workspaceSprints.filter(s => s.status !== 'COMPLETED');
    if (!backlogTasks.length) {
      bList.innerHTML = `<div class="hint">Backlog bo'sh. Barcha vazifalar sprintlarga taqsimlangan.</div>`;
    } else {
      backlogTasks.forEach(t => {
        const row = document.createElement('div');
        row.className = 'checklist-item';
        row.style.justifyContent = 'space-between';
        row.innerHTML = `
          <div style="min-width:0;flex:1;cursor:pointer;" class="b-info">
            <span class="task-key-badge" style="margin-right:6px;">${t.publicId || 'TASK'}</span>
            <strong class="b-title"></strong>
          </div>
          <div style="display:flex;gap:6px;align-items:center;" class="b-move"></div>
        `;
        row.querySelector('.b-title').textContent = t.title;
        row.querySelector('.b-info').onclick = () => openSheet('view', t.id);

        if (openSprints.length) {
          const sel = document.createElement('select');
          sel.className = 'compact-select';
          sel.innerHTML = `<option value="">Sprintga biriktirish...</option>`;
          openSprints.forEach(sp => {
            const opt = document.createElement('option');
            opt.value = sp.id;
            opt.textContent = sp.name;
            sel.appendChild(opt);
          });
          sel.onchange = async () => {
            if (!sel.value) return;
            await api(`/api/workspaces/${currentWsId}/sprints/${sel.value}/tasks`, {
              method: 'POST',
              body: JSON.stringify({ taskIds: [t.id] })
            });
            showToast("Vazifa sprintga qo'shildi");
            loadSprintsTab();
            loadBoard(currentWsId);
          };
          row.querySelector('.b-move').appendChild(sel);
        }
        bList.appendChild(row);
      });
    }
    container.appendChild(backlogCard);
  } catch (e) {
    container.innerHTML = `<div class="empty-placeholder">Sprintlarni yuklashda xatolik: ${e.message}</div>`;
  }
};

window.openSprintModal = function () {
  openCustomModal("Yangi Sprint yaratish", (body) => {
    body.innerHTML = `
      <div class="form-group">
        <label>Sprint nomi *</label>
        <input type="text" id="sp-name" placeholder="Masalan: Sprint 14 — Backend Optimization">
      </div>
      <div class="form-group">
        <label>Sprint maqsadi (Goal)</label>
        <textarea id="sp-goal" rows="2" placeholder="Ushbu sprintda nimaga erishamiz?"></textarea>
      </div>
      <div class="form-row">
        <div class="form-group">
          <label>Boshlanish sanasi</label>
          <input type="date" id="sp-start">
        </div>
        <div class="form-group">
          <label>Tugash sanasi</label>
          <input type="date" id="sp-end">
        </div>
      </div>
      <div class="detail-actions">
        <button type="button" class="btn-action btn-soft" data-action="close-sheet">Bekor qilish</button>
        <button type="button" class="btn-action btn-primary" id="sp-save">Yaratish</button>
      </div>
    `;
    body.querySelector('#sp-save').onclick = async () => {
      const name = body.querySelector('#sp-name').value.trim();
      const goal = body.querySelector('#sp-goal').value.trim();
      const startDate = body.querySelector('#sp-start').value || null;
      const endDate = body.querySelector('#sp-end').value || null;
      if (!name) return tgAlert("Sprint nomini kiriting");
      try {
        await api(endpoints.sprints(currentWsId), {
          method: 'POST',
          body: JSON.stringify({ name, goal, startDate, endDate })
        });
        closeSheet();
        showToast("Sprint yaratildi");
        loadSprintsTab();
        loadBoard(currentWsId);
      } catch (e) {}
    };
  });
};

// ===== TAB 3: MY TASKS =====
window.loadMyTasks = async function () {
  const container = document.getElementById('mytasks-container');
  container.innerHTML = '<div class="skeleton card" style="height:65px;margin-bottom:10px;"></div>';

  try {
    const res = await api('/api/users/me/tasks?size=100');
    const tasks = res?.data?.content || res?.data || [];

    const myTasks = tasks.map(t => {
      const ws = allWorkspaces.find(w => w.id === t.workspaceId);
      return { ...t, wsTitle: ws ? (ws.title || ws.name) : "Loyiha" };
    });

    if (!myTasks.length) {
      container.innerHTML = `<div class="empty-placeholder"><div style="font-size:36px;margin-bottom:10px;">☕</div>Sizga biriktirilgan faol vazifalar yo'q.</div>`;
      return;
    }

    const groups = { overdue: [], today: [], thisWeek: [], later: [], noDate: [] };
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const nextWeek = new Date(today);
    nextWeek.setDate(today.getDate() + 7);

    myTasks.forEach(t => {
      if (t.isOverdue) { groups.overdue.push(t); return; }
      if (!t.dueDate) { groups.noDate.push(t); return; }
      const due = new Date(t.dueDate);
      if (due < today) groups.overdue.push(t);
      else if (due.getTime() === today.getTime()) groups.today.push(t);
      else if (due < nextWeek) groups.thisWeek.push(t);
      else groups.later.push(t);
    });

    container.innerHTML = '';
    const renderGroup = (list, title, icon) => {
      if (!list.length) return;
      const g = document.createElement('div');
      g.className = 'section-card';
      g.innerHTML = `<div class="section-card-title" style="margin-bottom:10px;">${icon} <span class="g-t"></span> (${list.length})</div>`;
      g.querySelector('.g-t').textContent = title;
      list.forEach(t => {
        const card = createCard(t);
        const wsChip = document.createElement('div');
        wsChip.style = 'font-size:11px;color:var(--hint-color);margin-bottom:4px;font-weight:600;';
        wsChip.textContent = '📁 ' + t.wsTitle;
        card.insertBefore(wsChip, card.firstChild);
        card.onclick = () => {
          if (currentWsId !== t.workspaceId) {
            selectWorkspace(t.workspaceId);
            setTimeout(() => openSheet('view', t.id), 350);
          } else {
            openSheet('view', t.id);
          }
        };
        g.appendChild(card);
      });
      container.appendChild(g);
    };

    renderGroup(groups.overdue, "Muddati o'tgan", "🔴");
    renderGroup(groups.today, "Bugun bajarilishi kerak", "📅");
    renderGroup(groups.thisWeek, "Shu haftada", "🟡");
    renderGroup(groups.later, "Keyinroq", "🟢");
    renderGroup(groups.noDate, "Muddatsiz vazifalar", "⚪");
  } catch (e) {
    container.innerHTML = `<div class="empty-placeholder">Xatolik: ${e.message}</div>`;
  }
};

// ===== TAB 4: REAL-TIME TEAM CHAT =====
window.switchChatChannel = function (channel) {
  chatChannel = channel === 'direct' ? 'direct' : 'public';
  document.querySelectorAll('[data-action="chat-channel"]').forEach(btn => {
    btn.classList.toggle('active', btn.dataset.channel === chatChannel);
  });
  if (chatChannel === 'public') {
    activeChatPartner = null;
    document.getElementById('chat-room-title').textContent = 'Umumiy Jamoa Chati';
    document.getElementById('chat-room-subtitle').textContent = "Barcha jamoa a'zolari uchun ochiq kanal";
  }
  loadChatTab();
};

window.loadChatTab = async function () {
  const usersListEl = document.getElementById('chat-users-list');
  const msgBox = document.getElementById('chat-messages-box');
  if (!usersListEl || !msgBox) return;

  try {
    const [usersRes, convsRes] = await Promise.all([
      api('/api/chat/users').catch(() => ({ data: [] })),
      api('/api/chat/conversations').catch(() => ({ data: [] }))
    ]);
    chatUsers = usersRes?.data || [];
    const convs = convsRes?.data || [];
    const unreadMap = {};
    convs.forEach(c => {
      if (c.user?.id) unreadMap[c.user.id] = c.unreadCount || 0;
    });

    usersListEl.innerHTML = '';
    chatUsers.forEach(u => {
      const item = document.createElement('div');
      item.className = 'chat-user-item' + (activeChatPartner && activeChatPartner.id === u.id ? ' active' : '');
      const unread = unreadMap[u.id] || 0;
      item.innerHTML = `
        <div class="assignee-avatar" style="margin-left:0;">${getInitials(u.fullName || u.name)}</div>
        <div style="min-width:0;flex:1;">
          <div class="u-name" style="font-size:13px;"></div>
          <div class="hint" style="font-size:11px;display:flex;align-items:center;gap:4px;">
            <span class="online-dot ${u.online ? 'online' : ''}"></span>
            <span>${u.online ? 'Online' : 'Offline'}</span>
          </div>
        </div>
        ${unread > 0 ? `<span class="badge-pill">${unread}</span>` : ''}
      `;
      item.querySelector('.u-name').textContent = u.fullName || u.name;
      item.onclick = () => {
        activeChatPartner = u;
        chatChannel = 'direct';
        document.querySelectorAll('[data-action="chat-channel"]').forEach(btn => {
          btn.classList.toggle('active', btn.dataset.channel === 'direct');
        });
        document.getElementById('chat-room-title').textContent = u.fullName || u.name;
        document.getElementById('chat-room-subtitle').textContent = '@' + u.name + (u.online ? ' • Online' : '');
        api(`/api/chat/direct/${u.id}/read-all`, { method: 'POST' }).catch(() => {});
        loadChatMessages();
        loadChatTab();
      };
      usersListEl.appendChild(item);
    });

    await loadChatMessages();
  } catch (e) {}
};

async function loadChatMessages() {
  const msgBox = document.getElementById('chat-messages-box');
  if (!msgBox) return;

  if (chatChannel === 'direct' && !activeChatPartner) {
    msgBox.innerHTML = `<div class="empty-placeholder">Chap ro'yxatdan suhbatdoshni tanlang</div>`;
    return;
  }

  try {
    const url = chatChannel === 'public'
      ? '/api/chat/public?size=40'
      : `/api/chat/direct/${activeChatPartner.id}?size=40`;
    const res = await api(url);
    const messages = (res?.data?.content || res?.data || []).slice().reverse();

    msgBox.innerHTML = '';
    if (!messages.length) {
      msgBox.innerHTML = `<div class="empty-placeholder">Hali xabarlar yo'q. Birinchi bo'lib yozing!</div>`;
      return;
    }

    messages.forEach(m => {
      const isMine = m.senderId === currentUserId;
      const bubble = document.createElement('div');
      bubble.className = 'chat-bubble' + (isMine ? ' mine' : '');

      const timeStr = m.createdAt ? new Date(m.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : '';
      bubble.innerHTML = `
        <div class="chat-bubble-header">
          <span class="cb-author"></span>
          <span>${timeStr}</span>
        </div>
        <div class="cb-reply-slot"></div>
        <div class="cb-content" style="white-space:pre-wrap;word-break:break-word;"></div>
        <div class="chat-reactions-bar"></div>
      `;
      bubble.querySelector('.cb-author').textContent = isMine ? 'Siz' : (m.senderFullName || m.senderName || 'Foydalanuvchi');
      bubble.querySelector('.cb-content').textContent = m.content;

      if (m.replyTo) {
        const rq = document.createElement('div');
        rq.className = 'chat-reply-quote';
        rq.textContent = `${m.replyTo.senderName || ''}: ${m.replyTo.content || ''}`;
        bubble.querySelector('.cb-reply-slot').appendChild(rq);
      }

      const rBar = bubble.querySelector('.chat-reactions-bar');
      ['👍', '❤️', '🔥'].forEach(emoji => {
        const count = (m.reactions || []).filter(r => r.emoji === emoji).length;
        const btn = document.createElement('button');
        btn.className = 'react-btn';
        btn.textContent = `${emoji}${count ? ' ' + count : ''}`;
        btn.onclick = async (e) => {
          e.stopPropagation();
          await api(`/api/chat/${m.id}/reactions`, {
            method: 'POST',
            body: JSON.stringify({ emoji })
          });
          loadChatMessages();
        };
        rBar.appendChild(btn);
      });

      const replyBtn = document.createElement('button');
      replyBtn.className = 'react-btn';
      replyBtn.textContent = '↩ Javob';
      replyBtn.onclick = () => {
        chatReplyToId = m.id;
        document.getElementById('chat-reply-banner').style.display = 'flex';
        document.getElementById('chat-reply-text').textContent = `Javob: ${m.content.substring(0, 50)}`;
        document.getElementById('chat-msg-input').focus();
      };
      rBar.appendChild(replyBtn);

      if (isMine) {
        const delBtn = document.createElement('button');
        delBtn.className = 'react-btn';
        delBtn.style.color = 'var(--danger)';
        delBtn.textContent = '🗑';
        delBtn.onclick = async () => {
          await api(`/api/chat/messages/${m.id}`, { method: 'DELETE' });
          loadChatMessages();
        };
        rBar.appendChild(delBtn);
      }

      msgBox.appendChild(bubble);
    });
    msgBox.scrollTop = msgBox.scrollHeight;
  } catch (e) {}
}

window.cancelChatReply = function () {
  chatReplyToId = null;
  const banner = document.getElementById('chat-reply-banner');
  if (banner) banner.style.display = 'none';
};

window.sendChatMessage = async function () {
  const inp = document.getElementById('chat-msg-input');
  const content = inp.value.trim();
  if (!content) return;
  if (chatChannel === 'direct' && !activeChatPartner) {
    return tgAlert("Avval suhbatdoshni tanlang");
  }
  inp.disabled = true;
  try {
    if (chatChannel === 'public') {
      await api('/api/chat/public', {
        method: 'POST',
        body: JSON.stringify({ content, replyToId: chatReplyToId })
      });
    } else {
      await api('/api/chat/direct', {
        method: 'POST',
        body: JSON.stringify({ recipientId: activeChatPartner.id, content, replyToId: chatReplyToId })
      });
    }
    inp.value = '';
    cancelChatReply();
    await loadChatMessages();
  } catch (e) {}
  inp.disabled = false;
  inp.focus();
};

// ===== TAB 5: ANALYTICS, VELOCITY & CSV EXPORT =====
window.loadStats = async function () {
  const wsId = document.getElementById('stats-ws')?.value || currentWsId;
  if (!wsId) return;
  const container = document.getElementById('stats-container');
  container.innerHTML = '<div class="skeleton card" style="height:120px;margin-bottom:12px;"></div><div class="skeleton card" style="height:180px;"></div>';

  try {
    const [reportRes, colsRes, velocityRes, activitiesRes] = await Promise.all([
      api(`/api/workspaces/${wsId}/reports/summary`).catch(() => null),
      api(endpoints.columns(wsId)),
      api(`/api/workspaces/${wsId}/sprints/velocity`).catch(() => null),
      api(`/api/workspaces/${wsId}/activities?size=8`).catch(() => null)
    ]);

    const cols = colsRes?.data || [];
    let allTasks = [];
    cols.forEach(c => { if (c.tasks) allTasks.push(...c.tasks.filter(t => !t.isArchived)); });

    const rep = reportRes?.data;
    const total = rep ? rep.totalTasks : allTasks.length;
    const doneColIds = new Set(cols.filter(c => c.isDone || /bajarildi|tugadi|done|ko'z tegmasin/i.test(c.title || '')).map(c => c.id));
    const firstColId = cols[0] ? cols[0].id : null;

    const completed = rep ? rep.doneTasks : allTasks.filter(t => doneColIds.has(t.columnId)).length;
    const notStarted = allTasks.filter(t => firstColId && t.columnId === firstColId && !doneColIds.has(t.columnId)).length;
    const inProgress = Math.max(0, total - completed - notStarted);
    const overdue = allTasks.filter(t => t.isOverdue && !doneColIds.has(t.columnId)).length;
    const pct = rep ? Math.round(rep.completionPercentage || 0) : (total ? Math.round((completed / total) * 100) : 0);
    const totalSp = allTasks.reduce((s, t) => s + (t.storyPoints || 0), 0);
    const totalHours = Math.round(allTasks.reduce((s, t) => s + (t.loggedHours || 0), 0) * 10) / 10;

    container.innerHTML = `
      <div class="stats-grid">
        <div class="stat-card">
          <div class="stat-num" data-count="${total}">${total}</div>
          <div class="stat-label">Jami vazifalar</div>
        </div>
        <div class="stat-card">
          <div class="stat-num" style="color:var(--ok);">${completed} <span style="font-size:15px">(${pct}%)</span></div>
          <div class="stat-label">Bajarilgan vazifalar</div>
        </div>
        <div class="stat-card">
          <div class="stat-num" style="color:var(--btn-color);">${inProgress}</div>
          <div class="stat-label">Jarayonda (Faol)</div>
        </div>
        <div class="stat-card">
          <div class="stat-num" style="color:var(--gold-deep);">${notStarted}</div>
          <div class="stat-label">Boshlanmagan (Rejada)</div>
        </div>
        <div class="stat-card">
          <div class="stat-num" style="color:var(--terracotta);">${overdue}</div>
          <div class="stat-label">Muddati o‘tgan</div>
        </div>
        <div class="stat-card">
          <div class="stat-num">${totalSp} SP <span style="font-size:14px;color:var(--hint-color)">• ${totalHours}s</span></div>
          <div class="stat-label">Story Points va Ish vaqti</div>
        </div>
      </div>
      <div class="analytics-two-col" id="analytics-top-charts"></div>
    `;

    const topCharts = container.querySelector('#analytics-top-charts');

    // 1. SVG Donut Chart: Vazifalar holati bo'yicha ulush
    const donutCard = document.createElement('div');
    donutCard.className = 'chart-section';
    donutCard.style.marginBottom = '0';
    const r = 54;
    const circum = 2 * Math.PI * r;
    const safeTotal = Math.max(total, 1);
    const segDone = (completed / safeTotal) * circum;
    const segProg = (inProgress / safeTotal) * circum;
    const segTodo = (notStarted / safeTotal) * circum;

    donutCard.innerHTML = `
      <div class="chart-title"><span>Vazifalar holati ulushi</span><span class="points-badge">${pct}% Bajarildi</span></div>
      <div class="donut-layout">
        <svg class="donut-svg" viewBox="0 0 140 140">
          <circle cx="70" cy="70" r="${r}" fill="none" stroke="var(--sec-bg-color)" stroke-width="16"/>
          <circle cx="70" cy="70" r="${r}" fill="none" stroke="var(--ok)" stroke-width="16"
            stroke-dasharray="${segDone} ${circum}" stroke-dashoffset="0" transform="rotate(-90 70 70)"/>
          <circle cx="70" cy="70" r="${r}" fill="none" stroke="var(--btn-color)" stroke-width="16"
            stroke-dasharray="${segProg} ${circum}" stroke-dashoffset="${-segDone}" transform="rotate(-90 70 70)"/>
          <circle cx="70" cy="70" r="${r}" fill="none" stroke="var(--gold)" stroke-width="16"
            stroke-dasharray="${segTodo} ${circum}" stroke-dashoffset="${-(segDone + segProg)}" transform="rotate(-90 70 70)"/>
          <text x="70" y="66" text-anchor="middle" fill="currentColor" font-size="22" font-weight="800">${pct}%</text>
          <text x="70" y="84" text-anchor="middle" fill="var(--hint-color)" font-size="10" font-weight="700">SAMARADORLIK</text>
        </svg>
        <div class="donut-legend">
          <div class="legend-row"><span class="legend-left"><span class="legend-swatch" style="background:var(--ok)"></span>Bajarildi</span><strong>${completed}</strong></div>
          <div class="legend-row"><span class="legend-left"><span class="legend-swatch" style="background:var(--btn-color)"></span>Jarayonda</span><strong>${inProgress}</strong></div>
          <div class="legend-row"><span class="legend-left"><span class="legend-swatch" style="background:var(--gold)"></span>Boshlanmagan</span><strong>${notStarted}</strong></div>
          <div class="legend-row"><span class="legend-left"><span class="legend-swatch" style="background:var(--terracotta)"></span>Muddati o‘tgan</span><strong>${overdue}</strong></div>
        </div>
      </div>
    `;
    topCharts.appendChild(donutCard);

    // 2. Priority Breakdown Horizontal Bars
    const prioCounts = { URGENT: 0, HIGH: 0, MEDIUM: 0, LOW: 0 };
    allTasks.forEach(t => {
      const p = t.priority || 'MEDIUM';
      prioCounts[p] = (prioCounts[p] || 0) + 1;
    });
    const prioCard = document.createElement('div');
    prioCard.className = 'chart-section';
    prioCard.style.marginBottom = '0';
    prioCard.innerHTML = `<div class="chart-title"><span>Ustuvorlik bo‘yicha taqsimot</span></div><div class="horiz-bar-list"></div>`;
    const pList = prioCard.querySelector('.horiz-bar-list');
    [
      { key: 'URGENT', label: '🔥 Shoshilinch (Urgent)', color: 'var(--terracotta)' },
      { key: 'HIGH', label: '🔴 Yuqori (High)', color: '#DC2626' },
      { key: 'MEDIUM', label: '🟡 O‘rtacha (Medium)', color: 'var(--gold-deep)' },
      { key: 'LOW', label: '🟢 Past (Low)', color: 'var(--ok)' }
    ].forEach(item => {
      const cnt = prioCounts[item.key] || 0;
      const w = total > 0 ? Math.round((cnt / total) * 100) : 0;
      const row = document.createElement('div');
      row.className = 'horiz-bar-item';
      row.innerHTML = `
        <div class="horiz-bar-top"><span>${item.label}</span><span>${cnt} ta (${w}%)</span></div>
        <div class="horiz-bar-track"><div class="horiz-bar-fill" style="width:${w}%;background:${item.color}"></div></div>
      `;
      pList.appendChild(row);
    });
    topCharts.appendChild(prioCard);

    // 3. Column distribution chart
    const chartSection = document.createElement('div');
    chartSection.className = 'chart-section';
    chartSection.innerHTML = `<div class="chart-title"><span>Ustunlar bo‘yicha yuklama va WIP holati</span></div><div class="bar-chart"></div>`;
    const barChart = chartSection.querySelector('.bar-chart');
    const maxTasks = Math.max(...cols.map(c => c.tasks ? c.tasks.length : 0), 1);

    cols.forEach(c => {
      const count = c.tasks ? c.tasks.filter(t => !t.isArchived).length : 0;
      const h = Math.max((count / maxTasks) * 100, 6);
      const isOverWip = c.wipLimit && c.wipLimit > 0 && count > c.wipLimit;
      const barCol = document.createElement('div');
      barCol.className = 'bar-col';
      barCol.innerHTML = `
        <div class="bar-val" style="${isOverWip ? 'color:var(--terracotta)' : ''}">${c.wipLimit ? `${count}/${c.wipLimit}` : count}</div>
        <div class="bar" style="height:${h}%;background:${isOverWip ? 'var(--terracotta)' : getColColor(c.title)};"></div>
        <div class="bar-label"></div>
      `;
      barCol.querySelector('.bar-label').textContent = c.title;
      barChart.appendChild(barCol);
    });
    container.appendChild(chartSection);

    // 4. Sprint Velocity chart
    const velocities = velocityRes?.data?.sprints || [];
    if (velocities.length > 0) {
      const velSec = document.createElement('div');
      velSec.className = 'chart-section';
      velSec.innerHTML = `<div class="chart-title"><span>Sprint Velocity (Yopilgan Story Pointlar)</span></div><div class="bar-chart"></div>`;
      const vChart = velSec.querySelector('.bar-chart');
      const maxSp = Math.max(...velocities.map(v => v.completedStoryPoints || 0), 1);
      velocities.forEach(v => {
        const sp = v.completedStoryPoints || 0;
        const h = Math.max((sp / maxSp) * 100, 6);
        const bc = document.createElement('div');
        bc.className = 'bar-col';
        bc.innerHTML = `
          <div class="bar-val">${sp} SP</div>
          <div class="bar" style="height:${h}%;background:var(--ok);"></div>
          <div class="bar-label"></div>
        `;
        bc.querySelector('.bar-label').textContent = v.sprintName;
        vChart.appendChild(bc);
      });
      container.appendChild(velSec);
    }

    // 5. Workload List
    const workloadSection = document.createElement('div');
    workloadSection.className = 'chart-section';
    workloadSection.innerHTML = `<div class="chart-title"><span>Jamoa a’zolari samaradorligi</span></div><div class="workload-list"></div>`;
    const workloadList = workloadSection.querySelector('.workload-list');
    const workloads = rep?.workload || [];

    if (workloads.length) {
      workloads.forEach(w => {
        const totW = w.totalAssignedTasks || 0;
        const doneW = w.completedTasks || 0;
        const wPct = totW > 0 ? Math.round((doneW / totW) * 100) : 0;
        const item = document.createElement('div');
        item.className = 'workload-item';
        item.innerHTML = `
          <div class="assignee-avatar" style="margin-left:0;">${getInitials(w.userFullName || w.userName)}</div>
          <div style="flex:1;min-width:0;">
            <div class="wl-name"></div>
            <div class="horiz-bar-track" style="height:6px;margin-top:4px;"><div class="horiz-bar-fill" style="width:${wPct}%;background:var(--ok)"></div></div>
          </div>
          <div class="wl-count">${doneW} / ${totW} (${wPct}%)</div>
        `;
        item.querySelector('.wl-name').textContent = w.userFullName || w.userName;
        workloadList.appendChild(item);
      });
    } else {
      workloadList.innerHTML = '<div class="hint">Hozircha a’zolarga vazifalar biriktirilmagan</div>';
    }
    container.appendChild(workloadSection);

    // 6. Recent Workspace Activity Feed
    const acts = activitiesRes?.data?.content || activitiesRes?.data || [];
    if (acts.length > 0) {
      const actSec = document.createElement('div');
      actSec.className = 'chart-section';
      actSec.innerHTML = `<div class="chart-title"><span>So‘nggi harakatlar tarixi (Audit Trail)</span></div><div class="workload-list"></div>`;
      const aList = actSec.querySelector('.workload-list');
      acts.slice(0, 8).forEach(a => {
        const row = document.createElement('div');
        row.className = 'workload-item';
        row.innerHTML = `
          <div class="assignee-avatar" style="margin-left:0;">${getInitials(a.userFullName || a.userName || 'T')}</div>
          <div style="flex:1;min-width:0;font-size:13px;">
            <strong>${escapeHtmlText(a.userFullName || a.userName || 'Tizim')}</strong>
            <span class="hint"> • ${escapeHtmlText(a.actionType || '')}</span>
            <div class="hint" style="font-size:11.5px;">${escapeHtmlText(a.oldValue || '—')} → ${escapeHtmlText(a.newValue || '—')}</div>
          </div>
        `;
        aList.appendChild(row);
      });
      container.appendChild(actSec);
    }
  } catch (e) {
    container.innerHTML = `<div class="empty-placeholder">Hisobotni yuklashda xatolik yuz berdi</div>`;
  }
};

window.exportWorkspaceCsv = async function () {
  const wsId = document.getElementById('stats-ws')?.value || currentWsId;
  if (!wsId) return;
  try {
    const csvText = await api(`/api/workspaces/${wsId}/export/csv`);
    const blob = new Blob([csvText], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `workspace_${wsId}_tasks.csv`;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
    showToast("CSV fayl yuklab olindi!");
  } catch (e) {}
};

// ===== TAB 6: WORKSPACE SETTINGS, TEAM, INVITES, LABELS, DIRECTIONS & PROFILE =====
window.loadWorkspaceSettingsTab = async function () {
  const container = document.getElementById('workspace-settings-container');
  if (!container) return;
  container.innerHTML = '<div class="skeleton card" style="height:120px;margin-bottom:12px;"></div>';

  try {
    const [meRes, myInvitesRes, membersRes, wsInvitesRes, labelsRes, dirsRes] = await Promise.all([
      api('/api/me'),
      api('/api/invitations/me').catch(() => ({ data: [] })),
      currentWsId ? api(endpoints.members(currentWsId)).catch(() => ({ data: [] })) : Promise.resolve({ data: [] }),
      currentWsId ? api(`/api/workspaces/${currentWsId}/invites`).catch(() => ({ data: [] })) : Promise.resolve({ data: [] }),
      currentWsId ? api(endpoints.labels(currentWsId)).catch(() => ({ data: [] })) : Promise.resolve({ data: [] }),
      currentWsId ? api(endpoints.directions(currentWsId)).catch(() => ({ data: [] })) : Promise.resolve({ data: [] })
    ]);

    currentUser = meRes?.data || currentUser;
    updateSidebarUser();
    const myInvites = myInvitesRes?.data || [];
    const members = membersRes?.data || [];
    const wsInvites = wsInvitesRes?.data || [];
    workspaceLabels = labelsRes?.data || [];
    workspaceDirections = dirsRes?.data || [];

    container.innerHTML = '';

    // 1. Incoming Personal Invitations (if any)
    if (myInvites.length > 0) {
      const myInvCard = document.createElement('div');
      myInvCard.className = 'section-card';
      myInvCard.innerHTML = `<div class="section-card-title" style="margin-bottom:10px;">📨 Sizga kelgan taklifnomalar (${myInvites.length})</div><div class="my-inv-list"></div>`;
      const listEl = myInvCard.querySelector('.my-inv-list');
      myInvites.forEach(inv => {
        const row = document.createElement('div');
        row.className = 'checklist-item';
        row.innerHTML = `
          <div style="flex:1;">
            <strong class="inv-ws"></strong>
            <div class="hint">Taklif qiluvchi: <span class="inv-sender"></span> • Rol: ${inv.role}</div>
          </div>
          <button type="button" class="btn-chip-action primary acc-btn">Qabul qilish</button>
          <button type="button" class="btn-chip-action rej-btn" style="color:var(--danger);">Rad etish</button>
        `;
        row.querySelector('.inv-ws').textContent = inv.workspaceTitle || 'Loyiha';
        row.querySelector('.inv-sender').textContent = inv.senderName || 'Admin';
        row.querySelector('.acc-btn').onclick = async () => {
          await api(`/api/invitations/by-id/${inv.id}/accept`, { method: 'POST' });
          showToast("Taklifnoma qabul qilindi!");
          await loadWorkspacesList(inv.workspaceId);
          loadWorkspaceSettingsTab();
        };
        row.querySelector('.rej-btn').onclick = async () => {
          await api(`/api/invitations/by-id/${inv.id}/reject`, { method: 'POST' });
          showToast("Taklifnoma rad etildi");
          loadWorkspaceSettingsTab();
        };
        listEl.appendChild(row);
      });
      container.appendChild(myInvCard);
    }

    // 2. Team Members & Invite Section
    if (currentWsId) {
      const teamCard = document.createElement('div');
      teamCard.className = 'section-card';
      teamCard.innerHTML = `
        <div class="section-card-header">
          <div class="section-card-title">👥 Loyiha a'zolari va Taklifnomalar</div>
          <div style="display:flex;gap:6px;">
            <button type="button" class="btn-chip-action" id="btn-gen-invite-link">🔗 Havola olish</button>
          </div>
        </div>
        <div class="form-row" style="margin-bottom:14px;">
          <input type="text" id="invite-user-input" placeholder="Login yoki email kiriting (masalan: ali yoki ali@mail.com)" style="flex:2;padding:9px 12px;border-radius:10px;border:1px solid var(--line);background:var(--sec-bg-color);">
          <select id="invite-role-select" class="compact-select">
            <option value="MEMBER" selected>MEMBER</option>
            <option value="ADMIN">ADMIN</option>
            <option value="VIEWER">VIEWER</option>
          </select>
          <button type="button" class="btn-action btn-primary" id="btn-send-invite">Taklif qilish</button>
        </div>
        <div id="invite-link-box" style="display:none;margin-bottom:12px;padding:10px;border-radius:10px;background:var(--accent-soft);font-size:12.5px;"></div>
        <div class="members-list"></div>
      `;

      teamCard.querySelector('#btn-send-invite').onclick = async () => {
        const usernameOrEmail = teamCard.querySelector('#invite-user-input').value.trim();
        const role = teamCard.querySelector('#invite-role-select').value;
        if (!usernameOrEmail) return tgAlert("Login yoki email kiriting");
        try {
          await api(`/api/workspaces/${currentWsId}/invites`, {
            method: 'POST',
            body: JSON.stringify({ usernameOrEmail, role })
          });
          showToast("Taklif yuborildi!");
          loadWorkspaceSettingsTab();
        } catch (e) {}
      };

      teamCard.querySelector('#btn-gen-invite-link').onclick = async () => {
        try {
          const res = await api(`/api/workspaces/${currentWsId}/invites/link/regenerate`, {
            method: 'POST',
            body: JSON.stringify({ role: 'MEMBER', durationDays: 7 })
          });
          const data = res?.data;
          const link = data?.telegramMiniappLink || data?.telegramInviteLink || data?.inviteLink;
          const box = teamCard.querySelector('#invite-link-box');
          box.style.display = 'block';
          box.textContent = `Taklif havolasi (nusxa olindi): ${link}`;
          fallbackCopy(link);
        } catch (e) {}
      };

      const mList = teamCard.querySelector('.members-list');
      members.forEach(m => {
        const row = document.createElement('div');
        row.className = 'checklist-item';
        row.innerHTML = `
          <div class="assignee-avatar" style="margin-left:0;">${getInitials(m.fullName || m.name)}</div>
          <div style="flex:1;">
            <strong class="m-name"></strong>
            <div class="hint">@${m.name} • Rol: ${m.role}</div>
          </div>
        `;
        row.querySelector('.m-name').textContent = m.fullName || m.name;
        if (m.role !== 'OWNER' && m.id !== currentUserId) {
          const rmBtn = document.createElement('button');
          rmBtn.className = 'btn-chip-action';
          rmBtn.style.color = 'var(--danger)';
          rmBtn.textContent = 'Chiqarish';
          rmBtn.onclick = async () => {
            await api(`/api/workspaces/${currentWsId}/members/${m.id}`, { method: 'DELETE' });
            loadWorkspaceSettingsTab();
          };
          row.appendChild(rmBtn);
        }
        mList.appendChild(row);
      });

      wsInvites.forEach(inv => {
        const row = document.createElement('div');
        row.className = 'checklist-item';
        row.innerHTML = `
          <div style="flex:1;">
            <strong>⏳ Kutilayotgan taklif (${inv.type})</strong>
            <div class="hint">${inv.receiverEmail || 'Havola'} • Rol: ${inv.role}</div>
          </div>
          <button type="button" class="btn-chip-action" style="color:var(--danger);">Bekor qilish</button>
        `;
        row.querySelector('button').onclick = async () => {
          await api(`/api/workspaces/${currentWsId}/invites/${inv.id}`, { method: 'DELETE' });
          loadWorkspaceSettingsTab();
        };
        mList.appendChild(row);
      });

      container.appendChild(teamCard);

      // 3. Labels & Directions Management
      const metaCard = document.createElement('div');
      metaCard.className = 'section-card';
      metaCard.innerHTML = `
        <div class="section-card-title" style="margin-bottom:12px;">🏷 Teglar (Labels) va Yo'nalishlar (Directions)</div>
        <div class="form-row" style="margin-bottom:14px;">
          <div>
            <label class="sidebar-label">Yo'nalishlar (Backend, Frontend, QA...)</label>
            <div id="settings-dirs-list" class="badges" style="margin-bottom:8px;"></div>
            <div style="display:flex;gap:6px;">
              <input type="text" id="new-dir-name" placeholder="Yangi yo'nalish nomi..." style="flex:1;padding:7px 10px;border-radius:8px;border:1px solid var(--line);background:var(--sec-bg-color);">
              <button type="button" class="btn-chip-action primary" id="btn-add-dir">+</button>
            </div>
          </div>
          <div>
            <label class="sidebar-label">Teglar (Labels)</label>
            <div id="settings-lbls-list" class="badges" style="margin-bottom:8px;"></div>
            <div style="display:flex;gap:6px;">
              <input type="text" id="new-lbl-name" placeholder="Yangi teg nomi..." style="flex:1;padding:7px 10px;border-radius:8px;border:1px solid var(--line);background:var(--sec-bg-color);">
              <button type="button" class="btn-chip-action primary" id="btn-add-lbl">+</button>
            </div>
          </div>
        </div>
      `;

      const dirsBox = metaCard.querySelector('#settings-dirs-list');
      workspaceDirections.forEach(d => {
        const sp = document.createElement('span');
        sp.className = 'badge direction';
        sp.style.cursor = 'pointer';
        sp.textContent = `${d.name} ×`;
        sp.onclick = async () => {
          await api(`/api/workspaces/${currentWsId}/directions/${d.id}`, { method: 'DELETE' });
          loadWorkspaceSettingsTab();
          loadBoard(currentWsId);
        };
        dirsBox.appendChild(sp);
      });

      const lblsBox = metaCard.querySelector('#settings-lbls-list');
      workspaceLabels.forEach(l => {
        const sp = document.createElement('span');
        sp.className = 'badge';
        sp.style = 'background:var(--sec-bg-color);border:1px solid var(--line);cursor:pointer;';
        sp.textContent = `🏷 ${l.name} ×`;
        sp.onclick = async () => {
          await api(`/api/workspaces/${currentWsId}/labels/${l.id}`, { method: 'DELETE' });
          loadWorkspaceSettingsTab();
          loadBoard(currentWsId);
        };
        lblsBox.appendChild(sp);
      });

      metaCard.querySelector('#btn-add-dir').onclick = async () => {
        const name = metaCard.querySelector('#new-dir-name').value.trim();
        if (!name) return;
        await api(endpoints.directions(currentWsId), { method: 'POST', body: JSON.stringify({ name, color: '#3B82F6' }) });
        loadWorkspaceSettingsTab();
        loadBoard(currentWsId);
      };

      metaCard.querySelector('#btn-add-lbl').onclick = async () => {
        const name = metaCard.querySelector('#new-lbl-name').value.trim();
        if (!name) return;
        await api(endpoints.labels(currentWsId), { method: 'POST', body: JSON.stringify({ name, color: 'BLUE' }) });
        loadWorkspaceSettingsTab();
        loadBoard(currentWsId);
      };

      container.appendChild(metaCard);
    }

    // 4. User Profile, Password & Telegram Bot Link Card
    const profCard = document.createElement('div');
    profCard.className = 'section-card';
    profCard.innerHTML = `
      <div class="section-card-header">
        <div class="section-card-title">👤 Shaxsiy profil va Xavfsizlik</div>
        <button type="button" class="btn-chip-action" style="color:var(--danger);" id="btn-logout-user">Chiqish (Logout)</button>
      </div>
      <div class="form-row">
        <div class="form-group">
          <label>To'liq ism (Full Name)</label>
          <input type="text" id="prof-fullname">
        </div>
        <div class="form-group">
          <label>Login (@username)</label>
          <input type="text" id="prof-name">
        </div>
      </div>
      <button type="button" class="btn-action btn-primary" id="btn-save-profile" style="margin-bottom:16px;">Profilni saqlash</button>

      <div style="padding-top:14px;border-top:1px solid var(--line);display:flex;align-items:center;justify-content:space-between;flex-wrap:wrap;gap:10px;">
        <div>
          <strong>✈️ Telegram Bot ulanishi:</strong>
          <span class="hint">${currentUser?.telegramLinked ? '✅ Ulangan' : '❌ Ulanmagan'}</span>
        </div>
        <div id="tg-link-actions"></div>
      </div>
    `;
    profCard.querySelector('#prof-fullname').value = currentUser?.fullName || '';
    profCard.querySelector('#prof-name').value = currentUser?.name || '';
    profCard.querySelector('#btn-logout-user').onclick = () => window.logout();

    profCard.querySelector('#btn-save-profile').onclick = async () => {
      const fullName = profCard.querySelector('#prof-fullname').value.trim();
      const name = profCard.querySelector('#prof-name').value.trim();
      const res = await api('/api/users/me', {
        method: 'PATCH',
        body: JSON.stringify({ fullName, name })
      });
      currentUser = res?.data || currentUser;
      updateSidebarUser();
      showToast("Profil yangilandi!");
    };

    const tgActs = profCard.querySelector('#tg-link-actions');
    if (currentUser?.telegramLinked) {
      const btnUnlink = document.createElement('button');
      btnUnlink.className = 'btn-chip-action';
      btnUnlink.style.color = 'var(--danger)';
      btnUnlink.textContent = 'Telegramni uzish';
      btnUnlink.onclick = async () => {
        await api('/api/users/me/telegram', { method: 'DELETE' });
        showToast("Telegram hisob uzildi");
        loadWorkspaceSettingsTab();
      };
      tgActs.appendChild(btnUnlink);
    } else {
      const btnLink = document.createElement('button');
      btnLink.className = 'btn-chip-action primary';
      btnLink.textContent = '🔗 Telegram Botga ulash';
      btnLink.onclick = async () => {
        const res = await api('/api/users/me/telegram-link-token');
        const link = res?.data?.link;
        if (link) {
          window.open(link, '_blank');
        } else if (res?.data?.token) {
          fallbackCopy('/start ' + res.data.token);
          showToast("Botga yuborish uchun kod nusxalandi: /start " + res.data.token);
        }
      };
      tgActs.appendChild(btnLink);
    }

    container.appendChild(profCard);
  } catch (e) {
    container.innerHTML = `<div class="empty-placeholder">Yuklashda xatolik: ${e.message}</div>`;
  }
};

// ===== Workspace Create Modal & Notifications Drawer =====
window.openWorkspaceModal = function () {
  openCustomModal("Yangi loyiha (Workspace) yaratish", (body) => {
    body.innerHTML = `
      <div class="form-group">
        <label>Loyiha nomi *</label>
        <input type="text" id="ws-modal-title" placeholder="Masalan: Mobile Banking App">
      </div>
      <div class="form-group">
        <label>Qisqacha tavsif</label>
        <textarea id="ws-modal-desc" rows="2" placeholder="Loyiha maqsadi..."></textarea>
      </div>
      <div class="detail-actions">
        <button type="button" class="btn-action btn-soft" data-action="close-sheet">Bekor qilish</button>
        <button type="button" class="btn-action btn-primary" id="ws-modal-save">Yaratish</button>
      </div>
    `;
    body.querySelector('#ws-modal-save').onclick = async () => {
      const title = body.querySelector('#ws-modal-title').value.trim();
      const description = body.querySelector('#ws-modal-desc').value.trim();
      if (!title) return tgAlert("Loyiha nomini kiriting");
      try {
        const res = await api(endpoints.workspaces, {
          method: 'POST',
          body: JSON.stringify({ title, description, bgColor: '#4f46e5', initDefaultColumns: true })
        });
        closeSheet();
        showToast("Yangi loyiha yaratildi!");
        await loadWorkspacesList(res?.data?.id);
      } catch (e) {}
    };
  });
};

async function refreshNotificationsBadge() {
  try {
    const res = await api('/api/notifications?size=30');
    const items = res?.data?.content || res?.data || [];
    const unread = items.filter(n => !n.read).length;
    const badge = document.getElementById('notif-badge');
    if (badge) {
      badge.style.display = unread > 0 ? 'grid' : 'none';
      badge.textContent = unread > 99 ? '99+' : unread;
    }
  } catch (e) {}
}

window.openNotificationsModal = async function () {
  openCustomModal("Bildirishnomalar", async (body) => {
    body.innerHTML = '<div class="hint">Yuklanmoqda...</div>';
    try {
      const res = await api('/api/notifications?size=30');
      const items = res?.data?.content || res?.data || [];
      body.innerHTML = `
        <div style="display:flex;justify-content:flex-end;margin-bottom:10px;">
          <button type="button" class="btn-chip-action" id="btn-read-all-notifs">✓ Barchasini o'qilgan qilish</button>
        </div>
        <div id="notifs-list-box" style="max-height:360px;overflow-y:auto;"></div>
      `;
      body.querySelector('#btn-read-all-notifs').onclick = async () => {
        await api('/api/notifications/read-all', { method: 'PUT' });
        refreshNotificationsBadge();
        openNotificationsModal();
      };
      const listBox = body.querySelector('#notifs-list-box');
      if (!items.length) {
        listBox.innerHTML = '<div class="empty-placeholder">Bildirishnomalar yo\'q</div>';
        return;
      }
      items.forEach(n => {
        const row = document.createElement('div');
        row.className = 'checklist-item';
        row.style.opacity = n.read ? '0.65' : '1';
        row.innerHTML = `
          <div style="flex:1;">
            <strong class="n-title"></strong>
            <div class="hint n-msg"></div>
          </div>
        `;
        row.querySelector('.n-title').textContent = n.title || 'Xabar';
        row.querySelector('.n-msg').textContent = n.message || '';
        row.onclick = async () => {
          if (!n.read) {
            await api(`/api/notifications/${n.id}/read`, { method: 'PUT' });
            refreshNotificationsBadge();
          }
          if (n.referenceId && tasksData.some(t => t.id === n.referenceId)) {
            openSheet('view', n.referenceId);
          }
        };
        listBox.appendChild(row);
      });
    } catch (e) {
      body.innerHTML = '<div class="hint">Xatolik yuz berdi</div>';
    }
  });
};

// ===== Share & Clipboard =====
window.shareTask = function () {
  if (!currentTaskId) return;
  const url = botUsername
    ? `https://t.me/${botUsername}?start=task_${currentTaskId}`
    : `${location.origin}/app/?task=${currentTaskId}`;
  try {
    if (isTma && tg.openTelegramLink && botUsername) {
      const shareUrl = `https://t.me/share/url?url=${encodeURIComponent(url)}&text=Vazifa`;
      tg.openTelegramLink(shareUrl);
    } else {
      fallbackCopy(url);
    }
  } catch (e) {
    fallbackCopy(url);
  }
};

function fallbackCopy(text) {
  if (navigator.clipboard && navigator.clipboard.writeText) {
    navigator.clipboard.writeText(text).then(() => showToast('Havola nusxalandi!')).catch(() => {});
    return;
  }
  const el = document.createElement('textarea');
  el.value = text;
  document.body.appendChild(el);
  el.select();
  try {
    document.execCommand('copy');
    showToast('Havola nusxalandi!');
  } catch (e) {}
  document.body.removeChild(el);
}

// ===== Real-Time WebSocket Client (STOMP 1.2) =====
let ws = null;
let wsSubscribedWsId = null;
let wsHeartbeatTimer = null;
let wsReconnectTimer = null;
let wsReconnectAttempts = 0;
let wsIntentionallyClosed = false;
let wsAuthFailed = false;

function isTokenExpiredOrNear(tokenStr) {
  if (!tokenStr) return true;
  try {
    const payload = JSON.parse(atob(tokenStr.split('.')[1]));
    if (!payload.exp) return false;
    return (payload.exp * 1000 - Date.now()) < 60000;
  } catch (e) {
    return true;
  }
}

function disconnectWebSocket() {
  wsIntentionallyClosed = true;
  stopWsHeartbeat();
  if (wsReconnectTimer) {
    clearTimeout(wsReconnectTimer);
    wsReconnectTimer = null;
  }
  if (ws) {
    try { ws.close(); } catch (e) {}
    ws = null;
  }
}

window.logout = function () {
  disconnectWebSocket();
  try {
    localStorage.removeItem('tc_token');
    localStorage.removeItem('tc_refresh_token');
  } catch (e) {}
  token = null;
  refreshToken = null;
  closeSheet();
  showAuthScreen();
};

function updateLiveIndicator(status) {
  const el = document.getElementById('live-indicator');
  if (!el) return;
  el.className = 'live-indicator ' + status;
  const txt = el.querySelector('.live-text');
  if (txt) {
    if (status === 'connected') txt.textContent = 'Jonli';
    else if (status === 'connecting') txt.textContent = 'Ulanmoqda';
    else txt.textContent = 'Oflayn';
  }
}

function initWebSocket() {
  if (!token) return;
  wsIntentionallyClosed = false;
  if (ws && (ws.readyState === WebSocket.OPEN || ws.readyState === WebSocket.CONNECTING)) {
    return;
  }

  const proto = location.protocol === 'https:' ? 'wss:' : 'ws:';
  const wsUrl = `${proto}//${location.host}/ws`;

  try {
    updateLiveIndicator('connecting');
    ws = new WebSocket(wsUrl);

    ws.onopen = () => {
      wsReconnectAttempts = 0;
      const connectFrame = `CONNECT\naccept-version:1.2,1.1,1.0\nheart-beat:10000,10000\nAuthorization:Bearer ${token}\n\n\0`;
      ws.send(connectFrame);
    };

    ws.onmessage = (event) => {
      const data = event.data;
      if (typeof data !== 'string') return;

      if (data.startsWith('CONNECTED')) {
        updateLiveIndicator('connected');
        startWsHeartbeat();
        if (currentWsId) subscribeWsWorkspace(currentWsId);
        // Subscribe to public chat and personal notifications
        ws.send(`SUBSCRIBE\nid:sub-pub-chat\ndestination:/topic/public-chat\n\n\0`);
        ws.send(`SUBSCRIBE\nid:sub-user-chat\ndestination:/user/queue/private-chat\n\n\0`);
        ws.send(`SUBSCRIBE\nid:sub-user-notif\ndestination:/user/queue/notifications\n\n\0`);
      } else if (data.startsWith('ERROR')) {
        if (data.toLowerCase().includes('unauthorized') || data.toLowerCase().includes('token')) {
          wsAuthFailed = true;
          try { ws.close(); } catch (e) {}
        }
      } else if (data.startsWith('MESSAGE')) {
        const bodyIndex = data.indexOf('\n\n');
        if (bodyIndex !== -1) {
          const body = data.substring(bodyIndex + 2).replace(/\0$/, '');
          try {
            const eventObj = JSON.parse(body);
            handleRealTimeEvent(eventObj, data);
          } catch (err) {}
        }
      }
    };

    ws.onclose = () => {
      stopWsHeartbeat();
      wsSubscribedWsId = null;
      updateLiveIndicator('disconnected');
      if (!wsIntentionallyClosed) scheduleWsReconnect(wsAuthFailed);
    };

    ws.onerror = () => {
      try { ws.close(); } catch (e) {}
    };
  } catch (e) {
    scheduleWsReconnect();
  }
}

function startWsHeartbeat() {
  stopWsHeartbeat();
  wsHeartbeatTimer = setInterval(() => {
    if (ws && ws.readyState === WebSocket.OPEN) ws.send('\n');
  }, 15000);
}

function stopWsHeartbeat() {
  if (wsHeartbeatTimer) {
    clearInterval(wsHeartbeatTimer);
    wsHeartbeatTimer = null;
  }
}

function scheduleWsReconnect(forcedDueToAuth = false) {
  if (wsReconnectTimer || wsIntentionallyClosed) return;
  wsReconnectAttempts++;
  const delay = Math.min(1000 * Math.pow(1.5, wsReconnectAttempts), 15000);
  wsReconnectTimer = setTimeout(async () => {
    wsReconnectTimer = null;
    if (wsIntentionallyClosed) return;
    if (forcedDueToAuth || isTokenExpiredOrNear(token)) {
      try {
        await refreshAuthToken();
        wsAuthFailed = false;
      } catch (err) {
        return;
      }
    }
    if (token && !wsIntentionallyClosed) initWebSocket();
  }, delay);
}

function subscribeWsWorkspace(wsId) {
  if (!ws || ws.readyState !== WebSocket.OPEN || !wsId) return;
  if (wsSubscribedWsId && wsSubscribedWsId !== wsId) {
    ws.send(`UNSUBSCRIBE\nid:sub-ws-${wsSubscribedWsId}\n\n\0`);
  }
  wsSubscribedWsId = wsId;
  ws.send(`SUBSCRIBE\nid:sub-ws-${wsId}\ndestination:/topic/workspace/${wsId}\n\n\0`);
}

function handleRealTimeEvent(ev, rawFrame = '') {
  if (rawFrame.includes('/topic/public-chat') || rawFrame.includes('/queue/private-chat')) {
    if (currentTab === 'chat') loadChatMessages();
    return;
  }
  if (rawFrame.includes('/queue/notifications')) {
    refreshNotificationsBadge();
    return;
  }
  if (!ev || !ev.type) return;
  if (ev.workspaceId && currentWsId && ev.workspaceId !== currentWsId) return;

  const isDragging = document.querySelector('.sortable-ghost') !== null;
  if (isDragging) return;

  const type = ev.type;
  const data = ev.data;

  switch (type) {
    case 'TASK_CREATED':
      if (data && data.id && !tasksData.some(t => t.id === data.id)) {
        tasksData.push(data);
        renderBoard();
      }
      break;
    case 'TASK_UPDATED':
    case 'TASK_MOVED':
      if (data && data.id) {
        const idx = tasksData.findIndex(t => t.id === data.id);
        if (idx !== -1) tasksData[idx] = { ...tasksData[idx], ...data };
        else tasksData.push(data);
        renderBoard();
      }
      break;
    case 'TASK_DELETED':
      {
        const deletedId = data?.taskId || data?.id || (typeof data === 'string' ? data : null);
        if (deletedId) {
          tasksData = tasksData.filter(t => t.id !== deletedId);
          renderBoard();
          if (currentTaskId === deletedId) closeSheet();
        }
      }
      break;
    default:
      if (currentWsId) loadBoard(currentWsId);
      break;
  }
}

// ===== Interactive 3D Hero Parallax Controller (Pointer + Touch + Reduced Motion Aware) =====
(function initHero3DParallax() {
  try {
    const savedCollapsed = localStorage.getItem('tc_hero_collapsed');
    const heroEl = document.getElementById('hero-showcase');
    if (heroEl && savedCollapsed === '1') {
      heroEl.classList.add('collapsed');
    }
  } catch (e) {}

  if (window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
    return;
  }

  function bindStageParallax(viewportId, sceneId) {
    const vp = document.getElementById(viewportId);
    const sc = document.getElementById(sceneId);
    if (!vp || !sc) return;

    let rafId = null;
    function applyTilt(clientX, clientY) {
      if (rafId) cancelAnimationFrame(rafId);
      rafId = requestAnimationFrame(() => {
        const rect = vp.getBoundingClientRect();
        if (!rect.width || !rect.height) return;
        const nx = ((clientX - rect.left) / rect.width) - 0.5;
        const ny = ((clientY - rect.top) / rect.height) - 0.5;
        const ry = Math.max(-18, Math.min(18, nx * 28));
        const rx = Math.max(-14, Math.min(14, -ny * 22 + 5));
        sc.style.setProperty('--rx', rx.toFixed(1) + 'deg');
        sc.style.setProperty('--ry', ry.toFixed(1) + 'deg');
      });
    }

    function resetTilt() {
      sc.style.setProperty('--rx', '6deg');
      sc.style.setProperty('--ry', '-8deg');
    }

    vp.addEventListener('pointermove', (e) => applyTilt(e.clientX, e.clientY), { passive: true });
    vp.addEventListener('pointerleave', resetTilt, { passive: true });
    vp.addEventListener('touchmove', (e) => {
      if (e.touches && e.touches[0]) {
        applyTilt(e.touches[0].clientX, e.touches[0].clientY);
      }
    }, { passive: true });
    vp.addEventListener('touchend', resetTilt, { passive: true });
  }

  bindStageParallax('hero-3d-viewport', 'hero-3d-scene');
  bindStageParallax('auth-3d-stage', 'auth-3d-stage');
})();

init();
