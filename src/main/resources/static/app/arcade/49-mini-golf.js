// Arcade 100 · MIT · see LICENSE
window.__arcadeNativeUpgrade = {"id": 49, "name": "Bankshot Open", "description": "Each course is mirrored top to bottom, and crisp rail rebounds reward bank shots."}; const arcadeVariant = new URLSearchParams(location.search).get("mode") === "plus" || new URLSearchParams(location.search).get("plus") === "1";

"use strict";
const cv=document.getElementById("cv"), ctx=cv.getContext("2d");
const infoEl=document.getElementById("info"), overlay=document.getElementById("overlay");
const W=960,H=640,M=20;
const BALL_R=8, CUP_R=11, WALL_T=12, REST=arcadeVariant ? .88 : .72;
const FRICTION=0.986, SAND_FR=0.90, MAX_DRAG=230, PWR_K=0.055, MAX_V=15;
const BOUNDARY=[[M,M,W-M,M],[W-M,M,W-M,H-M],[W-M,H-M,M,H-M],[M,H-M,M,M]];
const PAR_NAMES=s=>s<=-3?"Albatross":s===-2?"Eagle":s===-1?"Birdie":s===0?"Par":s===1?"Bogey":s===2?"Double bogey":"+"+s;

// w: wall segments [x1,y1,x2,y2] | s: sand rects [x,y,w,h] | c: circular obstacles [x,y,r]
const HOLES=[
 {par:2,tee:[140,320],cup:[820,320],w:[[480,170,480,460]],s:[],c:[]},
 {par:3,tee:[140,520],cup:[820,140],w:[[620,260,620,620]],s:[[690,300,160,90]],c:[]},
 {par:3,tee:[140,320],cup:[810,320],w:[[340,140,640,270],[340,500,640,370]],s:[],c:[]},
 {par:4,tee:[120,120],cup:[840,540],w:[[20,220,620,220],[340,420,940,420]],s:[],c:[]},
 {par:3,tee:[140,320],cup:[820,320],w:[],s:[[380,520,200,60]],c:[[480,238,34],[480,402,34]]},
 {par:3,tee:[140,540],cup:[480,335],w:[[360,220,360,420],[360,420,600,420],[600,220,600,420]],s:[],c:[]},
 {par:3,tee:[140,320],cup:[820,320],w:[[680,120,680,520]],s:[],c:[]},
 {par:4,tee:[120,540],cup:[840,540],w:[[320,20,320,380],[560,260,560,620],[780,20,780,300]],s:[],c:[]},
 {par:4,tee:[130,320],cup:[870,320],w:[[330,120,610,290],[330,520,610,350]],s:[[540,430,170,70]],c:[[770,320,26]]},
].map(h => !arcadeVariant ? h : {...h,
  tee:[h.tee[0],H-h.tee[1]], cup:[h.cup[0],H-h.cup[1]],
  w:h.w.map(([x,y,X,Y])=>[x,H-y,X,H-Y]),
  s:h.s.map(([x,y,w,h])=>[x,H-y-h,w,h]), c:h.c.map(([x,y,r])=>[x,H-y,r]) });

