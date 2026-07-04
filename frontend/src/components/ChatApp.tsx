import { useCallback, useEffect, useRef, useState } from 'react'
import type {
  ActiveView,
  ChannelMessageResponse,
  ChannelResponse,
  DirectMessageResponse,
  FriendRequestResponse,
  UserResponse,
  WorkspaceResponse,
} from '../types'
import { channelApi, dmApi, friendsApi, messageApi, userApi, workspaceApi } from '../api'
import { websocketService } from '../services/websocket'
import { useAuth } from '../context/AuthContext'
import { MessageList } from './MessageList'
import { MessageInput } from './MessageInput'
import { Sidebar } from './Sidebar'
import { CreateWorkspaceModal } from './CreateWorkspaceModal'
import { CreateChannelModal } from './CreateChannelModal'
import { AddMemberModal } from './AddMemberModal'
import { AddFriendModal } from './AddFriendModal'
import { AccountModal } from './AccountModal'
import './ChatApp.css'

function appendUniqueMessage<T extends { id: string }>(prev: T[], msg: T): T[] {
  if (prev.some((m) => m.id === msg.id)) return prev
  return [...prev, msg]
}

function seedOnlineEmails(users: UserResponse[]): Set<string> {
  return new Set(users.filter((u) => u.isOnline).map((u) => u.email))
}

function applyOnlineStatus(users: UserResponse[], email: string, isOnline: boolean): UserResponse[] {
  return users.map((u) => (u.email === email ? { ...u, isOnline } : u))
}

