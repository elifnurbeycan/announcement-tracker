const { useState, useEffect } = React;

function UserLoginApp() {
    const [error, setError] = useState('');
    const [mode, setMode] = useState(null);

    useEffect(() => {
        fetch('/api/v1/auth/mode')
            .then(response => {
                if (!response.ok) throw new Error('Giriş yapılandırması alınamadı.');
                return response.json();
            })
            .then(response => setMode(response && response.data ? response.data : null))
            .catch(() => setError('Giriş servisine ulaşılamıyor. Lütfen daha sonra tekrar deneyin.'));

        const params = new URLSearchParams(window.location.search);
        if (params.get('ssoError')) {
            setError('SSO kimlik doğrulaması başarısız oldu veya bu uygulamaya giriş yetkiniz bulunmuyor.');
        }
        const sessionMessage = sessionStorage.getItem('sessionExpiredMessage');
        if (sessionMessage) {
            setError(sessionMessage);
            sessionStorage.removeItem('sessionExpiredMessage');
        }
        setTimeout(() => { if (window.lucide) window.lucide.createIcons(); }, 100);
    }, []);

    return (
        <div className="login-card">
            <div className="login-header">
                <div className="login-icon-box" style={{ background: 'linear-gradient(135deg, #06b6d4, #3b82f6)' }}>
                    <i data-lucide="user-check" style={{ width: '32px', height: '32px', color: 'white' }}></i>
                </div>
                <h2>Kullanıcı Girişi</h2>
                <p>Duyuru Takip Portalına Hoş Geldiniz</p>
            </div>

            {error && <div className="login-error">{error}</div>}

            {mode === null && !error && (
                <p style={{ fontSize: '13px', color: 'var(--text-sub)', textAlign: 'center' }}>Giriş servisi hazırlanıyor...</p>
            )}

            {mode && mode.ssoEnabled && (
                <a
                    href={mode.ssoLoginUrl || '/oauth2/authorization/keycloak'}
                    className="btn-submit"
                    style={{
                        display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '10px',
                        background: 'linear-gradient(135deg, #06b6d4, #3b82f6)', textDecoration: 'none'
                    }}
                >
                    <i data-lucide="key-round" style={{ width: '20px', height: '20px' }}></i>
                    Kurumsal SSO ile Giriş Yap
                </a>
            )}

            {mode && !mode.ssoEnabled && (
                <div className="login-error">Kurumsal SSO şu anda kullanıma hazır değil. Sistem yöneticinize başvurun.</div>
            )}

            <div style={{ marginTop: '24px', paddingTop: '20px', borderTop: '1px solid rgba(255,255,255,0.08)', textAlign: 'center' }}>
                <p style={{ fontSize: '13px', color: 'var(--text-sub)', marginBottom: '12px' }}>Yönetici Paneline Gitmek İster misiniz?</p>
                <a
                    href="/admin-login.html"
                    style={{
                        display: 'inline-flex', alignItems: 'center', justifyContent: 'center', gap: '8px', width: '100%',
                        background: 'rgba(139, 92, 246, 0.1)', border: '1px solid rgba(139, 92, 246, 0.3)',
                        color: '#c084fc', padding: '12px', borderRadius: '10px', fontSize: '13px',
                        fontWeight: '600', textDecoration: 'none', transition: 'all 0.2s'
                    }}
                >
                    <i data-lucide="shield-check" style={{ width: '16px', height: '16px' }}></i>
                    Yönetici Giriş Paneli &rarr;
                </a>
            </div>
        </div>
    );
}

ReactDOM.render(<UserLoginApp />, document.getElementById('root'));
