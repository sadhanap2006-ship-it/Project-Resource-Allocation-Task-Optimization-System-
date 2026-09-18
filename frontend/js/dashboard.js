/* =====================================================================
   dashboard.js  -  the Project Manager dashboard.

   It calls ONE endpoint, /api/dashboard/summary, and draws everything
   from the object that comes back.
   ===================================================================== */

document.addEventListener('DOMContentLoaded', function () {
    renderLayout('dashboard.html', 'Project Manager Dashboard',
        'Projects, people, conflicts, budget and alerts in one place.');

    document.getElementById('refresh-notifications')
        .addEventListener('click', loadDashboard);

    loadDashboard();
});

async function loadDashboard() {
    clearMessage('message-area');
    try {
        const summary = await api.get('/api/dashboard/summary');
        drawStats(summary);
        drawUtilization(summary.utilization);
        drawBudget(summary);
        drawConflicts(summary.conflicts);
        drawSkillGaps(summary.skillGapDetails);
        drawUpcoming(summary.upcomingMilestones);
        drawNotifications(summary.notifications);
    } catch (error) {
        showError('message-area', error);
    }
}

/* ---------------- the tiles across the top ---------------- */

function drawStats(summary) {
    const conflictTotal = summary.overAllocationConflicts + summary.leaveConflicts;
    const tiles = [
        { label: 'Total projects',   value: summary.totalProjects,      kind: '' },
        { label: 'Active projects',  value: summary.activeProjects,     kind: '' },
        { label: 'At-risk projects', value: summary.atRiskProjects,     kind: summary.atRiskProjects > 0 ? 'stat-warn' : 'stat-ok' },
        { label: 'Developers',       value: summary.totalDevelopers,    kind: '' },
        { label: 'Avg utilization',  value: formatPercent(summary.averageUtilization), kind: '' },
        { label: 'Over-allocations', value: summary.overAllocationConflicts, kind: summary.overAllocationConflicts > 0 ? 'stat-danger' : 'stat-ok' },
        { label: 'Leave conflicts',  value: summary.leaveConflicts,     kind: summary.leaveConflicts > 0 ? 'stat-warn' : 'stat-ok' },
        { label: 'Skill gaps',       value: summary.skillGaps,          kind: summary.skillGaps > 0 ? 'stat-warn' : 'stat-ok' },
        { label: 'Budget used',      value: summary.overallBudgetUtilization === null ? 'No data' : formatPercent(summary.overallBudgetUtilization),
          kind: summary.overallBudgetUtilization >= 90 ? 'stat-danger' : (summary.overallBudgetUtilization >= 75 ? 'stat-warn' : '') },
        { label: 'Unread alerts',    value: summary.unreadNotifications, kind: conflictTotal > 0 ? 'stat-warn' : '' }
    ];

    document.getElementById('stat-grid').innerHTML = tiles.map(function (tile) {
        return '<div class="stat ' + tile.kind + '">'
             + '<div class="stat-value">' + escapeHtml(tile.value) + '</div>'
             + '<div class="stat-label">' + escapeHtml(tile.label) + '</div></div>';
    }).join('');
}

/* ---------------- utilization table ---------------- */

function drawUtilization(rows) {
    const body = document.getElementById('utilization-body');
    if (!rows.length) {
        body.innerHTML = '<tr><td colspan="5" class="empty-note">No developers yet.</td></tr>';
        return;
    }
    body.innerHTML = rows.map(function (row) {
        const conflictClass = (row.status === 'Conflict') ? ' class="row-conflict"' : '';
        return '<tr' + conflictClass + '>'
             + '<td>' + escapeHtml(row.developerName)
             + (row.onLeaveToday ? ' <span class="badge badge-warn">on leave</span>' : '') + '</td>'
             + '<td class="nowrap"><strong>' + row.peakAllocation + '%</strong></td>'
             + '<td>' + barHtml(row.peakAllocation, row.availability) + '</td>'
             + '<td class="nowrap">' + row.availability + '%</td>'
             + '<td>' + statusBadge(row.status) + '</td>'
             + '</tr>';
    }).join('');
}

/* ---------------- budget ---------------- */

