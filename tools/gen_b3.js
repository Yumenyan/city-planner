'use strict';
/* v1.1 追加テンプレート(現代建築・施設・スポーツ・街路)。正面は南(+Z)、原点は最小コーナー、1段目(y=0)は地面層 */
const {V,emit,st,sl,door,trap,leaves,log}=require('./gen_core');
const {tower,hollow,shaft,lot,roofKit,rng,M,convenience}=require('./gen_b1');
const GB='light_blue_stained_glass',GRASS='grass_block[snowy=false]';

function tree(v,x,z,y,h,r){
  for(let i=0;i<h;i++)v.set(x,y+i,z,log('oak_log','y'));
  for(let dy=-1;dy<=r;dy++)for(let dz=-r;dz<=r;dz++)for(let dx=-r;dx<=r;dx++){
    const d=Math.hypot(dx,dz,dy*0.9);
    if(d<=r+0.2&&!(dx===0&&dz===0&&dy<0&&false)){const yy=y+h-1+dy;if(v.get(x+dx,yy,z+dz)==='air')v.set(x+dx,yy,z+dz,leaves('oak_leaves'));}}
}
function car(v,x,z,col,alongX,y){
  y=y||1;const w=alongX?4:2,l=alongX?2:4;
  v.fill(x,y,z,x+w-1,y,z+l-1,col);
  const a=alongX?[x+1,z,x+2,z+l-1]:[x,z+1,x+w-1,z+2];
  v.fill(a[0],y+1,a[1],a[2],y+1,a[3],'light_blue_stained_glass');
  v.fill(a[0],y+2,a[1],a[2],y+2,a[3],col);
  // ライト
  if(alongX){v.set(x,y,z,'yellow_concrete');v.set(x,y,z+1,'yellow_concrete');v.set(x+3,y,z,'red_concrete');v.set(x+3,y,z+1,'red_concrete');}
  else{v.set(x,y,z+3,'yellow_concrete');v.set(x+1,y,z+3,'yellow_concrete');v.set(x,y,z,'red_concrete');v.set(x+1,y,z,'red_concrete');}
}
const CARCOL=['white_concrete','black_concrete','gray_concrete','red_concrete','blue_concrete','light_gray_concrete','yellow_concrete'];
function lamp(v,x,z,h){v.fill(x,1,z,x,h,z,'iron_bars');v.set(x,h+1,z,'sea_lantern');}
function frontDoor(v,x,z,y){v.set(x,y,z,door('iron_door','south','lower','left'));v.set(x,y+1,z,door('iron_door','south','upper','left'));}

/* ---------- 住宅 ---------- */
function houseCube(){
  const bw=14,bd=12,W=bw+2*M,L=bd+2*M+2,v=new V(W,12,L);lot(v);
  const x0=M,z0=M,x1=M+bw-1,z1=M+bd-1;
  for(let x=0;x<W;x++)for(let z=z1+2;z<L;z++)v.set(x,0,z,GRASS);
  v.fill(x0,0,z0,x1,0,z1,'polished_andesite');
  // 1階(左)
  hollow(v,x0,1,z0,x0+8,4,z1,'white_concrete');
  // 2階(左)+ 木目の張り出し(右。下はカーポート)
  hollow(v,x0,5,z0,x0+8,8,z1,'white_concrete');
  hollow(v,x0+9,5,z0,x1,8,z1,'spruce_planks');
  v.fill(x0+9,0,z0,x1,0,z1,'gray_concrete');
  for(const [x,z] of [[x0+9,z1],[x1,z1],[x1,z0]])v.fill(x,1,z,x,4,z,'black_concrete');
  v.fill(x0+9,4,z0,x1,4,z1,'black_concrete');
  v.fill(x0,9,z0,x1,9,z1,'gray_concrete');v.ring(x0,10,z0,x1,10,z1,'white_concrete');
  // 窓
  v.fill(x0+1,2,z1,x0+3,3,z1,GB);v.fill(x0+6,2,z1,x0+7,3,z1,GB);
  v.fill(x0+1,6,z1,x0+7,7,z1,GB);v.fill(x0+10,6,z1,x1-1,7,z1,GB);
  v.fill(x0+8,2,z0+2,x0+8,3,z1-2,GB);v.fill(x0,2,z0+2,x0,3,z1-2,GB);v.fill(x0,6,z0+2,x0,7,z1-2,GB);
  frontDoor(v,x0+4,z1,1);v.fill(x0+4,3,z1,x0+4,3,z1,'white_concrete');
  v.fill(x0+3,4,z1+1,x0+5,4,z1+1,'black_concrete');v.set(x0+4,3,z1+1,'sea_lantern');
  v.fill(x0+4,0,z1+1,x0+4,0,z1+M,'polished_andesite');
  v.set((x0+x1)>>1,4,z0+3,'sea_lantern');
  // 庭木と車
  tree(v,1,L-2,1,3,2);tree(v,W-2,L-2,1,3,2);
  car(v,x0+10,z0+3,'white_concrete',false);
  emit('house_mod_03',{name:'モダン住宅C(木目の張り出し)',cat:'住宅',desc:'2階が張り出した木目の箱型住宅。下はカーポート',floors:2},v);
}
function mansion(){
  const bw=22,bd=14,W=bw+2*M,L=bd+2*M+6,v=new V(W,13,L);lot(v);
  const x0=M,z0=M,x1=M+bw-1,z1=M+bd-1;
  for(let x=0;x<W;x++)for(let z=z1+2;z<L;z++)v.set(x,0,z,GRASS);
  v.fill(x0,0,z0,x1,0,z1,'polished_andesite');
  // 1階: 柱とガラス
  hollow(v,x0,1,z0,x1,4,z1,'white_concrete');
  for(let x=x0+1;x<x1;x++)if((x-x0)%4!==0)v.fill(x,1,z1,x,3,z1,GB);
  for(let z=z0+2;z<z1-1;z+=3){v.fill(x0,2,z,x0,3,z+1,GB);v.fill(x1,2,z,x1,3,z+1,GB);}
  frontDoor(v,x0+10,z1,1);v.set(x0+11,1,z1,'air');v.set(x0+11,2,z1,'air');v.set(x0+11,1,z1,door('iron_door','south','lower','right'));v.set(x0+11,2,z1,door('iron_door','south','upper','right'));
  v.fill(x0-1,5,z0-1,x1+1,5,z1+3,'light_gray_concrete'); // 庇(2階の床を兼ねる)
  v.fill(x0,5,z0,x1,5,z1,'smooth_stone');
  // 2階(後方へ寄せた箱)
  hollow(v,x0+3,6,z0,x1-5,9,z1-2,'smooth_quartz');
  v.fill(x0+4,7,z1-2,x1-6,8,z1-2,'tinted_glass');
  v.fill(x0+3,10,z0,x1-5,10,z1-2,'gray_concrete');v.ring(x0+3,11,z0,x1-5,11,z1-2,'white_concrete');
  // テラス(2階の前と右)
  for(let x=x0;x<=x1+1;x++)v.set(x,6,z1+3,'iron_bars');
  for(let z=z1-1;z<=z1+3;z++){v.set(x0-1,6,z,'iron_bars');v.set(x1+1,6,z,'iron_bars');}
  for(const x of [x0,x0+4,x0+8,x0+12,x0+16,x0+20])v.fill(x,1,z1+3,x,4,z1+3,'quartz_pillar[axis=y]');
  // プール
  for(let z=z1+3;z<=z1+5;z++)for(let x=x1-9;x<=x1-1;x++)v.set(x,0,z,'water[level=0]');
  for(let x=x1-10;x<=x1;x++){v.set(x,0,z1+2,'smooth_quartz');v.set(x,0,z1+6,'smooth_quartz');}
  for(let z=z1+2;z<=z1+6;z++){v.set(x1-10,0,z,'smooth_quartz');v.set(x1,0,z,'smooth_quartz');}
  tree(v,2,L-2,1,4,2);tree(v,x0+4,L-2,1,3,2);
  emit('mansion_01',{name:'邸宅(プール付き)',cat:'住宅',desc:'ガラス張りの1階と白い2階、庭のプールがある大型住宅',floors:2},v);
}

