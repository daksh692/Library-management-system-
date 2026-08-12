import { createContext, useContext, useState, useCallback, useRef } from 'react';
import { CheckCircle2, AlertTriangle, XCircle, X } from 'lucide-react';

const ToastContext = createContext(null);

/**
 * App-wide toast notifications.
 *
 * Usage:
 *   const toast = useToast();
 *   toast.success('Book issued to Alice Smith');
 *   toast.error('That copy is already on loan');
 */
export const ToastProvider = ({ children }) => {
  const [toasts, setToasts] = useState([]);
  const idRef = useRef(0);

  const dismiss = useCallback((id) => {
    setToasts((current) => current.filter((t) => t.id !== id));
  }, []);

  const push = useCallback((variant, message, ttl = 5000) => {
    const id = ++idRef.current;
    setToasts((current) => [...current, { id, variant, message }]);
    if (ttl) setTimeout(() => dismiss(id), ttl);
    return id;
  }, [dismiss]);

  const api = {
    success: (m) => push('success', m),
    error:   (m) => push('error', m, 8000), // errors linger longer
    warning: (m) => push('warning', m, 6000),
    dismiss,
  };

  const styles = {
    success: 'bg-emerald-50 border-emerald-500 text-emerald-900',
    error:   'bg-red-50 border-red-500 text-red-900',
    warning: 'bg-amber-50 border-amber-500 text-amber-900',
  };

  const icons = {
    success: <CheckCircle2 className="h-5 w-5 text-emerald-600 flex-shrink-0" />,
    error:   <XCircle className="h-5 w-5 text-red-600 flex-shrink-0" />,
    warning: <AlertTriangle className="h-5 w-5 text-amber-600 flex-shrink-0" />,
  };

  return (
    <ToastContext.Provider value={api}>
      {children}

      <div
        className="fixed bottom-6 right-6 z-[100] flex flex-col gap-3 w-full max-w-sm"
        role="region"
        aria-label="Notifications"
      >
        {toasts.map((t) => (
          <div
            key={t.id}
            role="status"
            aria-live="polite"
            className={`flex items-start gap-3 border-l-4 rounded-r-md shadow-lg p-4 ${styles[t.variant]}`}
          >
            {icons[t.variant]}
            <p className="text-sm flex-1 leading-snug">{t.message}</p>
            <button
              onClick={() => dismiss(t.id)}
              className="opacity-50 hover:opacity-100 transition-opacity"
              aria-label="Dismiss notification"
            >
              <X className="h-4 w-4" />
            </button>
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  );
};

export const useToast = () => {
  const ctx = useContext(ToastContext);
  if (!ctx) throw new Error('useToast must be used inside a ToastProvider');
  return ctx;
};
