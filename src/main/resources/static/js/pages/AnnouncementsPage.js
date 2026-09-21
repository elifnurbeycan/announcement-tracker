/**
 * Announcements Page Component
 */
window.AnnouncementsPage = function AnnouncementsPage({
    announcements = [],
    availableSites = [],
    searchQuery,
    setSearchQuery,
    selectedSiteFilter,
    setSelectedSiteFilter,
    hasAttachmentFilter,
    setHasAttachmentFilter,
    page,
    setPage,
    totalPages,
    totalElements
}) {
    React.useEffect(() => {
        if (window.lucide) window.lucide.createIcons();
    }, [announcements, selectedSiteFilter, hasAttachmentFilter]);

    return (
        <div>
            {/* Toolbar Card */}
            <div className="toolbar-card">
                <div className="search-input-group">
                    <i data-lucide="search"></i>
                    <input 
                        type="text" 
                        className="input-field" 
                        placeholder="Duyuru ara (başlık, içerik)..." 
                        value={searchQuery}
                        onChange={(e) => setSearchQuery(e.target.value)}
                    />
                </div>

                <div className="filter-pills">
                    <button 
                        className={`filter-pill ${selectedSiteFilter === '' ? 'active' : ''}`}
                        onClick={() => setSelectedSiteFilter('')}
                    >
                        Tüm Kaynaklar
                    </button>
                    {availableSites.map(site => (
                        <button 
                            key={site} 
                            className={`filter-pill ${selectedSiteFilter === site ? 'active' : ''}`}
                            onClick={() => setSelectedSiteFilter(site)}
                        >
                            {site}
                        </button>
                    ))}
                    <button 
                        className={`filter-pill ${hasAttachmentFilter ? 'active' : ''}`}
                        onClick={() => setHasAttachmentFilter(!hasAttachmentFilter)}
                        style={{ borderStyle: 'dashed' }}
                    >
                        <i data-lucide="paperclip" style={{ width: '14px', height: '14px', display: 'inline-block', verticalAlign: 'middle', marginRight: '4px' }}></i>
                        Ek Dosyalı
                    </button>
                </div>
            </div>

            {/* List */}
            <div className="card">
                <div className="card-header-flex">
                    <h2>
                        <i data-lucide="list" style={{ color: '#60a5fa', width: '20px', height: '20px' }}></i>
                        Duyuru Listesi ({totalElements})
                    </h2>
                </div>

                <div className="announcements-grid">
                    {announcements.length === 0 ? (
                        <div style={{ textAlign: 'center', padding: '40px', color: 'var(--text-sub)' }}>
                            Filtrelere uygun duyuru bulunamadı.
                        </div>
                    ) : (
                        announcements.map((item, index) => (
                            <div key={item.id || index} className="announcement-card">
                                <div className="card-top">
                                    <span className="site-badge">{item.sourceSite}</span>
                                    <span className="card-date">
                                        <i data-lucide="calendar" style={{ width: '14px', height: '14px' }}></i>
                                        {item.announcementDate ? new Date(item.announcementDate).toLocaleDateString('tr-TR') : 'Tarih Belirtilmedi'}
                                    </span>
                                </div>
                                <h3 className="card-title-text">{item.title}</h3>
                                {item.content && <p className="card-body-text">{item.content}</p>}
                                <div className="card-footer">
                                    {item.sourceUrl && (
                                        <a href={item.sourceUrl} target="_blank" rel="noopener noreferrer" className="btn-link btn-source">
                                            <i data-lucide="external-link" style={{ width: '14px', height: '14px' }}></i>
                                            Resmi Kaynağa Git
                                        </a>
                                    )}
                                    {item.attachmentUrl && (
                                        <a href={item.attachmentUrl} target="_blank" rel="noopener noreferrer" className="btn-link btn-pdf">
                                            <i data-lucide="download" style={{ width: '14px', height: '14px' }}></i>
                                            Ek Dosya İndir
                                        </a>
                                    )}
                                </div>
                            </div>
                        ))
                    )}
                </div>

                {/* Pagination */}
                {totalPages > 1 && (
                    <div className="pagination">
                        <span style={{ fontSize: '13px', color: 'var(--text-sub)' }}>
                            Sayfa {page + 1} / {totalPages}
                        </span>
                        <div style={{ display: 'flex', gap: '8px' }}>
                            <button 
                                className="page-btn" 
                                disabled={page === 0}
                                onClick={() => setPage(page - 1)}
                            >
                                &larr; Önceki
                            </button>
                            <button 
                                className="page-btn" 
                                disabled={page >= totalPages - 1}
                                onClick={() => setPage(page + 1)}
                            >
                                Sonraki &rarr;
                            </button>
                        </div>
                    </div>
                )}
            </div>
        </div>
    );
};
