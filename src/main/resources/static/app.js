// Agentic DB Copilot Frontend Client

let currentRawSql = "";

document.addEventListener("DOMContentLoaded", () => {
    loadCacheCount();
    loadSchema();
    loadCacheMetrics();
});

function setPrompt(text) {
    const el = document.getElementById("user-prompt");
    el.value = text;
    el.focus();
}

function switchTab(tab) {
    const tabs = ['copilot', 'schema', 'cache'];
    tabs.forEach(t => {
        const btn = document.getElementById(`tab-${t}`);
        const view = document.getElementById(`view-${t}`);
        if (t === tab) {
            btn.className = "px-3 py-1.5 rounded-md font-medium text-emerald-400 bg-slate-700/80 shadow-sm transition";
            view.classList.remove("hidden");
        } else {
            btn.className = "px-3 py-1.5 rounded-md font-medium text-slate-400 hover:text-slate-200 transition";
            view.classList.add("hidden");
        }
    });

    if (tab === 'schema') loadSchema();
    if (tab === 'cache') loadCacheEntries();
}

async function executeCopilotQuery() {
    const promptInput = document.getElementById("user-prompt");
    const prompt = promptInput.value.trim();
    if (!prompt) {
        alert("Please enter a natural language query.");
        return;
    }

    const enableCache = document.getElementById("chk-enable-cache").checked;
    const analyzePerf = document.getElementById("chk-analyze-perf").checked;

    const btn = document.getElementById("btn-submit");
    const loadingState = document.getElementById("loading-state");
    const resultsContainer = document.getElementById("results-container");

    btn.disabled = true;
    btn.classList.add("opacity-50");
    loadingState.classList.remove("hidden");
    resultsContainer.classList.add("hidden");

    try {
        const res = await fetch("/api/copilot/query", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
                prompt: prompt,
                enableCache: enableCache,
                analyzePerformance: analyzePerf,
                maxCorrectionAttempts: 3
            })
        });

        if (!res.ok) {
            const err = await res.json();
            throw new Error(err.message || "Failed to execute query");
        }

        const data = await res.json();
        renderResults(data);
        loadCacheCount();
        loadCacheMetrics();

    } catch (err) {
        alert("Error: " + err.message);
    } finally {
        btn.disabled = false;
        btn.classList.remove("opacity-50");
        loadingState.classList.add("hidden");
    }
}

function renderResults(data) {
    const resultsContainer = document.getElementById("results-container");
    resultsContainer.classList.remove("hidden");

    // Banner KPIs
    const cacheStatusEl = document.getElementById("res-cache-status");
    const cacheIconEl = document.getElementById("badge-cache-icon");
    if (data.cached) {
        cacheStatusEl.innerHTML = `<span class="text-emerald-400">HIT (${(data.cacheSimilarity * 100).toFixed(1)}%)</span>`;
        cacheIconEl.className = "p-3 rounded-lg bg-emerald-500/20 text-emerald-400 text-lg";
    } else {
        cacheStatusEl.innerHTML = `<span class="text-slate-400">MISS (LLM Synthesized)</span>`;
        cacheIconEl.className = "p-3 rounded-lg bg-slate-800 text-slate-400 text-lg";
    }

    document.getElementById("res-latency").textContent = `${data.totalLatencyMs} ms`;
    document.getElementById("res-corrections").textContent = `${data.selfCorrectionAttempts} retry(s)`;
    
    const rowCount = data.result && data.result.rowCount ? data.result.rowCount : 0;
    document.getElementById("res-rows").textContent = `${rowCount} rows`;

    // SQL & Explanation
    currentRawSql = data.generatedSql || "";
    document.getElementById("res-sql").textContent = currentRawSql;
    document.getElementById("res-explanation").textContent = data.explanation || "No explanation provided.";

    // Render Table
    renderTable(data.result);

    // Render Query Plan
    const optCard = document.getElementById("optimizer-card");
    if (data.performancePlan) {
        optCard.classList.remove("hidden");
        document.getElementById("res-plan-cost").textContent = `Cost: ${data.performancePlan.totalCost || 'N/A'}`;
        
        const recList = document.getElementById("optimizer-recommendations");
        recList.innerHTML = "";
        (data.performancePlan.recommendations || []).forEach(r => {
            const li = document.createElement("div");
            li.className = "p-2 rounded bg-slate-950/60 border border-slate-800/80 text-emerald-300 flex items-start gap-2";
            li.innerHTML = `<i class="fa-solid fa-lightbulb text-amber-400 mt-0.5"></i> <span>${escapeHtml(r)}</span>`;
            recList.appendChild(li);
        });

        document.getElementById("raw-plan-output").textContent = data.performancePlan.rawPlan || "No plan data";
    } else {
        optCard.classList.add("hidden");
    }

    // Render Multi-Agent Trace
    renderTraceTimeline(data.agentTrace);
}

