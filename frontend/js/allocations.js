/* =====================================================================
   allocations.js  -  create, edit and delete resource allocations.

   When the backend saves an allocation it may return a "warnings" list
   (skill gap, over-allocation, leave conflict). Those warnings are shown
   immediately, so an assignment is never silent.
   ===================================================================== */

let developers = [];
let projects = [];
let milestones = [];
let allocations = [];

document.addEventListener('DOMContentLoaded', function () {
    renderLayout('allocations.html', 'Resource Allocations',
        'Assign developers to milestones. Overlapping allocations above the availability raise a conflict.');

    document.getElementById('allocation-form').addEventListener('submit', saveAllocation);
    document.getElementById('allocation-reset').addEventListener('click', resetForm);
    document.getElementById('allocation-project').addEventListener('change', onProjectChanged);
    document.getElementById('allocation-milestone').addEventListener('change', previewSkills);
    document.getElementById('allocation-developer').addEventListener('change', previewSkills);
    document.getElementById('filter-developer').addEventListener('change', drawAllocations);
    document.getElementById('filter-project').addEventListener('change', drawAllocations);
    document.getElementById('clear-filters').addEventListener('click', function () {
        document.getElementById('filter-developer').value = '';
        document.getElementById('filter-project').value = '';
        drawAllocations();
    });

    loadAll();
});

async function loadAll() {
    try {
        developers = await api.get('/api/developers');
        projects = await api.get('/api/projects');
        milestones = await api.get('/api/milestones');
        allocations = await api.get('/api/allocations');

        fillSelect('allocation-developer', developers, 'id', function (d) {
            return d.name + ' (' + d.availabilityPercentage + '% available)';
        }, 'Select a developer');
        fillSelect('allocation-project', projects, 'id', function (p) { return p.name; }, 'Select a project');
        fillSelect('filter-developer', developers, 'id', function (d) { return d.name; }, 'All developers');
        fillSelect('filter-project', projects, 'id', function (p) { return p.name; }, 'All projects');
        onProjectChanged();
        drawAllocations();
    } catch (error) {
        showError('message-area', error);
    }
}

/* ---------------- form behaviour ---------------- */

/** Only the chosen project's milestones are offered. */
function onProjectChanged() {
    const projectId = Number(document.getElementById('allocation-project').value);
    const forProject = milestones.filter(function (m) { return m.projectId === projectId; });
    fillSelect('allocation-milestone', forProject, 'id', function (m) {
        return m.name + '  (' + formatDate(m.startDate) + ' - ' + formatDate(m.endDate) + ')';
    }, 'Whole project (no milestone)');
    previewSkills();
}

/** Shows, before saving, which required skills the chosen developer lacks. */
function previewSkills() {
    const container = document.getElementById('skill-preview');
    const developerId = Number(document.getElementById('allocation-developer').value);
    const milestoneId = Number(document.getElementById('allocation-milestone').value);

    if (!developerId || !milestoneId) {
        container.innerHTML = '';
        return;
    }
    const developer = developers.find(function (d) { return d.id === developerId; });
    const milestone = milestones.find(function (m) { return m.id === milestoneId; });
    if (!developer || !milestone || !milestone.requiredSkills.length) {
        container.innerHTML = '';
        return;
    }

    const owned = developer.skills.map(function (s) { return s.toLowerCase(); });
    const missing = milestone.requiredSkills.filter(function (skill) {
        return owned.indexOf(skill.toLowerCase()) < 0;
    });

    if (missing.length) {
        container.innerHTML = '<div class="message warning"><strong>SKILL GAP</strong> - '
            + escapeHtml(developer.name) + ' does not have: ' + skillChips(missing, missing)
            + '<br><span class="inline-note">The allocation can still be saved, but it will be reported as a skill gap.</span></div>';
    } else {
        container.innerHTML = '<div class="message success">'
            + escapeHtml(developer.name) + ' has every skill this milestone requires: '
            + skillChips(milestone.requiredSkills) + '</div>';
    }
}

