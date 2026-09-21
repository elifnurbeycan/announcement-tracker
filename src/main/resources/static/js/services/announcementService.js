/**
 * Announcement Service
 */
window.AnnouncementService = {
    getAnnouncements: async function(page = 0, size = 10, site = '', search = '') {
        let url = `/api/v1/announcements?page=${page}&size=${size}`;
        if (site) url += `&site=${encodeURIComponent(site)}`;
        if (search) url += `&search=${encodeURIComponent(search)}`;
        const res = await window.ApiClient.get(url);
        return res.data || res;
    },

    getUserAnnouncements: async function(page = 0, size = 10, search = '') {
        let url = `/api/v1/user/me/announcements?page=${page}&size=${size}`;
        if (search) url += `&search=${encodeURIComponent(search)}`;
        const res = await window.ApiClient.get(url);
        return res.data || res;
    },

    triggerScrape: async function() {
        const res = await window.ApiClient.post('/api/v1/announcements/trigger');
        return res.data || res;
    },

    getSites: async function() {
        const res = await window.ApiClient.get('/api/v1/sites');
        return res.data || res;
    }
};
