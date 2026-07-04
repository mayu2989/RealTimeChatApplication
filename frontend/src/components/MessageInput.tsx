import { useState } from 'react'
import './MessageInput.css'

interface MessageInputProps {
  placeholder?: string
  onSend: (content: string) => void
  disabled?: boolean
}

export function MessageInput({
  placeholder = 'Type a message…',
  onSend,
  disabled = false,
}: MessageInputProps) {
  const [content, setContent] = useState('')

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    const trimmed = content.trim()
    if (!trimmed || disabled) return
    onSend(trimmed)
    setContent('')
  }

  function handleKeyDown(e: React.KeyboardEvent) {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault()
      handleSubmit(e)
    }
  }

  return (
    <form className="message-input" onSubmit={handleSubmit}>
      <textarea
        value={content}
        onChange={(e) => setContent(e.target.value)}
        onKeyDown={handleKeyDown}
        placeholder={placeholder}
        disabled={disabled}
        rows={1}
      />
      <button type="submit" disabled={disabled || !content.trim()} aria-label="Send message">
        ➤
      </button>
    </form>
  )
}
