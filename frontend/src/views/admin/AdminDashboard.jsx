import { useState, useEffect } from 'react';
import api from '../../services/api';
import { apiErrorMessage } from '../../services/errors';
import TransactionManager from './TransactionManager';
import UserManager from './UserManager';
import PaymentManager from './PaymentManager';
import BookFormModal from '../../components/BookFormModal';
import ManageCopiesModal from '../../components/ManageCopiesModal';
import IssueBookModal from '../../components/IssueBookModal';
import ConfirmDialog from '../../components/ui/ConfirmDialog';
import { useToast } from '../../components/ui/ToastProvider';

/**
 * AdminDashboard view component.
 */
const AdminDashboard = () => {
  const [books, setBooks] = useState([]);
  const [loading, setLoading] = useState(true);
  const [metrics, setMetrics] = useState(null);
  const [searchType, setSearchType] = useState('books'); // 'books' or 'users'
  const [searchQuery, setSearchQuery] = useState('');
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);

  // Modal States
  const [isBookModalOpen, setIsBookModalOpen] = useState(false);
  const [isCopiesModalOpen, setIsCopiesModalOpen] = useState(false);
  const [selectedBook, setSelectedBook] = useState(null);
  const [issueForBook, setIssueForBook] = useState(null);
  
  // Confirm Delete State
  const [deleteBookConfirm, setDeleteBookConfirm] = useState(null);

  const toast = useToast();

  useEffect(() => {
    fetchBooks();
    fetchMetrics();
  }, [page]);

  const fetchMetrics = async () => {
    try {
      const res = await api.get('/admin/metrics');
      setMetrics(res.data);
    } catch (err) {
      console.error(err);
    }
  };

  const fetchBooks = async () => {
    setLoading(true);
    try {
      // If we have a query, use search endpoint, otherwise paginated get
      const url = searchQuery 
        ? `/public/books/search?query=${encodeURIComponent(searchQuery)}`
        : `/public/books?page=${page}&size=20`;
      const res = await api.get(url);
      setBooks(res.data.content || res.data);
      if (res.data.totalPages) {
        setTotalPages(res.data.totalPages);
      } else {
        setTotalPages(1); // search endpoint returns unpaginated list
      }
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  // Debounce search
  useEffect(() => {
    const timer = setTimeout(() => {
      setPage(0); // reset page
      fetchBooks();
    }, 300);
    return () => clearTimeout(timer);
  }, [searchQuery]);

  /**
 * handleAddBook view component.
 */
const handleAddBook = () => {
    setSelectedBook(null);
    setIsBookModalOpen(true);
  };

  const handleEditBook = (book) => {
    setSelectedBook(book);
    setIsBookModalOpen(true);
  };

  const handleManageCopies = (book) => {
    setSelectedBook(book);
    setIsCopiesModalOpen(true);
  };

  const handleDeleteBook = async () => {
    if (!deleteBookConfirm) return;
    try {
      await api.delete(`/admin/books/${deleteBookConfirm.id}`);
      toast.success(`Deleted book "${deleteBookConfirm.name}"`);
      fetchBooks();
    } catch (err) {
      toast.error(apiErrorMessage(err, 'Failed to delete book.'));
    } finally {
      setDeleteBookConfirm(null);
    }
  };

  /**
 * onModalSuccess view component.
 */
const onModalSuccess = () => {
    setIsBookModalOpen(false);
    setIsCopiesModalOpen(false);
    setSelectedBook(null);
    fetchBooks();
  };

  const filteredBooks = books.filter(b => 
    b.name?.toLowerCase().includes(searchQuery.toLowerCase()) || 
    b.author?.toLowerCase().includes(searchQuery.toLowerCase()) ||
    b.isbn?.toLowerCase().includes(searchQuery.toLowerCase())
  );

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      
      <div className="flex justify-between items-end mb-8">
        <div>
          <h1 className="text-3xl font-serif font-bold text-slate-900">Admin Command Center</h1>
          <p className="text-slate-500 mt-2">Manage library inventory, users, and transactions.</p>
        </div>
        <button 
          onClick={handleAddBook}
          className="bg-slate-900 text-white px-4 py-2 rounded-md shadow hover:bg-slate-800 transition-colors"
        >
          + Add New Book
        </button>
      </div>

      {/* Metric Cards */}
      {metrics && (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4 mb-8">
          <div className="bg-white overflow-hidden shadow rounded-lg border border-slate-200">
            <div className="px-4 py-5 sm:p-6">
              <dt className="text-sm font-medium text-slate-500 truncate">Total Books</dt>
              <dd className="mt-1 text-3xl font-semibold text-slate-900">{metrics.totalBooks}</dd>
            </div>
          </div>
          <div className="bg-white overflow-hidden shadow rounded-lg border border-slate-200">
            <div className="px-4 py-5 sm:p-6">
              <dt className="text-sm font-medium text-slate-500 truncate">Active Loans</dt>
              <dd className="mt-1 text-3xl font-semibold text-slate-900">{metrics.activeLoans}</dd>
            </div>
          </div>
          <div className="bg-white overflow-hidden shadow rounded-lg border border-slate-200">
            <div className="px-4 py-5 sm:p-6">
              <dt className="text-sm font-medium text-slate-500 truncate">Overdue Items</dt>
              <dd className="mt-1 text-3xl font-semibold text-red-600">{metrics.overdueItems}</dd>
            </div>
          </div>
          <div className="bg-white overflow-hidden shadow rounded-lg border border-slate-200">
            <div className="px-4 py-5 sm:p-6">
              <dt className="text-sm font-medium text-slate-500 truncate">Unpaid Fines</dt>
              <dd className="mt-1 text-3xl font-semibold text-amber-600">{metrics.unpaidFines}</dd>
            </div>
          </div>
        </div>
      )}

      {/* Dual Search Toggle */}
      <div className="bg-white border border-slate-200 p-4 rounded-lg shadow-sm mb-8 flex space-x-4">
        <div className="flex bg-slate-100 p-1 rounded-md">
          <button 
            className={`px-4 py-1.5 text-sm font-medium rounded ${searchType === 'books' ? 'bg-white shadow text-slate-900' : 'text-slate-500 hover:text-slate-700'}`}
            onClick={() => setSearchType('books')}
          >
            Books Inventory
          </button>
          <button 
            className={`px-4 py-1.5 text-sm font-medium rounded ${searchType === 'users' ? 'bg-white shadow text-slate-900' : 'text-slate-500 hover:text-slate-700'}`}
            onClick={() => setSearchType('users')}
          >
            User Directory
          </button>
          <button 
            className={`px-4 py-1.5 text-sm font-medium rounded ${searchType === 'transactions' ? 'bg-white shadow text-slate-900' : 'text-slate-500 hover:text-slate-700'}`}
            onClick={() => setSearchType('transactions')}
          >
            Transactions
          </button>
          <button 
            className={`px-4 py-1.5 text-sm font-medium rounded ${searchType === 'payments' ? 'bg-white shadow text-slate-900' : 'text-slate-500 hover:text-slate-700'}`}
            onClick={() => setSearchType('payments')}
          >
            Payments
          </button>
        </div>
        {searchType !== 'transactions' && searchType !== 'payments' && (
          <input
            type="text"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder={`Search ${searchType}...`}
            className="flex-1 border border-slate-300 rounded-md px-4 py-2 focus:ring-emerald-500 focus:border-emerald-500"
          />
        )}
      </div>

      {searchType === 'transactions' ? (
        <TransactionManager />
      ) : searchType === 'books' ? (
        <div className="bg-white border border-slate-200 rounded-lg shadow-sm overflow-x-auto">
        <table className="min-w-full divide-y divide-slate-200">
          <thead className="bg-slate-50">
            <tr>
              <th className="px-6 py-3 text-left text-xs font-medium text-slate-500 uppercase tracking-wider">ISBN</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-slate-500 uppercase tracking-wider">Title & Author</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-slate-500 uppercase tracking-wider hidden md:table-cell">Location</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-slate-500 uppercase tracking-wider">Stock</th>
              <th className="px-6 py-3 text-right text-xs font-medium text-slate-500 uppercase tracking-wider">Actions</th>
            </tr>
          </thead>
          <tbody className="bg-white divide-y divide-slate-200">
            {loading ? (
              [...Array(5)].map((_, i) => (
                <tr key={i} className="animate-pulse">
                  <td className="px-6 py-4 hidden md:table-cell"><div className="h-4 bg-slate-200 rounded w-24"></div></td>
                  <td className="px-6 py-4"><div className="h-4 bg-slate-200 rounded w-48 mb-2"></div><div className="h-3 bg-slate-200 rounded w-32"></div></td>
                  <td className="px-6 py-4 hidden md:table-cell"><div className="h-6 bg-slate-200 rounded w-16"></div></td>
                  <td className="px-6 py-4"><div className="h-4 bg-slate-200 rounded w-16"></div></td>
                  <td className="px-6 py-4 text-right"><div className="h-4 bg-slate-200 rounded w-32 ml-auto"></div></td>
                </tr>
              ))
            ) : filteredBooks.length === 0 ? (
              <tr><td colSpan="5" className="px-6 py-4 text-center text-slate-500">No books found.</td></tr>
            ) : filteredBooks.map((book) => (
              <tr key={book.id} className="hover:bg-slate-50">
                <td className="px-6 py-4 whitespace-nowrap text-sm font-mono text-slate-600 hidden md:table-cell">{book.isbn}</td>
                <td className="px-6 py-4">
                  <div className="text-sm font-semibold text-slate-900">{book.name}</div>
                  <div className="text-sm text-slate-500">{book.author}</div>
                  <div className="text-xs text-slate-400 font-mono md:hidden mt-1">{book.isbn}</div>
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-sm font-mono text-slate-600 hidden md:table-cell">
                  <span className="bg-slate-100 px-2 py-1 rounded border border-slate-200">{book.location}</span>
                </td>
                <td className="px-6 py-4 whitespace-nowrap">
                  <div className="flex items-center">
                    <span className={`h-2.5 w-2.5 rounded-full mr-2 ${book.availableCopies > 0 ? 'bg-emerald-500' : 'bg-amber-500'}`}></span>
                    <span className="text-sm text-slate-900">{book.availableCopies} / {book.totalCopies}</span>
                  </div>
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                  <button
                    onClick={() => setIssueForBook(book)}
                    disabled={book.availableCopies <= 0 && book.totalCopies === 0}
                    className="text-blue-600 hover:text-blue-900 mr-4 disabled:opacity-40"
                  >
                    {book.availableCopies > 0 ? 'Issue' : 'Reserve'}
                  </button>
                  <button onClick={() => handleEditBook(book)} className="text-emerald-600 hover:text-emerald-900 mr-4">Edit</button>
                  <button onClick={() => handleManageCopies(book)} className="text-amber-600 hover:text-amber-900 mr-4">Manage Copies</button>
                  <button onClick={() => setDeleteBookConfirm(book)} className="text-red-600 hover:text-red-900">Delete</button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        
        {/* Pagination Controls */}
        {!searchQuery && searchType === 'books' && totalPages > 1 && (
          <div className="bg-white px-4 py-3 border-t border-slate-200 flex items-center justify-between sm:px-6">
            <div className="flex-1 flex justify-between sm:hidden">
              <button
                onClick={() => setPage(p => Math.max(0, p - 1))}
                disabled={page === 0}
                className="relative inline-flex items-center px-4 py-2 border border-slate-300 text-sm font-medium rounded-md text-slate-700 bg-white hover:bg-slate-50 disabled:opacity-50"
              >
                Previous
              </button>
              <button
                onClick={() => setPage(p => Math.min(totalPages - 1, p + 1))}
                disabled={page >= totalPages - 1}
                className="ml-3 relative inline-flex items-center px-4 py-2 border border-slate-300 text-sm font-medium rounded-md text-slate-700 bg-white hover:bg-slate-50 disabled:opacity-50"
              >
                Next
              </button>
            </div>
            <div className="hidden sm:flex-1 sm:flex sm:items-center sm:justify-between">
              <div>
                <p className="text-sm text-slate-700">
                  Showing page <span className="font-medium">{page + 1}</span> of <span className="font-medium">{totalPages}</span>
                </p>
              </div>
              <div>
                <nav className="relative z-0 inline-flex rounded-md shadow-sm -space-x-px" aria-label="Pagination">
                  <button
                    onClick={() => setPage(p => Math.max(0, p - 1))}
                    disabled={page === 0}
                    className="relative inline-flex items-center px-2 py-2 rounded-l-md border border-slate-300 bg-white text-sm font-medium text-slate-500 hover:bg-slate-50 disabled:opacity-50"
                  >
                    Previous
                  </button>
                  <button
                    onClick={() => setPage(p => Math.min(totalPages - 1, p + 1))}
                    disabled={page >= totalPages - 1}
                    className="relative inline-flex items-center px-2 py-2 rounded-r-md border border-slate-300 bg-white text-sm font-medium text-slate-500 hover:bg-slate-50 disabled:opacity-50"
                  >
                    Next
                  </button>
                </nav>
              </div>
            </div>
          </div>
        )}
      </div>
      ) : searchType === 'users' ? (
        <UserManager searchQuery={searchQuery} />
      ) : searchType === 'transactions' ? (
        <TransactionManager />
      ) : searchType === 'payments' ? (
        <PaymentManager />
      ) : null}

      {/* Modals */}
      <BookFormModal 
        isOpen={isBookModalOpen} 
        onClose={() => setIsBookModalOpen(false)} 
        onSuccess={onModalSuccess} 
        editBook={selectedBook} 
      />
      <ManageCopiesModal 
        isOpen={isCopiesModalOpen} 
        onClose={() => setIsCopiesModalOpen(false)} 
        onSuccess={onModalSuccess} 
        book={selectedBook} 
      />
      <IssueBookModal
        isOpen={!!issueForBook}
        onClose={() => setIssueForBook(null)}
        onSuccess={() => { setIssueForBook(null); fetchBooks(); }}
        initialBook={issueForBook}
      />
      <ConfirmDialog
        isOpen={!!deleteBookConfirm}
        title="Delete Book"
        message={`Are you sure you want to delete "${deleteBookConfirm?.name}"? This action cannot be undone.`}
        confirmLabel="Delete Book"
        onConfirm={handleDeleteBook}
        onCancel={() => setDeleteBookConfirm(null)}
      />
    </div>
  );
};

export default AdminDashboard;
