// ============================================================
// CONFIG / MATH
// ============================================================
const W = 384, H = 224, GROUND_BASE = 176; // logical gameplay coordinates
const HQ_ASSET_SCALE = 6;
const MAX_RENDER_SCALE = 6;
const FIXED_DT = 1 / 60;
const clamp = (v,a,b) => Math.max(a, Math.min(b, v));
const aabb = (a,b) => a.x < b.x + b.w && a.x + a.w > b.x && a.y < b.y + b.h && a.y + a.h > b.y;
const now = () => performance.now() / 1000;
const rand = (a,b) => a + Math.random() * (b-a);
const choice = arr => arr[Math.floor(Math.random()*arr.length)];

const canvas = document.getElementById('game');
canvas.tabIndex = 0;
const ctx = canvas.getContext('2d');
ctx.imageSmoothingEnabled = false;

// ============================================================
// INPUT
// ============================================================
const keys = Object.create(null);
const just = Object.create(null);
const downKeys = new Set(['arrowdown','s']);
const prevent = new Set(['arrowup','arrowdown','arrowleft','arrowright',' ','z','x','c','j','k','l','enter','w','a','s','d','o','escape','f1']);
function normKey(e){
  let k=(e.key||'').toLowerCase();
  if (k===' '||k==='spacebar') k=' ';
  if (e.code==='ArrowRight'||k==='arrowright') k='arrowright';
  if (e.code==='ArrowLeft'||k==='arrowleft') k='arrowleft';
  if (e.code==='ArrowUp'||k==='arrowup') k='arrowup';
  if (e.code==='ArrowDown'||k==='arrowdown') k='arrowdown';
  if (e.code==='Enter'||e.code==='NumpadEnter'||k==='enter') k='enter';
  if (e.code==='KeyZ') k='z'; if (e.code==='KeyX') k='x'; if (e.code==='KeyC') k='c';
  if (e.code==='KeyJ') k='j'; if (e.code==='KeyK') k='k'; if (e.code==='KeyL') k='l';
  if (e.code==='KeyA') k='a'; if (e.code==='KeyD') k='d'; if (e.code==='KeyW') k='w'; if (e.code==='KeyS') k='s';
  if (e.code==='KeyM') k='m';
  if (e.code==='KeyO') k='o';
  return k;
}
window.addEventListener('keydown', e => {
  const k=normKey(e);
  if (prevent.has(k) || k===' ') e.preventDefault();
  if (!k) return;
  if (!keys[k]) just[k]=true;
  keys[k]=true;
  // Start the local soundtrack inside the real keyboard gesture so browser
  // autoplay policies cannot reject the first briefing transition.
  if (k==='enter' || k==='z' || k==='j' || k==='o') {
    try { if (State.mode==='TITLE' || State.mode==='BRIEF') Music.play(); } catch (err) {}
  }
}, {passive:false});
window.addEventListener('keyup', e => { const k=normKey(e); if(k) keys[k]=false; }, {passive:false});
function menuConfirmPulse(){ just['enter']=true; }
canvas.addEventListener('pointerdown', ()=>{
  canvas.focus({preventScroll:true});
  // Pointer input advances menus, but clicking to refocus during play must not pause.
  const runtime = window.RUSTED_CANNON || window.CANNON_DEBT;
  const mode = runtime && runtime.getState ? runtime.getState().mode : 'TITLE';
  if (mode!=='PLAY') { menuConfirmPulse(); try { Music.play(); } catch (err) {} }
}, {passive:true});
const virtualPointers=new Map();
function virtualPress(key,pointerId='virtual') {
  if (!keys[key]) just[key]=true;
  keys[key]=true;
  virtualPointers.set(pointerId,key);
  if (key==='z' || key==='enter') {
    try { if (State.mode==='TITLE' || State.mode==='BRIEF') Music.play(); } catch (err) {}
  }
}
function virtualRelease(pointerId='virtual') {
  const key=virtualPointers.get(pointerId);
  if (!key) return;
  virtualPointers.delete(pointerId);
  if (![...virtualPointers.values()].includes(key)) keys[key]=false;
}
function releaseAllVirtualInputs() {
  for (const key of new Set(virtualPointers.values())) keys[key]=false;
  virtualPointers.clear();
}
function initTouchControls() {
  const buttons=document.querySelectorAll ? [...document.querySelectorAll('.touch-button[data-key]')] : [];
  for (const button of buttons) {
    const release=e=>{
      e.preventDefault(); e.stopPropagation();
      virtualRelease(e.pointerId); button.classList.remove('active');
    };
    button.addEventListener('pointerdown',e=>{
      e.preventDefault(); e.stopPropagation();
      button.setPointerCapture?.(e.pointerId);
      virtualPress(button.dataset.key,e.pointerId); button.classList.add('active');
      canvas.focus({preventScroll:true});
    },{passive:false});
    button.addEventListener('pointerup',release,{passive:false});
    button.addEventListener('pointercancel',release,{passive:false});
    button.addEventListener('lostpointercapture',release,{passive:false});
    button.addEventListener('contextmenu',e=>e.preventDefault());
  }
}
window.addEventListener('blur', () => {
  for (const k in keys) keys[k]=false;
  for (const k in just) delete just[k];
  releaseAllVirtualInputs();
});
document.addEventListener('visibilitychange',()=>{ if (document.hidden) releaseAllVirtualInputs(); });
const pressed = k => !!keys[k];
const tapped = k => !!just[k];
const clearJust = () => { for (const k in just) delete just[k]; };
const moveX = () => (pressed('arrowleft')||pressed('a')?-1:0) + (pressed('arrowright')||pressed('d')?1:0);
const shootHeld = () => pressed('z')||pressed('j');
const shootTap = () => tapped('z')||tapped('j');
const jumpTap = () => tapped('x')||tapped('k')||tapped(' ');
const grenadeTap = () => tapped('c')||tapped('l');
const crouchHeld = () => pressed('arrowdown')||pressed('s');

// ============================================================
// AUDIO (WebAudio SFX + TEMP VO captions)
// ============================================================
const AudioFX = (() => {
  let ac = null, muted = false, master = null, limiter = null, level = 0.75;
  // The previous natural voice clip clashed with the 16-bit presentation.
  // POW rescues now rely on the on-screen caption only.
  const rescueVoicePath = null;
  function ensure() {
    try {
      if (!ac) {
        const AC = window.AudioContext || window.webkitAudioContext;
        if (!AC) return null;
        ac = new AC();
        master = ac.createGain();
        master.gain.value = 0.30 * level;
        // Short overlapping gunshots used to sum harshly in captures. A
        // conservative WebAudio limiter keeps the arcade punch without clips.
        if (typeof ac.createDynamicsCompressor === 'function') {
          limiter = ac.createDynamicsCompressor();
          limiter.threshold.value = -12;
          limiter.knee.value = 10;
          limiter.ratio.value = 8;
          limiter.attack.value = 0.003;
          limiter.release.value = 0.18;
          master.connect(limiter); limiter.connect(ac.destination);
        } else master.connect(ac.destination);
      }
      if (ac && ac.state === 'suspended') { try { ac.resume(); } catch (e) {} }
      return ac;
    } catch (e) { return null; }
  }
  function beep(freq, dur, type='square', vol=0.4, slide=0) {
    if (muted) return;
    const a = ensure(); if (!a || !master) return;
    const o = a.createOscillator();
    const g = a.createGain();
    o.type = type;
    o.frequency.setValueAtTime(freq, a.currentTime);
    if (slide) o.frequency.exponentialRampToValueAtTime(Math.max(40, freq+slide), a.currentTime+dur);
    g.gain.setValueAtTime(vol, a.currentTime);
    g.gain.exponentialRampToValueAtTime(0.001, a.currentTime+dur);
    o.connect(g); g.connect(master);
    o.start(); o.stop(a.currentTime+dur);
  }
  function noise(dur=0.12, vol=0.3) {
    if (muted) return;
    const a = ensure(); if (!a || !master) return;
    const n = a.createBuffer(1, a.sampleRate*dur, a.sampleRate);
    const d = n.getChannelData(0);
    for (let i=0;i<d.length;i++) d[i] = (Math.random()*2-1) * (1-i/d.length);
    const s = a.createBufferSource(); s.buffer = n;
    const g = a.createGain(); g.gain.value = vol;
    s.connect(g); g.connect(master); s.start();
  }
  return {
    ensure, rescueVoicePath,
    toggleMute() {
      muted = !muted;
      try { Music.setMuted(muted); } catch (e) {}
      return muted;
    },
    isMuted() { return muted; },
    setLevel(value) {
      level=Math.max(0,Math.min(1,Number(value)||0));
      if (master) master.gain.value=0.30*level;
      return level;
    },
    getLevel() { return level; },
    ui() { beep(660, 0.05, 'square', 0.25); },
    shoot() { beep(880, 0.05, 'square', 0.2, -400); },
    heavy() { beep(220, 0.08, 'sawtooth', 0.25, -80); noise(0.05, 0.15); },
    rocket() { beep(140, 0.16, 'sawtooth', 0.3, 220); noise(0.1, 0.2); },
    jump() { beep(420, 0.08, 'square', 0.15, 180); },
    land() { beep(120, 0.05, 'triangle', 0.15); },
    grenade() { beep(300, 0.1, 'triangle', 0.18, -100); },
    explode(intensity=1) {
      const power=clamp(Number(intensity)||1,0.65,1.45);
      noise(0.18+power*0.05, 0.30+power*0.10);
      beep(96-power*8, 0.16+power*0.05, 'sawtooth', 0.22+power*0.06, -45);
      try { Music.duck(power>1.15?0.66:0.76,power>1.15?0.38:0.24); } catch (e) {}
    },
    rescue() { return false; },
    hurt() { beep(160,0.1,'sawtooth',0.25,-40); },
    die() { beep(110,0.25,'sawtooth',0.3,-70); noise(0.2,0.25); },
    pickup() { beep(640,0.06,'square',0.2); beep(960,0.08,'square',0.18); },
    engine() { beep(70,0.05,'sawtooth',0.05); },
    alarm() { beep(540,0.12,'square',0.2); setTimeout(()=>beep(360,0.12,'square',0.2),140); },
    clear() { beep(523,0.1,'square',0.2); setTimeout(()=>beep(659,0.1,'square',0.2),100); setTimeout(()=>beep(784,0.16,'square',0.22),200); }
  };
})();

const Music = (() => {
  const TRACKS = [
    'assets/audio/stage-1-superhero-cc0.ogg',
    'assets/audio/stage-2-jungle-battle-cc0.ogg',
    'assets/audio/stage-3-oldschool-action-cc0.ogg'
  ];
  // Offline loudness analysis: -22.0, -21.4 and -21.7 dB mean. These trims
  // keep all three stages subjectively level instead of making the jungle
  // track jump forward in social-video captures.
  const TRACK_TRIMS = [1, 0.93, 0.97];
  const BUS_GAIN = 0.20;
  let track = null;
  let trackIndex = -1;
  let muted = false;
  let desired = false;
  let paused = false;
  let level = 0.55;
  let fade = 1;
  let duckGain = 1;
  let duckHold = 0;

  function applyVolume() {
    if (!track) return;
    const trim=TRACK_TRIMS[trackIndex]??1;
    track.volume=clamp(BUS_GAIN*level*trim*fade*duckGain,0,1);
  }

  function ensure(mission=0) {
    const index=Math.max(0,Math.min(TRACKS.length-1,Number.isFinite(mission)?mission:0));
    if (track && trackIndex===index) return track;
    if (typeof Audio === 'undefined') return null;
    try {
      if (track) { try { track.pause(); } catch (e) {} }
      track = new Audio(TRACKS[index]);
      trackIndex=index;
      track.loop = true;
      track.preload = 'auto';
      fade = 0;
      duckGain = 1;
      duckHold = 0;
      applyVolume();
      track.muted = muted;
      return track;
    } catch (e) { return null; }
  }

  function play(mission=(typeof State!=='undefined'?State.mission:0)) {
    desired = true;
    paused = false;
    const audio = ensure(mission);
    if (!audio || muted) return;
    audio.muted = false;
    try {
      const promise = audio.play();
      if (promise && typeof promise.catch === 'function') promise.catch(()=>{});
    } catch (e) {}
  }

  function pause() {
    paused = true;
    if (track) { try { track.pause(); } catch (e) {} }
  }

  function resume() {
    if (!desired) return;
    paused = false;
    play(trackIndex<0?0:trackIndex);
  }

  function setMuted(value) {
    muted = !!value;
    if (track) track.muted = muted;
    if (!muted && desired && !paused) play(trackIndex<0?0:trackIndex);
  }

  function setLevel(value) {
    level=Math.max(0,Math.min(1,Number(value)||0));
    applyVolume();
    return level;
  }

  function duck(factor=0.76, duration=0.24) {
    duckGain=Math.min(duckGain,clamp(Number(factor)||0.76,0.5,1));
    duckHold=Math.max(duckHold,Math.max(0,Number(duration)||0));
    applyVolume();
  }

  function update(dt) {
    const step=Math.max(0,Number(dt)||0);
    if (desired && !paused) fade=Math.min(1,fade+step/0.45);
    if (duckHold>0) duckHold=Math.max(0,duckHold-step);
    else duckGain += (1-duckGain)*Math.min(1,step*7);
    applyVolume();
  }

  return {
    play, pause, resume, setMuted, setLevel, duck, update,
    getLevel:()=>level,
    isMuted:()=>muted,
    isDesired:()=>desired,
    currentMission:()=>trackIndex,
    mixState:()=>({bus:BUS_GAIN,trim:TRACK_TRIMS[trackIndex]??1,fade,duck:duckGain,duckHold,volume:track?.volume??0}),
    trackPaths:TRACKS.slice()
  };
})();

// Voice scripts + caption bus (TEMP AUDIO until real Voice clips are attached)
const VO = {
  title: { speaker:'ANNOUNCER', line:'Rusted Cannon! One mercenary. No retreat!' },
  missionStart: { speaker:'ANNOUNCER', line:'Mission Start!' },
  missionClear: { speaker:'ANNOUNCER', line:'Mission Clear!' },
  gameOver: { speaker:'ANNOUNCER', line:'Game Over' },
  cont: { speaker:'ANNOUNCER', line:'Continue?' },
  brief: [
    [
      {speaker:'RADIO OPS', line:'Rook, Cordite has seized Ash Boulevard.'},
      {speaker:'RADIO OPS', line:'Break through the checkpoint and free the marked POWs.'},
      {speaker:'RADIO OPS', line:'Expect convoy armor, then an Ironclad Marauder.'},
      {speaker:'RADIO OPS', line:'The city is burning. Keep moving.'},
      {speaker:'PLAYER', line:'Copy. Rusted Cannon moving in.'}
    ],
    [
      {speaker:'RADIO OPS', line:'Jungle green, river gray — bad poetry, worse flak.'},
      {speaker:'RADIO OPS', line:'Scrap Mule is staged mid-route. Mount it.'},
      {speaker:'RADIO OPS', line:'Rescue cages are tucked under the canopy.'},
      {speaker:'RADIO OPS', line:'A gunboat controls the river bend.'},
      {speaker:'PLAYER', line:'Then we take the river.'}
    ],
    [
      {speaker:'RADIO OPS', line:'Iron Hive front door is a suggestion. Use explosives.'},
      {speaker:'RADIO OPS', line:'Turret hallway, ambush closets, no polite lobby.'},
      {speaker:'RADIO OPS', line:'The core chamber houses their command mech.'},
      {speaker:'RADIO OPS', line:'Bring the whole facility down.'},
      {speaker:'PLAYER', line:'Rusted Cannon. Final run.'}
    ]
  ],
  player: {
    hit:['Still standing.','Not today.','I have had worse.'],
    grenade:['Clear out!','Fire in the hole!','Catch!'],
    mount:['Mule mines!','Vehicle acquired!'],
    win:['Target down.','Area secure.']
  },
  pow: [
    'Thank you!', 'I am getting out!', 'Running free!',
    'You came back for us!', 'You saved my life!', 'Go go go!'
  ],
  boss: {
    0:['You stop here!','Crush the mercenary!','Paint the asphalt!'],
    1:['The river is ours!','Swim in tracers!','Moss eats heroes!'],
    2:['Phase lock engaged!','The Hive remembers!','Rusted Cannon dies here!','Core overclock!','Kneel before the Hive!','All weapons free!','Impossible... the core...']
  }
};

const VOManifest = [];
function catalogVO() {
  const add = (id, speaker, line, when) => VOManifest.push({id, speaker, line, when, audio:'TEMP AUDIO caption'});
  add('title_tease', VO.title.speaker, VO.title.line, 'title screen');
  VO.brief.forEach((arr,i)=>arr.forEach((l,j)=>add(`brief_${i+1}_${j+1}`, l.speaker, l.line, `mission ${i+1} briefing`)));
  Object.entries(VO.player).forEach(([k,arr])=>arr.forEach((line,i)=>add(`player_${k}_${i+1}`,'PLAYER',line,`player ${k}`)));
  VO.pow.forEach((line,i)=>add(`pow_${i+1}`,'POW',line,'POW rescue'));
  Object.entries(VO.boss).forEach(([k,arr])=>arr.forEach((line,i)=>add(`boss_l${+k+1}_${i+1}`,'BOSS',line,`boss level ${+k+1}`)));
  add('sys_start', VO.missionStart.speaker, VO.missionStart.line, 'mission start');
  add('sys_clear', VO.missionClear.speaker, VO.missionClear.line, 'mission clear');
  add('sys_over', VO.gameOver.speaker, VO.gameOver.line, 'game over');
  add('sys_cont', VO.cont.speaker, VO.cont.line, 'continue screen');
}
catalogVO();

// ============================================================
// PIXEL ART GENERATOR (placeholder until Imagine frames swap)
// ============================================================
const GDD = {
  title: 'RUSTED CANNON',
  pitch: 'A battle-scarred mercenary tears through a rogue PMC and its scrap-iron war machines.',
  playerName: 'Elon "Rook" Kane',
  squad: 'Rusted Cannon Unit',
  playerFaction: 'Free Flare Irregulars',
  enemyFaction: 'Cordite Syndicate',
  weapons: { pistol: 'Service Pistol', heavy: 'Ripper Rifle', rocket: 'Howler Rocket' },
  vehicle: 'Scrap Mule',
  missions: [
    { id: 0, code: 'M1', name: 'Ash Boulevard', tag: 'City to scorched highway' },
    { id: 1, code: 'M2', name: 'Green Hell Run', tag: 'Jungle river push' },
    { id: 2, code: 'M3', name: 'Iron Hive', tag: 'Base / hangar core' }
  ],
  bosses: ['Ironclad Marauder', 'Moss Gunboat', 'Iron Hive Core'],
  palette: {
    bg0:'#0b1020', bg1:'#16213a', sand:'#c7a36a', brick:'#8b4d3a', leaf:'#2f6b3a',
    metal:'#7f8b98', red:'#d94b4b', gold:'#f0c14a', skin:'#e0b080', blue:'#4aa3ff',
    smoke:'#9aa3ad', white:'#f4f7fb', black:'#10141c', magenta:'#ff00ff', grit:'#5a4634'
  },
  controls: 'Arrows/WASD move, Z/J shoot, X/K jump, C/L grenade, Enter start, Escape pause/menu, O options, M mute, Down crouch',
  scoring: {
    grunt: 200, shield: 350, rocket: 400, turret: 500, truck: 600, pow: 1000,
    crate: 50, barrel: 75, miniboss: 5000, finalBoss: 12000, clearBonus: 3000
  }
};

