/**
 * Sources Page Component
 */
window.SourcesPage = function SourcesPage({
    availableSites = [],
    announcementCounts = {}
}) {
    React.useEffect(() => {
        if (window.lucide) window.lucide.createIcons();
    }, [availableSites, announcementCounts]);

    const siteInfoList = [
        {
            code: 'EBELGE_GIB',
            name: 'Gelir İdaresi Başkanlığı (e-Belge)',
            url: 'https://ebelge.gib.gov.tr/duyurular.html',
            description: 'GİB e-Fatura, e-Arşiv, e-İrsaliye ve e-Defter resmi portal duyuruları'
        },
        {
            code: 'TAKLIT_TAGSIS',
            name: 'Tarım ve Orman Bkn. Taklit/Tağşiş Duyuruları',
            url: 'https://guvenilirgida.tarimorman.gov.tr/GuvenilirGida/gkd/TaklitVeyaTagsis',
            description: 'Kamuoyuna açıklanan taklit veya tağşiş yapılan gıda ürünleri ve firmalar listesi'
        }

    ];

    return (
        <div>
            <div className="card-header-flex" style={{ marginBottom: '24px' }}>
                <div>
                    <h2 style={{ fontSize: '20px', fontWeight: '800', color: 'white' }}>Takip Edilen Duyuru Kaynakları</h2>
                    <p style={{ fontSize: '13px', color: 'var(--text-sub)', marginTop: '4px' }}>
                        Sistemin periyodik olarak taradığı resmi web siteleri ve servis durumları.
                    </p>
                </div>
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(360px, 1fr))', gap: '20px' }}>
                {siteInfoList.map(site => {
                    const count = announcementCounts[site.code] || 0;

                    return (
                        <div key={site.code} className="card">
                            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '14px' }}>
                                <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                                    <div style={{ width: '42px', height: '42px', borderRadius: '12px', background: 'rgba(6, 182, 212, 0.15)', color: '#22d3ee', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                                        <i data-lucide="globe" style={{ width: '22px', height: '22px' }}></i>
                                    </div>
                                    <div>
                                        <h3 style={{ fontSize: '16px', fontWeight: '700', color: 'white' }}>{site.name}</h3>
                                        <span className="site-badge" style={{ marginTop: '4px', display: 'inline-block' }}>{site.code}</span>
                                    </div>
                                </div>
                                <span style={{ background: 'rgba(16, 185, 129, 0.15)', color: '#34d399', border: '1px solid rgba(16, 185, 129, 0.3)', padding: '4px 10px', borderRadius: '8px', fontSize: '12px', fontWeight: '700' }}>
                                    Aktif
                                </span>
                            </div>

                            <p style={{ fontSize: '13px', color: 'var(--text-sub)', lineHeight: '1.5', marginBottom: '16px' }}>
                                {site.description}
                            </p>

                            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', borderTop: '1px solid var(--border-color)', paddingTop: '14px' }}>
                                <span style={{ fontSize: '13px', color: 'var(--text-sub)' }}>
                                    Toplam Duyuru: <strong style={{ color: 'white' }}>{count}</strong>
                                </span>
                                <a href={site.url} target="_blank" rel="noopener noreferrer" className="btn-link btn-source">
                                    <i data-lucide="external-link" style={{ width: '14px', height: '14px' }}></i>
                                    Siteye Git
                                </a>
                            </div>
                        </div>
                    );
                })}
            </div>
        </div>
    );
};
