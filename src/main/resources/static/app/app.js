const tg = window.Telegram.WebApp;
tg.ready(); 
tg.expand();
if (tg.colorScheme === 'dark') document.documentElement.setAttribute('data-theme', 'dark');

const API = location.origin;
let token = null;
let currentWsId = null;
let currentUserId = null;
let colsData = [];
let tasksData = [];
let workspaceMembers = [];
let currentTaskId = null; // for details/edit
let currentFilter = 'all';
let searchQuery = '';
let currentTab = 'board';
let allWorkspaces = [];
let sortPrefs = {};
let botUsername = '';

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
        t.style = 'position:fixed;bottom:20px;left:50%;transform:translateX(-50%);background:var(--sec-bg-color);color:var(--text-color);padding:8px 16px;border-radius:20px;font-size:14px;box-shadow:0 2px 8px rgba(0,0,0,0.2);z-index:9999;transition:opacity 0.3s;opacity:0;pointer-events:none;';
        document.body.appendChild(t);
    }
    t.textContent = msg;
    t.style.opacity = '1';
    setTimeout(() => t.style.opacity = '0', 2000);
}

const colors = ['#3b82f6', '#ef4444', '#10b981', '#f59e0b', '#8b5cf6', '#ec4899'];
function getColColor(str) {
  let hash = 0;
  for (let i = 0; i < str.length; i++) hash = str.charCodeAt(i) + ((hash << 5) - hash);
  return colors[Math.abs(hash) % colors.length];
}

function getInitials(name) {
  if (!name) return '?';
  const parts = name.split(' ').filter(Boolean);
  return (parts[0][0] + (parts[1] ? parts[1][0] : '')).toUpperCase();
}

function esc(s) {
  if (!s) return '';
  return String(s).replace(/[&<>"]/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;'}[c]));
}

let reauthPromise = null;

async function api(path, opts = {}, retry = true) {
  const controller = new AbortController();
  const id = setTimeout(() => controller.abort(), 20000);
  try {
    let res = await fetch(API + path, {
      ...opts,
      signal: controller.signal,
      headers: { 'Content-Type': 'application/json', ...(token && { Authorization: 'Bearer ' + token }) }
    });
    clearTimeout(id);

    if (res.status === 401 || res.status === 403) {
      if (retry && tg.initData && path !== '/api/v1/auth/telegram') {
        if (!reauthPromise) {
          reauthPromise = (async () => {
            const authController = new AbortController();
            const authId = setTimeout(() => authController.abort(), 20000);
            try {
              const authRes = await fetch(API + '/api/v1/auth/telegram', {
                method: 'POST',
                signal: authController.signal,
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ initData: tg.initData })
              });
              clearTimeout(authId);
              if (!authRes.ok) throw new Error('AUTH_FATAL');
              const data = await authRes.json();
              token = data?.data?.token || data?.token;
              return token;
            } catch (e) {
              clearTimeout(authId);
              throw e;
            }
          })().finally(() => { reauthPromise = null; });
        }
        try {
          await reauthPromise;
          return api(path, opts, false);
        } catch (e) {
          showFatalError("Sessiya muddati tugadi. Iltimos, ilovani qayta oching.");
          throw e;
        }
      } else {
        if (res.status === 401) throw new Error('AUTH');
      }
    }

    if (!res.ok) {
      const txt = await res.text();
      throw new Error(txt || ('HTTP ' + res.status));
    }
    if (res.status === 204) return null;
    return res.json();
  } catch (err) {
    clearTimeout(id);
    if (err.name === 'AbortError') throw new Error('Tarmoq xatosi (kutilish vaqti tugadi)');
    throw err;
  }
}

function showFatalError(msg) {
    document.getElementById('loader').style.display = 'none';
    const appView = document.getElementById('app');
    if (appView) appView.style.display = 'none';
    const loginView = document.getElementById('login-view');
    if (loginView) loginView.style.display = 'none';
    const overlay = document.getElementById('sheet-overlay');
    if (overlay) overlay.style.display = 'none';
    const bsheet = document.getElementById('bottom-sheet');
    if (bsheet) bsheet.style.display = 'none';
    
    document.body.insertAdjacentHTML('beforeend', `
        <div style="position:fixed;top:0;left:0;right:0;bottom:0;background:var(--bg-color);display:flex;flex-direction:column;align-items:center;justify-content:center;padding:20px;text-align:center;z-index:99999;">
            <div style="font-size:48px;margin-bottom:16px;">⚠️</div>
            <div style="font-size:18px;margin-bottom:24px;color:var(--text-color);">${msg}</div>
            <button class="btn" style="width:100%;max-width:300px;background:var(--tg-theme-button-color);color:var(--tg-theme-button-text-color);border:none;padding:12px;border-radius:12px;font-size:16px;" onclick="tg.close()">Chiqish</button>
        </div>
    `);
}

const endpoints = {
  workspaces: '/api/workspaces',
  columns: (ws) => `/api/workspaces/${ws}/columns`,
  tasks: (ws) => `/api/workspaces/${ws}/tasks`,
  taskMove: (ws, id) => `/api/workspaces/${ws}/tasks/${id}/reorder`,
  taskDetails: (ws, id) => `/api/workspaces/${ws}/tasks/${id}`,
  comments: (id) => `/api/tasks/${id}/comments`,
  members: (ws) => `/api/workspaces/${ws}/members`,
  assign: (ws, id, userId) => `/api/workspaces/${ws}/tasks/${id}/assign?userId=${userId}`
};

