package org.booking.client.model;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ClientConfig {

    private static final String RESOURCE = "/client.properties";

    private final String host;
    private final int port;

    public ClientConfig(String hostArg, String portArg) {
        Properties props = new Properties();
        try (InputStream in = ClientConfig.class.getResourceAsStream(RESOURCE)) {
            if (in != null) {
                props.load(in);
            }
        } catch (IOException e) {
            System.err.println("Could not read " + RESOURCE + ", using defaults: " + e.getMessage());
        }

        this.host = (hostArg != null && !hostArg.isBlank())
                ? hostArg.trim()
                : props.getProperty("server.host", "localhost").trim();

        String portStr = (portArg != null && !portArg.isBlank())
                ? portArg.trim()
                : props.getProperty("server.port", "3000").trim();
        this.port = Integer.parseInt(portStr);
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }
}
