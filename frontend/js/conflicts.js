/* =====================================================================
   conflicts.js  -  over-allocation, leave conflicts, skill gaps and the
   resource utilization table, all on one screen.
   ===================================================================== */

document.addEventListener('DOMContentLoaded', function () {
    renderLayout('conflicts.html', 'Conflicts and Skill Gaps',
        'Everything the system found wrong with the current plan.');
    loadAll();
});

async function loadAll() {
    clearMessage('message-area');
    try {
        const conflicts = await api.get('/api/conflicts');
        const gaps = await api.get('/api/skill-gaps');
        const utilization = await api.get('/api/utilization');

        const overAllocations = conflicts.filter(function (c) { return c.type === 'OVER_ALLOCATION'; });
        const leaveConflicts = conflicts.filter(function (c) { return c.type === 'LEAVE_CONFLICT'; });

        drawStats(overAllocations.length, leaveConflicts.length, gaps.length, utilization);
        drawOverAllocations(overAllocations);
        drawLeaveConflicts(leaveConflicts);
        drawSkillGaps(gaps);
        drawUtilization(utilization);
    } catch (error) {
        showError('message-area', error);
    }
}

function drawStats(over, leave, gaps, utilization) {
    const conflicted = utilization.filter(function (row) { return row.status === 'Conflict'; }).length;
    const tiles = [
        { label: 'Over-allocations', value: over,  kind: over > 0 ? 'stat-danger' : 'stat-ok' },
        { label: 'Leave conflicts',  value: leave, kind: leave > 0 ? 'stat-warn' : 'stat-ok' },
        { label: 'Skill gaps',       value: gaps,  kind: gaps > 0 ? 'stat-warn' : 'stat-ok' },
        { label: 'Developers in conflict', value: conflicted, kind: conflicted > 0 ? 'stat-danger' : 'stat-ok' }
    ];
    document.getElementById('stat-grid').innerHTML = tiles.map(function (tile) {
        return '<div class="stat ' + tile.kind + '"><div class="stat-value">' + tile.value
             + '</div><div class="stat-label">' + escapeHtml(tile.label) + '</div></div>';
    }).join('');
}

function drawOverAllocations(conflicts) {
    const body = document.getElementById('over-body');
    if (!conflicts.length) {
        body.innerHTML = '<tr><td colspan="5" class="empty-note">No over-allocation. Every developer is inside their availability.</td></tr>';
        return;
    }
    body.innerHTML = conflicts.map(function (conflict) {
        return '<tr class="row-conflict">'
             + '<td><strong>' + escapeHtml(conflict.developerName) + '</strong></td>'
             + '<td class="nowrap">' + formatDate(conflict.startDate) + ' to ' + formatDate(conflict.endDate) + '</td>'
             + '<td class="nowrap"><strong>' + conflict.totalAllocation + '%</strong></td>'
             + '<td class="nowrap">' + conflict.availability + '%</td>'
             + '<td>' + conflict.involvedAllocations.map(function (item) {
                    return '<div>' + escapeHtml(item) + '</div>';
               }).join('') + '</td></tr>';
    }).join('');
}

function drawLeaveConflicts(conflicts) {
    const body = document.getElementById('leave-body');
    if (!conflicts.length) {
        body.innerHTML = '<tr><td colspan="4" class="empty-note">Nobody is allocated during their leave.</td></tr>';
        return;
    }
    body.innerHTML = conflicts.map(function (conflict) {
        return '<tr class="row-warning">'
             + '<td><strong>' + escapeHtml(conflict.developerName) + '</strong></td>'
             + '<td class="nowrap">' + formatDate(conflict.startDate) + ' to ' + formatDate(conflict.endDate) + '</td>'
             + '<td class="nowrap">' + conflict.totalAllocation + '%</td>'
             + '<td>' + escapeHtml(conflict.message) + '</td></tr>';
    }).join('');
}

function drawSkillGaps(gaps) {
    const container = document.getElementById('skillgap-body');
    if (!gaps.length) {
        container.innerHTML = '<p class="empty-note">Every milestone has all of its required skills covered.</p>';
        return;
    }
    container.innerHTML = gaps.map(function (gap) {
        return '<div class="message warning">'
             + '<strong>' + escapeHtml(gap.projectName) + ' / ' + escapeHtml(gap.milestoneName) + '</strong><br>'
             + 'Required: ' + skillChips(gap.requiredSkills, gap.missingSkills) + '<br>'
             + 'Covered by the team: ' + skillChips(gap.coveredSkills) + '<br>'
             + 'Missing: ' + skillChips(gap.missingSkills, gap.missingSkills)
             + '<ul>' + gap.suggestions.map(function (suggestion) {
                    return '<li>' + escapeHtml(suggestion) + '</li>';
               }).join('') + '</ul></div>';
    }).join('');
}

function drawUtilization(rows) {
    const body = document.getElementById('utilization-body');
    if (!rows.length) {
        body.innerHTML = '<tr><td colspan="6" class="empty-note">No developers yet.</td></tr>';
        return;
    }
    body.innerHTML = rows.map(function (row) {
        const rowClass = row.status === 'Conflict' ? ' class="row-conflict"'
                       : (row.status === 'High Load' ? ' class="row-warning"' : '');
        return '<tr' + rowClass + '>'
             + '<td><strong>' + escapeHtml(row.developerName) + '</strong>'
             + (row.onLeaveToday ? ' <span class="badge badge-warn">on leave today</span>' : '') + '</td>'
             + '<td class="nowrap">' + row.peakAllocation + '%</td>'
             + '<td>' + barHtml(row.peakAllocation, row.availability) + '</td>'
             + '<td class="nowrap">' + row.availability + '%</td>'
             + '<td class="nowrap">' + row.remainingCapacity + '%</td>'
             + '<td>' + statusBadge(row.status) + '</td></tr>';
    }).join('');
}
