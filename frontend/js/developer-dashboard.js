/* =====================================================================
   developer-dashboard.js  -  the simpler dashboard a developer sees.

   It calls ONE endpoint: /api/dashboard/developer/{id}
   ===================================================================== */

document.addEventListener('DOMContentLoaded', async function () {
    renderLayout('', 'My Dashboard', 'Your projects, milestones, capacity, leave and conflicts.');

    const picker = document.getElementById('developer-picker');
    picker.addEventListener('change', function () { loadDashboard(picker.value); });

    try {
        const developers = await api.get('/api/developers');
        fillSelect('developer-picker', developers, 'id', function (d) { return d.name; }, '');

        // open the signed-in developer's own dashboard when there is one
        const session = getSession();
        const startId = (session && session.developerId) ? session.developerId
                      : (developers.length ? developers[0].id : null);
        if (startId) {
            picker.value = startId;
            await loadDashboard(startId);
        }
    } catch (error) {
        showError('message-area', error);
    }
});

async function loadDashboard(developerId) {
    if (!developerId) { return; }
    clearMessage('message-area');
    try {
        const dashboard = await api.get('/api/dashboard/developer/' + developerId);
        document.getElementById('page-title').textContent = dashboard.developerName + ' - My Dashboard';
        drawStats(dashboard);
        drawProjects(dashboard.myProjects);
        drawLeave(dashboard.myLeave);
        drawConflicts(dashboard.myConflicts);
        drawDeadlines(dashboard.upcomingDeadlines);
        drawMilestones(dashboard.myMilestones);
        drawAllocations(dashboard.myAllocations);
        drawNotifications(dashboard.myNotifications);
    } catch (error) {
        showError('message-area', error);
    }
}

function drawStats(dashboard) {
    const conflicted = dashboard.myConflicts.length > 0;
    const tiles = [
        { label: 'My projects',       value: dashboard.myProjects.length,   kind: '' },
        { label: 'My milestones',     value: dashboard.myMilestones.length, kind: '' },
        { label: 'Current allocation', value: dashboard.currentAllocation + '%',
          kind: dashboard.currentAllocation > dashboard.availability ? 'stat-danger' : '' },
        { label: 'Available capacity', value: dashboard.availableCapacity + '%',
          kind: dashboard.availableCapacity === 0 ? 'stat-warn' : 'stat-ok' },
        { label: 'My conflicts',      value: dashboard.myConflicts.length, kind: conflicted ? 'stat-danger' : 'stat-ok' },
        { label: 'Upcoming deadlines', value: dashboard.upcomingDeadlines.length, kind: '' }
    ];
    document.getElementById('stat-grid').innerHTML = tiles.map(function (tile) {
        return '<div class="stat ' + tile.kind + '"><div class="stat-value">' + escapeHtml(tile.value)
             + '</div><div class="stat-label">' + escapeHtml(tile.label) + '</div></div>';
    }).join('');
}

function drawProjects(projects) {
    const container = document.getElementById('my-projects');
    container.innerHTML = projects.length
        ? '<ul>' + projects.map(function (name) { return '<li>' + escapeHtml(name) + '</li>'; }).join('') + '</ul>'
        : '<p class="empty-note">You are not allocated to any project.</p>';
}

function drawLeave(leaves) {
    const container = document.getElementById('my-leave');
    if (!leaves.length) {
        container.innerHTML = '<p class="empty-note">No leave recorded.</p>';
        return;
    }
    container.innerHTML = leaves.map(function (leave) {
        return '<div class="notification INFO">'
             + formatDate(leave.startDate) + ' to ' + formatDate(leave.endDate)
             + ' &nbsp;' + statusBadge(leave.status)
             + '<span class="meta">' + escapeHtml(leave.reason || 'No reason given') + '</span></div>';
    }).join('');
}

function drawConflicts(conflicts) {
    const container = document.getElementById('my-conflicts');
    if (!conflicts.length) {
        container.innerHTML = '<p class="empty-note">No conflicts. Your workload is inside your availability.</p>';
        return;
    }
    container.innerHTML = conflicts.map(function (conflict) {
        return '<div class="notification ' + escapeHtml(conflict.severity) + '">'
             + escapeHtml(conflict.message) + '</div>';
    }).join('');
}

function drawDeadlines(milestones) {
    const container = document.getElementById('my-deadlines');
    if (!milestones.length) {
        container.innerHTML = '<p class="empty-note">Nothing is due in the next 30 days.</p>';
        return;
    }
    container.innerHTML = milestones.map(function (milestone) {
        return '<div class="notification WARNING">'
             + '<strong>' + escapeHtml(milestone.name) + '</strong> (' + escapeHtml(milestone.projectName) + ')'
             + '<span class="meta">Due ' + formatDate(milestone.endDate)
             + ' &middot; ' + milestone.progressPercentage + '% complete</span></div>';
    }).join('');
}

function drawMilestones(milestones) {
    const body = document.getElementById('my-milestones');
    if (!milestones.length) {
        body.innerHTML = '<tr><td colspan="6" class="empty-note">No milestones assigned.</td></tr>';
        return;
    }
    body.innerHTML = milestones.map(function (milestone) {
        return '<tr>'
             + '<td>' + escapeHtml(milestone.projectName) + '</td>'
             + '<td><strong>' + escapeHtml(milestone.name) + '</strong></td>'
             + '<td class="nowrap">' + formatDate(milestone.startDate) + ' to ' + formatDate(milestone.endDate) + '</td>'
             + '<td>' + barHtml(milestone.progressPercentage, 100)
             + '<div class="bar-label">' + milestone.progressPercentage + '%</div></td>'
             + '<td>' + skillChips(milestone.requiredSkills) + '</td>'
             + '<td>' + statusBadge(milestone.status) + '</td></tr>';
    }).join('');
}

function drawAllocations(allocations) {
    const body = document.getElementById('my-allocations');
    if (!allocations.length) {
        body.innerHTML = '<tr><td colspan="5" class="empty-note">No allocations.</td></tr>';
        return;
    }
    body.innerHTML = allocations.map(function (allocation) {
        return '<tr>'
             + '<td>' + escapeHtml(allocation.projectName) + '</td>'
             + '<td>' + escapeHtml(allocation.milestoneName || 'Whole project') + '</td>'
             + '<td class="nowrap">' + formatDate(allocation.startDate) + '</td>'
             + '<td class="nowrap">' + formatDate(allocation.endDate) + '</td>'
             + '<td class="nowrap"><strong>' + allocation.allocationPercentage + '%</strong></td></tr>';
    }).join('');
}

function drawNotifications(notifications) {
    const container = document.getElementById('my-notifications');
    if (!notifications.length) {
        container.innerHTML = '<p class="empty-note">No notifications for you.</p>';
        return;
    }
    container.innerHTML = notifications.map(function (notification) {
        return '<div class="notification ' + escapeHtml(notification.severity) + '">'
             + escapeHtml(notification.message)
             + '<span class="meta">' + escapeHtml(notification.type.replace(/_/g, ' '))
             + ' &middot; ' + formatDateTime(notification.createdAt) + '</span></div>';
    }).join('');
}
