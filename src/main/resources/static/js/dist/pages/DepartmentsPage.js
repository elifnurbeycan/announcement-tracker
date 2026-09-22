window.DepartmentsPage = function DepartmentsPage({
  departments = [],
  subscribers = [],
  onOpenAddModal,
  onOpenEditModal,
  onDeleteDepartment,
  onBulkDeleteDepartments
}) {
  const [selectionMode, setSelectionMode] = React.useState(false);
  const [selectedIds, setSelectedIds] = React.useState([]);
  React.useEffect(() => {
    if (window.lucide) window.lucide.createIcons();
  }, [departments, subscribers, selectionMode, selectedIds]);
  const isAllSelected = departments.length > 0 && selectedIds.length === departments.length;
  const handleSelectAll = () => {
    if (isAllSelected) {
      setSelectedIds([]);
    } else {
      setSelectedIds(departments.map(d => d.id));
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
    if (onBulkDeleteDepartments && selectedIds.length > 0) {
      onBulkDeleteDepartments(selectedIds, () => {
        setSelectedIds([]);
        setSelectionMode(false);
      });
    }
  };
  const getDeptMemberCount = deptId => {
    return subscribers.filter(s => {
      if (s.departments && s.departments.length > 0) {
        return s.departments.some(d => d.id === deptId);
      }
      return s.departmentId === deptId;
    }).length;
  };
  return React.createElement("div", null, React.createElement("div", {
    className: "card-header-flex",
    style: {
      marginBottom: '24px'
    }
  }, React.createElement("div", null, React.createElement("h2", {
    style: {
      fontSize: '20px',
      fontWeight: '800',
      color: 'white'
    }
  }, "Departman Y\xF6netimi"), React.createElement("p", {
    style: {
      fontSize: '13px',
      color: 'var(--text-sub)',
      marginTop: '4px'
    }
  }, "Kurumsal departmanlar\u0131 ve bu departmanlara atanan otomatik duyuru kaynaklar\u0131n\u0131 y\xF6netin.")), React.createElement("div", {
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
    className: "btn-action-primary",
    onClick: onOpenAddModal
  }, React.createElement("i", {
    "data-lucide": "plus",
    style: {
      width: '16px',
      height: '16px'
    }
  }), "+ Departman Ekle"))), React.createElement("div", {
    className: "card"
  }, React.createElement("div", {
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
  })), React.createElement("th", null, "Departman Ad\u0131"), React.createElement("th", null, "Atanan Kaynaklar"), React.createElement("th", null, "Ba\u011Fl\u0131 Abone Say\u0131s\u0131"), React.createElement("th", {
    style: {
      textAlign: 'right'
    }
  }, "\u0130\u015Flemler"))), React.createElement("tbody", null, departments.length === 0 ? React.createElement("tr", null, React.createElement("td", {
    colSpan: selectionMode ? "5" : "4",
    style: {
      textAlign: 'center',
      padding: '30px',
      color: 'var(--text-sub)'
    }
  }, "Kay\u0131tl\u0131 departman bulunmuyor.")) : departments.map(dept => {
    const isSelected = selectedIds.includes(dept.id);
    const memberCount = getDeptMemberCount(dept.id);
    const sites = dept.sites || [];
    return React.createElement("tr", {
      key: dept.id,
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
      onChange: () => handleToggleSelect(dept.id),
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
    }, dept.name), React.createElement("td", null, React.createElement("div", {
      style: {
        display: 'flex',
        flexWrap: 'wrap',
        gap: '6px'
      }
    }, sites.length === 0 ? React.createElement("span", {
      style: {
        fontSize: '12px',
        color: 'var(--text-sub)',
        fontStyle: 'italic'
      }
    }, "Sitesiz Departman") : sites.map(site => React.createElement("span", {
      key: site,
      className: "site-badge"
    }, site)))), React.createElement("td", {
      style: {
        fontSize: '13px',
        color: 'var(--text-sub)'
      }
    }, memberCount, " Abone"), React.createElement("td", {
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
        background: 'rgba(59, 130, 246, 0.15)',
        color: '#60a5fa',
        borderColor: 'rgba(59, 130, 246, 0.3)'
      },
      onClick: () => onOpenEditModal(dept)
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
      onClick: () => onDeleteDepartment(dept.id)
    }, React.createElement("i", {
      "data-lucide": "trash-2",
      style: {
        width: '14px',
        height: '14px'
      }
    }), "Sil"))));
  }))))));
};
