import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Properties;
import java.util.UUID;

public class FileStorage {

    private final Path filePath;

    public FileStorage(Path filePath) {
        this.filePath = filePath;
    }

    public void save(
            UserRepository userRepository,
            LinkRepository linkRepository
    ) throws IOException {

        Properties properties = new Properties();

        for (User user : userRepository.findAll()) {
            properties.setProperty(
                    "user." + user.getId(),
                    "true"
            );
        }

        for (ShortLink link : linkRepository.findAll()) {
            String prefix = "link." + link.getShortCode() + ".";

            properties.setProperty(
                    prefix + "originalUrl", link.getOriginalUrl()
            );
            properties.setProperty(
                    prefix + "ownerId", link.getOwnerId().toString()
            );
            properties.setProperty(
                    prefix + "maxClicks", String.valueOf(link.getMaxClicks())
            );
            properties.setProperty(
                    prefix + "clickCount", String.valueOf(link.getClickCount())
            );
            properties.setProperty(
                    prefix + "expiresAt", link.getExpiresAt().toString()
            );
        }

        Files.createDirectories(filePath.getParent());

        try (OutputStream out = Files.newOutputStream(filePath)) {
            properties.store(out, "Link Shortener Service data");
        }
    }

    public void load(
            UserRepository userRepository,
            LinkRepository linkRepository
    ) throws IOException {

        if (!Files.exists(filePath)) {
            return;
        }

        Properties properties = new Properties();

        try (InputStream in = Files.newInputStream(filePath)) {
            properties.load(in);
        }

        try {
            for (String key : properties.stringPropertyNames()) {
                if (key.startsWith("user.")) {
                    String id = key.substring("user.".length());
                    User user = new User(UUID.fromString(id));

                    if (!userRepository.add(user)) {
                        throw new IOException("Дублирующийся пользователь: " + id);
                    }
                }
            }

            for (String key : properties.stringPropertyNames()) {
                if (!key.startsWith("link.")
                        || !key.endsWith(".originalUrl")) {
                    continue;
                }

                String code = key.substring(
                        "link.".length(),
                        key.length() - ".originalUrl".length()
                );

                String prefix = "link." + code + ".";

                String originalUrl = required(properties, prefix + "originalUrl");
                UUID ownerId = UUID.fromString(
                        required(properties, prefix + "ownerId")
                );
                int maxClicks = Integer.parseInt(
                        required(properties, prefix + "maxClicks")
                );
                int clickCount = Integer.parseInt(
                        required(properties, prefix + "clickCount")
                );
                Instant expiresAt = Instant.parse(
                        required(properties, prefix + "expiresAt")
                );

                if (userRepository.findById(ownerId) == null) {
                    throw new IOException("Не найден владелец ссылки: " + code);
                }

                ShortLink link = new ShortLink(
                        originalUrl,
                        code,
                        ownerId,
                        maxClicks,
                        clickCount,
                        expiresAt
                );

                if (!link.isExpired() && !linkRepository.add(link)) {
                    throw new IOException("Дублирующаяся ссылка: " + code);
                }
            }

        } catch (IllegalArgumentException | NullPointerException e) {
            throw new IOException("Некорректные данные в файле", e);
        }
    }

    private static String required(
            Properties properties,
            String key
    ) throws IOException {

        String value = properties.getProperty(key);

        if (value == null) {
            throw new IOException("Отсутствует поле: " + key);
        }

        return value;
    }
}