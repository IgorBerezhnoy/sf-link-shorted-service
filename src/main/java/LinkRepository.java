import java.util.HashMap;
import java.util.Map;
import java.util.Collection;
import java.util.List;

public class LinkRepository {
    private final Map<String, ShortLink> shortLinksMap = new HashMap<>();

    public Collection<ShortLink> findAll() {
        return List.copyOf(shortLinksMap.values());
    }

    public boolean add(ShortLink link) {
        if (link != null && !exists(link.getShortCode())) {
            shortLinksMap.put(link.getShortCode(), link);
            return true;
        }

        return false;
    }

    public ShortLink findByCode(String code) {
        return shortLinksMap.get(code);

    }

    public boolean deleteByCode(String code) {
        return shortLinksMap.remove(code) != null;
    }

    public int removeExpiredLinks() {
        int before = shortLinksMap.size();
        shortLinksMap.values().removeIf(ShortLink::isExpired);
        return before - shortLinksMap.size();
    }

    public boolean exists(String code) {
        return shortLinksMap.containsKey(code);
    }
}
