package com.sensetime.autotest.entity;

import androidx.annotation.NonNull;

import java.io.Serializable;


import lombok.Data;

@Data
public class Task implements Serializable {

    String taskName;

    int projectId;

    int sdkId;

    int gtId;

    String modelIds;

    int taskType;

    int createBy;

    String functionName;

    String expandFieldOne;

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    public int getProjectId() {
        return projectId;
    }

    public void setProjectId(int projectId) {
        this.projectId = projectId;
    }

    public int getSdkId() {
        return sdkId;
    }

    public void setSdkId(int sdkId) {
        this.sdkId = sdkId;
    }

    public int getGtId() {
        return gtId;
    }

    public void setGtId(int gtId) {
        this.gtId = gtId;
    }

    public String getModelIds() {
        return modelIds;
    }

    public void setModelIds(String modelIds) {
        this.modelIds = modelIds;
    }

    public int getTaskType() {
        return taskType;
    }

    public void setTaskType(int taskType) {
        this.taskType = taskType;
    }

    public String getFunctionName() {
        return functionName;
    }

    public void setFunctionName(String functionName) {
        this.functionName = functionName;
    }

    public String getExpandFieldOne() {
        return expandFieldOne;
    }

    public void setExpandFieldOne(String expandFieldOne) {
        this.expandFieldOne = expandFieldOne;
    }

    public int getCreateBy() {
        return createBy;
    }

    public void setCreateBy(int createBy) {
        this.createBy = createBy;
    }

    @NonNull
    @Override
    public String toString() {
        return "TaskInfo{" +
                "taskName='" + taskName + '\'' +
                ", projectId=" + projectId +
                ", sdkId=" + sdkId +
                ", gtId=" + gtId +
                ", modelIds='" + modelIds + '\'' +
                ", taskType=" + taskType +
                ", functionName='" + functionName + '\'' +
                ", expandFieldOne='" + expandFieldOne + '\'' +
                '}';
    }


}