/* ---------- 商業 ---------- */
function cafe(){
  const bw=10,bd=8,W=bw+2*M+6,L=bd+2*M+6,v=new V(W,9,L);lot(v);
  const x0=M+6,z0=M,x1=x0+bw-1,z1=z0+bd-1;
  v.fill(x0,0,z0,x1,0,z1,'oak_planks');
  hollow(v,x0,1,z0,x1,4,z1,'white_concrete');
  v.fill(x0,1,z1,x1,3,z1,'glass');v.fill(x0+1,2,z1,x1-1,3,z1,'glass');
  for(const x of [x0,x0+4,x1])v.fill(x,1,z1,x,3,z1,'dark_oak_planks');
  v.fill(x1,1,z0+1,x1,3,z1-1,'glass');
  v.set(x0+2,1,z1,'air');v.set(x0+2,2,z1,'air');v.set(x0+2,1,z1,door('iron_door','south','lower','left'));v.set(x0+2,2,z1,door('iron_door','south','upper','left'));
  v.fill(x0,4,z0,x1,4,z1,'dark_oak_planks');v.ring(x0,5,z0,x1,5,z1,'white_concrete');
  v.fill(x0,4,z1+1,x1,4,z1+1,'white_concrete');
  // 日よけ(赤白ストライプ)
  for(let x=x0-1;x<=x1+1;x++)for(let z=z1+1;z<=z1+2;z++)v.set(x,3,z,((x+z)%2)?'white_concrete':'red_concrete');
  v.fill(x0,4,z1+1,x1,4,z1+1,'sea_lantern');
  // 店内のカウンター
  v.fill(x0+3,1,z0+1,x1-2,1,z0+1,'dark_oak_planks');v.fill(x0+3,2,z0+1,x1-2,2,z0+1,sl('smooth_quartz_slab','bottom'));
  for(let x=x0+2;x<=x1-2;x+=3)v.set(x,4,z0+3,'sea_lantern');
  // テラス席(店の右側ではなく前面)
  for(const [tx,tz] of [[x0+1,z1+4],[x0+5,z1+4],[x0+8,z1+4]]){
    v.set(tx,1,tz,'oak_fence');v.set(tx,2,tz,'oak_pressure_plate[powered=false]');
    v.set(tx-1,1,tz,st('oak_stairs','east'));v.set(tx+1,1,tz,st('oak_stairs','west'));
  }
  for(let x=0;x<M+5;x++)for(let z=z0;z<=z1;z++)v.set(x,0,z,'smooth_stone');
  tree(v,2,z0+2,1,3,2);
  v.fill(x0-3,1,z1+1,x0-3,3,z1+1,'iron_bars');v.set(x0-3,4,z1+1,'sea_lantern');
  emit('cafe_01',{name:'カフェ(テラス席付き)',cat:'商業',desc:'ガラス張りの店内と、赤白の日よけのテラス席があるカフェ',floors:1},v);
}
function bank(){
  const bw=18,bd=14,W=bw+2*M,L=bd+2*M+4,v=new V(W,18,L);lot(v);
  const x0=M,z0=M,x1=M+bw-1,z1=M+bd-1;
  v.fill(x0,0,z0,x1,0,z1+3,'polished_andesite');
  hollow(v,x0,1,z0,x1,12,z1,'smooth_quartz');
  v.fill(x0,5,z0,x1,5,z1,'chiseled_quartz_block');v.fill(x0,9,z0,x1,9,z1,'chiseled_quartz_block');v.fill(x0+1,5,z0+1,x1-1,5,z1-1,'smooth_stone');v.fill(x0+1,9,z0+1,x1-1,9,z1-1,'smooth_stone');
  v.fill(x0,13,z0,x1,13,z1,'gray_concrete');v.ring(x0,14,z0,x1,14,z1,'smooth_quartz');v.fill(x0-1,13,z0-1,x1+1,13,z1+4,'light_gray_concrete');
  // 柱廊(ポルティコ)
  for(let x=x0+1;x<=x1-1;x+=4)v.fill(x,1,z1+3,x,12,z1+3,'quartz_pillar[axis=y]');
  v.fill(x0,12,z1+1,x1,12,z1+3,'smooth_quartz');v.fill(x0,13,z1+1,x1,13,z1+3,'light_gray_concrete');
  // 窓(縦長)
  for(let x=x0+2;x<=x1-2;x+=4){for(const b of [2,6,10])v.fill(x,b,z1,x+1,b+2,z1,'tinted_glass');}
  for(let z=z0+3;z<=z1-3;z+=4)for(const b of [2,6,10]){v.fill(x0,b,z,x0,b+2,z+1,'tinted_glass');v.fill(x1,b,z,x1,b+2,z+1,'tinted_glass');}
  for(let y=1;y<=4;y++){v.clear(x0+8,y,z1);v.clear(x0+9,y,z1);}
  v.fill(x0+8,1,z1,x0+9,3,z1,'air');
  for(const [x,h] of [[x0+8,'left'],[x0+9,'right']]){v.set(x,1,z1,door('iron_door','south','lower',h));v.set(x,2,z1,door('iron_door','south','upper',h));}
  v.fill(x0+7,4,z1+3,x0+10,4,z1+3,'blue_concrete');v.fill(x0+5,11,z1+3,x1-5,11,z1+3,'blue_concrete');
  for(let x=x0+6;x<=x1-6;x+=2)v.set(x,11,z1+4,'sea_lantern');
  for(let k=1;k<=3;k++)v.fill(x0+8,0,z1+k,x0+9,0,z1+k,'white_concrete');
  roofKit(v,x0+1,z0+1,x1-1,z1-1,14,31,{antenna:0});
  emit('bank_01',{name:'銀行(柱廊付き)',cat:'商業',desc:'石造り風の柱廊を持つ3階建ての銀行',floors:3},v);
}
function carDealer(){
  const bw=20,bd=12,W=bw+2*M,L=bd+2*M+14,v=new V(W,9,L);
  v.fill(0,0,0,W-1,0,L-1,'gray_concrete');v.fill(0,0,0,W-1,0,1,'smooth_stone');v.fill(0,0,0,1,0,L-1,'smooth_stone');v.fill(W-2,0,0,W-1,0,L-1,'smooth_stone');
  const x0=M,z0=3,x1=M+bw-1,z1=z0+bd-1;
  v.fill(x0,0,z0,x1,0,z1,'polished_andesite');
  // ガラスのショールーム
  v.ring(x0,1,z0,x1,5,z1,'glass');
  for(const [x,z] of [[x0,z0],[x1,z0],[x0,z1],[x1,z1],[x0+10,z1],[x0+10,z0]])v.fill(x,1,z,x,5,z,'white_concrete');
  v.fill(x0-1,6,z0-1,x1+1,6,z1+3,'white_concrete');v.fill(x0-1,7,z0-1,x1+1,7,z1+3,'light_gray_concrete');
  for(let x=x0+2;x<=x1-2;x+=4)v.set(x,6,z0+4,'sea_lantern');
  v.fill(x0+1,1,z1,x0+2,2,z1,'air');v.set(x0+4,1,z1,'air');v.set(x0+4,2,z1,'air');
  v.set(x0+4,1,z1,door('iron_door','south','lower','left'));v.set(x0+4,2,z1,door('iron_door','south','upper','left'));
  v.fill(x0+1,1,z1,x0+2,2,z1,'glass');
  // 展示車
  car(v,x0+2,z0+2,'red_concrete',false);car(v,x0+7,z0+2,'white_concrete',false);car(v,x0+13,z0+3,'blue_concrete',true);
  // 外の展示場
  for(let i=0;i<5;i++){const x=x0+1+i*4;car(v,x,z1+6,CARCOL[(i*2+1)%CARCOL.length],false);
    v.fill(x-1,0,z1+5,x-1,0,z1+10,'white_concrete');}
  v.fill(x1+3,0,z1+5,x1+3,0,z1+10,'white_concrete');
  lamp(v,x1+2,z1+3,6);lamp(v,x0-1,z1+3,6);
  v.fill(x0+4,7,z1+1,x1-4,8,z1+1,'blue_concrete');v.fill(x0+5,8,z1+1,x1-5,8,z1+1,'sea_lantern');
  emit('car_dealer_01',{name:'カーディーラー',cat:'商業',desc:'ガラス張りのショールームと展示場のある自動車販売店',floors:1},v);
}
function shoppingRow(){
  const sw=8,sd=10,n=3,W=sw*n+2*M,L=sd+2*M+1,v=new V(W,11,L);lot(v);
  const z0=M,z1=M+sd-1,cols=[['red_concrete','white_concrete'],['blue_concrete','white_concrete'],['green_concrete','white_concrete']];
  const wall=['light_gray_concrete','white_concrete','smooth_sandstone'];
  for(let i=0;i<n;i++){
    const x0=M+i*sw,x1=x0+sw-1;
    v.fill(x0,0,z0,x1,0,z1,'polished_andesite');
    hollow(v,x0,1,z0,x1,8,z1,wall[i]);
    v.fill(x0,4,z0,x1,4,z1,wall[i]);v.fill(x0+1,4,z0+1,x1-1,4,z1-1,'smooth_stone');
    v.fill(x0,9,z0,x1,9,z1,'gray_concrete');v.ring(x0,10,z0,x1,10,z1,wall[i]);
    // 店頭
    v.fill(x0+1,1,z1,x1-1,3,z1,'glass');v.fill(x0,1,z1,x0,3,z1,wall[i]);v.fill(x1,1,z1,x1,3,z1,wall[i]);
    v.set(x0+2,1,z1,'air');v.set(x0+2,2,z1,'air');v.set(x0+2,1,z1,door('iron_door','south','lower','left'));v.set(x0+2,2,z1,door('iron_door','south','upper','left'));
    for(let x=x0;x<=x1;x++){v.set(x,3,z1+1,((x)%2)?cols[i][0]:cols[i][1]);}
    v.fill(x0+1,4,z1+1,x1-1,4,z1+1,'sea_lantern');
    // 2階の窓
    v.fill(x0+1,6,z1,x1-1,7,z1,GB);
    v.set(x0+3,2,z1-1,'sea_lantern');
  }
  v.fill(M-1,0,z1+1,W-M,0,L-1,'smooth_stone');
  emit('shopping_row_01',{name:'商店街(3軒つながり)',cat:'商業',desc:'色の違う日よけを持つ2階建て店舗が3軒並んだ商店街の一角',floors:2},v);
}

