# MYCHAT image download backend patch

Adds authenticated `GET /uploads/{filename}` to retrieve stored avatars and conversation images.

- Supply `Authorization: Bearer <access token>` when fetching the image (including Flutter `Image.network(headers: ...)`).
- Avatars can be fetched by signed-in users. Chat attachments require active conversation membership.
- Upload files are read from the **same** `app.upload.directory` as the uploader.
- Production `UPLOAD_DIR` must point to a durable mounted directory (for example `/var/data/mychat/uploads` with a Render persistent disk mounted at `/var/data`). Render services without persistent disk must use external object storage instead; otherwise uploads disappear on restart/redeploy.
- Old missing images (HTTP 404) cannot be recovered by this patch; re-upload them if original files are lost.
- Flutter must send Authorization header for both avatars and chat images. This backend ZIP does not modify Flutter.
- `IMAGE` messages with empty `attachments` have no image to render; investigate failed uploads and retry.
- This ZIP is based on the previously supplied backend snapshot, not a newly verified production commit.
- Build/test before deployment: `chmod +x mvnw && ./mvnw test` (requires Java 21+ and configured test environment).
