import type {
  ActiveView,
  ChannelResponse,
  FriendRequestResponse,
  UserResponse,
  WorkspaceResponse,
} from '../types'
import './Sidebar.css'

interface SidebarProps {
  workspaces: WorkspaceResponse[]
  activeWorkspace: WorkspaceResponse | null
  channels: ChannelResponse[]
  friends: UserResponse[]
  members: UserResponse[]
  incomingRequests: FriendRequestResponse[]
  activeView: ActiveView | null
  isUserOnline: (user: UserResponse) => boolean
  onSelectWorkspace: (ws: WorkspaceResponse) => void
  onSelectChannel: (ch: ChannelResponse) => void
  onSelectDm: (user: UserResponse) => void
  onCreateWorkspace: () => void
  onCreateChannel: () => void
  onAddMember: () => void
  onAddFriend: () => void
  onRemoveFriend: (friendId: string, friendName: string) => void
  onAcceptRequest: (requestId: string) => void
  onRejectRequest: (requestId: string) => void
  onLogout: () => void
  onOpenAccount: () => void
  userName: string
  canCreateChannel: boolean
  canAddMember: boolean
  unreadDmCounts: Record<string, number>
}

export function Sidebar({
  workspaces,
  activeWorkspace,
  channels,
  friends,
  members,
  incomingRequests,
  activeView,
  isUserOnline,
  onSelectWorkspace,
  onSelectChannel,
  onSelectDm,
  onCreateWorkspace,
  onCreateChannel,
  onAddMember,
  onAddFriend,
  onRemoveFriend,
  onAcceptRequest,
  onRejectRequest,
  onLogout,
  onOpenAccount,
  userName,
  canCreateChannel,
  canAddMember,
  unreadDmCounts,
}: SidebarProps) {
  const memberIds = new Set(members.map((m) => m.id))
  const totalUnread = Object.entries(unreadDmCounts).reduce((sum, [friendId, count]) => {
    if (activeView?.type === 'dm' && activeView.id === friendId) return sum
    return sum + count
  }, 0)

  return (
    <aside className="sidebar">
      <div className="sidebar-workspace-bar">
        <select
          value={activeWorkspace?.id ?? ''}
          onChange={(e) => {
            const ws = workspaces.find((w) => w.id === e.target.value)
            if (ws) onSelectWorkspace(ws)
          }}
        >
          {workspaces.map((ws) => (
            <option key={ws.id} value={ws.id}>
              {ws.name}
            </option>
          ))}
        </select>
        <button type="button" className="icon-btn" onClick={onCreateWorkspace} title="New workspace">
          +
        </button>
      </div>

      <nav className="sidebar-nav">
        {incomingRequests.length > 0 && (
          <div className="sidebar-section">
            <div className="sidebar-section-header">
              <span>Friend Requests</span>
              <span className="request-badge">{incomingRequests.length}</span>
            </div>
            <ul>
              {incomingRequests.map((req) => (
                <li key={req.id} className="friend-request-item">
                  <div className="friend-request-info">
                    <span className="friend-request-name">
                      {req.requesterDisplayName || req.requesterUsername}
                    </span>
                    <span className="friend-request-sub">wants to be friends</span>
                  </div>
                  <div className="friend-request-actions">
                    <button
                      type="button"
                      className="accept-btn"
                      onClick={() => onAcceptRequest(req.id)}
                      title="Accept"
                    >
                      ✓
                    </button>
                    <button
                      type="button"
                      className="reject-btn"
                      onClick={() => onRejectRequest(req.id)}
                      title="Reject"
                    >
                      ×
                    </button>
                  </div>
                </li>
              ))}
            </ul>
          </div>
        )}

        <div className="sidebar-section">
          <div className="sidebar-section-header">
            <span>Channels</span>
            {canCreateChannel && (
              <button type="button" className="icon-btn small" onClick={onCreateChannel} title="New channel">
                +
              </button>
            )}
          </div>
          <ul>
            {channels.map((ch) => (
              <li key={ch.id}>
                <button
                  type="button"
                  className={
                    activeView?.type === 'channel' && activeView.id === ch.id ? 'active' : ''
                  }
                  onClick={() => onSelectChannel(ch)}
                >
                  <span className="channel-hash">#</span>
                  {ch.name}
                  {ch.isPrivate && <span className="private-badge">🔒</span>}
                </button>
              </li>
            ))}
            {channels.length === 0 && (
              <li className="sidebar-empty">No channels yet</li>
            )}
          </ul>
        </div>

        <div className="sidebar-section">
          <div className="sidebar-section-header">
            <span>Members</span>
            {canAddMember && (
              <button type="button" className="icon-btn small" onClick={onAddMember} title="Add member">
                +
              </button>
            )}
          </div>
          <ul>
            {members.map((member) => (
              <li key={member.id}>
                <div className="member-item-static">
                  <span className={`status-dot ${isUserOnline(member) ? 'online' : ''}`} />
                  {member.displayName || member.username}
                </div>
              </li>
            ))}
            {members.length === 0 && (
              <li className="sidebar-empty">No members yet</li>
            )}
          </ul>
        </div>

        <div className="sidebar-section">
          <div className="sidebar-section-header">
            <span>Friends</span>
            <div className="sidebar-header-actions">
              {totalUnread > 0 && (
                <span className="unread-total-badge" title="Unread messages">
                  {totalUnread > 99 ? '99+' : totalUnread}
                </span>
              )}
              <button type="button" className="icon-btn small" onClick={onAddFriend} title="Send friend request">
                +
              </button>
            </div>
          </div>
          <ul>
            {friends.map((friend) => {
              const isActiveDm = activeView?.type === 'dm' && activeView.id === friend.id
              const unread = isActiveDm ? 0 : (unreadDmCounts[friend.id] ?? 0)
              return (
              <li key={friend.id} className="friend-item">
                <button
                  type="button"
                  className={`friend-dm-btn${
                    activeView?.type === 'dm' && activeView.id === friend.id ? ' active' : ''
                  }`}
                  onClick={() => onSelectDm(friend)}
                >
                  <span className={`status-dot ${isUserOnline(friend) ? 'online' : ''}`} />
                  <span className="friend-name">
                    {friend.displayName || friend.username}
                  </span>
                  {unread > 0 && (
                    <span className="unread-badge">{unread > 99 ? '99+' : unread}</span>
                  )}
                  {!memberIds.has(friend.id) && unread === 0 && (
                    <span className="external-badge" title="Not in this workspace">•</span>
                  )}
                </button>
                <button
                  type="button"
                  className="remove-friend-btn"
                  onClick={() =>
                    onRemoveFriend(friend.id, friend.displayName || friend.username)
                  }
                  title="Remove friend"
                  aria-label={`Remove ${friend.displayName || friend.username}`}
                >
                  ×
                </button>
              </li>
            )})}
            {friends.length === 0 && (
              <li className="sidebar-empty">No friends yet — send a request</li>
            )}
          </ul>
        </div>
      </nav>

      <div className="sidebar-footer">
        <button type="button" className="sidebar-user-btn" onClick={onOpenAccount} title="My account">
          <div className="user-avatar">{userName.charAt(0).toUpperCase()}</div>
          <span className="user-name">{userName}</span>
        </button>
        <button type="button" className="logout-btn" onClick={onLogout}>
          Sign out
        </button>
      </div>
    </aside>
  )
}
