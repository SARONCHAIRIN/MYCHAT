# MYCHAT Backend — Combined Update (2026-10-18)

Combined from the attachments patch and avatar file-upload patch. The avatar ZIP already contains all attachment patch files unchanged, so no overlapping code needed reconciliation.

## Included
- MessageResponse: attachments list and backward-compatible 15-argument constructor.
- MessageMapper: attachment DTO mapping.
- MessageAttachmentRepository: batch lookup by message IDs.
- MessageServiceImpl: batch mapping for message history.
- UserController: multipart avatar upload endpoint.
- UserService/UserServiceImpl: avatar file upload validation and storage.

## Verify locally
./mvnw clean test
./mvnw clean package

## Known limitations
- Maven tests have NOT been executed in this environment. Previous ApplicationContext integration tests failed; inspect first root cause in target/surefire-reports.
- File download accessibility and durable storage across Render deployments are NOT established by these patches.
- Legacy avatar URL endpoint remains in the code; file upload is additionally supported.
- Confirm image bytes/content, access controls and persistent storage before production use.