/* ---------- 公共 ---------- */
function kindergarten(){
  const bw=20,bd=9,W=bw+2*M,L=bd+2*M+12,v=new V(W,8,L);lot(v);
  const x0=M,z0=M,x1=M+bw-1,z1=M+bd-1;
  v.fill(x0,0,z0,x1,0,z1,'polished_andesite');
  hollow(v,x0,1,z0,x1,4,z1,'white_concrete');
  const cols=['yellow_concrete','orange_concrete','lime_concrete','light_blue_concrete'];
  for(let x=x0;x<=x1;x++)if((x-x0)%5<1||x===x1)v.fill(x,1,z1,x,4,z1,cols[((x-x0)/5|0)%4]);
  for(let c=0;c<4;c++){const xs=x0+1+c*5;v.fill(xs,2,z1,xs+3,3,z1,GB);}
  v.fill(x0,5,z0-1,x1,5,z1+1,'gray_concrete');v.fill(x0+1,5,z0,x1-1,5,z1,'light_gray_concrete');
  v.ring(x0,6,z0-1,x1,6,z1+1,'white_concrete');
  v.fill(x0+9,1,z1,x0+10,2,z1,'air');
  for(const [x,h] of [[x0+9,'left'],[x0+10,'right']]){v.set(x,1,z1,door('iron_door','south','lower',h));v.set(x,2,z1,door('iron_door','south','upper',h));}
  v.fill(x0+8,4,z1+1,x0+11,4,z1+1,'red_concrete');v.set(x0+9,4,z1+2,'sea_lantern');
  for(let x=x0+2;x<=x1-2;x+=4)v.set(x,4,z0+3,'sea_lantern');
  // 園庭
  const yz0=z1+2,yz1=L-2;
  for(let z=yz0;z<=yz1;z++)for(let x=1;x<W-1;x++)v.set(x,0,z,GRASS);
  for(let x=1;x<W-1;x++){v.set(x,1,yz1,'oak_fence');}
  for(let z=yz0;z<=yz1;z++){v.set(1,1,z,'oak_fence');v.set(W-2,1,z,'oak_fence');}
  // すべり台・砂場・ブランコ
  v.fill(4,0,yz0+2,8,0,yz0+5,'sand');
  v.fill(W-8,1,yz0+2,W-8,3,yz0+2,'red_concrete');v.set(W-8,4,yz0+2,sl('oak_slab','bottom'));
  v.set(W-8,3,yz0+3,st('quartz_stairs','south'));v.set(W-8,2,yz0+4,st('quartz_stairs','south'));v.set(W-8,1,yz0+5,st('quartz_stairs','south'));
  for(const x of [11,15]){v.fill(x,1,yz0+3,x,4,yz0+3,'iron_bars');}
  v.fill(11,4,yz0+3,15,4,yz0+3,'iron_bars');v.set(12,1,yz0+3,sl('oak_slab','bottom'));v.set(14,1,yz0+3,sl('oak_slab','bottom'));
  tree(v,W-4,yz1-1,1,3,2);
  emit('kindergarten_01',{name:'保育園(園庭付き)',cat:'公共',desc:'カラフルな平屋の園舎と、すべり台・ブランコのある園庭',floors:1},v);
}
function gym(){
  const bw=30,bd=22,W=bw+2*M,L=bd+2*M+2,v=new V(W,16,L);lot(v);
  const x0=M,z0=M,x1=M+bw-1,z1=M+bd-1,cx=(x0+x1)/2,hw=(x1-x0)/2;
  v.fill(x0,0,z0,x1,0,z1,'spruce_planks');
  hollow(v,x0,1,z0,x1,8,z1,'white_concrete');
  // 帯状の窓と柱
  for(let x=x0+2;x<=x1-2;x++)if((x-x0)%5!==0){v.fill(x,6,z1,x,7,z1,GB);v.fill(x,6,z0,x,7,z0,GB);}
  for(let z=z0+2;z<=z1-2;z++)if((z-z0)%5!==0){v.fill(x0,6,z,x0,7,z,GB);v.fill(x1,6,z,x1,7,z,GB);}
  v.fill(x0,1,z1,x1,2,z1,'gray_concrete');
  // かまぼこ屋根
  for(let x=x0;x<=x1;x++){const t=(x-cx)/hw,h=9+Math.round(Math.sqrt(Math.max(0,1-t*t))*4);
    for(let z=z0;z<=z1;z++){for(let y=9;y<h;y++)if(!(x>x0&&x<x1&&z>z0&&z<z1))v.set(x,y,z,'white_concrete');v.set(x,h,z,(z===z0||z===z1)?'white_concrete':'light_gray_concrete');}
    // 妻面の壁
    for(let y=9;y<h;y++){v.set(x,y,z0,'white_concrete');v.set(x,y,z1,'white_concrete');}
  }
  // 床ライン(バスケットコート風)
  for(let z=z0+3;z<=z1-3;z++){v.set(x0+3,0,z,'white_concrete');v.set(x1-3,0,z,'white_concrete');}
  for(let x=x0+3;x<=x1-3;x++){v.set(x,0,z0+3,'white_concrete');v.set(x,0,z1-3,'white_concrete');}
  for(let z=z0+3;z<=z1-3;z++)v.set(Math.round(cx),0,z,'white_concrete');
  for(let x=x0+2;x<=x1-2;x+=4)for(const z of [z0+4,z1-4])v.set(x,8,z,'sea_lantern');
  // 入口
  for(let y=1;y<=3;y++){v.clear(x0+14,y,z1);v.clear(x0+15,y,z1);}
  for(const [x,h] of [[x0+14,'left'],[x0+15,'right']]){v.set(x,1,z1,door('iron_door','south','lower',h));v.set(x,2,z1,door('iron_door','south','upper',h));}
  v.fill(x0+12,4,z1+1,x0+17,4,z1+2,'light_gray_concrete');v.fill(x0+12,0,z1+1,x0+17,0,z1+2,'polished_andesite');
  emit('gym_01',{name:'体育館',cat:'公共',desc:'かまぼこ形の屋根と帯状の窓、木の床を持つ体育館',floors:1},v);
}

