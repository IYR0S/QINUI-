export function mediaCard(compact=false) {
  const card=document.createElement('section');card.className='media-card'+(compact?' is-compact':'');
  card.dataset.focus='media';card.tabIndex=0;card.setAttribute('aria-label','Media player');
  card.innerHTML='<div class="album-art" aria-hidden="true"><span>♫</span></div><div class="media-info"><b>Midnight City</b><small>M83 · Hurry Up, We\'re Dreaming</small><div class="media-controls"><button data-media="prev" aria-label="Previous">|◀</button><button data-media="play" aria-label="Play or pause">▶</button><button data-media="next" aria-label="Next">▶|</button></div></div>';
  return card;
}
