// ============================================
//  PENGATURAN URL (ngrok)
// ============================================
const BASE_URL = 'https://impart-cube-crabgrass.ngrok-free.dev';

/**
 * Escape HTML special characters
 */
const esc = (s) => String(s ?? '')
  .replace(/[&<>"']/g, (c) => ({
    '&': '&amp;',
    '<': '&lt;',
    '>': '&gt;',
    '"': '&quot;',
    "'": '&#39;'
  }[c]));

/**
 * Custom Styled Confirm Modal Pop-up
 * Menggantikan confirm() standard pelayar dengan UI bermodal bergaya
 */
function showConfirmModal({
  title = 'Pengesahan',
  message = 'Adakah anda pasti?',
  confirmText = 'Ya, Teruskan',
  cancelText = 'Batal',
  type = 'primary', // 'primary', 'success', 'warning', 'danger'
  icon = 'fa-question-circle',
  showCancel = true
}) {
  return new Promise((resolve) => {
    const old = document.querySelector('#custom-confirm-modal');
    if (old) old.remove();

    const colorMap = {
      primary: { bg: 'linear-gradient(135deg, #00467f, #0d5ba5)', btn: 'btn-primary', icon: '#0d5ba5' },
      success: { bg: 'linear-gradient(135deg, #198754, #146c43)', btn: 'btn-success', icon: '#198754' },
      warning: { bg: 'linear-gradient(135deg, #ffc107, #d39e00)', btn: 'btn-warning text-dark', icon: '#ffc107' },
      danger:  { bg: 'linear-gradient(135deg, #dc3545, #b02a37)', btn: 'btn-danger', icon: '#dc3545' }
    };

    const style = colorMap[type] || colorMap.primary;

    const modal = document.createElement('div');
    modal.id = 'custom-confirm-modal';
    modal.style.cssText = `
      display: flex;
      position: fixed;
      top: 0; left: 0; right: 0; bottom: 0;
      background: rgba(0, 0, 0, 0.65);
      backdrop-filter: blur(4px);
      z-index: 10000;
      justify-content: center;
      align-items: center;
      padding: 16px;
      animation: fadeIn 0.2s ease-out;
    `;

    modal.innerHTML = `
      <style>
        @keyframes fadeIn {
          from { opacity: 0; }
          to { opacity: 1; }
        }
        @keyframes scaleUp {
          from { transform: scale(0.9); opacity: 0; }
          to { transform: scale(1); opacity: 1; }
        }
        .custom-modal-card {
          animation: scaleUp 0.25s cubic-bezier(0.175, 0.885, 0.32, 1.275);
        }
      </style>
      <div class="custom-modal-card" style="
        background: #ffffff;
        border-radius: 14px;
        width: 100%;
        max-width: 440px;
        box-shadow: 0 20px 50px rgba(0,0,0,0.3);
        overflow: hidden;
        text-align: center;
      ">
        <div style="
          background: ${style.bg};
          color: white;
          padding: 20px;
          font-size: 18px;
          font-weight: 600;
        ">
          ${esc(title)}
        </div>

        <div style="padding: 28px 24px 20px 24px;">
          <div style="margin-bottom: 16px;">
            <i class="fas ${esc(icon)}" style="font-size: 52px; color: ${style.icon};"></i>
          </div>
          <p style="font-size: 16px; color: #495057; margin-bottom: 0; line-height: 1.5;">
            ${esc(message)}
          </p>
        </div>

        <div style="
          display: flex; 
          gap: 12px; 
          padding: 16px 24px 24px 24px; 
          justify-content: center;
        ">
          ${showCancel ? `
          <button id="modal-btn-cancel" class="btn btn-outline-secondary px-4 py-2" style="border-radius: 8px; font-weight: 500;">
            ${esc(cancelText)}
          </button>` : ''}
          <button id="modal-btn-confirm" class="btn ${style.btn} px-4 py-2" style="border-radius: 8px; font-weight: 600;">
            ${esc(confirmText)}
          </button>
        </div>
      </div>
    `;

    document.body.appendChild(modal);

    const close = (result) => {
      modal.remove();
      resolve(result);
    };

    modal.querySelector('#modal-btn-confirm').onclick = () => close(true);
    modal.querySelector('#modal-btn-cancel')?.addEventListener('click', () => close(false));

    // Keyboard controls
    const onKey = (e) => {
      if (e.key === 'Escape') {
        document.removeEventListener('keydown', onKey);
        close(false);
      }
      if (e.key === 'Enter') {
        document.removeEventListener('keydown', onKey);
        close(true);
      }
    };
    document.addEventListener('keydown', onKey);

    // Klik luar modal = batal
    modal.onclick = (e) => {
      if (e.target === modal) close(false);
    };
  });
}

