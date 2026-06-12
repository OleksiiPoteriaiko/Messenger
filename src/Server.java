import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class Server {

    private static final int PORT = 8081;

    static final Set<ClientHandler> clients =
            Collections.synchronizedSet(new HashSet<>());

    static DatabaseHandler dbHandler;

    public static void main(String[] args) {
        System.out.println("Server started on port " + PORT);

        dbHandler = new DatabaseHandler();

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New connection: " + clientSocket.getRemoteSocketAddress());

                ClientHandler handler = new ClientHandler(clientSocket);
                Thread thread = new Thread(handler);
                thread.setDaemon(true);
                thread.start();
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        }
    }
}