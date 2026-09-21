/**
 * Main Dashboard Application Container
 */
function DashboardApp() {
    const [user, setUser] = React.useState({});
    const [activeTab, setActiveTab] = React.useState('overview');
    const [collapsed, setCollapsed] = React.useState(false);
    const [mobileOpen, setMobileOpen] = React.useState(false);
    const [toast, setToast] = React.useState(null);
    const [scraping, setScraping] = React.useState(false);
    const [userDropdownOpen, setUserDropdownOpen] = React.useState(false);

    // Data State
    const [announcements, setAnnouncements] = React.useState([]);
    const [subscribers, setSubscribers] = React.useState([]);
    const [departments, setDepartments] = React.useState([]);
    const [availableSites, setAvailableSites] = React.useState(['EBELGE_GIB', 'KOSGEB']);
    const [scrapePeriod, setScrapePeriod] = React.useState(30);

    // Pagination & Search State for Announcements
    const [page, setPage] = React.useState(0);
    const [totalPages, setTotalPages] = React.useState(1);
    const [totalElements, setTotalElements] = React.useState(0);
    const [searchQuery, setSearchQuery] = React.useState('');
    const [selectedSiteFilter, setSelectedSiteFilter] = React.useState('');
    const [hasAttachmentFilter, setHasAttachmentFilter] = React.useState(false);

    // Modal State
    const [subscriberModalOpen, setSubscriberModalOpen] = React.useState(false);
    const [editSubscriber, setEditSubscriber] = React.useState(null);
    const [departmentModalOpen, setDepartmentModalOpen] = React.useState(false);
    const [editDept, setEditDept] = React.useState(null);
    const [importModalOpen, setImportModalOpen] = React.useState(false);
    const [confirmModalConfig, setConfirmModalConfig] = React.useState({
        isOpen: false,
        title: '',
        message: '',
        confirmText: 'Sil',
        onConfirm: null
    });

    // Toast helper
    const showToast = (msg, type = 'success') => {
        setToast({ message: msg, type });
        setTimeout(() => setToast(null), 4000);
    };

    // Initialize & Route Handle
    React.useEffect(() => {
        const loadAdminProfile = async () => {
            try {
                const adminUser = await window.AuthService.getAdminProfile();
                sessionStorage.setItem('authRole', 'ADMIN');
                setUser(adminUser);
            } catch (error) {
                console.error('Yönetici profili yüklenemedi:', error);
            }
        };
        loadAdminProfile();

        // Hash Route Check
        const hash = window.location.hash.replace('#/', '');
        if (hash && ['overview', 'announcements', 'subscribers', 'departments', 'sources', 'settings', 'profile'].includes(hash)) {
            setActiveTab(hash);
        }

        const handleHashChange = () => {
            const h = window.location.hash.replace('#/', '');
            if (h && ['overview', 'announcements', 'subscribers', 'departments', 'sources', 'settings', 'profile'].includes(h)) {
                setActiveTab(h);
            }
        };

        window.addEventListener('hashchange', handleHashChange);
        return () => window.removeEventListener('hashchange', handleHashChange);
    }, []);

    // Load Data
    const loadAnnouncements = React.useCallback(async () => {
        try {
            const data = await window.AnnouncementService.getAnnouncements(page, 10, selectedSiteFilter, searchQuery);
            if (data && data.content) {
                let items = data.content;
                if (hasAttachmentFilter) {
                    items = items.filter(a => a.attachmentUrl && a.attachmentUrl.trim() !== '');
                }
                setAnnouncements(items);
                setTotalPages(data.totalPages || 1);
                setTotalElements(data.totalElements !== undefined ? data.totalElements : items.length);
            }
        } catch(e) {
            console.error('Failed to load announcements:', e);
        }
    }, [page, selectedSiteFilter, searchQuery, hasAttachmentFilter]);

    const loadSubscribers = React.useCallback(async () => {
        try {
            const data = await window.SubscriberService.getSubscribers();
            if (Array.isArray(data)) setSubscribers(data);
        } catch(e) {
            console.error('Failed to load subscribers:', e);
        }
    }, []);

    const loadDepartments = React.useCallback(async () => {
        try {
            const data = await window.DepartmentService.getDepartments();
            if (Array.isArray(data)) setDepartments(data);
        } catch(e) {
            console.error('Failed to load departments:', e);
        }
    }, []);

    const loadSites = React.useCallback(async () => {
        try {
            const data = await window.AnnouncementService.getSites();
            if (Array.isArray(data)) {
                const codes = data.map(s => typeof s === 'string' ? s : (s.code || s.name));
                setAvailableSites(codes);
            }
        } catch(e) {
            console.error('Failed to load sites:', e);
        }
    }, []);

    const loadSettings = React.useCallback(async () => {
        try {
            const data = await window.SettingsService.getSettings();
            if (data && data.intervalMinutes) setScrapePeriod(data.intervalMinutes);
        } catch(e) {
            console.error('Failed to load settings:', e);
        }
    }, []);

    React.useEffect(() => {
        loadAnnouncements();
        loadSubscribers();
        loadDepartments();
        loadSites();
        loadSettings();
    }, [loadAnnouncements, loadSubscribers, loadDepartments, loadSites, loadSettings]);

    // Handlers
    const handleScrape = async () => {
        setScraping(true);
        try {
            const data = await window.AnnouncementService.triggerScrape();
            const items = (data && Array.isArray(data.data)) ? data.data : (Array.isArray(data) ? data : []);
            const count = items.length;
            if (count > 0) {
                showToast(`Tarama tamamlandı. ${count} yeni duyuru eklendi.`);
            } else {
                showToast('Tarama tamamlandı. Yeni duyuru bulunamadı.');
            }
            await loadAnnouncements();
        } catch(e) {
            console.error('Tarama hatası:', e);
            showToast(e.message || 'Tarama sırasında bir hata oluştu.', 'error');
        } finally {
            setScraping(false);
        }
    };

    const handleSaveSubscriber = async (payload) => {
        try {
            if (payload.id) {
                await window.SubscriberService.updateSubscriber(payload.id, payload);
                showToast('Abone ve kurumsal giriş hesabı güncellendi.');
            } else {
                await window.SubscriberService.createSubscriber(payload);
                showToast('Abone ve kurumsal giriş hesabı oluşturuldu.');
            }
            setSubscriberModalOpen(false);
            setEditSubscriber(null);
            await loadSubscribers();
        } catch(e) {
            showToast(e.message || 'Abone kaydedilirken hata oluştu.', 'error');
        }
    };

    const handleDeleteSubscriber = (id) => {
        setConfirmModalConfig({
            isOpen: true,
            title: 'Abone Silinecek',
            message: 'Bu aboneyi silmek istediğinize emin misiniz? Bu işlem geri alınamaz.',
            confirmText: 'Aboneyi Sil',
            onConfirm: async () => {
                setConfirmModalConfig(prev => ({ ...prev, isOpen: false }));
                try {
                    await window.SubscriberService.deleteSubscriber(id);
                    showToast('Abone silindi.');
                    await loadSubscribers();
                } catch(e) {
                    showToast(e.message || 'Abone silinemedi.', 'error');
                }
            }
        });
    };

    const handleToggleStatus = async (sub) => {
        try {
            await window.SubscriberService.toggleStatus(sub.id, sub.active);
            showToast(`Abone durumu ${!sub.active ? 'Aktif' : 'Pasif'} yapıldı.`);
            await loadSubscribers();
        } catch(e) {
            showToast(e.message || 'Durum değiştirilemedi.', 'error');
        }
    };

    const handleSaveDepartment = async (payload) => {
        if (payload.id) {
            await window.DepartmentService.updateDepartment(payload.id, payload);
            showToast('Departman güncellendi.');
        } else {
            await window.DepartmentService.createDepartment(payload);
            showToast('Departman oluşturuldu.');
        }
        setDepartmentModalOpen(false);
        setEditDept(null);
        await loadDepartments();
        await loadSubscribers();
    };

    const handleDeleteDepartment = (id) => {
        setConfirmModalConfig({
            isOpen: true,
            title: 'Departman Silinecek',
            message: 'Bu departmanı silmek istediğinize emin misiniz? Bu işlem geri alınamaz.',
            confirmText: 'Departmanı Sil',
            onConfirm: async () => {
                setConfirmModalConfig(prev => ({ ...prev, isOpen: false }));
                try {
                    await window.DepartmentService.deleteDepartment(id);
                    showToast('Departman silindi.');
                    await loadDepartments();
                    await loadSubscribers();
                } catch(e) {
                    showToast(e.message || 'Departman silinemedi.', 'error');
                }
            }
        });
    };

    const handleBulkDeleteSubscribers = (ids, clearSelection) => {
        if (!ids || ids.length === 0) return;
        setConfirmModalConfig({
            isOpen: true,
            title: 'Toplu Abone Silme',
            message: `Seçilen ${ids.length} adet aboneyi silmek istediğinize emin misiniz? Bu işlem geri alınamaz.`,
            confirmText: `${ids.length} Aboneyi Sil`,
            onConfirm: async () => {
                setConfirmModalConfig(prev => ({ ...prev, isOpen: false }));
                try {
                    await window.SubscriberService.deleteSubscribersBatch(ids);
                    showToast(`${ids.length} adet abone başarıyla silindi.`);
                    if (clearSelection) clearSelection();
                    await loadSubscribers();
                } catch(e) {
                    showToast(e.message || 'Toplu silme işlemi başarısız.', 'error');
                }
            }
        });
    };

    const handleBulkDeleteDepartments = (ids, clearSelection) => {
        if (!ids || ids.length === 0) return;
        setConfirmModalConfig({
            isOpen: true,
            title: 'Toplu Departman Silme',
            message: `Seçilen ${ids.length} adet departmanı silmek istediğinize emin misiniz? Bu işlem geri alınamaz.`,
            confirmText: `${ids.length} Departmanı Sil`,
            onConfirm: async () => {
                setConfirmModalConfig(prev => ({ ...prev, isOpen: false }));
                try {
                    await window.DepartmentService.deleteDepartmentsBatch(ids);
                    showToast(`${ids.length} adet departman başarıyla silindi.`);
                    if (clearSelection) clearSelection();
                    await loadDepartments();
                    await loadSubscribers();
                } catch(e) {
                    showToast(e.message || 'Toplu silme işlemi başarısız.', 'error');
                }
            }
        });
    };

    const handleSaveScrapePeriod = async (periodMinutes) => {
        try {
            await window.SettingsService.updateScrapePeriod(periodMinutes);
            setScrapePeriod(periodMinutes);
            showToast('Tarama periyodu güncellendi.');
        } catch(e) {
            showToast(e.message || 'Ayarlar kaydedilemedi.', 'error');
        }
    };

    return (
        <div className="dashboard-container">
            <window.Sidebar 
                activeTab={activeTab} 
                setActiveTab={setActiveTab}
                collapsed={collapsed}
                mobileOpen={mobileOpen}
                role="ROLE_ADMIN"
            />

            <div className={`main-wrapper ${collapsed ? 'collapsed' : ''}`}>
                <window.Header 
                    activeTab={activeTab}
                    collapsed={collapsed}
                    setCollapsed={setCollapsed}
                    mobileOpen={mobileOpen}
                    setMobileOpen={setMobileOpen}
                    handleScrape={handleScrape}
                    scraping={scraping}
                    userDropdownOpen={userDropdownOpen}
                    setUserDropdownOpen={setUserDropdownOpen}
                    onLogout={() => window.AuthService.logoutAdmin()}
                    user={user}
                    role="ROLE_ADMIN"
                />

                <main className="content-body">
                    {activeTab === 'overview' && (
                        <window.OverviewPage 
                            announcements={announcements}
                            totalAnnouncements={totalElements}
                            subscribers={subscribers}
                            departments={departments}
                            availableSites={availableSites}
                            onNavigate={(tab) => {
                                window.location.hash = `#/${tab}`;
                                setActiveTab(tab);
                            }}
                        />
                    )}

                    {activeTab === 'announcements' && (
                        <window.AnnouncementsPage 
                            announcements={announcements}
                            availableSites={availableSites}
                            searchQuery={searchQuery}
                            setSearchQuery={setSearchQuery}
                            selectedSiteFilter={selectedSiteFilter}
                            setSelectedSiteFilter={setSelectedSiteFilter}
                            hasAttachmentFilter={hasAttachmentFilter}
                            setHasAttachmentFilter={setHasAttachmentFilter}
                            page={page}
                            setPage={setPage}
                            totalPages={totalPages}
                            totalElements={totalElements}
                        />
                    )}

                    {activeTab === 'subscribers' && (
                        <window.SubscribersPage 
                            subscribers={subscribers}
                            onOpenAddModal={() => { setEditSubscriber(null); setSubscriberModalOpen(true); }}
                            onOpenEditModal={(sub) => { setEditSubscriber(sub); setSubscriberModalOpen(true); }}
                            onOpenImportModal={() => setImportModalOpen(true)}
                            onToggleStatus={handleToggleStatus}
                            onDeleteSubscriber={handleDeleteSubscriber}
                            onBulkDeleteSubscribers={handleBulkDeleteSubscribers}
                        />
                    )}

                    {activeTab === 'departments' && (
                        <window.DepartmentsPage 
                            departments={departments}
                            subscribers={subscribers}
                            onOpenAddModal={() => { setEditDept(null); setDepartmentModalOpen(true); }}
                            onOpenEditModal={(dept) => { setEditDept(dept); setDepartmentModalOpen(true); }}
                            onDeleteDepartment={handleDeleteDepartment}
                            onBulkDeleteDepartments={handleBulkDeleteDepartments}
                        />
                    )}

                    {activeTab === 'sources' && (
                        <window.SourcesPage 
                            availableSites={availableSites}
                            announcements={announcements}
                        />
                    )}

                    {activeTab === 'settings' && (
                        <window.SettingsPage 
                            scrapePeriod={scrapePeriod}
                            onSavePeriod={handleSaveScrapePeriod}
                        />
                    )}

                    {activeTab === 'profile' && (
                        <window.ProfilePage 
                            user={user}
                            role="ROLE_ADMIN"
                            availableSites={availableSites}
                        />
                    )}
                </main>
            </div>

            {/* Modals */}
            <window.SubscriberModal 
                isOpen={subscriberModalOpen}
                onClose={() => { setSubscriberModalOpen(false); setEditSubscriber(null); }}
                onSave={handleSaveSubscriber}
                editSubscriber={editSubscriber}
                departments={departments}
                availableSites={availableSites}
            />

            <window.DepartmentModal 
                isOpen={departmentModalOpen}
                onClose={() => { setDepartmentModalOpen(false); setEditDept(null); }}
                onSave={handleSaveDepartment}
                editDept={editDept}
                availableSites={availableSites}
            />

            <window.ImportModal 
                isOpen={importModalOpen}
                onClose={() => setImportModalOpen(false)}
                onImportSuccess={(msg) => {
                    showToast(msg);
                    loadSubscribers();
                }}
            />

            <window.ConfirmModal 
                isOpen={confirmModalConfig.isOpen}
                onClose={() => setConfirmModalConfig(prev => ({ ...prev, isOpen: false }))}
                onConfirm={confirmModalConfig.onConfirm}
                title={confirmModalConfig.title}
                message={confirmModalConfig.message}
                confirmText={confirmModalConfig.confirmText}
            />

            {/* Toast Container */}
            <window.ToastContainer toast={toast} />
        </div>
    );
}

ReactDOM.render(<DashboardApp />, document.getElementById('root'));
