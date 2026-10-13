/**
 * Project Resource Allocation System — Frontend Application Logic
 * SIST CSE AI Capstone Project — Pure Vanilla JavaScript ES6+
 */

// Global State
let currentRole = "PM";
let currentDevId = 1;
let currentProjectIdForDetail = 1;
let budgetChartInstance = null;
let dedicatedBudgetChartInstance = null;
let allConflictsCache = [];
let allProjectsCache = [];
let allMilestonesCache = [];
let allDevelopersCache = [];
let allNotificationsCache = [];
let lastSimulationResult = null;
let currentConflictSeverityFilter = "ALL";
let currentNotifPageFilter = "ALL";

// ==========================================================================
// Utilities & Formatting Helpers
// ==========================================================================
function escapeHtml(str) {
    if (str === null || str === undefined) return "";
    return String(str)
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#039;");
}

function parseSkillsList(skills) {
    if (!skills) return [];
    if (Array.isArray(skills)) {
        const list = [];
        skills.forEach(item => {
            if (typeof item === 'string') {
                if (item.includes(',')) {
                    item.split(',').forEach(s => {
                        const trimmed = s.trim();
                        if (trimmed) list.push(trimmed);
                    });
                } else {
                    const trimmed = item.trim();
                    if (trimmed) list.push(trimmed);
                }
            } else if (item && typeof item === 'object' && item.name) {
                list.push(String(item.name).trim());
            }
        });
        return list;
    }
    if (typeof skills === 'string') {
        const trimmed = skills.trim();
        if (!trimmed) return [];
        if (trimmed.startsWith('[') && trimmed.endsWith(']')) {
            try {
                const parsed = JSON.parse(trimmed);
                return parseSkillsList(parsed);
            } catch (e) {}
        }
        if (trimmed.includes(',')) {
            return trimmed.split(',').map(s => s.trim()).filter(Boolean);
        }
        return [trimmed];
    }
    return [];
}

function switchToDevPortal(devId) {
    loginAsDeveloper(devId);
}

// ==========================================================================
// Initialization
// ==========================================================================
document.addEventListener("DOMContentLoaded", () => {
    initNavigation();
    initModals();
    initNotificationDrawer();
    initSimulatorControls();
    initFiltersAndSearch();
    initAssignCapacityCalculator();

    // Load initial dashboard
    loadPmDashboard();
});

// ==========================================================================
// Navigation & Portal Switching
// ==========================================================================
function initNavigation() {
    const tabButtons = document.querySelectorAll(".tab-btn");
    tabButtons.forEach(btn => {
        btn.addEventListener("click", () => {
            tabButtons.forEach(b => b.classList.remove("active"));
            btn.classList.add("active");
            const targetView = btn.getAttribute("data-view");
            switchTab(targetView);
        });
    });
}

function switchLoginMode(mode) {
    const btnMgr = document.getElementById("tabBtnManagerLogin");
    const btnDev = document.getElementById("tabBtnDeveloperLogin");
    const formMgr = document.getElementById("formLoginManager");
    const formDev = document.getElementById("formLoginDeveloper");

    if (!btnMgr || !btnDev || !formMgr || !formDev) return;

    if (mode === "MANAGER") {
        btnMgr.style.background = "white";
        btnMgr.style.color = "var(--primary)";
        btnMgr.style.fontWeight = "700";
        btnMgr.style.boxShadow = "var(--shadow-xs)";

        btnDev.style.background = "transparent";
        btnDev.style.color = "var(--text-muted)";
        btnDev.style.fontWeight = "600";
        btnDev.style.boxShadow = "none";

        formMgr.style.display = "block";
        formDev.style.display = "none";
    } else {
        btnDev.style.background = "white";
        btnDev.style.color = "var(--primary)";
        btnDev.style.fontWeight = "700";
        btnDev.style.boxShadow = "var(--shadow-xs)";

        btnMgr.style.background = "transparent";
        btnMgr.style.color = "var(--text-muted)";
        btnMgr.style.fontWeight = "600";
        btnMgr.style.boxShadow = "none";

        formDev.style.display = "block";
        formMgr.style.display = "none";
    }
}

function selectDeveloperDemo(devId) {
    const input = document.getElementById("loginDeveloperId");
    if (input) {
        input.value = devId;
    }
    loginAsDeveloper(devId);
}

function selectEmployeeDemo(devId) {
    selectDeveloperDemo(devId);
}

function switchTab(viewId) {
    document.querySelectorAll(".view-section").forEach(sec => sec.classList.remove("active"));
    const activeSec = document.getElementById(viewId);
    if (activeSec) {
        activeSec.classList.add("active");
    }

    const targetBtn = document.querySelector(`.tab-btn[data-view="${viewId}"]`);
    if (targetBtn) {
        document.querySelectorAll(".tab-btn").forEach(b => b.classList.remove("active"));
        targetBtn.classList.add("active");
    }

    if (viewId === "viewDashboard") loadPmDashboard();
    if (viewId === "viewProjects") loadProjectsManager();
    if (viewId === "viewProjectDetails") loadProjectDetailsView(currentProjectIdForDetail);
    if (viewId === "viewResources") loadResourceDirectory();
    if (viewId === "viewAllocationMatrix") loadAllocationMatrix();
    if (viewId === "viewTimeline") loadTimeline();
    if (viewId === "viewHeatmap") loadHeatmap();
    if (viewId === "viewLeaves") loadLeavesManager();
    if (viewId === "viewConflicts") loadConflictCenter();
    if (viewId === "viewBudgetForecast") loadBudgetForecastPage();
    if (viewId === "viewSimulator") loadSimulatorSetup();
    if (viewId === "viewNotificationsPage") loadNotificationsPage();
    if (viewId === "viewSkillVerificationQueue") loadManagerVerificationQueue();
    if (viewId === "viewTeamDirectory") loadTeamDirectory();
    if (viewId === "viewLeaveApprovals") loadManagerLeaveApprovals();
    if (viewId === "viewDeveloperPortal") loadDeveloperDashboard(currentDevId);
}

// ==========================================================================
// VIEW 1: PM Overview Dashboard
// ==========================================================================
async function loadPmDashboard() {
    try {
        const res = await fetch("/api/dashboard/manager");
        const json = await res.json();
        if (json.status !== "SUCCESS") return;

        const data = json.data;
        allConflictsCache = data.recentConflicts || [];
        allProjectsCache = data.projectSummaries || [];

        // KPI Summary Cards
        document.getElementById("kpiActiveProjects").innerText = data.activeProjects;
        document.getElementById("kpiTotalProjects").innerText = `Total: ${data.totalProjects}`;
        document.getElementById("kpiCriticalConflicts").innerText = data.criticalConflictCount;
        document.getElementById("kpiSkillGaps").innerText = data.skillGapCount;
        document.getElementById("kpiWarningsCount").innerText = `Warnings: ${data.warningConflictCount}`;
        document.getElementById("kpiAvgUtil").innerText = `${data.averageDeveloperUtilization}%`;
        document.getElementById("kpiBudgetSpend").innerText = `₹${formatNumber(data.totalSpend)}`;
        document.getElementById("kpiBudgetRemaining").innerText = `Remaining: ₹${formatNumber(data.totalRemainingBudget)}`;
        document.getElementById("kpiAtRiskProjects").innerText = data.atRiskProjects;

        // Populate Budget Dropdown
        const budgetSelect = document.getElementById("budgetProjectSelect");
        budgetSelect.innerHTML = "";
        data.projectSummaries.forEach(p => {
            const opt = document.createElement("option");
            opt.value = p.id;
            opt.text = p.name;
            budgetSelect.appendChild(opt);
        });

        budgetSelect.onchange = () => loadProjectBudgetForecast(budgetSelect.value);
        if (data.projectSummaries.length > 0) {
            loadProjectBudgetForecast(data.projectSummaries[0].id);
        }

        renderDashboardRecentConflicts(allConflictsCache);
        renderDashboardProjectsTable(data.projectSummaries);
        fetchNotifications();
    } catch (err) {
        console.error("Error loading PM dashboard:", err);
    }
}

async function loadProjectBudgetForecast(projectId) {
    try {
        const res = await fetch(`/api/projects/${projectId}/budget`);
        const json = await res.json();
        if (json.status !== "SUCCESS") return;

        const b = json.data;
        document.getElementById("budgetCurrentSpend").innerText = `₹${formatNumber(b.currentSpend)}`;
        document.getElementById("budgetRemaining").innerText = `₹${formatNumber(b.remainingBudget)}`;
        document.getElementById("budgetBurnRate").innerText = `₹${formatNumber(b.dailyBurnRate)}/day`;
        document.getElementById("budgetExhaustionDate").innerText = b.forecastExhaustionDate;

        renderBudgetBurnDownChart(b);
    } catch (err) {
        console.error("Error loading budget forecast:", err);
    }
}

function renderBudgetBurnDownChart(b) {
    const canvas = document.getElementById("budgetChart");
    if (!canvas) return;
    const ctx = canvas.getContext("2d");

    if (budgetChartInstance) {
        budgetChartInstance.destroy();
    }

    const labels = ["Project Start", "Elapsed Time", "Projected Exhaustion", "Scheduled End"];
    const spendSeries = [0, b.currentSpend, b.totalBudget, b.totalBudget];
    const budgetCeiling = [b.totalBudget, b.totalBudget, b.totalBudget, b.totalBudget];

    budgetChartInstance = new Chart(ctx, {
        type: "line",
        data: {
            labels: labels,
            datasets: [
                {
                    label: "Actual / Projected Spend",
                    data: spendSeries,
                    borderColor: "#ef4444",
                    backgroundColor: "rgba(239, 68, 68, 0.1)",
                    borderWidth: 3,
                    tension: 0.3,
                    fill: true
                },
                {
                    label: "Total Budget Ceiling",
                    data: budgetCeiling,
                    borderColor: "#10b981",
                    borderDash: [5, 5],
                    borderWidth: 2,
                    fill: false
                }
            ]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: { position: "top" }
            },
            scales: {
                y: {
                    beginAtZero: true,
                    ticks: {
                        callback: (value) => `₹${formatNumber(value)}`
                    }
                }
            }
        }
    });
}

function renderDashboardRecentConflicts(conflicts) {
    const container = document.getElementById("dashboardRecentConflictsList");
    if (!container) return;
    container.innerHTML = "";

    if (!conflicts || conflicts.length === 0) {
        container.innerHTML = "<div style='color: var(--success); font-weight: 600; padding: 1rem 0;'>🎉 No active conflicts. All schedules healthy!</div>";
        return;
    }

    conflicts.slice(0, 5).forEach(c => {
        const item = document.createElement("div");
        const severity = (c.severity || "WARNING").toUpperCase();
        item.className = `conflict-item ${severity.toLowerCase()}`;
        const typeLabel = (c.conflictType || c.type || c.title || "SCHEDULE CONFLICT").replace(/_/g, " ");
        item.innerHTML = `
            <div class="conflict-body">
                <h4>
                    <span class="badge badge-${severity.toLowerCase()}">${severity}</span>
                    ${typeLabel}
                </h4>
                <p>${c.description || 'Schedule conflict detected.'}</p>
                <div class="conflict-suggestion"><strong>Resolution:</strong> ${c.recommendation || 'Review allocation.'}</div>
            </div>
        `;
        container.appendChild(item);
    });
}

function renderDashboardProjectsTable(projects) {
    const tbody = document.getElementById("dashboardProjectsTbody");
    tbody.innerHTML = "";

    if (!projects || projects.length === 0) {
        tbody.innerHTML = "<tr><td colspan='7' style='text-align: center;'>No projects found.</td></tr>";
        return;
    }

    projects.forEach(p => {
        const tr = document.createElement("tr");
        tr.innerHTML = `
            <td><strong style="cursor: pointer; color: var(--primary);" onclick="openProjectDetails(${p.id})">${p.name}</strong><br><small style="color: var(--text-muted);">${p.description || ''}</small></td>
            <td><span class="badge badge-${p.status === 'ACTIVE' ? 'info' : (p.status === 'COMPLETED' ? 'success' : 'neutral')}">${p.status}</span></td>
            <td>${p.startDate} → ${p.endDate}</td>
            <td>₹${formatNumber(p.currentSpend)} / ₹${formatNumber(p.totalBudget)}</td>
            <td>
                <div style="display: flex; align-items: center; gap: 6px;">
                    <div class="progress-bar-container" style="flex: 1;">
                        <div class="progress-bar-fill" style="width: ${Math.min(100, p.budgetUtilization)}%; background: ${p.budgetUtilization > 85 ? 'var(--critical)' : 'var(--primary)'};"></div>
                    </div>
                    <span>${p.budgetUtilization}%</span>
                </div>
            </td>
            <td><span class="badge badge-neutral">${p.milestoneCount} milestones</span></td>
            <td>
                <button class="btn btn-sm btn-outline" onclick="openProjectDetails(${p.id})">Details</button>
                <button class="btn btn-sm btn-outline" onclick="openWhatIfForProject(${p.id})">Simulate</button>
            </td>
        `;
        tbody.appendChild(tr);
    });
}

// ==========================================================================
// VIEW 2: Projects & Milestones Management
// ==========================================================================
async function loadProjectsManager() {
    try {
        const res = await fetch("/api/projects");
        const json = await res.json();
        if (json.status !== "SUCCESS") return;

        allProjectsCache = json.data || [];
        applyProjectsFilter();
    } catch (err) {
        console.error("Error loading projects manager:", err);
    }
}

function applyProjectsFilter() {
    const query = (document.getElementById("projectSearchInput")?.value || "").toLowerCase().trim();
    const status = document.getElementById("projectStatusFilter")?.value || "ALL";

    const filtered = allProjectsCache.filter(p => {
        const matchesQuery = !query || 
            p.name.toLowerCase().includes(query) || 
            (p.description && p.description.toLowerCase().includes(query));
        const matchesStatus = (status === "ALL") || (p.status === status);
        return matchesQuery && matchesStatus;
    });

    renderProjectsList(filtered);
}

function renderProjectsList(projects) {
    const container = document.getElementById("projectsFullContainer");
    if (!container) return;
    container.innerHTML = "";

    if (!projects || projects.length === 0) {
        container.innerHTML = "<p style='color: var(--text-muted); padding: 1.5rem 0; text-align: center;'>No projects match your search or filter criteria.</p>";
        return;
    }

    projects.forEach(p => {
        const card = document.createElement("div");
        card.className = "card";
        card.style.marginBottom = "1.5rem";
        card.innerHTML = `
            <div class="card-header">
                <div>
                    <h3 style="display: flex; align-items: center; gap: 8px;">
                        <span style="cursor: pointer;" onclick="openProjectDetails(${p.id})">${p.name}</span>
                        <span class="badge badge-${p.status === 'COMPLETED' ? 'success' : (p.status === 'ACTIVE' ? 'info' : 'neutral')}">${p.status}</span>
                    </h3>
                    <small style="color: var(--text-muted);">${p.description || ''} • Dates: ${p.startDate} to ${p.endDate} • Budget: ₹${formatNumber(p.currentSpend)} / ₹${formatNumber(p.totalBudget)} (${p.budgetUtilization}%)</small>
                </div>
                <div style="display: flex; gap: 0.5rem;">
                    <button class="btn btn-sm btn-outline" onclick="openProjectDetails(${p.id})">View Details</button>
                    <button class="btn btn-sm btn-primary" onclick="openAddMilestoneForProject(${p.id})">+ Milestone</button>
                </div>
            </div>
            <div class="table-responsive">
                <table class="data-table">
                    <thead>
                        <tr>
                            <th>Milestone</th>
                            <th>Timeline</th>
                            <th>Status</th>
                            <th>Progress</th>
                            <th>Required Skills</th>
                            <th>Assigned Resources</th>
                            <th>Actions</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${p.milestones && p.milestones.length > 0 ? p.milestones.map(m => `
                            <tr>
                                <td><strong>${m.name}</strong><br><small style="color: var(--text-muted);">${m.description || ''}</small></td>
                                <td>${m.startDate} → ${m.endDate}</td>
                                <td><span class="badge badge-${m.status === 'COMPLETED' ? 'success' : (m.status === 'IN_PROGRESS' ? 'info' : 'neutral')}">${m.status}</span></td>
                                <td>
                                    <div style="display: flex; align-items: center; gap: 6px;">
                                        <div class="progress-bar-container" style="width: 70px;">
                                            <div class="progress-bar-fill" style="width: ${m.progressPercentage}%;"></div>
                                        </div>
                                        <span>${m.progressPercentage}%</span>
                                    </div>
                                </td>
                                <td>
                                    <div class="skills-container">
                                        ${(() => {
                                            const mSkills = parseSkillsList(m.requiredSkillNames || m.requiredSkills);
                                            return mSkills.length > 0 ? 
                                                mSkills.map(s => `<span class="skill-tag">${escapeHtml(s)}</span>`).join('') : '<small style="color: var(--text-muted);">None</small>';
                                        })()}
                                    </div>
                                </td>
                                <td>
                                    ${m.allocations && m.allocations.length > 0 ? 
                                        m.allocations.map(a => `<div><strong>${a.developerName}</strong> (${a.allocationPercentage}%) <button class="btn btn-sm btn-outline" style="padding: 1px 4px; font-size: 0.65rem;" onclick="removeAllocation(${a.id})">&times;</button></div>`).join('') 
                                        : '<span class="badge badge-warning">Unassigned</span>'}
                                </td>
                                <td>
                                    <button class="btn btn-sm btn-outline" onclick="openAssignModalForMilestone(${m.id})">Assign</button>
                                    <button class="btn btn-sm btn-outline" onclick="openWhatIfForMilestone(${m.id})">Simulate</button>
                                </td>
                            </tr>
                        `).join('') : '<tr><td colspan="7" style="text-align: center; color: var(--text-muted);">No milestones in this project yet.</td></tr>'}
                    </tbody>
                </table>
            </div>
        `;
        container.appendChild(card);
    });
}

