import { useState } from 'react'
import type { UserResponse } from '../types'
import { userApi } from '../api'
import './Modal.css'

interface UserSearchModalProps {
  onClose: () => void
  onSelect: (user: UserResponse) => void
}

export function UserSearchModal({ onClose, onSelect }: UserSearchModalProps) {
  const [keyword, setKeyword] = useState('')
  const [results, setResults] = useState<UserResponse[]>([])
  const [loading, setLoading] = useState(false)

  async function handleSearch(e: React.FormEvent) {
    e.preventDefault()
    if (!keyword.trim()) return
    setLoading(true)
    try {
      const users = await userApi.search(keyword.trim())
      setResults(users)
    } catch {
      setResults([])
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal" onClick={(e) => e.stopPropagation()}>
        <h3>New Direct Message</h3>
        <form onSubmit={handleSearch}>
          <label>
            Search users
            <input
              type="text"
              value={keyword}
              onChange={(e) => setKeyword(e.target.value)}
              placeholder="Search by username or email"
              autoFocus
            />
          </label>
          <button type="submit" disabled={loading || !keyword.trim()} style={{ width: '100%', marginBottom: '1rem' }}>
            {loading ? 'Searching…' : 'Search'}
          </button>
        </form>

        {results.length > 0 && (
          <ul className="user-search-results">
            {results.map((user) => (
              <li key={user.id}>
                <button type="button" onClick={() => onSelect(user)}>
                  <span className="search-avatar">
                    {(user.displayName || user.username).charAt(0).toUpperCase()}
                  </span>
                  <span>
                    <strong>{user.displayName || user.username}</strong>
                    <small>@{user.username}</small>
                  </span>
                </button>
              </li>
            ))}
          </ul>
        )}

        <div className="modal-actions">
          <button type="button" className="btn-secondary" onClick={onClose}>
            Cancel
          </button>
        </div>
      </div>
    </div>
  )
}
