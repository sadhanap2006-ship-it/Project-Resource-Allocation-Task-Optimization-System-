/**
 * SIST Capstone - Two-Theme System (Light & Dark)
 */

export function getStoredTheme() {
  return localStorage.getItem('prs_theme') || 'dark';
}

export function applyTheme(theme) {
  const finalTheme = theme === 'light' ? 'light' : 'dark';
  document.documentElement.setAttribute('data-theme', finalTheme);
  localStorage.setItem('prs_theme', finalTheme);
  updateThemeUI(finalTheme);
  window.dispatchEvent(new CustomEvent('themechange', { detail: { theme: finalTheme } }));
  return finalTheme;
}

export function toggleTheme() {
  const current = document.documentElement.getAttribute('data-theme') || getStoredTheme();
  const next = current === 'light' ? 'dark' : 'light';
  return applyTheme(next);
}

const sunIconSvg = `<svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="5"/><line x1="12" y1="1" x2="12" y2="3"/><line x1="12" y1="21" x2="12" y2="23"/><line x1="4.22" y1="4.22" x2="5.64" y2="5.64"/><line x1="18.36" y1="18.36" x2="19.78" y2="19.78"/><line x1="1" y1="12" x2="3" y2="12"/><line x1="21" y1="12" x2="23" y2="12"/><line x1="4.22" y1="19.78" x2="5.64" y2="18.36"/><line x1="18.36" y1="5.64" x2="19.78" y2="4.22"/></svg>`;
const moonIconSvg = `<svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"/></svg>`;

export function updateThemeUI(theme) {
  const current = theme || document.documentElement.getAttribute('data-theme') || getStoredTheme();
  const btn = document.getElementById('theme-toggle-btn');
  if (!btn) return;
  const isLight = current === 'light';
  btn.innerHTML = `
    <span class="theme-btn-content" style="display:inline-flex; align-items:center; gap:0.5rem;">
      <span class="theme-icon">${isLight ? sunIconSvg : moonIconSvg}</span>
      <span>${isLight ? 'Light Mode' : 'Dark Mode'}</span>
    </span>
    <span class="theme-toggle-pill">${isLight ? sunIconSvg : moonIconSvg} <span>${isLight ? 'Light' : 'Dark'}</span></span>
  `;
  btn.setAttribute('title', isLight ? 'Switch to Dark Mode' : 'Switch to Light Mode');
  btn.setAttribute('aria-label', isLight ? 'Switch to Dark Mode' : 'Switch to Light Mode');
}

// Immediate execution on load to prevent flash
(function init() {
  const saved = getStoredTheme();
  document.documentElement.setAttribute('data-theme', saved);
})();
