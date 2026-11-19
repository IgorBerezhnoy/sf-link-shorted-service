
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class LinkServiceTest {

    private final LinkRepository repository = new LinkRepository();

    private final LinkService service = new LinkService(
            repository,
            new ShortCodeGenerator(6),
            100
    );

    private ShortLink createLink(UUID ownerId) {
        return service.createShortLink(
                "https://example.com",
                ownerId,
                5,
                Duration.ofHours(1)
        );
    }

    @Test
    void differentUsersShouldGetDifferentCodes() {
        ShortLink first = createLink(UUID.randomUUID());
        ShortLink second = createLink(UUID.randomUUID());

        assertNotEquals(first.getShortCode(), second.getShortCode());
    }

    @Test
    void ownerShouldBeAbleToUpdateClickLimit() {
        UUID ownerId = UUID.randomUUID();
        ShortLink link = createLink(ownerId);

        boolean updated = service.updateClickLimit(
                link.getShortCode(), ownerId, 20
        );

        assertTrue(updated);
        assertEquals(20, link.getMaxClicks());
    }

    @Test
    void anotherUserCannotUpdateClickLimit() {
        ShortLink link = createLink(UUID.randomUUID());
        UUID anotherUserId = UUID.randomUUID();

        boolean updated = service.updateClickLimit(
                link.getShortCode(), anotherUserId, 20
        );

        assertFalse(updated);
        assertEquals(5, link.getMaxClicks());
    }

    @Test
    void ownerShouldBeAbleToDeleteLink() {
        UUID ownerId = UUID.randomUUID();
        ShortLink link = createLink(ownerId);

        boolean deleted = service.deleteLink(
                link.getShortCode(), ownerId
        );

        assertTrue(deleted);
        assertNull(service.findByCode(link.getShortCode()));
    }

    @Test
    void anotherUserCannotDeleteLink() {
        ShortLink link = createLink(UUID.randomUUID());
        UUID anotherUserId = UUID.randomUUID();

        boolean deleted = service.deleteLink(
                link.getShortCode(), anotherUserId
        );

        assertFalse(deleted);
        assertNotNull(service.findByCode(link.getShortCode()));
    }
}
