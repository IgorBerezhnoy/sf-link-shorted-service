
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class LinkRepositoryTest {

    private ShortLink createLink() {
        return new ShortLink(
                "https://example.com",
                "abc123",
                UUID.randomUUID(),
                5,
                Duration.ofHours(1)
        );
    }

    @Test
    void shouldAddAndFindLink() {
        LinkRepository repository = new LinkRepository();
        ShortLink link = createLink();

        assertTrue(repository.add(link));
        assertSame(link, repository.findByCode("abc123"));
    }

    @Test
    void shouldRejectDuplicateShortCode() {
        LinkRepository repository = new LinkRepository();

        ShortLink first = createLink();
        ShortLink second = createLink();

        assertTrue(repository.add(first));
        assertFalse(repository.add(second));
        assertSame(first, repository.findByCode("abc123"));
    }

    @Test
    void shouldDeleteLinkByCode() {
        LinkRepository repository = new LinkRepository();
        repository.add(createLink());

        assertTrue(repository.deleteByCode("abc123"));
        assertFalse(repository.exists("abc123"));
        assertFalse(repository.deleteByCode("abc123"));
    }
}
