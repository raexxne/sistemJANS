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
  let snapshot = 0;
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
    editFormSnapshot: () => snapshot++,
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

test('settings changes show success after saving and refreshing', async () => {
  const html = fs.readFileSync(path.join(root, 'admin.html'), 'utf8');
  const start = html.indexOf('    function settingEscape(');
  const end = html.indexOf('    function renderSettingField(', start);
  const requests = [];
  const messages = [];
  const refreshes = [];
  const updates = [];
  const context = vm.createContext({
    console,
    fetch: async (url, options) => { requests.push({ url, options }); return { ok: true, json: async () => ({}) }; },
    showInfoModal: async (title, message, type) => { messages.push({ title, message, type }); },
    loadSettings: async (category) => { refreshes.push(category); },
    localStorage: { setItem: (key) => { updates.push(key); } }
  });
  vm.runInContext(`let activeSettingCategory = 'announcement';\n${html.slice(start, end)}`, context);
  await context.saveSettingChange('announcement', 'POST', null, { key: 'notis' });
  await context.saveSettingChange('announcement', 'PUT', 42, { key: 'notis' });
  await context.saveSettingChange('announcement', 'DELETE', 42);
  vm.runInContext("activeSettingCategory = 'status'", context);
  await context.saveSettingChange('status', 'PUT', 7, { key: 'DIHANTAR' });
  assert.deepEqual(requests.map((request) => request.options.method), ['POST', 'PUT', 'DELETE', 'PUT']);
  assert.equal(requests[0].url, 'api/admin/settings/announcement');
  assert.equal(requests[1].url, 'api/admin/settings/announcement/42');
  assert.equal(requests[2].options.body, undefined);
  assert.deepEqual(refreshes, ['announcement', 'announcement', 'announcement', 'status']);
  assert.deepEqual(updates, ['statusSettingsUpdated']);
  assert.deepEqual(messages.map((message) => message.title), Array(4).fill('Perubahan Berjaya'));
  assert.deepEqual(messages.map((message) => message.type), Array(4).fill('success'));
  assert.match(messages[0].message, /ditambah/);
  assert.match(messages[1].message, /dikemas kini/);
  assert.match(messages[2].message, /dipadam/);
});

test('settings failures show server and connection errors without reporting success', async () => {
  const html = fs.readFileSync(path.join(root, 'admin.html'), 'utf8');
  const start = html.indexOf('    function settingEscape(');
  const end = html.indexOf('    function renderSettingField(', start);
  const messages = [];
  let fetchResult = async () => ({ ok: false, status: 400, json: async () => ({ ralat: '<tidak sah>' }) });
  const context = vm.createContext({
    console: { error() {}, warn() {} },
    fetch: (...args) => fetchResult(...args),
    showInfoModal: async (title, message, type) => { messages.push({ title, message, type }); },
    loadSettings: () => { throw new Error('Failure must not refresh settings'); }
  });
  vm.runInContext(`let activeSettingCategory = 'announcement';\n${html.slice(start, end)}`, context);
  await context.saveSettingChange('announcement', 'POST', null, {});
  fetchResult = async () => ({ ok: false, status: 500, json: async () => { throw new Error('Invalid JSON'); } });
  await context.saveSettingChange('announcement', 'PUT', 1, {});
  fetchResult = async () => { throw new Error('Offline'); };
  await context.saveSettingChange('announcement', 'DELETE', 1);
  fetchResult = async () => ({ ok: true, json: async () => { throw new Error('Invalid JSON'); } });
  await context.saveSettingChange('announcement', 'POST', null, {});
  assert.deepEqual(messages.map((message) => message.title), [
    'Perubahan Tidak Berjaya', 'Perubahan Tidak Berjaya', 'Perubahan Tidak Dapat Disahkan', 'Perubahan Tidak Dapat Disahkan'
  ]);
  assert.deepEqual(messages.map((message) => message.type), ['danger', 'danger', 'warning', 'warning']);
  assert.match(messages[0].message, /&lt;tidak sah&gt;/);
  assert.match(messages[1].message, /HTTP 500/);
  assert.match(messages[2].message, /masalah sambungan/);
  assert.match(messages[3].message, /semak tetapan/);
});

