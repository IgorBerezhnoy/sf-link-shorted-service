import java.net.URI;
import java.net.URISyntaxException;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

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
                    handleCreateCommand(scanner);

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

    private static void handleCreateCommand(Scanner scanner) {
        System.out.println("Введите URL, который хотите сократить:");

        String url = scanner.nextLine().trim();

        if (!validateUrl(url)) {
            System.out.println("Некорректный URL. Используйте http:// или https://");
            return;
        }

        System.out.println("URL принят: " + url);
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

