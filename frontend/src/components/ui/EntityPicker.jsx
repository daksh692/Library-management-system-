import { useState, useEffect, useRef, useCallback } from 'react';
import { Search, X, Check } from 'lucide-react';
import api from '../../services/api';
import { apiErrorMessage } from '../../services/errors';

/**
 * Debounced searchable picker for a backend collection.
 *
 * @param {object}   props
 * @param {string}   props.label        field label
 * @param {string}   props.endpoint     e.g. '/admin/users/search' — called with ?query=
 * @param {Function} props.renderOption (item) => ReactNode for the dropdown row
 * @param {Function} props.renderChosen (item) => string shown once selected
 * @param {Function} props.onChange     (item|null) => void
 * @param {object}   [props.value]      currently selected item
 * @param {string}   [props.placeholder]
 */
const EntityPicker = ({
  label,
  endpoint,
  renderOption,
  renderChosen,
  onChange,
  value,
  placeholder = 'Type to search…',
}) => {
  const [query, setQuery] = useState('');
  const [results, setResults] = useState([]);
  const [open, setOpen] = useState(false);
  const [loading, setLoading] = useState(false);
  const [highlight, setHighlight] = useState(0);
  const [error, setError] = useState('');
  const containerRef = useRef(null);

  // Debounced search
  useEffect(() => {
    if (!query.trim()) {
      setResults([]);
      return;
    }
    const timer = setTimeout(async () => {
      setLoading(true);
      setError('');
      try {
        const res = await api.get(endpoint, { params: { query } });
        setResults(res.data);
        setHighlight(0);
        setOpen(true);
      } catch (err) {
        setError(apiErrorMessage(err, 'Search failed.'));
        setResults([]);
      } finally {
        setLoading(false);
      }
    }, 250);

    return () => clearTimeout(timer);
  }, [query, endpoint]);

  // Close on outside click
  useEffect(() => {
    const onClickAway = (e) => {
      if (containerRef.current && !containerRef.current.contains(e.target)) setOpen(false);
    };
    document.addEventListener('mousedown', onClickAway);
    return () => document.removeEventListener('mousedown', onClickAway);
  }, []);

  const choose = useCallback((item) => {
    onChange(item);
    setQuery('');
    setResults([]);
    setOpen(false);
  }, [onChange]);

  const onKeyDown = (e) => {
    if (!open || results.length === 0) return;
    if (e.key === 'ArrowDown') {
      e.preventDefault();
      setHighlight((h) => (h + 1) % results.length);
    } else if (e.key === 'ArrowUp') {
      e.preventDefault();
      setHighlight((h) => (h - 1 + results.length) % results.length);
    } else if (e.key === 'Enter') {
      e.preventDefault();
      choose(results[highlight]);
    } else if (e.key === 'Escape') {
      setOpen(false);
    }
  };

  // Selected state — show the chosen item as a chip with a clear button
  if (value) {
    return (
      <div>
        <label className="block text-sm font-medium text-slate-700 mb-1">{label}</label>
        <div className="flex items-center justify-between border border-emerald-300 bg-emerald-50 rounded-md px-3 py-2">
          <span className="flex items-center gap-2 text-sm text-emerald-900 min-w-0">
            <Check className="h-4 w-4 flex-shrink-0" />
            <span className="truncate">{renderChosen(value)}</span>
          </span>
          <button
            type="button"
            onClick={() => onChange(null)}
            className="text-emerald-700 hover:text-emerald-900 flex-shrink-0 ml-2"
            aria-label={`Clear selected ${label}`}
          >
            <X className="h-4 w-4" />
          </button>
        </div>
      </div>
    );
  }

  return (
    <div ref={containerRef} className="relative">
      <label className="block text-sm font-medium text-slate-700 mb-1">{label}</label>
      <div className="relative">
        <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-slate-400 pointer-events-none" />
        <input
          type="text"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          onKeyDown={onKeyDown}
          onFocus={() => results.length && setOpen(true)}
          placeholder={placeholder}
          className="w-full pl-9 pr-3 py-2 border border-slate-300 rounded-md focus:ring-1 focus:ring-emerald-500 focus:border-emerald-500"
          role="combobox"
          aria-expanded={open}
          aria-autocomplete="list"
        />
      </div>

      {error && <p className="text-xs text-red-600 mt-1">{error}</p>}

      {open && (
        <ul
          className="absolute z-50 w-full mt-1 bg-white border border-slate-200 rounded-md shadow-lg max-h-64 overflow-y-auto"
          role="listbox"
        >
          {loading && <li className="px-3 py-2 text-sm text-slate-400">Searching…</li>}

          {!loading && results.length === 0 && query.trim() && (
            <li className="px-3 py-2 text-sm text-slate-500">No matches for "{query}"</li>
          )}

          {results.map((item, i) => (
            <li
              key={item.id}
              role="option"
              aria-selected={i === highlight}
              onMouseEnter={() => setHighlight(i)}
              onClick={() => choose(item)}
              className={`px-3 py-2 cursor-pointer text-sm ${
                i === highlight ? 'bg-emerald-50' : 'hover:bg-slate-50'
              }`}
            >
              {renderOption(item)}
            </li>
          ))}
        </ul>
      )}
    </div>
  );
};

export default EntityPicker;
