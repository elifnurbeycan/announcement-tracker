/**
 * Departments Page Component
 */
window.DepartmentsPage = function DepartmentsPage({
    departments = [],
    subscribers = [],
    onOpenAddModal,
    onOpenEditModal,
    onDeleteDepartment,
    onBulkDeleteDepartments
}) {
    const [selectionMode, setSelectionMode] = React.useState(false);
    const [selectedIds, setSelectedIds] = React.useState([]);

    React.useEffect(() => {
        if (window.lucide) window.lucide.createIcons();
    }, [departments, subscribers, selectionMode, selectedIds]);

    const isAllSelected = departments.length > 0 && selectedIds.length === departments.length;

    const handleSelectAll = () => {
        if (isAllSelected) {
            setSelectedIds([]);
        } else {
            setSelectedIds(departments.map(d => d.id));
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
        if (onBulkDeleteDepartments && selectedIds.length > 0) {
            onBulkDeleteDepartments(selectedIds, () => {
                setSelectedIds([]);
                setSelectionMode(false);
            });
        }
    };

    const getDeptMemberCount = (deptId) => {
        return subscribers.filter(s => {
            if (s.departments && s.departments.length > 0) {
                return s.departments.some(d => d.id === deptId);
            }
            return s.departmentId === deptId;
        }).length;
    };

    return (
        <div>
            {/* Header Flex */}
            <div className="card-header-flex" style={{ marginBottom: '24px' }}>
                <div>
                    <h2 style={{ fontSize: '20px', fontWeight: '800', color: 'white' }}>Departman Yönetimi</h2>
                    <p style={{ fontSize: '13px', color: 'var(--text-sub)', marginTop: '4px' }}>
                        Kurumsal departmanları ve bu departmanlara atanan otomatik duyuru kaynaklarını yönetin.
                    </p>
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

                    <button className="btn-action-primary" onClick={onOpenAddModal}>
                        <i data-lucide="plus" style={{ width: '16px', height: '16px' }}></i>
                        + Departman Ekle
                    </button>
                </div>
            </div>

            {/* Department Table Card */}
            <div className="card">
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
                                <th>Departman Adı</th>
                                <th>Atanan Kaynaklar</th>
                                <th>Bağlı Abone Sayısı</th>
                                <th style={{ textAlign: 'right' }}>İşlemler</th>
                            </tr>
                        </thead>
                        <tbody>
                            {departments.length === 0 ? (
                                <tr>
                                    <td colSpan={selectionMode ? "5" : "4"} style={{ textAlign: 'center', padding: '30px', color: 'var(--text-sub)' }}>
                                        Kayıtlı departman bulunmuyor.
                                    </td>
                                </tr>
                            ) : (
                                departments.map(dept => {
                                    const isSelected = selectedIds.includes(dept.id);
                                    const memberCount = getDeptMemberCount(dept.id);
                                    const sites = dept.assignedSites || [];

                                    return (
                                        <tr key={dept.id} style={{ background: isSelected ? 'rgba(239, 68, 68, 0.08)' : undefined }}>
                                            {selectionMode && (
                                                <td style={{ width: '44px', textAlign: 'center' }}>
                                                    <input 
                                                        type="checkbox" 
                                                        checked={isSelected}
                                                        onChange={() => handleToggleSelect(dept.id)}
                                                        style={{ cursor: 'pointer', width: '17px', height: '17px', accentColor: '#ef4444' }}
                                                    />
                                                </td>
                                            )}
                                            <td style={{ fontWeight: '700', color: 'white' }}>{dept.name}</td>
                                            <td>
                                                <div style={{ display: 'flex', flexWrap: 'wrap', gap: '6px' }}>
                                                    {sites.length === 0 ? (
                                                        <span style={{ fontSize: '12px', color: 'var(--text-sub)', fontStyle: 'italic' }}>Sitesiz Departman</span>
                                                    ) : (
                                                        sites.map(site => (
                                                            <span key={site} className="site-badge">{site}</span>
                                                        ))
                                                    )}
                                                </div>
                                            </td>
                                            <td style={{ fontSize: '13px', color: 'var(--text-sub)' }}>{memberCount} Abone</td>
                                            <td style={{ textAlign: 'right' }}>
                                                <div style={{ display: 'inline-flex', gap: '6px' }}>
                                                    <button 
                                                        className="btn-sm-action" 
                                                        style={{ background: 'rgba(59, 130, 246, 0.15)', color: '#60a5fa', borderColor: 'rgba(59, 130, 246, 0.3)' }}
                                                        onClick={() => onOpenEditModal(dept)}
                                                    >
                                                        <i data-lucide="edit-3" style={{ width: '14px', height: '14px' }}></i>
                                                        Düzenle
                                                    </button>
                                                    <button 
                                                        className="btn-sm-action" 
                                                        style={{ background: 'rgba(239, 68, 68, 0.15)', color: '#f87171', borderColor: 'rgba(239, 68, 68, 0.3)' }}
                                                        onClick={() => onDeleteDepartment(dept.id)}
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