const Art = (() => {
  const cache = new Map();
  const atlas = (typeof window !== 'undefined' && window.SPRITE_ATLAS) ? window.SPRITE_ATLAS : {};
  const imgCache = new Map();
  const P = GDD.palette;

  // preload atlas images (decode ASAP; data URLs usually sync-complete)
  let assetsPending = 0;
  let assetsFinalized = false;
  const assetsReadyWaiters = [];
  function finalizeAssetsReady() {
    if (assetsFinalized) return;
    assetsFinalized = true;
    assetsPending = 0;
    try { cache.clear(); } catch (e) {}
    while (assetsReadyWaiters.length) { try { assetsReadyWaiters.shift()(); } catch (e) {} }
  }
  function noteAsset() {
    if (assetsFinalized) {
      // A late decoder completion after the safety timeout must replace any
      // cached fallback on the very next frame.
      try { cache.clear(); } catch (e) {}
      return;
    }
    assetsPending = Math.max(0, assetsPending - 1);
    if (assetsPending === 0) finalizeAssetsReady();
  }
  function whenAssetsReady(cb) {
    if (assetsFinalized || assetsPending === 0) cb();
    else assetsReadyWaiters.push(cb);
  }
  const atlasKeys = Object.keys(atlas);
  assetsPending = atlasKeys.length;
  atlasKeys.forEach(k => {
    const im = new Image();
    // HQ frames carry six times the display pixels while retaining the exact
    // logical box of the approved gameplay sprite.
    if (k.startsWith('hq_')) im.__logicalScale = HQ_ASSET_SCALE;
    imgCache.set(k, im);
    let done=false; const once=()=>{ if(done) return; done=true; noteAsset(); };
    im.onload = once; im.onerror = once;
    try { im.src = atlas[k]; } catch (e) { once(); return; }
    if (im.complete && im.naturalWidth > 0) once();
  });
  if (assetsPending === 0) finalizeAssetsReady();
  // A broken image must never block boot forever, but healthy embedded assets
  // get time to decode before their procedural fallbacks are cached.
  setTimeout(finalizeAssetsReady, 2000);

  function make(key, w, h, draw) {
    if (cache.has(key)) return cache.get(key);
    const c = document.createElement('canvas');
    c.width = w; c.height = h;
    const g = c.getContext('2d');
    g.imageSmoothingEnabled = false;
    draw(g, w, h);
    cache.set(key, c);
    return c;
  }
  function px(g,x,y,w,h,col){ g.fillStyle=col; g.fillRect(x|0,y|0,w,h); }

  function fromAtlas(key, fw, fh, fallbackDraw) {
    const ck = 'atl_'+key+'_best';
    if (cache.has(ck)) return cache.get(ck);
    const quality = imgCache.get('hq_'+key);
    const im = quality && quality.complete && quality.naturalWidth > 0 ? quality : imgCache.get(key);
    if (im && im.complete && im.naturalWidth > 0) {
      // Keep fitted classic frames pixel-perfect. Never resize again here.
      cache.set(ck, im);
      return im;
    }
    const c = document.createElement('canvas');
    c.width = fw || 32; c.height = fh || 40;
    const g = c.getContext('2d');
    g.imageSmoothingEnabled = false;
    if (typeof fallbackDraw === 'function') fallbackDraw(g, c.width, c.height);
    else {
      g.fillStyle = '#ff00ff'; g.fillRect(0,0,c.width,c.height);
      g.fillStyle = '#111'; g.font='8px monospace'; g.fillText(String(key).slice(0,10),1,10);
    }
    cache.set(ck, c);
    return c;
  }

  function fittedFrame(key, fw, fh, fallbackDraw) {
    const src = fromAtlas(key, fw, fh, fallbackDraw);
    const sw = src.naturalWidth || src.width || fw;
    const sh = src.naturalHeight || src.height || fh;
    if (sw===fw && sh===fh) return src;
    return make('fit_'+key+'_'+fw+'x'+fh, fw, fh, g => {
      // Pad into a stable animation box; never stretch the source pixels.
      g.drawImage(src, Math.floor((fw-sw)/2), fh-sh);
    });
  }

  function hero(frame='idle', facing=1) {
    const map = {
      idle:'hero_idle', run1:'hero_run1', run2:'hero_run2', run3:'hero_run3', run4:'hero_run4',
      // The generated atlas exported these two source names swapped: the
      // `hero_hurt` PNG is the actual crouched firing pose, while
      // `hero_shootCrouch` contains the red hit reaction.
      jump:'hero_jump', fall:'hero_fall', crouch:'hero_crouch', shoot:'hero_shoot', shootCrouch:'hero_hurt',
      throw:'hero_throw', hurt:'hero_shootCrouch', death:'hero_death'
    };
    const key = map[frame] || 'hero_idle';
    return fromAtlas(key, 32, 40, (g)=>{
      const bob = frame==='run1' ? 1 : 0;
      const crouch = frame==='crouch' || frame==='shootCrouch';
      const bodyY = crouch ? 12 : 8 + bob;
      px(g,10,bodyY,10,crouch?16:18,P.blue);
      px(g,11,bodyY-6,8,7,P.skin);
      px(g,10,bodyY-8,10,4,'#2a3444');
      px(g,18,bodyY-4,2,2,'#111');
      if (frame==='run1') { px(g,10,bodyY+18,4,10,P.grit); px(g,16,bodyY+17,4,11,P.grit); }
      else if (frame==='run2') { px(g,11,bodyY+17,4,11,P.grit); px(g,15,bodyY+18,4,10,P.grit); }
      else if (frame==='jump' || frame==='fall') { px(g,10,bodyY+17,4,9,P.grit); px(g,16,bodyY+16,4,9,P.grit); }
      else if (crouch) { px(g,10,bodyY+16,11,8,P.grit); }
      else { px(g,11,bodyY+18,4,11,P.grit); px(g,16,bodyY+18,4,11,P.grit); }
      const gunY = crouch ? bodyY+8 : bodyY+10;
      if (frame!=='death' && frame!=='hurt') { px(g,20,gunY,9,3,P.metal); if (String(frame).startsWith('shoot')) px(g,28,gunY-1,4,4,P.gold); }
      if (frame==='throw') px(g,5,bodyY+5,5,5,P.red);
      if (frame==='hurt') px(g,10,bodyY,10,18,P.red);
      if (frame==='death') { px(g,4,28,24,8,P.blue); px(g,2,26,8,6,P.skin); }
    });
  }

  function enemy(type='grunt', frame='idle') {
    let key = 'enemy_'+type;
    if (type==='grunt') key = frame==='die' ? 'enemy_grunt_die' : (frame==='walk'||frame==='run') ? 'enemy_grunt_walk' : 'enemy_grunt_idle';
    const tw = type==='truck' ? 72 : type==='turret' ? 32 : 32;
    const th = type==='truck' ? 40 : type==='turret' ? 28 : 36;
    return fromAtlas(key, tw, th, (g)=>{
      if (type==='grunt') {
        px(g,10,8,10,14,P.red); px(g,11,3,8,6,P.skin); px(g,10,2,10,3,'#402018');
        px(g,11,20,4,10,P.grit); px(g,16,20,4,10,P.grit); px(g,20,12,8,3,P.metal);
      } else if (type==='shield') {
        px(g,12,6,10,16,'#6b7280'); px(g,13,2,8,5,P.skin); px(g,4,8,10,18,P.metal);
      } else if (type==='rocket') {
        px(g,10,8,10,14,'#7a3b2e'); px(g,11,3,8,6,P.skin); px(g,18,10,10,5,P.metal); px(g,26,8,4,8,P.red);
      } else if (type==='turret') {
        px(g,4,16,24,10,P.metal); px(g,8,6,14,12,'#4b5563'); px(g,20,10,10,4,P.red);
      } else if (type==='truck') {
        px(g,2,12,56,16,P.metal); px(g,36,4,20,12,'#4b5563'); px(g,6,24,10,10,'#222'); px(g,40,24,10,10,'#222');
      }
    });
  }

  function pow() {
    return fromAtlas('pow', 24, 36, (g)=>{
      px(g,7,10,10,14,'#d7dce8'); px(g,8,4,8,7,P.skin); px(g,6,3,12,4,'#b08a4a');
      px(g,8,24,4,9,P.grit); px(g,13,24,4,9,P.grit); px(g,3,12,4,10,P.gold);
    });
  }

  function vehicle(frame='idle') {
    // The dedicated drive frame contains the complete half-track.  Previous
    // vehicle frames were crops of unrelated vehicles and showed only a nose.
    const key = 'vehicle_drive';
    return fromAtlas(key, 80, 48, (g)=>{
      px(g,6,14,50,14,'#5d6b3a'); px(g,36,6,18,12,'#47532d'); px(g,12,26,10,9,'#222'); px(g,40,26,10,9,'#222');
      px(g,52,12,10,4,P.metal); px(g,18,8,10,8,P.blue);
      if (frame==='shoot') px(g,60,10,8,5,P.gold);
      if (frame==='damage' || frame==='explode') px(g,16,8,12,8,P.red);
    });
  }

  function asset(key, fw=16, fh=16) {
    return fromAtlas(key, fw, fh, null);
  }

  function boss(id, frame='idle') {
    let key = 'boss'+id;
    if (frame==='attack' || frame==='attackA' || frame==='attackB') key = 'boss'+id+'_attack';
    if (id===2 && (frame==='phase2')) key='boss2_phase2';
    if (frame==='defeat' || frame==='death') key=id===2 ? 'boss_defeat' : 'boss'+id+'_attack';
    const tw = id===2 ? 112 : 132;
    const th = id===2 ? 88 : id===1 ? 72 : 63;
    const quality = imgCache.get('hq_'+key);
    if (quality && quality.complete && quality.naturalWidth>0) return fromAtlas(key, tw, th, null);
    return fittedFrame(key, tw, th, (g)=>{
      if (id===0) { px(g,6,20,64,20,P.metal); px(g,48,8,24,20,'#4b5563'); px(g,12,36,12,10,'#222'); px(g,48,36,12,10,'#222'); px(g,68,16,12,5,P.red); }
      else if (id===1) { px(g,0,18,80,18,'#355e3b'); px(g,56,6,20,16,'#2a4a30'); px(g,10,30,14,10,'#1a1a1a'); }
      else { px(g,12,24,72,34,P.metal); px(g,24,10,48,18,'#3a4250'); px(g,36,2,24,12,P.red); px(g,40,34,16,14,P.gold); }
      if (frame==='attack') px(g,0,24,10,8,P.gold);
      if (frame==='defeat') px(g,20,12,50,34,P.gold);
    });
  }

  function tile(kind, theme) {
    if (kind==='ground') return fromAtlas('tile_ground_'+theme, 16, 16, (g)=>{ const c=theme===0?P.brick:theme===1?P.leaf:P.metal; px(g,0,0,16,16,c); });
    if (kind==='wall') return fromAtlas('tile_wall_'+theme, 16, 16, (g)=>{ px(g,0,0,16,16, theme===2?'#3a4250':'#6b5344'); });
    if (kind==='crate') return fromAtlas('prop_crate', 26, 24, (g)=>{ px(g,1,1,24,22,'#8a5a2b'); });
    if (kind==='barrel') return fromAtlas('prop_barrel', 18, 26, (g)=>{ px(g,3,1,12,24,P.red); });
    return fromAtlas('tile_wall_'+theme, 16, 16, (g)=>px(g,0,0,16,16,'#666'));
  }
  function platformSprite(theme, part='mid') {
    // Prefer crisp 32x22 girder tiles (top surface at y=0 for collider match)
    const key32 = 'plat_' + theme + '_' + part + '32';
    const key = 'plat_' + theme + '_' + part;
    const im32 = imgCache.get(key32);
    if (im32 && im32.complete && im32.naturalWidth > 0) return fromAtlas(key32, 32, 22, null);
    return fromAtlas(key, 32, 22, (g)=>{
      const top = theme===0 ? '#d2c8b0' : theme===1 ? '#c4924e' : '#becce0';
      const body = theme===0 ? '#6c655a' : theme===1 ? '#7a4e25' : '#46586c';
      const dark = theme===0 ? '#3c372f' : theme===1 ? '#3a2410' : '#243140';
      px(g,0,0,32,4,top);
      px(g,0,4,32,12,body);
      px(g,0,16,32,3,dark);
      for (let x=6;x<32;x+=10){ px(g,x,6,2,10,dark); px(g,x-2,15,6,3,dark); }
      if (part==='left') px(g,0,0,3,18,dark);
      if (part==='right') px(g,29,0,3,18,dark);
    });
  }

  function platformSpan(theme) {
    return fromAtlas('platform_'+theme,128,24,(g)=>{
      const top=theme===1?'#c4924e':theme===2?'#c9b88e':'#cbd5e1';
      const body=theme===1?'#6b421f':theme===2?'#71634c':'#475569';
      px(g,0,0,128,5,top); px(g,0,5,128,19,body);
      for(let x=8;x<128;x+=24){px(g,x,7,4,17,'#242a32');px(g,x+4,17,16,3,'#242a32');}
    });
  }

  function platformSupport(theme) {
    return fromAtlas('platform_support_'+theme,24,48,(g)=>{
      const body=theme===1?'#6b421f':theme===2?'#71634c':'#475569';
      const edge=theme===1?'#c4924e':theme===2?'#c9b88e':'#cbd5e1';
      px(g,8,0,8,48,body); px(g,10,0,2,48,edge);
      px(g,2,8,6,4,body); px(g,16,8,6,4,body);
    });
  }

  function dockSpan() {
    return fromAtlas('dock_span',192,24,(g)=>{
      px(g,0,0,192,6,'#c4924e'); px(g,0,6,192,18,'#6b421f');
      for(let x=4;x<192;x+=18) px(g,x,1,2,20,'#3a2410');
    });
  }

  function bgLayer(theme, layer) {
    // return native-size tileable strip (NOT forced into 384x224) for seamless infinite scroll
    const key = layer ? ('bg_'+theme+'_far') : ('bg_'+theme);
    const im = imgCache.get(key);
    if (im && im.complete && im.naturalWidth > 0) return im;
    // fallback canvas one screen wide, still ideally tileable
    return fromAtlas(key, 512, H, (g)=>{
      if (theme===0) { px(g,0,0,512,H, layer? '#3a2618' : '#5c3d24'); for(let i=0;i<16;i++){ const x=i*36, bh=40+((i*17)%70); px(g,x,H-70-bh,28,bh, layer? '#2a3348':'#4a3a32'); } }
      else if (theme===1) { px(g,0,0,512,H, layer? '#0c1c14':'#163022'); for(let i=0;i<18;i++){ const x=i*30,hgt=50+(i*13)%90; px(g,x,H-70-hgt,18,hgt,layer?'#163d28':'#1f5a38'); } }
      else { px(g,0,0,512,H, layer? '#12151c':'#1a2430'); for(let i=0;i<14;i++) px(g,i*40,30+((i%2)*20),28,120,layer?'#232a38':'#2c3548'); }
      // soft bottom fade
      for(let y=H-56;y<H;y++){ g.fillStyle=`rgba(0,0,0,${((y-(H-56))/56)*0.35}`; g.fillRect(0,y,512,1); }
    });
  }

  function bgFull(theme) {
    const im = imgCache.get('hq_bg_full_'+theme);
    if (!im || !im.complete || im.naturalWidth<1000) return null;
    // These full-scene sources use their own 2172x724 density rather than the
    // regular six-times sprite grid. Rendering code consumes these explicit
    // logical dimensions and therefore never magnifies a 384px background.
    im.__logicalWidth=672;
    im.__logicalHeight=224;
    return im;
  }

  function groundStrip(theme) {
    const key = 'ground_strip_' + theme;
    const im = imgCache.get(key);
    if (im && im.complete && im.naturalWidth > 0) return im;
    const h = H - GROUND_BASE; // fill all the way to bottom
    return fromAtlas(key, 256, Math.max(48,h), (g)=>{
      const cols = theme===0? ['#343943','#4a5260','#9aa6b8'] : theme===1? ['#5a4a2c','#3f5a2a','#b09a54'] : ['#334152','#4a5c70','#b8c6d6'];
      for(let y=0;y<h;y++){
        for(let x=0;x<256;x++){
          const n=(x*13+y*7)&3; g.fillStyle = n?cols[0]:cols[1];
          if(y<3) g.fillStyle=cols[2];
          if(y>h-6) g.fillStyle=cols[0];
          g.fillRect(x,y,1,1);
        }
      }
    });
  }

  function vfx(kind) {
    const map={muzzle:'vfx_muzzle', boom:'vfx_boom2', boom1:'vfx_boom1', boom2:'vfx_boom2', boom3:'vfx_boom3'};
    const key=map[kind]||'vfx_boom1';
    return fromAtlas(key, 24, 24, (g)=>{
      if (kind==='muzzle') { px(g,2,10,18,5,P.gold); px(g,14,7,9,10,P.white); }
      else if (String(kind).startsWith('boom')) { px(g,6,6,12,12,P.gold); px(g,3,3,7,7,P.red); }
      else { px(g,10,2,4,20,P.gold); px(g,2,10,20,4,P.gold); }
    });
  }

  function logo() {
    return fromAtlas('ui_logo_rusted', 238, 96, (g)=>{
      px(g,8,24,222,48,'#171b20'); px(g,10,26,218,44,P.gold);
      px(g,14,30,210,36,'#343941');
      g.fillStyle='#f4e4bf'; g.font='bold 22px monospace'; g.textAlign='center';
      g.fillText('RUSTED CANNON',119,55); g.textAlign='left';
    });
  }
  function menuLabel(name='PLAY') {
    const normalized=String(name).toLowerCase();
    return fromAtlas('ui_menu_'+normalized,128,20,(g)=>{
      g.fillStyle='#151a21'; g.font='bold 15px monospace'; g.textAlign='center';
      g.strokeStyle='#d59b38'; g.lineWidth=3; g.strokeText(String(name).toUpperCase(),64,16);
      g.fillStyle='#d6d3cd'; g.fillText(String(name).toUpperCase(),64,16); g.textAlign='left';
    });
  }
  function radio() {
    return fromAtlas('ui_radio',40,56,(g)=>{
      px(g,25,0,4,14,'#94a3b8'); px(g,5,13,30,42,'#4f5f32');
      px(g,9,17,21,9,'#9dd6bd'); px(g,9,29,21,16,'#29351f');
      for(let y=30;y<44;y+=2) px(g,11,y,17,1,'#87936a');
      px(g,1,24,5,9,'#d9a441');
    });
  }
  function rankBadge(rank='C') {
    const normalized = ['S','A','B','C'].includes(rank) ? rank : 'C';
    return fromAtlas('rank_'+normalized,52,52,(g)=>{
      const color=normalized==='S'?'#f0c14a':normalized==='A'?'#d9a441':normalized==='B'?'#94a3b8':'#9a6a35';
      px(g,4,4,44,44,'#111827');
      g.strokeStyle=color; g.lineWidth=4; g.strokeRect(5,5,42,42);
      g.fillStyle=color; g.font='bold 32px monospace'; g.textAlign='center'; g.textBaseline='middle'; g.fillText(normalized,26,28);
    });
  }
  function heart(on=true){
    if (on) return fromAtlas('ui_heart_on',18,16,(g)=>px(g,2,3,14,10,P.red));
    const empty=make('ui_heart_clearly_empty',18,16,g=>{
      px(g,3,4,5,4,'#475569'); px(g,10,4,5,4,'#475569');
      px(g,2,6,14,4,'#334155'); px(g,5,10,8,3,'#1e293b'); px(g,8,13,2,2,'#1e293b');
      g.strokeStyle='#64748b'; g.strokeRect(2.5,4.5,12,6);
    });
    empty.__emptyHeart=true;
    return empty;
  }

  function helicopter(frame=0){
    return fromAtlas('helicopter_gpt',140,52,(g)=>{
      // Compact procedural fallback if the generated atlas frame cannot load.
      const rotorX=(frame&1)?8:2;
      px(g,rotorX,2,112,3,'#202833');
      px(g,55,6,5,6,'#4b5563');
      px(g,24,13,70,22,'#394936');
      px(g,12,18,24,14,'#394936');
      px(g,18,16,18,12,'#71a6b8');
      px(g,91,18,29,6,'#394936');
      px(g,116,12,6,14,'#394936');
      px(g,44,34,24,4,'#252b28');
      px(g,30,40,54,2,'#202833');
      px(g,50,30,8,5,'#c89a3a');
    });
  }

  function decor(kind){
    const key = 'decor_'+kind;
    const sizes = {
      // relative to hero 32x40 (fitted native frames)
      sandbag:[36,22], sandbag_wide:[48,22], crate:[26,24], barrel:[18,26],
      fence:[40,26], lamp:[14,48], palm:[44,72], drums:[28,24], sign:[18,30],
      bush:[32,22], grass:[28,16], tower:[18,52], barrier:[40,24], tires:[28,22],
      rock:[30,20], vine:[20,34], ammo:[22,16]
    };
    const [tw,th] = sizes[kind] || [30,24];
    return fromAtlas(key, tw, th, (g)=>{
      g.fillStyle = '#5b6470'; g.fillRect(3, th-16, tw-6, 14);
      g.strokeStyle = '#111'; g.strokeRect(3.5, th-15.5, tw-7, 13);
    });
  }
  function bullet(){ return fromAtlas('vfx_bullet', 8, 4, (g)=>{ g.fillStyle='#ffe'; g.fillRect(0,1,8,2); }); }
  function smoke(){ return fromAtlas('vfx_smoke', 12, 12, (g)=>{ g.fillStyle='rgba(200,200,210,0.5)'; g.beginPath(); g.arc(6,6,5,0,Math.PI*2); g.fill(); }); }
  return { hero, enemy, pow, vehicle, boss, helicopter, tile, platformSprite, platformSpan, platformSupport, dockSpan, bgLayer, bgFull, groundStrip, vfx, logo, menuLabel, radio, rankBadge, heart, decor, bullet, smoke, asset, cache, imgCache, whenAssetsReady };
})();

// ============================================================
// GAME STATE
// ============================================================
const DIFFICULTIES = [
  {id:'recruit', name:'RECRUIT', playerHp:9, grenades:12, enemyHp:0.82, fireDelay:1.28, enemySpeed:0.88, dropChance:0.22, bossHp:0.84},
  {id:'arcade', name:'ARCADE', playerHp:7, grenades:10, enemyHp:1, fireDelay:1, enemySpeed:1, dropChance:0.16, bossHp:1},
  {id:'veteran', name:'VETERAN', playerHp:6, grenades:8, enemyHp:1.22, fireDelay:0.80, enemySpeed:1.12, dropChance:0.11, bossHp:1.18}
];
function difficultyConfig(){ return DIFFICULTIES[State.difficultyIndex]||DIFFICULTIES[1]; }
function setDifficulty(index) {
  const previous=difficultyConfig();
  State.difficultyIndex=(index+DIFFICULTIES.length)%DIFFICULTIES.length;
  const cfg=difficultyConfig();
  const p=State.player;
  if (p) {
    const ratio=clamp(p.hp/Math.max(1,p.maxHp),0,1);
    p.maxHp=cfg.playerHp; p.hp=Math.max(1,Math.round(cfg.playerHp*ratio));
  }
  if (State.level) {
    const enemyScale=cfg.enemyHp/Math.max(0.01,previous.enemyHp);
    for (const enemy of State.entities||[]) {
      if (enemy.type!=='enemy'||!enemy.alive) continue;
      const ratio=enemy.hp/Math.max(1,enemy.maxHp||enemy.hp);
      enemy.maxHp=Math.max(1,Math.round((enemy.maxHp||enemy.hp)*enemyScale));
      enemy.hp=Math.max(1,Math.round(enemy.maxHp*ratio));
    }
    if (State.boss&&State.boss.alive) {
      const ratio=State.boss.hp/Math.max(1,State.boss.maxHp);
      const bossScale=cfg.bossHp/Math.max(0.01,previous.bossHp);
      State.boss.maxHp=Math.max(1,Math.round(State.boss.maxHp*bossScale));
      State.boss.hp=Math.max(1,Math.round(State.boss.maxHp*ratio));
    }
  }
}

const MISSION_OBJECTIVES = [
  {type:'pow', target:5, label:'RESCUE ALL 5 POWs', short:'POW'},
  {type:'convoy', target:1, label:'DESTROY THE CONVOY', short:'CONVOY'},
  {type:'noDeath', target:0, label:'FINISH WITHOUT DYING', short:'NO DEATH'}
];

const State = {
  mode: 'TITLE', // TITLE OPTIONS BRIEF PLAY PAUSE CLEAR GAMEOVER ENDING
  mission: 0,
  briefIndex: 0,
  briefTimer: 0,
  score: 0,
  lives: 3,
  continues: 2,
  hudBanner: '',
  bannerTimer: 0,
  startCueTimer: 0,
  caption: null, // {speaker,line,t}
  captionQueue: [],
  shake: 0,
  hitstop: 0,
  camX: 0,
  debug: false,
  scanlines: true,
  integerScale: true,
  difficultyIndex: 1,
  shakeScale: 0.5,
  optionsReturn: 'TITLE',
  optionsIndex: 0,
  muted: false,
  runTime: 0,
  clearTimer: 0,
  endingTimer: 0,
  gameOverTimer: 0,
  continueTimer: 0,
  titlePulse: 0,
  titleMenuIndex: 0,
  checkpointX: 40,
  powRescued: 0,
  kills: 0,
  combo: 0,
  comboTimer: 0,
  maxCombo: 0,
  missionStats: null,
  missionResult: null,
  spawnCursor: 0,
  level: null,
  player: null,
  entities: [],
  bullets: [],
  particles: [],
  floats: [],
  solid: [],
  triggers: [],
  vehicle: null,
  boss: null,
  flags: Object.create(null)
};

// ============================================================
// LEVEL DATA
// ============================================================
function solidRect(x,y,w,h,type='ground'){ return {x,y,w,h,type,hp: type==='crate'?3: type==='barrel'?1: type==='weak'?5: 999, alive:true}; }

