import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { Sparkles, History, Library, AlertCircle } from 'lucide-react';
import api from '../../services/api';
import { apiErrorMessage } from '../../services/errors';
import BookCard from '../../components/BookCard';
import AlertBanner from '../../components/AlertBanner';
import { BookCardSkeleton } from '../../components/ui/Skeleton';

/** Small horizontal-scroll tile used by history and recommendations. */
const BookTile = ({ book, caption }) => (
  <Link
    to={`/book/${book.id}`}
    className="flex-shrink-0 w-36 group"
  >
    <div className="h-52 bg-slate-200 rounded-md overflow-hidden shadow-sm group-hover:shadow-md transition-shadow">
      {book.photoUrl ? (
        <img src={book.photoUrl} alt={book.name} className="w-full h-full object-cover" />
      ) : (
        <div className="w-full h-full flex items-center justify-center text-slate-400 text-xs font-serif px-2 text-center">
          {book.name}
        </div>
      )}
    </div>
    <p className="text-sm font-medium text-slate-900 mt-2 line-clamp-2 leading-snug">{book.name}</p>
    <p className="text-xs text-slate-500 line-clamp-1">{book.author}</p>
    {caption && <p className="text-xs text-slate-400 mt-0.5">{caption}</p>}
  </Link>
);

const Section = ({ icon: Icon, title, subtitle, children }) => (
  <section>
    <div className="flex items-baseline gap-2 mb-5">
      <Icon className="h-5 w-5 text-emerald-700 self-center" />
      <h2 className="text-2xl font-serif font-bold text-slate-900">{title}</h2>
      {subtitle && <span className="text-sm text-slate-500">{subtitle}</span>}
    </div>
    {children}
  </section>
);

const UserDashboard = () => {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    api.get('/user/dashboard')
      .then((res) => setData(res.data))
      .catch((err) => setError(apiErrorMessage(err, 'Could not load your dashboard.')))
      .finally(() => setLoading(false));
  }, []);

  if (loading) {
    return (
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-8">
        <div className="h-8 w-72 bg-slate-200 rounded animate-pulse" />
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          {Array.from({ length: 4 }).map((_, i) => <BookCardSkeleton key={i} />)}
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="max-w-3xl mx-auto px-4 py-16 text-center">
        <AlertCircle className="h-10 w-10 text-amber-500 mx-auto mb-4" />
        <p className="text-slate-700">{error}</p>
      </div>
    );
  }

  const { activeLoans, history, recommendations, newArrivals, outstandingFines } = data;
  const genres = [...new Set(history.map((h) => h.bookGenre).filter(Boolean))];

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-12">

      <header>
        <h1 className="text-3xl font-serif font-bold text-slate-900">The Quiet Reading Room</h1>
        <p className="text-slate-500 mt-2">Welcome back. Here is what is waiting for you.</p>
      </header>

      {/* Outstanding fines take priority over everything else */}
      {outstandingFines > 0 && (
        <div className="bg-red-50 border-l-4 border-red-500 p-4 rounded-r-md flex items-center gap-3">
          <AlertCircle className="h-5 w-5 text-red-600 flex-shrink-0" />
          <p className="text-sm text-red-900">
            You have <span className="font-semibold">${outstandingFines.toFixed(2)}</span> in
            unpaid fines. Please settle them at the front desk.
          </p>
        </div>
      )}

      {/* Active loans */}
      {activeLoans.length > 0 && (
        <div className="space-y-3">
          {activeLoans.map((txn) => <AlertBanner key={txn.id} transaction={txn} />)}
        </div>
      )}

      {/* Reading history — PRD asks for up to 4 */}
      {history.length > 0 && (
        <>
          <hr className="border-slate-200" />
          <Section icon={History} title="Recently Read">
            <div className="flex gap-5 overflow-x-auto pb-2">
              {history.map((txn) => (
                <BookTile
                  key={txn.id}
                  book={{
                    id: txn.bookId,
                    name: txn.bookName,
                    author: txn.bookAuthor,
                    photoUrl: txn.bookPhotoUrl,
                  }}
                  caption={txn.returnDate
                    ? `Returned ${new Date(txn.returnDate).toLocaleDateString()}`
                    : null}
                />
              ))}
            </div>
          </Section>
        </>
      )}

      {/* Recommendations — PRD asks for 5 */}
      {recommendations.length > 0 && (
        <>
          <hr className="border-slate-200" />
          <Section
            icon={Sparkles}
            title="Curated For You"
            subtitle={genres.length ? `Based on ${genres.slice(0, 2).join(' and ')}` : 'Fresh picks'}
          >
            <div className="flex gap-5 overflow-x-auto pb-2">
              {recommendations.map((book) => <BookTile key={book.id} book={book} />)}
            </div>
          </Section>
        </>
      )}

      {/* New arrivals — PRD asks for a 4x2 grid, newest first */}
      <hr className="border-slate-200" />
      <Section icon={Library} title="New Arrivals">
        {newArrivals.length === 0 ? (
          <div className="bg-white border border-slate-200 rounded-lg p-12 text-center shadow-sm">
            <span className="text-4xl block mb-4">📚</span>
            <h3 className="text-lg font-serif font-bold text-slate-900">The shelves are empty</h3>
            <p className="text-slate-500 mt-2 text-sm">
              No books have been catalogued yet. Check back soon.
            </p>
          </div>
        ) : (
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
            {newArrivals.slice(0, 8).map((book) => <BookCard key={book.id} book={book} />)}
          </div>
        )}
      </Section>

    </div>
  );
};

export default UserDashboard;
