import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.util.Base64;

public class ClientGUI {

    private static final String HOST = "localhost";
    private static final int PORT = 8081;

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    private JFrame frame;
    private JTextArea chatArea;
    private JTextField inputField;
    private JButton sendButton;
    private JButton attachButton; // НОВАЯ КНОПКА

    private DefaultListModel<String> usersListModel;
    private JList<String> usersList;

    private String nickname;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ClientGUI().launch());
    }

    private void launch() {
        nickname = JOptionPane.showInputDialog(
                null,
                "Enter your nickname:",
                "Chat — Connect",
                JOptionPane.PLAIN_MESSAGE
        );

        if (nickname == null || nickname.isBlank()) {
            System.exit(0);
        }
        nickname = nickname.trim();

        if (!connectToServer()) {
            JOptionPane.showMessageDialog(null,
                    "Could not connect to server " + HOST + ":" + PORT,
                    "Connection Error",
                    JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

        buildUI();
        startReceivingMessages();
    }

    private boolean connectToServer() {
        try {
            socket = new Socket(HOST, PORT);
            in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            out.println(nickname);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private void buildUI() {
        frame = new JFrame("Chat — " + nickname);
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.setSize(750, 450);
        frame.setLocationRelativeTo(null);

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        chatArea.setFont(new Font("Monospaced", Font.PLAIN, 13));
        JScrollPane scrollPane = new JScrollPane(chatArea);

        usersListModel = new DefaultListModel<>();
        usersList = new JList<>(usersListModel);
        usersList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        usersList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    String selectedUser = usersList.getSelectedValue();
                    if (selectedUser != null && !selectedUser.equals(nickname)) {
                        inputField.setText("/w " + selectedUser + " ");
                        inputField.requestFocusInWindow();
                    }
                }
            }
        });

        JScrollPane usersScrollPane = new JScrollPane(usersList);
        usersScrollPane.setPreferredSize(new Dimension(150, 0));
        usersScrollPane.setBorder(BorderFactory.createTitledBorder("Online"));

        inputField = new JTextField();
        sendButton = new JButton("Send");
        attachButton = new JButton("📎 File"); // СОЗДАЕМ КНОПКУ ФАЙЛА

        JPanel bottomPanel = new JPanel(new BorderLayout(6, 0));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(4, 6, 6, 6));

        bottomPanel.add(attachButton, BorderLayout.WEST); // Добавляем кнопку слева
        bottomPanel.add(inputField, BorderLayout.CENTER);
        bottomPanel.add(sendButton, BorderLayout.EAST);

        frame.getContentPane().add(scrollPane, BorderLayout.CENTER);
        frame.getContentPane().add(usersScrollPane, BorderLayout.EAST);
        frame.getContentPane().add(bottomPanel, BorderLayout.SOUTH);

        ActionListener sendAction = e -> sendMessage();
        sendButton.addActionListener(sendAction);
        inputField.addActionListener(sendAction);

        // ОБРАБОТЧИК КНОПКИ ФАЙЛА
        attachButton.addActionListener(e -> sendFile());

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                disconnect();
                frame.dispose();
                System.exit(0);
            }
        });

        frame.setVisible(true);
        inputField.requestFocusInWindow();
    }

    private void sendMessage() {
        String text = inputField.getText().trim();
        if (text.isEmpty()) return;

        out.println(text);
        inputField.setText("");
        inputField.requestFocusInWindow();
    }

    private void sendFile() {
        JFileChooser fileChooser = new JFileChooser();
        int result = fileChooser.showOpenDialog(frame);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();

            if (file.length() > 5 * 1024 * 1024) {
                JOptionPane.showMessageDialog(frame, "File is too big! Max size is 5MB.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            try {
                byte[] fileBytes = Files.readAllBytes(file.toPath());
                String base64Data = Base64.getEncoder().encodeToString(fileBytes);

                out.println("/FILE " + file.getName() + " " + base64Data);
                appendSystemMessage("You sent a file: " + file.getName());

            } catch (IOException ex) {
                JOptionPane.showMessageDialog(frame, "Error reading file", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void startReceivingMessages() {
        Thread receiverThread = new Thread(() -> {
            try {
                String line;
                while ((line = in.readLine()) != null) {
                    if (line.startsWith("/USERS ")) {
                        String usersStr = line.substring(7);
                        String[] usersArray = usersStr.split(",");
                        SwingUtilities.invokeLater(() -> {
                            usersListModel.clear();
                            for (String u : usersArray) {
                                if (!u.isBlank()) {
                                    usersListModel.addElement(u);
                                }
                            }
                        });
                    }
                    else if (line.startsWith("/FILE ")) {
                        String[] parts = line.split(" ", 4);
                        if (parts.length == 4) {
                            String sender = parts[1];
                            String fileName = parts[2];
                            String base64Data = parts[3];

                            if (!sender.equals(nickname)) {
                                receiveFile(sender, fileName, base64Data);
                            }
                        }
                    }
                    else {
                        final String message = line + "\n";
                        SwingUtilities.invokeLater(() -> {
                            chatArea.append(message);
                            chatArea.setCaretPosition(chatArea.getDocument().getLength());
                        });
                    }
                }
            } catch (IOException e) {
                SwingUtilities.invokeLater(() ->
                        appendSystemMessage("Disconnected from server."));
            }
        });
        receiverThread.setDaemon(true);
        receiverThread.start();
    }

    private void receiveFile(String sender, String fileName, String base64Data) {
        try {
            byte[] decodedBytes = Base64.getDecoder().decode(base64Data);

            File dir = new File("Downloads");
            if (!dir.exists()) {
                dir.mkdir();
            }

            File destFile = new File(dir, fileName);
            Files.write(destFile.toPath(), decodedBytes);

            SwingUtilities.invokeLater(() ->
                    appendSystemMessage(sender + " sent a file. Saved to: " + destFile.getAbsolutePath())
            );

        } catch (Exception e) {
            SwingUtilities.invokeLater(() ->
                    appendSystemMessage("Failed to download file from " + sender)
            );
        }
    }

    private void disconnect() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException ignored) {}
    }

    private void appendSystemMessage(String text) {
        chatArea.append("--- " + text + " ---\n");
    }
}