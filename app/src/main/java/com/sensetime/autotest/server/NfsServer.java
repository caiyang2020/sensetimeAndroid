package com.sensetime.autotest.server;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import com.apkfuns.logutils.LogUtils;
import com.emc.ecs.nfsclient.nfs.io.Nfs3File;
import com.emc.ecs.nfsclient.nfs.io.NfsFileInputStream;
import com.emc.ecs.nfsclient.nfs.io.NfsFileOutputStream;
import com.emc.ecs.nfsclient.nfs.nfs3.Nfs3;
import com.emc.ecs.nfsclient.rpc.CredentialUnix;
import com.sensetime.autotest.util.PowerShell;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


public class NfsServer extends Service {

    public static void getFile(Context context, String path,String type){

        String allPath = path;

        path=path.replaceFirst("/data","");
        String NFS_IP = "";
        if (path.contains("TestData")){
            NFS_IP="10.151.3.26";
        }else if(path.contains("Testdata")) {
            NFS_IP="10.151.4.123";
        }else {
            NFS_IP="10.151.4.123";
        }
        String NfsFileDir = path;
        String localDir = "/";
        InputStream inputStream = null;
        OutputStream outputStream = null;
        try {
            Nfs3 nfs3 = new Nfs3(NFS_IP, "/data", new CredentialUnix(0, 0, null), 3);
            //创建远程服务器上Nfs文件对象
//            System.out.println(NfsFileDir.split("/$"));
//            StringBuilder fatherPath = new StringBuilder();
//            for (int i=0; (i<NfsFileDir.split("/").length-1);i++){
//                if (i==0){
//                    fatherPath.append(NfsFileDir.split("/")[i]);
//                }else {
//                    fatherPath.append("/").append(NfsFileDir.split("/")[i]);
//                }
//            }
//            LogUtils.e(fatherPath);
//            Nfs3File nfsFile1 = new Nfs3File(nfs3, String.valueOf(fatherPath));
//            Nfs3File nfsFile = new Nfs3File(nfsFile1,NfsFileDir.split("/")[NfsFileDir.split("/").length-1]);
            LogUtils.e(NfsFileDir);
            Nfs3File nfsFile = new Nfs3File(nfs3, NfsFileDir);
            String localFileName = localDir + nfsFile.getName();
            //创建一个本地文件对象
            File localFile;
            switch (type){
                
                case "sdk":
//                    localFile = new File(context.getFilesDir()+"/Sdk", nfsFile.getName());
                    localFile = new File(context.getFilesDir()+"/Sdk", nfsFile.getName());
                    break;
                case "gt":
                    localFile = new File(context.getFilesDir()+"/Gt", nfsFile.getName());
                    break;
                case "video":
                    localFile = new File(context.getFilesDir()+"/Video",allPath.replaceAll("/","^"));
                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + type);
            }

            inputStream = new BufferedInputStream(new NfsFileInputStream(nfsFile));
            //打开一个远程Nfs文件输出流，将文件复制到的目的地
            if (!localFile.exists()) {
                outputStream = new BufferedOutputStream(new FileOutputStream(localFile));

                //缓冲内存
                byte[] buffer = new byte[8192];
                int lenth = 0;
                while ((lenth=inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer,0,lenth);
                }
                outputStream.close();
                inputStream.close();
                Log.i("info","文件下载完成！");
                if (type.equalsIgnoreCase("sdk")) {
                    PowerShell.cmd("cd " + context.getFilesDir() + "/Sdk",
//                            "mkdir "+nfsFile.getName().replace(".tar",""),
//                            "chmod 777 "+nfsFile.getName().replace(".tar",""),
                            "tar -xvf " + nfsFile.getName()+" -C /data/local/tmp/AutoTest/",
                            "chmod -R 777 /data/local/tmp/AutoTest/"+nfsFile.getName().replace(".tar",""));
                }
            }else {
                Log.i("info","文件已存在,不进入下载");
            }

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

    public static void uploadFile(String dirName) {
        String localDir = dirName;
        InputStream inputStream = null;
        OutputStream outputStream = null;
        try {
            //创建一个本地文件对象
            File localFile = new File(localDir);
            //获取本地文件的文件名，此名字用于在远程的Nfs服务器上指定目录创建同名文件
            String localFileName = localFile.getName();
            Nfs3 nfs3 = new Nfs3("10.151.4.123", "/data", new CredentialUnix(0, 0, null), 3);
            //创建远程服务器上Nfs文件对象
            Nfs3File NfsFile = new Nfs3File(nfs3, "/test_platform/task_log/" + localFileName);
            //打开一个文件输入流
            inputStream = new BufferedInputStream(new FileInputStream(localFile));
            //打开一个远程Nfs文件输出流，将文件复制到的目的地
            outputStream = new BufferedOutputStream(new NfsFileOutputStream(NfsFile));

            //缓冲内存
            byte[] buffer = new byte[8192];
            int lenth = 0;
            while ((lenth = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer,0,lenth);
            }
            System.out.println("文件上传完成！");
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            try {
                if (outputStream != null) {
                    outputStream.close();
                }
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
