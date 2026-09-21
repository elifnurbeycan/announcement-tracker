/**
 * Toast Component
 */
window.ToastContainer = function ToastContainer({ toast }) {
    if (!toast) return null;

    React.useEffect(() => {
        if (window.lucide) window.lucide.createIcons();
    }, [toast]);

    return (
        <div className="toast-container">
            <div className="toast">
                <i data-lucide="check-circle" style={{ color: '#10b981', width: '20px', height: '20px' }}></i>
                <span>{toast}</span>
            </div>
        </div>
    );
};
