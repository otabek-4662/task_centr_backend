(() => {
  'use strict';
  const params = new URLSearchParams(location.hash.slice(1));
  const recordKey = params.get('key');
  const saveKey = params.get('saveKey');
  const channel = params.get('channel');
  const values = new Map();
  const initialScore = params.get('record');
  if (recordKey && initialScore) values.set(recordKey, initialScore);
  const initialSave = params.get('save');
  if (saveKey && initialSave) values.set(saveKey, initialSave);

  // The embedded game has an opaque origin. Its own storage object never
  // grants access to the workspace's localStorage or authentication data.
  window.arcadeStorage = {
    getItem(key) {
      return values.get(key) ?? null;
    },
    setItem(key, value) {
      const record = String(value);
      values.set(key, record);
      if (window.parent === window || !channel) return;
      if (key === recordKey) window.parent.postMessage({ type: 'choy-arcade-record', channel, record }, '*');
      if (key === saveKey) window.parent.postMessage({ type: 'choy-arcade-save', channel, record }, '*');
    }
  };
})();
