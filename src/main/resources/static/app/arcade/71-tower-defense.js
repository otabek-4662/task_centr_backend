// Arcade 100 · MIT · see LICENSE
(function () {
  'use strict';

  /* ============================ constants ============================ */
  const W = 960, H = 640, T = 40, COLS = 24, ROWS = 16;
  const PREP_TIME = 12;
  const MAX_WAVE = 20;
  const START_GOLD = 180;
  const START_LIVES = 20;

  const $ = function (id) { return document.getElementById(id); };
  const clamp = function (v, a, b) { return v < a ? a : v > b ? b : v; };
  const lerp = function (a, b, t) { return a + (b - a) * t; };
  const rnd = function (a, b) { return a + Math.random() * (b - a); };

  function roundRect(c, x, y, w, h, r) {
    r = Math.min(r, w / 2, h / 2);
    c.beginPath();
    c.moveTo(x + r, y);
    c.arcTo(x + w, y, x + w, y + h, r);
    c.arcTo(x + w, y + h, x, y + h, r);
    c.arcTo(x, y + h, x, y, r);
    c.arcTo(x, y, x + w, y, r);
    c.closePath();
  }

  function rotToward(a, b, max) {
    let d = ((b - a + Math.PI) % (Math.PI * 2) + Math.PI * 2) % (Math.PI * 2) - Math.PI;
    if (Math.abs(d) <= max) return b;
    return a + Math.sign(d) * max;
  }

  /* ============================ the road ============================ */
  const WPT = [[-1, 2], [3, 2], [3, 7], [8, 7], [8, 2], [13, 2], [13, 11], [18, 11], [18, 4], [21, 4], [21, 13], [23, 13]];
  const WP = WPT.map(function (p) { return { x: p[0] * T + T / 2, y: p[1] * T + T / 2 }; });

  const CUM = [0];
  for (let i = 1; i < WP.length; i++) {
    CUM.push(CUM[i - 1] + Math.hypot(WP[i].x - WP[i - 1].x, WP[i].y - WP[i - 1].y));
  }
  const PATH_LEN = CUM[CUM.length - 1];

  function posAt(d) {
    if (d <= 0) return { x: WP[0].x, y: WP[0].y, a: 0 };
    if (d >= PATH_LEN) {
      const p = WP[WP.length - 1];
      return { x: p.x, y: p.y, a: 0 };
    }
    let i = 1;
    while (i < WP.length - 1 && d > CUM[i]) i++;
    const seg = CUM[i] - CUM[i - 1] || 1;
    const t = (d - CUM[i - 1]) / seg;
    const a = WP[i - 1], b = WP[i];
    return {
      x: a.x + (b.x - a.x) * t,
      y: a.y + (b.y - a.y) * t,
      a: Math.atan2(b.y - a.y, b.x - a.x)
    };
  }

  /* tiles occupied by the road cannot hold towers */
  const blocked = new Uint8Array(COLS * ROWS);
  (function markPath() {
    for (let s = 0; s < WPT.length - 1; s++) {
      let x = WPT[s][0], y = WPT[s][1];
      const x2 = WPT[s + 1][0], y2 = WPT[s + 1][1];
      const dx = Math.sign(x2 - x), dy = Math.sign(y2 - y);
      if (x >= 0 && x < COLS && y >= 0 && y < ROWS) blocked[y * COLS + x] = 1;
      while (x !== x2 || y !== y2) {
        x += dx; y += dy;
        if (x >= 0 && x < COLS && y >= 0 && y < ROWS) blocked[y * COLS + x] = 1;
      }
    }
  })();

  /* ============================ definitions ============================ */
  const TDEF = {
    arrow: {
      name: 'Archer', cost: 50, dmg: 10, cd: 0.45, range: 118,
      proj: 'arrow', pspd: 620, color: '#7fd1ff', desc: 'Fast single target'
    },
    cannon: {
      name: 'Mortar', cost: 95, dmg: 28, cd: 1.55, range: 136,
      splash: 52, proj: 'shell', pspd: 360, color: '#ffcf6b', desc: 'Splash damage'
    },
    frost: {
      name: 'Frost', cost: 75, dmg: 5, cd: 0.8, range: 108,
      slow: 0.45, slowDur: 1.5, proj: 'shard', pspd: 480, color: '#9be8ff', desc: 'Slows enemies'
    }
  };

  const EDEF = {
    grunt: { hp: 44, speed: 86, gold: 8, r: 11, leak: 1, score: 10, color: '#68d97e', light: '#b6f5c2', ring: '#2f7a45' },
    runner: { hp: 30, speed: 150, gold: 7, r: 9, leak: 1, score: 12, color: '#63cdf7', light: '#c2f0ff', ring: '#2a6f92' },
    tank: { hp: 210, speed: 62, gold: 20, r: 14, leak: 2, score: 30, color: '#f7a95c', light: '#ffd9ae', ring: '#8d5322' },
    boss: { hp: 1800, speed: 54, gold: 120, r: 19, leak: 6, score: 200, color: '#ff6f93', light: '#ffc2d2', ring: '#8e2f4b' }
  };

  const WAVES = [
    [{ t: 'grunt', n: 6, gap: 0.9 }],
    [{ t: 'grunt', n: 9, gap: 0.75 }],
    [{ t: 'runner', n: 7, gap: 0.55 }],
    [{ t: 'grunt', n: 8, gap: 0.7 }, { t: 'runner', n: 5, gap: 0.5, at: 5 }],
    [{ t: 'tank', n: 2, gap: 2.4 }, { t: 'grunt', n: 7, gap: 0.7, at: 1.5 }],
    [{ t: 'runner', n: 12, gap: 0.42 }],
    [{ t: 'tank', n: 3, gap: 2.0 }, { t: 'runner', n: 7, gap: 0.5, at: 3 }],
    [{ t: 'grunt', n: 16, gap: 0.45 }],
    [{ t: 'tank', n: 4, gap: 1.8 }, { t: 'grunt', n: 9, gap: 0.6, at: 2 }],
    [{ t: 'runner', n: 16, gap: 0.34 }, { t: 'tank', n: 2, gap: 2, at: 4 }],
    [{ t: 'grunt', n: 18, gap: 0.4 }, { t: 'runner', n: 8, gap: 0.4, at: 6 }],
    [{ t: 'tank', n: 5, gap: 1.6 }, { t: 'runner', n: 10, gap: 0.42, at: 3 }],
    [{ t: 'runner', n: 20, gap: 0.3 }, { t: 'grunt', n: 10, gap: 0.5, at: 4 }],
    [{ t: 'tank', n: 6, gap: 1.5 }, { t: 'grunt', n: 14, gap: 0.4, at: 2 }],
    [{ t: 'runner', n: 24, gap: 0.26 }, { t: 'tank', n: 3, gap: 1.8, at: 5 }],
    [{ t: 'tank', n: 7, gap: 1.4 }, { t: 'runner', n: 14, gap: 0.35, at: 3 }],
    [{ t: 'grunt', n: 26, gap: 0.3 }, { t: 'tank', n: 4, gap: 1.6, at: 4 }],
    [{ t: 'runner', n: 30, gap: 0.22 }, { t: 'tank', n: 5, gap: 1.5, at: 4 }],
    [{ t: 'tank', n: 10, gap: 1.2 }, { t: 'runner', n: 20, gap: 0.3, at: 3 }],
    [{ t: 'boss', n: 1, gap: 1 }, { t: 'tank', n: 6, gap: 1.5, at: 3 }, { t: 'runner', n: 18, gap: 0.3, at: 6 }]
  ];

  /* ============================ state ============================ */
  function newState() {
    return {
      gold: START_GOLD, lives: START_LIVES, wave: 0, score: 0,
      mode: 'prep',                    // prep | wave | over | win
      enemies: [], towers: [], shots: [], parts: [], texts: [],
      queue: [], qt: 0, cool: 0,
      speed: 1, paused: false,
      banner: null
    };
  }

  let G = newState();
  let started = false;
  let buildType = null;
  let selected = null;
  let hoverCell = null;
  let shake = 0;
  let lastT = 0;
  let animT = 0;

  /* ============================ canvas ============================ */
  const cv = $('game');
  const ctx = cv.getContext('2d');

  const bgCanvas = document.createElement('canvas');
  const bgCtx = bgCanvas.getContext('2d');
  let hintCanvas = null;

  function fitCanvas() {
    const dpr = Math.min(window.devicePixelRatio || 1, 2);
    cv.width = Math.round(W * dpr);
    cv.height = Math.round(H * dpr);
    ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
    ctx.imageSmoothingEnabled = true;
  }

  function strokeRoad(c, color, width) {
    c.save();
    c.strokeStyle = color;
    c.lineWidth = width;
    c.lineCap = 'round';
    c.lineJoin = 'round';
    c.beginPath();
    c.moveTo(WP[0].x, WP[0].y);
    for (let i = 1; i < WP.length; i++) c.lineTo(WP[i].x, WP[i].y);
    c.stroke();
    c.restore();
  }

  function drawKeep(c) {
    const p = WP[WP.length - 1];
    c.save();
    c.translate(p.x, p.y);

    const glow = c.createRadialGradient(0, 0, 4, 0, 0, 46);
    glow.addColorStop(0, 'rgba(110,200,255,.30)');
    glow.addColorStop(1, 'rgba(110,200,255,0)');
    c.fillStyle = glow;
    c.beginPath(); c.arc(0, 0, 46, 0, 7); c.fill();

    c.fillStyle = '#15222f';
    roundRect(c, -17, -17, 34, 34, 7); c.fill();
    c.strokeStyle = '#5ea8d8'; c.lineWidth = 2.5; c.stroke();

    c.fillStyle = '#5ea8d8';
    for (let i = -1; i <= 1; i++) c.fillRect(i * 11 - 3, -22, 6, 7);

    c.fillStyle = '#0b131b';
    roundRect(c, -6, 1, 12, 16, 5); c.fill();

    c.strokeStyle = 'rgba(140,215,255,.75)';
    c.lineWidth = 1.5;
    c.beginPath(); c.moveTo(0, -16); c.lineTo(0, -27); c.stroke();
    c.fillStyle = '#8fd8ff';
    c.beginPath(); c.moveTo(0, -27); c.lineTo(11, -23); c.lineTo(0, -19); c.closePath(); c.fill();

    c.restore();
  }

  function buildBackground() {
    bgCanvas.width = W;
    bgCanvas.height = H;
    const c = bgCtx;

    const g = c.createLinearGradient(0, 0, 0, H);
    g.addColorStop(0, '#16242e');
    g.addColorStop(0.55, '#122029');
    g.addColorStop(1, '#0d1820');
    c.fillStyle = g;
    c.fillRect(0, 0, W, H);

    for (let i = 0; i < 5; i++) {
      const x = rnd(80, W - 80), y = rnd(80, H - 80), r = rnd(130, 280);
      const rg = c.createRadialGradient(x, y, 0, x, y, r);
      rg.addColorStop(0, 'rgba(90,160,200,.055)');
      rg.addColorStop(1, 'rgba(90,160,200,0)');
      c.fillStyle = rg;
      c.beginPath(); c.arc(x, y, r, 0, 7); c.fill();
    }

    for (let i = 0; i < 2400; i++) {
      const x = rnd(0, W), y = rnd(0, H);
      c.fillStyle = Math.random() < 0.55 ? 'rgba(120,190,160,.06)' : 'rgba(0,0,0,.16)';
      c.fillRect(x, y, rnd(1, 2.4), rnd(1, 2.4));
    }

    c.strokeStyle = 'rgba(150,200,255,.035)';
    c.lineWidth = 1;
    for (let x = 0; x <= COLS; x++) {
      c.beginPath(); c.moveTo(x * T + 0.5, 0); c.lineTo(x * T + 0.5, H); c.stroke();
    }
    for (let y = 0; y <= ROWS; y++) {
      c.beginPath(); c.moveTo(0, y * T + 0.5); c.lineTo(W, y * T + 0.5); c.stroke();
    }

    strokeRoad(c, '#0a1219', 46);
    strokeRoad(c, '#2a3446', 40);
    strokeRoad(c, '#35415a', 33);

    c.save();
    c.setLineDash([13, 19]);
    strokeRoad(c, 'rgba(190,215,255,.15)', 2.5);
    c.restore();

    /* spawn gate at the left edge */
    c.save();
    c.translate(0, WP[0].y);
    c.fillStyle = '#0a1219';
    roundRect(c, -10, -24, 22, 48, 9); c.fill();
    c.strokeStyle = 'rgba(255,120,150,.32)';
    c.lineWidth = 2;
    c.stroke();
    const gg = c.createLinearGradient(0, -24, 0, 24);
    gg.addColorStop(0, 'rgba(255,110,140,0)');
    gg.addColorStop(0.5, 'rgba(255,110,140,.16)');
    gg.addColorStop(1, 'rgba(255,110,140,0)');
    c.fillStyle = gg;
    c.fillRect(-10, -24, 22, 48);
    c.restore();

    drawKeep(c);

    const vg = c.createRadialGradient(W / 2, H / 2, H * 0.35, W / 2, H / 2, H * 0.95);
    vg.addColorStop(0, 'rgba(0,0,0,0)');
    vg.addColorStop(1, 'rgba(0,0,0,.55)');
    c.fillStyle = vg;
    c.fillRect(0, 0, W, H);
  }

  function buildHint() {
    hintCanvas = document.createElement('canvas');
    hintCanvas.width = W;
    hintCanvas.height = H;
    const c = hintCanvas.getContext('2d');
    for (let y = 0; y < ROWS; y++) {
      for (let x = 0; x < COLS; x++) {
        if (blocked[y * COLS + x]) continue;
        roundRect(c, x * T + 4, y * T + 4, T - 8, T - 8, 6);
        c.fillStyle = 'rgba(95,224,160,.055)';
        c.fill();
        c.strokeStyle = 'rgba(95,224,160,.13)';
        c.lineWidth = 1;
        c.stroke();
      }
    }
  }

  /* ============================ audio ============================ */
  let actx = null, muted = false, lastSfx = 0;

  function audioCtx() {
    if (!actx) {
      try { actx = new (window.AudioContext || window.webkitAudioContext)(); }
      catch (e) { actx = null; }
    }
    return actx;
  }

  const SFX = {
    shoot: [640, 0.05, 'square', 0.018, 220],
    boom: [150, 0.18, 'sawtooth', 0.03, -70],
    frost: [1180, 0.07, 'sine', 0.016, -320],
    kill: [430, 0.08, 'triangle', 0.022, 200],
    leak: [95, 0.34, 'sawtooth', 0.045, -40],
    place: [520, 0.1, 'triangle', 0.035, 170],
    deny: [170, 0.12, 'square', 0.028, -70],
    waveStart: [330, 0.24, 'triangle', 0.04, 150],
    waveClear: [660, 0.3, 'sine', 0.035, 240],
    upgrade: [780, 0.15, 'triangle', 0.035, 200]
  };

  function sfx(name) {
    if (muted) return;
    try { if (arcadeStorage.getItem('aaa_sfx') === '0') return; } catch (e) {}
    const a = audioCtx();
    if (!a) return;
    if (a.state === 'suspended') a.resume();
    const now = performance.now();
    if (name === 'shoot' && now - lastSfx < 55) return;
    lastSfx = now;
    const conf = SFX[name];
    if (!conf) return;
    const f = conf[0], dur = conf[1], type = conf[2], vol = conf[3], slide = conf[4];
    try {
      const o = a.createOscillator(), g = a.createGain();
      o.type = type;
      o.frequency.setValueAtTime(f, a.currentTime);
      if (slide) o.frequency.exponentialRampToValueAtTime(Math.max(40, f + slide), a.currentTime + dur);
      g.gain.setValueAtTime(vol, a.currentTime);
      g.gain.exponentialRampToValueAtTime(0.0001, a.currentTime + dur);
      o.connect(g);
      g.connect(a.destination);
      o.start();
      o.stop(a.currentTime + dur + 0.03);
    } catch (e) { /* ignore */ }
  }

  // short multi-note stinger for win/lose
  function sting(winSound) {
    if (muted) return;
    try { if (arcadeStorage.getItem('aaa_sfx') === '0') return; } catch (e) {}
    const a = audioCtx();
    if (!a) return;
    if (a.state === 'suspended') a.resume();
    const seq = winSound ? [523, 659, 784, 1046] : [330, 262, 208, 156];
    seq.forEach(function (f, i) {
      try {
        const t = a.currentTime + i * 0.12;
        const o = a.createOscillator(), g = a.createGain();
        o.type = winSound ? 'triangle' : 'sawtooth';
        o.frequency.setValueAtTime(f, t);
        g.gain.setValueAtTime(0.0001, t);
        g.gain.linearRampToValueAtTime(0.05, t + 0.015);
        g.gain.exponentialRampToValueAtTime(0.0006, t + 0.3);
        o.connect(g); g.connect(a.destination);
        o.start(t); o.stop(t + 0.34);
      } catch (e) {}
    });
  }

  /* ============================ tower stats ============================ */
  function stats(t) {
    const d = TDEF[t.type];
    const L = t.level - 1;
    return {
      dmg: d.dmg * Math.pow(1.6, L),
      cd: d.cd * Math.pow(0.86, L),
      range: d.range * Math.pow(1.07, L),
      splash: d.splash ? d.splash * Math.pow(1.12, L) : 0,
      slow: d.slow ? Math.min(0.75, d.slow + 0.07 * L) : 0,
      slowDur: d.slowDur || 0
    };
  }

  function upCost(t) {
    if (t.level >= 3) return null;
    return Math.round(TDEF[t.type].cost * (0.8 + 0.55 * (t.level - 1)));
  }

  function sellValue(t) {
    return Math.floor(t.invested * 0.6);
  }

  function towerAt(cx, cy) {
    for (let i = 0; i < G.towers.length; i++) {
      const t = G.towers[i];
      if (t.cx === cx && t.cy === cy) return t;
    }
    return null;
  }

  /* ============================ particles / text ============================ */
  function burst(x, y, color, n) {
    n = n || 10;
    for (let i = 0; i < n; i++) {
      const a = rnd(0, Math.PI * 2), s = rnd(25, 130);
      G.parts.push({
        kind: 'dot', x: x, y: y,
        vx: Math.cos(a) * s, vy: Math.sin(a) * s,
        life: rnd(0.25, 0.6), max: 0.6,
        r: rnd(1.4, 3.2), color: color
      });
    }
  }

  function spark(x, y, color) {
    for (let i = 0; i < 4; i++) {
      const a = rnd(0, Math.PI * 2), s = rnd(20, 70);
      G.parts.push({
        kind: 'dot', x: x, y: y,
        vx: Math.cos(a) * s, vy: Math.sin(a) * s,
        life: rnd(0.12, 0.3), max: 0.3,
        r: rnd(1, 2.2), color: color
      });
    }
  }

  function ring(x, y, radius, color) {
    G.parts.push({ kind: 'ring', x: x, y: y, r: radius, life: 0.38, max: 0.38, color: color });
  }

  function addText(x, y, text, color) {
    G.texts.push({ x: x, y: y, text: text, color: color, vy: -28, life: 0.85, max: 0.85 });
  }

  /* ============================ gameplay ============================ */
  function spawnEnemy(type) {
    const d = EDEF[type];
    const scale = 1 + 0.16 * (G.wave - 1);
    const hp = Math.round(d.hp * scale);
    G.enemies.push({
      type: type, x: WP[0].x, y: WP[0].y, a: 0, dist: 0,
      hp: hp, maxhp: hp,
      speed: d.speed * (1 + 0.012 * (G.wave - 1)),
      r: d.r, color: d.color, light: d.light, ring: d.ring,
      gold: d.gold, score: d.score, leak: d.leak,
      slow: 0, slowT: 0, flash: 0, alive: true
    });
  }

  let siegeWaves = false;
  function startWave() {
    if (G.mode !== 'prep') return;
    const bonus = Math.floor(G.cool) * 2;
    if (bonus > 0) {
      G.gold += bonus;
      addText(WP[0].x + 70, WP[0].y - 14, '+' + bonus, '#ffcc57');
    }
    G.wave++;
    const groups = WAVES[G.wave - 1];
    const events = [];
    for (let i = 0; i < groups.length; i++) {
      const g = groups[i];
      const start = g.at || 0;
      for (let k = 0; k < g.n; k++) events.push({ t: start + k * g.gap, type: g.t });
    }
    if (siegeWaves && G.wave % 4 === 0 && events.length) {
      const lastEvent = events.reduce((last, event) => event.t > last.t ? event : last);
      events.push({t: lastEvent.t + 0.7, type: lastEvent.type}, {t: lastEvent.t + 1.4, type: lastEvent.type});
    }
    events.sort(function (a, b) { return a.t - b.t; });
    G.queue = events;
    G.qt = 0;
    G.cool = 0;
    G.mode = 'wave';
    banner('WAVE ' + G.wave, '#6fc6ff', 1.6);
    sfx('waveStart');
  }

  function banner(text, color, dur) {
    G.banner = { text: text, color: color, life: dur, max: dur };
  }

  function damage(e, dmg) {
    if (!e.alive) return;
    e.hp -= dmg;
    e.flash = 0.14;
    if (e.hp <= 0) {
      e.alive = false;
      G.gold += e.gold;
      G.score += e.score;
      burst(e.x, e.y, e.color, e.type === 'boss' ? 34 : 12);
      addText(e.x, e.y - e.r, '+' + e.gold, '#ffd479');
      sfx('kill');
    }
  }

  function fire(t, s, target) {
    const def = TDEF[t.type];
    const ang = Math.atan2(target.y - t.y, target.x - t.x);
    t.aim = ang;
    G.shots.push({
      x: t.x + Math.cos(ang) * 13,
      y: t.y + Math.sin(ang) * 13,
      tx: target.x, ty: target.y, target: target,
      ang: ang, speed: def.pspd,
      dmg: s.dmg, splash: s.splash, slow: s.slow, slowDur: s.slowDur,
      kind: def.proj, color: def.color, alive: true
    });
    t.flash = 1;
    sfx(t.type === 'cannon' ? 'boom' : t.type === 'frost' ? 'frost' : 'shoot');
  }

  function impact(p, x, y) {
    if (p.splash > 0) {
      for (let i = 0; i < G.enemies.length; i++) {
        const e = G.enemies[i];
        if (!e.alive) continue;
        const d = Math.hypot(e.x - x, e.y - y);
        if (d <= p.splash + e.r) damage(e, p.dmg * (1 - 0.4 * Math.min(1, d / p.splash)));
      }
      ring(x, y, p.splash, '#ffcf6b');
      burst(x, y, '#ffd79a', 8);
      sfx('boom');
    } else if (p.target && p.target.alive) {
      damage(p.target, p.dmg);
      if (p.slow > 0) {
        p.target.slow = Math.max(p.target.slow, p.slow);
        p.target.slowT = Math.max(p.target.slowT, p.slowDur);
      }
      spark(x, y, p.color);
    }
  }

  function updateEnemies(dt) {
    let leak = 0;
    const base = WP[WP.length - 1];
    for (let i = 0; i < G.enemies.length; i++) {
      const e = G.enemies[i];
      if (!e.alive) continue;
      if (e.slowT > 0) e.slowT -= dt;
      if (e.flash > 0) e.flash -= dt;
      const mul = e.slowT > 0 ? 1 - e.slow : 1;
      e.dist += e.speed * mul * dt;
      if (e.dist >= PATH_LEN) {
        e.dist = PATH_LEN;
        e.x = base.x;
        e.y = base.y;
        e.alive = false;
        leak += e.leak;
        burst(base.x, base.y, '#ff6b8a', 16);
        ring(base.x, base.y, 34, '#ff6b8a');
        continue;
      }
      const p = posAt(e.dist);
      e.x = p.x;
      e.y = p.y;
      e.a = p.a;
    }
    if (leak > 0) {
      G.lives = Math.max(0, G.lives - leak);
      shake = Math.max(shake, 8);
      sfx('leak');
      if (G.lives <= 0) { gameOver(); }
    }
    let anyDead = false;
    for (let i = 0; i < G.enemies.length; i++) if (!G.enemies[i].alive) { anyDead = true; break; }
    if (anyDead) G.enemies = G.enemies.filter(function (e) { return e.alive; });
  }

  function updateTowers(dt) {
    for (let i = 0; i < G.towers.length; i++) {
      const t = G.towers[i];
      const s = stats(t);
      t.cool -= dt;
      if (t.flash > 0) t.flash -= dt * 5;

      let target = null, best = -1;
      for (let k = 0; k < G.enemies.length; k++) {
        const e = G.enemies[k];
        if (!e.alive) continue;
        const dx = e.x - t.x, dy = e.y - t.y;
        const rr = s.range + e.r * 0.6;
        const priority = t.type === "arrow" ? e.dist + (e.hp < s.dmg ? s.range * 0.58 : 0) : t.type === "frost" ? e.dist + (e.slow <= 0 ? s.range * 0.55 : 0) : e.dist;
        if (dx * dx + dy * dy <= rr * rr && priority > best) {
          best = priority;
          target = e;
        }
      }

      if (target) {
        const want = Math.atan2(target.y - t.y, target.x - t.x);
        t.aim = rotToward(t.aim, want, 10 * dt);
        if (t.cool <= 0) {
          fire(t, s, target);
          t.cool = s.cd;
        }
      }
      if (t.cool < -1) t.cool = -1;
    }
  }

  function updateShots(dt) {
    for (let i = 0; i < G.shots.length; i++) {
      const p = G.shots[i];
      if (!p.alive) continue;
      if (p.target && p.target.alive) {
        p.tx = p.target.x;
        p.ty = p.target.y;
      }
      const dx = p.tx - p.x, dy = p.ty - p.y;
      const d = Math.hypot(dx, dy) || 1;
      const step = p.speed * dt;
      if (d <= step + 2) {
        impact(p, p.tx, p.ty);
        p.alive = false;
        continue;
      }
      p.x += dx / d * step;
      p.y += dy / d * step;
      p.ang = Math.atan2(dy, dx);
    }
    let anyDead = false;
    for (let i = 0; i < G.shots.length; i++) if (!G.shots[i].alive) { anyDead = true; break; }
    if (anyDead) G.shots = G.shots.filter(function (p) { return p.alive; });
  }

  function updateFx(dt) {
    for (let i = 0; i < G.parts.length; i++) {
      const p = G.parts[i];
      p.life -= dt;
      if (p.kind === 'dot') {
        p.x += p.vx * dt;
        p.y += p.vy * dt;
        p.vx *= 0.97;
        p.vy *= 0.97;
      }
    }
    G.parts = G.parts.filter(function (p) { return p.life > 0; });

    for (let i = 0; i < G.texts.length; i++) {
      const t = G.texts[i];
      t.life -= dt;
      t.y += t.vy * dt;
      t.vy *= 0.94;
    }
    G.texts = G.texts.filter(function (t) { return t.life > 0; });

    if (G.banner) {
      G.banner.life -= dt;
      if (G.banner.life <= 0) G.banner = null;
    }
  }

  function checkWaveEnd() {
    if (G.mode !== 'wave') return;
    if (G.queue.length > 0 || G.enemies.length > 0) return;
    const bonus = 25 + G.wave * 6;
    G.gold += bonus;
    G.score += 50 * G.wave;
    if (G.wave >= MAX_WAVE) { win(); return; }
    G.mode = 'prep';
    G.cool = PREP_TIME;
    banner('WAVE ' + G.wave + ' CLEARED  +' + bonus + 'g', '#5fe0a0', 2.2);
    sfx('waveClear');
  }

  function update(dt) {
    if (G.mode === 'prep' && G.cool > 0) {
      G.cool -= dt;
      if (G.cool <= 0) { G.cool = 0; startWave(); }
    }
    if (G.mode === 'wave') {
      G.qt += dt;
      while (G.queue.length > 0 && G.queue[0].t <= G.qt) {
        spawnEnemy(G.queue.shift().type);
      }
    }
    updateEnemies(dt);
    updateTowers(dt);
    updateShots(dt);
    updateFx(dt);
    checkWaveEnd();
  }

  var best = { score: 0, wave: 0 };
  try { best = JSON.parse(arcadeStorage.getItem('td_best') || 'null') || best; } catch (e) {}
  function recordBest() {
    var improved = false;
    if (G.score > best.score) { best.score = G.score; improved = true; }
    if (G.wave > best.wave) { best.wave = G.wave; improved = true; }
    if (improved) { try { arcadeStorage.setItem('td_best', JSON.stringify(best)); } catch (e) {} }
    return improved;
  }

  function gameOver() {
    if (G.mode === 'over') return;
    G.mode = 'over';
    sfx('leak');
    sting(false);
    if (window.AAA) AAA.flash(0.3);
    showEnd(false);
  }

  function win() {
    G.mode = 'win';
    banner('VICTORY', '#5fe0a0', 3);
    sfx('waveClear');
    sting(true);
    if (window.AAA) { AAA.flash(0.26); AAA.shockwave(innerWidth / 2, innerHeight / 2, '#5fe0a0', 200); }
    showEnd(true);
  }

  /* ============================ drawing ============================ */
  function drawTower(c, x, y, type, level, aim, flash, ghost) {
    const def = TDEF[type];
    c.save();
    c.translate(x, y);

    if (!ghost) {
      c.fillStyle = 'rgba(0,0,0,.32)';
      c.beginPath();
      c.ellipse(0, 4, 15, 9, 0, 0, 7);
      c.fill();
    }

    c.globalAlpha = ghost ? 0.68 : 1;

    c.beginPath(); c.arc(0, 0, 15, 0, 7);
    c.fillStyle = '#16212d'; c.fill();
    c.lineWidth = 2;
    c.strokeStyle = def.color;
    c.stroke();

    c.beginPath(); c.arc(0, 0, 10.5, 0, 7);
    c.fillStyle = '#1f2e3e'; c.fill();

    c.save();
    c.rotate(aim);

    if (type === 'arrow') {
      c.fillStyle = '#cfeaff';
      c.beginPath();
      c.moveTo(14, 0); c.lineTo(-1, 6.5); c.lineTo(1.5, 0); c.lineTo(-1, -6.5);
      c.closePath(); c.fill();
      c.fillStyle = '#3d7fa8';
      c.beginPath(); c.arc(0, 0, 5.5, 0, 7); c.fill();
    } else if (type === 'cannon') {
      c.fillStyle = '#7a5c33';
      roundRect(c, -2, -6, 17, 12, 4); c.fill();
      c.fillStyle = '#ffcf6b';
      roundRect(c, 0, -3.5, 13, 7, 3); c.fill();
      c.fillStyle = '#2a231a';
      c.beginPath(); c.arc(0, 0, 6, 0, 7); c.fill();
    } else {
      c.fillStyle = '#bff3ff';
      c.beginPath();
      c.moveTo(13, 0); c.lineTo(0, 6); c.lineTo(-4, 0); c.lineTo(0, -6);
      c.closePath(); c.fill();
      c.fillStyle = '#2b6f8c';
      c.beginPath(); c.arc(0, 0, 5.5, 0, 7); c.fill();
    }

    if (flash > 0.02 && !ghost) {
      c.globalAlpha = Math.min(1, flash);
      const fg = c.createRadialGradient(18, 0, 0, 18, 0, 13);
      fg.addColorStop(0, 'rgba(255,255,255,.85)');
      fg.addColorStop(1, 'rgba(255,200,120,0)');
      c.fillStyle = fg;
      c.beginPath(); c.arc(18, 0, 13, 0, 7); c.fill();
      c.globalAlpha = ghost ? 0.68 : 1;
    }

    c.restore();

    for (let i = 0; i < level; i++) {
      const ang = -Math.PI / 2 + (i - (level - 1) / 2) * 0.55;
      c.beginPath();
      c.arc(Math.cos(ang) * 17.4, Math.sin(ang) * 17.4, 1.8, 0, 7);
      c.fillStyle = def.color;
      c.fill();
    }

    c.restore();
  }

  function drawEnemy(c, e, t) {
    const r = e.r;

    c.fillStyle = 'rgba(0,0,0,.3)';
    c.beginPath();
    c.ellipse(e.x, e.y + r * 0.5, r * 0.95, r * 0.55, 0, 0, 7);
    c.fill();

    if (e.type === 'boss') {
      c.save();
      c.translate(e.x, e.y);
      c.rotate(t * 0.9);
      c.fillStyle = 'rgba(255,212,121,.85)';
      for (let i = 0; i < 6; i++) {
        c.rotate(Math.PI / 3);
        c.beginPath();
        c.moveTo(r + 3, -3.5);
        c.lineTo(r + 11, 0);
        c.lineTo(r + 3, 3.5);
        c.closePath();
        c.fill();
      }
      c.restore();
    }

    const g = c.createRadialGradient(e.x - r * 0.35, e.y - r * 0.4, r * 0.15, e.x, e.y, r);
    g.addColorStop(0, e.flash > 0 ? '#ffffff' : e.light);
    g.addColorStop(1, e.flash > 0 ? '#ffe0e0' : e.color);
    c.beginPath();
    c.arc(e.x, e.y, r, 0, 7);
    c.fillStyle = g;
    c.fill();
    c.lineWidth = 2.5;
    c.strokeStyle = e.ring;
    c.stroke();

    if (e.type === 'tank') {
      c.beginPath();
      c.arc(e.x, e.y, r - 4.5, 0, 7);
      c.strokeStyle = 'rgba(255,225,180,.55)';
      c.lineWidth = 2;
      c.stroke();
    }

    const ex = Math.cos(e.a) * r * 0.3, ey = Math.sin(e.a) * r * 0.3;
    const px = -Math.sin(e.a) * r * 0.33, py = Math.cos(e.a) * r * 0.33;
    c.fillStyle = '#0b1218';
    c.beginPath(); c.arc(e.x + ex + px, e.y + ey + py, r * 0.17, 0, 7); c.fill();
    c.beginPath(); c.arc(e.x + ex - px, e.y + ey - py, r * 0.17, 0, 7); c.fill();

    if (e.slowT > 0) {
      c.beginPath();
      c.arc(e.x, e.y, r + 3, 0, 7);
      c.strokeStyle = 'rgba(155,232,255,.85)';
      c.lineWidth = 2;
      c.stroke();
    }

    if (e.hp < e.maxhp || e.type === 'boss') {
      const w = Math.max(24, r * 2.3), h = 4;
      const bx = e.x - w / 2, by = e.y - r - 10;
      c.fillStyle = 'rgba(0,0,0,.6)';
      roundRect(c, bx - 1.5, by - 1.5, w + 3, h + 3, 3.5);
      c.fill();
      const f = clamp(e.hp / e.maxhp, 0, 1);
      c.fillStyle = f > 0.5 ? '#6ee08a' : f > 0.25 ? '#ffd166' : '#ff6b8a';
      roundRect(c, bx, by, w * f, h, 2);
      c.fill();
    }
  }

  function drawShots(c) {
    for (let i = 0; i < G.shots.length; i++) {
      const p = G.shots[i];
      c.save();
      c.translate(p.x, p.y);
      c.rotate(p.ang);

      if (p.kind === 'arrow') {
        c.strokeStyle = 'rgba(223,244,255,.9)';
        c.lineWidth = 2;
        c.lineCap = 'round';
        c.beginPath(); c.moveTo(-9, 0); c.lineTo(6, 0); c.stroke();
        c.fillStyle = '#9fd8ff';
        c.beginPath(); c.moveTo(9, 0); c.lineTo(3, -3); c.lineTo(3, 3); c.closePath(); c.fill();
      } else if (p.kind === 'shell') {
        c.strokeStyle = 'rgba(255,180,80,.45)';
        c.lineWidth = 3;
        c.beginPath(); c.moveTo(-11, 0); c.lineTo(0, 0); c.stroke();
        c.fillStyle = '#ffd479';
        c.beginPath(); c.arc(0, 0, 4.5, 0, 7); c.fill();
        c.fillStyle = '#fff3c9';
        c.beginPath(); c.arc(-1, -1, 1.6, 0, 7); c.fill();
      } else {
        c.fillStyle = '#c9f4ff';
        c.beginPath();
        c.moveTo(8, 0); c.lineTo(0, 4); c.lineTo(-7, 0); c.lineTo(0, -4);
        c.closePath(); c.fill();
      }
      c.restore();
    }
  }

  function drawParts(c) {
    for (let i = 0; i < G.parts.length; i++) {
      const p = G.parts[i];
      const a = clamp(p.life / p.max, 0, 1);
      if (p.kind === 'ring') {
        const rad = p.r * (1 - p.life / p.max) + 4;
        c.globalAlpha = a * 0.8;
        c.strokeStyle = p.color;
        c.lineWidth = 3 * a + 0.6;
        c.beginPath(); c.arc(p.x, p.y, rad, 0, 7); c.stroke();
      } else {
        c.globalAlpha = a;
        c.fillStyle = p.color;
        c.beginPath(); c.arc(p.x, p.y, p.r * (0.4 + a * 0.6), 0, 7); c.fill();
      }
    }
    c.globalAlpha = 1;
  }

  function drawTexts(c) {
    c.save();
    c.textAlign = 'center';
    c.font = 'bold 13px "Segoe UI", system-ui, sans-serif';
    for (let i = 0; i < G.texts.length; i++) {
      const t = G.texts[i];
      c.globalAlpha = clamp(t.life / t.max * 1.4, 0, 1);
      c.fillStyle = 'rgba(0,0,0,.5)';
      c.fillText(t.text, t.x + 1, t.y + 1);
      c.fillStyle = t.color;
      c.fillText(t.text, t.x, t.y);
    }
    c.restore();
    c.globalAlpha = 1;
  }

  function drawRanges() {
    if (!selected) return;
    const s = stats(selected);
    ctx.save();
    ctx.beginPath();
    ctx.arc(selected.x, selected.y, s.range, 0, 7);
    ctx.fillStyle = 'rgba(111,198,255,.08)';
    ctx.fill();
    ctx.setLineDash([6, 8]);
    ctx.lineWidth = 1.5;
    ctx.strokeStyle = 'rgba(111,198,255,.55)';
    ctx.stroke();
    ctx.restore();
  }

  function drawPreview() {
    if (!buildType || !hoverCell) return;
    const cx = hoverCell.cx, cy = hoverCell.cy;
    if (cx < 0 || cy < 0 || cx >= COLS || cy >= ROWS) return;

    const isPath = blocked[cy * COLS + cx];
    const occupied = !!towerAt(cx, cy);
    const def = TDEF[buildType];
    const ok = !isPath && !occupied && G.gold >= def.cost;
    const x = cx * T + T / 2, y = cy * T + T / 2;

    ctx.save();
    roundRect(ctx, cx * T + 3, cy * T + 3, T - 6, T - 6, 7);
    ctx.fillStyle = ok ? 'rgba(95,224,160,.13)' : 'rgba(255,90,110,.15)';
    ctx.fill();
    ctx.strokeStyle = ok ? 'rgba(95,224,160,.75)' : 'rgba(255,90,110,.85)';
    ctx.lineWidth = 1.5;
    ctx.stroke();

    ctx.beginPath();
    ctx.arc(x, y, def.range, 0, 7);
    ctx.fillStyle = ok ? 'rgba(95,224,160,.07)' : 'rgba(255,90,110,.06)';
    ctx.fill();
    ctx.setLineDash([5, 7]);
    ctx.strokeStyle = ok ? 'rgba(95,224,160,.5)' : 'rgba(255,90,110,.5)';
    ctx.stroke();
    ctx.setLineDash([]);

    if (!isPath && !occupied) {
      drawTower(ctx, x, y, buildType, 1, -Math.PI / 2, 0, true);
    } else {
      ctx.strokeStyle = 'rgba(255,90,110,.9)';
      ctx.lineWidth = 3;
      ctx.lineCap = 'round';
      const s = 9;
      ctx.beginPath();
      ctx.moveTo(x - s, y - s); ctx.lineTo(x + s, y + s);
      ctx.moveTo(x + s, y - s); ctx.lineTo(x - s, y + s);
      ctx.stroke();
    }
    ctx.restore();
  }

  function drawBanner() {
    const b = G.banner;
    if (!b) return;
    const a = clamp(b.life / 0.45, 0, 1) * clamp((b.max - b.life) / 0.18, 0, 1);
    ctx.save();
    ctx.globalAlpha = a;
    ctx.textAlign = 'center';
    ctx.font = 'bold 30px "Segoe UI", system-ui, sans-serif';
    ctx.fillStyle = 'rgba(0,0,0,.55)';
    ctx.fillText(b.text, W / 2 + 2, 152);
    ctx.fillStyle = b.color;
    ctx.shadowColor = b.color;
    ctx.shadowBlur = 18;
    ctx.fillText(b.text, W / 2, 150);
    ctx.restore();
    ctx.globalAlpha = 1;
  }

  function render(t) {
    ctx.fillStyle = '#05080c';
    ctx.fillRect(0, 0, W, H);

    ctx.save();
    if (shake > 0.2) ctx.translate(rnd(-shake, shake), rnd(-shake, shake));

    ctx.drawImage(bgCanvas, 0, 0);
    if (buildType) ctx.drawImage(hintCanvas, 0, 0);

    drawRanges();
    drawPreview();

    for (let i = 0; i < G.towers.length; i++) {
      const tw = G.towers[i];
      drawTower(ctx, tw.x, tw.y, tw.type, tw.level, tw.aim, tw.flash, false);
    }
    for (let i = 0; i < G.enemies.length; i++) drawEnemy(ctx, G.enemies[i], t);
    drawShots(ctx);
    drawParts(ctx);
    drawTexts(ctx);

    if (selected) {
      ctx.save();
      ctx.strokeStyle = 'rgba(111,198,255,.9)';
      ctx.lineWidth = 2;
      ctx.beginPath();
      ctx.arc(selected.x, selected.y, 19, 0, 7);
      ctx.stroke();
      ctx.restore();
    }

    drawBanner();
    ctx.restore();
  }

  /* ============================ loop ============================ */
  function loop(now) {
    const raw = Math.min(0.05, (now - lastT) / 1000 || 0);
    lastT = now;
    animT += raw;

    if (started && !G.paused && (G.mode === 'prep' || G.mode === 'wave')) {
      update(raw * G.speed);
    }

    shake = Math.max(0, shake - raw * 42);
    render(animT);
    syncUI();
    requestAnimationFrame(loop);
  }

  /* ============================ UI ============================ */
  const el = {
    gold: $('gold'), lives: $('lives'), wave: $('wave'), score: $('score'),
    status: $('status'), overlay: $('overlay'), tinfo: $('tinfo'),
    tiName: $('tiName'), tiLv: $('tiLv'), tiDmg: $('tiDmg'),
    tiRange: $('tiRange'), tiRate: $('tiRate'), tiUp: $('tiUp'), tiSell: $('tiSell'),
    waveBtn: $('waveBtn'), pauseBtn: $('pauseBtn'), speedBtn: $('speedBtn'),
    soundBtn: $('soundBtn'), restartBtn: $('restartBtn')
  };

  function setText(node, v) {
    if (node._txt !== v) {
      node._txt = v;
      node.textContent = v;
    }
  }

  const cardEls = Array.prototype.slice.call(document.querySelectorAll('.card'));

  function syncUI() {
    setText(el.gold, String(Math.floor(G.gold)));
    setText(el.lives, String(G.lives));
    setText(el.wave, String(G.wave));
    setText(el.score, String(G.score));

    for (let i = 0; i < cardEls.length; i++) {
      const card = cardEls[i];
      const type = card.dataset.type;
      card.classList.toggle('active', buildType === type);
      card.classList.toggle('poor', G.gold < TDEF[type].cost);
    }

    if (G.mode === 'prep') {
      el.waveBtn.disabled = false;
      if (G.cool > 0) {
        setText(el.waveBtn, 'Hujumni boshlash ' + (G.wave + 1) + ' \u00b7 ' + Math.ceil(G.cool) + 's \u00b7 +' + (Math.floor(G.cool) * 2) + 'g');
      } else {
        setText(el.waveBtn, 'Hujumni boshlash ' + (G.wave + 1));
      }
    } else if (G.mode === 'wave') {
      el.waveBtn.disabled = true;
      setText(el.waveBtn, 'Wave ' + G.wave + ' \u00b7 ' + (G.enemies.length + G.queue.length) + ' left');
    } else {
      el.waveBtn.disabled = true;
      setText(el.waveBtn, 'Wave ' + G.wave);
    }

    setText(el.pauseBtn, G.paused ? 'Davom etish' : 'Pauza');
    el.pauseBtn.classList.toggle('on', G.paused);
    setText(el.speedBtn, G.speed + '\u00d7');

    let s;
    if (G.mode === 'over') s = 'O‘yin tugadi. Takrorlash uchun “Qaytadan” tugmasini bosing.';
    else if (G.mode === 'win') s = 'G‘alaba! Barcha hujumlar qaytarildi.';
    else if (G.paused) s = 'O‘yin vaqtincha to‘xtatildi.';
    else if (buildType) s = 'Minorani qurish uchun yo‘l yonidagi katakni tanlang.';
    else if (selected) s = 'Minora darajasi: ' + selected.level + '. Uni yaxshilash yoki sotish mumkin.';
    else if (G.mode === 'wave') s = 'Hujum ' + G.wave + ' · qolgan raqiblar: ' + (G.enemies.length + G.queue.length);
    else if (G.cool > 0) s = 'Keyingi hujumga ' + Math.ceil(G.cool) + ' soniya qoldi. Erta boshlasangiz qo‘shimcha oltin olasiz.';
    else s = 'Minora tanlang va yo‘l yonidagi katakni belgilang.';
    setText(el.status, s);

    syncTowerPanel();
  }

  function syncTowerPanel() {
    if (!selected) {
      if (!el.tinfo.hidden) el.tinfo.hidden = true;
      return;
    }
    if (el.tinfo.hidden) el.tinfo.hidden = false;
    const s = stats(selected);
    const def = TDEF[selected.type];
    setText(el.tiName, def.name);
    setText(el.tiLv, 'Lv ' + selected.level);
    setText(el.tiDmg, s.dmg.toFixed(1));
    setText(el.tiRange, String(Math.round(s.range)));
    setText(el.tiRate, (1 / s.cd).toFixed(1) + '/s');

    const cost = upCost(selected);
    if (cost === null) {
      el.tiUp.disabled = true;
      setText(el.tiUp, 'Max level');
    } else {
      el.tiUp.disabled = G.gold < cost;
      setText(el.tiUp, 'Upgrade \u00b7 ' + cost + 'g');
    }
    setText(el.tiSell, 'Sell \u00b7 ' + sellValue(selected) + 'g');
  }

  /* ============================ interaction ============================ */
  function toGame(ev) {
    const r = cv.getBoundingClientRect();
    return {
      x: (ev.clientX - r.left) * (W / r.width),
      y: (ev.clientY - r.top) * (H / r.height)
    };
  }

  function cellOf(p) {
    return { cx: Math.floor(p.x / T), cy: Math.floor(p.y / T) };
  }

  function selectBuild(type) {
    if (buildType === type) {
      buildType = null;
    } else {
      buildType = type;
      selected = null;
    }
    sfx('place');
  }

  function tryPlace(cx, cy) {
    const def = TDEF[buildType];
    if (!def) return;
    if (cx < 0 || cy < 0 || cx >= COLS || cy >= ROWS) return;
    if (blocked[cy * COLS + cx] || towerAt(cx, cy)) {
      sfx('deny');
      shake = Math.max(shake, 3);
      return;
    }
    if (G.gold < def.cost) {
      sfx('deny');
      addText(cx * T + T / 2, cy * T + T / 2 - 12, 'Need ' + def.cost + 'g', '#ff6b8a');
      return;
    }
    G.gold -= def.cost;
    G.towers.push({
      cx: cx, cy: cy, x: cx * T + T / 2, y: cy * T + T / 2,
      type: buildType, level: 1, invested: def.cost,
      cool: 0, aim: -Math.PI / 2, flash: 0
    });
    burst(cx * T + T / 2, cy * T + T / 2, def.color, 9);
    sfx('place');
  }

  function handleClick(p) {
    if (el.overlay.hidden === false) return;
    if (G.mode === 'over' || G.mode === 'win') return;

    const cell = cellOf(p);
    const existing = towerAt(cell.cx, cell.cy);

    if (existing) {
      selected = existing;
      buildType = null;
      sfx('place');
      return;
    }
    if (buildType) {
      tryPlace(cell.cx, cell.cy);
      return;
    }
    selected = null;
  }

  function upgradeSelected() {
    if (!selected) return;
    const cost = upCost(selected);
    if (cost === null || G.gold < cost) { sfx('deny'); return; }
    G.gold -= cost;
    selected.invested += cost;
    selected.level++;
    burst(selected.x, selected.y, '#9fe8ff', 14);
    ring(selected.x, selected.y, 26, '#9fe8ff');
    addText(selected.x, selected.y - 22, 'Lv ' + selected.level, '#9fe8ff');
    sfx('upgrade');
  }

  function sellSelected() {
    if (!selected) return;
    const val = sellValue(selected);
    G.gold += val;
    addText(selected.x, selected.y - 18, '+' + val, '#ffd479');
    burst(selected.x, selected.y, '#8fa6bd', 10);
    G.towers = G.towers.filter(function (t) { return t !== selected; });
    selected = null;
    sfx('kill');
  }

  function setPaused(v) {
    G.paused = v;
  }

  function cycleSpeed() {
    G.speed = G.speed === 1 ? 2 : G.speed === 2 ? 3 : 1;
  }

  function toggleSound() {
    muted = !muted;
    setText(el.soundBtn, muted ? 'Ovoz o‘chiq' : 'Ovoz yoqilgan');
    el.soundBtn.classList.toggle('on', !muted);
    if (!muted) { audioCtx(); sfx('place'); }
  }

  /* ============================ overlays ============================ */
  function showStart() {
    el.overlay.hidden = false;
    el.overlay.innerHTML =
      '<div class="panel">' +
        '<div class="kicker">Tower Defense</div>' +
        '<h1>Tower Defense <span class="accent">Lite</span></h1>' +
        '<p>Yo‘l bo‘ylab minoralar quring va yigirma hujumni to‘xtating.</p>' +
        '<div class="howto">' +
          '<div><b>1</b><span>Quyidagi ro‘yxatdan minora tanlang</span></div>' +
          '<div><b>2</b><span>Yo‘l yonidagi maysazor katagini bosing</span></div>' +
          '<div><b>3</b><span>Yaxshilash yoki sotish uchun minorani bosing</span></div>' +
        '</div>' +
        '<button class="big" id="startBtn">Boshlash</button>' +
        (best.score ? '<div class="tiny" style="color:#ffd479">Rekord: ' + best.score + ' pts &middot; wave ' + best.wave + '</div>' : '') +
        '<div class="tiny">Tip: call the next wave early for bonus gold.</div>' +
      '</div>';
    $('startBtn').addEventListener('click', beginGame);
  }

  function beginGame() {
    el.overlay.hidden = true;
    started = true;
    lastT = performance.now();
    audioCtx();
  }

  function showEnd(isWin) {
    var improved = recordBest();
    el.overlay.hidden = false;
    el.overlay.innerHTML =
      '<div class="panel">' +
        '<div class="kicker">' + (isWin ? 'All 20 waves cleared' : 'The keep has fallen') + '</div>' +
        '<h1>' + (isWin ? 'Victory' : 'Game Over') + '</h1>' +
        '<p>' + (isWin
          ? 'Your towers held the line. The realm is safe, for now.'
          : 'The creeps broke through at wave ' + G.wave + '.') + '</p>' +
        '<div class="endstats">' +
          '<div><b>' + G.score + '</b><span>Hisob</span></div>' +
          '<div><b>' + G.wave + '</b><span>Wave</span></div>' +
          '<div><b>' + G.lives + '</b><span>Lives left</span></div>' +
        '</div>' +
        '<div class="tiny" style="margin-bottom:14px;color:' + (improved ? '#ffd479' : 'inherit') + '">' +
          (improved ? 'New best! ' : 'Rekord: ') + best.score + ' pts &middot; wave ' + best.wave + '</div>' +
        '<button class="big" id="againBtn">Yana o‘ynash</button>' +
      '</div>';
    $('againBtn').addEventListener('click', resetGame);
  }

  function resetGame() {
    G = newState();
    selected = null;
    buildType = null;
    hoverCell = null;
    shake = 0;
    el.overlay.hidden = true;
    started = true;
    lastT = performance.now();
    setText(el.pauseBtn, 'Pause');
    el.pauseBtn.classList.remove('on');
  }

  /* ============================ wiring ============================ */
  function drawIcon(canvas, type) {
    const c = canvas.getContext('2d');
    c.setTransform(1, 0, 0, 1, 0, 0);
    c.clearRect(0, 0, canvas.width, canvas.height);
    c.save();
    c.translate(canvas.width / 2, canvas.height / 2);
    c.scale(1.65, 1.65);
    drawTower(c, 0, 0, type, 1, -Math.PI / 2, 0, false);
    c.restore();
  }

  function bindEvents() {
    for (let i = 0; i < cardEls.length; i++) {
      (function (card) {
        const type = card.dataset.type;
        drawIcon(card.querySelector('canvas'), type);
        card.addEventListener('click', function () { selectBuild(type); });
      })(cardEls[i]);
    }

    el.tiUp.addEventListener('click', upgradeSelected);
    el.tiSell.addEventListener('click', sellSelected);
    el.waveBtn.addEventListener('click', function () { startWave(); });
    el.pauseBtn.addEventListener('click', function () { setPaused(!G.paused); });
    el.speedBtn.addEventListener('click', cycleSpeed);
    el.soundBtn.addEventListener('click', toggleSound);
    el.restartBtn.addEventListener('click', resetGame);

    cv.addEventListener('pointermove', function (ev) {
      const p = toGame(ev);
      hoverCell = cellOf(p);
    });
    cv.addEventListener('pointerleave', function () { hoverCell = null; });
    cv.addEventListener('pointerdown', function (ev) {
      if (ev.button === 2) return;
      ev.preventDefault();
      handleClick(toGame(ev));
    });
    cv.addEventListener('contextmenu', function (ev) {
      ev.preventDefault();
      buildType = null;
      selected = null;
    });

    window.addEventListener('keydown', function (ev) {
      const k = ev.key;
      if (k === '1') selectBuild('arrow');
      else if (k === '2') selectBuild('cannon');
      else if (k === '3') selectBuild('frost');
      else if (k.startsWith('Arrow')) {
        ev.preventDefault();
        const { cx, cy } = hoverCell || { cx: 0, cy: 0 };
        const dx = (k === 'ArrowRight') - (k === 'ArrowLeft');
        const dy = (k === 'ArrowDown') - (k === 'ArrowUp');
        hoverCell = { cx: Math.max(0, Math.min(COLS - 1, cx + dx)), cy: Math.max(0, Math.min(ROWS - 1, cy + dy)) };
      }
      else if (k === 'Enter' && hoverCell && !ev.target.closest('button')) {
        handleClick({ x: (hoverCell.cx + 0.5) * T, y: (hoverCell.cy + 0.5) * T });
      }
      else if (k === 'Escape') { buildType = null; selected = null; }
      else if (k === ' ') { ev.preventDefault(); setPaused(!G.paused); }
      else if (k === 'n' || k === 'N') startWave();
      else if (k === 'u' || k === 'U') upgradeSelected();
    });

    window.addEventListener('resize', function () {
      const dpr = Math.min(window.devicePixelRatio || 1, 2);
      if (cv.width !== Math.round(W * dpr)) fitCanvas();
    });
  }

  /* ============================ boot ============================ */
  function boot() {
    fitCanvas();
    buildBackground();
    buildHint();
    bindEvents();
    G = newState();
    showStart();
    requestAnimationFrame(function (t) {
      lastT = t;
      requestAnimationFrame(loop);
    });
  }

  boot();


  const siegeButton = document.createElement('button');
  siegeButton.type = 'button'; siegeButton.className = 'act'; siegeButton.textContent = 'Siege off'; siegeButton.setAttribute('aria-pressed', 'false');
  siegeButton.title = 'Add two reinforcements every fourth wave';
  siegeButton.onclick = () => { siegeWaves = !siegeWaves; siegeButton.textContent = siegeWaves ? 'Siege on' : 'Siege off'; siegeButton.setAttribute('aria-pressed', String(siegeWaves)); };
  document.getElementById('speedBtn').after(siegeButton);

  // Report native gameplay state to the optional mission engine.
  window.__arcadeMissionSource = Object.assign({"id":71,"label":"Waves cleared","goals":[2,4,7],"note":"Complete waves, not just deploy them. Upgrade where the path loops.","seconds":550}, { read: () => { return (G ? G.wave - (G.mode === 'wave' || G.mode === 'over' ? 1 : 0) : 0); } });
})();
