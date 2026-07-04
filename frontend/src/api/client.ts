import type { ApiError } from '../types'
import { clearAuthStorage } from '../auth/storage'

const API_BASE = import.meta.env.VITE_API_URL ?? ''

export class ApiClientError extends Error {
  status: number

  constructor(message: string, status: number) {
    super(message)
    this.name = 'ApiClientError'
    this.status = status
  }
}

function getToken(): string | null {
  return localStorage.getItem('token')
}

export function invalidateSession() {
  clearAuthStorage()
  window.dispatchEvent(new CustomEvent('auth:session-expired'))
}

export async function apiRequest<T>(
  path: string,
  options: RequestInit = {},
): Promise<T> {
  const token = getToken()
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...(options.headers as Record<string, string>),
  }

  if (token) {
    headers['Authorization'] = `Bearer ${token}`
  }

  const response = await fetch(`${API_BASE}${path}`, {
    ...options,
    headers,
  })

  if (!response.ok) {
    let message = response.statusText
    try {
      const error: ApiError = await response.json()
      message = error.message
    } catch {
      // use statusText
    }

    if (response.status === 401) {
      invalidateSession()
    }

    throw new ApiClientError(message, response.status)
  }

  if (response.status === 204 || response.headers.get('content-length') === '0') {
    return undefined as T
  }

  const text = await response.text()
  if (!text) return undefined as T
  return JSON.parse(text) as T
}
