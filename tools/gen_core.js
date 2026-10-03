'use strict';
/* 建物テンプレート生成の共通部分。出力は Sponge Schematic v2(.schem) で、WorldEditでも読めます。 */
const fs=require('fs'),zlib=require('zlib'),path=require('path');
const DATA_VERSION=3120; // Minecraft 1.19.2

const COLORS={
  white_concrete:'#cfd5d6',light_gray_concrete:'#8e8e86',gray_concrete:'#36393d',black_concrete:'#0b0d12',
  light_blue_concrete:'#2389c6',blue_concrete:'#2c2f8f',red_concrete:'#8e2121',yellow_concrete:'#f1af15',lime_concrete:'#5ea918',
  orange_concrete:'#e06101',green_concrete:'#495b24',cyan_concrete:'#157788',brown_concrete:'#603c20',pink_concrete:'#d5658f',
  purple_concrete:'#641f9c',magenta_concrete:'#a9309f',
  glass:'#a9d8e6',light_blue_stained_glass:'#7fb8e0',gray_stained_glass:'#5d6168',tinted_glass:'#2b2733',blue_stained_glass:'#4a63c4',
  black_stained_glass:'#1d1f24',cyan_stained_glass:'#4c8c99',green_stained_glass:'#66a04a',
  glass_pane:'#a9d8e6',light_blue_stained_glass_pane:'#7fb8e0',gray_stained_glass_pane:'#5d6168',
  smooth_stone:'#9e9e9e',stone:'#7d7d7d',stone_bricks:'#7a7a7a',polished_andesite:'#848a84',andesite:'#888888',cobblestone:'#7b7b7b',
  smooth_quartz:'#ece8e2',quartz_block:'#ebe6df',quartz_pillar:'#e9e4dc',chiseled_quartz_block:'#e8e3da',
  sea_lantern:'#c7e0dc',iron_bars:'#9a9a9a',iron_block:'#d8d8d8',iron_door:'#cfcfcf',iron_trapdoor:'#c6c6c6',lightning_rod:'#c47a50',
  grass_block:'#6a9f3e',oak_leaves:'#3f7f2a',birch_leaves:'#6f9a45',oak_log:'#6b5430',dirt_path:'#9a8049',coarse_dirt:'#77553b',
  smooth_sandstone:'#dfd3a5',sandstone:'#dbcf9f',cut_sandstone:'#dacd9d',white_terracotta:'#d1b2a1',red_terracotta:'#8f3d2e',
  orange_terracotta:'#a15325',terracotta:'#985e43',light_gray_terracotta:'#876b62',cyan_terracotta:'#575b5b',brown_terracotta:'#4d3323',
  water:'#3a6fd9',oak_planks:'#a2824f',spruce_planks:'#745a34',dark_oak_planks:'#432b13',birch_planks:'#c4b17a',
  deepslate_tiles:'#363636',polished_deepslate:'#484848',polished_blackstone:'#353039',blackstone:'#2a2429',
  copper_block:'#c06b4f',oxidized_copper:'#52a386',weathered_copper:'#6d936b',exposed_copper:'#a07d68',
  bookshelf:'#8f6a3a',barrel:'#8a6a3d',oak_fence:'#a2824f',stripped_oak_log:'#b09056',red_wool:'#a12722',white_wool:'#e9ecec',
  blue_wool:'#35399d',yellow_wool:'#f8c627',green_wool:'#667a28',orange_wool:'#f07613',gray_wool:'#3e4447',light_gray_wool:'#8e8e86',
  black_wool:'#15151a',white_carpet:'#e9ecec',gray_carpet:'#3e4447',red_carpet:'#a12722',light_blue_carpet:'#3aafd9',
  redstone_lamp:'#8a5b3a',glowstone:'#e0b66a',shroomlight:'#f09a4c',light_gray_stained_glass:'#8e8e86',
  mud_bricks:'#8a6e57',bricks:'#966454',podzol:'#5c4021',sand:'#dbd3a0',gravel:'#837f7e'
};
function stripProps(s){const i=s.indexOf('[');return i<0?s:s.slice(0,i);}
function colorOf(state){
  const n=stripProps(state);if(COLORS[n])return COLORS[n];
  const base=n.replace(/_(slab|stairs|wall|fence|door|trapdoor|button|pressure_plate|carpet)$/,'');
  for(const c of [base,base+'_block',base+'s',base+'_planks'])if(COLORS[c])return COLORS[c];
  return '#ff00ff';
}

