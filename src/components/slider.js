export function slider({id,label,value=50,vertical=false}) {
  const wrap=document.createElement('div');wrap.className='slider-wrap'+(vertical?' is-vertical':'');
  const title=document.createElement('label');title.className='slider-label';title.htmlFor=id+'-range';title.textContent=label;
  const input=document.createElement('input');input.type='range';input.min='0';input.max='100';input.value=String(value);
  input.id=id+'-range';input.dataset.focus='slider';input.dataset.id=id;input.setAttribute('aria-label',label);
  wrap.append(title,input);return wrap;
}
