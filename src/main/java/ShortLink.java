import java.util.UUID;

public class ShortLink {

    private final String originalUrl;
    private final String shortCode;
    private final UUID ownerId;
    private int maxClicks;
    private int clickCount;

    public ShortLink(String originalUrl, String shortCode, UUID ownerId, int maxClicks) {

        if (maxClicks < 1) {
            throw new IllegalArgumentException(
                    "maxClicks должен быть больше нуля"
            );
        }

        this.originalUrl = originalUrl;
        this.shortCode = shortCode;
        this.ownerId = ownerId;
        this.maxClicks = maxClicks;
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


    public void setMaxClicks(int maxClicks) {
        if (maxClicks < 1) {
            throw new IllegalArgumentException(
                    "maxClicks должен быть больше нуля"
            );
        }

        this.maxClicks = maxClicks;
    }

    public boolean canOpen() {
        return clickCount < maxClicks;
    }

    public boolean registerClick() {
        if (canOpen()) {
            clickCount++;
            return true;
        }

        return false;
    }

}
