/* =====================================================================
   api-explorer.js  -  a REST API explorer that runs in the browser.

   It does the same job as the Postman desktop application, but as a page
   of this website: pick an endpoint, fill in the path parameters, edit the
   JSON body, press Send, and read the real response.

   Everything here is plain JavaScript and the browser's own fetch().
   ===================================================================== */

/* ---------------------------------------------------------------------
   1. The endpoint catalogue.

   Each entry is:
     method   the HTTP method
     path     the URL. {id} marks a path parameter, which the page turns
              into a small input box.
     name     what to show in the list
     body     the sample JSON body (only for POST and PUT)
     expects  a short note shown above the Send button
   ------------------------------------------------------------------ */

const ENDPOINTS = [

  { group: 'Login', method: 'POST', path: '/api/login',
    name: 'Sign in',
    body: { username: 'manager', password: 'manager123' },
    expects: 'Returns the role, which decides the dashboard. Try username "arun" / password "arun123" for a developer, or a wrong password to see a 400.' },

  { group: 'Projects', method: 'GET', path: '/api/projects', name: 'All projects' },
  { group: 'Projects', method: 'GET', path: '/api/projects/{id}', name: 'One project', params: { id: 1 } },
  { group: 'Projects', method: 'POST', path: '/api/projects', name: 'Create a project',
    body: { name: 'Library Management System',
            description: 'Book issue and return system for the college library.',
            startDate: '2026-10-01', endDate: '2027-02-28',
            budget: 250000, status: 'PLANNED' },
    expects: 'Returns 201 Created. Creating a project also creates its budget row.' },
  { group: 'Projects', method: 'PUT', path: '/api/projects/{id}', name: 'Update a project', params: { id: 1 },
    body: { name: 'Hospital Management Portal',
            description: 'Patient records, appointments and billing portal.',
            startDate: '2026-09-01', endDate: '2026-12-31',
            budget: 500000, status: 'IN_PROGRESS' } },
  { group: 'Projects', method: 'DELETE', path: '/api/projects/{id}', name: 'Delete a project', params: { id: 4 },
    expects: 'Returns 204 No Content. Milestones, allocations and the budget are removed with it.' },
  { group: 'Projects', method: 'POST', path: '/api/projects', name: 'INVALID - end before start',
    body: { name: 'Bad dates', startDate: '2026-05-10', endDate: '2026-05-01', budget: 1000 },
    expects: 'Should return 400 with "End date cannot be before start date".' },
  { group: 'Projects', method: 'POST', path: '/api/projects', name: 'INVALID - negative budget',
    body: { name: 'Bad budget', startDate: '2026-05-01', endDate: '2026-05-10', budget: -5000 },
    expects: 'Should return 400 with "Budget cannot be negative".' },
  { group: 'Projects', method: 'GET', path: '/api/projects/{id}', name: 'MISSING - unknown project', params: { id: 999 },
    expects: 'Should return 404 with "Project not found with id 999".' },

  { group: 'Developers', method: 'GET', path: '/api/developers', name: 'All developers' },
  { group: 'Developers', method: 'GET', path: '/api/developers/{id}', name: 'One developer', params: { id: 1 } },
  { group: 'Developers', method: 'POST', path: '/api/developers', name: 'Create a developer',
    body: { name: 'Meena', email: 'meena@sist.edu', role: 'QA Engineer',
            hourlyCost: 400, availabilityPercentage: 100, status: 'ACTIVE',
            skills: ['Selenium', 'Java'] },
    expects: 'Skill names that do not exist yet are created automatically.' },
  { group: 'Developers', method: 'PUT', path: '/api/developers/{id}', name: 'Update a developer', params: { id: 1 },
    body: { name: 'Arun', email: 'arun@sist.edu', role: 'Senior Backend Developer',
            hourlyCost: 550, availabilityPercentage: 100, status: 'ACTIVE',
            skills: ['Java', 'Spring Boot', 'MySQL'] } },
  { group: 'Developers', method: 'POST', path: '/api/developers/{id}/skills', name: 'Add one skill', params: { id: 1 },
    body: { skillName: 'AWS', proficiencyLevel: 'EXPERT' } },
  { group: 'Developers', method: 'GET', path: '/api/developers/{id}/allocations', name: 'Their allocations', params: { id: 3 } },
  { group: 'Developers', method: 'GET', path: '/api/developers/{id}/dashboard', name: 'Their dashboard', params: { id: 3 } },
  { group: 'Developers', method: 'POST', path: '/api/developers', name: 'INVALID - negative cost + bad email',
    body: { name: '', email: 'not-an-email', role: 'Dev', hourlyCost: -100, availabilityPercentage: 100 },
    expects: 'Should return 400 listing three separate messages at once.' },
  { group: 'Developers', method: 'POST', path: '/api/developers', name: 'INVALID - duplicate email',
    body: { name: 'Copy of Arun', email: 'arun@sist.edu', role: 'Dev', hourlyCost: 100, availabilityPercentage: 100 },
    expects: 'Should return 400 - the email column is UNIQUE.' },

  { group: 'Skills', method: 'GET', path: '/api/skills', name: 'All skills' },
  { group: 'Skills', method: 'POST', path: '/api/skills', name: 'Create a skill',
    body: { name: 'Kotlin', category: 'Backend' } },
  { group: 'Skills', method: 'POST', path: '/api/skills', name: 'INVALID - duplicate skill',
    body: { name: 'Java', category: 'Backend' },
    expects: 'Should return 400 - skill names are unique.' },

  { group: 'Milestones', method: 'GET', path: '/api/milestones', name: 'All milestones' },
  { group: 'Milestones', method: 'GET', path: '/api/milestones?projectId={projectId}', name: 'By project', params: { projectId: 1 } },
  { group: 'Milestones', method: 'GET', path: '/api/milestones/{id}', name: 'One milestone', params: { id: 1 },
    expects: 'Look at estimatedDeliveryDate: start + ceil(effort / capacity) - 1 day.' },
  { group: 'Milestones', method: 'POST', path: '/api/milestones', name: 'Create a milestone',
    body: { projectId: 1, name: 'User Acceptance Testing',
            description: 'Hospital staff test the portal.',
            startDate: '2026-12-01', endDate: '2026-12-20',
            progressPercentage: 0, status: 'PLANNED',
            estimatedEffortDays: 12, requiredSkills: ['Java', 'MySQL'] } },
  { group: 'Milestones', method: 'POST', path: '/api/milestones/{id}/skills', name: 'Add a required skill', params: { id: 1 },
    body: { skillName: 'REST' } },
  { group: 'Milestones', method: 'POST', path: '/api/milestones', name: 'INVALID - outside project dates',
    body: { projectId: 1, name: 'Too early', startDate: '2026-01-01',
            endDate: '2026-01-31', estimatedEffortDays: 5 },
    expects: 'Should return 400 - a milestone must sit inside its project dates.' },

  { group: 'Allocations', method: 'GET', path: '/api/allocations', name: 'All allocations' },
  { group: 'Allocations', method: 'GET', path: '/api/allocations?developerId={developerId}', name: 'By developer', params: { developerId: 3 } },
  { group: 'Allocations', method: 'POST', path: '/api/allocations', name: 'Create - clean, no warnings',
    body: { developerId: 5, projectId: 1, milestoneId: 1,
            startDate: '2026-09-01', endDate: '2026-09-30', allocationPercentage: 20 },
    expects: 'Karthik has the required skills, so "warnings" comes back empty.' },
  { group: 'Allocations', method: 'POST', path: '/api/allocations', name: 'Create - RAISES A SKILL GAP',
    body: { developerId: 2, projectId: 1, milestoneId: 1,
            startDate: '2026-09-01', endDate: '2026-09-30', allocationPercentage: 30 },
    expects: 'Saved, but the response carries a "warnings" list. Priya has no backend skills and this pushes her over 100%. An assignment is never silent.' },
  { group: 'Allocations', method: 'PUT', path: '/api/allocations/{id}', name: 'Update an allocation', params: { id: 1 },
    body: { developerId: 1, projectId: 1, milestoneId: 1,
            startDate: '2026-09-01', endDate: '2026-09-30', allocationPercentage: 60 } },
  { group: 'Allocations', method: 'DELETE', path: '/api/allocations/{id}', name: 'Delete an allocation', params: { id: 8 } },
  { group: 'Allocations', method: 'POST', path: '/api/allocations', name: 'INVALID - 150%',
    body: { developerId: 1, projectId: 1, milestoneId: 1,
            startDate: '2026-09-01', endDate: '2026-09-10', allocationPercentage: 150 },
    expects: 'Should return 400 - the percentage must be between 1 and 100.' },
  { group: 'Allocations', method: 'POST', path: '/api/allocations', name: 'MISSING - unknown developer',
    body: { developerId: 999, projectId: 1,
            startDate: '2026-09-01', endDate: '2026-09-10', allocationPercentage: 50 },
    expects: 'Should return 404.' },

  { group: 'Leave', method: 'GET', path: '/api/leaves', name: 'All leave' },
  { group: 'Leave', method: 'POST', path: '/api/leaves', name: 'Record leave',
    body: { developerId: 3, startDate: '2026-09-20', endDate: '2026-09-25',
            reason: 'Wedding', status: 'APPROVED' },
    expects: 'Rahul is allocated on these dates, so /api/conflicts will now report an extra LEAVE CONFLICT. Run it afterwards and see.' },
  { group: 'Leave', method: 'POST', path: '/api/leaves', name: 'INVALID - end before start',
    body: { developerId: 1, startDate: '2026-09-20', endDate: '2026-09-10' },
    expects: 'Should return 400.' },

  { group: 'Conflicts and gaps', method: 'GET', path: '/api/conflicts', name: 'All conflicts',
    expects: 'CASE 1: Rahul reaches 110% between 15 Sep and 15 Oct. CASE 5: Priya is allocated during her leave. CASE 3: Divya’s September and November work does not overlap, so she is absent from this list.' },
  { group: 'Conflicts and gaps', method: 'GET', path: '/api/conflicts?developerId={developerId}', name: 'For one developer', params: { developerId: 1 },
    expects: 'CASE 2: Arun sits at 60%, inside his availability, so this comes back as an empty list.' },
  { group: 'Conflicts and gaps', method: 'GET', path: '/api/skill-gaps', name: 'Skill gaps',
    expects: 'CASE 4: "Cloud Deployment" needs Azure and nobody has it, so the suggestion reads "Skill Gap: No available developer has this skill."' },
  { group: 'Conflicts and gaps', method: 'GET', path: '/api/utilization', name: 'Utilization table',
    expects: 'Arun 60% Available, Priya 80% High Load, Rahul 110% Conflict.' },

  { group: 'Budget', method: 'GET', path: '/api/projects/{id}/budget', name: 'Budget summary', params: { id: 1 } },
  { group: 'Budget', method: 'GET', path: '/api/projects/{id}/budget/forecast', name: 'Forecast - CASE 6', params: { id: 1 },
    expects: 'CASE 6: 500,000 budget, 300,000 spent, 10,000 per day -> "approximately 20 days".' },
  { group: 'Budget', method: 'GET', path: '/api/projects/{id}/budget', name: 'Budget warning', params: { id: 2 },
    expects: '90% used, so warningLevel is CRITICAL.' },
  { group: 'Budget', method: 'GET', path: '/api/projects/{id}/budget/forecast', name: 'NO SPENDING DATA guard', params: { id: 3 },
    expects: 'Nothing has been spent, so no division happens. The message is "No spending data available".' },
  { group: 'Budget', method: 'GET', path: '/api/projects/{id}/budget/transactions', name: 'Spending entries', params: { id: 1 } },
  { group: 'Budget', method: 'POST', path: '/api/projects/{id}/budget/transactions', name: 'Record spending', params: { id: 1 },
    body: { transactionDate: '2026-09-18', amount: 25000, category: 'Salary', description: 'Extra sprint' },
    expects: 'This also updates current_spending, so re-run the forecast afterwards and watch the burn rate change.' },
  { group: 'Budget', method: 'POST', path: '/api/projects/{id}/budget/transactions', name: 'INVALID - zero amount', params: { id: 1 },
    body: { transactionDate: '2026-09-18', amount: 0, category: 'Salary' },
    expects: 'Should return 400.' },

  { group: 'Notifications', method: 'GET', path: '/api/notifications', name: 'All notifications' },
  { group: 'Notifications', method: 'POST', path: '/api/notifications/refresh', name: 'Re-scan the system',
    expects: 'Regenerates every alert. Safe to press twice - an identical message is never stored again.' },
  { group: 'Notifications', method: 'GET', path: '/api/notifications/unread-count', name: 'Unread count' },
  { group: 'Notifications', method: 'PUT', path: '/api/notifications/{id}/read', name: 'Mark as read', params: { id: 1 } },
  { group: 'Notifications', method: 'DELETE', path: '/api/notifications', name: 'Clear all' },

  { group: 'Simulations', method: 'POST', path: '/api/simulations/run', name: 'RUN - writes nothing',
    body: { milestoneId: 2, currentDeveloperId: 3, newDeveloperId: 5, newAllocationPercentage: 60 },
    expects: 'The reply has "id": null and "saved": false. Run the next request straight afterwards to prove the database was not touched.' },
  { group: 'Simulations', method: 'GET', path: '/api/simulations', name: 'Saved scenarios',
    expects: 'Use this right after a RUN to show the list is still empty - that is the proof the simulation saved nothing.' },
  { group: 'Simulations', method: 'POST', path: '/api/simulations', name: 'SAVE the scenario',
    body: { scenarioName: 'Replace Rahul with Divya on Frontend',
            milestoneId: 2, currentDeveloperId: 3, newDeveloperId: 4,
            newAllocationPercentage: 50 },
    expects: 'Only this call writes a row. It needs a scenarioName.' },
  { group: 'Simulations', method: 'GET', path: '/api/simulations/{id}', name: 'One saved scenario', params: { id: 1 } },
  { group: 'Simulations', method: 'GET', path: '/api/simulations/{id}/report', name: 'PDF report', params: { id: 1 },
    expects: 'This returns a PDF, not JSON. The page offers it as a download instead of printing it.' },
  { group: 'Simulations', method: 'DELETE', path: '/api/simulations/{id}', name: 'Delete a scenario', params: { id: 1 } },
  { group: 'Simulations', method: 'POST', path: '/api/simulations/run', name: 'INVALID - same developer twice',
    body: { milestoneId: 2, currentDeveloperId: 3, newDeveloperId: 3 },
    expects: 'Should return 400.' },

  { group: 'Dashboards', method: 'GET', path: '/api/dashboard/summary', name: 'Manager dashboard',
    expects: 'Every panel of the manager dashboard in one response.' },
  { group: 'Dashboards', method: 'GET', path: '/api/dashboard/developer/{developerId}', name: 'Developer dashboard', params: { developerId: 3 } },
  { group: 'Dashboards', method: 'GET', path: '/api/timeline', name: 'Full timeline' },
  { group: 'Dashboards', method: 'GET', path: '/api/projects/{id}/timeline', name: 'Timeline of one project', params: { id: 1 } }
];

