import java.net.URI;
import java.net.URISyntaxException;
import java.util.Scanner;

public class Main {
    private static final String SHORT_LINK_PREFIX = "short-link.ru/";

    public static void main(String[] args) {
        LinkRepository repository = new LinkRepository();
        ShortCodeGenerator generator = new ShortCodeGenerator();
        LinkService linkService = new LinkService(repository, generator);

        Scanner scanner = new Scanner(System.in);
        User currentUser = null;

        System.out.println("Добро пожаловать в Link Shorted Service!");
        System.out.println("Введите 'help' для списка команд.");

        boolean running = true;

        while (running) {
            System.out.print("> ");

            String command = scanner.nextLine().trim();

            switch (command) {
                case "help": {
                    printHelp();
                    break;
                }

                case "exit": {
                    System.out.println("Завершение программы...");
                    running = false;
                    break;
                }

                case "find": {
                    handleFindCommand(scanner, linkService);

                    break;

                }

                case "create": {
                    currentUser = handleCreateCommand(scanner, currentUser, linkService);
                    break;
                }

                case "": {
                    // Ignore the empty command
                    break;
                }

                default:
                    System.out.println("Неизвестная команда: " + command);
                    break;
            }
        }

        scanner.close();
    }

    private static void printHelp() {
        System.out.println("Доступные команды:");
        System.out.println("  help - показать помощь");
        System.out.println("  create - создать короткую ссылку");
        System.out.println("  find - найти созданную короткую ссылку");
        System.out.println("  exit - завершить программу");
    }

    private static User handleCreateCommand(Scanner scanner, User currentUser, LinkService linkService) {
        System.out.println("Введите URL, который хотите сократить:");

        String url = scanner.nextLine().trim();

        if (!validateUrl(url)) {
            System.out.println("Некорректный URL. Используйте http:// или https://");
            return currentUser;
        }

        if (currentUser == null) {
            currentUser = new User();
            System.out.println("Создан пользователь: " + currentUser.getId());
        } else {
            System.out.println("Текущий пользователь: " + currentUser.getId());
        }

        System.out.println("URL принят: " + url);

        try {
            ShortLink link = linkService.createShortLink(
                    url,
                    currentUser.getId());

            System.out.println("Короткая ссылка: " + SHORT_LINK_PREFIX + link.getShortCode());
        } catch (IllegalStateException e) {
            System.out.println("Произошла ошибка повторите попытку позже..");
        }

        return currentUser;
    }

    private static boolean validateUrl(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }

        try {
            URI uri = new URI(url.trim());

            String scheme = uri.getScheme();
            String host = uri.getHost();

            boolean validScheme = scheme != null &&
                    (scheme.equalsIgnoreCase("https") ||
                            scheme.equalsIgnoreCase("http"));
            boolean validHost = host != null && !host.isBlank();

            return validScheme && validHost;

        } catch (URISyntaxException e) {
            return false;
        }
    }

    private static void handleFindCommand(
            Scanner scanner,
            LinkService linkService
    ) {
        System.out.println("Введите вашу короткую ссылку или код:");
        String input = scanner.nextLine().trim();


        boolean hasPrefix = input.startsWith(SHORT_LINK_PREFIX);
        String shortCode = input;

        if (hasPrefix) {
            shortCode = input.substring(SHORT_LINK_PREFIX.length());
        }

        if (shortCode.isBlank()) {
            System.out.println("Короткая ссылка не может быть пустой.");

            return;
        }

        ShortLink shortLink = linkService.findByCode(shortCode);

        if (shortLink != null) {
            System.out.println("Исходный URL: " + shortLink.getOriginalUrl());
        } else {
            System.out.println("Ваша ссылка не найдена.");
        }

    }
}
