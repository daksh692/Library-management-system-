import { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import api from '../../services/api';
import { ArrowLeft } from 'lucide-react';

const BookDetails = () => {
  const { id } = useParams();
  const [book, setBook] = useState(null);
  const [loading, setLoading] = useState(true);

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
  }, [id]);

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
                <p className="font-sans text-slate-900 mt-1">14 Days</p>
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
              <div className="flex items-center justify-between bg-amber-50 border border-amber-100 p-4 rounded-lg">
                <div className="flex items-center space-x-2">
                  <span className="h-3 w-3 bg-amber-500 rounded-full"></span>
                  <span className="text-amber-800 font-medium">Currently Unavailable</span>
                </div>
                <button className="text-sm font-semibold text-amber-900 bg-white border border-amber-200 px-4 py-2 rounded shadow-sm hover:bg-amber-100 transition-colors">
                  Reserve Next
                </button>
              </div>
            )}
          </div>
        </div>

      </div>
    </div>
  );
};

export default BookDetails;