/* ---------- 産業・インフラ ---------- */
function substation(){
  const W=24,L=20,v=new V(W,10,L);
  v.fill(0,0,0,W-1,0,L-1,'gravel');
  for(let x=0;x<W;x++){v.set(x,1,0,'iron_bars');v.set(x,1,L-1,'iron_bars');v.set(x,2,0,'iron_bars');v.set(x,2,L-1,'iron_bars');}
  for(let z=0;z<L;z++){v.set(0,1,z,'iron_bars');v.set(W-1,1,z,'iron_bars');v.set(0,2,z,'iron_bars');v.set(W-1,2,z,'iron_bars');}
  v.set(11,1,L-1,'air');v.set(12,1,L-1,'air');v.set(11,2,L-1,'air');v.set(12,2,L-1,'air');
  v.fill(10,0,L-1,13,0,L-1,'gray_concrete');
  // 変圧器
  for(const x of [3,8,13]){
    v.fill(x,1,3,x+2,3,5,'iron_block');v.fill(x,1,3,x,1,3,'gray_concrete');
    for(let k=0;k<3;k++){v.set(x+k,4,3,'lightning_rod[facing=up,powered=false,waterlogged=false]');v.set(x+k,4,5,'lightning_rod[facing=up,powered=false,waterlogged=false]');}
    v.fill(x,0,3,x+2,0,5,'light_gray_concrete');
  }
  // 鉄塔(門型)
  for(const x of [3,8,13,18]){v.fill(x,1,9,x,6,9,'iron_bars');v.fill(x,1,13,x,6,13,'iron_bars');}
  for(let x=3;x<=18;x++){v.set(x,6,9,'iron_bars');v.set(x,6,13,'iron_bars');}
  for(let z=9;z<=13;z++)for(const x of [3,8,13,18])v.set(x,6,z,'iron_bars');
  // 制御棟
  hollow(v,17,1,2,21,4,6,'white_concrete');v.fill(17,5,2,21,5,6,'gray_concrete');
  v.fill(18,2,6,19,3,6,GB);v.set(20,1,6,'air');v.set(20,2,6,'air');v.set(20,1,6,door('iron_door','south','lower','left'));v.set(20,2,6,door('iron_door','south','upper','left'));
  v.fill(1,1,L-4,3,1,L-2,'yellow_concrete');v.fill(1,2,L-4,3,2,L-2,'black_concrete');
  emit('substation_01',{name:'変電所',cat:'産業',desc:'フェンスで囲まれた変圧器・門型鉄塔・制御棟のある変電所',floors:1,margin:0},v);
}
function dataCenter(){
  const bw=28,bd=20,W=bw+2*M,L=bd+2*M+4,v=new V(W,16,L);lot(v,'gray_concrete');
  const x0=M,z0=M,x1=M+bw-1,z1=M+bd-1;
  v.fill(x0,0,z0,x1,0,z1+2,'light_gray_concrete');
  v.fill(x0,1,z0,x1,10,z1,'light_gray_concrete');v.fill(x0+1,1,z0+1,x1-1,9,z1-1,'air');
  v.fill(x0,10,z0,x1,10,z1,'gray_concrete');
  for(let y=3;y<=9;y+=3)v.fill(x0,y,z0,x1,y,z1,'white_concrete');
  v.fill(x0,10,z0,x1,10,z1,'light_gray_concrete');v.ring(x0,11,z0,x1,11,z1,'gray_concrete');
  // 屋上の冷却装置
  for(let x=x0+2;x<=x1-4;x+=5)for(let z=z0+2;z<=z1-4;z+=5){
    v.fill(x,11,z,x+3,12,z+3,'light_gray_concrete');v.fill(x+1,13,z+1,x+2,13,z+2,'iron_block');
    for(const [a,b] of [[x,z],[x+3,z],[x,z+3],[x+3,z+3]])v.set(a,12,b,'gray_concrete');
    v.fill(x+1,12,z+1,x+2,12,z+2,trap('iron_trapdoor','south','top',false).replace(/./,'i'));
  }
  // 入口と守衛室
  for(let y=1;y<=3;y++){v.clear(x0+13,y,z1);v.clear(x0+14,y,z1);}
  for(const [x,h] of [[x0+13,'left'],[x0+14,'right']]){v.set(x,1,z1,door('iron_door','south','lower',h));v.set(x,2,z1,door('iron_door','south','upper',h));}
  v.fill(x0+11,4,z1+1,x0+16,4,z1+2,'gray_concrete');v.fill(x0+11,0,z1+1,x0+16,0,z1+3,'polished_andesite');
  v.fill(x0+11,5,z1+1,x0+16,5,z1+1,'blue_concrete');
  for(let x=x0+2;x<=x1-2;x+=4)v.set(x,3,z1,'sea_lantern');
  // 非常用発電機(側面)
  for(let i=0;i<3;i++){const z=z0+2+i*5;v.fill(x1+1,1,z,x1+1,3,z+3,'white_concrete');}
  emit('datacenter_01',{name:'データセンター',cat:'産業',desc:'窓のない箱型の建物と屋上の冷却装置を持つデータセンター',floors:2},v);
}
function busTerminal(){
  const W=38,L=20,v=new V(W,9,L);
  v.fill(0,0,0,W-1,0,L-1,'gray_concrete');
  v.fill(0,0,0,W-1,3,0,'smooth_stone');v.fill(0,0,L-4,W-1,0,L-1,'smooth_stone'); // 歩道側
  for(let z=0;z<4;z++)v.fill(0,0,z,W-1,0,z,'smooth_stone');
  // 乗降場(島)
  for(let i=0;i<4;i++){const x0=2+i*9;v.fill(x0,0,4,x0+6,0,8,'light_gray_concrete');v.fill(x0,1,4,x0+6,1,4,'air');
    for(const x of [x0,x0+6])v.fill(x,1,4,x,5,4,'white_concrete');
    v.fill(x0,6,3,x0+6,6,9,'white_concrete');v.fill(x0+1,6,4,x0+5,6,8,'light_gray_concrete');
    v.fill(x0+1,1,4,x0+5,3,4,'air');v.fill(x0+1,2,4,x0+5,4,4,GB);
    v.set(x0+3,5,6,'sea_lantern');
    for(let x=x0+1;x<=x0+4;x+=3)v.set(x,1,7,st('quartz_stairs','north'));
    v.fill(x0+3,1,9,x0+3,4,9,'iron_bars');v.set(x0+3,5,9,'blue_concrete');
    for(let z=10;z<L-2;z++)v.set(x0+3,0,z,'yellow_concrete');
  }
  // 屋根の連結
  v.fill(1,7,3,W-2,7,9,'white_concrete');
  for(let x=2;x<W-2;x+=3)v.set(x,7,6,'sea_lantern');
  emit('bus_terminal_01',{name:'バスターミナル(4バース)',cat:'交通',desc:'屋根付きの乗降場が4つ並ぶバスターミナル',margin:0},v);
}

