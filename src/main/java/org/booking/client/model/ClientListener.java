package org.booking.client.model;

public interface ClientListener {
    void onMessage(String message);
    void onConnected();
    void onDisconnected();
    void onError(String error);
}
