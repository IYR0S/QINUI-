import {icon} from '../icons/icons.js';
export function tile({id,label,wide=false,active=false,detail=''}) {
  const button=document.createElement('button');
  button.type='button';button.className='tile'+(wide?' tile-wide':'')+(active?' is-active':'');
  button.dataset.focus='tile';button.dataset.id=id;button.setAttribute('aria-pressed',String(active));
  button.innerHTML='<span class="tile-icon" aria-hidden="true">'+icon(id)+'</span><span class="tile-copy"><b>'+label+'</b>'+(detail?'<small>'+detail+'</small>':'')+'</span>';
  return button;
}