// ==========================================================================
// VIEW 3: Project Detail Drill-Down View
// ==========================================================================
function openProjectDetails(projectId) {
    currentProjectIdForDetail = projectId;
    switchTab("viewProjectDetails");
}

async function loadProjectDetailsView(projectId) {
    try {
        const [resProj, resBudget] = await Promise.all([
            fetch(`/api/projects`),
            fetch(`/api/projects/${projectId}/budget`)
        ]);
        const jsonProj = await resProj.json();
        const jsonBudget = await resBudget.json();

        const projects = jsonProj.data || [];
        const p = projects.find(item => item.id === projectId) || projects[0];
        if (!p) return;

        currentProjectIdForDetail = p.id;
        const b = jsonBudget.status === "SUCCESS" ? jsonBudget.data : null;

        // Banner
        document.getElementById("projectDetailName").innerText = p.name;
        document.getElementById("projectDetailDesc").innerText = p.description || "No description provided.";
        document.getElementById("projectDetailDates").innerText = `Dates: ${p.startDate} to ${p.endDate}`;
        
        const badge = document.getElementById("projectDetailStatusBadge");
        badge.innerText = p.status;
        badge.className = `badge badge-${p.status === 'COMPLETED' ? 'success' : (p.status === 'ACTIVE' ? 'info' : 'neutral')}`;

        document.getElementById("projectDetailBudgetRatio").innerText = `₹${formatNumber(p.currentSpend)} / ₹${formatNumber(p.totalBudget)}`;
        document.getElementById("projectDetailBudgetPercent").innerText = `${p.budgetUtilization}% spent`;

        // Wire Buttons in banner
        document.getElementById("btnProjectDetailAddMilestone").onclick = () => openAddMilestoneForProject(p.id);
        document.getElementById("btnProjectDetailSimulate").onclick = () => openWhatIfForProject(p.id);

        // KPIs
        document.getElementById("projectDetailTotalBudget").innerText = `₹${formatNumber(p.totalBudget)}`;
        document.getElementById("projectDetailSpend").innerText = `₹${formatNumber(p.currentSpend)}`;
        document.getElementById("projectDetailRemaining").innerText = `Remaining: ₹${formatNumber(b ? b.remainingBudget : (p.totalBudget - p.currentSpend))}`;
        document.getElementById("projectDetailBurnRate").innerText = b ? `₹${formatNumber(b.dailyBurnRate)}/day` : "-";
        document.getElementById("projectDetailExhaustion").innerText = b ? b.forecastExhaustionDate : "-";
        
        const ind = document.getElementById("projectDetailStatusIndicator");
        if (b && b.isOverBudget) {
            ind.innerText = "Status: OVER BUDGET";
            ind.style.color = "var(--critical)";
        } else if (b && b.currentSpend > b.totalBudget * 0.85) {
            ind.innerText = "Status: AT RISK";
            ind.style.color = "var(--warning)";
        } else {
            ind.innerText = "Status: HEALTHY";
            ind.style.color = "var(--success)";
        }

        // Milestones
        const mBody = document.getElementById("projectDetailMilestonesTbody");
        mBody.innerHTML = "";
        if (p.milestones && p.milestones.length > 0) {
            p.milestones.forEach(m => {
                const tr = document.createElement("tr");
                tr.innerHTML = `
                    <td><strong>${m.name}</strong><br><small style="color: var(--text-muted);">${m.description || ''}</small></td>
                    <td>${m.startDate} → ${m.endDate}</td>
                    <td><span class="badge badge-${m.status === 'COMPLETED' ? 'success' : (m.status === 'IN_PROGRESS' ? 'info' : 'neutral')}">${m.status}</span></td>
                    <td>
                        <div style="display: flex; align-items: center; gap: 6px;">
                            <div class="progress-bar-container" style="width: 70px;">
                                <div class="progress-bar-fill" style="width: ${m.progressPercentage}%;"></div>
                            </div>
                            <span>${m.progressPercentage}%</span>
                        </div>
                    </td>
                    <td>
                        <div class="skills-container">
                            ${(() => {
                                const mSkills = parseSkillsList(m.requiredSkillNames || m.requiredSkills);
                                return mSkills.length > 0 ? 
                                    mSkills.map(s => `<span class="skill-tag">${escapeHtml(s)}</span>`).join('') : '<small style="color: var(--text-muted);">None</small>';
                            })()}
                        </div>
                    </td>
                    <td>
                        ${m.allocations && m.allocations.length > 0 ? 
                            m.allocations.map(a => `<div><strong>${a.developerName}</strong> (${a.allocationPercentage}%)</div>`).join('') 
                            : '<span class="badge badge-warning">Unassigned</span>'}
                    </td>
                    <td>
                        <button class="btn btn-sm btn-outline" onclick="openAssignModalForMilestone(${m.id})">Assign</button>
                        <button class="btn btn-sm btn-outline" onclick="openWhatIfForMilestone(${m.id})">Simulate</button>
                    </td>
                `;
                mBody.appendChild(tr);
            });
        } else {
            mBody.innerHTML = "<tr><td colspan='7' style='text-align: center; color: var(--text-muted);'>No milestones in this project.</td></tr>";
        }

        // Assigned Resources
        const resList = document.getElementById("projectDetailResourcesList");
        resList.innerHTML = "";
        const devMap = new Map();
        if (p.milestones) {
            p.milestones.forEach(m => {
                if (m.allocations) {
                    m.allocations.forEach(a => {
                        if (!devMap.has(a.developerId)) {
                            devMap.set(a.developerId, { name: a.developerName, allocations: [] });
                        }
                        devMap.get(a.developerId).allocations.push({ milestone: m.name, pct: a.allocationPercentage });
                    });
                }
            });
        }

        if (devMap.size > 0) {
            devMap.forEach((v, devId) => {
                const item = document.createElement("div");
                item.style.padding = "0.75rem";
                item.style.background = "#f8fafc";
                item.style.borderRadius = "var(--radius-sm)";
                item.style.border = "1px solid var(--border-color)";
                item.innerHTML = `
                    <div style="display: flex; justify-content: space-between; align-items: center;">
                        <strong>${v.name}</strong>
                        <button class="btn btn-sm btn-outline" onclick="switchToDevPortal(${devId})">View Portal</button>
                    </div>
                    <div style="font-size: 0.8rem; color: var(--text-muted); margin-top: 4px;">
                        ${v.allocations.map(al => `${al.milestone} (${al.pct}%)`).join(', ')}
                    </div>
                `;
                resList.appendChild(item);
            });
        } else {
            resList.innerHTML = "<p style='color: var(--text-muted); font-size: 0.85rem;'>No resources allocated yet.</p>";
        }

        // Transactions Log
        const tBody = document.getElementById("projectDetailTransactionsTbody");
        tBody.innerHTML = "";
        const txs = b && b.recentTransactions ? b.recentTransactions : [];
        if (txs.length > 0) {
            txs.forEach(t => {
                const tr = document.createElement("tr");
                tr.innerHTML = `
                    <td>${t.transactionDate}</td>
                    <td><strong>₹${formatNumber(t.amount)}</strong></td>
                    <td>${t.description || '-'}</td>
                    <td>${t.recordedBy || 'System'}</td>
                `;
                tBody.appendChild(tr);
            });
        } else {
            tBody.innerHTML = "<tr><td colspan='4' style='text-align: center; color: var(--text-muted);'>No transactions recorded.</td></tr>";
        }
    } catch (err) {
        console.error("Error loading project details view:", err);
    }
}

// ==========================================================================
// VIEW 3: Resource Directory
// ==========================================================================
async function loadResourceDirectory() {
    try {
        const res = await fetch("/api/developers");
        const json = await res.json();
        if (json.status !== "SUCCESS") return;

        allDevelopersCache = json.data || [];
        populateDevSkillFilter(allDevelopersCache);
        applyDevelopersFilter();
    } catch (err) {
        console.error("Error loading resources:", err);
    }
}

function populateDevSkillFilter(developers) {
    const filterSelect = document.getElementById("devSkillFilter");
    if (!filterSelect) return;
    const currentVal = filterSelect.value;
    const skillSet = new Set();
    developers.forEach(d => {
        const skills = parseSkillsList(d.skills);
        skills.forEach(s => skillSet.add(s));
    });

    filterSelect.innerHTML = '<option value="ALL">All Skills</option>';
    Array.from(skillSet).sort().forEach(s => {
        const opt = document.createElement("option");
        opt.value = s;
        opt.text = s;
        filterSelect.appendChild(opt);
    });
    if (currentVal && Array.from(skillSet).includes(currentVal)) {
        filterSelect.value = currentVal;
    }
}

function applyDevelopersFilter() {
    const query = (document.getElementById("devSearchInput")?.value || "").toLowerCase().trim();
    const skill = document.getElementById("devSkillFilter")?.value || "ALL";
    const sort = document.getElementById("devSortSelect")?.value || "NAME";

    let filtered = allDevelopersCache.filter(d => {
        const matchesQuery = !query || 
            d.name.toLowerCase().includes(query) || 
            (d.roleTitle && d.roleTitle.toLowerCase().includes(query)) ||
            (d.email && d.email.toLowerCase().includes(query));
        const devSkills = parseSkillsList(d.skills);
        const matchesSkill = (skill === "ALL") || devSkills.includes(skill);
        return matchesQuery && matchesSkill;
    });

    filtered.sort((a, b) => {
        if (sort === "NAME") return a.name.localeCompare(b.name);
        if (sort === "WORKLOAD_DESC") return (b.currentAllocation || 0) - (a.currentAllocation || 0);
        if (sort === "WORKLOAD_ASC") return (a.currentAllocation || 0) - (b.currentAllocation || 0);
        if (sort === "RATE_ASC") return (a.hourlyCost || 0) - (b.hourlyCost || 0);
        return 0;
    });

    renderDevelopersGrid(filtered);
}

function renderDevelopersGrid(developers) {
    const grid = document.getElementById("developersCardsGrid");
    if (!grid) return;
    grid.innerHTML = "";

    if (!developers || developers.length === 0) {
        grid.innerHTML = "<p style='color: var(--text-muted); grid-column: 1 / -1; text-align: center; padding: 2rem 0;'>No developers match your search or filter criteria.</p>";
        return;
    }

    developers.forEach(d => {
        const card = document.createElement("div");
        card.className = "kpi-card";
        card.style.padding = "1.25rem";
        const devSkills = parseSkillsList(d.skills);
        card.innerHTML = `
            <div style="display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 0.75rem;">
                <div>
                    <h4 style="font-size: 1.05rem; font-weight: 700; color: var(--text-main);">${escapeHtml(d.name)}</h4>
                    <div style="font-size: 0.82rem; color: var(--text-muted); margin-top: 2px;">${escapeHtml(d.roleTitle || 'Developer')}</div>
                    <small style="color: var(--primary); font-size: 0.75rem; word-break: break-all;">${escapeHtml(d.email)}</small>
                </div>
                <div style="text-align: right;">
                    <span class="badge badge-${d.status === 'ACTIVE' ? 'success' : 'neutral'}">${escapeHtml(d.status)}</span>
                    <div style="font-weight: 700; color: #0f172a; font-size: 0.95rem; margin-top: 4px;">₹${d.hourlyCost}/hr</div>
                </div>
            </div>
            
            <div style="margin: 0.65rem 0;">
                <small style="font-weight: 600; color: var(--text-muted); text-transform: uppercase; font-size: 0.7rem; letter-spacing: 0.04em;">Skills</small>
                <div class="skills-container">
                    ${devSkills.length > 0 ? 
                        devSkills.map(s => `<span class="skill-tag">${escapeHtml(s)}</span>`).join('') : '<small style="color: var(--text-muted);">None</small>'}
                </div>
            </div>

            <div style="margin-top: 1rem; padding-top: 0.75rem; border-top: 1px solid var(--border-color);">
                <div style="display: flex; justify-content: space-between; font-size: 0.8rem; margin-bottom: 4px;">
                    <span>Current Workload:</span>
                    <strong style="color: ${d.currentAllocation > 100 ? 'var(--critical)' : 'var(--text-main)'};">${d.currentAllocation}% / ${d.availabilityPercentage}%</strong>
                </div>
                <div class="progress-bar-container">
                    <div class="progress-bar-fill" style="width: ${Math.min(100, d.currentAllocation)}%; background: ${d.currentAllocation > 100 ? 'var(--critical)' : (d.currentAllocation > 85 ? 'var(--warning)' : 'var(--primary)')};"></div>
                </div>
                <div style="display: flex; justify-content: space-between; font-size: 0.75rem; color: var(--text-muted); margin-top: 4px;">
                    <span>Remaining Capacity: ${d.remainingCapacity}%</span>
                    <span>Availability: ${d.availabilityPercentage}%</span>
                </div>
            </div>

            <div style="margin-top: 1rem; display: flex; gap: 0.5rem;">
                <button class="btn btn-sm btn-outline" style="flex: 1;" onclick="openAssignForDev(${d.id})">Assign Work</button>
                <button class="btn btn-sm btn-outline" onclick="switchToDevPortal(${d.id})">View Portal</button>
            </div>
        `;
        grid.appendChild(card);
    });
}

function switchToDevPortal(devId) {
    loginAsDeveloper(devId);
}

// ==========================================================================
// VIEW 5: Allocation Matrix (Resources x Milestones Grid)
// ==========================================================================
async function loadAllocationMatrix() {
    try {
        const [resDev, resProj] = await Promise.all([
            fetch("/api/developers"),
            fetch("/api/projects")
        ]);
        const jsonDev = await resDev.json();
        const jsonProj = await resProj.json();

        allDevelopersCache = jsonDev.data || [];
        allProjectsCache = jsonProj.data || [];

        // Populate Matrix Project Select Filter
        const pSel = document.getElementById("matrixProjectSelect");
        if (pSel && pSel.options.length <= 1) {
            allProjectsCache.forEach(p => {
                const opt = document.createElement("option");
                opt.value = p.id;
                opt.text = p.name;
                pSel.appendChild(opt);
            });
            pSel.onchange = () => renderAllocationMatrix();
        }

        renderAllocationMatrix();
    } catch (err) {
        console.error("Error loading allocation matrix:", err);
    }
}

function renderAllocationMatrix() {
    const pSel = document.getElementById("matrixProjectSelect");
    const filterProjId = pSel ? pSel.value : "ALL";

    // Gather milestones based on filter
    let targetMilestones = [];
    allProjectsCache.forEach(p => {
        if (filterProjId === "ALL" || p.id === parseInt(filterProjId)) {
            if (p.milestones) {
                p.milestones.forEach(m => {
                    targetMilestones.push({ ...m, projectName: p.name });
                });
            }
        }
    });

    const thead = document.getElementById("allocationMatrixThead");
    const tbody = document.getElementById("allocationMatrixTbody");
    if (!thead || !tbody) return;

    // Header: Resource + each Milestone + Total Workload
    thead.innerHTML = `
        <tr>
            <th style="min-width: 180px; position: sticky; left: 0; background: #f8fafc; z-index: 2;">Resource</th>
            ${targetMilestones.map(m => `
                <th style="min-width: 140px; text-align: center;">
                    <div style="font-weight: 700;">${m.name}</div>
                    <small style="color: var(--text-muted); font-weight: normal;">${m.projectName}</small>
                </th>
            `).join('')}
            <th style="min-width: 120px; text-align: center;">Total Workload</th>
        </tr>
    `;

    tbody.innerHTML = "";
    if (allDevelopersCache.length === 0) {
        tbody.innerHTML = `<tr><td colspan="${targetMilestones.length + 2}" style="text-align: center; color: var(--text-muted);">No resources found.</td></tr>`;
        return;
    }

    allDevelopersCache.forEach(d => {
        const tr = document.createElement("tr");

        // Map dev's milestone allocations
        const cellsHtml = targetMilestones.map(m => {
            const alloc = m.allocations ? m.allocations.find(a => a.developerId === d.id) : null;
            if (alloc) {
                return `<td style="text-align: center;"><span class="badge badge-info">${alloc.allocationPercentage}%</span></td>`;
            }
            return `<td style="text-align: center; color: #cbd5e1;">-</td>`;
        }).join('');

        const alloc = d.currentAllocation || 0;
        const badgeClass = alloc > 100 ? "badge-critical" : (alloc > 85 ? "badge-warning" : "badge-success");

        tr.innerHTML = `
            <td style="position: sticky; left: 0; background: white; z-index: 1;">
                <strong>${d.name}</strong><br>
                <small style="color: var(--text-muted);">${d.roleTitle || 'Developer'}</small>
            </td>
            ${cellsHtml}
            <td style="text-align: center;">
                <span class="badge ${badgeClass}">${alloc}%</span>
            </td>
        `;
        tbody.appendChild(tr);
    });
}

