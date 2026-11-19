import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.Scanner;
import java.util.UUID;
import java.nio.file.Path;
import java.util.NoSuchElementException;

public class Main {

    private static final String SHORT_LINK_PREFIX = "short-link.ru/";

    public static void main(String[] args) {
        AppConfig config;

        try {
            config = AppConfig.load(Path.of("application.properties"));
        } catch (IOException e) {
            System.err.println(
                    "Не удалось загрузить настройки: " + e.getMessage()
            );
            return;
        }

        LinkRepository repository = new LinkRepository();
        ShortCodeGenerator generator =
                new ShortCodeGenerator(config.codeLength());

        LinkService linkService = new LinkService(
                repository,
                generator,
                config.maxAllowedClicks()
        );

        UserRepository userRepository = new UserRepository();


        FileStorage fileStorage = new FileStorage(
                Path.of("data", "state.properties")
        );

        try {
            fileStorage.load(userRepository, repository);
            System.out.println("Сохранённые данные загружены.");
        } catch (IOException e) {
            System.err.println("Ошибка загрузки данных: " + e.getMessage());
            return;
        }

        Scanner scanner = new Scanner(System.in);
        User currentUser = null;

        System.out.println("Добро пожаловать в Link Shorted Service!");
        System.out.println("Введите 'help' для списка команд.");


        boolean running = true;

        try {
            while (running) {
                System.out.print("> ");

                if (!scanner.hasNextLine()) {
                    break;
                }

                String command = scanner.nextLine().trim();
                String[] commandParts = command.split("\\s+");
                String mainCommand = commandParts[0];

                // automatic removal of outdated links
                int removedLinksCount = linkService.removeExpiredLinks();

                if (removedLinksCount > 0) {
                    System.out.println("Автоматическая очистка: удалено просроченных ссылок: " + removedLinksCount);
                }

                try {
                    switch (mainCommand) {
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
                            currentUser = handleCreateCommand(scanner, currentUser, linkService, userRepository, config);
                            break;
                        }

                        case "user": {
                            currentUser = handleUserCommand(
                                    commandParts, currentUser, userRepository
                            );
                            break;
                        }

                        case "link": {
                            handleLinkCommand(commandParts, currentUser, linkService);
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
                } catch (NoSuchElementException e) {
                    System.out.println("\nВвод завершён. Завершаем программу...");
                    running = false;
                }
            }

            scanner.close();
        } finally {
            try {
                linkService.removeExpiredLinks();

                fileStorage.save(userRepository, repository);

                System.out.println("Данные успешно сохранены.");
            } catch (IOException e) {
                System.err.println(
                        "Ошибка сохранения данных: " + e.getMessage()
                );
            }

            scanner.close();
        }
    }


    private static void printHelp() {
        System.out.println("Доступные команды:");
        System.out.println("  help - показать помощь");
        System.out.println("  create - создать короткую ссылку");
        System.out.println("  open - открыть исходную ссылку в браузере");
        System.out.println("  find - найти исходный URL по короткой ссылке");
        System.out.println("  user new - начать работу нового пользователя");
        System.out.println("  user switch <UUID> - переключиться на другого пользователя по UUID");
        System.out.println("  user current - показать текущий UUID");
        System.out.println("  user list - показать список всех пользователей");
        System.out.println("  link delete <code> - удалить ссылку по коду");
        System.out.println("  link limit <code> <number> - изменить лимит ссылки по коду");
        System.out.println("  exit - завершить программу");
    }

    private static User handleCreateCommand(Scanner scanner,
                                            User currentUser,
                                            LinkService linkService,
                                            UserRepository userRepository,
                                            AppConfig config) {
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

        if (maxCount > config.maxAllowedClicks()) {
            System.out.println("Лимит не должен превышать максимально разрешённое значение.");
            System.out.println(
                    "Максимально допустимое количество переходов: "
                            + config.maxAllowedClicks()
            );
            return currentUser;
        }

        Duration lifetime = config.lifetime();


        boolean isNewUser = currentUser == null;
        User userToUse = isNewUser ? new User() : currentUser;

        try {
            ShortLink link = linkService.createShortLink(url, userToUse.getId(), maxCount, lifetime);

            if (isNewUser) {
                userRepository.add(userToUse);
            }


            System.out.println();

            if (isNewUser) {
                System.out.println("Создан пользователь: " + userToUse.getId());
            } else {
                System.out.println("Текущий пользователь: " + userToUse.getId());
            }

            System.out.println("URL принят: " + url);
            System.out.println("Короткая ссылка: " + SHORT_LINK_PREFIX + link.getShortCode());
            System.out.println("Лимит переходов: " + link.getMaxClicks());
            System.out.println(
                    "Срок действия ссылки: " + lifetime.toHours() + " ч."
            );

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

        if (shortLink.isExpired()) {
            System.out.println("Срок действия ссылки истёк!");
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
                if (shortLink.isExpired()) {
                    System.out.println("Срок действия ссылки истёк!");
                } else {
                    System.out.println("Лимит переходов исчерпан! Ссылка больше недоступна.");

                }
                return;
            }

            URI uri = URI.create(shortLink.getOriginalUrl());
            desktop.browse(uri);

            System.out.println();
            System.out.println("Запрос на открытие ссылки передан браузеру: " + shortLink.getOriginalUrl());

            System.out
                    .println("Использовано переходов: " + shortLink.getClickCount() + " / " + shortLink.getMaxClicks());

            if (shortLink.getClickCount() >= shortLink.getMaxClicks()) {
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

    private static User handleUserCommand(
            String[] commandParts,
            User currentUser,
            UserRepository userRepository
    ) {
        if (commandParts.length < 2) {
            System.out.println("Доступные команды: user new, switch, current, list");
            return currentUser;
        }

        String action = commandParts[1];

        switch (action) {
            case "new": {
                if (commandParts.length != 2) {
                    System.out.println("Использование: user new");
                    return currentUser;
                }

                System.out.println("Начата новая сессия пользователя.");
                System.out.println("UUID будет создан при создании первой ссылки.");
                return null;
            }

            case "current": {
                if (commandParts.length != 2) {
                    System.out.println("Использование: user current");
                    return currentUser;
                }

                if (currentUser == null) {
                    System.out.println("Текущий пользователь не выбран.");
                } else {
                    System.out.println("Текущий UUID: " + currentUser.getId());
                }
                return currentUser;
            }

            case "list": {
                if (commandParts.length != 2) {
                    System.out.println("Использование: user list");
                    return currentUser;
                }

                if (userRepository.findAll().isEmpty()) {
                    System.out.println("Пользователей пока нет.");
                    return currentUser;
                }

                System.out.println("Список пользователей:");

                for (User user : userRepository.findAll()) {
                    System.out.println(user.getId());
                }

                return currentUser;
            }

            case "switch": {
                if (commandParts.length != 3) {
                    System.out.println("Использование: user switch <UUID>");
                    return currentUser;
                }

                try {
                    UUID userId = UUID.fromString(commandParts[2]);
                    User user = userRepository.findById(userId);

                    if (user == null) {
                        System.out.println("Пользователь не найден.");
                        return currentUser;
                    }

                    System.out.println("Вы переключились на пользователя: " + userId);
                    return user;

                } catch (IllegalArgumentException e) {
                    System.out.println("Некорректный формат UUID.");
                    return currentUser;
                }
            }

            default: {
                System.out.println("Неизвестная команда пользователя: " + action);
                return currentUser;
            }
        }
    }

    private static void handleLinkCommand(
            String[] commandParts,
            User currentUser,
            LinkService linkService
    ) {
        if (currentUser == null) {
            System.out.println("Нет активного пользователя.");
            return;
        }

        if (commandParts.length < 2) {
            System.out.println("Доступные команды: link delete, link limit");
            return;
        }

        String action = commandParts[1];

        switch (action) {
            case "delete": {
                if (commandParts.length != 3) {
                    System.out.println("Использование: link delete <code>");
                    return;
                }

                boolean deleted = linkService.deleteLink(
                        commandParts[2],
                        currentUser.getId()
                );

                if (deleted) {
                    System.out.println("Ссылка успешно удалена.");
                } else {
                    System.out.println("Ссылка не найдена или у вас нет прав.");
                }
                break;
            }

            case "limit": {
                if (commandParts.length != 4) {
                    System.out.println("Использование: link limit <code> <number>");
                    return;
                }

                try {
                    int newMaxClicks = Integer.parseInt(commandParts[3]);

                    boolean updated = linkService.updateClickLimit(
                            commandParts[2],
                            currentUser.getId(),
                            newMaxClicks
                    );

                    if (updated) {
                        System.out.println("Лимит переходов обновлён.");
                    } else {
                        System.out.println("Ссылка не найдена или у вас нет прав.");
                    }

                } catch (NumberFormatException e) {
                    System.out.println("Ошибка: лимит должен быть целым числом.");
                } catch (IllegalArgumentException e) {
                    System.out.println("Ошибка: " + e.getMessage());
                }
                break;
            }

            default: {
                System.out.println("Неизвестная команда: link " + action);
            }
        }
    }
}