function buildMission(id) {
  const solids = [];
  const spawns = [];
  const triggers = [];
  const theme = id;
  const length = id===0 ? 3200 : id===1 ? 3600 : 3400;
  // continuous ground
  solids.push(solidRect(0, GROUND_BASE, length + W, 64, 'ground'));
  // platform geometry as proper spans (Metal Slug style beams/ledges)
  // y is TOP of platform, thickness deeper for readable underside bracing
  const platH = 6; // collision on top plate only; girder body is visual below
  const plats = id===0 ? [
    [240,148,96], [520,132,80], [820,140,112], [1180,128,80], [1500,144,96],
    [1880,130,112], [2280,140,96], [2680,126,80]
  ] : id===1 ? [
    [260,142,80], [540,120,96], [860,146,80], [1160,112,112], [1520,136,96],
    [1900,122,112], [2300,140,80], [2700,116,96], [3080,132,96]
  ] : [
    [300,140,96], [620,124,80], [940,148,96], [1280,118,112], [1660,136,80],
    [2040,120,112], [2420,144,96], [2800,114,96]
  ];
  plats.forEach(([x,y,w]) => {
    // The authored platform includes a thick upper rim. Keep the artwork at
    // `visualY`, but place the walkable collision five pixels inside that rim
    // so boots visually contact the deck instead of floating above it.
    const s = solidRect(x, y+5, w, platH, 'platform');
    s.visualY = y;
    s.theme = id;
    solids.push(s);
  });

  // crates / barrels / weak walls
  const props = id===0 ? [
    ['crate',180,GROUND_BASE-16], ['barrel',460,GROUND_BASE-16], ['crate',760,GROUND_BASE-16],
    ['barrel',1100,GROUND_BASE-16], ['crate',1500,GROUND_BASE-16], ['weak',2000,GROUND_BASE-48],
    ['crate',2050,GROUND_BASE-16], ['barrel',2400,GROUND_BASE-16]
  ] : id===1 ? [
    ['crate',300,GROUND_BASE-16], ['barrel',900,GROUND_BASE-16], ['crate',1320,GROUND_BASE-16],
    ['crate',1900,GROUND_BASE-16], ['barrel',2500,GROUND_BASE-16]
  ] : [
    ['crate',340,GROUND_BASE-16], ['barrel',700,GROUND_BASE-16], ['crate',1100,GROUND_BASE-16],
    ['crate',2000,GROUND_BASE-16], ['barrel',2400,GROUND_BASE-16],
    ['crate',2800,GROUND_BASE-16]
  ];
  props.forEach(([t,x,y]) => {
    if (t==='crate') solids.push(solidRect(x, GROUND_BASE-24, 26, 24, t));
    else if (t==='barrel') solids.push(solidRect(x, GROUND_BASE-26, 18, 26, t));
    else solids.push(solidRect(x,y,16, t==='weak'?48:16, t));
  });

  // enemy / pow / pickup spawns as timeline by x camera entry
  const foot = (h=28) => GROUND_BASE - h;
  function e(type,x,y,opt={}) {
    const defaultH = type==='truck' ? 52 : type==='turret' ? 20 : 28;
    spawns.push({type:'enemy', kind:type, x, y: y??foot(defaultH), ...opt});
  }
  function p(x,y){ spawns.push({type:'pow', x, y:y??foot(28)}); }
  function item(kind,x,y){ spawns.push({type:'pickup', kind, x, y:y??foot(14)}); }
  function truck(x){ e('truck', x, foot(52), {hp:12}); }
  function squad(x, kinds){
    const maxWave = id===2 ? 4 : 3;
    kinds.slice(0,maxWave).forEach((k,i)=> e(k, x+i*42));
  }
  function nest(x){ e('turret', x, foot(20)); e('grunt', x-40); e('grunt', x+46); }

  if (id===0) {
    // Ash Boulevard — tutorial then pressure waves like classic MS Mission 1
    e('grunt', 260); e('grunt', 320);
    p(380);
    squad(450, ['grunt','grunt']);
    e('shield', 560);
    item('heavy', 620);
    squad(720, ['grunt','rocket','grunt']);
    nest(880);
    p(960);
    squad(1040, ['grunt','grunt','shield']);
    truck(1200);
    e('grunt', 1280); e('grunt', 1340); e('rocket', 1400);
    p(1480);
    squad(1560, ['grunt','shield','grunt','rocket']);
    nest(1720);
    item('grenade', 1800);
    squad(1880, ['grunt','grunt','grunt']);
    e('shield', 2000); e('turret', 2080, foot(20));
    p(2140);
    truck(2260);
    squad(2360, ['rocket','grunt','shield','grunt']);
    item('rocket', 2500);
    squad(2580, ['grunt','grunt','rocket','shield','grunt']);
    e('turret', 2720, foot(20)); e('turret', 2780, foot(20));
    p(2840);
    // pre-boss gauntlet
    squad(2920, ['shield','grunt','rocket','grunt','shield']);
    triggers.push({type:'boss', x:2900, done:false});
    triggers.push({type:'checkpoint', x:1500, done:false});
    triggers.push({type:'heli', x:900, done:false});
    triggers.push({type:'heli', x:2400, done:false});
  } else if (id===1) {
    // Green Hell — denser, vertical POWs, vehicle mid
    e('grunt', 240); e('grunt', 300); e('rocket', 380);
    p(580, 97); // feet align with the inset platform collision at y=125
    squad(520, ['grunt','shield','grunt']);
    nest(680);
    item('heavy', 760);
    squad(840, ['rocket','grunt','grunt','shield']);
    p(980);
    squad(1060, ['grunt','grunt','rocket']);
    e('turret', 1180, foot(20));
    triggers.push({type:'vehicle', x:1500, done:false});
    // while approaching mule
    squad(1320, ['grunt','shield','grunt','grunt']);
    item('repair', 1580);
    // onboard stretch — keep ground threats
    e('rocket', 1700); e('grunt', 1760); e('grunt', 1820); e('shield', 1900);
    truck(2000);
    p(2100);
    nest(2220);
    squad(2320, ['grunt','rocket','grunt','shield','grunt']);
    item('rocket', 2460);
    squad(2540, ['shield','grunt','rocket','grunt']);
    e('turret', 2680, foot(20)); e('turret', 2740, foot(20));
    p(2820);
    squad(2920, ['grunt','grunt','shield','rocket','grunt','grunt']);
    triggers.push({type:'checkpoint', x:1900, done:false});
    triggers.push({type:'boss', x:3250, done:false});
    triggers.push({type:'heli', x:1100, done:false});
    triggers.push({type:'heli', x:2500, done:false});
  } else {
    // Iron Hive — turret gauntlets + ambush closets
    nest(280);
    squad(360, ['shield','grunt','grunt']);
    e('rocket', 480); e('grunt', 540);
    p(620);
    nest(720);
    squad(820, ['grunt','rocket','shield','grunt']);
    item('heavy', 940);
    e('turret', 1040, foot(20)); e('turret', 1100, foot(20)); e('grunt', 1160); e('grunt', 1220);
    p(1300);
    squad(1380, ['shield','rocket','grunt','shield','grunt']);
    nest(1580);
    item('rocket', 1680);
    squad(1760, ['grunt','grunt','rocket','grunt']);
    e('turret', 1900, foot(20)); e('shield', 1980); e('rocket', 2060);
    p(2140);
    // ambush closet style cluster
    squad(2220, ['grunt','grunt','shield','grunt','rocket','grunt']);
    nest(2460);
    truck(2580);
    squad(2680, ['shield','grunt','rocket','shield','grunt']);
    item('grenade', 2800);
    e('turret', 2880, foot(20)); e('turret', 2940, foot(20)); e('turret', 3000, foot(20));
    squad(3080, ['rocket','shield','grunt','grunt','rocket','shield']);
    p(3180);
    triggers.push({type:'checkpoint', x:1700, done:false});
    triggers.push({type:'boss', x:3100, done:false});
    triggers.push({type:'heli', x:900, done:false});
  }


  // scenery props — quality MS-inspired set dressing
  const decors = [];
  function decor(kind,x,y,flip=1){
    // Crates and barrels communicate destructibility; keep only collider-backed
    // versions so the player never shoots through a visually identical prop.
    if (kind==='crate' || kind==='barrel') return;
    const hpByKind={lamp:2,sign:1,fence:2,drums:2,sandbag:3};
    decors.push({kind,x,y:y??GROUND_BASE,flip,bob:Math.random()*8,
      hp:hpByKind[kind]||0,maxHp:hpByKind[kind]||0,alive:true,destroyT:0,fallDir:1});
  }
  const step = id===1 ? 105 : 125;
  for (let x=70; x<length-100; x+=step) {
    const lane = (Math.floor(x/step)+id*2) % 6;
    if (id===0) {
      if (lane===0) { decor('sandbag',x); decor('barrel',x+30); }
      else if (lane===1) { decor('crate',x); decor('drums',x+34); }
      else if (lane===2) { decor('lamp',x); decor('sign',x+46); }
      else if (lane===3) { decor('fence',x); decor('sandbag',x+44); }
      else if (lane===4) { decor('drums',x); decor('crate',x+32); }
      else { decor('barrel',x); decor('fence',x+36); decor('sandbag',x+70); }
    } else if (id===1) {
      if (lane===0) { decor('palm',x,GROUND_BASE, Math.random()<0.5?1:-1); decor('bush',x+38); }
      else if (lane===1) { decor('bush',x); decor('drums',x+40); }
      else if (lane===2) { decor('palm',x); decor('crate',x+36); }
      else if (lane===3) { decor('sandbag',x); decor('barrel',x+30); }
      else if (lane===4) { decor('palm',x,GROUND_BASE,-1); decor('fence',x+46); }
      else { decor('bush',x); decor('sign',x+34); decor('drums',x+64); }
    } else {
      if (lane===0) { decor('lamp',x); decor('crate',x+28); }
      else if (lane===1) { decor('drums',x); decor('barrel',x+32); }
      else if (lane===2) { decor('fence',x); decor('sandbag',x+38); }
      else if (lane===3) { decor('sign',x); decor('drums',x+30); }
      else if (lane===4) { decor('lamp',x); decor('barrel',x+26); }
      else { decor('crate',x); decor('fence',x+34); decor('sandbag',x+66); }
    }
  }
  // landmarks near common combat anchors
  decor('crate', 210); decor('sandbag', 520); decor('barrel', 870); decor('drums', 1410);
  decor('fence', 1820); decor('sign', 2310); decor('lamp', 2680); if (id===1) decor('palm', 980);

  // Keep the river boss arena readable: the dock and boat are the set piece,
  // so procedural jungle palms/bushes must not grow through the planks or hull.
  if (id===1) {
    const dockApproach = length - 450;
    for (let i=decors.length-1;i>=0;i--) {
      if (decors[i].x >= dockApproach) decors.splice(i,1);
    }
  }

  // The mission backgrounds already contain their own distant architecture.
  // Procedural opaque rectangles read as broken black doors on top of it.
  const mid = [];

  // updateSpawns advances with a single cursor, so definitions must be sorted.
  // Nested squads intentionally add flankers before/after their anchor.
  spawns.sort((a,b) => a.x-b.x);

  return {
    id, theme, length, solids, spawns, triggers, decors, mid,
    spawnIndex: 0,
    name: GDD.missions[id].name,
    code: GDD.missions[id].code
  };
}

// ============================================================
// ENTITIES
// ============================================================
function makePlayer() {
  const cfg=difficultyConfig();
  return {
    type:'player', x:40, y:GROUND_BASE-32, w:16, h:32,
    standingH:32, crouchH:20,
    vx:0, vy:0, facing:1, onGround:false, crouch:false,
    hp:cfg.playerHp, maxHp:cfg.playerHp, invuln:0, alive:true,
    weapon:'pistol', ammo: {pistol:Infinity, heavy:80, rocket:10},
    grenades:cfg.grenades, shootCd:0, grenadeCd:0, anim:'idle', animT:0,
    inVehicle:false, deadTimer:0, _deathHandled:false,
    _coyote:0, _jumpBuffer:0
  };
}

function spawnFromDef(def) {
  if (def.type==='enemy') {
    const base = {
      type:'enemy', kind:def.kind, x:def.x, y:def.y, facing:-1, alive:true, hurtT:0, shootCd:rand(0.8,1.6),
      vx:0, vy:0, onGround:false,
      animT:0, shootFlash:0, walkPhase:Math.random()*10, score:GDD.scoring[def.kind]||200
    };
    if (def.kind==='grunt') Object.assign(base,{w:16,h:28,hp:2,vx:-0.35});
    else if (def.kind==='shield') Object.assign(base,{w:18,h:28,hp:5,vx:-0.22});
    else if (def.kind==='rocket') Object.assign(base,{w:16,h:28,hp:3,vx:-0.28});
    else if (def.kind==='turret') Object.assign(base,{w:20,h:20,hp:6,vx:0});
    else if (def.kind==='truck') Object.assign(base,{w:140,h:64,y:GROUND_BASE-64,hp:def.hp||12,vx:-0.55, score:GDD.scoring.truck});
    const cfg=difficultyConfig();
    base.hp=Math.max(1,Math.round(base.hp*cfg.enemyHp));
    base.maxHp=base.hp;
    base.shootCd*=cfg.fireDelay;
    State.entities.push(base);
  } else if (def.type==='pow') {
    State.entities.push({type:'pow', x:def.x, y:def.y, w:14, h:28, alive:true, freed:false, animT:0, bob:0});
  } else if (def.type==='pickup') {
    State.entities.push({type:'pickup', kind:def.kind, x:def.x, y:def.y, w:14, h:14, alive:true, bob:0});
  }
}

function spawnVehicle(x) {
  State.vehicle = {
    type:'vehicle', x, y:GROUND_BASE-42, w:76, h:42, hp:20, maxHp:20, alive:true,
    mounted:false, facing:1, vx:0, shootCd:0, shootFlash:0, damageFlash:0, invuln:0,
    engineCd:0, explodeT:0, anim:'idle', repair:false
  };
  caption(choice(VO.player.mount), 'PLAYER');
  floatText(x, GROUND_BASE-40, 'SCRAP MULE', '#9dffb0');
}

function spawnBoss(missionId) {
  if (State.boss && (State.boss.alive || State.boss.deathTimer!=null)) return;
  AudioFX.alarm();
  // Spawn ahead of the player inside the locked arena, never behind them.
  const x = State.level.length - 120;
  if (missionId===0) {
    State.boss = {type:'boss', id:0, name:GDD.bosses[0], x:Math.min(x,State.level.length-132), y:GROUND_BASE-58, w:124, h:58, hp:55, maxHp:55, alive:true, phase:1, t:0, shootCd:1.2, attackT:0, hurtT:0, facing:-1, anim:'entrance', intro:1.2};
  } else if (missionId===1) {
    State.boss = {type:'boss', id:1, name:GDD.bosses[1], x:Math.min(x,State.level.length-132), y:GROUND_BASE-62, w:124, h:62, hp:70, maxHp:70, alive:true, phase:1, t:0, shootCd:1.2, attackT:0, hurtT:0, facing:-1, anim:'entrance', intro:1.2};
  } else {
    State.boss = {type:'boss', id:2, name:GDD.bosses[2], x:Math.min(x,State.level.length-112), y:GROUND_BASE-82, w:104, h:82, hp:95, maxHp:95, alive:true, phase:1, t:0, shootCd:1.2, attackT:0, hurtT:0, facing:-1, anim:'entrance', intro:1.5};
  }
  const cfg=difficultyConfig();
  State.boss.maxHp=Math.round(State.boss.maxHp*cfg.bossHp);
  State.boss.hp=State.boss.maxHp;
  State.boss.shootCd*=cfg.fireDelay;
  State.boss.phaseFlash=0;
  caption(choice(VO.boss[missionId]), 'BOSS');
  State.hudBanner = State.boss.name.toUpperCase();
  State.bannerTimer = 2.2;
  State.shake = 6;
}

function floatText(x,y,text,col='#fff'){ State.floats.push({x,y,text,col,t:1.1}); }
function burst(x,y,n=10,col='#f0c14a') {
  for (let i=0;i<n;i++) {
    const life=rand(0.3,0.7);
    State.particles.push({
      x,y,vx:rand(-2.2,2.2),vy:rand(-3.5,0.5),t:life,life,col,s:rand(1,3)
    });
  }
}
function impactBurst(x,y,strength=1,col='#fca5a5') {
  const power=clamp(Number(strength)||1,0.5,1.6);
  // Impacts stay physical: compact flecks only. The old yellow star read as
  // a pickup or stun marker and is intentionally gone from every target.
  burst(x,y,Math.round(4+power*4),col);
}
function explosionBurst(x,y,strength=1,col='#f0c14a') {
  const power=clamp(Number(strength)||1,0.6,1.5);
  burst(x,y,Math.round(9+power*8),col);
  const size=Math.round(20+power*12);
  const life=0.24+power*0.07;
  State.particles.push({x:x-size/2,y:y-size/2,vx:0,vy:0,t:life,life,col,s:size,sprite:'boom',gravity:0});
  for (let i=0;i<Math.round(1+power);i++) {
    const smokeLife=0.42+power*0.12;
    const smokeSize=Math.round(7+power*5);
    State.particles.push({
      x:x-smokeSize/2+rand(-7,7),y:y-smokeSize/2+rand(-5,4),
      vx:rand(-0.25,0.25),vy:rand(-0.65,-0.3),t:smokeLife,life:smokeLife,
      col:'#9ca3af',s:smokeSize,sprite:'smoke',gravity:0,grow:5
    });
  }
}
function spawnAmbientParticle(theme, forcedKind=null) {
  const worldX=State.camX+rand(-12,W+12);
  if (theme===0) {
    const life=rand(2.4,4.2);
    State.particles.push({
      x:worldX,y:rand(10,132),vx:rand(-0.12,0.12),vy:rand(0.08,0.22),
      t:life,life,col:'rgba(184,194,204,0.52)',s:rand(1,2),gravity:0,
      ambient:'ash',layer:'ambient'
    });
    return 'ash';
  }
  if (theme===1) {
    const life=rand(2.8,4.8);
    State.particles.push({
      x:worldX,y:rand(18,142),vx:rand(-0.28,-0.08),vy:rand(0.05,0.16),
      t:life,life,col:choice(['#6f8f45','#91a85b','#486f38']),s:rand(2,3),gravity:0,
      ambient:'leaf',layer:'ambient',phase:rand(0,Math.PI*2)
    });
    return 'leaf';
  }
  const kind=forcedKind||(Math.random()<0.58?'steam':'ember');
  if (kind==='steam') {
    const life=rand(1.4,2.6);
    const size=rand(5,8);
    State.particles.push({
      x:worldX,y:GROUND_BASE-rand(1,10),vx:rand(-0.09,0.09),vy:rand(-0.22,-0.11),
      t:life,life,col:'#64748b',s:size,sprite:'smoke',gravity:0,grow:2.2,
      ambient:'steam',layer:'ambient'
    });
  } else {
    const life=rand(0.8,1.6);
    State.particles.push({
      x:worldX,y:GROUND_BASE-rand(0,16),vx:rand(-0.08,0.08),vy:rand(-0.38,-0.18),
      t:life,life,col:choice(['#fb923c','#fbbf24','#ef4444']),s:1,gravity:0,
      ambient:'ember',layer:'ambient'
    });
  }
  return kind;
}
function bossDeathEffect(b, initial=false) {
  if (!b) return null;
  const start=State.particles.length;
  let style='mechanical';
  if (b.id===0) {
    const ex=b.x+rand(b.w*0.18,b.w*0.78);
    const ey=b.y+rand(b.h*0.48,b.h*0.84);
    explosionBurst(ex,ey,initial?1.5:rand(0.72,1.02),'#f59e0b');
    burst(ex,ey,initial?18:8,'#94a3b8');
  } else if (b.id===1) {
    style='inferno';
    const ex=b.x+rand(b.w*0.08,b.w*0.82);
    const ey=b.y+rand(b.h*0.56,b.h*0.88);
    explosionBurst(ex,ey,initial?1.45:rand(0.78,1.08),'#fb923c');
    for (let i=0;i<(initial?8:4);i++) {
      const life=rand(0.55,1.1);
      State.particles.push({x:ex+rand(-12,12),y:ey+rand(-5,5),vx:rand(-0.2,0.2),vy:rand(-0.8,-0.3),t:life,life,col:choice(['#fb923c','#ef4444','#fbbf24']),s:rand(1,2),gravity:0,ambient:'ember'});
    }
  } else {
    style='core';
    const ex=b.x+b.w*0.5+rand(-b.w*0.14,b.w*0.14);
    const ey=b.y+b.h*0.48+rand(-b.h*0.12,b.h*0.12);
    const life=initial?0.42:0.28;
    State.particles.push({x:ex,y:ey,t:life,life,vx:0,vy:0,s:initial?26:rand(14,21),sprite:'corePulse',gravity:0,col:'#67e8f9'});
    burst(ex,ey,initial?24:12,choice(['#67e8f9','#a5f3fc','#f8fafc']));
    const smokeLife=rand(0.5,0.9);
    State.particles.push({x:ex-5,y:ey-5,vx:rand(-0.2,0.2),vy:rand(-0.7,-0.35),t:smokeLife,life:smokeLife,col:'#64748b',s:10,sprite:'smoke',gravity:0,grow:4});
  }
  for (let i=start;i<State.particles.length;i++) State.particles[i].deathStyle=style;
  return style;
}
function caption(line, speaker='RADIO OPS', ttl=2.4) {
  State.caption = {line, speaker, t:ttl};
}
function queueCaption(line, speaker='RADIO OPS', ttl=2.4) {
  State.captionQueue.push({line, speaker, t:ttl});
}
function updateCaption(dt) {
  if (State.caption) {
    State.caption.t -= dt;
    if (State.caption.t <= 0) State.caption = null;
  }
  if (!State.caption && State.captionQueue.length) State.caption = State.captionQueue.shift();
}
let arcadeBest = Number(window.arcadeStorage.getItem('best') || 0);
function addScore(n, x, y) {
  State.score += n;
  if (State.score > arcadeBest) {
    arcadeBest = State.score;
    window.arcadeStorage.setItem('best', arcadeBest);
  }
  if (x!=null) floatText(x,y, `+${n}`, '#f0c14a');
}
function comboMultiplier(combo=State.combo) {
  return Math.min(4, 1 + Math.floor(Math.max(0, combo-1) / 4));
}
function registerKillScore(base, x, y) {
  State.combo = Math.min(99, (State.combo||0) + 1);
  State.comboTimer = 2.8;
  State.maxCombo = Math.max(State.maxCombo||0, State.combo);
  if (State.missionStats) State.missionStats.maxCombo=Math.max(State.missionStats.maxCombo||0,State.combo);
  const multiplier = comboMultiplier();
  addScore(Math.round(base * multiplier), x, y);
  if (State.combo > 1) floatText(x, y-11, `${State.combo} HIT  x${multiplier}`, '#7dd3fc');
}
function breakCombo() {
  State.combo = 0;
  State.comboTimer = 0;
}
function missionObjective(mission=State.mission){ return MISSION_OBJECTIVES[mission]||MISSION_OBJECTIVES[0]; }
function objectiveProgress(stats=State.missionStats, mission=State.mission) {
  const objective=missionObjective(mission);
  if (!stats) return {value:0,target:objective.target,complete:false};
  if (objective.type==='pow') {
    const value=Math.max(0,State.powRescued-stats.powStart);
    return {value,target:objective.target,complete:value>=objective.target};
  }
  if (objective.type==='convoy') {
    const value=stats.convoyDestroyed||0;
    return {value,target:objective.target,complete:value>=objective.target};
  }
  const value=stats.deaths||0;
  return {value,target:0,complete:value===0};
}
function objectiveHudText() {
  const objective=missionObjective();
  const progress=objectiveProgress();
  if (objective.type==='noDeath') return `OBJ NO DEATH ${progress.complete?'OK':'FAIL'}`;
  return `OBJ ${objective.short} ${progress.value}/${progress.target}`;
}
function calculateMissionResult() {
  const stats=State.missionStats||{deaths:0,maxCombo:0,powStart:State.powRescued,killsStart:State.kills,convoyDestroyed:0,sceneryDestroyed:0,time:0};
  const objective=missionObjective();
  const progress=objectiveProgress(stats);
  const hpRatio=State.player?State.player.hp/Math.max(1,State.player.maxHp):0;
  let points=progress.complete?2:0;
  if ((stats.deaths||0)===0) points++;
  if ((stats.maxCombo||0)>=5) points++;
  if (hpRatio>=0.5) points++;
  const rank=points>=5?'S':points>=4?'A':points>=3?'B':'C';
  const rankBonus={S:4000,A:2500,B:1000,C:0}[rank];
  const objectiveBonus=progress.complete?2500:0;
  return {
    rank,points,objective:objective.label,objectiveComplete:progress.complete,
    progress,rankBonus,objectiveBonus,
    bonus:GDD.scoring.clearBonus+rankBonus+objectiveBonus,
    kills:Math.max(0,State.kills-(stats.killsStart||0)),
    pows:Math.max(0,State.powRescued-(stats.powStart||0)),
    deaths:stats.deaths||0,maxCombo:stats.maxCombo||0,scenery:stats.sceneryDestroyed||0
  };
}

// ============================================================
// FLOW CONTROL
// ============================================================
function hardResetCampaign() {
  State.score = 0; State.lives = 3; State.continues = 2; State.mission = 0;
  State.powRescued = 0; State.kills = 0; State.combo = 0; State.comboTimer = 0; State.maxCombo = 0;
  State.missionStats = null; State.missionResult = null;
  State.flags = Object.create(null);
  State.captionQueue = [];
}

function startBrief(m) {
  State.mode = 'BRIEF';
  State.mission = m;
  State.briefIndex = 0;
  State.briefTimer = 0;
  State.captionQueue = [];
  const line = VO.brief[m][0];
  caption(line.line, line.speaker, 99);
  AudioFX.ui();
  Music.play(m);
}

function startMission(m) {
  State.mode = 'PLAY';
  try { AudioFX.ensure(); } catch (e) {}
  State.hitstop = 0;
  State.shake = 0;
  State.clearTimer = 0;
  State.gameOverTimer = 0;
  State.mission = m;
  State.level = buildMission(m);
  State.player = makePlayer();
  State.player.x = 40; State.checkpointX = 40;
  State.combo = 0; State.comboTimer = 0;
  State.missionStats = {powStart:State.powRescued,killsStart:State.kills,deaths:0,convoyDestroyed:0,sceneryDestroyed:0,maxCombo:0,time:0};
  State.missionResult = null;
  State.entities = []; State.bullets = []; State.particles = []; State.floats = [];
  State.vehicle = null; State.boss = null; State.camX = 0; State.spawnCursor = 0;
  State.captionQueue = [];
  State.flags = Object.create(null);
  State.hudBanner = 'MISSION ' + (m+1);
  State.bannerTimer = 1.8;
  State.startCueTimer = 0.9;
  caption(VO.missionStart.line, VO.missionStart.speaker, 1.1);
  queueCaption('BONUS OBJECTIVE: '+missionObjective(m).label, 'RADIO OPS', 2.4);
  AudioFX.clear();
  Music.play(m);
}

function killPlayer() {
  const p = State.player;
  if (!p || !p.alive || p._deathHandled) return;
  p.alive = false;
  p._deathHandled = true;
  if (State.missionStats) State.missionStats.deaths++;
  p.inVehicle = false;
  if (State.vehicle) State.vehicle.mounted = false;
  p.vx = 0; p.vy = 0; p.hp = 0;
  p.deadTimer = 0.75;
  p.anim = 'death';
  State.hitstop = 0;
  AudioFX.die();
  burst(p.x + p.w/2, p.y + p.h/2, 16, GDD.palette.red);
  State.shake = 5;
  State.hudBanner = 'WASTED';
  State.bannerTimer = 0.9;
}

function focusCameraOnPlayer(immediate) {
  const p = State.player;
  if (!p || !State.level) return;
  const maxCam = Math.max(0, State.level.length - W);
  if (State.boss && State.boss.alive) {
    const lock = Math.max(0, State.level.length - W - 20);
    // keep fighter inside boss arena when life lost mid-fight
    if (p.x < lock + 24) {
      p.x = lock + 40;
      p.y = GROUND_BASE - p.h;
    }
    State.camX = lock;
    return;
  }
  const target = clamp(p.x + p.w/2 - W*0.35, 0, maxCam);
  if (immediate) State.camX = target; else State.camX += (target - State.camX) * 0.2;
}