// ===== AAA audio kit (procedural; honors overlay prefs) =====
let AC=null;
function ac(){if(!AC){try{AC=new(window.AudioContext||window.webkitAudioContext)()}catch(e){}}if(AC&&AC.state==='suspended')AC.resume();return AC}
function sfxOn(){try{return arcadeStorage.getItem('aaa_sfx')!=='0'}catch(e){return true}}
function bgmOn(){try{return arcadeStorage.getItem('aaa_bgm')!=='0'}catch(e){return true}}
function tone(f,d,ty,v,sl,t){if(!sfxOn())return;const c=ac();if(!c)return;try{const n=c.currentTime+(t||0),o=c.createOscillator(),g=c.createGain();o.type=ty||'sine';o.frequency.setValueAtTime(f,n);if(sl)o.frequency.exponentialRampToValueAtTime(Math.max(30,sl),n+d);g.gain.setValueAtTime(.0008,n);g.gain.linearRampToValueAtTime(v||.08,n+.012);g.gain.exponentialRampToValueAtTime(.0006,n+d);o.connect(g);g.connect(c.destination);o.start(n);o.stop(n+d+.05)}catch(e){}}
function nz(d,v,f,q,ty,t){if(!sfxOn())return;const c=ac();if(!c)return;try{const n=c.currentTime+(t||0),L=Math.ceil(c.sampleRate*d),b=c.createBuffer(1,L,c.sampleRate),dd=b.getChannelData(0);for(let i=0;i<L;i++)dd[i]=(Math.random()*2-1)*(1-i/L);const s=c.createBufferSource();s.buffer=b;const fl=c.createBiquadFilter();fl.type=ty||'bandpass';fl.frequency.value=f||1400;fl.Q.value=q||.8;const g=c.createGain();g.gain.setValueAtTime(v||.1,n);g.gain.exponentialRampToValueAtTime(.0006,n+d);s.connect(fl);fl.connect(g);g.connect(c.destination);s.start(n)}catch(e){}}
function mtone(f,d,ty,v,sl,t){const c=ac();if(!c)return;try{const n=c.currentTime+(t||0),o=c.createOscillator(),g=c.createGain();o.type=ty||'sine';o.frequency.setValueAtTime(f,n);if(sl)o.frequency.exponentialRampToValueAtTime(Math.max(30,sl),n+d);g.gain.setValueAtTime(.0008,n);g.gain.linearRampToValueAtTime(v||.05,n+.02);g.gain.exponentialRampToValueAtTime(.0006,n+d);o.connect(g);g.connect(c.destination);o.start(n);o.stop(n+d+.05)}catch(e){}}
function stinger(w){(w?[523,659,784,1047]:[392,349,311,262]).forEach(function(f,i){(w?mtone:tone)(f,w?.2:.24,w?'triangle':'sawtooth',w?.09:.05,null,i*(w?.1:.12))})}
// fairway ambient: pads + birds
let bgmI=0,bgmNext=-1;const BSTEP=60/88/2;const ROOTS=[110,98,87.31,73.42];
setInterval(()=>{if(!bgmOn()){bgmNext=-1;return}const c=ac();if(!c||c.state!=='running')return;if(bgmNext<0)bgmNext=c.currentTime+.06;const a=c.currentTime+.32;while(bgmNext<a){const s=bgmI%16,t=bgmNext,R=ROOTS[(bgmI>>4)%4];
  if(s===0){mtone(R,3.2,'sine',.03,null,t);mtone(R*1.5,3.2,'sine',.018,null,t);mtone(R*2.003,3.2,'sine',.012,null,t)}
  if(s===5&&Math.random()<.4)mtone(1900+Math.random()*800,.11,'sine',.013,2500+Math.random()*700,t);
  if(s===11&&Math.random()<.3)mtone(2300+Math.random()*700,.08,'sine',.011,1700,t);
  bgmNext+=BSTEP;bgmI++}},80);
const scoreChip=document.getElementById('aaa-score');
function chipUpdate(v){if(scoreChip)scoreChip.textContent=v}
let bestRound=0;try{bestRound=+arcadeStorage.getItem('mg_best')||0}catch(e){}

let hi=0, strokes=0, scores=[], state="play";
let ball={x:0,y:0,vx:0,vy:0}, sinkAnim=0;
let drag={on:false,x:0,y:0};
let walls=[], lastT=0, overlayTimer=null;

const hole=()=>HOLES[hi];
const totalStrokes=()=>scores.reduce((a,s)=>a+s,0)+strokes;
const totalPar=()=>HOLES.slice(0,scores.length).reduce((a,h)=>a+h.par,0)+hole().par;

