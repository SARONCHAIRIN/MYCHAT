# MYCHAT private Cloudflare R2 storage migration

## Security model

- The R2 bucket MUST remain private; do not create a public r2.dev/custom-domain route.
- Keep `GET /uploads/{filename}` behind Spring Security JWT.
- Attachment downloads require active membership in the message conversation; avatar URLs keep the project's existing policy (any authenticated user).
- The database continues to store `/uploads/<uuid>.<ext>`; Flutter does not need to learn R2 URLs.
- The AWS SDK is used against R2's S3-compatible endpoint, with static credentials supplied only by Render environment variables.

## Cloudflare configuration

1. Create a private R2 bucket (e.g. `mychat-private`).
2. Create an R2 API token with **Object Read & Write** limited to that bucket.
3. In Render service environment, configure:

```
APP_STORAGE_PROVIDER=r2
R2_ENDPOINT=https://<ACCOUNT_ID>.r2.cloudflarestorage.com
R2_BUCKET=mychat-private
R2_ACCESS_KEY_ID=<secret>
R2_SECRET_ACCESS_KEY=<secret>
```

The endpoint must be the **account endpoint**, without the bucket suffix. Do not commit real credentials to Git.

## Deployment and tests

1. Merge the patch into the backend branch actually deployed by Render. The inspected `dev_rin` branch lacks the production `StoredImageController`, `findByFileUrl` and `existsByAvatarUrl`; this patch supplies them.
2. Run `./mvnw clean test` locally. Verify that the AWS SDK dependency resolves.
3. Deploy with `APP_STORAGE_PROVIDER=r2` and all R2 variables configured.
4. Authorize Swagger using a fresh JWT; POST `/api/v1/uploads?messageId=<your-message>` with a small JPEG.
5. Read `data.fileUrl` from the upload response. GET `/uploads/{uuid}.jpg` with the JWT: expect `200 image/jpeg`.
6. Test with an authenticated user who is NOT a conversation member: expect `403`.
7. Restart/redeploy Render and GET the same file: expect `200`.
8. Verify the bucket is private and its objects cannot be fetched anonymously.

## Existing uploads

The patch does **not** recover files already lost from `/tmp/mychat-uploads`. The database records still exist but need re-upload or migration from a surviving copy. Uploading a new file with the same original filename does not restore the old UUID URL.

## Compatibility notes

- `APP_STORAGE_PROVIDER` defaults to `local`, so local tests and development remain functional.
- The R2 implementation uses `getObjectAsBytes`; the existing upload limit is 25 MiB. Consider streaming for larger attachments in the future.
- `StoredImageController` handles attachments of all supported types; non-image data is served as a download to avoid content sniffing.
- Avatar updates in the inspected branch set a URL string, rather than uploading bytes. To persist an avatar in R2, upload it using an authorized flow and store the returned `/uploads/...` URL; validate ownership and file type in a dedicated avatar upload endpoint before production use.
- If the deployed code differs from GitHub `dev_rin`, compare before merging to avoid overwriting more recent controller logic.
