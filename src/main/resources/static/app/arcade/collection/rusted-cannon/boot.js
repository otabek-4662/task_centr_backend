(function(){
  function paint(msg){
    try{
      var c=document.getElementById('game'); if(!c) return false;
      // force intrinsic size
      if(!c.width) c.width=384; if(!c.height) c.height=224;
      var wrap=document.getElementById('wrap');
      if(wrap){
        var vw=Math.max(1, (window.innerWidth||1200)-24), vh=Math.max(1, (window.innerHeight||700)-48);
        var fit=Math.min(vw/c.width, vh/c.height);
        var sc=fit>=1?Math.max(1,Math.floor(fit)):fit;
        if(!(sc>0)||!isFinite(sc)) sc=1;
        wrap.style.width=Math.floor(c.width*sc)+'px';
        wrap.style.height=Math.floor(c.height*sc)+'px';
      }
      var ctx=c.getContext('2d'); if(!ctx) return false;
      ctx.setTransform(1,0,0,1,0,0);
      ctx.imageSmoothingEnabled=false;
      ctx.fillStyle='#101820'; ctx.fillRect(0,0,c.width,c.height);
      ctx.fillStyle='#f0c14a'; ctx.font='bold 22px monospace';
      ctx.fillText('RUSTED CANNON', 102, 88);
      ctx.fillStyle='#e8eef7'; ctx.font='12px monospace';
      ctx.fillText(msg||'loading assets...', 120, 120);
      ctx.fillStyle='#7dd3fc'; ctx.font='11px monospace';
      ctx.fillText('PRESS ENTER / Z', 130, 190);
      var d=document.getElementById('dbg'); if(d) if(d && d.style.display !== 'none') d.textContent=msg||'failsafe';
      var b=document.getElementById('modebadge'); if(b && b.style.display !== 'none') b.textContent='LOAD';
      return true;
    }catch(e){
      try{var d=document.getElementById('dbg'); if(d) d.textContent='failsafe-err '+e;}catch(_e){}
      return false;
    }
  }
  function tick(){ if(!window.__gameBooted) paint(window.__bootMsg||'loading...'); }
  window.__bootMsg='loading...';
  if(document.readyState==='loading') document.addEventListener('DOMContentLoaded', tick);
  tick();
  setTimeout(tick,0); setTimeout(tick,50); setTimeout(tick,200); setTimeout(tick,800);
})();