function loadHole(i){
  hi=i; strokes=0; state="play"; sinkAnim=0; drag.on=false;
  const h=hole();
  ball={x:h.tee[0],y:h.tee[1],vx:0,vy:0};
  walls=BOUNDARY.concat(h.w);
  chipUpdate(0);
  if(T3)buildScene();
  updateInfo(); hideOverlay();
}
function updateInfo(){
  infoEl.innerHTML=`Chuqurcha <b>${hi+1}/9</b> · Me’yor ${hole().par} · Zarba <b>${strokes}</b> · Jami <b>${totalStrokes()}</b>`;
}
function restartHole(){
  const keep=scores.slice(); // completed-hole scores stay
  loadHole(hi); scores=keep; updateInfo();
}
function restartGame(){ scores=[]; clearTimeout(overlayTimer); loadHole(0); }

// ---------- physics ----------
function closestOnSeg(x1,y1,x2,y2,px,py){
  const dx=x2-x1,dy=y2-y1,l2=dx*dx+dy*dy;
  let t=l2?((px-x1)*dx+(py-y1)*dy)/l2:0; t=Math.max(0,Math.min(1,t));
  return [x1+dx*t,y1+dy*t];
}
function collide(px,py,vx,vy,nx,ny){
  const dot=vx*nx+vy*ny;
  if(dot<0){
    vx-=2*dot*nx; vy-=2*dot*ny; vx*=REST; vy*=REST;
    if(dot<-0.8){nz(.07,Math.min(.14,-dot*.02),700,-1,'bandpass');tone(300+Math.min(300,-dot*30),.05,'triangle',Math.min(.1,-dot*.015));}
  }
  return [px,py,vx,vy];
}
function physStep(f){
  const h=hole();
  let vx=ball.vx*f, vy=ball.vy*f;
  // gentle cup attraction when close
  const cdx=h.cup[0]-ball.x, cdy=h.cup[1]-ball.y, cd=Math.hypot(cdx,cdy);
  if(cd<26&&cd>0.01){vx+=cdx/cd*0.05*f;vy+=cdy/cd*0.05*f;}
  ball.x+=vx; ball.y+=vy;
  // walls
  for(const s of walls){
    const[px,py]=closestOnSeg(s[0],s[1],s[2],s[3],ball.x,ball.y);
    const dx=ball.x-px,dy=ball.y-py,d=Math.hypot(dx,dy),min=BALL_R+WALL_T/2;
    if(d<min){
      let nx,ny;
      if(d>1e-4){nx=dx/d;ny=dy/d;}else{const l=Math.hypot(s[2]-s[0],s[3]-s[1])||1;nx=-(s[3]-s[1])/l;ny=(s[2]-s[0])/l;}
      ball.x=px+nx*min; ball.y=py+ny*min;
      const r=collide(ball.x,ball.y,ball.vx,ball.vy,nx,ny);
      ball.vx=r[2]; ball.vy=r[3];
    }
  }
  // circular obstacles
  for(const c of h.c){
    const dx=ball.x-c[0],dy=ball.y-c[1],d=Math.hypot(dx,dy),min=BALL_R+c[2];
    if(d<min){
      const nx=d>1e-4?dx/d:1, ny=d>1e-4?dy/d:0;
      ball.x=c[0]+nx*min; ball.y=c[1]+ny*min;
      const r=collide(ball.x,ball.y,ball.vx,ball.vy,nx,ny);
      ball.vx=r[2]; ball.vy=r[3];
    }
  }
}
function update(dt){
  if(state==="sunk"){sinkAnim+=dt;return;}
  const f=Math.min(dt/16.667,3);
  const sp=Math.hypot(ball.vx,ball.vy);
  if(sp>0.001){
    const steps=Math.max(1,Math.ceil(sp*f/6)); // substep to avoid tunneling
    for(let i=0;i<steps;i++) physStep(f/steps);
    const inSand=hole().s.some(r=>ball.x>r[0]&&ball.x<r[0]+r[2]&&ball.y>r[1]&&ball.y<r[1]+r[3]);
    if(inSand&&sp>1&&Math.random()<dt*.02)nz(.12,.05,400,.8,'lowpass');
    const fr=Math.pow(inSand?SAND_FR:FRICTION,f);
    ball.vx*=fr; ball.vy*=fr;
    if(Math.hypot(ball.vx,ball.vy)<0.07){ball.vx=ball.vy=0;}
    // cup capture: must be slow enough, else lips out
    const cd=Math.hypot(hole().cup[0]-ball.x,hole().cup[1]-ball.y);
    if(cd<CUP_R-1&&Math.hypot(ball.vx,ball.vy)<3.4){
      ball.vx=ball.vy=0; state="sunk"; sinkAnim=0;
      nz(.3,.16,900,1.2);tone(300,.14,'triangle',.09,180);tone(600,.12,'sine',.07,null,.12);
      onSunk();
    }
    else if(cd<CUP_R+3&&Math.hypot(ball.vx,ball.vy)>=3.4&&!ball._lipped){
      ball._lipped=true;tone(400,.08,'triangle',.05,300);nz(.08,.06,1200,1.5);
    }
  }
}

