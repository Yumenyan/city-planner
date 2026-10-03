'use strict';
/* 交通・公共・産業・緑地・都市設備(現代建築。正面は南=+Z) */
const {V,emit,st,sl,door,trap,leaves,log}=require('./gen_core');
const {lot,planter,roofKit,shaft,rng,M}=require('./gen_b1');
const GB='light_blue_stained_glass',GG='gray_stained_glass';
const IRON=(n,f,h,hinge)=>door('iron_door',f,h,hinge);
function doorPair(v,x,y,z,facing){
  v.clear(x,y,z);v.clear(x,y+1,z);v.clear(x+1,y,z);v.clear(x+1,y+1,z);
  v.set(x,y,z,IRON(0,facing,'lower','left'));v.set(x,y+1,z,IRON(0,facing,'upper','left'));
  v.set(x+1,y,z,IRON(0,facing,'lower','right'));v.set(x+1,y+1,z,IRON(0,facing,'upper','right'));
}
function tree(v,x,z,y0,h,r){
  for(let y=0;y<h;y++)v.set(x,y0+y,z,log('oak_log'));
  for(let dy=-1;dy<=1;dy++){const rr=dy===1?Math.max(1,r-1):r;
    for(let dx=-rr;dx<=rr;dx++)for(let dz=-rr;dz<=rr;dz++){
      if(Math.abs(dx)===rr&&Math.abs(dz)===rr&&rr>1)continue;
      if(v.get(x+dx,y0+h+dy,z+dz)==='air')v.set(x+dx,y0+h+dy,z+dz,leaves('oak_leaves'));}}
}
function circle(v,cx,cz,r,y0,y1,b,hollow){
  for(let z=cz-r;z<=cz+r;z++)for(let x=cx-r;x<=cx+r;x++){
    const d=Math.hypot(x-cx,z-cz);if(d>r+0.2)continue;if(hollow&&d<r-1)continue;v.fill(x,y0,z,x,y1,z,b);}
}

