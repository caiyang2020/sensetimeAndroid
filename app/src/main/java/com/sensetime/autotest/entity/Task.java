package com.sensetime.autotest.entity;

import lombok.Data;

@Data
public class Task {

    private int status;

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    private int taskCode;

    private int taskType;

    private String taskName;

    private String SdkPath;

    private String func;

    private String GtPath;

    private String deviceID;

    public String getDeviceID() {
        return deviceID;
    }

    public void setDeviceID(String deviceID) {
        this.deviceID = deviceID;
    }

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

    public int getTaskCode() {
        return taskCode;
    }

    public void setTaskCode(int taskCode) {
        this.taskCode = taskCode;
    }
}
