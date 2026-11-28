/* Revolved porcelain and its orbital plane share one depth buffer. */
(() => {
  'use strict';
  const host = document.getElementById('eclipse-piyola');
  const canvas = host?.querySelector('canvas');
  if (!canvas) return;
  const materialUrl = new URL('porcelain.webp', document.currentScript.src);
  const root = document.documentElement;
  const reducedMotion = matchMedia('(prefers-reduced-motion: reduce)');
  const compact = matchMedia('(max-width: 767px)');
  const TAU = 2 * Math.PI;

  const vertexSource = `
    attribute vec3 aPosition;
    attribute vec3 aNormal;
    attribute vec2 aUv;
    attribute float aMaterial;
    uniform vec2 uScale;
    uniform CLOCK_PRECISION vec2 uClock;
    varying vec2 uv;
    varying vec3 normal;
    varying vec3 point;
    varying float material;
    void main() {
      vec3 p = aPosition;
      vec3 n = aNormal;
      float sway = sin(uClock.x * .27) * .045;
      float lift = sin(uClock.x * .73) * .032;
      if (aMaterial < 3.5) {
        float angle = .03 + sway;
        mat2 rotate = mat2(cos(angle), -sin(angle), sin(angle), cos(angle));
        p.xz = rotate * p.xz;
        n.xz = rotate * n.xz;
        p.y += lift;
      } else if (aMaterial < 4.5) {
        // The entire inclined orbital plane precesses. Its light also travels
        // along the circumference so rotation reads even at shallow angles.
        float bank = .145 + sin(uClock.y * .22) * .044;
        p.y += p.x * bank + p.z * (.014 + .070*sin(uClock.y * .18));
      } else {
        float t = aUv.y;
        float strand = aNormal.x;
        p.y = .54 + t * (.74 - strand * .06) + lift;
        p.x = .16 + strand*.085 + sin(t*7.7 + uClock.x*.96 + strand*.9) * (.025 + .12*t)
                    + sin(t*3.1 - uClock.x*.61) * .11*t + aUv.x * (.021 + t*.061);
        p.z = .12 + strand*.09;
      }
      point = p;
      normal = n;
      uv = aUv;
      material = aMaterial;
      // Orthographic camera elevated 13 degrees. Front objects have larger z.
      float y = .9744*p.y - .2249*p.z;
      float z = .2249*p.y + .9744*p.z;
      gl_Position = vec4(p.x*uScale.x, (y-.14)*uScale.y, -z*.15, 1.);
    }`;

  const fragmentSource = `
    precision mediump float;
    uniform sampler2D uPattern;
    uniform CLOCK_PRECISION vec2 uClock;
    varying vec2 uv;
    varying vec3 normal;
    varying vec3 point;
    varying float material;
    const float TAU = 6.2831853;
    float streak(float x, float middle, float width) {
      return 1.-smoothstep(0.,width,abs(x-middle));
    }
    void main() {
      if (material > 4.5) {
        float veil = pow(max(0.,1.-abs(uv.x)),1.65) * smoothstep(0.,.1,uv.y) * (1.-smoothstep(.2,1.,uv.y));
        vec3 light = mix(vec3(1.,.53,.24),vec3(1.,.74,.94),uv.y);
        gl_FragColor = vec4(light, veil*.64);
        return;
      }
      if (material > 3.5) {
        float r=uv.y, a=uv.x*TAU-uClock.y*.56;
        float stream=.5+.5*cos(a*3.+r*9.);
        float outer=streak(r,.87,.046), gold=streak(r,.70,.048);
        float blue=streak(r,.44,.070), inner=streak(r,.14,.046);
        float glint=pow(max(0.,cos(a*2.-.4)),18.);
        float hair=pow(.5+.5*cos(r*181.+.5*sin(a*8.)),12.);
        // A chain of small diamond-shaped engravings on the broad ribbon.
        float diamonds=1.-smoothstep(.035,.14,abs(abs(fract(uv.x*62.-uClock.y*.23)-.5)*2.+abs((r-.55)*13.)-.85));
        float body=smoothstep(.38,.43,r)*(1.-smoothstep(.65,.71,r));
        vec3 light=mix(vec3(.55,.14,1.),vec3(1.,.26,.85),stream);
        light=mix(light,vec3(.30,.47,1.),blue*.75);
        light=mix(light,vec3(1.,.77,.45),max(gold,outer)*.95);
        light+=vec3(.82,.69,.48)*(glint*(outer+gold+inner)+diamonds*body*.9+hair*.15);
        float halo=.11*streak(r,.82,.20)+.09*streak(r,.15,.14);
        float alpha=halo+.04*(1.-r)+.39*body+.22*diamonds*body+.12*hair
          +.95*(outer+gold)+.74*(inner+blue)+.20*stream*body;
        alpha *= smoothstep(.0,.035,r)*(1.-smoothstep(.94,1.,r));
        gl_FragColor=vec4(light,min(.99,alpha));
        return;
      }
      vec3 n=normalize(normal);
      vec3 eye=normalize(vec3(0.,.225,.975));
      vec3 key=normalize(vec3(-.55,.76,.72));
      vec3 warm=normalize(vec3(.74,.52,.46));
      float edge=pow(1.-max(0.,dot(n,eye)),2.);
      float shine=pow(max(0.,dot(n,normalize(key+eye))),35.);
      float soft=pow(max(0.,dot(n,normalize(warm+eye))),7.);
      float facing=max(0.,dot(n,key));
      vec3 pigment;
      if (material < .5) {
        pigment=texture2D(uPattern,uv).rgb;
      } else if (material < 1.5) {
        pigment=vec3(1.,.64,.16)*(.64+.32*cos(point.y*110.));
        shine+=pow(max(0.,dot(n,normalize(warm+eye))),12.)*.75;
      } else if (material < 2.5) {
        pigment=vec3(.95,.81,.67);
      } else {
        float radius=length(point.xz);
        float ripple=.5+.5*cos(radius*44.+sin(point.x*7.+uClock.x*.8)*.55);
        float reflection=exp(-6.*abs(point.x-.32+.13*sin(point.z*5.)));
        vec3 amber=mix(vec3(.18,.036,.017),vec3(.70,.22,.035),.34+.35*point.z);
        gl_FragColor=vec4(amber+vec3(.67,.32,.14)*reflection*(.29+.17*ripple)
              +vec3(.65,.34,.14)*streak(radius,.97,.035),1.);
        return;
      }
      vec3 rim=mix(vec3(1.,.13,.57),vec3(.52,.36,1.),smoothstep(-.6,.8,n.x));
      vec3 cobalt=vec3(.18,.12,.54)*pow(max(0.,n.x),2.);
      vec3 ceramic=pigment*(.56+.48*facing+.27*soft)
              +rim*edge*.82+vec3(1.,.90,.74)*shine*.85+cobalt
              +vec3(.91,.13,.57)*max(0.,-n.x)*.22;
      gl_FragColor=vec4(ceramic,1.);
    }`;

  function makeGeometry() {
    const vertices = [], indices = [];
    const segments = 100;
    function band(profile, material, repeat = 1) {
      const start = vertices.length / 9;
      for (let row = 0; row < profile.length; row++) {
        const [r, y, v] = profile[row];
        const next = profile[Math.min(row + 1, profile.length - 1)];
        const prev = profile[Math.max(0, row - 1)];
        const radial = next[1] - prev[1], up = prev[0] - next[0];
        const length = Math.hypot(radial, up) || 1;
        for (let i = 0; i <= segments; i++) {
          const u = i / segments;
          const x = Math.sin(u * TAU), z = Math.cos(u * TAU);
          vertices.push(r*x, y, r*z, radial*x/length, up/length, radial*z/length,
            material === 0 ? u*repeat+.5 : u, v, material);
          if (row && i) {
            const end = start + row*(segments+1) + i;
            indices.push(end-segments-2, end-1, end, end-segments-2, end, end-segments-1);
          }
        }
      }
    }
    // A foot, a convex outer wall, a separately curved lip and recessed tea.
    band([[.29,-.49,0],[.375,-.48,.18],[.38,-.45,.35],[.34,-.42,.5],[.33,-.36,.78],[.38,-.335,1]],1);
    band([[.34,-.36,0],[.50,-.32,.07],[.64,-.25,.15],[.76,-.14,.28],[.87,0,.43],
      [.97,.17,.62],[1.045,.34,.80],[1.095,.49,.96],[1.107,.53,1]],0,4);
    band([[1.098,.50,0],[1.115,.515,.2],[1.118,.538,.45],[1.106,.548,.62],
      [1.08,.544,.80],[1.07,.531,1]],1);
    band([[1.07,.532,1],[1.055,.50,.75],[1.024,.457,.4],[.981,.427,0]],2);
    band([[.001,.442,0],[.49,.443,.5],[.97,.447,.93],[1.026,.459,1]],3);
    const potteryCount = indices.length;
    band([[1.37,.085,0],[2.57,.085,1]],4);
    const orbitCount = indices.length - potteryCount;
    for (let strand=0; strand<3; strand++) {
      const base = vertices.length/9;
      for (let j=0; j<=34; j++) {
        for (const side of [-1,1]) vertices.push(0,0,0,strand-1,0,0,side,j/34,5);
        if (j) {
          const end=base+j*2;
          indices.push(end-2,end-1,end+1,end-2,end+1,end);
        }
      }
    }
    return {vertices:new Float32Array(vertices),indices:new Uint16Array(indices),potteryCount,orbitCount};
  }

  function createRenderer() {
    const gl = canvas.getContext('webgl', {alpha:true, antialias:true, depth:true,
      premultipliedAlpha:true, powerPreference:'low-power', preserveDrawingBuffer:false});
    if (!gl) return null;
    const program = gl.createProgram();
    // Fragment highp is optional in WebGL 1. Use matching uniform precision
    // on both stages; longer sessions retain fine time steps on capable GPUs.
    const clockPrecision = gl.getShaderPrecisionFormat(gl.FRAGMENT_SHADER,gl.HIGH_FLOAT)?.precision
      ? 'highp' : 'mediump';
    for (const [type,source] of [[gl.VERTEX_SHADER,vertexSource],[gl.FRAGMENT_SHADER,fragmentSource]]) {
      const shader = gl.createShader(type);
      gl.shaderSource(shader,source.replace('CLOCK_PRECISION',clockPrecision));
      gl.compileShader(shader);
      gl.attachShader(program,shader);
      gl.deleteShader(shader);
    }
    gl.linkProgram(program);
    if (!gl.getProgramParameter(program,gl.LINK_STATUS)) {
      gl.deleteProgram(program);
      return null;
    }
    gl.useProgram(program);
    const mesh = makeGeometry();
    const vertex = gl.createBuffer(), index = gl.createBuffer();
    gl.bindBuffer(gl.ARRAY_BUFFER,vertex);
    gl.bufferData(gl.ARRAY_BUFFER,mesh.vertices,gl.STATIC_DRAW);
    gl.bindBuffer(gl.ELEMENT_ARRAY_BUFFER,index);
    gl.bufferData(gl.ELEMENT_ARRAY_BUFFER,mesh.indices,gl.STATIC_DRAW);
    for (const [name,size,offset] of [['aPosition',3,0],['aNormal',3,3],['aUv',2,6],['aMaterial',1,8]]) {
      const loc = gl.getAttribLocation(program,name);
      gl.enableVertexAttribArray(loc);
      gl.vertexAttribPointer(loc,size,gl.FLOAT,false,36,offset*4);
    }
    const pattern = gl.createTexture();
    gl.bindTexture(gl.TEXTURE_2D,pattern);
    gl.texParameteri(gl.TEXTURE_2D,gl.TEXTURE_WRAP_S,gl.REPEAT);
    gl.texParameteri(gl.TEXTURE_2D,gl.TEXTURE_WRAP_T,gl.CLAMP_TO_EDGE);
    gl.texParameteri(gl.TEXTURE_2D,gl.TEXTURE_MIN_FILTER,gl.LINEAR_MIPMAP_LINEAR);
    gl.texParameteri(gl.TEXTURE_2D,gl.TEXTURE_MAG_FILTER,gl.LINEAR);
    const image = new Image();
    let ready = false;
    image.onload = () => {
      if (gl.isContextLost()) return;
      gl.bindTexture(gl.TEXTURE_2D,pattern);
      gl.pixelStorei(gl.UNPACK_FLIP_Y_WEBGL,true);
      gl.texImage2D(gl.TEXTURE_2D,0,gl.RGB,gl.RGB,gl.UNSIGNED_BYTE,image);
      gl.generateMipmap(gl.TEXTURE_2D);
      ready=true;
      refresh();
    };
    image.src=materialUrl.href;
    const scale=gl.getUniformLocation(program,'uScale'), clock=gl.getUniformLocation(program,'uClock');
    gl.enable(gl.DEPTH_TEST);
    gl.enable(gl.BLEND);
    gl.blendFuncSeparate(gl.SRC_ALPHA,gl.ONE_MINUS_SRC_ALPHA,gl.ONE,gl.ONE_MINUS_SRC_ALPHA);
    gl.clearColor(0,0,0,0);
    return (width,height,bowlClock,orbitClock,showBowl) => {
      if (!ready || gl.isContextLost()) return false;
      if (width !== canvas.width || height !== canvas.height) {
        canvas.width=width;
        canvas.height=height;
        gl.viewport(0,0,width,height);
      }
      const aspect=width/height;
      const unit=Math.min(1/1.13, aspect/2.64);
      gl.uniform2f(scale,unit/aspect,unit);
      gl.uniform2f(clock,bowlClock,orbitClock);
      gl.depthMask(true);
      gl.clear(gl.COLOR_BUFFER_BIT|gl.DEPTH_BUFFER_BIT);
      if (showBowl) gl.drawElements(gl.TRIANGLES,mesh.potteryCount,gl.UNSIGNED_SHORT,0);
      gl.depthMask(false);
      gl.drawElements(gl.TRIANGLES,mesh.orbitCount,gl.UNSIGNED_SHORT,mesh.potteryCount*2);
      if (showBowl) {
        const start=mesh.potteryCount+mesh.orbitCount;
        gl.drawElements(gl.TRIANGLES,mesh.indices.length-start,gl.UNSIGNED_SHORT,start*2);
      }
      host.dataset.ecRenderer='webgl';
      return true;
    };
  }

  let renderer, attempted=false, inView=false, frame=0, timer=0;
  let bowlClock=0, orbitClock=0, previous=0, overBudget=0, lowQuality=false;
  let width=0,height=0;
  function stop() {
    cancelAnimationFrame(frame);
    clearTimeout(timer);
    frame=timer=previous=0;
  }
  function eligible() {
    return inView && !document.hidden && root.dataset.theme === 'eclipse'
      && root.dataset.eclipseScene !== 'off' && width>1 && height>1;
  }
  function paint(time) {
    if (!eligible() || !renderer) return;
    const motion=!reducedMotion.matches && root.dataset.eclipseMotion !== 'off';
    const showBowl=root.dataset.eclipseBowl !== 'off';
    const spin=root.dataset.eclipseOrbit !== 'off';
    const elapsed=previous ? (time-previous)/1000 : 0;
    if (elapsed>.075 && ++overBudget>=6) lowQuality=true;
    previous=time;
    if (motion && showBowl) bowlClock+=Math.min(elapsed,.1);
    if (motion && spin) orbitClock+=Math.min(elapsed,.1);
    const quality=Math.min(window.devicePixelRatio || 1,compact.matches || lowQuality ? 1 : 1.5, 1024/width);
    const ready=renderer(Math.max(1,Math.round(width*quality)),Math.max(1,Math.round(height*quality)),
      bowlClock,orbitClock,showBowl);
    if (ready && motion && (showBowl || spin)) {
      host.dataset.ecState='running';
      // One decorative draw every ~33ms leaves rendering time for card drag.
      timer=setTimeout(() => { frame=requestAnimationFrame(paint); },lowQuality ? 30 : 19);
    } else {
      host.dataset.ecState='still';
    }
  }
  function refresh() {
    stop();
    if (!eligible()) {
      host.dataset.ecState='paused';
      return;
    }
    if (!attempted) {
      attempted=true;
      try { renderer=createRenderer(); } catch { renderer=null; }
    }
    if (renderer) frame=requestAnimationFrame(paint);
    else host.dataset.ecState='still';
  }
  host.dataset.ecRenderer='static';
  canvas.addEventListener('webglcontextlost',event => {
    event.preventDefault();
    stop();
    renderer=null;
    host.dataset.ecRenderer='static';
    host.dataset.ecState='still';
  });
  canvas.addEventListener('webglcontextrestored',() => {attempted=false; refresh();});
  new MutationObserver(refresh).observe(root,{attributes:true,attributeFilter:
    ['data-theme','data-eclipse-scene','data-eclipse-bowl','data-eclipse-orbit','data-eclipse-motion']});
  document.addEventListener('visibilitychange',refresh);
  reducedMotion.addEventListener('change',refresh);
  compact.addEventListener('change',refresh);
  if (!window.ResizeObserver || !window.IntersectionObserver) return;
  new ResizeObserver(([entry]) => {
    width=entry.contentRect.width; height=entry.contentRect.height;
    refresh();
  }).observe(host);
  new IntersectionObserver(([entry]) => {inView=entry.isIntersecting; refresh();}).observe(host);
})();
