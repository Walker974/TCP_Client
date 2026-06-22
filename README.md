# TCP Group Chat — Client

JavaFX client for a real-time group chat application over **TCP sockets**. It connects to the central
server, sends messages typed by the user, and displays everything broadcast by the server in real time.

This is the **client** half of the project. The server lives in the companion `TCP_Server` repository.

## Features (mapped to requirements)

| Requirement | Where it lives |
|---|---|
| Enter a username before chatting | `ClientApp.askUsername` (login dialog) |
| Read-only mode if no username given | `ClientApp` `readOnly` flag → input controls disabled |
| Send via **SEND** button or **Enter** | `sendButton.setOnAction` + `messageField.setOnAction` |
| `allUsers` command lists active clients | typed in the field, sent to server |
| `end` / `bye` disconnects | `ClientApp.handleSend` → `ChatClient.disconnect` |
| "Online" status label + visual indicator (circle) | `statusLabel` + `indicator` (Circle) |
| Config (IP/port) from file, args override | `ClientConfig` (args → `client.properties` → default) |
| Model/View separation | `model` package has **zero** JavaFX imports; View talks to Model only via `ClientListener` |

## Architecture

```
            view (JavaFX)                     model (pure Java, no JavaFX)
   ┌─────────────────────────┐        ┌──────────────────────────────────────┐
   │ Launcher → ClientApp     │        │ ClientConfig   (args + client.properties)│
   │ implements ClientListener│◀──────▶│ ChatClient     (socket, reader thread)  │
   │  - chat TextArea         │  calls │ ClientListener (interface = boundary)   │
   │  - input + SEND button   │  back  │                                         │
   │  - Online circle + label │        │                                         │
   └─────────────────────────┘        └──────────────────────────────────────┘
```

The Model never imports JavaFX. `ChatClient` runs a background **reader thread** and reports incoming
lines / connection events through the `ClientListener` interface; `ClientApp` implements it and hops
onto the JavaFX thread with `Platform.runLater`.

## Configuration & command-line arguments

Per requirement 4.1 the client is started with the server address and port:
```bash
java TCPClient <ServerIPAddress> <PortNumber>     # e.g. localhost 3000
```
Resolution order for each value: **command-line argument → `client.properties` → built-in default**.

`src/main/resources/client.properties`:
```properties
server.host=localhost
server.port=3000
```

## Build & Run

Requirements: JDK 23+ and Maven.

```bash
# Run directly during development (uses client.properties defaults)
mvn clean javafx:run

# Or build the standalone executable JAR
mvn clean package
java -jar target/TCPClient.jar localhost 3000
```

On launch a dialog asks for a username. Leave it blank (or press Cancel) to join in **READ-ONLY mode**:
you'll receive messages but the input box and SEND button are disabled.

`mvn package` produces `target/TCPClient.jar` — a self-contained JAR launched via
`org.booking.client.view.Launcher`.

## Wire protocol (UTF-8, line-based)
1. First line sent = username (empty ⇒ read-only).
2. Then each line is a chat message or a command (`allUsers`, `end`, `bye`).
3. Lines received from the server are appended verbatim to the chat area; chat messages arrive as
   `[HH:mm:ss] username: message`.

## Project layout
```
src/main/java/org/booking/client/
  model/  ClientConfig, ClientListener, ChatClient
  view/   Launcher, ClientApp
src/main/resources/
  client.properties, client.css
```
