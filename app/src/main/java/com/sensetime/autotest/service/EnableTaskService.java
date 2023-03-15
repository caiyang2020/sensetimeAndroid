package com.sensetime.autotest.service;

import android.app.IntentService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.apkfuns.logutils.LogUtils;
import com.sensetime.autotest.entity.DeviceMessage;
import com.sensetime.autotest.entity.Task;
import com.sensetime.autotest.util.FileUtil;
import com.sensetime.autotest.util.HttpUtil;
import com.sensetime.autotest.util.PowerShell;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.SocketTimeoutException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import lombok.SneakyThrows;

public class EnableTaskService extends IntentService {

    private Context mContext;

    List<String[]> gtList = new LinkedList<>();

    private final List<String[]> readyVideo = new LinkedList<>();

    int num = 0;

    int total = 0;

    int process = 0;

    private WebSocketService webSocketService;


    private final Intent intent = new Intent("com.caisang");

    private DeviceMessage<Map<String, Object>> resMsg = new DeviceMessage<>();

    private Map<String, Object> respMap = new HashMap<>(1);

    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     * <p>
     * name Used to name the worker thread, important only for debugging.
     */
    public EnableTaskService() {
        super("EnableTaskService");
        resMsg.setData(respMap);
    }

    public void init(Task task) {
        CountDownLatch prepareTask = new CountDownLatch(3);
        //sdk 准备
        LogUtils.i("Start SDK preparation");
        HttpUtil.downloadFile(mContext, prepareTask, task.getSdkId(), "sdk", task.getSdkRootPath());
//        prepareTask.countDown();
        LogUtils.i("SDK preparation is complete");
        //gt 准备
        LogUtils.i("Start GT preparation");
        HttpUtil.downloadFile(mContext, prepareTask, task.getGtId(), "gt");
        LogUtils.i("Gt preparation is complete");
        //获取已经上传到测试平台的log
        LogUtils.i("Start Log preparation");
        HttpUtil.downloadFile(mContext, prepareTask, task.getId(), "log");
        LogUtils.i("Log preparation is complete");
        try {
            prepareTask.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (FileUtil.checkSdk() && FileUtil.checkGt()) {
            LogUtils.i("SDK和GT准备完毕");
            resMsg.setCode(1);
            respMap.put("info", "SDK和GT准备完毕");
            respMap.put("status", 2);
            respMap.put("id", task.getId());
            respMap.put("taskName", task.getTaskName());
            sendServer();
            respMap.clear();
            //分析生成GT列表
            prepareGtList(mContext, task);
            //程序运行
            runTask(mContext, task);
        } else {
            LogUtils.i("sdk或GT没准备好，不能执行任务");
            resMsg.setCode(1);
            respMap.put("id", task.getId());
            respMap.put("info", "SDK和GT准备出错");
            respMap.put("status", 7);
            sendServer();
        }

    }

    private void bindWebSocket() {
        Intent intent = new Intent(getBaseContext(), WebSocketService.class);
        bindService(intent, coon, BIND_AUTO_CREATE);
    }

    public void prepareGtList(Context context, Task task) {
        InputStreamReader isr;
        String gtName;
        gtName = task.getGtId() + ".csv";
        try {
            isr = new InputStreamReader(new FileInputStream(context.getFilesDir() + "/Gt/" + gtName));
            BufferedReader br = new BufferedReader(isr);
            String line;
            while ((line = br.readLine()) != null) {
                String[] strings = line.split(",");
                strings[0] = strings[0].replace("\uFEFF", "");
                gtList.add(strings);
                total++;
            }
            LogUtils.i("共检测到" + total + "条数据");
        } catch (Exception e) {
            LogUtils.e(e.getMessage());
        }
    }

    @SneakyThrows
    public void runTask(Context context, Task task) {

        final Semaphore taskSemaphore = new Semaphore(0);
        //创建以任务名称创建log保存文件夹
        File dir = new File(context.getFilesDir() + "/Log/" + task.getId());
        if (!dir.exists()) {
            dir.mkdir();
        }
        new Thread(new Runnable() {
            final Semaphore semaphore = new Semaphore(0);
            @Override
            public void run() {
                for (String[] gt : gtList) {
                    try {
                        while (readyVideo.size() > 5) {
                            Thread.sleep(10000);
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    File Logfile = new File(context.getFilesDir() + "/Log/" + task.getId() + "/" + gt[0].replaceAll("/", "^").replaceAll("\\.[a-zA-z0-9]+$", ".log"));
                    if (Logfile.exists()) {
                        num++;
                        if ((num * 100 / total) > process) {
                            process = num * 100 / total;
                            intent.putExtra("process", process);
                            context.sendBroadcast(intent);
                        }
                    } else {
                        String path = gt[0];

                        try {
                            HttpUtil.downloadFile(mContext, semaphore, path, "video");
                            System.out.println("请求下载视频文件");
                            semaphore.acquire();
                            System.out.println("下载视频文件完成");
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        } catch (SocketTimeoutException e) {
                            semaphore.release();
                        }
                        readyVideo.add(gt);
                        taskSemaphore.release();
                    }
                }
                String[] strings = {"finish"};
                readyVideo.add(strings);
            }
        }).start();

        while (true) {
            if (!readyVideo.isEmpty()) {
                if (readyVideo.get(0)[0].equals("finish")) {
                    LogUtils.i("任务运行完成");
                    resMsg.setCode(1);
                    respMap.put("status", 6);
                    respMap.put("id", task.getId());
                    for (int i = 0; i < 3; i++) {
                        sendServer();
                        sleep(3000);
                    }
                    LogUtils.i("finish");
                    WebSocketService.isRunning = false;
                    break;
                }
                try {
                    taskSemaphore.acquire();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                File Logfile = new File(context.getFilesDir() + "/Log/" + task.getTaskName() + "/" + readyVideo.get(0)[0].replaceAll("/", "^").replaceAll("\\.[a-zA-z0-9]+$", ".log"));
                if (Logfile.exists()) {
                    readyVideo.remove(0);
                    continue;
                }
                Log.i("INFO", "正在执行，当前执行视频" + readyVideo.get(0)[0]);
                //拼接字符命令
                String cmd = MessageFormat.format(task.getCmd(), context.getFilesDir() + "/Video/" + readyVideo.get(0)[0].replaceAll("/", "^"), 30,
                        context.getFilesDir() + "/Log/" + task.getId() + "/" + readyVideo.get(0)[0].replaceAll("/", "^").replaceAll("\\.[a-zA-z0-9]+$", ".log"));
                PowerShell.cmd(context, "cd /data/local/tmp/AutoTest/" + task.getSdkRootPath(),
                        "pwd",
                        "source env.sh",
                        "./" + task.getSdkRunPath() + File.separator + task.getRunFunc() + cmd);
//                NfsServer.uploadFile(context.getFilesDir() + "/Log/" + task.getId() + "/" + readyVideo.get(0)[0].replaceAll("/", "^").replaceAll("\\.[a-zA-z0-9]+$", ".log"), task.getId().toString());
                HttpUtil.fileUpload(task.getId(), context.getFilesDir() + "/Log/" + task.getId() + "/" + readyVideo.get(0)[0].replaceAll("/", "^").replaceAll("\\.[a-zA-z0-9]+$", ".log"));
                PowerShell.cmd("cd " + context.getFilesDir() + "/Video",
                        "rm " + readyVideo.get(0)[0].replaceAll("/", "^"));
                readyVideo.remove(0);
                num++;

                if ((num * 100 / total) > process) {
                    process = num * 100 / total;
                    intent.putExtra("process", process);
                    context.sendBroadcast(intent);
                    resMsg.setCode(1);
                    respMap.put("status", 2);
                    respMap.put("id", task.getId());
                    respMap.put("process", process);
                    sendServer();
                    LogUtils.d("taskProcess", "任务: " + task.getTaskName() + "， 进度更新为：" + process);
                }
            }
        }
    }

    private void sendServer() {
        webSocketService.sendMsg(JSON.toJSONString(resMsg));
    }

    static void sleep(long time) {
        try {
            System.gc();
            Thread.sleep(time);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        bindWebSocket();
        LogUtils.e("绑定websockets");
        assert intent != null;
        JSONObject task = JSONObject.parseObject(intent.getExtras().getString("task"));
        mContext = getBaseContext();
        System.out.println(task);
        Task task1 = JSON.toJavaObject(task, Task.class);
        init(task1);
    }

    ServiceConnection coon = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            webSocketService = ((WebSocketService.WebSocketClientBinder) service).getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };

    private void unBindWebSocket() {
//        Intent intent = new Intent(getBaseContext(),WebSocketService.class);
        unbindService(coon);
    }

    @Override
    public void onDestroy() {
        LogUtils.i("运行完成准备解除websocket的使用");
        unbindService(coon);
//        super.onDestroy();
    }
}