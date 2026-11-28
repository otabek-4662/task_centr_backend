(() => {
  const controls = document.querySelector('.mouse-controls');
  const accelerator = controls.querySelector('[data-key="ArrowUp"]');
  let accelerating = false;

  function send(type, code) {
    window.dispatchEvent(new KeyboardEvent(type, { code }));
  }

  controls.querySelectorAll('button').forEach(button => {
    const code = button.dataset.key;
    if (button === accelerator) {
      button.setAttribute('aria-pressed', 'false');
      button.addEventListener('click', () => {
        accelerating = !accelerating;
        button.setAttribute('aria-pressed', String(accelerating));
        send(accelerating ? 'keydown' : 'keyup', code);
      });
      return;
    }
    button.addEventListener('pointerdown', event => {
      button.setPointerCapture(event.pointerId);
      send('keydown', code);
    });
    button.addEventListener('lostpointercapture', () => send('keyup', code));
  });

  window.addEventListener('blur', () => {
    accelerating = false;
    accelerator.setAttribute('aria-pressed', 'false');
    send('keyup', 'ArrowUp');
  });
})();
