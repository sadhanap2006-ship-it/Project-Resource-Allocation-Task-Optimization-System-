/* =====================================================================
   simulator.js  -  the what-if resource swap simulator.

   Two clearly separated buttons:
     "Run simulation"      -> POST /api/simulations/run   (nothing is saved)
     "Save this scenario"  -> POST /api/simulations       (writes one row)

   The PDF report can be downloaded either way.
   ===================================================================== */

let milestones = [];
let developers = [];
let allocations = [];
let lastResult = null;

document.addEventListener('DOMContentLoaded', function () {
    renderLayout('simulator.html', 'What-If Resource Swap Simulator',
        'Test a replacement before you commit to it. Running a simulation changes nothing.');

    document.getElementById('simulation-form').addEventListener('submit', runSimulation);
    document.getElementById('sim-reset').addEventListener('click', resetForm);
    document.getElementById('sim-milestone').addEventListener('change', onMilestoneChanged);

    loadAll();
});

async function loadAll() {
    try {
        milestones = await api.get('/api/milestones');
        developers = await api.get('/api/developers');
        allocations = await api.get('/api/allocations');

        fillSelect('sim-milestone', milestones, 'id', function (m) {
            return m.projectName + ' / ' + m.name;
        }, 'Select a milestone');
        fillSelect('sim-new', developers, 'id', function (d) {
            return d.name + ' (' + formatMoney(d.hourlyCost) + '/hr)';
        }, 'Select the replacement');

        onMilestoneChanged();
        await drawSavedScenarios();
    } catch (error) {
        showError('message-area', error);
    }
}

/** Only the developers actually allocated to the milestone can be replaced. */
function onMilestoneChanged() {
    const milestoneId = Number(document.getElementById('sim-milestone').value);
    const onMilestone = allocations.filter(function (a) { return a.milestoneId === milestoneId; });

    const select = document.getElementById('sim-current');
    if (!milestoneId) {
        select.innerHTML = '<option value="">Select a milestone first</option>';
        return;
    }
    if (!onMilestone.length) {
        select.innerHTML = '<option value="">Nobody is allocated to this milestone</option>';
        return;
    }
    select.innerHTML = '<option value="">Select the current developer</option>'
        + onMilestone.map(function (allocation) {
            return '<option value="' + allocation.developerId + '">'
                 + escapeHtml(allocation.developerName) + ' (' + allocation.allocationPercentage + '%)</option>';
        }).join('');
}

/* ---------------- run ---------------- */

function buildRequest() {
    const percentage = document.getElementById('sim-percentage').value;
    return {
        milestoneId: Number(document.getElementById('sim-milestone').value),
        currentDeveloperId: Number(document.getElementById('sim-current').value),
        newDeveloperId: Number(document.getElementById('sim-new').value),
        newAllocationPercentage: percentage ? Number(percentage) : null
    };
}

async function runSimulation(event) {
    event.preventDefault();
    clearMessage('message-area');
    try {
        lastResult = await api.post('/api/simulations/run', buildRequest());
        drawResult(lastResult);
    } catch (error) {
        lastResult = null;
        document.getElementById('result-area').innerHTML = '';
        showError('message-area', error);
    }
}

function resetForm() {
    document.getElementById('simulation-form').reset();
    document.getElementById('result-area').innerHTML = '';
    lastResult = null;
    onMilestoneChanged();
}

/* ---------------- result ---------------- */

function drawResult(result) {
    const better = 'delta-better';
    const worse = 'delta-worse';
    const same = 'delta-same';

    const costClass = Number(result.budgetImpact) > 0 ? worse
                    : (Number(result.budgetImpact) < 0 ? better : same);
    const skillClass = result.simulatedSkillMatch > result.currentSkillMatch ? better
                     : (result.simulatedSkillMatch < result.currentSkillMatch ? worse : same);
    const conflictClass = result.simulatedConflicts > result.currentConflicts ? worse
                        : (result.simulatedConflicts < result.currentConflicts ? better : same);
    const deliveryClass = (result.deliveryImpactDays > 0) ? worse
                        : ((result.deliveryImpactDays < 0) ? better : same);

    let html = '<div class="card">'
        + '<h3>Simulation result (nothing has been saved)</h3>'
        + '<table class="compare-table">'
        + '<thead><tr><th>Item</th><th>Current</th><th>Simulated</th></tr></thead><tbody>'
        + compareRow('Project', escapeHtml(result.projectName), escapeHtml(result.projectName))
        + compareRow('Milestone', escapeHtml(result.milestoneName), escapeHtml(result.milestoneName))
        + compareRow('Resource', escapeHtml(result.currentDeveloperName), escapeHtml(result.newDeveloperName))
        + compareRow('Hourly cost', formatMoney(result.currentHourlyCost), formatMoney(result.newHourlyCost))
        + compareRow('Allocation', result.currentAllocationPercentage + '%', result.simulatedAllocationPercentage + '%')
        + compareRow('Cost for this milestone', formatMoney(result.currentCost),
              '<span class="' + costClass + '">' + formatMoney(result.simulatedCost) + '</span>')
        + compareRow('Skill match', formatPercent(result.currentSkillMatch),
              '<span class="' + skillClass + '">' + formatPercent(result.simulatedSkillMatch) + '</span>')
        + compareRow('Missing skills',
              result.currentMissingSkills.length ? skillChips(result.currentMissingSkills, result.currentMissingSkills) : 'None',
              result.simulatedMissingSkills.length ? skillChips(result.simulatedMissingSkills, result.simulatedMissingSkills) : 'None')
        + compareRow('Conflicts', result.currentConflicts,
              '<span class="' + conflictClass + '">' + result.simulatedConflicts + '</span>')
        + compareRow('Estimated delivery date',
              result.currentDeliveryDate ? formatDate(result.currentDeliveryDate) : 'Not estimated',
              '<span class="' + deliveryClass + '">'
              + (result.simulatedDeliveryDate ? formatDate(result.simulatedDeliveryDate) : 'Not estimated')
              + '</span>')
        + compareRow('Budget impact', '-',
              '<span class="' + costClass + '">' + formatMoney(result.budgetImpact) + '</span>')
        + '</tbody></table>';

    html += '<div class="message info"><strong>Delivery impact:</strong> '
          + escapeHtml(result.deliveryImpactMessage || '-') + '</div>';

    const recommendationKind = result.recommendation.startsWith('NOT RECOMMENDED') ? 'error'
                             : (result.recommendation.startsWith('RECOMMENDED') ? 'success' : 'warning');
    html += '<div class="message ' + recommendationKind + '"><strong>'
          + escapeHtml(result.recommendation) + '</strong></div>';

    if (result.simulatedConflictDetails.length) {
        html += '<div class="message warning"><strong>Conflicts the swap would create:</strong><ul>'
              + result.simulatedConflictDetails.map(function (detail) {
                    return '<li>' + escapeHtml(detail) + '</li>';
                }).join('') + '</ul></div>';
    }

    html += '<div class="form-actions no-print" style="margin-top:14px">'
          + '<input type="text" id="scenario-name" placeholder="Name this scenario" style="flex:1;min-width:200px">'
          + '<button type="button" onclick="saveScenario()">Save this scenario</button>'
          + '<button type="button" class="secondary" onclick="downloadReport()">Download PDF report</button>'
          + '<button type="button" class="secondary" onclick="window.print()">Print / Save as PDF</button>'
          + '</div>'
          + '<p class="inline-note">The scenario is stored only when you press "Save this scenario".</p>'
          + '</div>';

    document.getElementById('result-area').innerHTML = html;
    document.getElementById('result-area').scrollIntoView({ behavior: 'smooth', block: 'start' });
}

