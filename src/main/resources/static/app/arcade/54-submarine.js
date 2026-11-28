// Arcade 100 · MIT · see LICENSE
window.__arcadeNativeUpgrade = {"id": 54, "name": "Silent Running", "description": "Mines stay live, but releasing the dive control reduces torpedo homing. Alternate short dives and quiet climbs."}; const arcadeVariant = new URLSearchParams(location.search).get("mode") === "plus" || new URLSearchParams(location.search).get("plus") === "1";

'use strict';
var cv=document.getElementById('game'),ctx=cv.getContext('2d');
var W=0,H=0,T=0;
function resize(){var d=Math.min(window.devicePixelRatio||1,2);W=window.innerWidth;H=window.innerHeight;cv.width=W*d|0;cv.height=H*d|0;cv.style.width=W+'px';cv.style.height=H+'px';ctx.setTransform(d,0,0,d,0,0);}
window.addEventListener('resize',resize);resize();

// ---- audio (tiny synth, created on first input) ----
var AC=null,muted=false;
function audio(){if(!AC){try{AC=new(window.AudioContext||window.webkitAudioContext)();}catch(e){}}if(AC&&AC.state==='suspended')AC.resume();return AC;}
function sfxOff(){return muted||(window.AAA&&!AAA.sfxOn);}
function tone(f,dur,type,vol,slide){if(sfxOff())return;var ac=audio();if(!ac)return;try{var o=ac.createOscillator(),g=ac.createGain();o.type=type||'sine';o.frequency.setValueAtTime(f,ac.currentTime);if(slide)o.frequency.exponentialRampToValueAtTime(slide,ac.currentTime+dur);g.gain.setValueAtTime(vol||0.07,ac.currentTime);g.gain.exponentialRampToValueAtTime(0.0001,ac.currentTime+dur);o.connect(g);g.connect(ac.destination);o.start();o.stop(ac.currentTime+dur);}catch(e){}}
function boomSnd(){if(sfxOff())return;var ac=audio();if(!ac)return;try{var len=ac.sampleRate*0.5|0,b=ac.createBuffer(1,len,ac.sampleRate),d=b.getChannelData(0);for(var i=0;i<len;i++)d[i]=(Math.random()*2-1)*(1-i/len);var n=ac.createBufferSource();n.buffer=b;var f=ac.createBiquadFilter();f.type='lowpass';f.frequency.setValueAtTime(900,ac.currentTime);f.frequency.exponentialRampToValueAtTime(120,ac.currentTime+0.5);var g=ac.createGain();g.gain.setValueAtTime(0.35,ac.currentTime);g.gain.exponentialRampToValueAtTime(0.001,ac.currentTime+0.5);n.connect(f);f.connect(g);g.connect(ac.destination);n.start();}catch(e){}}

var best=0;try{best=+arcadeStorage.getItem('sub_best')||0;}catch(e){}
function saveBest(){try{arcadeStorage.setItem('sub_best',''+best);}catch(e){}}

// ---- state ----
var state='title',holding=false,camX=0;
var sub,mines,torps,fish,parts,weed;
var dist=0,speed=240,deadT=0,deathCause='',lastMile=0;
var tmMine=0,tmTorp=0,tmFish=0,tmWeed=0,tmPing=0;

function clamp(v,a,b){return v<a?a:v>b?b:v;}
function rnd(a,b){return a+Math.random()*(b-a);}
function rr(x,y,w,h,r){ctx.beginPath();ctx.moveTo(x+r,y);ctx.arcTo(x+w,y,x+w,y+h,r);ctx.arcTo(x+w,y+h,x,y+h,r);ctx.arcTo(x,y+h,x,y,r);ctx.arcTo(x,y,x+w,y,r);ctx.closePath();}
function surfY(x){return 34+5*Math.sin(x*0.028+T*1.6)+4*Math.sin(x*0.011-T*0.9);}
function floorY(x){return H-58+16*Math.sin((x+camX)*0.006)+9*Math.sin((x+camX)*0.017+3.1);}
function meters(){return Math.floor(dist/45);}

