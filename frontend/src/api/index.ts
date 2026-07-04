import { apiRequest } from './client'
import type {
  AuthResponse,
  ChannelMessageResponse,
  ChannelRequest,
  ChannelResponse,
  DirectMessageResponse,
  LoginRequest,
  MessageRequest,
  RegisterRequest,
  UserResponse,
  UserProfileResponse,
  UpdateUserRequest,
  DeleteAccountRequest,
  WorkspaceRequest,
  WorkspaceResponse,
  FriendRequestResponse,
  UnreadCountResponse,
} from '../types'

export const authApi = {
  register: (data: RegisterRequest) =>
    apiRequest<AuthResponse>('/api/v1/auth/register', {
      method: 'POST',
      body: JSON.stringify(data),
    }),

  login: (data: LoginRequest) =>
    apiRequest<AuthResponse>('/api/v1/auth/login', {
      method: 'POST',
      body: JSON.stringify(data),
    }),
}

export const workspaceApi = {
  list: () => apiRequest<WorkspaceResponse[]>('/api/v1/workspaces'),

  get: (id: string) => apiRequest<WorkspaceResponse>(`/api/v1/workspaces/${id}`),

  create: (data: WorkspaceRequest) =>
    apiRequest<WorkspaceResponse>('/api/v1/workspaces', {
      method: 'POST',
      body: JSON.stringify(data),
    }),

  members: (workspaceId: string) =>
    apiRequest<UserResponse[]>(`/api/v1/workspaces/${workspaceId}/members`),

  addMember: (workspaceId: string, userId: string) =>
    apiRequest<void>(`/api/v1/workspaces/${workspaceId}/members/${userId}`, {
      method: 'POST',
    }),
}

export const channelApi = {
  list: (workspaceId: string) =>
    apiRequest<ChannelResponse[]>(`/api/v1/channels/workspace/${workspaceId}`),

  create: (workspaceId: string, data: ChannelRequest) =>
    apiRequest<ChannelResponse>(`/api/v1/channels/workspace/${workspaceId}`, {
      method: 'POST',
      body: JSON.stringify(data),
    }),

  join: (channelId: string) =>
    apiRequest<void>(`/api/v1/channels/${channelId}/join`, { method: 'POST' }),

  members: (channelId: string) =>
    apiRequest<UserResponse[]>(`/api/v1/channels/${channelId}/members`),
}

export const messageApi = {
  list: (channelId: string) =>
    apiRequest<ChannelMessageResponse[]>(`/api/v1/channel-messages/${channelId}`),

  send: (channelId: string, data: MessageRequest) =>
    apiRequest<ChannelMessageResponse>(`/api/v1/channel-messages/send/${channelId}`, {
      method: 'POST',
      body: JSON.stringify(data),
    }),

  edit: (messageId: string, data: MessageRequest) =>
    apiRequest<ChannelMessageResponse>(`/api/v1/channel-messages/edit/${messageId}`, {
      method: 'PUT',
      body: JSON.stringify(data),
    }),

  delete: (messageId: string) =>
    apiRequest<void>(`/api/v1/channel-messages/delete/${messageId}`, {
      method: 'DELETE',
    }),
}

export const dmApi = {
  conversation: (receiverId: string) =>
    apiRequest<DirectMessageResponse[]>(`/api/v1/dm/conversation/${receiverId}`),

  send: (receiverId: string, data: MessageRequest) =>
    apiRequest<DirectMessageResponse>(`/api/v1/dm/send/${receiverId}`, {
      method: 'POST',
      body: JSON.stringify(data),
    }),

  unread: () => apiRequest<DirectMessageResponse[]>('/api/v1/dm/unread'),

  unreadCounts: () => apiRequest<UnreadCountResponse[]>('/api/v1/dm/unread/counts'),

  markConversationRead: (senderId: string) =>
    apiRequest<void>(`/api/v1/dm/read/conversation/${senderId}`, { method: 'PUT' }),

  markRead: (messageId: string) =>
    apiRequest<void>(`/api/v1/dm/read/${messageId}`, { method: 'PUT' }),
}

export const userApi = {
  me: () => apiRequest<UserProfileResponse>('/api/v1/users/me'),

  updateMe: (data: UpdateUserRequest) =>
    apiRequest<UserProfileResponse>('/api/v1/users/me', {
      method: 'PUT',
      body: JSON.stringify(data),
    }),

  deleteMe: (data: DeleteAccountRequest) =>
    apiRequest<void>('/api/v1/users/me', {
      method: 'DELETE',
      body: JSON.stringify(data),
    }),

  search: (keyword: string) =>
    apiRequest<UserResponse[]>(`/api/v1/users/search?keyword=${encodeURIComponent(keyword)}`),
}

export const friendsApi = {
  list: () => apiRequest<UserResponse[]>('/api/v1/friends'),

  incomingRequests: () =>
    apiRequest<FriendRequestResponse[]>('/api/v1/friends/requests/incoming'),

  outgoingRequests: () =>
    apiRequest<FriendRequestResponse[]>('/api/v1/friends/requests/outgoing'),

  sendRequest: (receiverId: string) =>
    apiRequest<FriendRequestResponse>(`/api/v1/friends/requests/${receiverId}`, {
      method: 'POST',
    }),

  acceptRequest: (requestId: string) =>
    apiRequest<FriendRequestResponse>(`/api/v1/friends/requests/${requestId}/accept`, {
      method: 'POST',
    }),

  rejectRequest: (requestId: string) =>
    apiRequest<void>(`/api/v1/friends/requests/${requestId}/reject`, { method: 'POST' }),

  remove: (friendId: string) =>
    apiRequest<void>(`/api/v1/friends/${friendId}`, { method: 'DELETE' }),
}
