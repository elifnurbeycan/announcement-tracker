/**
 * Centralized API Client for Announcement Tracker
 */
window.ApiClient = {
    getCookie: function(name) {
        const prefix = `${encodeURIComponent(name)}=`;
        const cookie = document.cookie.split('; ').find(item => item.startsWith(prefix));
        return cookie ? decodeURIComponent(cookie.substring(prefix.length)) : '';
    },

    getCsrfHeader: function() {
        const token = this.getCookie('XSRF-TOKEN');
        return token ? { 'X-XSRF-TOKEN': token } : {};
    },

    isProtectedRequest: function(url, method) {
        if (url.startsWith('/api/v1/user/')) return true;
        if (url.startsWith('/api/v1/auth/')) return true;
        if (url.startsWith('/api/v1/admin/')) return true;
        if (url.startsWith('/api/v1/settings/')) return true;
        if (url.startsWith('/api/v1/departments')) return true;
        if (url.startsWith('/api/v1/subscribers')) {
            return !url.includes('/unsubscribe');
        }
        return method !== 'GET' && url.startsWith('/api/v1/announcements');
    },

    redirectToLogin: function(url) {
        const target = '/login';

        sessionStorage.removeItem('authRole');
        localStorage.removeItem('userToken');
        localStorage.removeItem('userData');
        localStorage.removeItem('adminToken');
        localStorage.removeItem('adminUser');
        sessionStorage.setItem('sessionExpiredMessage', 'Oturumunuz sona erdi. Lütfen tekrar giriş yapın.');

        if (window.location.pathname !== target) {
            window.location.replace(target);
        }
    },

    request: async function(url, options = {}) {
        const method = (options.method || 'GET').toUpperCase();
        const csrfHeader = ['POST', 'PUT', 'PATCH', 'DELETE'].includes(method)
            ? this.getCsrfHeader()
            : {};
        const headers = {
            'Content-Type': 'application/json',
            ...csrfHeader,
            ...(options.headers || {})
        };

        const config = {
            ...options,
            headers,
            credentials: 'same-origin'
        };

        try {
            const response = await fetch(url, config);
            let data = null;
            const contentType = response.headers.get('content-type') || '';

            if (contentType.includes('application/json')) {
                try {
                    data = await response.json();
                } catch(parseErr) {
                    console.error('JSON parse error:', parseErr);
                }
            }

            const protectedRequest = this.isProtectedRequest(url, method);

            if ((response.status === 401 || response.status === 403) && protectedRequest) {
                this.redirectToLogin(url);
                throw new Error('Oturumunuz sona erdi. Giriş ekranına yönlendiriliyorsunuz.');
            }

            if (!response.ok) {
                const errorMessage = (data && data.message) ? data.message : 'Giriş işlemi tamamlanamadı.';
                throw new Error(errorMessage);
            }

            return data || {};
        } catch (error) {
            console.error(`API Error [${url}]:`, error);
            if (error.name === 'SyntaxError') {
                throw new Error('Giriş işlemi tamamlanamadı.');
            }
            throw error;
        }
    },

    get: function(url) {
        return this.request(url, { method: 'GET' });
    },

    post: function(url, body) {
        return this.request(url, { method: 'POST', body: body ? JSON.stringify(body) : undefined });
    },

    put: function(url, body) {
        return this.request(url, { method: 'PUT', body: body ? JSON.stringify(body) : undefined });
    },

    patch: function(url, body) {
        return this.request(url, { method: 'PATCH', body: body ? JSON.stringify(body) : undefined });
    },

    delete: function(url) {
        return this.request(url, { method: 'DELETE' });
    }
};
