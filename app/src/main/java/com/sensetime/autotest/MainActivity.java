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
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.StrictMode;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.sensetime.autotest.adapyer.MyAdapter;
import com.sensetime.autotest.asynctask.EnableTask;
import com.sensetime.autotest.database.MyDBOpenHelper;
import com.sensetime.autotest.server.WebSocketServer;
import com.sensetime.autotest.service.WebSocketService;
import com.sensetime.autotest.util.Wsutil;

import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.sql.SQLOutput;
import java.util.LinkedList;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MainActivity extends AppCompatActivity {

    private WebSocketService WebSClientService;

    private WebSocketService.JWebSocketClientBinder binder;

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

    private ProgressBar pb ;


    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mContext = getApplication();
        initUi();
        addPermisson();
//        openDataBase();
        init();

        ListView listView = findViewById(R.id.ListView);
        LinkedList<String> data = new LinkedList<>();
        MyAdapter myAdapter = new MyAdapter(mContext,data);
        listView.setAdapter(myAdapter);

    }

    private void openDataBase() {
        MyDBOpenHelper myDBHelper = new MyDBOpenHelper(mContext, "my.db", null, 1);
//        SQLiteDatabase db = myDBHelper.getWritableDatabase();

    }

    private void addPermisson() {
        requestPermission();
//        upgradeRootPermission(getPackageCodePath());
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        IntentFilter filter=new IntentFilter("com.caisang");
        receiver=new AppBroadcast();
        registerReceiver(receiver,filter);
    }

    private void bindService() {
        Intent bindIntent = new Intent(mContext, WebSocketService.class);
        bindService(bindIntent, serviceConnection, BIND_AUTO_CREATE);
    }

    private void startJWebSClientService() {
        Intent intent = new Intent(mContext, WebSocketService.class);
        startService(intent);
    }

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            Log.e("MainActivity", "服务与活动成功绑定");
            binder = (WebSocketService.JWebSocketClientBinder) iBinder;
            WebSClientService = binder.getService();
            client = WebSClientService.client;
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.e("MainActivity", "服务与活动成功断开");
        }
    };

    class AppBroadcast extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {
            System.out.println(intent.getExtras());
            Log.e("onReceive:","BroadCastDemo" );
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
                assert process != null;
                process.destroy();
            } catch (Exception ignored) {
            }
        }
        return true;
    }

    public void initUi(){
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
        connect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                System.out.println("触发点击事件");
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        for (int i = 0; i <101 ; i++) {
                            try {
                                Thread.sleep(100);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            pb.setProgress(i);
                        }
                    }
                }).start();

                Wsutil.devicesID=  deviceId.getText().toString();
                image.setVisibility(View.VISIBLE);
//                System.out.println(deviceId.getText());
                //启动服务
                startJWebSClientService();
                //绑定服务
                bindService();



            }
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public void init() {

        new Thread(new Runnable() {

            File SdkDir = new File(getFilesDir()+"/Sdk");
            File gtDir = new File(getFilesDir()+"/Gt");
            File logDir = new File( getFilesDir()+"/Log");
            File videoDir = new File( getFilesDir()+"/Video");
            File auto = new File( "/data/local/tmp/AutoTest");

            @Override
            public void run() {
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
                        dataOutputStream.writeBytes("mkdir " + videoDir.toString() + "\n");
                        dataOutputStream.writeBytes("chmod 777 " + videoDir.toString() + "\n");
                    }
                    if (!auto.exists()) {
                        Log.i("info", "创建auto文件夹");
                        dataOutputStream.writeBytes("mkdir " + auto.toString() + "\n");
                        dataOutputStream.writeBytes("chmod 777 " + auto.toString() + "\n");
                    }
                    dataOutputStream.flush();
                    dataOutputStream.close();
                    mkdirProcess.waitFor();
                    mkdirProcess.destroy();
                }catch (IOException | InterruptedException e){
                    e.printStackTrace();
                }
                Log.i("info","初始化程序完成");

            }
        }).start();
    }
}