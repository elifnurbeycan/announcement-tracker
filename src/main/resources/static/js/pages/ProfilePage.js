/**
 * Profile Page Component
 */
window.ProfilePage = function ProfilePage({
    user = {},
    role = 'ROLE_ADMIN',
    availableSites = []
}) {
    React.useEffect(() => {
        if (window.lucide) window.lucide.createIcons();
    }, [user, role, availableSites]);

    const isAdmin = role === 'ROLE_ADMIN';
    const name = user.fullName || user.username || user.email || (isAdmin ? 'Süper Admin' : 'Kullanıcı');
    const email = user.email || (user.username ? `${user.username}@kurum.com` : 'admin@kurum.com');
    const roleText = isAdmin ? 'Süper Admin (Yönetici)' : 'Normal Çalışan (Abone)';

    // Compute Effective Sources
    let effectiveSources = [];
    if (isAdmin) {
        effectiveSources = availableSites.length > 0 ? availableSites : ['EBELGE_GIB', 'KOSGEB'];
    } else {
        const userDepts = user.departments || (user.departmentName ? [{ name: user.departmentName }] : []);
        const isGeneralEmployee = userDepts.length === 0;

        if (isGeneralEmployee) {
            effectiveSources = availableSites.length > 0 ? availableSites : ['EBELGE_GIB', 'KOSGEB'];
        } else {
            const deptSites = userDepts.flatMap(d => d.assignedSites || []);
            const personalSites = user.preferredSites || [];
            effectiveSources = Array.from(new Set([...deptSites, ...personalSites]));
            if (effectiveSources.length === 0) {
                effectiveSources = availableSites;
            }
        }
    }

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
            </div>
        </div>
    );
};