/**
 * Show error popup modal
 */
function showErrorPopup(title, message, options = {}) {
  const { redirectTo = null } = options;

  let modal = document.querySelector('#error-modal');
  
  if (!modal) {
    modal = document.createElement('div');
    modal.id = 'error-modal';
    modal.style.cssText = `
      display: none;
      position: fixed;
      top: 0; left: 0; right: 0; bottom: 0;
      background: rgba(0, 0, 0, 0.7);
      backdrop-filter: blur(4px);
      z-index: 10000;
      justify-content: center;
      align-items: center;
    `;
    document.body.appendChild(modal);
  }

  const closeModal = () => {
    modal.style.display = 'none';
    if (redirectTo) {
      window.location.href = redirectTo;
    }
  };
  
  modal.innerHTML = `
    <div style="
      background: white;
      padding: 30px;
      border-radius: 12px;
      text-align: center;
      box-shadow: 0 10px 30px rgba(0,0,0,0.3);
      max-width: 450px;
      width: 90%;
    ">
      <div style="margin-bottom: 20px;">
        <i class="fas fa-exclamation-circle" style="font-size: 50px; color: #dc3545;"></i>
      </div>
      <h2 class="h4 mb-3" style="color: #dc3545; font-weight: 600;">${esc(title)}</h2>
      <p class="text-muted mb-4" style="font-size: 15px;">${esc(message)}</p>
      <button id="error-modal-close" class="btn btn-danger px-4 py-2" style="border-radius: 8px;">
        Tutup
      </button>
    </div>
  `;

  modal.querySelector('#error-modal-close').onclick = closeModal;
  modal.onclick = (e) => {
    if (e.target === modal) closeModal();
  };
  modal.style.display = 'flex';
}

/**
 * Show success popup modal (pop-up penuh skrin) selepas permohonan berjaya dihantar
 */
function showSuccessPopup(nomborPermohonan) {
  let modal = document.querySelector('#success-modal');

  if (!modal) {
    modal = document.createElement('div');
    modal.id = 'success-modal';
    modal.style.cssText = `
      display: none;
      position: fixed;
      top: 0; left: 0; right: 0; bottom: 0;
      background: rgba(0, 0, 0, 0.7);
      backdrop-filter: blur(4px);
      z-index: 10000;
      justify-content: center;
      align-items: center;
    `;
    document.body.appendChild(modal);
  }

  const closeModal = () => {
    modal.style.display = 'none';
  };

  modal.innerHTML = `
    <div style="
      background: white;
      padding: 30px;
      border-radius: 12px;
      text-align: center;
      box-shadow: 0 10px 30px rgba(0,0,0,0.3);
      max-width: 450px;
      width: 90%;
    ">
      <div style="margin-bottom: 20px;">
        <i class="fas fa-check-circle" style="font-size: 50px; color: #198754;"></i>
      </div>
      <h2 class="h4 mb-3" style="color: #198754; font-weight: 600;">Permohonan Berjaya Dihantar</h2>
      <p class="text-muted mb-1" style="font-size: 15px;">Nombor permohonan anda:</p>
      <p class="mb-3" style="font-size: 22px; font-weight: 700; color: #00467f;">${esc(nomborPermohonan)}</p>
      <p class="text-muted mb-4" style="font-size: 15px;">Sila simpan nombor ini.</p>
      <button id="success-modal-close" class="btn btn-success px-4 py-2" style="border-radius: 8px;">
        Tutup
      </button>
    </div>
  `;

  modal.querySelector('#success-modal-close').onclick = closeModal;
  modal.onclick = (e) => {
    if (e.target === modal) closeModal();
  };
  modal.style.display = 'flex';
}

