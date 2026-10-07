# Flutter API Handoff Specification

**Application:** `chat-backend` (`com.rindev:chat-backend:0.0.1-SNAPSHOT`)  
**Backend Framework:** Spring Boot 4.1.1 / Java 21 / Spring Security 7  
**Transport:** HTTP/1.1 REST (JSON & Multipart) + WebSocket (STOMP 1.2)  
**Specification Version:** Phase 26 Complete  
**Last Reconciled:** 2026-10-07  

---

## 1. Overview & Connection Architecture

The backend provides a complete set of versioned REST APIs (`/api/v1/...`) and a secure WebSocket/STOMP gateway (`/ws`) designed specifically for the Flutter client application.

### 1.1 Base URLs & Endpoints

| Environment | REST Base URL | WebSocket STOMP URL | OpenAPI Documentation |
|:---|:---|:---|:---|
| **Local Development** | `http://localhost:8080` | `ws://localhost:8080/ws` | `http://localhost:8080/v3/api-docs`<br>`http://localhost:8080/swagger-ui/index.html` |
| **Production** | Reverse Proxy / CDN (TLS) | `wss://<domain>/ws` | Disabled by default (opt-in via configuration) |

### 1.2 Content Negotiation & Media Types
- Standard REST requests and responses use `Content-Type: application/json` and `Accept: application/json`.
- File attachments use `Content-Type: multipart/form-data`.
- All timestamps in JSON responses are serialized as standard UTC ISO-8601 strings (e.g., `2026-10-07T09:00:00Z` or `2026-10-07T09:00:00`).

### 1.3 Uniform Envelope Format

#### Success Envelope (`ApiResponse<T>`)
Every successful HTTP response (except `204 No Content`) is wrapped in a standard JSON envelope:
```json
{
  "success": true,
  "code": "OK",
  "message": "Request successful",
  "timestamp": "2026-10-07T09:00:00.123456Z",
  "data": { ... }
}
```
*Note:* For resources created via `201 Created`, `"code"` is `"CREATED"` and `"message"` is `"Resource created"`.

#### HTTP 204 No Content
Operations returning `204 No Content` (e.g., delete message, leave conversation, read-all notifications) have an **empty body** (`Content-Length: 0`). The Flutter HTTP client should check for `status == 204` before attempting JSON deserialization.

#### Error Envelope (`ErrorResponse`)
All errors (including Spring Security 401/403 errors and validation errors) return an identical structure:
```json
{
  "success": false,
  "code": "VALIDATION_ERROR",
  "message": "Validation failed",
  "timestamp": "2026-10-07T09:00:00.123456Z",
  "errors": {
    "username": "This field is required",
    "password": "Password must be at least 8 characters"
  }
}
```
Standard Error Codes:
- `VALIDATION_ERROR` (HTTP 400): Request DTO validation failed; individual field errors are mapped in `errors`.
- `BAD_REQUEST` (HTTP 400): Malformed JSON, missing parameter, illegal argument, or invalid enum string.
- `UNAUTHORIZED` (HTTP 401): Missing, malformed, or expired Bearer access token; invalid login credentials. Always emits `WWW-Authenticate: Bearer` and `Cache-Control: no-store`.
- `FORBIDDEN` (HTTP 403): User is blocked, not a conversation member, or lacks required administrative permissions.
- `NOT_FOUND` (HTTP 404): Requested conversation, message, user, or attachment was not found.
- `CONFLICT` (HTTP 409): Resource already exists (e.g., duplicate username, email, or already added reaction).
- `INTERNAL_SERVER_ERROR` (HTTP 500): Safe fallback (`"An unexpected error occurred"`).

---

## 2. Authentication & Authorization Lifecycle

Stateless authentication uses HMAC-SHA256 (HS256) JSON Web Tokens (JWT) for access authorization and database-persisted refresh tokens for session renewal.

### 2.1 Token Properties
- **Access Token:** Short-lived JWT (default 15 minutes / 900 seconds). Claims contain `sub` (userId as string).
- **Refresh Token:** Cryptographically random UUID token stored in database with expiration (default 30 days). Revoked upon logout or replacement.
- **Authorization Header:** Every authenticated REST endpoint requires:
  ```http
  Authorization: Bearer <accessToken>
  ```

