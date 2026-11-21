const paths={
wifi:'<path d="M3 9a15 15 0 0 1 18 0M6.5 12.5a9 9 0 0 1 11 0M10 16a3.5 3.5 0 0 1 4 0"/><circle cx="12" cy="20" r=".8"/>',
bluetooth:'<path d="M12 2v20l7-6-14-12 7 10-7 10 14-12-7-6"/>',
dnd:'<circle cx="12" cy="12" r="9"/><path d="M7 12h10"/>',
flashlight:'<path d="M8 3h8l1 5-2 2v10H9V10L7 8zM10 20v2h4v-2"/>',
alarm:'<circle cx="12" cy="13" r="8"/><path d="M12 8v5l3 2M4 3 2 6m18-3 2 3"/>',
location:'<path d="M19 10c0 5-7 12-7 12S5 15 5 10a7 7 0 1 1 14 0Z"/><circle cx="12" cy="10" r="2"/>',
airplane:'<path d="m3 14 7-2 1-9 2-1 2 9 6 3v2l-7-1-1 6-2 1-2-7-6 1z"/>',
internet:'<path d="M3 9a15 15 0 0 1 18 0M6.5 13a9 9 0 0 1 11 0M10 17a3.5 3.5 0 0 1 4 0"/><circle cx="12" cy="20" r="1"/>',
media:'<path d="M9 18V5l11-2v13"/><circle cx="6" cy="18" r="3"/><circle cx="17" cy="16" r="3"/>',
volume:'<path d="M4 10v4h4l5 4V6l-5 4zM16 9a5 5 0 0 1 0 6m2-9a9 9 0 0 1 0 12"/>',
brightness:'<circle cx="12" cy="12" r="4"/><path d="M12 2v2m0 16v2M4.9 4.9l1.4 1.4m11.4 11.4 1.4 1.4M2 12h2m16 0h2M4.9 19.1l1.4-1.4M17.7 6.3l1.4-1.4"/>'
};
export function icon(name){return '<svg viewBox="0 0 24 24" aria-hidden="true" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">'+(paths[name]||'<circle cx="12" cy="12" r="7"/>')+'</svg>';}
