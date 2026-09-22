window.SourcesPage = function SourcesPage({
  availableSites = [],
  announcements = []
}) {
  React.useEffect(() => {
    if (window.lucide) window.lucide.createIcons();
  }, [availableSites, announcements]);
  const siteInfoList = [{
    code: 'EBELGE_GIB',
    name: 'Gelir İdaresi Başkanlığı (e-Belge)',
    url: 'https://ebelge.gib.gov.tr/duyurular.html',
    description: 'GİB e-Fatura, e-Arşiv, e-İrsaliye ve e-Defter resmi portal duyuruları'
  }, {
    code: 'KOSGEB',
    name: 'KOSGEB Genel Duyurular',
    url: 'https://www.kosgeb.gov.tr/site/tr/genel/duyurular',
    description: 'KOSGEB destek, hibe ve genel kurumsal duyuruları'
  }];
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
  }, "Takip Edilen Duyuru Kaynaklar\u0131"), React.createElement("p", {
    style: {
      fontSize: '13px',
      color: 'var(--text-sub)',
      marginTop: '4px'
    }
  }, "Sistemin periyodik olarak tarad\u0131\u011F\u0131 resmi web siteleri ve servis durumlar\u0131."))), React.createElement("div", {
    style: {
      display: 'grid',
      gridTemplateColumns: 'repeat(auto-fill, minmax(360px, 1fr))',
      gap: '20px'
    }
  }, siteInfoList.map(site => {
    const count = announcements.filter(a => a.sourceSite === site.code).length;
    return React.createElement("div", {
      key: site.code,
      className: "card"
    }, React.createElement("div", {
      style: {
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'flex-start',
        marginBottom: '14px'
      }
    }, React.createElement("div", {
      style: {
        display: 'flex',
        alignItems: 'center',
        gap: '12px'
      }
    }, React.createElement("div", {
      style: {
        width: '42px',
        height: '42px',
        borderRadius: '12px',
        background: 'rgba(6, 182, 212, 0.15)',
        color: '#22d3ee',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center'
      }
    }, React.createElement("i", {
      "data-lucide": "globe",
      style: {
        width: '22px',
        height: '22px'
      }
    })), React.createElement("div", null, React.createElement("h3", {
      style: {
        fontSize: '16px',
        fontWeight: '700',
        color: 'white'
      }
    }, site.name), React.createElement("span", {
      className: "site-badge",
      style: {
        marginTop: '4px',
        display: 'inline-block'
      }
    }, site.code))), React.createElement("span", {
      style: {
        background: 'rgba(16, 185, 129, 0.15)',
        color: '#34d399',
        border: '1px solid rgba(16, 185, 129, 0.3)',
        padding: '4px 10px',
        borderRadius: '8px',
        fontSize: '12px',
        fontWeight: '700'
      }
    }, "Aktif")), React.createElement("p", {
      style: {
        fontSize: '13px',
        color: 'var(--text-sub)',
        lineHeight: '1.5',
        marginBottom: '16px'
      }
    }, site.description), React.createElement("div", {
      style: {
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
        borderTop: '1px solid var(--border-color)',
        paddingTop: '14px'
      }
    }, React.createElement("span", {
      style: {
        fontSize: '13px',
        color: 'var(--text-sub)'
      }
    }, "Toplam Duyuru: ", React.createElement("strong", {
      style: {
        color: 'white'
      }
    }, count)), React.createElement("a", {
      href: site.url,
      target: "_blank",
      rel: "noopener noreferrer",
      className: "btn-link btn-source"
    }, React.createElement("i", {
      "data-lucide": "external-link",
      style: {
        width: '14px',
        height: '14px'
      }
    }), "Siteye Git")));
  })));
};
