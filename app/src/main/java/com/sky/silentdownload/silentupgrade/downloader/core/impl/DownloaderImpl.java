package com.sky.silentdownload.silentupgrade.downloader.core.impl;

import android.content.Context;
import android.util.Log;

import com.sky.silentdownload.silentupgrade.downloader.IDownloadTaskManagerListener;
import com.sky.silentdownload.silentupgrade.downloader.core.IDownloader;
import com.sky.silentdownload.silentupgrade.downloader.data.DownloadTaskInfo;
import com.sky.silentdownload.silentupgrade.downloader.data.Status;
import com.sky.silentdownload.silentupgrade.utils.Android;
import com.sky.silentdownload.silentupgrade.utils.BufferedRandomAccessFile;
import com.sky.silentdownload.silentupgrade.utils.MD5Util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by BaoCheng on 2017/2/22.
 */

public class DownloaderImpl implements Runnable, IDownloader {

    private Context mContext;
    private DownloadTaskInfo mDownloadTaskInfo;
    private IDownloadTaskManagerListener listener;

    private static final String TAG = "downloader";
    private static final float TIMER_INTERVAL = 2.0f;
    private Timer timer = null;
    public Thread thread = null;
    public long createtime = 0, starttime = 0;
    private long length = 0;
    private DOWNLOADER_STATE state = DOWNLOADER_STATE.NOT_RUNNING;

    /**
     * 当前线程状态
     */
    private enum DOWNLOADER_STATE {
        NOT_RUNNING,
        RUNNING_NOT_PREPARED,
        PREPARED_NOT_START,
        PROCESSING,
        FINISH,
        DOING_STOP,
        DOING_DELETE,
    }

    private class OnErrorException extends Exception {
        /**
         * @Fields serialVersionUID
         */
        private static final long serialVersionUID = -1242081093285115605L;
        public int error = Status.ERROR;

        public OnErrorException(int error) {
            super();
            this.error = error;
        }
    }

    private class OnStopException extends Exception {
        /**
         * @Fields serialVersionUID
         */
        private static final long serialVersionUID = 5539965820917633489L;
    }

    private class OnDeleteException extends Exception {
        /**
         * @Fields serialVersionUID
         */
        private static final long serialVersionUID = -1014700838147515744L;
    }

    public DownloaderImpl(Context context, DownloadTaskInfo taskInfo) {
        mContext = context;
        mDownloadTaskInfo = taskInfo;
    }

    @Override
    public void run() {
        try {
            if (mDownloadTaskInfo == null || mDownloadTaskInfo.downloadInfo == null) {
                if (listener != null)
                    listener.onDownloadStatus(Status.ERROR, mDownloadTaskInfo, null);
                return;
            }

            mDownloadTaskInfo.state = Status.START;
            synchronized (this) {
                if (checkStoppedOrDeletedBeforeRunning())
                    return;
                state = DOWNLOADER_STATE.RUNNING_NOT_PREPARED;
            }
            starttime = System.currentTimeMillis();
            Log.d(TAG, "downloader run onprepare");
            if (listener != null)
                listener.onDownloadStatus(Status.START, mDownloadTaskInfo, null);
            length = mDownloadTaskInfo.size;
            synchronized (this) {
                checkAlreadyStopOrDelete();
                state = DOWNLOADER_STATE.PREPARED_NOT_START;
            }
            _start();
//            if (listener != null)
//                listener.onStart(task);
            synchronized (this) {
                checkAlreadyStopOrDelete();
                state = DOWNLOADER_STATE.PROCESSING;
            }
            mDownloadTaskInfo.state = Status.PROGRESS;
            if (timer != null) {
                timer.cancel();
                timer = null;
            }
            timer = new Timer();
            timer.schedule(new TimerTask() {
                float prev_current = -1, current = -1;

                @Override
                public void run() {
                    if (state == DOWNLOADER_STATE.PROCESSING) {
                        current = _current();
                        Log.i(TAG, "curLength: " + current);
                        if (prev_current == -1)
                            prev_current = current;
                        else {
                            float speed = (current - prev_current) / TIMER_INTERVAL;
                            prev_current = current;

                            if (listener != null) {
                                listener.onDownloadStatus(Status.PROGRESS, mDownloadTaskInfo, null);
                            }
                        }
                    }
                }
            }, 0, (int) TIMER_INTERVAL * 1000);
            _process();
//            Android.chmodApkPermission(task.getSavedFilePath());
            mDownloadTaskInfo.state = Status.FINISH;
            state = DOWNLOADER_STATE.FINISH;
            if (listener != null) {
                //给下载文件赋予权限
                Android.chmodArchive(savefile, mContext);
                listener.onDownloadStatus(Status.FINISH, mDownloadTaskInfo, null);
                Log.i("xxx", "DownloaderImpl:下载完成");
            }
        } catch (OnStopException e) {
            Log.d(TAG, "*********************************OnStopException!! listener:" + listener);
            synchronized (this) {
                state = DOWNLOADER_STATE.NOT_RUNNING;
            }
            mDownloadTaskInfo.state = Status.PAUSE;
            if (listener != null)
                listener.onDownloadStatus(Status.PAUSE, mDownloadTaskInfo, null);
        } catch (OnDeleteException e) {
            synchronized (this) {
                state = DOWNLOADER_STATE.NOT_RUNNING;
            }
            mDownloadTaskInfo.state = Status.REMOVE;
            if (listener != null)
                listener.onDownloadStatus(Status.REMOVE, mDownloadTaskInfo, null);
        } catch (OnErrorException e) {
            synchronized (this) {
                state = DOWNLOADER_STATE.NOT_RUNNING;
            }
            if (listener != null) {
                if (e.error != 0) {
//                    if(!Android.isNetConnected(UpgraderManager.getContext())) {
//                        e.error.errcode = DownloadError.ERRCODE_DISCONNECT;
//                    } else if(e.error.errcode == 0) {
//                        e.error.errcode = DownloadError.ERRCODE;
//                    }
                }

                mDownloadTaskInfo.state = Status.ERROR;
                listener.onDownloadStatus(Status.ERROR, mDownloadTaskInfo, null);
            }
        } finally {
            if (timer != null)
                timer.cancel();
            _release();
            timer = null;
            thread = null;
            starttime = 0;
        }

        synchronized (state) {
            state = DOWNLOADER_STATE.NOT_RUNNING;
        }

        synchronized (this) {
            this.notify();
            Log.i(TAG, "processToStart notify");
        }
    }