/* ---------------------------------------------------------------------
   2. Page state
   ------------------------------------------------------------------ */

let selectedIndex = 0;

document.addEventListener('DOMContentLoaded', function () {
    renderLayout('api.html', 'REST API Explorer',
        'Try every endpoint straight from the browser. This page does the same job as Postman.');
    drawEndpointList();
    selectEndpoint(0);
    document.getElementById('send-button').addEventListener('click', sendRequest);
    document.getElementById('reset-body').addEventListener('click', function () {
        fillRequestPanel(ENDPOINTS[selectedIndex]);
    });
    document.getElementById('endpoint-search').addEventListener('input', drawEndpointList);

    // the curl line must follow every edit, otherwise a copied command is stale
    document.getElementById('body-editor').addEventListener('input', updateCurl);
    document.getElementById('request-line').addEventListener('input', updateCurl);
});

/* ---------------------------------------------------------------------
   3. The endpoint list on the left
   ------------------------------------------------------------------ */

function drawEndpointList() {
    const term = document.getElementById('endpoint-search').value.trim().toLowerCase();
    const container = document.getElementById('endpoint-list');

    let html = '';
    let lastGroup = null;
    let shown = 0;

    ENDPOINTS.forEach(function (endpoint, index) {
        const haystack = (endpoint.group + ' ' + endpoint.name + ' ' +
                          endpoint.method + ' ' + endpoint.path).toLowerCase();
        if (term && haystack.indexOf(term) < 0) { return; }

        if (endpoint.group !== lastGroup) {
            html += '<div class="endpoint-group-title">' + escapeHtml(endpoint.group) + '</div>';
            lastGroup = endpoint.group;
        }
        html += '<button class="endpoint-item' + (index === selectedIndex ? ' selected' : '') + '"'
              + ' onclick="selectEndpoint(' + index + ')">'
              + '<span class="method ' + endpoint.method + '">' + endpoint.method + '</span>'
              + '<span class="endpoint-label">' + escapeHtml(endpoint.name) + '</span>'
              + '</button>';
        shown++;
    });

    container.innerHTML = shown ? html
        : '<p class="empty-note">No endpoint matches that search.</p>';
    document.getElementById('endpoint-count').textContent =
        shown + ' of ' + ENDPOINTS.length + ' requests';
}

