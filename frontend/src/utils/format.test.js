import { describe, it, expect } from 'vitest';
import { formatMoney, formatDateTime, cacheLabel } from './format';

describe('formatMoney', () => {
  it('formats amounts with two decimals and RWF', () => {
    expect(formatMoney(1234.5)).toBe('1,234.50 RWF');
  });

  it('treats null/undefined as zero', () => {
    expect(formatMoney(null)).toBe('0.00 RWF');
    expect(formatMoney(undefined)).toBe('0.00 RWF');
  });

  it('accepts numeric strings from JSON', () => {
    expect(formatMoney('900')).toBe('900.00 RWF');
  });
});

describe('formatDateTime', () => {
  it('returns a dash for missing values', () => {
    expect(formatDateTime(null)).toBe('-');
    expect(formatDateTime('not-a-date')).toBe('-');
  });

  it('formats a valid ISO timestamp', () => {
    expect(formatDateTime('2026-01-05T10:30:00Z')).not.toBe('-');
  });
});

describe('cacheLabel', () => {
  it('labels cache hits as Redis and misses as MySQL', () => {
    expect(cacheLabel(true)).toBe('Redis cache');
    expect(cacheLabel(false)).toBe('MySQL (now cached)');
  });
});
