import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Properties;

public record AppConfig(
        Duration lifetime,
        int maxAllowedClicks,
        int codeLength
) {
    public AppConfig {
        if (lifetime == null || lifetime.isNegative() || lifetime.isZero()
                || maxAllowedClicks < 1
                || codeLength < 4 || codeLength > 32) {
            throw new IllegalArgumentException(
                    "Некорректные настройки приложения"
            );
        }
    }

    public static AppConfig load(Path path) throws IOException {
        Properties properties = new Properties();

        try (InputStream input = Files.newInputStream(path)) {
            properties.load(input);
        }

        try {
            int hours = Integer.parseInt(
                    required(properties, "link.ttl.hours")
            );
            int maxClicks = Integer.parseInt(
                    required(properties, "link.max-clicks")
            );
            int codeLength = Integer.parseInt(
                    required(properties, "link.code-length")
            );

            return new AppConfig(
                    Duration.ofHours(hours),
                    maxClicks,
                    codeLength
            );

        } catch (IllegalArgumentException e) {
            throw new IOException(
                    "Ошибка в конфигурации приложения: " + e.getMessage(),
                    e
            );
        }
    }

    private static String required(Properties properties, String key) {
        String value = properties.getProperty(key);

        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "Отсутствует настройка: " + key
            );
        }

        return value.trim();
    }
}