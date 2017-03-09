package com.sky.silentdownload.silentupgrade.downloader.core.impl;

import android.content.Context;
import android.text.TextUtils;

import com.alibaba.fastjson.JSONObject;
import com.sky.silentdownload.silentupgrade.downloader.core.IDB;
import com.sky.silentdownload.silentupgrade.downloader.data.DownloadTaskInfo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by lu on 17-2-22.
 */

public class FileDBImpl implements IDB {
    /**
     * 所有下载任务配置文件后缀为.dd
     * 如下载存储文件名为 aa.apk 则其对应配置文件名为 aa.apk.dd
     */
    private static final String SUFFIX = ".dd";
    private static final String FORMAT = "%s" + SUFFIX;

    public static final String getFileDBConfigFilePath(DownloadTaskInfo info) {
        return String.format(FORMAT, info.savePath);
    }

    private Context mContext;

    public FileDBImpl(Context context) {
        mContext = context;
    }

    @Override
    public boolean update(DownloadTaskInfo info) {
        try {
            File file = new File(getFileDBConfigFilePath(info));
            if (file.exists())
                file.delete();
            if (!file.getParentFile().exists())
                file.getParentFile().mkdirs();
            byte[] content = JSONObject.toJSONString(info).getBytes();
            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(file);
                fos.write(content);
                fos.close();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (fos != null)
                    try {
                        fos.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public DownloadTaskInfo read(String file) {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(new File(file));
            BufferedReader br = new BufferedReader(new InputStreamReader(fis));
            StringBuffer sb = new StringBuffer();
            String str = "";
            while ((str = br.readLine()) != null)
                sb.append(str);
            String value = sb.toString();
            return JSONObject.parseObject(value, DownloadTaskInfo.class);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (fis != null)
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }
        return null;
    }

    private List<DownloadTaskInfo> _list(String path) {
        List<DownloadTaskInfo> ret = new ArrayList<>();
        if (!TextUtils.isEmpty(path)) {
            File dir = new File(path);
            if (dir.exists()) {
                File[] childs = dir.listFiles();
                for (File child : childs) {
                    if (child.isFile()) {
                        if (child.getName().endsWith(SUFFIX)) {
                            DownloadTaskInfo info = read(child.getAbsolutePath());
                            if (info != null)
                                ret.add(info);
                        }
                    }
                }
            }
        }
        return ret;
    }

    @Override
    public List<DownloadTaskInfo> list() {
        List<DownloadTaskInfo> ret = new ArrayList<>();
        ret.addAll(_list(DownloadTaskManagerImpl.getInternalSaveDir(mContext)));
        ret.addAll(_list(DownloadTaskManagerImpl.getExternalSaveDir(mContext)));
        return ret;
    }

    @Override
    public boolean remove(DownloadTaskInfo info) {
        try {
            File file = new File(getFileDBConfigFilePath(info));
            file.delete();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
