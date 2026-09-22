window.SettingsPage = function SettingsPage({
  scrapePeriod = 30,
  onSavePeriod
}) {
  const isPreset = val => [30, 60, 120].includes(val);
  const [selectedOption, setSelectedOption] = React.useState(isPreset(scrapePeriod) ? String(scrapePeriod) : 'custom');
  const [customMinutes, setCustomMinutes] = React.useState(scrapePeriod || 30);
  const [loading, setLoading] = React.useState(false);
  React.useEffect(() => {
    if (isPreset(scrapePeriod)) {
      setSelectedOption(String(scrapePeriod));
    } else {
      setSelectedOption('custom');
    }
    setCustomMinutes(scrapePeriod || 30);
  }, [scrapePeriod]);
  React.useEffect(() => {
    if (window.lucide) window.lucide.createIcons();
  }, [selectedOption]);
  const handleSelectChange = async e => {
    const val = e.target.value;
    setSelectedOption(val);
    if (val !== 'custom') {
      const minutes = parseInt(val, 10);
      setCustomMinutes(minutes);
      setLoading(true);
      try {
        await onSavePeriod(minutes);
      } finally {
        setLoading(false);
      }
    }
  };
  const handleSubmit = async e => {
    e.preventDefault();
    setLoading(true);
    const minutes = selectedOption === 'custom' ? parseInt(customMinutes, 10) : parseInt(selectedOption, 10);
    try {
      await onSavePeriod(minutes);
    } finally {
      setLoading(false);
    }
  };
  return React.createElement("div", {
    style: {
      maxWidth: '680px'
    }
  }, React.createElement("div", {
    className: "card"
  }, React.createElement("div", {
    className: "card-header-flex",
    style: {
      marginBottom: '16px'
    }
  }, React.createElement("h2", null, React.createElement("i", {
    "data-lucide": "clock",
    style: {
      color: '#60a5fa',
      width: '20px',
      height: '20px'
    }
  }), "Otomatik Tarama Zamanlamas\u0131")), React.createElement("p", {
    style: {
      fontSize: '13.5px',
      color: 'var(--text-sub)',
      marginBottom: '24px',
      lineHeight: '1.6'
    }
  }, "Sistemin resmi duyuru kaynaklar\u0131n\u0131 (G\u0130B vb.) otomatik olarak ka\xE7 dakikada bir tarayaca\u011F\u0131n\u0131 ayarlay\u0131n. Yeni bir duyuru tespit edildi\u011Finde ilgili abonelere e-posta bildirimi g\xF6nderilir."), React.createElement("form", {
    onSubmit: handleSubmit
  }, React.createElement("div", {
    className: "form-group"
  }, React.createElement("label", {
    className: "form-label"
  }, "Tarama Periyodu Se\xE7in"), React.createElement("div", {
    style: {
      display: 'flex',
      flexDirection: 'column',
      gap: '14px',
      marginTop: '8px'
    }
  }, React.createElement("select", {
    className: "input-field",
    style: {
      width: '100%',
      maxWidth: '320px',
      fontSize: '14px',
      fontWeight: '600'
    },
    value: selectedOption,
    onChange: handleSelectChange
  }, React.createElement("option", {
    value: "30"
  }, "30 Dakika"), React.createElement("option", {
    value: "60"
  }, "1 Saat (60 Dakika)"), React.createElement("option", {
    value: "120"
  }, "2 Saat (120 Dakika)"), React.createElement("option", {
    value: "custom"
  }, "\xD6zel S\xFCre Gir...")), selectedOption === 'custom' && React.createElement("div", {
    style: {
      display: 'flex',
      alignItems: 'center',
      gap: '12px'
    }
  }, React.createElement("input", {
    type: "number",
    min: "5",
    max: "1440",
    className: "input-field",
    style: {
      width: '140px',
      fontSize: '15px',
      fontWeight: '700',
      textAlign: 'center'
    },
    placeholder: "Dakika",
    value: customMinutes,
    onChange: e => setCustomMinutes(e.target.value),
    required: true
  }), React.createElement("span", {
    style: {
      fontSize: '14px',
      color: 'var(--text-sub)',
      fontWeight: '600'
    }
  }, "Dakika"))), React.createElement("p", {
    style: {
      fontSize: '12px',
      color: 'var(--text-sub)',
      marginTop: '10px'
    }
  }, "* \xD6nerilen varsay\u0131lan de\u011Fer 30 dakikad\u0131r. Minimum 5 dakika ayarlanabilir.")), React.createElement("div", {
    style: {
      borderTop: '1px solid var(--border-color)',
      paddingTop: '20px',
      marginTop: '24px',
      display: 'flex',
      justifyContent: 'flex-end'
    }
  }, React.createElement("button", {
    type: "submit",
    className: "btn-action-primary",
    disabled: loading
  }, React.createElement("i", {
    "data-lucide": "save",
    style: {
      width: '16px',
      height: '16px'
    }
  }), loading ? 'Kaydediliyor...' : 'Ayarları Kaydet')))));
};
