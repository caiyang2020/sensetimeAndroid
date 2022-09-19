package com.sensetime.autotest.util;

import android.os.FileUtils;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


public class FileUtil {

    private static final int BUFFER_SIZE = 1024 * 100;


//         CompressUtil.decompress("/a/tmp/tmp/tar/test.tar.gz","/a/tmp/tmp/tar");

    public static boolean checkSdk(){
        return true;
    }

    public static boolean checkGt(){
        return true;
    }

    public static boolean decompress(String filePath, String outputDir) {
        File file = new File(filePath);
        if (!file.exists()) {
            System.out.println("decompress file not exist.");
            return false;
        }
        try {
//            if (filePath.endsWith(".zip")) {
//                unZip(file, outputDir);
//            }
            if (filePath.endsWith(".tar.gz") || filePath.endsWith(".tgz")) {
                decompressTarGz(file, outputDir);
            }
//            if (filePath.endsWith(".tar.bz2")) {
//                decompressTarBz2(file, outputDir);
//            }

            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }


    public static void decompressTarGz(File file, String outputDir) throws IOException {
        try (TarArchiveInputStream tarIn = new TarArchiveInputStream(
                new GzipCompressorInputStream(
                        new BufferedInputStream(
                                new FileInputStream(file))))) {
            //创建输出目录
            createDirectory(outputDir, null);
            TarArchiveEntry entry = null;
            while ((entry = tarIn.getNextTarEntry()) != null) {
                //是目录
                if (entry.isDirectory()) {
                    //创建空目录
                    createDirectory(outputDir, entry.getName());
                } else {
                    //是文件
                    try (OutputStream out = new FileOutputStream(
                            new File(outputDir + File.separator + entry.getName()))) {
                        writeFile(tarIn, out);
                    }
                }
            }
        }
    }

    /**
     * 写文件
     *
     * @param in
     * @param out
     * @throws IOException
     */
    public static void writeFile(InputStream in, OutputStream out) throws IOException {
        int length;
        byte[] b = new byte[BUFFER_SIZE];
        while ((length = in.read(b)) != -1) {
            out.write(b, 0, length);
        }
    }

    /**
     * 创建目录
     *
     * @param outputDir
     * @param subDir
     */
    public static void createDirectory(String outputDir, String subDir) {
        File file = new File(outputDir);
        //子目录不为空
        if (!(subDir == null || subDir.trim().equals(""))) {
            file = new File(outputDir + File.separator + subDir);
        }
        if (!file.exists()) {
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            file.mkdirs();
        }
    }
}