export function ChatApp() {
  const { user, logout, updateUserFromProfile } = useAuth()
  const activeViewRef = useRef<ActiveView | null>(null)

  const [workspaces, setWorkspaces] = useState<WorkspaceResponse[]>([])
  const [activeWorkspace, setActiveWorkspace] = useState<WorkspaceResponse | null>(null)
  const [channels, setChannels] = useState<ChannelResponse[]>([])
  const [members, setMembers] = useState<UserResponse[]>([])
  const [friends, setFriends] = useState<UserResponse[]>([])
  const [incomingRequests, setIncomingRequests] = useState<FriendRequestResponse[]>([])
  const [outgoingRequests, setOutgoingRequests] = useState<FriendRequestResponse[]>([])
  const [activeView, setActiveView] = useState<ActiveView | null>(null)

  const [channelMessages, setChannelMessages] = useState<ChannelMessageResponse[]>([])
  const [dmMessages, setDmMessages] = useState<DirectMessageResponse[]>([])

  const [showCreateWorkspace, setShowCreateWorkspace] = useState(false)
  const [showCreateChannel, setShowCreateChannel] = useState(false)
  const [showAddMember, setShowAddMember] = useState(false)
  const [showAddFriend, setShowAddFriend] = useState(false)
  const [showAccount, setShowAccount] = useState(false)
  const [onlineEmails, setOnlineEmails] = useState<Set<string>>(new Set())
  const [unreadDmCounts, setUnreadDmCounts] = useState<Record<string, number>>({})

  activeViewRef.current = activeView
  const userIdRef = useRef(user?.id)
  userIdRef.current = user?.id

  const loadUnreadCounts = useCallback(async () => {
    const counts = await dmApi.unreadCounts()
    const view = activeViewRef.current
    const map: Record<string, number> = {}
    counts.forEach((c) => {
      if (view?.type === 'dm' && view.id === c.senderId) return
      map[c.senderId] = c.count
    })
    setUnreadDmCounts(map)
  }, [])

  const loadFriendData = useCallback(async () => {
    const [list, incoming, outgoing] = await Promise.all([
      friendsApi.list(),
      friendsApi.incomingRequests(),
      friendsApi.outgoingRequests(),
    ])
    setFriends(list)
    setIncomingRequests(incoming)
    setOutgoingRequests(outgoing)
    setOnlineEmails((prev) => {
      const next = new Set(prev)
      list.filter((f) => f.isOnline).forEach((f) => next.add(f.email))
      return next
    })
  }, [])

  const loadWorkspaces = useCallback(async () => {
    const list = await workspaceApi.list()
    setWorkspaces(list)
    if (list.length > 0 && !activeWorkspace) {
      setActiveWorkspace(list[0])
    }
  }, [activeWorkspace])

  const loadMembers = useCallback(async (workspaceId: string) => {
    const list = await workspaceApi.members(workspaceId)
    setMembers(list)
    setOnlineEmails((prev) => {
      const next = new Set(prev)
      seedOnlineEmails(list).forEach((email) => next.add(email))
      return next
    })
  }, [])

  useEffect(() => {
    loadWorkspaces().catch(console.error)
    loadFriendData().catch(console.error)
    loadUnreadCounts().catch(console.error)
  }, [loadWorkspaces, loadFriendData, loadUnreadCounts])

  useEffect(() => {
    if (!activeWorkspace) return
    channelApi.list(activeWorkspace.id).then(setChannels).catch(console.error)
    loadMembers(activeWorkspace.id).catch(console.error)
  }, [activeWorkspace, loadMembers])

  useEffect(() => {
    websocketService.setOnlineStatusHandler((event) => {
      setOnlineEmails((prev) => {
        const next = new Set(prev)
        if (event.isOnline) next.add(event.email)
        else next.delete(event.email)
        return next
      })
      setMembers((prev) => applyOnlineStatus(prev, event.email, event.isOnline))
      setFriends((prev) => applyOnlineStatus(prev, event.email, event.isOnline))
    })

    websocketService.setFriendUpdateHandler(() => {
      loadFriendData().catch(console.error)
    })

    websocketService.setDirectMessageHandler((msg) => {
      const view = activeViewRef.current
      const currentUserId = userIdRef.current

      if (view?.type === 'dm' && (view.id === msg.senderId || view.id === msg.receiverId)) {
        setDmMessages((prev) => appendUniqueMessage(prev, msg))
      }

      if (currentUserId && msg.receiverId === currentUserId && msg.senderId !== currentUserId) {
        const isViewingSender = view?.type === 'dm' && view.id === msg.senderId
        if (isViewingSender) {
          dmApi.markConversationRead(msg.senderId).then(() => loadUnreadCounts()).catch(console.error)
        } else {
          loadUnreadCounts().catch(console.error)
        }
      }
    })

    websocketService.onConnect(() => {
      loadUnreadCounts().catch(console.error)
      loadFriendData().catch(console.error)
    })
  }, [loadUnreadCounts, loadFriendData])

  useEffect(() => {
    if (!activeView) return

    if (activeView.type === 'channel') {
      messageApi.list(activeView.id).then(setChannelMessages).catch(console.error)

      websocketService.subscribeToChannel(activeView.id, (msg) => {
        setChannelMessages((prev) => appendUniqueMessage(prev, msg))
      })

      return () => websocketService.unsubscribeFromChannel(activeView.id)
    }

    if (activeView.type === 'dm') {
      const friendId = activeView.id
      dmApi
        .conversation(friendId)
        .then((msgs) => {
          setDmMessages(msgs)
          return loadUnreadCounts()
        })
        .catch(console.error)
    }
  }, [activeView, loadUnreadCounts])

  function selectChannel(channel: ChannelResponse) {
    channelApi.join(channel.id).catch(() => {
      // already a member or private channel — ensureCanParticipate handles access on send
    })
    setActiveView({ type: 'channel', id: channel.id, name: channel.name })
  }

  function selectDm(person: UserResponse) {
    setUnreadDmCounts((prev) => {
      const next = { ...prev }
      delete next[person.id]
      return next
    })
    setActiveView({
      type: 'dm',
      id: person.id,
      name: person.displayName || person.username,
    })
  }

  async function handleSendMessage(content: string) {
    if (!activeView) return

    if (activeView.type === 'channel') {
      websocketService.sendChannelMessage(activeView.id, content)
    } else {
      const msg = await dmApi.send(activeView.id, { content, messageType: 'TEXT' })
      setDmMessages((prev) => appendUniqueMessage(prev, msg))
    }
  }

  async function handleCreateWorkspace(name: string, description: string) {
    const ws = await workspaceApi.create({ name, description })
    setWorkspaces((prev) => [...prev, ws])
    setActiveWorkspace(ws)
    setShowCreateWorkspace(false)
  }

  async function handleCreateChannel(name: string, description: string, isPrivate: boolean) {
    if (!activeWorkspace) return
    const ch = await channelApi.create(activeWorkspace.id, { name, description, isPrivate })
    setChannels((prev) => [...prev, ch])
    setShowCreateChannel(false)
    selectChannel(ch)
  }

  async function handleAddMember(userId: string) {
    if (!activeWorkspace) return
    await workspaceApi.addMember(activeWorkspace.id, userId)
    await loadMembers(activeWorkspace.id)
    setShowAddMember(false)
  }

  async function handleSendFriendRequest(userId: string) {
    await friendsApi.sendRequest(userId)
    await loadFriendData()
    setShowAddFriend(false)
  }

  async function handleAcceptRequest(requestId: string) {
    await friendsApi.acceptRequest(requestId)
    await loadFriendData()
  }

  async function handleRejectRequest(requestId: string) {
    await friendsApi.rejectRequest(requestId)
    await loadFriendData()
  }

  async function handleRemoveFriend(friendId: string, friendName: string) {
    if (!window.confirm(`Remove ${friendName} from your friends?`)) return
    await friendsApi.remove(friendId)
    setFriends((prev) => prev.filter((f) => f.id !== friendId))
    if (activeView?.type === 'dm' && activeView.id === friendId) {
      setActiveView(null)
    }
  }

  function isUserOnline(person: UserResponse) {
    return onlineEmails.has(person.email) || person.isOnline
  }

  const activeDmUser =
    activeView?.type === 'dm'
      ? friends.find((f) => f.id === activeView.id)
      : null

  const isWorkspaceOwner = activeWorkspace?.ownerId === user?.id

  const displayName = user?.displayName || user?.username || 'User'

  return (
    <div className="chat-app">
      <Sidebar
        workspaces={workspaces}
        activeWorkspace={activeWorkspace}
        channels={channels}
        friends={friends.filter((f) => f.id !== user?.id)}
        members={members.filter((m) => m.id !== user?.id)}
        incomingRequests={incomingRequests}
        activeView={activeView}
        isUserOnline={isUserOnline}
        onSelectWorkspace={setActiveWorkspace}
        onSelectChannel={selectChannel}
        onSelectDm={selectDm}
        onCreateWorkspace={() => setShowCreateWorkspace(true)}
        onCreateChannel={() => setShowCreateChannel(true)}
        onAddMember={() => setShowAddMember(true)}
        onAddFriend={() => setShowAddFriend(true)}
        onRemoveFriend={handleRemoveFriend}
        onAcceptRequest={handleAcceptRequest}
        onRejectRequest={handleRejectRequest}
        onLogout={logout}
        onOpenAccount={() => setShowAccount(true)}
        userName={displayName}
        canCreateChannel={isWorkspaceOwner}
        canAddMember={isWorkspaceOwner}
        unreadDmCounts={unreadDmCounts}
      />

      <main className="chat-main">
        {activeView ? (
          <>
            <header className="chat-header">
              <span className="chat-header-icon">
                {activeView.type === 'channel' ? '#' : '@'}
              </span>
              <div className="chat-header-info">
                <h2>{activeView.name}</h2>
                {activeView.type === 'dm' && activeDmUser && (
                  <span className={`chat-header-status ${isUserOnline(activeDmUser) ? 'online' : ''}`}>
                    {isUserOnline(activeDmUser) ? 'Online' : 'Offline'}
                  </span>
                )}
              </div>
            </header>

            <MessageList
              messages={activeView.type === 'channel' ? channelMessages : dmMessages}
              currentUserId={user!.id}
              mode={activeView.type === 'channel' ? 'channel' : 'dm'}
              emptyText={
                activeView.type === 'channel'
                  ? `This is the start of #${activeView.name}`
                  : `Start a conversation with ${activeView.name}`
              }
            />

            <MessageInput
              placeholder={`Message ${activeView.type === 'channel' ? '#' : ''}${activeView.name}`}
              onSend={handleSendMessage}
            />
          </>
        ) : (
          <div className="chat-welcome">
            <div className="welcome-icon">💬</div>
            <h2>Welcome, {displayName}!</h2>
            <p>Select a channel or message a friend to begin chatting.</p>
            {workspaces.length === 0 && (
              <button type="button" onClick={() => setShowCreateWorkspace(true)}>
                Create your first workspace
              </button>
            )}
          </div>
        )}
      </main>

      {showCreateWorkspace && (
        <CreateWorkspaceModal
          onClose={() => setShowCreateWorkspace(false)}
          onCreate={handleCreateWorkspace}
        />
      )}

      {showCreateChannel && (
        <CreateChannelModal
          onClose={() => setShowCreateChannel(false)}
          onCreate={handleCreateChannel}
        />
      )}

      {showAddMember && activeWorkspace && isWorkspaceOwner && (
        <AddMemberModal
          workspaceName={activeWorkspace.name}
          friends={friends.filter((f) => f.id !== user?.id)}
          existingMemberIds={members.map((m) => m.id)}
          onClose={() => setShowAddMember(false)}
          onAdd={handleAddMember}
        />
      )}

      {showAddFriend && (
        <AddFriendModal
          existingFriendIds={friends.map((f) => f.id)}
          pendingReceiverIds={outgoingRequests.map((r) => r.receiverId)}
          currentUserId={user!.id}
          onClose={() => setShowAddFriend(false)}
          onSendRequest={handleSendFriendRequest}
        />
      )}

      {showAccount && (
        <AccountModal
          onClose={() => setShowAccount(false)}
          loadProfile={userApi.me}
          updateProfile={userApi.updateMe}
          deleteAccount={userApi.deleteMe}
          onUpdated={(profile) => {
            updateUserFromProfile(profile)
            setShowAccount(false)
          }}
          onDeleted={() => {
            setShowAccount(false)
            logout()
          }}
        />
      )}
    </div>
  )
}