/* ---------- 交通 ---------- */
function stationSmall(){
  const bw=22,bd=12,W=bw+2*M+4,L=bd+2*M+10,v=new V(W,16,L);
  v.fill(0,0,0,W-1,0,L-1,'smooth_stone');
  const x0=M+2,z0=M,x1=x0+bw-1,z1=z0+bd-1,cx=(x0+x1)>>1;
  v.fill(x0,0,z0,x1,0,z1,'polished_andesite');
  v.fill(x0,1,z0,x1,6,z1,'white_concrete');v.fill(x0+1,1,z0+1,x1-1,5,z1-1,'air');
  v.fill(x0,7,z0,x1,7,z1,'light_gray_concrete');v.ring(x0,8,z0,x1,8,z1,'white_concrete');
  v.fill(x0+2,1,z1,x1-2,5,z1,GB);
  for(let x=x0+2;x<=x1-2;x+=5)v.fill(x,1,z1,x,5,z1,'white_concrete');
  v.fill(cx-2,1,z1,cx+3,5,z1,'air');v.fill(cx-2,1,z1,cx+3,5,z1,'air');
  for(const x of [cx-1,cx+1]){v.set(x,1,z1,IRON(0,'south','lower','left'));v.set(x,2,z1,IRON(0,'south','upper','left'));
    v.set(x+1,1,z1,IRON(0,'south','lower','right'));v.set(x+1,2,z1,IRON(0,'south','upper','right'));}
  v.fill(cx-2,3,z1,cx+3,5,z1,GB);
  // 屋根の張り出し
  v.fill(x0-1,7,z1+1,x1+1,7,z1+5,'light_gray_concrete');v.fill(x0-1,8,z1+1,x1+1,8,z1+5,'white_concrete');
  v.fill(x0,7,z1+1,x1,7,z1+4,'light_gray_concrete');
  for(const x of [x0,cx-2,cx+3,x1])v.fill(x,1,z1+5,x,6,z1+5,'white_concrete');
  for(let x=x0+2;x<=x1-2;x+=4)v.set(x,7,z1+3,'sea_lantern');
  // 駅名サイン
  v.fill(cx-4,9,z1,cx+5,11,z1,'blue_concrete');v.fill(cx-3,10,z1,cx+4,10,z1,'white_concrete');
  v.fill(cx-4,9,z1+1,cx+5,11,z1+1,'sea_lantern');v.fill(cx-3,10,z1+1,cx+4,10,z1+1,'white_concrete');
  v.fill(cx-4,9,z1,cx+5,11,z1,'blue_concrete');
  // 内部(改札のイメージ)
  for(let z=z0+3;z<=z0+7;z+=2)v.fill(cx-4,1,z,cx+5,1,z,'iron_bars');
  v.fill(cx,1,z0+3,cx+1,1,z0+7,'air');
  for(let x=x0+3;x<=x1-3;x+=4)v.set(x,6,(z0+z1)>>1,'sea_lantern');
  // 駅前タクシー・歩道
  for(let x=x0;x<=x1;x++)v.set(x,0,L-1,'yellow_concrete');
  planter(v,0,L-6,2,3);planter(v,W-2,L-6,2,3);
  emit('station_small_01',{name:'駅舎(小)',cat:'交通',desc:'平屋の小さな駅舎。正面に屋根付きの駅前スペース'},v);
}
function stationBridge(){
  const bw=36,bd=14,W=bw+2*M+16,L=bd+2*M+4,v=new V(W,22,L);
  v.fill(0,0,0,W-1,0,L-1,'smooth_stone');
  const x0=M+8,z0=M,x1=x0+bw-1,z1=z0+bd-1,cx=(x0+x1)>>1;
  v.fill(x0,0,z0,x1,0,z1,'gray_concrete');
  // 1階は柱だけの開放空間
  for(let x=x0;x<=x1;x+=6)for(const z of [z0,z1,(z0+z1)>>1])v.fill(x,1,z,x,7,z,'light_gray_concrete');
  v.fill(x0,8,z0,x1,8,z1,'light_gray_concrete');
  // 2階 コンコース
  v.fill(x0,9,z0,x1,13,z1,'white_concrete');v.fill(x0+1,9,z0+1,x1-1,12,z1-1,'air');
  for(let x=x0+1;x<=x1-1;x++){for(let y=10;y<=12;y++){if((x-x0)%6!==0){v.set(x,y,z1,GB);v.set(x,y,z0,GB);}}}
  v.fill(x0,14,z0,x1,14,z1,'gray_concrete');v.ring(x0,15,z0,x1,15,z1,'white_concrete');
  for(let x=x0+3;x<=x1-3;x+=5)v.set(x,13,(z0+z1)>>1,'sea_lantern');
  v.fill(cx-6,16,z1,cx+7,18,z1,'blue_concrete');v.fill(cx-5,17,z1,cx+6,17,z1,'white_concrete');v.fill(cx-6,16,z1+1,cx+7,18,z1+1,'sea_lantern');v.fill(cx-5,17,z1+1,cx+6,17,z1+1,'white_concrete');
  // 階段(東西の端から)
  for(const side of [-1,1]){
    const bx=side<0?x0-1:x1+1;
    for(let i=0;i<8;i++){
      const x=side<0?bx-i:bx+i,y=8-i;
      for(let z=z0+2;z<=z0+5;z++){
        v.fill(x,1,z,x,y-1,z,'light_gray_concrete');
        v.set(x,y,z,st('stone_stairs',side<0?'east':'west'));
      }
    }
    for(let i=0;i<8;i++){const x=side<0?bx-i:bx+i;for(const z of [z0+1,z0+6])v.set(x,9-i,z,'iron_bars');}
    v.set(side<0?x0:x1,9,z0+3,'air');v.set(side<0?x0:x1,10,z0+3,'air');
  }
  planter(v,0,L-3,3,2);planter(v,W-3,L-3,3,2);
  emit('station_bridge_01',{name:'橋上駅舎',cat:'交通',desc:'線路をまたぐ2階コンコースと東西の階段を持つ駅舎',floors:2},v);
}
function platform(id,meta,len,wide,room){
  const v=new V(wide,10,len);
  v.fill(0,0,0,wide-1,0,len-1,'smooth_stone');
  for(let z=0;z<len;z++){v.set(1,0,z,'yellow_concrete');v.set(wide-2,0,z,'yellow_concrete');v.set(0,0,z,'white_concrete');v.set(wide-1,0,z,'white_concrete');}
  const cols=[Math.floor((wide-1)/3),wide-1-Math.floor((wide-1)/3)];
  for(let z=3;z<len-2;z+=6)for(const x of cols)v.fill(x,1,z,x,6,z,'white_concrete');
  v.fill(1,7,0,wide-2,7,len-1,'light_gray_concrete');v.fill(0,8,0,wide-1,8,len-1,'white_concrete');v.fill(1,8,1,wide-2,8,len-2,'light_gray_concrete');
  for(let z=3;z<len-2;z+=4)v.set((wide-1)>>1,7,z,'sea_lantern');
  // ベンチと駅名標
  for(let z=6;z<len-4;z+=12){v.set(cols[0]+1,1,z,st('quartz_stairs','east'));v.set(cols[0]+1,1,z+1,st('quartz_stairs','east'));}
  for(let z=9;z<len-4;z+=18){v.fill((wide-1)>>1,1,z,(wide-1)>>1,4,z,'iron_bars');v.fill((wide-1)>>1,4,z,(wide-1)>>1,4,z,'blue_concrete');v.set(((wide-1)>>1)+1,4,z,'white_concrete');v.set(((wide-1)>>1)-1,4,z,'white_concrete');}
  if(room){const rz=(len>>1)-3,rx0=cols[1]-2;
    v.fill(rx0,1,rz,rx0+3,4,rz+6,'white_concrete');v.fill(rx0+1,1,rz+1,rx0+2,3,rz+5,'air');
    v.fill(rx0,2,rz+1,rx0,3,rz+5,GB);v.fill(rx0+3,1,rz,rx0+3,4,rz,'white_concrete');
    v.fill(rx0,4,rz,rx0+3,4,rz+6,'light_gray_concrete');
    v.set(rx0+1,1,rz+1,st('quartz_stairs','east'));v.set(rx0+1,1,rz+2,st('quartz_stairs','east'));
    v.set(rx0+3,1,rz+3,'air');v.set(rx0+3,2,rz+3,'air');v.set(rx0+3,1,rz+3,IRON(0,'east','lower','left'));v.set(rx0+3,2,rz+3,IRON(0,'east','upper','left'));
  }
  emit(id,meta,v);
}
function busStop(){
  const W=10,L=6,v=new V(W,7,L);v.fill(0,0,0,W-1,0,L-1,'smooth_stone');
  v.fill(0,0,L-1,W-1,0,L-1,'gray_concrete');
  const x0=1,x1=8,z0=1,z1=3;
  v.fill(x0,5,z0,x1,5,z1,'light_gray_concrete');v.fill(x0,6,z0,x1,6,z1,'white_concrete');v.fill(x0+1,5,z0+1,x1-1,5,z1-1,'light_gray_concrete');
  for(const x of [x0,x1])v.fill(x,1,z0,x,4,z0,'light_gray_concrete');
  v.fill(x0+1,1,z0,x1-1,4,z0,GB);v.fill(x0,1,z0,x0,4,z0,'light_gray_concrete');v.fill(x1,1,z0,x1,4,z0,'light_gray_concrete');
  v.fill(x0,1,z0,x0,4,z1,'light_gray_concrete');v.fill(x0,2,z0+1,x0,4,z1,GB);
  for(let x=x0+2;x<=x0+5;x++)v.set(x,1,z0+1,st('quartz_stairs','south'));
  v.set(x0+2,5,z0+1,'sea_lantern');v.set(x0+5,5,z0+1,'sea_lantern');
  v.fill(x1+1,1,z1,x1+1,5,z1,'iron_bars');v.fill(x1+1,5,z1,x1+1,5,z1,'blue_concrete');v.set(x1+1,6,z1,'sea_lantern');
  emit('busstop_01',{name:'バス停(屋根付き)',cat:'交通',desc:'ガラスの待合屋根とベンチ、停留所標識'},v);
}
function pedBridge(){
  const run=7,span=17,L=run*2+span,W=7,v=new V(W,10,L);
  const y=7,cx=3;
  v.fill(0,0,0,W-1,0,L-1,'smooth_stone');
  v.fill(cx-1,y,run,cx+1,y,run+span-1,'light_gray_concrete');
  for(const z of [run,run+span-1])for(const x of [cx-1,cx+1])v.fill(x,1,z,x,y-1,z,'light_gray_concrete');
  for(let z=run;z<run+span;z++)for(const x of [cx-2,cx+2])v.set(x,y+1,z,'iron_bars');
  for(let z=run;z<run+span;z++)for(const x of [cx-1,cx+1])v.set(x,y+1,z,'iron_bars');
  for(let z=run+1;z<run+span-1;z++){for(const x of [cx-1,cx+1])v.set(x,y+1,z,'air');}
  v.fill(cx-1,y+2,run,cx+1,y+2,run+span-1,'light_gray_concrete');v.fill(cx-1,y+2,run,cx+1,y+2,run+span-1,'air');
  for(let i=0;i<run;i++){
    const zN=run-1-i,zS=run+span+i,h=y-1-i;
    for(const x of [cx-1,cx,cx+1]){
      v.fill(x,1,zN,x,h-1,zN,'light_gray_concrete');v.set(x,h,zN,st('stone_stairs','south'));
      v.fill(x,1,zS,x,h-1,zS,'light_gray_concrete');v.set(x,h,zS,st('stone_stairs','north'));
    }
    for(const x of [cx-2,cx+2]){v.set(x,h+1,zN,'iron_bars');v.set(x,h+1,zS,'iron_bars');}
  }
  for(let z=run;z<run+span;z+=4)for(const x of [cx-1,cx+1])v.set(x,y,z,'sea_lantern');
  emit('ped_bridge_01',{name:'歩道橋',cat:'交通',desc:'幅3ブロック・全長約31ブロック。道路をまたぐ歩道橋(渡る方向がZ軸)'},v);
}

