/**
 * Subscribers Page Component
 */
window.SubscribersPage = function SubscribersPage({
    subscribers = [],
    onOpenAddModal,
    onOpenEditModal,
    onOpenImportModal,
    onToggleStatus,
    onSendPasswordSetupEmail,
    onDeleteSubscriber,
    onBulkDeleteSubscribers
}) {
    const [search, setSearch] = React.useState('');
    const [selectionMode, setSelectionMode] = React.useState(false);
    const [selectedIds, setSelectedIds] = React.useState([]);

    React.useEffect(() => {
        if (window.lucide) window.lucide.createIcons();
    }, [subscribers, search, selectionMode, selectedIds]);

    const filteredSubscribers = subscribers.filter(s => {
        const query = search.toLowerCase();
        const nameMatch = s.fullName && s.fullName.toLowerCase().includes(query);
        const emailMatch = s.email && s.email.toLowerCase().includes(query);
        return nameMatch || emailMatch;
    });

    const isAllSelected = filteredSubscribers.length > 0 && selectedIds.length === filteredSubscribers.length;

    const handleSelectAll = () => {
        if (isAllSelected) {
            setSelectedIds([]);
        } else {
            setSelectedIds(filteredSubscribers.map(s => s.id));
        }
    };

    const handleToggleSelect = (id) => {
        if (selectedIds.includes(id)) {
            setSelectedIds(selectedIds.filter(i => i !== id));
        } else {
            setSelectedIds([...selectedIds, id]);
        }
    };

    const handleExitSelectionMode = () => {
        setSelectionMode(false);
        setSelectedIds([]);
    };

    const handleBulkDeleteClick = () => {
        if (onBulkDeleteSubscribers && selectedIds.length > 0) {
            onBulkDeleteSubscribers(selectedIds, () => {
                setSelectedIds([]);
                setSelectionMode(false);
            });
        }
    };

    return (
        <div>
            {/* Toolbar Card */}
            <div className="toolbar-card">
                <div className="search-input-group">
                    <i data-lucide="search"></i>
                    <input 
                        type="text" 
                        className="input-field" 
                        placeholder="Abone ara / filtrele (ad, e-posta)..." 
                        value={search}
                        onChange={(e) => setSearch(e.target.value)}
                    />
                </div>

                <div style={{ display: 'flex', gap: '10px', alignItems: 'center' }}>
                    {!selectionMode ? (
                        <button 
                            className="btn-action-secondary" 
                            onClick={() => setSelectionMode(true)}
                        >
                            <i data-lucide="check-square" style={{ width: '16px', height: '16px' }}></i>
                            Toplu Seç
                        </button>
                    ) : (
                        <>
                            {selectedIds.length > 0 && (
                                <button 
                                    className="btn-action-secondary" 
                                    onClick={handleBulkDeleteClick}
                                    style={{ background: 'rgba(239, 68, 68, 0.25)', color: '#f87171', border: '1px solid rgba(239, 68, 68, 0.4)', fontWeight: '700' }}
                                >
                                    <i data-lucide="trash-2" style={{ width: '16px', height: '16px' }}></i>
                                    Seçilenleri Sil ({selectedIds.length})
                                </button>
                            )}
                            <button 
                                className="btn-action-secondary" 
                                onClick={handleExitSelectionMode}
                                style={{ background: 'rgba(148, 163, 184, 0.15)', color: '#cbd5e1', border: '1px solid rgba(148, 163, 184, 0.3)' }}
                            >
                                <i data-lucide="x" style={{ width: '16px', height: '16px' }}></i>
                                İptal Et
                            </button>
                        </>
                    )}

                    <button className="btn-action-secondary" onClick={onOpenImportModal}>
                        <i data-lucide="file-spread-sheet" style={{ width: '16px', height: '16px' }}></i>
                        Excel/CSV'den İçe Aktar
                    </button>
                    <button className="btn-action-primary" onClick={onOpenAddModal}>
                        <i data-lucide="user-plus" style={{ width: '16px', height: '16px' }}></i>
                        + Abone Ekle
                    </button>
                </div>
            </div>

            {/* Table Card */}
            <div className="card">
                <div className="card-header-flex">
                    <h2>
                        <i data-lucide="users" style={{ color: '#60a5fa', width: '20px', height: '20px' }}></i>
                        Kayıtlı Aboneler ({filteredSubscribers.length})
                        {selectionMode && selectedIds.length > 0 && (
                            <span style={{ fontSize: '13px', color: '#f87171', marginLeft: '10px', fontWeight: '600' }}>
                                ({selectedIds.length} abone seçildi)
                            </span>
                        )}
                    </h2>
                </div>

                <div className="table-responsive">
                    <table className="data-table">
                        <thead>
                            <tr>
                                {selectionMode && (
                                    <th style={{ width: '44px', textAlign: 'center' }}>
                                        <input 
                                            type="checkbox" 
                                            checked={isAllSelected}
                                            onChange={handleSelectAll}
                                            title="Tümünü Seç"
                                            style={{ cursor: 'pointer', width: '17px', height: '17px', accentColor: '#ef4444' }}
                                        />
                                    </th>
                                )}
                                <th>Ad Soyad</th>
                                <th>E-posta</th>
                                <th>Departman</th>
                                <th>Kişisel Siteler</th>
                                <th>Durum</th>
                                <th style={{ textAlign: 'right' }}>İşlemler</th>
                            </tr>
                        </thead>
                        <tbody>
                            {filteredSubscribers.length === 0 ? (
                                <tr>
                                    <td colSpan={selectionMode ? "7" : "6"} style={{ textAlign: 'center', padding: '30px', color: 'var(--text-sub)' }}>
                                        Kayıtlı abone bulunamadı.
                                    </td>
                                </tr>
                            ) : (
                                filteredSubscribers.map(sub => {
                                    const isSelected = selectedIds.includes(sub.id);
                                    const depts = sub.departments && sub.departments.length > 0
                                        ? sub.departments.map(d => d.name).join(', ')
                                        : (sub.departmentName || '');

                                    const sitesList = sub.subscribedSites || sub.preferredSites || [];
                                    const preferred = sitesList.length > 0
                                        ? sitesList.join(', ')
                                        : 'Tüm Kaynaklar';

                                    return (
                                        <tr key={sub.id} style={{ background: isSelected ? 'rgba(239, 68, 68, 0.08)' : undefined }}>
                                            {selectionMode && (
                                                <td style={{ width: '44px', textAlign: 'center' }}>
                                                    <input 
                                                        type="checkbox" 
                                                        checked={isSelected}
                                                        onChange={() => handleToggleSelect(sub.id)}
                                                        style={{ cursor: 'pointer', width: '17px', height: '17px', accentColor: '#ef4444' }}
                                                    />
                                                </td>
                                            )}
                                            <td style={{ fontWeight: '700', color: 'white' }}>{sub.fullName}</td>
                                            <td style={{ color: 'var(--text-sub)' }}>{sub.email}</td>
                                            <td>
                                                {depts ? (
                                                    <span style={{ background: 'rgba(59, 130, 246, 0.15)', color: '#60a5fa', border: '1px solid rgba(59, 130, 246, 0.3)', padding: '3px 8px', borderRadius: '6px', fontSize: '12px', fontWeight: '600' }}>
                                                        {depts}
                                                    </span>
                                                ) : (
                                                    <span style={{ background: 'rgba(139, 92, 246, 0.15)', color: '#c084fc', border: '1px solid rgba(139, 92, 246, 0.3)', padding: '3px 8px', borderRadius: '6px', fontSize: '12px', fontWeight: '700' }}>
                                                        Genel Çalışan
                                                    </span>
                                                )}
                                            </td>
                                            <td style={{ fontSize: '12.5px', color: 'var(--text-sub)' }}>{preferred}</td>
                                            <td>
                                                <button 
                                                    onClick={() => onToggleStatus(sub)}
                                                    style={{
                                                        background: sub.active ? 'rgba(16, 185, 129, 0.15)' : 'rgba(239, 68, 68, 0.15)',
                                                        color: sub.active ? '#34d399' : '#f87171',
                                                        border: `1px solid ${sub.active ? 'rgba(16, 185, 129, 0.3)' : 'rgba(239, 68, 68, 0.3)'}`,
                                                        padding: '4px 10px',
                                                        borderRadius: '8px',
                                                        fontSize: '12px',
                                                        fontWeight: '700',
                                                        cursor: 'pointer'
                                                    }}
                                                >
                                                    {sub.active ? 'Aktif' : 'Pasif'}
                                                </button>
                                            </td>
                                            <td style={{ textAlign: 'right' }}>
                                                <div style={{ display: 'inline-flex', gap: '6px' }}>
                                                    <button
                                                        className="btn-sm-action"
                                                        style={{ background: 'rgba(16, 185, 129, 0.15)', color: '#34d399', borderColor: 'rgba(16, 185, 129, 0.3)' }}
                                                        onClick={() => onSendPasswordSetupEmail(sub)}
                                                        title="Tek kullanımlık ve süreli şifre belirleme bağlantısını gönder"
                                                    >
                                                        <i data-lucide="mail-key" style={{ width: '14px', height: '14px' }}></i>
                                                        Şifre Bağlantısı
                                                    </button>
                                                    <button 
                                                        className="btn-sm-action" 
                                                        style={{ background: 'rgba(59, 130, 246, 0.15)', color: '#60a5fa', borderColor: 'rgba(59, 130, 246, 0.3)' }}
                                                        onClick={() => onOpenEditModal(sub)}
                                                    >
                                                        <i data-lucide="edit-3" style={{ width: '14px', height: '14px' }}></i>
                                                        Düzenle
                                                    </button>
                                                    <button 
                                                        className="btn-sm-action" 
                                                        style={{ background: 'rgba(239, 68, 68, 0.15)', color: '#f87171', borderColor: 'rgba(239, 68, 68, 0.3)' }}
                                                        onClick={() => onDeleteSubscriber(sub.id)}
                                                    >
                                                        <i data-lucide="trash-2" style={{ width: '14px', height: '14px' }}></i>
                                                        Sil
                                                    </button>
                                                </div>
                                            </td>
                                        </tr>
                                    );
                                })
                            )}
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    );
};
