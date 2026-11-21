export function pageDots(page=0) {
  const dots=document.createElement('div');dots.className='page-dots';dots.setAttribute('aria-label','Page '+(page+1)+' of 2');
  dots.innerHTML='<i class="'+(page===0?'is-current':'')+'"></i><i class="'+(page===1?'is-current':'')+'"></i>';
  return dots;
}
