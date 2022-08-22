package com.sensetime.autotest.service;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import androidx.annotation.Nullable;
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
import java.io.InputStreamReader;
import java.sql.SQLOutput;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class EnableTaskService extends IntentService {

    private  Context mContext;

    List<String[]> gtList = new LinkedList<>();

    private final List<String[]> readyVideo = new LinkedList<>();

    int num = 0;

    int gtNum = 0;

    int process = 0;


    private final Intent intent = new Intent("com.caisang");

    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     *
     * @param name Used to name the worker thread, important only for debugging.
     */
    public EnableTaskService() {
        super("EnableTaskService");
    }

//    public EnableTaskService(Context context) {
//        this.mContext = context;
//        webSocketService =  mContext.getSystemService(WebSocketService.class);;
//    }


//    @RequiresApi(api = Build.VERSION_CODES.M)
    public String init(Task task) {
        CountDownLatch prepareTask = new CountDownLatch(2);
        //sdk 准备
        LogUtils.i("Start SDK preparation");
        HttpUtil.downloadFile(mContext,prepareTask, task.getSdkPath(), "sdk");
//        prepareTask.countDown();
        LogUtils.i("SDK preparation is complete");
        //gt 准备
        LogUtils.i("Start GT preparation");
        HttpUtil.downloadFile(mContext, prepareTask,task.getGtPath(), "gt");
        LogUtils.i("Gt preparation is complete");
        try {
            prepareTask.await(1, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
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
                String[] strings = new String[4];
                strings[0]=line.split(",")[0];
                strings[1]=line.split(",")[1];
                strings[2]=line.split(",")[2];
                strings[3]=line.split(",")[3];
                gtList.add(strings);
                gtNum++;
            }
            System.out.println("共检测到" + gtNum + "条数据");
        } catch (Exception e) {
            e.printStackTrace();
        }
//        System.out.println(gtList);
    }

//    @RequiresApi(api = Build.VERSION_CODES.M)
    public void runTask(Context context, Task task)  {

        CountDownLatch countDownLatch = new CountDownLatch(1);

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
//                threadHashMap.put(task.getTaskName() + "downloadthread", Thread.currentThread());
                for (String[] path1 : gtList) {
                    try {
                        while (readyVideo.size() > 5) {
                            Thread.sleep(10000);
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    File Logfile = new File(context.getFilesDir() + "/Log/" + task.getTaskName() + "/" + path1[0].replaceAll("/", "^").replaceAll("\\.[a-zA-z0-9]+$", ".log"));
                    if (Logfile.exists()) {
                        num++;
                    } else {
                        String path = path1[0].replace("\uFEFF", "");
//                        NfsServer.getFile(context, path, "video");
                        CountDownLatch latch = new CountDownLatch(1);
                        HttpUtil.downloadFile(mContext, latch,path, "video");
                        try {
                            latch.await(30,TimeUnit.SECONDS);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        readyVideo.add(path1);
//                        System.out.println(readyVideo.get(0));

                    }
                }
                String[] strings = {"finish"};
                readyVideo.add(strings);
            }
        }).start();

        try {
            countDownLatch.await(30,TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


        while (true) {
//            threadHashMap.put(task.getTaskName() + "runthread", Thread.currentThread());
//            ThreadManager.setTaskList(threadHashMap);
            if (!readyVideo.isEmpty()) {
                if (readyVideo.get(0)[0].equals("finish")) {
                    System.out.println("任务运行完成");
//                    try {
//                        String[] cmds = {"sh", "-c ", "su; cd " + context.getFilesDir() + "/Log; tar -zcvf " + task.getTaskName() + ".tar.gz ./" + task.getTaskName()};
//                        Process process = Runtime.getRuntime().exec(cmds);
//
//                        process.waitFor();
//                    } catch (InterruptedException | IOException e) {
//                        e.printStackTrace();
//                    }
                    DeviceMessage<String> deviceMessage = new DeviceMessage<String>();
                    deviceMessage.setCode(1);
                    deviceMessage.setData(task.getTaskCode()+"");
                    System.out.println(JSON.toJSONString(deviceMessage));
                    HttpUtil.get("http://10.151.4.123:9001/androidDone/"+task.getTaskCode());
                    LogUtils.i("finish");
                    break;
                }
                File Logfile = new File(context.getFilesDir() + "/Log/" + task.getTaskName() + "/" + readyVideo.get(0)[0].replaceAll("/", "^").replaceAll("\\.[a-zA-z0-9]+$", ".log"));
                if (Logfile.exists()) {
                    readyVideo.remove(0);
                    continue;
                }
//                LogUtils.i("cmd:" + "./bin/" + task.getFunc() + " " + context.getFilesDir() + "/Video/" + readyVideo.get(0).replaceAll("/", "_") + " 30  | tee " + context.getFilesDir() + "/Log/" + task.getTaskName() + "/" + readyVideo.get(0).replaceAll("/", "^").replaceAll("\\.[a-zA-z0-9]+$", ".log") + " 2>&1");
                Log.i("INFO", "正在执行，当前执行视频" + readyVideo.get(0));
                //标准平台SDK调用方式
//                PowerShell.cmd( context,"cd /data/local/tmp/AutoTest/"+task.getSdkPath().split("/")[task.getSdkPath().split("/").length-1].replaceAll("\\.[a-zA-z0-9]+$","")+"/release/samples",
//                        "pwd",
//                        "source env.sh",
//                        "./samples_CAPI/bin/" + task.getFunc() + "  " + context.getFilesDir() + "/Video/" + readyVideo.get(0).replaceAll("/","^")+" 30  | tee " + context.getFilesDir() + "/Log/"+task.getTaskName()+"/"+ readyVideo.get(0).replaceAll("/", "^").replaceAll("\\.[a-zA-z0-9]+$", ".log") + " 2>&1");
//                V362

                PowerShell.cmd( context,"cd /data/local/tmp/AutoTest/"+task.getSdkPath().split("/")[task.getSdkPath().split("/").length-1].replaceAll("\\.[a-zA-z0-9]+$","")+"/release/samples",
                        "pwd",
                        "source env.sh",
                        "./samples_CAPI/bin/" + task.getFunc() + "  \"" + context.getFilesDir() + "/Video/" + readyVideo.get(0)[0].replaceAll("/","^")+"\" 30 "+readyVideo.get(0)[1]+" "+readyVideo.get(0)[2]+" "+readyVideo.get(0)[3]+" > \"" + context.getFilesDir() + "/Log/"+task.getTaskName()+"/"+ readyVideo.get(0)[0].replaceAll("/", "^").replaceAll("\\.[a-zA-z0-9]+$", ".log\"") + " 2>&1");
                System.out.println("./samples_CAPI/bin/" + task.getFunc() + "  \"" + context.getFilesDir() + "/Video/" + readyVideo.get(0)[0].replaceAll("/","^")+"\" 30 "+readyVideo.get(0)[1]+" "+readyVideo.get(0)[2]+" "+readyVideo.get(0)[3]+" > \"" + context.getFilesDir() + "/Log/"+task.getTaskName()+"/"+ readyVideo.get(0)[0].replaceAll("/", "^").replaceAll("\\.[a-zA-z0-9]+$", ".log\"") + " 2>&1");
//                PowerShell.cmd(context, "cd /data/local/tmp/AutoTest/" + task.getSdkPath().split("/")[task.getSdkPath().split("/").length - 1].replaceAll("\\.[a-zA-z0-9]+$", "") + "/release/samples",
//                        "pwd",
//                        "source env.sh",
//                        "./samples_CAPI/bin/" + task.getFunc() + " 1 \"" + context.getFilesDir() + "/Video/" + readyVideo.get(0)[0].replaceAll("/", "^") + "\" 30 ./face.db 1 > \"" + context.getFilesDir() + "/Log/" + task.getTaskName() + "/" + readyVideo.get(0)[0].replaceAll("/", "^").replaceAll("\\.[a-zA-z0-9]+$", ".log\"") + " 2>&1");
//                System.out.println("./samples_CAPI/bin/" + task.getFunc() + " 0 \"" + context.getFilesDir() + "/Video/" + readyVideo.get(0)[0].replaceAll("/", "^") + "\" 30 ./face.db 1 > \"" + context.getFilesDir() + "/Log/" + task.getTaskName() + "/" + readyVideo.get(0)[0].replaceAll("/", "^").replaceAll("\\.[a-zA-z0-9]+$", ".log\"") + " 2>&1");
                NfsServer.uploadFile(context.getFilesDir() + "/Log/"+task.getTaskName()+"/"+ readyVideo.get(0)[0].replaceAll("/", "^").replaceAll("\\.[a-zA-z0-9]+$", ".log"),task.getTaskName());
                PowerShell.cmd("cd " + context.getFilesDir() + "/Video",
                        "rm " + readyVideo.get(0)[0].replaceAll("/", "^"));
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
//                    System.out.println(JSON.toJSONString(taskInfo));

                    LogUtils.d("taskProcess","任务: "+task.getTaskCode()+"， 进度更新为："+process);

                    HttpUtil.post("http://10.151.4.123:9001/andtoidrate",taskInfo);
                }
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
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

        assert intent != null;
        String task = intent.getExtras().getString("task");
        mContext = getBaseContext();
        Task task1 = JSON.parseObject(task, Task.class);
        init(task1);

    }
}