test('admin notifications use one coloured OK button and return focus when closed', async () => {
  const html = fs.readFileSync(path.join(root, 'admin.html'), 'utf8');
  const start = html.indexOf('    function showInfoModal(');
  const end = html.indexOf('    function showEditModal(', start);
  const overlays = [];
  let keyHandler;
  let restored = false;
  const document = {
    activeElement: { isConnected: true, focus() { restored = true; } },
    createElement() {
      const overlay = {
        button: {
          addEventListener(event, handler) { this.handler = handler; },
          focus() { this.focused = true; }
        },
        querySelector() { return this.button; },
        remove() { this.removed = true; }
      };
      overlays.push(overlay);
      return overlay;
    },
    body: { appendChild() {} },
    addEventListener(event, handler, capture) { assert.equal(capture, true); keyHandler = handler; },
    removeEventListener(event, handler, capture) {
      assert.equal(handler, keyHandler);
      assert.equal(capture, true);
      keyHandler = null;
    }
  };
  const context = vm.createContext({ document });
  const helperStart = html.indexOf('    function editFormSnapshot(');
  const helperEnd = html.indexOf('    function showEditModal(', helperStart);
  vm.runInContext(`${html.slice(start, end)}\n${html.slice(helperStart, helperEnd)}`, context);
  for (const type of ['success', 'danger', 'warning', 'primary']) {
    restored = false;
    const result = context.showInfoModal('Notifikasi', '<p>Maklumat</p>', type);
    const overlay = overlays.at(-1);
    assert.match(overlay.innerHTML, new RegExp(`class="btn btn-${type}">OK</button>`));
    assert.equal((overlay.innerHTML.match(/<button\b/g) || []).length, 1);
    assert.equal(overlay.button.focused, true);
    overlay.button.handler();
    assert.equal(await result, true);
    assert.equal(overlay.removed, true);
    assert.equal(restored, true);
    assert.equal(keyHandler, null);
  }
  const result = context.showInfoModal('Maklumat', '<p>Maklumat</p>');
  let stopped = false;
  keyHandler({ key: 'Escape', stopImmediatePropagation() { stopped = true; } });
  assert.equal(await result, true);
  assert.equal(stopped, true);
  assert.equal(keyHandler, null);
  const noChanges = context.showNoChanges();
  const overlay = overlays.at(-1);
  assert.doesNotMatch(overlay.innerHTML, /<h2\b/);
  assert.match(overlay.innerHTML, /Tiada perubahan berlaku\./);
  assert.match(overlay.innerHTML, /aria-label="Makluman"/);
  overlay.button.handler();
  await noChanges;
});

test('unchanged user and location edits show notice and do not submit', async () => {
  const html = fs.readFileSync(path.join(root, 'admin.html'), 'utf8');
  const helpers = html.slice(html.indexOf('    function editFormSnapshot('), html.indexOf('    function showEditModal('));
  for (const [name, next, values] of [
    ['showEditModal', 'showTetapkanDaerahModal', { name: 'Ali', email: 'ali@example.com', phone: '', address: '', role: 'STAFF' }],
    ['showEditLokasiModal', null, { type: 'LOJI', name: 'Loji A', daerah: '' }]
  ]) {
    const start = html.indexOf(`    function ${name}(`);
    const end = next ? html.indexOf(`    async function ${next}(`, start) : html.indexOf('    const SETTING_CATEGORIES', start);
    let submit;
    let notices = 0;
    let closed = false;
    const form = { values: { ...values }, addEventListener(event, handler) { if (event === 'submit') submit = handler; } };
    const overlay = {
      querySelector(selector) {
        if (selector.endsWith('-form')) return form;
        if (selector === '#edit-user-role') return { addEventListener() {} };
        if (selector === '#edit-user-daerah-row') return {};
        return { addEventListener() {} };
      },
      addEventListener() {}
    };
    const context = vm.createContext({
      document: { createElement: () => ({ querySelector: () => ({ addEventListener() {} }) }) },
      FormData: class { constructor(f) { this.values = f.values; } entries() { return Object.entries(this.values)[Symbol.iterator](); } },
      createModalShell: () => ({ overlay, card: { appendChild() {} }, close() { closed = true; } }),
      showInfoModal: async () => { notices++; }
    });
    vm.runInContext(`${helpers}\n${html.slice(start, end)}`, context);
    const result = context[name](values);
    await submit({ preventDefault() {} });
    assert.equal(notices, 1);
    assert.equal(closed, false);
    form.values.name += ' baharu';
    await submit({ preventDefault() {} });
    assert.equal((await result).name, values.name + ' baharu');
    assert.equal(closed, true);
  }
});

test('settings snapshots compare normalized fields, passwords and checkbox changes', () => {
  const html = fs.readFileSync(path.join(root, 'admin.html'), 'utf8');
  const start = html.indexOf('    function editFormSnapshot(');
  const end = html.indexOf('    function showEditModal(', start);
  const context = vm.createContext({
    FormData: class { constructor(f) { this.values = f.values; } entries() { return Object.entries(this.values)[Symbol.iterator](); } }
  });
  vm.runInContext(html.slice(start, end), context);
  const fields = [{ name: 'port', type: 'number' }, { name: 'password', type: 'password' }, { name: 'auth', type: 'checkbox' }];
  const form = { values: { port: '587', password: '********', auth: 'on' } };
  const initial = context.editFormSnapshot(form, fields, true);
  form.values.password = '';
  form.values.port = '0587';
  assert.equal(context.editFormSnapshot(form, fields, true), initial);
  form.values.password = 'new-password';
  assert.notEqual(context.editFormSnapshot(form, fields, true), initial);
  form.values.password = '********';
  delete form.values.auth;
  assert.notEqual(context.editFormSnapshot(form, fields, true), initial);
  const selection = { querySelectorAll: () => [{ value: 'B' }, { value: 'A' }] };
  assert.equal(context.selectedDaerahSnapshot(selection), '["A","B"]');
});

