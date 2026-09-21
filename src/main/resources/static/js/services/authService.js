/**
 * Authentication Service
 */
window.AuthService = {
    loginAdmin: async function(username, password) {
        const data = await window.ApiClient.post('/api/v1/auth/login', { username, password });
        if (data.success && data.data) {
            localStorage.setItem('adminToken', data.data.token);
            localStorage.setItem('adminUser', JSON.stringify(data.data));
        }
        return data;
    },

    loginUser: async function(email, password) {
        const data = await window.ApiClient.post('/api/v1/user/login', { email, password });
        if (data.success && data.data) {
            localStorage.setItem('userToken', data.data.token);
            localStorage.setItem('userData', JSON.stringify(data.data));
        }
        return data;
    },

    getAdminUser: function() {
        try {
            return JSON.parse(localStorage.getItem('adminUser') || '{}');
        } catch(e) {
            return {};
        }
    },

    getUserData: function() {
        try {
            return JSON.parse(localStorage.getItem('userData') || '{}');
        } catch(e) {
            return {};
        }
    },

    logoutAdmin: function() {
        localStorage.removeItem('adminToken');
        localStorage.removeItem('adminUser');
        window.location.href = '/admin-login.html';
    },

    logoutUser: function() {
        localStorage.removeItem('userToken');
        localStorage.removeItem('userData');
        window.location.href = '/user-login.html';
    }
};
