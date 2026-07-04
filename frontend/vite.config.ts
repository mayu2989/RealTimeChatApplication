import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import type { IncomingMessage } from 'node:http'
import type { Socket } from 'node:net'

function suppressProxyResetErrors(proxy: {
  on(event: 'error', listener: (err: NodeJS.ErrnoException, req: IncomingMessage, res: unknown) => void): void
  on(event: 'proxyReqWs', listener: (proxyReq: unknown, req: IncomingMessage, socket: Socket) => void): void
}) {
  proxy.on('error', (err, _req, res) => {
    if (err.code === 'ECONNRESET' || err.code === 'ECONNREFUSED') {
      if (res && typeof (res as { writeHead?: unknown }).writeHead === 'function') {
        ;(res as { writeHead: (code: number) => void; end: () => void }).writeHead(502)
        ;(res as { end: () => void }).end()
      }
      return
    }
    console.error('[vite] proxy error:', err)
  })

  proxy.on('proxyReqWs', (_proxyReq, _req, socket) => {
    socket.on('error', (err: NodeJS.ErrnoException) => {
      if (err.code === 'ECONNRESET' || err.code === 'EPIPE') return
      console.error('[vite] ws proxy socket error:', err)
    })
  })
}

export default defineConfig({
  plugins: [react()],
  define: {
    global: 'globalThis',
  },
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        configure: suppressProxyResetErrors,
      },
      '/ws': {
        target: 'http://localhost:8080',
        ws: true,
        changeOrigin: true,
        configure: suppressProxyResetErrors,
      },
    },
  },
})
