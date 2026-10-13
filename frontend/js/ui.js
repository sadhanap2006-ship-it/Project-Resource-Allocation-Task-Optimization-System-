/**
 * SIST Capstone - UI Utility Library
 * Modal dialogs, toasts, confirm prompts, currency & date formatting, and state helpers.
 */

const inrFormatter = new Intl.NumberFormat('en-IN', {
  style: 'currency',
  currency: 'INR',
  maximumFractionDigits: 2
});

export const ui = {
  formatCurrency(amount) {
    if (amount === null || amount === undefined) return '₹0.00';
    return inrFormatter.format(amount);
  },

  formatDate(dateStr) {
    if (!dateStr) return '-';
    const d = new Date(dateStr);
    return d.toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' });
  },

  showToast(message, type = 'info') {
    let container = document.getElementById('toast-container');
    if (!container) {
      container = document.createElement('div');
      container.id = 'toast-container';
      document.body.appendChild(container);
    }

    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;
    toast.innerHTML = `
      <span>${this.escapeHtml(message)}</span>
      <button style="background:none; border:none; color:white; cursor:pointer; margin-left:12px; display:inline-flex; align-items:center;" aria-label="Close">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
      </button>
    `;

    toast.querySelector('button').onclick = () => toast.remove();
    container.appendChild(toast);

    setTimeout(() => {
      toast.style.opacity = '0';
      setTimeout(() => toast.remove(), 200);
    }, 4000);
  },

  confirm(message, title = 'Confirm Action') {
    return new Promise((resolve) => {
      const modal = document.createElement('div');
      modal.className = 'modal-backdrop show';
      modal.innerHTML = `
        <div class="modal-box" style="max-width: 420px;">
          <div class="modal-header">
            <h3>${this.escapeHtml(title)}</h3>
          </div>
          <div class="modal-body">
            <p>${this.escapeHtml(message)}</p>
          </div>
          <div class="modal-footer">
            <button class="btn btn-secondary" id="confirm-cancel">Cancel</button>
            <button class="btn btn-danger" id="confirm-ok">Confirm</button>
          </div>
        </div>
      `;

      document.body.appendChild(modal);

      modal.querySelector('#confirm-cancel').onclick = () => {
        modal.remove();
        resolve(false);
      };

      modal.querySelector('#confirm-ok').onclick = () => {
        modal.remove();
        resolve(true);
      };
    });
  },

  showModal({ title, bodyHtml, confirmText = 'Save', confirmClass = 'btn-primary', onConfirm }) {
    const modal = document.createElement('div');
    modal.className = 'modal-backdrop show';
    modal.innerHTML = `
      <div class="modal-box">
        <div class="modal-header">
          <h3>${this.escapeHtml(title)}</h3>
          <button style="background:none; border:none; color:var(--text-muted); cursor:pointer; display:inline-flex; align-items:center; padding:4px;" id="modal-close" aria-label="Close">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
          </button>
        </div>
        <div class="modal-body">
          ${bodyHtml}
        </div>
        <div class="modal-footer">
          <button class="btn btn-secondary" id="modal-cancel">Cancel</button>
          <button class="btn ${confirmClass}" id="modal-save">${this.escapeHtml(confirmText)}</button>
        </div>
      </div>
    `;

    document.body.appendChild(modal);

    const close = () => modal.remove();
    modal.querySelector('#modal-close').onclick = close;
    modal.querySelector('#modal-cancel').onclick = close;

    modal.querySelector('#modal-save').onclick = async (e) => {
      if (onConfirm) {
        const btn = e.target;
        btn.disabled = true;
        btn.innerText = 'Processing...';
        try {
          const success = await onConfirm(modal);
          if (success !== false) {
            close();
          }
        } finally {
          btn.disabled = false;
          btn.innerText = confirmText;
        }
      } else {
        close();
      }
    };
  },

  renderLoading(containerEl, message = 'Loading data...') {
    if (typeof containerEl === 'string') containerEl = document.getElementById(containerEl);
    if (!containerEl) return;
    containerEl.innerHTML = `
      <div style="text-align: center; padding: 3rem 1rem; color: var(--text-muted);">
        <div style="display: inline-block; width: 28px; height: 28px; border: 3px solid var(--border-color); border-top-color: var(--primary); border-radius: 50%; animation: spin 1s linear infinite; margin-bottom: 0.75rem;"></div>
        <div>${this.escapeHtml(message)}</div>
      </div>
      <style>@keyframes spin { 0% { transform: rotate(0deg); } 100% { transform: rotate(360deg); } }</style>
    `;
  },

  renderEmpty(containerEl, message = 'No records found.') {
    if (typeof containerEl === 'string') containerEl = document.getElementById(containerEl);
    if (!containerEl) return;
    containerEl.innerHTML = `
      <div style="text-align: center; padding: 2.5rem 1rem; color: var(--text-subtle);">
        <div style="margin-bottom: 0.75rem; color: var(--text-subtle); display:flex; justify-content:center;">
          <svg width="42" height="42" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z"/></svg>
        </div>
        <p style="font-size: 0.9rem; color: var(--text-muted);">${this.escapeHtml(message)}</p>
      </div>
    `;
  },

  renderError(containerEl, err, retryFn = null) {
    if (typeof containerEl === 'string') containerEl = document.getElementById(containerEl);
    if (!containerEl) return;
    const msg = err?.message || 'An error occurred while loading content.';
    containerEl.innerHTML = `
      <div style="text-align: center; padding: 2.5rem 1rem; color: var(--danger);">
        <div style="margin-bottom: 0.75rem; color: var(--danger); display:flex; justify-content:center;">
          <svg width="42" height="42" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
        </div>
        <p style="color: var(--danger); font-weight: 500; font-size: 0.9rem;">${this.escapeHtml(msg)}</p>
        ${retryFn ? `<button class="btn btn-secondary btn-sm" id="btn-retry" style="margin-top: 1rem;">Try Again</button>` : ''}
      </div>
    `;
    if (retryFn) {
      containerEl.querySelector('#btn-retry')?.addEventListener('click', retryFn);
    }
  },

  escapeHtml(text) {
    if (text === null || text === undefined) return '';
    return String(text).replace(/[&<>"']/g, m => ({
      '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#039;'
    })[m]);
  }
};