// ==========================================================================
// VIEW 6: Interactive Project Timeline
// ==========================================================================
async function loadTimeline() {
    const container = document.getElementById("timelineContainer");
    try {
        const res = await fetch("/api/projects");
        const json = await res.json();
        if (json.status !== "SUCCESS") return;

        const projects = json.data;
        container.innerHTML = "";

        projects.forEach(p => {
            const pDiv = document.createElement("div");
            pDiv.className = "timeline-project";
            pDiv.innerHTML = `
                <div class="timeline-project-title">
                    <span>📁 ${p.name}</span>
                    <span style="font-size: 0.82rem; color: var(--text-muted);">${p.startDate} to ${p.endDate}</span>
                </div>
                ${p.milestones && p.milestones.length > 0 ? p.milestones.map(m => `
                    <div class="timeline-milestone-row">
                        <div><strong>${m.name}</strong></div>
                        <div style="color: var(--text-muted);">${m.startDate} → ${m.endDate}</div>
                        <div>
                            <div class="progress-bar-container">
                                <div class="progress-bar-fill" style="width: ${m.progressPercentage}%;"></div>
                            </div>
                        </div>
                        <div>
                            <span class="badge badge-${m.status === 'COMPLETED' ? 'success' : (m.status === 'IN_PROGRESS' ? 'info' : 'neutral')}">${m.status}</span>
                        </div>
                        <div>
                            ${m.allocations && m.allocations.length > 0 ? 
                                m.allocations.map(a => `<span class="badge badge-info" style="margin-right: 2px;">${a.developerName} (${a.allocationPercentage}%)</span>`).join('') 
                                : '<span class="badge badge-warning">Unassigned</span>'}
                        </div>
                    </div>
                `).join('') : '<p style="color: var(--text-muted); font-size: 0.85rem;">No milestones configured.</p>'}
            `;
            container.appendChild(pDiv);
        });
    } catch (err) {
        console.error("Error loading timeline:", err);
    }
}

// ==========================================================================
// VIEW 5: Resource Utilization Heatmap
// ==========================================================================
async function loadHeatmap() {
    const tbody = document.getElementById("heatmapTbody");
    try {
        const res = await fetch("/api/developers");
        const json = await res.json();
        if (json.status !== "SUCCESS") return;

        const developers = json.data;
        tbody.innerHTML = "";

        developers.forEach(d => {
            const tr = document.createElement("tr");
            const alloc = d.currentAllocation;
            const utilClass = alloc === 0 ? "" : (alloc < 50 ? "util-low" : (alloc <= 85 ? "util-healthy" : (alloc <= 100 ? "util-high" : "util-over")));

            tr.innerHTML = `
                <td><strong>${d.name}</strong><br><small style="color: var(--text-muted);">${d.roleTitle || 'Developer'}</small></td>
                <td>${d.availabilityPercentage}%</td>
                <td><strong>${alloc}%</strong></td>
                <td class="${utilClass} util-cell">${alloc}%</td>
                <td class="${utilClass} util-cell">${alloc}%</td>
                <td class="${utilClass} util-cell">${alloc}%</td>
                <td class="${utilClass} util-cell">${alloc}%</td>
                <td class="${utilClass} util-cell">${alloc}%</td>
                <td>
                    ${alloc > 100 ? '<span class="badge badge-critical">Over-allocated</span>' : 
                      (alloc >= 85 ? '<span class="badge badge-warning">High</span>' : 
                      (alloc >= 50 ? '<span class="badge badge-success">Optimal</span>' : '<span class="badge badge-neutral">Available</span>'))}
                </td>
            `;
            tbody.appendChild(tr);
        });
    } catch (err) {
        console.error("Error loading heatmap:", err);
    }
}

// ==========================================================================
// VIEW 6: Leave & Availability Calendar
// ==========================================================================
async function loadLeavesManager() {
    const tbody = document.getElementById("leavesTableTbody");
    try {
        const res = await fetch("/api/availability");
        const json = await res.json();
        if (json.status !== "SUCCESS") return;

        const leaves = json.data;
        tbody.innerHTML = "";

        if (leaves.length === 0) {
            tbody.innerHTML = "<tr><td colspan='8' style='text-align: center; color: var(--text-muted);'>No planned leaves scheduled.</td></tr>";
            return;
        }

        leaves.forEach(l => {
            const tr = document.createElement("tr");
            tr.innerHTML = `
                <td><strong>${l.developerName}</strong></td>
                <td>${l.startDate}</td>
                <td>${l.endDate}</td>
                <td><span class="badge badge-neutral">${calculateDays(l.startDate, l.endDate)} days</span></td>
                <td>${l.reason || 'Personal Leave'}</td>
                <td><span class="badge badge-success">${l.status}</span></td>
                <td><span style="font-size: 0.8rem; color: var(--critical); font-weight: 600;">Capacity = 0%</span></td>
                <td>
                    <button class="btn btn-sm btn-outline btn-danger" onclick="cancelLeave(${l.id})">Cancel</button>
                </td>
            `;
            tbody.appendChild(tr);
        });
    } catch (err) {
        console.error("Error loading leaves:", err);
    }
}

// ==========================================================================
// VIEW 7: Conflict Center
// ==========================================================================
async function loadConflictCenter() {
    try {
        const res = await fetch("/api/conflicts");
        const json = await res.json();
        if (json.status !== "SUCCESS") return;

        allConflictsCache = json.data || [];
        updateConflictCounts();
        applyConflictsFilter();
    } catch (err) {
        console.error("Error loading conflicts:", err);
    }
}

function updateConflictCounts() {
    const total = allConflictsCache.length;
    const crit = allConflictsCache.filter(c => c.severity === "CRITICAL").length;
    const warn = allConflictsCache.filter(c => c.severity === "WARNING").length;

    const elAll = document.getElementById("confCountAll");
    const elCrit = document.getElementById("confCountCrit");
    const elWarn = document.getElementById("confCountWarn");

    if (elAll) elAll.innerText = total;
    if (elCrit) elCrit.innerText = crit;
    if (elWarn) elWarn.innerText = warn;
}

function filterConflicts(severity) {
    currentConflictSeverityFilter = severity;

    const btnContainer = document.getElementById("conflictFilterButtons");
    if (btnContainer) {
        btnContainer.querySelectorAll("button").forEach(b => b.classList.remove("active"));
        const targetBtn = Array.from(btnContainer.querySelectorAll("button")).find(b => b.getAttribute("onclick")?.includes(`'${severity}'`));
        if (targetBtn) targetBtn.classList.add("active");
    }

    applyConflictsFilter();
}

function applyConflictsFilter() {
    const query = (document.getElementById("conflictSearchInput")?.value || "").toLowerCase().trim();

    const filtered = allConflictsCache.filter(c => {
        const severity = (c.severity || "WARNING").toUpperCase();
        const matchesSeverity = (currentConflictSeverityFilter === "ALL") || (severity === currentConflictSeverityFilter);
        const typeStr = (c.conflictType || c.type || c.title || "").toLowerCase();
        const matchesQuery = !query ||
            typeStr.includes(query) ||
            (c.description && c.description.toLowerCase().includes(query)) ||
            (c.developerName && c.developerName.toLowerCase().includes(query)) ||
            (c.projectName && c.projectName.toLowerCase().includes(query));
        return matchesSeverity && matchesQuery;
    });

    renderFilteredConflicts(filtered);
}

function renderFilteredConflicts(conflicts) {
    const container = document.getElementById("conflictsFullList");
    if (!container) return;
    container.innerHTML = "";

    if (!conflicts || conflicts.length === 0) {
        container.innerHTML = "<div style='padding: 2.5rem; text-align: center; color: var(--success); font-weight: 600; font-size: 0.95rem;'>🎉 No active conflicts matching filter criteria. All resources and schedules are operating smoothly.</div>";
        return;
    }

    conflicts.forEach(c => {
        const item = document.createElement("div");
        const severity = (c.severity || "WARNING").toUpperCase();
        item.className = `conflict-item ${severity.toLowerCase()}`;
        const typeLabel = (c.conflictType || c.type || c.title || "SCHEDULE CONFLICT").replace(/_/g, " ");
        
        let actionBtnHtml = "";
        if (c.milestoneId) {
            actionBtnHtml = `<button class="btn btn-sm btn-primary" onclick="openWhatIfForMilestone(${c.milestoneId})" title="Simulate resource adjustments in What-If Simulator">⚡ Simulate Fix</button>`;
        } else {
            actionBtnHtml = `<button class="btn btn-sm btn-outline" onclick="switchTab('viewSimulator')" title="Open What-If Simulator">💡 Open Simulator</button>`;
        }

        item.innerHTML = `
            <div class="conflict-body" style="flex: 1;">
                <h4>
                    <span class="badge badge-${severity.toLowerCase()}">${severity}</span>
                    <span>${typeLabel}</span>
                    ${c.projectName ? `• <span style="font-weight: 500; color: var(--text-muted); font-size: 0.85rem;">${c.projectName}</span>` : ''}
                </h4>
                <p>${c.description || 'Schedule conflict detected.'}</p>
                <div class="conflict-suggestion"><strong>Recommended Resolution:</strong> ${c.recommendation || 'Reallocate resources or adjust milestones.'}</div>
            </div>
            <div style="display: flex; align-items: center; padding-left: 0.5rem;">
                ${actionBtnHtml}
            </div>
        `;
        container.appendChild(item);
    });
}

// ==========================================================================
// VIEW 10: Dedicated Budget Forecast Page
// ==========================================================================
async function loadBudgetForecastPage() {
    try {
        const res = await fetch("/api/projects");
        const json = await res.json();
        if (json.status !== "SUCCESS") return;

        allProjectsCache = json.data || [];
        const pSel = document.getElementById("dedicatedBudgetProjectSelect");
        if (!pSel) return;

        pSel.innerHTML = "";
        allProjectsCache.forEach(p => {
            const opt = document.createElement("option");
            opt.value = p.id;
            opt.text = p.name;
            pSel.appendChild(opt);
        });

        pSel.onchange = () => renderBudgetForecastPage(pSel.value);
        if (allProjectsCache.length > 0) {
            renderBudgetForecastPage(allProjectsCache[0].id);
        }
    } catch (err) {
        console.error("Error loading budget forecast page:", err);
    }
}

async function renderBudgetForecastPage(projectId) {
    try {
        const res = await fetch(`/api/projects/${projectId}/budget`);
        const json = await res.json();
        if (json.status !== "SUCCESS") return;

        const b = json.data;
        document.getElementById("forecastTotalBudget").innerText = `₹${formatNumber(b.totalBudget)}`;
        document.getElementById("forecastCurrentSpend").innerText = `₹${formatNumber(b.currentSpend)}`;
        const pct = b.totalBudget > 0 ? Math.round((b.currentSpend / b.totalBudget) * 100) : 0;
        document.getElementById("forecastSpendPercent").innerText = `${pct}% utilized`;
        document.getElementById("forecastRemaining").innerText = `₹${formatNumber(b.remainingBudget)}`;
        document.getElementById("forecastBurnRate").innerText = `₹${formatNumber(b.dailyBurnRate)}/day`;
        document.getElementById("forecastExhaustionDate").innerText = b.forecastExhaustionDate;

        const badge = document.getElementById("forecastStatusBadge");
        if (b.isOverBudget) {
            badge.innerText = "Status: OVER BUDGET";
            badge.className = "kpi-sub";
            badge.style.color = "var(--critical)";
        } else if (pct > 85) {
            badge.innerText = "Status: AT RISK";
            badge.style.color = "var(--warning)";
        } else {
            badge.innerText = "Status: HEALTHY";
            badge.style.color = "var(--success)";
        }

        // Render dedicated chart
        const canvas = document.getElementById("dedicatedBudgetChart");
        if (canvas) {
            const ctx = canvas.getContext("2d");
            if (dedicatedBudgetChartInstance) {
                dedicatedBudgetChartInstance.destroy();
            }

            const labels = ["Project Start", "Elapsed Window", "Projected Exhaustion", "Scheduled End"];
            const spendSeries = [0, b.currentSpend, b.totalBudget, b.totalBudget];
            const budgetCeiling = [b.totalBudget, b.totalBudget, b.totalBudget, b.totalBudget];

            dedicatedBudgetChartInstance = new Chart(ctx, {
                type: "line",
                data: {
                    labels: labels,
                    datasets: [
                        {
                            label: "Actual / Projected Spend",
                            data: spendSeries,
                            borderColor: "#ef4444",
                            backgroundColor: "rgba(239, 68, 68, 0.1)",
                            borderWidth: 3,
                            tension: 0.3,
                            fill: true
                        },
                        {
                            label: "Total Budget Ceiling",
                            data: budgetCeiling,
                            borderColor: "#10b981",
                            borderDash: [5, 5],
                            borderWidth: 2,
                            fill: false
                        }
                    ]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    scales: {
                        y: {
                            beginAtZero: true,
                            ticks: {
                                callback: val => "₹" + (val >= 1000 ? (val / 1000) + "k" : val)
                            }
                        }
                    }
                }
            });
        }

        // Transactions
        const tBody = document.getElementById("forecastTransactionsTbody");
        tBody.innerHTML = "";
        const txs = b.recentTransactions || [];
        if (txs.length > 0) {
            txs.forEach(t => {
                const tr = document.createElement("tr");
                tr.innerHTML = `
                    <td><code>#TX-${t.id}</code></td>
                    <td>${t.transactionDate}</td>
                    <td><strong>₹${formatNumber(t.amount)}</strong></td>
                    <td>${t.description || 'General Development Expense'}</td>
                    <td>${t.recordedBy || 'System'}</td>
                `;
                tBody.appendChild(tr);
            });
        } else {
            tBody.innerHTML = "<tr><td colspan='5' style='text-align: center; color: var(--text-muted);'>No spend transactions recorded for this project.</td></tr>";
        }
    } catch (err) {
        console.error("Error rendering budget forecast page:", err);
    }
}

// ==========================================================================
// VIEW 11: What-If Resource Swap Simulator
// ==========================================================================
async function loadSimulatorSetup() {
    try {
        const [resM, resD] = await Promise.all([
            fetch("/api/milestones"),
            fetch("/api/developers")
        ]);
        const jsonM = await resM.json();
        const jsonD = await resD.json();

        allMilestonesCache = jsonM.data || [];
        allDevelopersCache = jsonD.data || [];

        const mSelect = document.getElementById("simMilestoneSelect");
        const curDevSelect = document.getElementById("simCurrentDevSelect");
        const propDevSelect = document.getElementById("simProposedDevSelect");

        mSelect.innerHTML = "";
        allMilestonesCache.forEach(m => {
            const opt = document.createElement("option");
            opt.value = m.id;
            opt.text = `${m.projectName} — ${m.name}`;
            mSelect.appendChild(opt);
        });

        const populateDevs = () => {
            const selectedMilestoneId = parseInt(mSelect.value);
            const selectedMilestone = allMilestonesCache.find(m => m.id === selectedMilestoneId);

            curDevSelect.innerHTML = "";
            propDevSelect.innerHTML = "";

            allDevelopersCache.forEach(d => {
                const devSkills = parseSkillsList(d.skills);
                const skillsText = devSkills.length > 0 ? ` - ${devSkills.join(', ')}` : '';

                const opt1 = document.createElement("option");
                opt1.value = d.id;
                opt1.text = `${d.name} (₹${d.hourlyCost}/hr${skillsText})`;
                curDevSelect.appendChild(opt1);

                const opt2 = document.createElement("option");
                opt2.value = d.id;
                opt2.text = `${d.name} (₹${d.hourlyCost}/hr${skillsText})`;
                propDevSelect.appendChild(opt2);
            });

            if (selectedMilestone && selectedMilestone.allocations && selectedMilestone.allocations.length > 0) {
                curDevSelect.value = selectedMilestone.allocations[0].developerId;
                document.getElementById("simAllocPctInput").value = selectedMilestone.allocations[0].allocationPercentage;
            }
            if (allDevelopersCache.length > 1) {
                propDevSelect.value = allDevelopersCache.find(d => d.id !== parseInt(curDevSelect.value))?.id || allDevelopersCache[1].id;
            }
        };

        mSelect.onchange = populateDevs;
        populateDevs();

    } catch (err) {
        console.error("Error setting up simulator:", err);
    }
}