async function initCore() {
  if (!tg.initData) throw new Error("Bu dastur faqat Telegram ichida ishlaydi.");
  const r = await api('/api/v1/auth/telegram', { method: 'POST', body: JSON.stringify({ initData: tg.initData }) });
  token = r?.data?.token || r?.token;
  currentUserId = r?.data?.user?.id || r?.user?.id;
  
  try {
      const configRes = await api('/api/v1/config/public', {}, false);
      botUsername = configRes?.data?.botUsername || configRes?.botUsername || botUsername;
  } catch(e) {}

  const wssRes = await api(endpoints.workspaces);
  allWorkspaces = wssRes?.data?.content || wssRes?.data || [];
  const sel = document.getElementById('ws');
  const statsSel = document.getElementById('stats-ws');
  if (!allWorkspaces.length) throw new Error("G'alvalar topilmadi.");
  
  sel.innerHTML = '';
  statsSel.innerHTML = '';
  allWorkspaces.forEach(w => {
      const opt = document.createElement('option');
      opt.value = w.id;
      opt.textContent = w.title || w.name;
      sel.appendChild(opt);
      statsSel.appendChild(opt.cloneNode(true));
  });
  sel.onchange = (e) => loadBoard(e.target.value);
  
  tg.CloudStorage.getItem('last_ws_id', async (err, val) => {
    let targetWsId = allWorkspaces[0].id;
    if (initialTaskId) {
        try {
          const tRes = await api(`/api/tasks/${initialTaskId}`);
          if (tRes && tRes.data) {
             targetWsId = tRes.data.workspaceId;
          }
        } catch(e) {
           tg.showAlert("Kechirasiz, ushbu g'alva topilmadi yoki huquqingiz yo'q.");
           initialTaskId = null;
        }
    } else if (!err && val && allWorkspaces.find(w => w.id === val)) {
      targetWsId = val;
    }
    sel.value = targetWsId;
    statsSel.value = targetWsId;
    
    tg.CloudStorage.getItem('sort_prefs', (errSort, valSort) => {
      if (!errSort && valSort) {
        try { sortPrefs = JSON.parse(valSort); } catch(e){}
      }
      loadBoard(targetWsId);
    });
    
    if (initialTaskId) {
       openSheet('view', initialTaskId);
       setTimeout(() => {
           const card = document.querySelector(`.card[data-id="${initialTaskId}"]`);
           if (card) {
              card.scrollIntoView({ behavior: 'smooth', block: 'center' });
              card.style.transition = 'box-shadow 0.3s';
              card.style.boxShadow = '0 0 10px 2px var(--tg-theme-button-color)';
              setTimeout(() => { card.style.boxShadow = ''; }, 2000);
           }
           initialTaskId = null;
       }, 300);
    }
  });
}

let initAttempt = 0;
let initSlowTimer = null;
async function init() {
  if (initAttempt === 0) {
      initSlowTimer = setTimeout(() => {
          document.getElementById('loader-msg').textContent = "Tarmoq sekin ko'rinadi, kuting...";
      }, 4000);
  }
  try {
    await initCore();
    clearTimeout(initSlowTimer);
  } catch (e) {
    clearTimeout(initSlowTimer);
    if (e.message === 'AUTH') {
        document.getElementById('loader').style.display = 'none';
        document.getElementById('login-view').style.display = 'block';
        tg.MainButton.text = "KIRISH VA ULASH";
        tg.MainButton.show();
        tg.MainButton.onClick(handleLogin);
    } else if (e.message === 'AUTH_FATAL') {
        // handled
    } else {
        initAttempt++;
        let delay = Math.min(1000 * Math.pow(2, initAttempt), 15000);
        document.getElementById('loader-msg').textContent = `Xatolik: ${e.message}. ${delay/1000}s dan so'ng qayta uriniladi...`;
        document.querySelector('.spinner').style.display = 'block';
        setTimeout(init, delay);
    }
  }
}

async function handleLogin() {
    const username = document.getElementById('login-username').value.trim();
    const password = document.getElementById('login-password').value.trim();
    const errDiv = document.getElementById('login-error');
    errDiv.textContent = '';
    
    if (!username || !password) {
        errDiv.textContent = 'Iltimos, login va parolni kiriting';
        return;
    }
    
    tg.MainButton.showProgress();
    try {
        const loginRes = await fetch(API + '/api/v1/auth/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ nameOrEmail: username, password: password })
        });
        
        if (!loginRes.ok) {
            if (loginRes.status === 429) {
                const retryAfter = loginRes.headers.get('Retry-After') || 60;
                tg.MainButton.disable();
                setTimeout(() => {
                    tg.MainButton.enable();
                    errDiv.textContent = '';
                }, parseInt(retryAfter) * 1000);
                throw new Error("Juda ko'p urinish. Birozdan keyin qayta urinib ko'ring.");
            }
            throw new Error('Login yoki parol xato');
        }
        
        const loginData = await loginRes.json();
        const tempToken = loginData.data?.token || loginData.token;
        
        const linkRes = await fetch(API + '/api/users/me/telegram', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json', 'Authorization': 'Bearer ' + tempToken },
            body: JSON.stringify({ initData: tg.initData })
        });
        
        if (!linkRes.ok) {
            if (linkRes.status === 409) throw new Error('Bu Telegram boshqa akkauntga ulangan');
            throw new Error('Ulashda xatolik yuz berdi');
        }
        
        document.getElementById('login-password').value = '';
        tg.MainButton.hideProgress();
        tg.MainButton.offClick(handleLogin);
        tg.MainButton.hide();
        
        document.getElementById('login-view').style.display = 'none';
        document.getElementById('loader').style.display = 'flex';
        init();
    } catch (e) {
        tg.MainButton.hideProgress();
        errDiv.textContent = e.message;
    }
}

async function loadBoard(wsId) {
  currentWsId = wsId;
  tg.CloudStorage.setItem('last_ws_id', wsId);
  const container = document.getElementById('board-container');
  container.innerHTML = Array(3).fill(0).map(() => `
    <div class="col" style="background:var(--sec-bg-color); opacity: 0.7;">
      <div class="col-header"><div class="skeleton" style="width:120px;height:20px;"></div></div>
      <div class="list" style="padding:12px;">
         <div class="skeleton card" style="height:80px;margin-bottom:10px;"></div>
         <div class="skeleton card" style="height:60px;"></div>
      </div>
    </div>
  `).join('');

  try {
    const colsRes = await api(endpoints.columns(wsId));
    colsData = colsRes?.data || [];
    
    tasksData = [];
    colsData.forEach(c => {
      if (c.tasks) tasksData.push(...c.tasks);
    });
    
    const membersRes = await api(endpoints.members(wsId));
    workspaceMembers = membersRes?.data || [];
    renderAssigneePicker();
    
    renderBoard();
    
    const fCol = document.getElementById('f-col');
    fCol.innerHTML = '';
    colsData.forEach(c => {
        const opt = document.createElement('option');
        opt.value = c.id;
        opt.textContent = c.title;
        fCol.appendChild(opt);
    });
    
    document.getElementById('loader').style.opacity = '0';
    setTimeout(() => document.getElementById('loader').style.display = 'none', 300);
  } catch (e) {
    container.innerHTML = `<div class="empty-placeholder">Yuklashda xatolik: ${e.message}</div>`;
  }
}

