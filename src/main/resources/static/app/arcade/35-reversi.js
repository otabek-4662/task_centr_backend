// Arcade 100 · MIT · see LICENSE
/*AAA-INJECT-SFX*/
window.__gsfx = (function(){
  var ctx=null;
  function ac(){ if(ctx===null){var A=window.AudioContext||window.webkitAudioContext;ctx=A?new A():false;} if(ctx&&ctx.state==='suspended')ctx.resume().catch(function(){}); return ctx||null; }
  function ok(){ return arcadeStorage.getItem('aaa_sfx')!=='0'; }
  function tone(f,d,type,v,slide,when){ if(!ok())return; var c=ac(); if(!c)return; try{ var t=c.currentTime+(when||0); var o=c.createOscillator(),g=c.createGain(); o.type=type||'sine'; o.frequency.setValueAtTime(f,t); if(slide)o.frequency.exponentialRampToValueAtTime(Math.max(30,slide),t+d); g.gain.setValueAtTime(0.001,t); g.gain.linearRampToValueAtTime(v||0.08,t+0.012); g.gain.exponentialRampToValueAtTime(0.0006,t+d); o.connect(g); g.connect(c.destination); o.start(t); o.stop(t+d+0.03);}catch(e){} }
  function noise(dur,v,fq,when,type){ if(!ok())return; var c=ac(); if(!c)return; try{ var t=c.currentTime+(when||0),n=Math.floor(c.sampleRate*dur); var b=c.createBuffer(1,n,c.sampleRate),d=b.getChannelData(0); for(var i=0;i<n;i++)d[i]=(Math.random()*2-1)*(1-i/n); var s=c.createBufferSource(); s.buffer=b; var f=c.createBiquadFilter(); f.type=type||'lowpass'; f.frequency.value=fq||1200; var g=c.createGain(); g.gain.value=v||0.1; s.connect(f); f.connect(g); g.connect(c.destination); s.start(t);}catch(e){} }
  var api={
    unlock:ac, tone:tone, noise:noise,
    tap:function(){tone(520,0.05,'triangle',0.05);},
    key:function(){tone(680+Math.random()*60,0.035,'triangle',0.04);},
    deal:function(){noise(0.09,0.09,2600,0,'highpass');tone(300,0.06,'sine',0.04);},
    flip:function(){noise(0.07,0.08,3200,0,'highpass');},
    chip:function(){tone(1900,0.04,'square',0.03);tone(2400,0.03,'square',0.025,null,0.03);noise(0.04,0.05,5000,0,'highpass');},
    clack:function(){noise(0.045,0.1,1600);tone(220,0.05,'triangle',0.07,150);},
    step:function(){tone(330,0.04,'triangle',0.045,280);},
    slide:function(){noise(0.08,0.06,900);tone(260,0.07,'sine',0.045,200);},
    pop:function(i){i=i||0;tone(500+i*80,0.07,'sine',0.07,900+i*120);noise(0.03,0.04,3000,0,'highpass');},
    shoot:function(){tone(700,0.09,'sawtooth',0.05,1400);},
    thud:function(){tone(120,0.12,'sine',0.12,60);noise(0.07,0.08,600);},
    sonar:function(){tone(880,0.35,'sine',0.06);tone(880,0.2,'sine',0.03,null,0.18);},
    splash:function(){noise(0.3,0.1,1200);},
    boom:function(){tone(80,0.5,'sine',0.2,35);noise(0.4,0.16,400);},
    coin:function(){tone(1318,0.06,'square',0.05);tone(1760,0.12,'square',0.05,null,0.06);},
    ding:function(){tone(1046,0.15,'triangle',0.07);tone(1568,0.18,'triangle',0.06,null,0.08);},
    good:function(){tone(659,0.08,'triangle',0.06);tone(880,0.1,'triangle',0.06,null,0.06);},
    bad:function(){tone(180,0.12,'square',0.05,120);},
    win:function(){[523,659,784,1047,1319].forEach(function(f,i){tone(f,0.2,'triangle',0.08,null,i*0.09);});},
    lose:function(){[392,330,262,196].forEach(function(f,i){tone(f,0.26,'sawtooth',0.05,f*0.92,i*0.13);});},
    chord:function(){tone(523,0.14,'sine',0.05);tone(659,0.14,'sine',0.045,null,0.01);tone(784,0.16,'sine',0.04,null,0.02);}
  };
  function unl(){ac();}
  window.addEventListener('pointerdown',unl,{passive:true});
  window.addEventListener('keydown',unl);
  document.addEventListener('click',function(e){var b=e.target&&e.target.closest?e.target.closest('button'):null; if(b&&!b.disabled)api.tap();},true);
  return api;
})();