/* ---------- 公共 ---------- */
function school(){
  const bw=40,bd=12,W=bw+2*M+2,L=bd+2*M+22,v=new V(W,18,L);
  v.fill(0,0,0,W-1,0,L-1,'smooth_stone');
  const x0=M+1,z0=M,x1=x0+bw-1,z1=z0+bd-1,cx=(x0+x1)>>1;
  v.fill(x0,0,z0,x1,0,z1,'polished_andesite');
  const sh=shaft(v,{x0,z0,x1,z1,y0:0,floors:3,fh:4,gh:4,lobby:true,wall:'white_concrete',band:'light_gray_concrete',glass:GB,pane:true,apartment:false,pillar:4,core:false,groundWall:false,canopy:'white_concrete'});
  for(let k=1;k<=3;k++)for(let x=x0+1;x<x1;x++){if((x-x0)%4!==0)for(const y of [sh.s[k-1]+1,sh.s[k-1]+2,sh.s[k-1]+3])v.set(x,y,z1,y===sh.s[k-1]+1?'white_concrete':GB+'_pane');}
  v.ring(x0,sh.top+1,z0,x1,sh.top+1,z1,'white_concrete');
  v.fill(cx-3,sh.top+2,z0+2,cx+3,sh.top+5,z0+8,'white_concrete');v.fill(cx-3,sh.top+6,z0+2,cx+3,sh.top+6,z0+8,'gray_concrete');
  v.fill(cx-1,sh.top+3,z0+8,cx+1,sh.top+4,z0+8,'light_blue_stained_glass');
  v.fill(cx-1,sh.top+3,z0+8,cx+1,sh.top+4,z0+8,'white_concrete');v.set(cx,sh.top+4,z0+8,'white_concrete');
  // 校庭(トラック風)
  const gz0=z1+5,gz1=L-3;
  v.fill(x0+2,0,gz0,x1-2,0,gz1,'orange_terracotta');
  for(let x=x0+2;x<=x1-2;x++){v.set(x,0,gz0+1,'white_concrete');v.set(x,0,gz1-1,'white_concrete');}
  v.fill(x0+8,0,gz0+3,x1-8,0,gz1-3,'green_concrete');
  for(const x of [x0+8,x1-8])for(const dz of [-2,2]){const z=((gz0+gz1)>>1)+dz;v.fill(x,1,z,x,3,z,'iron_bars');}
  v.fill(x0+8,3,((gz0+gz1)>>1)-2,x0+8,3,((gz0+gz1)>>1)+2,'iron_bars');v.fill(x1-8,3,((gz0+gz1)>>1)-2,x1-8,3,((gz0+gz1)>>1)+2,'iron_bars');
  // 国旗ポール
  v.fill(x0+4,1,z1+3,x0+4,7,z1+3,'iron_bars');v.fill(x0+5,6,z1+3,x0+6,7,z1+3,'white_wool');v.set(x0+5,6,z1+3,'red_wool');
  for(let x=x0+6;x<=x1-6;x+=8)planter(v,x,z1+2,2,1);
  roofKit(v,x0+2,z0+10,x1-2,z1-1,sh.top+2,51,{antenna:0});
  emit('school_01',{name:'学校(3階建て校舎+校庭)',cat:'公共',desc:'横長の校舎と、正面のトラック付き校庭',floors:3},v);
}
function hospital(){
  const bw=30,bd=16,W=bw+2*M,L=bd+2*M+4,v=new V(W,34,L);lot(v);
  const x0=M,z0=M,x1=x0+bw-1,z1=z0+bd-1,cx=(x0+x1)>>1;
  v.fill(x0,0,z0,x1,0,z1,'polished_andesite');
  const sh=shaft(v,{x0,z0,x1,z1,y0:0,floors:6,fh:4,gh:5,lobby:true,wall:'white_concrete',band:'light_gray_concrete',glass:GB,pane:true,apartment:false,pillar:5,core:true,groundWall:false});
  // 赤十字
  const cy=sh.s[5]-1;for(let d=-3;d<=3;d++){v.set(cx+d,cy,z1,'red_concrete');v.set(cx,cy+d,z1,'red_concrete');}
  for(let d=-2;d<=2;d++){v.set(cx+d,cy,z1,'red_concrete');v.set(cx,cy+d,z1,'red_concrete');}
  v.ring(x0,sh.top+1,z0,x1,sh.top+1,z1,'white_concrete');
  // ヘリポート
  const hx=cx-4,hz=z0+3;v.fill(hx,sh.top+1,hz,hx+8,sh.top+1,hz+8,'gray_concrete');
  for(let i=2;i<=6;i++){v.set(hx+2,sh.top+1,hz+i,'yellow_concrete');v.set(hx+6,sh.top+1,hz+i,'yellow_concrete');}
  for(let i=2;i<=6;i++)v.set(hx+i,sh.top+1,hz+4,'yellow_concrete');
  roofKit(v,x0+1,z0+1,x1-1,z1-1,sh.top+2,61,{antenna:0});
  v.fill(x0+8,sh.top+2,z0+12,x1-8,sh.top+2,z0+12,'air');
  // 救急入口の庇と車寄せ
  v.fill(x1-8,0,z1+1,x1-1,0,L-1,'gray_concrete');v.fill(x1-8,5,z1+1,x1-1,5,z1+4,'red_concrete');
  for(const x of [x1-8,x1-1])v.fill(x,1,z1+4,x,4,z1+4,'white_concrete');
  emit('hospital_01',{name:'総合病院(6階建て)',cat:'公共',desc:'赤十字とヘリポートのある病院。救急の車寄せ付き',floors:6},v);
}
function policeBox(){
  const bw=8,bd=8,W=bw+2*M,L=bd+2*M,v=new V(W,13,L);lot(v);
  const x0=M,z0=M,x1=M+bw-1,z1=M+bd-1;
  const sh=shaft(v,{x0,z0,x1,z1,y0:0,floors:2,fh:4,gh:4,lobby:true,wall:'white_concrete',band:'blue_concrete',glass:GB,pane:false,pillar:7,core:false,groundWall:true});
  v.fill(x0,sh.s[1]+1,z0,x1,sh.s[1]+1,z0,'white_concrete');
  v.fill(x0+1,sh.s[1],z1+1,x1-1,sh.s[1],z1+1,'blue_concrete');
  v.set(x0+1,sh.s[1]+1,z1+1,'sea_lantern');v.set(x1-1,sh.s[1]+1,z1+1,'sea_lantern');
  v.set(((x0+x1)>>1),sh.s[1]-1,z1+1,'red_stained_glass');v.set(((x0+x1)>>1)+1,sh.s[1]-1,z1+1,'red_stained_glass');
  v.ring(x0,sh.top+1,z0,x1,sh.top+1,z1,'white_concrete');
  v.fill(x0+1,sh.top+1,z0+1,x1-1,sh.top+1,z1-1,'gray_concrete');
  v.fill(x0+3,sh.top+1,z0+3,x0+3,sh.top+6,z0+3,'iron_bars');
  emit('police_box_01',{name:'交番',cat:'公共',desc:'青い帯と赤色灯の小さな2階建て交番',floors:2},v);
}
function fireStation(){
  const bw=24,bd=14,W=bw+2*M+6,L=bd+2*M+6,v=new V(W,18,L);
  v.fill(0,0,0,W-1,0,L-1,'smooth_stone');v.fill(M+0,0,L-6,W-1,0,L-1,'gray_concrete');
  const x0=M,z0=M,x1=M+bw-1,z1=M+bd-1;
  v.fill(x0,0,z0,x1,0,z1,'polished_andesite');
  const sh=shaft(v,{x0,z0,x1,z1,y0:0,floors:2,fh:5,gh:6,lobby:false,wall:'white_concrete',band:'red_concrete',glass:GB,pane:false,pillar:8,core:false,groundWall:true});
  // 車庫(3ベイ)
  for(let b=0;b<3;b++){const bx=x0+2+b*6;
    v.fill(bx,1,z1,bx+4,4,z1,'air');v.fill(bx,1,z1,bx+4,4,z1,'light_gray_stained_glass');
    v.fill(bx-0,5,z1,bx+4,5,z1,'red_concrete');
    for(let x=bx;x<=bx+4;x++)for(let y=2;y<=4;y+=2)v.set(x,y,z1,'gray_stained_glass');}
  v.fill(x0+20,1,z1,x0+22,4,z1,'white_concrete');v.set(x0+21,1,z1,IRON(0,'south','lower','left'));v.set(x0+21,2,z1,IRON(0,'south','upper','left'));
  v.fill(x0,sh.top,z1,x1,sh.top,z1,'red_concrete');
  v.ring(x0,sh.top+1,z0,x1,sh.top+1,z1,'white_concrete');
  // ホース乾燥塔
  v.fill(x1-3,sh.top+1,z0+1,x1-1,sh.top+8,z0+3,'white_concrete');v.fill(x1-3,sh.top+9,z0+1,x1-1,sh.top+9,z0+3,'red_concrete');
  for(let y=sh.top+3;y<=sh.top+7;y+=2)v.set(x1-2,y,z0+3,GB);
  for(let x=x0+2;x<=x1-2;x+=6)v.fill(x,1,z1+1,x+4,1,z1+1,'air');
  for(let x=x0+2;x<=x1-2;x+=6)v.fill(x,0,z1+1,x+4,0,L-2,'gray_concrete');
  emit('fire_station_01',{name:'消防署',cat:'公共',desc:'3ベイの車庫とホース乾燥塔を持つ消防署',floors:2},v);
}
function cityHall(){
  const bw=28,bd=16,W=bw+2*M+4,L=bd+2*M+10,v=new V(W,26,L);lot(v);
  const x0=M+2,z0=M,x1=x0+bw-1,z1=z0+bd-1,cx=(x0+x1)>>1;
  v.fill(x0,0,z0,x1,0,z1,'polished_andesite');
  const sh=shaft(v,{x0,z0,x1,z1,y0:0,floors:4,fh:4,gh:5,lobby:true,wall:'white_concrete',band:'light_gray_concrete',glass:GG,pane:false,pillar:4,pillarBlock:'quartz_pillar[axis=y]',core:true,groundWall:false,canopy:'white_concrete'});
  // 正面の列柱
  for(let x=x0+2;x<=x1-2;x+=4)v.fill(x,1,z1+3,x,sh.s[1]-0,z1+3,'quartz_pillar[axis=y]');
  v.fill(x0+1,sh.s[1],z1+1,x1-1,sh.s[1],z1+3,'white_concrete');
  v.ring(x0,sh.top+1,z0,x1,sh.top+1,z1,'white_concrete');
  roofKit(v,x0+1,z0+1,x1-1,z1-1,sh.top+2,71,{antenna:0,stair:true});
  // 旗竿3本
  for(const dx of [-8,0,8]){const x=cx+dx;v.fill(x,0,L-2,x,9,L-2,'iron_bars');v.set(x+1,9,L-2,'blue_wool');v.set(x+1,8,L-2,'white_wool');v.set(x,10,L-2,'sea_lantern');}
  planter(v,0,z1+4,3,2);planter(v,W-3,z1+4,3,2);
  v.fill(cx-2,0,z1+4,cx+3,0,L-1,'white_concrete');
  emit('cityhall_01',{name:'市役所(4階建て)',cat:'公共',desc:'列柱のある正面と旗竿を備えた庁舎',floors:4},v);
}
function postOffice(){
  const bw=16,bd=12,W=bw+2*M+4,L=bd+2*M+6,v=new V(W,14,L);lot(v);
  v.fill(0,0,L-6,W-1,0,L-1,'gray_concrete');
  const x0=M,z0=M,x1=M+bw-1,z1=M+bd-1;
  v.fill(x0,0,z0,x1,0,z1,'polished_andesite');
  const sh=shaft(v,{x0,z0,x1,z1,y0:0,floors:2,fh:4,gh:5,lobby:true,wall:'white_concrete',band:'red_concrete',glass:GB,pane:false,pillar:5,core:false,groundWall:false});
  v.fill(x0+1,sh.s[1]+1,z1+1,x1-1,sh.s[1]+2,z1+1,'red_concrete');v.fill(x0+1,sh.s[1],z1+1,x1-1,sh.s[1],z1+1,'sea_lantern');
  v.ring(x0,sh.top+1,z0,x1,sh.top+1,z1,'white_concrete');
  for(const x of [x1+2,x1+3]){v.fill(x,1,z1+1,x,2,z1+1,'red_concrete');}
  v.fill(x1+2,3,z1+1,x1+3,3,z1+1,'sea_lantern');
  roofKit(v,x0+1,z0+1,x1-1,z1-1,sh.top+2,81,{antenna:0});
  emit('post_office_01',{name:'郵便局',cat:'公共',desc:'赤い帯の2階建ての郵便局',floors:2},v);
}

