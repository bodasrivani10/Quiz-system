import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class QuizServer {
    private static final int DEFAULT_PORT = 5000;
    private static final QuizQuestion[] QUESTIONS = {
        new QuizQuestion(
            "What is a distributed system?",
            "One computer running one program",
            "Multiple computers working together",
            "A computer without a network",
            "A type of programming language", "B"),
        new QuizQuestion(
            "What is a node in a distributed system?",
            "A computer or device participating in the system",
            "A type of keyboard",
            "A programming error",
            "A computer screen", "A"),
        new QuizQuestion(
            "Why is data replication used in distributed systems?",
            "To make the network slower",
            "To remove all copies of data",
            "To improve availability and reliability",
            "To prevent computers from communicating", "C"),
        new QuizQuestion(
            "What does a network failure mean for a distributed system?",
            "A computer or communication link may stop working",
            "All programs are automatically deleted",
            "The system has no computers",
            "The internet becomes a programming language", "A"),
        new QuizQuestion(
            "What is the main purpose of a load balancer?",
            "To distribute work across multiple servers",
            "To store passwords in plain text",
            "To disconnect users from the network",
            "To compile programs", "A")
    };

    private QuizServer() {
    }

    public static void main(String[] args) {
        int port;
        try {
            port = args.length > 0 ? Integer.parseInt(args[0]) : DEFAULT_PORT;
            if (port < 1 || port > 65535) {
                throw new NumberFormatException("Port must be between 1 and 65535.");
            }
        } catch (NumberFormatException error) {
            System.err.println("Usage: java QuizServer [port]");
            System.err.println(error.getMessage());
            System.exit(1);
            return;
        }

        ExecutorService clients = Executors.newCachedThreadPool();
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Client-Server Quiz System listening on port " + port + ".");
            System.out.println("Start QuizClient in another terminal to take the quiz.");
            while (!serverSocket.isClosed()) {
                Socket client = serverSocket.accept();
                System.out.println("Client connected: " + client.getRemoteSocketAddress());
                clients.execute(() -> handleClient(client));
            }
        } catch (IOException error) {
            System.err.println("Quiz server stopped: " + error.getMessage());
            System.exit(1);
        } finally {
            clients.shutdown();
        }
    }

    private static void handleClient(Socket socket) {
        try (Socket client = socket;
             BufferedReader input = new BufferedReader(
                 new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter output = new PrintWriter(
                 new OutputStreamWriter(client.getOutputStream(), StandardCharsets.UTF_8), true)) {
            output.println("READY|" + encode("Welcome to the Client-Server Quiz System!"));
            int score = 0;

            for (int i = 0; i < QUESTIONS.length; i++) {
                sendQuestion(output, i + 1, QUESTIONS[i]);
                String answer = input.readLine();
                while (answer != null && !isValidAnswer(answer)) {
                    output.println("INVALID|" + encode("Enter A, B, C, or D."));
                    answer = input.readLine();
                }
                if (answer == null) {
                    System.out.println("Client disconnected before finishing the quiz.");
                    return;
                }

                if (QUESTIONS[i].getCorrectAnswer().trim().equalsIgnoreCase(answer.trim())) {
                    score++;
                }
                output.println("ACK");
            }

            output.println("RESULT|" + score + "|" + QUESTIONS.length);
            System.out.println("Quiz completed. Score: " + score + "/" + QUESTIONS.length + ".");
        } catch (IOException error) {
            System.err.println("Could not complete a client session: " + error.getMessage());
        }
    }

    private static void sendQuestion(PrintWriter output, int number, QuizQuestion question) {
        output.println("QUESTION|" + number + "|" + QUESTIONS.length
            + "|" + encode(question.getQuestion())
            + "|" + encode(question.getOptionA())
            + "|" + encode(question.getOptionB())
            + "|" + encode(question.getOptionC())
            + "|" + encode(question.getOptionD()));
    }

    private static boolean isValidAnswer(String answer) {
        if (answer == null) {
            return false;
        }
        String normalized = answer.trim();
        return normalized.length() == 1
            && "ABCD".indexOf(normalized.toUpperCase().charAt(0)) >= 0;
    }

    private static String encode(String value) {
        return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }
}