async function saveAllocation(event) {
    event.preventDefault();
    clearMessage('message-area');

    const id = document.getElementById('allocation-id').value;
    const milestoneValue = document.getElementById('allocation-milestone').value;
    const payload = {
        developerId: Number(document.getElementById('allocation-developer').value),
        projectId: Number(document.getElementById('allocation-project').value),
        milestoneId: milestoneValue ? Number(milestoneValue) : null,
        startDate: document.getElementById('allocation-start').value,
        endDate: document.getElementById('allocation-end').value,
        allocationPercentage: Number(document.getElementById('allocation-percentage').value)
    };

    try {
        const saved = id
            ? await api.put('/api/allocations/' + id, payload)
            : await api.post('/api/allocations', payload);

        if (saved.warnings && saved.warnings.length) {
            showMessage('message-area', 'warning',
                'Allocation saved, but the system raised the following:', saved.warnings);
        } else {
            showMessage('message-area', 'success',
                'Allocation saved. No conflict and no skill gap.');
        }
        resetForm();
        await loadAll();
    } catch (error) {
        showError('message-area', error);
    }
}

function editAllocation(id) {
    const allocation = allocations.find(function (a) { return a.id === id; });
    if (!allocation) { return; }
    document.getElementById('allocation-id').value = allocation.id;
    document.getElementById('allocation-developer').value = allocation.developerId;
    document.getElementById('allocation-project').value = allocation.projectId;
    onProjectChanged();
    document.getElementById('allocation-milestone').value = allocation.milestoneId || '';
    document.getElementById('allocation-start').value = allocation.startDate;
    document.getElementById('allocation-end').value = allocation.endDate;
    document.getElementById('allocation-percentage').value = allocation.allocationPercentage;
    document.getElementById('allocation-form-title').textContent =
        'Edit allocation: ' + allocation.developerName + ' on ' + allocation.projectName;
    document.getElementById('allocation-submit').textContent = 'Update allocation';
    previewSkills();
    window.scrollTo({ top: 0, behavior: 'smooth' });
}

async function deleteAllocation(id) {
    if (!confirm('Delete this allocation?')) { return; }
    try {
        await api.del('/api/allocations/' + id);
        showMessage('message-area', 'success', 'Allocation deleted.');
        resetForm();
        await loadAll();
    } catch (error) {
        showError('message-area', error);
    }
}

function resetForm() {
    document.getElementById('allocation-form').reset();
    document.getElementById('allocation-id').value = '';
    document.getElementById('allocation-form-title').textContent = 'Allocate a developer';
    document.getElementById('allocation-submit').textContent = 'Save allocation';
    document.getElementById('skill-preview').innerHTML = '';
    onProjectChanged();
}

/* ---------------- table ---------------- */

function drawAllocations() {
    const developerFilter = document.getElementById('filter-developer').value;
    const projectFilter = document.getElementById('filter-project').value;

    const rows = allocations.filter(function (allocation) {
        if (developerFilter && String(allocation.developerId) !== developerFilter) { return false; }
        if (projectFilter && String(allocation.projectId) !== projectFilter) { return false; }
        return true;
    });

    const body = document.getElementById('allocation-body');
    if (!rows.length) {
        body.innerHTML = '<tr><td colspan="7" class="empty-note">No allocations match.</td></tr>';
        return;
    }
    body.innerHTML = rows.map(function (allocation) {
        return '<tr>'
             + '<td><strong>' + escapeHtml(allocation.developerName) + '</strong></td>'
             + '<td>' + escapeHtml(allocation.projectName) + '</td>'
             + '<td>' + escapeHtml(allocation.milestoneName || 'Whole project') + '</td>'
             + '<td class="nowrap">' + formatDate(allocation.startDate) + '</td>'
             + '<td class="nowrap">' + formatDate(allocation.endDate) + '</td>'
             + '<td class="nowrap"><strong>' + allocation.allocationPercentage + '%</strong></td>'
             + '<td><div class="row-actions no-print">'
             + '<button class="small secondary" onclick="editAllocation(' + allocation.id + ')">Edit</button>'
             + '<button class="small danger" onclick="deleteAllocation(' + allocation.id + ')">Delete</button>'
             + '</div></td></tr>';
    }).join('');
}