/* ---------- 産業 ---------- */
function warehouse(){
  const bw=30,bd=20,W=bw+2*M,L=bd+2*M+10,v=new V(W,14,L);
  v.fill(0,0,0,W-1,0,L-1,'gray_concrete');v.fill(0,0,0,W-1,0,1,'smooth_stone');
  const x0=M,z0=M,x1=M+bw-1,z1=M+bd-1;
  v.fill(x0,0,z0,x1,0,z1,'polished_andesite');
  v.fill(x0,1,z0,x1,9,z1,'light_gray_concrete');v.fill(x0+1,1,z0+1,x1-1,9,z1-1,'air');
  for(let x=x0;x<=x1;x+=3)for(let y=1;y<=9;y++){v.set(x,y,z1,'gray_concrete');v.set(x,y,z0,'gray_concrete');}
  v.fill(x0,10,z0,x1,10,z1,'gray_concrete');v.ring(x0,11,z0,x1,11,z1,'light_gray_concrete');
  for(let b=0;b<4;b++){const bx=x0+3+b*7;v.fill(bx,1,z1,bx+3,5,z1,'iron_block');for(let y=2;y<=5;y+=1)v.fill(bx,y,z1,bx+3,y,z1,y%2?'light_gray_concrete':'iron_block');v.fill(bx,6,z1,bx+3,6,z1,'yellow_concrete');}
  v.fill(x0,7,z1+1,x1,7,z1+1,'blue_concrete');
  for(let x=x0+4;x<=x1-4;x+=5)v.set(x,10,(z0+z1)>>1,'sea_lantern');
  for(let z=z1+2;z<=L-2;z+=3)v.fill(x0+3,0,z,x1-3,0,z,'gray_concrete');
  for(let b=0;b<4;b++)v.fill(x0+3+b*7,0,z1+1,x0+6+b*7,0,z1+4,'yellow_concrete');
  emit('warehouse_01',{name:'倉庫(物流)',cat:'産業',desc:'4つのシャッターを持つ大型倉庫',floors:1},v);
}
function factory(){
  const bw=36,bd=24,W=bw+2*M+14,L=bd+2*M+6,v=new V(W,34,L);
  v.fill(0,0,0,W-1,0,L-1,'gray_concrete');v.fill(0,0,0,W-1,0,1,'smooth_stone');
  const x0=M,z0=M,x1=M+bw-1,z1=M+bd-1;
  v.fill(x0,0,z0,x1,0,z1,'polished_andesite');
  v.fill(x0,1,z0,x1,10,z1,'white_concrete');v.fill(x0+1,1,z0+1,x1-1,10,z1-1,'air');
  for(let x=x0+2;x<=x1-2;x+=4)v.fill(x,7,z1,x+1,8,z1,GG);
  v.fill(x0,6,z1,x1,6,z1,'light_gray_concrete');v.fill(x0,10,z1,x1,10,z1,'blue_concrete');
  v.fill(x0,11,z0,x1,11,z1,'gray_concrete');v.fill(x0+1,11,z0+1,x1-1,11,z1-1,'light_gray_concrete');
  for(let b=0;b<2;b++){const bx=x0+4+b*14;v.fill(bx,1,z1,bx+5,5,z1,'iron_block');v.fill(bx,6,z1,bx+5,6,z1,'yellow_concrete');}
  // 煙突
  const cxk=x1+5,czk=z0+4;
  circle(v,cxk,czk,2,1,30,'white_concrete',false);
  for(let y=6;y<=30;y+=6)circle(v,cxk,czk,2,y,y+1,'red_concrete',false);
  circle(v,cxk,czk,2,31,31,'gray_concrete',false);
  // サイロ
  for(const [cx,cz] of [[x1+4,z1-4],[x1+10,z1-4],[x1+4,z1-11]]){circle(v,cx,cz,3,1,13,'light_gray_concrete',false);circle(v,cx,cz,3,14,14,'gray_concrete',false);v.set(cx,15,cz,'iron_bars');}
  for(let x=x0+3;x<=x1-3;x+=6)for(let z=z0+3;z<=z1-3;z+=7)v.set(x,11,z,'sea_lantern');
  emit('factory_01',{name:'工場(煙突・サイロ付き)',cat:'産業',desc:'大型の工場棟と煙突、3基のサイロ'},v);
}

