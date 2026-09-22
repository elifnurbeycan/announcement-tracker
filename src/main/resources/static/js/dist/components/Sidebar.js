window.Sidebar = function Sidebar({
  activeTab,
  setActiveTab,
  collapsed,
  mobileOpen,
  role = 'ROLE_ADMIN'
}) {
  React.useEffect(() => {
    if (window.lucide) window.lucide.createIcons();
  }, [activeTab, collapsed, mobileOpen]);
  const isAdmin = role === 'ROLE_ADMIN';
  return React.createElement("aside", {
    className: `sidebar ${collapsed ? 'collapsed' : ''} ${mobileOpen ? 'mobile-open' : ''}`
  }, React.createElement("div", {
    className: "sidebar-brand"
  }, React.createElement("div", {
    className: "brand-icon"
  }, React.createElement("i", {
    "data-lucide": "bell-ring",
    style: {
      width: '22px',
      height: '22px',
      color: 'white'
    }
  })), !collapsed && React.createElement("div", {
    className: "brand-text"
  }, React.createElement("h1", null, "e-Duyuru Takip"), React.createElement("p", null, isAdmin ? 'Super Admin Paneli' : 'Kullanıcı Portal'))), React.createElement("nav", {
    className: "sidebar-nav"
  }, isAdmin && React.createElement("a", {
    href: "#/overview",
    className: `nav-item ${activeTab === 'overview' ? 'active' : ''}`,
    onClick: () => setActiveTab('overview')
  }, React.createElement("i", {
    "data-lucide": "layout-dashboard"
  }), !collapsed && React.createElement("span", null, "Genel Bak\u0131\u015F")), React.createElement("a", {
    href: "#/announcements",
    className: `nav-item ${activeTab === 'announcements' ? 'active' : ''}`,
    onClick: () => setActiveTab('announcements')
  }, React.createElement("i", {
    "data-lucide": "megaphone"
  }), !collapsed && React.createElement("span", null, "Duyurular")), isAdmin && React.createElement(React.Fragment, null, React.createElement("a", {
    href: "#/subscribers",
    className: `nav-item ${activeTab === 'subscribers' ? 'active' : ''}`,
    onClick: () => setActiveTab('subscribers')
  }, React.createElement("i", {
    "data-lucide": "users"
  }), !collapsed && React.createElement("span", null, "Aboneler")), React.createElement("a", {
    href: "#/departments",
    className: `nav-item ${activeTab === 'departments' ? 'active' : ''}`,
    onClick: () => setActiveTab('departments')
  }, React.createElement("i", {
    "data-lucide": "building-2"
  }), !collapsed && React.createElement("span", null, "Departmanlar")), React.createElement("a", {
    href: "#/sources",
    className: `nav-item ${activeTab === 'sources' ? 'active' : ''}`,
    onClick: () => setActiveTab('sources')
  }, React.createElement("i", {
    "data-lucide": "globe"
  }), !collapsed && React.createElement("span", null, "Kaynaklar")), React.createElement("a", {
    href: "#/settings",
    className: `nav-item ${activeTab === 'settings' ? 'active' : ''}`,
    onClick: () => setActiveTab('settings')
  }, React.createElement("i", {
    "data-lucide": "settings"
  }), !collapsed && React.createElement("span", null, "Ayarlar"))), !isAdmin && React.createElement("a", {
    href: "#/profile",
    className: `nav-item ${activeTab === 'profile' ? 'active' : ''}`,
    onClick: () => setActiveTab('profile')
  }, React.createElement("i", {
    "data-lucide": "user"
  }), !collapsed && React.createElement("span", null, "Profilim"))));
};
