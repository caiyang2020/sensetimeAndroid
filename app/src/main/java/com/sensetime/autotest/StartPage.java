package com.sensetime.autotest;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import java.io.File;
import java.io.IOException;

public class StartPage extends AppCompatActivity {

    Handler mHandler = new Handler();


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.start_layout);
        ActionBar actionBar = getSupportActionBar();
        actionBar.hide();
//        String mSDCardPath= Environment.getExternalStorageDirectory().getAbsolutePath();
//        String path = getFilesDir().getPath();
//        System.out.println("++++++++++++++"+mSDCardPath);
//        File file = new File(path+"/1.csv");
//        try {
//            file.createNewFile();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(StartPage.this,MainActivity.class);
                startActivity(intent);
                finish();
            }
        },3000);

    }
}
