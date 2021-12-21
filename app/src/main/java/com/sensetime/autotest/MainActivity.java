package com.sensetime.autotest;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
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
import java.net.URI;
import java.util.Arrays;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        requestPermission();
        upgradeRootPermission(getPackageCodePath());
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        init();
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                NfsServer nfsServer = new NfsServer(getFilesDir(),"Testdata");
//                nfsServer.doingdown();
//            }
//        }).start();


        new Thread(new Runnable() {
            @Override
            public void run() {
                WebSocketServer webSocketServer = new WebSocketServer( URI.create("ws://192.168.211.103:9000/ArmTest/1"));
                webSocketServer.connect();
//                while (!)
//                System.out.println(!webSocketServer.getState().equals("ok"));
                while (!webSocketServer.getState()) {
                }
                webSocketServer.send("你好");
            }
        }).start();

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

        new Thread(new Runnable() {

            File SdkDir = new File(getDataDir()+"/Sdk");
            File gtDir = new File(getDataDir()+"/Gt");
            File logDir = new File( getDataDir()+"/Log");

            @Override
            public void run() {
                try {
                    Process mkdirProcess = Runtime.getRuntime().exec("su");
                    DataOutputStream dataOutputStream = new DataOutputStream(mkdirProcess.getOutputStream());
                    Log.i("info", "程序进入初始化");
                    if (!SdkDir.exists()) {
                        Log.i("info", "创建SDK文件夹");
                        dataOutputStream.writeBytes("mkdir " + SdkDir.toString() + "\n");
                    }
                    if (!gtDir.exists()) {
                        Log.i("info", "创建Gt文件夹");
                        dataOutputStream.writeBytes("mkdir " + gtDir.toString() + "\n");
                    }
                    if (!logDir.exists()) {
                        Log.i("info", "创建Log文件夹");
                        dataOutputStream.writeBytes("mkdir " + logDir.toString() + "\n");
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
    }
}