function renderAssigneePicker() {
    const picker = document.getElementById('f-assignee-picker');
    picker.innerHTML = `<div class="member-avatar" data-id="" title="Hech kim">❌</div>`;
    picker.querySelector('.member-avatar').onclick = function() { selectAssignee(this); };
    workspaceMembers.forEach(m => {
        const d = document.createElement('div');
        d.className = 'member-avatar';
        d.dataset.id = m.id;
        d.title = m.fullName || m.name;
        d.textContent = getInitials(m.fullName || m.name);
        d.onclick = function() { selectAssignee(this); };
        picker.appendChild(d);
    });
}

function selectAssignee(el) {
    document.querySelectorAll('#f-assignee-picker .member-avatar').forEach(a => a.classList.remove('active'));
    el.classList.add('active');
}

function renderBoard() {
  const container = document.getElementById('board-container');
  container.innerHTML = '';
  
  colsData.forEach(c => {
    let colTasks = tasksData.filter(t => t.columnId === c.id);
    
    // Sort logic
    const sort = sortPrefs[c.id] || 'manual';
    if (sort === 'deadline') {
        colTasks.sort((a,b) => (a.dueDate||'9999-99-99').localeCompare(b.dueDate||'9999-99-99'));
    } else if (sort === 'priority') {
        const pMap = { 'HIGH': 3, 'MEDIUM': 2, 'LOW': 1 };
        colTasks.sort((a,b) => (pMap[b.priority] || 0) - (pMap[a.priority] || 0));
    } else {
        colTasks.sort((a,b) => (a.lexoRank||'').localeCompare(b.lexoRank||''));
    }
    
    const colColor = getColColor(c.title || c.id);
    
    const colEl = document.createElement('div');
    colEl.className = 'col';
    colEl.style.setProperty('--col-color', colColor);
    
    colEl.innerHTML = `
      <div class="col-header">
        <div class="col-header-left">
          <span class="col-title-text"></span>
          <span class="col-count"></span>
        </div>
        <div style="display:flex; align-items:center; gap:4px;">
          <button class="sort-btn" title="Tartiblash">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="12" y1="5" x2="12" y2="19"></line><polyline points="19 12 12 19 5 12"></polyline></svg>
          </button>
          <span class="del-btn" style="color:var(--hint-color); font-size:18px; padding:4px; cursor:pointer;">×</span>
        </div>
      </div>
      <div class="list"></div>
      <div class="col-footer">
        <div class="quick-add">
          <input type="text" placeholder="+ Tez qo'shish...">
        </div>
        <button class="btn-add-task">Batafsil</button>
      </div>
    `;
    
    colEl.querySelector('.col-header').onclick = () => promptRenameColumn(c.id, c.title);
    colEl.querySelector('.col-title-text').textContent = c.title;
    colEl.querySelector('.col-count').textContent = colTasks.length;
    colEl.querySelector('.sort-btn').onclick = (e) => { e.stopPropagation(); promptSort(c.id); };
    colEl.querySelector('.del-btn').onclick = (e) => { e.stopPropagation(); promptDeleteColumn(c.id); };
    colEl.querySelector('.list').dataset.col = c.id;
    if (colTasks.length === 0) {
        colEl.querySelector('.list').innerHTML = `<div class="empty-placeholder"><div style="font-size:32px; margin-bottom:8px;">🏖️</div>Bo'sh, xavotirga o'rin yo'q</div>`;
    }
    const qInp = colEl.querySelector('.quick-add input');
    qInp.onkeydown = (e) => { if(e.key === 'Enter') quickAddTask(qInp, c.id); };
    colEl.querySelector('.btn-add-task').onclick = () => openSheet('create', null, c.id);
    
    const listEl = colEl.querySelector('.list');
    colTasks.forEach(t => listEl.appendChild(createCard(t)));
    container.appendChild(colEl);
    
    Sortable.create(listEl, {
      group: 'board',
      animation: 200,
      delay: 150,
      delayOnTouchOnly: true,
      ghostClass: 'sortable-ghost',
      dragClass: 'sortable-drag',
      onStart: () => tg.HapticFeedback.impactOccurred('light'),
      onEnd: (ev) => {
        const taskId = ev.item.dataset.id;
        const newColId = ev.to.dataset.col;
        if (ev.from !== ev.to) {
          tg.HapticFeedback.impactOccurred('medium');
          moveTask(taskId, newColId);
        }
      }
    });
  });

  container.insertAdjacentHTML('beforeend', `
    <div class="col" style="justify-content:center; align-items:center; cursor:pointer; opacity:0.6; background:transparent; border:2px dashed var(--hint-color);" onclick="promptAddColumn()">
       <div style="font-weight:600;">+ Yangi ustun</div>
    </div>
  `);
  
  applyFilters();
}

function promptAddColumn() {
    const title = prompt("Yangi ustun nomi:");
    if (!title) return;
    tg.HapticFeedback.impactOccurred('light');
    api(endpoints.columns(currentWsId), { method: 'POST', body: JSON.stringify({title}) })
      .then(res => { colsData.push(res.data); renderBoard(); })
      .catch(e => tg.showAlert(e.message));
}

function promptRenameColumn(colId, oldTitle) {
    const title = prompt("Ustun nomini o'zgartirish:", oldTitle);
    if (!title || title === oldTitle) return;
    const col = colsData.find(c => c.id === colId);
    col.title = title; 
    renderBoard();
    api(`/api/workspaces/${currentWsId}/columns/${colId}`, { method: 'PUT', body: JSON.stringify({title}) })
      .catch(e => { col.title = oldTitle; renderBoard(); tg.showAlert(e.message); });
}

