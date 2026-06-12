# Java Client-Server Messenger

A simple, multi-threaded client-server chat application written in Java. This project demonstrates network programming using Sockets, GUI creation with Swing, and database integration.

## Features

* **Real-time Group Chat:** Multiple clients can connect to the server and broadcast messages to everyone.
* **Private Messaging:** Send direct messages to specific users using the `/w` command (e.g., `/w Bob Hello!`).
* **Message History:** The server uses an SQLite database to save group chat history, which is automatically loaded for new clients upon connection.
* **File Transfer:** Users can send and receive files (up to 5MB) directly in the chat using Base64 encoding. Downloaded files are saved in the local `Downloads/` directory.
* **Graphical Interface:** A clean, user-friendly GUI built with Java Swing.

## Tech Stack

* **Language:** Java
* **UI Framework:** Swing
* **Database:** SQLite (JDBC)
* **Networking:** `java.net.Socket`, `java.net.ServerSocket`

## How to Run

1. Clone this repository to your local machine.
2. Open the project in an IDE (e.g., IntelliJ IDEA).
3. Ensure the `sqlite-jdbc.jar` driver from the `lib/` folder is added to your project's libraries.
4. Run `Main.java` to start the server on port 8081.
5. Run multiple instances of `ClientGUI.java` to connect clients to the chat.

---
*Developed by Oleksii Poteriaiko*