function showCatatanModal(title = 'Catatan', placeholder = 'Tulis catatan di sini...') {
  return new Promise((resolve) => {
    const old = document.querySelector('#catatan-modal');
    if (old) old.remove();

    const modal = document.createElement('div');
    modal.id = 'catatan-modal';
    modal.style.cssText = `
      display: flex;
      position: fixed;
      top: 0; left: 0; right: 0; bottom: 0;
      background: rgba(0, 0, 0, 0.65);
      backdrop-filter: blur(4px);
      z-index: 9999;
      justify-content: center;
      align-items: center;
      padding: 16px;
    `;

    modal.innerHTML = `
      <div style="
        background: white;
        border-radius: 12px;
        width: 100%;
        max-width: 480px;
        box-shadow: 0 20px 60px rgba(0,0,0,0.3);
        overflow: hidden;
      ">
        <div style="
          background: linear-gradient(135deg, #00467f, #0d5ba5);
          color: white;
          padding: 18px 24px;
          font-size: 18px;
          font-weight: 600;
        ">
          <i class="fas fa-comment-dots me-2"></i>${esc(title)}
        </div>

        <div style="padding: 24px;">
          <textarea id="catatan-input" rows="5" placeholder="${esc(placeholder)}"
            style="
              width: 100%;
              border: 2px solid #dee2e6;
              border-radius: 8px;
              padding: 12px 14px;
              font-size: 15px;
              resize: vertical;
              outline: none;
              transition: border-color 0.2s;
              font-family: inherit;
            "
            onfocus="this.style.borderColor='#00467f'"
            onblur="this.style.borderColor='#dee2e6'"
          ></textarea>

          <div style="display: flex; gap: 10px; margin-top: 20px; justify-content: flex-end;">
            <button id="catatan-batal" class="btn btn-outline-secondary px-4">
              Batal
            </button>
            <button id="catatan-simpan" class="btn btn-primary px-4">
              <i class="fas fa-paper-plane me-1"></i> Hantar
            </button>
          </div>
        </div>
      </div>
    `;

    document.body.appendChild(modal);

    const input = modal.querySelector('#catatan-input');
    input.focus();

    const close = (value) => {
      modal.remove();
      resolve(value);
    };

    modal.querySelector('#catatan-batal').onclick = () => close(null);

    modal.querySelector('#catatan-simpan').onclick = () => {
      close(input.value.trim());
    };

    const onKey = (e) => {
      if (e.key === 'Escape') {
        document.removeEventListener('keydown', onKey);
        close(null);
      }
      if (e.key === 'Enter' && e.ctrlKey) {
        document.removeEventListener('keydown', onKey);
        close(input.value.trim());
      }
    };
    document.addEventListener('keydown', onKey);

    modal.onclick = (e) => {
      if (e.target === modal) close(null);
    };
  });
}

async function api(url, opt = {}) {
  const r = await fetch(url, {
    ...opt,
    headers: {
      'Content-Type': 'application/json',
      // elak halaman amaran ngrok free-tier menggantikan respons JSON sebenar
      'ngrok-skip-browser-warning': 'true',
      ...(opt.headers || {})
    }
  });

  if (!r.ok) {
    const err = await r.json().catch(() => ({ ralat: 'Ralat sistem' }));
    throw new Error(err.ralat || err.message || err.error || 'Tindakan gagal');
  }

  return r.json().catch(() => {
    throw new Error('Format maklum balas daripada server tidak sah');
  });
}

function scrollToNotice(targetSelector = '#mesej', offset = 90) {
  const target = document.querySelector(targetSelector);
  if (!target) {
    window.scrollTo({ top: 0, behavior: 'smooth' });
    return;
  }

  const top = window.scrollY + target.getBoundingClientRect().top - offset;
  window.scrollTo({ top: Math.max(0, top), behavior: 'smooth' });
}

/**
 * Handle form submission for new application
 * FIX: guard "sedangHantar" + disable butang semasa proses,
 * supaya request/e-mel/popover TIDAK berlaku dua kali (double submit).
 */
