//package com.sensetime.autotest.service;
//
//import android.app.Service;
//import android.content.BroadcastReceiver;
//import android.content.Context;
//import android.content.Intent;
//import android.os.IBinder;
//import android.util.Log;
//
//import java.text.SimpleDateFormat;
//import java.util.Date;
//import java.util.Locale;
//import java.util.Timer;
//import java.util.TimerTask;
//
//public class MonitoringService extends Service {
//
//    private final static String TAG = "MonitoringService";
//
//    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
//        @Override
//        public void onReceive(Context context, Intent intent) {
//            if ("kill_self".equals(intent.getAction())) {
//                Log.e(TAG, "onReceive:杀死自己的进程！");
//                killMyselfPid(); // 杀死自己的进程
//            }
//        }
//    };
//
//    private final Timer timer = new Timer();
//    private final TimerTask task = new TimerTask() {
//        @Override
//        public void run() {
//            checkIsAlive();
//        }
//    };
//
//    /**
//     * 检测应用是否活着
//     */
//    private void checkIsAlive() {
//        String format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",
//                Locale.CHINA).format(new Date());
//        Log.e(TAG, "CustodyService Run: " + format);
//
//        boolean AIsRunning = CheckUtil.isClsRunning(
//                MonitoringService.this, "com.xpf.monitor", "com.xpf.monitor.activity.AActivity");
//        boolean BIsRunning = CheckUtil.isClsRunning(
//                MonitoringService.this, "com.xpf.monitor", "com.xpf.monitor.activity.BActivity");
//        boolean b = (AIsRunning || BIsRunning);
//        boolean CIsRunning = CheckUtil.isClsRunning(
//                MonitoringService.this, "com.xpf.monitor", "com.xpf.monitor.activity.CActivity");
//
//        Log.e(TAG, "AIsRunning || BIsRunning is running:" + b + ",CIsRunning:" + CIsRunning);
//
//        if (!CIsRunning) {
//            if (!b) { //如果界面挂掉直接启动AActivity
//                Intent intent = new Intent();
//                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                intent.setClass(MonitoringService.this, AActivity.class);
//                startActivity(intent);
//            }
//        }
//    }
//
//
//    @Override
//    public void onCreate() {
//        super.onCreate();
//        Log.e(TAG, "onCreate: 启动监控服务! ");
//        IntentFilter intentFilter = new IntentFilter();
//        intentFilter.addAction("kill_self");
//        registerReceiver(broadcastReceiver, intentFilter);
//        timer.schedule(task, 0, 10000);// 设置检测的时间周期(毫秒数)
//    }
//
//    @Override
//    public int onStartCommand(Intent intent, int flags, int startId) {
//        return START_STICKY;
//    }
//
//    @Override
//    public IBinder onBind(Intent arg0) {
//        return null;
//    }
//
//    /**
//     * 杀死自身的进程
//     */
//    private void killMyselfPid() {
//        int pid = android.os.Process.myPid();
//        String command = "kill -9 " + pid;
//        Log.e(TAG, "killMyselfPid: " + command);
//        stopService(new Intent(MonitoringService.this, MonitoringService.class));
//        try {
//            Runtime.getRuntime().exec(command);
//            System.exit(0);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//
//    @Override
//    public void onDestroy() {
//        super.onDestroy();
//        unregisterReceiver(broadcastReceiver);
//        if (task != null) {
//            task.cancel();
//        }
//        if (timer != null) {
//            timer.cancel();
//        }
//    }
//
//    @Override
//    public IBinder onBind(Intent intent) {
//        // TODO: Return the communication channel to the service.
//        throw new UnsupportedOperationException("Not yet implemented");
//    }
//}