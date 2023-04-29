package com.sensetime.autotest.service;

import android.annotation.SuppressLint;
import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import androidx.annotation.Nullable;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.apkfuns.logutils.LogUtils;
import com.sensetime.autotest.MainActivity;
import com.sensetime.autotest.config.ThreadPool;
import com.sensetime.autotest.entity.DeviceMessage;
import com.sensetime.autotest.entity.Task;
import com.sensetime.autotest.util.Cmd;
import com.sensetime.autotest.util.CommandUtil;
import com.sensetime.autotest.util.FileUtil;
import com.sensetime.autotest.util.HttpUtil;
import com.sensetime.autotest.util.PowerShell;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.SocketTimeoutException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Semaphore;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import dagger.hilt.android.qualifiers.ApplicationContext;
import lombok.SneakyThrows;

@AndroidEntryPoint
public class EnableTaskService extends IntentService {

    private final String TAG = "EnableTaskService";

    private Context mContext;

    private final ExecutorService executor = ThreadPool.Executor;

    private final Handler mainActivityHandler = MainActivity.getMainActivityHandler();

    private Task task;

    List<String[]> gtList = new LinkedList<>();

    private final List<String> readyVideo = new LinkedList<>();

    int num = 0;

    int total = 0;

    int process = 0;

    @Inject
    HttpUtil httpUtil;

    @Inject
    CommandUtil cu;

    WebSocketService webSocketService = WebSocketService.instance;

    private DeviceMessage<Map<String, Object>> resMsg = new DeviceMessage<>();

    private Map<String, Object> respMap = new HashMap<>(1);

    private Semaphore taskSemaphore = new Semaphore(0);

    private Semaphore downloadSemaphore = new Semaphore(5);

    public EnableTaskService() {
        super("EnableTaskService");
        resMsg.setData(respMap);
    }

