package com.sensetime.autotest.entity;

import java.io.Serializable;

public class DeviceMessage<T> implements Serializable {

    private int code;

    private T data;


    public T getData() {
        return data;
    }

    public void setData(T message) {
        this.data = message;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

}
