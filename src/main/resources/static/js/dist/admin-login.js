const {
  useState,
  useEffect
} = React;
function AdminLoginApp() {
  const [error, setError] = useState('');
  const [mode, setMode] = useState({
    ssoEnabled: true,
    ssoLoginUrl: '/oauth2/authorization/keycloak'
  });
  useEffect(() => {
    fetch('/api/v1/auth/mode').then(r => r.json()).then(res => {
      if (res && res.data) setMode(res.data);
    }).catch(err => console.debug('Auth mode fetch fallback:', err));
    const params = new URLSearchParams(window.location.search);
    if (params.get('ssoError')) {
      setError('SSO kimlik doğrulaması başarısız oldu veya yönetici yetkiniz bulunmuyor.');
    }
    const sessionMessage = sessionStorage.getItem('sessionExpiredMessage');
    if (sessionMessage) {
      setError(sessionMessage);
      sessionStorage.removeItem('sessionExpiredMessage');
    }
    setTimeout(() => {
      if (window.lucide) window.lucide.createIcons();
    }, 100);
  }, []);
  return React.createElement("div", {
    className: "login-card"
  }, React.createElement("div", {
    className: "login-header"
  }, React.createElement("div", {
    className: "login-icon-box"
  }, React.createElement("i", {
    "data-lucide": "shield-check",
    style: {
      width: '32px',
      height: '32px',
      color: 'white'
    }
  })), React.createElement("h2", null, "Y\xF6netici Giri\u015Fi"), React.createElement("p", null, "Duyuru Takip Y\xF6netim Paneli")), error && React.createElement("div", {
    className: "login-error"
  }, error), mode.ssoEnabled && React.createElement("div", {
    style: {
      marginBottom: '20px'
    }
  }, React.createElement("a", {
    href: mode.ssoLoginUrl || '/oauth2/authorization/keycloak',
    style: {
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      gap: '10px',
      width: '100%',
      background: 'linear-gradient(135deg, #8b5cf6, #6366f1)',
      color: '#ffffff',
      padding: '14px',
      borderRadius: '10px',
      fontWeight: '700',
      fontSize: '14px',
      textDecoration: 'none',
      boxShadow: '0 4px 12px rgba(139, 92, 246, 0.3)'
    }
  }, React.createElement("i", {
    "data-lucide": "key-round",
    style: {
      width: '20px',
      height: '20px'
    }
  }), "Kurumsal SSO (Keycloak Admin) ile Giri\u015F Yap")), !mode.ssoEnabled && React.createElement("p", {
    style: {
      fontSize: '13px',
      color: '#f87171',
      textAlign: 'center'
    }
  }, "Kurumsal SSO \u015Fu anda kullan\u0131ma haz\u0131r de\u011Fil. Sistem y\xF6neticinize ba\u015Fvurun."), React.createElement("div", {
    style: {
      marginTop: '24px',
      paddingTop: '20px',
      borderTop: '1px solid rgba(255,255,255,0.08)',
      textAlign: 'center'
    }
  }, React.createElement("p", {
    style: {
      fontSize: '13px',
      color: 'var(--text-sub)',
      marginBottom: '12px'
    }
  }, "Kullan\u0131c\u0131 Giri\u015Fi Yapmak \u0130ster misiniz?"), React.createElement("a", {
    href: "/user-login.html",
    style: {
      display: 'inline-flex',
      alignItems: 'center',
      justifyContent: 'center',
      gap: '8px',
      width: '100%',
      background: 'rgba(59, 130, 246, 0.1)',
      border: '1px solid rgba(59, 130, 246, 0.3)',
      color: '#60a5fa',
      padding: '12px',
      borderRadius: '10px',
      fontSize: '13px',
      fontWeight: '600',
      textDecoration: 'none',
      transition: 'all 0.2s'
    }
  }, React.createElement("i", {
    "data-lucide": "user",
    style: {
      width: '16px',
      height: '16px'
    }
  }), "Kullan\u0131c\u0131 Paneline Git \u2192")));
}
ReactDOM.render(React.createElement(AdminLoginApp, null), document.getElementById('root'));
