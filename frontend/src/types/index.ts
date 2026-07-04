export interface AuthResponse {
  token: string
  id: string
  username: string
  email: string
  displayName: string
  phone: string
}

export interface RegisterRequest {
  username: string
  email: string
  password: string
  phone: string
  displayName?: string
}

export interface LoginRequest {
  email: string
  password: string
}

export interface UserResponse {
  id: string
  username: string
  email: string
  displayName: string
  isOnline: boolean
  role?: string
}

export interface UserProfileResponse {
  id: string
  username: string
  email: string
  displayName: string
  phone: string
  avatarUrl: string | null
  isOnline: boolean
  lastSeen: string | null
  createdAt: string
}

export interface UpdateUserRequest {
  username?: string
  email?: string
  displayName?: string
  phone?: string
  newPassword?: string
}

export interface DeleteAccountRequest {
  password: string
}

export interface WorkspaceResponse {
  id: string
  name: string
  description: string
  ownerId: string
  ownerUsername: string
  createdAt: string
}

export interface WorkspaceRequest {
  name: string
  description?: string
}

export interface ChannelResponse {
  id: string
  name: string
  description: string
  isPrivate: boolean
  workspaceId: string
  createdById: string
  createdByUsername: string
  createdAt: string
}

export interface ChannelRequest {
  name: string
  description?: string
  isPrivate?: boolean
}

export interface MessageRequest {
  content: string
  messageType?: string
  fileUrl?: string
}

export interface ChannelMessageResponse {
  id: string
  channelId: string
  senderId: string
  senderUsername: string
  content: string
  messageType: string
  fileUrl: string
  isEdited: boolean
  createdAt: string
}

export interface DirectMessageResponse {
  id: string
  senderId: string
  senderUsername: string
  receiverId: string
  content: string
  messageType: string
  fileUrl: string
  isRead: boolean
  isEdited: boolean
  createdAt: string
}

export interface OnlineStatusEvent {
  email: string
  isOnline: boolean
}

export interface FriendUpdateEvent {
  type: 'INCOMING_REQUEST' | 'ACCEPTED' | 'REJECTED' | 'REMOVED'
  requestId: string | null
  requesterId: string
  receiverId: string
}

export interface FriendRequestResponse {
  id: string
  requesterId: string
  requesterUsername: string
  requesterDisplayName: string
  receiverId: string
  receiverUsername: string
  receiverDisplayName: string
  status: string
  createdAt: string
}

export interface UnreadCountResponse {
  senderId: string
  count: number
}

export interface ApiError {
  status: number
  message: string
  timestamp: string
}

export type ViewType = 'channel' | 'dm'

export interface ActiveView {
  type: ViewType
  id: string
  name: string
}
