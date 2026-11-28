// Arcade 100 · MIT · see LICENSE
(() => {
  const c = document.getElementById('c'), ctx = c.getContext('2d');
  const W = c.width, H = c.height;
  const scoreEl = document.getElementById('score'), ballsEl = document.getElementById('balls'), bestEl = document.getElementById('best');

  /* ---------- procedural SFX (respects the overlay's speaker preference) ---------- */
  const Sfx = (() => {
    let actx = null;
    const ac = () => {
      if (!actx) { try { actx = new (window.AudioContext || window.webkitAudioContext)(); } catch (e) {} }
      if (actx && actx.state === 'suspended') { try { actx.resume(); } catch (e) {} }
      return actx;
    };
    const on = () => { try { return arcadeStorage.getItem('aaa_sfx') !== '0'; } catch (e) { return true; } };
    const tone = (f, d, type, vol, slide, delay) => {
      if (!on()) return;
      const c2 = ac(); if (!c2) return;
      try {
        const t = c2.currentTime + (delay || 0);
        const o = c2.createOscillator(), g = c2.createGain();
        o.type = type || 'sine';
        o.frequency.setValueAtTime(Math.max(30, f), t);
        if (slide) o.frequency.exponentialRampToValueAtTime(Math.max(30, slide), t + d);
        g.gain.setValueAtTime(0.0001, t);
        g.gain.linearRampToValueAtTime(vol || 0.05, t + 0.008);
        g.gain.exponentialRampToValueAtTime(0.0006, t + d);
        o.connect(g); g.connect(c2.destination);
        o.start(t); o.stop(t + d + 0.05);
      } catch (e) {}
    };
    const noise = (d, vol, freq, delay) => {
      if (!on()) return;
      const c2 = ac(); if (!c2) return;
      try {
        const t = c2.currentTime + (delay || 0);
        const n = Math.max(32, (c2.sampleRate * d) | 0);
        const buf = c2.createBuffer(1, n, c2.sampleRate);
        const ch = buf.getChannelData(0);
        for (let i = 0; i < n; i++) ch[i] = (Math.random() * 2 - 1) * Math.pow(1 - i / n, 1.7);
        const src = c2.createBufferSource(); src.buffer = buf;
        const f = c2.createBiquadFilter(); f.type = 'lowpass'; f.frequency.value = freq || 800;
        const g = c2.createGain(); g.gain.value = vol || 0.06;
        src.connect(f); f.connect(g); g.connect(c2.destination);
        src.start(t);
      } catch (e) {}
    };
    let lastBump = 0;
    return {
      unlock: ac,
      flip: () => { noise(.05, .07, 1600); tone(240, .04, 'square', .05, 160); },
      bump: pts => {
        const now = performance.now();
        if (now - lastBump < 50) return;
        lastBump = now;
        noise(.04, .08, 1200);
        tone(500 + pts * 1.5, .09, 'triangle', .07, (500 + pts * 1.5) * 1.4);
      },
      sling: () => { tone(700, .07, 'square', .05, 950); },
      rail: () => { noise(.03, .03, 900); },
      launch: p => { tone(180, .3, 'sawtooth', .06, 180 + p * 600); noise(.15, .05, 700); },
      drain: () => { [392, 311, 233].forEach((f, i) => tone(f, .18, 'sawtooth', .045, null, i * .1)); },
      over: () => { [330, 262, 196, 130].forEach((f, i) => tone(f, .26, 'sawtooth', .05, null, i * .14)); },
      newBall: () => { tone(523, .09, 'triangle', .045, 660); tone(784, .1, 'triangle', .04, null, .08); }
    };
  })();

  let best = Number(arcadeStorage.getItem('pinball_best') || 0);
  bestEl.textContent = best;

  let score = 0, ballsLeft = 3, gameOver = false, newBest = false;
  let ball = { x: 374, y: 540, vx: 0, vy: 0, r: 9, inLane: true };
  let leftFlip = false, rightFlip = false, launchCharge = 0, charging = false;
  let lAng = 0.42, rAng = Math.PI - 0.42;
  const sparks = [];
  const litBumpers = new Set();
  let bankAwarded = false;

  const bumpers = [
    { x: 130, y: 170, r: 24, pts: 100, col: '#f43f5e', hit: 0 },
    { x: 240, y: 170, r: 24, pts: 100, col: '#3b82f6', hit: 0 },
    { x: 185, y: 245, r: 26, pts: 150, col: '#eab308', hit: 0 },
    { x: 105, y: 295, r: 18, pts: 75, col: '#10b981', hit: 0 },
    { x: 265, y: 295, r: 18, pts: 75, col: '#a855f7', hit: 0 }
  ];

  function resetBall() {
    litBumpers.clear(); bankAwarded = false; updateBank();
    ball = { x: 374, y: 550, vx: 0, vy: 0, r: 9, inLane: true };
    Sfx.newBall();
  }

  function resetGame() {
    score = 0; ballsLeft = 3; gameOver = false; newBest = false;
    scoreEl.textContent = '0'; ballsEl.textContent = '3';
    resetBall();
  }

  function launch(power) {
    if (gameOver) { resetGame(); return; }
    if (!ball.inLane) return;
    ball.vy = -(760 + power * 420);
    ball.vx = -25 - Math.random()*25;
    Sfx.launch(power);
  }

  function addScore(pts) {
    score += pts;
    scoreEl.textContent = score;
    if (score > best) {
      best = score; bestEl.textContent = best;
      try { arcadeStorage.setItem('pinball_best', String(best)); } catch (e) {}
    }
  }

  function flipperSegment(isLeft) {
    const px = isLeft ? 112 : 248, py = 548, len = 62;
    const ang = isLeft ? lAng : rAng;
    return { x1: px, y1: py, x2: px + Math.cos(ang)*len, y2: py + Math.sin(ang)*len };
  }

  function collideSeg(x1, y1, x2, y2, boost = 0) {
    const dx = x2 - x1, dy = y2 - y1;
    const len2 = dx*dx + dy*dy;
    const t = Math.max(0, Math.min(1, ((ball.x - x1)*dx + (ball.y - y1)*dy) / len2));
    const cx = x1 + t*dx, cy = y1 + t*dy;
    const dist = Math.hypot(ball.x - cx, ball.y - cy);
    if (dist < ball.r + 6 && dist > 0.001) {
      const nx = (ball.x - cx)/dist, ny = (ball.y - cy)/dist;
      ball.x = cx + nx * (ball.r + 6.5);
      ball.y = cy + ny * (ball.r + 6.5);
      const dot = ball.vx * nx + ball.vy * ny;
      if (dot < 0) {
        ball.vx -= 1.82 * dot * nx;
        ball.vy -= 1.82 * dot * ny;
      }
      if (boost > 0) {
        ball.vy -= boost;
        ball.vx += (Math.random() - 0.5) * 80;
        Sfx.sling();
        for (let i = 0; i < 6; i++) sparks.push({ x: cx, y: cy, vx: (Math.random()-.5)*260, vy: (Math.random()-.5)*260, life: .3 });
      }
    }
  }

  let last = performance.now();
  function loop(now) {
    const dt = Math.min(0.025, (now - last) / 1000);
    last = now;
    update(dt);
    draw();
    requestAnimationFrame(loop);
  }

  function update(dt) {
    const lTarget = leftFlip ? -0.42 : 0.42;
    const rTarget = rightFlip ? Math.PI + 0.42 : Math.PI - 0.42;
    const lVel = (lTarget - lAng) * 20;
    const rVel = (rTarget - rAng) * 20;
    lAng += lVel * dt;
    rAng += rVel * dt;

    if (charging && ball.inLane) launchCharge = Math.min(1, launchCharge + dt * 1.4);

    for (let i = sparks.length - 1; i >= 0; i--) {
      const s = sparks[i];
      s.x += s.vx * dt; s.y += s.vy * dt; s.life -= dt;
      if (s.life <= 0) sparks.splice(i, 1);
    }

    if (gameOver) return;

    // Sub-step physics for reliability
    const steps = 3, sdt = dt / steps;
    for (let s = 0; s < steps; s++) {
      ball.vy += 640 * sdt;
      ball.x += ball.vx * sdt;
      ball.y += ball.vy * sdt;

      if (ball.x < 355 && ball.y < 140) ball.inLane = false;

      // Outer walls
      if (ball.x < 16 + ball.r) { ball.x = 16 + ball.r; ball.vx = Math.abs(ball.vx)*0.85; Sfx.rail(); }
      if (ball.x > W - 16 - ball.r) { ball.x = W - 16 - ball.r; ball.vx = -Math.abs(ball.vx)*0.85; Sfx.rail(); }
      if (ball.y < 42 + ball.r) { ball.y = 42 + ball.r; ball.vy = Math.abs(ball.vy)*0.85; }

      // Curved top arch redirect from launch lane
      if (ball.y < 110 && ball.x > 330 && ball.vy < 0) {
        ball.vx -= 420 * sdt;
      }

      // Shooter lane divider
      if (ball.y > 140) {
        if (ball.inLane) {
          if (ball.x < 358 + ball.r) { ball.x = 358 + ball.r; ball.vx = Math.abs(ball.vx)*0.5; }
          if (ball.y > 555) { ball.y = 555; ball.vy = 0; ball.vx = 0; }
        } else {
          if (ball.x > 352 - ball.r) { ball.x = 352 - ball.r; ball.vx = -Math.abs(ball.vx)*0.85; }
        }
      }

      // Guide rails to flippers
      collideSeg(16, 480, 112, 548);
      collideSeg(352, 480, 248, 548);
      // Slingshots
      collideSeg(50, 390, 95, 455, 220);
      collideSeg(310, 390, 265, 455, 220);

      // Flippers
      const lf = flipperSegment(true);
      const rf = flipperSegment(false);
      collideSeg(lf.x1, lf.y1, lf.x2, lf.y2, leftFlip ? 460 : 0);
      collideSeg(rf.x1, rf.y1, rf.x2, rf.y2, rightFlip ? 460 : 0);

      // Bumpers
      for (const b of bumpers) {
        const dx = ball.x - b.x, dy = ball.y - b.y;
        const dist = Math.hypot(dx, dy);
        if (dist < ball.r + b.r && dist > 0.001) {
          const nx = dx / dist, ny = dy / dist;
          ball.x = b.x + nx * (ball.r + b.r + 1);
          ball.y = b.y + ny * (ball.r + b.r + 1);
          ball.vx = nx * 420;
          ball.vy = ny * 420;
          b.hit = 0.18;
          const firstHit = !litBumpers.has(b);
          litBumpers.add(b);
          if (firstHit) updateBank();
          addScore(b.pts);
          if (!bankAwarded && litBumpers.size === bumpers.length) { bankAwarded = true; addScore(750); updateBank(); }
          Sfx.bump(b.pts);
          for (let i = 0; i < 8; i++) sparks.push({ x: b.x + nx * b.r, y: b.y + ny * b.r, vx: nx * 120 + (Math.random()-.5)*200, vy: ny * 120 + (Math.random()-.5)*200, life: .32 });
        }
      }
    }

    for (const b of bumpers) if (b.hit > 0) b.hit -= dt;

    // Drain
    if (ball.y > H + 20) {
      ballsLeft--;
      ballsEl.textContent = Math.max(0, ballsLeft);
      Sfx.drain();
      if (ballsLeft <= 0) {
        gameOver = true;
        if (score >= best && score > 0) newBest = true;
        setTimeout(() => Sfx.over(), 350);
        if (window.AAA) { AAA.flash(newBest ? 0.3 : 0.2); }
      } else {
        resetBall();
      }
    }
  }

  function draw() {
    const bg = ctx.createLinearGradient(0, 0, 0, H);
    bg.addColorStop(0, '#171338'); bg.addColorStop(.6, '#100d24'); bg.addColorStop(1, '#0a0819');
    ctx.fillStyle = bg;
    ctx.fillRect(0, 0, W, H);

    // faint starfield
    ctx.fillStyle = 'rgba(255,255,255,.06)';
    for (let i = 0; i < 40; i++) {
      const sx = (i * 97) % W, sy = (i * 211) % H;
      ctx.fillRect(sx, sy, 1.5, 1.5);
    }

    // Outer wall glow
    ctx.strokeStyle = 'rgba(139,92,246,.35)'; ctx.lineWidth = 3;
    ctx.strokeRect(16, 42, W - 32, H - 42);

    // Shooter divider
    ctx.strokeStyle = '#6366f1'; ctx.lineWidth = 4;
    ctx.shadowColor = '#6366f1'; ctx.shadowBlur = 8;
    ctx.beginPath(); ctx.moveTo(355, 140); ctx.lineTo(355, H); ctx.stroke();
    ctx.shadowBlur = 0;

    // Guide rails
    ctx.strokeStyle = '#8b5cf6'; ctx.lineWidth = 6; ctx.lineCap = 'round';
    ctx.beginPath(); ctx.moveTo(16, 480); ctx.lineTo(112, 548); ctx.stroke();
    ctx.beginPath(); ctx.moveTo(352, 480); ctx.lineTo(248, 548); ctx.stroke();

    // Slingshots
    ctx.strokeStyle = '#22d3ee'; ctx.lineWidth = 5;
    ctx.shadowColor = '#22d3ee'; ctx.shadowBlur = 7;
    ctx.beginPath(); ctx.moveTo(50, 390); ctx.lineTo(95, 455); ctx.stroke();
    ctx.beginPath(); ctx.moveTo(310, 390); ctx.lineTo(265, 455); ctx.stroke();
    ctx.shadowBlur = 0;

    // Bumpers
    for (const b of bumpers) {
      ctx.save();
      ctx.shadowColor = b.col; ctx.shadowBlur = b.hit > 0 ? 26 : 12;
      ctx.fillStyle = b.hit > 0 ? '#ffffff' : b.col;
      ctx.beginPath(); ctx.arc(b.x, b.y, b.r + (b.hit > 0 ? 4 : 0), 0, Math.PI*2); ctx.fill();
      ctx.restore();
      ctx.fillStyle = 'rgba(255,255,255,.35)';
      ctx.beginPath(); ctx.arc(b.x - b.r*.3, b.y - b.r*.35, b.r*.38, 0, Math.PI*2); ctx.fill();
      ctx.fillStyle = '#0f172a'; ctx.font = 'bold 11px sans-serif'; ctx.textAlign = 'center'; ctx.textBaseline = 'middle';
      ctx.fillText(b.pts, b.x, b.y);
    }

    // Sparks
    for (const s of sparks) {
      ctx.globalAlpha = Math.max(0, s.life / .32);
      ctx.fillStyle = '#fef08a';
      ctx.fillRect(s.x - 1.5, s.y - 1.5, 3, 3);
    }
    ctx.globalAlpha = 1;

    // Flippers
    const lf = flipperSegment(true), rf = flipperSegment(false);
    ctx.strokeStyle = '#f472b6'; ctx.lineWidth = 12; ctx.lineCap = 'round';
    ctx.shadowColor = '#f472b6'; ctx.shadowBlur = 12;
    ctx.beginPath(); ctx.moveTo(lf.x1, lf.y1); ctx.lineTo(lf.x2, lf.y2); ctx.stroke();
    ctx.beginPath(); ctx.moveTo(rf.x1, rf.y1); ctx.lineTo(rf.x2, rf.y2); ctx.stroke();
    ctx.shadowBlur = 0;

    // Plunger meter
    if (ball.inLane) {
      ctx.fillStyle = '#facc15';
      ctx.shadowColor = '#facc15'; ctx.shadowBlur = charging ? 14 : 0;
      ctx.fillRect(366, 562, 18, 45 * (1 - launchCharge * 0.6));
      ctx.shadowBlur = 0;
    }

    // Ball — metallic with rim highlight
    const bg2 = ctx.createRadialGradient(ball.x - 3, ball.y - 3, 1, ball.x, ball.y, ball.r + 1);
    bg2.addColorStop(0, '#ffffff');
    bg2.addColorStop(.55, '#cbd5e1');
    bg2.addColorStop(1, '#64748b');
    ctx.fillStyle = bg2;
    ctx.shadowColor = '#e2e8f0'; ctx.shadowBlur = 10;
    ctx.beginPath(); ctx.arc(ball.x, ball.y, ball.r, 0, Math.PI*2); ctx.fill();
    ctx.shadowBlur = 0;

    if (gameOver) {
      ctx.fillStyle = 'rgba(9,8,20,.85)';
      ctx.fillRect(0, 0, W, H);
      ctx.fillStyle = '#f472b6'; ctx.font = '900 30px sans-serif'; ctx.textAlign = 'center';
      ctx.shadowColor = '#f472b6'; ctx.shadowBlur = 20;
      ctx.fillText('O‘YIN TUGADI', W/2, H/2 - 40);
      ctx.shadowBlur = 0;
      ctx.fillStyle = '#fff'; ctx.font = '700 22px sans-serif';
      ctx.fillText('Hisob: ' + score, W/2, H/2);
      if (newBest) {
        ctx.fillStyle = '#facc15'; ctx.font = '900 16px sans-serif';
        ctx.fillText('YANGI REKORD!', W/2, H/2 + 30);
      } else {
        ctx.fillStyle = '#94a3b8'; ctx.font = '600 14px sans-serif';
        ctx.fillText('Rekord: ' + best, W/2, H/2 + 30);
      }
      ctx.fillStyle = 'rgba(255,255,255,' + (0.55 + 0.45 * Math.sin(performance.now() / 300)) + ')';
      ctx.font = '700 15px sans-serif';
      ctx.fillText('Tap LAUNCH to play again', W/2, H/2 + 72);
    }
  }

  window.addEventListener('keydown', e => {
    if (e.repeat) { if (e.code === 'Space' || e.code === 'ArrowDown') e.preventDefault(); return; }
    if (e.code === 'KeyZ' || e.code === 'ArrowLeft') { if (!leftFlip) Sfx.flip(); leftFlip = true; e.preventDefault(); }
    if (e.code === 'KeyM' || e.code === 'Slash' || e.code === 'ArrowRight') { if (!rightFlip) Sfx.flip(); rightFlip = true; e.preventDefault(); }
    if (e.code === 'Space' || e.code === 'ArrowDown') { charging = true; e.preventDefault(); }
  });
  window.addEventListener('keyup', e => {
    if (e.code === 'KeyZ' || e.code === 'ArrowLeft') leftFlip = false;
    if (e.code === 'KeyM' || e.code === 'Slash' || e.code === 'ArrowRight') rightFlip = false;
    if (e.code === 'Space' || e.code === 'ArrowDown') {
      charging = false;
      launch(Math.max(0.4, launchCharge));
      launchCharge = 0;
    }
  });

  // touch buttons
  const lB = document.getElementById('lBtn'), rB = document.getElementById('rBtn'), pB = document.getElementById('pBtn');
  const bindHold = (el, set) => {
    el.addEventListener('pointerdown', e => { e.preventDefault(); Sfx.unlock(); set(true); });
    ['pointerup', 'pointercancel', 'pointerleave'].forEach(ev => el.addEventListener(ev, () => set(false)));
  };
  bindHold(lB, v => { if (v && !leftFlip) Sfx.flip(); leftFlip = v; });
  bindHold(rB, v => { if (v && !rightFlip) Sfx.flip(); rightFlip = v; });
  pB.addEventListener('pointerdown', e => { e.preventDefault(); Sfx.unlock(); charging = true; });
  ['pointerup', 'pointercancel', 'pointerleave'].forEach(ev => pB.addEventListener(ev, () => {
    if (charging) { charging = false; launch(Math.max(0.5, launchCharge)); launchCharge = 0; }
  }));

  // tap on canvas: left half = left flipper, right half = right flipper
  const touchFlip = new Map();
  c.addEventListener('pointerdown', e => {
    Sfx.unlock();
    const r = c.getBoundingClientRect();
    const isLeft = (e.clientX - r.left) < r.width / 2;
    touchFlip.set(e.pointerId, isLeft);
    if (isLeft && !leftFlip) Sfx.flip();
    if (!isLeft && !rightFlip) Sfx.flip();
    if (isLeft) leftFlip = true; else rightFlip = true;
  });
  const releaseFlip = e => {
    const isLeft = touchFlip.get(e.pointerId);
    if (isLeft === undefined) return;
    touchFlip.delete(e.pointerId);
    if (isLeft) leftFlip = false; else rightFlip = false;
  };
  ['pointerup', 'pointercancel', 'pointerleave'].forEach(ev => c.addEventListener(ev, releaseFlip));

  requestAnimationFrame(loop);


  const bankReadout = document.createElement('div'); bankReadout.id = 'bumperBank';
  function updateBank() { bankReadout.textContent = bankAwarded ? 'Bumper bank cleared: +750' : 'Light all bumpers: ' + litBumpers.size + '/' + bumpers.length; }
  scoreEl.parentElement.after(bankReadout); updateBank();

  // Report native gameplay state to the optional mission engine.
  window.__arcadeMissionSource = Object.assign({"id":72,"label":"Pinball points","goals":[300,900,1600],"note":"Keep the ball alive and build points from bumpers and targets."}, { read: () => { return (score); } });
})();
