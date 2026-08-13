import { useContext, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { AuthContext } from '../context/AuthContext';
import NotificationBell from './NotificationBell';
import { Search, LogOut, BookOpen } from 'lucide-react';

/**
 * Navbar view component.
 */
const Navbar = () => {
  const { user, logout } = useContext(AuthContext);
  const [query, setQuery] = useState('');
  const navigate = useNavigate();

  const submitSearch = (e) => {
    e.preventDefault();
    const q = query.trim();
    if (q) navigate(`/search?q=${encodeURIComponent(q)}`);
  };

  return (
    <nav className="bg-white border-b border-slate-200 sticky top-0 z-50">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex justify-between h-16 items-center">

          <button
            onClick={() => navigate(user?.role === 'ROLE_ADMIN' ? '/admin/dashboard' : '/home')}
            className="flex items-center flex-shrink-0"
            aria-label="Go to dashboard"
          >
            <BookOpen className="h-8 w-8 text-emerald-700" />
            <span className="ml-2 text-xl font-serif font-bold text-slate-900 tracking-tight">LMS</span>
          </button>

          {user && (
            <form onSubmit={submitSearch} className="hidden md:flex flex-1 max-w-2xl px-8" role="search">
              <div className="relative w-full">
                <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                  <Search className="h-5 w-5 text-slate-400" />
                </div>
                <input
                  type="search"
                  value={query}
                  onChange={(e) => setQuery(e.target.value)}
                  aria-label="Search books by title, genre, or author"
                  className="block w-full pl-10 pr-3 py-2 border border-slate-300 rounded-md leading-5 bg-slate-50 placeholder-slate-500 focus:outline-none focus:bg-white focus:ring-1 focus:ring-emerald-500 focus:border-emerald-500 sm:text-sm transition-colors duration-200"
                  placeholder="Search books, genres, authors…"
                />
              </div>
            </form>
          )}

          <div className="flex items-center space-x-4">
            {user && (
              <>
                <div className="text-sm font-medium text-slate-700 hidden sm:block">
                  {user.name}
                  <span className="text-xs text-slate-500 uppercase ml-1">
                    ({user.role.replace('ROLE_', '')})
                  </span>
                </div>
                {user.role === 'ROLE_USER' && <NotificationBell />}
                <button
                  onClick={logout}
                  aria-label="Log out"
                  className="p-2 text-slate-500 hover:text-amber-700 hover:bg-amber-50 rounded-full transition-colors duration-200"
                >
                  <LogOut className="h-5 w-5" />
                </button>
              </>
            )}
          </div>

        </div>
      </div>
    </nav>
  );
};

export default Navbar;
