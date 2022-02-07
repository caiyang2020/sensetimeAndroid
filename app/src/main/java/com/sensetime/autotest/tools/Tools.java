package com.sensetime.autotest.tools;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.Nullable;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

public class Tools extends Service {

    public Tools() {
    }

    private Context mcontext;

    public Tools(Context mcontext) {
        this.mcontext = mcontext;
    }

    public  void taskComplete() throws IOException {

        URL url = new URL("https://cn.bing.com/search?q=no+protocol&cvid=25594cd00d8842399f4bb6ee18a34d61&aqs=edge.0.0l9.403j0j4&FORM=ANAB01&PC=W011");
        HttpURLConnection conn = (HttpsURLConnection) url.openConnection();
        conn.setConnectTimeout(1000);
        if (conn.getResponseCode()!=200){

        }
        System.out.println("成功了");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
