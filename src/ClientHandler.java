import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;

public class ClientHandler implements Runnable {

    private final Socket socket;
    private PrintWriter out;
    private String nickname;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    public String getNickname() {
        return nickname;
    }

    @Override
    public void run() {
        try (
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(socket.getInputStream()))
        ) {
            out = new PrintWriter(socket.getOutputStream(), true);

            nickname = in.readLine();
            if (nickname == null || nickname.isBlank()) {
                nickname = "Unknown";
            }

            Server.clients.add(this);
            broadcastUsersList();

            List<String> history = Server.dbHandler.getMessageHistory();
            for (String pastMessage : history) {
                sendMessage(pastMessage);
            }

            broadcast("*** " + nickname + " joined the chat ***");
            System.out.println(nickname + " connected.");

            String message;
            while ((message = in.readLine()) != null) {
                if (message.startsWith("/w ")) {
                    String[] split = message.split(" ", 3);
                    if (split.length == 3) {
                        String targetName = split[1];
                        String privateText = split[2];
                        sendPrivateMessage(targetName, privateText);
                    } else {
                        sendMessage("--- Invalid private message format. Use: /w Name Text ---");
                    }
                }
                else if (message.startsWith("/FILE ")) {
                    String[] parts = message.split(" ", 3);
                    if (parts.length == 3) {
                        String fileName = parts[1];
                        String fileData = parts[2];

                        broadcast("/FILE " + nickname + " " + fileName + " " + fileData);

                        Server.dbHandler.saveMessage(nickname, "[Отправил файл: " + fileName + "]");
                    }
                }
                else {
                    Server.dbHandler.saveMessage(nickname, message);
                    broadcast("[" + nickname + "]: " + message);
                }
            }

        } catch (IOException e) {
            System.out.println(nickname + " disconnected abruptly.");
        } finally {
            Server.clients.remove(this);
            broadcastUsersList();
            broadcast("*** " + nickname + " left the chat ***");
            System.out.println(nickname + " removed from active clients.");
            try {
                socket.close();
            } catch (IOException ignored) {}
        }
    }

    static void broadcast(String message) {
        synchronized (Server.clients) {
            for (ClientHandler client : Server.clients) {
                client.out.println(message);
            }
        }
    }

    static void broadcastUsersList() {
        StringBuilder usersCommand = new StringBuilder("/USERS ");
        synchronized (Server.clients) {
            for (ClientHandler client : Server.clients) {
                usersCommand.append(client.getNickname()).append(",");
            }
        }
        broadcast(usersCommand.toString());
    }

    private void sendPrivateMessage(String targetName, String text) {
        boolean found = false;
        synchronized (Server.clients) {
            for (ClientHandler client : Server.clients) {
                if (client.getNickname().equals(targetName)) {
                    client.sendMessage("[Private from " + this.nickname + "]: " + text);
                    found = true;
                    break;
                }
            }
        }

        if (found) {
            this.sendMessage("[Private to " + targetName + "]: " + text);
        } else {
            this.sendMessage("--- User " + targetName + " not found or offline ---");
        }
    }

    public void sendMessage(String message) {
        out.println(message);
    }
}