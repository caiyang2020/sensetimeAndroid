package com.sensetime.autotest.service;

import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import com.alibaba.fastjson.JSON;
import com.sensetime.autotest.entity.Task;
import com.sensetime.autotest.server.WebSocketServer;
import com.sensetime.autotest.util.Wsutil;
import org.java_websocket.handshake.ServerHandshake;
import java.net.URI;


public class WebSocketService extends Service {

    private final static int GRAY_SERVICE_ID = 1001;

    private Context mContext;

    private URI uri;

    public WebSocketServer client;

    private JWebSocketClientBinder mBinder = new JWebSocketClientBinder();

    //设置intent用来向MainActivity传递消息修改UI
    private Intent intent= new Intent("com.caisang");

    //用于Activity和service通讯
    public class JWebSocketClientBinder extends Binder {
        public WebSocketService getService() {
            return WebSocketService.this;
        }
    }
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //初始化websocket
        initSocketClient();
        mHandler.postDelayed(heartBeatRunnable, HEART_BEAT_RATE);//开启心跳检测

//        //设置service为前台服务，提高优先级
//        if (Build.VERSION.SDK_INT < 18) {
//            //Android4.3以下 ，隐藏Notification上的图标
//            startForeground(GRAY_SERVICE_ID, new Notification());
//        } else if(Build.VERSION.SDK_INT>18 && Build.VERSION.SDK_INT<25){
//            //Android4.3 - Android7.0，隐藏Notification上的图标
//            Intent innerIntent = new Intent(this, GrayInnerService.class);
//            startService(innerIntent);
//            startForeground(GRAY_SERVICE_ID, new Notification());
//        }else{
//            //Android7.0以上app启动后通知栏会出现一条"正在运行"的通知
//            startForeground(GRAY_SERVICE_ID, new Notification());
//        }

//        acquireWakeLock();
        return START_STICKY;
    }

    public static class GrayInnerService extends Service {

        @Override
        public int onStartCommand(Intent intent, int flags, int startId) {
            startForeground(GRAY_SERVICE_ID, new Notification());
            stopForeground(true);
            stopSelf();
            return super.onStartCommand(intent, flags, startId);
        }
        @Override
        public IBinder onBind(Intent intent) {
            return null;
        }
    }

    @Override
    public void onDestroy() {
        closeConnect();
        super.onDestroy();
    }
    private void initSocketClient() {
        URI uri = URI.create(Wsutil.ws+Wsutil.devicesID);
        client = new WebSocketServer(uri) {
            @Override
            public void onMessage(String message) {
                System.out.println(message);
                EnableTaskService enableTaskService = new EnableTaskService(getBaseContext(),client);
                Task task= JSON.parseObject(message, Task.class);
                intent.putExtra("task",JSON.toJSONString(task));
                sendBroadcast(intent);
                enableTaskService.init(task);

            }

            @Override
            public void onOpen(ServerHandshake handshakedata) {
                super.onOpen(handshakedata);
            }
        };
        connect();
    }

    private void connect() {
        new Thread() {
            @Override
            public void run() {
                try {
                    client.connectBlocking();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }.start();

    }


    public void sendMsg(String msg) {
        if (null != client) {
            client.send(msg);
        }
    }

    private void closeConnect() {
        try {
            if (null != client) {
                client.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            client = null;
        }
    }


    private static final long HEART_BEAT_RATE = 10 * 1000;//每隔10秒进行一次对长连接的心跳检测
    private Handler mHandler = new Handler();
    private Runnable heartBeatRunnable = new Runnable() {
        @Override
        public void run() {
//            Log.e("JWebSocketClientService", "心跳包检测websocket连接状态");
            if (client != null) {
                if (client.isClosed()) {
                    reconnectWs();
                }
            } else {
                //如果client已为空，重新初始化连接
                client = null;
                initSocketClient();
            }
            //每隔一定的时间，对长连接进行一次心跳检测
            mHandler.postDelayed(this, HEART_BEAT_RATE);
        }
    };

    private void reconnectWs() {
        mHandler.removeCallbacks(heartBeatRunnable);
        new Thread() {
            @Override
            public void run() {
                try {
//                    client.send("000000000");
                    Log.e("JWebSocketClientService", "开启重连");
                    client.reconnectBlocking();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }



//    //获取电源锁，保持该服务在屏幕熄灭时仍然获取CPU时，保持运行
//    @SuppressLint("InvalidWakeLockTag")
//    private void acquireWakeLock()
//    {
//        if (null == wakeLock)
//        {
//            PowerManager pm = (PowerManager)this.getSystemService(Context.POWER_SERVICE);
//            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK|PowerManager.ON_AFTER_RELEASE, "PostLocationService");
//            if (null != wakeLock)
//            {
//                wakeLock.acquire();
//            }
//        }
//    }




    }