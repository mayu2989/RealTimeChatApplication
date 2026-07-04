import { useEffect, useRef } from 'react'
import type { ChannelMessageResponse, DirectMessageResponse } from '../types'
import './MessageList.css'

interface MessageListProps {
  messages: (ChannelMessageResponse | DirectMessageResponse)[]
  currentUserId: string
  mode: 'dm' | 'channel'
  emptyText?: string
}

function formatTime(dateStr: string) {
  const date = new Date(dateStr)
  return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
}

function formatDate(dateStr: string) {
  const date = new Date(dateStr)
  const today = new Date()
  if (date.toDateString() === today.toDateString()) return 'Today'
  const yesterday = new Date(today)
  yesterday.setDate(yesterday.getDate() - 1)
  if (date.toDateString() === yesterday.toDateString()) return 'Yesterday'
  return date.toLocaleDateString([], { month: 'short', day: 'numeric', year: 'numeric' })
}

const MEMBER_BUBBLE_COLORS = [
  '#1e3a5f',
  '#3d2a5c',
  '#1f4d3a',
  '#5c3d1e',
  '#4a2c2c',
  '#2c4a4a',
  '#4a3d2c',
  '#2c3d4a',
]

function bubbleColorForSender(senderId: string): string {
  let hash = 0
  for (let i = 0; i < senderId.length; i++) {
    hash = senderId.charCodeAt(i) + ((hash << 5) - hash)
  }
  return MEMBER_BUBBLE_COLORS[Math.abs(hash) % MEMBER_BUBBLE_COLORS.length]
}

export function MessageList({
  messages,
  currentUserId,
  mode,
  emptyText = 'No messages yet',
}: MessageListProps) {
  const bottomRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages])

  if (messages.length === 0) {
    return (
      <div className="message-list empty">
        <p>{emptyText}</p>
      </div>
    )
  }

  let prevDate = ''

  return (
    <div className={`message-list ${mode}`}>
      {messages.map((msg) => {
        const dateLabel = formatDate(msg.createdAt)
        const showDateDivider = dateLabel !== prevDate
        prevDate = dateLabel
        const isOwn = msg.senderId === currentUserId
        const isEdited = 'isEdited' in msg && msg.isEdited

        if (mode === 'dm') {
          return (
            <div key={msg.id}>
              {showDateDivider && (
                <div className="date-divider">
                  <span>{dateLabel}</span>
                </div>
              )}
              <div className={`message dm ${isOwn ? 'own' : 'other'}`}>
                <div className="dm-bubble">
                  <div className="dm-bubble-meta">
                    <span className="message-author">{msg.senderUsername}</span>
                    <span className="message-time">{formatTime(msg.createdAt)}</span>
                    {isEdited && <span className="message-edited">(edited)</span>}
                  </div>
                  <div className="message-content">{msg.content}</div>
                </div>
              </div>
            </div>
          )
        }

        const bubbleColor = bubbleColorForSender(msg.senderId)

        return (
          <div key={msg.id}>
            {showDateDivider && (
              <div className="date-divider">
                <span>{dateLabel}</span>
              </div>
            )}
            <div className="message channel">
              <div
                className="channel-bubble"
                style={{ backgroundColor: bubbleColor }}
              >
                <div className="message-avatar">
                  {msg.senderUsername.charAt(0).toUpperCase()}
                </div>
                <div className="message-body">
                  <div className="message-meta">
                    <span className="message-author">{msg.senderUsername}</span>
                    <span className="message-time">{formatTime(msg.createdAt)}</span>
                    {isEdited && <span className="message-edited">(edited)</span>}
                  </div>
                  <div className="message-content">{msg.content}</div>
                </div>
              </div>
            </div>
          </div>
        )
      })}
      <div ref={bottomRef} />
    </div>
  )
}