function reset(){
  sub={x:W*0.26,y:H*0.45,vy:0,tilt:0};
  mines=[];torps=[];fish=[];parts=[];weed=[];
  dist=0;speed=240;deadT=0;lastMile=0;deathCause='';
  tmMine=1.4;tmTorp=5;tmFish=0.5;tmWeed=0.1;tmPing=2.5;
}
reset();

function die(cause){
  if(state!=='play')return;
  state='dead';deadT=0;deathCause=cause;holding=false;
  var m=meters();if(m>best){best=m;saveBest();}
  boomSnd();tone(160,0.5,'sawtooth',0.12,60);
  if(window.AAA){AAA.flash(0.3);AAA.burst(sub.x,sub.y,'#ffb03a',22);AAA.shockwave(sub.x,sub.y,'#ff6b5e',90);}
  for(var i=0;i<46;i++)parts.push({k:'boom',x:sub.x+rnd(-10,10),y:sub.y+rnd(-8,8),vx:rnd(-220,220),vy:rnd(-220,140),r:rnd(2,5),life:rnd(0.5,1.1)});
  for(i=0;i<20;i++)parts.push({k:'bub',x:sub.x+rnd(-16,16),y:sub.y+rnd(-8,8),vx:rnd(-40,40),vy:rnd(-140,-40),r:rnd(1.5,4),life:rnd(0.8,1.6)});
}

function spawnMine(){var top=surfY(0)+60,bot=Math.max(top+40,H-140);mines.push({x:W+70,y:rnd(top,bot),r:rnd(15,24),ph:rnd(0,7)});}
function spawnTorp(){var y=clamp(sub.y+rnd(-140,140),60,Math.max(120,H-160));torps.push({x:W+90,y:y,v:speed+rnd(240,300),ph:rnd(0,7)});}
function spawnFish(){fish.push({x:W+30,y:rnd(90,Math.max(140,H-160)),v:rnd(40,110),s:rnd(0.7,1.3),ph:rnd(0,7)});}
function spawnWeed(){var r=Math.random();weed.push(r<0.55?{type:'weed',x:W+50,h:rnd(30,90),blades:2+(Math.random()*3|0),ph:rnd(0,7),col:['#1d5c43','#256b4e','#17493a'][Math.random()*3|0]}:{type:'rock',x:W+50,w:rnd(24,70),h:rnd(12,30)});}
function ambient(dt){if(Math.random()<dt*3)parts.push({k:'mote',x:rnd(0,W),y:rnd(0,H),vx:0,vy:rnd(-26,-12),r:rnd(1,2),life:rnd(4,8)});}

function moveEnts(dt){
  var sc=state==='title'?55:(state==='dead'?speed*Math.max(0,1-deadT*1.5):speed);
  var i;
  for(i=mines.length-1;i>=0;i--){var m=mines[i];m.x-=sc*0.94*dt;m.y+=Math.sin(T*2+m.ph)*8*dt;
    if(m.x<-60){mines.splice(i,1);continue;}
    if(state==='play'){var dx=sub.x-m.x,dy=sub.y-m.y,rad=m.r+14;if(dx*dx+dy*dy<rad*rad)die('You hit a naval mine');}}
  for(i=torps.length-1;i>=0;i--){var tp=torps[i];tp.x-=tp.v*dt;
    if(state==='play'){tp.y+=clamp(sub.y-tp.y,-70,70)*dt*(arcadeVariant ? (holding ? 1.2 : .07) : .5);
      if(Math.random()<dt*30)parts.push({k:'bub',x:tp.x+26,y:tp.y+rnd(-3,3),vx:rnd(-30,-10),vy:rnd(-30,0),r:rnd(1,2.5),life:0.7});}
    if(tp.x<-80){torps.splice(i,1);continue;}
    if(state==='play'){var cx=clamp(sub.x,tp.x-26,tp.x+22),ddx=sub.x-cx,ddy=sub.y-tp.y;if(ddx*ddx+ddy*ddy<289)die('Torpedoed!');}}
  for(i=fish.length-1;i>=0;i--){var f=fish[i];f.x-=(sc*0.5+f.v)*dt;if(f.x<-40)fish.splice(i,1);}
  for(i=weed.length-1;i>=0;i--){var w=weed[i];w.x-=sc*dt;if(w.x<-60)weed.splice(i,1);}
}