function promptDeleteColumn(colId) {
    const colTasks = tasksData.filter(t => t.columnId === colId);
    if (colTasks.length > 0) {
        return tg.showAlert("Ustunda g'alvalar tiqilib yotibdi, avval tozalang.");
    }
    tg.showConfirm("Ustunni chopamizmi?", (ok) => {
        if (!ok) return;
        tg.HapticFeedback.impactOccurred('medium');
        api(`/api/workspaces/${currentWsId}/columns/${colId}`, { method: 'DELETE' })
          .then(() => { colsData = colsData.filter(c => c.id !== colId); renderBoard(); })
          .catch(e => tg.showAlert(e.message));
    });
}

function setFilter(el) {
  document.querySelectorAll('.chip').forEach(c => c.classList.remove('active'));
  el.classList.add('active');
  currentFilter = el.dataset.filter;
  tg.HapticFeedback.impactOccurred('light');
  applyFilters();
}

function applyFilters() {
  searchQuery = document.getElementById('search-input').value.toLowerCase();
  
  document.querySelectorAll('.card').forEach(card => {
    const taskId = card.dataset.id;
    const task = tasksData.find(t => t.id === taskId);
    if (!task) return;
    
    let match = true;
    if (searchQuery && !task.title.toLowerCase().includes(searchQuery)) match = false;
    
    if (match) {
        if (currentFilter === 'mine') {
            if (!currentUserId || !task.assignees || !task.assignees.some(a => a.id === currentUserId)) match = false;
        } else if (currentFilter === 'overdue') {
            if (!task.isOverdue) match = false;
        } else if (currentFilter === 'high') {
            if (task.priority !== 'HIGH') match = false;
        }
    }
    
    card.style.display = match ? 'block' : 'none';
  });
  
  document.querySelectorAll('.col').forEach(col => {
      const list = col.querySelector('.list');
      if (list) {
          const count = Array.from(list.children).filter(c => c.style.display !== 'none' && c.classList.contains('card')).length;
          const countBadge = col.querySelector('.col-count');
          if (countBadge) countBadge.textContent = count;
      }
  });
}

let pressTimer;
function onCardTouchStart(e, tId) {
    pressTimer = setTimeout(() => {
        pressTimer = null;
        tg.HapticFeedback.impactOccurred('heavy');
        openSheet('quick', tId);
    }, 600);
}
function onCardTouchEnd() {
    if (pressTimer) clearTimeout(pressTimer);
}

function createCard(t) {
  const card = document.createElement('div');
  card.className = 'card';
  card.dataset.id = t.id;
  
  card.onmousedown = card.ontouchstart = (e) => onCardTouchStart(e, t.id);
  card.onmouseup = card.onmouseleave = card.ontouchend = card.ontouchcancel = () => onCardTouchEnd();
  card.oncontextmenu = (e) => { e.preventDefault(); openSheet('quick', t.id); };
  card.onclick = (e) => {
      if (pressTimer) openSheet('view', t.id);
  };
  
  let prioLabel = t.priority === 'HIGH' ? 'Yuqori' : (t.priority === 'LOW' ? 'Past' : "O'rta");
  let prioClass = t.priority ? t.priority.toLowerCase() : 'medium';
  
  card.innerHTML = `
    <div class="card-title"></div>
    <div class="card-meta">
      <div class="badges"></div>
    </div>
  `;
  card.querySelector('.card-title').textContent = t.title;
  const badgesEl = card.querySelector('.badges');
  
  const bP = document.createElement('span');
  bP.className = `badge priority ${prioClass}`;
  bP.textContent = prioLabel;
  badgesEl.appendChild(bP);
  
  if (t.dueDate) {
      const bD = document.createElement('span');
      bD.className = `badge date ${t.isOverdue ? 'overdue' : ''}`;
      bD.textContent = t.dueDate;
      badgesEl.appendChild(bD);
  }
  
  if (t.assignees && t.assignees.length > 0) {
      const a = t.assignees[0];
      const av = document.createElement('div');
      av.className = 'assignee-avatar';
      av.textContent = getInitials(a.fullName || a.name);
      card.querySelector('.card-meta').appendChild(av);
  }
  return card;
}

async function moveTask(taskId, targetColId) {
  const task = tasksData.find(t => t.id === taskId);
  if (!task) return;
  const oldColId = task.columnId;
  task.columnId = targetColId; 
  renderBoard(); 
  
  try {
    await api(endpoints.taskMove(currentWsId, taskId), { 
      method: 'PATCH', 
      body: JSON.stringify({ columnId: targetColId }) 
    });
    
    const col = colsData.find(c => c.id === targetColId);
    if (col && /bajarildi|tugadi|done/i.test(col.title)) {
        celebrate();
    } else {
        showToast("Ko'chirildi");
    }
  } catch (e) {
    tg.HapticFeedback.notificationOccurred('error');
    task.columnId = oldColId; 
    renderBoard();
    tg.showAlert("Xatolik: " + e.message);
  }
}

function celebrate() {
    tg.HapticFeedback.notificationOccurred('success');
    const confetti = document.createElement('div');
    confetti.className = 'confetti';
    confetti.innerHTML = `<svg width="100%" height="100%"><rect width="100%" height="100%" fill="none"/></svg>`;
    // Simple pure css/js confetti burst simulation
    for (let i = 0; i < 50; i++) {
        const dot = document.createElement('div');
        dot.style.position = 'absolute';
        dot.style.left = '50%';
        dot.style.top = '50%';
        dot.style.width = '8px';
        dot.style.height = '8px';
        dot.style.backgroundColor = colors[Math.floor(Math.random() * colors.length)];
        dot.style.borderRadius = Math.random() > 0.5 ? '50%' : '0';
        
        const angle = Math.random() * Math.PI * 2;
        const velocity = 50 + Math.random() * 100;
        const tx = Math.cos(angle) * velocity;
        const ty = Math.sin(angle) * velocity - 50;
        
        dot.style.transition = 'transform 1s cubic-bezier(0,0,0.2,1), opacity 1s';
        dot.style.transform = `translate(0, 0) scale(0)`;
        dot.style.opacity = '1';
        
        confetti.appendChild(dot);
        
        setTimeout(() => {
            dot.style.transform = `translate(${tx}px, ${ty}px) scale(1) rotate(${Math.random()*360}deg)`;
            dot.style.opacity = '0';
        }, 10);
    }
    document.body.appendChild(confetti);
    setTimeout(() => confetti.remove(), 1000);
}