function renderTable(result) {
    const thead = document.getElementById("table-head");
    const tbody = document.getElementById("table-body");
    const execTime = document.getElementById("res-exec-time");

    thead.innerHTML = "";
    tbody.innerHTML = "";

    if (!result || !result.success) {
        execTime.textContent = "Error";
        tbody.innerHTML = `<tr><td class="p-4 text-rose-400 font-mono">Error: ${escapeHtml(result ? result.errorMessage : 'Unknown execution failure')}</td></tr>`;
        return;
    }

    execTime.textContent = `Exec: ${result.executionTimeMs} ms`;

    if (!result.columns || result.columns.length === 0) {
        tbody.innerHTML = `<tr><td class="p-4 text-slate-400">Statement completed with ${result.rowCount} affected rows.</td></tr>`;
        return;
    }

    // Table Header
    const trHead = document.createElement("tr");
    result.columns.forEach((col, idx) => {
        const th = document.createElement("th");
        th.className = "p-3 font-semibold text-slate-300 uppercase tracking-wider";
        const type = result.columnTypes && result.columnTypes[idx] ? ` <span class="text-[10px] text-slate-500 font-normal">(${result.columnTypes[idx]})</span>` : "";
        th.innerHTML = `${escapeHtml(col)}${type}`;
        trHead.appendChild(th);
    });
    thead.appendChild(trHead);

    // Table Rows
    if (!result.rows || result.rows.length === 0) {
        tbody.innerHTML = `<tr><td colspan="${result.columns.length}" class="p-6 text-center text-slate-500">No rows returned by query.</td></tr>`;
        return;
    }

    result.rows.forEach(row => {
        const tr = document.createElement("tr");
        tr.className = "hover:bg-slate-800/40 transition";
        result.columns.forEach(col => {
            const td = document.createElement("td");
            td.className = "p-3 font-mono text-slate-300 truncate max-w-xs";
            const val = row[col];
            td.textContent = val !== null && val !== undefined ? val : "NULL";
            if (val === null) td.className += " text-slate-600 italic";
            tr.appendChild(td);
        });
        tbody.appendChild(tr);
    });
}

function renderTraceTimeline(trace) {
    const container = document.getElementById("timeline-container");
    container.innerHTML = "";

    if (!trace || trace.length === 0) {
        container.innerHTML = `<div class="text-xs text-slate-500">No trace steps captured.</div>`;
        return;
    }

    trace.forEach(step => {
        const stepDiv = document.createElement("div");
        stepDiv.className = "trace-step-item";

        let statusClass = "success";
        if (step.status === "FAILED") statusClass = "failed";
        else if (step.status === "RETRYING") statusClass = "retrying";
        else if (step.status === "MISS") statusClass = "miss";

        stepDiv.innerHTML = `
            <div class="trace-step-dot ${statusClass}"></div>
            <div class="bg-slate-950/70 border border-slate-800/80 rounded-xl p-3 space-y-1.5">
                <div class="flex items-center justify-between">
                    <span class="text-xs font-bold text-slate-200">${escapeHtml(step.agentName)}</span>
                    <span class="text-[10px] font-mono text-slate-400 bg-slate-800/80 px-2 py-0.5 rounded">${step.durationMs}ms</span>
                </div>
                <div class="text-xs text-slate-300">${escapeHtml(step.description)}</div>
                ${step.output ? `<pre class="mt-1 p-2 bg-slate-900 border border-slate-800/80 rounded text-[11px] font-mono text-emerald-400 overflow-x-auto">${escapeHtml(step.output)}</pre>` : ''}
            </div>
        `;
        container.appendChild(stepDiv);
    });
}

