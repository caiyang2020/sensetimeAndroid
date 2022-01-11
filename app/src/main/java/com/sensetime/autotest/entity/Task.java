package com.sensetime.autotest.entity;

import lombok.Data;

@Data
public class Task {

    private int taskType;

    private String taskName;

    private String SdkPath;

    private String func;

    private String GtPath;

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    public String getSdkPath() {
        return SdkPath;
    }

    public void setSdkPath(String sdkPath) {
        SdkPath = sdkPath;
    }

    public String getFunc() {
        return func;
    }

    public void setFunc(String func) {
        this.func = func;
    }

    public String getGtPath() {
        return GtPath;
    }

    public void setGtPath(String gtPath) {
        GtPath = gtPath;
    }

    public int getTaskType() {
        return taskType;
    }

    public void setTaskType(int taskType) {
        this.taskType = taskType;
    }
}