const form = document.querySelector('#borang');
if (form) {
  let sedangHantar = false; // penghalang hantar berganda

  form.onsubmit = async (e) => {
    e.preventDefault();

    // Jika sedang proses, abaikan sebarang klik/submit tambahan
    if (sedangHantar) return;
    sedangHantar = true;

    const submitBtn = form.querySelector('button[type="submit"]');
    const teksAsalBtn = submitBtn ? submitBtn.innerHTML : '';
    if (submitBtn) {
      submitBtn.disabled = true;
      submitBtn.innerHTML = '<i class="fas fa-spinner fa-spin me-2"></i> Menghantar...';
    }

    const data = Object.fromEntries(new FormData(form));

    if (data.phoneMobile && !data.phone) {
      data.phone = data.phoneMobile;
    }

    try {
      const r = await api('/api/public/permohonan', {
        method: 'POST',
        body: JSON.stringify(data)
      });

      showSuccessPopup(r.nomborPermohonan);

      form.reset();

      const appDate = document.getElementById('applicationDate');
      if (appDate) {
        appDate.value = new Date().toISOString().slice(0, 10);
      }
    } catch (x) {
      showErrorPopup('Ralat Permohonan', x.message);
    } finally {
      // Reset flag & butang tak kira berjaya/gagal, supaya
      // pengguna boleh cuba semula selepas ralat.
      sedangHantar = false;
      if (submitBtn) {
        submitBtn.disabled = false;
        submitBtn.innerHTML = teksAsalBtn;
      }
    }
  };
}

async function semak() {
  let no = document.querySelector('#no').value.trim();
  if (!no) return;

  try {
    let p = await api('/api/public/permohonan/' + encodeURIComponent(no));

    // === Status paparan: hanya 3 label dipaparkan kepada pemohon ===
    let statusPaparan;
    if (p.status === 'DITOLAK') {
      statusPaparan = { label: 'Tidak Diluluskan', warna: 'danger' };
    } else if (p.status === 'DILULUSKAN' || p.status === 'PAS_DIKELUARKAN') {
      statusPaparan = { label: 'Pas Dikeluarkan', warna: 'success' };
    } else {
      statusPaparan = { label: 'Dalam Proses', warna: 'warning text-dark' };
    }
    // ========================================

    // Butang cetak pas & QR hanya muncul bila status benar-benar PAS_DIKELUARKAN
    // (passToken dijana terus oleh sistem sebaik pengarah tekan Setuju)
    let pas = p.status === 'PAS_DIKELUARKAN'
      ? `
        <p class="mt-3">
          <a href="/api/pas/${p.passToken}/pdf-tanpa-qr" target="_blank" class="btn btn-primary me-2">
            Cetak Pas Kebenaran
          </a>
          <button class="btn btn-info" onclick="lihatQR('${p.passToken}')">
            Sahkan QR
          </button>
        </p>
      `
      : '';

    document.querySelector('#hasil').innerHTML = `
      <hr>
      <h2 class="h4 mt-4 mb-3">${esc(p.nomborPermohonan)}</h2>
      <p>
        <span class="badge bg-${statusPaparan.warna}">${statusPaparan.label}</span>
      </p>
      <p class="text-muted">
        <strong>Lokasi:</strong> ${esc(p.locationType)} — ${esc(p.locationName)}
      </p>
      ${pas}
    `;
  } catch (x) {
    showErrorPopup('Ralat Pencarian', 'Nombor permohonan tidak wujud, sila masukkan nombor permohonan dengan betul.');
  }
}

