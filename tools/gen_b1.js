'use strict';
/* 住宅・オフィス・商業の建物(すべて現代建築。正面は南=+Z) */
const {V,emit,st,sl,door,trap,leaves,log}=require('./gen_core');
const M=2; // 敷地の周りの歩道(ブロック)
function rng(seed){let a=seed|0;return()=>{a=a+0x6D2B79F5|0;let t=Math.imul(a^a>>>15,1|a);t=t+Math.imul(t^t>>>7,61|t)^t;return((t^t>>>14)>>>0)/4294967296;};}

function lot(v,paving){
  v.fill(0,0,0,v.w-1,0,v.l-1,paving||'smooth_stone');
}
function planter(v,x,z,w,l){
  for(let i=0;i<w;i++)for(let j=0;j<l;j++){v.set(x+i,0,z+j,'grass_block[snowy=false]');v.set(x+i,1,z+j,leaves('oak_leaves'));}
}
function roofKit(v,x0,z0,x1,z1,y,seed,o){
  const r=rng(seed);o=o||{};
  const w=x1-x0+1,l=z1-z0+1;
  // 室外機
  for(let i=0,n=Math.max(2,Math.min(8,(w*l/30)|0));i<n;i++){
    const x=x0+1+Math.floor(r()*Math.max(1,w-4)),z=z0+1+Math.floor(r()*Math.max(1,l-3));
    v.fill(x,y,z,x+1,y,z,'light_gray_concrete');v.set(x,y+1,z,trap('iron_trapdoor','south','bottom',false));
    v.set(x+1,y+1,z,trap('iron_trapdoor','south','bottom',false));
  }
  // 貯水槽
  if(o.tank&&w>=8&&l>=8){const tx=x1-4,tz=z0+2;v.fill(tx,y,tz,tx+2,y+2,tz+2,'light_gray_concrete');v.fill(tx,y+3,tz,tx+2,y+3,tz+2,'gray_concrete');}
  // 塔屋(階段室)
  if(o.stair&&w>=7&&l>=7){v.fill(x0+2,y,z0+2,x0+5,y+3,z0+5,'white_concrete');v.fill(x0+2,y+4,z0+2,x0+5,y+4,z0+5,'gray_concrete');
    v.set(x0+3,y+1,z0+5,door('iron_door','south','lower','left'));v.set(x0+3,y+2,z0+5,door('iron_door','south','upper','left'));}
  // アンテナと航空障害灯
  const ax=(x0+x1)>>1,az=z1-2;
  if(o.antenna){for(let k=0;k<o.antenna;k++)v.set(ax,y+k,az,'iron_bars');v.set(ax,y+o.antenna,az,'red_concrete');v.set(ax,y+o.antenna+1,az,'lightning_rod[facing=up,powered=false,waterlogged=false]');}
}

