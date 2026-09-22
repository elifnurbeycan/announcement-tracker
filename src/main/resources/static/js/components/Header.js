/**
 * Header Component
 */
window.Header = function Header({
    activeTab,
    collapsed,
    setCollapsed,
    mobileOpen,
    setMobileOpen,
    handleScrape,
    scraping,
    userDropdownOpen,
    setUserDropdownOpen,
    onLogout,
    user = {},
    role = 'ROLE_ADMIN'
}) {
    React.useEffect(() => {
        if (window.lucide) window.lucide.createIcons();
    }, [activeTab, collapsed, mobileOpen, scraping, userDropdownOpen]);

    const tabTitles = {
        overview: 'Genel Bakış',
        announcements: 'Duyurular',
        subscribers: 'Abone Yönetimi',
        departments: 'Departman Yönetimi',
        sources: 'Kaynak Takibi',
        settings: 'Sistem Ayarları',
        profile: 'Kullanıcı Profilim'
    };

    const isAdmin = role === 'ROLE_ADMIN';
    const displayName = user.fullName || user.username || user.email || (isAdmin ? 'Admin' : 'Kullanıcı');
    const avatarLetter = displayName.charAt(0).toUpperCase();

    return (
        <header className="header-bar">
            <div className="header-left">
                <button 
                    className="toggle-btn" 
                    onClick={() => {
                        if (window.innerWidth <= 900) setMobileOpen(!mobileOpen);
                        else setCollapsed(!collapsed);
                    }}
                >
                    <i data-lucide={collapsed ? "menu" : "panel-left-close"}></i>
                </button>
                <div className="page-title-badge">
                    <span>{tabTitles[activeTab] || 'Dashboard'}</span>
                </div>
            </div>

            <div className="header-right">
                {isAdmin && handleScrape && (
                    <button className="btn-scrape" onClick={handleScrape} disabled={scraping}>
                        <i data-lucide="refresh-cw" className={scraping ? "spin" : ""}></i>
                        <span>{scraping ? 'Taranıyor...' : 'Şimdi Tara'}</span>
                    </button>
                )}

                <div className="user-dropdown">
                    <div className="user-trigger" onClick={() => setUserDropdownOpen(!userDropdownOpen)}>
                        <div className="avatar">{avatarLetter}</div>
                        <span style={{ fontSize: '13px', fontWeight: '600' }}>{displayName}</span>
                        <i data-lucide="chevron-down" style={{ width: '16px', height: '16px' }}></i>
                    </div>

                    {userDropdownOpen && (
                        <div className="dropdown-menu">
                            {!isAdmin && (
                                <a href="#/profile" className="dropdown-item" onClick={() => setUserDropdownOpen(false)}>
                                    <i data-lucide="user" style={{ width: '16px', height: '16px' }}></i>
                                    Profilim
                                </a>
                            )}
                            {isAdmin && (
                                <a href="#/settings" className="dropdown-item" onClick={() => setUserDropdownOpen(false)}>
                                    <i data-lucide="settings" style={{ width: '16px', height: '16px' }}></i>
                                    Ayarlar
                                </a>
                            )}
                            <div className="dropdown-item danger" onClick={onLogout}>
                                <i data-lucide="log-out" style={{ width: '16px', height: '16px' }}></i>
                                Çıkış Yap
                            </div>
                        </div>
                    )}
                </div>
            </div>
        </header>
    );
};
