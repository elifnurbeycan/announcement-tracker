window.SubscribersPage = function SubscribersPage({
  subscribers = [],
  onOpenAddModal,
  onOpenEditModal,
  onOpenImportModal,
  onToggleStatus,
  onSendPasswordSetupEmail,
  onDeleteSubscriber,
  onBulkDeleteSubscribers
}) {
  const [search, setSearch] = React.useState('');
  const [selectionMode, setSelectionMode] = React.useState(false);
  const [selectedIds, setSelectedIds] = React.useState([]);
  React.useEffect(() => {
    if (window.lucide) window.lucide.createIcons();
  }, [subscribers, search, selectionMode, selectedIds]);
  const filteredSubscribers = subscribers.filter(s => {
    const query = search.toLowerCase();
    const nameMatch = s.fullName && s.fullName.toLowerCase().includes(query);
    const emailMatch = s.email && s.email.toLowerCase().includes(query);
    return nameMatch || emailMatch;
  });
  const isAllSelected = filteredSubscribers.length > 0 && selectedIds.length === filteredSubscribers.length;
  const handleSelectAll = () => {
    if (isAllSelected) {
      setSelectedIds([]);
    } else {
      setSelectedIds(filteredSubscribers.map(s => s.id));
    }
  };
  const handleToggleSelect = id => {
    if (selectedIds.includes(id)) {
      setSelectedIds(selectedIds.filter(i => i !== id));
    } else {
      setSelectedIds([...selectedIds, id]);
    }
  };
  const handleExitSelectionMode = () => {
    setSelectionMode(false);
    setSelectedIds([]);
  };
  const handleBulkDeleteClick = () => {
    if (onBulkDeleteSubscribers && selectedIds.length > 0) {
      onBulkDeleteSubscribers(selectedIds, () => {
        setSelectedIds([]);
        setSelectionMode(false);
      });
    }
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
    placeholder: "Abone ara / filtrele (ad, e-posta)...",
    value: search,
    onChange: e => setSearch(e.target.value)
  })), React.createElement("div", {
    style: {
      display: 'flex',
      gap: '10px',
      alignItems: 'center'
    }
  }, !selectionMode ? React.createElement("button", {
    className: "btn-action-secondary",
    onClick: () => setSelectionMode(true)
  }, React.createElement("i", {
    "data-lucide": "check-square",
    style: {
      width: '16px',
      height: '16px'
    }
  }), "Toplu Se\xE7") : React.createElement(React.Fragment, null, selectedIds.length > 0 && React.createElement("button", {
    className: "btn-action-secondary",
    onClick: handleBulkDeleteClick,
    style: {
      background: 'rgba(239, 68, 68, 0.25)',
      color: '#f87171',
      border: '1px solid rgba(239, 68, 68, 0.4)',
      fontWeight: '700'
    }
  }, React.createElement("i", {
    "data-lucide": "trash-2",
    style: {
      width: '16px',
      height: '16px'
    }
  }), "Se\xE7ilenleri Sil (", selectedIds.length, ")"), React.createElement("button", {
    className: "btn-action-secondary",
    onClick: handleExitSelectionMode,
    style: {
      background: 'rgba(148, 163, 184, 0.15)',
      color: '#cbd5e1',
      border: '1px solid rgba(148, 163, 184, 0.3)'
    }
  }, React.createElement("i", {
    "data-lucide": "x",
    style: {
      width: '16px',
      height: '16px'
    }
  }), "\u0130ptal Et")), React.createElement("button", {
    className: "btn-action-secondary",
    onClick: onOpenImportModal
  }, React.createElement("i", {
    "data-lucide": "file-spread-sheet",
    style: {
      width: '16px',
      height: '16px'
    }
  }), "Excel/CSV'den \u0130\xE7e Aktar"), React.createElement("button", {
    className: "btn-action-primary",
    onClick: onOpenAddModal
  }, React.createElement("i", {
    "data-lucide": "user-plus",
    style: {
      width: '16px',
      height: '16px'
    }
  }), "+ Abone Ekle"))), React.createElement("div", {
    className: "card"
  }, React.createElement("div", {
    className: "card-header-flex"
  }, React.createElement("h2", null, React.createElement("i", {
    "data-lucide": "users",
    style: {
      color: '#60a5fa',
      width: '20px',
      height: '20px'
    }
  }), "Kay\u0131tl\u0131 Aboneler (", filteredSubscribers.length, ")", selectionMode && selectedIds.length > 0 && React.createElement("span", {
    style: {
      fontSize: '13px',
      color: '#f87171',
      marginLeft: '10px',
      fontWeight: '600'
    }
  }, "(", selectedIds.length, " abone se\xE7ildi)"))), React.createElement("div", {
    className: "table-responsive"
  }, React.createElement("table", {
    className: "data-table"
  }, React.createElement("thead", null, React.createElement("tr", null, selectionMode && React.createElement("th", {
    style: {
      width: '44px',
      textAlign: 'center'
    }
  }, React.createElement("input", {
    type: "checkbox",
    checked: isAllSelected,
    onChange: handleSelectAll,
    title: "T\xFCm\xFCn\xFC Se\xE7",
    style: {
      cursor: 'pointer',
      width: '17px',
      height: '17px',
      accentColor: '#ef4444'
    }
  })), React.createElement("th", null, "Ad Soyad"), React.createElement("th", null, "E-posta"), React.createElement("th", null, "Departman"), React.createElement("th", null, "Ki\u015Fisel Siteler"), React.createElement("th", null, "Durum"), React.createElement("th", {
    style: {
      textAlign: 'right'
    }
  }, "\u0130\u015Flemler"))), React.createElement("tbody", null, filteredSubscribers.length === 0 ? React.createElement("tr", null, React.createElement("td", {
    colSpan: selectionMode ? "7" : "6",
    style: {
      textAlign: 'center',
      padding: '30px',
      color: 'var(--text-sub)'
    }
  }, "Kay\u0131tl\u0131 abone bulunamad\u0131.")) : filteredSubscribers.map(sub => {
    const isSelected = selectedIds.includes(sub.id);
    const depts = sub.departments && sub.departments.length > 0 ? sub.departments.map(d => d.name).join(', ') : sub.departmentName || '';
    const sitesList = sub.subscribedSites || sub.preferredSites || [];
    const preferred = sitesList.length > 0 ? sitesList.join(', ') : 'Tüm Kaynaklar';
    return React.createElement("tr", {
      key: sub.id,
      style: {
        background: isSelected ? 'rgba(239, 68, 68, 0.08)' : undefined
      }
    }, selectionMode && React.createElement("td", {
      style: {
        width: '44px',
        textAlign: 'center'
      }
    }, React.createElement("input", {
      type: "checkbox",
      checked: isSelected,
      onChange: () => handleToggleSelect(sub.id),
      style: {
        cursor: 'pointer',
        width: '17px',
        height: '17px',
        accentColor: '#ef4444'
      }
    })), React.createElement("td", {
      style: {
        fontWeight: '700',
        color: 'white'
      }
    }, sub.fullName), React.createElement("td", {
      style: {
        color: 'var(--text-sub)'
      }
    }, sub.email), React.createElement("td", null, depts ? React.createElement("span", {
      style: {
        background: 'rgba(59, 130, 246, 0.15)',
        color: '#60a5fa',
        border: '1px solid rgba(59, 130, 246, 0.3)',
        padding: '3px 8px',
        borderRadius: '6px',
        fontSize: '12px',
        fontWeight: '600'
      }
    }, depts) : React.createElement("span", {
      style: {
        background: 'rgba(139, 92, 246, 0.15)',
        color: '#c084fc',
        border: '1px solid rgba(139, 92, 246, 0.3)',
        padding: '3px 8px',
        borderRadius: '6px',
        fontSize: '12px',
        fontWeight: '700'
      }
    }, "Genel \xC7al\u0131\u015Fan")), React.createElement("td", {
      style: {
        fontSize: '12.5px',
        color: 'var(--text-sub)'
      }
    }, preferred), React.createElement("td", null, React.createElement("button", {
      onClick: () => onToggleStatus(sub),
      style: {
        background: sub.active ? 'rgba(16, 185, 129, 0.15)' : 'rgba(239, 68, 68, 0.15)',
        color: sub.active ? '#34d399' : '#f87171',
        border: `1px solid ${sub.active ? 'rgba(16, 185, 129, 0.3)' : 'rgba(239, 68, 68, 0.3)'}`,
        padding: '4px 10px',
        borderRadius: '8px',
        fontSize: '12px',
        fontWeight: '700',
        cursor: 'pointer'
      }
    }, sub.active ? 'Aktif' : 'Pasif')), React.createElement("td", {
      style: {
        textAlign: 'right'
      }
    }, React.createElement("div", {
      style: {
        display: 'inline-flex',
        gap: '6px'
      }
    }, React.createElement("button", {
      className: "btn-sm-action",
      style: {
        background: 'rgba(16, 185, 129, 0.15)',
        color: '#34d399',
        borderColor: 'rgba(16, 185, 129, 0.3)'
      },
      onClick: () => onSendPasswordSetupEmail(sub),
      title: "Tek kullan\u0131ml\u0131k ve s\xFCreli \u015Fifre belirleme ba\u011Flant\u0131s\u0131n\u0131 g\xF6nder"
    }, React.createElement("i", {
      "data-lucide": "mail-key",
      style: {
        width: '14px',
        height: '14px'
      }
    }), "\u015Eifre Ba\u011Flant\u0131s\u0131"), React.createElement("button", {
      className: "btn-sm-action",
      style: {
        background: 'rgba(59, 130, 246, 0.15)',
        color: '#60a5fa',
        borderColor: 'rgba(59, 130, 246, 0.3)'
      },
      onClick: () => onOpenEditModal(sub)
    }, React.createElement("i", {
      "data-lucide": "edit-3",
      style: {
        width: '14px',
        height: '14px'
      }
    }), "D\xFCzenle"), React.createElement("button", {
      className: "btn-sm-action",
      style: {
        background: 'rgba(239, 68, 68, 0.15)',
        color: '#f87171',
        borderColor: 'rgba(239, 68, 68, 0.3)'
      },
      onClick: () => onDeleteSubscriber(sub.id)
    }, React.createElement("i", {
      "data-lucide": "trash-2",
      style: {
        width: '14px',
        height: '14px'
      }
    }), "Sil"))));
  }))))));
};
