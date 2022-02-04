package com.sensetime.autotest.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import androidx.annotation.Nullable;
import com.sensetime.autotest.entity.Task;
import com.sensetime.autotest.server.WebSocketServer;
import com.sensetime.autotest.server.NfsServer;
import com.sensetime.autotest.util.PowerShell;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;

public class EnableTaskService extends Service {

    private Context mcontext;



    List<String> gtList = new LinkedList<String>();

    private final List<String> readyVideo = new LinkedList<String>();

    final transient Object lock = new Object();

    int gtNum = 0;

    private WebSocketServer webSocketServer;

    public EnableTaskService(Context mcontext) {
        this.mcontext = mcontext;
    }

    public String init(Context context, Task task, WebSocketServer webSocketServer) {
        this.webSocketServer = webSocketServer;
        System.out.println("进入SDK准备 ");
        //sdk 准备
        NfsServer.getFile(context,task.getSdkPath(),"sdk");
//        gt 准备
        NfsServer.getFile(context, task.getGtPath(), "gt");
//
//        //分析生成GT列表
        prepareGtList(context, task);
//        //程序运行
        runTask(context, task);
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

        String[] sdk = task.getSdkPath().split("/");
        String sdkName = sdk[sdk.length - 1].split("\\.")[0];
        int total = 0;

        new Thread(new Runnable() {
            @Override
            public void run() {
                for (String path : gtList) {
                    try {
                        while (readyVideo.size() > 5) {
                            Thread.sleep(10000);
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if (readyVideo.size() <= 5) {
                        File Logfile = new File(context.getFilesDir() + "/Log/" + path.replaceAll("/", "^").replaceAll("\\.[a-zA-z0-9]+$", ".log"));
                        if (Logfile.exists()) {
                            System.out.println("log存在，视频不下载");
                        } else {
                            NfsServer.getFile(context, path, "video");
                            readyVideo.add(path);
                            System.out.println(readyVideo.get(0));
                        }
                    }
                }
                readyVideo.add("finish");
            }
        }).start();

    while (true){

            System.out.println("等待任务执行");
            if (!readyVideo.isEmpty()) {
                if (readyVideo.get(0).equals("finish")) {
//                PowerShell.cmd("cd "+context.getFilesDir()+"/Log",
//                        "tar -cvf "+task.getTaskName()+".tar *"
////                        "rm *.log");
//                );
//                NfsServer.uploadFile(context.getFilesDir()+"/Log/"+task.getTaskName()+".tar");
////                if(webSocketServer.getState()){
////                    webSocketServer.reconnect();
////                }
////                try {
////                    task.setStatus(1);
////                    webSocketServer.reconnect();
////                    Thread.sleep(1000);
////                } catch (InterruptedException e) {
////                    e.printStackTrace();
////                }
//                webSocketServer.send(task.getTaskCode()+"|done");
                    webSocketServer.send("执行任务完成");
                    break;
                }
                System.out.println("进入执行任务");
                File Logfile = new File(context.getFilesDir() + "/Log/" + readyVideo.get(0).replaceAll("/", "^").replaceAll("\\.[a-zA-z0-9]+$", ".log"));
                if (Logfile.exists()) {
                    System.out.println("log存在，下一任务");
                    readyVideo.remove(0);
                    continue;
                }

//                System.out.println("./bin/" + task.getFunc() + " " + context.getFilesDir() + "/Video/" + readyVideo.get(0).split("/")[readyVideo.get(0).split("/").length - 1] + " 30 ./ | tee " + context.getFilesDir() + "/Log/" + readyVideo.get(0).replaceAll("/", "^").replaceAll("\\.[a-zA-z0-9]+$", ".log") + " 2>&1");
                PowerShell.cmd("cd /data/local/tmp/AutoTest/"+task.getTaskName(),
                        "pwd",
//                    "export ADSP_LIBRARY_PATH=\"./lib/snpe_1.43;/system/lib/rfsa/adsp;/system/vendor/lib/rfsa/adsp;/dsp\"",
//                    "export LD_LIBRARY_PATH=./lib:./samples_for_test/bin/opencv:./lib/snpe_1.43:$LD_LIBRARY_PATH",
                        "source profile",
                        "./bin/" + task.getFunc() + " 0 " + context.getFilesDir() + "/Video/" + readyVideo.get(0).replaceAll("/","_")+ " 30 ./face.db 30 | tee " + context.getFilesDir() + "/Log/" + readyVideo.get(0).replaceAll("/", "^").replaceAll("\\.[a-zA-z0-9]+$", ".log") + " 2>&1");
                Log.i("INFO", "正在执行，当前进度" + readyVideo.get(0));
                PowerShell.cmd("cd " + context.getFilesDir() + "/Video",
                        "rm " + readyVideo.get(0).replaceAll("/","_"));
                readyVideo.remove(0);
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}