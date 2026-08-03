import { useState, useEffect } from 'react';
import api from '../../services/api';
import BookCard from '../../components/BookCard';
import AlertBanner from '../../components/AlertBanner';

const UserDashboard = () => {
  const [books, setBooks] = useState([]);
  const [transactions, setTransactions] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchBooks();
    fetchTransactions();
  }, []);

  const fetchTransactions = async () => {
    try {
      const res = await api.get('/user/transactions/active');
      setTransactions(res.data);
    } catch (error) {
      console.error('Failed to fetch transactions', error);
    }
  };

  const fetchBooks = async () => {
    try {
      const res = await api.get('/public/books');
      setBooks(res.data);
    } catch (error) {
      console.error('Failed to fetch books', error);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-12">
      
      {/* Header Section */}
      <section>
        <h1 className="text-3xl font-serif font-bold text-slate-900">The Quiet Reading Room</h1>
        <p className="text-slate-500 mt-2">Welcome back. Explore our curated collections below.</p>
      </section>

      {/* Phase 4: Active Reading Reminder */}
      <div className="space-y-4">
        {transactions.map(txn => {
           const bookDetails = books.find(b => b.id === txn.bookId);
           return <AlertBanner key={txn.id} transaction={txn} bookDetails={bookDetails} />;
        })}
      </div>
      
      {/* Placeholder for Phase 4: Recently Read */}

      <hr className="border-slate-200" />

      {/* New Collections */}
      <section>
        <h2 className="text-2xl font-serif font-bold text-slate-900 mb-6">New Collections</h2>
        
        {loading ? (
          <div className="text-slate-500">Loading library catalog...</div>
        ) : books.length === 0 ? (
          <div className="bg-white border border-slate-200 rounded-lg p-12 text-center shadow-sm">
            <span className="text-4xl block mb-4">📚</span>
            <h3 className="text-lg font-serif font-bold text-slate-900">No books found</h3>
            <p className="text-slate-500 mt-2">The collection is currently empty.</p>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-2 xl:grid-cols-2 gap-6">
            {books.map((book) => (
              <BookCard key={book.id} book={book} />
            ))}
          </div>
        )}
      </section>
      
    </div>
  );
};

export default UserDashboard;
