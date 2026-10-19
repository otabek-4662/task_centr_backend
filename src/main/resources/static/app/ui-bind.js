// Event delegation binder compliant with Content-Security-Policy (no inline onclick/onchange)
(function () {
  'use strict';

  function call(name) {
    var fn = window[name];
    if (typeof fn !== 'function') {
      console.warn('ui-bind: missing function', name);
      return;
    }
    return fn.apply(null, Array.prototype.slice.call(arguments, 1));
  }

  document.addEventListener('click', function (e) {
    var el = e.target.closest('[data-action]');
    if (!el) return;
    switch (el.dataset.action) {
      case 'switch-auth-mode':
        call('switchAuthMode', el.dataset.mode);
        break;
      case 'submit-auth':
        call('handleLogin');
        break;
      case 'demo-login':
        call('handleDemoLogin');
        break;
      case 'admin-login':
        call('handleAdminLogin');
        break;
      case 'toggle-theme':
        call('toggleTheme');
        break;
      case 'toggle-hero':
        call('toggleHeroShowcase');
        break;
      case 'piyola-interact':
        call('interactWithPiyola');
        break;
      case 'play-game':
        call('playGame', el.dataset.game);
        break;
      case 'restart-game':
        call('restartCurrentGame');
        break;
      case 'close-game-stage':
        call('closeGameStage');
        break;
      case 'game-ctrl':
        call('handleGameCtrl', el.dataset.dir);
        break;
      case 'toggle-games-menu':
        call('toggleGamesMenu');
        break;
      case 'game-cell':
      case 'game-cell-click':
        call('handleGameCellClick', el);
        break;
      case 'game-mode-toggle':
        call('toggleGameMode');
        break;
      case 'game-num-select':
        call('handleGameNumSelect', el.dataset.num);
        break;
      case 'open-ws-modal':
        call('openWorkspaceModal');
        break;
      case 'open-notifications':
        call('openNotificationsModal');
        break;
      case 'refresh-board':
        call('refreshCurrentBoard');
        break;
      case 'refresh-sprints':
        call('loadSprintsTab');
        break;
      case 'open-sprint-modal':
        call('openSprintModal');
        break;
      case 'refresh-mytasks':
        call('loadMyTasks');
        break;
      case 'refresh-chat':
        call('loadChatTab');
        break;
      case 'open-add-contact-modal':
        call('openAddContactModal');
        break;
      case 'open-change-password':
        call('openChangePasswordModal');
        break;
      case 'logout-user':
        call('promptLogout');
        break;
      case 'chat-channel':
        call('switchChatChannel', el.dataset.channel);
        break;
      case 'send-chat-msg':
        call('sendChatMessage');
        break;
      case 'cancel-chat-reply':
        call('cancelChatReply');
        break;
      case 'export-csv':
        call('exportWorkspaceCsv');
        break;
      case 'export-pdf':
        call('exportWorkspacePdf');
        break;
      case 'start-focus-timer':
        call('startFocusTimerForCurrentTask');
        break;
      case 'stop-focus-timer':
        call('stopActiveFocusTimer');
        break;
      case 'add-dependency':
        call('addTaskDependency');
        break;
      case 'refresh-workspace-tab':
        call('loadWorkspaceSettingsTab');
        break;
      case 'admin-refresh':
        call('loadAdminDashboard');
        break;
      case 'admin-evict-cache':
        call('evictAdminCache');
        break;
      case 'admin-switch-subtab':
        call('switchAdminSubtab', el.dataset.subtab);
        break;
      case 'admin-user-search-btn':
        call('searchAdminUsers');
        break;
      case 'admin-ws-search-btn':
        call('searchAdminWorkspaces');
        break;
      case 'admin-audit-refresh':
        call('loadAdminAuditLogs');
        break;
      case 'admin-send-broadcast':
        call('sendAdminBroadcast');
        break;
      case 'filter':
        call('setFilter', el);
        break;
      case 'add-column':
        call('promptAddColumn');
        break;
      case 'new-task':
        call('openSheet', 'create');
        break;
      case 'save-task':
        call('saveTask');
        break;
      case 'tab':
        call('switchTab', el.dataset.tab);
        break;
      case 'detail-subtab':
        call('switchDetailSubtab', el.dataset.subtab);
        break;
      case 'close-sheet':
        call('closeSheet');
        break;
      case 'share-task':
        call('shareTask');
        break;
      case 'edit-task':
        call('editCurrentTask');
        break;
      case 'delete-task':
        call('deleteTask');
        break;
      case 'toggle-watch':
        call('toggleWatchTask');
        break;
      case 'toggle-archive':
        call('toggleArchiveTask');
        break;
      case 'add-checklist':
        call('addChecklist');
        break;
      case 'add-subtask':
        call('addSubtask');
        break;
      case 'add-worklog':
        call('addWorklog');
        break;
      case 'add-comment':
        call('addComment');
        break;
      case 'upload-attachment':
        call('uploadTaskAttachment');
        break;
      case 'qa-edit':
        call('closeSheet');
        setTimeout(function () {
          call('editCurrentTask');
        }, 300);
        break;
    }
  });

  document.addEventListener('keydown', function (e) {
    if ((e.key === 'Enter' || e.key === ' ') && e.target.matches('.tab-btn')) {
      e.preventDefault();
      call('switchTab', e.target.dataset.tab);
    }
    if (e.key === 'Escape') {
      var overlay = document.getElementById('sheet-overlay');
      if (overlay && overlay.classList.contains('active')) {
        call('closeSheet');
      }
    }
    if (e.key === 'Enter') {
      if (e.target.id === 'login-password' || e.target.id === 'login-username') {
        e.preventDefault();
        call('handleLogin');
      } else if (e.target.id === 'chat-msg-input') {
        e.preventDefault();
        call('sendChatMessage');
      } else if (e.target.id === 'checklist-input') {
        e.preventDefault();
        call('addChecklist');
      } else if (e.target.id === 'subtask-input') {
        e.preventDefault();
        call('addSubtask');
      } else if (e.target.id === 'dependency-input') {
        e.preventDefault();
        call('addTaskDependency');
      } else if (e.target.id === 'worklog-desc-input' || e.target.id === 'worklog-hours-input') {
        e.preventDefault();
        call('addWorklog');
      } else if (e.target.id === 'comment-input') {
        e.preventDefault();
        call('addComment');
      } else if (e.target.id === 'admin-user-search') {
        e.preventDefault();
        call('searchAdminUsers');
      } else if (e.target.id === 'admin-ws-search') {
        e.preventDefault();
        call('searchAdminWorkspaces');
      }
    }
  });

  document.addEventListener('input', function (e) {
    if (e.target.id === 'search-input') call('applyFilters');
  });

  document.addEventListener('change', function (e) {
    var t = e.target;
    if (t.id === 'ws' || t.id === 'ws-desktop') call('selectWorkspace', t.value);
    else if (t.id === 'stats-ws') call('loadStats');
    else if (t.id === 'board-sprint-select' || t.id === 'filter-direction' || t.id === 'filter-label') {
      call('applyFilters');
    } else if (t.id === 'd-col-select') {
      call('moveTaskFromSelect', t.value);
    } else if (t.id === 'qa-col-select') {
      Promise.resolve(call('moveTaskFromSelect', t.value)).then(function () { call('closeSheet'); });
    }
  });
})();