### 2.2 Endpoints

#### `POST /api/v1/auth/register` (Public)
Creates a new user account.
- **Status:** `201 Created`
- **Request Body:**
  ```json
  {
    "name": "Alice Smith",
    "username": "alice",
    "email": "alice@example.test",
    "password": "ExamplePassword123!"
  }
  ```
- **Response Data (`AuthResponse`):**
  ```json
  {
    "success": true,
    "code": "CREATED",
    "message": "Resource created",
    "timestamp": "2026-10-07T09:00:00Z",
    "data": {
      "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.synthetic-access-token",
      "refreshToken": "synthetic-refresh-uuid-0001",
      "tokenType": "Bearer",
      "expiresIn": 900,
      "refreshExpiresAt": "2026-11-06T09:00:00Z",
      "user": {
        "id": 1,
        "name": "Alice Smith",
        "username": "alice",
        "email": "alice@example.test",
        "phone": null,
        "avatarUrl": null,
        "bio": null,
        "status": "OFFLINE",
        "lastSeenAt": null,
        "createdAt": "2026-10-07T09:00:00",
        "updatedAt": "2026-10-07T09:00:00"
      }
    }
  }
  ```

#### `POST /api/v1/auth/login` (Public)
Authenticates user with username and password.
- **Status:** `200 OK`
- **Request Body:**
  ```json
  {
    "username": "alice",
    "password": "ExamplePassword123!"
  }
  ```
- **Response Data:** `AuthResponse` (same structure as register).

#### `POST /api/v1/auth/refresh` (Public)
Exchanges an active refresh token for a new access token and a rotated refresh token.
- **Status:** `200 OK`
- **Request Body:**
  ```json
  {
    "refreshToken": "synthetic-refresh-uuid-0001"
  }
  ```
- **Response Data:** `AuthResponse` containing new access and refresh tokens.

#### `POST /api/v1/auth/logout` (Authenticated)
Revokes the refresh token session in the database.
- **Status:** `200 OK`
- **Headers:** `Authorization: Bearer <accessToken>`
- **Request Body:**
  ```json
  {
    "refreshToken": "synthetic-refresh-uuid-0001"
  }
  ```
- **Response Body:** `{"success": true, "code": "OK", "message": "Request successful", "data": null}`
- **Security & Lifecycle Rule:**
  - If the refresh token is missing, expired, or belongs to another user account, returns `401 Unauthorized`.
  - The JWT access token is stateless and remains cryptographically valid until its 15-minute expiry. The Flutter client **MUST purge the access token and refresh token from local storage immediately**.

#### `GET /api/v1/auth/me` (Authenticated)
Returns private profile information for the authenticated user.
- **Status:** `200 OK`
- **Response Data:** `UserResponse`

---

## 3. Users, Profiles & Settings

### 3.1 Users (`/api/v1/users`)
- `GET /api/v1/users?page=0&size=20`: List public user profiles (paginated via Spring Data `Page<UserPublicResponse>`).
- `GET /api/v1/users/search?q={query}&page=0&size=20`: Search users by display name or username.
- `GET /api/v1/users/me`: Current user private profile (`UserResponse`).
- `PATCH /api/v1/users/me`: Update profile fields (`name`, `bio`, `phone`).
- `PATCH /api/v1/users/me/avatar`: Update avatar URL (`{"avatarUrl": "https://..."}`).
- `GET /api/v1/users/{id}`: Public user profile (`UserPublicResponse`).

### 3.2 User Settings (`/api/v1/settings`)
- `GET /api/v1/settings`: Fetch user preferences (`UserSettingResponse`).
- `PATCH /api/v1/settings`: Update settings:
  ```json
  {
    "theme": "DARK",
    "messageNotifications": true,
    "groupNotifications": true,
    "reactionNotifications": true,
    "readReceipts": true,
    "lastSeenPrivacy": "EVERYONE",
    "profilePhotoPrivacy": "CONTACTS",
    "groupAddPrivacy": "NOBODY"
  }
  ```
  - `theme`: `LIGHT`, `DARK`, `SYSTEM`
  - Privacy levels: `EVERYONE`, `CONTACTS`, `NOBODY`

