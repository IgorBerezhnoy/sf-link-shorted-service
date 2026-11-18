import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

public class ShortLink {

    private final String originalUrl;
    private final String shortCode;
    private final UUID ownerId;
    private int maxClicks;
    private int clickCount;
    private final Instant expiresAt;

    public ShortLink(String originalUrl, String shortCode, UUID ownerId, int maxClicks, Duration lifetime) {

        if (maxClicks < 1) {
            throw new IllegalArgumentException("maxClicks должен быть больше нуля");
        }

        if (lifetime == null || lifetime.isZero() || lifetime.isNegative()) {
            throw new IllegalArgumentException(
                    "Lifetime должен быть положительным"
            );
        }

        this.originalUrl = originalUrl;
        this.shortCode = shortCode;
        this.ownerId = ownerId;
        this.maxClicks = maxClicks;
        this.expiresAt = Instant.now().plus(lifetime);
    }

    public ShortLink(
            String originalUrl,
            String shortCode,
            UUID ownerId,
            int maxClicks,
            int clickCount,
            Instant expiresAt
    ) {
        if (originalUrl == null || originalUrl.isBlank()
                || shortCode == null || shortCode.isBlank()
                || ownerId == null || expiresAt == null) {
            throw new IllegalArgumentException("Некорректные данные ссылки");
        }

        if (maxClicks < 1 || clickCount < 0) {
            throw new IllegalArgumentException("Некорректные счётчики переходов");
        }

        this.originalUrl = originalUrl;
        this.shortCode = shortCode;
        this.ownerId = ownerId;
        this.maxClicks = maxClicks;
        this.clickCount = clickCount;
        this.expiresAt = expiresAt;
    }

    public String getOriginalUrl() {
        return originalUrl;
    }

    public String getShortCode() {
        return shortCode;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public int getMaxClicks() {
        return maxClicks;
    }

    public int getClickCount() {
        return clickCount;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setMaxClicks(int maxClicks) {
        if (maxClicks < 1) {
            throw new IllegalArgumentException("maxClicks должен быть больше нуля");
        }

        this.maxClicks = maxClicks;
    }

    public boolean canOpen() {
        return (clickCount < maxClicks) && !isExpired();
    }

    public boolean isExpired() {
        return Instant.now().compareTo(expiresAt) >= 0;
    }

    public boolean registerClick() {
        if (canOpen()) {
            clickCount++;
            return true;
        }

        return false;
    }

}
