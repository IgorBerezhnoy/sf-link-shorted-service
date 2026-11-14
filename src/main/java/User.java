import java.util.UUID;

public class User {
    private final UUID id;

    public User() {
        id = UUID.randomUUID();
    }

    public UUID getId() {
        return id;
    }
}