/* ---------- 緑地・広場 ---------- */
function park(id,meta,size){
  const W=size,L=size,v=new V(W,10,L),r=rng(size*7+3);
  v.fill(0,0,0,W-1,0,L-1,'grass_block[snowy=false]');
  v.fill(0,0,0,W-1,0,0,'smooth_stone');v.fill(0,0,0,0,0,L-1,'smooth_stone');v.fill(W-1,0,0,W-1,0,L-1,'smooth_stone');v.fill(0,0,L-1,W-1,0,L-1,'smooth_stone');
  const cx=W>>1,cz=L>>1;
  // 園路
  for(let x=0;x<W;x++)for(let d=-1;d<=1;d++)v.set(x,0,cz+d,'smooth_stone');
  for(let z=0;z<L;z++)for(let d=-1;d<=1;d++)v.set(cx+d,0,z,'smooth_stone');
  // 中央の円形広場と噴水
  const R=size>=24?5:3;
  for(let z=cz-R;z<=cz+R;z++)for(let x=cx-R;x<=cx+R;x++){const d=Math.hypot(x-cx,z-cz);if(d<=R+0.3)v.set(x,0,z,'light_gray_concrete');}
  if(size>=24){for(let z=cz-2;z<=cz+2;z++)for(let x=cx-2;x<=cx+2;x++){const d=Math.hypot(x-cx,z-cz);if(d<=2.3){v.set(x,0,z,'water[level=0]');}
    else if(d<=3.2){v.set(x,0,z,'white_concrete');v.set(x,1,z,'white_concrete');}}
    v.set(cx,0,cz,'white_concrete');v.fill(cx,1,cz,cx,2,cz,'white_concrete');v.set(cx,3,cz,'sea_lantern');}
  else{v.fill(cx-1,1,cz-1,cx+1,1,cz+1,'smooth_stone');v.set(cx,2,cz,'sea_lantern');}
  // 木とベンチと街灯
  const spots=[];for(let q=0;q<(size>=24?9:4);q++){
    const x=2+Math.floor(r()*(W-4)),z=2+Math.floor(r()*(L-4));
    if(Math.abs(x-cx)<=3||Math.abs(z-cz)<=3)continue;
    if(spots.some(s=>Math.hypot(s[0]-x,s[1]-z)<5))continue;spots.push([x,z]);tree(v,x,z,1,3+Math.floor(r()*2),2);}
  const bo=R+2;
  for(const [x,z,f] of [[cx-bo,cz-2,'east'],[cx+bo,cz+2,'west'],[cx-2,cz-bo,'south'],[cx+2,cz+bo,'north']]){if(x>1&&z>1&&x<W-2&&z<L-2){v.set(x,1,z,st('quartz_stairs',f));}}
  for(const [x,z] of [[cx-2,cz-R-2],[cx+2,cz+R+2],[cx-R-2,cz+2],[cx+R+2,cz-2]]){if(x>0&&z>0&&x<W-1&&z<L-1){v.fill(x,1,z,x,5,z,'iron_bars');v.set(x,6,z,'sea_lantern');}}
  // 花壇
  for(const [x,z] of [[3,3],[W-5,3],[3,L-5],[W-5,L-5]]){if(size<20)break;for(let i=0;i<2;i++)for(let j=0;j<2;j++){v.set(x+i,0,z+j,'grass_block[snowy=false]');v.set(x+i,1,z+j,['poppy','dandelion','blue_orchid','allium'][(i+j*2+x)%4]);}}
  emit(id,meta,v);
}
function plaza(){
  const W=32,L=32,v=new V(W,10,L);
  v.fill(0,0,0,W-1,0,L-1,'light_gray_concrete');
  for(let z=0;z<L;z++)for(let x=0;x<W;x++)if(((x>>2)+(z>>2))%2===0)v.set(x,0,z,'white_concrete');
  const cx=W>>1,cz=L>>1;
  for(let z=cz-6;z<=cz+6;z++)for(let x=cx-6;x<=cx+6;x++){const d=Math.hypot(x-cx,z-cz);
    if(d<=6.3&&d>4.6){v.set(x,0,z,'white_concrete');v.set(x,1,z,'white_concrete');}
    else if(d<=4.6){v.set(x,0,z,'water[level=0]');}}
  for(const [dx,dz] of [[0,0],[1,0],[-1,0],[0,1],[0,-1]])v.fill(cx+dx,0,cz+dz,cx+dx,(dx||dz)?2:4,cz+dz,'white_concrete');
  v.set(cx,5,cz,'sea_lantern');
  for(const [x,z] of [[4,4],[W-5,4],[4,L-5],[W-5,L-5]]){
    for(let i=0;i<3;i++)for(let j=0;j<3;j++){v.set(x-1+i,0,z-1+j,'gray_concrete');v.set(x-1+i,1,z-1+j,'gray_concrete');}
    v.fill(x-1,1,z-1,x+1,1,z+1,'gray_concrete');v.fill(x,1,z,x,1,z,'grass_block[snowy=false]');tree(v,x,z,2,3,2);}
  for(let k=-1;k<=1;k+=2)for(const [x,z,f] of [[cx+k*9,cz-4,'west'],[cx+k*9,cz+4,'west']]){v.set(x,1,z,st('quartz_stairs',k>0?'west':'east'));}
  for(const [x,z] of [[10,6],[W-11,6],[10,L-7],[W-11,L-7]]){v.fill(x,1,z,x,6,z,'iron_bars');v.set(x,7,z,'sea_lantern');}
  emit('plaza_01',{name:'駅前広場(噴水)',cat:'緑地・広場',desc:'市松模様の舗装と円形の噴水、植栽のある広場'},v);
}

