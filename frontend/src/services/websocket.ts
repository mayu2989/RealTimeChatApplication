import { Client, type IMessage, type StompSubscription } from '@stomp/stompjs'
import type {
  ChannelMessageResponse,
  DirectMessageResponse,
  FriendUpdateEvent,
  OnlineStatusEvent,
} from '../types'

type ChannelMessageHandler = (message: ChannelMessageResponse) => void
type DirectMessageHandler = (message: DirectMessageResponse) => void
type OnlineStatusHandler = (event: OnlineStatusEvent) => void
type FriendUpdateHandler = (event: FriendUpdateEvent) => void

class WebSocketService {
  private client: Client | null = null
  private channelSubscriptions = new Map<string, StompSubscription>()
  private dmSubscription: StompSubscription | null = null
  private onlineSubscription: StompSubscription | null = null
  private friendSubscription: StompSubscription | null = null
  private onConnectCallbacks: Array<() => void> = []

  private dmHandler: DirectMessageHandler | null = null
  private onlineHandler: OnlineStatusHandler | null = null
  private friendHandler: FriendUpdateHandler | null = null

  connect(token: string): Promise<void> {
    if (this.client?.connected) {
      return Promise.resolve()
    }

    if (this.client) {
      this.client.deactivate()
      this.client = null
    }

    return new Promise((resolve) => {
      let settled = false
      const finish = () => {
        if (!settled) {
          settled = true
          resolve()
        }
      }

      const timeout = setTimeout(finish, 8000)

      // Convert http/https to ws/wss for native WebSocket
      const wsUrl = (import.meta.env.VITE_WS_URL ?? 'ws://localhost:8080/ws')
        .replace('https://', 'wss://')
        .replace('http://', 'ws://')

      this.client = new Client({
        brokerURL: wsUrl,
        connectHeaders: {
          Authorization: `Bearer ${token}`,
        },
        reconnectDelay: 5000,
        heartbeatIncoming: 10000,
        heartbeatOutgoing: 10000,
        onConnect: () => {
          clearTimeout(timeout)
          this.setupPersistentSubscriptions()
          this.onConnectCallbacks.forEach((cb) => cb())
          finish()
        },
        onStompError: (frame) => {
          console.error('STOMP error:', frame.headers['message'])
        },
        onWebSocketError: () => {
          // Transient errors during reconnect/refresh are expected
        },
        onDisconnect: () => {
          this.dmSubscription = null
          this.onlineSubscription = null
          this.friendSubscription = null
        },
      })

      this.client.activate()
    })
  }

  onConnect(callback: () => void) {
    this.onConnectCallbacks.push(callback)
    if (this.client?.connected) callback()
  }

  disconnect() {
    this.channelSubscriptions.forEach((sub) => sub.unsubscribe())
    this.channelSubscriptions.clear()
    this.dmSubscription?.unsubscribe()
    this.onlineSubscription?.unsubscribe()
    this.friendSubscription?.unsubscribe()
    this.dmSubscription = null
    this.onlineSubscription = null
    this.friendSubscription = null
    this.dmHandler = null
    this.onlineHandler = null
    this.friendHandler = null
    this.onConnectCallbacks = []

    if (this.client) {
      this.client.deactivate()
      this.client = null
    }
  }

  private setupPersistentSubscriptions() {
    if (!this.client?.connected) return

    if (this.dmHandler) {
      this.dmSubscription?.unsubscribe()
      this.dmSubscription = this.client.subscribe(
        '/user/queue/messages',
        (message: IMessage) => {
          this.dmHandler?.(JSON.parse(message.body) as DirectMessageResponse)
        },
      )
    }

    if (this.onlineHandler) {
      this.onlineSubscription?.unsubscribe()
      this.onlineSubscription = this.client.subscribe(
        '/topic/online-status',
        (message: IMessage) => {
          this.onlineHandler?.(JSON.parse(message.body) as OnlineStatusEvent)
        },
      )
    }

    if (this.friendHandler) {
      this.friendSubscription?.unsubscribe()
      this.friendSubscription = this.client.subscribe(
        '/user/queue/friends',
        (message: IMessage) => {
          this.friendHandler?.(JSON.parse(message.body) as FriendUpdateEvent)
        },
      )
    }
  }

  subscribeToChannel(channelId: string, handler: ChannelMessageHandler) {
    if (!this.client?.connected) return

    this.unsubscribeFromChannel(channelId)

    const subscription = this.client.subscribe(
      `/topic/channel.${channelId}`,
      (message: IMessage) => {
        handler(JSON.parse(message.body) as ChannelMessageResponse)
      },
    )
    this.channelSubscriptions.set(channelId, subscription)
  }

  unsubscribeFromChannel(channelId: string) {
    const sub = this.channelSubscriptions.get(channelId)
    if (sub) {
      sub.unsubscribe()
      this.channelSubscriptions.delete(channelId)
    }
  }

  setDirectMessageHandler(handler: DirectMessageHandler) {
    this.dmHandler = handler
    if (this.client?.connected) {
      this.dmSubscription?.unsubscribe()
      this.dmSubscription = this.client.subscribe(
        '/user/queue/messages',
        (message: IMessage) => {
          handler(JSON.parse(message.body) as DirectMessageResponse)
        },
      )
    }
  }

  setOnlineStatusHandler(handler: OnlineStatusHandler) {
    this.onlineHandler = handler
    if (this.client?.connected) {
      this.onlineSubscription?.unsubscribe()
      this.onlineSubscription = this.client.subscribe(
        '/topic/online-status',
        (message: IMessage) => {
          handler(JSON.parse(message.body) as OnlineStatusEvent)
        },
      )
    }
  }

  setFriendUpdateHandler(handler: FriendUpdateHandler) {
    this.friendHandler = handler
    if (this.client?.connected) {
      this.friendSubscription?.unsubscribe()
      this.friendSubscription = this.client.subscribe(
        '/user/queue/friends',
        (message: IMessage) => {
          handler(JSON.parse(message.body) as FriendUpdateEvent)
        },
      )
    }
  }

  sendChannelMessage(channelId: string, content: string) {
    this.client?.publish({
      destination: `/app/channel.send/${channelId}`,
      body: JSON.stringify({ content, messageType: 'TEXT' }),
    })
  }

  get connected() {
    return this.client?.connected ?? false
  }
}

export const websocketService = new WebSocketService()

if (typeof window !== 'undefined') {
  window.addEventListener('pagehide', () => {
    websocketService.disconnect()
  })
}
