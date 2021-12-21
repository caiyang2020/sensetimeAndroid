package com.sensetime.autotest.util;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;


public class WebSocketServer extends WebSocketClient {

    private boolean state;

    public boolean getState() {
        return state;
    }

    public WebSocketServer(URI serverUri) {
        super(serverUri,new Draft_6455());
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        state=true;
        System.out.println("链接成功");
    }

    @Override
    public void onMessage(String message) {
        System.out.println(message);

    }

    @Override
    public void onClose(int code, String reason, boolean remote) {

    }

    @Override
    public void onError(Exception ex) {

    }
}
