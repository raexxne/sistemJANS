(() => {
  const nativeFetch = window.fetch.bind(window);
  let tokenPromise;

  const getToken = () => {
    if (!tokenPromise) {
      tokenPromise = nativeFetch('/api/csrf', {
        credentials: 'same-origin',
        headers: { Accept: 'application/json' },
        cache: 'no-store'
      }).then(async (response) => {
        if (!response.ok) {
          throw new Error(`Gagal mendapatkan token keselamatan (HTTP ${response.status}).`);
        }
        const payload = await response.json();
        if (!payload.token) {
          throw new Error('Server tidak memulangkan token keselamatan.');
        }
        return payload.token;
      }).catch((error) => {
        tokenPromise = undefined;
        throw error;
      });
    }
    return tokenPromise;
  };

  window.fetch = async (input, init = {}) => {
    const request = input instanceof Request ? input : undefined;
    const method = (init.method || request?.method || 'GET').toUpperCase();
    const url = new URL(typeof input === 'string' ? input : input.url, window.location.href);
    if (url.origin !== window.location.origin || ['GET', 'HEAD', 'OPTIONS', 'TRACE'].includes(method)) {
      return nativeFetch(input, init);
    }

    const headers = new Headers(request?.headers);
    new Headers(init.headers).forEach((value, name) => headers.set(name, value));
    headers.set('X-XSRF-TOKEN', await getToken());
    return nativeFetch(input, { ...init, headers, credentials: init.credentials || 'same-origin' });
  };

  document.addEventListener('submit', async (event) => {
    const form = event.target;
    if (!(form instanceof HTMLFormElement) || form.method.toUpperCase() !== 'POST'
        || form.dataset.csrfReady === 'true') {
      return;
    }

    event.preventDefault();
    try {
      const token = await getToken();
      let field = form.querySelector('input[name="_csrf"]');
      if (!field) {
        field = document.createElement('input');
        field.type = 'hidden';
        field.name = '_csrf';
        form.append(field);
      }
      field.value = token;
      form.dataset.csrfReady = 'true';
      form.requestSubmit(event.submitter);
    } catch (error) {
      console.error('Borang tidak dapat dihantar kerana token keselamatan gagal diperoleh.', error);
      const notice = document.createElement('p');
      notice.className = 'alert alert-danger';
      notice.setAttribute('role', 'alert');
      notice.textContent = 'Borang tidak dapat dihantar buat masa ini. Sila muat semula halaman dan cuba lagi.';
      form.insertAdjacentElement('beforebegin', notice);
    }
  }, true);
})();