    public void init(Task task) {
        CountDownLatch prepareTask = new CountDownLatch(3);
        //sdk 准备
        LogUtils.i("Start SDK preparation");
        httpUtil.downloadFile(mContext, prepareTask, task.getSdkId(), "sdk", task.getSdkRootPath());
        LogUtils.i("SDK preparation is complete");
        //gt 准备
        LogUtils.i("Start GT preparation");
        httpUtil.downloadFile(mContext, prepareTask, task.getGtId(), "gt");
        LogUtils.i("Gt preparation is complete");
        //获取已经上传到测试平台的log
        LogUtils.i("Start Log preparation");
        httpUtil.downloadFile(mContext, prepareTask, task.getId(), "log");
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
            prepareGtList();
            sendtoHandler(1, JSONObject.toJSONString(task), mainActivityHandler);
            //程序运行
            runTask();
        } else {
            LogUtils.i("sdk或GT没准备好，不能执行任务");
            resMsg.setCode(1);
            respMap.put("id", task.getId());
            respMap.put("info", "SDK和GT准备出错");
            respMap.put("status", 7);
            sendServer();
        }

    }

    public void prepareGtList() {
        String gtName;
        gtName = task.getGtId() + ".csv";
        try (InputStreamReader isr = new InputStreamReader(new FileInputStream(mContext.getFilesDir() + "/Gt/" + gtName));){
            BufferedReader br = new BufferedReader(isr);
            String line;
            while ((line = br.readLine()) != null) {
                String[] strings = line.split(",");
                strings[0] = strings[0].replaceAll("\uFEFF", "");
                gtList.add(strings);
                total++;
            }
            LogUtils.i("共检测到" + total + "条数据");
        } catch (Exception e) {
            LogUtils.e(e.getMessage());
        }
    }

    @SneakyThrows
    public void runTask() {
        //创建以任务名称创建log保存文件夹
        File dir = new File(mContext.getFilesDir() + "/Log/" + task.getId());
        if (!dir.exists()) {
            dir.mkdir();
        }
        //蔚来项目专用下载器
        executor.execute(() -> {
            //下载计数器
            int downloadCount = 0;
            for (int j = 0; j <= gtList.size() / 100; j++) {
                try {
                    downloadSemaphore.acquire();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                String path;
                String name = "list" + j;
                Log.i(TAG, "视频地址是" + name);
                CountDownLatch countDownLatch = new CountDownLatch(100);
                try (FileWriter fw = new FileWriter(mContext.getFilesDir() + File.separator + "Video" + File.separator + "list" + (downloadCount / 100));
                     BufferedWriter out = new BufferedWriter(fw)){
                    for (int i = 0; i < 100; i++) {
                        try {
                            path = gtList.get(downloadCount++)[0];
                            httpUtil.downloadFile(mContext, countDownLatch, path, "video");
                            out.write(mContext.getFilesDir() + File.separator + "Video" + File.separator + path.replaceAll("/", "^") + "\n");
                        } catch (IndexOutOfBoundsException e) {
                            countDownLatch.countDown();
                            Log.i(TAG, "文件全部读写完成");
                        }
                    }
                    out.flush();
                    out.close();
                    fw.close();
                    Log.i(TAG, "下载视频文件完成 " + j);
                } catch (SocketTimeoutException e) {
                    Log.e(TAG, "下载视频连接断开",e );
                } catch (IOException e) {
                    Log.i(TAG, "log文件写入失败");
                    e.printStackTrace();
                }
                try {
                    countDownLatch.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                //开始task
                taskSemaphore.release();
                readyVideo.add(name);
            }
//            String[] strings = {"finish"};
            readyVideo.add("finish");
            taskSemaphore.release();
        });
        //开始循环跑任务
        while (true) {
            try {
                taskSemaphore.acquire();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (!readyVideo.isEmpty()) {
                if (readyVideo.get(0).equals("finish")) {
                    LogUtils.i("任务运行完成");
                    resMsg.setCode(1);
                    respMap.put("status", 6);
                    respMap.put("id", task.getId());
                    for (int i = 0; i < 3; i++) {
                        sendServer();
                        sleep();
                    }
                    LogUtils.i("finish");
                    WebSocketService.isRunning = false;
                    break;
                }
                File Logfile = new File(mContext.getFilesDir() + "/Log/" + task.getId() + "/" + readyVideo.get(0).replaceAll("/", "^").replaceAll("\\.[a-zA-z0-9]+$", ".log"));
                if (Logfile.exists()) {
                    Cmd.executes("cd " + mContext.getFilesDir() + "/Video",
                            "rm " + readyVideo.get(0).replaceAll("/", "^"));
                    readyVideo.remove(0);
                    continue;
                }
                Log.i("INFO", "正在执行，当前执行视频" + readyVideo.get(0));
                //拼接字符命令
//                String cmd = MessageFormat.format(task.getCmd(), mContext.getFilesDir() + "/Video/" + readyVideo.get(0).replaceAll("/", "^"), 30,
//                        mContext.getFilesDir() + "/Log/" + task.getId() + "/" + readyVideo.get(0).replaceAll("/", "^").replaceAll("\\.[a-zA-z0-9]+$", ".log"));
                String cmd = cu.createCommand(task,readyVideo);
                //执行命令
                Cmd.executes("cd /data/local/tmp/AutoTest/" + task.getSdkRootPath(),
                        "source env.sh",
                        "./" + task.getSdkRunPath() + File.separator + task.getRunFunc() + cmd);
                splitLog(Logfile);
                removePic(mContext.getFilesDir() + "/Video/"+readyVideo.get(0));
                readyVideo.remove(0);
                downloadSemaphore.release();
                num = num + 100;
                if ((num * 100 / total) > process) {
                    process = num * 100 / total;
                    sendtoHandler(2, process, mainActivityHandler);
                    resMsg.setCode(1);
                    respMap.put("status", 2);
                    respMap.put("id", task.getId());
                    respMap.put("process", process);
                    sendServer();
                    LogUtils.d("taskProcess", "任务: " + task.getTaskName() + "， 进度更新为：" + process);
                }
            }
            Log.i(TAG, "可运行任务余量"+taskSemaphore.availablePermits());
            Log.i(TAG, "下载任务释放量"+downloadSemaphore.availablePermits());
        }
    }

    private void removePic(String s) {
        try (BufferedReader br = new BufferedReader(new FileReader(s))){
            String line ;
            while ((line=br.readLine())!=null){
                Cmd.executes("cd " + mContext.getFilesDir() + "/Video",
                                "rm " + line);
            }
        } catch (FileNotFoundException e) {
            Log.e(TAG, "找不到文件");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @SuppressLint("SdCardPath")
    private void splitLog(File logfile) {
        Log.i(TAG, "正准备分析的log:" + logfile);
        FileWriter fw ;
        try (BufferedReader br = new BufferedReader(new FileReader(logfile))){
            String line;
            String fileName = null;
            BufferedWriter writeFile = null;
            fw = null;
            while ((line = br.readLine()) != null) {
                if (line.contains("current image")) {
                    if (writeFile != null) {
                        writeFile.flush();
                        writeFile.close();
                        fw.close();
                        httpUtil.fileUpload(task.getId(),fileName.replaceAll("\\.[a-zA-z0-9]+$", ".log"));
                    }
                    fileName = mContext.getFilesDir() + "/Log/" + task.getId() + File.separator +line.split("/data/user/0/com.sensetime.autotest/files/Video/")[1];
                    Log.i(TAG, "生成的log地址" +  fileName);
                    fw = new FileWriter(fileName.replaceAll("\\.[a-zA-z0-9]+$", ".log"));
                    writeFile = new BufferedWriter(fw);
                }
                if (fileName == null) {
                    continue;
                }
                writeFile.write(line + "\n");
            }
        } catch (FileNotFoundException e) {
            Log.e(TAG, "分解文件失败，文件不存在");
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            Log.e(TAG, "splitLog: e", e);
        }
    }

    private void sendtoHandler(int what, Object o, Handler mainActivityHandler) {
        Message msg = new Message();
        Bundle bundle = new Bundle();
        switch (what) {
            case 1:
                msg.what = what;
                bundle.putString("task", JSONObject.toJSONString(this.task));
                msg.setData(bundle);
                break;
            case 2:
                msg.what = 2;
                bundle.putInt("process", process);
                msg.setData(bundle);
                break;
        }
        mainActivityHandler.sendMessage(msg);
    }

    private void sendServer() {
        webSocketService.sendMsg(JSON.toJSONString(resMsg));
    }

    static void sleep() {
        try {
            System.gc();
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        assert intent != null;
        JSONObject task = JSONObject.parseObject(intent.getExtras().getString("task"));
        mContext = getBaseContext();
        this.task = JSON.toJavaObject(task, Task.class);
        init(this.task);
    }

    @Override
    public void onDestroy() {
        LogUtils.e("执行器运行完毕，开始执行销毁");
        super.onDestroy();
    }
}