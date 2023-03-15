package com.sensetime.autotest;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.StrictMode;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.apkfuns.log2file.LogFileEngineFactory;
import com.apkfuns.logutils.LogUtils;
import com.sensetime.autotest.adapyer.MyAdapter;
import com.sensetime.autotest.database.MyDBOpenHelper;
import com.sensetime.autotest.server.WebSocketServer;
import com.sensetime.autotest.service.WebSocketService;
import com.sensetime.autotest.util.Cmd;
import com.sensetime.autotest.util.Wsutil;


import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.Objects;

import dagger.hilt.android.HiltAndroidApp;
import lombok.SneakyThrows;

public class MainActivity extends AppCompatActivity {

    private WebSocketService WebSClientService;

    private WebSocketService.WebSocketClientBinder binder;

    private WebSocketServer client;

    private Context mContext;

    private AppBroadcast receiver;

    private SQLiteOpenHelper myDBHelper;

    private TextView deviceId;

    private TextView taskName;

    private TextView sdk;

    private TextView runFunc;

    private TextView funGt;

    private Button connect;

    private Button user;

    private ImageView image;

    private ProgressBar pb;
    private TextView pbText;


    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mContext = getApplication();
        initLog();
        initUi();
        addPermisson();
        openDataBase();
        init();
        String androidID = Settings.System.getString(getContentResolver(), Settings.System.ANDROID_ID);
        Wsutil.devicesID = androidID;
        LogUtils.e(androidID);
        //启动服务
        startWebSClientService();
        //绑定服务
        bindService();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

    }

    @SneakyThrows
    @RequiresApi(api = Build.VERSION_CODES.N)
    private void initLog() {

        LogUtils.getLogConfig()
                .configAllowLog(true)
                .configTagPrefix("app")
                .configShowBorders(false)
                .configFormatTag("%d{HH:mm:ss:SSS} %t %c");

        LogUtils.getLog2FileConfig().configLog2FileEnable(true)
                // targetSdkVersion >= 23 需要确保有写sdcard权限
                .configLog2FilePath(getDataDir() + "/cache")
                .configLog2FileNameFormat("%d{yyyyMMdd}.txt")
                .configLogFileEngine(new LogFileEngineFactory(mContext));

        File logDir = new File(getDataDir() + "/cache");
        @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) - 7);
        Date lastDay = calendar.getTime();
        System.out.println(sdf.format(lastDay));
        for (File file : Objects.requireNonNull(logDir.listFiles())) {
            try {
                Date trueDay = sdf.parse(file.toString().split("/")[file.toString().split("/").length - 1].replace(".txt", ""));
                assert trueDay != null;
                if (trueDay.compareTo(lastDay) < 0) {
                    file.delete();
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
    }

    private void openDataBase() {
        MyDBOpenHelper myDBHelper = new MyDBOpenHelper(mContext, "my.db", null, 1);
        SQLiteDatabase db = myDBHelper.getWritableDatabase();
        LogUtils.i("Database preparation is complete");
    }

    private void addPermisson() {
        requestPermission();
        upgradeRootPermission(getPackageCodePath());
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        LogUtils.i("The system permission request is complete");
        IntentFilter filter = new IntentFilter("com.caisang");
        receiver = new AppBroadcast();
        registerReceiver(receiver, filter);
        LogUtils.i("Broadcast Listener registration is complete");
    }

    private void bindService() {
        Intent bindIntent = new Intent(mContext, WebSocketService.class);
        bindService(bindIntent, serviceConnection, BIND_AUTO_CREATE);
    }

    private void startWebSClientService() {
        Intent intent = new Intent(mContext, WebSocketService.class);
        startService(intent);
    }

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            Log.e("MainActivity", "服务与活动成功绑定");
            binder = (WebSocketService.WebSocketClientBinder) iBinder;
            WebSClientService = binder.getService();
            client = WebSClientService.client;
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.e("MainActivity", "服务与活动成功断开");
        }
    };

    class AppBroadcast extends BroadcastReceiver {

        //消息接收模块
        @Override
        public void onReceive(Context context, Intent intent) {


            String message = null;
            if ((message = intent.getStringExtra("task")) != null)
//            {String message = intent.getStringExtra("task");
            {
                Intent intentTask = new Intent();
                intentTask.setPackage(getPackageName());
                intentTask.setAction("com.auto.test");
                Bundle bundle = new Bundle();
                bundle.putString("task", message);
                intentTask.putExtras(bundle);
//                intentTask.putExtra("context", (Parcelable) context);
                startService(intentTask);
                LogUtils.i("任务启动");
            }

            int process = intent.getIntExtra("process", 1000);
//            Task task = JSON.parseObject(intent.getStringExtra("task"),Task.class);
//            if (task!=null) {
//                taskName.setText(task.getTaskName());
//                sdk.setText(task.getSdkPath().split("/")[task.getSdkPath().split("/").length - 1].replace(".tar", ""));
//                funGt.setText(task.getGtPath().split("/")[task.getGtPath().split("/").length - 1].replace(".csv", ""));
//                runFunc.setText(task.getFunc());
//            }
            if (process != 1000) {
                pb.setProgress(process);
                pbText.setText(process + "%");
            }

//            String message;
//            if ((message = intent.getStringExtra("message"))!=null){
//                try {
//                    client.send(message);
//                }catch (WebsocketNotConnectedException e){
//
//                }
//
//            }


        }
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
            String cmd = "chmod 777 " + pkgCodePath;
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

    public void initUi() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.hide();
        deviceId = findViewById(R.id.editTextTextPersonName1);
        taskName = findViewById(R.id.editTextTextPersonName2);
        sdk = findViewById(R.id.editTextTextPersonName3);
        runFunc = findViewById(R.id.editTextTextPersonName4);
        funGt = findViewById(R.id.editTextTextPersonName5);
        connect = findViewById(R.id.button);
        user = findViewById(R.id.button2);
        image = findViewById(R.id.imageView);
        pb = findViewById(R.id.progressBar);
        pbText = findViewById(R.id.textView8);
        deviceId.setText(Settings.System.getString(getContentResolver(), Settings.System.ANDROID_ID));
        image.setVisibility(View.VISIBLE);
//        connect.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Wsutil.devicesID = deviceId.getText().toString();
//                image.setVisibility(View.VISIBLE);
////                System.out.println(deviceId.getText());
//                //启动服务
//                startJWebSClientService();
//                //绑定服务
//                bindService();
//            }
//        });

        ListView listView = findViewById(R.id.ListView);
        LinkedList<String> data = new LinkedList<>();
        MyAdapter myAdapter = new MyAdapter(mContext, data);
        listView.setAdapter(myAdapter);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public void init() {
        new Thread(new Runnable() {

            File SdkDir = new File(getFilesDir() + "/Sdk");
            File gtDir = new File(getFilesDir() + "/Gt");
            File logDir = new File(getFilesDir() + "/Log");
            File videoDir = new File(getFilesDir() + "/Video");
            File auto = new File("/data/local/tmp/AutoTest");
            @Override
            public void run() {
                //启动时先删除之前的log
                SdkDir.delete();
                gtDir.delete();
                logDir.delete();
                videoDir.delete();
//                auto.delete();
                Cmd.execute("rm "+auto);
                try {

                    Process mkdirProcess = Runtime.getRuntime().exec("su");
                    DataOutputStream dataOutputStream = new DataOutputStream(mkdirProcess.getOutputStream());
                    Log.i("info", "程序进入初始化");
                    if (!SdkDir.exists()) {
                        Log.i("info", "创建SDK文件夹");
                        dataOutputStream.writeBytes("mkdir " + SdkDir.toString() + "\n");
                        dataOutputStream.writeBytes("chmod 777 " + SdkDir.toString() + "\n");
                    }
                    if (!gtDir.exists()) {
                        Log.i("info", "创建Gt文件夹");
                        dataOutputStream.writeBytes("mkdir " + gtDir.toString() + "\n");
                        dataOutputStream.writeBytes("chmod 777 " + gtDir.toString() + "\n");
                    }
                    if (!logDir.exists()) {
                        Log.i("info", "创建Log文件夹");
                        dataOutputStream.writeBytes("mkdir " + logDir.toString() + "\n");
                        dataOutputStream.writeBytes("chmod 777 " + logDir.toString() + "\n");
                    }
                    if (!videoDir.exists()) {
                        Log.i("info", "创建video文件夹");
//                      dataOutputStream.writeBytes("rm " + videoDir.toString() + "/*\n");
                        dataOutputStream.writeBytes("mkdir " + videoDir.toString() + "\n");
                        dataOutputStream.writeBytes("chmod 777 " + videoDir.toString() + "\n");
                    } else {
                        dataOutputStream.writeBytes("rm " + videoDir.toString() + "/*\n");
//                        dataOutputStream.writeBytes("mkdir " + videoDir.toString() + "\n");
//                        dataOutputStream.writeBytes("chmod 777 " + videoDir.toString() + "\n");
                    }
                    dataOutputStream.flush();
                    dataOutputStream.close();
                    mkdirProcess.waitFor();
                    mkdirProcess.destroy();
                    Log.i("info", "Initialization complete");
                } catch (IOException | InterruptedException e) {
                    LogUtils.e("Failed to initialize folder");
                    LogUtils.e(e);
                    e.printStackTrace();
                }
            }
        }).start();
    }
}