/* ---------- 都市設備(小物) ---------- */
function crosswalk(id,meta,span){
  const v=new V(5,1,span);
  for(let z=0;z<span;z++)for(let x=0;x<5;x++)v.set(x,0,z,x%2===0?'white_concrete':'gray_concrete');
  emit(id,meta,v);
}
function trafficLight(){
  const v=new V(7,9,3);
  v.fill(1,0,1,1,0,1,'light_gray_concrete');
  for(let y=1;y<=6;y++)v.set(1,y,1,'iron_bars');
  for(let x=2;x<=5;x++)v.set(x,7,1,'iron_bars');
  v.fill(5,6,1,5,6,1,'black_concrete');v.set(5,5,1,'black_concrete');v.set(5,4,1,'black_concrete');
  v.set(5,6,2,'red_concrete');v.set(5,5,2,'yellow_concrete');v.set(5,4,2,'lime_concrete');
  v.fill(1,5,1,1,5,1,'black_concrete');v.set(1,5,2,'red_concrete');v.set(1,4,2,'lime_concrete');v.set(1,5,1,'iron_bars');
  v.fill(0,0,0,6,0,2,'smooth_stone');
  emit('traffic_light_01',{name:'信号機',cat:'都市設備',desc:'交差点の角に置く、腕木式の車両用信号機'},v);
}
function streetLight(){
  const v=new V(5,9,3);v.fill(0,0,0,4,0,2,'smooth_stone');
  for(let y=1;y<=7;y++)v.set(1,y,1,'iron_bars');
  v.fill(2,7,1,3,7,1,'iron_bars');v.set(3,6,1,'sea_lantern');v.set(4,7,1,'light_gray_concrete');
  v.set(1,8,1,'light_gray_concrete');
  emit('streetlight_01',{name:'街灯(アーム型)',cat:'都市設備',desc:'道路側に腕を伸ばす現代的な街灯'},v);
}
function vending(){
  const v=new V(5,4,3);v.fill(0,0,0,4,0,2,'smooth_stone');
  v.fill(1,1,1,3,2,1,'red_concrete');v.fill(2,1,2,2,2,2,'light_blue_stained_glass');
  v.fill(2,1,1,2,2,1,'light_blue_concrete');v.set(1,3,1,'light_gray_concrete');v.set(2,3,1,'sea_lantern');v.set(3,3,1,'light_gray_concrete');
  v.set(1,1,2,'blue_concrete');v.set(3,1,2,'air');
  emit('vending_01',{name:'自動販売機(3台)',cat:'都市設備',desc:'歩道脇の自動販売機のまとまり'},v);
}
function utilityPole(){
  const v=new V(5,12,3);v.fill(0,0,0,4,0,2,'smooth_stone');
  for(let y=1;y<=10;y++)v.set(2,y,1,log('oak_log'));
  v.fill(0,10,1,4,10,1,'oak_planks');v.fill(1,8,1,3,8,1,'oak_planks');
  v.set(0,11,1,'iron_bars');v.set(4,11,1,'iron_bars');v.set(2,11,1,'iron_bars');v.set(2,6,1,'gray_concrete');v.set(2,5,1,'gray_concrete');
  emit('utility_pole_01',{name:'電柱',cat:'都市設備',desc:'道路脇に立つ電柱と腕木'},v);
}
function buildInfra(){
  stationSmall();stationBridge();
  platform('platform_roof_01',{name:'ホーム(屋根付き30m)',cat:'交通',desc:'幅10・長さ30。ホーム縁の黄色線と屋根、ベンチ付き'},30,10,false);
  platform('platform_01',{name:'ホーム(待合室付き40m)',cat:'交通',desc:'幅12・長さ40。屋根・ベンチ・待合室付き'},40,12,true);
  busStop();pedBridge();
  school();hospital();policeBox();fireStation();cityHall();postOffice();
  warehouse();factory();
  park('park_01',{name:'公園(中央噴水)',cat:'緑地・広場',desc:'28ブロック四方の公園。園路・噴水・ベンチ・花壇'},28);
  park('park_small_01',{name:'小公園',cat:'緑地・広場',desc:'16ブロック四方の小さな公園'},16);
  plaza();
  crosswalk('crosswalk_01',{name:'横断歩道(幅9の道路用)',cat:'都市設備',desc:'幅5・長さ9。道路の地面と同じ高さに置く'},9);
  crosswalk('crosswalk_02',{name:'横断歩道(幅13の道路用)',cat:'都市設備',desc:'幅5・長さ13。道路の地面と同じ高さに置く'},13);
  trafficLight();streetLight();vending();utilityPole();
}
module.exports={buildInfra};
