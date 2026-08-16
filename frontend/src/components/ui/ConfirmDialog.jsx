import { AlertTriangle } from 'lucide-react';
import { useEffect } from 'react';
import { useFocusTrap } from '../../hooks/useFocusTrap';

/**
 * Modal confirmation. Render conditionally on `isOpen`.
 *
 * @param {{isOpen: boolean, title: string, message: string, confirmLabel?: string,
 *          variant?: 'danger'|'default', onConfirm: () => void, onCancel: () => void}} props
 */
const ConfirmDialog = ({
  isOpen,
  title,
  message,
  confirmLabel = 'Confirm',
  variant = 'danger',
  onConfirm,
  onCancel,
}) => {
  useEffect(() => {
    const onKey = (e) => e.key === 'Escape' && onCancel();
    if (isOpen) {
      document.addEventListener('keydown', onKey);
    }
    return () => document.removeEventListener('keydown', onKey);
  }, [isOpen, onCancel]);

  const trapRef = useFocusTrap(isOpen);

  if (!isOpen) return null;

  const confirmStyles = variant === 'danger'
    ? 'bg-red-600 hover:bg-red-700'
    : 'bg-slate-900 hover:bg-slate-800';

  return (
    <div
      className="fixed inset-0 bg-slate-900/50 flex items-center justify-center p-4 z-[90]"
      role="dialog"
      aria-modal="true"
      aria-labelledby="confirm-title"
      onClick={onCancel}
      ref={trapRef}
    >
      <div
        className="bg-white rounded-lg shadow-xl w-full max-w-md"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="p-6 flex gap-4">
          <div className="h-10 w-10 rounded-full bg-red-50 flex items-center justify-center flex-shrink-0">
            <AlertTriangle className="h-5 w-5 text-red-600" />
          </div>
          <div>
            <h3 id="confirm-title" className="text-lg font-serif font-bold text-slate-900">
              {title}
            </h3>
            <p className="text-sm text-slate-600 mt-2 leading-relaxed">{message}</p>
          </div>
        </div>
        <div className="px-6 py-4 bg-slate-50 border-t border-slate-200 flex justify-end gap-3 rounded-b-lg">
          <button
            onClick={onCancel}
            className="px-4 py-2 border border-slate-300 bg-white rounded-md text-slate-700 hover:bg-slate-50 text-sm font-medium"
          >
            Cancel
          </button>
          <button
            onClick={onConfirm}
            className={`px-4 py-2 text-white rounded-md text-sm font-medium ${confirmStyles}`}
          >
            {confirmLabel}
          </button>
        </div>
      </div>
    </div>
  );
};

export default ConfirmDialog;
