# MYCHAT attachment-history backend patch

## Included
- MessageResponse now includes `attachments: UploadResponse[]` and retains the 15-argument constructor used by existing tests.
- MessageMapper maps persisted attachments and excludes them for soft-deleted messages.
- Paginated message history fetches attachments in one batch query instead of an N+1 query per message.
- No database schema migration is needed for these changes.

## Local verification
```bash
./mvnw clean test
./mvnw clean package
```

The archive was not compiled in the assistant environment: Maven wrapper could not download Maven due to network restrictions. The previously reported Spring ApplicationContext integration-test error must be investigated separately using `target/surefire-reports` (usually test DB/env/config).

## Important next work
- `/uploads/...` is currently returned by LocalFileStorageService but no explicit file download route/resource handler was found in the uploaded project. Verify authenticated download and access control before treating images as fully fixed.
- Files are stored locally under `app.upload.directory` (default `uploads`). On ephemeral hosts, configure durable object storage or a persistent disk.
- Sending images still requires create-message first, then upload with messageId. Handle failed uploads/retry in Flutter.
- After deploying, verify `GET /api/v1/conversations/{id}/messages` returns `attachments` for image messages, then verify downloading the URL returns actual image bytes.
