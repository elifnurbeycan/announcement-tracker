/**
 * Settings Page Component
 */
window.SettingsPage = function SettingsPage({
    scrapePeriod = 30,
    onSavePeriod
}) {
    const isPreset = (val) => [30, 60, 120].includes(val);

    const [selectedOption, setSelectedOption] = React.useState(
        isPreset(scrapePeriod) ? String(scrapePeriod) : 'custom'
    );
    const [customMinutes, setCustomMinutes] = React.useState(scrapePeriod || 30);
    const [loading, setLoading] = React.useState(false);

    React.useEffect(() => {
        if (isPreset(scrapePeriod)) {
            setSelectedOption(String(scrapePeriod));
        } else {
            setSelectedOption('custom');
        }
        setCustomMinutes(scrapePeriod || 30);
    }, [scrapePeriod]);

    React.useEffect(() => {
        if (window.lucide) window.lucide.createIcons();
    }, [selectedOption]);

    const handleSelectChange = async (e) => {
        const val = e.target.value;
        setSelectedOption(val);
        if (val !== 'custom') {
            const minutes = parseInt(val, 10);
            setCustomMinutes(minutes);
            setLoading(true);
            try {
                await onSavePeriod(minutes);
            } finally {
                setLoading(false);
            }
        }
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setLoading(true);
        const minutes = selectedOption === 'custom' ? parseInt(customMinutes, 10) : parseInt(selectedOption, 10);
        try {
            await onSavePeriod(minutes);
        } finally {
            setLoading(false);
        }
    };

    return (
        <div style={{ maxWidth: '680px' }}>
            <div className="card">
                <div className="card-header-flex" style={{ marginBottom: '16px' }}>
                    <h2>
                        <i data-lucide="clock" style={{ color: '#60a5fa', width: '20px', height: '20px' }}></i>
                        Otomatik Tarama Zamanlaması
                    </h2>
                </div>

                <p style={{ fontSize: '13.5px', color: 'var(--text-sub)', marginBottom: '24px', lineHeight: '1.6' }}>
                    Sistemin resmi duyuru kaynaklarını (GİB vb.) otomatik olarak kaç dakikada bir tarayacağını ayarlayın. Yeni bir duyuru tespit edildiğinde ilgili abonelere e-posta bildirimi gönderilir.
                </p>

                <form onSubmit={handleSubmit}>
                    <div className="form-group">
                        <label className="form-label">Tarama Periyodu Seçin</label>
                        <div style={{ display: 'flex', flexDirection: 'column', gap: '14px', marginTop: '8px' }}>
                            <select 
                                className="input-field" 
                                style={{ width: '100%', maxWidth: '320px', fontSize: '14px', fontWeight: '600' }}
                                value={selectedOption}
                                onChange={handleSelectChange}
                            >
                                <option value="30">30 Dakika</option>
                                <option value="60">1 Saat (60 Dakika)</option>
                                <option value="120">2 Saat (120 Dakika)</option>
                                <option value="custom">Özel Süre Gir...</option>
                            </select>

                            {selectedOption === 'custom' && (
                                <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                                    <input 
                                        type="number" 
                                        min="5" 
                                        max="1440" 
                                        className="input-field" 
                                        style={{ width: '140px', fontSize: '15px', fontWeight: '700', textAlign: 'center' }}
                                        placeholder="Dakika"
                                        value={customMinutes}
                                        onChange={(e) => setCustomMinutes(e.target.value)}
                                        required 
                                    />
                                    <span style={{ fontSize: '14px', color: 'var(--text-sub)', fontWeight: '600' }}>Dakika</span>
                                </div>
                            )}
                        </div>
                        <p style={{ fontSize: '12px', color: 'var(--text-sub)', marginTop: '10px' }}>
                            * Önerilen varsayılan değer 30 dakikadır. Minimum 5 dakika ayarlanabilir.
                        </p>
                    </div>

                    <div style={{ borderTop: '1px solid var(--border-color)', paddingTop: '20px', marginTop: '24px', display: 'flex', justifyContent: 'flex-end' }}>
                        <button type="submit" className="btn-action-primary" disabled={loading}>
                            <i data-lucide="save" style={{ width: '16px', height: '16px' }}></i>
                            {loading ? 'Kaydediliyor...' : 'Ayarları Kaydet'}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
};
