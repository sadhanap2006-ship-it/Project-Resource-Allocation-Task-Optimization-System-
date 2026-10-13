/**
 * SIST Capstone - Auth and Session Management
 * Role guards, vertical left sidebar rendering, theme toggle, notification badge polling.
 */
import { api } from './api.js';
import { getStoredTheme, toggleTheme, updateThemeUI, applyTheme } from './theme.js';

let currentUser = null;

export async function getCurrentUser() {
  if (currentUser) return currentUser;
  try {
    currentUser = await api.get('/auth/me');
    return currentUser;
  } catch (err) {
    return null;
  }
}

export async function requireAuth(allowedRoles = []) {
  const user = await getCurrentUser();
  if (!user) {
    window.location.href = `/login.html?redirect=${encodeURIComponent(window.location.pathname)}`;
    return null;
  }

  if (allowedRoles.length > 0 && !allowedRoles.includes(user.role)) {
    if (user.role === 'DEVELOPER') {
      window.location.href = '/dev-dashboard.html';
    } else {
      window.location.href = '/pm-dashboard.html';
    }
    return null;
  }

  renderAppHeader(user);
  startNotificationPolling();
  return user;
}

export function renderAppHeader(user) {
  const headerContainer = document.getElementById('app-header-container');
  if (!headerContainer) return;

  const isPM = user.role === 'PM' || user.role === 'ADMIN';
  const currentPath = window.location.pathname;

  // Derive user initials for avatar
  const initials = (user.name || user.email || 'U')
    .split(' ')
    .filter(Boolean)
    .slice(0, 2)
    .map(w => w[0].toUpperCase())
    .join('');

  const icons = {
    menu: `<svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="3" y1="12" x2="21" y2="12"/><line x1="3" y1="6" x2="21" y2="6"/><line x1="3" y1="18" x2="21" y2="18"/></svg>`,
    bell: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"/><path d="M13.73 21a2 2 0 0 1-3.46 0"/></svg>`,
    dashboard: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="7" height="9" rx="1"/><rect x="14" y="3" width="7" height="5" rx="1"/><rect x="14" y="12" width="7" height="9" rx="1"/><rect x="3" y="16" width="7" height="5" rx="1"/></svg>`,
    projects: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z"/></svg>`,
    milestones: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M4 15s1-1 4-1 5 2 8 2 4-1 4-1V3s-1 1-4 1-5-2-8-2-4 1-4 1z"/><line x1="4" y1="22" x2="4" y2="15"/></svg>`,
    resources: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>`,
    matrix: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="18" height="18" rx="2"/><line x1="3" y1="9" x2="21" y2="9"/><line x1="3" y1="15" x2="21" y2="15"/><line x1="9" y1="3" x2="9" y2="21"/><line x1="15" y1="3" x2="15" y2="21"/></svg>`,
    timeline: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="4" width="18" height="18" rx="2" ry="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/></svg>`,
    heatmap: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="5" height="5" rx="1"/><rect x="10" y="3" width="5" height="5" rx="1"/><rect x="17" y="3" width="5" height="5" rx="1"/><rect x="3" y="10" width="5" height="5" rx="1"/><rect x="10" y="10" width="5" height="5" rx="1"/><rect x="17" y="10" width="5" height="5" rx="1"/><rect x="3" y="17" width="5" height="5" rx="1"/><rect x="10" y="17" width="5" height="5" rx="1"/><rect x="17" y="17" width="5" height="5" rx="1"/></svg>`,
    conflicts: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="m21.73 18-8-14a2 2 0 0 0-3.48 0l-8 14A2 2 0 0 0 4 21h16a2 2 0 0 0 1.73-3Z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg>`,
    budget: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><rect width="20" height="14" x="2" y="5" rx="2"/><line x1="2" x2="22" y1="10" y2="10"/><circle cx="16" cy="15" r="1"/></svg>`,
    leave: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="4" width="18" height="18" rx="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/><path d="M9 16l6-6"/><path d="M15 16l-6-6"/></svg>`,
    simulator: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><line x1="4" y1="21" x2="4" y2="14"/><line x1="4" y1="10" x2="4" y2="3"/><line x1="12" y1="21" x2="12" y2="12"/><line x1="12" y1="8" x2="12" y2="3"/><line x1="20" y1="21" x2="20" y2="16"/><line x1="20" y1="12" x2="20" y2="3"/><line x1="1" y1="14" x2="7" y2="14"/><line x1="9" y1="8" x2="15" y2="8"/><line x1="17" y1="16" x2="23" y2="16"/></svg>`,
    workspace: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><rect x="2" y="3" width="20" height="14" rx="2"/><line x1="8" y1="21" x2="16" y2="21"/><line x1="12" y1="17" x2="12" y2="21"/></svg>`,
    logout: `<svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/><polyline points="16 17 21 12 16 7"/><line x1="21" y1="12" x2="9" y2="12"/></svg>`
  };

  headerContainer.innerHTML = `
    <!-- Mobile Responsive Top Bar -->
    <div class="mobile-topbar" id="mobile-topbar">
      <button class="mobile-toggle-btn" id="mobile-sidebar-toggle" title="Toggle Navigation Menu">${icons.menu}</button>
      <div style="display:flex; align-items:center; gap:0.5rem;">
        <div class="sidebar-logo" style="width:28px; height:28px; font-size:12px;">PR</div>
        <span style="font-weight:700; font-size:0.9rem; color:var(--text-main);">Resource Optimizer</span>
      </div>
      <a href="/notifications.html" style="font-size:1.1rem; text-decoration:none; position:relative; display:flex; align-items:center; color:var(--text-main);">
        <span style="display:inline-flex; width:20px; height:20px;">${icons.bell}</span>
        <span id="mobile-notif-badge" class="notif-badge" style="display:none; position:absolute; top:-6px; right:-6px; font-size:10px; padding:1px 4px; background:var(--danger); color:white; border-radius:9999px;"></span>
      </a>
    </div>

    <!-- Mobile Drawer Backdrop -->
    <div class="sidebar-backdrop" id="sidebar-backdrop"></div>

    <!-- Main Left Vertical Sidebar -->
    <aside class="app-sidebar" id="app-sidebar">
      <!-- Brand & Logo -->
      <a href="${isPM ? '/pm-dashboard.html' : '/dev-dashboard.html'}" class="sidebar-brand">
        <div class="sidebar-logo">PR</div>
        <div class="sidebar-brand-text">
          <span class="sidebar-brand-title">Resource Optimizer</span>
          <span class="sidebar-brand-subtitle">Task & Resource System</span>
        </div>
      </a>

      <!-- Vertical Nav Links -->
      <nav class="sidebar-nav-container">
        <div class="sidebar-nav-category">${isPM ? 'Portfolio & Planning' : 'Workspace'}</div>

        ${isPM ? `
          <a href="/pm-dashboard.html" class="sidebar-link ${currentPath.includes('pm-dashboard') ? 'active' : ''}">
            <span class="sidebar-icon">${icons.dashboard}</span>
            <span>Dashboard</span>
          </a>
          <a href="/projects.html" class="sidebar-link ${currentPath.includes('project') ? 'active' : ''}">
            <span class="sidebar-icon">${icons.projects}</span>
            <span>Projects</span>
          </a>
          <a href="/milestones.html" class="sidebar-link ${currentPath.includes('milestones') ? 'active' : ''}">
            <span class="sidebar-icon">${icons.milestones}</span>
            <span>Milestones</span>
          </a>
          <a href="/resources.html" class="sidebar-link ${currentPath.includes('resources') ? 'active' : ''}">
            <span class="sidebar-icon">${icons.resources}</span>
            <span>Resources</span>
          </a>
          <a href="/allocation-matrix.html" class="sidebar-link ${currentPath.includes('matrix') ? 'active' : ''}">
            <span class="sidebar-icon">${icons.matrix}</span>
            <span>Matrix</span>
          </a>
          <a href="/timeline.html" class="sidebar-link ${currentPath.includes('timeline') ? 'active' : ''}">
            <span class="sidebar-icon">${icons.timeline}</span>
            <span>Timeline</span>
          </a>
          <a href="/heatmap.html" class="sidebar-link ${currentPath.includes('heatmap') ? 'active' : ''}">
            <span class="sidebar-icon">${icons.heatmap}</span>
            <span>Heatmap</span>
          </a>
          <a href="/conflicts.html" class="sidebar-link ${currentPath.includes('conflicts') ? 'active' : ''}">
            <span class="sidebar-icon">${icons.conflicts}</span>
            <span>Conflicts</span>
          </a>
          <a href="/budget.html" class="sidebar-link ${currentPath.includes('budget') ? 'active' : ''}">
            <span class="sidebar-icon">${icons.budget}</span>
            <span>Budget</span>
          </a>
          <a href="/leave.html" class="sidebar-link ${currentPath.includes('leave') ? 'active' : ''}">
            <span class="sidebar-icon">${icons.leave}</span>
            <span>Leave</span>
          </a>
          <a href="/simulator.html" class="sidebar-link ${currentPath.includes('simulator') ? 'active' : ''}">
            <span class="sidebar-icon">${icons.simulator}</span>
            <span>Simulator</span>
          </a>
        ` : `
          <a href="/dev-dashboard.html" class="sidebar-link ${currentPath.includes('dev-dashboard') ? 'active' : ''}">
            <span class="sidebar-icon">${icons.workspace}</span>
            <span>My Workspace</span>
          </a>
          <a href="/leave.html" class="sidebar-link ${currentPath.includes('leave') ? 'active' : ''}">
            <span class="sidebar-icon">${icons.leave}</span>
            <span>My Leave</span>
          </a>
        `}

        <div class="sidebar-nav-category" style="margin-top:0.4rem;">Alerts & Feeds</div>
        <a href="/notifications.html" class="sidebar-link ${currentPath.includes('notifications') ? 'active' : ''}">
          <span class="sidebar-icon">${icons.bell}</span>
          <span>Notifications</span>
          <span id="header-notif-badge" class="sidebar-badge">0</span>
        </a>
      </nav>

      <!-- Sidebar Footer Deck -->
      <div class="sidebar-footer">
        <!-- 2-Theme Switcher (Light / Dark) -->
        <button id="theme-toggle-btn" class="theme-toggle-btn" type="button" title="Toggle Theme">
          <!-- Populated by updateThemeUI -->
        </button>

        <!-- User Profile Card -->
        <div class="sidebar-user-card">
          <div class="sidebar-user-info">
            <div class="sidebar-avatar">${escapeHtml(initials)}</div>
            <div class="sidebar-user-meta">
              <span class="sidebar-user-name" title="${escapeHtml(user.name)}">${escapeHtml(user.name)}</span>
              <span class="sidebar-user-role">
                <span class="badge ${user.role === 'ADMIN' ? 'badge-amber' : user.role === 'PM' ? 'badge-blue' : 'badge-green'}" style="font-size:10px; padding:1px 6px;">
                  ${user.role}
                </span>
              </span>
            </div>
          </div>
          <button id="btn-logout" class="btn-sidebar-logout" title="Sign Out" aria-label="Sign Out">${icons.logout}</button>
        </div>
      </div>
    </aside>
  `;

  // Initialize theme button UI
  updateThemeUI();

  // Attach Theme Toggle event
  document.getElementById('theme-toggle-btn')?.addEventListener('click', () => {
    toggleTheme();
  });

  // Attach Logout event
  document.getElementById('btn-logout')?.addEventListener('click', async () => {
    try {
      await api.post('/auth/logout');
    } catch (_) {}
    localStorage.removeItem('prs_jwt_token');
    window.location.href = '/login.html';
  });

  // Mobile drawer controls
  const toggleBtn = document.getElementById('mobile-sidebar-toggle');
  const sidebar = document.getElementById('app-sidebar');
  const backdrop = document.getElementById('sidebar-backdrop');

  if (toggleBtn && sidebar && backdrop) {
    toggleBtn.addEventListener('click', () => {
      sidebar.classList.toggle('open');
      backdrop.classList.toggle('show');
    });

    backdrop.addEventListener('click', () => {
      sidebar.classList.remove('open');
      backdrop.classList.remove('show');
    });
  }
}

function escapeHtml(text) {
  if (!text) return '';
  return text.replace(/[&<>"']/g, m => ({
    '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#039;'
  })[m]);
}

async function updateNotificationBadge() {
  try {
    const res = await api.get('/notifications/unread-count');
    const count = typeof res === 'number' ? res : (res?.count || 0);

    const badge = document.getElementById('header-notif-badge');
    if (badge) {
      if (count > 0) {
        badge.innerText = count > 99 ? '99+' : count;
        badge.style.display = 'inline-block';
      } else {
        badge.style.display = 'none';
      }
    }

    const mobBadge = document.getElementById('mobile-notif-badge');
    if (mobBadge) {
      if (count > 0) {
        mobBadge.innerText = count > 99 ? '99+' : count;
        mobBadge.style.display = 'inline-block';
      } else {
        mobBadge.style.display = 'none';
      }
    }
  } catch (_) {}
}

function startNotificationPolling() {
  updateNotificationBadge();
  setInterval(updateNotificationBadge, 30000);
}
