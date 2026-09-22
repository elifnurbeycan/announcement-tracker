/**
 * Announcement Service
 */
window.AnnouncementService = {
    getAnnouncements: async function(page = 0, size = 10, site = '', search = '', hasAttachment = false) {
        let url = `/api/v1/announcements?page=${page}&size=${size}`;
        if (site) url += `&siteType=${encodeURIComponent(site)}`;
        if (search) url += `&search=${encodeURIComponent(search)}`;
        if (hasAttachment) url += '&hasAttachment=true';
        const res = await window.ApiClient.get(url);
        return res.data || res;
    },

    getUserAnnouncements: async function(page = 0, size = 10, site = '', search = '', hasAttachment = false) {
        let url = `/api/v1/user/me/announcements?page=${page}&size=${size}`;
        if (site) url += `&siteType=${encodeURIComponent(site)}`;
        if (search) url += `&search=${encodeURIComponent(search)}`;
        if (hasAttachment) url += '&hasAttachment=true';
        const res = await window.ApiClient.get(url);
        return res.data || res;
    },

    getAnnouncementCounts: async function() {
        const res = await window.ApiClient.get('/api/v1/announcements/counts');
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
