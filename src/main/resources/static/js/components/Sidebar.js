/**
 * Sidebar Component
 */
window.Sidebar = function Sidebar({ activeTab, setActiveTab, collapsed, mobileOpen, role = 'ROLE_ADMIN' }) {
    React.useEffect(() => {
        if (window.lucide) window.lucide.createIcons();
    }, [activeTab, collapsed, mobileOpen]);

    const isAdmin = role === 'ROLE_ADMIN';

    return (
        <aside className={`sidebar ${collapsed ? 'collapsed' : ''} ${mobileOpen ? 'mobile-open' : ''}`}>
            <div className="sidebar-brand">
                <div className="brand-icon">
                    <i data-lucide="bell-ring" style={{ width: '22px', height: '22px', color: 'white' }}></i>
                </div>
                {!collapsed && (
                    <div className="brand-text">
                        <h1>e-Duyuru Takip</h1>
                        <p>{isAdmin ? 'Super Admin Paneli' : 'Kullanıcı Portal'}</p>
                    </div>
                )}
            </div>

            <nav className="sidebar-nav">
                {isAdmin && (
                    <a 
                        href="#/overview"
                        className={`nav-item ${activeTab === 'overview' ? 'active' : ''}`}
                        onClick={() => setActiveTab('overview')}
                    >
                        <i data-lucide="layout-dashboard"></i>
                        {!collapsed && <span>Genel Bakış</span>}
                    </a>
                )}

                <a 
                    href="#/announcements"
                    className={`nav-item ${activeTab === 'announcements' ? 'active' : ''}`}
                    onClick={() => setActiveTab('announcements')}
                >
                    <i data-lucide="megaphone"></i>
                    {!collapsed && <span>Duyurular</span>}
                </a>

                {isAdmin && (
                    <>
                        <a 
                            href="#/subscribers"
                            className={`nav-item ${activeTab === 'subscribers' ? 'active' : ''}`}
                            onClick={() => setActiveTab('subscribers')}
                        >
                            <i data-lucide="users"></i>
                            {!collapsed && <span>Aboneler</span>}
                        </a>

                        <a 
                            href="#/departments"
                            className={`nav-item ${activeTab === 'departments' ? 'active' : ''}`}
                            onClick={() => setActiveTab('departments')}
                        >
                            <i data-lucide="building-2"></i>
                            {!collapsed && <span>Departmanlar</span>}
                        </a>

                        <a 
                            href="#/sources"
                            className={`nav-item ${activeTab === 'sources' ? 'active' : ''}`}
                            onClick={() => setActiveTab('sources')}
                        >
                            <i data-lucide="globe"></i>
                            {!collapsed && <span>Kaynaklar</span>}
                        </a>

                        <a 
                            href="#/settings"
                            className={`nav-item ${activeTab === 'settings' ? 'active' : ''}`}
                            onClick={() => setActiveTab('settings')}
                        >
                            <i data-lucide="settings"></i>
                            {!collapsed && <span>Ayarlar</span>}
                        </a>
                    </>
                )}

                {!isAdmin && (
                    <a
                        href="#/profile"
                        className={`nav-item ${activeTab === 'profile' ? 'active' : ''}`}
                        onClick={() => setActiveTab('profile')}
                    >
                        <i data-lucide="user"></i>
                        {!collapsed && <span>Profilim</span>}
                    </a>
                )}
            </nav>
        </aside>
    );
};
