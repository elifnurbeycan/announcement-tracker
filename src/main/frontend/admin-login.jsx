const { useState, useEffect } = React;

        function AdminLoginApp() {
            const [error, setError] = useState('');
            const [mode, setMode] = useState({ ssoEnabled: true, ssoLoginUrl: '/oauth2/authorization/keycloak' });

            useEffect(() => {
                fetch('/api/v1/auth/mode')
                    .then(r => r.json())
                    .then(res => { if (res && res.data) setMode(res.data); })
                    .catch(err => console.debug('Auth mode fetch fallback:', err));

                const params = new URLSearchParams(window.location.search);
                if (params.get('ssoError')) {
                    setError('SSO kimlik doğrulaması başarısız oldu veya yönetici yetkiniz bulunmuyor.');
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
                        <div className="login-icon-box">
                            <i data-lucide="shield-check" style={{ width: '32px', height: '32px', color: 'white' }}></i>
                        </div>
                        <h2>Yönetici Girişi</h2>
                        <p>Duyuru Takip Yönetim Paneli</p>
                    </div>

                    {error && <div className="login-error">{error}</div>}

                    {mode.ssoEnabled && (
                        <div style={{ marginBottom: '20px' }}>
                            <a
                                href={mode.ssoLoginUrl || '/oauth2/authorization/keycloak'}
                                style={{
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
                                }}
                            >
                                <i data-lucide="key-round" style={{ width: '20px', height: '20px' }}></i>
                                Kurumsal SSO (Keycloak Admin) ile Giriş Yap
                            </a>
                        </div>
                    )}

                    {!mode.ssoEnabled && (
                        <p style={{ fontSize: '13px', color: '#f87171', textAlign: 'center' }}>
                            Kurumsal SSO şu anda kullanıma hazır değil. Sistem yöneticinize başvurun.
                        </p>
                    )}

                    <div style={{ marginTop: '24px', paddingTop: '20px', borderTop: '1px solid rgba(255,255,255,0.08)', textAlign: 'center' }}>
                        <p style={{ fontSize: '13px', color: 'var(--text-sub)', marginBottom: '12px' }}>Kullanıcı Girişi Yapmak İster misiniz?</p>
                        <a 
                            href="/user-login.html" 
                            style={{
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
                            }}
                        >
                            <i data-lucide="user" style={{ width: '16px', height: '16px' }}></i>
                            Kullanıcı Paneline Git &rarr;
                        </a>
                    </div>
                </div>
            );
        }

        ReactDOM.render(<AdminLoginApp />, document.getElementById('root'));