### 3.3 Blocking (`/api/v1/users/{id}/block`, `/api/v1/users/me/blocked`)
- `POST /api/v1/users/{id}/block`: Block user. Returns `201 Created` with `BlockedUserResponse`.
- `DELETE /api/v1/users/{id}/block`: Unblock user. Returns `204 No Content`.
- `GET /api/v1/users/me/blocked`: List blocked users. Returns `200 OK` with `List<BlockedUserResponse>`.

---

## 4. Conversations & Membership

### 4.1 Conversation Types & Roles
- **Conversation Types:** `DIRECT` (1-on-1 private chat), `GROUP` (group chat).
- **Member Roles:** `OWNER`, `ADMIN`, `MEMBER`.

### 4.2 Endpoints
- `POST /api/v1/conversations`: Create a conversation.
  - **Status:** `201 Created`
  - **Direct Chat Request:**
    ```json
    {
      "type": "DIRECT",
      "memberId": 2
    }
    ```
  - **Group Chat Request:**
    ```json
    {
      "type": "GROUP",
      "name": "Mobile Dev Team",
      "description": "Flutter and Backend Sync",
      "memberIds": [2, 3, 4]
    }
    ```
- `GET /api/v1/conversations`: List active conversations for current user. Returns `200 OK` with `List<ConversationResponse>`.
- `GET /api/v1/conversations/{id}`: Get conversation details. Returns `200 OK` with `ConversationResponse`.
- `PATCH /api/v1/conversations/{id}`: Update group title/description/avatar (Admin/Owner only). Returns `200 OK`.
- `GET /api/v1/conversations/{conversationId}/members`: List conversation members (`List<ConversationMemberResponse>`).
- `POST /api/v1/conversations/{conversationId}/members`: Add a user to a group conversation (`{"userId": 5}`). Returns `201 Created`.
- `DELETE /api/v1/conversations/{conversationId}/members/{userId}`: Remove a member or leave conversation. Returns `204 No Content`.
- `PATCH /api/v1/conversations/{conversationId}/members/{userId}/role`: Change role (`{"role": "ADMIN"}`). Returns `200 OK`.

---

## 5. Messaging & Cursor Pagination

### 5.1 Message Types
`TEXT`, `IMAGE`, `VIDEO`, `AUDIO`, `FILE`, `LOCATION`, `CONTACT`, `STICKER`, `CALL`.

### 5.2 Sending & Managing Messages
- `POST /api/v1/conversations/{conversationId}/messages`: Send a message.
  - **Status:** `201 Created`
  - **Request Body:**
    ```json
    {
      "type": "TEXT",
      "content": "Hello team!",
      "replyToId": null
    }
    ```
  - **Response:** `201 Created` + `MessageResponse`. Also emits real-time `message:new` event on `/topic/conversations/{conversationId}`.
- `GET /api/v1/messages/{messageId}`: Fetch a single message. Returns `200 OK` with `MessageResponse`.
- `PATCH /api/v1/messages/{messageId}`: Edit text content. Returns `200 OK` with `MessageResponse`. Emits `message:edited`.
- `DELETE /api/v1/messages/{messageId}`: Soft-delete message. Returns `204 No Content`. Emits `message:deleted`.

### 5.3 Cursor Pagination (`GET /api/v1/conversations/{conversationId}/messages`)
To support infinite scroll / backward pagination in Flutter, messages are retrieved by descending message ID (newest first).

#### Query Parameters:
- `limit` *(optional, int)*: Number of items per page. Minimum `1`, maximum `100`, default `50`.
- `before` *(optional, long)*: Cursor message ID. If provided, must be `> 0`. Fetches messages with `id < before`. If omitted, fetches the newest `limit` messages.

