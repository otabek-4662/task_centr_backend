import{a as A,b as Q,c as ao,d as We,e as D,f as io,g as je,h as vt,i as T,j as Le,k as fe,l as x,m as lo,n as j,o as co,p as $e,q as le,r as yt,s as Ce}from"./chunk-LCPEU4CA.mjs";import{a as no,b as so}from"./chunk-YEKQJ4YC.mjs";var Bo=no((Ct,Ro)=>{(function(n,t){typeof Ct=="object"?Ro.exports=t():typeof define=="function"&&define.amd?define(t):n.Alea=t()})(Ct,function(){"use strict";return n.importState=function(o){var e=new n;return e.importState(o),e},n;function n(){return(function(o){var e=0,r=0,i=0,s=1;o.length==0&&(o=[+new Date]);var a=t();e=a(" "),r=a(" "),i=a(" ");for(var l=0;l<o.length;l++)e-=a(o[l]),e<0&&(e+=1),r-=a(o[l]),r<0&&(r+=1),i-=a(o[l]),i<0&&(i+=1);a=null;var d=function(){var g=2091639*e+s*23283064365386963e-26;return e=r,r=i,i=g-(s=g|0)};return d.next=d,d.uint32=function(){return d()*4294967296},d.fract53=function(){return d()+(d()*2097152|0)*11102230246251565e-32},d.version="Alea 0.9",d.args=o,d.exportState=function(){return[e,r,i,s]},d.importState=function(g){e=+g[0]||0,r=+g[1]||0,i=+g[2]||0,s=+g[3]||0},d})(Array.prototype.slice.call(arguments))}function t(){var o=4022871197,e=function(r){r=r.toString();for(var i=0;i<r.length;i++){o+=r.charCodeAt(i);var s=.02519603282416938*o;o=s>>>0,s-=o,s*=o,o=s>>>0,s-=o,o+=s*4294967296}return(o>>>0)*23283064365386963e-26};return e.version="Mash 0.9",e}})});var zo=no((Ko,et)=>{(function(n,t){var o=n.KonamiCode,e=n.KonamiCode=t;e.noConflict=function(){return n.KonamiCode=o,e},typeof define=="function"&&define.amd&&define(function(){return t}),typeof et=="object"&&et.exports&&(et.exports=t)})(Ko||globalThis,function n(t){var o=this,e={},r=n;r.getNumberOfInstance=function(){return r._numberOfInstance},o.enable=function(){return e.listenCodeCharSequence(),e.listenCodeGestureSequence(),e.debug&&e.debug("Listener enabled for all."),o},o.enableKeyboardKeys=function(){return e.listenCodeCharSequence(),e.debug&&e.debug("Listener enabled for Keyboard Keys."),o},o.enableTouchGesture=function(){return e.listenCodeGestureSequence(),e.debug&&e.debug("Listener enabled for Touch Gesture."),o},o.disable=function(){return e.stopCodeCharSequence(),e.stopCodeGestureSequence(),e.debug&&e.debug("Listener disabled for all."),o},o.disableKeyboardKeys=function(){return e.stopCodeCharSequence(),e.debug&&e.debug("Listener disabled for Keyboard Keys."),o},o.disableTouchGesture=function(){return e.stopCodeGestureSequence(),e.debug&&e.debug("Listener disabled for Touch Gesture."),o},o.setListener=function(i){return e.stopCodeCharSequence(),e.stopCodeGestureSequence(),e.listener=i||document,e.listenCodeCharSequence(),e.listenCodeGestureSequence(),e.debug&&e.debug("Listener changed.",i),o},o.setCallback=function(i){return e.afterCodeSequenceCallback=typeof i=="function"&&i||e.defaultCallback,e.debug&&e.debug("Callback changed.",i),o},o.setOptions=function(i){return e.initOptions(i),o},e.keptLastCodeChar=function(){e.input.length>e.konamiCodeChar.length&&(e.input=e.input.substr(e.input.length-e.konamiCodeChar.length))},e.defaultCallback=function(){e.debug&&e.debug("Konami Code Sequence Entered. There is no action defined.")},e.checkIfCodeCharIsValid=function(){e.input===e.konamiCodeChar&&e.afterCodeSequenceCallback(o)},e.codeSequenceEventKeyDown=function(i){e.input+=i.keyCode,e.keptLastCodeChar(),e.checkIfCodeCharIsValid()},e.codeSequenceEventTouchMove=function(i){var s;i.touches.length===1&&e.capture===!0&&(s=i.touches[0],e.stopX=s.pageX,e.stopY=s.pageY,e.tap=!1,e.capture=!1,e.checkIfCodeGestureIsValid())},e.codeSequenceEventTouchEnd=function(){e.tap===!0&&e.checkIfCodeGestureIsValid()},e.codeSequenceEventTouchStart=function(i){e.startX=i.changedTouches[0].pageX,e.startY=i.changedTouches[0].pageY,e.tap=!0,e.capture=!0},e.stopCodeCharSequence=function(){e.listener.removeEventListener("keydown",e.codeSequenceEventKeyDown)},e.stopCodeGestureSequence=function(){e.listener.removeEventListener("touchstart",e.codeSequenceEventTouchStart),e.listener.removeEventListener("touchmove",e.codeSequenceEventTouchMove),e.listener.removeEventListener("touchend",e.codeSequenceEventTouchEnd)},e.listenCodeCharSequence=function(){e.stopCodeCharSequence(),e.listener.addEventListener("keydown",e.codeSequenceEventKeyDown)},e.listenCodeGestureSequence=function(){e.originalCodeGesture=e.konamiCodeGesture,e.stopCodeGestureSequence(),e.listener.addEventListener("touchstart",e.codeSequenceEventTouchStart),e.listener.addEventListener("touchmove",e.codeSequenceEventTouchMove),e.listener.addEventListener("touchend",e.codeSequenceEventTouchEnd,!1)},e.checkIfCodeGestureIsValid=function(){var i=Math.abs(e.startX-e.stopX),s=Math.abs(e.startY-e.stopY),a=e.startX-e.stopX<0?"rt":"lt",l=e.startY-e.stopY<0?"dn":"up",d=i>s?a:l;d=e.tap===!0?"tp":d,d===e.konamiCodeGesture.substr(0,2)?e.konamiCodeGesture=e.konamiCodeGesture.substr(2,e.konamiCodeGesture.length-2):e.konamiCodeGesture=e.originalCodeGesture,e.konamiCodeGesture.length===0&&(e.konamiCodeGesture=e.originalCodeGesture,e.afterCodeSequenceCallback(o))},e.checkDebugMode=function(i){i&&i.debug===!0?(e.debug=function(s,a){a!==void 0?console.log(s,a):console.log(s)},e.debug&&e.debug("Debug Mode On.")):e.debug=!1},e.initOptions=function(i){e.checkDebugMode(i),e.listener=i&&i.listener||document,e.afterCodeSequenceCallback=typeof i=="function"&&i||i&&typeof i.callback=="function"&&i.callback||e.defaultCallback},e.init=function(){e.input="",e.konamiCodeChar="38384040373937396665",e.konamiCodeGesture="upupdndnltrtltrttptp",e.startX=0,e.startY=0,e.stopX=0,e.stopY=0,e.tap=!1,e.capture=!1,r._numberOfInstance=r._numberOfInstance?r._numberOfInstance+1:1,e.initOptions(t),e.listenCodeCharSequence(),e.listenCodeGestureSequence()},e.init()})});var go="sprite-garden.v1";var mo=globalThis.arcadeStorage,mn=new Map(JSON.parse(mo?.getItem(go)??"[]"));function uo(n){let t=JSON.stringify([...n]);if(t.length>512e3)throw new Error("Saqlash joyi to\u2018ldi. Eski saqlangan dunyolarni o\u2018chiring.");return mo?.setItem(go,t),n}var be=mn,I={async getItem(n){return be.get(n)??null},async setItem(n,t){return be=uo(new Map(be).set(n,t)),t},async removeItem(n){let t=new Map(be);t.delete(n),be=uo(t)},async keys(){return[...be.keys()]}};function ce(n){let t=n.spriteGarden.state,o=n.spriteGarden.config,e=t.plantStructures.get();return{config:{breakMode:o.breakMode.get(),canvasScale:o.canvasScale.get(),currentResolution:o.currentResolution.get(),fogMode:o.fogMode.get(),fogScale:o.fogScale.get(),FRICTION:o.FRICTION.get(),GRAVITY:o.GRAVITY.get(),isFogScaled:o.isFogScaled.get(),MAX_FALL_SPEED:o.MAX_FALL_SPEED.get(),SURFACE_LEVEL:o.SURFACE_LEVEL.get(),TILE_SIZE:o.TILE_SIZE.get(),version:o.version.get(),WORLD_HEIGHT:o.WORLD_HEIGHT.get(),WORLD_WIDTH:o.WORLD_WIDTH.get(),worldSeed:o.worldSeed.get()},state:{camera:t.camera.get(),exploredMap:t.exploredMap.get().toObject(),gameTime:t.gameTime.get(),growthTimers:t.growthTimers.get(),materialsInventory:t.materialsInventory.get(),plantStructures:Object.fromEntries(Object.entries(e).map(([r,i])=>{if(typeof i?.blocks=="object"){let s=Object.entries(i.blocks);return[r,{...i,blocks:Object.fromEntries(s.map(([a,l])=>[a,{...l,tile:l.tile.id}]))}]}return[r,{...i}]})),player:t.player.get(),seedInventory:t.seedInventory.get(),selectedMaterialType:t.selectedMaterialType.get(),selectedSeedType:t.selectedSeedType.get(),viewMode:t.viewMode.get(),world:t.world.get().toArray()}}}var ee={color:{"amber-500":"#f39c12","amber-800":"#8b4513","amber-900":"#654321","antique-white":"#fffacd",beige:"#f5f5dc","black-alpha-60":"#00000099","black-alpha-80":"#000000cc",black:"#000000","blue-400":"#4a90e2","blue-500":"#2196f3","blue-700":"#0066cc",bronze:"#b87333",brown:"#654321",burlywood:"#deb887",chocolate:"#d2691e",coral:"#ff7f50",crimson:"#dc143c","dark-brown":"#59392b","dark-clay":"#cd853f","dark-cyan":"#2f4f2f","dark-green":"#2e5930","dark-olive-green":"#556b2f","dark-orange":"#ff8c00","dark-red":"#8b0000","dark-sea-green-light":"#9bcd9b","dark-sea-green":"#4f7942","dark-slate-gray":"#2f4f4f","dark-tan":"#8b7355","deep-pink":"#ff1493","dim-gray":"#696969","emerald-600":"#45a049","emerald-700":"#27ae60","forest-green":"#228b22",gold:"#ffd700",goldenrod:"#daa520","granite-gray-alpha-30":"#6464644d","gray-100":"#eeeeee","gray-200":"#e3e3e3","gray-300":"#e0e0e0","gray-400":"#dddddd","gray-50":"#f0f0f0","gray-500":"#cccccc","gray-600":"#bbbbbb","gray-700":"#888888","gray-800":"#666666","gray-900":"#333333","gray-alpha-10":"#ffffff1a","gray-alpha-20":"#ffffff33","gray-alpha-30":"#ffffff4d","gray-granite-alpha-30":"#6464644d","green-500":"#4caf50","green-yellow-400":"#adff2f","hot-pink":"#ff69b4","indian-red":"#cd5c5c",jet:"#1c1c1c",khaki:"#ffec8b","lawn-green":"#7cfc00","light-coral-pink":"#f08080","light-coral":"#ffa54f","light-pink":"#ffb6d9","light-sky-blue":"#87ceeb","light-steel-blue":"#c5d1d3","lime-400":"#32cd32","medium-purple":"#8a7fc8","medium-sea-green":"#3cb371","medium-slate-blue":"#9370db","misty-rose":"#ffe4e1","neutral-950":"#555","olive-drab-700":"#6b8e23","orange-500":"#ff6b35","orange-red":"#ff4500","pale-goldenrod":"#eee8aa","pale-green-200":"#90ee90","pale-green-300":"#98fb98",pink:"#ff69b4",plum:"#dda0dd","powder-blue":"#b0e0e6","red-500":"#ff4444","royal-blue":"#4169e1","saddle-brown":"#8b4513","sandy-brown":"#f4a460","sea-green":"#2e8b57","sky-300":"#87ceeb","sky-50":"#e6f3ff","stone-100":"#f1f1f1","stone-50":"#fffafa",tan:"#b8a99a",tomato:"#ff6347",white:"#ffffff","wood-brown":"#362200","yellow-400":"#ffff00","yellow-green-400":"#9acd32"},ui:{"touch-btn-background":"var(--sg-color-black-alpha-60)","touch-btn-border":"var(--sg-color-gray-alpha-30)","touch-btn":"var(--sg-color-white)","ui-grid-corner-container":"var(--sg-color-white)","ui-grid-corner-heading-background":"var(--black-alpha-80)","ui-grid-corner-heading-border":"var(--sg-color-gray-alpha-10)","ui-grid-corner-heading":"var(--sg-color-white)","ui-grid-corner-sub-heading":"var(--sg-color-white)"},tile:{"agave-base":"var(--sg-color-lime-400)","agave-flower-stalk":"var(--sg-color-yellow-green-400)","agave-flower":"var(--sg-color-yellow-400)","agave-growing":"var(--sg-color-green-yellow-400)","agave-spike":"var(--sg-color-lawn-green)",agave:"var(--sg-color-lawn-green)",air:"var(--sg-color-sky-300)","bamboo-growing":"var(--sg-color-pale-green-300)","bamboo-joint":"var(--sg-color-olive-drab-700)","bamboo-leaves":"var(--sg-color-lime-400)","bamboo-stalk":"var(--sg-color-pale-green-200)",bamboo:"var(--sg-color-pale-green-200)",bedrock:"var(--sg-color-jet)","berry-bush-berries":"var(--sg-color-crimson)","berry-bush-branch":"var(--sg-color-saddle-brown)","berry-bush-growing":"var(--sg-color-indian-red)","berry-bush-leaves":"var(--sg-color-forest-green)","berry-bush":"var(--sg-color-crimson)","birch-bark":"var(--sg-color-white)","birch-branches":"var(--sg-color-dark-tan)","birch-catkins":"var(--sg-color-pale-goldenrod)","birch-growing":"var(--sg-color-antique-white)","birch-leaves":"var(--sg-color-pale-green-300)","birch-trunk":"var(--sg-color-beige)",birch:"var(--sg-color-beige)","cactus-body":"var(--sg-color-sea-green)","cactus-flower":"var(--sg-color-hot-pink)","cactus-growing":"var(--sg-color-forest-green)",cactus:"var(--sg-color-lime-400)","carrot-growing":"var(--sg-color-coral)","carrot-leaves":"var(--sg-color-forest-green)","carrot-root":"var(--sg-color-tomato)",carrot:"var(--sg-color-dark-orange)",clay:"var(--sg-color-dark-clay)",cloud:"var(--sg-color-light-steel-blue)",coal:"var(--sg-color-dark-slate-gray)","corn-ear":"var(--sg-color-pale-goldenrod)","corn-growing":"var(--sg-color-pale-goldenrod)","corn-leaves":"var(--sg-color-olive-drab-700)","corn-silk":"var(--sg-color-burlywood)","corn-stalk":"var(--sg-color-yellow-green-400)",corn:"var(--sg-color-pale-goldenrod)",dirt:"var(--sg-color-saddle-brown)","fern-frond":"var(--sg-color-medium-sea-green)","fern-growing":"var(--sg-color-pale-green-200)","fern-stem":"var(--sg-color-dark-olive-green)",fern:"var(--sg-color-medium-sea-green)",fog:"var(--sg-color-black)",gold:"var(--sg-color-gold)",grass:"var(--sg-color-pale-green-200)",ice:"var(--sg-color-powder-blue)",iron:"var(--sg-color-bronze)","kelp-blade":"var(--sg-color-sea-green)","kelp-bulb":"var(--sg-color-dark-sea-green)","kelp-growing":"var(--sg-color-medium-slate-blue)",kelp:"var(--sg-color-dark-cyan)",lava:"var(--sg-color-orange-red)","lavender-bush":"var(--sg-color-medium-purple)","lavender-flowers":"var(--sg-color-medium-slate-blue)","lavender-growing":"var(--sg-color-plum)","lavender-stem":"var(--sg-color-dark-olive-green)",lavender:"var(--sg-color-medium-slate-blue)","loading-pixel":"var(--sg-color-light-sky-blue)","lotus-bud":"var(--sg-color-light-pink)","lotus-flower":"var(--sg-color-pink)","lotus-growing":"var(--sg-color-misty-rose)","lotus-pad":"var(--sg-color-medium-sea-green)","lotus-stem":"var(--sg-color-sea-green)",lotus:"var(--sg-color-pink)",moss:"var(--sg-color-dark-olive-green)","mushroom-cap":"var(--sg-color-dark-red)","mushroom-growing":"var(--sg-color-indian-red)","mushroom-stem":"var(--sg-color-chocolate)",mushroom:"var(--sg-color-dark-red)","pine-cone":"var(--sg-color-dark-tan)","pine-needles":"var(--sg-color-dark-green)","pine-tree-growing":"var(--sg-color-dark-olive-green)","pine-tree":"var(--sg-color-dark-green)","pine-trunk":"var(--sg-color-saddle-brown)","player-body":"var(--sg-color-pink)","player-border":"var(--sg-color-black)","player-eyes":"var(--sg-color-black)",pumice:"var(--sg-color-tan)","pumpkin-fruit":"var(--sg-color-dark-orange)","pumpkin-growing":"var(--sg-color-light-coral)","pumpkin-leaves":"var(--sg-color-yellow-green-400)","pumpkin-stem":"var(--sg-color-saddle-brown)","pumpkin-vine":"var(--sg-color-olive-drab-700)",pumpkin:"var(--sg-color-dark-orange)","rose-bloom":"var(--sg-color-crimson)","rose-bud":"var(--sg-color-indian-red)","rose-growing":"var(--sg-color-light-coral-pink)","rose-leaves":"var(--sg-color-dark-cyan)","rose-stem":"var(--sg-color-brown)","rose-thorns":"var(--sg-color-saddle-brown)",rose:"var(--sg-color-crimson)",sand:"var(--sg-color-sandy-brown)",snow:"var(--sg-color-stone-50)",stone:"var(--sg-color-dim-gray)","sunflower-center":"var(--sg-color-saddle-brown)","sunflower-growing":"var(--sg-color-khaki)","sunflower-leaves":"var(--sg-color-forest-green)","sunflower-petals":"var(--sg-color-gold)","sunflower-stem":"var(--sg-color-dark-tan)",sunflower:"var(--sg-color-gold)","tree-growing":"var(--sg-color-yellow-green-400)","tree-leaves":"var(--sg-color-forest-green)","tree-trunk":"var(--sg-color-dark-brown)","tulip-bulb":"var(--sg-color-dark-tan)","tulip-growing":"var(--sg-color-light-pink)","tulip-leaves":"var(--sg-color-forest-green)","tulip-petals":"var(--sg-color-deep-pink)","tulip-stem":"var(--sg-color-olive-drab-700)",tulip:"var(--sg-color-deep-pink)",walnut:"var(--sg-color-brown)",water:"var(--sg-color-royal-blue)","wheat-grain":"var(--sg-color-goldenrod)","wheat-growing":"var(--sg-color-yellow-green-400)","wheat-stalk":"var(--sg-color-dark-tan)",wheat:"var(--sg-color-goldenrod)","willow-branches":"var(--sg-color-medium-slate-blue)","willow-leaves":"var(--sg-color-yellow-green-400)","willow-tree-growing":"var(--sg-color-dark-sea-green-light)","willow-tree":"var(--sg-color-medium-slate-blue)","willow-trunk":"var(--sg-color-dark-tan)",wood:"var(--sg-color-wood-brown)",xray:"var(--sg-color-granite-gray-alpha-30)"}};function Ue(n,t){let o;for(let e of t){o||(o={});let r=e.startsWith("--sg-color-")?e:`${e}-color`;o[r]=n.getPropertyValue(r)}return o}function X(n,t){let o=n.getComputedStyle(t.host);return{...Ue(o,Object.keys(ee.color).map(e=>`--sg-color-${e}`)),...Ue(o,Object.keys(ee.tile).map(e=>`--sg-tile-${e}`)),...Ue(o,Object.keys(ee.ui).map(e=>`--sg-ui-${e}`))}}function St(n,t){let o=new Uint8Array(t),e="";for(let r=0;r<o.byteLength;r++)e+=String.fromCharCode(o[r]);return n.btoa(e)}function pn(n,t){let o=n.atob(t),e=o.length,r=new Uint8Array(e);for(let i=0;i<e;i++)r[i]=o.charCodeAt(i);return r.buffer}function ae(n,t,o){let e=pn(n,t),r=new Uint8Array(e);return new Blob([r.buffer],{type:o})}async function hn(n,t,o,e){if(!n.catalog.has(e.of("Names")))return[];let r=n.catalog.lookup(e.of("Names"),o);if(!r.has(e.of("EmbeddedFiles")))return[];let i=r.lookup(e.of("EmbeddedFiles"),o);if(!i.has(e.of("Names")))return[];let s=i.lookup(e.of("Names"),t),a=[];for(let l=0,d=s.size();l<d;l+=2){let g=s.lookup(l),u=s.lookup(l+1,o);a.push({fileName:g,fileSpec:u})}return a}async function de(n){let t=await import("./es-VGINXDCM.mjs"),{PDFDocument:o,PDFName:e,PDFDict:r,PDFArray:i,PDFStream:s,decodePDFRawStream:a}=t,l;n instanceof File&&(l=await n.arrayBuffer()),n instanceof ArrayBuffer&&(l=n);let d=await o.load(l);return(await hn(d,i,r,e)).map(({fileName:u,fileSpec:c})=>{let m=c.lookup(e.of("EF"),r).lookup(e.of("F"),s);return{name:u.decodeText(),data:a(m).decode()}})}function fn(n){let t=-1;for(let o=0;o<n.length;o++){t^=n[o];for(let e=0;e<8;e++)t=t>>>1^(t&1?3988292384:0)}return(t^-1)>>>0}function bn(n,t){let o=new TextEncoder().encode(n),e=t.length,r=new Uint8Array(8+e+4),i=new DataView(r.buffer);i.setUint32(0,e),r.set(o,4),r.set(t,8);let s=new Uint8Array(4+e);s.set(o,0),s.set(t,4);let a=fn(s);return i.setUint32(8+e,a),r}function vn(n,t){let o=n.slice(0,8),e=[],r=8;for(;r<n.length;){let m=new DataView(n.buffer,n.byteOffset+r).getUint32(0),p=String.fromCharCode(n[r+4],n[r+5],n[r+6],n[r+7]),b=8+m+4,v=n.slice(r,r+b);if(e.push({type:p,data:v}),r+=b,p==="IEND")break}let s=new TextEncoder().encode("gamestate"+"\0"+t),a=bn("tEXt",s),l=[o];l.push(e[0].data);for(let c=1;c<e.length;c++)e[c].type==="IEND"&&l.push(a),l.push(e[c].data);let d=0;for(let c of l)d+=c.length;let g=new Uint8Array(d),u=0;for(let c of l)g.set(c,u),u+=c.length;return g}async function po(n,t,o=400){let e=JSON.stringify(t),r=document.createElement("canvas");r.width=o,r.height=o;let i=r.getContext("2d"),s=n.width,a=n.height,l=Math.min(s,a),d=(s-l)/2,g=(a-l)/2;i.drawImage(n,d,g,l,l,0,0,o,o);let u=await new Promise(b=>r.toBlob(b,"image/png"));if(!u)throw new Error("Failed to create PNG blob");let c=await u.arrayBuffer(),m=new Uint8Array(c),p=vn(m,e);return new Blob([p],{type:"image/png"})}async function ue(n){let t=await n.arrayBuffer(),o=new Uint8Array(t),e=8;for(;e<o.length;){let i=new DataView(o.buffer,o.byteOffset+e).getUint32(0),s=String.fromCharCode(o[e+4],o[e+5],o[e+6],o[e+7]),a=e+8,l=a+i;if(s==="tEXt"){let d=o.slice(a,l),g=new TextDecoder().decode(d),u=g.indexOf("\0");if(u!==-1){let c=g.slice(0,u),m=g.slice(u+1);if(c==="gamestate")return JSON.parse(m)}}if(e=l+4,s==="IEND")break}return null}async function ve(n,t,o){let e=o;if(e?.type==="pdf"){let l=ae(n,e.contents,"application/pdf"),[d]=await de(new File([l],"sprite-garden-game-card.png"));e=JSON.parse(await ue(new Blob([d.data])))}let r=n.spriteGarden.config,i=n.spriteGarden.state;for(let l in e.config)l!=="currentResolution"&&l!=="isFogScaled"&&r[l]?.set&&n.spriteGarden.setConfig(l,e.config[l]);let s=e.config.WORLD_HEIGHT,a=e.config.WORLD_WIDTH;for(let l in e.state){if(l==="seedInventory"){let d=e.state[l];for(let g in i.seedInventory.get())d[g]===void 0&&(d[g]=0);i.seedInventory.set(d);continue}if(l==="materialsInventory"){let d=e.state[l];for(let g in i.materialsInventory.get())d[g]===void 0&&(d[g]=0);i.materialsInventory.set(d);continue}if(l==="plantStructures"){let d=e.state.plantStructures,g=r.TILES,u=Object.fromEntries(Object.entries(d).map(([c,m])=>[c,{...m,blocks:Object.fromEntries(Object.entries(m.blocks).map(([p,b])=>{let v=g.AIR;if(typeof b.tile=="object")v=b.tile;else if(typeof b.tile=="number"){let h=io(g,b.tile);h&&(v=h)}return[p,{...b,tile:v}]}))}]));i.plantStructures.set(u);continue}if(l==="exploredMap"){let d=e.state.exploredMap,g=X(n,t),u;d&&Object.keys(d).length>0?u=Le.fromObject(d,a,s,g):u=new Le(a,s,g),i.exploredMap.set(u);continue}if(l==="world"){let d=e.state[l];if(d&&Array.isArray(d)&&d.length>0){console.log(`Converting world: ${a}x${s}`);let g=fe.fromArray(d,a,s),u=r.TILES,c=0;for(let m=0;m<a;m++)for(let p=0;p<s;p++){let b=g.getTile(m,p);b&&b!==u.AIR&&c++}console.log(`Converted world contains ${c} non-air tiles`),i.world.set(g),console.log("World converted successfully")}else console.error("Invalid world data in save state:",d);continue}i[l]?.set&&n.spriteGarden.setState(l,e.state[l])}console.log("Save state loaded successfully"),t.dispatchEvent(new CustomEvent("sprite-garden-reset"))}function Me(){let n=new Date,t=n.getFullYear(),o=(n.getMonth()+1).toString().padStart(2,"0"),e=n.getDate().toString().padStart(2,"0"),r=n.getHours().toString().padStart(2,"0"),i=n.getMinutes().toString().padStart(2,"0"),s=n.getSeconds().toString().padStart(2,"0");return`${t}-${o}-${e}_${r}-${i}-${s}`}async function Ie(n){let t=new TextEncoder().encode(n);if("CompressionStream"in window){let e=new Blob([t]).stream().pipeThrough(new CompressionStream("gzip"));return await new Response(e).blob()}}async function yn(n,t){let o=await Ie(n),e=await t.createWritable();await e.write(o),await e.close()}async function ho(n,t){let o=`Sprite-Garden-Game-Save-${Me()}.sgs`,e;if(n.showSaveFilePicker)e=await n.showSaveFilePicker({suggestedName:o}),await yn(t,e);else{let r=await Ie(t),i=URL.createObjectURL(r),s=n.document.createElement("a");s.href=i,s.download=o,n.document.body.append(s),s.click(),n.document.body.removeChild(s),URL.revokeObjectURL(i)}}function ye(n,t){let o=(r,i)=>{if(!r)return null;if(r.tagName===i.toUpperCase()&&r.shadowRoot)return r;let s=[...r.children||[]];return r.shadowRoot&&s.push(r.shadowRoot),s.map(a=>o(a,i)).find(Boolean)||null},e=o(n,t);return e?e.shadowRoot:null}var Sn=1e3,En=60*Sn,wt=En,wn=wt/2,Ke="sprite-garden-autosave",Tt="sprite-garden-autosave-mode",fo="sprite-garden-save-";async function Se(){try{return await I.getItem(Tt)}catch(n){return console.info("Failed to get save mode:",n),"manual"}}async function kt(n){try{await I.setItem(Tt,n),console.info("Save mode set to:",n)}catch(t){console.info("Failed to set save mode:",t)}}var bo=0;async function Ee(n){try{if(await Se()!=="auto")return;let o=Date.now();if(o-bo<wn){console.info("Auto-save skipped (too soon since last save)");return}let e=ce(n),r=JSON.stringify(e),s=await(await Ie(r)).arrayBuffer(),a=St(n,s),l={name:"[Auto Save]",timestamp:Date.now(),data:a,isAutoSave:!0};await I.setItem(Ke,l),bo=o,console.info("Game auto-saved successfully")}catch(t){console.error("Failed to auto-save game:",t)}}async function vo(n,t){try{let o=await I.getItem(Ke),e=await I.getItem(Tt)==="auto";if(!o||!e)return!1;let r=n.document.createElement("dialog");r.style.cssText=`
      background: var(--sg-color-gray-50);
      border-radius: 0.5rem;
      border: 0.125rem solid var(--sg-color-gray-900);
      color: var(--sg-color-gray-900);
      font-family: monospace;
      padding: 1.25rem;
      max-width: 25rem;
      z-index: 10000;
    `;let i=new Date(o.timestamp).toLocaleString();return r.innerHTML=`
      <h3 style="margin: 0 0 1rem 0">Avtomatik saqlangan dunyo</h3>
      <p style="margin: 0 0 1rem 0">
        ${i} da saqlangan dunyoni ochasizmi?
      </p>
      <div style="display: flex; gap: 0.625rem; justify-content: flex-end">
        <button id="autoSaveNo" style="
          background: var(--sg-color-red-500);
          border-radius: 0.25rem;
          border: none;
          color: white;
          cursor: pointer;
          padding: 0.5rem 0.9375rem;
        ">Yo\u2018q</button>
        <button id="autoSaveYes" style="
          background: var(--sg-color-green-500);
          border-radius: 0.25rem;
          border: none;
          color: white;
          cursor: pointer;
          padding: 0.5rem 0.9375rem;
        ">Ha</button>
      </div>
    `,t.append(r),r.showModal(),new Promise(s=>{let a=r.querySelector("#autoSaveNo"),l=r.querySelector("#autoSaveYes");l.addEventListener("click",async()=>{a.setAttribute("disabled","disabled");try{let d=ae(n,o.data,"application/gzip"),g;if("DecompressionStream"in n){let b=d.stream().pipeThrough(new n.DecompressionStream("gzip"));g=await(await new n.Response(b).blob()).text()}else throw new Error("DecompressionStream not supported");let u=JSON.parse(g);await ve(n,t,u);let{worldSeed:c}=u.config,m=t.getElementById("worldSeedInput"),p=t.getElementById("currentSeed");m instanceof HTMLInputElement&&(m.value=c),p&&(p.textContent=c),console.log("Auto save loaded successfully")}catch(d){console.error("Failed to load auto-save:",d)}r.close(),r.remove(),s(!0)}),a.addEventListener("click",()=>{l.setAttribute("disabled","disabled"),r.close(),r.remove(),s(!1)}),r.addEventListener("cancel",()=>{s(!1)})})}catch(o){return console.error("Failed to check for auto-save:",o),!1}}var Et=class{constructor(t,o,e){this.gThis=t,this.doc=o,this.shadow=e,this.dialog=null,this.savedGames=[],this.close=this.close.bind(this),this.deleteSelectedGame=this.deleteSelectedGame.bind(this),this.getPDFGameStateAttachment=this.getPDFGameStateAttachment.bind(this),this.getSelectedGameAsPNG=this.getSelectedGameAsPNG.bind(this),this.handleDialogClick=this.handleDialogClick.bind(this),this.handleDragLeave=this.handleDragLeave.bind(this),this.handleDragOver=this.handleDragOver.bind(this),this.handleFileDrop=this.handleFileDrop.bind(this),this.handleFileSelect=this.handleFileSelect.bind(this),this.handleWorldNameInput=this.handleWorldNameInput.bind(this),this.loadSelectedGame=this.loadSelectedGame.bind(this),this.saveCurrentGame=this.saveCurrentGame.bind(this),this.shareSelectedGame=this.shareSelectedGame.bind(this),this.shareSelectedGameAsPDF=this.shareSelectedGameAsPDF.bind(this)}async createDialog(){this.dialog&&this.dialog.remove();let t=this.doc.createElement("dialog");return t.setAttribute("id","storageDialog"),t.style.cssText=`
      background: var(--sg-color-gray-50);
      border-radius: 0.5rem;
      border: 0.125rem solid var(--sg-color-gray-900);
      color: var(--sg-color-gray-900);
      font-family: monospace;
      max-height: 80vh;
      max-width: 31.25rem;
      overflow-y: auto;
      padding: 1.25rem;
      width: 90%;
    `,t.innerHTML=`
      <div
        style="
          align-items: center;
          display: flex;
          justify-content: space-between;
          margin-bottom: 0.9375rem;
        "
      >
        <h3 style="margin: 0">Saqlangan dunyolar</h3>
        <button
          id="closeStorageDialog"
          style="
            background: var(--sg-color-red-500);
            border-radius: 0.25rem;
            border: none;
            color: white;
            cursor: pointer;
            padding: 0.3125rem 0.625rem;
          "
        >
          &times;
        </button>
      </div>

      <div style="margin-bottom: 1.25rem">
        <h4 style="margin: 0.625rem 0">Joriy dunyoni saqlash</h4>
        <div
          style="
            align-items: center;
            display: flex;
            gap: 0.625rem;
            margin-bottom: 0.625rem;
          "
        >
          <input
            type="text"
            id="worldNameInput"
            placeholder="Dunyo nomi"
            style="
              border-radius: 0.25rem;
              border: 0.0625rem solid var(--sg-color-gray-500);
              flex: 1;
              padding: 0.3125rem;
            "
          />
          <button
            id="saveToStorageBtn"
            style="
              background: var(--sg-color-green-500);
              border-radius: 0.25rem;
              border: none;
              color: white;
              cursor: pointer;
              padding: 0.5rem 0.9375rem;
            "
          >
            Saqlash
          </button>
        </div>
      </div>

      <div>
        <h4 style="margin: 0.625rem 0">Dunyolar</h4>
        <div
          id="gameDropZone"
          style="
            border: 0.0625rem dashed var(--sg-color-gray-400);
            border-radius: 0.25rem;
            position: relative;
            transition: all 0.2s ease;
            padding: 0;
          "
        >
          <div
            id="savedGamesList"
            style="
              border: 0.0625rem solid var(--sg-color-gray-400);
              border-radius: 0.25rem;
              max-height: 18.75rem;
              overflow-y: auto;
            "
          >
            <!-- Saved games will be populated here -->
          </div>
          <input
            id="fileInput"
            type="file"
            accept=".sgs,.pdf,.txt,text/plain,application/pdf,application/gzip,application/*"
            style="display: none"
            multiple
          />
        </div>
        <div style="margin-top: 0.625rem; display: flex; gap: 0.625rem">
          <button
            id="deleteSelectedBtn"
            disabled
            style="
              background: var(--sg-color-red-500);
              border-radius: 0.25rem;
              border: none;
              color: white;
              cursor: pointer;
              padding: 0.5rem 0.9375rem;
            "
          >
            Tanlanganni o\u2018chirish
          </button>
          <button
            id="shareSelectedBtn"
            disabled
            hidden
            style="
              background: var(--sg-color-medium-purple);
              border-radius: 0.25rem;
              border: none;
              color: white;
              cursor: pointer;
              padding: 0.5rem 0.9375rem;
            "
          >
            Share Selected
          </button>
          <button
            id="shareSelectedAsPdfBtn"
            disabled
            hidden
            style="
              background: var(--sg-color-medium-purple);
              border-radius: 0.25rem;
              border: none;
              color: white;
              cursor: pointer;
              padding: 0.5rem 0.9375rem;
            "
          >
            Share Selected As PDF
          </button>
          <button
            id="loadSelectedBtn"
            disabled
            style="
              background: var(--sg-color-blue-500);
              border-radius: 0.25rem;
              border: none;
              color: white;
              cursor: pointer;
              padding: 0.5rem 0.9375rem;
            "
          >
            Tanlanganni ochish
          </button>
        </div>
      </div>
    `,this.shadow.append(t),this.dialog=t,await this.loadSavedGamesList(),this.initEventListeners(),this.updateButtonStates(),t}async loadSavedGamesList(){this.savedGames=[];let t=await I.keys(),o=await I.getItem(Ke);o&&this.savedGames.push({key:Ke,name:o.name,timestamp:o.timestamp,data:o.data,isAutoSave:!0});for(let e of t)if(e.startsWith(fo)){let r=await I.getItem(e);r&&this.savedGames.push({key:e,name:r.name,timestamp:r.timestamp,data:r.data,isAutoSave:r.isAutoSave||!1})}this.savedGames.sort((e,r)=>r.timestamp-e.timestamp),this.renderSavedGamesList()}renderSavedGamesList(){let t=this.dialog.querySelector("#savedGamesList");if(this.savedGames.length===0){t.innerHTML=`
      <div style="padding: 1.25rem; text-align: center; color: var(--sg-color-neutral-950);">
        Hali dunyo saqlanmagan
      </div>
    `;return}t.innerHTML=this.savedGames.map((o,e)=>`
        <div
          class="saved-game-item"
          data-index="${e}"
          style="
            padding: 0.625rem;
            border-bottom: 0.0625rem solid var(--sg-color-gray-100);
            cursor: pointer;
            display: flex;
            justify-content: space-between;
            align-items: center;
            ${o.isAutoSave?"background: var(--sg-color-blue-50);":""}
          "
        >
          <div>
            <div class="saved-game-name" style="font-weight: bold; ${o.isAutoSave?"color: var(--sg-color-blue-700);":""}"></div>
            <div style="font-size: 0.75rem; color: var(--sg-color-neutral-950);">
              ${new Date(o.timestamp).toLocaleString()}
            </div>
          </div>
          <input
            name="selectedGame"
            style="margin-left: 0.625rem"
            type="radio"
            value="${e}"
          />
        </div>
    `).join(""),t.querySelectorAll(".saved-game-item").forEach((o,e)=>{o.querySelector(".saved-game-name").textContent=this.savedGames[e].name,o.addEventListener("click",r=>{if(r.target instanceof HTMLElement&&r.target.getAttribute("type")!=="radio"){let i=o.querySelector('input[type="radio"]');i instanceof HTMLInputElement&&(i.checked=!0),this.updateButtonStates()}})}),t.querySelectorAll('input[type="radio"]').forEach(o=>{o.addEventListener("change",()=>this.updateButtonStates())})}updateButtonStates(){let o=!!this.dialog.querySelector('input[name="selectedGame"]:checked'),e=this.dialog.querySelector("#loadSelectedBtn");e instanceof HTMLButtonElement&&(e.disabled=!o,e.style.opacity=o?"1":"0.5",e.style.cursor=o?"pointer":"not-allowed");let r=this.dialog.querySelector("#deleteSelectedBtn");r instanceof HTMLButtonElement&&(r.disabled=!o,r.style.opacity=o?"1":"0.5",r.style.cursor=o?"pointer":"not-allowed");let i=this.dialog.querySelector("#shareSelectedBtn"),s=this.dialog.querySelector("#shareSelectedAsPdfBtn");if(i instanceof HTMLButtonElement&&s instanceof HTMLButtonElement){let a=typeof navigator<"u"&&typeof navigator.canShare<"u",l=!1;if(a)try{let d=new File([],"test");l=navigator.canShare({files:[d]}),i.disabled=!o,i.style.opacity=l?"1":"0.5",i.style.cursor=l?"pointer":"not-allowed",s.disabled=!o,s.style.opacity=l?"1":"0.5",s.style.cursor=l?"pointer":"not-allowed"}catch{i.disabled=!0,i.setAttribute("hidden","hidden"),i.style.opacity="0.5",i.style.cursor="not-allowed",s.disabled=!0,s.setAttribute("hidden","hidden"),s.style.opacity="0.5",s.style.cursor="not-allowed"}else i.disabled=!0,i.setAttribute("hidden","hidden"),i.style.opacity="0.5",i.style.cursor="not-allowed",s.disabled=!0,s.setAttribute("hidden","hidden"),s.style.opacity="0.5",s.style.cursor="not-allowed";l&&(i.removeAttribute("hidden"),$e.get()&&s.removeAttribute("hidden"))}}handleWorldNameInput(t){/^[\p{L}\p{N}\p{P}\s]+$/u.test(t.key)&&t.stopPropagation(),t.key==="Enter"&&this.saveCurrentGame()}handleDialogClick(t){t.target===this.dialog&&this.close()}initEventListeners(){let t=this.dialog.querySelector("#closeStorageDialog"),o=this.dialog.querySelector("#saveToStorageBtn"),e=this.dialog.querySelector("#loadSelectedBtn"),r=this.dialog.querySelector("#deleteSelectedBtn"),i=this.dialog.querySelector("#shareSelectedBtn"),s=this.dialog.querySelector("#shareSelectedAsPdfBtn"),a=this.dialog.querySelector("#worldNameInput"),l=this.dialog.querySelector("#gameDropZone"),d=this.dialog.querySelector("#fileInput");t.addEventListener("click",this.close),o.addEventListener("click",this.saveCurrentGame),e.addEventListener("click",this.loadSelectedGame),r.addEventListener("click",this.deleteSelectedGame),i.addEventListener("click",this.shareSelectedGame),s.addEventListener("click",this.shareSelectedGameAsPDF),a.addEventListener("keydown",this.handleWorldNameInput),l.addEventListener("dragover",this.handleDragOver),l.addEventListener("dragleave",this.handleDragLeave),l.addEventListener("drop",this.handleFileDrop),d.addEventListener("change",this.handleFileSelect),this.dialog.addEventListener("click",this.handleDialogClick)}removeEventListeners(){let t=this.dialog.querySelector("#closeStorageDialog"),o=this.dialog.querySelector("#saveToStorageBtn"),e=this.dialog.querySelector("#loadSelectedBtn"),r=this.dialog.querySelector("#deleteSelectedBtn"),i=this.dialog.querySelector("#shareSelectedBtn"),s=this.dialog.querySelector("#shareSelectedAsPdfBtn"),a=this.dialog.querySelector("#worldNameInput"),l=this.dialog.querySelector("#gameDropZone"),d=this.dialog.querySelector("#fileInput");t.removeEventListener("click",this.close),o.removeEventListener("click",this.saveCurrentGame),e.removeEventListener("click",this.loadSelectedGame),r.removeEventListener("click",this.deleteSelectedGame),i.removeEventListener("click",this.shareSelectedGame),s.removeEventListener("click",this.shareSelectedGameAsPDF),a.removeEventListener("keydown",this.handleWorldNameInput),l.removeEventListener("dragover",this.handleDragOver),l.removeEventListener("dragleave",this.handleDragLeave),l.removeEventListener("drop",this.handleFileDrop),d.removeEventListener("change",this.handleFileSelect),this.dialog.removeEventListener("click",this.handleDialogClick)}handleDragOver(t){t.preventDefault(),t.stopPropagation();let o=this.dialog.querySelector("#gameDropZone");o instanceof HTMLElement&&(o.style.borderColor="var(--sg-color-blue-500)",o.style.backgroundColor="rgba(100, 200, 255, 0.1)")}handleDragLeave(t){t.preventDefault(),t.stopPropagation();let o=this.dialog.querySelector("#gameDropZone");o instanceof HTMLElement&&(o.style.borderColor="var(--sg-color-gray-400)",o.style.backgroundColor="")}handleFileDrop(t){t.preventDefault(),t.stopPropagation();let o=this.dialog.querySelector("#gameDropZone");o instanceof HTMLElement&&(o.style.borderColor="var(--sg-color-gray-400)",o.style.backgroundColor="");let e=t.dataTransfer.files;this.processFiles(e)}handleFileSelect(t){let o=t.target.files;this.processFiles(o)}async processFiles(t){for(let o of t){if(!o.name.endsWith(".sgs")&&!o.name.endsWith(".txt")&&!o.name.endsWith(".pdf")){console.warn(`Skipping file ${o.name}: invalid extension`);continue}try{let e=await o.arrayBuffer();await this.loadGameFromFile(e,o.name)}catch(e){console.error(`Failed to load file ${o.name}:`,e),alert(`Failed to load file: ${o.name}. Check console for details.`)}}}async loadGameFromFile(t,o){try{let e;if(o.endsWith(".txt"))e=new TextDecoder().decode(t);else if(o.endsWith(".pdf")){let[d]=await de(t);e=await ue(new Blob([d.data]))}else{let d=new this.gThis.Blob([t],{type:"application/gzip"});if("DecompressionStream"in this.gThis){let g=d.stream().pipeThrough(new this.gThis.DecompressionStream("gzip"));e=await(await new this.gThis.Response(g).blob()).text()}else throw new Error("DecompressionStream not supported")}let r=JSON.parse(e);await ve(this.gThis,this.shadow,r);let{worldSeed:i}=r.config,s=this.doc.getElementById("worldSeedInput"),a=this.doc.getElementById("currentSeed");s instanceof HTMLInputElement&&(s.value=i),a&&(a.textContent=i),console.log(`Game loaded from file: ${o}`);let l=this.dialog.querySelector("#fileInput");l instanceof HTMLInputElement&&(l.value=""),this.close()}catch(e){throw console.error(`Failed to process game file ${o}:`,e),e}}async saveCurrentGame(){let t=this.dialog.querySelector("#worldNameInput"),o;if(t instanceof HTMLInputElement&&(o=t.value.trim()),!o){alert("Dunyo nomini kiriting.");return}try{let e=ce(this.gThis),r=JSON.stringify(e),s=await(await Ie(r)).arrayBuffer(),a=St(this.gThis,s),l={name:o,timestamp:Date.now(),data:a},d=`${fo}${Date.now()}-${o.replace(/[^a-zA-Z0-9]/g,"_")}`;await I.setItem(d,l),console.log("Game saved to storage:",o),t instanceof HTMLInputElement&&(t.value=""),await this.loadSavedGamesList()}catch(e){console.error("Failed to save game to storage:",e),alert("Dunyo saqlanmadi. Joy bo\u2018shatish uchun eski dunyolarni o\u2018chiring.")}}async loadSelectedGame(){let t=this.dialog.querySelector('input[name="selectedGame"]:checked');if(!t)return;let o;if(t instanceof HTMLInputElement){let e=parseInt(t.value);o=this.savedGames[e]}try{let e=ae(this.gThis,o.data,"application/gzip"),r;if("DecompressionStream"in this.gThis){let d=e.stream().pipeThrough(new this.gThis.DecompressionStream("gzip"));r=await(await new this.gThis.Response(d).blob()).text()}else throw new Error("DecompressionStream not supported");let i=JSON.parse(r);await ve(this.gThis,this.shadow,i);let{worldSeed:s}=i.config,a=this.doc.getElementById("worldSeedInput"),l=this.doc.getElementById("currentSeed");a instanceof HTMLInputElement&&(a.value=s),l&&(l.textContent=s),console.log("Game loaded from storage:",o.name),this.close()}catch(e){console.error("Failed to load game from storage:",e),alert("Failed to load game. Check console for details.")}}async deleteSelectedGame(){let t=this.dialog.querySelector('input[name="selectedGame"]:checked');if(!t)return;let o;if(t instanceof HTMLInputElement){let e=parseInt(t.value);o=this.savedGames[e]}if(confirm(`Are you sure you want to delete "${o.name}"?`))try{await I.removeItem(o.key),console.log("Game deleted from storage:",o.name),await this.loadSavedGamesList()}catch(e){console.error("Failed to delete game from storage:",e),alert("Failed to delete game. Check console for details.")}}async getPDFGameStateAttachment(){let t=Me(),o=await import("./es-VGINXDCM.mjs"),{PDFDocument:e,StandardFonts:r,rgb:i}=o,{game:s,pngSave:a}=await this.getSelectedGameAsPNG(),l=await a.arrayBuffer(),d;if("DecompressionStream"in this.gThis){let V=ae(this.gThis,s.data,"application/gzip").stream().pipeThrough(new this.gThis.DecompressionStream("gzip")),xe=await(await new this.gThis.Response(V).blob()).text();d=JSON.parse(xe)}let g=await e.create(),u=g.addPage([800,1100]),{width:c,height:m}=u.getSize(),p=await g.embedFont(r.HelveticaBold),b=await g.embedFont(r.Helvetica),v=i(.22,.55,.85),h=i(.68,.68,.68),f=i(.97,.97,.97),S=i(.6,.6,.6),E=i(.1,.1,.1),y=i(.2,.65,.35);u.drawRectangle({x:0,y:0,width:c,height:m,color:f});let k="Sprite Garden",w=38,L=p.widthOfTextAtSize(k,w),C=(c-L)/2,M=m-80;u.drawText(k,{x:C+2,y:M-2,size:w,font:p,color:h}),u.drawText(k,{x:C,y:M,size:w,font:p,color:y});let N=g.context.obj({Type:"Annot",Subtype:"Link",Rect:[C,M,C+L,M+w],Border:[0,0,0],A:g.context.obj({Type:"Action",S:"URI",URI:o.PDFString.of("https://kherrick.github.io/sprite-garden/")})}),$="Game Save",G=24,P=p.widthOfTextAtSize($,G),H=(c-P)/2,ke=M-35;u.drawText($,{x:H,y:ke,size:G,font:b,color:E});let W=await g.embedPng(l),z=.65,U=W.width*z,Y=W.height*z,me=ke-Y-40,pe=(c-U)/2;u.drawRectangle({x:pe-4,y:me-4,width:U+8,height:Y+8,borderWidth:1,borderColor:S,color:i(1,1,1)}),u.drawImage(W,{x:pe,y:me,width:U,height:Y});let Vt=d?.config?.worldSeed||"",rn=Vt?`https://kherrick.github.io/sprite-garden/?seed=${Vt}`:"https://kherrick.github.io/sprite-garden/",Xt=g.context.obj({Type:"Annot",Subtype:"Link",Rect:[pe,me,pe+U,me+Y],Border:[0,0,0],A:g.context.obj({Type:"Action",S:"URI",URI:o.PDFString.of(rn)})}),pt=u.node.get(o.PDFName.of("Annots"));pt?(pt.push(N),pt.push(Xt)):u.node.set(o.PDFName.of("Annots"),g.context.obj([N,Xt]));let Jt=d?.state?.seedInventory||{},nn=Object.values(Jt).reduce((V,Z)=>V+Z,0),sn=30,ht=390,Zt=me-sn-ht,ft=c-160,Qt=(c-ft)/2;u.drawRectangle({x:Qt,y:Zt,width:ft,height:ht,borderWidth:2,borderColor:E,color:f});let an=new Date().toLocaleString(void 0,{dateStyle:"medium",timeStyle:"short"}),O=Zt+ht-25,K=Qt+20,he=18;u.drawText("Saved On:",{x:K,y:O,size:12,font:p,color:E}),u.drawText(an,{x:K+p.widthOfTextAtSize("Saved On: ",12),y:O,size:12,font:b,color:E}),O-=he,u.drawText("World Name:",{x:K,y:O,size:12,font:p,color:E}),u.drawText(s.name,{x:K+p.widthOfTextAtSize("World Name: ",12),y:O,size:12,font:b,color:E}),O-=he,u.drawText("Total Seeds:",{x:K,y:O,size:12,font:p,color:E}),u.drawText(`${nn}`,{x:K+p.widthOfTextAtSize("Total Seeds: ",12),y:O,size:12,font:b,color:E}),O-=he,u.drawText("Game Time:",{x:K,y:O,size:12,font:p,color:E}),u.drawText(`${Math.floor(d?.state?.gameTime||0)}`,{x:K+p.widthOfTextAtSize("Game Time: ",12),y:O,size:12,font:b,color:E}),O-=he+8,u.drawText("Seed Inventory:",{x:K,y:O,size:11,font:p,color:E}),O-=he;let eo=(ft-60)/2,J=0,ie=O,ln=Object.entries(Jt).sort((V,Z)=>V[0].localeCompare(Z[0]));for(let[V,Z]of ln){let xe=K+J*eo,bt=V.replace(/_/g," ");u.drawText(`${bt}: ${Z}`,{x:xe,y:ie,size:9,font:b,color:E}),J++,J>=2&&(J=0,ie-=14)}J!==0&&(ie-=14),O=ie-8,u.drawText("Materials Inventory:",{x:K,y:O,size:11,font:p,color:E}),O-=he;let cn=d?.state?.materialsInventory||{};J=0,ie=O;let dn=Object.entries(cn).sort((V,Z)=>V[0].localeCompare(Z[0]));for(let[V,Z]of dn){let xe=K+J*eo,bt=V.replace(/_/g," ");u.drawText(`${bt}: ${Z}`,{x:xe,y:ie,size:9,font:b,color:E}),J++,J>=2&&(J=0,ie-=14)}let to="Generated by Sprite Garden",oo=12;u.drawText(to,{x:(c-b.widthOfTextAtSize(to,oo))/2,y:40,size:oo,font:b,color:y});let un=`sprite-garden-game-card-${t}.png`;g.attach(new Uint8Array(l),un,{mimeType:"image/png",description:"Sprite Garden Game Card"});let gn=await g.save(),ro=new Blob([gn],{type:"application/pdf"});return{game:s,file:new File([ro],`Sprite-Garden-Game-Save-${t}.pdf`,{type:ro.type,lastModified:Date.now()})}}async getSelectedGameAsPNG(){let t=this.dialog.querySelector('input[name="selectedGame"]:checked');if(!t)return;let o;if(t instanceof HTMLInputElement){let s=parseInt(t.value);o=this.savedGames[s]}let e;if("DecompressionStream"in globalThis){let s=ae(globalThis,o.data,"application/gzip").stream().pipeThrough(new globalThis.DecompressionStream("gzip"));e=await(await new globalThis.Response(s).blob()).text()}else throw new Error("DecompressionStream not supported");let r=ye(globalThis.document,"sprite-garden").querySelector("canvas"),i=await po(r,e);return{game:o,pngSave:i}}async shareSelectedGameAsPDF(){let{game:t,file:o}=await this.getPDFGameStateAttachment();try{typeof navigator<"u"&&typeof navigator.canShare<"u"&&navigator.canShare({files:[o]})?(await navigator.share({files:[o],title:"Sprite Garden Game Save",url:"https://kherrick.github.io/sprite-garden",text:`Visit Sprite Garden, then 'Load' and checkout my world: ${t.name}

`}),console.log("Game shared successfully:",t.name)):alert("Web Share API is not available on this device or browser.")}catch(e){e.name!=="AbortError"?(console.error("Failed to share game:",e),alert("Failed to share game. Check console for details.")):console.log("Game sharing was cancelled by the user")}}async shareSelectedGame(){let t=Me(),o=this.dialog.querySelector('input[name="selectedGame"]:checked');if(!o)return;let e;if(o instanceof HTMLInputElement){let i=parseInt(o.value);e=this.savedGames[i]}let r;if("DecompressionStream"in globalThis){let i=ae(globalThis,e.data,"application/gzip").stream().pipeThrough(new globalThis.DecompressionStream("gzip"));r=await(await new globalThis.Response(i).blob()).text()}else throw new Error("DecompressionStream not supported");try{let i=new Blob([r],{type:"text/plain"}),s=`Sprite-Garden-Game-Save-${t}.json.txt`,a=new File([i],s,{type:"text/plain"});typeof navigator<"u"&&typeof navigator.canShare<"u"&&navigator.canShare({files:[a]})?(await navigator.share({files:[a],title:"Sprite Garden Game Save",url:"https://kherrick.github.io/sprite-garden",text:`Visit Sprite Garden, then 'Load' and checkout my world: ${e.name}

`}),console.log("Game shared successfully:",e.name)):alert("Web Share API is not available on this device or browser.")}catch(i){i.name!=="AbortError"?(console.error("Failed to share game:",i),alert("Failed to share game. Check console for details.")):console.log("Game sharing was cancelled by the user")}}show(){this.dialog instanceof HTMLDialogElement&&this.dialog.showModal()}close(){this.removeEventListeners(),this.dialog instanceof HTMLDialogElement&&this.dialog.close()}};async function yo(n,t,o){let e=new Et(n,t,o);return await e.createDialog(),e.show(),e}function ze(n,t){return Object.entries(t).map(([o,e])=>`${n}${o}${n==="--sg-color-"?"":"-color"}: ${e};`).join(`
`)}var Ye="1.41.0";var xt=!0,Ve=new A.subtle.Watcher(()=>{xt&&(xt=!1,globalThis.queueMicrotask(kn))});function kn(){xt=!0;for(let n of Ve.getPending())n.get();Ve.watch()}function F(n){let t,o=new A.Computed(()=>{typeof t=="function"&&t(),t=n()});return Ve.watch(o),o.get(),()=>{Ve.unwatch(o),typeof t=="function"&&t(),t=void 0}}function So(n,t,o){Object.keys(o).forEach(e=>{let r=e.toLowerCase(),i=n?.getElementById(`${r}Count`);try{i.textContent=o[e]}catch(s){console.error(s)}}),Object.keys(t).forEach(e=>{let r=e.toLowerCase(),i=n?.getElementById(`${r}Count`);if(i)try{i.textContent=t[e]}catch(s){console.error(s)}})}function Eo(n,t,o,e,r,i,s,a,l,d,g){F(()=>{So(n,s.get(),a.get())}),F(()=>{let u=n.getElementById("worldSeedInput");if(u instanceof HTMLInputElement&&!u.value){let c=n.getElementById("currentSeed"),m=r.get();if(c&&m){u.value=m,c.textContent=m;return}let p=String(Q());u.value=p,c.textContent=p}}),F(()=>{let u=i.get(),c=n.getElementById("gameTime");c&&(c.textContent=String(Math.floor(u)))}),F(()=>{let u=g.get(),c=n.getElementById("viewModeText");c&&(c.textContent=u==="normal"?"Oddiy ko\u2018rinish":"Rentgen ko\u2018rinishi")}),F(()=>{let u=e.get(),c=n.getElementById("fogModeText");c&&(c.textContent=u==="fog"?"Tuman":"Ochiq")}),F(()=>{let u=o.get(),c=n.getElementById("breakModeText");c&&(c.textContent=u==="regular"?"Oddiy qazish":"Keng qazish")}),F(()=>{let u=t.get(),c=globalThis.arcadeStorage;c&&Number.isSafeInteger(u)&&u>Number(c.getItem("best")??0)&&c.setItem("best",u);let m=n.getElementById("seedCount");m&&(m.textContent=u)}),F(()=>{let u=d.get(),c=n.getElementById("selectedSeed");c&&(c.textContent=u?.replace("_"," ")||"None")}),F(()=>{let u=l.get(),c=n.getElementById("selectedMaterial");c&&(c.textContent=u?.replace("_"," ")||"None")})}function Ae(n,t,o,e){return n.set(!1),new Le(o,t,e)}function Xe(n){return Object.fromEntries(Object.entries(n).filter(([t,o])=>o.isSeed).map(([t,o])=>[o.id,t]))}function wo(n,t=1){return Object.fromEntries(Object.values(n).map(o=>[o,t]))}function To(n,t,o){if(!(o.currentTarget instanceof HTMLElement))throw new Error("currentTarget is not an HTMLElement");let[e]=Object.keys(o.currentTarget.dataset);for(let a of o.currentTarget.parentElement.children)a.classList.remove("selected");o.currentTarget.classList.toggle("selected"),console.log(`Selecting seed: ${e}`);let r=t.selectedSeedType.get();console.log(`Current selected seed: ${r}`);let i=r===e.toUpperCase()?null:e.toUpperCase();t.selectedSeedType.set(i);let s;i?s=`New selected seed: ${D(i)}`:s="No selected seed.",console.log(s),n.dispatchEvent(new CustomEvent("sprite-garden-toast",{detail:{message:s}}))}function ko(n,t,o,e){for(let r=1;r<t-1;r++)for(let i=1;i<o-1;i++)n.getTile(r,i)===e.AIR&&[n.getTile(r-1,i),n.getTile(r+1,i),n.getTile(r,i-1),n.getTile(r,i+1)].some(a=>a&&a.solid)&&i>T.SURFACE_LEVEL.get()+5&&Math.random()<.5&&n.setTile(r,i,e.MOSS)}function Lt(n,t,o,e,r,i,s){for(let a=n-o;a<=n+o;a++)for(let l=t-o;l<=t+o;l++)a>=0&&a<s&&l>=0&&l<i&&Math.sqrt((a-n)**2+(l-t)**2)<=o&&r.getTile(a,l)!==e.BEDROCK&&r.setTile(a,l,e.AIR)}function xn(n,t,o,e,r,i,s,a,l){let d=o,g=e;for(let u=0;u<t;u++){n+=(Math.random()-.5)*.3,d+=Math.cos(n),g+=Math.sin(n);for(let c=-i;c<=i;c++)for(let m=-i;m<=i;m++){let p=Math.floor(d+c),b=Math.floor(g+m);p>=0&&p<l&&b>=0&&b<a&&Math.sqrt(c*c+m*m)<=i&&s.getTile(p,b)!==r.BEDROCK&&s.setTile(p,b,r.AIR)}Math.random()<.1&&Lt(Math.floor(d),Math.floor(g),2+Math.floor(Math.random()*2),r,s,a,l)}}function xo(n,t,o,e,r){let i=[];for(let s=0;s<25;s++)i.push({x:Math.floor(Math.random()*r),y:n+5+Math.floor(Math.random()*(e-n-15)),size:3+Math.floor(Math.random()*8),branches:1+Math.floor(Math.random()*3)});i.forEach(s=>{Lt(s.x,s.y,s.size,t,o,e,r);for(let a=0;a<s.branches;a++){let l=Math.PI*2*a/s.branches+(Math.random()-.5)*.5,d=10+Math.floor(Math.random()*20);xn(l,d,s.x,s.y,t,1+Math.floor(Math.random()*2),o,e,r)}});for(let s=0;s<50;s++){let a=Math.floor(Math.random()*r),l=n+3+Math.floor(Math.random()*(e-n-10)),d=1+Math.floor(Math.random()*3);Math.random()<.3&&Lt(a,l,d,t,o,e,r)}}var Mo=Math.sqrt(3),Io=Math.sqrt(5),Ln=.5*(Mo-1),Re=(3-Mo)/6,aa=1/3,ia=1/6,la=(Io-1)/4,ca=(5-Io)/20,Lo=n=>Math.floor(n)|0,Co=new Float64Array([1,1,-1,1,1,-1,-1,-1,1,0,-1,0,1,0,-1,0,0,1,0,-1,0,1,0,-1]);function Ao(n=Math.random){let t=Cn(n),o=new Float64Array(t).map(r=>Co[r%12*2]),e=new Float64Array(t).map(r=>Co[r%12*2+1]);return function(i,s){let a=0,l=0,d=0,g=(i+s)*Ln,u=Lo(i+g),c=Lo(s+g),m=(u+c)*Re,p=u-m,b=c-m,v=i-p,h=s-b,f,S;v>h?(f=1,S=0):(f=0,S=1);let E=v-f+Re,y=h-S+Re,k=v-1+2*Re,w=h-1+2*Re,L=u&255,C=c&255,M=.5-v*v-h*h;if(M>=0){let G=L+t[C],P=o[G],H=e[G];M*=M,a=M*M*(P*v+H*h)}let N=.5-E*E-y*y;if(N>=0){let G=L+f+t[C+S],P=o[G],H=e[G];N*=N,l=N*N*(P*E+H*y)}let $=.5-k*k-w*w;if($>=0){let G=L+1+t[C+1],P=o[G],H=e[G];$*=$,d=$*$*(P*k+H*w)}return 70*(a+l+d)}}function Cn(n){let o=new Uint8Array(512);for(let e=0;e<512/2;e++)o[e]=e;for(let e=0;e<512/2-1;e++){let r=e+~~(n()*(256-e)),i=o[e];o[e]=o[r],o[r]=i}for(let e=256;e<512;e++)o[e]=o[e-256];return o}var _o=so(Bo(),1),Mt=null,It=null;function ne(n){n!==It&&(It=n,Mt=Ao((0,_o.default)(n)))}function Je(n,t,o=0,e=3,r=.5,i=.02){(!Mt||It!==o)&&ne(o);let s=0,a=1,l=i,d=0;for(let g=0;g<e;g++)s+=Mt(n*l,t*l)*a,d+=a,a*=r,l*=2;return s/d}function Be(n,t=0){return Je(n,0,t,4,.6,.015)}function At(n,t=500){return Je(n,0,t,2,.8,.008)}function Rt(n,t=2e3){return Je(n,0,t,2,.5,.012)}function Do(n,t=0,o=2e3,e=2,r=.5,i=.01){return Je(n,t,o,e,r,i)}function Oo(n,t,o){ne(o);let e=[];for(let r=0;r<n;r++){let i=t;i+=Be(r,o)*15,i+=Be(r,o+100)*8,i+=Be(r,o+200)*4;let s=Be(r,o+300);s>.6&&(i+=(s-.6)*20),i=Math.max(10,Math.min(t*1.5,i)),e[r]=Math.floor(i)}return Mn(e,2)}function Mn(n,t=1){let o=[...n];for(let e=0;e<t;e++){for(let r=1;r<n.length-1;r++)o[r]=Math.floor((n[r-1]+n[r]*2+n[r+1])/4);n.splice(0,n.length,...o)}return n}function Po(n,t,o,e,r,i,s){let a=Math.floor(e/2);for(let l=-a;l<=a;l++)for(let d=0;d<=Math.floor(e*.3);d++){let g=t+l,u=o+d+1;g>=0&&g<r&&u>=0&&u<i&&Math.sqrt(l*l+d*d*2)<=a&&n.getTile(g,u).id!==s.SAND.id&&n.setTile(g,u,s.WATER)}}function Fo(n,t,o,e,r,i){let s=o+1;if(t>=0&&t<e&&s>=0&&s<r){n.getTile(t,s).id!==i.SAND.id&&n.setTile(t,s,i.WATER);let a=o+2;a<r&&Math.random()<.7&&n.getTile(t,a).id!==i.SAND.id&&n.setTile(t,a,i.WATER)}}function No(n,t,o,e,r,i){let s=o;if(t>=0&&t<e&&s>=0&&s<r){let a=n.getTile(t,s);a===i.AIR&&a.id!==i.SAND.id&&n.setTile(t,s,i.WATER)}}function Go(n,t,o,e,r,i,s){if(!n||typeof n.getTile!="function"){console.error("generateWaterSources: Invalid world object at entry",{hasWorld:!!n,worldType:typeof n,worldWidth:o,worldHeight:e});return}ne(s);for(let a=0;a<o;a++){let l=t[a],d=Rt(a,parseInt(String(s))+2e3);if(l>r&&No(n,a,l,o,e,i),d>.5&&l<r+5){let u=Math.floor((d-.4)*15)+5;Po(n,a,l,u,o,e,i)}Rt(a,parseInt(String(s))+2500)>.5&&Fo(n,a,l,o,e,i)}}function Ho(n,t,o,e,r){ne(r);let i=5,s=Math.max(o-30,60);for(let a=0;a<t;a++)if(Do(r)>.2){let l=Math.floor(6+Math.random()*10),d=Math.floor(4+Math.random()*6),g=i+Math.floor(Math.random()*(s-i));for(let u=0;u<l;u++)for(let c=0;c<d;c++){let m=a+u;if(m>=t||g+c>=o)continue;(!(u===0||u===l-1||c===0||c===d-1)||Math.random()>.3)&&n.getTile(m,g+c)===e.AIR&&n.setTile(m,g+c,e.CLOUD)}a+=l+Math.floor(Math.random()*10)}}function Ze(n,t,o){ne(o);let e=At(n,o+600),r=At(n,o+700),i=e,s=r;return i<-.4?t.TUNDRA:i>.4&&s<-.2?t.DESERT:s>.3?t.SWAMP:t.FOREST}var we=n=>Xe(n);function Qe(n,t,o,e,r,i){let s=o.get();if(s.size===0||(t.frameCounter++,t.frameCounter<t.updateInterval))return;t.frameCounter=0;let a=new Set,l=!1,d=e.get();for(let g of s){let[u,c]=g.split(",").map(Number),m=d.getTile(u,c);if(m===n.WATER){let p=[{x:u-1,y:c},{x:u+1,y:c},{x:u,y:c-1},{x:u,y:c+1}],b=!1;for(let v of p)if(v.x>=0&&v.x<i&&v.y>=0&&v.y<r&&d.getTile(v.x,v.y)===n.LAVA){b=!0;break}if(b){d.setTile(u,c,n.PUMICE);for(let v of p)v.x>=0&&v.x<i&&v.y>=0&&v.y<r&&a.add(`${v.x},${v.y}`);l=!0;continue}if(c+1<r){let v=d.getTile(u,c+1);if(v===n.AIR){d.setTile(u,c,n.AIR),d.setTile(u,c+1,n.WATER),a.add(`${u},${c+1}`),u>0&&a.add(`${u-1},${c+1}`),u<i-1&&a.add(`${u+1},${c+1}`),c+2<r&&a.add(`${u},${c+2}`),l=!0;continue}else if(v===n.LAVA){d.setTile(u,c,n.AIR),d.setTile(u,c+1,n.PUMICE),l=!0;continue}else if(v&&v.solid){let h=u>0&&d.getTile(u-1,c)===n.AIR,f=u<i-1&&d.getTile(u+1,c)===n.AIR,S=h&&c+1<r&&d.getTile(u-1,c+1)===n.AIR,E=f&&c+1<r&&d.getTile(u+1,c+1)===n.AIR;if(S||E){let y=0;S&&E?y=Math.random()<.5?-1:1:S?y=-1:y=1;let k=u+y;d.setTile(u,c,n.AIR),d.setTile(k,c,n.WATER),a.add(`${k},${c}`),a.add(`${k},${c+1}`),l=!0}else if(h||f){let y=0;h&&f?y=Math.random()<.5?-1:1:h?y=-1:y=1;let k=u+y;if(c+1<r){let w=d.getTile(k,c+1);w&&w.solid&&(d.setTile(u,c,n.AIR),d.setTile(k,c,n.WATER),a.add(`${k},${c}`),l=!0)}}}}}else m!==n.WATER&&c>0&&d.getTile(u,c-1)===n.WATER&&a.add(`${u},${c-1}`)}if(l)for(let g of a){let[u,c]=g.split(",").map(Number);for(let m=-2;m<=2;m++)for(let p=-2;p<=2;p++){let b=u+m,v=c+p;b>=0&&b<i&&v>=0&&v<r&&d.getTile(b,v)===n.WATER&&v+1<r&&d.getTile(b,v+1)===n.AIR&&a.add(`${b},${v}`)}}l&&a.size>0?o.set(a):o.set(new Set)}function qo(n,t,o,e,r,i){console.log(`Generating world with seed: ${e}`);let s=new fe(i,r),a=Oo(i,t,e);for(let u=0;u<i;u++){let c=Ze(u,n,e)||n.FOREST,m=a[u];for(let p=0;p<r;p++)if(p>m){let b=p-m;b<2?c.surfaceTile===o.GRASS||c.surfaceTile===o.SNOW?s.setTile(u,p,c.surfaceTile):s.setTile(u,p,c.subTile):b<Q(20,50)?Math.random()<.1?s.setTile(u,p,o.COAL):Math.random()<.95?s.setTile(u,p,c.subTile):s.setTile(u,p,o.STONE):b<Q(50,90)?Math.random()<.05?s.setTile(u,p,o.IRON):Math.random()<.02?s.setTile(u,p,o.GOLD):s.setTile(u,p,o.STONE):p>r-2?s.setTile(u,p,o.BEDROCK):p>r-4||Math.random()<.01?s.setTile(u,p,o.LAVA):s.setTile(u,p,o.STONE)}else p===m&&s.setTile(u,p,c.surfaceTile);if(c.trees&&Math.random()<.025){let p=Q(3,5),b=m,v=u,h=b-1,f=[];for(let y=0;y<p;y++){let k=b-y-1;k>=0&&(s.setTile(u,k,o.TREE_TRUNK),f.push({x:u,y:k,tile:o.TREE_TRUNK}))}let S=b-p,E=3;for(let y=-E;y<=E;y++)for(let k=-E;k<=1;k++){let w=u+y,L=S+k;Math.sqrt(y*y+k*k)<=E&&k<=0&&w>=0&&w<i&&L>=0&&L<r&&!(w===u&&L>=S&&L<b)&&s.getTile(w,L)===o.AIR&&(s.setTile(w,L,o.TREE_LEAVES),f.push({x:w,y:L,tile:o.TREE_LEAVES}))}}if(c.crops.length>0&&Math.random()<.05){let p=c.crops[Math.floor(Math.random()*c.crops.length)],b=m-1;if(b>=0&&s.getTile(u,b)===o.AIR){s.setTile(u,b,p);let v=we(o)[p.id];v&&j("seedInventory",h=>({...h,[v]:(h&&h[v]?h[v]:0)+2}))}}}xo(t,o,s,r,i),ko(s,i,r,o),Ho(s,i,t,o,e),Go(s,a,i,r,t,o,e);let l=new Set;for(let u=0;u<i;u++)for(let c=0;c<r;c++)s.getTile(u,c)===o.WATER&&l.add(`${u},${c}`);let d=new A.State(new Set(l)),g=new A.State(s);for(let u=0;u<100;u++){Qe(o,{updateInterval:0,frameCounter:999,maxIterationsPerUpdate:0,checkRadius:0,dirtyRegions:new Set},d,g,i,r);let c=d.get();if(c.size===0)break;let m=new Set;c.forEach(p=>{let[b,v]=p.split(",").map(Number);m.add(p),b>0&&m.add(`${b-1},${v}`),b<i-1&&m.add(`${b+1},${v}`),v>0&&m.add(`${b},${v-1}`),v<r-1&&m.add(`${b},${v+1}`)}),d.set(m)}return console.log("World generation complete!"),s}function Wo(n,t,o,e){let r={};for(let i=0;i<t;i++)for(let s=0;s<o;s++)if(n.getTile(i,s)===e.TREE_TRUNK){let l=n.getTile(i,s+1),d=n.getTile(i,s-1);if(l&&l.solid&&l!==e.TREE_TRUNK){let g=`${i},${s}`,u=[],c=s;for(;c>=0&&n.getTile(i,c)===e.TREE_TRUNK;)u.push({x:i,y:c,tile:e.TREE_TRUNK}),c--;let m=c+1,p=3;for(let b=-p;b<=p;b++)for(let v=-p;v<=p;v++){let h=i+b,f=m+v;h>=0&&h<t&&f>=0&&f<o&&n.getTile(h,f)===e.TREE_LEAVES&&u.push({x:h,y:f,tile:e.TREE_LEAVES})}r[g]={seedType:"WALNUT",mature:!0,blocks:u,baseX:i,baseY:s}}}return r}function ge(n,t,o,e,r,i,s,a,l,d,g,u,c,m=null){let p;m!==null?(s.set(m),p=m):p=s?.get();let b=qo(n,t,e,p,r,i);l.set({}),a.set(0),d.set(Wo(b,i,r,e));let h=wo(Xe(e),1);c.set(h);let f=Object.fromEntries(Object.entries(e).filter(([y,k])=>!y.toLowerCase().startsWith("tree_")&&k.drops&&!k.isSeed).map(([y,k])=>[y,0]));u.set(f);let S=Math.floor(i/2),E=Math.floor(t-5);for(let y=S-25;y<S+25;y++)for(let k=E-5;k<E+5;k++)if(y>=0&&y<i&&k>=0&&k<r){let w=Math.floor(y),L=Math.floor(k);if(b.getTile(w,L)===e.AIR&&L+1<r&&b.getTile(w,L+1)&&b.getTile(w,L+1).solid){let C=!0;for(let M=L-2;M<=L;M++)if(M>=0&&b.getTile(w,M)!==e.AIR){C=!1;break}if(C){let M={...g.get(),x:y*o,y:k*o,velocityX:0,velocityY:0,lastDirection:0};g.set(M)}}}return b}var R={isEnabled:!1,selectedTile:null,brushSize:1,isDragging:!1,lastPaintedTile:null};function jo(n,t,o){In(n,t,o)}function In(n,t,o){let e=n.getElementById("toggleMapEditor");e&&e.addEventListener("click",()=>{R.isEnabled=!R.isEnabled,An(n,t,o)});let r=n.getElementById("brushSizeSelect");r&&r.addEventListener("change",d=>{d.target instanceof HTMLSelectElement&&(R.brushSize=parseInt(d.target.value))}),n.querySelectorAll(".tile-btn").forEach(d=>{d.addEventListener("click",g=>{if(g.target instanceof HTMLButtonElement){let u=g.target.dataset.tile;Rn(n,u)}})});let i=n.getElementById("clearMapEditor");i&&i.addEventListener("click",()=>{confirm("Clear the entire map? This cannot be undone.")&&Bn()});let s=n.getElementById("fillMapEditor");s&&s.addEventListener("click",()=>{R.selectedTile&&confirm(`Fill current layer with ${R.selectedTile}?`)&&_n(n.getElementById("canvas"))});let a=n.getElementById("saveMapAsState");a&&a.addEventListener("click",()=>{Dn()});let l=n.getElementById("regenerateMap");l&&l.addEventListener("click",()=>{if(confirm("Regenerated world? This will lose all editor changes.")){let d=ge(T.BIOMES,T.SURFACE_LEVEL.get(),T.TILE_SIZE.get(),T.TILES,T.WORLD_HEIGHT.get(),T.WORLD_WIDTH.get(),T.worldSeed,x.gameTime,x.growthTimers,x.plantStructures,x.player,x.materialsInventory,x.seedInventory);x.world.set(d)}})}function An(n,t,o){let e=n.getElementById("mapEditorText"),r=n.getElementById("mapEditorControls");e&&r&&(R.isEnabled?(e.textContent="Disable Editor",r.removeAttribute("hidden"),t.set("clear"),o.set("normal")):(t.set("fog"),e.textContent="Enable Editor",r.setAttribute("hidden","")))}function Rn(n,t){R.selectedTile=t,console.log("Selected tile:",t,"Editor enabled:",R.isEnabled),n.querySelectorAll(".tile-btn").forEach(e=>{e.classList.remove("selected")});let o=n.querySelector(`[data-tile="${t}"]`);o&&o.classList.add("selected")}function Bt(n,t,o,e,r,i,s,a){if(!R.isEnabled||!R.selectedTile||typeof o?.get!="function")return!1;let l=o.get(),d=Math.floor((n+l.x)/r),g=Math.floor((t+l.y)/r);return Uo(d,g,e,i,s,a),!0}function _e(n,t,o,e,r,i,s,a,l=!1){if(!R.isEnabled||!R.selectedTile)return!1;l&&(R.isDragging=!0,R.lastPaintedTile=null);let d=o.get(),g=Math.floor((n+d.x)/r),u=Math.floor((t+d.y)/r),c=`${g},${u}`;return R.lastPaintedTile!==c&&(Uo(g,u,e,i,s,a),R.lastPaintedTile=c),!0}function $o(){R.isDragging=!1,R.lastPaintedTile=null}function Uo(n,t,o,e,r,i){let s=o[R.selectedTile];if(!s)return;let a=i.get(),l=Math.floor(R.brushSize/2),d=!1;for(let g=-l;g<=l;g++)for(let u=-l;u<=l;u++){let c=n+g,m=t+u;c<0||c>=r||m<0||m>=e||R.brushSize>1&&Math.sqrt(g*g+u*u)>l+.5||a.getTile(c,m)!==s&&(a.setTile(c,m,s),d=!0)}d&&x.world.set(a)}function Bn(){let n=T.TILES,t=T.WORLD_WIDTH.get(),o=T.WORLD_HEIGHT.get(),e=new fe(t,o);for(let r=0;r<t;r++)for(let i=0;i<o;i++)e.setTile(r,i,n.AIR);x.world.set(e),x.plantStructures.set({}),x.growthTimers.set({})}function _n(n){if(!R.selectedTile)return;let t=x.camera.get(),o=x.world.get(),e=T.TILES,r=T.TILE_SIZE.get(),i=e[R.selectedTile],s=T.WORLD_HEIGHT.get(),a=T.WORLD_WIDTH.get(),l=Math.floor(t.x/r),d=Math.floor(t.y/r),g=Math.min(a,l+Math.ceil(n.width/r)),u=Math.min(s,d+Math.ceil(n.height/r));for(let c=Math.max(0,l);c<g;c++)for(let m=Math.max(0,d);m<u;m++)o.setTile(c,m,i);x.world.set(o)}async function Dn(){try{let n=ce(globalThis);n.state.exploredMap=x.exploredMap.get().toObject();let t=T.WORLD_WIDTH.get(),o=T.SURFACE_LEVEL.get(),e=T.TILE_SIZE.get();n.state.player={x:t/2*e,y:o*e-50,width:6,height:8,velocityX:0,velocityY:0,speed:3,jumpPower:12,onGround:!1,color:"#FF69B4",lastDirection:0},n.state.gameTime=0,n.state.growthTimers={},n.state.plantStructures={};let r=JSON.stringify(n),i=new Blob([r],{type:"application/json"}),s=URL.createObjectURL(i),a=document.createElement("a");a.href=s,a.download=`sprite-garden-map-${Date.now()}.json`,document.body.append(a),a.click(),document.body.removeChild(a),URL.revokeObjectURL(s),console.log("Map saved as game state file"),alert('Map saved successfully! You can load this file using the "Load Game File" button.')}catch(n){console.error("Failed to save map as state:",n),alert("Failed to save map. Check console for details.")}}var br=so(zo(),1);async function _t(n){let t=await I.getItem("sprite-garden-movement-scale");t||(t=1,await I.setItem("sprite-garden-movement-scale",t)),n.querySelector('[data-key="x"].middle').innerHTML=`&times;${Number(t)}`}async function te(n){let t=Number(Number(await I.getItem("sprite-garden-movement-scale"))||1),o=Number(Number(t.toFixed(2))+.125);o>1&&(o=Number(o.toFixed(1))),o>1&&(o=.5),await I.setItem("sprite-garden-movement-scale",o),await _t(n)}var B={UP:0,DOWN:1,LEFT:2,RIGHT:3,PLANT:4,SELECT_SEED:5,TOGGLE_SPEED:6,NOOP:7},Yo={0:"\u2191",1:"\u2193",2:"\u2190",3:"\u2192",4:"\u{1F331}",5:"\u{1F392}",6:"\u26A1",7:"\u2E3A"},De={0:[87],1:[83],2:[65],3:[68],4:[70],5:[],6:[88],7:[]};var Oe=class{constructor(t,o,e,r){this.nActions=t,this.alpha=o,this.gamma=e,this.epsilon=r,this.q=new Map}stateKey(t){if(!t||typeof t!="object")return"invalid_state";let o={x:isFinite(t.x)?Math.floor(t.x):0,y:isFinite(t.y)?Math.floor(t.y):0,g:t.onGround?1:0,cjmp:t.canJump?1:0,hs:t.hasSeeds?1:0,sel:t.seedSelected?1:0,f:t.farmableBelow?1:0,a:t.airAbove?1:0,p:t.alreadyPlanted?1:0,prog:isFinite(t.progress)?Math.floor(t.progress*10):0,cj:Math.min(t.consecutiveJumps||0,5)};return JSON.stringify(o)}getQ(t){return this.q.has(t)||this.q.set(t,Array(this.nActions).fill(0)),this.q.get(t)}selectAction(t,o=null){let e=this.stateKey(t),r=this.getQ(e);if((!o||o.length===0)&&(o=Array.from({length:this.nActions},(a,l)=>l)),t.onGround&&t.farmableBelow&&!t.alreadyPlanted&&t.hasSeeds&&t.seedSelected&&t.consecutiveJumps<2&&Math.random()>this.epsilon&&(o=o.filter(a=>a!==0)),o.length===0&&(o=[B.PLANT,B.NOOP]),Math.random()<this.epsilon)return o[Math.floor(Math.random()*o.length)];let i=o[0],s=r[i];for(let a of o)r[a]>s&&(s=r[a],i=a);return i}learn(t,o,e,r,i){let s=this.stateKey(t),a=this.stateKey(r),l=this.getQ(s),d=this.getQ(a),g=isFinite(e)?e:0,u=i?0:Math.max(...d);l[o]=isFinite(l[o])?l[o]:0;let c=g+this.gamma*u;l[o]+=this.alpha*(c-l[o]),isFinite(l[o])||(l[o]=0)}exportQ(){return{q:Array.from(this.q.entries()),alpha:this.alpha,gamma:this.gamma,epsilon:this.epsilon}}importQ(t){try{if(!t?.q)throw new Error("No Q-table data");this.q.clear();for(let[o,e]of t.q)if(Array.isArray(e)&&e.length===this.nActions){let r=e.map(i=>isFinite(i)?i:0);this.q.set(o,r)}return typeof t.alpha=="number"&&(this.alpha=Math.max(.01,Math.min(1,t.alpha))),typeof t.gamma=="number"&&(this.gamma=Math.max(.8,Math.min(.99,t.gamma))),typeof t.epsilon=="number"&&(this.epsilon=Math.max(0,Math.min(1,t.epsilon))),!0}catch(o){return console.error("Import failed:",o),this.q.clear(),!1}}};async function _(n){if(typeof n!="number"||Number.isNaN(n))throw new TypeError("ms must be a valid number");if(n<0)throw new RangeError("ms must be non-negative");return new Promise(t=>setTimeout(t,n))}var oe={13:"Enter",17:"Control",32:" ",37:"ArrowLeft",38:"ArrowUp",39:"ArrowRight",40:"ArrowDown",48:"0",49:"1",50:"2",51:"3",52:"4",53:"5",54:"6",55:"7",56:"8",57:"9",65:"a",66:"b",67:"c",68:"d",69:"e",70:"f",71:"g",72:"h",73:"i",74:"j",75:"k",76:"l",77:"m",78:"n",79:"o",80:"p",81:"q",82:"r",83:"s",84:"t",85:"u",86:"v",87:"w",88:"x",89:"y",90:"z"},Vo={...oe,13:"Enter",17:"ControlLeft",32:"Space",48:"Digit0",49:"Digit1",50:"Digit2",51:"Digit3",52:"Digit4",53:"Digit5",54:"Digit6",55:"Digit7",56:"Digit8",57:"Digit9",65:"KeyA",66:"KeyB",67:"KeyC",68:"KeyD",69:"KeyE",70:"KeyF",71:"KeyG",72:"KeyH",73:"KeyI",74:"KeyJ",75:"KeyK",76:"KeyL",77:"KeyM",78:"KeyN",79:"KeyO",80:"KeyP",81:"KeyQ",82:"KeyR",83:"KeyS",84:"KeyT",85:"KeyU",86:"KeyV",87:"KeyW",88:"KeyX",89:"KeyY",90:"KeyZ"};function Te(n,t,o=Vo,e=oe){return new KeyboardEvent(n,{key:e[t]||"",code:o[t]||"",keyCode:t,which:t,bubbles:!0,cancelable:!0,composed:!0})}async function Pe(n,t,o=100){n.dispatchEvent(Te("keydown",t)),await _(o),n.dispatchEvent(Te("keyup",t))}function se(n,t){let o=n.getElementById("canvas");if(o instanceof HTMLCanvasElement){let e=t.currentResolution?.get();if(e==="fullscreen"){n?.host.classList.remove("resolution","resolution-400","resolution-800"),o.width=window.innerWidth,o.height=window.innerHeight,o.style.width="100vw",o.style.height="100vh",t.fogScale.set(36);return}n.host.classList.add("resolution"),n.host.classList.remove("resolution-400","resolution-800");let r=parseInt(e);if(o.width=r,o.height=r,o.style.width=r+"px",o.style.height=r+"px",n.host.classList.add(`resolution-${r}`),e==="800"){t.fogScale.set(24);return}t.fogScale.set(12)}}function tt(n,t,o,e,r,i=5){let s=o.get();for(let a=-i;a<=i;a++)for(let l=-i;l<=i;l++){let d=n+a,g=t+l;d>=0&&d<e&&g>=0&&g<r&&s.add(`${d},${g}`)}}var Fe={A:["0110","1001","1111","1001","1001"],B:["1110","1001","1110","1001","1110"],C:["0111","1000","1000","1000","0111"],D:["1110","1001","1001","1001","1110"],E:["1111","1000","1110","1000","1111"],F:["1111","1000","1110","1000","1000"],G:["0111","1000","1011","1001","0111"],H:["1001","1001","1111","1001","1001"],I:["111","010","010","010","111"],J:["0011","0001","0001","1001","0110"],K:["1001","1010","1100","1010","1001"],L:["1000","1000","1000","1000","1111"],M:["10001","11011","10101","10001","10001"],N:["1001","1101","1011","1001","1001"],O:["0110","1001","1001","1001","0110"],P:["1110","1001","1110","1000","1000"],Q:["0110","1001","1001","1011","0111"],R:["1110","1001","1110","1010","1001"],S:["01110","10000","01100","00010","11100"],T:["11111","00100","00100","00100","00100"],U:["1001","1001","1001","1001","0110"],V:["10001","10001","10001","01010","00100"],W:["10001","10001","10101","11011","10001"],X:["1001","1001","0110","1001","1001"],Y:["1001","1001","0110","0010","0010"],Z:["1111","0010","0100","1000","1111"],0:["0110","1001","1001","1001","0110"],1:["010","110","010","010","111"],2:["1110","0001","1110","1000","1111"],3:["1110","0001","1110","0001","1110"],4:["1001","1001","1111","0001","0001"],5:["1111","1000","1110","0001","1110"],6:["0110","1000","1110","1001","0110"],7:["1111","0001","0010","0100","0100"],8:["0110","1001","0110","1001","0110"],9:["0110","1001","0111","0001","0110"],",":["0","0","0","10","10"],".":["0","0","0","0","1"],"?":["1110","0001","0110","0000","0100"],"!":["1","1","1","0","1"]," ":["0","0","0","0","0"],"-":["0","0","1111","0","0"],"/":["0001","0010","0100","1000","0000"],":":["0","1","0","1","0"],"@":["0110","1011","1011","1000","0111"],\u00C4:["01010","0110","1001","1111","1001"],\u00D6:["01010","0110","1001","1001","0110"],\u00DC:["01010","1001","1001","1001","0110"],\u00DF:["1100","1010","1110","1010","1100"],\u00C0:["0100","0110","1111","1001","1001"],\u00C2:["0100","1010","1111","1001","1001"],\u00C6:["01111","10100","11110","10100","11111"],\u00C7:["0111","1000","1000","1000","0111","0010"],\u00C9:["0100","1111","1000","1110","1000"],\u00C8:["0100","1111","1000","1110","1000"],\u00CA:["0100","1010","1111","1000","1111"],\u00CB:["01010","1111","1000","1110","1000"],\u00CE:["010","101","010","010","111"],\u00CF:["101","010","010","010","111"],\u00D4:["0100","1010","0110","1001","0110"],\u0152:["01111","10100","10110","10100","01111"],\u00D9:["0100","1001","1001","1001","0110"],\u00DB:["0100","1010","1001","1001","0110"],\u0178:["101","1001","0110","0010","0010"],\u00D1:["10101","11011","10111","10011","10001"],"\xBF":["0010","0000","0110","1000","0111"],"\xA1":["0","1","0","1","1"]};var ot=class{constructor(){this.gThis=globalThis,this.doc=this.gThis.document,this.config=this.gThis.spriteGarden.config,this.state=this.gThis.spriteGarden.state,this.computed=this.gThis.spriteGarden.computed,this.tiles=this.config.TILES}get shadow(){return ye(this.doc,"sprite-garden")}getWorld(){return this.gThis.spriteGarden.getState("world")}setWorld(t){this.gThis.spriteGarden.setState("world",t)}setFogMode(t){this.config.fogMode.set(t)}setBreakMode(t){this.config.breakMode.set(t)}getInventory(){return this.state.materialsInventory?.get?this.state.materialsInventory.get():{}}getMaterialCount(t){return this.getInventory()[t]||0}setMaterialCount(t,o){let e=this.getInventory();e[t]=o,this.state.materialsInventory.set(e)}getTile(t,o){return this.getWorld().getTile(t,o)}setTile(t,o,e){let r=this.getWorld();r.setTile(t,o,e),this.setWorld(r)}batchSetTiles(t){let o=this.getWorld();t.forEach(({x:e,y:r,tile:i})=>o.setTile(e,r,i)),this.setWorld(o)}fillRect(t,o,e,r,i){let s=[];for(let a=0;a<r;a++)for(let l=0;l<e;l++)s.push({x:t+l,y:o+a,tile:i});this.batchSetTiles(s)}drawBorder(t,o,e,r,i){let s=[];for(let a=0;a<r;a++)for(let l=0;l<e;l++)(a===0||a===r-1||l===0||l===e-1)&&s.push({x:t+l,y:o+a,tile:i});this.batchSetTiles(s)}drawBitmap(t,o,e,r,i=this.tiles.AIR){let s=[];for(let a=0;a<t.length;a++)for(let l=0;l<t[a].length;l++){let g=t[a][l]===1?r:i;s.push({x:o+l,y:e+a,tile:g})}this.batchSetTiles(s)}drawSparseBitmapBottomCenterMultiTiles(t,o,e){let r=t[0].length,i=t.length,s=o-Math.floor(r/2),a=[];for(let l=0;l<i;l++)for(let d=0;d<r;d++){let g=t[l][d];if(g){let u=s+d,c=e-(i-1-l),m=this.tiles[g];m||(console.warn(`Tile not found for ${g}, falling back to AIR`),m=this.tiles.AIR),a.push({x:u,y:c,tile:m})}}return this.batchSetTiles(a),{x:s,y:e-i,width:r,height:i}}drawSparseBitmapBottomCenter(t,o,e,r=this.tiles.LAVA){let i=t[0].length,s=t.length,a=o-Math.floor(i/2),l=[];for(let d=0;d<s;d++)for(let g=0;g<i;g++)if(t[d][g]===1){let u=a+g,c=e-(s-1-d);l.push({x:u,y:c,tile:r})}return this.batchSetTiles(l),{x:a,y:e-s,width:i,height:s}}getTextWidth(t,o){let e=0;for(let r of t){let i=o[r.toUpperCase()];i&&(e+=i[0].length+1)}return Math.max(0,e-1)}drawText(t,o,e,r,i=this.tiles.DIRT,s=1,a=Fe){let l=[],d=o;for(let g of t.toUpperCase()){let u=a[g];if(u){for(let c=0;c<u.length;c++)for(let m=0;m<u[c].length;m++){let b=u[c][m]==="1"?r:i;l.push({x:d+m,y:e+c,tile:b})}for(let c=0;c<5;c++)for(let m=0;m<s;m++)l.push({x:d+u[0].length+m,y:e+c,tile:i});d+=u[0].length+s}}return this.batchSetTiles(l),{x:o,y:e,width:d-o,height:5}}getMaxVisibleChars(t,o=Fe){return Math.max(1,Math.floor((t-1)/5))}getScrollableTextLine(t,o,e){if(t.length<=e)return t;let r=Math.max(0,t.length-e),i=Math.min(o,r);return t.substring(i,i+e)}drawScrollableTextBox(t,o,e,r=30,i={},s=Fe){let{textTile:a=this.tiles.WATER,backgroundTile:l=this.tiles.DIRT,borderTile:d=this.tiles.CLAY,padding:g=1,scrollX:u=0}=i,m=5+g*2;this.drawBorder(o,e,r,m,d),this.fillRect(o+1,e+1,r-2,m-2,l);let p=this.getMaxVisibleChars(r,s)+1,b=this.getScrollableTextLine(t,u,p),v=e+g;return this.drawText(b,o+g,v,a,l,1,s),{x:o,y:e,width:r,height:m,maxVisibleChars:p}}drawTextWithBox(t,o,e,r={},i=Fe){let{textTile:s=this.tiles.WATER,backgroundTile:a=this.tiles.DIRT,borderTile:l=this.tiles.CLAY,padding:d=1,waterDropDelay:g=500}=r,u=this.getTextWidth(t,i),c=5,m=u+d*2,p=c+d*2;return this.drawBorder(o,e,m,p,l),this.fillRect(o+1,e+1,m-2,p-2,a),this.drawText(t,o+d,e+d,s,a,1,i),g>0&&setTimeout(()=>this.createWaterDropEffect(o,e,m,p),g),{x:o,y:e,width:m,height:p}}async createWaterDropEffect(t,o,e,r){let i=this.getWorld(),s=[];for(let g=t;g<t+e;g++)s.push(g);let a=0,l=5,d=()=>{let g=Math.min(a+l,s.length);for(let u=a;u<g;u++)i.setTile(s[u],o+r-1,this.tiles.AIR);a===0&&tt(t,o,this.state.waterPhysicsQueue,this.config.WORLD_WIDTH.get(),this.config.WORLD_HEIGHT.get(),Math.max(e,r)+2),this.setWorld(i),a=g,a<s.length&&requestAnimationFrame(d)};d()}async getQRCodeModule(){let{default:t}=await import("./qrcode-BROTBPGA.mjs");return t}async drawQRCode(t,o,e,r=this.tiles.ICE,i=this.tiles.COAL){let a=(await this.getQRCodeModule())(0,"L");a.addData(t),a.make();let l=a.getModuleCount(),d=[];for(let g=0;g<l;g++)for(let u=0;u<l;u++){let m=a.isDark(g,u)?r:i;d.push({x:o+u,y:e+g,tile:m})}return this.batchSetTiles(d),{x:o,y:e,width:l,height:l,data:a.createDataURL()}}async holdKey(t){this.shadow.dispatchEvent(Te("keydown",t))}async releaseKey(t){this.shadow.dispatchEvent(Te("keyup",t))}async releaseAllKeys(t,o=50){for(let e of t)await this.releaseKey(e);await _(o)}async pressKeyRepeat(t,o,e=100){for(let r=0;r<o;r++)await Pe(this.shadow,t,e/2),await _(e)}async pressKeySequence(t,o=100){for(let e of t)await Pe(this.shadow,e,o/2),await _(o)}async showFullScreen(){let t=this.shadow.getElementById("resolutionSelect");t.querySelector('[value="fullscreen"]').removeAttribute("hidden"),t instanceof HTMLSelectElement&&(t.value="fullscreen")}async setFullscreen(){this.showFullScreen(),this.config.currentResolution.set("fullscreen"),se(this.shadow,this.config)}async moveAndDig(t,o,e=50){let r=0;for(;r<o;){let i=Math.min(e,o-r);console.log(`Moving with directionKey ${t} and digging ${i} tiles`),await this.holdKey(t),await this.pressKeyRepeat(82,i,100),await this.releaseKey(t),r+=i}}};function Xo(n){if(!n)return"";try{return n.querySelector("#selectedSeed")?.textContent?.trim()||""}catch{return""}}function Jo({getExportData:n}){let t=n();if(!t)return;let o=new Blob([JSON.stringify(t,null,2)],{type:"application/json"}),e=URL.createObjectURL(o),r=document.createElement("a");r.href=e,r.download=`sprite-garden-qtable-${Date.now()}.json`,r.click(),URL.revokeObjectURL(e)}function Zo({importAgent:n,log:t}){let o=document.createElement("input");o.type="file",o.accept=".json",o.addEventListener("change",async e=>{let i=e.target?.files?.[0];if(i)try{let s=await i.text(),a=JSON.parse(s);n(a)}catch(s){let a=s instanceof Error?s.message:String(s??"Unknown error");t(`\u274C Import failed: ${a}`,"error")}}),o.click()}async function Qo({logsContainer:n,log:t}){if(!n){t("\u274C Log container not found.","error");return}let e=Array.from(n.children).map(r=>{let i=r.querySelector(".train-log-time"),s=r.querySelector(".train-log-message");if(i&&s){let a=i.textContent?.trim()??"",l=s.textContent?.trim()??"";return`[${a}] ${l}`}return""}).filter(Boolean).join(`
`);if(!e){t("\u{1F4CB} No logs to copy.","info");return}try{await navigator.clipboard.writeText(e),t("\u{1F4CB} Logs copied to clipboard.","success")}catch(r){t("\u274C Failed to copy logs.","error"),console.error("Copy logs error:",r)}}function Dt(n){let t=[];return n.canJump&&n.onGround&&t.push(B.UP),n.hasSeeds&&n.farmableBelow&&!n.alreadyPlanted&&t.push(B.PLANT),n.hasSeeds&&(!n.seedSelected||n.selectedSeedCount===0)&&t.push(B.SELECT_SEED),t.push(B.LEFT),t.push(B.RIGHT),t.push(B.TOGGLE_SPEED),t.push(B.NOOP),t}function Ne(n){if(!n)return[];try{return Array.from(n.querySelectorAll("button.seed-btn")||[])}catch{return[]}}function Ot(n){let t=Ne(n);if(!t.length)return;let o=t.findIndex(r=>r.classList.contains("selected")||r.getAttribute("aria-pressed")==="true");o<0&&(o=-1);let e=null;for(let r=0;r<t.length;r++){let i=(o+1+r)%t.length,s=t[i],a=s.textContent||"";if(parseInt(a.match(/\d+/)?.[0]||"0")>0){e=s;break}}e&&(t.forEach(r=>{let i=r===e;r.classList.toggle("selected",i),r.setAttribute("aria-pressed",i?"true":"false")}),e.click())}var rt=class extends ot{#o=!1;#t=null;#e=null;constructor(){super(),this.isHalfSpeed=new A.State(!1),this.isTraining=new A.State(!1),this.isTesting=new A.State(!1),this.agent=null,this.bestTime=new A.State(1/0),this.episode=new A.State(0),this.totalEpisodes=new A.State(0),this.consecutiveFailures=new A.State(0),this.successStreak=new A.State(0),this.logs=new A.State([]),this.startTime=new A.State(0),this.stepCount=new A.State(0),this.plantedTiles=new A.State(new Set),this.lastPos=new A.State(null),this.stillStepsAtTile=new A.State(0),this.jumpCount=new A.State(0),this.consecutiveJumps=new A.State(0),this.lastActionWasJump=new A.State(!1),this.speedToggleCount=new A.State(0),this.failedPlantAttempts=new A.State(0),this.lastFailedPlantTime=new A.State(0),this.seedsLeftToPlant=this.computed.totalSeeds,this.trainAction=new A.State(null),this.seedsPlanted=new A.State(0),this.lastReward=new A.State(0),this.lastJumpTime=0}async initialize(){this.#e?.panel||this.#n(),this.log("\u2705 Environment initialized","success")}#n(){let t=this.shadow?.querySelector("#trainPanel");t&&(this.#e={panel:t,episode:this.shadow.querySelector("#trainEpisode"),totalEpisodes:this.shadow.querySelector("#trainTotalEpisodes"),bestTime:this.shadow.querySelector("#trainBestTime"),qTableSize:this.shadow.querySelector("#trainQTableSize"),epsilon:this.shadow.querySelector("#trainEpsilon"),seedsPlanted:this.shadow.querySelector("#seedsPlanted"),seedsLeftToPlant:this.shadow.querySelector("#seedsLeftToPlant"),successStreak:this.shadow.querySelector("#trainSuccessStreak"),trainAction:this.shadow.querySelector("#trainAction"),consecutiveFailures:this.shadow.querySelector("#trainConsecutiveFailures"),logs:this.shadow.querySelector("#trainLogs"),episodesInput:this.shadow.querySelector("#trainEpisodes"),maxStepsInput:this.shadow.querySelector("#trainMaxSteps"),alphaInput:this.shadow.querySelector("#trainAlpha"),gammaInput:this.shadow.querySelector("#trainGamma"),epsilonInput:this.shadow.querySelector("#trainEpsilonInput"),decayInput:this.shadow.querySelector("#trainDecay"),testEpsilonInput:this.shadow.querySelector("#testEpsilonInput"),startBtn:this.shadow.querySelector("#trainStartBtn"),stopBtn:this.shadow.querySelector("#trainStopBtn"),testBtn:this.shadow.querySelector("#trainTestBtn"),exportBtn:this.shadow.querySelector("#trainExportBtn"),importBtn:this.shadow.querySelector("#trainImportBtn"),clearLogsBtn:this.shadow.querySelector("#trainClearLogsBtn"),copyLogsBtn:this.shadow.querySelector("#trainCopyLogsBtn")},this.#e.stopBtn?.addEventListener("click",()=>this.stop()),this.#e.testBtn?.addEventListener("click",()=>this.test()),this.#e.clearLogsBtn?.addEventListener("click",()=>this.clearLogs()),this.#e.startBtn?.addEventListener("click",()=>this.handleStartTraining()),this.#e.exportBtn?.addEventListener("click",()=>Jo({getExportData:this.exportAgent.bind(this)})),this.#e.importBtn?.addEventListener("click",()=>Zo({importAgent:this.importAgent.bind(this),log:this.log.bind(this)})),this.#e.copyLogsBtn?.addEventListener("click",()=>Qo({logsContainer:this.#e?.logs,log:this.log.bind(this)})),this.#s())}#s(){let t=this.#e;t&&(F(()=>{t.trainAction&&(t.trainAction.textContent=this.trainAction.get()??"")}),F(()=>{let o=this.isTraining.get(),e=this.isTesting.get();t.startBtn&&(t.startBtn.disabled=o),t.stopBtn&&(t.stopBtn.disabled=!o&&!e),t.testBtn&&(t.testBtn.disabled=o)}),F(()=>{t.episode&&(t.episode.textContent=String(this.episode.get())),t.totalEpisodes&&(t.totalEpisodes.textContent=String(this.totalEpisodes.get()))}),F(()=>{let o=this.bestTime.get();t.bestTime&&(t.bestTime.textContent=o===1/0?"--":o.toFixed(1))}),F(()=>{if(t.seedsPlanted&&(t.seedsPlanted.textContent=String(this.seedsPlanted.get())),t.seedsLeftToPlant){let o=this.seedsLeftToPlant.get();t.seedsLeftToPlant.textContent=String(o??0)}}),F(()=>{t.successStreak&&(t.successStreak.textContent=String(this.successStreak.get())),t.consecutiveFailures&&(t.consecutiveFailures.textContent=String(this.consecutiveFailures.get()))}),F(()=>{this.isTraining.get(),this.episode.get(),this.agent&&(t.qTableSize&&(t.qTableSize.textContent=String(this.agent.q.size)),t.epsilon&&(t.epsilon.textContent=this.agent.epsilon.toFixed(3)))}))}async handleStartTraining(){let t={episodes:parseInt(this.#e.episodesInput?.value??"")||50,maxSteps:parseInt(this.#e.maxStepsInput?.value??"")||100,alpha:parseFloat(this.#e.alphaInput?.value??"")||.2,gamma:parseFloat(this.#e.gammaInput?.value??"")||.95,epsilon:parseFloat(this.#e.epsilonInput?.value??"")||.2,decay:parseFloat(this.#e.decayInput?.value??"")||.995};await this.startTraining(t)}clearLogs(){this.logs.set([])}updateLogsDisplay(t){if(!this.#e?.logs)return;let o=document.createElement("div");o.className=`train-log-entry train-log-entry--${t.type}`,o.innerHTML=`
      <span class="train-log-time">${t.time}</span>
      <span class="train-log-message">${t.message}</span>
    `,this.#e.logs.prepend(o)}#r(){this.#t||(this.#t=t=>{if(!t.detail?.message)return;let o=t.detail.message;this.log(`\u{1F35E} ${o}`)},this.shadow?.addEventListener("sprite-garden-toast",this.#t))}#i(){this.#t&&(this.shadow?.removeEventListener("sprite-garden-toast",this.#t),this.#t=null)}getState(){try{let t=this.state?.player?.get();if(!t)throw new Error("Player state not available");let o=this.getWorld(),e=this.config?.TILE_SIZE?.get()||16,r=Math.floor((t.x+t.width/2)/e),i=Math.floor((t.y+t.height/2)/e),s=o?.getTile?.(r,i+1),a=o?.getTile?.(r,i-1),l=this.computed?.totalSeeds?.get()??0,d=l>0,u=Ne(this.shadow).find(y=>y.classList.contains("selected")),c=!!u,m=null,p=0;if(u){m=Object.keys(u.dataset)[0]||null;let y=u.textContent||"";p=parseInt(y.match(/\d+/)?.[0]||"0")}let b=l>0?this.seedsPlanted.get()/l:0,v=`${r},${i}`,h=this.plantedTiles.get().has(v),S=Date.now()-this.lastJumpTime>=1e3;return{x:isFinite(r)?r:0,y:isFinite(i)?i:0,airAbove:!a||a.id===0,alreadyPlanted:h,canJump:S,consecutiveJumps:this.consecutiveJumps.get(),farmableBelow:!!s?.farmable,hasSeeds:d,isHalfSpeed:this.isHalfSpeed.get(),onGround:!!t.onGround,progress:b,seedSelected:c,selectedSeedType:m,selectedSeedCount:p,seedsLeftToPlant:l,seedsPlanted:this.seedsPlanted.get(),stepCount:this.stepCount.get()}}catch{return this.log("State error:","error"),{x:0,y:0,airAbove:!1,alreadyPlanted:!1,canJump:!0,consecutiveJumps:0,farmableBelow:!1,hasSeeds:!1,isHalfSpeed:!1,onGround:!1,progress:0,seedSelected:!1,selectedSeedType:null,selectedSeedCount:0,seedsLeftToPlant:0,seedsPlanted:0,stepCount:0}}}async step(t){this.trainAction.set(Yo[t]);let o=this.getState();if(this.computed?.totalSeeds?.get()===0)return await this.releaseAllKeys(Object.keys(oe)),{state:o,reward:0,done:!0};let r=this.failedPlantAttempts.get(),i=t===B.UP&&(!o.canJump||!o.onGround);await this.#a(t),await _(30);let s=this.getState(),l=this.failedPlantAttempts.get()>r;if(this.stepCount.get()>200&&s.seedsPlanted===0&&s.hasSeeds)return this.log(`\u26A0\uFE0F Ep ${this.episode.get()} Force End: no planting after 200 steps`,"warning"),{state:s,reward:-500,done:!0};let d=this.#l(o,s,t,l,i),g=this.lastPos.get(),u=g&&s.x===g.x&&s.y===g.y&&this.stillStepsAtTile.get()>300,m=this.computed?.totalSeeds?.get()===0||this.stepCount.get()>=800||!s.hasSeeds&&this.stepCount.get()>150||u||s.progress>=.95&&this.stepCount.get()>400;return m&&await this.releaseAllKeys(Object.keys(oe)),this.stepCount.set(this.stepCount.get()+1),this.lastReward.set(d),{state:s,reward:d,done:m}}async#a(t){try{if(t===B.UP){let e=Date.now(),r=e-this.lastJumpTime;if(this.log(`\u{1F998} Jump attempt: cooldown=${r}ms, onGround=${this.getState().onGround}`),r<1e3)return this.log("\u274C Jump blocked: cooldown"),_(50);if(!this.getState().onGround)return _(50);this.lastJumpTime=e,this.jumpCount.set(this.jumpCount.get()+1),this.consecutiveJumps.set(this.consecutiveJumps.get()+1),this.lastActionWasJump.set(!0)}else this.lastActionWasJump.get()&&this.consecutiveJumps.set(0),this.lastActionWasJump.set(!1);if(t===B.SELECT_SEED)return Ot(this.shadow),await _(100);if(t===B.TOGGLE_SPEED)return this.speedToggleCount.set(this.speedToggleCount.get()+1),this.isHalfSpeed.get()?(await I.setItem("sprite-garden-movement-scale",.885),await te(this.shadow),this.isHalfSpeed.set(!1),await _(100)):(await I.setItem("sprite-garden-movement-scale",1),await te(this.shadow),this.isHalfSpeed.set(!0),await _(100));if(t===B.PLANT)return Xo(this.shadow)||(Ot(this.shadow),await _(100)),await this.releaseAllKeys(Object.keys(oe)),await _(50),await this.holdKey(70),await _(250),await this.releaseKey(70),_(50);if(t===B.NOOP)return _(50);let o=De[t];if(o?.length){let e=t===B.UP?90:130;for(let r of o)await Pe(this.shadow,r,e)}}catch{this.log("Action error:","error")}}resetEnvironment(){return this.consecutiveJumps.set(0),this.failedPlantAttempts.set(0),this.jumpCount.set(0),this.lastActionWasJump.set(!1),this.lastFailedPlantTime.set(0),this.lastJumpTime=0,this.lastPos.set(null),this.plantedTiles.set(new Set),this.seedsPlanted.set(0),this.speedToggleCount.set(0),this.startTime.set(Date.now()),this.stepCount.set(0),this.stillStepsAtTile.set(0),this.isHalfSpeed.set(!1),this.getState()}#l(t,o,e,r=!1,i=!1){let s=0;if(o.seedsPlanted>t.seedsPlanted){s+=500;let p=new Set(this.plantedTiles.get());p.add(`${o.x},${o.y}`),this.plantedTiles.set(p),this.lastPos.set({x:o.x,y:o.y});let b=Math.max(0,1-this.stepCount.get()/200);return s+=b*100,s}if(i)return-50;if(r)return-40;let a=`${o.x},${o.y}`,l=!this.plantedTiles.get().has(a),d=this.lastPos.get(),g=!d||o.x!==d.x||o.y!==d.y;!t.seedSelected&&o.seedSelected&&(s+=50),o.hasSeeds&&o.seedSelected?s+=2:o.hasSeeds&&!o.seedSelected&&(s-=5);let u=o.onGround&&o.farmableBelow&&!o.alreadyPlanted;u&&o.seedSelected&&o.hasSeeds&&(s+=40,l&&(s+=20)),e===B.PLANT&&(t.onGround&&t.farmableBelow&&!t.alreadyPlanted&&t.seedSelected&&t.hasSeeds?s+=80:s-=15),g&&l&&(s+=10),(e===B.LEFT||e===B.RIGHT)&&o.onGround&&(s+=8,o.hasSeeds&&o.seedSelected&&(s+=7)),e===B.UP&&(u&&t.seedSelected&&t.hasSeeds?s-=100:this.consecutiveJumps.get()>=2?s-=20*this.consecutiveJumps.get():s-=2);let c=this.stillStepsAtTile.get();g?(this.lastPos.set({x:o.x,y:o.y}),this.stillStepsAtTile.set(0)):c>20?s-=Math.min(20,c/3):s-=1,e===B.NOOP&&(s-=10);let m=isFinite(o.progress)&&isFinite(t.progress)?o.progress-t.progress:0;return m>0&&(s+=m*500),o.stepCount>50&&o.seedsPlanted===0&&o.hasSeeds&&(s-=10),g?(this.lastPos.set({x:o.x,y:o.y}),this.stillStepsAtTile.set(0)):this.stillStepsAtTile.set(c+1),isFinite(s)?s:0}getEpisodeTime(){return(Date.now()-this.startTime.get())/1e3}createAgent(t=.15,o=.98,e=.4){this.agent=new Oe(8,t,o,e),this.log(`\u{1F916} Agent created (\u03B1=${t}, \u03B3=${o}, \u03B5=${e})`,"info")}log(t,o="info"){let e={time:new Date().toLocaleTimeString(),message:t,type:o};this.logWatcher(e);let r=this.logs.get();this.logs.set([...r,e]),this.updateLogsDisplay(e),console.log(`[${e.time}] ${t}`)}logWatcher(t){let o=t.message.toLowerCase();if(o.includes("cannot plant")){let e=this.failedPlantAttempts.get()+1;this.failedPlantAttempts.set(e),this.lastFailedPlantTime.set(Date.now())}if(o.includes("cannot farm")){let e=this.failedPlantAttempts.get()+1;this.failedPlantAttempts.set(e),this.lastFailedPlantTime.set(Date.now())}if(o.includes("planted")){let e=this.seedsPlanted.get()+1;this.seedsPlanted.set(e)}}async startTraining(t={episodes:50,maxSteps:100,alpha:.2,gamma:.95,epsilon:.2,decay:.995}){let{episodes:o,maxSteps:e,alpha:r,gamma:i,epsilon:s,decay:a}=t;this.#o=!1,this.#t||this.#r(),this.agent||this.createAgent(r,i,s),this.agent.q.size<100&&(this.agent.epsilon=s),this.isTraining.set(!0),this.totalEpisodes.set(o),this.log(`\u{1F680} Starting training: ${o} episodes`);let l=Math.random()<.5;this.log(`Executing initial jump sequence Up / ${l?"Left":"Right"}`);let[d]=De[B.LEFT],[g]=De[B.RIGHT],[u]=De[B.UP],c=l?d:g;await this.holdKey(u),await this.holdKey(c),await _(200),await this.releaseKey(u),await this.releaseKey(c),await _(100);for(let m=0;m<o&&!this.#o;m++){this.episode.set(m+1);let p=this.resetEnvironment(),b=0;for(let v=0;v<e;v++){if(this.#o){this.log("\u23F9 Stopped mid-training","warning");return}let h=Dt(p),f=this.agent.selectAction(p,h),{state:S,reward:E,done:y}=await this.step(f);if(this.agent.learn(p,f,E,S,y),b+=E,p=S,y){let k=this.getEpisodeTime(),w=this.seedsPlanted.get(),L=this.computed?.totalSeeds?.get()??0,C=w>=L&&L===0;C&&k<this.bestTime.get()&&(this.bestTime.set(k),this.log(`\u{1F3C6} New best time: ${k.toFixed(1)}s (${w}/${L})`,"success"));let M=b/(v+1);this.log(`Episode ${m+1}: ${w}/${L} (${(w/Math.max(L,1)*100).toFixed(0)}%) ${k.toFixed(1)}s ${C?"\u2705":"\u274C"} | Jumps: ${this.jumpCount.get()} | FailedPlants: ${this.failedPlantAttempts.get()} | AvgR: ${M.toFixed(1)}`,C?"success":"warning"),C?(this.consecutiveFailures.set(0),this.successStreak.set(this.successStreak.get()+1),this.log("\u2705 All seeds planted. Training goal reached.","success"),this.#o=!0):(this.consecutiveFailures.set(this.consecutiveFailures.get()+1),this.successStreak.set(0),this.consecutiveFailures.get()>=5&&(this.agent.epsilon=Math.min(.75,this.agent.epsilon+.15),this.log(`\u{1F525} Exploration boost: \u03B5=${this.agent.epsilon.toFixed(3)}`,"warning"),this.consecutiveFailures.set(0)));break}}this.#o&&this.log("\u23F9 Training stopped.","success"),this.agent.epsilon=Math.max(.15,this.agent.epsilon*a),await _(30)}this.isTraining.set(!1),this.log("\u2705 Training complete!","success")}async hardResetEnvironment(){await this.releaseAllKeys(Object.keys(oe)),Ne(this.shadow).forEach(o=>o.classList.remove("selected")),await I.setItem("sprite-garden-movement-scale",.885),await te(this.shadow),this.consecutiveJumps.set(0),this.failedPlantAttempts.set(0),this.jumpCount.set(0),this.lastActionWasJump.set(!1),this.lastFailedPlantTime.set(0),this.lastJumpTime=0,this.lastPos.set(null),this.plantedTiles.set(new Set),this.speedToggleCount.set(0),this.startTime.set(Date.now()),this.stepCount.set(0),this.stillStepsAtTile.set(0),this.isHalfSpeed.set(!1),this.lastReward.set(0)}stop(){this.#o=!0,this.isTraining.set(!1),this.isTesting.set(!1),this.#i(),this.log("\u23F9 Stopped","warning")}async test(t=100){if(this.hardResetEnvironment(),this.#o=!1,this.isTesting.set(!0),!this.agent){this.log("\u274C No trained agent available","error");return}let o=this.agent.epsilon,e=parseFloat(this.#e.testEpsilonInput?.value??"")||.25;this.agent.epsilon=e,this.#t||this.#r();let r=this.resetEnvironment();this.log(`\u{1F9EA} Testing trained agent (\u03B5=${e})...`,"info");for(let l=0;l<t;l++){if(this.#o){this.log("\u{1F6D1} Test stopped","warning"),await this.releaseAllKeys(Object.keys(oe));return}let d=Dt(r),g=this.agent.selectAction(r,d),{state:u,done:c}=await this.step(g);if(r=u,c){await this.releaseAllKeys(Object.keys(oe));break}}let i=this.getEpisodeTime(),s=this.seedsPlanted.get(),a=this.computed.totalSeeds.get();this.log(`TEST: ${s} in ${i.toFixed(1)}s | Jumps: ${this.jumpCount.get()} | Failed Plants: ${this.failedPlantAttempts.get()} | Remaining Seeds: ${a}`,a===0?"success":"warning"),this.agent.epsilon=o,this.isTesting.set(!1)}exportAgent(){if(!this.agent)return this.log("\u274C No agent to export","error"),null;let t=this.agent.exportQ();return this.log("\u{1F4BE} Agent exported","success"),t}importAgent(t){this.agent||this.createAgent();let o=this.agent.importQ(t);return o?this.log(`\u{1F4E5} Imported Q-table: ${this.agent.q.size} states`,"success"):this.log("\u274C Import failed","error"),o}getStats(){return{episode:this.episode.get(),totalEpisodes:this.totalEpisodes.get(),bestTime:this.bestTime.get(),isTraining:this.isTraining.get(),isTesting:this.isTesting.get(),isHalfSpeed:this.isHalfSpeed.get(),qTableSize:this.agent?.q.size||0,epsilon:this.agent?.epsilon||0,consecutiveFailures:this.consecutiveFailures.get(),successStreak:this.successStreak.get(),seedsPlanted:this.seedsPlanted.get(),seedsLeftToPlant:this.seedsLeftToPlant.get(),jumpCount:this.jumpCount.get(),failedPlantAttempts:this.failedPlantAttempts.get(),lastReward:this.lastReward.get()}}dispose(){this.#t&&(this.shadow?.removeEventListener("sprite-garden-toast",this.#t),this.#t=null),this.isTraining.set(!1),this.log("\u{1F9F9} Resources cleaned up","info")}};async function er(){let n=new rt;await I.setItem("sprite-garden-movement-scale",.885),await te(n.shadow),await n.initialize(),n.gThis?.spriteGarden&&(n.gThis.spriteGarden.api={...n.gThis.spriteGarden.api,train:n})}async function tr(n,t){let o=n.navigator;if(o.clipboard&&n.isSecureContext)try{await o.clipboard.writeText(t)}catch{}}function nt(n,t){let o;return function(...e){clearTimeout(o),o=setTimeout(()=>n.apply(this,e),t)}}var st=n=>{let t=n.getElementById("toastContainer");t&&t.querySelectorAll(".toast").forEach(e=>{e.classList.add("toast--slide-out"),setTimeout(()=>e.remove(),300)}),n.removeEventListener("keydown",Ce.get()),le.set(!0)};function or(n,t){return n?.drops??Object.fromEntries(Object.entries(t).map(([o,e])=>[e.id,o]))[n.id]}function at(n,t,o,e,r,i,s,a,l){if(e.blocks&&(Array.isArray(e.blocks)?e.blocks:Object.values(e.blocks)).forEach(m=>{m.x>=0&&m.x<l&&m.y>=0&&m.y<a&&s.setTile(m.x,m.y,i.AIR)}),e.seedType){let c=1+Math.floor(Math.random()*3);j("seedInventory",f=>({...f,[e.seedType]:f[e.seedType]+c}));let m=`Harvested mature ${D(e.seedType)}, gained ${c} seed${c>1?"s":""}.`,p="WOOD",b=i[e.seedType].drops,h=(Array.isArray(b)?b.some(f=>f.toLowerCase().includes(f.toLowerCase())):b.toLowerCase().includes(p.toLowerCase()))?1+Math.floor(Math.random()*3):0;h&&(j("materialsInventory",f=>({...f,[p]:f[p]+h})),m=`${m} Gained ${h} ${D(p)}.`),console.log(m),n.dispatchEvent(new CustomEvent("sprite-garden-toast",{detail:{message:m}}))}let g={...o.get()},u={...t.get()};delete g[r],delete u[r],o.set(g),t.set(u)}function rr(n,t,o){for(let[e,r]of Object.entries(o.get()))if(r.mature&&r.blocks&&(Array.isArray(r.blocks)?r.blocks:Object.values(r.blocks)).find(s=>s.x===n&&s.y===t))return!0;return!1}function On(n,t){return n===t.TREE_TRUNK||n===t.TREE_LEAVES}function Pn(n,t,o,e,r,i,s,a,l,d="regular"){if(R.isEnabled){let m="Breaking disabled in map editor mode";console.log(m),n.dispatchEvent(new CustomEvent("sprite-garden-toast",{detail:{message:m}}));return}let g=Math.floor((e.x+e.width/2)/i),u=Math.floor((e.y+e.height/2)/i),c=[];if(d==="regular")if(e.lastDirection!==0){let m=e.lastDirection>0?1:-1;for(let p=0;p<3;p++){let b=g+m,v=u-p;if(b<0||b>=l||v<0||v>=a)continue;let h=s.getTile(b,v);h&&h!==r.AIR&&h!==r.BEDROCK&&h!==r.LAVA&&h!==r.WATER&&c.push({x:b,y:v,tile:h,priority:p})}}else{let m=g,p=u+1;if(m>=0&&m<l&&p>=0&&p<a){let b=s.getTile(m,p);b&&b!==r.AIR&&b!==r.BEDROCK&&b!==r.LAVA&&b!==r.WATER&&c.push({x:m,y:p,tile:b,priority:1})}}else if(e.lastDirection!==0)for(let p=-1;p<=1;p++)for(let b=-1;b<=1;b++){let v=g+p*(e.lastDirection>0,1),h=u+b;if(v<0||v>=l||h<0||h>=a)continue;let f=s.getTile(v,h);if(f&&f!==r.AIR&&f!==r.BEDROCK&&f!==r.LAVA&&f!==r.WATER&&!rr(v,h,o)){let S=Math.abs(p)===0?1:2-Math.abs(p)/1;c.push({x:v,y:h,tile:f,priority:S})}}else for(let p=-1;p<=1;p++)for(let b=-1;b<=1;b++){let v=g+p,h=u+b;if(v<0||v>=l||h<0||h>=a)continue;let f=s.getTile(v,h);f&&f!==r.AIR&&f!==r.BEDROCK&&f!==r.LAVA&&f!==r.WATER&&c.push({x:v,y:h,tile:f,priority:1})}if(c.length>0){let m=o.get(),p=new Set;for(let y of c)for(let[k,w]of Object.entries(m))if(w.mature&&w.blocks&&(Array.isArray(w.blocks)?w.blocks:Object.values(w.blocks)).find(M=>M.x===y.x&&M.y===y.y)){p.add(k);break}if(p.size>0){for(let y of p){let k=m[y];at(n,t,o,k,y,r,s,a,l)}return}let v={...t.get()},h={...m},f={},S={};c.forEach(y=>{let k=!1,w=null;for(let[L,C]of Object.entries(m)){if(!C.mature&&C.blocks){let M=Array.isArray(C.blocks)?C.blocks:Object.values(C.blocks);for(let N of M)if(N.x===y.x&&N.y===y.y){k=!0,w=L;break}}if(k)break}if(k&&w){let L=m[w];L.blocks&&(Array.isArray(L.blocks)?L.blocks:Object.values(L.blocks)).forEach(M=>{M.x>=0&&M.x<l&&M.y>=0&&M.y<a&&s.setTile(M.x,M.y,r.AIR)}),L.seedType&&Math.random()<.5&&(f[L.seedType]=(f[L.seedType]||0)+1),delete h[w],delete v[w]}else{if(s.setTile(y.x,y.y,r.AIR),delete v[`${y.x},${y.y}`],On(y.tile,r)&&Math.random()<.15&&(f.WALNUT=(f.WALNUT||0)+1),y.tile.isSeed||y.tile.crop&&Math.random()<.3){let M=we(r)[y.tile.id];M&&(f[M]=(f[M]||0)+1)}let L=or(y.tile,r);L&&(y.tile===r.TREE_LEAVES&&Math.random()<.3||y.tile!==r.TREE_LEAVES)&&(S[L]=(S[L]||0)+1)}});let E=c.map(y=>D(je(r,y.tile.id))).join(", ");n.dispatchEvent(new CustomEvent("sprite-garden-toast",{detail:{message:`Collected ${JSON.stringify(E)}`}})),t.set(v),o.set(h),Object.keys(f).length>0&&j("seedInventory",y=>{let k={...y};return Object.entries(f).forEach(([w,L])=>{k[w]=(k[w]||0)+L}),k}),Object.keys(S).length>0&&j("materialsInventory",y=>{let k={...y};return Object.entries(S).forEach(([w,L])=>{r[w]?.isSeed||(k[w]=(k[w]||0)+L)}),k})}}function it(n,t,o,e,r,i,s,a,l,d,g="regular"){let u=e.get(),c=s.get(),m=Math.floor((u.x+u.width/2)/i),p=Math.floor((u.y+u.height/2)/i),b=Pn(n,t,o,u,r,i,c,a,l,g),v=3,h=!1;for(let f=-v;f<=v;f++)for(let S=-v;S<=v;S++){let E=m+f,y=p+S;E>=0&&E<l&&y>=0&&y<a&&c.getTile(E,y)===r.WATER&&(h=!0,tt(E,y,d,l,a,10))}return b}function nr(n,t,o,e,r,i){let s=we(o)[t.id];if(s){let a=2+Math.floor(Math.random()*3);j("seedInventory",d=>({...d,[s]:d[s]+a})),e.setTile(r,i,o.AIR);let l=`Harvested ${D(s)} crop, gained ${a} seeds`;return console.log(l),n.dispatchEvent(new CustomEvent("sprite-garden-toast",{detail:{message:l}})),!0}return!1}function sr(n,t,o,e,r,i,s,a,l){let d=s.getTile(a,l+1);if(!d||!d.farmable){let u=`Cannot plant at (${a}, ${l}) - no farmable ground below`;return console.log(u),n.dispatchEvent(new CustomEvent("sprite-garden-toast",{detail:{message:u}})),!1}let g;if(We[r]&&e[r]>0){s.setTile(a,l,We[r]),j("seedInventory",c=>({...c,[r]:c[r]-1}));let u=`${a},${l}`;return t.set({...t.get(),[u]:{timeLeft:i[r].growthTime,seedType:r}}),o.set({...o.get(),[u]:{seedType:r,mature:!1,blocks:[{x:a,y:l,tile:We[r]}],baseX:a,baseY:l}}),g=`Planted ${D(r)} at (${a}, ${l}), ${e[r]-1} seeds remaining`,console.log(g),n.dispatchEvent(new CustomEvent("sprite-garden-toast",{detail:{message:g}})),!0}return g=`Cannot plant ${D(r)} - no seeds available or invalid seed type`,console.log(g),n.dispatchEvent(new CustomEvent("sprite-garden-toast",{detail:{message:g}})),!1}var ar=0,Fn=500;function Nn(n,t,o){let e=[];if(n.lastDirection!==0){let r=n.lastDirection>0?1:-1;e.push({x:t+r,y:o},{x:t+r,y:o+1})}return e.push({x:t,y:o+1},{x:t,y:o}),e}function Gn(n,t,o,e){return n>=0&&n<o&&t>=0&&t<e}function Hn(n,t,o){let e=n.get();for(let[r,i]of Object.entries(e)){if(!i.mature||!i.blocks)continue;if((Array.isArray(i.blocks)?i.blocks:Object.values(i.blocks)).find(l=>l.x===t&&l.y===o))return{structure:i,key:r}}return null}function qn(n){return n?.crop&&!n.seed}function Wn(n,t,o,e){return n===t.AIR&&o&&e[o]>0}function jn(n,t){let{shadow:o,growthTimers:e,plantStructures:r,tiles:i,world:s,worldHeight:a,worldWidth:l}=n;return at(o,e,r,t.structure,t.key,i,s,a,l),!0}function $n(n,t,o,e){let{shadow:r,tiles:i,world:s}=n,a=nr(r,t,i,s,o,e);return a||ir(r,s,o,e,"harvest"),a}function Un(n,t,o){let{shadow:e,growthTimers:r,plantStructures:i,seedInventory:s,selectedSeedType:a,tiles:l,world:d}=n;return sr(e,r,i,s,a,l,d,t,o)}function Kn(n,t){let{plantStructures:o,tiles:e,selectedSeedType:r,seedInventory:i,world:s,worldWidth:a,worldHeight:l}=n,{x:d,y:g}=t;if(!Gn(d,g,a,l))return!1;let u=s.getTile(d,g),c=Hn(o,d,g);return c?jn(n,c):Wn(u,e,r,i)?Un(n,d,g):qn(u)?$n(n,u,d,g):!1}function ir(n,t,o,e,r){let i=D(je(ao,t.getTile(o,e).id)),s=`Cannot ${r} ${i} at (${o}, ${e})`;n.dispatchEvent(new CustomEvent("sprite-garden-toast",{detail:{message:s}}))}function lt(n,t,o,e,r,i,s,a,l,d,g){let u=Math.floor((e.x+e.width/2)/a),c=Math.floor((e.y+e.height/2)/a),m=Nn(e,u,c),p={shadow:n,growthTimers:t,plantStructures:o,seedInventory:r,selectedSeedType:i,tiles:s,world:l,worldHeight:d,worldWidth:g};m.some(v=>Kn(p,v))?ar=Date.now():Date.now()-ar>Fn&&ir(n,l,u,c,"farm")}function lr(n,t){return t[n]||null}async function Pt(n){let t=await I.getItem("sprite-garden-range")||1;n.querySelector('[data-key="k"].middle').innerHTML=`&times;${Number(t)}`}async function cr(n){let t=Number(await I.getItem("sprite-garden-range")||1),o=Number(t+1);t>=3&&(o=1),await I.setItem("sprite-garden-range",o),await Pt(n)}async function Ge(n,t,o,e,r,i,s,a,l,d){let g=Math.floor((e.x+e.width/2)/s),u=Math.floor((e.y+e.height/2)/s),c,m,p=await I.getItem("sprite-garden-range")||1;switch(t.toLowerCase()){case"k":await cr(n);return;case"u":p===1&&(c=g-p,m=u-p),p===2&&(c=g-1,m=u-p),p===3&&(c=g-1,m=u-p);break;case"i":p===1&&(c=g,m=u-p),p===2&&(c=g,m=u-p),p===3&&(c=g,m=u-p);break;case"o":p===1&&(c=g+p,m=u-p),p===2&&(c=g+1,m=u-p),p===3&&(c=g+1,m=u-p);break;case"j":p===1&&(c=g-p,m=u),p===2&&(c=g-p,m=u),p===3&&(c=g-p,m=u);break;case"l":p===1&&(c=g+p,m=u),p===2&&(c=g+p,m=u),p===3&&(c=g+p,m=u);break;case"m":p===1&&(c=g-p,m=u+p),p===2&&(c=g-1,m=u+p),p===3&&(c=g-1,m=u+p);break;case",":p===1&&(c=g,m=u+p),p===2&&(c=g,m=u+p),p===3&&(c=g,m=u+p);break;case".":p===1&&(c=g+p,m=u+p),p===2&&(c=g+1,m=u+p),p===3&&(c=g+1,m=u+p);break;default:let f=`Invalid placement key: ${t}`;console.log(f),n.dispatchEvent(new CustomEvent("sprite-garden-toast",{detail:{message:f}}));return}if(!r){let f="No material selected for placement";console.log(f),n.dispatchEvent(new CustomEvent("sprite-garden-toast",{detail:{message:f}}));return}if(o[r]<=0){let f=`No ${D(r)} available to place`;console.log(f),n.dispatchEvent(new CustomEvent("sprite-garden-toast",{detail:{message:f}}));return}if(c<0||c>=d||m<0||m>=l){let f=`Cannot place block outside world bounds at (${c}, ${m})`;console.log(f),n.dispatchEvent(new CustomEvent("sprite-garden-toast",{detail:{message:f}}));return}let b=a.getTile(c,m);if(b&&b!==i.AIR&&b.solid){let f=`Cannot place block at (${c}, ${m}) - position occupied`;console.log(f),n.dispatchEvent(new CustomEvent("sprite-garden-toast",{detail:{message:f}}));return}let v=lr(r,i);if(!v){let f=`Invalid material type: ${D(r)}`;console.log(f),n.dispatchEvent(new CustomEvent("sprite-garden-toast",{detail:{message:f}}));return}a.setTile(c,m,v),j("materialsInventory",f=>({...f,[r]:f[r]-1}));let h=`Placed ${D(r)} at (${c}, ${m}), ${o[r]-1} remaining.`;console.log(h),n.dispatchEvent(new CustomEvent("sprite-garden-toast",{detail:{message:h}}))}function dr(n,t,o){if(!(o.currentTarget instanceof HTMLElement))throw new Error("currentTarget is not an HTMLElement");let[e]=Object.keys(o.currentTarget.dataset);for(let a of o.currentTarget.parentElement.children)a.classList.remove("selected");o.currentTarget.classList.toggle("selected"),console.log(`Selecting material: ${e}`);let r=t.selectedMaterialType.get();console.log(`Current selected material: ${r}`);let i=r===e.toUpperCase()?null:e.toUpperCase();t.selectedMaterialType.set(i);let s;i?s=`New selected material: ${D(i)}`:s="No selected material.",console.log(s),n.dispatchEvent(new CustomEvent("sprite-garden-toast",{detail:{message:s}}))}function He(n,t){for(let[o,e]of Object.entries(t))n.host instanceof HTMLElement&&n.host.style.setProperty(o,e)}async function ur(n,t,o){try{await I.removeItem(o);let e=X(n,t);for(let r of Object.keys(e))t.host instanceof HTMLElement&&t.host.style.removeProperty(r);console.log("Reset to default colors")}catch(e){console.error("Failed to reset colors:",e)}}async function gr(n,t){try{await I.setItem(t,n),console.log("Saved custom colors")}catch(o){console.error("Failed to save colors:",o)}}var ct="sprite-garden-custom-colors",dt=class{constructor(t,o,e){this.gThis=t,this.doc=o,this.shadow=e,this.dialog=null,this.colors={},this.originalColors={},this.close=this.close.bind(this),this.handleColorChange=this.handleColorChange.bind(this),this.handleDialogClose=this.handleDialogClose.bind(this),this.handleDialogKeydown=this.handleDialogKeydown.bind(this),this.handleReset=this.handleReset.bind(this),this.handleSave=this.handleSave.bind(this),this.dirty=!1}async createDialog(){this.dialog&&this.dialog.remove(),this.originalColors=X(this.gThis,this.shadow),this.colors={...this.originalColors};let t=this.doc.createElement("dialog");return t.setAttribute("id","customizeColorsDialog"),t.addEventListener("close",this.handleDialogClose),t.addEventListener("keydown",this.handleDialogKeydown),t.style.cssText=`
      background: var(--sg-color-gray-50);
      border-radius: 0.5rem;
      border: 0.125rem solid var(--sg-color-gray-900);
      color: var(--sg-color-gray-900);
      font-family: monospace;
      max-height: 80vh;
      max-width: 30rem;
      overflow-y: auto;
      padding: 1.25rem;
      width: 90%;
    `,t.innerHTML=`
      <div style="
        align-items: center;
        display: flex;
        justify-content: space-between;
        margin-bottom: 0.9375rem;
      ">
        <h3 style="margin: 0">Customize Colors</h3>
        <button
          id="closeColorDialog"
          style="
            background: var(--sg-color-red-500);
            border-radius: 0.25rem;
            border: none;
            color: var(--sg-color-white);
            cursor: pointer;
            font-size: 1.2rem;
            padding: 0.5rem 1rem;
          "
        >
          &times;
        </button>
      </div>

      <div style="margin-bottom: 1rem;">
        <p style="color: var(--sg-color-gray-700); font-size: 0.875rem; margin: 0 0 0.5rem 0;">
          Customize the color palette for Sprite Garden. Changes are applied in real-time and saved automatically.
        </p>
      </div>

      <div id="colorInputsContainer" style="
        display: grid;
        grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
        gap: 1rem;
        margin-bottom: 1rem;
      ">
        <!-- Color inputs will be generated here -->
      </div>

      <div style="border-top: 1px solid var(--sg-color-gray-300); display: flex; gap: 0.625rem; justify-content: flex-end; padding-top: 1rem;">
        <button
          id="resetColorsBtn"
          style="
            background: var(--sg-color-orange-500);
            border-radius: 0.25rem;
            border: none;
            color: white;
            cursor: pointer;
            padding: 0.5rem 0.9375rem;
          "
        >
          Reset to Defaults
        </button>
        <button
          id="saveColorsBtn"
          style="
            background: var(--sg-color-green-500);
            border-radius: 0.25rem;
            border: none;
            color: white;
            cursor: pointer;
            padding: 0.5rem 0.9375rem;
          "
        >
          Save & Close
        </button>
      </div>
    `,this.shadow.append(t),this.dialog=t,this.renderColorInputs(),this.initEventListeners(),t}async handleSave(){await gr(this.colors,ct),await He(this.shadow,this.colors),this.shadow.dispatchEvent(new CustomEvent("sprite-garden-reset",{detail:{colors:this.colors}})),this.dirty=!1,this.close()}async handleReset(){confirm("Reset all colors to defaults and close? This cannot be undone.")&&(await ur(this.gThis,this.shadow,ct),this.shadow.dispatchEvent(new CustomEvent("sprite-garden-reset",{detail:{colors:null}})),this.dirty=!1,this.close())}renderColorInputs(){let t=this.dialog.querySelector("#colorInputsContainer"),o={};for(let[r,i]of Object.entries(this.colors)){let s=r.match(/--sg-?([a-z]+)-/),a=s?s[1]:"other";["host","touch","ui"].includes(a)||(o[a]||(o[a]=[]),o[a].push({property:r,value:i.slice(0,7)}))}let e=Object.keys(o).sort();for(let r of e){let i=this.doc.createElement("div");i.style.cssText=`
        grid-column: 1 / -1;
        margin-top: 1rem;
      `;let s=this.doc.createElement("h4");s.textContent=(r.charAt(0).toUpperCase()+r.slice(1)).replace("Tile","Tiles"),s.style.cssText=`
        border-bottom: 1px solid var(--sg-color-gray-300);
        color: var(--sg-color-gray-800);
        font-size: 1rem;
        margin: 0 0 0.5rem 0;
        padding-bottom: 0.25rem;
      `,i.append(s);let[a]=o[r];a.property.startsWith("--sg-color")&&i.setAttribute("hidden","hidden"),t.append(i);for(let{property:l,value:d}of o[r]){let g=this.doc.createElement("div");g.style.cssText=`
          display: flex;
          flex-direction: column;
          gap: 0.25rem;
        `,l.startsWith("--sg-color")&&g.setAttribute("hidden","hidden");let u=this.doc.createElement("label");u.textContent=l.replace("--sg-","").replace(/-/g," ").replace("tile ","").replace("color ","").replace(" color",""),u.style.cssText=`
          color: var(--sg-color-gray-700);
          font-size: 0.75rem;
          text-transform: capitalize;
        `;let c=this.doc.createElement("div");c.style.cssText=`
          align-items: center;
          display: flex;
          gap: 0.5rem;
        `;let m=this.doc.createElement("input");m.type="color",m.value=this.normalizeColor(d),m.dataset.property=l,m.style.cssText=`
          border-radius: 0.25rem;
          border: 1px solid var(--sg-color-gray-400);
          cursor: pointer;
          height: 2rem;
          width: 3rem;
        `;let p=this.doc.createElement("input");p.type="text",p.value=d,p.dataset.property=l,p.style.cssText=`
          border-radius: 0.25rem;
          border: 1px solid var(--sg-color-gray-400);
          flex: 1;
          font-family: monospace;
          font-size: 0.75rem;
          padding: 0.25rem;
        `;let b=nt(()=>{this.handleColorChange(l,p.value)},500);m.addEventListener("input",v=>{v.target instanceof HTMLInputElement&&(p.value=v.target.value,b())}),p.addEventListener("input",v=>{if(v.target instanceof HTMLInputElement){let h=this.normalizeColor(v.target.value);h&&(m.value=h),b()}}),c.append(m),c.append(p),g.append(u),g.append(c),t.append(g)}}}normalizeColor(t){if(!t)return"#000000";if(t.startsWith("#"))return t.length===7?t:"#000000";let e=this.doc.createElement("canvas").getContext("2d");return e.fillStyle=t,e.fillStyle}handleColorChange(t,o){this.colors[t]=o,this.dirty=!0;let e=this.shadow.host;e instanceof HTMLElement&&(e.style.setProperty(t,o),this.shadow.dispatchEvent(new CustomEvent("sprite-garden-reset",{detail:{colors:this.colors}})))}initEventListeners(){let t=this.dialog.querySelector("#closeColorDialog"),o=this.dialog.querySelector("#saveColorsBtn"),e=this.dialog.querySelector("#resetColorsBtn");t.addEventListener("click",this.close),o.addEventListener("click",this.handleSave),e.addEventListener("click",this.handleReset)}show(){this.dialog instanceof HTMLDialogElement&&this.dialog.showModal()}handleDialogKeydown(t){t.key==="Escape"&&(t.preventDefault(),this.close())}close(){if(this.dirty){confirm("Close without saving?")&&(He(this.shadow,this.originalColors),requestAnimationFrame(()=>{this.shadow.dispatchEvent(new CustomEvent("sprite-garden-reset",{detail:{colors:this.originalColors}}))}),this.dialog instanceof HTMLDialogElement&&this.dialog.close());return}this.dialog instanceof HTMLDialogElement&&this.dialog.close()}handleDialogClose(){this.removeEventListeners(),this.dialog.remove()}removeEventListeners(){let t=this.dialog.querySelector("#closeColorDialog"),o=this.dialog.querySelector("#saveColorsBtn"),e=this.dialog.querySelector("#resetColorsBtn");t.removeEventListener("click",this.close),o.removeEventListener("click",this.handleSave),e.removeEventListener("click",this.handleReset)}};async function Ft(n){let t=ye(n.document,"sprite-garden"),o=new dt(n,n.document,t);return await o.createDialog(),o.show(),o}function Nt(){let n=T.breakMode.get();T.breakMode.set(n==="regular"?"extra":"regular")}var Gt=class{constructor(t,o){this.doc=t,this.shadow=o,this.dialog=null,this.close=this.close.bind(this),this.handleDialogClick=this.handleDialogClick.bind(this)}async createDialog(t){this.dialog&&this.dialog.remove();let o=`${t}-content`,e=this.shadow.querySelector(`.${o}`);if(!e){e=this.doc.createElement("dialog"),e.setAttribute("class",o);let r=new DOMParser,i=await(await fetch(`${t}.html`)).text(),a=r.parseFromString(i,"text/html").querySelector(`.${o}`);e.innerHTML=a.innerHTML,this.shadow.append(e),this.shadow.querySelector(`.${t}-content_close-btn`).removeAttribute("hidden")}return this.dialog=e,this.initEventListeners(),e}handleDialogClick(t){t.target===this.dialog&&this.close()}initEventListeners(){this.dialog.querySelector(".examples-content_close-btn").addEventListener("click",this.close),this.dialog.addEventListener("click",this.handleDialogClick)}removeEventListeners(){this.dialog.querySelector(".examples-content_close-btn").removeEventListener("click",this.close),this.dialog.removeEventListener("click",this.handleDialogClick)}show(){this.dialog instanceof HTMLDialogElement&&this.dialog.showModal()}close(){this.removeEventListeners(),this.dialog instanceof HTMLDialogElement&&this.dialog.close()}};async function mr(n,t){let o=new Gt(n,t);return await o.createDialog("examples"),o.show(),o}var Ht=class{constructor(t,o){this.doc=t,this.shadow=o,this.dialog=null,this.close=this.close.bind(this),this.handleDialogClick=this.handleDialogClick.bind(this)}async createDialog(t){this.dialog&&this.dialog.remove();let o=`${t}-content`,e=this.shadow.querySelector(`.${o}`);if(!e){e=this.doc.createElement("dialog"),e.setAttribute("class",o);let r=new DOMParser,i=await(await fetch(`${t}.html`)).text(),a=r.parseFromString(i,"text/html").querySelector(`.${o}`);e.innerHTML=a.innerHTML,this.shadow.append(e),this.shadow.querySelector(`.${t}-content_close-btn`).removeAttribute("hidden")}return this.dialog=e,this.initEventListeners(),e}handleDialogClick(t){t.target===this.dialog&&this.close()}initEventListeners(){this.dialog.querySelector(".privacy-content_close-btn").addEventListener("click",this.close),this.dialog.addEventListener("click",this.handleDialogClick)}removeEventListeners(){this.dialog.querySelector(".privacy-content_close-btn").removeEventListener("click",this.close),this.dialog.removeEventListener("click",this.handleDialogClick)}show(){this.dialog instanceof HTMLDialogElement&&this.dialog.showModal()}close(){this.removeEventListeners(),this.dialog instanceof HTMLDialogElement&&this.dialog.close()}};async function pr(n,t){let o=new Ht(n,t);return await o.createDialog("privacy"),o.show(),o}function qt(n){!n||!n.parentElement||(n.classList.add("toast--slide-out"),setTimeout(()=>n.remove(),300))}function hr(n,t,o={}){let{duration:e=3e3,manualClose:r=!1,stack:i=!1,bottomOffset:s=1,useSingle:a=!0}=o,l=n.getElementById("toastContainer");if(!l){console.warn("Toast container not found");return}l.style.bottom=`${s}rem`;let d;if(a&&(d=l.querySelector(".toast"),d)){let u=d.querySelector(".toast__content");if(u&&(u.textContent=t),e>0){let c=d;clearTimeout(c.autoRemoveTimer),c.autoRemoveTimer=setTimeout(()=>qt(d),e)}return}!i&&!a&&l.querySelectorAll(".toast").forEach(c=>{c.classList.add("toast--fade-out"),setTimeout(()=>c.remove(),300)}),d=n.ownerDocument.createElement("div"),d.className="toast";let g=n.ownerDocument.createElement("div");if(g.className="toast__content",g.textContent=t,d.appendChild(g),r){let u=n.ownerDocument.createElement("button");u.className="toast__close-btn",u.innerHTML="&times;",u.addEventListener("click",()=>qt(d)),d.appendChild(u)}if(l.appendChild(d),e>0){let u=d;u.autoRemoveTimer=setTimeout(()=>qt(d),e)}}var Wt=class{constructor(t,o){this.doc=t,this.shadow=o,this.dialog=null,this.close=this.close.bind(this),this.handleDialogClick=this.handleDialogClick.bind(this)}async createDialog(t){this.dialog&&this.dialog.remove();let o=`${t}-content`,e=this.shadow.querySelector(`.${o}`);if(!e){e=this.doc.createElement("dialog"),e.setAttribute("class",o);let r=new DOMParser,i=await(await fetch(`${t}.html`)).text(),a=r.parseFromString(i,"text/html").querySelector(`.${o}`);e.innerHTML=a.innerHTML,this.shadow.append(e),this.shadow.querySelector(`.${t}-content_close-btn`).removeAttribute("hidden")}if(!(e instanceof HTMLDialogElement))throw new Error("Failed to create or find HTMLDialogElement");return this.dialog=e,this.initEventListeners(),this.dialog}handleDialogClick(t){t.target===this.dialog&&this.close()}initEventListeners(){this.dialog.querySelector(".about-content_close-btn").addEventListener("click",this.close),this.dialog.addEventListener("click",this.handleDialogClick)}removeEventListeners(){this.dialog.querySelector(".about-content_close-btn").removeEventListener("click",this.close),this.dialog.removeEventListener("click",this.handleDialogClick)}show(){this.dialog instanceof HTMLDialogElement&&this.dialog.showModal()}close(){this.removeEventListeners(),this.dialog instanceof HTMLDialogElement&&this.dialog.close()}};async function fr(n,t){let o=new Wt(n,t);return await o.createDialog("about"),o.show(),o}function vr(n,t){let o=nt(()=>{se(t,T)},200);new ResizeObserver(s=>{o()}).observe(t.host),Yn(n,t),zn(t),Pt(t),_t(t);let r=t.getElementById("closeWorldGeneration");r&&r.addEventListener("click",()=>{t.querySelector('[class="seed-controls"]').setAttribute("hidden","hidden")});let i=t.getElementById("customizeColorsBtn");if(i){let s=n.spriteGarden.config;i.addEventListener("click",async()=>{let a=s.currentResolution.get();if(a==="400"){s.currentResolution.set("800"),se(t,s),(await Ft(n)).dialog.addEventListener("close",()=>{s.currentResolution.set(a),se(t,s)});return}await Ft(n)})}}function zn(n){let t=n.querySelector('[data-key="x"].middle');t&&(t.addEventListener("click",async()=>await te(n)),t.addEventListener("touchstart",async()=>await te(n)))}function Yn(n,t){let o=t.getElementById("aboutBtn");o&&o.addEventListener("click",async function(){try{await fr(n.document,t)}catch(i){console.error("Failed to open about dialog:",i),alert("Failed to open about dialog. Check console for details.")}});let e=t.getElementById("examplesBtn");e&&e.addEventListener("click",async function(){try{await mr(n.document,t)}catch(i){console.error("Failed to open examples dialog:",i),alert("Failed to open examples dialog. Check console for details.")}});let r=t.getElementById("privacyBtn");r&&r.addEventListener("click",async function(){try{await pr(n.document,t)}catch(i){console.error("Failed to open privacy dialog:",i),alert("Failed to open privacy dialog. Check console for details.")}})}function Vn(n){n.preventDefault(),n.stopPropagation();let t=n.currentTarget;if(t instanceof HTMLDivElement){let o=t.nextElementSibling,e=o.hasAttribute("hidden");o.toggleAttribute("hidden",!e),t.parentElement.id==="controls"&&t.getRootNode().querySelector(".touch-controls").toggleAttribute("hidden",!e)}}function yr(n,t){new br.default(h=>{t.getElementById("mapEditor").removeAttribute("hidden"),t.getElementById("customizeColorsBtnContainer").removeAttribute("hidden"),t.getElementById("examplesBtnContainer").removeAttribute("hidden"),t.querySelector('sprite-garden-option[value="fullscreen"]').removeAttribute("hidden");let f=t.getElementById("customizeColorsDialog");f&&f.querySelectorAll("[hidden]").forEach(y=>y.removeAttribute("hidden")),t.querySelector('#settings > [class="ui-grid__corner--container"]').removeAttribute("hidden"),t.querySelector("#trainPanel").removeAttribute("hidden"),er(),$e.set(!0),h.disable()}),t.addEventListener("sprite-garden-toast",h=>{h instanceof CustomEvent&&hr(t,h.detail.message,h.detail.config)}),Ce.set(h=>{if(h instanceof KeyboardEvent&&!le.get()){let f=h.key.toLowerCase();(f==="w"||f==="arrowup"||f===" ")&&(st(t),le.set(!0))}}),t.addEventListener("keydown",Ce.get()),t.addEventListener("keydown",async h=>{let f=h.key.toLowerCase(),S=t.host;if(S.keys[f]=!0,f==="enter"&&h.target instanceof HTMLInputElement&&h.target.getAttribute("id")==="worldSeedInput"&&i(),f==="escape"&&t.querySelector('[class="seed-controls"]').setAttribute("hidden","hidden"),f==="s"&&h.ctrlKey&&(h.preventDefault(),t.querySelector('[class="seed-controls"]').toggleAttribute("hidden")),f>="0"&&f<="9"||f==="backspace"||f==="delete"||f==="escape")return;f==="r"&&h.ctrlKey&&(h.preventDefault(),s()),f==="g"&&h.ctrlKey&&(h.preventDefault(),i()),f==="e"&&(h.preventDefault(),Nt()),f==="x"&&(h.preventDefault(),await te(t)),f==="f"&&lt(t,x.growthTimers,x.plantStructures,x.player.get(),x.seedInventory.get(),x.selectedSeedType.get(),T.TILES,T.TILE_SIZE.get(),x.world.get(),T.WORLD_HEIGHT.get(),T.WORLD_WIDTH.get()),f==="r"&&it(t,x.growthTimers,x.plantStructures,x.player,T.TILES,T.TILE_SIZE.get(),x.world,T.WORLD_HEIGHT.get(),T.WORLD_WIDTH.get(),x.waterPhysicsQueue,T.breakMode.get()),["u","i","o","j","k","l","m",",","."].includes(f)&&await Ge(t,f,x.materialsInventory.get(),x.player.get(),x.selectedMaterialType.get(),T.TILES,T.TILE_SIZE.get(),x.world.get(),T.WORLD_HEIGHT.get(),T.WORLD_WIDTH.get()),h.preventDefault()}),t.getElementById("toggleFog").addEventListener("click",function(){if(t.getElementById("fogModeText").textContent==="Clear"){T.fogMode.set("fog");return}T.fogMode.set("clear")});function e(){t.querySelector('[class="seed-controls"]').toggleAttribute("hidden")}t.getElementById("worldState").addEventListener("click",e);function i(){let h=null,f=t.getElementById("worldSeedInput");f instanceof HTMLInputElement&&(h=f.value);let S=t.getElementById("currentSeed"),E=T.WORLD_HEIGHT.get(),y=T.WORLD_WIDTH.get(),k=ge(T.BIOMES,T.SURFACE_LEVEL.get(),T.TILE_SIZE.get(),T.TILES,E,y,T.worldSeed,x.gameTime,x.growthTimers,x.plantStructures,x.player,x.materialsInventory,x.seedInventory,Number(h));x.world.set(k);let w=X(n,t),L=Ae(T.isFogScaled,E,y,w);x.exploredMap.set(L),console.log(`Generated new world with seed: ${h}`),S.textContent=h}function s(){let h=t.getElementById("currentSeed"),f=t.getElementById("worldSeedInput"),S=Q(),E=T.WORLD_HEIGHT.get(),y=T.WORLD_WIDTH.get(),k=ge(T.BIOMES,T.SURFACE_LEVEL.get(),T.TILE_SIZE.get(),T.TILES,E,y,T.worldSeed,x.gameTime,x.growthTimers,x.plantStructures,x.player,x.materialsInventory,x.seedInventory,S);x.world.set(k);let w=X(n,t),L=Ae(T.isFogScaled,E,y,w);x.exploredMap.set(L),console.log(`Generated new world with random seed: ${S}`),f instanceof HTMLInputElement&&(f.value=String(S)),h.textContent=String(S)}t.getElementById("generateWithSeed").addEventListener("click",i),t.getElementById("randomSeed").addEventListener("click",s),t.getElementById("copySeed").addEventListener("click",async function(){let h=t.getElementById("worldSeedInput");h instanceof HTMLInputElement&&await tr(n,h.value)});let g=t.getElementById("saveModeToggle");Se().then(async h=>{let f=h==="auto"?"auto":"manual";if(console.log("Save Mode:",f),f==="auto"){g.innerText="Avtomatik saqlash",g.style.backgroundColor="var(--sg-color-green-500)";return}g.innerText="Qo\u2018lda saqlash",g.style.backgroundColor="var(--sg-color-red-500)"}),g.addEventListener("click",async function(){let f=await Se()==="auto"?"auto":"manual";if(f==="manual"){g.innerText="Avtomatik saqlash",g.style.backgroundColor="var(--sg-color-green-500)",await kt("auto"),await Ee(n);return}f==="auto"&&(g.innerText="Qo\u2018lda saqlash",g.style.backgroundColor="var(--sg-color-red-500)",await kt("manual"))}),t.getElementById("saveExternalGameFile").addEventListener("click",async function(){try{let h=ce(n),f=JSON.stringify(h);await ho(n,f),console.log("Game state saved successfully")}catch(h){console.error("Failed to save game state:",h),alert("Failed to save game state. Check console for details.")}}),t.getElementById("loadExternalGameFile").addEventListener("click",async function(){try{let h=t.getElementById("currentSeed"),f=t.getElementById("worldSeedInput"),S;if(n.top===n&&n.showOpenFilePicker){let[w]=await n.showOpenFilePicker({types:[{description:"Sprite Garden Save Game Files",accept:{"application/*":[".sgs"],"application/pdf":[".pdf"],"text/plain":[".txt"]}}]});S=await w.getFile()}else{let w=n.document.createElement("input");w.type="file",w.accept=".sgs,.pdf,.txt,text/plain,application/pdf,application/gzip,application/*",w.style.display="none",t.append(w);let L=new Promise(C=>{w.onchange=()=>C(w.files[0])});w.click(),S=await L,t.removeChild(w)}let E="{}";if(S.name.endsWith(".txt")&&(E=(await S.text()).replace(/\s+/g,"")),S.name.endsWith(".pdf")){let[w]=await de(S);E=await ue(new Blob([w.data]))}if(S.name.endsWith(".sgs")){let w=S.stream().pipeThrough(new n.DecompressionStream("gzip"));E=await(await new n.Response(w).blob()).text()}let y;try{y=JSON.parse(E)}catch{throw new Error("Invalid game state file: not valid JSON.")}if(!y.config||!y.state)throw new Error("Invalid game state file: missing required config or state.");await ve(n,t,y);let{worldSeed:k}=y.config;f instanceof HTMLInputElement&&(f.value=k),h.textContent=k,console.log("Game state loaded successfully")}catch(h){console.error("Failed to load game state:",h),alert("Failed to load game state. Check console for details.")}});let m=!1,p=t.getElementById("shareExternalGameFile");if(typeof navigator<"u"&&typeof navigator.canShare<"u")try{m=navigator.canShare({files:[new File([],"test")]})}catch(h){console.info(`File sharing is not enabled. ${JSON.stringify(h)}`)}m&&(t.querySelectorAll(".seed-controls--share").forEach(h=>h.removeAttribute("hidden")),p.addEventListener("click",async function(){try{let h;if(n.top===n&&n.showOpenFilePicker){let[E]=await n.showOpenFilePicker({types:[{description:"Sprite Garden Save Game Files",accept:{"application/*":[".sgs"],"application/pdf":[".pdf"],"text/plain":[".txt"]}}]});h=await E.getFile()}else{let E=n.document.createElement("input");E.type="file",E.accept=".sgs,.pdf,.txt,text/plain,application/pdf,application/gzip,application/*",E.style.display="none",t.append(E);let y=new Promise(k=>{E.onchange=()=>k(E.files[0])});E.click(),h=await y,t.removeChild(E)}let f="{}";if(h.name.endsWith(".txt")&&(f=(await h.text()).replace(/\s+/g,"")),h.name.endsWith(".pdf")){let[E]=await de(h);f=await ue(new Blob([E.data]))}if(h.name.endsWith(".sgs")){let E=h.stream().pipeThrough(new n.DecompressionStream("gzip"));f=await(await new n.Response(E).blob()).text()}let S;try{S=JSON.parse(f)}catch{throw new Error("Invalid game state file: not valid JSON.")}if(!S.config||!S.state)throw new Error("Invalid game state file: missing required config or state.");h=new File([f],`${h.name}.json.txt`,{type:"text/plain",lastModified:Date.now()}),typeof navigator<"u"&&typeof navigator.canShare<"u"&&navigator.canShare({files:[h]})?(await navigator.share({files:[h],title:"Sprite Garden Game Save",text:`Visit Sprite Garden, then 'Load' and checkout my world: ${h.name}

`,url:"https://kherrick.github.io/sprite-garden"}),console.log("Game state file shared successfully")):alert("Web Share API is not available on this device or browser.")}catch(h){h.name!=="AbortError"?(console.error("Failed to share game state file:",h),alert("Failed to share game state file. Check console for details.")):console.log("Game state file sharing was cancelled by the user")}}));let b=t.getElementById("openStorageBtn");b&&b.addEventListener("click",async function(){try{await yo(n,n.document,t)}catch(h){console.error("Failed to open storage dialog:",h),alert("Failed to open storage dialog. Check console for details.")}}),t.querySelectorAll(".ui-grid__corner").forEach(h=>{h.querySelector(".ui-grid__corner--heading").addEventListener("click",S=>Vn(S))})}function Sr(n,t){let o=t.getElementById("resolutionSelect");o&&o.addEventListener("change",l=>{T.currentResolution.set(l.detail.value),se(t,T)});let e=t.getElementById("toggleView");e&&e.addEventListener("click",()=>x.viewMode.set(x.viewMode.get()==="normal"?"xray":"normal"));let r=t.getElementById("toggleBreakMode");r&&r.addEventListener("click",()=>Nt()),t.querySelectorAll(".seed-btn").forEach(l=>{l.addEventListener("click",d=>To(t,x,d))}),t.querySelectorAll(".material-btn").forEach(l=>{l.addEventListener("click",d=>dr(t,x,d))});let a=t.getElementById("resolutionSelect");a instanceof HTMLSelectElement&&(a.value="400")}function Er(n){let t=n.host;n.querySelectorAll(".touch-btn").forEach(e=>{let r=e.getAttribute("data-key"),i=!1,s=null;function a(){t.touchKeys[r]=!0,e instanceof HTMLButtonElement&&(e.style.background="var(--sg-color-gray-alpha-30)"),!le.get()&&(r==="r"||r==="w"||r==="upright"||r=="upleft")&&st(n),r==="f"&&lt(n,x.growthTimers,x.plantStructures,x.player.get(),x.seedInventory.get(),x.selectedSeedType.get(),T.TILES,T.TILE_SIZE.get(),x.world.get(),T.WORLD_HEIGHT.get(),T.WORLD_WIDTH.get()),r==="r"&&it(n,x.growthTimers,x.plantStructures,x.player,T.TILES,T.TILE_SIZE.get(),x.world,T.WORLD_HEIGHT.get(),T.WORLD_WIDTH.get(),x.waterPhysicsQueue,T.breakMode.get())}function l(){i||(i=!0,a(),(r==="f"||r==="r")&&(s=setInterval(a,100)))}function d(){i=!1,t.touchKeys[r]=!1,e instanceof HTMLButtonElement&&(e.style.background="var(--sg-ui-touch-btn-background-color)"),s&&(clearInterval(s),s=null)}e.addEventListener("touchstart",g=>{g.preventDefault(),g.stopPropagation(),l()}),e.addEventListener("touchend",g=>{g.preventDefault(),g.stopPropagation(),d()}),e.addEventListener("touchcancel",g=>{g.preventDefault(),g.stopPropagation(),d()}),e.addEventListener("mousedown",g=>{g.preventDefault(),g.stopPropagation(),l()}),e.addEventListener("mouseup",g=>{g.preventDefault(),g.stopPropagation(),d()}),e.addEventListener("mouseleave",g=>{g.preventDefault(),g.stopPropagation(),d()})}),n.querySelectorAll(".touch-btn.place-block").forEach(e=>{e.addEventListener("touchstart",async()=>await Ge(n,e.dataset.key,x.materialsInventory.get(),x.player.get(),x.selectedMaterialType.get(),T.TILES,T.TILE_SIZE.get(),x.world.get(),T.WORLD_HEIGHT.get(),T.WORLD_WIDTH.get())),e.addEventListener("click",async()=>await Ge(n,e.dataset.key,x.materialsInventory.get(),x.player.get(),x.selectedMaterialType.get(),T.TILES,T.TILE_SIZE.get(),x.world.get(),T.WORLD_HEIGHT.get(),T.WORLD_WIDTH.get()))}),n.addEventListener("keyup",e=>{t.keys[e.key.toLowerCase()]=!1,e.preventDefault()}),n.addEventListener("touchstart",e=>{(e.target instanceof HTMLDivElement&&e.target.closest(".touch-controls")||e.target===n.getElementById("canvas"))&&e.preventDefault()},{passive:!1}),n.addEventListener("touchmove",e=>{(e.target instanceof HTMLDivElement&&e.target.closest(".touch-controls")||e.target===n.getElementById("canvas"))&&e.preventDefault()},{passive:!1}),n.addEventListener("touchend",e=>{(e.target instanceof HTMLDivElement&&e.target.closest(".touch-controls")||e.target===n.getElementById("canvas"))&&e.preventDefault()},{passive:!1}),n.addEventListener("contextmenu",e=>{(e.target instanceof HTMLDivElement&&e.target.closest(".touch-controls")||e.target===n.getElementById("canvas"))&&e.preventDefault()}),n.addEventListener("dblclick",e=>{(e.target instanceof HTMLDivElement&&e.target.closest(".touch-controls")||e.target===n.getElementById("canvas"))&&e.preventDefault()})}function Xn(n,t,o){let e,r;if(n instanceof MouseEvent)e=n.clientX,r=n.clientY;else if(n.touches&&n.touches.length>0)e=n.touches[0].clientX,r=n.touches[0].clientY;else return;if(e===void 0||r===void 0||!isFinite(e)||!isFinite(r)||!isFinite(o)||o<=0)return;let i=t.getBoundingClientRect(),s=t.width/i.width*o,a=t.height/i.height*o;if(!(!isFinite(s)||!isFinite(a)))return{x:(e-i.left)*s,y:(r-i.top)*a}}function jt(n,t,o,e,r,i,s,a,l){let d=Xn(n,t,e),g=o.get(),u=Math.floor((d.x+g.x)/i),c=Math.floor((d.y+g.y)/i);if(u>=0&&u<l&&c>=0&&c<a){let m=s.getTile?s.getTile(u,c):null;if(!m||m===r.AIR){t.title=`Tile: AIR (${u}, ${c})`;return}let p=Object.keys(r).find(b=>r[b]===m)||"Custom";t.title=`Tile: ${p} (${u}, ${c})`}}function Jn(n,t,o,e,r,i,s){if(!(n.target instanceof HTMLCanvasElement))return;let l=n.target.getBoundingClientRect(),d=n.clientX-l.left,g=n.clientY-l.top;if(Bt(d,g,t,o,e,r,i,s)){_e(d,g,t,o,e,r,i,s,!0),n.preventDefault();return}}function Zn(){$o()}function Qn(n,t,o,e,r,i,s,a){if(!(n.target instanceof HTMLCanvasElement))return;let l=n.target,d=l.getBoundingClientRect(),g=n.clientX-d.left,u=n.clientY-d.top;if(n.buttons===1&&R.isEnabled&&_e(g,u,t,e,r,i,s,a,!1)){n.preventDefault();return}jt(n,l,t,o,e,r,a.get(),i,s)}function es(n,t,o,e,r,i,s,a){if(!(n.target instanceof HTMLCanvasElement))return;let l=n.target;if(n.touches.length===1){let d=l.getBoundingClientRect(),g=n.touches[0],u=g.clientX-d.left,c=g.clientY-d.top;if(Bt(u,c,t,e,r,i,s,a)){_e(u,c,t,e,r,i,s,a,!0),n.preventDefault();return}}jt(n,l,t,o,e,r,a.get(),i,s)}function ts(n,t,o,e,r,i,s,a){if(!(n.target instanceof HTMLCanvasElement))return;let l=n.target;if(n.touches.length===1){let d=l.getBoundingClientRect(),g=n.touches[0],u=g.clientX-d.left,c=g.clientY-d.top;if(_e(u,c,t,e,r,i,s,a,!1)){n.preventDefault();return}}jt(n,l,t,o,e,r,a.get(),i,s)}function wr(n,t,o,e,r,i,s,a){n.addEventListener("mousedown",l=>Jn(l,t,e,r,i,s,a)),n.addEventListener("mousemove",l=>Qn(l,t,o,e,r,i,s,a)),n.addEventListener("mouseup",()=>Zn()),n.addEventListener("touchmove",l=>ts(l,t,o,e,r,i,s,a)),n.addEventListener("touchstart",l=>es(l,t,o,e,r,i,s,a))}async function Tr(n){try{let t=await I.getItem(n);if(t&&typeof t=="object")return console.log("Loaded custom colors:",Object.keys(t).length,"properties"),t}catch(t){console.error("Failed to load saved colors:",t)}return null}function kr(n,t,o,e,r,i){let s=Math.floor(t.get().x/e);if(s>=0&&s<r){let a=Ze(s,o,i.get());n.textContent=a.name}}function xr(n,t,o,e){let r=[];if(o<.1)return r.push({x:n,y:t,tile:e.AGAVE_GROWING}),r;if(r.push({x:n,y:t,tile:e.AGAVE_BASE}),o>.2){let i=Math.min(3,Math.ceil((o-.2)*4));for(let s=-i;s<=i;s++)for(let a=-1;a<=1;a++){if(s===0&&a===0)continue;let l=Math.abs(s)+Math.abs(a);l<=i&&l>0&&r.push({x:n+s,y:t+a,tile:e.AGAVE_SPIKE})}}if(o>.8){let i=Math.ceil((o-.8)*30);for(let s=1;s<=i;s++)r.push({x:n,y:t-s,tile:e.AGAVE_FLOWER_STALK});if(o>.95){let s=t-i;r.push({x:n,y:s,tile:e.AGAVE_FLOWER}),r.push({x:n-1,y:s,tile:e.AGAVE_FLOWER}),r.push({x:n+1,y:s,tile:e.AGAVE_FLOWER})}}return r}function Lr(n,t,o,e){let r=[];if(o<.1)return r.push({x:n,y:t,tile:e.BAMBOO_GROWING}),r;let s=Math.max(1,Math.ceil(7*o));for(let a=0;a<s;a++)a%2===0?r.push({x:n,y:t-a,tile:e.BAMBOO_JOINT}):r.push({x:n,y:t-a,tile:e.BAMBOO_STALK});if(o>.5){let a=t-s;r.push({x:n-1,y:a,tile:e.BAMBOO_LEAVES}),r.push({x:n+1,y:a,tile:e.BAMBOO_LEAVES}),o>.7&&(r.push({x:n-1,y:a+1,tile:e.BAMBOO_LEAVES}),r.push({x:n+1,y:a+1,tile:e.BAMBOO_LEAVES}))}return r}function Cr(n,t,o,e){let r=[];if(o<.1)return r.push({x:n,y:t,tile:e.BERRY_BUSH_GROWING}),r;let s=Math.max(1,Math.ceil(3*o));for(let a=0;a<s;a++)r.push({x:n,y:t-a,tile:e.BERRY_BUSH_BRANCH});if(o>.3){let a=t-s,l=Math.min(2,Math.ceil((o-.3)*3));for(let d=-l;d<=l;d++)for(let g=0;g<=l;g++){let u=n+d,c=a+g,m=Math.abs(d)+Math.abs(g);m<=l&&m>0&&r.push({x:u,y:c,tile:e.BERRY_BUSH_LEAVES})}}if(o>.8){let a=t-s;Math.random()<.6&&r.push({x:n-1,y:a+1,tile:e.BERRY_BUSH_BERRIES}),Math.random()<.6&&r.push({x:n+1,y:a+1,tile:e.BERRY_BUSH_BERRIES}),Math.random()<.6&&r.push({x:n-1,y:a,tile:e.BERRY_BUSH_BERRIES}),Math.random()<.6&&r.push({x:n+1,y:a,tile:e.BERRY_BUSH_BERRIES})}return r}function Mr(n,t,o,e){let r=[];if(o<.1)return r.push({x:n,y:t,tile:e.BIRCH_GROWING}),r;let s=Math.max(1,Math.ceil(7*o));for(let a=0;a<s;a++)r.push({x:n,y:t-a,tile:e.BIRCH_TRUNK}),o>.3&&a%2===1&&r.push({x:n,y:t-a,tile:e.BIRCH_BARK});if(o>.5){let a=t-s,l=t-Math.floor(s*.7);r.push({x:n-1,y:l,tile:e.BIRCH_BRANCHES}),r.push({x:n+1,y:l,tile:e.BIRCH_BRANCHES}),o>.7&&(r.push({x:n-2,y:l,tile:e.BIRCH_BRANCHES}),r.push({x:n+2,y:l,tile:e.BIRCH_BRANCHES}))}if(o>.6){let a=t-s,l=2;for(let d=-l;d<=l;d++)for(let g=-l;g<=1;g++)Math.abs(d)+Math.abs(g)<=l+1&&g<=0&&(d===0&&g>-s||r.push({x:n+d,y:a+g,tile:e.BIRCH_LEAVES}))}if(o>.9){let a=t-s;r.push({x:n-1,y:a+1,tile:e.BIRCH_CATKINS}),r.push({x:n+1,y:a+1,tile:e.BIRCH_CATKINS})}return r}function Ir(n,t,o,e){let r=[];if(o<.1)return r.push({x:n,y:t,tile:e.CACTUS_GROWING}),r;let s=Math.max(1,Math.ceil(5*o));for(let a=0;a<s;a++)r.push({x:n,y:t-a,tile:e.CACTUS_BODY});if(o>.4&&s>2){let a=t-Math.floor(s*.6);r.push({x:n-1,y:a,tile:e.CACTUS_BODY}),o>.6&&r.push({x:n-1,y:a-1,tile:e.CACTUS_BODY})}if(o>.5&&s>3){let a=t-Math.floor(s*.7);r.push({x:n+1,y:a,tile:e.CACTUS_BODY}),o>.7&&r.push({x:n+1,y:a-1,tile:e.CACTUS_BODY})}if(o>.95){let a=t-s;r.push({x:n,y:a,tile:e.CACTUS_FLOWER})}return r}function Ar(n,t,o,e){let r=[];if(o<.1)return r.push({x:n,y:t,tile:e.CARROT_GROWING}),r;if(o>.2){let s=Math.ceil(2*o);for(let a=1;a<=s;a++)r.push({x:n,y:t+a,tile:e.CARROT_ROOT})}let i=Math.max(1,Math.ceil(2*o));for(let s=0;s<i;s++)r.push({x:n,y:t-s,tile:e.CARROT_LEAVES}),o>.5&&s===i-1&&(r.push({x:n-1,y:t-s,tile:e.CARROT_LEAVES}),r.push({x:n+1,y:t-s,tile:e.CARROT_LEAVES}));return r}function Rr(n,t,o,e){let r=[];if(o<.1)return r.push({x:n,y:t,tile:e.CORN_GROWING}),r;let s=Math.max(1,Math.ceil(4*o));for(let a=0;a<s;a++)r.push({x:n,y:t-a,tile:e.CORN_STALK});if(o>.4)for(let a=1;a<s;a++)a%2===1?r.push({x:n-1,y:t-a,tile:e.CORN_LEAVES}):r.push({x:n+1,y:t-a,tile:e.CORN_LEAVES});if(o>.7){let a=t-Math.floor(s*.6);r.push({x:n+1,y:a,tile:e.CORN_EAR}),o>.85&&r.push({x:n+1,y:a-1,tile:e.CORN_SILK})}return r}function Br(n,t,o,e){let r=[];if(o<.1)return r.push({x:n,y:t,tile:e.FERN_GROWING}),r;let s=Math.max(1,Math.ceil(3*o));for(let a=0;a<s;a++)r.push({x:n,y:t-a,tile:e.FERN_STEM});if(o>.3){let a=Math.ceil(o*2);for(let l=0;l<s;l++){let d=Math.min(a,l+1);for(let g=-d;g<=d;g++)g!==0&&r.push({x:n+g,y:t-l,tile:e.FERN_FROND})}}return r}function _r(n,t,o,e){let r=[];if(o<.1)return r.push({x:n,y:t,tile:e.KELP_GROWING}),r;let s=Math.max(1,Math.ceil(6*o));for(let a=0;a<s;a++){let l=Math.floor(Math.sin(a*.5)*1.5);r.push({x:n+l,y:t-a,tile:e.KELP_BLADE}),o>.5&&a%2===1&&a<s-1&&r.push({x:n+l,y:t-a,tile:e.KELP_BULB})}return r}function Dr(n,t,o,e){let r=[];if(o<.1)return r.push({x:n,y:t,tile:e.LAVENDER_GROWING}),r;let s=Math.max(1,Math.ceil(2*o));if(o>.2&&(r.push({x:n,y:t,tile:e.LAVENDER_BUSH}),o>.4&&(r.push({x:n-1,y:t,tile:e.LAVENDER_BUSH}),r.push({x:n+1,y:t,tile:e.LAVENDER_BUSH}))),o>.5)for(let a=1;a<=s;a++)r.push({x:n,y:t-a,tile:e.LAVENDER_STEM}),o>.7&&a===s&&(r.push({x:n-1,y:t-a,tile:e.LAVENDER_STEM}),r.push({x:n+1,y:t-a,tile:e.LAVENDER_STEM}));if(o>.75){let a=t-s;r.push({x:n,y:a,tile:e.LAVENDER_FLOWERS}),o>.85&&(r.push({x:n-1,y:a,tile:e.LAVENDER_FLOWERS}),r.push({x:n+1,y:a,tile:e.LAVENDER_FLOWERS}))}return r}function Or(n,t,o,e){let r=[];if(o<.1)return r.push({x:n,y:t,tile:e.LOTUS_GROWING}),r;if(o>.2){let i=Math.ceil(o*2);for(let s=1;s<=i;s++)r.push({x:n,y:t+s,tile:e.LOTUS_STEM})}if(o>.4&&(r.push({x:n,y:t,tile:e.LOTUS_PAD}),o>.6&&(r.push({x:n-1,y:t,tile:e.LOTUS_PAD}),r.push({x:n+1,y:t,tile:e.LOTUS_PAD}))),o>.7&&r.push({x:n,y:t-1,tile:e.LOTUS_BUD}),o>.85){let i=t-1;r.push({x:n,y:i,tile:e.LOTUS_FLOWER}),r.push({x:n-1,y:i,tile:e.LOTUS_FLOWER}),r.push({x:n+1,y:i,tile:e.LOTUS_FLOWER}),r.push({x:n,y:i-1,tile:e.LOTUS_FLOWER})}return r}function Pr(n,t,o,e){let r=[];if(o<.1)return r.push({x:n,y:t,tile:e.MUSHROOM_GROWING}),r;let s=Math.max(1,Math.ceil(3*o));for(let a=0;a<s;a++)r.push({x:n,y:t-a,tile:e.MUSHROOM_STEM});if(o>.4){let a=t-s;r.push({x:n,y:a,tile:e.MUSHROOM_CAP}),o>.6&&(r.push({x:n-1,y:a,tile:e.MUSHROOM_CAP}),r.push({x:n+1,y:a,tile:e.MUSHROOM_CAP})),o>.8&&(r.push({x:n-1,y:a-1,tile:e.MUSHROOM_CAP}),r.push({x:n,y:a-1,tile:e.MUSHROOM_CAP}),r.push({x:n+1,y:a-1,tile:e.MUSHROOM_CAP}))}return r}function Fr(n,t,o,e){let r=[];if(o<.1)return r.push({x:n,y:t,tile:e.PINE_TREE_GROWING}),r;let s=Math.max(1,Math.ceil(8*o));for(let a=0;a<s;a++)r.push({x:n,y:t-a,tile:e.PINE_TRUNK});if(o>.25){let a=t-Math.floor(s*.3),l=Math.ceil(s*.7);for(let d=0;d<l;d++){let g=a-d,u=Math.max(1,Math.floor((l-d)/2));for(let c=-u;c<=u;c++)(c!==0||d!==0)&&r.push({x:n+c,y:g,tile:e.PINE_NEEDLES})}}if(o>.9){let a=t-Math.floor(s*.5);Math.random()<.5&&r.push({x:n-1,y:a,tile:e.PINE_CONE}),Math.random()<.5&&r.push({x:n+1,y:a,tile:e.PINE_CONE})}return r}function Nr(n,t,o,e){let r=[];if(o<.1)return r.push({x:n,y:t,tile:e.PUMPKIN_GROWING}),r;if(o>.2){let i=Math.ceil(o*3);for(let s=0;s<i;s++)r.push({x:n+s,y:t,tile:e.PUMPKIN_VINE}),r.push({x:n-s,y:t,tile:e.PUMPKIN_VINE})}if(o>.4){let s=Math.ceil(o*3);for(let a=2;a<s;a+=2)r.push({x:n+a,y:t-1,tile:e.PUMPKIN_LEAVES}),r.push({x:n-a,y:t-1,tile:e.PUMPKIN_LEAVES})}if(o>.6){let i=Math.ceil((o-.6)*5);r.push({x:n,y:t,tile:e.PUMPKIN_FRUIT}),i>1&&(r.push({x:n+1,y:t,tile:e.PUMPKIN_FRUIT}),r.push({x:n-1,y:t,tile:e.PUMPKIN_FRUIT})),i>2&&(r.push({x:n+1,y:t+1,tile:e.PUMPKIN_FRUIT}),r.push({x:n-1,y:t+1,tile:e.PUMPKIN_FRUIT}))}return o>.9&&r.push({x:n,y:t-1,tile:e.PUMPKIN_STEM}),r}function Gr(n,t,o,e){let r=[];if(o<.1)return r.push({x:n,y:t,tile:e.ROSE_GROWING}),r;let s=Math.max(1,Math.ceil(4*o));for(let a=0;a<s;a++)r.push({x:n,y:t-a,tile:e.ROSE_STEM}),o>.3&&a>0&&a<s-1&&(a%2===0?r.push({x:n-1,y:t-a,tile:e.ROSE_THORNS}):r.push({x:n+1,y:t-a,tile:e.ROSE_THORNS}));if(o>.4)for(let a=1;a<s;a+=2)r.push({x:n-1,y:t-a,tile:e.ROSE_LEAVES}),r.push({x:n+1,y:t-a,tile:e.ROSE_LEAVES});if(o>.6){let a=t-s;r.push({x:n,y:a,tile:e.ROSE_BUD})}if(o>.85){let a=t-s;r.push({x:n,y:a,tile:e.ROSE_BLOOM}),r.push({x:n-1,y:a,tile:e.ROSE_BLOOM}),r.push({x:n+1,y:a,tile:e.ROSE_BLOOM}),r.push({x:n,y:a-1,tile:e.ROSE_BLOOM})}return r}function Hr(n,t,o,e){let r=[];if(o<.1)return r.push({x:n,y:t,tile:e.SUNFLOWER_GROWING}),r;let s=Math.max(1,Math.ceil(5*o));for(let a=0;a<s;a++)r.push({x:n,y:t-a,tile:e.SUNFLOWER_STEM});if(o>.3)for(let l=2;l<s;l+=2)l%2===0?r.push({x:n-1,y:t-l,tile:e.SUNFLOWER_LEAVES}):r.push({x:n+1,y:t-l,tile:e.SUNFLOWER_LEAVES});if(o>.7){let a=t-s;r.push({x:n,y:a,tile:e.SUNFLOWER_CENTER}),o>.85&&(r.push({x:n-1,y:a,tile:e.SUNFLOWER_PETALS}),r.push({x:n+1,y:a,tile:e.SUNFLOWER_PETALS}),r.push({x:n,y:a-1,tile:e.SUNFLOWER_PETALS}),r.push({x:n,y:a+1,tile:e.SUNFLOWER_PETALS}))}return r}function qr(n,t,o,e){let r=[];if(o<.1)return r.push({x:n,y:t,tile:e.TREE_GROWING}),r;let i=Q(3,5),s=Math.max(1,Math.ceil(i*o));for(let a=0;a<s;a++){let l=t-a;r.push({x:n,y:l,tile:e.TREE_TRUNK})}if(o>.3){let a=t-s,l=Math.min(3,Math.ceil((o-.3)*5));for(let d=-l;d<=l;d++)for(let g=-l;g<=1;g++){let u=n+d,c=a+g;Math.sqrt(d*d+g*g)<=l&&g<=0&&(r.find(b=>b.x===u&&b.y===c&&b.tile===e.TREE_TRUNK)||r.push({x:u,y:c,tile:e.TREE_LEAVES}))}}return r}function Wr(n,t,o,e){let r=[];if(o<.1)return r.push({x:n,y:t,tile:e.TULIP_GROWING}),r;o>.15&&r.push({x:n,y:t+1,tile:e.TULIP_BULB});let s=Math.max(1,Math.ceil(3*o));for(let a=0;a<s;a++)r.push({x:n,y:t-a,tile:e.TULIP_STEM});if(o>.3){let a=t-Math.floor(s*.5);r.push({x:n-1,y:a,tile:e.TULIP_LEAVES}),r.push({x:n+1,y:a,tile:e.TULIP_LEAVES})}if(o>.7){let a=t-s;r.push({x:n,y:a,tile:e.TULIP_PETALS}),o>.85&&(r.push({x:n-1,y:a,tile:e.TULIP_PETALS}),r.push({x:n+1,y:a,tile:e.TULIP_PETALS}),r.push({x:n,y:a-1,tile:e.TULIP_PETALS}))}return r}function jr(n,t,o,e){let r=[];if(o<.1)return r.push({x:n,y:t,tile:e.WHEAT_GROWING}),r;let s=Math.max(1,Math.ceil(4*o));for(let a=0;a<s;a++){let l=t-a;a<s-1||o<.8?r.push({x:n,y:l,tile:e.WHEAT_STALK}):r.push({x:n,y:l,tile:e.WHEAT_GRAIN}),o>.5&&a>0&&a<s-1&&(Math.random()<.4&&r.push({x:n-1,y:l,tile:e.WHEAT_STALK}),Math.random()<.4&&r.push({x:n+1,y:l,tile:e.WHEAT_STALK}))}if(o>.9){let a=t-s+1;r.push({x:n-1,y:a,tile:e.WHEAT_GRAIN}),r.push({x:n+1,y:a,tile:e.WHEAT_GRAIN})}return r}function $r(n,t,o,e){let r=[];if(o<.1)return r.push({x:n,y:t,tile:e.WILLOW_TREE_GROWING}),r;let s=Math.max(1,Math.ceil(6*o));for(let a=0;a<s;a++)r.push({x:n,y:t-a,tile:e.WILLOW_TRUNK});if(o>.3){let a=t-s,l=Math.ceil(o*4);for(let d=0;d<l;d++)r.push({x:n-1-Math.floor(d/2),y:a+d,tile:e.WILLOW_BRANCHES}),o>.6&&d>0&&r.push({x:n-1-Math.floor(d/2),y:a+d,tile:e.WILLOW_LEAVES});for(let d=0;d<l;d++)r.push({x:n+1+Math.floor(d/2),y:a+d,tile:e.WILLOW_BRANCHES}),o>.6&&d>0&&r.push({x:n+1+Math.floor(d/2),y:a+d,tile:e.WILLOW_LEAVES})}if(o>.8){let a=t-s;r.push({x:n-2,y:a+2,tile:e.WILLOW_LEAVES}),r.push({x:n+2,y:a+2,tile:e.WILLOW_LEAVES}),r.push({x:n-3,y:a+3,tile:e.WILLOW_LEAVES}),r.push({x:n+3,y:a+3,tile:e.WILLOW_LEAVES})}return r}function Ur(n,t,o,e,r){switch(e=Math.max(0,Math.min(1,e)),o){case"WHEAT":return jr(n,t,e,r);case"CARROT":return Ar(n,t,e,r);case"MUSHROOM":return Pr(n,t,e,r);case"CACTUS":return Ir(n,t,e,r);case"WALNUT":return qr(n,t,e,r);case"BERRY_BUSH":return Cr(n,t,e,r);case"BAMBOO":return Lr(n,t,e,r);case"SUNFLOWER":return Hr(n,t,e,r);case"CORN":return Rr(n,t,e,r);case"PINE_TREE":return Fr(n,t,e,r);case"WILLOW_TREE":return $r(n,t,e,r);case"FERN":return Br(n,t,e,r);case"TULIP":return Wr(n,t,e,r);case"AGAVE":return xr(n,t,e,r);case"LAVENDER":return Dr(n,t,e,r);case"KELP":return _r(n,t,e,r);case"ROSE":return Gr(n,t,e,r);case"PUMPKIN":return Nr(n,t,e,r);case"LOTUS":return Or(n,t,e,r);case"BIRCH":return Mr(n,t,e,r);default:return[{x:n,y:t,tile:r.WHEAT_GROWING}]}}function Kr(n,t,o,e,r,i){let s=!1,a=!1,l=e.get(),d={...n.get()},g={...t.get()};for(let[u,c]of Object.entries(d)){if(c.timeLeft-=1/60,g[u]){let m=g[u],p=Math.max(0,Math.min(1,1-c.timeLeft/o[c.seedType].growthTime));m.blocks&&(Array.isArray(m.blocks)?m.blocks:Object.values(m.blocks)).forEach(S=>{S.x>=0&&S.x<i&&S.y>=0&&S.y<r&&l.getTile(S.x,S.y)!==o.AIR&&l.setTile(S.x,S.y,o.AIR)});let[b,v]=u.split(",").map(Number);m.blocks=Ur(b,v,c.seedType,p,o),(Array.isArray(m.blocks)?m.blocks:Object.values(m.blocks)).forEach(f=>{f.x>=0&&f.x<i&&f.y>=0&&f.y<r&&l.setTile(f.x,f.y,f.tile)}),a=!0}c.timeLeft<=0&&(g[u]&&(g[u].mature=!0,g[u].seedType=c.seedType),delete d[u],s=!0)}s&&n.set(d),a&&t.set(g)}function zr(n,t,o,e){let r=Math.floor(t.get().y/e),i="Surface";if(r>o){let s=r-o;s<15?i="Shallow":s<30?i="Deep":i="Very Deep"}else r<o-5&&(i="Sky");n.textContent=i}function Yr(n,t,o,e,r,i){if(!t||typeof t.getTile!="function")return console.error("isSolid: Invalid world object",{hasWorld:!!t,worldType:typeof t,hasGetTile:t?typeof t.getTile:"N/A",args:{tileSize:n,worldHeight:o,worldWidth:e,x:r,y:i},stack:new Error().stack}),!0;let s=Math.floor(r/n),a=Math.floor(i/n);if(s<0||s>=e||a<0||a>=o)return!0;let l=t.getTile(s,a);return l&&l.solid}function $t(n,t,o,e,r,i,s,a){return[[s,a],[s+o,a],[s,a+n],[s+o,a+n],[s+o/2,a],[s+o/2,a+n],[s,a+n/2],[s+o,a+n/2]].some(d=>Yr(t,e,r,i,d[0],d[1]))}function q(n,t){let o=n.host;return o&&typeof o=="object"&&"keys"in o&&"touchKeys"in o?o.keys[t]||o.touchKeys[t]:!1}function Vr(n,t,o,e,r,i,s,a,l,d,g,u=1){let c=l.get(),m=a.get(),p=s.get();c.velocityY+=t,c.velocityY>o&&(c.velocityY=o);let b=0,v=0;if(q(g,"upleft")?(b=-1,v=-1,c.lastDirection=-1):q(g,"upright")?(b=1,v=-1,c.lastDirection=1):q(g,"downleft")?(b=-1,v=1,c.lastDirection=-1):q(g,"downright")?(b=1,v=1,c.lastDirection=1):(q(g,"a")||q(g,"arrowleft")?(b=-1,c.lastDirection=-1):(q(g,"d")||q(g,"arrowright"))&&(b=1,c.lastDirection=1),q(g,"s")&&(v=1)),b!==0){let y=b*c.speed*u;Math.abs(c.velocityX)<.5||Math.sign(c.velocityX)!==Math.sign(y)?c.velocityX=y*.3:c.velocityX=y}else c.velocityX*=n,c.lastDirection=0;(q(g,"w")||q(g,"arrowup")||q(g," ")||q(g,"upleft")||q(g,"upright"))&&c.onGround&&(c.velocityY=-c.jumpPower,c.onGround=!1),b!==0&&v!==0&&(c.velocityX*=.707);let h=c.x+c.velocityX;$t(c.height,e,c.width,p,r,i,h,c.y)?c.velocityX=0:c.x=h;let f=c.y+c.velocityY;$t(c.height,e,c.width,p,r,i,c.x,f)?(c.velocityY>0&&(c.onGround=!0),c.velocityY=0):(c.y=f,c.onGround=!1),c.x=Math.max(0,Math.min(c.x,i*e-c.width)),c.y=Math.max(0,Math.min(c.y,r*e-c.height));let S=c.x+c.width/2-d.width/2,E=c.y+c.height/2-d.height/2;m.x+=(S-m.x)*.1,m.y+=(E-m.y)*.1,m.x=Math.max(0,Math.min(m.x,i*e-d.width)),m.y=Math.max(0,Math.min(m.y,r*e-d.height))}function Xr(n,t,o,e,r,i,s,a,l,d,g,u,c,m,p,b,v){let h=t.get(),f=o.get(),S=s.get(),E=c.get(),y=m.player.x+(h.x-m.player.x)*p,k=m.player.y+(h.y-m.player.y)*p,w=m.camera.x+(f.x-m.camera.x)*p,L=m.camera.y+(f.y-m.camera.y)*p,C=n.getContext("2d");C.fillStyle=b.air,C.fillRect(0,0,n.width,n.height);let M=Math.ceil(n.width/r)+1,N=Math.ceil(n.height/r)+1,$=Math.floor(w/r),G=Math.floor(L/r);for(let W=0;W<M;W++)for(let z=0;z<N;z++){let U=$+W,Y=G+z;if(U>=0&&U<l&&Y>=0&&Y<a){let re=S.getTile(U,Y);if(!re||re===e.AIR)continue;let pe=new A.Computed(()=>i.get()==="xray"&&re.solid&&re!==e.COAL&&re!==e.IRON&&re!==e.GOLD&&re!==e.LAVA).get()?"xray":v[re.id];C.fillStyle=b[pe],C.fillRect(Math.round(W*r-w%r),Math.round(z*r-L%r),r,r)}}let P=y-w,H=k-L;if(C.fillStyle=b["player-body"],C.fillRect(P,H,h.width,h.height),C.strokeStyle=b["player-border"],C.lineWidth=1,C.strokeRect(P,H,h.width,h.height),C.fillStyle=b["player-eyes"],C.fillRect(P+1,H+1,1,1),C.fillRect(P+4,H+1,1,1),d.get()==="fog"){E.updateFromPlayer(t,r);let W={get:()=>({x:w,y:L})};if(g.get()){E.renderScaled(C,n,r,W,u.get());return}let{velocityX:z,velocityY:U}=h;(Math.abs(z)>.1||Math.abs(U)>.1)&&g.set(!0),E.render(C,n,r,W)}}var os=50,ut=1e3/os,rs=20,Jr=performance.now(),gt=0,qe={player:{x:0,y:0},camera:{x:0,y:0}},Ut=null,Zr=0,ns=1e3;async function ss(){let n=Date.now();return(n-Zr>ns||Ut===null)&&(Ut=await I.getItem("sprite-garden-movement-scale"),Zr=n),Ut}async function mt(n,t,o,e,r,i,s,a,l,d,g,u,c,m,p,b,v,h,f,S,E,y,k,w,L,C,M,N,$,G,P){if(N.get()){N.set(!1);return}let H=performance.now(),ke=Math.min(H-Jr,250);Jr=H,gt+=ke;let W=0;for(;gt>=ut&&W<rs;){let U=M.get(),Y=y.get();qe.player.x=U.x,qe.player.y=U.y,qe.camera.x=Y.x,qe.camera.y=Y.y,Vr(g,u,m,b,f,S,P,y,M,n,o,await ss()),Kr(L,C,v,P,f,S),Qe(v,h,G,P,f,S),kr(e,M,a,b,S,E),zr(r,M,p,b),w.set(w.get()+ut/1e3),gt-=ut,W++}let z=gt/ut;Xr(n,M,y,v,b,$,P,f,S,l,c,d,k,qe,z,s,i),requestAnimationFrame(async()=>await mt(n,t,o,e,r,i,s,a,l,d,g,u,c,m,p,b,v,h,f,S,E,y,k,w,L,C,M,N,$,G,P))}function Qr(n,t,o="",e=""){let r;for(let i of t){r||(r={});let s=i.slice(o.length);s=s.slice(0,e.length>0?-e.length:void 0),r[s]=n.getPropertyValue(i)}return r}function Kt(n={},t="--sg-",o="",e=r=>r){let r;for(let[i,s]of Object.entries(n)){if(r||(r={}),!i.startsWith(t))continue;let a=i.slice(t.length);a=a.slice(0,o.length>0?-o.length:void 0);let l=e(a);r[l]=s.trim().replace(/^['"]|['"]$/g,"")}return r}async function en(n,t,o){if(t.dispatchEvent(new CustomEvent("sprite-garden-load",{detail:{isLoading:!0,error:null},bubbles:!0,composed:!0})),!o){let h="HTML canvas is required to init Sprite Garden.";console.error(h),t.dispatchEvent(new CustomEvent("sprite-garden-load",{detail:{isLoading:!1,error:h},bubbles:!0,composed:!0}));return}t.addEventListener("focusout",h=>{o.focus()}),n.top===n&&o.focus();let{gameConfig:e,gameState:r}=await co(n,Ye),i=n.document,s=t.host;s.keys={},s.touchKeys={};let a=await Tr(ct),l=X(n,t),d=a??l;He(t,d),jo(t,e.fogMode,r.viewMode),vr(n,t),yr(n,t),Sr(n,t),Er(t),Eo(t,lo.totalSeeds,e.breakMode,e.fogMode,e.worldSeed,r.gameTime,r.materialsInventory,r.seedInventory,r.selectedMaterialType,r.selectedSeedType,r.viewMode);let g=e.WORLD_HEIGHT.get(),u=e.WORLD_WIDTH.get();if(!await vo(n,t)){let h=ge(e.BIOMES,e.SURFACE_LEVEL.get(),e.TILE_SIZE.get(),e.TILES,g,u,e.worldSeed,r.gameTime,r.growthTimers,r.plantStructures,r.player,r.materialsInventory,r.seedInventory);r.world.set(h);let f=Ae(e.isFogScaled,g,u,d);r.exploredMap.set(f)}setInterval(async()=>{await Se()==="auto"&&await Ee(n)},wt),wr(o,r.camera,e.canvasScale.get(),e.TILES,e.TILE_SIZE.get(),e.WORLD_HEIGHT.get(),e.WORLD_WIDTH.get(),r.world),se(t,e);let m=await I.setItem("sprite-garden-version",Ye);console.log(`Sprite Garden version: ${m}`),t.addEventListener("sprite-garden-reset",async h=>{let f;h instanceof CustomEvent&&(f=h?.detail?.colors??{});let S;Object.keys(f).length&&f.constructor===Object?S=Kt(f,"--sg-tile-","-color"):S=Kt(l,"--sg-tile-","-color"),await mt(o,n,t,t.getElementById("currentBiome"),t.getElementById("currentDepth"),vt(e.TILES),S,e.BIOMES,e.fogMode,e.fogScale,e.FRICTION.get(),e.GRAVITY.get(),e.isFogScaled,e.MAX_FALL_SPEED.get(),e.SURFACE_LEVEL.get(),e.TILE_SIZE.get(),e.TILES,e.waterPhysics,e.WORLD_HEIGHT.get(),e.WORLD_WIDTH.get(),e.worldSeed,r.camera,r.exploredMap,r.gameTime,r.growthTimers,r.plantStructures,r.player,r.shouldReset,r.viewMode,r.waterPhysicsQueue,r.world),r.shouldReset.set(!0)});let p=n.getComputedStyle(t.host),b=Qr(p,Object.keys(ee.tile).map(h=>`--sg-tile-${h}-color`),"--sg-tile-","-color"),v=()=>{yt.get()||(yt.set(!0),t.dispatchEvent(new CustomEvent("sprite-garden-toast",{detail:{message:"\u{1F3AE} Harakat uchun klaviatura yoki ekrandagi tugmalardan foydalaning. Boshlash uchun Space, \u2191, W yoki R ni bosing.",config:{duration:0,manualClose:!0,stack:!0,bottomOffset:2}}})))};await mt(o,n,t,t.getElementById("currentBiome"),t.getElementById("currentDepth"),vt(e.TILES),b,e.BIOMES,e.fogMode,e.fogScale,e.FRICTION.get(),e.GRAVITY.get(),e.isFogScaled,e.MAX_FALL_SPEED.get(),e.SURFACE_LEVEL.get(),e.TILE_SIZE.get(),e.TILES,e.waterPhysics,e.WORLD_HEIGHT.get(),e.WORLD_WIDTH.get(),e.worldSeed,r.camera,r.exploredMap,r.gameTime,r.growthTimers,r.plantStructures,r.player,r.shouldReset,r.viewMode,r.waterPhysicsQueue,r.world),setTimeout(()=>v(),500),t.getElementById("loading").setAttribute("hidden","hidden"),t.dispatchEvent(new CustomEvent("sprite-garden-load",{detail:{isLoading:!1,pkg:{version:Ye},error:null},bubbles:!0,composed:!0}))}var tn="sprite-garden-select",zt=class extends HTMLElement{#o;#t;#e;#n;#s;#r=[];constructor(){super();let t=globalThis.document.createElement("template");t.innerHTML=`
      <style>
        [hidden] {
          display: none !important;
        }

        .sprite-garden-select {
          align-items: center;
          background: var(--sg-color-green-500);
          border-radius: 0.25rem;
          border: none;
          color: var(--sg-color-white);
          cursor: pointer;
          display: flex;
          font-size: 0.625rem;
          justify-content: space-between;
          min-height: 2.125rem;
          padding: 0.375rem 0.75rem;
          text-align: center;
          transition: all 0.2s;
          user-select: none;
          width: 100%;
          box-sizing: border-box;
          overflow: hidden;
        }

        .sprite-garden-select > * {
          flex-shrink: 0;
        }

        .selected-text {
          flex: 1;
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
        }

        .sprite-garden-select:hover {
          background: var(--sg-color-green-600);
        }

        .sprite-garden-select:focus {
          outline-offset: -0.125rem;
          outline: 0.125rem solid var(--sg-color-green-500);
        }

        .sprite-garden-select[aria-expanded="true"] {
          border-radius: 0.25rem 0.25rem 0 0;
        }

        .chevron {
          flex-shrink: 0;
          font-size: 0.75rem;
          margin-left: 0.25rem;
          padding-left: 0.25rem;
          transition: transform 0.2s;
        }

        .sprite-garden-select[aria-expanded="true"] .chevron {
          transform: rotate(180deg);
        }

        .placeholder {
          opacity: 0.8;
        }

        .select-list {
          background: var(--sg-color-gray-900);
          border-radius: 0 0 0.25rem 0.25rem;
          border-top: none;
          border: 0.0625rem solid var(--sg-color-neutral-950);
          left: 0;
          list-style: none;
          margin: 0;
          max-height: 12rem;
          overflow-y: auto;
          padding: 0;
          position: absolute;
          right: 0;
          top: 100%;
          z-index: 1000;
        }

        .select-list[hidden] {
          display: none;
        }

        .select-item {
          border-bottom: 1px solid rgba(255,255,255,0.05);
          color: var(--sg-color-white);
          cursor: pointer;
          font-size: 0.625rem;
          padding: 0.375rem 0.75rem;
          transition: all 0.2s;
        }

        .select-item:last-child {
          border-bottom: none;
        }

        .select-item:hover,
        .select-item:focus,
        .select-item[aria-selected="true"] {
          background: var(--sg-color-green-500);
          outline: none;
        }

        .select-item:focus {
          outline: 2px solid var(--sg-color-green-500);
          outline-offset: -2px;
        }

        /* scrollbar */
        .select-list {
          scrollbar-color: var(--sg-color-gray-700) var(--sg-color-gray-900);
          scrollbar-width: thin;
        }

        .select-list::-webkit-scrollbar {
          width: 0.625rem;
        }

        .select-list::-webkit-scrollbar-track {
          background: var(--sg-color-gray-900);
        }

        .select-list::-webkit-scrollbar-thumb {
          background: var(--sg-color-gray-700);
          border-radius: 0.625rem;
          border: 0.125rem solid var(--sg-color-gray-900);
        }

        .select-list::-webkit-scrollbar-thumb:hover {
          background: var(--sg-color-neutral-950);
        }
      </style>

      <div
        aria-expanded="false"
        aria-haspopup="listbox"
        aria-labelledby="selectedValue"
        class="sprite-garden-select"
        role="combobox"
        tabindex="0"
      >
        <span class="selected-text placeholder" id="selectedValue">Choose resolution</span>
        <span aria-hidden="true" class="chevron">\u25BE</span>
      </div>

      <ul class="select-list" hidden id="selectList" role="listbox"></ul>

      <input id="valueInput" type="hidden">
    `,this.#o=this.attachShadow({mode:"open"}),this.#o.appendChild(t.content.cloneNode(!0)),this.#t=this.#o.querySelector(".sprite-garden-select"),this.#e=this.#o.querySelector("#selectList"),this.#n=this.#o.querySelector("#selectedValue"),this.#s=this.#o.querySelector("#valueInput")}connectedCallback(){requestAnimationFrame(()=>this.#i())}#i(){if(this.#r.length>0)return;this.querySelectorAll("sprite-garden-option").length===0&&(this.innerHTML=`
        <sprite-garden-option value="400">400x400</sprite-garden-option>
        <sprite-garden-option value="800">800x800</sprite-garden-option>
        <sprite-garden-option hidden="hidden" value="fullscreen">Fullscreen</sprite-garden-option>
      `),this.#a();let t=-1,o=()=>{this.#a(),this.#e.hidden=!1,this.#t.setAttribute("aria-expanded","true"),this.#r[0]?.focus(),t=0},e=()=>{this.#e.hidden=!0,this.#t.setAttribute("aria-expanded","false"),this.#t.focus(),t=-1},r=i=>{let s=i.dataset.value,a=i.textContent.trim();this.#n.textContent=a,this.#n.classList.remove("placeholder"),this.#s.value=s,this.#r.forEach(l=>l.setAttribute("aria-selected","false")),i.setAttribute("aria-selected","true"),e(),this.dispatchEvent(new CustomEvent("change",{detail:{value:s,label:a},bubbles:!0,composed:!0}))};this.#t.addEventListener("keydown",i=>{switch(i.key){case"Enter":case" ":i.preventDefault(),this.#e.hidden?o():e();break;case"ArrowDown":case"ArrowUp":i.preventDefault(),o();break;case"Escape":e();break}}),this.#e.addEventListener("keydown",i=>{let s=i.target.closest(".select-item");if(s)switch(i.stopPropagation(),i.key){case"Enter":case" ":i.preventDefault(),r(s);break;case"Escape":e();break;case"ArrowDown":i.preventDefault(),t=(t+1)%this.#r.length,this.#r[t].focus();break;case"ArrowUp":i.preventDefault(),t=t===0?this.#r.length-1:t-1,this.#r[t].focus();break;case"Home":i.preventDefault(),this.#r[0].focus(),t=0;break;case"End":i.preventDefault(),this.#r[this.#r.length-1].focus(),t=this.#r.length-1;break}}),this.#t.addEventListener("click",i=>{i.stopPropagation(),this.#e.hidden?o():e()}),this.#o.addEventListener("click",i=>{let s=i.target.closest(".select-item");s&&(i.stopPropagation(),r(s))}),globalThis.document.addEventListener("click",i=>{this.contains(i.target)||e()}),Object.defineProperty(this,"value",{get:()=>this.#s.value,set:i=>{this.#s.value=i;let s=this.#e.querySelector(`[data-value="${i}"]`);s?(this.#n.textContent=s.textContent.trim(),this.#n.classList.remove("placeholder")):(this.#n.textContent=i,this.#n.classList.remove("placeholder"))}}),this.getAttribute("value")&&(this.value=this.getAttribute("value"))}#a(){this.#e.innerHTML="",this.#r=[],Array.from(this.querySelectorAll("sprite-garden-option")).forEach(t=>{if(t.hasAttribute("hidden"))return;let o=globalThis.document.createElement("li");o.className="select-item",o.setAttribute("role","option"),o.tabIndex=-1;let e=t.getAttribute("value")||"";o.dataset.value=e,o.textContent=t.textContent.trim(),this.#e.appendChild(o),this.#r.push(o)})}static get observedAttributes(){return["value"]}attributeChangedCallback(t){t==="value"&&(this.value=this.getAttribute("value"))}};globalThis.customElements?.get(tn)||globalThis.customElements.define(tn,zt);var on="sprite-garden",Yt=class extends HTMLElement{constructor(){if(super(),!this.shadowRoot){let t=globalThis.document.createElement("template");t.innerHTML=`
        <style>
          [hidden] {
            display: none !important;
          }

          :focus {
            outline: none;
          }

          :host {
            align-items: center;
            display: flex;
            height: var(--sg-ui-host-height, 100vh);
            height: var(--sg-ui-host-height, 100dvh);
            justify-content: center;
            overflow: var(--sg-ui-host-overflow, hidden);
            position: relative;
            width: var(--sg-ui-host-width, 100vw);
            width: var(--sg-ui-host-width, 100dvw);

            -webkit-user-select: none;
            -moz-user-select: none;
            -ms-user-select: none;
            user-select: none;
            -webkit-touch-callout: none;

            ${ze("--sg-color-",ee.color)}
            ${ze("--sg-tile-",ee.tile)}
            ${ze("--sg-ui-",ee.ui)}
          }

          #canvas {
            object-fit: cover;
            background: var(--sg-color-black);
            display: block;
            image-rendering: -moz-crisp-edges;
            image-rendering: -webkit-crisp-edges;
            image-rendering: crisp-edges;
            position: absolute;
          }

          #loading {
            animation: pulseSpin 1.8s infinite;
            height: 11rem;
            image-rendering: pixelated;
            position: relative;
            width: 11rem;
            z-index: 1;
          }

          #loading .pixel {
            background: var(--sg-tile-loading-pixel-color);
            height: 4rem;
            position: absolute;
            width: 4rem;
          }

          .pixel-one {
            top: 0;
            left: 4rem;
          }

          .pixel-two {
            top: 1rem;
            left: 1rem;
          }

          .pixel-three {
            top: 1rem;
            left: 7rem;
          }

          .pixel-four {
            top: 4rem;
            left: 0;
          }

          .pixel-five {
            top: 4rem;
            left: 8rem;
          }

          .pixel-six {
            top: 7rem;
            left: 1rem;
          }

          .pixel-seven {
            top: 7rem;
            left: 7rem;
          }

          .pixel-eight {
            top: 8rem;
            left: 4rem;
          }

          .pixel-nine {
            top: 4rem;
            left: 4rem;
          }

          @keyframes pulseSpin {
            0% {
              transform: scale(0.05) rotate(0deg);
              opacity: 0;
            }
            50% {
              transform: scale(1) rotate(-180deg);
              opacity: 1;
            }
            100% {
              transform: scale(0.05) rotate(-360deg);
              opacity: 0;
            }
          }

          .ui-grid {
            display: grid;
            grid-template-columns: 1fr 1fr;
            grid-template-rows: 1fr 1fr;
            height: var(--sg-ui-grid-height, calc(100vh - 1rem));
            height: var(--sg-ui-grid-height, calc(100dvh - 1rem));
            margin: 0.5rem;
            pointer-events: none;
            position: absolute;
            top: 0;
            width: var(--sg-ui-grid-width, calc(100vw - 1rem));
            width: var(--sg-ui-grid-width, calc(100dvw - 1rem));
          }

          .ui-grid__corner {
            max-height: var(--sg-ui-grid-corner-max-height, calc(100vh - 1rem));
            max-height: var(--sg-ui-grid-corner-max-height, calc(100dvh - 1rem));
            overflow-y: auto;
            pointer-events: auto;
            transition: height 0.35s cubic-bezier(0.68, -0.55, 0.27, 1.5);
          }

          .ui-grid__corner--heading {
            color: var(--sg-ui-grid-corner-heading-color);
            backdrop-filter: blur(0.3125rem);
            background-color: var(--sg-ui-grid-corner-heading-background-color);
            border-radius: 0.5rem;
            border-width: 0.0625rem;
            border-style: solid;
            border-color: var(--sg-ui-grid-corner-heading-border-color);
            cursor: pointer;
            font-size: 0.75rem;
            padding: 0.75rem;
          }

          .ui-grid__corner--container {
            color: var(--sg-ui-grid-corner-container-color);
            padding: 0.5rem;
          }

          #controls .ui-grid__corner--container {
            cursor: pointer;
          }

          .ui-grid__corner--sub-heading {
            color: var(--sg-ui-grid-corner-heading-color);
            margin: 1rem 0 0.3125rem 0;
          }

          .ui-grid__corner--sub-section {
            overflow: auto;
          }

          .ui-grid__corner {
            scrollbar-width: thin; /* Slim scrollbar */
            scrollbar-color: var(--sg-color-gray-700) var(--sg-color-stone-100); /* Handle and track colors */
          }

          .ui-grid__corner::-webkit-scrollbar {
            width: 0.625rem; /* Custom width */
          }

          .ui-grid__corner::-webkit-scrollbar-track {
            background: var(--sg-color-stone-100); /* Track background */
          }

          .ui-grid__corner::-webkit-scrollbar-thumb {
            background: var(--sg-color-gray-700); /* Handle color */
            border-radius: 0.625rem; /* Rounded edges */
            border: 0.125rem solid var(--sg-color-stone-100); /* Padding around handle */
          }

          .ui-grid__corner::-webkit-scrollbar-thumb:hover {
            background: var(--sg-color-neutral-950); /* Highlight on hover */
          }

          /* Element modifiers for sticky corners */
          .ui-grid__corner--top-left {
            align-self: start;
            grid-column: 1;
            grid-row: 1;
            justify-self: start;
          }

          .ui-grid__corner--top-right {
            align-self: start;
            grid-column: 2;
            grid-row: 1;
            justify-self: end;
            z-index: 1;
          }

          .ui-grid__corner--bottom-left {
            align-self: end;
            grid-column: 1;
            grid-row: 2;
            justify-self: start;
          }

          .ui-grid__corner--bottom-right {
            align-self: end;
            grid-column: 2;
            grid-row: 2;
            justify-self: end;
            max-width: 15.625rem;
          }

          /* Center position for train panel */
          .ui-grid__corner--center {
            align-self: start;
            grid-column: 1 / 3;
            grid-row: 1 / 3;
            justify-self: center;
            margin-top: 0.5rem;
            max-height: 80dvh;
            max-height: 80vh;
            max-width: 90dvw;
            max-width: 90vw;
            z-index: 100;
          }

          /* Train Panel */
          #train {
            backdrop-filter: blur(0.3125rem);
            background: var(--sg-color-black-alpha-80);
            border-radius: 0.5rem;
            border: 0.0625rem solid var(--sg-color-gray-alpha-10);
            color: var(--sg-color-white);
            font-size: 0.5625rem;
          }

          .train-section {
            border-bottom: 0.0625rem solid var(--sg-color-gray-700);
            margin-bottom: 1rem;
            padding-bottom: 1rem;
          }

          .train-section:last-child {
            border-bottom: none;
          }

          .train-status {
            display: grid;
            font-size: 0.625rem;
            gap: 0.5rem;
            grid-template-columns: 1fr 1fr;
          }

          .train-status > div {
            padding: 0.25rem;
          }

          .train-config {
            display: flex;
            flex-direction: column;
            gap: 0.5rem;
          }

          .train-config label {
            align-items: center;
            display: flex;
            font-size: 0.625rem;
            justify-content: space-between;
          }

          .train-config input {
            background: var(--sg-color-gray-900);
            border-radius: 0.25rem;
            border: 0.0625rem solid var(--sg-color-gray-700);
            color: var(--sg-color-white);
            font-size: 0.625rem;
            padding: 0.25rem 0.5rem;
            width: 5rem;
          }

          .train-config input:focus {
            outline: 0.125rem solid var(--sg-color-green-500);
          }

          .train-actions {
            display: flex;
            flex-wrap: wrap;
            gap: 0.5rem;
            justify-content: space-between;
          }

          .train-actions button {
            background: var(--sg-color-green-500);
            border-radius: 0.25rem;
            border: none;
            color: var(--sg-color-white);
            cursor: pointer;
            font-size: 0.625rem;
            padding: 0.5rem 1rem;
            transition: background 0.2s;
            width: 12rem;
          }

          .train-actions button:hover:not(:disabled) {
            background: var(--sg-color-green-600);
          }

          .train-actions button:active:not(:disabled) {
            transform: scale(0.95);
          }

          .train-actions button:disabled {
            background: var(--sg-color-gray-700);
            cursor: not-allowed;
            opacity: 0.5;
          }

          #trainStartBtn {
            background: var(--sg-color-green-500);
          }

          #trainStopBtn {
            background: var(--sg-color-red-500);
          }

          #trainTestBtn {
            background: var(--sg-color-blue-500);
          }

          #trainExportBtn {
            background: var(--sg-color-amber-500);
          }

          #trainCopyLogsBtn {
            background: var(--sg-color-medium-purple);
          }

          #trainImportBtn {
            background: var(--sg-color-gray-900);
          }

          #trainClearLogsBtn {
            background: var(--sg-color-gray-600);
            color: var(--sg-color-gray-900);
          }

          .train-logs {
            background: var(--sg-color-black);
            border-radius: 0.25rem;
            border: 0.0625rem solid var(--sg-color-gray-700);
            font-family: monospace;
            font-size: 0.5625rem;
            padding: 0.5rem;
          }

          .train-log-entry {
            border-bottom: 0.0625rem solid var(--sg-color-gray-800);
            padding: 0.125rem 0;
          }

          .train-log-entry:last-child {
            border-bottom: none;
          }

          .train-log-entry--success {
            color: var(--sg-color-green-400);
          }

          .train-log-entry--warning {
            color: var(--sg-color-amber-400);
          }

          .train-log-entry--error {
            color: var(--sg-color-red-400);
          }

          .train-log-entry--info {
            color: var(--sg-color-blue-400);
          }

          .train-log-time {
            color: var(--sg-color-gray-500);
            margin-right: 0.5rem;
          }

          @media (min-width: 30rem) {
            #train {
              font-size: 0.725rem;
            }

            .train-status,
            .train-config label,
            .train-config input,
            .train-actions button {
              font-size: 0.725rem;
            }

            .train-logs {
              font-size: 0.6875rem;
            }
          }

          @media (min-width: 48rem) {
            #train {
              font-size: 1rem;
            }

            .train-status {
              font-size: 0.875rem;
            }

            .train-config label,
            .train-config input {
              font-size: 0.875rem;
            }

            .train-config input {
              width: 6rem;
            }

            .train-actions button {
              font-size: 0.875rem;
              padding: 0.625rem 1.25rem;
            }

            .train-logs {
              font-size: 0.75rem;
            }
          }

          #stats,
          #controls,
          #settings,
          #inventory,
          .seed-controls {
            font-size: 0.5625rem;
          }

          /* Stats Panel */
          #stats {
            backdrop-filter: blur(0.3125rem);
            background: var(--sg-color-black-alpha-80);
            border-radius: 0.5rem;
            border: 0.0625rem solid var(--sg-color-gray-alpha-10);
            color: var(--sg-color-white);
          }

          /* Settings Panel */
          #settings {
            backdrop-filter: blur(0.3125rem);
            background: var(--sg-color-black-alpha-80);
            border-radius: 0.5rem;
            border: 0.0625rem solid var(--sg-color-gray-alpha-10);
            color: var(--sg-color-white);
            display: flex;
            flex-direction: column;
            position: relative;
          }

          #settings .settings-actions {
            display: flex;
            flex-direction: column;
          }

          #settings button {
            width: 100%;
          }

          .map-editor-controls_header,
          .map-editor-controls-section_header {
            margin-bottom: 0.3125rem;
          }

          .map-editor-controls-section {
            margin-top: 0.3125rem;
          }

          .map-editor-controls-section_buttons-container {
            display: grid;
            gap: 0.125rem;
            grid-template-columns: repeat(4, 1fr);
            margin-bottom: 0.625rem;
          }

          /* Inventory Panel */
          #inventory {
            backdrop-filter: blur(0.3125rem);
            background: var(--sg-color-black-alpha-80);
            border-radius: 0.5rem;
            border-top: 0.0625rem solid var(--sg-color-gray-500);
            border: 0.0625rem solid var(--sg-color-gray-alpha-10);
            bottom: 1.625rem;
            color: var(--sg-color-white);
          }

          /* Controls Panel */
          #controls {
            backdrop-filter: blur(0.3125rem);
            background: var(--sg-color-black-alpha-80);
            border-radius: 0.5rem;
            border: 0.0625rem solid var(--sg-color-gray-alpha-10);
            color: var(--sg-color-white);
          }

          .control-instruction {
            padding: 0.25rem 0;
          }

          .control-instruction_icon-container {
            align-items: center;
            display: flex;
          }

          .control-instruction_icon-container svg {
            padding-right: 0.25rem;
          }

          .touch-controls {
            bottom: var(--sg-ui-touch-controls-bottom, 6rem);
            pointer-events: none;
            position: var(--sg-ui-touch-controls-position, absolute);
            width: var(--sg-ui-touch-controls-width, 100%);
          }

          .touch-btn {
            align-items: center;
            backdrop-filter: blur(0.3125rem);
            background: var(--sg-ui-touch-btn-background-color);
            border-radius: 50%;
            border: 0.125rem solid var(--sg-ui-touch-btn-border-color);
            color: var(--sg-ui-touch-btn-color);
            cursor: pointer;
            display: inline-flex;
            font-size: 0.7rem;
            height: 3.25rem;
            justify-content: center;
            padding: 0.125rem;
            pointer-events: auto;
            touch-action: manipulation;
            width: 3.25rem;
            z-index: 3;
          }

          .touch-btn:active {
            background: var(--sg-color-gray-alpha-20);
            transform: scale(0.95);
          }

          #toastContainer {
            align-items: center;
            bottom: 1rem;
            display: flex;
            flex-direction: column;
            gap: 0.5rem;
            left: 50%;
            max-height: var(--sg-ui-toast-container-max-height);
            max-width: 90vw;
            max-width: 90dvw;
            overflow-y: var(--sg-ui-toast-container-overflow-y);
            pointer-events: none;
            position: fixed;
            transform: translateX(-50%);
            z-index: 9999;
          }

          .toast {
            align-items: flex-start;
            animation: toastSlideIn 0.3s cubic-bezier(0.68, -0.55, 0.27, 1.55);
            backdrop-filter: blur(0.5rem);
            background: var(--sg-color-black-alpha-80);
            border-radius: 0.5rem;
            border: 0.125rem solid var(--sg-color-gray-alpha-30);
            box-shadow: 0 0.25rem 0.75rem rgba(0, 0, 0, 0.3);
            color: var(--sg-color-white);
            display: flex;
            font-family: monospace;
            font-size: 0.875rem;
            gap: 0.75rem;
            justify-content: space-between;
            max-width: 25rem;
            min-width: 15rem;
            padding: 0.75rem 1rem;
            pointer-events: auto;
            position: relative;
            text-align: center;
          }

          .toast--fade-out {
            animation: toastFadeOut 0.2s ease-out forwards;
          }

          .toast--slide-out {
            animation: toastSlideOut 0.2s ease-out forwards;
          }

          .toast__content {
            flex: 1;
            line-height: 1.4;
          }

          .toast__close-btn {
            background: var(--sg-color-red-500);
            border-radius: 0.25rem;
            border: none;
            color: var(--sg-color-white);
            cursor: pointer;
            font-size: 1rem;
            height: 1.5rem;
            line-height: 1;
            padding: 0;
            transition: background 0.2s;
            width: 1.5rem;
          }

          .toast__close-btn:hover {
            background: var(--sg-color-red-500);
            opacity: 0.8;
          }

          .toast__close-btn:active {
            transform: scale(0.95);
          }

          @keyframes toastSlideIn {
            from {
              opacity: 0;
              transform: translateY(100%);
            }

            to {
              opacity: 1;
              transform: translateY(0);
            }
          }

          @keyframes toastFadeOut {
            from {
              opacity: 1;
            }

            to {
              opacity: 0;
            }
          }

          @keyframes toastSlideOut {
            from {
              opacity: 1;
              transform: translateY(0);
            }

            to {
              opacity: 0;
              transform: translateY(100%);
            }
          }

          .dpad-container {
            display: flex;
            flex-direction: row;
            justify-content: space-between;
            padding: 0 1rem 1rem 0.5rem;
          }

          .dpad {
            display: grid;
            gap: 0.3125rem;
            grid-template-columns: 3.125rem 3.125rem 3.125rem;
            grid-template-rows: 3.125rem 3.125rem 3.125rem;
            justify-content: left;
            position: relative;
            z-index: 3;
          }

          .dpad .up-left {
            grid-column: 1;
            grid-row: 1;
            margin-left: 0.5rem;
            margin-top: 0.5rem;
            padding: 0;
            z-index: 4;
          }

          .dpad .up {
            grid-column: 2;
            grid-row: 1;
            margin-left: 0.125rem;
          }

          .dpad .up-right {
            grid-column: 3;
            grid-row: 1;
            margin-right: 0.5rem;
            margin-top: 0.5rem;
            padding: 0;
            z-index: 4;
          }

          .dpad .left {
            grid-column: 1;
            grid-row: 2;
            margin-top: 0.125rem;
          }

          .dpad .middle {
            grid-column: 2;
            grid-row: 2;
            z-index: 2;
          }

          .dpad .right {
            grid-column: 3;
            grid-row: 2;
            margin-top: 0.125rem;
          }

          .dpad .down-left {
            grid-column: 1;
            grid-row: 3;
            margin-bottom: 0.5rem;
            margin-left: 0.5rem;
            padding: 0;
            z-index: 4;
          }

          .dpad .down {
            grid-column: 2;
            grid-row: 3;
            margin-left: 0.125rem;
          }

          .dpad .down-right {
            grid-column: 3;
            grid-row: 3;
            margin-bottom: 0.5rem;
            margin-right: 0.5rem;
            padding: 0;
            z-index: 4;
          }

          .action-buttons {
            display: flex;
            flex-wrap: wrap;
            gap: 0.625rem;
            justify-content: end;
          }

          /* Button Styles */
          button {
            background: var(--sg-color-green-500);
            border-radius: 0.25rem;
            border: none;
            color: var(--sg-color-white);
            cursor: pointer;
            font-size: 0.625rem;
            margin: 0.125rem;
            padding: 0.375rem 0.75rem;
            transition: background 0.2s;
          }

          button:hover,
          button:focus {
            outline: none;
          }

          button:active {
            transform: scale(0.95);
          }

          .material-btn,
          .seed-btn {
            border: 0.0625rem solid var(--sg-color-black);
            cursor: pointer;
            font-size: 0.5625rem;
            margin: 0.125rem;
            padding: 0.25rem 0.5rem;
          }

          .material-btn {
            background: var(--sg-color-gray-100);
            color: var(--sg-color-black);
          }

          .material-btn--cloud,
          .material-btn--grass,
          .material-btn--ice,
          .material-btn--iron,
          .material-btn--pumice,
          .material-btn--sand,
          .material-btn--snow {
            background: var(--sg-color-gray-800);
            color: var(--sg-color-white);
          }

          .material-btn:hover {
            background: var(--sg-color-gray-alpha-10);
            color: var(--sg-color-white);
          }

          .material-btn.selected,
          .seed-btn.selected {
            border: 0.125rem solid var(--sg-color-sky-50);
            outline: 0.125rem solid var(--sg-color-blue-500);
          }

          .seed-btn:hover {
            background: var(--sg-color-amber-900);
            color: var(--sg-color-white);
          }

          .seed-btn {
            background: var(--sg-color-amber-800);
            color: var(--sg-color-white);
          }

          .seed-btn--berry-bush {
            background: var(--sg-tile-berry-bush-color);
          }

          .seed-btn--bamboo {
            background: var(--sg-tile-bamboo-color);
            color: var(--sg-color-black);
          }

          .seed-btn--sunflower {
            background: var(--sg-tile-sunflower-color);
            color: var(--sg-color-black);
          }

          .seed-btn--corn {
            background: var(--sg-tile-corn-color);
            color: var(--sg-color-black);
          }

          .seed-btn--pine-tree {
            background: var(--sg-tile-pine-tree-color);
          }

          .seed-btn--willow-tree {
            background: var(--sg-tile-willow-tree-color);
            color: var(--sg-color-black);
          }

          .seed-btn--fern {
            background: var(--sg-tile-fern-color);
            color: var(--sg-color-black);
          }

          .seed-btn--tulip {
            background: var(--sg-tile-tulip-color);
          }

          .seed-btn--agave {
            color: var(--sg-color-black);
            background: var(--sg-tile-agave-color);
          }

          .seed-btn--lavender {
            background: var(--sg-tile-lavender-color);
          }

          .seed-btn--kelp {
            background: var(--sg-tile-kelp-color);
          }

          .seed-btn--rose {
            background: var(--sg-tile-rose-color);
          }

          .seed-btn--pumpkin {
            background: var(--sg-tile-pumpkin-color);
          }

          .seed-btn--lotus {
            color: var(--sg-color-black);
            background: var(--sg-tile-lotus-color);
          }

          .seed-btn--birch {
            color: var(--sg-color-black);
            background: var(--sg-tile-birch-color);
          }

          .info-buttons-container {
            border-top: 0.0625rem solid var(--sg-color-gray-500);
            margin-top: 0.9375rem;
            padding-top: 0.625rem;
          }

          #aboutBtn,
          #privacyBtn {
            border-radius: 0.25rem;
            border: none;
            color: var(--sg-color-white);
            cursor: pointer;
            margin: 0.25rem;
            padding: 0.5rem 1rem;
            width: calc(100% - 0.5rem);
          }

          #aboutBtn {
            background: var(--sg-color-green-500);
          }

          #privacyBtn {
            background: var(--sg-color-blue-500);
          }

          #resolution {
            display: block;
            margin: 0 0  0.25rem 0.125rem;
            width: 100%;
            position: relative;
          }

          /* Desktop Resolution Classes */
          .resolution-400 #canvas {
            border: 0.125rem solid var(--sg-color-gray-900);
            height: 25rem;
            margin: auto;
            width: 25rem;
          }

          .resolution-800 #canvas {
            border: 0.125rem solid (var(--sg-color-gray-900));
            height: 50rem;
            margin: auto;
            width: 50rem;
          }

          .resolution #gameContainer {
            background: var(--sg-color-black);
          }

          .seed-controls {
            background: rgba(0, 0, 0, 0.7);
            border-radius: 0.3125rem;
            bottom: 3rem;
            color: var(--sg-color-white);
            margin: 0.625rem 0;
            padding: 0.625rem;
            position: absolute;
            z-index: 2;
          }

          .seed-controls__actions {
            align-items: center;
            display: flex;
            flex-wrap: wrap;
            gap: 0.625rem;
          }

          .seed-controls__header {
            display: flex;
            gap: 0.625rem;
            justify-content: space-between;
            margin-bottom: 0.625rem;
          }

          .seed-controls h4, .seed-controls__header h4 {
            color: var(--sg-color-white);
            margin: 0 0 0.625rem 0;
          }

          .seed-controls__header button {
            background: var(--sg-color-red-500);
            border-radius: 0.25rem;
            border: none;
            color: white;
            cursor: pointer;
            margin: 0 0 0.625rem 0;
            padding: 0.3125rem 0.625rem;
          }

          .seed-controls input {
            font-size: 0.75rem;
            margin-left: 0.3125rem;
            padding: 0.125rem 0.3125rem;
            width: 5rem;
            font-size: 0.5rem;
          }

          .seed-controls input:focus {
            outline: 0.125rem solid var(--sg-color-blue-400);
          }

          .seed-controls__save-load {
            display: flex;
            flex-direction: column;
            margin-bottom: 0.5rem;
          }

          #saveModeToggle {
            background: var(--sg-color-blue-500);
          }

          .tile-btn {
            border-radius: 0.125rem;
            border: 0.0625rem solid var(--sg-color-gray-500);
            cursor: pointer;
            font-size: 0.625rem;
            padding: 0.25rem 0.375rem;
            transition: all 0.2s;
          }

          .tile-btn--air {
            background: var(--sg-tile-air-color);
            color: var(--sg-color-black);
          }

          .tile-btn--sand {
            background: var(--sg-tile-sand-color);
            color: var(--sg-color-black);
          }

          .tile-btn--birch {
            color: var(--sg-color-black);
          }

          .tile-btn--clay {
            background: var(--sg-tile-clay-color);
          }

          .tile-btn--cloud,
          .tile-btn--gold,
          .tile-btn--grass,
          .tile-btn--ice,
          .tile-btn--pumice,
          .tile-btn--snow {
            color: var(--sg-color-gray-900);
          }

          .tile-btn--cloud {
            background: var(--sg-tile-cloud-color);
          }

          .tile-btn--gold {
            background: var(--sg-tile-gold-color);
          }

          .tile-btn--grass {
            background: var(--sg-tile-grass-color);
          }

          .tile-btn--ice {
            background: var(--sg-tile-ice-color);
          }

          .tile-btn--pumice {
            background: var(--sg-tile-pumice-color);
          }

          .tile-btn--snow {
            background: var(--sg-tile-snow-color);
          }

          .tile-btn--coal,
          .tile-btn--dirt,
          .tile-btn--lava,
          .tile-btn--mushroom,
          .tile-btn--stone,
          .tile-btn--tree-leaves,
          .tile-btn--tree-trunk,
          .tile-btn--water {
            color: var(--sg-color-white);
          }

          .tile-btn--iron {
            background: var(--sg-tile-iron-color);
          }

          .tile-btn--wheat {
            background: var(--sg-tile-wheat-color);
            color: var(--sg-color-black);
          }

          .tile-btn--carrot {
            background: var(--sg-tile-carrot-color);
          }

          .tile-btn--cactus {
            background: var(--sg-tile-cactus-color);
            color: var(--sg-color-black);
          }

          .tile-btn--tulip {
            background: var(--sg-tile-tulip-color);
          }

          .tile-btn--agave {
            background: var(--sg-tile-agave-color);
            color: var(--sg-color-black);
          }

          .tile-btn--lavender {
            background: var(--sg-tile-lavender-color);
          }

          .tile-btn--kelp {
            background: var(--sg-tile-kelp-color);
          }

          .tile-btn--rose {
            background: var(--sg-tile-rose-color);
          }

          .tile-btn--pumpkin {
            background: var(--sg-tile-pumpkin-color);
          }

          .tile-btn--lotus {
            background: var(--sg-tile-lotus-color);
            color: var(--sg-color-black);
          }

          .tile-btn--birch {
            background: var(--sg-tile-birch-color);
          }

          .tile-btn--coal {
            background: var(--sg-tile-coal-color);
          }

          .tile-btn--dirt {
            background: var(--sg-tile-dirt-color);
          }

          .tile-btn--lava {
            background: var(--sg-tile-lava-color);
          }

          .tile-btn--mushroom {
            background: var(--sg-tile-mushroom-color);
          }

          .tile-btn--stone {
            background: var(--sg-tile-stone-color);
          }

          .tile-btn--tree_trunk {
            background: var(--sg-tile-tree-trunk-color);
          }

          .tile-btn--tree_leaves {
            background: var(--sg-tile-tree-leaves-color);
          }

          .tile-btn--water {
            background: var(--sg-tile-water-color);
          }

          .tile-btn:hover {
            border-color: var(--sg-color-gray-800);
            transform: scale(1.05);
          }

          .tile-btn.selected {
            border: 0.125rem solid var(--sg-color-sky-50);
            outline: 0.125rem solid var(--sg-color-blue-500);
            font-weight: bold;
          }

          #mapEditor,
          #examplesBtnContainer {
            border-top: 0.0625rem solid var(--sg-color-gray-500);
            margin-top: 0.625rem;
            padding-top: 0.625rem;
          }

          #mapEditorControls {
            display: flex;
            flex-direction: column;
            margin-top: 0.3125rem;
            padding-right: 1rem;
          }

          #mapEditorControls div {
            margin-bottom: 0.5rem;
          }

          #brushSizeSelect {
            margin-left: 0.3125rem;
            padding: 0.125rem;
          }

          #generateWithSeed {
            background: var(--sg-color-blue-400);
          }

          #randomSeed {
            background: var(--sg-color-amber-500);
          }

          #copySeed {
            background: var(--sg-color-emerald-700);
          }

          .seed-controls button {
            border-radius: 0.1875rem;
            border: none;
            color: var(--sg-color-white);
            cursor: pointer;
            font-size: 0.75rem;
            padding: 0.3125rem 0.625rem;
          }

          .seed-controls button:hover {
            opacity: 0.8;
            transform: translateY(-0.0625rem);
          }

          .seed-controls button:active {
            transform: translateY(0);
          }

          .current-seed {
            color: var(--sg-color-gray-600);
            font-size: 0.6875rem;
            margin-top: 0.5rem;
          }

          .swatch-cloud,
          .swatch-grass,
          .swatch-ice,
          .swatch-kelp,
          .swatch-pumice,
          .swatch-snow {
            display: inline-block;
            height: 0.625rem;
            width: 0.625rem;
          }

          .swatch-cloud {
            background: var(--sg-tile-cloud-color);
          }

          .swatch-grass {
            background: var(--sg-tile-grass-color);
          }

          .swatch-kelp {
            background: var(--sg-tile-kelp-color);
          }

          .swatch-ice {
            background: var(--sg-tile-ice-color);
          }

          .swatch-pumice {
            background: var(--sg-tile-pumice-color);
          }

          .swatch-snow {
            background: var(--sg-tile-snow-color);
          }

          .about,
          .examples,
          .privacy {
            background: var(--sg-color-white);
            line-height: 1.5;
            overflow: auto;
            padding: 0.5rem 1rem 1rem 2rem;
          }

          dialog.about-content,
          dialog.examples-content,
          dialog.privacy-content {
            background: var(--sg-color-gray-50);
            border-radius: 0.5rem;
            border: 0.125rem solid var(--sg-color-gray-900);
            color: var(--sg-color-gray-900);
            font-family: monospace;
            line-height: 1.5;
            max-height: 80vh;
            max-height: 80dvh;
            max-width: 50rem;
            width: 90%;
          }

          .about-content li,
          .examples-content li,
          .privacy-content li {
            margin: 0.25rem 0;
          }

          .about-content_header,
          .examples-content_header,
          .privacy-content_header {
            align-items: center;
            display: flex;
            justify-content: space-between;
            margin-bottom: 1rem;
            padding-top: 1rem;
          }

          .about-content_close-btn,
          .examples-content_close-btn,
          .privacy-content_close-btn {
            background: var(--sg-color-red-500);
            border-radius: 0.25rem;
            border: none;
            color: var(--sg-color-white);
            cursor: pointer;
            font-size: 1.2rem;
            padding: 0.5rem 1rem;
          }

          .about-controls {
            background: var(--sg-color-gray-300);
            border-radius: 0.25rem;
            list-style-type: none;
            margin-bottom: 1.5rem;
            margin-top: 0.5rem;
            padding: 1rem;
          }

          /* Mobile Responsive */
          @media (min-width: 30rem) {
            .ui-grid__corner--heading {
              font-size: 1rem;
            }

            #stats,
            #controls,
            #settings,
            #inventory,
            .seed-controls {
              font-size: 0.725rem;
            }

            .touch-btn {
              font-size: 0.75rem;
            }

            .seed-controls {
              font-size: 0.6875rem;
            }

            .seed-controls button {
              font-size: 0.6875rem;
              padding: 0.25rem 0.5rem;
            }

            .seed-controls input {
              width: 3.75rem;
            }
          }

          @media (min-width: 48rem) {
            .ui-grid__corner--heading {
              font-size: 1.25rem;
            }

            #stats,
            #controls,
            #settings,
            #inventory,
            .seed-controls {
              font-size: 1.25rem;
            }

            .touch-btn {
              font-size: 0.875rem;
            }
          }
        </style>

        <div id="loading">
          <div class="pixel pixel-one"></div>
          <div class="pixel pixel-two"></div>
          <div class="pixel pixel-three"></div>
          <div class="pixel pixel-four"></div>
          <div class="pixel pixel-five"></div>
          <div class="pixel pixel-six"></div>
          <div class="pixel pixel-seven"></div>
          <div class="pixel pixel-eight"></div>
          <div class="pixel pixel-nine"></div>
        </div>
        <canvas id="canvas" tabindex="0"></canvas>
        <div id="ui-grid" class="ui-grid">
          <div class="ui-grid__corner ui-grid__corner--top-left">
            <div id="stats">
              <div class="ui-grid__corner--heading">\u{1F331} Sprite Garden</div>
              <div class="ui-grid__corner--container" hidden="hidden">
                <div>Hudud <span id="currentBiome">O\u2018rmon</span></div>
                <div>Chuqurlik <span id="currentDepth">Yer usti</span></div>
                <div>Urug\u2018lar <span id="seedCount">0</span></div>
                <div>Vaqt <span id="gameTime">0</span> s</div>

                <div class="info-buttons-container">
                  <button id="aboutBtn">\u2139\uFE0F O\u2018yin haqida</button>
                  <button id="privacyBtn">\u{1F512} Maxfiylik</button>
                </div>
              </div>
            </div>
          </div>

          <div class="ui-grid__corner ui-grid__corner--top-right">
            <div id="settings">
              <div class="ui-grid__corner--heading">\u2699\uFE0F Sozlamalar</div>
              <div class="ui-grid__corner--container" hidden="hidden">
                <div class="settings-actions">
                  <div id="resolution">
                    <sprite-garden-select id="resolutionSelect" value="400">
                      <sprite-garden-option value="400">400x400</sprite-garden-option>
                      <sprite-garden-option value="800">800x800</sprite-garden-option>
                      <sprite-garden-option hidden="hidden" value="fullscreen">Fullscreen</sprite-garden-option>
                    </sprite-garden-select>
                  </div>
                  <button id="worldState">\u{1F30D} Dunyo sozlamalari</button>
                  <button id="toggleView">
                    \u{1F50D} <span id="viewModeText">View Normal</span>
                  </button>
                  <button id="toggleFog">
                    \u2601 <span id="fogModeText">Fog</span>
                  </button>
                  <button id="toggleBreakMode">
                    \u26CF\uFE0F <span id="breakModeText">Dig Regular</span>
                  </button>
                </div>

                <div class="settings-actions">
                  <div id="customizeColorsBtnContainer">
                    <div class="ui-grid__corner--sub-heading">\u{1F5FA}\uFE0F Colors</div>
                    <button id="customizeColorsBtn">Customize</button>
                  </div>
                  <div id="examplesBtnContainer" hidden="hidden">
                    <div class="ui-grid__corner--sub-heading">\u{1F5FA}\uFE0F Examples</div>
                    <button id="examplesBtn">
                      \u{1F4DD} <span id="examplesBtnText">Open</span>
                    </button>
                  </div>
                  <div id="mapEditor" hidden="hidden">
                    <div class="ui-grid__corner--sub-heading">
                      \u{1F5FA}\uFE0F Map Editor
                    </div>
                    <button id="toggleMapEditor">
                      \u{1F4DD} <span id="mapEditorText">Enable Editor</span>
                    </button>
                    <div
                      class="ui-grid__corner--sub-section"
                      id="mapEditorControls"
                      hidden="hidden"
                    >
                      <div>
                        Brush Size:
                        <select id="brushSizeSelect">
                          <option value="1">1x1</option>
                          <option value="3">3x3</option>
                          <option value="5">5x5</option>
                        </select>
                      </div>
                      <div class="map-editor-controls-section">
                        <div class="map-editor-controls-section_header">
                          \u{1F30D} Terrain:
                        </div>
                        <div
                          class="map-editor-controls-section_buttons-container"
                        >
                          <button
                            class="tile-btn tile-btn--air"
                            data-tile="AIR"
                          >
                            Air
                          </button>
                          <button
                            class="tile-btn tile-btn--dirt"
                            data-tile="DIRT"
                          >
                            Dirt
                          </button>
                          <button
                            class="tile-btn tile-btn--grass"
                            data-tile="GRASS"
                          >
                            Grass
                          </button>
                          <button
                            class="tile-btn tile-btn--stone"
                            data-tile="STONE"
                          >
                            Stone
                          </button>
                          <button
                            class="tile-btn tile-btn--sand"
                            data-tile="SAND"
                          >
                            Sand
                          </button>
                          <button
                            class="tile-btn tile-btn--clay"
                            data-tile="CLAY"
                          >
                            Clay
                          </button>
                          <button
                            class="tile-btn tile-btn--snow"
                            data-tile="SNOW"
                          >
                            Snow
                          </button>
                          <button
                            class="tile-btn tile-btn--ice"
                            data-tile="ICE"
                          >
                            Ice
                          </button>
                          <button
                            class="tile-btn tile-btn--cloud"
                            data-tile="CLOUD"
                          >
                            Cloud
                          </button>
                        </div>
                        <div class="map-editor-controls_header">
                          \u{1F3D4}\uFE0F Resources:
                        </div>
                        <div
                          class="map-editor-controls-section_buttons-container"
                        >
                          <button
                            class="tile-btn tile-btn--coal"
                            data-tile="COAL"
                          >
                            Coal
                          </button>
                          <button
                            class="tile-btn tile-btn--iron"
                            data-tile="IRON"
                          >
                            Iron
                          </button>
                          <button
                            class="tile-btn tile-btn--gold"
                            data-tile="GOLD"
                          >
                            Gold
                          </button>
                          <button
                            class="tile-btn tile-btn--pumice"
                            data-tile="PUMICE"
                          >
                            Pumice
                          </button>
                        </div>
                        <div class="map-editor-controls_header">\u{1F333} Nature:</div>
                        <div
                          class="map-editor-controls-section_buttons-container"
                        >
                          <button
                            class="tile-btn tile-btn--tree_trunk"
                            data-tile="TREE_TRUNK"
                          >
                            Trunk
                          </button>
                          <button
                            class="tile-btn tile-btn--tree_leaves"
                            data-tile="TREE_LEAVES"
                          >
                            Leaves
                          </button>
                          <button
                            class="tile-btn tile-btn--water"
                            data-tile="WATER"
                          >
                            Water
                          </button>
                          <button
                            class="tile-btn tile-btn--lava"
                            data-tile="LAVA"
                          >
                            Lava
                          </button>
                        </div>
                        <div class="map-editor-controls_header">\u{1F331} Crops:</div>
                        <div
                          class="map-editor-controls-section_buttons-container"
                        >
                          <button
                            class="tile-btn tile-btn--wheat"
                            data-tile="WHEAT"
                          >
                            Wheat
                          </button>
                          <button
                            class="tile-btn tile-btn--carrot"
                            data-tile="CARROT"
                          >
                            Carrot
                          </button>
                          <button
                            class="tile-btn tile-btn--mushroom"
                            data-tile="MUSHROOM"
                          >
                            Mushroom
                          </button>
                          <button
                            class="tile-btn tile-btn--cactus"
                            data-tile="CACTUS"
                          >
                            Cactus
                          </button>
                          <button
                            class="tile-btn tile-btn--tulip"
                            data-tile="TULIP"
                          >
                            Tulip
                          </button>
                          <button
                            class="tile-btn tile-btn--agave"
                            data-tile="AGAVE"
                          >
                            Agave
                          </button>
                          <button
                            class="tile-btn tile-btn--lavender"
                            data-tile="LAVENDER"
                          >
                            Lavender
                          </button>
                          <button
                            class="tile-btn tile-btn--kelp"
                            data-tile="KELP"
                          >
                            Kelp
                          </button>
                          <button
                            class="tile-btn tile-btn--rose"
                            data-tile="ROSE"
                          >
                            Rose
                          </button>
                          <button
                            class="tile-btn tile-btn--pumpkin"
                            data-tile="PUMPKIN"
                          >
                            Pumpkin
                          </button>
                          <button
                            class="tile-btn tile-btn--lotus"
                            data-tile="LOTUS"
                          >
                            Lotus
                          </button>
                          <button
                            class="tile-btn tile-btn--birch"
                            data-tile="BIRCH"
                          >
                            Birch
                          </button>
                        </div>
                      </div>
                      <div>
                        <button id="clearMapEditor">\u{1F9F9} Clear All</button>
                        <button id="fillMapEditor">\u{1FAA3} Fill Layer</button>
                        <button id="saveMapAsState">\u{1F4BE} Save as State</button>
                        <button id="regenerateMap">\u{1F30D} Regenerate Map</button>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div class="ui-grid__corner ui-grid__corner--bottom-left">
            <div id="controls">
              <div class="ui-grid__corner--heading">\u2328\uFE0F Boshqaruv</div>
              <div class="ui-grid__corner--container" hidden="hidden">
                <div class="control-instruction">W / \u2191 / Space \u2014 sakrash</div>
                <div class="control-instruction">A / D yoki \u2190 / \u2192 \u2014 yurish</div>
                <div class="control-instruction">F \u2014 ekish va hosil yig\u2018ish</div>
                <div class="control-instruction">R \u2014 qazish</div>
                <div class="control-instruction">E / X / K \u2014 rejim / tezlik / masofa</div>
                <div class="control-instruction">
                  <span style="font-family: monospace;">u</span> / i / o / j / l / m / , / .: Set Block
                </div>
                <div class="control-instruction">ctrl+s: World State</div>
                <div class="control-instruction">Mouse: Inspect Tiles</div>
                <div
                  class="control-instruction control-instruction_icon-container"
                >
                  <svg
                    xmlns="http://www.w3.org/2000/svg"
                    height="24px"
                    viewBox="0 -960 960 960"
                    width="24px"
                    fill="var(--sg-color-gray-200)"
                  >
                    <path
                      d="M419-80q-28 0-52.5-12T325-126L107-403l19-20q20-21 48-25t52 11l74 45v-328q0-17 11.5-28.5T340-760q17 0 29 11.5t12 28.5v472l-97-60 104 133q6 7 14 11t17 4h221q33 0 56.5-23.5T720-240v-160q0-17-11.5-28.5T680-440H461v-80h219q50 0 85 35t35 85v160q0 66-47 113T640-80H419ZM167-620q-13-22-20-47.5t-7-52.5q0-83 58.5-141.5T340-920q83 0 141.5 58.5T540-720q0 27-7 52.5T513-620l-69-40q8-14 12-28.5t4-31.5q0-50-35-85t-85-35q-50 0-85 35t-35 85q0 17 4 31.5t12 28.5l-69 40Zm335 280Z"
                    />
                  </svg>
                  Mobile Controls
                </div>
              </div>
            </div>
          </div>

          <div class="ui-grid__corner ui-grid__corner--bottom-right">
            <div id="inventory">
              <div class="ui-grid__corner--heading" id="seeds">
                \u{1F392} Inventory
              </div>
              <div class="ui-grid__corner--container" hidden="hidden">
                <div class="ui-grid__corner--sub-heading">\u{1FAD8} Seeds</div>
                <div class="ui-grid__corner--sub-section">
                  <div>Selected: <span id="selectedSeed">None</span></div>
                  <div>
                    <button class="seed-btn" data-wheat="data-wheat">
                      \u{1F33E} <span id="wheatCount">0</span>
                    </button>
                    <button class="seed-btn" data-carrot="data-carrot">
                      \u{1F955} <span id="carrotCount">0</span>
                    </button>
                    <button class="seed-btn" data-mushroom="data-mushroom">
                      \u{1F344} <span id="mushroomCount">0</span>
                    </button>
                    <button class="seed-btn" data-cactus="data-cactus">
                      \u{1F335} <span id="cactusCount">0</span>
                    </button>
                    <button class="seed-btn" data-walnut="data-walnut">
                      \u{1F330} <span id="walnutCount">0</span>
                    </button>
                    <button
                      class="seed-btn seed-btn--berry-bush"
                      data-berry_bush="data-berry_bush"
                    >
                      \u{1F353} <span id="berry_bushCount">0</span>
                    </button>

                    <button
                      class="seed-btn seed-btn--bamboo"
                      data-bamboo="data-bamboo"
                    >
                      \u{1F38B} <span id="bambooCount">0</span>
                    </button>

                    <button
                      class="seed-btn seed-btn--sunflower"
                      data-sunflower="data-sunflower"
                    >
                      \u{1F33B} <span id="sunflowerCount">0</span>
                    </button>

                    <button
                      class="seed-btn seed-btn--corn"
                      data-corn="data-corn"
                    >
                      \u{1F33D} <span id="cornCount">0</span>
                    </button>

                    <button
                      class="seed-btn seed-btn--pine-tree"
                      data-pine_tree="data-pine_tree"
                    >
                      \u{1F332} <span id="pine_treeCount">0</span>
                    </button>

                    <button
                      class="seed-btn seed-btn--willow-tree"
                      data-willow_tree="data-willow_tree"
                    >
                      \u{1F333} <span id="willow_treeCount">0</span>
                    </button>

                    <button
                      class="seed-btn seed-btn--fern"
                      data-fern="data-fern"
                    >
                      \u{1F33F} <span id="fernCount">0</span>
                    </button>
                    <button
                      class="seed-btn seed-btn--tulip"
                      data-tulip="data-tulip"
                    >
                      \u{1F337} <span id="tulipCount">0</span>
                    </button>
                    <button
                      class="seed-btn seed-btn--agave"
                      data-agave="data-agave"
                    >
                      \u{1F331} <span id="agaveCount">0</span>
                    </button>
                    <button
                      class="seed-btn seed-btn--lavender"
                      data-lavender="data-lavender"
                    >
                      \u{1F33A} <span id="lavenderCount">0</span>
                    </button>
                    <button
                      class="seed-btn seed-btn--kelp"
                      data-kelp="data-kelp"
                    >
                      <div class="swatch-kelp"></div>
                      <span id="kelpCount">0</span>
                    </button>
                    <button
                      class="seed-btn seed-btn--rose"
                      data-rose="data-rose"
                    >
                      \u{1F338} <span id="roseCount">0</span>
                    </button>
                    <button
                      class="seed-btn seed-btn--pumpkin"
                      data-pumpkin="data-pumpkin"
                    >
                      \u{1F383} <span id="pumpkinCount">0</span>
                    </button>
                    <button
                      class="seed-btn seed-btn--lotus"
                      data-lotus="data-lotus"
                    >
                      \u{1FAB7} <span id="lotusCount">0</span>
                    </button>
                    <button
                      class="seed-btn seed-btn--birch"
                      data-birch="data-birch"
                    >
                      \u{1FAB5} <span id="birchCount">0</span>
                    </button>
                  </div>
                </div>

                <div id="materials">
                  <div class="ui-grid__corner--sub-heading">\u{1F9F1} Materials</div>
                  <div class="ui-grid__corner--sub-section">
                    <div>Selected: <span id="selectedMaterial">None</span></div>
                    <div>
                      <button
                        class="material-btn material-btn--dirt"
                        data-dirt="data-dirt"
                      >
                        \u{1F7E4} <span id="dirtCount">0</span>
                      </button>
                      <button
                        class="material-btn material-btn--stone"
                        data-stone="data-stone"
                      >
                        \u{1FAA8} <span id="stoneCount">0</span>
                      </button>

                      <button
                        class="material-btn material-btn--grass"
                        data-grass="data-grass"
                      >
                        <div class="swatch-grass"></div>
                        <span id="grassCount">0</span>
                      </button>

                      <button
                        class="material-btn material-btn--snow"
                        data-snow="data-snow"
                      >
                        \u2744\uFE0F <span id="snowCount">0</span>
                      </button>

                      <button
                        class="material-btn material-btn--ice"
                        data-ice="data-ice"
                      >
                        \u{1F9CA} <span id="iceCount">0</span>
                      </button>

                      <button
                        class="material-btn material-btn--wood"
                        data-wood="data-wood"
                      >
                        \u{1FAB5} <span id="woodCount">0</span>
                      </button>
                      <button
                        class="material-btn material-btn--sand"
                        data-sand="data-sand"
                      >
                        \u{1F3D6}\uFE0F <span id="sandCount">0</span>
                      </button>

                      <button
                        class="material-btn material-btn--clay"
                        data-clay="data-clay"
                      >
                        \u{1F9F1} <span id="clayCount">0</span>
                      </button>
                      <button
                        class="material-btn material-btn--coal"
                        data-coal="data-coal"
                      >
                        \u26AB <span id="coalCount">0</span>
                      </button>

                      <button
                        class="material-btn material-btn--iron"
                        data-iron="data-iron"
                      >
                        \u{1F529} <span id="ironCount">0</span>
                      </button>
                      <button
                        class="material-btn material-btn--gold"
                        data-gold="data-gold"
                      >
                        \u{1F7E1} <span id="goldCount">0</span>
                      </button>
                      <button
                        class="material-btn material-btn--pumice"
                        data-pumice="data-pumice"
                      >
                        <div class="swatch-pumice"></div>
                        <span id="pumiceCount">0</span>
                      </button>
                      <button
                        class="material-btn material-btn--cloud"
                        data-cloud="data-cloud"
                      >
                        \u2601\uFE0F <span id="cloudCount">0</span>
                      </button>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
        <div class="ui-grid__corner ui-grid__corner--center" id="trainPanel" hidden="hidden">
          <div id="train">
            <div class="ui-grid__corner--heading">\u{1F916} Training</div>
            <div class="ui-grid__corner--container" hidden="hidden">

              <!-- Status Section -->
              <div class="train-section">
                <div class="ui-grid__corner--sub-heading">\u{1F4CA} Status</div>
                <div class="train-status">
                  <div>Episode: <span id="trainEpisode">0</span> / <span id="trainTotalEpisodes">0</span></div>
                  <div>Best Time: <span id="trainBestTime">--</span>s</div>
                  <div>Q-Table Size: <span id="trainQTableSize">0</span></div>
                  <div>Epsilon: <span id="trainEpsilon">0.00</span></div>
                  <div>Seeds: <span id="seedsPlanted">0</span> / <span id="seedsLeftToPlant">0</span></div>
                  <div>Streak: <span id="trainSuccessStreak">0</span> \u2705 | <span id="trainConsecutiveFailures">0</span> \u274C</div>
                  <div>Action: <span id="trainAction"></span></div>
                </div>
              </div>

              <!-- Training Controls -->
              <div class="train-section">
                <div class="ui-grid__corner--sub-heading">\u2699\uFE0F Configuration</div>
                <div class="train-config">
                  <label>
                    Episodes:
                    <input id="trainEpisodes" type="number" value="50" min="1" max="1000" />
                  </label>
                  <label>
                    Max Steps:
                    <input id="trainMaxSteps" type="number" value="100" min="10" max="1000" />
                  </label>
                  <label>
                    Learning Rate (\u03B1):
                    <input id="trainAlpha" type="number" value="0.2" step="0.01" min="0" max="1" />
                  </label>
                  <label>
                    Discount (\u03B3):
                    <input id="trainGamma" type="number" value="0.95" step="0.01" min="0" max="1" />
                  </label>
                  <label>
                    Decay:
                    <input id="trainDecay" type="number" value="0.995" step="0.001" min="0.9" max="1" />
                  </label>
                  <label>
                    Epsilon (\u03B5):
                    <input id="trainEpsilonInput" type="number" value="0.6" step="0.01" min="0" max="1" />
                  </label>
                  <label>
                    Test Epsilon (\u03B5):
                    <input id="testEpsilonInput" type="number"value="0.25" step="0.01" min="0" max="1" />
                  </label>
                </div>
              </div>

              <!-- Action Buttons -->
              <div class="train-section">
                <div class="train-actions">
                  <button id="trainStartBtn">\u{1F680} Start Training</button>
                  <button id="trainStopBtn" disabled>\u23F9 Stop</button>
                  <button id="trainTestBtn">\u{1F9EA} Test Agent</button>
                  <button id="trainExportBtn">\u{1F4BE} Export Q-Table</button>
                  <button id="trainImportBtn">\u{1F4E5} Import Q-Table</button>
                  <button id="trainCopyLogsBtn">\u{1F4CB} Copy Logs</button>
                  <button id="trainClearLogsBtn">\u{1F9F9} Clear Logs</button>
                </div>
              </div>

              <!-- Logs Section -->
              <div class="train-section">
                <div class="ui-grid__corner--sub-heading">\u{1F4DD} Logs</div>
                <div id="trainLogs" class="train-logs"></div>
              </div>

            </div>
          </div>
        </div>
        <div class="seed-controls" hidden="hidden">
          <div class="seed-controls__header">
            <h4>Dunyo yaratish</h4>
            <button id="closeWorldGeneration">
              &times;
            </button>
          </div>
          <div class="seed-controls__actions">
            <label>
              Dunyo kodi
              <input
                id="worldSeedInput"
                placeholder="Kodni kiriting"
                type="number"
              />
            </label>

            <button id="generateWithSeed">Yaratish</button>
            <button id="randomSeed">Tasodifiy dunyo</button>
            <button id="copySeed">Kodni nusxalash</button>
          </div>

          <div class="current-seed">
            <p>Joriy kod <span id="currentSeed"></span></p>
          </div>

          <h4>Saqlash va yuklash</h4>

          <div class="seed-controls__save-load">
            <button id="openStorageBtn">\u{1F5C4}\uFE0F Saqlangan dunyolar</button>
            <button id="saveExternalGameFile">\u{1F5C3}\uFE0F Faylga saqlash</button>
            <button id="loadExternalGameFile">\u{1F4BE} Fayldan ochish</button>
          </div>

          <h4 class="seed-controls__header seed-controls--share" hidden>Share</h4>

          <div class="seed-controls__save-load seed-controls--share" hidden>
            <button id="shareExternalGameFile">\u{1F30D} Share Game File From Disk</button>
          </div>

          <h4>Mode</h4>

          <div class="seed-controls__save-load">
            <button id="saveModeToggle">Qo\u2018lda saqlash</button>
          </div>
        </div>

        <div class="touch-controls" hidden="hidden">
          <div class="dpad-container">
            <div class="dpad">
              <div class="touch-btn up-left" data-key="upleft">\u2196</div>
              <div class="touch-btn up" data-key="w">\u2191</div>
              <div class="touch-btn up-right" data-key="upright">\u2197</div>

              <div class="touch-btn left" data-key="a">\u2190</div>
              <div class="touch-btn middle" data-key="x">&times;</div>
              <div class="touch-btn right" data-key="d">\u2192</div>

              <div class="touch-btn down-left" data-key="downleft">\u2199</div>
              <div class="touch-btn down" data-key="s">\u2193</div>
              <div class="touch-btn down-right" data-key="downright">\u2198</div>
            </div>
            <div class="dpad">
              <div class="touch-btn place-block up-left" data-key="u">\u2196</div>
              <div class="touch-btn place-block up" data-key="i">\u2191</div>
              <div class="touch-btn place-block up-right" data-key="o">\u2197</div>

              <div class="touch-btn place-block left" data-key="j">\u2190</div>
              <div class="touch-btn place-block middle" data-key="k">
                &times;
              </div>
              <div class="touch-btn place-block right" data-key="l">\u2192</div>

              <div class="touch-btn place-block down-left" data-key="m">\u2199</div>
              <div class="touch-btn place-block down" data-key=",">\u2193</div>
              <div class="touch-btn place-block down-right" data-key=".">
                \u2198
              </div>
            </div>
          </div>
          <div>
            <div class="action-buttons">
              <div class="touch-btn" data-key=" ">\u{1F998}</div>
              <div class="touch-btn" data-key="r">\u26CF\uFE0F</div>
              <div class="touch-btn" data-key="f">\u{1F9D1}\u200D\u{1F33E}</div>
            </div>
          </div>
        </div>

        <div id="toastContainer"></div>
      `,this.attachShadow({mode:"open"}).appendChild(t.content.cloneNode(!0))}}async connectedCallback(){let t=this.shadowRoot,o=t.querySelector("canvas");await en(globalThis,t,o)}async disconnectedCallback(){await Ee(globalThis)}};globalThis.customElements?.get(on)||globalThis.customElements?.define(on,Yt);export{Yt as SpriteGarden,on as tagName};