function selectEndpoint(index) {
    selectedIndex = index;
    drawEndpointList();
    fillRequestPanel(ENDPOINTS[index]);
    document.getElementById('response-panel').innerHTML =
        '<p class="empty-note">Press Send to call this endpoint.</p>';
}

/* ---------------------------------------------------------------------
   4. The request panel on the right
   ------------------------------------------------------------------ */

/** Splits "/api/projects/{id}/budget" into text and {placeholder} pieces. */
function buildRequestLine(endpoint) {
    const pieces = endpoint.path.split(/(\{[a-zA-Z]+\})/);
    return pieces.map(function (piece) {
        const match = piece.match(/^\{([a-zA-Z]+)\}$/);
        if (!match) { return escapeHtml(piece); }
        const name = match[1];
        const value = (endpoint.params && endpoint.params[name] !== undefined)
            ? endpoint.params[name] : 1;
        return '<input class="param-input" id="param-' + name + '" data-param="' + name
             + '" value="' + escapeHtml(value) + '" title="' + escapeHtml(name) + '">';
    }).join('');
}

function fillRequestPanel(endpoint) {
    document.getElementById('request-line').innerHTML =
        '<span class="method ' + endpoint.method + '">' + endpoint.method + '</span>'
        + '<span class="url-parts">' + escapeHtml(API_BASE || window.location.origin)
        + buildRequestLine(endpoint) + '</span>';

    const noteBox = document.getElementById('expects-note');
    if (endpoint.expects) {
        noteBox.innerHTML = '<div class="expects-note">' + escapeHtml(endpoint.expects) + '</div>';
    } else {
        noteBox.innerHTML = '';
    }

    const bodyBox = document.getElementById('body-section');
    if (endpoint.body) {
        bodyBox.style.display = '';
        document.getElementById('body-editor').value = JSON.stringify(endpoint.body, null, 2);
    } else {
        bodyBox.style.display = 'none';
        document.getElementById('body-editor').value = '';
    }
    updateCurl();
}