/* 柱と窓で構成された外壁の階層 */
function shaft(v,o){
  const {x0,z0,x1,z1}=o,fh=o.fh||4,gh=o.gh||fh,P=o.pillar||4;
  const s=[o.y0];let y=o.y0;
  for(let k=0;k<o.floors;k++){y+=(k===0&&o.lobby)?gh:fh;s.push(y);}
  const top=s[o.floors],cx=(x0+x1)>>1;
  const wall=o.wall,band=o.band||o.wall,glass=o.glass,pane=!!o.pane,gl=pane?`${glass}_pane`:glass;
  const isPillar=(x,z)=>{
    const onX=(x===x0||x===x1),onZ=(z===z0||z===z1);
    if(onX&&onZ)return true;
    if(onZ)return (x-x0)%P===0;
    return (z-z0)%P===0;
  };
  v.fill(x0+1,s[0]+1,z0+1,x1-1,top-1,z1-1,'air');
  for(let k=0;k<o.floors;k++){
    const lo=s[k],hi=s[k+1],ground=(k===0&&o.lobby);
    for(let z=z0;z<=z1;z++)for(let x=x0;x<=x1;x++){
      if(x!==x0&&x!==x1&&z!==z0&&z!==z1)continue;
      const pil=isPillar(x,z);
      for(let yy=lo+1;yy<hi;yy++){
        let b;
        if(pil)b=o.pillarBlock||wall;
        else if(o.apartment&&!ground){b=(yy===lo+1)?wall:gl;}
        else if(ground&&o.groundWall&&!(z===z1&&Math.abs(x-cx)<=3))b=(yy>=hi-2&&!pil)?gl:wall;
        else b=gl;
        v.set(x,yy,z,b);
      }
    }
    // 床スラブ(周囲はスパンドレル帯)
    for(let z=z0;z<=z1;z++)for(let x=x0;x<=x1;x++){
      const edge=(x===x0||x===x1||z===z0||z===z1);
      v.set(x,hi,z,edge?band:(o.floorBlock||'smooth_stone'));
    }
    // バルコニー(南面)
    if(o.balcony&&k>=1&&z1+1<v.l-1){
      for(let x=x0+1;x<=x1-1;x++){v.set(x,lo,z1+1,sl('smooth_quartz_slab','bottom'));v.set(x,lo+1,z1+1,'iron_bars');}
      v.set(x0+1,lo+1,z1,'air');
    }
  }
  // 内部の照明とコア
  if(o.core!==false){const w=x1-x0,l=z1-z0;if(w>=8&&l>=6)v.fill(cx-1,s[0]+1,((z0+z1)>>1)-1,cx+1,top,((z0+z1)>>1)+1,'light_gray_concrete');}
  for(let k=1;k<=o.floors;k++)for(let x=x0+2;x<=x1-2;x+=5)for(let z=z0+2;z<=z1-2;z+=5){
    if(v.get(x,s[k],z)==='light_gray_concrete')continue;v.set(x,s[k],z,'sea_lantern');}
  // 玄関
  if(o.lobby&&o.entrance!==false){
    for(let yy=1;yy<=gh-1;yy++){for(const x of [cx,cx+1])v.set(x,s[0]+yy,z1,'air');}
    for(const [x,h] of [[cx,'left'],[cx+1,'right']]){v.set(x,s[0]+1,z1,door('iron_door','south','lower',h));v.set(x,s[0]+2,z1,door('iron_door','south','upper',h));}
    for(let yy=3;yy<=gh-1;yy++)for(const x of [cx,cx+1])v.set(x,s[0]+yy,z1,gl);
    v.fill(cx-3,s[0]+gh,z1+1,cx+4,s[0]+gh,z1+2,o.canopy||'light_gray_concrete');
    v.set(cx-2,s[0]+gh,z1+2,'sea_lantern');v.set(cx+3,s[0]+gh,z1+2,'sea_lantern');
    v.fill(cx-1,0,z1+1,cx+2,0,z1+M,'polished_andesite');
  }
  return {top,s};
}

function tower(id,meta,o){
  const bw=o.bw,bd=o.bd,fh=o.fh||4,gh=o.gh||5;
  const W=bw+2*M,L=bd+2*M,up=o.upper;
  const total=gh+(o.floors-1)*fh+(up?up.floors*fh:0);
  const v=new V(W,total+14,L);
  lot(v,o.paving);
  const x0=M,z0=M,x1=M+bw-1,z1=M+bd-1;
  v.fill(x0,0,z0,x1,0,z1,'polished_andesite');
  planter(v,0,L-1,2,1);planter(v,W-2,L-1,2,1);
  const base=shaft(v,Object.assign({x0,z0,x1,z1,y0:0,floors:o.floors,fh,gh,lobby:true},o.look));
  let roofY=base.top,rx0=x0,rz0=z0,rx1=x1,rz1=z1;
  if(up){
    const i=up.inset;
    v.ring(x0,roofY+1,z0,x1,roofY+1,z1,o.look.wall);
    const u=shaft(v,Object.assign({},o.look,{x0:x0+i,z0:z0+i,x1:x1-i,z1:z1-i,y0:roofY,floors:up.floors,fh,lobby:false,balcony:false,core:o.look.core}));
    roofY=u.top;rx0=x0+i;rz0=z0+i;rx1=x1-i;rz1=z1-i;
    v.ring(rx0,roofY+1,rz0,rx1,roofY+1,rz1,o.look.wall);
  }else v.ring(x0,roofY+1,z0,x1,roofY+1,z1,o.look.wall);
  roofKit(v,rx0+1,rz0+1,rx1-1,rz1-1,roofY+1,o.seed||7,o.roof||{tank:true,stair:true,antenna:4});
  emit(id,meta,v);
}