    @Override
    public void onStart() {

    }

    @Override
    public void onPause() {

    }

    @Override
    public void onCancel() {
        _delete();
    }

    @Override
    public DownloadTaskInfo getDownloadTaskInfo() {
        return mDownloadTaskInfo;
    }

    @Override
    public void setDownloadTaskManagerListener(IDownloadTaskManagerListener listener) {
        this.listener = listener;
    }

    private boolean checkStoppedOrDeletedBeforeRunning() {
        boolean ret = false;
        synchronized (this) {
            ret = (state == DOWNLOADER_STATE.DOING_STOP)
                    || (state == DOWNLOADER_STATE.DOING_DELETE);

            state = DOWNLOADER_STATE.NOT_RUNNING;
        }
        return ret;
    }

    private void checkAlreadyStopOrDelete() throws OnStopException, OnDeleteException {
        synchronized (this) {
            if (state == DOWNLOADER_STATE.DOING_STOP)
                throw new OnStopException();
            else if (state == DOWNLOADER_STATE.DOING_DELETE)
                throw new OnDeleteException();
        }
    }

    protected float _current() {
        return mDownloadTaskInfo.getCurrentLength();
    }

    protected void _start() {
        bStop = false;
        bDelete = false;
    }

    protected void _stop() {
        bStop = true;
    }

    protected void _delete() {
        bDelete = true;
        if (state != DOWNLOADER_STATE.PROCESSING) {
            _do_delete();
            mDownloadTaskInfo.state = Status.REMOVE;
            listener.onDownloadStatus(Status.REMOVE, mDownloadTaskInfo, null);
        }
    }

    protected void _release() {
        try {
            if (input != null)
                input.close();
            if (oSavedFile != null)
                oSavedFile.close();
            if (httpUrl != null)
                httpUrl.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
            Log.i("xxx", "_release Exception");
        }
    }

    protected void _do_delete() {
        if (savefile != null) {
            File df = new File(savefile);
            if (df.exists()) {
                df.delete();
            }
        } else {
            if (mDownloadTaskInfo == null)
                return;
            savefile = mDownloadTaskInfo.savePath;
            if (savefile != null) {
                File df = new File(savefile);
                if (df.exists()) {
                    df.delete();
                }
            }
        }
    }

    boolean isSocketTimeout = false;
    boolean isReconnection = false;
    int timeoutNum = 0;

    private boolean bStop = false, bDelete = false;
    private String savefile = null;

    private HttpURLConnection httpUrl = null;
    private InputStream input = null;
    private BufferedRandomAccessFile oSavedFile = null;