/** Replaces every {placeholder} with whatever is typed in its input box. */
function buildPath(endpoint) {
    let path = endpoint.path;
    document.querySelectorAll('#request-line .param-input').forEach(function (input) {
        path = path.replace('{' + input.dataset.param + '}', encodeURIComponent(input.value.trim()));
    });
    return path;
}

/** The address fetch() is given. */
function buildUrl(endpoint) {
    return (API_BASE || '') + buildPath(endpoint);
}

/**
 * Shows the same request as a curl command so it can be pasted into a
 * terminal. It is rebuilt whenever the body or a path parameter changes,
 * so what is on screen always matches what Send would do.
 */
function updateCurl() {
    const endpoint = ENDPOINTS[selectedIndex];
    const fullUrl = (API_BASE || window.location.origin) + buildPath(endpoint);

    let curl = 'curl -X ' + endpoint.method + ' "' + fullUrl + '"';
    if (endpoint.body) {
        const oneLine = document.getElementById('body-editor').value.replace(/\s*\n\s*/g, ' ');
        curl += " \\\n  -H 'Content-Type: application/json' \\\n  -d '" + oneLine + "'";
    }
    document.getElementById('curl-line').textContent = curl;
}

/* ---------------------------------------------------------------------
   5. Sending the request
   ------------------------------------------------------------------ */