/* ---------- ブロック状態の組み立て ---------- */
const st=(n,f,half)=>`${n}[facing=${f},half=${half||'bottom'},shape=straight,waterlogged=false]`;
const sl=(n,t)=>`${n}[type=${t||'bottom'},waterlogged=false]`;
const door=(n,f,half,hinge)=>`${n}[facing=${f},half=${half},hinge=${hinge||'left'},open=false,powered=false]`;
const trap=(n,f,half,open)=>`${n}[facing=${f},half=${half||'bottom'},open=${!!open},powered=false,waterlogged=false]`;
const leaves=n=>`${n}[persistent=true]`;
const log=(n,axis)=>`${n}[axis=${axis||'y'}]`;
const PANE_RE=/(^iron_bars$|glass_pane$)/;

class V{
  constructor(w,h,l){this.w=w;this.h=h;this.l=l;this.a=new Uint16Array(w*h*l);this.pal=['air'];this.pm=new Map([['air',0]]);}
  id(s){let i=this.pm.get(s);if(i===undefined){i=this.pal.length;this.pal.push(s);this.pm.set(s,i);}return i;}
  ok(x,y,z){return x>=0&&y>=0&&z>=0&&x<this.w&&y<this.h&&z<this.l;}
  idx(x,y,z){return (y*this.l+z)*this.w+x;}
  set(x,y,z,s){if(this.ok(x,y,z))this.a[this.idx(x,y,z)]=this.id(s);}
  get(x,y,z){return this.ok(x,y,z)?this.pal[this.a[this.idx(x,y,z)]]:'air';}
  clear(x,y,z){this.set(x,y,z,'air');}
  fill(x0,y0,z0,x1,y1,z1,s){for(let y=y0;y<=y1;y++)for(let z=z0;z<=z1;z++)for(let x=x0;x<=x1;x++)this.set(x,y,z,s);}
  ring(x0,y0,z0,x1,y1,z1,s){for(let y=y0;y<=y1;y++)for(let z=z0;z<=z1;z++)for(let x=x0;x<=x1;x++)if(x===x0||x===x1||z===z0||z===z1)this.set(x,y,z,s);}
  /* 窓や鉄格子の接続を後処理で決める */
  connect(){
    const isSolid=n=>{const b=stripProps(n);if(b==='air')return false;
      if(PANE_RE.test(b))return true;
      if(/(slab|stairs|door|trapdoor|rod|carpet|button|plate|torch|lantern|fence|wall|bars)$/.test(b))return false;return true;};
    const dirs=[['north',0,-1],['east',1,0],['south',0,1],['west',-1,0]];
    for(let y=0;y<this.h;y++)for(let z=0;z<this.l;z++)for(let x=0;x<this.w;x++){
      const s=this.get(x,y,z),b=stripProps(s);if(!PANE_RE.test(b))continue;
      const p=dirs.map(([d,dx,dz])=>`${d}=${isSolid(this.get(x+dx,y,z+dz))}`).join(',');
      this.set(x,y,z,`${b}[${p},waterlogged=false]`);
    }
  }
}

