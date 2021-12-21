package com.sensetime.autotest.util;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import com.emc.ecs.nfsclient.nfs.io.Nfs3File;
import com.emc.ecs.nfsclient.nfs.io.NfsFileInputStream;
import com.emc.ecs.nfsclient.nfs.nfs3.Nfs3;
import com.emc.ecs.nfsclient.rpc.CredentialUnix;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Logger;

public class NfsServer{

    private File FilesDir;
    private   String NFS_IP;
    private   String NFS_DIR = "/data";

    public NfsServer(File FilesDir,String string){
        switch (string){
            case "TestData" :
                NFS_IP="10.151.3.26";

            case "Testdata":
                NFS_IP="10.151.4.123";

        }
        this.FilesDir=FilesDir;
    }


    public void doingdown(){
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
            File localFile = new File(FilesDir,nfsFile.getName());
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
            if (!localFile.exists()) {
                outputStream = new BufferedOutputStream(new FileOutputStream(localFile));

                //缓冲内存
                byte[] buffer = new byte[1024];

                while (inputStream.read(buffer) != -1) {
                    outputStream.write(buffer);
                }
                Log.i("info","文件下载完成！");
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

}
