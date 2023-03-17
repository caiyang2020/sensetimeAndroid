package com.sensetime.autotest.entity;

import android.content.Context;
import android.util.Log;

import com.apkfuns.logutils.LogUtils;
import com.sensetime.autotest.config.ThreadPool;
import com.sensetime.autotest.util.Cmd;

import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.components.ApplicationComponent;
import dagger.hilt.android.qualifiers.ActivityContext;
import dagger.hilt.android.qualifiers.ApplicationContext;

@Module
@InstallIn(ApplicationComponent.class)
public class AndroidSdkBase {

    Context mContext ;

//    private final String SdkDir = mContext.getFilesDir() + File.separator + "Sdk";
//
//    private final String gtDir = mContext.getFilesDir() + File.separator + "Gt";
//
//    private final String logDir = mContext.getFilesDir() + File.separator + "Log";
//
//    private final String videoDir = mContext.getFilesDir() + File.separator + "Video";
//
//    private final String auto = "/data/local/tmp/AutoTest";


    public AndroidSdkBase(@ApplicationContext Context mContext) {
        this.mContext = mContext;
    }

    @Provides
    @Singleton
    public AndroidSdkBase getAndroidSdkBase(){
        return new AndroidSdkBase(@ActivityContext Context);
    }

    public void init() {
        

        String SdkDir = mContext.getFilesDir() + File.separator + "Sdk";
         String gtDir = mContext.getFilesDir() + File.separator + "Gt";

         String logDir = mContext.getFilesDir() + File.separator + "Log";

         String videoDir = mContext.getFilesDir() + File.separator + "Video";

         String auto = "/data/local/tmp/AutoTest";
        ThreadPool.Executor.execute(() -> {
            //启动时先删除之前的log
            File temp = new File(SdkDir);
            temp.delete();
            temp = new File(gtDir);
            temp.delete();
            temp = new File(logDir);
            temp.delete();
            temp = new File(videoDir);
            temp.delete();
            Cmd.execute("rm " + auto);
            try {
                Process mkdirProcess = Runtime.getRuntime().exec("su");
                DataOutputStream dataOutputStream = new DataOutputStream(mkdirProcess.getOutputStream());
                Log.i("info", "程序进入初始化");
                Log.i("info", "创建SDK文件夹");
                dataOutputStream.writeBytes("mkdir " + SdkDir + "\n");
                dataOutputStream.writeBytes("chmod 777 " + SdkDir + "\n");
                Log.i("info", "创建Gt文件夹");
                dataOutputStream.writeBytes("mkdir " + gtDir + "\n");
                dataOutputStream.writeBytes("chmod 777 " + gtDir + "\n");
                Log.i("info", "创建Log文件夹");
                dataOutputStream.writeBytes("mkdir " + logDir + "\n");
                dataOutputStream.writeBytes("chmod 777 " + logDir + "\n");
                Log.i("info", "创建video文件夹");
                dataOutputStream.writeBytes("mkdir " + videoDir + "\n");
                dataOutputStream.writeBytes("chmod 777 " + videoDir + "\n");
                dataOutputStream.flush();
                dataOutputStream.close();
                mkdirProcess.waitFor();
                mkdirProcess.destroy();
                Log.i("info", "Initialization complete");
            } catch (IOException | InterruptedException e) {
                LogUtils.e("Failed to initialize folder");
                LogUtils.e(e);
                e.printStackTrace();
            }
        });
    }
}
