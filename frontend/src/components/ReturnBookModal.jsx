import { useState, useEffect } from 'react';
import { BookDown } from 'lucide-react';
import api from '../services/api';
import { apiErrorMessage } from '../services/errors';
import { useToast } from './ui/ToastProvider';

const CONDITIONS = [
  { value: 'GOOD',    label: 'Good',    hint: 'No fine beyond any late charge',
    on: 'bg-emerald-600 text-white border-emerald-600',
    off: 'border-emerald-300 text-emerald-800 hover:bg-emerald-50' },
  { value: 'DAMAGED', label: 'Damaged', hint: 'Adds a share of the book price',
    on: 'bg-amber-500 text-white border-amber-500',
    off: 'border-amber-300 text-amber-800 hover:bg-amber-50' },
  { value: 'LOST',    label: 'Lost',    hint: 'Full price, and the copy is written off',
    on: 'bg-red-600 text-white border-red-600',
    off: 'border-red-300 text-red-800 hover:bg-red-50' },
];

/** Check-in flow. Pass the whole transaction so the dialog can show context. */
const ReturnBookModal = ({ transaction, onClose, onSuccess }) => {
  const [condition, setCondition] = useState('GOOD');
  const [submitting, setSubmitting] = useState(false);
  const toast = useToast();

  useEffect(() => { if (transaction) setCondition('GOOD'); }, [transaction]);

  useEffect(() => {
    const onKey = (e) => e.key === 'Escape' && onClose();
    if (transaction) document.addEventListener('keydown', onKey);
    return () => document.removeEventListener('keydown', onKey);
  }, [transaction, onClose]);

  if (!transaction) return null;

  const submit = async (e) => {
    e.preventDefault();
    setSubmitting(true);
    try {
      const res = await api.post('/admin/transactions/return', {
        transactionId: transaction.id,
        condition,
      });

      const fine = res.data.penaltyApplied;
      toast.success(
        fine > 0
          ? `Returned. Fine of $${fine.toFixed(2)} applied to ${transaction.userName}.`
          : `'${transaction.bookName}' returned. No fine.`
      );
      onSuccess();
    } catch (err) {
      toast.error(apiErrorMessage(err, 'Could not process the return.'));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div
      className="fixed inset-0 bg-slate-900/50 flex items-center justify-center p-4 z-50"
      role="dialog"
      aria-modal="true"
      aria-labelledby="return-title"
    >
      <div className="bg-white rounded-lg shadow-xl w-full max-w-lg">
        <div className="p-6 border-b border-slate-200 flex justify-between items-center">
          <h2 id="return-title" className="text-xl font-serif font-bold text-slate-900 flex items-center gap-2">
            <BookDown className="h-5 w-5 text-amber-600" /> Check In
          </h2>
          <button onClick={onClose} aria-label="Close Check In Modal" className="text-slate-400 hover:text-slate-600 text-2xl leading-none">
            &times;
          </button>
        </div>

        <form onSubmit={submit}>
          <div className="p-6 space-y-6">
            {/* Context, so the librarian can confirm they have the right item */}
            <div className="bg-slate-50 border border-slate-200 rounded-md p-4 text-sm space-y-1">
              <div className="font-semibold text-slate-900">{transaction.bookName}</div>
              <div className="text-slate-600">
                Borrowed by {transaction.userName}
                <span className="font-mono text-xs text-slate-500"> ({transaction.userCode})</span>
              </div>
              {transaction.dueDate && (
                <div className={transaction.overdue ? 'text-amber-700 font-medium' : 'text-slate-600'}>
                  Due {new Date(transaction.dueDate).toLocaleDateString()}
                  {transaction.overdue && ` — ${transaction.daysOverdue} day${transaction.daysOverdue === 1 ? '' : 's'} overdue`}
                </div>
              )}
            </div>

            <fieldset>
              <legend className="text-sm font-medium text-slate-700 mb-3">
                Condition on return
              </legend>
              <div className="grid grid-cols-3 gap-3">
                {CONDITIONS.map((c) => (
                  <button
                    key={c.value}
                    type="button"
                    onClick={() => setCondition(c.value)}
                    aria-pressed={condition === c.value}
                    className={`px-4 py-3 rounded-md border-2 text-sm font-semibold transition-colors ${
                      condition === c.value ? c.on : `bg-white ${c.off}`
                    }`}
                  >
                    {c.label}
                  </button>
                ))}
              </div>
              <p className="text-xs text-slate-500 mt-2">
                {CONDITIONS.find((c) => c.value === condition).hint}
              </p>
            </fieldset>

            {transaction.overdue && (
              <div className="bg-amber-50 border-l-4 border-amber-500 p-3 rounded-r text-sm text-amber-900">
                A late fee applies for {transaction.daysOverdue} overdue
                day{transaction.daysOverdue === 1 ? '' : 's'}. The exact total is calculated on submit.
              </div>
            )}
          </div>

          <div className="p-6 border-t border-slate-200 bg-slate-50 flex justify-end gap-3 rounded-b-lg">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 border border-slate-300 bg-white rounded-md text-slate-700 hover:bg-slate-100 text-sm font-medium"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={submitting}
              className="px-4 py-2 bg-amber-600 text-white rounded-md hover:bg-amber-700 disabled:opacity-40 text-sm font-medium"
            >
              {submitting ? 'Working…' : 'Confirm Return'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default ReturnBookModal;