function initSimulatorControls() {
    const btnRun = document.getElementById("btnRunSimulation");
    if (btnRun) btnRun.addEventListener("click", runSimulation);

    const btnApply = document.getElementById("btnApplySimulation");
    if (btnApply) btnApply.addEventListener("click", applySimulationSwap);
}

async function runSimulation() {
    const milestoneId = parseInt(document.getElementById("simMilestoneSelect").value);
    const currentDevId = parseInt(document.getElementById("simCurrentDevSelect").value);
    const proposedDevId = parseInt(document.getElementById("simProposedDevSelect").value);
    const allocPct = parseInt(document.getElementById("simAllocPctInput").value);

    if (currentDevId === proposedDevId) {
        showToast("Please select two different developers to simulate a swap.", "warning");
        return;
    }

    try {
        const res = await fetch("/api/simulations", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
                milestoneId: milestoneId,
                currentDeveloperId: currentDevId,
                proposedDeveloperId: proposedDevId,
                allocationPercentage: allocPct
            })
        });

        const json = await res.json();
        if (json.status !== "SUCCESS") {
            showToast(json.message, "error");
            return;
        }

        const data = json.data;
        lastSimulationResult = data;
        renderSimulationResults(data);
        showToast(`Simulation computed: ${data.scenarioVerdict} verdict. Real DB is unmutated.`, data.scenarioVerdict === 'OPTIMAL' ? 'success' : 'warning');
    } catch (err) {
        console.error("Simulation failed:", err);
        showToast("Simulation failed to execute.", "error");
    }
}

function renderSimulationResults(data) {
    document.getElementById("simulationResultsContainer").style.display = "block";
    const btnPrint = document.getElementById("btnPrintSimulationReport");
    const btnPdf = document.getElementById("btnExportSimulationPdf");
    if (btnPrint) btnPrint.style.display = "inline-flex";
    if (btnPdf) btnPdf.style.display = "inline-flex";

    // Verdict Banner
    const banner = document.getElementById("simVerdictBanner");
    const verdictText = document.getElementById("simVerdictText");
    banner.className = `sim-verdict-banner ${data.scenarioVerdict.toLowerCase()}`;
    verdictText.innerHTML = `<strong>${data.scenarioVerdict} OUTCOME:</strong> ${data.recommendation}`;

    // Current State
    document.getElementById("simCurrentDevName").innerText = data.currentDeveloperName;
    document.getElementById("simCurrentHourlyCost").innerText = `₹${data.currentCostPerHour}/hr`;
    document.getElementById("simCurrentTotalCost").innerText = `₹${formatNumber(data.currentMilestoneCost)}`;
    document.getElementById("simCurrentDeliveryDate").innerText = data.currentDeliveryDate;
    document.getElementById("simCurrentSkillMatch").innerText = `${data.currentSkillMatchPercentage}% matched`;

    // Simulated State
    document.getElementById("simProposedDevName").innerText = data.proposedDeveloperName;
    document.getElementById("simSimulatedHourlyCost").innerText = `₹${data.simulatedCostPerHour}/hr`;
    document.getElementById("simSimulatedTotalCost").innerText = `₹${formatNumber(data.simulatedMilestoneCost)}`;
    document.getElementById("simSimulatedDeliveryDate").innerText = `${data.simulatedDeliveryDate} (${data.deliveryDaysDelta >= 0 ? '+' : ''}${data.deliveryDaysDelta} days)`;
    document.getElementById("simSimulatedSkillMatch").innerText = `${data.simulatedSkillMatchPercentage}% matched`;

    // Impact KPI values
    const deltaHr = data.costDifferencePerHour;
    document.getElementById("simDeltaCost").innerText = `${deltaHr >= 0 ? '+₹' : '-₹'}${Math.abs(deltaHr)}/hr`;
    document.getElementById("simDeltaCost").style.color = deltaHr > 0 ? "var(--warning)" : "var(--success)";
    document.getElementById("simDeltaCostSub").innerText = `Milestone Total Delta: ${data.milestoneCostDifference >= 0 ? '+₹' : '-₹'}${formatNumber(Math.abs(data.milestoneCostDifference))}`;

    const deltaDays = data.deliveryDaysDelta;
    document.getElementById("simDeltaDelivery").innerText = `${deltaDays >= 0 ? '+' : ''}${deltaDays} Days`;
    document.getElementById("simDeltaDelivery").style.color = deltaDays > 0 ? "var(--critical)" : "var(--success)";
    document.getElementById("simDeltaDeliverySub").innerText = deltaDays === 0 ? "On schedule" : (deltaDays > 0 ? "Delay introduced" : "Delivery expedited");

    document.getElementById("simSkillImpactVal").innerText = `${data.simulatedSkillMatchPercentage}%`;
    document.getElementById("simSkillImpactSub").innerText = `${data.simulatedSkillMatchPercentage}% requirement match`;
}

async function exportSimulationReport() {
    if (!lastSimulationResult || !lastSimulationResult.id) {
        showToast("Please run a simulation first before exporting the report.", "warning");
        return;
    }

    try {
        showToast("Generating PDF report...", "info");
        const res = await fetch(`/api/simulations/${lastSimulationResult.id}/report`);
        if (!res.ok) {
            showToast("Failed to generate PDF report from server.", "error");
            return;
        }

        const blob = await res.blob();
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement("a");
        a.href = url;
        a.download = `simulation-report-${lastSimulationResult.id}.pdf`;
        document.body.appendChild(a);
        a.click();
        a.remove();
        window.URL.revokeObjectURL(url);
        showToast("PDF report downloaded successfully!", "success");
    } catch (err) {
        console.error("PDF export error:", err);
        showToast("Error downloading PDF report.", "error");
    }
}

async function applySimulationSwap() {
    if (!lastSimulationResult) return;
    if (!confirm("Are you sure you want to commit this swap to the real database?")) return;

    try {
        const res = await fetch(`/api/simulations/${lastSimulationResult.id}/apply`, {
            method: "POST"
        });
        const json = await res.json();
        if (json.status === "SUCCESS") {
            showToast("Swap successfully applied to real project schedule!", "success");
            loadProjectsManager();
            loadResourceDirectory();
            loadTimeline();
            loadHeatmap();
            loadConflictCenter();
            loadPmDashboard();
        } else {
            showToast(json.message || "Failed to apply swap.", "error");
        }
    } catch (err) {
        showToast("Error committing simulation to database.", "error");
    }
}

function openWhatIfForProject(projectId) {
    switchTab("viewSimulator");
}

function openWhatIfForMilestone(milestoneId) {
    switchTab("viewSimulator");
    const mSelect = document.getElementById("simMilestoneSelect");
    if (mSelect) {
        mSelect.value = milestoneId;
        mSelect.dispatchEvent(new Event("change"));
    }
}

// ==========================================================================
// VIEW 9: DEVELOPER PORTAL
// ==========================================================================
async function loadDeveloperDashboard(devId) {
    try {
        const res = await fetch(`/api/dashboard/developer/${devId}`);
        const json = await res.json();
        if (json.status !== "SUCCESS") return;

        const data = json.data;

        const nameEl = document.getElementById("devPortalName");
        if (nameEl) nameEl.innerText = data.name;

        const devSkills = parseSkillsList(data.skills);
        const skillsLabel = devSkills.length > 0 ? ` • Skills: ${devSkills.join(', ')}` : '';
        const roleEl = document.getElementById("devPortalRoleTitle");
        if (roleEl) roleEl.innerText = `${data.roleTitle || 'Developer'}${skillsLabel}`;

        const emailEl = document.getElementById("devPortalEmail");
        if (emailEl) emailEl.innerText = data.email;

        const rateEl = document.getElementById("devPortalRate");
        if (rateEl) rateEl.innerText = `₹${data.hourlyCost} / hr`;

        const allocEl = document.getElementById("devPortalAllocVal");
        if (allocEl) {
            allocEl.innerText = `${data.currentAllocationPercentage}%`;
            allocEl.style.color = data.currentAllocationPercentage > 100 ? "var(--critical)" : "var(--text-main)";
        }
        const remEl = document.getElementById("devPortalRemainingVal");
        if (remEl) remEl.innerText = `${data.remainingCapacityPercentage}%`;
        const msCountEl = document.getElementById("devPortalMilestonesCount");
        if (msCountEl) msCountEl.innerText = data.assignedMilestones ? data.assignedMilestones.length : 0;

        const mList = document.getElementById("devPortalMilestonesList");
        if (mList) {
            mList.innerHTML = "";
            if (data.assignedMilestones && data.assignedMilestones.length > 0) {
                data.assignedMilestones.forEach(m => {
                    const item = document.createElement("div");
                    item.className = "timeline-milestone-row";
                    item.style.gridTemplateColumns = "180px 140px 1fr 100px";
                    item.innerHTML = `
                        <div><strong>${escapeHtml(m.name)}</strong><br><small style="color: var(--text-muted);">${escapeHtml(m.projectName)}</small></div>
                        <div style="color: var(--text-muted);">${m.startDate} → ${m.endDate}</div>
                        <div>
                            <div class="progress-bar-container">
                                <div class="progress-bar-fill" style="width: ${m.progressPercentage}%;"></div>
                            </div>
                        </div>
                        <div><span class="badge badge-${m.status === 'COMPLETED' ? 'success' : 'info'}">${m.status}</span></div>
                    `;
                    mList.appendChild(item);
                });
            } else {
                mList.innerHTML = "<p style='color: var(--text-muted); padding: 1rem 0;'>No active milestones assigned to you.</p>";
            }
        }

        // Trigger Sub-Module Loaders for Employee Portal
        loadProfileCompleteness(devId);
        loadWorkScheduleCalendar(devId);
        loadEmployeeSkills(devId);
        loadCertificatesVault(devId);
        loadEmployeeLeaves(devId);
        loadPersonalAnalytics(devId);
    } catch (err) {
        console.error("Error loading developer portal:", err);
    }
}

// ==========================================================================
// NOTIFICATIONS DRAWER
// ==========================================================================
function initNotificationDrawer() {
    const bell = document.getElementById("btnToggleNotif");
    const drawer = document.getElementById("notifDrawer");
    bell.addEventListener("click", (e) => {
        e.stopPropagation();
        drawer.classList.toggle("active");
    });

    document.addEventListener("click", (e) => {
        if (!drawer.contains(e.target) && !bell.contains(e.target)) {
            drawer.classList.remove("active");
        }
    });

    document.getElementById("btnMarkAllRead").addEventListener("click", async () => {
        await fetch("/api/notifications/read-all", { method: "PUT" });
        fetchNotifications();
        showToast("All notifications marked as read.", "info");
    });
}

async function fetchNotifications() {
    try {
        const res = await fetch("/api/notifications");
        const json = await res.json();
        if (json.status !== "SUCCESS") return;

        const notifs = json.data || [];
        const unreadCount = notifs.filter(n => !n.read).length;

        document.getElementById("notifCountBadge").innerText = unreadCount;
        document.getElementById("notifCountBadge").style.display = unreadCount > 0 ? "block" : "none";

        const container = document.getElementById("notifListContainer");
        container.innerHTML = "";

        if (notifs.length === 0) {
            container.innerHTML = "<div style='padding: 1rem; text-align: center; color: var(--text-muted);'>No notifications.</div>";
            return;
        }

        notifs.forEach(n => {
            const card = document.createElement("div");
            card.className = `notif-card ${!n.read ? 'unread' : ''}`;
            card.innerHTML = `
                <div style="display: flex; justify-content: space-between; margin-bottom: 2px;">
                    <span class="badge badge-${n.severity ? n.severity.toLowerCase() : 'info'}">${n.severity || 'INFO'}</span>
                    <small style="color: var(--text-muted);">${n.createdAt ? formatTime(n.createdAt) : ''}</small>
                </div>
                <div><strong>${n.title || 'Alert'}</strong></div>
                <div style="font-size: 0.78rem; color: #334155; margin-top: 2px;">${n.message}</div>
            `;
            card.onclick = async () => {
                if (!n.read) {
                    await fetch(`/api/notifications/${n.id}/read`, { method: "PUT" });
                    fetchNotifications();
                }
            };
            container.appendChild(card);
        });
    } catch (err) {
        console.error("Error loading notifications:", err);
    }
}

// ==========================================================================
// VIEW 13: Dedicated Notifications Page View
// ==========================================================================
async function loadNotificationsPage() {
    try {
        const res = await fetch("/api/notifications");
        const json = await res.json();
        if (json.status !== "SUCCESS") return;

        allNotificationsCache = json.data || [];
        renderNotificationsPage();

        const btnMarkAll = document.getElementById("btnPageMarkAllRead");
        if (btnMarkAll) {
            btnMarkAll.onclick = async () => {
                await fetch("/api/notifications/read-all", { method: "PUT" });
                fetchNotifications();
                loadNotificationsPage();
                showToast("All notifications marked as read.", "info");
            };
        }
    } catch (err) {
        console.error("Error loading notifications page:", err);
    }
}

function filterNotifPage(severity) {
    currentNotifPageFilter = severity;
    const btnContainer = document.getElementById("notifPageFilterButtons");
    if (btnContainer) {
        btnContainer.querySelectorAll("button").forEach(b => b.classList.remove("active"));
        const targetBtn = Array.from(btnContainer.querySelectorAll("button")).find(b => b.getAttribute("onclick")?.includes(`'${severity}'`));
        if (targetBtn) targetBtn.classList.add("active");
    }
    renderNotificationsPage();
}

function renderNotificationsPage() {
    const container = document.getElementById("fullNotificationsList");
    if (!container) return;
    container.innerHTML = "";

    const filtered = allNotificationsCache.filter(n => {
        if (currentNotifPageFilter === "ALL") return true;
        return (n.severity || "INFO").toUpperCase() === currentNotifPageFilter;
    });

    if (filtered.length === 0) {
        container.innerHTML = "<div style='padding: 2.5rem; text-align: center; color: var(--text-muted);'>No notifications match the filter.</div>";
        return;
    }

    filtered.forEach(n => {
        const card = document.createElement("div");
        card.className = `notif-card ${n.severity ? n.severity.toLowerCase() : 'info'} ${!n.read ? 'unread' : ''}`;
        card.innerHTML = `
            <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 4px;">
                <div style="display: flex; align-items: center; gap: 8px;">
                    <span class="badge badge-${n.severity ? n.severity.toLowerCase() : 'info'}">${n.severity || 'INFO'}</span>
                    <strong style="font-size: 0.95rem;">${n.title || 'System Alert'}</strong>
                </div>
                <small style="color: var(--text-muted);">${n.createdAt ? formatTime(n.createdAt) : ''}</small>
            </div>
            <div style="font-size: 0.85rem; color: #334155; margin-top: 4px;">${n.message}</div>
            <div style="margin-top: 6px; display: flex; justify-content: flex-end;">
                ${!n.read ? `<button class="btn btn-sm btn-outline" onclick="markNotificationRead(${n.id})">Mark as Read</button>` : '<span style="font-size: 0.75rem; color: var(--text-muted);">Read</span>'}
            </div>
        `;
        container.appendChild(card);
    });
}

async function markNotificationRead(id) {
    await fetch(`/api/notifications/${id}/read`, { method: "PUT" });
    fetchNotifications();
    loadNotificationsPage();
}

