import java.util.UUID;

public class ShortLink {
    private final String originalUrl;
    private final String shortCode;
    private final UUID ownerId;

    public ShortLink(String originalUrl, String shortCode, UUID ownerId) {
        this.originalUrl = originalUrl;
        this.shortCode = shortCode;
        this.ownerId = ownerId;
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


}