function updParts(dt){for(var i=parts.length-1;i>=0;i--){var p=parts[i];p.life-=dt;if(p.life<=0){parts.splice(i,1);continue;}p.x+=(p.vx||0)*dt;p.y+=(p.vy||0)*dt;if(p.k==='bub'){p.vy-=60*dt;p.x+=Math.sin(T*6+p.r*9)*20*dt;if(p.y<surfY(p.x))p.life=0;}else if(p.k==='boom'){p.vy+=260*dt;p.vx*=0.98;}}}

function update(dt){
  if(state==='title'){
    camX+=55*dt;sub.y=H*0.45+Math.sin(T*1.4)*10;sub.tilt=Math.sin(T*1.1)*0.08;
    tmFish-=dt;if(tmFish<=0){tmFish=rnd(1,3);spawnFish();}
    tmWeed-=dt;if(tmWeed<=0){tmWeed=rnd(0.5,1.2);spawnWeed();}
    moveEnts(dt);updParts(dt);ambient(dt);return;
  }
  if(state==='dead'){
    deadT+=dt;camX+=speed*dt*Math.max(0,1-deadT*1.5);
    sub.tilt+=(1.5-sub.tilt)*dt*2;
    var fl=floorY(sub.x)-4;
    if(sub.y<fl){sub.vy+=420*dt;sub.y+=sub.vy*dt;if(sub.y>fl)sub.y=fl;}
    moveEnts(dt);updParts(dt);ambient(dt);return;
  }
  // ---- play ----
  speed=240+Math.min(470,dist*0.009);
  camX+=speed*dt;dist+=speed*dt;
  var m=meters();
  if(Math.floor(m/250)>lastMile){lastMile=Math.floor(m/250);tone(880,0.1,'sine',0.05);tone(1320,0.16,'sine',0.04);if(window.AAA){AAA.popup(W/2,90,'+'+lastMile*250+'m','#ffd257');AAA.sfx.level&&AAA.sfx.level();}}
  tmPing-=dt;if(tmPing<=0){tmPing=rnd(3,6);tone(1150,0.18,'sine',0.028,700);}
  sub.vy+=(holding?1900:-1900)*dt;sub.vy-=sub.vy*4.4*dt;sub.vy=clamp(sub.vy,-430,430);
  sub.y+=sub.vy*dt;
  var top=surfY(sub.x)+15;
  if(sub.y<top){sub.y=top;if(sub.vy<0)sub.vy=40;}
  if(sub.y+13>=floorY(sub.x))die('You crashed into the seabed');
  sub.tilt+=((sub.vy/640)-sub.tilt)*Math.min(1,dt*8);
  tmMine-=dt;if(tmMine<=0){tmMine=Math.max(0.7,rnd(1.1,2.0)-dist*0.000012);spawnMine();}
  tmTorp-=dt;if(tmTorp<=0){tmTorp=Math.max(2.2,rnd(4,6.5)-dist*0.00006);spawnTorp();}
  tmFish-=dt;if(tmFish<=0){tmFish=rnd(0.8,2.5);spawnFish();}
  tmWeed-=dt;if(tmWeed<=0){tmWeed=rnd(0.4,1.1);spawnWeed();}
  if(Math.random()<dt*16)parts.push({k:'bub',x:sub.x-48,y:sub.y+rnd(-3,5),vx:rnd(-60,-20)-speed*0.3,vy:rnd(-70,-20),r:rnd(1,3),life:rnd(0.6,1.4)});
  moveEnts(dt);updParts(dt);ambient(dt);
}