#### Response Structure (`MessagePageResponse`):
```json
{
  "success": true,
  "code": "OK",
  "message": "Request successful",
  "timestamp": "2026-10-07T09:00:00Z",
  "data": {
    "messages": [
      {
        "id": 150,
        "conversationId": 10,
        "senderId": 1,
        "senderName": "Alice",
        "senderUsername": "alice",
        "type": "TEXT",
        "content": "Latest message in conversation",
        "replyToId": null,
        "forwardedFromId": null,
        "edited": false,
        "deleted": false,
        "editedAt": null,
        "deletedAt": null,
        "createdAt": "2026-10-07T09:00:00",
        "updatedAt": "2026-10-07T09:00:00"
      },
      {
        "id": 149,
        "conversationId": 10,
        "senderId": 2,
        "senderName": "Bob",
        "senderUsername": "bob",
        "type": "TEXT",
        "content": "Earlier message",
        "replyToId": null,
        "forwardedFromId": null,
        "edited": false,
        "deleted": false,
        "editedAt": null,
        "deletedAt": null,
        "createdAt": "2026-10-07T08:59:00",
        "updatedAt": "2026-10-07T08:59:00"
      }
    ],
    "nextCursor": 149,
    "hasMore": true
  }
}
```
#### Flutter Pagination Workflow:
1. **Initial Load:** Fetch `GET /api/v1/conversations/10/messages?limit=50`.
2. **Next Page (Scroll Up):** If `hasMore == true`, request `GET /api/v1/conversations/10/messages?limit=50&before=149`.
3. Repeat until `hasMore == false` or `nextCursor == null`.

### 5.4 Delivery & Read Receipts
- `POST /api/v1/messages/{messageId}/delivered`: Mark message delivered to device. Returns `200 OK` with `ReceiptResponse`. Emits `message:delivered`.
- `POST /api/v1/messages/{messageId}/read`: Mark message read. Returns `200 OK` with `ReceiptResponse`. Emits `message:read`.

### 5.5 Reactions
- `GET /api/v1/messages/{messageId}/reactions`: List reactions (`List<ReactionResponse>`).
- `POST /api/v1/messages/{messageId}/reactions`: Add reaction (`{"emoji": "👍"}`). Returns `201 Created` with `ReactionResponse`. Emits `reaction:added`.
- `DELETE /api/v1/messages/{messageId}/reactions/{emoji}`: Remove reaction. Returns `204 No Content`. Emits `reaction:removed`.

### 5.6 Pinned Messages
- `GET /api/v1/conversations/{conversationId}/pins`: List pinned messages (`List<PinnedMessageResponse>`).
- `POST /api/v1/conversations/{conversationId}/pins/{messageId}`: Pin message. Returns `201 Created`.
- `DELETE /api/v1/conversations/{conversationId}/pins/{messageId}`: Unpin message. Returns `204 No Content`.

---

## 6. Attachment Upload Workflow & Serving Behavior

### 6.1 Two-Step Attachment Lifecycle
Attachments cannot be uploaded independently; they must attach to an existing message created by the authenticated user.
1. **Create Message:**
   Call `POST /api/v1/conversations/{conversationId}/messages` with `type: "IMAGE"` (or `VIDEO`, `AUDIO`, `FILE`).
   Retrieve the generated `messageId` from the response.
2. **Upload Binary Payload:**
   Send `POST /api/v1/uploads`:
   - `Content-Type: multipart/form-data`
   - Parameter `messageId` *(query/form parameter)*: ID of the message from Step 1.
   - Part `file` *(binary)*: File content. Maximum allowed size is **25 MB**. Supported types: images (`image/*`), audio (`audio/*`), video (`video/*`), documents (`application/pdf`, `text/plain`, doc, etc.).

### 6.2 Upload Response (`UploadResponse`)
```json
{
  "success": true,
  "code": "CREATED",
  "message": "Resource created",
  "timestamp": "2026-10-07T09:00:00Z",
  "data": {
    "id": 1,
    "messageId": 100,
    "type": "IMAGE",
    "fileName": "photo.jpg",
    "fileUrl": "/uploads/d17e7bca-4b07-4e02-9831-294022b7bf7c.jpg",
    "mimeType": "image/jpeg",
    "fileSize": 1048576
  }
}
```

### 6.3 Critical Backend Limitation: Upload Serving
- `LocalFileStorageService` stores files in the local filesystem directory configured by `app.upload.directory` (default `uploads/`).
- The returned `fileUrl` is a relative URI path (e.g., `/uploads/<uuid>.<ext>`).
- **Limitation:** The Spring Boot backend currently **does not register a static resource handler or download controller** for `/uploads/**`. Attempting a `GET /uploads/<uuid>.<ext>` directly against the Spring Boot port returns HTTP 404 (or 401 if unauthenticated).
- **Deployment Requirement for Flutter:** File retrieval in production requires:
  1. An external reverse proxy (such as Nginx) routing requests matching `/uploads/*` directly to the shared storage volume, or
  2. A dedicated cloud storage provider / CDN URL mapping.

