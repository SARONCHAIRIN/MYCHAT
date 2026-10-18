# Profile avatar file upload

New endpoint: `PATCH /api/v1/users/me/avatar` with `Content-Type: multipart/form-data`, form part `file` (JPEG, PNG, WebP, maximum 5 MB). Bearer authentication required. Returns existing `ApiResponse<UserResponse>` with updated `avatarUrl` pointing to `/uploads/<uuid>.<ext>`.

The old JSON avatarUrl PATCH remains supported for existing clients, but new Flutter clients should use multipart. Example:

```bash
curl -X PATCH 'http://localhost:8080/api/v1/users/me/avatar' -H 'Authorization: Bearer YOUR_TOKEN' -F 'file=@/path/to/photo.jpg;type=image/jpeg'
```

**Important:** This patch stores files using existing `FileStorageService` (`LocalFileStorageService` by default). It does not add a file-serving endpoint or persistent object storage. Check that `/uploads/<uuid>` is actually served and accessible, and configure durable storage on Render before production. The existing service checks declared MIME type and size; for production, also validate file signatures, decode/re-encode images, remove EXIF, and add cleanup for replaced avatars.

Run `./mvnw clean test` locally. The ZIP has not been compiled in this environment.