function respawnOrGameOver() {
  State.lives--;
  State.bullets = [];
  State.hitstop = 0;

  // `lives` is the remaining stock shown by the HUD. Reaching zero means the
  // last fighter was just spent; do not create a hidden extra life at 0.
  if (State.lives <= 0) {
    State.lives = 0;
    State.mode = 'GAMEOVER';
    State.gameOverTimer = 0;
    caption(VO.gameOver.line, 'ANNOUNCER', 2.2);
    queueCaption(VO.cont.line, 'ANNOUNCER', 99);
    State.hudBanner = 'GAME OVER';
    State.bannerTimer = 99;
    return;
  }

  const keepX = State.checkpointX;
  State.player = makePlayer();
  State.player.x = keepX;
  State.player.y = GROUND_BASE - State.player.h;
  State.player.invuln = 2.5;
  State.player._deathHandled = false;
  if (State.vehicle) State.vehicle.mounted = false;
  focusCameraOnPlayer(true);
  State.hudBanner = `LIVES x${State.lives}`;
  State.bannerTimer = 1.2;
  caption('Re-linked. Stay mean.', 'RADIO OPS', 1.6);
  AudioFX.ui();
}

function missionClear() {
  State.mode = 'CLEAR';
  State.clearTimer = 0;
  State.missionResult=calculateMissionResult();
  addScore(State.missionResult.bonus);
  caption(VO.missionClear.line, 'ANNOUNCER', 2.5);
  queueCaption(choice(VO.player.win), 'PLAYER', 2.5);
  AudioFX.clear();
}

// ============================================================
// COMBAT HELPERS
// ============================================================
function fireBullet(opts) {
  State.bullets.push({
    x:opts.x, y:opts.y, w:opts.w||4, h:opts.h||2,
    vx:opts.vx, vy:opts.vy||0, dmg:opts.dmg||1,
    from:opts.from, life:opts.life||2, kind:opts.kind||'bullet',
    gravity: !!opts.gravity, gravityAmount:opts.gravityAmount??0.15, radius: opts.radius||0
  });
}

const PLAYER_MUZZLE_ANCHORS = {
  // Logical coordinates measured directly on the approved HQ shooting art.
  // They mark the last barrel pixel immediately before the authored flash.
  shoot:{x:42,y:13.5},
  shootCrouch:{x:24,y:27.3}
};

function playerShotSpriteRect(p, anim) {
  const img=Art.hero(anim,p.facing);
  const native=spriteSize(img,32,40);
  const scale=heroPoseScale(anim);
  const w=Math.round(native.w*scale),h=Math.round(native.h*scale);
  const foot=Math.min(GROUND_BASE,p.y+p.h);
  return {x:Math.round(p.x+p.w/2-w/2),y:Math.round(foot-h),w,h,native};
}

function playerMuzzlePosition(p) {
  const anim=p.crouch?'shootCrouch':'shoot';
  const anchor=PLAYER_MUZZLE_ANCHORS[anim];
  const rect=playerShotSpriteRect(p,anim);
  const scaledX=anchor.x/rect.native.w*rect.w;
  const x=(p.facing||1)>=0 ? rect.x+scaledX : rect.x+rect.w-scaledX;
  const y=rect.y+anchor.y/rect.native.h*rect.h;
  return {x:Math.round(x),y:Math.round(y),anim,rect};
}

function vehicleMuzzlePosition(v) {
  const img=Art.vehicle(v.anim||'idle');
  const rect=spriteRectOnFeet(img,v,80,48);
  // The authored half-track faces left. Anchor both projectile and flash to
  // the same last barrel pixel after mirroring instead of to its hitbox edge.
  const sourceAnchor={x:1,y:12};
  const sx=sourceAnchor.x/rect.native.w*rect.w;
  const sy=sourceAnchor.y/rect.native.h*rect.h;
  const x=(v.facing||1)<0?rect.x+sx:rect.x+rect.w-sx;
  return {x:Math.round(x),y:Math.round(rect.y+sy),rect};
}

function playerShoot() {
  const p = State.player;
  if (p.shootCd>0 || !p.alive) return;
  if (p.inVehicle && State.vehicle) {
    if (State.vehicle.shootCd>0) return;
    State.vehicle.shootCd = 0.09;
    State.vehicle.shootFlash = 0.12;
    const muzzle=vehicleMuzzlePosition(State.vehicle);
    fireBullet({x:muzzle.x, y:muzzle.y, vx:5*State.vehicle.facing, dmg:2, from:'player', kind:'heavy'});
    AudioFX.heavy();
    return;
  }
  const weapon = p.weapon;
  const muzzle=playerMuzzlePosition(p);
  if (weapon==='pistol') {
    p.shootCd = 0.16;
    fireBullet({x:muzzle.x, y:muzzle.y, vx:5.2*p.facing, dmg:1, from:'player', h:8});
    State.particles.push({x:muzzle.x, y:muzzle.y, vx:0.5*p.facing, vy:0, t:0.08, col:'#fff', s:8, sprite:'muzzle'});
    AudioFX.shoot();
  } else if (weapon==='heavy') {
    if (p.ammo.heavy<=0) { p.weapon='pistol'; return; }
    p.ammo.heavy--; p.shootCd = 0.1;
    fireBullet({x:muzzle.x, y:muzzle.y, vx:6*p.facing, dmg:2, from:'player', kind:'heavy', w:5, h:8});
    State.particles.push({x:muzzle.x, y:muzzle.y, vx:0.5*p.facing, vy:0, t:0.08, col:'#fff', s:10, sprite:'muzzle'});
    AudioFX.heavy();
  } else if (weapon==='rocket') {
    if (p.ammo.rocket<=0) { p.weapon='pistol'; return; }
    p.ammo.rocket--; p.shootCd = 0.45;
    fireBullet({x:muzzle.x, y:muzzle.y, vx:3.4*p.facing, dmg:6, from:'player', kind:'rocket', w:6, h:8, radius:28});
    State.particles.push({x:muzzle.x, y:muzzle.y, vx:0.5*p.facing, vy:0, t:0.1, col:'#fff', s:12, sprite:'muzzle'});
    AudioFX.rocket();
  }
  p.anim = p.crouch ? 'shootCrouch' : 'shoot'; p.animT=0;
}

function playerGrenade() {
  const p = State.player;
  if (p.grenadeCd>0 || p.grenades<=0 || !p.alive || p.inVehicle) return;
  p.grenades--; p.grenadeCd=0.6; p.anim='throw'; p.animT=0;
  fireBullet({x:p.x+8, y:p.y+10, vx:2.4*p.facing, vy:-3.2, dmg:5, from:'player', kind:'grenade', w:5, h:5, gravity:true, life:2.5, radius:34});
  AudioFX.grenade();
  caption(choice(VO.player.grenade), 'PLAYER', 1.2);
}

function damagePlayer(dmg=1) {
  const p = State.player;
  if (!p || !p.alive || p.invuln>0) return;
  if (p.inVehicle && State.vehicle && State.vehicle.alive) {
    if (State.vehicle.invuln>0) return;
    breakCombo();
    State.vehicle.hp -= dmg;
    State.vehicle.invuln=0.35; State.vehicle.anim='damage'; State.vehicle.damageFlash=0.18;
    if (State.vehicle.hp <= 0) {
      explosionBurst(State.vehicle.x+State.vehicle.w/2, State.vehicle.y+State.vehicle.h/2, 1.2, '#f0c14a');
      AudioFX.explode(1.2);
      State.vehicle.alive=false; State.vehicle.mounted=false; State.vehicle.anim='explode'; State.vehicle.explodeT=0.45; p.inVehicle=false;
      p.invuln=1.2;
    }
    return;
  }
  breakCombo();
  p.hp -= dmg; p.invuln = 1.35; p.anim='hurt'; p.animT=0;
  AudioFX.hurt(); State.shake=3; State.hitstop=0.025;
  caption(choice(VO.player.hit), 'PLAYER', 1.0);
  if (p.hp <= 0) killPlayer();
}

function damageEnemy(en, dmg, srcX) {
  if (!en.alive) return;
  let shieldBlocked=false;
  if (en.kind==='shield' && srcX != null) {
    // frontal block if shot from front-ish
    if ((en.facing<0 && srcX < en.x+8) || (en.facing>0 && srcX > en.x+en.w-8)) {
      // blocked partially
      dmg = Math.max(0.25, dmg*0.25);
      shieldBlocked=true;
      const blockX=en.facing<0?en.x:en.x+en.w;
      // Metal flecks only: the star-shaped hit sprite looked like a bonus or
      // stun effect even though the shield was simply deflecting a round.
      burst(blockX, en.y+10, 4, '#cbd5e1');
    }
  }
  en.hp -= dmg; en.hurtT = 0.12;
  if (en.kind==='truck') {
    // Armoured bodywork should throw compact metal flecks, never the
    // star-shaped infantry hit marker.
    burst(en.x+en.w/2, en.y+en.h/2, 5, '#94a3b8');
  } else if (!shieldBlocked) impactBurst(en.x+en.w/2, en.y+en.h/2, Math.min(1.35,0.65+dmg*0.2), '#fca5a5');
  if (en.hp <= 0) {
    if (en.kind==='truck' && State.missionStats) State.missionStats.convoyDestroyed++;
    en.alive=false; en.deadT=0.35; en.vx=0; State.kills++; registerKillScore(en.score||200, en.x, en.y);
    const deathPower=en.kind==='truck'?1.25:0.82;
    AudioFX.explode(deathPower); explosionBurst(en.x+en.w/2, en.y+en.h/2, deathPower, '#f0c14a'); State.shake=2;
    // classic MS-ish weapon candy
    if (Math.random() < difficultyConfig().dropChance) {
      const kind = choice(['heavy','rocket','grenade','heal']);
      State.entities.push({type:'pickup', kind, x:en.x, y:GROUND_BASE-16, w:14,h:14,alive:true,bob:0});
    }
  }
}

function enterBossPhase(b, phase) {
  if (!b || phase<=b.phase) return;
  b.phase=phase;
  b.phaseFlash=0.55;
  b.shootCd=Math.min(b.shootCd,0.35*difficultyConfig().fireDelay);
  b.attackT=0.3;
  State.hudBanner=`BOSS PHASE ${phase}`;
  State.bannerTimer=0.9;
  State.shake=Math.max(State.shake,4);
  impactBurst(b.x+b.w/2,b.y+b.h/2,1.35,phase===3?'#fb7185':'#f0c14a');
  const lines = phase===2
    ? ['Armor split. Turret overdrive!','River guns unlocked!','Core shields collapsing!']
    : ['Ironclad overdrive!','Gunboat final barrage!','Core meltdown protocol!'];
  caption(lines[b.id], 'RADIO OPS', 1.8);
  AudioFX.alarm();
}

function damageBoss(dmg) {
  const b = State.boss;
  if (!b||!b.alive||b.intro>0) return;
  b.hp -= dmg; b.hurtT=0.12; b.anim='hurt'; impactBurst(b.x+b.w/2,b.y+b.h/2,1.1,'#f0c14a'); State.shake=2;
  if (b.hp>0) {
    const ratio=b.hp/b.maxHp;
    const desired=ratio<=0.30?3:ratio<=0.65?2:1;
    if (desired>b.phase) enterBossPhase(b,desired);
  }
  if (b.hp <= 0) {
    const durations=[1.7,2.15,1.9];
    b.hp=0; b.alive=false; b.anim='defeat'; b.deathTimer=durations[b.id]||1.8; b.deathDuration=b.deathTimer; b.deathFxCd=0; State.kills++;
    registerKillScore(b.id===2?GDD.scoring.finalBoss:GDD.scoring.miniboss, b.x, b.y);
    AudioFX.explode(1.45); bossDeathEffect(b,true); State.shake=8;
    if (b.id===2) caption(VO.boss[2][6], 'BOSS', 2.8);
    else caption(choice(VO.boss[b.id]), 'BOSS', 1.5);
  }
}

function explodeAt(x,y,r,dmg, from) {
  const power=clamp((r||24)/30,0.8,1.35);
  AudioFX.explode(power); explosionBurst(x,y,power,'#f0c14a'); State.shake = Math.min(8, 3+r/10);
  State.entities.forEach(en => {
    if (!en.alive || en.type!=='enemy') return;
    const cx=en.x+en.w/2, cy=en.y+en.h/2;
    if (Math.hypot(cx-x,cy-y) < r+10) damageEnemy(en, dmg, x);
  });
  if (State.boss && State.boss.alive) {
    const b=State.boss; const cx=b.x+b.w/2, cy=b.y+b.h/2;
    if (Math.hypot(cx-x,cy-y) < r+16) damageBoss(dmg);
  }
  // props
  State.level.solids.forEach(s => {
    if (!s.alive) return;
    if (s.type!=='crate' && s.type!=='barrel' && s.type!=='weak') return;
    const cx=s.x+s.w/2, cy=s.y+s.h/2;
    if (Math.hypot(cx-x,cy-y) < r+8) hitSolid(s, dmg);
  });
  (State.level.decors||[]).forEach(d=>{
    if (!d.alive || !d.hp) return;
    const bounds=decorBounds(d);
    const cx=bounds.x+bounds.w/2,cy=bounds.y+bounds.h/2;
    if (Math.hypot(cx-x,cy-y)<r+10) hitDecor(d,dmg,from,x<d.x?1:-1);
  });
  if (from!=='player') {
    const p=State.player; if (p&&p.alive) {
      const cx=p.x+p.w/2, cy=p.y+p.h/2;
      if (Math.hypot(cx-x,cy-y) < r) damagePlayer(1);
    }
  }
}

function decorBounds(d) {
  const img=Art.decor(d.kind);
  const native=spriteSize(img,24,24);
  const scale=decorScaleFor(d.kind);
  const w=Math.round(native.w*scale),h=Math.round(native.h*scale);
  return {x:d.x-(w>>1),y:(d.y||GROUND_BASE)-h+1,w,h};
}

function hitDecor(d,dmg=1,from='player',impactDir=1) {
  if (!d || !d.alive || !d.hp) return false;
  d.hp-=dmg;
  impactBurst(d.x,(d.y||GROUND_BASE)-18,0.65,d.kind==='lamp'?'#cbd5e1':'#c4a574');
  if (d.hp>0) return true;
  d.alive=false; d.destroyT=0.48; d.fallDir=impactDir||1;
  if (State.missionStats) State.missionStats.sceneryDestroyed++;
  if (from==='player') addScore(25,d.x,(d.y||GROUND_BASE)-24);
  if (d.kind==='drums') {
    d.destroyT=0;
    explodeAt(d.x,(d.y||GROUND_BASE)-14,28,3,from);
  } else {
    AudioFX.explode(0.7);
    explosionBurst(d.x,(d.y||GROUND_BASE)-20,0.7,d.kind==='lamp'?'#94a3b8':'#a98456');
  }
  return true;
}

function updateDecors(dt) {
  for (const d of State.level?.decors||[]) {
    if (!d.alive && d.destroyT>0) d.destroyT=Math.max(0,d.destroyT-dt);
  }
}

function hitSolid(s, dmg=1) {
  if (!s.alive) return false;
  if (s.type==='ground' || (s.type==='wall' && s.hp>100)) return true;
  s.hp -= dmg;
  if (s.hp <= 0) {
    s.alive=false;
    if (s.type==='barrel') {
      addScore(GDD.scoring.barrel, s.x, s.y);
      explodeAt(s.x+8,s.y+8,36,4,'world');
    } else {
      burst(s.x+8,s.y+8,8,'#c4a574');
      if (s.type==='crate') addScore(GDD.scoring.crate, s.x, s.y);
    }
    // chance item from crate
    if (s.type==='crate' && Math.random()<0.5) {
      const kind = choice(['heavy','rocket','grenade','heal','heal']);
      State.entities.push({type:'pickup', kind, x:s.x, y:GROUND_BASE-16, w:14,h:14,alive:true,bob:0});
    }
  }
  return true;
}

// ============================================================
// PHYSICS
// ============================================================
function moveActor(ent, dt, ignorePlatforms=false) {
  ent.vy += 0.18; // gravity
  if (ent.vy > 6) ent.vy = 6;
  // horizontal
  ent.x += ent.vx;
  resolveSolids(ent, true);
  // vertical
  ent.onGround = false;
  ent.y += ent.vy;
  resolveSolids(ent, false);
}

function resolveSolids(ent, horizontal) {
  const solids = State.level.solids;
  for (let i=0;i<solids.length;i++) {
    const s = solids[i];
    if (!s.alive) continue;
    if (!aabb(ent,s)) continue;
    if (s.type==='crate' || s.type==='barrel' || s.type==='weak') {
      // treat as solid but sniper destructible; act solid for collision
    }
    const isPlat = s.type==='platform';
    if (horizontal) {
      if (isPlat) continue;
      if (ent.vx > 0) ent.x = s.x - ent.w;
      else if (ent.vx < 0) ent.x = s.x + s.w;
      ent.vx = 0;
    } else {
      if (ent.vy > 0) {
        const prevBottom = ent.y + ent.h - ent.vy;
        if (!isPlat || prevBottom <= s.y + 2) {
          ent.y = s.y - ent.h; ent.vy = 0; ent.onGround = true;
        }
      } else if (ent.vy < 0 && !isPlat) {
        ent.y = s.y + s.h; ent.vy = 0;
      }
    }
  }
  // level bounds
  if (ent.x < 0) { ent.x = 0; ent.vx = 0; }
  if (State.level && ent.x > State.level.length-ent.w) { ent.x = State.level.length-ent.w; ent.vx=0; }
  // hard floor only when not rising
  if (!horizontal && ent.vy >= 0 && ent.y + ent.h > GROUND_BASE) {
    ent.y = GROUND_BASE - ent.h; ent.vy = 0; ent.onGround = true;
  }
  if (ent.y > H+80 && ent.type==='player') killPlayer();
}

// ============================================================
// UPDATE SYSTEMS
// ============================================================
function updateSpawns() {
  const viewR = State.camX + W + 40;
  const sp = State.level.spawns;
  while (State.level.spawnIndex < sp.length && sp[State.level.spawnIndex].x < viewR) {
    spawnFromDef(sp[State.level.spawnIndex++]);
  }
  // triggers
  const p = State.player;
  State.level.triggers.forEach(tr => {
    if (tr.done) return;
    if (p.x + p.w < tr.x) return;
    tr.done = true;
    if (tr.type==='checkpoint') {
      State.checkpointX = tr.x;
      p.hp=Math.min(p.maxHp,p.hp+2);
      p.grenades=Math.min(12,p.grenades+1);
      floatText(tr.x, GROUND_BASE-50, 'CHECKPOINT +HP', '#9ae6b4');
      AudioFX.ui();
    }
    if (tr.type==='vehicle') spawnVehicle(tr.x + 20);
    if (tr.type==='boss') spawnBoss(State.mission);
    if (tr.type==='heli') {
      State.flags.heli = 4.5;
      State.flags.heliBombCd = 0.8;
      caption('Hostile bird on the skyline — keep low!', 'RADIO OPS', 2.2);
    }
  });
}

function setPlayerCrouch(p, wantsCrouch) {
  const standingH = p.standingH || 32;
  const crouchH = p.crouchH || 20;
  const foot = p.y + p.h;
  if (wantsCrouch) {
    if (p.h !== crouchH) { p.h = crouchH; p.y = foot - crouchH; }
    p.crouch = true;
    return;
  }
  if (p.h !== standingH) {
    const candidate = {x:p.x, y:foot-standingH, w:p.w, h:standingH};
    const blocked = State.level.solids.some(s => s.alive && s.type!=='platform' && aabb(candidate,s));
    if (blocked) { p.crouch = true; return; }
    p.h = standingH;
    p.y = foot - standingH;
  }
  p.crouch = false;
}

function updatePlayer(dt) {
  const p = State.player;
  if (!p) return;
  if (!p.alive) {
    return; // death timer handled in updatePlay
  }
  if (p.invuln>0) p.invuln -= dt;
  if (p.shootCd>0) p.shootCd -= dt;
  if (p.grenadeCd>0) p.grenadeCd -= dt;
  p.animT += dt;

  // vehicle mount/dismount
  if (State.vehicle && State.vehicle.alive) {
    const v = State.vehicle;
    v.shootCd = Math.max(0, v.shootCd-dt);
    v.shootFlash = Math.max(0, v.shootFlash-dt);
    v.damageFlash = Math.max(0, v.damageFlash-dt);
    v.invuln = Math.max(0, v.invuln-dt);
    v.engineCd = Math.max(0, v.engineCd-dt);
    if (!p.inVehicle && Math.abs((p.x+p.w/2)-(v.x+v.w/2))<v.w/2+8 && Math.abs(p.y-v.y)<36 && (tapped('arrowup')||tapped('w')||jumpTap())) {
      p.inVehicle = true; v.mounted = true; AudioFX.pickup(); caption(choice(VO.player.mount),'PLAYER',1.2);
    } else if (p.inVehicle && crouchHeld() && jumpTap()) {
      p.inVehicle = false; v.mounted = false; p.y = v.y - p.h; p.invuln=0.6;
    }
    if (!p.inVehicle && v.damageFlash<=0 && v.shootFlash<=0) v.anim='idle';
  }

  if (p.inVehicle && State.vehicle && State.vehicle.alive) {
    const v = State.vehicle;
    const mx = moveX();
    v.facing = mx!==0 ? Math.sign(mx) || v.facing : v.facing;
    v.vx = mx * 2.2;
    // simple vehicle physics on ground rails
    v.x += v.vx;
    v.x = clamp(v.x, 0, State.level.length-v.w);
    v.y = GROUND_BASE - v.h;
    // ram enemies
    State.entities.forEach(en => {
      if (en.alive && en.type==='enemy' && aabb(v,en)) damageEnemy(en, 999, v.x);
    });
    // repair crate-like pickup already handled
    p.x = v.x+v.w/2-p.w/2; p.y = v.y-8; p.facing = v.facing;
    if (shootHeld()) playerShoot();
    if (v.damageFlash>0) v.anim='damage';
    else if (v.shootFlash>0) v.anim='shoot';
    else if (Math.abs(v.vx)>0.2) v.anim='move';
    else v.anim='idle';
    if (Math.abs(v.vx)>0.2 && v.engineCd<=0) { AudioFX.engine(); v.engineCd=0.18; }
    return;
  }

  setPlayerCrouch(p, crouchHeld() && p.onGround);
  const mx = moveX();
  if (!p.crouch) {
    p.vx = mx * 1.55;
    if (mx) p.facing = Math.sign(mx);
  } else p.vx = mx * 0.5;
  // Forgiving arcade jump: coyote time after leaving an edge plus a short
  // input buffer before landing, so valid button presses are never swallowed.
  if (jumpTap() && !p.crouch) p._jumpBuffer = 0.12;
  else p._jumpBuffer = Math.max(0, (p._jumpBuffer||0)-dt);
  if (p.onGround) p._coyote = 0.12; else p._coyote = Math.max(0,(p._coyote||0)-dt);
  if ((p._jumpBuffer||0)>0 && !p.crouch && (p.onGround || (p._coyote||0)>0)) {
    p.vy = -4.35; p.onGround=false; p._coyote=0; p._jumpBuffer=0; p._jumpLock=0.1; AudioFX.jump();
  }
  if (p._jumpLock) p._jumpLock=Math.max(0,p._jumpLock-dt);
  moveActor(p, dt);
  if (p.onGround && p.vy===0 && Math.abs(p._prevVy||0)>1.5) AudioFX.land();
  p._prevVy = p.vy;

  if (shootHeld()) playerShoot();
  if (grenadeTap()) playerGrenade();
  // running dust
  if (p.onGround && Math.abs(p.vx)>0.4 && Math.random()<0.25) {
    State.particles.push({x:p.x+p.w/2, y:p.y+p.h-1, vx:-p.facing*rand(0.2,0.8), vy:-rand(0.2,0.8), t:0.25, col:'#c2b280', s:2});
  }

  // anim
  if (p.anim==='throw' && p.animT<0.25) {}
  else if (p.anim==='hurt' && p.animT<0.2) {}
  else if (!p.onGround) p.anim = p.vy<0 ? 'jump' : 'fall';
  else if (p.crouch) p.anim = shootHeld() ? 'shootCrouch' : 'crouch';
  else if (Math.abs(p.vx)>0.05 || keys['arrowright'] || keys['d'] || keys['arrowleft'] || keys['a']) { if (Math.abs(p.vx)<0.05) { const dir = (keys['arrowleft']||keys['a']) ? -1 : 1; p.vx = dir * 1.55; p.facing = dir; } const f = Math.floor(p.animT*10)%4; p.anim = ['run1','run2','run3','run4'][f]; }
  else p.anim = shootHeld() ? 'shoot' : 'idle';

  // auto weapon fallback
  if (p.weapon==='heavy' && p.ammo.heavy<=0) p.weapon='pistol';
  if (p.weapon==='rocket' && p.ammo.rocket<=0) p.weapon='pistol';
}

