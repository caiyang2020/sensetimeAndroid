package com.sensetime.autotest.util;


import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.apkfuns.logutils.LogUtils;
import com.sensetime.autotest.MainActivity;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class Cmd {

    private final static String TAG = "CMD";

    static Process process;

    static {
        try {
            process = Runtime.getRuntime().exec("su");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void execute(String command){
        try {
            command = command+" 2>&1\n";
            Process process = Runtime.getRuntime().exec("su");
            DataOutputStream dos = new DataOutputStream(process.getOutputStream());
            dos.writeBytes(command);
            dos.flush();
            dos.close();
            BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
            String line;
            while ((line = br.readLine())!=null){
                Log.i(TAG, "execute: "+line);
            }
            process.waitFor();
            process.destroy();
        } catch (InterruptedException|IOException e) {
            LogUtils.e("执行出现问题请检查");
            LogUtils.e(e);
        }
    }

    public static void executes(String... cmds) {
//        Handler mainActivityHandler = MainActivity.getMainActivityHandler();
        String successInfo ;
        try {
            Process process = Runtime.getRuntime().exec("su");
            DataOutputStream dos = new DataOutputStream(process.getOutputStream());
            dos.writeBytes("cd /data/data/com.sensetime.autotest" + "\n");
            for (String cmd :
                    cmds) {
                dos.writeBytes(cmd + "\n");
            }
            dos.writeBytes("");
            dos.flush();
            dos.close();
//            BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
//            int i = 0;
//            while ((successInfo = br.readLine()) != null) {
//                Log.i("info", successInfo);
////                if (++i%10==0){
////                    Message message = new Message();
////                    message.what=3;
////                    Bundle bundle = new Bundle();
////                    bundle.putString("successInfo",successInfo);
////                    message.setData(bundle);
////                    mainActivityHandler.sendMessage(message);
////                }
//            }
            process.waitFor();
            process.destroy();
            System.gc();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void exec(String... cmds){
        try {
            DataOutputStream dos = new DataOutputStream(process.getOutputStream());
            dos.writeBytes("cd /data/data/com.sensetime.autotest" + "\n");
            for (String cmd : cmds) {
                dos.writeBytes(cmd + "\n");
            }
            dos.flush();
            dos.close();
//            BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
//            String successInfo;
//            while ((successInfo= br.readLine()) != null) {
//                System.out.print(successInfo);
//            }
//            process.waitFor();
        } catch (IOException e) {
            LogUtils.e("执行出现问题请检查");
            LogUtils.e(e);
        }
    }
}
