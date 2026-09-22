window.ProfilePage = function ProfilePage({
  user = {},
  role = 'ROLE_ADMIN',
  availableSites = [],
  onUpdatePreferences = null
}) {
  React.useEffect(() => {
    if (window.lucide) window.lucide.createIcons();
  }, []);
  const isAdmin = role === 'ROLE_ADMIN';
  const name = user.fullName || user.username || user.email || (isAdmin ? 'Süper Admin' : 'Kullanıcı');
  const email = user.email || (user.username ? `${user.username}@kurum.com` : 'admin@kurum.com');
  const roleText = isAdmin ? 'Süper Admin (Yönetici)' : 'Normal Çalışan (Abone)';
  const normalizedSites = availableSites.map(site => typeof site === 'string' ? site : site.name);
  const isGeneralEmployee = user.generalEmployee ?? (user.departments || []).length === 0;
  const departmentSites = new Set(isGeneralEmployee ? normalizedSites : user.departmentSites || (user.departments || []).flatMap(department => department.sites || []));
  const [selectedAdditionalSites, setSelectedAdditionalSites] = React.useState(new Set());
  const [saving, setSaving] = React.useState(false);
  const [saveMessage, setSaveMessage] = React.useState('');
  React.useEffect(() => {
    const personalSites = user.subscribedSites || [];
    setSelectedAdditionalSites(new Set(personalSites.filter(site => !departmentSites.has(site))));
  }, [user, availableSites]);
  let effectiveSources = [];
  if (isAdmin) {
    effectiveSources = normalizedSites.length > 0 ? normalizedSites : ['EBELGE_GIB', 'KOSGEB'];
  } else {
    effectiveSources = user.effectiveSites || Array.from(new Set([...departmentSites, ...(user.subscribedSites || [])]));
  }
  const toggleAdditionalSite = site => {
    if (departmentSites.has(site)) return;
    setSelectedAdditionalSites(current => {
      const next = new Set(current);
      if (next.has(site)) next.delete(site);else next.add(site);
      return next;
    });
    setSaveMessage('');
  };
  const savePreferences = async () => {
    if (!onUpdatePreferences) return;
    setSaving(true);
    setSaveMessage('');
    try {
      await onUpdatePreferences(Array.from(selectedAdditionalSites));
      setSaveMessage('Ek kaynak tercihleriniz kaydedildi.');
    } catch (error) {
      setSaveMessage(error.message || 'Tercihler kaydedilemedi.');
    } finally {
      setSaving(false);
    }
  };
  return React.createElement("div", {
    style: {
      maxWidth: '780px'
    }
  }, React.createElement("div", {
    className: "card"
  }, React.createElement("div", {
    style: {
      display: 'flex',
      alignItems: 'center',
      gap: '20px',
      marginBottom: '24px',
      paddingBottom: '24px',
      borderBottom: '1px solid var(--border-color)'
    }
  }, React.createElement("div", {
    style: {
      width: '64px',
      height: '64px',
      borderRadius: '18px',
      background: 'linear-gradient(135deg, #3b82f6, #8b5cf6)',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      fontSize: '24px',
      fontWeight: '800',
      color: 'white'
    }
  }, name.charAt(0).toUpperCase()), React.createElement("div", null, React.createElement("h2", {
    className: "card-title",
    style: {
      fontSize: '20px',
      fontWeight: '800',
      color: 'white'
    }
  }, "Kullan\u0131c\u0131 Profili (", name, ")"), React.createElement("span", {
    style: {
      fontSize: '13px',
      color: 'var(--text-sub)'
    }
  }, email), React.createElement("div", {
    style: {
      marginTop: '6px'
    }
  }, React.createElement("span", {
    className: "site-badge",
    style: {
      background: isAdmin ? 'rgba(139, 92, 246, 0.15)' : 'rgba(59, 130, 246, 0.15)',
      color: isAdmin ? '#c084fc' : '#60a5fa'
    }
  }, roleText)))), React.createElement("h3", {
    style: {
      fontSize: '15px',
      fontWeight: '700',
      color: 'white',
      marginBottom: '16px',
      display: 'flex',
      alignItems: 'center',
      gap: '8px'
    }
  }, React.createElement("i", {
    "data-lucide": "shield",
    style: {
      width: '18px',
      height: '18px',
      color: '#60a5fa'
    }
  }), "Hesap ve Departman Bilgileri"), React.createElement("div", {
    style: {
      display: 'grid',
      gridTemplateColumns: '1fr 1fr',
      gap: '16px',
      marginBottom: '24px'
    }
  }, React.createElement("div", {
    style: {
      background: 'rgba(15, 23, 42, 0.6)',
      border: '1px solid var(--border-color)',
      padding: '14px',
      borderRadius: '12px'
    }
  }, React.createElement("span", {
    style: {
      fontSize: '12px',
      color: 'var(--text-sub)',
      display: 'block',
      marginBottom: '4px'
    }
  }, "Ad Soyad / Unvan"), React.createElement("span", {
    style: {
      fontSize: '14px',
      fontWeight: '600',
      color: 'white'
    }
  }, name)), React.createElement("div", {
    style: {
      background: 'rgba(15, 23, 42, 0.6)',
      border: '1px solid var(--border-color)',
      padding: '14px',
      borderRadius: '12px'
    }
  }, React.createElement("span", {
    style: {
      fontSize: '12px',
      color: 'var(--text-sub)',
      display: 'block',
      marginBottom: '4px'
    }
  }, "E-posta Adresi"), React.createElement("span", {
    style: {
      fontSize: '14px',
      fontWeight: '600',
      color: 'white'
    }
  }, email)), React.createElement("div", {
    style: {
      background: 'rgba(15, 23, 42, 0.6)',
      border: '1px solid var(--border-color)',
      padding: '14px',
      borderRadius: '12px',
      gridColumn: '1 / -1'
    }
  }, React.createElement("span", {
    style: {
      fontSize: '12px',
      color: 'var(--text-sub)',
      display: 'block',
      marginBottom: '6px'
    }
  }, "Ba\u011Fl\u0131 Departmanlar"), isAdmin ? React.createElement("span", {
    style: {
      fontSize: '13.5px',
      fontWeight: '700',
      color: '#60a5fa'
    }
  }, "T\xFCm Departmanlar ve Y\xF6netim Paneli") : user.departments && user.departments.length > 0 ? React.createElement("div", {
    style: {
      display: 'flex',
      flexWrap: 'wrap',
      gap: '6px'
    }
  }, user.departments.map(d => React.createElement("span", {
    key: d.id,
    style: {
      background: 'rgba(59, 130, 246, 0.15)',
      color: '#60a5fa',
      border: '1px solid rgba(59, 130, 246, 0.3)',
      padding: '4px 10px',
      borderRadius: '8px',
      fontSize: '12px',
      fontWeight: '600'
    }
  }, d.name))) : React.createElement("span", {
    style: {
      background: 'rgba(139, 92, 246, 0.15)',
      color: '#c084fc',
      border: '1px solid rgba(139, 92, 246, 0.3)',
      padding: '4px 10px',
      borderRadius: '8px',
      fontSize: '12px',
      fontWeight: '700'
    }
  }, "Genel \xC7al\u0131\u015Fan (T\xFCm kaynaklardan bildirim al\u0131r)"))), React.createElement("h3", {
    style: {
      fontSize: '15px',
      fontWeight: '700',
      color: 'white',
      marginBottom: '16px',
      display: 'flex',
      alignItems: 'center',
      gap: '8px'
    }
  }, React.createElement("i", {
    "data-lucide": "rss",
    style: {
      width: '18px',
      height: '18px',
      color: '#34d399'
    }
  }), "Efektif Duyuru Takip Kapsam\u0131"), React.createElement("p", {
    style: {
      fontSize: '13px',
      color: 'var(--text-sub)',
      marginBottom: '12px'
    }
  }, "Departman ve ki\u015Fisel tercihlerinizin birle\u015Fimi sonucu bildirim alaca\u011F\u0131n\u0131z aktif kaynak siteleri:"), React.createElement("div", {
    style: {
      display: 'flex',
      flexWrap: 'wrap',
      gap: '8px'
    }
  }, effectiveSources.map(site => React.createElement("span", {
    key: site,
    className: "site-badge",
    style: {
      padding: '6px 12px',
      fontSize: '13px'
    }
  }, site))), !isAdmin && React.createElement("div", {
    style: {
      marginTop: '28px',
      paddingTop: '24px',
      borderTop: '1px solid var(--border-color)'
    }
  }, React.createElement("h3", {
    style: {
      fontSize: '15px',
      fontWeight: '700',
      color: 'white',
      marginBottom: '8px'
    }
  }, "Ek Duyuru Kaynaklar\u0131"), React.createElement("p", {
    style: {
      fontSize: '13px',
      color: 'var(--text-sub)',
      marginBottom: '16px',
      lineHeight: '1.6'
    }
  }, isGeneralEmployee ? 'Genel çalışan olduğunuz için tüm kaynaklar zorunlu olarak aktiftir.' : 'Departmanınızın kaynakları zorunludur ve kapatılamaz. Bunlara ek olarak diğer kaynakları seçebilirsiniz.'), React.createElement("div", {
    className: "preference-options"
  }, normalizedSites.map(site => {
    const mandatory = departmentSites.has(site);
    const checked = mandatory || selectedAdditionalSites.has(site);
    return React.createElement("label", {
      key: site,
      className: `preference-option ${mandatory ? 'is-mandatory' : ''} ${checked ? 'is-selected' : ''}`
    }, React.createElement("span", {
      className: "preference-option-main"
    }, React.createElement("input", {
      className: "preference-native-checkbox",
      type: "checkbox",
      checked: checked,
      disabled: mandatory,
      onChange: () => toggleAdditionalSite(site)
    }), React.createElement("span", {
      className: "preference-checkbox",
      "aria-hidden": "true"
    }, checked && React.createElement("span", {
      className: "preference-check-mark"
    }, "\u2713")), React.createElement("span", {
      className: "preference-option-copy"
    }, React.createElement("strong", null, site), React.createElement("small", null, mandatory ? 'Departman kapsamınızdan gelir' : 'Kişisel ek bildirim tercihi'))), mandatory && React.createElement("span", {
      className: "preference-required-badge"
    }, React.createElement("span", {
      className: "preference-lock-mark",
      "aria-hidden": "true"
    }, "\u25CF"), isGeneralEmployee ? 'GENEL ÇALIŞAN' : 'DEPARTMAN ZORUNLULUĞU'));
  })), !isGeneralEmployee && React.createElement("div", {
    className: "preference-actions"
  }, React.createElement("span", {
    className: "preference-actions-hint"
  }, "De\u011Fi\u015Fiklikler yaln\u0131zca ek kaynaklar\u0131n\u0131z\u0131 etkiler."), React.createElement("button", {
    className: "btn-action-primary preference-save-button",
    onClick: savePreferences,
    disabled: saving
  }, React.createElement("span", {
    className: `preference-button-icon ${saving ? 'is-spinning' : ''}`,
    "aria-hidden": "true"
  }, saving ? '↻' : '✓'), saving ? 'Tercihler Kaydediliyor...' : 'Ek Tercihleri Kaydet')), saveMessage && React.createElement("p", {
    className: `preference-feedback ${saveMessage.includes('kaydedildi') ? 'is-success' : 'is-error'}`
  }, React.createElement("span", {
    className: "preference-feedback-icon",
    "aria-hidden": "true"
  }, saveMessage.includes('kaydedildi') ? '✓' : '!'), saveMessage))));
};
