import { useEffect, useState } from 'react'
import type { DeleteAccountRequest, UpdateUserRequest, UserProfileResponse } from '../types'
import './Modal.css'
import './AccountModal.css'

interface AccountModalProps {
  onClose: () => void
  onUpdated: (profile: UserProfileResponse) => void
  onDeleted: () => void
  loadProfile: () => Promise<UserProfileResponse>
  updateProfile: (data: UpdateUserRequest) => Promise<UserProfileResponse>
  deleteAccount: (data: DeleteAccountRequest) => Promise<void>
}

type Tab = 'view' | 'edit' | 'delete'

function formatDateTime(dateStr: string | null | undefined) {
  if (!dateStr) return '—'
  return new Date(dateStr).toLocaleString([], {
    month: 'short',
    day: 'numeric',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}

export function AccountModal({
  onClose,
  onUpdated,
  onDeleted,
  loadProfile,
  updateProfile,
  deleteAccount,
}: AccountModalProps) {
  const [tab, setTab] = useState<Tab>('view')
  const [profile, setProfile] = useState<UserProfileResponse | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const [username, setUsername] = useState('')
  const [email, setEmail] = useState('')
  const [displayName, setDisplayName] = useState('')
  const [phone, setPhone] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [deletePassword, setDeletePassword] = useState('')
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    loadProfile()
      .then((p) => {
        setProfile(p)
        setUsername(p.username)
        setEmail(p.email)
        setDisplayName(p.displayName || '')
        setPhone(p.phone || '')
      })
      .catch(() => setError('Failed to load profile'))
      .finally(() => setLoading(false))
  }, [loadProfile])

  async function handleSave(e: React.FormEvent) {
    e.preventDefault()
    setSaving(true)
    setError('')
    try {
      const data: UpdateUserRequest = {}
      if (username.trim() !== profile?.username) data.username = username.trim()
      if (email.trim() !== profile?.email) data.email = email.trim()
      if (displayName.trim() !== (profile?.displayName || '')) data.displayName = displayName.trim()
      if (phone.trim() !== (profile?.phone || '')) data.phone = phone.trim()
      if (newPassword.trim()) data.newPassword = newPassword.trim()

      const updated = await updateProfile(data)
      setProfile(updated)
      setNewPassword('')
      onUpdated(updated)
      setTab('view')
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to update profile')
    } finally {
      setSaving(false)
    }
  }

  async function handleDelete(e: React.FormEvent) {
    e.preventDefault()
    if (!window.confirm('This permanently deletes your account and all owned workspaces. Continue?')) {
      return
    }
    setSaving(true)
    setError('')
    try {
      await deleteAccount({ password: deletePassword })
      onDeleted()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to delete account')
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal account-modal" onClick={(e) => e.stopPropagation()}>
        <h3>My Account</h3>
        <p className="modal-subtitle">View and manage your personal details</p>

        <div className="account-tabs">
          <button
            type="button"
            className={tab === 'view' ? 'active' : ''}
            onClick={() => setTab('view')}
          >
            Profile
          </button>
          <button
            type="button"
            className={tab === 'edit' ? 'active' : ''}
            onClick={() => setTab('edit')}
          >
            Edit
          </button>
          <button
            type="button"
            className={`danger-tab${tab === 'delete' ? ' active' : ''}`}
            onClick={() => setTab('delete')}
          >
            Delete
          </button>
        </div>

        {error && <div className="modal-error">{error}</div>}

        {loading ? (
          <p className="account-loading">Loading profile…</p>
        ) : profile && tab === 'view' ? (
          <dl className="profile-details">
            <div>
              <dt>Display name</dt>
              <dd>{profile.displayName || profile.username}</dd>
            </div>
            <div>
              <dt>Username</dt>
              <dd>@{profile.username}</dd>
            </div>
            <div>
              <dt>Email</dt>
              <dd>{profile.email}</dd>
            </div>
            <div>
              <dt>Phone</dt>
              <dd>{profile.phone || '—'}</dd>
            </div>
            <div>
              <dt>Status</dt>
              <dd>{profile.isOnline ? 'Online' : 'Offline'}</dd>
            </div>
            <div>
              <dt>Last seen</dt>
              <dd>{formatDateTime(profile.lastSeen)}</dd>
            </div>
            <div>
              <dt>Member since</dt>
              <dd>{formatDateTime(profile.createdAt)}</dd>
            </div>
          </dl>
        ) : profile && tab === 'edit' ? (
          <form onSubmit={handleSave}>
            <label>
              Display name
              <input
                type="text"
                value={displayName}
                onChange={(e) => setDisplayName(e.target.value)}
                placeholder="Your display name"
              />
            </label>
            <label>
              Username
              <input
                type="text"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                placeholder="username"
              />
            </label>
            <label>
              Email
              <input
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="you@example.com"
              />
            </label>
            <label>
              Phone
              <input
                type="text"
                value={phone}
                onChange={(e) => setPhone(e.target.value)}
                placeholder="Phone number"
              />
            </label>
            <label>
              New password
              <input
                type="password"
                value={newPassword}
                onChange={(e) => setNewPassword(e.target.value)}
                placeholder="Leave blank to keep current"
                autoComplete="new-password"
              />
            </label>
            <div className="modal-actions">
              <button type="button" className="btn-secondary" onClick={onClose}>
                Cancel
              </button>
              <button type="submit" disabled={saving}>
                {saving ? 'Saving…' : 'Save changes'}
              </button>
            </div>
          </form>
        ) : profile && tab === 'delete' ? (
          <form onSubmit={handleDelete} className="delete-account-form">
            <p className="delete-warning">
              Deleting your account removes your profile, friendships, messages, and any
              workspaces you own. This cannot be undone.
            </p>
            <label>
              Confirm with your password
              <input
                type="password"
                required
                value={deletePassword}
                onChange={(e) => setDeletePassword(e.target.value)}
                placeholder="Current password"
                autoComplete="current-password"
              />
            </label>
            <div className="modal-actions">
              <button type="button" className="btn-secondary" onClick={onClose}>
                Cancel
              </button>
              <button type="submit" className="btn-danger" disabled={saving || !deletePassword}>
                {saving ? 'Deleting…' : 'Delete my account'}
              </button>
            </div>
          </form>
        ) : null}

        {tab === 'view' && !loading && (
          <div className="modal-actions">
            <button type="button" className="btn-secondary" onClick={onClose}>
              Close
            </button>
          </div>
        )}
      </div>
    </div>
  )
}
