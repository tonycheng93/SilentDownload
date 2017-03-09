package com.sky.silentdownload.silentupgrade.downloader.data;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by BaoCheng on 2017/2/18.
 */

public class DownloadInfo implements Parcelable, Cloneable {

    public String url, md5;

    /**
     * 需要辅助存储的信息
     */
    public Map<String, String> map = new HashMap<>();


    public DownloadInfo() {
    }

    protected DownloadInfo(Parcel in) {
        url = in.readString();
        md5 = in.readString();
        map = in.readHashMap(Thread.currentThread().getContextClassLoader());
    }

    public void putParam(String key, String value) {
        synchronized (map) {
            map.put(key, value);
    }
    }

    public String getParam(String key) {
        synchronized (map) {
            return map.get(key);
        }
    }

    public DownloadInfo clone() {
        try {
            return (DownloadInfo) super.clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DownloadInfo that = (DownloadInfo) o;

        if (url != null ? !url.equalsIgnoreCase(that.url) : that.url != null) return false;
        if (md5 != null ? !md5.equalsIgnoreCase(that.md5) : that.md5 != null) return false;
        return map != null ? map.equals(that.map) : that.map == null;
    }

    @Override
    public int hashCode() {
        int result = url != null ? url.hashCode() : 0;
        result = 31 * result + (md5 != null ? md5.hashCode() : 0);
        result = 31 * result + (map != null ? map.hashCode() : 0);
        return result;
    }

    public static final Creator<DownloadInfo> CREATOR = new Creator<DownloadInfo>() {
        @Override
        public DownloadInfo createFromParcel(Parcel in) {
            return new DownloadInfo(in);
        }

        @Override
        public DownloadInfo[] newArray(int size) {
            return new DownloadInfo[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(url);
        dest.writeString(md5);
        dest.writeMap(map);
    }
}
