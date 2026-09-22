window.SubscriberModal = function SubscriberModal({
  isOpen,
  onClose,
  onSave,
  editSubscriber,
  departments = [],
  availableSites = []
}) {
  if (!isOpen) return null;
  const [fullName, setFullName] = React.useState(editSubscriber ? editSubscriber.fullName : '');
  const [email, setEmail] = React.useState(editSubscriber ? editSubscriber.email : '');
  const [selectedSites, setSelectedSites] = React.useState(editSubscriber ? editSubscriber.subscribedSites || editSubscriber.preferredSites || [] : []);
  const [selectedDeptIds, setSelectedDeptIds] = React.useState(editSubscriber ? editSubscriber.departments ? editSubscriber.departments.map(d => d.id) : editSubscriber.departmentId ? [editSubscriber.departmentId] : [] : []);
  React.useEffect(() => {
    if (window.lucide) window.lucide.createIcons();
  }, []);
  const toggleSite = site => {
    if (selectedSites.includes(site)) {
      setSelectedSites(selectedSites.filter(s => s !== site));
    } else {
      setSelectedSites([...selectedSites, site]);
    }
  };
  const toggleDept = deptId => {
    if (selectedDeptIds.includes(deptId)) {
      setSelectedDeptIds(selectedDeptIds.filter(id => id !== deptId));
    } else {
      setSelectedDeptIds([...selectedDeptIds, deptId]);
    }
  };
  const handleSubmit = e => {
    e.preventDefault();
    onSave({
      id: editSubscriber ? editSubscriber.id : null,
      fullName,
      email,
      preferredSites: selectedSites,
      subscribedSites: selectedSites,
      departmentIds: selectedDeptIds
    });
  };
  return React.createElement("div", {
    className: "modal-overlay"
  }, React.createElement("div", {
    className: "modal-card"
  }, React.createElement("div", {
    style: {
      display: 'flex',
      justifyContent: 'space-between',
      alignItems: 'center',
      marginBottom: '20px'
    }
  }, React.createElement("h3", {
    style: {
      color: 'white',
      fontSize: '17px',
      fontWeight: '700'
    }
  }, editSubscriber ? 'Abone Düzenle' : 'Yeni Abone Ekle'), React.createElement("button", {
    onClick: onClose,
    style: {
      background: 'none',
      border: 'none',
      color: 'var(--text-sub)',
      cursor: 'pointer'
    }
  }, React.createElement("i", {
    "data-lucide": "x"
  }))), React.createElement("form", {
    onSubmit: handleSubmit
  }, !editSubscriber && React.createElement("div", {
    style: {
      marginBottom: '16px',
      padding: '10px 12px',
      borderRadius: '9px',
      background: 'rgba(59, 130, 246, 0.1)',
      border: '1px solid rgba(59, 130, 246, 0.25)',
      color: '#93c5fd',
      fontSize: '12px',
      lineHeight: '1.5'
    }
  }, "Kurumsal giri\u015F hesab\u0131 kaydetme s\u0131ras\u0131nda g\xFCvenli bi\xE7imde arka planda olu\u015Fturulur."), React.createElement("div", {
    className: "form-group"
  }, React.createElement("label", {
    className: "form-label"
  }, "Ad Soyad"), React.createElement("input", {
    type: "text",
    className: "modal-input",
    placeholder: "\xD6rn: Ahmet Y\u0131lmaz",
    value: fullName,
    onChange: e => setFullName(e.target.value),
    required: true
  })), React.createElement("div", {
    className: "form-group"
  }, React.createElement("label", {
    className: "form-label"
  }, "E-posta Adresi"), React.createElement("input", {
    type: "email",
    className: "modal-input",
    placeholder: "ahmet@kurum.com",
    value: email,
    onChange: e => setEmail(e.target.value),
    required: true
  })), React.createElement("div", {
    className: "form-group"
  }, React.createElement("label", {
    className: "form-label"
  }, "Departmanlar"), React.createElement("div", {
    style: {
      display: 'flex',
      flexWrap: 'wrap',
      gap: '8px',
      marginTop: '6px'
    }
  }, departments.map(d => React.createElement("label", {
    key: d.id,
    style: {
      display: 'inline-flex',
      alignItems: 'center',
      gap: '6px',
      background: selectedDeptIds.includes(d.id) ? 'rgba(59, 130, 246, 0.2)' : 'rgba(15, 23, 42, 0.6)',
      border: `1px solid ${selectedDeptIds.includes(d.id) ? '#3b82f6' : 'var(--border-color)'}`,
      padding: '6px 12px',
      borderRadius: '8px',
      fontSize: '12px',
      cursor: 'pointer',
      color: selectedDeptIds.includes(d.id) ? '#60a5fa' : 'var(--text-sub)'
    }
  }, React.createElement("input", {
    type: "checkbox",
    checked: selectedDeptIds.includes(d.id),
    onChange: () => toggleDept(d.id),
    style: {
      display: 'none'
    }
  }), d.name)))), React.createElement("div", {
    className: "form-group"
  }, React.createElement("label", {
    className: "form-label"
  }, "Ki\u015Fisel Site Tercihleri (\u0130ste\u011Fe Ba\u011Fl\u0131)"), React.createElement("div", {
    style: {
      display: 'flex',
      flexWrap: 'wrap',
      gap: '8px',
      marginTop: '6px'
    }
  }, availableSites.map(site => React.createElement("label", {
    key: site,
    style: {
      display: 'inline-flex',
      alignItems: 'center',
      gap: '6px',
      background: selectedSites.includes(site) ? 'rgba(139, 92, 246, 0.2)' : 'rgba(15, 23, 42, 0.6)',
      border: `1px solid ${selectedSites.includes(site) ? '#8b5cf6' : 'var(--border-color)'}`,
      padding: '6px 12px',
      borderRadius: '8px',
      fontSize: '12px',
      cursor: 'pointer',
      color: selectedSites.includes(site) ? '#c084fc' : 'var(--text-sub)'
    }
  }, React.createElement("input", {
    type: "checkbox",
    checked: selectedSites.includes(site),
    onChange: () => toggleSite(site),
    style: {
      display: 'none'
    }
  }), site)))), React.createElement("div", {
    style: {
      display: 'flex',
      justifyContent: 'flex-end',
      gap: '10px',
      marginTop: '24px'
    }
  }, React.createElement("button", {
    type: "button",
    className: "btn-action-secondary",
    onClick: onClose
  }, "\u0130ptal"), React.createElement("button", {
    type: "submit",
    className: "btn-action-primary"
  }, "Kaydet")))));
};
window.DepartmentModal = function DepartmentModal({
  isOpen,
  onClose,
  onSave,
  editDept,
  availableSites = []
}) {
  if (!isOpen) return null;
  const [name, setName] = React.useState(editDept ? editDept.name : '');
  const [selectedSites, setSelectedSites] = React.useState(editDept ? editDept.sites || [] : []);
  const [error, setError] = React.useState('');
  React.useEffect(() => {
    if (window.lucide) window.lucide.createIcons();
  }, []);
  const toggleSite = site => {
    if (selectedSites.includes(site)) {
      setSelectedSites(selectedSites.filter(s => s !== site));
    } else {
      setSelectedSites([...selectedSites, site]);
    }
  };
  const handleSubmit = async e => {
    e.preventDefault();
    setError('');
    try {
      await onSave({
        id: editDept ? editDept.id : null,
        name,
        sites: selectedSites
      });
    } catch (err) {
      setError(err.message || 'Departman kaydedilirken hata oluştu');
    }
  };
  return React.createElement("div", {
    className: "modal-overlay"
  }, React.createElement("div", {
    className: "modal-card"
  }, React.createElement("div", {
    style: {
      display: 'flex',
      justifyContent: 'space-between',
      alignItems: 'center',
      marginBottom: '20px'
    }
  }, React.createElement("h3", {
    style: {
      color: 'white',
      fontSize: '17px',
      fontWeight: '700'
    }
  }, editDept ? 'Departman Düzenle' : 'Yeni Departman Ekle'), React.createElement("button", {
    onClick: onClose,
    style: {
      background: 'none',
      border: 'none',
      color: 'var(--text-sub)',
      cursor: 'pointer'
    }
  }, React.createElement("i", {
    "data-lucide": "x"
  }))), error && React.createElement("div", {
    style: {
      background: 'rgba(239,68,68,0.15)',
      color: '#f87171',
      border: '1px solid rgba(239,68,68,0.3)',
      padding: '10px',
      borderRadius: '8px',
      marginBottom: '14px',
      fontSize: '13px'
    }
  }, error), React.createElement("form", {
    onSubmit: handleSubmit
  }, React.createElement("div", {
    className: "form-group"
  }, React.createElement("label", {
    className: "form-label"
  }, "Departman Ad\u0131"), React.createElement("input", {
    type: "text",
    className: "modal-input",
    placeholder: "\xD6rn: Yaz\u0131l\u0131m Geli\u015Ftirme",
    value: name,
    onChange: e => setName(e.target.value),
    required: true
  })), React.createElement("div", {
    className: "form-group"
  }, React.createElement("label", {
    className: "form-label"
  }, "Atanacak Duyuru Kaynaklar\u0131"), React.createElement("div", {
    style: {
      display: 'flex',
      flexWrap: 'wrap',
      gap: '8px',
      marginTop: '6px'
    }
  }, availableSites.map(site => React.createElement("label", {
    key: site,
    style: {
      display: 'inline-flex',
      alignItems: 'center',
      gap: '6px',
      background: selectedSites.includes(site) ? 'rgba(59, 130, 246, 0.2)' : 'rgba(15, 23, 42, 0.6)',
      border: `1px solid ${selectedSites.includes(site) ? '#3b82f6' : 'var(--border-color)'}`,
      padding: '6px 12px',
      borderRadius: '8px',
      fontSize: '12px',
      cursor: 'pointer',
      color: selectedSites.includes(site) ? '#60a5fa' : 'var(--text-sub)'
    }
  }, React.createElement("input", {
    type: "checkbox",
    checked: selectedSites.includes(site),
    onChange: () => toggleSite(site),
    style: {
      display: 'none'
    }
  }), site)))), React.createElement("div", {
    style: {
      display: 'flex',
      justifyContent: 'flex-end',
      gap: '10px',
      marginTop: '24px'
    }
  }, React.createElement("button", {
    type: "button",
    className: "btn-action-secondary",
    onClick: onClose
  }, "\u0130ptal"), React.createElement("button", {
    type: "submit",
    className: "btn-action-primary"
  }, "Kaydet")))));
};
window.ImportModal = function ImportModal({
  isOpen,
  onClose,
  onImportSuccess
}) {
  if (!isOpen) return null;
  const [file, setFile] = React.useState(null);
  const [loading, setLoading] = React.useState(false);
  const [error, setError] = React.useState('');
  React.useEffect(() => {
    if (window.lucide) window.lucide.createIcons();
  }, []);
  const handleSubmit = async e => {
    e.preventDefault();
    if (!file) return;
    setLoading(true);
    setError('');
    const formData = new FormData();
    formData.append('file', file);
    try {
      const data = await window.SubscriberService.importExcel(formData);
      onImportSuccess(data.message || 'Yükleme başarılı');
      onClose();
    } catch (err) {
      setError(err.message || 'Yükleme hatası');
    } finally {
      setLoading(false);
    }
  };
  return React.createElement("div", {
    className: "modal-overlay"
  }, React.createElement("div", {
    className: "modal-card"
  }, React.createElement("div", {
    style: {
      display: 'flex',
      justifyContent: 'space-between',
      alignItems: 'center',
      marginBottom: '20px'
    }
  }, React.createElement("h3", {
    style: {
      color: 'white',
      fontSize: '17px',
      fontWeight: '700'
    }
  }, "Toplu \u0130\xE7e Aktar"), React.createElement("button", {
    onClick: onClose,
    style: {
      background: 'none',
      border: 'none',
      color: 'var(--text-sub)',
      cursor: 'pointer'
    }
  }, React.createElement("i", {
    "data-lucide": "x"
  }))), error && React.createElement("div", {
    style: {
      background: 'rgba(239,68,68,0.15)',
      color: '#f87171',
      border: '1px solid rgba(239,68,68,0.3)',
      padding: '10px',
      borderRadius: '8px',
      marginBottom: '14px',
      fontSize: '13px'
    }
  }, error), React.createElement("div", {
    style: {
      background: 'rgba(59, 130, 246, 0.1)',
      border: '1px solid rgba(59, 130, 246, 0.2)',
      padding: '12px',
      borderRadius: '10px',
      marginBottom: '16px',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'space-between'
    }
  }, React.createElement("span", {
    style: {
      fontSize: '12.5px',
      color: '#60a5fa'
    }
  }, "\xD6rnek Excel format\u0131 indirmek i\xE7in:"), React.createElement("button", {
    type: "button",
    className: "btn-action-secondary",
    style: {
      padding: '4px 10px',
      fontSize: '12px'
    },
    onClick: () => window.SubscriberService.downloadTemplate()
  }, React.createElement("i", {
    "data-lucide": "download",
    style: {
      width: '14px',
      height: '14px'
    }
  }), "Excel \u015Eablonunu \u0130ndir")), React.createElement("form", {
    onSubmit: handleSubmit
  }, React.createElement("div", {
    className: "form-group"
  }, React.createElement("label", {
    className: "form-label"
  }, "Excel (.xlsx) / CSV Dosyas\u0131 Se\xE7"), React.createElement("input", {
    type: "file",
    accept: ".xlsx, .xls, .csv",
    className: "modal-input",
    onChange: e => setFile(e.target.files[0]),
    required: true
  })), React.createElement("div", {
    style: {
      display: 'flex',
      justifyContent: 'flex-end',
      gap: '10px',
      marginTop: '24px'
    }
  }, React.createElement("button", {
    type: "button",
    className: "btn-action-secondary",
    onClick: onClose
  }, "Kapat"), React.createElement("button", {
    type: "submit",
    className: "btn-action-primary",
    disabled: loading || !file
  }, loading ? 'Yükleniyor...' : 'Yükle ve İçe Aktar')))));
};
window.ConfirmModal = function ConfirmModal({
  isOpen,
  onClose,
  onConfirm,
  title = 'Silme Onayı',
  message = 'Bu kaydı silmek istediğinize emin misiniz?',
  confirmText = 'Evet, Sil',
  cancelText = 'İptal',
  danger = true
}) {
  if (!isOpen) return null;
  React.useEffect(() => {
    if (window.lucide) window.lucide.createIcons();
  }, [isOpen]);
  return React.createElement("div", {
    className: "modal-overlay"
  }, React.createElement("div", {
    className: "modal-card",
    style: {
      maxWidth: '420px',
      textAlign: 'center',
      padding: '28px'
    }
  }, React.createElement("div", {
    style: {
      width: '56px',
      height: '56px',
      borderRadius: '50%',
      background: danger ? 'rgba(239, 68, 68, 0.15)' : 'rgba(59, 130, 246, 0.15)',
      border: `1px solid ${danger ? 'rgba(239, 68, 68, 0.3)' : 'rgba(59, 130, 246, 0.3)'}`,
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      margin: '0 auto 16px auto',
      color: danger ? '#f87171' : '#60a5fa'
    }
  }, React.createElement("i", {
    "data-lucide": danger ? "alert-triangle" : "help-circle",
    style: {
      width: '28px',
      height: '28px'
    }
  })), React.createElement("h3", {
    style: {
      color: 'white',
      fontSize: '18px',
      fontWeight: '700',
      marginBottom: '8px'
    }
  }, title), React.createElement("p", {
    style: {
      color: 'var(--text-sub)',
      fontSize: '14px',
      lineHeight: '1.5',
      marginBottom: '24px'
    }
  }, message), React.createElement("div", {
    style: {
      display: 'flex',
      gap: '12px',
      justifyContent: 'center'
    }
  }, React.createElement("button", {
    type: "button",
    className: "btn-action-secondary",
    onClick: onClose,
    style: {
      flex: 1,
      padding: '10px 16px',
      justifyContent: 'center'
    }
  }, cancelText), React.createElement("button", {
    type: "button",
    onClick: onConfirm,
    style: {
      flex: 1,
      padding: '10px 16px',
      borderRadius: '10px',
      fontWeight: '600',
      fontSize: '13.5px',
      border: 'none',
      cursor: 'pointer',
      background: danger ? 'linear-gradient(135deg, #ef4444 0%, #dc2626 100%)' : 'var(--accent-gradient)',
      color: 'white',
      boxShadow: danger ? '0 4px 14px rgba(239, 68, 68, 0.35)' : 'var(--accent-shadow)',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      gap: '6px'
    }
  }, confirmText))));
};
