// Arcade 100 · MIT · see LICENSE
(() => {
  const c = document.getElementById('c'), ctx = c.getContext('2d');
  const W = c.width, H = c.height, CX = W/2, CY = H/2;
  const RINGS = [95, 145, 195];
  const scoreEl = document.getElementById('score'), bestEl = document.getElementById('best'), comboEl = document.getElementById('combo');
  const ov = document.getElementById('ov'), ovT = document.getElementById('ovT'), ovP = document.getElementById('ovP'), ovStats = document.getElementById('ovStats'), startBtn = document.getElementById('startBtn');

  if (!CanvasRenderingContext2D.prototype.roundRect) {
    CanvasRenderingContext2D.prototype.roundRect = function(x,y,w,h,r){this.moveTo(x+r,y);this.arcTo(x+w,y,x+w,y+h,r);this.arcTo(x+w,y+h,x,y+h,r);this.arcTo(x,y+h,x,y,r);this.arcTo(x,y,x+w,y,r);return this;};
  }

  // ---- tiny sfx helper (respects the shared aaa_sfx pref) ----
  let ac = null;
  const audio = () => { const A = window.AudioContext || window.webkitAudioContext; if (!ac && A) ac = new A(); if (ac && ac.state === 'suspended') ac.resume().catch(()=>{}); return ac; };
  const sfxOn = () => arcadeStorage.getItem('aaa_sfx') !== '0';
  function tone(f, d, type='sine', vol=0.06, slide=null){
    if (!sfxOn()) return; const x = audio(); if (!x) return;
    try {
      const t = x.currentTime, o = x.createOscillator(), g = x.createGain();
      o.type = type; o.frequency.setValueAtTime(f, t);
      if (slide) o.frequency.exponentialRampToValueAtTime(Math.max(30, slide), t + d);
      g.gain.setValueAtTime(0.001, t); g.gain.linearRampToValueAtTime(vol, t + 0.012);
      g.gain.exponentialRampToValueAtTime(0.0006, t + d);
      o.connect(g); g.connect(x.destination); o.start(t); o.stop(t + d + 0.03);
    } catch {}
  }
  function whoosh(dir){ tone(dir > 0 ? 300 : 380, .16, 'sawtooth', .035, dir > 0 ? 720 : 150); }
  function boom(){
    const x = audio(); if (!x || !sfxOn()) return;
    try {
      const n = x.createBufferSource(), b = x.createBuffer(1, x.sampleRate * .45, x.sampleRate), d = b.getChannelData(0);
      for (let i = 0; i < d.length; i++) d[i] = (Math.random()*2-1) * Math.pow(1 - i/d.length, 2.2);
      n.buffer = b;
      const f = x.createBiquadFilter(); f.type = 'lowpass'; f.frequency.value = 900;
      const g = x.createGain(); g.gain.value = .3;
      n.connect(f); f.connect(g); g.connect(x.destination); n.start();
      tone(120, .5, 'sawtooth', .14, 40);
    } catch {}
  }

  let best = Number(arcadeStorage.getItem('orbit_best') || 0);
  bestEl.textContent = best;

  let state = 'idle', ringIdx = 1, curR = RINGS[1], theta = -Math.PI/2, score = 0;
  let meteors = [], stars = [], sparks = [], trail = [], spawnT = 0;
  let combo = 0, comboT = 0, shake = 0, milestone = 0, planetRot = 0;
  let shieldReady = true, shieldTime = 0;
  const bgStars = Array.from({length: 90}, () => ({ x: Math.random()*W, y: Math.random()*H, r: .4 + Math.random()*1.6, tw: Math.random()*6 }));

  function reset() {
    ringIdx = 1; curR = RINGS[1]; theta = -Math.PI/2; score = 0; shieldReady = true; shieldTime = 0; updateShield();
    meteors = []; stars = []; sparks = []; trail = [];
    spawnT = .8; combo = 0; comboT = 0; shake = 0; milestone = 0;
    for (let i = 0; i < 3; i++) spawnStar(i);
    scoreEl.textContent = '0'; comboEl.textContent = 'x1';
    state = 'play';
    ov.classList.add('hidden');
    audio();
  }

  function spawnStar(i) {
    stars.push({ r: RINGS[(i ?? Math.floor(Math.random()*3))], ang: Math.random()*Math.PI*2, ph: Math.random()*6 });
  }

  function spawnMeteor() {
    const a = Math.random()*Math.PI*2;
    const spd = 115 + Math.min(175, score * 1.05);
    const tx = CX + Math.cos(a + (Math.random()-.5)*.7)*140;
    const ty = CY + Math.sin(a + (Math.random()-.5)*.7)*140;
    const sx = CX + Math.cos(a)*330, sy = CY + Math.sin(a)*330;
    const ang = Math.atan2(ty - sy, tx - sx);
    const verts = []; const n = 8;
    for (let i = 0; i < n; i++) verts.push(.7 + Math.random()*.55);
    meteors.push({ x: sx, y: sy, vx: Math.cos(ang)*spd, vy: Math.sin(ang)*spd, r: 10 + Math.random()*6, rot: Math.random()*6, vr: (Math.random()-.5)*4, verts, near: false });
  }

  function addScore(v) {
    score += v;
    scoreEl.textContent = Math.floor(score);
  }

  let last = performance.now();
  function loop(now) {
    const dt = Math.min(.04, (now - last) / 1000);
    last = now;
    if (state === 'play') update(dt);
    draw(now / 1000);
    requestAnimationFrame(loop);
  }

  function update(dt) {
    planetRot += dt * .12;
    if (shieldTime > 0) { shieldTime = Math.max(0, shieldTime - dt); if (!shieldTime) updateShield(); }
    const spdGain = 1 + Math.min(.8, score * .004);
    theta += (2.0 * spdGain) * dt;
    curR += (RINGS[ringIdx] - curR) * Math.min(1, dt * 13);
    const sx = CX + Math.cos(theta)*curR, sy = CY + Math.sin(theta)*curR;

    trail.push({ x: sx, y: sy, t: .35 });
    if (trail.length > 22) trail.shift();
    for (const p of trail) p.t -= dt;

    addScore(dt * 8);
    const disp = Math.floor(score);
    if (disp > best) { best = disp; bestEl.textContent = best; arcadeStorage.setItem('orbit_best', String(best)); }

    if (comboT > 0) { comboT -= dt; if (comboT <= 0) { combo = 0; comboEl.textContent = 'x1'; } }

    const lvl = Math.floor(disp / 150);
    if (lvl > milestone) { milestone = lvl; tone(440, .3, 'square', .05, 880); }

    spawnT -= dt;
    if (spawnT <= 0) {
      spawnMeteor();
      spawnT = Math.max(.3, 1.05 - score * .0028);
    }

    // star pickups
    for (let i = stars.length - 1; i >= 0; i--) {
      const st = stars[i];
      const px = CX + Math.cos(st.ang)*st.r, py = CY + Math.sin(st.ang)*st.r;
      if (Math.hypot(sx - px, sy - py) < 24) {
        combo = Math.min(8, combo + 1); comboT = 3.2;
        comboEl.textContent = 'x' + Math.max(1, combo);
        if (combo % 3 === 0) { shieldReady = true; updateShield(); }
        addScore(15 * Math.max(1, combo));
        for (let k = 0; k < 10; k++) sparks.push({ x: px, y: py, vx: (Math.random()-.5)*180, vy: (Math.random()-.5)*180, t: .4, col: '#fde047' });
        stars.splice(i, 1);
        spawnStar();
      }
    }

    // meteors
    for (let i = meteors.length - 1; i >= 0; i--) {
      const m = meteors[i];
      m.x += m.vx * dt; m.y += m.vy * dt; m.rot += m.vr * dt;
      const dShip = Math.hypot(sx - m.x, sy - m.y);
      if (dShip < m.r + 9) {
        if (shieldTime > 0) { shieldTime = 0; meteors.splice(i, 1); updateShield(); tone(360, .15, 'triangle', .07, 660); continue; }
        die(sx, sy);
        return;
      }
      if (!m.near && dShip < m.r + 30) { m.near = true; addScore(6); tone(700, .1, 'triangle', .04, 990); }
      const dC = Math.hypot(m.x - CX, m.y - CY);
      if (dC > 380 || dC < 40) meteors.splice(i, 1);
    }

    for (let i = sparks.length - 1; i >= 0; i--) {
      const s = sparks[i];
      s.x += s.vx * dt; s.y += s.vy * dt; s.t -= dt;
      if (s.t <= 0) sparks.splice(i, 1);
    }
    if (shake > 0) shake = Math.max(0, shake - dt * 26);
  }

  function die(x, y) {
    state = 'over';
    shake = 14;
    boom();
    for (let k = 0; k < 42; k++) {
      const a = Math.random()*Math.PI*2, s = 60 + Math.random()*260;
      sparks.push({ x, y, vx: Math.cos(a)*s, vy: Math.sin(a)*s, t: .5 + Math.random()*.5, col: k % 3 ? '#fb7185' : '#fde047' });
    }
    const fs = Math.floor(score);
    if (fs > best) { best = fs; bestEl.textContent = best; arcadeStorage.setItem('orbit_best', String(best)); }
    setTimeout(() => {
      ovT.innerHTML = 'O‘YIN TUGADI'; ovT.classList.add('hot');
      ovP.innerHTML = 'Kema meteorga urildi.';
      ovStats.style.display = 'flex';
      ovStats.innerHTML = `<div>Score <b>${fs}</b></div><div>Best <b>${best}</b></div>`;
      startBtn.textContent = 'QAYTA UCHISH';
      ov.classList.remove('hidden');
    }, 700);
  }

  function draw(t) {
    ctx.save();
    if (shake > 0) ctx.translate((Math.random()-.5)*shake, (Math.random()-.5)*shake);
    ctx.fillStyle = '#05070f';
    ctx.fillRect(-20, -20, W + 40, H + 40);

    // parallax starfield
    for (const b of bgStars) {
      const a = .35 + .55 * Math.abs(Math.sin(t * 1.7 + b.tw));
      ctx.globalAlpha = a;
      ctx.fillStyle = '#dbeafe';
      ctx.beginPath(); ctx.arc(b.x, b.y, b.r, 0, Math.PI*2); ctx.fill();
    }
    ctx.globalAlpha = 1;

    // planet w/ atmosphere + surface
    const at = ctx.createRadialGradient(CX, CY, 30, CX, CY, 78);
    at.addColorStop(0, 'rgba(96,165,250,.35)'); at.addColorStop(1, 'rgba(96,165,250,0)');
    ctx.fillStyle = at; ctx.beginPath(); ctx.arc(CX, CY, 78, 0, Math.PI*2); ctx.fill();
    const pg = ctx.createRadialGradient(CX - 14, CY - 16, 6, CX, CY, 46);
    pg.addColorStop(0, '#7cc4ff'); pg.addColorStop(.55, '#2563eb'); pg.addColorStop(1, '#101d4e');
    ctx.fillStyle = pg; ctx.beginPath(); ctx.arc(CX, CY, 44, 0, Math.PI*2); ctx.fill();
    ctx.save();
    ctx.beginPath(); ctx.arc(CX, CY, 44, 0, Math.PI*2); ctx.clip();
    ctx.strokeStyle = 'rgba(186,230,253,.28)'; ctx.lineWidth = 6;
    for (let i = -2; i <= 2; i++) {
      ctx.beginPath();
      ctx.ellipse(CX + Math.sin(planetRot + i) * 8, CY + i * 15, 46, 9, 0, 0, Math.PI*2);
      ctx.stroke();
    }
    ctx.restore();

    // orbit rings (dashed, glowing)
    for (let i = 0; i < RINGS.length; i++) {
      const active = i === ringIdx && state === 'play';
      ctx.strokeStyle = active ? 'rgba(125,211,252,.55)' : 'rgba(125,160,220,.16)';
      ctx.lineWidth = active ? 2.4 : 1.6;
      ctx.setLineDash([10, 8]);
      ctx.lineDashOffset = -t * 22;
      ctx.beginPath(); ctx.arc(CX, CY, RINGS[i], 0, Math.PI*2); ctx.stroke();
    }
    ctx.setLineDash([]);

    // pickups (spinning stars)
    for (const st of stars) {
      const px = CX + Math.cos(st.ang)*st.r, py = CY + Math.sin(st.ang)*st.r;
      const sc = 1 + Math.sin(t * 4 + st.ph) * .18;
      ctx.save(); ctx.translate(px, py); ctx.rotate(t * 1.5 + st.ph); ctx.scale(sc, sc);
      ctx.fillStyle = '#fde047';
      ctx.shadowColor = '#fde047'; ctx.shadowBlur = 14;
      ctx.beginPath();
      for (let k = 0; k < 4; k++) {
        ctx.rotate(Math.PI/2);
        ctx.moveTo(0, 0); ctx.lineTo(3, -3); ctx.lineTo(0, -9); ctx.lineTo(-3, -3);
      }
      ctx.fill();
      ctx.restore();
    }

    // meteors (craggy + flame trail)
    for (const m of meteors) {
      const sp = Math.hypot(m.vx, m.vy), nx = m.vx / sp, ny = m.vy / sp;
      const tg = ctx.createLinearGradient(m.x, m.y, m.x - nx * 46, m.y - ny * 46);
      tg.addColorStop(0, 'rgba(251,113,133,.55)'); tg.addColorStop(1, 'rgba(251,113,133,0)');
      ctx.strokeStyle = tg; ctx.lineWidth = m.r * 1.15; ctx.lineCap = 'round';
      ctx.beginPath(); ctx.moveTo(m.x, m.y); ctx.lineTo(m.x - nx * 46, m.y - ny * 46); ctx.stroke();
      ctx.save(); ctx.translate(m.x, m.y); ctx.rotate(m.rot);
      ctx.fillStyle = '#7f1d1d';
      ctx.beginPath();
      m.verts.forEach((v, k) => {
        const a = k / m.verts.length * Math.PI * 2;
        const rr = m.r * v;
        k === 0 ? ctx.moveTo(Math.cos(a)*rr, Math.sin(a)*rr) : ctx.lineTo(Math.cos(a)*rr, Math.sin(a)*rr);
      });
      ctx.closePath(); ctx.fill();
      ctx.fillStyle = '#fca5a5';
      ctx.beginPath(); ctx.arc(-m.r*.2, -m.r*.2, m.r*.45, 0, Math.PI*2); ctx.fill();
      ctx.restore();
    }

    // ship trail
    if (state === 'play' || state === 'over') {
      for (let i = 0; i < trail.length; i++) {
        const p = trail[i];
        if (p.t <= 0) continue;
        ctx.globalAlpha = (p.t / .35) * .5 * (i / trail.length);
        ctx.fillStyle = '#7dd3fc';
        ctx.beginPath(); ctx.arc(p.x, p.y, 4.5 * (i / trail.length) + 1, 0, Math.PI*2); ctx.fill();
      }
      ctx.globalAlpha = 1;
    }

    // ship
    if (state === 'play') {
      const sx = CX + Math.cos(theta)*curR, sy = CY + Math.sin(theta)*curR;
      ctx.save(); ctx.translate(sx, sy); ctx.rotate(theta + Math.PI/2);
      // engine flame
      const fl = 6 + Math.sin(t * 30) * 3;
      ctx.fillStyle = 'rgba(251,191,36,.85)';
      ctx.beginPath(); ctx.moveTo(-4, 8); ctx.lineTo(0, 8 + fl + 6); ctx.lineTo(4, 8); ctx.closePath(); ctx.fill();
      ctx.fillStyle = '#7dd3fc';
      ctx.shadowColor = '#38bdf8'; ctx.shadowBlur = 12;
      ctx.beginPath(); ctx.moveTo(13, 0); ctx.lineTo(-11, -9); ctx.lineTo(-6, 0); ctx.lineTo(-11, 9); ctx.closePath(); ctx.fill();
      ctx.shadowBlur = 0;
      ctx.fillStyle = '#e0f2fe';
      ctx.beginPath(); ctx.arc(4, 0, 3.4, 0, Math.PI*2); ctx.fill();
      ctx.restore();
    }

    // sparks
    for (const s of sparks) {
      ctx.globalAlpha = Math.max(0, s.t * 2);
      ctx.fillStyle = s.col;
      ctx.beginPath(); ctx.arc(s.x, s.y, 3, 0, Math.PI*2); ctx.fill();
    }
    ctx.globalAlpha = 1;

    // vignette
    const vg = ctx.createRadialGradient(CX, CY, H*.42, CX, CY, H*.72);
    vg.addColorStop(0, 'rgba(0,0,0,0)'); vg.addColorStop(1, 'rgba(2,4,12,.5)');
    ctx.fillStyle = vg; ctx.fillRect(0, 0, W, H);
    ctx.restore();
  }

  function shift(dir) {
    if (state !== 'play') return;
    const n = Math.max(0, Math.min(2, ringIdx + dir));
    if (n !== ringIdx) { ringIdx = n; whoosh(dir); }
  }
  function cycle() { shift(ringIdx === 2 ? -2 : 1); }

  window.addEventListener('keydown', e => {
    if (e.code === 'Space') { e.preventDefault(); state === 'play' ? cycle() : startBtn.click(); }
    else if (e.code === 'ArrowUp' || e.code === 'KeyW') { e.preventDefault(); shift(1); }
    else if (e.code === 'ArrowDown' || e.code === 'KeyS') { e.preventDefault(); shift(-1); }
    else if (e.code === 'Enter' && state !== 'play') startBtn.click();
  });
  c.addEventListener('pointerdown', e => { e.preventDefault(); cycle(); });
  startBtn.onclick = () => { ovT.innerHTML = 'ORBITA<br>MANEVRI'; ovT.classList.remove('hot'); ovStats.style.display = 'none'; reset(); };
  requestAnimationFrame(loop);


  const shieldButton = document.createElement('button'); shieldButton.type = 'button'; shieldButton.className = 'btn';
  function updateShield() { shieldButton.textContent = shieldTime > 0 ? 'Qalqon faol' : shieldReady ? 'Qalqon · X' : 'Qalqon uchun 3 yulduz yig‘ing'; shieldButton.disabled = !shieldReady || shieldTime > 0; }
  function useShield() { if (state === 'play' && shieldReady && shieldTime <= 0) { shieldTime = 1.7; shieldReady = false; updateShield(); } }
  shieldButton.onclick = useShield; scoreEl.parentElement.after(shieldButton); updateShield();
  window.addEventListener('keydown', e => { if (!e.repeat && e.code === 'KeyX') useShield(); });

  // Report native gameplay state to the optional mission engine.
  window.__arcadeMissionSource = Object.assign({"id":81,"label":"Orbit points","goals":[80,180,350],"note":"Switch rings early enough to dodge obstacles and keep your chain alive."}, { read: () => { return (score); } });
})();
