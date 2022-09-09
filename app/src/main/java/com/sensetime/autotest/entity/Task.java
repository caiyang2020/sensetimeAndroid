package com.sensetime.autotest.entity;

import androidx.annotation.NonNull;

import java.io.Serializable;


import lombok.Data;

public class Task implements Serializable {

    Long sdkId;

    String taskName;

    Long gtId;

    String sdkRootPath;

    String sdkRunPath;

    String runFunc;

    String cmd;

    public Long getSdkId() {
        return sdkId;
    }

    public void setSdkId(Long sdkId) {
        this.sdkId = sdkId;
    }

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    public Long getGtId() {
        return gtId;
    }

    public void setGtId(Long gtId) {
        this.gtId = gtId;
    }

    public String getSdkRootPath() {
        return sdkRootPath;
    }

    public void setSdkRootPath(String sdkRootPath) {
        this.sdkRootPath = sdkRootPath;
    }

    public String getSdkRunPath() {
        return sdkRunPath;
    }

    public void setSdkRunPath(String sdkRunPath) {
        this.sdkRunPath = sdkRunPath;
    }

    public String getRunFunc() {
        return runFunc;
    }

    public void setRunFunc(String runFunc) {
        this.runFunc = runFunc;
    }

    public String getCmd() {
        return cmd;
    }

    public void setCmd(String cmd) {
        this.cmd = cmd;
    }

    @Override
    public String toString() {
        return "Task{" +
                "sdkId=" + sdkId +
                ", taskName='" + taskName + '\'' +
                ", gtId=" + gtId +
                ", sdkRootPath='" + sdkRootPath + '\'' +
                ", sdkRunPath='" + sdkRunPath + '\'' +
                ", runFunc='" + runFunc + '\'' +
                ", cmd='" + cmd + '\'' +
                '}';
    }
}
