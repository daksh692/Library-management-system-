import { useState, useEffect } from 'react';
import api from '../../services/api';
import { apiErrorMessage } from '../../services/errors';
import TransactionManager from './TransactionManager';
import UserManager from './UserManager';
import BookFormModal from '../../components/BookFormModal';
import ManageCopiesModal from '../../components/ManageCopiesModal';
import IssueBookModal from '../../components/IssueBookModal';
import ConfirmDialog from '../../components/ui/ConfirmDialog';
import { useToast } from '../../components/ui/ToastProvider';

const AdminDashboard = () => {
  const [books, setBooks] = useState([]);
  const [loading, setLoading] = useState(true);
  const [searchType, setSearchType] = useState('books'); // 'books' or 'users'
  const [searchQuery, setSearchQuery] = useState('');

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
  }, []);

  const fetchBooks = async () => {
    try {
      const res = await api.get('/public/books');
      setBooks(res.data);
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

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
        </div>
        {searchType !== 'transactions' && (
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
              <tr><td colSpan="5" className="px-6 py-4 text-center text-slate-500">Loading...</td></tr>
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
      </div>
      ) : searchType === 'users' ? (
        <UserManager searchQuery={searchQuery} />
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
