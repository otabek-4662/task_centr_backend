// Arcade 100 · MIT · see LICENSE
(function () {
  'use strict';

  var pads = Array.prototype.slice.call(document.querySelectorAll('.pad'));
  var statusEl = document.getElementById('status');
  var roundEl = document.getElementById('round');
  var roundNum = document.getElementById('round-num');
  var bestEl = document.getElementById('bestV');
  var startBtn = document.getElementById('startBtn');

const arcadePlus = (new URLSearchParams(location.search).get('mode') === 'plus');
  var seq = [], userIdx = 0, playing = false, showing = false;
  var best = 0;
  try { best = parseInt(arcadeStorage.getItem(('simon_best' + (new URLSearchParams(location.search).get('mode')==='plus'?'_plus':''))) || '0', 10) || 0; } catch (e) {}
  bestEl.textContent = best;

  // Classic Simon frequencies
  var FREQ = [392.0, 329.6, 261.6, 196.0];

  var ACX = null;
  function ac() {
    try {
      if (!ACX) ACX = new (window.AudioContext || window.webkitAudioContext)();
      if (ACX.state === 'suspended') ACX.resume();
    } catch (e) {}
    return ACX;
  }

  function padTone(i, dur) {
    var c = ac(); if (!c) return;
    var t = c.currentTime;
    try {
      var o = c.createOscillator(), o2 = c.createOscillator(), g = c.createGain();
      o.type = 'triangle'; o.frequency.value = FREQ[i];
      o2.type = 'sine'; o2.frequency.value = FREQ[i] * 2;
      g.gain.setValueAtTime(0.0001, t);
      g.gain.linearRampToValueAtTime(0.09, t + 0.015);
      g.gain.setValueAtTime(0.09, t + dur * 0.7);
      g.gain.exponentialRampToValueAtTime(0.0001, t + dur);
      var g2 = c.createGain(); g2.gain.value = 0.3;
      o.connect(g); o2.connect(g2); g2.connect(g); g.connect(c.destination);
      o.start(t); o.stop(t + dur + 0.05);
      o2.start(t); o2.stop(t + dur + 0.05);
    } catch (e) {}
  }

  function buzz() {
    var c = ac(); if (!c) return;
    var t = c.currentTime;
    try {
      var o = c.createOscillator(), g = c.createGain();
      o.type = 'sawtooth'; o.frequency.setValueAtTime(140, t);
      o.frequency.linearRampToValueAtTime(90, t + 0.6);
      g.gain.setValueAtTime(0.0001, t);
      g.gain.linearRampToValueAtTime(0.11, t + 0.02);
      g.gain.exponentialRampToValueAtTime(0.0001, t + 0.7);
      o.connect(g); g.connect(c.destination);
      o.start(t); o.stop(t + 0.75);
    } catch (e) {}
  }

  function tone(f, d, type, v, delay) {
    var c = ac(); if (!c) return;
    var t = c.currentTime + (delay || 0);
    try {
      var o = c.createOscillator(), g = c.createGain();
      o.type = type || 'sine'; o.frequency.setValueAtTime(f, t);
      g.gain.setValueAtTime(0.0001, t);
      g.gain.linearRampToValueAtTime(v || 0.05, t + 0.012);
      g.gain.exponentialRampToValueAtTime(0.0001, t + d);
      o.connect(g); g.connect(c.destination);
      o.start(t); o.stop(t + d + 0.03);
    } catch (e) {}
  }

  function lightPad(i, dur) {
    pads[i].classList.add('lit');
    padTone(i, dur);
    setTimeout(function () { pads[i].classList.remove('lit'); }, dur * 1000 * 0.85);
  }

  function playbackSpeed() {
    // ramp: faster playback each few rounds
    return Math.max(0.28, 0.62 - seq.length * 0.022);
  }

  function showSequence() {
    showing = true;
    userIdx = 0;
    statusEl.className = '';
    statusEl.textContent = 'Kuzating…';
    var dur = playbackSpeed();
    seq.forEach(function (p, i) {
      setTimeout(function () { lightPad(p, dur * 0.8); }, i * dur * 1000 + 500);
    });
    setTimeout(function () {
      showing = false;
      statusEl.textContent = arcadePlus && seq.length >= 3 ? 'Repeat backwards!' : 'Navbat sizda!';
    }, seq.length * dur * 1000 + 600);
  }

  function nextRound() {
    seq.push(Math.floor(Math.random() * 4));
    var r = seq.length;
    roundEl.textContent = r;
    roundNum.textContent = r;
    if (r > best) { best = r; bestEl.textContent = best; try { arcadeStorage.setItem(('simon_best' + (new URLSearchParams(location.search).get('mode')==='plus'?'_plus':'')), String(best)); } catch (e) {} }
    showSequence();
  }

  function gameOver() {
    playing = false;
    showing = false;
    startBtn.classList.remove('playing');
    startBtn.textContent = 'TAKRORLASH';
    buzz();
    statusEl.className = 'err';
    statusEl.textContent = 'Xato. Bosqich: ' + seq.length + ' · Rekord: ' + best;
    // flash all pads
    pads.forEach(function (p) { p.classList.add('lit'); });
    setTimeout(function () { pads.forEach(function (p) { p.classList.remove('lit'); }); }, 500);
  }

  function start() {
    ac();
    seq = [];
    playing = true;
    startBtn.classList.add('playing');
    startBtn.textContent = 'O‘YIN DAVOM ETMOQDA';
    [523, 659, 784].forEach(function (f, i) { tone(f, 0.12, 'triangle', 0.06, i * 0.08); });
    nextRound();
  }

  pads.forEach(function (pad) {
    pad.addEventListener('pointerdown', function () {
      if (!playing || showing) return;
      var i = +pad.dataset.p;
      padTone(i, 0.3);
      pad.classList.add('lit');
      setTimeout(function () { pad.classList.remove('lit'); }, 240);
      if (i === seq[arcadePlus && seq.length >= 3 ? seq.length - 1 - userIdx : userIdx]) {
        userIdx++;
        if (userIdx === seq.length) {
          statusEl.className = 'good';
          statusEl.textContent = 'To‘g‘ri!';
          tone(880, 0.1, 'sine', 0.05, 0.1);
          showing = true;
          setTimeout(nextRound, 900);
        }
      } else {
        gameOver();
      }
    });
  });

  startBtn.addEventListener('click', function () { if (!playing) start(); });
  document.addEventListener('keydown', function (e) {
    if ((e.code === 'Space' || e.code === 'Enter') && !playing) { start(); e.preventDefault(); }
    var kmap = { Digit1: 0, Digit2: 1, Digit3: 2, Digit4: 3, KeyQ: 0, KeyW: 1, KeyA: 2, KeyS: 3 };
    if (playing && !showing && e.code in kmap) {
      pads[kmap[e.code]].dispatchEvent(new PointerEvent('pointerdown'));
    }
  });

  window.__arcadeNativeUpgrade = {"id": 11, "name": "Reverse Echo", "description": "after the first two rounds, repeat the sequence backwards rather than forward."};
  // Report native gameplay state to the optional mission engine.
  window.__arcadeMissionSource = Object.assign({"id":11,"label":"Sequences copied","goals":[3,6,9],"note":"Repeat whole sequences. Merely reaching a round does not count."}, { read: () => { return (Math.max(0, seq.length - (statusEl.className === 'good' ? 0 : 1))); } });
})();
