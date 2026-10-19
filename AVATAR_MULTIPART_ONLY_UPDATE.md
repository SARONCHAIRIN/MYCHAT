# Avatar Multipart-only Swagger fix (2026-10-19)

Based on previously provided merged backend ZIP; GitHub dev_rin inspected for comparison, but the ZIP is NOT an export of the latest remote commit.

UserController.java: removed old JSON PATCH /api/v1/users/me/avatar mapping; kept multipart PATCH with @RequestPart("file") MultipartFile.

Expected Swagger: multipart/form-data with a file chooser. Existing UserService.updateAvatar and DTO remain for compatibility with tests and other references.

Run ./mvnw clean test and ./mvnw spring-boot:run locally, then deploy and refresh Swagger. MySQL credentials must be configured.

Note: this ZIP is not build-tested. File storage serving and persistence still need separate validation.