---

## 7. Push Notifications & Device Management

### 7.1 Devices (`/api/v1/devices`)
To receive FCM push notifications when the Flutter app is in the background or killed, the client must register its FCM registration token.
- `POST /api/v1/devices`:
  - **Status:** `201 Created`
  - **Request Body:**
    ```json
    {
      "deviceName": "Pixel 8 Pro",
      "platform": "ANDROID",
      "fcmToken": "synthetic-fcm-token-sample-value"
    }
    ```
    - `platform`: `ANDROID`, `IOS`, or `WEB`.
  - **Behavior:** Idempotent upsert. If the `fcmToken` already exists in the database, it is updated and reassociated with the current user.
- `DELETE /api/v1/devices/{id}`: Deregister device. Returns `204 No Content`.

### 7.2 In-App Notifications (`/api/v1/notifications`)
- `GET /api/v1/notifications?page=0&size=20`: List notifications. Returns `200 OK` with `NotificationPageResponse`.
- `POST /api/v1/notifications/{id}/read`: Mark notification as read. Returns `200 OK` with `NotificationResponse`.
- `POST /api/v1/notifications/read-all`: Mark all notifications as read. Returns `204 No Content`.
- `DELETE /api/v1/notifications/{id}`: Delete notification. Returns `204 No Content`.

### 7.3 Real-Time Notification Limitation
- **Notification events are NOT published via WebSocket/STOMP.**
- There are no destinations like `/topic/notifications` or `/user/queue/notifications`.
- Live in-conversation updates (messages, typing, receipts, reactions) are delivered over conversation WebSocket topics. Out-of-conversation alerts are delivered via Firebase Cloud Messaging (FCM) and synchronized via the REST notification endpoints.

---

## 8. WebSocket & STOMP Protocol Contract

### 8.1 Connection & Handshake
- **URL:** `ws://<host>:<port>/ws` (or `wss://...` over TLS)
- **STOMP Broker:** Spring SimpleBroker (`/topic`, `/queue`)
- **Application Destination Prefix:** `/app`
- **Authentication:** In the STOMP `CONNECT` frame, the client **MUST** supply the Bearer JWT in the `Authorization` header:
  ```stomp
  CONNECT
  accept-version:1.2,1.1,1.0
  heart-beat:10000,10000
  Authorization:Bearer <accessToken>

  ^@
  ```
- If the token is absent, invalid, or expired, the backend sends a STOMP `ERROR` frame (`"WebSocket request could not be processed"`) and closes the connection.

### 8.2 Client Subscriptions (Inbound to Client)
Clients receive events by subscribing to specific conversations:
- **Destination:** `/topic/conversations/{conversationId}`
- **Subscription Authorization:** When the client sends `SUBSCRIBE` for `/topic/conversations/{conversationId}`, the server validates that the authenticated user is an active member of that conversation. If not, the subscription is rejected.
- **Allowed Subscriptions:** Only `/topic/conversations/{conversationId}` is permitted. Any attempt to subscribe to arbitrary topics or queues throws `IllegalArgumentException` and terminates with a STOMP error.

### 8.3 Client Send Operations (Outbound from Client)
Client STOMP `SEND` destinations are **strictly restricted** to ephemeral typing indicators:
- `/app/typing/start`:
  ```json
  {
    "conversationId": 10
  }
  ```
- `/app/typing/stop`:
  ```json
  {
    "conversationId": 10
  }
  ```
- **Critical Security Limitation:** The client **cannot send messages, reactions, receipts, or pin updates over STOMP `SEND`**. All business actions must be performed using the authenticated REST APIs (`POST /api/v1/conversations/{id}/messages`, etc.). Attempting to send to any destination other than `/app/typing/start` and `/app/typing/stop` is rejected by `WebSocketAuthInterceptor`.

### 8.4 Server-to-Client Event Envelope (`WebSocketEvent<T>`)
All events broadcast over `/topic/conversations/{conversationId}` use a consistent envelope:
```json
{
  "type": "message:new",
  "conversationId": 10,
  "timestamp": "2026-10-07T09:00:00Z",
  "data": { ... }
}
```

