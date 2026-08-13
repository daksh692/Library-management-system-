import { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import api from '../../services/api';
import { apiErrorMessage } from '../../services/errors';
import { ArrowLeft } from 'lucide-react';
import { useToast } from '../../components/ui/ToastProvider';

/**
 * BookDetails view component.
 */
const BookDetails = () => {
  const { id } = useParams();
  const [book, setBook] = useState(null);
  const [loading, setLoading] = useState(true);
  
  const [reserving, setReserving] = useState(false);
  const toast = useToast();
  
  const [related, setRelated] = useState([]);
  const [policy, setPolicy] = useState({ loanPeriodDays: 14 });

  useEffect(() => {
    const fetchBook = async () => {
      try {
        const res = await api.get(`/public/books/${id}`);
        setBook(res.data);
      } catch (err) {
        console.error(err);
      } finally {
        setLoading(false);
      }
    };
    fetchBook();
    
    api.get(`/public/books/${id}/related`, { params: { limit: 6 } })
      .then((res) => setRelated(res.data))
      .catch(() => setRelated([]));
      
    api.get('/public/books/policy')
      .then((r) => setPolicy(r.data))
      .catch(() => {});
  }, [id]);

  const handleReserve = async () => {
    setReserving(true);
    try {
      const res = await api.post('/user/reservations', { bookId: book.id });
      toast.success(`Reserved. You are number ${res.data.queueSequence} in the queue.`);
    } catch (err) {
      toast.error(apiErrorMessage(err, 'Could not reserve this book.'));
    } finally {
      setReserving(false);
    }
  };

  if (loading) return <div className="p-8 text-slate-500">Loading details...</div>;
  if (!book) return <div className="p-8 text-amber-700">Book not found.</div>;

  const isAvailable = book.availableCopies > 0;

  return (
    <div className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <Link to="/home" className="inline-flex items-center text-sm text-slate-500 hover:text-emerald-700 mb-8 transition-colors">
        <ArrowLeft className="h-4 w-4 mr-1" /> Back to Collection
      </Link>

      <div className="bg-white border border-slate-200 rounded-xl shadow-md overflow-hidden flex flex-col md:flex-row min-h-[600px]">
        
        {/* Left: High-Res Cover */}
        <div className="w-full md:w-2/5 bg-slate-100 flex-shrink-0 flex items-center justify-center p-8 border-b md:border-b-0 md:border-r border-slate-200">
          {book.photoUrl ? (
            <img src={book.photoUrl} alt={book.name} className="max-w-full max-h-full object-contain shadow-lg" />
          ) : (
            <span className="text-slate-400 font-serif">No Cover Image</span>
          )}
        </div>

        {/* Right: Metadata */}
        <div className="w-full md:w-3/5 p-8 md:p-12 flex flex-col justify-between">
          <div>
            <div className="flex items-center justify-between mb-2">
              <span className="text-sm font-semibold tracking-wider text-emerald-700 uppercase">
                {book.genre}
              </span>
              <span className="text-xs text-slate-400 font-mono">ISBN: {book.isbn}</span>
            </div>
            
            <h1 className="text-4xl font-serif font-bold text-slate-900 mb-2 leading-tight">
              {book.name}
            </h1>
            <p className="text-xl text-slate-600 font-sans mb-8">by {book.author}</p>
            
            <h3 className="text-sm font-bold text-slate-900 uppercase tracking-wide mb-2">Synopsis</h3>
            <p className="text-slate-600 leading-relaxed mb-8">
              {book.longDescription || book.shortDescription || 'No description available.'}
            </p>
          </div>

          <div className="space-y-6">
            <div className="grid grid-cols-2 gap-4 border-y border-slate-100 py-6">
              <div>
                <p className="text-xs text-slate-500 uppercase tracking-wide">Physical Location</p>
                <p className="font-mono text-slate-900 mt-1">{book.location}</p>
              </div>
              <div>
                <p className="text-xs text-slate-500 uppercase tracking-wide">Max Loan Period</p>
                <p className="font-sans text-slate-900 mt-1">{policy.loanPeriodDays} Days</p>
              </div>
            </div>

            {/* Availability UI */}
            {isAvailable ? (
              <div className="flex items-center justify-between bg-emerald-50 border border-emerald-100 p-4 rounded-lg">
                <div className="flex items-center space-x-2">
                  <span className="h-3 w-3 bg-emerald-500 rounded-full animate-pulse"></span>
                  <span className="text-emerald-800 font-medium">Available Now</span>
                </div>
                <span className="text-sm text-emerald-700">{book.availableCopies} of {book.totalCopies} copies</span>
              </div>
            ) : (
              <div className="bg-amber-50 border border-amber-100 p-4 rounded-lg space-y-3">
                <div className="flex items-center justify-between">
                  <div className="flex items-center space-x-2">
                    <span className="h-3 w-3 bg-amber-500 rounded-full" />
                    <span className="text-amber-800 font-medium">
                      {book.estimatedAvailableOn
                        ? `Unavailable — expected ${new Date(book.estimatedAvailableOn).toLocaleDateString(undefined, { month: 'short', day: 'numeric' })}`
                        : 'Currently Unavailable'}
                    </span>
                  </div>
                  <button
                    onClick={handleReserve}
                    disabled={reserving}
                    className="text-sm font-semibold text-amber-900 bg-white border border-amber-200 px-4 py-2 rounded shadow-sm hover:bg-amber-100 disabled:opacity-40 transition-colors"
                  >
                    {reserving ? 'Reserving…' : 'Reserve Next'}
                  </button>
                </div>
                <p className="text-xs text-amber-700">
                  Reserving adds you to the queue. You will be notified when a copy is ready and
                  will have 48 hours to collect it.
                </p>
              </div>
            )}
          </div>
        </div>

      </div>

      {related.length > 0 && (
        <section className="mt-12">
          <h2 className="text-2xl font-serif font-bold text-slate-900 mb-6">Related Books</h2>
          <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-4">
            {related.map((b) => (
              <Link key={b.id} to={`/book/${b.id}`} className="group">
                <div className="aspect-[2/3] bg-slate-200 rounded-md overflow-hidden shadow-sm group-hover:shadow-md transition-shadow">
                  {b.photoUrl
                    ? <img src={b.photoUrl} alt={b.name} className="w-full h-full object-cover" />
                    : <div className="w-full h-full flex items-center justify-center text-xs text-slate-400 font-serif p-2 text-center">{b.name}</div>}
                </div>
                <p className="text-sm font-medium text-slate-900 mt-2 line-clamp-2 leading-snug">{b.name}</p>
                <p className="text-xs text-slate-500 line-clamp-1">{b.author}</p>
              </Link>
            ))}
          </div>
        </section>
      )}
    </div>
  );
};

export default BookDetails;
