package com.rindev.chat.storage;

import com.rindev.chat.exception.BadRequestException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalFileStorageServiceTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void configuredDirectoryReceivesUploadedBytesAndPreservesPublicMetadata() {
        Path uploadDirectory = temporaryDirectory.resolve("mounted/uploads");
        byte[] bytes = "uploaded content".getBytes(StandardCharsets.UTF_8);
        var upload = new MockMultipartFile("file", "document.txt", "text/plain", bytes);

        new ApplicationContextRunner().withUserConfiguration(LocalFileStorageService.class)
                .withPropertyValues("app.upload.directory=" + uploadDirectory)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    StoredFile stored = context.getBean(LocalFileStorageService.class).store(upload);
                    Path destination = uploadDirectory.resolve(stored.storedFileName());

                    assertThat(Files.readAllBytes(destination)).isEqualTo(bytes);
                    assertThat(destination.getParent()).isEqualTo(uploadDirectory);
                    assertThat(stored.storedFileName()).matches("[0-9a-f-]{36}\\.txt");
                    assertThat(stored.originalFileName()).isEqualTo("document.txt");
                    assertThat(stored.fileUrl()).isEqualTo("/uploads/" + stored.storedFileName());
                    assertThat(stored.contentType()).isEqualTo("text/plain");
                    assertThat(stored.size()).isEqualTo(bytes.length);
                });
    }

    @ParameterizedTest
    @ValueSource(strings = { "image.png/nested", "image.png/../../escaped", "image.png/../../../escaped" })
    void pathTraversalAndNestedPathsRemainRejected(String filename) throws Exception {
        Path root = temporaryDirectory.resolve("uploads");
        var storage = new LocalFileStorageService(root.toString());
        var upload = new MockMultipartFile("file", filename, "image/png", new byte[] { 1, 2, 3 });

        assertThatThrownBy(() -> storage.store(upload))
                .isInstanceOf(BadRequestException.class).hasMessage("Invalid file name");
        try (var files = Files.list(root)) {
            assertThat(files).isEmpty();
        }
        assertThat(temporaryDirectory.resolve("escaped")).doesNotExist();
    }

    @Test
    void emptyUploadIsRejectedBeforeWritingAnyFile() throws Exception {
        var storage = new LocalFileStorageService(temporaryDirectory.toString());
        var upload = new MockMultipartFile("file", "empty.txt", "text/plain", new byte[0]);

        assertThatThrownBy(() -> storage.store(upload))
                .isInstanceOf(BadRequestException.class).hasMessage("File is required");
        try (var files = Files.list(temporaryDirectory)) {
            assertThat(files).isEmpty();
        }
    }

    @Test
    void blankDirectoryIsRejectedInsteadOfWritingToWorkingDirectory() {
        assertThatThrownBy(() -> new LocalFileStorageService(" "))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("Upload directory must not be blank");
    }
}
