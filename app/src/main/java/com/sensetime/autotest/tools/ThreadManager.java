package com.sensetime.autotest.tools;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ThreadManager {

    final public static List<HashMap<String,Thread>> taskList = new ArrayList<HashMap<String,Thread>>();

    public static int threadNum (){
        return taskList.size();
    }

    public static boolean setTaskList (HashMap s){
        return taskList.add(s);
    }
}
