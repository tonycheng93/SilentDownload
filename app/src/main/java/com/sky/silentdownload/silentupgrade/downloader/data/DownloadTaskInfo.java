package com.sky.silentdownload.silentupgrade.downloader.data;


import java.io.File;
import java.io.Serializable;

import static com.sky.silentdownload.silentupgrade.downloader.data.Status.NONE;


/**
 * Created by BaoCheng on 2017/2/20.
 */

public class DownloadTaskInfo implements Serializable, Cloneable {
    public DownloadInfo downloadInfo;
    public long size;
    public String savePath;
    public int state = NONE;


    public DownloadTaskInfo() {

    }

    public long getCurrentLength() {
        return new File(savePath).length();
    }


    public DownloadTaskInfo clone() {
        try {
            return (DownloadTaskInfo) super.clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DownloadTaskInfo that = (DownloadTaskInfo) o;

        if (size != that.size) return false;
        if (state != that.state) return false;
        if (downloadInfo != null ? !downloadInfo.equals(that.downloadInfo) : that.downloadInfo != null)
            return false;
        return savePath != null ? savePath.equals(that.savePath) : that.savePath == null;

    }

    @Override
    public int hashCode() {
        int result = downloadInfo != null ? downloadInfo.hashCode() : 0;
        result = 31 * result + (int) (size ^ (size >>> 32));
        result = 31 * result + (savePath != null ? savePath.hashCode() : 0);
        result = 31 * result + state;
        return result;
    }
}