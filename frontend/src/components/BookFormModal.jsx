import { useState, useEffect } from 'react';
import api from '../services/api';
import { apiErrorMessage } from '../services/errors';

/**
 * BookFormModal component.
 *
 * @param {Object} props.isOpen - TODO: Describe isOpen
 * @param {Object} props.onClose - TODO: Describe onClose
 * @param {Object} props.onSuccess - TODO: Describe onSuccess
 * @param {Object} props.editBook - TODO: Describe editBook
 */
const BookFormModal = ({ isOpen, onClose, onSuccess, editBook }) => {
  const [formData, setFormData] = useState({
    isbn: '',
    name: '',
    author: '',
    shortDescription: '',
    longDescription: '',
    genre: '',
    photoUrl: '',
    location: '',
    totalCopies: 1,
    price: 50.0
  });

  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (editBook) {
      setFormData({
        isbn: editBook.isbn || '',
        name: editBook.name || '',
        author: editBook.author || '',
        shortDescription: editBook.shortDescription || '',
        longDescription: editBook.longDescription || '',
        genre: editBook.genre || '',
        photoUrl: editBook.photoUrl || '',
        location: editBook.location || '',
        totalCopies: editBook.totalCopies || 1,
        price: editBook.price || 50.0
      });
    } else {
      setFormData({
        isbn: '', name: '', author: '', shortDescription: '', longDescription: '',
        genre: '', photoUrl: '', location: '', totalCopies: 1, price: 50.0
      });
    }
  }, [editBook, isOpen]);

  useEffect(() => {
    const onKey = (e) => e.key === 'Escape' && onClose();
    if (isOpen) document.addEventListener('keydown', onKey);
    return () => document.removeEventListener('keydown', onKey);
  }, [isOpen, onClose]);

  if (!isOpen) return null;

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({ ...prev, [name]: value }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      if (editBook) {
        await api.put(`/admin/books/${editBook.id}`, formData);
      } else {
        await api.post('/admin/books', formData);
      }
      onSuccess();
    } catch (err) {
      console.error(err);
      setError(apiErrorMessage(err, 'Failed to save the book.'));
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-slate-900 bg-opacity-50 flex items-center justify-center p-4 z-50"
         role="dialog"
         aria-modal="true"
         aria-labelledby="book-modal-title"
    >
      <div className="bg-white rounded-lg shadow-xl w-full max-w-2xl max-h-[90vh] flex flex-col">
        <div className="p-6 border-b border-slate-200 flex justify-between items-center">
          <h2 id="book-modal-title" className="text-xl font-serif font-bold text-slate-900">
            {editBook ? 'Edit Book' : 'Add New Book'}
          </h2>
          <button onClick={onClose} aria-label="Close Book Modal" className="text-slate-400 hover:text-slate-600">&times;</button>
        </div>
        
        <div className="p-6 overflow-y-auto">
          {error && <div className="mb-4 bg-red-50 text-red-700 p-3 rounded">{error}</div>}
          
          <form id="bookForm" onSubmit={handleSubmit} className="space-y-4">
            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium text-slate-700">ISBN *</label>
                <input required name="isbn" value={formData.isbn} onChange={handleChange} className="mt-1 w-full border border-slate-300 rounded-md px-3 py-2" />
              </div>
              <div>
                <label className="block text-sm font-medium text-slate-700">Name *</label>
                <input required name="name" value={formData.name} onChange={handleChange} className="mt-1 w-full border border-slate-300 rounded-md px-3 py-2" />
              </div>
              <div>
                <label className="block text-sm font-medium text-slate-700">Author *</label>
                <input required name="author" value={formData.author} onChange={handleChange} className="mt-1 w-full border border-slate-300 rounded-md px-3 py-2" />
              </div>
              <div>
                <label className="block text-sm font-medium text-slate-700">Genre *</label>
                <input required name="genre" value={formData.genre} onChange={handleChange} className="mt-1 w-full border border-slate-300 rounded-md px-3 py-2" />
              </div>
              <div className="col-span-2">
                <label className="block text-sm font-medium text-slate-700">Short Description *</label>
                <input required name="shortDescription" value={formData.shortDescription} onChange={handleChange} className="mt-1 w-full border border-slate-300 rounded-md px-3 py-2" />
              </div>
              <div className="col-span-2">
                <label className="block text-sm font-medium text-slate-700">Long Description</label>
                <textarea name="longDescription" value={formData.longDescription} onChange={handleChange} rows="3" className="mt-1 w-full border border-slate-300 rounded-md px-3 py-2"></textarea>
              </div>
              <div className="col-span-2">
                <label className="block text-sm font-medium text-slate-700">Photo URL</label>
                <input name="photoUrl" value={formData.photoUrl} onChange={handleChange} className="mt-1 w-full border border-slate-300 rounded-md px-3 py-2" />
              </div>
              <div>
                <label className="block text-sm font-medium text-slate-700">Location (e.g. A-04-S2) *</label>
                <input required name="location" value={formData.location} onChange={handleChange} className="mt-1 w-full border border-slate-300 rounded-md px-3 py-2" />
              </div>
              <div>
                <label className="block text-sm font-medium text-slate-700">Total Copies *</label>
                <input type="number" min="0" required name="totalCopies" value={formData.totalCopies} onChange={handleChange} className="mt-1 w-full border border-slate-300 rounded-md px-3 py-2" />
              </div>
              <div>
                <label className="block text-sm font-medium text-slate-700">Price *</label>
                <input type="number" min="0" step="0.01" required name="price" value={formData.price} onChange={handleChange} className="mt-1 w-full border border-slate-300 rounded-md px-3 py-2" />
              </div>
            </div>
          </form>
        </div>
        
        <div className="p-6 border-t border-slate-200 flex justify-end space-x-3">
          <button onClick={onClose} className="px-4 py-2 border border-slate-300 rounded-md text-slate-700 hover:bg-slate-50">Cancel</button>
          <button form="bookForm" type="submit" disabled={loading} className="px-4 py-2 bg-emerald-700 text-white rounded-md hover:bg-emerald-800 disabled:opacity-50">
            {loading ? 'Saving...' : 'Save Book'}
          </button>
        </div>
      </div>
    </div>
  );
};

export default BookFormModal;
