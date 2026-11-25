const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const test = require('node:test');
const vm = require('node:vm');

const root = path.resolve(__dirname, '..', '..', 'main', 'resources', 'static');
const script = fs.readFileSync(path.join(root, 'js', 'status-colors.js'), 'utf8');
const reviewScript = fs.readFileSync(path.join(root, 'js', 'review-status.js'), 'utf8');

test('custom and existing Bootstrap colors render with white badge text', () => {
  const context = vm.createContext({ console });
  const colors = vm.runInContext(`${script}\nStatusColors`, context);
  assert.equal(colors.hex('success'), '#198754');
  assert.equal(colors.hex('#A14F90'), '#a14f90');
  assert.equal(colors.hex('red;display:none'), null);
  assert.equal(colors.hex('constructor'), null);
  assert.match(colors.badgeStyle('#ffffff'), /color:#ffffff/);
  assert.match(colors.badgeStyle('#000000'), /color:#ffffff/);
});

test('status colors apply to the matching dashboard cards', () => {
  const applied = {};
  const document = {
    getElementById(id) {
      return { closest: () => ({ style: {
        setProperty: (name, value) => { applied[id] = value; },
        removeProperty: () => { delete applied[id]; }
      } }) };
    }
  };
  const context = vm.createContext({ console, document });
  const colors = vm.runInContext(`${script}\nStatusColors`, context);
  colors.applyCards({ DIHANTAR: { color: '#123456' }, DILULUSKAN: { color: 'success' } },
    { pending: 'DIHANTAR', approved: 'DILULUSKAN', rejected: 'DITOLAK' });
  assert.equal(applied.pending, '#123456');
  assert.equal(applied.approved, '#198754');
  assert.equal(applied.rejected, undefined);
  colors.applyCards({ DIHANTAR: { color: 'secondary' } }, { pending: 'DIHANTAR' });
  assert.equal(applied.pending, '#6c757d');
});

test('dashboard scripts load saved colors for the relevant cards', async () => {
  for (const file of ['admin.html', 'petugas.html', 'penyelia-loji.html']) {
    const html = fs.readFileSync(path.join(root, file), 'utf8');
    for (const match of html.matchAll(/<script(?:\s[^>]*)?>([\s\S]*?)<\/script>/g)) {
      if (match[1].trim()) new vm.Script(match[1]);
    }
  }
  const settings = [
    { key: 'DIHANTAR', value: { color: '#123456' } },
    { key: 'DILULUSKAN', value: { color: '#abcdef' } },
    { key: 'PAS_DIKELUARKAN', value: { color: '#aa4400' } }
  ];
  for (const file of ['petugas.html', 'penyelia-loji.html']) {
    const html = fs.readFileSync(path.join(root, file), 'utf8');
    const start = html.indexOf('    async function loadStatusSettings()');
    const remaining = html.slice(start);
    const end = remaining.slice(1).search(/\n    (?:async )?function /) + 1;
    const applied = {};
    const context = vm.createContext({
      console,
      fetch: async () => ({ ok: true, json: async () => settings }),
      document: { getElementById: (id) => ({
        closest: () => ({ style: {
          setProperty: (name, value) => { applied[id] = value; },
          removeProperty: () => { delete applied[id]; }
        } })
      }) }
    });
    vm.runInContext(`${script}\nlet statusSettings = {};\n${remaining.slice(0, end)}`, context);
    await context.loadStatusSettings();
    if (file === 'petugas.html') {
      assert.equal(applied['stat-pending-review'], '#0d6efd');
      assert.equal(applied['stat-pending-unreviewed'], '#123456');
      assert.equal(applied['stat-approved'], '#abcdef');
    } else {
      assert.equal(applied['stat-pas-issued'], '#aa4400');
    }
  }
});

test('both dashboards count reviewed and unreviewed applications separately', async () => {
  const apps = [
    { status: 'DIHANTAR', kppNote: 'OK' },
    { status: 'DIHANTAR', kppNote: '  ' },
    { status: 'SEMAKAN_STAFF', kppNote: 'lama' },
    { status: 'PAS_DIKELUARKAN' }
  ];
  for (const file of ['petugas.html', 'penyelia-loji.html']) {
    const html = fs.readFileSync(path.join(root, file), 'utf8');
    const staff = file === 'petugas.html';
    const start = html.indexOf(staff ? '    async function loadDashboardStats()' : '    async function loadPenyeliaTable()');
    const end = html.indexOf(staff ? '    function debounce(' : '    function renderTablePage()', start);
    const elements = {};
    const context = vm.createContext({
      console,
      document: { querySelector: (selector) => elements[selector] ||= {} },
      fetch: async () => ({ ok: true, json: async () => [...apps] }),
      updateKppNotification() {}, renderPieChart() {}, renderCategoryChart() {},
      renderStatusTrendChart() {}, renderTablePage() {}
    });
    vm.runInContext(`${reviewScript}\nlet allApplications = []; let kppReturnedApplications = []; let cachedApplications = []; let currentPage = 1;\n${html.slice(start, end)}`, context);
    await context[staff ? 'loadDashboardStats' : 'loadPenyeliaTable']();
    assert.equal(elements['#stat-pending-review'].textContent, 1);
    assert.equal(elements['#stat-pending-unreviewed'].textContent, 2);
    assert.equal(elements['#stat-total'].textContent, 4);
    assert(!html.includes('id="pie-legend"'));
  }
});

test('settings fields provide help and preserve existing colors in the picker', () => {
  const html = fs.readFileSync(path.join(root, 'admin.html'), 'utf8');
  const start = html.indexOf('    const SETTING_CATEGORIES =');
  const end = html.indexOf('    async function loadSettings(', start);
  const context = vm.createContext({ console });
  vm.runInContext(`${script}\n${html.slice(start, end)}`, context);
  const result = vm.runInContext(`({
    announcement: renderSettingField(SETTING_CATEGORIES.announcement.fields[0], 'notis-air'),
    smtp: renderSettingField(SETTING_CATEGORIES.smtp.fields[1], 'smtp.example.com'),
    template: renderSettingField(SETTING_CATEGORIES['email-template'].fields[3], '<p>{{code}}</p>'),
    color: renderSettingField(SETTING_CATEGORIES.status.fields[2], 'success')
  })`, context);
  for (const field of [result.announcement, result.smtp, result.template, result.color]) {
    assert.match(field, /<button type="button" class="setting-help"[^>]*>\?<\/button>/);
    assert.match(field, /data-help-content=/);
  }
  assert.match(result.announcement, /value="notis-air"/);
  assert.match(result.template, /&lt;p&gt;{{code}}&lt;\/p&gt;/);
  assert.match(result.color, /type="color" value="#198754"/);
});

test('admin previews display both review states with white text', () => {
  const html = fs.readFileSync(path.join(root, 'admin.html'), 'utf8');
  const configStart = html.indexOf('    const SETTING_CATEGORIES =');
  const configEnd = html.indexOf('    async function loadSettings(', configStart);
  const cellsStart = html.indexOf('    function settingCells(');
  const cellsEnd = html.indexOf('    function renderSettings(', cellsStart);
  const context = vm.createContext({ console });
  vm.runInContext(`${script}\n${html.slice(configStart, configEnd)}\n${html.slice(cellsStart, cellsEnd)}\nactiveSettingCategory = 'status';`, context);
  const preview = context.settingCells({ key: 'DIHANTAR', value: {
    label: 'Menunggu Semakan', color: '#112233', reviewedColor: '#445566'
  } })[1];
  assert.match(preview, /background-color:#112233;color:#ffffff/);
  assert.match(preview, /background-color:#445566;color:#ffffff/);
});

test('completed and pending reviews have distinct default and configurable colors', () => {
  const html = fs.readFileSync(path.join(root, 'petugas.html'), 'utf8');
  const badgeStart = html.indexOf('    function renderStatusBadge(');
  const badgeEnd = html.indexOf('    async function loadStatusSettings()', badgeStart);
  const context = vm.createContext({ console, escapeHtml: (value) => value });
  vm.runInContext(`${script}\n${reviewScript}\nlet statusSettings = { DIHANTAR: {label:'Menunggu Semakan', color:'secondary'} };\n${html.slice(badgeStart, badgeEnd)}`, context);
  const pending = { status: 'DIHANTAR', kppNote: '' };
  const reviewed = { status: 'DIHANTAR', kppNote: 'Selesai' };
  assert.match(context.renderStatusBadge('DIHANTAR', pending), /background-color:#6c757d/);
  assert.match(context.renderStatusBadge('DIHANTAR', reviewed), /background-color:#0d6efd/);
  assert.match(context.renderStatusBadge('DIHANTAR', reviewed), /color:#ffffff/);
  vm.runInContext("statusSettings.DIHANTAR.reviewedColor = '#123456'", context);
  assert.match(context.renderStatusBadge('DIHANTAR', reviewed), /background-color:#123456/);
  assert.match(context.renderStatusBadge('DIHANTAR', pending), /background-color:#6c757d/);
});

test('staff charts split pending reviews and use saved colors', () => {
  const html = fs.readFileSync(path.join(root, 'petugas.html'), 'utf8');
  const start = html.indexOf('    function getChartStatusCounts(');
  const end = html.indexOf('    function escapeHtml(', start);
  const charts = [];
  const canvas = { getContext: () => ({}), setAttribute: (name, value) => { canvas[name] = value; } };
  const context = vm.createContext({
    console,
    document: { getElementById: () => canvas },
    Chart: class {
      constructor(ctx, config) { this.config = config; charts.push(this); }
      destroy() { this.destroyed = true; }
    }
  });
  vm.runInContext(`${script}\n${reviewScript}\nlet statusSettings = {
    DIHANTAR:{color:'#112233',reviewedColor:'#445566'},
    DILULUSKAN:{color:'#778899'}, DITOLAK:{color:'#aa1122'},
    MENUNGGU_PENGARAH:{color:'#bb2233'}, SELESAI:{color:'#cc3344'}
  }; let pieChartInstance = null; let categoryChartInstance = null;\n${html.slice(start, end)}`, context);
  const apps = [
    { status: 'DIHANTAR' }, { status: 'DIHANTAR', kppNote: 'OK' },
    { status: 'SEMAKAN_STAFF' }, { status: 'PAS_DIKELUARKAN' }, { status: 'DILULUSKAN' }
  ];
  context.renderPieChart(apps);
  context.renderCategoryChart(apps);
  assert.deepEqual(Array.from(charts[0].config.data.datasets[0].data), [2, 1, 0, 2, 0, 0]);
  assert.deepEqual(Array.from(charts[0].config.data.datasets[0].backgroundColor),
    ['#112233', '#445566', '#bb2233', '#778899', '#aa1122', '#cc3344']);
  const tooltip = charts[0].config.options.plugins.tooltip.callbacks;
  assert.equal(tooltip.title(), '');
  assert.equal(tooltip.label({ label: 'Semakan Belum Selesai', raw: 2,
    dataset: charts[0].config.data.datasets[0] }), 'Semakan Belum Selesai: 40.0%');
  assert.deepEqual(Array.from(charts[1].config.data.datasets[0].pointBackgroundColor),
    ['#778899', '#aa1122', '#112233', '#445566', '#bb2233', '#cc3344']);
  assert.match(canvas['aria-label'], /Semakan Belum Selesai: 2/);
  assert.equal(charts[1].config.options.scales.x.ticks.display, false);
  vm.runInContext("statusSettings.DIHANTAR.reviewedColor = '#abcdef'", context);
  context.renderPieChart(apps);
  assert.equal(charts[0].destroyed, true);
  assert.equal(charts[2].config.data.datasets[0].backgroundColor[1], '#abcdef');
  context.renderPieChart([]);
  assert.deepEqual(Array.from(charts[3].config.data.datasets[0].backgroundColor), ['#e9ecef']);
  assert.equal(charts[3].config.options.plugins.tooltip.callbacks.label({}), 'Tiada data');
});

test('supervisor pie and trend chart use the edited status colors', () => {
  const html = fs.readFileSync(path.join(root, 'penyelia-loji.html'), 'utf8');
  const start = html.indexOf('    function getSupervisorChartData(');
  const end = html.indexOf('    function showCatatanPenyeliaModal(', start);
  const charts = [];
  const canvas = { getContext: () => ({}), setAttribute: (name, value) => { canvas[name] = value; } };
  const context = vm.createContext({
    console,
    document: { getElementById: () => canvas },
    Chart: class {
      constructor(ctx, config) { this.config = config; charts.push(this); }
      destroy() {}
    }
  });
  vm.runInContext(`${script}\n${reviewScript}\nlet statusSettings = {
    MENUNGGU_PENGARAH:{color:'#112233'}, PAS_DIKELUARKAN:{color:'#445566'},
    DITOLAK:{color:'#778899'}, SELESAI:{color:'#aabbcc'}
  }; let pieChartInstance = null; let statusTrendChartInstance = null;\n${html.slice(start, end)}`, context);
  const apps = [{ status: 'PAS_DIKELUARKAN' }, { status: 'SELESAI' },
    { status: 'DIHANTAR' }, { status: 'DIHANTAR', kppNote: 'OK' }];
  context.renderPieChart(apps);
  context.renderStatusTrendChart(apps);
  assert.deepEqual(Array.from(charts[0].config.data.datasets[0].backgroundColor),
    ['#6c757d', '#0d6efd', '#112233', '#445566', '#778899', '#aabbcc']);
  assert.deepEqual(Array.from(charts[1].config.data.datasets[0].pointBackgroundColor),
    ['#6c757d', '#0d6efd', '#112233', '#445566', '#778899', '#aabbcc']);
  assert.deepEqual(Array.from(charts[0].config.data.datasets[0].data), [1, 1, 0, 1, 0, 1]);
  const tooltip = charts[0].config.options.plugins.tooltip.callbacks;
  assert.equal(tooltip.title(), '');
  assert.equal(tooltip.label({ label: 'Menunggu Lawatan', raw: 1,
    dataset: charts[0].config.data.datasets[0] }), 'Menunggu Lawatan: 25.0%');
  assert.match(canvas['aria-label'], /Semakan Selesai: 1/);
  assert.equal(charts[1].config.options.scales.x.ticks.display, false);
});

test('open dashboard tabs reload settings and redraw charts after an admin edit', async () => {
  for (const file of ['petugas.html', 'penyelia-loji.html']) {
    const html = fs.readFileSync(path.join(root, file), 'utf8');
    const start = html.indexOf("      window.addEventListener('storage', async");
    const end = html.indexOf('      });', start) + '      });'.length;
    const calls = [];
    let callback;
    const context = vm.createContext({
      window: { addEventListener: (event, handler) => { callback = handler; } },
      cachedApplications: [{ status: 'SELESAI' }],
      loadStatusSettings: async () => { calls.push('settings'); },
      loadDashboardStats: async () => { calls.push('dashboard'); },
      loadPermohonanTable: async () => { calls.push('table'); },
      renderPieChart: () => { calls.push('pie'); },
      renderStatusTrendChart: () => { calls.push('trend'); },
      renderTablePage: () => { calls.push('table'); }
    });
    vm.runInContext(html.slice(start, end), context);
    await callback({ key: 'otherSetting' });
    assert.deepEqual(calls, []);
    await callback({ key: 'statusSettingsUpdated' });
    assert.deepEqual(calls, file === 'petugas.html'
      ? ['settings', 'dashboard', 'table']
      : ['settings', 'pie', 'trend', 'table']);
  }
});

test('supervisor badge follows admin colors and distinguishes reviewed applications', () => {
  const html = fs.readFileSync(path.join(root, 'penyelia-loji.html'), 'utf8');
  const start = html.indexOf('    function penyeliaStatusBadge(');
  const end = html.indexOf('    function formatDisplayDate(', start);
  const context = vm.createContext({ console, esc: (value) => value });
  vm.runInContext(`${script}\n${reviewScript}\nlet statusSettings = {PAS_DIKELUARKAN:{color:'success'}, DIHANTAR:{color:'secondary',reviewedColor:'primary'}};\n${html.slice(start, end)}`, context);
  assert.match(context.penyeliaStatusBadge('PAS_DIKELUARKAN'), /background-color:#198754/);
  vm.runInContext("statusSettings.PAS_DIKELUARKAN.color = '#aa4400'", context);
  assert.match(context.penyeliaStatusBadge('PAS_DIKELUARKAN'), /background-color:#aa4400/);
  assert.match(context.penyeliaStatusBadge('DIHANTAR', { status: 'DIHANTAR' }), /background-color:#6c757d/);
  assert.match(context.penyeliaStatusBadge('DIHANTAR', { status: 'DIHANTAR', kppNote: 'OK' }), /background-color:#0d6efd/);
});

test('help opens as a dismissible popover and unchanged colors keep their original values', async () => {
  const html = fs.readFileSync(path.join(root, 'admin.html'), 'utf8');
  const configStart = html.indexOf('    const SETTING_CATEGORIES =');
  const configEnd = html.indexOf('    async function loadSettings(', configStart);
  const modalStart = html.indexOf('    function showSettingModal(');
  const modalEnd = html.indexOf("    document.addEventListener('DOMContentLoaded'", modalStart);
  const handlers = {};
  const popovers = [];
  const button = {
    dataset: { helpContent: 'Panduan warna' },
    attributes: { 'aria-expanded': 'false' },
    addEventListener: (name, handler) => { handlers.help = handler; },
    getAttribute(name) { return this.attributes[name]; },
    setAttribute(name, value) { this.attributes[name] = value; }
  };
  const form = {
    querySelectorAll: () => [button],
    addEventListener: (name, handler) => { handlers.submit = handler; }
  };
  const overlay = {
    querySelector: (selector) => selector === '#setting-form' ? form : {
      addEventListener: (name, handler) => { handlers.scroll = handler; }
    },
    addEventListener: (name, handler) => { handlers.overlay = handler; }
  };
  const context = vm.createContext({
    console,
    document: {
      createElement: () => ({ querySelector: () => ({ addEventListener() {} }) }),
      addEventListener: (name, handler) => { handlers[name] = handler; },
      removeEventListener: (name) => { delete handlers[name]; }
    },
    bootstrap: { Popover: class {
      constructor() { this.visible = false; popovers.push(this); }
      show() { this.visible = true; }
      hide() { this.visible = false; }
      dispose() { this.disposed = true; }
    } },
    createModalShell: () => ({ overlay, card: { appendChild() {} }, close() {} }),
    FormData: class {
      get(name) { return { key: 'DIHANTAR', label: 'Menunggu Semakan', color: '#6c757d',
        reviewedColor: '#0d6efd', description: 'Ulasan' }[name]; }
      has() { return false; }
    }
  });
  vm.runInContext(`${script}\n${html.slice(configStart, configEnd)}\n${html.slice(modalStart, modalEnd)}\nactiveSettingCategory = 'status';`, context);
  const result = context.showSettingModal({ key: 'DIHANTAR', value: { color: 'secondary' } });
  handlers.help();
  assert.equal(popovers[0].visible, true);
  assert.equal(button.getAttribute('aria-expanded'), 'true');
  handlers.keydown({ key: 'Escape' });
  assert.equal(popovers[0].visible, false);
  handlers.submit({ preventDefault() {} });
  const payload = await result;
  assert.equal(payload.value.color, 'secondary');
  assert.equal(payload.value.reviewedColor, undefined);
  assert.equal(popovers[0].disposed, true);
  assert.equal(handlers.click, undefined);
});
