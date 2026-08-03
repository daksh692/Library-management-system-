import { AlertTriangle, Clock } from 'lucide-react';
import { Link } from 'react-router-dom';

const AlertBanner = ({ transaction, bookDetails }) => {
  const isHeld = transaction.status === 'HELD_FOR_PICKUP';
  const isOverdue = transaction.status === 'ISSUED' && new Date(transaction.dueDate) < new Date();
  
  if (!isHeld && !isOverdue && transaction.status !== 'ISSUED') return null;

  return (
    <div className={`border-l-4 p-4 rounded-r-md flex justify-between items-center my-4 shadow-sm ${isOverdue ? 'bg-amber-50 border-amber-500' : isHeld ? 'bg-emerald-50 border-emerald-500' : 'bg-slate-100 border-slate-400'}`}>
      <div className="flex items-center space-x-3">
        {isOverdue || isHeld ? (
          <AlertTriangle className={`h-6 w-6 ${isOverdue ? 'text-amber-700' : 'text-emerald-700'}`} />
        ) : (
          <Clock className="h-6 w-6 text-slate-500" />
        )}
        <div>
          {isHeld ? (
            <p className="text-sm font-sans text-emerald-900">
              Your reserved book <span className="font-semibold">{bookDetails?.name || 'Loading...'}</span> is ready! 
              Please pick it up within 48 hours.
            </p>
          ) : isOverdue ? (
            <p className="text-sm font-sans text-amber-900">
              You have an active return overdue for <span className="font-semibold">{bookDetails?.name || 'Loading...'}</span>!
              Please return it to avoid further penalties.
            </p>
          ) : (
            <p className="text-sm font-sans text-slate-700">
              You are currently reading <span className="font-semibold">{bookDetails?.name || 'Loading...'}</span>. 
              Due back on {new Date(transaction.dueDate).toLocaleDateString()}.
            </p>
          )}
        </div>
      </div>
      {bookDetails && (
        <Link 
          to={`/book/${bookDetails.id}`} 
          className={`text-xs font-semibold underline transition-colors ${isOverdue ? 'text-amber-900 hover:text-amber-700' : isHeld ? 'text-emerald-900 hover:text-emerald-700' : 'text-slate-700 hover:text-slate-500'}`}
        >
          View Details
        </Link>
      )}
    </div>
  );
};

export default AlertBanner;
