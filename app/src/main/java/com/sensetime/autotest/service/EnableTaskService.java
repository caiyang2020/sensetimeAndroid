package com.sensetime.autotest.service;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.alibaba.fastjson.JSON;
import com.apkfuns.logutils.LogUtils;
import com.sensetime.autotest.entity.DeviceMessage;
import com.sensetime.autotest.entity.Task;
import com.sensetime.autotest.entity.TaskInfo;
import com.sensetime.autotest.server.NfsServer;
import com.sensetime.autotest.util.HttpUtil;
import com.sensetime.autotest.util.ThreadManager;
import com.sensetime.autotest.util.PowerShell;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class EnableTaskService {

    private final Context mContext;

    List<String> gtList = new LinkedList<String>();

    int num = 0;

    private final List<String> readyVideo = new LinkedList<String>();

    int gtNum = 0;

    int process = 0;

//    private final WebSocketServer webSocketServer;

    WebSocketService webSocketService ;

    private final Intent intent = new Intent("com.caisang");

    @RequiresApi(api = Build.VERSION_CODES.M)
    public EnableTaskService(Context context) {
        this.mContext = context;
        webSocketService =  mContext.getSystemService(WebSocketService.class);;
    }


    @RequiresApi(api = Build.VERSION_CODES.M)
    public String init(Task task) {

        //sdk 准备
        LogUtils.i("Start SDK preparation");
        NfsServer.getFile(mContext, task.getSdkPath(), "sdk");
        sleep(5000);
        LogUtils.i("SDK preparation is complete");
        //gt 准备
        LogUtils.i("Start GT preparation");
        NfsServer.getFile(mContext, task.getGtPath(), "gt");
        LogUtils.i("Gt preparation is complete");
        //分析生成GT列表
        prepareGtList(mContext, task);
        //程序运行

        runTask(mContext, task);
        return null;
    }

    public void prepareGtList(Context context, Task task) {

        InputStreamReader isr;
        String gtName;
        String[] gts = task.getGtPath().split("/");
        gtName = gts[gts.length - 1];
        try {
            isr = new InputStreamReader(new FileInputStream(new File(context.getFilesDir() + "/Gt/" + gtName)));
            BufferedReader br = new BufferedReader(isr);
            String line;
            while ((line = br.readLine()) != null) {
                gtList.add(line.split(",")[0]);
                gtNum++;
            }
            System.out.println("共检测到" + gtNum + "条数据");
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println(gtList);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public void runTask(Context context, Task task) {

        HashMap<String, Thread> threadHashMap = new HashMap<>();

        String[] sdk = task.getSdkPath().split("/");
        String sdkName = sdk[sdk.length - 1].split("\\.")[0];

        //创建以任务名称创建log保存文件夹
        File dir = new File(context.getFilesDir() + "/Log/" + task.getTaskName());
        if (!dir.exists()) {
            dir.mkdir();
        }
        int total = gtNum;

        new Thread(new Runnable() {
            @Override
            public void run() {
                threadHashMap.put(task.getTaskName() + "downloadthread", Thread.currentThread());
                for (String path : gtList) {
                    try {
                        while (readyVideo.size() > 5) {
                            Thread.sleep(10000);
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    File Logfile = new File(context.getFilesDir() + "/Log/" + task.getTaskName() + "/" + path.replaceAll("/", "^").replaceAll("\\.[a-zA-z0-9]+$", ".log"));
                    if (Logfile.exists()) {
//                        System.out.println("log存在，视频不下载");
                        num++;
                    } else {
                        path = path.replace("\uFEFF", "");
                        NfsServer.getFile(context, path, "video");
                        readyVideo.add(path);
                        System.out.println(readyVideo.get(0));
                    }
                }
                readyVideo.add("finish");
            }
        }).start();

//        Boolean status = true;
        while (true) {
            threadHashMap.put(task.getTaskName() + "runthread", Thread.currentThread());
            ThreadManager.setTaskList(threadHashMap);
//        System.out.println("等待任务执行");
            if (!readyVideo.isEmpty()) {
                if (readyVideo.get(0).equals("finish")) {
                    System.out.println("任务运行完成");
//                    sleep(3000);

//                    try {
//                        String[] cmds = {"sh", "-c ", "su; cd " + context.getFilesDir() + "/Log; tar -zcvf " + task.getTaskName() + ".tar.gz ./" + task.getTaskName()};
//                        Process process = Runtime.getRuntime().exec(cmds);
//
//                        process.waitFor();
//                    } catch (InterruptedException | IOException e) {
//                        e.printStackTrace();
//                    }
//                    NfsServer.uploadFile(context.getFilesDir() + "/Log/" + task.getTaskName() + ".tar.gz", task.getTaskName());
//                    webSocketServer.send("执行任务完成");

                    DeviceMessage<String> deviceMessage = new DeviceMessage<String>();
                    deviceMessage.setCode(1);
                    deviceMessage.setData(task.getTaskCode()+"");
                    System.out.println(JSON.toJSONString(deviceMessage));
                    HttpUtil.get("http://10.151.5.190:9001/androidDone/"+task.getTaskCode());
                    LogUtils.i("finish");
//                    status=false;
                    break;
                }
//                System.out.println("进入执行任务");
                File Logfile = new File(context.getFilesDir() + "/Log/" + task.getTaskName() + "/" + readyVideo.get(0).replaceAll("/", "^").replaceAll("\\.[a-zA-z0-9]+$", ".log"));
                if (Logfile.exists()) {
//                    System.out.println("log存在，下一任务");
                    readyVideo.remove(0);
                    continue;
                }
                LogUtils.i("cmd:" + "./bin/" + task.getFunc() + " " + context.getFilesDir() + "/Video/" + readyVideo.get(0).replaceAll("/", "_") + " 30  | tee " + context.getFilesDir() + "/Log/" + task.getTaskName() + "/" + readyVideo.get(0).replaceAll("/", "^").replaceAll("\\.[a-zA-z0-9]+$", ".log") + " 2>&1");

                //标准平台SDK调用方式
//                PowerShell.cmd( context,"cd /data/local/tmp/AutoTest/"+task.getSdkPath().split("/")[task.getSdkPath().split("/").length-1].replaceAll("\\.[a-zA-z0-9]+$","")+"/release/samples",
//                        "pwd",
//                        "source env.sh",
//                        "./samples_CAPI/bin/" + task.getFunc() + "  " + context.getFilesDir() + "/Video/" + readyVideo.get(0).replaceAll("/","^")+" 30  | tee " + context.getFilesDir() + "/Log/"+task.getTaskName()+"/"+ readyVideo.get(0).replaceAll("/", "^").replaceAll("\\.[a-zA-z0-9]+$", ".log") + " 2>&1");
//                V362

                PowerShell.cmd( context,"cd /data/local/tmp/AutoTest/"+task.getSdkPath().split("/")[task.getSdkPath().split("/").length-1].replaceAll("\\.[a-zA-z0-9]+$","")+"/release/samples",
                        "pwd",
                        "source env.sh",
                        "./samples_CAPI/bin/" + task.getFunc() + "  " + context.getFilesDir() + "/Video/" + readyVideo.get(0).replaceAll("/","^")+" 30 > " + context.getFilesDir() + "/Log/"+task.getTaskName()+"/"+ readyVideo.get(0).replaceAll("/", "^").replaceAll("\\.[a-zA-z0-9]+$", ".log") + " 2>&1");
//                PowerShell.cmd(context, "cd /data/local/tmp/AutoTest/" + task.getSdkPath().split("/")[task.getSdkPath().split("/").length - 1].replaceAll("\\.[a-zA-z0-9]+$", "") + "/release/samples",
//                        "pwd",
//                        "source env.sh",
//                        "./samples_CAPI/bin/" + task.getFunc() + " 1 " + context.getFilesDir() + "/Video/" + readyVideo.get(0).replaceAll("/", "^") + " 30 ./face.db 1 | tee " + context.getFilesDir() + "/Log/" + task.getTaskName() + "/" + readyVideo.get(0).replaceAll("/", "^").replaceAll("\\.[a-zA-z0-9]+$", ".log") + " 2>&1");
//                PowerShell.cmd(context, "cd /data/local/tmp/AutoTest/" + task.getSdkPath().split("/")[task.getSdkPath().split("/").length - 1].replaceAll("\\.[a-zA-z0-9]+$", ""),
//                        "pwd",
//                        "source profile",
//                        "./bin/" + task.getFunc() + "  " + context.getFilesDir() + "/Video/" + readyVideo.get(0).replaceAll("/", "^") + " 30  | tee " + context.getFilesDir() + "/Log/" + task.getTaskName() + "/" + readyVideo.get(0).replaceAll("/", "^").replaceAll("\\.[a-zA-z0-9]+$", ".log") + " 2>&1");

                NfsServer.uploadFile(context.getFilesDir() + "/Log/"+task.getTaskName()+"/"+ readyVideo.get(0).replaceAll("/", "^").replaceAll("\\.[a-zA-z0-9]+$", ".log"),task.getTaskName());
                Log.i("INFO", "正在执行，当前进度" + process);
                PowerShell.cmd("cd " + context.getFilesDir() + "/Video",
                        "rm " + readyVideo.get(0).replaceAll("/", "^"));
                readyVideo.remove(0);
                num++;

                if ((num * 100 / gtNum) > process) {
                    process = num * 100 / gtNum;
//                    System.out.println(process);
                    intent.putExtra("process", process);
                    context.sendBroadcast(intent);
                    TaskInfo taskInfo  = new TaskInfo();
                    taskInfo.setTaskCode(task.getTaskCode());
                    taskInfo.setData(process+"");
                    System.out.println(JSON.toJSONString(taskInfo));
                    HttpUtil.post("http://10.151.5.190:9001/andtoidrate",taskInfo);
                }
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    static void sleep(long time) {
        try {
//            System.gc();
            Thread.sleep(time);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}