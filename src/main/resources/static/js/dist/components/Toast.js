window.ToastContainer = function ToastContainer({
  toast
}) {
  if (!toast) return null;
  const message = typeof toast === 'string' ? toast : toast.message;
  const isError = typeof toast === 'object' && toast.type === 'error';
  const accentColor = isError ? '#ef4444' : '#10b981';
  React.useEffect(() => {
    if (window.lucide) window.lucide.createIcons();
  }, [toast]);
  return React.createElement("div", {
    className: "toast-container"
  }, React.createElement("div", {
    className: "toast",
    style: {
      borderColor: accentColor
    },
    role: isError ? 'alert' : 'status'
  }, React.createElement("i", {
    "data-lucide": isError ? 'alert-circle' : 'check-circle',
    style: {
      color: accentColor,
      width: '20px',
      height: '20px'
    }
  }), React.createElement("span", null, message)));
};
