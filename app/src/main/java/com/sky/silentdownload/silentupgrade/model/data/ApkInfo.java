package com.sky.silentdownload.silentupgrade.model.data;


import java.io.Serializable;

/**
 * Created by BaoCheng on 2017/3/9.
 */

public class ApkInfo implements Serializable {
    private int ret;

    private String md5;

    private String download;

    private String msg;

    public int getRet() {
        return ret;
    }

    public void setRet(int ret) {
        this.ret = ret;
    }

    public String getDownload() {
        return download;
    }

    public void setDownload(String download) {
        this.download = download;
    }

    public String getMd5() {
        return md5;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }
}
