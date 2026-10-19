// ==========================================================================
// CHOYXONA ARCADE — 12 PLAYABLE CLASSIC WEB & RETRO GAMES
// 100% CSP-Compliant (no inline handlers; event-delegation aware)
// ==========================================================================
(function () {
  'use strict';

  // Canvas roundRect compatibility polyfill
  if (typeof CanvasRenderingContext2D !== 'undefined' && !CanvasRenderingContext2D.prototype.roundRect) {
    CanvasRenderingContext2D.prototype.roundRect = function (x, y, w, h, r) {
      if (typeof r === 'undefined') r = 0;
      const radius = typeof r === 'number' ? { tl: r, tr: r, br: r, bl: r } : r;
      this.beginPath();
      this.moveTo(x + (radius.tl || 0), y);
      this.lineTo(x + w - (radius.tr || 0), y);
      this.quadraticCurveTo(x + w, y, x + w, y + (radius.tr || 0));
      this.lineTo(x + w, y + h - (radius.br || 0));
      this.quadraticCurveTo(x + w, y + h, x + w - (radius.br || 0), y + h);
      this.lineTo(x + (radius.bl || 0), y + h);
      this.quadraticCurveTo(x, y + h, x, y + h - (radius.bl || 0));
      this.lineTo(x, y + (radius.tl || 0));
      this.quadraticCurveTo(x, y, x + (radius.tl || 0), y);
      this.closePath();
      return this;
    };
  }

  // Audio / Haptic helper
  function triggerHaptic(type, style) {
    try {
      if (window.Telegram && window.Telegram.WebApp && window.Telegram.WebApp.HapticFeedback) {
        const hf = window.Telegram.WebApp.HapticFeedback;
        if (type === 'impact') hf.impactOccurred(style || 'light');
        if (type === 'notification') hf.notificationOccurred(style || 'success');
        if (type === 'selection') hf.selectionChanged();
      }
    } catch (e) {}
  }

  // High score tracking
  function getHs(id) {
    try {
      return parseInt(localStorage.getItem('choy_hs_' + id) || '0', 10);
    } catch (e) {
      return 0;
    }
  }

  function setHs(id, score) {
    try {
      const cur = getHs(id);
      if (score > cur) {
        localStorage.setItem('choy_hs_' + id, String(score));
        return true;
      }
    } catch (e) {}
    return false;
  }

  // Catalog of 12 Games
  const GAMES_CATALOG = [
    {
      id: 'g2048',
      title: '2048 Classic',
      icon: '🔢',
      tag: 'Boshqotirma',
      desc: 'Katakchalarni suring va 2048 raqamiga erishing! Harakatlar va kombinatsiyalar ustasi bo‘ling.'
    },
    {
      id: 'snake',
      title: 'Retro Iloncha (Snake)',
      icon: '🐍',
      tag: 'Retro Arkada',
      desc: 'Klassik iloncha o‘yini. Paxta-gulli choy va tilla tangalarni terib o‘sing va devorlarga urilmang.'
    },
    {
      id: 'tetris',
      title: 'Block Puzzle (Tetris)',
      icon: '🧱',
      tag: 'Mantiqiy Arkada',
      desc: 'Tushayotgan shakllarni to‘g‘ri joylashtiring, qatorlarni to‘ldirib portlating va rekord o‘rnating.'
    },
    {
      id: 'flappy',
      title: 'Flappy Piyola',
      icon: '☕',
      tag: 'Refleks',
      desc: 'Gravitatsiyaga qarshi parvoz qiling! To‘siqlar orasidan chaqqonlik bilan o‘ting.'
    },
    {
      id: 'breakout',
      title: 'Arkanoid Breakout',
      icon: '🏓',
      tag: 'Arkada',
      desc: 'Reketka orqali koptokni qaytaring, barcha rang-barang g‘ishtlarni maydalab tashlang.'
    },
    {
      id: 'mines',
      title: 'Sapyor (Minesweeper)',
      icon: '💣',
      tag: 'Mantiq',
      desc: '8x8 maydonidagi 10 ta minani toping. Raqamlarni hisoblang va bayroqlar o‘rnating.'
    },
    {
      id: 'tictactoe',
      title: 'X-O vs AI (Tic-Tac-Toe)',
      icon: '❌',
      tag: 'Klassik',
      desc: 'Sun’iy intellektga qarshi 3x3 doskada bellashing. Aqlli Minimax algoritmini yenga olasizmi?'
    },
    {
      id: 'memory',
      title: 'Xotira Kartalari (Memory)',
      icon: '🃏',
      tag: 'Xotira',
      desc: '16 ta kartochka orasidan bir xil 8 juftlikni toping. Qisqa vaqt va kam harakat sarflang.'
    },
    {
      id: 'sudoku',
      title: 'Tezkor Sudoku (Mini Logic)',
      icon: '🧩',
      tag: 'Mantiq',
      desc: 'Har bir qatorda, ustunda va 2x2 kvadratda 1 dan 4 gacha raqamlarni takrorlamasdan tering.'
    },
    {
      id: 'whack',
      title: 'Bug Hunter (Xatolarni tut!)',
      icon: '🐞',
      tag: 'Tezlik',
      desc: 'Kod ichidan chiqayotgan xatoliklar (bug)ni tezda bosing! 30 soniyada maksimal ochko to‘plang.'
    },
    {
      id: 'space',
      title: 'Space Defender',
      icon: '🚀',
      tag: 'Retro Otishma',
      desc: 'Kosmik kemangizni boshqaring, tushayotgan dushmanlar to‘dasini lazer bilan yo‘q qiling.'
    },
    {
      id: 'math',
      title: 'Miya Sprinti (Speed Math)',
      icon: '⚡',
      tag: 'Miya Chiniqishi',
      desc: '30 soniyada tezkor arifmetik amallarni yeching. Dasturchilar uchun aqliy gimnastika.'
    }
  ];

  let currentGameInstance = null;
  let currentActiveId = null;

  // Render Lobby
  function renderLobby() {
    const container = document.getElementById('games-container');
    if (!container) return;

    if (currentGameInstance && currentGameInstance.destroy) {
      currentGameInstance.destroy();
      currentGameInstance = null;
    }
    currentActiveId = null;

    let html = `
      <div class="arcade-header-banner">
        <div>
          <div style="font-size:18px;font-weight:800;margin-bottom:4px;">☕ Choyxona Arcade — 12 ta O‘yin</div>
          <div class="hint">Har bir o‘yin mobil va kompyuter uchun to‘liq moslashtirilgan. Rekordlar qurilmada saqlanadi.</div>
        </div>
        <div style="display:flex;align-items:center;gap:8px;">
          <button type="button" class="btn-chip-action primary" data-action="toggle-games-menu">🙈 Menyudan yashirish</button>
        </div>
      </div>
      <div class="arcade-grid">
    `;

    GAMES_CATALOG.forEach(g => {
      const hs = getHs(g.id);
      html += `
        <div class="arcade-card">
          <div class="arcade-card-top">
            <div class="arcade-icon">${g.icon}</div>
            <div class="arcade-info">
              <div class="arcade-title">${g.title}</div>
              <span class="arcade-tag">${g.tag}</span>
            </div>
          </div>
          <div class="arcade-desc">${g.desc}</div>
          <div class="arcade-footer">
            <div class="arcade-hs">🏆 Rekord: <strong>${hs}</strong></div>
            <button type="button" class="btn-chip-action primary" data-action="play-game" data-game="${g.id}">
              ▶ O‘ynash
            </button>
          </div>
        </div>
      `;
    });

    html += `</div>`;
    container.innerHTML = html;
  }

  // Stage Renderer
  function openStage(gameId) {
    const gameMeta = GAMES_CATALOG.find(g => g.id === gameId);
    if (!gameMeta) return;

    const container = document.getElementById('games-container');
    if (!container) return;

    if (currentGameInstance && currentGameInstance.destroy) {
      currentGameInstance.destroy();
    }
    currentActiveId = gameId;

    const hs = getHs(gameId);

    container.innerHTML = `
      <div class="game-stage-wrap">
        <div class="game-stage-header">
          <button type="button" class="btn-chip-action" data-action="close-game-stage">◀ O‘yinlar</button>
          <div style="display:flex;align-items:center;gap:6px;font-weight:800;font-size:14.5px;">
            <span>${gameMeta.icon}</span>
            <span>${gameMeta.title}</span>
          </div>
          <div class="game-scores">
            <div class="game-score-badge live">Hisob: <strong id="game-live-score">0</strong></div>
            <div class="game-score-badge">🏆 <strong id="game-hs-score">${hs}</strong></div>
          </div>
          <button type="button" class="btn-chip-action" data-action="restart-game" title="Qaytadan">🔄</button>
        </div>

        <div class="game-viewport" id="game-viewport"></div>

        <div class="game-touch-controls" id="game-controls">
          <div class="game-dpad">
            <button type="button" class="dpad-btn dpad-up" data-action="game-ctrl" data-dir="up">▲</button>
            <button type="button" class="dpad-btn dpad-left" data-action="game-ctrl" data-dir="left">◀</button>
            <div class="dpad-center">❖</div>
            <button type="button" class="dpad-btn dpad-right" data-action="game-ctrl" data-dir="right">▶</button>
            <button type="button" class="dpad-btn dpad-down" data-action="game-ctrl" data-dir="down">▼</button>
          </div>
          <div class="game-actions-cluster">
            <button type="button" class="game-action-btn" data-action="game-ctrl" data-dir="action">
              <span>Harakat</span>
            </button>
          </div>
        </div>
      </div>
    `;

    // Instantiate game
    const viewport = document.getElementById('game-viewport');
    currentGameInstance = createGameEngine(gameId, viewport);
    if (currentGameInstance && currentGameInstance.init) {
      currentGameInstance.init();
    }
  }

  function updateLiveScore(score) {
    const el = document.getElementById('game-live-score');
    if (el) el.textContent = score;
    if (currentActiveId) {
      const isNew = setHs(currentActiveId, score);
      if (isNew) {
        const hsEl = document.getElementById('game-hs-score');
        if (hsEl) hsEl.textContent = score;
      }
    }
  }

  // =========================================================================
  // 12 INDIVIDUAL GAME IMPLEMENTATIONS
  // =========================================================================
  function createGameEngine(gameId, viewport) {
    switch (gameId) {
      // 1. 2048 CLASSIC
      case 'g2048': {
        let board = [
          [0, 0, 0, 0],
          [0, 0, 0, 0],
          [0, 0, 0, 0],
          [0, 0, 0, 0]
        ];
        let score = 0;
        let isOver = false;

        function addTile() {
          const empty = [];
          for (let r = 0; r < 4; r++) {
            for (let c = 0; c < 4; c++) {
              if (board[r][c] === 0) empty.push({ r, c });
            }
          }
          if (empty.length > 0) {
            const spot = empty[Math.floor(Math.random() * empty.length)];
            board[spot.r][spot.c] = Math.random() < 0.9 ? 2 : 4;
          }
        }

        function render() {
          let bHtml = '<div class="g2048-board">';
          for (let r = 0; r < 4; r++) {
            for (let c = 0; c < 4; c++) {
              const val = board[r][c];
              bHtml += `<div class="g2048-tile" data-val="${val}">${val > 0 ? val : ''}</div>`;
            }
          }
          bHtml += '</div>';
          if (isOver) {
            bHtml += `
              <div class="game-overlay-msg">
                <h2>O‘yin tugadi!</h2>
                <p>Yakuniy hisobingiz: <strong>${score}</strong></p>
                <button type="button" class="btn-action btn-primary" data-action="restart-game">Qayta o‘ynash</button>
              </div>
            `;
          }
          viewport.innerHTML = bHtml;
          updateLiveScore(score);
        }

        function slide(dir) {
          if (isOver) return;
          let moved = false;

          function operate(row) {
            let filtered = row.filter(x => x !== 0);
            for (let i = 0; i < filtered.length - 1; i++) {
              if (filtered[i] === filtered[i + 1]) {
                filtered[i] *= 2;
                score += filtered[i];
                filtered[i + 1] = 0;
                triggerHaptic('impact', 'light');
              }
            }
            filtered = filtered.filter(x => x !== 0);
            while (filtered.length < 4) filtered.push(0);
            return filtered;
          }

          const oldBoard = JSON.stringify(board);

          if (dir === 'left') {
            for (let r = 0; r < 4; r++) board[r] = operate(board[r]);
          } else if (dir === 'right') {
            for (let r = 0; r < 4; r++) board[r] = operate(board[r].slice().reverse()).reverse();
          } else if (dir === 'up') {
            for (let c = 0; c < 4; c++) {
              let col = [board[0][c], board[1][c], board[2][c], board[3][c]];
              col = operate(col);
              for (let r = 0; r < 4; r++) board[r][c] = col[r];
            }
          } else if (dir === 'down') {
            for (let c = 0; c < 4; c++) {
              let col = [board[3][c], board[2][c], board[1][c], board[0][c]];
              col = operate(col);
              for (let r = 0; r < 4; r++) board[3 - r][c] = col[r];
            }
          }

          if (JSON.stringify(board) !== oldBoard) {
            addTile();
            checkOver();
            render();
          }
        }

        function checkOver() {
          for (let r = 0; r < 4; r++) {
            for (let c = 0; c < 4; c++) {
              if (board[r][c] === 0) return;
              if (r < 3 && board[r][c] === board[r + 1][c]) return;
              if (c < 3 && board[r][c] === board[r][c + 1]) return;
            }
          }
          isOver = true;
          triggerHaptic('notification', 'error');
        }

        let touchStartX = 0;
        let touchStartY = 0;
        function onTouchStart(e) {
          if (e.touches && e.touches[0]) {
            touchStartX = e.touches[0].clientX;
            touchStartY = e.touches[0].clientY;
          }
        }
        function onTouchEnd(e) {
          if (!e.changedTouches || !e.changedTouches[0]) return;
          const dx = e.changedTouches[0].clientX - touchStartX;
          const dy = e.changedTouches[0].clientY - touchStartY;
          if (Math.abs(dx) > Math.abs(dy) && Math.abs(dx) > 30) {
            slide(dx > 0 ? 'right' : 'left');
          } else if (Math.abs(dy) > 30) {
            slide(dy > 0 ? 'down' : 'up');
          }
        }

        return {
          init: () => {
            addTile();
            addTile();
            render();
            viewport.addEventListener('touchstart', onTouchStart, { passive: true });
            viewport.addEventListener('touchend', onTouchEnd, { passive: true });
          },
          onCtrl: (dir) => slide(dir),
          destroy: () => {
            viewport.removeEventListener('touchstart', onTouchStart);
            viewport.removeEventListener('touchend', onTouchEnd);
          }
        };
      }

      // 2. RETRO ILONCHA (SNAKE)
      case 'snake': {
        viewport.innerHTML = '<canvas class="game-canvas" width="320" height="320"></canvas>';
        const canvas = viewport.querySelector('canvas');
        const ctx = canvas.getContext('2d');
        const gridSize = 16;
        const tileCount = 20;

        let snake = [{ x: 10, y: 10 }, { x: 9, y: 10 }, { x: 8, y: 10 }];
        let dir = { x: 1, y: 0 };
        let nextDir = { x: 1, y: 0 };
        let food = { x: 15, y: 10 };
        let score = 0;
        let isOver = false;
        let intervalId = null;

        function placeFood() {
          food = {
            x: Math.floor(Math.random() * tileCount),
            y: Math.floor(Math.random() * tileCount)
          };
          for (let part of snake) {
            if (part.x === food.x && part.y === food.y) return placeFood();
          }
        }

        function tick() {
          if (isOver) return;
          dir = nextDir;
          const head = { x: snake[0].x + dir.x, y: snake[0].y + dir.y };

          // Wall collision
          if (head.x < 0 || head.x >= tileCount || head.y < 0 || head.y >= tileCount) {
            gameOver();
            return;
          }
          // Self collision
          for (let part of snake) {
            if (part.x === head.x && part.y === head.y) {
              gameOver();
              return;
            }
          }

          snake.unshift(head);
          if (head.x === food.x && head.y === food.y) {
            score += 10;
            triggerHaptic('impact', 'medium');
            updateLiveScore(score);
            placeFood();
          } else {
            snake.pop();
          }

          draw();
        }

        function draw() {
          ctx.fillStyle = '#090D16';
          ctx.fillRect(0, 0, canvas.width, canvas.height);

          // Grid lines
          ctx.strokeStyle = 'rgba(255, 255, 255, 0.04)';
          for (let i = 0; i <= canvas.width; i += gridSize) {
            ctx.beginPath();
            ctx.moveTo(i, 0); ctx.lineTo(i, canvas.height);
            ctx.moveTo(0, i); ctx.lineTo(canvas.width, i);
            ctx.stroke();
          }

          // Food (Golden tea bowl icon)
          ctx.font = '14px sans-serif';
          ctx.textAlign = 'center';
          ctx.textBaseline = 'middle';
          ctx.fillText('☕', food.x * gridSize + gridSize / 2, food.y * gridSize + gridSize / 2);

          // Snake
          snake.forEach((part, idx) => {
            ctx.fillStyle = idx === 0 ? '#10B981' : (idx % 2 === 0 ? '#34D399' : '#059669');
            ctx.beginPath();
            ctx.roundRect(part.x * gridSize + 1, part.y * gridSize + 1, gridSize - 2, gridSize - 2, 4);
            ctx.fill();
          });
        }

        function gameOver() {
          isOver = true;
          clearInterval(intervalId);
          triggerHaptic('notification', 'error');
          viewport.innerHTML += `
            <div class="game-overlay-msg">
              <h2>Iloncha to‘qnashdi!</h2>
              <p>To‘plangan ochko: <strong>${score}</strong></p>
              <button type="button" class="btn-action btn-primary" data-action="restart-game">Qayta o‘ynash</button>
            </div>
          `;
        }

        return {
          init: () => {
            placeFood();
            draw();
            intervalId = setInterval(tick, 125);
          },
          onCtrl: (d) => {
            if (d === 'up' && dir.y === 0) nextDir = { x: 0, y: -1 };
            if (d === 'down' && dir.y === 0) nextDir = { x: 0, y: 1 };
            if (d === 'left' && dir.x === 0) nextDir = { x: -1, y: 0 };
            if (d === 'right' && dir.x === 0) nextDir = { x: 1, y: 0 };
          },
          destroy: () => clearInterval(intervalId)
        };
      }

      // 3. BLOCK PUZZLE (TETRIS)
      case 'tetris': {
        viewport.innerHTML = '<canvas class="game-canvas" width="200" height="360"></canvas>';
        const canvas = viewport.querySelector('canvas');
        const ctx = canvas.getContext('2d');
        const cols = 10;
        const rows = 18;
        const sz = 20;

        let grid = Array.from({ length: rows }, () => Array(cols).fill(0));
        let score = 0;
        let isOver = false;
        let intervalId = null;

        const SHAPES = [
          { shape: [[1, 1, 1, 1]], color: '#06B6D4' }, // I
          { shape: [[1, 1], [1, 1]], color: '#FBBF24' }, // O
          { shape: [[0, 1, 0], [1, 1, 1]], color: '#A855F7' }, // T
          { shape: [[1, 0, 0], [1, 1, 1]], color: '#3B82F6' }, // J
          { shape: [[0, 0, 1], [1, 1, 1]], color: '#F97316' }, // L
          { shape: [[0, 1, 1], [1, 1, 0]], color: '#10B981' }, // S
          { shape: [[1, 1, 0], [0, 1, 1]], color: '#EF4444' }  // Z
        ];

        let piece = null;
        let pieceX = 3;
        let pieceY = 0;

        function spawn() {
          const r = SHAPES[Math.floor(Math.random() * SHAPES.length)];
          piece = { shape: r.shape, color: r.color };
          pieceX = Math.floor((cols - piece.shape[0].length) / 2);
          pieceY = 0;

          if (collide(pieceX, pieceY, piece.shape)) {
            isOver = true;
            clearInterval(intervalId);
            viewport.innerHTML += `
              <div class="game-overlay-msg">
                <h2>O‘yin tugadi!</h2>
                <p>Yakuniy ochko: <strong>${score}</strong></p>
                <button type="button" class="btn-action btn-primary" data-action="restart-game">Qayta boshlash</button>
              </div>
            `;
          }
        }

        function collide(px, py, s) {
          for (let r = 0; r < s.length; r++) {
            for (let c = 0; c < s[r].length; c++) {
              if (s[r][c]) {
                const nx = px + c;
                const ny = py + r;
                if (nx < 0 || nx >= cols || ny >= rows) return true;
                if (ny >= 0 && grid[ny][nx]) return true;
              }
            }
          }
          return false;
        }

        function merge() {
          piece.shape.forEach((r, y) => {
            r.forEach((val, x) => {
              if (val) grid[pieceY + y][pieceX + x] = piece.color;
            });
          });

          // Clear full lines
          let lines = 0;
          for (let y = rows - 1; y >= 0; y--) {
            if (grid[y].every(cell => cell !== 0)) {
              grid.splice(y, 1);
              grid.unshift(Array(cols).fill(0));
              lines++;
              y++;
            }
          }
          if (lines > 0) {
            score += lines * 100 * lines;
            triggerHaptic('notification', 'success');
            updateLiveScore(score);
          }
          spawn();
        }

        function drop() {
          if (isOver) return;
          if (!collide(pieceX, pieceY + 1, piece.shape)) {
            pieceY++;
          } else {
            merge();
          }
          draw();
        }

        function rotate() {
          const rotated = piece.shape[0].map((_, i) => piece.shape.map(row => row[i]).reverse());
          if (!collide(pieceX, pieceY, rotated)) {
            piece.shape = rotated;
            triggerHaptic('selection');
            draw();
          }
        }

        function draw() {
          ctx.fillStyle = '#070B12';
          ctx.fillRect(0, 0, canvas.width, canvas.height);

          // Grid cells
          for (let y = 0; y < rows; y++) {
            for (let x = 0; x < cols; x++) {
              if (grid[y][x]) {
                ctx.fillStyle = grid[y][x];
                ctx.fillRect(x * sz + 1, y * sz + 1, sz - 2, sz - 2);
              }
            }
          }

          // Active piece
          if (piece) {
            ctx.fillStyle = piece.color;
            piece.shape.forEach((r, y) => {
              r.forEach((val, x) => {
                if (val) {
                  ctx.fillRect((pieceX + x) * sz + 1, (pieceY + y) * sz + 1, sz - 2, sz - 2);
                }
              });
            });
          }
        }

        return {
          init: () => {
            spawn();
            draw();
            intervalId = setInterval(drop, 480);
          },
          onCtrl: (d) => {
            if (d === 'left' && !collide(pieceX - 1, pieceY, piece.shape)) { pieceX--; draw(); }
            if (d === 'right' && !collide(pieceX + 1, pieceY, piece.shape)) { pieceX++; draw(); }
            if (d === 'down') drop();
            if (d === 'up' || d === 'action') rotate();
          },
          destroy: () => clearInterval(intervalId)
        };
      }

      // 4. FLAPPY PIYOLA
      case 'flappy': {
        viewport.innerHTML = '<canvas class="game-canvas" width="320" height="380"></canvas>';
        const canvas = viewport.querySelector('canvas');
        const ctx = canvas.getContext('2d');

        let y = 160;
        let vy = 0;
        let pipes = [];
        let score = 0;
        let isOver = false;
        let animId = null;
        let frame = 0;

        function jump() {
          if (isOver) return;
          vy = -6.2;
          triggerHaptic('impact', 'light');
        }

        function loop() {
          frame++;
          ctx.fillStyle = '#060A14';
          ctx.fillRect(0, 0, canvas.width, canvas.height);

          vy += 0.36; // Gravity
          y += vy;

          // Spawn pipes
          if (frame % 85 === 0) {
            const gap = 110;
            const topH = Math.floor(Math.random() * (canvas.height - gap - 60)) + 30;
            pipes.push({ x: canvas.width, topH, gap, passed: false });
          }

          // Draw Piyola (Tea cup)
          ctx.font = '26px sans-serif';
          ctx.textAlign = 'center';
          ctx.textBaseline = 'middle';
          ctx.fillText('☕', 60, y);

          // Update & draw pipes
          ctx.fillStyle = '#D97706';
          for (let i = 0; i < pipes.length; i++) {
            const p = pipes[i];
            p.x -= 2.4;

            ctx.fillRect(p.x, 0, 36, p.topH);
            ctx.fillRect(p.x, p.topH + p.gap, 36, canvas.height - p.topH - p.gap);

            // Score check
            if (!p.passed && p.x + 36 < 60) {
              p.passed = true;
              score++;
              triggerHaptic('impact', 'medium');
              updateLiveScore(score);
            }

            // Collision check
            if (60 + 12 > p.x && 60 - 12 < p.x + 36) {
              if (y - 12 < p.topH || y + 12 > p.topH + p.gap) {
                gameOver();
                return;
              }
            }
          }

          // Floor / Ceiling hit
          if (y < 0 || y > canvas.height) {
            gameOver();
            return;
          }

          // Clean pipes
          pipes = pipes.filter(p => p.x > -50);

          animId = requestAnimationFrame(loop);
        }

        function gameOver() {
          isOver = true;
          cancelAnimationFrame(animId);
          triggerHaptic('notification', 'error');
          viewport.innerHTML += `
            <div class="game-overlay-msg">
              <h2>Piyola sindi!</h2>
              <p>Ochko: <strong>${score}</strong></p>
              <button type="button" class="btn-action btn-primary" data-action="restart-game">Qayta uchish</button>
            </div>
          `;
        }

        viewport.addEventListener('click', jump);

        return {
          init: () => {
            loop();
          },
          onCtrl: () => jump(),
          destroy: () => {
            cancelAnimationFrame(animId);
            viewport.removeEventListener('click', jump);
          }
        };
      }

      // 5. ARKANOID BREAKOUT
      case 'breakout': {
        viewport.innerHTML = '<canvas class="game-canvas" width="320" height="360"></canvas>';
        const canvas = viewport.querySelector('canvas');
        const ctx = canvas.getContext('2d');

        let paddle = { x: 125, w: 70, h: 10 };
        let ball = { x: 160, y: 220, dx: 3, dy: -3, r: 6 };
        let bricks = [];
        let score = 0;
        let isOver = false;
        let animId = null;

        const colors = ['#EF4444', '#F59E0B', '#10B981', '#38BDF8'];
        for (let r = 0; r < 4; r++) {
          for (let c = 0; c < 6; c++) {
            bricks.push({ x: c * 52 + 6, y: r * 20 + 35, w: 46, h: 14, active: true, color: colors[r] });
          }
        }

        function loop() {
          ctx.fillStyle = '#080C14';
          ctx.fillRect(0, 0, canvas.width, canvas.height);

          // Draw Bricks
          bricks.forEach(b => {
            if (b.active) {
              ctx.fillStyle = b.color;
              ctx.beginPath();
              ctx.roundRect(b.x, b.y, b.w, b.h, 3);
              ctx.fill();
            }
          });

          // Draw Paddle
          ctx.fillStyle = '#38BDF8';
          ctx.beginPath();
          ctx.roundRect(paddle.x, canvas.height - 22, paddle.w, paddle.h, 5);
          ctx.fill();

          // Draw Ball
          ctx.fillStyle = '#FFFDF9';
          ctx.beginPath();
          ctx.arc(ball.x, ball.y, ball.r, 0, Math.PI * 2);
          ctx.fill();

          // Move ball
          ball.x += ball.dx;
          ball.y += ball.dy;

          // Wall bounces
          if (ball.x - ball.r < 0 || ball.x + ball.r > canvas.width) ball.dx *= -1;
          if (ball.y - ball.r < 0) ball.dy *= -1;

          // Paddle bounce
          if (ball.y + ball.r >= canvas.height - 22 && ball.y - ball.r <= canvas.height - 12) {
            if (ball.x >= paddle.x && ball.x <= paddle.x + paddle.w) {
              ball.dy = -Math.abs(ball.dy);
              const hitOffset = (ball.x - (paddle.x + paddle.w / 2)) / (paddle.w / 2);
              ball.dx = hitOffset * 4.5;
              triggerHaptic('impact', 'light');
            }
          }

          // Brick collision
          bricks.forEach(b => {
            if (b.active && ball.x > b.x && ball.x < b.x + b.w && ball.y > b.y && ball.y < b.y + b.h) {
              b.active = false;
              ball.dy *= -1;
              score += 15;
              triggerHaptic('impact', 'medium');
              updateLiveScore(score);
            }
          });

          // All bricks cleared victory
          if (bricks.every(b => !b.active)) {
            isOver = true;
            cancelAnimationFrame(animId);
            updateLiveScore(score + 100);
            triggerHaptic('notification', 'success');
            viewport.innerHTML += `
              <div class="game-overlay-msg">
                <h2>🎉 Barcha g‘ishtlar urildi!</h2>
                <p>Yakuniy ochko: <strong>${score + 100}</strong></p>
                <button type="button" class="btn-action btn-primary" data-action="restart-game">Yana o‘ynash</button>
              </div>
            `;
            return;
          }

          // Floor death
          if (ball.y > canvas.height) {
            isOver = true;
            cancelAnimationFrame(animId);
            viewport.innerHTML += `
              <div class="game-overlay-msg">
                <h2>Koptok tushib ketdi!</h2>
                <p>Ochko: <strong>${score}</strong></p>
                <button type="button" class="btn-action btn-primary" data-action="restart-game">Qaytadan</button>
              </div>
            `;
            return;
          }

          animId = requestAnimationFrame(loop);
        }

        function onTouchMove(e) {
          if (!e.touches || !e.touches[0]) return;
          const rect = canvas.getBoundingClientRect();
          const clientX = e.touches[0].clientX - rect.left;
          paddle.x = Math.max(0, Math.min(canvas.width - paddle.w, (clientX / rect.width) * canvas.width - paddle.w / 2));
        }

        canvas.addEventListener('touchmove', onTouchMove, { passive: true });

        return {
          init: () => { loop(); },
          onCtrl: (d) => {
            if (d === 'left') paddle.x = Math.max(0, paddle.x - 28);
            if (d === 'right') paddle.x = Math.min(canvas.width - paddle.w, paddle.x + 28);
          },
          destroy: () => {
            cancelAnimationFrame(animId);
            canvas.removeEventListener('touchmove', onTouchMove);
          }
        };
      }

      // 6. SAPYOR (MINESWEEPER)
      case 'mines': {
        const size = 8;
        const totalMines = 10;
        let grid = [];
        let flagMode = false;
        let isOver = false;
        let started = false;
        let revealedCount = 0;

        function createBoard() {
          grid = [];
          for (let r = 0; r < size; r++) {
            grid[r] = [];
            for (let c = 0; c < size; c++) {
              grid[r][c] = { mine: false, revealed: false, flagged: false, count: 0 };
            }
          }
        }

        function populateMines(safeR, safeC) {
          let placed = 0;
          while (placed < totalMines) {
            const r = Math.floor(Math.random() * size);
            const c = Math.floor(Math.random() * size);
            if ((r === safeR && c === safeC) || grid[r][c].mine) continue;
            grid[r][c].mine = true;
            placed++;
          }
          // Compute neighbors
          for (let r = 0; r < size; r++) {
            for (let c = 0; c < size; c++) {
              if (grid[r][c].mine) continue;
              let cnt = 0;
              for (let dr = -1; dr <= 1; dr++) {
                for (let dc = -1; dc <= 1; dc++) {
                  const nr = r + dr, nc = c + dc;
                  if (nr >= 0 && nr < size && nc >= 0 && nc < size && grid[nr][nc].mine) cnt++;
                }
              }
              grid[r][c].count = cnt;
            }
          }
        }

        function render() {
          let html = `
            <div style="display:flex;align-items:center;justify-content:space-between;width:92%;margin-bottom:8px;">
              <button type="button" class="btn-chip-action ${flagMode ? 'primary' : ''}" data-action="game-mode-toggle">
                ${flagMode ? '🚩 Bayroq rejimi' : '⛏️ Qazish rejimi'}
              </button>
              <span class="hint">Minlar: 10 ta</span>
            </div>
            <div class="mines-board">
          `;

          for (let r = 0; r < size; r++) {
            for (let c = 0; c < size; c++) {
              const cell = grid[r][c];
              let content = '';
              let cls = 'mines-cell';
              if (cell.flagged) {
                content = '🚩';
              } else if (cell.revealed) {
                cls += ' revealed';
                if (cell.mine) {
                  cls += ' mine';
                  content = '💣';
                } else if (cell.count > 0) {
                  content = cell.count;
                }
              }
              html += `<div class="${cls}" data-action="game-cell" data-r="${r}" data-c="${c}" data-num="${cell.count}">${content}</div>`;
            }
          }
          html += '</div>';

          if (isOver) {
            html += `
              <div class="game-overlay-msg">
                <h2>Mina portladi!</h2>
                <button type="button" class="btn-action btn-primary" data-action="restart-game">Qayta urinish</button>
              </div>
            `;
          }
          viewport.innerHTML = html;
        }

        function clickCell(r, c) {
          if (isOver) return;
          const cell = grid[r][c];

          if (flagMode) {
            if (!cell.revealed) {
              cell.flagged = !cell.flagged;
              triggerHaptic('selection');
              render();
            }
            return;
          }

          if (cell.flagged || cell.revealed) return;

          if (!started) {
            started = true;
            populateMines(r, c);
          }

          if (cell.mine) {
            cell.revealed = true;
            isOver = true;
            triggerHaptic('notification', 'error');
            // Reveal all mines
            grid.forEach(row => row.forEach(cl => { if (cl.mine) cl.revealed = true; }));
            render();
            return;
          }

          reveal(r, c);
          triggerHaptic('impact', 'light');
          updateLiveScore(revealedCount * 5);

          // Check win
          if (revealedCount === size * size - totalMines) {
            triggerHaptic('notification', 'success');
            viewport.innerHTML += `
              <div class="game-overlay-msg">
                <h2>🎉 G‘alaba!</h2>
                <p>Barcha minalardan muvaffaqiyatli o‘tdingiz!</p>
                <button type="button" class="btn-action btn-primary" data-action="restart-game">Yana o‘ynash</button>
              </div>
            `;
          } else {
            render();
          }
        }

        function reveal(r, c) {
          if (r < 0 || r >= size || c < 0 || c >= size) return;
          const cell = grid[r][c];
          if (cell.revealed || cell.flagged || cell.mine) return;

          cell.revealed = true;
          revealedCount++;

          if (cell.count === 0) {
            for (let dr = -1; dr <= 1; dr++) {
              for (let dc = -1; dc <= 1; dc++) {
                reveal(r + dr, c + dc);
              }
            }
          }
        }

        return {
          init: () => {
            createBoard();
            render();
          },
          onCellClick: (el) => {
            const r = parseInt(el.dataset.r, 10);
            const c = parseInt(el.dataset.c, 10);
            clickCell(r, c);
          },
          onToggleMode: () => {
            flagMode = !flagMode;
            render();
          },
          destroy: () => {}
        };
      }

      // 7. TIC-TAC-TOE VS SMART AI
      case 'tictactoe': {
        let board = Array(9).fill(null);
        let turn = 'X'; // Player: X, AI: O
        let isOver = false;
        let score = 0;

        function checkWinner(bd) {
          const lines = [
            [0, 1, 2], [3, 4, 5], [6, 7, 8],
            [0, 3, 6], [1, 4, 7], [2, 5, 8],
            [0, 4, 8], [2, 4, 6]
          ];
          for (let l of lines) {
            if (bd[l[0]] && bd[l[0]] === bd[l[1]] && bd[l[0]] === bd[l[2]]) {
              return bd[l[0]];
            }
          }
          if (bd.every(x => x !== null)) return 'tie';
          return null;
        }

        function minimax(newBoard, player) {
          const avail = newBoard.map((v, i) => v === null ? i : null).filter(v => v !== null);
          const winner = checkWinner(newBoard);
          if (winner === 'O') return { score: 10 };
          if (winner === 'X') return { score: -10 };
          if (winner === 'tie') return { score: 0 };

          const moves = [];
          for (let i = 0; i < avail.length; i++) {
            const idx = avail[i];
            newBoard[idx] = player;
            const res = minimax(newBoard, player === 'O' ? 'X' : 'O');
            moves.push({ index: idx, score: res.score });
            newBoard[idx] = null;
          }

          let bestMove;
          if (player === 'O') {
            let bestScore = -10000;
            for (let m of moves) {
              if (m.score > bestScore) { bestScore = m.score; bestMove = m; }
            }
          } else {
            let bestScore = 10000;
            for (let m of moves) {
              if (m.score < bestScore) { bestScore = m.score; bestMove = m; }
            }
          }
          return bestMove;
        }

        function aiMove() {
          const best = minimax(board, 'O');
          if (best && best.index !== undefined) {
            board[best.index] = 'O';
            triggerHaptic('impact', 'light');
            const win = checkWinner(board);
            if (win) handleEnd(win);
            else render();
          }
        }

        function handleEnd(winner) {
          isOver = true;
          let msg = '';
          if (winner === 'X') {
            msg = '🎉 Siz g‘alaba qozondingiz!';
            score += 50;
            updateLiveScore(score);
            triggerHaptic('notification', 'success');
          } else if (winner === 'O') {
            msg = '🤖 AI g‘alaba qozondi!';
            triggerHaptic('notification', 'error');
          } else {
            msg = '🤝 Durrang!';
          }

          render();
          viewport.innerHTML += `
            <div class="game-overlay-msg">
              <h2>${msg}</h2>
              <button type="button" class="btn-action btn-primary" data-action="restart-game">Yana o‘ynash</button>
            </div>
          `;
        }

        function render() {
          let html = '<div class="ttt-board">';
          board.forEach((val, i) => {
            html += `<div class="ttt-cell ${val ? val.toLowerCase() : ''}" data-action="game-cell" data-idx="${i}">${val || ''}</div>`;
          });
          html += '</div>';
          viewport.innerHTML = html;
        }

        return {
          init: () => render(),
          onCellClick: (el) => {
            if (isOver) return;
            const idx = parseInt(el.dataset.idx, 10);
            if (board[idx] === null) {
              board[idx] = 'X';
              triggerHaptic('selection');
              const win = checkWinner(board);
              if (win) {
                handleEnd(win);
              } else {
                render();
                setTimeout(aiMove, 250);
              }
            }
          },
          destroy: () => {}
        };
      }

      // 8. MEMORY MATCH
      case 'memory': {
        const EMOJIS = ['☕', '🍵', '🇺🇿', '💻', '🚀', '💎', '🎯', '⚡'];
        let cards = [...EMOJIS, ...EMOJIS].sort(() => Math.random() - 0.5);
        let flipped = [];
        let matched = [];
        let moves = 0;
        let isLocked = false;
        let flipTimer = null;

        function render() {
          let html = '<div class="memory-board">';
          cards.forEach((emoji, i) => {
            const isFlipped = flipped.includes(i) || matched.includes(i);
            const isMatch = matched.includes(i);
            html += `
              <div class="memory-card ${isFlipped ? 'flipped' : ''} ${isMatch ? 'matched' : ''}" data-action="game-cell" data-idx="${i}">
                ${isFlipped ? emoji : '❓'}
              </div>
            `;
          });
          html += '</div>';

          if (matched.length === cards.length) {
            const finalScore = Math.max(10, 200 - moves * 5);
            updateLiveScore(finalScore);
            triggerHaptic('notification', 'success');
            html += `
              <div class="game-overlay-msg">
                <h2>Barcha juftliklar topildi!</h2>
                <p>Harakatlar: <strong>${moves}</strong> | Ochko: <strong>${finalScore}</strong></p>
                <button type="button" class="btn-action btn-primary" data-action="restart-game">Yana o‘ynash</button>
              </div>
            `;
          }
          viewport.innerHTML = html;
        }

        return {
          init: () => render(),
          onCellClick: (el) => {
            if (isLocked) return;
            const idx = parseInt(el.dataset.idx, 10);
            if (flipped.includes(idx) || matched.includes(idx)) return;

            flipped.push(idx);
            triggerHaptic('selection');
            render();

            if (flipped.length === 2) {
              moves++;
              isLocked = true;
              const [i1, i2] = flipped;
              if (cards[i1] === cards[i2]) {
                matched.push(i1, i2);
                flipped = [];
                isLocked = false;
                triggerHaptic('impact', 'medium');
                render();
              } else {
                flipTimer = setTimeout(() => {
                  flipped = [];
                  isLocked = false;
                  render();
                }, 600);
              }
            }
          },
          destroy: () => {
            if (flipTimer) clearTimeout(flipTimer);
          }
        };
      }

      // 9. TEZKOR SUDOKU 4x4
      case 'sudoku': {
        const solution = [
          [1, 2, 3, 4],
          [3, 4, 1, 2],
          [2, 3, 4, 1],
          [4, 1, 2, 3]
        ];
        let given = [
          [1, 0, 3, 0],
          [0, 4, 0, 2],
          [2, 0, 4, 0],
          [0, 1, 0, 3]
        ];
        let board = given.map(r => r.slice());
        let selected = { r: 0, c: 1 };

        function render() {
          let html = '<div class="sudoku-board">';
          for (let r = 0; r < 4; r++) {
            for (let c = 0; c < 4; c++) {
              const val = board[r][c];
              const isGv = given[r][c] !== 0;
              const isSel = selected && selected.r === r && selected.c === c;
              html += `
                <div class="sudoku-cell ${isGv ? 'given' : ''} ${isSel ? 'selected' : ''}" data-action="game-cell" data-r="${r}" data-c="${c}">
                  ${val > 0 ? val : ''}
                </div>
              `;
            }
          }
          html += '</div>';

          // Keypad
          html += `
            <div class="sudoku-keypad">
              <button type="button" class="sudoku-key" data-action="game-num-select" data-num="1">1</button>
              <button type="button" class="sudoku-key" data-action="game-num-select" data-num="2">2</button>
              <button type="button" class="sudoku-key" data-action="game-num-select" data-num="3">3</button>
              <button type="button" class="sudoku-key" data-action="game-num-select" data-num="4">4</button>
              <button type="button" class="sudoku-key" data-action="game-num-select" data-num="0">⌫</button>
            </div>
          `;

          // Check win
          let won = true;
          for (let r = 0; r < 4; r++) {
            for (let c = 0; c < 4; c++) {
              if (board[r][c] !== solution[r][c]) won = false;
            }
          }
          if (won) {
            updateLiveScore(100);
            triggerHaptic('notification', 'success');
            html += `
              <div class="game-overlay-msg">
                <h2>🎉 Sudoku to‘liq yechildi!</h2>
                <button type="button" class="btn-action btn-primary" data-action="restart-game">Yana yechish</button>
              </div>
            `;
          }
          viewport.innerHTML = html;
        }

        return {
          init: () => render(),
          onCellClick: (el) => {
            const r = parseInt(el.dataset.r, 10);
            const c = parseInt(el.dataset.c, 10);
            if (given[r][c] === 0) {
              selected = { r, c };
              triggerHaptic('selection');
              render();
            }
          },
          onNumSelect: (num) => {
            if (selected && given[selected.r][selected.c] === 0) {
              board[selected.r][selected.c] = parseInt(num, 10);
              triggerHaptic('impact', 'light');
              render();
            }
          },
          destroy: () => {}
        };
      }

      // 10. BUG HUNTER
      case 'whack': {
        let score = 0;
        let timeLeft = 30;
        let activeHole = -1;
        let timerId = null;
        let spawnId = null;

        function spawn() {
          activeHole = Math.floor(Math.random() * 9);
          render();
        }

        function render() {
          let html = '<div class="whack-board">';
          for (let i = 0; i < 9; i++) {
            const hasBug = (i === activeHole);
            html += `
              <div class="whack-hole" data-action="game-cell" data-idx="${i}">
                ${hasBug ? '<span class="whack-bug">🐞</span>' : ''}
              </div>
            `;
          }
          html += '</div>';

          if (timeLeft <= 0) {
            updateLiveScore(score);
            triggerHaptic('notification', 'success');
            html += `
              <div class="game-overlay-msg">
                <h2>Vaqt tugadi!</h2>
                <p>Ushlangan xatoliklar: <strong>${score / 10} ta</strong></p>
                <button type="button" class="btn-action btn-primary" data-action="restart-game">Yana ovlash</button>
              </div>
            `;
          }
          viewport.innerHTML = html;
        }

        return {
          init: () => {
            render();
            spawnId = setInterval(spawn, 750);
            timerId = setInterval(() => {
              timeLeft--;
              if (timeLeft <= 0) {
                clearInterval(timerId);
                clearInterval(spawnId);
                render();
              }
            }, 1000);
          },
          onCellClick: (el) => {
            const idx = parseInt(el.dataset.idx, 10);
            if (idx === activeHole && timeLeft > 0) {
              score += 10;
              activeHole = -1;
              triggerHaptic('impact', 'medium');
              updateLiveScore(score);
              render();
            }
          },
          destroy: () => {
            clearInterval(timerId);
            clearInterval(spawnId);
          }
        };
      }

      // 11. SPACE DEFENDER
      case 'space': {
        viewport.innerHTML = '<canvas class="game-canvas" width="320" height="380"></canvas>';
        const canvas = viewport.querySelector('canvas');
        const ctx = canvas.getContext('2d');

        let ship = { x: 145, y: 340, w: 30, h: 20 };
        let bullets = [];
        let aliens = [];
        let alienDir = 1;
        let score = 0;
        let isOver = false;
        let animId = null;

        for (let r = 0; r < 3; r++) {
          for (let c = 0; c < 5; c++) {
            aliens.push({ x: c * 50 + 40, y: r * 28 + 30, w: 30, h: 20, active: true });
          }
        }

        function fire() {
          if (isOver) return;
          bullets.push({ x: ship.x + ship.w / 2 - 2, y: ship.y, w: 4, h: 10 });
          triggerHaptic('impact', 'light');
        }

        function loop() {
          ctx.fillStyle = '#060810';
          ctx.fillRect(0, 0, canvas.width, canvas.height);

          // Draw Ship
          ctx.fillStyle = '#38BDF8';
          ctx.beginPath();
          ctx.moveTo(ship.x + ship.w / 2, ship.y);
          ctx.lineTo(ship.x, ship.y + ship.h);
          ctx.lineTo(ship.x + ship.w, ship.y + ship.h);
          ctx.closePath();
          ctx.fill();

          // Move & Draw Bullets
          ctx.fillStyle = '#FBBF24';
          bullets.forEach(b => {
            b.y -= 7;
            ctx.fillRect(b.x, b.y, b.w, b.h);
          });
          bullets = bullets.filter(b => b.y > -20);

          // Move & Draw Aliens
          let edgeHit = false;
          aliens.forEach(a => {
            if (!a.active) return;
            a.x += alienDir * 1.2;
            if (a.x < 10 || a.x + a.w > canvas.width - 10) edgeHit = true;

            ctx.font = '18px sans-serif';
            ctx.fillText('👾', a.x, a.y + a.h);

            // Alien collision with bullets
            bullets.forEach(b => {
              if (b.x > a.x && b.x < a.x + a.w && b.y > a.y && b.y < a.y + a.h) {
                a.active = false;
                b.y = -100;
                score += 20;
                triggerHaptic('impact', 'medium');
                updateLiveScore(score);
              }
            });

            // Alien reached bottom
            if (a.y + a.h >= ship.y) isOver = true;
          });

          if (edgeHit) {
            alienDir *= -1;
            aliens.forEach(a => { a.y += 12; });
          }

          if (isOver || aliens.every(a => !a.active)) {
            cancelAnimationFrame(animId);
            viewport.innerHTML += `
              <div class="game-overlay-msg">
                <h2>${isOver ? 'Kema yakson qilindi!' : 'Kosmos tozalandi!'}</h2>
                <p>Ochko: <strong>${score}</strong></p>
                <button type="button" class="btn-action btn-primary" data-action="restart-game">Qayta jangga</button>
              </div>
            `;
            return;
          }

          animId = requestAnimationFrame(loop);
        }

        canvas.addEventListener('click', fire);

        return {
          init: () => loop(),
          onCtrl: (d) => {
            if (d === 'left') ship.x = Math.max(10, ship.x - 24);
            if (d === 'right') ship.x = Math.min(canvas.width - ship.w - 10, ship.x + 24);
            if (d === 'action' || d === 'up') fire();
          },
          destroy: () => {
            cancelAnimationFrame(animId);
            canvas.removeEventListener('click', fire);
          }
        };
      }

      // 12. MIYA SPRINTI (SPEED MATH)
      case 'math': {
        let score = 0;
        let streak = 0;
        let timeLeft = 30;
        let currentQuestion = null;
        let timerId = null;

        function generateQuestion() {
          const ops = ['+', '-', '*'];
          const op = ops[Math.floor(Math.random() * (streak > 5 ? 3 : 2))];
          let a = Math.floor(Math.random() * 20) + 1;
          let b = Math.floor(Math.random() * 20) + 1;
          if (op === '*') {
            a = Math.floor(Math.random() * 9) + 2;
            b = Math.floor(Math.random() * 9) + 2;
          }
          let ans = 0;
          if (op === '+') ans = a + b;
          if (op === '-') ans = a - b;
          if (op === '*') ans = a * b;

          const options = new Set([ans]);
          while (options.size < 4) {
            const offset = (Math.floor(Math.random() * 7) + 1) * (Math.random() < 0.5 ? 1 : -1);
            options.add(ans + offset);
          }

          currentQuestion = {
            prompt: `${a} ${op === '*' ? '×' : op} ${b}`,
            answer: ans,
            options: Array.from(options).sort(() => Math.random() - 0.5)
          };
          render();
        }

        function checkAnswer(chosen) {
          if (timeLeft <= 0) return;
          if (chosen === currentQuestion.answer) {
            score += 10 + streak * 2;
            streak++;
            timeLeft = Math.min(45, timeLeft + 1); // Time bonus
            triggerHaptic('impact', 'medium');
            updateLiveScore(score);
          } else {
            streak = 0;
            score = Math.max(0, score - 5);
            triggerHaptic('notification', 'error');
            updateLiveScore(score);
          }
          generateQuestion();
        }

        function render() {
          if (!currentQuestion) return;
          let html = `
            <div class="math-container">
              <div class="hint">⏱ Vaqt: <strong>${timeLeft}s</strong> | Ketma-ketlik: <strong>${streak}🔥</strong></div>
              <div class="math-prompt">${currentQuestion.prompt} = ?</div>
              <div class="math-options-grid">
          `;

          currentQuestion.options.forEach(opt => {
            html += `
              <button type="button" class="math-opt-btn" data-action="game-cell" data-val="${opt}">
                ${opt}
              </button>
            `;
          });
          html += '</div></div>';

          if (timeLeft <= 0) {
            triggerHaptic('notification', 'success');
            html += `
              <div class="game-overlay-msg">
                <h2>Vaqt yakunlandi!</h2>
                <p>To‘plangan ochko: <strong>${score}</strong></p>
                <button type="button" class="btn-action btn-primary" data-action="restart-game">Yana sinash</button>
              </div>
            `;
          }
          viewport.innerHTML = html;
        }

        return {
          init: () => {
            generateQuestion();
            timerId = setInterval(() => {
              timeLeft--;
              if (timeLeft <= 0) {
                clearInterval(timerId);
                render();
              } else {
                const hint = viewport.querySelector('.hint');
                if (hint) hint.innerHTML = `⏱ Vaqt: <strong>${timeLeft}s</strong> | Ketma-ketlik: <strong>${streak}🔥</strong>`;
              }
            }, 1000);
          },
          onCellClick: (el) => {
            const val = parseInt(el.dataset.val, 10);
            checkAnswer(val);
          },
          destroy: () => clearInterval(timerId)
        };
      }

      default:
        return null;
    }
  }

  // Keyboard controls listener (Arrows + WASD + Space) — only when games tab is active & not in an input
  window.addEventListener('keydown', (e) => {
    if (!currentGameInstance) return;
    const tabGames = document.getElementById('tab-games');
    if (!tabGames || tabGames.style.display === 'none') return;
    if (e.target && (e.target.matches('input, textarea, select') || e.target.isContentEditable)) return;

    if (['ArrowUp', 'KeyW'].includes(e.code)) {
      e.preventDefault();
      if (currentGameInstance.onCtrl) currentGameInstance.onCtrl('up');
    } else if (['ArrowDown', 'KeyS'].includes(e.code)) {
      e.preventDefault();
      if (currentGameInstance.onCtrl) currentGameInstance.onCtrl('down');
    } else if (['ArrowLeft', 'KeyA'].includes(e.code)) {
      e.preventDefault();
      if (currentGameInstance.onCtrl) currentGameInstance.onCtrl('left');
    } else if (['ArrowRight', 'KeyD'].includes(e.code)) {
      e.preventDefault();
      if (currentGameInstance.onCtrl) currentGameInstance.onCtrl('right');
    } else if (e.code === 'Space') {
      e.preventDefault();
      if (currentGameInstance.onCtrl) currentGameInstance.onCtrl('action');
    }
  });

  // Public API attached to window
  window.ChoyGames = {
    catalog: GAMES_CATALOG,
    init: () => {
      if (!currentActiveId) renderLobby();
      else openStage(currentActiveId);
    },
    renderLobby: renderLobby,
    play: openStage,
    restart: () => {
      if (currentActiveId) openStage(currentActiveId);
    },
    closeStage: () => {
      renderLobby();
    },
    destroyCurrent: () => {
      if (currentGameInstance && currentGameInstance.destroy) {
        try { currentGameInstance.destroy(); } catch (e) {}
        currentGameInstance = null;
      }
      currentActiveId = null;
    },
    handleCtrl: (dir) => {
      if (currentGameInstance && currentGameInstance.onCtrl) {
        currentGameInstance.onCtrl(dir);
        triggerHaptic('selection');
      }
    },
    handleCellClick: (el) => {
      if (currentGameInstance && currentGameInstance.onCellClick) {
        currentGameInstance.onCellClick(el);
      }
    },
    toggleMode: () => {
      if (currentGameInstance && currentGameInstance.onToggleMode) {
        currentGameInstance.onToggleMode();
      }
    },
    handleNumSelect: (num) => {
      if (currentGameInstance && currentGameInstance.onNumSelect) {
        currentGameInstance.onNumSelect(num);
      }
    }
  };

  // Direct global bridge for ui-bind.js
  window.playGame = (id) => openStage(id);
  window.restartCurrentGame = () => window.ChoyGames.restart();
  window.closeGameStage = () => window.ChoyGames.closeStage();
  window.handleGameCtrl = (dir) => window.ChoyGames.handleCtrl(dir);
  window.handleGameCellClick = (el) => window.ChoyGames.handleCellClick(el);
  window.toggleGameMode = () => window.ChoyGames.toggleMode();
  window.handleGameNumSelect = (num) => window.ChoyGames.handleNumSelect(num);

})();