function promptSort(colId) {
    tg.showPopup({
        title: "Tartiblash",
        message: "Ustun ichidagi vazifalarni qanday tartiblaymiz?",
        buttons: [
            { id: "manual", type: "default", text: "Qo'lda (Manual)" },
            { id: "deadline", type: "default", text: "Muddatiga ko'ra" },
            { id: "priority", type: "default", text: "Muhimligiga ko'ra" },
            { type: "cancel" }
        ]
    }, (btnId) => {
        if (btnId) {
            sortPrefs[colId] = btnId;
            tg.CloudStorage.setItem('sort_prefs', JSON.stringify(sortPrefs));
            renderBoard();
        }
    });
}

async function quickAddTask(inputEl, colId) {
    const title = inputEl.value.trim();
    if (!title) return;
    inputEl.disabled = true;
    try {
        const res = await api(endpoints.tasks(currentWsId), {
            method: 'POST',
            body: JSON.stringify({ title, columnId: colId, issueType: 'TASK', priority: 'MEDIUM' })
        });
        tasksData.push(res.data);
        inputEl.value = '';
        renderBoard();
        tg.HapticFeedback.notificationOccurred('success');
    } catch(e) {
        tg.showAlert("Xato: " + e.message);
    }
    inputEl.disabled = false;
    inputEl.focus();
}

