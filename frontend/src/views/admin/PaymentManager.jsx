import { useState, useEffect } from 'react';
import api from '../../services/api';
import { apiErrorMessage } from '../../services/errors';
import { useToast } from '../../components/ui/ToastProvider';
import ConfirmDialog from '../../components/ui/ConfirmDialog';

/**
 * PaymentManager view component.
 */
const PaymentManager = () => {
  const [payments, setPayments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [filter, setFilter] = useState('ALL'); // 'ALL', 'PENDING', 'PAID'
  const [userSearch, setUserSearch] = useState('');
  const [settleConfirm, setSettleConfirm] = useState(null);
  const toast = useToast();

  useEffect(() => {
    fetchPayments();
  }, [filter]);

  const fetchPayments = async () => {
    try {
      setLoading(true);
      const endpoint = filter === 'ALL' 
        ? '/admin/payments' 
        : `/admin/payments?status=${filter}`;
      const res = await api.get(endpoint);
      setPayments(res.data);
    } catch (err) {
      toast.error(apiErrorMessage(err, 'Failed to fetch payments.'));
    } finally {
      setLoading(false);
    }
  };

  const handleSettle = async () => {
    if (!settleConfirm) return;
    try {
      await api.post(`/admin/payments/${settleConfirm.id}/settle`);
      toast.success('Payment marked as paid successfully.');
      fetchPayments();
    } catch (err) {
      toast.error(apiErrorMessage(err, 'Failed to settle payment.'));
    } finally {
      setSettleConfirm(null);
    }
  };

  const filteredPayments = payments.filter(p => {
    if (!userSearch) return true;
    const q = userSearch.toLowerCase();
    return (p.userName?.toLowerCase().includes(q) || p.userId?.toLowerCase().includes(q));
  });

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <h2 className="text-xl font-bold text-slate-900">Payment Ledger</h2>
        
        <div className="flex flex-wrap items-center gap-4">
          <input
            type="text"
            placeholder="Search by user name or ID..."
            value={userSearch}
            onChange={(e) => setUserSearch(e.target.value)}
            className="border border-slate-300 rounded-md px-3 py-1 text-sm focus:ring-emerald-500 focus:border-emerald-500 min-w-[250px]"
          />
          <div className="flex space-x-2">
            {['ALL', 'PENDING', 'PAID'].map((status) => (
              <button
                key={status}
                onClick={() => setFilter(status)}
                className={`px-3 py-1 text-sm rounded-md transition-colors ${
                  filter === status
                    ? 'bg-emerald-600 text-white'
                    : 'bg-white text-slate-600 hover:bg-slate-50 border border-slate-200'
                }`}
              >
                {status}
              </button>
            ))}
          </div>
        </div>
      </div>

      <div className="bg-white rounded-lg border border-slate-200 overflow-hidden shadow-sm">
        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-slate-200">
            <thead className="bg-slate-50">
              <tr>
                <th className="px-6 py-3 text-left text-xs font-medium text-slate-500 uppercase tracking-wider">User</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-slate-500 uppercase tracking-wider">Type / Reason</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-slate-500 uppercase tracking-wider">Amount</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-slate-500 uppercase tracking-wider">Status</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-slate-500 uppercase tracking-wider">Date</th>
                <th className="px-6 py-3 text-right text-xs font-medium text-slate-500 uppercase tracking-wider">Actions</th>
              </tr>
            </thead>
            <tbody className="bg-white divide-y divide-slate-200">
              {loading ? (
                <tr>
                  <td colSpan="6" className="px-6 py-12 text-center text-slate-500">Loading...</td>
                </tr>
              ) : filteredPayments.length === 0 ? (
                <tr>
                  <td colSpan="6" className="px-6 py-12 text-center text-slate-500">No payments found.</td>
                </tr>
              ) : (
                filteredPayments.map((p) => (
                  <tr key={p.id} className="hover:bg-slate-50">
                    <td className="px-6 py-4 whitespace-nowrap">
                      <div className="text-sm font-semibold text-slate-900">{p.userName}</div>
                      <div className="text-xs text-slate-500 font-mono mt-0.5">{p.userId}</div>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <div className="text-sm text-slate-900">{p.reason}</div>
                      <div className="text-xs text-slate-500">{p.type}</div>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-slate-900 font-semibold">
                      ${p.amount.toFixed(2)}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <span className={`px-2 inline-flex text-xs leading-5 font-semibold rounded-full ${
                        p.status === 'PAID' ? 'bg-emerald-100 text-emerald-800' : 'bg-amber-100 text-amber-800'
                      }`}>
                        {p.status}
                      </span>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-slate-500">
                      {new Date(p.createdAt).toLocaleDateString()}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                      {p.status === 'PENDING' && (
                        <button
                          onClick={() => setSettleConfirm(p)}
                          className="text-emerald-600 hover:text-emerald-900 bg-emerald-50 hover:bg-emerald-100 px-3 py-1 rounded transition-colors"
                        >
                          Mark Paid
                        </button>
                      )}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      <ConfirmDialog
        isOpen={!!settleConfirm}
        title="Confirm Payment"
        message={`Mark $${settleConfirm?.amount.toFixed(2)} for "${settleConfirm?.reason}" as paid?`}
        confirmLabel="Mark Paid"
        onConfirm={handleSettle}
        onCancel={() => setSettleConfirm(null)}
      />
    </div>
  );
};

export default PaymentManager;
