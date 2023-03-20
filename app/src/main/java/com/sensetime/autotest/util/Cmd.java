package com.sensetime.autotest.util;


import android.util.Log;

import com.apkfuns.logutils.LogUtils;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class Cmd {

    private final static String TAG = "CMD";

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
        String successInfo = "";
        try {
            Process process = Runtime.getRuntime().exec("su");
            DataOutputStream dos = new DataOutputStream(process.getOutputStream());
//            dos.writeBytes("cd /data/data/com.sensetime.autotest" + "\n");
            for (String cmd :
                    cmds) {
                dos.writeBytes(cmd + "\n");
            }
            dos.writeBytes("");
            dos.flush();
            dos.close();
            BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
            while ((successInfo = br.readLine()) != null) {
                Log.i("info", successInfo);
            }
            process.waitFor();
            process.destroy();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