// ==========================================================================
// MODALS LOGIC
// ==========================================================================
function initModals() {
    document.getElementById("btnOpenAddProjectModal")?.addEventListener("click", () => openModal("modalProject"));
    document.getElementById("btnOpenAddProjectModal2")?.addEventListener("click", () => openModal("modalProject"));
    document.getElementById("btnOpenAddMilestoneModal")?.addEventListener("click", () => openAddMilestoneModal());
    document.getElementById("btnOpenAssignModal")?.addEventListener("click", () => openAssignResourceModal());
    document.getElementById("btnOpenAddLeaveModal")?.addEventListener("click", () => openLeaveModal());

    // Form Submissions
    document.getElementById("formLoginManager")?.addEventListener("submit", handleManagerLogin);
    document.getElementById("formLoginDeveloper")?.addEventListener("submit", handleDeveloperLogin);
    document.getElementById("formLoginEmployee")?.addEventListener("submit", handleDeveloperLogin);
    document.getElementById("formCreateProject")?.addEventListener("submit", handleCreateProject);
    document.getElementById("formCreateMilestone")?.addEventListener("submit", handleCreateMilestone);
    document.getElementById("formAssignResource")?.addEventListener("submit", handleAssignResource);
    document.getElementById("formScheduleLeave")?.addEventListener("submit", handleScheduleLeave);

    // New Employee Portal Form Submissions
    document.getElementById("formForgotPassword")?.addEventListener("submit", handleForgotPassword);
    document.getElementById("formResetPassword")?.addEventListener("submit", handleResetPassword);
    document.getElementById("formOnboardingWizard")?.addEventListener("submit", handleOnboardingSubmit);
    document.getElementById("formEmployeeLeaveRequest")?.addEventListener("submit", handleEmployeeLeaveSubmit);
    document.getElementById("formPostTaskFeedback")?.addEventListener("submit", handlePostTaskFeedbackSubmit);
    document.getElementById("formAddManualSkill")?.addEventListener("submit", handleManualAddSkill);
    document.getElementById("formConfirmCertificate")?.addEventListener("submit", confirmSaveCertificate);
}

function openModal(id) {
    const el = document.getElementById(id);
    if (el) el.classList.add("active");
}

function closeModal(id) {
    const el = document.getElementById(id);
    if (el) el.classList.remove("active");
}

async function handleManagerLogin(e) {
    if (e) e.preventDefault();
    const email = document.getElementById("loginEmail").value.trim();
    const password = document.getElementById("loginPassword").value.trim();

    try {
        const res = await fetch("/api/auth/login", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ email: email, password: password })
        });
        const json = await res.json();
        if (json.status === "SUCCESS") {
            closeModal("modalLogin");
            const user = json.data;

            if (user.token) {
                localStorage.setItem("emp_jwt_token", user.token);
                if (user.refreshToken) localStorage.setItem("emp_refresh_token", user.refreshToken);
            }

            const pmTabs = document.getElementById("pmNavTabs");
            const empTabs = document.getElementById("employeeNavTabs");

            if (user.role === "EMPLOYEE") {
                currentRole = "EMPLOYEE";
                currentDevId = user.developerId || 1;

                const userStatus = document.getElementById("userStatusText");
                if (userStatus) userStatus.innerHTML = `👤 Employee: ${user.name} (ID #${currentDevId})`;

                if (pmTabs) pmTabs.style.display = "none";
                if (empTabs) empTabs.style.display = "flex";

                switchTab("viewDeveloperPortal");
                loadDeveloperDashboard(currentDevId);
                showToast(`Welcome, ${user.name}! Signed in to Employee Portal.`, "success");

                if (user.profileCompleted === false) {
                    setTimeout(() => openOnboardingWizard(), 600);
                }
            } else {
                currentRole = user.role || "PM";
                const userStatus = document.getElementById("userStatusText");
                if (userStatus) userStatus.innerHTML = `👔 ${user.role || 'Project Manager'}: ${user.name}`;

                if (pmTabs) pmTabs.style.display = "flex";
                if (empTabs) empTabs.style.display = "none";

                switchTab("viewDashboard");
                loadPmDashboard();
                showToast(`Welcome, ${user.name}! Signed in as ${user.role || 'Project Manager'}.`, "success");
            }
        } else {
            showToast(json.message || "Invalid credentials.", "error");
        }
    } catch (err) {
        showToast("Error signing in. Check connection.", "error");
    }
}

async function handleDeveloperLogin(e) {
    if (e) e.preventDefault();
    const devIdInput = document.getElementById("loginDeveloperId") || document.getElementById("loginEmployeeId");
    const devId = parseInt(devIdInput ? devIdInput.value : 1);
    loginAsDeveloper(devId);
}

function handleEmployeeLogin(e) {
    handleDeveloperLogin(e);
}

async function loginAsDeveloper(devId) {
    if (!devId || isNaN(devId) || devId <= 0) {
        showToast("Please enter a valid Developer ID number (e.g. 1 to 5)", "warning");
        return;
    }

    try {
        const res = await fetch(`/api/dashboard/developer/${devId}`);
        const json = await res.json();
        if (json.status === "SUCCESS" && json.data) {
            closeModal("modalLogin");
            const emp = json.data;
            currentRole = "EMPLOYEE";
            currentDevId = devId;

            const userStatus = document.getElementById("userStatusText");
            if (userStatus) userStatus.innerHTML = `👤 Employee: ${emp.name} (ID #${devId})`;

            const pmTabs = document.getElementById("pmNavTabs");
            if (pmTabs) pmTabs.style.display = "none";

            const empTabs = document.getElementById("employeeNavTabs");
            if (empTabs) empTabs.style.display = "flex";

            switchTab("viewDeveloperPortal");
            loadDeveloperDashboard(devId);
            showToast(`Welcome, ${emp.name}! Employee Portal opened.`, "success");
        } else {
            showToast(`Developer ID #${devId} not found. Please enter a valid ID (1 to 5).`, "error");
        }
    } catch (err) {
        showToast("Error connecting to server. Please ensure backend is running.", "error");
    }
}

function loginAsEmployee(devId) {
    loginAsDeveloper(devId);
}

async function openAddMilestoneModal(projectId = null) {
    const pSelect = document.getElementById("modalMilestoneProjectSelect");
    pSelect.innerHTML = "";
    allProjectsCache.forEach(p => {
        const opt = document.createElement("option");
        opt.value = p.id;
        opt.text = p.name;
        if (projectId && p.id === projectId) opt.selected = true;
        pSelect.appendChild(opt);
    });

    const sSelect = document.getElementById("modalMilestoneSkillsSelect");
    sSelect.innerHTML = "";
    const res = await fetch("/api/skills");
    const json = await res.json();
    if (json.status === "SUCCESS") {
        json.data.forEach(s => {
            const opt = document.createElement("option");
            opt.value = s.id;
            opt.text = s.name;
            sSelect.appendChild(opt);
        });
    }

    openModal("modalMilestone");
}

function openAddMilestoneForProject(projectId) {
    openAddMilestoneModal(projectId);
}

async function openAssignResourceModal(milestoneId = null, devId = null) {
    const mSelect = document.getElementById("modalAssignMilestoneSelect");
    const dSelect = document.getElementById("modalAssignDevSelect");
    mSelect.innerHTML = "";
    dSelect.innerHTML = "";

    const [resM, resD] = await Promise.all([
        fetch("/api/milestones"),
        fetch("/api/developers")
    ]);
    const jsonM = await resM.json();
    const jsonD = await resD.json();

    allMilestonesCache = jsonM.data || [];
    allDevelopersCache = jsonD.data || [];

    allMilestonesCache.forEach(m => {
        const opt = document.createElement("option");
        opt.value = m.id;
        opt.text = `${m.projectName} — ${m.name} (${m.startDate} to ${m.endDate})`;
        if (milestoneId && m.id === milestoneId) opt.selected = true;
        mSelect.appendChild(opt);
    });

    allDevelopersCache.forEach(d => {
        const opt = document.createElement("option");
        opt.value = d.id;
        opt.text = `${d.name} (${d.roleTitle || 'Dev'} - Remaining: ${d.remainingCapacity}%)`;
        if (devId && d.id === devId) opt.selected = true;
        dSelect.appendChild(opt);
    });

    updateAssignCapacityPreview();
    openModal("modalAssign");
}

function openAssignModalForMilestone(milestoneId) {
    openAssignResourceModal(milestoneId, null);
}

function openAssignForDev(devId) {
    openAssignResourceModal(null, devId);
}

async function openLeaveModal(defaultDevId = null) {
    const dSelect = document.getElementById("modalLeaveDevSelect");
    dSelect.innerHTML = "";

    const res = await fetch("/api/developers");
    const json = await res.json();
    if (json.status === "SUCCESS") {
        json.data.forEach(d => {
            const opt = document.createElement("option");
            opt.value = d.id;
            opt.text = `${d.name} (${d.roleTitle || 'Developer'})`;
            if (defaultDevId && d.id === defaultDevId) opt.selected = true;
            dSelect.appendChild(opt);
        });
    }

    openModal("modalLeave");
}

function initAssignCapacityCalculator() {
    const slider = document.getElementById("modalAssignPctSlider");
    const numInput = document.getElementById("modalAssignPctInput");
    const label = document.getElementById("modalAssignPctLabel");
    const devSelect = document.getElementById("modalAssignDevSelect");
    const mSelect = document.getElementById("modalAssignMilestoneSelect");

    if (slider && numInput) {
        slider.addEventListener("input", (e) => {
            numInput.value = e.target.value;
            if (label) label.innerText = `${e.target.value}%`;
            updateAssignCapacityPreview();
        });

        numInput.addEventListener("input", (e) => {
            let val = parseInt(e.target.value) || 0;
            if (val > 100) val = 100;
            if (val < 1) val = 1;
            slider.value = val;
            if (label) label.innerText = `${val}%`;
            updateAssignCapacityPreview();
        });
    }

    if (devSelect) devSelect.addEventListener("change", updateAssignCapacityPreview);
    if (mSelect) mSelect.addEventListener("change", updateAssignCapacityPreview);
}

function updateAssignCapacityPreview() {
    const devSelect = document.getElementById("modalAssignDevSelect");
    const mSelect = document.getElementById("modalAssignMilestoneSelect");
    const numInput = document.getElementById("modalAssignPctInput");
    const preview = document.getElementById("modalDevCapacityPreview");

    if (!devSelect || !preview) return;

    const devId = parseInt(devSelect.value);
    const mId = mSelect ? parseInt(mSelect.value) : null;
    const requestedPct = parseInt(numInput?.value || 50);

    const dev = allDevelopersCache.find(d => d.id === devId);
    const milestone = allMilestonesCache.find(m => m.id === mId);

    if (!dev) {
        preview.innerHTML = "Select a developer to view live capacity impact.";
        return;
    }

    const currentAlloc = dev.currentAllocation || 0;
    const projectedAlloc = currentAlloc + requestedPct;
    const isOverAllocated = projectedAlloc > 100;

    let skillWarning = "";
    const mSkills = parseSkillsList(milestone?.requiredSkillNames || milestone?.requiredSkills);
    if (milestone && mSkills.length > 0) {
        const devSkills = parseSkillsList(dev.skills);
        const missingSkills = mSkills.filter(s => !devSkills.includes(s));
        if (missingSkills.length > 0) {
            skillWarning = `<div style="margin-top: 6px; color: #b45309; font-weight: 600;">⚠️ Skill Gap Alert: ${escapeHtml(dev.name)} lacks required skill(s): ${escapeHtml(missingSkills.join(', '))}</div>`;
        }
    }

    preview.innerHTML = `
        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 6px;">
            <span><strong>${dev.name}</strong> (${dev.roleTitle || 'Developer'})</span>
            <span class="badge badge-${isOverAllocated ? 'critical' : (projectedAlloc > 85 ? 'warning' : 'success')}">
                ${isOverAllocated ? '⚠️ OVER-ALLOCATION RISK' : '✅ SAFE CAPACITY'}
            </span>
        </div>
        <div>
            Current Allocation: <strong>${currentAlloc}%</strong> | Requested: <strong>+${requestedPct}%</strong>
            <br>
            Projected Total: <strong style="color: ${isOverAllocated ? 'var(--critical)' : 'var(--primary)'}; font-size: 0.95rem;">${projectedAlloc}%</strong> (Available: ${dev.availabilityPercentage}%)
        </div>
        ${skillWarning}
    `;
}

function initFiltersAndSearch() {
    const projSearch = document.getElementById("projectSearchInput");
    const projFilter = document.getElementById("projectStatusFilter");
    if (projSearch) projSearch.addEventListener("input", applyProjectsFilter);
    if (projFilter) projFilter.addEventListener("change", applyProjectsFilter);

    const devSearch = document.getElementById("devSearchInput");
    const devSkill = document.getElementById("devSkillFilter");
    const devSort = document.getElementById("devSortSelect");
    if (devSearch) devSearch.addEventListener("input", applyDevelopersFilter);
    if (devSkill) devSkill.addEventListener("change", applyDevelopersFilter);
    if (devSort) devSort.addEventListener("change", applyDevelopersFilter);

    const confSearch = document.getElementById("conflictSearchInput");
    if (confSearch) confSearch.addEventListener("input", applyConflictsFilter);
}

// ==========================================================================
// Modal Form Submissions with Toast Feedback
// ==========================================================================
async function handleCreateProject(e) {
    e.preventDefault();
    const name = document.getElementById("modalProjectName").value;
    const desc = document.getElementById("modalProjectDesc").value;
    const start = document.getElementById("modalProjectStart").value;
    const end = document.getElementById("modalProjectEnd").value;
    const budget = parseFloat(document.getElementById("modalProjectBudget").value);

    try {
        const res = await fetch("/api/projects", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
                name: name,
                description: desc,
                startDate: start,
                endDate: end,
                totalBudget: budget,
                status: "ACTIVE"
            })
        });
        const json = await res.json();
        if (json.status === "SUCCESS") {
            closeModal("modalProject");
            loadProjectsManager();
            loadPmDashboard();
            showToast(`Project '${name}' created successfully!`, "success");
        } else {
            showToast(json.message || "Error creating project.", "error");
        }
    } catch (err) {
        showToast("Network error creating project.", "error");
    }
}

async function handleCreateMilestone(e) {
    e.preventDefault();
    const projectId = parseInt(document.getElementById("modalMilestoneProjectSelect").value);
    const name = document.getElementById("modalMilestoneName").value;
    const desc = document.getElementById("modalMilestoneDesc").value;
    const start = document.getElementById("modalMilestoneStart").value;
    const end = document.getElementById("modalMilestoneEnd").value;
    const effort = parseFloat(document.getElementById("modalMilestoneEffort").value);

    const sSelect = document.getElementById("modalMilestoneSkillsSelect");
    const skillIds = Array.from(sSelect.selectedOptions).map(o => parseInt(o.value));

    try {
        const res = await fetch(`/api/milestones`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
                projectId: projectId,
                name: name,
                description: desc,
                startDate: start,
                endDate: end,
                estimatedEffortHours: effort,
                requiredSkillIds: skillIds,
                status: "IN_PROGRESS",
                progressPercentage: 0
            })
        });
        const json = await res.json();
        if (json.status === "SUCCESS") {
            closeModal("modalMilestone");
            loadProjectsManager();
            showToast(`Milestone '${name}' created successfully!`, "success");
        } else {
            showToast(json.message || "Error adding milestone.", "error");
        }
    } catch (err) {
        showToast("Network error adding milestone.", "error");
    }
}

async function handleAssignResource(e) {
    e.preventDefault();
    const milestoneId = parseInt(document.getElementById("modalAssignMilestoneSelect").value);
    const devId = parseInt(document.getElementById("modalAssignDevSelect").value);
    const pct = parseInt(document.getElementById("modalAssignPctInput").value);

    try {
        const res = await fetch("/api/allocations", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
                milestoneId: milestoneId,
                developerId: devId,
                allocationPercentage: pct
            })
        });
        const json = await res.json();
        if (json.status === "SUCCESS") {
            closeModal("modalAssign");
            loadProjectsManager();
            loadResourceDirectory();
            loadTimeline();
            loadHeatmap();
            loadConflictCenter();
            loadPmDashboard();
            fetchNotifications();
            showToast("Resource allocation committed! Conflict engine updated.", "success");
        } else {
            showToast(json.message || "Error allocating resource.", "error");
        }
    } catch (err) {
        showToast("Network error allocating resource.", "error");
    }
}

async function removeAllocation(id) {
    if (!confirm("Are you sure you want to remove this allocation?")) return;
    try {
        const res = await fetch(`/api/allocations/${id}`, { method: "DELETE" });
        const json = await res.json();
        if (json.status === "SUCCESS") {
            loadProjectsManager();
            loadResourceDirectory();
            loadTimeline();
            loadHeatmap();
            loadConflictCenter();
            loadPmDashboard();
            fetchNotifications();
            showToast("Allocation removed successfully.", "info");
        }
    } catch (err) {
        showToast("Error removing allocation.", "error");
    }
}