function updateEnemies(dt) {
  const p = State.player;
  State.entities.forEach(en => {
    if (!en.alive) {
      if (en.type==='enemy' && en.deadT>0) en.deadT-=dt;
      return;
    }
    en.animT = (en.animT||0) + dt;
    if (en.hurtT>0) en.hurtT -= dt;
    if (en.type==='pow') {
      en.bob += dt*4;
      const rescuer = p && p.inVehicle && State.vehicle && State.vehicle.alive ? State.vehicle : p;
      if (!en.freed && p && p.alive && rescuer && aabb(rescuer,en)) {
        en.freed = true;
        en.runT = 1.6;
        en.vx = 1.6;
        State.powRescued++;
        p.hp=Math.min(p.maxHp,p.hp+1);
        addScore(GDD.scoring.pow, en.x, en.y);
        AudioFX.rescue(); caption(choice(VO.pow), 'POW', 1.2);
        burst(en.x+8,en.y+8,12,'#f0c14a');
        floatText(en.x, en.y-8, 'THANK YOU!  +HP', '#fde68a');
      }
      if (en.freed) {
        en.x += (en.vx||1.6);
        en.y = GROUND_BASE - en.h;
        en.runT -= dt;
        if (en.runT <= 0 || en.x > State.camX + W + 20) en.alive = false;
      }
      return;
    }
    if (en.type==='pickup') {
      en.bob += dt*5;
      if (p && p.alive && aabb(p,en)) {
        en.alive=false; AudioFX.pickup();
        if (en.kind==='heavy') { p.weapon='heavy'; p.ammo.heavy += 40; floatText(en.x,en.y,GDD.weapons.heavy.toUpperCase(),'#93c5fd'); }
        if (en.kind==='rocket') { p.weapon='rocket'; p.ammo.rocket += 5; floatText(en.x,en.y,GDD.weapons.rocket.toUpperCase(),'#fca5a5'); }
        if (en.kind==='grenade') { p.grenades += 3; floatText(en.x,en.y,'+GRENADES','#fde68a'); }
        if (en.kind==='heal') { p.hp = Math.min(p.maxHp, p.hp+2); floatText(en.x,en.y,'+HP','#86efac'); }
        if (en.kind==='repair' && State.vehicle) { State.vehicle.hp = State.vehicle.maxHp; floatText(en.x,en.y,'REPAIRED','#86efac'); }
      }
      return;
    }
    // enemy AI — denser MS-like infantry behavior
    if (!p || !p.alive) return;
    if (en.shootFlash>0) en.shootFlash -= dt;
    const dx = (p.x - en.x);
    const adx = Math.abs(dx);
    en.facing = dx>=0 ? 1 : -1;
    en.walkPhase = (en.walkPhase||0) + dt * (en.kind==='shield'?6:9);
    if (en.kind==='turret') {
      en.shootCd -= dt;
      if (en.shootCd<=0 && adx<210) {
        en.shootCd = 1.35*difficultyConfig().fireDelay;
        en.shootFlash = 0.12;
        // Standard rounds pass above a crouched player; rockets remain the
        // pressure tool that must be jumped or outrun.
        fireBullet({x:en.x+(en.facing>0?20:0), y:GROUND_BASE-24, vx:2.0*en.facing, dmg:1, from:'enemy'});
        State.particles.push({x:en.x+(en.facing>0?22: -2), y:en.y+6, vx:0, vy:-0.2, t:0.2, col:'#ccc', s:10, sprite:'smoke'});
        AudioFX.shoot();
      }
      return;
    }
    if (en.kind!=='truck') {
      // advance, short pause to shoot, hop sometimes+
      let speed = (en.kind==='shield'?0.36: en.kind==='rocket'?0.42:0.50)*difficultyConfig().enemySpeed;
      if (adx < 70 && en.kind!=='rocket') speed *= 0.2; // hold ground to fire
      if (adx > 14) en.vx = Math.sign(dx)*speed; else en.vx = 0;
      // occasional hop for juiceblockchain
      if (en.onGround && Math.random()<0.002 && adx<180) en.vy = -2.8;
      moveActor(en, dt);
    } else {
      en.facing=Math.sign(en.vx)||-1;
      en.x += en.vx;
      // spray while moving
      en.shootCd -= dt;
      if (en.shootCd<=0) {
        en.shootCd = 1.0*difficultyConfig().fireDelay;
        fireBullet({x:en.x+(en.facing>0?en.w:0), y:en.y+12, vx:2.2*en.facing, dmg:1, from:'enemy'});
        en.shootFlash = 0.1;
        AudioFX.shoot();
      }
      if (en.x < State.camX - 100) en.alive=false;
      // residual smoke
      if (Math.random()<0.08) State.particles.push({x:en.x+8,y:en.y+6,vx:-0.4,vy:-0.5,t:0.4,col:'#999',s:8,sprite:'smoke'});
      if (p && p.alive && aabb(p,en) && !p.inVehicle) damagePlayer(1);
      return;
    }
    en.shootCd -= dt;
    const range = en.kind==='rocket'?210: en.kind==='shield'?145:165;
    if (en.shootCd<=0 && adx<range && Math.abs((p.y||0)-en.y)<80) {
      en.shootCd = (en.kind==='rocket'?1.8: en.kind==='shield'?1.5:1.15)*difficultyConfig().fireDelay;
      en.shootFlash = 0.12;
      if (en.kind==='rocket') {
        fireBullet({x:en.x+8, y:en.y+8, vx:2.1*en.facing, vy:-0.3, dmg:1, from:'enemy', kind:'rocket', w:6,h:4, gravity:false, radius:20});
        AudioFX.rocket();
      } else if (en.kind==='shield') {
        // peeks shoot slower bullets
        fireBullet({x:en.x+(en.facing>0?16:0), y:GROUND_BASE-24, vx:1.7*en.facing, dmg:1, from:'enemy'});
        AudioFX.shoot();
      } else {
        // One readable round instead of an overlapping two-shot burst.
        fireBullet({x:en.x+(en.facing>0?16:0), y:GROUND_BASE-24, vx:2.2*en.facing, dmg:1, from:'enemy'});
        AudioFX.shoot();
      }
      State.particles.push({x:en.x+(en.facing>0?en.w:0), y:en.y+10, vx:0.4*en.facing, vy:-0.3, t:0.18, col:'#ddd', s:10, sprite:'smoke'});
    }
    if (p && p.alive && aabb(p,en) && !p.inVehicle) damagePlayer(1);
    if (p && p.inVehicle && State.vehicle && aabb(State.vehicle,en)) damageEnemy(en,2,State.vehicle.x);
  });
  State.entities = State.entities.filter(e => e.alive || (e.type==='enemy' && e.deadT>0));
}

function resolveBossContact(p,b) {
  if (!p || !p.alive) return;
  if (p.inVehicle && State.vehicle && State.vehicle.alive && aabb(State.vehicle,b)) {
    const v = State.vehicle;
    damageBoss(0.2);
    v.x = (v.x+v.w/2 < b.x+b.w/2) ? b.x-v.w : b.x+b.w;
    v.vx = 0;
    p.x = v.x+v.w/2-p.w/2;
  } else if (!p.inVehicle && aabb(p,b)) {
    damagePlayer(1);
    p.x = (p.x+p.w/2 < b.x+b.w/2) ? b.x-p.w : b.x+b.w;
    p.vx = 0;
  }
}

function bossCannonPivot(b) {
  const artW=b.id===2?112:132;
  const artH=b.id===2?88:b.id===1?72:63;
  const artX=b.x+(b.w-artW)/2;
  const artY=b.y+b.h-artH;
  if (b.id===0) return {x:artX+79,y:artY+11};
  if (b.id===1) return {x:artX+42,y:artY+24};
  return {x:artX+54,y:artY+40};
}

function bossMuzzlePosition(b) {
  const pivot=bossCannonPivot(b);
  const defaultAngle=(b.facing||-1)<0?Math.PI:0;
  const angle=Number.isFinite(b.aimAngle)?b.aimAngle:defaultAngle;
  const length=b.id===0?43:b.id===1?28:32;
  // Boss 0 keeps the beautifully authored cannon.  Its muzzle remains fixed
  // at the source-art barrel mouth while only the projectile trajectory aims.
  if (b.id===0) return {x:pivot.x-43,y:pivot.y,angle,pivot};
  return {
    x:pivot.x+Math.cos(angle)*length,
    y:pivot.y+Math.sin(angle)*length,
    angle,
    pivot
  };
}

function updateBoss(dt) {
  const b = State.boss; if (!b) return;
  if (!b.alive) {
    if (b.deathTimer != null) {
      b.deathFxCd=(b.deathFxCd||0)-dt;
      if (b.deathFxCd<=0) {
        b.deathFxCd=b.id===0?0.14:b.id===1?0.17:0.105;
        bossDeathEffect(b,false);
        State.shake=Math.max(State.shake,4);
      }
      b.deathTimer -= dt;
      if (b.deathTimer <= 0) { b.deathTimer = null; if (State.mode==='PLAY') missionClear(); }
    }
    return;
  }
  const p = State.player;
  b.t += dt;
  if (b.intro>0) { b.intro -= dt; b.anim='entrance'; resolveBossContact(p,b); return; }
  if (b.phaseFlash>0) b.phaseFlash=Math.max(0,b.phaseFlash-dt);
  if (b.hurtT>0) { b.hurtT=Math.max(0,b.hurtT-dt); b.anim='hurt'; }
  else if (b.attackT>0) { b.attackT=Math.max(0,b.attackT-dt); b.anim='attack'; }
  else b.anim=(b.id===2 && b.phase>=2) ? 'phase2' : 'idle';
  b.shootCd -= dt;
  // health-based taunts occasional
  if (b.t > 3 && Math.random()<0.002) caption(choice(VO.boss[b.id].slice(0,3)), 'BOSS', 1.5);

  if (b.id===0) {
    b.x += Math.sin(b.t*(1.2+b.phase*0.2))*(0.45+b.phase*0.12) - 0.12;
    b.x = clamp(b.x, State.level.length-300, State.level.length-b.w-20);
    b.y = GROUND_BASE - b.h - Math.abs(Math.sin(b.t*3))*1.5;
    const pivot=bossCannonPivot(b);
    const mouth={x:pivot.x-43,y:pivot.y};
    const targetX=p&&p.alive?p.x+p.w/2:b.x-120;
    const targetY=p&&p.alive?p.y+p.h*0.45:mouth.y;
    let aim=Math.atan2(targetY-mouth.y,targetX-mouth.x);
    if (aim<0) aim+=Math.PI*2;
    b.aimAngle=clamp(aim,Math.PI-0.55,Math.PI+0.55);
    if (b.shootCd<=0) {
      const rate=b.phase===1?1.12:b.phase===2?0.84:0.62;
      b.shootCd = rate*difficultyConfig().fireDelay; b.attackT=0.16; b.anim='attack';
      b.attackCount=(b.attackCount||0)+1;
      const muzzle=bossMuzzlePosition(b);
      const rounds=b.phase;
      for(let i=0;i<rounds;i++) {
        const spread=muzzle.angle+(i-(rounds-1)/2)*0.13;
        fireBullet({x:muzzle.x, y:muzzle.y, vx:Math.cos(spread)*2.35, vy:Math.sin(spread)*2.35, dmg:1, from:'enemy', kind:i===0?'heavy':'bullet'});
      }
      if (b.phase===3 && b.attackCount%3===0) {
        fireBullet({x:b.x+24,y:b.y+4,vx:-1.15,vy:-2.6,dmg:1,from:'enemy',kind:'rocket',gravity:true,radius:18,life:3});
      }
    }
  } else if (b.id===1) {
    b.x += Math.sin(b.t*(0.8+b.phase*0.18))*(0.3+b.phase*0.12);
    b.x = clamp(b.x, State.level.length-300, State.level.length-b.w-20);
    if (b.shootCd<=0) {
      const rate=b.phase===1?1.0:b.phase===2?0.76:0.58;
      b.shootCd = rate*difficultyConfig().fireDelay; b.attackT=0.16; b.anim='attack';
      b.attackCount=(b.attackCount||0)+1;
      const lanes=b.phase===1?2:3;
      for (let i=0;i<lanes;i++) fireBullet({x:b.x, y:b.y+8+i*8, vx:-2.05-i*0.12, dmg:1, from:'enemy'});
      if ((b.phase===2&&b.attackCount%3===0)||(b.phase===3&&b.attackCount%2===0)) {
        const tx=p&&p.alive?p.x:b.x-100;
        fireBullet({x:b.x+24, y:b.y, vx:clamp((tx-b.x)/85,-1.6,-0.7), vy:-2.5, dmg:1, from:'enemy', kind:'rocket', gravity:true, radius:20, life:3});
      }
    }
  } else {
    // multi pattern final
    const rate = b.phase===1 ? 1.05 : b.phase===2 ? 0.8 : 0.58;
    b.x = clamp(b.x + Math.sin(b.t*0.7)*0.35, State.level.length-300, State.level.length-b.w-20);
    if (b.shootCd<=0) {
      b.shootCd = rate*difficultyConfig().fireDelay; b.attackT=0.16; b.anim='attack';
      b.attackCount=(b.attackCount||0)+1;
      const bolts = b.phase===1?1:b.phase===2?2:3;
      const ox = b.x+b.w/2, oy = b.y+20;
      const tx = p && p.alive ? p.x+p.w/2 : b.x-120;
      const ty = p && p.alive ? p.y+p.h/2 : oy;
      const aim = Math.atan2(ty-oy, tx-ox);
      for (let i=0;i<bolts;i++) {
        const ang = aim + (i-(bolts-1)/2)*0.18;
        fireBullet({x:ox, y:oy, vx:Math.cos(ang)*1.8, vy:Math.sin(ang)*1.8, dmg:1, from:'enemy', life:3});
      }
      if (b.phase>=2 && (b.phase===3 || b.attackCount%3===0)) {
        fireBullet({x:b.x+10, y:b.y+40, vx:-1.8, dmg:1, from:'enemy', kind:'rocket', radius:20});
        fireBullet({x:b.x+b.w-10, y:b.y+40, vx:1.8, dmg:1, from:'enemy', kind:'rocket', radius:20});
      }
    }
    // spawn support drones occasionally
    if (b.phase>=2 && Math.random()<(b.phase===3?0.0016:0.0008)) eSafeSpawn('grunt', b.x-30, GROUND_BASE-28);
  }
  resolveBossContact(p,b);
}

function eSafeSpawn(kind,x,y){
  spawnFromDef({type:'enemy', kind, x, y});
}

function updateBullets(dt) {
  const p = State.player;
  for (const b of State.bullets) {
    if (b.gravity) b.vy += b.gravityAmount;
    b.x += b.vx; b.y += b.vy; b.life -= dt;
    if (b.kind==='rocket' && Math.random()<0.35) {
      State.particles.push({x:b.x,y:b.y+1,vx:-Math.sign(b.vx||1)*0.4,vy:rand(-0.2,0.2),t:0.15,col:'#bbb',s:6,sprite:'smoke'});
    }
    // collide solids
    for (const s of State.level.solids) {
      if (!s.alive) continue;
      // Platforms are one-way actor surfaces. Their lowered walk collision
      // must not become an invisible wall for bullets travelling underneath.
      if (s.type==='platform') continue;
      if (!aabb(b,s)) continue;
      if (s.type==='crate'||s.type==='barrel'||s.type==='weak') {
        hitSolid(s, b.dmg||1);
      }
      if (b.kind==='grenade' || b.kind==='rocket' || b.kind==='bomb') explodeAt(b.x,b.y,b.radius||24,b.dmg,b.from);
      b.life = 0; break;
    }
    if (b.life>0) {
      for (const d of State.level.decors||[]) {
        if (!d.alive || !d.hp) continue;
        if (Math.abs(d.x-b.x)>80) continue;
        if (!aabb(b,decorBounds(d))) continue;
        hitDecor(d,b.dmg||1,b.from,Math.sign(b.vx)||1);
        if (b.kind==='grenade'||b.kind==='rocket'||b.kind==='bomb') explodeAt(b.x,b.y,b.radius||24,b.dmg,b.from);
        b.life=0; break;
      }
    }
    if (b.life<=0) continue;
    if (b.from==='player') {
      for (const en of State.entities) {
        if (!en.alive || en.type!=='enemy') continue;
        if (!aabb(b,en)) continue;
        if (b.kind==='grenade'||b.kind==='rocket') explodeAt(b.x,b.y,b.radius||24,b.dmg,'player');
        else damageEnemy(en, b.dmg, b.x);
        b.life=0; break;
      }
      if (b.life>0 && State.boss && State.boss.alive && aabb(b,State.boss)) {
        if (b.kind==='grenade'||b.kind==='rocket') explodeAt(b.x,b.y,b.radius||24,b.dmg,'player');
        else damageBoss(b.dmg);
        b.life=0;
      }
    } else if (p && p.alive) {
      const target = p.inVehicle && State.vehicle ? State.vehicle : p;
      if (aabb(b, target)) {
        if (b.kind==='rocket' || b.kind==='bomb') explodeAt(b.x,b.y,b.radius||20,b.dmg,'enemy');
        else damagePlayer(b.dmg||1);
        b.life=0;
      }
    }
  }
  State.bullets = State.bullets.filter(b => b.life>0 && b.y < H+40 && b.x>State.camX-40 && b.x<State.camX+W+40);
}

function updateParticles(dt) {
  const frame=Math.max(0,dt*60);
  State.particles.forEach(p => {
    const drift=p.ambient==='leaf'?Math.sin((State.runTime||0)*3.6+(p.phase||0))*0.08:0;
    p.x+=(p.vx+drift)*frame; p.y+=p.vy*frame;
    if (p.sprite==='smoke') {
      p.vx*=Math.pow(0.985,frame);
      p.vy-=0.002*frame;
      if (p.grow) p.s+=p.grow*dt;
    } else p.vy+=(p.gravity??0.1)*frame;
    p.t-=dt;
  });
  State.particles = State.particles.filter(p => p.t>0);
  State.floats.forEach(f => { f.y -= 12*dt; f.t -= dt; });
  State.floats = State.floats.filter(f => f.t>0);
}

function updateCamera(dt) {
  const p = State.player; if (!p) return;
  const target = (p.inVehicle && State.vehicle ? State.vehicle.x+State.vehicle.w/2 : p.x+8) - W*0.35;
  State.camX += (target - State.camX) * Math.min(1, 6*dt);
  State.camX = clamp(State.camX, 0, Math.max(0, State.level.length - W));
  // boss lock keeps arena, but never hides a living player off-screen
  if (State.boss && State.boss.alive) {
    const lock = Math.max(0, State.level.length - W - 20);
    const prefer = (p.inVehicle && State.vehicle ? State.vehicle.x+State.vehicle.w/2 : p.x + 8) - W * 0.45;
    const desired = clamp(Math.max(lock - 20, prefer), lock, Math.max(0, State.level.length - W));
    State.camX += (desired - State.camX) * 0.14;
  }
}

function heliPose() {
  const duration=4.5;
  const remaining=State.flags.heli||0;
  const progress=clamp((duration-remaining)/duration,0,1);
  return {
    x:State.camX+W+140-progress*(W+280),
    y:12+Math.sin(State.runTime*7)*2
  };
}

function updatePlayPresentation(dt) {
  if (State.missionStats) State.missionStats.time=(State.missionStats.time||0)+dt;
  if (State.vehicle && !State.vehicle.alive && State.vehicle.explodeT>0) {
    State.vehicle.explodeT=Math.max(0,State.vehicle.explodeT-dt);
  }
  if (State.startCueTimer>0) {
    State.startCueTimer -= dt;
    if (State.startCueTimer<=0 && State.mode==='PLAY' && State.player && State.player.alive) {
      State.hudBanner='START!';
      State.bannerTimer=0.9;
    }
  }
  if (State.shake>0) State.shake = Math.max(0, State.shake-dt*18);
  if (State.bannerTimer>0) State.bannerTimer -= dt;
  if (State.comboTimer>0) {
    State.comboTimer=Math.max(0,State.comboTimer-dt);
    if (State.comboTimer<=0) State.combo=0;
  }
  updateCaption(dt);
}

function updatePlay(dt) {
  if (State.player && State.player.animT == null) State.player.animT = 0;
  updatePlayPresentation(dt);
  // Always resolve death first so hitstop/boss lock cannot softlock restart
  if (State.player && !State.player.alive) {
    State.player.deadTimer -= dt;
    if (State.boss && !State.boss.alive) updateBoss(dt);
    if (State.mode!=='PLAY') return;
    if (State.player.deadTimer <= 0) respawnOrGameOver();
    return;
  }
  if (State.hitstop > 0) { State.hitstop -= dt; return; }
  updateSpawns();
  updatePlayer(dt);
  updateEnemies(dt);
  updateBoss(dt);
  updateBullets(dt);
  updateDecors(dt);
  updateParticles(dt);
  updateCamera(dt);
  if (State.flags.heli > 0) {
    State.flags.heli -= dt;
    State.flags.heliBombCd=(State.flags.heliBombCd||0)-dt;
    const heli=heliPose();
    if (State.flags.heliBombCd<=0 && heli.x>State.camX-100 && heli.x<State.camX+W-28) {
      State.flags.heliBombCd=1.2;
      fireBullet({x:heli.x+68, y:heli.y+44, vx:-0.18, vy:0.45, w:6, h:8, dmg:1, from:'enemy', kind:'bomb', gravity:true, gravityAmount:0.06, radius:20, life:4});
      AudioFX.grenade();
    }
  }
  // repair pickup spawn near vehicle once
  if (State.vehicle && State.vehicle.alive && !State.flags.repairSpawned && State.vehicle.hp < State.vehicle.maxHp*0.7) {
    State.flags.repairSpawned = true;
    State.entities.push({type:'pickup', kind:'repair', x:State.vehicle.x+80, y:GROUND_BASE-20, w:14,h:14,alive:true,bob:0});
  }
  // Each battlefield has its own quiet atmospheric signature.
  if (Math.random() < (State.level.theme===2?0.075:0.045)) spawnAmbientParticle(State.level.theme);

  // win condition without boss for safety if player walks past end and no boss alive / already beaten
  if (!State.boss && State.player.alive && State.player.x > State.level.length - 80 && State.level.triggers.some(t=>t.type==='boss'&&t.done)) {
    // waiting for boss death handled by damageBoss
  }
}

const OPTION_COUNT=7;
function openOptions(returnMode) {
  State.optionsReturn=returnMode==='PAUSE'?'PAUSE':'TITLE';
  State.optionsIndex=0;
  State.mode='OPTIONS';
  AudioFX.ui();
}
function closeOptions() {
  State.mode=State.optionsReturn||'TITLE';
  AudioFX.ui();
}
function adjustOption(delta) {
  const row=State.optionsIndex;
  if (row===0) setDifficulty(State.difficultyIndex+delta);
  else if (row===1) Music.setLevel(Music.getLevel()+delta*0.05);
  else if (row===2) AudioFX.setLevel(AudioFX.getLevel()+delta*0.05);
  else if (row===3) {
    const values=[0,0.25,0.5];
    let index=values.reduce((best,value,i)=>Math.abs(value-State.shakeScale)<Math.abs(values[best]-State.shakeScale)?i:best,0);
    index=(index+delta+values.length)%values.length;
    State.shakeScale=values[index];
  } else if (row===4) State.scanlines=!State.scanlines;
  else if (row===5) { State.integerScale=!State.integerScale; applyScale(); }
  AudioFX.ui();
}
function updateOptionsInput() {
  if (tapped('escape')||tapped('o')) { closeOptions(); return; }
  if (tapped('arrowup')||tapped('w')) {
    State.optionsIndex=(State.optionsIndex-1+OPTION_COUNT)%OPTION_COUNT; AudioFX.ui();
  }
  if (tapped('arrowdown')||tapped('s')) {
    State.optionsIndex=(State.optionsIndex+1)%OPTION_COUNT; AudioFX.ui();
  }
  const left=tapped('arrowleft')||tapped('a');
  const right=tapped('arrowright')||tapped('d');
  if (left) adjustOption(-1);
  if (right) adjustOption(1);
  if (tapped('enter')||shootTap()) {
    if (State.optionsIndex===6) closeOptions();
    else if (State.optionsIndex===0||State.optionsIndex>=3) adjustOption(1);
  }
}

const TITLE_MENU_COUNT=3;
function activateTitleMenu() {
  if (State.titleMenuIndex===0) {
    hardResetCampaign(); startBrief(0);
  } else if (State.titleMenuIndex===1) {
    openOptions('TITLE');
  } else {
    let escaped=false;
    try {
      if (window.history && window.history.length>1) { window.history.back(); escaped=true; }
    } catch (e) {}
    if (!escaped) caption('CLOSE THIS TAB TO EXIT.', 'SYSTEM', 2.5);
    AudioFX.ui();
  }
}