// ---------- scoring / flow ----------
function onSunk(){
  scores[hi]=strokes;
  const diff=strokes-hole().par;
  stinger(diff<=0);
  if(diff<=-2){tone(880,.16,'sine',.08);tone(1174,.22,'sine',.07,null,.12);}
  if(hi<HOLES.length-1){
    showOverlay(`<div class="card"><h2>Chuqurcha ${hi+1} · ${PAR_NAMES(diff)}</h2>
      <p>${strokes} ta zarba (me’yor ${hole().par})</p>
      <button class="primary" id="nextBtn">Keyingi chuqurcha <svg width="12" height="12" viewBox="0 0 12 12" style="vertical-align:-1px"><path d="M3 1.5 7.5 6 3 10.5" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/></svg></button></div>`);
    document.getElementById("nextBtn").onclick=nextHole;
    overlayTimer=setTimeout(nextHole,1800);
  }else showScorecard();
}
function nextHole(){ clearTimeout(overlayTimer); const keep=scores.slice(); loadHole(hi+1); scores=keep; updateInfo(); }
function showScorecard(){
  const tot=scores.reduce((a,b)=>a+b,0), par=HOLES.reduce((a,h)=>a+h.par,0);
  const nb=!bestRound||tot<bestRound;
  if(nb){bestRound=tot;try{arcadeStorage.setItem('mg_best',String(tot))}catch(e){}}
  let rows=HOLES.map((h,i)=>`<tr><td>${i+1}</td><td>${h.par}</td><td>${scores[i]}</td></tr>`).join("");
  showOverlay(`<div class="card"><h2>O‘yin tugadi!${nb?" — YANGI REKORD":""}</h2>
    <p>Eng yaxshi natija: ${bestRound}</p>
    <table><tr><th>Chuqurcha</th><th>Par</th><th>You</th></tr>${rows}
    <tr class="tot"><td>Jami</td><td>${par}</td><td>${tot} (${PAR_NAMES(tot-par)})</td></tr></table>
    <button class="primary" id="againBtn">Yana o‘ynash</button></div>`);
  document.getElementById("againBtn").onclick=restartGame;
}
function showOverlay(html){overlay.innerHTML=html;overlay.classList.remove("hidden");}
function hideOverlay(){overlay.classList.add("hidden");overlay.innerHTML="";}
overlay.addEventListener("click",e=>{if(e.target===overlay&&hi<HOLES.length-1&&state==="sunk")nextHole();});

