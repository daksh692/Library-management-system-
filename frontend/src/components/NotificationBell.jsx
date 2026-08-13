import { useState, useEffect, useRef, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { Bell, CheckCheck } from 'lucide-react';
import api from '../services/api';

const ICONS = {
  READY_FOR_PICKUP: '📗',
  HOLD_EXPIRED:     '⌛',
  DUE_SOON:         '📅',
  OVERDUE:          '⚠️',
  PENALTY:          '💰',
  ISSUED:           '📖',
  QUEUED:           '🔖',
};

/** Relative time without pulling in a date library. */
const ago = (iso) => {
  const seconds = Math.floor((Date.now() - new Date(iso)) / 1000);
  if (seconds < 60) return 'just now';
  const mins = Math.floor(seconds / 60);
  if (mins < 60) return `${mins}m ago`;
  const hours = Math.floor(mins / 60);
  if (hours < 24) return `${hours}h ago`;
  const days = Math.floor(hours / 24);
  return days < 7 ? `${days}d ago` : new Date(iso).toLocaleDateString();
};

const NotificationBell = () => {
  const [items, setItems] = useState([]);
  const [unread, setUnread] = useState(0);
  const [open, setOpen] = useState(false);
  const panelRef = useRef(null);
  const navigate = useNavigate();

  const load = useCallback(async () => {
    try {
      const res = await api.get('/user/notifications');
      setItems(res.data.items);
      setUnread(res.data.unread);
    } catch {
      // Silent: a failing bell must never disrupt the page.
    }
  }, []);

  useEffect(() => {
    load();
    const timer = setInterval(load, 60_000);   // poll once a minute
    return () => clearInterval(timer);
  }, [load]);

  useEffect(() => {
    const onClickAway = (e) => {
      if (panelRef.current && !panelRef.current.contains(e.target)) setOpen(false);
    };
    document.addEventListener('mousedown', onClickAway);
    return () => document.removeEventListener('mousedown', onClickAway);
  }, []);

  const openItem = async (n) => {
    if (!n.read) {
      await api.post(`/user/notifications/${n.id}/read`).catch(() => {});
      load();
    }
    if (n.link) { setOpen(false); navigate(n.link); }
  };

  const markAll = async () => {
    await api.post('/user/notifications/read-all').catch(() => {});
    load();
  };

  return (
    <div ref={panelRef} className="relative">
      <button
        onClick={() => setOpen((o) => !o)}
        className="relative p-2 text-slate-500 hover:text-emerald-700 hover:bg-emerald-50 rounded-full transition-colors"
        aria-label={unread > 0 ? `Notifications, ${unread} unread` : 'Notifications'}
        aria-expanded={open}
      >
        <Bell className="h-5 w-5" />
        {unread > 0 && (
          <span className="absolute top-1 right-1 min-w-[18px] h-[18px] px-1 bg-amber-500 text-white text-[10px] font-bold rounded-full flex items-center justify-center">
            {unread > 9 ? '9+' : unread}
          </span>
        )}
      </button>

      {open && (
        <div className="absolute right-0 mt-2 w-96 max-w-[calc(100vw-2rem)] bg-white border border-slate-200 rounded-lg shadow-xl z-50 overflow-hidden">
          <div className="px-4 py-3 border-b border-slate-200 flex justify-between items-center bg-slate-50">
            <h3 className="font-semibold text-slate-900 text-sm">Notifications</h3>
            {unread > 0 && (
              <button
                onClick={markAll}
                className="text-xs text-emerald-700 hover:text-emerald-900 inline-flex items-center gap-1 font-medium"
              >
                <CheckCheck className="h-3.5 w-3.5" /> Mark all read
              </button>
            )}
          </div>

          <div className="max-h-96 overflow-y-auto">
            {items.length === 0 ? (
              <div className="px-4 py-12 text-center">
                <Bell className="h-8 w-8 text-slate-200 mx-auto mb-3" />
                <p className="text-sm text-slate-500">Nothing here yet.</p>
                <p className="text-xs text-slate-400 mt-1">
                  We'll let you know when a reserved book is ready.
                </p>
              </div>
            ) : items.map((n) => (
              <button
                key={n.id}
                onClick={() => openItem(n)}
                className={`w-full text-left px-4 py-3 border-b border-slate-100 last:border-0 hover:bg-slate-50 transition-colors flex gap-3 ${
                  !n.read ? 'bg-emerald-50/40' : ''
                }`}
              >
                <span className="text-lg leading-none mt-0.5" aria-hidden="true">
                  {ICONS[n.type] || '🔔'}
                </span>
                <span className="min-w-0 flex-1">
                  <span className={`block text-sm ${!n.read ? 'font-semibold text-slate-900' : 'text-slate-700'}`}>
                    {n.title}
                  </span>
                  <span className="block text-xs text-slate-600 mt-0.5 leading-snug">
                    {n.message}
                  </span>
                  <span className="block text-xs text-slate-400 mt-1">{ago(n.createdAt)}</span>
                </span>
                {!n.read && (
                  <span className="h-2 w-2 bg-emerald-500 rounded-full flex-shrink-0 mt-2" aria-hidden="true" />
                )}
              </button>
            ))}
          </div>
        </div>
      )}
    </div>
  );
};

export default NotificationBell;