function update(dt) {
  if (State.mode!=='PAUSE') State.runTime += dt;
  State.titlePulse += dt;
  Music.update(dt);

  // global toggles
  if (tapped('m')) { State.muted = AudioFX.toggleMute(); }
  if (tapped('f1')) State.debug = !State.debug;
  syncModeBadge();
  if (tapped('g')) State.scanlines = !State.scanlines;
  if (tapped('i')) State.integerScale = !State.integerScale;

  if (State.mode==='OPTIONS') { updateOptionsInput(); return; }
  if ((State.mode==='TITLE'||State.mode==='PAUSE')&&tapped('o')) {
    openOptions(State.mode); return;
  }

  if (State.mode==='TITLE') {
    if (tapped('arrowup')||tapped('w')) {
      State.titleMenuIndex=(State.titleMenuIndex-1+TITLE_MENU_COUNT)%TITLE_MENU_COUNT; AudioFX.ui();
    }
    if (tapped('arrowdown')||tapped('s')) {
      State.titleMenuIndex=(State.titleMenuIndex+1)%TITLE_MENU_COUNT; AudioFX.ui();
    }
    if (tapped('escape')) { State.titleMenuIndex=2; activateTitleMenu(); }
    else if (tapped('enter') || shootTap()) activateTitleMenu();
  } else if (State.mode==='BRIEF') {
    State.briefTimer += dt;
    if (tapped('enter') || shootTap()) {
      State.briefIndex++;
      const lines = VO.brief[State.mission];
      if (State.briefIndex >= lines.length) startMission(State.mission);
      else { const l=lines[State.briefIndex]; caption(l.line,l.speaker,99); AudioFX.ui(); }
    }
  } else if (State.mode==='PLAY') {
    if (tapped('enter')||tapped('escape')) { State.mode='PAUSE'; Music.pause(); AudioFX.ui(); }
    else updatePlay(dt);
  } else if (State.mode==='PAUSE') {
    if (tapped('enter')||tapped('escape')) { State.mode='PLAY'; Music.resume(); AudioFX.ui(); }
  } else if (State.mode==='CLEAR') {
    State.clearTimer += dt;
    if (State.clearTimer > 2.2 && (tapped('enter') || shootTap() || State.clearTimer>5)) {
      if (State.mission >= 2) {
        State.mode='ENDING'; State.endingTimer=0; caption(choice(VO.player.win),'PLAYER',3);
      } else startBrief(State.mission+1);
    }
  } else if (State.mode==='GAMEOVER') {
    State.gameOverTimer += dt;
    updateCaption(dt);
    if (State.gameOverTimer > 0.12) {
      if (!State.caption) caption(VO.cont.line,'ANNOUNCER',99);
      const confirm = tapped('enter') || shootTap() || jumpTap() || tapped(' ');
      if (confirm && State.continues > 0) {
        State.continues--;
        State.lives = 3;
        State.score = Math.floor(State.score * 0.9);
        startMission(State.mission);
        AudioFX.ui();
      } else if ((confirm && State.continues <= 0 && State.gameOverTimer > 0.5) || tapped('escape')) {
        hardResetCampaign();
        State.mode = 'TITLE';
        caption(VO.title.line, VO.title.speaker, 3);
        AudioFX.ui();
      }
    }
  } else if (State.mode==='ENDING') {
    State.endingTimer += dt;
    if (State.endingTimer>0.8 && (tapped('enter')||shootTap())) State.mode='TITLE';
  }
}

// ============================================================
// RENDER
// ============================================================
function drawImageFacing(img, x, y, facing=1, w, h) {
  if (facing>=0) ctx.drawImage(img, x, y, w||img.width, h||img.height);
  else {
    ctx.save();
    ctx.translate(x+(w||img.width), y);
    ctx.scale(-1,1);
    ctx.drawImage(img, 0, 0, w||img.width, h||img.height);
    ctx.restore();
  }
}

function spriteSize(img, fallbackW, fallbackH) {
  const logicalScale = img && img.__logicalScale || 1;
  return {
    w: (img.naturalWidth || img.width) ? (img.naturalWidth || img.width)/logicalScale : fallbackW,
    h: (img.naturalHeight || img.height) ? (img.naturalHeight || img.height)/logicalScale : fallbackH
  };
}

function setLogicalCanvasTransform() {
  const sx = canvas.width / W;
  const sy = canvas.height / H;
  ctx.setTransform(sx, 0, 0, sy, 0, 0);
  ctx.imageSmoothingEnabled = false;
}

function spriteRectOnFeet(img, ent, fallbackW=32, fallbackH=32, offsetY=0, scale=1) {
  const native = spriteSize(img, fallbackW, fallbackH);
  const size = {w:Math.round(native.w*scale), h:Math.round(native.h*scale)};
  const foot = Math.min(GROUND_BASE, ent.y + ent.h);
  const x = Math.round(ent.x + ent.w/2 - size.w/2);
  const y = Math.round(foot - size.h + offsetY);
  return {x, y, w:size.w, h:size.h, foot, native};
}

function drawSpriteOnFeet(img, ent, facing=1, fallbackW=32, fallbackH=32, offsetY=0, scale=1) {
  const rect=spriteRectOnFeet(img,ent,fallbackW,fallbackH,offsetY,scale);
  const {x,y,w,h}=rect;
  drawImageFacing(img, x, y, facing, w, h);
  return rect;
}

function heroPoseScale(anim) {
  const globalScale=0.85;
  // Normalize the visible alpha silhouette, not the transparent canvas. This
  // removes the last size jumps on run, grenade throw, damage and crouched
  // fire while retaining each authored pose and the global 15% reduction.
  const visibleHeights={
    idle:38,run1:30,run2:33,run3:30,run4:33,jump:38,fall:38,
    crouch:25,shoot:38,shootCrouch:21,throw:27,hurt:33,death:17
  };
  const visible=visibleHeights[anim]||38;
  const target=(anim==='crouch'||anim==='shootCrouch')?25:(anim==='death'?17:38);
  return globalScale*target/visible;
}

function drawTiledSprite(img, x, y, w, h) {
  const size = spriteSize(img, 16, 16);
  for (let dy=0; dy<h; dy+=size.h) {
    for (let dx=0; dx<w; dx+=size.w) {
      const dw = Math.min(size.w, w-dx);
      const dh = Math.min(size.h, h-dy);
      ctx.drawImage(img, 0, 0, dw, dh, x+dx, y+dy, dw, dh);
    }
  }
}

function shadowSurfaceY(ent) {
  if (!State.level) return GROUND_BASE;
  const foot = ent.y + ent.h;
  let surface = GROUND_BASE;
  for (const solid of State.level.solids) {
    if (!solid.alive || solid.y < foot-2) continue;
    if (ent.x+ent.w <= solid.x || ent.x >= solid.x+solid.w) continue;
    surface = Math.min(surface, solid.y);
  }
  return surface;
}

function decorScaleFor(kind) {
  return kind==='lamp' ? 1.6 : kind==='sign' ? 1.7 : 1;
}

function bossArenaSurface(level) {
  if (!level || level.id!==1) return null;
  return {
    waterX:level.length-430,
    waterY:GROUND_BASE-7,
    waterW:470,
    dockX:level.length-430,
    dockW:270
  };
}

function drawBossArenaSurface(level) {
  const arena=bossArenaSurface(level);
  if (!arena) return;
  ctx.fillStyle='#173f43';
  ctx.fillRect(arena.waterX,arena.waterY,arena.waterW,H-arena.waterY);
  const pile=Art.platformSupport(1);
  const pileW=18;
  for(let x=arena.dockX+12;x<arena.dockX+arena.dockW-8;x+=42) {
    ctx.drawImage(pile,x,GROUND_BASE+8,pileW,H-(GROUND_BASE+8));
  }
  const deck=Art.dockSpan();
  const nw=deck.naturalWidth||deck.width||192;
  const nh=deck.naturalHeight||deck.height||24;
  ctx.drawImage(deck,0,0,nw,nh,arena.dockX,GROUND_BASE-8,arena.dockW,24);
}

function drawLabelBox(x,y,w,h,col,label) {
  ctx.fillStyle = col; ctx.fillRect(x,y,w,h);
  ctx.strokeStyle = '#000'; ctx.strokeRect(x+0.5,y+0.5,w-1,h-1);
  if (State.debug && label) {
    ctx.fillStyle = '#fff'; ctx.font='8px monospace'; ctx.fillText(label, x+2, y+8);
  }
}

function drawParticle(pt) {
  const life=Math.max(0.001,pt.life||0.5);
  const ratio=clamp(pt.t/life,0,1);
  ctx.globalAlpha=clamp(ratio*2,0,1);
  if (pt.sprite==='boom') {
    const bimg=Art.vfx(ratio>0.66?'boom1':ratio>0.33?'boom2':'boom3');
    ctx.drawImage(bimg,pt.x,pt.y,pt.s,pt.s);
  } else if (pt.sprite==='smoke') {
    const smoke=Art.smoke();
    ctx.drawImage(smoke,pt.x,pt.y,pt.s,pt.s);
  } else if (pt.sprite==='muzzle') {
    const muzzle=Art.vfx('muzzle');
    const size=spriteSize(muzzle,18,14);
    const facing=pt.vx<0?-1:1;
    const mx=facing<0?pt.x-size.w:pt.x;
    drawImageFacing(muzzle,Math.round(mx),Math.round(pt.y-size.h/2),facing,size.w,size.h);
  } else if (pt.sprite==='corePulse') {
    const size=Math.max(4,Math.round(pt.s*ratio));
    const x=Math.round(pt.x-size/2),y=Math.round(pt.y-size/2);
    ctx.fillStyle='rgba(103,232,249,0.38)'; ctx.fillRect(x,y,size,size);
    ctx.strokeStyle='#a5f3fc'; ctx.lineWidth=1; ctx.strokeRect(x+0.5,y+0.5,size-1,size-1);
    const inner=Math.max(2,Math.round(size*0.32));
    ctx.fillStyle='#f8fafc'; ctx.fillRect(Math.round(pt.x-inner/2),Math.round(pt.y-inner/2),inner,inner);
  } else if (pt.ambient==='leaf') {
    ctx.fillStyle=pt.col;
    ctx.fillRect(Math.round(pt.x),Math.round(pt.y),Math.max(2,Math.round(pt.s)),1);
    ctx.fillRect(Math.round(pt.x+1),Math.round(pt.y+1),Math.max(1,Math.round(pt.s)-1),1);
  } else {
    ctx.fillStyle=pt.col;
    ctx.fillRect(Math.round(pt.x),Math.round(pt.y),Math.max(1,Math.round(pt.s)),Math.max(1,Math.round(pt.s)));
  }
  ctx.globalAlpha=1;
}

function drawWorld() {
  if (!State.level || !State.player) return;
  // visible proof drawWorld executed (left poison strip, world coords later)
  // (drawn in screen space before camera translate)
  /* marker after bg */
  const theme = State.level.theme;
  const cam = State.camX;
  // Keep feedback readable without moving the whole scene by double-digit
  // logical pixels on high-density displays.
  const shake = (State.mode==='PAUSE'||State.mode==='OPTIONS') ? 0 : Math.min(4,State.shake*(State.shakeScale??0.5));
  const shakeX = shake>0 ? rand(-shake, shake) : 0;
  const shakeY = shake>0 ? rand(-shake, shake)*0.6 : 0;
  ctx.save();
  try {
  ctx.translate(shakeX, shakeY);

  // parallax background: repeat a native tile, but stretch only vertically to the arena.
  function tileBlit(img, scroll, y, drawH) {
    if (!img) return;
    const sw = img.naturalWidth || img.width || W;
    const sh = img.naturalHeight || img.height || H;
    const iw = img.__logicalWidth || sw;
    const ih = img.__logicalHeight || sh;
    const dh = drawH == null ? H : drawH;
    // Every authored background has mismatched left/right edge pixels. Pairing
    // each tile with its mirror makes both joins identical. Integer placement
    // plus a one-pixel overlap prevents fractional raster gaps at any scale.
    const seamOverlap=1;
    const period = Math.max(1, iw-seamOverlap);
    const snappedScroll=Math.round(scroll);
    const phase=((snappedScroll % period) + period) % period;
    let x = -phase;
    let tileIndex=Math.floor(snappedScroll/period);
    while (x < W + period) {
      if (tileIndex&1) {
        ctx.save();
        ctx.translate(x+iw,0);
        ctx.scale(-1,1);
        ctx.drawImage(img,0,0,sw,sh,0,y||0,iw,dh);
        ctx.restore();
      } else ctx.drawImage(img, 0, 0, sw, sh, x, y || 0, iw, dh);
      x += period;
      tileIndex++;
    }
  }
  const fullBackground=Art.bgFull(theme);
  if (fullBackground) {
    tileBlit(fullBackground, cam*0.26, 0, H);
  } else {
    const far = Art.bgLayer(theme,1);
    const near = Art.bgLayer(theme,0);
    tileBlit(far, cam * 0.18, 0, null);
    tileBlit(near, cam * 0.42, 0, null);
  }

  // infinite ground strip fills all the way to bottom of screen
  const gh = H - GROUND_BASE;
  // solid underlay prevents any transparency/magenta flash
  ctx.fillStyle = theme===0 ? '#2f333b' : theme===1 ? '#3a2e1b' : '#243040';
  ctx.fillRect(0, GROUND_BASE, W, gh);
  const gimg = Art.groundStrip(theme);
  // top metal/asphalt lip
  ctx.fillStyle = theme===0 ? 'rgba(170,180,196,0.35)' : theme===1 ? 'rgba(150,130,70,0.28)' : 'rgba(170,190,210,0.35)';
  ctx.fillRect(0, GROUND_BASE-1, W, 2);
  if (gimg) {
    const iw = gimg.naturalWidth || gimg.width || 256;
    const ih = gimg.naturalHeight || gimg.height || gh;
    let x = -((cam % iw) + iw) % iw;
    while (x < W) {
      ctx.drawImage(gimg, 0, 0, iw, ih, x, GROUND_BASE, iw, gh);
      x += iw;
    }
  }

  if (State.debug) {
    ctx.fillStyle = '#19f58a';
    ctx.fillRect(0, 0, 4, H);
    ctx.fillStyle = 'rgba(0,0,0,0.5)';
    ctx.fillRect(8, 16, 70, 12);
    ctx.fillStyle = '#9affc2';
    ctx.font = '9px monospace';
    ctx.fillText('PLAY', 12, 25);
  }

  ctx.translate(-cam, 0);

  // Mission 2 ends on a river dock so its gunboat is actually afloat.
  drawBossArenaSurface(State.level);

  // Atmospheric particles stay behind actors and gameplay effects.
  for (const pt of State.particles) if (pt.layer==='ambient') drawParticle(pt);

  // Animated gunship; bomb positions are derived from this exact pose.
  if (State.flags.heli>0) {
    const heli=heliPose();
    const sprite=Art.helicopter(Math.floor(State.runTime*18));
    const size=spriteSize(sprite,140,52);
    ctx.drawImage(sprite,Math.round(heli.x),Math.round(heli.y),size.w,size.h);
  }

  // decorative props
    if (State.level.decors) {
    for (const d of State.level.decors) {
      if (d.x < cam-50 || d.x > cam+W+50) continue;
      if (!d.alive && d.destroyT<=0) continue;
      const img = Art.decor(d.kind);
      const native = spriteSize(img, 24, 24);
      const nativeH = native.h;
      const nativeW = native.w;
      // Street furniture must read against a 32x40 hero: lamps are roughly
      // twice a person, warning signs stand clearly above shoulder height.
      const propScale = decorScaleFor(d.kind);
      const ih = Math.round(nativeH*propScale);
      const iw = Math.round(nativeW*propScale);
      const swayKinds = {grass:1,bush:1,palm:1,vine:1};
      const bob = swayKinds[d.kind] ? Math.sin((State.runTime||0)*2.1 + (d.bob||0))*1.0 : 0;
      const dx = d.x - (iw>>1);
      const dy = (d.y||GROUND_BASE) - ih + 1 + bob;
      if (!d.alive) {
        const remaining=clamp(d.destroyT/0.48,0,1);
        const progress=1-remaining;
        ctx.save();
        ctx.globalAlpha=remaining;
        ctx.translate(d.x,d.y||GROUND_BASE);
        ctx.rotate((d.fallDir||1)*progress*1.18);
        if ((d.flip||1)<0) ctx.scale(-1,1);
        ctx.drawImage(img,-iw/2,-ih,iw,ih);
        ctx.restore();
        ctx.globalAlpha=1;
        continue;
      }
      ctx.fillStyle='rgba(0,0,0,0.28)';
      ctx.beginPath(); ctx.ellipse(d.x, (d.y||GROUND_BASE)-1, Math.max(6, iw*0.28), 2.4, 0, 0, Math.PI*2); ctx.fill();
      if ((d.flip||1) < 0) drawImageFacing(img, dx, dy, -1, iw, ih);
      else ctx.drawImage(img, dx, dy, iw, ih);
    }
  }

  // solids (skip drawing generic ground tiles — replaced by infinite full-bottom strip)
  for (const s of State.level.solids) {
    if (!s.alive) continue;
    if (s.x > cam+W+20 || s.x+s.w < cam-20) continue;
    if (s.type==='ground') continue;
    else if (s.type==='wall') {
      drawTiledSprite(Art.tile('wall', theme), s.x, s.y, s.w, s.h);
    } else if (s.type==='platform') {
      const th = s.theme != null ? s.theme : theme;
      const visualY=s.visualY!=null?s.visualY:s.y;
      const span=Art.platformSpan(th);
      const support=Art.platformSupport(th);
      const naturalW=span.naturalWidth||span.width||128;
      const naturalH=span.naturalHeight||span.height||24;
      const visualH=24;
      const supportTop=visualY+visualH-6;
      const supportH=GROUND_BASE-supportTop;
      if (supportH>5) {
        const supportW=20;
        const anchors=s.w>=92?[0.25,0.75]:[0.5];
        for(const anchor of anchors) {
          const sx=Math.round(s.x+s.w*anchor-supportW/2);
          ctx.fillStyle='rgba(0,0,0,0.22)';
          ctx.fillRect(sx+3,GROUND_BASE-2,supportW-6,3);
          ctx.drawImage(support,sx,supportTop,supportW,supportH+2);
        }
      }
      if (th!==1) {
        ctx.fillStyle = 'rgba(0,0,0,0.28)';
        ctx.fillRect(s.x+2,visualY+visualH-3,Math.max(0,s.w-4),4);
      }
      ctx.drawImage(span,0,0,naturalW,naturalH,s.x,visualY,s.w,visualH);
      ctx.fillStyle = th===1 ? 'rgba(255,220,150,0.22)' : th===2 ? 'rgba(210,230,250,0.2)' : 'rgba(245,236,210,0.2)';
      ctx.fillRect(s.x, visualY, s.w, 1);
    }
else if (s.type==='crate') { const im=Art.tile('crate', theme); const size=spriteSize(im,26,24); ctx.drawImage(im, s.x+s.w/2-size.w/2, s.y+s.h-size.h+2, size.w, size.h); }
    else if (s.type==='barrel') { const im=Art.tile('barrel', theme); const size=spriteSize(im,18,26); ctx.drawImage(im, s.x+s.w/2-size.w/2, s.y+s.h-size.h, size.w, size.h); }
    else if (s.type==='weak') {
      drawTiledSprite(Art.tile('wall', theme), s.x, s.y, s.w, s.h);
      ctx.fillStyle='rgba(240,193,74,0.25)'; ctx.fillRect(s.x,s.y,s.w,s.h);
    }
  }

  // entities
  for (const en of State.entities) {
    if (en.type==='enemy') {
      let frame = 'idle';
      if (!en.alive) frame = 'die';
      else if (en.kind==='grunt' && Math.abs(en.vx||0)>0.05) frame = ((Math.floor((en.walkPhase||0))%2) ? 'walk' : 'idle');
      else if (en.shootFlash>0) frame = 'idle';
      const img = Art.enemy(en.kind, frame);
      const foot = Math.min(GROUND_BASE, en.y + en.h);
      ctx.fillStyle='rgba(0,0,0,0.30)';
      ctx.beginPath(); ctx.ellipse(en.x+en.w/2, foot-1, Math.max(8,en.w*0.35), 3, 0, 0, Math.PI*2); ctx.fill();
      const bob = (frame==='walk') ? ((Math.floor(en.walkPhase*2)%2)?1:0) : 0;
      if (!en.alive) ctx.globalAlpha=clamp((en.deadT||0)/0.2,0,1);
      // Enemy source art faces left; logical facing is movement/aim direction.
      // Invert only at draw time so bullets and AI keep their world direction.
      const hurtBlink=en.alive&&en.hurtT>0&&(Math.floor(en.hurtT*100)%2===0);
      if (!hurtBlink) drawSpriteOnFeet(img, en, -en.facing, en.kind==='truck'?72:32, en.kind==='truck'?40:(en.kind==='turret'?28:36), -bob, en.kind==='truck'?2:1);
      ctx.globalAlpha=1;
      if (en.alive && en.shootFlash>0) {
        const mz = Art.vfx('muzzle');
        const size = spriteSize(mz, 18, 14);
        const mx = en.facing>0 ? en.x+en.w-2 : en.x-size.w+2;
        drawImageFacing(mz, Math.round(mx), Math.round(en.y+9-size.h/2), en.facing, size.w, size.h);
      }
    } else if (en.type==='pow') {
      const img = Art.pow();
      const bob = en.freed ? 0 : -Math.abs(Math.sin(en.bob)*2);
      // shadow
      ctx.fillStyle='rgba(0,0,0,0.3)';
      const powFoot = Math.min(GROUND_BASE, en.y+en.h);
      ctx.beginPath(); ctx.ellipse(en.x+en.w/2, powFoot-1, 8, 2.5, 0, 0, Math.PI*2); ctx.fill();
      drawSpriteOnFeet(img, en, en.freed?1:-1, 24, 36, bob);
      if (en.freed) {
        ctx.fillStyle='#fde68a'; ctx.font='8px monospace'; ctx.fillText('RUN', en.x, en.y-2);
      }
        } else if (en.type==='pickup') {
      const y = en.y + Math.sin(en.bob)*2;
      const key = en.kind==='heavy' ? 'prop_pickup_heavy' : en.kind==='rocket' ? 'prop_pickup_rocket' : en.kind==='heal' || en.kind==='repair' ? 'prop_pickup_heal' : 'prop_pickup_grenade';
      const grenadePickup=en.kind==='grenade';
      const ic = Art.asset(key, grenadePickup?24:14, grenadePickup?20:14);
      ctx.fillStyle='rgba(0,0,0,0.25)';
      ctx.beginPath(); ctx.ellipse(en.x+7, y+16, 7, 2.5, 0, 0, Math.PI*2); ctx.fill();
      if (ic && ic.complete) {
        const size = spriteSize(ic, grenadePickup?24:14, grenadePickup?20:14);
        ctx.drawImage(ic, Math.round(en.x+en.w/2-size.w/2), Math.round(y+en.h/2-size.h/2), size.w, size.h);
      } else {
        const col = en.kind==='heavy'?'#60a5fa': en.kind==='rocket'?'#f87171': en.kind==='heal'||en.kind==='repair'?'#4ade80':'#fbbf24';
        ctx.fillStyle=col; ctx.fillRect(en.x,y,14,14);
        ctx.strokeStyle='#111'; ctx.strokeRect(en.x+0.5,y+0.5,13,13);
        ctx.fillStyle='#111'; ctx.font='8px monospace';
        ctx.fillText((en.kind[0]||'?').toUpperCase(), en.x+4, y+10);
      }
    }
  }

  // vehicle
  if (State.vehicle && (State.vehicle.alive || State.vehicle.explodeT>0)) {
    const v=State.vehicle; const img=Art.vehicle(v.alive?v.anim:'explode');
    if (!v.alive) ctx.globalAlpha=clamp(v.explodeT/0.45,0.25,1);
    // Vehicle source art faces left, like enemy and boss atlas art.
    const vehicleHurtBlink=v.alive&&v.damageFlash>0&&Math.floor(v.damageFlash*42)%2===0;
    if (!vehicleHurtBlink) drawSpriteOnFeet(img, v, -(v.facing||1), 80, 48);
    ctx.globalAlpha=1;
    if (v.alive && v.shootFlash>0) {
      const muzzle=Art.vfx('muzzle');
      const size=spriteSize(muzzle,18,14);
      const muzzlePos=vehicleMuzzlePosition(v);
      const mx=v.facing>0?muzzlePos.x:muzzlePos.x-size.w;
      drawImageFacing(muzzle,Math.round(mx),Math.round(muzzlePos.y-size.h/2),v.facing,size.w,size.h);
    }
    // hp bar
    if (v.alive) {
      const hpW = Math.min(52, v.w);
      const hpX = Math.round(v.x+(v.w-hpW)/2);
      ctx.fillStyle='#000'; ctx.fillRect(hpX, v.y-6, hpW, 3);
      ctx.fillStyle='#4ade80'; ctx.fillRect(hpX, v.y-6, hpW*clamp(v.hp/v.maxHp,0,1), 3);
    }
  }

  // boss
  if (State.boss) {
    const b=State.boss;
    const img=Art.boss(b.id, b.alive?b.anim:'defeat');
    const dying=!b.alive&&b.deathTimer!=null;
    const deathRatio=dying?clamp(b.deathTimer/(b.deathDuration||1.8),0,1):1;
    if (dying) ctx.globalAlpha=Math.max(0.22,deathRatio);
    else if (b.alive && b.hurtT>0 && Math.floor(b.hurtT*100)%2===0) ctx.globalAlpha=0.46;
    // Boss atlas art also faces left natively, like the infantry atlas.
    const deathProgress=dying?1-deathRatio:0;
    const deathSettle=dying?(b.id===0?deathProgress*3:b.id===1?deathProgress*9:deathProgress*2):0;
    drawSpriteOnFeet(img,b,-(b.facing||-1),b.id===2?112:132,b.id===2?88:(b.id===1?72:63),deathSettle);
    ctx.globalAlpha=1;
    if (b.alive && b.attackT>0 && b.id<2) {
      const muzzle=Art.vfx('muzzle');
      const size=spriteSize(muzzle,18,14);
      const muzzlePos=bossMuzzlePosition(b);
      ctx.save();
      ctx.translate(Math.round(muzzlePos.x),Math.round(muzzlePos.y));
      ctx.rotate(muzzlePos.angle);
      ctx.drawImage(muzzle,0,-size.h/2,size.w,size.h);
      ctx.restore();
    }
    if (b.alive) {
      ctx.fillStyle='#000'; ctx.fillRect(b.x, b.y-8, b.w, 4);
      ctx.fillStyle=b.phase===2?'#fb923c':'#f87171';
      ctx.fillRect(b.x, b.y-8, b.w*clamp(b.hp/b.maxHp,0,1), 4);
      ctx.fillStyle='#fff'; ctx.font='8px monospace'; ctx.fillText(`${b.name}  P${b.phase}`, b.x, b.y-10);
    }
  }

  // player
  const p = State.player;
  if (p) {
    if (!p.inVehicle) {
      if (p.invuln>0 && Math.floor(p.invuln*20)%2===0) {
        // flicker
      } else {
        // contact shadow
        ctx.fillStyle='rgba(0,0,0,0.35)';
        const shadowY=shadowSurfaceY(p);
        const shadowScale=clamp(1-(shadowY-(p.y+p.h))/100,0.35,1);
        ctx.beginPath(); ctx.ellipse(p.x+p.w/2, shadowY-1, 10*shadowScale, 3*shadowScale, 0, 0, Math.PI*2); ctx.fill();
        const img = Art.hero(p.anim, p.facing);
        drawSpriteOnFeet(img, p, p.facing, 32, 40, 0, heroPoseScale(p.anim));
      }
    }
  }

  // bullets
  for (const b of State.bullets) {
    if (b.kind==='rocket') {
      ctx.fillStyle='#f97316'; ctx.fillRect(b.x,b.y,7,4);
      ctx.fillStyle='#fde68a'; ctx.fillRect(b.x-2,b.y+1,3,2);
    } else if (b.kind==='grenade') {
      ctx.fillStyle='#17210f'; ctx.beginPath(); ctx.arc(b.x+2,b.y+2,3.5,0,Math.PI*2); ctx.fill();
      ctx.fillStyle='#5f7a32'; ctx.beginPath(); ctx.arc(b.x+2,b.y+2,2.5,0,Math.PI*2); ctx.fill();
      ctx.fillStyle='#9bb85a'; ctx.fillRect(b.x+1,b.y,2,1);
      ctx.fillStyle='#9ca3af'; ctx.fillRect(b.x+3,b.y-2,2,2);
      ctx.fillStyle='#d6a93a'; ctx.fillRect(b.x+5,b.y-2,2,1);
    } else if (b.kind==='bomb') {
      ctx.strokeStyle=Math.floor(State.runTime*10)%2?'#ef4444':'#fca5a5';
      ctx.beginPath(); ctx.ellipse(b.x+3,GROUND_BASE-2,10,3,0,0,Math.PI*2); ctx.stroke();
      ctx.fillStyle='#1f2937'; ctx.fillRect(b.x+1,b.y,4,6);
      ctx.fillStyle='#64748b'; ctx.fillRect(b.x,b.y+2,6,4);
      ctx.fillStyle='#ef4444'; ctx.fillRect(b.x+2,b.y+6,2,2);
    } else {
      const bil = Art.bullet();
      if (b.vx < 0) drawImageFacing(bil, b.x, b.y, -1, 8, 4);
      else ctx.drawImage(bil, b.x, b.y, 8, 4);
      if (b.kind==='heavy') { ctx.fillStyle='rgba(147,197,253,0.7)'; ctx.fillRect(b.x-2,b.y,3,2); }
    }
  }

  // particles / floats
  for (const pt of State.particles) {
    if (pt.layer==='ambient') continue;
    drawParticle(pt);
  }
  for (const f of State.floats) {
    ctx.globalAlpha = clamp(f.t,0,1);
    ctx.fillStyle = f.col; ctx.font='10px monospace'; ctx.fillText(f.text, f.x, f.y);
    ctx.globalAlpha = 1;
  }

  if (State.debug) {
    ctx.strokeStyle='#0f0';
    if (p) ctx.strokeRect(p.x,p.y,p.w,p.h);
    State.entities.forEach(e => ctx.strokeRect(e.x,e.y,e.w,e.h));
    if (State.boss) ctx.strokeRect(State.boss.x,State.boss.y,State.boss.w,State.boss.h);
  }

  } finally {
    try { ctx.restore(); } catch (e) {}
    try { setLogicalCanvasTransform(); ctx.globalAlpha = 1; } catch (e) {}
  }
}

