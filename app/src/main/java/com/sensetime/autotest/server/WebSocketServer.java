package com.sensetime.autotest.server;

import android.content.Context;

import com.alibaba.fastjson.JSON;
import com.sensetime.autotest.entity.Task;
import com.sensetime.autotest.util.EnableTask;

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

    private EnableTask enableTask =new EnableTask(mContext);

    public WebSocketServer(Context context, URI serverUri) {

        super(serverUri,new Draft_6455());
        this.mContext=context;
    }

    public WebSocketServer(URI uri) {
        super(uri,new Draft_6455());
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        state=true;
        System.out.println("链接成功");
    }

    @Override
    public void onMessage(String message) {
        System.out.println(message);
    Task task= JSON.parseObject(message, Task.class);
    enableTask.init(mContext,task,this);
        System.out.println(task);
//    this.send("收到任务初始化完成开始执行");

    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        this.state=false;
        System.out.println(code);
        System.out.println(reason);
        System.out.println(remote);
    }

    @Override
    public void onError(Exception ex) {

    }
}