function lihatQR(token) {
  console.log('lihatQR dipanggil, token:', token);

  if (!token) {
    showErrorPopup('Ralat Token', 'Token pas tidak dijumpai');
    return;
  }

  let modal = document.querySelector('#qr-modal');
  
  if (!modal) {
    modal = document.createElement('div');
    modal.id = 'qr-modal';
    modal.style.cssText = `
      display: none;
      position: fixed;
      top: 0; left: 0; right: 0; bottom: 0;
      background: rgba(0, 0, 0, 0.7);
      backdrop-filter: blur(4px);
      z-index: 9999;
      justify-content: center;
      align-items: center;
    `;
    
    modal.innerHTML = `
      <div style="
        background: white;
        padding: 30px;
        border-radius: 12px;
        text-align: center;
        box-shadow: 0 5px 20px rgba(0,0,0,0.3);
        max-width: 500px;
        width: 90%;
      ">
        <h2 class="h4 mb-3">Sahkan Pas Kebenaran</h2>
        <img 
          id="qr-image" 
          alt="QR Code"
          style="
            width: 280px;
            height: 280px;
            margin: 20px 0;
            border: 2px solid #ddd;
            border-radius: 8px;
            object-fit: contain;
          "
        >
        <p class="text-muted mb-3">Imbas kod QR untuk mengesahkan kesahihan pas</p>
        <button class="btn btn-secondary px-4 py-2" id="tutup-qr-btn" style="border-radius: 8px;">
          Tutup
        </button>
      </div>
    `;
    
    document.body.appendChild(modal);

    modal.querySelector('#tutup-qr-btn').onclick = function() {
      modal.style.display = 'none';
    };

    modal.onclick = function(e) {
      if (e.target === modal) {
        modal.style.display = 'none';
      }
    };
  }

  const img = document.querySelector('#qr-image');
  // guna path relatif (bukan BASE_URL) supaya request sentiasa ke asal (origin)
  // yang sama dengan halaman semasa — elak sekatan CORS bila halaman dibuka
  // melalui domain/hos lain daripada domain ngrok
  const qrUrl = '/api/pas/' + encodeURIComponent(token) + '/qr?t=' + Date.now();

  console.log('QR Image URL:', qrUrl);

  const closeQrModal = () => {
    modal.style.display = 'none';
    if (img.dataset.blobUrl) {
      URL.revokeObjectURL(img.dataset.blobUrl);
      delete img.dataset.blobUrl;
    }
    img.removeAttribute('src');
  };

  // guna fetch + blob (bukan img.src terus) supaya request boleh bawa header
  // 'ngrok-skip-browser-warning', jika tidak halaman amaran ngrok free-tier
  // akan dihantar balik sebagai HTML dan bukannya imej PNG QR
  (async () => {
    try {
      const res = await fetch(qrUrl, {
        headers: { 'ngrok-skip-browser-warning': 'true' }
      });
      if (!res.ok) throw new Error('Respons tidak ok: ' + res.status);
      const blob = await res.blob();
      if (!blob.type.startsWith('image/')) throw new Error('Respons bukan imej');

      const blobUrl = URL.createObjectURL(blob);
      img.dataset.blobUrl = blobUrl;
      img.src = blobUrl;
    } catch (e) {
      console.error('Gagal load QR image', e);
      showErrorPopup('Ralat QR', 'Gagal memuatkan QR code. Sila cuba lagi.', {
        redirectTo: '/semak.html'
      });
    }
  })();

  modal.style.display = 'flex';
  modal.querySelector('#tutup-qr-btn').onclick = closeQrModal;
  modal.onclick = function(e) {
    if (e.target === modal) closeQrModal();
  };
}

const q = new URLSearchParams(location.search);
if (document.querySelector('#no') && q.get('no')) {
  document.querySelector('#no').value = q.get('no');
  semak();
}

let selectedPermohonanIds = new Set();

function updateBulkSelectionUI() {
  const bar = document.querySelector('#bulk-actions-bar');
  const countEl = document.querySelector('#selected-count');
  const selectAllEl = document.querySelector('#select-all-checkbox');
  const approveBtn = document.querySelector('#bulk-approve-btn');
  const rejectBtn = document.querySelector('#bulk-reject-btn');

  if (!bar || !countEl) return;

  const count = selectedPermohonanIds.size;
  bar.classList.toggle('d-none', count === 0);
  countEl.textContent = count;

  if (approveBtn) approveBtn.disabled = count === 0;
  if (rejectBtn) rejectBtn.disabled = count === 0;

  if (selectAllEl) {
    const totalRows = document.querySelectorAll('.row-checkbox').length;
    selectAllEl.checked = totalRows > 0 && count === totalRows;
    selectAllEl.indeterminate = totalRows > 0 && count > 0 && count < totalRows;
  }
}

