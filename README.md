# RealTimeChat

A full-stack real-time chat platform inspired by Slack. Users can register, create workspaces, join channels, message friends directly, and see live online status — powered by a Spring Boot REST + WebSocket backend and a React frontend.

![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.6-brightgreen)
![React](https://img.shields.io/badge/React-19-blue)
![License](https://img.shields.io/badge/license-educational-lightgrey)

---

## Live Deployment

| Component | Platform | URL |
|-----------|----------|-----|
| **Frontend** | Vercel | https://real-time-chat-application-sand.vercel.app/ |
| **Backend API** | Render | https://realtimechatapplication-dlzm.onrender.com/ |
| **Database** | Neon (PostgreSQL) | Managed cloud database |

> **Render free tier note:** The backend sleeps after ~15 minutes of inactivity. The first request after it wakes can take **30–60 seconds** to respond — this is expected on the free plan.

---

## Table of Contents

1. [Features](#features)
2. [Architecture](#architecture)
3. [Tech Stack](#tech-stack)
4. [Project Structure](#project-structure)
5. [Database Schema](#database-schema)
6. [Authentication](#authentication)
7. [REST API Reference](#rest-api-reference)
8. [WebSocket / Real-Time](#websocket--real-time)
9. [Business Rules](#business-rules)
10. [Local Development](#local-development)
11. [Production Deployment](#production-deployment)
12. [Environment Variables](#environment-variables)
13. [How to Use the App](#how-to-use-the-app)
14. [Troubleshooting](#troubleshooting)
15. [License](#license)

---

## Features

### Authentication & Accounts
- User **registration** with username, email, phone, password, and display name
- **Login** with email + password
- **JWT-based** stateless authentication
- **Session validation** on app load (stale tokens cleared automatically after a DB reset)
- **Profile page** — view personal details (email, phone, online status, member since)
- **Edit account** — update display name, username, email, phone, or password
- **Delete account** — permanently removes profile, friendships, messages, and owned workspaces

### Workspaces
- Create multiple **workspaces** (teams)
- Workspace **owner** is auto-added as admin member
- Owner can **add members** (must be accepted friends)
- View workspace **members** with online status
- Switch between workspaces from the sidebar dropdown

### Channels
- Workspace owner can create **public** or **private** channels
- **Public channels** — all workspace members can read/write; auto-joined on membership
- **Private channels** — require an explicit join
- Real-time channel messaging via WebSocket
- Each member's messages appear in a **distinct colored bubble** for easy identification
- Channel message **history** loads on open

### Direct Messages (DMs)
- DMs only between **accepted friends**
- **Friend request flow** — send → accept/reject → then chat
- Real-time DM delivery via WebSocket
- **Unread badge** per friend, plus a total count on the Friends header
- Badges clear automatically when a conversation is opened
- Chat layout: **your messages on the left (blue)**, **friend's messages on the right (purple)**

### Friends
- Search users by username or display name
- Send, accept, or reject friend requests
- **Live friend list updates** via WebSocket (no page refresh needed)
- Remove friends with a confirmation dialog

### Online Presence
- Real-time **online/offline** indicators (green dot)
- Broadcast via WebSocket to all connected clients
- Updated on WebSocket connect/disconnect

---

## Architecture

```
┌──────────────────────┐         REST (HTTPS)          ┌──────────────────────┐
│                      │ ──────────────────────────────▶│                      │
│    React Frontend    │          JSON + JWT            │  Spring Boot Backend │
│      (Vercel)        │◀────────────────────────────── │      (Render)        │
│                      │                                 │                      │
└──────────┬───────────┘                                └──────────┬───────────┘
           │                                                        │
           │           WebSocket (STOMP + SockJS)                   │
           │────────────────────────────────────────────────────────▶
           │◀────────────────────────────────────────────────────────
           │     Real-time messages, presence, friend events         │
           │                                                         │
           │                                              ┌──────────▼──────────┐
           │                                              │  PostgreSQL (Neon)   │
           │                                              │  Users, Workspaces,  │
           │                                              │  Channels, Messages  │
           │                                              └──────────────────────┘
```

### Request Flow
1. User logs in → backend returns a JWT
2. Frontend stores the token in `localStorage`
3. All REST calls include `Authorization: Bearer <token>`
4. WebSocket connects to `/ws` with the JWT in the STOMP connect headers
5. Messages sent over WebSocket are persisted to PostgreSQL and pushed to subscribers

---

## Tech Stack

### Frontend
| Technology | Purpose |
|------------|---------|
| React 19 | UI framework |
| TypeScript | Type safety |
| Vite 8 | Build tool & dev server |
| @stomp/stompjs | STOMP WebSocket client |
| sockjs-client | WebSocket transport fallback |

### Backend
| Technology | Purpose |
|------------|---------|
| Java 21 | Runtime |
| Spring Boot 4.0.6 | Application framework |
| Spring Security + JWT | Authentication |
| Spring WebSocket + STOMP | Real-time messaging |
| Spring Data JPA / Hibernate | ORM |
| PostgreSQL | Database |
| Lombok | Boilerplate reduction |
| BCrypt | Password hashing |

### Infrastructure
| Service | Role |
|---------|------|
| **Vercel** | Frontend hosting (static SPA) |
| **Render** | Backend hosting (Java web service) |
| **Neon** | Managed PostgreSQL |

---

## Project Structure

```
RealTimeChat/
├── frontend/
│   ├── src/
│   │   ├── api/
│   │   │   ├── client.ts          # Fetch wrapper, JWT headers, 401 handling
│   │   │   └── index.ts           # All REST API functions
│   │   ├── auth/
│   │   │   └── storage.ts         # localStorage helpers
│   │   ├── components/
│   │   │   ├── ChatApp.tsx        # Main app shell & state
│   │   │   ├── Sidebar.tsx        # Workspaces, channels, friends
│   │   │   ├── MessageList.tsx    # Chat bubbles (DM + channel modes)
│   │   │   ├── MessageInput.tsx   # Message compose box
│   │   │   ├── AuthPage.tsx       # Login / register
│   │   │   ├── AccountModal.tsx   # Profile view / edit / delete
│   │   │   ├── AddFriendModal.tsx
│   │   │   ├── AddMemberModal.tsx
│   │   │   ├── CreateWorkspaceModal.tsx
│   │   │   └── CreateChannelModal.tsx
│   │   ├── context/
│   │   │   └── AuthContext.tsx    # Auth state & session bootstrap
│   │   ├── services/
│   │   │   └── websocket.ts       # STOMP client singleton
│   │   └── types/
│   │       └── index.ts           # TypeScript interfaces
│   ├── vite.config.ts             # Dev proxy to localhost:8080
│   └── package.json
│
├── src/main/java/com/example/realtimechat/
│   ├── config/
│   │   ├── SecurityConfig.java    # JWT filter, CORS, route permissions
│   │   ├── WebSocketConfig.java   # STOMP broker, JWT on CONNECT
│   │   ├── JwtAuthFilter.java     # Validates JWT on every request
│   │   └── CorsConfig.java
│   ├── controller/                # REST + WebSocket message handlers
│   ├── service/                   # Business logic
│   ├── repository/                # Spring Data JPA repos
│   ├── entity/                    # JPA entities
│   ├── dto/                       # Request/response objects
│   ├── event/                     # Application events (online status, friends)
│   └── util/
│       └── JwtUtil.java
│
├── src/main/resources/
│   └── application.properties
├── pom.xml
└── README.md
```

---

## Database Schema

Hibernate auto-creates and updates tables (`ddl-auto=update`) on startup.

| Table | Description |
|-------|-------------|
| `users` | Accounts (username, email, phone, password hash, online status) |
| `workspaces` | Teams with an owner |
| `workspace_members` | User ↔ workspace membership with role (ADMIN/MEMBER) |
| `channels` | Text channels inside a workspace (public or private) |
| `channel_members` | User ↔ channel membership |
| `channel_messages` | Messages sent in channels |
| `direct_messages` | Private messages between two users |
| `friend_requests` | Friend request lifecycle (PENDING → ACCEPTED/REJECTED) |

---

## Authentication

- Passwords are hashed with **BCrypt** before storage
- On login/register, the backend returns a **JWT** containing the user's email
- The token is sent as `Authorization: Bearer <token>` on all protected REST calls
- The WebSocket STOMP **CONNECT** frame includes the same JWT header
- Token expiry: **24 hours** (`86400000` ms)
- On app load, the frontend calls `GET /api/v1/users/me` to validate the stored session
- Invalid or expired tokens trigger automatic logout and a redirect to the login screen

---

## REST API Reference

Base URL (production): `https://realtimechatapplication-dlzm.onrender.com`

All protected endpoints require: `Authorization: Bearer <jwt>`

### Auth — `/api/v1/auth`

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | `/register` | No | Create account |
| POST | `/login` | No | Login, returns JWT |

**Register body:**
```json
{
  "username": "johndoe",
  "email": "john@example.com",
  "password": "secret123",
  "phone": "9876543210",
  "displayName": "John Doe"
}
```

### Users — `/api/v1/users`

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/me` | Get own profile |
| PUT | `/me` | Update own profile |
| DELETE | `/me` | Delete own account (requires password in body) |
| GET | `/{userId}` | Get user by ID |
| GET | `/username/{username}` | Get user by username |
| GET | `/search?keyword=` | Search users |

### Workspaces — `/api/v1/workspaces`

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/` | List my workspaces |
| POST | `/` | Create workspace |
| GET | `/{workspaceId}` | Get workspace details |
| GET | `/{workspaceId}/members` | List members |
| POST | `/{workspaceId}/members/{userId}` | Add member (owner only, must be friend) |
| DELETE | `/{workspaceId}/members/{userId}` | Remove member |

### Channels — `/api/v1/channels`

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/workspace/{workspaceId}` | List channels in workspace |
| POST | `/workspace/{workspaceId}` | Create channel (owner only) |
| GET | `/{channelId}` | Get channel details |
| POST | `/{channelId}/join` | Join a channel |
| DELETE | `/{channelId}/leave` | Leave a channel |
| GET | `/{channelId}/members` | List channel members |

### Channel Messages — `/api/v1/channel-messages`

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/{channelId}` | Get message history |
| POST | `/send/{channelId}` | Send message (REST fallback) |
| PUT | `/edit/{messageId}` | Edit a message |
| DELETE | `/delete/{messageId}` | Delete a message |

### Direct Messages — `/api/v1/dm`

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/conversation/{friendId}` | Get full conversation (auto marks read) |
| POST | `/send/{receiverId}` | Send a DM |
| GET | `/unread` | List all unread DMs |
| GET | `/unread/counts` | Unread count grouped by sender |
| PUT | `/read/conversation/{senderId}` | Mark all messages from sender as read |
| PUT | `/read/{messageId}` | Mark single message as read |

### Friends — `/api/v1/friends`

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/` | List accepted friends |
| GET | `/requests/incoming` | Pending requests received |
| GET | `/requests/outgoing` | Pending requests sent |
| POST | `/requests/{receiverId}` | Send friend request |
| POST | `/requests/{requestId}/accept` | Accept request |
| POST | `/requests/{requestId}/reject` | Reject request |
| DELETE | `/{friendId}` | Remove friend |

---

## WebSocket / Real-Time

**Endpoint:** `wss://realtimechatapplication-dlzm.onrender.com/ws` (SockJS)

**Connect headers:**
```
Authorization: Bearer <jwt>
```

### Subscribe (receive)

| Destination | Purpose |
|-------------|---------|
| `/topic/channel.{channelId}` | Channel messages |
| `/user/queue/messages` | Direct messages to you |
| `/user/queue/friends` | Friend request events |
| `/topic/online-status` | Online/offline broadcasts |

### Send (publish)

| Destination | Purpose |
|-------------|---------|
| `/app/channel.send/{channelId}` | Send channel message |
| `/app/dm.send/{receiverId}` | Send DM |

### Friend WebSocket Events

| Event type | Trigger |
|------------|---------|
| `INCOMING_REQUEST` | Someone sent you a friend request |
| `ACCEPTED` | Someone accepted your friend request |
| `REJECTED` | Someone rejected your friend request |
| `REMOVED` | Someone removed you as a friend |

---

## Business Rules

| Rule | Detail |
|------|--------|
| Workspace creation | Any authenticated user can create a workspace; creator becomes owner |
| Channel creation | **Only the workspace owner** can create channels |
| Add workspace member | **Only the owner** can add members; target must be an **accepted friend** |
| Public channels | All workspace members are auto-joined and can send messages immediately |
| Private channels | Must be explicitly joined before messaging |
| Direct messages | Only allowed between **accepted friends** |
| Friend requests | Must be accepted before chatting or adding to a workspace |
| Unread badges | Updated via WebSocket on new DM; cleared when the conversation is opened |
| Account deletion | Removes user data, friendships, messages, and **owned workspaces** |

---

## Local Development

### Prerequisites

- **Java 21+**
- **Node.js 18+**
- **PostgreSQL** (local install or free Neon project)
- **Maven** (included via the `./mvnw` wrapper)

### Step 1 — Database

**Option A: Local PostgreSQL**
```sql
CREATE DATABASE "RealTimeChat";
```

**Option B: Neon**
1. Create a free project at https://neon.tech
2. Copy the JDBC connection string

### Step 2 — Backend Configuration

Edit `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/RealTimeChat
spring.datasource.username=postgres
spring.datasource.password=postgres
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
jwt.secret=404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970
jwt.expiration=86400000
server.port=8080
```

> **Note:** the `jwt.secret` above is a placeholder for local development only. Never commit a production secret — use an environment variable instead (see [Environment Variables](#environment-variables)).

Start the backend:
```bash
./mvnw spring-boot:run
```

Verify it's running: open http://localhost:8080 — a `403` response means the server is up (the root route just isn't public).

### Step 3 — Frontend

```bash
cd frontend
npm install
npm run dev
```

Open http://localhost:5173.

The Vite dev server proxies:
- `/api/*` → `http://localhost:8080`
- `/ws/*` → `http://localhost:8080` (WebSocket)

No `VITE_API_URL` is needed locally — leave it empty.

### Step 4 — Test Locally

1. Register two accounts (use two browser windows or an incognito tab)
2. User A sends a friend request to User B
3. User B accepts → both see each other in their Friends list
4. User A creates a workspace, then creates a channel
5. User A adds User B to the workspace (owner only)
6. Both users can message in the channel and DM each other

---

## Production Deployment

### Database — Neon

1. Go to https://neon.tech and create a project
2. Create a database (e.g. `realtimechat`)
3. From the dashboard, copy the **host**, **database name**, **user**, and **password**
4. Build the JDBC URL:
   ```
   jdbc:postgresql://<host>/<dbname>?sslmode=require
   ```
5. Use the **pooled connection** endpoint for cloud backends (recommended for Render)

### Backend — Render

1. Push your code to GitHub
2. Go to https://render.com → **New → Web Service**
3. Connect your GitHub repo
4. Configure:

   | Setting | Value |
   |---------|-------|
   | **Root Directory** | _(leave empty — repo root)_ |
   | **Runtime** | Docker or Native (Java) |
   | **Build Command** | `./mvnw clean package -DskipTests` |
   | **Start Command** | `java -jar target/RealTimeChat-0.0.1-SNAPSHOT.jar` |
   | **Instance Type** | Free |

5. Add the environment variables listed in the table below
6. Deploy, then note your URL: `https://realtimechatapplication-dlzm.onrender.com`

### Frontend — Vercel

1. Go to https://vercel.com → **Add New Project**
2. Import your GitHub repo
3. Configure:

   | Setting | Value |
   |---------|-------|
   | **Root Directory** | `frontend` |
   | **Framework Preset** | Vite |
   | **Build Command** | `npm run build` |
   | **Output Directory** | `dist` |

4. Add the environment variable `VITE_API_URL` set to your Render backend URL (no trailing slash) — this tells the frontend where to send REST API calls
5. Deploy, then note your URL: `https://real-time-chat-application-sand.vercel.app/`

---

## Environment Variables

| Variable | Where | Required | Description |
|----------|-------|----------|--------------|
| `VITE_API_URL` | Vercel | Yes (prod) | Backend base URL |
| `SPRING_DATASOURCE_URL` | Render | Yes | Neon JDBC URL |
| `SPRING_DATASOURCE_USERNAME` | Render | Yes | DB username |
| `SPRING_DATASOURCE_PASSWORD` | Render | Yes | DB password |
| `JWT_SECRET` | Render | Yes | JWT signing key (base64, 256-bit minimum) |
| `JWT_EXPIRATION` | Render | No | Default `86400000` (24h) |
| `SPRING_JPA_HIBERNATE_DDL_AUTO` | Render | No | Default `update` |
| `SPRING_JPA_SHOW_SQL` | Render | No | Set to `false` in production |

---

## How to Use the App

### First-time setup (new account)
1. Open https://real-time-chat-application-sand.vercel.app/
2. Click **Register** and fill in username, email, phone, and password
3. After logging in you'll see an empty sidebar — **create a workspace first**
4. Click **+** next to the workspace dropdown and name your workspace

### Adding friends
1. Click **+** next to **Friends** in the sidebar
2. Search by username
3. Send a friend request
4. The other user accepts it from their **Requests** section
5. The friend appears in both users' Friends lists automatically — no refresh needed

### Workspace & channels
1. Select your workspace from the dropdown
2. Click **+** next to **Channels** to create one (owner only)
3. Choose public or private
4. Click a channel to open it and start messaging
5. To add a friend to your workspace: click **+** next to **Members** (owner only)

### Direct messages
1. Click a friend in the **Friends** list
2. Type a message in the input box at the bottom
3. An unread count badge appears when you receive a message while away
4. Opening the chat clears the badge

### Account settings
1. Click your **name/avatar** at the bottom of the sidebar
2. View your profile, edit your details, or delete your account

---

## License

This project is built for **educational and portfolio purposes**.

---

## Author

Deployed on **Vercel + Render + Neon** — a full-stack real-time chat application.
