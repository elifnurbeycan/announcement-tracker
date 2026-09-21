/**
 * Settings Service
 */
window.SettingsService = {
    getSettings: async function() {
        const res = await window.ApiClient.get('/api/v1/settings/scrape');
        return res.data || res;
    },

    updateScrapePeriod: async function(periodMinutes) {
        const res = await window.ApiClient.post(`/api/v1/settings/scrape?intervalMinutes=${periodMinutes}`);
        return res.data || res;
    }
};
