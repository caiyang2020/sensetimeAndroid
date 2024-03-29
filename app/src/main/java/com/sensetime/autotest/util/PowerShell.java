package com.sensetime.autotest.util;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.apkfuns.logutils.LogUtils;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class PowerShell {
    Process process;
    DataOutputStream dos;

    public PowerShell() {
        try {
            this.process = Runtime.getRuntime().exec("su");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void cmd(String... cmds) {
        String errorInfo = "";
        String successInfo = "";
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
//            System.out.println(process.waitFor());
//            process.waitFor();
            BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
            while ((successInfo = br.readLine()) != null) {
                Log.i("info", successInfo);
//               if ("Read video finished".equalsIgnoreCase(successInfo)){
//                   break;
//               }
            }
//            InputStream is = process.getErrorStream();
//            System.out.println(is.read());
//            Log.i("error", String.valueOf(is.read()));
//            BufferedReader errorResult = new BufferedReader(new InputStreamReader(process.getErrorStream()));
//            while ((errorInfo = errorResult.readLine()) != null) {
//                Log.i("info",errorInfo);
//            }
            process.waitFor();
            process.destroy();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void cmd(Context context, String... cmds) {
        Intent messageintent = new Intent("com.caisang");
        String errorInfo = "";
        String successInfo = "";
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
//            System.out.println(process.waitFor());
//            process.waitFor();
            BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
            while ((successInfo = br.readLine()) != null) {
                messageintent.putExtra("message", successInfo);
//                Log.i("info",successInfo);
//               if ("Read video finished".equalsIgnoreCase(successInfo)){
//                   break;
//               }
                context.sendBroadcast(messageintent);
            }
//            InputStream is = process.getErrorStream();
//            System.out.println(is.read());
//            LogUtils.e("error", String.valueOf(is.read()));
//            BufferedReader errorResult = new BufferedReader(new InputStreamReader(process.getErrorStream()));
//            while ((errorInfo = errorResult.readLine()) != null) {
////                LogUtils.e("error",errorInfo);
//            }
            process.destroy();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
