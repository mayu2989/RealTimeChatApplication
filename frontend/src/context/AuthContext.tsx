import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from 'react'
import type { AuthResponse, LoginRequest, RegisterRequest, UserProfileResponse } from '../types'
import { authApi, userApi } from '../api'
import { clearAuthStorage, persistAuth, loadStoredUser } from '../auth/storage'
import { websocketService } from '../services/websocket'

interface AuthContextValue {
  user: AuthResponse | null
  isLoading: boolean
  login: (data: LoginRequest) => Promise<void>
  register: (data: RegisterRequest) => Promise<void>
  logout: () => void
  updateUserFromProfile: (profile: UserProfileResponse) => void
}

const AuthContext = createContext<AuthContextValue | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthResponse | null>(loadStoredUser)
  const [isLoading, setIsLoading] = useState(true)

  const logout = useCallback(() => {
    websocketService.disconnect()
    clearAuthStorage()
    setUser(null)
  }, [])

  useEffect(() => {
    function onSessionExpired() {
      websocketService.disconnect()
      setUser(null)
    }

    window.addEventListener('auth:session-expired', onSessionExpired)
    return () => window.removeEventListener('auth:session-expired', onSessionExpired)
  }, [])

  useEffect(() => {
    async function bootstrapSession() {
      const token = localStorage.getItem('token')
      const storedUser = loadStoredUser()

      if (!token || !storedUser) {
        clearAuthStorage()
        setUser(null)
        setIsLoading(false)
        return
      }

      try {
        const profile = await userApi.me()
        const synced: AuthResponse = {
          token,
          id: profile.id,
          username: profile.username,
          email: profile.email,
          displayName: profile.displayName,
          phone: profile.phone,
        }
        persistAuth(synced)
        setUser(synced)
        await websocketService.connect(token)
      } catch {
        logout()
      } finally {
        setIsLoading(false)
      }
    }

    bootstrapSession()
  }, [logout])

  const handleAuthSuccess = useCallback(async (auth: AuthResponse) => {
    websocketService.disconnect()
    persistAuth(auth)
    setUser(auth)
    await websocketService.connect(auth.token)
  }, [])

  const login = useCallback(
    async (data: LoginRequest) => {
      const auth = await authApi.login(data)
      await handleAuthSuccess(auth)
    },
    [handleAuthSuccess],
  )

  const register = useCallback(
    async (data: RegisterRequest) => {
      const auth = await authApi.register(data)
      await handleAuthSuccess(auth)
    },
    [handleAuthSuccess],
  )

  const updateUserFromProfile = useCallback((profile: UserProfileResponse) => {
    setUser((prev) => {
      if (!prev) return prev
      const updated: AuthResponse = {
        ...prev,
        username: profile.username,
        email: profile.email,
        displayName: profile.displayName,
        phone: profile.phone,
      }
      persistAuth(updated)
      return updated
    })
  }, [])

  const value = useMemo(
    () => ({ user, isLoading, login, register, logout, updateUserFromProfile }),
    [user, isLoading, login, register, logout, updateUserFromProfile],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within AuthProvider')
  return ctx
}
