// Replaces the inline onclick/oninput/onchange attributes that the page's
// Content-Security-Policy blocks. Calls the existing global functions in app.js.
(function () {
  'use strict';

  function call(name) {
    var fn = window[name];
    if (typeof fn !== 'function') { console.warn('ui-bind: missing function', name); return; }
    return fn.apply(null, Array.prototype.slice.call(arguments, 1));
  }

  document.addEventListener('click', function (e) {
    var el = e.target.closest('[data-action]');
    if (!el) return;
    switch (el.dataset.action) {
      case 'refresh-board':
        if (typeof currentWsId !== 'undefined' && currentWsId) call('loadBoard', currentWsId);
        break;
      case 'refresh-mytasks': call('loadMyTasks'); break;
      case 'filter': call('setFilter', el); break;
      case 'new-task': call('openSheet', 'create'); break;
      case 'tab': call('switchTab', el.dataset.tab); break;
      case 'close-sheet': call('closeSheet'); break;
      case 'share-task': call('shareTask'); break;
      case 'edit-task':
        if (typeof currentTaskId !== 'undefined') call('openSheet', 'edit', currentTaskId);
        break;
      case 'delete-task': call('deleteTask'); break;
      case 'add-checklist': call('addChecklist'); break;
      case 'add-comment': call('addComment'); break;
      case 'qa-edit':
        call('closeSheet');
        setTimeout(function () {
          if (typeof currentTaskId !== 'undefined') call('openSheet', 'edit', currentTaskId);
        }, 300);
        break;
    }
  });

  // keyboard access for the tab bar (role="button" divs)
  document.addEventListener('keydown', function (e) {
    if ((e.key === 'Enter' || e.key === ' ') && e.target.matches('.tab-btn')) {
      e.preventDefault();
      call('switchTab', e.target.dataset.tab);
    }
  });

  document.addEventListener('input', function (e) {
    if (e.target.id === 'search-input') call('applyFilters');
  });

  document.addEventListener('change', function (e) {
    var t = e.target;
    if (t.id === 'stats-ws') call('loadStats');
    else if (t.id === 'd-col-select') call('moveTaskFromSelect', t.value);
    else if (t.id === 'qa-col-select') {
      Promise.resolve(call('moveTaskFromSelect', t.value)).then(function () { call('closeSheet'); });
    }
  });
})();
