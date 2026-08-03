import { useState, useEffect } from 'react';
import api from '../services/api';

const UserFormModal = ({ isOpen, onClose, onSuccess, editUser }) => {
  const [formData, setFormData] = useState({
    userId: '',
    name: '',
    email: '',
    phone: '',
    role: 'ROLE_USER',
    password: ''
  });

  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (editUser) {
      setFormData({
        userId: editUser.userId || '',
        name: editUser.name || '',
        email: editUser.email || '',
        phone: editUser.phone || '',
        role: editUser.role || 'ROLE_USER',
        password: '' // Keep empty for edits unless they want to change it
      });
    } else {
      setFormData({
        userId: '', name: '', email: '', phone: '', role: 'ROLE_USER', password: ''
      });
    }
  }, [editUser, isOpen]);

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
      if (editUser) {
        await api.put(`/admin/users/${editUser.id}`, formData);
      } else {
        await api.post('/admin/users', formData);
      }
      onSuccess();
    } catch (err) {
      console.error(err);
      setError(err.response?.data?.message || err.response?.data?.errors?.[0]?.defaultMessage || 'Failed to save user.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-slate-900 bg-opacity-50 flex items-center justify-center p-4 z-50">
      <div className="bg-white rounded-lg shadow-xl w-full max-w-lg flex flex-col">
        <div className="p-6 border-b border-slate-200 flex justify-between items-center">
          <h2 className="text-xl font-serif font-bold text-slate-900">
            {editUser ? 'Edit User' : 'Add New User'}
          </h2>
          <button onClick={onClose} className="text-slate-400 hover:text-slate-600">&times;</button>
        </div>
        
        <div className="p-6 overflow-y-auto">
          {error && <div className="mb-4 bg-red-50 text-red-700 p-3 rounded">{error}</div>}
          
          <form id="userForm" onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-slate-700">User ID (e.g. LIB-2026-001) *</label>
              <input required name="userId" value={formData.userId} onChange={handleChange} disabled={!!editUser} className="mt-1 w-full border border-slate-300 rounded-md px-3 py-2 disabled:bg-slate-100 disabled:text-slate-500" />
            </div>
            <div>
              <label className="block text-sm font-medium text-slate-700">Full Name *</label>
              <input required name="name" value={formData.name} onChange={handleChange} className="mt-1 w-full border border-slate-300 rounded-md px-3 py-2" />
            </div>
            <div>
              <label className="block text-sm font-medium text-slate-700">Email Address *</label>
              <input type="email" required name="email" value={formData.email} onChange={handleChange} className="mt-1 w-full border border-slate-300 rounded-md px-3 py-2" />
            </div>
            <div>
              <label className="block text-sm font-medium text-slate-700">Phone Number *</label>
              <input required name="phone" value={formData.phone} onChange={handleChange} className="mt-1 w-full border border-slate-300 rounded-md px-3 py-2" />
            </div>
            <div>
              <label className="block text-sm font-medium text-slate-700">Role *</label>
              <select required name="role" value={formData.role} onChange={handleChange} className="mt-1 w-full border border-slate-300 rounded-md px-3 py-2 bg-white">
                <option value="ROLE_USER">Patron (ROLE_USER)</option>
                <option value="ROLE_ADMIN">Administrator (ROLE_ADMIN)</option>
              </select>
            </div>
            <div>
              <label className="block text-sm font-medium text-slate-700">Password {!editUser && '*'}</label>
              <input type="password" name="password" required={!editUser} value={formData.password} onChange={handleChange} placeholder={editUser ? "Leave blank to keep unchanged" : ""} className="mt-1 w-full border border-slate-300 rounded-md px-3 py-2" />
            </div>
          </form>
        </div>
        
        <div className="p-6 border-t border-slate-200 flex justify-end space-x-3 bg-slate-50">
          <button onClick={onClose} className="px-4 py-2 border border-slate-300 bg-white rounded-md text-slate-700 hover:bg-slate-50">Cancel</button>
          <button form="userForm" type="submit" disabled={loading} className="px-4 py-2 bg-slate-900 text-white rounded-md hover:bg-slate-800 disabled:opacity-50">
            {loading ? 'Saving...' : 'Save User'}
          </button>
        </div>
      </div>
    </div>
  );
};

export default UserFormModal;