function drawHUD() {
  const p = State.player;
  // Two clean rows: score/lives above, health/ammo below. The previous
  // single row placed seven hearts directly under the ammo counter.
  ctx.textAlign='left';
  ctx.textBaseline='alphabetic';
  ctx.fillStyle = 'rgba(7,11,17,0.82)';
  ctx.fillRect(0, 0, W, 29);
  ctx.fillStyle='rgba(240,193,74,0.14)';
  ctx.fillRect(0,14,W,1);
  ctx.strokeStyle='rgba(125,211,252,0.28)';
  ctx.beginPath(); ctx.moveTo(0,28.5); ctx.lineTo(W,28.5); ctx.stroke();

  ctx.fillStyle = '#f8fafc';
  ctx.font = '10px monospace';
  ctx.fillText('1P', 4, 11);
  ctx.fillStyle = '#f0c14a';
  ctx.fillText(String(State.score).padStart(8,'0'), 22, 11);
  ctx.fillStyle = '#f8fafc';
  ctx.fillText('HI', 82, 11);
  ctx.fillStyle = '#cbd5e1';
  ctx.fillText(String(Math.max(State.score, arcadeBest)).padStart(8,'0'), 100, 11);

  ctx.fillStyle = '#f8fafc';
  ctx.fillText('REST', 166, 11);
  ctx.fillStyle='#f0c14a';
  ctx.fillText(String(Math.max(0,State.lives)),196,11);
  ctx.fillStyle='#94a3b8';
  ctx.fillText(State.level ? State.level.code : ('M'+(State.mission+1)),218,11);
  if (State.combo>1) {
    ctx.textAlign='right';
    ctx.fillStyle='#7dd3fc';
    ctx.font='bold 8px monospace';
    ctx.fillText(`${State.combo} HIT  x${comboMultiplier()}`,W-4,11);
    ctx.textAlign='left';
    ctx.font='10px monospace';
  }

  if (p) {
    const showHp = p.alive ? p.hp : 0;
    ctx.fillStyle='#f8fafc';
    ctx.fillText('HP',4,26);
    for (let i=0;i<p.maxHp;i++) {
      const himg = Art.heart(i < showHp);
      ctx.drawImage(himg,18+i*15,15,14,12);
    }
    // The supplied off-heart duplicated the full red heart.  A continuous
    // meter makes every accepted hit visible even during rapid action.
    ctx.fillStyle='#1e293b'; ctx.fillRect(18,27,104,2);
    ctx.fillStyle=showHp<=2?'#ef4444':'#4ade80';
    ctx.fillRect(18,27,104*clamp(showHp/p.maxHp,0,1),2);

    const code = p.weapon==='pistol' ? 'PST' : p.weapon==='heavy' ? 'HMG' : 'RKT';
    const ammo = p.weapon==='pistol' ? 'INF' : (p.weapon==='heavy' ? p.ammo.heavy : p.ammo.rocket);
    ctx.fillStyle = '#7dd3fc';
    ctx.fillText('W:'+code,136,26);
    ctx.fillStyle='#cbd5e1';
    ctx.fillText('A:'+ammo,184,26);
    ctx.fillStyle='#fde68a';
    ctx.fillText('G:'+p.grenades,236,26);
    if (State.level) {
      ctx.fillStyle='#94a3b8';
      ctx.font='7px monospace';
      ctx.fillText(objectiveHudText().slice(0,24),276,25);
    }
  }
  if (State.muted) { ctx.fillStyle='#fbbf24'; ctx.font='8px monospace'; ctx.fillText('M', W-8, 10); }

  if (State.bannerTimer>0 && State.hudBanner) {
    // MS big mission banner
    const t = Math.min(1, State.bannerTimer);
    ctx.fillStyle = 'rgba(0,0,0,0.55)';
    ctx.fillRect(40, 36, W-80, 28);
    ctx.strokeStyle = '#f0c14a';
    ctx.strokeRect(40.5,36.5,W-81,27);
    drawArcadeStamp(String(State.hudBanner).slice(0,22), W/2, 51, '#f0c14a', 0.62);
  }

  // captions - compact radio bar, not giant murk
  if (State.caption) {
    const c = State.caption;
    const body = wrapText(c.line, 42);
    const hh = 16 + body.length*10;
    ctx.fillStyle = 'rgba(0,0,0,0.72)';
    ctx.fillRect(8, H-hh-8, W-16, hh);
    ctx.fillStyle = '#86efac';
    ctx.font = '8px monospace';
    ctx.fillText(String(c.speaker||'RADIO'), 14, H-hh+4);
    ctx.fillStyle = '#f8fafc';
    ctx.font = '10px monospace';
    body.forEach((ln,i)=> ctx.fillText(ln, 14, H-hh+16+i*10));
  }
}

function wrapText(text, maxChars) {
  const words = String(text || '').split(/\s+/);
  const lines = [];
  let cur = '';
  for (const w of words) {
    const trial = cur ? cur + ' ' + w : w;
    if (trial.length > maxChars && cur) { lines.push(cur); cur = w; }
    else cur = trial;
  }
  if (cur) lines.push(cur);
  return lines.length ? lines : [''];
}

function drawPixelPanel(x, y, w, h, opts={}) {
  const border = opts.border || '#f0c14a';
  const fill = opts.fill || 'rgba(8,12,20,0.92)';
  const thick = opts.thick || 2;
  // Heavy arcade cabinet shadow and stepped steel frame.
  ctx.fillStyle = 'rgba(0,0,0,0.72)';
  ctx.fillRect(x+4,y+5,w,h);
  ctx.fillStyle = '#11161d';
  ctx.fillRect(x-2,y+3,2,h-4);
  ctx.fillStyle = fill;
  ctx.fillRect(x, y, w, h);
  // Subtle stamped-metal grid keeps large menu surfaces from feeling empty.
  ctx.fillStyle = 'rgba(148,163,184,0.035)';
  for (let gy=y+20; gy<y+h-7; gy+=8) ctx.fillRect(x+6,gy,w-12,1);
  for (let gx=x+14; gx<x+w-7; gx+=24) ctx.fillRect(gx,y+20,1,h-27);
  // Outer brass and inner gunmetal bevels.
  ctx.strokeStyle = '#30260f';
  ctx.lineWidth = thick+2;
  ctx.strokeRect(x-0.5,y-0.5,w+1,h+1);
  ctx.strokeStyle = border;
  ctx.lineWidth = thick;
  ctx.strokeRect(x + 0.5, y + 0.5, w - 1, h - 1);
  ctx.strokeStyle = '#6f5620';
  ctx.lineWidth = 1;
  ctx.strokeRect(x + 3.5, y + 3.5, w - 7, h - 7);
  ctx.strokeStyle = 'rgba(226,232,240,0.16)';
  ctx.strokeRect(x + 5.5, y + 5.5, w - 11, h - 11);
  // Riveted header rail.
  ctx.fillStyle = 'rgba(240,193,74,0.13)';
  ctx.fillRect(x+5, y+5, w-10, 14);
  ctx.fillStyle = 'rgba(255,255,255,0.05)';
  ctx.fillRect(x+6,y+6,w-12,1);
  ctx.fillStyle = '#3a3323';
  for(let rx=x+12;rx<x+w-12;rx+=16) ctx.fillRect(rx,y+17,8,1);
  // Corner armor blocks and bolts.
  ctx.fillStyle = border;
  const b = 3;
  [[x+5,y+5],[x+w-5-b,y+5],[x+5,y+h-5-b],[x+w-5-b,y+h-5-b]].forEach(([bx,by]) => ctx.fillRect(bx,by,b,b));
  ctx.fillStyle='#1b2028';
  [[x+6,y+6],[x+w-7,y+6],[x+6,y+h-7],[x+w-7,y+h-7]].forEach(([bx,by])=>ctx.fillRect(bx,by,1,1));
  if (opts.hazard) drawHazardStrip(x+7,y+h-9,w-14,4,border);
}

function drawHazardStrip(x,y,w,h,color='#f0c14a') {
  ctx.fillStyle='#171a1f'; ctx.fillRect(x,y,w,h);
  ctx.save(); ctx.beginPath(); ctx.rect(x,y,w,h); ctx.clip();
  ctx.fillStyle=color;
  for(let sx=x-6;sx<x+w+6;sx+=12) {
    ctx.beginPath(); ctx.moveTo(sx,y+h); ctx.lineTo(sx+5,y); ctx.lineTo(sx+10,y); ctx.lineTo(sx+5,y+h); ctx.fill();
  }
  ctx.restore();
}

const PIXEL_FONT = {
  'A':['01110','10001','10001','11111','10001','10001','10001'],
  'B':['11110','10001','10001','11110','10001','10001','11110'],
  'C':['01111','10000','10000','10000','10000','10000','01111'],
  'D':['11110','10001','10001','10001','10001','10001','11110'],
  'E':['11111','10000','10000','11110','10000','10000','11111'],
  'F':['11111','10000','10000','11110','10000','10000','10000'],
  'G':['01111','10000','10000','10111','10001','10001','01111'],
  'H':['10001','10001','10001','11111','10001','10001','10001'],
  'I':['11111','00100','00100','00100','00100','00100','11111'],
  'J':['00111','00010','00010','00010','10010','10010','01100'],
  'K':['10001','10010','10100','11000','10100','10010','10001'],
  'L':['10000','10000','10000','10000','10000','10000','11111'],
  'M':['10001','11011','10101','10101','10001','10001','10001'],
  'N':['10001','11001','10101','10011','10001','10001','10001'],
  'O':['01110','10001','10001','10001','10001','10001','01110'],
  'P':['11110','10001','10001','11110','10000','10000','10000'],
  'Q':['01110','10001','10001','10001','10101','10010','01101'],
  'R':['11110','10001','10001','11110','10100','10010','10001'],
  'S':['01111','10000','10000','01110','00001','00001','11110'],
  'T':['11111','00100','00100','00100','00100','00100','00100'],
  'U':['10001','10001','10001','10001','10001','10001','01110'],
  'V':['10001','10001','10001','10001','10001','01010','00100'],
  'W':['10001','10001','10001','10101','10101','10101','01010'],
  'X':['10001','10001','01010','00100','01010','10001','10001'],
  'Y':['10001','10001','01010','00100','00100','00100','00100'],
  'Z':['11111','00001','00010','00100','01000','10000','11111'],
  '0':['01110','10001','10011','10101','11001','10001','01110'],
  '1':['00100','01100','00100','00100','00100','00100','01110'],
  '2':['01110','10001','00001','00010','00100','01000','11111'],
  '3':['11110','00001','00001','01110','00001','00001','11110'],
  '4':['00010','00110','01010','10010','11111','00010','00010'],
  '5':['11111','10000','10000','11110','00001','00001','11110'],
  '6':['01110','10000','10000','11110','10001','10001','01110'],
  '7':['11111','00001','00010','00100','01000','01000','01000'],
  '8':['01110','10001','10001','01110','10001','10001','01110'],
  '9':['01110','10001','10001','01111','00001','00001','01110'],
  '-':['00000','00000','00000','11111','00000','00000','00000'],
  ':':['00000','00100','00100','00000','00100','00100','00000'],
  '.':['00000','00000','00000','00000','00000','00110','00110'],
  '/':['00001','00010','00010','00100','01000','01000','10000'],
  '!':['00100','00100','00100','00100','00100','00000','00100'],
  '?':['01110','10001','00001','00010','00100','00000','00100'],
  '+':['00000','00100','00100','11111','00100','00100','00000'],
  '"':['01010','01010','01010','00000','00000','00000','00000'],
  "'":['00100','00100','00000','00000','00000','00000','00000'],
  '(':['00010','00100','01000','01000','01000','00100','00010'],
  ')':['01000','00100','00010','00010','00010','00100','01000'],
  ' ':['00000','00000','00000','00000','00000','00000','00000']
};

function pixelTextWidth(text,scale=1,tracking=1) {
  const count=String(text).length;
  return count?((count*5+Math.max(0,count-1)*tracking)*scale):0;
}

function drawPixelText(text,x,y,options={}) {
  const value=String(text).toUpperCase();
  const scale=Math.max(1,Math.round(options.scale||1));
  const tracking=options.tracking==null?1:options.tracking;
  const width=pixelTextWidth(value,scale,tracking);
  const align=options.align||'left';
  const start=Math.round(align==='center'?x-width/2:align==='right'?x-width:x);
  const top=Math.round(y);
  const paint=(ox,oy,color) => {
    ctx.fillStyle=color;
    let cursor=start;
    for (const char of value) {
      const glyph=PIXEL_FONT[char]||PIXEL_FONT['?'];
      for (let row=0;row<7;row++) for (let col=0;col<5;col++) {
        if (glyph[row][col]==='1') ctx.fillRect(cursor+col*scale+ox,top+row*scale+oy,scale,scale);
      }
      cursor+=(5+tracking)*scale;
    }
  };
  if (options.shadow!==false) paint(scale,scale,options.shadowColor||'rgba(0,0,0,0.78)');
  paint(0,0,options.color||'#f8fafc');
  return {x:start,y:top,width,height:7*scale};
}

function drawMenuHeader(label,x,y,w,accent='#f0c14a',code='RC') {
  ctx.fillStyle='#252a31'; ctx.fillRect(x,y,w,15);
  ctx.fillStyle='#0b0f15'; ctx.fillRect(x+2,y+2,w-4,11);
  ctx.fillStyle=accent; ctx.fillRect(x,y,4,15); ctx.fillRect(x+w-4,y,4,15);
  ctx.fillStyle='rgba(240,193,74,0.18)'; ctx.fillRect(x+5,y+2,w-10,2);
  drawPixelText(label,x+10,y+4,{color:'#f8fafc',shadow:true});
  drawPixelText(code,x+w-9,y+4,{color:accent,align:'right',shadow:true});
}

function drawMenuButton(x,y,w,key,label,accent='#7dd3fc',selected=false) {
  ctx.fillStyle=selected?'rgba(240,193,74,0.18)':'rgba(15,23,34,0.88)';
  ctx.fillRect(x,y,w,17);
  ctx.strokeStyle=selected?'#f0c14a':'#34465a'; ctx.lineWidth=1; ctx.strokeRect(x+0.5,y+0.5,w-1,16);
  ctx.fillStyle=selected?'#f0c14a':accent; ctx.fillRect(x+3,y+3,3,11);
  ctx.textAlign='left'; ctx.textBaseline='alphabetic';
  ctx.font='bold 8px monospace'; ctx.fillStyle=selected?'#fff7d6':accent; ctx.fillText(key,x+11,y+11);
  ctx.textAlign='right'; ctx.fillStyle='#f8fafc'; ctx.fillText(label,x+w-9,y+11); ctx.textAlign='left';
}

function drawMetricCard(x,y,w,label,value,accent='#f0c14a') {
  ctx.fillStyle='rgba(15,23,34,0.88)'; ctx.fillRect(x,y,w,25);
  ctx.strokeStyle='#34465a'; ctx.lineWidth=1; ctx.strokeRect(x+0.5,y+0.5,w-1,24);
  ctx.fillStyle=accent; ctx.fillRect(x,y,3,25);
  drawPixelText(label,x+9,y+4,{color:'#94a3b8',shadow:false,tracking:0});
  drawPixelText(String(value),x+9,y+14,{color:'#f8fafc',shadow:true,tracking:0});
}

function drawArcadeStamp(text, cx, cy, color='#f0c14a', scale=1) {
  const pixelScale=scale>=0.72?3:2;
  return drawPixelText(text,cx,Math.round(cy-7*pixelScale/2),{scale:pixelScale,color,align:'center',shadow:true});
}

function drawSoftBg(theme=0) {
  ctx.fillStyle = '#07090d';
  ctx.fillRect(0,0,W,H);
  const fullBackground=Art.bgFull(theme);
  if (fullBackground) {
    const sw=fullBackground.naturalWidth||fullBackground.width;
    const sh=fullBackground.naturalHeight||fullBackground.height;
    ctx.drawImage(fullBackground,0,0,sw,sh,0,0,672,H);
  } else {
    const far = Art.bgLayer(theme, 1);
    const near = Art.bgLayer(theme, 0);
    // fixed non-scrolling framing for menus
    for (const layer of [far, near]) {
      if (!layer) continue;
      const size = spriteSize(layer, W, H);
      const sw = Math.min(W, size.w), sh = Math.min(H, size.h);
      ctx.drawImage(layer, 0, 0, sw, sh, 0, 0, sw, sh);
    }
  }
  // ground underlay
  ctx.fillStyle = theme===0 ? '#2f333b' : theme===1 ? '#3a2e1b' : '#243040';
  ctx.fillRect(0, GROUND_BASE, W, H-GROUND_BASE);
  const g = Art.groundStrip(theme);
  if (g) {
    const iw = g.naturalWidth || g.width || 256;
    const ih = g.naturalHeight || g.height || (H-GROUND_BASE);
    for (let x=0; x<W; x+=iw) ctx.drawImage(g, 0, 0, iw, ih, x, GROUND_BASE, iw, H-GROUND_BASE);
  }
  ctx.fillStyle = 'rgba(0,0,0,0.55)';
  ctx.fillRect(0,0,W,H);
}

function drawCenterPanel(title, lines, foot) {
  // compat wrapper used by abort paths
  drawSoftBg(State.mission || 0);
  const panelW = 300, panelH = 150;
  const x = (W-panelW)/2, y = (H-panelH)/2;
  drawPixelPanel(x,y,panelW,panelH);
  ctx.fillStyle = '#f0c14a';
  ctx.font = 'bold 12px monospace';
  ctx.fillText(title, x+14, y+22);
  ctx.fillStyle = '#d6deea';
  ctx.font = '10px monospace';
  let yy = y+42;
  (lines||[]).forEach(ln => {
    wrapText(ln, 34).forEach(wln => { ctx.fillText(wln, x+14, yy); yy += 12; });
  });
  if (foot) {
    ctx.fillStyle = '#7dd3fc';
    ctx.font = '10px monospace';
    ctx.fillText(foot, x+14, y+panelH-14);
  }
}

function drawTitleChoice(index,y,label) {
  const selected=State.titleMenuIndex===index;
  const lettering=Art.menuLabel(label);
  const logical=spriteSize(lettering,128,20);
  const maxW=128,maxH=20;
  const scale=Math.min(maxW/logical.w,maxH/logical.h);
  const dw=Math.round(logical.w*scale),dh=Math.round(logical.h*scale);
  ctx.globalAlpha=selected?1:0.76;
  ctx.drawImage(lettering,Math.round((W-dw)/2),Math.round(y+(24-dh)/2),dw,dh);
  ctx.globalAlpha=1;
  if (selected) {
    const arrowX=Math.round((W-dw)/2)-15;
    const arrowY=y+12;
    ctx.fillStyle='rgba(0,0,0,0.75)';
    ctx.beginPath(); ctx.moveTo(arrowX+2,arrowY+2); ctx.lineTo(arrowX-7,arrowY-5); ctx.lineTo(arrowX-7,arrowY+9); ctx.fill();
    ctx.fillStyle='#f0c14a';
    ctx.beginPath(); ctx.moveTo(arrowX,arrowY); ctx.lineTo(arrowX-8,arrowY-7); ctx.lineTo(arrowX-8,arrowY+7); ctx.fill();
  }
}