function compareRow(label, current, simulated) {
    return '<tr><td>' + escapeHtml(label) + '</td><td>' + current + '</td><td>' + simulated + '</td></tr>';
}

/* ---------------- save + report ---------------- */

async function saveScenario() {
    clearMessage('message-area');
    const name = document.getElementById('scenario-name').value.trim();
    if (!name) {
        showMessage('message-area', 'warning', 'Give the scenario a name before saving it.');
        return;
    }
    try {
        const request = buildRequest();
        request.scenarioName = name;
        const saved = await api.post('/api/simulations', request);
        lastResult = saved;
        showMessage('message-area', 'success', 'Scenario "' + name + '" saved.');
        await drawSavedScenarios();
    } catch (error) {
        showError('message-area', error);
    }
}

/** Downloads the PDF. A saved scenario uses its id; an unsaved one is
    generated on the fly without storing anything. */
async function downloadReport() {
    clearMessage('message-area');
    try {
        let response;
        if (lastResult && lastResult.id) {
            response = await fetch(API_BASE + '/api/simulations/' + lastResult.id + '/report');
        } else {
            response = await fetch(API_BASE + '/api/simulations/report', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(buildRequest())
            });
        }
        if (!response.ok) {
            throw new Error('The server could not generate the PDF (HTTP ' + response.status + ').');
        }
        const blob = await response.blob();
        const url = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = 'simulation-report.pdf';
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        URL.revokeObjectURL(url);
    } catch (error) {
        showError('message-area', error);
    }
}

/* ---------------- saved scenarios ---------------- */

async function drawSavedScenarios() {
    const scenarios = await api.get('/api/simulations');
    const body = document.getElementById('scenario-body');
    if (!scenarios.length) {
        body.innerHTML = '<tr><td colspan="7" class="empty-note">No scenario has been saved yet.</td></tr>';
        return;
    }
    body.innerHTML = scenarios.map(function (scenario) {
        const impact = Number(scenario.budgetImpact);
        const impactClass = impact > 0 ? 'delta-worse' : (impact < 0 ? 'delta-better' : 'delta-same');
        return '<tr>'
             + '<td><strong>' + escapeHtml(scenario.scenarioName) + '</strong></td>'
             + '<td>' + escapeHtml(scenario.projectName) + '</td>'
             + '<td>' + escapeHtml(scenario.milestoneName || '-') + '</td>'
             + '<td>' + escapeHtml(scenario.currentDeveloperName) + ' &rarr; '
             + escapeHtml(scenario.newDeveloperName) + '</td>'
             + '<td class="nowrap"><span class="' + impactClass + '">' + formatMoney(scenario.budgetImpact) + '</span></td>'
             + '<td class="nowrap">' + formatDateTime(scenario.createdAt) + '</td>'
             + '<td><div class="row-actions no-print">'
             + '<a class="button small secondary" href="' + API_BASE + '/api/simulations/'
             + scenario.id + '/report">PDF</a>'
             + '<button class="small danger" onclick="deleteScenario(' + scenario.id + ')">Delete</button>'
             + '</div></td></tr>';
    }).join('');
}

async function deleteScenario(id) {
    if (!confirm('Delete this saved scenario?')) { return; }
    try {
        await api.del('/api/simulations/' + id);
        showMessage('message-area', 'success', 'Scenario deleted.');
        await drawSavedScenarios();
    } catch (error) {
        showError('message-area', error);
    }
}