test('unchanged settings edits keep the form open, while changed values submit', async () => {
  const html = fs.readFileSync(path.join(root, 'admin.html'), 'utf8');
  const helpers = html.slice(html.indexOf('    function editFormSnapshot('), html.indexOf('    function showEditModal('));
  const config = html.slice(html.indexOf('    const SETTING_CATEGORIES ='), html.indexOf('    async function loadSettings('));
  const modal = html.slice(html.indexOf('    function showSettingModal('), html.indexOf("    document.addEventListener('DOMContentLoaded'"));
  let submit;
  let notices = 0;
  let closed = false;
  const form = {
    values: { key: 'notis', title: 'Tajuk', message: 'Maklumat', active: 'on', sortOrder: '1' },
    querySelectorAll: () => [],
    addEventListener(event, handler) { if (event === 'submit') submit = handler; }
  };
  const overlay = {
    querySelector(selector) { return selector === '#setting-form' ? form : { addEventListener() {} }; },
    addEventListener() {}
  };
  const context = vm.createContext({
    console,
    document: {
      createElement: () => ({ querySelector: () => ({ addEventListener() {} }) }),
      addEventListener() {}, removeEventListener() {}
    },
    FormData: class {
      constructor(f) { this.values = f.values; }
      entries() { return Object.entries(this.values)[Symbol.iterator](); }
      get(name) { return this.values[name] ?? null; }
      has(name) { return Object.hasOwn(this.values, name); }
    },
    bootstrap: { Popover: class {} },
    createModalShell: () => ({ overlay, card: { appendChild() {} }, close() { closed = true; } }),
    showInfoModal: async () => { notices++; }
  });
  vm.runInContext(`${script}\n${helpers}\n${config}\n${modal}`, context);
  const result = context.showSettingModal({ key: 'notis', value: { title: 'Tajuk', message: 'Maklumat', active: true, sortOrder: 1 } });
  await submit({ preventDefault() {} });
  assert.equal(notices, 1);
  assert.equal(closed, false);
  form.values.title = 'Tajuk baharu';
  await submit({ preventDefault() {} });
  assert.equal((await result).value.title, 'Tajuk baharu');
  assert.equal(closed, true);
});

test('unchanged supervisor districts are not saved', async () => {
  const html = fs.readFileSync(path.join(root, 'admin.html'), 'utf8');
  const helpers = html.slice(html.indexOf('    function editFormSnapshot('), html.indexOf('    function showEditModal('));
  const start = html.indexOf('    async function showTetapkanDaerahModal(');
  const end = html.indexOf('    function renderDaerahCheckboxesHtml(', start);
  let submit;
  let notices = 0;
  const requests = [];
  const form = {
    selected: ['A'],
    querySelectorAll() { return this.selected.map((value) => ({ value })); },
    addEventListener(event, handler) { if (event === 'submit') submit = handler; }
  };
  const context = vm.createContext({
    console,
    document: { createElement: () => ({ querySelector: () => ({ addEventListener() {} }) }) },
    fetch: async (url, options) => {
      requests.push({ url, options });
      if (options?.method === 'POST') return { ok: true, json: async () => ({ berjaya: true }) };
      return { ok: true, json: async () => url.includes('/daerah') ? { daerah: ['A'] } : [{ daerah: 'A' }, { daerah: 'B' }] };
    },
    renderDaerahCheckboxesHtml: () => '',
    createModalShell: () => ({
      overlay: { querySelector: () => form, addEventListener() {} },
      card: { appendChild() {} },
      close() {}
    }),
    showInfoModal: async () => { notices++; }
  });
  vm.runInContext(`${helpers}\n${html.slice(start, end)}`, context);
  const result = context.showTetapkanDaerahModal(1);
  await new Promise((resolve) => setImmediate(resolve));
  await submit({ preventDefault() {} });
  assert.equal(notices, 1);
  assert.equal(requests.length, 2);
  form.selected.push('B');
  await submit({ preventDefault() {} });
  assert.equal(await result, true);
  assert.deepEqual(JSON.parse(requests[2].options.body), { daerah: ['A', 'B'] });
});
