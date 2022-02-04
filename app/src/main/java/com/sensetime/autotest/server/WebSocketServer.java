package com.sensetime.autotest.server;

import android.content.Context;
import android.util.Log;

import com.sensetime.autotest.service.EnableTaskService;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;


public class WebSocketServer extends WebSocketClient {

    private boolean state;

    public boolean getState() {
        return state;
    }

    private Context mContext=null;

    private EnableTaskService enableTaskService =new EnableTaskService(mContext);

    public WebSocketServer(Context context, URI serverUri) {

        super(serverUri,new Draft_6455());
        this.mContext=context;
    }

    public WebSocketServer(URI uri) {
        super(uri,new Draft_6455());
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        Log.e("WebSocketClient",handshakedata.toString());
    }

    @Override
    public void onMessage(String message) {
        Log.e("WebSocketClient",message);

    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        Log.e("WebSocketClient",reason);
    }

    @Override
    public void onError(Exception ex) {
        Log.e("WebSocketClient",ex.toString());
    }
}
