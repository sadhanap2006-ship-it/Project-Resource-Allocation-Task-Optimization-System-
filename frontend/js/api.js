/* =====================================================================
   api.js  -  shared JavaScript used by every page.

   It does three things:
     1. wraps the browser's fetch() so a request is one short line
     2. holds small formatting helpers (dates, rupees, percentages)
     3. draws the navigation bar so it is written only once

   Plain JavaScript only - no framework, no build step.
   ===================================================================== */

/* When the pages are served by Spring Boot the API is on the same address,
   so the base is empty. When index.html is opened from a different port
   (for example the VS Code Live Server on 5500) we fall back to port 8080. */
const API_BASE = (window.location.port === '8080' || window.location.port === '')
    ? ''
    : 'http://localhost:8080';

/* ---------------------------------------------------------------------
   1. Talking to the REST API
   ------------------------------------------------------------------ */

/**
 * Sends one request and returns the parsed JSON.
 * If the backend answers with an error, its messages are thrown as an Error
 * so the page can show them with showError().
 */
async function apiRequest(method, path, body) {
    const options = { method: method, headers: {} };
    if (body !== undefined && body !== null) {
        options.headers['Content-Type'] = 'application/json';
        options.body = JSON.stringify(body);
    }

    let response;
    try {
        response = await fetch(API_BASE + path, options);
    } catch (networkError) {
        throw new Error('Cannot reach the server. Is the Spring Boot application running on port 8080?');
    }

    if (response.status === 204) {
        return null;
    }

    const text = await response.text();
    let data = null;
    if (text) {
        try {
            data = JSON.parse(text);
        } catch (parseError) {
            data = null;
        }
    }

    if (!response.ok) {
        // our GlobalExceptionHandler always sends { messages: [...] }
        const messages = (data && data.messages) ? data.messages : ['Request failed (HTTP ' + response.status + ')'];
        const error = new Error(messages.join('\n'));
        error.messages = messages;
        error.status = response.status;
        throw error;
    }
    return data;
}

const api = {
    get:  (path)        => apiRequest('GET', path),
    post: (path, body)  => apiRequest('POST', path, body),
    put:  (path, body)  => apiRequest('PUT', path, body),
    del:  (path)        => apiRequest('DELETE', path)
};

/* ---------------------------------------------------------------------
   2. Formatting helpers
   ------------------------------------------------------------------ */

/** "2026-09-18" -> "18 Sep 2026" */
function formatDate(isoDate) {
    if (!isoDate) { return '-'; }
    const parts = String(isoDate).substring(0, 10).split('-');
    if (parts.length !== 3) { return isoDate; }
    const months = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun',
                    'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
    return Number(parts[2]) + ' ' + months[Number(parts[1]) - 1] + ' ' + parts[0];
}

/** "2026-09-18T10:15:00" -> "18 Sep 2026, 10:15" */
function formatDateTime(isoDateTime) {
    if (!isoDateTime) { return '-'; }
    const time = String(isoDateTime).substring(11, 16);
    return formatDate(isoDateTime) + (time ? ', ' + time : '');
}

