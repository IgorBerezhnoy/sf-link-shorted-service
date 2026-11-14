import java.net.URI;
import java.net.URISyntaxException;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
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

                case "create": {
                    currentUser = handleCreateCommand(scanner, currentUser);

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
        System.out.println("  exit - завершить программу");
    }

    private static User handleCreateCommand(Scanner scanner, User currentUser) {
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
}

