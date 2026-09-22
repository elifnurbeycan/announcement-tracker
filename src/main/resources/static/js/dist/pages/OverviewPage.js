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
  return React.createElement("div", null, React.createElement("div", {
    className: "stats-grid"
  }, React.createElement("div", {
    className: "stat-card"
  }, React.createElement("div", {
    className: "stat-info"
  }, React.createElement("div", {
    className: "title"
  }, "Toplam Duyuru"), React.createElement("div", {
    className: "value"
  }, displayTotalAnnouncements)), React.createElement("div", {
    className: "stat-icon",
    style: {
      background: 'rgba(59, 130, 246, 0.15)',
      color: '#60a5fa'
    }
  }, React.createElement("i", {
    "data-lucide": "megaphone"
  }))), React.createElement("div", {
    className: "stat-card"
  }, React.createElement("div", {
    className: "stat-info"
  }, React.createElement("div", {
    className: "title"
  }, "Kay\u0131tl\u0131 Aboneler"), React.createElement("div", {
    className: "value"
  }, subscribers.length)), React.createElement("div", {
    className: "stat-icon",
    style: {
      background: 'rgba(16, 185, 129, 0.15)',
      color: '#34d399'
    }
  }, React.createElement("i", {
    "data-lucide": "users"
  }))), React.createElement("div", {
    className: "stat-card"
  }, React.createElement("div", {
    className: "stat-info"
  }, React.createElement("div", {
    className: "title"
  }, "Departman Say\u0131s\u0131"), React.createElement("div", {
    className: "value"
  }, departments.length)), React.createElement("div", {
    className: "stat-icon",
    style: {
      background: 'rgba(139, 92, 246, 0.15)',
      color: '#c084fc'
    }
  }, React.createElement("i", {
    "data-lucide": "building-2"
  }))), React.createElement("div", {
    className: "stat-card"
  }, React.createElement("div", {
    className: "stat-info"
  }, React.createElement("div", {
    className: "title"
  }, "Takip Edilen Kaynaklar"), React.createElement("div", {
    className: "value"
  }, availableSites.length)), React.createElement("div", {
    className: "stat-icon",
    style: {
      background: 'rgba(6, 182, 212, 0.15)',
      color: '#22d3ee'
    }
  }, React.createElement("i", {
    "data-lucide": "globe"
  })))), React.createElement("div", {
    className: "card"
  }, React.createElement("div", {
    className: "card-header-flex"
  }, React.createElement("h2", null, React.createElement("i", {
    "data-lucide": "sparkles",
    style: {
      color: '#60a5fa',
      width: '20px',
      height: '20px'
    }
  }), "Son Yakalanan Duyurular"), React.createElement("button", {
    className: "btn-action-secondary",
    onClick: () => onNavigate('announcements')
  }, "T\xFCm\xFCn\xFC G\xF6r \u2192")), React.createElement("div", {
    className: "announcements-grid"
  }, recentAnnouncements.length === 0 ? React.createElement("div", {
    style: {
      textAlign: 'center',
      padding: '30px',
      color: 'var(--text-sub)'
    }
  }, "Hen\xFCz duyuru bulunmuyor.") : recentAnnouncements.map((item, index) => React.createElement("div", {
    key: item.id || index,
    className: "announcement-card"
  }, React.createElement("div", {
    className: "card-top"
  }, React.createElement("span", {
    className: "site-badge"
  }, item.sourceSite), React.createElement("span", {
    className: "card-date"
  }, React.createElement("i", {
    "data-lucide": "calendar",
    style: {
      width: '14px',
      height: '14px'
    }
  }), item.announcementDate ? new Date(item.announcementDate).toLocaleDateString('tr-TR') : 'Tarih Belirtilmedi')), React.createElement("h3", {
    className: "card-title-text"
  }, item.title), item.content && React.createElement("p", {
    className: "card-body-text"
  }, item.content.substring(0, 160), "..."), React.createElement("div", {
    className: "card-footer"
  }, item.sourceUrl && React.createElement("a", {
    href: item.sourceUrl,
    target: "_blank",
    rel: "noopener noreferrer",
    className: "btn-link btn-source"
  }, React.createElement("i", {
    "data-lucide": "external-link",
    style: {
      width: '14px',
      height: '14px'
    }
  }), "Kayna\u011Fa Git"), item.attachmentUrl && React.createElement("a", {
    href: item.attachmentUrl,
    target: "_blank",
    rel: "noopener noreferrer",
    className: "btn-link btn-pdf"
  }, React.createElement("i", {
    "data-lucide": "file-text",
    style: {
      width: '14px',
      height: '14px'
    }
  }), "Ek Dosya")))))));
};
