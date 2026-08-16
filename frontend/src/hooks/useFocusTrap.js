import { useEffect, useRef } from 'react';

export const useFocusTrap = (isActive) => {
  const ref = useRef(null);

  useEffect(() => {
    if (!isActive) return;

    const element = ref.current;
    if (!element) return;

    const focusableEls = element.querySelectorAll(
      'a[href], button:not([disabled]), textarea:not([disabled]), input:not([disabled]), select:not([disabled]), [tabindex]:not([tabindex="-1"])'
    );
    
    if (focusableEls.length === 0) return;

    const firstFocusableEl = focusableEls[0];
    const lastFocusableEl = focusableEls[focusableEls.length - 1];

    const handleTab = (e) => {
      if (e.key !== 'Tab') return;

      if (e.shiftKey) { // Shift + Tab
        if (document.activeElement === firstFocusableEl) {
          lastFocusableEl.focus();
          e.preventDefault();
        }
      } else { // Tab
        if (document.activeElement === lastFocusableEl) {
          firstFocusableEl.focus();
          e.preventDefault();
        }
      }
    };

    element.addEventListener('keydown', handleTab);
    
    // Focus first element slightly after render
    setTimeout(() => {
      if (document.activeElement && element.contains(document.activeElement)) return;
      firstFocusableEl.focus();
    }, 10);

    return () => {
      element.removeEventListener('keydown', handleTab);
    };
  }, [isActive]);

  return ref;
};
