import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class UserRepository {

    private final Map<UUID, User> users = new HashMap<>();

    public boolean add(User user) {
        if (user == null || users.containsKey(user.getId())) {
            return false;
        }

        users.put(user.getId(), user);
        return true;
    }

    public User findById(UUID id) {
        return users.get(id);
    }

    public Collection<User> findAll() {
        return users.values();
    }
}