function UserDashboardApp() {
  const [user, setUser] = React.useState({});
  const [activeTab, setActiveTab] = React.useState('announcements');
  const [collapsed, setCollapsed] = React.useState(false);
  const [mobileOpen, setMobileOpen] = React.useState(false);
  const [userDropdownOpen, setUserDropdownOpen] = React.useState(false);
  const [announcements, setAnnouncements] = React.useState([]);
  const [availableSites, setAvailableSites] = React.useState(['EBELGE_GIB']);
  const [page, setPage] = React.useState(0);
  const [totalPages, setTotalPages] = React.useState(1);
  const [totalElements, setTotalElements] = React.useState(0);
  const [searchQuery, setSearchQuery] = React.useState('');
  const [selectedSiteFilter, setSelectedSiteFilter] = React.useState('');
  const [hasAttachmentFilter, setHasAttachmentFilter] = React.useState(false);
  React.useEffect(() => {
    const loadProfileAndSites = async () => {
      try {
        const [profile, sites] = await Promise.all([window.AuthService.getUserProfile(), window.AnnouncementService.getSites()]);
        sessionStorage.setItem('authRole', 'USER');
        setUser(profile);
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
  const loadUserAnnouncements = React.useCallback(async () => {
    try {
      const data = await window.AnnouncementService.getUserAnnouncements(page, 10, selectedSiteFilter, searchQuery, hasAttachmentFilter);
      if (data && data.content) {
        setAnnouncements(data.content);
        setTotalPages(data.totalPages || 1);
        setTotalElements(data.totalElements !== undefined ? data.totalElements : data.content.length);
      }
    } catch (e) {
      console.error('Failed to load user announcements:', e);
    }
  }, [page, searchQuery, selectedSiteFilter, hasAttachmentFilter]);
  React.useEffect(() => {
    loadUserAnnouncements();
  }, [loadUserAnnouncements]);
  const updatePreferences = async siteTypes => {
    const updatedUser = await window.AuthService.updateUserPreferences(siteTypes);
    setUser(updatedUser);
    await loadUserAnnouncements();
    return updatedUser;
  };
  return React.createElement("div", {
    className: "dashboard-container"
  }, React.createElement(window.Sidebar, {
    activeTab: activeTab,
    setActiveTab: setActiveTab,
    collapsed: collapsed,
    mobileOpen: mobileOpen,
    role: "ROLE_USER"
  }), React.createElement("div", {
    className: `main-wrapper ${collapsed ? 'collapsed' : ''}`
  }, React.createElement(window.Header, {
    activeTab: activeTab,
    collapsed: collapsed,
    setCollapsed: setCollapsed,
    mobileOpen: mobileOpen,
    setMobileOpen: setMobileOpen,
    userDropdownOpen: userDropdownOpen,
    setUserDropdownOpen: setUserDropdownOpen,
    onLogout: () => window.AuthService.logoutUser(),
    user: user,
    role: "ROLE_USER"
  }), React.createElement("main", {
    className: "content-body"
  }, activeTab === 'announcements' && React.createElement(window.AnnouncementsPage, {
    announcements: announcements,
    availableSites: availableSites,
    searchQuery: searchQuery,
    setSearchQuery: setSearchQuery,
    selectedSiteFilter: selectedSiteFilter,
    setSelectedSiteFilter: setSelectedSiteFilter,
    hasAttachmentFilter: hasAttachmentFilter,
    setHasAttachmentFilter: setHasAttachmentFilter,
    page: page,
    setPage: setPage,
    totalPages: totalPages,
    totalElements: totalElements
  }), activeTab === 'profile' && React.createElement(window.ProfilePage, {
    user: user,
    role: "ROLE_USER",
    availableSites: availableSites,
    onUpdatePreferences: updatePreferences
  }))));
}
ReactDOM.render(React.createElement(UserDashboardApp, null), document.getElementById('root'));
