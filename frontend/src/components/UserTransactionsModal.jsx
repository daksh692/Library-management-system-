import { useState, useEffect } from 'react';
import api from '../services/api';

/**
 * UserTransactionsModal component.
 *
 * @param {Object} props.isOpen - TODO: Describe isOpen
 * @param {Object} props.onClose - TODO: Describe onClose
 * @param {Object} props.user - TODO: Describe user
 */
const UserTransactionsModal = ({ isOpen, onClose, user }) => {
  const [transactions, setTransactions] = useState([]);
  const [books, setBooks] = useState({});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    if (isOpen && user) {
      fetchUserTransactions();
    }
  }, [isOpen, user]);

  const fetchUserTransactions = async () => {
    setLoading(true);
    setError('');
    try {
      const res = await api.get(`/admin/users/${user.userId}/transactions`);
      setTransactions(res.data);
      
      // Fetch book details for each transaction to display the title
      if (res.data.length > 0) {
        const bookIds = [...new Set(res.data.map(t => t.bookId))];
        const booksMap = {};
        for (const bookId of bookIds) {
          try {
            const bookRes = await api.get(`/public/books/${bookId}`);
            booksMap[bookId] = bookRes.data;
          } catch (e) {
            console.error("Failed to fetch book", bookId);
          }
        }
        setBooks(booksMap);
      }
    } catch (err) {
      console.error(err);
      setError('Failed to load transactions for this user.');
    } finally {
      setLoading(false);
    }
  };

  if (!isOpen || !user) return null;

  return (
    <div className="fixed inset-0 bg-slate-900 bg-opacity-50 flex items-center justify-center p-4 z-50">
      <div className="bg-white rounded-lg shadow-xl w-full max-w-3xl flex flex-col max-h-[90vh]">
        <div className="p-6 border-b border-slate-200 flex justify-between items-center bg-slate-50">
          <div>
            <h2 className="text-xl font-serif font-bold text-slate-900">
              Active Transactions
            </h2>
            <p className="text-sm text-slate-500 mt-1">Showing issued books for {user.name} ({user.userId})</p>
          </div>
          <button onClick={onClose} className="text-slate-400 hover:text-slate-600">&times;</button>
        </div>
        
        <div className="p-6 overflow-y-auto">
          {error && <div className="mb-4 bg-red-50 text-red-700 p-3 rounded text-sm">{error}</div>}
          
          {loading ? (
            <div className="text-center py-8 text-slate-500">Loading records...</div>
          ) : transactions.length === 0 ? (
            <div className="text-center py-8 text-slate-500 bg-slate-50 rounded-lg border border-slate-200">
              This user has no active issued or waitlisted books.
            </div>
          ) : (
            <div className="overflow-hidden border border-slate-200 rounded-lg">
              <table className="min-w-full divide-y divide-slate-200">
                <thead className="bg-slate-50">
                  <tr>
                    <th className="px-4 py-3 text-left text-xs font-medium text-slate-500 uppercase tracking-wider">Book</th>
                    <th className="px-4 py-3 text-left text-xs font-medium text-slate-500 uppercase tracking-wider">Status</th>
                    <th className="px-4 py-3 text-left text-xs font-medium text-slate-500 uppercase tracking-wider">Issue Date</th>
                    <th className="px-4 py-3 text-left text-xs font-medium text-slate-500 uppercase tracking-wider">Due Date</th>
                  </tr>
                </thead>
                <tbody className="bg-white divide-y divide-slate-200">
                  {transactions.map(txn => {
                    const book = books[txn.bookId];
                    return (
                      <tr key={txn.id} className="hover:bg-slate-50">
                        <td className="px-4 py-3">
                          <div className="text-sm font-semibold text-slate-900">{book ? book.name : 'Unknown Book'}</div>
                          <div className="text-xs text-slate-500 font-mono">{txn.bookId}</div>
                        </td>
                        <td className="px-4 py-3 text-sm">
                          <span className={`px-2 py-1 rounded text-xs font-semibold ${
                            txn.status === 'ISSUED' ? 'bg-blue-100 text-blue-800' :
                            txn.status === 'HELD_FOR_PICKUP' ? 'bg-emerald-100 text-emerald-800' :
                            'bg-amber-100 text-amber-800'
                          }`}>
                            {txn.status}
                          </span>
                        </td>
                        <td className="px-4 py-3 text-sm text-slate-600">
                          {txn.issueDate ? new Date(txn.issueDate).toLocaleDateString() : '-'}
                        </td>
                        <td className="px-4 py-3 text-sm text-slate-600">
                          {txn.dueDate ? new Date(txn.dueDate).toLocaleDateString() : '-'}
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          )}
        </div>
        
        <div className="p-4 border-t border-slate-200 flex justify-end">
          <button onClick={onClose} className="px-6 py-2 border border-slate-300 bg-white rounded-md text-slate-700 hover:bg-slate-50">
            Close
          </button>
        </div>
      </div>
    </div>
  );
};

export default UserTransactionsModal;
