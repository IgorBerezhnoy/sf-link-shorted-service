import java.util.UUID;

public class LinkService {
    private final LinkRepository repository;
    private final ShortCodeGenerator generator;

    public LinkService(LinkRepository repository, ShortCodeGenerator generator) {
        this.repository = repository;
        this.generator = generator;
    }

    public ShortLink createShortLink(String originalUrl, UUID ownerId) {
        if (originalUrl == null || originalUrl.isBlank() || ownerId == null) {
            throw new IllegalArgumentException("Неверный URL или ownerId");
        }

        for (int i = 0; i < 100; i++) {
            String shortCode = generator.generate();

            ShortLink shortLink = new ShortLink(
                    originalUrl,
                    shortCode,
                    ownerId
            );

            if (repository.add(shortLink)) {
                return shortLink;
            }
        }


        throw new IllegalStateException(
                "Не удалось создать уникальную короткую ссылку"
        );

    }
}
