package com.sensetime.autotest.server;

import android.util.Log;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;

import javax.net.ssl.SSLParameters;


public class WebSocketServer extends WebSocketClient {

    @Override
    protected void onSetSSLParameters(SSLParameters sslParameters) {
//        super.onSetSSLParameters(sslParameters);
    }

    public WebSocketServer(URI uri) {
        super(uri, new Draft_6455());
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        Log.e("WebSocketClient", "connect");
    }

    @Override
    public void onMessage(String message) {
        Log.e("WebSocketClient", message);

    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        Log.e("WebSocketClient", reason);
    }

    @Override
    public void onError(Exception ex) {
        Log.e("WebSocketClient", ex.toString());
    }
}