// ---- drawing ----
function text(s,x,y,size,col,align){ctx.font='bold '+size+'px "Segoe UI",system-ui,Arial,sans-serif';ctx.textAlign=align||'center';ctx.textBaseline='middle';ctx.fillStyle='rgba(0,10,20,0.55)';ctx.fillText(s,x+2,y+2);ctx.fillStyle=col;ctx.fillText(s,x,y);}
function rays(){ctx.save();ctx.globalAlpha=0.05;ctx.fillStyle='#bfeaff';for(var i=0;i<4;i++){var bx=((i*337+T*14)%(W+300))-150;ctx.beginPath();ctx.moveTo(bx,0);ctx.lineTo(bx+70,0);ctx.lineTo(bx+190,H);ctx.lineTo(bx+90,H);ctx.closePath();ctx.fill();}ctx.restore();}
function hills(par,col,off){ctx.fillStyle=col;ctx.beginPath();ctx.moveTo(0,H);for(var x=0;x<=W;x+=30){var wx=x+camX*par;var y=H-90-40*Math.sin(wx*0.004+off)-24*Math.sin(wx*0.011+off*2);ctx.lineTo(x,y);}ctx.lineTo(W,H);ctx.closePath();ctx.fill();}
function drawFloor(){var g=ctx.createLinearGradient(0,H-90,0,H);g.addColorStop(0,'#8a6b45');g.addColorStop(1,'#4a3520');ctx.fillStyle=g;ctx.beginPath();ctx.moveTo(0,H);for(var x=0;x<=W;x+=20)ctx.lineTo(x,floorY(x));ctx.lineTo(W,H);ctx.closePath();ctx.fill();ctx.strokeStyle='rgba(0,0,0,0.3)';ctx.lineWidth=2;ctx.stroke();}
function drawWeed(w){var fy=floorY(w.x);
  if(w.type==='rock'){ctx.fillStyle='#3d4652';ctx.beginPath();ctx.ellipse(w.x,fy+2,w.w/2,w.h,0,Math.PI,Math.PI*2);ctx.fill();ctx.fillStyle='rgba(255,255,255,0.07)';ctx.beginPath();ctx.ellipse(w.x-w.w*0.15,fy-w.h*0.3,w.w*0.22,w.h*0.4,0,Math.PI,Math.PI*2);ctx.fill();return;}
  ctx.strokeStyle=w.col;ctx.lineWidth=4;ctx.lineCap='round';
  for(var b=0;b<w.blades;b++){var bx=w.x+b*8-10,sw=Math.sin(T*1.6+w.ph+b)*7;ctx.beginPath();ctx.moveTo(bx,fy+4);ctx.quadraticCurveTo(bx+sw*0.4,fy-w.h*0.55,bx+sw,fy-w.h);ctx.stroke();}}
function drawFish(f){ctx.save();ctx.translate(f.x,f.y);ctx.scale(f.s,f.s);var w=Math.sin(T*8+f.ph)*2;ctx.fillStyle='rgba(140,185,205,0.85)';ctx.beginPath();ctx.ellipse(0,0,10,4.5,0,0,Math.PI*2);ctx.fill();ctx.beginPath();ctx.moveTo(9,0);ctx.lineTo(15,-5+w);ctx.lineTo(15,5+w);ctx.closePath();ctx.fill();ctx.fillStyle='#0b2233';ctx.beginPath();ctx.arc(-6,-1,1.2,0,Math.PI*2);ctx.fill();ctx.restore();}
function drawMine(m){ctx.save();ctx.translate(m.x,m.y);ctx.strokeStyle='#39414d';ctx.lineWidth=3;
  for(var i=0;i<8;i++){var a=i/8*Math.PI*2+m.ph*0.1;ctx.beginPath();ctx.moveTo(Math.cos(a)*m.r*0.7,Math.sin(a)*m.r*0.7);ctx.lineTo(Math.cos(a)*(m.r+7),Math.sin(a)*(m.r+7));ctx.stroke();ctx.fillStyle='#39414d';ctx.beginPath();ctx.arc(Math.cos(a)*(m.r+8),Math.sin(a)*(m.r+8),2.5,0,Math.PI*2);ctx.fill();}
  var g=ctx.createRadialGradient(-m.r*0.3,-m.r*0.3,2,0,0,m.r);g.addColorStop(0,'#4d5866');g.addColorStop(1,'#1c222b');
  ctx.fillStyle=g;ctx.beginPath();ctx.arc(0,0,m.r,0,Math.PI*2);ctx.fill();
  if(Math.sin(T*5+m.ph)>0.4){ctx.fillStyle='#ff5252';ctx.beginPath();ctx.arc(0,-m.r*0.35,3,0,Math.PI*2);ctx.fill();}
  ctx.restore();}
