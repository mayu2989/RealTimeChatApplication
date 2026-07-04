import { useState } from 'react'
import type { UserResponse } from '../types'
import { ApiClientError } from '../api/client'
import './Modal.css'

interface AddMemberModalProps {
  workspaceName: string
  friends: UserResponse[]
  existingMemberIds: string[]
  onClose: () => void
  onAdd: (userId: string) => Promise<void>
}

export function AddMemberModal({
  workspaceName,
  friends,
  existingMemberIds,
  onClose,
  onAdd,
}: AddMemberModalProps) {
  const [addingId, setAddingId] = useState<string | null>(null)
  const [error, setError] = useState('')

  const availableFriends = friends.filter((f) => !existingMemberIds.includes(f.id))

  async function handleAdd(userId: string) {
    setAddingId(userId)
    setError('')
    try {
      await onAdd(userId)
    } catch (err) {
      setError(err instanceof ApiClientError ? err.message : 'Failed to add member')
    } finally {
      setAddingId(null)
    }
  }

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal" onClick={(e) => e.stopPropagation()}>
        <h3>Add Member to {workspaceName}</h3>
        <p className="modal-subtitle">
          Only accepted friends can be added to a workspace you own.
        </p>
        {error && <div className="modal-error">{error}</div>}

        {availableFriends.length > 0 ? (
          <ul className="user-search-results">
            {availableFriends.map((user) => (
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
                    disabled={addingId === user.id}
                    onClick={() => handleAdd(user.id)}
                  >
                    {addingId === user.id ? 'Adding…' : 'Add'}
                  </button>
                </div>
              </li>
            ))}
          </ul>
        ) : (
          <p className="modal-empty-note">
            No friends available to add. Send and accept friend requests first.
          </p>
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
