import { beforeEach, describe, expect, it } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useAuthStore } from './authStore'

describe('authStore', () => {
  beforeEach(() => {
    localStorage.removeItem('token')
    localStorage.removeItem('username')
    localStorage.removeItem('role')
    localStorage.removeItem('theme')
    localStorage.removeItem('draft')
    setActivePinia(createPinia())
  })

  it('logout only removes auth keys', () => {
    localStorage.setItem('token', 'token')
    localStorage.setItem('username', 'admin')
    localStorage.setItem('role', 'ROOT')
    localStorage.setItem('theme', 'dark')
    localStorage.setItem('draft', 'keep')

    const store = useAuthStore()
    store.logout()

    expect(localStorage.getItem('token')).toBeNull()
    expect(localStorage.getItem('username')).toBeNull()
    expect(localStorage.getItem('role')).toBeNull()
    expect(localStorage.getItem('theme')).toBe('dark')
    expect(localStorage.getItem('draft')).toBe('keep')
  })
})