window.__arcadeNativeUpgrade = {"id": 35, "name": "Corner Siege", "description": "The CPU looks a move ahead to restrict mobility and deny dangerous corners."}; const arcadeVariant = new URLSearchParams(location.search).get("mode") === "plus" || new URLSearchParams(location.search).get("plus") === "1";

(function () {
  "use strict";
  var N = 8, EMPTY = 0, BLACK = 1, WHITE = 2;
  var DIRS = [[-1,-1],[-1,0],[-1,1],[0,-1],[0,1],[1,-1],[1,0],[1,1]];
  var WEIGHTS = [
    100,-20, 10,  5,  5, 10,-20,100,
    -20,-50, -2, -2, -2, -2,-50,-20,
     10, -2,  5,  1,  1,  5, -2, 10,
      5, -2,  1,  0,  0,  1, -2,  5,
      5, -2,  1,  0,  0,  1, -2,  5,
     10, -2,  5,  1,  1,  5, -2, 10,
    -20,-50, -2, -2, -2, -2,-50,-20,
    100,-20, 10,  5,  5, 10,-20,100
  ];

  var boardEl = document.getElementById("board");
  var scoreBEl = document.getElementById("scoreB");
  var scoreWEl = document.getElementById("scoreW");
  var turnEl = document.getElementById("turn");
  var msgEl = document.getElementById("msg");
  var cells = [];
  var board, gameOver, lastMove, thinking;

  function sfx(n, a) { try { if (window.__gsfx && __gsfx[n]) __gsfx[n](a); } catch (e) {} }
  var record = { w: 0, l: 0, d: 0 };
  try { var rc = JSON.parse(arcadeStorage.getItem('reversiRecord') || 'null'); if (rc) record = rc; } catch (e) {}
  function drawRecord() {
    document.getElementById('record').textContent =
      record.w + record.l + record.d > 0
        ? 'Umumiy natija — Siz ' + record.w + ' · AI ' + record.l + ' · Durang ' + record.d
        : '';
  }
  drawRecord();

  for (var i = 0; i < N * N; i++) {
    var c = document.createElement("button");
    c.type = "button";
    c.className = "cell";
    c.dataset.idx = i;
    c.tabIndex = i === 0 ? 0 : -1;
    c.setAttribute("aria-label", (Math.floor(i / N) + 1) + "-qator, " + (i % N + 1) + "-katak");
    boardEl.appendChild(c);
    cells.push(c);
  }
  boardEl.addEventListener("click", onCellClick);
  boardEl.addEventListener("keydown", function (event) {
    if (!event.target.matches(".cell")) return;
    var change = { ArrowLeft: -1, ArrowRight: 1, ArrowUp: -N, ArrowDown: N }[event.key];
    var next = cells[Number(event.target.dataset.idx) + change];
    if (!next) return;
    event.preventDefault();
    event.target.tabIndex = -1;
    next.tabIndex = 0;
    next.focus();
  });
  document.getElementById("restart").addEventListener("click", reset);

  function reset() {
    board = new Array(N * N).fill(EMPTY);
    board[27] = WHITE; board[28] = BLACK;
    board[35] = BLACK; board[36] = WHITE;
    gameOver = false;
    thinking = false;
    lastMove = -1;
    msgEl.textContent = "";
    render();
    if (legalMoves(BLACK).length === 0) handleNoMoves(BLACK);
  }

  function flipsFor(b, idx, color) {
    if (b[idx] !== EMPTY) return null;
    var r = (idx / N) | 0, c = idx % N, opp = 3 - color, all = [];
    for (var d = 0; d < DIRS.length; d++) {
      var dr = DIRS[d][0], dc = DIRS[d][1], line = [], rr = r + dr, cc = c + dc;
      while (rr >= 0 && rr < N && cc >= 0 && cc < N && b[rr * N + cc] === opp) {
        line.push(rr * N + cc); rr += dr; cc += dc;
      }
      if (line.length && rr >= 0 && rr < N && cc >= 0 && cc < N && b[rr * N + cc] === color)
        all = all.concat(line);
    }
    return all.length ? all : null;
  }

  function legalMoves(color) {
    var m = [];
    for (var i = 0; i < N * N; i++) if (flipsFor(board, i, color)) m.push(i);
    return m;
  }

  function applyMove(b, idx, color) {
    var f = flipsFor(b, idx, color);
    b[idx] = color;
    for (var i = 0; i < f.length; i++) b[f[i]] = color;
    return f.length;
  }

  function counts() {
    var bk = 0, wh = 0;
    for (var i = 0; i < N * N; i++) {
      if (board[i] === BLACK) bk++;
      else if (board[i] === WHITE) wh++;
    }
    return [bk, wh];
  }

  function render() {
    var hints = (!gameOver && !thinking) ? legalMoves(BLACK) : [];
    var hintSet = {};
    for (var i = 0; i < hints.length; i++) hintSet[hints[i]] = true;
    for (var j = 0; j < N * N; j++) {
      var cell = cells[j], v = board[j];
      cell.innerHTML = "";
      cell.className = "cell" + (hintSet[j] ? " hint" : "") + (j === lastMove ? " last" : "");
      if (v !== EMPTY) {
        var d = document.createElement("div");
        d.className = "disc " + (v === BLACK ? "b" : "w");
        cell.appendChild(d);
      }
    }
    var ct = counts();
    scoreBEl.textContent = ct[0];
    scoreWEl.textContent = ct[1];
    if (gameOver) {
      turnEl.textContent = "O‘yin tugadi";
    } else {
      turnEl.textContent = thinking ? "Kompyuter o‘ylayapti…" : "Navbat sizda";
      turnEl.style.color = thinking ? "#9fb4c8" : "#9fd48a";
    }
  }

  function onCellClick(e) {
    if (gameOver || thinking) return;
    var t = e.target;
    while (t && t !== boardEl && !t.dataset.idx) t = t.parentNode;
    if (!t || t === boardEl) return;
    var idx = +t.dataset.idx;
    if (!flipsFor(board, idx, BLACK)) { sfx('bad'); return; }
    var nf = applyMove(board, idx, BLACK);
    lastMove = idx;
    sfx('flip'); if (nf >= 5) sfx('pop', 2); else sfx('clack');
    afterMove(WHITE);
  }

  function afterMove(next) {
    render();
    var moves = legalMoves(next);
    if (moves.length === 0) { handleNoMoves(next); return; }
    if (next === WHITE) {
      thinking = true;
      render();
      setTimeout(aiTurn, 420);
    }
  }

  function handleNoMoves(color) {
    var other = 3 - color;
    if (legalMoves(other).length === 0) { endGame(); return; }
    msgEl.textContent = (color === BLACK ? "You have" : "AI has") + " uchun yurish qolmadi. Navbat o‘tdi.";
    if (color === BLACK) { thinking = false; render(); }
    else { thinking = false; setTimeout(function(){ render(); }, 300); }
  }

  function aiTurn() {
    if (gameOver) return;
    var moves = legalMoves(WHITE);
    if (moves.length === 0) { thinking = false; handleNoMoves(WHITE); return; }
    var best = -Infinity, top = [];
    for (var i = 0; i < moves.length; i++) {
      var copy = board.slice();
      var n = applyMove(copy, moves[i], WHITE);
      var s = WEIGHTS[moves[i]] + n * 2;
      if (arcadeVariant) {
        var replies = [];
        for (var j = 0; j < copy.length; j++) if (copy[j] === EMPTY && flipsFor(copy, j, BLACK).length) replies.push(j);
        s = WEIGHTS[moves[i]] + n - replies.length * 5;
        if (!replies.length) s += 36;
        if (replies.some(function(j){ return j === 0 || j === 7 || j === 56 || j === 63; })) s -= 90;
      }
      // penalize giving the player a corner
      var pm = [0, 7, 56, 63];
      for (var k = 0; k < pm.length; k++) {
        if (board[pm[k]] === EMPTY && flipsFor(copy, pm[k], BLACK)) { s -= 60; break; }
      }
      if (s > best) { best = s; top = [moves[i]]; }
      else if (s === best) top.push(moves[i]);
    }
    var pick = top[(Math.random() * top.length) | 0];
    applyMove(board, pick, WHITE);
    lastMove = pick;
    thinking = false;
    sfx('step');
    msgEl.textContent = "";
    var pm2 = legalMoves(BLACK);
    if (pm2.length === 0) {
      render();
      handleNoMoves(BLACK);
      if (!gameOver && legalMoves(WHITE).length) {
        thinking = true;
        render();
        setTimeout(aiTurn, 700);
      }
      return;
    }
    render();
  }

  function endGame() {
    gameOver = true;
    thinking = false;
    var ct = counts();
    if (ct[0] > ct[1]) { msgEl.textContent = "Siz yutdingiz " + ct[0] + "–" + ct[1] + "!"; record.w++; sfx('win'); }
    else if (ct[1] > ct[0]) { msgEl.textContent = "Kompyuter yutdi " + ct[1] + "–" + ct[0] + "."; record.l++; sfx('lose'); }
    else { msgEl.textContent = "Draw " + ct[0] + "–" + ct[1] + "."; record.d++; sfx('ding'); }
    try { arcadeStorage.setItem('reversiRecord', JSON.stringify(record)); } catch (e) {}
    drawRecord();
    render();
  }

  reset();

  // Report native gameplay state to the optional mission engine.
  window.__arcadeMissionSource = Object.assign({"id":35,"label":"Winning disc margin","goals":[2,8,16],"note":"Finish with more discs than the CPU; secure corners to widen your lead.","seconds":420}, { read: () => { return (gameOver ? Math.max(0, counts()[0] - counts()[1]) : 0); } });
})();
