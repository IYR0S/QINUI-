import {tile} from './components/tile.js';
import {slider} from './components/slider.js';
import {mediaCard} from './components/media-card.js';
import {pageDots} from './components/page-dots.js';

const root=document.querySelector('#qs-root');
const themes=['android17','oneui8','nothingos5'];
const state={theme:new URLSearchParams(location.search).get('theme')||document.documentElement.dataset.theme,page:0,focus:0,brightness:72,volume:46,playing:false,wifi:true,bluetooth:true,dnd:false,flashlight:false,alarm:true,location:false,airplane:false};
const tileDefs=[
  {id:'wifi',label:'Internet',wide:true,detail:'Connected'},
  {id:'bluetooth',label:'Bluetooth',wide:true,detail:'On'},
  {id:'dnd',label:'Do Not Disturb'},{id:'flashlight',label:'Flashlight'},
  {id:'alarm',label:'Alarm'},{id:'location',label:'Location'},{id:'airplane',label:'Airplane mode'}
];

function render(){
  document.documentElement.dataset.theme=state.theme;
  root.replaceChildren();
  const page=document.createElement('div');page.className='qs-page page-'+state.page;
  if(state.page===0){
    page.append(slider({id:'brightness',label:'Brightness',value:state.brightness}));
    const wide=document.createElement('div');wide.className='wide-tiles';
    tileDefs.filter(t=>t.wide).forEach(t=>wide.append(tile({...t,active:state[t.id]})));
    page.append(wide);
    const grid=document.createElement('div');grid.className='tile-grid';
    tileDefs.filter(t=>!t.wide).forEach(t=>grid.append(tile({...t,active:state[t.id]})));
    page.append(grid,mediaCard(true));
  }else{
    page.append(mediaCard(false),slider({id:'volume',label:'Volume',value:state.volume}),slider({id:'brightness',label:'Brightness',value:state.brightness}));
    if(state.theme==='nothingos5'){
      const notifications=document.createElement('section');notifications.className='notifications-row';
      notifications.dataset.focus='notifications';notifications.tabIndex=0;
      notifications.innerHTML='<b>NOTIFICATIONS</b><div class="notification-icons"><i>✉</i><i>☎</i><i>▢</i><i>◉</i><i>⚙</i></div>';
      page.append(notifications);
    }
  }
  page.append(pageDots(state.page));root.append(page);
  root.querySelectorAll('[data-focus]').forEach((el,i)=>{el.dataset.focusIndex=String(i);});
  syncFocus();
}
function focusables(){return [...root.querySelectorAll('[data-focus]')];}
function syncFocus(){
  const list=focusables();if(!list.length)return;
  state.focus=Math.max(0,Math.min(state.focus,list.length-1));
  list.forEach((el,i)=>el.classList.toggle('is-focused',i===state.focus));
  list[state.focus].focus({preventScroll:true});
}
function setPage(next){state.page=(next+2)%2;state.focus=0;render();}
function toggle(id){if(!(id in state)||typeof state[id]!=='boolean')return;state[id]=!state[id];render();}
function adjust(id,delta){
  const key=id==='brightness'?'brightness':'volume';
  state[key]=Math.max(0,Math.min(100,state[key]+delta));
  const input=root.querySelector('#'+key+'-range');if(input)input.value=String(state[key]);
}
function move(dx,dy){
  const list=focusables(),current=list[state.focus];if(!current)return;
  if(current.matches('input[type=range]')&&dx){adjust(current.dataset.id,dx*5);return;}
  const a=current.getBoundingClientRect(),cx=a.left+a.width/2,cy=a.top+a.height/2;
  let best=-1,score=Infinity;
  list.forEach((el,i)=>{
    if(i===state.focus)return;
    const b=el.getBoundingClientRect(),x=b.left+b.width/2-cx,y=b.top+b.height/2-cy;
    if((dx&&Math.sign(x)!==Math.sign(dx))||(dy&&Math.sign(y)!==Math.sign(dy)))return;
    const primary=dx?Math.abs(x):Math.abs(y),secondary=dx?Math.abs(y):Math.abs(x),s=primary+secondary*2.2;
    if(s<score){score=s;best=i;}
  });
  if(best>=0){state.focus=best;syncFocus();}
  else if(dy>0&&state.page===0)setPage(1);
  else if(dy<0&&state.page===1)setPage(0);
}
function activate(){
  const el=focusables()[state.focus];if(!el)return;
  if(el.dataset.focus==='tile')toggle(el.dataset.id);
  else if(el.dataset.focus==='media'){
    state.playing=!state.playing;
    const b=el.querySelector('[data-media=play]');if(b)b.textContent=state.playing?'Ⅱ':'▶';
  }else if(el.dataset.focus==='slider')adjust(el.dataset.id,5);
}
function back(){if(state.page===1)setPage(0);else{state.focus=0;syncFocus();}}
function onKey(e){
  if(['ArrowUp','ArrowDown','ArrowLeft','ArrowRight','Enter','Backspace',' '].includes(e.key))e.preventDefault();
  if(e.key==='1'||e.key==='2'||e.key==='3'){state.theme=themes[Number(e.key)-1];render();return;}
  if(e.key==='ArrowLeft')move(-1,0);
  else if(e.key==='ArrowRight')move(1,0);
  else if(e.key==='ArrowUp')move(0,-1);
  else if(e.key==='ArrowDown')move(0,1);
  else if(e.key==='Enter'||e.key===' ')activate();
  else if(e.key==='Backspace'||e.key==='Escape')back();
  else if(e.key.toLowerCase()==='q')setPage(0);
  else if(e.key.toLowerCase()==='e')setPage(1);
}
root.addEventListener('input',e=>{if(e.target.matches('input[type=range]'))state[e.target.dataset.id]=Number(e.target.value);});
root.addEventListener('click',e=>{
  const media=e.target.closest('[data-media]');if(!media)return;
  state.playing=!state.playing;
  const btn=media.closest('.media-card').querySelector('[data-media=play]');btn.textContent=state.playing?'Ⅱ':'▶';
});
document.addEventListener('keydown',onKey);render();
