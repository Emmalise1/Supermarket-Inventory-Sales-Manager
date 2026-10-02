/** Formatting helpers used across the UI (unit-tested in format.test.js). */

export function formatMoney(amount) {
  const value = Number(amount ?? 0);
  return `${value.toLocaleString('en-US', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  })} RWF`;
}

export function formatDateTime(iso) {
  if (!iso) return '-';
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) return '-';
  return date.toLocaleString();
}

export function cacheLabel(cacheHit) {
  return cacheHit ? 'Redis cache' : 'MySQL (now cached)';
}
