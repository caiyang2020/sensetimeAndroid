package com.sensetime.autotest.entity;

import java.io.Serializable;

public class Task implements Serializable {

    private Long id;

    private Long sdkId;

    private String taskName;

    private Long gtId;

    private String sdkRootPath;

    private String sdkRunPath;

    private String runFunc;

    private String cmd;

    private String expectOne;

    public String getExpectOne() {
        return expectOne;
    }

    public void setExpectOne(String expectOne) {
        this.expectOne = expectOne;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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
                "id=" + id +
                ", sdkId=" + sdkId +
                ", taskName='" + taskName + '\'' +
                ", gtId=" + gtId +
                ", sdkRootPath='" + sdkRootPath + '\'' +
                ", sdkRunPath='" + sdkRunPath + '\'' +
                ", runFunc='" + runFunc + '\'' +
                ", cmd='" + cmd + '\'' +
                ", expectOne='" + expectOne + '\'' +
                '}';
    }
}
