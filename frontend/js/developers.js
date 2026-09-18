/* =====================================================================
   developers.js  -  resource profiles, their skills, and planned leave.
   ===================================================================== */

let developers = [];
let leaves = [];

document.addEventListener('DOMContentLoaded', function () {
    renderLayout('developers.html', 'Developers',
        'Resource profiles, skills and planned leave.');

    document.getElementById('developer-form').addEventListener('submit', saveDeveloper);
    document.getElementById('developer-reset').addEventListener('click', resetDeveloperForm);
    document.getElementById('leave-form').addEventListener('submit', saveLeave);

    loadAll();
});

async function loadAll() {
    try {
        developers = await api.get('/api/developers');
        leaves = await api.get('/api/leaves');
        drawDevelopers();
        drawLeaves();
        fillSelect('leave-developer', developers, 'id', function (d) { return d.name; }, 'Select a developer');
    } catch (error) {
        showError('message-area', error);
    }
}

/* ---------------- developers ---------------- */

function drawDevelopers() {
    const body = document.getElementById('developer-body');
    if (!developers.length) {
        body.innerHTML = '<tr><td colspan="8" class="empty-note">No developers yet.</td></tr>';
        return;
    }
    body.innerHTML = developers.map(function (developer) {
        return '<tr>'
             + '<td><strong>' + escapeHtml(developer.name) + '</strong></td>'
             + '<td>' + escapeHtml(developer.email) + '</td>'
             + '<td>' + escapeHtml(developer.role) + '</td>'
             + '<td class="nowrap">' + formatMoney(developer.hourlyCost) + ' / hour</td>'
             + '<td class="nowrap">' + developer.availabilityPercentage + '%</td>'
             + '<td>' + skillChips(developer.skills) + '</td>'
             + '<td>' + statusBadge(developer.status) + '</td>'
             + '<td><div class="row-actions no-print">'
             + '<button class="small secondary" onclick="editDeveloper(' + developer.id + ')">Edit</button>'
             + '<button class="small danger" onclick="deleteDeveloper(' + developer.id + ')">Delete</button>'
             + '</div></td></tr>';
    }).join('');
}

async function saveDeveloper(event) {
    event.preventDefault();
    clearMessage('message-area');

    const id = document.getElementById('developer-id').value;
    const payload = {
        name: document.getElementById('developer-name').value,
        email: document.getElementById('developer-email').value,
        role: document.getElementById('developer-role').value,
        hourlyCost: Number(document.getElementById('developer-cost').value),
        availabilityPercentage: Number(document.getElementById('developer-availability').value),
        status: document.getElementById('developer-status').value,
        skills: splitList(document.getElementById('developer-skills').value)
    };

    try {
        if (id) {
            await api.put('/api/developers/' + id, payload);
            showMessage('message-area', 'success', 'Developer updated.');
        } else {
            await api.post('/api/developers', payload);
            showMessage('message-area', 'success', 'Developer added.');
        }
        resetDeveloperForm();
        await loadAll();
    } catch (error) {
        showError('message-area', error);
    }
}

function editDeveloper(id) {
    const developer = developers.find(function (d) { return d.id === id; });
    if (!developer) { return; }
    document.getElementById('developer-id').value = developer.id;
    document.getElementById('developer-name').value = developer.name;
    document.getElementById('developer-email').value = developer.email;
    document.getElementById('developer-role').value = developer.role;
    document.getElementById('developer-cost').value = developer.hourlyCost;
    document.getElementById('developer-availability').value = developer.availabilityPercentage;
    document.getElementById('developer-status').value = developer.status;
    document.getElementById('developer-skills').value = developer.skills.join(', ');
    document.getElementById('developer-form-title').textContent = 'Edit developer: ' + developer.name;
    document.getElementById('developer-submit').textContent = 'Update developer';
    window.scrollTo({ top: 0, behavior: 'smooth' });
}

async function deleteDeveloper(id) {
    if (!confirm('Delete this developer? Their allocations and leave will be removed too.')) { return; }
    try {
        await api.del('/api/developers/' + id);
        showMessage('message-area', 'success', 'Developer deleted.');
        resetDeveloperForm();
        await loadAll();
    } catch (error) {
        showError('message-area', error);
    }
}

function resetDeveloperForm() {
    document.getElementById('developer-form').reset();
    document.getElementById('developer-id').value = '';
    document.getElementById('developer-form-title').textContent = 'Add a developer';
    document.getElementById('developer-submit').textContent = 'Save developer';
}

/* ---------------- leave ---------------- */

function drawLeaves() {
    const body = document.getElementById('leave-body');
    if (!leaves.length) {
        body.innerHTML = '<tr><td colspan="7" class="empty-note">No leave recorded.</td></tr>';
        return;
    }
    body.innerHTML = leaves.map(function (leave) {
        return '<tr>'
             + '<td>' + escapeHtml(leave.developerName) + '</td>'
             + '<td class="nowrap">' + formatDate(leave.startDate) + '</td>'
             + '<td class="nowrap">' + formatDate(leave.endDate) + '</td>'
             + '<td>' + daysBetween(leave.startDate, leave.endDate) + '</td>'
             + '<td>' + escapeHtml(leave.reason || '') + '</td>'
             + '<td>' + statusBadge(leave.status) + '</td>'
             + '<td><div class="row-actions no-print">'
             + '<button class="small danger" onclick="deleteLeave(' + leave.id + ')">Delete</button>'
             + '</div></td></tr>';
    }).join('');
}

async function saveLeave(event) {
    event.preventDefault();
    clearMessage('message-area');
    try {
        await api.post('/api/leaves', {
            developerId: Number(document.getElementById('leave-developer').value),
            startDate: document.getElementById('leave-start').value,
            endDate: document.getElementById('leave-end').value,
            reason: document.getElementById('leave-reason').value,
            status: document.getElementById('leave-status').value
        });
        showMessage('message-area', 'success',
            'Leave recorded. Any allocation on those dates now shows as a LEAVE CONFLICT.');
        document.getElementById('leave-form').reset();
        await loadAll();
    } catch (error) {
        showError('message-area', error);
    }
}

async function deleteLeave(id) {
    if (!confirm('Delete this leave entry?')) { return; }
    try {
        await api.del('/api/leaves/' + id);
        showMessage('message-area', 'success', 'Leave entry deleted.');
        await loadAll();
    } catch (error) {
        showError('message-area', error);
    }
}

function splitList(text) {
    if (!text) { return []; }
    return text.split(',').map(function (item) { return item.trim(); })
               .filter(function (item) { return item.length > 0; });
}
