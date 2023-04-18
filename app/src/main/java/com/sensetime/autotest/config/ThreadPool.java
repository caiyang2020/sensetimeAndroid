package com.sensetime.autotest.config;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;



public class ThreadPool {

    public static ThreadPoolExecutor Executor = new ThreadPoolExecutor(3,6,60L, TimeUnit.SECONDS,new LinkedBlockingQueue<Runnable>());

}
