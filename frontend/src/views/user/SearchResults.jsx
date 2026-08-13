import { useState, useEffect } from 'react';
import { useSearchParams, Link } from 'react-router-dom';
import { SearchX } from 'lucide-react';
import api from '../../services/api';
import { apiErrorMessage } from '../../services/errors';
import BookCard from '../../components/BookCard';
import { BookCardSkeleton } from '../../components/ui/Skeleton';

/**
 * SearchResults view component.
 */
const SearchResults = () => {
  const [params] = useSearchParams();
  const query = params.get('q') || '';

  const [books, setBooks] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    if (!query) { setBooks([]); setLoading(false); return; }

    let cancelled = false;
    setLoading(true);
    setError('');

    api.get('/public/books/search', { params: { query } })
      .then((res) => { if (!cancelled) setBooks(res.data); })
      .catch((err) => { if (!cancelled) setError(apiErrorMessage(err, 'Search failed.')); })
      .finally(() => { if (!cancelled) setLoading(false); });

    return () => { cancelled = true; };
  }, [query]);

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <header className="mb-8">
        <h1 className="text-2xl font-serif font-bold text-slate-900">
          Results for "{query}"
        </h1>
        {!loading && !error && (
          <p className="text-slate-500 mt-1 text-sm">
            {books.length} {books.length === 1 ? 'book' : 'books'} found
          </p>
        )}
      </header>

      {error && (
        <div className="bg-amber-50 border-l-4 border-amber-500 p-4 rounded-r text-amber-900">
          {error}
        </div>
      )}

      {loading ? (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          {Array.from({ length: 4 }).map((_, i) => <BookCardSkeleton key={i} />)}
        </div>
      ) : books.length === 0 && !error ? (
        <div className="bg-white border border-slate-200 rounded-lg p-16 text-center shadow-sm">
          <SearchX className="h-10 w-10 text-slate-300 mx-auto mb-4" />
          <h3 className="text-lg font-serif font-bold text-slate-900">
            Nothing matched "{query}"
          </h3>
          <p className="text-slate-500 mt-2 text-sm">
            Try an author's surname, a genre, or part of the ISBN.
          </p>
          <Link
            to="/home"
            className="inline-block mt-6 text-sm font-medium text-emerald-700 hover:text-emerald-900"
          >
            Back to the collection
          </Link>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          {books.map((book) => <BookCard key={book.id} book={book} />)}
        </div>
      )}
    </div>
  );
};

export default SearchResults;
