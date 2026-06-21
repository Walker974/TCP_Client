package org.booking.client.model;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;


public class ChatClient {

    private final String host;
    private final int port;
    private final ClientListener listener;

    private Socket socket;
    private PrintWriter out;
    private volatile boolean connected;

    public ChatClient(String host, int port, ClientListener listener) {
        this.host = host;
        this.port = port;
        this.listener = listener;
    }

    public void connect(String username) {
        try {
            socket = new Socket(host, port);
            out = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));

            out.println(username == null ? "" : username);
            connected = true;
            listener.onConnected();

            Thread reader = new Thread(() -> readLoop(in), "server-reader");
            reader.setDaemon(true);
            reader.start();
        } catch (IOException e) {
            listener.onError("Could not connect to " + host + ":" + port + " - " + e.getMessage());
        }
    }

    private void readLoop(BufferedReader in) {
        try {
            String line;
            while ((line = in.readLine()) != null) {
                listener.onMessage(line);
            }
        } catch (IOException e) {
            if (connected) {
                listener.onError("Connection lost: " + e.getMessage());
            }
        } finally {
            connected = false;
            listener.onDisconnected();
        }
    }

    public void send(String text) {
        if (connected && out != null) {
            out.println(text);
        }
    }

    public boolean isConnected() {
        return connected;
    }

    public void disconnect() {
        if (connected) {
            send("end");
        }
        connected = false;
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException ignored) {
        }
    }
}
