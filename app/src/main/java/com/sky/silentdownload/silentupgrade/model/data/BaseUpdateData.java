package com.sky.silentdownload.silentupgrade.model.data;

import java.io.Serializable;
import java.util.List;

/**
 * Created by BaoCheng on 2017/3/9.
 */

public class BaseUpdateData implements Serializable {

    private int ret;

    private String msg;

    private List<AppUpdateInfo> data;

    public int getRet() {
        return ret;
    }

    public void setRet(int ret) {
        this.ret = ret;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public List<AppUpdateInfo> getData() {
        return data;
    }

    public void setData(List<AppUpdateInfo> data) {
        this.data = data;
    }
}
