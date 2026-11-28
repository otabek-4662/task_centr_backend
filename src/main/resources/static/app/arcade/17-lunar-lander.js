// Arcade 100 · MIT · see LICENSE
(function () {
  "use strict";

  var canvas = document.getElementById("game");
  var ctx = canvas.getContext("2d");

  var W = 0, H = 0, DPR = 1;
  var IS_TOUCH = ('ontouchstart' in window) || (navigator.maxTouchPoints > 0);
  if (IS_TOUCH) document.body.classList.add('touchy');

  function resize() {
    DPR = Math.min(window.devicePixelRatio || 1, 2);
    W = window.innerWidth;
    H = window.innerHeight;
    canvas.width = Math.round(W * DPR);
    canvas.height = Math.round(H * DPR);
    canvas.style.width = W + 'px';
    canvas.style.height = H + 'px';
    ctx.setTransform(DPR, 0, 0, DPR, 0, 0);
    buildTerrain();
    buildNebula();
  }
  window.addEventListener("resize", resize);

  // ---------- Audio ----------
  var ACX = null;
  function ac() {
    try {
      if (!ACX) ACX = new (window.AudioContext || window.webkitAudioContext)();
      if (ACX.state === 'suspended') ACX.resume();
    } catch (e) {}
    return ACX;
  }
  function tone(f, d, type, v, slide, delay) {
    var c = ac(); if (!c) return;
    d = d || 0.1; type = type || 'sine'; v = v || 0.05;
    var t = c.currentTime + (delay || 0);
    try {
      var o = c.createOscillator(), g = c.createGain();
      o.type = type;
      o.frequency.setValueAtTime(f, t);
      if (slide) o.frequency.exponentialRampToValueAtTime(Math.max(24, slide), t + d);
      g.gain.setValueAtTime(0.0001, t);
      g.gain.linearRampToValueAtTime(v, t + 0.015);
      g.gain.exponentialRampToValueAtTime(0.0001, t + d);
      o.connect(g); g.connect(c.destination);
      o.start(t); o.stop(t + d + 0.04);
    } catch (e) {}
  }
  var noiseBuf = null;
  function noiseSfx(d, v, fc) {
    var c = ac(); if (!c) return;
    try {
      if (!noiseBuf) {
        var n = Math.floor(c.sampleRate * 1.2);
        noiseBuf = c.createBuffer(1, n, c.sampleRate);
        var ch = noiseBuf.getChannelData(0);
        for (var i = 0; i < n; i++) ch[i] = Math.random() * 2 - 1;
      }
      var s = c.createBufferSource(); s.buffer = noiseBuf;
      var f = c.createBiquadFilter(); f.type = 'lowpass'; f.frequency.value = fc || 1200;
      var g = c.createGain(); g.gain.value = v || 0.1;
      s.connect(f); f.connect(g); g.connect(c.destination); s.start();
      s.stop(c.currentTime + (d || 0.3));
    } catch (e) {}
  }
  // continuous thrust rumble (looping noise node)
  var thrustNodes = null;
  function thrustOn() {
    var c = ac(); if (!c || thrustNodes) return;
    try {
      if (!noiseBuf) { noiseSfx(0.01, 0.0001, 400); }
      var s = c.createBufferSource(); s.buffer = noiseBuf; s.loop = true;
      var f = c.createBiquadFilter(); f.type = 'bandpass'; f.frequency.value = 220; f.Q.value = 0.7;
      var g = c.createGain(); g.gain.value = 0.0001;
      g.gain.setTargetAtTime(0.09, c.currentTime, 0.05);
      var o = c.createOscillator(), g2 = c.createGain();
      o.type = 'sawtooth'; o.frequency.value = 55; g2.gain.value = 0.022;
      s.connect(f); f.connect(g); g.connect(c.destination);
      o.connect(g2); g2.connect(c.destination);
      s.start(); o.start();
      thrustNodes = { s: s, o: o, g: g, g2: g2 };
    } catch (e) {}
  }
  function thrustOff() {
    if (!thrustNodes || !ACX) { thrustNodes = null; return; }
    try {
      var t = ACX.currentTime;
      thrustNodes.g.gain.setTargetAtTime(0.0001, t, 0.05);
      thrustNodes.g2.gain.setTargetAtTime(0.0001, t, 0.05);
      var s = thrustNodes.s, o = thrustNodes.o;
      setTimeout(function () { try { s.stop(); o.stop(); } catch (e) {} }, 260);
    } catch (e) {}
    thrustNodes = null;
  }
  var sfx = {
    crash: function () { noiseSfx(0.6, 0.28, 700); tone(140, 0.5, 'sawtooth', 0.1, 40); tone(90, 0.6, 'square', 0.06, 30, 0.1); },
    land:  function () { [523, 659, 784, 1047].forEach(function (f, i) { tone(f, 0.16, 'triangle', 0.07, null, i * 0.08); }); },
    warn:  function () { tone(760, 0.09, 'square', 0.045, 560); },
    deny:  function () { tone(180, 0.14, 'square', 0.05, 120); },
    blip:  function () { tone(920, 0.05, 'sine', 0.045, 1240); }
  };
  // Ambient pad BGM — quiet drone, ducks while overlay music is enabled
  var bgm = {
    t: null, step: 0,
    chords: [[65.4, 98, 130.8], [61.7, 92.5, 123.5], [55, 82.4, 110], [58.3, 87.3, 116.5]],
    start: function () {
      this.stop(); this.step = 0; var self = this;
      this.t = setInterval(function () { self.tick(); }, 900);
    },
    tick: function () {
      var c = ac(); if (!c || c.state !== 'running') return;
      try { if (arcadeStorage.getItem('aaa_bgm') === '1') return; } catch (e) {}
      var ch = this.chords[this.step++ % 4], now = c.currentTime;
      var self = this;
      ch.forEach(function (f) {
        var o = c.createOscillator(), g = c.createGain();
        o.type = 'sine'; o.frequency.value = f;
        g.gain.setValueAtTime(0.0001, now);
        g.gain.linearRampToValueAtTime(0.028, now + 0.5);
        g.gain.exponentialRampToValueAtTime(0.0004, now + 2.6);
        o.connect(g); g.connect(c.destination);
        o.start(now); o.stop(now + 2.7);
      });
      if (Math.random() < 0.4) tone(660 + Math.random() * 660, 0.4, 'sine', 0.012, null, 0.3);
    },
    stop: function () { if (this.t) clearInterval(this.t); this.t = null; }
  };

  // ---------- Constants ----------
  var GRAVITY = 30;
  var THRUST = 92;
  var ROT_SPEED = 2.6;
  var FUEL_MAX = 100;
  var FUEL_BURN = 20;
  var SAFE_VX = 22;
  var SAFE_VY = 30;
  var SAFE_ANGLE = 0.35;
  var LANDER_R = 11;

  // ---------- Input ----------
  var keys = {};
  var touch = { left: false, right: false, up: false };

  window.addEventListener("keydown", function (e) {
    keys[e.code] = true;
    if (["ArrowUp", "ArrowDown", "ArrowLeft", "ArrowRight", "Space"].indexOf(e.code) >= 0) e.preventDefault();
    if (e.code === "KeyR" || e.code === "Space" || e.code === "Enter") {
      if (state === "title") startGame();
      else if (state !== "flying") nextRound();
    }
    if (e.code === "Space" && state === "flying" && lander.landed) {
      lander.landed = false; lander.vy = -5;
    }
  });
  window.addEventListener("keyup", function (e) { keys[e.code] = false; });

  canvas.addEventListener("pointerdown", function (e) {
    ac();
    if (state === "title") startGame();
    else if (state !== "flying") nextRound();
  });

  function bindBtn(id, prop) {
    var el = document.getElementById(id);
    var set = function (v) {
      return function (e) {
        e.preventDefault(); e.stopPropagation();
        ac();
        touch[prop] = v;
        el.classList.toggle('on', v);
        if (v && state === 'title') startGame();
        else if (v && state !== 'flying') nextRound();
      };
    };
    el.addEventListener('pointerdown', set(true));
    el.addEventListener('pointerup', set(false));
    el.addEventListener('pointercancel', set(false));
    el.addEventListener('pointerleave', set(false));
    el.addEventListener('contextmenu', function (e) { e.preventDefault(); });
  }
  bindBtn('btnLeft', 'left');
  bindBtn('btnRight', 'right');
  bindBtn('btnThrust', 'up');

  // ---------- Terrain ----------
  var terrain = [];
const arcadePlus = (new URLSearchParams(location.search).get('mode') === 'plus');
  var pad = { x1: 0, x2: 0, y: 0 };
  var padW0 = 0;

  function buildTerrain() {
    terrain = [];
    var seg = 24;
    var groundBase = H * 0.84;
    var shrink = Math.min(landings * 4, 40);
    var padW = Math.max(46, Math.max(80, W * .09) - shrink - (arcadePlus ? 12 : 0));
    padW0 = padW;
    var padCenter = W * 0.15 + Math.random() * W * 0.7;
    pad.x1 = padCenter - padW / 2;
    pad.x2 = padCenter + padW / 2;
    pad.y = groundBase + (Math.random() - 0.5) * H * 0.05;

    var seed = Math.random() * 1000;
    var rough = 1 + Math.min(landings * 0.08, 0.7);
    for (var x = -seg; x <= W + seg; x += seg) {
      var y;
      if (x >= pad.x1 - seg * 0.5 && x <= pad.x2 + seg * 0.5) {
        y = pad.y;
      } else {
        var n = Math.sin(x * 0.011 + seed) * 0.5 +
                Math.sin(x * 0.027 + seed * 2.7) * 0.3 +
                Math.sin(x * 0.053 + seed * 5.1) * 0.2;
        y = groundBase + n * H * 0.13 * rough;
        if (y < H * 0.55) y = H * 0.55;
      }
      terrain.push({ x: x, y: y });
    }
    for (var i = 0; i < terrain.length; i++) {
      var p = terrain[i];
      if (p.x > pad.x1 - seg && p.x < pad.x2 + seg) p.y = pad.y;
    }
  }

  function terrainYAt(x) {
    if (!terrain.length) return H * 0.84;
    if (x <= terrain[0].x) return terrain[0].y;
    for (var i = 0; i < terrain.length - 1; i++) {
      var a = terrain[i], b = terrain[i + 1];
      if (x >= a.x && x <= b.x) {
        var t = (x - a.x) / (b.x - a.x);
        return a.y + (b.y - a.y) * t;
      }
    }
    return terrain[terrain.length - 1].y;
  }

  // ---------- Sky decor ----------
  var stars = [];
  for (var i = 0; i < 160; i++) {
    stars.push({ x: Math.random(), y: Math.random() * 0.72, r: Math.random() * 1.5 + 0.3, tw: Math.random() * 6, big: Math.random() < 0.08 });
  }
  var nebula = [];
  function buildNebula() {
    nebula = [];
    var cols = [
      [80, 40, 160, 0.10], [140, 60, 200, 0.08], [40, 90, 170, 0.09],
      [200, 90, 160, 0.06], [90, 60, 200, 0.07]
    ];
    for (var i = 0; i < cols.length; i++) {
      nebula.push({
        x: Math.random() * W, y: Math.random() * H * 0.5,
        r: Math.min(W, H) * (0.22 + Math.random() * 0.2),
        c: cols[i], drift: 4 + Math.random() * 8, ph: Math.random() * 6
      });
    }
  }

  // ---------- Lander & state ----------
  var lander = {};
  var state = "title";   // title | flying | landed | crashed
  var stateTimer = 0;
  var particles = [];
  var dust = [];
  var score = 0, streak = 0, landings = 0, best = 0, newBest = false;
  var lastAward = 0, shake = 0, warnT = 0, lowFuelWarned = false;

  try { best = parseInt(arcadeStorage.getItem(('lunar-best' + (new URLSearchParams(location.search).get('mode')==='plus'?'_plus':''))) || '0', 10) || 0; } catch (e) {}

  function saveBest() {
    if (score > best) {
      best = score; newBest = true;
      try { arcadeStorage.setItem(('lunar-best' + (new URLSearchParams(location.search).get('mode')==='plus'?'_plus':'')), String(best)); } catch (e) {}
    }
  }

  function gust() { return Math.sin(warnT*.85 + landings) * 24 + Math.sin(warnT*2.1 + 1) * 12; }

  function spawnLander() {
    lander = {
      x: W * 0.5 + (Math.random() - 0.5) * W * 0.3,
      y: H * 0.12,
      vx: (Math.random() - 0.5) * 30,
      vy: 0,
      angle: (Math.random() - 0.5) * 0.35,
      fuel: arcadePlus ? FUEL_MAX * .78 : FUEL_MAX,
      landed: false,
      thrusting: false
    };
    lowFuelWarned = false;
  }

  function startGame() {
    score = 0; streak = 0; landings = 0; lastAward = 0; newBest = false;
    buildTerrain(); spawnLander();
    state = "flying"; stateTimer = 0;
    sfx.blip(); bgm.start();
  }
  function nextRound() {
    buildTerrain(); spawnLander();
    state = "flying"; stateTimer = 0;
    sfx.blip();
  }
  function reset() { if (state === "title") startGame(); else nextRound(); }

  function explode() {
    thrustOff();
    shake = 16;
    for (var i = 0; i < 70; i++) {
      var a = Math.random() * Math.PI * 2;
      var s = 30 + Math.random() * 170;
      particles.push({
        x: lander.x, y: lander.y,
        vx: Math.cos(a) * s, vy: Math.sin(a) * s - 40,
        life: 0.6 + Math.random() * 1.0, max: 1.6,
        r: 1.5 + Math.random() * 2.5,
        color: Math.random() < 0.5 ? "#ff8c3a" : (Math.random() < 0.5 ? "#ffd23a" : "#ff4444")
      });
    }
    sfx.crash();
    if (window.AAA) { AAA.flash(0.3); AAA.shockwave(lander.x, lander.y, '#ff8c5a', 130); }
  }

  function touchdown(quality) {
    landings++;
    streak++;
    var fuelBonus = Math.round(lander.fuel * 1.5);
    var softBonus = Math.round((1 - quality) * 60);
    var streakBonus = (streak - 1) * 25;
    lastAward = 100 + fuelBonus + softBonus + streakBonus;
    score += lastAward;
    saveBest();
    sfx.land();
    if (window.AAA) {
      AAA.popup(W / 2, H * 0.3, '+' + lastAward, '#7CFC98');
      if (streak > 1) AAA.popup(W / 2, H * 0.3 + 30, 'STREAK x' + streak, '#f472b6');
      AAA.burst(lander.x, lander.y + LANDER_R, '#7CFC98', 14);
    }
    for (var i = 0; i < 18; i++) {
      dust.push({
        x: lander.x + (Math.random() - 0.5) * 30, y: lander.y + LANDER_R,
        vx: (Math.random() - 0.5) * 90, vy: -10 - Math.random() * 40,
        life: 0.5 + Math.random() * 0.5, max: 1, r: 2 + Math.random() * 3
      });
    }
  }

  // ---------- Update ----------
  function update(dt) {
    shake = Math.max(0, shake - dt * 26);
    warnT += dt;

    var left = keys.ArrowLeft || keys.KeyA || touch.left;
    var right = keys.ArrowRight || keys.KeyD || touch.right;
    var up = keys.ArrowUp || keys.KeyW || touch.up;

    if (state === "flying") {
      if (left) lander.angle -= ROT_SPEED * dt;
      if (right) lander.angle += ROT_SPEED * dt;

      lander.thrusting = false;
      if (up && lander.fuel > 0) {
        lander.thrusting = true;
        thrustOn();
        lander.fuel = Math.max(0, lander.fuel - FUEL_BURN * dt);
        lander.vx -= Math.sin(lander.angle) * THRUST * dt;
        lander.vy -= Math.cos(lander.angle) * THRUST * dt;
        for (var i = 0; i < 3; i++) {
          var spread = (Math.random() - 0.5) * 0.5;
          particles.push({
            x: lander.x + Math.sin(lander.angle) * (LANDER_R + 4),
            y: lander.y + Math.cos(lander.angle) * (LANDER_R + 4),
            vx: Math.sin(lander.angle + spread) * 70 + lander.vx * 0.3,
            vy: Math.cos(lander.angle + spread) * 70 + lander.vy * 0.3,
            life: 0.22 + Math.random() * 0.2, max: 0.45, r: 1.5 + Math.random() * 2,
            color: Math.random() < 0.6 ? "#ffb03a" : "#ff6633"
          });
        }
        // dust kicked up near ground
        var gyd = terrainYAt(lander.x) - lander.y;
        if (gyd < 90 && Math.random() < 0.5) {
          dust.push({
            x: lander.x + (Math.random() - 0.5) * 26, y: gyd + lander.y,
            vx: (Math.random() - 0.5) * 60, vy: -8 - Math.random() * 24,
            life: 0.4 + Math.random() * 0.4, max: 0.8, r: 1.5 + Math.random() * 2.5
          });
        }
      } else {
        thrustOff();
      }

      // low fuel warning chirp
      if (lander.fuel < 22 && lander.fuel > 0 && !lowFuelWarned) {
        lowFuelWarned = true;
      }
      if (lowFuelWarned && lander.fuel > 0 && Math.floor(warnT * 2.5) !== Math.floor((warnT - dt) * 2.5) && lander.fuel < 22) {
        sfx.warn();
      }

      lander.vy += GRAVITY * dt;
      if (arcadePlus) lander.vx += gust() * dt;
      lander.x += lander.vx * dt;
      lander.y += lander.vy * dt;

      if (lander.x < -LANDER_R) lander.x = W + LANDER_R;
      if (lander.x > W + LANDER_R) lander.x = -LANDER_R;

      var footY = lander.y + LANDER_R;
      var gy = terrainYAt(lander.x);
      if (footY >= gy) {
        lander.y = gy - LANDER_R;
        var onPad = lander.x >= pad.x1 && lander.x <= pad.x2 && Math.abs(gy - pad.y) < 1;
        var a = lander.angle % (Math.PI * 2);
        if (a > Math.PI) a -= Math.PI * 2;
        if (a < -Math.PI) a += Math.PI * 2;
        var upright = Math.abs(a) < SAFE_ANGLE;
        var quality = Math.max(Math.abs(lander.vx) / SAFE_VX, lander.vy / SAFE_VY);

        if (onPad && upright && Math.abs(lander.vx) <= SAFE_VX && lander.vy <= SAFE_VY) {
          state = "landed";
          lander.vx = 0; lander.vy = 0; lander.angle = a;
          thrustOff();
          touchdown(quality);
        } else {
          state = "crashed";
          streak = 0;
          saveBest();
          explode();
        }
        stateTimer = 0;
      }
      if (lander.y > H + 60 && state === "flying") { state = "crashed"; streak = 0; saveBest(); explode(); }
    } else {
      thrustOff();
    }

    stateTimer += dt;

    for (var i = particles.length - 1; i >= 0; i--) {
      var p = particles[i];
      p.life -= dt;
      p.x += p.vx * dt; p.y += p.vy * dt;
      p.vy += GRAVITY * 0.4 * dt;
      if (p.life <= 0) particles.splice(i, 1);
    }
    for (i = dust.length - 1; i >= 0; i--) {
      var d = dust[i];
      d.life -= dt;
      d.x += d.vx * dt; d.y += d.vy * dt;
      d.vy += GRAVITY * 0.25 * dt;
      if (d.life <= 0) dust.splice(i, 1);
    }
  }

  // ---------- Draw ----------
  function draw() {
    ctx.save();
    if (shake > 0) ctx.translate((Math.random() - 0.5) * shake, (Math.random() - 0.5) * shake);

    // deep space gradient
    var g = ctx.createLinearGradient(0, 0, 0, H);
    g.addColorStop(0, '#02020a');
    g.addColorStop(0.5, '#0a0a24');
    g.addColorStop(1, '#161031');
    ctx.fillStyle = g;
    ctx.fillRect(-30, -30, W + 60, H + 60);

    var t = performance.now() / 1000;

    // nebula clouds
    for (var i = 0; i < nebula.length; i++) {
      var n = nebula[i];
      var nx = n.x + Math.sin(t * 0.05 + n.ph) * n.drift;
      var rg = ctx.createRadialGradient(nx, n.y, 0, nx, n.y, n.r);
      rg.addColorStop(0, 'rgba(' + n.c[0] + ',' + n.c[1] + ',' + n.c[2] + ',' + n.c[3] + ')');
      rg.addColorStop(1, 'rgba(' + n.c[0] + ',' + n.c[1] + ',' + n.c[2] + ',0)');
      ctx.fillStyle = rg;
      ctx.fillRect(nx - n.r, n.y - n.r, n.r * 2, n.r * 2);
    }

    // stars
    for (i = 0; i < stars.length; i++) {
      var s = stars[i];
      var alpha = 0.3 + 0.7 * Math.abs(Math.sin(t * 0.8 + s.tw));
      ctx.fillStyle = "rgba(255,255,255," + (alpha * (s.big ? 1 : 0.7)).toFixed(2) + ")";
      var sr = s.big ? s.r + 0.8 : s.r;
      ctx.fillRect(s.x * W, s.y * H, sr, sr);
      if (s.big) {
        ctx.fillRect(s.x * W - sr, s.y * H + sr / 2 - 0.5, sr * 3, 1);
        ctx.fillRect(s.x * W + sr / 2 - 0.5, s.y * H - sr, 1, sr * 3);
      }
    }

    // earth with atmosphere glow
    var ex = W * 0.85, ey = H * 0.12, er = 24;
    var eg = ctx.createRadialGradient(ex, ey, er * 0.6, ex, ey, er * 2.2);
    eg.addColorStop(0, 'rgba(90,160,255,0.4)');
    eg.addColorStop(1, 'rgba(90,160,255,0)');
    ctx.fillStyle = eg;
    ctx.fillRect(ex - er * 2.2, ey - er * 2.2, er * 4.4, er * 4.4);
    var eb = ctx.createRadialGradient(ex - er * 0.4, ey - er * 0.4, 2, ex, ey, er);
    eb.addColorStop(0, '#7db8ff');
    eb.addColorStop(0.6, '#2a6fbf');
    eb.addColorStop(1, '#123a75');
    ctx.fillStyle = eb;
    ctx.beginPath(); ctx.arc(ex, ey, er, 0, Math.PI * 2); ctx.fill();
    ctx.fillStyle = 'rgba(80,190,120,0.5)';
    ctx.beginPath(); ctx.arc(ex - 6, ey - 4, 7, 0, Math.PI * 2); ctx.fill();
    ctx.beginPath(); ctx.arc(ex + 8, ey + 7, 5, 0, Math.PI * 2); ctx.fill();

    // terrain body
    if (terrain.length) {
      ctx.beginPath();
      ctx.moveTo(terrain[0].x, H + 30);
      for (i = 0; i < terrain.length; i++) ctx.lineTo(terrain[i].x, terrain[i].y);
      ctx.lineTo(terrain[terrain.length - 1].x, H + 30);
      ctx.closePath();
      var tg = ctx.createLinearGradient(0, H * 0.55, 0, H);
      tg.addColorStop(0, '#4a4a62');
      tg.addColorStop(0.4, '#33334a');
      tg.addColorStop(1, '#1d1d30');
      ctx.fillStyle = tg;
      ctx.fill();

      // glowing surface line
      ctx.strokeStyle = 'rgba(160,175,255,0.9)';
      ctx.lineWidth = 1.6;
      ctx.shadowColor = 'rgba(120,140,255,0.8)';
      ctx.shadowBlur = 6;
      ctx.beginPath();
      ctx.moveTo(terrain[0].x, terrain[0].y);
      for (i = 1; i < terrain.length; i++) ctx.lineTo(terrain[i].x, terrain[i].y);
      ctx.stroke();
      ctx.shadowBlur = 0;

      // craters
      ctx.fillStyle = 'rgba(0,0,0,0.22)';
      for (i = 0; i < 14; i++) {
        var cx2 = ((i * 331.7) % (W + 80)) - 40;
        var cy2 = terrainYAt(cx2) + 14 + (i % 3) * 16;
        if (cy2 < H + 10) {
          ctx.beginPath();
          ctx.ellipse(cx2, cy2, 10 + (i % 4) * 5, 3.5, 0, 0, Math.PI * 2);
          ctx.fill();
        }
      }
    }

    // landing pad — pulsing beacon
    var pulse = 0.5 + 0.5 * Math.sin(t * 4);
    ctx.strokeStyle = '#37e05a';
    ctx.lineWidth = 4;
    ctx.shadowColor = '#37e05a';
    ctx.shadowBlur = 8 + pulse * 10;
    ctx.beginPath();
    ctx.moveTo(pad.x1, pad.y); ctx.lineTo(pad.x2, pad.y);
    ctx.stroke();
    ctx.shadowBlur = 0;
    // pad legs
    ctx.strokeStyle = '#229644';
    ctx.lineWidth = 2;
    ctx.beginPath();
    ctx.moveTo(pad.x1 + 4, pad.y); ctx.lineTo(pad.x1 + 4, pad.y + 10);
    ctx.moveTo(pad.x2 - 4, pad.y); ctx.lineTo(pad.x2 - 4, pad.y + 10);
    ctx.stroke();
    // beacon lights
    var bl = 0.4 + 0.6 * pulse;
    ctx.fillStyle = 'rgba(90,255,140,' + bl.toFixed(2) + ')';
    ctx.beginPath(); ctx.arc(pad.x1 + 6, pad.y - 8, 3, 0, Math.PI * 2); ctx.fill();
    ctx.beginPath(); ctx.arc(pad.x2 - 6, pad.y - 8, 3, 0, Math.PI * 2); ctx.fill();
    // holographic "SHU YERGA QO‘NING" tag
    ctx.font = '700 10px Consolas, monospace';
    ctx.textAlign = 'center';
    ctx.fillStyle = 'rgba(90,255,140,' + (0.35 + 0.3 * pulse).toFixed(2) + ')';
    ctx.fillText(arcadePlus ? ('WIND '+ (gust()<0?'←':'→')+' '+ Math.round(Math.abs(gust()))) : 'SHU YERGA QO‘NING', (pad.x1 + pad.x2) / 2, pad.y - 18);

    // projected descent guide (dotted)
    if (state === 'flying' && lander.vy > 0) {
      ctx.strokeStyle = 'rgba(140,170,255,0.22)';
      ctx.setLineDash([3, 7]);
      ctx.lineWidth = 1.5;
      ctx.beginPath();
      var px = lander.x, py2 = lander.y;
      var pvx = lander.vx, pvy = lander.vy, steps = 26;
      ctx.moveTo(px, py2);
      for (i = 0; i < steps; i++) {
        pvy += GRAVITY * 0.09;
        px += pvx * 0.09; py2 += pvy * 0.09;
        if (py2 > terrainYAt(px)) break;
        ctx.lineTo(px, py2);
      }
      ctx.stroke();
      ctx.setLineDash([]);
    }

    // exhaust / explosion particles
    for (i = 0; i < particles.length; i++) {
      var p = particles[i];
      ctx.globalAlpha = Math.max(0, p.life / p.max);
      ctx.fillStyle = p.color;
      ctx.beginPath(); ctx.arc(p.x, p.y, p.r, 0, Math.PI * 2); ctx.fill();
    }
    ctx.globalAlpha = 1;

    // dust
    for (i = 0; i < dust.length; i++) {
      var d2 = dust[i];
      ctx.globalAlpha = Math.max(0, d2.life / d2.max) * 0.6;
      ctx.fillStyle = '#9aa0c0';
      ctx.beginPath(); ctx.arc(d2.x, d2.y, d2.r, 0, Math.PI * 2); ctx.fill();
    }
    ctx.globalAlpha = 1;

    if (state !== "crashed" && state !== "title") drawLander();
    if (state === "title") drawLander();

    ctx.restore();
    drawHUD();

    if (state === "title") {
      titleScreen();
    } else if (state === "landed" && stateTimer > 0.25) {
      banner("TOUCHDOWN!", "+" + lastAward + " pts" + (streak > 1 ? "  ·  streak x" + streak : ""), "#37e05a", IS_TOUCH ? "Tap to fly again" : "Press Space / R to fly again");
    } else if (state === "crashed" && stateTimer > 0.4) {
      banner("CRASHED!", "Score " + score + "  ·  Best " + best, "#ff5b52", IS_TOUCH ? "Tap to try again" : "Press Space / R to retry");
    }
  }

  function drawLander() {
    ctx.save();
    ctx.translate(lander.x, lander.y);
    ctx.rotate(lander.angle);

    if (lander.thrusting) {
      var fl = 12 + Math.random() * 12;
      var fg = ctx.createLinearGradient(0, LANDER_R, 0, LANDER_R + fl + 8);
      fg.addColorStop(0, 'rgba(255,220,140,0.95)');
      fg.addColorStop(0.5, 'rgba(255,140,60,0.7)');
      fg.addColorStop(1, 'rgba(255,80,40,0)');
      ctx.fillStyle = fg;
      ctx.beginPath();
      ctx.moveTo(-5, LANDER_R - 2);
      ctx.lineTo(0, LANDER_R + fl + 8);
      ctx.lineTo(5, LANDER_R - 2);
      ctx.closePath(); ctx.fill();
    }

    // legs
    ctx.strokeStyle = '#aab2cc';
    ctx.lineWidth = 2;
    ctx.beginPath();
    ctx.moveTo(-7, 2); ctx.lineTo(-11, LANDER_R);
    ctx.moveTo(7, 2); ctx.lineTo(11, LANDER_R);
    ctx.stroke();
    // footpads
    ctx.fillStyle = '#8890b0';
    ctx.fillRect(-13.5, LANDER_R - 1, 5, 2.5);
    ctx.fillRect(8.5, LANDER_R - 1, 5, 2.5);

    // body with glow
    ctx.shadowColor = 'rgba(140,190,255,0.55)';
    ctx.shadowBlur = 10;
    var bg2 = ctx.createLinearGradient(-8, 0, 8, 0);
    bg2.addColorStop(0, '#f0f0fa');
    bg2.addColorStop(0.5, '#c8c8e0');
    bg2.addColorStop(1, '#9090b8');
    ctx.fillStyle = bg2;
    ctx.beginPath();
    ctx.moveTo(0, -LANDER_R);
    ctx.lineTo(-8, -2); ctx.lineTo(-8, 6); ctx.lineTo(8, 6); ctx.lineTo(8, -2);
    ctx.closePath(); ctx.fill();
    ctx.shadowBlur = 0;
    ctx.strokeStyle = '#7878a0';
    ctx.lineWidth = 1;
    ctx.stroke();

    // window
    var wg = ctx.createRadialGradient(-1, -3, 0.5, 0, -2, 3.4);
    wg.addColorStop(0, '#c8f0ff');
    wg.addColorStop(1, '#3a9ae0');
    ctx.fillStyle = wg;
    ctx.beginPath(); ctx.arc(0, -2, 3, 0, Math.PI * 2); ctx.fill();

    ctx.restore();
  }

  function drawHUD() {
    var alt = Math.max(0, Math.round(terrainYAt(lander.x) - (lander.y + LANDER_R)));
    var hs = Math.round(Math.abs(lander.vx));
    var vs = Math.round(lander.vy);
    var okH = hs <= SAFE_VX, okV = vs <= SAFE_VY;

    // left instrument panel
    var pw = 176, ph2 = 108, px2 = 12, py3 = 12;
    ctx.fillStyle = 'rgba(8,12,28,0.62)';
    ctx.strokeStyle = 'rgba(120,150,255,0.25)';
    ctx.lineWidth = 1;
    roundRect(px2, py3, pw, ph2, 10); ctx.fill(); ctx.stroke();

    ctx.font = '700 13px Consolas, monospace';
    ctx.textAlign = 'left';
    ctx.fillStyle = '#9fb4e8';
    ctx.fillText('ALT', px2 + 12, py3 + 22);
    ctx.fillText('H-SPD', px2 + 12, py3 + 42);
    ctx.fillText('V-SPD', px2 + 12, py3 + 62);
    ctx.fillText('FUEL', px2 + 12, py3 + 92);

    ctx.textAlign = 'right';
    ctx.fillStyle = '#e8ecff';
    ctx.fillText(alt + ' m', px2 + pw - 14, py3 + 22);
    ctx.fillStyle = okH ? '#e8ecff' : '#ff6a5e';
    ctx.fillText(hs + (okH ? '' : ' !'), px2 + pw - 14, py3 + 42);
    ctx.fillStyle = okV ? '#e8ecff' : '#ff6a5e';
    ctx.fillText(vs + (okV ? '' : ' !'), px2 + pw - 14, py3 + 62);

    // fuel bar
    var gx = px2 + 52, gy2 = py3 + 82, gw = pw - 66, gh = 10;
    ctx.strokeStyle = 'rgba(140,160,220,0.4)';
    ctx.strokeRect(gx, gy2, gw, gh);
    var frac = lander.fuel / FUEL_MAX;
    var fc = frac > 0.3 ? '#37e05a' : (frac > 0.12 ? '#ffcc33' : '#ff4444');
    if (lowFuelWarned && frac < 0.22 && frac > 0 && Math.sin(performance.now() / 90) > 0) fc = '#ff2222';
    ctx.fillStyle = fc;
    ctx.fillRect(gx + 1, gy2 + 1, (gw - 2) * frac, gh - 2);

    // score panel (right, offset below AAA hud)
    ctx.textAlign = 'right';
    ctx.fillStyle = 'rgba(8,12,28,0.62)';
    var sw = 132;
    roundRect(W - sw - 12, 58, sw, 64, 10); ctx.fill(); ctx.stroke();
    ctx.fillStyle = '#9fb4e8';
    ctx.font = '700 11px Consolas, monospace';
    ctx.fillText('SCORE', W - 24, 76);
    ctx.fillStyle = '#fff';
    ctx.font = '800 20px Consolas, monospace';
    ctx.fillText(String(score), W - 24, 96);
    ctx.fillStyle = '#8a94b8';
    ctx.font = '700 11px Consolas, monospace';
    ctx.fillText('REKORD ' + Math.max(best, score), W - 24, 112);
    if (streak > 1) {
      ctx.fillStyle = '#f472b6';
      ctx.fillText('STREAK x' + streak, W - 24, 128 - 14 + 2);
    }

    // controls hint (desktop only)
    if (!IS_TOUCH) {
      ctx.fillStyle = 'rgba(190,200,230,0.5)';
      ctx.font = '12px Consolas, monospace';
      ctx.textAlign = 'center';
      ctx.fillText('A/D yoki ← → burilish · W yoki ↑ dvigatel · R qaytadan', W / 2, H - 14);
    }
  }

  function roundRect(x, y, w, h, r) {
    ctx.beginPath();
    if (ctx.roundRect) ctx.roundRect(x, y, w, h, r);
    else { ctx.moveTo(x + r, y); ctx.arcTo(x + w, y, x + w, y + h, r); ctx.arcTo(x + w, y + h, x, y + h, r); ctx.arcTo(x, y + h, x, y, r); ctx.arcTo(x, y, x + w, y, r); ctx.closePath(); }
  }

  function titleScreen() {
    ctx.fillStyle = 'rgba(4,6,16,0.62)';
    ctx.fillRect(0, 0, W, H);
    var cx = W / 2, cy = H * 0.34;
    var bob = Math.sin(performance.now() / 500) * 5;

    ctx.textAlign = 'center';
    ctx.save();
    ctx.shadowColor = 'rgba(90,140,255,0.9)';
    ctx.shadowBlur = 30;
    ctx.fillStyle = '#cfe0ff';
    ctx.font = '900 ' + Math.min(52, W * 0.09) + 'px "Segoe UI", sans-serif';
    ctx.fillText('OYGA QO‘NISH', cx, cy + bob);
    ctx.restore();

    ctx.fillStyle = '#9fb4e8';
    ctx.font = '600 ' + Math.min(16, W * 0.04) + 'px "Segoe UI", sans-serif';
    ctx.fillText('Sekin tushing va yashil maydonga qo‘ning', cx, cy + 56 + bob * 0.5);
    ctx.fillText('— tezlikni kamaytiring va kemani tekislang.', cx, cy + 78 + bob * 0.5);

    ctx.fillStyle = 'rgba(255,255,255,0.75)';
    ctx.font = '500 14px "Segoe UI", sans-serif';
    ctx.fillText(IS_TOUCH ? 'Ekrandagi tugmalardan foydalaning' : 'A/D yoki ← → burilish · W yoki ↑ dvigatel', cx, cy + 116);

    var bl = 0.55 + 0.45 * Math.sin(performance.now() / 280);
    ctx.globalAlpha = bl;
    ctx.fillStyle = '#ffd76a';
    ctx.font = '800 18px "Segoe UI", sans-serif';
    ctx.fillText(IS_TOUCH ? 'UCHIRISH UCHUN BOSING' : 'SPACE YOKI BOSISH BILAN UCHIRING', cx, cy + 170);
    ctx.globalAlpha = 1;

    if (best > 0) {
      ctx.fillStyle = '#8a94b8';
      ctx.font = '700 14px Consolas, monospace';
      ctx.fillText('REKORD  ' + best, cx, cy + 210);
    }
  }

  function banner(title, sub, color, hint) {
    var by = H * 0.32;
    ctx.fillStyle = 'rgba(4,6,16,0.6)';
    ctx.fillRect(0, by - 10, W, 140);
    ctx.textAlign = 'center';
    ctx.save();
    ctx.shadowColor = color; ctx.shadowBlur = 24;
    ctx.fillStyle = color;
    ctx.font = '900 ' + Math.min(46, W * 0.08) + 'px "Segoe UI", sans-serif';
    ctx.fillText(title, W / 2, by + 46);
    ctx.restore();
    ctx.fillStyle = '#e8ecff';
    ctx.font = '600 18px "Segoe UI", sans-serif';
    ctx.fillText(sub, W / 2, by + 80);
    var bl = 0.5 + 0.5 * Math.sin(performance.now() / 280);
    ctx.globalAlpha = bl;
    ctx.fillStyle = '#ffd76a';
    ctx.font = '700 15px "Segoe UI", sans-serif';
    ctx.fillText(hint, W / 2, by + 112);
    ctx.globalAlpha = 1;
  }

  // ---------- Main loop ----------
  var last = performance.now();
  function frame(now) {
    var dt = Math.min(0.05, (now - last) / 1000);
    last = now;
    update(dt);
    draw();
    requestAnimationFrame(frame);
  }

  resize();
  spawnLander();
  requestAnimationFrame(frame);

  window.__arcadeNativeUpgrade = {"id": 17, "name": "Crosswind Landing", "description": "approach with reduced fuel, a narrower pad and horizontal gusts; the pad beacon previews wind direction and strength."};
  // Report native gameplay state to the optional mission engine.
  window.__arcadeMissionSource = Object.assign({"id":17,"label":"Safe landings","goals":[1,2,4],"note":"Set down on landing pads without crashing."}, { read: () => { return (landings || 0); } });
})();
