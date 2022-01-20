package com.sensetime.autotest.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;

import androidx.annotation.Nullable;

import com.sensetime.autotest.util.WebSocketServer;

import java.net.URI;


public class WebSocketService extends Service {

        private URI uri;
        public WebSocketServer client;
        private JWebSocketClientBinder mBinder = new JWebSocketClientBinder();

        //用于Activity和service通讯
        public class JWebSocketClientBinder extends Binder {
            public WebSocketService getService() {
                return WebSocketService.this;
            }
        }
        @Override
        public IBinder onBind(Intent intent) {
            return mBinder;
        }
    }