package com.sensetime.autotest;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.widget.Toast;

import com.emc.ecs.nfsclient.nfs.io.Nfs3File;
import com.emc.ecs.nfsclient.nfs.io.NfsFileInputStream;
import com.emc.ecs.nfsclient.nfs.nfs3.Nfs3;
import com.emc.ecs.nfsclient.rpc.CredentialUnix;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class MainActivity extends AppCompatActivity {

    private  final String NFS_IP = "10.151.4.123";
    private  final String NFS_DIR = "/data";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        requestPermission();
        System.out.println(getPackageCodePath());
        upgradeRootPermission(getPackageCodePath());
        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }
//        doingdown();

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
                process.destroy();
            } catch (Exception e) {
            }
        }
        return true;
    }


    private void doingdown(){
        String NfsFileDir = "/Testdata/N822/action_hard/ACTION_N822_20211113/00001/CA/AP14/ISD01ADQJX40CAAEEA01.mp4";
        String localDir = "/";
        InputStream inputStream = null;
        OutputStream outputStream = null;
        try {
            Nfs3 nfs3 = new Nfs3(NFS_IP, NFS_DIR, new CredentialUnix(0, 0, null), 3);
            //创建远程服务器上Nfs文件对象
            Nfs3File nfsFile = new Nfs3File(nfs3, NfsFileDir);
            String localFileName = localDir + nfsFile.getName();
            //创建一个本地文件对象
            System.out.println(localFileName);
            File localFile = new File(getFilesDir(),nfsFile.getName());
//            try {
//                localFile.createNewFile();
//                localFile.setWritable(true);
//
//            }catch (Exception e){
//                e.printStackTrace();
//            }
            //打开一个文件输入流
//            File localFile = getFilesDir();
            inputStream = new BufferedInputStream(new NfsFileInputStream(nfsFile));
            //打开一个远程Nfs文件输出流，将文件复制到的目的地
            outputStream = new BufferedOutputStream(new FileOutputStream(localFile,true));

            //缓冲内存
            byte[] buffer = new byte[1024];

            while (inputStream.read(buffer) != -1) {
                outputStream.write(buffer);
            }
            System.out.println("文件下载完成！");
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            try {
                if (outputStream != null) {
                    outputStream.close();
                }
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }



}