package com.sensetime.autotest;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.icu.text.IDNA;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.widget.Toast;
import com.sensetime.autotest.util.NfsServer;
import com.sensetime.autotest.util.PowerShell;
import com.sensetime.autotest.util.WebSocketServer;

import org.slf4j.Logger;

import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.Properties;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MainActivity extends AppCompatActivity {


    private Context mContext;

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        requestPermission();
        upgradeRootPermission(getPackageCodePath());
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        mContext = getApplication();
        init();


//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                PowerShell powerShell = new PowerShell();
//                powerShell.cmd(new String[]{"pwd"});
//            }
//        }).start();



    }

    public void requestPermission() {

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            Toast.makeText(this, "申请权限", Toast.LENGTH_SHORT).show();

            // 申请 相机 麦克风权限
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,

                    Manifest.permission.READ_EXTERNAL_STORAGE}, 100);
        }
    }

    public static boolean upgradeRootPermission(String pkgCodePath) {
        Process process = null;
        DataOutputStream os = null;
        try {
            String cmd="chmod 777 " + pkgCodePath;
            process = Runtime.getRuntime().exec("su"); //切换到root帐号
            os = new DataOutputStream(process.getOutputStream());
            os.writeBytes(cmd + "\n");
            os.writeBytes("exit\n");
            os.flush();
            process.waitFor();
        } catch (Exception e) {
            return false;
        } finally {
            try {
                if (os != null) {
                    os.close();
                }
                assert process != null;
                process.destroy();
            } catch (Exception ignored) {
            }
        }
        return true;
    }
    @RequiresApi(api = Build.VERSION_CODES.N)
    public void init() {
        WebSocketServer webSocketServer = new WebSocketServer(mContext,URI.create("ws://10.151.4.123:9000/ArmTest/1"));


        new Thread(new Runnable() {

            File SdkDir = new File(getFilesDir()+"/Sdk");
            File gtDir = new File(getFilesDir()+"/Gt");
            File logDir = new File( getFilesDir()+"/Log");
            File videoDir = new File( getFilesDir()+"/Video");

            @Override
            public void run() {
                try {
                    Process mkdirProcess = Runtime.getRuntime().exec("su");
                    DataOutputStream dataOutputStream = new DataOutputStream(mkdirProcess.getOutputStream());
                    Log.i("info", "程序进入初始化");
                    if (!SdkDir.exists()) {
                        Log.i("info", "创建SDK文件夹");
                        dataOutputStream.writeBytes("mkdir " + SdkDir.toString() + "\n");
                        dataOutputStream.writeBytes("chmod 777 " + SdkDir.toString() + "\n");
                    }
                    if (!gtDir.exists()) {
                        Log.i("info", "创建Gt文件夹");
                        dataOutputStream.writeBytes("mkdir " + gtDir.toString() + "\n");
                        dataOutputStream.writeBytes("chmod 777 " + gtDir.toString() + "\n");
                    }
                    if (!logDir.exists()) {
                        Log.i("info", "创建Log文件夹");
                        dataOutputStream.writeBytes("mkdir " + logDir.toString() + "\n");
                        dataOutputStream.writeBytes("chmod 777 " + logDir.toString() + "\n");
                    }
                    if (!videoDir.exists()) {
                        Log.i("info", "创建video文件夹");
                        dataOutputStream.writeBytes("mkdir " + videoDir.toString() + "\n");
                        dataOutputStream.writeBytes("chmod 777 " + videoDir.toString() + "\n");
                    }
                    dataOutputStream.flush();
                    dataOutputStream.close();
                    mkdirProcess.waitFor();
                    mkdirProcess.destroy();
                }catch (IOException | InterruptedException e){
                    e.printStackTrace();
                }
                Log.i("info","初始化程序完成");

            }
        }).start();

        new Thread(new Runnable() {
            @Override
            public void run() {
                Log.i("info","开始连接websocket");
                try {
                    webSocketServer.connectBlocking();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                while (!webSocketServer.getState()) {
                    try {
                        Log.i("info","websocket连接中");
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }
}