const GLASS_BLUE='light_blue_stained_glass',GLASS_GRAY='gray_stained_glass';

function buildResidential(){
  tower('apt_mid_01',{name:'マンション(8階・白)',cat:'住宅',desc:'白い壁と南向きバルコニーの中層マンション',floors:8},
    {bw:14,bd:12,floors:8,seed:11,look:{wall:'white_concrete',band:'light_gray_concrete',glass:GLASS_BLUE,pane:true,apartment:true,balcony:true,pillar:5}});
  tower('apt_mid_02',{name:'マンション(12階・ベージュ)',cat:'住宅',desc:'ベージュ系の外壁の中層マンション',floors:12},
    {bw:16,bd:12,floors:12,seed:12,look:{wall:'smooth_sandstone',band:'white_concrete',glass:GLASS_GRAY,pane:true,apartment:true,balcony:true,pillar:4,pillarBlock:'white_concrete'}});
  tower('apt_high_01',{name:'タワーマンション(26階)',cat:'住宅',desc:'上層が少し絞られた高層タワーマンション',floors:20},
    {bw:20,bd:16,floors:20,upper:{inset:2,floors:6},seed:13,look:{wall:'light_gray_concrete',band:'white_concrete',glass:GLASS_GRAY,pane:true,apartment:true,balcony:true,pillar:5,pillarBlock:'white_concrete'},roof:{stair:true,tank:true,antenna:8}});
  tower('apt_low_01',{name:'低層アパート(3階)',cat:'住宅',desc:'3階建ての賃貸アパート',floors:3},
    {bw:16,bd:10,floors:3,gh:4,seed:14,look:{wall:'white_concrete',band:'gray_concrete',glass:GLASS_BLUE,pane:true,apartment:true,balcony:true,pillar:4,core:false,groundWall:true},roof:{antenna:0}});
  // 団地(細長い中層住棟)
  tower('danchi_01',{name:'団地(5階建て住棟)',cat:'住宅',desc:'細長い5階建ての住棟。何棟か並べて団地になります',floors:5},
    {bw:36,bd:10,floors:5,gh:4,seed:15,look:{wall:'white_concrete',band:'light_gray_concrete',glass:GLASS_BLUE,pane:true,apartment:true,balcony:true,pillar:6,core:false,groundWall:true,entrance:true,canopy:'gray_concrete'},roof:{antenna:0}});
  houseModern('house_mod_01',{name:'モダン住宅A(白)',cat:'住宅',desc:'白い箱型の2階建て住宅',floors:2},{light:true});
  houseModern('house_mod_02',{name:'モダン住宅B(黒)',cat:'住宅',desc:'黒い外壁とカーポートのある住宅',floors:2},{light:false});
  terrace('terrace_01',{name:'テラスハウス(4戸)',cat:'住宅',desc:'2階建てが4戸つながったテラスハウス',floors:2});
}
function hollow(v,x0,y0,z0,x1,y1,z1,b){
  v.fill(x0,y0,z0,x1,y1,z1,b);v.fill(x0+1,y0,z0+1,x1-1,y1,z1-1,'air');
}
function houseModern(id,meta,o){
  const bw=12,bd=10,extra=o.light?0:7,W=bw+2*M+extra,L=bd+2*M,v=new V(W,13,L);
  lot(v);
  const x0=M,z0=M,x1=M+bw-1,z1=M+bd-1;
  const wall=o.light?'white_concrete':'gray_concrete',acc=o.light?'oak_planks':'light_gray_concrete',slab=o.light?'light_gray_concrete':'black_concrete';
  const glass='light_blue_stained_glass';
  for(let x=0;x<W;x++)v.set(x,0,L-1,'grass_block[snowy=false]');
  v.fill(x0,0,z0,x1,0,z1,'polished_andesite');
  // 1階
  hollow(v,x0,1,z0,x1,3,z1,wall);
  v.fill(x0,4,z0,x1,4,z1,wall);v.fill(x0+1,4,z0+1,x1-1,4,z1-1,'smooth_stone');
  // 2階(東側を1階屋上テラスにする)
  const sx1=x1-4;
  hollow(v,x0,5,z0,sx1,8,z1,wall);
  v.fill(x0,9,z0,sx1,9,z1,slab);v.ring(x0,10,z0,sx1,10,z1,wall);
  // テラスの手すり
  for(let x=sx1+1;x<=x1;x++)v.set(x,5,z1,'iron_bars');
  for(let z=z0;z<=z1;z++)v.set(x1,5,z,'iron_bars');
  for(let x=sx1+1;x<=x1;x++)v.set(x,5,z0,'iron_bars');
  // 窓
  v.fill(x0+1,2,z1,x1-5,3,z1,glass);
  v.fill(x0+1,6,z1,sx1-1,8,z1,glass);
  for(let z=z0+2;z<=z1-2;z+=3){v.fill(x1,2,z,x1,3,Math.min(z+1,z1-1),glass);v.fill(x0,6,z,x0,7,Math.min(z+1,z1-1),glass);v.fill(x0,2,z,x0,3,Math.min(z+1,z1-1),glass);}
  v.fill(sx1,6,z0+2,sx1,8,z1-3,glass);
  // 玄関と木目のアクセント壁
  const dx=x1-2;
  v.fill(x1-3,1,z1,x1-1,3,z1,acc);
  v.set(dx,1,z1,door('iron_door','south','lower','left'));v.set(dx,2,z1,door('iron_door','south','upper','left'));
  v.fill(x1-4,4,z1+1,x1,4,z1+1,slab);v.set(dx,4,z1+1,'sea_lantern');
  v.fill(x1-3,0,z1+1,x1-1,0,z1+M,'polished_andesite');
  // 照明
  v.set((x0+x1)>>1,4,(z0+z1)>>1,'sea_lantern');v.set(x0+3,9,z0+3,'sea_lantern');v.set(x0+3,9,z1-3,'sea_lantern');
  if(!o.light){ // カーポート
    const cx0=x1+3,cx1=W-3;
    v.fill(cx0,5,z0+2,cx1,5,z1,slab);
    for(const [x,z] of [[cx0,z0+2],[cx1,z0+2],[cx0,z1],[cx1,z1]])v.fill(x,1,z,x,4,z,'light_gray_concrete');
    v.fill(cx0,0,z0+2,cx1,0,z1,'gray_concrete');
    for(let z=z0+2;z<=z1;z+=2)v.set(cx0-1,0,z,'white_concrete');
  }
  emit(id,meta,v);
}
function terrace(id,meta){
  const units=4,uw=5,bw=units*uw,bd=9,W=bw+2*M,L=bd+2*M,v=new V(W,16,L);lot(v);
  const x0=M,z0=M,x1=M+bw-1,z1=M+bd-1,cols=['white_concrete','light_gray_concrete','white_concrete','light_gray_concrete'];
  v.fill(x0,0,z0,x1,0,z1,'polished_andesite');
  for(let u=0;u<units;u++){
    const ux0=x0+u*uw,ux1=ux0+uw-1,c=cols[u];
    v.fill(ux0,1,z0,ux1,8,z1,c);v.fill(ux0+1,1,z0+1,ux1-1,3,z1-1,'air');v.fill(ux0+1,5,z0+1,ux1-1,7,z1-1,'air');
    v.fill(ux0+1,4,z0+1,ux1-1,4,z1-1,'smooth_stone');v.fill(ux0,9,z0,ux1,9,z1,'gray_concrete');
    v.fill(ux0+1,2,z1,ux1-1,3,z1,'light_blue_stained_glass');v.fill(ux0+1,6,z1,ux1-1,7,z1,'light_blue_stained_glass');
    const dx=ux0+1;v.set(dx,1,z1,'air');v.set(dx,2,z1,'air');v.set(dx,1,z1,door('iron_door','south','lower','right'));v.set(dx,2,z1,door('iron_door','south','upper','right'));
    v.set(ux0+2,3,z1,c);v.set(ux0+2,2,z1,c);v.set(ux0+3,2,z1,'light_blue_stained_glass');
    v.fill(ux0,5,z1+1,ux1,5,z1+1,sl('smooth_quartz_slab','bottom'));v.fill(ux0+1,6,z1+1,ux1-1,6,z1+1,'iron_bars');
    v.set(ux0+2,4,z1+1,'sea_lantern');
    v.set(ux0+2,4,(z0+z1)>>1,'sea_lantern');
  }
  v.fill(x0,10,z0,x1,10,z0,'white_concrete');
  planter(v,0,L-1,3,1);planter(v,W-3,L-1,3,1);
  emit(id,meta,v);
}

