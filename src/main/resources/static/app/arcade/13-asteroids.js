// Arcade 100 · MIT · see LICENSE
(function () {
  'use strict';

  var canvas = document.getElementById('game');
  var ctx = canvas.getContext('2d');
  var W = canvas.width, H = canvas.height;
  var scoreEl = document.getElementById('score');
  var levelEl = document.getElementById('level');
  var livesEl = document.getElementById('lives');
  var bestEl = document.getElementById('best');
  var screen = document.getElementById('screen');
  var screenBest = document.getElementById('screenBest');

  var best = 0;
  try { best = parseInt(arcadeStorage.getItem(('asteroids_best' + (new URLSearchParams(location.search).get('mode')==='plus'?'_plus':''))) || '0', 10) || 0; } catch (e) {}
  bestEl.textContent = best;
  screenBest.textContent = best > 0 ? 'Rekord: ' + best : '';

  var ship, rocks, bullets, parts;
  var score, lives, level, playing, deadTimer;
  var keys = {};
const arcadePlus = (new URLSearchParams(location.search).get('mode') === 'plus');
  var touch = { left: false, right: false, thrust: false, fire: false, brake: false };

  // ---------- audio ----------
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
    var t = c.currentTime + (delay || 0);
    try {
      var o = c.createOscillator(), g = c.createGain();
      o.type = type || 'sine';
      o.frequency.setValueAtTime(f, t);
      if (slide) o.frequency.exponentialRampToValueAtTime(Math.max(25, slide), t + d);
      g.gain.setValueAtTime(0.0001, t);
      g.gain.linearRampToValueAtTime(v || 0.05, t + 0.01);
      g.gain.exponentialRampToValueAtTime(0.0001, t + d);
      o.connect(g); g.connect(c.destination);
      o.start(t); o.stop(t + d + 0.03);
    } catch (e) {}
  }
  function noiseSfx(d, v, type, f) {
    var c = ac(); if (!c) return;
    var t = c.currentTime;
    var len = Math.floor(c.sampleRate * d);
    var buf = c.createBuffer(1, len, c.sampleRate);
    var ch = buf.getChannelData(0);
    for (var i = 0; i < len; i++) ch[i] = (Math.random() * 2 - 1) * (1 - i / len);
    var src = c.createBufferSource(); src.buffer = buf;
    var fl = c.createBiquadFilter(); fl.type = type || 'lowpass'; fl.frequency.value = f || 700;
    var g = c.createGain(); g.gain.setValueAtTime(v || 0.07, t); g.gain.exponentialRampToValueAtTime(0.0001, t + d);
    src.connect(fl); fl.connect(g); g.connect(c.destination); src.start(t);
  }
  var sfx = {
    shoot:  function () { tone(880, 0.09, 'sawtooth', 0.04, 220); },
    rockL:  function () { noiseSfx(0.25, 0.1, 'lowpass', 500); tone(180, 0.2, 'square', 0.05, 70); },
    rockS:  function () { noiseSfx(0.14, 0.08, 'bandpass', 1400); tone(420, 0.09, 'square', 0.04, 160); },
    die:    function () { noiseSfx(0.5, 0.14, 'lowpass', 350); tone(160, 0.45, 'sawtooth', 0.07, 50); },
    hyper:  function () { tone(300, 0.3, 'sine', 0.06, 1400); noiseSfx(0.2, 0.04, 'highpass', 2000); },
    spawn:  function () { [262, 330, 392, 523].forEach(function (f, i) { tone(f, 0.1, 'sine', 0.05, null, i * 0.06); }); },
    level:  function () { [330, 415, 494, 659].forEach(function (f, i) { tone(f, 0.14, 'triangle', 0.06, null, i * 0.09); }); },
    over:   function () { [294, 233, 196, 147].forEach(function (f, i) { tone(f, 0.24, 'sawtooth', 0.05, null, i * 0.15); }); }
  };
  // thrust rumble loop
  var thrustNodes = null;
  function setThrust(on) {
    var c = ac(); if (!c) return;
    if (on && !thrustNodes) {
      try {
        var len = Math.floor(c.sampleRate * 0.5);
        var buf = c.createBuffer(1, len, c.sampleRate);
        var ch = buf.getChannelData(0);
        for (var i = 0; i < len; i++) ch[i] = Math.random() * 2 - 1;
        var src = c.createBufferSource(); src.buffer = buf; src.loop = true;
        var fl = c.createBiquadFilter(); fl.type = 'lowpass'; fl.frequency.value = 260;
        var g = c.createGain(); g.gain.setValueAtTime(0.0001, c.currentTime);
        g.gain.linearRampToValueAtTime(0.09, c.currentTime + 0.1);
        src.connect(fl); fl.connect(g); g.connect(c.destination);
        src.start();
        thrustNodes = { src: src, g: g };
      } catch (e) {}
    } else if (!on && thrustNodes) {
      try {
        thrustNodes.g.gain.linearRampToValueAtTime(0.0001, c.currentTime + 0.1);
        var s = thrustNodes.src;
        setTimeout(function () { try { s.stop(); } catch (e) {} }, 150);
      } catch (e) {}
      thrustNodes = null;
    }
  }
  var bgm = {
    t: null, step: 0,
    seq: [110, 0, 110, 0, 130.8, 0, 110, 0, 98, 0, 98, 0, 146.8, 0, 130.8, 0],
    start: function () { this.stop(); var s = this; this.t = setInterval(function () { s.tickB(); }, 420); },
    tickB: function () {
      var c = ac(); if (!c || c.state !== 'running') return;
      try { if (arcadeStorage.getItem('aaa_bgm') === '1') return; } catch (e) {}
      var n = this.seq[this.step++ % 16];
      if (n) tone(n, 0.3, 'triangle', 0.03);
    },
    stop: function () { if (this.t) clearInterval(this.t); this.t = null; }
  };

  // ---------- visuals ----------
  var stars = [], nebulae = [];
  for (var i = 0; i < 3; i++) {
    var layer = [];
    for (var j = 0; j < [50, 28, 14][i]; j++) {
      layer.push({ x: Math.random() * W, y: Math.random() * H, s: 0.5 + i * 0.5 });
    }
    stars.push({ pts: layer, depth: 0.15 + i * 0.35 });
  }
  for (var n = 0; n < 3; n++) {
    nebulae.push({ x: Math.random() * W, y: Math.random() * H, r: 90 + Math.random() * 130, hue: 250 + Math.random() * 60, a: 0.05 + Math.random() * 0.04 });
  }

  function makeRock(x, y, size) {
    var pts = [];
    var n = 8 + Math.floor(Math.random() * 4);
    for (var i = 0; i < n; i++) {
      pts.push(0.7 + Math.random() * 0.5);
    }
    var speed = 40 + level * 8 + Math.random() * 40;
    var a = Math.random() * Math.PI * 2;
    return { x: x, y: y, vx: Math.cos(a) * speed, vy: Math.sin(a) * speed,
      size: size, r: size * 16, rot: Math.random() * 7, vrot: (Math.random() - 0.5) * 1.6, pts: pts };
  }

  function spawnLevel() {
    rocks = [];
    var count = (arcadePlus ? 4 : 3) + Math.min(level,18);
    for (var i = 0; i < count; i++) {
      var x, y;
      do {
        x = Math.random() * W; y = Math.random() * H;
      } while (Math.hypot(x - ship.x, y - ship.y) < 140);
      rocks.push(makeRock(x, y, 3));
    }
  }

  function reset() {
    ship = { x: W / 2, y: H / 2, a: -Math.PI / 2, vx: 0, vy: 0, inv: 2.5, cd: 0, dead: false };
    bullets = []; parts = [];
    score = 0; lives = 3; level = 1; deadTimer = 0;
    scoreEl.textContent = '0';
    livesEl.textContent = '3';
    levelEl.textContent = '1';
    spawnLevel();
  }

  function boom(x, y, n, color) {
    for (var i = 0; i < n; i++) {
      var a = Math.random() * Math.PI * 2, s = 40 + Math.random() * 200;
      parts.push({ x: x, y: y, vx: Math.cos(a) * s, vy: Math.sin(a) * s, life: .4 + Math.random() * .6, color: color || '#c9b8ff', r: 1 + Math.random() * 2.5 });
    }
  }

  function addScore(v) {
    score += v;
    scoreEl.textContent = score;
    if (score > best) { best = score; bestEl.textContent = best; try { arcadeStorage.setItem(('asteroids_best' + (new URLSearchParams(location.search).get('mode')==='plus'?'_plus':'')), String(best)); } catch (e) {} }
  }

  function wrap(o) {
    if (o.x < -20) o.x += W + 40; if (o.x > W + 20) o.x -= W + 40;
    if (o.y < -20) o.y += H + 40; if (o.y > H + 20) o.y -= H + 40;
  }

  function hyperspace() {
    boom(ship.x, ship.y, 20, '#7df0ff');
    sfx.hyper();
    ship.x = Math.random() * W;
    ship.y = Math.random() * H;
    ship.vx *= 0.3; ship.vy *= 0.3;
    ship.inv = 1.2;
    // small chance of disaster
    if (Math.random() < 0.08) killShip();
  }

  function killShip() {
    if (ship.dead || ship.inv > 0) return;
    ship.dead = true;
    deadTimer = 1.4;
    setThrust(false);
    boom(ship.x, ship.y, 40, '#7df0ff');
    sfx.die();
    lives--;
    livesEl.textContent = Math.max(0, lives);
    if (lives <= 0) {
      setTimeout(gameOver, 900);
    }
  }

  function gameOver() {
    playing = false;
    sfx.over();
    screen.querySelector('h2').textContent = 'O‘YIN TUGADI';
    screen.querySelector('p').innerHTML = 'Final score: <b style="color:#c9b8ff">' + score + '</b> · Level ' + level;
    screenBest.textContent = score >= best && score > 0 ? 'YANGI REKORD!' : 'Rekord: ' + best;
    screen.querySelector('button').textContent = 'RETRY (Space)';
    screen.classList.remove('hidden');
  }

  var last = performance.now();
  function loop(now) {
    var dt = Math.min(0.04, (now - last) / 1000);
    last = now;
    requestAnimationFrame(loop);
    if (playing) update(dt);
    draw(now);
  }

  function update(dt) {
    var L = keys.ArrowLeft || keys.a || touch.left;
    var R = keys.ArrowRight || keys.d || touch.right;
    var T = keys.ArrowUp || keys.w || touch.thrust;
    var F = keys[' '] || touch.fire;

    if (!ship.dead) {
      if (L) ship.a -= 4.6 * dt;
      if (R) ship.a += 4.6 * dt;
      if (T) {
        ship.vx += Math.cos(ship.a) * 260 * dt;
        ship.vy += Math.sin(ship.a) * 260 * dt;
        if (Math.random() < 0.6) {
          parts.push({ x: ship.x - Math.cos(ship.a) * 14, y: ship.y - Math.sin(ship.a) * 14,
            vx: -Math.cos(ship.a) * 90 + (Math.random() - .5) * 50, vy: -Math.sin(ship.a) * 90 + (Math.random() - .5) * 50,
            life: .3, color: '#ff9a5a', r: 2 });
        }
      }
      setThrust(T && !ship.dead);
      var drag = arcadePlus && (keys.b || keys.B || touch.brake) ? 4.8 : 0.6;
      ship.vx *= 1-drag*dt; ship.vy *= 1-drag*dt;
      var sp = Math.hypot(ship.vx, ship.vy);
      if (sp > 380) { ship.vx *= 380 / sp; ship.vy *= 380 / sp; }
      ship.x += ship.vx * dt;
      ship.y += ship.vy * dt;
      wrap(ship);
      ship.inv -= dt;
      ship.cd -= dt;
      if (F && ship.cd <= 0) {
        ship.cd = 0.22;
        bullets.push({ x: ship.x + Math.cos(ship.a) * 14, y: ship.y + Math.sin(ship.a) * 14,
          vx: Math.cos(ship.a) * 480 + ship.vx * 0.4, vy: Math.sin(ship.a) * 480 + ship.vy * 0.4, life: 1.1 });
        sfx.shoot();
      }
    } else {
      deadTimer -= dt;
      if (deadTimer <= 0 && lives > 0) {
        ship.dead = false;
        ship.x = W / 2; ship.y = H / 2;
        ship.vx = ship.vy = 0;
        ship.inv = 2.5;
        sfx.spawn();
      }
    }

    bullets.forEach(function (b) { b.x += b.vx * dt; b.y += b.vy * dt; b.life -= dt; wrap(b); });
    bullets = bullets.filter(function (b) { return b.life > 0; });

    rocks.forEach(function (r) {
      r.x += r.vx * dt; r.y += r.vy * dt; r.rot += r.vrot * dt;
      wrap(r);
    });

    // bullet-rock collisions
    bullets.forEach(function (b) {
      rocks.forEach(function (r) {
        if (r.size <= 0) return;
        var dx = b.x - r.x, dy = b.y - r.y;
        if (dx * dx + dy * dy < r.r * r.r * 0.8) {
          b.life = 0;
          splitRock(r);
        }
      });
    });
    bullets = bullets.filter(function (b) { return b.life > 0; });
    rocks = rocks.filter(function (r) { return r.size > 0; });

    // ship-rock collisions
    if (!ship.dead && ship.inv <= 0) {
      for (var i = 0; i < rocks.length; i++) {
        var r = rocks[i];
        var dx = ship.x - r.x, dy = ship.y - r.y;
        if (dx * dx + dy * dy < (r.r * 0.85 + 10) * (r.r * 0.85 + 10)) {
          killShip();
          break;
        }
      }
    }

    for (var i = parts.length - 1; i >= 0; i--) {
      var p = parts[i];
      p.x += p.vx * dt; p.y += p.vy * dt; p.life -= dt;
      if (p.life <= 0) parts.splice(i, 1);
    }

    if (rocks.length === 0 && playing) {
      level++;
      levelEl.textContent = level;
      addScore(150);
      sfx.level();
      spawnLevel();
    }
  }

  function splitRock(r) {
    boom(r.x, r.y, r.size === 3 ? 18 : 10, '#b8a8e8');
    addScore(r.size === 3 ? 20 : r.size === 2 ? 50 : 100);
    if (r.size === 3) sfx.rockL(); else sfx.rockS();
    if (r.size > 1) {
      rocks.push(makeRock(r.x, r.y, r.size - 1));
      rocks.push(makeRock(r.x, r.y, r.size - 1));
    }
    r.size = 0;
  }

  function draw(now) {
    ctx.fillStyle = '#05040e';
    ctx.fillRect(0, 0, W, H);

    // nebulae
    nebulae.forEach(function (nb) {
      var g = ctx.createRadialGradient(nb.x, nb.y, 0, nb.x, nb.y, nb.r);
      g.addColorStop(0, 'hsla(' + nb.hue + ',70%,60%,' + nb.a + ')');
      g.addColorStop(1, 'transparent');
      ctx.fillStyle = g;
      ctx.fillRect(nb.x - nb.r, nb.y - nb.r, nb.r * 2, nb.r * 2);
    });

    // parallax stars drift with ship velocity
    stars.forEach(function (l) {
      ctx.fillStyle = 'rgba(255,255,255,' + (0.2 + l.depth * 0.6) + ')';
      l.pts.forEach(function (p) {
        if (playing && ship) {
          p.x -= ship.vx * l.depth * 0.02;
          p.y -= ship.vy * l.depth * 0.02;
          wrap(p);
        }
        ctx.fillRect(p.x, p.y, p.s, p.s);
      });
    });

    // rocks
    rocks.forEach(function (r) {
      ctx.save();
      ctx.translate(r.x, r.y);
      ctx.rotate(r.rot);
      ctx.strokeStyle = '#b8a8e8';
      ctx.lineWidth = 2;
      ctx.shadowColor = '#9a7ae8';
      ctx.shadowBlur = 6;
      ctx.beginPath();
      for (var i = 0; i < r.pts.length; i++) {
        var a = i / r.pts.length * Math.PI * 2;
        var rad = r.r * r.pts[i];
        if (i === 0) ctx.moveTo(Math.cos(a) * rad, Math.sin(a) * rad);
        else ctx.lineTo(Math.cos(a) * rad, Math.sin(a) * rad);
      }
      ctx.closePath();
      ctx.stroke();
      ctx.restore();
    });
    ctx.shadowBlur = 0;

    // ship
    if (ship && !ship.dead && (ship.inv <= 0 || Math.floor(now / 90) % 2 === 0)) {
      ctx.save();
      ctx.translate(ship.x, ship.y);
      ctx.rotate(ship.a);
      ctx.strokeStyle = '#e8e0ff';
      ctx.lineWidth = 2;
      ctx.shadowColor = '#9a7ae8';
      ctx.shadowBlur = 10;
      ctx.beginPath();
      ctx.moveTo(14, 0);
      ctx.lineTo(-10, -9);
      ctx.lineTo(-5, 0);
      ctx.lineTo(-10, 9);
      ctx.closePath();
      ctx.stroke();
      ctx.restore();
      ctx.shadowBlur = 0;
    }

    // bullets
    ctx.fillStyle = '#fff';
    ctx.shadowColor = '#7df0ff'; ctx.shadowBlur = 8;
    bullets.forEach(function (b) {
      ctx.beginPath();
      ctx.arc(b.x, b.y, 2.4, 0, Math.PI * 2);
      ctx.fill();
    });
    ctx.shadowBlur = 0;

    // particles
    parts.forEach(function (p) {
      ctx.globalAlpha = Math.min(1, p.life * 2);
      ctx.fillStyle = p.color;
      ctx.fillRect(p.x - p.r, p.y - p.r, p.r * 2, p.r * 2);
    });
    ctx.globalAlpha = 1;
  }

  document.addEventListener('keydown', function (e) {
    keys[e.key] = true;
    if (e.key === 'Shift' && playing && !ship.dead) hyperspace();
    if ((e.key === ' ' || e.key === 'Enter') && !playing && !screen.classList.contains('hidden')) { startGame(); e.preventDefault(); }
    if ([' ', 'ArrowUp', 'ArrowDown', 'ArrowLeft', 'ArrowRight'].includes(e.key)) e.preventDefault();
  });
  document.addEventListener('keyup', function (e) { keys[e.key] = false; });

  if (arcadePlus) {
    var brake = document.createElement('button');
    brake.dataset.k = 'brake'; brake.textContent='Brake'; brake.setAttribute('aria-label','Hold brake');
    document.querySelector('#touchpad .tgroup').appendChild(brake);
    document.getElementById('asteroid-help').append(' · B brakes');
  }
  document.querySelectorAll('#touchpad button').forEach(function (b) {
    var k = b.dataset.k;
    b.addEventListener('pointerdown', function (e) { e.preventDefault(); ac(); touch[k] = true; });
    b.addEventListener('pointerup', function () { touch[k] = false; });
    b.addEventListener('pointercancel', function () { touch[k] = false; });
    b.addEventListener('pointerleave', function () { touch[k] = false; });
  });
  canvas.addEventListener('pointerdown', function (e) { e.preventDefault(); ac(); });

  function startGame() {
    ac();
    reset();
    playing = true;
    bgm.start();
    screen.classList.add('hidden');
  }
  screen.querySelector('button').addEventListener('click', startGame);

  reset();
  requestAnimationFrame(loop);

  window.__arcadeNativeUpgrade = {"id": 13, "name": "Vector Brake", "description": "hold B or the touch brake for tighter steering while faster asteroid fields push you to manage momentum."};
  // Report native gameplay state to the optional mission engine.
  window.__arcadeMissionSource = Object.assign({"id":13,"label":"Asteroid points","goals":[300,700,1300],"note":"Break rocks into smaller targets and clear the field without losing your ship."}, { read: () => { return (score || 0); } });
})();
