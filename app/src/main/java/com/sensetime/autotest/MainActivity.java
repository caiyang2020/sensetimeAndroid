package com.sensetime.autotest;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.StrictMode;
import android.provider.Settings;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.fastjson.JSONObject;
import com.apkfuns.log2file.LogFileEngineFactory;
import com.apkfuns.logutils.LogUtils;
import com.sensetime.autotest.adapyer.MyAdapter;
import com.sensetime.autotest.config.ThreadPool;
import com.sensetime.autotest.database.MyDBOpenHelper;
import com.sensetime.autotest.entity.AndroidSdkBase;
import com.sensetime.autotest.entity.Task;
import com.sensetime.autotest.service.WebSocketService;
import com.sensetime.autotest.util.Wsutil;


import java.io.DataOutputStream;
import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.Objects;
import java.util.concurrent.ThreadPoolExecutor;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import lombok.SneakyThrows;


@AndroidEntryPoint
public class MainActivity extends AppCompatActivity {

    private final WebSocketService WebSClientService = WebSocketService.instance;
//    private final ThreadPoolExecutor executor = ThreadPool.Executor;

    @SuppressLint("StaticFieldLeak")
    private static Context mContext;

    @Inject
    AndroidSdkBase androidSdkBase;

    private static Handler MainActivityHandler;
//    private AppBroadcast receiver;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        applicationInit();
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

    private void addPermission() {
        requestPermission();
        //升级root权限，这个是启动中最重要的一步
        if (upgradeRootPermission(getPackageCodePath())){
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
            LogUtils.i("The system permission request is complete");
        }
    }

    //启动Websocket服务
    private void startWebSClientService() {
        Intent intent = new Intent(mContext, WebSocketService.class);
        startService(intent);
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

    public boolean upgradeRootPermission(String pkgCodePath) {
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
        assert actionBar != null;
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
        ListView listView = findViewById(R.id.ListView);
        LinkedList<String> data = new LinkedList<>();
        MyAdapter myAdapter = new MyAdapter(mContext, data);
        listView.setAdapter(myAdapter);
    }

    private void applicationInit() {
        mContext = getApplication();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            initLog();
        }
        initUi();
        addPermission();
        openDataBase();
        androidSdkBase.init();
        String androidID = Settings.System.getString(getContentResolver(), Settings.System.ANDROID_ID);
        Wsutil.devicesID = androidID;
        LogUtils.e(androidID);
        CreateHandler();
        //启动服务
        startWebSClientService();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @SuppressLint("HandlerLeak")
    private void CreateHandler() {
        MainActivityHandler = new Handler() {
            @SuppressLint("SetTextI18n")
            @Override
            public void handleMessage(@NonNull Message msg) {
                switch (msg.what){
                    case 1:
                        Task test = JSONObject.parseObject(msg.getData().getString("task"),Task.class);
                        taskName.setText(test.getTaskName());
                        sdk.setText(test.getSdkId()+"");
                        funGt.setText(test.getGtId()+"");
                        runFunc.setText(test.getRunFunc());
                        break;
                    case 2:
                        int process = msg.getData().getInt("process");
                        pb.setProgress(process);
                        pbText.setText(process + "%");
                        break;
                    default:
                        System.out.println("kongkongruye");
                }
            }
        };
    }


    /**
     * 以下全部为getter
     * @return
     */
    public static Handler getMainActivityHandler() {
        return MainActivityHandler;
    }

    public static Context getContext(){

        return mContext;
    }
}