function openSheet(mode, taskId = null, defaultColId = null) {
  tg.HapticFeedback.impactOccurred('light');
  document.getElementById('sheet-overlay').classList.add('active');
  document.getElementById('bottom-sheet').classList.add('active');
  
  const formView = document.getElementById('task-form-view');
  const detailView = document.getElementById('task-detail-view');
  const quickView = document.getElementById('quick-action-view');
  
  currentTaskId = taskId;
  tg.BackButton.show();
  tg.BackButton.onClick(closeSheet);
  
  formView.style.display = 'none';
  detailView.style.display = 'none';
  quickView.style.display = 'none';
  
  if (mode === 'create') {
    formView.style.display = 'block';
    document.getElementById('sheet-title').textContent = "Yangi g'alva";
    document.getElementById('f-id').value = '';
    document.getElementById('f-title').value = '';
    document.getElementById('f-desc').value = '';
    document.getElementById('f-priority').value = 'MEDIUM';
    document.getElementById('f-date').value = '';
    if (defaultColId) document.getElementById('f-col').value = defaultColId;
    
    document.querySelectorAll('#f-assignee-picker .member-avatar').forEach(a => a.classList.remove('active'));
    document.querySelector('#f-assignee-picker .member-avatar[data-id=""]')?.classList.add('active');
    
    tg.MainButton.text = "BUNI HAM DUSHANBADAN QILAMIZ!";
    tg.MainButton.show();
    tg.MainButton.onClick(saveTask);
  } 
  else if (mode === 'edit') {
    const task = tasksData.find(t => t.id === taskId);
    if (!task) return closeSheet();
    formView.style.display = 'block';
    document.getElementById('sheet-title').textContent = "G'alva ta'miri";
    document.getElementById('f-id').value = task.id;
    document.getElementById('f-title').value = task.title || '';
    document.getElementById('f-desc').value = task.description || '';
    document.getElementById('f-priority').value = task.priority || 'MEDIUM';
    document.getElementById('f-date').value = task.dueDate || '';
    document.getElementById('f-col').value = task.columnId;
    
    const assigneeId = task.assignees?.[0]?.id || "";
    document.querySelectorAll('#f-assignee-picker .member-avatar').forEach(a => a.classList.remove('active'));
    const toSelect = document.querySelector(`#f-assignee-picker .member-avatar[data-id="${assigneeId}"]`);
    if (toSelect) toSelect.classList.add('active');
    
    tg.MainButton.text = "BUNI HAM DUSHANBADAN QILAMIZ!";
    tg.MainButton.show();
    tg.MainButton.onClick(saveTask);
  }
  else if (mode === 'view') {
    const task = tasksData.find(t => t.id === taskId);
    if (!task) return closeSheet();
    detailView.style.display = 'block';
    
    document.getElementById('d-title').textContent = task.title;
    document.getElementById('d-desc').textContent = task.description || 'Tavsif yo\'q';
    
    const dCol = document.getElementById('d-col-select');
    dCol.innerHTML = '';
    colsData.forEach(c => {
        const opt = document.createElement('option');
        opt.value = c.id;
        opt.textContent = c.title;
        dCol.appendChild(opt);
    });
    dCol.value = task.columnId;
    
    let prioLabel = task.priority === 'HIGH' ? 'Yuqori' : (task.priority === 'LOW' ? 'Past' : "O'rta");
    let prioClass = task.priority ? task.priority.toLowerCase() : 'medium';
    const dBadges = document.getElementById('d-badges');
    dBadges.innerHTML = '';
    
    if (task.labels && task.labels.length > 0) {
        task.labels.forEach(l => {
            const ls = document.createElement('span');
            ls.className = 'badge';
            ls.style.background = l.color + '33';
            ls.style.color = l.color;
            ls.textContent = l.name;
            dBadges.appendChild(ls);
        });
    }
    
    const pb = document.createElement('span');
    pb.className = `badge priority ${prioClass}`;
    pb.textContent = prioLabel;
    dBadges.appendChild(pb);
    
    if (task.dueDate) {
        const db = document.createElement('span');
        db.className = `badge date ${task.isOverdue ? 'overdue' : ''}`;
        db.textContent = task.dueDate;
        dBadges.appendChild(db);
    }
    
    tg.MainButton.hide();
    loadComments(task.id);
    loadChecklists(task.id);
  }
  else if (mode === 'quick') {
    const task = tasksData.find(t => t.id === taskId);
    if (!task) return closeSheet();
    quickView.style.display = 'block';
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
}

function closeSheet() {
  document.getElementById('sheet-overlay').classList.remove('active');
  document.getElementById('bottom-sheet').classList.remove('active');
  tg.BackButton.hide();
  tg.BackButton.offClick(closeSheet);
  tg.MainButton.hide();
  tg.MainButton.offClick(saveTask);
}

async function saveTask() {
  const id = document.getElementById('f-id').value;
  const colId = document.getElementById('f-col').value;
  const title = document.getElementById('f-title').value.trim();
  const desc = document.getElementById('f-desc').value.trim();
  const priority = document.getElementById('f-priority').value;
  const dueDate = document.getElementById('f-date').value || null;
  const assigneeId = document.querySelector('#f-assignee-picker .member-avatar.active')?.dataset.id || null;
  
  if (!title) {
    tg.HapticFeedback.notificationOccurred('error');
    return tg.showAlert("Sarlavha bo'sh bo'lishi mumkin emas!");
  }
  
  tg.MainButton.showProgress();
  try {
    let res;
    if (id) {
      res = await api(endpoints.taskDetails(currentWsId, id), {
        method: 'PUT',
        body: JSON.stringify({ title, description: desc, priority, dueDate, columnId: colId })
      });
      const oldAssignee = tasksData.find(t => t.id === id).assignees?.[0]?.id;
      if (oldAssignee !== assigneeId) {
          if (oldAssignee) await api(endpoints.assign(currentWsId, id, oldAssignee), {method:'POST'}); 
          if (assigneeId) await api(endpoints.assign(currentWsId, id, assigneeId), {method:'POST'}); 
      }
      const updated = res.data;
      if (assigneeId) updated.assignees = [workspaceMembers.find(m => m.id === assigneeId)];
      else updated.assignees = [];
      tasksData = tasksData.map(t => t.id === id ? updated : t);
    } else {
      res = await api(endpoints.tasks(currentWsId), {
        method: 'POST',
        body: JSON.stringify({ title, description: desc, priority, dueDate, columnId: colId, issueType: 'TASK' })
      });
      if (assigneeId) {
          await api(endpoints.assign(currentWsId, res.data.id, assigneeId), {method:'POST'});
          res.data.assignees = [workspaceMembers.find(m => m.id === assigneeId)];
      }
      tasksData.push(res.data);
    }
    tg.HapticFeedback.notificationOccurred('success');
    showToast("Saqlandi");
    closeSheet();
    renderBoard();
  } catch (e) {
    tg.HapticFeedback.notificationOccurred('error');
    tg.showAlert("Xatolik: " + e.message);
  } finally {
    tg.MainButton.hideProgress();
  }
}

async function deleteTask() {
  if (!currentTaskId) return;
  tg.showConfirm("Haqiqatan ham bu g'alvani chopmoqchimisiz?", async (ok) => {
    if (!ok) return;
    try {
      await api(endpoints.taskDetails(currentWsId, currentTaskId), { method: 'DELETE' });
      tasksData = tasksData.filter(t => t.id !== currentTaskId);
      tg.HapticFeedback.notificationOccurred('success');
      showToast("Chopildi!");
      closeSheet();
      renderBoard();
    } catch (e) {
      tg.showAlert("Xatolik: " + e.message);
    }
  });
}

async function moveTaskFromSelect(newColId) {
  if (!currentTaskId) return;
  const task = tasksData.find(t => t.id === currentTaskId);
  if (!task || task.columnId === newColId) return;
  await moveTask(currentTaskId, newColId);
}

async function loadComments(taskId) {
  const list = document.getElementById('comments-list');
  const count = document.getElementById('comments-count');
  list.innerHTML = '<div class="skeleton" style="height:40px;margin-bottom:8px;"></div><div class="skeleton" style="height:40px;margin-bottom:8px;width:70%;"></div>';
  try {
    const res = await api(endpoints.comments(taskId));
    const comments = res.data || [];
    count.textContent = comments.length;
    list.innerHTML = '';
    if (!comments.length) {
        list.innerHTML = `<div style="text-align:center;color:var(--hint-color);font-size:12px;">Hali gap-so'z yo'q</div>`;
    } else {
        comments.forEach(c => {
            const div = document.createElement('div');
            div.className = 'comment';
            div.innerHTML = `
              <div class="comment-avatar"></div>
              <div class="comment-content">
                <div class="comment-author"></div>
                <div class="comment-text"></div>
              </div>
            `;
            div.querySelector('.comment-avatar').textContent = getInitials(c.authorFullName || c.authorName);
            div.querySelector('.comment-author').textContent = c.authorFullName || c.authorName;
            div.querySelector('.comment-text').textContent = c.content;
            list.appendChild(div);
        });
    }
    list.scrollTop = list.scrollHeight;
  } catch(e) { list.innerHTML = `<div style="color:red;font-size:12px;">Yuklashda xatolik</div>`; }
}

async function addComment() {
  const input = document.getElementById('comment-input');
  const text = input.value.trim();
  if(!text || !currentTaskId) return;
  tg.HapticFeedback.impactOccurred('light');
  input.disabled = true;
  try {
    await api(endpoints.comments(currentTaskId), { method: 'POST', body: JSON.stringify({ content: text }) });
    input.value = '';
    loadComments(currentTaskId);
  } catch(e) { tg.showAlert("Xatolik: " + e.message); }
  input.disabled = false;
}

// Checklists
async function loadChecklists(taskId) {
  const list = document.getElementById('checklists-list');
  list.innerHTML = '<div class="skeleton" style="height:30px;margin-bottom:8px;"></div>';
  try {
    const res = await api(`/api/workspaces/${currentWsId}/tasks/${taskId}/checklists`);
    const items = res.data || [];
    list.innerHTML = '';
    if (!items.length) {
        list.innerHTML = `<div style="color:var(--hint-color);font-size:12px;margin-bottom:12px;">Ichki vazifalar yo'q</div>`;
    } else {
        items.forEach(c => {
            const d = document.createElement('div');
            d.className = 'checklist-item';
            d.innerHTML = `
              <input type="checkbox">
              <span class="checklist-title"></span>
              <button class="icon-btn" style="color:var(--tg-theme-destructive-text-color, red);">×</button>
            `;
            d.querySelector('input').checked = c.isCompleted;
            d.querySelector('input').onchange = (e) => toggleChecklist(c.id, e.target.checked);
            d.querySelector('.checklist-title').textContent = c.title;
            d.querySelector('button').onclick = () => deleteChecklist(c.id);
            if (c.isCompleted) d.classList.add('completed');
            list.appendChild(d);
        });
    }

  } catch(e) { list.innerHTML = `<div style="color:red;font-size:12px;">Yuklashda xatolik</div>`; }
}

async function addChecklist() {
  const input = document.getElementById('checklist-input');
  const title = input.value.trim();
  if(!title || !currentTaskId) return;
  tg.HapticFeedback.impactOccurred('light');
  input.disabled = true;
  try {
    await api(`/api/workspaces/${currentWsId}/tasks/${currentTaskId}/checklists`, { method: 'POST', body: JSON.stringify({ title }) });
    input.value = '';
    loadChecklists(currentTaskId);
  } catch(e) { tg.showAlert("Xatolik: " + e.message); }
  input.disabled = false;
}

async function toggleChecklist(itemId, isCompleted) {
  try {
    await api(`/api/workspaces/${currentWsId}/tasks/${currentTaskId}/checklists/${itemId}`, { method: 'PUT', body: JSON.stringify({ isCompleted }) });
    loadChecklists(currentTaskId);
  } catch(e) { tg.showAlert("Xatolik: " + e.message); loadChecklists(currentTaskId); }
}

async function deleteChecklist(itemId) {
  try {
    await api(`/api/workspaces/${currentWsId}/tasks/${currentTaskId}/checklists/${itemId}`, { method: 'DELETE' });
    loadChecklists(currentTaskId);
  } catch(e) { tg.showAlert("Xatolik: " + e.message); }
}

// Tab navigation
function switchTab(tabId) {
    document.querySelectorAll('.tab-content').forEach(c => c.style.display = 'none');
    document.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));
    document.getElementById('tab-' + tabId).style.display = 'flex';
    document.querySelector(`.tab-btn[data-tab="${tabId}"]`).classList.add('active');
    currentTab = tabId;
    tg.HapticFeedback.impactOccurred('light');
    
    if (tabId === 'mytasks') loadMyTasks();
    if (tabId === 'stats') loadStats();
}

