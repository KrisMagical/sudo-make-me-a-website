import { describe, expect, it } from 'vitest'
import { formatDate, formatDateTime } from './date'

describe('date utils', () => {
  it('formats dates consistently', () => {
    const date = new Date(2026, 0, 2, 3, 4)
    expect(formatDate(date)).toBe('2026-01-02')
    expect(formatDateTime(date)).toBe('2026-01-02 03:04')
  })
})
