/**
 * User Dashboard Application Container (Normal Employee Portal)
 */
function UserDashboardApp() {
    const [user, setUser] = React.useState({});
    const [activeTab, setActiveTab] = React.useState('announcements');
    const [collapsed, setCollapsed] = React.useState(false);
    const [mobileOpen, setMobileOpen] = React.useState(false);
    const [userDropdownOpen, setUserDropdownOpen] = React.useState(false);

    // Data State
    const [announcements, setAnnouncements] = React.useState([]);
    const [availableSites, setAvailableSites] = React.useState(['EBELGE_GIB', 'KOSGEB']);

    // Pagination & Search State
    const [page, setPage] = React.useState(0);
    const [totalPages, setTotalPages] = React.useState(1);
    const [totalElements, setTotalElements] = React.useState(0);
    const [searchQuery, setSearchQuery] = React.useState('');
    const [selectedSiteFilter, setSelectedSiteFilter] = React.useState('');
    const [hasAttachmentFilter, setHasAttachmentFilter] = React.useState(false);

    // Initialize & Route Handle
    React.useEffect(() => {
        const token = localStorage.getItem('userToken');
        if (!token) {
            window.location.href = '/user-login.html';
            return;
        }

        const userData = window.AuthService.getUserData();
        setUser(userData);

        const loadProfileAndSites = async () => {
            try {
                const [profile, sites] = await Promise.all([
                    window.AuthService.getUserProfile(),
                    window.AnnouncementService.getSites()
                ]);
                setUser(profile);
                localStorage.setItem('userData', JSON.stringify(profile));
                if (Array.isArray(sites)) {
                    setAvailableSites(sites.map(site => typeof site === 'string' ? site : site.name));
                }
            } catch (error) {
                console.error('Kullanıcı profili yüklenemedi:', error);
            }
        };
        loadProfileAndSites();

        const hash = window.location.hash.replace('#/', '');
        if (hash && ['announcements', 'profile'].includes(hash)) {
            setActiveTab(hash);
        }

        const handleHashChange = () => {
            const h = window.location.hash.replace('#/', '');
            if (h && ['announcements', 'profile'].includes(h)) {
                setActiveTab(h);
            }
        };

        window.addEventListener('hashchange', handleHashChange);
        return () => window.removeEventListener('hashchange', handleHashChange);
    }, []);

    // Load User Announcements
    const loadUserAnnouncements = React.useCallback(async () => {
        try {
            const data = await window.AnnouncementService.getUserAnnouncements(page, 10, searchQuery);
            if (data && data.content) {
                let items = data.content;
                if (selectedSiteFilter) {
                    items = items.filter(a => a.sourceSite === selectedSiteFilter);
                }
                if (hasAttachmentFilter) {
                    items = items.filter(a => a.attachmentUrl && a.attachmentUrl.trim() !== '');
                }
                setAnnouncements(items);
                setTotalPages(data.totalPages || 1);
                setTotalElements(data.totalElements || items.length);
            }
        } catch(e) {
            console.error('Failed to load user announcements:', e);
        }
    }, [page, searchQuery, selectedSiteFilter, hasAttachmentFilter]);

    React.useEffect(() => {
        loadUserAnnouncements();
    }, [loadUserAnnouncements]);

    const updatePreferences = async (siteTypes) => {
        const updatedUser = await window.AuthService.updateUserPreferences(siteTypes);
        setUser(updatedUser);
        await loadUserAnnouncements();
        return updatedUser;
    };

    return (
        <div className="dashboard-container">
            <window.Sidebar 
                activeTab={activeTab} 
                setActiveTab={setActiveTab}
                collapsed={collapsed}
                mobileOpen={mobileOpen}
                role="ROLE_USER"
            />

            <div className={`main-wrapper ${collapsed ? 'collapsed' : ''}`}>
                <window.Header 
                    activeTab={activeTab}
                    collapsed={collapsed}
                    setCollapsed={setCollapsed}
                    mobileOpen={mobileOpen}
                    setMobileOpen={setMobileOpen}
                    userDropdownOpen={userDropdownOpen}
                    setUserDropdownOpen={setUserDropdownOpen}
                    onLogout={() => window.AuthService.logoutUser()}
                    user={user}
                    role="ROLE_USER"
                />

                <main className="content-body">
                    {activeTab === 'announcements' && (
                        <window.AnnouncementsPage 
                            announcements={announcements}
                            availableSites={availableSites}
                            searchQuery={searchQuery}
                            setSearchQuery={setSearchQuery}
                            selectedSiteFilter={selectedSiteFilter}
                            setSelectedSiteFilter={setSelectedSiteFilter}
                            hasAttachmentFilter={hasAttachmentFilter}
                            setHasAttachmentFilter={setHasAttachmentFilter}
                            page={page}
                            setPage={setPage}
                            totalPages={totalPages}
                            totalElements={totalElements}
                        />
                    )}

                    {activeTab === 'profile' && (
                        <window.ProfilePage 
                            user={user}
                            role="ROLE_USER"
                            availableSites={availableSites}
                            onUpdatePreferences={updatePreferences}
                        />
                    )}
                </main>
            </div>
        </div>
    );
}

ReactDOM.render(<UserDashboardApp />, document.getElementById('root'));
