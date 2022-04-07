package com.sensetime.autotest.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.Parcelable;
import android.util.Log;
import androidx.annotation.Nullable;
import com.apkfuns.logutils.LogUtils;
import com.sensetime.autotest.entity.Task;
import com.sensetime.autotest.server.WebSocketServer;
import com.sensetime.autotest.server.NfsServer;
import com.sensetime.autotest.tools.ThreadManager;
import com.sensetime.autotest.util.PowerShell;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class EnableTaskService  {

    private final Context mContext;

    List<String> gtList = new LinkedList<String>();

    int num =0;

    private final List<String> readyVideo = new LinkedList<String>();

    int gtNum = 0;

    int process=0;

    private final WebSocketServer webSocketServer;

    private final Intent intent = new Intent("com.caisang");

    public EnableTaskService(Context context,WebSocketServer webSocketServer) {
        this.mContext = context;
        this.webSocketServer=webSocketServer;
    }

    public String init(Task task ) {

        //sdk 准备
        LogUtils.i("Start SDK preparation");
        NfsServer.getFile(mContext,task.getSdkPath(),"sdk");
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

    public void runTask(Context context, Task task) {

        HashMap<String,Thread> threadHashMap = new HashMap<>();

        String[] sdk = task.getSdkPath().split("/");
        String sdkName = sdk[sdk.length - 1].split("\\.")[0];

        //创建以任务名称创建log保存文件夹
        File dir = new File(context.getFilesDir() + "/Log/" + task.getTaskName());
        if (!dir.exists()){
            dir.mkdir();
        }
        int total = gtNum;

        new Thread(new Runnable() {
            @Override
            public void run() {
                threadHashMap.put(task.getTaskName()+"downloadthread",Thread.currentThread());
                for (String path : gtList) {
                    try {
                        while (readyVideo.size() > 5) {
                            Thread.sleep(10000);
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    File Logfile = new File(context.getFilesDir() + "/Log/" +task.getTaskName()+"/"+ path.replaceAll("/", "^").replaceAll("\\.[a-zA-z0-9]+$", ".log"));
                    if (Logfile.exists()) {
                        System.out.println("log存在，视频不下载");
                        num++;
                    } else {
                        path = path.replace("\uFEFF","");
                        NfsServer.getFile(context, path, "video");
                        readyVideo.add(path);
                        System.out.println(readyVideo.get(0));
                    }
                }
                readyVideo.add("finish");
            }
        }).start();

    while (true){
        threadHashMap.put(task.getTaskName()+"runthread",Thread.currentThread());
        ThreadManager.setTaskList(threadHashMap);
        System.out.println("等待任务执行");
            if (!readyVideo.isEmpty()) {
                if (readyVideo.get(0).equals("finish")) {
                    sleep(3000);
                PowerShell.cmd("cd "+context.getFilesDir()+"/Log",
                        "tar -zcvf "+task.getTaskName()+".tar.gz ./"+task.getTaskName()
//                        "rm *.log");
                );
                NfsServer.uploadFile(context.getFilesDir()+"/Log/"+task.getTaskName()+".tar.gz");
                    webSocketServer.send("执行任务完成");
                    break;
                }
                System.out.println("进入执行任务");
                File Logfile = new File(context.getFilesDir() + "/Log/" +task.getTaskName()+"/"+ readyVideo.get(0).replaceAll("/", "^").replaceAll("\\.[a-zA-z0-9]+$", ".log"));
                if (Logfile.exists()) {
                    System.out.println("log存在，下一任务");
                    readyVideo.remove(0);
                    continue;
                }
                LogUtils.i("cmd:"+"./bin/" + task.getFunc() + " " + context.getFilesDir() + "/Video/" + readyVideo.get(0).replaceAll("/","_")+" 30  | tee " + context.getFilesDir() + "/Log/"+task.getTaskName()+"/"+ readyVideo.get(0).replaceAll("/", "^").replaceAll("\\.[a-zA-z0-9]+$", ".log") + " 2>&1");

                //标准平台SDK调用方式
//                PowerShell.cmd( context,"cd /data/local/tmp/AutoTest/"+task.getSdkPath().split("/")[task.getSdkPath().split("/").length-1].replaceAll("\\.[a-zA-z0-9]+$","")+"/release/samples",
//                        "pwd",
//                        "source env.sh",
//                        "./samples_CAPI/bin/" + task.getFunc() + "  " + context.getFilesDir() + "/Video/" + readyVideo.get(0).replaceAll("/","^")+" 30  | tee " + context.getFilesDir() + "/Log/"+task.getTaskName()+"/"+ readyVideo.get(0).replaceAll("/", "^").replaceAll("\\.[a-zA-z0-9]+$", ".log") + " 2>&1");
//                V362
                PowerShell.cmd( context,"cd /data/local/tmp/AutoTest/"+task.getSdkPath().split("/")[task.getSdkPath().split("/").length-1].replaceAll("\\.[a-zA-z0-9]+$","")+"/release/samples",
                        "pwd",
                        "source env.sh",
                        "./samples_CAPI/bin/" + task.getFunc() + "  " + context.getFilesDir() + "/Video/" + readyVideo.get(0).replaceAll("/","^")+" 30  | tee " + context.getFilesDir() + "/Log/"+task.getTaskName()+"/"+ readyVideo.get(0).replaceAll("/", "^").replaceAll("\\.[a-zA-z0-9]+$", ".log") + " 2>&1");
//                PowerShell.cmd( context,"cd /data/local/tmp/AutoTest/"+task.getSdkPath().split("/")[task.getSdkPath().split("/").length-1].replaceAll("\\.[a-zA-z0-9]+$",""),
//                        "pwd",
//                        "source profile",
//                        "./bin/" + task.getFunc() + "  " + context.getFilesDir() + "/Video/" + readyVideo.get(0).replaceAll("/","^")+" 30  | tee " + context.getFilesDir() + "/Log/"+task.getTaskName()+"/"+ readyVideo.get(0).replaceAll("/", "^").replaceAll("\\.[a-zA-z0-9]+$", ".log") + " 2>&1");
                Log.i("INFO", "正在执行，当前进度" + readyVideo.get(0));
                PowerShell.cmd("cd " + context.getFilesDir() + "/Video",
                        "rm " + readyVideo.get(0).replaceAll("/","^"));
                readyVideo.remove(0);
                num++;

                if ((num*100/gtNum)>process){
                    process = num*100/gtNum;
                    System.out.println(process);
                    intent.putExtra("process",process);
                    context.sendBroadcast(intent);
                }
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    static void sleep(long time){
        try {
            System.gc();
            Thread.sleep(time);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}