async function handleScheduleLeave(e) {
    e.preventDefault();
    const devId = parseInt(document.getElementById("modalLeaveDevSelect").value);
    const start = document.getElementById("modalLeaveStart").value;
    const end = document.getElementById("modalLeaveEnd").value;
    const reason = document.getElementById("modalLeaveReason").value;

    try {
        const res = await fetch("/api/availability", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
                developerId: devId,
                startDate: start,
                endDate: end,
                reason: reason,
                status: "APPROVED"
            })
        });
        const json = await res.json();
        if (json.status === "SUCCESS") {
            closeModal("modalLeave");
            loadLeavesManager();
            loadConflictCenter();
            loadPmDashboard();
            if (currentRole === "DEVELOPER") {
                loadDeveloperDashboard(currentDevId);
            }
            fetchNotifications();
            showToast("Planned leave scheduled. Capacity evaluated.", "success");
        } else {
            showToast(json.message || "Error scheduling leave.", "error");
        }
    } catch (err) {
        showToast("Network error scheduling leave.", "error");
    }
}

async function cancelLeave(id) {
    if (!confirm("Are you sure you want to cancel this planned leave?")) return;
    try {
        const res = await fetch(`/api/availability/${id}`, { method: "DELETE" });
        const json = await res.json();
        if (json.status === "SUCCESS") {
            loadLeavesManager();
            loadConflictCenter();
            loadPmDashboard();
            fetchNotifications();
            showToast("Planned leave cancelled.", "info");
        }
    } catch (err) {
        showToast("Error cancelling leave.", "error");
    }
}

// ==========================================================================
// Viva Demo Scenarios Shortcuts
// ==========================================================================
function demoScenario(type) {
    if (type === "OVER_ALLOCATION") {
        switchTab("viewConflicts");
        const sInput = document.getElementById("conflictSearchInput");
        if (sInput) {
            sInput.value = "Priya";
            applyConflictsFilter();
        }
        showToast("🎓 Scenario 1: Priya Sharma allocated to 110% across active milestones (Critical Conflict)", "warning");
    } else if (type === "LEAVE_CONFLICT") {
        switchTab("viewLeaves");
        showToast("🎓 Scenario 2: Rahul Verma scheduled 5-day leave during active milestone assignment!", "warning");
    } else if (type === "SKILL_GAP") {
        switchTab("viewConflicts");
        const sInput = document.getElementById("conflictSearchInput");
        if (sInput) {
            sInput.value = "Python";
            applyConflictsFilter();
        }
        showToast("🎓 Scenario 3: Milestone requires Python not possessed by assignee Arun Kumar!", "info");
    } else if (type === "BUDGET_BURN") {
        switchTab("viewDashboard");
        const bSel = document.getElementById("budgetProjectSelect");
        if (bSel && bSel.options.length > 0) {
            bSel.selectedIndex = 0;
            bSel.dispatchEvent(new Event("change"));
        }
        window.scrollTo({ top: 250, behavior: 'smooth' });
        showToast("🎓 Scenario 4: Automated daily burn-rate and exhaustion date forecasting!", "info");
    } else if (type === "SIMULATION_SWAP") {
        switchTab("viewSimulator");
        setTimeout(() => {
            const mSelect = document.getElementById("simMilestoneSelect");
            if (mSelect && mSelect.options.length > 0) {
                mSelect.selectedIndex = 0;
                mSelect.dispatchEvent(new Event("change"));
            }
            runSimulation();
            showToast("🎓 Scenario 5: What-If Swap Simulation executed in-memory without DB mutation!", "success");
        }, 400);
    }
}

// ==========================================================================
// UTILITIES
// ==========================================================================
function showToast(message, type = "info") {
    const container = document.getElementById("toastContainer");
    if (!container) return;
    const toast = document.createElement("div");
    toast.className = `toast toast-${type}`;
    const icon = type === 'success' ? '✅' : (type === 'error' || type === 'critical' ? '❌' : (type === 'warning' ? '⚠️' : 'ℹ️'));
    toast.innerHTML = `
        <span style="font-size: 1.1rem; line-height: 1;">${icon}</span>
        <div style="flex: 1; font-weight: 500; font-size: 0.85rem; line-height: 1.3;">${message}</div>
        <button style="background: none; border: none; font-size: 1.2rem; cursor: pointer; color: inherit; opacity: 0.6; line-height: 1;" onclick="this.parentElement.remove()">&times;</button>
    `;
    container.appendChild(toast);
    setTimeout(() => {
        if (toast.parentElement) {
            toast.style.animation = "slideIn 0.3s ease reverse forwards";
            setTimeout(() => toast.remove(), 300);
        }
    }, 4200);
}

function formatNumber(num) {
    if (num == null) return "0";
    return num.toLocaleString('en-IN', { maximumFractionDigits: 2 });
}

function calculateDays(start, end) {
    const d1 = new Date(start);
    const d2 = new Date(end);
    const diff = Math.abs(d2 - d1);
    return Math.ceil(diff / (1000 * 60 * 60 * 24)) + 1;
}

function formatTime(isoString) {
    if (!isoString) return "";
    const d = new Date(isoString);
    return d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
}

// ==========================================================================
// EMPLOYEE PORTAL SUB-VIEW NAVIGATION
// ==========================================================================
function switchEmpSubTab(tabId) {
    document.querySelectorAll(".emp-subview").forEach(el => el.style.display = "none");
    const activeSub = document.getElementById(tabId);
    if (activeSub) activeSub.style.display = "block";

    document.querySelectorAll(".employee-nav-tabs .subtab-btn").forEach(btn => btn.classList.remove("active"));
    const activeBtn = Array.from(document.querySelectorAll(".employee-nav-tabs .subtab-btn")).find(b => b.getAttribute("onclick")?.includes(tabId));
    if (activeBtn) activeBtn.classList.add("active");

    if (tabId === "empViewCalendar") loadWorkScheduleCalendar(currentDevId);
    if (tabId === "empViewMySkills") loadEmployeeSkills(currentDevId);
    if (tabId === "empViewDocuments") loadCertificatesVault(currentDevId);
    if (tabId === "empViewLeaves") loadEmployeeLeaves(currentDevId);
    if (tabId === "empViewAnalytics") loadPersonalAnalytics(currentDevId);
}

// ==========================================================================
// FEATURE 1: AUTHENTICATION, FORGOT PASSWORD & ONBOARDING WIZARD
// ==========================================================================
async function handleForgotPassword(e) {
    if (e) e.preventDefault();
    const email = document.getElementById("forgotEmailInput").value.trim();
    const btn = document.getElementById("btnSubmitForgot");
    if (btn) btn.disabled = true;

    try {
        const res = await fetch("/api/auth/forgot-password", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ email: email })
        });
        const json = await res.json();
        if (json.status === "SUCCESS") {
            const token = json.data.resetToken;
            const linkSec = document.getElementById("forgotTokenOutputSection");
            const linkDiv = document.getElementById("forgotTokenOutputLink");
            if (linkSec && linkDiv) {
                linkSec.style.display = "block";
                linkDiv.innerText = `Token: ${token}`;
            }
            showToast("Password reset link generated!", "success");
        } else {
            showToast(json.message || "Email address not registered.", "error");
        }
    } catch (err) {
        showToast("Error requesting password reset.", "error");
    } finally {
        if (btn) btn.disabled = false;
    }
}

function openResetFromForgot() {
    const linkDiv = document.getElementById("forgotTokenOutputLink");
    const token = linkDiv ? linkDiv.innerText.replace("Token: ", "").trim() : "";
    closeModal("modalForgotPassword");
    const resetInput = document.getElementById("resetTokenInput");
    if (resetInput) resetInput.value = token;
    openModal("modalResetPassword");
}

async function handleResetPassword(e) {
    if (e) e.preventDefault();
    const resetToken = document.getElementById("resetTokenInput").value.trim();
    const newPassword = document.getElementById("resetNewPasswordInput").value.trim();

    try {
        const res = await fetch("/api/auth/reset-password", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ resetToken: resetToken, newPassword: newPassword })
        });
        const json = await res.json();
        if (json.status === "SUCCESS") {
            closeModal("modalResetPassword");
            showToast("Password updated successfully! Please sign in with your new password.", "success");
            openModal("modalLogin");
        } else {
            showToast(json.message || "Invalid or expired reset token.", "error");
        }
    } catch (err) {
        showToast("Error resetting password.", "error");
    }
}

function openOnboardingWizard() {
    openModal("modalOnboarding");
}

async function handleOnboardingSubmit(e) {
    if (e) e.preventDefault();
    const roleTitle = document.getElementById("onboardRoleTitle").value.trim();
    const avatarUrl = document.getElementById("onboardAvatarUrl").value.trim();
    const s1 = document.getElementById("onboardSkill1").value.trim();
    const s2 = document.getElementById("onboardSkill2").value.trim();
    const s3 = document.getElementById("onboardSkill3").value.trim();
    const hours = parseInt(document.getElementById("onboardWeeklyHours").value) || 40;

    const skillsList = [s1, s2, s3].filter(Boolean).map(s => ({
        skillName: s,
        proficiency: "INTERMEDIATE",
        yearsExperience: 2
    }));

    try {
        const res = await fetch("/api/auth/onboarding", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
                userId: currentDevId,
                developerId: currentDevId,
                roleTitle: roleTitle,
                avatarUrl: avatarUrl,
                weeklyHours: hours,
                skills: skillsList
            })
        });
        const json = await res.json();
        if (json.status === "SUCCESS") {
            closeModal("modalOnboarding");
            showToast("Profile Onboarding completed! Welcome to your personal portal.", "success");
            loadDeveloperDashboard(currentDevId);
        } else {
            showToast(json.message || "Could not complete onboarding.", "error");
        }
    } catch (err) {
        showToast("Network error submitting onboarding.", "error");
    }
}

// ==========================================================================
// FEATURE 2: WORK SCHEDULE CALENDAR (MONTH & WEEK VIEWS)
// ==========================================================================
let calYear = 2026;
let calMonth = 9; // 0-indexed: 9 = October
let calMode = "MONTH"; // "MONTH" or "WEEK"
let cachedScheduleDays = [];

function setCalendarMode(mode) {
    calMode = mode;
    const btnMonth = document.getElementById("btnCalModeMonth");
    const btnWeek = document.getElementById("btnCalModeWeek");
    if (btnMonth && btnWeek) {
        if (mode === "MONTH") {
            btnMonth.style.background = "white";
            btnMonth.style.color = "var(--primary)";
            btnMonth.style.fontWeight = "700";
            btnWeek.style.background = "transparent";
            btnWeek.style.color = "var(--text-muted)";
            btnWeek.style.fontWeight = "600";
        } else {
            btnWeek.style.background = "white";
            btnWeek.style.color = "var(--primary)";
            btnWeek.style.fontWeight = "700";
            btnMonth.style.background = "transparent";
            btnMonth.style.color = "var(--text-muted)";
            btnMonth.style.fontWeight = "600";
        }
    }
    loadWorkScheduleCalendar(currentDevId);
}

function changeCalendarMonth(delta) {
    calMonth += delta;
    if (calMonth < 0) {
        calMonth = 11;
        calYear--;
    } else if (calMonth > 11) {
        calMonth = 0;
        calYear++;
    }
    loadWorkScheduleCalendar(currentDevId);
}

function resetCalendarToCurrent() {
    const now = new Date();
    calYear = now.getFullYear();
    calMonth = now.getMonth();
    loadWorkScheduleCalendar(currentDevId);
}

async function loadWorkScheduleCalendar(devId) {
    const grid = document.getElementById("calendarDaysGrid");
    const label = document.getElementById("calCurrentMonthLabel");
    if (!grid) return;

    const monthNames = ["January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"];
    if (label) label.innerText = `${monthNames[calMonth]} ${calYear}`;

    const monthStr = `${calYear}-${String(calMonth + 1).padStart(2, '0')}`;
    let url = `/api/employee/${devId}/calendar?month=${monthStr}`;
    if (calMode === "WEEK") {
        url = `/api/employee/${devId}/calendar?week=${calYear}-W41`;
    }

    try {
        const res = await fetch(url);
        const json = await res.json();
        if (json.status === "SUCCESS" && json.data) {
            cachedScheduleDays = json.data.days || [];
            renderCalendarGrid(cachedScheduleDays);
        } else {
            renderCalendarFallback();
        }
    } catch (err) {
        renderCalendarFallback();
    }
}

function renderCalendarGrid(days) {
    const grid = document.getElementById("calendarDaysGrid");
    if (!grid) return;
    grid.innerHTML = "";

    if (!days || days.length === 0) {
        grid.innerHTML = `<div style="grid-column: 1/-1; text-align: center; padding: 2rem; color: var(--text-muted);">No calendar data found.</div>`;
        return;
    }

    // Determine day of week offset for the 1st of the month
    const firstDate = new Date(days[0].date);
    const startDayOfWeek = firstDate.getDay(); // 0 = Sun, 1 = Mon ...

    if (calMode === "MONTH") {
        for (let i = 0; i < startDayOfWeek; i++) {
            const emptyCell = document.createElement("div");
            emptyCell.className = "calendar-day-cell";
            emptyCell.style.opacity = "0.3";
            emptyCell.style.background = "#fafafa";
            grid.appendChild(emptyCell);
        }
    }

    days.forEach(d => {
        const cell = document.createElement("div");
        cell.className = "calendar-day-cell";
        const dayNum = parseInt(d.date.split("-")[2]);
        const alloc = d.totalAllocationPercentage || 0;

        let loadPillHtml = "";
        if (alloc > 0) {
            let dotClass = "dot-green";
            let pillClass = "pill-green";
            if (alloc > 90) {
                dotClass = "dot-red";
                pillClass = "pill-red";
            } else if (alloc >= 70) {
                dotClass = "dot-amber";
                pillClass = "pill-amber";
            }
            loadPillHtml = `<span class="load-pill ${pillClass}"><span class="legend-dot ${dotClass}"></span>${alloc}%</span>`;
        }

        let leavePill = d.hasLeave ? `<span class="load-pill pill-red">🏖️ Leave</span>` : "";
        let deadlineIcon = d.hasDeadline ? `<span style="font-size: 0.8rem;" title="Deadline">🚩</span>` : "";

        let taskSnippets = "";
        if (d.tasks && d.tasks.length > 0) {
            taskSnippets = `<div class="day-tasks-preview" style="font-size: 0.72rem; color: #475569; margin-top: 4px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;">${escapeHtml(d.tasks[0].taskName || d.tasks[0].projectName)}</div>`;
        }

        cell.innerHTML = `
            <div class="day-number">
                <span>${dayNum}</span>
                ${deadlineIcon}
            </div>
            <div style="display: flex; flex-direction: column; gap: 3px; margin-top: 4px;">
                ${loadPillHtml}
                ${leavePill}
            </div>
            ${taskSnippets}
        `;

        cell.onclick = () => openDayDetailModal(d.date, d);
        grid.appendChild(cell);
    });
}

function renderCalendarFallback() {
    const daysInMonth = new Date(calYear, calMonth + 1, 0).getDate();
    const fallbackDays = [];
    for (let i = 1; i <= daysInMonth; i++) {
        const dateStr = `${calYear}-${String(calMonth + 1).padStart(2, '0')}-${String(i).padStart(2, '0')}`;
        fallbackDays.push({
            date: dateStr,
            totalAllocationPercentage: (i % 7 === 0 || i % 7 === 6) ? 0 : 60,
            hasLeave: (i === 15 || i === 16),
            hasDeadline: (i === 20),
            tasks: [{ taskName: "Auth Module Implementation", projectName: "Project Alpha" }]
        });
    }
    renderCalendarGrid(fallbackDays);
}