function bindBulkActionButtons() {
  const approveBtn = document.querySelector('#bulk-approve-btn');
  const rejectBtn = document.querySelector('#bulk-reject-btn');

  if (approveBtn && !approveBtn.dataset.bound) {
    approveBtn.dataset.bound = 'true';
    approveBtn.addEventListener('click', () => bulkPutus(true));
  }

  if (rejectBtn && !rejectBtn.dataset.bound) {
    rejectBtn.dataset.bound = 'true';
    rejectBtn.addEventListener('click', () => bulkPutus(false));
  }
}

function bindBulkSelectionEvents() {
  const tableBody = document.querySelector('#senarai-pengarah');
  if (!tableBody) return;

  tableBody.onclick = null;
  tableBody.addEventListener('change', (event) => {
    if (event.target.classList.contains('row-checkbox')) {
      const id = Number(event.target.dataset.id);
      if (Number.isNaN(id)) return;

      if (event.target.checked) {
        selectedPermohonanIds.add(id);
      } else {
        selectedPermohonanIds.delete(id);
      }

      updateBulkSelectionUI();
    }
  });

  const selectAllEl = document.querySelector('#select-all-checkbox');
  if (selectAllEl && !selectAllEl.dataset.bound) {
    selectAllEl.dataset.bound = 'true';
    selectAllEl.addEventListener('change', (event) => {
      const checked = event.target.checked;
      document.querySelectorAll('.row-checkbox').forEach((checkbox) => {
        checkbox.checked = checked;
        const id = Number(checkbox.dataset.id);
        if (Number.isNaN(id)) return;
        if (checked) {
          selectedPermohonanIds.add(id);
        } else {
          selectedPermohonanIds.delete(id);
        }
      });
      updateBulkSelectionUI();
    });
  }
}

function setBulkPutusLoading(isLoading, lulus) {
  const approveBtn = document.querySelector('#bulk-approve-btn');
  const rejectBtn = document.querySelector('#bulk-reject-btn');
  const activeBtn = lulus ? approveBtn : rejectBtn;
  if (!approveBtn || !rejectBtn) return;

  if (isLoading) {
    approveBtn.disabled = true;
    rejectBtn.disabled = true;
    if (activeBtn) {
      activeBtn.dataset.originalHtml = activeBtn.innerHTML;
      activeBtn.innerHTML = '<span class="spinner-border spinner-border-sm me-1" role="status"></span>Memproses...';
    }
  } else if (activeBtn?.dataset.originalHtml) {
    activeBtn.innerHTML = activeBtn.dataset.originalHtml;
    delete activeBtn.dataset.originalHtml;
  }
}

async function bulkPutus(lulus) {
  const ids = [...selectedPermohonanIds];
  if (ids.length === 0) return;

  const sahkan = await showConfirmModal({
    title: lulus ? 'Sahkan Kelulusan Terpilih' : 'Sahkan Penolakan Terpilih',
    message: `Anda akan memberi keputusan kepada ${ids.length} permohonan terpilih.`,
    confirmText: lulus ? 'Ya, Setuju' : 'Ya, Tolak',
    cancelText: 'Batal',
    type: lulus ? 'success' : 'danger',
    icon: lulus ? 'fa-check-circle' : 'fa-times-circle'
  });

  if (!sahkan) return;

  // NOTA: Ruang catatan sudah dibuang mengikut permintaan.
  // Terus hantar keputusan tanpa minta catatan lagi.
  setBulkPutusLoading(true, lulus);
  try {
    for (const id of ids) {
      await api(`/api/pengarah/permohonan/${id}/keputusan`, {
        method: 'POST',
        body: JSON.stringify({ lulus, catatan: '' })
      });
    }

    selectedPermohonanIds.clear();
    director();

    // Popover keputusan selepas berjaya proses (hanya SEKALI, selepas gelung selesai)
    showConfirmModal({
      title: lulus ? 'Permohonan Diluluskan' : 'Permohonan Ditolak',
      message: `Keputusan telah berjaya direkodkan untuk ${ids.length} permohonan.`,
      confirmText: 'OK',
      type: lulus ? 'success' : 'danger',
      icon: lulus ? 'fa-check-circle' : 'fa-times-circle',
      showCancel: false
    });
  } catch (e) {
    showErrorPopup('Ralat Keputusan', e.message);
  } finally {
    setBulkPutusLoading(false, lulus);
  }
}

