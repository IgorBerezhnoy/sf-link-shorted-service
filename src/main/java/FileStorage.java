
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.DateTimeException;
import java.time.Instant;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

public class FileStorage {

    private static final Set<String> LINK_FIELDS = Set.of(
            "originalUrl",
            "ownerId",
            "maxClicks",
            "clickCount",
            "expiresAt"
    );

    private final Path filePath;

    public FileStorage(Path filePath) {
        this.filePath = filePath.toAbsolutePath().normalize();
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
                    prefix + "originalUrl",
                    link.getOriginalUrl()
            );

            properties.setProperty(
                    prefix + "ownerId",
                    link.getOwnerId().toString()
            );

            properties.setProperty(
                    prefix + "maxClicks",
                    String.valueOf(link.getMaxClicks())
            );

            properties.setProperty(
                    prefix + "clickCount",
                    String.valueOf(link.getClickCount())
            );

            properties.setProperty(
                    prefix + "expiresAt",
                    link.getExpiresAt().toString()
            );
        }

        Path parent = filePath.getParent();
        Files.createDirectories(parent);

        Path tempFile = Files.createTempFile(
                parent,
                "link-shortener-",
                ".tmp"
        );

        try {
            try (OutputStream out = Files.newOutputStream(tempFile)) {
                properties.store(out, "Link Shortener Service data");
            }

            Files.move(
                    tempFile,
                    filePath,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING
            );

        } finally {
            Files.deleteIfExists(tempFile);
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

        try {
            try (InputStream in = Files.newInputStream(filePath)) {
                properties.load(in);
            }

            // Restore users
            for (String key : properties.stringPropertyNames()) {
                if (!key.startsWith("user.")) {
                    continue;
                }

                String id = key.substring("user.".length());
                User user = new User(UUID.fromString(id));

                if (!userRepository.add(user)) {
                    throw new IOException(
                            "Дублирующийся пользователь: " + id
                    );
                }
            }

            // Find all saved link codes
            Set<String> linkCodes = new HashSet<>();

            for (String key : properties.stringPropertyNames()) {
                if (!key.startsWith("link.")) {
                    continue;
                }

                String remainder = key.substring("link.".length());
                int separator = remainder.lastIndexOf('.');

                if (separator <= 0
                        || separator == remainder.length() - 1) {
                    throw new IOException(
                            "Некорректный ключ ссылки: " + key
                    );
                }

                String code = remainder.substring(0, separator);
                String field = remainder.substring(separator + 1);

                if (!LINK_FIELDS.contains(field)) {
                    throw new IOException(
                            "Неизвестное поле ссылки: " + key
                    );
                }

                linkCodes.add(code);
            }

            // Restore links
            for (String code : linkCodes) {
                String prefix = "link." + code + ".";

                String originalUrl = required(
                        properties, prefix + "originalUrl"
                );

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
                    throw new IOException(
                            "Не найден владелец ссылки: " + code
                    );
                }

                ShortLink link = new ShortLink(
                        originalUrl,
                        code,
                        ownerId,
                        maxClicks,
                        clickCount,
                        expiresAt
                );

                if (link.isExpired()) {
                    continue;
                }

                if (!linkRepository.add(link)) {
                    throw new IOException(
                            "Дублирующаяся ссылка: " + code
                    );
                }
            }

        } catch (IllegalArgumentException | DateTimeException e) {
            throw new IOException(
                    "Некорректные данные в файле: " + e.getMessage(),
                    e
            );
        }
    }

    private static String required(
            Properties properties,
            String key
    ) throws IOException {

        String value = properties.getProperty(key);

        if (value == null) {
            throw new IOException(
                    "Отсутствует обязательное поле: " + key
            );
        }

        return value;
    }
}
