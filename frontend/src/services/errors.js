/**
 * Extracts a user-safe message from an Axios error using the project-wide
 * error contract: { error, code, status, timestamp, fields? }.
 *
 * @param {unknown} err       the caught Axios error
 * @param {string}  fallback  shown when the server sent nothing usable
 * @returns {string}
 */
export function apiErrorMessage(err, fallback = 'Something went wrong. Please try again.') {
  const data = err?.response?.data;

  if (!data) {
    // No response at all — network down, CORS, or the API is not running.
    return err?.request
      ? 'Cannot reach the server. Check that the backend is running.'
      : fallback;
  }

  // Validation failure: surface the first field message, it is the most actionable.
  if (data.fields) {
    const first = Object.values(data.fields)[0];
    if (first) return first;
  }

  return data.error || fallback;
}

/** @returns {string|null} the machine-readable code, for branching on specific errors. */
export function apiErrorCode(err) {
  return err?.response?.data?.code ?? null;
}