let pengarahAllPermohonan = [];
let pengarahPageSize = 20;
let pengarahCurrentPage = 1;

function renderPengarahPaginationControls(totalItems) {
  const paginationNav = document.querySelector('#pagination-nav');
  const paginationSummary = document.querySelector('#pagination-summary');
  const pageSizeSelect = document.querySelector('#page-size-select');
  const totalPages = Math.max(1, Math.ceil(totalItems / pengarahPageSize));

  if (pageSizeSelect) {
    pageSizeSelect.value = String(pengarahPageSize);
  }

  if (pengarahCurrentPage > totalPages) {
    pengarahCurrentPage = totalPages;
  }

  if (paginationSummary) {
    if (!totalItems) {
      paginationSummary.textContent = 'Tiada rekod dipaparkan.';
    } else {
      const start = (pengarahCurrentPage - 1) * pengarahPageSize + 1;
      const end = Math.min(pengarahCurrentPage * pengarahPageSize, totalItems);
      paginationSummary.textContent = `Menunjukkan ${start}-${end} daripada ${totalItems} permohonan.`;
    }
  }

  if (!paginationNav) return;

  if (!totalItems) {
    paginationNav.innerHTML = '';
    return;
  }

  const buttons = [];
  buttons.push(`<li class="page-item ${pengarahCurrentPage === 1 ? 'disabled' : ''}">
    <button type="button" class="page-link" data-page="prev" ${pengarahCurrentPage === 1 ? 'disabled' : ''}>
      <i class="fas fa-chevron-left"></i>
    </button>
  </li>`);

  const maxVisiblePages = 5;
  let startPage = Math.max(1, pengarahCurrentPage - 2);
  let endPage = Math.min(totalPages, startPage + maxVisiblePages - 1);
  if (endPage - startPage + 1 < maxVisiblePages) {
    startPage = Math.max(1, endPage - maxVisiblePages + 1);
  }

  for (let page = startPage; page <= endPage; page += 1) {
    buttons.push(`<li class="page-item ${page === pengarahCurrentPage ? 'active' : ''}">
      <button type="button" class="page-link" data-page="${page}">${page}</button>
    </li>`);
  }

  buttons.push(`<li class="page-item ${pengarahCurrentPage === totalPages ? 'disabled' : ''}">
    <button type="button" class="page-link" data-page="next" ${pengarahCurrentPage === totalPages ? 'disabled' : ''}>
      <i class="fas fa-chevron-right"></i>
    </button>
  </li>`);

  paginationNav.innerHTML = buttons.join('');
}

function bindPengarahPaginationEvents() {
  const pageSizeSelect = document.querySelector('#page-size-select');
  const paginationNav = document.querySelector('#pagination-nav');

  if (pageSizeSelect && !pageSizeSelect.dataset.bound) {
    pageSizeSelect.dataset.bound = 'true';
    pageSizeSelect.addEventListener('change', (event) => {
      pengarahPageSize = Number(event.target.value) || 20;
      pengarahCurrentPage = 1;
      renderPengarahTable();
    });
  }

  if (paginationNav && !paginationNav.dataset.bound) {
    paginationNav.dataset.bound = 'true';
    paginationNav.addEventListener('click', (event) => {
      const button = event.target.closest('button[data-page]');
      if (!button) return;

      const targetPage = button.getAttribute('data-page');
      if (targetPage === 'prev') {
        if (pengarahCurrentPage > 1) pengarahCurrentPage -= 1;
      } else if (targetPage === 'next') {
        pengarahCurrentPage += 1;
      } else {
        pengarahCurrentPage = Number(targetPage) || 1;
      }
      renderPengarahTable();
    });
  }
}

