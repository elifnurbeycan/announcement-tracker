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
  return React.createElement("header", {
    className: "header-bar"
  }, React.createElement("div", {
    className: "header-left"
  }, React.createElement("button", {
    className: "toggle-btn",
    onClick: () => {
      if (window.innerWidth <= 900) setMobileOpen(!mobileOpen);else setCollapsed(!collapsed);
    }
  }, React.createElement("i", {
    "data-lucide": collapsed ? "menu" : "panel-left-close"
  })), React.createElement("div", {
    className: "page-title-badge"
  }, React.createElement("span", null, tabTitles[activeTab] || 'Dashboard'))), React.createElement("div", {
    className: "header-right"
  }, isAdmin && handleScrape && React.createElement("button", {
    className: "btn-scrape",
    onClick: handleScrape,
    disabled: scraping
  }, React.createElement("i", {
    "data-lucide": "refresh-cw",
    className: scraping ? "spin" : ""
  }), React.createElement("span", null, scraping ? 'Taranıyor...' : 'Şimdi Tara')), React.createElement("div", {
    className: "user-dropdown"
  }, React.createElement("div", {
    className: "user-trigger",
    onClick: () => setUserDropdownOpen(!userDropdownOpen)
  }, React.createElement("div", {
    className: "avatar"
  }, avatarLetter), React.createElement("span", {
    style: {
      fontSize: '13px',
      fontWeight: '600'
    }
  }, displayName), React.createElement("i", {
    "data-lucide": "chevron-down",
    style: {
      width: '16px',
      height: '16px'
    }
  })), userDropdownOpen && React.createElement("div", {
    className: "dropdown-menu"
  }, !isAdmin && React.createElement("a", {
    href: "#/profile",
    className: "dropdown-item",
    onClick: () => setUserDropdownOpen(false)
  }, React.createElement("i", {
    "data-lucide": "user",
    style: {
      width: '16px',
      height: '16px'
    }
  }), "Profilim"), isAdmin && React.createElement("a", {
    href: "#/settings",
    className: "dropdown-item",
    onClick: () => setUserDropdownOpen(false)
  }, React.createElement("i", {
    "data-lucide": "settings",
    style: {
      width: '16px',
      height: '16px'
    }
  }), "Ayarlar"), React.createElement("div", {
    className: "dropdown-item danger",
    onClick: onLogout
  }, React.createElement("i", {
    "data-lucide": "log-out",
    style: {
      width: '16px',
      height: '16px'
    }
  }), "\xC7\u0131k\u0131\u015F Yap")))));
};
