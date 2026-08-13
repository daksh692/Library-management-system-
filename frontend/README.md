# LMS Frontend

React 19 + Vite 8 + Tailwind 4. Talks to the Spring Boot API at `http://localhost:8080/api`.

## Running

```bash
npm install
npm run dev      # http://localhost:5173
npm run build    # production bundle into dist/
npm run lint     # oxlint
```

The backend must be running, or every request fails with "Cannot reach the server."

## Structure

```
src/
├── components/
│   ├── ui/          primitives: ToastProvider, Skeleton, EntityPicker, ConfirmDialog
│   └── *.jsx        domain components: BookCard, AlertBanner, modals
├── context/         AuthContext — user state, login/logout, token persistence
├── services/
│   ├── api.js       Axios instance with the Bearer interceptor
│   └── errors.js    apiErrorMessage() — the one way to read an API error
└── views/
    ├── auth/        Login, Register
    ├── user/        UserDashboard, BookDetails, SearchResults
    └── admin/       AdminDashboard, UserManager, TransactionManager
```

## Conventions

**Errors.** Never read `err.response.data` directly. Always:

```jsx
import { apiErrorMessage } from '../services/errors';
toast.error(apiErrorMessage(err, 'Could not save the book.'));
```

**Feedback.** `useToast()`, never `alert()` or `confirm()`. Use `<ConfirmDialog>` for confirmations.

**Loading.** Skeletons from `components/ui/Skeleton`, not "Loading…" text.

**Design tokens.** Defined in [`../IMP_Files/Desigine.md`](../IMP_Files/Desigine.md):
slate base, emerald secondary, amber for warnings and fines. Serif (`font-serif`) for headings and
book titles, sans for everything else. Only Tailwind core utilities — there is no config extension.

**Accessibility.** Every modal needs `role="dialog"`, `aria-modal`, `aria-labelledby`, Escape to
close, and focus restoration. Every icon-only button needs an `aria-label`.
