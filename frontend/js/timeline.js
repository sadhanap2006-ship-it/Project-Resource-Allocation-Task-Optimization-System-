/* =====================================================================
   timeline.js  -  a simple Gantt-style timeline drawn with plain divs.

   How the bars are positioned:
     the project's own start and end dates form the full width (100%).
     For each milestone:
        left  = (milestone start - project start) / project length * 100
        width = milestone length / project length * 100
   Everything else is CSS.
   ===================================================================== */

let timelineData = [];

document.addEventListener('DOMContentLoaded', function () {
    renderLayout('timeline.html', 'Project Timeline',
        'Each project with its milestones, their progress and the developers assigned.');

    document.getElementById('timeline-project').addEventListener('change', draw);
    load();
});

async function load() {
    clearMessage('message-area');
    try {
        timelineData = await api.get('/api/timeline');
        const select = document.getElementById('timeline-project');
        select.innerHTML = '<option value="">All projects</option>'
            + timelineData.map(function (project) {
                return '<option value="' + project.projectId + '">' + escapeHtml(project.projectName) + '</option>';
            }).join('');
        draw();
    } catch (error) {
        showError('message-area', error);
    }
}

function draw() {
    const selected = document.getElementById('timeline-project').value;
    const container = document.getElementById('timeline-area');

    const projects = selected
        ? timelineData.filter(function (p) { return String(p.projectId) === selected; })
        : timelineData;

    if (!projects.length) {
        container.innerHTML = '<p class="empty-note">No projects to show.</p>';
        return;
    }

    container.innerHTML = projects.map(drawProject).join('');
}

function drawProject(project) {
    const projectDays = daysBetween(project.projectStartDate, project.projectEndDate);

    let html = '<div class="timeline-project">'
        + '<div class="project-head">' + escapeHtml(project.projectName)
        + ' &nbsp;<span class="badge badge-muted">' + escapeHtml(project.projectStatus.replace('_', ' ')) + '</span>'
        + ' &nbsp;<span class="inline-note">' + formatDate(project.projectStartDate)
        + ' to ' + formatDate(project.projectEndDate) + '</span></div>';

    if (!project.milestones.length) {
        html += '<p class="empty-note">This project has no milestones yet.</p></div>';
        return html;
    }

    project.milestones.forEach(function (milestone, index) {
        const isLast = index === project.milestones.length - 1;
        const offsetDays = daysBetween(project.projectStartDate, milestone.startDate) - 1;
        const lengthDays = daysBetween(milestone.startDate, milestone.endDate);

        const left = (offsetDays / projectDays) * 100;
        const width = Math.max(4, (lengthDays / projectDays) * 100);

        html += '<div class="timeline-row">'
             + '<div class="ms-name"><span class="tree">' + (isLast ? '&#9492;&#9472;' : '&#9500;&#9472;') + '</span>'
             + '<strong>' + escapeHtml(milestone.name) + '</strong>'
             + '<div class="timeline-meta">' + formatDate(milestone.startDate) + ' - ' + formatDate(milestone.endDate) + '</div>'
             + '<div class="timeline-meta">' + (milestone.assignedDevelopers.length
                    ? escapeHtml(milestone.assignedDevelopers.join(', '))
                    : 'Nobody assigned') + '</div>'
             + '</div>'
             + '<div>'
             + '<div class="timeline-track">'
             + '<div class="timeline-bar ' + escapeHtml(milestone.status) + '" '
             + 'style="left:' + left + '%;width:' + width + '%" '
             + 'title="' + escapeHtml(milestone.name) + ' - ' + milestone.progressPercentage + '% complete">'
             + '<div class="progress-inner" style="width:' + milestone.progressPercentage + '%"></div>'
             + '<span style="position:relative">' + milestone.progressPercentage + '%</span>'
             + '</div></div>'
             + '<div class="timeline-meta">' + statusBadge(milestone.status)
             + ' &nbsp;Estimated delivery: ' + (milestone.estimatedDeliveryDate
                    ? formatDate(milestone.estimatedDeliveryDate) : 'not estimated')
             + ' &nbsp;(capacity ' + milestone.totalAllocationPercentage + '%)'
             + '</div></div></div>';
    });

    html += '</div>';
    return html;
}