/* ---------- 緑地・スポーツ ---------- */
function soccer(){
  const W=52,L=36,v=new V(W,7,L);
  v.fill(0,0,0,W-1,0,L-1,'red_concrete');
  const gx0=4,gz0=4,gx1=W-5,gz1=L-5;
  for(let z=gz0;z<=gz1;z++)for(let x=gx0;x<=gx1;x++)v.set(x,0,z,((x>>2)%2)?GRASS:'moss_block');
  const white='white_concrete',cx=(gx0+gx1)>>1,cz=(gz0+gz1)>>1;
  for(let x=gx0;x<=gx1;x++){v.set(x,0,gz0,white);v.set(x,0,gz1,white);}
  for(let z=gz0;z<=gz1;z++){v.set(gx0,0,z,white);v.set(gx1,0,z,white);v.set(cx,0,z,white);}
  for(let a=0;a<360;a+=4){const x=Math.round(cx+Math.cos(a*Math.PI/180)*4.5),z=Math.round(cz+Math.sin(a*Math.PI/180)*4.5);v.set(x,0,z,white);}
  for(const sx of [gx0,gx1]){const d=sx===gx0?1:-1;
    for(let z=cz-5;z<=cz+5;z++)v.set(sx+d*5,0,z,white);
    for(let k=1;k<=5;k++){v.set(sx+d*k,0,cz-5,white);v.set(sx+d*k,0,cz+5,white);}
    // ゴール
    for(const z of [cz-2,cz+2])v.fill(sx-d,1,z,sx-d,2,z,'iron_bars');
    for(let z=cz-2;z<=cz+2;z++)v.set(sx-d,3,z,'iron_bars');
    v.fill(sx-d*2,1,cz-2,sx-d*2,2,cz+2,'white_wool');v.fill(sx-d*2,1,cz-2,sx-d*2,2,cz+2,'iron_bars');
  }
  // 観客席(北側)
  for(let k=0;k<3;k++)for(let x=8;x<=W-9;x++){v.set(x,1+k,1+(2-k),st('quartz_stairs','south'));if(k>0)for(let y=1;y<1+k;y++)v.set(x,y,1+(2-k),'light_gray_concrete');}
  for(const x of [3,W-4])lamp(v,x,L-2,6);
  emit('soccer_field_01',{name:'サッカー場(陸上トラック付き)',cat:'緑地・広場',desc:'芝のピッチ・ゴール・白線とトラック、北側に観客席',margin:0},v);
}
function tennis(){
  const W=28,L=32,v=new V(W,6,L);
  v.fill(0,0,0,W-1,0,L-1,'green_concrete');
  for(const cx0 of [3,15]){
    v.fill(cx0,0,3,cx0+9,0,L-4,'blue_concrete');
    for(let z=3;z<=L-4;z++){v.set(cx0,0,z,'white_concrete');v.set(cx0+9,0,z,'white_concrete');}
    for(let x=cx0;x<=cx0+9;x++){v.set(x,0,3,'white_concrete');v.set(x,0,L-4,'white_concrete');}
    for(let x=cx0+1;x<=cx0+8;x++){v.set(x,0,8,'white_concrete');v.set(x,0,L-9,'white_concrete');}
    for(let z=8;z<=L-9;z++)v.set(cx0+4,0,z,'white_concrete');
    for(let x=cx0-1;x<=cx0+10;x++){v.set(x,1,L>>1,'iron_bars');}
  }
  for(let x=1;x<W-1;x++)for(let y=1;y<=4;y++){v.set(x,y,1,'iron_bars');v.set(x,y,L-2,'iron_bars');}
  for(let z=1;z<L-1;z++)for(let y=1;y<=4;y++){v.set(1,y,z,'iron_bars');v.set(W-2,y,z,'iron_bars');}
  for(let y=1;y<=3;y++){v.clear(W>>1,y,L-2);v.clear((W>>1)-1,y,L-2);}
  for(const [x,z] of [[0,0],[W-1,0],[0,L-1],[W-1,L-1]])lamp(v,x,z,5);
  emit('tennis_court_01',{name:'テニスコート(2面)',cat:'緑地・広場',desc:'フェンスで囲まれた2面のハードコート',margin:0},v);
}
function playground(){
  const W=18,L=16,v=new V(W,7,L);
  v.fill(0,0,0,W-1,0,L-1,GRASS);v.fill(2,0,2,W-3,0,L-3,'sand');
  for(let x=0;x<W;x++){v.set(x,1,0,'oak_fence');if(x<7||x>10)v.set(x,1,L-1,'oak_fence');}
  for(let z=0;z<L;z++){v.set(0,1,z,'oak_fence');v.set(W-1,1,z,'oak_fence');}
  for(let z=L-4;z<L;z++)for(let x=7;x<=10;x++)v.set(x,0,z,'smooth_stone');
  // 滑り台
  v.fill(3,1,3,3,4,3,'red_concrete');v.set(3,5,3,sl('oak_slab','bottom'));
  v.fill(2,1,3,2,3,3,'oak_fence');
  v.set(3,4,4,st('quartz_stairs','south'));v.set(3,3,5,st('quartz_stairs','south'));v.set(3,2,6,st('quartz_stairs','south'));v.set(3,1,7,st('quartz_stairs','south'));
  // ブランコ
  for(const x of [8,13])v.fill(x,1,3,x,5,3,'iron_bars');
  for(let x=8;x<=13;x++)v.set(x,5,3,'iron_bars');
  for(const x of [9,12]){v.set(x,1,3,sl('oak_slab','bottom'));v.set(x,4,3,'chain[axis=y]');v.set(x,3,3,'chain[axis=y]');v.set(x,2,3,'chain[axis=y]');}
  // シーソーとベンチ
  for(let x=4;x<=8;x++)v.set(x,2,10,sl('oak_slab','bottom'));v.set(6,1,10,'oak_fence');
  v.set(14,1,9,st('oak_stairs','west'));v.set(14,1,10,st('oak_stairs','west'));
  tree(v,W-3,L-4,1,3,2);
  emit('playground_01',{name:'児童公園',cat:'緑地・広場',desc:'滑り台・ブランコ・シーソーのある小さな公園',margin:0},v);
}
function parkingFlat(){
  const W=32,L=22,v=new V(W,8,L),r=rng(77);
  v.fill(0,0,0,W-1,0,L-1,'gray_concrete');
  v.fill(0,0,0,W-1,0,0,'smooth_stone');v.fill(0,0,L-1,W-1,0,L-1,'smooth_stone');
  for(const zz of [2,L-7]){
    for(let x=2;x<W-1;x+=3){for(let z=zz;z<zz+5;z++)v.set(x,0,z,'white_concrete');}
    for(let x=3;x<W-3;x+=3){if(r()<0.55)car(v,x,zz+0,CARCOL[(r()*CARCOL.length)|0],false);}
  }
  for(let x=1;x<W-1;x+=2){v.set(x,0,L>>1,'yellow_concrete');}
  for(const [x,z] of [[1,1],[W-2,1],[1,L-2],[W-2,L-2],[W>>1,L>>1]])lamp(v,x,z,6);
  v.fill(W>>1,0,0,(W>>1)+1,0,0,'gray_concrete');v.fill(W>>1,0,L-1,(W>>1)+1,0,L-1,'gray_concrete');
  emit('parking_flat_01',{name:'平面駐車場',cat:'交通',desc:'白線と車止め、街灯のある2列の平面駐車場',margin:0},v);
}
function pondPark(){
  const W=34,L=30,v=new V(W,10,L),r=rng(91);
  v.fill(0,0,0,W-1,0,L-1,GRASS);
  const cx=W/2,cz=L/2-1;
  for(let z=0;z<L;z++)for(let x=0;x<W;x++){
    const d=Math.hypot((x-cx)/1.25,(z-cz)/0.95);
    if(d<=7.2)v.set(x,0,z,'water[level=0]');
    else if(d<=8.4)v.set(x,0,z,'sand');
    else if(d<=10)v.set(x,0,z,'dirt_path');
  }
  // 園路(出入口から池へ)
  for(let z=0;z<L;z++)for(const d of [0,1])v.set(Math.round(cx)-1+d,0,z,Math.hypot((Math.round(cx)-cz*0-cx),0)>99?GRASS:(v.get(Math.round(cx)-1+d,0,z)==='water[level=0]'?'water[level=0]':'dirt_path'));
  // 橋
  for(let x=Math.round(cx)-9;x<=Math.round(cx)+8;x++){if(v.get(x,0,Math.round(cz))==='water[level=0]'){v.set(x,1,Math.round(cz),sl('oak_slab','bottom'));v.set(x,1,Math.round(cz)+1,sl('oak_slab','bottom'));}}
  for(let x=Math.round(cx)-9;x<=Math.round(cx)+8;x++){if(v.get(x,0,Math.round(cz))==='water[level=0]'){v.set(x,2,Math.round(cz)-1,'oak_fence');v.set(x,2,Math.round(cz)+2,'oak_fence');}}
  const spots=[];
  for(let q=0;q<60&&spots.length<9;q++){const x=2+Math.floor(r()*(W-4)),z=2+Math.floor(r()*(L-4));
    if(Math.hypot((x-cx)/1.25,(z-cz)/0.95)<=11.5)continue;if(spots.some(s=>Math.hypot(s[0]-x,s[1]-z)<5))continue;spots.push([x,z]);tree(v,x,z,1,3+Math.floor(r()*2),2);}
  for(const [x,z,f] of [[Math.round(cx)-11,Math.round(cz),'east'],[Math.round(cx)+11,Math.round(cz),'west']]){v.set(x,1,z,st('quartz_stairs',f));}
  for(const [x,z] of [[2,2],[W-3,2],[2,L-3],[W-3,L-3]])lamp(v,x,z,5);
  emit('pond_park_01',{name:'池のある公園',cat:'緑地・広場',desc:'池と木の橋、園路とベンチのある公園',margin:0},v);
}

