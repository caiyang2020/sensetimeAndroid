package com.sensetime.autotest.util;

import android.content.Context;
import android.util.Log;


import androidx.annotation.NonNull;

import com.alibaba.fastjson.JSON;
import com.apkfuns.logutils.LogUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.components.ApplicationComponent;
import dagger.hilt.android.components.ServiceComponent;
import dagger.hilt.android.scopes.ActivityScoped;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.BufferedSink;
import okio.Okio;
import okio.Sink;

@Module
@InstallIn({ApplicationComponent.class, ServiceComponent.class})
public class HttpUtil {


    @Provides
    public HttpUtil getHttpUtil() {
        return new HttpUtil();
    }

    public void get(String getUrl) {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(getUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Charset", "UTF-8");
            connection.setRequestProperty("Content-Type", "text/html;charset=UTF-8");
            connection.connect();
            if (connection.getResponseCode() == 200) {
                InputStream is = connection.getInputStream();
                //do something
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (null != connection) {
                connection.disconnect();
            }
        }
    }

    public void post(String postUrl, Object obj) {

        try {
            // 1. 获取访问地址URL
            URL url = new URL(postUrl);
            // 2. 创建HttpURLConnection对象
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            /* 3. 设置请求参数等 */
            // 请求方式
            connection.setRequestMethod("POST");
            // 设置连接超时时间
            connection.setConnectTimeout(3000);
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setUseCaches(false);
            connection.setInstanceFollowRedirects(true);
            connection.setRequestProperty("Charset", "UTF-8");
            connection.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
            connection.connect();
            String params = JSON.toJSONString(obj);
            OutputStream out = connection.getOutputStream();
            out.write(params.getBytes());
            out.flush();
            out.close();
            // 从连接中读取响应信息
            String msg = "";
            int code = connection.getResponseCode();
            if (code == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    msg += line + "\n";
                }
                reader.close();
            }
            // 5. 断开连接
            connection.disconnect();

            // 处理结果
            System.out.println(msg);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void getAsyn(String url) {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(url).build();
        Call call = client.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                //...
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String result = response.body().string();
                    //处理UI需要切换到UI线程处理
                }
            }
        });
    }

    public void post(String url, String key, String value) {
        OkHttpClient client = new OkHttpClient();
        FormBody body = new FormBody.Builder()
                .add(key, value)
                .build();
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();
        Call call = client.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                //...
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String result = response.body().string();
                    //处理UI需要切换到UI线程处理
                }
            }
        });
    }


    public void downloadFile(Context mContext, String file, String type) {
        //下载路径，如果路径无效了，可换成你的下载路径
        final String url = "http://savvcenter.sensetime.com/resource/" + file;
//        final long startTime = System.currentTimeMillis();
//        Log.i("DOWNLOAD","startTime="+startTime);

        Request request = new Request.Builder().url(url).build();
        new OkHttpClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // 下载失败
                e.printStackTrace();
//                Log.i("DOWNLOAD","download failed");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Sink sink = null;
                BufferedSink bufferedSink = null;

                File localFile;
                switch (type) {

                    case "sdk":
                        localFile = new File(mContext.getFilesDir() + "/Sdk", url.substring(url.lastIndexOf("/") + 1));
                        break;
                    case "gt":
                        localFile = new File(mContext.getFilesDir() + "/Gt", url.substring(url.lastIndexOf("/") + 1));
                        break;
                    case "video":
                        localFile = new File(mContext.getFilesDir() + "/Video", file.replaceAll("/", "^"));
                        break;
                    default:
                        throw new IllegalStateException("Unexpected value: " + type);
                }

                try {
//                    String mSDCardPath= Environment.getExternalStorageDirectory().getAbsolutePath();
//                    File dest = new File(mSDCardPath,   url.substring(url.lastIndexOf("/") + 1));
                    sink = Okio.sink(localFile);
                    bufferedSink = Okio.buffer(sink);
                    bufferedSink.writeAll(response.body().source());

                    bufferedSink.close();
//                    Log.i("DOWNLOAD","download success");
//                    Log.i("DOWNLOAD","totalTime="+ (System.currentTimeMillis() - startTime));

                    if (type.equalsIgnoreCase("sdk")) {
                        PowerShell.cmd("cd " + mContext.getFilesDir() + "/Sdk",
//                            "mkdir "+nfsFile.getName().replace(".tar",""),
//                            "chmod 777 "+nfsFile.getName().replace(".tar",""),
                                "tar -xvf " + url.substring(url.lastIndexOf("/") + 1) + " -C /data/local/tmp/AutoTest/",
                                "chmod -R 777 /data/local/tmp/AutoTest/" + url.substring(url.lastIndexOf("/") + 1).replace(".tar", ""));
                        String[] cmds = {"sh", "-c", "su;cd " + mContext.getFilesDir() + "/Sdk;tar -xvf " + url.substring(url.lastIndexOf("/") + 1) + "\\ -C /data/local/tmp/AutoTest/;chmod -R 777 /data/local/tmp/AutoTest/" + url.substring(url.lastIndexOf("/") + 1).replace(".tar", "")};
                        System.out.println(cmds);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
//                    Log.i("DOWNLOAD","download failed");
                } finally {
                    if (bufferedSink != null) {
                        bufferedSink.close();
                    }

                }
            }
        });
    }

    public void downloadFile(Context mContext, CountDownLatch countDownLatch, String file, String type) {
        //下载路径，如果路径无效了，可换成你的下载路径
        final String url = "http://savvcenter.sensetime.com/resource/" + file;
//        final long startTime = System.currentTimeMillis();
//        Log.i("DOWNLOAD","startTime="+startTime);

        Request request = new Request.Builder().url(url).build();
        new OkHttpClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // 下载失败
                e.printStackTrace();
//                Log.i("DOWNLOAD","download failed");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Sink sink = null;
                BufferedSink bufferedSink = null;

                File localFile;
                switch (type) {

                    case "sdk":
                        localFile = new File(mContext.getFilesDir() + "/Sdk", url.substring(url.lastIndexOf("/") + 1));
                        break;
                    case "gt":
                        localFile = new File(mContext.getFilesDir() + "/Gt", url.substring(url.lastIndexOf("/") + 1));
                        break;
                    case "video":
                        localFile = new File(mContext.getFilesDir() + "/Video", file.replaceAll("/", "^"));
                        break;
                    default:
                        throw new IllegalStateException("Unexpected value: " + type);
                }
                String filename = url.substring(url.lastIndexOf("/") + 1);
                try {
//                    String mSDCardPath= Environment.getExternalStorageDirectory().getAbsolutePath();
//                    File dest = new File(mSDCardPath,   url.substring(url.lastIndexOf("/") + 1));
                    sink = Okio.sink(localFile);
                    bufferedSink = Okio.buffer(sink);
                    bufferedSink.writeAll(response.body().source());

                    bufferedSink.close();
                    Thread.sleep(1000);
//                    Log.i("DOWNLOAD","download success");
//                    Log.i("DOWNLOAD","totalTime="+ (System.currentTimeMillis() - startTime));
                    if (type.equalsIgnoreCase("sdk")) {
                        PowerShell.cmd("cd " + mContext.getFilesDir() + "/Sdk",
//                            "mkdir "+nfsFile.getName().replace(".tar",""),
//                            "chmod 777 "+nfsFile.getName().replace(".tar",""),
                                "tar -xvf " + url.substring(url.lastIndexOf("/") + 1) + " -C  /data/local/tmp/AutoTest/",
                                "chmod -R 777 /data/local/tmp/AutoTest/" + url.substring(url.lastIndexOf("/") + 1).replace(".tar", ""));
//                        String[] cmds = {"sh","-c","su;cd " +mContext.getFilesDir() + "/Sdk;tar -zxvf " + url.substring(url.lastIndexOf("/") + 1)  + " /data/local/tmp/AutoTest/;chmod -R 777 /data/local/tmp/AutoTest/"+url.substring(url.lastIndexOf("/") + 1) .replace(".tgz","")};
//                        System.out.println(cmds);
//                        Process pro = Runtime.getRuntime().exec(cmds);
//                        pro.waitFor();
//                        pro.destroy();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
//                    Log.i("DOWNLOAD","download failed");
                } finally {
                    if (bufferedSink != null) {
                        bufferedSink.close();
                    }
                    countDownLatch.countDown();
                }
            }
        });
    }

    public void downloadFile(Context mContext, CountDownLatch countDownLatch, Long fileId, String type) {
        //下载路径，如果路径无效了，可换成你的下载路径
        String sourcePath;
        switch (type) {
            case "sdk":
                sourcePath = "/data/TestBetterWorkSpace/sdk/" + fileId + ".tar.gz";
                break;
            case "gt":
                sourcePath = "/data/TestBetterWorkSpace/gt/" + fileId + ".csv";
                break;
            case "log":
                sourcePath = "/data/TestBetterWorkSpace/tasklog/" + "TaskLog" + fileId + ".tar.gz";
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + type);
        }
        final String url = "http://savvcenter.sensetime.com/resource/" + sourcePath;
        Request request = new Request.Builder().url(url).build();
        new OkHttpClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // 下载失败
                e.printStackTrace();
                LogUtils.i("DOWNLOAD", "download failed");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Sink sink = null;
                BufferedSink bufferedSink = null;

                File localFile;
                switch (type) {

                    case "sdk":
                        localFile = new File(mContext.getFilesDir() + "/Sdk", url.substring(url.lastIndexOf("/") + 1));
                        break;
                    case "gt":
                        localFile = new File(mContext.getFilesDir() + "/Gt", url.substring(url.lastIndexOf("/") + 1));
                        break;
                    case "log":
                        localFile = new File(mContext.getFilesDir() + "/Log", url.substring(url.lastIndexOf("/") + 1));
                        break;
                    default:
                        throw new IllegalStateException("Unexpected value: " + type);
                }
                String filename = url.substring(url.lastIndexOf("/") + 1);
                try {
                    sink = Okio.sink(localFile);
                    bufferedSink = Okio.buffer(sink);
                    bufferedSink.writeAll(response.body().source());
                    bufferedSink.close();
                    Thread.sleep(1000);
                    switch (type) {
                        case "sdk":
                            PowerShell.cmd("cd " + mContext.getFilesDir() + "/Sdk",
                                    "tar -zxvf " + url.substring(url.lastIndexOf("/") + 1) + " -C  /data/local/tmp/AutoTest/",
                                    "chmod -R 777 /data/local/tmp/AutoTest/" + url.substring(url.lastIndexOf("/") + 1).replace(".tar.gz", ""));
                            break;
                        case "log":
                            //创建以任务名称创建log保存文件夹
                            File dir = new File(mContext.getFilesDir() + "/Log/" + fileId);
                            if (!dir.exists()) {
                                dir.mkdir();
                            }
                            PowerShell.cmd("cd " + mContext.getFilesDir() + "/Log",
                                    "tar -zxvf " + url.substring(url.lastIndexOf("/") + 1) + " -C  " + mContext.getFilesDir() + File.separator + "Log" + File.separator + fileId);
                            break;
                    }
                    response.close();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (bufferedSink != null) {
                        bufferedSink.close();
                    }
                    countDownLatch.countDown();
                }
            }
        });
    }

    public void downloadFile(Context mContext, CountDownLatch countDownLatch, Long fileId, String type, String filename) {
        //下载路径，如果路径无效了，可换成你的下载路径
        String sourcePath;
        switch (type) {
            case "sdk":
                sourcePath = "/data/TestBetterWorkSpace/sdk/" + fileId + ".tar.gz";
                break;
            case "gt":
                sourcePath = "/data/TestBetterWorkSpace/gt/" + fileId + ".csv";
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + type);
        }
        final String url = "http://savvcenter.sensetime.com/resource/" + sourcePath;
        Request request = new Request.Builder().url(url).build();
        new OkHttpClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // 下载失败
                e.printStackTrace();
                LogUtils.i("DOWNLOAD", "download failed");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Sink sink = null;
                BufferedSink bufferedSink = null;

                File localFile;
                switch (type) {
                    case "sdk":
                        localFile = new File(mContext.getFilesDir() + "/Sdk", url.substring(url.lastIndexOf("/") + 1));
                        break;
                    case "gt":
                        localFile = new File(mContext.getFilesDir() + "/Gt", url.substring(url.lastIndexOf("/") + 1));
                        break;
                    default:
                        throw new IllegalStateException("Unexpected value: " + type);
                }
                try {
                    sink = Okio.sink(localFile);
                    bufferedSink = Okio.buffer(sink);
                    bufferedSink.writeAll(response.body().source());
                    bufferedSink.close();
                    Thread.sleep(1000);
                    if (type.equalsIgnoreCase("sdk")) {
                        PowerShell.cmd("cd " + mContext.getFilesDir() + "/Sdk",
                                "tar -zxvf " + url.substring(url.lastIndexOf("/") + 1) + " -C  /data/local/tmp/AutoTest/",
                                "chmod -R 777 /data/local/tmp/AutoTest/" + filename);
                        System.out.println("chmod -R 777 /data/local/tmp/AutoTest/" + filename);
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (bufferedSink != null) {
                        bufferedSink.close();
                    }
                    countDownLatch.countDown();
                }
            }
        });
    }

    public void downloadFile(Context mContext, Semaphore semaphore, String file, String type) throws SocketTimeoutException {
        //下载路径，如果路径无效了，可换成你的下载路径
        final String url = "http://savvcenter.sensetime.com/resource/" + file;
//        final long startTime = System.currentTimeMillis();
//        Log.i("DOWNLOAD","startTime="+startTime);
        Request request = new Request.Builder()
                .url(url).build();
        OkHttpClient client = new OkHttpClient().newBuilder().connectTimeout(60, TimeUnit.SECONDS).build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                if (Objects.equals(e.getClass(), SocketTimeoutException.class)) {
                    Log.i("DOWNLOAD", "异常捕获");
                    semaphore.release();
                }
                // 下载失败
//                Log.i("DOWNLOAD","download failed");
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                Sink sink = null;
                BufferedSink bufferedSink = null;

                File localFile;
                switch (type) {

                    case "sdk":
                        localFile = new File(mContext.getFilesDir() + "/Sdk", url.substring(url.lastIndexOf("/") + 1));
                        break;
                    case "gt":
                        localFile = new File(mContext.getFilesDir() + "/Gt", url.substring(url.lastIndexOf("/") + 1));
                        break;
                    case "video":
                        localFile = new File(mContext.getFilesDir() + "/Video", file.replaceAll("/", "^"));
                        break;
                    default:
                        throw new IllegalStateException("Unexpected value: " + type);
                }
                String filename = url.substring(url.lastIndexOf("/") + 1);
                try {
                    sink = Okio.sink(localFile);
                    bufferedSink = Okio.buffer(sink);
                    bufferedSink.writeAll(response.body().source());
                    bufferedSink.close();
                    response.body().close();
                    Thread.sleep(1000);
                    if (type.equalsIgnoreCase("sdk")) {
                        PowerShell.cmd("cd " + mContext.getFilesDir() + "/Sdk",
                                "tar -xvf " + url.substring(url.lastIndexOf("/") + 1) + " -C  /data/local/tmp/AutoTest/",
                                "chmod -R 777 /data/local/tmp/AutoTest/" + url.substring(url.lastIndexOf("/") + 1).replace(".tar", ""));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (bufferedSink != null) {
                        bufferedSink.close();
                    }
                    semaphore.release();
                }
            }
        });
    }

    public void fileUpload(Long id, String path) {
        // 获得输入框中的路径
        File file = new File(path);
        OkHttpClient client = new OkHttpClient();
        // 上传文件使用MultipartBody.Builder
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("id", String.valueOf(id)) // 提交普通字段
                .addFormDataPart("file", file.getName(), RequestBody.create(MediaType.parse("multipart/form-data"), file)) // 提交图片，第一个参数是键（name="第一个参数"），第二个参数是文件名，第三个是一个RequestBody
                .build();
        // POST请求
        Request request = new Request.Builder()
                .url("http://savvcenter.sensetime.com:9111/MobileCenter/upload")
                .post(requestBody)
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                LogUtils.w("文件上传出现问题");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                LogUtils.i("log文件上传成功");
                assert response.body() != null;
                response.body().close();
                file.delete();
            }
        });
    }
}

