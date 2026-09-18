/* =====================================================================
   projects.js  -  project CRUD and milestone CRUD on one page.
   ===================================================================== */

let projects = [];
let milestones = [];

document.addEventListener('DOMContentLoaded', function () {
    renderLayout('projects.html', 'Projects',
        'Create projects and break them into milestones with required skills.');

    document.getElementById('project-form').addEventListener('submit', saveProject);
    document.getElementById('project-reset').addEventListener('click', resetProjectForm);
    document.getElementById('milestone-form').addEventListener('submit', saveMilestone);
    document.getElementById('milestone-reset').addEventListener('click', resetMilestoneForm);

    loadAll();
});

async function loadAll() {
    try {
        projects = await api.get('/api/projects');
        milestones = await api.get('/api/milestones');
        drawProjects();
        drawMilestones();
        fillSelect('milestone-project', projects, 'id', function (p) { return p.name; }, 'Select a project');
    } catch (error) {
        showError('message-area', error);
    }
}

/* ---------------- projects ---------------- */

function drawProjects() {
    const body = document.getElementById('project-body');
    if (!projects.length) {
        body.innerHTML = '<tr><td colspan="7" class="empty-note">No projects yet.</td></tr>';
        return;
    }
    body.innerHTML = projects.map(function (project) {
        return '<tr>'
             + '<td><strong>' + escapeHtml(project.name) + '</strong><br>'
             + '<span class="inline-note">' + escapeHtml(project.description || '') + '</span></td>'
             + '<td class="nowrap">' + formatDate(project.startDate) + '<br>to ' + formatDate(project.endDate) + '</td>'
             + '<td class="nowrap">' + formatMoney(project.budget) + '</td>'
             + '<td>' + statusBadge(project.status) + '</td>'
             + '<td>' + project.milestoneCount + '</td>'
             + '<td>' + project.allocationCount + '</td>'
             + '<td><div class="row-actions no-print">'
             + '<button class="small secondary" onclick="editProject(' + project.id + ')">Edit</button>'
             + '<button class="small danger" onclick="deleteProject(' + project.id + ')">Delete</button>'
             + '</div></td></tr>';
    }).join('');
}

async function saveProject(event) {
    event.preventDefault();
    clearMessage('message-area');

    const id = document.getElementById('project-id').value;
    const payload = {
        name: document.getElementById('project-name').value,
        description: document.getElementById('project-description').value,
        startDate: document.getElementById('project-start').value,
        endDate: document.getElementById('project-end').value,
        budget: Number(document.getElementById('project-budget').value),
        status: document.getElementById('project-status').value
    };

    try {
        if (id) {
            await api.put('/api/projects/' + id, payload);
            showMessage('message-area', 'success', 'Project updated.');
        } else {
            await api.post('/api/projects', payload);
            showMessage('message-area', 'success', 'Project created.');
        }
        resetProjectForm();
        await loadAll();
    } catch (error) {
        showError('message-area', error);
    }
}

function editProject(id) {
    const project = projects.find(function (p) { return p.id === id; });
    if (!project) { return; }
    document.getElementById('project-id').value = project.id;
    document.getElementById('project-name').value = project.name;
    document.getElementById('project-description').value = project.description || '';
    document.getElementById('project-start').value = project.startDate;
    document.getElementById('project-end').value = project.endDate;
    document.getElementById('project-budget').value = project.budget;
    document.getElementById('project-status').value = project.status;
    document.getElementById('project-form-title').textContent = 'Edit project: ' + project.name;
    document.getElementById('project-submit').textContent = 'Update project';
    window.scrollTo({ top: 0, behavior: 'smooth' });
}

async function deleteProject(id) {
    if (!confirm('Delete this project? Its milestones, allocations and budget will be removed too.')) { return; }
    try {
        await api.del('/api/projects/' + id);
        showMessage('message-area', 'success', 'Project deleted.');
        resetProjectForm();
        await loadAll();
    } catch (error) {
        showError('message-area', error);
    }
}

function resetProjectForm() {
    document.getElementById('project-form').reset();
    document.getElementById('project-id').value = '';
    document.getElementById('project-form-title').textContent = 'Add a project';
    document.getElementById('project-submit').textContent = 'Save project';
}

/* ---------------- milestones ---------------- */

