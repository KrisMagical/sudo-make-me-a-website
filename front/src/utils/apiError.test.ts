import { describe, expect, it } from 'vitest'
import { getApiErrorMessage, isCanceledRequest } from './apiError'

describe('api error utils', () => {
  it('reads message and field errors from api responses', () => {
    const error = {
      response: {
        data: {
          message: 'Validation failed',
          errors: { email: 'email must be valid' }
        }
      }
    }
    expect(getApiErrorMessage(error, 'fallback')).toBe('Validation failed')
  })

  it('detects canceled axios requests', () => {
    expect(isCanceledRequest({ code: 'ERR_CANCELED' })).toBe(true)
    expect(isCanceledRequest({ name: 'CanceledError' })).toBe(true)
    expect(isCanceledRequest({ name: 'AbortError' })).toBe(true)
  })
})
