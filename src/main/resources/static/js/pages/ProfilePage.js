/**
 * Profile Page Component
 */
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
    const isGeneralEmployee = user.generalEmployee ?? ((user.departments || []).length === 0);
    const departmentSites = new Set(
        isGeneralEmployee
            ? normalizedSites
            : (user.departmentSites || (user.departments || []).flatMap(department => department.sites || []))
    );
    const [selectedAdditionalSites, setSelectedAdditionalSites] = React.useState(new Set());
    const [saving, setSaving] = React.useState(false);
    const [saveMessage, setSaveMessage] = React.useState('');

    React.useEffect(() => {
        const personalSites = user.subscribedSites || [];
        setSelectedAdditionalSites(new Set(personalSites.filter(site => !departmentSites.has(site))));
    }, [user, availableSites]);

    // Compute Effective Sources
    let effectiveSources = [];
    if (isAdmin) {
        effectiveSources = normalizedSites.length > 0 ? normalizedSites : ['EBELGE_GIB', 'KOSGEB'];
    } else {
        effectiveSources = user.effectiveSites || Array.from(new Set([
            ...departmentSites,
            ...(user.subscribedSites || [])
        ]));
    }

    const toggleAdditionalSite = (site) => {
        if (departmentSites.has(site)) return;
        setSelectedAdditionalSites(current => {
            const next = new Set(current);
            if (next.has(site)) next.delete(site);
            else next.add(site);
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

    return (
        <div style={{ maxWidth: '780px' }}>
            <div className="card">
                <div style={{ display: 'flex', alignItems: 'center', gap: '20px', marginBottom: '24px', paddingBottom: '24px', borderBottom: '1px solid var(--border-color)' }}>
                    <div style={{ width: '64px', height: '64px', borderRadius: '18px', background: 'linear-gradient(135deg, #3b82f6, #8b5cf6)', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: '24px', fontWeight: '800', color: 'white' }}>
                        {name.charAt(0).toUpperCase()}
                    </div>
                    <div>
                        <h2 className="card-title" style={{ fontSize: '20px', fontWeight: '800', color: 'white' }}>Kullanıcı Profili ({name})</h2>
                        <span style={{ fontSize: '13px', color: 'var(--text-sub)' }}>{email}</span>
                        <div style={{ marginTop: '6px' }}>
                            <span className="site-badge" style={{ background: isAdmin ? 'rgba(139, 92, 246, 0.15)' : 'rgba(59, 130, 246, 0.15)', color: isAdmin ? '#c084fc' : '#60a5fa' }}>
                                {roleText}
                            </span>
                        </div>
                    </div>
                </div>

                <h3 style={{ fontSize: '15px', fontWeight: '700', color: 'white', marginBottom: '16px', display: 'flex', alignItems: 'center', gap: '8px' }}>
                    <i data-lucide="shield" style={{ width: '18px', height: '18px', color: '#60a5fa' }}></i>
                    Hesap ve Departman Bilgileri
                </h3>

                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px', marginBottom: '24px' }}>
                    <div style={{ background: 'rgba(15, 23, 42, 0.6)', border: '1px solid var(--border-color)', padding: '14px', borderRadius: '12px' }}>
                        <span style={{ fontSize: '12px', color: 'var(--text-sub)', display: 'block', marginBottom: '4px' }}>Ad Soyad / Unvan</span>
                        <span style={{ fontSize: '14px', fontWeight: '600', color: 'white' }}>{name}</span>
                    </div>

                    <div style={{ background: 'rgba(15, 23, 42, 0.6)', border: '1px solid var(--border-color)', padding: '14px', borderRadius: '12px' }}>
                        <span style={{ fontSize: '12px', color: 'var(--text-sub)', display: 'block', marginBottom: '4px' }}>E-posta Adresi</span>
                        <span style={{ fontSize: '14px', fontWeight: '600', color: 'white' }}>{email}</span>
                    </div>

                    <div style={{ background: 'rgba(15, 23, 42, 0.6)', border: '1px solid var(--border-color)', padding: '14px', borderRadius: '12px', gridColumn: '1 / -1' }}>
                        <span style={{ fontSize: '12px', color: 'var(--text-sub)', display: 'block', marginBottom: '6px' }}>Bağlı Departmanlar</span>
                        {isAdmin ? (
                            <span style={{ fontSize: '13.5px', fontWeight: '700', color: '#60a5fa' }}>Tüm Departmanlar ve Yönetim Paneli</span>
                        ) : (
                            user.departments && user.departments.length > 0 ? (
                                <div style={{ display: 'flex', flexWrap: 'wrap', gap: '6px' }}>
                                    {user.departments.map(d => (
                                        <span key={d.id} style={{ background: 'rgba(59, 130, 246, 0.15)', color: '#60a5fa', border: '1px solid rgba(59, 130, 246, 0.3)', padding: '4px 10px', borderRadius: '8px', fontSize: '12px', fontWeight: '600' }}>
                                            {d.name}
                                        </span>
                                    ))}
                                </div>
                            ) : (
                                <span style={{ background: 'rgba(139, 92, 246, 0.15)', color: '#c084fc', border: '1px solid rgba(139, 92, 246, 0.3)', padding: '4px 10px', borderRadius: '8px', fontSize: '12px', fontWeight: '700' }}>
                                    Genel Çalışan (Tüm kaynaklardan bildirim alır)
                                </span>
                            )
                        )}
                    </div>
                </div>

                <h3 style={{ fontSize: '15px', fontWeight: '700', color: 'white', marginBottom: '16px', display: 'flex', alignItems: 'center', gap: '8px' }}>
                    <i data-lucide="rss" style={{ width: '18px', height: '18px', color: '#34d399' }}></i>
                    Efektif Duyuru Takip Kapsamı
                </h3>

                <p style={{ fontSize: '13px', color: 'var(--text-sub)', marginBottom: '12px' }}>
                    Departman ve kişisel tercihlerinizin birleşimi sonucu bildirim alacağınız aktif kaynak siteleri:
                </p>

                <div style={{ display: 'flex', flexWrap: 'wrap', gap: '8px' }}>
                    {effectiveSources.map(site => (
                        <span key={site} className="site-badge" style={{ padding: '6px 12px', fontSize: '13px' }}>
                            {site}
                        </span>
                    ))}
                </div>

                {!isAdmin && (
                    <div style={{ marginTop: '28px', paddingTop: '24px', borderTop: '1px solid var(--border-color)' }}>
                        <h3 style={{ fontSize: '15px', fontWeight: '700', color: 'white', marginBottom: '8px' }}>
                            Ek Duyuru Kaynakları
                        </h3>
                        <p style={{ fontSize: '13px', color: 'var(--text-sub)', marginBottom: '16px', lineHeight: '1.6' }}>
                            {isGeneralEmployee
                                ? 'Genel çalışan olduğunuz için tüm kaynaklar zorunlu olarak aktiftir.'
                                : 'Departmanınızın kaynakları zorunludur ve kapatılamaz. Bunlara ek olarak diğer kaynakları seçebilirsiniz.'}
                        </p>

                        <div className="preference-options">
                            {normalizedSites.map(site => {
                                const mandatory = departmentSites.has(site);
                                const checked = mandatory || selectedAdditionalSites.has(site);
                                return (
                                    <label
                                        key={site}
                                        className={`preference-option ${mandatory ? 'is-mandatory' : ''} ${checked ? 'is-selected' : ''}`}
                                    >
                                        <span className="preference-option-main">
                                            <input
                                                className="preference-native-checkbox"
                                                type="checkbox"
                                                checked={checked}
                                                disabled={mandatory}
                                                onChange={() => toggleAdditionalSite(site)}
                                            />
                                            <span className="preference-checkbox" aria-hidden="true">
                                                {checked && <span className="preference-check-mark">✓</span>}
                                            </span>
                                            <span className="preference-option-copy">
                                                <strong>{site}</strong>
                                                <small>{mandatory ? 'Departman kapsamınızdan gelir' : 'Kişisel ek bildirim tercihi'}</small>
                                            </span>
                                        </span>
                                        {mandatory && (
                                            <span className="preference-required-badge">
                                                <span className="preference-lock-mark" aria-hidden="true">●</span>
                                                {isGeneralEmployee ? 'GENEL ÇALIŞAN' : 'DEPARTMAN ZORUNLULUĞU'}
                                            </span>
                                        )}
                                    </label>
                                );
                            })}
                        </div>

                        {!isGeneralEmployee && (
                            <div className="preference-actions">
                                <span className="preference-actions-hint">Değişiklikler yalnızca ek kaynaklarınızı etkiler.</span>
                                <button className="btn-action-primary preference-save-button" onClick={savePreferences} disabled={saving}>
                                    <span className={`preference-button-icon ${saving ? 'is-spinning' : ''}`} aria-hidden="true">
                                        {saving ? '↻' : '✓'}
                                    </span>
                                    {saving ? 'Tercihler Kaydediliyor...' : 'Ek Tercihleri Kaydet'}
                                </button>
                            </div>
                        )}
                        {saveMessage && (
                            <p className={`preference-feedback ${saveMessage.includes('kaydedildi') ? 'is-success' : 'is-error'}`}>
                                <span className="preference-feedback-icon" aria-hidden="true">
                                    {saveMessage.includes('kaydedildi') ? '✓' : '!'}
                                </span>
                                {saveMessage}
                            </p>
                        )}
                    </div>
                )}
            </div>
        </div>
    );
};
