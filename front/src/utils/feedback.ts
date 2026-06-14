export const notify = (msg: string, type: 'info' | 'error' | 'success' = 'info') => {
  const existing = document.querySelectorAll('.custom-notification')
  existing.forEach(el => el.remove())

  const el = document.createElement('div')
  el.className = `custom-notification notification-${type}`
  el.innerHTML = `
    <div class="notification-content">
      <div class="notification-icon">${type === 'success' ? 'OK' : type === 'error' ? '!' : '>'}</div>
      <div class="notification-text">${msg.toUpperCase()}</div>
    </div>
  `

  document.body.appendChild(el)

  setTimeout(() => {
    el.classList.add('is-visible')
  }, 10)

  setTimeout(() => {
    el.classList.remove('is-visible')
  }, 3000)

  setTimeout(() => {
    if (el.parentNode) el.remove()
  }, 3500)
}

export const injectNotificationStyles = () => {
  if (document.querySelector('#notification-styles')) return
  const style = document.createElement('style')
  style.id = 'notification-styles'
  style.textContent = `
    .custom-notification {
      position: fixed;
      bottom: 2rem;
      right: 2rem;
      min-width: 280px;
      padding: 0.75rem 1.25rem;
      z-index: 2147483647 !important;
      font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
      font-size: 0.875rem;
      border: 1px solid #000;
      box-shadow: 8px 8px 0px rgba(0,0,0,0.1);
      background: white;
      color: black;
      opacity: 0;
      transform: translateY(1rem);
      transition: all 0.4s cubic-bezier(0.16, 1, 0.3, 1);
      pointer-events: none;
    }

    .custom-notification.is-visible {
      opacity: 1;
      transform: translateY(0);
    }

    .notification-content {
      display: flex;
      align-items: center;
      gap: 0.75rem;
    }

    .notification-icon {
      font-weight: bold;
      flex-shrink: 0;
    }

    .notification-text {
      letter-spacing: 0;
      line-height: 1.2;
    }

    .notification-success {
      background: #18181b;
      color: #ffffff;
      border-color: #000;
    }

    .notification-error {
      background: #ffffff;
      color: #dc2626;
      border-color: #dc2626;
      box-shadow: 8px 8px 0px rgba(220, 38, 38, 0.1);
    }

    .dark .notification-success {
      background: #ffffff;
      color: #000000;
      border-color: #ffffff;
    }

    .dark .notification-error {
      background: #18181b;
      color: #f87171;
      border-color: #f87171;
    }
  `
  document.head.appendChild(style)
}
