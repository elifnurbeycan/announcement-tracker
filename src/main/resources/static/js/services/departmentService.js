/**
 * Department Service
 */
window.DepartmentService = {
    getDepartments: async function() {
        const res = await window.ApiClient.get('/api/v1/departments');
        return res.data || res;
    },

    createDepartment: async function(payload) {
        const res = await window.ApiClient.post('/api/v1/departments', payload);
        return res.data || res;
    },

    updateDepartment: async function(id, payload) {
        const res = await window.ApiClient.put(`/api/v1/departments/${id}`, payload);
        return res.data || res;
    },

    deleteDepartment: async function(id) {
        return await window.ApiClient.delete(`/api/v1/departments/${id}`);
    },

    deleteDepartmentsBatch: async function(ids) {
        return await window.ApiClient.post('/api/v1/departments/batch-delete', ids);
    }
};
