# Client-Server Quiz System

A Java console quiz using TCP sockets. `QuizServer` sends multiple-choice questions to clients, validates answers, and calculates scores. Each connected client gets its own quiz session.

## Requirements

- JDK 8 or later
- No external libraries

## Compile

Open a terminal in this folder and run:

```powershell
javac QuizQuestion.java QuizServer.java QuizClient.java
```

## Run the quiz

Start the server in one terminal:

```powershell
java QuizServer
```

Start a client in a second terminal:

```powershell
java QuizClient
```

The default server port is `5000`. To use a different port, start the server with `java QuizServer 6000` and connect the client with `java QuizClient localhost 6000`.

For another computer on the same network, replace `localhost` with the server computer's IP address and allow the port through its firewall.
