import axios from 'axios'

type ApiErrors = Record<string, string>

export const isCanceledRequest = (error: unknown) => {
  const err = error as any
  return axios.isCancel(error)
      || err?.code === 'ERR_CANCELED'
      || err?.name === 'CanceledError'
      || err?.name === 'AbortError'
}

export const getApiFieldErrors = (error: unknown): ApiErrors => {
  const data = (error as any)?.response?.data
  return data?.errors && typeof data.errors === 'object' ? data.errors : {}
}

export const getApiErrorMessage = (error: unknown, fallback = 'Request failed') => {
  const data = (error as any)?.response?.data
  if (data?.message) return data.message
  const fieldErrors = getApiFieldErrors(error)
  const firstFieldError = Object.values(fieldErrors)[0]
  if (firstFieldError) return firstFieldError
  if ((error as any)?.message) return (error as any).message
  return fallback
}

export const getApiRequestId = (error: unknown): string | undefined => {
  const headers = (error as any)?.response?.headers
  if (!headers) return undefined

  if (typeof headers.get === 'function') {
    return headers.get('x-request-id') || headers.get('X-Request-Id') || undefined
  }

  return headers['x-request-id'] || headers['X-Request-Id']
}

export const getApiErrorMessageWithRequestId = (error: unknown, fallback = 'Request failed') => {
  const message = getApiErrorMessage(error, fallback)
  const requestId = getApiRequestId(error)
  return requestId ? `${message} [requestId: ${requestId}]` : message
}