async function sendRequest() {
    const endpoint = ENDPOINTS[selectedIndex];
    const panel = document.getElementById('response-panel');
    const url = buildUrl(endpoint);

    const options = { method: endpoint.method, headers: {} };

    if (endpoint.body) {
        const raw = document.getElementById('body-editor').value;
        try {
            JSON.parse(raw);                       // fail early on a typo
        } catch (parseError) {
            panel.innerHTML = '<div class="message error">The request body is not valid JSON: '
                            + escapeHtml(parseError.message) + '</div>';
            return;
        }
        options.headers['Content-Type'] = 'application/json';
        options.body = raw;
    }

    panel.innerHTML = '<p class="empty-note">Sending...</p>';
    const startedAt = performance.now();

    let response;
    try {
        response = await fetch(url, options);
    } catch (networkError) {
        panel.innerHTML = '<div class="message error"><strong>Could not reach the server.</strong>'
            + ' Check that the Spring Boot application is running on port 8080.</div>';
        return;
    }

    const elapsed = Math.round(performance.now() - startedAt);
    const contentType = response.headers.get('content-type') || '';
    showResponse(response, contentType, elapsed, url);
}

/* Some browsers hand back an empty statusText, which would leave the pill
   reading "200" with a dangling space. This fills in the standard wording. */