async function openDayDetailModal(dateStr, dayData) {
    const dateTitle = document.getElementById("modalDayDetailDate");
    const loadVal = document.getElementById("modalDayDetailLoad");
    const badgeContainer = document.getElementById("modalDayDetailStatusBadge");
    const tasksList = document.getElementById("modalDayDetailTasksList");
    const leaveSec = document.getElementById("modalDayDetailLeaveSection");
    const leaveText = document.getElementById("modalDayDetailLeaveText");

    if (dateTitle) dateTitle.innerText = `📅 Details for ${dateStr}`;

    try {
        const res = await fetch(`/api/employee/${currentDevId}/day-detail?date=${dateStr}`);
        const json = await res.json();
        const data = (json.status === "SUCCESS" && json.data) ? json.data : dayData;

        const alloc = data.totalAllocationPercentage || 0;
        if (loadVal) loadVal.innerText = `${alloc}%`;

        if (badgeContainer) {
            if (alloc > 90) {
                badgeContainer.innerHTML = `<span class="load-pill pill-red">⚠️ Critical Overload (&gt;90%)</span>`;
            } else if (alloc >= 70) {
                badgeContainer.innerHTML = `<span class="load-pill pill-amber">⚖️ Heavy Load (70-90%)</span>`;
            } else if (alloc > 0) {
                badgeContainer.innerHTML = `<span class="load-pill pill-green">✅ Balanced Workload (&lt;70%)</span>`;
            } else {
                badgeContainer.innerHTML = `<span class="load-pill" style="background: #f1f5f9; color: var(--text-muted);">🟢 Zero Allocation (Free)</span>`;
            }
        }

        if (tasksList) {
            tasksList.innerHTML = "";
            const tasks = data.tasks || [];
            if (tasks.length === 0) {
                tasksList.innerHTML = `<p style="color: var(--text-muted); font-size: 0.85rem; padding: 0.5rem 0;">No active tasks scheduled on this date.</p>`;
            } else {
                tasks.forEach(t => {
                    const div = document.createElement("div");
                    div.style.padding = "0.6rem 0.8rem";
                    div.style.background = "#f8fafc";
                    div.style.border = "1px solid var(--border-color)";
                    div.style.borderRadius = "var(--radius-sm)";
                    div.innerHTML = `
                        <div style="display: flex; justify-content: space-between; align-items: center;">
                            <strong>${escapeHtml(t.taskName || t.name)}</strong>
                            <span class="badge badge-primary">${t.allocationPercentage || 50}% Load</span>
                        </div>
                        <div style="font-size: 0.8rem; color: var(--text-muted); margin-top: 2px;">
                            ${escapeHtml(t.projectName || 'Active Project')} • ${t.startDate || ''} → ${t.endDate || ''}
                        </div>
                    `;
                    tasksList.appendChild(div);
                });
            }
        }

        if (leaveSec && leaveText) {
            if (data.hasLeave || (data.leaves && data.leaves.length > 0)) {
                leaveSec.style.display = "block";
                const reason = data.leaves && data.leaves[0] ? data.leaves[0].reason : "Scheduled Out of Office";
                leaveText.innerHTML = `<strong>Status:</strong> Approved Leave<br><strong>Reason:</strong> ${escapeHtml(reason)}`;
            } else {
                leaveSec.style.display = "none";
            }
        }

        openModal("modalDayDetail");
    } catch (err) {
        openModal("modalDayDetail");
    }
}

// ==========================================================================
// FEATURE 3: AI SKILL EXTRACTION & CERTIFICATION OCR PIPELINE
// ==========================================================================
async function triggerSkillExtraction() {
    const textInput = document.getElementById("aiProjectInputText");
    const btn = document.getElementById("btnExtractSkills");
    const sec = document.getElementById("extractedSkillsSection");
    const list = document.getElementById("extractedSuggestionsList");

    if (!textInput || !textInput.value.trim()) {
        showToast("Please enter a project description to analyze.", "warning");
        return;
    }

    if (btn) {
        btn.disabled = true;
        btn.innerHTML = `⏳ Analyzing with Claude AI...`;
    }

    try {
        const res = await fetch("/api/skills/extract-text", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ projectDescription: textInput.value })
        });
        const json = await res.json();
        if (json.status === "SUCCESS" && json.data) {
            const skills = json.data.extractedSkills || [];
            if (sec && list) {
                sec.style.display = "block";
                list.innerHTML = "";

                if (skills.length === 0) {
                    list.innerHTML = `<p style="color: var(--text-muted); font-size: 0.85rem;">No new technical skills recognized in the text.</p>`;
                } else {
                    skills.forEach(s => {
                        const chip = document.createElement("div");
                        chip.className = "suggestion-chip";
                        const conf = Math.round((s.confidence || 0.85) * 100);
                        chip.innerHTML = `
                            <span><strong>${escapeHtml(s.skillName)}</strong> (${escapeHtml(s.category || 'Tech')})</span>
                            <span class="confidence-chip">${conf}% Conf</span>
                            <button class="action-btn accept" title="Accept & Add to Profile as Pending Verification" onclick="acceptExtractedSkill('${escapeHtml(s.skillName)}', '${escapeHtml(s.category || 'General')}', ${conf}, this)">✓ Accept</button>
                            <button class="action-btn reject" title="Dismiss suggestion" onclick="this.closest('.suggestion-chip').remove()">✕ Reject</button>
                        `;
                        list.appendChild(chip);
                    });
                }
            }
            showToast(`Extracted ${skills.length} skills! Click Accept to save to profile.`, "success");
        } else {
            showToast(json.message || "Failed to extract skills.", "error");
        }
    } catch (err) {
        showToast("Error communicating with AI Extraction service.", "error");
    } finally {
        if (btn) {
            btn.disabled = false;
            btn.innerHTML = `✨ Extract Skills with Claude API`;
        }
    }
}

async function acceptExtractedSkill(skillName, category, confidence, btnEl) {
    try {
        const res = await fetch(`/api/employee/${currentDevId}/skills`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
                skillName: skillName,
                category: category,
                proficiency: "INTERMEDIATE",
                yearsExperience: 2,
                status: "PENDING",
                source: "PROJECT_TEXT",
                interestedInFuture: false
            })
        });
        const json = await res.json();
        if (json.status === "SUCCESS") {
            if (btnEl) {
                btnEl.innerText = "✓ Accepted";
                btnEl.disabled = true;
                btnEl.style.opacity = "0.7";
            }
            showToast(`'${skillName}' saved to profile as 'Pending verification'!`, "success");
            loadEmployeeSkills(currentDevId);
            loadProfileCompleteness(currentDevId);
        } else {
            showToast(json.message || "Could not save skill.", "error");
        }
    } catch (err) {
        showToast("Error saving accepted skill.", "error");
    }
}

// 3b: Certificate OCR & Upload
let pendingCertMappedSkills = [];

async function handleCertFileSelect(event) {
    const file = event.target.files[0];
    if (!file) return;

    const previewSec = document.getElementById("certOcrPreviewSection");
    showToast(`Parsing certificate document: ${file.name}...`, "info");

    try {
        const res = await fetch("/api/skills/extract-cert", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ fileName: file.name, rawText: "" })
        });
        const json = await res.json();
        const data = (json.status === "SUCCESS" && json.data) ? json.data : {
            certificationName: file.name.replace(/\.[^/.]+$/, ""),
            issuingBody: "Industry Credential Issuer",
            issueDate: new Date().toISOString().split("T")[0],
            credentialId: "CERT-" + Math.floor(100000 + Math.random() * 900000),
            confidenceScore: 0.95,
            mappedSkills: ["Cloud", "DevOps"]
        };

        if (previewSec) {
            previewSec.style.display = "block";
            document.getElementById("certNameInput").value = data.certificationName || "";
            document.getElementById("certIssuerInput").value = data.issuingBody || "";
            document.getElementById("certDateInput").value = data.issueDate || "";
            document.getElementById("certIdInput").value = data.credentialId || "";

            const confBadge = document.getElementById("certConfidenceBadge");
            if (confBadge) confBadge.innerText = `Confidence: ${Math.round((data.confidenceScore || 0.95) * 100)}%`;

            const chipsDiv = document.getElementById("certMappedSkillsChips");
            if (chipsDiv) {
                chipsDiv.innerHTML = "";
                pendingCertMappedSkills = data.mappedSkills || [];
                pendingCertMappedSkills.forEach(s => {
                    const tag = document.createElement("span");
                    tag.className = "badge badge-primary";
                    tag.innerText = `+ ${s}`;
                    chipsDiv.appendChild(tag);
                });
            }
        }
        showToast("Certificate OCR details extracted. Review and confirm below.", "success");
    } catch (err) {
        showToast("Error processing certificate.", "error");
    }
}

async function confirmSaveCertificate(e) {
    if (e) e.preventDefault();
    const name = document.getElementById("certNameInput").value.trim();
    const issuer = document.getElementById("certIssuerInput").value.trim();
    const date = document.getElementById("certDateInput").value;
    const credId = document.getElementById("certIdInput").value.trim();

    try {
        const res = await fetch(`/api/employee/${currentDevId}/certifications`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
                name: name,
                issuingBody: issuer,
                issueDate: date,
                credentialId: credId,
                mappedSkills: pendingCertMappedSkills
            })
        });
        const json = await res.json();
        if (json.status === "SUCCESS") {
            const previewSec = document.getElementById("certOcrPreviewSection");
            if (previewSec) previewSec.style.display = "none";
            showToast(`Certificate '${name}' added to Documents Vault & skills registered!`, "success");
            loadCertificatesVault(currentDevId);
            loadEmployeeSkills(currentDevId);
            loadProfileCompleteness(currentDevId);
        } else {
            showToast(json.message || "Error saving certificate.", "error");
        }
    } catch (err) {
        showToast("Error saving certificate.", "error");
    }
}

// ==========================================================================
// FEATURE 4: MANUAL SKILLS, TAXONOMY AUTOCOMPLETE & ENDORSEMENTS
// ==========================================================================
let taxonomySearchTimeout = null;

function handleTaxonomyAutocomplete(query) {
    clearTimeout(taxonomySearchTimeout);
    const dropdown = document.getElementById("taxonomyAutocompleteDropdown");
    if (!dropdown) return;

    if (!query || query.trim().length < 2) {
        dropdown.style.display = "none";
        return;
    }

    taxonomySearchTimeout = setTimeout(async () => {
        try {
            const res = await fetch(`/api/skills/taxonomy/search?q=${encodeURIComponent(query.trim())}`);
            const json = await res.json();
            if (json.status === "SUCCESS" && json.data) {
                const results = json.data;
                dropdown.innerHTML = "";
                if (results.length === 0) {
                    dropdown.style.display = "none";
                    return;
                }
                results.forEach(item => {
                    const div = document.createElement("div");
                    div.style.padding = "0.5rem 0.75rem";
                    div.style.cursor = "pointer";
                    div.style.borderBottom = "1px solid #f1f5f9";
                    div.style.fontSize = "0.85rem";
                    div.innerHTML = `<strong>${escapeHtml(item.canonicalName || item.name)}</strong> <small style="color: var(--text-muted);">(${escapeHtml(item.category || 'Skill')})</small>`;
                    div.onmousedown = () => {
                        const input = document.getElementById("manualSkillInput");
                        if (input) input.value = item.canonicalName || item.name;
                        dropdown.style.display = "none";
                    };
                    dropdown.appendChild(div);
                });
                dropdown.style.display = "block";
            }
        } catch (err) {
            dropdown.style.display = "none";
        }
    }, 250);
}

async function handleManualAddSkill(e) {
    if (e) e.preventDefault();
    const skillName = document.getElementById("manualSkillInput").value.trim();
    const prof = document.getElementById("manualProficiencySelect").value;
    const years = parseInt(document.getElementById("manualYearsExp").value) || 1;
    const interested = document.getElementById("manualInterestedToggle")?.checked || false;

    if (!skillName) {
        showToast("Please enter a skill name.", "warning");
        return;
    }

    try {
        const res = await fetch(`/api/employee/${currentDevId}/skills`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
                skillName: skillName,
                proficiency: prof,
                yearsExperience: years,
                status: "SELF_REPORTED",
                source: "MANUAL",
                interestedInFuture: interested
            })
        });
        const json = await res.json();
        if (json.status === "SUCCESS") {
            document.getElementById("manualSkillInput").value = "";
            showToast(`Skill '${skillName}' added to profile!`, "success");
            loadEmployeeSkills(currentDevId);
            loadProfileCompleteness(currentDevId);
        } else {
            showToast(json.message || "Error adding skill.", "error");
        }
    } catch (err) {
        showToast("Error adding skill.", "error");
    }
}

async function loadEmployeeSkills(devId) {
    const tbody = document.getElementById("employeeSkillsTableBody");
    if (!tbody) return;

    try {
        const res = await fetch(`/api/employee/${devId}/skills`);
        const json = await res.json();
        if (json.status === "SUCCESS" && json.data) {
            const skills = json.data;
            tbody.innerHTML = "";
            if (skills.length === 0) {
                tbody.innerHTML = `<tr><td colspan="8" style="text-align:center; color:var(--text-muted); padding:1.5rem;">No skills registered yet. Add skills or extract them above!</td></tr>`;
                return;
            }

            skills.forEach(s => {
                const tr = document.createElement("tr");

                let badgeHtml = `<span class="badge badge-info">📝 Self-reported</span>`;
                if (s.status === "VERIFIED_MANAGER") {
                    badgeHtml = `<span class="badge badge-success">🛡️ Verified (Manager)</span>`;
                } else if (s.status === "VERIFIED_TRACK_RECORD") {
                    badgeHtml = `<span class="badge badge-success">🏆 Verified (Track Record)</span>`;
                } else if (s.status === "PENDING") {
                    badgeHtml = `<span class="badge badge-warning">⏳ Pending verification</span>`;
                }

                const futureTag = s.interestedInFuture ? `<span style="color: #ea580c; font-weight: 700; font-size: 0.8rem;">⭐ Looking to Use</span>` : `<span style="color: var(--text-muted);">-</span>`;

                tr.innerHTML = `
                    <td><strong>${escapeHtml(s.skillName || 'Skill')}</strong></td>
                    <td><span class="badge badge-secondary">${escapeHtml(s.category || 'Tech')}</span></td>
                    <td>${s.proficiency || 'Intermediate'}</td>
                    <td>${s.yearsExperience || 1} yrs</td>
                    <td>${badgeHtml}</td>
                    <td>${futureTag}</td>
                    <td>
                        <span style="font-weight: 700; color: var(--primary);">${s.endorsementsCount || 0}</span>
                        <button class="btn btn-xs btn-outline" style="margin-left: 6px; padding: 2px 6px;" onclick="endorseSkill(${s.developerId}, ${s.skillId})">👍 +1</button>
                    </td>
                    <td>
                        <button class="btn btn-xs btn-outline" style="color: var(--critical); border-color: #fca5a5;" onclick="deleteEmployeeSkill(${s.developerId}, ${s.skillId})">Delete</button>
                    </td>
                `;
                tbody.appendChild(tr);
            });
        }
    } catch (err) {
        tbody.innerHTML = `<tr><td colspan="8" style="text-align:center; color:var(--text-muted); padding:1.5rem;">Failed to load skills list.</td></tr>`;
    }
}

async function deleteEmployeeSkill(devId, skillId) {
    if (!confirm("Remove this skill from your profile?")) return;
    try {
        const res = await fetch(`/api/employee/${devId}/skills/${skillId}`, { method: "DELETE" });
        const json = await res.json();
        if (json.status === "SUCCESS") {
            showToast("Skill deleted from profile.", "info");
            loadEmployeeSkills(devId);
            loadProfileCompleteness(devId);
        }
    } catch (err) {
        showToast("Error deleting skill.", "error");
    }
}

async function endorseSkill(devId, skillId) {
    try {
        const res = await fetch("/api/skills/endorse", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
                developerId: devId,
                skillId: skillId,
                endorserId: currentDevId || 1
            })
        });
        const json = await res.json();
        if (json.status === "SUCCESS") {
            showToast("Skill endorsed! Credibility score boosted.", "success");
            loadEmployeeSkills(devId);
        } else {
            showToast(json.message || "Could not endorse skill.", "error");
        }
    } catch (err) {
        showToast("Error endorsing skill.", "error");
    }
}

// ==========================================================================
// FEATURE 3d: MANAGER SKILL VERIFICATION REVIEW QUEUE
// ==========================================================================
async function loadManagerVerificationQueue() {
    const tbody = document.getElementById("managerQueueTableBody");
    const badge = document.getElementById("queueCountBadge");
    if (!tbody) return;

    try {
        const res = await fetch("/api/skills/verification-queue");
        const json = await res.json();
        if (json.status === "SUCCESS" && json.data) {
            const queue = json.data;
            if (badge) {
                badge.innerText = queue.length;
                badge.style.display = queue.length > 0 ? "inline-block" : "none";
            }
            tbody.innerHTML = "";
            if (queue.length === 0) {
                tbody.innerHTML = `<tr><td colspan="7" style="text-align:center; color:var(--text-muted); padding:2rem;">🎉 No pending skills in verification queue. All clean!</td></tr>`;
                return;
            }

            queue.forEach(item => {
                const tr = document.createElement("tr");
                tr.innerHTML = `
                    <td><strong>${escapeHtml(item.developerName || 'Developer')}</strong></td>
                    <td><strong>${escapeHtml(item.skillName || 'Skill')}</strong></td>
                    <td><span class="badge badge-secondary">${escapeHtml(item.category || 'General')}</span></td>
                    <td>${item.proficiency || 'Intermediate'}</td>
                    <td><span class="badge badge-warning">${item.source || 'EXTRACTED'}</span></td>
                    <td>${item.yearsExperience || 1} yrs</td>
                    <td>
                        <div style="display: flex; gap: 4px;">
                            <button class="btn btn-xs btn-success" onclick="verifySkillAction(${item.developerId}, ${item.skillId}, 'VERIFIED_MANAGER')">✓ Approve</button>
                            <button class="btn btn-xs btn-outline" style="color: var(--critical);" onclick="verifySkillAction(${item.developerId}, ${item.skillId}, 'REJECTED')">✕ Reject</button>
                            <button class="btn btn-xs btn-outline" onclick="requestSkillClarification(${item.developerId}, ${item.skillId})">💬 Clarify</button>
                        </div>
                    </td>
                `;
                tbody.appendChild(tr);
            });
        }
    } catch (err) {
        tbody.innerHTML = `<tr><td colspan="7" style="text-align:center; color:var(--text-muted); padding:2rem;">Error loading verification queue.</td></tr>`;
    }
}

