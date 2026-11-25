const StatusColors = (() => {
  const bootstrap = {
    primary: '#0d6efd',
    secondary: '#6c757d',
    success: '#198754',
    danger: '#dc3545',
    warning: '#ffc107',
    info: '#0dcaf0',
    dark: '#212529'
  };
  function hex(value) {
    const color = String(value || '').toLowerCase();
    if (Object.prototype.hasOwnProperty.call(bootstrap, color)) return bootstrap[color];
    return /^#[0-9a-f]{6}$/.test(color) ? color : null;
  }

  function badgeStyle(value) {
    let color = hex(value);
    if (!color) {
      console.warn('Warna status tidak sah:', value);
      color = bootstrap.secondary;
    }
    return `background-color:${color};color:#ffffff`;
  }

  function chartColor(settings, status, fallback, field = 'color') {
    const value = settings[status]?.[field];
    if (!value) return fallback;
    const color = hex(value);
    if (!color) {
      console.warn('Warna status tidak sah:', value);
      return fallback;
    }
    return color;
  }

  async function load() {
    const response = await fetch('api/public/settings/statuses', { cache: 'no-store' });
    if (!response.ok) throw new Error('Konfigurasi status tidak dapat dimuatkan');
    const settings = await response.json();
    return Object.fromEntries(settings.map((item) => [String(item.key).toUpperCase(), item.value]));
  }

  function applyCards(settings, cards) {
    Object.entries(cards).forEach(([id, config]) => {
      const { status, field, fallback } = typeof config === 'string'
        ? { status: config, field: 'color' }
        : { field: 'color', ...config };
      const value = settings[status]?.[field] || fallback;
      const style = document.getElementById(id)?.closest('.col')?.style;
      if (!style) return;
      if (!value) {
        style.removeProperty('--kpi-color');
        return;
      }
      const color = hex(value);
      if (!color) {
        console.warn('Warna status tidak sah:', value);
        return;
      }
      style.setProperty('--kpi-color', color);
    });
  }

  return { hex, badgeStyle, chartColor, load, applyCards };
})();