function drawBudget(summary) {
    const container = document.getElementById('budget-overview');
    let html = '<p><strong>Total budget:</strong> ' + formatMoney(summary.totalBudget)
             + '<br><strong>Total spent:</strong> ' + formatMoney(summary.totalSpending) + '</p>';

    if (summary.overallBudgetUtilization === null) {
        html += '<div class="message info">' + escapeHtml(summary.budgetMessage) + '</div>';
    } else {
        const kind = summary.overallBudgetUtilization >= 90 ? 'danger'
                   : (summary.overallBudgetUtilization >= 75 ? 'warn' : 'info');
        html += '<div class="bar-track"><div class="bar-fill ' + kind
              + '" style="width:' + Math.min(100, summary.overallBudgetUtilization) + '%"></div></div>'
              + '<div class="bar-label">' + formatPercent(summary.overallBudgetUtilization) + ' of the total budget used</div>';
    }

    if (summary.budgetWarnings.length) {
        html += '<h4 style="margin:14px 0 6px;font-size:14px;">Projects that need attention</h4>';
        html += summary.budgetWarnings.map(function (budget) {
            return '<div class="message ' + (budget.warningLevel === 'CRITICAL' ? 'error' : 'warning') + '">'
                 + escapeHtml(budget.message) + '</div>';
        }).join('');
    } else {
        html += '<p class="inline-note">No project has crossed the 75% budget warning line.</p>';
    }
    container.innerHTML = html;
}

/* ---------------- conflicts ---------------- */

function drawConflicts(conflicts) {
    const container = document.getElementById('conflict-list');
    if (!conflicts.length) {
        container.innerHTML = '<p class="empty-note">No conflicts. Every developer is inside their availability.</p>';
        return;
    }
    container.innerHTML = conflicts.map(function (conflict) {
        return '<div class="notification ' + (conflict.severity === 'CRITICAL' ? 'CRITICAL' : 'WARNING') + '">'
             + escapeHtml(conflict.message)
             + '<span class="meta">' + escapeHtml(conflict.involvedAllocations.join('  +  ')) + '</span>'
             + '</div>';
    }).join('');
}

/* ---------------- skill gaps ---------------- */

function drawSkillGaps(gaps) {
    const container = document.getElementById('skillgap-list');
    if (!gaps.length) {
        container.innerHTML = '<p class="empty-note">Every milestone has all of its required skills covered.</p>';
        return;
    }
    container.innerHTML = gaps.map(function (gap) {
        return '<div class="notification WARNING">'
             + '<strong>' + escapeHtml(gap.projectName) + ' / ' + escapeHtml(gap.milestoneName) + '</strong><br>'
             + 'Missing: ' + skillChips(gap.missingSkills, gap.missingSkills)
             + '<span class="meta">' + escapeHtml(gap.suggestions.join(' | ')) + '</span>'
             + '</div>';
    }).join('');
}

/* ---------------- upcoming milestones ---------------- */

function drawUpcoming(milestones) {
    const container = document.getElementById('upcoming-list');
    if (!milestones.length) {
        container.innerHTML = '<p class="empty-note">No milestone is due in the next 30 days.</p>';
        return;
    }
    container.innerHTML = '<table class="data-table"><thead><tr>'
        + '<th>Milestone</th><th>Due</th><th>Progress</th><th>Status</th></tr></thead><tbody>'
        + milestones.map(function (milestone) {
            return '<tr><td><strong>' + escapeHtml(milestone.name) + '</strong>'
                 + '<div class="inline-note">' + escapeHtml(milestone.projectName) + '</div></td>'
                 + '<td class="nowrap">' + formatDate(milestone.endDate) + '</td>'
                 + '<td>' + barHtml(milestone.progressPercentage, 100)
                 + '<div class="bar-label">' + milestone.progressPercentage + '%</div></td>'
                 + '<td>' + statusBadge(milestone.status) + '</td></tr>';
        }).join('')
        + '</tbody></table>';
}

/* ---------------- notifications ---------------- */

function drawNotifications(notifications) {
    const container = document.getElementById('notification-list');
    if (!notifications.length) {
        container.innerHTML = '<p class="empty-note">No notifications.</p>';
        return;
    }
    container.innerHTML = notifications.slice(0, 25).map(function (notification) {
        return '<div class="notification ' + escapeHtml(notification.severity) + '">'
             + escapeHtml(notification.message)
             + '<span class="meta">' + escapeHtml(notification.type.replace(/_/g, ' '))
             + ' &middot; ' + formatDateTime(notification.createdAt)
             + (notification.read ? '' : ' &middot; unread') + '</span></div>';
    }).join('');
}
