import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Properties;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class FileStorageTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldSaveAndRestoreUserAndLink() throws Exception {
        UserRepository users = new UserRepository();
        LinkRepository links = new LinkRepository();

        User user = new User();
        users.add(user);

        ShortLink link = new ShortLink(
                "https://example.com",
                "abc123",
                user.getId(),
                5,
                Duration.ofDays(1)
        );

        link.registerClick();
        link.registerClick();
        links.add(link);

        FileStorage storage = new FileStorage(
                tempDir.resolve("data/state.properties")
        );

        storage.save(users, links);

        UserRepository restoredUsers = new UserRepository();
        LinkRepository restoredLinks = new LinkRepository();

        storage.load(restoredUsers, restoredLinks);

        assertNotNull(restoredUsers.findById(user.getId()));

        ShortLink restored = restoredLinks.findByCode("abc123");

        assertNotNull(restored);
        assertEquals(link.getOriginalUrl(), restored.getOriginalUrl());
        assertEquals(link.getOwnerId(), restored.getOwnerId());
        assertEquals(5, restored.getMaxClicks());
        assertEquals(2, restored.getClickCount());
        assertEquals(link.getExpiresAt(), restored.getExpiresAt());
    }

    @Test
    void expiredLinkShouldNotBeRestored() throws Exception {
        UserRepository users = new UserRepository();
        LinkRepository links = new LinkRepository();

        User user = new User();
        users.add(user);

        ShortLink expiredLink = new ShortLink(
                "https://example.com",
                "expired123",
                user.getId(),
                5,
                0,
                Instant.now().minusSeconds(60)
        );

        links.add(expiredLink);

        FileStorage storage = new FileStorage(
                tempDir.resolve("state.properties")
        );

        storage.save(users, links);

        LinkRepository restoredLinks = new LinkRepository();
        UserRepository restoredUsers = new UserRepository();

        storage.load(restoredUsers, restoredLinks);

        assertNull(restoredLinks.findByCode("expired123"));
        assertNotNull(restoredUsers.findById(user.getId()));
    }

    @Test
    void missingDataFileShouldNotCauseError() {
        FileStorage storage = new FileStorage(
                tempDir.resolve("missing.properties")
        );

        assertDoesNotThrow(() -> storage.load(
                new UserRepository(),
                new LinkRepository()
        ));
    }


    @Test
    void missingOriginalUrlShouldCauseLoadError() throws Exception {
        Path path = tempDir.resolve("broken.properties");

        Properties properties = createValidProperties();
        properties.remove("link.abc123.originalUrl");

        writeProperties(path, properties);

        FileStorage storage = new FileStorage(path);

        assertThrows(
                IOException.class,
                () -> storage.load(
                        new UserRepository(),
                        new LinkRepository()
                )
        );
    }

    @Test
    void invalidExpiresAtShouldCauseLoadError() throws Exception {
        Path path = tempDir.resolve("broken-date.properties");

        Properties properties = createValidProperties();
        properties.setProperty(
                "link.abc123.expiresAt",
                "invalid-date"
        );

        writeProperties(path, properties);

        FileStorage storage = new FileStorage(path);

        assertThrows(
                IOException.class,
                () -> storage.load(
                        new UserRepository(),
                        new LinkRepository()
                )
        );
    }

    private Properties createValidProperties() {
        UUID ownerId = UUID.randomUUID();
        Properties properties = new Properties();

        properties.setProperty("user." + ownerId, "true");

        String prefix = "link.abc123.";

        properties.setProperty(prefix + "originalUrl",
                "https://example.com");
        properties.setProperty(prefix + "ownerId",
                ownerId.toString());
        properties.setProperty(prefix + "maxClicks", "5");
        properties.setProperty(prefix + "clickCount", "2");
        properties.setProperty(prefix + "expiresAt",
                Instant.now().plusSeconds(3600).toString());

        return properties;
    }

    private void writeProperties(
            Path path,
            Properties properties
    ) throws IOException {
        try (OutputStream out = Files.newOutputStream(path)) {
            properties.store(out, "test");
        }
    }

}
