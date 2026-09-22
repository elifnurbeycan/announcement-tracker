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
  const taklitAnnouncements = announcements.filter(item => item.sourceSite === 'TAKLIT_TAGSIS');
  const standardAnnouncements = announcements.filter(item => item.sourceSite !== 'TAKLIT_TAGSIS');
  const cleanTaklitValue = (value = '') => value.replace(/<\/?br\s*\/?>/gi, ' · ').replace(/\s*·\s*(?:·\s*)+/g, ' · ').replace(/\s+/g, ' ').trim();
  const parseTaklitDetails = (content = '') => {
    const details = {};
    content.split('|').forEach(part => {
      const separatorIndex = part.indexOf(':');
      if (separatorIndex < 0) return;
      const key = part.slice(0, separatorIndex).trim();
      const value = cleanTaklitValue(part.slice(separatorIndex + 1));
      details[key] = value;
    });
    const location = (details['İl/İlçe'] || '').split('/');
    return {
      company: details.Firma || '-',
      brand: details.Marka || '-',
      product: details['Ürün'] || '-',
      violation: details.Uygunsuzluk || '-',
      batchNumber: details['Parti/Seri No'] || '-',
      district: location.length > 1 ? location.slice(1).join('/').trim() : '-',
      city: location[0] ? location[0].trim() : '-',
      productGroup: details['Ürün Grubu'] || '-'
    };
  };
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
    onChange: e => {
      setPage(0);
      setSearchQuery(e.target.value);
    }
  })), React.createElement("div", {
    className: "filter-pills"
  }, React.createElement("button", {
    className: `filter-pill ${selectedSiteFilter === '' ? 'active' : ''}`,
    onClick: () => {
      setPage(0);
      setSelectedSiteFilter('');
    }
  }, "T\xFCm Kaynaklar"), availableSites.map(site => React.createElement("button", {
    key: site,
    className: `filter-pill ${selectedSiteFilter === site ? 'active' : ''}`,
    onClick: () => {
      setPage(0);
      setSelectedSiteFilter(site);
    }
  }, site)), React.createElement("button", {
    className: `filter-pill ${hasAttachmentFilter ? 'active' : ''}`,
    onClick: () => {
      setPage(0);
      setHasAttachmentFilter(!hasAttachmentFilter);
    },
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
  }, announcements.length === 0 && React.createElement("div", {
    style: {
      textAlign: 'center',
      padding: '40px',
      color: 'var(--text-sub)'
    }
  }, "Filtrelere uygun duyuru bulunamad\u0131."), taklitAnnouncements.length > 0 && React.createElement("div", {
    className: "table-responsive taklit-table-wrapper"
  }, React.createElement("table", {
    className: "data-table taklit-table"
  }, React.createElement("thead", null, React.createElement("tr", null, React.createElement("th", null, "Kamuoyu Duyuru Tarihi"), React.createElement("th", null, "Firma Ad\u0131"), React.createElement("th", null, "Marka"), React.createElement("th", null, "\xDCr\xFCn Ad\u0131"), React.createElement("th", null, "Uygunsuzluk"), React.createElement("th", null, "Parti/Seri No"), React.createElement("th", null, "\u0130l\xE7e"), React.createElement("th", null, "\u0130l"), React.createElement("th", null, "\xDCr\xFCn Grubu"), React.createElement("th", null, "Kaynak"))), React.createElement("tbody", null, taklitAnnouncements.map((item, index) => {
    const details = parseTaklitDetails(item.content);
    return React.createElement("tr", {
      key: item.id || index
    }, React.createElement("td", {
      className: "taklit-date-cell"
    }, item.announcementDate ? new Date(item.announcementDate).toLocaleDateString('tr-TR') : '-'), React.createElement("td", {
      className: "taklit-company-cell"
    }, details.company), React.createElement("td", null, details.brand), React.createElement("td", null, details.product), React.createElement("td", null, details.violation), React.createElement("td", null, details.batchNumber), React.createElement("td", null, details.district), React.createElement("td", null, details.city), React.createElement("td", null, details.productGroup), React.createElement("td", null, item.sourceUrl ? React.createElement("a", {
      href: item.sourceUrl,
      target: "_blank",
      rel: "noopener noreferrer",
      className: "btn-link btn-source taklit-source-link"
    }, React.createElement("i", {
      "data-lucide": "external-link"
    }), "A\xE7") : '-'));
  })))), standardAnnouncements.map((item, index) => React.createElement("div", {
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
