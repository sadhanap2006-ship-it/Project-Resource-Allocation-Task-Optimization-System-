/* =====================================================================
   budget.js  -  budget table, burn rate, forecast and spending entries.
   ===================================================================== */

let projects = [];

document.addEventListener('DOMContentLoaded', function () {
    renderLayout('budget.html', 'Budget and Forecast',
        'Spending, remaining budget, burn rate and a simple burn-rate forecast.');

    document.getElementById('forecast-project').addEventListener('change', loadForecast);
    document.getElementById('transaction-form').addEventListener('submit', addTransaction);

    loadAll();
});

async function loadAll() {
    clearMessage('message-area');
    try {
        projects = await api.get('/api/projects');
        fillSelect('forecast-project', projects, 'id', function (p) { return p.name; }, '');
        await drawBudgetTable();
        await loadForecast();
    } catch (error) {
        showError('message-area', error);
    }
}

async function drawBudgetTable() {
    const body = document.getElementById('budget-body');
    if (!projects.length) {
        body.innerHTML = '<tr><td colspan="7" class="empty-note">No projects yet.</td></tr>';
        return;
    }

    const rows = [];
    for (const project of projects) {
        const budget = await api.get('/api/projects/' + project.id + '/budget');
        const utilization = budget.utilizationPercentage;

        let utilizationCell;
        if (utilization === null) {
            utilizationCell = '<span class="inline-note">' + escapeHtml(budget.message) + '</span>';
        } else {
            const kind = utilization >= 90 ? 'danger' : (utilization >= 75 ? 'warn' : 'info');
            utilizationCell = '<div class="bar-track"><div class="bar-fill ' + kind
                            + '" style="width:' + Math.min(100, utilization) + '%"></div></div>'
                            + '<div class="bar-label">' + formatPercent(utilization) + '</div>';
        }

        const rowClass = budget.warningLevel === 'CRITICAL' ? ' class="row-conflict"'
                       : (budget.warningLevel === 'WARNING' ? ' class="row-warning"' : '');

        rows.push('<tr' + rowClass + '>'
            + '<td><strong>' + escapeHtml(budget.projectName) + '</strong></td>'
            + '<td class="nowrap">' + formatMoney(budget.totalBudget) + '</td>'
            + '<td class="nowrap">' + formatMoney(budget.currentSpending) + '</td>'
            + '<td class="nowrap">' + formatMoney(budget.remainingBudget) + '</td>'
            + '<td>' + utilizationCell + '</td>'
            + '<td>' + statusBadge(budget.warningLevel) + '</td>'
            + '<td><button class="small secondary no-print" onclick="showProject(' + project.id + ')">Forecast</button></td>'
            + '</tr>');
    }
    body.innerHTML = rows.join('');
}

function showProject(projectId) {
    document.getElementById('forecast-project').value = projectId;
    loadForecast();
    document.getElementById('forecast-detail').scrollIntoView({ behavior: 'smooth' });
}

async function loadForecast() {
    const projectId = document.getElementById('forecast-project').value;
    const container = document.getElementById('forecast-detail');
    if (!projectId) {
        container.innerHTML = '<p class="empty-note">Select a project.</p>';
        document.getElementById('transaction-body').innerHTML = '';
        return;
    }

    try {
        const forecast = await api.get('/api/projects/' + projectId + '/budget/forecast');

        let html = '<table class="compare-table"><tbody>'
            + row('Total budget', formatMoney(forecast.totalBudget))
            + row('Current spending', formatMoney(forecast.currentSpending))
            + row('Remaining budget', formatMoney(forecast.remainingBudget));

        if (forecast.hasSpendingData) {
            html += row('Days of spending so far', forecast.daysElapsed + ' days')
                 +  row('Current burn rate', formatMoney(forecast.burnRatePerDay) + ' per day')
                 +  row('Estimated days remaining', forecast.estimatedDaysRemaining + ' days')
                 +  row('Estimated exhaustion date', formatDate(forecast.estimatedExhaustionDate));
        } else {
            html += row('Burn rate', '<span class="inline-note">No spending data available</span>')
                 +  row('Forecast', '<span class="inline-note">No spending data available</span>');
        }
        html += '</tbody></table>';

        const messageKind = forecast.hasSpendingData ? 'info' : 'warning';
        html += '<div class="message ' + messageKind + '"><strong>' + escapeHtml(forecast.message) + '</strong></div>';
        html += '<p class="inline-note">' + escapeHtml(forecast.methodNote) + '</p>';

        container.innerHTML = html;
        await drawTransactions(projectId);
    } catch (error) {
        showError('message-area', error);
    }
}

function row(label, value) {
    return '<tr><td>' + escapeHtml(label) + '</td><td>' + value + '</td></tr>';
}

async function drawTransactions(projectId) {
    const transactions = await api.get('/api/projects/' + projectId + '/budget/transactions');
    const body = document.getElementById('transaction-body');
    if (!transactions.length) {
        body.innerHTML = '<tr><td colspan="4" class="empty-note">No spending recorded for this project.</td></tr>';
        return;
    }
    body.innerHTML = transactions.map(function (transaction) {
        return '<tr>'
             + '<td class="nowrap">' + formatDate(transaction.transactionDate) + '</td>'
             + '<td class="nowrap">' + formatMoney(transaction.amount) + '</td>'
             + '<td>' + escapeHtml(transaction.category || '') + '</td>'
             + '<td>' + escapeHtml(transaction.description || '') + '</td></tr>';
    }).join('');
}

async function addTransaction(event) {
    event.preventDefault();
    clearMessage('message-area');

    const projectId = document.getElementById('forecast-project').value;
    if (!projectId) {
        showMessage('message-area', 'warning', 'Select a project first.');
        return;
    }

    try {
        await api.post('/api/projects/' + projectId + '/budget/transactions', {
            transactionDate: document.getElementById('transaction-date').value,
            amount: Number(document.getElementById('transaction-amount').value),
            category: document.getElementById('transaction-category').value,
            description: document.getElementById('transaction-description').value
        });
        showMessage('message-area', 'success', 'Spending entry added and the budget totals updated.');
        document.getElementById('transaction-form').reset();
        await drawBudgetTable();
        await loadForecast();
    } catch (error) {
        showError('message-area', error);
    }
}
