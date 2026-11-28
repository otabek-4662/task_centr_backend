(() => {
  'use strict';
  const params = new URLSearchParams(location.hash.slice(1));
  const recordKey = params.get('key');
  const channel = params.get('channel');
  const values = new Map();
  const initialScore = params.get('record');
  if (recordKey && initialScore) values.set(recordKey, initialScore);

  // The embedded game has an opaque origin. Its own storage object never
  // grants access to the workspace's localStorage or authentication data.
  window.arcadeStorage = {
    getItem(key) {
      return values.get(key) ?? null;
    },
    setItem(key, value) {
      const record = String(value);
      values.set(key, record);
      if (window.parent !== window && channel && key === recordKey) {
        window.parent.postMessage({ type: 'choy-arcade-record', channel, record }, '*');
      }
    }
  };
})();