const STATUS_TEXT = {
    200: 'OK', 201: 'Created', 204: 'No Content',
    400: 'Bad Request', 401: 'Unauthorized', 403: 'Forbidden',
    404: 'Not Found', 405: 'Method Not Allowed', 409: 'Conflict',
    415: 'Unsupported Media Type', 500: 'Internal Server Error'
};

async function showResponse(response, contentType, elapsed, url) {
    const panel = document.getElementById('response-panel');
    const statusText = response.statusText || STATUS_TEXT[response.status] || '';

    let statusClass = 'serr';
    if (response.status >= 200 && response.status < 300) { statusClass = 's2xx'; }
    else if (response.status >= 400 && response.status < 500) { statusClass = 's4xx'; }
    else if (response.status >= 500) { statusClass = 's5xx'; }

    let meta = '<div class="response-meta">'
        + '<span class="status-pill ' + statusClass + '">' + response.status
        + (statusText ? ' ' + escapeHtml(statusText) : '') + '</span>'
        + '<span>' + elapsed + ' ms</span>'
        + '<span>' + escapeHtml(contentType.split(';')[0] || 'no content type') + '</span>'
        + '</div>';

    // a PDF is binary, so offer it as a download instead of printing bytes
    if (contentType.indexOf('application/pdf') >= 0) {
        const blob = await response.blob();
        const objectUrl = URL.createObjectURL(blob);
        panel.innerHTML = meta
            + '<div class="message success">The server returned a PDF of '
            + blob.size + ' bytes, so it is offered as a download rather than shown as text.</div>'
            + '<a class="button" href="' + objectUrl + '" download="simulation-report.pdf">Download the PDF</a>';
        return;
    }

    if (response.status === 204) {
        panel.innerHTML = meta + '<div class="message success">204 No Content - '
            + 'the delete succeeded and there is no response body.</div>';
        return;
    }

    const text = await response.text();
    let pretty = text;
    try {
        pretty = JSON.stringify(JSON.parse(text), null, 2);
    } catch (parseError) {
        /* not JSON - show it exactly as it came back */
    }

    // point out the two things that are easy to miss
    let hint = '';
    if (pretty.indexOf('"warnings"') >= 0 && pretty.indexOf('"warnings": []') < 0) {
        hint = '<div class="message warning">This allocation was saved, but the API returned '
             + 'warnings. Look at the "warnings" list below.</div>';
    } else if (pretty.indexOf('"saved": false') >= 0) {
        hint = '<div class="message info">"saved": false and "id": null - this simulation was '
             + 'calculated in memory and nothing was written to the database.</div>';
    }

    panel.innerHTML = meta + hint + '<pre class="response-body">' + escapeHtml(pretty) + '</pre>';
}
