import { useState, useEffect, useCallback } from 'react';
import { BookUp, BookDown, HandCoins, Clock, RefreshCw } from 'lucide-react';
import api from '../../services/api';
import { apiErrorMessage } from '../../services/errors';
import { useToast } from '../../components/ui/ToastProvider';
import { TableSkeleton } from '../../components/ui/Skeleton';
import IssueBookModal from '../../components/IssueBookModal';
import ReturnBookModal from '../../components/ReturnBookModal';

/** Human countdown to a hold expiry, e.g. "31h left" or "Expired". */
const holdCountdown = (expiresAt) => {
  if (!expiresAt) return null;
  const ms = new Date(expiresAt) - new Date();
  if (ms <= 0) return { label: 'Expired', urgent: true };
  const hours = Math.floor(ms / 3_600_000);
  if (hours < 1) return { label: `${Math.floor(ms / 60_000)}m left`, urgent: true };
  return { label: `${hours}h left`, urgent: hours < 6 };
};

const STATUS_STYLES = {
  ISSUED:          'bg-blue-100 text-blue-800',
  HELD_FOR_PICKUP: 'bg-emerald-100 text-emerald-800',
  BOOKED_IN_QUEUE: 'bg-amber-100 text-amber-800',
};

const TransactionManager = () => {
  const [transactions, setTransactions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [issueOpen, setIssueOpen] = useState(false);
  const [returnTarget, setReturnTarget] = useState(null);
  const [busyId, setBusyId] = useState(null);
  const toast = useToast();

  const fetchTransactions = useCallback(async () => {
    setLoading(true);
    try {
      const res = await api.get('/admin/transactions/active');
      setTransactions(res.data);
    } catch (err) {
      toast.error(apiErrorMessage(err, 'Could not load transactions.'));
    } finally {
      setLoading(false);
    }
  }, [toast]);

  useEffect(() => { fetchTransactions(); }, [fetchTransactions]);

  const handleHandover = async (txn) => {
    setBusyId(txn.id);
    try {
      await api.post(`/admin/transactions/${txn.id}/handover`);
      toast.success(`'${txn.bookName}' handed to ${txn.userName}.`);
      fetchTransactions();
    } catch (err) {
      toast.error(apiErrorMessage(err, 'Handover failed.'));
    } finally {
      setBusyId(null);
    }
  };

  const handleSettle = async (txn) => {
    setBusyId(txn.id);
    try {
      await api.post(`/admin/transactions/${txn.id}/settle-penalty`);
      toast.success(`Fine of $${txn.penaltyApplied.toFixed(2)} marked as paid.`);
      fetchTransactions();
    } catch (err) {
      toast.error(apiErrorMessage(err, 'Could not settle the fine.'));
    } finally {
      setBusyId(null);
    }
  };

  return (
    <div className="space-y-6">
      {/* Action bar */}
      <div className="flex flex-wrap justify-between items-center gap-3">
        <h2 className="text-xl font-serif font-bold text-slate-900">Active Transactions</h2>
        <div className="flex gap-3">
          <button
            onClick={fetchTransactions}
            className="inline-flex items-center gap-2 px-3 py-2 border border-slate-300 rounded-md text-sm text-slate-700 hover:bg-slate-50"
          >
            <RefreshCw className="h-4 w-4" /> Refresh
          </button>
          <button
            onClick={() => setIssueOpen(true)}
            className="inline-flex items-center gap-2 bg-emerald-700 text-white px-4 py-2 rounded-md text-sm font-medium hover:bg-emerald-800"
          >
            <BookUp className="h-4 w-4" /> Issue a Book
          </button>
        </div>
      </div>

      {/* Table — horizontally scrollable rather than overflowing on mobile */}
      <div className="bg-white border border-slate-200 rounded-lg shadow-sm overflow-x-auto">
        <table className="min-w-full divide-y divide-slate-200">
          <thead className="bg-slate-50">
            <tr>
              <th className="px-6 py-3 text-left text-xs font-medium text-slate-500 uppercase tracking-wider">Book</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-slate-500 uppercase tracking-wider">Borrower</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-slate-500 uppercase tracking-wider">Status</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-slate-500 uppercase tracking-wider">Dates</th>
              <th className="px-6 py-3 text-right text-xs font-medium text-slate-500 uppercase tracking-wider">Actions</th>
            </tr>
          </thead>
          <tbody className="bg-white divide-y divide-slate-200">
            {loading ? (
              <TableSkeleton rows={4} cols={5} />
            ) : transactions.length === 0 ? (
              <tr>
                <td colSpan="5" className="px-6 py-16 text-center">
                  <span className="text-4xl block mb-3">📖</span>
                  <p className="font-serif font-bold text-slate-900">Nothing on loan right now</p>
                  <p className="text-sm text-slate-500 mt-1">
                    Use "Issue a Book" above to check something out.
                  </p>
                </td>
              </tr>
            ) : transactions.map((txn) => {
              const countdown = holdCountdown(txn.holdExpiresAt);
              const busy = busyId === txn.id;

              return (
                <tr key={txn.id} className={`hover:bg-slate-50 ${txn.overdue ? 'bg-amber-50/40' : ''}`}>
                  {/* Book — title and ISBN, not a hex id */}
                  <td className="px-6 py-4">
                    <div className="text-sm font-semibold text-slate-900">{txn.bookName}</div>
                    <div className="text-xs text-slate-500 font-mono">
                      {txn.bookIsbn} {txn.bookLocation && `· ${txn.bookLocation}`}
                    </div>
                  </td>

                  {/* Borrower */}
                  <td className="px-6 py-4">
                    <div className="text-sm text-slate-900">{txn.userName}</div>
                    <div className="text-xs text-slate-500 font-mono">{txn.userCode}</div>
                  </td>

                  {/* Status */}
                  <td className="px-6 py-4">
                    <span className={`px-2 py-1 rounded text-xs font-semibold ${STATUS_STYLES[txn.status] || 'bg-slate-100 text-slate-700'}`}>
                      {txn.status.replace(/_/g, ' ')}
                    </span>

                    {txn.status === 'BOOKED_IN_QUEUE' && (
                      <div className="text-xs text-slate-500 mt-1">Position {txn.queueSequence}</div>
                    )}

                    {countdown && (
                      <div className={`text-xs mt-1 inline-flex items-center gap-1 ${countdown.urgent ? 'text-red-600 font-medium' : 'text-slate-500'}`}>
                        <Clock className="h-3 w-3" /> {countdown.label}
                      </div>
                    )}

                    {txn.overdue && (
                      <div className="text-xs text-amber-700 font-medium mt-1">
                        {txn.daysOverdue} day{txn.daysOverdue === 1 ? '' : 's'} overdue
                      </div>
                    )}
                  </td>

                  {/* Dates */}
                  <td className="px-6 py-4 text-sm text-slate-600">
                    {txn.issueDate && <div>Out: {new Date(txn.issueDate).toLocaleDateString()}</div>}
                    {txn.dueDate && (
                      <div className={txn.overdue ? 'text-amber-700 font-medium' : ''}>
                        Due: {new Date(txn.dueDate).toLocaleDateString()}
                      </div>
                    )}
                    {!txn.issueDate && !txn.dueDate && <span className="text-slate-400">—</span>}
                  </td>

                  {/* Status-aware actions */}
                  <td className="px-6 py-4 text-right whitespace-nowrap">
                    <div className="inline-flex gap-3">
                      {txn.status === 'ISSUED' && (
                        <button
                          onClick={() => setReturnTarget(txn)}
                          disabled={busy}
                          className="inline-flex items-center gap-1 text-sm font-medium text-amber-700 hover:text-amber-900 disabled:opacity-40"
                        >
                          <BookDown className="h-4 w-4" /> Return
                        </button>
                      )}

                      {txn.status === 'HELD_FOR_PICKUP' && (
                        <button
                          onClick={() => handleHandover(txn)}
                          disabled={busy || countdown?.label === 'Expired'}
                          className="inline-flex items-center gap-1 text-sm font-medium text-emerald-700 hover:text-emerald-900 disabled:opacity-40"
                        >
                          <BookUp className="h-4 w-4" /> {busy ? 'Working…' : 'Hand Over'}
                        </button>
                      )}

                      {txn.penaltyApplied > 0 && !txn.penaltyPaid && (
                        <button
                          onClick={() => handleSettle(txn)}
                          disabled={busy}
                          className="inline-flex items-center gap-1 text-sm font-medium text-red-600 hover:text-red-800 disabled:opacity-40"
                        >
                          <HandCoins className="h-4 w-4" /> Settle ${txn.penaltyApplied.toFixed(2)}
                        </button>
                      )}
                    </div>
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>

      <IssueBookModal
        isOpen={issueOpen}
        onClose={() => setIssueOpen(false)}
        onSuccess={() => { setIssueOpen(false); fetchTransactions(); }}
      />

      <ReturnBookModal
        transaction={returnTarget}
        onClose={() => setReturnTarget(null)}
        onSuccess={() => { setReturnTarget(null); fetchTransactions(); }}
      />
    </div>
  );
};

export default TransactionManager;
