
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class AppConfigTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldLoadValidConfiguration() throws Exception {
        Path configPath = tempDir.resolve("application.properties");

        Files.writeString(configPath, """
                link.ttl.hours=24
                link.max-clicks=1000
                link.code-length=8
                """);

        AppConfig config = AppConfig.load(configPath);

        assertEquals(Duration.ofHours(24), config.lifetime());
        assertEquals(1000, config.maxAllowedClicks());
        assertEquals(8, config.codeLength());
    }

    @Test
    void negativeTtlShouldBeRejected() throws Exception {
        Path configPath = tempDir.resolve("application.properties");

        Files.writeString(configPath, """
                link.ttl.hours=-1
                link.max-clicks=1000
                link.code-length=6
                """);

        assertThrows(
                IOException.class,
                () -> AppConfig.load(configPath)
        );
    }

    @Test
    void missingConfigurationParameterShouldBeRejected() throws Exception {
        Path configPath = tempDir.resolve("application.properties");

        Files.writeString(configPath, """
                link.ttl.hours=24
                link.max-clicks=1000
                """);

        assertThrows(
                IOException.class,
                () -> AppConfig.load(configPath)
        );
    }
}
