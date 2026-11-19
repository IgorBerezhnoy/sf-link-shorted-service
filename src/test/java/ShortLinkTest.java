
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ShortLinkTest {

    private ShortLink createLink(int maxClicks) {
        return new ShortLink(
                "https://example.com",
                "abc123",
                UUID.randomUUID(),
                maxClicks,
                Duration.ofHours(1)
        );
    }

    @Test
    void newLinkShouldHaveZeroClicks() {
        ShortLink link = createLink(5);

        assertEquals(0, link.getClickCount());
    }

    @Test
    void newLinkShouldBeAvailable() {
        ShortLink link = createLink(5);

        assertTrue(link.canOpen());
    }

    @Test
    void registerClickShouldIncreaseCounter() {
        ShortLink link = createLink(5);

        boolean registered = link.registerClick();

        assertTrue(registered);
        assertEquals(1, link.getClickCount());
    }

    @Test
    void linkShouldBeBlockedAfterClickLimit() {
        ShortLink link = createLink(2);

        assertTrue(link.registerClick());
        assertTrue(link.registerClick());

        assertFalse(link.registerClick());
        assertFalse(link.canOpen());
        assertEquals(2, link.getClickCount());
    }

    @Test
    void zeroClickLimitShouldBeRejected() {
        assertThrows(
                IllegalArgumentException.class,
                () -> createLink(0)
        );
    }

    @Test
    void negativeLifetimeShouldBeRejected() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new ShortLink(
                        "https://example.com",
                        "abc123",
                        UUID.randomUUID(),
                        5,
                        Duration.ofSeconds(-1)
                )
        );
    }

    @Test
    void changingLimitToZeroShouldBeRejected() {
        ShortLink link = createLink(5);

        assertThrows(
                IllegalArgumentException.class,
                () -> link.setMaxClicks(0)
        );
    }
}