async function loadMyTasks() {
    const container = document.getElementById('mytasks-container');
    container.innerHTML = Array(3).fill(0).map(() => `<div class="skeleton card" style="height:60px;margin-bottom:10px;"></div>`).join('');
    
    try {
        const promises = allWorkspaces.map(w => api(endpoints.tasks(w.id)).then(res => ({ws: w, tasks: res.data})));
        const results = await Promise.all(promises);
        
        let myTasks = [];
        results.forEach(({ws, tasks}) => {
            if (!tasks) return;
            const userTasks = tasks.filter(t => t.assignees && t.assignees.some(a => a.id === currentUserId));
            userTasks.forEach(t => t.wsTitle = ws.title || ws.name);
            myTasks.push(...userTasks);
        });
        
        if (myTasks.length === 0) {
            container.innerHTML = `<div class="empty-placeholder">
                <div style="font-size:40px; margin-bottom:12px;">☕</div>
                Bo'shlik. O'zingiz uchun vaqt ajrating.
            </div>`;
            return;
        }
        
        const groups = {
            overdue: [], today: [], thisWeek: [], later: [], noDate: []
        };
        
        const today = new Date(); today.setHours(0,0,0,0);
        const nextWeek = new Date(today); nextWeek.setDate(today.getDate() + 7);
        
        myTasks.forEach(t => {
            if (t.isOverdue) { groups.overdue.push(t); return; }
            if (!t.dueDate) { groups.noDate.push(t); return; }
            
            const due = new Date(t.dueDate);
            if (due < today) { groups.overdue.push(t); } // fallback
            else if (due.getTime() === today.getTime()) { groups.today.push(t); }
            else if (due < nextWeek) { groups.thisWeek.push(t); }
            else { groups.later.push(t); }
        });
        
        container.innerHTML = '';
        const renderGroup = (tasks, title, icon) => {
            if (tasks.length === 0) return;
            const g = document.createElement('div');
            g.className = 'task-group';
            const titleHtml = `<div class="group-title">${icon} <span class="g-title-text"></span> (${tasks.length})</div>`;
            g.innerHTML = titleHtml;
            g.querySelector('.g-title-text').textContent = title;
            tasks.forEach(t => {
                const card = createCard(t);
                // Add workspace chip
                const wsChip = document.createElement('div');
                wsChip.style = 'font-size:10px; color:var(--hint-color); margin-bottom:4px;';
                wsChip.textContent = t.wsTitle;
                card.insertBefore(wsChip, card.firstChild);
                
                card.onclick = () => {
                    // Override click to ensure proper WS context
                    if (currentWsId !== t.workspaceId) {
                        currentWsId = t.workspaceId;
                        document.getElementById('ws').value = currentWsId;
                        loadBoard(currentWsId).then(() => openSheet('view', t.id));
                    } else {
                        openSheet('view', t.id);
                    }
                };
                g.appendChild(card);
            });
            container.appendChild(g);
        };
        
        renderGroup(groups.overdue, "Muddati o'tgan", "🔴");
        renderGroup(groups.today, "Bugun", "📅");
        renderGroup(groups.thisWeek, "Bu hafta", "🟡");
        renderGroup(groups.later, "Keyinroq", "🟢");
        renderGroup(groups.noDate, "Muddatsiz", "⚪");
        
    } catch(e) {
        container.innerHTML = `<div class="empty-placeholder">Yuklashda xatolik: ${e.message}</div>`;
    }
}