function buildOffice(){
  tower('office_01',{name:'オフィスビル(10階)',cat:'オフィス',desc:'青いガラスのカーテンウォールのオフィス',floors:10},
    {bw:16,bd:16,floors:10,seed:21,look:{wall:'light_gray_concrete',band:'gray_concrete',glass:GLASS_BLUE,pane:false,pillar:4,pillarBlock:'white_concrete'},roof:{stair:true,tank:false,antenna:5}});
  tower('office_02',{name:'オフィスビル(18階・グレー)',cat:'オフィス',desc:'色付きガラスで上層が絞られたオフィスビル',floors:14},
    {bw:20,bd:18,floors:14,upper:{inset:2,floors:4},seed:22,look:{wall:'gray_concrete',band:'black_concrete',glass:'tinted_glass',pane:false,pillar:5,pillarBlock:'light_gray_concrete'},roof:{stair:true,antenna:7}});
  tower('office_03',{name:'超高層オフィス(32階)',cat:'オフィス',desc:'段状に絞られた超高層ビル',floors:24},
    {bw:24,bd:24,floors:24,upper:{inset:3,floors:8},seed:23,look:{wall:'white_concrete',band:'light_gray_concrete',glass:GLASS_GRAY,pane:false,pillar:6,pillarBlock:'white_concrete'},roof:{stair:true,antenna:10}});
  // 雑居ビル
  const bw=9,bd=10,W=bw+2*M,L=bd+2*M,v=new V(W,32,L);lot(v);
  const x0=M,z0=M,x1=M+bw-1,z1=M+bd-1;
  const sh=shaft(v,{x0,z0,x1,z1,y0:0,floors:6,fh:4,gh:4,lobby:true,wall:'light_gray_concrete',band:'white_concrete',glass:GLASS_BLUE,pane:true,apartment:false,pillar:4,core:false,groundWall:false});
  const cols=['red_concrete','yellow_concrete','blue_concrete','green_concrete','orange_concrete'];
  for(let k=1;k<6;k++)v.fill(x0+1,sh.s[k]+1,z1+1,x1-1,sh.s[k]+2,z1+1,cols[(k-1)%5]);
  for(let k=1;k<6;k++)v.fill(x0+1,sh.s[k]+1,z1+1,x1-1,sh.s[k]+2,z1+1,cols[(k-1)%5]);
  v.ring(x0,sh.top+1,z0,x1,sh.top+1,z1,'light_gray_concrete');
  roofKit(v,x0+1,z0+1,x1-1,z1-1,sh.top+1,24,{antenna:3});
  emit('office_small_01',{name:'雑居ビル(6階)',cat:'商業',desc:'色とりどりの看板が付いた駅前の雑居ビル',floors:6},v);
}

