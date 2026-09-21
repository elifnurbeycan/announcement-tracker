/**
 * Overview Page Component
 */
window.OverviewPage = function OverviewPage({
    announcements = [],
    totalAnnouncements = 0,
    subscribers = [],
    departments = [],
    availableSites = [],
    onNavigate
}) {
    React.useEffect(() => {
        if (window.lucide) window.lucide.createIcons();
    }, [announcements, subscribers, departments]);

    const recentAnnouncements = announcements.slice(0, 5);
    const displayTotalAnnouncements = totalAnnouncements > 0 ? totalAnnouncements : announcements.length;

    return (
        <div>
            {/* Stats Grid */}
            <div className="stats-grid">
                <div className="stat-card">
                    <div className="stat-info">
                        <div className="title">Toplam Duyuru</div>
                        <div className="value">{displayTotalAnnouncements}</div>
                    </div>
                    <div className="stat-icon" style={{ background: 'rgba(59, 130, 246, 0.15)', color: '#60a5fa' }}>
                        <i data-lucide="megaphone"></i>
                    </div>
                </div>

                <div className="stat-card">
                    <div className="stat-info">
                        <div className="title">Kayıtlı Aboneler</div>
                        <div className="value">{subscribers.length}</div>
                    </div>
                    <div className="stat-icon" style={{ background: 'rgba(16, 185, 129, 0.15)', color: '#34d399' }}>
                        <i data-lucide="users"></i>
                    </div>
                </div>

                <div className="stat-card">
                    <div className="stat-info">
                        <div className="title">Departman Sayısı</div>
                        <div className="value">{departments.length}</div>
                    </div>
                    <div className="stat-icon" style={{ background: 'rgba(139, 92, 246, 0.15)', color: '#c084fc' }}>
                        <i data-lucide="building-2"></i>
                    </div>
                </div>

                <div className="stat-card">
                    <div className="stat-info">
                        <div className="title">Takip Edilen Kaynaklar</div>
                        <div className="value">{availableSites.length}</div>
                    </div>
                    <div className="stat-icon" style={{ background: 'rgba(6, 182, 212, 0.15)', color: '#22d3ee' }}>
                        <i data-lucide="globe"></i>
                    </div>
                </div>
            </div>

            {/* Recent Announcements */}
            <div className="card">
                <div className="card-header-flex">
                    <h2>
                        <i data-lucide="sparkles" style={{ color: '#60a5fa', width: '20px', height: '20px' }}></i>
                        Son Yakalanan Duyurular
                    </h2>
                    <button className="btn-action-secondary" onClick={() => onNavigate('announcements')}>
                        Tümünü Gör &rarr;
                    </button>
                </div>

                <div className="announcements-grid">
                    {recentAnnouncements.length === 0 ? (
                        <div style={{ textAlign: 'center', padding: '30px', color: 'var(--text-sub)' }}>
                            Henüz duyuru bulunmuyor.
                        </div>
                    ) : (
                        recentAnnouncements.map((item, index) => (
                            <div key={item.id || index} className="announcement-card">
                                <div className="card-top">
                                    <span className="site-badge">{item.sourceSite}</span>
                                    <span className="card-date">
                                        <i data-lucide="calendar" style={{ width: '14px', height: '14px' }}></i>
                                        {item.announcementDate ? new Date(item.announcementDate).toLocaleDateString('tr-TR') : 'Tarih Belirtilmedi'}
                                    </span>
                                </div>
                                <h3 className="card-title-text">{item.title}</h3>
                                {item.content && <p className="card-body-text">{item.content.substring(0, 160)}...</p>}
                                <div className="card-footer">
                                    {item.sourceUrl && (
                                        <a href={item.sourceUrl} target="_blank" rel="noopener noreferrer" className="btn-link btn-source">
                                            <i data-lucide="external-link" style={{ width: '14px', height: '14px' }}></i>
                                            Kaynağa Git
                                        </a>
                                    )}
                                    {item.attachmentUrl && (
                                        <a href={item.attachmentUrl} target="_blank" rel="noopener noreferrer" className="btn-link btn-pdf">
                                            <i data-lucide="file-text" style={{ width: '14px', height: '14px' }}></i>
                                            Ek Dosya
                                        </a>
                                    )}
                                </div>
                            </div>
                        ))
                    )}
                </div>
            </div>
        </div>
    );
};
