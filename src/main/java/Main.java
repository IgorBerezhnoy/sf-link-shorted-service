import java.awt.Desktop;
import java.io.IOException;
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

            if (!scanner.hasNextLine()) {
                break;
            }

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

                case "open": {
                    handleOpenCommand(scanner, linkService);
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

                default: {
                    System.out.println("Неизвестная команда: " + command);
                    break;
                }
            }
        }

        scanner.close();
    }

    private static void printHelp() {
        System.out.println("Доступные команды:");
        System.out.println("  help - показать помощь");
        System.out.println("  create - создать короткую ссылку");
        System.out.println("  open - открыть исходную ссылку в браузере");
        System.out.println("  find - найти исходный URL по короткой ссылке");
        System.out.println("  exit - завершить программу");
    }

    private static User handleCreateCommand(Scanner scanner, User currentUser, LinkService linkService) {
        System.out.println("Введите URL, который хотите сократить:");
        String url = scanner.nextLine().trim();

        if (!validateUrl(url)) {
            System.out.println("Некорректный URL. Используйте http:// или https://");
            return currentUser;
        }

        System.out.println("Введите максимальное количество переходов:");

        int maxCount;

        try {
            String input = scanner.nextLine().trim();
            maxCount = Integer.parseInt(input);
        } catch (NumberFormatException e) {
            System.out.println("Ошибка: введите целое число.");
            return currentUser;
        }

        if (!validateMaxCount(maxCount)) {
            System.out.println("Лимит должен быть целым числом больше нуля.");
            return currentUser;
        }

        boolean isNewUser = currentUser == null;
        User userToUse = isNewUser ? new User() : currentUser;

        try {
            ShortLink link = linkService.createShortLink(url, userToUse.getId(), maxCount);

            System.out.println();

            if (isNewUser) {
                System.out.println("Создан пользователь: " + userToUse.getId());
            } else {
                System.out.println("Текущий пользователь: " + userToUse.getId());
            }

            System.out.println("URL принят: " + url);
            System.out.println("Короткая ссылка: " + SHORT_LINK_PREFIX + link.getShortCode());
            System.out.println("Лимит переходов: " + link.getMaxClicks());

            System.out.println();

            return userToUse;

        } catch (IllegalStateException e) {
            System.out.println("Ошибка: не удалось создать уникальную короткую ссылку.");
            return currentUser;

        } catch (IllegalArgumentException e) {
            System.out.println("Ошибка: " + e.getMessage());
            return currentUser;
        }
    }

    private static boolean validateUrl(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }

        try {
            URI uri = new URI(url.trim());

            String scheme = uri.getScheme();
            String host = uri.getHost();

            boolean validScheme = scheme != null
                    && (scheme.equalsIgnoreCase("https") || scheme.equalsIgnoreCase("http"));

            boolean validHost = host != null && !host.isBlank();

            return validScheme && validHost;

        } catch (URISyntaxException e) {
            return false;
        }
    }

    private static boolean validateMaxCount(int count) {
        return count > 0;
    }

    private static ShortLink findLinkFromInput(Scanner scanner, LinkService linkService) {
        System.out.println("Введите вашу короткую ссылку или код:");
        String input = scanner.nextLine().trim();

        String shortCode = input;

        if (input.startsWith(SHORT_LINK_PREFIX)) {
            shortCode = input.substring(SHORT_LINK_PREFIX.length());
        }

        if (shortCode.isBlank()) {
            System.out.println("Короткая ссылка не может быть пустой.");
            return null;
        }

        ShortLink shortLink = linkService.findByCode(shortCode);

        if (shortLink == null) {
            System.out.println("Ваша ссылка не найдена.");
            return null;
        }

        return shortLink;
    }

    private static void handleOpenCommand(Scanner scanner, LinkService linkService) {
        ShortLink shortLink = findLinkFromInput(scanner, linkService);

        if (shortLink == null) {
            return;
        }

        try {
            if (!Desktop.isDesktopSupported()) {
                System.out.println("Desktop не поддерживается в этой среде.");
                return;
            }

            Desktop desktop = Desktop.getDesktop();

            if (!desktop.isSupported(Desktop.Action.BROWSE)) {
                System.out.println("Открытие браузера не поддерживается.");
                return;
            }

            boolean clickRegistered = linkService.registerClick(shortLink.getShortCode());

            if (!clickRegistered) {
                System.out.println("Лимит переходов исчерпан! Ссылка больше недоступна.");
                return;
            }

            URI uri = URI.create(shortLink.getOriginalUrl());
            desktop.browse(uri);

            System.out.println("Запрос на открытие ссылки передан браузеру: " + shortLink.getOriginalUrl());

            System.out
                    .println("Использовано переходов: " + shortLink.getClickCount() + " / " + shortLink.getMaxClicks());

            if (!shortLink.canOpen()) {
                System.out.println("Лимит достигнут. Следующий переход будет заблокирован.");
            }

        } catch (IllegalArgumentException e) {
            System.out.println("Ошибка: некорректный формат URL.");

        } catch (SecurityException e) {
            System.out.println("Ошибка: недостаточно прав для открытия браузера.");

        } catch (UnsupportedOperationException e) {
            System.out.println("Ошибка: открытие браузера не поддерживается в текущей среде.");

        } catch (IOException e) {
            System.out.println("Ошибка: не удалось передать ссылку браузеру.");
            System.err.println("Подробности: " + e.getMessage());
        }
    }

    private static void handleFindCommand(Scanner scanner, LinkService linkService) {
        ShortLink shortLink = findLinkFromInput(scanner, linkService);

        if (shortLink == null) {
            return;
        }

        System.out.println("Исходный URL: " + shortLink.getOriginalUrl());

        System.out.println("Использовано переходов: " + shortLink.getClickCount() + " / " + shortLink.getMaxClicks());
    }
}
