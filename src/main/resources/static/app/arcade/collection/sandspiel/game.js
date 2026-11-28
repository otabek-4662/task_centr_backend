(() => {
  'use strict';

  // Max Bittker's original Sandspiel simulation runs in sandtable.wasm.
  const materials = [
    ['O‘chirg‘ich', '#18131a'], ['Devor', '#65615e'], ['Qum', '#d5aa56'],
    ['Suv', '#549eea'], ['Gaz', '#d4b1ac'], ['Nusxalovchi', '#b66da5'],
    ['Olov', '#fc572b'], ['Yog‘och', '#9c703e'], ['Lava', '#ed5926'],
    ['Muz', '#a0d9ee'], null, ['O‘simlik', '#51ab68'], ['Kislota', '#d3db46'],
    ['Tosh', '#6d777d'], ['Chang', '#b084d2'], ['Kana', '#a942b7'],
    ['Moy', '#655255'], ['Raketa', '#f1d0bb'], ['Qo‘ziqorin', '#f3a2c4'], ['Urug‘', '#7dbe68']
  ];
  const colors = materials.map(material => material
    ? material[1].slice(1).match(/../g).map(value => parseInt(value, 16))
    : null);
  const canvas = document.getElementById('sandbox');
  const status = document.getElementById('status');
  const material = document.getElementById('material');
  const size = document.getElementById('size');
  const side = canvas.width;
  const ctx = canvas.getContext('2d', { alpha: false });
  const image = ctx.createImageData(side, side);
  materials.forEach((entry, id) => {
    if (entry) material.add(new Option(entry[0], id));
  });
  material.value = '2';

  async function start() {
    let engine;
    const imports = {
      './sandtable_bg.js': {
        __wbg_random_0c73839ced1bb4a5: Math.random,
        __wbg___wbindgen_throw_dd24417ed36fc46e(pointer, length) {
          throw new Error(new TextDecoder().decode(new Uint8Array(engine.memory.buffer, pointer, length)));
        },
        __wbindgen_init_externref_table() {
          const table = engine.__wbindgen_externrefs;
          const offset = table.grow(4);
          [undefined, null, true, false].forEach((value, index) => table.set(offset + index, value));
        }
      }
    };
    const response = await fetch('sandtable.wasm');
    if (!response.ok) throw new Error('Sandspiel: ' + response.status);
    const { instance } = await WebAssembly.instantiate(await response.arrayBuffer(), imports);
    engine = instance.exports;
    engine.__wbindgen_start();
    const universe = engine.universe_new(side, side);
    for (let x = 15; x < side - 15; x += 8) {
      engine.universe_paint(universe, x, side - 28 + Math.round(Math.sin(x / 24) * 7), 17, 2);
    }
    for (let x = 38; x < side - 25; x += 53) engine.universe_paint(universe, x, side - 46, 5, 19);
    status.hidden = true;

    let x = side / 2;
    let y = side / 2;
    let pointer = null;
    let cursor = false;
    let paused = false;
    let lastTick = 0;
    function paint() {
      engine.universe_paint(universe, x, y, Number(size.value), Number(material.value));
    }
    function move(event) {
      const rect = canvas.getBoundingClientRect();
      const nextX = Math.max(0, Math.min(side - 1, Math.floor((event.clientX - rect.left) * side / rect.width)));
      const nextY = Math.max(0, Math.min(side - 1, Math.floor((event.clientY - rect.top) * side / rect.height)));
      const steps = Math.max(Math.abs(nextX - x), Math.abs(nextY - y), 1);
      if (pointer === event.pointerId) {
        const startX = x;
        const startY = y;
        for (let step = 1; step < steps; step += 2) {
          x = Math.round(startX + (nextX - startX) * step / steps);
          y = Math.round(startY + (nextY - startY) * step / steps);
          paint();
        }
      }
      x = nextX;
      y = nextY;
      if (pointer === event.pointerId) paint();
    }
    canvas.addEventListener('pointerdown', event => {
      if (pointer !== null) return;
      canvas.focus({ preventScroll: true });
      cursor = false;
      move(event);
      pointer = event.pointerId;
      canvas.setPointerCapture(pointer);
      engine.universe_push_undo(universe);
      paint();
    });
    canvas.addEventListener('pointermove', move);
    canvas.addEventListener('lostpointercapture', () => { pointer = null; });
    canvas.addEventListener('keydown', event => {
      const { key } = event;
      const offsets = { ArrowLeft: [-3, 0], ArrowRight: [3, 0], ArrowUp: [0, -3], ArrowDown: [0, 3] };
      if (key === ' ') {
        event.preventDefault();
        if (!event.repeat) engine.universe_push_undo(universe);
        paint();
      } else if (offsets[key]) {
        event.preventDefault();
        cursor = true;
        const [dx, dy] = offsets[key];
        x = Math.max(0, Math.min(side - 1, x + dx));
        y = Math.max(0, Math.min(side - 1, y + dy));
      }
    });
    document.getElementById('pause').addEventListener('click', event => {
      paused = !paused;
      event.currentTarget.textContent = paused ? 'Davom etish' : 'To‘xtatish';
      event.currentTarget.setAttribute('aria-pressed', String(paused));
    });
    document.getElementById('undo').addEventListener('click', () => engine.universe_pop_undo(universe));
    document.getElementById('reset').addEventListener('click', () => {
      engine.universe_push_undo(universe);
      engine.universe_reset(universe);
    });

    function frame(time) {
      if (time - lastTick >= 1000 / 30) {
        lastTick = time;
        if (!paused) {
          if (pointer !== null) paint();
          engine.universe_tick(universe);
        }
        const cells = new Uint8Array(engine.memory.buffer, engine.universe_cells(universe), side * side * 4);
        for (let py = 0; py < side; py++) {
          for (let px = 0; px < side; px++) {
            const cell = (px * side + py) * 4;
            const pixel = (py * side + px) * 4;
            const color = colors[cells[cell]] || colors[0];
            const shade = cells[cell] === 0 ? 1 : 0.72 + cells[cell + 1] / 450;
            image.data[pixel] = color[0] * shade;
            image.data[pixel + 1] = color[1] * shade;
            image.data[pixel + 2] = color[2] * shade;
            image.data[pixel + 3] = 255;
          }
        }
        ctx.putImageData(image, 0, 0);
        if (cursor && document.activeElement === canvas) {
          ctx.strokeStyle = '#fff';
          ctx.strokeRect(x - 5, y - 5, 10, 10);
        }
      }
      requestAnimationFrame(frame);
    }
    requestAnimationFrame(frame);
  }

  start().catch(error => {
    status.textContent = 'Sandspiel ochilmadi. O‘yinni qaytadan yuklang.';
    console.error(error);
  });
})();