async function verifySkillAction(devId, skillId, status, notes = "") {
    try {
        const res = await fetch("/api/skills/verify", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
                developerId: devId,
                skillId: skillId,
                status: status,
                notes: notes
            })
        });
        const json = await res.json();
        if (json.status === "SUCCESS") {
            showToast(`Skill verification updated to ${status}! Priority match weights refreshed.`, "success");
            loadManagerVerificationQueue();
        } else {
            showToast(json.message || "Error updating verification status.", "error");
        }
    } catch (err) {
        showToast("Error updating verification status.", "error");
    }
}

function requestSkillClarification(devId, skillId) {
    const note = prompt("Enter clarification request note for the employee (e.g. Please provide milestone or credential proof):");
    if (note) {
        verifySkillAction(devId, skillId, "CLARIFICATION_REQUESTED", note);
    }
}

// ==========================================================================
// DOCUMENTS & CERTIFICATIONS VAULT
// ==========================================================================
async function loadCertificatesVault(devId) {
    const grid = document.getElementById("documentsVaultGrid");
    if (!grid) return;

    try {
        const res = await fetch(`/api/employee/${devId}/certifications`);
        const json = await res.json();
        if (json.status === "SUCCESS" && json.data) {
            const certs = json.data;
            grid.innerHTML = "";
            if (certs.length === 0) {
                grid.innerHTML = `<p style="color: var(--text-muted); padding: 1.5rem; grid-column: 1/-1;">No documents or certifications registered yet.</p>`;
                return;
            }

            certs.forEach(c => {
                const card = document.createElement("div");
                card.className = "document-card";
                card.innerHTML = `
                    <div style="font-size: 2rem; margin-bottom: 0.5rem;">📜</div>
                    <strong style="font-size: 0.95rem; color: var(--text-main);">${escapeHtml(c.name)}</strong>
                    <div style="font-size: 0.82rem; color: var(--text-muted); margin: 3px 0 6px 0;">${escapeHtml(c.issuingBody)}</div>
                    <div style="font-size: 0.78rem; color: var(--primary); font-family: monospace;">ID: ${escapeHtml(c.credentialId || 'VERIFIED')}</div>
                    <div style="font-size: 0.75rem; color: var(--text-muted); margin-top: 4px;">Issued: ${c.issueDate || '2026'}</div>
                `;
                grid.appendChild(card);
            });
        }
    } catch (err) {
        grid.innerHTML = `<p style="color: var(--text-muted); padding: 1.5rem;">Error loading documents vault.</p>`;
    }
}

// ==========================================================================
// LEAVE REQUEST WORKFLOW
// ==========================================================================
async function loadEmployeeLeaves(devId) {
    const tbody = document.getElementById("employeeLeavesTableBody");
    if (!tbody) return;

    try {
        const res = await fetch(`/api/employee/${devId}/leaves`);
        const json = await res.json();
        if (json.status === "SUCCESS" && json.data) {
            const leaves = json.data;
            tbody.innerHTML = "";
            if (leaves.length === 0) {
                tbody.innerHTML = `<tr><td colspan="6" style="text-align:center; color:var(--text-muted); padding:1.5rem;">No leave requests submitted.</td></tr>`;
                return;
            }

            leaves.forEach(l => {
                const tr = document.createElement("tr");
                let statusBadge = `<span class="badge badge-warning">${l.status}</span>`;
                if (l.status === "APPROVED") statusBadge = `<span class="badge badge-success">APPROVED</span>`;
                if (l.status === "REJECTED") statusBadge = `<span class="badge badge-critical">REJECTED</span>`;

                tr.innerHTML = `
                    <td><strong>${l.startDate}</strong></td>
                    <td><strong>${l.endDate}</strong></td>
                    <td>${escapeHtml(l.reason)}</td>
                    <td>${statusBadge}</td>
                    <td>${escapeHtml(l.reviewerName || 'Project Manager')}</td>
                    <td><small style="color: var(--text-muted);">${escapeHtml(l.managerNotes || '-')}</small></td>
                `;
                tbody.appendChild(tr);
            });
        }
    } catch (err) {
        tbody.innerHTML = `<tr><td colspan="6" style="text-align:center; color:var(--text-muted); padding:1.5rem;">Failed to load leaves.</td></tr>`;
    }
}

async function handleEmployeeLeaveSubmit(e) {
    if (e) e.preventDefault();
    const start = document.getElementById("empLeaveStart").value;
    const end = document.getElementById("empLeaveEnd").value;
    const reason = document.getElementById("empLeaveReason").value.trim();

    try {
        const res = await fetch(`/api/employee/${currentDevId}/leaves`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
                startDate: start,
                endDate: end,
                reason: reason
            })
        });
        const json = await res.json();
        if (json.status === "SUCCESS") {
            closeModal("modalLeaveRequest");
            showToast("Leave request submitted for manager approval!", "success");
            loadEmployeeLeaves(currentDevId);
            loadWorkScheduleCalendar(currentDevId);
        } else {
            showToast(json.message || "Error submitting leave request.", "error");
        }
    } catch (err) {
        showToast("Error submitting leave request.", "error");
    }
}

async function loadManagerLeaveApprovals() {
    const tbody = document.getElementById("managerLeaveQueueTableBody");
    if (!tbody) return;

    try {
        const res = await fetch("/api/leaves/pending");
        const json = await res.json();
        if (json.status === "SUCCESS" && json.data) {
            const leaves = json.data;
            tbody.innerHTML = "";
            if (leaves.length === 0) {
                tbody.innerHTML = `<tr><td colspan="6" style="text-align:center; color:var(--text-muted); padding:2rem;">🏖️ No pending leave requests.</td></tr>`;
                return;
            }

            leaves.forEach(l => {
                const tr = document.createElement("tr");
                tr.innerHTML = `
                    <td><strong>${escapeHtml(l.developerName || 'Developer')}</strong></td>
                    <td>${l.startDate}</td>
                    <td>${l.endDate}</td>
                    <td>${escapeHtml(l.reason)}</td>
                    <td><small style="color: var(--text-muted);">${l.createdAt ? l.createdAt.split('T')[0] : 'Recent'}</small></td>
                    <td>
                        <div style="display: flex; gap: 4px;">
                            <button class="btn btn-xs btn-success" onclick="reviewLeaveAction(${l.id}, 'APPROVED')">✓ Approve</button>
                            <button class="btn btn-xs btn-outline" style="color: var(--critical);" onclick="reviewLeaveAction(${l.id}, 'REJECTED')">✕ Reject</button>
                        </div>
                    </td>
                `;
                tbody.appendChild(tr);
            });
        }
    } catch (err) {
        tbody.innerHTML = `<tr><td colspan="6" style="text-align:center; color:var(--text-muted); padding:2rem;">Error loading pending leaves.</td></tr>`;
    }
}

async function reviewLeaveAction(leaveId, status) {
    const endpoint = status === "APPROVED" ? `/api/leaves/${leaveId}/approve` : `/api/leaves/${leaveId}/reject`;
    try {
        const res = await fetch(endpoint, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ notes: `Reviewed and ${status}` })
        });
        const json = await res.json();
        if (json.status === "SUCCESS") {
            showToast(`Leave request ${status.toLowerCase()}! Calendar & capacity updated.`, "success");
            loadManagerLeaveApprovals();
            loadLeavesManager();
        } else {
            showToast(json.message || "Error reviewing leave.", "error");
        }
    } catch (err) {
        showToast("Error reviewing leave.", "error");
    }
}

// ==========================================================================
// PROFILE COMPLETENESS INDICATOR
// ==========================================================================
async function loadProfileCompleteness(devId) {
    const pctLabel = document.getElementById("completenessPctLabel");
    const barFill = document.getElementById("completenessBarFill");
    const nudge = document.getElementById("completenessNudgeText");
    const banner = document.getElementById("profileCompletenessBanner");

    try {
        const res = await fetch(`/api/employee/${devId}/profile-completeness`);
        const json = await res.json();
        if (json.status === "SUCCESS" && json.data) {
            const data = json.data;
            const pct = data.completenessPercentage || 75;

            if (pctLabel) pctLabel.innerText = `${pct}% Complete`;
            if (barFill) barFill.style.width = `${pct}%`;
            if (nudge) nudge.innerText = data.nextStepNudge || "Profile looking good!";

            if (pct >= 100 && banner) {
                banner.style.background = "linear-gradient(135deg, #059669 0%, #10b981 100%)";
            }
        }
    } catch (err) {
        // Fallback default
    }
}

// ==========================================================================
// TEAM DIRECTORY (MANAGER VIEW)
// ==========================================================================
let allTeamDirectoryCache = [];

async function loadTeamDirectory() {
    const grid = document.getElementById("teamDirectoryGrid");
    if (!grid) return;

    try {
        const res = await fetch("/api/team/directory");
        const json = await res.json();
        if (json.status === "SUCCESS" && json.data) {
            allTeamDirectoryCache = json.data;
            filterTeamDirectory();
        }
    } catch (err) {
        grid.innerHTML = `<p style="color: var(--text-muted); padding: 1.5rem;">Failed to load team directory.</p>`;
    }
}

function filterTeamDirectory() {
    const grid = document.getElementById("teamDirectoryGrid");
    if (!grid) return;

    const searchVal = document.getElementById("teamDirSearchInput")?.value.toLowerCase().trim() || "";
    const roleVal = document.getElementById("teamDirRoleFilter")?.value || "";

    const filtered = allTeamDirectoryCache.filter(d => {
        const roleMatch = !roleVal || (d.roleTitle || "").toLowerCase().includes(roleVal.toLowerCase());
        const skills = parseSkillsList(d.skills);
        const nameMatch = (d.name || "").toLowerCase().includes(searchVal);
        const skillMatch = !searchVal || skills.some(s => s.toLowerCase().includes(searchVal));
        return roleMatch && (nameMatch || skillMatch);
    });

    grid.innerHTML = "";
    if (filtered.length === 0) {
        grid.innerHTML = `<p style="color: var(--text-muted); padding: 1.5rem; grid-column: 1/-1;">No resources found matching filter criteria.</p>`;
        return;
    }

    filtered.forEach(d => {
        const card = document.createElement("div");
        card.className = "document-card";
        card.style.cursor = "default";
        const skills = parseSkillsList(d.skills);

        const skillBadges = skills.slice(0, 4).map(s => `<span class="badge badge-secondary" style="font-size: 0.72rem;">${escapeHtml(s)}</span>`).join(" ");

        card.innerHTML = `
            <div style="display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 6px;">
                <strong style="font-size: 1rem; color: var(--text-main);">${escapeHtml(d.name)}</strong>
                <span class="badge badge-${(d.currentAllocation || 0) > 90 ? 'critical' : 'success'}">${d.currentAllocation || 0}% Load</span>
            </div>
            <div style="font-size: 0.82rem; color: var(--text-muted); margin-bottom: 8px;">${escapeHtml(d.roleTitle || 'Developer')}</div>
            <div style="display: flex; flex-wrap: wrap; gap: 4px; margin-bottom: 10px;">
                ${skillBadges}
            </div>
            <div style="display: flex; justify-content: space-between; align-items: center; font-size: 0.8rem; border-top: 1px solid #f1f5f9; padding-top: 6px;">
                <span style="color: var(--primary); font-weight: 700;">₹${d.hourlyCost || 500}/hr</span>
                <button class="btn btn-xs btn-outline" onclick="openAssignResourceModal(null, ${d.id})">+ Assign</button>
            </div>
        `;
        grid.appendChild(card);
    });
}

// ==========================================================================
// PERSONAL ANALYTICS & POST-TASK FEEDBACK
// ==========================================================================
let personalUtilizationChart = null;

async function loadPersonalAnalytics(devId) {
    try {
        const res = await fetch(`/api/employee/${devId}/analytics`);
        const json = await res.json();
        if (json.status === "SUCCESS" && json.data) {
            const data = json.data;

            const tEl = document.getElementById("analyticsTasksCompletedCount");
            const sEl = document.getElementById("analyticsVerifiedSkillsCount");
            const uEl = document.getElementById("analyticsAvgUtilizationVal");

            if (tEl) tEl.innerText = data.completedMilestonesCount || 3;
            if (sEl) sEl.innerText = data.verifiedSkillsCount || 3;
            if (uEl) uEl.innerText = `${data.averageUtilization || 74}%`;

            renderPersonalUtilizationChart(data.utilizationTrend || [65, 70, 75, 80, 72, 74]);
            renderPersonalFeedbackList(data.recentFeedback || []);
        }
    } catch (err) {
        // Fallback chart
        renderPersonalUtilizationChart([65, 70, 75, 80, 72, 74]);
    }
}

function renderPersonalUtilizationChart(trend) {
    const canvas = document.getElementById("chartPersonalUtilization");
    if (!canvas || typeof Chart === 'undefined') return;

    const ctx = canvas.getContext("2d");
    if (personalUtilizationChart) {
        personalUtilizationChart.destroy();
    }

    personalUtilizationChart = new Chart(ctx, {
        type: 'line',
        data: {
            labels: ['May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct'],
            datasets: [{
                label: 'Monthly Allocation %',
                data: trend,
                borderColor: '#6366f1',
                backgroundColor: 'rgba(99, 102, 241, 0.1)',
                borderWidth: 3,
                fill: true,
                tension: 0.35,
                pointRadius: 4,
                pointBackgroundColor: '#6366f1'
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            scales: {
                y: {
                    min: 0,
                    max: 120,
                    ticks: { callback: v => `${v}%` }
                }
            }
        }
    });
}

function renderPersonalFeedbackList(feedbackList) {
    const container = document.getElementById("analyticsFeedbackList");
    if (!container) return;

    if (!feedbackList || feedbackList.length === 0) {
        container.innerHTML = `<p style="color: var(--text-muted); font-size: 0.85rem;">No post-task manager reviews submitted yet.</p>`;
        return;
    }

    container.innerHTML = "";
    feedbackList.forEach(fb => {
        const div = document.createElement("div");
        div.style.padding = "0.75rem";
        div.style.background = "#f8fafc";
        div.style.border = "1px solid var(--border-color)";
        div.style.borderRadius = "var(--radius-sm)";
        div.style.marginBottom = "0.5rem";

        const stars = "⭐".repeat(fb.rating || 5);
        div.innerHTML = `
            <div style="display: flex; justify-content: space-between; align-items: center;">
                <strong>${escapeHtml(fb.milestoneName || 'Milestone Delivery')}</strong>
                <span>${stars} (${fb.rating}/5)</span>
            </div>
            <div style="font-size: 0.85rem; color: #334155; margin-top: 4px;">"${escapeHtml(fb.comment || 'Great execution.')}"</div>
            <div style="font-size: 0.75rem; color: var(--text-muted); margin-top: 4px;">Reviewed by Manager • ${fb.createdAt ? fb.createdAt.split('T')[0] : 'Recent'}</div>
        `;
        container.appendChild(div);
    });
}

function openPostTaskFeedbackModal(milestoneId, devId) {
    document.getElementById("feedbackMilestoneId").value = milestoneId;
    document.getElementById("feedbackDeveloperId").value = devId;
    openModal("modalPostTaskFeedback");
}

async function handlePostTaskFeedbackSubmit(e) {
    if (e) e.preventDefault();
    const milestoneId = parseInt(document.getElementById("feedbackMilestoneId").value);
    const devId = parseInt(document.getElementById("feedbackDeveloperId").value);
    const rating = parseInt(document.getElementById("feedbackRating").value) || 5;
    const comment = document.getElementById("feedbackComment").value.trim();

    try {
        const res = await fetch("/api/feedback", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
                milestoneId: milestoneId,
                developerId: devId,
                managerId: 1,
                rating: rating,
                comment: comment
            })
        });
        const json = await res.json();
        if (json.status === "SUCCESS") {
            closeModal("modalPostTaskFeedback");
            showToast("Post-task feedback saved! Added to employee portfolio.", "success");
        } else {
            showToast(json.message || "Error submitting feedback.", "error");
        }
    } catch (err) {
        showToast("Error submitting feedback.", "error");
    }
}
