import { useState, useEffect } from 'react';
import api from '../../services/api';
import UserFormModal from '../../components/UserFormModal';
import UserTransactionsModal from '../../components/UserTransactionsModal';

const UserManager = ({ searchQuery }) => {
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);

  // Modals
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [isTxnModalOpen, setIsTxnModalOpen] = useState(false);
  const [selectedUser, setSelectedUser] = useState(null);

  useEffect(() => {
    fetchUsers();
  }, []);

  const fetchUsers = async () => {
    setLoading(true);
    try {
      const res = await api.get('/admin/users');
      setUsers(res.data);
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const handleAddUser = () => {
    setSelectedUser(null);
    setIsFormOpen(true);
  };

  const handleEditUser = (user) => {
    setSelectedUser(user);
    setIsFormOpen(true);
  };

  const handleDeleteUser = async (user) => {
    if (window.confirm(`Are you sure you want to remove ${user.name}? This will mark their account as deleted.`)) {
      try {
        await api.delete(`/admin/users/${user.id}`);
        fetchUsers();
      } catch (err) {
        alert('Failed to delete user');
      }
    }
  };

  const handleViewTransactions = (user) => {
    setSelectedUser(user);
    setIsTxnModalOpen(true);
  };

  const onFormSuccess = () => {
    setIsFormOpen(false);
    fetchUsers();
  };

  const filteredUsers = users.filter(u => 
    !u.deleted && (
      u.name?.toLowerCase().includes(searchQuery.toLowerCase()) || 
      u.userId?.toLowerCase().includes(searchQuery.toLowerCase()) ||
      u.email?.toLowerCase().includes(searchQuery.toLowerCase())
    )
  );

  return (
    <div className="space-y-6">
      <div className="flex justify-end">
        <button 
          onClick={handleAddUser}
          className="bg-indigo-600 text-white px-4 py-2 rounded-md shadow hover:bg-indigo-700 transition-colors"
        >
          + Add New User
        </button>
      </div>

      <div className="bg-white border border-slate-200 rounded-lg shadow-sm overflow-hidden">
        <table className="min-w-full divide-y divide-slate-200">
          <thead className="bg-slate-50">
            <tr>
              <th className="px-6 py-3 text-left text-xs font-medium text-slate-500 uppercase tracking-wider">User ID</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-slate-500 uppercase tracking-wider">Name & Contact</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-slate-500 uppercase tracking-wider">Role</th>
              <th className="px-6 py-3 text-right text-xs font-medium text-slate-500 uppercase tracking-wider">Actions</th>
            </tr>
          </thead>
          <tbody className="bg-white divide-y divide-slate-200">
            {loading ? (
              <tr><td colSpan="4" className="px-6 py-4 text-center text-slate-500">Loading users...</td></tr>
            ) : filteredUsers.length === 0 ? (
              <tr><td colSpan="4" className="px-6 py-4 text-center text-slate-500">No active users found.</td></tr>
            ) : filteredUsers.map((user) => (
              <tr key={user.id} className="hover:bg-slate-50">
                <td className="px-6 py-4 whitespace-nowrap text-sm font-mono text-slate-600">{user.userId}</td>
                <td className="px-6 py-4">
                  <div className="text-sm font-semibold text-slate-900">{user.name}</div>
                  <div className="text-xs text-slate-500">{user.email} | {user.phone}</div>
                </td>
                <td className="px-6 py-4 whitespace-nowrap">
                  <span className={`px-2 inline-flex text-xs leading-5 font-semibold rounded-full ${user.role === 'ROLE_ADMIN' ? 'bg-purple-100 text-purple-800' : 'bg-green-100 text-green-800'}`}>
                    {user.role === 'ROLE_ADMIN' ? 'Admin' : 'Patron'}
                  </span>
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-right text-sm font-medium space-x-4">
                  <button onClick={() => handleViewTransactions(user)} className="text-blue-600 hover:text-blue-900">View Issued Books</button>
                  <button onClick={() => handleEditUser(user)} className="text-emerald-600 hover:text-emerald-900">Edit</button>
                  <button onClick={() => handleDeleteUser(user)} className="text-red-600 hover:text-red-900">Remove</button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <UserFormModal 
        isOpen={isFormOpen} 
        onClose={() => setIsFormOpen(false)} 
        onSuccess={onFormSuccess} 
        editUser={selectedUser} 
      />
      
      <UserTransactionsModal 
        isOpen={isTxnModalOpen} 
        onClose={() => setIsTxnModalOpen(false)} 
        user={selectedUser} 
      />
    </div>
  );
};

export default UserManager;