function drawMilestones() {
    const body = document.getElementById('milestone-body');
    if (!milestones.length) {
        body.innerHTML = '<tr><td colspan="10" class="empty-note">No milestones yet.</td></tr>';
        return;
    }
    body.innerHTML = milestones.map(function (milestone) {
        return '<tr>'
             + '<td>' + escapeHtml(milestone.projectName) + '</td>'
             + '<td><strong>' + escapeHtml(milestone.name) + '</strong></td>'
             + '<td class="nowrap">' + formatDate(milestone.startDate) + '<br>to ' + formatDate(milestone.endDate) + '</td>'
             + '<td class="nowrap">' + milestone.estimatedEffortDays + ' dev-days</td>'
             + '<td>' + barHtml(milestone.progressPercentage, 100)
             + '<div class="bar-label">' + milestone.progressPercentage + '%</div></td>'
             + '<td>' + skillChips(milestone.requiredSkills) + '</td>'
             + '<td>' + (milestone.assignedDevelopers.length
                    ? escapeHtml(milestone.assignedDevelopers.join(', '))
                    : '<span class="inline-note">Nobody yet</span>')
             + '<br><span class="inline-note">capacity ' + milestone.totalAllocationPercentage + '%</span></td>'
             + '<td class="nowrap">' + (milestone.estimatedDeliveryDate
                    ? formatDate(milestone.estimatedDeliveryDate)
                    : '<span class="inline-note">Not estimated</span>') + '</td>'
             + '<td>' + statusBadge(milestone.status) + '</td>'
             + '<td><div class="row-actions no-print">'
             + '<button class="small secondary" onclick="editMilestone(' + milestone.id + ')">Edit</button>'
             + '<button class="small danger" onclick="deleteMilestone(' + milestone.id + ')">Delete</button>'
             + '</div></td></tr>';
    }).join('');
}

async function saveMilestone(event) {
    event.preventDefault();
    clearMessage('message-area');

    const id = document.getElementById('milestone-id').value;
    const skillText = document.getElementById('milestone-skills').value;
    const payload = {
        projectId: Number(document.getElementById('milestone-project').value),
        name: document.getElementById('milestone-name').value,
        description: document.getElementById('milestone-description').value,
        startDate: document.getElementById('milestone-start').value,
        endDate: document.getElementById('milestone-end').value,
        progressPercentage: Number(document.getElementById('milestone-progress').value),
        status: document.getElementById('milestone-status').value,
        estimatedEffortDays: Number(document.getElementById('milestone-effort').value),
        requiredSkills: splitList(skillText)
    };

    try {
        if (id) {
            await api.put('/api/milestones/' + id, payload);
            showMessage('message-area', 'success', 'Milestone updated.');
        } else {
            await api.post('/api/milestones', payload);
            showMessage('message-area', 'success', 'Milestone created.');
        }
        resetMilestoneForm();
        await loadAll();
    } catch (error) {
        showError('message-area', error);
    }
}

function editMilestone(id) {
    const milestone = milestones.find(function (m) { return m.id === id; });
    if (!milestone) { return; }
    document.getElementById('milestone-id').value = milestone.id;
    document.getElementById('milestone-project').value = milestone.projectId;
    document.getElementById('milestone-name').value = milestone.name;
    document.getElementById('milestone-description').value = milestone.description || '';
    document.getElementById('milestone-start').value = milestone.startDate;
    document.getElementById('milestone-end').value = milestone.endDate;
    document.getElementById('milestone-progress').value = milestone.progressPercentage;
    document.getElementById('milestone-status').value = milestone.status;
    document.getElementById('milestone-effort').value = milestone.estimatedEffortDays;
    document.getElementById('milestone-skills').value = milestone.requiredSkills.join(', ');
    document.getElementById('milestone-form-title').textContent = 'Edit milestone: ' + milestone.name;
    document.getElementById('milestone-submit').textContent = 'Update milestone';
    document.getElementById('milestone-form').scrollIntoView({ behavior: 'smooth' });
}

async function deleteMilestone(id) {
    if (!confirm('Delete this milestone? Its allocations will be removed too.')) { return; }
    try {
        await api.del('/api/milestones/' + id);
        showMessage('message-area', 'success', 'Milestone deleted.');
        resetMilestoneForm();
        await loadAll();
    } catch (error) {
        showError('message-area', error);
    }
}

function resetMilestoneForm() {
    document.getElementById('milestone-form').reset();
    document.getElementById('milestone-id').value = '';
    document.getElementById('milestone-form-title').textContent = 'Add a milestone';
    document.getElementById('milestone-submit').textContent = 'Save milestone';
}

/** "Java, Spring Boot ,MySQL" -> ["Java", "Spring Boot", "MySQL"] */
function splitList(text) {
    if (!text) { return []; }
    return text.split(',').map(function (item) { return item.trim(); })
               .filter(function (item) { return item.length > 0; });
}