function drawTorp(tp){ctx.save();ctx.translate(tp.x,tp.y);
  ctx.fillStyle='#9aa3ad';rr(-24,-5,44,10,5);ctx.fill();
  ctx.fillStyle='#c0392b';ctx.beginPath();ctx.moveTo(-24,-5);ctx.lineTo(-34,0);ctx.lineTo(-24,5);ctx.closePath();ctx.fill();
  ctx.fillStyle='#6d7680';ctx.fillRect(14,-8,5,16);
  ctx.restore();}
function drawSub(x,y,tilt){ctx.save();ctx.translate(x,y);ctx.rotate(tilt);
  ctx.fillStyle='#c88f1f';ctx.beginPath();ctx.moveTo(-26,0);ctx.lineTo(-44,-12);ctx.lineTo(-38,0);ctx.lineTo(-44,12);ctx.closePath();ctx.fill();
  var g=ctx.createLinearGradient(0,-14,0,14);g.addColorStop(0,'#f7c948');g.addColorStop(0.55,'#e8a51f');g.addColorStop(1,'#b87714');
  ctx.fillStyle=g;ctx.beginPath();ctx.ellipse(0,0,34,13,0,0,Math.PI*2);ctx.fill();
  ctx.strokeStyle='rgba(60,30,0,0.5)';ctx.lineWidth=1.5;ctx.stroke();
  ctx.fillStyle='#e8a51f';rr(-8,-25,18,13,5);ctx.fill();ctx.strokeStyle='rgba(60,30,0,0.5)';ctx.stroke();
  ctx.strokeStyle='#7a4d0c';ctx.lineWidth=3;ctx.beginPath();ctx.moveTo(2,-24);ctx.lineTo(2,-33);ctx.lineTo(9,-33);ctx.stroke();
  ctx.fillStyle='#9fd4e8';ctx.strokeStyle='#5b3a08';ctx.lineWidth=1.5;
  for(var i=-1;i<=1;i++){ctx.beginPath();ctx.arc(i*14+2,0,4.5,0,Math.PI*2);ctx.fill();ctx.stroke();}
  ctx.fillStyle='rgba(255,255,255,0.25)';ctx.beginPath();ctx.ellipse(16,-4,10,5,-0.3,0,Math.PI*2);ctx.fill();
  var pb=8*Math.cos(T*25);ctx.strokeStyle='#d8d8d8';ctx.lineWidth=3;ctx.beginPath();ctx.moveTo(-47,-pb);ctx.lineTo(-47,pb);ctx.stroke();
  ctx.restore();}
function drawParts(){for(var i=0;i<parts.length;i++){var p=parts[i],a=clamp(p.life,0,1);
  if(p.k==='bub'){ctx.strokeStyle='rgba(190,230,250,'+(0.6*a)+')';ctx.lineWidth=1;ctx.beginPath();ctx.arc(p.x,p.y,p.r,0,Math.PI*2);ctx.stroke();}
  else if(p.k==='mote'){ctx.fillStyle='rgba(180,220,240,'+(0.22*a)+')';ctx.beginPath();ctx.arc(p.x,p.y,p.r,0,Math.PI*2);ctx.fill();}
  else{ctx.fillStyle='rgba('+(p.life>0.35?'255,180,40':'255,80,20')+','+a+')';ctx.beginPath();ctx.arc(p.x,p.y,p.r*(0.5+a),0,Math.PI*2);ctx.fill();}}}
function hud(){if(state==='title')return;
  text(meters()+' m',W/2,34,34,'#e8f6ff');
  text('REKORD '+best+' m',W-16,24,16,'rgba(220,240,255,0.75)','right');
  text(muted?'M · muted':'M · sound',W-16,46,12,'rgba(220,240,255,0.45)','right');
  if(state==='play'&&dist<600)text('hold to dive · release to rise',W/2,H-24,15,'rgba(220,240,255,0.6)');}
function title(){ctx.fillStyle='rgba(2,14,26,0.45)';ctx.fillRect(0,0,W,H);
  text('SUBMARINE',W/2,H*0.3,Math.min(64,Math.max(28,W/11)),'#ffd257');
  text('a deep-sea dodge',W/2,H*0.3+Math.min(64,Math.max(28,W/11))*0.75,18,'#9fd0e8');
  text('HOLD mouse · SPACE · touch — DIVE',W/2,H*0.52,17,'#e8f6ff');
  text('release to rise',W/2,H*0.52+26,17,'#e8f6ff');
  text('dodge the mines and torpedoes',W/2,H*0.52+52,15,'#9fd0e8');
  if(Math.sin(T*4)>-0.3)text('click or press SPACE to launch',W/2,H*0.68,20,'#ffd257');
  if(best>0)text('best '+best+' m',W/2,H*0.68+34,14,'rgba(230,245,255,0.6)');}
