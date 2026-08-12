import { useState, useEffect } from 'react';
import { BookUp } from 'lucide-react';
import api from '../services/api';
import { apiErrorMessage } from '../services/errors';
import { useToast } from './ui/ToastProvider';
import EntityPicker from './ui/EntityPicker';

/**
 * Issue-a-book flow. Both fields are searchable pickers, so no identifier is
 * ever typed by hand.
 */
const IssueBookModal = ({ isOpen, onClose, onSuccess, initialBook = null }) => {
  const [book, setBook] = useState(initialBook);
  const [user, setUser] = useState(null);
  const [submitting, setSubmitting] = useState(false);
  const toast = useToast();

  useEffect(() => {
    if (isOpen) {
      setBook(initialBook);
      setUser(null);
    }
  }, [isOpen, initialBook]);
  
  useEffect(() => {
    const onKey = (e) => e.key === 'Escape' && onClose();
    if (isOpen) document.addEventListener('keydown', onKey);
    return () => document.removeEventListener('keydown', onKey);
  }, [isOpen, onClose]);

  if (!isOpen) return null;

  const willQueue = book && book.availableCopies <= 0;

  const submit = async (e) => {
    e.preventDefault();
    if (!book || !user) return;

    setSubmitting(true);
    try {
      const res = await api.post('/admin/transactions/issue', {
        bookId: book.id,
        userId: user.userId,          // the LIB-XXXX member code, per TransactionRequest
      });

      toast.success(
        res.data.status === 'BOOKED_IN_QUEUE'
          ? `No copies free — ${user.name} is queued at position ${res.data.queueSequence}.`
          : `'${book.name}' issued to ${user.name}. Due ${new Date(res.data.dueDate).toLocaleDateString()}.`
      );
      onSuccess();
    } catch (err) {
      toast.error(apiErrorMessage(err, 'Could not issue the book.'));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div
      className="fixed inset-0 bg-slate-900/50 flex items-center justify-center p-4 z-50"
      role="dialog"
      aria-modal="true"
      aria-labelledby="issue-title"
    >
      <div className="bg-white rounded-lg shadow-xl w-full max-w-lg">
        <div className="p-6 border-b border-slate-200 flex justify-between items-center">
          <h2 id="issue-title" className="text-xl font-serif font-bold text-slate-900 flex items-center gap-2">
            <BookUp className="h-5 w-5 text-emerald-700" /> Issue a Book
          </h2>
          <button onClick={onClose} aria-label="Close Issue Book Modal" className="text-slate-400 hover:text-slate-600 text-2xl leading-none">
            &times;
          </button>
        </div>

        <form onSubmit={submit}>
          <div className="p-6 space-y-5">
            <EntityPicker
              label="Book"
              endpoint="/public/books/search"
              placeholder="Search by title, author, or ISBN…"
              value={book}
              onChange={setBook}
              renderChosen={(b) => `${b.name} — ${b.author}`}
              renderOption={(b) => (
                <div className="flex justify-between items-center gap-3">
                  <div className="min-w-0">
                    <div className="font-medium text-slate-900 truncate">{b.name}</div>
                    <div className="text-xs text-slate-500 truncate">
                      {b.author} · {b.isbn}
                    </div>
                  </div>
                  <span className={`text-xs font-medium whitespace-nowrap ${b.availableCopies > 0 ? 'text-emerald-700' : 'text-amber-700'}`}>
                    {b.availableCopies > 0 ? `${b.availableCopies} free` : 'Queue'}
                  </span>
                </div>
              )}
            />

            <EntityPicker
              label="Patron"
              endpoint="/admin/users/search"
              placeholder="Search by phone, member ID, name, or email…"
              value={user}
              onChange={setUser}
              renderChosen={(u) => `${u.name} (${u.userId})`}
              renderOption={(u) => (
                <div className="flex justify-between items-center gap-3">
                  <div className="min-w-0">
                    <div className="font-medium text-slate-900 truncate">{u.name}</div>
                    <div className="text-xs text-slate-500 font-mono truncate">
                      {u.userId} · {u.phone}
                    </div>
                  </div>
                  {u.cardExpired && (
                    <span className="text-xs font-medium text-red-600 whitespace-nowrap">Card expired</span>
                  )}
                </div>
              )}
            />

            {willQueue && (
              <div className="bg-amber-50 border-l-4 border-amber-500 p-3 rounded-r text-sm text-amber-900">
                No copies are free. This patron will be added to the reservation queue and
                notified when a copy is returned.
              </div>
            )}

            {user?.cardExpired && (
              <div className="bg-red-50 border-l-4 border-red-500 p-3 rounded-r text-sm text-red-900">
                This library card has expired. Renew it from the User Directory before issuing.
              </div>
            )}
          </div>

          <div className="p-6 border-t border-slate-200 bg-slate-50 flex justify-end gap-3 rounded-b-lg">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 border border-slate-300 bg-white rounded-md text-slate-700 hover:bg-slate-100 text-sm font-medium"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={!book || !user || submitting}
              className="px-4 py-2 bg-emerald-700 text-white rounded-md hover:bg-emerald-800 disabled:opacity-40 text-sm font-medium"
            >
              {submitting ? 'Working…' : willQueue ? 'Add to Queue' : 'Issue Book'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default IssueBookModal;