/* コンビニ・店舗・ロードサイド */
function convenience(id,meta,col){
  const bw=14,bd=10,W=bw+2*M+8,L=bd+2*M+10,v=new V(W,10,L);
  v.fill(0,0,0,W-1,0,L-1,'gray_concrete');
  v.fill(0,0,0,W-1,0,1,'smooth_stone');v.fill(0,0,0,1,0,L-1,'smooth_stone');v.fill(W-2,0,0,W-1,0,L-1,'smooth_stone');
  const x0=4,z0=3,x1=x0+bw-1,z1=z0+bd-1;
  v.fill(x0,0,z0,x1,0,z1,'polished_andesite');
  v.fill(x0,1,z0,x1,4,z1,'white_concrete');v.fill(x0+1,1,z0+1,x1-1,3,z1-1,'air');
  v.fill(x0,4,z0,x1,4,z1,'white_concrete');v.fill(x0,5,z0,x1,5,z1,col[0]);v.fill(x0,5,z0,x1,5,z0,col[0]);
  v.ring(x0,6,z0,x1,6,z1,'white_concrete');v.fill(x0+1,5,z0+1,x1-1,5,z1-1,'light_gray_concrete');
  v.fill(x0,4,z0,x1,4,z1,col[1]);v.fill(x0+1,4,z0+1,x1-1,4,z1-1,'smooth_stone');
  // 正面ガラス
  v.fill(x0+1,1,z1,x1-1,3,z1,'glass');v.fill(x0,1,z1,x0,3,z1,'white_concrete');v.fill(x1,1,z1,x1,3,z1,'white_concrete');
  v.fill(x0+1,1,z1,x0+1,3,z1,'white_concrete');
  for(const x of [x0+6,x0+7]){for(let y=1;y<=2;y++)v.clear(x,y,z1);}
  v.set(x0+6,1,z1,door('iron_door','south','lower','left'));v.set(x0+6,2,z1,door('iron_door','south','upper','left'));
  v.set(x0+7,1,z1,door('iron_door','south','lower','right'));v.set(x0+7,2,z1,door('iron_door','south','upper','right'));
  // 看板(発光)
  v.fill(x0+1,5,z1+1,x1-1,5,z1+1,'sea_lantern');v.fill(x0+1,6,z1+1,x1-1,6,z1+1,col[0]);
  v.fill(x0,4,z1+1,x1,4,z1+1,col[1]);
  // 棚・照明
  for(let x=x0+3;x<=x1-3;x+=3){v.fill(x,1,z0+3,x,2,z1-4,'bookshelf');}
  for(let x=x0+2;x<=x1-2;x+=3)v.set(x,4,(z0+z1)>>1,'sea_lantern');
  // 駐車場の白線
  for(let x=x0;x<=x1;x+=3)for(let z=z1+3;z<=z1+8;z++)v.set(x,0,z,'white_concrete');
  // ごみ箱・自販機
  v.fill(x1+1,1,z1,x1+1,2,z1,'red_concrete');v.set(x1+1,2,z1,'light_blue_stained_glass');
  emit(id,meta,v);
}
function gasStation(){
  const W=34,L=28,v=new V(W,12,L);
  v.fill(0,0,0,W-1,0,L-1,'gray_concrete');v.fill(0,0,0,W-1,1,1,'smooth_stone');
  v.fill(0,0,0,W-1,0,1,'smooth_stone');
  // 店舗
  v.fill(4,1,3,15,4,10,'white_concrete');v.fill(5,1,4,14,3,9,'air');v.fill(4,5,3,15,5,10,'red_concrete');
  v.fill(5,1,10,14,3,10,'glass');v.set(9,1,10,door('iron_door','south','lower','left'));v.set(9,2,10,door('iron_door','south','upper','left'));
  v.set(9,1,10,'air');v.set(9,2,10,'air');v.set(9,1,10,door('iron_door','south','lower','left'));v.set(9,2,10,door('iron_door','south','upper','left'));
  v.fill(4,0,3,15,0,10,'polished_andesite');
  // キャノピー
  const cx0=6,cx1=28,cz0=14,cz1=24;
  v.fill(cx0,6,cz0,cx1,6,cz1,'white_concrete');v.fill(cx0,7,cz0,cx1,7,cz1,'red_concrete');
  v.fill(cx0+1,6,cz0+1,cx1-1,6,cz1-1,'light_gray_concrete');
  for(let x=cx0+3;x<=cx1-3;x+=4)for(let z=cz0+3;z<=cz1-3;z+=4)v.set(x,6,z,'sea_lantern');
  for(const [x,z] of [[cx0+2,cz0+2],[cx1-2,cz0+2],[cx0+2,cz1-2],[cx1-2,cz1-2]])v.fill(x,1,z,x,5,z,'white_concrete');
  // 給油機アイランド
  for(const x of [cx0+8,cx0+14]){v.fill(x-1,1,cz0+3,x+1,1,cz1-3,'light_gray_concrete');v.fill(x-1,0,cz0+3,x+1,0,cz1-3,'smooth_stone');
    for(const z of [cz0+5,cz0+8]){v.fill(x,2,z,x,3,z,'red_concrete');v.set(x,3,z,'black_concrete');}}
  v.fill(cx0,0,cz0,cx1,0,cz0,'white_concrete');
  // 看板ポール
  v.fill(2,1,25,2,9,25,'iron_bars');v.fill(1,10,25,3,10,25,'red_concrete');v.fill(1,11,25,3,11,25,'sea_lantern');
  emit('gas_station_01',{name:'ガソリンスタンド',cat:'商業',desc:'キャノピーと給油機のあるロードサイド施設'},v);
}
function mall(){
  const bw=44,bd=32,W=bw+2*M+4,L=bd+2*M+8,v=new V(W,26,L);
  v.fill(0,0,0,W-1,0,L-1,'gray_concrete');v.fill(0,0,0,W-1,0,2,'smooth_stone');
  const x0=M+2,z0=M,x1=x0+bw-1,z1=z0+bd-1;
  const sh=shaft(v,{x0,z0,x1,z1,y0:0,floors:3,fh:6,gh:6,lobby:true,wall:'white_concrete',band:'light_gray_concrete',glass:GLASS_BLUE,pane:false,pillar:6,core:false,groundWall:false,canopy:'white_concrete'});
  v.fill(x0,sh.top+1,z0,x1,sh.top+1,z1,'gray_concrete');v.ring(x0,sh.top+2,z0,x1,sh.top+2,z1,'white_concrete');
  for(let x=x0+6;x<=x1-6;x+=6)v.fill(x,sh.s[1]+1,z1+1,x,sh.s[1]+3,z1+1,'sea_lantern');
  v.fill(x0+12,1,z1+1,x1-12,1,z1+1,'air');v.fill(x0+10,sh.s[2],z1+1,x1-10,sh.s[2]+2,z1+1,'blue_concrete');
  // 中央の吹き抜け
  v.fill(x0+14,sh.s[1],z0+10,x1-14,sh.s[1],z1-10,'glass');v.fill(x0+14,sh.s[2],z0+10,x1-14,sh.s[2],z1-10,'glass');
  // 駐車場
  for(let x=x0;x<=x1;x+=4)for(let z=z1+4;z<=L-3;z++)v.set(x,0,z,'white_concrete');
  roofKit(v,x0+2,z0+2,x1-2,z1-2,sh.top+3,31,{tank:true,stair:true});
  emit('mall_01',{name:'ショッピングモール',cat:'商業',desc:'3階建ての大型商業施設。正面に駐車場付き',floors:3},v);
}
function supermarket(){
  const bw=30,bd=22,W=bw+2*M+4,L=bd+2*M+8,v=new V(W,12,L);
  v.fill(0,0,0,W-1,0,L-1,'gray_concrete');v.fill(0,0,0,W-1,0,2,'smooth_stone');
  const x0=M+2,z0=M,x1=x0+bw-1,z1=z0+bd-1;
  v.fill(x0,0,z0,x1,0,z1,'polished_andesite');
  v.fill(x0,1,z0,x1,6,z1,'white_concrete');v.fill(x0+1,1,z0+1,x1-1,5,z1-1,'air');
  v.fill(x0,7,z0,x1,7,z1,'light_gray_concrete');v.ring(x0,8,z0,x1,8,z1,'white_concrete');
  v.fill(x0+3,2,z1,x1-3,5,z1,'glass');v.fill(x0+3,5,z1+1,x1-3,6,z1+1,'green_concrete');v.fill(x0+3,4,z1+1,x1-3,4,z1+1,'sea_lantern');
  const cx=(x0+x1)>>1;for(const x of [cx,cx+1]){for(let y=1;y<=3;y++)v.clear(x,y,z1);}
  v.set(cx,1,z1,door('iron_door','south','lower','left'));v.set(cx,2,z1,door('iron_door','south','upper','left'));
  v.set(cx+1,1,z1,door('iron_door','south','lower','right'));v.set(cx+1,2,z1,door('iron_door','south','upper','right'));
  for(let x=x0+3;x<=x1-3;x+=4)v.set(x,6,(z0+z1)>>1,'sea_lantern');
  for(let x=x0+6;x<=x1-6;x+=4)v.fill(x,1,z0+4,x,2,z1-6,'bookshelf');
  for(let x=x0;x<=x1;x+=4)for(let z=z1+3;z<=L-3;z++)v.set(x,0,z,'white_concrete');
  roofKit(v,x0+2,z0+2,x1-2,z1-2,9,41,{antenna:0});
  emit('supermarket_01',{name:'スーパーマーケット',cat:'商業',desc:'平屋の食品スーパー。正面に駐車場付き'},v);
}
function parkingMulti(){
  const bw=24,bd=32,W=bw+2*M,L=bd+2*M,floors=4,v=new V(W,floors*4+4,L);lot(v);
  const x0=M,z0=M,x1=M+bw-1,z1=M+bd-1;
  v.fill(x0,0,z0,x1,0,z1,'gray_concrete');
  for(let k=1;k<=floors;k++){
    const y=k*4-0;
    v.fill(x0,y,z0,x1,y,z1,'gray_concrete');
    v.ring(x0,y+1,z0,x1,y+1,z1,'light_gray_concrete');
    for(let z=z0+2;z<=z1-2;z+=3)for(let x=x0+2;x<=x1-2;x+=4)v.set(x,y,z,'white_concrete');
    for(let x=x0+4;x<=x1-4;x+=6)v.set(x,y,z0+1,'sea_lantern');
  }
  for(let k=0;k<floors;k++)for(let x=x0;x<=x1;x+=6)for(const z of [z0,z1,(z0+z1)>>1])v.fill(x,k*4+1,z,x,k*4+3,z,'light_gray_concrete');
  for(const [x,z] of [[x0,z0],[x1,z0],[x0,z1],[x1,z1]])v.fill(x,1,z,x,floors*4+1,z,'light_gray_concrete');
  v.fill(x1-4,1,z1+1,x1-1,1,z1+M,'air');
  v.fill(x0+2,floors*4+1,z1,x1-2,floors*4+3,z1,'iron_bars');
  v.fill(x0,floors*4+2,z0,x1,floors*4+3,z0,'blue_concrete');
  emit('parking_multi_01',{name:'立体駐車場(4層)',cat:'商業',desc:'開放型の4層立体駐車場',floors:4},v);
}
function restaurant(){
  const bw=18,bd=12,W=bw+2*M+6,L=bd+2*M+8,v=new V(W,9,L);
  v.fill(0,0,0,W-1,0,L-1,'gray_concrete');v.fill(0,0,0,W-1,0,2,'smooth_stone');
  const x0=M+3,z0=M,x1=x0+bw-1,z1=z0+bd-1;
  v.fill(x0,0,z0,x1,0,z1,'polished_andesite');
  v.fill(x0,1,z0,x1,4,z1,'orange_terracotta');v.fill(x0+1,1,z0+1,x1-1,3,z1-1,'air');
  v.fill(x0,5,z0,x1,5,z1,'white_concrete');v.ring(x0,6,z0,x1,6,z1,'red_concrete');v.fill(x0+1,5,z0+1,x1-1,5,z1-1,'light_gray_concrete');
  v.fill(x0+2,1,z1,x1-2,3,z1,'glass');
  const cx=(x0+x1)>>1;for(const x of [cx,cx+1])for(let y=1;y<=2;y++)v.clear(x,y,z1);
  v.set(cx,1,z1,door('iron_door','south','lower','left'));v.set(cx,2,z1,door('iron_door','south','upper','left'));
  v.set(cx+1,1,z1,door('iron_door','south','lower','right'));v.set(cx+1,2,z1,door('iron_door','south','upper','right'));
  v.fill(x0+2,4,z1+1,x1-2,4,z1+1,'sea_lantern');
  for(let x=x0+3;x<=x1-3;x+=3)v.set(x,4,(z0+z1)>>1,'sea_lantern');
  v.fill(x1-1,7,z1+2,x1-1,12 - 4,z1+2,'iron_bars');
  for(let x=x0;x<=x1;x+=3)for(let z=z1+3;z<=L-3;z++)v.set(x,0,z,'white_concrete');
  emit('restaurant_01',{name:'ファミリーレストラン',cat:'商業',desc:'平屋のロードサイド店舗'},v);
}
function convenienceSet(){
  convenience('conv_01',{name:'コンビニ(青白)',cat:'商業',desc:'正面にガラス張りの店舗と駐車スペース'},['blue_concrete','white_concrete']);
  convenience('conv_02',{name:'コンビニ(緑赤)',cat:'商業',desc:'正面にガラス張りの店舗と駐車スペース'},['green_concrete','red_concrete']);
}
module.exports={tower,hollow,houseModern,buildResidential,buildOffice,convenienceSet,gasStation,mall,supermarket,parkingMulti,restaurant,lot,planter,roofKit,shaft,rng,M,convenience};
