package com.sensetime.autotest.util;

import android.util.Log;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

public class PowerShell {
    Process process ;
    DataOutputStream dos;

    public PowerShell() {
        try {
            this.process=Runtime.getRuntime().exec("su");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void cmd(String... cmds) {
        String errorInfo ="";
        String successInfo ="";
        try {
            Process process=Runtime.getRuntime().exec("su");
            DataOutputStream dos = new DataOutputStream(process.getOutputStream());
            dos.writeBytes("cd /data/data/com.sensetime.autotest"+"\n");
            for (String cmd:
                 cmds) {
               dos.writeBytes(cmd+"\n");
            }
            dos.writeBytes("");
            dos.flush();
            dos.close();
//            System.out.println(process.waitFor());
//            process.waitFor();
            BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
            while ((successInfo = br.readLine()) != null) {
               Log.i("info",successInfo);
//               if ("Read video finished".equalsIgnoreCase(successInfo)){
//                   break;
//               }
            }
            InputStream is = process.getErrorStream();
            System.out.println(is.read());
            Log.i("error", String.valueOf(is.read()));
            BufferedReader errorResult = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            while ((errorInfo = errorResult.readLine()) != null) {
                Log.i("info",errorInfo);
            }
            process.destroy();
        } catch (IOException  e) {
            e.printStackTrace();
            System.out.println("barrier99999");
        }
    }

}