// ---------- input ----------
function toCanvas(e){
  const r=cv.getBoundingClientRect();
  return [(e.clientX-r.left)*W/r.width,(e.clientY-r.top)*H/r.height];
}
cv.addEventListener("pointerdown",e=>{
  if(state!=="play"||Math.hypot(ball.vx,ball.vy)>0.001)return;
  const[x,y]=toCanvas(e);
  if(Math.hypot(x-ball.x,y-ball.y)<46){drag.on=true;drag.x=x;drag.y=y;cv.setPointerCapture(e.pointerId);}
});
cv.addEventListener("pointermove",e=>{if(drag.on){const[x,y]=toCanvas(e);drag.x=x;drag.y=y;}});
cv.addEventListener("pointerup",e=>{
  if(!drag.on)return; drag.on=false;
  hitBall(ball.x-drag.x, ball.y-drag.y);
});
cv.addEventListener("pointercancel",()=>{ drag.on=false; });
function hitBall(dx,dy){
  const d=Math.hypot(dx,dy);
  if(d<8)return; // too small, cancel
  const p=Math.min(d,MAX_DRAG)*PWR_K;
  ball._lipped=false;
  ball.vx=dx/d*p; ball.vy=dy/d*p;
  const sp=Math.hypot(ball.vx,ball.vy);
  if(sp>MAX_V){ball.vx*=MAX_V/sp;ball.vy*=MAX_V/sp;}
  strokes++; updateInfo(); chipUpdate(strokes);
  tone(900+d*2,.04,'square',.05,500);
  nz(.3,Math.min(.14,p*.02),1400,1,'bandpass');
}
document.getElementById("btnHole").onclick=restartHole;
document.getElementById("btnGame").onclick=restartGame;
let aimAngle=0, aimPower=40;
document.addEventListener("keydown",e=>{
  if(e.code==="KeyR"){ restartHole(); return; }
  if(e.code==="Enter" && state==="sunk"){ document.getElementById("nextBtn")?.click(); return; }
  if(state!=="play" || drag.on || Math.hypot(ball.vx,ball.vy)>0.001 ||
     !["ArrowLeft","ArrowRight","ArrowUp","ArrowDown","Space"].includes(e.code))return;
  if(e.code==="Space" && e.target.closest("button"))return;
  e.preventDefault();
  if(e.code==="ArrowLeft")aimAngle-=0.12;
  if(e.code==="ArrowRight")aimAngle+=0.12;
  if(e.code==="ArrowUp")aimPower=Math.min(MAX_DRAG,aimPower+6);
  if(e.code==="ArrowDown")aimPower=Math.max(12,aimPower-6);
  if(e.code==="Space" && !e.repeat)hitBall(Math.cos(aimAngle)*aimPower,Math.sin(aimAngle)*aimPower);
});

