const {
  useState,
  useEffect
} = React;
function UserLoginApp() {
  const [error, setError] = useState('');
  const [mode, setMode] = useState(null);
  useEffect(() => {
    fetch('/api/v1/auth/mode').then(response => {
      if (!response.ok) throw new Error('Giriş yapılandırması alınamadı.');
      return response.json();
    }).then(response => setMode(response && response.data ? response.data : null)).catch(() => setError('Giriş servisine ulaşılamıyor. Lütfen daha sonra tekrar deneyin.'));
    const params = new URLSearchParams(window.location.search);
    if (params.get('ssoError')) {
      setError('SSO kimlik doğrulaması başarısız oldu veya bu uygulamaya giriş yetkiniz bulunmuyor.');
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
    className: "login-icon-box",
    style: {
      background: 'linear-gradient(135deg, #06b6d4, #3b82f6)'
    }
  }, React.createElement("i", {
    "data-lucide": "user-check",
    style: {
      width: '32px',
      height: '32px',
      color: 'white'
    }
  })), React.createElement("h2", null, "Kullan\u0131c\u0131 Giri\u015Fi"), React.createElement("p", null, "Duyuru Takip Portal\u0131na Ho\u015F Geldiniz")), error && React.createElement("div", {
    className: "login-error"
  }, error), mode === null && !error && React.createElement("p", {
    style: {
      fontSize: '13px',
      color: 'var(--text-sub)',
      textAlign: 'center'
    }
  }, "Giri\u015F servisi haz\u0131rlan\u0131yor..."), mode && mode.ssoEnabled && React.createElement("a", {
    href: mode.ssoLoginUrl || '/oauth2/authorization/keycloak',
    className: "btn-submit",
    style: {
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      gap: '10px',
      background: 'linear-gradient(135deg, #06b6d4, #3b82f6)',
      textDecoration: 'none'
    }
  }, React.createElement("i", {
    "data-lucide": "key-round",
    style: {
      width: '20px',
      height: '20px'
    }
  }), "Kurumsal SSO ile Giri\u015F Yap"), mode && !mode.ssoEnabled && React.createElement("div", {
    className: "login-error"
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
  }, "Y\xF6netici Paneline Gitmek \u0130ster misiniz?"), React.createElement("a", {
    href: "/admin-login.html",
    style: {
      display: 'inline-flex',
      alignItems: 'center',
      justifyContent: 'center',
      gap: '8px',
      width: '100%',
      background: 'rgba(139, 92, 246, 0.1)',
      border: '1px solid rgba(139, 92, 246, 0.3)',
      color: '#c084fc',
      padding: '12px',
      borderRadius: '10px',
      fontSize: '13px',
      fontWeight: '600',
      textDecoration: 'none',
      transition: 'all 0.2s'
    }
  }, React.createElement("i", {
    "data-lucide": "shield-check",
    style: {
      width: '16px',
      height: '16px'
    }
  }), "Y\xF6netici Giri\u015F Paneli \u2192")));
}
ReactDOM.render(React.createElement(UserLoginApp, null), document.getElementById('root'));
