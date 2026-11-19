
import java.security.SecureRandom;

public class ShortCodeGenerator {
    private final int codeLength;
    private static final String VALID_CHARACTERS =
            "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    public ShortCodeGenerator(int codeLength) {
        if (codeLength < 4 || codeLength > 32) {
            throw new IllegalArgumentException(
                    "Длина кода должна быть от 4 до 32 символов"
            );
        }
        this.codeLength = codeLength;
    }

    private final SecureRandom random = new SecureRandom();

    public String generate() {
        StringBuilder builder = new StringBuilder(codeLength);

        for (int i = 0; i < codeLength; i++) {
            int index = random.nextInt(VALID_CHARACTERS.length());
            char symbol = VALID_CHARACTERS.charAt(index);

            builder.append(symbol);
        }

        return builder.toString();
    }
}
