(() => {
  'use strict';

  const canvas = document.getElementById('game');
  const ctx = canvas.getContext('2d');
  ctx.imageSmoothingEnabled = false;

  const W = 960;
  const H = 540;
  const TILE = 32;
  const LEVEL_H = 17;
  const LEVEL_W = 205;
  const WORLD_W = LEVEL_W * TILE;
  const WORLD_H = LEVEL_H * TILE;
  const GRAVITY = 2050;
  const TERMINAL_VY = 1200;

  const TILE_EMPTY = 0;
  const TILE_DIRT = 1;
  const TILE_GRASS = 2;
  const TILE_BRICK = 3;
  const TILE_SURPRISE = 4;
  const TILE_USED = 5;
  const TILE_PIPE_L = 6;
  const TILE_PIPE_R = 7;
  const TILE_STONE = 8;
  const TILE_CLOUD = 9;

  const solidTiles = new Set([TILE_DIRT, TILE_GRASS, TILE_BRICK, TILE_SURPRISE, TILE_USED, TILE_PIPE_L, TILE_PIPE_R, TILE_STONE]);
  const bumpableTiles = new Set([TILE_BRICK, TILE_SURPRISE]);

  const keys = {
    left: false, right: false, jump: false, run: false,
    jumpPressed: false, jumpReleased: false
  };

  const pressedPhysical = new Set();
  let game;
  let last = performance.now();
  let accumulator = 0;
  const STEP = 1 / 120;

  const clamp = (v, a, b) => Math.max(a, Math.min(b, v));
  const lerp = (a, b, t) => a + (b - a) * t;
  const overlap = (a, b) => a.x < b.x + b.w && a.x + a.w > b.x && a.y < b.y + b.h && a.y + a.h > b.y;
  const rect = (x, y, w, h) => ({ x, y, w, h });

  function color(hex) { return hex; }

  class Level {
    constructor() {
      this.width = LEVEL_W;
      this.height = LEVEL_H;
      this.tiles = new Array(LEVEL_W * LEVEL_H).fill(TILE_EMPTY);
      this.coins = [];
      this.enemies = [];
      this.hazards = [];
      this.powerups = [];
      this.particles = [];
      this.flag = { x: (LEVEL_W - 8) * TILE + 12, y: 8 * TILE, w: 36, h: 7 * TILE };
      this.spawn = { x: 92, y: 11 * TILE };
      this.decorate = [];
      this.build();
    }
    index(tx, ty) { return ty * this.width + tx; }
    inBounds(tx, ty) { return tx >= 0 && tx < this.width && ty >= 0 && ty < this.height; }
    tile(tx, ty) {
      if (tx < 0 || tx >= this.width) return TILE_STONE;
      if (ty < 0 || ty >= this.height) return TILE_EMPTY;
      return this.tiles[this.index(tx, ty)];
    }
    set(tx, ty, type) { if (this.inBounds(tx, ty)) this.tiles[this.index(tx, ty)] = type; }
    fillRect(tx, ty, tw, th, type) {
      for (let y = ty; y < ty + th; y++) for (let x = tx; x < tx + tw; x++) this.set(x, y, type);
    }
    ground(x0, x1, top = 13) {
      for (let x = x0; x <= x1; x++) {
        this.set(x, top, TILE_GRASS);
        for (let y = top + 1; y < LEVEL_H; y++) this.set(x, y, TILE_DIRT);
      }
    }
    platform(x0, x1, y, type = TILE_STONE) { for (let x = x0; x <= x1; x++) this.set(x, y, type); }
    pipe(tx, top, height) {
      for (let y = top; y < top + height; y++) {
        this.set(tx, y, TILE_PIPE_L);
        this.set(tx + 1, y, TILE_PIPE_R);
      }
      this.set(tx - 1, top, TILE_PIPE_L);
      this.set(tx + 2, top, TILE_PIPE_R);
    }
    coin(tx, ty, count = 1, gap = 1) {
      for (let i = 0; i < count; i++) this.coins.push(new Coin((tx + i * gap) * TILE + 16, ty * TILE + 15));
    }
    enemy(tx, ty, kind = 'snail') { this.enemies.push(new Enemy(tx * TILE + 2, ty * TILE + 4, kind)); }
    spike(tx, ty, count = 1) { for (let i = 0; i < count; i++) this.hazards.push({ x: (tx + i) * TILE + 2, y: ty * TILE + 8, w: 28, h: 24 }); }
    build() {
      // Ground with jumps, pits, and sprint lanes.
      this.ground(0, 27, 13);
      this.ground(30, 48, 13);
      this.ground(52, 77, 13);
      this.ground(81, 103, 13);
      this.ground(107, 131, 13);
      this.ground(136, 162, 13);
      this.ground(166, 204, 13);

      // Sloped-ish stair stacks and higher landings.
      this.fillRect(20, 12, 2, 1, TILE_STONE);
      this.fillRect(22, 11, 2, 2, TILE_STONE);
      this.fillRect(24, 10, 2, 3, TILE_STONE);
      this.fillRect(62, 12, 3, 1, TILE_STONE);
      this.fillRect(65, 11, 3, 2, TILE_STONE);
      this.fillRect(68, 10, 3, 3, TILE_STONE);
      this.fillRect(118, 12, 2, 1, TILE_STONE);
      this.fillRect(120, 11, 2, 2, TILE_STONE);
      this.fillRect(122, 10, 2, 3, TILE_STONE);
      this.fillRect(124, 9, 2, 4, TILE_STONE);
      this.fillRect(187, 12, 2, 1, TILE_STONE);
      this.fillRect(189, 11, 2, 2, TILE_STONE);
      this.fillRect(191, 10, 2, 3, TILE_STONE);
      this.fillRect(193, 9, 2, 4, TILE_STONE);

      // Floating brick/surprise courses.
      [8, 9, 10, 14, 15, 16].forEach(x => this.set(x, 9, TILE_BRICK));
      this.set(12, 9, TILE_SURPRISE);
      this.set(13, 5, TILE_SURPRISE);
      this.platform(34, 39, 9, TILE_BRICK);
      this.set(36, 5, TILE_SURPRISE);
      this.set(37, 9, TILE_SURPRISE);
      this.platform(55, 62, 8, TILE_BRICK);
      this.platform(72, 77, 7, TILE_BRICK);
      this.set(74, 7, TILE_SURPRISE);
      this.set(90, 9, TILE_SURPRISE);
      this.platform(92, 96, 9, TILE_BRICK);
      this.platform(110, 115, 7, TILE_BRICK);
      this.set(112, 7, TILE_SURPRISE);
      this.platform(141, 148, 9, TILE_BRICK);
      this.set(145, 5, TILE_SURPRISE);
      this.platform(157, 161, 7, TILE_BRICK);
      this.platform(173, 178, 9, TILE_BRICK);
      this.set(175, 9, TILE_SURPRISE);

      // Pipes and block columns.
      this.pipe(42, 11, 2);
      this.pipe(83, 10, 3);
      this.pipe(101, 11, 2);
      this.pipe(135, 9, 4);
      this.pipe(164, 10, 3);

      // Coins: arcs and breadcrumbs.
      this.coin(6, 8, 4);
      this.coin(13, 4, 1);
      this.coin(31, 10, 4);
      this.coin(35, 7, 5);
      this.coin(54, 11, 6);
      this.coin(59, 6, 5);
      this.coin(73, 5, 5);
      this.coin(85, 8, 4);
      this.coin(91, 6, 6);
      this.coin(109, 5, 7);
      this.coin(126, 8, 5);
      this.coin(139, 7, 9);
      this.coin(155, 5, 8);
      this.coin(171, 7, 8);
      this.coin(184, 6, 10);

      // Enemies and hazards.
      this.enemy(17, 12);
      this.enemy(38, 12);
      this.enemy(58, 12, 'beetle');
      this.enemy(74, 12);
      this.enemy(94, 12, 'beetle');
      this.enemy(113, 12);
      this.enemy(147, 12, 'beetle');
      this.enemy(159, 12);
      this.enemy(177, 12);
      this.spike(67, 12, 3);
      this.spike(128, 12, 3);
      this.spike(152, 12, 2);

      // Decorative parallax anchors.
      for (let i = 0; i < 34; i++) {
        this.decorate.push({
          x: 80 + i * 205 + (i % 3) * 30,
          y: 52 + ((i * 37) % 85),
          s: 0.7 + (i % 4) * 0.18,
          kind: i % 5
        });
      }
    }
    isSolid(tx, ty) { return solidTiles.has(this.tile(tx, ty)); }
    rectTiles(box) {
      const x0 = Math.floor(box.x / TILE);
      const y0 = Math.floor(box.y / TILE);
      const x1 = Math.floor((box.x + box.w - 0.001) / TILE);
      const y1 = Math.floor((box.y + box.h - 0.001) / TILE);
      const out = [];
      for (let y = y0; y <= y1; y++) for (let x = x0; x <= x1; x++) out.push({ x, y, type: this.tile(x, y) });
      return out;
    }
    spawnPowerup(x, y) { this.powerups.push(new Powerup(x + 4, y - 24)); }
    burst(x, y, palette, count = 10, power = 120) {
      for (let i = 0; i < count; i++) {
        const a = -Math.PI + Math.random() * Math.PI * 2;
        const sp = 40 + Math.random() * power;
        this.particles.push({
          x, y,
          vx: Math.cos(a) * sp,
          vy: Math.sin(a) * sp - 80,
          life: 0.45 + Math.random() * 0.42,
          max: 0.9,
          r: 2 + Math.random() * 4,
          c: palette[i % palette.length]
        });
      }
    }
  }

  class Coin {
    constructor(x, y) { this.x = x; this.y = y; this.r = 9; this.collected = false; this.float = Math.random() * 10; }
    get box() { return { x: this.x - 9, y: this.y - 12, w: 18, h: 24 }; }
  }

  class Powerup {
    constructor(x, y) {
      this.x = x; this.y = y; this.w = 24; this.h = 24;
      this.vx = 78; this.vy = -220;
      this.dead = false;
    }
    box() { return { x: this.x, y: this.y, w: this.w, h: this.h }; }
    update(dt, level) {
      this.vy = Math.min(TERMINAL_VY, this.vy + GRAVITY * dt * 0.72);
      moveEntity(this, dt, level, true);
      if (this.y > WORLD_H + 100) this.dead = true;
    }
  }

  class Enemy {
    constructor(x, y, kind = 'snail') {
      this.x = x; this.y = y; this.w = kind === 'beetle' ? 30 : 28; this.h = kind === 'beetle' ? 26 : 24;
      this.vx = kind === 'beetle' ? -72 : -48;
      this.vy = 0;
      this.kind = kind;
      this.alive = true;
      this.squash = 0;
      this.step = Math.random() * Math.PI * 2;
    }
    box() { return { x: this.x, y: this.y, w: this.w, h: this.h }; }
    update(dt, level) {
      if (!this.alive) { this.squash -= dt; return; }
      this.step += dt * 6;
      this.vy = Math.min(TERMINAL_VY, this.vy + GRAVITY * dt);
      const oldX = this.x;
      moveEntity(this, dt, level, true);
      const dir = Math.sign(this.vx) || 1;
      const footX = Math.floor((this.x + (dir > 0 ? this.w + 3 : -3)) / TILE);
      const footY = Math.floor((this.y + this.h + 4) / TILE);
      if (!level.isSolid(footX, footY) || Math.abs(this.x - oldX) < 0.1) this.vx *= -1;
      if (this.y > WORLD_H + 60) this.alive = false;
    }
  }

  function moveEntity(entity, dt, level, reverseOnWall = false) {
    entity.x += entity.vx * dt;
    let box = entity.box ? entity.box() : rect(entity.x, entity.y, entity.w, entity.h);
    for (const t of level.rectTiles(box)) {
      if (!solidTiles.has(t.type)) continue;
      const tb = { x: t.x * TILE, y: t.y * TILE, w: TILE, h: TILE };
      if (!overlap(box, tb)) continue;
      if (entity.vx > 0) entity.x = tb.x - entity.w;
      else if (entity.vx < 0) entity.x = tb.x + tb.w;
      if (reverseOnWall) entity.vx *= -1;
      else entity.vx = 0;
      box = entity.box ? entity.box() : rect(entity.x, entity.y, entity.w, entity.h);
    }

    entity.y += entity.vy * dt;
    box = entity.box ? entity.box() : rect(entity.x, entity.y, entity.w, entity.h);
    entity.onGround = false;
    for (const t of level.rectTiles(box)) {
      if (!solidTiles.has(t.type)) continue;
      const tb = { x: t.x * TILE, y: t.y * TILE, w: TILE, h: TILE };
      if (!overlap(box, tb)) continue;
      if (entity.vy > 0) {
        entity.y = tb.y - entity.h;
        entity.vy = 0;
        entity.onGround = true;
      } else if (entity.vy < 0) {
        entity.y = tb.y + tb.h;
        entity.vy = 0;
      }
      box = entity.box ? entity.box() : rect(entity.x, entity.y, entity.w, entity.h);
    }
  }

  class Player {
    constructor(spawn) {
      this.spawnX = spawn.x;
      this.spawnY = spawn.y;
      this.reset(true);
    }
    reset(full = false) {
      this.x = this.spawnX;
      this.y = this.spawnY;
      this.w = 25;
      this.h = full ? 38 : this.h;
      this.vx = 0;
      this.vy = 0;
      this.big = false;
      this.facing = 1;
      this.onGround = false;
      this.coyote = 0;
      this.jumpBuffer = 0;
      this.invuln = full ? 0 : 1.6;
      this.anim = 0;
      this.squash = 0;
      this.deadTimer = 0;
    }
    box() { return { x: this.x, y: this.y, w: this.w, h: this.h }; }
    center() { return { x: this.x + this.w / 2, y: this.y + this.h / 2 }; }
    grow(level) {
      if (this.big) return;
      this.big = true;
      const oldBottom = this.y + this.h;
      this.h = 50;
      this.w = 28;
      this.y = oldBottom - this.h;
      this.invuln = 0.85;
      level.burst(this.x + this.w / 2, this.y + this.h / 2, ['#fff7a8', '#ff8f4d', '#66d17a'], 22, 170);
    }
    hurt(level) {
      if (this.invuln > 0) return false;
      if (this.big) {
        this.big = false;
        const oldBottom = this.y + this.h;
        this.w = 25;
        this.h = 38;
        this.y = oldBottom - this.h;
        this.vy = -290;
        this.invuln = 1.8;
        level.burst(this.x + this.w / 2, this.y + this.h / 2, ['#9ce6ff', '#fff0b4', '#ff7e79'], 16, 140);
        return false;
      }
      return true;
    }
    update(dt, game) {
      const level = game.level;
      this.invuln = Math.max(0, this.invuln - dt);
      this.anim += dt * (Math.abs(this.vx) > 10 ? Math.abs(this.vx) / 22 : 2.5);
      if (keys.jumpPressed) this.jumpBuffer = 0.12;
      else this.jumpBuffer = Math.max(0, this.jumpBuffer - dt);
      this.coyote = this.onGround ? 0.11 : Math.max(0, this.coyote - dt);

      const run = keys.run;
      const maxSpeed = run ? 325 : 235;
      const accel = this.onGround ? 2350 : 1450;
      const friction = this.onGround ? 1850 : 280;
      let desired = 0;
      if (keys.left) desired -= 1;
      if (keys.right) desired += 1;
      if (desired !== 0) {
        this.vx += desired * accel * dt;
        this.facing = desired;
      } else {
        const f = friction * dt;
        if (Math.abs(this.vx) <= f) this.vx = 0;
        else this.vx -= Math.sign(this.vx) * f;
      }
      this.vx = clamp(this.vx, -maxSpeed, maxSpeed);

      if (this.jumpBuffer > 0 && this.coyote > 0) {
        this.vy = this.big ? -690 : -650;
        this.onGround = false;
        this.coyote = 0;
        this.jumpBuffer = 0;
        this.squash = 0.08;
        level.burst(this.x + this.w / 2, this.y + this.h, ['#f9df9b', '#ffffff'], 8, 80);
      }
      if (keys.jumpReleased && this.vy < -170) this.vy *= 0.52;
      this.vy = Math.min(TERMINAL_VY, this.vy + GRAVITY * dt);

      // Horizontal player movement.
      this.x += this.vx * dt;
      this.x = Math.max(0, this.x);
      let box = this.box();
      for (const t of level.rectTiles(box)) {
        if (!solidTiles.has(t.type)) continue;
        const tb = { x: t.x * TILE, y: t.y * TILE, w: TILE, h: TILE };
        if (!overlap(box, tb)) continue;
        if (this.vx > 0) this.x = tb.x - this.w;
        else if (this.vx < 0) this.x = tb.x + tb.w;
        this.vx = 0;
        box = this.box();
      }

      // Vertical player movement with block bumps.
      this.y += this.vy * dt;
      box = this.box();
      this.onGround = false;
      for (const t of level.rectTiles(box)) {
        if (!solidTiles.has(t.type)) continue;
        const tb = { x: t.x * TILE, y: t.y * TILE, w: TILE, h: TILE };
        if (!overlap(box, tb)) continue;
        if (this.vy > 0) {
          this.y = tb.y - this.h;
          this.vy = 0;
          this.onGround = true;
        } else if (this.vy < 0) {
          this.y = tb.y + tb.h;
          this.vy = 55;
          if (bumpableTiles.has(t.type)) game.bumpBlock(t.x, t.y, t.type);
        }
        box = this.box();
      }

      if (this.y > H + 100) game.loseLife();
    }
  }

  class Game {
    constructor() {
      this.state = 'title';
      this.bestCoins = 0;
      this.reset();
    }
    reset() {
      this.level = new Level();
      this.player = new Player(this.level.spawn);
      this.cameraX = 0;
      this.cameraY = 0;
      this.score = 0;
      this.coins = 0;
      this.lives = 3;
      this.time = 300;
      this.combo = 0;
      this.message = '';
      this.messageTimer = 0;
      this.flash = 0;
      this.winTimer = 0;
      this.state = 'title';
    }
    start() {
      this.level = new Level();
      this.player = new Player(this.level.spawn);
      this.cameraX = 0;
      this.score = 0;
      this.coins = 0;
      this.lives = 3;
      this.time = 300;
      this.combo = 0;
      this.state = 'playing';
      this.message = 'Go!';
      this.messageTimer = 1.1;
    }
    restartFromCheckpoint() {
      this.player = new Player(this.level.spawn);
      this.player.invuln = 1.4;
      this.cameraX = Math.max(0, this.player.x - W * 0.35);
      this.state = 'playing';
      this.message = 'Try again';
      this.messageTimer = 1.0;
    }
    bumpBlock(tx, ty, type) {
      const px = tx * TILE + TILE / 2;
      const py = ty * TILE;
      this.level.burst(px, py, ['#fff3a6', '#ffcc54', '#ff8a4a'], 12, 140);
      if (type === TILE_SURPRISE) {
        this.level.set(tx, ty, TILE_USED);
        if ((tx + ty) % 3 === 0) {
          this.level.spawnPowerup(tx * TILE, ty * TILE);
          this.message = 'Sunfruit!';
          this.messageTimer = 1.0;
        } else {
          this.collectCoin(px, py - 12, 1);
        }
      } else if (type === TILE_BRICK) {
        if (this.player.big) {
          this.level.set(tx, ty, TILE_EMPTY);
          this.score += 50;
        } else {
          this.level.particles.push({ x: px, y: py, vx: 0, vy: -90, life: .25, max: .25, r: 6, c: '#d47b4a' });
        }
      }
    }
    collectCoin(x, y, amount = 1) {
      this.coins += amount;
      this.score += 100 * amount;
      this.level.burst(x, y, ['#fff07a', '#ffbd3f'], 8, 100);
      if (this.coins >= 100) {
        this.coins -= 100;
        this.lives++;
        this.message = '+1 life';
        this.messageTimer = 1.3;
      }
    }
    loseLife() {
      if (this.state !== 'playing') return;
      this.lives--;
      this.flash = 0.4;
      if (this.lives <= 0) {
        this.state = 'gameover';
        this.message = 'Game Over';
        this.messageTimer = 999;
      } else {
        this.restartFromCheckpoint();
      }
    }
    win() {
      if (this.state !== 'playing') return;
      this.state = 'win';
      this.winTimer = 0;
      this.score += Math.max(0, Math.floor(this.time)) * 10;
      this.bestCoins = Math.max(this.bestCoins, this.coins);
      this.level.burst(this.player.x + 16, this.player.y + 12, ['#fff5a8', '#7ee07c', '#78d9ff', '#ff8ac2'], 52, 260);
    }
    update(dt) {
      if (this.state !== 'playing') {
        if (this.state === 'win') this.winTimer += dt;
        this.updateAmbient(dt);
        return;
      }
      this.time -= dt;
      if (this.time <= 0) this.loseLife();
      this.messageTimer = Math.max(0, this.messageTimer - dt);
      this.flash = Math.max(0, this.flash - dt);

      this.player.update(dt, this);

      const pBox = this.player.box();
      for (const coin of this.level.coins) {
        if (!coin.collected && overlap(pBox, coin.box)) {
          coin.collected = true;
          this.collectCoin(coin.x, coin.y, 1);
        }
      }
      for (const power of this.level.powerups) {
        if (!power.dead) {
          power.update(dt, this.level);
          if (overlap(pBox, power.box())) {
            power.dead = true;
            this.player.grow(this.level);
            this.score += 1000;
          }
        }
      }
      this.level.powerups = this.level.powerups.filter(p => !p.dead);

      for (const enemy of this.level.enemies) {
        enemy.update(dt, this.level);
        if (!enemy.alive) continue;
        const eBox = enemy.box();
        if (overlap(pBox, eBox)) {
          const playerBottom = this.player.y + this.player.h;
          const stomp = this.player.vy > 80 && playerBottom - enemy.y < 19;
          if (stomp) {
            enemy.alive = false;
            enemy.squash = 0.45;
            this.player.vy = keys.jump ? -520 : -390;
            this.combo++;
            const pts = 200 * this.combo;
            this.score += pts;
            this.message = `+${pts}`;
            this.messageTimer = 0.55;
            this.level.burst(enemy.x + enemy.w / 2, enemy.y + enemy.h / 2, ['#e9c28f', '#fff2aa', '#8bd47f'], 16, 160);
          } else {
            this.combo = 0;
            if (this.player.hurt(this.level)) this.loseLife();
          }
        }
      }

      for (const h of this.level.hazards) {
        if (overlap(pBox, h)) {
          if (this.player.hurt(this.level)) this.loseLife();
          break;
        }
      }

      if (overlap(pBox, this.level.flag) || this.player.x > this.level.flag.x + 30) this.win();

      this.updateAmbient(dt);
      this.cameraX = clamp(lerp(this.cameraX, this.player.x + this.player.w / 2 - W * 0.42, 0.12), 0, WORLD_W - W);
    }
    updateAmbient(dt) {
      for (const p of this.level.particles) {
        p.life -= dt;
        p.x += p.vx * dt;
        p.y += p.vy * dt;
        p.vy += GRAVITY * 0.68 * dt;
      }
      this.level.particles = this.level.particles.filter(p => p.life > 0);
      for (const coin of this.level.coins) coin.float += dt * 3.2;
    }
  }

  function setInput(name, value) {
    if (name === 'left') keys.left = value;
    if (name === 'right') keys.right = value;
    if (name === 'run') keys.run = value;
    if (name === 'jump') {
      if (value && !keys.jump) keys.jumpPressed = true;
      if (!value && keys.jump) keys.jumpReleased = true;
      keys.jump = value;
    }
  }

  window.addEventListener('keydown', (e) => {
    const key = e.key.toLowerCase();
    if (['arrowleft','arrowright','arrowup','arrowdown',' ', 'w','a','s','d','shift'].includes(key)) e.preventDefault();
    if (pressedPhysical.has(e.code)) return;
    pressedPhysical.add(e.code);
    if (key === 'arrowleft' || key === 'a') setInput('left', true);
    if (key === 'arrowright' || key === 'd') setInput('right', true);
    if (key === 'arrowup' || key === 'w' || key === ' ') setInput('jump', true);
    if (key === 'shift') setInput('run', true);
    if (key === 'enter') {
      if (game.state === 'title' || game.state === 'gameover' || game.state === 'win') game.start();
    }
    if (key === 'p') {
      if (game.state === 'playing') game.state = 'paused';
      else if (game.state === 'paused') game.state = 'playing';
    }
    if (key === 'r') game.start();
  }, { passive: false });

  window.addEventListener('keyup', (e) => {
    const key = e.key.toLowerCase();
    pressedPhysical.delete(e.code);
    if (key === 'arrowleft' || key === 'a') setInput('left', false);
    if (key === 'arrowright' || key === 'd') setInput('right', false);
    if (key === 'arrowup' || key === 'w' || key === ' ') setInput('jump', false);
    if (key === 'shift') setInput('run', false);
  });

  document.querySelectorAll('.touch-btn').forEach(btn => {
    const key = btn.dataset.key;
    const down = (e) => { e.preventDefault(); btn.classList.add('down'); setInput(key, true); if (game.state !== 'playing') game.start(); };
    const up = (e) => { e.preventDefault(); btn.classList.remove('down'); setInput(key, false); };
    btn.addEventListener('pointerdown', down);
    btn.addEventListener('pointerup', up);
    btn.addEventListener('pointercancel', up);
    btn.addEventListener('pointerleave', up);
  });
  canvas.addEventListener('pointerdown', () => { if (game.state !== 'playing') game.start(); });

  function draw() {
    ctx.clearRect(0, 0, W, H);
    drawSky();
    const cam = game.cameraX;
    ctx.save();
    ctx.translate(-Math.round(cam), 0);
    drawDistantDecor(cam);
    drawLevel(game.level, cam);
    drawEntities(game.level, cam);
    drawPlayer(game.player);
    drawFlag(game.level.flag);
    ctx.restore();
    drawHUD();
    if (game.flash > 0) {
      ctx.fillStyle = `rgba(255, 80, 90, ${game.flash * 0.8})`;
      ctx.fillRect(0, 0, W, H);
    }
    drawOverlay();
  }

  function drawSky() {
    const g = ctx.createLinearGradient(0, 0, 0, H);
    g.addColorStop(0, '#75d6ff');
    g.addColorStop(0.54, '#c7f2ff');
    g.addColorStop(1, '#ffe0a3');
    ctx.fillStyle = g;
    ctx.fillRect(0, 0, W, H);

    ctx.globalAlpha = 0.45;
    for (let i = 0; i < 11; i++) {
      const x = (i * 151 - (game.cameraX * 0.12) % 151) - 70;
      const y = 34 + (i % 4) * 31;
      drawCloud(x, y, 0.7 + (i % 3) * 0.18, '#fff7dc');
    }
    ctx.globalAlpha = 1;

    // Mountains.
    ctx.save();
    ctx.translate(-(game.cameraX * 0.18) % 480, 0);
    for (let x = -480; x < W + 480; x += 240) {
      ctx.fillStyle = '#78b87b';
      tri(x + 40, 368, x + 170, 130, x + 300, 368);
      ctx.fillStyle = '#5fa46b';
      tri(x + 210, 368, x + 340, 180, x + 470, 368);
      ctx.fillStyle = 'rgba(255,247,219,.55)';
      tri(x + 170, 130, x + 142, 183, x + 198, 183);
    }
    ctx.restore();

    // Warm horizon haze.
    const h = ctx.createLinearGradient(0, H - 145, 0, H);
    h.addColorStop(0, 'rgba(255, 225, 158, 0)');
    h.addColorStop(1, 'rgba(255, 182, 93, 0.42)');
    ctx.fillStyle = h;
    ctx.fillRect(0, H - 170, W, 170);
  }

  function drawDistantDecor(cam) {
    // Painted bushes behind terrain.
    for (let x = Math.floor((cam - 200) / 180) * 180; x < cam + W + 260; x += 180) {
      ctx.fillStyle = '#61bc67';
      roundedRect(x, 405, 120, 44, 22, true);
      ctx.fillStyle = '#76d874';
      circle(x + 20, 404, 24); circle(x + 53, 392, 34); circle(x + 87, 408, 28);
      ctx.fillStyle = 'rgba(34, 99, 58, .22)';
      ctx.fillRect(x + 7, 428, 106, 7);
    }
  }

  function drawLevel(level, cam) {
    const x0 = Math.max(0, Math.floor(cam / TILE) - 2);
    const x1 = Math.min(level.width - 1, Math.floor((cam + W) / TILE) + 2);
    for (let y = 0; y < level.height; y++) {
      for (let x = x0; x <= x1; x++) {
        const t = level.tile(x, y);
        if (t !== TILE_EMPTY) drawTile(t, x * TILE, y * TILE, x, y);
      }
    }
    // Hazards.
    for (const h of level.hazards) drawSpike(h.x, h.y);
    // Particles.
    for (const p of level.particles) {
      ctx.globalAlpha = Math.max(0, p.life / p.max);
      ctx.fillStyle = p.c;
      circle(p.x, p.y, p.r);
    }
    ctx.globalAlpha = 1;
  }

  function drawTile(t, x, y, tx, ty) {
    if (t === TILE_DIRT || t === TILE_GRASS) {
      ctx.fillStyle = t === TILE_GRASS ? '#4fbd5f' : '#a6643a';
      ctx.fillRect(x, y, TILE, TILE);
      ctx.fillStyle = t === TILE_GRASS ? '#79e06e' : '#c9854f';
      ctx.fillRect(x, y, TILE, t === TILE_GRASS ? 9 : 3);
      ctx.fillStyle = 'rgba(72, 43, 36, .28)';
      for (let i = 0; i < 3; i++) {
        const px = x + ((tx * 17 + ty * 7 + i * 11) % 28);
        const py = y + 13 + ((tx * 5 + ty * 19 + i * 9) % 15);
        ctx.fillRect(px, py, 7, 3);
      }
      if (t === TILE_GRASS) {
        ctx.fillStyle = '#307f45';
        ctx.fillRect(x, y + 27, TILE, 5);
      }
    } else if (t === TILE_BRICK) {
      ctx.fillStyle = '#c56c43';
      ctx.fillRect(x + 1, y + 1, TILE - 2, TILE - 2);
      ctx.fillStyle = '#e58b55';
      ctx.fillRect(x + 2, y + 2, TILE - 4, 5);
      ctx.fillStyle = '#7e3f34';
      ctx.fillRect(x, y + 14, TILE, 3);
      ctx.fillRect(x + 14, y, 3, 14);
      ctx.fillRect(x + 7, y + 17, 3, 15);
      ctx.strokeStyle = '#512b2d';
      ctx.strokeRect(x + 1.5, y + 1.5, TILE - 3, TILE - 3);
    } else if (t === TILE_SURPRISE || t === TILE_USED) {
      ctx.fillStyle = t === TILE_SURPRISE ? '#ffc84c' : '#b77f4c';
      ctx.fillRect(x + 2, y + 2, TILE - 4, TILE - 4);
      ctx.fillStyle = t === TILE_SURPRISE ? '#fff29a' : '#d0a26d';
      ctx.fillRect(x + 5, y + 4, TILE - 10, 5);
      ctx.strokeStyle = '#70412c';
      ctx.lineWidth = 3;
      ctx.strokeRect(x + 2.5, y + 2.5, TILE - 5, TILE - 5);
      ctx.fillStyle = t === TILE_SURPRISE ? '#7f4a2a' : '#8a684e';
      if (t === TILE_SURPRISE) {
        ctx.font = 'bold 24px ui-rounded, sans-serif';
        ctx.textAlign = 'center';
        ctx.textBaseline = 'middle';
        ctx.fillText('?', x + 16, y + 17);
      } else {
        ctx.fillRect(x + 9, y + 14, 14, 5);
      }
      ctx.lineWidth = 1;
    } else if (t === TILE_PIPE_L || t === TILE_PIPE_R) {
      const left = t === TILE_PIPE_L;
      ctx.fillStyle = left ? '#2e9b55' : '#36b965';
      ctx.fillRect(x, y, TILE, TILE);
      ctx.fillStyle = left ? '#62dc78' : '#88ef8b';
      ctx.fillRect(x + (left ? 5 : 0), y + 2, 8, TILE - 4);
      ctx.strokeStyle = '#165a3a';
      ctx.lineWidth = 3;
      ctx.strokeRect(x + 1.5, y + 1.5, TILE - 3, TILE - 3);
      ctx.lineWidth = 1;
    } else if (t === TILE_STONE) {
      ctx.fillStyle = '#8c86a5';
      ctx.fillRect(x + 1, y + 1, TILE - 2, TILE - 2);
      ctx.fillStyle = '#b0a9c5';
      ctx.fillRect(x + 3, y + 3, TILE - 6, 4);
      ctx.strokeStyle = '#514a68';
      ctx.strokeRect(x + 1.5, y + 1.5, TILE - 3, TILE - 3);
    }
  }

  function drawEntities(level) {
    for (const coin of level.coins) if (!coin.collected) drawCoin(coin);
    for (const power of level.powerups) drawPowerup(power);
    for (const enemy of level.enemies) drawEnemy(enemy);
  }

  function drawCoin(coin) {
    const bob = Math.sin(coin.float) * 3;
    ctx.save();
    ctx.translate(coin.x, coin.y + bob);
    ctx.fillStyle = '#ffcd4c';
    ellipse(0, 0, 15, 22);
    ctx.fillStyle = '#fff18a';
    ellipse(-3, -3, 6, 13);
    ctx.strokeStyle = '#b96d2c';
    ctx.lineWidth = 2;
    ctx.stroke();
    ctx.restore();
  }

  function drawPowerup(p) {
    ctx.save();
    ctx.translate(p.x + p.w / 2, p.y + p.h / 2);
    ctx.rotate(Math.sin(performance.now() / 170) * 0.08);
    ctx.fillStyle = '#ff7b45';
    roundedRect(-12, -10, 24, 20, 8, true);
    ctx.fillStyle = '#fff18e';
    circle(-5, -4, 4); circle(5, 1, 3); circle(0, 5, 3);
    ctx.fillStyle = '#4bbd69';
    roundedRect(-8, 3, 16, 12, 5, true);
    ctx.restore();
  }

  function drawEnemy(e) {
    if (!e.alive && e.squash <= 0) return;
    ctx.save();
    ctx.translate(e.x, e.y);
    if (!e.alive) ctx.scale(1, 0.38);
    if (e.kind === 'beetle') {
      ctx.fillStyle = '#5a4c8a';
      roundedRect(1, 5, e.w - 2, e.h - 6, 10, true);
      ctx.fillStyle = '#8b76d8';
      roundedRect(4, 2, e.w - 8, 12, 8, true);
      ctx.fillStyle = '#241d3d';
      circle(9, 13, 2.5); circle(21, 13, 2.5);
    } else {
      ctx.fillStyle = '#d7904e';
      roundedRect(0, 8, e.w, e.h - 8, 9, true);
      ctx.fillStyle = '#f1c067';
      roundedRect(6, 2, e.w - 9, 14, 8, true);
      ctx.fillStyle = '#26213a';
      circle(9, 12, 2.3); circle(20, 12, 2.3);
      ctx.strokeStyle = '#80512e';
      ctx.beginPath(); ctx.moveTo(8, 2); ctx.quadraticCurveTo(13, -5, 17, 2); ctx.stroke();
    }
    ctx.restore();
  }

  function drawPlayer(p) {
    const blink = p.invuln > 0 && Math.floor(performance.now() / 80) % 2 === 0;
    if (blink) return;
    const foot = Math.sin(p.anim) * (p.onGround ? 3 : 1);
    const squash = p.squash > 0 ? 1 + p.squash * 2 : 1;
    p.squash = Math.max(0, p.squash - 0.016);
    ctx.save();
    ctx.translate(p.x + p.w / 2, p.y + p.h);
    ctx.scale(p.facing, 1 / squash);
    // Shadow.
    ctx.fillStyle = 'rgba(22, 16, 33, .28)';
    ellipse(0, 4, p.w * 0.9, 7);
    // Boots.
    ctx.fillStyle = '#5b392b';
    roundedRect(-p.w/2 + 1, -6 + foot, 11, 7, 3, true);
    roundedRect(p.w/2 - 12, -6 - foot, 11, 7, 3, true);
    // Overalls/body.
    ctx.fillStyle = '#2461bb';
    roundedRect(-p.w/2 + 3, -p.h + 17, p.w - 6, p.h - 20, 5, true);
    ctx.fillStyle = '#ff5f45';
    roundedRect(-p.w/2 + 2, -p.h + 8, p.w - 4, 20, 8, true);
    // Face.
    ctx.fillStyle = '#ffd29a';
    roundedRect(-p.w/2 + 4, -p.h + 1, p.w - 6, 18, 8, true);
    ctx.fillStyle = '#30203e';
    circle(5, -p.h + 9, 2.2);
    // Cap / leaf crest.
    ctx.fillStyle = '#e6463b';
    roundedRect(-p.w/2 + 1, -p.h - 3, p.w, 10, 5, true);
    ctx.fillStyle = '#55c76d';
    ctx.beginPath();
    ctx.moveTo(-1, -p.h - 2); ctx.quadraticCurveTo(8, -p.h - 18, 16, -p.h - 5); ctx.quadraticCurveTo(8, -p.h - 6, -1, -p.h - 2); ctx.fill();
    if (p.big) {
      ctx.fillStyle = 'rgba(255, 244, 150, .78)';
      circle(-8, -p.h + 22, 3); circle(8, -p.h + 29, 2.5);
    }
    ctx.restore();
  }

  function drawFlag(f) {
    ctx.fillStyle = '#594337';
    ctx.fillRect(f.x + 18, f.y - 6, 6, f.h + 18);
    ctx.fillStyle = '#fff6bd';
    circle(f.x + 21, f.y - 8, 8);
    ctx.fillStyle = '#59c96e';
    ctx.beginPath();
    ctx.moveTo(f.x + 23, f.y + 4);
    ctx.lineTo(f.x + 82, f.y + 22);
    ctx.lineTo(f.x + 23, f.y + 43);
    ctx.closePath();
    ctx.fill();
    ctx.fillStyle = '#fff39c';
    star(f.x + 45, f.y + 24, 10, 5, 5);
  }

  function drawHUD() {
    ctx.save();
    ctx.fillStyle = 'rgba(21, 18, 39, .50)';
    roundedRect(18, 15, 924, 50, 16, true);
    ctx.strokeStyle = 'rgba(255, 245, 191, .45)';
    ctx.strokeRect(24.5, 21.5, 911, 37);
    ctx.fillStyle = '#fff8ce';
    ctx.font = 'bold 20px ui-rounded, Trebuchet MS, sans-serif';
    ctx.textBaseline = 'middle';
    ctx.textAlign = 'left';
    ctx.fillText(`OCHKO ${String(game.score).padStart(6, '0')}`, 40, 41);
    ctx.fillText(`COINS ${String(game.coins).padStart(2, '0')}`, 268, 41);
    ctx.fillText(`LIVES ${game.lives}`, 448, 41);
    ctx.fillText(`TIME ${Math.max(0, Math.ceil(game.time))}`, 585, 41);
    const progress = clamp((game.player.x || 0) / (game.level.flag.x || WORLD_W), 0, 1);
    ctx.fillStyle = 'rgba(255,255,255,.22)';
    roundedRect(742, 33, 166, 15, 8, true);
    ctx.fillStyle = '#67d86b';
    roundedRect(744, 35, 162 * progress, 11, 7, true);
    ctx.fillStyle = '#fff6bc';
    circle(744 + 162 * progress, 40, 8);
    ctx.restore();
  }

  function drawOverlay() {
    if (game.messageTimer > 0 && game.state === 'playing') {
      ctx.save();
      ctx.globalAlpha = Math.min(1, game.messageTimer * 2);
      ctx.fillStyle = 'rgba(31, 20, 45, .64)';
      roundedRect(W / 2 - 120, 86, 240, 48, 18, true);
      ctx.fillStyle = '#fff4b2';
      ctx.font = '900 27px ui-rounded, Trebuchet MS, sans-serif';
      ctx.textAlign = 'center'; ctx.textBaseline = 'middle';
      ctx.fillText(game.message, W / 2, 110);
      ctx.restore();
    }
    if (game.state === 'title') drawTitleCard('SPROUT RUNNER', 'Tangalarni yig‘ing va marraga yeting', 'Enter bosing yoki ekranga teging');
    if (game.state === 'paused') drawTitleCard('TANAFFUS', 'O‘yin to‘xtatildi', 'Davom etish uchun P bosing');
    if (game.state === 'gameover') drawTitleCard('O‘YIN TUGADI', `Ochko ${game.score}`, 'Qayta o‘ynash uchun Enter yoki R');
    if (game.state === 'win') drawTitleCard('MARRAGA YETDINGIZ!', `Ochko ${game.score} · Tanga ${game.coins}`, 'Yangi o‘yin uchun Enter bosing');
  }

  function drawTitleCard(title, subtitle, prompt) {
    ctx.save();
    ctx.fillStyle = 'rgba(12, 10, 24, .52)';
    ctx.fillRect(0, 0, W, H);
    ctx.translate(W / 2, H / 2);
    ctx.fillStyle = 'rgba(255, 239, 176, .22)';
    roundedRect(-286, -124, 572, 240, 28, true);
    ctx.strokeStyle = 'rgba(255, 249, 211, .72)';
    ctx.lineWidth = 4;
    roundedRect(-276, -114, 552, 220, 22, false);
    ctx.fillStyle = '#fff1a8';
    ctx.font = '900 55px ui-rounded, Trebuchet MS, sans-serif';
    ctx.textAlign = 'center'; ctx.textBaseline = 'middle';
    ctx.shadowColor = 'rgba(0,0,0,.45)'; ctx.shadowBlur = 0; ctx.shadowOffsetY = 5;
    ctx.fillText(title, 0, -50);
    ctx.shadowOffsetY = 0;
    ctx.font = '700 22px ui-rounded, Trebuchet MS, sans-serif';
    ctx.fillStyle = '#ddffcf';
    ctx.fillText(subtitle, 0, 9);
    ctx.font = '900 24px ui-rounded, Trebuchet MS, sans-serif';
    ctx.fillStyle = '#ffffff';
    if (Math.floor(performance.now() / 420) % 2 === 0) ctx.fillText(prompt, 0, 66);
    ctx.restore();
  }

  // Drawing helpers.
  function tri(x1, y1, x2, y2, x3, y3) { ctx.beginPath(); ctx.moveTo(x1,y1); ctx.lineTo(x2,y2); ctx.lineTo(x3,y3); ctx.closePath(); ctx.fill(); }
  function circle(x, y, r) { ctx.beginPath(); ctx.arc(x, y, r, 0, Math.PI * 2); ctx.fill(); }
  function ellipse(x, y, rx, ry) { ctx.beginPath(); ctx.ellipse(x, y, rx, ry, 0, 0, Math.PI * 2); ctx.fill(); }
  function roundedRect(x, y, w, h, r, fill) {
    r = Math.min(r, Math.abs(w) / 2, Math.abs(h) / 2);
    ctx.beginPath();
    ctx.moveTo(x + r, y);
    ctx.arcTo(x + w, y, x + w, y + h, r);
    ctx.arcTo(x + w, y + h, x, y + h, r);
    ctx.arcTo(x, y + h, x, y, r);
    ctx.arcTo(x, y, x + w, y, r);
    if (fill) ctx.fill(); else ctx.stroke();
  }
  function drawCloud(x, y, s, c) {
    ctx.fillStyle = c;
    circle(x, y + 18 * s, 17 * s);
    circle(x + 18 * s, y + 8 * s, 22 * s);
    circle(x + 43 * s, y + 15 * s, 18 * s);
    roundedRect(x - 8 * s, y + 18 * s, 64 * s, 18 * s, 8 * s, true);
  }
  function drawSpike(x, y) {
    ctx.fillStyle = '#59486a';
    tri(x, y + 24, x + 14, y, x + 28, y + 24);
    ctx.fillStyle = '#c8c0d9';
    tri(x + 8, y + 17, x + 14, y + 5, x + 20, y + 17);
  }
  function star(cx, cy, outer, inner, points) {
    ctx.beginPath();
    for (let i = 0; i < points * 2; i++) {
      const a = -Math.PI / 2 + i * Math.PI / points;
      const r = i % 2 === 0 ? outer : inner;
      const x = cx + Math.cos(a) * r;
      const y = cy + Math.sin(a) * r;
      if (i === 0) ctx.moveTo(x, y); else ctx.lineTo(x, y);
    }
    ctx.closePath();
    ctx.fill();
  }

  let bestScore = Number(window.arcadeStorage.getItem("best") || 0);

  function frame(now) {
    const dt = Math.min(0.05, (now - last) / 1000);
    last = now;
    accumulator += dt;
    while (accumulator >= STEP) {
      game.update(STEP);
      keys.jumpPressed = false;
      keys.jumpReleased = false;
      accumulator -= STEP;
    }
    if (game.score > bestScore) {
      bestScore = game.score;
      window.arcadeStorage.setItem("best", bestScore);
    }
    draw();
    requestAnimationFrame(frame);
  }

  game = new Game();

  requestAnimationFrame(frame);
})();
