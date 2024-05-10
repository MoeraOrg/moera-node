package org.moera.node.config;

public class PoolsConfig {

    private int naming = 16;
    private int remoteTask = 16;
    private int notificationSender = 32;
    private int picker = 12;
    private int push = 4;

    public int getNaming() {
        return naming;
    }

    public void setNaming(int naming) {
        this.naming = naming;
    }

    public int getRemoteTask() {
        return remoteTask;
    }

    public void setRemoteTask(int remoteTask) {
        this.remoteTask = remoteTask;
    }

    public int getNotificationSender() {
        return notificationSender;
    }

    public void setNotificationSender(int notificationSender) {
        this.notificationSender = notificationSender;
    }

    public int getPicker() {
        return picker;
    }

    public void setPicker(int picker) {
        this.picker = picker;
    }

    public int getPush() {
        return push;
    }

    public void setPush(int push) {
        this.push = push;
    }

}
