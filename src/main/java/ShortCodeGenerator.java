
import java.security.SecureRandom;

public class ShortCodeGenerator {
    private static final String VALID_CHARACTERS =
            "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    private static final int CODE_LENGTH = 6;

    private final SecureRandom random = new SecureRandom();

    public String generate() {
        StringBuilder builder = new StringBuilder(CODE_LENGTH);

        for (int i = 0; i < CODE_LENGTH; i++) {
            int index = random.nextInt(VALID_CHARACTERS.length());
            char symbol = VALID_CHARACTERS.charAt(index);

            builder.append(symbol);
        }

        return builder.toString();
    }
}
