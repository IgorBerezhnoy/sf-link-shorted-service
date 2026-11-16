import java.time.Duration;
import java.util.UUID;

public class LinkService {

    private final LinkRepository repository;
    private final ShortCodeGenerator generator;

    public LinkService(LinkRepository repository, ShortCodeGenerator generator) {
        this.repository = repository;
        this.generator = generator;
    }

    public ShortLink createShortLink(String originalUrl, UUID ownerId, int maxClicks, Duration lifetime) {

        if (originalUrl == null || originalUrl.isBlank() || ownerId == null) {
            throw new IllegalArgumentException("Неверный URL или ownerId");
        }

        if (maxClicks < 1) {
            throw new IllegalArgumentException("Лимит переходов должен быть больше нуля");
        }

        if (lifetime == null || lifetime.isZero() || lifetime.isNegative()) {
            throw new IllegalArgumentException(
                    "Lifetime должен быть положительным"
            );
        }

        for (int i = 0; i < 100; i++) {
            String shortCode = generator.generate();

            ShortLink shortLink = new ShortLink(
                    originalUrl,
                    shortCode,
                    ownerId,
                    maxClicks,
                    lifetime);

            if (repository.add(shortLink)) {
                return shortLink;
            }
        }

        throw new IllegalStateException(
                "Не удалось создать уникальную короткую ссылку");

    }

    public boolean registerClick(String code) {
        ShortLink shortLink = findByCode(code);

        if (shortLink == null) {
            return false;
        }

        return shortLink.registerClick();

    }

    public int removeExpiredLinks() {
        return repository.removeExpiredLinks();
    }

    public ShortLink findByCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }

        return repository.findByCode(code);
    }
}
