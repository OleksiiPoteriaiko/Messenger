import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHandler {
    private static final String DB_URL = "jdbc:sqlite:chat_history.db";

    public DatabaseHandler() {
        initDatabase();
    }

    private void initDatabase() {
        String createUsersTable = "CREATE TABLE IF NOT EXISTS users (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "username TEXT UNIQUE NOT NULL" +
                ");";

        String createMessagesTable = "CREATE TABLE IF NOT EXISTS messages (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "sender_id INTEGER, " +
                "text TEXT NOT NULL, " +
                "created_at DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                "FOREIGN KEY(sender_id) REFERENCES users(id)" +
                ");";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {

            stmt.execute(createUsersTable);
            stmt.execute(createMessagesTable);
            System.out.println("Database initialized successfully.");

        } catch (SQLException e) {
            System.err.println("Error initializing database: " + e.getMessage());
        }
    }

    private int getOrCreateUserId(String username) {
        String selectSql = "SELECT id FROM users WHERE username = ?";
        String insertSql = "INSERT INTO users(username) VALUES(?)";

        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            try (PreparedStatement pstmt = conn.prepareStatement(selectSql)) {
                pstmt.setString(1, username);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
            // Если не нашли — создаем
            try (PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
                pstmt.setString(1, username);
                pstmt.executeUpdate();
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        return generatedKeys.getInt(1);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting/creating user: " + e.getMessage());
        }
        return -1;
    }

    public void saveMessage(String username, String text) {
        int userId = getOrCreateUserId(username);
        if (userId == -1) return;

        String insertSql = "INSERT INTO messages(sender_id, text) VALUES(?, ?)";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(insertSql)) {

            pstmt.setInt(1, userId);
            pstmt.setString(2, text);
            pstmt.executeUpdate();

        } catch (SQLException e) {
            System.err.println("Error saving message: " + e.getMessage());
        }
    }

    public List<String> getMessageHistory() {
        List<String> history = new ArrayList<>();
        String selectSql = "SELECT u.username, m.text, m.created_at " +
                "FROM messages m " +
                "JOIN users u ON m.sender_id = u.id " +
                "ORDER BY m.id DESC LIMIT 50";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(selectSql)) {

            List<String> temp = new ArrayList<>();
            while (rs.next()) {
                String username = rs.getString("username");
                String text = rs.getString("text");
                temp.add("[" + username + "]: " + text);
            }
            for (int i = temp.size() - 1; i >= 0; i--) {
                history.add(temp.get(i));
            }

        } catch (SQLException e) {
            System.err.println("Error loading history: " + e.getMessage());
        }
        return history;
    }
}