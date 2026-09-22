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
  return React.createElement("div", null, React.createElement("div", {
    className: "toolbar-card"
  }, React.createElement("div", {
    className: "search-input-group"
  }, React.createElement("i", {
    "data-lucide": "search"
  }), React.createElement("input", {
    type: "text",
    className: "input-field",
    placeholder: "Duyuru ara (ba\u015Fl\u0131k, i\xE7erik)...",
    value: searchQuery,
    onChange: e => setSearchQuery(e.target.value)
  })), React.createElement("div", {
    className: "filter-pills"
  }, React.createElement("button", {
    className: `filter-pill ${selectedSiteFilter === '' ? 'active' : ''}`,
    onClick: () => setSelectedSiteFilter('')
  }, "T\xFCm Kaynaklar"), availableSites.map(site => React.createElement("button", {
    key: site,
    className: `filter-pill ${selectedSiteFilter === site ? 'active' : ''}`,
    onClick: () => setSelectedSiteFilter(site)
  }, site)), React.createElement("button", {
    className: `filter-pill ${hasAttachmentFilter ? 'active' : ''}`,
    onClick: () => setHasAttachmentFilter(!hasAttachmentFilter),
    style: {
      borderStyle: 'dashed'
    }
  }, React.createElement("i", {
    "data-lucide": "paperclip",
    style: {
      width: '14px',
      height: '14px',
      display: 'inline-block',
      verticalAlign: 'middle',
      marginRight: '4px'
    }
  }), "Ek Dosyal\u0131"))), React.createElement("div", {
    className: "card"
  }, React.createElement("div", {
    className: "card-header-flex"
  }, React.createElement("h2", null, React.createElement("i", {
    "data-lucide": "list",
    style: {
      color: '#60a5fa',
      width: '20px',
      height: '20px'
    }
  }), "Duyuru Listesi (", totalElements, ")")), React.createElement("div", {
    className: "announcements-grid"
  }, announcements.length === 0 ? React.createElement("div", {
    style: {
      textAlign: 'center',
      padding: '40px',
      color: 'var(--text-sub)'
    }
  }, "Filtrelere uygun duyuru bulunamad\u0131.") : announcements.map((item, index) => React.createElement("div", {
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
  }, item.content), React.createElement("div", {
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
  }), "Resmi Kayna\u011Fa Git"), item.attachmentUrl && React.createElement("a", {
    href: item.attachmentUrl,
    target: "_blank",
    rel: "noopener noreferrer",
    className: "btn-link btn-pdf"
  }, React.createElement("i", {
    "data-lucide": "download",
    style: {
      width: '14px',
      height: '14px'
    }
  }), "Ek Dosya \u0130ndir"))))), totalPages > 1 && React.createElement("div", {
    className: "pagination"
  }, React.createElement("span", {
    style: {
      fontSize: '13px',
      color: 'var(--text-sub)'
    }
  }, "Sayfa ", page + 1, " / ", totalPages), React.createElement("div", {
    style: {
      display: 'flex',
      gap: '8px'
    }
  }, React.createElement("button", {
    className: "page-btn",
    disabled: page === 0,
    onClick: () => setPage(page - 1)
  }, "\u2190 \xD6nceki"), React.createElement("button", {
    className: "page-btn",
    disabled: page >= totalPages - 1,
    onClick: () => setPage(page + 1)
  }, "Sonraki \u2192")))));
};
