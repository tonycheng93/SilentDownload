package com.sky.silentdownload.silentupgrade.downloader.data;

/**
 * Created by BaoCheng on 2017/2/20.
 */

public class Status {

    public static final int NONE = 1000; //无状态
    public static final int START = 1001; //准备下载
    public static final int PROGRESS = 1002; //下载中
    public static final int PAUSE = 1003; //暂停
    public static final int REMOVE = 1004; //取消
    public static final int FINISH = 1005; //下载完成
    public static final int ERROR = 1006; //下载出错
    public static final int ERROR_MD5 = 1007;//MD5错误
    public static final int ERROR_SPACE = 1008;//空间不足

}
