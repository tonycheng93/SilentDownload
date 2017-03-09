package com.sky.silentdownload.silentupgrade.model.data;

import java.io.Serializable;

/**
 * Created by BaoCheng on 2017/3/9.
 */

public class AppUpdateInfo implements Serializable{

    private long fileSize;
    private String appName;
    private String icon;
    private String appId;
    private String updateTime;
    private String verName;
    private String verCode;
    private int updateType; //1：强制升级，0：默认升级
    private String appPackage;
    private String startAction;
    private String updateInfo;
    public static final int UPDATETYPE_FORCE = 1;
    public static final int UPDATETYPE_NORMAL = 0;

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public String getUpdateInfo() {
        return updateInfo;
    }

    public void setUpdateInfo(String updateInfo) {
        this.updateInfo = updateInfo;
    }

    public String getStartAction() {
        return startAction;
    }

    public void setStartAction(String startAction) {
        this.startAction = startAction;
    }

    public String getAppPackage() {
        return appPackage;
    }

    public void setAppPackage(String appPackage) {
        this.appPackage = appPackage;
    }

    public int getUpdateType() {
        return updateType;
    }

    public void setUpdateType(int updateType) {
        this.updateType = updateType;
    }

    public String getVerCode() {
        return verCode;
    }

    public void setVerCode(String verCode) {
        this.verCode = verCode;
    }

    public String getVerName() {
        return verName;
    }

    public void setVerName(String verName) {
        this.verName = verName;
    }

    public String getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(String updateTime) {
        this.updateTime = updateTime;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }
}