async function loadSchema() {
    const grid = document.getElementById("schema-grid");
    try {
        const res = await fetch("/api/schema");
        const schema = await res.json();

        grid.innerHTML = "";
        (schema.tables || []).forEach(table => {
            const card = document.createElement("div");
            card.className = "bg-slate-900 border border-slate-800 rounded-2xl p-5 shadow-lg space-y-3";
            
            let colsHtml = "";
            (table.columns || []).forEach(col => {
                let badge = "";
                if (col.primaryKey) badge += `<span class="px-1.5 py-0.5 rounded bg-amber-500/10 text-amber-400 text-[10px] font-semibold">PK</span> `;
                if (col.foreignKey) badge += `<span class="px-1.5 py-0.5 rounded bg-sky-500/10 text-sky-400 text-[10px] font-semibold">FK</span> `;

                colsHtml += `
                    <div class="flex items-center justify-between text-xs py-1 border-b border-slate-800/60 last:border-0">
                        <div class="flex items-center space-x-2">
                            ${badge}
                            <span class="font-mono text-slate-200">${escapeHtml(col.columnName)}</span>
                        </div>
                        <span class="text-slate-400 font-mono text-[11px]">${escapeHtml(col.dataType)}</span>
                    </div>
                `;
            });

            card.innerHTML = `
                <div class="flex items-center justify-between border-b border-slate-800 pb-2">
                    <div class="flex items-center space-x-2">
                        <i class="fa-solid fa-table-cells text-emerald-400"></i>
                        <h4 class="text-sm font-bold text-slate-100 font-mono">${escapeHtml(table.tableName)}</h4>
                    </div>
                    <span class="text-[10px] bg-slate-800 text-slate-400 px-2 py-0.5 rounded-full">${table.columns.length} cols</span>
                </div>
                <div class="space-y-1">
                    ${colsHtml}
                </div>
            `;
            grid.appendChild(card);
        });
    } catch (e) {
        grid.innerHTML = `<div class="text-rose-400 text-xs">Failed to load schema: ${e.message}</div>`;
    }
}

async function loadCacheEntries() {
    const tbody = document.getElementById("cache-table-body");
    try {
        const res = await fetch("/api/cache/entries");
        const entries = await res.json();

        tbody.innerHTML = "";
        if (entries.length === 0) {
            tbody.innerHTML = `<tr><td colspan="6" class="p-6 text-center text-slate-500">Pgvector cache is currently empty. Run a query to store vectors.</td></tr>`;
            return;
        }

        entries.forEach(entry => {
            const tr = document.createElement("tr");
            tr.className = "hover:bg-slate-800/40 transition";
            tr.innerHTML = `
                <td class="p-3 font-mono text-slate-400">${entry.id}</td>
                <td class="p-3 font-medium text-slate-200">${escapeHtml(entry.naturalQuery)}</td>
                <td class="p-3 font-mono text-emerald-400 max-w-xs truncate">${escapeHtml(entry.generatedSql)}</td>
                <td class="p-3 font-mono text-slate-300">${entry.hitCount}</td>
                <td class="p-3 font-mono text-sky-400">${entry.executionLatencyMs}ms</td>
                <td class="p-3 text-slate-400 text-[11px]">${entry.lastAccessedAt || ''}</td>
            `;
            tbody.appendChild(tr);
        });
    } catch (e) {
        tbody.innerHTML = `<tr><td colspan="6" class="p-4 text-rose-400">Error loading cache entries.</td></tr>`;
    }
}

async function loadCacheCount() {
    try {
        const res = await fetch("/api/cache/entries");
        const entries = await res.json();
        document.getElementById("nav-cache-count").textContent = entries.length;
    } catch (ignored) {}
}

async function loadCacheMetrics() {
    try {
        const res = await fetch("/api/cache/metrics");
        const metrics = await res.json();

        document.getElementById("metric-hit-rate").textContent = `${metrics.hitRatePercent}%`;
        document.getElementById("metric-latency-reduction").textContent = `${metrics.latencyReductionPercent}%`;
        document.getElementById("metric-avg-cached").textContent = `${metrics.averageLatencyCachedMs} ms`;
        document.getElementById("metric-tokens-saved").textContent = metrics.totalTokensSavedEstimate.toLocaleString();
    } catch (ignored) {}
}

async function clearCache() {
    if (!confirm("Are you sure you want to flush all Pgvector semantic cache entries?")) return;
    try {
        await fetch("/api/cache", { method: "DELETE" });
        loadCacheEntries();
        loadCacheCount();
        loadCacheMetrics();
    } catch (err) {
        alert("Failed to clear cache: " + err.message);
    }
}

function copySql() {
    if (!currentRawSql) return;
    navigator.clipboard.writeText(currentRawSql);
    alert("SQL copied to clipboard!");
}

function escapeHtml(str) {
    if (!str) return "";
    return String(str)
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#039;");
}