// ---------- render ----------
function drawCourse(){
  // felt
  ctx.fillStyle="#2f8f57";ctx.fillRect(0,0,W,H);
  ctx.save();ctx.globalAlpha=0.06;ctx.fillStyle="#fff";
  for(let y=0;y<H;y+=40)for(let x=(y/40%2)*20;x<W;x+=40)ctx.fillRect(x,y,20,20);
  ctx.restore();
  const h=hole();
  // sand traps
  for(const r of h.s){
    ctx.fillStyle="#d8c58a";
    ctx.beginPath();ctx.roundRect(r[0],r[1],r[2],r[3],14);ctx.fill();
    ctx.strokeStyle="#b6a26a";ctx.lineWidth=2;ctx.stroke();
  }
  // tee marker
  ctx.strokeStyle="rgba(255,255,255,.35)";ctx.lineWidth=1.5;
  ctx.beginPath();ctx.arc(h.tee[0],h.tee[1],12,0,7);ctx.stroke();
  // cup
  ctx.fillStyle="#0d1410";
  ctx.beginPath();ctx.arc(h.cup[0],h.cup[1],CUP_R,0,7);ctx.fill();
  ctx.strokeStyle="#cfe8d8";ctx.lineWidth=2;ctx.stroke();
  // flag
  ctx.strokeStyle="#e8e8e8";ctx.lineWidth=2;
  ctx.beginPath();ctx.moveTo(h.cup[0],h.cup[1]);ctx.lineTo(h.cup[0],h.cup[1]-34);ctx.stroke();
  ctx.fillStyle="#e2443b";
  ctx.beginPath();ctx.moveTo(h.cup[0],h.cup[1]-34);ctx.lineTo(h.cup[0]+20,h.cup[1]-28);ctx.lineTo(h.cup[0],h.cup[1]-22);ctx.fill();
  // walls
  ctx.lineCap="round";ctx.lineWidth=WALL_T;
  for(const s of walls){
    ctx.strokeStyle="#5a3d22";ctx.beginPath();ctx.moveTo(s[0],s[1]);ctx.lineTo(s[2],s[3]);ctx.stroke();
    ctx.strokeStyle="#8a6236";ctx.lineWidth=WALL_T-5;ctx.beginPath();ctx.moveTo(s[0],s[1]);ctx.lineTo(s[2],s[3]);ctx.stroke();
    ctx.lineWidth=WALL_T;
  }
  // circular obstacles
  for(const c of h.c){
    ctx.fillStyle="#5a3d22";ctx.beginPath();ctx.arc(c[0],c[1],c[2],0,7);ctx.fill();
    ctx.fillStyle="#8a6236";ctx.beginPath();ctx.arc(c[0],c[1],c[2]-4,0,7);ctx.fill();
  }
}
function drawBall(){
  const r=state==="sunk"?Math.max(0,BALL_R*(1-sinkAnim/300)):BALL_R;
  if(r<=0)return;
  ctx.fillStyle="rgba(0,0,0,.25)";
  ctx.beginPath();ctx.ellipse(ball.x+2,ball.y+3,r,r*0.8,0,0,7);ctx.fill();
  const g=ctx.createRadialGradient(ball.x-2.5,ball.y-2.5,1,ball.x,ball.y,r);
  g.addColorStop(0,"#ffffff");g.addColorStop(1,"#c9ccd2");
  ctx.fillStyle=g;ctx.beginPath();ctx.arc(ball.x,ball.y,r,0,7);ctx.fill();
  ctx.strokeStyle="#9aa0a8";ctx.lineWidth=1;ctx.stroke();
}
function drawAim(){
  if(!drag.on && state==="play" && Math.hypot(ball.vx,ball.vy)<=0.001){
    ctx.save();
    ctx.strokeStyle="#fff7c2";
    ctx.setLineDash([4,5]);
    ctx.beginPath();
    ctx.moveTo(ball.x,ball.y);
    ctx.lineTo(ball.x+Math.cos(aimAngle)*aimPower,ball.y+Math.sin(aimAngle)*aimPower);
    ctx.stroke();
    ctx.restore();
  }
  if(!drag.on)return;
  const dx=ball.x-drag.x,dy=ball.y-drag.y,d=Math.hypot(dx,dy);
  if(d<8)return;
  const p=Math.min(d,MAX_DRAG),vx=dx/d,vy=dy/d;
  // pull-back line
  ctx.strokeStyle="rgba(255,255,255,.4)";ctx.lineWidth=1.5;ctx.setLineDash([4,4]);
  ctx.beginPath();ctx.moveTo(ball.x,ball.y);ctx.lineTo(drag.x,drag.y);ctx.stroke();
  ctx.setLineDash([]);
  // aim arrow (dotted, scales with power)
  const len=40+p*1.1;
  ctx.strokeStyle="#ffd34d";ctx.lineWidth=3;ctx.setLineDash([2,9]);ctx.lineCap="round";
  ctx.beginPath();ctx.moveTo(ball.x+vx*14,ball.y+vy*14);
  ctx.lineTo(ball.x+vx*len,ball.y+vy*len);ctx.stroke();ctx.setLineDash([]);
  // arrowhead
  const ax=ball.x+vx*len,ay=ball.y+vy*len,px=-vy,py=vx;
  ctx.fillStyle="#ffd34d";
  ctx.beginPath();ctx.moveTo(ax+vx*10,ay+vy*10);
  ctx.lineTo(ax+px*6,ay+py*6);ctx.lineTo(ax-px*6,ay-py*6);ctx.fill();
  // power bar
  const pw=p/MAX_DRAG;
  ctx.fillStyle="rgba(0,0,0,.45)";ctx.fillRect(W/2-110,H-26,220,12);
  ctx.fillStyle=pw<.5?"#7bd88a":pw<.8?"#ffd34d":"#ff6b57";
  ctx.fillRect(W/2-108,H-24,216*pw,8);
  ctx.strokeStyle="rgba(255,255,255,.4)";ctx.lineWidth=1;
  ctx.strokeRect(W/2-110,H-26,220,12);
}
// ---------- Three.js 3D course (guarded, optional) ----------
let T3=null, t3={};
function init3D(){
  if(T3||!window.THREE)return;
  try{
    const r=new THREE.WebGLRenderer({antialias:true,alpha:true});
    r.setPixelRatio(Math.min(window.devicePixelRatio||1,2));
    r.setSize(W,H);
    r.domElement.style.cssText='position:absolute;inset:0;width:100%;height:100%;pointer-events:none;border-radius:0 0 10px 10px';
    r.shadowMap.enabled=true;
    document.getElementById('stage').appendChild(r.domElement);
    const sc=new THREE.Scene();
    sc.fog=new THREE.Fog(0x1a2520,900,2400);
    const cam=new THREE.PerspectiveCamera(44,W/H,10,4000);
    cam.position.set(0,640,380);cam.lookAt(0,0,-40);
    sc.add(new THREE.AmbientLight(0xffffff,.5));
    const dl=new THREE.DirectionalLight(0xfff2d8,.95);
    dl.position.set(-260,560,180);dl.castShadow=true;
    dl.shadow.mapSize.set(1024,1024);
    dl.shadow.camera.left=-560;dl.shadow.camera.right=560;
    dl.shadow.camera.top=560;dl.shadow.camera.bottom=-560;
    sc.add(dl);
    const dl2=new THREE.DirectionalLight(0x88bbff,.25);
    dl2.position.set(300,200,-300);sc.add(dl2);
    T3={r,sc,cam,grp:null,ball:null,shaft:null,cone:null};
    cv.style.opacity='0';
    buildScene();
  }catch(e){T3=null;}
}
const X=v=>v-W/2, Z=v=>v-H/2;
function buildScene(){
  if(!T3)return;
  if(T3.grp)T3.sc.remove(T3.grp);
  const g=new THREE.Group();T3.grp=g;T3.sc.add(g);
  const h=hole();
  // felt
  const felt=new THREE.Mesh(new THREE.PlaneGeometry(W,H),
    new THREE.MeshLambertMaterial({color:0x2f8f57}));
  felt.rotation.x=-Math.PI/2;felt.receiveShadow=true;g.add(felt);
  // fringe border
  const edge=new THREE.Mesh(new THREE.PlaneGeometry(W+70,H+70),
    new THREE.MeshLambertMaterial({color:0x256b41}));
  edge.rotation.x=-Math.PI/2;edge.position.y=-2;g.add(edge);
  const woodM=new THREE.MeshLambertMaterial({color:0x8a6236});
  const woodD=new THREE.MeshLambertMaterial({color:0x5a3d22});
  // sand
  for(const r of h.s){
    const m=new THREE.Mesh(new THREE.BoxGeometry(r[2],5,r[3]),
      new THREE.MeshLambertMaterial({color:0xd8c58a}));
    m.position.set(X(r[0]+r[2]/2),2.5,Z(r[1]+r[3]/2));m.receiveShadow=true;g.add(m);
  }
  // walls (incl. boundary)
  for(const s of walls){
    const dx=s[2]-s[0],dy=s[3]-s[1],len=Math.hypot(dx,dy)||1;
    const m=new THREE.Mesh(new THREE.BoxGeometry(len,30,WALL_T+8),woodM);
    m.position.set(X((s[0]+s[2])/2),15,Z((s[1]+s[3])/2));
    m.rotation.y=-Math.atan2(dy,dx);
    m.castShadow=m.receiveShadow=true;g.add(m);
  }
  // circular obstacles
  for(const c of h.c){
    const m=new THREE.Mesh(new THREE.CylinderGeometry(c[2]+3,c[2]+3,30,24),woodM);
    m.position.set(X(c[0]),15,Z(c[1]));m.castShadow=m.receiveShadow=true;g.add(m);
  }
  // cup hole + flag
  const cupM=new THREE.Mesh(new THREE.CylinderGeometry(CUP_R,CUP_R,4,24),
    new THREE.MeshBasicMaterial({color:0x0d1410}));
  cupM.position.set(X(h.cup[0]),1,Z(h.cup[1]));g.add(cupM);
  const pole=new THREE.Mesh(new THREE.CylinderGeometry(1,1,44,8),
    new THREE.MeshLambertMaterial({color:0xf0f0f0}));
  pole.position.set(X(h.cup[0]),22,Z(h.cup[1]));pole.castShadow=true;g.add(pole);
  const flag=new THREE.Mesh(new THREE.PlaneGeometry(22,12),
    new THREE.MeshLambertMaterial({color:0xe2443b,side:THREE.DoubleSide}));
  flag.position.set(X(h.cup[0])+11,38,Z(h.cup[1]));g.add(flag);
  T3.flag=flag;
  // tee ring
  const ring=new THREE.Mesh(new THREE.RingGeometry(10,12,24),
    new THREE.MeshBasicMaterial({color:0xffffff,transparent:true,opacity:.4,side:THREE.DoubleSide}));
  ring.rotation.x=-Math.PI/2;ring.position.set(X(h.tee[0]),.6,Z(h.tee[1]));g.add(ring);
  // ball
  T3.ball=new THREE.Mesh(new THREE.SphereGeometry(BALL_R,20,16),
    new THREE.MeshPhongMaterial({color:0xffffff,shininess:110}));
  T3.ball.castShadow=true;g.add(T3.ball);
  // aim arrow
  const am=new THREE.MeshLambertMaterial({color:0xffd34d});
  T3.shaft=new THREE.Mesh(new THREE.BoxGeometry(1,4,4),am);
  T3.cone=new THREE.Mesh(new THREE.ConeGeometry(7,20,10),am);
  T3.cone.rotation.z=-Math.PI/2;
  T3.aim=new THREE.Group();
  T3.aim.add(T3.shaft);T3.aim.add(T3.cone);T3.aim.visible=false;g.add(T3.aim);
}
function render3D(){
  const h=hole();
  const r=state==="sunk"?Math.max(0.001,1-sinkAnim/300):1;
  T3.ball.position.set(X(ball.x),BALL_R*r,Z(ball.y));
  T3.ball.scale.setScalar(r);
  // flag flutters
  if(T3.flag)T3.flag.rotation.y=Math.sin(performance.now()*.004)*.4;
  // aim arrow
  const dx=ball.x-drag.x,dy=ball.y-drag.y,d=Math.hypot(dx,dy);
  if(drag.on&&d>8){
    const p=Math.min(d,MAX_DRAG),vx=dx/d,vy=dy/d,len=40+p*1.1;
    T3.aim.visible=true;
    T3.aim.position.set(X(ball.x),6,Z(ball.y));
    T3.aim.rotation.y=-Math.atan2(vy,vx);
    T3.shaft.scale.x=len;
    T3.shaft.position.x=len/2+16;
    T3.cone.position.x=len+26;
    T3.shaft.material.color.setHex(p/MAX_DRAG>.8?0xff6b57:0xffd34d);
  }else T3.aim.visible=false;
  T3.r.render(T3.sc,T3.cam);
}

function frame(t){
  const dt=lastT?Math.min(t-lastT,50):16.667; lastT=t;
  update(dt);
  if(window.THREE&&!T3)init3D();
  if(T3){render3D();}
  else{drawCourse(); drawBall(); drawAim();}
  requestAnimationFrame(frame);
}
ctx.roundRect=ctx.roundRect||function(x,y,w,h,r){this.moveTo(x+r,y);this.arcTo(x+w,y,x+w,y+h,r);this.arcTo(x+w,y+h,x,y+h,r);this.arcTo(x,y+h,x,y,r);this.arcTo(x,y,x+w,y,r);};
loadHole(0);
requestAnimationFrame(frame);

  // Report native gameplay state to the optional mission engine.
  window.__arcadeMissionSource = Object.assign({"id":49,"label":"Holes under control","goals":[2,5,8],"note":"Sink each hole in no more than one stroke over par. Skipped holes do not qualify.","seconds":420}, { read: () => { return (scores.filter((s,i) => Number.isFinite(s) && s <= HOLES[i].par + 1).length); } });