function renderPengarahTable() {
  const totalPages = Math.max(1, Math.ceil(pengarahAllPermohonan.length / pengarahPageSize));
  if (pengarahCurrentPage > totalPages) {
    pengarahCurrentPage = totalPages;
  }

  const startIndex = (pengarahCurrentPage - 1) * pengarahPageSize;
  const pageItems = pengarahAllPermohonan.slice(startIndex, startIndex + pengarahPageSize);

  document.querySelector('#senarai-pengarah').innerHTML = pageItems.map(p => `
    <tr>
      <td>
        <input class="form-check-input row-checkbox" type="checkbox" data-id="${p.id}">
      </td>
      <td>${esc(p.nomborPermohonan)}</td>
      <td class="text-start">${esc(p.applicantName)}</td>
      <td>${esc(p.reviewedBy?.name || 'Tidak direkodkan')}</td>
      <td>
        <small>
          ${esc(p.locationName)}<br>
          ${esc(p.visitDate)}
        </small>
      </td>
      <td class="text-start">
        <small>${esc(p.purpose || '-')}</small>
      </td>
      <td>
        <a href="/api/public/permohonan/${p.nomborPermohonan}/pdf" target="_blank" class="btn btn-sm btn-info">
          Buka PDF
        </a>
      </td>
    </tr>
  `).join('') || '<tr><td colspan="8" class="text-center text-muted">Tiada permohonan menunggu.</td></tr>';

  renderPengarahPaginationControls(pengarahAllPermohonan.length);
  bindBulkSelectionEvents();
  bindBulkActionButtons();
  bindPengarahPaginationEvents();
  updateBulkSelectionUI();
}

async function director() {
  pengarahAllPermohonan = await api('/api/pengarah/permohonan');
  selectedPermohonanIds.clear();
  renderPengarahTable();
}

async function putus(id, lulus) {
  const sahkan = await showConfirmModal({
    title: lulus ? 'Sahkan Kelulusan' : 'Sahkan Penolakan',
    message: lulus
      ? 'Adakah anda pasti untuk meluluskan permohonan ini?'
      : 'Adakah anda pasti untuk menolak permohonan ini?',
    confirmText: lulus ? 'Ya, Setuju' : 'Ya, Tolak',
    cancelText: 'Batal',
    type: lulus ? 'success' : 'danger',
    icon: lulus ? 'fa-check-circle' : 'fa-times-circle'
  });

  if (!sahkan) return;

  // NOTA: Ruang catatan sudah dibuang mengikut permintaan.
  // Selepas tekan "Ya, Setuju" / "Ya, Tolak", terus hantar keputusan.
  try {
    await api(`/api/pengarah/permohonan/${id}/keputusan`, {
      method: 'POST',
      body: JSON.stringify({ lulus, catatan: '' })
    });
    director();

    // Popover keputusan selepas berjaya proses
    showConfirmModal({
      title: lulus ? 'Permohonan Diluluskan' : 'Permohonan Ditolak',
      message: lulus
        ? 'Permohonan ini telah berjaya diluluskan.'
        : 'Permohonan ini telah berjaya ditolak.',
      confirmText: 'OK',
      type: lulus ? 'success' : 'danger',
      icon: lulus ? 'fa-check-circle' : 'fa-times-circle',
      showCancel: false
    });
  } catch (e) {
    showErrorPopup('Ralat Keputusan', e.message);
  }
}

if (document.querySelector('#senarai-pengarah')) {
  director();
}

if (document.querySelector('#sah')) {
  let token = q.get('token');
  api('/api/pas/' + encodeURIComponent(token))
    .then(p => {
      document.querySelector('#sah').innerHTML = `
        <div class="alert alert-success mb-3">
          <h2 class="h5 mb-0">PAS SAH</h2>
        </div>
        <dl class="row">
          <dt class="col-sm-4">No. Permohonan:</dt>
          <dd class="col-sm-8">${esc(p.nomborPermohonan)}</dd>
          <dt class="col-sm-4">Pemegang:</dt>
          <dd class="col-sm-8">${esc(p.applicantName)}</dd>
          <dt class="col-sm-4">Lokasi:</dt>
          <dd class="col-sm-8">${esc(p.locationName)}</dd>
          <dt class="col-sm-4">Tarikh lawatan:</dt>
          <dd class="col-sm-8">${esc(p.visitDate)}</dd>
        </dl>
      `;
    })
    .catch(() => {
      document.querySelector('#sah').innerHTML = `
        <div class="alert alert-danger mb-0">
          <h2 class="h5 mb-0">✗ PAS TIDAK SAH</h2>
          <small>Pas tidak sah atau telah tamat tempohnya.</small>
        </div>
      `;
    });
}