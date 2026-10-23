import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public final class QuizClient {
    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 5000;

    private QuizClient() {
    }

    public static void main(String[] args) {
        String host = args.length > 0 ? args[0] : DEFAULT_HOST;
        int port;
        try {
            port = args.length > 1 ? Integer.parseInt(args[1]) : DEFAULT_PORT;
            if (port < 1 || port > 65535) {
                throw new NumberFormatException("Port must be between 1 and 65535.");
            }
        } catch (NumberFormatException error) {
            System.err.println("Usage: java QuizClient [server-host] [port]");
            System.err.println(error.getMessage());
            System.exit(1);
            return;
        }

        try (Socket socket = new Socket(host, port);
             BufferedReader server = new BufferedReader(
                 new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter output = new PrintWriter(
                 new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
             BufferedReader console = new BufferedReader(
                 new InputStreamReader(System.in, StandardCharsets.UTF_8))) {
            String greeting = server.readLine();
            if (greeting == null || !greeting.startsWith("READY|")) {
                throw new IOException("The server sent an unexpected greeting.");
            }
            System.out.println(decode(greeting.substring("READY|".length())));

            String message;
            while ((message = server.readLine()) != null) {
                String[] parts = message.split("\\|", -1);
                if ("QUESTION".equals(parts[0])) {
                    askQuestion(parts, output, server, console);
                } else if ("RESULT".equals(parts[0])) {
                    if (parts.length != 3) {
                        throw new IOException("The server sent an invalid result.");
                    }
                    System.out.println("\nQuiz complete! Your score: " + parts[1] + "/" + parts[2]);
                    return;
                } else {
                    throw new IOException("The server sent an unknown message.");
                }
            }
            throw new IOException("The server closed the connection before the quiz finished.");
        } catch (IOException error) {
            System.err.println("Could not complete the quiz: " + error.getMessage());
            System.err.println("Check that the quiz server is running at " + host + ":" + port + ".");
            System.exit(1);
        }
    }

    private static void askQuestion(String[] parts, PrintWriter output, BufferedReader server,
                                    BufferedReader console) throws IOException {
        if (parts.length != 8) {
            throw new IOException("The server sent an invalid question.");
        }

        System.out.println("\nQuestion " + parts[1] + " of " + parts[2]);
        System.out.println(decode(parts[3]));
        System.out.println("A. " + decode(parts[4]));
        System.out.println("B. " + decode(parts[5]));
        System.out.println("C. " + decode(parts[6]));
        System.out.println("D. " + decode(parts[7]));

        while (true) {
            System.out.print("Your answer (A-D): ");
            String answer = console.readLine();
            if (answer == null) {
                throw new IOException("Input ended before the quiz was finished.");
            }
            output.println(answer.trim());

            String response = server.readLine();
            if (response == null) {
                throw new IOException("The server closed the connection.");
            }
            if (response.startsWith("INVALID|")) {
                System.out.println(decode(response.substring("INVALID|".length())));
                continue;
            }
            if ("ACK".equals(response)) {
                System.out.println();
                return;
            }
            throw new IOException("The server sent an unexpected response.");
        }
    }

    private static String decode(String value) throws IOException {
        try {
            return new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException error) {
            throw new IOException("The server sent invalid text data.", error);
        }
    }
}