function drawTitle() {
  if (State.mode !== 'TITLE') return;
  drawSoftBg(0);
  ctx.fillStyle='rgba(0,0,0,0.42)'; ctx.fillRect(0,0,W,24); ctx.fillRect(0,H-32,W,32);

  let logo = null;
  try { logo = Art.logo(); } catch (e) { logo = null; }
  if (logo && (logo.naturalWidth||logo.width)) {
    const size = spriteSize(logo, 238, 96);
    const scale=Math.min(226/size.w,90/size.h);
    const dw=Math.round(size.w*scale),dh=Math.round(size.h*scale);
    ctx.drawImage(logo, Math.round((W-dw)/2), 0, dw, dh);
  }
  else {
    drawArcadeStamp('RUSTED CANNON',W/2,54,'#f0c14a',0.9);
  }

  // Clean title copy only: centered below the logo with a one-pixel shadow,
  // no plate, frame or background block.
  ctx.textAlign = 'center';
  ctx.shadowColor='rgba(0,0,0,0.9)'; ctx.shadowOffsetX=1; ctx.shadowOffsetY=1; ctx.shadowBlur=0;
  ctx.font = 'bold 8px monospace'; ctx.fillStyle='#f0c14a';
  ctx.fillText('MERCENARY ASSAULT',W/2,101);
  ctx.fillStyle='#c7d2e0'; ctx.font='7px monospace';
  ctx.fillText('ARCADE RUN & GUN  //  RUSTED CANNON UNIT',W/2,111);
  ctx.shadowColor='transparent'; ctx.shadowOffsetX=0; ctx.shadowOffsetY=0;

  drawTitleChoice(0,119,'PLAY');
  drawTitleChoice(1,147,'OPTIONS');
  drawTitleChoice(2,175,'ESCAPE');
  ctx.textAlign='center'; ctx.fillStyle='#94a3b8'; ctx.font='7px monospace';
  ctx.fillText('↑ ↓ / W S  SELECT     ENTER / Z  CONFIRM     M  SOUND',W/2,216);
  ctx.textAlign = 'left';
}

function drawOptions() {
  if (State.optionsReturn==='PAUSE'&&State.level&&State.player) {
    drawWorld(); drawHUD();
    ctx.fillStyle='rgba(0,0,0,0.62)'; ctx.fillRect(0,0,W,H);
  } else drawSoftBg(State.mission||0);

  drawPixelPanel(24,7,W-48,210,{hazard:true});
  drawMenuHeader('SYSTEM CONFIGURATION',32,15,W-64,'#f0c14a','CFG');
  const shakeName=State.shakeScale<=0?'OFF':State.shakeScale<0.5?'LOW':'NORMAL';
  const rows=[
    ['DIFFICULTY',difficultyConfig().name],
    ['MUSIC',Math.round(Music.getLevel()*100)+'%'],
    ['SFX',Math.round(AudioFX.getLevel()*100)+'%'],
    ['SCREEN SHAKE',shakeName],
    ['SCANLINES',State.scanlines?'ON':'OFF'],
    ['INTEGER SCALE',State.integerScale?'ON':'OFF'],
    ['BACK','ENTER']
  ];
  rows.forEach((row,index)=>{
    const y=48+index*16;
    const selected=index===State.optionsIndex;
    if (selected) {
      ctx.fillStyle='rgba(240,193,74,0.18)'; ctx.fillRect(36,y-11,W-72,15);
      ctx.fillStyle='#f0c14a'; ctx.font='bold 10px monospace'; ctx.fillText('>',40,y);
    }
    ctx.fillStyle=selected?'#f8fafc':'#cbd5e1';
    ctx.font=(selected?'bold ':'')+'10px monospace';
    ctx.fillText(row[0],54,y);
    ctx.fillStyle=selected?'#7dd3fc':'#94a3b8';
    ctx.fillText(row[1],196,y);
    if (index===1||index===2) {
      const level=index===1?Music.getLevel():AudioFX.getLevel();
      for(let segment=0;segment<10;segment++) {
        ctx.fillStyle=segment<Math.round(level*10)?'#f0c14a':'#334155';
        ctx.fillRect(238+segment*7,y-7,5,7);
      }
    }
  });
  drawMenuButton(38,163,W-76,'W/S + A/D','SELECT / CHANGE','#7dd3fc',false);
  ctx.fillStyle='#94a3b8'; ctx.font='7px monospace';
  ctx.fillText('MOVE WASD/ARROWS   FIRE Z/J   JUMP X/K   GRENADE C/L',42,190);
  ctx.fillText('ESC BACK   ENTER CONFIRM   M MUTE',42,202);
}

function drawBrief() {
  drawSoftBg(State.mission || 0);
  const m = GDD.missions[State.mission];
  const lines = VO.brief[State.mission] || [];
  const cur = lines[Math.min(State.briefIndex, lines.length-1)] || {speaker:'RADIO', line:'...'};

  // Mission plate and radio console share the same armored cabinet language.
  drawPixelPanel(14, 8, W-28, 52);
  drawMenuHeader('MISSION BRIEFING',22,16,W-44,'#f0c14a',m.code);
  ctx.fillStyle = '#f0c14a';
  ctx.font = 'bold 10px monospace';
  ctx.fillText(m.code + '  ' + m.name.toUpperCase(), 28, 43);
  ctx.textAlign='right'; ctx.fillStyle='#7dd3fc'; ctx.font='bold 8px monospace';
  ctx.fillText(difficultyConfig().name,W-28,43); ctx.textAlign='left';

  // radio window
  drawPixelPanel(14, 64, W-28, 116,{hazard:true});
  drawMenuHeader('ENCRYPTED RADIO LINK',22,72,W-44,'#86efac','LIVE');
  ctx.fillStyle = '#86efac';
  ctx.font = '8px monospace';
  ctx.fillText('CALLSIGN // ' + cur.speaker, 32, 97);

  const radio=Art.radio();
  const radioSize=spriteSize(radio,40,56);
  ctx.drawImage(radio,27,102,radioSize.w,radioSize.h);
  ctx.fillStyle='rgba(134,239,172,0.65)';
  ctx.fillRect(65,112,3,1); ctx.fillRect(67,109,3,1); ctx.fillRect(69,106,3,1);

  ctx.fillStyle = '#f8fafc';
  ctx.font = '11px monospace';
  let yy = 116;
  ctx.save();
  ctx.beginPath(); ctx.rect(74,104,W-102,50); ctx.clip();
  wrapText(cur.line, 28).slice(0,3).forEach(ln => { ctx.fillText(ln, 76, yy); yy += 14; });
  ctx.restore();

  // progress pips
  const total = lines.length;
  for (let i=0;i<total;i++) {
    ctx.fillStyle = i <= State.briefIndex ? '#f0c14a' : '#334155';
    ctx.fillRect(32 + i*12, 158, 8, 4);
  }

  const more = State.briefIndex < total-1;
  drawMenuButton(52,190,W-104,'ENTER / Z',more?'NEXT MESSAGE':'START MISSION','#7dd3fc',true);
  ctx.fillStyle='#94a3b8'; ctx.font='7px monospace'; ctx.textAlign='center';
  ctx.fillText('RUSTED CANNON UNIT // SECURE CHANNEL',W/2,216); ctx.textAlign='left';
}

function drawPause() {
  ctx.fillStyle = 'rgba(0,0,0,0.58)'; ctx.fillRect(0,0,W,H);
  ctx.fillStyle='#080d16'; ctx.fillRect(32,18,W-64,188);
  drawPixelPanel(32,18,W-64,188,{hazard:true});
  drawMenuHeader('FIELD PAUSE',40,26,W-80,'#f0c14a','RC');
  drawArcadeStamp('PAUSED',W/2,58,'#f8fafc',0.58);
  ctx.textAlign='left'; ctx.font='bold 8px monospace'; ctx.fillStyle='#94a3b8';
  ctx.fillText('MISSION',46,78); ctx.fillStyle='#f8fafc'; ctx.fillText(GDD.missions[State.mission].name.toUpperCase(),94,78);
  const p=State.player;
  const weapon=p ? (p.weapon==='pistol'?'PISTOL':p.weapon==='heavy'?'HEAVY MG':'ROCKET') : 'PISTOL';
  drawMetricCard(46,86,88,'SCORE',String(State.score).padStart(8,'0'),'#f0c14a');
  drawMetricCard(140,86,58,'POWs',State.powRescued,'#86efac');
  drawMetricCard(204,86,72,'LOADOUT',weapon,'#7dd3fc');
  drawMetricCard(282,86,56,'GRENADES',p?p.grenades:0,'#fde68a');
  drawMenuButton(52,120,W-104,'ESC / ENTER','RESUME','#7dd3fc',true);
  drawMenuButton(52,141,W-104,'O','OPTIONS','#f0c14a',false);
  drawMenuButton(52,162,W-104,'M','MUTE AUDIO','#94a3b8',false);
  ctx.textAlign='center'; ctx.fillStyle='#64748b'; ctx.font='7px monospace';
  ctx.fillText(difficultyConfig().name+' // '+GDD.squad.toUpperCase(),W/2,194); ctx.textAlign='left';
}

function drawClear() {
  drawWorld();
  ctx.fillStyle = 'rgba(0,0,0,0.5)'; ctx.fillRect(0,0,W,H);
  drawPixelPanel(22, 12, W-44, 202,{hazard:true});
  drawMenuHeader('AFTER ACTION REPORT',30,20,W-60,'#86efac','AAR');
  drawArcadeStamp('MISSION COMPLETE', W/2-18, 50, '#f0c14a', 0.58);
  const result=State.missionResult||calculateMissionResult();
  const rankBadge=Art.rankBadge(result.rank);
  const rankSize=spriteSize(rankBadge,52,52);
  ctx.drawImage(rankBadge,Math.round(W-75-rankSize.w/2),42,rankSize.w,rankSize.h);
  ctx.textAlign = 'left';
  ctx.font = '8px monospace';
  ctx.fillStyle = '#f8fafc';
  const rows = [
    GDD.missions[State.mission].name.toUpperCase(),
    (result.objectiveComplete?'OBJECTIVE COMPLETE  ':'OBJECTIVE FAILED    ')+result.objective,
    'SCORE  '+String(State.score).padStart(8,'0'),
    `POWs ${result.pows}   KILLS ${result.kills}   DEATHS ${result.deaths}`,
    `MAX COMBO ${result.maxCombo}   SCENERY ${result.scenery}`,
    `MISSION BONUS  +${result.bonus}`,
  ];
  rows.forEach((r,i)=> {
    const color=i===1?(result.objectiveComplete?'#86efac':'#fca5a5'):i===5?'#f0c14a':'#f8fafc';
    drawPixelText(String(r).slice(0,50),38,76+i*17,{color,shadow:true,tracking:0});
  });
  drawMenuButton(52,182,W-104,'ENTER',State.mission>=2?'VIEW ENDING':'NEXT MISSION','#7dd3fc',true);
}

function drawGameOver() {
  drawSoftBg(State.mission || 0);
  ctx.fillStyle='rgba(70,10,12,0.18)'; ctx.fillRect(0,0,W,H);
  drawPixelPanel(38,27,W-76,172,{border:'#d96a4b',hazard:true});
  drawMenuHeader('RUSTED CANNON CASUALTY REPORT',46,35,W-92,'#f87171','KIA');
  drawArcadeStamp('OPERATION FAILED', W/2, 66, '#f87171', 0.62);
  drawMetricCard(62,85,126,'FINAL SCORE',String(State.score).padStart(8,'0'),'#f0c14a');
  drawMetricCard(196,85,126,'CONTINUE TOKENS',State.continues,'#7dd3fc');
  ctx.textAlign='center'; ctx.font='7px monospace'; ctx.fillStyle='#94a3b8';
  ctx.fillText('THE GUN FALLS SILENT. THE WAR GOES ON.',W/2,124);
  if (State.continues > 0) {
    drawMenuButton(62,136,260,'ENTER / Z','CONTINUE MISSION','#7dd3fc',true);
    drawMenuButton(62,157,260,'ESC','RETURN TO TITLE','#94a3b8',false);
  } else {
    drawMenuButton(62,146,260,'ENTER / Z','RETURN TO TITLE','#7dd3fc',true);
  }
  ctx.textAlign='left';
}

function drawEnding() {
  drawSoftBg(2);
  drawPixelPanel(26, 16, W-52, 192,{border:'#86efac',hazard:true});
  drawMenuHeader('CAMPAIGN ARCHIVE',34,24,W-68,'#86efac','FINAL');
  drawArcadeStamp('IRON HIVE DESTROYED', W/2, 55, '#86efac', 0.58);
  ctx.textAlign = 'left';
  ctx.fillStyle = '#f8fafc';
  ctx.font = '9px monospace';
  const rows = [
    GDD.playerName + ' wins.',
    'FINAL SCORE  ' + State.score,
    'POWs FREED   ' + State.powRescued,
    'HOSTILES     ' + State.kills,
    '',
    'Radio: Rusted Cannon, mission complete.',
    'Announcer: Campaign Clear!',
  ];
  rows.forEach((r,i)=> {
    const color=i===0?'#f0c14a':i>=5?'#86efac':'#f8fafc';
    drawPixelText(r,48,74+i*14,{color,shadow:true,tracking:0});
  });
  drawMenuButton(58,177,W-116,'ENTER','RETURN TO TITLE','#7dd3fc',true);
}

function drawScanlines() {
  if (!State.scanlines) return;
  ctx.fillStyle='rgba(0,0,0,0.12)';
  for (let y=0;y<H;y+=2) ctx.fillRect(0,y,W,1);
}

function syncModeBadge(){
  try {
    const show = !!(typeof State !== 'undefined' && State && State.debug);
    const b=document.getElementById('modebadge');
    const d=document.getElementById('dbg');
    if (b) {
      b.style.display = show ? 'block' : 'none';
      b.textContent = 'MODE:'+String((typeof State!=='undefined' && State && State.mode) || '?');
    }
    if (d) {
      d.style.display = show ? 'block' : 'none';
      d.textContent = 'mode='+String((typeof State!=='undefined' && State && State.mode) || '?')+' score='+String((typeof State!=='undefined' && State && State.score)||0);
    }
  } catch (e) {}
}
function dbg(message){
  if (!State.debug) return;
  const d=document.getElementById('dbg');
  if (d) d.textContent=String(message);
}
function render() {
  try{syncModeBadge();}catch(e){}

  try { setLogicalCanvasTransform(); ctx.globalAlpha = 1; ctx.globalCompositeOperation='source-over'; } catch (e) {}
  ctx.fillStyle='#120a08'; ctx.fillRect(0,0,W,H); // never leave pure featureless black forever
  const mode = String(State.mode || '');
  try {
    if (mode === 'TITLE') drawTitle();
    else if (mode === 'OPTIONS') drawOptions();
    else if (mode === 'BRIEF') drawBrief();
    else if (mode === 'PLAY' || mode === 'PAUSE') {
      drawWorld();
      drawHUD();
      if (mode === 'PAUSE') drawPause();
    }
    else if (mode === 'CLEAR') drawClear();
    else if (mode === 'GAMEOVER') drawGameOver();
    else if (mode === 'ENDING') drawEnding();
    else {
      // fallback: never sit on a stale title frame
      if (State.level) { drawWorld(); drawHUD(); }
      else drawTitle();
    }
  } catch (err) {
    ctx.fillStyle = '#400'; ctx.fillRect(0,0,W,H);
    ctx.fillStyle = '#f88'; ctx.font = '10px monospace';
    ctx.fillText('RENDER ERR', 10, 20);
    String(err && (err.stack || err.message || err)).split('\n').slice(0,8).forEach((ln,i)=>{
      ctx.fillText(ln.slice(0,54), 10, 36+i*12);
    });
    try { dbg('RENDER ERR '+err); } catch (e) {}
  }
  // canvas mode stamp only in debug
  if (State.debug) {
    ctx.fillStyle = 'rgba(0,0,0,0.55)';
    ctx.fillRect(2, 2, 150, 14);
    ctx.fillStyle = '#0f0';
    ctx.font = '10px monospace';
    ctx.textAlign = 'left';
    ctx.fillText(String(mode || '?') + (State.player ? (' '+(State.player.anim||'')) : ''), 6, 12);
  }

  drawScanlines();
  if (State.debug) try{
    syncModeBadge();
    const badge=document.getElementById('modebadge');
    if(badge){
      badge.textContent = String(State.mode||'?') + ' ' + ((State.player&&State.player.anim)||'') + ' x' + (State.player?(State.player.x|0):0) + ' px=' + (State._px||'-');
      badge.style.background = (State.mode==='PLAY'||State.mode==='PAUSE') ? '#ff00aa' : (State.mode==='TITLE' ? '#2244ff' : '#666');
    }
    dbg(`mode ${State.mode} m${State.mission} cam ${State.camX|0} p ${State.player?(State.player.x|0)+","+(State.player.y|0)+" "+(State.player.anim||""):"np"}`);
  }catch(e){}
  try {
    const nextTitle = State.debug ? ('MODE:'+State.mode+'|'+(State.player&&State.player.anim||'')+'|'+Math.floor(State.camX||0)) : 'RUSTED CANNON — Arcade Run & Gun';
    if (document.title!==nextTitle) document.title=nextTitle;
  } catch (e) {}
  try {
    const nextBg = State.debug ? ((State.mode==='PLAY'||State.mode==='PAUSE') ? '#062416' : (State.mode==='TITLE' ? '#07090d' : '#101820')) : '#07090d';
    if (document.body.style.background!==nextBg) document.body.style.background=nextBg;
  } catch (e) {}
  // QA probe (only when debug) - samples canvas top pixel, optional full bar
  if (State.debug) try {
    setLogicalCanvasTransform();
    ctx.globalAlpha = 1;
    ctx.globalCompositeOperation = 'source-over';
    if (State.mode === 'PLAY' || State.mode === 'PAUSE') {
        ctx.fillStyle = '#ff00aa';
        ctx.fillRect(W-84, 2, 80, 14);
        ctx.fillStyle = '#fff';
        ctx.font = 'bold 10px monospace';
        ctx.textAlign = 'left';
        ctx.fillText('PLAY ' + ((State.player && State.player.anim) || ''), W-80, 12);
      } else if (State.mode === 'TITLE') {
        ctx.fillStyle = '#2244ff';
        ctx.fillRect(W-60, 2, 56, 14);
        ctx.fillStyle = '#fff';
        ctx.font = 'bold 10px monospace';
        ctx.fillText('TITLE', W-52, 12);
    }
    const pr = ctx.getImageData(2, 2, 1, 1).data;
    State._px = pr[0] + ',' + pr[1] + ',' + pr[2];
  } catch (e) {
    State._px = 'probe-fail';
  }


  if (State.debug) {
    ctx.fillStyle='rgba(0,0,0,0.5)'; ctx.fillRect(2,H-60,168,56);
    ctx.fillStyle='#0f0'; ctx.font='9px monospace';
    ctx.fillText(`mode ${State.mode}`, 6, H-48);
    ctx.fillText(`fps ${fps|0} ents ${State.entities.length}`, 6, H-36);
    ctx.fillText(`bul ${State.bullets.length} cam ${State.camX|0}`, 6, H-24);
    if (State.player) ctx.fillText(`p ${State.player.x|0},${State.player.y|0}`, 6, H-12);
  }
}

// ============================================================
// MAIN LOOP + RESIZE
// ============================================================
let acc = 0, last = now(), fps = 60, fpsT = 0, frames = 0;
let scaleCacheKey = '';

function applyScale() {
  const wrap = document.getElementById('wrap');
  if (!wrap) return;
  const dpr = Math.max(1, Number(window.devicePixelRatio)||1);
  const viewportW=Math.max(1,Math.round(window.visualViewport?.width||window.innerWidth));
  const viewportH=Math.max(1,Math.round(window.visualViewport?.height||window.innerHeight));
  const coarsePointer=typeof window.matchMedia==='function'&&window.matchMedia('(pointer: coarse)').matches;
  const scaleKey = viewportW+'x'+viewportH+':'+dpr+':'+(State.integerScale?'integer':'fit')+':'+(coarsePointer?'touch':'pointer');
  if (scaleKey===scaleCacheKey) return;
  scaleCacheKey = scaleKey;
  const help = document.getElementById('help');
  const helpVisible = help && getComputedStyle(help).display !== 'none';
  const margin = viewportW < 900 ? 4 : 24;
  const helpReserve = helpVisible ? Math.max(36, help.offsetHeight + 24) : 8;
  const maxW = Math.max(1, viewportW-margin);
  const maxH = Math.max(1, viewportH-helpReserve-margin);
  const fit = Math.max(0.01, Math.min(maxW/W, maxH/H));
  // Integer scaling is ideal on desktop, but on a landscape phone flooring a
  // 1.5x fit to 1x wastes a third of the screen. Touch devices always use the
  // exact fractional fit while the high-density backing canvas stays crisp.
  const scale = State.integerScale && !coarsePointer && fit>=1 ? Math.max(1, Math.floor(fit)) : fit;
  const cssW = Math.max(1, Math.floor(W*scale));
  const cssH = Math.max(1, Math.floor(H*scale));
  if (wrap.dataset.cssSize !== cssW+'x'+cssH) {
    wrap.style.width = cssW+'px';
    wrap.style.height = cssH+'px';
    wrap.dataset.cssSize = cssW+'x'+cssH;
  }
  // Match the backing store to the real display density (up to the native
  // 6x sprite budget) while every simulation coordinate remains 384x224.
  const renderScale = Math.max(1, Math.min(MAX_RENDER_SCALE, Math.round(scale*dpr)));
  const backingW = W*renderScale;
  const backingH = H*renderScale;
  if (canvas.width!==backingW || canvas.height!==backingH) {
    canvas.width=backingW;
    canvas.height=backingH;
    ctx.imageSmoothingEnabled=false;
  }
  State.renderScale=renderScale;
  canvas.style.imageRendering = 'pixelated';
}
window.addEventListener('resize', ()=>{ scaleCacheKey=''; applyScale(); }, {passive:true});
window.visualViewport?.addEventListener('resize',()=>{ scaleCacheKey=''; applyScale(); },{passive:true});

function mainLoop() {
  const t = now();
  let dt = Math.min(0.05, t - last);
  last = t;
  acc += dt;
  frames++;
  fpsT += dt;
  if (fpsT >= 0.5) { fps = frames / fpsT; frames = 0; fpsT = 0; }

  let steps = 0;
  while (acc >= FIXED_DT && steps < 5) {
    update(FIXED_DT);
    // Edge-triggered inputs live until a simulation step consumes them. This
    // avoids lost taps at 120/144 Hz and prevents double toggles on catch-up.
    clearJust();
    acc -= FIXED_DT;
    steps++;
  }
  applyScale();
  render();
  requestAnimationFrame(mainLoop);
}

// boot
initTouchControls();
try { AudioFX.ensure(); } catch (e) {}
caption(VO.title.line, VO.title.speaker, 3);
let bootedLoop=false;
function bootLoop(){
  if (bootedLoop) return; bootedLoop=true;
  window.__gameBooted=true;
  try{syncModeBadge();}catch(e){}
  try{const d=document.getElementById('dbg'); if(d) d.textContent='bootLoop';}catch(e){}
  try { Art.cache.clear(); } catch (e) {}
  try {
    const q=new URLSearchParams(location.search||'');
    if (q.get('start')==='1' || q.get('walk')==='1' || (location.hash||'').includes('start')) {
      State.debug = false;
      hardResetCampaign();
      startMission(0);
      keys['arrowright']=true; keys['d']=true; just['arrowright']=true;
      setTimeout(()=>{ keys['arrowright']=false; keys['d']=false; }, 8000);
    }
  } catch (e) {}
  render();
  requestAnimationFrame(mainLoop);
}
try { syncModeBadge(); } catch (e) {}
try { render(); } catch (e) {
  try {
    const d=document.getElementById('dbg');
    if (d) d.textContent='render fail: '+(e && e.message || e);
    const b=document.getElementById('modebadge');
    if (b) b.textContent='ERR';
    ctx.fillStyle='#401010'; ctx.fillRect(0,0,W,H);
    ctx.fillStyle='#f88'; ctx.font='12px monospace';
    ctx.fillText(String(e && e.message || e), 12, 40);
  } catch (e2) {}
}
Art.whenAssetsReady(bootLoop);
setTimeout(bootLoop, 2100);
try { render(); } catch (e) {}