    protected void _process() throws OnStopException, OnDeleteException, OnErrorException {
        DownloadTaskInfo task = mDownloadTaskInfo;
        Log.i("xxx", "加后缀apk之前");
        if (new File(task.savePath).isDirectory()) {
            task.savePath = task.savePath + File.separator + MD5Util.md5Encode(task.downloadInfo.url) + ".apk";
            Log.i("xxx", "加后缀apk之后:" + task.savePath);
            new File(task.savePath);
        } else if (!new File(task.savePath).exists()) {
            new File(task.savePath);
        }

        savefile = task.savePath;
        boolean isDelete = false;
        Log.i(TAG, "_process: " + task.savePath);
        Log.i(TAG, "_process: " + task.downloadInfo.url);

        synchronized (this) {
            long freeSpace = getFreeSpace(new File(savefile).getParentFile().getAbsolutePath());
            Log.i(TAG, "freeSpace: " + freeSpace + ", size: " + mDownloadTaskInfo.size);
            if (mDownloadTaskInfo.size > freeSpace) {
                if (listener != null) {
                    mDownloadTaskInfo.state = Status.ERROR_SPACE;
                    listener.onDownloadStatus(Status.ERROR_SPACE, mDownloadTaskInfo, null);
                }
                return;
            }
        }

        try {
            URL _url = new URL(task.downloadInfo.url);
            Log.i(TAG, "_process1");
            oSavedFile = new BufferedRandomAccessFile(savefile, "rws");
            Log.i(TAG, "_process2");
            oSavedFile.seek(task.getCurrentLength());
            httpUrl = (HttpURLConnection) _url.openConnection();
            httpUrl.setReadTimeout(1000 * 10);
            httpUrl.setConnectTimeout(1000 * 10);
            Log.i(TAG, "ConnectTimeout 1000 * 10");
            if (task.getCurrentLength() > 0)
                httpUrl.setRequestProperty("RANGE", "bytes=" + task.getCurrentLength() + "-");

            input = httpUrl.getInputStream();
            byte[] b = new byte[1024];
            int nRead;

            isSocketTimeout = false;
            isReconnection = false;

            int sum = 0;
            while ((nRead = input.read(b)) > 0) {
                if (bStop) {
                    throw new OnStopException();
                }

                if (bDelete) {
                    if (savefile != null) {
                        File df = new File(savefile);
                        if (df.exists()) {
                            df.delete();
                        }
                    }

                    isDelete = true;
                    throw new OnDeleteException();
                }

                oSavedFile.write(b, 0, nRead);

                sum = 0;
                for (int i = 0; i < nRead; i++) {
                    sum += b[i];
                }

//                task.(task.getCheckSum() + sum);
//                task.setCurrent(task.getCurrent() + nRead);
            }
        } catch (OnStopException e) {
            Log.i(TAG, "OnStopException");
            throw e;
        } catch (OnDeleteException e) {
            Log.i(TAG, "OnDeleteException");
            throw e;
        } catch (MalformedURLException e) {
            e.printStackTrace();
            throw new OnErrorException(Status.ERROR);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            throw new OnErrorException(Status.ERROR);
        } catch (SocketTimeoutException e) {
            //直接打印，否则异常信息过多且频繁导致刷屏
            Log.i(TAG, "java.net.SocketTimeoutException");
            isSocketTimeout = true;
        } catch (ProtocolException e) {
            Log.i(TAG, "ProtocolException: " + e.toString());
            isReconnection = true;
        } catch (IOException e) {
            e.printStackTrace();
            throw new OnErrorException(Status.ERROR);
        } catch (Exception e) {
            Log.i(TAG, e.toString());
            e.printStackTrace();
            throw new OnErrorException(Status.ERROR);
        } finally {
            if (isSocketTimeout || isReconnection) {
                if (bStop) {
                    throw new OnStopException();
                }

                if (bDelete) {
                    if (savefile != null) {
                        File df = new File(savefile);
                        if (df.exists()) {
                            df.delete();
                        }
                    }
                    throw new OnDeleteException();
                }

                if (isSocketTimeout) {
                    if (timeoutNum >= 3) {
                        timeoutNum = 0;
                        Log.i(TAG, "timeout num is greater than 3.");
                        throw new OnErrorException(Status.ERROR);
                    }

                    timeoutNum++;
                    Log.i(TAG, "timeout num = " + timeoutNum + ", taskId = ");
                }

                if (isReconnection) {
                    isSocketTimeout = true;
                }

                if (Android.isNetworkConnected(mContext)) {
                    try {
                        if (input != null)
                            input.close();
                        if (oSavedFile != null)
                            oSavedFile.close();
                        if (httpUrl != null)
                            httpUrl.disconnect();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    _process();
                } else {
                    throw new OnErrorException(Status.ERROR);
                }
                return;
            }

            if (!isDelete)
                try {
//                    table.update(task.ID, downloaderInfo);
                } catch (Exception e) {
                    e.printStackTrace();
                }

            //使用BufferedRandomAccessFile需先关闭文件流，否则某些APK MD5校验跟实际不一致
            try {
                if (input != null)
                    input.close();
                if (oSavedFile != null)
                    oSavedFile.close();
                if (httpUrl != null)
                    httpUrl.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    public long getFreeSpace(String path) {
        return Android.getFreeSpace(path);
    }
}
