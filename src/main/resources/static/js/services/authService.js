/**
 * Authentication Service
 */
window.AuthService = {
    getUserProfile: async function() {
        const response = await window.ApiClient.get('/api/v1/user/me');
        return response.data || response;
    },

    getAdminProfile: async function() {
        const response = await window.ApiClient.get('/api/v1/auth/me');
        return response.data || response;
    },

    updateUserPreferences: async function(siteTypes) {
        const response = await window.ApiClient.put('/api/v1/user/me/preferences', siteTypes);
        const updatedUser = response.data || response;
        return updatedUser;
    },

    logoutAdmin: async function() {
        try {
            await window.ApiClient.post('/api/v1/auth/logout');
        } catch (error) {
            console.warn('Admin logout request failed:', error);
        }
        sessionStorage.removeItem('authRole');
        localStorage.removeItem('adminToken');
        localStorage.removeItem('adminUser');
        window.location.href = '/sso/logout';
    },

    logoutUser: async function() {
        try {
            await window.ApiClient.post('/api/v1/user/logout');
        } catch (error) {
            console.warn('User logout request failed:', error);
        }
        sessionStorage.removeItem('authRole');
        localStorage.removeItem('userToken');
        localStorage.removeItem('userData');
        window.location.href = '/sso/logout';
    }
};
