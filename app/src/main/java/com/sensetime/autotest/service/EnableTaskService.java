package com.sensetime.autotest.service;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import androidx.annotation.Nullable;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.apkfuns.logutils.LogUtils;
import com.sensetime.autotest.entity.DeviceMessage;
import com.sensetime.autotest.entity.Task;
import com.sensetime.autotest.entity.TaskInfo;
import com.sensetime.autotest.server.NfsServer;
import com.sensetime.autotest.util.FileUtil;
import com.sensetime.autotest.util.HttpUtil;
import com.sensetime.autotest.util.ThreadManager;
import com.sensetime.autotest.util.PowerShell;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.sql.SQLOutput;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import lombok.SneakyThrows;

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
     * name Used to name the worker thread, important only for debugging.
     */
    public EnableTaskService() {
        super("EnableTaskService");
    }

    public void init(Task task) {
        CountDownLatch prepareTask = new CountDownLatch(2);
        //sdk 准备
        LogUtils.i("Start SDK preparation");
        HttpUtil.downloadFile(mContext,prepareTask, task.getSdkId(), "sdk");
//        prepareTask.countDown();
        LogUtils.i("SDK preparation is complete");
        //gt 准备
        LogUtils.i("Start GT preparation");
        HttpUtil.downloadFile(mContext, prepareTask,task.getGtId(), "gt");
        LogUtils.i("Gt preparation is complete");
        try {
            prepareTask.await(1, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if (FileUtil.checkSdk()&&FileUtil.checkGt()){
            //分析生成GT列表
            prepareGtList(mContext, task);
            //程序运行
            runTask(mContext, task);
        }else {
            LogUtils.i("sdk或GT没准备好，不能执行任务");
        }
    }

    public void prepareGtList(Context context, Task task) {

        InputStreamReader isr;
        String gtName;
        gtName = String.valueOf(task.getGtId())+"csv";
        try {
            isr = new InputStreamReader(new FileInputStream(new File(context.getFilesDir() + "/Gt/" + gtName)));
            BufferedReader br = new BufferedReader(isr);
            String line;
            while ((line = br.readLine()) != null) {
                String[] strings = line.split(",");
                strings[0] = strings[0].replace("\uFEFF","");
                gtList.add(strings);
                gtNum++;
            }
            System.out.println("共检测到" + gtNum + "条数据");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SneakyThrows
    public void runTask(Context context, Task task)  {

        final Semaphore taskSemaphore = new Semaphore(0);

        //创建以任务名称创建log保存文件夹
         File dir = new File(context.getFilesDir() + "/Log/" + task.getTaskName());
        if (!dir.exists()) {
            dir.mkdir();
        }
        int total = gtNum;

        new Thread(new Runnable() {
            final Semaphore semaphore = new Semaphore(1);
            @Override
            public void run() {
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
                        String path = path1[0];
                        try {
                            semaphore.acquire();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        HttpUtil.downloadFile(mContext, semaphore,path, "video");
                        readyVideo.add(path1);
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
                    System.out.println("任务运行完成");
//                    try {
//                        String[] cmds = {"sh", "-c ", "su; cd " + context.getFilesDir() + "/Log; tar -zcvf " + task.getTaskName() + ".tar.gz ./" + task.getTaskName()};
//                        Process process = Runtime.getRuntime().exec(cmds);
//
//                        process.waitFor();
//                    } catch (InterruptedException | IOException e) {
//                        e.printStackTrace();
//                    }
//                    DeviceMessage<String> deviceMessage = new DeviceMessage<String>();
//                    deviceMessage.setCode(1);
//                    deviceMessage.setData(task.getTaskCode()+"");
//                    System.out.println(JSON.toJSONString(deviceMessage));
//                    HttpUtil.get("http://10.151.4.123:9001/androidDone/"+task.getTaskCode());
                    LogUtils.i("finish");
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
                String cmd = MessageFormat.format(task.getCmd(),readyVideo.get(0)[0],30,
                        context.getFilesDir() + "/Log/"+task.getTaskName()+"/"+ readyVideo.get(0)[0].replaceAll("/", "^").replaceAll("\\.[a-zA-z0-9]+$", ".log\""));
                PowerShell.cmd( context,"cd /data/local/tmp/AutoTest/"+task.getSdkRootPath(),
                        "pwd",
                        "source env.sh",
                        "./"+task.getSdkRunPath()+File.separator + task.getRunFunc()+ cmd);
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
//                    taskInfo.setTaskCode(task.getTaskCode());
//                    taskInfo.setData(process+"");
//                    System.out.println(JSON.toJSONString(taskInfo));

//                    LogUtils.d("taskProcess","任务: "+task.getTaskCode()+"， 进度更新为："+process);

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
        JSONObject task = JSONObject.parseObject(intent.getExtras().getString("task"));
        mContext = getBaseContext();
        System.out.println(task);
        Task task1=JSON.toJavaObject(task,Task.class);
        init(task1);


    }
}