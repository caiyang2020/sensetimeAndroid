package com.sensetime.autotest.service;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.apkfuns.logutils.LogUtils;
import com.sensetime.autotest.entity.DeviceMessage;
import com.sensetime.autotest.server.WebSocketServer;
import com.sensetime.autotest.util.MonitoringUtil;
import com.sensetime.autotest.util.Wsutil;

import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

public class WebSocketService extends Service {

    public static WebSocketService instance ;

    private final static String TAG = "WebSocketClient";

    private final static int GRAY_SERVICE_ID = 1001;

    private static final long CLOSE_RECON_TIME = 3000;

    public static boolean isRunning = false;

    private URI uri;

    public WebSocketServer client;

    Intent intentTask = new Intent();

    public static WebSocketService singleton;

    @Override
    public void onCreate() {
        super.onCreate();
        instance=this;
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
//        return START_STICKY;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "onStartCommand: 被调用了");
        if (client != null && client.isOpen()) {
            String message = intent.getStringExtra("message");
            Log.i(TAG, "收到消息："+message);
            sendMsg("{\"code\":0,\"data\":{\"status\":0}}");
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        closeConnect();
        LogUtils.w("ws被销毁");
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void initSocketClient() {
        URI uri = URI.create(Wsutil.ws + Wsutil.devicesID);
        Log.i(TAG,"正在连接服务："+Wsutil.ws+Wsutil.devicesID);
        client = new WebSocketServer(uri) {
            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void onMessage(String message) {
                DeviceMessage deviceMessage = JSON.parseObject(message, DeviceMessage.class);
                switch (deviceMessage.getCode()) {
                    case 0:
                        DeviceMessage<Map<String, Object>> resMsg = new DeviceMessage<>();
                        Map<String, Object> respMap = new HashMap<>(1);
                        LogUtils.i("isRunning："+isRunning);
                        LogUtils.i("com.sensetime.autotest.service.EnableTaskService："+MonitoringUtil.isServiceWorked(getBaseContext(), "com.sensetime.autotest.service.EnableTaskService"));
                        if ( isRunning || MonitoringUtil.isServiceWorked(getBaseContext(), "com.sensetime.autotest.service.EnableTaskService")) {
                            resMsg.setCode(0);
                            respMap.put("status", 1);
                            resMsg.setData(respMap);
                            sendMsg(JSON.toJSONString(resMsg));
                        } else {
                            resMsg.setCode(0);
                            respMap.put("status", 0);
                            resMsg.setData(respMap);
                            sendMsg(JSON.toJSONString(resMsg));
                        }
                        break;
                    case 1:
                        LogUtils.i("收到服务端发送的任务，开始运行任务");
                        JSONObject jsonObject = JSONObject.parseObject(message);
                        JSONObject json1 = JSONObject.parseObject(jsonObject.getString("data"));
                        Log.i(TAG, "onMessage: "+json1.get("cmd"));
                        intentTask.setPackage(getPackageName());
                        intentTask.setAction("com.auto.test");
                        Bundle bundle = new Bundle();
                        bundle.putString("task", JSONObject.toJSONString(json1));
                        intentTask.putExtras(bundle);
                        startService(intentTask);
                        LogUtils.i("任务启动");
                        break;

                    case 2:
                        LogUtils.i("收到占用信号消息，开始占用机器");
                        isRunning=true;
                        DeviceMessage<Map<String, Object>> resMsg1 = new DeviceMessage<>();
                        Map<String, Object> respMap1 = new HashMap<>(1);
                        resMsg1.setCode(0);
                        respMap1.put("status", 1);
                        resMsg1.setData(respMap1);
                        sendMsg(JSON.toJSONString(resMsg1));
                }
            }

            @Override
            public void onOpen(ServerHandshake handshakedata) {
                super.onOpen(handshakedata);
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {//在连接断开时调用
//                LogUtil.e(TAG, "onClose() 连接断开_reason：" + reason);
                mHandler.removeCallbacks(heartBeatRunnable);
                mHandler.postDelayed(heartBeatRunnable, CLOSE_RECON_TIME);//开启心跳检测
            }

            @Override
            public void onError(Exception ex) {//在连接出错时调用
//                LogUtil.e(TAG, "onError() 连接出错：" + ex.getMessage());
                mHandler.removeCallbacks(heartBeatRunnable);
                mHandler.postDelayed(heartBeatRunnable, CLOSE_RECON_TIME);//开启心跳检测
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
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (null != client && client.isOpen()) {
                    client.send(msg);
                }
            }
        }).start();
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
    private final Handler mHandler = new Handler();
    private final Runnable heartBeatRunnable = new Runnable() {
        @Override
        public void run() {
//            client.send("心跳包");
//            Log.e("JWebSocketClientService", "心跳包检测websocket连接状态");
            if (client != null) {
                if (client.isClosed()) {
                    reconnectWs();
                }
            } else {
                //如果client已为空，重新初始化连接
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
                    Thread.sleep(1000);
                    Log.e("Websocket", "开启重连");
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