async function loadStats() {
    const wsId = document.getElementById('stats-ws').value;
    if (!wsId) return;
    const container = document.getElementById('stats-container');
    container.innerHTML = Array(2).fill(0).map(() => `<div class="skeleton card" style="height:100px;margin-bottom:10px;"></div>`).join('');
    
    try {
        const colsRes = await api(endpoints.columns(wsId));
        const cols = colsRes?.data || [];
        const memsRes = await api(endpoints.members(wsId));
        const mems = memsRes?.data || [];
        
        let allTasks = [];
        cols.forEach(c => { if(c.tasks) allTasks.push(...c.tasks); });
        
        const total = allTasks.length;
        const completed = allTasks.filter(t => {
            const col = cols.find(c => c.id === t.columnId);
            return col && /bajarildi|tugadi|done/i.test(col.title);
        }).length;
        const overdue = allTasks.filter(t => t.isOverdue).length;
        const mine = allTasks.filter(t => t.assignees && t.assignees.some(a => a.id === currentUserId)).length;
        
        const mainStats = document.createElement('div');
        mainStats.innerHTML = `
          <div class="stats-grid">
            <div class="stat-card">
              <div class="stat-num">${total}</div>
              <div class="stat-label">Jami</div>
            </div>
            <div class="stat-card">
              <div class="stat-num" style="color:#10b981;">${completed}</div>
              <div class="stat-label">Bajarilgan</div>
            </div>
            <div class="stat-card">
              <div class="stat-num" style="color:#ef4444;">${overdue}</div>
              <div class="stat-label">Kechikkan</div>
            </div>
            <div class="stat-card">
              <div class="stat-num" style="color:var(--btn-color);">${mine}</div>
              <div class="stat-label">Mening</div>
            </div>
          </div>
        `;
        
        container.innerHTML = '';
        container.appendChild(mainStats);
        
        // Chart
        const chartSection = document.createElement('div');
        chartSection.className = 'chart-section';
        chartSection.innerHTML = `<div class="chart-title">Ustunlar bo'yicha</div><div class="bar-chart"></div>`;
        const barChart = chartSection.querySelector('.bar-chart');
        
        const maxTasks = Math.max(...cols.map(c => c.tasks ? c.tasks.length : 0), 1);
        cols.forEach(c => {
            const count = c.tasks ? c.tasks.length : 0;
            const h = (count / maxTasks) * 100;
            const colTitle = c.title;
            const barCol = document.createElement('div');
            barCol.className = 'bar-col';
            barCol.innerHTML = `
                <div class="bar-val">${count}</div>
                <div class="bar" style="height:${h}%; background:${getColColor(colTitle)}"></div>
                <div class="bar-label"></div>
            `;
            const labelEl = barCol.querySelector('.bar-label');
            labelEl.textContent = colTitle;
            labelEl.title = colTitle;
            barChart.appendChild(barCol);
        });
        container.appendChild(chartSection);
        
        // Workload
        const workloadSection = document.createElement('div');
        workloadSection.className = 'chart-section';
        workloadSection.innerHTML = `<div class="chart-title">Kimga qancha ilingan?</div><div class="workload-list"></div>`;
        const workloadList = workloadSection.querySelector('.workload-list');
        
        const workload = {};
        mems.forEach(m => workload[m.id] = { name: m.fullName || m.name, count: 0 });
        allTasks.forEach(t => {
            if (t.assignees) {
                t.assignees.forEach(a => {
                    if (workload[a.id]) workload[a.id].count++;
                });
            }
        });
        Object.values(workload).sort((a,b)=>b.count-a.count).forEach(w => {
            const item = document.createElement('div');
            item.className = 'workload-item';
            item.innerHTML = `
                <div class="member-avatar" style="width:28px;height:28px;font-size:10px;color:white;background:var(--btn-color)">${getInitials(w.name)}</div>
                <div class="wl-name"></div>
                <div class="wl-count">${w.count}</div>
            `;
            item.querySelector('.wl-name').textContent = w.name;
            workloadList.appendChild(item);
        });
        
        container.appendChild(workloadSection);
        
    } catch(e) {
        container.innerHTML = `<div class="empty-placeholder">Yuklashda xatolik</div>`;
    }
}

async function checkAndRefreshBoard() {
    if (!currentWsId) return;
    const isSheetOpen = document.getElementById('bottom-sheet').classList.contains('active');
    const isDragging = document.querySelector('.sortable-ghost') !== null;
    if (isSheetOpen || isDragging) return;

    try {
        const colsRes = await api(endpoints.columns(currentWsId));
        const newColsData = colsRes?.data || [];
        
        let newTasksData = [];
        newColsData.forEach(c => {
          if (c.tasks) newTasksData.push(...c.tasks);
        });
        
        // Oddiy solishtirish
        const oldHash = JSON.stringify(tasksData.map(t => ({id: t.id, col: t.columnId, title: t.title, desc: t.description, due: t.dueDate, prio: t.priority, assignees: t.assignees?.map(a=>a.id)})));
        const newHash = JSON.stringify(newTasksData.map(t => ({id: t.id, col: t.columnId, title: t.title, desc: t.description, due: t.dueDate, prio: t.priority, assignees: t.assignees?.map(a=>a.id)})));
        
        if (oldHash !== newHash) {
            colsData = newColsData;
            tasksData = newTasksData;
            renderBoard();
        }
    } catch(e) {
        console.warn("Auto-refresh failed", e);
    }
}

document.addEventListener('visibilitychange', () => {
    if (document.visibilityState === 'visible') {
        checkAndRefreshBoard();
    }
});
setInterval(checkAndRefreshBoard, 30000);

function shareTask() {
    if (!currentTaskId || !botUsername) {
        showToast("Bot usernamesi topilmadi yoki vazifa tanlanmagan.");
        return;
    }
    const url = `https://t.me/${botUsername}?start=task_${currentTaskId}`;
    try {
        if (tg.openTelegramLink) {
            const shareUrl = `https://t.me/share/url?url=${encodeURIComponent(url)}&text=Vazifa`;
            tg.openTelegramLink(shareUrl);
        } else {
            fallbackCopy(url);
        }
    } catch(e) {
        fallbackCopy(url);
    }
}

function fallbackCopy(text) {
    const el = document.createElement('textarea');
    el.value = text;
    document.body.appendChild(el);
    el.select();
    try {
        document.execCommand('copy');
        showToast('Nusxa olindi!');
    } catch (e) {
        showToast('Nusxa olishda xatolik!');
    }
    document.body.removeChild(el);
}

window.addEventListener('offline', () => showToast("Internet tarmog'i uzildi!"));
window.addEventListener('online', () => {
    showToast("Internet tarmog'i tiklandi!");
    if (currentTab === 'board' && currentWsId) {
        checkAndRefreshBoard();
    }
});

init();
