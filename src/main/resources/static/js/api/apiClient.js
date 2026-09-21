/**
 * Centralized API Client for Announcement Tracker
 */
window.ApiClient = {
    getAuthToken: function() {
        return localStorage.getItem('adminToken') || localStorage.getItem('userToken') || '';
    },

    getAuthHeader: function() {
        const token = this.getAuthToken();
        return token ? { 'Authorization': `Bearer ${token}` } : {};
    },

    request: async function(url, options = {}) {
        const headers = {
            'Content-Type': 'application/json',
            ...this.getAuthHeader(),
            ...(options.headers || {})
        };

        const config = {
            ...options,
            headers
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
