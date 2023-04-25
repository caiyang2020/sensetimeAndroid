package com.sensetime.autotest.util;

import android.content.Context;
import android.util.Log;

import com.sensetime.autotest.MainActivity;
import com.sensetime.autotest.entity.Task;

import java.io.File;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.components.ApplicationComponent;
import dagger.hilt.android.components.ServiceComponent;
import dagger.hilt.android.qualifiers.ActivityContext;
import dagger.hilt.android.qualifiers.ApplicationContext;

@Module
@InstallIn({ApplicationComponent.class, ServiceComponent.class})
public class CommandUtil {


    @Provides
    public CommandUtil getCommandUtil() {
        return new CommandUtil();
    }


    public String createCommand(Task t, List<String> l){
        Context mContext = MainActivity.getContext();
        String cmd = t.getCmd();
        String videoPath = l.get(0);
        cmd=cmd.replace("{videoPath}",splicePath(String.valueOf(mContext.getFilesDir()),"Video",videoPath.replaceAll("/","^")));
        cmd=cmd.replace("{fps}","30");
        cmd=cmd.replace("{logPath}",splicePath(String.valueOf(mContext.getFilesDir()),"Log", String.valueOf(t.getId()),videoPath.replaceAll("/","^").replaceAll("\\.[a-zA-z0-9]+$", ".log")));
        cmd=cmd.replace("{facedbPath}",splicePath(String.valueOf(mContext.getFilesDir()),"Log", String.valueOf(t.getId()),"reg.db"));
//        cmd=cmd.replace("{x}",l.get(0));
//        cmd=cmd.replace("{y}",l.get(0));
//        cmd=cmd.replace("{z}",l.get(0));
        Log.d("cmd",cmd);
        return cmd;
    }

    private String splicePath(String... s){
        StringBuilder sb = new StringBuilder();
        Iterator<String> iterator = Arrays.asList(s).iterator();
        while (iterator.hasNext()){
            sb.append(iterator.next());
            if (iterator.hasNext()) {
                sb.append(File.separator);
            }
        }
        return sb.toString();
    }
}