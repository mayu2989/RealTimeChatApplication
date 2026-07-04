import { useState } from 'react'
import { userApi } from '../api'
import { ApiClientError } from '../api/client'
import './Modal.css'

interface AddFriendModalProps {
  existingFriendIds: string[]
  pendingReceiverIds: string[]
  currentUserId: string
  onClose: () => void
  onSendRequest: (userId: string) => Promise<void>
}

export function AddFriendModal({
  existingFriendIds,
  pendingReceiverIds,
  currentUserId,
  onClose,
  onSendRequest,
}: AddFriendModalProps) {
  const [keyword, setKeyword] = useState('')
  const [loading, setLoading] = useState(false)
  const [sendingId, setSendingId] = useState<string | null>(null)
  const [error, setError] = useState('')
  const [results, setResults] = useState<
    Array<{ id: string; username: string; displayName: string; email: string }>
  >([])

  async function handleSearch(e: React.FormEvent) {
    e.preventDefault()
    if (!keyword.trim()) return
    setLoading(true)
    setError('')
    try {
      const users = await userApi.search(keyword.trim())
      setResults(
        users.filter(
          (u) =>
            u.id !== currentUserId &&
            !existingFriendIds.includes(u.id) &&
            !pendingReceiverIds.includes(u.id),
        ),
      )
    } catch {
      setResults([])
      setError('Search failed')
    } finally {
      setLoading(false)
    }
  }

  async function handleSend(userId: string) {
    setSendingId(userId)
    setError('')
    try {
      await onSendRequest(userId)
    } catch (err) {
      setError(err instanceof ApiClientError ? err.message : 'Failed to send request')
    } finally {
      setSendingId(null)
    }
  }

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal" onClick={(e) => e.stopPropagation()}>
        <h3>Send Friend Request</h3>
        <p className="modal-subtitle">
          Search for users. They must accept before you can chat or add them to your workspace.
        </p>
        {error && <div className="modal-error">{error}</div>}
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
          <button type="submit" disabled={loading || !keyword.trim()} className="modal-search-btn">
            {loading ? 'Searching…' : 'Search'}
          </button>
        </form>

        {results.length > 0 && (
          <ul className="user-search-results">
            {results.map((user) => (
              <li key={user.id}>
                <div className="search-result-row">
                  <span className="search-avatar">
                    {(user.displayName || user.username).charAt(0).toUpperCase()}
                  </span>
                  <div className="search-result-info">
                    <span className="search-result-name">
                      {user.displayName || user.username}
                    </span>
                    <span className="search-result-username">@{user.username}</span>
                  </div>
                  <button
                    type="button"
                    className="action-btn"
                    disabled={sendingId === user.id}
                    onClick={() => handleSend(user.id)}
                  >
                    {sendingId === user.id ? 'Sending…' : 'Request'}
                  </button>
                </div>
              </li>
            ))}
          </ul>
        )}

        <div className="modal-actions">
          <button type="button" className="btn-secondary" onClick={onClose}>
            Close
          </button>
        </div>
      </div>
    </div>
  )
}