/** 52800 -> "Rs. 52,800.00" (Indian digit grouping) */
function formatMoney(amount) {
    if (amount === null || amount === undefined || amount === '') { return '-'; }
    const value = Number(amount);
    if (isNaN(value)) { return String(amount); }
    return 'Rs. ' + value.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

function formatPercent(value) {
    if (value === null || value === undefined) { return '-'; }
    return Number(value).toFixed(2).replace(/\.00$/, '') + '%';
}

/** Never write user text straight into innerHTML without this. */
function escapeHtml(value) {
    if (value === null || value === undefined) { return ''; }
    return String(value)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
}

/** Days between two "yyyy-MM-dd" strings, counting both ends. */
function daysBetween(fromIso, toIso) {
    const from = new Date(fromIso + 'T00:00:00');
    const to = new Date(toIso + 'T00:00:00');
    return Math.round((to - from) / 86400000) + 1;
}

/* ---------------------------------------------------------------------
   3. Page furniture
   ------------------------------------------------------------------ */

const NAV_LINKS = [
    { href: 'dashboard.html',   label: 'Dashboard' },
    { href: 'projects.html',    label: 'Projects' },
    { href: 'developers.html',  label: 'Developers' },
    { href: 'allocations.html', label: 'Allocations' },
    { href: 'conflicts.html',   label: 'Conflicts' },
    { href: 'budget.html',      label: 'Budget' },
    { href: 'timeline.html',    label: 'Timeline' },
    { href: 'simulator.html',   label: 'What-If Simulator' }
];

/** Draws the header and the navigation bar into the current page. */
function renderLayout(activePage, title, hint) {
    const session = getSession();
    const header = document.getElementById('app-header');
    if (header) {
        header.innerHTML =
            '<h1>Project Resource Allocation System</h1>' +
            '<div class="subtitle">SIST CSE Capstone Project</div>';
    }

    const nav = document.getElementById('app-nav');
    if (nav) {
        let html = NAV_LINKS.map(function (link) {
            const active = (link.href === activePage) ? ' class="active"' : '';
            return '<a href="' + link.href + '"' + active + '>' + link.label + '</a>';
        }).join('');
        html += '<span class="nav-spacer"></span>';
        if (session) {
            html += '<span class="nav-user">Signed in as ' + escapeHtml(session.username)
                  + ' (' + escapeHtml(session.role.replace('_', ' ')) + ')</span>';
        }
        html += '<a href="index.html">Sign out</a>';
        nav.innerHTML = html;
    }

    const titleElement = document.getElementById('page-title');
    if (titleElement && title) { titleElement.textContent = title; }
    const hintElement = document.getElementById('page-hint');
    if (hintElement && hint) { hintElement.textContent = hint; }
}

/** Shows a red box with the error text at the top of the page. */
function showError(containerId, error) {
    const container = document.getElementById(containerId);
    if (!container) { return; }
    const messages = (error && error.messages) ? error.messages : [String(error && error.message ? error.message : error)];
    container.innerHTML = '<div class="message error"><strong>Error</strong><ul>'
        + messages.map(function (m) { return '<li>' + escapeHtml(m) + '</li>'; }).join('')
        + '</ul></div>';
    container.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
}

function showMessage(containerId, kind, text, extraLines) {
    const container = document.getElementById(containerId);
    if (!container) { return; }
    let html = '<div class="message ' + kind + '">' + escapeHtml(text);
    if (extraLines && extraLines.length) {
        html += '<ul>' + extraLines.map(function (line) {
            return '<li>' + escapeHtml(line) + '</li>';
        }).join('') + '</ul>';
    }
    html += '</div>';
    container.innerHTML = html;
}

function clearMessage(containerId) {
    const container = document.getElementById(containerId);
    if (container) { container.innerHTML = ''; }
}

/* ---------------------------------------------------------------------
   4. Who is signed in (kept in sessionStorage - no server session)
   ------------------------------------------------------------------ */

function saveSession(session) {
    sessionStorage.setItem('rms-session', JSON.stringify(session));
}

function getSession() {
    const raw = sessionStorage.getItem('rms-session');
    return raw ? JSON.parse(raw) : null;
}

function clearSession() {
    sessionStorage.removeItem('rms-session');
}

/* ---------------------------------------------------------------------
   5. Small drawing helpers shared by several pages
   ------------------------------------------------------------------ */

/** A coloured percentage bar. Over 100% is always red. */
function barHtml(percentage, availability) {
    const limit = availability || 100;
    const width = Math.min(100, (percentage / Math.max(limit, 1)) * 100);
    let cssClass = 'bar-fill';
    if (percentage > limit) {
        cssClass += ' danger';
    } else if (percentage >= limit * 0.8) {
        cssClass += ' warn';
    }
    return '<div class="bar-track"><div class="' + cssClass + '" style="width:' + width + '%"></div></div>';
}

/** Turns a status word into a coloured badge. */
function statusBadge(status) {
    const map = {
        'ACTIVE': 'ok', 'INACTIVE': 'muted', 'ON_LEAVE': 'warn',
        'PLANNED': 'muted', 'IN_PROGRESS': 'info', 'COMPLETED': 'ok',
        'AT_RISK': 'warn', 'DELAYED': 'danger',
        'Available': 'ok', 'High Load': 'warn', 'Fully Allocated': 'info', 'Conflict': 'danger',
        'APPROVED': 'ok', 'CANCELLED': 'muted',
        'NONE': 'ok', 'WARNING': 'warn', 'CRITICAL': 'danger', 'INFO': 'info'
    };
    const kind = map[status] || 'muted';
    return '<span class="badge badge-' + kind + '">' + escapeHtml(String(status).replace(/_/g, ' ')) + '</span>';
}

function skillChips(skills, missing) {
    if (!skills || !skills.length) { return '<span class="inline-note">None</span>'; }
    const missingList = (missing || []).map(function (s) { return s.toLowerCase(); });
    return skills.map(function (skill) {
        const isMissing = missingList.indexOf(String(skill).toLowerCase()) >= 0;
        return '<span class="skill-chip' + (isMissing ? ' missing' : '') + '">' + escapeHtml(skill) + '</span>';
    }).join('');
}

/** Fills a <select> with options. */
function fillSelect(selectId, items, valueKey, labelFunction, placeholder) {
    const select = document.getElementById(selectId);
    if (!select) { return; }
    let html = placeholder ? '<option value="">' + escapeHtml(placeholder) + '</option>' : '';
    html += items.map(function (item) {
        return '<option value="' + item[valueKey] + '">' + escapeHtml(labelFunction(item)) + '</option>';
    }).join('');
    select.innerHTML = html;
}