/* ---------- 書き出し ---------- */
class Buf{
  constructor(){this.a=new Uint8Array(1<<16);this.n=0;}
  need(k){if(this.n+k>this.a.length){let l=this.a.length*2;while(l<this.n+k)l*=2;const t=new Uint8Array(l);t.set(this.a.subarray(0,this.n));this.a=t;}}
  u8(v){this.need(1);this.a[this.n++]=v&255;}i16(v){this.u8(v>>8);this.u8(v);}
  i32(v){this.u8(v>>>24);this.u8(v>>>16);this.u8(v>>>8);this.u8(v);}
  str(s){const e=Buffer.from(s,'utf8');this.i16(e.length);this.need(e.length);this.a.set(e,this.n);this.n+=e.length;}
  bytes(u){this.need(u.length);this.a.set(u,this.n);this.n+=u.length;}out(){return this.a.slice(0,this.n);}
}
function varint(b,v){while(v>=128){b.u8((v&127)|128);v>>>=7;}b.u8(v);}
function schemBytes(v,name){
  v.connect();
  const bd=new Buf();
  for(let y=0;y<v.h;y++)for(let z=0;z<v.l;z++)for(let x=0;x<v.w;x++)varint(bd,v.a[v.idx(x,y,z)]);
  const w=new Buf();
  w.u8(10);w.str('Schematic');
  w.u8(3);w.str('Version');w.i32(2);
  w.u8(3);w.str('DataVersion');w.i32(DATA_VERSION);
  w.u8(2);w.str('Width');w.i16(v.w);w.u8(2);w.str('Height');w.i16(v.h);w.u8(2);w.str('Length');w.i16(v.l);
  w.u8(3);w.str('PaletteMax');w.i32(v.pal.length);
  w.u8(10);w.str('Palette');
  v.pal.forEach((s,i)=>{w.u8(3);w.str('minecraft:'+s);w.i32(i);});
  w.u8(0);
  const b=bd.out();w.u8(7);w.str('BlockData');w.i32(b.length);w.bytes(b);
  w.u8(11);w.str('Offset');w.i32(3);w.i32(0);w.i32(0);w.i32(0);
  w.u8(10);w.str('Metadata');w.u8(8);w.str('Name');w.str(name);w.u8(8);w.str('Author');w.str('CityBuilder');w.u8(0);
  w.u8(0);
  return zlib.gzipSync(Buffer.from(w.out()),{level:9});
}
/* 真上から見た色(WebUIのサムネイル用) */
function thumbnail(v){
  const chars='abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789',pal={},pm={};let rows=[];
  for(let z=0;z<v.l;z++){let r='';
    for(let x=0;x<v.w;x++){
      let s='air';for(let y=v.h-1;y>=0;y--){s=v.get(x,y,z);if(s!=='air')break;}
      if(s==='air'){r+='.';continue;}
      const c=colorOf(s);if(!pm[c]){pm[c]=chars[Object.keys(pm).length];pal[pm[c]]=c;}r+=pm[c];}
    rows.push(r);}
  return {top:rows.join('/'),pal};
}
const OUT=path.join(__dirname,'..','server-data','citybuilder');
const catalog=[],webCatalog=[];
function emit(id,meta,v){
  fs.mkdirSync(path.join(OUT,'templates'),{recursive:true});
  fs.writeFileSync(path.join(OUT,'templates',id+'.schem'),schemBytes(v,id));
  const margin=meta.margin!==undefined?meta.margin:((/^station_/.test(id)||['住宅','オフィス','商業','公共','産業'].includes(meta.cat))?2:0);
  const e={id,name:meta.name,category:meta.cat,size:[v.w,v.h,v.l],margin,description:meta.desc||''};
  if(meta.floors)e.floors=meta.floors;
  catalog.push(e);
  webCatalog.push(Object.assign({},e,thumbnail(v)));
}
function finish(){
  fs.writeFileSync(path.join(OUT,'catalog.json'),JSON.stringify({version:1,buildings:catalog},null,2));
  fs.writeFileSync(path.join(__dirname,'catalog.web.json'),JSON.stringify({version:1,buildings:webCatalog}));
  console.log('templates:',catalog.length);
}
module.exports={V,emit,finish,st,sl,door,trap,leaves,log,COLORS,colorOf};
