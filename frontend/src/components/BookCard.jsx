import { Link } from 'react-router-dom';

const BookCard = ({ book }) => {
  return (
    <div className="flex bg-white border border-slate-100 rounded-lg overflow-hidden shadow-sm hover:shadow-md transition-shadow duration-200 h-48">
      {/* Left: Book Cover Placeholder */}
      <div className="w-1/3 bg-slate-200 flex-shrink-0 flex items-center justify-center border-r border-slate-100">
        {book.photoUrl ? (
          <img src={book.photoUrl} alt={book.name} className="w-full h-full object-cover" />
        ) : (
          <span className="text-slate-400 font-serif text-xs px-2 text-center">No Cover</span>
        )}
      </div>
      
      {/* Right: Clean Meta Details */}
      <div className="w-2/3 p-4 flex flex-col justify-between">
        <div>
          <span className="text-xs font-semibold tracking-wider text-emerald-700 uppercase">{book.genre}</span>
          <h3 className="font-serif text-lg font-bold text-slate-900 mt-1 leading-snug line-clamp-1" title={book.name}>
            {book.name}
          </h3>
          <p className="text-sm text-slate-500 font-sans line-clamp-1">by {book.author}</p>
          <p className="text-xs text-slate-600 mt-2 line-clamp-2">{book.shortDescription}</p>
        </div>
        
        <div className="mt-2 flex items-center justify-between border-t border-slate-50 pt-2">
          <span className="text-xs font-mono font-semibold bg-slate-50 text-slate-700 px-2 py-1 rounded">
            Loc: {book.location}
          </span>
          <Link
            to={`/book/${book.id}`}
            className="text-xs font-medium text-emerald-700 hover:text-emerald-900 transition-colors"
          >
            Details &rarr;
          </Link>
        </div>
      </div>
    </div>
  );
};

export default BookCard;