function over(){if(deadT<0.25)return;
  ctx.fillStyle='rgba(10,4,4,0.45)';ctx.fillRect(0,0,W,H);
  text('SUNK!',W/2,H*0.34,52,'#ff6b5e');
  text(deathCause,W/2,H*0.34+44,17,'#ffc9c2');
  var m=meters();text('distance  '+m+' m',W/2,H*0.5,24,'#e8f6ff');
  var nb=m>=best&&m>0;text(nb?'YANGI REKORD!':'best '+best+' m',W/2,H*0.5+32,16,nb?'#ffd257':'rgba(220,240,255,0.7)');
  if(deadT>0.7&&Math.sin(T*4)>-0.3)text('SPACE / click / R — dive again',W/2,H*0.64,18,'#e8f6ff');}

function draw(){
  var g=ctx.createLinearGradient(0,0,0,H);g.addColorStop(0,'#0a4d6e');g.addColorStop(0.5,'#063a5a');g.addColorStop(1,'#02182e');
  ctx.fillStyle=g;ctx.fillRect(0,0,W,H);
  rays();hills(0.3,'rgba(4,30,52,0.7)',90);hills(0.55,'rgba(3,24,42,0.8)',40);
  ctx.fillStyle='rgba(160,220,255,0.16)';ctx.beginPath();ctx.moveTo(0,0);for(var x=0;x<=W;x+=24)ctx.lineTo(x,surfY(x));ctx.lineTo(W,0);ctx.closePath();ctx.fill();
  ctx.strokeStyle='rgba(200,235,255,0.4)';ctx.lineWidth=2;ctx.beginPath();
  for(x=0;x<=W;x+=16){var y=surfY(x);if(x===0)ctx.moveTo(x,y);else ctx.lineTo(x,y);}
  ctx.stroke();
  var i;
  for(i=0;i<fish.length;i++)drawFish(fish[i]);
  for(i=0;i<mines.length;i++)drawMine(mines[i]);
  for(i=0;i<torps.length;i++)drawTorp(torps[i]);
  drawFloor();
  for(i=0;i<weed.length;i++)drawWeed(weed[i]);
  drawSub(sub.x,sub.y,sub.tilt);
  drawParts();
  hud();
  if(state==='title')title();
  if(state==='dead')over();
}

// ---- input ----
function press(){audio();
  if(state==='title'){reset();state='play';holding=true;}
  else if(state==='dead'){if(deadT>0.6){reset();state='play';holding=true;}}
  else holding=true;}
function release(){holding=false;}
window.addEventListener('keydown',function(e){if(e.repeat)return;
  if(e.code==='Space'||e.code==='ArrowDown'||e.code==='KeyS'||e.code==='Enter'){e.preventDefault();press();}
  if(e.code==='KeyR'&&state==='dead'&&deadT>0.4){reset();state='play';}
  if(e.code==='KeyM'){muted=!muted;}});
window.addEventListener('keyup',function(e){if(e.code==='Space'||e.code==='ArrowDown'||e.code==='KeyS'||e.code==='Enter')release();});
cv.addEventListener('pointerdown',function(e){e.preventDefault();press();});
window.addEventListener('pointerup',release);
window.addEventListener('pointercancel',release);
window.addEventListener('blur',release);
cv.addEventListener('contextmenu',function(e){e.preventDefault();});

var last=performance.now();
function frame(now){var dt=Math.min(0.033,(now-last)/1000);last=now;T+=dt;update(dt);draw();requestAnimationFrame(frame);}
requestAnimationFrame(frame);

  // Report native gameplay state to the optional mission engine.
  window.__arcadeMissionSource = Object.assign({"id":54,"label":"Dive distance (m)","goals":[65,120,220],"note":"Dive beneath mines and release to rise over the terrain."}, { read: () => { return (meters()); } });
