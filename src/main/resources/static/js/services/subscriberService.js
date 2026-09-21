/**
 * Subscriber Service
 */
window.SubscriberService = {
    getSubscribers: async function() {
        const res = await window.ApiClient.get('/api/v1/subscribers');
        return res.data || res;
    },

    createSubscriber: async function(payload) {
        const res = await window.ApiClient.post('/api/v1/subscribers', payload);
        return res.data || res;
    },

    updateSubscriber: async function(id, payload) {
        const res = await window.ApiClient.put(`/api/v1/subscribers/${id}`, payload);
        return res.data || res;
    },

    deleteSubscriber: async function(id) {
        return await window.ApiClient.delete(`/api/v1/subscribers/${id}`);
    },

    deleteSubscribersBatch: async function(ids) {
        return await window.ApiClient.post('/api/v1/subscribers/batch-delete', ids);
    },

    toggleStatus: async function(id, currentActive) {
        const newStatus = !currentActive;
        return await window.ApiClient.patch(`/api/v1/subscribers/${id}/status?active=${newStatus}`);
    },

    importExcel: async function(formData) {
        const token = window.ApiClient.getAuthToken();
        const headers = token ? { 'Authorization': `Bearer ${token}` } : {};

        const response = await fetch('/api/v1/subscribers/upload-excel', {
            method: 'POST',
            headers,
            body: formData
        });
        const data = await response.json();
        if (!response.ok) {
            throw new Error(data.message || 'İçe aktarma hatası');
        }
        return data;
    },

    downloadTemplate: function() {
        window.location.href = '/api/v1/subscribers/template-excel';
    }
};
