// Arcade 100 · MIT · see LICENSE
(function () {
  'use strict';

  var canvas = document.getElementById('game');
  var ctx = canvas.getContext('2d');
  var overlay = document.getElementById('overlay');
  var overlayTitle = document.getElementById('overlayTitle');
  var overlaySub = document.getElementById('overlaySub');
  var restartBtn = document.getElementById('restartBtn');
  var winsEl = document.getElementById('wins');
  var youScoreEl = document.getElementById('youScore');
  var aiScoreEl = document.getElementById('aiScore');

  var W = canvas.width, H = canvas.height;
const arcadePlus = (new URLSearchParams(location.search).get('mode') === 'plus');
  var WIN_SCORE = arcadePlus ? 7 : 10;

  var PADDLE_W = 12, PADDLE_H = 90, PADDLE_MARGIN = 24;
  var BALL_R = 7;
  var PLAYER_SPEED = 6.5, paddleMotion = 0, lastPaddleY = H / 2;
  var AI_SPEED = 4.6;
  var AI_REACT_ZONE = 0.45;
  var BALL_SPEED0 = 5.2;
  var BALL_SPEEDUP = arcadePlus ? 1.065 : 1.045;
  var BALL_MAX = 12;

  var player, ai, ball, keys, running, over;
  var trail = [], sparks = [], shake = 0, animT = 0, rally = 0, rallyFlash = 0;
  var totalWins = 0;
  try { totalWins = parseInt(arcadeStorage.getItem(('pong_wins' + (new URLSearchParams(location.search).get('mode')==='plus'?'_plus':''))), 10) || 0; } catch (e) {}

  // ---------- AAA audio ----------
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
      if (slide) o.frequency.exponentialRampToValueAtTime(Math.max(30, slide), t + d);
      g.gain.setValueAtTime(0.0001, t);
      g.gain.linearRampToValueAtTime(v || 0.05, t + 0.008);
      g.gain.exponentialRampToValueAtTime(0.0001, t + d);
      o.connect(g); g.connect(c.destination);
      o.start(t); o.stop(t + d + 0.03);
    } catch (e) {}
  }
  var sfx = {
    hit:   function (speed, isPlayer) { var f = isPlayer ? 380 : 300; tone(f + speed * 14, 0.07, 'square', 0.055, f * 0.7); },
    wall:  function () { tone(240, 0.04, 'square', 0.03, 180); },
    youPt: function () { tone(660, 0.1, 'sine', 0.07); tone(990, 0.18, 'sine', 0.06, null, 0.09); },
    aiPt:  function () { tone(330, 0.14, 'sawtooth', 0.05, 180); },
    win:   function () { [523, 659, 784, 1047, 1319].forEach(function (f, i) { tone(f, 0.16, 'triangle', 0.07, null, i * 0.09); }); },
    lose:  function () { [392, 311, 262, 196].forEach(function (f, i) { tone(f, 0.2, 'sawtooth', 0.05, null, i * 0.11); }); }
  };
  var bgm = {
    t: null, step: 0,
    bass: [82.4, 0, 82.4, 0, 98, 0, 73.4, 0],
    start: function () {
      this.stop(); this.step = 0; var self = this;
      this.t = setInterval(function () { self.tickB(); }, 230);
    },
    tickB: function () {
      var c = ac(); if (!c || c.state !== 'running') return;
      try { if (arcadeStorage.getItem('aaa_bgm') === '1') return; } catch (e) {}
      var st = this.step++;
      var n = this.bass[st % 8];
      if (n) tone(n, 0.2, 'triangle', 0.04);
      // pulse quickens as ball gets faster
      if (ball && Math.hypot(ball.vx, ball.vy) > 8 && st % 2 === 0) tone(n * 4, 0.05, 'square', 0.012);
    },
    stop: function () { if (this.t) clearInterval(this.t); this.t = null; }
  };

  function resetPositions(serveDir) {
    player = { x: PADDLE_MARGIN, y: H / 2 - PADDLE_H / 2, w: PADDLE_W, h: PADDLE_H };
    ai     = { x: W - PADDLE_MARGIN - PADDLE_W, y: H / 2 - PADDLE_H / 2, w: PADDLE_W, h: PADDLE_H };
    ball = {
      x: W / 2, y: H / 2,
      vx: BALL_SPEED0 * serveDir,
      vy: (Math.random() * 2 - 1) * 3,
      r: BALL_R
    };
    trail = [];
    rally = 0; lastPaddleY = player.y; paddleMotion = 0;
  }

  var score;
  function newGame() {
    ac();
    score = { you: 0, ai: 0 };
    youScoreEl.textContent = '0';
    aiScoreEl.textContent = '0';
    resetPositions(Math.random() < 0.5 ? -1 : 1);
    sparks = [];
    shake = 0;
    over = false;
    running = true;
    bgm.start();
    overlay.classList.add('hidden');
  }

  function endGame(youWon) {
    over = true;
    running = false;
    bgm.stop();
    if (youWon) {
      totalWins++;
      try { arcadeStorage.setItem(('pong_wins' + (new URLSearchParams(location.search).get('mode')==='plus'?'_plus':'')), String(totalWins)); } catch (e) {}
      sfx.win();
    } else {
      sfx.lose();
    }
    overlayTitle.textContent = youWon ? 'SIZ YUTDINGIZ' : 'KOMPYUTER YUTDI';
    overlayTitle.style.color = youWon ? '#7dd3fc' : '#f472b6';
    overlayTitle.style.textShadow = '0 0 30px ' + (youWon ? 'rgba(125,211,252,.8)' : 'rgba(244,114,182,.8)');
    overlaySub.innerHTML = 'Yakuniy hisob: ' + score.you + ' : ' + score.ai + '<br>Yana o‘ynash uchun bosing yoki Space tugmasidan foydalaning.';
    restartBtn.textContent = 'Qaytadan';
    winsEl.textContent = 'Jami g‘alabalar: ' + totalWins;
    overlay.classList.remove('hidden');
  }

  function clamp(v, lo, hi) { return v < lo ? lo : v > hi ? hi : v; }

  function sparkBurst(x, y, color, n) {
    for (var i = 0; i < n; i++) {
      var a = Math.random() * Math.PI * 2, s = 60 + Math.random() * 160;
      sparks.push({ x: x, y: y, vx: Math.cos(a) * s, vy: Math.sin(a) * s, life: 0.3 + Math.random() * 0.25, max: 0.55, color: color });
    }
  }

  function paddleHit(p, dir, isPlayer) {
    var rel = (ball.y - (p.y + p.h / 2)) / (p.h / 2);
    rel = clamp(rel, -1, 1);
    var speed = Math.min(Math.hypot(ball.vx, ball.vy) * BALL_SPEEDUP, BALL_MAX);
    var angle = rel * (Math.PI / 3);
    ball.vx = Math.cos(angle) * speed * dir;
    ball.vy = Math.sin(angle) * speed + (arcadePlus && isPlayer ? clamp(paddleMotion,-5,5)*0.55 : 0);
    ball.x = dir > 0 ? p.x + p.w + ball.r + 0.5 : p.x - ball.r - 0.5;
    rally++;
    rallyFlash = 1;
    shake = Math.min(4, 1 + speed * 0.2);
    sfx.hit(speed, isPlayer);
    sparkBurst(ball.x, ball.y, isPlayer ? '#7dd3fc' : '#f472b6', 8);
  }

  function point(winner) {
    score[winner]++;
    youScoreEl.textContent = score.you;
    aiScoreEl.textContent = score.ai;
    shake = 6;
    sparkBurst(ball.x < W / 2 ? 20 : W - 20, ball.y, winner === 'you' ? '#7dd3fc' : '#f472b6', 22);
    if (winner === 'you') sfx.youPt(); else sfx.aiPt();
    if (score.you >= WIN_SCORE && (!arcadePlus || score.you-score.ai >= 2)) { endGame(true); return; }
    if (score.ai >= WIN_SCORE && (!arcadePlus || score.ai-score.you >= 2)) { endGame(false); return; }
    resetPositions(winner === 'you' ? 1 : -1);
  }

  function update(dt) {
    animT += dt;
    shake = Math.max(0, shake - dt * 18);
    rallyFlash = Math.max(0, rallyFlash - dt * 3);

    if (keys.w) player.y -= PLAYER_SPEED;
    if (keys.s) player.y += PLAYER_SPEED;
    player.y = clamp(player.y, 0, H - player.h);

    paddleMotion = player.y - lastPaddleY; lastPaddleY = player.y;
    var aiCenter = ai.y + ai.h / 2;
    var ballComing = ball.vx > 0 || ball.x > W * AI_REACT_ZONE;
    var target = ballComing ? ball.y : H / 2;
    if (Math.abs(target - aiCenter) > 6) {
      ai.y += Math.sign(target - aiCenter) * Math.min(AI_SPEED, Math.abs(target - aiCenter));
    }
    ai.y = clamp(ai.y, 0, H - ai.h);

    ball.x += ball.vx;
    ball.y += ball.vy;

    trail.push({ x: ball.x, y: ball.y, life: 0.45 });
    if (trail.length > 26) trail.shift();
    for (var i = trail.length - 1; i >= 0; i--) {
      trail[i].life -= dt * 2;
      if (trail[i].life <= 0) trail.splice(i, 1);
    }
    for (var i = sparks.length - 1; i >= 0; i--) {
      var p = sparks[i];
      p.x += p.vx * dt; p.y += p.vy * dt; p.life -= dt;
      if (p.life <= 0) sparks.splice(i, 1);
    }

    if (ball.y - ball.r <= 0) { ball.y = ball.r; ball.vy = Math.abs(ball.vy); sfx.wall(); }
    if (ball.y + ball.r >= H) { ball.y = H - ball.r; ball.vy = -Math.abs(ball.vy); sfx.wall(); }

    if (ball.vx < 0 &&
        ball.x - ball.r <= player.x + player.w &&
        ball.x - ball.r >= player.x - 4 &&
        ball.y >= player.y - ball.r && ball.y <= player.y + player.h + ball.r) {
      paddleHit(player, 1, true);
    }
    if (ball.vx > 0 &&
        ball.x + ball.r >= ai.x &&
        ball.x + ball.r <= ai.x + ai.w + 4 &&
        ball.y >= ai.y - ball.r && ball.y <= ai.y + ai.h + ball.r) {
      paddleHit(ai, -1, false);
    }

    if (ball.x + ball.r < 0) point('ai');
    else if (ball.x - ball.r > W) point('you');
  }

  function drawPaddle(p, color, glow) {
    ctx.save();
    ctx.shadowColor = glow;
    ctx.shadowBlur = 14;
    var g = ctx.createLinearGradient(p.x, p.y, p.x + p.w, p.y);
    g.addColorStop(0, color);
    g.addColorStop(1, '#ffffff');
    ctx.fillStyle = g;
    ctx.beginPath();
    if (ctx.roundRect) { ctx.roundRect(p.x, p.y, p.w, p.h, 6); } else { ctx.rect(p.x, p.y, p.w, p.h); }
    ctx.fill();
    ctx.restore();
  }

  var last = performance.now();
  function draw() {
    ctx.clearRect(0, 0, W, H);
    ctx.save();
    if (shake > 0) ctx.translate((Math.random() - 0.5) * shake, (Math.random() - 0.5) * shake);

    // grid backdrop
    ctx.strokeStyle = 'rgba(125,180,252,0.05)';
    ctx.lineWidth = 1;
    for (var gx = 0; gx <= W; gx += 40) { ctx.beginPath(); ctx.moveTo(gx, 0); ctx.lineTo(gx, H); ctx.stroke(); }
    for (var gy = 0; gy <= H; gy += 40) { ctx.beginPath(); ctx.moveTo(0, gy); ctx.lineTo(W, gy); ctx.stroke(); }

    // center: animated dashed line + circle
    ctx.strokeStyle = 'rgba(140,160,220,0.25)';
    ctx.lineWidth = 2;
    ctx.setLineDash([10, 12]);
    ctx.lineDashOffset = -animT * 30;
    ctx.beginPath(); ctx.moveTo(W / 2, 0); ctx.lineTo(W / 2, H); ctx.stroke();
    ctx.setLineDash([]);
    ctx.strokeStyle = 'rgba(140,160,220,0.18)';
    ctx.beginPath(); ctx.arc(W / 2, H / 2, 60, 0, Math.PI * 2); ctx.stroke();
    ctx.beginPath(); ctx.arc(W / 2, H / 2, 4, 0, Math.PI * 2); ctx.fillStyle = 'rgba(140,160,220,0.3)'; ctx.fill();

    // trail
    for (var i = 0; i < trail.length; i++) {
      var t = trail[i];
      ctx.globalAlpha = t.life * 0.35;
      ctx.fillStyle = '#cfe8ff';
      ctx.beginPath(); ctx.arc(t.x, t.y, ball.r * t.life, 0, Math.PI * 2); ctx.fill();
    }
    ctx.globalAlpha = 1;

    drawPaddle(player, '#7dd3fc', 'rgba(125,211,252,.9)');
    drawPaddle(ai, '#f472b6', 'rgba(244,114,182,.9)');

    // ball
    ctx.save();
    ctx.shadowColor = '#ffffff';
    ctx.shadowBlur = 18;
    var bgr = ctx.createRadialGradient(ball.x - 2, ball.y - 2, 1, ball.x, ball.y, ball.r + 1);
    bgr.addColorStop(0, '#ffffff');
    bgr.addColorStop(1, '#9fb8ff');
    ctx.fillStyle = bgr;
    ctx.beginPath(); ctx.arc(ball.x, ball.y, ball.r, 0, Math.PI * 2); ctx.fill();
    ctx.restore();

    // sparks
    for (var i = 0; i < sparks.length; i++) {
      var p = sparks[i];
      ctx.globalAlpha = Math.max(0, p.life / p.max);
      ctx.fillStyle = p.color;
      ctx.beginPath(); ctx.arc(p.x, p.y, 2.4, 0, Math.PI * 2); ctx.fill();
    }
    ctx.globalAlpha = 1;

    // rally counter
    if (rally >= 5) {
      ctx.globalAlpha = 0.5 + rallyFlash * 0.5;
      ctx.font = '300 15px "Segoe UI", sans-serif';
      ctx.textAlign = 'center';
      ctx.fillStyle = '#e8d27c';
      ctx.fillText('RALLY ' + rally, W / 2, 30);
      ctx.globalAlpha = 1;
    }

    ctx.restore();
  }

  function loop(now) {
    var dt = Math.min(0.05, (now - last) / 1000);
    last = now;
    if (running) update(dt);
    draw();
    requestAnimationFrame(loop);
  }

  // --- input ---
  keys = { w: false, s: false };
  document.addEventListener('keydown', function (e) {
    var k = e.key.toLowerCase();
    if (k === 'w' || e.key === 'ArrowUp') keys.w = true;
    else if (k === 's' || e.key === 'ArrowDown') keys.s = true;
    else if ((k === ' ' || k === 'enter') && over) newGame();
    if (k === ' ' || k === 'arrowup' || k === 'arrowdown') e.preventDefault();
  });
  document.addEventListener('keyup', function (e) {
    var k = e.key.toLowerCase();
    if (k === 'w' || e.key === 'ArrowUp') keys.w = false;
    else if (k === 's' || e.key === 'ArrowDown') keys.s = false;
  });

  function pointerToY(clientY) {
    var rect = canvas.getBoundingClientRect();
    var y = (clientY - rect.top) * (H / rect.height);
    player.y = clamp(y - player.h / 2, 0, H - player.h);
  }

  canvas.addEventListener('mousemove', function (e) { pointerToY(e.clientY); });
  canvas.addEventListener('touchstart', function (e) {
    e.preventDefault();
    if (over) { newGame(); return; }
    pointerToY(e.touches[0].clientY);
  }, { passive: false });
  canvas.addEventListener('touchmove', function (e) {
    e.preventDefault();
    pointerToY(e.touches[0].clientY);
  }, { passive: false });

  restartBtn.addEventListener('click', newGame);

  score = { you: 0, ai: 0 };
  resetPositions(1);
  running = false;
  over = true;
  winsEl.textContent = totalWins > 0 ? 'Jami g‘alabalar: ' + totalWins : '';
  requestAnimationFrame(loop);

  window.__arcadeNativeUpgrade = {"id": 4, "name": "Spin Cup", "description": "first to seven by two, with paddle motion imparting spin and stronger late-rally ball speed."};
  // Report native gameplay state to the optional mission engine.
  window.__arcadeMissionSource = Object.assign({"id":4,"label":"Points past the CPU","goals":[3,6,10],"note":"Outplay the CPU. Gold means taking the match."}, { read: () => { return (score?.you || 0); } });
})();
