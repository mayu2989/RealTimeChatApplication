# RealTimeChat Frontend

React + TypeScript frontend for the RealTimeChat backend.

## Features

- User registration and login (JWT)
- Workspace and channel management
- Real-time channel messaging via WebSocket (STOMP)
- Direct messages with live delivery
- Online presence indicators
- Slack-inspired dark UI

## Prerequisites

- Node.js 18+
- Backend running on `http://localhost:8080`
- PostgreSQL database configured for the backend

## Getting Started

```bash
# Install dependencies
npm install

# Start dev server (proxies API & WebSocket to backend)
npm run dev
```

Open [http://localhost:5173](http://localhost:5173).

## Backend

Start the Spring Boot backend first:

```bash
cd ..
./mvnw spring-boot:run
```

## Environment

| Variable | Default | Description |
|----------|---------|-------------|
| `VITE_API_URL` | _(empty)_ | API base URL. Leave empty to use Vite dev proxy. |

## Build

```bash
npm run build
npm run preview
```
