import { useState, useEffect } from 'react';
import api from '../../services/api';

const TransactionManager = () => {
  const [activeTxns, setActiveTxns] = useState([]);
  const [loading, setLoading] = useState(true);
  
  // Issue form
  const [issueBookId, setIssueBookId] = useState('');
  const [issueUserId, setIssueUserId] = useState('');
  
  // Return form
  const [returnTxnId, setReturnTxnId] = useState('');
  const [returnCondition, setReturnCondition] = useState('GOOD');

  const [message, setMessage] = useState({ text: '', type: '' });

  useEffect(() => {
    fetchActiveTransactions();
  }, []);

  const fetchActiveTransactions = async () => {
    try {
      const res = await api.get('/admin/transactions/active');
      setActiveTxns(res.data);
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const handleIssue = async (e) => {
    e.preventDefault();
    setMessage({ text: '', type: '' });
    try {
      await api.post('/admin/transactions/issue', { bookId: issueBookId, userId: issueUserId });
      setIssueBookId('');
      setIssueUserId('');
      fetchActiveTransactions();
      setMessage({ text: 'Book issued/waitlisted successfully!', type: 'success' });
    } catch (err) {
      setMessage({ text: err.response?.data?.message || 'Failed to issue book. Check User ID and Book ID.', type: 'error' });
    }
  };

  const handleReturn = async (e) => {
    e.preventDefault();
    setMessage({ text: '', type: '' });
    try {
      const res = await api.post('/admin/transactions/return', { transactionId: returnTxnId, condition: returnCondition });
      setReturnTxnId('');
      setReturnCondition('GOOD');
      fetchActiveTransactions();
      setMessage({ text: `Book returned. Penalty applied: $${res.data.penaltyApplied}`, type: 'success' });
    } catch (err) {
      setMessage({ text: err.response?.data?.message || 'Failed to return book. Check Transaction ID.', type: 'error' });
    }
  };

  return (
    <div className="space-y-8">
      {message.text && (
        <div className={`p-4 rounded-md ${message.type === 'success' ? 'bg-emerald-50 text-emerald-800' : 'bg-red-50 text-red-800'}`}>
          {message.text}
        </div>
      )}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
        
        {/* Issue Book Panel */}
        <div className="bg-white border border-slate-200 p-6 rounded-lg shadow-sm">
          <h2 className="text-xl font-serif font-bold text-slate-900 mb-4">Issue Book</h2>
          <form onSubmit={handleIssue} className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-slate-700">Book ID</label>
              <input type="text" required value={issueBookId} onChange={e => setIssueBookId(e.target.value)}
                className="w-full px-3 py-2 border border-slate-300 rounded-md mt-1" />
            </div>
            <div>
              <label className="block text-sm font-medium text-slate-700">User ID (LIB-...)</label>
              <input type="text" required value={issueUserId} onChange={e => setIssueUserId(e.target.value)}
                className="w-full px-3 py-2 border border-slate-300 rounded-md mt-1" />
            </div>
            <button type="submit" className="w-full bg-emerald-700 text-white py-2 rounded-md hover:bg-emerald-800">Issue / Waitlist</button>
          </form>
        </div>

        {/* Return Book Panel */}
        <div className="bg-white border border-slate-200 p-6 rounded-lg shadow-sm">
          <h2 className="text-xl font-serif font-bold text-slate-900 mb-4">Return Book</h2>
          <form onSubmit={handleReturn} className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-slate-700">Transaction ID</label>
              <input type="text" required value={returnTxnId} onChange={e => setReturnTxnId(e.target.value)}
                className="w-full px-3 py-2 border border-slate-300 rounded-md mt-1" />
            </div>
            <div>
              <label className="block text-sm font-medium text-slate-700">Condition</label>
              <select value={returnCondition} onChange={e => setReturnCondition(e.target.value)}
                className="w-full px-3 py-2 border border-slate-300 rounded-md mt-1">
                <option value="GOOD">Good</option>
                <option value="DAMAGED">Damaged (Penalty applies)</option>
                <option value="LOST">Lost (Penalty applies)</option>
              </select>
            </div>
            <button type="submit" className="w-full bg-amber-600 text-white py-2 rounded-md hover:bg-amber-700">Process Return</button>
          </form>
        </div>
      </div>

      {/* Active Transactions Table */}
      <div className="bg-white border border-slate-200 rounded-lg shadow-sm overflow-hidden">
        <div className="p-4 bg-slate-50 border-b border-slate-200">
          <h3 className="font-semibold text-slate-700">Active Transactions Queue</h3>
        </div>
        <table className="min-w-full divide-y divide-slate-200">
          <thead className="bg-white">
            <tr>
              <th className="px-6 py-3 text-left text-xs font-medium text-slate-500 uppercase">Txn ID</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-slate-500 uppercase">Status</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-slate-500 uppercase">Issue Date</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-slate-500 uppercase">Due Date</th>
            </tr>
          </thead>
          <tbody className="bg-white divide-y divide-slate-200">
            {loading ? <tr><td colSpan="4" className="text-center p-4">Loading...</td></tr> :
              activeTxns.map(txn => (
                <tr key={txn.id}>
                  <td className="px-6 py-4 text-sm font-mono text-slate-600">{txn.id}</td>
                  <td className="px-6 py-4 text-sm">
                    <span className={`px-2 py-1 rounded text-xs font-semibold ${
                      txn.status === 'ISSUED' ? 'bg-blue-100 text-blue-800' :
                      txn.status === 'HELD_FOR_PICKUP' ? 'bg-emerald-100 text-emerald-800' :
                      'bg-amber-100 text-amber-800'
                    }`}>
                      {txn.status}
                    </span>
                  </td>
                  <td className="px-6 py-4 text-sm text-slate-600">{txn.issueDate ? new Date(txn.issueDate).toLocaleDateString() : 'N/A'}</td>
                  <td className="px-6 py-4 text-sm text-slate-600">{txn.dueDate ? new Date(txn.dueDate).toLocaleDateString() : 'N/A'}</td>
                </tr>
              ))
            }
          </tbody>
        </table>
      </div>
    </div>
  );
};

export default TransactionManager;
