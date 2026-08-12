import { useState, useEffect } from 'react';
import api from '../services/api';
import { apiErrorMessage } from '../services/errors';

const ManageCopiesModal = ({ isOpen, onClose, onSuccess, book }) => {
  const [copies, setCopies] = useState(0);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (book) {
      setCopies(book.totalCopies);
    }
  }, [book, isOpen]);

  if (!isOpen || !book) return null;

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      // We need to send all required fields for a PUT request according to BookDto.
      // So we take the existing book data and just update the totalCopies.
      const updatedBookData = {
        isbn: book.isbn,
        name: book.name,
        author: book.author,
        shortDescription: book.shortDescription,
        longDescription: book.longDescription,
        genre: book.genre,
        photoUrl: book.photoUrl,
        location: book.location,
        price: book.price,
        totalCopies: parseInt(copies, 10)
      };

      await api.put(`/admin/books/${book.id}`, updatedBookData);
      onSuccess();
    } catch (err) {
      setError(apiErrorMessage(err, 'Failed to update the copy count.'));
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-slate-900 bg-opacity-50 flex items-center justify-center p-4 z-50">
      <div className="bg-white rounded-lg shadow-xl w-full max-w-sm flex flex-col">
        <div className="p-6 border-b border-slate-200 flex justify-between items-center">
          <h2 className="text-xl font-serif font-bold text-slate-900">
            Manage Copies
          </h2>
          <button onClick={onClose} className="text-slate-400 hover:text-slate-600">&times;</button>
        </div>
        
        <div className="p-6">
          {error && <div className="mb-4 bg-red-50 text-red-700 p-3 rounded text-sm">{error}</div>}
          <p className="text-sm text-slate-500 mb-4">
            Adjust the total number of copies for <strong>{book.name}</strong>. Current total: {book.totalCopies} (Available: {book.availableCopies})
          </p>
          <form id="copiesForm" onSubmit={handleSubmit}>
            <label className="block text-sm font-medium text-slate-700">Total Copies</label>
            <input 
              type="number" 
              min="0" 
              required 
              value={copies} 
              onChange={(e) => setCopies(e.target.value)} 
              className="mt-1 w-full border border-slate-300 rounded-md px-3 py-2 focus:ring-amber-500 focus:border-amber-500" 
            />
          </form>
        </div>
        
        <div className="p-6 border-t border-slate-200 flex justify-end space-x-3 bg-slate-50">
          <button onClick={onClose} className="px-4 py-2 border border-slate-300 bg-white rounded-md text-slate-700 hover:bg-slate-50">Cancel</button>
          <button form="copiesForm" type="submit" disabled={loading} className="px-4 py-2 bg-amber-600 text-white rounded-md hover:bg-amber-700 disabled:opacity-50">
            {loading ? 'Updating...' : 'Update Copies'}
          </button>
        </div>
      </div>
    </div>
  );
};

export default ManageCopiesModal;
