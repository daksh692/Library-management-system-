import { Navigate, Outlet } from 'react-router-dom';
import { useContext } from 'react';
import { AuthContext } from '../context/AuthContext';

/**
 * ProtectedRoute component.
 *
 * @param {Object} props.allowedRoles - TODO: Describe allowedRoles
 */
const ProtectedRoute = ({ allowedRoles }) => {
  const { user, loading } = useContext(AuthContext);

  if (loading) {
    return <div className="min-h-screen flex items-center justify-center text-slate-500">Loading...</div>;
  }

  if (!user) {
    return <Navigate to="/login" replace />;
  }

  if (allowedRoles && !allowedRoles.includes(user.role)) {
    return <Navigate to={user.role === 'ROLE_ADMIN' ? '/admin/dashboard' : '/home'} replace />;
  }

  return <Outlet />;
};

export default ProtectedRoute;
