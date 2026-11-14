import java.util.HashMap;
import java.util.Map;

public class LinkRepository {
    private final Map<String, ShortLink> shortLinksMap = new HashMap<>();


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

    public boolean exists(String code) {
        return shortLinksMap.containsKey(code);
    }
}