/* ---------- 都市設備 ---------- */
function streetTree(){
  const v=new V(5,9,5);v.fill(0,0,0,4,0,4,'smooth_stone');
  v.fill(1,0,1,3,0,3,'gray_concrete');v.set(2,0,2,GRASS);tree(v,2,2,1,4,2);
  emit('street_tree_01',{name:'街路樹(1本)',cat:'都市設備',desc:'歩道に植えられた街路樹',margin:0},v);
}
function streetTrees(){
  const L=24,v=new V(5,9,L);v.fill(0,0,0,4,0,L-1,'smooth_stone');
  for(let z=3;z<L;z+=8){v.fill(1,0,z-1,3,0,z+1,'gray_concrete');v.set(2,0,z,GRASS);tree(v,2,z,1,4,2);}
  for(let z=7;z<L;z+=8){v.set(1,1,z,st('quartz_stairs','east'));v.set(1,1,z+1,st('quartz_stairs','east'));}
  lamp(v,4,1,6);lamp(v,4,L-2,6);
  emit('street_trees_01',{name:'街路樹と歩道(24m)',cat:'都市設備',desc:'街路樹・ベンチ・街灯が並んだ歩道の一区画',margin:0},v);
}

function buildExtra(){
  tower('apt_mid_03',{name:'マンション(10階・グレー)',cat:'住宅',desc:'グレーの外壁にガラスの手すりのバルコニーが並ぶ中層マンション',floors:10},
    {bw:16,bd:12,floors:10,seed:41,look:{wall:'gray_concrete',band:'white_concrete',glass:GB,pane:true,apartment:true,balcony:true,pillar:4,pillarBlock:'light_gray_concrete'}});
  tower('apt_mid_04',{name:'マンション(9階・茶色)',cat:'住宅',desc:'茶色いタイル調の外壁の中層マンション',floors:9},
    {bw:14,bd:12,floors:9,seed:42,look:{wall:'brown_terracotta',band:'smooth_sandstone',glass:'gray_stained_glass',pane:true,apartment:true,balcony:true,pillar:5,pillarBlock:'smooth_sandstone'}});
  tower('apt_high_02',{name:'タワーマンション(22階・青)',cat:'住宅',desc:'青い窓と白い柱の高層タワーマンション',floors:22},
    {bw:18,bd:16,floors:22,seed:43,look:{wall:'white_concrete',band:'light_gray_concrete',glass:'blue_stained_glass',pane:true,apartment:true,balcony:true,pillar:6,pillarBlock:'white_concrete'},roof:{stair:true,tank:true,antenna:6}});
  houseCube();mansion();
  tower('office_04',{name:'オフィスビル(6階・白)',cat:'オフィス',desc:'白い外壁の小規模な中層オフィス',floors:6},
    {bw:14,bd:12,floors:6,seed:44,look:{wall:'white_concrete',band:'light_gray_concrete',glass:GB,pane:false,pillar:4,pillarBlock:'light_gray_concrete'},roof:{stair:true,antenna:3}});
  tower('office_05',{name:'オフィスビル(15階・黒)',cat:'オフィス',desc:'黒いガラスのシャープな高層オフィス',floors:15},
    {bw:18,bd:16,floors:15,seed:45,look:{wall:'black_concrete',band:'gray_concrete',glass:'black_stained_glass',pane:false,pillar:6,pillarBlock:'gray_concrete'},roof:{stair:true,antenna:6}});
  tower('office_06',{name:'超高層オフィス(40階)',cat:'オフィス',desc:'二段に絞られた青緑ガラスの超高層ビル',floors:30},
    {bw:24,bd:22,floors:30,upper:{inset:3,floors:10},seed:46,look:{wall:'light_gray_concrete',band:'white_concrete',glass:'cyan_stained_glass',pane:false,pillar:6,pillarBlock:'white_concrete'},roof:{stair:true,antenna:12}});
  tower('hotel_01',{name:'ビジネスホテル(12階)',cat:'商業',desc:'客室の窓が整然と並ぶ駅前のビジネスホテル',floors:12},
    {bw:16,bd:12,floors:12,gh:5,seed:47,look:{wall:'white_concrete',band:'brown_concrete',glass:'light_gray_stained_glass',pane:true,apartment:true,balcony:false,pillar:4,pillarBlock:'brown_concrete',canopy:'brown_concrete'},roof:{stair:true,tank:true,antenna:3}});
  tower('department_01',{name:'百貨店(6階)',cat:'商業',desc:'赤い帯の入った大型の百貨店',floors:6},
    {bw:28,bd:22,floors:6,gh:6,fh:5,seed:48,look:{wall:'white_concrete',band:'red_concrete',glass:GB,pane:false,pillar:4,pillarBlock:'light_gray_concrete',core:true},roof:{stair:true,tank:true,antenna:2}});
  tower('police_station_01',{name:'警察署(5階)',cat:'公共',desc:'青い帯の入った5階建ての警察署',floors:5},
    {bw:20,bd:14,floors:5,seed:49,look:{wall:'white_concrete',band:'blue_concrete',glass:GB,pane:true,apartment:false,pillar:4,pillarBlock:'light_gray_concrete',canopy:'blue_concrete'},roof:{stair:true,antenna:8}});
  tower('library_01',{name:'図書館(3階)',cat:'公共',desc:'ガラスのファサードと白い柱の図書館',floors:3},
    {bw:24,bd:18,floors:3,gh:6,fh:5,seed:50,look:{wall:'smooth_quartz',band:'light_gray_concrete',glass:'glass',pane:false,pillar:4,pillarBlock:'smooth_quartz',canopy:'smooth_quartz'},roof:{stair:true,antenna:0}});
  convenience('drugstore_01',{name:'ドラッグストア',cat:'商業',desc:'緑の看板と駐車場のあるロードサイドのドラッグストア',floors:1},['green_concrete','white_concrete']);
  convenience('conv_03',{name:'コンビニ(オレンジ)',cat:'商業',desc:'オレンジ色の看板のコンビニ',floors:1},['orange_concrete','white_concrete']);
  cafe();bank();carDealer();shoppingRow();
  kindergarten();gym();
  substation();dataCenter();busTerminal();
  soccer();tennis();playground();parkingFlat();pondPark();
  streetTree();streetTrees();
}
module.exports={buildExtra};