### 8.5 Event Types & Payloads Reference

| Event String | Java Event Type | Description | `data` Payload Type |
|:---|:---|:---|:---|
| `message:new` | `MESSAGE_NEW` | New message sent | `MessageResponse` |
| `message:edited` | `MESSAGE_EDITED` | Message edited | `MessageResponse` |
| `message:deleted` | `MESSAGE_DELETED` | Message soft-deleted | `MessageResponse` (`deleted: true`) |
| `message:delivered` | `MESSAGE_DELIVERED` | Message delivered receipt | `ReceiptResponse` |
| `message:read` | `MESSAGE_READ` | Message read receipt | `ReceiptResponse` |
| `reaction:added` | `REACTION_ADDED` | Reaction added | `ReactionResponse` |
| `reaction:removed` | `REACTION_REMOVED` | Reaction removed | `ReactionResponse` |
| `typing:start` | `TYPING_START` | User started typing | `TypingEventPayload` (`userId`, `username`) |
| `typing:stop` | `TYPING_STOP` | User stopped typing | `TypingEventPayload` (`userId`, `username`) |
| `user:online` | `USER_ONLINE` | Active member came online | `PresenceEventPayload` (`userId`, `status`, `lastSeenAt`) |
| `user:offline` | `USER_OFFLINE` | Active member went offline | `PresenceEventPayload` (`userId`, `status`, `lastSeenAt`) |

---

## 9. Abuse Reporting (`/api/v1/reports`)

### `POST /api/v1/reports`
Submits an abuse report against a message, user, or conversation.
- **Status:** `201 Created`
- **Request Body:**
  ```json
  {
    "reportedUserId": 2,
    "conversationId": 10,
    "messageId": 100,
    "reason": "SPAM",
    "description": "Sending automated promotional messages"
  }
  ```
  - `reason`: `SPAM`, `HARASSMENT`, `INAPPROPRIATE_CONTENT`, `OTHER`.
- **Response:** `201 Created` with `ReportResponse` (`status: "PENDING"`).

---

## 10. Complete REST API Matrix (45 Operations)

All 45 operations below have been verified and reconciled against Spring MVC, Springdoc OpenAPI, and Spring Security:

| # | HTTP Method | Path | Auth | Success Status | Return Data Structure |
|---|:---|:---|:---|:---|:---|
| 1 | `POST` | `/api/v1/auth/register` | Public | `201 Created` | `AuthResponse` |
| 2 | `POST` | `/api/v1/auth/login` | Public | `200 OK` | `AuthResponse` |
| 3 | `POST` | `/api/v1/auth/refresh` | Public | `200 OK` | `AuthResponse` |
| 4 | `POST` | `/api/v1/auth/logout` | Bearer | `200 OK` | `null` (`ApiResponse<Void>`) |
| 5 | `GET` | `/api/v1/auth/me` | Bearer | `200 OK` | `UserResponse` |
| 6 | `GET` | `/api/v1/users` | Bearer | `200 OK` | `Page<UserPublicResponse>` |
| 7 | `GET` | `/api/v1/users/search` | Bearer | `200 OK` | `Page<UserPublicResponse>` |
| 8 | `GET` | `/api/v1/users/me` | Bearer | `200 OK` | `UserResponse` |
| 9 | `GET` | `/api/v1/users/{id}` | Bearer | `200 OK` | `UserPublicResponse` |
| 10 | `PATCH` | `/api/v1/users/me` | Bearer | `200 OK` | `UserResponse` |
| 11 | `PATCH` | `/api/v1/users/me/avatar` | Bearer | `200 OK` | `UserResponse` |
| 12 | `GET` | `/api/v1/settings` | Bearer | `200 OK` | `UserSettingResponse` |
| 13 | `PATCH` | `/api/v1/settings` | Bearer | `200 OK` | `UserSettingResponse` |
| 14 | `POST` | `/api/v1/devices` | Bearer | `201 Created` | `DeviceResponse` |
| 15 | `DELETE` | `/api/v1/devices/{id}` | Bearer | `204 No Content` | *(empty body)* |
| 16 | `POST` | `/api/v1/conversations` | Bearer | `201 Created` | `ConversationResponse` |
| 17 | `GET` | `/api/v1/conversations` | Bearer | `200 OK` | `List<ConversationResponse>` |
| 18 | `GET` | `/api/v1/conversations/{id}` | Bearer | `200 OK` | `ConversationResponse` |
| 19 | `PATCH` | `/api/v1/conversations/{id}` | Bearer | `200 OK` | `ConversationResponse` |
| 20 | `GET` | `/api/v1/conversations/{conversationId}/members` | Bearer | `200 OK` | `List<ConversationMemberResponse>` |
| 21 | `POST` | `/api/v1/conversations/{conversationId}/members` | Bearer | `201 Created` | `ConversationMemberResponse` |
| 22 | `DELETE` | `/api/v1/conversations/{conversationId}/members/{userId}` | Bearer | `204 No Content` | *(empty body)* |
| 23 | `PATCH` | `/api/v1/conversations/{conversationId}/members/{userId}/role` | Bearer | `200 OK` | `ConversationMemberResponse` |
| 24 | `GET` | `/api/v1/conversations/{conversationId}/messages` | Bearer | `200 OK` | `MessagePageResponse` |
| 25 | `POST` | `/api/v1/conversations/{conversationId}/messages` | Bearer | `201 Created` | `MessageResponse` |
| 26 | `GET` | `/api/v1/messages/{messageId}` | Bearer | `200 OK` | `MessageResponse` |
| 27 | `PATCH` | `/api/v1/messages/{messageId}` | Bearer | `200 OK` | `MessageResponse` |
| 28 | `DELETE` | `/api/v1/messages/{messageId}` | Bearer | `204 No Content` | *(empty body)* |
| 29 | `GET` | `/api/v1/messages/{messageId}/reactions` | Bearer | `200 OK` | `List<ReactionResponse>` |
| 30 | `POST` | `/api/v1/messages/{messageId}/reactions` | Bearer | `201 Created` | `ReactionResponse` |
| 31 | `DELETE` | `/api/v1/messages/{messageId}/reactions/{emoji}` | Bearer | `204 No Content` | *(empty body)* |
| 32 | `POST` | `/api/v1/messages/{messageId}/delivered` | Bearer | `200 OK` | `ReceiptResponse` |
| 33 | `POST` | `/api/v1/messages/{messageId}/read` | Bearer | `200 OK` | `ReceiptResponse` |
| 34 | `POST` | `/api/v1/conversations/{conversationId}/pins/{messageId}` | Bearer | `201 Created` | `PinnedMessageResponse` |
| 35 | `DELETE` | `/api/v1/conversations/{conversationId}/pins/{messageId}` | Bearer | `204 No Content` | *(empty body)* |
| 36 | `GET` | `/api/v1/conversations/{conversationId}/pins` | Bearer | `200 OK` | `List<PinnedMessageResponse>` |
| 37 | `POST` | `/api/v1/uploads` | Bearer | `201 Created` | `UploadResponse` |
| 38 | `POST` | `/api/v1/users/{id}/block` | Bearer | `201 Created` | `BlockedUserResponse` |
| 39 | `DELETE` | `/api/v1/users/{id}/block` | Bearer | `204 No Content` | *(empty body)* |
| 40 | `GET` | `/api/v1/users/me/blocked` | Bearer | `200 OK` | `List<BlockedUserResponse>` |
| 41 | `GET` | `/api/v1/notifications` | Bearer | `200 OK` | `NotificationPageResponse` |
| 42 | `POST` | `/api/v1/notifications/{id}/read` | Bearer | `200 OK` | `NotificationResponse` |
| 43 | `POST` | `/api/v1/notifications/read-all` | Bearer | `204 No Content` | *(empty body)* |
| 44 | `DELETE` | `/api/v1/notifications/{id}` | Bearer | `204 No Content` | *(empty body)* |
| 45 | `POST` | `/api/v1/reports` | Bearer | `201 Created` | `ReportResponse` |

---

## 11. Security & Synthetic Testing Compliance

In accordance with strict security policies:
- No production database credentials, passwords, or actual JWT secrets are disclosed in this document or in tests.
- All tokens in examples are synthetic dummy placeholders.
- The backend enforces input validation and authentication checks before any business logic executes.
